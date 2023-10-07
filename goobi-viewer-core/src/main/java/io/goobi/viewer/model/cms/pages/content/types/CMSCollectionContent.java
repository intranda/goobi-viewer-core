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
package io.goobi.viewer.model.cms.pages.content.types;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.CollectionViewBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import io.goobi.viewer.model.search.CollectionResult;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.collections.CollectionView;
import io.goobi.viewer.model.viewer.collections.Sorting;
import io.goobi.viewer.solr.SolrConstants;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "cms_content_collection")
@DiscriminatorValue("collection")
public class CMSCollectionContent extends CMSContent {

    private static final String COMPONENT_NAME = "collection";

    @Column(name = "solr_field", length=40)
    private String solrField = SolrConstants.DC;
    @Column(name = "collection_name")
    private String collectionName = ""; //if black, all collections of the solrField are included
    @Column(name = "sorting", length=20)
    @Enumerated(EnumType.STRING)
    private Sorting sorting = Sorting.alphanumeric;
    @Column(name = "filter_query")
    private String filterQuery = "";
    /** Name of SOLR field by which to group results of the collection */
    @Column(name = "grouping_field", length=40)
    private String groupingField = "";
    /** Comma separated list of collection names to ignore for display */
    @Column(name = "ignore_collections", columnDefinition = "LONGTEXT")
    private String ignoreCollections = null;
    @Column(name = "open_expanded")
    private boolean openExpanded = false;

    @Transient
    private Map<String, CollectionResult> dcStrings = null;

    public CMSCollectionContent() {
        super();
    }

    private CMSCollectionContent(CMSCollectionContent orig) {
        super(orig);
        this.solrField = orig.solrField;
        this.collectionName = orig.collectionName;
        this.sorting = orig.sorting;
        this.filterQuery = orig.filterQuery;
        this.groupingField = orig.groupingField;
        this.ignoreCollections = orig.ignoreCollections;
    }

    public String getSolrField() {
        return solrField;
    }

    public void setSolrField(String solrField) {
        this.solrField = solrField;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getFilterQuery() {
        return filterQuery;
    }

    public void setFilterQuery(String filterQuery) {
        this.filterQuery = filterQuery;
    }

    public Sorting getSorting() {
        return sorting;
    }

    public void setSorting(Sorting sorting) {
        this.sorting = sorting;
    }

    public String getGroupingField() {
        return groupingField;
    }

    public void setGroupingField(String groupingSolrField) {
        this.groupingField = groupingSolrField;
    }

    public String getIgnoreCollections() {
        return ignoreCollections;
    }

    public void setIgnoreCollections(String ignoreCollections) {
        this.ignoreCollections = ignoreCollections;
    }

    public boolean isOpenExpanded() {
        return openExpanded;
    }

    public void setOpenExpanded(boolean openExpanded) {
        this.openExpanded = openExpanded;
    }

    /**
     * <p>
     * getIgnoreCollectionsAsList.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getIgnoreCollectionsAsList() {
        if (StringUtils.isNotBlank(ignoreCollections)) {
            return Arrays.stream(ignoreCollections.split(",")).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public String getIgnoreCollectionsAsJsonArray() {
        if (StringUtils.isNotBlank(ignoreCollections)) {
            String[] collections = ignoreCollections.split(",");
            JSONArray array = new JSONArray();
            for (String string : collections) {
                array.put(string);
            }
            return array.toString();
        }
        return "[]";
    }

    /**
     * <p>
     * setIgnoreCollectionsAsList.
     * </p>
     *
     * @param toIgnore a {@link java.util.List} object.
     */
    public void setIgnoreCollectionsAsList(List<String> toIgnore) {
        if (toIgnore == null || toIgnore.isEmpty()) {
            this.ignoreCollections = null;
        } else {
            this.ignoreCollections = StringUtils.join(toIgnore, ",");
        }
        this.resetCollection();
    }

    /**
     * remove cached CollectionView for this content from {@link CollectionViewBean}
     */
    private void resetCollection() {
        BeanUtils.getCollectionViewBean().removeCollection(this);
    }

    @Override
    public String getBackendComponentName() {
        return COMPONENT_NAME;
    }

    @Override
    public CMSContent copy() {
        return new CMSCollectionContent(this);
    }

    @Override
    public List<File> exportHtmlFragment(String outputFolderPath, String namingScheme) throws IOException, ViewerConfigurationException {
        return Collections.emptyList();
    }

    /**
     * call {@link CollectionView#reset(boolean) CollectionView#reset(true)} on the CollectionView stored in the cmsBean for this item, if any
     */
    @Override
    public String handlePageLoad(boolean resetResults, CMSComponent component) {
        BeanUtils.getCollectionViewBean().getCollectionIfStored(this).ifPresent(c -> c.reset(true));
        return "";
    }

    /**
     * @param subThemeDiscriminatorValue
     * @return
     */
    public String getCombinedFilterQuery() {
        String subThemeDiscriminatorValue = getOwningPage().getSubThemeDiscriminatorValue();
        if (StringUtils.isNoneBlank(subThemeDiscriminatorValue, this.filterQuery)) {
            return "(" + this.filterQuery + ") AND " + DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField() + ":"
                    + subThemeDiscriminatorValue;
        } else if (StringUtils.isNotBlank(subThemeDiscriminatorValue)) {
            return DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField() + ":" + subThemeDiscriminatorValue;
        } else {
            return this.filterQuery;
        }
    }

    /**
     * Querys solr for a list of all values of the set collectionField which my serve as a collection
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<String> getPossibleBaseCollectionList() throws IndexUnreachableException {
        if (StringUtils.isBlank(solrField)) {
            return Collections.singletonList("");
        }
        Map<String, CollectionResult> dcStringMap = getColletionMap();
        List<String> list = new ArrayList<>(dcStringMap.keySet());
        list.add(0, "");
        Collections.sort(list);
        return list;
    }

    /**
     * @return
     * @throws IndexUnreachableException
     */
    public Map<String, CollectionResult> getColletionMap() throws IndexUnreachableException {
        if (dcStrings == null) {
            dcStrings =
                    SearchHelper.findAllCollectionsFromField(solrField, null, getFilterQuery(), true, true,
                            DataManager.getInstance().getConfiguration().getCollectionSplittingChar(solrField));
        }
        return dcStrings;
    }

    /**
     * Alias for {@link #getCollectionName()}. Used in legacy templates
     * 
     * @return
     */
    public String getBaseCollection() {
        return getCollectionName();
    }

    /**
     * Alias for {@link #getSolrField()}. Used in legacy templates
     * 
     * @return
     */
    public String getCollectionField() {
        return getSolrField();
    }

    /**
     * Alias for {@link #getGroupingField()}. Used in legacy templates
     * 
     * @return
     */
    public String getGroupBy() {
        return getGroupingField();
    }

    @Override
    public String getData(Integer w, Integer h) {
        return "";
    }

    @Override
    public boolean isEmpty() {
        return StringUtils.isBlank(solrField);
    }

}
