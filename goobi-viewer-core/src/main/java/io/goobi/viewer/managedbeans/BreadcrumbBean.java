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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.faces.url.URL;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordDeletedException;
import io.goobi.viewer.exceptions.RecordLimitExceededException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.cms.CMSStaticPage;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.search.SearchFacets;
import io.goobi.viewer.model.viewer.CompoundLabeledLink;
import io.goobi.viewer.model.viewer.LabeledLink;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.ViewManager;
import io.goobi.viewer.model.viewer.collections.CollectionView;
import io.goobi.viewer.solr.SolrConstants;

@Named
@SessionScoped
public class BreadcrumbBean implements Serializable {

    private static final long serialVersionUID = -7671680493703878185L;

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(BreadcrumbBean.class);

    /** Constant <code>WEIGHT_TAG_MAIN_MENU=1</code> */
    public static final int WEIGHT_TAG_MAIN_MENU = 1;
    /** Constant <code>WEIGHT_ACTIVE_COLLECTION=2</code> */
    public static final int WEIGHT_ACTIVE_COLLECTION = 2;
    /** Constant <code>WEIGHT_OPEN_DOCUMENT=3</code> */
    public static final int WEIGHT_OPEN_DOCUMENT = 3;
    /** Constant <code>WEIGHT_BROWSE=1</code> */
    public static final int WEIGHT_BROWSE = 1;
    /** Constant <code>WEIGHT_SEARCH=1</code> */
    public static final int WEIGHT_SEARCH = 1;
    /** Constant <code>WEIGHT_SEARCH_RESULTS=2</code> */
    public static final int WEIGHT_SEARCH_RESULTS = 2;
    /** Constant <code>WEIGHT_SEARCH_TERMS=1</code> */
    public static final int WEIGHT_SEARCH_TERMS = 1;
    /** Constant <code>WEIGHT_TAG_CLOUD=1</code> */
    public static final int WEIGHT_TAG_CLOUD = 1;
    /** Constant <code>WEIGHT_SITELINKS=1</code> */
    public static final int WEIGHT_SITELINKS = 1;
    /** Constant <code>WEIGHT_USER_ACCOUNT=1</code> */
    public static final int WEIGHT_USER_ACCOUNT = 1;
    /** Constant <code>WEIGHT_CROWDSOURCING_OVERVIEW=3</code> */
    public static final int WEIGHT_CROWDSOURCING_OVERVIEW = 3;
    /** Constant <code>WEIGHT_CROWDSOURCING_EDIT_OVERVIEW=4</code> */
    public static final int WEIGHT_CROWDSOURCING_EDIT_OVERVIEW = 4;
    /** Constant <code>WEIGHT_CROWDSOURCING_EDIT_OCR_CONTENTS=5</code> */
    public static final int WEIGHT_CROWDSOURCING_EDIT_OCR_CONTENTS = 5;
    /** Constant <code>WEIGHT_CROWDSOURCING_CAMPAIGN=2</code> */
    public static final int WEIGHT_CROWDSOURCING_CAMPAIGN = 2;
    /** Constant <code>WEIGHT_CROWDSOURCING_CAMPAIGN_ITEM=3</code> */
    public static final int WEIGHT_CROWDSOURCING_CAMPAIGN_ITEM = 3;
    /** Constant <code>WEIGHT_CROWDSOURCING_CAMPAIGN_PARENT=1</code> */
    public static final int WEIGHT_CROWDSOURCING_CAMPAIGN_PARENT = 1;

    private List<LabeledLink> breadcrumbs = new LinkedList<>();

    @Inject
    private NavigationHelper navigationHelper;

    /**
     * Attaches a new link to the breadcrumb list at the appropriate position (depending on the link's weight).
     *
     * @param newLink The breadcrumb link to add.
     * @should always remove breadcrumbs coming after the proposed breadcrumb
     */
    public void updateBreadcrumbs(LabeledLink newLink) {
        logger.trace("updateBreadcrumbs (LabeledLink): {}", newLink);
        //        List<LabeledLink> breadcrumbs = Collections.synchronizedList(this.breadcrumbs);
        synchronized (breadcrumbs) {

            // Always add the home page if there are no breadcrumbs
            if (breadcrumbs.isEmpty()) {
                resetBreadcrumbs();
            }
            // logger.trace("Adding breadcrumb: {} ({})", newLink.getUrl(), newLink.getWeight()); //NOSONAR Sometimes needed for debugging
            // Determine the position at which to add the new link
            int position = breadcrumbs.size();
            for (int i = 0; i < breadcrumbs.size(); ++i) {
                LabeledLink link = breadcrumbs.get(i);
                // logger.trace("existing breadcrumb: {}", link.toString()); //NOSONAR Sometimes needed for debugging
                if (link.getWeight() >= newLink.getWeight()) {
                    position = i;
                    break;
                }
            }
            try {
                // To avoid duplicate breadcrumbs while flipping pages, the LabeledLink.equals() method
                // will prevent multiple breadcrumbs with the same name
                if (breadcrumbs.contains(newLink)) {
                    logger.trace("Breadcrumb is already in the list: '{}'", newLink);
                }
                breadcrumbs.add(position, newLink);
            } finally {
                // Remove any following links, even if the proposed link is a duplicate
                if (position < breadcrumbs.size()) {
                    try {
                        breadcrumbs.subList(position + 1, breadcrumbs.size()).clear();
                    } catch (NullPointerException e) {
                        // This throws a NPE sometimes
                    }
                }
            }
        }

    }

    /**
     * Updates breadcrumbs from the given CMS page (and any breadcrumb predecessor pages).
     *
     * @param cmsPage The CMS page from which to create a breadcrumb
     * @throws RecordNotFoundException
     * @throws RecordDeletedException
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws ViewerConfigurationException
     * @throws RecordLimitExceededException
     * @throws NumberFormatException
     */
    public void updateBreadcrumbs(CMSPage cmsPage)
            throws RecordNotFoundException, RecordDeletedException, DAOException, IndexUnreachableException, ViewerConfigurationException,
            RecordLimitExceededException {
        logger.trace("updateBreadcrumbs (CMSPage): {}", cmsPage.getTitle());

        List<LabeledLink> tempBreadcrumbs = new ArrayList<>();
        int weight = 1;
        try {
            // If the CMS page is part of a record, add a breadcrumb after said record and abort
            if (StringUtils.isNotBlank(cmsPage.getRelatedPI())) {
                // TODO Find a way without having a cyclic dependency
                ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
                if (adb != null) {
                    try {
                        adb.setPersistentIdentifier(cmsPage.getRelatedPI());
                        adb.open();
                    } catch (PresentationException e) {
                        logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage(), e);
                        Messages.error(e.getMessage());
                    }
                }
                weight = this.breadcrumbs.get(this.breadcrumbs.size() - 1).getWeight() + 1;
                tempBreadcrumbs.add(new LabeledLink(StringUtils.isNotBlank(cmsPage.getMenuTitle()) ? cmsPage.getMenuTitle() : cmsPage.getTitle(),
                        cmsPage.getPageUrl(), weight));
                return;
            }
            resetBreadcrumbs();
            Set<CMSPage> linkedPages = new HashSet<>();
            CMSPage currentPage = cmsPage;

            // If the current cms page contains a collection and we are in a subcollection of it,
            // attempt to add a breadcrumb link for the subcollection
            List<CollectionView> pageCollections = BeanUtils.getCollectionViewBean().getLoadedCollectionsForPage(cmsPage);
            if (!pageCollections.isEmpty()) {
                CollectionView firstCollection = pageCollections.get(0);
                LabeledLink link = new LabeledLink(firstCollection.getTopVisibleElement(),
                        firstCollection.getCollectionUrl(firstCollection.getTopVisibleElement()), WEIGHT_SEARCH_RESULTS);
                tempBreadcrumbs.add(0, link);
            }

            while (currentPage != null) {
                if (linkedPages.contains(currentPage)) {
                    //encountered a breadcrumb loop. Simply break here
                    break;
                }
                linkedPages.add(currentPage);
                if (DataManager.getInstance()
                        .getDao()
                        .getStaticPageForCMSPage(currentPage)
                        .stream()
                        .findFirst()
                        .map(CMSStaticPage::getPageName)
                        .filter(name -> PageType.index.name().equals(name))
                        .isPresent()) {
                    logger.trace("CMS index page found");
                    // The current page is the start page, which is already the breadcrumb root
                    break;
                }
                LabeledLink pageLink =
                        new LabeledLink(StringUtils.isNotBlank(currentPage.getMenuTitle()) ? currentPage.getMenuTitle() : currentPage.getTitle(),
                                currentPage.getPageUrl(), weight);
                tempBreadcrumbs.add(0, pageLink);
                if (StringUtils.isNotBlank(currentPage.getParentPageId())) {
                    try {
                        Long cmsPageId = Long.parseLong(currentPage.getParentPageId());
                        currentPage = DataManager.getInstance().getDao().getCMSPage(cmsPageId);
                    } catch (NumberFormatException | DAOException e) {
                        logger.error("CMS breadcrumb creation: Parent page of page {} is not a valid page id", currentPage.getId());
                        currentPage = null;
                    }
                } else {
                    currentPage = null;
                }

            }
        } finally {
            //            List<LabeledLink> breadcrumbs = Collections.synchronizedList(this.breadcrumbs);
            synchronized (breadcrumbs) {
                for (LabeledLink bc : tempBreadcrumbs) {
                    bc.setWeight(weight++);
                    breadcrumbs.add(bc);
                }
                // tempBreadcrumbs.forEach(bc -> breadcrumbs.add(bc));
            }
        }
    }

    /**
     * This is used for flipping search result pages (so that the breadcrumb always has the last visited result page as its URL).
     *
     * @param facetString a {@link java.lang.String} object.
     */
    public void updateBreadcrumbsForSearchHits(final String facetString) {
        logger.trace("updateBreadcrumbsForSearchHits: {}", facetString);
        List<String> facets =
                SearchFacets.getHierarchicalFacets(StringTools.decodeUrl(facetString),
                        DataManager.getInstance().getConfiguration().getHierarchicalFacetFields());
        if (!facets.isEmpty()) {
            String facet = facets.get(0);
            facets = SearchFacets.splitHierarchicalFacet(facet);
            updateBreadcrumbsWithCurrentCollection(DataManager.getInstance().getConfiguration().getHierarchicalFacetFields().get(0), facets,
                    WEIGHT_SEARCH_RESULTS);
        } else {
            updateBreadcrumbsWithCurrentUrl("searchHitNavigation", WEIGHT_SEARCH_RESULTS);
        }
    }

    /**
     * Adds a new collection breadcrumb hierarchy for the current Pretty URL.
     *
     * @param field Facet field for building the URL
     * @param subItems Facet values
     * @param weight The weight of the link
     */
    private void updateBreadcrumbsWithCurrentCollection(String field, List<String> subItems, int weight) {
        logger.trace("updateBreadcrumbsWithCurrentCollection: {} ({})", field, weight);
        updateBreadcrumbs(new LabeledLink("browseCollection", getBrowseUrl() + '/', WEIGHT_BROWSE));
        updateBreadcrumbs(new CompoundLabeledLink("browseCollection", "", field, subItems, weight));
    }

    /**
     * Adds a new breadcrumb for the current Pretty URL.
     *
     * @param name Breadcrumb name.
     * @param weight The weight of the link.
     */
    void updateBreadcrumbsWithCurrentUrl(String name, int weight) {
        logger.trace("updateBreadcrumbsWithCurrentUrl: {} / {}", name, weight);
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        URL url = PrettyContext.getCurrentInstance(request).getRequestURL();
        logger.trace("URL: {}", url);
        updateBreadcrumbs(new LabeledLink(name, BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + url.toURL(), weight));
    }

    /**
     * Empties the breadcrumb list and adds a link to the start page.
     */
    void resetBreadcrumbs() {
        resetBreadcrumbs(true);
    }

    void resetBreadcrumbs(boolean addStartPage) {
        // logger.trace("reset breadcrumbs"); //NOSONAR Sometimes needed for debugging
        //        List<LabeledLink> breadcrumbs = Collections.synchronizedList(this.breadcrumbs);
        synchronized (breadcrumbs) {
            breadcrumbs.clear();
            if (addStartPage) {
                breadcrumbs.add(new LabeledLink("home", BeanUtils.getServletPathWithHostAsUrlFromJsfContext(), 0));
            }
        }
    }

    /**
     * Adds a link to the breadcrumbs using the current PrettyURL. Can be called from XHTML.
     *
     * @param linkName a {@link java.lang.String} object.
     * @param linkWeight a int.
     */
    public void addStaticLinkToBreadcrumb(String linkName, int linkWeight) {
        addStaticLinkToBreadcrumb(linkName, navigationHelper.getCurrentPrettyUrl(), linkWeight);
    }

    /**
     * Adds a link to the breadcrumbs using the given URL. Can be called from XHTML.
     *
     * @param linkName a {@link java.lang.String} object.
     * @param linkWeight a int.
     * @param url a {@link java.lang.String} object.
     */
    public void addStaticLinkToBreadcrumb(String linkName, final String url, int linkWeight) {
        // logger.trace("addStaticLinkToBreadcrumb: {} - {} ({})", linkName, url, linkWeight);
        if (linkWeight < 0) {
            return;
        }
        PageType page = PageType.getByName(url);
        String localUrl = url;
        if (page != null && !page.equals(PageType.other)) {
            localUrl = getUrl(page);
        }
        LabeledLink newLink = new LabeledLink(linkName, localUrl, linkWeight);
        updateBreadcrumbs(newLink);
    }

    /**
     * <p>
     * addCollectionHierarchyToBreadcrumb.
     * </p>
     *
     * @param collection Full collection string containing all levels
     * @param field Solr field
     * @param splittingChar a {@link java.lang.String} object.
     * @should create breadcrumbs correctly
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void addCollectionHierarchyToBreadcrumb(final String collection, final String field, final String splittingChar)
            throws PresentationException, DAOException {
        logger.trace("addCollectionHierarchyToBreadcrumb: {}", collection);
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (splittingChar == null) {
            throw new IllegalArgumentException("splittingChar may not be null");
        }
        if (StringUtils.isEmpty(collection)) {
            return;
        }

        updateBreadcrumbs(new LabeledLink("browseCollection",
                BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.browse.getName() + '/', WEIGHT_BROWSE));
        List<String> hierarchy = StringTools.getHierarchyForCollection(collection, splittingChar);
        // Individual hierarchy elements will all be added with the active collection weight
        updateBreadcrumbs(new CompoundLabeledLink("browseCollection", "", field, hierarchy, WEIGHT_ACTIVE_COLLECTION));
    }

    /**
     *
     * @param viewManager
     * @param name
     * @param url
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public void addRecordBreadcrumbs(ViewManager viewManager, IMetadataValue name, URL url)
            throws IndexUnreachableException, PresentationException, DAOException {
        logger.trace("addRecordBreadcrumbs: {}", url);
        // Add collection hierarchy to breadcrumbs, if the record only belongs to one collection
        String collectionHierarchyField = DataManager.getInstance().getConfiguration().getCollectionHierarchyField();
        if (collectionHierarchyField != null) {
            List<String> collections = viewManager.getTopStructElement().getMetadataValues(collectionHierarchyField);
            if (collections.size() == 1) {
                addCollectionHierarchyToBreadcrumb(collections.get(0), collectionHierarchyField,
                        DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionHierarchyField));
            }
        }
        int weight = WEIGHT_OPEN_DOCUMENT;
        IMetadataValue anchorName = null;

        if (viewManager.getTopStructElement().isVolume() && viewManager.getAnchorPi() != null) {
            logger.trace("anchor breadcrumb");
            // Anchor breadcrumb
            StructElement anchorDocument = viewManager.getTopStructElement().getParent();
            anchorName = anchorDocument.getMultiLanguageDisplayLabel();
            for (String language : anchorName.getLanguages()) {
                String translation = anchorName.getValue(language).orElse("");
                if (translation != null && translation.length() > DataManager.getInstance().getConfiguration().getBreadcrumbsClipping()) {
                    translation = new StringBuilder(translation.substring(0, DataManager.getInstance().getConfiguration().getBreadcrumbsClipping()))
                            .append("...")
                            .toString();
                    anchorName.setValue(translation, language);
                }
            }
            PageType pageType = PageType.determinePageType(anchorDocument.getDocStructType(), null, true, false, false);
            String anchorUrl = '/' + DataManager.getInstance().getUrlBuilder().buildPageUrl(anchorDocument.getPi(), 1, null, pageType, true);
            updateBreadcrumbs(new LabeledLink(anchorName, BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + anchorUrl, weight++));
        }
        // If volume name is the same as anchor name, add the volume number, otherwise the volume breadcrumb will be rejected as a duplicate
        Optional<String> nameValue = name.getValue();
        if (anchorName != null && anchorName.getValue().equals(nameValue)) {
            StringBuilder sb = new StringBuilder(nameValue.isPresent() ? nameValue.get() : "");
            sb.append(" (");
            if (viewManager.getTopStructElement().getMetadataValue(SolrConstants.CURRENTNO) != null) {
                sb.append(viewManager.getTopStructElement().getMetadataValue(SolrConstants.CURRENTNO));
            } else if (viewManager.getTopStructElement().getMetadataValue(SolrConstants.CURRENTNOSORT) != null) {
                sb.append(viewManager.getTopStructElement().getMetadataValue(SolrConstants.CURRENTNOSORT));
            }
            sb.append(')');
            name.setValue(sb.toString());
        }
        // Volume/monograph breadcrumb
        updateBreadcrumbs(new LabeledLink(name, BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + url.toURL(), weight));
    }

    /**
     * Returns the list of current breadcrumb elements. Note that only the sub-links are used for elements of class <code>CompoundLabeledLink</code>,
     * not the main link.
     *
     * @return the List of flattened breadcrumb links
     */
    public List<LabeledLink> getBreadcrumbs() {
        //      List<LabeledLink> breadcrumbs = Collections.synchronizedList(this.breadcrumbs);
        synchronized (breadcrumbs) {
            List<LabeledLink> flattenedLinks = new ArrayList<>(breadcrumbs.size());
            for (LabeledLink labeledLink : breadcrumbs) {
                if (labeledLink instanceof CompoundLabeledLink) {
                    flattenedLinks.addAll(((CompoundLabeledLink) labeledLink).getSubLinks());
                } else {
                    flattenedLinks.add(labeledLink);
                }
                // logger.trace("breadcrumb: {}", labeledLink); //NOSONAR Sometimes needed for debugging
            }
            // logger.trace("getBreadcrumbs: {}", flattenedLinks.toString()); //NOSONAR Sometimes needed for debugging
            return flattenedLinks;
        }
    }

    /**
     * Returns the bottom breadcrumb. Used to return to the previous page from the errorGeneral page.
     *
     * @return a {@link io.goobi.viewer.model.viewer.LabeledLink} object.
     */
    public LabeledLink getLastBreadcrumb() {
        //        List<LabeledLink> breadcrumbs = Collections.synchronizedList(this.breadcrumbs);
        synchronized (breadcrumbs) {
            if (!breadcrumbs.isEmpty()) {
                return breadcrumbs.get(breadcrumbs.size() - 1);
            }

            return null;
        }
    }

    /**
     * <p>
     * getBrowseUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    private static String getBrowseUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.browse.getName();
    }

    /**
     * @param page
     * @return Absolute URL to the given page type
     */
    private static String getUrl(PageType page) {
        return getApplicationUrl() + page.getName();
    }

    /**
     * <p>
     * getApplicationUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    private static String getApplicationUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/";
    }
}
