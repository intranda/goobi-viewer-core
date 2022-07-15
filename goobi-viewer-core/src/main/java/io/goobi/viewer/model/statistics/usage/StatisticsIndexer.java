package io.goobi.viewer.model.statistics.usage;

import java.time.LocalDate;

import org.json.JSONArray;
import org.json.JSONObject;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;

/**
 * Writes Indexing files for SOLR indexer
 * 
 * @author florian
 *
 */
public class StatisticsIndexer {
    
    private final IDAO dao;
    
    public StatisticsIndexer(IDAO dao) {
        this.dao = dao;
    }
    
    public StatisticsIndexer() throws DAOException {
        this(DataManager.getInstance().getDao());
    }
    
    public void indexStatistics(LocalDate date) throws DAOException {
        DailySessionUsageStatistics stats = dao.getUsageStatistics(date);
        JSONObject statsObject = new JSONObject();
        JSONArray records = new JSONArray();
        statsObject.put("records", records);
        for (String pi : stats.getRecordIdentifier()) {
            JSONArray counts = new JSONArray(6);
            for (RequestType type : RequestType.values()) {
                counts.put(type.getTotalCountIndex(), stats.getTotalRequestCount(type, pi));
                counts.put(type.getUniqueCountIndex(), stats.getUniqueRequestCount(type, pi));
            }
        }
        
        
    }

}
