/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.archives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.model.CachingMap;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.cms.CMSArchiveConfig;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

/**
 * Application-wide manager for {@link CardCatalogIndex card catalog indexes}: builds and caches a
 * compact positional index per card catalog archive and answers the interval, list and register
 * lookups of the card catalog REST API. Deliberately does not use {@link ArchiveManager}'s full
 * archive trees; a card catalog with hundreds of thousands of nodes must never be materialized as
 * {@link ArchiveEntry} objects.
 */
public class CardCatalogManager {

    private static final Logger logger = LogManager.getLogger(CardCatalogManager.class);

    /** Minimum interval between DATEINDEXED staleness lookups per catalog. */
    private static final long STALENESS_CHECK_INTERVAL_MILLIS = 60_000;
    private static final int MAX_CACHED_CATALOGS = 20;
    /** Upper bound for the number of boundary cards of one zoom step. */
    public static final int MAX_BOUNDARIES = 25;
    /** Upper bound for the number of cards returned by one entries request. */
    public static final int MAX_ENTRIES_PER_REQUEST = 100;

    private static final String FIELD_ARCHIVE_ORDER = "SORTNUM_ARCHIVE_ORDER";
    private static final List<String> SOLR_FIELDS_ROOT =
            Arrays.asList(SolrConstants.PI, SolrConstants.TITLE, SolrConstants.DATEINDEXED);
    private static final List<String> SOLR_FIELDS_STRUCTURE = Arrays.asList(SolrConstants.IDDOC, SolrConstants.IDDOC_PARENT,
            SolrConstants.EAD_NODE_ID, SolrConstants.TITLE, SolrConstants.PI, FIELD_ARCHIVE_ORDER);
    private static final List<String> SOLR_FIELDS_ENTRIES = Arrays.asList(SolrConstants.EAD_NODE_ID, SolrConstants.TITLE);

    private final CachingMap<String, CardCatalogIndex> catalogs = new CachingMap<>(MAX_CACHED_CATALOGS);
    /** Global EAD_NODE_ID -> (record PI, image available) map, loaded lazily; reset when an index is (re)built or unloaded. */
    private volatile Map<String, Entry<String, Boolean>> associatedRecordMap = null;
    /** Cached card catalog flags per archive PI, cleared periodically so admin changes propagate. */
    private final CachingMap<String, Boolean> cardCatalogFlags = new CachingMap<>(100);
    private volatile long cardCatalogFlagsCleared = System.currentTimeMillis();

    /**
     * Returns the (possibly cached) card catalog index for the given archive resource. Staleness against the Solr index is checked via DATEINDEXED,
     * throttled to one lookup per minute per catalog.
     *
     * @param pi PI of the archive resource
     * @return the index, or empty if no archive with the given PI is indexed
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public Optional<CardCatalogIndex> getCatalog(String pi) throws PresentationException, IndexUnreachableException {
        if (StringUtils.isBlank(pi)) {
            return Optional.empty();
        }

        CardCatalogIndex index = catalogs.get(pi);
        if (index != null && !index.isStalenessCheckDue(STALENESS_CHECK_INTERVAL_MILLIS)) {
            return Optional.of(index);
        }

        SolrDocument rootDoc = getSearchIndex().getFirstDoc(
                "+" + SolrConstants.PI + ":\"" + pi + "\" +" + SolrConstants.DOCTYPE + ":" + DocType.ARCHIVE.name(), SOLR_FIELDS_ROOT);
        if (rootDoc == null) {
            synchronized (catalogs) {
                catalogs.remove(pi);
            }
            return Optional.empty();
        }

        long dateIndexed = getLatestDateIndexed(rootDoc);
        if (index != null && index.getDateIndexed() == dateIndexed) {
            index.touchStalenessCheck();
            return Optional.of(index);
        }

        index = buildIndex(pi, rootDoc, dateIndexed);
        catalogs.put(pi, index);
        associatedRecordMap = null;
        return Optional.of(index);
    }

    /**
     * Returns the boundary cards of one zoom step: {@code count} cards evenly distributed over the position interval [from, to] of the given drawer.
     *
     * @param index card catalog index
     * @param drawer drawer to resolve positions in
     * @param from 1-based start position; values out of range are clamped
     * @param to 1-based end position; values out of range are clamped
     * @param count number of boundary cards; clamped to [2, {@link #MAX_BOUNDARIES}]
     * @return boundary positions (1-based) in ascending order, without duplicates
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public List<Integer> getBoundaryPositions(CardCatalogIndex index, CardCatalogDrawer drawer, Integer from, Integer to, Integer count)
            throws PresentationException, IndexUnreachableException {
        List<CardCatalogEntry> entries = getEntries(index, drawer);
        int n = entries.size();
        if (n == 0) {
            return Collections.emptyList();
        }
        int fromPos = clamp(from != null ? from : 1, 1, n);
        int toPos = clamp(to != null ? to : n, fromPos, n);
        int boundaryCount = clamp(count != null ? count : DataManager.getInstance().getConfiguration().getCardCatalogZoomFactor() + 1, 2,
                MAX_BOUNDARIES);
        return computeBoundaryPositions(fromPos, toPos, boundaryCount);
    }

    /**
     * Returns the cards of the given drawer, loading them from Solr on first access.
     *
     * @param index card catalog index the drawer belongs to
     * @param drawer drawer whose cards to return
     * @return cards in catalog order
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public List<CardCatalogEntry> getEntries(CardCatalogIndex index, CardCatalogDrawer drawer)
            throws PresentationException, IndexUnreachableException {
        List<CardCatalogEntry> entries = drawer.getEntries();
        if (entries != null) {
            return entries;
        }
        synchronized (drawer) {
            entries = drawer.getEntries();
            if (entries != null) {
                return entries;
            }
            entries = loadEntries(drawer);
            drawer.setEntries(entries);
            logger.debug("Loaded {} cards for drawer '{}' of card catalog '{}'.", entries.size(), drawer.getLabel(), index.getPi());
            return entries;
        }
    }

    /**
     * Returns the computed from-to register of the given catalog, or empty if the catalog exceeds the configured register threshold. The register is
     * computed once per drawer and cached.
     *
     * @param index card catalog index
     * @return register ranges per drawer, in drawer order; empty if the catalog is above the register threshold
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public Optional<Map<CardCatalogDrawer, List<CardCatalogRegisterRange>>> getRegister(CardCatalogIndex index)
            throws PresentationException, IndexUnreachableException {
        int threshold = DataManager.getInstance().getConfiguration().getCardCatalogRegisterThreshold();
        if (index.getTotalEntries() > threshold) {
            return Optional.empty();
        }

        int chunkSize = DataManager.getInstance().getConfiguration().getCardCatalogRegisterChunkSize();
        Map<CardCatalogDrawer, List<CardCatalogRegisterRange>> ret = new java.util.LinkedHashMap<>();
        for (CardCatalogDrawer drawer : index.getDrawers()) {
            List<CardCatalogRegisterRange> ranges = drawer.getRegisterRanges();
            if (ranges == null) {
                synchronized (drawer) {
                    ranges = drawer.getRegisterRanges();
                    if (ranges == null) {
                        ranges = computeRegisterRanges(getEntries(index, drawer), chunkSize);
                        drawer.setRegisterRanges(ranges);
                    }
                }
            }
            ret.put(drawer, ranges);
        }

        return Optional.of(ret);
    }

    /**
     * Checks whether the archive resource with the given PI is flagged as a card catalog in its CMS configuration. Results are cached and refreshed
     * periodically, so this is cheap enough for search hit and widget rendering.
     *
     * @param pi PI of the archive resource
     * @return true if the archive is flagged as a card catalog
     */
    public boolean isCardCatalogResource(String pi) {
        if (StringUtils.isBlank(pi)) {
            return false;
        }
        if (System.currentTimeMillis() - cardCatalogFlagsCleared > STALENESS_CHECK_INTERVAL_MILLIS) {
            synchronized (cardCatalogFlags) {
                cardCatalogFlags.clear();
            }
            cardCatalogFlagsCleared = System.currentTimeMillis();
        }
        Boolean flag = cardCatalogFlags.get(pi);
        if (flag == null) {
            try {
                flag = DataManager.getInstance()
                        .getDao()
                        .getCmsArchiveConfigForArchive(pi)
                        .map(CMSArchiveConfig::isCardCatalog)
                        .orElse(Boolean.FALSE);
            } catch (DAOException e) {
                logger.warn("Could not load archive configuration for '{}': {}", pi, e.getMessage());
                flag = Boolean.FALSE;
            }
            cardCatalogFlags.put(pi, flag);
        }
        return flag;
    }

    /**
     * Locates a card within the catalog by its EAD node ID: the drawer containing it and its 1-based position within that drawer. Resolves the
     * card's parent via one Solr lookup and loads only that one drawer.
     *
     * @param index card catalog index
     * @param entryId EAD node ID of the card
     * @return drawer and 1-based position; empty if the entry is not part of a drawer of this catalog
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public Optional<ImmutablePair<CardCatalogDrawer, Integer>> findEntryPosition(CardCatalogIndex index, String entryId)
            throws PresentationException, IndexUnreachableException {
        if (StringUtils.isBlank(entryId)) {
            return Optional.empty();
        }
        SolrDocument doc = getSearchIndex().getFirstDoc("+" + SolrConstants.DOCTYPE + ":" + DocType.ARCHIVE.name() + " +"
                + SolrConstants.PI_TOPSTRUCT + ":\"" + index.getPi() + "\" +" + SolrConstants.EAD_NODE_ID + ":\"" + entryId + "\"",
                Collections.singletonList(SolrConstants.IDDOC_PARENT));
        if (doc == null) {
            return Optional.empty();
        }
        String parentIddoc = SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC_PARENT);
        for (CardCatalogDrawer drawer : index.getDrawers()) {
            if (drawer.getIddoc().equals(parentIddoc)) {
                List<CardCatalogEntry> entries = getEntries(index, drawer);
                for (int i = 0; i < entries.size(); i++) {
                    if (entryId.equals(entries.get(i).getId())) {
                        return Optional.of(new ImmutablePair<>(drawer, i + 1));
                    }
                }
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * Removes the cached indexes of the given archive resources, so that they are rebuilt from Solr on next access. Called by the message queue
     * handler when records associated with an archive have been (re)indexed.
     *
     * @param resourceIds PIs of the archive resources to unload
     * @return number of unloaded indexes
     */
    public int unloadCatalogs(Collection<String> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return 0;
        }
        int count = 0;
        synchronized (catalogs) {
            for (String pi : resourceIds) {
                if (catalogs.remove(pi) != null) {
                    count++;
                }
            }
        }
        if (count > 0) {
            associatedRecordMap = null;
        }
        return count;
    }

    /**
     * @param from 1-based start position, inclusive
     * @param to 1-based end position, inclusive; must be &gt;= from
     * @param count requested number of boundaries
     * @return 1-based boundary positions, evenly distributed over [from, to], ascending, without duplicates
     * @should return single position for empty interval
     * @should distribute positions evenly
     * @should not return duplicate positions for narrow intervals
     */
    static List<Integer> computeBoundaryPositions(int from, int to, int count) {
        int width = to - from;
        if (width <= 0) {
            return Collections.singletonList(from);
        }
        int boundaries = Math.min(count, width + 1);
        List<Integer> ret = new ArrayList<>(boundaries);
        for (int i = 0; i < boundaries; i++) {
            int pos = from + Math.round((float) i * width / (boundaries - 1));
            if (ret.isEmpty() || ret.get(ret.size() - 1) != pos) {
                ret.add(pos);
            }
        }
        return ret;
    }

    /**
     * @param entries cards of one drawer, in catalog order
     * @param chunkSize number of cards per register range
     * @return register ranges covering all given cards
     * @should chunk entries into ranges of given size
     * @should fall back to nearest titled entry for blank labels
     * @should return empty list for empty entries
     */
    static List<CardCatalogRegisterRange> computeRegisterRanges(List<CardCatalogEntry> entries, int chunkSize) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }
        int size = Math.max(1, chunkSize);
        List<CardCatalogRegisterRange> ret = new ArrayList<>((entries.size() + size - 1) / size);
        for (int start = 0; start < entries.size(); start += size) {
            int end = Math.min(start + size - 1, entries.size() - 1);
            ret.add(new CardCatalogRegisterRange(start + 1, end + 1, findLabel(entries, start, end, true), findLabel(entries, end, start, false)));
        }
        return ret;
    }

    /**
     * Returns the label of the entry at the given position, falling back to the nearest titled entry within [min(from,to), max(from,to)] when the
     * label is blank. Display labels may differ from the actual boundary card this way, but a register entry always shows a usable ordering title.
     */
    private static String findLabel(List<CardCatalogEntry> entries, int from, int to, boolean forward) {
        int step = forward ? 1 : -1;
        for (int i = from; forward ? i <= to : i >= to; i += step) {
            String label = entries.get(i).getLabel();
            if (StringUtils.isNotBlank(label)) {
                return label;
            }
        }
        return "";
    }

    private CardCatalogIndex buildIndex(String pi, SolrDocument rootDoc, long dateIndexed)
            throws PresentationException, IndexUnreachableException {
        logger.debug("Building card catalog index for '{}'.", pi);

        // Child counts per parent node over the entire archive
        String nodesQuery = "+" + SolrConstants.DOCTYPE + ":" + DocType.ARCHIVE.name() + " +" + SolrConstants.PI_TOPSTRUCT + ":\"" + pi + "\"";
        QueryResponse facetResponse =
                getSearchIndex().searchFacetsAndStatistics(nodesQuery, null, Collections.singletonList(SolrConstants.IDDOC_PARENT), 1, false);
        Map<String, Integer> childCounts = new HashMap<>();
        FacetField facetField = facetResponse.getFacetField(SolrConstants.IDDOC_PARENT);
        if (facetField != null && facetField.getValues() != null) {
            for (FacetField.Count value : facetField.getValues()) {
                childCounts.put(value.getName(), (int) value.getCount());
            }
        }
        if (childCounts.isEmpty()) {
            return new CardCatalogIndex(pi, getRootLabel(rootDoc, pi), dateIndexed, new ArrayList<>());
        }

        // All structure nodes (= nodes that have children), including the root document.
        // Plain Lucene OR syntax instead of a {!terms} local-params query: query preprocessing in
        // SolrSearchIndex does not preserve local params, and there are only ever few structure nodes.
        String structureQuery = "+" + SolrConstants.IDDOC + ":(" + String.join(" ", childCounts.keySet()) + ")";
        SolrDocumentList structureDocs = getSearchIndex().search(structureQuery, SolrSearchIndex.MAX_HITS, null, SOLR_FIELDS_STRUCTURE);

        Map<String, List<SolrDocument>> structureChildren = new HashMap<>();
        String rootIddoc = null;
        for (SolrDocument doc : structureDocs) {
            if (pi.equals(SolrTools.getSingleFieldStringValue(doc, SolrConstants.PI))) {
                rootIddoc = SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC);
                continue;
            }
            String parentIddoc = SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC_PARENT);
            if (parentIddoc != null) {
                structureChildren.computeIfAbsent(parentIddoc, k -> new ArrayList<>()).add(doc);
            }
        }
        for (List<SolrDocument> children : structureChildren.values()) {
            children.sort((a, b) -> Integer.compare(getOrder(a), getOrder(b)));
        }

        // Drawers are the structure nodes without structural children, in depth-first catalog order.
        // Intermediate levels (e.g. a single wrapping "Zettelkatalog" folder) are skipped this way.
        List<CardCatalogDrawer> drawers = new ArrayList<>();
        if (rootIddoc != null) {
            collectDrawers(rootIddoc, rootDoc, structureChildren, childCounts, drawers);
        } else {
            logger.warn("Root document of archive '{}' not found among its structure nodes; card catalog index will be empty.", pi);
        }

        logger.debug("Card catalog index for '{}' built: {} drawers, {} cards.", pi, drawers.size(),
                drawers.stream().mapToInt(CardCatalogDrawer::getEntryCount).sum());
        return new CardCatalogIndex(pi, getRootLabel(rootDoc, pi), dateIndexed, drawers);
    }

    private static void collectDrawers(String iddoc, SolrDocument doc, Map<String, List<SolrDocument>> structureChildren,
            Map<String, Integer> childCounts, List<CardCatalogDrawer> drawers) {
        List<SolrDocument> children = structureChildren.get(iddoc);
        if (children == null || children.isEmpty()) {
            int entryCount = childCounts.getOrDefault(iddoc, 0);
            if (entryCount > 0) {
                String id = SolrTools.getSingleFieldStringValue(doc, SolrConstants.EAD_NODE_ID);
                String label = SolrTools.getSingleFieldStringValue(doc, SolrConstants.TITLE);
                drawers.add(new CardCatalogDrawer(StringUtils.isNotBlank(id) ? id : iddoc, iddoc,
                        StringUtils.isNotBlank(label) ? label : String.valueOf(drawers.size() + 1), entryCount));
            }
            return;
        }
        for (SolrDocument child : children) {
            collectDrawers(SolrTools.getSingleFieldStringValue(child, SolrConstants.IDDOC), child, structureChildren, childCounts, drawers);
        }
    }

    private List<CardCatalogEntry> loadEntries(CardCatalogDrawer drawer) throws PresentationException, IndexUnreachableException {
        String query = "+" + SolrConstants.DOCTYPE + ":" + DocType.ARCHIVE.name() + " +" + SolrConstants.IDDOC_PARENT + ":\"" + drawer.getIddoc()
                + "\"";
        SolrDocumentList docs = getSearchIndex().search(query, SolrSearchIndex.MAX_HITS,
                Collections.singletonList(new StringPair(FIELD_ARCHIVE_ORDER, "asc")), SOLR_FIELDS_ENTRIES);

        Map<String, Entry<String, Boolean>> recordMap = getAssociatedRecordMap();
        List<CardCatalogEntry> ret = new ArrayList<>(docs.size());
        for (SolrDocument doc : docs) {
            String id = SolrTools.getSingleFieldStringValue(doc, SolrConstants.EAD_NODE_ID);
            String label = SolrTools.getSingleFieldStringValue(doc, SolrConstants.TITLE);
            Entry<String, Boolean> record = id != null ? recordMap.get(id) : null;
            ret.add(new CardCatalogEntry(id, label, record != null ? record.getKey() : null,
                    record != null && Boolean.TRUE.equals(record.getValue())));
        }
        return Collections.unmodifiableList(ret);
    }

    private Map<String, Entry<String, Boolean>> getAssociatedRecordMap() throws PresentationException, IndexUnreachableException {
        Map<String, Entry<String, Boolean>> local = associatedRecordMap;
        if (local == null) {
            synchronized (this) {
                if (associatedRecordMap == null) {
                    associatedRecordMap = ArchiveParser.getAssociatedRecordPis(getSearchIndex());
                    logger.debug("Loaded {} associated record identifiers for card catalogs.", associatedRecordMap.size());
                }
                local = associatedRecordMap;
            }
        }
        return local;
    }

    private static String getRootLabel(SolrDocument rootDoc, String pi) {
        String label = SolrTools.getSingleFieldStringValue(rootDoc, SolrConstants.TITLE);
        return StringUtils.isNotBlank(label) ? label : pi;
    }

    private static long getLatestDateIndexed(SolrDocument rootDoc) {
        long ret = 0;
        for (String value : SolrTools.getMetadataValues(rootDoc, SolrConstants.DATEINDEXED)) {
            try {
                ret = Math.max(ret, Long.parseLong(value));
            } catch (NumberFormatException e) {
                logger.warn("Unparseable {} value: {}", SolrConstants.DATEINDEXED, value);
            }
        }
        return ret;
    }

    private static int getOrder(SolrDocument doc) {
        Object value = doc.getFieldValue(FIELD_ARCHIVE_ORDER);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static SolrSearchIndex getSearchIndex() {
        return DataManager.getInstance().getSearchIndex();
    }
}
