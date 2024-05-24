package io.goobi.viewer.managedbeans;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.goobi.viewer.controller.JsonTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.viewer.record.views.VisibilityCondition;

@Named
@SessionScoped
public class SidebarBean implements Serializable {

    private static final long serialVersionUID = 6193053985791285569L;
    @Inject
    protected ActiveDocumentBean activeDocumentBean;
    @Inject
    protected NavigationHelper navigationHelper;
    @Inject
    private HttpServletRequest httpRequest;

    public boolean isVisibleForRecord(VisibilityCondition condition) throws IndexUnreachableException, DAOException, RecordNotFoundException {
        if (activeDocumentBean != null && activeDocumentBean.isRecordLoaded()) {
            return condition.matchesRecord(navigationHelper.getCurrentPageType(), activeDocumentBean.getViewManager(), httpRequest);
        }
        return false;
    }

    public boolean isVisibleForPage(VisibilityCondition condition) throws IndexUnreachableException, DAOException, RecordNotFoundException {
        if (activeDocumentBean != null && activeDocumentBean.isRecordLoaded()) {
            return condition.matchesPage(navigationHelper.getCurrentPageType(), activeDocumentBean.getViewManager().getCurrentPage(), httpRequest);
        }
        return false;
    }

    public boolean checkVisibility(String json) throws IOException {
        json = json.replace("`", "\"");
        VisibilityConditionInfo info = JsonTools.getAsObject(json, VisibilityConditionInfo.class);
        return true;
    }

    public static class VisibilityConditionInfo {
        private List<String> fileTypes;
        private String baseMimeType;
        private String accessCondition;
        private List<String> pageTypes;
        private Boolean hasPages;

        public List<String> getFileTypes() {
            return fileTypes;
        }

        public void setFileTypes(List<String> fileTypes) {
            this.fileTypes = fileTypes;
        }

        public String getBaseMimeType() {
            return baseMimeType;
        }

        public void setBaseMimeType(String baseMimeType) {
            this.baseMimeType = baseMimeType;
        }

        public String getAccessCondition() {
            return accessCondition;
        }

        public void setAccessCondition(String accessCondition) {
            this.accessCondition = accessCondition;
        }

        public List<String> getPageTypes() {
            return pageTypes;
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

        @Override
        public String toString() {
            try {
                return JsonTools.getAsJson(this);
            } catch (JsonProcessingException e) {
                return super.toString();
            }
        }

    }

}
