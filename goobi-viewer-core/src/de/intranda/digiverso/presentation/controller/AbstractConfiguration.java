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

    protected XMLConfiguration config;
    protected XMLConfiguration configLocal;

    /**
     * 
     * @param inPath
     * @param inDefault
     * @return
     */
    protected int getLocalInt(String inPath, int inDefault) {
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
    protected float getLocalFloat(String inPath) {
        return configLocal.getFloat(inPath, config.getFloat(inPath));
    }

    /**
     * 
     * @param inPath
     * @param inDefault
     * @return
     */
    protected String getLocalString(String inPath, String inDefault) {
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
    protected String getLocalString(String inPath) {
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
    protected static List<String> getLocalList(HierarchicalConfiguration config, HierarchicalConfiguration altConfig, String inPath, List<String> defaultList) {
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
    protected List<Object> getLocalNodeList(String inPath) {
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
    protected List<String> getLocalList(String inPath, List<String> defaultList) {
        return getLocalList(configLocal, config, inPath, defaultList);
    }

    /**
     * 
     * @param inPath
     * @return
     */
    protected List<String> getLocalList(String inPath) {
        return getLocalList(inPath, null);
    }

    /**
     * 
     * @param inPath
     * @param inDefault
     * @return
     */
    protected boolean getLocalBoolean(String inPath, boolean inDefault) {
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
    protected List<HierarchicalConfiguration> getLocalConfigurationsAt(String inPath) {
        List<HierarchicalConfiguration> ret = configLocal.configurationsAt(inPath);
        if (ret == null || ret.isEmpty()) {
            ret = config.configurationsAt(inPath);
        }

        return ret;
    }

    /**
     * 
     * @param inPath
     * @return
     */
    protected HierarchicalConfiguration getLocalConfigurationAt(String inPath) {
        HierarchicalConfiguration ret = null;
        try {
            ret = configLocal.configurationAt(inPath);
            if (ret == null || ret.isEmpty()) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            ret = config.configurationAt(inPath);
        }

        return ret;
    }
}
