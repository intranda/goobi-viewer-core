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
package io.goobi.viewer.dao.update;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSSlider;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSBrowseContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSCollectionContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSDocumentContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSGeomapContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSImageListContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSMediaContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSMediumTextContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSMetadataContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSPageListContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSRSSContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSRecordListContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSSearchContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSShortTextContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSSliderContent;
import io.goobi.viewer.model.maps.GeoMap;
import io.goobi.viewer.model.viewer.collections.Sorting;

public class CMSContentConverter {

    private final IDAO dao;

    public CMSContentConverter(IDAO dao) {
        this.dao = dao;
    }

    public CMSShortTextContent createShortTextContent(String language, Map<String, Object> legacyItem, Optional<CMSContent> orig) {
        String text = (String) legacyItem.get("html_fragment");
        CMSShortTextContent content =
                orig.filter(CMSShortTextContent.class::isInstance).map(CMSShortTextContent.class::cast).orElse(new CMSShortTextContent());
        content.getText().setValue(text, language);
        return content;
    }

    public CMSMediumTextContent createMediumTextContent(String language, Map<String, Object> legacyItem, Optional<CMSContent> orig) {
        String text = (String) legacyItem.get("html_fragment");
        CMSMediumTextContent content =
                orig.filter(CMSMediumTextContent.class::isInstance).map(CMSMediumTextContent.class::cast).orElse(new CMSMediumTextContent());
        content.getText().setValue(text, language);
        return content;
    }

    public CMSMediaContent createMediaContent(Map<String, Object> legacyItem) throws DAOException {
        Long mediaItemId = (Long) legacyItem.get("media_item_id");
        if (mediaItemId != null) {
            CMSMediaItem media = dao.getCMSMediaItem(mediaItemId);
            if (media != null) {
                CMSMediaContent content;
                if (media.getFileName().matches("(?i).*\\.(pdf|html)")) {
                    content = new CMSDocumentContent();
                } else {
                    content = new CMSMediaContent();
                }
                content.setMediaItem(media);
                return content;
            }
        }
        return null;
    }

    public CMSBrowseContent createBrowseContent(Map<String, Object> legacyItem) {
        String collectionField = (String) legacyItem.get("collection_field");
        CMSBrowseContent content = new CMSBrowseContent();
        content.setSolrField(collectionField);
        return content;
    }

    public CMSCollectionContent createCollectionContent(Map<String, Object> legacyItem) {
        String collectionField = (String) legacyItem.get("collection_field");
        String baseCollection = (String) legacyItem.get("base_collection");
        String ignoreCollections = (String) legacyItem.get("ignore_collections");
        Boolean openExpanded = (Boolean) legacyItem.get("collection_open_expanded");
        String groupByField = (String) legacyItem.get("group_by");
        String sorting = (String) legacyItem.get("sorting");
        String filterQuery = (String) legacyItem.get("search_prefix");
        CMSCollectionContent content = new CMSCollectionContent();
        content.setSolrField(collectionField);
        content.setCollectionName(baseCollection);
        content.setFilterQuery(filterQuery);
        content.setGroupingField(groupByField);
        content.setIgnoreCollections(ignoreCollections);
        content.setOpenExpanded(openExpanded);
        if (sorting != null) {
            content.setSorting(Sorting.valueOf(sorting));
        }

        return content;
    }

    public CMSGeomapContent createGeomapContent(Map<String, Object> legacyItem) throws DAOException {
        Long geomapId = (Long) legacyItem.get("geomap_id");
        CMSGeomapContent content = new CMSGeomapContent();
        if (geomapId != null) {
            GeoMap map = dao.getGeoMap(geomapId);
            if (map != null) {
                content.setMap(map);
            }
        }
        return content;
    }

    public CMSImageListContent createImageListContent(Map<String, Object> legacyItem) throws DAOException {
        Long itemId = (Long) legacyItem.get("cms_content_item_id");
        Integer itemsPerView = (Integer) legacyItem.get("elements_per_page");
        Integer importantItemsPerView = (Integer) legacyItem.get("important_count");

        List<CMSCategory> categories = getCategories(itemId);

        CMSImageListContent collection = new CMSImageListContent();
        collection.setImagesPerView(itemsPerView);
        collection.setImportantImagesPerView(importantItemsPerView);
        categories.forEach(collection::addCategory);

        return collection;
    }

    public CMSRecordListContent createRecordListContent(Map<String, Object> legacyItem) {
        String solrQuery = (String) legacyItem.get("solr_query");
        String solrSortFields = (String) legacyItem.get("solr_sort_fields");
        String groupField = (String) legacyItem.get("group_by");
        Boolean noAggregation = (Boolean) legacyItem.get("no_search_aggregation");
        Integer itemsPerView = (Integer) legacyItem.get("elements_per_page");

        CMSRecordListContent content = new CMSRecordListContent();
        content.setSolrQuery(solrQuery);
        content.setSortField(solrSortFields);
        content.setGroupingField(groupField);
        content.setIncludeStructureElements(noAggregation != null && noAggregation);
        content.setElementsPerPage(itemsPerView);

        return content;
    }

    public CMSSearchContent createSearchContent(Map<String, Object> legacyItem) {
        Boolean displayEmptySearchResults = (Boolean) legacyItem.get("displayEmptySearchResults");
        String solrQuery = (String) legacyItem.get("search_prefix");

        CMSSearchContent content = new CMSSearchContent();
        content.setDisplayEmptySearchResults(displayEmptySearchResults);
        content.setSearchPrefix(solrQuery);

        return content;
    }

    public CMSSliderContent createSliderContent(Map<String, Object> legacyItem) throws DAOException {

        Long sliderId = (Long) legacyItem.get("slider_id");
        CMSSliderContent content = new CMSSliderContent();
        if (sliderId != null) {
            CMSSlider slider = dao.getSlider(sliderId);
            if (slider != null) {
                content.setSlider(slider);
            }
        }
        return content;
    }

    public CMSContent createRSSContent(Map<String, Object> legacyItem) {
        Integer itemsPerView = (Integer) legacyItem.get("elements_per_page");
        CMSRSSContent content = new CMSRSSContent();
        content.setItemsPerView(itemsPerView);
        return content;
    }

    public CMSContent createPageListContent(Map<String, Object> legacyItem) throws DAOException {
        Long itemId = (Long) legacyItem.get("cms_content_item_id");
        Integer itemsPerView = (Integer) legacyItem.get("elements_per_page");
        List<CMSCategory> categories = getCategories(itemId);

        CMSPageListContent content = new CMSPageListContent();
        categories.forEach(content::addCategory);
        return content;
    }

    @SuppressWarnings("unchecked")
    private List<CMSCategory> getCategories(Long itemId) throws DAOException {
        List<Long> categoryIds =
                dao.getNativeQueryResults("SELECT category_id FROM cms_content_item_cms_categories WHERE content_item_id = " + itemId);
        List<CMSCategory> categories = new ArrayList<>(categoryIds.size());
        for (Long id : categoryIds) {
            CMSCategory category = dao.getCategory(id);
            categories.add(category);
        }
        return categories;
    }

    public CMSContent createMetadataContent(Map<String, Object> legacyItem) {
        String metadataFields = (String) legacyItem.get("metadataFields");
        CMSMetadataContent content = new CMSMetadataContent();
        content.setMetadataFields(metadataFields);
        return content;
    }

}
