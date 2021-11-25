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
package io.goobi.viewer.faces.converters;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.search.SearchSortingOption;
import io.goobi.viewer.solr.SolrConstants;

/**
 * <p>
 * BookshelfConverter class.
 * </p>
 */
@Deprecated
@FacesConverter("searchSortingOptionConverter")
public class SearchSortingOptionConverter implements Converter<SearchSortingOption> {

    /** {@inheritDoc} */
    @Override
    public final SearchSortingOption getAsObject(final FacesContext context, final UIComponent component, final String value) {
        // System.out.println("getAsObject: " + value);
        if (value == null) {
            return null;
        }

        return new SearchSortingOption(value);
    }

    /** {@inheritDoc} */
    @Override
    public final String getAsString(final FacesContext context, final UIComponent component, final SearchSortingOption object) {
        if (object == null) {
            return null;
        }

        try {
            return String.valueOf(object.getSortString());
        } catch (NumberFormatException nfe) {
            return null;
        }
    }
}
