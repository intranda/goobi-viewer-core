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
     * <p>
     * Constructor for SelectableNavigationItem.
     * </p>
     */
    public SelectableNavigationItem() {
        super();
    }

    /**
     * <p>
     * Constructor for SelectableNavigationItem.
     * </p>
     *
     * @param original a {@link io.goobi.viewer.model.cms.CMSNavigationItem} object.
     */
    public SelectableNavigationItem(CMSNavigationItem original) {
        super(original);
    }

    /**
     * <p>
     * Constructor for SelectableNavigationItem.
     * </p>
     *
     * @param cmsPage a {@link io.goobi.viewer.model.cms.pages.CMSPage} object.
     */
    public SelectableNavigationItem(CMSPage cmsPage) {
        super(cmsPage);
    }

    /**
     * <p>
     * Constructor for SelectableNavigationItem.
     * </p>
     *
     * @param targetUrl a {@link java.lang.String} object.
     * @param label a {@link java.lang.String} object.
     */
    public SelectableNavigationItem(String targetUrl, String label) {
        super(targetUrl, label);
    }

    /**
     * <p>
     * Setter for the field <code>selected</code>.
     * </p>
     *
     * @param selected the selected to set
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * <p>
     * isSelected.
     * </p>
     *
     * @return the selected
     */
    public boolean isSelected() {
        return selected;
    }
}
