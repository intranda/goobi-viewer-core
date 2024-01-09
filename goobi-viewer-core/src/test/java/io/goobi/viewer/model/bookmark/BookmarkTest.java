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
package io.goobi.viewer.model.bookmark;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.metadata.MetadataElement;

/**
 * @author florian
 *
 */
public class BookmarkTest extends AbstractDatabaseAndSolrEnabledTest {

    private final String TITLE = "Nobiltà pisana osservata";
    private final String PI = "74241";
    private final String LOGID = "LOG_0003";
    private final Integer PAGE = 10;

    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
    }

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testGetMetadataElement() throws IndexUnreachableException, PresentationException {
        Bookmark bookmarkWork = new Bookmark(PI, null, TITLE);
        Bookmark bookmarkChapter = new Bookmark(PI, LOGID, TITLE);
        Bookmark bookmarkPage = new Bookmark(PI, null, PAGE);

        MetadataElement mdWork = bookmarkWork.getMetadataElement();
        Assertions.assertNotNull(mdWork);

        MetadataElement mdChapter = bookmarkChapter.getMetadataElement();
        Assertions.assertNotNull(mdChapter);

        MetadataElement mdPage = bookmarkPage.getMetadataElement();
        Assertions.assertNotNull(mdPage);
    }

}
