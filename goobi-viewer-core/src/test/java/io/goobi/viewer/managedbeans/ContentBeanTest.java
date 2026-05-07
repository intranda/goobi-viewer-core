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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.crowdsourcing.DisplayUserGeneratedContent;

/**
 * @author florian
 *
 */
class ContentBeanTest extends AbstractDatabaseEnabledTest {

    private static final String PI = "PI_1";

    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    //Needs annotations in test system
    /**
     * @verifies load all annotations
     * @see ContentBean#getUserGeneratedContentsForDisplay
     */
    @Test
    void getUserGeneratedContentsForDisplay_shouldLoadAllAnnotations() throws PresentationException, IndexUnreachableException, DAOException {
        ContentBean bean = new ContentBean();
        List<DisplayUserGeneratedContent> ugcList = bean.getUserGeneratedContentsForDisplay(PI);
        assertEquals(2, ugcList.size());
    }

    /**
     * @verifies return unmodifiable list
     * @see ContentBean#getUserGeneratedContentsForDisplay
     */
    @Test
    void getUserGeneratedContentsForDisplay_shouldReturnUnmodifiableList() throws PresentationException, IndexUnreachableException, DAOException {
        ContentBean bean = new ContentBean();
        List<DisplayUserGeneratedContent> ugcList = bean.getUserGeneratedContentsForDisplay(PI);
        assertThrows(UnsupportedOperationException.class, () -> ugcList.add(null));
    }

    /**
     * @see ContentBean#cleanUpValue(String)
     * @verifies remove script tags
     */
    @Test
    void cleanUpValue_shouldRemoveScriptTags() {
        ContentBean bean = new ContentBean();
        String result = bean.cleanUpValue("hello<script>alert(1)</script>world");
        assertFalse(result.toLowerCase().contains("<script"));
        assertTrue(result.contains("hello"));
        assertTrue(result.contains("world"));
    }

    /**
     * @see ContentBean#cleanUpValue(String)
     * @verifies remove img onerror attribute
     */
    @Test
    void cleanUpValue_shouldRemoveImgOnerrorAttribute() {
        ContentBean bean = new ContentBean();
        String result = bean.cleanUpValue("<img src=\"x\" onerror=\"alert(1)\">");
        assertFalse(result.toLowerCase().contains("onerror"));
    }

}
