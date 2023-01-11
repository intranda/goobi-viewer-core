package io.goobi.viewer.model.cms.pages.content.types;

import io.goobi.viewer.managedbeans.CmsMediaBean;

public class CMSDocumentContent extends CMSMediaContent {

    @Override
    public String getMediaFilter() {
        return CmsMediaBean.getDocumentFilter();
    }
    
    @Override
    public String getMediaTypes() {
        return CmsMediaBean.getDocumentTypes();
    }
}
