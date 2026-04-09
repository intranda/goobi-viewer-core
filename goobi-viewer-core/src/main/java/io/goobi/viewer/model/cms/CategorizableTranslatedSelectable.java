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
package io.goobi.viewer.model.cms;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * A {@link io.goobi.viewer.model.cms.TranslatedSelectable} which may also contain a list of {@link CMSCategory categories}.
 *
 * @author Florian Alpers
 * @param <T>
 */
public class CategorizableTranslatedSelectable<T> extends TranslatedSelectable<T> implements Serializable {

    private static final long serialVersionUID = 8238554814746815173L;

    private List<Selectable<CMSCategory>> categories;

    /**
     * Creates a new CategorizableTranslatedSelectable instance.
     *
     * @param value wrapped item value
     * @param selected initial selection state
     * @param defaultLocale locale used for translation display
     * @param categories selectable CMS categories associated with this item
     */
    public CategorizableTranslatedSelectable(T value, boolean selected, Locale defaultLocale, List<Selectable<CMSCategory>> categories) {
        super(value, selected, defaultLocale);
        this.categories = categories;
    }

    /**
     * Getter for the field <code>categories</code>.
     *
     * @return the list of selectable CMS categories associated with this item
     */
    public List<Selectable<CMSCategory>> getCategories() {
        return categories;
    }

    /**
     * Setter for the field <code>categories</code>.
     *
     * @param categories selectable CMS categories to assign
     */
    public void setCategories(List<Selectable<CMSCategory>> categories) {
        this.categories = categories;
    }

    /**
     * getSelectedCategories.
     *
     * @return a list of CMS categories that are currently selected
     */
    public List<CMSCategory> getSelectedCategories() {
        return categories.stream().filter(Selectable::isSelected).map(Selectable::getValue).collect(Collectors.toList());
    }

}
