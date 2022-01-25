package io.goobi.viewer.model.cms.widgets.embed;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.widgets.type.AutomaticWidgetType;
import io.goobi.viewer.model.maps.GeoMap;

@Entity
@DiscriminatorValue("AUTOMATIC")
public class CMSSidebarElementAutomatic extends CMSSidebarElement {
    
    @ManyToOne
    @JoinColumn(name = "geomap_id", nullable = false)
    private GeoMap map;

    public CMSSidebarElementAutomatic() {
        super();
    }

    
    public CMSSidebarElementAutomatic(GeoMap map) {
        super(AutomaticWidgetType.WIDGET_CMSGEOMAP);
        this.map = map;
    }
    
    public CMSSidebarElementAutomatic(CMSSidebarElementAutomatic orig, CMSPage owner) {
        super(orig.getContentType(), owner);
        this.map = orig.map;
    }


    public GeoMap getMap() {
        return map;
    }
}
