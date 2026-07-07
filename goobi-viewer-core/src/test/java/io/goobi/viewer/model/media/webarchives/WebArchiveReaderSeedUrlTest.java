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
package io.goobi.viewer.model.media.webarchives;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;

class WebArchiveReaderSeedUrlTest extends AbstractSolrEnabledTest {

    private static final String PI = "PI_WEBARCHIVE_SEED_TEST";

    private static final String PRIMARY_QUERY_PREFIX = "+PI_TOPSTRUCT:" + PI + " +DOCTYPE:PAGE +MIMETYPE:application/warc";
    private static final String FALLBACK_QUERY_PREFIX = "+PI:" + PI + " +MD_WEBARCHIVE_IDENTIFIER:*";

    private SolrSearchIndex mockedIndex;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        SolrSearchIndex defaultIndex = DataManager.getInstance().getSearchIndex();
        mockedIndex = Mockito.spy(defaultIndex);
        DataManager.getInstance().injectSearchIndex(mockedIndex);
    }

    /**
     * @verifies not use the external fallback when local archive docs exist
     * @see WebArchiveReader#getSeedUrl(String)
     */
    @Test
    void getSeedUrl_shouldNotUseFallbackWhenLocalDocsExist() throws Exception {
        SolrDocumentList localDocs = new SolrDocumentList();
        localDocs.add(new SolrDocument(Map.of(SolrConstants.FILENAME, "does-not-exist.wacz")));
        Mockito.when(mockedIndex.getDocs(ArgumentMatchers.startsWith(PRIMARY_QUERY_PREFIX), ArgumentMatchers.eq(Collections.emptyList())))
                .thenReturn(localDocs);

        SolrDocumentList fallbackDocs = new SolrDocumentList();
        fallbackDocs.add(new SolrDocument(Map.of(SolrConstants.MD_WEBARCHIVE_IDENTIFIER,
                List.of("https://replayweb.page/?source=https://archive.example.org/replay.json#view=pages&url=https://example.org/"))));
        Mockito.when(mockedIndex.getDocs(ArgumentMatchers.startsWith(FALLBACK_QUERY_PREFIX), ArgumentMatchers.eq(Collections.emptyList())))
                .thenReturn(fallbackDocs);

        assertEquals("", WebArchiveReader.getSeedUrl(PI));
        Mockito.verify(mockedIndex, Mockito.never()).getDocs(ArgumentMatchers.startsWith(FALLBACK_QUERY_PREFIX), ArgumentMatchers.any());
    }

    /**
     * @verifies return the url param value from the fallback identifier when no local docs exist
     * @see WebArchiveReader#getSeedUrl(String)
     */
    @Test
    void getSeedUrl_shouldReturnUrlParamFromFallbackWhenNoLocalDocsExist() throws Exception {
        Mockito.when(mockedIndex.getDocs(ArgumentMatchers.startsWith(PRIMARY_QUERY_PREFIX), ArgumentMatchers.eq(Collections.emptyList())))
                .thenReturn(null);

        // Real-world shape (replayweb.page): "source" is a query param, but "url" (the seed URL) is a param
        // inside the URL fragment (after '#'), not the query string.
        SolrDocumentList fallbackDocs = new SolrDocumentList();
        fallbackDocs.add(new SolrDocument(Map.of(SolrConstants.MD_WEBARCHIVE_IDENTIFIER,
                List.of("https://replayweb.page/?source=https%3A%2F%2Farchive.example.org%2Freplay.json"
                        + "#view=pages&url=https%3A%2F%2Fexample.org%2Fstart&ts=20251016100005"))));
        Mockito.when(mockedIndex.getDocs(ArgumentMatchers.startsWith(FALLBACK_QUERY_PREFIX), ArgumentMatchers.eq(Collections.emptyList())))
                .thenReturn(fallbackDocs);

        assertEquals("https://example.org/start", WebArchiveReader.getSeedUrl(PI));
    }

    /**
     * @verifies return empty string when the fallback identifier has no url param
     * @see WebArchiveReader#getSeedUrl(String)
     */
    @Test
    void getSeedUrl_shouldReturnEmptyWhenFallbackIdentifierHasNoUrlParam() throws Exception {
        Mockito.when(mockedIndex.getDocs(ArgumentMatchers.startsWith(PRIMARY_QUERY_PREFIX), ArgumentMatchers.eq(Collections.emptyList())))
                .thenReturn(null);

        SolrDocumentList fallbackDocs = new SolrDocumentList();
        fallbackDocs.add(new SolrDocument(Map.of(SolrConstants.MD_WEBARCHIVE_IDENTIFIER,
                List.of("https://replayweb.page/?source=https://archive.example.org/replay.json#view=pages"))));
        Mockito.when(mockedIndex.getDocs(ArgumentMatchers.startsWith(FALLBACK_QUERY_PREFIX), ArgumentMatchers.eq(Collections.emptyList())))
                .thenReturn(fallbackDocs);

        assertEquals("", WebArchiveReader.getSeedUrl(PI));
    }

    /**
     * @verifies skip a malformed fallback identifier and use the next one
     * @see WebArchiveReader#getSeedUrl(String)
     */
    @Test
    void getSeedUrl_shouldSkipMalformedFallbackIdentifierAndUseNextOne() throws Exception {
        Mockito.when(mockedIndex.getDocs(ArgumentMatchers.startsWith(PRIMARY_QUERY_PREFIX), ArgumentMatchers.eq(Collections.emptyList())))
                .thenReturn(null);

        SolrDocumentList fallbackDocs = new SolrDocumentList();
        fallbackDocs.add(new SolrDocument(Map.of(SolrConstants.MD_WEBARCHIVE_IDENTIFIER,
                List.of("http://exa mple.org/broken",
                        "https://replayweb.page/?source=https://archive.example.org/replay.json#view=pages&url=https://example.org/start"))));
        Mockito.when(mockedIndex.getDocs(ArgumentMatchers.startsWith(FALLBACK_QUERY_PREFIX), ArgumentMatchers.eq(Collections.emptyList())))
                .thenReturn(fallbackDocs);

        assertEquals("https://example.org/start", WebArchiveReader.getSeedUrl(PI));
    }

    /**
     * @verifies return empty string when neither the local nor the fallback query find anything
     * @see WebArchiveReader#getSeedUrl(String)
     */
    @Test
    void getSeedUrl_shouldReturnEmptyWhenNothingFound() throws Exception {
        Mockito.when(mockedIndex.getDocs(ArgumentMatchers.startsWith(PRIMARY_QUERY_PREFIX), ArgumentMatchers.eq(Collections.emptyList())))
                .thenReturn(null);
        Mockito.when(mockedIndex.getDocs(ArgumentMatchers.startsWith(FALLBACK_QUERY_PREFIX), ArgumentMatchers.eq(Collections.emptyList())))
                .thenReturn(null);

        assertEquals("", WebArchiveReader.getSeedUrl(PI));
    }
}
