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

    public static int defaultCollapseLevel = 2;

    /** TOC element map. */
    private Map<String, List<EadEntry>> entryMap = new HashMap<>(1);

    private EadEntry selectedEntry;

    private boolean treeBuilt = false;

    private int toExpandIndex = -1;

    private int toCollapseIndex = -1;

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

        List<EadEntry> tree = root.getAsFlatList(true);
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
            buildTree(group, defaultCollapseLevel);
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
     * @param collapseLevel
     */
    private void buildTree(String group, int collapseLevel) {
        logger.trace("buildTree");
        if (group == null) {
            throw new IllegalArgumentException("group may not be null");
        }

        synchronized (this) {
            if (entryMap == null) {
                return;
            }
            int lastLevel = 0;
            int lastParent = 0;
            for (EadEntry entry : entryMap.get(group)) {
                // Current element index
                int index = entryMap.get(group).indexOf(entry);
                entry.setIndex(index);
                if (entry.getHierarchy() > maxTocDepth) {
                    maxTocDepth = entry.getHierarchy();
                }

                if (lastLevel < entry.getHierarchy() && index > 0) {
                    entry.setParentIndex(lastParent);
                    //                    entryMap.get(group).get(index - 1).setHasChild(true);
                    if (entry.getHierarchy() > collapseLevel) {
                        entryMap.get(group).get(index - 1).setExpanded(false);
                        entry.setVisible(false);
                    } else {
                        entryMap.get(group).get(index - 1).setExpanded(true);
                    }

                    for (int i = index + 1; i < entryMap.get(group).size(); i++) {
                        EadEntry tc = entryMap.get(group).get(i);
                        if (tc.getHierarchy() == entry.getHierarchy()) {
                            // Elements on the same level as the current element get the same parent ID and are set visible
                            tc.setParentIndex(entry.getParentIndex());
                        }
                        if (tc.getHierarchy() > collapseLevel) {
                            tc.setVisible(false);
                        }
                    }

                }
                lastParent = index;
                lastLevel = entry.getHierarchy();
            }
            treeBuilt = true;
            resetCollapseLevel(getRootElement(), collapseLevel);
        }
    }

    /**
     * 
     * @param entry
     * @param maxDepth
     */
    public void resetCollapseLevel(EadEntry entry, int maxDepth) {
        if (entry == null) {
            return;
        }

        if (entry.getHierarchy() <= maxDepth) {
            entry.setVisible(true);
            entry.setExpanded(entry.getHierarchy() != maxDepth);
        } else {
            entry.setVisible(false);
            entry.setExpanded(false);
        }

        if (entry.getSubEntryList() != null && !entry.getSubEntryList().isEmpty()) {
            for (EadEntry child : entry.getSubEntryList()) {
                resetCollapseLevel(child, maxDepth);
            }
        }
    }

    /**
     * Recalculates the visibility of TOC elements after a +/- button has been pressed.
     *
     * @return a {@link io.goobi.viewer.model.toc.TOCElement} object.
     */
    public EadEntry updateTree() {
        EadEntry ret = null;
        if (entryMap == null) {
            return ret;
        }

        if (toExpandIndex != -1) {
            expandSubtree(toExpandIndex);
            ret = entryMap.get(DEFAULT_GROUP).get(toExpandIndex);
            ret.setExpanded(true);
            toExpandIndex = -1;
        }
        if (toCollapseIndex != -1) {
            collapseSubtree(toCollapseIndex);
            ret = entryMap.get(DEFAULT_GROUP).get(toCollapseIndex);
            ret.setExpanded(false);
            toCollapseIndex = -1;
        }

        return ret;
    }

    /**
     * @return the selectedEntry
     */
    public EadEntry getSelectedEntry() {
        return selectedEntry;
    }

    /**
     * @param selectedEntry the selectedEntry to set
     */
    public void setSelectedEntry(EadEntry selectedEntry) {
        logger.trace("setSelectedEntry: {}", selectedEntry != null ? selectedEntry.getId() : null);
        this.selectedEntry = selectedEntry;
    }

    /**
     * 
     * @return
     */
    public EadEntry getRootElement() {
        return getRootElement(DEFAULT_GROUP);
    }

    /**
     * 
     * @param group
     * @return
     */
    public EadEntry getRootElement(String group) {
        if (group == null || entryMap == null || entryMap.isEmpty()) {
            return null;
        }

        return entryMap.get(group).get(0);
    }

    /**
     * Collapses all elements below the element with the given ID.
     *
     * @param parentIndex
     */
    private void collapseSubtree(int parentIndex) {
        logger.trace("collapseSubtree: {}", parentIndex);
        if (entryMap == null) {
            return;
        }

        int level = entryMap.get(DEFAULT_GROUP).get(parentIndex).getHierarchy();
        for (int i = parentIndex + 1; i < entryMap.get(DEFAULT_GROUP).size(); i++) {
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
    private void expandSubtree(int parentId) {
        // logger.trace("expandSubtree: {}", parentId);
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
                    expandSubtree(child.getIndex());
                }
            } else if (child.getHierarchy() <= level) {
                // Rest of the elements are irrelevant because they belong
                // to a different subtree on the same level
                break;
            }
        }
        // logger.trace("expandTree END");
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
        if (entryMap == null) {
            return;
        }

        collapseAll(false);
    }

    /**
     * 
     * @param collapseAllEntries If true, all invisible child children will also be collapsed
     */
    public void collapseAll(boolean collapseAllEntries) {
        logger.trace("collapseAll");
        if (entryMap == null) {
            return;
        }

        for (EadEntry tcElem : entryMap.get(DEFAULT_GROUP)) {
            if (tcElem.getHierarchy() == 0) {
                tcElem.setExpanded(false);
            } else {
                if (collapseAllEntries) {
                    tcElem.setExpanded(false);
                }
                tcElem.setVisible(false);
            }
        }
    }

    /**
     * <p>
     * setToExpandIndex.
     * </p>
     *
     * @param id a int.
     */
    public void setToExpandIndex(int toExpandIndex) {
        this.toExpandIndex = toExpandIndex;
    }

    /**
     * <p>
     * setToCollapseIndexs.
     * </p>
     *
     * @param id a int.
     */
    public void setToCollapseIndex(int toCollapseIndex) {
        this.toCollapseIndex = toCollapseIndex;
    }

    /**
     * @return the entryMap
     */
    Map<String, List<EadEntry>> getEntryMap() {
        return entryMap;
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
