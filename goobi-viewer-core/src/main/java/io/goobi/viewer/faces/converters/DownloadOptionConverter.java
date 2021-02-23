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

import io.goobi.viewer.model.download.DownloadOption;

@FacesConverter("downloadOptionConverter")
@Deprecated
public class DownloadOptionConverter implements Converter<DownloadOption> {

    @Override
    public DownloadOption getAsObject(FacesContext context, UIComponent component, String submittedValue) {
        System.out.println("submittedValue: " + submittedValue);
        if (submittedValue == null || submittedValue.isEmpty()) {
            return null;
        }

        DownloadOption ret = DownloadOption.getByLabel(submittedValue);

        return ret;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, DownloadOption option) {
        if (option == null) {
            return "";
        }

        System.out.println("option: " + option);

        return option.getLabel();

    }
}