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
package io.goobi.viewer.model.urlresolution;

import java.net.URI;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ViewerPathTest {

    /**
     * @verifies append query string to combined URL when includeParams is true
     */
    @Test
    void getCombinedPrettyfiedUrl_shouldAppendQueryStringToCombinedURLWhenIncludeParamsIsTrue() {
        ViewerPath path = new ViewerPath("http://localhost:8080", "viewer", URI.create("search"), URI.create("PI:*/1/-/-/"));
        path.setQueryString("filterQuery=FOO:bar");
        Assertions.assertEquals("/search/PI:*/1/-/-/?filterQuery=FOO:bar", path.getCombinedPrettyfiedUrl(true));
    }

    /**
     * @verifies omit query string from combined URL when includeParams is false
     */
    @Test
    void getCombinedPrettyfiedUrl_shouldOmitQueryStringFromCombinedURLWhenIncludeParamsIsFalse() {
        ViewerPath path = new ViewerPath("http://localhost:8080", "viewer", URI.create("search"), URI.create("PI:*/1/-/-/"));
        path.setQueryString("filterQuery=FOO:bar");
        Assertions.assertEquals("/search/PI:*/1/-/-/", path.getCombinedPrettyfiedUrl(false));
    }
}
