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
import io.goobi.viewer.model.cms.widgets.CustomSidebarWidget;
import io.goobi.viewer.model.translations.TranslatedText;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;

/**
 * Wrapper element for custom (user created) sidebar widgets. Linked to an instance of {@link CustomSidebarWidget} providing the data to display
 * 
 * @author florian
 *
 */
@Entity
@DiscriminatorValue("CUSTOM")
public class CMSSidebarElementCustom extends CMSSidebarElement {

    private static final long serialVersionUID = -8790669781907905873L;

    @JoinColumn(name = "custom_widget_id")
    private CustomSidebarWidget widget;

    /**
     * Empty contructor for the DAO
     */
    public CMSSidebarElementCustom() {
    }

    /**
     * Default constructor for a {@link CustomSidebarWidget} providing the data and an owning {@link CMSPage}
     * 
     * @param widget
     * @param owner
     */
    public CMSSidebarElementCustom(CustomSidebarWidget widget, CMSPage owner) {
        super(widget.getType(), owner);
        this.widget = widget;
    }

    public CMSSidebarElementCustom(CustomSidebarWidget widget, CMSPageTemplate owner) {
        super(widget.getType(), owner);
        this.widget = widget;
    }

    /**
     * Cloning constructor with a CMSPage to set as owner
     * 
     * @param orig
     * @param owner
     */
    public CMSSidebarElementCustom(CMSSidebarElementCustom orig, CMSPage owner) {
        super(orig, owner);
        this.widget = orig.widget;
    }

    public CMSSidebarElementCustom(CMSSidebarElementCustom orig, CMSPageTemplate owner) {
        super(orig, owner);
        this.widget = orig.widget;
    }

    /**
     * Get the {@link CustomSidebarWidget} providing the data
     * 
     * @return the widget
     */
    public CustomSidebarWidget getWidget() {
        return widget;
    }

    /**
     *
     * @param widget
     */
    public void setWidget(CustomSidebarWidget widget) {
        this.widget = widget;
    }

    /**
     * Get the title of the underlying widget
     * 
     * @return the title
     */
    @Override
    public TranslatedText getTitle() {
        return widget.getTitle();
    }
}
