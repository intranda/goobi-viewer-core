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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;
import java.util.stream.StreamSupport;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.solr.SolrConstants;

public class StatisticsIndexerTest {

    @Test
    public void test() throws DAOException, IOException {
        Path hotfolder = Paths.get("src/test/resources/hotfolder");
        Path hotfolderFile = hotfolder.resolve("statistics-usage-2022-07-04.json");
        if(Files.exists(hotfolderFile)) {
            Files.delete(hotfolderFile);
        }
        DailySessionUsageStatistics stats = createStatistics();
        
        StatisticsIndexer indexer = new StatisticsIndexer(hotfolder);
        indexer.indexStatistics(stats);
        
        assertTrue(Files.exists(hotfolderFile));
        String jsonString = Files.readString(hotfolderFile);
        JSONObject json = new JSONObject(jsonString);
        assertEquals("viewer-test", json.getString("viewer-name"));
        assertEquals("2022-07-04", json.getString("date"));
        
        JSONArray records = json.getJSONArray("records");
        assertEquals(3, records.length());

        JSONObject pi1Records = StreamSupport.stream(records.spliterator(), false)
                .map(o -> (JSONObject)o)
                .filter(j -> j.getString("pi").equals("PI_01"))
                .findAny().orElse(null);
        assertNotNull(pi1Records);
        assertEquals(9, pi1Records.getJSONArray("counts").get(0));
        assertEquals(2, pi1Records.getJSONArray("counts").get(1));
        
        JSONObject pi2Records = StreamSupport.stream(records.spliterator(), false)
                .map(o -> (JSONObject)o)
                .filter(j -> j.getString("pi").equals("PI_02"))
                .findAny().orElse(null);
        assertNotNull(pi2Records);
        assertEquals(3, pi2Records.getJSONArray("counts").get(0));
        assertEquals(1, pi2Records.getJSONArray("counts").get(1));
        
        JSONObject pi3Records = StreamSupport.stream(records.spliterator(), false)
                .map(o -> (JSONObject)o)
                .filter(j -> j.getString("pi").equals("PI_03"))
                .findAny().orElse(null);
        assertNotNull(pi3Records);
        assertEquals(4, pi3Records.getJSONArray("counts").get(0));
        assertEquals(1, pi3Records.getJSONArray("counts").get(1));
        
    }
    
    @Test
    public void testBuildQuery() {
        LocalDate date = LocalDate.of(2022, 8, 30);
        String query1 = String.format("+%s:%s +%s:\"%s\"", 
                SolrConstants.DOCTYPE, 
                StatisticsLuceneFields.USAGE_STATISTICS_DOCTYPE,
                StatisticsLuceneFields.DATE,
                StatisticsLuceneFields.solrDateFormatter.format(date.atStartOfDay()));
        String query2 = "+{docTypeField}:{doctype} +{dateField}:\"{date}\""
                .replace("{docTypeField}", SolrConstants.DOCTYPE)
                .replace("{doctype}", StatisticsLuceneFields.USAGE_STATISTICS_DOCTYPE)
                .replace("{dateField}", StatisticsLuceneFields.DATE)
                .replace("{date}", StatisticsLuceneFields.solrDateFormatter.format(date.atStartOfDay()));
        
        assertEquals(query1, query2);
        assertEquals("+DOCTYPE:STATISTICS_USAGE +STATISTICS_DATE:\"2022-08-30T00:00:00Z\"", query1);
    }
    
    private DailySessionUsageStatistics createStatistics() {
            LocalDate date = LocalDate.of(2022, Month.JULY, 4);
            DailySessionUsageStatistics stats = new DailySessionUsageStatistics(date, "viewer-test");
            
            SessionUsageStatistics session1 = new SessionUsageStatistics("ABCD", "Ubuntu Firefox", "168.178.192.2");
            session1.setRecordRequectCount(RequestType.RECORD_VIEW, "PI_01", 7);
            session1.setRecordRequectCount(RequestType.RECORD_VIEW, "PI_02", 3);
            stats.addSession(session1);
            
            SessionUsageStatistics session2 = new SessionUsageStatistics("EFGH", "Ubuntu Chrome", "168.178.192.3");
            session2.setRecordRequectCount(RequestType.RECORD_VIEW, "PI_01", 2);
            session2.setRecordRequectCount(RequestType.RECORD_VIEW, "PI_03", 4);
            stats.addSession(session2);
    
            return stats;
    }
    

}
