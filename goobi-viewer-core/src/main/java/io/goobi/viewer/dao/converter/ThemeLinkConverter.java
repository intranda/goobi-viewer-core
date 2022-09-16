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
package io.goobi.viewer.dao.converter;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.json.JSONObject;

import io.goobi.viewer.model.viewer.themes.ThemeLink;
import io.goobi.viewer.model.viewer.themes.ThemeLink.InternalService;
import io.goobi.viewer.model.viewer.themes.ThemeLink.Service;
import io.goobi.viewer.model.viewer.themes.ThemeLink.SocialMediaService;

/**
 * @author florian
 *
 */
@Converter
public class ThemeLinkConverter implements AttributeConverter<List<ThemeLink>, String> {

    /* (non-Javadoc)
     * @see jakarta.persistence.AttributeConverter#convertToDatabaseColumn(java.lang.Object)
     */
    @Override
    public String convertToDatabaseColumn(List<ThemeLink> attribute) {
        JSONObject json = new JSONObject();
        for (ThemeLink link : attribute) {
            json.put(link.getService().getInternalName(), link.getLinkUrl());
        }
        return json.toString();
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.AttributeConverter#convertToEntityAttribute(java.lang.Object)
     */
    @Override
    public List<ThemeLink> convertToEntityAttribute(String dbData) {
        JSONObject json = new JSONObject(dbData);
        List<ThemeLink> links = new ArrayList<>();
        for (String key : json.keySet()) {
            Service service = null;
            try {
                service = SocialMediaService.valueOf(key);
            } catch (IllegalArgumentException e) {
                service = InternalService.valueOf(key);
            }
            if (service != null) {
                String url = json.getString(key);
                ThemeLink link = new ThemeLink(service);
                link.setLinkUrl(url);
                links.add(link);
            }
        }
        return links;
    }

}
