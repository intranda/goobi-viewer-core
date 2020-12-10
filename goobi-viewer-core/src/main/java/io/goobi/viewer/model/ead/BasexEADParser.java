/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.ead;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.model.viewer.StringPair;

public class BasexEADParser {

    private static final Logger logger = LoggerFactory.getLogger(BasexEADParser.class);

    public static final Namespace NAMESPACE_EAD = Namespace.getNamespace("ead", "urn:isbn:1-931666-22-9");

    private static final XPathFactory xFactory = XPathFactory.instance();

    private final String basexUrl;

    private String selectedDatabase;

    private boolean databaseLoaded = false;

    private EadEntry rootElement = null;

    private List<EadEntry> flatEntryList;

    //    private XMLConfiguration xmlConfig;

    private List<EadMetadataField> configuredFields;

    private List<StringPair> eventList;
    private List<String> editorList;

    /**
     * 
     * @param configFilePath
     * @throws ConfigurationException
     */
    public BasexEADParser(String basexUrl) throws ConfigurationException {

        this.basexUrl = basexUrl;

    }


    /**
     * Get the database names and file names from the basex databases
     * 
     * @return
     * @throws HTTPException
     * @throws IOException
     * @throws ClientProtocolException
     */
    public List<EadResource> getPossibleDatabases() throws ClientProtocolException, IOException, HTTPException {
        String response = NetTools.getWebContentGET(basexUrl + "databases");
        if (StringUtils.isBlank(response)) {
            return Collections.emptyList();
        }

        Document document;
        try {
            document = openDocument(response);

            Element root = document.getRootElement();
            List<Element> databaseList = root.getChildren("database");
            List<EadResource> ret = new ArrayList<>();
            for (Element db : databaseList) {
                String dbName = db.getChildText("name");

                Element details = db.getChild("details");
                for (Element resource : details.getChildren()) {
                    String resourceName = resource.getText();
                    String lastUpdated = resource.getAttributeValue("modified-date");
                    EadResource eadResource = new EadResource(dbName, resourceName, lastUpdated);
                    ret.add(eadResource);
                }

            }

            return ret;
        } catch (JDOMException e) {
            logger.error("Failed to parse response from " + (basexUrl + "databases"), e);
            return Collections.emptyList();
        }
    }

    public Document retrieveDatabaseDocument(String database) throws IOException, IllegalStateException {
        try {
            if (StringUtils.isNotBlank(database)) {
                String[] parts = database.split(" - ");
                String url = basexUrl + "db/" + parts[0] + "/" + parts[1];
                logger.trace("URL: {}", url);
                String response;
                response = NetTools.getWebContentGET(url);

                // get xml root element
                Document document = openDocument(response);
                return document;
            } else {
                throw new IllegalStateException("Must provide database name before loading database");
            }
        } catch (IOException | HTTPException | JDOMException e) {
            throw new IOException("Error loading ead database ", e);
        }
    }

    /**
     * open the selected database and load the file
     * 
     * @throws IllegalStateException
     * 
     * @throws HTTPException
     * @throws IOException
     * @throws ClientProtocolException
     */
    public void loadDatabase(String database, HierarchicalConfiguration metadataConfig, Document document) throws IllegalStateException, IOException {

        if (document == null) {
            document = retrieveDatabaseDocument(database);
        }
        // get field definitions from config file
        metadataConfig.setListDelimiter('&');
        metadataConfig.setExpressionEngine(new XPathExpressionEngine());
        readConfiguration(metadataConfig);

        // parse ead file
        parseEadFile(document);
        this.databaseLoaded = true;
        this.selectedDatabase = database;
        logger.info("Loaded EAD database: {}", selectedDatabase);
    }

    public List<String> getDistinctDatabaseNames() throws ClientProtocolException, IOException, HTTPException {
        List<String> answer = new ArrayList<>();
        List<EadResource> completeList = getPossibleDatabases();
        for (EadResource resource : completeList) {
            String dbName = resource.databaseName;
            if (!answer.contains(dbName)) {
                answer.add(dbName);
            }
        }

        return answer;
    }

    /*
     * get ead root element from document
     */
    private void parseEadFile(Document document) {
        eventList = new ArrayList<>();
        editorList = new ArrayList<>();

        Element collection = document.getRootElement();
        Element eadElement = collection.getChild("ead", NAMESPACE_EAD);
        rootElement = parseElement(1, 0, eadElement);
        rootElement.setDisplayChildren(true);

        Element archdesc = eadElement.getChild("archdesc", NAMESPACE_EAD);
        if (archdesc != null) {
            Element processinfoElement = archdesc.getChild("processinfo", NAMESPACE_EAD);
            if (processinfoElement != null) {
                Element list = processinfoElement.getChild("list", NAMESPACE_EAD);
                List<Element> entries = list.getChildren("item", NAMESPACE_EAD);
                for (Element item : entries) {
                    editorList.add(item.getText());
                }
            }
        }
        Element control = eadElement.getChild("control", NAMESPACE_EAD);
        if (control != null) {
            Element maintenancehistory = control.getChild("maintenancehistory", NAMESPACE_EAD);
            if (maintenancehistory != null) {
                List<Element> events = maintenancehistory.getChildren("maintenancehistory", NAMESPACE_EAD);
                for (Element event : events) {
                    String type = event.getChildText("eventtype", NAMESPACE_EAD);
                    String date = event.getChildText("eventdatetime", NAMESPACE_EAD);
                    eventList.add(new StringPair(type, date));
                }
            }
        }
    }

    /**
     * read the metadata for the current xml node. - create an {@link EadEntry} - execute the configured xpaths on the current node - add the metadata
     * to one of the 7 levels - check if the node has sub nodes - call the method recursively for all sub nodes
     */
    private EadEntry parseElement(int order, int hierarchy, Element element) {
        EadEntry entry = new EadEntry(order, hierarchy);

        for (EadMetadataField emf : configuredFields) {

            List<String> stringValues = new ArrayList<>();
            if ("text".equalsIgnoreCase(emf.getXpathType())) {
                XPathExpression<Text> engine = xFactory.compile(emf.getXpath(), Filters.text(), null, NAMESPACE_EAD);
                List<Text> values = engine.evaluate(element);
                for (Text value : values) {
                    String stringValue = value.getValue();
                    stringValues.add(stringValue);
                }
            } else if ("attribute".equalsIgnoreCase(emf.getXpathType())) {
                XPathExpression<Attribute> engine = xFactory.compile(emf.getXpath(), Filters.attribute(), null, NAMESPACE_EAD);
                List<Attribute> values = engine.evaluate(element);

                for (Attribute value : values) {
                    String stringValue = value.getValue();
                    stringValues.add(stringValue);
                }
            } else {
                XPathExpression<Element> engine = xFactory.compile(emf.getXpath(), Filters.element(), null, NAMESPACE_EAD);
                List<Element> values = engine.evaluate(element);
                for (Element value : values) {
                    String stringValue = value.getValue();
                    stringValues.add(stringValue);
                }
            }
            addFieldToEntry(entry, emf, stringValues);
        }

        Element eadheader = element.getChild("eadheader", NAMESPACE_EAD);

        entry.setId(element.getAttributeValue("id"));

        if (eadheader != null) {
            entry.setLabel(
                    eadheader.getChild("filedesc", NAMESPACE_EAD).getChild("titlestmt", NAMESPACE_EAD).getChildText("titleproper", NAMESPACE_EAD));
        }

        // nodeType
        // get child elements
        List<Element> clist = null;
        Element archdesc = element.getChild("archdesc", NAMESPACE_EAD);
        if (archdesc != null) {
            String type = archdesc.getAttributeValue("localtype");
            entry.setNodeType(type);
            Element dsc = archdesc.getChild("dsc", NAMESPACE_EAD);
            if (dsc != null) {
                // read process title
                List<Element> altformavailList = dsc.getChildren("altformavail", NAMESPACE_EAD);
                for (Element altformavail : altformavailList) {
                    if ("goobi_process".equals(altformavail.getAttributeValue("localtype"))) {
                        entry.setGoobiProcessTitle(altformavail.getText());
                    }
                }
                clist = dsc.getChildren("c", NAMESPACE_EAD);
            }

        } else {
            String type = element.getAttributeValue("otherlevel");
            entry.setNodeType(type);
            List<Element> altformavailList = element.getChildren("altformavail", NAMESPACE_EAD);
            for (Element altformavail : altformavailList) {
                if ("goobi_process".equals(altformavail.getAttributeValue("localtype"))) {
                    entry.setGoobiProcessTitle(altformavail.getText());
                }
            }
        }

        if (StringUtils.isBlank(entry.getNodeType())) {
            entry.setNodeType("folder");
        }

        // Set description level value
        entry.setDescriptionLevel(element.getAttributeValue("level"));

        if (clist == null) {
            clist = element.getChildren("c", NAMESPACE_EAD);
        }
        if (clist != null) {
            int subOrder = 0;
            int subHierarchy = hierarchy + 1;
            for (Element c : clist) {
                EadEntry child = parseElement(subOrder, subHierarchy, c);
                entry.addSubEntry(child);
                child.setParentNode(entry);
                subOrder++;
            }
        }

        // generate new id, if id is null
        if (entry.getId() == null) {
            entry.setId(String.valueOf(UUID.randomUUID()));
        }

        return entry;
    }

    /**
     * Add the metadata to the configured level
     * 
     * @param entry
     * @param emf
     * @param stringValue
     */

    private static void addFieldToEntry(EadEntry entry, EadMetadataField emf, List<String> stringValues) {
        if (StringUtils.isBlank(entry.getLabel()) && emf.getXpath().contains("unittitle") && stringValues != null && !stringValues.isEmpty()) {
            entry.setLabel(stringValues.get(0));
        }
        EadMetadataField toAdd = new EadMetadataField(emf.getLabel(), emf.getType(), emf.getXpath(), emf.getXpathType());
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
        }

    }

    /**
     * Parse the string response from the basex database into a xml document
     * 
     * @param response
     * @return
     * @throws IOException
     * @throws JDOMException
     */
    private static Document openDocument(String response) throws JDOMException, IOException {
        // read response
        SAXBuilder builder = new SAXBuilder(XMLReaders.NONVALIDATING);
        builder.setFeature("http://xml.org/sax/features/validation", false);
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        Document document = builder.build(new StringReader(response), "utf-8");
        return document;

    }

    public void resetFlatList() {
        flatEntryList = null;
    }

    /**
     * Get the hierarchical tree as a flat list
     * 
     * @return
     */

    public List<EadEntry> getFlatEntryList() {
        if (flatEntryList == null) {
            if (rootElement != null) {
                flatEntryList = new LinkedList<>();
                flatEntryList.addAll(rootElement.getAsFlatList(false));
            }
        }
        return flatEntryList;
    }

    public void search(String searchValue) {
        if (rootElement == null) {
            logger.error("Database not loaded");
            return;
        }

        if (StringUtils.isNotBlank(searchValue)) {
            // hide all elements
            rootElement.resetFoundList();
            // search in all/some metadata fields of all elements?

            // for now: search only labels
            searchInNode(rootElement, searchValue);

            // fill flatList with displayable fields
            flatEntryList = rootElement.getSearchList();
        } else {
            resetSearch();
        }
    }

    /**
     * 
     * @param node
     * @param searchValue
     */
    static void searchInNode(EadEntry node, String searchValue) {
        if (node.getId() != null && node.getId().equals(searchValue)) {
            // ID match
            node.markAsFound(true);
        } else if (node.getLabel() != null && node.getLabel().toLowerCase().contains(searchValue.toLowerCase())) {
            // mark element + all parents as displayable
            node.markAsFound(true);
        }
        if (node.getSubEntryList() != null) {
            for (EadEntry child : node.getSubEntryList()) {
                searchInNode(child, searchValue);
            }
        }
    }

    public void resetSearch() {
        rootElement.resetFoundList();
        flatEntryList = null;
    }

    /**
     * read in all parameters from the configuration file
     * 
     */
    private void readConfiguration(HierarchicalConfiguration metadataConfig) {

        configuredFields = new ArrayList<>();

        for (HierarchicalConfiguration hc : metadataConfig.configurationsAt("/metadata")) {
            EadMetadataField field = new EadMetadataField(hc.getString("[@label]"), hc.getInt("[@type]"), hc.getString("[@xpath]"),
                    hc.getString("[@xpathType]", "element"));
            configuredFields.add(field);
        }
    }

    /**
     * @return the selectedDatabase
     */
    public String getSelectedDatabase() {
        return selectedDatabase;
    }

    /**
     * @return the databaseLoaded
     */
    public boolean isDatabaseLoaded() {
        return databaseLoaded;
    }

    /**
     * @return the rootElement
     */
    public EadEntry getRootElement() {
        return rootElement;
    }

    /**
     * 
     * @return the {@link EadEntry} with the given identifier if it exists in the tree; null otherwise
     * @param identifier
     */
    public EadEntry getEntryById(String identifier) {
        return findEntry(identifier, getRootElement()).orElse(null);
    }

    /**
     * Return this node if it has the given identifier or the first of its descendents with the identifier
     * 
     * @param identifier
     * @param topNode
     * @return
     */
    private Optional<EadEntry> findEntry(String identifier, EadEntry node) {
        if (StringUtils.isNotBlank(identifier)) {
            if (identifier.equals(node.getId())) {
                return Optional.of(node);
            } else {
                if (node.getSubEntryList() != null) {
                    for (EadEntry child : node.getSubEntryList()) {
                        Optional<EadEntry> find = findEntry(identifier, child);
                        if (find.isPresent()) {
                            return find;
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * @return the basexUrl
     */
    public String getBasexUrl() {
        return basexUrl;
    }
}
