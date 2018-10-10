/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.intranda.digiverso.presentation.model.cms;

/**
 * 
 * A {@link CMSNavigationItem} which can be selected and deselected. Used for selecting available NavigationItems to be used in the navigation menu
 * 
 * @author Florian Alpers
 *
 */
public class SelectableNavigationItem extends CMSNavigationItem {

    private boolean selected = false;
    
    
    
    /**
     * 
     */
    public SelectableNavigationItem() {
        super();
    }

    /**
     * @param original
     */
    public SelectableNavigationItem(CMSNavigationItem original) {
        super(original);
    }

    /**
     * @param cmsPage
     */
    public SelectableNavigationItem(CMSPage cmsPage) {
        super(cmsPage);
    }

    /**
     * @param targetUrl
     * @param label
     */
    public SelectableNavigationItem(String targetUrl, String label) {
        super(targetUrl, label);
    }

    /**
     * @param selected the selected to set
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    /**
     * @return the selected
     */
    public boolean isSelected() {
        return selected;
    }
}
