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
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;

import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

public abstract class ArchiveParser {

    private static final Logger logger = LogManager.getLogger(ArchiveParser.class);

    protected final SolrSearchIndex searchIndex;

    protected List<ArchiveMetadataField> configuredFields;

    protected Map<String, Entry<String, Boolean>> associatedRecordMap;

    /**
     *
     * @param searchIndex
     */
    protected ArchiveParser(SolrSearchIndex searchIndex) {
        this.searchIndex = searchIndex;
    }

    public void updateAssociatedRecordMap() throws PresentationException, IndexUnreachableException {
        this.associatedRecordMap = getAssociatedRecordPis(this.searchIndex);
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
                    .search("+" + SolrConstants.ARCHIVE_ENTRY_ID + ":*" + " +" + SolrConstants.PI + ":*",
                            Arrays.asList(SolrConstants.ARCHIVE_ENTRY_ID, SolrConstants.PI, SolrConstants.BOOL_IMAGEAVAILABLE))
                    .stream()
                    .collect(Collectors.toMap(doc -> SolrTools.getAsString(doc.getFieldValue(SolrConstants.ARCHIVE_ENTRY_ID)),
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
     * @return Root element of the loaded tree
     * @throws HTTPException
     * @throws IllegalStateException
     * @throws IndexUnreachableException
     * @throws IOException
     * @throws JDOMException
     * @throws PresentationException
     */
    public abstract ArchiveEntry loadDatabase(ArchiveResource database)
            throws PresentationException, IndexUnreachableException, IllegalStateException, IOException, HTTPException, JDOMException;

    /**
     * Add the metadata to the configured level
     *
     * @param entry
     * @param emf
     * @param stringValues
     */
    protected static void addFieldToEntry(ArchiveEntry entry, ArchiveMetadataField emf, List<String> stringValues) {
        if (StringUtils.isBlank(entry.getLabel()) && emf.getXpath().contains("unittitle") && stringValues != null && !stringValues.isEmpty()) {
            entry.setLabel(stringValues.get(0));
        }
        ArchiveMetadataField toAdd = new ArchiveMetadataField(emf.getLabel(), emf.getType(), emf.getXpath(), emf.getXpathType(), emf.getIndexField());
        toAdd.setEadEntry(entry);

        if (stringValues != null && !stringValues.isEmpty()) {

            // split single value into multiple fields
            for (String stringValue : stringValues) {
                FieldValue fv = new FieldValue(toAdd);
                fv.setValue(stringValue);
                toAdd.addFieldValue(fv);
            }
        } else {
            FieldValue fv = new FieldValue(toAdd);
            toAdd.addFieldValue(fv);
        }

        switch (toAdd.getType()) {
            case 1:
                entry.getIdentityStatementAreaList().add(toAdd);
                break;
            case 2:
                entry.getContextAreaList().add(toAdd);
                break;
            case 3:
                entry.getContentAndStructureAreaAreaList().add(toAdd);
                break;
            case 4:
                entry.getAccessAndUseAreaList().add(toAdd);
                break;
            case 5:
                entry.getAlliedMaterialsAreaList().add(toAdd);
                break;
            case 6:
                entry.getNotesAreaList().add(toAdd);
                break;
            case 7:
                entry.getDescriptionControlAreaList().add(toAdd);
                break;
            default:
                break;
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
     * Loads fields from the given configuration node.
     *
     * @param metadataConfig
     * @return {@link ArchiveParser}
     * @throws ConfigurationException
     */
    public ArchiveParser readConfiguration(HierarchicalConfiguration<ImmutableNode> metadataConfig) throws ConfigurationException {
        if (metadataConfig == null) {
            throw new ConfigurationException("No basexMetadata configurations found");
        }
        // metadataConfig.setListDelimiter('&');
        metadataConfig.setExpressionEngine(new XPathExpressionEngine());

        List<HierarchicalConfiguration<ImmutableNode>> configurations = metadataConfig.configurationsAt("/metadata");
        if (configurations == null) {
            throw new ConfigurationException("Error reading basexMetadata configuration: No basexMetadata configurations found");
        }
        configuredFields = new ArrayList<>(configurations.size());
        for (HierarchicalConfiguration<ImmutableNode> hc : configurations) {
            ArchiveMetadataField field = new ArchiveMetadataField(hc.getString("@label"), hc.getInt("@type"), hc.getString("@xpath"),
                    hc.getString("@xpathType", "element"), hc.getString("@indexField"));
            configuredFields.add(field);
        }

        return this;
    }

    public static String getIdForName(String name) {
        return name.replaceAll("(?i)\\.xml", "");
    }

    public abstract String getUrl();
}
