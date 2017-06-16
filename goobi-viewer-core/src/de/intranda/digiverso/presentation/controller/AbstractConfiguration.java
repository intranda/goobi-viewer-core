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
package de.intranda.digiverso.presentation.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract configuration with base getters.
 */
public abstract class AbstractConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    XMLConfiguration config;
    XMLConfiguration configLocal;

    /**
     * 
     * @param inPath
     * @param inDefault
     * @return
     */
    int getLocalInt(String inPath, int inDefault) {
        try {
            return configLocal.getInt(inPath, config.getInt(inPath, inDefault));
        } catch (ConversionException e) {
            logger.error("{}. Using default value {} instead.", e.getMessage(), inDefault);
            return inDefault;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return inDefault;
        }
    }

    /**
     * 
     * @param inPath
     * @return
     */
    float getLocalFloat(String inPath) {
        return configLocal.getFloat(inPath, config.getFloat(inPath));
    }

    /**
     * 
     * @param inPath
     * @param inDefault
     * @return
     */
    String getLocalString(String inPath, String inDefault) {
        try {
            return configLocal.getString(inPath, config.getString(inPath, inDefault));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return inDefault;
        }
    }

    /**
     * 
     * @param inPath
     * @return
     */
    String getLocalString(String inPath) {
        return configLocal.getString(inPath, config.getString(inPath));
    }

    /**
     * 
     * @param config Preferred configuration
     * @param altConfig Alternative configuration
     * @param inPath XML path
     * @param defaultList List of default values to return if none found in config
     * @return
     */
    static List<String> getLocalList(HierarchicalConfiguration config, HierarchicalConfiguration altConfig, String inPath, List<String> defaultList) {
        if (config == null) {
            throw new IllegalArgumentException("config may not be null");
        }
        List<Object> objects = config.getList(inPath, altConfig == null ? null : altConfig.getList(inPath, defaultList));
        if (objects != null && !objects.isEmpty()) {
            List<String> ret = new ArrayList<>(objects.size());
            for (Object obj : objects) {
                ret.add((String) obj);
            }
            return ret;
        }

        return new ArrayList<>();
    }

    /**
     * 
     * @param inPath
     * @return
     */
    List<Object> getLocalNodeList(String inPath) {
        List<Object> objects = ((HierarchicalConfiguration) configLocal).getList(inPath, ((HierarchicalConfiguration) config).getList(inPath));
        if (objects != null && !objects.isEmpty()) {
            List<Object> ret = new ArrayList<>(objects.size());
            for (Object obj : objects) {
                ret.add(obj);
            }
            return ret;
        }

        return new ArrayList<>();
    }

    /**
     * 
     * @param inPath
     * @param defaultList
     * @return
     */
    List<String> getLocalList(String inPath, List<String> defaultList) {
        return getLocalList(configLocal, config, inPath, defaultList);
    }

    /**
     * 
     * @param inPath
     * @return
     */
    List<String> getLocalList(String inPath) {
        return getLocalList(inPath, null);
    }

    /**
     * 
     * @param inPath
     * @param inDefault
     * @return
     */
    boolean getLocalBoolean(String inPath, boolean inDefault) {
        try {
            return configLocal.getBoolean(inPath, config.getBoolean(inPath, inDefault));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return inDefault;
        }
    }

    /**
     * 
     * @param inPath
     * @return
     */
    List<HierarchicalConfiguration> getLocalConfigurationsAt(String inPath) {
        List<HierarchicalConfiguration> ret = configLocal.configurationsAt(inPath);
        if (ret == null || ret.isEmpty()) {
            ret = config.configurationsAt(inPath);
        }

        return ret;
    }
}
