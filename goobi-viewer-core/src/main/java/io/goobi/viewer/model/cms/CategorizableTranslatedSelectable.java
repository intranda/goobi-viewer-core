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
 * A {@link io.goobi.viewer.model.cms.TranslatedSelectable} which may also contain a list of {@link CMSCategory categories}
 *
 * @author florian
 * @param <T>
 */
public class CategorizableTranslatedSelectable<T> extends TranslatedSelectable<T> implements Serializable {

    private static final long serialVersionUID = 8238554814746815173L;

    private List<Selectable<CMSCategory>> categories;

    /**
     * <p>
     * Constructor for CategorizableTranslatedSelectable.
     * </p>
     *
     * @param value a T object.
     * @param selected a boolean.
     * @param defaultLocale a {@link java.util.Locale} object.
     * @param categories a {@link java.util.List} object.
     */
    public CategorizableTranslatedSelectable(T value, boolean selected, Locale defaultLocale, List<Selectable<CMSCategory>> categories) {
        super(value, selected, defaultLocale);
        this.categories = categories;
    }

    /**
     * <p>
     * Getter for the field <code>categories</code>.
     * </p>
     *
     * @return the categories
     */
    public List<Selectable<CMSCategory>> getCategories() {
        return categories;
    }

    /**
     * <p>
     * Setter for the field <code>categories</code>.
     * </p>
     *
     * @param categories a {@link java.util.List} object.
     */
    public void setCategories(List<Selectable<CMSCategory>> categories) {
        this.categories = categories;
    }

    /**
     * <p>
     * getSelectedCategories.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<CMSCategory> getSelectedCategories() {
        return categories.stream().filter(Selectable::isSelected).map(Selectable::getValue).collect(Collectors.toList());
    }

}
