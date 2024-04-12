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
package io.goobi.viewer.model.cms.widgets.embed;

import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.CMSPageTemplate;
import io.goobi.viewer.model.cms.widgets.type.AutomaticWidgetType;
import io.goobi.viewer.model.maps.GeoMap;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;

/**
 * Wrapper for automatic widgets contained in a CMSPage. Currently always bound to a {@link GeoMap} object to display in this widget
 * 
 * @author florian
 *
 */
@Entity
@DiscriminatorValue("AUTOMATIC")
public class CMSSidebarElementAutomatic extends CMSSidebarElement {

    private static final long serialVersionUID = 6660551841281434712L;

    @JoinColumn(name = "geomap_id")
    private GeoMap map;

    /**
     * Empty constructor for the DAO
     */
    public CMSSidebarElementAutomatic() {
        super();
    }

    /**
     * Default constructor for a {@link GeoMap} to display and an owning {@link CMSPage}
     * 
     * @param map
     * @param owner
     */
    public CMSSidebarElementAutomatic(GeoMap map, CMSPage owner) {
        super(AutomaticWidgetType.WIDGET_CMSGEOMAP, owner);
        this.map = map;
    }

    public CMSSidebarElementAutomatic(GeoMap map, CMSPageTemplate owner) {
        super(AutomaticWidgetType.WIDGET_CMSGEOMAP, owner);
        this.map = map;
    }

    /**
     * Cloning constructor assigning the given CMSPage as owner
     * 
     * @param orig
     * @param owner
     */
    public CMSSidebarElementAutomatic(CMSSidebarElementAutomatic orig, CMSPage owner) {
        super(orig, owner);
        this.map = orig.map;
    }

    public CMSSidebarElementAutomatic(CMSSidebarElementAutomatic orig, CMSPageTemplate owner) {
        super(orig, owner);
        this.map = orig.map;
    }

    /**
     * The underlying {@link GeoMap}
     * 
     * @return the map
     */
    public GeoMap getMap() {
        return map;
    }

    /**
     * The title of the underlying geomap
     * 
     * @return the title
     */
    @Override
    public TranslatedText getTitle() {
        return new TranslatedText(map.getTitles(), IPolyglott.getCurrentLocale());
    }
}
