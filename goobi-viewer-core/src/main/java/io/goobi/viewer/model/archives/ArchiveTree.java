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
package io.goobi.viewer.model.archives;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Table of contents and associated functionality for a record.
 */
public class ArchiveTree implements Serializable {

    private static final long serialVersionUID = 1798213211987072214L;

    private static final Logger logger = LogManager.getLogger(ArchiveTree.class);

    /** Constant <code>DEFAULT_GROUP="_DEFAULT"</code> */
    public static final String DEFAULT_GROUP = "_DEFAULT";

    public static final int DEFAULT_COLLAPSE_LEVEL = 1;

    /** Actual root of the document (even if it's not part of the displayed tree) */
    private ArchiveEntry trueRootElement = null;

    /** Currently displayed tree. Can be partial after a search, etc. */
    private List<ArchiveEntry> flatEntryList;

    /** TOC element map. */
    private Map<String, List<ArchiveEntry>> entryMap = new HashMap<>(1);

    /** Actively selected entry */
    private ArchiveEntry selectedEntry;

    private boolean treeBuilt = false;

    /**
     * <p>
     * Constructor for TOC.
     * </p>
     */
    public ArchiveTree() {
        logger.trace("new EADTree()");
    }

    /**
     * Cloning constructor.
     * 
     * @param orig
     */
    public ArchiveTree(ArchiveTree orig) {
        this.generate(new ArchiveEntry(orig.getRootElement(), null));
        this.getTreeViewForGroup(DEFAULT_GROUP);
    }

    /**
     * 
     * @param rootElement
     */
    public void update(ArchiveEntry rootElement) {
        logger.trace("update: {}", rootElement);
        generate(rootElement);
        if (getSelectedEntry() == null) {
            setSelectedEntry(getRootElement());
        }
        // This should happen before the tree is expanded to the selected entry, otherwise the collapse level will be reset
        getTreeView();
    }

    /**
     * Sets the given root entry, generates a new flat list and adds it to entryMap.
     * 
     * @param root The root entry to set
     */
    public void generate(ArchiveEntry root) {
        logger.trace("generate: {}", root);
        if (root == null) {
            throw new IllegalArgumentException("root may not be null");
        }

        setTrueRootElement(root);

        List<ArchiveEntry> tree = root.getAsFlatList(true);
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
    public List<ArchiveEntry> getViewForGroup(String group) {
        if (entryMap != null) {
            logger.trace("Tree size: {}", entryMap.get(group).size());
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
    public List<ArchiveEntry> getTreeViewForGroup(String group) {
        logger.trace("getTreeViewForGroup: {}", group);
        if (!treeBuilt) {
            buildTree(group, DEFAULT_COLLAPSE_LEVEL);
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
    public List<ArchiveEntry> getFlatView() {
        logger.trace("getFlatView"); //NOSONAR Sometimes needed for debugging
        return getViewForGroup(DEFAULT_GROUP);
    }

    /**
     * <p>
     * getTreeView.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<ArchiveEntry> getTreeView() {
        logger.trace("getTreeView");
        return getTreeViewForGroup(DEFAULT_GROUP);
    }

    public List<ArchiveEntry> getFilteredTreeView(boolean searchActive) {
        logger.debug("getFilteredTreeView");
        List<ArchiveEntry> ret = new ArrayList<>();

        for (ArchiveEntry entry : getTreeView()) {
            if (entry.isVisible() && (!searchActive || entry.isDisplaySearch())) {
                ret.add(entry);
            }
        }

        logger.debug("getFilteredTreeView END");
        return ret;
    }

    /**
     * 
     * @param searchActive
     * @return List<ArchiveEntry>
     */
    public List<ArchiveEntry> getVisibleTree(boolean searchActive) {
        logger.trace("getVisibleTree");
        return getTreeView().stream().filter(e -> e.isVisible()).filter(e -> e.isDisplaySearch() || !searchActive).toList();

        //        List<ArchiveEntry> ret = new ArrayList<>();
        //        for (ArchiveEntry entry : getTreeView()) {
        //            if (entry.isVisible() && (!searchActive || entry.isDisplaySearch())) {
        //                ret.add(entry);
        //            }
        //        }
        //
        //        return ret;
    }

    /**
     *
     * @param group
     * @param collapseLevel
     */
    private void buildTree(String group, int collapseLevel) {
        logger.trace("buildTree: {} - {}", group, collapseLevel);
        if (group == null) {
            throw new IllegalArgumentException("group may not be null");
        }

        synchronized (this) {
            if (entryMap == null) {
                return;
            }
            int lastLevel = 0;
            List<ArchiveEntry> entries = entryMap.get(group);
            for (int index = 0; index < entries.size(); index++) {
                // Current element index
                ArchiveEntry entry = entries.get(index);
                if (lastLevel < entry.getHierarchyLevel() && index > 0) {
                    if (entry.getHierarchyLevel() > collapseLevel) {
                        entries.get(index - 1).setExpanded(false);
                        entry.setVisible(false);
                        // logger.trace("Set node invisible: {} (level {})", entry.getLabel(), entry.getHierarchyLevel()); //NOSONAR Debug
                    } else {
                        entries.get(index - 1).setExpanded(true);
                    }
                } else if (entry.getHierarchyLevel() > collapseLevel) {
                    entry.setVisible(false);
                }
                lastLevel = entry.getHierarchyLevel();
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
    public void resetCollapseLevel(ArchiveEntry entry, int maxDepth) {
        if (entry == null) {
            return;
        }

        if (entry.getHierarchyLevel() <= maxDepth) {
            entry.setVisible(true);
            entry.setExpanded(entry.getHierarchyLevel() != maxDepth);
        } else {
            entry.setVisible(false);
            entry.setExpanded(false);
        }

        if (entry.getSubEntryList() != null && !entry.getSubEntryList().isEmpty()) {
            for (ArchiveEntry child : entry.getSubEntryList()) {
                resetCollapseLevel(child, maxDepth);
            }
        }
    }

    /**
     * @return the selectedEntry
     */
    public ArchiveEntry getSelectedEntry() {
        // logger.trace("getSelectedEntry"); //NOSONAR Sometimes needed for debugging
        return selectedEntry;
    }

    /**
     * @param selectedEntry the selectedEntry to set
     */
    public void setSelectedEntry(ArchiveEntry selectedEntry) {
        logger.trace("setSelectedEntry: {}", selectedEntry != null ? selectedEntry.getLabel() : null);
        this.selectedEntry = selectedEntry;
        if (selectedEntry != null && !selectedEntry.isMetadataLoaded()) {
            selectedEntry.loadMetadata();
        }
    }

    /**
     * 
     * @param selectedEntry
     */
    public void toggleSelectedEntry(ArchiveEntry selectedEntry) {
        if (selectedEntry != null && selectedEntry.equals(this.selectedEntry)) {
            this.selectedEntry = null;
        } else {
            this.selectedEntry = selectedEntry;
            if (selectedEntry != null && !selectedEntry.isMetadataLoaded()) {
                selectedEntry.loadMetadata();
            }
        }
    }

    /**
     * @return the trueRootElement
     */
    public ArchiveEntry getTrueRootElement() {
        return trueRootElement;
    }

    /**
     * @param trueRootElement the trueRootElement to set
     */
    public void setTrueRootElement(ArchiveEntry trueRootElement) {
        this.trueRootElement = trueRootElement;
    }

    /**
     *
     * @return Root element for the default group
     */
    public ArchiveEntry getRootElement() {
        return getRootElement(DEFAULT_GROUP);
    }

    /**
     *
     * @param group
     * @return Root element for the given group
     */
    public ArchiveEntry getRootElement(String group) {
        if (group == null || entryMap == null || entryMap.isEmpty()) {
            return null;
        }

        return entryMap.get(group).get(0);
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

        for (ArchiveEntry tcElem : entryMap.get(DEFAULT_GROUP)) {
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

        for (ArchiveEntry tcElem : entryMap.get(DEFAULT_GROUP)) {
            if (tcElem.getHierarchyLevel() == 0) {
                tcElem.setExpanded(false);
                tcElem.setVisible(true);
            } else {
                if (collapseAllEntries) {
                    tcElem.setExpanded(false);
                }
                tcElem.setVisible(false);
            }
        }
    }

    /**
     * @return the entryMap
     */
    Map<String, List<ArchiveEntry>> getEntryMap() {
        return entryMap;
    }

    /**
     * <p>
     * getTocElements.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<ArchiveEntry> getTocElements() {
        if (entryMap != null) {
            return entryMap.get(DEFAULT_GROUP);
        }

        return null;
    }

    /**
     * Get the hierarchical tree as a flat list
     *
     * @return List<ArchiveEntry>
     */

    public List<ArchiveEntry> getFlatEntryList() {
        if (flatEntryList == null && trueRootElement != null) {
            flatEntryList = new LinkedList<>();
            flatEntryList.addAll(trueRootElement.getAsFlatList(false));
        }

        return flatEntryList;
    }

    /**
     *
     * @return the {@link ArchiveEntry} with the given identifier if it exists in the tree; null otherwise
     * @param identifier
     */
    public ArchiveEntry getEntryById(String identifier) {
        return findEntry(identifier, getRootElement()).orElse(null);
    }

    /**
     *
     * @param searchValue
     */
    public void search(String searchValue) {
        if (getRootElement() == null) {
            logger.error("Database not loaded");
            return;
        }

        if (StringUtils.isNotBlank(searchValue)) {
            // hide all elements
            getRootElement().resetFoundList();
            // search in all/some metadata fields of all elements?

            // for now: search only labels
            searchInNode(getRootElement(), searchValue);

            // fill flatList with displayable fields
            flatEntryList = getRootElement().getSearchList();
        } else {
            resetSearch();
        }
    }

    /**
     *
     * @param node
     * @param searchValue
     */
    static void searchInNode(ArchiveEntry node, String searchValue) {
        if (node.getId() != null && node.getId().equals(searchValue)) {
            // ID match
            node.markAsFound(true);
        } else if (node.getLabel() != null && node.getLabel().toLowerCase().contains(searchValue.toLowerCase())) {
            // mark element + all parents as displayable
            node.markAsFound(true);
        }
        if (node.getSubEntryList() != null) {
            for (ArchiveEntry child : node.getSubEntryList()) {
                searchInNode(child, searchValue);
            }
        }
    }

    /**
     * Return this node if it has the given identifier or the first of its descendents with the identifier
     *
     * @param identifier
     * @param node
     * @return Optional<ArchiveEntry>
     */
    private Optional<ArchiveEntry> findEntry(String identifier, ArchiveEntry node) {
        if (StringUtils.isNotBlank(identifier)) {
            if (identifier.equals(node.getId())) {
                return Optional.of(node);
            }
            if (node.getSubEntryList() != null) {
                for (ArchiveEntry child : node.getSubEntryList()) {
                    Optional<ArchiveEntry> find = findEntry(identifier, child);
                    if (find.isPresent()) {
                        return find;
                    }
                }
            }
        }

        return Optional.empty();
    }

    public void resetSearch() {
        trueRootElement.resetFoundList();
        flatEntryList = null;
    }
}
