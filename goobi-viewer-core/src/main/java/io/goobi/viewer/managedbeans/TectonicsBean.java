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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.ead.BasexEADParser;
import io.goobi.viewer.model.ead.EADTree;
import io.goobi.viewer.model.ead.EadEntry;

@Named
@SessionScoped
public class TectonicsBean implements Serializable {

    private static final long serialVersionUID = -1755934299534933504L;

    private static final Logger logger = LoggerFactory.getLogger(TectonicsBean.class);

    private static final String CONFIG_FILE_NAME = "plugin_intranda_administration_archive_management.xml";

    private static final Object lock = new Object();

    private BasexEADParser eadParser = null;

    private EADTree tectonicsTree;

    private String searchString;

    /**
     * Empty constructor.
     */
    public TectonicsBean() {
        // the emptiness inside
    }

    /**
     * 
     */
    @PostConstruct
    public void init() {
        try {
            String databaseUrl = DataManager.getInstance().getConfiguration().getBaseXUrl();
            String databaseName = DataManager.getInstance().getConfiguration().getBaseXDatabase();
            HierarchicalConfiguration baseXMetadataConfig = DataManager.getInstance().getConfiguration().getBaseXMetadataConfig();
            eadParser = new BasexEADParser(databaseUrl, databaseName, baseXMetadataConfig);
            eadParser.loadSelectedDatabase();
            // TODO selection/reloading of different databases
        } catch (ConfigurationException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 
     * @return
     */
    public EADTree getTectonicsTree() {
        // logger.trace("getTectonicsTree");
        if (eadParser == null || !eadParser.isDatabaseLoaded()) {
            return null;
        }

        EADTree h = tectonicsTree;
        if (h == null) {
            synchronized (lock) {
                // Another thread might have initialized hierarchy by now
                h = tectonicsTree;
                if (h == null) {
                    h = generateHierarchy();
                    tectonicsTree = h;
                }
            }
        }

        return tectonicsTree;
    }

    /**
     * 
     * @return
     */
    EADTree generateHierarchy() {
        if (eadParser == null || !eadParser.isDatabaseLoaded()) {
            return null;
        }

        EADTree ret = new EADTree();
        ret.generate(eadParser.getRootElement());
        if (ret.getSelectedEntry() == null) {
            ret.setSelectedEntry(eadParser.getRootElement());
        }
        // This should happen before the tree is expanded to the selected entry, otherwise the collapse level will be reset
        ret.getTreeView();

        return ret;
    }

    /**
     * <p>
     * expandEntry.
     * </p>
     *
     * @param entry a {@link io.goobi.viewer.model.toc.TOCElement} object.
     */
    public void expandEntry(EadEntry entry) {
        logger.trace("expandEntry: {}", entry);
        if (tectonicsTree == null) {
            return;
        }
        synchronized (tectonicsTree) {
            tectonicsTree.setToExpandIndex(entry.getIndex());
            tectonicsTree.updateTree();
        }
    }

    /**
     * <p>
     * collapseEntry.
     * </p>
     *
     * @param entry a {@link io.goobi.viewer.model.toc.TOCElement} object.
     */
    public void collapseEntry(EadEntry entry) {
        logger.trace("collapseEntry: {}", entry);
        if (tectonicsTree == null) {
            return;
        }

        synchronized (tectonicsTree) {
            tectonicsTree.setToCollapseIndex(entry.getIndex());
            tectonicsTree.updateTree();
        }
    }

    /**
     * Returns the entry hierarchy from the root down to the entry with the given identifier.
     * 
     * @param identifier Entry identifier
     * @param List of entries
     */
    public List<EadEntry> getTectonicsHierarchyForIdentifier(String identifier) {
        if (StringUtils.isEmpty(identifier)) {
            return Collections.emptyList();
        }

        if (eadParser == null) {
            logger.error("EAD parser not intialized");
            return Collections.emptyList();
        }

        eadParser.search(identifier);

        switch (eadParser.getFlatEntryList().size()) {
            case 0:
                return Collections.emptyList();
            case 1:
            case 2:
                return Collections.singletonList(eadParser.getFlatEntryList().get(0));
            default:
                // Remove root and the entry with the given identifier
                return eadParser.getFlatEntryList().subList(1, eadParser.getFlatEntryList().size() - 1);
        }
    }

    /**
     * 
     * @param entry
     * @return
     */
    public String selectEntryAction(EadEntry entry) {
        if (entry == null || tectonicsTree == null) {
            return "";
        }

        tectonicsTree.setSelectedEntry(entry);

        return "";
    }

    /**
     * 
     * @return
     */
    public String searchAction() {
        logger.trace("searchAction: {}", searchString);
        if (eadParser == null || !eadParser.isDatabaseLoaded() || tectonicsTree == null) {
            logger.warn("Tree not loaded, cannot search.");
            return "";
        }

        if (StringUtils.isEmpty(searchString)) {
            eadParser.resetSearch();
            tectonicsTree.resetCollapseLevel(tectonicsTree.getRootElement(), EADTree.defaultCollapseLevel);
            return "";
        }

        eadParser.search(searchString);
        List<EadEntry> results = eadParser.getFlatEntryList();
        if (results == null || results.isEmpty()) {
            return "";
        }
        logger.trace("result entries: {}", results.size());

        tectonicsTree.setSelectedEntry(null);
        tectonicsTree.collapseAll(true);
        for (EadEntry entry : results) {
            if (entry.isSearchHit()) {
                expandHierarchyToEntry(entry, false);
            }
        }
        tectonicsTree.updateTree();

        return "";
    }

    /**
     * @return the searchString
     */
    public String getSearchString() {
        return searchString;
    }

    /**
     * @param searchString the searchString to set
     */
    public void setSearchString(String searchString) {
        logger.trace("setSearchString: {}", searchString);
        this.searchString = searchString;
    }

    /**
     * 
     * @return
     */
    public String getSelectedEntryId() {
        if (tectonicsTree == null || tectonicsTree.getSelectedEntry() == null) {
            return "-";
        }

        return tectonicsTree.getSelectedEntry().getId();
    }

    /**
     * 
     * @param id
     */
    public void setSelectedEntryId(String id) {
        logger.trace("setSelectedEntryId: {}", id);
        if (getTectonicsTree() == null || eadParser == null) {
            return;
        }
        if (StringUtils.isBlank(id)) {
            tectonicsTree.setSelectedEntry(null);
            return;
        } else if ("-".equals(id)) {
            // tectonicsTree.resetCollapseLevel();
            tectonicsTree.setSelectedEntry(eadParser.getRootElement());
            return;
        } else {
            // Find entry with given ID in the tree
            eadParser.search(id);
            List<EadEntry> results = eadParser.getFlatEntryList();
            if (results == null || results.isEmpty()) {
                logger.debug("Entry not found: {}", id);
                tectonicsTree.setSelectedEntry(eadParser.getRootElement());
                return;
            }

            EadEntry entry = results.get(results.size() - 1);
            tectonicsTree.setSelectedEntry(entry);
            expandHierarchyToEntry(entry, false);
            tectonicsTree.updateTree();
        }
    }

    /**
     * 
     * @param entry
     * @param expand
     */
    void expandHierarchyToEntry(EadEntry entry, boolean expand) {
        logger.trace("expandHierarchyToEntry: {}", entry);
        if (entry == null || tectonicsTree == null) {
            return;
        }

        entry.setVisible(true);
        entry.setExpanded(expand);
        //        if (expand) {
        //            setChildrenVisible(entry);
        //        } else {
        //            setChildrenInvisible(entry);
        //        }

        expandHierarchyToEntry(entry.getParentNode(), true);
    }

    /**
     * 
     * @param hierarchy
     */
    void selectAndExpandEntry(List<EadEntry> hierarchy) {
        if (hierarchy == null || hierarchy.isEmpty() || tectonicsTree == null) {
            return;
        }

        logger.trace("hierarchy size: {}", hierarchy.size());
        for (int i = 0; i < hierarchy.size(); ++i) {
            EadEntry entry = hierarchy.get(i);
            if (i == hierarchy.size() - 1) {
                // Select last node
                tectonicsTree.setSelectedEntry(entry);
            } else {
                // Expand all parent nodes
                entry.setExpanded(true);
                expandEntry(entry);
            }
        }
    }

}
