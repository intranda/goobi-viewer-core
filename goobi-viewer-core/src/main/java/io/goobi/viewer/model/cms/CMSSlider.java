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
package io.goobi.viewer.model.cms;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.dao.converter.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Class to persist sliders ("slideshows") created in CMS backend
 * 
 * @author florian
 *
 */
@Entity
@Table(name = "cms_sliders")
public class CMSSlider implements Serializable {

    public static final int MAX_ENTRIES_MIN = 1;
    public static final int MAX_ENTRIES_MAX = 30;
    public static final int MAX_ENTRIES_DEFAULT = 10;

    private static final long serialVersionUID = -3029283417613875012L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slider_id")
    private Long id;
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;
    @Column(name = "name", columnDefinition = "LONGTEXT")
    private String name;
    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;
    @Column(name = "query", columnDefinition = "LONGTEXT")
    private String solrQuery;
    @Column(name = "sort_field", columnDefinition = "LONGTEXT")
    private String sortField;
    @Column(name = "max_entries")
    private int maxEntries = MAX_ENTRIES_DEFAULT;
    @Column(name = "categories", columnDefinition = "LONGTEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> categories = new ArrayList<>();
    @Column(name = "collections", columnDefinition = "LONGTEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> collections = new ArrayList<>();
    @Column(name = "style")
    private String style = "base";

    /**
     * Copy constructor
     * @param o
     */
    public CMSSlider(CMSSlider o) {
        this.id = o.id;
        this.sourceType = o.sourceType;
        this.name = o.name;
        this.description = o.description;
        this.solrQuery = o.solrQuery;
        this.sortField = o.sortField;
        this.maxEntries = o.maxEntries;
        this.categories = new ArrayList<>(o.categories);
        this.collections = new ArrayList<>(o.collections);
        this.style = o.style;
    }

    /**
     * persistence constructor
     */
    public CMSSlider() {

    }

    /**
     * Default constructor. Provides the source type which should be treated as final
     *
     * @param type
     */
    public CMSSlider(SourceType type) {
        this.sourceType = type;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the solrQuery
     */
    public String getSolrQuery() {
        return solrQuery;
    }

    /**
     * @param solrQuery the solrQuery to set
     */
    public void setSolrQuery(String solrQuery) {
        this.solrQuery = solrQuery;
    }

    /**
     * @return the categories
     */
    public List<String> getCategories() {
        return categories;
    }

    /**
     * @param categories the categories to set
     */
    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    /**
     * @return the collections
     */
    public List<String> getCollections() {
        return collections;
    }

    /**
     * @param collections the collections to set
     */
    public void setCollections(List<String> collections) {
        this.collections = collections;
    }

    /**
     * @return the serialversionuid
     */
    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    /**
     * @return the style
     */
    public String getStyle() {
        return style;
    }

    /**
     * @param style the style to set
     */
    public void setStyle(String style) {
        this.style = style;
    }

    /**
     * @return the sourceType
     */
    public SourceType getSourceType() {
        return sourceType;
    }

    /**
     *
     * @return true if either {@link #solrQuery}, {@link #collections} or {@link #categories} is empty, depending on the {@link sourceType}
     */
    public boolean isEmpty() {
        switch (sourceType) {
            case COLLECTIONS:
                return this.collections.isEmpty();
            case PAGES:
            case MEDIA:
                return this.categories.isEmpty();
            case RECORDS:
                return StringUtils.isBlank(this.solrQuery);
            default:
                return true;
        }
    }

    public enum SourceType {
        RECORDS("label__records"), //has solrQuery
        COLLECTIONS("cms_collections"), //has collections
        PAGES("label__cms_pages"), //has categories
        MEDIA("cms_overviewMedia"); //has categories

        private final String label;

        private SourceType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /**
     * @return the sortField
     */
    public String getSortField() {
        return sortField;
    }

    /**
     * @param sortField the sortField to set
     */
    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    /**
     * @param maxEntries the maxEntries to set
     */
    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    /**
     * @return the maxEntries
     */
    public int getMaxEntries() {
        return maxEntries;
    }

    public List<Integer> getMaxEntriesOptions() {
        return IntStream.range(MAX_ENTRIES_MIN, MAX_ENTRIES_MAX + 1).boxed().collect(Collectors.toList());
    }

}
