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
package io.goobi.viewer.model.toc;

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.model.toc.TOC;
import io.goobi.viewer.model.toc.TOCElement;
import io.goobi.viewer.model.toc.TocMaker;
import io.goobi.viewer.model.viewer.MimeType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;

public class TocMakerTest extends AbstractDatabaseAndSolrEnabledTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();

        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("src/test/resources/config_viewer.test.xml"));
    }

    /**
     * @see TocMaker#getSolrFieldsToFetch()
     * @verifies return both static and configured fields
     */
    @Test
    public void getSolrFieldsToFetch_shouldReturnBothStaticAndConfiguredFields() throws Exception {
        List<?> fields = TocMaker.getSolrFieldsToFetch("_DEFAULT");
        Assert.assertNotNull(fields);
        Assert.assertEquals(33, fields.size()); //The fields configured in getTocLabelConfiguration() are counted twice, once  suffixed with _LANG_...
    }

    /**
     * @see TocMaker#generateToc(TOC,StructElement,boolean,String,int,int)
     * @verifies generate volume TOC with siblings correctly
     */
    @Test
    public void generateToc_shouldGenerateVolumeTOCWithSiblingsCorrectly() throws Exception {
        long iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648_1891");
        Assert.assertTrue(iddoc > 0);
        StructElement structElement = new StructElement(iddoc);
        Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(new TOC(), structElement, true, MimeType.IMAGE.getName(), 1, -1);
        Assert.assertNotNull(tocElements);
        Assert.assertNotNull(tocElements.get(TOC.DEFAULT_GROUP));
        Assert.assertEquals(111, tocElements.get(TOC.DEFAULT_GROUP).size()); // 1 anchor + 104 elements of volume 306653648_1891 + 6 sibling volume top elements
        // Anchor first
        Assert.assertEquals("306653648", tocElements.get(TOC.DEFAULT_GROUP).get(0).getTopStructPi());
        // First volume (complete tree)
        for (int i = 1; i <= 104; ++i) {
            TOCElement element = tocElements.get(TOC.DEFAULT_GROUP).get(i);
            Assert.assertEquals("306653648_1891", element.getTopStructPi());
        }
        // Sibling volumes (just topstruct)
        Assert.assertEquals("306653648_1892", tocElements.get(TOC.DEFAULT_GROUP).get(105).getTopStructPi());
        Assert.assertEquals("306653648_1893", tocElements.get(TOC.DEFAULT_GROUP).get(106).getTopStructPi());
        Assert.assertEquals("306653648_1894", tocElements.get(TOC.DEFAULT_GROUP).get(107).getTopStructPi());
        Assert.assertEquals("306653648_1897", tocElements.get(TOC.DEFAULT_GROUP).get(108).getTopStructPi());
        Assert.assertEquals("306653648_1898", tocElements.get(TOC.DEFAULT_GROUP).get(109).getTopStructPi());
        Assert.assertEquals("306653648_1899", tocElements.get(TOC.DEFAULT_GROUP).get(110).getTopStructPi());
    }

    /**
     * @see TocMaker#generateToc(TOC,StructElement,boolean,String,int,int)
     * @verifies generate volume TOC without siblings correctly
     */
    @Test
    public void generateToc_shouldGenerateVolumeTOCWithoutSiblingsCorrectly() throws Exception {
        long iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648_1891");
        Assert.assertTrue(iddoc > 0);
        StructElement structElement = new StructElement(iddoc);
        Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(new TOC(), structElement, false, MimeType.IMAGE.getName(), 1, -1);
        Assert.assertNotNull(tocElements);
        Assert.assertNotNull(tocElements.get(TOC.DEFAULT_GROUP));
        Assert.assertEquals(105, tocElements.get(TOC.DEFAULT_GROUP).size()); // 1 anchor + 104 elements of volume ZDB026544598_0001
        // Anchor first
        Assert.assertEquals("306653648", tocElements.get(TOC.DEFAULT_GROUP).get(0).getTopStructPi());
        // First volume (complete tree)
        for (int i = 1; i <= 104; ++i) {
            TOCElement element = tocElements.get(TOC.DEFAULT_GROUP).get(i);
            Assert.assertEquals("306653648_1891", element.getTopStructPi());
        }
    }

    /**
     * @see TocMaker#generateToc(TOC,StructElement,boolean,String,int)
     * @verifies generate anchor TOC correctly
     */
    @Test
    public void generateToc_shouldGenerateAnchorTOCCorrectly() throws Exception {
        long iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648");
        Assert.assertTrue(iddoc > 0);
        StructElement structElement = new StructElement(iddoc);
        TOC toc = new TOC();
        Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(toc, structElement, true, MimeType.IMAGE.getName(), 1, -1);
        Assert.assertNotNull(tocElements);
        Assert.assertNotNull(tocElements.get(TOC.DEFAULT_GROUP));
        Assert.assertEquals(8, tocElements.get(TOC.DEFAULT_GROUP).size());
        Assert.assertEquals(7, toc.getTotalTocSize()); // 7 volumes
        Assert.assertEquals("306653648", tocElements.get(TOC.DEFAULT_GROUP).get(0).getTopStructPi());
        for (int i = 1; i < tocElements.get(TOC.DEFAULT_GROUP).size(); ++i) {
            Assert.assertTrue(tocElements.get(TOC.DEFAULT_GROUP).get(i).getTopStructPi().startsWith("306653648_189"));
        }
    }

    /**
     * @see TocMaker#generateToc(TOC,StructElement,boolean,String,int,int)
     * @verifies paginate anchor TOC correctly
     */
    @Test
    public void generateToc_shouldPaginateAnchorTOCCorrectly() throws Exception {
        long iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648");
        Assert.assertTrue(iddoc > 0);
        StructElement structElement = new StructElement(iddoc);
        TOC toc = new TOC();
        {
            // Page 1
            Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(toc, structElement, true, MimeType.IMAGE.getName(), 1, 3);
            Assert.assertNotNull(tocElements);
            Assert.assertNotNull(tocElements.get(TOC.DEFAULT_GROUP));
            Assert.assertEquals(4, tocElements.get(TOC.DEFAULT_GROUP).size());
            Assert.assertEquals(7, toc.getTotalTocSize());
            Assert.assertEquals("306653648", tocElements.get(TOC.DEFAULT_GROUP).get(0).getTopStructPi());
            Assert.assertEquals("306653648_1891", tocElements.get(TOC.DEFAULT_GROUP).get(1).getTopStructPi());
            Assert.assertEquals("306653648_1892", tocElements.get(TOC.DEFAULT_GROUP).get(2).getTopStructPi());
            Assert.assertEquals("306653648_1893", tocElements.get(TOC.DEFAULT_GROUP).get(3).getTopStructPi());
        }
        {
            // Page 2
            Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(toc, structElement, true, MimeType.IMAGE.getName(), 2, 3);
            Assert.assertNotNull(tocElements);
            Assert.assertNotNull(tocElements.get(TOC.DEFAULT_GROUP));
            Assert.assertEquals(4, tocElements.get(TOC.DEFAULT_GROUP).size());
            Assert.assertEquals(7, toc.getTotalTocSize());
            Assert.assertEquals("306653648", tocElements.get(TOC.DEFAULT_GROUP).get(0).getTopStructPi());
            Assert.assertEquals("306653648_1894", tocElements.get(TOC.DEFAULT_GROUP).get(1).getTopStructPi());
            Assert.assertEquals("306653648_1897", tocElements.get(TOC.DEFAULT_GROUP).get(2).getTopStructPi());
            Assert.assertEquals("306653648_1898", tocElements.get(TOC.DEFAULT_GROUP).get(3).getTopStructPi());
        }
        {
            // Page 3
            Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(toc, structElement, true, MimeType.IMAGE.getName(), 3, 3);
            Assert.assertNotNull(tocElements);
            Assert.assertNotNull(tocElements.get(TOC.DEFAULT_GROUP));
            Assert.assertEquals(2, tocElements.get(TOC.DEFAULT_GROUP).size());
            Assert.assertEquals(7, toc.getTotalTocSize());
            Assert.assertEquals("306653648", tocElements.get(TOC.DEFAULT_GROUP).get(0).getTopStructPi());
            Assert.assertEquals("306653648_1899", tocElements.get(TOC.DEFAULT_GROUP).get(1).getTopStructPi());
        }
    }

    /**
     * @see TocMaker#generateToc(TOC,StructElement,boolean,String,int)
     * @verifies throw IllegalArgumentException if structElement is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void generateToc_shouldThrowIllegalArgumentExceptionIfStructElementIsNull() throws Exception {
        Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(new TOC(), null, true, MimeType.IMAGE.getName(), 1, -1);
    }

    /**
     * @see TocMaker#generateToc(TOC,StructElement,boolean,String,int)
     * @verifies throw IllegalArgumentException if toc is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void generateToc_shouldThrowIllegalArgumentExceptionIfTocIsNull() throws Exception {
        long iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648_1891");
        Assert.assertTrue(iddoc > 0);
        Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(null, new StructElement(iddoc), true, MimeType.IMAGE.getName(), 1, -1);
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
