package io.goobi.viewer.model.cms.widgets.embed;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.widgets.type.DefaultWidgetType;
import io.goobi.viewer.model.cms.widgets.type.WidgetContentType;

@Entity
@DiscriminatorValue("DEFAULT")
public class CMSSidebarElementDefault extends CMSSidebarElement {

    public CMSSidebarElementDefault() {
    }

    public CMSSidebarElementDefault(WidgetContentType type, CMSPage owner) {
        super(type, owner);
    }

    public CMSSidebarElementDefault(CMSSidebarElementDefault orig, CMSPage owner) {
        super(orig.getContentType(), owner);
    }
    
}
