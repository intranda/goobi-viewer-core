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
package io.goobi.viewer.model.translations.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * Group item for core messages.
 */
public class CoreMessagesTranslationGroupItem extends TranslationGroupItem {

    /**
     * Protected constructor.
     *
     * @param key
     * @param regex
     */
    protected CoreMessagesTranslationGroupItem(String key, boolean regex) {
        super(key, regex);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.translations.admin.TranslationGroupItem#loadEntries()
     */
    @Override
    protected void loadEntries() {
        List<String> keys;
        if (regex) {
            keys = StringTools.filterStringsViaRegex(new ArrayList<>(ViewerResourceBundle.getAllKeys()), key);
        } else {
            keys = Collections.singletonList(key);
        }
        createMessageKeyStatusMap(keys);
    }
}
