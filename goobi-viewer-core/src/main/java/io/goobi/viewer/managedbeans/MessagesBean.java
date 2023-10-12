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
package io.goobi.viewer.managedbeans;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.messages.ViewerResourceBundle;

@Named
@RequestScoped
public class MessagesBean {

    public String createMessageKey(String... strings) {
        return Arrays.stream(strings).map(this::clean).collect(Collectors.joining("__"));
    }

    private String clean(String string) {
        if (StringUtils.isNotBlank(string)) {
            return string.toLowerCase().replaceAll("\\s", "_").replaceAll("[^\\w-]", "");
        }
        return "none";
    }

    private String translate(String value, Locale language) {
        return ViewerResourceBundle.getTranslation(value, language);
    }

    private IMetadataValue getTranslations(String value) {
        return ViewerResourceBundle.getTranslations(value);
    }

    public String cleanHtml(String html) {
        return Jsoup.clean(html, Safelist.relaxed());
    }
}
