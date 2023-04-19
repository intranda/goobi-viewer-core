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
package io.goobi.viewer.model.toc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.model.viewer.BaseMimeType;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;

public class TocMakerTest extends AbstractDatabaseAndSolrEnabledTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
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
        Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(new TOC(), structElement, true, BaseMimeType.IMAGE.getName(), 1, -1);
        Assert.assertNotNull(tocElements);
        Assert.assertNotNull(tocElements.get(StringConstants.DEFAULT_NAME));
        Assert.assertEquals(111, tocElements.get(StringConstants.DEFAULT_NAME).size()); // 1 anchor + 104 elements of volume 306653648_1891 + 6 sibling volume top elements
        // Anchor first
        Assert.assertEquals("306653648", tocElements.get(StringConstants.DEFAULT_NAME).get(0).getTopStructPi());
        // First volume (complete tree)
        for (int i = 1; i <= 104; ++i) {
            TOCElement element = tocElements.get(StringConstants.DEFAULT_NAME).get(i);
            Assert.assertEquals("306653648_1891", element.getTopStructPi());
        }
        // Sibling volumes (just topstruct)
        Assert.assertEquals("306653648_1892", tocElements.get(StringConstants.DEFAULT_NAME).get(105).getTopStructPi());
        Assert.assertEquals("306653648_1893", tocElements.get(StringConstants.DEFAULT_NAME).get(106).getTopStructPi());
        Assert.assertEquals("306653648_1894", tocElements.get(StringConstants.DEFAULT_NAME).get(107).getTopStructPi());
        Assert.assertEquals("306653648_1897", tocElements.get(StringConstants.DEFAULT_NAME).get(108).getTopStructPi());
        Assert.assertEquals("306653648_1898", tocElements.get(StringConstants.DEFAULT_NAME).get(109).getTopStructPi());
        Assert.assertEquals("306653648_1899", tocElements.get(StringConstants.DEFAULT_NAME).get(110).getTopStructPi());
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
        Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(new TOC(), structElement, false, BaseMimeType.IMAGE.getName(), 1, -1);
        Assert.assertNotNull(tocElements);
        Assert.assertNotNull(tocElements.get(StringConstants.DEFAULT_NAME));
        Assert.assertEquals(105, tocElements.get(StringConstants.DEFAULT_NAME).size()); // 1 anchor + 104 elements of volume ZDB026544598_0001
        // Anchor first
        Assert.assertEquals("306653648", tocElements.get(StringConstants.DEFAULT_NAME).get(0).getTopStructPi());
        // First volume (complete tree)
        for (int i = 1; i <= 104; ++i) {
            TOCElement element = tocElements.get(StringConstants.DEFAULT_NAME).get(i);
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
        Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(toc, structElement, true, BaseMimeType.IMAGE.getName(), 1, -1);
        Assert.assertNotNull(tocElements);
        Assert.assertNotNull(tocElements.get(StringConstants.DEFAULT_NAME));
        Assert.assertEquals(8, tocElements.get(StringConstants.DEFAULT_NAME).size());
        Assert.assertEquals(7, toc.getTotalTocSize()); // 7 volumes
        Assert.assertEquals("306653648", tocElements.get(StringConstants.DEFAULT_NAME).get(0).getTopStructPi());
        for (int i = 1; i < tocElements.get(StringConstants.DEFAULT_NAME).size(); ++i) {
            Assert.assertTrue(tocElements.get(StringConstants.DEFAULT_NAME).get(i).getTopStructPi().startsWith("306653648_189"));
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
            Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(toc, structElement, true, BaseMimeType.IMAGE.getName(), 1, 3);
            Assert.assertNotNull(tocElements);
            Assert.assertNotNull(tocElements.get(StringConstants.DEFAULT_NAME));
            Assert.assertEquals(4, tocElements.get(StringConstants.DEFAULT_NAME).size());
            Assert.assertEquals(7, toc.getTotalTocSize());
            Assert.assertEquals("306653648", tocElements.get(StringConstants.DEFAULT_NAME).get(0).getTopStructPi());
            Assert.assertEquals("306653648_1891", tocElements.get(StringConstants.DEFAULT_NAME).get(1).getTopStructPi());
            Assert.assertEquals("306653648_1892", tocElements.get(StringConstants.DEFAULT_NAME).get(2).getTopStructPi());
            Assert.assertEquals("306653648_1893", tocElements.get(StringConstants.DEFAULT_NAME).get(3).getTopStructPi());
        }
        {
            // Page 2
            Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(toc, structElement, true, BaseMimeType.IMAGE.getName(), 2, 3);
            Assert.assertNotNull(tocElements);
            Assert.assertNotNull(tocElements.get(StringConstants.DEFAULT_NAME));
            Assert.assertEquals(4, tocElements.get(StringConstants.DEFAULT_NAME).size());
            Assert.assertEquals(7, toc.getTotalTocSize());
            Assert.assertEquals("306653648", tocElements.get(StringConstants.DEFAULT_NAME).get(0).getTopStructPi());
            Assert.assertEquals("306653648_1894", tocElements.get(StringConstants.DEFAULT_NAME).get(1).getTopStructPi());
            Assert.assertEquals("306653648_1897", tocElements.get(StringConstants.DEFAULT_NAME).get(2).getTopStructPi());
            Assert.assertEquals("306653648_1898", tocElements.get(StringConstants.DEFAULT_NAME).get(3).getTopStructPi());
        }
        {
            // Page 3
            Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(toc, structElement, true, BaseMimeType.IMAGE.getName(), 3, 3);
            Assert.assertNotNull(tocElements);
            Assert.assertNotNull(tocElements.get(StringConstants.DEFAULT_NAME));
            Assert.assertEquals(2, tocElements.get(StringConstants.DEFAULT_NAME).size());
            Assert.assertEquals(7, toc.getTotalTocSize());
            Assert.assertEquals("306653648", tocElements.get(StringConstants.DEFAULT_NAME).get(0).getTopStructPi());
            Assert.assertEquals("306653648_1899", tocElements.get(StringConstants.DEFAULT_NAME).get(1).getTopStructPi());
        }
    }

    /**
     * @see TocMaker#generateToc(TOC,StructElement,boolean,String,int)
     * @verifies throw IllegalArgumentException if structElement is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void generateToc_shouldThrowIllegalArgumentExceptionIfStructElementIsNull() throws Exception {
        TocMaker.generateToc(new TOC(), null, true, BaseMimeType.IMAGE.getName(), 1, -1);
    }

    /**
     * @see TocMaker#generateToc(TOC,StructElement,boolean,String,int)
     * @verifies throw IllegalArgumentException if toc is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void generateToc_shouldThrowIllegalArgumentExceptionIfTocIsNull() throws Exception {
        long iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648_1891");
        Assert.assertTrue(iddoc > 0);
        TocMaker.generateToc(null, new StructElement(iddoc), true, BaseMimeType.IMAGE.getName(), 1, -1);
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
            String label = TocMaker.buildLabel(doc, null).getValue().orElse("");
            Assert.assertEquals("label / creator", label);
        }
        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.CURRENTNO, "1");
            doc.setField("MD_TITLE", "title");
            Assert.assertEquals("Number 1: title", TocMaker.buildLabel(doc, "PeriodicalVolume").getValue().orElse(""));
        }
    }

    /**
     * @see TocMaker#buildLabel(SolrDocument,String)
     * @verifies fill remaining parameters correctly if docstruct fallback used
     */
    @Test
    public void buildLabel_shouldFillRemainingParametersCorrectlyIfDocstructFallbackUsed() throws Exception {

        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.CURRENTNO, "1");
        doc.setField(SolrConstants.DOCSTRCT, "PeriodicalVolume");
        IMetadataValue value = TocMaker.buildLabel(doc, "PeriodicalVolume");
        String label = value.getValue(Locale.ENGLISH).orElse("");
        Assert.assertEquals("Number 1: Periodical volume", label);
    }

    /**
     * @see TocMaker#createOrderedGroupDocMap(List,List,String)
     * @verifies create correctly sorted map
     */
    @Test
    public void createOrderedGroupDocMap_shouldCreateCorrectlySortedMap() throws Exception {
        String pi = "PPN123";
        List<SolrDocument> groupMemberDocs = new ArrayList<>(5);
        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.IDDOC, String.valueOf(1));
            doc.setField("GROUPID_SERIES", pi);
            doc.setField("GROUPORDER_SERIES", 5);
            groupMemberDocs.add(doc);
        }
        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.IDDOC, String.valueOf(2));
            doc.setField("GROUPID_SERIES_2", pi);
            doc.setField("GROUPORDER_SERIES_2", 4);
            groupMemberDocs.add(doc);
        }
        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.IDDOC, String.valueOf(3));
            doc.setField("GROUPID_SERIES_3", pi);
            doc.setField("GROUPORDER_SERIES_3", 3);
            groupMemberDocs.add(doc);
        }
        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.IDDOC, String.valueOf(4));
            doc.setField("GROUPID_SERIES_2", pi);
            doc.setField("GROUPORDER_SERIES_2", 2);
            groupMemberDocs.add(doc);
        }
        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.IDDOC, String.valueOf(5));
            doc.setField("GROUPID_SERIES", pi);
            doc.setField("GROUPORDER_SERIES", 1);
            groupMemberDocs.add(doc);
        }
        Map<Integer, SolrDocument> result = TocMaker.createOrderedGroupDocMap(groupMemberDocs,
                Arrays.asList(new String[] { "GROUPID_SERIES", "GROUPID_SERIES_2", "GROUPID_SERIES_3" }), pi);
        Assert.assertNotNull(result);
        Assert.assertEquals(5, result.size());
        Assert.assertEquals("5", result.get(1).getFieldValue(SolrConstants.IDDOC));
        Assert.assertEquals("4", result.get(2).getFieldValue(SolrConstants.IDDOC));
        Assert.assertEquals("3", result.get(3).getFieldValue(SolrConstants.IDDOC));
        Assert.assertEquals("2", result.get(4).getFieldValue(SolrConstants.IDDOC));
        Assert.assertEquals("1", result.get(5).getFieldValue(SolrConstants.IDDOC));
    }
}
