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

import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.ArchiveBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("archive")
public class CMSArchiveContent extends CMSContent {

    private static final String COMPONENT_NAME = "archive";

    @Column(name = "archive_id")
    private String archiveId = "";
    @Column(name = "open_in_overlay")
    private boolean openInOverlay = false;

    public CMSArchiveContent() {
        super();
    }

    public CMSArchiveContent(CMSArchiveContent orig) {
        super(orig);
        this.archiveId = orig.archiveId;
        this.openInOverlay = orig.openInOverlay;
    }

    @Override
    public String getBackendComponentName() {
        return COMPONENT_NAME;
    }

    @Override
    public CMSContent copy() {
        return new CMSArchiveContent(this);
    }

    @Override
    public List<File> exportHtmlFragment(String outputFolderPath, String namingScheme) throws IOException, ViewerConfigurationException {
        return Collections.emptyList();
    }

    @Override
    public String handlePageLoad(boolean resetResults, CMSComponent component) throws PresentationException {
        ArchiveBean bean = (ArchiveBean) BeanUtils.getBeanByName("archiveBean", ArchiveBean.class);
        if (bean != null) {
            bean.setCurrentResource(this.getArchiveId());
            bean.initializeArchiveTree();
        } else {
            throw new PresentationException("Could not load archive bean");
        }
        return "";
    }

    @Override
    public boolean isEmpty() {
        return StringUtils.isBlank(this.archiveId);
    }

    @Override
    public String getData(Integer width, Integer height) {
        return this.archiveId;
    }

    public String getArchiveId() {
        return archiveId;
    }

    public void setArchiveId(String archiveId) {
        this.archiveId = archiveId;
    }

    public boolean isOpenInOverlay() {
        return openInOverlay;
    }

    public void setOpenInOverlay(boolean openInOverlay) {
        this.openInOverlay = openInOverlay;
    }

}
