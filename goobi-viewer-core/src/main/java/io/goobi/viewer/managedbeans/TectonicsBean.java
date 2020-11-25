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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
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

    private static String database = "Test - EAD_StadtA_GOE_Dep__109_9227_2018_10_09_13_14_33.xml";

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
            eadParser = new BasexEADParser(DataManager.getInstance().getConfiguration().getBaseXUrl(),
                    DataManager.getInstance().getConfiguration().getConfigLocalPath()
                            + CONFIG_FILE_NAME);

            // TODO configurable database name
            eadParser.setSelectedDatabase(database);
            eadParser.loadSelectedDatabase();
        } catch (ConfigurationException e) {
            logger.error(e.getMessage(), e);
        } catch (ClientProtocolException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } catch (HTTPException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 
     * @return
     */
    public EADTree getTectonicsTree() {
        // logger.trace("getTectonicsTree");
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

        //        tectonicsTree =  generateHierarchy();

        return tectonicsTree;
    }

    EADTree generateHierarchy() {
        logger.trace("getTectonicsTree");
        EADTree ret = new EADTree();
        ret.generate(eadParser.getRootElement());

        return ret;
    }

    /**
     * <p>
     * setChildrenVisible.
     * </p>
     *
     * @param element a {@link io.goobi.viewer.model.toc.TOCElement} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public void setChildrenVisible(EadEntry element)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (tectonicsTree == null) {
            return;
        }
        synchronized (tectonicsTree) {
            tectonicsTree.setChildVisible(element.getIndex());
            tectonicsTree.getActiveElement();
        }
    }

    /**
     * <p>
     * setChildrenInvisible.
     * </p>
     *
     * @param element a {@link io.goobi.viewer.model.toc.TOCElement} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public void setChildrenInvisible(EadEntry element)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (tectonicsTree == null) {
            return;
        }

        synchronized (tectonicsTree) {
            tectonicsTree.setChildInvisible(element.getIndex());
            tectonicsTree.getActiveElement();
        }
    }

    /**
     * @param identifier
     */
    public List<String> getTectonicsHierarchyForIdentifier(String identifier) {
        if (StringUtils.isEmpty(identifier)) {
            return Collections.emptyList();
        }

        if (eadParser == null) {
            logger.error("EAD parser not intialized");
            return Collections.emptyList();
        }

        eadParser.setSearchValue(identifier);
        eadParser.search();
        if (eadParser.getFlatEntryList().isEmpty()) {
            return Collections.emptyList();
        }

        if (eadParser.getFlatEntryList().size() == 1) {
            return Collections.singletonList(eadParser.getFlatEntryList().get(0).getLabel());
        }

        List<String> ret = new ArrayList<>(eadParser.getFlatEntryList().size() - 1);
        for (int i = 1; i < eadParser.getFlatEntryList().size(); ++i) {
            EadEntry entry = eadParser.getFlatEntryList().get(i);
            ret.add(entry.getLabel());
        }

        return ret;
    }
    
    public String searchAction() {
        logger.trace("searchAction: {}", searchString);
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
        this.searchString = searchString;
    }
}
