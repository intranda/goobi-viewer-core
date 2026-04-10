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
package io.goobi.viewer.model.search;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.StringTools;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Parses the {@code filterQuery} parameter from an HTTP request's query string and returns it as a decoded Solr filter query string.
 */
public class FilterQueryParser {

    private static final String FILTER_QUERY_REGEX = "[?&]?filterQuery=([^&]+)";

    public Optional<String> getFilterQuery(HttpServletRequest request) {
        if (request != null && StringUtils.isNotBlank(request.getQueryString())) {
            Matcher matcher = Pattern.compile(FILTER_QUERY_REGEX).matcher(request.getQueryString());
            if (matcher.find()) {
                return Optional.of(StringTools.decodeUrl(matcher.group(1)));
            }
        }
        return Optional.empty();
    }

}
