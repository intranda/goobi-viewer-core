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

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

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
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RedirectException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.CMSStaticPage;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.urlresolution.ViewHistory;
import io.goobi.viewer.model.urlresolution.ViewerPath;
import io.goobi.viewer.model.urlresolution.ViewerPathBuilder;
import io.goobi.viewer.model.viewer.CollectionLabeledLink;
import io.goobi.viewer.model.viewer.LabeledLink;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.collections.CollectionView;
import io.goobi.viewer.modules.IModule;
import io.goobi.viewer.servlets.utils.ServletUtils;
import io.goobi.viewer.solr.SolrConstants;

/**
 * This bean contains useful navigation parameters.
 */
@Named
@SessionScoped
public class NavigationHelper implements Serializable {

    private static final long serialVersionUID = 4171362984701032679L;

    private static final Logger logger = LogManager.getLogger(NavigationHelper.class);

    private static final String URL_RSS = "rss";

    @Inject
    private BreadcrumbBean breadcrumbBean;

    /** Constant <code>KEY_CURRENT_VIEW="currentView"</code> */
    protected static final String KEY_CURRENT_VIEW = "currentView";
    /** Constant <code>KEY_PREFERRED_VIEW="preferredView"</code> */
    protected static final String KEY_PREFERRED_VIEW = "preferredView";
    /** Constant <code>KEY_CURRENT_PARTNER_PAGE="preferredView"</code> */
    /** Constant <code>KEY_SELECTED_NEWS_ARTICLE="selectedNewsArticle"</code> */
    protected static final String KEY_SELECTED_NEWS_ARTICLE = "selectedNewsArticle";
    /** Constant <code>KEY_MENU_PAGE="menuPage"</code> */
    protected static final String KEY_MENU_PAGE = "menuPage";
    /** Constant <code>KEY_SUBTHEME_DISCRIMINATOR_VALUE="subThemeDicriminatorValue"</code> */
    protected static final String KEY_SUBTHEME_DISCRIMINATOR_VALUE = "subThemeDicriminatorValue";

    private static final String HOME_PAGE = "index";
    private static final String SEARCH_PAGE = "search";
    private static final String SEARCH_TERM_LIST_PAGE = "searchTermList";
    private static final String BROWSE_PAGE = "browse";
    private static final String TAGS_PAGE = "tags";

    private Locale locale = Locale.ENGLISH;

    /** Map for setting any navigation status variables. Replaces currentView, etc. */
    protected Map<String, String> statusMap = new HashMap<>();

    private final String theme;

    /** Currently selected page from the main navigation bar. */
    private String currentPage = HOME_PAGE;

    private boolean isCmsPage = false;

    /**
     * Empty constructor.
     */
    public NavigationHelper() {
        theme = DataManager.getInstance().getConfiguration().getTheme();
    }

    /**
     * <p>
     * init.
     * </p>
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
     * Required setter for ManagedProperty injection
     *
     * @param breadcrumbBean the breadcrumbBean to set
     */
    public void setBreadcrumbBean(BreadcrumbBean breadcrumbBean) {
        this.breadcrumbBean = breadcrumbBean;
    }

    /**
     * <p>
     * searchPage.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String searchPage() {
        this.setCurrentPage(SEARCH_PAGE);
        return SEARCH_PAGE;
    }

    /**
     * <p>
     * homePage.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String homePage() {
        this.setCurrentPage(HOME_PAGE);
        return HOME_PAGE;
    }

    /**
     * <p>
     * browsePage.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String browsePage() {
        this.setCurrentPage(BROWSE_PAGE);
        return BROWSE_PAGE;
    }

    /**
     * <p>
     * Getter for the field <code>currentPage</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getCurrentPage() {
        return StringTools.escapeQuotes(currentPage);
    }

    /**
     * <p>
     * isCmsPage.
     * </p>
     *
     * @return the isCmsPage
     */
    public boolean isCmsPage() {
        return isCmsPage;
    }

    /**
     * <p>
     * setCmsPage.
     * </p>
     *
     * @param isCmsPage the isCmsPage to set
     */
    public void setCmsPage(boolean isCmsPage) {
        this.isCmsPage = isCmsPage;
    }

    /**
     * Produce an identifier string for a cms page to use for identifying the page in the navigation bar
     *
     * @param cmsPage
     * @return
     */
    public static String getCMSPageNavigationId(CMSPage cmsPage) {
        try {
            Optional<CMSStaticPage> staticPage = DataManager.getInstance().getDao().getStaticPageForCMSPage(cmsPage).stream().findFirst();
            if (staticPage.isPresent()) {
                return staticPage.get().getPageName();
            }
        } catch (DAOException e) {
        }
        return "cms_" + String.format("%04d", cmsPage.getId());
    }

    public void setCurrentPage(CMSPage cmsPage) {
        setCurrentPage(getCMSPageNavigationId(cmsPage), false, true, true);
    }

    /**
     * <p>
     * Setter for the field <code>currentPage</code>.
     * </p>
     *
     * @param currentPage a {@link java.lang.String} object.
     */
    public void setCurrentPage(String currentPage) {
        logger.trace("setCurrentPage: {}", currentPage);
        setCurrentPage(currentPage, false, false);
    }

    /**
     * <p>
     * Setter for the field <code>currentPage</code>.
     * </p>
     *
     * @param currentPage a {@link java.lang.String} object.
     * @param resetBreadcrubs a boolean.
     * @param resetCurrentDocument a boolean.
     */
    public void setCurrentPage(String currentPage, boolean resetBreadcrubs, boolean resetCurrentDocument) {
        // logger.trace("setCurrentPage: {}", currentPage);
        setCurrentPage(currentPage, resetBreadcrubs, resetCurrentDocument, false);
    }

    /**
     * <p>
     * Setter for the field <code>currentPage</code>.
     * </p>
     *
     * @param currentPage a {@link java.lang.String} object.
     * @param resetBreadcrubs a boolean.
     * @param resetCurrentDocument a boolean.
     * @param setCmsPage a boolean.
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
     * <p>
     * setCurrentBreadcrumbPage.
     * </p>
     *
     * @param pageName a {@link java.lang.String} object.
     * @param pageWeight a {@link java.lang.String} object.
     * @param pageURL a {@link java.lang.String} object.
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
     * @return a {@link java.lang.String} object.
     */
    public String getPreferredView() {
        return statusMap.get(KEY_PREFERRED_VIEW);
    }

    /**
     * Sets the manually selected view type (will be used for search result browsing, if set).
     *
     * @should set value correctly
     * @param preferredView a {@link java.lang.String} object.
     */
    public void setPreferredView(String preferredView) {
        statusMap.put(KEY_PREFERRED_VIEW, preferredView);
    }

    /**
     * <p>
     * setCurrentPageIndex.
     * </p>
     */
    public void setCurrentPageIndex() {
        setCurrentPage(HOME_PAGE, true, true);
    }

    /**
     * <p>
     * setCurrentPageSearch.
     * </p>
     */
    public void setCurrentPageSearch() {
        setCurrentPage(SEARCH_PAGE, true, true);
        breadcrumbBean.updateBreadcrumbs(new LabeledLink(SEARCH_PAGE, getSearchUrl() + '/', BreadcrumbBean.WEIGHT_SEARCH));
    }

    /**
     * <p>
     * setCurrentPageBrowse.
     * </p>
     */
    public void setCurrentPageBrowse() {
        setCurrentPage(BROWSE_PAGE, true, true);
        breadcrumbBean.updateBreadcrumbs(new LabeledLink("browseCollection", getBrowseUrl() + '/', BreadcrumbBean.WEIGHT_BROWSE));
    }

    /**
     * <p>
     * setCurrentPageBrowse.
     * </p>
     *
     * @param collection a {@link io.goobi.viewer.model.viewer.collections.CollectionView} object.
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
     * <p>
     * setCurrentPageTags.
     * </p>
     */
    public void setCurrentPageTags() {
        setCurrentPage(TAGS_PAGE, true, true);
        breadcrumbBean.updateBreadcrumbs(
                new LabeledLink("tagclouds", BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/tags/", BreadcrumbBean.WEIGHT_TAG_CLOUD));
    }

    /**
     * <p>
     * setCurrentPageStatistics.
     * </p>
     */
    public void setCurrentPageStatistics() {
        setCurrentPage("statistics", true, true);
        breadcrumbBean.updateBreadcrumbs(new LabeledLink("statistics", BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/statistics/",
                BreadcrumbBean.WEIGHT_TAG_MAIN_MENU));
    }

    /**
     * Set the current page to a crowdsourcing annotation page with the given campaign as parent and the given pi as current identifier
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @param pi a {@link java.lang.String} object.
     * @param status a {@link io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CrowdsourcingStatus} object.
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
     * <p>
     * setCurrentPageUser.
     * </p>
     */
    public void setCurrentPageUser() {
        setCurrentPage("user", false, true);
    }

    /**
     * <p>
     * setCurrentPageAdmin.
     * </p>
     *
     * @param pageName a {@link java.lang.String} object.
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
                label = ViewerResourceBundle.getTranslationWithParameters(key, locale, params);
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
     * <p>
     * setCurrentPageAdmin.
     * </p>
     */
    public void setCurrentPageAdmin() {
        setCurrentPageAdmin("adminAllUsers");
    }

    /**
     * <p>
     * setCurrentPageSitelinks.
     * </p>
     */
    public void setCurrentPageSitelinks() {
        setCurrentPage("sitelinks", true, true);
        breadcrumbBean.updateBreadcrumbs(
                new LabeledLink("sitelinks", BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/sitelinks/", BreadcrumbBean.WEIGHT_SITELINKS));

    }

    /**
     * <p>
     * setCurrentPageTimeMatrix.
     * </p>
     */
    public void setCurrentPageTimeMatrix() {
        setCurrentPage("timeMatrix", true, true);

    }

    /**
     * <p>
     * setCurrentPageSearchTermList.
     * </p>
     */
    public void setCurrentPageSearchTermList() {
        setCurrentPage(SEARCH_TERM_LIST_PAGE, false, true);
    }

    /**
     * <p>
     * resetCurrentPage.
     * </p>
     */
    public void resetCurrentPage() {
        logger.trace("resetCurrentPage");
        setCurrentPage(null, true, true);
    }

    /**
     * <p>
     * getViewAction.
     * </p>
     *
     * @param view a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getViewAction(String view) {
        return view;
    }

    /**
     * <p>
     * getCurrentView.
     * </p>
     *
     * @return the currentView
     * @should return value correctly
     */
    public String getCurrentView() {
        return statusMap.get(KEY_CURRENT_VIEW);
    }

    /**
     * Sets the currently selected content view name.
     *
     * @param currentView a {@link java.lang.String} object.
     * @throws DAOException
     * @should set value correctly
     */
    public void setCurrentView(String currentView) throws DAOException {
        logger.trace("{}: {}", KEY_CURRENT_VIEW, currentView);
        statusMap.put(KEY_CURRENT_VIEW, currentView);
        setCurrentPage(currentView);
    }

    /**
     * <p>
     * getDefaultLocale.
     * </p>
     *
     * @return a {@link java.util.Locale} object.
     */
    public Locale getDefaultLocale() {
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getApplication() != null) {
            return FacesContext.getCurrentInstance().getApplication().getDefaultLocale();
        }

        return null;
    }

    /**
     * <p>
     * Getter for the field <code>locale</code>.
     * </p>
     *
     * @return a {@link java.util.Locale} object.
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * <p>
     * Returns the language code of the current <code>locale</code> in the ISO 639-1 (two-character) format.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getLocaleString() {
        return locale.getLanguage();
    }

    /**
     * <p>
     * getSupportedLocales.
     * </p>
     *
     * @return a {@link java.util.Iterator} object.
     */
    public Iterator<Locale> getSupportedLocales() {
        return ViewerResourceBundle.getAllLocales().iterator();
    }

    /**
     * Returns ISO 639-1 language codes of available JSF locales.
     *
     * @return a {@link java.util.List} object.
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
     * Returns ISO 639-1 language codes of available JSF locales as json array.
     *
     * @return a String to be interpreted as json array of strings.
     */
    public String getSupportedLanguagesAsJson() {

        Iterable<Locale> locales = () -> getSupportedLocales();
        String ret = StreamSupport.stream(locales.spliterator(), false)
                .map(lang -> "\"" + lang + "\"")
                .collect(Collectors.joining(","));
        ret = "[" + ret + "]";
        return ret;
    }

    /**
     * <p>
     * setLocaleString.
     * </p>
     *
     * @param inLocale a {@link java.lang.String} object.
     */
    public void setLocaleString(String inLocale) {
        logger.trace("setLocaleString: {}", inLocale);
        locale = new Locale(inLocale);
        FacesContext.getCurrentInstance().getViewRoot().setLocale(locale);

        // Make sure browsing terms are reloaded, so that locale-specific sorting can be applied
        if (SEARCH_TERM_LIST_PAGE.equals(getCurrentPage())) {
            BrowseBean bb = BeanUtils.getBrowseBean();
            if (bb != null) {
                bb.resetTerms();
                try {
                    bb.searchTerms();
                } catch (PresentationException e) {
                    logger.error(e.getMessage(), e);
                } catch (IndexUnreachableException e) {
                    logger.error(e.getMessage(), e);
                } catch (RedirectException e) {
                    // TODO
                }
            }
        }

        // Also set ActiveDocumentBean.selectedRecordLanguage, so that multilingual metadata values etc. are displayed in the selected language as well
        ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
        if (adb != null) {
            adb.setSelectedRecordLanguage(inLocale);
        }
    }

    /**
     * <p>
     * getDatePattern.
     * </p>
     *
     * @return a {@link java.lang.String} object.
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
     * <p>
     * getDatePatternjQueryDatePicker.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDatePatternjQueryDatePicker() {
        String pattern = getDatePattern();
        pattern = pattern.replace("MM", "mm").replace("yyyy", "yy");
        return pattern;
    }

    /**
     *
     * @return
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
     * <p>
     * reload.
     * </p>
     */
    public void reload() {
        //noop
    }

    /**
     * <p>
     * getApplicationUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getApplicationUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/";
    }

    /**
     * Used for social bookmarks.
     *
     * @return a {@link java.lang.String} object.
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
     * <p>
     * getCurrentUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
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

    /**
     * <p>
     * getRssUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getRssUrl() {
        try {
            return URL_RSS + "/?lang=" + CmsBean.getCurrentLocale().getLanguage();
        } catch (NullPointerException e) {
            return URL_RSS;
        }
    }

    /**
     * <p>
     * getRequestPath.
     * </p>
     *
     * @return the complete Request Path, eg http://hostname.de/viewer/pathxyz/pathxyz/
     * @param externalContext a {@link javax.faces.context.ExternalContext} object.
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
     * <p>
     * getRequestPath.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param prettyFacesURI a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
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
     * <p>
     * getFullRequestUrl.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param prettyFacesURI a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
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
     * @return a {@link java.lang.String} object.
     */
    public String getCurrentPrettyUrl() {
        Optional<HttpServletRequest> request = Optional.ofNullable(FacesContext.getCurrentInstance())
                .map(context -> context.getExternalContext())
                .map(ExternalContext::getRequest)
                .map(o -> (HttpServletRequest) o);

        Optional<URL> requestUrl = request
                .map(r -> PrettyContext.getCurrentInstance(r))
                .map(PrettyContext::getRequestURL);

        return request.map(r -> ServletUtils.getServletPathWithHostAsUrlFromRequest(r)).orElse("")
                + requestUrl.map(URL::toURL).orElse("");
    }

    /**
     * <p>
     * getTimeZone.
     * </p>
     *
     * @return a {@link java.util.TimeZone} object.
     */
    public TimeZone getTimeZone() {
        return TimeZone.getDefault();
    }

    /**
     * <p>
     * setMenuPage.
     * </p>
     *
     * @param page a {@link java.lang.String} object.
     * @should set value correctly
     */
    public void setMenuPage(String page) {
        logger.debug("Menu Page ist: " + page);
        statusMap.put(KEY_MENU_PAGE, page);
    }

    /**
     * <p>
     * getMenuPage.
     * </p>
     *
     * @should return value correctly
     * @return a {@link java.lang.String} object.
     */
    public String getMenuPage() {
        return statusMap.get(KEY_MENU_PAGE);
    }

    /**
     * <p>
     * Getter for the field <code>theme</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getTheme() {
        return theme;
    }

    /**
     * Returns the value of the configured sub-theme discriminator field. The value can be set via
     * <code>setSubThemeDiscriminatorValue(java.lang.String)</code> (e.g. via PrettyFacces). If a record is currently loaded and has a
     * dicriminatorField:discriminatorValue pair, the currently set value is replaced with that from the record.
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getSubThemeDiscriminatorValue() throws IndexUnreachableException {

        String ret = StringUtils.isNotEmpty(statusMap.get(KEY_SUBTHEME_DISCRIMINATOR_VALUE)) ? statusMap.get(KEY_SUBTHEME_DISCRIMINATOR_VALUE) : "-";
        //         logger.trace("getSubThemeDiscriminatorValue: {}", ret);
        return ret;
    }

    /**
     * Get the subthemeDiscriminator value either from a property of the currently loaded CMS page or the currently loaded document in the
     * activeDocumentbean if the current page is a docmentPage.
     *
     * @return the subtheme name determined from current cmsPage or current document. If {@link Configuration#getSubthemeDiscriminatorField} is blank,
     *         always return an empty string
     *
     */
    public String determineCurrentSubThemeDiscriminatorValue() {
        // Automatically set the sub-theme discriminator value to the
        // current record's value, if configured to do so
        String subThemeDiscriminatorValue = "";
        String discriminatorField = DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField();
        if (StringUtils.isNotBlank(discriminatorField)) {
            ActiveDocumentBean activeDocumentBean = BeanUtils.getActiveDocumentBean();
            if (activeDocumentBean != null && activeDocumentBean.getViewManager() != null && getCurrentPageType().isDocumentPage()) {
                // If a record is loaded, get the value from the record's value
                // in discriminatorField
                subThemeDiscriminatorValue = activeDocumentBean.getViewManager().getTopStructElement().getMetadataValue(discriminatorField);
            } else if (isCmsPage()) {
                CmsBean cmsBean = BeanUtils.getCmsBean();
                if (cmsBean != null && cmsBean.getCurrentPage() != null) {
                    subThemeDiscriminatorValue = cmsBean.getCurrentPage().getSubThemeDiscriminatorValue();
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
     * <p>
     * setSubThemeDiscriminatorValue.
     * </p>
     *
     * @param subThemeDiscriminatorValue a {@link java.lang.String} object.
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
        }
    }

    /**
     * <p>
     * resetTheme.
     * </p>
     */
    public void resetTheme() {
        logger.trace("resetTheme");
        // Resetting the current page here would result in the current record being flushed, which is bad for CMS overview pages
        //        resetCurrentPage();
        setCmsPage(false);
        setSubThemeDiscriminatorValue("");
    }

    /**
     * <p>
     * getObjectUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getObjectUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewObject.getName();
    }

    /**
     * <p>
     * getImageUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getImageUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewImage.getName();
    }

    public String getCurrentPageTypeUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + getCurrentPageType().getName();

    }

    /**
     * <p>
     * getImageActiveUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getImageActiveUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/!" + PageType.viewImage.getName();
    }

    /**
     * <p>
     * getReadingModeUrl.
     * </p>
     *
     * @return the reading mode url
     * @deprecated renamed to fullscreen
     */
    public String getReadingModeUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewFullscreen.getName();
    }

    /**
     * This method checks the Solr height attribute of the current page. If this is > 0, than the current page is displayed with OpenLayers
     *
     * @return the path which viewImageFullscreen.xhtml the user should see for the current page.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getViewImagePathFullscreen() throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        String imageDisplayType = DataManager.getInstance().getConfiguration().getZoomFullscreenViewType();
        logger.trace("Detected display mode: {}", imageDisplayType);
        if (StringUtils.isNotEmpty(imageDisplayType)) {
            // MIX data exists
            if (imageDisplayType.equalsIgnoreCase("openlayersimage") && BeanUtils.getActiveDocumentBean().getViewManager() != null
                    && BeanUtils.getActiveDocumentBean().getViewManager().getCurrentPage().getPhysicalImageHeight() > 0) {
                String path =
                        "/resources/themes/" + DataManager.getInstance().getConfiguration().getTheme() + "/urlMappings/viewImageFullscreen.xhtml";
                logger.debug("MIX data detected. Redirect to the Fullscreen view  (viewImageFullscreen.xhtml) of the '{}' theme.",
                        DataManager.getInstance().getConfiguration().getTheme());
                return path;
            }
            if (imageDisplayType.equalsIgnoreCase("classic")) {
                logger.debug("No MIX data detected. Redirect to the normal /viewImageFullscreen.xhtml.");
                return "/viewImageFullscreen.xhtml";
            }
        }
        logger.error("No correct configuration, use the standard Fullscreen Image view. Detected: {} from <zoomFullscreenView/> in the {}.",
                imageDisplayType, Configuration.CONFIG_FILE_NAME);

        return "/viewImageFullscreen.xhtml";
    }

    /**
     * <p>
     * getCalendarUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getCalendarUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewCalendar.getName();
    }

    /**
     * <p>
     * getCalendarActiveUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getCalendarActiveUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/!" + PageType.viewCalendar.getName();
    }

    /**
     * <p>
     * getTocUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getTocUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewToc.getName();
    }

    /**
     * <p>
     * getTocActiveUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getTocActiveUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/!" + PageType.viewToc.getName();
    }

    /**
     * <p>
     * getThumbsUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getThumbsUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewThumbs.getName();
    }

    /**
     * <p>
     * getThumbsActiveUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getThumbsActiveUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/!" + PageType.viewThumbs.getName();
    }

    /**
     * <p>
     * getMetadataUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMetadataUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewMetadata.getName();
    }

    /**
     * <p>
     * getMetadataActiveUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMetadataActiveUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/!" + PageType.viewMetadata.getName();
    }

    /**
     * <p>
     * getFulltextUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getFulltextUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.viewFulltext.getName();
    }

    /**
     * <p>
     * getFulltextActiveUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getFulltextActiveUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/!" + PageType.viewFulltext.getName();
    }

    /**
     * <p>
     * getSearchUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSearchUrl() {
        return getSearchUrl(SearchHelper.SEARCH_TYPE_REGULAR);
    }

    /**
     * <p>
     * getAdvancedSearchUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getAdvancedSearchUrl() {
        return getSearchUrl(SearchHelper.SEARCH_TYPE_ADVANCED);
    }

    /**
     * <p>
     * getPageUrl.
     * </p>
     *
     * @param pageType a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getPageUrl(String pageType) {
        PageType page = PageType.getByName(pageType);
        if (page != null) {
            return getPageUrl(page);
        }

        return "";
    }

    /**
     * <p>
     * getPageUrl.
     * </p>
     *
     * @param page a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @return a {@link java.lang.String} object.
     */
    public String getPageUrl(PageType page) {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + page.getName();
    }

    /**
     * <p>
     * getSearchUrl.
     * </p>
     *
     * @param activeSearchType a int.
     * @return a {@link java.lang.String} object.
     */
    public String getSearchUrl(int activeSearchType) {

        //If we are on a cms-page, return the cms page url
        try {
            Optional<ViewerPath> oView = ViewHistory.getCurrentView(BeanUtils.getRequest());
            if (oView.isPresent() && oView.get().isCmsPage() && oView.get().getCmsPage().hasSearchFunctionality()) {
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

    /**
     * <p>
     * getTermUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getTermUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.term.getName();
    }

    /**
     * <p>
     * getBrowseUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getBrowseUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.browse.getName();
    }

    /**
     * <p>
     * getSortUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSortUrl() {
        return getSearchUrl();
    }

    /**
     * Adds a link to the breadcrumbs using the current PrettyURL. Can be called from XHTML.
     *
     * @param linkName a {@link java.lang.String} object.
     * @param linkWeight a int.
     */
    public void addStaticLinkToBreadcrumb(String linkName, int linkWeight) {
        addStaticLinkToBreadcrumb(linkName, getCurrentPrettyUrl(), linkWeight);
    }

    /**
     * Adds a link to the breadcrumbs using the given URL. Can be called from XHTML.
     *
     * @param linkName a {@link java.lang.String} object.
     * @param linkWeight a int.
     * @param url a {@link java.lang.String} object.
     */
    public void addStaticLinkToBreadcrumb(String linkName, String url, int linkWeight) {
        if (linkWeight < 0) {
            return;
        }
        PageType page = PageType.getByName(url);
        if (page != null && !page.equals(PageType.other)) {
            url = getUrl(page);
        }
        LabeledLink newLink = new LabeledLink(linkName, url, linkWeight);
        breadcrumbBean.updateBreadcrumbs(newLink);
    }

    /**
     * @param page
     * @return
     */
    private String getUrl(PageType page) {
        return getApplicationUrl() + page.getName();
    }

    /**
     * Returns the string representation of the given <code>Date</code> based on the current <code>locale</code>.
     *
     * @param date a {@link java.time.LocalDateTime} object.
     * @return a {@link java.lang.String} object.
     */
    public String getLocalDate(LocalDateTime date) {
        return DateTools.getLocalDate(date, locale.getLanguage());
    }

    /**
     * <p>
     * getMessageValueList.
     * </p>
     *
     * @param keyPrefix a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public List<String> getMessageValueList(String keyPrefix) {
        List<String> sortetList = ViewerResourceBundle.getMessagesValues(locale, keyPrefix);
        Collections.reverse(sortetList);

        return sortetList;
    }

    /**
     * <p>
     * setSelectedNewsArticle.
     * </p>
     *
     * @param art a {@link java.lang.String} object.
     * @should set value correctly
     */
    public void setSelectedNewsArticle(String art) {
        statusMap.put(KEY_SELECTED_NEWS_ARTICLE, art);
    }

    /**
     * <p>
     * getSelectedNewsArticle.
     * </p>
     *
     * @should return value correctly
     * @return a {@link java.lang.String} object.
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
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * <p>
     * getLastRequestTimestamp.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getLastRequestTimestamp() {
        return (String) BeanUtils.getRequest().getSession(false).getAttribute("lastRequest");
    }

    public String getSessionIPAddress() {
        String ipAddress = NetTools.getIpAddress(BeanUtils.getRequest());
        return ipAddress;
    }

    public Optional<String> getSessionId() {
        return Optional.ofNullable(FacesContext.getCurrentInstance())
                .map(FacesContext::getExternalContext)
                .map(extCtx -> extCtx.getSessionId(false));
    }

    /**
     * <p>
     * getStatusMapValue.
     * </p>
     *
     * @param key a {@link java.lang.String} object.
     * @should return value correctly
     * @return a {@link java.lang.String} object.
     */
    public String getStatusMapValue(String key) {
        return statusMap.get(key);
    }

    /**
     * <p>
     * setStatusMapValue.
     * </p>
     *
     * @param key a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     * @should set value correctly
     */
    public void setStatusMapValue(String key, String value) {
        statusMap.put(key, value);
    }

    /**
     * <p>
     * Getter for the field <code>statusMap</code>.
     * </p>
     *
     * @return the statusMap
     */
    public Map<String, String> getStatusMap() {
        return statusMap;
    }

    /**
     * <p>
     * Setter for the field <code>statusMap</code>.
     * </p>
     *
     * @param statusMap the statusMap to set
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
        String msg = ViewerResourceBundle.getTranslationWithParameters(msgKey, null, params);

        // If msg contains unescaped quotation marks, it may interfere with calls to this method from JavaScript
        return StringEscapeUtils.escapeJava(msg);
    }

    /**
     * Returns the translation for the given <code>msgKey</code> and replaces all {i} placeholders with values from the given <code>params</code>.
     * Does not carry out character escaping
     *
     * @param msgKey Message key to translate
     * @param params One or more parameter values to replace the placeholders.
     * @return Translated, escaped key with parameter replacements
     * @should escape quotation marks
     */
    public String getTranslationWithParamsUnescaped(String msgKey, String... params) {
        String msg = ViewerResourceBundle.getTranslationWithParameters(msgKey, null, params);
        return msg;
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
        }

        return false;
    }

    /**
     * Checks if the current page displays document information, solely based on the String getCurrentPage() The Pages for which this method should
     * return true are set in the PageType class.
     *
     * @return a boolean.
     */
    public boolean isDocumentPage() {
        PageType page = PageType.getByName(getCurrentPage());
        if (page != null) {
            return page.isDocumentPage();
        }
        return false;
    }

    /**
     * <p>
     * getSubThemeDiscriminatorQuerySuffix.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getSubThemeDiscriminatorQuerySuffix() throws IndexUnreachableException {
        return SearchHelper.getDiscriminatorFieldFilterSuffix(this, DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField());
    }

    /**
     * Get the {@link PageType} for the page name from {@link NavigationHelper#getCurrentPage()}
     *
     * @return a {@link io.goobi.viewer.model.viewer.PageType} object.
     */
    public PageType getCurrentPageType() {
        return PageType.getByName(getCurrentPage());
    }

    /**
     * <p>
     * getPreviousViewUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
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
     * <p>
     * redirectToPreviousView.
     * </p>
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
     * <p>
     * getCurrentViewUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
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

    public String resolvePrettyUrl(String prettyId, Object... parameters) {

        if (parameters == null || parameters.length == 0) {
            List<PathParameter> pathParams =
                    PrettyContext.getCurrentInstance().getConfig().getMappingById(prettyId).getPatternParser().getPathParameters();
            parameters = new Object[pathParams.size()];
            int index = 0;
            for (PathParameter param : pathParams) {
                Object value = BeanUtils.getManagedBeanValue(param.getExpression().getELExpression());
                parameters[index++] = (value != null && StringUtils.isNotBlank(value.toString())) ? value : "-";
            }
        }

        URL mappedUrl = PrettyContext.getCurrentInstance().getConfig().getMappingById(prettyId).getPatternParser().getMappedURL(parameters);
        return mappedUrl.toString();
    }

    /**
     * <p>
     * redirectToCurrentView.
     * </p>
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
     * <p>
     * urlEncode.
     * </p>
     *
     * @param s a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String urlEncode(String s) {
        return StringTools.encodeUrl(s);
    }

    /**
     * <p>
     * urlEncodeUnicode.
     * </p>
     *
     * @param s a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String urlEncodeUnicode(String s) {
        return BeanUtils.escapeCriticalUrlChracters(s);
    }

    /**
     * <p>
     * getThemeOrSubtheme.
     * </p>
     *
     * @return a {@link java.lang.String} object.
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
     * <p>
     * isSubthemeSelected.
     * </p>
     *
     * @return true exactly if {@link #getSubThemeDiscriminatorValue()} is not blank
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean isSubthemeSelected() throws IndexUnreachableException {
        return StringUtils.isNotBlank(getSubThemeDiscriminatorValue());
    }

    /**
     * <p>
     * getVersion.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getVersion() {
        return Version.VERSION;
    }

    /**
     * <p>
     * getBuildDate.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getBuildDate() {
        return Version.BUILDDATE;
    }

    /**
     * <p>
     * getBuildVersion.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getBuildVersion() {
        return Version.BUILDVERSION;
    }

    /**
     * <p>
     * getApplicationName.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getApplicationName() {
        return Version.APPLICATION_NAME;
    }

    /**
     * Get the path to a viewer resource relative to the root path ("/viewer") If it exists, the resource from the theme, otherwise from the core If
     * the resource exists neither in theme nor core. An Exception will be thrown
     *
     * @param path The resource path relative to the first "resources" directory
     * @return
     */
    public String getResource(String path) {
        FileResourceManager manager = DataManager.getInstance().getFileResourceManager();
        if (manager != null) {
            Path themePath = manager.getThemeResourcePath(path);
            //            Path corePath = manager.getCoreResourcePath(path);
            if (Files.exists(themePath)) {
                String ret = manager.getThemeResourceURI(path).toString();
                return ret;
            }
            //            } else if(Files.exists(corePath)) {
            String ret = manager.getCoreResourceURI(path).toString();
            return ret;
            //            } else {
            //                return "";
        }
        return "";
    }

    public boolean isRtl() {
        return isRtl(getLocale());
    }

    public boolean isRtl(String locale) {
        return isRtl(new Locale(locale));
    }

    public boolean isRtl(Locale locale) {
        return !ComponentOrientation.getOrientation(locale).isLeftToRight();
    }

    public boolean isSolrIndexOnline() {
        return DataManager.getInstance().getSearchIndex().isSolrIndexOnline();
    }

    /**
     * If the current page url is a search page url without or with empty search parameters replace
     * {@link ViewHistory#getCurrentView(javax.servlet.ServletRequest)} with a search url containing the default sort string. This is done so the view
     * history contains the current random seed for random search list sorting and returning to the page yields the same ordering as the original
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
     * @param path
     * @param language
     * @return
     */
    private static ViewerPath setupRandomSearchSeed(ViewerPath path, String language) {
        String defaultSortField = DataManager.getInstance().getConfiguration().getDefaultSortField(language);
        if (SolrConstants.SORT_RANDOM.equalsIgnoreCase(defaultSortField)) {
            String parameterPath = path.getParameterPath().toString();
            if (StringUtils.isBlank(parameterPath) || parameterPath.matches("\\/?-\\/-\\/\\d+\\/-\\/-\\/?")) {
                SearchBean sb = BeanUtils.getSearchBean();
                if (sb != null) {
                    String pageUrl = PrettyUrlTools.getRelativePageUrl("newSearch5",
                            sb.getExactSearchString(),
                            sb.getCurrentPage(),
                            sb.getSortString(),
                            sb.getFacets().getActiveFacetString());
                    try {
                        ViewerPath newPath =
                                ViewerPathBuilder.createPath(path.getApplicationUrl(), path.getApplicationName(), pageUrl, path.getQueryString())
                                        .orElse(path);
                        return newPath;
                    } catch (DAOException e) {
                        logger.error("Error creating search url with current random sort string", e);
                    }
                }
            }
        }
        return path;
    }

    public String getCurrentTime() {
        return Long.toString(System.currentTimeMillis());
    }

    public LocalDate getCurrentDate() {
        return LocalDate.now();
    }

    public String returnTo(String page) {
        return page;
    }
    
    public LocalDate getToday() {
        return LocalDate.now();
    }

    public String getTranslationsAsJson(List<String> keys) {
        Locale locale = getLocale();
        JSONObject json = new JSONObject();
        for (String key : keys) {
            String translation = ViewerResourceBundle.getTranslation(key, locale);
            json.put(key, translation);
        }
        return json.toString();
    }

    public List<Integer> getRange(long from, long to) {
        List<Integer> range = IntStream.range((int) from, (int) to + 1).boxed().collect(Collectors.toList());
        return range;
    }

}
