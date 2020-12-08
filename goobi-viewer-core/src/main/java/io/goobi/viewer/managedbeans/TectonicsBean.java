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
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multiset.Entry;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.ead.BasexEADParser;
import io.goobi.viewer.model.ead.EADTree;
import io.goobi.viewer.model.ead.EadEntry;

@Named
@ViewScoped
public class TectonicsBean implements Serializable {

    private static final long serialVersionUID = -1755934299534933504L;

    private static final Logger logger = LoggerFactory.getLogger(TectonicsBean.class);

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
     * @return actual root element of the document, even if it's not in the displayed tree
     */
    public EadEntry getTrueRoot() {
        if (eadParser == null || !eadParser.isDatabaseLoaded()) {
            return null;
        }

        return eadParser.getRootElement();
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
            entry.expand();
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
            entry.collapse();
        }
    }

    /**
     * Returns the entry hierarchy from the root down to the entry with the given identifier.
     * 
     * @param identifier Entry identifier
     * @param List of entries   An empty list if the identified node has no anchestors or doesn't exist
     */
    public List<EadEntry> getTectonicsHierarchyForIdentifier(String identifier) {
        if (StringUtils.isEmpty(identifier)) {
            return Collections.emptyList();
        }

        if (eadParser == null) {
            logger.error("EAD parser not intialized");
            return Collections.emptyList();
        }

        EadEntry entry = eadParser.getEntryById(identifier);
        if(entry == null) {
//            return Collections.emptyList();
            return Collections.singletonList(getTrueRoot());
        } else if(getTrueRoot().equals(entry) || getTrueRoot().equals(entry.getParentNode())) {
            return Collections.singletonList(entry);
        } else {
            return entry.getAncestors(false).stream().skip(1).collect(Collectors.toList());            
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
        search(true, true);

        return "";
    }

    /**
     * Executes search for searchString.
     * 
     * @param resetSelectedEntry If true, selected entry will be set to null
     * @param collapseAll If true, all elements will be collapsed before expanding path to search hits
     */
    void search(boolean resetSelectedEntry, boolean collapseAll) {
        if (eadParser == null || !eadParser.isDatabaseLoaded() || tectonicsTree == null) {
            logger.warn("Tree not loaded, cannot search.");
            return;
        }

        if (StringUtils.isEmpty(searchString)) {
            eadParser.resetSearch();
            tectonicsTree.resetCollapseLevel(tectonicsTree.getRootElement(), EADTree.defaultCollapseLevel);
            return;
        }

        eadParser.search(searchString);
        List<EadEntry> results = eadParser.getFlatEntryList();
        if (results == null || results.isEmpty()) {
            return;
        }
        logger.trace("result entries: {}", results.size());

        if (resetSelectedEntry) {
            setSelectedEntryId(null);
        }
        tectonicsTree.collapseAll(collapseAll);
        for (EadEntry entry : results) {
            if (entry.isSearchHit()) {
                entry.expandUp();
            }
        }
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
     * Setter for the URL parameter. Loads the entry that has the given ID. Loads the tree, if this is a new sessions.
     * 
     * @param id Entry ID
     */
    public void setSelectedEntryId(String id) {
        logger.trace("setSelectedEntryId: {}", id);

        // getTectonicsTree() will also load the tree, if not yet loaded
        if (getTectonicsTree() == null || eadParser == null) {
            return;
        }
        // Select root element if no ID given
        if (StringUtils.isBlank(id)) {
            id = "-";
        }
        if ("-".equals(id)) {
            tectonicsTree.setSelectedEntry(null);
            return;
        }
        // Requested entry is already selected
        if (tectonicsTree.getSelectedEntry() != null && tectonicsTree.getSelectedEntry().getId().equals(id)) {
            return;
        }

        // Find entry with given ID in the tree
            EadEntry result = eadParser.getEntryById(id);
            if(result != null) {
                tectonicsTree.setSelectedEntry(result);
                result.expandUp();
            } else {
                logger.debug("Entry not found: {}", id);
                tectonicsTree.setSelectedEntry(eadParser.getRootElement());
            }


    }
    
    public boolean isSearchActive() {
        return StringUtils.isNotBlank(searchString);
    }
    
    /**
     * 
     * @return the {@link EadEntry} to display in the metadata section of the archives view.
     * Either {@link EADTree#getSelectedEntry()} or {@link EADTree#getRootElement()} if the former is null
     */
    public EadEntry getDisplayEntry() {
        return Optional.ofNullable(tectonicsTree.getSelectedEntry()).orElse(tectonicsTree.getRootElement());
    }
}
