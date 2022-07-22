package io.goobi.viewer.model.statistics.usage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final Logger logger = LoggerFactory.getLogger(StatisticsSummaryBuilder.class);
    
    private final IDAO dao;
    private final SolrSearchIndex searchIndex;
     
    public StatisticsSummaryBuilder() throws DAOException {
          this(DataManager.getInstance().getDao(), DataManager.getInstance().getSearchIndex());      
    }
    
    public StatisticsSummaryBuilder(IDAO dao, SolrSearchIndex searchIndex) {
        this.dao = dao;
        this.searchIndex = searchIndex;
    }
    
    public StatisticsSummary loadSummary(StatisticsSummaryFilter filter) throws DAOException, IndexUnreachableException, PresentationException {
        StatisticsSummary fromDAO = loadFromSolr(filter);
        return fromDAO;
    }
    
    /**
     * 
     * @param filter
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     * @deprecated statistics are loaded from solr, using the {@link #loadFromSolr(StatisticsSummaryFilter) method
     */
    @Deprecated
    private StatisticsSummary loadFromDAO(StatisticsSummaryFilter filter) throws DAOException, IndexUnreachableException {
        List<String> identifiersToInclude = getFilteredIdentifierList(filter);
        List<DailySessionUsageStatistics> days = this.dao.getUsageStatistics(filter.getStartDate(), filter.getEndDate());
        return days.stream().reduce(StatisticsSummary.empty(), (s,d) -> this.add(s, d, identifiersToInclude) , StatisticsSummary::add);
    }
    
    private StatisticsSummary loadFromSolr(StatisticsSummaryFilter filter) throws DAOException, IndexUnreachableException, PresentationException {
        List<String> identifiersToInclude = getFilteredIdentifierList(filter);
        SolrDocumentList docs = this.searchIndex.search(getSolrQuery(filter), getFieldListForRecords(identifiersToInclude));
        return docs.stream().reduce(StatisticsSummary.empty(), this::add , StatisticsSummary::add);
    }
    private StatisticsSummary add(StatisticsSummary s, SolrDocument d) {
        StatisticsSummary s2 = getStatisticsFromSolrDoc(d);
        return s.add(s2);
    }

    private StatisticsSummary getStatisticsFromSolrDoc(SolrDocument doc) {
        Long[] counts = new Long[] {0l,0l,0l,0l,0l,0l};
        for (String fieldName : doc.getFieldNames()) {
            try {
                Long[] values = (Long[])doc.getFieldValue(fieldName);
                for (int i = 0; i < counts.length; i++) {
                    counts[i] += values[i];
                }
            } catch(ClassCastException e) {
                logger.warn("Envountered solr doc field of unexcepted type: '{}' : '{}'",  fieldName, doc.getFieldValue(fieldName));
            }
        }

        Map<RequestType, RequestTypeSummary> map = new HashMap<>();
        for (int i = 0; i < counts.length; i+=2) {
                RequestType type = RequestType.getTypeForTotalCountIndex(i);
                long total = counts[i];
                long unique = counts[i+1];
                map.put(type, new RequestTypeSummary(total, unique));
        }
        return new StatisticsSummary(map);
    }

    private List<String> getFieldListForRecords(List<String> identifiersToInclude) {
        return identifiersToInclude.stream().map(StatisticsLuceneFields::getFieldName).collect(Collectors.toList());
    }

    private String getSolrQuery(StatisticsSummaryFilter filter) {
        StringBuilder sb = new StringBuilder();
        sb.append("+").append(SolrConstants.DOCTYPE).append(":").append(StatisticsLuceneFields.USAGE_STATISTICS_DOCTYPE);
        
        if(filter.isDateRange()) {
            sb.append(" +").append(StatisticsLuceneFields.DATE).append(":").append("[")
            .append(StatisticsLuceneFields.solrDateFormatter.format(filter.getStartDate()))
            .append(" TO ").append(StatisticsLuceneFields.solrDateFormatter.format(filter.getEndDate()));
        } else if(filter.hasStartDateRestriction()) {
            sb.append(" +").append(StatisticsLuceneFields.DATE).append(":").append(StatisticsLuceneFields.solrDateFormatter.format(filter.getStartDate()));
        }
        return sb.toString();
    }

    private List<String> getFilteredIdentifierList(StatisticsSummaryFilter filter) throws IndexUnreachableException {
        List<String> identifiersToInclude = new ArrayList<>();
        if(StringUtils.isNotBlank(filter.getFilterQuery())) {
            try {
            String completeFilter = "+({}) +ISWORK:*".replace("{}", filter.getFilterQuery());
                identifiersToInclude.addAll(this.searchIndex
                        .search(completeFilter, Collections.singletonList(SolrConstants.PI))
                        .stream().map(doc -> doc.getFieldValue(SolrConstants.PI).toString())
                        .collect(Collectors.toList()));
            } catch (PresentationException  e) {
                throw new IndexUnreachableException(e.toString());
            }
        }
        return identifiersToInclude;
    }
    
    private StatisticsSummary add(StatisticsSummary summary, DailySessionUsageStatistics dailyStats, List<String> identifiersToInclude) {
        StatisticsSummary dailyStatsSummary = new StatisticsSummary(dailyStats, identifiersToInclude);
        StatisticsSummary combined = summary.add(dailyStatsSummary);
        return combined;
    }

}
