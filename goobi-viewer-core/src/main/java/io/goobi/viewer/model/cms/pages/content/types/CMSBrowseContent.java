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

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RedirectException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.cms.itemfunctionality.BrowseFunctionality;
import io.goobi.viewer.model.cms.itemfunctionality.Functionality;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import io.goobi.viewer.model.cms.pages.content.PagedCMSContent;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "cms_content_browse")
@DiscriminatorValue("browse")
public class CMSBrowseContent extends CMSContent implements PagedCMSContent {

    private static final String COMPONENT_NAME = "browse";

    @Column(name = "solr_field", length = 40)
    private String solrField;

    @Transient
    private BrowseFunctionality browse = null;

    @Override
    public String getBackendComponentName() {
        return COMPONENT_NAME;
    }

    public CMSBrowseContent() {
        super();
    }

    private CMSBrowseContent(CMSBrowseContent orig) {
        super(orig);
        this.solrField = orig.solrField;
    }

    public String getSolrField() {
        return solrField;
    }

    public void setSolrField(String solrField) {
        this.solrField = solrField;
    }

    private BrowseFunctionality initBrowse() {
        if (getOwningComponent() != null) {
            BrowseFunctionality b = new BrowseFunctionality(this.solrField);
            b.setPageNo(getCurrentListPage());
            return b;
        }
        return new BrowseFunctionality(this.solrField);
    }

    @Override
    public CMSContent copy() {
        return new CMSBrowseContent(this);
    }

    @Override
    public List<File> exportHtmlFragment(String outputFolderPath, String namingScheme) throws IOException, ViewerConfigurationException {
        return Collections.emptyList();
    }

    @Override
    public String handlePageLoad(boolean resetResults, CMSComponent component) throws PresentationException {
        if (this.browse == null) {
            this.browse = this.initBrowse();
        }

        if (resetResults) {
            browse.reset();
        }
        //filter for subtheme
        if (StringUtils.isNotBlank(getOwningPage().getSubThemeDiscriminatorValue())) {
            browse.setFilter(DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField(),
                    getOwningPage().getSubThemeDiscriminatorValue());
        } else {
            //reset subtheme filter
            browse.setFilter(null, null);
        }
        try {
            browse.searchTerms();
        } catch (RedirectException e) {
            return "pretty:cmsBrowse3";
        } catch (IndexUnreachableException e) {
            throw new PresentationException("Error initializing browsing on page load", e);
        }
        return "";
    }

    @Override
    public String getData(Integer w, Integer h) {
        return "";
    }

    public BrowseFunctionality getBrowse() {
        return browse;
    }

    /**
     * Alias for {@link #getBrowse()}. Used in legacy templates
     * 
     * @return {@link Functionality}
     */
    public Functionality getFunctionality() {
        return getBrowse();
    }

    @Override
    public boolean isEmpty() {
        return StringUtils.isBlank(solrField);
    }

}
