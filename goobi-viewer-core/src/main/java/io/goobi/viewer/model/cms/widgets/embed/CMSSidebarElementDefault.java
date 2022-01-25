package io.goobi.viewer.model.cms.widgets.embed;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.widgets.type.DefaultWidgetType;

@Entity
@DiscriminatorValue("DEFAULT")
public class CMSSidebarElementDefault extends CMSSidebarElement {

    public CMSSidebarElementDefault() {
    }
    
    public CMSSidebarElementDefault(DefaultWidgetType type) {
        super(type);
    }

    public CMSSidebarElementDefault(CMSSidebarElementDefault orig, CMSPage owner) {
        super(orig.getContentType(), owner);
    }
    
}
