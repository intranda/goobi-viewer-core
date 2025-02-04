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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.archives.ArchiveEntry;
import io.goobi.viewer.model.archives.ArchiveEntryMetadataList;

/**
 * Short lived bean to store {@link ArchiveEntryMetadataList metadata lists} for {@link ArchiveEntry archiveEntries}.
 */
@Named
@ViewScoped
public class ArchiveMetadataBean implements Serializable {

    private static final long serialVersionUID = 7525640333611786457L;
    private final Map<String, ArchiveEntryMetadataList> entryMap = new ConcurrentHashMap<>();
    private final Configuration config;

    /**
     * default constructor using local configuration to load metadata list
     */
    public ArchiveMetadataBean() {
        this(DataManager.getInstance().getConfiguration());
    }

    /**
     * Constructor for testing. Receives custom config object of which only the method {@link Configuration#getArchiveMetadata()} is used
     * 
     * @param config
     */
    public ArchiveMetadataBean(Configuration config) {
        this.config = config;
    }

    /**
     * Check if metadata for the {@link ArchiveEntry} with the given id has already been cached in the bean
     * 
     * @param entryId id of the archive entry
     * @return true if metadata for the entry is stored in the bean
     */
    public boolean isMetadataLoaded(String entryId) {
        return entryMap.containsKey(entryId);
    }

    /**
     * Get the {@link ArchiveEntryMetadataList} for the given {@link ArchiveEntry}. If the metadata is not already stored in the bean, it is created
     * using the entry's solr document and the metadata list of the bean, and stored within the bean
     * 
     * @param entry The entry which metadata to return
     * @return the metadata list for the given entry
     * @throws PresentationException If the metadata list had to be created and an error occured while doing so
     */
    public ArchiveEntryMetadataList getMetadata(ArchiveEntry entry) throws PresentationException {
        if (!isMetadataLoaded(entry.getId())) {
            loadMetadata(entry);
        }
        return getMetadata(entry.getId());
    }

    public void setUnitDate(ArchiveEntry entry) {
        config.getArchiveMetadata().stream().filter(md -> "unitdate".equals(md.getLabel())).findAny().ifPresent(mdDate -> {

        });
    }

    private void loadMetadata(ArchiveEntry entry) throws PresentationException {

        if (entry == null) {
            throw new PresentationException("Cannot find ArchiveMetadataBean in scope");
        }

        ArchiveEntryMetadataList metadata = new ArchiveEntryMetadataList(entry.getId(), entry.getDoc(), config.getArchiveMetadata());
        this.entryMap.put(entry.getId(), metadata);

        if (StringUtils.isBlank(entry.getLabel())) {
            String unitTitle = metadata.getFirstValue("unittitle");
            if (StringUtils.isNotBlank(unitTitle)) {
                entry.setLabel(unitTitle);
            }
        }

        String unitDate = metadata.getFirstValue("unitdate", 1);
        if (StringUtils.isNotBlank(unitDate)) {
            entry.setUnitdate(unitDate);
        }

    }

    private ArchiveEntryMetadataList getMetadata(String entryId) {
        return entryMap.get(entryId);
    }

}
