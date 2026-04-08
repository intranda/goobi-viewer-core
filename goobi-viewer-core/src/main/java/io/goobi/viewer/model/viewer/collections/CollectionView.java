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
import jakarta.ws.rs.core.UriBuilder;

/**
 * Manages the hierarchical collection browsing state, including tree structure, sorting, and Solr queries.
 */
public class CollectionView implements Serializable {

    private static final long serialVersionUID = -9166556644187757289L;

    private static final Logger logger = LogManager.getLogger(CollectionView.class);

    private List<HierarchicalBrowseDcElement> completeCollectionList = new ArrayList<>();
    private List<HierarchicalBrowseDcElement> visibleCollectionList = new ArrayList<>();

    private final String field;
    private final String splittingChar;
    private BrowseDataProvider dataProvider;

    private String baseElementName = null;
    private boolean showAllHierarchyLevels = false;
    private String searchUrl = "";
    private boolean ignoreHierarchy = false;
    private final int displayNumberOfVolumesLevel;

    private List<String> ignoreList = new ArrayList<>();

    /**
     * Creates a new CollectionView instance.
     *
     * @param field Solr field name for this collection
     * @param dataProvider supplier of raw collection data from the index
     */
    public CollectionView(String field, BrowseDataProvider dataProvider) {
        super();
        this.field = field;
        this.splittingChar = DataManager.getInstance().getConfiguration().getCollectionSplittingChar(field);
        this.dataProvider = dataProvider;
        this.displayNumberOfVolumesLevel = DataManager.getInstance().getConfiguration().getCollectionDisplayNumberOfVolumesLevel(field);
    }

    /**
     * Creates a new CollectionView from an already existing one, keeping only the list of all collections without any display information.
     *
     * @param blueprint existing CollectionView to copy structure from
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
     * populateCollectionList.
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
     * @param collectionName collection identifier to test
     * @return boolean
     */
    private boolean shouldOpenInOwnWindow(String collectionName) {
        return getBaseElementName() != null && getBaseElementName().startsWith(collectionName + splittingChar);
    }

    /**
     * calculateVisibleDcElements.
     */
    public void calculateVisibleDcElements() {
        calculateVisibleDcElements(true);
    }

    /**
     * calculateVisibleDcElements.
     *
     * @param loadDescriptions If true, associated CMS collection configurations will be loaded
     */
    public void calculateVisibleDcElements(boolean loadDescriptions) {
        logger.trace("calculateVisibleDcElements: {}", loadDescriptions);
        if (completeCollectionList == null) {
            return;
        }
        synchronized (this) {
            List<HierarchicalBrowseDcElement> visibleList = new ArrayList<>();
            HierarchicalBrowseDcElement baseElement = getElement(getBaseElementName(), completeCollectionList);

            if (baseElement == null) {
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
                                || (!element.getName().startsWith(baseElement.getName() + splittingChar))) {
                            continue;
                        }
                        visibleList.add(element);
                        visibleList.addAll(element.getAllVisibleDescendents(false));
                    }
                } else {
                    baseElement.setShowSubElements(true);
                    visibleList.add(baseElement);
                    Collection<? extends HierarchicalBrowseDcElement> descendents = baseElement.getAllVisibleDescendents(false);
                    descendents = descendents.stream().filter(c -> !this.ignoreList.contains(c.getName())).collect(Collectors.toList());
                    visibleList.addAll(descendents);
                }
            }
            this.visibleCollectionList =
                    sortDcList(visibleList, DataManager.getInstance().getConfiguration().getCollectionSorting(field), getBaseElementName(),
                            splittingChar);
            if (!isIgnoreHierarchy() && StringUtils.isNotBlank(getBaseElementName())
                    && !this.visibleCollectionList.isEmpty()) {
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
     * @param elementName name of the collection element to find ancestors for
     * @param includeSelf if true, include the element itself in the result
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
     * associateElementsWithCMSData.
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
     * associateElementsWithCMSData.
     *
     * @param cmsCollections collection data with which to enricht the browse elements
     */
    public void associateElementsWithCMSData(List<CMSCollection> cmsCollections) {
        associateWithCMSCollections(new ArrayList<>(this.visibleCollectionList), cmsCollections);
    }

    public static void associateWithCMSCollections(List<HierarchicalBrowseDcElement> collections, String solrField)
            throws DAOException {
        List<CMSCollection> cmsCollections = DataManager.getInstance().getDao().getCMSCollections(solrField);
        associateWithCMSCollections(collections, cmsCollections);
    }

    /**
     * associateWithCMSCollections.
     *
     * <p>returns the 'collection' parameter
     *
     * @param collections browse elements to enrich with CMS data
     * @param cmsCollections list of CMS collection configurations to associate
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public static void associateWithCMSCollections(List<HierarchicalBrowseDcElement> collections, List<CMSCollection> cmsCollections) {
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
            element.ifPresent(ele -> ele.setInfo(cmsCollection.loadRepresentativeImage()));
        }
    }

    /**
     * getVisibleDcElements.
     *
     * @return a {@link java.util.List} object.
     */
    public List<HierarchicalBrowseDcElement> getVisibleDcElements() {
        // logger.trace("getVisibleDcElements"); //NOSONAR Debug
        return visibleCollectionList;
    }

    /**
     * @param elementName name of the collection element to find
     * @param collections list of collection elements to search
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
     * Counts the hierarchy level of the given collection name.
     *
     * @param collectionName collection identifier whose depth to count
     * @param splittingChar character used to separate hierarchy levels
     * @return -1 if collection is emtpy, otherwise the number of occurrences of the splitting char
     */
    public static int getLevel(String collectionName, String splittingChar) {
        if (StringUtils.isBlank(collectionName)) {
            return -1;
        }
        return collectionName.length() - collectionName.replace(splittingChar, "").length();
    }

    /**
     * resetCollectionList.
     */
    public void resetCollectionList() {
        synchronized (this) {
            completeCollectionList = new ArrayList<>();
        }
    }

    /**
     * showChildren.
     *
     * @param element collection element whose children to make visible
     */
    public void showChildren(HierarchicalBrowseDcElement element) {
        int elementIndex = visibleCollectionList.indexOf(element);
        if (elementIndex > -1) {
            visibleCollectionList.addAll(elementIndex + 1, element.getChildrenAndVisibleDescendants());
            element.setShowSubElements(true);
            // associateElementsWithCMSData();
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
     * hideChildren.
     *
     * @param element collection element whose children to hide
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
     * toggleChildren.
     *
     * @param element collection element whose children to toggle
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
                        DataManager.getInstance().getConfiguration().getCollectionSorting(field), getBaseElementName(), splittingChar);
                associateElementsWithCMSData();
            }
        }
        return "";
    }

    /**
     * Sorts the given <code>BrowseDcElement</code> list as defined in the configuration. All other elements are moved to the end of the list.
     *
     * @param inDcList list of collection elements to sort
     * @param sortCriteriaSuperList configured sort criteria lists defining the desired order
     * @param topElement name of the current root collection element, or null for top level
     * @param splittingChar character used to separate hierarchy levels
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
     * @param collections ordered list into which sub-collections are inserted
     * @param unsortedSubCollections sub-collections to insert at their correct hierarchy position
     * @param splittingChar character used to separate hierarchy levels
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
     * @param collections list of collection names to inspect
     * @param splittingChar character used to separate hierarchy levels
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
     * @param collection collection name whose nesting depth to compute
     * @param splittingChar character used to separate hierarchy levels
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
     * @param elementList list of collection elements to search for the insertion index
     * @param sortAfter name of the element after which sorted collections are inserted
     * @param sortingLevel hierarchy level of the sorted collection group
     * @param splittingChar character used to separate hierarchy levels
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
     * Sets all descendants of this element to visible.
     *
     * @param element collection element whose descendants to expand
     */
    public void expandAll(HierarchicalBrowseDcElement element) {
        expandAll(element, -1, true);
    }

    /**
     * Sets all descendants of this element to visible, but not beyond level 'depth'.
     *
     * @param depth maximum hierarchy level to expand; negative means unlimited
     * @param element collection element whose descendants to expand
     * @param loadDescriptions if true, load CMS collection descriptions after expanding
     */
    public void expandAll(HierarchicalBrowseDcElement element, int depth, boolean loadDescriptions) {
        if (depth < 0 || element.getLevel() < depth) {
            showChildren(element);
            for (HierarchicalBrowseDcElement child : element.getChildren()) {
                expandAll(child, depth, loadDescriptions);
            }
        }
    }

    /**
     * Sets all collection elements visible.
     */
    public void expandAll() {
        expandAll(-1, true);
    }

    /**
     * Sets all collection elements visible up to 'depth' levels into the hierarchy.
     *
     * @param depth maximum hierarchy level to expand; negative means unlimited
     * @param loadDescriptions if true, load CMS collection descriptions after expanding
     */
    public void expandAll(int depth, boolean loadDescriptions) {
        if (completeCollectionList != null) {
            for (HierarchicalBrowseDcElement collection : completeCollectionList) {
                expandAll(collection, depth, loadDescriptions);
            }
            this.visibleCollectionList = sortDcList(visibleCollectionList, DataManager.getInstance().getConfiguration().getCollectionSorting(field),
                    getBaseElementName(), splittingChar);
            if (loadDescriptions) {
                associateElementsWithCMSData();
            }
        }
    }

    /**
     * getCompleteList.
     *
     * @return a {@link java.util.List} object.
     */
    public List<HierarchicalBrowseDcElement> getCompleteList() {
        return completeCollectionList;
    }

    /**
     * Getter for the field <code>baseElementName</code>.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getBaseElementName() {
        return baseElementName;
    }

    /**
     * Setter for the field <code>baseElementName</code>.
     *
     * @param baseElementName name of the collection to use as root element
     */
    public void setBaseElementName(String baseElementName) {
        this.baseElementName = baseElementName;
    }

    /**
     * showAll.
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
     * hideAll.
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
     * @param collection collection element whose descendants are all made visible
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
     * getBaseElementLevel.
     *
     * @return a int.
     */
    public int getBaseElementLevel() {
        return getLevel(baseElementName, splittingChar);
    }

    /**
     * Setter for the field <code>showAllHierarchyLevels</code>.
     *
     * @param showAllHierarchyLevels true to expand and display all collection hierarchy levels
     */
    public void setShowAllHierarchyLevels(boolean showAllHierarchyLevels) {
        this.showAllHierarchyLevels = showAllHierarchyLevels;
    }

    /**
     * isShowAllHierarchyLevels.
     *
     * @return true if all hierarchy levels are shown expanded, false otherwise
     */
    public boolean isShowAllHierarchyLevels() {
        return showAllHierarchyLevels;
    }

    /**
     * getCollectionUrl.
     *
     * @param collection collection name to look up in the complete list
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
     * loadCollection.
     *
     * @param element a {@link io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement} object.
     * @return a {@link java.lang.String} object.
     */
    public String loadCollection(HierarchicalBrowseDcElement element) {
        logger.trace("Set current collection to {}", element);
        String url = getCollectionUrl(element);
        url = url.replace(BeanUtils.getServletPathWithHostAsUrlFromJsfContext(), "");
        return url;
    }

    /**
     * getCollectionUrl.
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
     * getCollectionUrl.
     *
     * @param collection a {@link io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement} object.
     * @param field Solr collection field name
     * @param baseSearchUrl base URL of the search page
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
            }
            return getSearchUrl(collection, field, baseSearchUrl);
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

    /**
     *
     * @param collection collection element whose first record URL to build
     * @param field Solr collection field name
     * @return URL of the first record in the given collection field/name combo
     * @should escape url encode collection name
     */
    public static String getFirstRecordUrl(HierarchicalBrowseDcElement collection, String field) {

        // Link directly to single record, if record PI known
        if (collection.getSingleRecordUrl() != null) {
            return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + collection.getSingleRecordUrl();
        }

        String escapedCollectionName = StringTools.encodeUrl(collection.getLuceneName(), true);
        return new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext())
                .append("/browse/")
                .append(field)
                .append("/")
                .append(escapedCollectionName)
                .append("/record/")
                .toString();
    }

    public boolean hasCollectionPage(HierarchicalBrowseDcElement collection) {
        return collection.getInfo().getLinkURI(BeanUtils.getRequest()) != null;
    }

    public String getCollectionPageUrl(HierarchicalBrowseDcElement collection) {

        if (hasCollectionPage(collection)) {
            String ret = collection.getInfo().getLinkURI(BeanUtils.getRequest()).toString();
            logger.trace("COLLECTION static url: {}", ret);
            return ret;
        }

        return "";
    }

    public String getSearchUrl(HierarchicalBrowseDcElement collection, String field, final String baseSearchUrl) {
        String useSearchUrl = baseSearchUrl;
        if (StringUtils.isBlank(useSearchUrl)) {
            useSearchUrl = BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.search.getName() + "/";
        }
        String facetString = field + ":" + collection.getLuceneName();
        String encFacetString = StringTools.encodeUrl(facetString, true);
        return new StringBuilder(useSearchUrl)
                .append("-/-/1/")
                .append(collection.getSortField())
                .append('/')
                .append(encFacetString)
                .append('/')
                .toString();
    }

    /**
     * setIgnore.
     *
     * @param collectionName collection name to exclude from the visible list
     */
    public void setIgnore(String collectionName) {
        this.ignoreList.add(collectionName);
    }

    /**
     * setIgnore.
     *
     * @param collectionNames collection names to exclude from the visible list
     */
    public void setIgnore(Collection<String> collectionNames) {
        this.ignoreList = new ArrayList<>(collectionNames);
    }

    /**
     * resetIgnore.
     */
    public void resetIgnore() {
        this.ignoreList = new ArrayList<>();
    }

    /**
     * Set the {@link io.goobi.viewer.model.viewer.collections.BrowseElementInfo} of the
     * {@link io.goobi.viewer.model.viewer.collections.BrowseDcElement} with the given name to the given info object.
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
     * Getter for the field <code>field</code>.
     *
     * @return the Solr field name that provides the collection hierarchy for this view
     */
    public String getField() {
        return field;
    }

    
    public String getSearchUrl() {
        return searchUrl;
    }

    
    public void setSearchUrl(String searchUrl) {
        this.searchUrl = searchUrl;
    }

    
    public boolean isIgnoreHierarchy() {
        return ignoreHierarchy;
    }

    
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

    
    public String getSplittingChar() {
        return splittingChar;
    }

    
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
