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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;

/**
 * @author florian Class to be called on requests to be recorded in usage statistics. Stores and updates usage statistics in database on each request
 */
public class UsageStatisticsRecorder {


    private static final String USER_AGENT_HEADER = "User-Agent";

    private static final Logger logger = LoggerFactory.getLogger(UsageStatisticsRecorder.class);

    private final IDAO dao;
    private final Configuration config;
    private final String viewerName;
    private final Object dailyStatisticsLock = new Object();

    public UsageStatisticsRecorder(IDAO dao, Configuration config, String viewerName) {
        this.dao = dao;
        this.config = config;
        this.viewerName = viewerName;
    }

    public boolean isActive() {
        return config.isStatisticsEnabled();
    }
    
    public void recordRequest(RequestType type, String recordIdentifier, HttpServletRequest request) {
        if(isActive() && !NetTools.isCrawlerBotRequest(request)) {            
            recordRequest(type, recordIdentifier, request.getSession().getId(), request.getHeader(USER_AGENT_HEADER), NetTools.getIpAddress(request));
        }
    }

    protected void recordRequest(RequestType type, String recordIdentifier, String sessionID, String userAgent, String clientIP) {
        synchronized (dailyStatisticsLock) {
            try {
                LocalDate date = LocalDate.now();
                DailySessionUsageStatistics stats = getStatistics(date);
                if (stats == null) {
                    stats = initStatistics(date);
                }
                SessionUsageStatistics session = stats.getSession(sessionID);
                if(session == null) {
                    session = new SessionUsageStatistics(sessionID, userAgent, clientIP);
                    stats.addSession(session);
                }
                session.incrementRequestCount(type, recordIdentifier);
                updateStatistics(stats);
            } catch (DAOException e) {
                logger.error("Unable to record update usage statistics: {}", e.toString());
            }
        }
    }

    /**
     * Get the statistics for the given date from the database
     * 
     * @param date
     * @return
     * @throws DAOException If an error occured regarding the database
     */
    private DailySessionUsageStatistics getStatistics(LocalDate date) throws DAOException {
        return this.dao.getUsageStatistics(date);
    }

    /**
     * Update the given statistics object in the database
     * 
     * @param statistics
     * @return
     * @throws DAOException If an error occured regarding the database
     * @throws IllegalArgumentException If no statistics object exists in the database for the date of the given statistics
     */
    private boolean updateStatistics(DailySessionUsageStatistics statistics) throws DAOException, IllegalArgumentException {
        if (statistics.getId() != null) {
            return this.dao.updateUsageStatistics(statistics);
        } else {
            throw new IllegalArgumentException("given statistics object is not a dao entity (doesn't have a database id)");
        }
    }

    /**
     * Create a new statistics object in the database for the given date
     * 
     * @param date
     * @return the created statistics object
     * @throws DAOException If an error occured regarding the database
     */
    private DailySessionUsageStatistics initStatistics(LocalDate date) throws DAOException {
        DailySessionUsageStatistics stats = new DailySessionUsageStatistics(date, viewerName);
        this.dao.addUsageStatistics(stats);
        return stats;
    }

}
