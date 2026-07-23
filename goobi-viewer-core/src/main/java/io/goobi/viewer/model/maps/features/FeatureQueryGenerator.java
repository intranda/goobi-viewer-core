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
package io.goobi.viewer.model.maps.features;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.model.metadata.MetadataContainer;

/**
 * Generates Solr search filter queries for geo map features based on coordinate and label metadata extracted from a given
 * {@link io.goobi.viewer.model.metadata.MetadataContainer}.
 */
public class FeatureQueryGenerator {

    public String createSearchFilterQuery(MetadataContainer metadata, LabelCreator labelCreator) {

        String longLat = metadata.getFirstValue("WKT_COORDS");
        if (StringUtils.isNotBlank(longLat)) {
            String query = StringTools.encodeUrl(createSearchTerm(metadata, labelCreator));
            String locationQuery = "WKT_COORDS:\"Intersects(POINT(%s)) distErrPct=0\"".formatted(longLat);
            if (StringUtils.isNotBlank(query)) {
                return StringTools.encodeUrl(query);
            } else {
                return StringTools.encodeUrl(locationQuery);
            }
        }
        return "";
    }

    private String createSearchTerm(MetadataContainer metadata, LabelCreator labelCreator) {

        if (StringUtils.isBlank(labelCreator.getFilterQueryField()) || !metadata.containsField(labelCreator.getFilterQueryField())) {
            return "";
        }

        return labelCreator.getFilterQueryField() + ":\"" + metadata.getFirstValue(labelCreator.getFilterQueryField()) + "\"";

    }

}
