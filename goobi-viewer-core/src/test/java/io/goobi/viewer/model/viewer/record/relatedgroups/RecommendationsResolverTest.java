package io.goobi.viewer.model.viewer.record.relatedgroups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Random;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.managedbeans.ImageDeliveryBean;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.ViewManager;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;

class RecommendationsResolverTest {

    /** Sentinel access-control suffix returned by the mocked {@link SearchHelper#getAllSuffixes()}. */
    private static final String ACCESS_SUFFIX = " +(ACCESSCONDITION:\"OPENACCESS\")";

    private SolrSearchIndex searchIndex;
    private Configuration config;
    private DataManager dataManager;
    private ImageDeliveryBean imageDelivery;
    private ViewManager viewManager;
    private StructElement topStruct;
    private MockedStatic<SearchHelper> searchHelperStatic;

    @BeforeEach
    void setUp() throws Exception {
        searchIndex = mock(SolrSearchIndex.class);
        config = mock(Configuration.class);
        dataManager = mock(DataManager.class);
        when(dataManager.getConfiguration()).thenReturn(config);
        when(dataManager.getSearchIndex()).thenReturn(searchIndex);

        when(config.getSidebarWidgetRecommendationsMaxResults()).thenReturn(4);
        when(config.getSidebarWidgetRecommendationsIdentifierFields()).thenReturn(List.of("IdentifierRelatedWork"));
        when(config.getSidebarWidgetRecommendationsFallbackFields()).thenReturn(List.of("MD_TOPIC"));

        ThumbnailHandler thumbs = mock(ThumbnailHandler.class);
        when(thumbs.getThumbnailUrl(any(SolrDocument.class))).thenReturn("http://example/thumb.jpg");
        imageDelivery = mock(ImageDeliveryBean.class);
        when(imageDelivery.getThumbs()).thenReturn(thumbs);

        topStruct = mock(StructElement.class);
        when(topStruct.getPi()).thenReturn("PI_CURRENT");
        viewManager = mock(ViewManager.class);
        when(viewManager.getTopStructElement()).thenReturn(topStruct);

        // Isolate the access-control suffix from request/session state; the resolver appends its return
        // value to every Solr query it builds.
        searchHelperStatic = mockStatic(SearchHelper.class);
        searchHelperStatic.when(() -> SearchHelper.getAllSuffixes()).thenReturn(ACCESS_SUFFIX);
    }

    @AfterEach
    void tearDown() {
        if (searchHelperStatic != null) {
            searchHelperStatic.close();
        }
    }

    private static SolrDocument doc(String pi, String title) {
        SolrDocument d = new SolrDocument();
        d.setField(SolrConstants.PI, pi);
        d.setField(SolrConstants.TITLE, title);
        return d;
    }

    private static SolrDocumentList docList(SolrDocument... docs) {
        SolrDocumentList list = new SolrDocumentList();
        for (SolrDocument d : docs) {
            list.add(d);
        }
        return list;
    }

    @Test
    void resolve_shouldReturnEmptyListIfViewManagerNull() throws Exception {
        try (MockedStatic<DataManager> dmStatic = mockStatic(DataManager.class)) {
            dmStatic.when(DataManager::getInstance).thenReturn(dataManager);
            List<GroupMemberDetail> result = new RecommendationsResolver(imageDelivery, new Random(1L)).resolve(null);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void resolve_shouldQueryTargetFieldWhenIdentifierPresent() throws Exception {
        when(topStruct.getMetadataValues("IdentifierRelatedWork")).thenReturn(List.of("AC13373468"));
        when(searchIndex.search(anyString(), anyInt(), any(), anyList())).thenReturn(docList(doc("PI_A", "Work A"), doc("PI_B", "Work B")));

        try (MockedStatic<DataManager> dmStatic = mockStatic(DataManager.class)) {
            dmStatic.when(DataManager::getInstance).thenReturn(dataManager);
            List<GroupMemberDetail> result = new RecommendationsResolver(imageDelivery, new Random(1L)).resolve(viewManager);

            assertEquals(2, result.size());
            assertEquals("PI_A", result.get(0).getPi());

            ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
            org.mockito.Mockito.verify(searchIndex).search(queryCaptor.capture(), anyInt(), any(), anyList());
            String query = queryCaptor.getValue();
            assertTrue(query.contains("PI:(AC13373468)"), query);
            assertTrue(query.contains("-PI:PI_CURRENT"), query);
            // Security: the access-control suffix must be appended to the query
            assertTrue(query.contains("ACCESSCONDITION"), query);
        }
    }

    @Test
    void resolve_shouldUseFallbackFieldsWhenNoIdentifier() throws Exception {
        when(topStruct.getMetadataValues("IdentifierRelatedWork")).thenReturn(List.of());
        when(topStruct.getMetadataValues("MD_TOPIC")).thenReturn(List.of("Arbeitsmarkt"));
        when(searchIndex.search(anyString(), anyInt(), any(), anyList())).thenReturn(docList(doc("PI_C", "Work C")));

        try (MockedStatic<DataManager> dmStatic = mockStatic(DataManager.class)) {
            dmStatic.when(DataManager::getInstance).thenReturn(dataManager);
            List<GroupMemberDetail> result = new RecommendationsResolver(imageDelivery, new Random(1L)).resolve(viewManager);

            assertEquals(1, result.size());
            ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
            org.mockito.Mockito.verify(searchIndex).search(queryCaptor.capture(), anyInt(), any(), anyList());
            assertTrue(queryCaptor.getValue().contains("MD_TOPIC:\"Arbeitsmarkt\""), queryCaptor.getValue());
            // Security: the access-control suffix must be appended to the query
            assertTrue(queryCaptor.getValue().contains("ACCESSCONDITION"), queryCaptor.getValue());
        }
    }

    @Test
    void resolve_shouldCapAtMaxResults() throws Exception {
        when(config.getSidebarWidgetRecommendationsMaxResults()).thenReturn(2);
        when(topStruct.getMetadataValues("IdentifierRelatedWork")).thenReturn(List.of("AC1"));
        when(searchIndex.search(anyString(), anyInt(), any(), anyList()))
                .thenReturn(docList(doc("PI_A", "A"), doc("PI_B", "B"), doc("PI_C", "C")));

        try (MockedStatic<DataManager> dmStatic = mockStatic(DataManager.class)) {
            dmStatic.when(DataManager::getInstance).thenReturn(dataManager);
            List<GroupMemberDetail> result = new RecommendationsResolver(imageDelivery, new Random(1L)).resolve(viewManager);
            assertEquals(2, result.size());
        }
    }

    @Test
    void resolve_shouldFillFromCollectionWhenEnabled() throws Exception {
        when(config.getSidebarWidgetRecommendationsMaxResults()).thenReturn(4);
        when(topStruct.getMetadataValues("IdentifierRelatedWork")).thenReturn(List.of("AC1"));
        when(topStruct.getMetadataValues(SolrConstants.DC)).thenReturn(List.of("collectionX"));
        when(searchIndex.search(anyString(), anyInt(), any(), anyList()))
                .thenReturn(docList(doc("PI_A", "A")))
                .thenReturn(docList(doc("PI_X", "X"), doc("PI_Y", "Y"), doc("PI_Z", "Z")));

        try (MockedStatic<DataManager> dmStatic = mockStatic(DataManager.class)) {
            dmStatic.when(DataManager::getInstance).thenReturn(dataManager);
            List<GroupMemberDetail> result = new RecommendationsResolver(imageDelivery, new Random(1L)).resolve(viewManager);
            assertEquals(4, result.size());
            assertEquals("PI_A", result.get(0).getPi());

            // Security: every query (incl. the collection-fill query) must carry the access-control suffix
            ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
            org.mockito.Mockito.verify(searchIndex, org.mockito.Mockito.atLeastOnce())
                    .search(queryCaptor.capture(), anyInt(), any(), anyList());
            for (String query : queryCaptor.getAllValues()) {
                assertTrue(query.contains("ACCESSCONDITION"), query);
            }
            assertTrue(queryCaptor.getAllValues().stream().anyMatch(q -> q.contains(SolrConstants.DC + ":")),
                    "Expected a collection-fill query on " + SolrConstants.DC);
        }
    }
}
