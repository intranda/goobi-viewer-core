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

import java.util.List;
import java.util.Optional;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.IntegerRange;
import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.model.viewer.MimeType;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.ViewManager;

/**
 * Used to check whether a zoomImageView configuration block should be applied to the image view represented by the given viewManager and pageType
 */
public class ViewAttributes {

    private final PageType pageType;
    private final MimeType mimeType;
    private final Integer recordPageCount;
    private final String docStructType;
    private final String collection;

    public ViewAttributes(PageType pageType, MimeType mimeType, Integer recordPageCount, String docStructType, String collection) {
        this.pageType = pageType;
        this.mimeType = mimeType;
        this.recordPageCount = recordPageCount;
        this.docStructType = docStructType;
        this.collection = collection;
    }

    public ViewAttributes(PhysicalElement page, StructElement structElement, PageType pageType) {
        this.pageType = pageType;
        this.mimeType = Optional.ofNullable(page).map(PhysicalElement::getMediaType).orElse(null);
        this.docStructType = Optional.ofNullable(structElement).map(StructElement::getDocStructType).orElse(null);
        this.collection = Optional.ofNullable(structElement).map(StructElement::getCollection).orElse(null);
        this.recordPageCount = Optional.ofNullable(structElement).map(StructElement::getNumPages).orElse(null);
    }

    public ViewAttributes(ViewManager viewManager, PageType pageType) {
        this(Optional.ofNullable(viewManager).map(ViewManager::getCurrentPage).orElse(null),
                Optional.ofNullable(viewManager).map(ViewManager::getTopStructElement).orElse(null),
                pageType);
    }

    public ViewAttributes(PhysicalElement page, PageType pageType) {
        this(page, null, pageType);
    }

    public ViewAttributes(PageType pageType) {
        this(pageType, null, null, null, null);
    }

    public boolean matchesConfiguration(HierarchicalConfiguration<ImmutableNode> conditionConfigNode) {
        List<String> views = conditionConfigNode.getList("view").stream().map(Object::toString).toList();
        List<String> mimeTypes = conditionConfigNode.getList("mimeType").stream().map(Object::toString).toList();
        List<String> docTypes = conditionConfigNode.getList("docType").stream().map(Object::toString).toList();
        List<String> collections = conditionConfigNode.getList("collection").stream().map(Object::toString).toList();
        List<IntegerRange> pageRanges = conditionConfigNode.getList("pageCount").stream().map(Object::toString).map(this::parseIntRange).toList();

        return (views.isEmpty() || this.pageType == null || views.stream().anyMatch(view -> this.pageType.matches(view))) &&
                (mimeTypes.isEmpty() || this.mimeType == null || mimeTypes.stream().anyMatch(type -> type.equals(this.mimeType.toString()))) &&
                (docTypes.isEmpty() || StringUtils.isBlank(this.docStructType) || docTypes.contains(this.docStructType)) &&
                (collections.isEmpty() || StringUtils.isBlank(this.collection) || collections.contains(this.collection)) &&
                (pageRanges.isEmpty() || this.recordPageCount == null || pageRanges.stream().anyMatch(range -> range.contains(this.recordPageCount)));

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

    public String getCollection() {
        return collection;
    }

}
