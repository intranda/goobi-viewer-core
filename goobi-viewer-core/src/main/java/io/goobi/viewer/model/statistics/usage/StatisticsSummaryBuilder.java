package io.goobi.viewer.model.statistics.usage;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
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
    private final StatisticsSummaryFilter filter;
     
    public StatisticsSummaryBuilder(StatisticsSummaryFilter filter) throws DAOException {
          this(filter, DataManager.getInstance().getDao(), DataManager.getInstance().getSearchIndex());      
    }
    
    public StatisticsSummaryBuilder(StatisticsSummaryFilter filter, IDAO dao, SolrSearchIndex searchIndex) {
        this.dao = dao;
        this.searchIndex = searchIndex;
        this.filter = filter;  
    }
    
    public StatisticsSummary loadSummary() throws DAOException {
        StatisticsSummary fromDAO = loadFromDAO();
        return fromDAO;
    }
    
    private StatisticsSummary loadFromDAO() throws DAOException {
        List<DailySessionUsageStatistics> days = this.dao.getUsageStatistics(this.filter.getStartDate(), this.filter.getEndDate());
        return days.stream().reduce(StatisticsSummary.empty(), this::add, StatisticsSummary::add);
    }
    
    private StatisticsSummary add(StatisticsSummary summary, DailySessionUsageStatistics dailyStats) {
        StatisticsSummary dailyStatsSummary = new StatisticsSummary(dailyStats);
        StatisticsSummary combined = summary.add(dailyStatsSummary);
        return combined;
    }

}
