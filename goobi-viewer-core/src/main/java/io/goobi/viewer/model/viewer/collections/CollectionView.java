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
package io.goobi.viewer.model.viewer.collections;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.UriBuilder;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.sorting.ObjectComparatorBuilder;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.collections.CMSCollection;
import io.goobi.viewer.model.search.CollectionResult;
import io.goobi.viewer.model.urlresolution.ViewHistory;
import io.goobi.viewer.model.viewer.PageType;

/**
 * <p>
 * CollectionView class.
 * </p>
 */
public class CollectionView implements Serializable {

    private static final long serialVersionUID = -9166556644187757289L;

    private static final Logger logger = LogManager.getLogger(CollectionView.class);

    private List<HierarchicalBrowseDcElement> completeCollectionList = new ArrayList<>();
    private List<HierarchicalBrowseDcElement> visibleCollectionList = new ArrayList<>();

    private final String field;
    private final String splittingChar;
    private BrowseDataProvider dataProvider;
    /**
     * @deprecated Currently collection views always start with the baseElement.
     */
    @Deprecated(since = "24.08")
    private String topVisibleElement = null;
    private String baseElementName = null;
    /**
     * @deprecated Previously used to reload the same page showing only children of a collection which had a hierarchy level equal or less than
     *             "baseLevels"
     */
    @Deprecated(since = "24.08")
    private int baseLevels = 0;
    private boolean showAllHierarchyLevels = false;
    /**
     * @deprecated Previously used to display parents of the topVisibleElement to navigate backwards
     */
    @Deprecated(since = "24.08")
    private boolean displayParentCollections = false;
    private String searchUrl = "";
    private boolean ignoreHierarchy = false;
    private final int displayNumberOfVolumesLevel;

    private List<String> ignoreList = new ArrayList<>();

    /**
     * <p>
     * Constructor for CollectionView.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @param dataProvider a {@link io.goobi.viewer.model.viewer.collections.CollectionView.BrowseDataProvider} object.
     */
    public CollectionView(String field, BrowseDataProvider dataProvider) {
        super();
        this.field = field;
        this.splittingChar = DataManager.getInstance().getConfiguration().getCollectionSplittingChar(field);
        this.dataProvider = dataProvider;
        this.displayNumberOfVolumesLevel = DataManager.getInstance().getConfiguration().getCollectionDisplayNumberOfVolumesLevel(field);
    }

    /**
     * Creates a new CollectionView from an already existing one, keeping only the list of all collections without any display information
     *
     * @param blueprint a {@link io.goobi.viewer.model.viewer.collections.CollectionView} object.
     */
    public CollectionView(CollectionView blueprint) {
        this.completeCollectionList =
                blueprint.completeCollectionList.stream().map(element -> new HierarchicalBrowseDcElement(element)).collect(Collectors.toList());
        this.field = blueprint.field;
        this.splittingChar = blueprint.splittingChar;
        this.dataProvider = blueprint.dataProvider;
        this.searchUrl = blueprint.searchUrl;
        this.displayNumberOfVolumesLevel = blueprint.displayNumberOfVolumesLevel;
    }

    /**
     * <p>
     * populateCollectionList.
     * </p>
     *
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws IllegalRequestException
     */
    public void populateCollectionList() throws IndexUnreachableException, IllegalRequestException {
        synchronized (this) {
            try {
                logger.trace("populateCollectionList");
                Map<String, CollectionResult> dcStrings = dataProvider.getData();
                logger.trace("Creating browse elements...");
                // this has to be null and not empty at first; make sure it is initialized after the call to Solr
                completeCollectionList = new ArrayList<>();
                HierarchicalBrowseDcElement lastElement = null;
                List<String> list = new ArrayList<>(dcStrings.keySet());
                Map<String, String> sortFields = DataManager.getInstance().getConfiguration().getCollectionDefaultSortFields(field);
                Collections.sort(list);
                for (String dcName : list) {
                    String collectionName = dcName.intern();
                    long collectionSize = dcStrings.get(dcName).getCount();
                    String sortField = CollectionView.getCollectionDefaultSortField(collectionName, sortFields);
                    HierarchicalBrowseDcElement dc = new HierarchicalBrowseDcElement(collectionName, collectionSize, field, sortField,
                            this.splittingChar, this.displayNumberOfVolumesLevel);
                    dc.setFacetValues(dcStrings.get(dcName).getFacetValues());
                    dc.setOpensInNewWindow(shouldOpenInOwnWindow(collectionName));
                    if (!shouldOpenInOwnWindow(collectionName) && showAllHierarchyLevels) {
                        dc.setShowSubElements(true);
                    }

                    String applicationUrl =
                            DataManager.getInstance().getRestApiManager().getContentApiManager().map(urls -> urls.getApplicationUrl()).orElse(null);

                    // Set single record PI if collection has one one record
                    if (collectionSize == 1 && StringUtils.isNotBlank(applicationUrl)) {
                        String recordUrl = UriBuilder.fromPath("/browse/{field}/{collection}/record/").build(field, dcName).toString();
                        //String recordUrl = PrettyUrlTools.getRelativePageUrl("browseFirstRecord", field, dcName);
                        dc.setSingleRecordUrl(recordUrl);
                    }

                    if (ignoreHierarchy) {
                        completeCollectionList.add(dc);
                    } else {
                        int collectionLevel = dc.getLevel();
                        if (collectionLevel > 0 && lastElement != null) {
                            while (lastElement != null && lastElement.getLevel() >= collectionLevel) {
                                lastElement = lastElement.getParent();
                            }
                            if (lastElement != null) {
                                lastElement.addChild(dc);
                            }
                        } else {
                            completeCollectionList.add(dc);
                        }
                        lastElement = dc;
                    }
                }
                //apply configured sorting of collections after hierarchy is build
                completeCollectionList.stream().flatMap(dc -> dc.getChildren(true).stream()).forEach(dc -> sortCollection(dc));
                calculateVisibleDcElements();
                logger.trace("populateCollectionList end");
            } catch (PresentationException e) {
                logger.error("Failed to initialize collection: {}", e.getMessage());
            }
        }
    }

    /**
     * @param collectionName
     * @return boolean
     */
    private boolean shouldOpenInOwnWindow(String collectionName) {
        if (StringUtils.isNotBlank(collectionName) && collectionName.equals(getTopVisibleElement())) {
            //if this is the current top element, open in search
            return false;
        } else if (StringUtils.isBlank(getBaseElementName()) && getLevel(collectionName, splittingChar) < getBaseLevels()) {
            //If we are beneath the base level, open in collection view
            return true;
        } else if (collectionName.equals(getBaseElementName())) {
            //If this is the base element of the entire collection view, open in collection view (TODO: is that correct?)
            return true;
        } else if (collectionName.startsWith(getBaseElementName() + splittingChar)
                && getLevel(collectionName, splittingChar) - getLevel(getBaseElementName(), splittingChar) <= getBaseLevels()) {
            // If this is a subcollection of the base element and less than base levels beneath the base element,
            // open in collection view (same as second 'if' but for views with a base element
            return true;
        }

        return getBaseElementName() != null && getBaseElementName().startsWith(collectionName + splittingChar);
    }

    /**
     * <p>
     * calculateVisibleDcElements.
     * </p>
     *
     * @throws IllegalRequestException
     */
    public void calculateVisibleDcElements() throws IllegalRequestException {
        calculateVisibleDcElements(true);
    }

    /**
     * <p>
     * calculateVisibleDcElements.
     * </p>
     *
     * @param loadDescriptions a boolean.
     * @throws IllegalRequestException
     */
    public void calculateVisibleDcElements(boolean loadDescriptions) throws IllegalRequestException {
        logger.trace("calculateVisibleDcElements: {}", loadDescriptions);
        if (completeCollectionList == null) {
            return;
        }
        synchronized (this) {
            List<HierarchicalBrowseDcElement> visibleList = new ArrayList<>();
            HierarchicalBrowseDcElement topElement = getElement(getTopVisibleElement(), completeCollectionList);
            HierarchicalBrowseDcElement baseElement = getElement(getBaseElementName(), completeCollectionList);
            if (StringUtils.isNotBlank(getTopVisibleElement()) && topElement == null) {
                //invalid top element
                throw new IllegalRequestException("No collection found with name " + getTopVisibleElement());
            }
            if (topElement == null) {
                for (HierarchicalBrowseDcElement element : completeCollectionList) {
                    if (this.ignoreList.contains(element.getName())) {
                        continue;
                    }
                    visibleList.add(element);
                    visibleList.addAll(element.getAllVisibleDescendents(false));
                }
            } else {
                if (isIgnoreHierarchy()) {
                    for (HierarchicalBrowseDcElement element : completeCollectionList) {
                        if (this.ignoreList.contains(element.getName())
                                || (baseElement != null && !element.getName().startsWith(baseElement.getName() + splittingChar))) {
                            continue;
                        }
                        visibleList.add(element);
                        visibleList.addAll(element.getAllVisibleDescendents(false));
                    }
                } else {
                    topElement.setShowSubElements(true);
                    visibleList.add(topElement);
                    Collection<? extends HierarchicalBrowseDcElement> descendents = topElement.getAllVisibleDescendents(false);
                    descendents = descendents.stream().filter(c -> !this.ignoreList.contains(c.getName())).collect(Collectors.toList());
                    visibleList.addAll(descendents);
                    if (isDisplayParentCollections()
                            && (baseElement == null || topElement.getName().contains(baseElement.getName() + splittingChar))) {
                        HierarchicalBrowseDcElement parent = topElement.getParent();
                        while (parent != null) {
                            visibleList.add(0, parent);
                            if (parent.equals(baseElement)) {
                                break;
                            }
                            parent = parent.getParent();
                        }
                    }
                }
            }
            this.visibleCollectionList = sortDcList(visibleList, DataManager.getInstance().getConfiguration().getCollectionSorting(field),
                    getTopVisibleElement(), splittingChar);
            if (!isDisplayParentCollections() && StringUtils.isNotBlank(getTopVisibleElement()) && !this.visibleCollectionList.isEmpty()) {
                //if parent elements should be hidden, remove topElement from the list
                //This cannot be done earlier because it breaks sortDcList...
                this.visibleCollectionList.remove(0);
            }
            if (loadDescriptions) {
                associateElementsWithCMSData();
            }
        }
    }

    /**
     * 
     * @param elementName
     * @param includeSelf
     * @return List<HierarchicalBrowseDcElement>
     */
    public List<HierarchicalBrowseDcElement> getAncestors(String elementName, boolean includeSelf) {
        List<HierarchicalBrowseDcElement> elements = new ArrayList<>();
        HierarchicalBrowseDcElement currentElement = getElement(elementName, completeCollectionList);
        if (currentElement != null) {
            HierarchicalBrowseDcElement baseElement = getElement(getBaseElementName(), completeCollectionList);
            if (includeSelf) {
                elements.add(currentElement);
            }
            HierarchicalBrowseDcElement parent = currentElement.getParent();
            while (parent != null) {
                elements.add(parent);
                if (parent.equals(baseElement)) {
                    break;
                }
                parent = parent.getParent();
            }
            Collections.reverse(elements);
        }
        return elements;
    }

    /**
     * 
     * <p>
     * associateElementsWithCMSData.
     * </p>
     */
    public void associateElementsWithCMSData() {
        try {
            List<CMSCollection> cmsCollections = DataManager.getInstance().getDao().getCMSCollections(this.field);
            associateElementsWithCMSData(cmsCollections);
        } catch (DAOException e) {
            logger.error("Failed to associate collections with media items: {}", e.getMessage());
        }
    }

    /**
     * <p>
     * associateElementsWithCMSData.
     * </p>
     * 
     * @param cmsCollections collection data with which to enricht the browse elements
     */
    public void associateElementsWithCMSData(List<CMSCollection> cmsCollections) {
        associateWithCMSCollections(new ArrayList<>(this.visibleCollectionList), this.field, cmsCollections);
    }

    public static void associateWithCMSCollections(List<HierarchicalBrowseDcElement> collections, String solrField)
            throws DAOException {
        List<CMSCollection> cmsCollections = DataManager.getInstance().getDao().getCMSCollections(solrField);
        associateWithCMSCollections(collections, solrField, cmsCollections);
    }

    /**
     * <p>
     * associateWithCMSCollections.
     * </p>
     * returns the 'collection' parameter
     *
     * @param collections a {@link java.util.List} object.
     * @param solrField a {@link java.lang.String} object.
     * @param cmsCollections
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public static void associateWithCMSCollections(List<HierarchicalBrowseDcElement> collections, String solrField,
            List<CMSCollection> cmsCollections) {
        if (cmsCollections == null || cmsCollections.isEmpty()) {
            return;
        }
        for (CMSCollection cmsCollection : cmsCollections) {
            String collectionName = cmsCollection.getSolrFieldValue();
            if (StringUtils.isBlank(collectionName)) {
                continue;
            }
            //include direct child elements to handle views which include children of visible elements (luzern theme e.g.)
            Optional<HierarchicalBrowseDcElement> element = collections.stream()
                    .flatMap(ele -> ele.getChildren(true).stream())
                    .filter(ele -> ele.getName().equals(collectionName))
                    .findAny();
            element.ifPresent(ele -> ele.setInfo(cmsCollection));
        }
    }

    /**
     * <p>
     * getVisibleDcElements.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<HierarchicalBrowseDcElement> getVisibleDcElements() {
        // logger.trace("getVisibleDcElements"); //NOSONAR Debug
        return visibleCollectionList;
    }

    /**
     * @param elementName
     * @param collections
     * @return {@link HierarchicalBrowseDcElement}
     */
    private HierarchicalBrowseDcElement getElement(String elementName, List<HierarchicalBrowseDcElement> collections) {
        if (elementName == null) {
            return null;
        }
        for (HierarchicalBrowseDcElement element : collections) {
            if (element.getName().equals(elementName)) {
                return element;
            } else if (elementName.contains(element.getName() + splittingChar)) {
                HierarchicalBrowseDcElement matchingElement = getElement(elementName, element.getChildren());
                if (matchingElement != null) {
                    return matchingElement;
                }
            }
        }
        return null;
    }

    /**
     * Count the hierarchy level of the given collection name
     * 
     * @param collectionName
     * @param splittingChar
     * @return -1 if collection is emtpy, otherwise the number of occurrences of the splitting char
     */
    public static int getLevel(String collectionName, String splittingChar) {
        if (StringUtils.isBlank(collectionName)) {
            return -1;
        } else {
            return collectionName.length() - collectionName.replace(splittingChar, "").length();
        }
    }

    /**
     * <p>
     * resetCollectionList.
     * </p>
     */
    public void resetCollectionList() {
        synchronized (this) {
            completeCollectionList = new ArrayList<>();
        }
    }

    /**
     * <p>
     * isSubcollection.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isSubcollection() {
        return StringUtils.isNotBlank(getTopVisibleElement()) && !getTopVisibleElement().equals(getBaseElementName());
    }

    /**
     * <p>
     * Getter for the field <code>topVisibleElement</code>.
     * </p>
     * 
     * @deprecated use {@link #getBaseElementName()} instead
     * @return a {@link java.lang.String} object.
     */
    @Deprecated(since = "24.08")
    public String getTopVisibleElement() {
        if (StringUtils.isBlank(topVisibleElement) && StringUtils.isNotBlank(baseElementName)) {
            return baseElementName;
        }
        return topVisibleElement;
    }

    /**
     * <p>
     * Setter for the field <code>topVisibleElement</code>.
     * </p>
     * 
     * @deprecated use {@link #setBaseElementName(String)} instead
     * @param topVisibleElement a {@link java.lang.String} object.
     */
    @Deprecated(since = "24.08")
    public void setTopVisibleElement(String topVisibleElement) {
        this.topVisibleElement = topVisibleElement;
    }

    /**
     * <p>
     * Setter for the field <code>topVisibleElement</code>.
     * </p>
     * 
     * @deprecated use {@link #setBaseElementName(String)} instead
     * @param element a {@link io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement} object.
     */
    @Deprecated(since = "24.08")
    public void setTopVisibleElement(HierarchicalBrowseDcElement element) {
        this.topVisibleElement = element.getName();
    }

    /**
     * <p>
     * showChildren.
     * </p>
     *
     * @param element a {@link io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement} object.
     */
    public void showChildren(HierarchicalBrowseDcElement element) {
        int elementIndex = visibleCollectionList.indexOf(element);
        if (elementIndex > -1) {
            visibleCollectionList.addAll(elementIndex + 1, element.getChildrenAndVisibleDescendants());
            element.setShowSubElements(true);
            associateElementsWithCMSData();
        }
    }

    public void sortCollection(HierarchicalBrowseDcElement element) {
        String sortOrder = getSortOrder(this.field, element.getName());

        if (StringUtils.isNotBlank(sortOrder)) {
            element.getChildren().sort(ObjectComparatorBuilder.build(sortOrder, null, HierarchicalBrowseDcElement::getName));
        }
    }

    public String getSortOrder(String field, String collectionName) {
        Map<String, String> sortOrderMap = DataManager.getInstance().getConfiguration().getCollectionSortOrders(field);
        String sortOrder = sortOrderMap.entrySet()
                .stream()
                .filter(entry -> collectionName.matches(entry.getKey()))
                .map(Map.Entry::getValue)
                .findAny()
                .orElse("");
        return sortOrder;
    }

    /**
     * <p>
     * hideChildren.
     * </p>
     *
     * @param element a {@link io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement} object.
     */
    public void hideChildren(HierarchicalBrowseDcElement element) {
        int elementIndex = visibleCollectionList.indexOf(element);
        if (elementIndex > -1) {
            visibleCollectionList.removeAll(element.getAllVisibleDescendents(true));
            element.setShowSubElements(false);
        }
        // this.visibleCollectionList = sortDcList(visibleCollectionList, DataManager.getInstance().getConfiguration().getCollectionSorting());
    }

    /**
     * <p>
     * toggleChildren.
     * </p>
     *
     * @param element a {@link io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement} object.
     * @return a {@link java.lang.String} object.
     */
    public String toggleChildren(HierarchicalBrowseDcElement element) {
        logger.trace("toggleChildren: {}", element.getName());
        if (element.isHasSubelements()) {
            if (element.isShowSubElements()) {
                hideChildren(element);
            } else {
                showChildren(element);
                this.visibleCollectionList = sortDcList(visibleCollectionList,
                        DataManager.getInstance().getConfiguration().getCollectionSorting(field), getTopVisibleElement(), splittingChar);
                associateElementsWithCMSData();
            }
        }
        return "";
    }

    /**
     * Sorts the given <code>BrowseDcElement</code> list as defined in the configuration. All other elements are moved to the end of the list.
     *
     * @param inDcList The list to sort.
     * @param sortCriteriaSuperList a {@link java.util.List} object.
     * @param topElement a {@link java.lang.String} object.
     * @param splittingChar a {@link java.lang.String} object.
     * @return A sorted list.
     */
    protected static List<HierarchicalBrowseDcElement> sortDcList(final List<HierarchicalBrowseDcElement> inDcList,
            List<DcSortingList> sortCriteriaSuperList, String topElement, String splittingChar) {
        if (sortCriteriaSuperList == null) {
            return inDcList;
        }

        //iterate over all sorting lists
        List<HierarchicalBrowseDcElement> ret = inDcList;
        for (DcSortingList sortCriteriaList : sortCriteriaSuperList) {

            if (StringUtils.isNotBlank(topElement)) {
                boolean insideTopElement = false;
                for (String collection : sortCriteriaList.getCollections()) {
                    if (collection.equals(topElement) || collection.startsWith(topElement + ".")) {
                        insideTopElement = true;
                        break;
                    }
                }
                if (!insideTopElement) {
                    continue;
                }
            }

            //iterate over all entries in one list
            List<HierarchicalBrowseDcElement> sortedDcList = new ArrayList<>();
            List<HierarchicalBrowseDcElement> unsortedSubCollections = new ArrayList<>();
            for (String s : sortCriteriaList.getCollections()) {
                // logger.trace("sort: {}", s); //NOSONAR Debug
                for (HierarchicalBrowseDcElement dc : ret) {
                    if (dc.getName().equals(s) && !sortedDcList.contains(dc)) {
                        sortedDcList.add(dc);
                        // logger.trace("adding dc: {}", dc.getName()); //NOSONAR Debug
                    } else if (dc.getName().startsWith(s + splittingChar) && !sortCriteriaList.getCollections().contains(dc.getName())
                            && !unsortedSubCollections.contains(dc)) {
                        unsortedSubCollections.add(dc);
                        // logger.trace("adding dc to unsorted subcollections: {}", dc.getName()); //NOSONAR Debug
                    }
                }
            }
            List<HierarchicalBrowseDcElement> unsortedRest = ListUtils.subtract(ret, sortedDcList);
            unsortedRest = ListUtils.subtract(unsortedRest, unsortedSubCollections);
            int firstLevel = getLevelOfFirstElement(sortCriteriaList.getCollections(), splittingChar);
            int index = getIndexOfElementWithName(unsortedRest, sortCriteriaList.getSortAfter(), firstLevel, splittingChar);
            unsortedRest.addAll(index, sortedDcList);
            ret = addUnsortedSubcollections(unsortedRest, unsortedSubCollections, splittingChar);
        }

        return ret;
    }

    /**
     * Add all collections within the second list into the first list as they fit in the hierarchy.
     *
     *
     * @param collections
     * @param unsortedSubCollections
     * @param splittingChar
     * @return the first collection list
     */
    private static List<HierarchicalBrowseDcElement> addUnsortedSubcollections(List<HierarchicalBrowseDcElement> collections,
            List<HierarchicalBrowseDcElement> unsortedSubCollections, String splittingChar) {
        Collections.reverse(unsortedSubCollections);
        for (HierarchicalBrowseDcElement unsortedCollection : unsortedSubCollections) {
            for (int i = collections.size() - 1; i > -1; i--) {
                String collectionName = collections.get(i).getName();
                if (unsortedCollection.getName().startsWith(collectionName + splittingChar)) {
                    collections.add(i + 1, unsortedCollection);
                    break;
                }
            }
        }
        return collections;
    }

    /**
     * @param collections
     * @param splittingChar
     * @return The hierarchy level of the first collection within collections (1-based), or 0 if the list is empty
     */
    private static int getLevelOfFirstElement(List<String> collections, String splittingChar) {
        if (collections.isEmpty()) {
            return 0;
        }
        String collection = collections.get(0);
        return getCollectionLevel(collection, splittingChar);
    }

    /**
     *
     * @param collection
     * @param splittingChar
     * @return Level of the given collection
     */
    private static int getCollectionLevel(String collection, String splittingChar) {
        if (collection == null || collection.trim().isEmpty() || StringUtils.isEmpty(splittingChar)) {
            return 0;
        }
        String separator = splittingChar;
        if (!collection.contains(splittingChar)) {
            return 1;
        }
        if (separator.equals(".")) {
            separator = "\\.";
        }
        String[] elementLevels = collection.split(separator);
        return elementLevels.length;
    }

    /**
     * @param elementList
     * @param sortAfter
     * @param sortingLevel
     * @param splittingChar
     * @return inz
     */
    private static int getIndexOfElementWithName(List<HierarchicalBrowseDcElement> elementList, String sortAfter, int sortingLevel,
            String splittingChar) {
        int index = 0;
        Integer selectedIndex = null;
        int sortAfterLevel = getCollectionLevel(sortAfter, splittingChar);
        for (HierarchicalBrowseDcElement browseDcElement : elementList) {
            index++;
            if (browseDcElement.getName().equals(sortAfter)
                    || browseDcElement.getName().startsWith(sortAfter + splittingChar) && sortingLevel <= sortAfterLevel) {
                //sort after this element
                selectedIndex = index;
            } else if (selectedIndex != null) {
                return selectedIndex;
            }
        }

        return selectedIndex == null ? 0 : selectedIndex;
    }

    public static interface BrowseDataProvider {

        /**
         * @return Map&lt;String, CollectionResult&gt;
         * @throws IndexUnreachableException
         */
        Map<String, CollectionResult> getData() throws IndexUnreachableException;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("COLLECTION\n");
        for (HierarchicalBrowseDcElement element : completeCollectionList) {
            sb.append("\t").append(element).append("\n");
        }
        return sb.toString();
    }

    /**
     * Sets all descendants of this element to visible
     *
     * @param element a {@link io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement} object.
     */
    public void expandAll(HierarchicalBrowseDcElement element) {
        expandAll(element, -1);
    }

    /**
     * Sets all descendants of this element to visible, but not beyond level 'depth'
     *
     * @param depth a int.
     * @param element a {@link io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement} object.
     */
    public void expandAll(HierarchicalBrowseDcElement element, int depth) {
        if (depth < 0 || element.getLevel() < depth) {
            showChildren(element);
            for (HierarchicalBrowseDcElement child : element.getChildren()) {
                expandAll(child, depth);
            }
        }
    }

    /**
     * Sets all collection elements visible
     */
    public void expandAll() {
        expandAll(-1);
    }

    /**
     * Sets all collection elements visible up to 'depth' levels into the hierarchy
     *
     * @param depth a int.
     */
    public void expandAll(int depth) {
        if (completeCollectionList != null) {
            for (HierarchicalBrowseDcElement collection : completeCollectionList) {
                expandAll(collection, depth);
            }
            this.visibleCollectionList = sortDcList(visibleCollectionList, DataManager.getInstance().getConfiguration().getCollectionSorting(field),
                    getTopVisibleElement(), splittingChar);
            associateElementsWithCMSData();
        }
    }

    /**
     * <p>
     * getCompleteList.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<HierarchicalBrowseDcElement> getCompleteList() {
        return completeCollectionList;
    }

    /**
     * <p>
     * expand.
     * </p>
     *
     * @param element a {@link io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws IllegalRequestException
     */
    public void expand(HierarchicalBrowseDcElement element) throws IndexUnreachableException, IllegalRequestException {
        setTopVisibleElement(element.getName());
        populateCollectionList();
    }

    /**
     * Resets the top visible element so the topmost hierarchy level is shown
     *
     * @reset only actually resets if true
     * @param reset a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws IllegalRequestException
     */
    public void reset(boolean reset) {
        if (reset && StringUtils.isNotBlank(getTopVisibleElement()) && !getTopVisibleElement().equals(getBaseElementName())) {
            setTopVisibleElement((String) null);
            try {
                populateCollectionList();
            } catch (IllegalRequestException | IndexUnreachableException e) {
                logger.error("Error resetting collection: ", e);
            }
        }
    }

    /**
     * <p>
     * Getter for the field <code>baseElementName</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getBaseElementName() {
        return baseElementName;
    }

    /**
     * <p>
     * Setter for the field <code>baseElementName</code>.
     * </p>
     *
     * @param baseElementName a {@link java.lang.String} object.
     */
    public void setBaseElementName(String baseElementName) {
        this.baseElementName = baseElementName;
        setTopVisibleElement(baseElementName);
    }

    /**
     * <p>
     * Getter for the field <code>baseLevels</code>.
     * </p>
     *
     * @deprecated should always return 0
     * @return a int.
     */
    @Deprecated(since = "24.08")
    public int getBaseLevels() {
        return baseLevels;
    }

    /**
     * <p>
     * Setter for the field <code>baseLevels</code>.
     * </p>
     *
     * @deprecated should always be 0
     * @param baseLevels a int.
     */
    @Deprecated(since = "24.08")
    public void setBaseLevels(int baseLevels) {
        this.baseLevels = baseLevels;
    }

    /**
     * <p>
     * isTopVisibleElement.
     * </p>
     *
     * @param element a {@link io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement} object.
     * @return a boolean.
     */
    public boolean isTopVisibleElement(HierarchicalBrowseDcElement element) {
        return element.getName().equals(getTopVisibleElement());
    }

    /**
     * <p>
     * showAll.
     * </p>
     *
     * @throws IllegalRequestException
     */
    public void showAll() throws IllegalRequestException {
        if (completeCollectionList != null) {
            for (HierarchicalBrowseDcElement collection : completeCollectionList) {
                displayAllDescendents(collection);
            }
        }
        calculateVisibleDcElements();
    }

    /**
     * <p>
     * hideAll.
     * </p>
     *
     * @throws IllegalRequestException
     */
    public void hideAll() throws IllegalRequestException {
        if (completeCollectionList != null) {
            for (HierarchicalBrowseDcElement collection : completeCollectionList) {
                hideAllDescendents(collection);
            }
        }
        calculateVisibleDcElements();
    }

    /**
     * @param collection
     */
    private void displayAllDescendents(HierarchicalBrowseDcElement collection) {
        if (!collection.isOpensInNewWindow()) {
            collection.setShowSubElements(true);
        }
        for (HierarchicalBrowseDcElement child : collection.getChildren()) {
            displayAllDescendents(child);
        }
    }

    private void hideAllDescendents(HierarchicalBrowseDcElement collection) {
        if (!collection.isOpensInNewWindow()) {
            collection.setShowSubElements(false);
        }
        for (HierarchicalBrowseDcElement child : collection.getChildren()) {
            displayAllDescendents(child);
        }
    }

    /**
     * <p>
     * getTopVisibleElementLevel.
     * </p>
     *
     * @return a int.
     */
    public int getTopVisibleElementLevel() {
        if (StringUtils.isNotBlank(topVisibleElement)) {
            return getLevel(topVisibleElement, splittingChar);
        }
        return getBaseElementLevel();
    }

    /**
     * <p>
     * getBaseElementLevel.
     * </p>
     *
     * @return a int.
     */
    public int getBaseElementLevel() {
        return getLevel(baseElementName, splittingChar);
    }

    /**
     * <p>
     * Setter for the field <code>showAllHierarchyLevels</code>.
     * </p>
     *
     * @param showAllHierarchyLevels the showAllHierarchyLevels to set
     */
    public void setShowAllHierarchyLevels(boolean showAllHierarchyLevels) {
        this.showAllHierarchyLevels = showAllHierarchyLevels;
    }

    /**
     * <p>
     * isShowAllHierarchyLevels.
     * </p>
     *
     * @return the showAllHierarchyLevels
     */
    public boolean isShowAllHierarchyLevels() {
        return showAllHierarchyLevels;
    }

    /**
     * <p>
     * getCollectionUrl.
     * </p>
     *
     * @param collection a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getCollectionUrl(String collection) {
        return getCompleteList().stream()
                .filter(element -> element.getName().equals(collection))
                .findFirst()
                .map(this::getCollectionUrl)
                .orElse("");
    }

    /**
     * <p>
     * loadCollection.
     * </p>
     *
     * @param element a {@link io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement} object.
     * @return a {@link java.lang.String} object.
     */
    public String loadCollection(HierarchicalBrowseDcElement element) {
        logger.trace("Set current collection to {}", element);
        setTopVisibleElement(element);
        String url = getCollectionUrl(element);
        url = url.replace(BeanUtils.getServletPathWithHostAsUrlFromJsfContext(), "");
        return url;
    }

    /**
     * <p>
     * getCollectionUrl.
     * </p>
     *
     * @param collection a {@link io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement} object.
     * @return a {@link java.lang.String} object.
     * @throws URISyntaxException
     */
    public String getCollectionUrl(HierarchicalBrowseDcElement collection) {
        return getCollectionUrl(collection, field, getSearchUrl(), true);
    }

    public String getCollectionUrl(HierarchicalBrowseDcElement collection, boolean openInSearch) {
        return getCollectionUrl(collection, field, getSearchUrl(), openInSearch);
    }

    /**
     * <p>
     * getCollectionUrl.
     * </p>
     *
     * @param collection a {@link io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement} object.
     * @param field a {@link java.lang.String} object.
     * @param baseSearchUrl
     * @param openInSearch if true, return a search url if no cms page is associated with the collection. In case of single record in collection, the
     *            record may be opened directly
     * @return a {@link java.lang.String} object.
     * @throws URISyntaxException
     * @should return identifier resolver url if single record and pi known
     * @should escape critical url chars in collection name
     */
    public String getCollectionUrl(HierarchicalBrowseDcElement collection, String field, final String baseSearchUrl, boolean openInSearch) {
        if (hasCollectionPage(collection)) {
            return getCollectionPageUrl(collection);
        } else if (openInSearch) {
            if (hasSingleRecordLink(collection)) {
                return getFirstRecordUrl(collection, field);
            } else {
                return getSearchUrl(collection, field, baseSearchUrl);
            }
        } else {
            return getCollectionViewUrl(collection);
        }
    }

    public String getCollectionViewUrl(HierarchicalBrowseDcElement collection) {

        String baseUri = ViewHistory.getCurrentView(BeanUtils.getRequest())
                .map(view -> view.getApplicationUrl() + "/" + view.getPagePath().toString())
                .orElse("");
        baseUri = StringTools.appendTrailingSlash(baseUri);
        try {
            String ret = new URIBuilder(baseUri).addParameter("collection", collection.getName()).build().toString();
            logger.trace("COLLECTION new window url: {}", ret);
            return ret;
        } catch (URISyntaxException e) {
            logger.error("Error creating collection url ", e);
            return "";
        }

    }

    public boolean hasSingleRecordLink(HierarchicalBrowseDcElement collection) {
        return DataManager.getInstance().getConfiguration().isAllowRedirectCollectionToWork() && collection.getNumberOfVolumes() == 1;
    }

    public String getFirstRecordUrl(HierarchicalBrowseDcElement collection, String field) {

        // Link directly to single record, if record PI known
        if (collection.getSingleRecordUrl() != null) {
            return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + collection.getSingleRecordUrl();
        } else {

            return new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext())
                    .append("/browse/")
                    .append(field)
                    .append("/")
                    .append(collection.getLuceneName())
                    .append("/record/")
                    .toString();
        }
    }

    public boolean hasCollectionPage(HierarchicalBrowseDcElement collection) {
        return collection.getInfo().getLinkURI(BeanUtils.getRequest()) != null;
    }

    public String getCollectionPageUrl(HierarchicalBrowseDcElement collection) {

        if (hasCollectionPage(collection)) {
            String ret = collection.getInfo().getLinkURI(BeanUtils.getRequest()).toString();
            logger.trace("COLLECTION static url: {}", ret);
            return ret;
        } else {
            return "";
        }
    }

    public String getSearchUrl(HierarchicalBrowseDcElement collection, String field, final String baseSearchUrl) {
        String searchUrl = baseSearchUrl;
        if (StringUtils.isBlank(searchUrl)) {
            searchUrl = BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.search.getName() + "/";
        }
        String facetString = field + ":" + collection.getLuceneName();
        String encFacetString = StringTools.encodeUrl(facetString, true);
        return new StringBuilder(searchUrl)
                .append("-/-/1/")
                .append(collection.getSortField())
                .append('/')
                .append(encFacetString)
                .append('/')
                .toString();
    }

    /**
     * <p>
     * Setter for the field <code>displayParentCollections</code>.
     * </p>
     *
     * @param displayParents a boolean.
     * @deprecated should always be false
     */
    @Deprecated(since = "24.08")
    public void setDisplayParentCollections(boolean displayParents) {
        this.displayParentCollections = displayParents;
    }

    /**
     * <p>
     * isDisplayParentCollections.
     * </p>
     *
     * @return the displayParentCollections
     * @deprecated should always return false
     */
    @Deprecated(since = "24.08")
    public boolean isDisplayParentCollections() {
        return displayParentCollections;
    }

    /**
     * <p>
     * setIgnore.
     * </p>
     *
     * @param collectionName a {@link java.lang.String} object.
     */
    public void setIgnore(String collectionName) {
        this.ignoreList.add(collectionName);
    }

    /**
     * <p>
     * setIgnore.
     * </p>
     *
     * @param collectionNames a {@link java.util.Collection} object.
     */
    public void setIgnore(Collection<String> collectionNames) {
        this.ignoreList = new ArrayList<>(collectionNames);
    }

    /**
     * <p>
     * resetIgnore.
     * </p>
     */
    public void resetIgnore() {
        this.ignoreList = new ArrayList<>();
    }

    /**
     * Set the {@link io.goobi.viewer.model.viewer.collections.BrowseElementInfo} of the
     * {@link io.goobi.viewer.model.viewer.collections.BrowseDcElement} with the given name to the given info object
     *
     * @param name The collection name
     * @param info The info to apply
     */
    public void setCollectionInfo(String name, BrowseElementInfo info) {
        if (completeCollectionList != null) {
            completeCollectionList.stream()
                    .flatMap(ele -> ele.getAllDescendents(true).stream())
                    .filter(ele -> ele.getName().equals(name))
                    .findFirst()
                    .ifPresent(ele -> ele.setInfo(info));
        }
    }

    /**
     * Remove all custom collection info from the browse element with the given name. The element will get a new
     * {@link io.goobi.viewer.model.viewer.collections.SimpleBrowseElementInfo}
     *
     * @param name The collection name
     */
    public void removeCollectionInfo(String name) {
        if (completeCollectionList != null) {
            completeCollectionList.stream()
                    .flatMap(ele -> ele.getAllDescendents(true).stream())
                    .filter(ele -> ele.getName().equals(name))
                    .findFirst()
                    .ifPresent(ele -> ele.setInfo(new SimpleBrowseElementInfo(name)));
        }
    }

    /**
     *
     * @param name Raw collection name
     * @return Translation for the current langauge; null of none found
     */
    public String getTranslationForName(String name) {
        logger.trace("getTranslationForName: {}", name);
        if (completeCollectionList == null) {
            return null;
        }

        for (HierarchicalBrowseDcElement ele : completeCollectionList) {
            if (ele.getName().equals(name)) {
                return ele.getLabel();
            }
        }

        return null;
    }

    /**
     * <p>
     * Getter for the field <code>field</code>.
     * </p>
     *
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * @return the searchUrl
     */
    public String getSearchUrl() {
        return searchUrl;
    }

    /**
     * @param searchUrl the searchUrl to set
     */
    public void setSearchUrl(String searchUrl) {
        this.searchUrl = searchUrl;
    }

    /**
     * @return the ignoreHierarchy
     */
    public boolean isIgnoreHierarchy() {
        return ignoreHierarchy;
    }

    /**
     * @param ignoreHierarchy the ignoreHierarchy to set
     */
    public void setIgnoreHierarchy(boolean ignoreHierarchy) {
        this.ignoreHierarchy = ignoreHierarchy;
    }

    public static String getCollectionDefaultSortField(String name, Map<String, String> configuredSortFields) {

        String exactMatch = null;
        String inheritedMatch = null;
        for (Entry<String, String> entry : configuredSortFields.entrySet()) {
            if (name.equals(entry.getKey())) {
                exactMatch = entry.getValue();
            } else if (entry.getKey().endsWith("*") && name.startsWith(entry.getKey().substring(0, entry.getKey().length() - 1))) {
                inheritedMatch = entry.getValue();
            }
        }
        // Exact match is given priority so that it is possible to override the inherited sort field
        if (StringUtils.isNotEmpty(exactMatch)) {
            return exactMatch;
        }
        if (StringUtils.isNotEmpty(inheritedMatch)) {
            return inheritedMatch;
        }
        return "-";
    }

    /**
     * @return the splittingChar
     */
    public String getSplittingChar() {
        return splittingChar;
    }

    /**
     * @return the displayNumberOfVolumesLevel
     */
    public int getDisplayNumberOfVolumesLevel() {
        return displayNumberOfVolumesLevel;
    }

    public HierarchicalBrowseDcElement getCollectionElement(String name) {
        return getCollectionElement(name, false);
    }

    public HierarchicalBrowseDcElement getCollectionElement(String name, boolean includeDescendants) {
        return this.completeCollectionList.stream()
                .flatMap(e -> e.getAllDescendents(true).stream())
                .filter(e -> e.getName().equals(name))
                .findAny()
                .orElse(null);
    }

    public HierarchicalBrowseDcElement getBaseElement() {
        if (StringUtils.isNotBlank(this.getBaseElementName())) {
            return getCollectionElement(getBaseElementName());
        }
        return null;
    }
}
