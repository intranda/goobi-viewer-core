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
package io.goobi.viewer.managedbeans;

import java.awt.ComponentOrientation;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.faces.config.mapping.PathParameter;
import com.ocpsoft.pretty.faces.url.URL;

import io.goobi.viewer.Version;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.FileResourceManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RedirectException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.CMSStaticPage;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.urlresolution.ViewHistory;
import io.goobi.viewer.model.urlresolution.ViewerPath;
import io.goobi.viewer.model.urlresolution.ViewerPathBuilder;
import io.goobi.viewer.model.viewer.CollectionLabeledLink;
import io.goobi.viewer.model.viewer.LabeledLink;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.ViewManager;
import io.goobi.viewer.model.viewer.collections.CollectionView;
import io.goobi.viewer.modules.IModule;
import io.goobi.viewer.servlets.utils.ServletUtils;
import io.goobi.viewer.solr.SolrConstants;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

/**
 * JSF session-scoped backing bean providing navigation state, URL building, and breadcrumb
 * tracking for the viewer frontend. Initialised via {@code @PostConstruct init()} which resolves
 * the user's locale from the current JSF view root and seeds the status map with default values.
 *
 * <p><b>Lifecycle:</b> Created once per HTTP session; survives across page navigations and is
 * destroyed when the session expires.
 *
 * <p><b>Thread safety:</b> Not explicitly synchronised; all state is expected to be accessed
 * from the JSF request thread of the owning session only.
 */
@Named
@SessionScoped
public class NavigationHelper implements Serializable {

    private static final List<String> BROWSER_IMAGE_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp", "gif", "svg");

    private static final int MAX_HTML_ID_LENGTH = 100;

    private static final long serialVersionUID = 4171362984701032679L;

    private static final Logger logger = LogManager.getLogger(NavigationHelper.class);

    private static final String URL_RSS = "rss";

    @Inject
    private BreadcrumbBean breadcrumbBean;
    @Inject
    private CmsBean cmsBean;

    /** Constant <code>KEY_CURRENT_VIEW="currentView"</code>. */
    protected static final String KEY_CURRENT_VIEW = "currentView";
    /** Constant <code>KEY_PREFERRED_VIEW="preferredView"</code>. */
    protected static final String KEY_PREFERRED_VIEW = "preferredView";
    /** Constant <code>KEY_CURRENT_PARTNER_PAGE="preferredView"</code> */
    /** Constant <code>KEY_SELECTED_NEWS_ARTICLE="selectedNewsArticle"</code>. */
    protected static final String KEY_SELECTED_NEWS_ARTICLE = "selectedNewsArticle";
    /** Constant <code>KEY_MENU_PAGE="menuPage"</code>. */
    protected static final String KEY_MENU_PAGE = "menuPage";
    /** Constant <code>KEY_SUBTHEME_DISCRIMINATOR_VALUE="subThemeDicriminatorValue"</code>. */
    protected static final String KEY_SUBTHEME_DISCRIMINATOR_VALUE = "subThemeDicriminatorValue";

    private static final String HOME_PAGE = "index";
    private static final String SEARCH_PAGE = "search";
    private static final String SEARCH_TERM_LIST_PAGE = "searchTermList";
    private static final String BROWSE_PAGE = "browse";
    private static final String TAGS_PAGE = "tags";

    private Locale locale = Locale.ENGLISH;

    /** Map for setting any navigation status variables. Replaces currentView, etc. */
    private Map<String, String> statusMap = new HashMap<>();

    private final String theme;

    /** Currently selected page from the main navigation bar. */
    private String currentPage = HOME_PAGE;

    private boolean isCmsPage = false;

    private final FileResourceManager fileResourceManager;

    /**
     * Empty constructor.
     */
    public NavigationHelper() {
        theme = DataManager.getInstance().getConfiguration().getTheme();
        this.fileResourceManager = DataManager.getInstance().getFileResourceManager();
    }

    public NavigationHelper(String theme, FileResourceManager fileResourceManager) {
        this.theme = theme;
        this.fileResourceManager = fileResourceManager;
    }

    public void setCmsBean(CmsBean cmsBean) {
        this.cmsBean = cmsBean;
    }

    /**
     * init.
     */
    @PostConstruct
    public void init() {
        try {
            locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
        } catch (NullPointerException e) {
            locale = ViewerResourceBundle.getFallbackLocale();
        }
        statusMap.put(KEY_SELECTED_NEWS_ARTICLE, "");
        statusMap.put(KEY_MENU_PAGE, "user");
    }

    /**
     * Required setter for ManagedProperty injection.
     *
     * @param breadcrumbBean breadcrumb bean to inject
     */
    public void setBreadcrumbBean(BreadcrumbBean breadcrumbBean) {
        this.breadcrumbBean = breadcrumbBean;
    }

    /**
     * searchPage.
     *
     * @return the search page name after setting it as the current navigation page
     */
    public String searchPage() {
        this.setCurrentPage(SEARCH_PAGE);
        return SEARCH_PAGE;
    }

    /**
     * homePage.
     *
     * @return the home page name after setting it as the current navigation page
     */
    public String homePage() {
        this.setCurrentPage(HOME_PAGE);
        return HOME_PAGE;
    }

    /**
     * browsePage.
     *
     * @return the browse page name after setting it as the current navigation page
     */
    public String browsePage() {
        this.setCurrentPage(BROWSE_PAGE);
        return BROWSE_PAGE;
    }

    /**
     * Getter for the field <code>currentPage</code>.
     *
     * @return the name of the currently active navigation page, with quotes escaped
     */
    public String getCurrentPage() {
        return StringTools.escapeQuotes(currentPage);
    }

    /**
     * isCmsPage.
     *
     * @return true if the current page is a CMS page, false otherwise
     */
    public boolean isCmsPage() {
        return isCmsPage;
    }

    /**
     * setCmsPage.
     *
     * @param isCmsPage true if the current page is a CMS page
     */
    public void setCmsPage(boolean isCmsPage) {
        this.isCmsPage = isCmsPage;
    }

    /**
     * Produce an identifier string for a cms page to use for identifying the page in the navigation bar.
     *
     * @param cmsPage CMS page whose navigation identifier is to be determined
     * @return {@link String}
     */
    public static String getCMSPageNavigationId(CMSPage cmsPage) {
        try {
            Optional<CMSStaticPage> staticPage = DataManager.getInstance().getDao().getStaticPageForCMSPage(cmsPage).stream().findFirst();
            if (staticPage.isPresent()) {
                return staticPage.get().getPageName();
            }
        } catch (DAOException e) {
            //
        }
        return "cms_" + String.format("%04d", cmsPage.getId());
    }

    /**
     * 
     * @param cmsPage CMS page to set as current page
     */
    public void setCurrentPage(CMSPage cmsPage) {
        try {
            //call "setCurrentView" first, because it calls setCurrentPage which needs to be overwritten by the 
            //call to "setCurrentPage" here
            setCurrentView(cmsBean.isRelatedWorkLoaded() ? PageType.cmsPageOfWork.name() : PageType.cmsPage.name());
            setCurrentPage(getCMSPageNavigationId(cmsPage), false, !cmsBean.isRelatedWorkLoaded(), true);
        } catch (IndexUnreachableException e) {
            logger.error("Error checking if related work for cmsPage is loaded", e);
            setCurrentPage(getCMSPageNavigationId(cmsPage), false, true, true);
        }
    }

    /**
     * Setter for the field <code>currentPage</code>.
     *
     * @param currentPage page name to set as current
     */
    public void setCurrentPage(String currentPage) {
        logger.trace("setCurrentPage: {}", currentPage);
        setCurrentPage(currentPage, false, false);
    }

    /**
     * Sets the current page for the error page, mapping generic error types (general, general_no_url)
     * to the "error" page name so that the browser title shows "Fehler" instead of unrelated translations.
     *
     * <p>Specific error types (e.g. recordNotFound, download) are passed through directly so that
     * their own message keys are used as the page title.
     *
     * @param errorType the error type string set by the exception handler; may be null
     */
    public void setCurrentPageForError(String errorType) {
        if (errorType == null || "general".equals(errorType) || "general_no_url".equals(errorType)) {
            setCurrentPage("error");
        } else {
            setCurrentPage(errorType);
        }
    }

    /**
     * Setter for the field <code>currentPage</code>.
     *
     * @param currentPage page name to set as current
     * @param resetBreadcrubs if true, reset breadcrumbs
     * @param resetCurrentDocument if true, reset the loaded document
     */
    public void setCurrentPage(String currentPage, boolean resetBreadcrubs, boolean resetCurrentDocument) {
        // logger.trace("setCurrentPage: {}", currentPage); //NOSONAR Debug
        setCurrentPage(currentPage, resetBreadcrubs, resetCurrentDocument, false);
    }

    /**
     * Setter for the field <code>currentPage</code>.
     *
     * @param currentPage page name to set as current
     * @param resetBreadcrubs if true, reset breadcrumbs
     * @param resetCurrentDocument if true, reset the loaded document
     * @param setCmsPage if true, mark current page as CMS page
     */
    public void setCurrentPage(String currentPage, boolean resetBreadcrubs, boolean resetCurrentDocument, boolean setCmsPage) {
        logger.trace("setCurrentPage: {}", currentPage);
        if (resetBreadcrubs) {
            breadcrumbBean.resetBreadcrumbs();
        }
        if (resetCurrentDocument) {
            resetCurrentDocument();
        }

        //        this.savePageUrl();

        setCmsPage(setCmsPage);
        this.currentPage = currentPage;

        //after setting page, detemine subtheme
        setSubThemeDiscriminatorValue();
    }

    /**
     * setCurrentBreadcrumbPage.
     *
     * @param pageName display name for the breadcrumb entry
     * @param pageWeight breadcrumb sort weight as string
     * @param pageURL relative URL for the breadcrumb link
     */
    public void setCurrentBreadcrumbPage(String pageName, String pageWeight, String pageURL) {
        // logger.debug("Current Breadcrumb Page: {}", pageName);
        // logger.debug("pageWeight: {}", pageWeight);
        // logger.debug("pageURL: {}", pageURL);
        breadcrumbBean.resetBreadcrumbs();
        // logger.debug("pageNameTranslation: {}", pageNameTranslation);
        breadcrumbBean.updateBreadcrumbs(
                new LabeledLink(pageName, BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + pageURL, Integer.valueOf(pageWeight)));
        this.currentPage = pageName;

    }

    /**
     * Returns the manually selected view type (will be used for search result browsing, if set).
     *
     * @should return value correctly
     * @return the manually selected view type name, or null if none has been set
     */
    public String getPreferredView() {
        return statusMap.get(KEY_PREFERRED_VIEW);
    }

    /**
     * Sets the manually selected view type (will be used for search result browsing, if set).
     *
     * @should set value correctly
     * @param preferredView view type name to set as preferred
     */
    public void setPreferredView(String preferredView) {
        statusMap.put(KEY_PREFERRED_VIEW, preferredView);
    }

    /**
     * setCurrentPageIndex.
     */
    public void setCurrentPageIndex() {
        setCurrentPage(HOME_PAGE, true, true);
    }

    /**
     * setCurrentPageSearch.
     */
    public void setCurrentPageSearch() {
        setCurrentPage(SEARCH_PAGE, true, true);
        breadcrumbBean.updateBreadcrumbs(new LabeledLink(SEARCH_PAGE, getSearchUrl() + '/', BreadcrumbBean.WEIGHT_SEARCH));
    }

    /**
     * setCurrentPageBrowse.
     */
    public void setCurrentPageBrowse() {
        setCurrentPage(BROWSE_PAGE, true, true);
        breadcrumbBean.updateBreadcrumbs(new LabeledLink("browseCollection", getBrowseUrl() + '/', BreadcrumbBean.WEIGHT_BROWSE));
    }

    /**
     * setCurrentPageBrowse.
     *
     * @param collection collection view to use for the breadcrumb link
     */
    public void setCurrentPageBrowse(CollectionView collection) {
        if (collection != null) {
            logger.trace("setCurrentPageBrowse: {}", collection.getBaseElementName());
        }
        setCurrentPage(BROWSE_PAGE, true, true);
        breadcrumbBean
                .updateBreadcrumbs(new CollectionLabeledLink("browseCollection", getBrowseUrl() + '/', collection, BreadcrumbBean.WEIGHT_BROWSE));
    }

    /**
     * setCurrentPageTags.
     */
    public void setCurrentPageTags() {
        setCurrentPage(TAGS_PAGE, true, true);
        breadcrumbBean.updateBreadcrumbs(
                new LabeledLink("tagclouds", BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/tags/", BreadcrumbBean.WEIGHT_TAG_CLOUD));
    }

    /**
     * setCurrentPageStatistics.
     */
    public void setCurrentPageStatistics() {
        setCurrentPage("statistics", true, true);
        breadcrumbBean.updateBreadcrumbs(new LabeledLink("statistics", BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/statistics/",
                BreadcrumbBean.WEIGHT_TAG_MAIN_MENU));
    }

    /**
     * Sets the current page to a crowdsourcing annotation page with the given campaign as parent and the given pi as current identifier.
     *
     * @param campaign crowdsourcing campaign to use as breadcrumb parent
     * @param pi persistent identifier of the record being annotated
     * @param status annotation or review status determining the URL action segment
     */
    public void setCrowdsourcingAnnotationPage(Campaign campaign, String pi, CrowdsourcingStatus status) {
        if (campaign == null) {
            return;
        }
        String urlActionParam = CrowdsourcingStatus.REVIEW.equals(status) ? "review" : "annotate";
        setCurrentPage("crowdsourcingAnnotation", false, true);
        breadcrumbBean.updateBreadcrumbs(new LabeledLink(campaign.getMenuTitleOrElseTitle(getLocaleString(), true),
                BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/campaigns/" + campaign.getId() + "/" + urlActionParam + "/",
                BreadcrumbBean.WEIGHT_CROWDSOURCING_CAMPAIGN));

        if (pi != null) {
            breadcrumbBean.updateBreadcrumbs(new LabeledLink(pi,
                    BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/campaigns/" + campaign.getId() + "/" + urlActionParam + "/" + pi
                            + "/",
                    BreadcrumbBean.WEIGHT_CROWDSOURCING_CAMPAIGN_ITEM));
        }
    }

    /**
     * setCurrentPageUser.
     */
    public void setCurrentPageUser() {
        setCurrentPage("user", false, true);
    }

    /**
     * setCurrentPageAdmin.
     *
     * @param pageName admin page name to activate
     */
    public void setCurrentPageAdmin(String pageName) {
        setCurrentPageAdmin(pageName, Collections.emptyList());
    }

    public void setCurrentPageAdmin(String pageName, List<List<String>> labels) {
        breadcrumbBean.resetBreadcrumbs(false);
        resetCurrentDocument();
        if (pageName != null && !pageName.trim().isEmpty()) {
            PageType pageType = PageType.getByName(pageName);
            if (pageType == null || PageType.other == pageType) {
                this.currentPage = PageType.admin.name();
            } else {
                this.currentPage = pageType.name();
                List<LabeledLink> breadcrumbs = createAdminBreadcrumbs(pageType, labels);
                breadcrumbs.forEach(link -> breadcrumbBean.updateBreadcrumbs(link));

            }
        } else {
            this.currentPage = "adminAllUsers";
        }

    }

    /**
     * 
     * @param pageType page type for which the breadcrumb hierarchy is built
     * @param labels optional label overrides for each breadcrumb level
     * @return List<LabeledLink>
     */
    protected List<LabeledLink> createAdminBreadcrumbs(PageType pageType, List<List<String>> labels) {
        PageType breadcrumbType = pageType;
        List<LabeledLink> links = new ArrayList<>();
        Iterator<List<String>> labelIterator = labels.iterator();
        while (breadcrumbType != null) {
            String label;
            if (labelIterator.hasNext()) {
                List<String> labelArray = labelIterator.next();
                String key = labelArray.get(0);
                String[] params = labelArray.subList(1, labelArray.size()).toArray(new String[labelArray.size() - 1]);
                label = ViewerResourceBundle.getTranslationWithParameters(key, locale, true, params);
            } else {
                label = ViewerResourceBundle.getTranslation(breadcrumbType.getLabel(), locale);
            }
            links.add(
                    new LabeledLink(label,
                            BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + breadcrumbType.getName(),
                            0));
            breadcrumbType = breadcrumbType.getParent();
        }
        Collections.reverse(links);
        for (int i = 0; i < links.size(); i++) {
            links.get(i).setWeight(i);
        }
        return links;
    }

    /**
     * setCurrentPageAdmin.
     */
    public void setCurrentPageAdmin() {
        setCurrentPageAdmin("adminAllUsers");
    }

    /**
     * setCurrentPageSitelinks.
     */
    public void setCurrentPageSitelinks() {
        setCurrentPage("sitelinks", true, true);
        breadcrumbBean.updateBreadcrumbs(
                new LabeledLink("sitelinks", BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/sitelinks/", BreadcrumbBean.WEIGHT_SITELINKS));

    }

    /**
     * setCurrentPageTimeMatrix.
     */
    public void setCurrentPageTimeMatrix() {
        setCurrentPage("timeMatrix", true, true);

    }

    /**
     * setCurrentPageSearchTermList.
     */
    public void setCurrentPageSearchTermList() {
        setCurrentPage(SEARCH_TERM_LIST_PAGE, false, true);
    }

    /**
     * resetCurrentPage.
     */
    public void resetCurrentPage() {
        logger.trace("resetCurrentPage");
        setCurrentPage(null, true, true);
    }

    /**
     * getViewAction.
     *
     * @param view view name to return as action string
     * @return the given view name unchanged, for use as a JSF action outcome
     */
    public String getViewAction(String view) {
        return view;
    }

    /**
     * getCurrentView.
     *
     * @return the name of the currently selected content view
     * @should return value correctly
     */
    public String getCurrentView() {
        return statusMap.get(KEY_CURRENT_VIEW);
    }

    /**
     * Sets the currently selected content view name.
     *
     * @param currentView view name to set as current
     * @should set value correctly
     */
    public void setCurrentView(String currentView) {
        logger.trace("{}: {}", KEY_CURRENT_VIEW, currentView);
        statusMap.put(KEY_CURRENT_VIEW, currentView);
        setCurrentPage(currentView);
    }

    /**
     * getDefaultLocale.
     *
     * @return the default locale from the JSF application, or null if no FacesContext is available
     */
    public Locale getDefaultLocale() {
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getApplication() != null) {
            return FacesContext.getCurrentInstance().getApplication().getDefaultLocale();
        }

        return null;
    }

    /**
     * Getter for the field <code>locale</code>.
     *
     * @return the currently active locale
     */
    public Locale getLocale() {
        // logger.trace("getLocale: {}", locale); //NOSONAR Debug
        return locale;
    }

    /**
     * Returns the language code of the current <code>locale</code> in the ISO 639-1 (two-character) format.
     *
     * @return the two-character ISO 639-1 language code of the current locale
     */
    public String getLocaleString() {
        return locale.getLanguage();
    }

    /**
     * getSupportedLocales.
     *
     * @return an iterator over all supported application locales
     */
    public Iterator<Locale> getSupportedLocales() {
        return ViewerResourceBundle.getAllLocales().iterator();
    }

    /**
     * Returns ISO 639-1 language codes of available JSF locales.
     *
     * @return a list of ISO 639-1 language codes for all supported application locales
     */
    public List<String> getSupportedLanguages() {
        List<String> ret = new ArrayList<>();

        Iterable<Locale> locales = this::getSupportedLocales;
        StreamSupport.stream(locales.spliterator(), false)
                //                .peek(language -> logger.trace("Adding sort field: {}", language))
                .forEach(loc -> ret.add(loc.getLanguage()));

        return ret;
    }

    /**
     * Returns ISO 639-1 language codes of available JSF locales as json array.
     *
     * @return a String to be interpreted as json array of strings.
     */
    public String getSupportedLanguagesAsJson() {

        Iterable<Locale> locales = this::getSupportedLocales;
        String ret = StreamSupport.stream(locales.spliterator(), false)
                .map(lang -> "\"" + lang + "\"")
                .collect(Collectors.joining(","));
        ret = "[" + ret + "]";
        return ret;
    }

    /**
     * setLocaleString.
     *
     * @param inLocale ISO 639-1 language code to set as locale
     */
    public void setLocaleString(String inLocale) {
        logger.trace("setLocaleString: {}", inLocale);
        locale = Locale.forLanguageTag(inLocale);
        FacesContext.getCurrentInstance().getViewRoot().setLocale(locale);

        // Make sure browsing terms are reloaded, so that locale-specific sorting can be applied
        if (SEARCH_TERM_LIST_PAGE.equals(getCurrentPage())) {
            BrowseBean bb = BeanUtils.getBrowseBean();
            if (bb != null) {
                bb.resetTerms();
                try {
                    bb.searchTerms();
                } catch (IndexUnreachableException | PresentationException e) {
                    logger.error(e.getMessage(), e);
                } catch (RedirectException e) {
                    // TODO
                }
            }
        }

        // Also set ActiveDocumentBean.selectedRecordLanguage, so that multilingual metadata
        // values etc. are displayed in the selected language as well
        ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
        if (adb != null) {
            adb.setSelectedRecordLanguage(inLocale);
        }

        // Reset advanced search parameters so that the SearchQueryItems have correct language fields
        SearchBean sb = BeanUtils.getSearchBean();
        if (sb != null && sb.getActiveSearchType() == SearchHelper.SEARCH_TYPE_ADVANCED) {
            sb.resetAdvancedSearchParameters();
        }
    }

    /**
     * getDatePattern.
     *
     * @return the locale-appropriate date format pattern for the current locale
     */
    public String getDatePattern() {
        return getDatePattern(locale);
    }

    public static String getDatePattern(String language) {
        return getDatePattern(Locale.forLanguageTag(language));
    }

    public static String getDatePattern(Locale locale) {
        if (locale == null) {
            return "yyyy-MM-dd";
        }

        return DataManager.getInstance()
                .getConfiguration()
                .getStringFormat("date", locale)
                .orElseGet(() -> {
                    switch (locale.getLanguage()) {
                        case "de":
                            return "dd.MM.yyyy";
                        case "en":
                            return "MM/dd/yyyy";
                        case "es":
                        case "fr":
                            return "dd/MM/yyyy";
                        default:
                            return "yyyy-MM-dd";
                    }
                });
    }

    /**
     * Get the date/time pattern for the current locale for use with jQuery date picker. Uses the value of {@link #getDatePattern()} and adapts the
     * month and year patterns in the following way:
     * <ul>
     * <li>MM --> mm</li>
     * <li>yyyy --> yy</li>
     * </ul>
     *
     * @return a date pattern suitable for jquery date picker
     */
    public String getDatePatternjQueryDatePicker() {
        String pattern = getDatePattern();
        pattern = pattern.replace("MM", "mm").replace("yyyy", "yy");
        return pattern;
    }

    /**
     *
     * @return Appropriate date/time pattern for the current locale
     */
    public String getDateTimePattern() {
        if (locale == null) {
            return "yyyy-MM-dd - HH:mm";
        }

        switch (locale.getLanguage()) {
            case "de":
                return "dd.MM.yyyy - HH:mm";
            case "en":
                return "MM/dd/yyyy - h:mm a";
            case "es":
                return "dd/MM/yyyy - HH:mm";
            default:
                return "yyyy-MM-dd - HH:mm";
        }
    }

    /**
     * reload.
     */
    public void reload() {
        //noop
    }

    /**
     * getApplicationUrl.
     *
     * @return the absolute base URL of the viewer application ending with a slash
     */
    public String getApplicationUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/";
    }

    /**
     * Used for social bookmarks.
     *
     * @return the current page URL encoded for use in social bookmark links
     */
    public String getEncodedUrl() {
        try {
            return URLEncoder.encode(getRequestPath(FacesContext.getCurrentInstance().getExternalContext()), "ASCII");
        } catch (UnsupportedEncodingException e) {
            logger.warn("Not possible to encode URL", e);
        }
        return "";
    }

    /**
     * getCurrentUrl.
     *
     * @return the absolute URL of the current request, or null if no request URL is available
     */
    public String getCurrentUrl() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        if (request != null) {
            URL url = PrettyContext.getCurrentInstance(request).getRequestURL();
            if (url != null) {
                return getApplicationUrl() + StringTools.stripJS(url.toURL().substring(1));
            }
        }
        return null;
    }

    /**
     * getRssUrl.
     *
     * @return the URL to the RSS feed for the current locale
     */
    public String getRssUrl() {
        try {
            return URL_RSS + "/?lang=" + CmsBean.getCurrentLocale().getLanguage();
        } catch (NullPointerException e) {
            return URL_RSS;
        }
    }

    /**
     * getRequestPath.
     *
     * @param externalContext JSF external context providing the current request
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

    /**
     * getRequestPath.
     *
     * @param request incoming HTTP servlet request
     * @param prettyFacesURI PrettyFaces forwarded URI, may be null or empty
     * @return the complete request URL including scheme, host, port and path
     */
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

    /**
     * getFullRequestUrl.
     *
     * @param request incoming HTTP servlet request
     * @param prettyFacesURI PrettyFaces forwarded URI, may be null or empty
     * @return the full request URL including query string for standard URLs, or the pretty URL without query string
     */
    public static String getFullRequestUrl(HttpServletRequest request, String prettyFacesURI) {
        if (StringUtils.isEmpty(prettyFacesURI)) {
            return getRequestPath(request, prettyFacesURI) + "?" + request.getQueryString();
        }

        return getRequestPath(request, prettyFacesURI);
    }

    /**
     * Returns the current PrettyURL.
     *
     * @return the current PrettyFaces URL of the active request
     */
    public String getCurrentPrettyUrl() {
        Optional<HttpServletRequest> request = Optional.ofNullable(FacesContext.getCurrentInstance())
                .map(FacesContext::getExternalContext)
                .map(ExternalContext::getRequest)
                .map(o -> (HttpServletRequest) o);

        Optional<URL> requestUrl = request
                .map(PrettyContext::getCurrentInstance)
                .map(PrettyContext::getRequestURL);

        return request.map(ServletUtils::getServletPathWithHostAsUrlFromRequest).orElse("")
                + requestUrl.map(URL::toURL).orElse("");
    }

    /**
     * getTimeZone.
     *
     * @return the default system time zone
     */
    public TimeZone getTimeZone() {
        return TimeZone.getDefault();
    }

    /**
     * setMenuPage.
     *
     * @param page menu page name to store in the status map
     * @should set value correctly
     */
    public void setMenuPage(String page) {
        statusMap.put(KEY_MENU_PAGE, page);
    }

    /**
     * getMenuPage.
     *
     * @should return value correctly
     * @return the currently active menu page name stored in the status map
     */
    public String getMenuPage() {
        return statusMap.get(KEY_MENU_PAGE);
    }

    /**
     * Getter for the field <code>theme</code>.
     *
     * @return the name of the currently active viewer theme
     */
    public String getTheme() {
        return theme;
    }

    /**
     * Returns the value of the configured sub-theme discriminator field. The value can be set via
     * <code>setSubThemeDiscriminatorValue(java.lang.String)</code> (e.g. via PrettyFacces). If a record is currently loaded and has a
     * dicriminatorField:discriminatorValue pair, the currently set value is replaced with that from the record.
     *
     * @return the current sub-theme discriminator value, or "-" if none is set
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getSubThemeDiscriminatorValue() throws IndexUnreachableException {
        return StringUtils.isNotEmpty(statusMap.get(KEY_SUBTHEME_DISCRIMINATOR_VALUE)) ? statusMap.get(KEY_SUBTHEME_DISCRIMINATOR_VALUE) : "-";
    }

    /**
     * Get the subthemeDiscriminator value either from a property of the currently loaded CMS page or the currently loaded document in the
     * activeDocumentbean if the current page is a docmentPage.
     *
     * @return the subtheme name determined from current cmsPage or current document. If {@link Configuration#getSubthemeDiscriminatorField} is blank,
     *         always return an empty string
     */
    public String determineCurrentSubThemeDiscriminatorValue() {
        // Automatically set the sub-theme discriminator value to the
        // current record's value, if configured to do so
        String subThemeDiscriminatorValue = "";
        String discriminatorField = DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField();
        if (StringUtils.isNotBlank(discriminatorField)) {
            ActiveDocumentBean activeDocumentBean = BeanUtils.getActiveDocumentBean();
            ViewManager viewManager = activeDocumentBean != null ? activeDocumentBean.getViewManager() : null;
            if (viewManager != null && getCurrentPageType().isDocumentPage()) {
                // If a record is loaded, get the value from the record's value
                // in discriminatorField
                StructElement topStructElement = viewManager.getTopStructElement();
                if (topStructElement != null) {
                    subThemeDiscriminatorValue = topStructElement.getMetadataValue(discriminatorField);
                }
            } else if (isCmsPage()) {
                if (cmsBean != null && cmsBean.getCurrentPage() != null) {
                    subThemeDiscriminatorValue = cmsBean.getCurrentPage().getSubTheme();
                }
            }
        }
        return subThemeDiscriminatorValue;
    }

    public void setSubThemeDiscriminatorValue() {
        String subThemeDiscriminatorValue = determineCurrentSubThemeDiscriminatorValue();
        setSubThemeDiscriminatorValue(subThemeDiscriminatorValue);
    }

    /**
     * setSubThemeDiscriminatorValue.
     *
     * @param subThemeDiscriminatorValue discriminator value identifying the active sub-theme
     * @should set value correctly
     */
    public void setSubThemeDiscriminatorValue(String subThemeDiscriminatorValue) {
        logger.trace("setSubThemeDiscriminatorValue: {}", subThemeDiscriminatorValue);
        // If a new discriminator value has been selected, the visible
        // collection list must be generated anew
        String previousSubThemeDiscriminatorValue = statusMap.get(KEY_SUBTHEME_DISCRIMINATOR_VALUE);
        statusMap.put(KEY_SUBTHEME_DISCRIMINATOR_VALUE, subThemeDiscriminatorValue);
        if ((StringUtils.isBlank(subThemeDiscriminatorValue) && StringUtils.isNotBlank(previousSubThemeDiscriminatorValue)
                || (StringUtils.isNotBlank(subThemeDiscriminatorValue) && !subThemeDiscriminatorValue.equals(previousSubThemeDiscriminatorValue)))) {
            BrowseBean browseBean = BeanUtils.getBrowseBean();
            if (browseBean != null) {
                browseBean.resetAllLists();
            }
            CalendarBean calendarBean = BeanUtils.getCalendarBean();
            if (calendarBean != null) {
                try {
                    calendarBean.resetYears();
                } catch (PresentationException e) {
                    logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
                } catch (IndexUnreachableException e) {
                    logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                }
            }
            // Reset access permissions in session (user might not have the same permissions for a different subtheme)
            logger.trace("{} access premissions removed from user session.", AccessConditionUtils.clearSessionPermissions(BeanUtils.getSession()));

            // Reset navigation menu
            if (cmsBean != null && cmsBean.getCurrentPage() != null) {
                cmsBean.resetNavigationMenuItems();
            }
        }
    }

    /**
     * resetTheme.
     */
    public void resetTheme() {
        logger.trace("resetTheme");
        // Resetting the current page here would result in the current record being flushed, which is bad for CMS overview pages
        //        resetCurrentPage();
        setCmsPage(false);
        setSubThemeDiscriminatorValue("");
    }

    /**
     * getObjectUrl.
     *
     * @return the absolute base URL for the object view page
     */
    public String getObjectUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewObject.getName();
    }

    /**
     * getImageUrl.
     *
     * @return the absolute base URL for the image view page
     */
    public String getImageUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewImage.getName();
    }

    public String getCurrentPageTypeUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + getCurrentPageType().getName();

    }

    /**
     * getImageActiveUrl.
     *
     * @return the absolute base URL for the active image view page (with leading "!")
     */
    public String getImageActiveUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/!" + PageType.viewImage.getName();
    }

    /**
     * getCalendarUrl.
     *
     * @return the absolute base URL for the TOC (formerly calendar) view page
     * @deprecated Calendar view has been retired; use <code>getTocUrl()</code>
     */
    @Deprecated(since = "26.03")
    public String getCalendarUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewToc.getName();
    }

    /**
     * getCalendarActiveUrl.
     *
     * @return the absolute active URL for the TOC (formerly calendar) view page (with leading "!")
     * @deprecated Calendar view has been retired; use <code>getTocActiveUrl()</code>
     */
    @Deprecated(since = "26.03")
    public String getCalendarActiveUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/!" + PageType.viewToc.getName();
    }

    /**
     * getTocUrl.
     *
     * @return the absolute base URL for the TOC view page
     */
    public String getTocUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewToc.getName();
    }

    /**
     * getTocActiveUrl.
     *
     * @return the absolute active URL for the TOC view page (with leading "!")
     */
    public String getTocActiveUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/!" + PageType.viewToc.getName();
    }

    /**
     * getThumbsUrl.
     *
     * @return the absolute base URL for the thumbnail view page
     */
    public String getThumbsUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewThumbs.getName();
    }

    /**
     * getThumbsActiveUrl.
     *
     * @return the absolute active URL for the thumbnail view page (with leading "!")
     */
    public String getThumbsActiveUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/!" + PageType.viewThumbs.getName();
    }

    /**
     * getMetadataUrl.
     *
     * @return the absolute base URL for the metadata view page
     */
    public String getMetadataUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewMetadata.getName();
    }

    /**
     * getMetadataActiveUrl.
     *
     * @return the absolute active URL for the metadata view page (with leading "!")
     */
    public String getMetadataActiveUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/!" + PageType.viewMetadata.getName();
    }

    /**
     * getFulltextUrl.
     *
     * @return the absolute base URL for the fulltext view page
     */
    public String getFulltextUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewFulltext.getName();
    }

    /**
     * getMeiActiveUrl.
     *
     * @return the absolute active URL for the MEI music view page (with leading "!")
     */
    public String getMeiActiveUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/!" + PageType.viewMei.getName();
    }

    /**
     * getMeiUrl.
     *
     * @return the absolute base URL for the MEI music view page
     */
    public String getMeiUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewMei.getName();
    }

    /**
     * getFulltextActiveUrl.
     *
     * @return the absolute active URL for the fulltext view page (with leading "!")
     */
    public String getFulltextActiveUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/!" + PageType.viewFulltext.getName();
    }

    /**
     * 
     * @param pi persistent identifier of the record
     * @param docStructType structural type used to determine the page type
     * @param order page order within the record
     * @param anchorOrGroup true if the record is an anchor or group
     * @param hasImages true if the record has image pages
     * @return Record URL
     * @should construct url correctly
     */
    public String getRecordUrl(String pi, String docStructType, int order, boolean anchorOrGroup, boolean hasImages) {
        PageType pageType = PageType.determinePageType(docStructType, "image/tiff", anchorOrGroup, hasImages, false);
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + '/'
                + DataManager.getInstance().getUrlBuilder().buildPageUrl(pi, order, null, pageType, true);
    }

    /**
     * getSearchUrl.
     *
     * @return the absolute URL for the regular search page
     */
    public String getSearchUrl() {
        return getSearchUrl(SearchHelper.SEARCH_TYPE_REGULAR);
    }

    /**
     * getAdvancedSearchUrl.
     *
     * @return the absolute URL for the advanced search page
     */
    public String getAdvancedSearchUrl() {
        return getSearchUrl(SearchHelper.SEARCH_TYPE_ADVANCED);
    }

    /**
     * getPageUrl.
     *
     * @param pageType page type name to resolve to a URL
     * @return the absolute URL for the given page type name, or empty string if the page type is unknown
     */
    public String getPageUrl(String pageType) {
        PageType page = PageType.getByName(pageType);
        if (page != null) {
            return getPageUrl(page);
        }

        return "";
    }

    /**
     * getPageUrl.
     *
     * @param page page type whose absolute URL is returned
     * @return the absolute URL for the given page type
     */
    public String getPageUrl(PageType page) {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + page.getName();
    }

    /**
     * getSearchUrl.
     *
     * @param activeSearchType integer constant identifying the search type
     * @return the absolute URL for the search page matching the given search type
     */
    public String getSearchUrl(int activeSearchType) {
        return getSearchUrl(activeSearchType, null);
    }

    /**
     * getSearchUrl.
     *
     * @param activeSearchType integer constant identifying the search type
     * @param cmsPage optional CMS page with search functionality to redirect to instead
     * @return the absolute URL for the search page matching the given type, or the CMS search page URL if provided
     */
    public String getSearchUrl(int activeSearchType, CMSPage cmsPage) {

        //If we are on a cms-page, return the cms page url
        if (cmsPage != null && cmsPage.hasSearchFunctionality()) {
            return StringTools.removeTrailingSlashes(cmsPage.getPageUrl());
        }

        switch (activeSearchType) {
            case SearchHelper.SEARCH_TYPE_ADVANCED:
                return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.advancedSearch.getName();
            case SearchHelper.SEARCH_TYPE_TERMS:
                return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.term.getName();
            default:
                return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.search.getName();
        }
    }

    /**
     * getTermUrl.
     *
     * @return the absolute URL for the term browse search page
     */
    public String getTermUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.term.getName();
    }

    /**
     * getBrowseUrl.
     *
     * @return the absolute URL for the browse page
     */
    public String getBrowseUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.browse.getName();
    }

    /**
     * getSortUrl.
     *
     * @return the absolute URL for the search page used as sort target
     */
    public String getSortUrl() {
        return getSearchUrl();
    }

    /**
     * Adds a link to the breadcrumbs using the current PrettyURL. Can be called from XHTML.
     *
     * @param linkName display label for the breadcrumb
     * @param linkWeight breadcrumb sort weight
     */
    public void addStaticLinkToBreadcrumb(String linkName, int linkWeight) {
        addStaticLinkToBreadcrumb(linkName, getCurrentPrettyUrl(), linkWeight);
    }

    /**
     * Adds a link to the breadcrumbs using the given URL. Can be called from XHTML.
     *
     * @param linkName display label for the breadcrumb
     * @param url target URL for the breadcrumb link
     * @param linkWeight breadcrumb sort weight
     */
    public void addStaticLinkToBreadcrumb(String linkName, final String url, int linkWeight) {
        if (linkWeight < 0) {
            return;
        }

        String useUrl = url;
        PageType page = PageType.getByName(useUrl);
        if (page != null && !page.equals(PageType.other)) {
            useUrl = getUrl(page);
        }
        LabeledLink newLink = new LabeledLink(linkName, useUrl, linkWeight);
        breadcrumbBean.updateBreadcrumbs(newLink);
    }

    /**
     * @param page page type whose absolute URL is to be constructed
     * @return Absolute URL for the given page type
     */
    private String getUrl(PageType page) {
        return getApplicationUrl() + page.getName();
    }

    /**
     * Returns the string representation of the given <code>Date</code> based on the current <code>locale</code>.
     *
     * @param date date-time value to format
     * @return the locale-formatted date string for the given date-time in the current locale
     */
    public String getLocalDate(LocalDateTime date) {
        return DateTools.getLocalDate(date, locale.getLanguage());
    }

    /**
     * getMessageValueList.
     *
     * @param keyPrefix message key prefix to filter translations
     * @return a list of message key strings matching the given prefix, sorted in reverse order
     */
    public List<String> getMessageValueList(String keyPrefix) {
        List<String> sortetList = ViewerResourceBundle.getMessagesValues(locale, keyPrefix);
        Collections.reverse(sortetList);

        return sortetList;
    }

    /**
     * setSelectedNewsArticle.
     *
     * @param art identifier or key of the selected news article
     * @should set value correctly
     */
    public void setSelectedNewsArticle(String art) {
        statusMap.put(KEY_SELECTED_NEWS_ARTICLE, art);
    }

    /**
     * getSelectedNewsArticle.
     *
     * @should return value correctly
     * @return the identifier of the currently selected news article stored in the status map
     */
    public String getSelectedNewsArticle() {
        return statusMap.get(KEY_SELECTED_NEWS_ARTICLE);
    }

    /**
     * Purges all traces of the currently loaded record from ActiveDocumentBean.
     */
    private static void resetCurrentDocument() {
        ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
        if (adb != null) {
            if (!adb.isRecordLoaded()) {
                logger.trace("No record loaded, no need to reset.");
                return;
            }
            try {
                adb.reset();
            } catch (IndexUnreachableException e) {
                logger.error(e.getMessage(), e);
            }
        }

        // Module augmentations
        for (IModule module : DataManager.getInstance().getModules()) {
            try {
                module.augmentResetRecord();
            } catch (NullPointerException | IllegalArgumentException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * getLastRequestTimestamp.
     *
     * @return the timestamp string of the last request stored in the current HTTP session
     */
    public String getLastRequestTimestamp() {
        return (String) BeanUtils.getRequest().getSession(false).getAttribute("lastRequest");
    }

    public String getSessionIPAddress() {
        return NetTools.getIpAddress(BeanUtils.getRequest());
    }

    public Optional<String> getSessionId() {
        return Optional.ofNullable(FacesContext.getCurrentInstance())
                .map(FacesContext::getExternalContext)
                .map(extCtx -> extCtx.getSessionId(false));
    }

    /**
     * getStatusMapValue.
     *
     * @param key status map key to look up
     * @should return value correctly
     * @return the value associated with the given key in the navigation status map
     */
    public String getStatusMapValue(String key) {
        return statusMap.get(key);
    }

    /**
     * setStatusMapValue.
     *
     * @param key status map key to set
     * @param value value to associate with the key
     * @should set value correctly
     */
    public void setStatusMapValue(String key, String value) {
        statusMap.put(key, value);
    }

    /**
     * Getter for the field <code>statusMap</code>.
     *
     * @return the navigation status map holding key-value pairs for the current navigation state
     */
    public Map<String, String> getStatusMap() {
        return statusMap;
    }

    /**
     * Setter for the field <code>statusMap</code>.
     *
     * @param statusMap navigation status map to replace the current one
     */
    public void setStatusMap(Map<String, String> statusMap) {
        this.statusMap = statusMap;
    }

    /**
     * Returns the translation for the given <code>msgKey</code> and replaces all {i} placeholders with values from the given <code>params</code>.
     *
     * @param msgKey Message key to translate
     * @param params One or more parameter values to replace the placeholders.
     * @return Translated, escaped key with parameter replacements
     * @should escape quotation marks
     */
    public String getTranslationWithParams(String msgKey, String... params) {
        String msg = ViewerResourceBundle.getTranslationWithParameters(msgKey, null, true, params);

        // If msg contains unescaped quotation marks, it may interfere with calls to this method from JavaScript
        return StringEscapeUtils.escapeJava(msg);
    }

    /**
     * Returns the translation for the given <code>msgKey</code> and replaces all {i} placeholders with values from the given <code>params</code>.
     *
     * <p>Does not carry out character escaping
     *
     * @param msgKey Message key to translate
     * @param params One or more parameter values to replace the placeholders.
     * @return Translated, escaped key with parameter replacements
     * @should escape quotation marks
     */
    public String getTranslationWithParamsUnescaped(String msgKey, String... params) {
        return ViewerResourceBundle.getTranslationWithParameters(msgKey, null, true, params);
    }

    /**
     * Returns a simple translation for the given language (or current language, if none given).
     *
     * @param msgKey Message key to translate
     * @param language Optional desired language
     * @return Translated, escaped key
     * @should escape quotation marks
     */
    public String getTranslation(String msgKey, String language) {
        return getTranslation(msgKey, language, true);
    }

    /**
     * Returns a simple translation for the given language (or current language, if none given).
     *
     * @param msgKey Message key to translate
     * @param language Optional desired language
     * @param escape If true the return string will be Java-escaped
     * @return Translated key
     */
    public String getTranslation(String msgKey, String language, boolean escape) {
        String msg = ViewerResourceBundle.getTranslation(msgKey, language != null ? Locale.forLanguageTag(language) : null);

        // If msg contains unescaped quotation marks, it may interfere with calls to this method from JavaScript
        if (escape) {
            return StringEscapeUtils.escapeJava(msg);
        }

        return msg;
    }

    /**
     * Checks whether to display a noindex meta tag on the current page.
     *
     * @return true for a set of current page values; false otherwise
     */
    public boolean isDisplayNoIndexMetaTag() {
        if (this.currentPage == null) {
            return false;
        }

        switch (this.currentPage) {
            case SEARCH_PAGE:
            case TAGS_PAGE:
            case SEARCH_TERM_LIST_PAGE:
                return true;
            default:
                break;
        }

        return false;
    }

    /**
     * Checks if the current page displays document information, solely based on the String getCurrentPage() The Pages for which this method should
     * return true are set in the PageType class.
     *
     * @return true if the current page is a document display page (as defined in {@link io.goobi.viewer.model.viewer.PageType}), false otherwise
     */
    public boolean isDocumentPage() {
        PageType page = PageType.getByName(getCurrentPage());
        if (page != null) {
            return page.isDocumentPage();
        }
        return false;
    }

    /**
     * getSubThemeDiscriminatorQuerySuffix.
     *
     * @return the Solr query suffix for filtering by the current sub-theme discriminator value
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getSubThemeDiscriminatorQuerySuffix() throws IndexUnreachableException {
        return SearchHelper.getDiscriminatorFieldFilterSuffix(this, DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField());
    }

    /**
     * Get the {@link PageType} for the page name from {@link NavigationHelper#getCurrentPage()}.
     *
     * @return the PageType corresponding to the current page name
     */
    public PageType getCurrentPageType() {
        return PageType.getByName(getCurrentPage());
    }

    /**
     * getPreviousViewUrl.
     *
     * @return the URL of the previously visited view, or "/" if no previous view is available
     */
    public String getPreviousViewUrl() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String previousUrl = ViewHistory.getPreviousView(request).map(path -> (path.getCombinedUrl())).orElse("");
        if (StringUtils.isBlank(previousUrl)) {
            previousUrl = "/";
        }
        return previousUrl;
    }

    /**
     * redirectToPreviousView.
     *
     * @throws java.io.IOException if any.
     */
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

    /**
     * getCurrentViewUrl.
     *
     * @return the URL of the currently active view, or "/" if no current view is available
     */
    public String getCurrentViewUrl() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String previousUrl = ViewHistory.getCurrentView(request).map(path -> (path.getCombinedUrl())).orElse("");
        if (StringUtils.isBlank(previousUrl)) {
            previousUrl = "/";
        }
        return previousUrl;
    }

    public String getCurrentViewPrettyUrl() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String previousUrl = ViewHistory.getCurrentView(request).map(ViewerPath::getCombinedPrettyfiedUrl).orElse("");
        if (StringUtils.isBlank(previousUrl)) {
            previousUrl = "/";
        } else if (previousUrl.endsWith("/")) {
            previousUrl = previousUrl.substring(0, previousUrl.length() - 1);
        }
        return previousUrl;
    }

    public String getExitUrl() {
        return getExitUrl(getCurrentPageType());
    }

    /**
     * 
     * @param pageType current page type used to look up the configured exit view
     * @return Appropriate exit URL
     */
    public String getExitUrl(PageType pageType) {
        String exitView = DataManager.getInstance().getConfiguration().getPageTypeExitView(pageType);
        if (StringUtils.isNotBlank(exitView) && exitView.startsWith(StringConstants.PREFIX_PRETTY)) {
            return resolvePrettyUrl(exitView);
        } else if (StringUtils.isBlank(exitView) || exitView.equalsIgnoreCase("previousView")) {
            return getPreviousViewUrl();
        } else {
            return exitView;
        }
    }

    /**
     * 
     * @param prettyId PrettyFaces mapping ID to resolve
     * @param parameters optional parameter values to fill into the URL pattern
     * @return Resolved Pretty URL
     */
    public String resolvePrettyUrl(String prettyId, final Object... parameters) {
        Object[] useParams = parameters;
        if (useParams == null || useParams.length == 0) {
            List<PathParameter> pathParams =
                    PrettyContext.getCurrentInstance().getConfig().getMappingById(prettyId).getPatternParser().getPathParameters();
            useParams = new Object[pathParams.size()];
            int index = 0;
            for (PathParameter param : pathParams) {
                Object value = BeanUtils.getManagedBeanValue(param.getExpression().getELExpression());
                useParams[index++] = (value != null && StringUtils.isNotBlank(value.toString())) ? value : "-";
            }
        }

        URL mappedUrl = PrettyContext.getCurrentInstance().getConfig().getMappingById(prettyId).getPatternParser().getMappedURL(useParams);
        return mappedUrl.toString();
    }

    /**
     * redirectToCurrentView.
     *
     * @throws java.io.IOException if any.
     */
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

    /**
     * urlEncode.
     *
     * @param s string to URL-encode
     * @return the URL-encoded representation of the input string
     */
    public String urlEncode(String s) {
        return StringTools.encodeUrl(s);
    }

    /**
     * urlEncodeUnicode.
     *
     * @param s string to encode with Unicode-safe escaping
     * @return the input string with critical URL characters Unicode-safely escaped
     */
    public String urlEncodeUnicode(String s) {
        return BeanUtils.escapeCriticalUrlChracters(s);
    }

    /**
     * getThemeOrSubtheme.
     *
     * @return the active sub-theme discriminator value if set, otherwise the default theme name
     */
    public String getThemeOrSubtheme() {
        String currentTheme = getTheme();
        try {
            String discriminatorValue = getSubThemeDiscriminatorValue();
            if (StringUtils.isNotEmpty(discriminatorValue) && !"-".equals(discriminatorValue)) {
                currentTheme = discriminatorValue;
            }
        } catch (IndexUnreachableException e) {
            logger.error("Cannot read current subtheme", e);
        }
        return currentTheme;
    }

    /**
     * isSubthemeSelected.
     *
     * @return true exactly if {@link #getSubThemeDiscriminatorValue()} is not blank
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean isSubthemeSelected() throws IndexUnreachableException {
        return StringUtils.isNotBlank(getSubThemeDiscriminatorValue());
    }

    /**
     * getVersion.
     *
     * @return the version string of the viewer application
     */
    public String getVersion() {
        return Version.VERSION;
    }

    /**
     * getBuildDate.
     *
     * @return the build date string of the viewer application
     */
    public String getBuildDate() {
        return Version.BUILDDATE;
    }

    /**
     * getBuildVersion.
     *
     * @return the build version string of the viewer application
     */
    public String getBuildVersion() {
        return Version.BUILDVERSION;
    }

    /**
     * getApplicationName.
     *
     * @return the display name of the viewer application
     */
    public String getApplicationName() {
        return Version.APPLICATION_NAME;
    }

    /**
     * Get the path to a viewer resource relative to the root path ("/viewer") If it exists, the resource from the theme, otherwise from the core If
     * the resource exists neither in theme nor core. An Exception will be thrown
     *
     * @param path The resource path relative to the first "resources" directory
     * @return Resource path
     */
    public String getResource(String path) {
        return getResource(path, true);
    }

    /**
     * Get the path to a viewer resource relative to the root path ("/viewer") If it exists, the resource from the theme, otherwise from the core If
     * the resource exists neither in theme nor core. An Exception will be thrown
     *
     * @param path The resource path relative to the first "resources" directory
     * @param considerAlternativeSuffixes Whether to check files with different file extensions in the theme if the given file doesn't exist there.
     *            This does not effect the core path.
     * @return Resource path
     */
    public String getResource(String path, boolean considerAlternativeSuffixes) {
        return getResource(path, considerAlternativeSuffixes ? BROWSER_IMAGE_EXTENSIONS : Collections.emptyList());
    }

    /**
     * Get the path to a viewer resource relative to the root path ("/viewer") If it exists, the resource from the theme, otherwise from the core If
     * the resource exists neither in theme nor core. An Exception will be thrown
     *
     * @param path The resource path relative to the first "resources" directory
     * @param alternativeSuffixes a list of alternative file extensions to check in the theme if the given file doesn't exist there. This does not
     *            effect the core path. May be an empty list, but not null. Suffixes should be written without the extension separator dot
     * @return Resource path
     */
    public String getResource(String path, List<String> alternativeSuffixes) {
        FileResourceManager fileResourceManager = DataManager.getInstance().getFileResourceManager();
        if (fileResourceManager != null) {
            Path themePath = fileResourceManager.getThemeResourcePath(path);
            if (Files.exists(themePath)) {
                String ret = fileResourceManager.getThemeResourceURI(path).toString();
                return ret;
            } else if (!alternativeSuffixes.isEmpty()) {
                for (String suffix : alternativeSuffixes) {
                    Optional<String> resourceUri = findResource(path, themePath, removeLeadingDot(suffix));
                    if (resourceUri.isPresent()) {
                        return resourceUri.get();
                    }
                }
            }
            return fileResourceManager.getCoreResourceURI(path).toString();
        }
        return "";
    }

    private Optional<String> findResource(String path, Path themePath, String suffix) {
        Path file = FileTools.replaceExtension(themePath, suffix);
        if (Files.exists(file)) {
            Path resourcePath = FileTools.replaceExtension(Path.of(path), suffix);
            return Optional.ofNullable(this.fileResourceManager.getThemeResourceURI(resourcePath.toString()).toString());
        }
        return Optional.empty();
    }

    private String removeLeadingDot(String string) {
        if (string != null && string.startsWith(".")) {
            return string.substring(1);
        } else {
            return string;
        }
    }

    public boolean isRtl() {
        return isRtl(getLocale());
    }

    public boolean isRtl(String lang) {
        return isRtl(Locale.forLanguageTag(lang));
    }

    public boolean isRtl(Locale locale) {
        return !ComponentOrientation.getOrientation(locale).isLeftToRight();
    }

    public boolean isSolrIndexOnline() {
        return DataManager.getInstance().getSearchIndex().isSolrIndexOnline();
    }

    /**
     * If the current page url is a search page url without or with empty search parameters replace
     * {@link ViewHistory#getCurrentView(jakarta.servlet.ServletRequest)} with a search url containing the default sort string. This is done so the
     * view history contains the current random seed for random search list sorting and returning to the page yields the same ordering as the original
     * call. Must be called in the pretty mappings for all search urls which deliver randomly sorted hitlists
     */
    public void addSearchUrlWithCurrentSortStringToHistory() {
        ViewHistory.getCurrentView(BeanUtils.getRequest())
                .ifPresent(path -> {
                    ViewerPath sortStringPath = setupRandomSearchSeed(path, getLocaleString());
                    if (sortStringPath != path) {
                        ViewHistory.setCurrentView(sortStringPath, BeanUtils.getSession());
                    }
                });
    }

    /**
     * 
     * @param path current viewer path representing the search URL
     * @param language language code used to look up the configured default sort field
     * @return {@link ViewerPath}
     */
    private static ViewerPath setupRandomSearchSeed(ViewerPath path, String language) {
        String defaultSortField = DataManager.getInstance().getConfiguration().getDefaultSortField(language);
        if (SolrConstants.SORT_RANDOM.equalsIgnoreCase(defaultSortField)) {
            String parameterPath = path.getParameterPath().toString();
            if (StringUtils.isBlank(parameterPath) || parameterPath.matches("\\/?-\\/-\\/\\d+\\/-\\/-\\/?")) {
                SearchBean sb = BeanUtils.getSearchBean();
                if (sb != null) {
                    String pageUrl = PrettyUrlTools.getRelativePageUrl("newSearch5",
                            sb.getActiveContext(),
                            sb.getExactSearchString(),
                            sb.getCurrentPage(),
                            sb.getSortString(),
                            sb.getFacets().getActiveFacetString());
                    try {
                        return ViewerPathBuilder.createPath(path.getApplicationUrl(), path.getApplicationName(), pageUrl, path.getQueryString())
                                .orElse(path);
                    } catch (DAOException e) {
                        logger.error("Error creating search url with current random sort string", e);
                    }
                }
            }
        }
        return path;
    }

    /**
     * Gets the current time in milliseconds as string.
     * 
     * @return the current time in milliseconds as string
     */
    public String getCurrentTime() {
        return Long.toString(System.currentTimeMillis());
    }

    /**
     * Get the current date as {@link LocalDate}.
     * 
     * @return the current date as {@link LocalDate}
     */
    public LocalDate getCurrentDate() {
        return LocalDate.now();
    }

    /**
     * 
     * @param keys list of message keys to translate
     * @return JSON with translations for the given message keys
     */
    public String getTranslationsAsJson(List<String> keys) {
        JSONObject json = new JSONObject();
        for (String key : keys) {
            String translation = ViewerResourceBundle.getTranslation(key, locale);
            json.put(key, translation);
        }
        return json.toString();
    }

    public List<Integer> getRange(long from, long to) {
        return IntStream.range((int) from, (int) to + 1).boxed().toList();
    }

    public String getAsId(String text) {
        return StringTools.convertToSingleWord(text, MAX_HTML_ID_LENGTH, "_");
    }
}
