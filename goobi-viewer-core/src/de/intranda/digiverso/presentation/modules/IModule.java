/*************************************************************************
 * 
 * Copyright intranda GmbH
 * 
 * ************************* CONFIDENTIAL ********************************
 * 
 * [2003] - [2017] intranda GmbH, Bertha-von-Suttner-Str. 9, 37085 Göttingen, Germany 
 * 
 * All Rights Reserved.
 * 
 * NOTICE: All information contained herein is protected by copyright. 
 * The source code contained herein is proprietary of intranda GmbH. 
 * The dissemination, reproduction, distribution or modification of 
 * this source code, without prior written permission from intranda GmbH, 
 * is expressly forbidden and a violation of international copyright law.
 * 
 *************************************************************************/
package de.intranda.digiverso.presentation.modules;

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;

import de.intranda.digiverso.presentation.controller.AbstractConfiguration;

public interface IModule {

    public String getId();

    public String getName();

    public String getVersion();

    public boolean isLoaded();

    /**
     * Module configuration object.
     * 
     * @return
     */
    public AbstractConfiguration getConfiguration();

    /**
     * URLs for the CMS menu.
     * 
     * @return
     */
    public Map<String, String> getCmsMenuContributions();

    /**
     * URLs to sidebar widgets.
     * 
     * @return
     */
    public List<String> getSidebarContributions();

    /**
     * URLs to widgets containing admin menu links.
     * 
     * @return
     */
    public List<String> getAdminContributions();

    /**
     * Generic widget URLs than can be used from virtually anywhere. The URLs are configured in the config file.
     * 
     * @param type
     * @return
     */
    public List<String> getWidgets(String type);

    /**
     * Any additional tasks this module needs to perform when re-indexing a record (e.g. putting additional files into the hotfolder).
     * 
     * @param pi
     * @param dataRepository
     * @param namingScheme
     */
    public void augmentReIndexRecord(String pi, String dataRepository, String namingScheme) throws Exception;

    /**
     * Any additional tasks this module needs to perform when re-indexing a page (e.g. putting additional files into the hotfolder).
     * 
     * @param pi
     * @param page
     * @param doc
     * @param recordType
     * @param dataRepository
     * @param namingScheme
     * @return true if successful; false otherwise
     */
    public boolean augmentReIndexPage(String pi, int page, SolrDocument doc, String recordType, String dataRepository, String namingScheme)
            throws Exception;

    /**
     * Any clean-up the module might want to do when resetting the currently loaded record.
     * 
     * @return true if successful; false otherwise
     */
    public boolean augmentResetRecord();
}
