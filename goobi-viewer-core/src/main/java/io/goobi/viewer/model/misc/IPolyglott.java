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
package io.goobi.viewer.model.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import io.goobi.viewer.managedbeans.utils.BeanUtils;

/**
 * Interface for objects containing translations for a set of languages. Used to construct tab panels to switch beween languages
 * 
 * @author florian
 *
 */
public interface IPolyglott {

    public boolean isComplete(Locale locale);

    public Locale getSelectedLocale();

    public void setSelectedLocale(Locale locale);

    public default void setSelectedLocale(String language) {
        Locale locale = Locale.forLanguageTag(language);
        if (locale != null) {
            this.setSelectedLocale(locale);
        } else {
            throw new IllegalArgumentException("'" + language + "' is not a valid language tag");
        }
    }

    public default boolean isDefaultLocaleSelected() {
        return getSelectedLocale() != null && getSelectedLocale().equals(getDefaultLocale());
    }

    public default boolean isSelected(Locale locale) {
        return locale != null && locale.equals(getSelectedLocale());
    }

    public default Collection<Locale> getLocales() {
        Iterator<Locale> i = BeanUtils.getNavigationHelper().getSupportedLocales();
        ArrayList<Locale> list = new ArrayList<>();
        i.forEachRemaining(list::add);
        return list;
    }

    public default Locale getDefaultLocale() {
        return BeanUtils.getNavigationHelper().getDefaultLocale();
    }

}
