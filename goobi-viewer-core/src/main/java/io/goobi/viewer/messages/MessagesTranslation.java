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
package io.goobi.viewer.messages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.model.translations.Translation;

/**
 * <p>
 * MessagesTranslation class.
 * </p>
 *
 * @author florian
 */
public class MessagesTranslation extends Translation {

    /**
     * <p>
     * Constructor for MessagesTranslation.
     * </p>
     *
     * @param key a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     * @param language a {@link java.lang.String} object.
     */
    public MessagesTranslation(String key, String value, String language) {
        super(language, key, value);
        this.id = 0L; //just to prevent nullpointer
    }

    /**
     * <p>
     * getTranslations.
     * </p>
     *
     * @param key a {@link java.lang.String} object.
     * @return a {@link java.util.Collection} object.
     */
    public static Collection<Translation> getTranslations(String key) {
        Collection<Translation> translations = new ArrayList<>();
        for (Locale locale : ViewerResourceBundle.getAllLocales()) {
            String value = ViewerResourceBundle.getTranslation(key, locale, true);
            if (StringUtils.isNotBlank(value)) {
                MessagesTranslation translation = new MessagesTranslation(key, value, locale.getLanguage());
                translations.add(translation);
            }
        }
        return translations;
    }

}
