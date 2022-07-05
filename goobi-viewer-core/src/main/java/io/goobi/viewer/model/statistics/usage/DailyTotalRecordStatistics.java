/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.statistics.usage;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import io.goobi.viewer.controller.DataManager;

/**
 * @author florian
 * Statistics of record requests for a single day without session information. 
 * Only statistics are total and unique requests per PI
 */
public class DailyTotalRecordStatistics {

    /**
     * The date the statistics were recorded
     */
    private final LocalDate date;
    
    /**
     * A name for the viewer instance the statistics were recorded for
     */
    private final String viewerInstance;
    
    /**
     * A map of persistent identifiers (PIs) mapped to an array of two numbers:
     * 1. total request: the total number of request for the PI
     * 2. unique requests: the number of user sessions that have requested this PI 
     */
    private final Map<String, long[]> requestCounts = new HashMap<>();
    
    public DailyTotalRecordStatistics(LocalDate date, String viewer) {
        this.date = date;
        this.viewerInstance = viewer;
    }
    
    public DailyTotalRecordStatistics() {
        this(LocalDate.now(), DataManager.getInstance().getConfiguration().getTheme());   
    }
    
    public DailyTotalRecordStatistics(DailyTotalRecordStatistics orig) {
        this(orig.date, orig.viewerInstance);
        this.requestCounts.putAll(orig.requestCounts);
    }
    
    public void setTotalRequests(long requests, String pi) {
        
        long[] counts = this.requestCounts.get(pi);
        if(counts == null) {
            counts = 
        }
    }
    
}
