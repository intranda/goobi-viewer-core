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

import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

public abstract class ArchiveParser implements Serializable {

    private static final long serialVersionUID = -7986836324388942249L;

    private static final Logger logger = LogManager.getLogger(ArchiveParser.class);

    protected transient Map<String, Entry<String, Boolean>> associatedRecordMap;

    /**
     *
     * @param searchIndex
     */
    protected ArchiveParser() {
    }

    public void updateAssociatedRecordMap() throws PresentationException, IndexUnreachableException {
        this.associatedRecordMap = getAssociatedRecordPis(DataManager.getInstance().getSearchIndex());
    }

    /**
     * 
     * @param searchIndex
     * @return Map<String, Entry<String, Boolean>>
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    protected static Map<String, Entry<String, Boolean>> getAssociatedRecordPis(SolrSearchIndex searchIndex)
            throws PresentationException, IndexUnreachableException {
        if (searchIndex != null) {
            return searchIndex
                    .search("+" + SolrConstants.EAD_NODE_ID + ":* +" + SolrConstants.PI + ":* +" + SolrConstants.DOCTYPE + ":"
                            + DocType.DOCSTRCT.name(),
                            Arrays.asList(SolrConstants.EAD_NODE_ID, SolrConstants.PI, SolrConstants.BOOL_IMAGEAVAILABLE))
                    .stream()
                    .collect(Collectors.toMap(doc -> SolrTools.getAsString(doc.getFieldValue(SolrConstants.EAD_NODE_ID)),
                            doc -> new SimpleEntry<String, Boolean>(SolrTools.getAsString(doc.getFieldValue(SolrConstants.PI)),
                                    SolrTools.getAsBoolean(doc.getFieldValue(SolrConstants.BOOL_IMAGEAVAILABLE)))));
        }
        return Collections.emptyMap();
    }

    /**
     * Get the database names and file names.
     *
     * @return List<ArchiveResource>
     * @throws HTTPException
     * @throws IndexUnreachableException
     * @throws IOException
     * @throws PresentationException
     */
    public abstract List<ArchiveResource> getPossibleDatabases() throws PresentationException, IndexUnreachableException, IOException, HTTPException;

    /**
     * Loads the given database and parses the EAD document.
     *
     * @param database
     * @param lazyLoadingThreshold Number of total archive nodes from which to no longer eager load the entire tree
     * @return Root element of the loaded tree
     * @throws HTTPException
     * @throws IllegalStateException
     * @throws IndexUnreachableException
     * @throws IOException
     * @throws JDOMException
     * @throws PresentationException
     */
    public abstract ArchiveEntry loadDatabase(ArchiveResource database, int lazyLoadingThreshold)
            throws PresentationException, IndexUnreachableException, IllegalStateException, IOException, HTTPException, JDOMException;

    public static String getIdForName(String name) {
        return name.replaceAll("(?i)\\.xml", "");
    }

    public abstract String getUrl();

    /**
     * 
     * @param node
     * @param searchValue
     * @return true if new nodes were loaded; false otherwise
     */
    public abstract boolean searchInUnparsedNodes(ArchiveEntry node, String searchValue);
}
