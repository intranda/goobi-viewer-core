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

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;

/**
 * Class collecting usage statistics data for a number of days to provide an overall summary for output
 * 
 * @author florian
 *
 */
public class StatisticsSummaryBuilder {

    private final Logger logger = LogManager.getLogger(StatisticsSummaryBuilder.class);

    /**
     * The DAO from which to query the usage statistics
     * 
     * @deprecated statistics are queried from SOLR (via {@link SolrSearchIndex})
     */
    @Deprecated(since = "22.08")
    private final IDAO dao;

    /**
     * The SOLR interface from which to query the usage statistics
     */
    private final SolrSearchIndex searchIndex;

    /**
     * Constructor using instances from {@link DataManager}
     * 
     * @throws DAOException
     */
    public StatisticsSummaryBuilder() throws DAOException {
        this(DataManager.getInstance().getDao(), DataManager.getInstance().getSearchIndex());
    }

    /**
     * Default constructor
     * 
     * @param dao the {@link IDAO} to set. May be null since it isn't used
     * @param searchIndex the {@link SolrSearchIndex} to set
     */
    public StatisticsSummaryBuilder(IDAO dao, SolrSearchIndex searchIndex) {
        this.dao = dao;
        this.searchIndex = searchIndex;
    }

    /**
     * Collect usage statistics from SOLR in a {@link StatisticsSummary}
     * 
     * @param filter a {@link StatisticsSummaryFilter} to filter results
     * @return a {@link StatisticsSummary}
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public StatisticsSummary loadSummary(StatisticsSummaryFilter filter) throws IndexUnreachableException, PresentationException {
        return loadFromSolr(filter);
    }

    /**
     * 
     * @param filter
     * @return {@link StatisticsSummary}
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private StatisticsSummary loadFromSolr(StatisticsSummaryFilter filter) throws IndexUnreachableException, PresentationException {
        List<String> identifiersToInclude = getFilteredIdentifierList(filter);
        if (filter.hasFilterQuery() && identifiersToInclude.isEmpty()) {
            throw new WebApplicationException("No records found matching filter " + filter.getFilterQuery());
        }
        List<String> fields = new ArrayList<>(getFieldListForRecords(identifiersToInclude));
        if (!fields.isEmpty()) {
            fields.add(StatisticsLuceneFields.DATE);
        }
        SolrDocumentList docs =
                // search(getSolrQuery(filter), fields);
                this.searchIndex.search(getSolrQuery(filter), fields);
        return docs.stream().reduce(StatisticsSummary.empty(), this::add, StatisticsSummary::add);
    }

    /**
     * 
     * @param s
     * @param d
     * @return {@link StatisticsSummary}
     */
    private StatisticsSummary add(StatisticsSummary s, SolrDocument d) {
        StatisticsSummary s2 = getStatisticsFromSolrDoc(d);
        return s.add(s2);
    }

    /**
     * 
     * @param doc
     * @return {@link StatisticsSummary}
     */
    @SuppressWarnings("unchecked")
    private StatisticsSummary getStatisticsFromSolrDoc(SolrDocument doc) {
        Long[] counts = new Long[] { 0L, 0L, 0L, 0L, 0L, 0L };
        for (String fieldName : doc.getFieldNames()) {
            if (fieldName.startsWith(StatisticsLuceneFields.RECORD_STATISTICS_PREFIX)) {
                try {
                    List<Long> values = (List<Long>) doc.getFieldValue(fieldName);
                    for (int i = 0; i < counts.length; i++) {
                        counts[i] += values.get(i);
                    }
                } catch (ClassCastException e) {
                    logger.warn("Envountered solr doc field of unexcepted type: '{}' : '{}'", fieldName, doc.getFieldValue(fieldName));
                }
            }
        }

        Map<RequestType, RequestTypeSummary> map = new EnumMap<>(RequestType.class);
        LocalDate date = getDate(doc);
        for (int i = 0; i < counts.length; i += 2) {
            RequestType type = RequestType.getTypeForTotalCountIndex(i);
            long total = counts[i];
            long unique = counts[i + 1];
            map.put(type, new RequestTypeSummary(total, unique, date, date));
        }
        return new StatisticsSummary(map);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<RequestType, RequestTypeSummary>> getRecordStatisticsFromSolrDoc(SolrDocument doc) {
        Map<String, List<Long>> countsMap = new HashMap<>();
        for (String fieldName : doc.getFieldNames()) {
            if (fieldName.startsWith(StatisticsLuceneFields.RECORD_STATISTICS_PREFIX)) {
                try {
                    List<Long> values = (List<Long>) doc.getFieldValue(fieldName);
                    countsMap.put(StatisticsLuceneFields.getPi(fieldName), values);
                } catch (ClassCastException e) {
                    logger.warn("Envountered solr doc field of unexcepted type: '{}' : '{}'", fieldName, doc.getFieldValue(fieldName));
                }
            }
        }

        Map<String, Map<RequestType, RequestTypeSummary>> map = new HashMap<>(); // new EnumMap<>(RequestType.class);
        LocalDate date = getDate(doc);

        for (Entry<String, List<Long>> entry : countsMap.entrySet()) {
            String pi = entry.getKey();
            List<Long> counts = entry.getValue();
            Map<RequestType, RequestTypeSummary> recordMap = new EnumMap<>(RequestType.class);
            for (int i = 0; i < counts.size(); i += 2) {
                RequestType type = RequestType.getTypeForTotalCountIndex(i);
                long total = counts.get(i);
                long unique = counts.get(i + 1);
                recordMap.put(type, new RequestTypeSummary(total, unique, date, date));
            }
            map.put(pi, recordMap);
        }
        return map;
    }

    private static LocalDate getDate(SolrDocument doc) {
        if (doc.containsKey(StatisticsLuceneFields.DATE)) {
            Date date = (Date) doc.getFieldValue(StatisticsLuceneFields.DATE);
            return new Timestamp(date.getTime()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }

    /**
     * 
     * @param identifiersToInclude
     * @return List<String> (immutable!)
     */
    private static List<String> getFieldListForRecords(List<String> identifiersToInclude) {
        return identifiersToInclude.stream().map(StatisticsLuceneFields::getFieldName).toList();
    }

    private static String getSolrQuery(StatisticsSummaryFilter filter) {
        StringBuilder sb = new StringBuilder();
        sb.append("+").append(SolrConstants.DOCTYPE).append(":").append(StatisticsLuceneFields.USAGE_STATISTICS_DOCTYPE);

        if (filter.isDateRange()) {
            sb.append(" +")
                    .append(StatisticsLuceneFields.DATE)
                    .append(":")
                    .append("[")
                    .append(StatisticsLuceneFields.SOLR_DATE_FORMATTER.format(filter.getStartDate().atStartOfDay()))
                    .append(" TO ")
                    .append(StatisticsLuceneFields.SOLR_DATE_FORMATTER.format(filter.getEndDate().atStartOfDay()))
                    .append("]");
        } else if (filter.hasStartDateRestriction()) {
            sb.append(" +")
                    .append(StatisticsLuceneFields.DATE)
                    .append(":")
                    .append("\"")
                    .append(StatisticsLuceneFields.SOLR_DATE_FORMATTER.format(filter.getStartDate().atStartOfDay()))
                    .append("\"");
        }
        return sb.toString();
    }

    /**
     * 
     * @param filter
     * @return List<String>
     * @throws IndexUnreachableException
     * @should extract pi from filter correctly
     */
    List<String> getFilteredIdentifierList(StatisticsSummaryFilter filter) throws IndexUnreachableException {
        List<String> identifiersToInclude = new ArrayList<>();
        if (StringUtils.isNotBlank(filter.getFilterQuery())) {
            try {
                String completeFilter = "+({}) +(ISWORK:true ISANCHOR:true DOCTYPE:GROUP)".replace("{}", filter.getFilterQuery());
                identifiersToInclude.addAll(
                        //search(completeFilter, Collections.singletonList(SolrConstants.PI))
                        this.searchIndex.search(completeFilter, Collections.singletonList(SolrConstants.PI))
                                .stream()
                                .map(doc -> doc.getFieldValue(SolrConstants.PI).toString())
                                .toList());
            } catch (PresentationException e) {
                throw new IndexUnreachableException(e.toString());
            }
        }
        return identifiersToInclude;
    }

    private static StatisticsSummary add(StatisticsSummary summary, DailySessionUsageStatistics dailyStats, List<String> identifiersToInclude) {
        StatisticsSummary dailyStatsSummary = new StatisticsSummary(dailyStats, identifiersToInclude);
        return summary.add(dailyStatsSummary);
    }
}
