package io.goobi.viewer.model.statistics.usage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;


import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;

/**
 * Writes Indexing files for SOLR indexer
 * 
 * @author florian
 *
 */
public class StatisticsIndexer {
    
    private final Path hotfolder;
    
    public StatisticsIndexer(Path hotfolder) {
        this.hotfolder = hotfolder;
    }
    
    public StatisticsIndexer() throws DAOException {
        this(Paths.get(DataManager.getInstance().getConfiguration().getHotfolder()));
    }
    
    public Path indexStatistics(DailySessionUsageStatistics stats) throws DAOException, IOException {
        String json = createStatisticsJsonString(stats);
        return writeToHotfolder(stats.getDate(), json);
  
    }

    private Path writeToHotfolder(LocalDate date, String json) throws IOException {
        String filename = "statistics-usage-" + getAsFormattedString(date) + ".json";
        Files.writeString(hotfolder.resolve(filename), json);
        return hotfolder.resolve(filename);
    }

    private String createStatisticsJsonString(DailySessionUsageStatistics stats) {
        JSONObject statsObject = new JSONObject();
        JSONArray records = new JSONArray();
        statsObject.put("date", getAsFormattedString(stats.getDate()));
        statsObject.put("viewer-name", stats.getViewerInstance());
        statsObject.put("records", records);
        for (String pi : stats.getRecordIdentifier()) {
            JSONObject recordObject = new JSONObject();
            records.put(recordObject);
            recordObject.put("pi", pi);
            JSONArray counts = new JSONArray(6);
            for (RequestType type : RequestType.values()) {
                counts.put(type.getTotalCountIndex(), stats.getTotalRequestCount(type, pi));
                counts.put(type.getUniqueCountIndex(), stats.getUniqueRequestCount(type, pi));
            }
            recordObject.put("counts", counts);
        }
        return statsObject.toString();
    }

    private String getAsFormattedString(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

}
