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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.jdom2.Element;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

/**
 * Loads and parses EAD documents from the Solr index.
 */
public class SolrEADParser extends ArchiveParser {

    private static final Logger logger = LogManager.getLogger(SolrEADParser.class);

    /**
     *
     * @param searchIndex
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public SolrEADParser(SolrSearchIndex searchIndex) throws PresentationException, IndexUnreachableException {
        super(searchIndex);
    }

    /**
     * Get the database names and file names from the basex databases
     *
     * @return List<ArchiveResource>
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    @Override
    public List<ArchiveResource> getPossibleDatabases() throws PresentationException, IndexUnreachableException {
        List<SolrDocument> docs = DataManager.getInstance()
                .getSearchIndex()
                .search("+" + SolrConstants.ISWORK + ":true +" + SolrConstants.DOCTYPE + ":" + DocType.ARCHIVE.name());

        List<ArchiveResource> ret = new ArrayList<>();
        String dbName = "TODO";
        for (SolrDocument doc : docs) {
            String resourceName = SolrTools.getSingleFieldStringValue(doc, SolrConstants.TITLE);
            Long lastUpdatedTimestamp = SolrTools.getSingleFieldLongValue(doc, SolrConstants.DATEUPDATED);
            LocalDateTime ldtDateUpdated = lastUpdatedTimestamp != null ? DateTools.getLocalDateTimeFromMillis(lastUpdatedTimestamp, false) : null;
            String lastUpdated = ldtDateUpdated != null ? DateTools.FORMATTERCNDATE.format(ldtDateUpdated) : null;
            String size = "0";
            ArchiveResource eadResource = new ArchiveResource(dbName, resourceName, lastUpdated, size);
            ret.add(eadResource);
        }

        return ret;
    }

    /**
     * Loads the given database and parses the EAD document.
     *
     * @param database
     * @return Root element of the loaded tree
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    @Override
    public ArchiveEntry loadDatabase(ArchiveResource database) throws PresentationException, IndexUnreachableException {

        SolrDocument doc = searchIndex.getFirstDoc(SolrConstants.PI + ":" + database.getResourceName(), null);
        if (doc != null) {
            return loadHierarchyFromIndex(1, 0, doc, configuredFields, associatedRecordMap);
        }

        return null;
    }

    /**
     * @param order
     * @param hierarchy
     * @param doc
     * @param configuredFields
     * @param associatedPIs
     * @return {@link ArchiveEntry}
     */
    private static ArchiveEntry loadHierarchyFromIndex(int order, int hierarchy, SolrDocument doc, List<ArchiveMetadataField> configuredFields,
            Map<String, Entry<String, Boolean>> associatedPIs) {
        if (doc == null) {
            throw new IllegalArgumentException("doc may not be null");
        }
        if (configuredFields == null) {
            throw new IllegalArgumentException("configuredFields may not be null");
        }

        ArchiveEntry entry = new ArchiveEntry(order, hierarchy);

        // Collect metadata
        for (ArchiveMetadataField emf : configuredFields) {
            List<String> stringValues = new ArrayList<>();
            if (StringUtils.isNotEmpty(emf.getIndexField())) {
                for (String value : SolrTools.getMetadataValues(doc, emf.getIndexField())) {
                    stringValues.add(value);
                }
            }

            addFieldToEntry(entry, emf, stringValues);
        }

        String id = SolrTools.getSingleFieldStringValue(doc, "MD_ARCHIVE_ENTRY_ID");
        if (StringUtils.isNotEmpty(id)) {
            entry.setId(id);
        }

        String label = SolrTools.getSingleFieldStringValue(doc, SolrConstants.TITLE);
        if (StringUtils.isNotEmpty(label)) {
            entry.setLabel(label);
        }

        //        // nodeType
        //        // get child elements
        //        List<Element> clist = null;
        //        Element archdesc = element.getChild("archdesc", NAMESPACE_EAD);
        //        if (archdesc != null) {
        //            setNodeType(archdesc, entry);
        //            Element dsc = archdesc.getChild("dsc", NAMESPACE_EAD);
        //            if (dsc != null) {
        //                clist = dsc.getChildren("c", NAMESPACE_EAD);
        //            }
        //
        //        } else {
        //            setNodeType(element, entry);
        //
        //        }
        //
        //        if (entry.getNodeType() == null) {
        //            entry.setNodeType("folder");
        //        }
        //
        //        Entry<String, Boolean> associatedRecordEntry = associatedPIs.get(entry.getId());
        //        if (associatedRecordEntry != null) {
        //            entry.setAssociatedRecordPi(associatedRecordEntry.getKey());
        //            entry.setContainsImage(associatedRecordEntry.getValue());
        //        }
        //
        //        // Set description level value
        //        entry.setDescriptionLevel(element.getAttributeValue("level"));
        //
        //        if (clist == null) {
        //            clist = element.getChildren("c", NAMESPACE_EAD);
        //        }
        //        if (clist != null) {
        //            int subOrder = 0;
        //            int subHierarchy = hierarchy + 1;
        //            for (Element c : clist) {
        //                ArchiveEntry child = parseElement(subOrder, subHierarchy, c, configuredFields, associatedPIs);
        //                entry.addSubEntry(child);
        //                child.setParentNode(entry);
        //                if (child.isContainsImage()) {
        //                    entry.setContainsImage(true);
        //                }
        //                subOrder++;
        //            }
        //        }

        // generate new id, if id is null
        if (entry.getId() == null) {
            entry.setId(String.valueOf(UUID.randomUUID()));
        }

        return entry;
    }

    /**
     * 
     * @param node
     * @param entry
     */
    public static void setNodeType(Element node, ArchiveEntry entry) {
        String type = node.getAttributeValue("otherlevel");
        if (StringUtils.isBlank(type)) {
            type = node.getAttributeValue("level");
        }
        entry.setNodeType(type);
    }

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
        ArchiveMetadataField toAdd = new ArchiveMetadataField(emf.getLabel(), emf.getType(), emf.getXpath(), emf.getXpathType());
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
     * @return {@link SolrEADParser}
     * @throws ConfigurationException
     */
    public SolrEADParser readConfiguration(HierarchicalConfiguration<ImmutableNode> metadataConfig) throws ConfigurationException {
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
                    hc.getString("@xpathType", "element"));
            configuredFields.add(field);
        }

        return this;
    }

    public static String getIdForName(String name) {
        return name.replaceAll("(?i)\\.xml", "");
    }
}
