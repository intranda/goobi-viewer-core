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
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "cms_content_metadata")
@DiscriminatorValue("metadata")
public class CMSMetadataContent extends CMSContent {

    private static final String COMPONENT_NAME = "metadata";
    private static final String DEFAULT_METADATA_FIELD_SELECTION = "URN,PI,MD_TITLE,DOCSTRCT_TOP";

    @Column(name = "metadata_fields")
    private String metadataFields = DEFAULT_METADATA_FIELD_SELECTION;

    public CMSMetadataContent() {
        super();
    }

    private CMSMetadataContent(CMSMetadataContent orig) {
        super(orig);
        this.metadataFields = orig.metadataFields;
    }

    @Override
    public String getBackendComponentName() {
        return COMPONENT_NAME;
    }

    public String getMetadataFields() {
        return metadataFields;
    }

    public void setMetadataFields(String metadataFields) {
        this.metadataFields = metadataFields;
    }

    public List<String> getMetadataFieldsAsList() {
        if (StringUtils.isNotBlank(metadataFields)) {
            return Arrays.stream(metadataFields.split(",")).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public void setMetadataFieldsAsList(List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            this.metadataFields = null;
        } else {
            this.metadataFields = StringUtils.join(fields, ",");
        }
    }

    @Override
    public CMSContent copy() {
        return new CMSMetadataContent(this);
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

    @Override
    public boolean isEmpty() {
        return StringUtils.isBlank(metadataFields);
    }

}
