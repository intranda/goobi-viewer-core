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

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.solr.common.SolrDocument;

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

    public boolean isMetadataLoaded(String entryId) {
        return entryMap.containsKey(entryId);
    }

    public ArchiveEntryMetadataList getMetadata(String entryId) {
        return entryMap.get(entryId);
    }

    public ArchiveEntryMetadataList loadMetadata(String entryId, SolrDocument doc) {
        ArchiveEntryMetadataList list = new ArchiveEntryMetadataList(entryId, doc);
        this.entryMap.put(entryId, list);
        return list;
    }

}
