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
package io.goobi.viewer.model.iiif.presentation.v2.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import de.intranda.api.iiif.presentation.v2.Collection2;
import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.cms.collections.DynamicCollection;
import io.goobi.viewer.model.cms.collections.DynamicCollectionTranslation;
import io.goobi.viewer.solr.SolrConstants;

class CollectionBuilderDynamicTest extends AbstractDatabaseAndSolrEnabledTest {

    /**
     * The IIIF collection REST resource (which backs the client-side accordion collection widget) must return the database-defined dynamic
     * collections as members when the DC_DYNAMIC pseudo field is requested.
     *
     * @see CollectionBuilder#generateCollection(String, String, String, String, java.util.List)
     * @verifies build an iiif collection of dynamic collections for the dc dynamic field
     */
    @Test
    void generateCollection_shouldBuildIiifCollectionOfDynamicCollections() throws Exception {
        DynamicCollection dc1 = new DynamicCollection("cbd_dyncol_1");
        dc1.setSolrQuery("ISWORK:true");
        dc1.addLabel(new DynamicCollectionTranslation("en", "Dynamic IIIF one"));

        DynamicCollection dc2 = new DynamicCollection("cbd_dyncol_2");
        dc2.setSolrQuery("DOCSTRCT:monograph");
        dc2.addLabel(new DynamicCollectionTranslation("en", "Dynamic IIIF two"));

        DataManager.getInstance().getDao().addDynamicCollection(dc1);
        DataManager.getInstance().getDao().addDynamicCollection(dc2);
        try {
            long expectedCollectionCount = DataManager.getInstance()
                    .getDao()
                    .getAllDynamicCollections()
                    .stream()
                    .filter(dc -> dc.getSolrQuery() != null && !dc.getSolrQuery().isBlank())
                    .count();

            CollectionBuilder builder = new CollectionBuilder(new ApiUrls("http://localhost:8080/viewer/rest"));
            Collection2 collection = builder.generateCollection(SolrConstants.DC_DYNAMIC, null, null, ".", Collections.emptyList());
            assertNotNull(collection);

            List<Collection2> members = collection.getCollections();
            assertEquals(expectedCollectionCount, members.size(), "Each dynamic collection should be one IIIF member");

            List<String> internalNames = members.stream().map(Collection2::getInternalName).collect(Collectors.toList());
            assertTrue(internalNames.contains("cbd_dyncol_1"));
            assertTrue(internalNames.contains("cbd_dyncol_2"));

            Collection2 member1 = members.stream().filter(m -> "cbd_dyncol_1".equals(m.getInternalName())).findFirst().orElseThrow();
            assertNotNull(member1.getLabel());
            assertEquals("Dynamic IIIF one", member1.getLabel().getValue(Locale.ENGLISH).orElse(null),
                    "Member label should come from the DB translation");
        } finally {
            DataManager.getInstance().getDao().deleteDynamicCollection(dc1);
            DataManager.getInstance().getDao().deleteDynamicCollection(dc2);
        }
    }
}
