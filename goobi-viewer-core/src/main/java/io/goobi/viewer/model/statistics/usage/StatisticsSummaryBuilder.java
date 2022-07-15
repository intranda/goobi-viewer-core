package io.goobi.viewer.model.statistics.usage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

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

    private final IDAO dao;
    private final SolrSearchIndex searchIndex;
     
    public StatisticsSummaryBuilder() throws DAOException {
          this(DataManager.getInstance().getDao(), DataManager.getInstance().getSearchIndex());      
    }
    
    public StatisticsSummaryBuilder(IDAO dao, SolrSearchIndex searchIndex) {
        this.dao = dao;
        this.searchIndex = searchIndex;
    }
    
    public StatisticsSummary loadSummary(StatisticsSummaryFilter filter) throws DAOException, IndexUnreachableException {
        StatisticsSummary fromDAO = loadFromDAO(filter);
        return fromDAO;
    }
    
    private StatisticsSummary loadFromDAO(StatisticsSummaryFilter filter) throws DAOException, IndexUnreachableException {
        List<String> identifiersToInclude = getFilteredIdentifierList(filter);
        List<DailySessionUsageStatistics> days = this.dao.getUsageStatistics(filter.getStartDate(), filter.getEndDate());
        return days.stream().reduce(StatisticsSummary.empty(), (s,d) -> this.add(s, d, identifiersToInclude) , StatisticsSummary::add);
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
