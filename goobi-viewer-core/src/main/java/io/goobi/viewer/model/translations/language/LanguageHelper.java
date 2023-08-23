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
package io.goobi.viewer.model.translations.language;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.ReloadingFileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.reloading.PeriodicReloadingTrigger;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * <p>
 * LanguageHelper class.
 * </p>
 */
public class LanguageHelper {

    private static final Logger logger = LogManager.getLogger(LanguageHelper.class);

    ReloadingFileBasedConfigurationBuilder<XMLConfiguration> builder;

    /**
     * <p>
     * Constructor for LanguageHelper.
     * </p>
     *
     * @param configFilePath a {@link java.lang.String} object.
     */
    public LanguageHelper(String configFilePath) {
        try {
            builder =
                    new ReloadingFileBasedConfigurationBuilder<XMLConfiguration>(XMLConfiguration.class)
                            .configure(new Parameters().properties()
                                    .setFileName(configFilePath)
                                    .setListDelimiterHandler(new DefaultListDelimiterHandler('&')) // TODO Why '&'?
                                    .setThrowExceptionOnMissing(false));
            builder.getConfiguration().setExpressionEngine(new XPathExpressionEngine());
            PeriodicReloadingTrigger trigger = new PeriodicReloadingTrigger(builder.getReloadingController(),
                    null, 10, TimeUnit.SECONDS);
            trigger.start();
        } catch (ConfigurationException e) {
            logger.error(e.getMessage());
        }
    }

    private XMLConfiguration getConfig() {
        try {
            return builder.getConfiguration();
        } catch (ConfigurationException e) {
            logger.error(e.getMessage());
            return new XMLConfiguration();
        }
    }

    public List<Language> getAllLanguages() {
        List<Language> languages = new ArrayList<>();
        List<HierarchicalConfiguration<ImmutableNode>> nodes = getConfig().configurationsAt("language");
        for (HierarchicalConfiguration<ImmutableNode> node : nodes) {
            String code = node.getString("iso_639-2");
            if (StringUtils.isNotBlank(code)) {
                Language language = createLanguage(node);
                if (!languages.contains(language)) {
                    languages.add(language);
                }
            }
        }
        return languages;
    }

    public List<Language> getMajorLanguages() {
        List<Language> languages = new ArrayList<>();
        List<HierarchicalConfiguration<ImmutableNode>> nodes = getConfig().configurationsAt("language");
        for (HierarchicalConfiguration<ImmutableNode> node : nodes) {
            String code = node.getString("iso_639-1", "").replaceAll("\\W+", "");
            if (!code.isEmpty()) {
                Language language = createLanguage(node);
                if (!languages.contains(language)) {
                    languages.add(language);
                }
            }
        }
        return languages;
    }

    /**
     * Gets the language data for the given iso-code 639-1 or 639-2B
     *
     * @param isoCode a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.translations.language.Language} object.
     */
    public Language getLanguage(String isoCode) {
        HierarchicalConfiguration<ImmutableNode> languageConfig = null;
        try {
            if (isoCode.length() == 3) {
                List<HierarchicalConfiguration<ImmutableNode>> nodes = getConfig().configurationsAt("language[iso_639-2=\"" + isoCode + "\"]");
                if (nodes.isEmpty()) {
                    nodes = getConfig().configurationsAt("language[iso_639-2T=\"" + isoCode + "\"]");
                }
                if (nodes.isEmpty()) {
                    nodes = getConfig().configurationsAt("language[iso_639-2B=\"" + isoCode + "\"]");
                }
                languageConfig = nodes.get(0);
            } else if (isoCode.length() == 2) {
                languageConfig = getConfig().configurationsAt("language[iso_639-1=\"" + isoCode + "\"]").get(0);
            }
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("No matching language found for " + isoCode);
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
        if (languageConfig == null) {
            throw new IllegalArgumentException("No matching language found for " + isoCode);
        }
        
        return createLanguage(languageConfig);
    }

    /**
     * @param languageConfig
     * @return
     */
    public Language createLanguage(HierarchicalConfiguration<ImmutableNode> languageConfig) {
        Language language = new Language();
        language.setIsoCode_639_2B(languageConfig.getString("iso_639-2", languageConfig.getString("iso_639-2B")));
        language.setIsoCode_639_2T(languageConfig.getString("iso_639-2T"));
        language.setIsoCode_639_1(languageConfig.getString("iso_639-1"));
        language.setEnglishName(languageConfig.getString("eng"));
        language.setGermanName(languageConfig.getString("ger"));
        language.setFrenchName(languageConfig.getString("fre"));
        return language;
    }

}
