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
package io.goobi.viewer.faces.converters;

import io.goobi.viewer.controller.HtmlSanitizer;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

/**
 * <p>
 * JSF converter that runs {@link HtmlSanitizer#cleanRichText(String)} on a submitted
 * input value before it reaches the model. Intended for {@code <h:inputTextarea>}
 * elements bound to TinyMCE-style rich-text editors, so persisted CMS content cannot
 * carry stored XSS payloads.
 * </p>
 *
 * <p>
 * {@link #getAsString(FacesContext, UIComponent, String)} returns the value unchanged
 * because the editor's display value should not be re-sanitized on every render — that
 * is the render-side {@code HtmlSanitizerBean}'s job, applied at the read sink.
 * </p>
 */
@FacesConverter("htmlSanitizer")
public class HtmlSanitizerConverter implements Converter<String> {

    /** {@inheritDoc} */
    @Override
    public String getAsObject(final FacesContext context, final UIComponent component, final String value) {
        return HtmlSanitizer.cleanRichText(value);
    }

    /** {@inheritDoc} */
    @Override
    public String getAsString(final FacesContext context, final UIComponent component, final String value) {
        return value;
    }
}
