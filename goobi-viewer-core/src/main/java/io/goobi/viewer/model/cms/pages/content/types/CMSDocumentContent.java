package io.goobi.viewer.model.cms.pages.content.types;

import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "cms_content_document")
@DiscriminatorValue("document")
public class CMSDocumentContent extends CMSMediaContent {

    public CMSDocumentContent() {
        super();
    }

    public CMSDocumentContent(CMSDocumentContent orig) {
        super(orig);
    }
    
    @Override
    public String getMediaFilter() {
        return CmsMediaBean.getDocumentFilter();
    }
    
    @Override
    public String getMediaTypes() {
        return CmsMediaBean.getDocumentTypes();
    }
    
    @Override
    public CMSContent copy() {
        return new CMSDocumentContent(this);
    }
}
