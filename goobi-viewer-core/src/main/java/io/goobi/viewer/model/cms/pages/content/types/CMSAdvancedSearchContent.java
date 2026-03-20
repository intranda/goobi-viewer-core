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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import io.goobi.viewer.model.cms.pages.content.PagedCMSContent;
import io.goobi.viewer.model.search.SearchHelper;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "cms_content_advanced_search")
@DiscriminatorValue("advancedsearch")
public class CMSAdvancedSearchContent extends CMSContent implements PagedCMSContent {

    private static final Logger logger = LogManager.getLogger(CMSAdvancedSearchContent.class);

    private static final String BACKEND_COMPONENT_NAME = "searchadvanced";

    @Column(name = "template_name")
    private String templateName;

    @Column(name = "result_group_name")
    private String resultGroupName;

    public CMSAdvancedSearchContent() {
        super();
    }

    public CMSAdvancedSearchContent(CMSAdvancedSearchContent orig) {
        super(orig);
        this.templateName = orig.templateName;
        this.resultGroupName = orig.resultGroupName;
    }

    @Override
    public String handlePageLoad(boolean resetResults, CMSComponent component) throws PresentationException {
        logger.trace("handlePageLoad");
        if (StringUtils.isBlank(templateName) && StringUtils.isBlank(resultGroupName)) {
            return "";
        }
        if (!isTemplateValid()) {
            logger.warn("Advanced search template '{}' or result group '{}' not found in configuration", templateName, resultGroupName);
            return "";
        }
        try {
            SearchBean searchBean = BeanUtils.getSearchBean();
            if (searchBean != null) {
                searchBean.setActiveSearchType(SearchHelper.SEARCH_TYPE_ADVANCED);
                if (StringUtils.isNotBlank(templateName)) {
                    searchBean.setAdvancedSearchFieldTemplate(templateName);
                }
                if (StringUtils.isNotBlank(resultGroupName)
                        && DataManager.getInstance().getConfiguration().isSearchResultGroupsEnabled()) {
                    searchBean.setActiveResultGroupName(resultGroupName);
                }
            }
        } catch (ContextNotActiveException e) {
            throw new PresentationException("Error initializing advanced search on CMS page", e);
        }
        return "";
    }

    @Override
    public String getBackendComponentName() {
        return BACKEND_COMPONENT_NAME;
    }

    @Override
    public CMSContent copy() {
        return new CMSAdvancedSearchContent(this);
    }

    @Override
    public List<File> exportHtmlFragment(String outputFolderPath, String namingScheme) throws IOException, ViewerConfigurationException {
        return Collections.emptyList();
    }

    @Override
    public String getData(Integer w, Integer h) {
        return "";
    }

    @Override
    public boolean isEmpty() {
        return StringUtils.isBlank(templateName) && StringUtils.isBlank(resultGroupName);
    }

    public boolean isTemplateValid() {
        if (StringUtils.isNotBlank(templateName)
                && !DataManager.getInstance().getConfiguration().getAdvancedSearchTemplateNames().contains(templateName)) {
            return false;
        }
        if (StringUtils.isNotBlank(resultGroupName)
                && DataManager.getInstance()
                        .getConfiguration()
                        .getSearchResultGroups()
                        .stream()
                        .noneMatch(g -> g.getName().equals(resultGroupName))) {
            return false;
        }
        return true;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getResultGroupName() {
        return resultGroupName;
    }

    public void setResultGroupName(String resultGroupName) {
        this.resultGroupName = resultGroupName;
    }
}
