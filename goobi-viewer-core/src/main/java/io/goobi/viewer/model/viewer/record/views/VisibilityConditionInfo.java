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
package io.goobi.viewer.model.viewer.record.views;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.goobi.viewer.controller.JsonTools;

/**
 * Interchange class to pass visibility conditions from the jsf fronend to a backing bean. Takes a pseudo-json object to create a
 * {@link VisibilityCondition} instance
 */
@JsonIgnoreProperties
public class VisibilityConditionInfo {
    private List<String> fileTypes;
    private List<String> sourceFormat;
    private List<String> baseMimeType;
    private String accessCondition;
    private List<String> pageTypes;
    private List<String> docTypes;
    private String numPages;
    private String tocSize;

    public List<String> getRequiredFileTypes() {
        return fileTypes == null ? Collections.emptyList() : fileTypes;
    }

    public void setRequiredFileTypes(List<String> fileTypes) {
        this.fileTypes = fileTypes;
    }

    public List<String> getBaseMimeType() {
        return baseMimeType == null ? Collections.emptyList() : baseMimeType;
    }

    public void setBaseMimeType(List<String> baseMimeType) {
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

    public String getNumPages() {
        return numPages;
    }

    public void setNumPages(String numPages) {
        this.numPages = numPages;
    }

    public List<String> getDocTypes() {
        return docTypes == null ? Collections.emptyList() : docTypes;
    }

    public void setDocTypes(List<String> docTypes) {
        this.docTypes = docTypes;
    }

    public List<String> getSourceFormat() {
        return sourceFormat == null ? Collections.emptyList() : sourceFormat;
    }

    public void setSourceFormat(List<String> sourceFormat) {
        this.sourceFormat = sourceFormat;
    }

    public void setTocSize(String tocSize) {
        this.tocSize = tocSize;
    }

    public String getTocSize() {
        return tocSize;
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
