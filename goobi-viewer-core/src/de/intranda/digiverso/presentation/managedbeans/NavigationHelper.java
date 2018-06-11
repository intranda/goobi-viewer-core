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
package de.intranda.digiverso.presentation.managedbeans;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.faces.url.URL;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.DateTools;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.messages.ViewerResourceBundle;
import de.intranda.digiverso.presentation.model.cms.CMSPage;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.urlresolution.ViewHistory;
import de.intranda.digiverso.presentation.model.urlresolution.ViewerPath;
import de.intranda.digiverso.presentation.model.viewer.LabeledLink;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.modules.IModule;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

/**
 * This bean contains useful navigation parameters.
 */
@Named
@SessionScoped
public class NavigationHelper implements Serializable {

    private static final long serialVersionUID = 4171362984701032679L;

    private static final Logger logger = LoggerFactory.getLogger(NavigationHelper.class);

    public static final String URL_RSS = "rss";

    public static final int WEIGHT_TAG_MAIN_MENU = 1;
    public static final int WEIGHT_ACTIVE_COLLECTION = 2;
    public static final int WEIGHT_OPEN_DOCUMENT = 3;
    public static final int WEIGHT_BROWSE = 1;
    public static final int WEIGHT_SEARCH = 1;
    public static final int WEIGHT_SEARCH_RESULTS = 2;
    public static final int WEIGHT_SEARCH_TERMS = 1;
    public static final int WEIGHT_TAG_CLOUD = 1;
    public static final int WEIGHT_USER_ACCOUNT = 1;
    public static final int WEIGHT_CROWDSOURCING_OVERVIEW = 3;
    public static final int WEIGHT_CROWDSOURCING_EDIT_OVERVIEW = 4;
    public static final int WEIGHT_CROWDSOURCING_EDIT_OCR_CONTENTS = 5;

    protected static final String KEY_CURRENT_VIEW = "currentView";
    protected static final String KEY_PREFERRED_VIEW = "preferredView";
    protected static final String KEY_CURRENT_PARTNER_PAGE = "preferredView";
    protected static final String KEY_SELECTED_NEWS_ARTICLE = "selectedNewsArticle";
    protected static final String KEY_MENU_PAGE = "menuPage";
    protected static final String KEY_SUBTHEME_DISCRIMINATOR_VALUE = "subThemeDicriminatorValue";

    private static final String HOME_PAGE = "index";
    private static final String SEARCH_PAGE = "search";
    private static final String BROWSE_PAGE = "browse";

    private Locale locale = Locale.ENGLISH;

    /** Map for setting any navigation status variables. Replaces currentView, etc. */
    protected Map<String, String> statusMap = new HashMap<>();

    private String theme = "";

    /** Currently selected page from the main navigation bar. */
    private String currentPage = "index";

    private List<LabeledLink> breadcrumbs = new LinkedList<>();

    private boolean isCmsPage = false;

    /** Empty constructor. */
    public NavigationHelper() {
        // the emptiness inside
    }

    @PostConstruct
    public void init() {
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getViewRoot() != null) {
            locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
        } else {
            locale = Locale.GERMAN;
            logger.warn("Could not access FacesContext, locale set to DE.");
        }
        theme = DataManager.getInstance().getConfiguration().getTheme();
        statusMap.put(KEY_CURRENT_PARTNER_PAGE, "");
        statusMap.put(KEY_SELECTED_NEWS_ARTICLE, "");
        statusMap.put(KEY_MENU_PAGE, "user");
    }

    public String searchPage() {
        this.setCurrentPage(SEARCH_PAGE);
        return SEARCH_PAGE;
    }

    public String homePage() {
        this.setCurrentPage(HOME_PAGE);
        return HOME_PAGE;
    }

    public String browsePage() {
        this.setCurrentPage(BROWSE_PAGE);
        return BROWSE_PAGE;
    }

    public String getCurrentPage() {
        return currentPage;
    }

    /**
     * @return the isCmsPage
     */
    public boolean isCmsPage() {
        return isCmsPage;
    }

    /**
     * @param isCmsPage the isCmsPage to set
     */
    public void setCmsPage(boolean isCmsPage) {
        this.isCmsPage = isCmsPage;
    }

    /**
     * 
     * @param currentPage
     */
    public void setCurrentPage(String currentPage) {
        setCurrentPage(currentPage, false, false);
    }

    public void setCurrentPage(String currentPage, boolean resetBreadcrubs, boolean resetCurrentDocument) {
        setCurrentPage(currentPage, resetBreadcrubs, resetCurrentDocument, false);
    }

    /**
     *
     * @param currentPage
     * @param resetBreadcrubs
     * @param resetCurrentDocument
     */
    public void setCurrentPage(String currentPage, boolean resetBreadcrubs, boolean resetCurrentDocument, boolean setCmsPage) {
        logger.trace("setCurrentPage: {}", currentPage);
        if (resetBreadcrubs) {
            resetBreadcrumbs();
        }
        if (resetCurrentDocument) {
            resetCurrentDocument();
        }

        //        this.savePageUrl();

        setCmsPage(setCmsPage);
        this.currentPage = currentPage;
    }

    public void setCurrentBreadcrumbPage(String pageName, String pageWeight, String pageURL) {
        // logger.debug("Current Breadcrumb Page: {}", pageName);
        // logger.debug("pageWeight: {}", pageWeight);
        // logger.debug("pageURL: {}", pageURL);
        resetBreadcrumbs();
        // logger.debug("pageNameTranslation: {}", pageNameTranslation);
        updateBreadcrumbs(new LabeledLink(pageName, BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + pageURL, Integer.valueOf(pageWeight)));
        this.currentPage = pageName;

    }

    /**
     * @param currentPartnerPage
     * @should set value correctly
     */
    public void setCurrentPartnerPage(String currentPartnerPage) {
        statusMap.put(KEY_CURRENT_PARTNER_PAGE, currentPartnerPage);
        logger.trace("current Partner Page: {}", currentPartnerPage);
    }

    /**
     * @return
     * @should return value correctly
     */
    public String getCurrentPartnerPage() {
        return statusMap.get(KEY_CURRENT_PARTNER_PAGE);
    }

    /**
     * Returns the manually selected view type (will be used for search result browsing, if set).
     *
     * @should return value correctly
     */
    public String getPreferredView() {
        return statusMap.get(KEY_PREFERRED_VIEW);
    }

    /**
     * Sets the manually selected view type (will be used for search result browsing, if set).
     *
     * @should set value correctly
     */
    public void setPreferredView(String preferredView) {
        statusMap.put(KEY_PREFERRED_VIEW, preferredView);
    }

    public void setCurrentPageIndex() {
        setCurrentPage("index", true, true);
    }

    public void setCurrentPageSearch() {
        setCurrentPage("search", true, true);
        updateBreadcrumbs(
                new LabeledLink("search", BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/search/", NavigationHelper.WEIGHT_SEARCH));
    }

    public void setCurrentPageBrowse() {
        setCurrentPage("browse", true, true);
        updateBreadcrumbs(new LabeledLink("browseCollection", BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/browse/",
                NavigationHelper.WEIGHT_BROWSE));
    }

    public void setCurrentPageTags() {
        setCurrentPage("tags", true, true);
        updateBreadcrumbs(
                new LabeledLink("tagclouds", BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/tags/", NavigationHelper.WEIGHT_TAG_CLOUD));
    }

    public void setCurrentPageStatistics() {
        setCurrentPage("statistics", true, true);
        updateBreadcrumbs(new LabeledLink("statistics", BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/statistics/",
                NavigationHelper.WEIGHT_TAG_MAIN_MENU));
    }

    public void setCurrentPageUser() {
        setCurrentPage("user", false, true);
    }

    public void setCurrentPageAdmin(String pageName) {
        resetBreadcrumbs();
        resetCurrentDocument();
        if (pageName != null && !pageName.trim().isEmpty()) {
            this.currentPage = pageName;
        } else {
            this.currentPage = "adminAllUsers";
        }

    }

    public void setCurrentPageAdmin() {
        setCurrentPageAdmin("adminAllUsers");
    }

    public void setCurrentPageSites() {
        setCurrentPage("sites", true, true);
    }

    public void setCurrentPageTimeMatrix() {
        setCurrentPage("timeMatrix", true, true);

    }

    public void setCurrentPageSearchTermList() {
        setCurrentPage("searchTermList", false, true);
    }

    public void resetCurrentPage() {
        setCurrentPage(null, true, true);
    }

    public String getViewAction(String view) {
        return view;
    }

    /**
     * @return the currentView
     * @should return value correctly
     */
    public String getCurrentView() {
        return statusMap.get(KEY_CURRENT_VIEW);
    }

    /**
     * Sets the currently selected content view name.
     *
     * @param currentView
     * @should set value correctly
     */
    public void setCurrentView(String currentView) {
        logger.trace("{}: {}", KEY_CURRENT_VIEW, currentView);
        statusMap.put(KEY_CURRENT_VIEW, currentView);
        setCurrentPage(currentView);
    }

    public Locale getLocale() {
        return locale;
    }

    public String getLocaleString() {
        return locale.getLanguage();
    }

    public Iterator<Locale> getSupportedLocales() {
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getApplication() != null) {
            return FacesContext.getCurrentInstance().getApplication().getSupportedLocales();
        }

        return null;
    }

    /**
     * Returns ISO 639-1 language codes of available JSF locales.
     * 
     * @return
     */
    public List<String> getSupportedLanguages() {
        List<String> ret = new ArrayList<>();

        Iterable<Locale> locales = () -> getSupportedLocales();
        StreamSupport.stream(locales.spliterator(), false)
                //                .peek(language -> logger.trace("Adding sort field: {}", language))
                .forEach(locale -> ret.add(locale.getLanguage()));

        return ret;
    }

    /**
     * 
     * @param inLocale
     */
    public void setLocaleString(String inLocale) {
        logger.trace("setLocaleString: {}", inLocale);
        locale = new Locale(inLocale);
        FacesContext.getCurrentInstance().getViewRoot().setLocale(locale);

        // Also set ActiveDocumentBean.selectedRecordLanguage, if it's configured to match the locale
        if (DataManager.getInstance().getConfiguration().isUseViewerLocaleAsRecordLanguage()) {
            ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
            if (adb != null) {
                adb.setSelectedRecordLanguage(inLocale);
            }
        }
    }

    public String getDatePattern() {
        switch (locale.getLanguage()) {
            case "en":
                return "MM/dd/yyyy";
            case "es":
                return "dd/MM/yyyy";
            default:
                return "dd.MM.yyyy";
        }
    }

    public void reload() {
    }

    public String getVersion() {
        return Helper.getVersion();
    }

    public String getApplicationUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/";
    }

    /**
     * Used for social bookmarks.
     */
    public String getEncodedUrl() {
        try {
            return URLEncoder.encode(getRequestPath(FacesContext.getCurrentInstance().getExternalContext()), "ASCII");
        } catch (UnsupportedEncodingException e) {
            logger.warn("Not possible to encode URL", e);
        }
        return "";
    }

    public String getCurrentUrl() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        if (request != null) {
            URL url = PrettyContext.getCurrentInstance(request).getRequestURL();
            if (url != null) {
                return getApplicationUrl() + url.toURL().substring(1);
            }
        }
        return null;
    }

    public String getRssUrl() {
        try {
            return URL_RSS + "/?lang=" + CmsBean.getCurrentLocale().getLanguage();
        } catch (NullPointerException e) {
            return URL_RSS;
        }
    }

    /**
     *
     * @return the complete Request Path, eg http://hostname.de/viewer/pathxyz/pathxyz/
     */
    public String getRequestPath(ExternalContext externalContext) {
        ExternalContext exContext = externalContext;
        HttpServletRequest request = (HttpServletRequest) exContext.getRequest();
        // Request path for PrettyURL, e.g.
        // http://hostname.com/viewer/image/xyz/
        String prettyFacesURI = (String) exContext.getRequestMap().get(RequestDispatcher.FORWARD_REQUEST_URI);
        return getRequestPath(request, prettyFacesURI);
    }

    public static String getRequestPath(HttpServletRequest request, String prettyFacesURI) {
        String requestPath = "";
        if (StringUtils.isEmpty(prettyFacesURI)) {
            // The standard request, the prettyFacesURI is empty for sites
            // without a prettyurl mapping, e.g. index.xhtml, browse.xhtml.
            requestPath = request.getRequestURI();
        } else {
            requestPath = prettyFacesURI;
        }

        String scheme = request.getScheme(); // http
        String serverName = request.getServerName(); // hostname.com
        int serverPort = request.getServerPort(); // 80
        String baseURL = "";
        if (serverPort != 80) {
            baseURL = scheme + "://" + serverName + ":" + serverPort;
        } else {
            baseURL = scheme + "://" + serverName;
        }

        // logger.debug("RequestPath: {}{}", baseURL, requestPath);
        return baseURL + requestPath;
    }

    public static String getFullRequestUrl(HttpServletRequest request, String prettyFacesURI) {
        if (StringUtils.isEmpty(prettyFacesURI)) {
            return getRequestPath(request, prettyFacesURI) + "?" + request.getQueryString();
        }

        return getRequestPath(request, prettyFacesURI);
    }

    /**
     * Returns the current PrettyURL.
     *
     * @return
     */
    public String getCurrentPrettyUrl() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        URL url = PrettyContext.getCurrentInstance(request).getRequestURL();
        return ServletUtils.getServletPathWithHostAsUrlFromRequest(request) + url.toURL();
    }

    public TimeZone getTimeZone() {
        return TimeZone.getDefault();
    }

    /**
     * @param page
     * @should set value correctly
     */
    public void setMenuPage(String page) {
        logger.debug("Menu Page ist: " + page);
        statusMap.put(KEY_MENU_PAGE, page);
    }

    /**
     * @return
     * @should return value correctly
     */
    public String getMenuPage() {
        return statusMap.get(KEY_MENU_PAGE);
    }

    /**
     * @return the activePartnerId
     * @should return value correctly
     */
    @Deprecated
    public String getActivePartnerId() {
        return statusMap.get(KEY_SUBTHEME_DISCRIMINATOR_VALUE);
    }

    /**
     * @param activePartnerId the activePartnerId to set
     * @should reset current partner page
     * @should set value correctly
     */
    @Deprecated
    public void setActivePartnerId(String activePartnerId) {
        statusMap.put(KEY_CURRENT_PARTNER_PAGE, "");
        if ("-".equals(activePartnerId)) {
            activePartnerId = "";
        }
        logger.trace("setActivePartnerId: {}", activePartnerId);
        statusMap.put(KEY_SUBTHEME_DISCRIMINATOR_VALUE, activePartnerId);
    }

    /**
     * @should reset value correctly
     */
    @Deprecated
    public void resetActivePartnerId() {
        if (DataManager.getInstance().getConfiguration().isSubthemeAutoSwitch()) {
            logger.trace("resetActivePartnerId");
            statusMap.put(KEY_SUBTHEME_DISCRIMINATOR_VALUE, "");
        }
    }

    public String getTheme() {
        return theme;
    }

    /**
     * Returns the value of the configured sub-theme discriminator field. The value can be set via
     * <code>setSubThemeDiscriminatorValue(java.lang.String)</code> (e.g. via PrettyFacces). If a record is currently loaded and has a
     * dicriminatorField:discriminatorValue pair, the currently set value is replaced with that from the record.
     *
     * @return
     * @throws IndexUnreachableException
     */
    public String getSubThemeDiscriminatorValue() throws IndexUnreachableException {

        if (DataManager.getInstance().getConfiguration().isSubthemeAutoSwitch()) {
            // Automatically set the sub-theme discriminator value to the
            // current record's value, if configured to do so
            ActiveDocumentBean activeDocumentBean = BeanUtils.getActiveDocumentBean();
            if (activeDocumentBean != null) {
                String subThemeDiscriminatorValue = "";
                if (activeDocumentBean.getViewManager() != null && getCurrentPagerType().isDocumentPage()) {
                    // If a record is loaded, get the value from the record's value
                    // in discriminatorField

                    String discriminatorField = DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField();
                    subThemeDiscriminatorValue = activeDocumentBean.getViewManager().getActiveDocument().getMetadataValue(discriminatorField);
                    if (StringUtils.isNotEmpty(subThemeDiscriminatorValue)) {
                        logger.trace("Setting discriminator value from open record: '{}'", subThemeDiscriminatorValue);
                        statusMap.put(KEY_SUBTHEME_DISCRIMINATOR_VALUE, subThemeDiscriminatorValue);
                    }
                } else if (isCmsPage()) {
                    CmsBean cmsBean = BeanUtils.getCmsBean();
                    if (cmsBean != null && cmsBean.getCurrentPage() != null) {
                        subThemeDiscriminatorValue = cmsBean.getCurrentPage().getSubThemeDiscriminatorValue();
                        if (StringUtils.isNotEmpty(subThemeDiscriminatorValue)) {
                            logger.trace("Setting discriminator value from cms page: '{}'", subThemeDiscriminatorValue);
                            return subThemeDiscriminatorValue;
                        }
                    }
                }
            }
        }

        String ret = StringUtils.isNotEmpty(statusMap.get(KEY_SUBTHEME_DISCRIMINATOR_VALUE)) ? statusMap.get(KEY_SUBTHEME_DISCRIMINATOR_VALUE) : "-";
        //         logger.trace("getSubThemeDiscriminatorValue: {}", ret);
        return ret;
    }

    /**
     *
     * @param subThemeDiscriminatorValue
     * @should set value correctly
     */
    public void setSubThemeDiscriminatorValue(String subThemeDiscriminatorValue) {
        logger.trace("setSubThemeDiscriminatorValue: {}", subThemeDiscriminatorValue);
        // If a new discriminator value has been selected, the visible
        // collection list must be generated anew
        if ((subThemeDiscriminatorValue == null && statusMap.get(KEY_SUBTHEME_DISCRIMINATOR_VALUE) != null)
                || (subThemeDiscriminatorValue != null && !subThemeDiscriminatorValue.equals(statusMap.get(KEY_SUBTHEME_DISCRIMINATOR_VALUE)))) {
            statusMap.put(KEY_SUBTHEME_DISCRIMINATOR_VALUE, subThemeDiscriminatorValue);
            BrowseBean browseBean = BeanUtils.getBrowseBean();
            if (browseBean != null) {
                browseBean.resetAllLists();
            }
            CalendarBean calendarBean = BeanUtils.getCalendarBean();
            if (calendarBean != null) {
                try {
                    calendarBean.resetYears();
                } catch (PresentationException e) {
                    logger.debug("PresentationException thrown here: {}", e.getMessage());
                } catch (IndexUnreachableException e) {
                    logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                }
            }
        } else {
            statusMap.put(KEY_SUBTHEME_DISCRIMINATOR_VALUE, subThemeDiscriminatorValue);
        }
    }

    public void changeTheme() throws IndexUnreachableException {
        if (DataManager.getInstance().getConfiguration().isSubthemesEnabled()) {
            String discriminatorField = DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField();
            String discriminatorValue = getSubThemeDiscriminatorValue();
            if (StringUtils.isNotEmpty(discriminatorValue) && !"-".equals(discriminatorValue)) {
                logger.trace("Using discriminator value: {}", discriminatorValue);
                theme = DataManager.getInstance().getConfiguration().getTheme();
            } else {
                theme = DataManager.getInstance().getConfiguration().getTheme();
                logger.trace("Using default theme");
            }
        }
        logger.debug("theme: {}", theme);
    }

    public void resetTheme() {
        logger.trace("resetTheme");
        theme = DataManager.getInstance().getConfiguration().getTheme();
        if (DataManager.getInstance().getConfiguration().isSubthemeAutoSwitch()) {
            setSubThemeDiscriminatorValue(null);
        }
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    /**
     * 
     * @deprecated Use ConfigurationBean.isAddDublinCoreTags()
     */
    @Deprecated
    public boolean isHtmlHeadDCMetadata() {
        return DataManager.getInstance().getConfiguration().isAddDublinCoreMetaTags();
    }

    public String getOverviewUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewOverview.getName();
    }

    public String getOverviewActiveUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/!" + PageType.viewOverview.getName();
    }

    public String getObjectUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewObject.getName();
    }

    public String getImageUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewImage.getName();
    }

    public String getImageActiveUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/!" + PageType.viewImage.getName();
    }

    public String getReadingModeUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewReadingMode.getName();
    }

    /**
     * This method checks the Solr height attribute of the current page. If this is > 0, than the current page is displayed with OpenLayers
     *
     * @return the path which viewImageFullscreen.xhtml the user should see for the current page.
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ConfigurationException
     */
    public String getViewImagePathFullscreen() throws IndexUnreachableException, DAOException, ConfigurationException {
        String imageDisplayType = DataManager.getInstance().getConfiguration().getZoomFullscreenViewType();
        logger.trace("Detected display mode: {}", imageDisplayType);
        if (StringUtils.isNotEmpty(imageDisplayType)) {
            // MIX data exists
            if (imageDisplayType.equalsIgnoreCase("openlayersimage") && BeanUtils.getActiveDocumentBean().getViewManager() != null
                    && BeanUtils.getActiveDocumentBean().getViewManager().getCurrentPage().getPhysicalImageHeight() > 0) {
                String path =
                        "/resources/themes/" + DataManager.getInstance().getConfiguration().getTheme() + "/urlMappings/viewImageFullscreen.xhtml";
                logger.debug("MIX data detected. Redirect to the Fullscreen view  (viewImageFullscreen.xhtml) of the "
                        + DataManager.getInstance().getConfiguration().getTheme() + " theme.");
                return path;
            }
            if (imageDisplayType.equalsIgnoreCase("classic")) {
                logger.debug("No MIX data detected. Redirect to the normal /viewImageFullscreen.xhtml.");
                return "/viewImageFullscreen.xhtml";
            }
        }
        logger.error("No correct configuration, use the standard Fullscreen Image view. Detected: " + imageDisplayType
                + " from <zoomFullscreenView/> in the config_viewer.xml.");

        return "/viewImageFullscreen.xhtml";
    }

    public String getCalendarUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewCalendar.getName();
    }

    public String getCalendarActiveUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/!" + PageType.viewCalendar.getName();
    }

    public String getTocUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewToc.getName();
    }

    public String getTocActiveUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/!" + PageType.viewToc.getName();
    }

    public String getThumbsUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewThumbs.getName();
    }

    public String getThumbsActiveUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/!" + PageType.viewThumbs.getName();
    }

    public String getMetadataUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewMetadata.getName();
    }

    public String getMetadataActiveUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/!" + PageType.viewMetadata.getName();
    }

    public String getFulltextUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewFulltext.getName();
    }

    public String getFulltextActiveUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/!" + PageType.viewFulltext.getName();
    }

    public String getSearchUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.search.getName();
    }

    public String getAdvancedSearchUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.advancedSearch.getName();
    }

    public String getSearchUrl(int activeSearchType) {

        //If we are on a cms-page, return the cms page url
        try {
            Optional<ViewerPath> oView = ViewHistory.getCurrentView(BeanUtils.getRequest());
            if (oView.isPresent()) {
                String path =
                        BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + oView.get().getPagePath().toString().replaceAll("\\+", "/");
                return path;
            }
        } catch (Throwable e) {
            logger.error(e.toString(), e);
        }

        switch (activeSearchType) {
            case SearchHelper.SEARCH_TYPE_ADVANCED:
                return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.advancedSearch.getName();
            default:
                return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.search.getName();
        }
    }

    public String getTermUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.term.getName();
    }

    public String getBrowseUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.browse.getName();

    }

    public String getSortUrl() {
        return getSearchUrl();
    }

    /**
     * @return the breadcrumbs
     */
    public List<LabeledLink> getBreadcrumbs() {
        return Collections.synchronizedList(this.breadcrumbs);
    }

    /**
     * Returns the bottom breadcrumb. Used to return to the previous page from the errorGeneral page.
     *
     * @return
     */
    public LabeledLink getLastBreadcrumb() {
        List<LabeledLink> breadcrumbs = Collections.synchronizedList(this.breadcrumbs);
        synchronized (breadcrumbs) {
            if (breadcrumbs != null && !breadcrumbs.isEmpty()) {
                return breadcrumbs.get(breadcrumbs.size() - 1);
            }

            return null;
        }
    }

    public void updateBreadcrumbs(CMSPage cmsPage) throws DAOException {
        resetBreadcrumbs();
        Set<CMSPage> linkedPages = new HashSet<>();
        List<LabeledLink> tempBreadcrumbs = new ArrayList<>();
        CMSPage currentPage = cmsPage;
        
        
        //If the current cms page contains a collection and we are in a subcollection of it, attempt to add a breadcrumb link for the subcollection
        try {
            if(cmsPage.getCollection() != null && cmsPage.getCollection().isSubcollection()) {
                LabeledLink link = new LabeledLink(cmsPage.getCollection().getTopVisibleElement(), cmsPage.getCollection().getCollectionUrl(cmsPage.getCollection().getTopVisibleElement()), 0);
                tempBreadcrumbs.add(0, link);
            }
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error(e.toString(), e);
        }
        
        while (currentPage != null) {
            if (linkedPages.contains(currentPage)) {
                //encountered a breadcrumb loop. Simply break here
                return;
            }
            linkedPages.add(currentPage);
            if (DataManager.getInstance()
                    .getDao()
                    .getStaticPageForCMSPage(currentPage)
                    .map(sp -> sp.getPageName())
                    .filter(name -> PageType.index.name().equals(name))
                    .isPresent()) {
                //            if (PageType.index.matches(cmsPage.getStaticPageName())) {
                //The current page is the start page. No need to add further breadcrumbs
                return;
            }
            LabeledLink pageLink = new LabeledLink(currentPage.getMenuTitle(), currentPage.getPageUrl(), 0);
            tempBreadcrumbs.add(0, pageLink);
            if (StringUtils.isNotBlank(currentPage.getParentPageId())) {
                try {
                    Long cmsPageId = Long.parseLong(currentPage.getParentPageId());
                    currentPage = DataManager.getInstance().getDao().getCMSPage(cmsPageId);
                } catch (NumberFormatException | DAOException e) {
                    logger.error("CMS breadcrumb creation: Parent page of page " + currentPage.getId() + " is not a valid page id");
                    currentPage = null;
                }
            } else {
                currentPage = null;
            }

            
        }
        List<LabeledLink> breadcrumbs = Collections.synchronizedList(this.breadcrumbs);
        synchronized (breadcrumbs) {
            tempBreadcrumbs.forEach(bc -> breadcrumbs.add(bc));
        }
    }

    /**
     * Attaches a new link to the breadcrumb list at the appropriate position (depending on the link's weight).
     *
     * @param newLink The breadcrumb link to add.
     */
    public void updateBreadcrumbs(LabeledLink newLink) {
        List<LabeledLink> breadcrumbs = Collections.synchronizedList(this.breadcrumbs);
        synchronized (breadcrumbs) {
            // Always add the home page if there are no breadcrumbs
            if (breadcrumbs.isEmpty()) {
                resetBreadcrumbs();
            }
            logger.trace("Adding breadcrumb: {} ({})", newLink.getUrl(), newLink.getWeight());
            // Determine the position at which to add the new link
            int position = breadcrumbs.size();
            for (int i = 0; i < breadcrumbs.size(); ++i) {
                LabeledLink link = breadcrumbs.get(i);
                if (link.getWeight() >= newLink.getWeight()) {
                    position = i;
                    break;
                }
            }
            breadcrumbs.add(position, newLink);
            // Remove any following links
            if (position < breadcrumbs.size()) {
                try {
                    breadcrumbs.subList(position + 1, breadcrumbs.size()).clear();
                } catch (NullPointerException e) {
                    // This throws a NPE sometimes
                }
            }
            // logger.trace("breadcrumbs: " + breadcrumbs.size() + " " +
            // breadcrumbs.toString());
        }
    }

    /**
     * Empties the breadcrumb list and adds a link to the start page.
     */
    private void resetBreadcrumbs() {
        // logger.trace("reset breadcrumbs");
        List<LabeledLink> breadcrumbs = Collections.synchronizedList(this.breadcrumbs);
        synchronized (breadcrumbs) {
            breadcrumbs.clear();
            breadcrumbs.add(new LabeledLink("home", BeanUtils.getServletPathWithHostAsUrlFromJsfContext(), 0));
        }
    }

    /**
     * Adds a link to the breadcrumbs using the current PrettyURL. Can be called from XHTML.
     *
     * @param linkName
     * @param linkWeight
     */
    public void addStaticLinkToBreadcrumb(String linkName, int linkWeight) {
        addStaticLinkToBreadcrumb(linkName, getCurrentPrettyUrl(), linkWeight);
    }

    /**
     * Adds a link to the breadcrumbs using the given URL. Can be called from XHTML.
     *
     * @param linkName
     * @param linkWeight
     */
    public void addStaticLinkToBreadcrumb(String linkName, String url, int linkWeight) {
        PageType page = PageType.getByName(url);
        if (page != null && !page.equals(PageType.other)) {
            url = getUrl(page);
        } else {
        }
        LabeledLink newLink = new LabeledLink(linkName, url, linkWeight);
        updateBreadcrumbs(newLink);
    }

    /**
     * @param page
     * @return
     */
    private String getUrl(PageType page) {
        return getApplicationUrl() + page.getName();
    }

    public String getCurrentPartnerUrl() {
        logger.trace("activePartnerId: {}", statusMap.get(KEY_SUBTHEME_DISCRIMINATOR_VALUE));
        logger.trace("currentPartnerPage: {}", statusMap.get(KEY_CURRENT_PARTNER_PAGE));
        if (StringUtils.isEmpty(statusMap.get(KEY_SUBTHEME_DISCRIMINATOR_VALUE))) {
            return "/index.xhtml";
        }
        if (StringUtils.isEmpty(statusMap.get(KEY_CURRENT_PARTNER_PAGE))
                || statusMap.get(KEY_CURRENT_PARTNER_PAGE).equalsIgnoreCase("RES_NOT_FOUND")) {
            return "/resources/themes/" + theme + "/" + statusMap.get(KEY_SUBTHEME_DISCRIMINATOR_VALUE) + "/index.xhtml";
        }
        if ("index".equals(statusMap.get(KEY_CURRENT_PARTNER_PAGE))) {
            statusMap.put(statusMap.get(KEY_CURRENT_PARTNER_PAGE), "index.xhtml");
        }
        String tmp = statusMap.get(KEY_CURRENT_PARTNER_PAGE);
        logger.trace("******************************** {} ", statusMap.get(KEY_CURRENT_PARTNER_PAGE));
        statusMap.put(statusMap.get(KEY_CURRENT_PARTNER_PAGE), "index");
        return "/resources/themes/" + theme + "/" + statusMap.get(KEY_SUBTHEME_DISCRIMINATOR_VALUE) + "/" + tmp;

    }

    /**
     * Returns the string representation of the given <code>Date</code> based on the current <code>locale</code>.
     *
     * @param date
     * @return
     */
    public String getLocalDate(Date date) {
        return DateTools.getLocalDate(date, locale.getLanguage());
    }

    public List<String> getMessageValueList(String keyPrefix) {
        List<String> sortetList = ViewerResourceBundle.getMessagesValues(locale, keyPrefix);
        Collections.reverse(sortetList);

        return sortetList;
    }

    /**
     *
     * @param art
     * @should set value correctly
     */
    public void setSelectedNewsArticle(String art) {
        statusMap.put(KEY_SELECTED_NEWS_ARTICLE, art);
    }

    /**
     * @return
     * @should return value correctly
     */
    public String getSelectedNewsArticle() {
        return statusMap.get(KEY_SELECTED_NEWS_ARTICLE);
    }

    /**
     * Purges all traces of the currently loaded record from ActiveDocumentBean
     */
    private static void resetCurrentDocument() {
        ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
        if (adb != null) {
            adb.reset();
        }

        // Module augmentations
        for (IModule module : DataManager.getInstance().getModules()) {
            try {
                module.augmentResetRecord();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public String getLastRequestTimestamp() {
        return (String) BeanUtils.getRequest().getSession(false).getAttribute("lastRequest");
    }

    /**
     * @param key
     * @return
     * @should return value correctly
     */
    public String getStatusMapValue(String key) {
        return statusMap.get(key);
    }

    /**
     * @param key
     * @param value
     * @should set value correctly
     */
    public void setStatusMapValue(String key, String value) {
        statusMap.put(key, value);
    }

    /**
     * @return the statusMap
     */
    public Map<String, String> getStatusMap() {
        return statusMap;
    }

    /**
     * @param statusMap the statusMap to set
     */
    public void setStatusMap(Map<String, String> statusMap) {
        this.statusMap = statusMap;
    }

    /**
     * Returns the translation for the given <code>msgKey</code> and replaces all {i} placeholders with values from the given <code>params</code>.
     *
     * @param msgKey
     * @param params One or more parameter values to replace the placeholders.
     * @return
     */
    public String getTranslationWithParams(String msgKey, String... params) {
        String msg = Helper.getTranslation(msgKey, null);
        if (params != null) {
            for (int i = 0; i < params.length; ++i) {
                msg = msg.replace(new StringBuilder("{").append(i).append("}").toString(), params[i]);
            }
        }

        return msg;
    }

    public String getTranslation(String msgKey, String language) {
        String msg = Helper.getTranslation(msgKey, language != null ? Locale.forLanguageTag(language) : null);

        return msg;
    }

    /**
     * Checks if the current page displays document information, solely based on the String getCurrentPage() The Pages for which this method should
     * return true are set in the PageType class.
     *
     * @return
     */
    public boolean isDocumentPage() {
        PageType page = PageType.getByName(getCurrentPage());
        if (page != null) {
            return page.isDocumentPage();
        }
        return false;
    }

    /**
     * 
     * @return
     * @throws IndexUnreachableException
     */
    public String getSubThemeDiscriminatorQuerySuffix() throws IndexUnreachableException {
        return SearchHelper.getDiscriminatorFieldFilterSuffix(this, DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField());
    }

    /**
     * @return
     */
    public PageType getCurrentPagerType() {
        return PageType.getByName(getCurrentPage());
    }

    public String getPreviousViewUrl() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String previousUrl = ViewHistory.getPreviousView(request).map(path -> (path.getCombinedUrl())).orElse("");
        if (StringUtils.isBlank(previousUrl)) {
            previousUrl = getApplicationUrl();
        }
        return previousUrl;
    }

    public void redirectToPreviousView() throws IOException {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String previousUrl = ViewHistory.getPreviousView(request)
                .map(path -> path.getApplicationUrl() + path.getCombinedPrettyfiedUrl())
                .map(path -> path.replaceAll("\\/index\\/?", "\\/"))
                .orElse("");
        if (StringUtils.isBlank(previousUrl)) {
            previousUrl = homePage();
        }
        ViewHistory.redirectToUrl(previousUrl);

    }

    public String getCurrentViewUrl() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String previousUrl = ViewHistory.getCurrentView(request).map(path -> (path.getCombinedUrl())).orElse("");
        if (StringUtils.isBlank(previousUrl)) {
            previousUrl = getApplicationUrl();
        }
        return previousUrl;
    }

    public void redirectToCurrentView() throws IOException {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String previousUrl = ViewHistory.getCurrentView(request)
                .map(path -> path.getApplicationUrl() + path.getCombinedPrettyfiedUrl())
                .map(path -> path.replaceAll("\\/index\\/?", "\\/"))
                .orElse("");
        if (StringUtils.isBlank(previousUrl)) {
            previousUrl = homePage();
        }
        ViewHistory.redirectToUrl(previousUrl);

    }

}
