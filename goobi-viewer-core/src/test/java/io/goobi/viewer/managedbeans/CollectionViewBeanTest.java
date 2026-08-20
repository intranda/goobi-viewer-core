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
package io.goobi.viewer.managedbeans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.faces.validators.SolrQueryValidator;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.collections.DynamicCollection;
import io.goobi.viewer.model.cms.collections.DynamicCollectionTranslation;
import io.goobi.viewer.model.cms.pages.content.types.CMSCollectionContent;
import io.goobi.viewer.model.viewer.collections.CollectionView;
import io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement;
import io.goobi.viewer.solr.SolrConstants;

class CollectionViewBeanTest extends AbstractDatabaseAndSolrEnabledTest {

    /**
     * A collection-listing component configured with the DC_DYNAMIC pseudo field must render the database-defined dynamic collections as its entries,
     * each carrying the hit count of its stored query and its DB label.
     *
     * @see CollectionViewBean#initializeCollection(CMSCollectionContent, String)
     * @verifies build a flat collection view of all dynamic collections with query hit counts and db labels
     */
    @Test
    void initializeCollection_shouldRenderDynamicCollectionsAsEntries() throws Exception {
        String language = BeanUtils.getLocale() != null ? BeanUtils.getLocale().getLanguage() : Locale.ENGLISH.getLanguage();

        DynamicCollection dc1 = new DynamicCollection("cvb_dyncol_1");
        dc1.setSolrQuery("ISWORK:true");
        dc1.addLabel(new DynamicCollectionTranslation(language, "Dynamic collection one"));

        DynamicCollection dc2 = new DynamicCollection("cvb_dyncol_2");
        dc2.setSolrQuery("DOCSTRCT:monograph");
        dc2.addLabel(new DynamicCollectionTranslation(language, "Dynamic collection two"));

        DataManager.getInstance().getDao().addDynamicCollection(dc1);
        DataManager.getInstance().getDao().addDynamicCollection(dc2);
        try {
            long expectedCollectionCount = DataManager.getInstance()
                    .getDao()
                    .getAllDynamicCollections()
                    .stream()
                    .filter(dc -> dc.getSolrQuery() != null && !dc.getSolrQuery().isBlank())
                    .count();

            CMSCollectionContent content = new CMSCollectionContent();
            content.setSolrField(SolrConstants.DC_DYNAMIC);

            CollectionViewBean bean = new CollectionViewBean();
            CollectionView view = bean.getCollection(content);
            assertNotNull(view, "CollectionView should be created for the DC_DYNAMIC pseudo field");

            List<HierarchicalBrowseDcElement> elements = view.getVisibleDcElements();
            assertEquals(expectedCollectionCount, elements.size(), "Each dynamic collection with a query should be one flat entry");

            HierarchicalBrowseDcElement e1 = getElement(elements, "cvb_dyncol_1");
            HierarchicalBrowseDcElement e2 = getElement(elements, "cvb_dyncol_2");
            assertNotNull(e1, "Entry for cvb_dyncol_1 should exist");
            assertNotNull(e2, "Entry for cvb_dyncol_2 should exist");

            // Counts must match the hit count of the collection's stored query
            assertEquals(SolrQueryValidator.getHitCount("ISWORK:true"), e1.getNumberOfVolumes());
            assertEquals(SolrQueryValidator.getHitCount("DOCSTRCT:monograph"), e2.getNumberOfVolumes());
            assertTrue(e1.getNumberOfVolumes() > 0, "The dynamic collection should report a positive hit count");

            // Labels must come from the DB translation, not the identifier
            assertEquals("Dynamic collection one", e1.getLabel());
            assertEquals("Dynamic collection two", e2.getLabel());
        } finally {
            DataManager.getInstance().getDao().deleteDynamicCollection(dc1);
            DataManager.getInstance().getDao().deleteDynamicCollection(dc2);
        }
    }

    private static HierarchicalBrowseDcElement getElement(List<HierarchicalBrowseDcElement> elements, String name) {
        return elements.stream().filter(e -> name.equals(e.getName())).findFirst().orElse(null);
    }
}
