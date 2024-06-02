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
package io.goobi.viewer.model.annotation.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;

/**
 * @author florian
 *
 */
class SqlAnnotationListerTest {

    IDAO dao;
    SqlAnnotationLister lister;

    /**
     * <p>setUp.</p>
     *
     * @throws java.lang.Exception if any.
     */
    @BeforeEach
    public void setUp() throws Exception {
        CrowdsourcingAnnotation a1 = createAnnotation(1l, "OPENACCESS", "Text 1", 10l, 100l, "describing", "PI01", 5);
        CrowdsourcingAnnotation a2 = createAnnotation(2l, "OPENACCESS", "Text 2", 10l, 101l, "describing", "PI02", 10);
        CrowdsourcingAnnotation a3 = createAnnotation(3l, "OPENACCESS", "Text 3", 11l, 101l, "commenting", "PI03", 5);
        CrowdsourcingAnnotation a4 = createAnnotation(4l, "OPENACCESS", "Text 4", 11l, 102l, "commenting", "PI03", 8);
        CrowdsourcingAnnotation a5 = createAnnotation(5l, "OPENACCESS", "Text 5", 12l, 102l, "annotating", "PI04", 1);
        CrowdsourcingAnnotation a6 = createAnnotation(6l, "RESTRICTED", "Text 6", 11l, 102l, "describing", "PI04", 5);
        CrowdsourcingAnnotation a7 = createAnnotation(7l, "RESTRICTED", "Text 7", 12l, 103l, "describing", "PI03", 5);
        dao = Mockito.mock(IDAO.class);
        Mockito.when(dao.getAllAnnotations(Mockito.anyString(), Mockito.anyBoolean()))
        .thenReturn(Arrays.asList(a1, a2, a3, a4, a5, a6, a7));

        lister = new SqlAnnotationLister(dao);
    }

    @Test
    void testGetAllAnnotations() {
        assertEquals(7, lister.getAllAnnotations().size());
    }

    @Test
    void testGetAnnotationsWithCondition() {
        assertEquals(1, lister.getAnnotationCount("Text 1", null, null, null, null, null));
        assertEquals(7, lister.getAnnotationCount("Text", null, null, null, null, null));
        assertEquals(2, lister.getAnnotationCount(null, Arrays.asList("commenting"), null, null, null, null));
        assertEquals(3, lister.getAnnotationCount(null, Arrays.asList("annotating", "commenting"), null, null, null, null));
        assertEquals(3, lister.getAnnotationCount(null, null, null, Arrays.asList(11l), null, null));
        assertEquals(2, lister.getAnnotationCount(null, null, null, null, "PI04", null));
        assertEquals(2, lister.getAnnotationCount(null, null, null, null, "PI03", 5));
        assertEquals(7, lister.getAnnotationCount(null, null, null, null, null, null));
    }

    @Test
    void testGetAnnotationPage() {
        assertEquals(5, lister.getAnnotations(0, 5, null, null, null, null, null, null, "", false).size());
        assertEquals(2, lister.getAnnotations(5, 5, null, null, null, null, null, null, "", false).size());
    }


    private CrowdsourcingAnnotation createAnnotation(long id, String accessCondition, String text, long creatorId, long reviewerId, String motivation, String pi, int page) {
        CrowdsourcingAnnotation anno = new CrowdsourcingAnnotation();
        anno.setId(id);
        anno.setAccessCondition(accessCondition);
        anno.setBody(text);
        anno.setCreatorId(creatorId);
        anno.setReviewerId(reviewerId);
        anno.setMotivation(motivation);
        anno.setTargetPI(pi);
        anno.setTargetPageOrder(page);
        return anno;
    }
}
