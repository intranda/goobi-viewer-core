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
import java.util.Collections;
import java.util.List;

import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Optional Solr-filter content for statistics chart components. Stores a single Lucene sub-query that the statistics
 * service appends — wrapped in MUST clauses ({@code +(...)} ) — to its own queries so the chart aggregates only over a
 * subset of the index. The wrapping is a security-relevant invariant: a MUST clause can only narrow the result set, so
 * an admin filter cannot loosen surrounding access restrictions ({@code SearchHelper.getAllSuffixes()}) by smuggling in
 * boolean disjunctions.
 *
 * <p>
 * Single-table inheritance through {@link CMSContent} means the {@code @Table} annotation here is informational only;
 * the {@code filter_query} column physically lives in the {@code cms_content} table. The annotation is kept to match
 * the convention used by sibling content classes ({@link CMSRSSContent}, {@link CMSRecordListContent}, …).
 * </p>
 */
@Entity
@Table(name = "cms_content_statistics_filter")
@DiscriminatorValue("statistics_filter")
public class CMSStatisticsFilterContent extends CMSContent {

    private static final String COMPONENT_NAME = "statisticsFilter";

    @Column(name = "filter_query", columnDefinition = "LONGTEXT")
    private String filterQuery = "";

    /**
     * Used by the publication-centuries chart to switch its Y-axis between linear and logarithmic. Persisted on every
     * {@code CMSStatisticsFilterContent} instance to keep the entity uniform; the languages and top-collections charts
     * persist the field but ignore it (their backend editor doesn't even render the checkbox).
     */
    @Column(name = "logarithmic_scale")
    private boolean logarithmicScale = false;

    public CMSStatisticsFilterContent() {
        super();
    }

    private CMSStatisticsFilterContent(CMSStatisticsFilterContent orig) {
        super(orig);
        this.filterQuery = orig.filterQuery;
        this.logarithmicScale = orig.logarithmicScale;
    }

    @Override
    public String getBackendComponentName() {
        return COMPONENT_NAME;
    }

    /**
     * @return a deep copy with the same {@code filterQuery}
     * @should produce an independent copy with same filter query
     */
    @Override
    public CMSContent copy() {
        return new CMSStatisticsFilterContent(this);
    }

    public String getFilterQuery() {
        return filterQuery;
    }

    public void setFilterQuery(String filterQuery) {
        this.filterQuery = filterQuery;
    }

    public boolean isLogarithmicScale() {
        return logarithmicScale;
    }

    public void setLogarithmicScale(boolean logarithmicScale) {
        this.logarithmicScale = logarithmicScale;
    }

    @Override
    public List<File> exportHtmlFragment(String outputFolderPath, String namingScheme) throws IOException, ViewerConfigurationException {
        return Collections.emptyList();
    }

    @Override
    public String handlePageLoad(boolean resetResults, CMSComponent component) throws PresentationException {
        return "";
    }

    @Override
    public String getData(Integer w, Integer h) {
        return "";
    }

    /**
     * An empty filter is still a valid "no constraint" state, so this content type is never empty; mirrors
     * {@link CMSRSSContent#isEmpty()}.
     *
     * @return always false
     * @should return false even with blank filter query
     */
    @Override
    public boolean isEmpty() {
        return false;
    }
}
