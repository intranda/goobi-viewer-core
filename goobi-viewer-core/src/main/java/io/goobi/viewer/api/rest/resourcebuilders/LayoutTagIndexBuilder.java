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
package io.goobi.viewer.api.rest.resourcebuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.api.rest.model.LayoutTagIndex;
import io.goobi.viewer.controller.ALTOTools;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.model.alto.AltoHtmlBuilder.AnnotationStyle;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrTools;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Builds the record-wide {@link LayoutTagIndex} from the indexed {@code MD_LAYOUTTAG} values of the page documents.
 * Unlike {@link LayoutTagBuilder} no ALTO file is read: one Solr query answers which tag occurs on which pages. The
 * configured printed-text label is dropped (it anchors margin detection and marks nothing), display names are resolved
 * server-side in the order configured annotation-tag name → translated word-tag type → translated raw label.
 */
public class LayoutTagIndexBuilder {

    /** Message-key prefix of the display names for the semantic word-tag types (underline, strikethrough, mark). */
    static final String TYPE_NAME_KEY_PREFIX = "layout_tags__type_";

    private final String printedLabel;
    private final Map<String, AnnotationStyle> annotationTags;
    private final Map<String, String> wordTagTypes;
    private final Locale locale;

    /**
     * @param config viewer configuration providing the {@code altoLayoutTags} mappings
     * @param locale desired locale for the display names; null resolves to the current locale
     */
    public LayoutTagIndexBuilder(Configuration config, Locale locale) {
        this.printedLabel = config.getAltoLayoutPrintedLabel();
        this.annotationTags = ALTOTools.translateAnnotationTagNames(config.getAltoLayoutAnnotationTags(), locale);
        this.wordTagTypes = config.getAltoLayoutWordTagTypes();
        this.locale = locale;
    }

    /**
     * Loads the index of the given record. Access is checked once on record level ({@code VIEW_FULLTEXT}, the layout
     * tags stem from the ALTO fulltext); without access the index carries no tags.
     *
     * @param topStruct the record's top structure element
     * @param request HTTP servlet request (for the access check)
     * @return the index, with an empty tag list when access is denied or the record has no layout tags
     * @throws PresentationException if the Solr query fails
     * @throws IndexUnreachableException if the index cannot be reached
     * @throws DAOException if the access check fails
     */
    public LayoutTagIndex getLayoutTagIndex(StructElement topStruct, HttpServletRequest request)
            throws PresentationException, IndexUnreachableException, DAOException {
        String pi = topStruct.getPi();
        int pageCount = topStruct.getNumPages();
        boolean access;
        try {
            access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(pi, null, IPrivilegeHolder.PRIV_VIEW_FULLTEXT, request)
                    .isGranted();
        } catch (RecordNotFoundException e) {
            access = false;
        }
        if (!access) {
            return new LayoutTagIndex(pi, pageCount, Collections.emptyList());
        }

        String query = "+" + SolrConstants.PI_TOPSTRUCT + ":" + pi + " +" + SolrConstants.DOCTYPE + ":" + DocType.PAGE.name() + " +"
                + SolrConstants.MD_LAYOUTTAG + ":*";
        SolrDocumentList docs =
                DataManager.getInstance().getSearchIndex().search(query, Arrays.asList(SolrConstants.ORDER, SolrConstants.MD_LAYOUTTAG));
        return build(pi, pageCount, docs);
    }

    /**
     * Pure transformation of the page documents into the index: pages are collected per label (ascending, without
     * duplicates), the printed-text label is skipped, tags are sorted by descending page count, then by label.
     *
     * @param pi record identifier
     * @param pageCount total number of pages of the record
     * @param pageDocs page documents carrying {@code ORDER} and {@code MD_LAYOUTTAG}
     * @return the index
     * @should drop the printed label
     * @should collect pages ascending without duplicates
     * @should sort tags by descending page count then label
     * @should resolve display names in configured order
     * @should deliver style classes for configured annotation tags only
     * @should skip documents without order
     */
    public LayoutTagIndex build(String pi, int pageCount, SolrDocumentList pageDocs) {
        Map<String, SortedSet<Integer>> pagesByLabel = new HashMap<>();
        if (pageDocs != null) {
            for (SolrDocument doc : pageDocs) {
                Integer order = SolrTools.getAsInt(doc.getFieldValue(SolrConstants.ORDER));
                Collection<Object> labels = doc.getFieldValues(SolrConstants.MD_LAYOUTTAG);
                if (order == null || labels == null) {
                    continue;
                }
                for (Object value : labels) {
                    String label = SolrTools.getAsString(value);
                    if (StringUtils.isBlank(label) || label.equals(printedLabel)) {
                        continue;
                    }
                    pagesByLabel.computeIfAbsent(label, k -> new TreeSet<>()).add(order);
                }
            }
        }

        List<LayoutTagIndex.Tag> tags = new ArrayList<>(pagesByLabel.size());
        for (Map.Entry<String, SortedSet<Integer>> entry : pagesByLabel.entrySet()) {
            String label = entry.getKey();
            tags.add(new LayoutTagIndex.Tag(label, displayName(label), styleClass(label), new ArrayList<>(entry.getValue())));
        }
        tags.sort(Comparator.comparingInt((LayoutTagIndex.Tag t) -> t.getPages().size()).reversed().thenComparing(LayoutTagIndex.Tag::getLabel));
        return new LayoutTagIndex(pi, pageCount, tags);
    }

    /**
     * Resolves the display name of a label: the configured (translated) annotation-tag name, else the translated
     * semantic word-tag type, else the translated raw label (the label itself when no message key exists).
     *
     * @param label raw layout-tag LABEL
     * @return display name, never null
     */
    String displayName(String label) {
        AnnotationStyle style = annotationTags.get(label);
        if (style != null && StringUtils.isNotBlank(style.name())) {
            return style.name();
        }
        String type = wordTagTypes.get(label);
        if (StringUtils.isNotBlank(type)) {
            String typeName = ViewerResourceBundle.getTranslation(TYPE_NAME_KEY_PREFIX + type, locale, true, false, false, true);
            if (StringUtils.isNotBlank(typeName)) {
                return typeName;
            }
        }
        return ViewerResourceBundle.getTranslation(label, locale);
    }

    /**
     * @param label raw layout-tag LABEL
     * @return the configured CSS class of an annotation tag, or null
     */
    String styleClass(String label) {
        AnnotationStyle style = annotationTags.get(label);
        return style != null && StringUtils.isNotBlank(style.styleClass()) ? style.styleClass() : null;
    }
}
