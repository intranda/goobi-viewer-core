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
import io.goobi.viewer.model.cms.widgets.type.WidgetContentType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Wrapper element for default (static) sidebar widgets. These contain no data since they are entirely described by the xhtml component given by the
 * {@link WidgetContentType content type}
 * 
 * @author florian
 *
 */
@Entity
@DiscriminatorValue("DEFAULT")
public class CMSSidebarElementDefault extends CMSSidebarElement {

    private static final long serialVersionUID = 3406687346141124347L;

    /**
     * Empty constructor for the DAO
     */
    public CMSSidebarElementDefault() {
    }

    /**
     * Default constructor for a {@link WidgetContentType} determining the xhtml component to use and an owning {@link CMSPage}
     * 
     * @param type
     * @param owner
     */
    public CMSSidebarElementDefault(WidgetContentType type, CMSPage owner) {
        super(type, owner);
    }

    public CMSSidebarElementDefault(WidgetContentType orig, CMSPageTemplate owner) {
        super(orig, owner);
    }

    /**
     * Cloning constructor with a CMSPage to set as owner
     * 
     * @param orig
     * @param owner
     */
    public CMSSidebarElementDefault(CMSSidebarElementDefault orig, CMSPage owner) {
        super(orig, owner);
    }

    public CMSSidebarElementDefault(CMSSidebarElementDefault orig, CMSPageTemplate owner) {
        super(orig, owner);
    }

}
