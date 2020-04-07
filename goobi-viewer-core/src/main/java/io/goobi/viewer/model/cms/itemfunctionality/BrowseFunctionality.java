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
package io.goobi.viewer.model.cms.itemfunctionality;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.BrowseBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;

/**
 * @author florian
 *
 */
public class BrowseFunctionality implements Functionality {

    private BrowseBean bean = BeanUtils.getBrowseBean();
    
    private String browseField = "";
    
    /**
     * @param collectionField
     */
    public BrowseFunctionality(String field) {
        setBrowseField(field);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.itemfunctionality.Functionality#setPageNo(int)
     */
    @Override
    public void setPageNo(int pageNo) {
        bean.setCurrentPage(pageNo);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.itemfunctionality.Functionality#getPageNo()
     */
    @Override
    public int getPageNo() {
        return bean.getCurrentPage();
    }
    
    /**
     * Set the SOLR field which to browse to create the list
     * @param field
     */
    public void setBrowseField(String field) {
       this.browseField = field;
    }
    
    /**
     * Get the SOLR field which to browse to create the list
     * @return
     */
    public String getBrowseField() {
        return this.browseField;
    }
    
    /**
     * Set an additionl filter to restrict the list to a subset of all records
     * @param field
     * @param value
     */
    public void setFilter(String field, String value) {
        bean.setFilterQuery(field + ":" + value);
    }
    
    /**
     * Set the start character for which results should be displayed
     * @param start
     */
    public void setStartingCharacter(String start) {
        bean.setCurrentStringFilter(start);
    }
    
    /**
     * Get the start character for which results should be displayed
     * @return
     */
    public String getStartingCharacter() {
        return bean.getCurrentStringFilter();
    }
    
    public void searchTerms() throws PresentationException, IndexUnreachableException {
        bean.setBrowsingMenuField(getBrowseField());
        bean.searchTerms();
    }

    /**
     * Resets the current browse page and the current string filter
     */
    public void reset() {
       bean.setCurrentStringFilter("");
       bean.setCurrentPage(1);
    }
    
}
