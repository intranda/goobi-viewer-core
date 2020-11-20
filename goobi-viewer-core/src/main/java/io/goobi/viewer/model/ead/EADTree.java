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
package io.goobi.viewer.model.ead;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;

/**
 * Table of contents and associated functionality for a record.
 */
public class EADTree implements Serializable {

    private static final long serialVersionUID = 1798213211987072214L;

    private static final Logger logger = LoggerFactory.getLogger(EADTree.class);

    /** Constant <code>DEFAULT_GROUP="_DEFAULT"</code> */
    public static final String DEFAULT_GROUP = "_DEFAULT";

    /** TOC element map. */
    private Map<String, List<EadEntry>> entryMap = new HashMap<>(1);

    private boolean treeBuilt = false;

    private int tocVisible = -1;

    private int tocInvisible = -1;

    private int maxTocDepth = 0;

    private int totalTocSize = 0;

    private int currentPage = 1;

    /**
     * <p>
     * Constructor for TOC.
     * </p>
     */
    public EADTree() {
        logger.trace("new EADTree()");
    }

    public void generate(EadEntry root) {
        if (root == null) {
            throw new IllegalArgumentException("root may not be null");
        }

        List<EadEntry> tree = root.getAsFlatList();
        // remove root
//        if(tree.size() > 1) {
//            tree = tree.subList(1, tree.size());
//        }
        entryMap.put(DEFAULT_GROUP, tree);
    }

    /**
     * <p>
     * getViewForGroup.
     * </p>
     *
     * @param group a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public List<EadEntry> getViewForGroup(String group) {
        if (entryMap != null) {
            return entryMap.get(group);
        }

        return null;
    }

    /**
     * <p>
     * getTreeViewForGroup.
     * </p>
     *
     * @param group a {@link java.lang.String} object.
     * @should call buildTree and set maxTocDepth correctly
     * @return a {@link java.util.List} object.
     */
    public List<EadEntry> getTreeViewForGroup(String group) {
        if (!treeBuilt) {
            int visibleLevel = DataManager.getInstance().getConfiguration().getSidebarTocInitialCollapseLevel();
            int collapseThreshold = DataManager.getInstance().getConfiguration().getSidebarTocCollapseLengthThreshold();
            int lowestLevelToCollapse = DataManager.getInstance().getConfiguration().getSidebarTocLowestLevelToCollapseForLength();
            buildTree(group, visibleLevel, collapseThreshold, lowestLevelToCollapse);
        }
        return getViewForGroup(group);
    }

    /**
     * <p>
     * getFlatView.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<EadEntry> getFlatView() {
        // logger.trace("getFlatView");
        return getViewForGroup(DEFAULT_GROUP);
    }

    /**
     * <p>
     * getTreeView.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<EadEntry> getTreeView() {
        return getTreeViewForGroup(DEFAULT_GROUP);
    }

    /**
     *
     * @param group
     * @param visibleLevel
     * @param collapseThreshold
     * @param lowestLevelToCollapse
     */
    private void buildTree(String group, int visibleLevel, int collapseThreshold, int lowestLevelToCollapse) {
        logger.trace("buildTree");
        if (group == null) {
            throw new IllegalArgumentException("group may not be null");
        }

        synchronized (this) {
            if (entryMap == null) {
                return;
            }
            //                long start = System.nanoTime();
            int lastLevel = 0;
            int lastParent = 0;
            for (EadEntry tocElement : entryMap.get(group)) {
                // Current element index
                int index = entryMap.get(group).indexOf(tocElement);
                tocElement.setIndex(index);
                if (tocElement.getHierarchy() > maxTocDepth) {
                    maxTocDepth = tocElement.getHierarchy();
                }

                if (lastLevel < tocElement.getHierarchy() && index > 0) {
                    tocElement.setParentIndex(lastParent);
                    entryMap.get(group).get(index - 1).setHasChild(true);
                    if (tocElement.getHierarchy() > visibleLevel) {
                        entryMap.get(group).get(index - 1).setExpanded(false);
                        tocElement.setVisible(false);
                    } else {
                        entryMap.get(group).get(index - 1).setExpanded(true);
                    }

                    for (int i = index + 1; i < entryMap.get(group).size(); i++) {
                        EadEntry tc = entryMap.get(group).get(i);
                        if (tc.getHierarchy() == tocElement.getHierarchy()) {
                            // Elements on the same level as the current element get the same parent ID and are set visible
                            tc.setParentIndex(tocElement.getParentIndex());
                        }
                        if (tc.getHierarchy() > visibleLevel) {

                            tc.setVisible(false);
                        }
                    }

                }
                lastParent = index;
                lastLevel = tocElement.getHierarchy();
            }
            //                long end = System.nanoTime();
            //                logger.trace("Time for initial collapse: {} ns", (end - start));
            collapseTocForLength(collapseThreshold, lowestLevelToCollapse);
            treeBuilt = true;
        }
    }

    private void collapseTocForLength(int collapseThreshold, int lowestLevelToCollapse) {
        if (collapseThreshold > 0 && entryMap != null) {
            //        long start = System.nanoTime();
            int index = 0;
            int hideLevel = -1;
            boolean hide = false;
            for (index = 0; index < entryMap.get(DEFAULT_GROUP).size(); index++) {
                EadEntry tocElem = entryMap.get(DEFAULT_GROUP).get(index);

                if (tocElem.getHierarchy() < hideLevel || tocElem.getHierarchy() < lowestLevelToCollapse) {
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
                    for (int i = index; i < entryMap.get(DEFAULT_GROUP).size(); i++) {
                        EadEntry tempElem = entryMap.get(DEFAULT_GROUP).get(i);
                        if (tempElem.getHierarchy() < tocElem.getHierarchy()) {
                            break;
                        } else if (tempElem.getHierarchy() == tocElem.getHierarchy()) {
                            levelLength++;
                        }
                    }
                    if (levelLength > collapseThreshold) {
                        entryMap.get(DEFAULT_GROUP).get(index - 1).setExpanded(false); //collapse parent
                        hideLevel = tocElem.getHierarchy();
                        hide = true;
                        tocElem.setExpanded(false);
                        tocElem.setVisible(false);
                    }
                }
            }
            //        long end = System.nanoTime();
            //          logger.trace("Time for length collapse: {} ns", (end - start))
        }
    }

    /**
     * Recalculates the visibility of TOC elements after a +/- button has been pressed.
     *
     * @return a {@link io.goobi.viewer.model.toc.TOCElement} object.
     */
    public EadEntry getActiveElement() {
        EadEntry activeTocElement = null;
        if (entryMap != null) {
            if (tocVisible != -1) {
                expandTree(tocVisible);
                activeTocElement = entryMap.get(DEFAULT_GROUP).get(tocVisible);
                activeTocElement.setExpanded(true);
                tocVisible = -1;
            }
            if (tocInvisible != -1) {
                collapseTree(tocInvisible);
                activeTocElement = entryMap.get(DEFAULT_GROUP).get(tocInvisible);
                activeTocElement.setExpanded(false);
                tocInvisible = -1;
            }
        }

        return activeTocElement;
    }

    /**
     * Collapses all elements below the element with the given ID.
     *
     * @param parentId
     */
    private void collapseTree(int parentId) {
        logger.trace("collapseTree: {}", parentId);
        if (entryMap == null) {
            return;
        }

        int level = entryMap.get(DEFAULT_GROUP).get(parentId).getHierarchy();
        for (int i = parentId + 1; i < entryMap.get(DEFAULT_GROUP).size(); i++) {
            EadEntry child = entryMap.get(DEFAULT_GROUP).get(i);
            if (child.getHierarchy() > level) {
                child.setVisible(false);
            } else {
                // Rest of the elements are irrelevant because they belong
                // to a different subtree on the same level
                break;
            }
        }
    }

    /**
     * Recursively expands the child elements of the element with the given ID.
     *
     * @param parentId
     */
    private void expandTree(int parentId) {
        logger.trace("expandTree: {}", parentId);
        if (entryMap == null) {
            return;
        }

        int level = entryMap.get(DEFAULT_GROUP).get(parentId).getHierarchy();
        for (int i = parentId + 1; i < entryMap.get(DEFAULT_GROUP).size(); i++) {
            EadEntry child = entryMap.get(DEFAULT_GROUP).get(i);
            if (child.getHierarchy() == level + 1) {
                // Set immediate children visible
                child.setVisible(true);
                // Elements further down the tree are handled recursively
                if (child.isHasChild() && child.isExpanded()) {
                    expandTree(child.getIndex());
                }
            } else if (child.getHierarchy() <= level) {
                // Rest of the elements are irrelevant because they belong
                // to a different subtree on the same level
                break;
            }
        }
    }

    /**
     * <p>
     * expandAll.
     * </p>
     */
    public void expandAll() {
        logger.trace("expandAll");
        if (entryMap == null) {
            return;
        }

        for (EadEntry tcElem : entryMap.get(DEFAULT_GROUP)) {
            tcElem.setVisible(true);
            if (tcElem.isHasChild()) {
                tcElem.setExpanded(true);
            }
        }
    }

    /**
     * <p>
     * collapseAll.
     * </p>
     */
    public void collapseAll() {
        logger.trace("collapseAll");
        if (entryMap == null) {
            return;
        }

        for (EadEntry tcElem : entryMap.get(DEFAULT_GROUP)) {
            if (tcElem.getHierarchy() == 0) {
                tcElem.setExpanded(false);
            } else {
                tcElem.setVisible(false);
            }
        }
    }

    /**
     * <p>
     * setChildVisible.
     * </p>
     *
     * @param id a int.
     */
    public void setChildVisible(int id) {
        this.tocVisible = id;
    }

    /**
     * <p>
     * setChildInvisible.
     * </p>
     *
     * @param id a int.
     */
    public void setChildInvisible(int id) {
        this.tocInvisible = id;
    }

    /**
     * @return the entryMap
     */
    Map<String, List<EadEntry>> getEntryMap() {
        return entryMap;
    }

    /**
     * @param entryMap the entryMap to set
     */
    void setentryMap(Map<String, List<EadEntry>> entryMap) {
        this.entryMap = entryMap;
    }

    /**
     * <p>
     * getTocElements.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<EadEntry> getTocElements() {
        if (entryMap != null) {
            return entryMap.get(DEFAULT_GROUP);
        }

        return null;
    }

    /**
     * <p>
     * Getter for the field <code>tocVisible</code>.
     * </p>
     *
     * @return the tocVisible
     */
    public int getTocVisible() {
        return tocVisible;
    }

    /**
     * <p>
     * Setter for the field <code>tocVisible</code>.
     * </p>
     *
     * @param tocVisible the tocVisible to set
     */
    public void setTocVisible(int tocVisible) {
        this.tocVisible = tocVisible;
    }

    /**
     * <p>
     * Getter for the field <code>tocInvisible</code>.
     * </p>
     *
     * @return the tocInvisible
     */
    public int getTocInvisible() {
        return tocInvisible;
    }

    /**
     * <p>
     * Setter for the field <code>tocInvisible</code>.
     * </p>
     *
     * @param tocInvisible the tocInvisible to set
     */
    public void setTocInvisible(int tocInvisible) {
        this.tocInvisible = tocInvisible;
    }

    /**
     * <p>
     * Getter for the field <code>maxTocDepth</code>.
     * </p>
     *
     * @return the maxTocDepth
     */
    public int getMaxTocDepth() {
        // If this method is called before getTreeView, the depth will always be 0
        if (!treeBuilt) {
            logger.trace("Building tree to determine the TOC depth");
            getTreeView();
        }
        // logger.trace("getMaxTocDepth: {}", maxTocDepth);
        return maxTocDepth;
    }

    /**
     * <p>
     * Setter for the field <code>maxTocDepth</code>.
     * </p>
     *
     * @param maxTocDepth the maxTocDepth to set
     */
    public void setMaxTocDepth(int maxTocDepth) {
        this.maxTocDepth = maxTocDepth;
    }

    /**
     * <p>
     * Getter for the field <code>totalTocSize</code>.
     * </p>
     *
     * @return the totalTocSize
     */
    public int getTotalTocSize() {
        return totalTocSize;
    }

    /**
     * <p>
     * Setter for the field <code>totalTocSize</code>.
     * </p>
     *
     * @param totalTocSize the totalTocSize to set
     */
    public void setTotalTocSize(int totalTocSize) {
        this.totalTocSize = totalTocSize;
    }

    /**
     * <p>
     * Getter for the field <code>currentPage</code>.
     * </p>
     *
     * @return the currentPage
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * <p>
     * Setter for the field <code>currentPage</code>.
     * </p>
     *
     * @param currentPage the currentPage to set
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
     * @should calculate number correctly
     * @return a int.
     */
    public int getNumPages() {
        int hitsPerPage = DataManager.getInstance().getConfiguration().getTocAnchorGroupElementsPerPage();
        if (hitsPerPage > 0) {
            // logger.trace("numPages: {}/{}={}", totalTocSize, hitsPerPage, totalTocSize / hitsPerPage);
            int num = totalTocSize / hitsPerPage;
            if (totalTocSize % hitsPerPage != 0 || num == 0) {
                num++;
            }
            return num;
        }

        return 1;
    }

    /**
     * <p>
     * hasChildren.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isHasChildren() {
        if (entryMap == null || entryMap.get(DEFAULT_GROUP) == null || entryMap.get(DEFAULT_GROUP).isEmpty()) {
            return false;
        } else if (entryMap.get(DEFAULT_GROUP).size() == 1 && !entryMap.get(DEFAULT_GROUP).get(0).isHasChild()) {
            return false;
        } else {
            return true;
        }
    }
}
