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
package de.intranda.digiverso.presentation.controller.language;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.Configuration;

public class LanguageHelper {

    private static final Logger logger = LoggerFactory.getLogger(LanguageHelper.class);

    private XMLConfiguration config;

    public LanguageHelper(String configFilePath) {
        try {
            config = new XMLConfiguration(configFilePath);
        } catch (ConfigurationException e) {
            logger.error("ConfigurationException", e);
            config = new XMLConfiguration();
        }
        config.setListDelimiter('&');
        config.setReloadingStrategy(new FileChangedReloadingStrategy());
        config.setExpressionEngine(new XPathExpressionEngine());
    }

    /**
     * Gets the language data for the given iso-code 639-1 or 639-2B
     * 
     * @param isoCode
     * @return
     */
    public Language getLanguage(String isoCode) {
        SubnodeConfiguration languageConfig = null;
        try {
            if (isoCode.length() == 3) {
                languageConfig = (SubnodeConfiguration) config.configurationsAt("language[iso_639-2=\"" + isoCode + "\"]")
                        .get(0);
            } else if (isoCode.length() == 2) {
                languageConfig = (SubnodeConfiguration) config.configurationsAt("language[iso_639-1=\"" + isoCode + "\"]")
                        .get(0);
            }
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("No matching language found for " + isoCode);
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
        if (languageConfig == null) {
            throw new IllegalArgumentException("No matching language found for " + isoCode);
        }
        Language language = new Language();
        language.setIsoCode(languageConfig.getString("iso_639-2"));
        language.setIsoCodeOld(languageConfig.getString("iso_639-1"));
        language.setEnglishName(languageConfig.getString("eng"));
        language.setGermanName(languageConfig.getString("ger"));
        language.setFrenchName(languageConfig.getString("fre"));

        return language;
    }

}
