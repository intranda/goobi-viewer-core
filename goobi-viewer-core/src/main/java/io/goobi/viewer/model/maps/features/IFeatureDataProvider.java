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

import java.util.List;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;

public interface IFeatureDataProvider {

    static final List<String> REQUIRED_FIELDS =
            List.of("IDDOC*", "PI*", "GROUPFIELD", "DOCTYPE", "LOGID", "DOCSTRCT*", "LABEL", "ISWORK", "METADATATYPE",
                    "MD_VALUE", "MD_REFID", "MD_PROCESSID", "MD_IDENTIFIER", "THUMBPAGENO", "ORDER", "MIMETYPE", "THUMBNAIL", "BOOL_WKT_COORDS",
                    "WKT_COORDS");

    public List<MetadataDocument> getResults(String query, int maxResults) throws PresentationException, IndexUnreachableException;

}