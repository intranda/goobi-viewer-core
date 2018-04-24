/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.intranda.digiverso.presentation.model.toc;

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.intranda.digiverso.presentation.AbstractDatabaseAndSolrEnabledTest;
import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

public class TocMakerTest extends AbstractDatabaseAndSolrEnabledTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();

        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
    }

    /**
     * @see TocMaker#getSolrFieldsToFetch()
     * @verifies return both static and configured fields
     */
    @Test
    public void getSolrFieldsToFetch_shouldReturnBothStaticAndConfiguredFields() throws Exception {
        List<?> fields = TocMaker.getSolrFieldsToFetch("_DEFAULT");
        Assert.assertNotNull(fields);
        Assert.assertEquals(19, fields.size());
    }

    /**
     * @see TocMaker#generateToc(TOC,StructElement,boolean,String,int,int)
     * @verifies generate volume TOC correctly with siblings correctly
     */
    @Test
    public void generateToc_shouldGenerateVolumeTOCCorrectlyWithSiblingsCorrectly() throws Exception {
        long iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("ZDB026544598_0001");
        Assert.assertTrue(iddoc > 0);
        StructElement structElement = new StructElement(iddoc);
        Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(new TOC(), structElement, true, PhysicalElement.MIME_TYPE_IMAGE, 1, -1);
        Assert.assertNotNull(tocElements);
        Assert.assertNotNull(tocElements.get(TOC.DEFAULT_GROUP));
        Assert.assertEquals(302, tocElements.get(TOC.DEFAULT_GROUP).size()); // 1 anchor + 290 elements of volume ZDB026544598_0001 + 11 sibling volume top elements
        // Anchor first
        Assert.assertEquals("ZDB026544598", tocElements.get(TOC.DEFAULT_GROUP).get(0).getTopStructPi());
        // First volume (complete tree)
        for (int i = 1; i <= 290; ++i) {
            TOCElement element = tocElements.get(TOC.DEFAULT_GROUP).get(i);
            Assert.assertEquals("ZDB026544598_0001", element.getTopStructPi());
        }
        // Sibling volumes (just topstruct)
        Assert.assertEquals("ZDB026544598_0002", tocElements.get(TOC.DEFAULT_GROUP).get(291).getTopStructPi());
        Assert.assertEquals("ZDB026544598_0003", tocElements.get(TOC.DEFAULT_GROUP).get(292).getTopStructPi());
        Assert.assertEquals("ZDB026544598_0004", tocElements.get(TOC.DEFAULT_GROUP).get(293).getTopStructPi());
        Assert.assertEquals("ZDB026544598_0005", tocElements.get(TOC.DEFAULT_GROUP).get(294).getTopStructPi());
        Assert.assertEquals("ZDB026544598_0006", tocElements.get(TOC.DEFAULT_GROUP).get(295).getTopStructPi());
        Assert.assertEquals("ZDB026544598_0007", tocElements.get(TOC.DEFAULT_GROUP).get(296).getTopStructPi());
        Assert.assertEquals("ZDB026544598_0008", tocElements.get(TOC.DEFAULT_GROUP).get(297).getTopStructPi());
        Assert.assertEquals("ZDB026544598_0009", tocElements.get(TOC.DEFAULT_GROUP).get(298).getTopStructPi());
        Assert.assertEquals("ZDB026544598_0010", tocElements.get(TOC.DEFAULT_GROUP).get(299).getTopStructPi());
        Assert.assertEquals("ZDB026544598_0011", tocElements.get(TOC.DEFAULT_GROUP).get(300).getTopStructPi());
        Assert.assertEquals("ZDB026544598_0012", tocElements.get(TOC.DEFAULT_GROUP).get(301).getTopStructPi());
    }

    /**
     * @see TocMaker#generateToc(TOC,StructElement,boolean,String,int,int)
     * @verifies generate volume TOC correctly without siblings correctly
     */
    @Test
    public void generateToc_shouldGenerateVolumeTOCCorrectlyWithoutSiblingsCorrectly() throws Exception {
        long iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("ZDB026544598_0001");
        Assert.assertTrue(iddoc > 0);
        StructElement structElement = new StructElement(iddoc);
        Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(new TOC(), structElement, false, PhysicalElement.MIME_TYPE_IMAGE, 1, -1);
        Assert.assertNotNull(tocElements);
        Assert.assertNotNull(tocElements.get(TOC.DEFAULT_GROUP));
        Assert.assertEquals(291, tocElements.get(TOC.DEFAULT_GROUP).size()); // 1 anchor + 290 elements of volume ZDB026544598_0001
        // Anchor first
        Assert.assertEquals("ZDB026544598", tocElements.get(TOC.DEFAULT_GROUP).get(0).getTopStructPi());
        // First volume (complete tree)
        for (int i = 1; i <= 290; ++i) {
            TOCElement element = tocElements.get(TOC.DEFAULT_GROUP).get(i);
            Assert.assertEquals("ZDB026544598_0001", element.getTopStructPi());
        }
    }

    /**
     * @see TocMaker#generateToc(TOC,StructElement,boolean,String,int)
     * @verifies generate anchor TOC correctly
     */
    @Test
    public void generateToc_shouldGenerateAnchorTOCCorrectly() throws Exception {
        long iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("ZDB026544598");
        Assert.assertTrue(iddoc > 0);
        StructElement structElement = new StructElement(iddoc);
        TOC toc = new TOC();
        Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(toc, structElement, true, PhysicalElement.MIME_TYPE_IMAGE, 1, -1);
        Assert.assertNotNull(tocElements);
        Assert.assertNotNull(tocElements.get(TOC.DEFAULT_GROUP));
        Assert.assertEquals(13, tocElements.get(TOC.DEFAULT_GROUP).size());
        Assert.assertEquals(12, toc.getTotalTocSize());
        Assert.assertEquals("ZDB026544598", tocElements.get(TOC.DEFAULT_GROUP).get(0).getTopStructPi());
        for (int i = 1; i < tocElements.get(TOC.DEFAULT_GROUP).size(); ++i) {
            Assert.assertEquals(i < 10 ? "ZDB026544598_000" + i : "ZDB026544598_00" + i, tocElements.get(TOC.DEFAULT_GROUP).get(i).getTopStructPi());
        }
    }

    /**
     * @see TocMaker#generateToc(TOC,StructElement,boolean,String,int,int)
     * @verifies paginate anchor TOC correctly
     */
    @Test
    public void generateToc_shouldPaginateAnchorTOCCorrectly() throws Exception {
        long iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("ZDB026544598");
        Assert.assertTrue(iddoc > 0);
        StructElement structElement = new StructElement(iddoc);
        TOC toc = new TOC();
        {
            // Page 1
            Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(toc, structElement, true, PhysicalElement.MIME_TYPE_IMAGE, 1, 5);
            Assert.assertNotNull(tocElements);
            Assert.assertNotNull(tocElements.get(TOC.DEFAULT_GROUP));
            Assert.assertEquals(6, tocElements.get(TOC.DEFAULT_GROUP).size());
            Assert.assertEquals(12, toc.getTotalTocSize());
            Assert.assertEquals("ZDB026544598", tocElements.get(TOC.DEFAULT_GROUP).get(0).getTopStructPi());
            for (int i = 1; i < tocElements.get(TOC.DEFAULT_GROUP).size(); ++i) {
                Assert.assertEquals("ZDB026544598_000" + i, tocElements.get(TOC.DEFAULT_GROUP).get(i).getTopStructPi());
            }
        }
        {
            // Page 2
            Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(toc, structElement, true, PhysicalElement.MIME_TYPE_IMAGE, 2, 5);
            Assert.assertNotNull(tocElements);
            Assert.assertNotNull(tocElements.get(TOC.DEFAULT_GROUP));
            Assert.assertEquals(6, tocElements.get(TOC.DEFAULT_GROUP).size());
            Assert.assertEquals(12, toc.getTotalTocSize());
            Assert.assertEquals("ZDB026544598", tocElements.get(TOC.DEFAULT_GROUP).get(0).getTopStructPi());
            for (int i = 1; i < tocElements.get(TOC.DEFAULT_GROUP).size(); ++i) {
                int volumeNumber = 5 + i;
                Assert.assertEquals(volumeNumber < 10 ? "ZDB026544598_000" + volumeNumber : "ZDB026544598_00" + volumeNumber,
                        tocElements.get(TOC.DEFAULT_GROUP).get(i).getTopStructPi());
            }
        }
        {
            // Page 3
            Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(toc, structElement, true, PhysicalElement.MIME_TYPE_IMAGE, 3, 5);
            Assert.assertNotNull(tocElements);
            Assert.assertNotNull(tocElements.get(TOC.DEFAULT_GROUP));
            Assert.assertEquals(3, tocElements.get(TOC.DEFAULT_GROUP).size());
            Assert.assertEquals(12, toc.getTotalTocSize());
            Assert.assertEquals("ZDB026544598", tocElements.get(TOC.DEFAULT_GROUP).get(0).getTopStructPi());
            for (int i = 1; i < tocElements.get(TOC.DEFAULT_GROUP).size(); ++i) {
                int volumeNumber = 10 + i;
                Assert.assertEquals("ZDB026544598_00" + volumeNumber, tocElements.get(TOC.DEFAULT_GROUP).get(i).getTopStructPi());
            }
        }
    }

    /**
     * @see TocMaker#generateToc(TOC,StructElement,boolean,String,int)
     * @verifies throw IllegalArgumentException if structElement is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void generateToc_shouldThrowIllegalArgumentExceptionIfStructElementIsNull() throws Exception {
        Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(new TOC(), null, true, PhysicalElement.MIME_TYPE_IMAGE, 1, -1);
    }

    /**
     * @see TocMaker#generateToc(TOC,StructElement,boolean,String,int)
     * @verifies throw IllegalArgumentException if toc is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void generateToc_shouldThrowIllegalArgumentExceptionIfTocIsNull() throws Exception {
        long iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("ZDB026544598_0001");
        Assert.assertTrue(iddoc > 0);
        Map<String, List<TOCElement>> tocElements =
                TocMaker.generateToc(null, new StructElement(iddoc), true, PhysicalElement.MIME_TYPE_IMAGE, 1, -1);
    }

    /**
     * @see TocMaker#buildLabel(SolrDocument)
     * @verifies build configured label correctly
     */
    @Test
    public void buildLabel_shouldBuildConfiguredLabelCorrectly() throws Exception {
        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.LABEL, "label");
            doc.setField("MD_CREATOR", "creator");
            Assert.assertEquals("label / creator", TocMaker.buildLabel(doc, null).getValue().orElse(""));
        }
        {
            SolrDocument doc = new SolrDocument();
            //            doc.setField(SolrConstants.DOCSTRCT, "PeriodicalVolume");
            doc.setField(SolrConstants.CURRENTNO, "1");
            doc.setField("MD_TITLE", "title");
            Assert.assertEquals("Number 1: title", TocMaker.buildLabel(doc, "PeriodicalVolume").getValue().orElse(""));
        }
    }
}