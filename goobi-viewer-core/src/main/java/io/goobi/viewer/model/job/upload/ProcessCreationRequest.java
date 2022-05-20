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
package io.goobi.viewer.model.job.upload;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProcessCreationRequest {

    private String identifier;
    private String processtitle;
    private String logicalDSType;
    private Integer templateId;
    private String templateName;

    @JsonProperty("metadata")
    private Map<String, String> metadata;

    @JsonProperty("properties")
    private Map<String, String> properties;

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return the processtitle
     */
    public String getProcesstitle() {
        return processtitle;
    }

    /**
     * @param processtitle the processtitle to set
     */
    public void setProcesstitle(String processtitle) {
        this.processtitle = processtitle;
    }

    /**
     * @return the logicalDSType
     */
    public String getLogicalDSType() {
        return logicalDSType;
    }

    /**
     * @param logicalDSType the logicalDSType to set
     */
    public void setLogicalDSType(String logicalDSType) {
        this.logicalDSType = logicalDSType;
    }

    /**
     * @return the templateId
     */
    public Integer getTemplateId() {
        return templateId;
    }

    /**
     * @param templateId the templateId to set
     */
    public void setTemplateId(Integer templateId) {
        this.templateId = templateId;
    }

    /**
     * @return the templateName
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * @param templateName the templateName to set
     */
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    /**
     * @return the metadata
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * @param metadata the metadata to set
     */
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    /**
     * @return the properties
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * @param properties the properties to set
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    /*
    
    <?xml version="1.0"?>
    <record>
     <identifier>PPN1234567</identifier>
     <processtitle>foobar_XYZ_PPN1234567</processtitle>
     <docstruct>Monograph</docstruct>
    
    <metadataList>
     <metadata name="TitleDocMain" value="Lorem Ipsum dolor sit
    amet" />
     <metadata name="Author" value="Mustermann, Max" />
     <metadata name="PublicationYear" value="1984" />
     <metadata name="DocLanguage" value="ger" />
     <metadata name="DocLanguage" value="lat" />
     <metadata name="shelfmarksource" value="SHLF98A2" />
    </metadataList>
    
    <propertyList>
     <property name="OCR" value="Fraktur" />
    </propertyList>
    </record>
    
    
     */

}
