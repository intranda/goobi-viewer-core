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
package de.intranda.digiverso.presentation.model.viewer.pageloader;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.messages.ViewerResourceBundle;

public abstract class AbstractPageLoader implements IPageLoader {

    /**
     * Replaces the static variable placeholders (the ones that don't change depending on the page) of the given label format with values.
     * 
     * @param format
     * @param locale
     * @return
     * @throws IndexUnreachableException
     * @should replace numpages currectly
     * @should replace message keys correctly
     */
    protected String buildPageLabelTemplate(String format, Locale locale) throws IndexUnreachableException {
        if (format == null) {
            throw new IllegalArgumentException("format may not be null");
        }
        String labelTemplate = format.replace("{numpages}", String.valueOf(getNumPages()));
        Pattern p = Pattern.compile("\\{msg\\..*?\\}");
        Matcher m = p.matcher(labelTemplate);
        while (m.find()) {
            String key = labelTemplate.substring(m.start() + 5, m.end() - 1);
            labelTemplate = labelTemplate.replace(labelTemplate.substring(m.start(), m.end()), ViewerResourceBundle.getTranslation(key, locale));
        }
        return labelTemplate;
    }
}
