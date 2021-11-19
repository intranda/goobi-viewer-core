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
package io.goobi.viewer.dao.converter;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.AttributeConverter;

import org.json.JSONObject;

import io.goobi.viewer.model.viewer.themes.FooterLink;
import io.goobi.viewer.model.viewer.themes.SocialMediaLink;

/**
 * @author florian
 *
 */
public class FooterLinkConverter implements AttributeConverter<List<FooterLink>, String> {

    /* (non-Javadoc)
     * @see javax.persistence.AttributeConverter#convertToDatabaseColumn(java.lang.Object)
     */
    @Override
    public String convertToDatabaseColumn(List<FooterLink> attribute) {
        JSONObject json = new JSONObject();
        for (FooterLink link : attribute) {
            json.put(link.getService().name(), link.getLinkUrl());
        }
        return json.toString();
    }

    /* (non-Javadoc)
     * @see javax.persistence.AttributeConverter#convertToEntityAttribute(java.lang.Object)
     */
    @Override
    public List<FooterLink> convertToEntityAttribute(String dbData) {
        JSONObject json = new JSONObject(dbData);
        List<FooterLink> links = new ArrayList<>();
        for (String key : json.keySet()) {
            FooterLink.Service service = FooterLink.Service.valueOf(key);
            if(service != null) {
                String url = json.getString(key);
                FooterLink link = new FooterLink(service);
                link.setLinkUrl(url);
                links.add(link);
            }
        }
        return links;
    }

}
