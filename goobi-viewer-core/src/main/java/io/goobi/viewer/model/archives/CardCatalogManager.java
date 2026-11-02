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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
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
 * {@link ArchiveEntry} objects. Card pages and associated records are queried on demand.
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
    /** Upper bound of pages read when a card has to be located by scanning its drawer. */
    public static final int MAX_POSITION_SCAN_PAGES = 10;
    private static final int REGISTER_PAGE_SIZE = 1000;
    private static final int STRUCTURE_IDS_PER_QUERY = 256;
    private static final int CATALOG_LOCK_COUNT = 64;

    private static final String FIELD_ARCHIVE_ORDER = "SORTNUM_ARCHIVE_ORDER";
    private static final List<String> SOLR_FIELDS_ROOT =
            Arrays.asList(SolrConstants.PI, SolrConstants.TITLE, SolrConstants.DATEINDEXED, SolrConstants.ACCESSCONDITION);
    private static final List<String> SOLR_FIELDS_STRUCTURE = Arrays.asList(SolrConstants.IDDOC, SolrConstants.IDDOC_PARENT,
            SolrConstants.EAD_NODE_ID, SolrConstants.TITLE, SolrConstants.PI, FIELD_ARCHIVE_ORDER);
    private static final List<String> SOLR_FIELDS_ENTRIES = Arrays.asList(SolrConstants.EAD_NODE_ID, SolrConstants.TITLE, FIELD_ARCHIVE_ORDER);
    private static final List<String> SOLR_FIELDS_PAGES = Arrays.asList(SolrConstants.PI_TOPSTRUCT, SolrConstants.ORDER, SolrConstants.FILENAME,
            SolrConstants.WIDTH, SolrConstants.HEIGHT);
    /** Card records carry front, back and at most one more image, such as a header strip. */
    private static final int MAX_CARD_PAGES = 3;
    /** Largest relative deviation of an image's aspect ratio from the front for it to count as the back of the card. */
    private static final double BACK_SIDE_RATIO_TOLERANCE = 0.2;

    private final CachingMap<String, CardCatalogIndex> catalogs = new CachingMap<>(MAX_CACHED_CATALOGS);
    private final Object[] catalogLocks = new Object[CATALOG_LOCK_COUNT];
    /** Cached catalog types per archive PI (empty without catalog view), cleared periodically so admin changes propagate. */
    private final CachingMap<String, Optional<CMSArchiveConfig.CatalogType>> catalogTypes = new CachingMap<>(100);
    private volatile long catalogTypesCleared = System.currentTimeMillis();

    public CardCatalogManager() {
        Arrays.setAll(catalogLocks, i -> new Object());
    }

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

        synchronized (catalogLock(pi)) {
            CardCatalogIndex index = catalogs.get(pi);
            if (index != null && !index.isStalenessCheckDue(STALENESS_CHECK_INTERVAL_MILLIS)) {
                return Optional.of(index);
            }

            SolrDocument rootDoc = loadRootDocument(pi);
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

            for (int attempt = 0; attempt < 2; attempt++) {
                CardCatalogIndex built = buildIndex(pi, rootDoc, dateIndexed);
                SolrDocument checkedRoot = loadRootDocument(pi);
                if (checkedRoot == null) {
                    synchronized (catalogs) {
                        catalogs.remove(pi);
                    }
                    return Optional.empty();
                }
                long checkedDate = getLatestDateIndexed(checkedRoot);
                if (checkedDate == dateIndexed) {
                    catalogs.put(pi, built);
                    return Optional.of(built);
                }
                rootDoc = checkedRoot;
                dateIndexed = checkedDate;
            }
            throw new IllegalStateException("Card catalog '" + pi + "' changed repeatedly while building its index");
        }
    }

    private SolrDocument loadRootDocument(String pi) throws PresentationException, IndexUnreachableException {
        return getSearchIndex().getFirstDoc(
                "+" + SolrConstants.PI + ":" + quoteSolr(pi) + " +" + SolrConstants.DOCTYPE + ":" + DocType.ARCHIVE.name(), SOLR_FIELDS_ROOT);
    }

    /**
     * Returns the boundary cards of one zoom step: zoomFactor + 1 cards evenly distributed over the position interval [from, to] of the given
     * drawer.
     *
     * @param drawer drawer to resolve positions in
     * @param from 1-based start position; values out of range are clamped
     * @param to 1-based end position; values out of range are clamped
     * @return boundary positions (1-based) in ascending order, without duplicates
     */
    public List<Integer> getBoundaryPositions(CardCatalogDrawer drawer, Integer from, Integer to) {
        int n = drawer.getEntryCount();
        if (n == 0) {
            return Collections.emptyList();
        }
        int fromPos = clamp(from != null ? from : 1, 1, n);
        int toPos = clamp(to != null ? to : n, fromPos, n);
        int boundaryCount = normalizeZoomFactor(DataManager.getInstance().getConfiguration().getCardCatalogZoomFactor()) + 1;
        return computeBoundaryPositions(fromPos, toPos, boundaryCount);
    }

    public static int normalizeZoomFactor(int zoomFactor) {
        return clamp(zoomFactor, 2, MAX_BOUNDARIES - 1);
    }

    /**
     * Reads a bounded, 1-based inclusive range of cards in catalog order. The caller may request
     * at most {@link #MAX_ENTRIES_PER_REQUEST} cards; larger ranges are rejected rather than
     * silently truncated.
     */
    public List<CardCatalogEntry> getEntryRange(CardCatalogDrawer drawer, int from, int to)
            throws PresentationException, IndexUnreachableException {
        if (from < 1 || to < from || to > drawer.getEntryCount() || to - from + 1 > MAX_ENTRIES_PER_REQUEST) {
            throw new IllegalArgumentException("Invalid card catalog entry range: " + from + "-" + to);
        }
        return loadEntries(drawer, from - 1, to - from + 1, true);
    }

    /**
     * Reads the sparse boundary cards in position order and resolves their associated records in
     * one batch. Missing Solr rows are rejected so a stale index cannot return wrong positions.
     */
    public List<CardCatalogEntry> getEntriesAtPositions(CardCatalogDrawer drawer, List<Integer> positions)
            throws PresentationException, IndexUnreachableException {
        if (positions.size() > MAX_BOUNDARIES) {
            throw new IllegalArgumentException("Too many card catalog boundary positions: " + positions.size());
        }
        if (positions.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Integer> ranks = new HashSet<>(positions.size());
        for (int position : positions) {
            if (position < 1 || position > drawer.getEntryCount()) {
                throw new IllegalArgumentException("Invalid card catalog boundary position: " + position);
            }
            if (!ranks.add(position - 1)) {
                throw new IllegalArgumentException("Duplicate card catalog boundary position: " + position);
            }
        }

        String rankQuery = entryBaseQuery(drawer) + " +" + FIELD_ARCHIVE_ORDER + ":("
                + ranks.stream().sorted().map(String::valueOf).collect(Collectors.joining(" ")) + ")";
        try {
            SolrDocumentList ranked = getSearchIndex().search(rankQuery, 0, positions.size(), entrySort(), null, SOLR_FIELDS_ENTRIES).getResults();
            if (ranked.getNumFound() == positions.size() && ranked.size() == positions.size()) {
                Map<Integer, SolrDocument> byRank = new HashMap<>();
                for (SolrDocument doc : ranked) {
                    Integer rank = getIndexedOrder(doc);
                    if (rank == null || !ranks.contains(rank) || byRank.putIfAbsent(rank, doc) != null) {
                        byRank.clear();
                        break;
                    }
                }
                if (byRank.size() == positions.size()) {
                    List<SolrDocument> ordered = new ArrayList<>(positions.size());
                    for (int position : positions) {
                        ordered.add(byRank.get(position - 1));
                    }
                    return toEntries(ordered, true);
                }
            }
            logger.debug("Rank lookup is unavailable or inconsistent for drawer '{}'; using positional fallback.", drawer.getId());
        } catch (PresentationException e) {
            logger.warn("Rank lookup failed for drawer '{}'; using positional fallback: {}", drawer.getId(), e.getMessage());
        }

        List<SolrDocument> docs = new ArrayList<>(positions.size());
        for (int position : positions) {
            docs.addAll(loadOffsetEntryDocuments(drawer, position - 1, 1));
        }
        return toEntries(docs, true);
    }

    /** Returns only the requested drawer's register without computing any other drawer. */
    public Optional<List<CardCatalogRegisterRange>> getDrawerRegister(CardCatalogIndex index, CardCatalogDrawer drawer)
            throws PresentationException, IndexUnreachableException {
        int threshold = DataManager.getInstance().getConfiguration().getCardCatalogRegisterThreshold();
        if (index.getTotalEntries() > threshold) {
            return Optional.empty();
        }
        int chunkSize = Math.max(1, DataManager.getInstance().getConfiguration().getCardCatalogRegisterChunkSize());
        return Optional.of(drawer.getOrLoadRegisterRanges(chunkSize, () -> loadRegisterRanges(drawer, chunkSize)));
    }

    /**
     * Returns the catalog type of the archive resource with the given PI if its CMS configuration enables the catalog view. Results are cached and
     * refreshed periodically, so this is cheap enough for search hit and widget rendering.
     *
     * @param pi PI of the archive resource
     * @return the catalog type, or empty if the archive has no catalog view
     */
    public Optional<CMSArchiveConfig.CatalogType> getCatalogType(String pi) {
        if (StringUtils.isBlank(pi)) {
            return Optional.empty();
        }
        if (System.currentTimeMillis() - catalogTypesCleared > STALENESS_CHECK_INTERVAL_MILLIS) {
            synchronized (catalogTypes) {
                catalogTypes.clear();
            }
            catalogTypesCleared = System.currentTimeMillis();
        }
        return Optional.ofNullable(catalogTypes.get(pi)).orElseGet(() -> loadCatalogType(pi));
    }

    /**
     * Loads the catalog type of the archive resource with the given PI from its CMS configuration and caches the result (also if empty).
     *
     * @param pi PI of the archive resource
     * @return the catalog type, or empty if the archive has no catalog view or the configuration could not be loaded
     */
    private Optional<CMSArchiveConfig.CatalogType> loadCatalogType(String pi) {
        Optional<CMSArchiveConfig.CatalogType> type;
        try {
            type = DataManager.getInstance()
                    .getDao()
                    .getCmsArchiveConfigForArchive(pi)
                    .filter(CMSArchiveConfig::isCardCatalog)
                    .map(CMSArchiveConfig::getCatalogType);
        } catch (DAOException e) {
            logger.warn("Could not load archive configuration for '{}': {}", pi, e.getMessage());
            type = Optional.empty();
        }
        catalogTypes.put(pi, type);
        return type;
    }

    /**
     * Locates a card within the catalog by its EAD node ID: the drawer containing it and its 1-based position within that drawer. Current indexes
     * store a dense, zero-based sibling order, which is verified against the sorted card at that position. Otherwise the position is derived from
     * two Solr count queries (siblings sorting before the card) and verified the same way; as a last resort the first
     * {@link #MAX_POSITION_SCAN_PAGES} pages of the drawer are scanned.
     *
     * @param index card catalog index
     * @param entryId EAD node ID of the card
     * @return drawer and 1-based position; empty if the entry is not part of a drawer of this catalog or could not be located
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public Optional<ImmutablePair<CardCatalogDrawer, Integer>> findEntryPosition(CardCatalogIndex index, String entryId)
            throws PresentationException, IndexUnreachableException {
        if (StringUtils.isBlank(entryId)) {
            return Optional.empty();
        }
        SolrDocument doc = getSearchIndex().getFirstDoc("+" + SolrConstants.DOCTYPE + ":" + DocType.ARCHIVE.name() + " +"
                + SolrConstants.PI_TOPSTRUCT + ":" + quoteSolr(index.getPi()) + " +" + SolrConstants.EAD_NODE_ID + ":" + quoteSolr(entryId),
                Arrays.asList(SolrConstants.IDDOC, SolrConstants.IDDOC_PARENT, FIELD_ARCHIVE_ORDER));
        if (doc == null) {
            return Optional.empty();
        }
        String parentIddoc = SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC_PARENT);
        for (CardCatalogDrawer drawer : index.getDrawers()) {
            if (drawer.getIddoc().equals(parentIddoc)) {
                Integer order = getIndexedOrder(doc);
                if (order != null && order >= 0 && isEntryAt(drawer, order, entryId)) {
                    return Optional.of(new ImmutablePair<>(drawer, order + 1));
                }
                logger.warn("Archive order for card '{}' in drawer '{}' is not positional; counting preceding siblings as fallback.", entryId,
                        drawer.getId());
                Optional<Integer> counted = countPrecedingSiblings(drawer, doc);
                if (counted.isPresent() && isEntryAt(drawer, counted.get(), entryId)) {
                    return Optional.of(new ImmutablePair<>(drawer, counted.get() + 1));
                }
                int scanLimit = Math.min(drawer.getEntryCount(), MAX_ENTRIES_PER_REQUEST * MAX_POSITION_SCAN_PAGES);
                for (int start = 0; start < scanLimit; start += MAX_ENTRIES_PER_REQUEST) {
                    List<CardCatalogEntry> entries = loadEntries(drawer, start, Math.min(MAX_ENTRIES_PER_REQUEST, scanLimit - start), false);
                    for (int i = 0; i < entries.size(); i++) {
                        if (entryId.equals(entries.get(i).getId())) {
                            return Optional.of(new ImmutablePair<>(drawer, start + i + 1));
                        }
                    }
                }
                logger.warn("Card '{}' not found within the first {} cards of drawer '{}'; giving up.", entryId, scanLimit, drawer.getId());
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
        for (String pi : resourceIds) {
            if (pi != null) {
                synchronized (catalogLock(pi)) {
                    synchronized (catalogs) {
                        if (catalogs.remove(pi) != null) {
                            count++;
                        }
                    }
                }
            }
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
     * Builds the drawer index of an archive in three passes: child counts per parent node from one facet query over the whole archive, the
     * structure nodes (nodes with children) read in bounded batches, and finally the drawers, i.e. the structure nodes without structural children
     * in depth-first catalog order, which skips intermediate wrapping levels such as a single "Zettelkatalog" folder.
     *
     * @param pi archive identifier
     * @param rootDoc Solr document of the archive root
     * @param dateIndexed index timestamp of the archive
     * @return the built index
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private CardCatalogIndex buildIndex(String pi, SolrDocument rootDoc, long dateIndexed)
            throws PresentationException, IndexUnreachableException {
        logger.debug("Building card catalog index for '{}'.", pi);

        String nodesQuery = "+" + SolrConstants.DOCTYPE + ":" + DocType.ARCHIVE.name() + " +" + SolrConstants.PI_TOPSTRUCT + ":" + quoteSolr(pi);
        QueryResponse facetResponse = getSearchIndex().searchFacetsAndStatistics(nodesQuery, null,
                Arrays.asList(SolrConstants.IDDOC_PARENT, SolrConstants.ACCESSCONDITION), 1, false);
        Map<String, Integer> childCounts = new HashMap<>();
        FacetField facetField = facetResponse.getFacetField(SolrConstants.IDDOC_PARENT);
        if (facetField != null && facetField.getValues() != null) {
            for (FacetField.Count value : facetField.getValues()) {
                childCounts.put(value.getName(), (int) value.getCount());
            }
        }
        if (childCounts.isEmpty()) {
            return new CardCatalogIndex(pi, getRootLabel(rootDoc, pi), dateIndexed, new ArrayList<>(), false);
        }
        FacetField accessConditions = facetResponse.getFacetField(SolrConstants.ACCESSCONDITION);
        if (accessConditions == null) {
            throw new IllegalStateException("Cannot verify archive node access conditions for card catalog '" + pi + "'");
        }
        boolean restrictedDescendants = hasRestrictedDescendantConditions(rootDoc, accessConditions);

        Map<String, List<SolrDocument>> structureChildren = new HashMap<>();
        String rootIddoc = null;
        List<String> structureIds = new ArrayList<>(childCounts.keySet());
        for (int start = 0; start < structureIds.size(); start += STRUCTURE_IDS_PER_QUERY) {
            List<String> batch = structureIds.subList(start, Math.min(start + STRUCTURE_IDS_PER_QUERY, structureIds.size()));
            String structureQuery = "+" + SolrConstants.IDDOC + ":(" + batch.stream().map(CardCatalogManager::quoteSolr)
                    .collect(Collectors.joining(" ")) + ")";
            SolrDocumentList structureDocs = getSearchIndex().search(structureQuery, batch.size(), null, SOLR_FIELDS_STRUCTURE);
            Set<String> expected = new HashSet<>(batch);
            Set<String> seen = new HashSet<>();
            for (SolrDocument doc : structureDocs) {
                String iddoc = SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC);
                if (!expected.contains(iddoc) || !seen.add(iddoc)) {
                    throw new IllegalStateException("Unexpected or duplicate card catalog structure node: " + iddoc);
                }
                if (pi.equals(SolrTools.getSingleFieldStringValue(doc, SolrConstants.PI))) {
                    rootIddoc = iddoc;
                    continue;
                }
                String parentIddoc = SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC_PARENT);
                if (parentIddoc != null) {
                    structureChildren.computeIfAbsent(parentIddoc, k -> new ArrayList<>()).add(doc);
                }
            }
            if (seen.size() != expected.size() || structureDocs.getNumFound() != expected.size()) {
                throw new IllegalStateException("Card catalog structure changed while building index for '" + pi + "'");
            }
        }
        for (List<SolrDocument> children : structureChildren.values()) {
            children.sort(Comparator.comparingInt(CardCatalogManager::getOrder)
                    .thenComparing(doc -> StringUtils.defaultString(SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC))));
        }

        List<CardCatalogDrawer> drawers = new ArrayList<>();
        if (rootIddoc != null) {
            collectDrawers(rootIddoc, rootDoc, structureChildren, childCounts, drawers);
        } else {
            logger.warn("Root document of archive '{}' not found among its structure nodes; card catalog index will be empty.", pi);
        }

        logger.debug("Card catalog index for '{}' built: {} drawers, {} cards.", pi, drawers.size(),
                drawers.stream().mapToInt(CardCatalogDrawer::getEntryCount).sum());
        return new CardCatalogIndex(pi, getRootLabel(rootDoc, pi), dateIndexed, drawers, restrictedDescendants);
    }

    static boolean hasRestrictedDescendantConditions(SolrDocument rootDoc, FacetField accessConditions) {
        Set<String> rootConditions = new HashSet<>(SolrTools.getMetadataValues(rootDoc, SolrConstants.ACCESSCONDITION));
        if (accessConditions.getValues() == null) {
            return false;
        }
        for (FacetField.Count value : accessConditions.getValues()) {
            if (!SolrConstants.OPEN_ACCESS_VALUE.equals(value.getName())
                    && value.getCount() > (rootConditions.contains(value.getName()) ? 1 : 0)) {
                return true;
            }
        }
        return false;
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

    private List<CardCatalogRegisterRange> loadRegisterRanges(CardCatalogDrawer drawer, int chunkSize)
            throws PresentationException, IndexUnreachableException {
        int size = Math.max(1, chunkSize);
        List<CardCatalogRegisterRange> ranges = new ArrayList<>();
        int from = 1;
        int to = (int) Math.min((long) size, drawer.getEntryCount());
        String firstLabel = "";
        String lastLabel = "";
        for (int page = 0; page < drawer.getEntryCount(); page += REGISTER_PAGE_SIZE) {
            List<CardCatalogEntry> entries = loadEntries(drawer, page,
                    Math.min(REGISTER_PAGE_SIZE, drawer.getEntryCount() - page), false);
            for (int i = 0; i < entries.size(); i++) {
                int position = page + i + 1;
                String label = entries.get(i).getLabel();
                if (StringUtils.isNotBlank(label)) {
                    if (firstLabel.isEmpty()) {
                        firstLabel = label;
                    }
                    lastLabel = label;
                }
                if (position == to) {
                    ranges.add(new CardCatalogRegisterRange(from, to, firstLabel, lastLabel));
                    from = to + 1;
                    to = (int) Math.min((long) from + size - 1, drawer.getEntryCount());
                    firstLabel = "";
                    lastLabel = "";
                }
            }
        }
        return Collections.unmodifiableList(ranges);
    }

    private boolean isEntryAt(CardCatalogDrawer drawer, int position, String entryId) throws PresentationException, IndexUnreachableException {
        if (position < 0 || position >= drawer.getEntryCount()) {
            return false;
        }
        List<CardCatalogEntry> candidates = loadEntries(drawer, position, 1, false);
        return !candidates.isEmpty() && entryId.equals(candidates.get(0).getId());
    }

    /**
     * Zero-based rank of a card from two count queries: siblings with a lower archive order plus siblings with the same order and a smaller IDDOC,
     * the tie-breaker of the entry sort.
     *
     * @param drawer drawer containing the card
     * @param doc Solr document of the card with IDDOC and archive order
     * @return rank within the drawer; empty if the card carries no usable order or the counts exceed the drawer
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private Optional<Integer> countPrecedingSiblings(CardCatalogDrawer drawer, SolrDocument doc)
            throws PresentationException, IndexUnreachableException {
        Integer order = getIndexedOrder(doc);
        String iddoc = SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC);
        if (order == null || iddoc == null) {
            return Optional.empty();
        }
        long preceding = getSearchIndex().getHitCount(entryBaseQuery(drawer) + " +" + FIELD_ARCHIVE_ORDER + ":[* TO " + (order - 1) + "]")
                + getSearchIndex().getHitCount(entryBaseQuery(drawer) + " +" + FIELD_ARCHIVE_ORDER + ":" + order + " +" + SolrConstants.IDDOC
                        + ":[* TO " + quoteSolr(iddoc) + "}");
        return preceding < drawer.getEntryCount() ? Optional.of((int) preceding) : Optional.empty();
    }

    private List<CardCatalogEntry> loadEntries(CardCatalogDrawer drawer, int start, int rows, boolean withRecords)
            throws PresentationException, IndexUnreachableException {
        return toEntries(loadEntryDocuments(drawer, start, rows), withRecords);
    }

    private SolrDocumentList loadEntryDocuments(CardCatalogDrawer drawer, int start, int rows)
            throws PresentationException, IndexUnreachableException {
        String query = entryBaseQuery(drawer) + " +" + FIELD_ARCHIVE_ORDER + ":[" + start + " TO " + (start + rows - 1) + "]";
        try {
            SolrDocumentList ranked = getSearchIndex().search(query, 0, rows, entrySort(), null, SOLR_FIELDS_ENTRIES).getResults();
            if (ranked.getNumFound() == rows && ranked.size() == rows) {
                boolean dense = true;
                for (int i = 0; i < rows; i++) {
                    Integer rank = getIndexedOrder(ranked.get(i));
                    if (rank == null || rank != start + i) {
                        dense = false;
                        break;
                    }
                }
                if (dense) {
                    return ranked;
                }
            }
            logger.debug("Rank range is unavailable or inconsistent for drawer '{}'; using positional fallback.", drawer.getId());
        } catch (PresentationException e) {
            logger.warn("Rank range failed for drawer '{}'; using positional fallback: {}", drawer.getId(), e.getMessage());
        }
        return loadOffsetEntryDocuments(drawer, start, rows);
    }

    private SolrDocumentList loadOffsetEntryDocuments(CardCatalogDrawer drawer, int start, int rows)
            throws PresentationException, IndexUnreachableException {
        SolrDocumentList docs = getSearchIndex().search(entryBaseQuery(drawer), start, rows, entrySort(), null,
                SOLR_FIELDS_ENTRIES).getResults();
        if (docs.getNumFound() != drawer.getEntryCount() || docs.size() != rows) {
            throw new IllegalStateException("Card catalog drawer '" + drawer.getId() + "' changed while reading entries at offset " + start);
        }
        return docs;
    }

    private static String entryBaseQuery(CardCatalogDrawer drawer) {
        return "+" + SolrConstants.DOCTYPE + ":" + DocType.ARCHIVE.name() + " +" + SolrConstants.IDDOC_PARENT + ":"
                + quoteSolr(drawer.getIddoc());
    }

    private static List<StringPair> entrySort() {
        return Arrays.asList(new StringPair(FIELD_ARCHIVE_ORDER, "asc"), new StringPair(SolrConstants.IDDOC, "asc"));
    }

    private List<CardCatalogEntry> toEntries(Collection<SolrDocument> docs, boolean withRecords)
            throws PresentationException, IndexUnreachableException {
        Map<String, Entry<String, Boolean>> recordMap = withRecords ? getAssociatedRecords(docs) : Collections.emptyMap();
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

    private Map<String, Entry<String, Boolean>> getAssociatedRecords(Collection<SolrDocument> cards)
            throws PresentationException, IndexUnreachableException {
        List<String> ids = cards.stream()
                .map(doc -> SolrTools.getSingleFieldStringValue(doc, SolrConstants.EAD_NODE_ID))
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        String query = associatedRecordQuery(ids);
        QueryResponse facetResponse = getSearchIndex().searchFacetsAndStatistics(query, null,
                Collections.singletonList(SolrConstants.EAD_NODE_ID), 1, false);
        FacetField facet = facetResponse.getFacetField(SolrConstants.EAD_NODE_ID);
        if (facet == null || facet.getValues() == null) {
            return Collections.emptyMap();
        }
        Set<String> requested = new HashSet<>(ids);
        List<String> uniqueIds = new ArrayList<>(ids.size());
        for (FacetField.Count value : facet.getValues()) {
            if (requested.contains(value.getName())) {
                if (value.getCount() == 1) {
                    uniqueIds.add(value.getName());
                } else {
                    logger.warn("Ambiguous associated record for EAD node ID '{}': {} records; omitting link.",
                            value.getName(), value.getCount());
                }
            }
        }
        if (uniqueIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> uniqueIdSet = new HashSet<>(uniqueIds);
        query = associatedRecordQuery(uniqueIds);
        SolrDocumentList records = getSearchIndex().search(query, 0, uniqueIds.size() + 1,
                Collections.singletonList(new StringPair(SolrConstants.IDDOC, "asc")), null,
                Arrays.asList(SolrConstants.EAD_NODE_ID, SolrConstants.PI, SolrConstants.BOOL_IMAGEAVAILABLE)).getResults();
        if (records.getNumFound() > uniqueIds.size()) {
            logger.warn("Associated records changed while resolving card catalog links; omitting links for this page.");
            return Collections.emptyMap();
        }
        Map<String, Entry<String, Boolean>> ret = new HashMap<>();
        Set<String> duplicateIds = new HashSet<>();
        for (SolrDocument record : records) {
            String id = SolrTools.getSingleFieldStringValue(record, SolrConstants.EAD_NODE_ID);
            String pi = SolrTools.getSingleFieldStringValue(record, SolrConstants.PI);
            if (uniqueIdSet.contains(id) && pi != null) {
                Entry<String, Boolean> previous = ret.putIfAbsent(id, new java.util.AbstractMap.SimpleImmutableEntry<>(pi,
                        SolrTools.getAsBoolean(record.getFieldValue(SolrConstants.BOOL_IMAGEAVAILABLE))));
                if (previous != null) {
                    duplicateIds.add(id);
                }
            }
        }
        duplicateIds.forEach(ret::remove);
        return ret;
    }

    private static String associatedRecordQuery(Collection<String> ids) {
        String values = ids.stream().map(CardCatalogManager::quoteSolr).collect(Collectors.joining(" "));
        return "+" + SolrConstants.DOCTYPE + ":" + DocType.DOCSTRCT.name() + " +" + SolrConstants.PI + ":* +"
                + SolrConstants.EAD_NODE_ID + ":(" + values + ")";
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
        Integer order = getIndexedOrder(doc);
        return order != null ? order : 0;
    }

    private static Integer getIndexedOrder(SolrDocument doc) {
        Object value = doc.getFieldValue(FIELD_ARCHIVE_ORDER);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string) {
            try {
                return Integer.valueOf(string);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Finds the back of each given card record: the first further image in the format of the front. Header strips and other excerpts differ in
     * format and are left out; images without indexed dimensions never count as a back.
     *
     * @param recordPis PIs of card records
     * @return file name of the back image per record PI, only for records that have one
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should return the first further page in the format of the front and skip header strips and pages without dimensions
     * @should not query the index without records
     */
    public Map<String, String> getBackSideFiles(Collection<String> recordPis) throws PresentationException, IndexUnreachableException {
        if (recordPis.isEmpty()) {
            return Collections.emptyMap();
        }
        String pis = recordPis.stream().map(CardCatalogManager::quoteSolr).collect(Collectors.joining(" "));
        String query = "+" + SolrConstants.PI_TOPSTRUCT + ":(" + pis + ") +" + SolrConstants.DOCTYPE + ":" + DocType.PAGE.name() + " +"
                + SolrConstants.ORDER + ":[1 TO " + MAX_CARD_PAGES + "]";
        SolrDocumentList pages = getSearchIndex().search(query, 0, recordPis.size() * MAX_CARD_PAGES, null, null, SOLR_FIELDS_PAGES).getResults();
        Map<String, List<SolrDocument>> pagesByRecord = new HashMap<>();
        for (SolrDocument page : pages) {
            pagesByRecord.computeIfAbsent(SolrTools.getSingleFieldStringValue(page, SolrConstants.PI_TOPSTRUCT), k -> new ArrayList<>()).add(page);
        }
        Map<String, String> ret = new HashMap<>();
        pagesByRecord.forEach((recordPi, recordPages) -> findBackSideFile(recordPages).ifPresent(file -> ret.put(recordPi, file)));
        return ret;
    }

    private static Optional<String> findBackSideFile(List<SolrDocument> pages) {
        List<SolrDocument> sorted = new ArrayList<>(pages);
        sorted.sort(Comparator.comparingInt(page -> getIntValue(page, SolrConstants.ORDER)));
        double front = getAspectRatio(sorted.get(0));
        if (front <= 0) {
            return Optional.empty();
        }
        return sorted.stream()
                .skip(1)
                .filter(page -> Math.abs(getAspectRatio(page) / front - 1) <= BACK_SIDE_RATIO_TOLERANCE)
                .map(page -> SolrTools.getSingleFieldStringValue(page, SolrConstants.FILENAME))
                .filter(StringUtils::isNotBlank)
                .findFirst();
    }

    private static double getAspectRatio(SolrDocument page) {
        int width = getIntValue(page, SolrConstants.WIDTH);
        int height = getIntValue(page, SolrConstants.HEIGHT);
        return width > 0 && height > 0 ? (double) width / height : 0;
    }

    private static int getIntValue(SolrDocument doc, String field) {
        Object value = doc.getFirstValue(field);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private Object catalogLock(String pi) {
        return catalogLocks[(pi.hashCode() & Integer.MAX_VALUE) % CATALOG_LOCK_COUNT];
    }

    private static String quoteSolr(String value) {
        return "\"" + ClientUtils.escapeQueryChars(value) + "\"";
    }

    SolrSearchIndex getSearchIndex() {
        return DataManager.getInstance().getSearchIndex();
    }
}
