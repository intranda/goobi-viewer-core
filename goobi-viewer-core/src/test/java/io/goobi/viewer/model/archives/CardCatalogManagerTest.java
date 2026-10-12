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
package io.goobi.viewer.model.archives;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;

class CardCatalogManagerTest {

    /**
     * @verifies return single position for empty interval
     * @see CardCatalogManager#computeBoundaryPositions(int,int,int)
     */
    @Test
    void computeBoundaryPositions_shouldReturnSinglePositionForEmptyInterval() {
        assertEquals(List.of(5), CardCatalogManager.computeBoundaryPositions(5, 5, 9));
    }

    /**
     * @verifies distribute positions evenly
     * @see CardCatalogManager#computeBoundaryPositions(int,int,int)
     */
    @Test
    void computeBoundaryPositions_shouldDistributePositionsEvenly() {
        List<Integer> positions = CardCatalogManager.computeBoundaryPositions(1, 1022, 9);
        assertEquals(9, positions.size());
        assertEquals(1, positions.get(0));
        assertEquals(1022, positions.get(positions.size() - 1));
        // Every step covers roughly the same number of cards
        for (int i = 1; i < positions.size(); i++) {
            int step = positions.get(i) - positions.get(i - 1);
            assertTrue(step >= 127 && step <= 129, "Unexpected step width: " + step);
        }
    }

    /**
     * @verifies not return duplicate positions for narrow intervals
     * @see CardCatalogManager#computeBoundaryPositions(int,int,int)
     */
    @Test
    void computeBoundaryPositions_shouldNotReturnDuplicatePositionsForNarrowIntervals() {
        List<Integer> positions = CardCatalogManager.computeBoundaryPositions(1, 4, 9);
        assertEquals(List.of(1, 2, 3, 4), positions);
    }

    @Test
    void getDrawerRegister_shouldChunkEntriesIntoRangesOfConfiguredSize() throws Exception {
        int chunkSize = DataManager.getInstance().getConfiguration().getCardCatalogRegisterChunkSize();
        int entryCount = chunkSize * 2 + 5;
        List<String> labels = new ArrayList<>();
        for (int i = 1; i <= entryCount; i++) {
            labels.add("title" + i);
        }
        SolrSearchIndex searchIndex = registerSearchIndex(labels);

        List<CardCatalogRegisterRange> ranges = drawerRegister(managerWith(searchIndex), entryCount);
        assertEquals(3, ranges.size());
        assertEquals(1, ranges.get(0).getFrom());
        assertEquals(chunkSize, ranges.get(0).getTo());
        assertEquals("title1", ranges.get(0).getFromLabel());
        assertEquals("title" + chunkSize, ranges.get(0).getToLabel());
        assertEquals(chunkSize + 1, ranges.get(1).getFrom());
        assertEquals(chunkSize * 2, ranges.get(1).getTo());
        assertEquals(chunkSize * 2 + 1, ranges.get(2).getFrom());
        assertEquals(entryCount, ranges.get(2).getTo());
        assertEquals("title" + entryCount, ranges.get(2).getToLabel());
        Mockito.verify(searchIndex, Mockito.times(1)).search(anyString(), eq(0), eq(entryCount), anyList(), isNull(), anyList());
    }

    @Test
    void getDrawerRegister_shouldUseNearestTitledEntryWithinEachChunk() throws Exception {
        int chunkSize = DataManager.getInstance().getConfiguration().getCardCatalogRegisterChunkSize();
        assertTrue(chunkSize >= 4);
        List<String> labels = new ArrayList<>(Collections.nCopies(chunkSize * 2, " "));
        labels.set(0, null);
        labels.set(1, "Abel");
        labels.set(chunkSize - 2, "Adler");
        labels.set(chunkSize - 1, "");

        List<CardCatalogRegisterRange> ranges = drawerRegister(managerWith(registerSearchIndex(labels)), labels.size());
        assertEquals(2, ranges.size());
        assertEquals("Abel", ranges.get(0).getFromLabel());
        assertEquals("Adler", ranges.get(0).getToLabel());
        assertEquals("", ranges.get(1).getFromLabel());
        assertEquals("", ranges.get(1).getToLabel());
    }

    @Test
    void getDrawerRegister_shouldReturnEmptyListForEmptyDrawerWithoutSolrQuery() throws Exception {
        SolrSearchIndex searchIndex = registerSearchIndex(Collections.emptyList());
        assertTrue(drawerRegister(managerWith(searchIndex), 0).isEmpty());
        Mockito.verifyNoInteractions(searchIndex);
    }

    @Test
    void getDrawerRegister_shouldStreamAcrossSolrPages() throws Exception {
        List<String> labels = new ArrayList<>();
        for (int i = 1; i <= 1005; i++) {
            labels.add("title" + i);
        }
        SolrSearchIndex searchIndex = registerSearchIndex(labels);

        List<CardCatalogRegisterRange> ranges = drawerRegister(managerWith(searchIndex), labels.size());
        assertFalse(ranges.isEmpty());
        assertEquals(1, ranges.get(0).getFrom());
        assertEquals(1005, ranges.get(ranges.size() - 1).getTo());
        assertEquals("title1005", ranges.get(ranges.size() - 1).getToLabel());
        for (int i = 1; i < ranges.size(); i++) {
            assertEquals(ranges.get(i - 1).getTo() + 1, ranges.get(i).getFrom());
        }
        Mockito.verify(searchIndex).search(anyString(), eq(0), eq(1000), anyList(), isNull(), anyList());
        Mockito.verify(searchIndex).search(anyString(), eq(0), eq(5), anyList(), isNull(), anyList());
    }

    @Test
    void getBoundaryPositions_shouldAlwaysAllowZoomProgress() {
        CardCatalogManager manager = new CardCatalogManager();
        CardCatalogDrawer drawer = new CardCatalogDrawer("drawer", "iddoc", "A", 100);
        assertEquals(2, CardCatalogManager.normalizeZoomFactor(1));
        assertEquals(CardCatalogManager.MAX_BOUNDARIES - 1, CardCatalogManager.normalizeZoomFactor(Integer.MAX_VALUE));
        List<Integer> positions = manager.getBoundaryPositions(drawer, 1, 100);
        assertTrue(positions.size() >= 3);
        assertTrue(positions.get(1) < 100);
    }

    @Test
    void registerCache_shouldBeKeyedByChunkSize() {
        CardCatalogDrawer drawer = new CardCatalogDrawer("drawer", "iddoc", "A", 10);
        List<CardCatalogRegisterRange> ranges = List.of(new CardCatalogRegisterRange(1, 10, "A", "Z"));
        drawer.setRegisterRanges(10, ranges);
        assertEquals(ranges, drawer.getRegisterRanges(10));
        assertNull(drawer.getRegisterRanges(5));
    }

    @Test
    void restrictedDescendantDetection_shouldIgnoreOpenAccessAndRootOnlyConditions() {
        SolrDocument root = new SolrDocument();
        root.setField(SolrConstants.ACCESSCONDITION, "restricted");
        FacetField rootOnly = new FacetField(SolrConstants.ACCESSCONDITION);
        rootOnly.add(SolrConstants.OPEN_ACCESS_VALUE, 10);
        rootOnly.add("restricted", 1);
        assertFalse(CardCatalogManager.hasRestrictedDescendantConditions(root, rootOnly));

        FacetField withRestrictedChild = new FacetField(SolrConstants.ACCESSCONDITION);
        withRestrictedChild.add(SolrConstants.OPEN_ACCESS_VALUE, 10);
        withRestrictedChild.add("restricted", 2);
        assertTrue(CardCatalogManager.hasRestrictedDescendantConditions(root, withRestrictedChild));
    }

    @Test
    void getCatalog_shouldBuildOnceForConcurrentRequests() throws Exception {
        SolrSearchIndex searchIndex = emptyCatalogSearchIndex();
        CountDownLatch buildEntered = new CountDownLatch(1);
        CountDownLatch releaseBuild = new CountDownLatch(1);
        AtomicInteger builds = new AtomicInteger();
        QueryResponse facets = Mockito.mock(QueryResponse.class);
        Mockito.when(searchIndex.searchFacetsAndStatistics(anyString(), isNull(), anyList(), eq(1), eq(false)))
                .thenAnswer(invocation -> {
                    builds.incrementAndGet();
                    buildEntered.countDown();
                    assertTrue(releaseBuild.await(5, TimeUnit.SECONDS));
                    return facets;
                });
        CardCatalogManager manager = managerWith(searchIndex);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Optional<CardCatalogIndex>> first = pool.submit(() -> manager.getCatalog("archive"));
            assertTrue(buildEntered.await(5, TimeUnit.SECONDS));
            Future<Optional<CardCatalogIndex>> second = pool.submit(() -> manager.getCatalog("archive"));
            assertFalse(second.isDone());
            releaseBuild.countDown();
            assertTrue(first.get(5, TimeUnit.SECONDS).isPresent());
            assertTrue(second.get(5, TimeUnit.SECONDS).isPresent());
            assertEquals(1, builds.get());
        } finally {
            releaseBuild.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void unloadCatalogs_shouldNotAllowInFlightBuildToRepopulateCache() throws Exception {
        SolrSearchIndex searchIndex = emptyCatalogSearchIndex();
        CountDownLatch buildEntered = new CountDownLatch(1);
        CountDownLatch releaseBuild = new CountDownLatch(1);
        AtomicInteger builds = new AtomicInteger();
        QueryResponse facets = Mockito.mock(QueryResponse.class);
        Mockito.when(searchIndex.searchFacetsAndStatistics(anyString(), isNull(), anyList(), eq(1), eq(false)))
                .thenAnswer(invocation -> {
                    if (builds.incrementAndGet() == 1) {
                        buildEntered.countDown();
                        assertTrue(releaseBuild.await(5, TimeUnit.SECONDS));
                    }
                    return facets;
                });
        CardCatalogManager manager = managerWith(searchIndex);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Optional<CardCatalogIndex>> build = pool.submit(() -> manager.getCatalog("archive"));
            assertTrue(buildEntered.await(5, TimeUnit.SECONDS));
            CountDownLatch invalidationStarted = new CountDownLatch(1);
            Future<Integer> invalidation = pool.submit(() -> {
                invalidationStarted.countDown();
                return manager.unloadCatalogs(List.of("archive"));
            });
            assertTrue(invalidationStarted.await(5, TimeUnit.SECONDS));
            assertFalse(invalidation.isDone());
            releaseBuild.countDown();
            assertTrue(build.get(5, TimeUnit.SECONDS).isPresent());
            assertEquals(1, invalidation.get(5, TimeUnit.SECONDS));
            assertTrue(manager.getCatalog("archive").isPresent());
            assertEquals(2, builds.get());
        } finally {
            releaseBuild.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void getEntryRange_shouldNotLinkAmbiguousAssociatedRecords() throws Exception {
        SolrSearchIndex searchIndex = Mockito.mock(SolrSearchIndex.class);
        SolrDocument card = new SolrDocument();
        card.setField(SolrConstants.EAD_NODE_ID, "shared");
        card.setField(SolrConstants.TITLE, "Card");
        card.setField("SORTNUM_ARCHIVE_ORDER", 0);
        SolrDocumentList cards = new SolrDocumentList();
        cards.add(card);
        cards.setNumFound(1);
        QueryResponse cardsResponse = Mockito.mock(QueryResponse.class);
        Mockito.when(cardsResponse.getResults()).thenReturn(cards);
        Mockito.when(searchIndex.search(anyString(), eq(0), eq(1), anyList(), isNull(), anyList())).thenReturn(cardsResponse);

        FacetField recordsFacet = new FacetField(SolrConstants.EAD_NODE_ID);
        recordsFacet.add("shared", 2);
        QueryResponse recordsFacetResponse = Mockito.mock(QueryResponse.class);
        Mockito.when(recordsFacetResponse.getFacetField(SolrConstants.EAD_NODE_ID)).thenReturn(recordsFacet);
        Mockito.when(searchIndex.searchFacetsAndStatistics(anyString(), isNull(), anyList(), eq(1), eq(false)))
                .thenReturn(recordsFacetResponse);

        CardCatalogEntry entry = managerWith(searchIndex)
                .getEntryRange(new CardCatalogDrawer("drawer", "iddoc", "A", 1), 1, 1).get(0);
        assertEquals("shared", entry.getId());
        assertNull(entry.getRecordPi());
        assertFalse(entry.isHasImage());
        Mockito.verify(searchIndex, Mockito.times(1)).search(anyString(), anyInt(), anyInt(), anyList(), isNull(), anyList());
    }

    @Test
    void getEntryRange_shouldLinkOnlyUniqueAssociatedRecord() throws Exception {
        SolrSearchIndex searchIndex = Mockito.mock(SolrSearchIndex.class);
        QueryResponse cardResponse = cardPage(List.of("unique"), 1, 0);
        SolrDocument record = new SolrDocument();
        record.setField(SolrConstants.EAD_NODE_ID, "unique");
        record.setField(SolrConstants.PI, "record");
        record.setField(SolrConstants.BOOL_IMAGEAVAILABLE, true);
        SolrDocumentList records = new SolrDocumentList();
        records.add(record);
        records.setNumFound(1);
        QueryResponse recordResponse = Mockito.mock(QueryResponse.class);
        Mockito.when(recordResponse.getResults()).thenReturn(records);
        Mockito.when(searchIndex.search(anyString(), anyInt(), anyInt(), anyList(), isNull(), anyList()))
                .thenAnswer(invocation -> ((String) invocation.getArgument(0)).contains("DOCTYPE:ARCHIVE")
                        ? cardResponse : recordResponse);
        FacetField facet = new FacetField(SolrConstants.EAD_NODE_ID);
        facet.add("unique", 1);
        QueryResponse facetResponse = Mockito.mock(QueryResponse.class);
        Mockito.when(facetResponse.getFacetField(SolrConstants.EAD_NODE_ID)).thenReturn(facet);
        Mockito.when(searchIndex.searchFacetsAndStatistics(anyString(), isNull(), anyList(), eq(1), eq(false)))
                .thenReturn(facetResponse);

        CardCatalogEntry entry = managerWith(searchIndex)
                .getEntryRange(new CardCatalogDrawer("drawer", "iddoc", "A", 1), 1, 1).get(0);
        assertEquals("record", entry.getRecordPi());
        assertTrue(entry.isHasImage());
    }

    @Test
    void findEntryPosition_shouldUseVerifiedIndexedSiblingOrder() throws Exception {
        SolrSearchIndex searchIndex = Mockito.mock(SolrSearchIndex.class);
        SolrDocument location = new SolrDocument();
        location.setField(SolrConstants.IDDOC_PARENT, "drawerDoc");
        location.setField("SORTNUM_ARCHIVE_ORDER", 5);
        Mockito.when(searchIndex.getFirstDoc(anyString(), anyList())).thenReturn(location);
        QueryResponse page = cardPage(List.of("target"), 1, 5);
        Mockito.when(searchIndex.search(anyString(), eq(0), eq(1), anyList(), isNull(), anyList())).thenReturn(page);

        CardCatalogDrawer drawer = new CardCatalogDrawer("drawer", "drawerDoc", "A", 10);
        CardCatalogIndex index = new CardCatalogIndex("archive", "Archive", 1, List.of(drawer), false);
        assertEquals(6, managerWith(searchIndex).findEntryPosition(index, "target").orElseThrow().getRight());
        Mockito.verify(searchIndex, Mockito.times(1)).search(anyString(), anyInt(), anyInt(), anyList(), isNull(), anyList());
    }

    @Test
    void findEntryPosition_shouldScanWhenIndexedOrderDoesNotMatchSortedPage() throws Exception {
        SolrSearchIndex searchIndex = Mockito.mock(SolrSearchIndex.class);
        SolrDocument location = new SolrDocument();
        location.setField(SolrConstants.IDDOC, "targetDoc");
        location.setField(SolrConstants.IDDOC_PARENT, "drawerDoc");
        location.setField("SORTNUM_ARCHIVE_ORDER", 1);
        Mockito.when(searchIndex.getFirstDoc(anyString(), anyList())).thenReturn(location);
        Mockito.when(searchIndex.search(anyString(), eq(0), eq(1), anyList(), isNull(), anyList())).thenAnswer(invocation -> {
            String query = invocation.getArgument(0);
            return query.contains("[1 TO 1]") ? cardPage(List.of("other"), 1, 1) : cardPage(List.of("first"), 1, 0);
        });
        QueryResponse completePage = cardPage(List.of("first", "other", "target"), 3, 0);
        Mockito.when(searchIndex.search(anyString(), eq(0), eq(3), anyList(), isNull(), anyList()))
                .thenReturn(completePage);

        CardCatalogDrawer drawer = new CardCatalogDrawer("drawer", "drawerDoc", "A", 3);
        CardCatalogIndex index = new CardCatalogIndex("archive", "Archive", 1, List.of(drawer), false);
        assertEquals(3, managerWith(searchIndex).findEntryPosition(index, "target").orElseThrow().getRight());
    }

    @Test
    void findEntryPosition_shouldCountPrecedingSiblingsInsteadOfScanning() throws Exception {
        SolrSearchIndex searchIndex = Mockito.mock(SolrSearchIndex.class);
        SolrDocument location = new SolrDocument();
        location.setField(SolrConstants.IDDOC, "targetDoc");
        location.setField(SolrConstants.IDDOC_PARENT, "drawerDoc");
        location.setField("SORTNUM_ARCHIVE_ORDER", 7);
        Mockito.when(searchIndex.getFirstDoc(anyString(), anyList())).thenReturn(location);
        Mockito.when(searchIndex.search(anyString(), eq(0), eq(1), anyList(), isNull(), anyList())).thenAnswer(invocation -> {
            String query = invocation.getArgument(0);
            return query.contains("[3 TO 3]") ? cardPage(List.of("target"), 1, 3) : cardPage(List.of("other"), 1, 7);
        });
        Mockito.when(searchIndex.getHitCount(anyString())).thenAnswer(invocation -> {
            String query = invocation.getArgument(0);
            return query.contains("[* TO 6]") ? 2L : 1L;
        });

        CardCatalogDrawer drawer = new CardCatalogDrawer("drawer", "drawerDoc", "A", 1000);
        CardCatalogIndex index = new CardCatalogIndex("archive", "Archive", 1, List.of(drawer), false);
        assertEquals(4, managerWith(searchIndex).findEntryPosition(index, "target").orElseThrow().getRight());
        Mockito.verify(searchIndex, Mockito.times(2)).getHitCount(anyString());
        Mockito.verify(searchIndex, Mockito.times(2)).search(anyString(), anyInt(), anyInt(), anyList(), isNull(), anyList());
    }

    @Test
    void findEntryPosition_shouldGiveUpAfterTheBoundedScan() throws Exception {
        SolrSearchIndex searchIndex = Mockito.mock(SolrSearchIndex.class);
        SolrDocument location = new SolrDocument();
        location.setField(SolrConstants.IDDOC_PARENT, "drawerDoc");
        Mockito.when(searchIndex.getFirstDoc(anyString(), anyList())).thenReturn(location);
        int pageSize = CardCatalogManager.MAX_ENTRIES_PER_REQUEST;
        Mockito.when(searchIndex.search(anyString(), eq(0), eq(pageSize), anyList(), isNull(), anyList())).thenAnswer(invocation -> {
            Matcher range = Pattern.compile("\\[(\\d+) TO (\\d+)\\]").matcher(invocation.getArgument(0));
            assertTrue(range.find());
            int start = Integer.parseInt(range.group(1));
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < pageSize; i++) {
                ids.add("card" + (start + i));
            }
            return cardPage(ids, pageSize, start);
        });

        CardCatalogDrawer drawer = new CardCatalogDrawer("drawer", "drawerDoc", "A", 100_000);
        CardCatalogIndex index = new CardCatalogIndex("archive", "Archive", 1, List.of(drawer), false);
        assertTrue(managerWith(searchIndex).findEntryPosition(index, "target").isEmpty());
        Mockito.verify(searchIndex, Mockito.times(CardCatalogManager.MAX_POSITION_SCAN_PAGES))
                .search(anyString(), anyInt(), anyInt(), anyList(), isNull(), anyList());
        Mockito.verify(searchIndex, Mockito.never()).getHitCount(anyString());
    }

    @Test
    void getEntriesAtPositions_shouldResolveSparseRanksInOneQuery() throws Exception {
        SolrSearchIndex searchIndex = Mockito.mock(SolrSearchIndex.class);
        SolrDocumentList cards = new SolrDocumentList();
        for (int rank : List.of(0, 4, 9)) {
            SolrDocument card = new SolrDocument();
            card.setField(SolrConstants.EAD_NODE_ID, "id" + rank);
            card.setField("SORTNUM_ARCHIVE_ORDER", rank);
            cards.add(card);
        }
        cards.setNumFound(3);
        QueryResponse cardsResponse = Mockito.mock(QueryResponse.class);
        Mockito.when(cardsResponse.getResults()).thenReturn(cards);
        Mockito.when(searchIndex.search(anyString(), eq(0), eq(3), anyList(), isNull(), anyList())).thenReturn(cardsResponse);
        QueryResponse noRecords = Mockito.mock(QueryResponse.class);
        Mockito.when(searchIndex.searchFacetsAndStatistics(anyString(), isNull(), anyList(), eq(1), eq(false)))
                .thenReturn(noRecords);

        List<CardCatalogEntry> entries = managerWith(searchIndex)
                .getEntriesAtPositions(new CardCatalogDrawer("drawer", "iddoc", "A", 10), List.of(10, 1, 5));
        assertEquals(List.of("id9", "id0", "id4"), entries.stream().map(CardCatalogEntry::getId).toList());
        Mockito.verify(searchIndex, Mockito.times(1)).search(anyString(), anyInt(), anyInt(), anyList(), isNull(), anyList());
    }

    @Test
    void getEntryRange_shouldUseOldOffsetIndexWhenRanksAreMissing() throws Exception {
        SolrSearchIndex searchIndex = Mockito.mock(SolrSearchIndex.class);
        SolrDocumentList missingRanks = new SolrDocumentList();
        missingRanks.setNumFound(0);
        QueryResponse missingResponse = Mockito.mock(QueryResponse.class);
        Mockito.when(missingResponse.getResults()).thenReturn(missingRanks);
        QueryResponse oldIndexResponse = cardPage(List.of("target"), 10, 0);
        Mockito.when(searchIndex.search(anyString(), anyInt(), eq(1), anyList(), isNull(), anyList()))
                .thenAnswer(invocation -> ((String) invocation.getArgument(0)).contains("SORTNUM_ARCHIVE_ORDER:[")
                        ? missingResponse : oldIndexResponse);
        QueryResponse noRecords = Mockito.mock(QueryResponse.class);
        Mockito.when(searchIndex.searchFacetsAndStatistics(anyString(), isNull(), anyList(), eq(1), eq(false)))
                .thenReturn(noRecords);

        CardCatalogEntry entry = managerWith(searchIndex)
                .getEntryRange(new CardCatalogDrawer("drawer", "iddoc", "A", 10), 6, 6).get(0);
        assertEquals("target", entry.getId());
        ArgumentCaptor<Integer> starts = ArgumentCaptor.forClass(Integer.class);
        Mockito.verify(searchIndex, Mockito.times(2)).search(anyString(), starts.capture(), eq(1), anyList(), isNull(), anyList());
        assertTrue(starts.getAllValues().containsAll(List.of(0, 5)));
    }

    @Test
    void getEntryRange_shouldRejectStaleDrawerCountOnOffsetFallback() throws Exception {
        SolrSearchIndex searchIndex = Mockito.mock(SolrSearchIndex.class);
        SolrDocumentList missingRanks = new SolrDocumentList();
        missingRanks.setNumFound(0);
        QueryResponse missingResponse = Mockito.mock(QueryResponse.class);
        Mockito.when(missingResponse.getResults()).thenReturn(missingRanks);
        QueryResponse oldIndexResponse = cardPage(List.of("target"), 11, 0);
        Mockito.when(searchIndex.search(anyString(), anyInt(), eq(1), anyList(), isNull(), anyList()))
                .thenAnswer(invocation -> ((String) invocation.getArgument(0)).contains("SORTNUM_ARCHIVE_ORDER:[")
                        ? missingResponse : oldIndexResponse);

        CardCatalogManager manager = managerWith(searchIndex);
        assertThrows(IllegalStateException.class,
                () -> manager.getEntryRange(new CardCatalogDrawer("drawer", "iddoc", "A", 10), 6, 6));
    }

    @Test
    void getCatalog_shouldBoundStructureQueries() throws Exception {
        SolrSearchIndex searchIndex = emptyCatalogSearchIndex();
        FacetField parents = new FacetField(SolrConstants.IDDOC_PARENT);
        for (int i = 0; i < 257; i++) {
            parents.add("parent" + i, 1);
        }
        QueryResponse facets = Mockito.mock(QueryResponse.class);
        Mockito.when(facets.getFacetField(SolrConstants.IDDOC_PARENT)).thenReturn(parents);
        Mockito.when(facets.getFacetField(SolrConstants.ACCESSCONDITION)).thenReturn(new FacetField(SolrConstants.ACCESSCONDITION));
        Mockito.when(searchIndex.searchFacetsAndStatistics(anyString(), isNull(), anyList(), eq(1), eq(false))).thenReturn(facets);
        Mockito.when(searchIndex.search(anyString(), anyInt(), isNull(), anyList())).thenAnswer(invocation -> {
            Matcher matcher = Pattern.compile("parent\\d+").matcher(invocation.getArgument(0));
            SolrDocumentList docs = new SolrDocumentList();
            while (matcher.find()) {
                SolrDocument doc = new SolrDocument();
                doc.setField(SolrConstants.IDDOC, matcher.group());
                docs.add(doc);
            }
            docs.setNumFound(docs.size());
            return docs;
        });

        assertTrue(managerWith(searchIndex).getCatalog("archive").isPresent());
        ArgumentCaptor<Integer> rows = ArgumentCaptor.forClass(Integer.class);
        Mockito.verify(searchIndex, Mockito.times(2)).search(anyString(), rows.capture(), isNull(), anyList());
        assertTrue(rows.getAllValues().containsAll(List.of(256, 1)));
    }

    @Test
    void getCatalog_shouldRejectIncompleteStructureBatches() throws Exception {
        SolrSearchIndex searchIndex = emptyCatalogSearchIndex();
        FacetField parents = new FacetField(SolrConstants.IDDOC_PARENT);
        parents.add("parent1", 1);
        QueryResponse facets = Mockito.mock(QueryResponse.class);
        Mockito.when(facets.getFacetField(SolrConstants.IDDOC_PARENT)).thenReturn(parents);
        Mockito.when(facets.getFacetField(SolrConstants.ACCESSCONDITION)).thenReturn(new FacetField(SolrConstants.ACCESSCONDITION));
        Mockito.when(searchIndex.searchFacetsAndStatistics(anyString(), isNull(), anyList(), eq(1), eq(false))).thenReturn(facets);
        Mockito.when(searchIndex.search(anyString(), anyInt(), isNull(), anyList())).thenReturn(new SolrDocumentList());

        CardCatalogManager manager = managerWith(searchIndex);
        assertThrows(IllegalStateException.class, () -> manager.getCatalog("archive"));
    }

    @Test
    void getCatalog_shouldRetryWhenRootChangesDuringBuild() throws Exception {
        SolrSearchIndex searchIndex = Mockito.mock(SolrSearchIndex.class);
        SolrDocument first = new SolrDocument();
        first.setField(SolrConstants.PI, "archive");
        first.setField(SolrConstants.DATEINDEXED, 1L);
        SolrDocument updated = new SolrDocument();
        updated.setField(SolrConstants.PI, "archive");
        updated.setField(SolrConstants.DATEINDEXED, 2L);
        Mockito.when(searchIndex.getFirstDoc(anyString(), anyList())).thenReturn(first, updated, updated);
        QueryResponse noParents = Mockito.mock(QueryResponse.class);
        Mockito.when(searchIndex.searchFacetsAndStatistics(anyString(), isNull(), anyList(), eq(1), eq(false)))
                .thenReturn(noParents);

        CardCatalogIndex index = managerWith(searchIndex).getCatalog("archive").orElseThrow();
        assertEquals(2L, index.getDateIndexed());
        Mockito.verify(searchIndex, Mockito.times(2)).searchFacetsAndStatistics(anyString(), isNull(), anyList(), eq(1), eq(false));
    }

    private static CardCatalogManager managerWith(SolrSearchIndex searchIndex) {
        CardCatalogManager manager = Mockito.spy(new CardCatalogManager());
        Mockito.doReturn(searchIndex).when(manager).getSearchIndex();
        return manager;
    }

    private static List<CardCatalogRegisterRange> drawerRegister(CardCatalogManager manager, int entryCount) throws Exception {
        CardCatalogDrawer drawer = new CardCatalogDrawer("drawer", "drawerDoc", "A", entryCount);
        CardCatalogIndex index = new CardCatalogIndex("archive", "Archive", 1, List.of(drawer), false);
        return manager.getDrawerRegister(index, drawer).orElseThrow();
    }

    private static SolrSearchIndex registerSearchIndex(List<String> labels) throws Exception {
        SolrSearchIndex searchIndex = Mockito.mock(SolrSearchIndex.class);
        Mockito.when(searchIndex.search(anyString(), eq(0), anyInt(), anyList(), isNull(), anyList())).thenAnswer(invocation -> {
            String query = invocation.getArgument(0);
            Matcher matcher = Pattern.compile("SORTNUM_ARCHIVE_ORDER:\\[(\\d+) TO (\\d+)\\]").matcher(query);
            assertTrue(matcher.find(), query);
            int start = Integer.parseInt(matcher.group(1));
            int end = Integer.parseInt(matcher.group(2));
            int rows = invocation.getArgument(2);
            assertEquals(start + rows - 1, end);
            assertTrue(end < labels.size());
            SolrDocumentList docs = new SolrDocumentList();
            for (int rank = start; rank <= end; rank++) {
                SolrDocument doc = new SolrDocument();
                doc.setField(SolrConstants.EAD_NODE_ID, "id" + (rank + 1));
                doc.setField(SolrConstants.TITLE, labels.get(rank));
                doc.setField("SORTNUM_ARCHIVE_ORDER", rank);
                docs.add(doc);
            }
            docs.setNumFound(docs.size());
            QueryResponse response = Mockito.mock(QueryResponse.class);
            Mockito.when(response.getResults()).thenReturn(docs);
            return response;
        });
        return searchIndex;
    }

    private static SolrSearchIndex emptyCatalogSearchIndex() throws Exception {
        SolrSearchIndex searchIndex = Mockito.mock(SolrSearchIndex.class);
        SolrDocument root = new SolrDocument();
        root.setField(SolrConstants.PI, "archive");
        root.setField(SolrConstants.TITLE, "Archive");
        root.setField(SolrConstants.DATEINDEXED, 1L);
        Mockito.when(searchIndex.getFirstDoc(anyString(), anyList())).thenReturn(root);
        return searchIndex;
    }

    private static QueryResponse cardPage(List<String> ids, long numFound, int firstRank) {
        SolrDocumentList docs = new SolrDocumentList();
        for (int i = 0; i < ids.size(); i++) {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.EAD_NODE_ID, ids.get(i));
            doc.setField("SORTNUM_ARCHIVE_ORDER", firstRank + i);
            docs.add(doc);
        }
        docs.setNumFound(numFound);
        QueryResponse response = Mockito.mock(QueryResponse.class);
        Mockito.when(response.getResults()).thenReturn(docs);
        return response;
    }
}
