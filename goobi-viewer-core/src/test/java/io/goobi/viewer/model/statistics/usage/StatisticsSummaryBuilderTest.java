package io.goobi.viewer.model.statistics.usage;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Test;
import org.mockito.Mockito;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;

public class StatisticsSummaryBuilderTest {

    @Test
    public void test_filterResults() throws DAOException, IndexUnreachableException, PresentationException {
        
        IDAO dao = createDAOData();
        
        SolrSearchIndex searchIndex = createSolrRecords();
        createSolrStatistics(searchIndex);
        
        StatisticsSummaryBuilder builder = new StatisticsSummaryBuilder(dao, searchIndex);
        
        StatisticsSummaryFilter filter = StatisticsSummaryFilter.ofQuery("DC:test");
        StatisticsSummary summary = builder.loadSummary(filter);
        assertEquals(22, summary.getTypes().get(RequestType.RECORD_VIEW).getTotalRequests());
        assertEquals(4, summary.getTypes().get(RequestType.RECORD_VIEW).getUniqueRequests());
        
    }
    
    private void createSolrStatistics(SolrSearchIndex searchIndex) throws PresentationException, IndexUnreachableException {
        
        SolrDocumentList docs = new SolrDocumentList();
        docs.add(new SolrDocument(Map.of(
                StatisticsLuceneFields.getFieldName("PI_01"), new Long[] {12l, 2l, 0l, 0l, 0l, 0l},
                StatisticsLuceneFields.getFieldName("PI_04"), new Long[] {4l, 1l, 0l, 0l, 0l, 0l}
                )));
        docs.add(new SolrDocument(Map.of(
                StatisticsLuceneFields.getFieldName("PI_01"), new Long[] {6l, 1l, 0l, 0l, 0l, 0l},
                StatisticsLuceneFields.getFieldName("PI_04"), new Long[] {0l, 0l, 0l, 0l, 0l, 0l}
                )));
        
        Mockito.when(searchIndex.search(Mockito.contains("DOCTYPE:" + StatisticsLuceneFields.USAGE_STATISTICS_DOCTYPE), Mockito.anyList())).thenReturn(docs);
    }

    private SolrSearchIndex createSolrRecords() throws PresentationException, IndexUnreachableException {
        SolrDocumentList docs = new SolrDocumentList();
        docs.add(new SolrDocument(Collections.singletonMap(SolrConstants.PI, "PI_01")));
        docs.add(new SolrDocument(Collections.singletonMap(SolrConstants.PI, "PI_04")));
        SolrSearchIndex searchIndex = Mockito.mock(SolrSearchIndex.class);
        Mockito.when(searchIndex.search(Mockito.eq("+(DC:test) +ISWORK:*"), Mockito.anyList())).thenReturn(docs);
        return searchIndex;
    }

    private IDAO createDAOData() throws DAOException {
        DailySessionUsageStatistics day1 = new DailySessionUsageStatistics(LocalDate.of(2022, Month.JULY, 10), "viewer");
        SessionUsageStatistics session1 = new SessionUsageStatistics("1", "", "");
        session1.setRecordRequectCount(RequestType.RECORD_VIEW, "PI_01", 10);
        session1.setRecordRequectCount(RequestType.RECORD_VIEW, "PI_02", 12);
        day1.addSession(session1);
        
        SessionUsageStatistics session2 = new SessionUsageStatistics("2", "", "");
        session2.setRecordRequectCount(RequestType.RECORD_VIEW, "PI_01", 5);
        session2.setRecordRequectCount(RequestType.RECORD_VIEW, "PI_03", 17);
        day1.addSession(session2);
        
        DailySessionUsageStatistics day2 = new DailySessionUsageStatistics(LocalDate.of(2022, Month.JULY, 12), "viewer");
        SessionUsageStatistics session3 = new SessionUsageStatistics("3", "", "");
        session3.setRecordRequectCount(RequestType.RECORD_VIEW, "PI_02", 14);
        session3.setRecordRequectCount(RequestType.RECORD_VIEW, "PI_04", 4);
        day2.addSession(session3);
        
        SessionUsageStatistics session4 = new SessionUsageStatistics("4", "", "");
        session4.setRecordRequectCount(RequestType.RECORD_VIEW, "PI_03", 21);
        session4.setRecordRequectCount(RequestType.RECORD_VIEW, "PI_01", 3);
        day2.addSession(session4);
        
        IDAO dao = Mockito.mock(IDAO.class);
        Mockito.when(dao.getUsageStatistics(Mockito.any(), Mockito.any())).thenReturn(List.of(day1, day2));
        return dao;
    }

}
