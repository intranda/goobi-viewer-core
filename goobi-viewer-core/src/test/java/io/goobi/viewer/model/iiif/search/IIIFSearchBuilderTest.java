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
package io.goobi.viewer.model.iiif.search;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import de.intranda.api.annotation.wa.Motivation;
import de.intranda.api.iiif.search.SearchResult;
import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.IPrivilegeHolder;

class IIIFSearchBuilderTest extends AbstractDatabaseAndSolrEnabledTest {

    /** Open-access record with ALTO files on disk and fulltext pages in the test Solr. */
    private static final String PI_WITH_ALTO = "PPN648829383";
    private static final String QUERY = "der";

    private final ApiUrls urls = new ApiUrls("");

    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
    }

    /**
     * @verifies not return fulltext hits when view fulltext access is denied
     */
    @Test
    void build_shouldNotReturnFulltextHitsWhenViewFulltextAccessDenied() throws Exception {
        // Baseline: with access granted the record must actually yield fulltext hits,
        // otherwise the security assertion below would be vacuous. Restricting the
        // motivation to painting isolates the fulltext branch (no metadata/comment hits).
        SearchResult granted = new IIIFSearchBuilder(urls, QUERY, PI_WITH_ALTO, null).setMotivation(Motivation.PAINTING).build();
        Assumptions.assumeTrue(granted.getResources() != null && !granted.getResources().isEmpty(),
                "Skipping: no fulltext search hits for " + PI_WITH_ALTO + " in the test Solr");

        // With VIEW_FULLTEXT denied the same search must not leak any fulltext annotation.
        try (MockedStatic<AccessConditionUtils> access =
                Mockito.mockStatic(AccessConditionUtils.class, invocation -> invocation.callRealMethod())) {
            access.when(() -> AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(
                    eq(PI_WITH_ALTO), isNull(), eq(IPrivilegeHolder.PRIV_VIEW_FULLTEXT), any()))
                    .thenReturn(AccessPermission.denied());

            SearchResult denied = new IIIFSearchBuilder(urls, QUERY, PI_WITH_ALTO, null).setMotivation(Motivation.PAINTING).build();
            Assertions.assertTrue(denied.getResources() == null || denied.getResources().isEmpty(),
                    "Fulltext search must not return hits when VIEW_FULLTEXT is denied");
        }
    }
}
