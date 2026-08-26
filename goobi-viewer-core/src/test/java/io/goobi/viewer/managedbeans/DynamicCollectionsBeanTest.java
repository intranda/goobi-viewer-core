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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.CmsCollectionsBean.CMSCollectionImageMode;
import io.goobi.viewer.model.cms.collections.DynamicCollection;
import jakarta.faces.validator.ValidatorException;

class DynamicCollectionsBeanTest extends AbstractDatabaseEnabledTest {

    /**
     * @verifies persist a new collection and update it on subsequent save
     */
    @Test
    void saveCurrentCollection_shouldCreateAndUpdate() throws DAOException {
        DynamicCollectionsBean bean = new DynamicCollectionsBean();
        bean.createNewCollection();
        bean.getCurrentCollection().setIdentifier("bean_create_test");
        bean.getCurrentCollection().setSolrQuery("DOCSTRCT:monograph");
        assertEquals("pretty:adminCmsCollections", bean.saveCurrentCollection());

        DynamicCollection persisted = DataManager.getInstance().getDao().getDynamicCollection("bean_create_test");
        assertNotNull(persisted);
        assertEquals("DOCSTRCT:monograph", persisted.getSolrQuery());

        bean.setCollectionName("bean_create_test");
        assertFalse(bean.isDirty());
        bean.getCurrentCollection().setSolrQuery("DOCSTRCT:manuscript");
        assertTrue(bean.isDirty());
        bean.saveCurrentCollection();
        assertEquals("DOCSTRCT:manuscript", DataManager.getInstance().getDao().getDynamicCollection("bean_create_test").getSolrQuery());

        DataManager.getInstance().getDao().deleteDynamicCollection(persisted);
    }

    /**
     * @verifies clear image fields not matching the selected image mode on save
     */
    @Test
    void saveCurrentCollection_shouldClearUnusedImageFields() throws DAOException {
        DynamicCollectionsBean bean = new DynamicCollectionsBean();
        bean.createNewCollection();
        bean.getCurrentCollection().setIdentifier("bean_image_test");
        bean.getCurrentCollection().setRepresentativeWorkPI("PPN123");
        bean.setImageMode(CMSCollectionImageMode.NONE);
        bean.saveCurrentCollection();

        DynamicCollection persisted = DataManager.getInstance().getDao().getDynamicCollection("bean_image_test");
        assertNotNull(persisted);
        assertTrue(persisted.getRepresentativeWorkPI() == null || persisted.getRepresentativeWorkPI().isEmpty());

        DataManager.getInstance().getDao().deleteDynamicCollection(persisted);
    }

    /**
     * @verifies reject identifiers with characters outside the URL-safe set
     */
    @Test
    void validateIdentifier_shouldRejectInvalidCharacters() {
        DynamicCollectionsBean bean = new DynamicCollectionsBean();
        bean.setCurrentCollection(new DynamicCollection());
        assertThrows(ValidatorException.class, () -> bean.validateIdentifier(null, null, "foo:bar"));
        assertThrows(ValidatorException.class, () -> bean.validateIdentifier(null, null, "foo;bar"));
        assertThrows(ValidatorException.class, () -> bean.validateIdentifier(null, null, "foo bar"));
        assertThrows(ValidatorException.class, () -> bean.validateIdentifier(null, null, "  "));
        assertThrows(ValidatorException.class, () -> bean.validateIdentifier(null, null, "-"));
    }

    /**
     * @verifies reject a duplicate identifier for a different collection
     */
    @Test
    void validateIdentifier_shouldRejectDuplicate() throws DAOException {
        DynamicCollection existing = new DynamicCollection("bean_dup_test");
        existing.setSolrQuery("*:*");
        DataManager.getInstance().getDao().addDynamicCollection(existing);
        try {
            DynamicCollectionsBean bean = new DynamicCollectionsBean();
            bean.setCurrentCollection(new DynamicCollection());
            assertThrows(ValidatorException.class, () -> bean.validateIdentifier(null, null, "bean_dup_test"));
        } finally {
            DataManager.getInstance().getDao().deleteDynamicCollection(existing);
        }
    }

    /**
     * @verifies treat a blank PI as valid
     */
    @Test
    void validatePI_shouldTreatBlankAsValid() throws Exception {
        assertTrue(DynamicCollectionsBean.validatePI(""));
        assertTrue(DynamicCollectionsBean.validatePI(null));
    }

    /**
     * @verifies not resolve a non-existent dynamic collection
     */
    @Test
    void getDynamicCollection_shouldReturnNullForUnknownName() throws DAOException {
        assertNull(DataManager.getInstance().getDao().getDynamicCollection("does_not_exist_xyz"));
    }

    /**
     * @verifies delete the current collection and navigate to the overview
     */
    @Test
    void deleteCurrentCollection_shouldDeleteAndNavigate() throws DAOException {
        DynamicCollectionsBean bean = new DynamicCollectionsBean();
        bean.createNewCollection();
        bean.getCurrentCollection().setIdentifier("bean_delete_test");
        bean.saveCurrentCollection();
        assertNotNull(DataManager.getInstance().getDao().getDynamicCollection("bean_delete_test"));

        bean.setCollectionName("bean_delete_test");
        assertEquals("pretty:adminCmsCollections", bean.deleteCurrentCollection());
        assertNull(DataManager.getInstance().getDao().getDynamicCollection("bean_delete_test"));
    }

    /**
     * @verifies compute the query hit count when loading an existing collection
     */
    @Test
    void setCollectionName_shouldComputeQueryHitCount() throws DAOException {
        DynamicCollection existing = new DynamicCollection("bean_hitcount_test");
        existing.setSolrQuery("*:*");
        DataManager.getInstance().getDao().addDynamicCollection(existing);
        try {
            DynamicCollectionsBean bean = new DynamicCollectionsBean();
            bean.setCollectionName("bean_hitcount_test");
            assertNotNull(bean.getQueryHitCount());
            assertTrue(bean.getQueryHitCount() > 0);
        } finally {
            DataManager.getInstance().getDao().deleteDynamicCollection(existing);
        }
    }
}
