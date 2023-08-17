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
package io.goobi.viewer.model.statistics.usage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;

public class StatisticsSummaryBuilderTest extends AbstractSolrEnabledTest {

    @Test
    public void test_filterResults() throws DAOException, IndexUnreachableException, PresentationException {

        IDAO dao = createDAOData();

        SolrSearchIndex searchIndex = createSolrRecords();
        createSolrStatistics(searchIndex);
        assertNotNull(searchIndex);

        StatisticsSummaryBuilder builder = new StatisticsSummaryBuilder(dao, searchIndex);

        StatisticsSummaryFilter filter = StatisticsSummaryFilter.ofQuery("DC:test");
        StatisticsSummary summary = builder.loadSummary(filter);
        assertEquals(22, summary.getTypes().get(RequestType.RECORD_VIEW).getTotalRequests());
        assertEquals(4, summary.getTypes().get(RequestType.RECORD_VIEW).getUniqueRequests());

    }

    private static void createSolrStatistics(SolrSearchIndex searchIndex) throws PresentationException, IndexUnreachableException {

        SolrDocumentList docs = new SolrDocumentList();
        docs.add(new SolrDocument(Map.of(
                StatisticsLuceneFields.DATE, Date.from(LocalDate.of(2022, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()),
                StatisticsLuceneFields.getFieldName("PI_01"), Arrays.asList(new Long[] { 12l, 2l, 0l, 0l, 0l, 0l }),
                StatisticsLuceneFields.getFieldName("PI_04"), Arrays.asList(new Long[] { 4l, 1l, 0l, 0l, 0l, 0l }))));
        docs.add(new SolrDocument(Map.of(
                StatisticsLuceneFields.DATE, Date.from(LocalDate.of(2022, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()),
                StatisticsLuceneFields.getFieldName("PI_01"), Arrays.asList(new Long[] { 6l, 1l, 0l, 0l, 0l, 0l }),
                StatisticsLuceneFields.getFieldName("PI_04"), Arrays.asList(new Long[] { 0l, 0l, 0l, 0l, 0l, 0l }))));
        QueryResponse resp = Mockito.mock(QueryResponse.class);
        Mockito.when(resp.getResults()).thenReturn(docs);
        Mockito.when(searchIndex.search(
                Mockito.contains("DOCTYPE:" + StatisticsLuceneFields.USAGE_STATISTICS_DOCTYPE),
                Mockito.anyList()))
                .thenReturn(docs);
    }

    private static SolrSearchIndex createSolrRecords() throws PresentationException, IndexUnreachableException {
        SolrDocumentList docs = new SolrDocumentList();
        QueryResponse resp = Mockito.mock(QueryResponse.class);
        Mockito.when(resp.getResults()).thenReturn(docs);
        docs.add(new SolrDocument(Map.of(StatisticsLuceneFields.DATE,
                Date.from(LocalDate.of(2022, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), SolrConstants.PI, "PI_01")));
        docs.add(new SolrDocument(Map.of(StatisticsLuceneFields.DATE,
                Date.from(LocalDate.of(2022, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), SolrConstants.PI, "PI_04")));
        SolrSearchIndex searchIndex = Mockito.mock(SolrSearchIndex.class);
        Mockito.when(searchIndex.search(
                Mockito.eq("+(DC:test) +(ISWORK:true ISANCHOR:true DOCTYPE:GROUP)"),
                Mockito.anyList())).thenReturn(docs);
        return searchIndex;
    }

    private static IDAO createDAOData() throws DAOException {
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

    /**
     * @see StatisticsSummaryBuilder#getFilteredIdentifierList(StatisticsSummaryFilter)
     * @verifies extract pi from filter correctly
     */
    @Test
    public void getFilteredIdentifierList_shouldExtractPiFromFilterCorrectly() throws Exception {
        StatisticsSummaryFilter filter = StatisticsSummaryFilter.forRecord(AbstractSolrEnabledTest.PI_KLEIUNIV);
        IDAO dao = createDAOData();
        List<String> result = new StatisticsSummaryBuilder(dao, DataManager.getInstance().getSearchIndex()).getFilteredIdentifierList(filter);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(AbstractSolrEnabledTest.PI_KLEIUNIV, result.get(0));
    }
}
