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
package io.goobi.viewer.model.citation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import de.undercouch.citeproc.csl.CSLType;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.citation.CitationLink.CitationLinkLevel;
import io.goobi.viewer.model.citation.CitationLink.CitationLinkType;
import io.goobi.viewer.model.variables.VariableReplacer;
import io.goobi.viewer.model.viewer.ViewManager;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrTools;

public final class CitationTools {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(CitationTools.class);

    /** Private constructor. */
    private CitationTools() {
    }

    /**
     * 
     * @param allLinks
     * @param level
     * @param viewManager
     * @return Subset of allLinks that corresponds given level, populated with values
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @should throw IllegalArgumentException if allLinks null
     * @should throw IllegalArgumentException if level null
     * @should throw IllegalArgumentException if viewManager null
     * @should preserve internal links
     * @should set correct value for record type
     * @should set correct value for docstruct type
     * @should set correct value for image type
     * @should fall back to topstruct value correctly
     * @should apply pattern correctly
     */
    public static List<CitationLink> generateCitationLinksForLevel(List<CitationLink> allLinks, CitationLinkLevel level, ViewManager viewManager)
            throws PresentationException, IndexUnreachableException {
        if (allLinks == null) {
            throw new IllegalArgumentException("allLinks may not be null");
        }
        if (level == null) {
            throw new IllegalArgumentException("level may not be null");
        }
        if (viewManager == null) {
            throw new IllegalArgumentException("viewManager may not be null");
        }

        // Collect relevant configurations and required index fields
        List<CitationLink> ret = new ArrayList<>();
        Set<String> indexFields = new HashSet<>();
        for (CitationLink link : allLinks) {
            if (level.equals(link.getLevel())) {
                ret.add(link);
                if (CitationLinkType.URL.equals(link.getType()) && StringUtils.isNotBlank(link.getField())) {
                    indexFields.add(link.getField());
                }
            }
        }

        // Populate values
        String query = null;
        switch (level) {
            case RECORD:
                query = SolrConstants.PI + ":" + viewManager.getPi();
                break;
            case DOCSTRUCT:
                query = "+" + SolrConstants.IDDOC + ":" + viewManager.getCurrentStructElement().getLuceneId();
                break;
            case IMAGE:
                int order = viewManager.getCurrentImageOrder() > 0 ? viewManager.getCurrentImageOrder() : 1;
                query = "+" + SolrConstants.PI_TOPSTRUCT + ":" + viewManager.getPi() + " +" + SolrConstants.ORDER + ":"
                        + order + " +" + SolrConstants.DOCTYPE + ":" + DocType.PAGE.name();
                break;
            default:
                break;
        }
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, new ArrayList<>(indexFields));
        SolrDocument topDoc = null;
        if (doc == null) {
            logger.warn("Solr doc not found: {}", query);
            return Collections.emptyList();
        }

        VariableReplacer vr = new VariableReplacer(viewManager);

        for (CitationLink link : ret) {
            if (!CitationLinkType.URL.equals(link.getType())) {
                continue;
            }

            // logger.trace("Loading value: {}/{}", level, link.getField()); //NOSONAR Debug
            String value = "";
            if (doc.get(link.getField()) != null) {
                value = SolrTools.getAsString(doc.get(link.getField()));
            } else if (link.isTopstructValueFallback() && !CitationLinkLevel.RECORD.equals(level)) {
                query = SolrConstants.PI + ":" + viewManager.getPi();
                topDoc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, Collections.singletonList(link.getField()));
                if (topDoc != null && topDoc.get(link.getField()) != null) {
                    value = SolrTools.getAsString(topDoc.get(link.getField()));
                }
            }
            if (StringUtils.isNotBlank(value)) {
                vr.addReplacement("value", value);
                vr.addReplacement("page", String.valueOf(viewManager.getCurrentImageOrder()));
                String pattern = Optional.ofNullable(link.getPattern()).filter(StringUtils::isNotBlank).orElse("{value}");
                link.setValue(vr.replaceFirst(pattern));
            }
        }

        return ret;
    }

    /**
     *
     * @param docstruct
     * @param topstruct
     * @return CLSType for the given docstruct; default value if none mapped
     * @should return correct type
     */
    public static CSLType getCSLTypeForDocstrct(String docstruct, String topstruct) {
        if (docstruct == null) {
            return CSLType.ARTICLE;
        }

        // Use configured mapping, if available
        Map<String, String> docstructMap = DataManager.getInstance().getConfiguration().getSidebarWidgetCitationCitationRecommendationDocstructMapping();
        if (docstructMap != null && docstructMap.containsKey(docstruct)) {
            CSLType ret = CSLType.fromString(docstructMap.get(docstruct));
            logger.trace("Mapped CSL type: {}", ret.name());
            return ret;
        }

        // Hardcoded mapping
        switch (docstruct.toLowerCase()) {
            case "monograph":
            case "volume":
                return CSLType.BOOK;
            case "manuscript":
                return CSLType.MANUSCRIPT;
            case "map":
            case "singlemap":
                return CSLType.MAP;
            case "article":
                if (topstruct != null) {
                    if (topstruct.toLowerCase().startsWith("newspaper")) {
                        return CSLType.ARTICLE_NEWSPAPER;
                    }
                    if ("periodicalvolume".equalsIgnoreCase(topstruct)) {
                        return CSLType.ARTICLE_JOURNAL;
                    }
                }
                return CSLType.ARTICLE;
            case "chapter":
                return CSLType.CHAPTER;
            default:
                return CSLType.ARTICLE;
        }
    }
}
