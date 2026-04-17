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
package io.goobi.viewer.model.toc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * Table of contents and associated functionality for a record.
 */
public class TOC implements Serializable {

    private static final long serialVersionUID = 2615373293377347746L;

    private static final Logger logger = LogManager.getLogger(TOC.class);

    /** TOC element map. */
    private Map<String, List<TOCElement>> tocElementMap;

    private boolean treeBuilt = false;

    private int tocVisible = -1;

    private int tocInvisible = -1;

    private int maxTocDepth = 0;

    private int totalTocSize = 0;

    private int currentPage = 1;

    /**
     * Creates a new TOC instance.
     */
    public TOC() {
        logger.trace("new TOC()");
    }

    /**
     * generate.
     *
     * @param structElement root struct element for TOC generation
     * @param addAllSiblings if true, sibling elements are included in the TOC
     * @param mimeType MIME type of the record
     * @param tocCurrentPage current paginator page of anchor group elements
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public void generate(StructElement structElement, boolean addAllSiblings, String mimeType, int tocCurrentPage)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        synchronized (this) {
            tocElementMap = TocMaker.generateToc(this, structElement, addAllSiblings, mimeType, tocCurrentPage,
                    DataManager.getInstance().getConfiguration().getTocAnchorGroupElementsPerPage());
            // Reset the flag so buildTree runs again for the new tocElementMap data.
            treeBuilt = false;
        }
    }

    /**
     * getGroupNames.
     *
     * @return a list of TOC group names available in this table of contents
     */
    public List<String> getGroupNames() {
        if (tocElementMap != null) {
            return new ArrayList<>(tocElementMap.keySet());
        }

        return Collections.emptyList();
    }

    /**
     * getViewForGroup.
     *
     * @param group TOC group name to retrieve elements for
     * @return a list of TOC elements belonging to the given group, or null if none found
     */
    public List<TOCElement> getViewForGroup(String group) {
        if (tocElementMap != null) {
            return tocElementMap.get(group);
        }

        return null;
    }

    /**
     * getTreeViewForGroup.
     *
     * @param group TOC group name to build the tree for
     * @return a list of TOC elements for the given group with tree nesting applied
     * @should not throw NPE when ViewManager is null
     */
    public List<TOCElement> getTreeViewForGroup(String group) {
        if (!treeBuilt) {
            int visibleLevel = DataManager.getInstance().getConfiguration().getSidebarTocInitialCollapseLevel();
            int collapseThreshold = DataManager.getInstance().getConfiguration().getSidebarTocCollapseLengthThreshold();
            int lowestLevelToCollapse = DataManager.getInstance().getConfiguration().getSidebarTocLowestLevelToCollapseForLength();
            // Guard against TOCTOU: fetch bean once and null-check ViewManager directly,
            // because isRecordLoaded() and getViewManager() may race in concurrent requests.
            io.goobi.viewer.managedbeans.ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
            io.goobi.viewer.model.viewer.ViewManager vm = adb != null ? adb.getViewManager() : null;
            String currentElementIdDoc = vm != null ? vm.getCurrentStructElementIddoc() : null;
            buildTree(group, visibleLevel, collapseThreshold, lowestLevelToCollapse, currentElementIdDoc);
        }
        return getViewForGroup(group);
    }

    /**
     * getFlatView.
     *
     * @return a flat list of all TOC elements in the default group
     */
    public List<TOCElement> getFlatView() {
        // logger.trace("getFlatView"); //NOSONAR Debug
        return getViewForGroup(StringConstants.DEFAULT_NAME);
    }

    /**
     * getTreeView.
     *
     * @return a list of TOC elements for the default group with tree nesting applied
     */
    public List<TOCElement> getTreeView() {
        return getTreeViewForGroup(StringConstants.DEFAULT_NAME);
    }

    /**
     *
     * @param group TOC group name to build the tree for
     * @param visibleLevel maximum hierarchy level shown expanded initially
     * @param collapseThreshold sibling count above which a level is auto-collapsed
     * @param lowestLevelToCollapse minimum hierarchy level eligible for length-based collapse
     * @param currentElementIdDoc IDDOC of the currently displayed struct element
     * @should expand ancestors of target element
     * @should not throw NPE when group not in tocElementMap
     * @should not throw NPE when group not in map
     */
    protected void buildTree(String group, int visibleLevel, int collapseThreshold, int lowestLevelToCollapse, String currentElementIdDoc) {
        logger.trace("buildTree");
        if (group == null) {
            throw new IllegalArgumentException("group may not be null");
        }

        // Capture the group list before entering the synchronized block so we can pass
        // the same (non-null-checked) reference to uncollapseCurrentElementAncestors below.
        List<TOCElement> groupElements = null;
        synchronized (this) {
            if (tocElementMap != null) {
                groupElements = tocElementMap.get(group);
                // Guard: if the requested group is absent from the map, skip tree building.
                // This prevents an NPE when tocElementMap is concurrently replaced with a
                // new instance that has different keys (e.g. after generate() is called for
                // a different document between getGroupNames() and getTreeViewForGroup()).
                if (groupElements == null) {
                    logger.warn("Requested TOC group '{}' not found in tocElementMap (available: {}); skipping tree build.", group,
                            tocElementMap.keySet());
                    return;
                }
                // long start = System.nanoTime(); //NOSONAR Debug
                int lastLevel = 0;
                int lastParent = 0;
                for (TOCElement tocElement : groupElements) {
                    // Current element index
                    int index = groupElements.indexOf(tocElement);
                    tocElement.setID(index);
                    if (tocElement.getLevel() > maxTocDepth) {
                        maxTocDepth = tocElement.getLevel();
                    }

                    if (lastLevel < tocElement.getLevel() && index > 0) {
                        tocElement.setParentId(lastParent);
                        groupElements.get(index - 1).setHasChild(true);
                        if (tocElement.getLevel() > visibleLevel) {
                            groupElements.get(index - 1).setExpanded(false);
                            tocElement.setVisible(false);
                        } else {
                            groupElements.get(index - 1).setExpanded(true);
                        }

                        for (int i = index + 1; i < groupElements.size(); i++) {
                            TOCElement tc = groupElements.get(i);
                            if (tc.getLevel() == tocElement.getLevel()) {
                                // Elements on the same level as the current element get the same parent ID and are set visible
                                tc.setParentId(tocElement.getParentId());
                            }
                            if (tc.getLevel() > visibleLevel) {

                                tc.setVisible(false);
                            }
                        }

                    }
                    lastParent = index;
                    lastLevel = tocElement.getLevel();
                }
                // long end = System.nanoTime(); //NOSONAR Debug
                // logger.trace("Time for initial collapse: {} ns", (end - start)); //NOSONAR Debug
                collapseTocForLength(collapseThreshold, lowestLevelToCollapse);
                treeBuilt = true;
            }
        }
        uncollapseCurrentElementAncestors(groupElements, currentElementIdDoc);
    }

    /**
     *
     * @param list flat list of all TOC elements
     * @param currentElementIdDoc IDDOC of the currently displayed struct element
     */
    private void uncollapseCurrentElementAncestors(List<TOCElement> list, String currentElementIdDoc) {
        // Guard against null list (group absent from map) or unknown current element.
        if (list == null || currentElementIdDoc == null) {
            return;
        }
        TOCElement currentElement = getElement(list, currentElementIdDoc);
        if (currentElement != null) {
            currentElement.setVisible(true);
            int parentId = currentElement.getParentId();
            TOCElement parent = getElement(list, parentId);
            // recursively expand all direct ancestors
            while (parent != null) {
                parent.setExpanded(true);
                expandTree(parentId);
                parentId = parent.getParentId();
                parent = getElement(list, parentId);
            }
        }
    }

    /**
     *
     * @param list flat list of all TOC elements
     * @param iddoc IDDOC value to search for
     * @return {@link TOCElement}
     */
    private static TOCElement getElement(List<TOCElement> list, String iddoc) {
        return list.stream().filter(ele -> ele.getIddoc().equals(iddoc.toString())).findAny().orElse(null);
    }

    /**
     *
     * @param list flat list of all TOC elements
     * @param id numeric element ID to search for
     * @return {@link TOCElement}
     */
    private static TOCElement getElement(List<TOCElement> list, int id) {
        return list.stream().filter(ele -> ele.getID() == id).findAny().orElse(null);
    }

    /**
     *
     * @param collapseThreshold sibling count above which a level is auto-collapsed
     * @param lowestLevelToCollapse minimum hierarchy level eligible for length-based collapse
     */
    private void collapseTocForLength(int collapseThreshold, int lowestLevelToCollapse) {
        if (collapseThreshold <= 0 || tocElementMap == null) {
            return;
        }

        // long start = System.nanoTime();s
        int index = 0;
        int hideLevel = -1;
        boolean hide = false;
        for (index = 0; index < tocElementMap.get(StringConstants.DEFAULT_NAME).size(); index++) {
            TOCElement tocElem = tocElementMap.get(StringConstants.DEFAULT_NAME).get(index);

            if (tocElem.getLevel() < hideLevel || tocElem.getLevel() < lowestLevelToCollapse) {
                //if we return above the hidden level, reset flags
                hide = false;
                hideLevel = -1;
            } else if (hide) {
                //if hide flag is set from previous sibling, hide and collapse this element
                tocElem.setExpanded(false);
                tocElem.setVisible(false);
            } else {
                //else check if we need to hide this and following siblings
                int levelLength = 0;
                for (int i = index; i < tocElementMap.get(StringConstants.DEFAULT_NAME).size(); i++) {
                    TOCElement tempElem = tocElementMap.get(StringConstants.DEFAULT_NAME).get(i);
                    if (tempElem.getLevel() < tocElem.getLevel()) {
                        break;
                    } else if (tempElem.getLevel() == tocElem.getLevel()) {
                        levelLength++;
                    }
                }
                if (levelLength > collapseThreshold) {
                    tocElementMap.get(StringConstants.DEFAULT_NAME).get(index - 1).setExpanded(false); //collapse parent
                    hideLevel = tocElem.getLevel();
                    hide = true;
                    tocElem.setExpanded(false);
                    tocElem.setVisible(false);
                }
                if (levelLength > collapseThreshold) {
                    tocElementMap.get(StringConstants.DEFAULT_NAME).get(index - 1).setExpanded(false); //collapse parent
                    hideLevel = tocElem.getLevel();
                    hide = true;
                    tocElem.setExpanded(false);
                    tocElem.setVisible(false);
                }
            }
        }

        // long end = System.nanoTime(); //NOSONAR Debug
        // logger.trace("Time for length collapse: {} ns", (end - start)) //NOSONAR Debug
    }

    /**
     * Recalculates the visibility of TOC elements after a +/- button has been pressed.
     *
     * @return the TOCElement that was expanded or collapsed after processing the pending visibility change, or null if none was pending
     */
    public TOCElement getActiveElement() {
        TOCElement activeTocElement = null;
        if (tocElementMap != null) {
            if (tocVisible != -1) {
                expandTree(tocVisible);
                activeTocElement = tocElementMap.get(StringConstants.DEFAULT_NAME).get(tocVisible);
                activeTocElement.setExpanded(true);
                tocVisible = -1;
            }
            if (tocInvisible != -1) {
                collapseTree(tocInvisible);
                activeTocElement = tocElementMap.get(StringConstants.DEFAULT_NAME).get(tocInvisible);
                activeTocElement.setExpanded(false);
                tocInvisible = -1;
            }
        }

        return activeTocElement;
    }

    /**
     * Collapses all elements below the element with the given ID.
     *
     * @param parentId index of the parent element in the flat TOC list
     */
    private void collapseTree(int parentId) {
        logger.trace("collapseTree: {}", parentId);
        if (tocElementMap != null) {
            int level = tocElementMap.get(StringConstants.DEFAULT_NAME).get(parentId).getLevel();
            for (int i = parentId + 1; i < tocElementMap.get(StringConstants.DEFAULT_NAME).size(); i++) {
                TOCElement child = tocElementMap.get(StringConstants.DEFAULT_NAME).get(i);
                if (child.getLevel() > level) {
                    child.setVisible(false);
                } else {
                    // Rest of the elements are irrelevant because they belong
                    // to a different subtree on the same level
                    break;
                }
            }
        }
    }

    /**
     * Recursively expands the child elements of the element with the given ID.
     *
     * @param parentId index of the parent element in the flat TOC list
     */
    private void expandTree(int parentId) {
        // logger.trace("expandTree: {}", parentId); //NOSONAR Debug
        if (tocElementMap != null) {
            int level = tocElementMap.get(StringConstants.DEFAULT_NAME).get(parentId).getLevel();
            for (int i = parentId + 1; i < tocElementMap.get(StringConstants.DEFAULT_NAME).size(); i++) {
                TOCElement child = tocElementMap.get(StringConstants.DEFAULT_NAME).get(i);
                if (child.getLevel() == level + 1) {
                    // Set immediate children visible
                    child.setVisible(true);
                    // Elements further down the tree are handled recursively
                    if (child.isHasChild() && child.isExpanded()) {
                        expandTree(child.getID());
                    }
                } else if (child.getLevel() <= level) {
                    // Rest of the elements are irrelevant because they belong
                    // to a different subtree on the same level
                    break;
                }
            }
        }
    }

    /**
     * expandAll.
     */
    public void expandAll() {
        logger.trace("expandAll");
        if (tocElementMap != null) {
            for (TOCElement tcElem : tocElementMap.get(StringConstants.DEFAULT_NAME)) {
                tcElem.setVisible(true);
                if (tcElem.isHasChild()) {
                    tcElem.setExpanded(true);
                }
            }
        }
    }

    /**
     * collapseAll.
     */
    public void collapseAll() {
        logger.trace("collapseAll");
        if (tocElementMap != null) {
            for (TOCElement tcElem : tocElementMap.get(StringConstants.DEFAULT_NAME)) {
                if (tcElem.getLevel() == 0) {
                    tcElem.setExpanded(false);
                } else {
                    tcElem.setVisible(false);
                }
            }
        }
    }

    /**
     * setChildVisible.
     *
     * @param id index of the TOC element to expand
     */
    public void setChildVisible(int id) {
        this.tocVisible = id;
    }

    /**
     * setChildInvisible.
     *
     * @param id index of the TOC element to collapse
     */
    public void setChildInvisible(int id) {
        this.tocInvisible = id;
    }

    
    Map<String, List<TOCElement>> getTocElementMap() {
        return tocElementMap;
    }

    
    void setTocElementMap(Map<String, List<TOCElement>> tocElementMap) {
        this.tocElementMap = tocElementMap;
    }

    /**
     * getTocElements.
     *
     * @return a list of all TOC elements in the default group
     */
    public List<TOCElement> getTocElements() {
        if (tocElementMap != null) {
            return tocElementMap.getOrDefault(StringConstants.DEFAULT_NAME, Collections.emptyList());
        }

        return Collections.emptyList();
    }

    /**
     *
     * @param iddoc IDDOC of the element to find
     * @return Index of the element with the matching IDDOC within the list of elements; -1 if none found
     */
    public int findTocElementIndexByIddoc(String iddoc) {
        // logger.trace("findTocElementIndexByIddoc: {}", iddoc); //NOSONAR Debug
        if (StringUtils.isEmpty(iddoc)) {
            return -1;
        }

        int index = 0;
        for (TOCElement element : getTocElements()) {
            if (iddoc.equals(element.getIddoc())) {
                return index;
            }
            index++;
        }

        return -1;
    }

    /**
     * Getter for the field <code>tocVisible</code>.
     *
     * @return the number of visible TOC entries on the current page
     */
    public int getTocVisible() {
        return tocVisible;
    }

    /**
     * Setter for the field <code>tocVisible</code>.
     *
     * @param tocVisible the number of visible TOC entries on the current page to set
     */
    public void setTocVisible(int tocVisible) {
        this.tocVisible = tocVisible;
    }

    /**
     * Getter for the field <code>tocInvisible</code>.
     *
     * @return the number of hidden (collapsed) TOC entries on the current page
     */
    public int getTocInvisible() {
        return tocInvisible;
    }

    /**
     * Setter for the field <code>tocInvisible</code>.
     *
     * @param tocInvisible the number of hidden (collapsed) TOC entries on the current page to set
     */
    public void setTocInvisible(int tocInvisible) {
        this.tocInvisible = tocInvisible;
    }

    /**
     * Getter for the field <code>maxTocDepth</code>.
     *
     * @return the maximum nesting depth of the TOC tree
     */
    public int getMaxTocDepth() {
        // If this method is called before getTreeView, the depth will always be 0
        if (!treeBuilt) {
            logger.trace("Building tree to determine the TOC depth");
            getTreeView();
        }
        // logger.trace("getMaxTocDepth: {}", maxTocDepth); //NOSONAR Debug
        return maxTocDepth;
    }

    /**
     * Setter for the field <code>maxTocDepth</code>.
     *
     * @param maxTocDepth the maximum nesting depth of the TOC tree to set
     */
    public void setMaxTocDepth(int maxTocDepth) {
        this.maxTocDepth = maxTocDepth;
    }

    /**
     * Getter for the field <code>totalTocSize</code>.
     *
     * @return the total number of TOC entries across all pages
     */
    public int getTotalTocSize() {
        return totalTocSize;
    }

    /**
     * Setter for the field <code>totalTocSize</code>.
     *
     * @param totalTocSize the total number of TOC entries across all pages to set
     */
    public void setTotalTocSize(int totalTocSize) {
        this.totalTocSize = totalTocSize;
    }

    /**
     * Getter for the field <code>currentPage</code>.
     *
     * @return the 1-based page number of the currently displayed TOC page
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Setter for the field <code>currentPage</code>.
     *
     * @param currentPage the 1-based page number of the TOC page to display
     * @should set value to 1 if given value too low
     * @should set value to last page number if given value too high
     */
    public void setCurrentPage(int currentPage) {
        if (currentPage < 1) {
            this.currentPage = 1;
            return;
        }

        int numPages = getNumPages();
        if (currentPage > numPages) {
            this.currentPage = numPages;
        } else {
            this.currentPage = currentPage;
        }
    }

    /**
     * Returns the number of paginator pages for the given TOC size and elements per page.
     *
     * @should return ceiling division of total TOC size by page size
     * @return a int.
     */
    public int getNumPages() {
        int hitsPerPage = DataManager.getInstance().getConfiguration().getTocAnchorGroupElementsPerPage();
        if (hitsPerPage > 0) {
            // logger.trace("numPages: {}/{}={}", totalTocSize, hitsPerPage, totalTocSize / hitsPerPage); //NOSONAR Debug
            int num = totalTocSize / hitsPerPage;
            if (totalTocSize % hitsPerPage != 0 || num == 0) {
                num++;
            }
            return num;
        }

        return 1;
    }

    /**
     * hasChildren.
     *
     * @return true if the TOC contains more than one element or any element has child entries, false otherwise
     */
    public boolean isHasChildren() {
        if (tocElementMap == null || tocElementMap.get(StringConstants.DEFAULT_NAME) == null
                || tocElementMap.get(StringConstants.DEFAULT_NAME).isEmpty()) {
            return false;
        }

        return !(tocElementMap.get(StringConstants.DEFAULT_NAME).size() == 1 && !tocElementMap.get(StringConstants.DEFAULT_NAME).get(0).isHasChild());
    }

    /**
     * Returns the label of the first found TOCElement that has the given PI as its topStructPi.
     *
     * @param pi persistent identifier of the record
     * @should return correct label
     * @return the label of the matching TOC element, or null if none found
     */
    public String getLabel(String pi) {
        return getLabel(pi, MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
    }

    /**
     * Returns the label in the given language of the first found TOCElement that has the given PI as its topStructPi.
     *
     * @param pi persistent identifier of the record
     * @should return correct label
     * @param language ISO 639-1 language code for the desired label
     * @return the language-specific label of the matching TOC element, or null if none found
     */
    public String getLabel(String pi, String language) {
        if (StringUtils.isEmpty(pi)) {
            return null;
        }

        List<TOCElement> tocElements = getTocElements();
        if (tocElements != null) {
            for (TOCElement element : tocElements) {
                if (pi.equals(element.getTopStructPi())) {
                    return element.getLabel(language);
                }
            }
        }

        return null;
    }

    /**
     * Returns the label in the given locale of the first found TOCElement that has the given PI as its topStructPi.
     *
     * @param pi persistent identifier of the record
     * @should return correct label
     * @param locale locale for the desired label language
     * @return the locale-specific label of the matching TOC element, or null if none found
     */
    public String getLabel(String pi, Locale locale) {
        return getLabel(pi, locale.getLanguage());
    }
}