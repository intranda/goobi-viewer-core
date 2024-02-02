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
package io.goobi.viewer.model.citation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.undercouch.citeproc.csl.CSLType;
import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.model.citation.CitationLink.CitationLinkLevel;
import io.goobi.viewer.model.viewer.ViewManager;
import io.goobi.viewer.solr.SolrConstants;

class CitationToolsTest extends AbstractSolrEnabledTest {

    /**
     * @see CitationLink#getValue(ViewManager)
     * @verifies throw IllegalArgumentException if allLinks null
     */
    @Test
    void getValue_shouldThrowIllegalArgumentExceptionIfAllLinksNull() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV);
        Assertions.assertNotNull(viewManager);

        Exception e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> CitationTools.generateCitationLinksForLevel(null, CitationLinkLevel.RECORD, viewManager));
        Assertions.assertEquals("allLinks may not be null", e.getMessage());
    }

    /**
     * @see CitationLink#getValue(ViewManager)
     * @verifies throw IllegalArgumentException if level null
     */
    @Test
    void getValue_shouldThrowIllegalArgumentExceptionIfLevelNull() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV);
        Assertions.assertNotNull(viewManager);

        List<CitationLink> allLinks = Collections.emptyList();
        Exception e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> CitationTools.generateCitationLinksForLevel(allLinks, null, viewManager));
        Assertions.assertEquals("level may not be null", e.getMessage());
    }
    
    /**
     * @see CitationLink#getValue(ViewManager)
     * @verifies throw IllegalArgumentException if viewManager null
     */
    @Test
    void getValue_shouldThrowIllegalArgumentExceptionIfViewManagerNull() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV);
        Assertions.assertNotNull(viewManager);

        List<CitationLink> allLinks = Collections.emptyList();
        Exception e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> CitationTools.generateCitationLinksForLevel(allLinks, CitationLinkLevel.RECORD, null));
        Assertions.assertEquals("viewManager may not be null", e.getMessage());
    }

    /**
     * @see CitationLink#getValue(ViewManager)
     * @verifies preserve internal links
     */
    @Test
    void getValue_shouldPreserveInternalLinks() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV);
        Assertions.assertNotNull(viewManager);

        CitationLink link = new CitationLink("url", "record", "foo").setField(SolrConstants.PI);
        CitationLink link2 = new CitationLink("internal", "record", "bar").setField(SolrConstants.PI);
        Assertions.assertEquals(2, CitationTools.generateCitationLinksForLevel(Arrays.asList(link, link2), link.getLevel(), viewManager).size());
    }

    /**
     * @see CitationLink#getValue(ViewManager)
     * @verifies set correct value for record type
     */
    @Test
    void getValue_shouldSetCorrectValueForRecordType() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV);
        Assertions.assertNotNull(viewManager);

        CitationLink link = new CitationLink("url", "record", "foo").setField(SolrConstants.PI);
        CitationTools.generateCitationLinksForLevel(Collections.singletonList(link), link.getLevel(), viewManager);
        Assertions.assertEquals(PI_KLEIUNIV, link.getValue());
    }

    /**
     * @see CitationLink#getValue(ViewManager)
     * @verifies set correct value for docstruct type
     */
    @Test
    void getValue_shouldSetCorrectValueForDocstructType() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV);
        Assertions.assertNotNull(viewManager);

        CitationLink link = new CitationLink("url", "docstruct", "foo").setField(SolrConstants.PI);
        CitationTools.generateCitationLinksForLevel(Collections.singletonList(link), link.getLevel(), viewManager);
        Assertions.assertEquals(PI_KLEIUNIV, link.getValue());
    }

    /**
     * @see CitationLink#getValue(ViewManager)
     * @verifies set correct value for image type
     */
    @Test
    void getValue_shouldSetCorrectValueForImageType() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV);
        Assertions.assertNotNull(viewManager);
        viewManager.setCurrentImageOrder(10);

        CitationLink link = new CitationLink("url", "image", "foo").setField(SolrConstants.ORDER);
        CitationTools.generateCitationLinksForLevel(Collections.singletonList(link), link.getLevel(), viewManager);
        Assertions.assertEquals("10", link.getValue());
    }

    /**
     * @see CitationLink#getValue(ViewManager)
     * @verifies fall back to topstruct value correctly
     */
    @Test
    void getValue_shouldFallBackToTopstructValueCorrectly() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV);
        Assertions.assertNotNull(viewManager);
        viewManager.setCurrentImageOrder(10);

        CitationLink link = new CitationLink("url", "image", "foo").setField(SolrConstants.PI).setTopstructValueFallback(true);
        CitationTools.generateCitationLinksForLevel(Collections.singletonList(link), link.getLevel(), viewManager);
        Assertions.assertEquals(PI_KLEIUNIV, link.getValue());
    }

    /**
     * @see CitationLink#getUrl(ViewManager)
     * @verifies apply pattern correctly
     */
    @Test
    void getValue_shouldApplyPatternCorrectly() throws Exception {
        ViewManager viewManager = ViewManager.createViewManager(PI_KLEIUNIV);
        Assertions.assertNotNull(viewManager);
        viewManager.setCurrentImageOrder(2);

        CitationLink link = new CitationLink("url", "image", "foo").setField(SolrConstants.PI_TOPSTRUCT)
                .setPattern("https://viewer.goobi.io/resolver?id={value}&page={page}");
        CitationTools.generateCitationLinksForLevel(Collections.singletonList(link), link.getLevel(), viewManager);
        Assertions.assertEquals("https://viewer.goobi.io/resolver?id=" + PI_KLEIUNIV + "&page=2", link.getUrl(viewManager));
    }

    /**
     * @see CitationTools#getCSLTypeForDocstrct(String)
     * @verifies return correct type
     */
    @Test
    void getCSLTypeForDocstrct_shouldReturnCorrectType() throws Exception {
        Assertions.assertEquals(CSLType.ARTICLE, CitationTools.getCSLTypeForDocstrct(null, null));
        Assertions.assertEquals(CSLType.ARTICLE, CitationTools.getCSLTypeForDocstrct("article", null));
        Assertions.assertEquals(CSLType.ARTICLE, CitationTools.getCSLTypeForDocstrct("article", "Other"));
        Assertions.assertEquals(CSLType.ARTICLE_JOURNAL, CitationTools.getCSLTypeForDocstrct("article", "PeriodicalVolume"));
        Assertions.assertEquals(CSLType.ARTICLE_NEWSPAPER, CitationTools.getCSLTypeForDocstrct("article", "NewspaperIssue"));
        Assertions.assertEquals(CSLType.ARTICLE, CitationTools.getCSLTypeForDocstrct("object", null));
        Assertions.assertEquals(CSLType.BOOK, CitationTools.getCSLTypeForDocstrct("monograph", null));
        Assertions.assertEquals(CSLType.CHAPTER, CitationTools.getCSLTypeForDocstrct("chapter", null));
        Assertions.assertEquals(CSLType.MANUSCRIPT, CitationTools.getCSLTypeForDocstrct("manuscript", null));
        Assertions.assertEquals(CSLType.MAP, CitationTools.getCSLTypeForDocstrct("SingleMap", null));
    }
}
