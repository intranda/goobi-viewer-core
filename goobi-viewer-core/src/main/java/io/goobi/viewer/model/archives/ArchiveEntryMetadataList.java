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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.ArchiveMetadataBean;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * Metadata list for {@link ArchiveEntry archive entries}. This is delegated to its own class so it can live in a different scope than the actual
 * entries. Specifically, this is stored in {@link ArchiveMetadataBean} so changes to the metadata list config are updated within the scope of that
 * bean
 */
public class ArchiveEntryMetadataList {

    private static final Logger logger = LogManager.getLogger(ArchiveEntryMetadataList.class);

    /**
     * id referencing an archiveEntry
     */
    private final String id;

    /* 1. metadata for Identity Statement Area */
    //    Reference code(s)
    //    Title
    //    private String unittitle; // did/unittitle
    //    Date(s)
    //    Level of description
    //    Extent and medium of the unit of description (quantity, bulk, or size)
    private final List<Metadata> identityStatementAreaList = new ArrayList<>();

    /* 2. Context Area */
    //    Name of creator(s)
    //    Administrative | Biographical history
    //    Archival history
    //    Immediate source of acquisition or transfer
    private final List<Metadata> contextAreaList = new ArrayList<>();

    /* 3. Content and Structure Area */
    //    Scope and content
    //    Appraisal, destruction and scheduling information
    //    Accruals
    //    System of arrangement
    private final List<Metadata> contentAndStructureAreaAreaList = new ArrayList<>();

    /* 4. Condition of Access and Use Area */
    //    Conditions governing access
    //    Conditions governing reproduction
    //    Language | Scripts of material
    //    Physical characteristics and technical requirements
    //    Finding aids
    private final List<Metadata> accessAndUseAreaList = new ArrayList<>();

    /* 5. Allied Materials Area */
    //    Existence and location of originals
    //    Existence and location of copies
    //    Related units of description
    //    Publication note
    private final List<Metadata> alliedMaterialsAreaList = new ArrayList<>();

    /* 6. Note Area */
    //    Note
    private final List<Metadata> notesAreaList = new ArrayList<>();

    /* 7. Description Control Area */
    //    Archivist's Note
    //    Rules or Conventions
    //    Date(s) of descriptions
    private final List<Metadata> descriptionControlAreaList = new ArrayList<>();

    public ArchiveEntryMetadataList(String id, SolrDocument doc, List<Metadata> metadataList) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("Id of archive entry metadata list may not be null or empty");
        } else if (doc == null) {
            throw new IllegalArgumentException("Cannot create an archvie entry metadata list from solr document 'null'");
        }
        this.id = id;
        logger.trace("loadMetadata ({})", doc);
        try {
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
        }
    }

    /**
     * @return the identityStatementAreaList
     */
    public List<Metadata> getIdentityStatementAreaList() {
        // logger.trace("getIdentityStatementAreaList ({})", id); //NOSONAR Debug
        return identityStatementAreaList;
    }

    /**
     * @return the contextAreaList
     */
    public List<Metadata> getContextAreaList() {
        // logger.trace("getContextAreaList ({})", id); //NOSONAR Debug
        return contextAreaList;
    }

    /**
     * @return the contentAndStructureAreaAreaList
     */
    public List<Metadata> getContentAndStructureAreaAreaList() {
        // logger.trace("getContentAndStructureAreaAreaList ({})", id); //NOSONAR Debug
        return contentAndStructureAreaAreaList;
    }

    /**
     * @return the accessAndUseAreaList
     */
    public List<Metadata> getAccessAndUseAreaList() {
        // logger.trace("getAccessAndUseAreaList ({})", id); //NOSONAR Debug
        return accessAndUseAreaList;
    }

    /**
     * @return the alliedMaterialsAreaList
     */
    public List<Metadata> getAlliedMaterialsAreaList() {
        // logger.trace("getAlliedMaterialsAreaList ({})", id); //NOSONAR Debug
        return alliedMaterialsAreaList;
    }

    /**
     * @return the notesAreaList
     */
    public List<Metadata> getNotesAreaList() {
        // logger.trace("getNotesAreaList ({})", id); //NOSONAR Debug
        return notesAreaList;
    }

    /**
     * @return the descriptionControlAreaList
     */
    public List<Metadata> getDescriptionControlAreaList() {
        // logger.trace("getDescriptionControlAreaList ({})", id); //NOSONAR Debug
        return descriptionControlAreaList;
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

    public List<Metadata> getAllAreaLists() {
        // logger.trace("getAllAreaLists ({})", id); //NOSONAR Debug
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

        // logger.trace("getAllAreaLists END"); //NOSONAR Debug
        return ret;
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

        switch (metadata.getType()) {
            case 1:
                getIdentityStatementAreaList().add(metadata);
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

    public String getFirstValue(String metadataLabel, int metadataType) {
        if (StringUtils.isBlank(metadataLabel)) {
            return "";
        }
        return getAreaList(metadataType).stream().filter(md -> metadataLabel.equals(md.getLabel())).map(Metadata::getFirstValue).findAny().orElse("");
    }

    public String getFirstValue(String metadataLabel) {
        if (StringUtils.isBlank(metadataLabel)) {
            return "";
        }
        return getAllAreaLists().stream().filter(md -> metadataLabel.equals(md.getLabel())).map(Metadata::getFirstValue).findAny().orElse("");
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(this.getClass())) {
            return ((ArchiveEntryMetadataList) obj).id.equals(this.id);
        }
        return false;
    }

}
