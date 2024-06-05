package io.goobi.viewer.model.viewer.record.views;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.goobi.viewer.controller.JsonTools;

public class VisibilityConditionInfo {
    private List<String> requiredFileTypes;
    private String baseMimeType;
    private String accessCondition;
    private List<String> pageTypes;
    private List<String> docTypes;
    private Boolean hasPages;

    public List<String> getRequiredFileTypes() {
        return requiredFileTypes == null ? Collections.emptyList() : requiredFileTypes;
    }

    public void setRequiredFileTypes(List<String> fileTypes) {
        this.requiredFileTypes = fileTypes;
    }

    public String getBaseMimeType() {
        return baseMimeType == null ? "" : baseMimeType;
    }

    public void setBaseMimeType(String baseMimeType) {
        this.baseMimeType = baseMimeType;
    }

    public String getAccessCondition() {
        return accessCondition == null ? "" : accessCondition;
    }

    public void setAccessCondition(String accessCondition) {
        this.accessCondition = accessCondition;
    }

    public List<String> getPageTypes() {
        return pageTypes == null ? Collections.emptyList() : pageTypes;
    }

    public void setPageTypes(List<String> pageTypes) {
        this.pageTypes = pageTypes;
    }

    public Boolean getHasPages() {
        return hasPages;
    }

    public void setHasPages(Boolean hasPages) {
        this.hasPages = hasPages;
    }

    public List<String> getDocTypes() {
        return docTypes == null ? Collections.emptyList() : docTypes;
    }

    public void setDocTypes(List<String> docTypes) {
        this.docTypes = docTypes;
    }

    @Override
    public String toString() {
        try {
            return JsonTools.getAsJson(this);
        } catch (JsonProcessingException e) {
            return super.toString();
        }
    }

}
