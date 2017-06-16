/**
 * This file is part of the Goobi Viewer - a content presentation and management application for digitized objects.
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
package de.intranda.digiverso.presentation.managedbeans.utils;

import java.util.Locale;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.managedbeans.ActiveDocumentBean;
import de.intranda.digiverso.presentation.managedbeans.BookshelfBean;
import de.intranda.digiverso.presentation.managedbeans.BrowseBean;
import de.intranda.digiverso.presentation.managedbeans.CalendarBean;
import de.intranda.digiverso.presentation.managedbeans.CmsBean;
import de.intranda.digiverso.presentation.managedbeans.NavigationHelper;
import de.intranda.digiverso.presentation.managedbeans.SearchBean;
import de.intranda.digiverso.presentation.managedbeans.UserBean;
import de.intranda.digiverso.presentation.model.user.User;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

/**
 * Utility class for methods that use the FacesContext.
 */
public class BeanUtils {

    private static final Logger logger = LoggerFactory.getLogger(BeanUtils.class);

    public static final String SLASH_REPLACEMENT = "U002F";
    public static final String BACKSLASH_REPLACEMENT = "U005C";
    public static final String QUESTION_MARK_REPLACEMENT = "U003F";

    /**
     * 
     * @return
     */
    public static HttpServletRequest getRequest() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null && context.getExternalContext() != null) {
            HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
            return request;
        }

        return null;
    }

    public static HttpServletRequest getRequest(FacesContext context) {
        if (context != null && context.getExternalContext() != null) {
            HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
            return request;
        }

        return getRequest();
    }

    /**
     * retrieve complete Servlet url from servlet context, including Url, Port, Servletname etc. call this method only from jsf context
     *
     * @return complete url as string
     */
    public static String getServletPathWithHostAsUrlFromJsfContext() {
        if (FacesContext.getCurrentInstance() != null) {
            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            if (request != null) {
                return ServletUtils.getServletPathWithHostAsUrlFromRequest(request);
            }
        }

        return "";
    }

    /**
     * 
     * @return
     */
    public static ServletContext getServletContext() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null && context.getExternalContext() != null) {
            return (ServletContext) context.getExternalContext().getContext();
        }

        return null;
    }

    /**
     * 
     * @return
     */
    public static Locale getLocale() {
        NavigationHelper nh = BeanUtils.getNavigationHelper();
        if (nh != null) {
            return nh.getLocale();
        }

        return Locale.ENGLISH;
    }

    /**
     * 
     * @return
     */
    public static NavigationHelper getNavigationHelper() {
        if (FacesContext.getCurrentInstance() != null) {
            Object o = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("navigationHelper");
            if (o != null) {
                return (NavigationHelper) o;
            }
        }

        return null;
    }

    /**
     * 
     * @return
     */
    public static ActiveDocumentBean getActiveDocumentBean() {
        if (FacesContext.getCurrentInstance() != null) {
            Object o = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("activeDocumentBean");
            if (o != null) {
                return (ActiveDocumentBean) o;
            }
        }

        return null;
    }

    /**
     * 
     * @return
     */
    public static SearchBean getSearchBean() {
        if (FacesContext.getCurrentInstance() != null) {
            Object o = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("searchBean");
            if (o != null) {
                return (SearchBean) o;
            }
        }

        return null;
    }

    /**
     * 
     * @return
     */
    public static CmsBean getCmsBean() {
        if (FacesContext.getCurrentInstance() != null) {
            Object o = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("cmsBean");
            if (o != null) {
                return (CmsBean) o;
            }
        }

        return null;
    }

    /**
     * 
     * @return
     */
    public static CalendarBean getCalendarBean() {
        if (FacesContext.getCurrentInstance() != null) {
            Object o = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("calendarBean");
            if (o != null) {
                return (CalendarBean) o;
            }
        }

        return null;
    }

    /**
     * 
     * @return
     */
    public static BookshelfBean getBookshelfBean() {
        if (FacesContext.getCurrentInstance() != null) {
            Object o = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("bookshelfBean");
            if (o != null) {
                return (BookshelfBean) o;
            }
        }

        return null;
    }

    /**
     * 
     * @return
     */
    public static UserBean getUserBean() {
        if (FacesContext.getCurrentInstance() != null) {
            Object o = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("userBean");
            if (o != null) {
                return (UserBean) o;
            }
        }

        return null;
    }

    /**
     * 
     * @return
     */
    public static BrowseBean getBrowseBean() {
        if (FacesContext.getCurrentInstance() != null) {
            Object o = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("browseBean");
            if (o != null) {
                return (BrowseBean) o;
            }
        }

        return null;
    }

    /**
     * 
     * @return
     */
    public static UserBean getUserBeanFromRequest(HttpServletRequest request) {
        if (request != null) {
            return (UserBean) request.getSession().getAttribute("userBean");
        }

        return null;
    }

    /**
     * 
     * @return
     */
    public static User getUserFromRequest(HttpServletRequest request) {
        UserBean ub = getUserBeanFromRequest(request);
        if (ub != null) {
            return ub.getUser();
        }

        return null;
    }

    /**
     *
     * @param value
     * @return
     * @should replace characters correctly
     */
    public static String escapeCriticalUrlChracters(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value may not be null");
        }

        return value.replace("/", SLASH_REPLACEMENT).replace("\\", BACKSLASH_REPLACEMENT).replace("?", QUESTION_MARK_REPLACEMENT);
    }

    /**
     *
     * @param value
     * @return
     * @should replace characters correctly
     */
    public static String unescapeCriticalUrlChracters(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value may not be null");
        }

        return value.replace(SLASH_REPLACEMENT, "/").replace(BACKSLASH_REPLACEMENT, "\\").replace(QUESTION_MARK_REPLACEMENT, "?");
    }
}
