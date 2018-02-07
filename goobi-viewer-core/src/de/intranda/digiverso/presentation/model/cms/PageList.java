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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.managedbeans.CmsBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;

/**
 * @author Florian Alpers
 *
 */
public class PageList implements Iterable<String>{

    private static final Logger logger = LoggerFactory.getLogger(PageList.class);

    private List<String> pages = new ArrayList<>();
    
    public PageList() {
        super();
    }

    public PageList(String stringRep) {
        super();
        if (StringUtils.isNotBlank(stringRep)) {
            String[] ids = stringRep.split(";");
            for (String id : ids) {
                if (StringUtils.isNotBlank(id) && id.matches("\\d+")) {
                    try {
//                        CMSPage page = BeanUtils.getCmsBean().getPage(Long.parseLong(id));
                        this.pages.add(id);
                    } catch (NumberFormatException e) {
                        logger.error(e.toString(), e);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return StringUtils.join(pages, ";");
//        String string = this.stream().map(page -> page.getId()).map(id -> Long.toString(id)).collect(Collectors.joining(";"));
//        return string;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return pages.hashCode();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if(obj != null) {
            return this.toString().equals(obj.toString());
        } else {
            return false;
        }
    }
    
    /**
     * @return the pages
     */
    public List<String> getPages() {
        return pages;
    }
    
    /**
     * @param pages the pages to set
     */
    public void setPages(List<String> pages) {
        this.pages = pages;
    }
    
    /**
     * get the value of the first page
     * 
     * @return
     */
    public String getPage() {
        if(!pages.isEmpty()) {            
            return pages.get(0);
        } else {
            return "";
        }
    }
    
    public Optional<CMSPage> getCMSPageIfExists() {
        try {
            return Optional.ofNullable(DataManager.getInstance().getDao().getCMSPage(Long.parseLong(getPage())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        } catch(DAOException e)  {
            logger.error("Error querying dao for cms page '" + getPage() + "'", e);
            return Optional.empty();
        }
    }
    
    /**
     * Sets the pages list to a list containing only the given string
     * 
     * @param page
     */
    public void setPage(String page) {
        this.pages = Collections.singletonList(page);
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<String> iterator() {
        return pages.iterator();
    }
    
    public CMSPage getPage(String idString) throws DAOException {
        if(StringUtils.isNotBlank(idString) && idString.matches("\\d+")) {
            Long id = Long.parseLong(idString);
            CmsBean bean = BeanUtils.getCmsBean();
            if(bean != null) {
                return bean.getPage(id);
            }
        }
        throw new IllegalArgumentException("No cms page found with id = " + idString);
    }
    
    public boolean isEmpty() {
        return pages == null || pages.isEmpty();
    }

}
