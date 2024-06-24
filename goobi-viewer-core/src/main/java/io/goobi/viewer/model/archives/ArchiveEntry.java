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
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * Single archive tree node.
 */
public class ArchiveEntry implements Serializable {

    private static final long serialVersionUID = 8544674817802657532L;

    private static final Logger logger = LogManager.getLogger(ArchiveEntry.class);

    // parent node
    private ArchiveEntry parentNode;
    // list contains all child elements
    private List<ArchiveEntry> subEntryList = new ArrayList<>();
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
    // node is open/closed
    private boolean displayChildren;
    // node is search hit
    private boolean searchHit;
    // node type -  @level
    private String nodeType;
    // display node in a search result
    private boolean displaySearch;
    // true if the validation of all metadata fields was successful
    private boolean valid = true;

    private String associatedRecordPi;

    private String descriptionLevel;

    private String unitdate;

    private boolean visible = true;

    private boolean expanded = false;

    private boolean containsImage = false;

    private SolrDocument doc;

    private boolean metadataLoaded = false;

    private boolean childrenFound = false;

    private boolean childrenLoaded = false;

    /* 1. metadata for Identity Statement Area */
    //    Reference code(s)
    //    Title
    //    private String unittitle; // did/unittitle
    //    Date(s)
    //    Level of description
    //    Extent and medium of the unit of description (quantity, bulk, or size)
    private List<Metadata> identityStatementAreaList = new ArrayList<>();

    /* 2. Context Area */
    //    Name of creator(s)
    //    Administrative | Biographical history
    //    Archival history
    //    Immediate source of acquisition or transfer
    private List<Metadata> contextAreaList = new ArrayList<>();

    /* 3. Content and Structure Area */
    //    Scope and content
    //    Appraisal, destruction and scheduling information
    //    Accruals
    //    System of arrangement
    private List<Metadata> contentAndStructureAreaAreaList = new ArrayList<>();

    /* 4. Condition of Access and Use Area */
    //    Conditions governing access
    //    Conditions governing reproduction
    //    Language | Scripts of material
    //    Physical characteristics and technical requirements
    //    Finding aids
    private List<Metadata> accessAndUseAreaList = new ArrayList<>();

    /* 5. Allied Materials Area */
    //    Existence and location of originals
    //    Existence and location of copies
    //    Related units of description
    //    Publication note
    private List<Metadata> alliedMaterialsAreaList = new ArrayList<>();

    /* 6. Note Area */
    //    Note
    private List<Metadata> notesAreaList = new ArrayList<>();

    /* 7. Description Control Area */
    //    Archivist's Note
    //    Rules or Conventions
    //    Date(s) of descriptions
    private List<Metadata> descriptionControlAreaList = new ArrayList<>();

    /**
     * 
     * @param order
     * @param hierarchy
     * @param doc
     */
    public ArchiveEntry(Integer order, Integer hierarchy, SolrDocument doc) {
        this.orderNumber = order;
        this.hierarchyLevel = hierarchy;
        this.doc = doc;
    }

    /**
     * 
     * @param orig
     * @param parent
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

        this.subEntryList = orig.subEntryList.stream().map(e -> new ArchiveEntry(e, this)).collect(Collectors.toList());
        this.accessAndUseAreaList = orig.accessAndUseAreaList; //flat copy, because effectively final
        this.alliedMaterialsAreaList = orig.alliedMaterialsAreaList; //flat copy, because effectively final
        this.contentAndStructureAreaAreaList = orig.contentAndStructureAreaAreaList; //flat copy, because effectively final
        this.contextAreaList = orig.contextAreaList; //flat copy, because effectively final
        this.descriptionControlAreaList = orig.descriptionControlAreaList; //flat copy, because effectively final
        this.identityStatementAreaList = orig.identityStatementAreaList; //flat copy, because effectively final
        this.notesAreaList = orig.notesAreaList; //flat copy, because effectively final

        this.visible = orig.visible;
        this.expanded = orig.expanded;
        this.searchHit = orig.searchHit;
        this.displayChildren = orig.displayChildren;
        this.displaySearch = orig.displaySearch;
        this.doc = orig.doc;
        this.childrenFound = orig.childrenFound;
        this.childrenLoaded = orig.childrenLoaded;
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
     * @param ignoreDisplayChildren
     * @return List<ArchiveEntry>
     */
    public List<ArchiveEntry> getAsFlatList(boolean ignoreDisplayChildren) {
        // logger.trace("getAsFlatList");
        List<ArchiveEntry> list = new LinkedList<>();
        list.add(this);
        if ((displayChildren || ignoreDisplayChildren) && subEntryList != null && !subEntryList.isEmpty()) {
            for (ArchiveEntry ds : subEntryList) {
                list.addAll(ds.getAsFlatList(ignoreDisplayChildren));
                // logger.trace("ID: {}, level {}", ds.getId(), ds.getHierarchyLevel()); //NOSONAR Sometimes needed for debugging
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
        // logger.trace("resetFoundList: {}", id); //NOSONAR Sometimes needed for debugging
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
     * @param offset
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
     * Expands and sets visible all ancestors of this node and expands siblings of this node.
     */
    public void expandUp() {
        if (parentNode == null) {
            return;
        }

        parentNode.setVisible(true);
        parentNode.expand();
        parentNode.expandUp();
    }

    /**
     * Expands this entry and sets all sub-entries visible if their immediate parent is expanded.
     */
    public void expand() {
        // logger.trace("expand: {}", label); //NOSONAR Sometimes needed for debugging
        if (!isHasChildren()) {
            return;
        }

        if (!isChildrenLoaded()) {
            logger.trace("Loading children for entry: {}", label);
            try {
                ((SolrEADParser) DataManager.getInstance().getArchiveManager().getEadParser()).loadChildren(this, null, false);
            } catch (PresentationException | IndexUnreachableException e) {
                logger.error(e.getMessage());
            }
        }

        setExpanded(true);
        setChildrenVisibility(true);
    }

    /**
     * Collapses this entry and hides all sub-entries.
     */
    public void collapse() {
        // logger.trace("collapse: {}", id); //NOSONAR Sometimes needed for debugging
        if (!isHasChildren()) {
            return;
        }

        setExpanded(false);
        setChildrenVisibility(false);
    }

    /**
     *
     * @param visible
     */
    void setChildrenVisibility(boolean visible) {
        if (!isHasChildren()) {
            return;
        }

        for (ArchiveEntry sub : subEntryList) {
            sub.setVisible(visible);
            if (sub.isExpanded() && sub.isHasChildren()) {
                sub.setChildrenVisibility(visible);
            }
        }
    }

    public void loadMetadata() {
        logger.trace("loadMetadata ({})", label);
        try {
            List<Metadata> metadataList = DataManager.getInstance().getConfiguration().getArchiveMetadataForTemplate("");
            // Collect metadata
            if (doc != null && metadataList != null && !metadataList.isEmpty()) {
                StructElement se = new StructElement(doc);
                for (Metadata md : metadataList) {
                    if (md.populate(se, null, null, null)) {
                        addMetadataField(md);
                    }
                }
            }
        } catch (IndexUnreachableException | PresentationException e) {
            logger.error(e.getMessage());
        } finally {
            metadataLoaded = true;
        }
    }

    /**
     * Add the metadata to the configured level.
     *
     * @param entry
     * @param metadata
     */
    void addMetadataField(Metadata metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("metadata may not be null");
        }

        if (StringUtils.isBlank(getLabel()) && metadata.getLabel().equals("unittitle")) {
            setLabel(metadata.getFirstValue());
        }

        switch (metadata.getType()) {
            case 1:
                getIdentityStatementAreaList().add(metadata);
                if ("unitdate".equals(metadata.getLabel())) {
                    this.unitdate = metadata.getFirstValue();
                }
                break;
            case 2:
                getContextAreaList().add(metadata);
                break;
            case 3:
                getContentAndStructureAreaAreaList().add(metadata);
                break;
            case 4:
                getAccessAndUseAreaList().add(metadata);
                break;
            case 5:
                getAlliedMaterialsAreaList().add(metadata);
                break;
            case 6:
                getNotesAreaList().add(metadata);
                break;
            case 7:
                getDescriptionControlAreaList().add(metadata);
                break;
            default:
                break;
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

        // logger.trace("found root: {}", ret);
        return ret;
    }

    /**
     * @return the parentNode
     */
    public ArchiveEntry getParentNode() {
        return parentNode;
    }

    /**
     * @param parentNode the parentNode to set
     */
    public void setParentNode(ArchiveEntry parentNode) {
        this.parentNode = parentNode;
    }

    /**
     * @return the subEntryList
     */
    public List<ArchiveEntry> getSubEntryList() {
        return subEntryList;
    }

    /**
     * @param subEntryList the subEntryList to set
     */
    public void setSubEntryList(List<ArchiveEntry> subEntryList) {
        this.subEntryList = subEntryList;
    }

    /**
     * @return the orderNumber
     */
    public Integer getOrderNumber() {
        return orderNumber;
    }

    /**
     * @param orderNumber the orderNumber to set
     */
    public void setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
    }

    /**
     * @return the hierarchyLevel
     */
    public Integer getHierarchyLevel() {
        return hierarchyLevel;
    }

    /**
     * @param hierarchyLevel the hierarchyLevel to set
     */
    public void setHierarchyLevel(Integer hierarchyLevel) {
        this.hierarchyLevel = hierarchyLevel;
    }

    /**
     * @return the id
     */
    public String getId() {
        // logger.trace("getId: {}", id);
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the topstructPi
     */
    public String getTopstructPi() {
        return topstructPi;
    }

    /**
     * @param topstructPi the topstructPi to set
     */
    public void setTopstructPi(String topstructPi) {
        this.topstructPi = topstructPi;
    }

    /**
     * @return the displayChildren
     */
    public boolean isDisplayChildren() {
        return displayChildren;
    }

    /**
     * @param displayChildren the displayChildren to set
     */
    public void setDisplayChildren(boolean displayChildren) {
        this.displayChildren = displayChildren;
    }

    /**
     * @return the searchHit
     */
    public boolean isSearchHit() {
        return searchHit;
    }

    /**
     * @param searchHit the searchHit to set
     */
    public void setSearchHit(boolean searchHit) {
        this.searchHit = searchHit;
    }

    /**
     * @return the nodeType
     */
    public String getNodeType() {
        return nodeType;
    }

    /**
     * @param nodeType the nodeType to set
     */
    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    /**
     * @return the displaySearch
     */
    public boolean isDisplaySearch() {
        return displaySearch;
    }

    /**
     * @param displaySearch the displaySearch to set
     */
    public void setDisplaySearch(boolean displaySearch) {
        this.setDisplaySearch(displaySearch, false);
    }

    /**
     * 
     * @param displaySearch
     * @param recursive
     */
    public void setDisplaySearch(boolean displaySearch, boolean recursive) {
        this.displaySearch = displaySearch;
        if (recursive) {
            for (ArchiveEntry child : this.subEntryList) {
                child.setDisplaySearch(displaySearch, recursive);
            }
        }
    }

    public List<Metadata> getAllAreaLists() {
        // logger.trace("getAllAreaLists ({})", id);
        List<Metadata> ret = new ArrayList<>(getIdentityStatementAreaList().size()
                + getContextAreaList().size()
                + getContentAndStructureAreaAreaList().size()
                + getAccessAndUseAreaList().size()
                + getAlliedMaterialsAreaList().size()
                + getNotesAreaList().size()
                + getDescriptionControlAreaList().size());
        ret.addAll(getIdentityStatementAreaList());
        ret.addAll(getContextAreaList());
        ret.addAll(getContentAndStructureAreaAreaList());
        ret.addAll(getAccessAndUseAreaList());
        ret.addAll(getAlliedMaterialsAreaList());
        ret.addAll(getNotesAreaList());
        ret.addAll(getDescriptionControlAreaList());

        // logger.trace("getAllAreaLists END");
        return ret;
    }

    /**
     * 
     * @param index Area list index
     * @return Appropriate metadata list for the given index
     */
    public List<Metadata> getAreaList(int index) {
        switch (index) {
            case 0:
                return getIdentityStatementAreaList();
            case 1:
                return getContextAreaList();
            case 2:
                return getContentAndStructureAreaAreaList();
            case 3:
                return getAccessAndUseAreaList();
            case 4:
                return getAlliedMaterialsAreaList();
            case 5:
                return getNotesAreaList();
            case 6:
                return getDescriptionControlAreaList();
            default:
                return Collections.emptyList();
        }
    }

    /**
     * 
     * @param index Area list index
     * @return Appropriate message key for the given index
     */
    public String getAreaListLabel(int index) {
        switch (index) {
            case 0:
                return "archives__archive_area_identity";
            case 1:
                return "archives__archive_area_context";
            case 2:
                return "archives__archive_area_content";
            case 3:
                return "archives__archive_area_access";
            case 4:
                return "archives__archive_area_materials";
            case 5:
                return "archives__archive_area_notes";
            case 6:
                return "archives__archive_area_descriptionControl";
            default:
                return "TODO";
        }
    }

    /**
     * @return the identityStatementAreaList
     */
    public List<Metadata> getIdentityStatementAreaList() {
        // logger.trace("getIdentityStatementAreaList ({})", id); //NOSONAR Sometimes needed for debugging
        return identityStatementAreaList;
    }

    /**
     * @param identityStatementAreaList the identityStatementAreaList to set
     */
    public void setIdentityStatementAreaList(List<Metadata> identityStatementAreaList) {
        this.identityStatementAreaList = identityStatementAreaList;
    }

    /**
     * @return the contextAreaList
     */
    public List<Metadata> getContextAreaList() {
        // logger.trace("getContextAreaList ({})", id); //NOSONAR Sometimes needed for debugging
        return contextAreaList;
    }

    /**
     * @param contextAreaList the contextAreaList to set
     */
    public void setContextAreaList(List<Metadata> contextAreaList) {
        this.contextAreaList = contextAreaList;
    }

    /**
     * @return the contentAndStructureAreaAreaList
     */
    public List<Metadata> getContentAndStructureAreaAreaList() {
        // logger.trace("getContentAndStructureAreaAreaList ({})", id); //NOSONAR Sometimes needed for debugging
        return contentAndStructureAreaAreaList;
    }

    /**
     * @param contentAndStructureAreaAreaList the contentAndStructureAreaAreaList to set
     */
    public void setContentAndStructureAreaAreaList(List<Metadata> contentAndStructureAreaAreaList) {
        this.contentAndStructureAreaAreaList = contentAndStructureAreaAreaList;
    }

    /**
     * @return the accessAndUseAreaList
     */
    public List<Metadata> getAccessAndUseAreaList() {
        // logger.trace("getAccessAndUseAreaList ({})", id); //NOSONAR Sometimes needed for debugging
        return accessAndUseAreaList;
    }

    /**
     * @param accessAndUseAreaList the accessAndUseAreaList to set
     */
    public void setAccessAndUseAreaList(List<Metadata> accessAndUseAreaList) {
        this.accessAndUseAreaList = accessAndUseAreaList;
    }

    /**
     * @return the alliedMaterialsAreaList
     */
    public List<Metadata> getAlliedMaterialsAreaList() {
        // logger.trace("getAlliedMaterialsAreaList ({})", id); //NOSONAR Sometimes needed for debugging
        return alliedMaterialsAreaList;
    }

    /**
     * @param alliedMaterialsAreaList the alliedMaterialsAreaList to set
     */
    public void setAlliedMaterialsAreaList(List<Metadata> alliedMaterialsAreaList) {
        this.alliedMaterialsAreaList = alliedMaterialsAreaList;
    }

    /**
     * @return the notesAreaList
     */
    public List<Metadata> getNotesAreaList() {
        // logger.trace("getNotesAreaList ({})", id); //NOSONAR Sometimes needed for debugging
        return notesAreaList;
    }

    /**
     * @param notesAreaList the notesAreaList to set
     */
    public void setNotesAreaList(List<Metadata> notesAreaList) {
        this.notesAreaList = notesAreaList;
    }

    /**
     * @return the descriptionControlAreaList
     */
    public List<Metadata> getDescriptionControlAreaList() {
        // logger.trace("getDescriptionControlAreaList ({})", id); //NOSONAR Sometimes needed for debugging
        return descriptionControlAreaList;
    }

    /**
     * @param descriptionControlAreaList the descriptionControlAreaList to set
     */
    public void setDescriptionControlAreaList(List<Metadata> descriptionControlAreaList) {
        this.descriptionControlAreaList = descriptionControlAreaList;
    }

    /**
     * @return the valid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * @param valid the valid to set
     */
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /**
     * @return the descriptionLevel
     */
    public String getDescriptionLevel() {
        return descriptionLevel;
    }

    /**
     * @param descriptionLevel the descriptionLevel to set
     */
    public void setDescriptionLevel(String descriptionLevel) {
        this.descriptionLevel = descriptionLevel;
    }

    /**
     * @return the unitdate
     */
    public String getUnitdate() {
        return unitdate;
    }

    /**
     * @return the visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @param visible the visible to set
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * @return the expanded
     */
    public boolean isExpanded() {
        return expanded;
    }

    /**
     * @param expanded the expanded to set
     */
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    /**
     * @return the hasChild
     */
    public boolean isHasChild() {
        return isHasChildren();
    }

    /**
     * @return the associatedRecordPi
     */
    public String getAssociatedRecordPi() {
        return associatedRecordPi;
    }

    /**
     * @param associatedRecordPi the associatedRecordPi to set
     */
    public void setAssociatedRecordPi(String associatedRecordPi) {
        this.associatedRecordPi = associatedRecordPi;
    }

    /**
     * Get the parent node hierarchy of this node, optionally including the node itself The list is sorted with hightest hierarchy level first, so the
     * node itself will always be the last element, if included
     *
     * @param includeSelf
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

    /**
     * @return the doc
     */
    public SolrDocument getDoc() {
        return doc;
    }

    /**
     * @return the metadataLoaded
     */
    public boolean isMetadataLoaded() {
        return metadataLoaded;
    }

    /**
     * @return the childrenFound
     */
    public boolean isChildrenFound() {
        return childrenFound;
    }

    /**
     * @param childrenFound the childrenFound to set
     */
    public void setChildrenFound(boolean childrenFound) {
        this.childrenFound = childrenFound;
    }

    /**
     * @return the childrenLoaded
     */
    public boolean isChildrenLoaded() {
        return childrenLoaded;
    }

    /**
     * @param childrenLoaded the childrenLoaded to set
     */
    public void setChildrenLoaded(boolean childrenLoaded) {
        this.childrenLoaded = childrenLoaded;
    }

    public String getFieldValue(String field) {
        return getAllAreaLists().stream()
                .filter(entry -> entry.getLabel().equals(field))
                .map(Metadata::getFirstValue)
                .filter(StringUtils::isNotBlank)
                .findAny()
                .orElse(null);
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
