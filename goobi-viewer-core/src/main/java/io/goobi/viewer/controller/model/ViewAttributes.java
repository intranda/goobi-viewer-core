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
package io.goobi.viewer.controller.model;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.IntegerRange;

import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.model.viewer.MimeType;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.ViewManager;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Used to check whether a zoomImageView configuration block should be applied to the image view represented by the given viewManager and pageType
 * Stores {@link PageType} (the type of view, like fullscreen or viewImage), {@link MimeType} of the current page, the number of pages in the record,
 * the main DocStructType of the record and the collections (based on {@link SolrConstants#DC}) of the record.
 */
public class ViewAttributes {

    private final PageType pageType;
    private final MimeType mimeType;
    private final Integer recordPageCount;
    private final String docStructType;
    private final List<String> collections;

    /**
     * create an instance based on individual values. All values may be null if they can be ignored for the check
     * 
     * @param mimeType the media mimeType of the page
     * @param recordPageCount the number of pages in the record
     * @param docStructType The DocStructType of the record
     * @param collections the {@link SolrConstants#DC} collections of the record
     * @param pageType the type of view, one of the values of {@link PageType}
     */
    public ViewAttributes(MimeType mimeType, Integer recordPageCount, String docStructType, List<String> collections, PageType pageType) {
        this.mimeType = mimeType;
        this.recordPageCount = recordPageCount;
        this.docStructType = docStructType;
        this.collections = collections == null ? Collections.emptyList() : collections;
        this.pageType = pageType;
    }

    /**
     * create an instance based on a {@link PhysicalElement page} and {@link StructElement}, along with a {@link PageType}. All values may be null if
     * they can be ignored for the check
     * 
     * @param page the page to consider
     * @param structElement the structural element to consider
     * @param pageType the type of view, one of the values of {@link PageType}
     */
    public ViewAttributes(PhysicalElement page, StructElement structElement, PageType pageType) {
        this.pageType = pageType;
        this.mimeType = Optional.ofNullable(page).map(PhysicalElement::getMediaType).orElse(null);
        this.docStructType = Optional.ofNullable(structElement).map(StructElement::getDocStructType).orElse(null);
        this.recordPageCount = Optional.ofNullable(structElement).map(StructElement::getNumPages).orElse(null);
        this.collections = Optional.ofNullable(structElement).map(StructElement::getCollections).orElse(Collections.emptyList());
    }

    /**
     * Creates an instance from a {@link ViewManager} All values may be null if they can be ignored for the check.
     * 
     * @param viewManager the {@link ViewManager}
     * @param pageType the type of view, one of the values of {@link PageType}
     */
    public ViewAttributes(ViewManager viewManager, PageType pageType) {
        this(Optional.ofNullable(viewManager).map(ViewManager::getCurrentPage).orElse(null),
                Optional.ofNullable(viewManager).map(ViewManager::getTopStructElement).orElse(null),
                pageType);
    }

    public ViewAttributes(PhysicalElement page, PageType pageType) {
        this(page, null, pageType);
    }

    public ViewAttributes(PageType pageType) {
        this(null, null, null, null, pageType);
    }

    public boolean matchesConfiguration(HierarchicalConfiguration<ImmutableNode> conditionConfigNode) {
        List<String> views = conditionConfigNode.getList("view").stream().map(Object::toString).toList();
        List<String> mimeTypes = conditionConfigNode.getList("mimeType").stream().map(Object::toString).toList();
        List<String> docTypes = conditionConfigNode.getList("docType").stream().map(Object::toString).toList();
        List<String> collections = conditionConfigNode.getList("collection").stream().map(Object::toString).toList();
        List<IntegerRange> pageRanges = conditionConfigNode.getList("pageCount").stream().map(Object::toString).map(this::parseIntRange).toList();

        return (views.isEmpty() || (this.pageType != null && views.stream().anyMatch(view -> this.pageType.matches(view))))
                && (mimeTypes.isEmpty() || (this.mimeType != null && mimeTypes.stream().anyMatch(type -> type.equals(this.mimeType.toString()))))
                && (docTypes.isEmpty() || docTypes.contains(this.docStructType))
                && (collections.isEmpty() || !CollectionUtils.intersection(collections, this.collections).isEmpty())
                && (pageRanges.isEmpty() || (this.recordPageCount != null
                        && pageRanges.stream().anyMatch(range -> range.contains(this.recordPageCount))));

    }

    IntegerRange parseIntRange(String input) {
        try {
            return StringTools.parseIntRange(input);
        } catch (IllegalArgumentException e) {
            return IntegerRange.of(0, 0);
        }
    }

    public PageType getPageType() {
        return pageType;
    }

    public MimeType getMimeType() {
        return mimeType;
    }

    public int getRecordPageCount() {
        return recordPageCount;
    }

    public String getDocStructType() {
        return docStructType;
    }

    public List<String> getCollection() {
        return collections;
    }

}
