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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.model.viewer.BaseMimeType;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;

class TocMakerTest extends AbstractDatabaseAndSolrEnabledTest {

    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
    }

    /**
     * @see TocMaker#getSolrFieldsToFetch()
     * @verifies return both static and configured fields
     */
    @Test
    void getSolrFieldsToFetch_shouldReturnBothStaticAndConfiguredFields() throws Exception {
        List<?> fields = TocMaker.getSolrFieldsToFetch("_DEFAULT");
        Assertions.assertNotNull(fields);
        Assertions.assertEquals(33, fields.size()); //The fields configured in getTocLabelConfiguration() are counted twice, once  suffixed with _LANG_...
    }

    /**
     * @see TocMaker#generateToc(TOC,StructElement,boolean,String,int,int)
     * @verifies generate volume TOC with siblings correctly
     */
    @Test
    void generateToc_shouldGenerateVolumeTOCWithSiblingsCorrectly() throws Exception {
        String iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648_1891");
        Assertions.assertNotNull(iddoc);
        StructElement structElement = new StructElement(iddoc);
        Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(new TOC(), structElement, true, BaseMimeType.IMAGE.getName(), 1, -1);
        Assertions.assertNotNull(tocElements);
        Assertions.assertNotNull(tocElements.get(StringConstants.DEFAULT_NAME));
        Assertions.assertEquals(111, tocElements.get(StringConstants.DEFAULT_NAME).size()); // 1 anchor + 104 elements of volume 306653648_1891 + 6 sibling volume top elements
        // Anchor first
        Assertions.assertEquals("306653648", tocElements.get(StringConstants.DEFAULT_NAME).get(0).getTopStructPi());
        // First volume (complete tree)
        for (int i = 1; i <= 104; ++i) {
            TOCElement element = tocElements.get(StringConstants.DEFAULT_NAME).get(i);
            Assertions.assertEquals("306653648_1891", element.getTopStructPi());
        }
        // Sibling volumes (just topstruct)
        Assertions.assertEquals("306653648_1892", tocElements.get(StringConstants.DEFAULT_NAME).get(105).getTopStructPi());
        Assertions.assertEquals("306653648_1893", tocElements.get(StringConstants.DEFAULT_NAME).get(106).getTopStructPi());
        Assertions.assertEquals("306653648_1894", tocElements.get(StringConstants.DEFAULT_NAME).get(107).getTopStructPi());
        Assertions.assertEquals("306653648_1897", tocElements.get(StringConstants.DEFAULT_NAME).get(108).getTopStructPi());
        Assertions.assertEquals("306653648_1898", tocElements.get(StringConstants.DEFAULT_NAME).get(109).getTopStructPi());
        Assertions.assertEquals("306653648_1899", tocElements.get(StringConstants.DEFAULT_NAME).get(110).getTopStructPi());
    }

    /**
     * @see TocMaker#generateToc(TOC,StructElement,boolean,String,int,int)
     * @verifies generate volume TOC without siblings correctly
     */
    @Test
    void generateToc_shouldGenerateVolumeTOCWithoutSiblingsCorrectly() throws Exception {
        String iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648_1891");
        Assertions.assertNotNull(iddoc);
        StructElement structElement = new StructElement(iddoc);
        Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(new TOC(), structElement, false, BaseMimeType.IMAGE.getName(), 1, -1);
        Assertions.assertNotNull(tocElements);
        Assertions.assertNotNull(tocElements.get(StringConstants.DEFAULT_NAME));
        Assertions.assertEquals(105, tocElements.get(StringConstants.DEFAULT_NAME).size()); // 1 anchor + 104 elements of volume ZDB026544598_0001
        // Anchor first
        Assertions.assertEquals("306653648", tocElements.get(StringConstants.DEFAULT_NAME).get(0).getTopStructPi());
        // First volume (complete tree)
        for (int i = 1; i <= 104; ++i) {
            TOCElement element = tocElements.get(StringConstants.DEFAULT_NAME).get(i);
            Assertions.assertEquals("306653648_1891", element.getTopStructPi());
        }
    }

    /**
     * @see TocMaker#generateToc(TOC,StructElement,boolean,String,int)
     * @verifies generate anchor TOC correctly
     */
    @Test
    void generateToc_shouldGenerateAnchorTOCCorrectly() throws Exception {
        String iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648");
        Assertions.assertNotNull(iddoc);
        StructElement structElement = new StructElement(iddoc);
        TOC toc = new TOC();
        Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(toc, structElement, true, BaseMimeType.IMAGE.getName(), 1, -1);
        Assertions.assertNotNull(tocElements);
        Assertions.assertNotNull(tocElements.get(StringConstants.DEFAULT_NAME));
        Assertions.assertEquals(8, tocElements.get(StringConstants.DEFAULT_NAME).size());
        Assertions.assertEquals(7, toc.getTotalTocSize()); // 7 volumes
        Assertions.assertEquals("306653648", tocElements.get(StringConstants.DEFAULT_NAME).get(0).getTopStructPi());
        for (int i = 1; i < tocElements.get(StringConstants.DEFAULT_NAME).size(); ++i) {
            Assertions.assertTrue(tocElements.get(StringConstants.DEFAULT_NAME).get(i).getTopStructPi().startsWith("306653648_189"));
        }
    }

    /**
     * @see TocMaker#generateToc(TOC,StructElement,boolean,String,int,int)
     * @verifies paginate anchor TOC correctly
     */
    @Test
    void generateToc_shouldPaginateAnchorTOCCorrectly() throws Exception {
        String iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648");
        Assertions.assertNotNull(iddoc);
        StructElement structElement = new StructElement(iddoc);
        TOC toc = new TOC();
        {
            // Page 1
            Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(toc, structElement, true, BaseMimeType.IMAGE.getName(), 1, 3);
            Assertions.assertNotNull(tocElements);
            Assertions.assertNotNull(tocElements.get(StringConstants.DEFAULT_NAME));
            Assertions.assertEquals(4, tocElements.get(StringConstants.DEFAULT_NAME).size());
            Assertions.assertEquals(7, toc.getTotalTocSize());
            Assertions.assertEquals("306653648", tocElements.get(StringConstants.DEFAULT_NAME).get(0).getTopStructPi());
            Assertions.assertEquals("306653648_1891", tocElements.get(StringConstants.DEFAULT_NAME).get(1).getTopStructPi());
            Assertions.assertEquals("306653648_1892", tocElements.get(StringConstants.DEFAULT_NAME).get(2).getTopStructPi());
            Assertions.assertEquals("306653648_1893", tocElements.get(StringConstants.DEFAULT_NAME).get(3).getTopStructPi());
        }
        {
            // Page 2
            Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(toc, structElement, true, BaseMimeType.IMAGE.getName(), 2, 3);
            Assertions.assertNotNull(tocElements);
            Assertions.assertNotNull(tocElements.get(StringConstants.DEFAULT_NAME));
            Assertions.assertEquals(4, tocElements.get(StringConstants.DEFAULT_NAME).size());
            Assertions.assertEquals(7, toc.getTotalTocSize());
            Assertions.assertEquals("306653648", tocElements.get(StringConstants.DEFAULT_NAME).get(0).getTopStructPi());
            Assertions.assertEquals("306653648_1894", tocElements.get(StringConstants.DEFAULT_NAME).get(1).getTopStructPi());
            Assertions.assertEquals("306653648_1897", tocElements.get(StringConstants.DEFAULT_NAME).get(2).getTopStructPi());
            Assertions.assertEquals("306653648_1898", tocElements.get(StringConstants.DEFAULT_NAME).get(3).getTopStructPi());
        }
        {
            // Page 3
            Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(toc, structElement, true, BaseMimeType.IMAGE.getName(), 3, 3);
            Assertions.assertNotNull(tocElements);
            Assertions.assertNotNull(tocElements.get(StringConstants.DEFAULT_NAME));
            Assertions.assertEquals(2, tocElements.get(StringConstants.DEFAULT_NAME).size());
            Assertions.assertEquals(7, toc.getTotalTocSize());
            Assertions.assertEquals("306653648", tocElements.get(StringConstants.DEFAULT_NAME).get(0).getTopStructPi());
            Assertions.assertEquals("306653648_1899", tocElements.get(StringConstants.DEFAULT_NAME).get(1).getTopStructPi());
        }
    }

    /**
     * @see TocMaker#generateToc(TOC,StructElement,boolean,String,int)
     * @verifies throw IllegalArgumentException if structElement is null
     */
    @Test
    void generateToc_shouldThrowIllegalArgumentExceptionIfStructElementIsNull() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> TocMaker.generateToc(new TOC(), null, true, BaseMimeType.IMAGE.getName(), 1, -1));
    }

    /**
     * @see TocMaker#generateToc(TOC,StructElement,boolean,String,int)
     * @verifies throw IllegalArgumentException if toc is null
     */
    @Test
    void generateToc_shouldThrowIllegalArgumentExceptionIfTocIsNull() throws Exception {
        String iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648_1891");
        Assertions.assertNotNull(iddoc);
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> TocMaker.generateToc(null, new StructElement(iddoc), true, BaseMimeType.IMAGE.getName(), 1, -1));
    }

    /**
     * @see TocMaker#buildLabel(SolrDocument)
     * @verifies build configured label correctly
     */
    @Test
    void buildLabel_shouldBuildConfiguredLabelCorrectly() {
        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.LABEL, "label");
            doc.setField("MD_CREATOR", "creator");
            String label = TocMaker.buildLabel(doc, null).getValue().orElse("");
            Assertions.assertEquals("label / creator", label);
        }
        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.CURRENTNO, "1");
            doc.setField("MD_TITLE", "title");
            Assertions.assertEquals("Number 1: title", TocMaker.buildLabel(doc, "PeriodicalVolume").getValue().orElse(""));
        }
    }

    /**
     * @see TocMaker#buildLabel(SolrDocument,String)
     * @verifies fill remaining parameters correctly if docstruct fallback used
     */
    @Test
    void buildLabel_shouldFillRemainingParametersCorrectlyIfDocstructFallbackUsed() {

        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.CURRENTNO, "1");
        doc.setField(SolrConstants.DOCSTRCT, "PeriodicalVolume");
        IMetadataValue value = TocMaker.buildLabel(doc, "PeriodicalVolume");
        String label = value.getValue(Locale.ENGLISH).orElse("");
        Assertions.assertEquals("Number 1: Periodical volume", label);
    }

    /**
     * @see TocMaker#createOrderedGroupDocMap(List,List,String)
     * @verifies create correctly sorted map
     */
    @Test
    void createOrderedGroupDocMap_shouldCreateCorrectlySortedMap() {
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
        Assertions.assertNotNull(result);
        Assertions.assertEquals(5, result.size());
        Assertions.assertEquals("5", result.get(1).getFieldValue(SolrConstants.IDDOC));
        Assertions.assertEquals("4", result.get(2).getFieldValue(SolrConstants.IDDOC));
        Assertions.assertEquals("3", result.get(3).getFieldValue(SolrConstants.IDDOC));
        Assertions.assertEquals("2", result.get(4).getFieldValue(SolrConstants.IDDOC));
        Assertions.assertEquals("1", result.get(5).getFieldValue(SolrConstants.IDDOC));
    }
}
