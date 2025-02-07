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
package io.goobi.viewer.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.ReloadingFileBasedConfigurationBuilder;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.ex.ConversionException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Abstract configuration with base getters.
 */
public abstract class AbstractConfiguration {

    private static final Logger logger = LogManager.getLogger(AbstractConfiguration.class);

    protected ReloadingFileBasedConfigurationBuilder<XMLConfiguration> builder;
    protected ReloadingFileBasedConfigurationBuilder<XMLConfiguration> builderLocal;

    /**
     *
     * @return {@link XMLConfiguration} that is synced with the current state of the config file
     */
    protected XMLConfiguration getConfig() {
        try {
            return builder.getConfiguration();
        } catch (ConfigurationException e) {
            logger.error(e.getMessage());
        }

        return new XMLConfiguration();
    }

    /**
     *
     * @return {@link XMLConfiguration} that is synced with the current state of the config file
     */
    protected XMLConfiguration getConfigLocal() {
        if (builderLocal != null) {
            try {
                return builderLocal.getConfiguration();
            } catch (ConfigurationException e) {
                logger.trace(e.getMessage());
            }
        }

        return new XMLConfiguration();
    }

    /**
     * <p>
     * getLocalInt.
     * </p>
     *
     * @param inPath a {@link java.lang.String} object.
     * @param inDefault a int.
     * @return a int.
     */
    protected int getLocalInt(String inPath, int inDefault) {
        try {
            return getConfigLocal().getInt(inPath, getConfig().getInt(inPath, inDefault));
        } catch (ConversionException e) {
            logger.error("{}. Using default value {} instead.", e.getMessage(), inDefault);
            return inDefault;
        } catch (NullPointerException | IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            return inDefault;
        }
    }

    /**
     * <p>
     * getLocalFloat.
     * </p>
     *
     * @param inPath a {@link java.lang.String} object.
     * @return a float.
     */
    protected float getLocalFloat(String inPath) {
        return getConfigLocal().getFloat(inPath, getConfig().getFloat(inPath));
    }

    /**
     * <p>
     * getLocalFloat.
     * </p>
     *
     * @param inPath a {@link java.lang.String} object.
     * @param inDefault a float.
     * @return a float.
     */
    protected float getLocalFloat(String inPath, float inDefault) {
        try {
            return getConfigLocal().getFloat(inPath, getConfig().getFloat(inPath, inDefault));
        } catch (ConversionException e) {
            logger.error("{}. Using default value {} instead.", e.getMessage(), inDefault);
            return inDefault;
        } catch (NullPointerException | IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            return inDefault;
        }
    }

    /**
     * <p>
     * getLocalString.
     * </p>
     *
     * @param inPath a {@link java.lang.String} object.
     * @param inDefault a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    protected String getLocalString(String inPath, String inDefault) {
        try {
            return getConfigLocal().getString(inPath, getConfig().getString(inPath, inDefault));
        } catch (NullPointerException | IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            return inDefault;
        }
    }

    /**
     * <p>
     * getLocalString.
     * </p>
     *
     * @param inPath a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    protected String getLocalString(String inPath) {
        return getConfigLocal().getString(inPath, getConfig().getString(inPath));
    }

    /**
     * <p>
     * getLocalNodeList.
     * </p>
     *
     * @param inPath a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    protected List<Object> getLocalNodeList(String inPath) {
        List<Object> objects = ((HierarchicalConfiguration<ImmutableNode>) getConfigLocal()).getList(inPath,
                ((HierarchicalConfiguration<ImmutableNode>) getConfig()).getList(inPath));
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
     * <p>
     * getLocalList.
     * </p>
     *
     * @param config Preferred configuration
     * @param altConfig Alternative configuration
     * @param inPath XML path
     * @param defaultList List of default values to return if none found in config
     * @return a {@link java.util.List} object.
     */
    protected static List<String> getLocalList(HierarchicalConfiguration<ImmutableNode> config, HierarchicalConfiguration<ImmutableNode> altConfig,
            String inPath, List<String> defaultList) {
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
     * <p>
     * getLocalList.
     * </p>
     *
     * @param inPath a {@link java.lang.String} object.
     * @param defaultList a {@link java.util.List} object.
     * @return configured list; defaultList if none found
     */
    protected List<String> getLocalList(String inPath, List<String> defaultList) {
        return getLocalList(getConfigLocal(), getConfig(), inPath, defaultList);
    }

    /**
     * <p>
     * getLocalList.
     * </p>
     *
     * @param inPath a {@link java.lang.String} object.
     * @return configured list; empty list if none found
     */
    protected List<String> getLocalList(String inPath) {
        return getLocalList(inPath, Collections.emptyList());
    }

    /**
     * <p>
     * getLocalBoolean.
     * </p>
     *
     * @param inPath a {@link java.lang.String} object.
     * @param inDefault a boolean.
     * @return a boolean.
     */
    protected boolean getLocalBoolean(String inPath, boolean inDefault) {
        try {
            return getConfigLocal().getBoolean(inPath, getConfig().getBoolean(inPath, inDefault));
        } catch (NullPointerException | IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            return inDefault;
        }
    }

    /**
     *
     * @param config
     * @param altConfig
     * @param inPath
     * @return List&lt;HierarchicalConfiguration&lt;ImmutableNode&gt;&gt;
     */
    protected static List<HierarchicalConfiguration<ImmutableNode>> getLocalConfigurationsAt(HierarchicalConfiguration<ImmutableNode> config,
            HierarchicalConfiguration<ImmutableNode> altConfig, String inPath) {
        if (config == null) {
            throw new IllegalArgumentException("config may not be null");
        }

        List<HierarchicalConfiguration<ImmutableNode>> ret = config.configurationsAt(inPath);
        if ((ret == null || ret.isEmpty()) && altConfig != null) {
            ret = altConfig.configurationsAt(inPath);
        }

        return ret;
    }

    /**
     * <p>
     * getLocalConfigurationsAt.
     * </p>
     *
     * @param inPath a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    protected List<HierarchicalConfiguration<ImmutableNode>> getLocalConfigurationsAt(String inPath) {
        return getLocalConfigurationsAt(getConfigLocal(), getConfig(), inPath);
    }

    /**
     * <p>
     * getLocalConfigurationAt.
     * </p>
     *
     * @param inPath a {@link java.lang.String} object.
     * @return a {@link org.apache.commons.configuration2.HierarchicalConfiguration} object.
     */
    protected HierarchicalConfiguration<ImmutableNode> getLocalConfigurationAt(String inPath) {
        List<HierarchicalConfiguration<ImmutableNode>> ret = null;
        try {
            ret = getConfigLocal().configurationsAt(inPath);
            if (ret == null || ret.isEmpty()) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            ret = getConfig().configurationsAt(inPath);
        }

        if (ret != null && !ret.isEmpty()) {
            return ret.get(0);
        }

        return null;
    }

    /**
     * Overrides values in the config file (for unit test purposes).
     *
     * @param property Property path (e.g. "accessConditions.fullAccessForLocalhost")
     * @param value New value to set
     */
    public void overrideValue(String property, Object value) {
        getConfig().setProperty(property, value);
    }
}
