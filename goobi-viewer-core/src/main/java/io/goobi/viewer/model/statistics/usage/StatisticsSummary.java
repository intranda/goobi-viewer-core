package io.goobi.viewer.model.statistics.usage;

import java.time.LocalDate;

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
public class StatisticsSummary {

    private final IDAO dao;
    private final SolrSearchIndex searchIndex;
     
    public StatisticsSummary(LocalDate startDate, LocalDate endDate) throws DAOException {
          this(startDate, endDate, DataManager.getInstance().getDao(), DataManager.getInstance().getSearchIndex());      
    }
    
    public StatisticsSummary(LocalDate startDate, LocalDate endDate, IDAO dao, SolrSearchIndex searchIndex) {
        this.dao = dao;
        this.searchIndex = searchIndex;
        
        
    }
}
