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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;

/**
 * Table of contents and associated functionality for a record. Instances can be either the default archive or a session-local copy.
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
    private Map<String, List<ArchiveEntry>> entryMap = HashMap.newHashMap(1);

    private Map<String, Boolean> entryExpansionMap = new HashMap<>();
    private Map<String, Boolean> entryVisiblilityMap = new HashMap<>();

    /** Actively selected entry */
    private ArchiveEntry selectedEntry;

    private boolean treeBuilt = false;
    private boolean treeFullyLoaded = true;

    private final boolean expandEntryOnSelection;

    /**
     * <p>
     * Constructor for TOC.
     * </p>
     */
    public ArchiveTree() {
        logger.trace("new EADTree()");
        this.expandEntryOnSelection = DataManager.getInstance().getConfiguration().isExpandArchiveEntryOnSelection();
    }

    public ArchiveTree(ArchiveTree orig) {
        this();
        update(orig.trueRootElement);
    }

    /**
     * 
     * @param rootElement
     */
    public void update(ArchiveEntry rootElement) {
        logger.trace("update: {}", rootElement);
        generate(rootElement);
        if (getSelectedEntry() == null) {
            setSelectedEntry(rootElement);
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
        for (ArchiveEntry entry : tree) {
            if (entry.isHasChildren() && !entry.isChildrenLoaded()) {
                checkTreeFullyLoaded(tree);
                break;
            }
        }
        // logger.trace("Generated tree of size {}", tree.size()); //NOSONAR Debug
        entryMap.put(DEFAULT_GROUP, tree);
    }

    /**
     * Checks whether any nodes in given tree have children that are not yet loaded.
     * 
     * @param tree
     * @should set treeFullyLoaded false if tree incomplete
     */
    void checkTreeFullyLoaded(List<ArchiveEntry> tree) {
        for (ArchiveEntry node : tree) {
            if (node.isHasChildren() && !node.isChildrenLoaded()) {
                treeFullyLoaded = false;
                logger.trace("Tree not fully loaded due to lazy loading.");
                break;
            }
        }
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
        logger.trace("getFlatView"); //NOSONAR Debug
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

    /**
     * 
     * @param searchActive
     * @return List<ArchiveEntry>
     */
    public List<ArchiveEntry> getVisibleTree(boolean searchActive) {
        logger.trace("getVisibleTree: {}", trueRootElement.getLabel());
        return getTreeView().stream()
                .filter(e -> isEntryVisible(e) && (e.isDisplaySearch() || !searchActive) && e.isAccessAllowed())
                .toList();
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
            int index = 0;
            for (ArchiveEntry entry : entries) {
                // Current element index
                if (lastLevel < entry.getHierarchyLevel() && index > 0) {
                    if (entry.getHierarchyLevel() > collapseLevel) {
                        entryExpansionMap.put(entry.getParentNode().getId(), false);
                        entryVisiblilityMap.put(entry.getId(), false);
                        // logger.trace("Set node invisible: {} (level {})", entry.getLabel(), entry.getHierarchyLevel()); //NOSONAR Debug
                    } else {
                        entryExpansionMap.put(entry.getParentNode().getId(), true);
                    }
                } else if (entry.getHierarchyLevel() > collapseLevel) {
                    entryVisiblilityMap.put(entry.getId(), false);
                }
                lastLevel = entry.getHierarchyLevel();
                index++;
            }
            treeBuilt = true;
            resetCollapseLevel(getRootElement(), collapseLevel); // TODO Check whether redundant here
            logger.trace("buildTree END");
        }
    }

    /**
     * Recursively expands and sets visible entries at or below maxDepth; hides and collapses any below.
     * 
     * @param entry
     * @param maxDepth
     */
    public void resetCollapseLevel(ArchiveEntry entry, int maxDepth) {
        if (entry == null) {
            return;
        }

        if (entry.getHierarchyLevel() <= maxDepth) {
            entryVisiblilityMap.put(entry.getId(), true);
            entryExpansionMap.put(entry.getId(), entry.getHierarchyLevel() != maxDepth);
        } else {
            entryVisiblilityMap.put(entry.getId(), false);
            entryExpansionMap.put(entry.getId(), false);
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
        // logger.trace("getSelectedEntry"); //NOSONAR Debug
        return selectedEntry;
    }

    /**
     * @param selectedEntry the selectedEntry to set
     */
    public void setSelectedEntry(ArchiveEntry selectedEntry) {
        logger.trace("setSelectedEntry: {}", selectedEntry != null ? selectedEntry.getLabel() : null);

        ArchiveEntry currentEntry = Optional.ofNullable(selectedEntry).orElse(this.selectedEntry);
        if (currentEntry != null && isExpandEntryOnSelection()) {
            if (isEntryExpanded(currentEntry)) {
                collapseEntry(currentEntry);
            } else {
                expandEntry(currentEntry);
            }
        }
        this.selectedEntry = selectedEntry;
    }

    public boolean isExpandEntryOnSelection() {
        return expandEntryOnSelection;
    }

    /**
     * 
     * @param selectedEntry
     */
    public void toggleSelectedEntry(ArchiveEntry selectedEntry) {
        if (selectedEntry != null && selectedEntry.equals(this.selectedEntry)) {
            setSelectedEntry(null);
        } else {
            this.setSelectedEntry(selectedEntry);
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
            entryVisiblilityMap.put(tcElem.getId(), true);
            if (tcElem.isHasChild()) {
                entryExpansionMap.put(tcElem.getId(), true);
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
                entryExpansionMap.put(tcElem.getId(), false);
                entryVisiblilityMap.put(tcElem.getId(), true);
            } else {
                if (collapseAllEntries) {
                    entryExpansionMap.put(tcElem.getId(), false);
                }
                entryVisiblilityMap.put(tcElem.getId(), false);
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
            searchInNode(getRootElement(), searchValue, isTreeFullyLoaded());

            // fill flatList with displayable fields
            flatEntryList = getRootElement().getSearchList();
        } else {
            resetSearch();
        }
    }

    /**
     * Recursively searches for searchValue in the given node and its descendants.
     * 
     * @param node
     * @param searchValue Search terms
     * @param searchInNotLoadedNodes
     */
    void searchInNode(ArchiveEntry node, String searchValue, boolean searchInNotLoadedNodes) {
        if (searchInNotLoadedNodes) {
            // Do a Solr search and load subtrees of found nodes
            if (DataManager.getInstance().getArchiveManager().getEadParser().searchInUnparsedNodes(node, searchValue)) {
                update(node.getRootNode());
                logger.trace("New nodes were loaded during the search, updating tree...");
            }
        }
        if (node.getId() != null && node.getId().equals(searchValue)) {
            // ID match
            node.markAsFound(true);
        } else if (node.getLabel() != null && node.getLabel().toLowerCase().contains(searchValue.toLowerCase())) {
            // mark element + all parents as displayable
            node.markAsFound(true);
        }
        if (node.getSubEntryList() != null) {
            for (ArchiveEntry child : node.getSubEntryList()) {
                searchInNode(child, searchValue, false);
            }
        }
    }

    /**
     * Return this node if it has the given identifier or the first of its descendants with the identifier
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

    public boolean isTreeFullyLoaded() {
        return treeFullyLoaded;
    }

    /**
     * 
     * @param entry
     * @return true if given entry is currently visible; false otherwise
     */
    public boolean isEntryVisible(ArchiveEntry entry) {
        if (entry == null) {
            return false;
        }

        return entryVisiblilityMap.get(entry.getId()) != null && entryVisiblilityMap.get(entry.getId());
    }

    /**
     * 
     * @param entry
     * @return true if given entry is currently expanded; false otherwise
     */
    public boolean isEntryExpanded(ArchiveEntry entry) {
        if (entry == null) {
            return false;
        }

        return entryExpansionMap.get(entry.getId()) != null && entryExpansionMap.get(entry.getId());
    }

    /**
     * Expands this entry and sets all sub-entries visible if their immediate parent is expanded.
     * 
     * @param entry
     */
    public void expandEntry(ArchiveEntry entry) {
        // logger.trace("expand: {}", label); //NOSONAR Debug
        if (!entry.isHasChildren()) {
            return;
        }

        if (!entry.isChildrenLoaded()) {
            logger.trace("Loading children for entry: {}", entry.getLabel());
            try {
                ((SolrEADParser) DataManager.getInstance().getArchiveManager().getEadParser()).loadChildren(entry, null, false);
            } catch (PresentationException | IndexUnreachableException e) {
                logger.error(e.getMessage());
            }
        }

        entryExpansionMap.put(entry.getId(), true);
        setEntryChildrenVisibility(entry, true);
    }

    /**
     * Collapses this entry and hides all sub-entries.
     * 
     * @param entry
     */
    public void collapseEntry(ArchiveEntry entry) {
        // logger.trace("collapse: {}", id); //NOSONAR Debug
        if (!entry.isHasChildren()) {
            return;
        }

        entryExpansionMap.put(entry.getId(), false);
        setEntryChildrenVisibility(entry, false);
    }

    /**
     * @param entry
     * @param visible
     */
    void setEntryChildrenVisibility(ArchiveEntry entry, boolean visible) {
        if (!entry.isHasChildren()) {
            return;
        }

        for (ArchiveEntry sub : entry.getSubEntryList()) {
            entryVisiblilityMap.put(sub.getId(), visible);
            if (isEntryExpanded(sub) && sub.isHasChildren()) {
                setEntryChildrenVisibility(sub, visible);
            }
        }
    }

    /**
     * Expands and sets visible all ancestors of this node and expands siblings of this node.
     * 
     * @param entry
     */
    public void expandUpEntry(ArchiveEntry entry) {
        if (entry.getParentNode() == null) {
            return;
        }

        entryVisiblilityMap.put(entry.getParentNode().getId(), true);
        expandEntry(entry.getParentNode());
        expandUpEntry(entry.getParentNode());
    }
}
