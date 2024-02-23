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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.CmsBean;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.itemfunctionality.SearchFunctionality;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.search.SearchInterface;
import io.goobi.viewer.model.viewer.PageType;

/**
 * A list of strings with some convenience methods. Each string is either the id of a cmsPage or a pageType name The list may be stored as a single
 * string containing all strings in the list separated by ;
 *
 * @author Florian Alpers
 */
public class PageList implements Iterable<String> {

    private static final Logger logger = LogManager.getLogger(PageList.class);

    private List<String> pages = new ArrayList<>();

    /**
     * <p>
     * Constructor for PageList.
     * </p>
     */
    public PageList() {
        super();
    }

    public PageList(List<Long> pageIds) {
        this.pages = pageIds.stream().map(l -> Long.toString(l)).collect(Collectors.toList());
    }

    /**
     * <p>
     * Constructor for PageList.
     * </p>
     *
     * @param stringRep a {@link java.lang.String} object.
     */
    public PageList(String stringRep) {
        super();
        if (StringUtils.isNotBlank(stringRep)) {
            String[] ids = stringRep.split(";");
            for (String id : ids) {
                if (StringUtils.isNotBlank(id) && (id.matches("\\d+") || PageType.getByName(id) != PageType.other)) {
                    try {
                        this.pages.add(id);
                    } catch (NumberFormatException e) {
                        logger.error(e.toString(), e);
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return StringUtils.join(pages, ";");
        //        String string = this.stream().map(page -> page.getId()).map(id -> Long.toString(id)).collect(Collectors.joining(";"));
        //        return string;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return pages.hashCode();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj != null) {
            return this.toString().equals(obj.toString());
        }
        return false;
    }

    /**
     * <p>
     * Getter for the field <code>pages</code>.
     * </p>
     *
     * @return the pages
     */
    public List<String> getPages() {
        return pages;
    }

    /**
     * <p>
     * Setter for the field <code>pages</code>.
     * </p>
     *
     * @param pages the pages to set
     */
    public void setPages(List<String> pages) {
        this.pages = pages;
    }

    /**
     * get the if/pageName of the first page, or an empty string if no pages exist
     *
     * @return the if/pageName of the first page, or an empty string if no pages exist
     */
    public String getPage() {
        return getPage(0);
    }

    /**
     * <p>
     * getPage.
     * </p>
     *
     * @param index a int.
     * @return a {@link java.lang.String} object.
     */
    public String getPage(int index) {
        if (pages.size() > index) {
            return pages.get(index);
        }
        return "";
    }

    /**
     * Get the matching {@link io.goobi.viewer.model.search.SearchInterface} for the first listed page. This is the {@link SearchFunctionality} of
     * that page if any exists, or otherwise the {@link SearchBean}
     *
     * @return the matching {@link io.goobi.viewer.model.search.SearchInterface} for the first listed page. This is the {@link SearchFunctionality} of
     *         that page if any exists, or otherwise the {@link SearchBean}
     * @throws java.lang.NumberFormatException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public SearchInterface getSearch() throws NumberFormatException, DAOException {
        return getSearch(0);
    }

    /**
     * <p>
     * getSearch.
     * </p>
     *
     * @param pageIndex a int.
     * @return a {@link io.goobi.viewer.model.search.SearchInterface} object.
     * @throws java.lang.NumberFormatException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public SearchInterface getSearch(int pageIndex) throws NumberFormatException, DAOException {
        SearchInterface search = null;
        //        String page = getPage(pageIndex);
        //        if (isCMSPage(page)) {
        //            CMSPage cmsPage = DataManager.getInstance().getDao().getCMSPage(Long.parseLong(getPage()));
        //            if (cmsPage != null) {
        //                search = cmsPage.getSearch();
        //            }
        //        }
        //        if (search == null) {
        //            search = BeanUtils.getSearchBean();
        //        }
        return search;
    }

    /**
     * <p>
     * getUrl.
     * </p>
     *
     * @param pageIndex the index of the desired page in the page list
     * @return the url of the page at pageIndex, relative to the host url
     * @throws java.lang.NumberFormatException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getUrl(int pageIndex) throws NumberFormatException, DAOException {
        String url = "";
        String page = getPage(pageIndex);
        if (isCMSPage(page)) {
            CMSPage cmsPage = DataManager.getInstance().getDao().getCMSPage(Long.parseLong(getPage()));
            if (cmsPage != null) {
                url = cmsPage.getRelativeUrlPath();
            }
        } else {
            url = PageType.getByName(page).getName() + "/";
        }
        return BeanUtils.getRequest().getContextPath() + "/" + url;
    }

    /**
     * @param page
     * @return true if the given string is a number, i.e. it is the identifier of a cms page
     */
    private static boolean isCMSPage(String page) {
        return page.matches("\\d+");
    }

    /**
     * Sets the pages list to a list containing only the given string
     *
     * @param page a {@link java.lang.String} object.
     */
    public void setPage(String page) {
        this.pages = Collections.singletonList(page);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Iterable#iterator()
     */
    /** {@inheritDoc} */
    @Override
    public Iterator<String> iterator() {
        return pages.iterator();
    }

    /**
     * <p>
     * getPage.
     * </p>
     *
     * @param idString a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.cms.pages.CMSPage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSPage getPage(String idString) throws DAOException {
        if (StringUtils.isNotBlank(idString) && idString.matches("\\d+")) {
            Long id = Long.parseLong(idString);
            CmsBean bean = BeanUtils.getCmsBean();
            if (bean != null) {
                return bean.getPage(id);
            }
        }
        throw new IllegalArgumentException("No cms page found with id = " + idString);
    }

    /**
     * <p>
     * isEmpty.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isEmpty() {
        return pages == null || pages.isEmpty();
    }

}
