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
package de.intranda.digiverso.presentation.model.viewer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.cms.CMSCollection;
import de.intranda.digiverso.presentation.model.cms.CMSMediaItem;
import de.intranda.digiverso.presentation.model.urlresolution.ViewHistory;

public class CollectionView {

    private static final Logger logger = LoggerFactory.getLogger(CollectionView.class);

    private List<HierarchicalBrowseDcElement> completeCollectionList = new ArrayList<>();
    private List<HierarchicalBrowseDcElement> visibleCollectionList = new ArrayList<>();

    private final String field;
    private final String splittingChar;
    private BrowseDataProvider dataProvider;
    private String topVisibleElement = null;
    private String baseElementName = null;
    private int baseLevels = 0;
    private boolean showAllHierarchyLevels = false;
    private boolean displayParentCollections = true;

    private List<String> ignoreList = new ArrayList<>();

    public CollectionView(String field, BrowseDataProvider dataProvider) {
        super();
        this.field = field;
        this.splittingChar = DataManager.getInstance().getConfiguration().getCollectionSplittingChar(field);
        this.dataProvider = dataProvider;
    }

    /**
     * Creates a new CollectionView from an already existing one, keeping only the list of all collections without any display information
     * 
     * @param blueprint
     */
    public CollectionView(CollectionView blueprint) {
        this.completeCollectionList =
                blueprint.completeCollectionList.stream().map(element -> new HierarchicalBrowseDcElement(element)).collect(Collectors.toList());
        this.field = blueprint.field;
        this.splittingChar = blueprint.splittingChar;
        this.dataProvider = blueprint.dataProvider;
    }

    public void populateCollectionList() throws IndexUnreachableException {
        synchronized (this) {
            try {
                logger.trace("populateCollectionList");
                Map<String, Long> dcStrings = dataProvider.getData();
                logger.trace("Creating browse elements...");
                completeCollectionList = new ArrayList<>(); // this has to be null and not empty at first; make sure it is initialized after the call to Solr
                HierarchicalBrowseDcElement lastElement = null;
                List<String> list = new ArrayList<>(dcStrings.keySet());
                Collections.sort(list);
                for (String dcName : list) {
                    String collectionName = dcName.intern();
                    long collectionSize = dcStrings.get(dcName);
                    HierarchicalBrowseDcElement dc = new HierarchicalBrowseDcElement(collectionName, collectionSize, field,
                            DataManager.getInstance().getConfiguration().getCollectionDefaultSortField(field, collectionName));
                    dc.setOpensInNewWindow(shouldOpenInOwnWindow(collectionName));
                    if (!shouldOpenInOwnWindow(collectionName) && showAllHierarchyLevels) {
                        dc.setShowSubElements(true);
                    }
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
                //            Collections.sort(completeCollectionList);
                calculateVisibleDcElements();
                logger.trace("populateCollectionList end");
            } catch (PresentationException e) {
                logger.error("Failed to initialize collection: " + e.toString());
            }
        }
    }

    /**
     * @param collectionName
     * @return
     */
    private boolean shouldOpenInOwnWindow(String collectionName) {
        if (StringUtils.isNotBlank(collectionName) && collectionName.equals(getTopVisibleElement())) {
            //if this is the current top element, open in search
            return false;
        } else if (StringUtils.isBlank(getBaseElementName()) && calculateLevel(collectionName) < getBaseLevels()) {
            //If we are beneath the base level, open in collection view
            return true;
        } else if (collectionName.equals(getBaseElementName())) {
            //If this is the base element of the entiry collection view, open in collection view (TODO: is that correct?)
            return true;
        } else if (collectionName.startsWith(getBaseElementName() + splittingChar)
                && calculateLevel(collectionName) - calculateLevel(getBaseElementName()) <= getBaseLevels()) {
            //If this is a subcollection of the base element and less than base levels beneath the base element, open in collection view (same as second 'if' but for views with a base element
            return true;
        } else if (getBaseElementName() != null && getBaseElementName().startsWith(collectionName + splittingChar)) {
            //If this is a parent of the base collection, open in collection view (effectively going upwards in the view hierarchy
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     */
    public void calculateVisibleDcElements() {
        calculateVisibleDcElements(true);
    }

    public void calculateVisibleDcElements(boolean loadDescriptions) {
        logger.trace("calculateVisibleDcElements: {}", loadDescriptions);
        if (completeCollectionList == null) {
            return;
        }
        synchronized (this) {
            List<HierarchicalBrowseDcElement> visibleList = new ArrayList<>();
            HierarchicalBrowseDcElement topElement = getElement(getTopVisibleElement(), completeCollectionList);
            HierarchicalBrowseDcElement baseElement = getElement(getBaseElementName(), completeCollectionList);
            if (topElement == null) {
                for (HierarchicalBrowseDcElement element : completeCollectionList) {
                    if (this.ignoreList.contains(element.getName())) {
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
                if (isDisplayParentCollections() && (baseElement == null || topElement.getName().contains(baseElement.getName() + splittingChar))) {
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
            this.visibleCollectionList = sortDcList(visibleList, DataManager.getInstance().getConfiguration().getCollectionSorting(field),
                    getTopVisibleElement(), splittingChar);
            if (!isDisplayParentCollections() && StringUtils.isNotBlank(topVisibleElement) && !this.visibleCollectionList.isEmpty()) {
                //if parent elements should be hidden, remove topElement from the list
                //This cannot be done earlier because it breaks sortDcList...
                this.visibleCollectionList.remove(0);
            }
            if (loadDescriptions) {
                associateElementsWithCMSData();
            }
        }
    }

    List<HierarchicalBrowseDcElement> getAncestors(String elementName, boolean includeSelf) {
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
     */
    public void associateElementsWithCMSData() {
        try {
            this.visibleCollectionList = associateWithCMSMediaItems(this.visibleCollectionList);
            this.visibleCollectionList = associateWithCMSCollections(this.visibleCollectionList, this.field);
        } catch (PresentationException | DAOException e) {
            logger.error("Failed to associate collections with media items: " + e.getMessage());
        }
    }

    /**
     * @param visibleCollectionList2
     * @return
     * @throws DAOException
     * @throws PresentationException
     */
    private static List<HierarchicalBrowseDcElement> associateWithCMSMediaItems(List<HierarchicalBrowseDcElement> collections)
            throws DAOException, PresentationException {
        List<CMSMediaItem> mediaItems = DataManager.getInstance().getDao().getAllCMSCollectionItems();
        if (mediaItems != null) {
            for (CMSMediaItem cmsMediaItem : mediaItems) {
                String collectionName = cmsMediaItem.getCollectionName();
                if (StringUtils.isBlank(collectionName)) {
                    continue;
                }
                HierarchicalBrowseDcElement searchItem = new HierarchicalBrowseDcElement(collectionName, 0, null, null);
                int index = collections.indexOf(searchItem);
                if (index > -1) {
                    HierarchicalBrowseDcElement element = collections.get(index);
                    element.setInfo(cmsMediaItem);
                }
            }
        }
        return collections;
    }

    private static List<HierarchicalBrowseDcElement> associateWithCMSCollections(List<HierarchicalBrowseDcElement> collections, String solrField)
            throws DAOException, PresentationException {
        List<CMSCollection> cmsCollections = DataManager.getInstance().getDao().getCMSCollections(solrField);
        if (cmsCollections != null) {
            for (CMSCollection cmsCollection : cmsCollections) {
                String collectionName = cmsCollection.getSolrFieldValue();
                if (StringUtils.isBlank(collectionName)) {
                    continue;
                }
                HierarchicalBrowseDcElement searchItem = new HierarchicalBrowseDcElement(collectionName, 0, null, null);
                int index = collections.indexOf(searchItem);
                if (index > -1) {
                    HierarchicalBrowseDcElement element = collections.get(index);
                    element.setInfo(cmsCollection);
                }
            }
        }
        return collections;
    }

    public List<HierarchicalBrowseDcElement> getVisibleDcElements() {
        logger.trace("getVisibleDcElements");
        return visibleCollectionList;
    }

    /**
     * @param topVisibleElement2
     * @return
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

    public int calculateLevel(String name) {
        if (StringUtils.isNotEmpty(splittingChar)) {
            int parts = name.split("\\" + splittingChar).length - 1;
            return parts;
        }
        return 0;
    }

    public void resetCollectionList() {
        synchronized (this) {
            completeCollectionList = null;
        }
    }

    public boolean isSubcollection() {
        boolean subcollection = StringUtils.isNotBlank(getTopVisibleElement()) && !getTopVisibleElement().equals(getBaseElementName());
        return subcollection;
    }

    public String getTopVisibleElement() {
        if (topVisibleElement == null && getBaseElementName() != null) {
            return getBaseElementName();
        }
        return topVisibleElement;
    }

    public void setTopVisibleElement(String topVisibleElement) {
        this.topVisibleElement = topVisibleElement;
    }

    public void setTopVisibleElement(HierarchicalBrowseDcElement element) {
        this.topVisibleElement = element.getName();
    }

    public void showChildren(HierarchicalBrowseDcElement element) {
        int elementIndex = visibleCollectionList.indexOf(element);
        if (elementIndex > -1) {
            visibleCollectionList.addAll(elementIndex + 1, element.getChildrenAndVisibleDescendants());
            element.setShowSubElements(true);
            associateElementsWithCMSData();
        }
    }

    public void hideChildren(HierarchicalBrowseDcElement element) {
        int elementIndex = visibleCollectionList.indexOf(element);
        if (elementIndex > -1) {
            visibleCollectionList.removeAll(element.getAllVisibleDescendents(true));
            element.setShowSubElements(false);
        }
        //        this.visibleCollectionList = sortDcList(visibleCollectionList, DataManager.getInstance().getConfiguration().getCollectionSorting());
    }

    public String toggleChildren(HierarchicalBrowseDcElement element) {
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
     * @param sortCriteriaList
     * @param sortCriteriaSuperList
     * @param topElement
     * @param splittingChar
     * @return A sorted list.
     */
    @SuppressWarnings("unchecked")
    protected static List<HierarchicalBrowseDcElement> sortDcList(List<HierarchicalBrowseDcElement> inDcList,
            List<DcSortingList> sortCriteriaSuperList, String topElement, String splittingChar) {
        if (sortCriteriaSuperList != null) {
            //iterate over all sorting lists
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
                    // logger.trace("sort: {}", s);
                    for (HierarchicalBrowseDcElement dc : inDcList) {
                        if (dc.getName().equals(s) && !sortedDcList.contains(dc)) {
                            sortedDcList.add(dc);
                            //                            logger.trace("adding dc: {}", dc.getName());
                        } else if (dc.getName().startsWith(s + splittingChar) && !sortCriteriaList.getCollections().contains(dc.getName())
                                && !unsortedSubCollections.contains(dc)) {
                            unsortedSubCollections.add(dc);
                            //                            logger.trace("adding dc to unsorted subcollections: {}", dc.getName());
                        }
                    }
                }
                List<HierarchicalBrowseDcElement> unsortedRest = ListUtils.subtract(inDcList, sortedDcList);
                unsortedRest = ListUtils.subtract(unsortedRest, unsortedSubCollections);
                int firstLevel = getLevelOfFirstElement(sortCriteriaList.getCollections(), splittingChar);
                int index = getIndexOfElementWithName(unsortedRest, sortCriteriaList.getSortAfter(), firstLevel, splittingChar);
                unsortedRest.addAll(index, sortedDcList);
                inDcList = addUnsortedSubcollections(unsortedRest, unsortedSubCollections, splittingChar);
            }
        }

        return inDcList;
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
     * @return The hiearchy level of the first collection within collections (1-based), or 0 if the list is empty
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
     * @return
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
     * @return
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
         * @return
         * @throws IndexUnreachableException
         */
        Map<String, Long> getData() throws IndexUnreachableException;

        //        /**
        //         * @return
        //         */
        //        String getSortField();

    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("COLLECTION\n");
        for (HierarchicalBrowseDcElement element : completeCollectionList) {
            sb.append("\t").append(element).append("\n");
        }
        return sb.toString();
    }

    /**
     * Sets all descendents of this element to visible
     *
     * @param element
     */
    public void expandAll(HierarchicalBrowseDcElement element) {
        expandAll(element, -1);
    }

    /**
     * Sets all descendents of this element to visible, but not beyond level 'depth'
     *
     * @param depth
     * @param element
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
     *
     */
    public void expandAll() {
        expandAll(-1);
    }

    /**
     * Sets all collection elements visible up to 'depth' levels into the hierarchy
     *
     * @param depth
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
     * @return
     */
    public List<HierarchicalBrowseDcElement> getCompleteList() {
        return completeCollectionList;
    }

    public void expand(HierarchicalBrowseDcElement element) throws IndexUnreachableException {
        setTopVisibleElement(element.getName());
        populateCollectionList();
    }

    /**
     * Resets the top visible element so the topmost hierarchy level is shown
     * 
     * @reset only actually resets if true
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public void reset(boolean reset) throws DAOException, IndexUnreachableException {
        if (reset && StringUtils.isNotBlank(getTopVisibleElement()) && !getTopVisibleElement().equals(getBaseElementName())) {
            setTopVisibleElement((String) null);
            populateCollectionList();
        }
    }

    public String getBaseElementName() {
        return baseElementName;
    }

    public void setBaseElementName(String baseElementName) {
        this.baseElementName = baseElementName;
        setTopVisibleElement(baseElementName);
    }

    public int getBaseLevels() {
        return baseLevels;
    }

    public void setBaseLevels(int baseLevels) {
        this.baseLevels = baseLevels;
    }

    public boolean isTopVisibleElement(HierarchicalBrowseDcElement element) {
        return element.getName().equals(getTopVisibleElement());
    }

    public void showAll() {
        if (completeCollectionList != null) {
            for (HierarchicalBrowseDcElement collection : completeCollectionList) {
                displayAllDescendents(collection);
            }
        }
        calculateVisibleDcElements();
    }

    public void hideAll() {
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

    public int getTopVisibleElementLevel() {
        if (topVisibleElement != null) {
            return topVisibleElement.split("\\" + splittingChar).length - 1;
        }
        return getBaseElementLevel();
    }

    /**
     * @return
     */
    public int getBaseElementLevel() {
        if (baseElementName != null) {
            return baseElementName.split("\\" + splittingChar).length - 1;
        }
        return 0;
    }

    /**
     * @param showAllHierarchyLevels the showAllHierarchyLevels to set
     */
    public void setShowAllHierarchyLevels(boolean showAllHierarchyLevels) {
        this.showAllHierarchyLevels = showAllHierarchyLevels;
    }

    /**
     * @return the showAllHierarchyLevels
     */
    public boolean isShowAllHierarchyLevels() {
        return showAllHierarchyLevels;
    }

    public String getCollectionUrl(String collection) {
        return getCompleteList().stream()
                .filter(element -> element.getName().equals(collection))
                .findFirst()
                .map(element -> getCollectionUrl(element))
                .orElse("");
    }

    public String loadCollection(HierarchicalBrowseDcElement element) {
        logger.debug("Set current collection to " + element);
        setTopVisibleElement(element);
        String url = getCollectionUrl(element);
        url = url.replace(BeanUtils.getServletPathWithHostAsUrlFromJsfContext(), "");
        return url;
    }

    public String getCollectionUrl(HierarchicalBrowseDcElement collection) {
        if (collection.getInfo().getLinkURI(BeanUtils.getRequest()) != null) {
            String ret = collection.getInfo().getLinkURI(BeanUtils.getRequest()).toString();
            logger.trace("COLLETION static url: {}", ret);
            return ret;
        } else if (collection.isOpensInNewWindow()) {
            String baseUri = ViewHistory.getCurrentView(BeanUtils.getRequest())
                    .map(view -> view.getApplicationUrl() + "/" + view.getPagePath().toString())
                    .orElse("");// BeanUtils.getRequest().getRequestURL().toString();
            //            int cutoffIndex = baseUri.indexOf(PageType.expandCollection.getName());
            //            if (cutoffIndex > 0) {
            //                baseUri = baseUri.substring(0, cutoffIndex - 1);
            //            }
            String ret = baseUri + "/" + PageType.expandCollection.getName() + "/" + collection.getName() + "/";
            logger.trace("COLLETION new window url: {}", ret);
            return ret;
        } else if (DataManager.getInstance().getConfiguration().isAllowRedirectCollectionToWork() && collection.getNumberOfVolumes() == 1) {
            //            return collection.getRepresentativeUrl();
            String ret = BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.firstWorkInCollection.getName() + "/" + this.field
                    + "/" + collection.getLuceneName() + "/";
            logger.trace("COLLETION single volume url: {}", ret);
            return ret;
        } else {
            String ret = new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append('/')
                    .append(PageType.browse.getName())
                    .append("/-/1/")
                    .append(collection.getSortField())
                    .append('/')
                    .append(field)
                    .append(':')
                    .append(collection.getLuceneName())
                    .append('/')
                    .toString();
            return ret;
        }
    }

    /**
     * @param collectionDisplayParents
     */
    public void setDisplayParentCollections(boolean displayParents) {
        this.displayParentCollections = displayParents;
    }

    /**
     * @return the displayParentCollections
     */
    public boolean isDisplayParentCollections() {
        return displayParentCollections;
    }

    public void setIgnore(String collectionName) {
        this.ignoreList.add(collectionName);
    }

    public void setIgnore(Collection<String> collectionNames) {
        this.ignoreList = new ArrayList<>(collectionNames);
    }

    public void resetIgnore() {
        this.ignoreList = new ArrayList<>();
    }
}
