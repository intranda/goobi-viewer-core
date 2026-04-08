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
package io.goobi.viewer.model.cms;

import io.goobi.viewer.model.cms.pages.CMSPage;

/**
 *
 * A {@link io.goobi.viewer.model.cms.CMSNavigationItem} which can be selected and deselected. Used for selecting available NavigationItems
 * to be used in the navigation menu
 *
 * @author Florian Alpers
 */
public class SelectableNavigationItem extends CMSNavigationItem {

    private static final long serialVersionUID = -8569951374947384020L;

    private boolean selected = false;

    /**
     * Creates a new SelectableNavigationItem instance.
     */
    public SelectableNavigationItem() {
        super();
    }

    /**
     * Creates a new SelectableNavigationItem instance.
     *
     * @param original navigation item to copy state from
     */
    public SelectableNavigationItem(CMSNavigationItem original) {
        super(original);
    }

    /**
     * Creates a new SelectableNavigationItem instance.
     *
     * @param cmsPage CMS page to wrap as a navigation item
     */
    public SelectableNavigationItem(CMSPage cmsPage) {
        super(cmsPage);
    }

    /**
     * Creates a new SelectableNavigationItem instance.
     *
     * @param targetUrl navigation target URL
     * @param label display label for the menu item
     */
    public SelectableNavigationItem(String targetUrl, String label) {
        super(targetUrl, label);
    }

    /**
     * Setter for the field <code>selected</code>.
     *
     * @param selected the selected to set
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * isSelected.
     *

     */
    public boolean isSelected() {
        return selected;
    }
}
