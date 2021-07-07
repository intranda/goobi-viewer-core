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
package io.goobi.viewer.model.cms;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.managedbeans.AdminBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.admin.MessageEntry.TranslationStatus;
import io.goobi.viewer.model.translations.admin.TranslationGroup;
import io.goobi.viewer.model.translations.admin.TranslationGroupItem;

/**
 * Object representing tab status for a collection tree.
 */
public class CMSCollectionTreeTab implements IPolyglott {

    private static final Logger logger = LoggerFactory.getLogger(CMSCollectionTreeTab.class);

    private Map<Locale, TranslationStatus> translationStatusMap = new HashMap<>(getLocales().size());
    private Locale selectedLocale = BeanUtils.getLocale();

    public CMSCollectionTreeTab(String field) {
        List<TranslationGroup> groups = AdminBean.getTranslationGroupsForSolrFieldStatic(field);
        if (!groups.isEmpty() && !groups.get(0).getItems().isEmpty()) {
            for (Locale locale : getLocales()) {
                try {
                    for (TranslationGroupItem item : groups.get(0).getItems()) {
                        if (item.getKey().equals(field)) {
                            TranslationStatus translationStatus = item.getTranslationStatusLanguage(locale.getLanguage());
                            translationStatusMap.put(locale, translationStatus);
                            logger.trace("translation status {}: {}", locale, translationStatus);
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.translations.IPolyglott#isComplete(java.util.Locale)
     */
    @Override
    public boolean isComplete(Locale locale) {
        return TranslationStatus.FULL.equals(translationStatusMap.get(locale));
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.translations.IPolyglott#isValid(java.util.Locale)
     */
    @Override
    public boolean isValid(Locale locale) {
        return true;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.translations.IPolyglott#isEmpty(java.util.Locale)
     */
    @Override
    public boolean isEmpty(Locale locale) {
        return TranslationStatus.NONE.equals(translationStatusMap.get(locale));
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.translations.IPolyglott#getSelectedLocale()
     */
    @Override
    public Locale getSelectedLocale() {
        return selectedLocale;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.translations.IPolyglott#setSelectedLocale(java.util.Locale)
     */
    @Override
    public void setSelectedLocale(Locale locale) {
        this.selectedLocale = locale;
    }

}
