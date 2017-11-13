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
import de.intranda.digiverso.presentation.model.cms.CMSMediaItem;

public class CollectionView {

    private static final Logger logger = LoggerFactory.getLogger(CollectionView.class);

    private List<HierarchicalBrowseDcElement> completeCollectionList;
    private List<HierarchicalBrowseDcElement> visibleCollectionList;

    private String field;
    private BrowseDataProvider dataProvider;
    private String topVisibleElement = null;
    private String baseElementName = null;
    private int baseLevels = 0;
    private boolean showAllHierarchyLevels = false;
    private boolean displayParentCollections = true;

    public CollectionView(String field, BrowseDataProvider dataProvider) {
        super();
        this.field = field;
        this.dataProvider = dataProvider;
    }

    /**
     * Creates a new CollectionView from an already existing one, keeping only the list of all collections without any display information
     * 
     * @param blueprint
     */
    public CollectionView(CollectionView blueprint) {
        this.completeCollectionList = blueprint.completeCollectionList.stream().map(element -> new HierarchicalBrowseDcElement(element)).collect(
                Collectors.toList());
        this.field = blueprint.field;
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
                    HierarchicalBrowseDcElement dc = new HierarchicalBrowseDcElement(collectionName, collectionSize, field, DataManager.getInstance()
                            .getConfiguration().getCollectionDefaultSortField(field, collectionName));
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
        if (StringUtils.isBlank(getBaseElementName()) && calculateLevel(collectionName) < getBaseLevels()) {
            return true;
        } else if (collectionName.equals(getBaseElementName())) {
            return true;
        } else if (collectionName.startsWith(getBaseElementName() + BrowseDcElement.split) && calculateLevel(collectionName) - calculateLevel(
                getBaseElementName()) <= getBaseLevels()) {
            return true;
        } else if (getBaseElementName() != null && getBaseElementName().startsWith(collectionName + BrowseDcElement.split)) {
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
                    visibleList.add(element);
                    visibleList.addAll(element.getAllVisibleDescendents(false));
                }
            } else {
                topElement.setShowSubElements(true);
                visibleList.add(topElement);
                visibleList.addAll(topElement.getAllVisibleDescendents(false));
                if (isDisplayParentCollections() && (baseElement == null || topElement.getName().contains(baseElement.getName()
                        + BrowseDcElement.split))) {
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
                    getTopVisibleElement());
            if (!isDisplayParentCollections()) {
                //if parent elements should be hidden, remove topElement from the list
                //This cannot be done earlier because it breaks sortDcList...
                this.visibleCollectionList.remove(0);
            }
            if (loadDescriptions) {
                try {
                    this.visibleCollectionList = associateWithCMSMediaItems(this.visibleCollectionList);
                } catch (PresentationException | DAOException e) {
                    logger.error("Failed to associate collections with media items: " + e.getMessage());
                }
            }
        }
    }

    /**
     * @param visibleCollectionList2
     * @return
     * @throws DAOException
     * @throws PresentationException
     */
    private static List<HierarchicalBrowseDcElement> associateWithCMSMediaItems(List<HierarchicalBrowseDcElement> collections) throws DAOException,
            PresentationException {
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
            } else if (elementName.contains(element.getName() + BrowseDcElement.split)) {
                HierarchicalBrowseDcElement matchingElement = getElement(elementName, element.getChildren());
                if (matchingElement != null) {
                    return matchingElement;
                }
            }
        }
        return null;
    }

    public int calculateLevel(String name) {
        if (StringUtils.isNotEmpty(BrowseDcElement.split)) {
            return name.split("\\" + BrowseDcElement.split).length - 1;
        }
        return 0;
    }

    public void resetCollectionList() {
        synchronized (this) {
            completeCollectionList = null;
        }
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
                this.visibleCollectionList = sortDcList(visibleCollectionList, DataManager.getInstance().getConfiguration().getCollectionSorting(
                        field), getTopVisibleElement());
            }
        }
        return "";
    }

    /**
     * Sorts the given <code>BrowseDcElement</code> list as defined in the configuration. All other elements are moved to the end of the list.
     *
     * @param inDcList The list to sort.
     * @param string
     * @param sortCriteriaList
     * @return A sorted list.
     */
    @SuppressWarnings("unchecked")
    protected static List<HierarchicalBrowseDcElement> sortDcList(List<HierarchicalBrowseDcElement> inDcList,
            List<DcSortingList> sortCriteriaSuperList, String topElement) {
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
                        } else if (dc.getName().startsWith(s + BrowseDcElement.split) && !sortCriteriaList.getCollections().contains(dc.getName())
                                && !unsortedSubCollections.contains(dc)) {
                            unsortedSubCollections.add(dc);
                            //                            logger.trace("adding dc to unsorted subcollections: {}", dc.getName());
                        }
                    }
                }
                List<HierarchicalBrowseDcElement> unsortedRest = ListUtils.subtract(inDcList, sortedDcList);
                unsortedRest = ListUtils.subtract(unsortedRest, unsortedSubCollections);
                int firstLevel = getLevelOfFirstElement(sortCriteriaList.getCollections());
                int index = getIndexOfElementWithName(unsortedRest, sortCriteriaList.getSortAfter(), firstLevel);
                unsortedRest.addAll(index, sortedDcList);
                inDcList = addUnsortedSubcollections(unsortedRest, unsortedSubCollections);
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
     * @return the first collection list
     */
    private static List<HierarchicalBrowseDcElement> addUnsortedSubcollections(List<HierarchicalBrowseDcElement> collections,
            List<HierarchicalBrowseDcElement> unsortedSubCollections) {
        Collections.reverse(unsortedSubCollections);
        for (HierarchicalBrowseDcElement unsortedCollection : unsortedSubCollections) {
            for (int i = collections.size() - 1; i > -1; i--) {
                String collectionName = collections.get(i).getName();
                if (unsortedCollection.getName().startsWith(collectionName + BrowseDcElement.split)) {
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
    private static int getLevelOfFirstElement(List<String> collections) {
        if (collections.isEmpty()) {
            return 0;
        }
        String collection = collections.get(0);
        return getCollectionLevel(collection);
    }

    private static int getCollectionLevel(String collection) {
        if (collection == null || collection.trim().isEmpty()) {
            return 0;
        }
        String separator = BrowseDcElement.split;
        if (!collection.contains(BrowseDcElement.split)) {
            return 1;
        }
        if (separator.equals(".")) {
            separator = "\\.";
        }
        String[] elementLevels = collection.split(separator);
        return elementLevels.length;
    }

    /**
     * @param unsortedRest
     * @param sortAfter
     * @param firstLevel
     * @return
     */
    private static int getIndexOfElementWithName(List<HierarchicalBrowseDcElement> elementList, String sortAfter, int sortingLevel) {
        int index = 0;
        Integer selectedIndex = null;
        int sortAfterLevel = getCollectionLevel(sortAfter);
        for (HierarchicalBrowseDcElement browseDcElement : elementList) {
            index++;
            if (browseDcElement.getName().equals(sortAfter) || browseDcElement.getName().startsWith(sortAfter + BrowseDcElement.split)
                    && sortingLevel <= sortAfterLevel) {
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
                    getTopVisibleElement());
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
            return topVisibleElement.split("\\" + BrowseDcElement.split).length - 1;
        }
        return getBaseElementLevel();
    }

    /**
     * @return
     */
    public int getBaseElementLevel() {
        if (baseElementName != null) {
            return baseElementName.split("\\" + BrowseDcElement.split).length - 1;
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

    public String getCollectionUrl(HierarchicalBrowseDcElement collection) {
        if (collection.getInfo().getLinkURI(BeanUtils.getRequest()) != null) {
            return collection.getInfo().getLinkURI(BeanUtils.getRequest()).toString();
        } else if (collection.isOpensInNewWindow()) {
            String baseUri = BeanUtils.getRequest().getRequestURL().toString();
            int cutoffIndex = baseUri.indexOf(PageType.expandCollection.getName());
            if (cutoffIndex > 0) {
                baseUri = baseUri.substring(0, cutoffIndex - 1);
            }
            return baseUri + "/" + PageType.expandCollection.getName() + "/" + collection.getName() + "/";
        } else if (collection.getNumberOfVolumes() == 1) {
            //            return collection.getRepresentativeUrl();
            return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.firstWorkInCollection.getName() + "/" + this.field + "/"
                    + collection.getLuceneName() + "/";
        } else {
            String url =  BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.browse.getName() + "/" + field + ':' + collection
                    .getLuceneName() + "/-/1/" + collection.getSortField() + "/-/";
            return url;
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
}
