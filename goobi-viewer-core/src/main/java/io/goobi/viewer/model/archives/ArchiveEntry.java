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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;

/**
 * Single archive tree node.
 */
public class ArchiveEntry implements Serializable {

    private static final long serialVersionUID = 8544674817802657532L;

    private static final Logger logger = LogManager.getLogger(ArchiveEntry.class);

    // parent node
    private ArchiveEntry parentNode;
    // list contains all child elements
    // volatile: on-demand loading publishes the complete child list of nodes that may be shared between sessions in one step
    private volatile List<ArchiveEntry> subEntryList = new ArrayList<>();

    // order number of the current element within the current hierarchy
    private Integer orderNumber;
    // hierarchy level
    private Integer hierarchyLevel;
    // c@id
    private String id;
    // display label
    private String label;
    // Main record  PI
    private String topstructPi;
    // Node LOGID
    private String logId;
    // node is search hit
    private boolean searchHit;
    // node type - @level
    private String nodeType;
    // display node in a search result
    private boolean displaySearch;
    // true if the validation of all metadata fields was successful
    private boolean valid = true;

    private String associatedRecordPi;

    private String descriptionLevel;

    private String unitdate;

    private List<String> accessConditions = new ArrayList<>();

    private boolean containsImage = false;

    private SolrDocument doc;

    private boolean childrenFound = false;

    private final AtomicBoolean childrenLoaded = new AtomicBoolean(false);

    /** Guards on-demand child loading. Object[] rather than Object so that the entry stays serializable. */
    private final Object[] childrenLock = new Object[0];

    /**
     * Loads the children of an entry.
     */
    @FunctionalInterface
    public interface ChildrenLoader {
        List<ArchiveEntry> load() throws PresentationException, IndexUnreachableException;
    }

    /**
     *
     * @param order position of this node within its parent's children
     * @param hierarchy depth level of this node in the tree
     * @param doc Solr document representing this archive entry
     */
    public ArchiveEntry(Integer order, Integer hierarchy, SolrDocument doc) {
        this.orderNumber = order;
        this.hierarchyLevel = hierarchy;
        this.doc = doc;
    }

    /**
     *
     * @param orig the entry to copy all fields from
     * @param parent the parent node in the cloned tree
     * @should clone entry correctly
     * @should copy children loaded flag
     */
    public ArchiveEntry(ArchiveEntry orig, ArchiveEntry parent) {
        this.parentNode = parent;

        this.id = orig.id;
        this.associatedRecordPi = orig.associatedRecordPi;
        this.containsImage = orig.containsImage;
        this.valid = orig.valid;
        this.label = orig.label;
        this.nodeType = orig.nodeType;
        this.orderNumber = orig.orderNumber;
        this.hierarchyLevel = orig.hierarchyLevel;
        this.descriptionLevel = orig.descriptionLevel;

        this.topstructPi = orig.topstructPi;
        this.logId = orig.logId;
        for (String accessCondition : orig.getAccessConditions()) {
            this.getAccessConditions().add(accessCondition);
        }

        // Read the loaded flag before the child list: loading publishes the list before setting the flag, so a copy
        // can never be marked as loaded while missing its children
        boolean loaded = orig.isChildrenLoaded();
        this.subEntryList = orig.subEntryList.stream().map(e -> new ArchiveEntry(e, this)).collect(Collectors.toList());

        this.searchHit = orig.searchHit;
        this.displaySearch = orig.displaySearch;
        this.doc = orig.doc;
        this.childrenFound = orig.childrenFound;
        this.childrenLoaded.set(loaded);
    }

    public void addSubEntry(ArchiveEntry other) {
        subEntryList.add(other);
        other.setParentNode(this);
    }

    public void removeSubEntry(ArchiveEntry other) {
        subEntryList.remove(other);
        reOrderElements();
    }

    public void reOrderElements() {
        int order = 0;
        for (ArchiveEntry entry : subEntryList) {
            entry.setOrderNumber(order++);
        }
    }

    /**
     *
     * @param ignoreDisplayChildren if true, recurse into children regardless of their display state
     * @return List<ArchiveEntry> LinkedList containing all nodes
     */
    public List<ArchiveEntry> getAsFlatList(boolean ignoreDisplayChildren) {
        // logger.trace("getAsFlatList"); //NOSONAR Debug
        List<ArchiveEntry> list = new LinkedList<>(); // LinkedList more efficient to create here due to the recursion
        list.add(this);
        if (ignoreDisplayChildren && subEntryList != null && !subEntryList.isEmpty()) {
            for (ArchiveEntry ds : subEntryList) {
                list.addAll(ds.getAsFlatList(ignoreDisplayChildren));
                // logger.trace("ID: {}, level: {}, label: {}", ds.getId(), ds.getHierarchyLevel(), ds.getLabel()); //NOSONAR Debug
            }
        }
        return list;
    }

    public boolean isHasChildren() {
        return isChildrenFound() || !subEntryList.isEmpty();
    }

    public void updateHierarchy() {
        // root node
        if (parentNode == null) {
            hierarchyLevel = 0;
        } else {
            hierarchyLevel = parentNode.getHierarchyLevel() + 1;
        }

        for (ArchiveEntry child : subEntryList) {
            child.updateHierarchy();
        }
    }

    public void markAsFound(boolean keepChildrenVisible) {
        displaySearch = true;
        searchHit = true;

        if (parentNode != null) {
            ArchiveEntry node = parentNode;
            while (!node.isDisplaySearch()) {
                node.setDisplaySearch(true);
                if (node.parentNode != null) {
                    node = node.parentNode;
                }
            }
        }
        if (keepChildrenVisible) {
            for (ArchiveEntry child : this.subEntryList) {
                child.setDisplaySearch(true, true);
            }
        }
    }

    public void resetFoundList() {
        // logger.trace("resetFoundList: {}", id); //NOSONAR Debug
        displaySearch = false;
        searchHit = false;
        if (subEntryList != null) {
            for (ArchiveEntry ds : subEntryList) {
                ds.resetFoundList();
            }
        }
    }

    public List<ArchiveEntry> getSearchList() {
        List<ArchiveEntry> list = new LinkedList<>();
        if (displaySearch) {
            list.add(this);
            if (subEntryList != null) {
                for (ArchiveEntry child : subEntryList) {
                    list.addAll(child.getSearchList());
                }
            }
        }
        return list;
    }

    /**
     *
     * @param offset value added to the hierarchy level of this node and all descendants
     */
    public void shiftHierarchy(int offset) {
        this.hierarchyLevel += offset;
        if (isHasChildren()) {
            for (ArchiveEntry sub : subEntryList) {
                sub.shiftHierarchy(offset);
            }
        }
    }

    /**
     * 
     * @return Root node
     */
    public ArchiveEntry getRootNode() {
        ArchiveEntry ret = this;
        while (ret.getParentNode() != null) {
            ret = ret.getParentNode();
        }

        // logger.trace("found root: {}", ret); //NOSONAR Debug
        return ret;
    }

    
    public ArchiveEntry getParentNode() {
        return parentNode;
    }

    
    public void setParentNode(ArchiveEntry parentNode) {
        this.parentNode = parentNode;
    }

    
    public List<ArchiveEntry> getSubEntryList() {
        return subEntryList;
    }

    
    public void setSubEntryList(List<ArchiveEntry> subEntryList) {
        this.subEntryList = subEntryList;
    }

    
    public Integer getOrderNumber() {
        return orderNumber;
    }

    
    public void setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
    }

    
    public Integer getHierarchyLevel() {
        return hierarchyLevel;
    }

    
    public void setHierarchyLevel(Integer hierarchyLevel) {
        this.hierarchyLevel = hierarchyLevel;
    }

    
    public String getId() {
        // logger.trace("getId: {}", id); //NOSONAR Debug
        return id;
    }

    
    public void setId(String id) {
        this.id = id;
    }

    
    public String getLabel() {
        return label;
    }

    
    public void setLabel(String label) {
        this.label = label;
    }

    
    public String getTopstructPi() {
        return topstructPi;
    }

    
    public void setTopstructPi(String topstructPi) {
        this.topstructPi = topstructPi;
    }

    
    public String getLogId() {
        return logId;
    }

    
    public void setLogId(String logId) {
        this.logId = logId;
    }

    
    public boolean isSearchHit() {
        return searchHit;
    }

    
    public void setSearchHit(boolean searchHit) {
        this.searchHit = searchHit;
    }

    
    public String getNodeType() {
        return nodeType;
    }

    
    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    
    public boolean isDisplaySearch() {
        return displaySearch;
    }

    
    public void setDisplaySearch(boolean displaySearch) {
        this.setDisplaySearch(displaySearch, false);
    }

    /**
     *
     * @param displaySearch the display-in-search-results flag to set
     * @param recursive if true, apply the flag to all descendant nodes as well
     */
    public void setDisplaySearch(boolean displaySearch, boolean recursive) {
        this.displaySearch = displaySearch;
        if (recursive) {
            for (ArchiveEntry child : this.subEntryList) {
                child.setDisplaySearch(displaySearch, recursive);
            }
        }
    }

    /**
     * Checks whether access to the given node is allowed due to set access conditions.
     * 
     * @return true if access granted; false otherwise
     * @should return true if access conditions empty
     * @should return false when entry has a restricted access condition
     */
    public boolean isAccessAllowed() {
        if (getAccessConditions().isEmpty()) {
            // OPENACCESS values are omitted
            return true;
        }

        try {
            boolean ret = AccessConditionUtils
                    .checkAccessPermissionByIdentifierAndLogId(topstructPi, logId, IPrivilegeHolder.PRIV_ARCHIVE_DISPLAY_NODE, BeanUtils.getRequest())
                    .isGranted();
            if (!ret) {
                logger.trace("Access denied to {}", label);
            }
            return ret;
        } catch (IndexUnreachableException | DAOException e) {
            logger.error(e.getMessage(), e);
            return false;
        } catch (RecordNotFoundException e) {
            logger.warn("Archive not found in index: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks whether access to the given node is allowed due to set access conditions.
     * 
     * @return true if access granted; false otherwise
     */
    public boolean isImageAccessAllowed() {
        // Return true for potential parents of nodes with images
        if (!isContainsImage() || StringUtils.isEmpty(getAssociatedRecordPi())) {
            return true;
        }

        try {
            boolean ret = AccessConditionUtils
                    .checkAccessPermissionByIdentifierAndLogId(getAssociatedRecordPi(), null, IPrivilegeHolder.PRIV_VIEW_THUMBNAILS,
                            BeanUtils.getRequest())
                    .isGranted();
            if (!ret) {
                logger.trace("Image access denied to {}", label);
            }
            return ret;
        } catch (IndexUnreachableException | DAOException | RecordNotFoundException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    
    public boolean isValid() {
        return valid;
    }

    
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    
    public String getDescriptionLevel() {
        return descriptionLevel;
    }

    
    public void setDescriptionLevel(String descriptionLevel) {
        this.descriptionLevel = descriptionLevel;
    }

    
    public String getUnitdate() {
        return unitdate;
    }

    public void setUnitdate(String unitdate) {
        this.unitdate = unitdate;
    }

    
    public List<String> getAccessConditions() {
        return accessConditions;
    }

    
    public void setAccessConditions(List<String> accessConditions) {
        this.accessConditions = accessConditions;
    }

    
    public boolean isHasChild() {
        return isHasChildren();
    }

    
    public String getAssociatedRecordPi() {
        return associatedRecordPi;
    }

    
    public void setAssociatedRecordPi(String associatedRecordPi) {
        this.associatedRecordPi = associatedRecordPi;
    }

    /**
     * Get the parent node hierarchy of this node, optionally including the node itself The list is sorted with hightest hierarchy level first, so the
     * node itself will always be the last element, if included.
     *
     * @param includeSelf if true, this node is appended at the end of the list
     * @return List<ArchiveEntry>
     */
    public List<ArchiveEntry> getAncestors(boolean includeSelf) {
        List<ArchiveEntry> ancestors = new ArrayList<>();
        if (includeSelf) {
            ancestors.add(this);
        }
        ArchiveEntry parent = this.parentNode;
        while (parent != null) {
            ancestors.add(parent);
            parent = parent.parentNode;
        }
        Collections.reverse(ancestors);
        return ancestors;
    }

    public boolean isContainsImage() {
        return this.containsImage;
    }

    public void setContainsImage(boolean containsImage) {
        this.containsImage = containsImage;
    }

    
    public SolrDocument getDoc() {
        return doc;
    }

    
    public boolean isChildrenFound() {
        return childrenFound;
    }

    
    public void setChildrenFound(boolean childrenFound) {
        this.childrenFound = childrenFound;
    }

    
    public boolean isChildrenLoaded() {
        return childrenLoaded.get();
    }

    
    public void setChildrenLoaded(boolean childrenLoaded) {
        this.childrenLoaded.set(childrenLoaded);
    }

    /**
     * Loads the children of this entry with the given loader, unless they are already loaded. Nodes may be shared between the global
     * archive tree and session copies, so loading is serialized per entry and the child list is published in one step.
     *
     * @param loader supplies the child entries
     * @return true if children were loaded by this call; false if they were already loaded or the loader found none
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should load children only once
     * @should not mark entry as loaded if loader returns no children
     */
    public boolean loadChildrenOnce(ChildrenLoader loader) throws PresentationException, IndexUnreachableException {
        synchronized (childrenLock) {
            if (isChildrenLoaded()) {
                return false;
            }
            List<ArchiveEntry> children = loader.load();
            if (children.isEmpty()) {
                return false;
            }
            boolean image = containsImage;
            for (ArchiveEntry child : children) {
                child.setParentNode(this);
                if (child.isContainsImage()) {
                    image = true;
                }
            }
            setSubEntryList(children);
            setContainsImage(image);
            childrenLoaded.set(true);
            return true;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ArchiveEntry other = (ArchiveEntry) obj;
        if (hierarchyLevel == null) {
            if (other.hierarchyLevel != null) {
                return false;
            }
        } else if (!hierarchyLevel.equals(other.hierarchyLevel)) {
            return false;
        }
        if (orderNumber == null) {
            if (other.orderNumber != null) {
                return false;
            }
        } else if (!orderNumber.equals(other.orderNumber)) {
            return false;
        }
        if (label != null && !label.equals(other.getLabel())) {
            return false;
        }
        if (parentNode == null && other.parentNode == null) {
            return true;
        }
        if (parentNode == null && other.parentNode != null) {
            return false;
        }
        if (parentNode != null && other.parentNode == null) {
            return false;
        }
        if (parentNode != null && other.parentNode != null) {
            if (!parentNode.getOrderNumber().equals(other.parentNode.getOrderNumber())) {
                return false;
            }
            if (!parentNode.getHierarchyLevel().equals(other.parentNode.getHierarchyLevel())) {
                return false;
            }
            if (!parentNode.equals(other.parentNode)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((hierarchyLevel == null) ? 0 : hierarchyLevel.hashCode());
        result = prime * result + ((orderNumber == null) ? 0 : orderNumber.hashCode());
        result = prime * result + ((parentNode == null) ? 0 : parentNode.getHierarchyLevel().hashCode());
        result = prime * result + ((parentNode == null) ? 0 : parentNode.getOrderNumber().hashCode());
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return id;
    }

}