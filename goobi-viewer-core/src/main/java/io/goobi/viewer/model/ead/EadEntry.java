package io.goobi.viewer.model.ead;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;

public class EadEntry {

    private static final Logger logger = LoggerFactory.getLogger(EadEntry.class);

    // parent node
    private EadEntry parentNode;

    // list contains all child elements
    private List<EadEntry> subEntryList = new ArrayList<>();

    // order number of the current element within the current hierarchy
    private Integer orderNumber;

    // hierarchy level
    private Integer hierarchy;

    private String id; // c@id

    // display label
    private String label;
    // node is open/closed
    private boolean displayChildren;
    // node is saerch hit
    private boolean searchHit;

    // node can be selected as destination when moving nodes
    private boolean selectable;

    // node type -  @level
    private String nodeType;

    // display node in a search result
    private boolean displaySearch;

    /* 1. metadata for Identity Statement Area */
    //    Reference code(s)
    //    Title
    //    private String unittitle; // did/unittitle
    //    Date(s)
    //    Level of description
    //    Extent and medium of the unit of description (quantity, bulk, or size)
    private List<EadMetadataField> identityStatementAreaList = new ArrayList<>();

    /* 2. Context Area */
    //    Name of creator(s)
    //    Administrative | Biographical history
    //    Archival history
    //    Immediate source of acquisition or transfer
    private List<EadMetadataField> contextAreaList = new ArrayList<>();

    /* 3. Content and Structure Area */
    //    Scope and content
    //    Appraisal, destruction and scheduling information
    //    Accruals
    //    System of arrangement
    private List<EadMetadataField> contentAndStructureAreaAreaList = new ArrayList<>();

    /* 4. Condition of Access and Use Area */
    //    Conditions governing access
    //    Conditions governing reproduction
    //    Language | Scripts of material
    //    Physical characteristics and technical requirements
    //    Finding aids
    private List<EadMetadataField> accessAndUseAreaList = new ArrayList<>();

    /* 5. Allied Materials Area */
    //    Existence and location of originals
    //    Existence and location of copies
    //    Related units of description
    //    Publication note
    private List<EadMetadataField> alliedMaterialsAreaList = new ArrayList<>();

    /* 6. Note Area */
    //    Note
    private List<EadMetadataField> notesAreaList = new ArrayList<>();

    /* 7. Description Control Area */
    //    Archivist's Note
    //    Rules or Conventions
    //    Date(s) of descriptions
    private List<EadMetadataField> descriptionControlAreaList = new ArrayList<>();

    // empty if no process was created, otherwise the name of othe process is stored
    private String goobiProcessTitle;

    // true if the validation of all metadata fields was successful
    private boolean valid = true;

    private String descriptionLevel;

    private int index;
    private int parentIndex;
    private boolean visible = true;
    private boolean expanded = false;
    private boolean hasChild = false;
    @Deprecated
    private boolean showMetadata = false;
    private String associatedRecordPi;

    public EadEntry(Integer order, Integer hierarchy) {
        this.orderNumber = order;
        this.hierarchy = hierarchy;
    }

    public void addSubEntry(EadEntry other) {
        subEntryList.add(other);
        other.setParentNode(this);
    }

    public void removeSubEntry(EadEntry other) {
        subEntryList.remove(other);
        reOrderElements();
    }

    public void reOrderElements() {
        int order = 0;
        for (EadEntry entry : subEntryList) {
            entry.setOrderNumber(order++);
        }
    }

    /**
     * 
     * @param ignoreDisplayChildren
     * @return
     */
    public List<EadEntry> getAsFlatList(boolean ignoreDisplayChildren) {
        List<EadEntry> list = new LinkedList<>();
        list.add(this);
        if (displayChildren || ignoreDisplayChildren) {
            if (subEntryList != null && !subEntryList.isEmpty()) {
                setHasChild(true);
                for (EadEntry ds : subEntryList) {
                    list.addAll(ds.getAsFlatList(ignoreDisplayChildren));
                    // logger.trace("ID: {}, level {}", ds.getId(), ds.getHierarchy());
                }
            }
        }
        return list;
    }

    public void findAssociatedRecordPi() throws PresentationException, IndexUnreachableException {
        if (id == null) {
            associatedRecordPi = "";
            return;
        }

        SolrDocument doc = DataManager.getInstance()
                .getSearchIndex()
                .getFirstDoc("+" + SolrConstants.TECTONICS_ID + ":" + id, Collections.singletonList(SolrConstants.PI));
        if (doc != null) {
            associatedRecordPi = (String) doc.getFieldValue(SolrConstants.PI);
        }
        if (associatedRecordPi == null) {
            associatedRecordPi = "";
        }
    }

    @Deprecated
    public String toggleMetadata() {
        showMetadata = !showMetadata;
        return "";
    }

    public boolean isHasChildren() {
        return !subEntryList.isEmpty();
    }

    public List<EadEntry> getMoveToDestinationList(EadEntry entry) {
        List<EadEntry> list = new LinkedList<>();
        list.add(this);

        if (entry.equals(this)) {
            setSelectable(false);
            parentNode.setSelectable(false);
        } else if (subEntryList != null) {
            for (EadEntry ds : subEntryList) {
                list.addAll(ds.getMoveToDestinationList(entry));
            }
        }

        return list;
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
        EadEntry other = (EadEntry) obj;
        if (hierarchy == null) {
            if (other.hierarchy != null) {
                return false;
            }
        } else if (!hierarchy.equals(other.hierarchy)) {
            return false;
        }
        if (orderNumber == null) {
            if (other.orderNumber != null) {
                return false;
            }
        } else if (!orderNumber.equals(other.orderNumber)) {
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

        if (!parentNode.getOrderNumber().equals(other.parentNode.getOrderNumber())) {
            return false;
        }
        if (!parentNode.getHierarchy().equals(other.parentNode.getHierarchy())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((hierarchy == null) ? 0 : hierarchy.hashCode());
        result = prime * result + ((orderNumber == null) ? 0 : orderNumber.hashCode());
        result = prime * result + ((parentNode == null) ? 0 : parentNode.getHierarchy().hashCode());
        result = prime * result + ((parentNode == null) ? 0 : parentNode.getOrderNumber().hashCode());
        return result;
    }

    public void updateHierarchy() {
        // root node
        if (parentNode == null) {
            hierarchy = 0;
        } else {
            hierarchy = parentNode.getHierarchy() + 1;
        }

        for (EadEntry child : subEntryList) {
            child.updateHierarchy();
        }
    }

    public void markAsFound() {
        displaySearch = true;
        searchHit = true;

        if (parentNode != null) {
            EadEntry node = parentNode;
            while (!node.isDisplaySearch()) {
                node.setDisplaySearch(true);
                if (node.parentNode != null) {
                    node = node.parentNode;
                }
            }
        }
    }

    public void resetFoundList() {
        // logger.trace("resetFoundList: {}", id);
        displaySearch = false;
        searchHit = false;
        if (subEntryList != null) {
            for (EadEntry ds : subEntryList) {
                ds.resetFoundList();
            }
        }
    }

    public List<EadEntry> getSearchList() {
        List<EadEntry> list = new LinkedList<>();
        if (displaySearch) {
            list.add(this);
            if (subEntryList != null) {
                for (EadEntry child : subEntryList) {
                    list.addAll(child.getSearchList());
                }
            }
        }
        return list;
    }

    /**
     * Expands and sets visible all ancestors of this node and expands siblings of this node.
     */
    public void expandUp() {
        if (parentNode == null) {
            return;
        }

        parentNode.setVisible(visible);
        parentNode.expand();
        parentNode.expandUp();
    }

    /**
     * Expands this entry and sets all sub-entries visible if their immediate parent is expanded.
     */
    public void expand() {
        // logger.trace("expand: {}", id);
        if (!isHasChildren()) {
            return;
        }

        setExpanded(true);
        setChildrenVisibility(true);
    }

    /**
     * Collapses this entry and hides all sub-entries.
     */
    public void collapse() {
        // logger.trace("collapse: {}", id);
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

        for (EadEntry sub : subEntryList) {
            sub.setVisible(visible);
            if (sub.isExpanded() && sub.isHasChildren()) {
                sub.setChildrenVisibility(visible);
            }
        }
    }

    /**
     * @return the parentNode
     */
    public EadEntry getParentNode() {
        return parentNode;
    }

    /**
     * @param parentNode the parentNode to set
     */
    public void setParentNode(EadEntry parentNode) {
        this.parentNode = parentNode;
    }

    /**
     * @return the subEntryList
     */
    public List<EadEntry> getSubEntryList() {
        return subEntryList;
    }

    /**
     * @param subEntryList the subEntryList to set
     */
    public void setSubEntryList(List<EadEntry> subEntryList) {
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
     * @return the hierarchy
     */
    public Integer getHierarchy() {
        return hierarchy;
    }

    /**
     * @param hierarchy the hierarchy to set
     */
    public void setHierarchy(Integer hierarchy) {
        this.hierarchy = hierarchy;
    }

    /**
     * @return the id
     */
    public String getId() {
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
     * @return the selectable
     */
    public boolean isSelectable() {
        return selectable;
    }

    /**
     * @param selectable the selectable to set
     */
    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
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
        this.displaySearch = displaySearch;
    }

    public EadMetadataField getIdentityStatementAreaField(String name) {
        for (EadMetadataField field : identityStatementAreaList) {
            if (field.getName().equals(name)) {
                return field;
            }
        }

        return null;
    }

    public List<EadMetadataField> getAllAreaLists() {
        logger.trace("getAllAreaLists ({})", id);
        List<EadMetadataField> ret = new ArrayList<>(getIdentityStatementAreaList().size()
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

        logger.trace("getAllAreaLists END");
        return ret;
    }

    /**
     * @return the identityStatementAreaList
     */
    public List<EadMetadataField> getIdentityStatementAreaList() {
        // logger.trace("getIdentityStatementAreaList ({})", id);
        return identityStatementAreaList;
    }

    /**
     * @param identityStatementAreaList the identityStatementAreaList to set
     */
    public void setIdentityStatementAreaList(List<EadMetadataField> identityStatementAreaList) {
        this.identityStatementAreaList = identityStatementAreaList;
    }

    /**
     * @return the contextAreaList
     */
    public List<EadMetadataField> getContextAreaList() {
        // logger.trace("getContextAreaList ({})", id);
        return contextAreaList;
    }

    /**
     * @param contextAreaList the contextAreaList to set
     */
    public void setContextAreaList(List<EadMetadataField> contextAreaList) {
        this.contextAreaList = contextAreaList;
    }

    /**
     * @return the contentAndStructureAreaAreaList
     */
    public List<EadMetadataField> getContentAndStructureAreaAreaList() {
        // logger.trace("getContentAndStructureAreaAreaList ({})", id);
        return contentAndStructureAreaAreaList;
    }

    /**
     * @param contentAndStructureAreaAreaList the contentAndStructureAreaAreaList to set
     */
    public void setContentAndStructureAreaAreaList(List<EadMetadataField> contentAndStructureAreaAreaList) {
        this.contentAndStructureAreaAreaList = contentAndStructureAreaAreaList;
    }

    /**
     * @return the accessAndUseAreaList
     */
    public List<EadMetadataField> getAccessAndUseAreaList() {
        // logger.trace("getAccessAndUseAreaList ({})", id);
        return accessAndUseAreaList;
    }

    /**
     * @param accessAndUseAreaList the accessAndUseAreaList to set
     */
    public void setAccessAndUseAreaList(List<EadMetadataField> accessAndUseAreaList) {
        this.accessAndUseAreaList = accessAndUseAreaList;
    }

    /**
     * @return the alliedMaterialsAreaList
     */
    public List<EadMetadataField> getAlliedMaterialsAreaList() {
        // logger.trace("getAlliedMaterialsAreaList ({})", id);
        return alliedMaterialsAreaList;
    }

    /**
     * @param alliedMaterialsAreaList the alliedMaterialsAreaList to set
     */
    public void setAlliedMaterialsAreaList(List<EadMetadataField> alliedMaterialsAreaList) {
        this.alliedMaterialsAreaList = alliedMaterialsAreaList;
    }

    /**
     * @return the notesAreaList
     */
    public List<EadMetadataField> getNotesAreaList() {
        // logger.trace("getNotesAreaList ({})", id);
        return notesAreaList;
    }

    /**
     * @param notesAreaList the notesAreaList to set
     */
    public void setNotesAreaList(List<EadMetadataField> notesAreaList) {
        this.notesAreaList = notesAreaList;
    }

    /**
     * @return the descriptionControlAreaList
     */
    public List<EadMetadataField> getDescriptionControlAreaList() {
        // logger.trace("getDescriptionControlAreaList ({})", id);
        return descriptionControlAreaList;
    }

    /**
     * @param descriptionControlAreaList the descriptionControlAreaList to set
     */
    public void setDescriptionControlAreaList(List<EadMetadataField> descriptionControlAreaList) {
        this.descriptionControlAreaList = descriptionControlAreaList;
    }

    /**
     * @return the goobiProcessTitle
     */
    public String getGoobiProcessTitle() {
        return goobiProcessTitle;
    }

    /**
     * @param goobiProcessTitle the goobiProcessTitle to set
     */
    public void setGoobiProcessTitle(String goobiProcessTitle) {
        this.goobiProcessTitle = goobiProcessTitle;
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
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * @param index the index to set
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * @return the parentIndex
     */
    public int getParentIndex() {
        return parentIndex;
    }

    /**
     * @param parentIndex the parentIndex to set
     */
    public void setParentIndex(int parentIndex) {
        this.parentIndex = parentIndex;
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
        return hasChild;
    }

    /**
     * @param hasChild the hasChild to set
     */
    public void setHasChild(boolean hasChild) {
        this.hasChild = hasChild;
    }

    /**
     * @return the showMetadata
     */
    public boolean isShowMetadata() {
        return showMetadata;
    }

    /**
     * @param showMetadata the showMetadata to set
     */
    public void setShowMetadata(boolean showMetadata) {
        this.showMetadata = showMetadata;
    }

    /**
     * @return the associatedRecordPi
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public String getAssociatedRecordPi() throws PresentationException, IndexUnreachableException {
        if (associatedRecordPi == null) {
            findAssociatedRecordPi();
        }

        return associatedRecordPi;
    }

    @Override
    public String toString() {
        return id;
    }
}
