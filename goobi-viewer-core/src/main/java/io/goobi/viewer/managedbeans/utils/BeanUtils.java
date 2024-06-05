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
package io.goobi.viewer.managedbeans.utils;

import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.el.ValueExpression;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.faces.application.Application;
import javax.faces.context.FacesContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.weld.serialization.spi.helpers.SerializableContextualInstance;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.managedbeans.ActiveDocumentBean;
import io.goobi.viewer.managedbeans.AdminBean;
import io.goobi.viewer.managedbeans.BookmarkBean;
import io.goobi.viewer.managedbeans.BrowseBean;
import io.goobi.viewer.managedbeans.CalendarBean;
import io.goobi.viewer.managedbeans.CaptchaBean;
import io.goobi.viewer.managedbeans.CmsBean;
import io.goobi.viewer.managedbeans.CmsCollectionsBean;
import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.managedbeans.CollectionViewBean;
import io.goobi.viewer.managedbeans.ContentBean;
import io.goobi.viewer.managedbeans.CreateRecordBean;
import io.goobi.viewer.managedbeans.ImageDeliveryBean;
import io.goobi.viewer.managedbeans.MetadataBean;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.managedbeans.PersistentStorageBean;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.SessionBean;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.servlets.utils.ServletUtils;

/**
 * Utility class for methods that use the FacesContext.
 */
public final class BeanUtils {

    private static final Logger logger = LogManager.getLogger(BeanUtils.class);

    private static Locale defaultLocale = null;

    /**
     * Private constructor.
     */
    private BeanUtils() {

    }

    /**
     * Gets the current Request from the faces context
     *
     * @return a {@link javax.servlet.http.HttpServletRequest} object.
     */
    public static HttpServletRequest getRequest() {
        SessionBean sb = getSessionBean();
        try {
            return sb.getRequest();
        } catch (ContextNotActiveException | IllegalStateException e) {
            // logger.trace(e.getMessage());
        }

        FacesContext context = FacesContext.getCurrentInstance();
        return getRequest(context);
    }

    /**
     * <p>
     * getRequest.
     * </p>
     *
     * @param context a {@link javax.faces.context.FacesContext} object.
     * @return a {@link javax.servlet.http.HttpServletRequest} object.
     */
    public static HttpServletRequest getRequest(FacesContext context) {
        if (context != null && context.getExternalContext() != null) {
            return (HttpServletRequest) context.getExternalContext().getRequest();
        }

        return null;
    }

    /**
     * <p>getSession.</p>
     *
     * @return HttpSession from current request
     */
    public static HttpSession getSession() {
        HttpServletRequest request = getRequest();
        if (request != null) {
            return request.getSession();
        }

        return null;
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
            return "";
        }
        HttpServletRequest request = getRequest();
        if (request != null) {
            return ServletUtils.getServletPathWithHostAsUrlFromRequest(request);
        }
        return "";
    }

    /**
     * <p>
     * hasJsfContext.
     * </p>
     *
     * @return a boolean.
     */
    public static boolean hasJsfContext() {
        return FacesContext.getCurrentInstance() != null;
    }

    /**
     * <p>
     * getServletImagesPathFromRequest.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param theme a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getServletImagesPathFromRequest(HttpServletRequest request, String theme) {
        StringBuilder sb = new StringBuilder(ServletUtils.getServletPathWithHostAsUrlFromRequest(request));
        if (!sb.toString().endsWith("/")) {
            sb.append("/");
        }
        sb.append("resources").append("/");
        if (StringUtils.isNotBlank(theme)) {
            sb.append("themes").append("/").append(theme).append("/");
        }
        sb.append("images").append("/");
        return sb.toString();
    }

    /**
     * <p>
     * getServletContext.
     * </p>
     *
     * @return a {@link javax.servlet.ServletContext} object.
     */
    public static ServletContext getServletContext() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null && context.getExternalContext() != null) {
            return (ServletContext) context.getExternalContext().getContext();
        }
        
        return null;
    }

    /**
     * <p>getInitialLocale.</p>
     *
     * @return Currently selected or default {@link java.util.Locale}
     */
    public static Locale getInitialLocale() {
        Locale ret = null;
        if (FacesContext.getCurrentInstance() != null) {
            if (FacesContext.getCurrentInstance().getViewRoot() != null) {
                // Currently selected locale in FacesContext
                ret = FacesContext.getCurrentInstance().getViewRoot().getLocale();
            } else {
                // Default locale from Faces config
                ret = ViewerResourceBundle.getDefaultLocale();
            }
        }

        if (ret == null) {
            // Manually read Faces config file and return the first available locale
            // TODO This probably won't return anything if no FacesContext is available
            List<Locale> locales = ViewerResourceBundle.getLocalesFromFacesConfig(getServletContext());
            if (locales != null && !locales.isEmpty()) {
                ret = locales.get(0);
            }
        }

        if (ret == null) {

            // No Faces or servlet context available whatsoever - return locale for the configured default language.
            ret = ViewerResourceBundle.getFallbackLocale();
            logger.warn("Could not access FacesContext, using configured fallback locale: {}.", ret != null ? ret.getLanguage() : "NONE");
        }

        return ret;
    }

    /**
     * <p>
     * getLocale.
     * </p>
     *
     * @return Current Locale in {@link io.goobi.viewer.managedbeans.NavigationHelper}; default locale if none found
     */
    public static Locale getLocale() {
        NavigationHelper nh = BeanUtils.getNavigationHelper();
        if (nh != null) {
            return nh.getLocale();
        }

        return getInitialLocale();
    }

    /**
     * <p>
     * getDefaultLocale.
     * </p>
     *
     * @return a {@link java.util.Locale} object.
     */
    public static Locale getDefaultLocale() {
        if (defaultLocale == null) {
            NavigationHelper nh = BeanUtils.getNavigationHelper();
            if (nh != null) {
                defaultLocale = nh.getDefaultLocale();
            } else {
                return Locale.ENGLISH;
            }
        }
        return defaultLocale;
    }

    private static BeanManager getBeanManager() {
        BeanManager ret = null;

        // Via CDI
        try {
            ret = CDI.current().getBeanManager();
            if (ret != null) {
                return ret;
            }
        } catch (IllegalStateException e) {
            //
        }
        // Via FacesContext
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getExternalContext().getContext() != null) {
            ret = (BeanManager) ((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext())
                    .getAttribute("javax.enterprise.inject.spi.BeanManager");
            if (ret != null) {
                return ret;
            }
        }
        // Via JNDI
        try {
            InitialContext initialContext = new InitialContext();
            return (BeanManager) initialContext.lookup("java:comp/BeanManager");
        } catch (NamingException e) {
            // logger.warn("Couldn't get BeanManager through JNDI: {}", e.getMessage());
            return null;
        }
    }

    /**
     * <p>
     * getBeanByName.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @param clazz a {@link java.lang.Class} object.
     * @return a {@link java.lang.Object} object.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Object getBeanByName(String name, Class clazz) {
        BeanManager bm = getBeanManager();
        if (bm != null && bm.getBeans(name).iterator().hasNext()) {
            Bean bean = bm.getBeans(name).iterator().next();
            CreationalContext ctx = bm.createCreationalContext(bean);
            return bm.getReference(bean, clazz, ctx);
        }

        return null;
    }

    /**
     * <p>
     * getNavigationHelper.
     * </p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.NavigationHelper} object.
     */
    public static NavigationHelper getNavigationHelper() {
        //Don't attempt to get navigationHelper outside of faces context. Otherwise a new navigationHelper entity will be constructed
        //with false assumptions on current locale
        if (FacesContext.getCurrentInstance() != null) {
            return (NavigationHelper) getBeanByName("navigationHelper", NavigationHelper.class);
        }
        return null;
    }

    /**
     * <p>
     * getAdminBean.
     * </p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.AdminBean} object.
     */
    public static AdminBean getAdminBean() {
        return (AdminBean) getBeanByName("adminBean", AdminBean.class);
    }

    /**
     * <p>getCollectionViewBean.</p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.CollectionViewBean} object
     */
    public static CollectionViewBean getCollectionViewBean() {
        return (CollectionViewBean) getBeanByName("collectionViewBean", CollectionViewBean.class);
    }

    /**
     * <p>
     * getActiveDocumentBean.
     * </p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.ActiveDocumentBean} object.
     */
    public static ActiveDocumentBean getActiveDocumentBean() {
        return (ActiveDocumentBean) getBeanByName("activeDocumentBean", ActiveDocumentBean.class);
    }

    /**
     * <p>getPersistentStorageBean.</p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.PersistentStorageBean} object
     */
    public static PersistentStorageBean getPersistentStorageBean() {
        return (PersistentStorageBean) getBeanByName("applicationBean", PersistentStorageBean.class);
    }

    /**
     * <p>
     * getSearchBean.
     * </p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.SearchBean} object.
     */
    public static SearchBean getSearchBean() {
        return (SearchBean) getBeanByName("searchBean", SearchBean.class);
    }

    /**
     * <p>getBookmarkBean.</p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.BookmarkBean} object
     */
    public static BookmarkBean getBookmarkBean() {
        return (BookmarkBean) getBeanByName("bookmarkBean", BookmarkBean.class);
    }

    /**
     * <p>getCreateRecordBean.</p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.CreateRecordBean} object
     */
    public static CreateRecordBean getCreateRecordBean() {
        return (CreateRecordBean) getBeanByName("createRecordBean", CreateRecordBean.class);
    }

    /**
     * <p>
     * getCMSCollectionsBean.
     * </p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.CmsCollectionsBean} object.
     */
    public static CmsCollectionsBean getCMSCollectionsBean() {
        return (CmsCollectionsBean) getBeanByName("cmsCollectionsBean", CmsCollectionsBean.class);
    }

    /**
     * <p>
     * getMetadataBean.
     * </p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.MetadataBean} object.
     */
    public static MetadataBean getMetadataBean() {
        return (MetadataBean) getBeanByName("metadataBean", MetadataBean.class);
    }

    /**
     * <p>
     * getCmsBean.
     * </p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.CmsBean} object.
     */
    public static CmsBean getCmsBean() {
        return (CmsBean) getBeanByName("cmsBean", CmsBean.class);
    }

    /**
     * <p>
     * getCmsMediaBean.
     * </p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.CmsMediaBean} object.
     */
    public static CmsMediaBean getCmsMediaBean() {
        return (CmsMediaBean) getBeanByName("cmsMediaBean", CmsMediaBean.class);
    }

    /**
     * <p>
     * getCalendarBean.
     * </p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.CalendarBean} object.
     */
    public static CalendarBean getCalendarBean() {
        return (CalendarBean) getBeanByName("calendarBean", CalendarBean.class);
    }

    /**
     * <p>
     * getCaptchaBean.
     * </p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.CaptchaBean} object.
     */
    public static CaptchaBean getCaptchaBean() {
        return (CaptchaBean) getBeanByName("captchaBean", CaptchaBean.class);
    }

    /**
     * <p>
     * getUserBean.
     * </p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.UserBean} object.
     */
    public static UserBean getUserBean() {
        return (UserBean) getBeanByName("userBean", UserBean.class);
    }

    /**
     * <p>getSessionBean.</p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.SessionBean} object
     */
    public static SessionBean getSessionBean() {
        Object bean = getBeanByName("sessionBean", SessionBean.class);
        if (bean != null) {
            return (SessionBean) bean;
        }

        return new SessionBean();
    }

    /**
     * <p>
     * getImageDeliveryBean.
     * </p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.ImageDeliveryBean} object.
     */
    public static ImageDeliveryBean getImageDeliveryBean() {
        ImageDeliveryBean bean = (ImageDeliveryBean) getBeanByName("imageDelivery", ImageDeliveryBean.class);
        if (bean == null) {
            bean = new ImageDeliveryBean();
        } else {
            try {
                bean.getThumbs();
            } catch (ContextNotActiveException e) {
                bean = new ImageDeliveryBean();
                bean.init(
                        DataManager.getInstance().getConfiguration(),
                        DataManager.getInstance().getRestApiManager().getIIIFDataApiManager(),
                        DataManager.getInstance().getRestApiManager().getContentApiManager().orElse(null));
            }
        }
        return bean;
    }

    /**
     * <p>
     * getBrowseBean.
     * </p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.BrowseBean} object.
     */
    public static BrowseBean getBrowseBean() {
        return (BrowseBean) getBeanByName("browseBean", BrowseBean.class);
    }

    /**
     * <p>
     * getUserBean.
     * </p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.ContentBean} object.
     */
    public static ContentBean getContentBean() {
        return (ContentBean) getBeanByName("contentBean", ContentBean.class);
    }

    /**
     * <p>
     * getUserBeanFromRequest.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @return a {@link io.goobi.viewer.managedbeans.UserBean} object.
     */
    public static UserBean getUserBeanFromRequest(HttpServletRequest request) {
        if (request != null && request.getSession() != null) {
            Object bean = request.getSession().getAttribute("userBean");
            if (bean != null) {
                return (UserBean) bean;
            }
            return findInstanceInSessionAttributes(request, UserBean.class)
                    .orElse(null);
        }

        return null;
    }

    /**
     * <p>getBeanFromRequest.</p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object
     * @param beanName a {@link java.lang.String} object
     * @param clazz a {@link java.lang.Class} object
     * @param <T> a T class
     * @return a {@link java.util.Optional} object
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getBeanFromRequest(HttpServletRequest request, String beanName, Class<T> clazz) {
        if (request != null && request.getSession() != null) {
            Object bean = request.getSession().getAttribute(beanName);
            if (bean != null && bean.getClass().equals(clazz)) {
                return Optional.of(bean).map(o -> (T) o);
            }
            return findInstanceInSessionAttributes(request, clazz);
        }

        return Optional.empty();
    }

    /**
     * <p>
     * getUserFromRequest.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @return a {@link io.goobi.viewer.model.security.user.User} object.
     */
    public static User getUserFromRequest(HttpServletRequest request) {
        UserBean ub = getUserBeanFromRequest(request);
        if (ub != null) {
            return ub.getUser();
        }

        return null;
    }

    /**
     * <p>
     * escapeCriticalUrlChracters.
     * </p>
     *
     * @param value a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String escapeCriticalUrlChracters(String value) {
        return StringTools.escapeCriticalUrlChracters(value, false);
    }

    /**
     * <p>findInstanceInSessionAttributes.</p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object
     * @param clazz a {@link java.lang.Class} object
     * @param <T> a T class
     * @return a {@link java.util.Optional} object
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Optional<T> findInstanceInSessionAttributes(HttpServletRequest request, Class<T> clazz) {
        Enumeration<String> attributeNames = request.getSession().getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            Object attributeValue = request.getSession().getAttribute(attributeName);
            if (attributeValue != null && attributeValue.getClass().equals(clazz)) {
                return Optional.of(attributeValue).map(o -> (T) o);
            } else if (attributeValue instanceof SerializableContextualInstance) {
                Object instance = ((SerializableContextualInstance) attributeValue).getInstance();
                if (instance != null && instance.getClass().equals(clazz)) {
                    return Optional.of(instance).map(o -> (T) o);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * <p>
     * getResponse.
     * </p>
     *
     * @return a {@link javax.servlet.http.HttpServletResponse} object.
     */
    public static HttpServletResponse getResponse() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null && context.getExternalContext() != null) {
            return (HttpServletResponse) context.getExternalContext().getResponse();
        }

        return null;
    }

    /**
     * <p>getManagedBeanValue.</p>
     *
     * @param expr a {@link java.lang.String} object
     * @return a {@link java.lang.Object} object
     */
    public static Object getManagedBeanValue(String expr) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return null;
        }

        Object value = null;
        Application application = context.getApplication();
        if (application != null) {
            ValueExpression vb = application.getExpressionFactory().createValueExpression(context.getELContext(), expr, String.class);
            if (vb != null) {
                try {
                    value = vb.getValue(context.getELContext());
                } catch (Exception e) {
                    logger.error("Error getting the object {} from context: {}", expr, e.getMessage());
                }
            }
        }
        return value;
    }

}
