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
package io.goobi.viewer.model.termbrowsing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;

/**
 * <p>
 * BrowsingMenuFieldConfig class.
 * </p>
 */
public class BrowsingMenuFieldConfig implements Serializable {

    private static final long serialVersionUID = 3986773493941416989L;

    private final String field;
    private final String sortField;
    private final List<String> filterQueries = new ArrayList<>(3);
    private boolean translate;
    private boolean alwaysApplyFilter;
    private boolean skipInWidget;

    /**
     * Constructor.
     *
     * @param field a {@link java.lang.String} object.
     * @param sortField a {@link java.lang.String} object.
     * @param filterQuery a {@link java.lang.String} object.
     * @should add doctype filter if field MD or MD2
     */
    public BrowsingMenuFieldConfig(String field, String sortField, String filterQuery) {
        this.field = field;
        this.sortField = sortField;
        if (StringUtils.isNotEmpty(filterQuery)) {
            filterQueries.add(filterQuery);
        }

        addDoctypeFilterQuery();
    }

    /**
     * Adds a filter for DOCTYPE:DOCSTRCT for certain field types to reduce the number of returned docs.
     *
     * @should add doctype filter if field MD or MD2
     * @should add doctype filter if field DC
     * @should add doctype filter if field DOCSTRCT
     * @should not add doctype filter if field NE
     */
    void addDoctypeFilterQuery() {
        if (field == null) {
            return;
        }

        switch (field) {
            case SolrConstants.DC, SolrConstants.DOCSTRCT:
                filterQueries.add("+" + SolrConstants.DOCTYPE + ":" + DocType.DOCSTRCT.name());
                break;
            default:
                if (field.startsWith("MD_") || field.startsWith("MD2_")) {
                    filterQueries.add("+" + SolrConstants.DOCTYPE + ":" + DocType.DOCSTRCT.name());
                }
                break;
        }
    }

    /**
     * <p>
     * Getter for the field <code>field</code>.
     * </p>
     *
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * 
     * @param language
     * @return Language specific variant of field if it contains a placeholder; otherwise unaltered field
     */
    public String getFieldForLanguage(String language) {
        if (field != null && field.endsWith(SolrConstants.MIDFIX_LANG + "{}")) {
            return field.replace("{}", language.toUpperCase());
        }
        return field;
    }

    /**
     * <p>
     * Getter for the field <code>sortField</code>.
     * </p>
     *
     * @return the sortField
     */
    public String getSortField() {
        return sortField;
    }

    /**
     * <p>
     * Getter for the field <code>filterQueries</code>.
     * </p>
     *
     * @return the filterQueries
     */
    public List<String> getFilterQueries() {
        return filterQueries;
    }

    /**
     * @return the translate
     */
    public boolean isTranslate() {
        return translate;
    }

    /**
     * @param translate the translate to set
     * @return this
     */
    public BrowsingMenuFieldConfig setTranslate(boolean translate) {
        this.translate = translate;
        return this;
    }

    /**
     * @return the alwaysApplyFilter
     */
    public boolean isAlwaysApplyFilter() {
        return alwaysApplyFilter;
    }

    /**
     * @param alwaysApplyFilter the alwaysApplyFilter to set
     * @return this
     */
    public BrowsingMenuFieldConfig setAlwaysApplyFilter(boolean alwaysApplyFilter) {
        this.alwaysApplyFilter = alwaysApplyFilter;
        return this;
    }

    /**
     * @return the skipInWidget
     */
    public boolean isSkipInWidget() {
        return skipInWidget;
    }

    /**
     * @param skipInWidget the skipInWidget to set
     * @return this;
     */
    public BrowsingMenuFieldConfig setSkipInWidget(boolean skipInWidget) {
        this.skipInWidget = skipInWidget;
        return this;
    }

    /**
     *
     * @return true if filterQueries contains SearchHelper.ALL_RECORDS_QUERY; false otherwise
     */
    public boolean isRecordsAndAnchorsOnly() {
        return filterQueries.contains(SearchHelper.ALL_RECORDS_QUERY);
    }

    /**
     *
     * @param recordsAndAnchorsOnly
     * @return this
     * @should create filter query correctly
     */
    public BrowsingMenuFieldConfig setRecordsAndAnchorsOnly(boolean recordsAndAnchorsOnly) {
        if (recordsAndAnchorsOnly) {
            filterQueries.add(SearchHelper.ALL_RECORDS_QUERY);
        }

        return this;
    }
}
