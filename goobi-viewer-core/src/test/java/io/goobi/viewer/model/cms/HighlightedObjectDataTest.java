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
package io.goobi.viewer.model.cms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.HighlightedObjectData.ImageMode;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.translations.TranslatedText;

public class HighlightedObjectDataTest extends AbstractDatabaseEnabledTest {

    private IDAO dao;

    @Before
    public void setup() throws Exception {
        super.setUp();
        this.dao = DataManager.getInstance().getDao();
    }

    @Test
    public void test_add() throws DAOException {
        HighlightedObjectData object = new HighlightedObjectData();
        object.setName(new TranslatedText("Test Name"));
        object.setDateStart(LocalDate.of(2023, 6, 12));
        object.setDateEnd(LocalDate.of(2023, 6, 24));
        CMSMediaItem mediaItem = dao.getCMSMediaItem(1);
        object.setMediaItem(mediaItem);
        object.setImageMode(ImageMode.UPLOADED_IMAGE);
        
        assertTrue(this.dao.addHighlightedObject(object));
        assertNotNull(object.getId());
        
        HighlightedObjectData loaded = this.dao.getHighlightedObject(object.getId());
        assertEquals("Test Name", loaded.getName().getText());
        assertEquals(mediaItem, loaded.getMediaItem());
        assertEquals(ImageMode.UPLOADED_IMAGE, object.getImageMode());
    }
    
    @Test
    public void test_update() throws DAOException {
        CMSMediaItem mediaItem = dao.getCMSMediaItem(1);
        HighlightedObjectData object = dao.getHighlightedObject(1l);
        assertEquals("Objekt des Monats Januar", object.getName().getText());
        assertEquals(null, object.getMediaItem());
        assertEquals(ImageMode.RECORD_REPRESENTATIVE, object.getImageMode());
        
        object.setName(new TranslatedText("New Name"));
        object.setDateStart(LocalDate.of(2023, 4, 12));
        object.setDateEnd(LocalDate.of(2023, 4, 24));
        object.setMediaItem(mediaItem);
        object.setImageMode(ImageMode.UPLOADED_IMAGE);
        
        assertTrue(this.dao.updateHighlightedObject(object));
        assertNotNull(object.getId());
        
        HighlightedObjectData loaded = this.dao.getHighlightedObject(object.getId());
        assertEquals("New Name", loaded.getName().getText());
        assertEquals(mediaItem, loaded.getMediaItem());
        assertEquals(ImageMode.UPLOADED_IMAGE, object.getImageMode());
    }
    
    @Test
    public void test_delete() throws DAOException {
        assertEquals(3, dao.getAllHighlightedObjects().size());
        dao.deleteHighlightedObject(1l);
        assertEquals(2, dao.getAllHighlightedObjects().size());
    }
    
    @Test
    public void test_getAll() throws DAOException {
        assertEquals(3, dao.getAllHighlightedObjects().size());
    }

    @Test
    public void test_getForDate() throws DAOException {
        LocalDateTime time = LocalDate.of(2023, 2, 15).atStartOfDay();
        assertEquals(1, dao.getHighlightedObjectsForDate(time).size());
        assertEquals("Objekt des Monats Februar", dao.getHighlightedObjectsForDate(time).get(0).getName().getText());
        
        LocalDateTime time2 = LocalDate.of(2023, 2, 1).atStartOfDay();
        assertEquals(2, dao.getHighlightedObjectsForDate(time2).size());
        
        LocalDateTime time3 = LocalDate.of(2023, 3, 15).atStartOfDay();
        assertEquals(0, dao.getHighlightedObjectsForDate(time3).size());
    }

    @Test
    public void test_EndDateOnly() throws DAOException {
        HighlightedObjectData object = new HighlightedObjectData();
        object.setDateEnd(LocalDate.of(2022, 5, 1));
        object.setDateStart(null);
        assertTrue(dao.addHighlightedObject(object));
        assertEquals(object, dao.getHighlightedObjectsForDate(LocalDate.of(2022,4,1).atStartOfDay()).get(0));
        assertEquals(0, dao.getHighlightedObjectsForDate(LocalDate.of(2022, 6, 1).atStartOfDay()).size());
    }
    
    @Test
    public void test_StartdDateOnly() throws DAOException {
        HighlightedObjectData object = new HighlightedObjectData();
        object.setDateStart(LocalDate.of(2022, 5, 1));
        object.setDateEnd(null);
        assertTrue(dao.addHighlightedObject(object));
        assertEquals(object, dao.getHighlightedObjectsForDate(LocalDate.of(2022,6,1).atStartOfDay()).get(0));
        assertEquals(0, dao.getHighlightedObjectsForDate(LocalDate.of(2022, 4, 1).atStartOfDay()).size());
    }
    
    @Test
    public void test_noDates() throws DAOException {
        HighlightedObjectData object = new HighlightedObjectData();
        object.setDateStart(null);
        object.setDateEnd(null);
        assertTrue(dao.addHighlightedObject(object));
        assertEquals(object, dao.getHighlightedObjectsForDate(LocalDate.of(1900,1,1).atStartOfDay()).get(0));
        assertEquals(object, dao.getHighlightedObjectsForDate(LocalDate.of(3000,1,1).atStartOfDay()).get(0));
    }

}
