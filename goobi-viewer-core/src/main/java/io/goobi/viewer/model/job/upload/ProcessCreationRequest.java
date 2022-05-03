package io.goobi.viewer.model.job.upload;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

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
