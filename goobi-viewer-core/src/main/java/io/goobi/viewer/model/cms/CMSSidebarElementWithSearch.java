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
package io.goobi.viewer.model.cms;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * CMSSidebarElementWithSearch class.
 * </p>
 */
@Entity
public class CMSSidebarElementWithSearch extends CMSSidebarElement {

    @Column(name = "additional_query")
    private String additionalQuery = "";

    /**
     * <p>
     * Constructor for CMSSidebarElementWithSearch.
     * </p>
     */
    public CMSSidebarElementWithSearch() {

    }

    /**
     * <p>
     * Constructor for CMSSidebarElementWithSearch.
     * </p>
     *
     * @param original a {@link io.goobi.viewer.model.cms.CMSSidebarElementWithSearch} object.
     * @param owner a {@link io.goobi.viewer.model.cms.CMSPage} object.
     */
    public CMSSidebarElementWithSearch(CMSSidebarElementWithSearch original, CMSPage owner) {
        super(original, owner);
        this.additionalQuery = original.additionalQuery;
    }

    /**
     * <p>
     * Getter for the field <code>additionalQuery</code>.
     * </p>
     *
     * @return the additionalQuery
     */
    public String getAdditionalQuery() {
        return additionalQuery;
    }

    /**
     * <p>
     * Setter for the field <code>additionalQuery</code>.
     * </p>
     *
     * @param additionalQuery the additionalQuery to set
     */
    public void setAdditionalQuery(String additionalQuery) {
        this.additionalQuery = additionalQuery;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.CMSSidebarElement#hashCode()
     */
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int code = super.hashCode();
        if (StringUtils.isNotBlank(getWidgetTitle())) {
            code += HASH_MULTIPLIER * getWidgetTitle().hashCode();
        }
        if (StringUtils.isNotBlank(getAdditionalQuery())) {
            code *= HASH_MULTIPLIER * getAdditionalQuery().hashCode();
        }
        return code;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        return o.getClass().equals(CMSSidebarElementWithSearch.class) && bothNullOrEqual(getType(), ((CMSSidebarElement) o).getType())
                && bothNullOrEqual(getWidgetTitle(), ((CMSSidebarElementWithSearch) o).getWidgetTitle())
                && bothNullOrEqual(getAdditionalQuery(), ((CMSSidebarElementWithSearch) o).getAdditionalQuery());
    }

    /** {@inheritDoc} */
    @Override
    public PageList getLinkedPages() {
        if (super.getLinkedPages() == null) {
            setLinkedPages(new PageList());
        }
        return super.getLinkedPages();
    }

}
