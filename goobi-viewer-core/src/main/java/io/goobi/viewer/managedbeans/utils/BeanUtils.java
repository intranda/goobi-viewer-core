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

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.weld.serialization.spi.helpers.SerializableContextualInstance;

import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.imaging.MediaDeliveryHandlers;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.managedbeans.ActiveDocumentBean;
import io.goobi.viewer.managedbeans.AdminBean;
import io.goobi.viewer.managedbeans.BookmarkBean;
import io.goobi.viewer.managedbeans.CalendarBean;
import io.goobi.viewer.managedbeans.CaptchaBean;
import io.goobi.viewer.managedbeans.CmsBean;
import io.goobi.viewer.managedbeans.CmsCollectionsBean;
import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.managedbeans.CollectionBrowseBean;
import io.goobi.viewer.managedbeans.CollectionViewBean;
import io.goobi.viewer.managedbeans.ContentBean;
import io.goobi.viewer.managedbeans.CreateRecordBean;
import io.goobi.viewer.managedbeans.DisplayConditions;
import io.goobi.viewer.managedbeans.ImageDeliveryBean;
import io.goobi.viewer.managedbeans.MetadataBean;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.TermBrowseBean;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.storage.ApplicationBean;
import io.goobi.viewer.managedbeans.storage.SessionBean;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.servlets.utils.ServletUtils;
import jakarta.el.ELException;
import jakarta.el.ValueExpression;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.faces.application.Application;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Utility class for methods that use the FacesContext.
 */
public final class BeanUtils {

    private static final Logger logger = LogManager.getLogger(BeanUtils.class);

    private static Locale defaultLocale = null;

    // ApplicationBean is @ApplicationScoped — the same instance for the entire application lifetime.
    // Caching it here avoids a full Weld CDI lookup (getBeanByName → BeanManager → StackWalker) on
    // every call to getPersistentStorageBean(). volatile ensures safe publication across threads.
    private static volatile ApplicationBean cachedApplicationBean = null; //NOSONAR S3077: @ApplicationScoped singleton, assigned once

    /**
     * Private constructor.
     */
    private BeanUtils() {

    }

    /**
     * Gets the current Request from the faces context.
     *
     * @return the current HTTP servlet request, or null if unavailable
     */
    public static HttpServletRequest getRequest() {
        // Check FacesContext first — it is a cheap thread-local lookup with no CDI overhead.
        // CDI fallback is only needed outside JSF (REST resources, servlets, filters).
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            return getRequest(facesContext);
        }

        return getRequestFromCdi(getBeanManager());
    }

    /**
     * Returns the current request via Weld's built-in {@link HttpServletRequest} bean. Unlike going through a session scoped bean, this never creates
     * an HTTP session.
     *
     * @param bm CDI bean manager; may be null
     * @return the current request, or null if no servlet request is active on this thread
     * @should return current request from cdi
     * @should return null if no request is active
     */
    static HttpServletRequest getRequestFromCdi(BeanManager bm) {
        if (bm == null) {
            return null;
        }
        try {
            Bean<?> bean = bm.resolve(bm.getBeans(HttpServletRequest.class));
            if (bean == null) {
                return null;
            }
            HttpServletRequest request = (HttpServletRequest) bm.getReference(bean, HttpServletRequest.class, bm.createCreationalContext(bean));
            // The reference is a client proxy; probe it so callers get null instead of a proxy that fails on use
            // when no servlet request is active (background threads, shutdown)
            request.getDispatcherType();
            return request;
        } catch (RuntimeException e) {
            // ContextNotActiveException, IllegalStateException, ClassCastException (request context not backed by a servlet request), ...
            return null;
        }
    }

    /**
     * getRequest.
     *
     * @param context faces context to extract the request from
     * @return the HTTP servlet request from the given FacesContext, or null if unavailable
     */
    public static HttpServletRequest getRequest(FacesContext context) {
        if (context != null && context.getExternalContext() != null) {
            return (HttpServletRequest) context.getExternalContext().getRequest();
        }

        return null;
    }

    /**
     * getSession.
     *
     * @return HttpSession from current request
     */
    public static HttpSession getSession() {
        HttpServletRequest request = getRequest();
        if (request != null) {
            return request.getSession(false);
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
     * hasJsfContext.
     *
     * @return true if a JSF FacesContext is available in the current thread, false otherwise
     */
    public static boolean hasJsfContext() {
        return FacesContext.getCurrentInstance() != null;
    }

    /**
     * getServletImagesPathFromRequest.
     *
     * @param request incoming HTTP request for base URL resolution
     * @param theme theme folder name appended to the path
     * @return the absolute URL path to the servlet images directory for the given theme
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
     * getServletContext.
     *
     * @return the current ServletContext, or null if no FacesContext is available
     */
    public static ServletContext getServletContext() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null && context.getExternalContext() != null) {
            return (ServletContext) context.getExternalContext().getContext();
        }

        return null;
    }

    /**
     * getInitialLocale.
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
     * getLocale.
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
     * getDefaultLocale.
     *
     * @return the default application locale
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
        } catch (IllegalStateException | IndexOutOfBoundsException e) {
            // CDI container not available or already shut down (Weld may throw IndexOutOfBoundsException during shutdown)
        }
        // Via FacesContext
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getExternalContext().getContext() != null) {
            ret = (BeanManager) ((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext())
                    .getAttribute("jakarta.enterprise.inject.spi.BeanManager");
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
     * getBeanByName.
     *
     * @param name CDI bean name to look up
     * @param clazz expected type used to create the CDI reference
     * @return the CDI-managed bean reference for the given name and type, or null if not found
     * @should throw IllegalArgumentException if named bean of different class
     * @should throw IllegalStateException if named bean of different class
     * @throws ContextNotActiveException if no jsf context is available to retrieve the bean from
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Object getBeanByName(String name, Class clazz) throws ContextNotActiveException {
        try {
            BeanManager bm = getBeanManager();
            if (bm != null && bm.getBeans(name).iterator().hasNext()) {
                Bean bean = bm.getBeans(name).iterator().next();
                if (wouldCreateSession(bean, bm)) {
                    logger.trace("Not looking up session scoped bean '{}' outside of JSF because the request has no session", name);
                    return null;
                }
                CreationalContext ctx = bm.createCreationalContext(bean);
                return bm.getReference(bean, clazz, ctx);
            }
        } catch (IllegalStateException e) {
            // Expected during CDI container shutdown (e.g. in-flight requests after Weld has stopped)
            logger.warn("CDI container not available when looking up bean '{}': {}", name, e.getMessage());
        } catch (IllegalArgumentException | NullPointerException e) {
            logger.error("Error when getting bean by name '{}'", name, e);
        }

        return null;
    }

    /**
     * Checks whether using a reference to the given bean would make Weld create a new HTTP session. This is the case for session scoped beans outside
     * of JSF (REST resources, servlets, filters) when the current request has no session yet: the first method call on the client proxy instantiates
     * the bean and with it the session. JSF requests are not affected, they need a session anyway.
     *
     * @param bean the CDI bean
     * @param bm CDI bean manager
     * @return true if the bean is session scoped, there is no FacesContext and the current request has no session
     * @should return true for session scoped bean outside jsf if request has no session
     * @should return false if request has session
     * @should return false for bean that is not session scoped
     * @should return false in jsf context
     * @should return false if no request is active
     */
    static boolean wouldCreateSession(Bean<?> bean, BeanManager bm) {
        if (!SessionScoped.class.equals(bean.getScope()) || hasJsfContext()) {
            return false;
        }
        HttpServletRequest request = getRequestFromCdi(bm);
        // Without request the session context is not active either; the proxy will throw ContextNotActiveException as before
        return request != null && request.getSession(false) == null;
    }

    /**
     * getNavigationHelper.
     *
     * @return the NavigationHelper managed bean, or null if outside a JSF context
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
     * getAdminBean.
     *
     * @return the AdminBean managed bean
     */
    public static AdminBean getAdminBean() {
        return (AdminBean) getBeanByName("adminBean", AdminBean.class);
    }

    /**
     * getCollectionViewBean.
     *
     * @return the CollectionViewBean managed bean
     */
    public static CollectionViewBean getCollectionViewBean() {
        return (CollectionViewBean) getBeanByName("collectionViewBean", CollectionViewBean.class);
    }

    /**
     * getActiveDocumentBean.
     *
     * @return the ActiveDocumentBean managed bean
     */
    public static ActiveDocumentBean getActiveDocumentBean() {
        return (ActiveDocumentBean) getBeanByName("activeDocumentBean", ActiveDocumentBean.class);
    }

    /**
     * getPersistentStorageBean.
     *
     * <p>
     * Returns the {@code @ApplicationScoped} ApplicationBean. The result is cached in a static volatile field after the first successful lookup to
     * avoid repeated Weld CDI bean resolution (which involves a Thread.getStackTrace() call on every invocation).
     *
     * @return the ApplicationBean managed bean
     */
    public static ApplicationBean getPersistentStorageBean() {
        ApplicationBean cached = cachedApplicationBean;
        if (cached != null) {
            return cached;
        }
        // First call — resolve through CDI and cache for all future calls
        ApplicationBean fresh = (ApplicationBean) getBeanByName("applicationBean", ApplicationBean.class);
        if (fresh != null) {
            cachedApplicationBean = fresh;
        }
        return fresh;
    }

    /**
     * Clears the cached ApplicationBean instance. Call this only in tests that replace the bean with a mock, to prevent stale cache entries from
     * leaking across test methods.
     */
    static void clearCachedApplicationBean() {
        cachedApplicationBean = null;
    }

    /**
     * getSearchBean.
     *
     * @return the SearchBean managed bean
     */
    public static SearchBean getSearchBean() {
        return (SearchBean) getBeanByName("searchBean", SearchBean.class);
    }

    /**
     * getBookmarkBean.
     *
     * @return the BookmarkBean managed bean
     */
    public static BookmarkBean getBookmarkBean() {
        return (BookmarkBean) getBeanByName("bookmarkBean", BookmarkBean.class);
    }

    /**
     * getCreateRecordBean.
     *
     * @return the CreateRecordBean managed bean
     */
    public static CreateRecordBean getCreateRecordBean() {
        return (CreateRecordBean) getBeanByName("createRecordBean", CreateRecordBean.class);
    }

    /**
     * getCMSCollectionsBean.
     *
     * @return the CmsCollectionsBean managed bean
     */
    public static CmsCollectionsBean getCMSCollectionsBean() {
        return (CmsCollectionsBean) getBeanByName("cmsCollectionsBean", CmsCollectionsBean.class);
    }

    /**
     * getMetadataBean.
     *
     * @return the MetadataBean managed bean
     */
    public static MetadataBean getMetadataBean() {
        return (MetadataBean) getBeanByName("metadataBean", MetadataBean.class);
    }

    /**
     * getCmsBean.
     *
     * @return the CmsBean managed bean
     */
    public static CmsBean getCmsBean() {
        return (CmsBean) getBeanByName("cmsBean", CmsBean.class);
    }

    /**
     * getCmsMediaBean.
     *
     * @return the CmsMediaBean managed bean
     */
    public static CmsMediaBean getCmsMediaBean() {
        return (CmsMediaBean) getBeanByName("cmsMediaBean", CmsMediaBean.class);
    }

    /**
     * getCalendarBean.
     *
     * @return the CalendarBean managed bean
     */
    public static CalendarBean getCalendarBean() {
        return (CalendarBean) getBeanByName("calendarBean", CalendarBean.class);
    }

    /**
     * getCaptchaBean.
     *
     * @return the CaptchaBean managed bean
     */
    public static CaptchaBean getCaptchaBean() {
        return (CaptchaBean) getBeanByName("captchaBean", CaptchaBean.class);
    }

    /**
     * getUserBean.
     *
     * @return the UserBean managed bean
     * @throws ContextNotActiveException if no jsf context is available to retrieve the bean from
     */
    public static UserBean getUserBean() throws ContextNotActiveException {
        return (UserBean) getBeanByName("userBean", UserBean.class);
    }

    /**
     * getSessionBean.
     *
     * @return the SessionBean managed bean, or a new instance if the CDI bean is unavailable
     */
    public static SessionBean getSessionBean() {
        Object bean = getBeanByName("sessionBean", SessionBean.class);
        if (bean != null) {
            return (SessionBean) bean;
        }

        return new SessionBean();
    }

    /**
     * getImageDeliveryBean.
     *
     * <p>
     * Within JSF the session scoped CDI bean is returned. Outside of JSF (REST resources, servlets, filters) an unmanaged instance backed by
     * session independent {@link MediaDeliveryHandlers} for the current request is returned instead, so that no HTTP session is created. The methods
     * depending on the current record (e.g. {@link ImageDeliveryBean#getCurrentImage()}) use {@link #getActiveDocumentBean()} in both cases.
     *
     * @return the ImageDeliveryBean managed bean, or an initialized unmanaged instance if outside JSF or unavailable
     * @should return initialized instance outside jsf
     */
    public static ImageDeliveryBean getImageDeliveryBean() {
        if (hasJsfContext()) {
            ImageDeliveryBean bean = (ImageDeliveryBean) getBeanByName("imageDelivery", ImageDeliveryBean.class);
            if (bean != null) {
                try {
                    bean.getThumbs();
                    return bean;
                } catch (ContextNotActiveException e) {
                    // fall through to unmanaged instance
                }
            }
        }
        return new ImageDeliveryBean(MediaDeliveryHandlers.forRequest(getRequest()));
    }

    /**
     * getCollectionBrowseBean.
     *
     * @return the CollectionBrowseBean managed bean
     */
    public static CollectionBrowseBean getCollectionBrowseBean() {
        return (CollectionBrowseBean) getBeanByName("collectionBrowseBean", CollectionBrowseBean.class);
    }

    /**
     * getTermBrowseBean.
     *
     * @return the TermBrowseBean managed bean
     */
    public static TermBrowseBean getTermBrowseBean() {
        return (TermBrowseBean) getBeanByName("termBrowseBean", TermBrowseBean.class);
    }

    /**
     * getUserBean.
     *
     * @return the ContentBean managed bean
     */
    public static ContentBean getContentBean() {
        return (ContentBean) getBeanByName("contentBean", ContentBean.class);
    }

    /**
     * getUserBeanFromSession.
     *
     * @param session HTTP session to retrieve the UserBean from
     * @return the UserBean stored in the given session, or null if not found
     */
    public static UserBean getUserBeanFromSession(HttpSession session) {
        if (session != null) {
            try {
                Object bean = session.getAttribute("userBean");
                if (bean != null) {
                    return (UserBean) bean;
                }
                return findInstanceInSessionAttributes(session, UserBean.class)
                        .orElse(null);
            } catch (IllegalStateException e) {
                // Session was invalidated before the request finished
                logger.warn("Session already invalidated when retrieving userBean: {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * getBeanFromSession.
     *
     * @param session HTTP session to retrieve the bean from
     * @param beanName session attribute name of the bean
     * @param clazz expected type of the bean
     * @param <T> a T class
     * @return an Optional containing the typed bean from the session, or empty if not found
     * @should return null when finds subclass when direct lookup
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getBeanFromSession(HttpSession session, String beanName, Class<T> clazz) {
        if (session != null) {
            Object bean = session.getAttribute(beanName);
            if (clazz.isInstance(bean)) {
                return Optional.of(bean).map(o -> (T) o);
            }
            return findInstanceInSessionAttributes(session, clazz);
        }

        return Optional.empty();
    }

    /**
     * getUserFromSession. This performs a scan of the whole session and may be expensive. Prefer using {@link #getUserBean()} and
     * #{@link UserBean#getUser()}
     *
     * @param session HTTP session containing the UserBean
     * @return the User stored in the UserBean of the given session, or null if not found
     */
    public static User getUserFromSession(HttpSession session) {
        UserBean ub = getUserBeanFromSession(session);
        if (ub != null) {
            return ub.getUser();
        }

        return null;
    }

    /**
     * escapeCriticalUrlChracters.
     *
     * @param value URL string to escape
     * @return the input string with critical URL characters escaped
     */
    public static String escapeCriticalUrlChracters(String value) {
        return StringTools.escapeCriticalUrlChracters(value, false);
    }

    /**
     * findInstanceInSessionAttributes.
     *
     * @param session HTTP session whose attributes are scanned
     * @param clazz type to search for among session attributes
     * @param <T> a T class
     * @return an Optional containing the first session attribute of the given type, or empty if none found
     * @should return subclass stored under internal weld key
     * @should return exact class match
     * @should not return unrelated class
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> Optional<T> findInstanceInSessionAttributes(HttpSession session, Class<T> clazz) {
        Enumeration<String> attributeNames = session.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            Object attributeValue = session.getAttribute(attributeName);
            if (clazz.isInstance(attributeValue)) {
                return Optional.of(attributeValue).map(o -> (T) o);
            } else if (attributeValue instanceof SerializableContextualInstance serializableContextualInstance) {
                Object instance = serializableContextualInstance.getInstance();
                if (clazz.isInstance(instance)) {
                    return Optional.of(instance).map(o -> (T) o);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * getResponse.
     *
     * @return the current HTTP servlet response, or null if no FacesContext is available
     */
    public static HttpServletResponse getResponse() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null && context.getExternalContext() != null) {
            return (HttpServletResponse) context.getExternalContext().getResponse();
        }

        return null;
    }

    /**
     * getManagedBeanValue.
     *
     * @param expr EL expression string to evaluate
     * @return the value resolved by the EL expression, or null if the context or expression is unavailable
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
                } catch (NullPointerException | ELException | IllegalStateException e) {
                    logger.error("Error getting the object {} from context: {}", expr, e.getMessage());
                }
            }
        }
        return value;
    }

    /**
     * Removes the user and permission attributes from the session.
     * 
     * @param session {@link HttpSession}
     */
    public static void wipeSessionAttributes(HttpSession session) {
        logger.trace("wipeSession");
        if (session == null) {
            return;
        }

        session.removeAttribute("user");

        // Remove priv maps
        AccessConditionUtils.clearSessionPermissions(session);

        getBeanFromSession(session, "sessionBean", SessionBean.class)
                .ifPresentOrElse(SessionBean::cleanObjects,
                        () -> logger.trace("Cannot invalidate SessionBean. Not instantiated yet?"));
        getBeanFromSession(session, "collectionViewBean", CollectionViewBean.class)
                .ifPresentOrElse(CollectionViewBean::invalidate,
                        () -> logger.trace("Cannot invalidate CollectionViewBean. Not instantiated yet?"));
        getBeanFromSession(session, "activeDocumentBean", ActiveDocumentBean.class)
                .ifPresentOrElse(ActiveDocumentBean::resetAccess,
                        () -> logger.trace("Cannot reset access permissions in ActiveDocumentBean. Not instantiated yet?"));
        getBeanFromSession(session, "displayConditions", DisplayConditions.class)
                .ifPresentOrElse(DisplayConditions::clearCache,
                        () -> logger.trace("Cannot clear DosplayConditions cache. Not instantiated yet?"));
        // Reset loaded user-generated content lists
        getBeanFromSession(session, "contentBean", ContentBean.class)
                .ifPresentOrElse(ContentBean::resetContentList,
                        () -> logger.trace("Cannot reset content list. Not instantiated yet?"));
        // Reset visible navigation menu
        getBeanFromSession(session, "cmsBean", CmsBean.class)
                .ifPresentOrElse(CmsBean::resetNavigationMenuItems,
                        () -> logger.trace("Cannot reset navigation menu items. Not instantiated yet?"));
        // Unload current record to reset permissions
        getBeanFromSession(session, "activeDocumentBean", ActiveDocumentBean.class)
                .ifPresentOrElse(t -> {
                    try {
                        t.reset();
                    } catch (IndexUnreachableException e) {
                        logger.error(e.getMessage());
                    }
                },
                        () -> logger.trace("Cannot reset loaded record. Not instantiated yet?"));
    }
}
