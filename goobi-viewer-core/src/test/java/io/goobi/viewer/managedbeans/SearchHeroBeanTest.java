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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.managedbeans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.collections.CollectionView;
import io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement;
import io.goobi.viewer.solr.SolrConstants;

class SearchHeroBeanTest {

    @Mock
    private CollectionBrowseBean collectionBrowseBean;

    @InjectMocks
    private SearchHeroBean bean;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void getHeading_preservesWhitespaceEmphasisAttributesAndInlineMarkup() {
        String heading = "Discover\n  <em class=\"accent\" data-kind=\"collection\">our <strong>collections</strong></em> today";

        assertEquals("Discover\n  <em class=\"accent\" data-kind=\"collection\"><a href=\"/viewer/browse/\">our <strong>collections</strong></a></em> today",
                bean.getHeading(heading, "/viewer/browse/"));
    }

    @Test
    void getHeading_preservesExistingLinksInsideAndOutsideEmphasis() {
        String heading = "<a href=\"/existing\"><em class=\"accent\">Existing</em></a> "
                + "<em>More <a href=\"/other\">information</a></em> <em>Collections</em>";

        Element result = Jsoup.parseBodyFragment(bean.getHeading(heading, "/viewer/browse/")).body();

        assertEquals(List.of("/existing", "/other", "/viewer/browse/"), result.select("a").eachAttr("href"));
        assertEquals("Existing", result.selectFirst("a[href=/existing] > em.accent").text());
        assertEquals("More information", result.select("em").get(1).text());
        assertTrue(result.select("a a").isEmpty());
    }

    @Test
    void getHeading_escapesLinkAttributesAndKeepsEncodedHeadingText() {
        String url = "/viewer/browse/?label=\"Art & Science\"";

        String html = bean.getHeading("Art &amp; <em>&lt;Science&gt;</em>", url);
        Element result = Jsoup.parseBodyFragment(html).body();

        assertEquals("Art & <Science>", result.text());
        assertEquals(url, result.selectFirst("a").attr("href"));
        assertEquals(1, result.selectFirst("a").attributes().size());
        assertTrue(html.contains("&quot;"));
        assertTrue(result.select("science").isEmpty());
    }

    @Test
    void getHeading_handlesMissingHeadingOrDestination() {
        assertEquals("", bean.getHeading(null, "/viewer/browse/"));
        assertEquals("Plain <em>emphasis</em>", bean.getHeading("Plain <em>emphasis</em>", ""));
    }

    @Test
    void getSlots_exposesExactlyThreeImmutableDefinitions() {
        List<SearchHeroBean.Slot> slots = bean.getSlots();

        assertEquals(List.of("a", "b", "c"), slots.stream().map(SearchHeroBean.Slot::getArea).toList());
        assertEquals(List.of(1200, 900, 900), slots.stream().map(SearchHeroBean.Slot::getImageSize).toList());
        assertThrows(UnsupportedOperationException.class, slots::clear);
    }

    @Test
    void getCollectionCount_countsRootsOnceDespiteVisibleChildren() throws Exception {
        CollectionView collection = mock(CollectionView.class);
        HierarchicalBrowseDcElement first = mock(HierarchicalBrowseDcElement.class);
        HierarchicalBrowseDcElement second = mock(HierarchicalBrowseDcElement.class);
        HierarchicalBrowseDcElement child = mock(HierarchicalBrowseDcElement.class);
        when(first.getLevel()).thenReturn(0);
        when(second.getLevel()).thenReturn(0);
        when(child.getLevel()).thenReturn(1);
        when(collectionBrowseBean.getList(SolrConstants.DC, 0)).thenReturn(List.of(first, second, child));
        when(collectionBrowseBean.getCollection(SolrConstants.DC)).thenReturn(collection);
        when(collection.getCompleteList()).thenReturn(List.of(first, second));

        assertEquals(2, bean.getCollectionCount());
        assertEquals(2, bean.getCollectionCount());
        verify(collectionBrowseBean, times(1)).getList(SolrConstants.DC, 0);
        verify(collection, times(1)).getCompleteList();
    }

    @Test
    void getCollectionCount_cachesAnIndexFailureAsZero() throws Exception {
        when(collectionBrowseBean.getList(SolrConstants.DC, 0)).thenThrow(new IndexUnreachableException("unavailable"));

        assertEquals(0, bean.getCollectionCount());
        assertEquals(0, bean.getCollectionCount());
        verify(collectionBrowseBean, times(1)).getList(SolrConstants.DC, 0);
    }

    @Test
    void yearGetters_shareOneIndexLookupPerRequest() {
        try (MockedStatic<SearchHelper> search = mockStatic(SearchHelper.class)) {
            search.when(() -> SearchHelper.getMinMaxYears(null)).thenReturn(new int[] { 1450, 2026 });

            assertEquals(2026, bean.getLastYear());
            assertEquals(1450, bean.getFirstYear());
            assertEquals(2026, bean.getLastYear());
            search.verify(() -> SearchHelper.getMinMaxYears(null), times(1));
        }
    }

    @Test
    void yearGetters_cacheAnIndexFailureAsUnavailable() {
        try (MockedStatic<SearchHelper> search = mockStatic(SearchHelper.class)) {
            search.when(() -> SearchHelper.getMinMaxYears(null)).thenThrow(new IndexUnreachableException("unavailable"));

            assertEquals(-1, bean.getFirstYear());
            assertEquals(-1, bean.getLastYear());
            search.verify(() -> SearchHelper.getMinMaxYears(null), times(1));
        }
    }

    @Test
    void yearGetters_cacheAPresentationFailureAsUnavailable() {
        try (MockedStatic<SearchHelper> search = mockStatic(SearchHelper.class)) {
            search.when(() -> SearchHelper.getMinMaxYears(null)).thenThrow(new PresentationException("invalid response"));

            assertEquals(-1, bean.getFirstYear());
            assertEquals(-1, bean.getLastYear());
            search.verify(() -> SearchHelper.getMinMaxYears(null), times(1));
        }
    }
}
