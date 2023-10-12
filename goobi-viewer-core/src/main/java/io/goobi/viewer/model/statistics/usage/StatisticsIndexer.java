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

import io.goobi.viewer.controller.Configuration;
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

    /**
     * Absolute path to write index files to
     */
    private final Path hotfolder;

    /**
     * default constructor
     * 
     * @param hotfolder the {@link #hotfolder}
     */
    public StatisticsIndexer(Path hotfolder) {
        this.hotfolder = hotfolder;
    }

    /**
     * constructor using the {@link #hotfolder} given by {@link Configuration#getHotfolder()}
     * 
     * @throws DAOException
     */
    public StatisticsIndexer() throws DAOException {
        this(Paths.get(DataManager.getInstance().getConfiguration().getHotfolder()));
    }

    /**
     * Write index file to {@link #hotfolder} path for the given {@link DailySessionUsageStatistics}
     * 
     * @param stats the statistics to index
     * @return The filepath to the index files
     * @throws DAOException
     * @throws IOException
     */
    public Path indexStatistics(DailySessionUsageStatistics stats) throws IOException {
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
            for (RequestType type : RequestType.getUsedValues()) {
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
