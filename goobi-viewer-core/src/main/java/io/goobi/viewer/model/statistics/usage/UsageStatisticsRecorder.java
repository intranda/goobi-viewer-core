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

import java.time.LocalDate;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Class to be called on requests to be recorded in usage statistics. Stores and updates usage statistics in database on each request
 * 
 * @author florian
 */
public class UsageStatisticsRecorder {

    private static final String USER_AGENT_HEADER = "User-Agent";

    private static final Logger logger = LogManager.getLogger(UsageStatisticsRecorder.class);

    /**
     * {@link IDAO} to write recorded request counts to
     */
    private final IDAO dao;
    /**
     * {@link Configuration viewer configuration} to use for settings
     */
    private final Configuration config;
    /**
     * A name for the viewer for which the data is recorded. Usually this is set to the main theme name
     */
    private final String viewerName;
    /**
     * Locks all calls to {@link #recordRequest(RequestType, String, String, String, String)}
     */
    private final Object dailyStatisticsLock = new Object();

    /**
     * Default constructor
     * 
     * @param dao the {@link #dao} to set
     * @param config the {@link #config} to set
     * @param viewerName the {@link #viewerName} to set
     */
    public UsageStatisticsRecorder(IDAO dao, Configuration config, String viewerName) {
        this.dao = dao;
        this.config = config;
        this.viewerName = viewerName;
    }

    /**
     * Check if usage statistics are enabled by configuration
     * 
     * @return true if {@link Configuration#isStatisticsEnabled()} returns true
     */
    public boolean isActive() {
        return config.isStatisticsEnabled();
    }

    /**
     * Add a http request to the usage statistics
     * 
     * @param type the {@link RequestType} for which to count the request
     * @param recordIdentifier the record identifier requested by the request
     * @param request a {@link HttpServletRequest}
     */
    public void recordRequest(RequestType type, String recordIdentifier, HttpServletRequest request) {
        if (isActive() && !NetTools.isCrawlerBotRequest(request)) {
            recordRequest(type, recordIdentifier,
                    Optional.ofNullable(request).map(HttpServletRequest::getSession).map(HttpSession::getId).orElse(null),
                    Optional.ofNullable(request).map(req -> req.getHeader(USER_AGENT_HEADER)).orElse(""), NetTools.getIpAddress(request));
        }
    }

    /**
     * Add a request to the internal request counts
     * 
     * @param type the {@link RequestType} for which to count the request
     * @param recordIdentifier the record identifier requested by the request
     * @param sessionID The session issuing this request
     * @param userAgent the 'User-Agent' header value of the request
     * @param clientIP The IP Address from which the request is issued
     */
    protected void recordRequest(RequestType type, String recordIdentifier, String sessionID, String userAgent, String clientIP) {
        if (sessionID != null) {
            synchronized (dailyStatisticsLock) {
                try {
                    LocalDate date = LocalDate.now();
                    DailySessionUsageStatistics stats = getStatistics(date);
                    if (stats == null) {
                        stats = initStatistics(date);
                    }
                    SessionUsageStatistics session = stats.getSession(sessionID);
                    if (session == null) {
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
    }

    /**
     * Get the statistics for the given date from the database
     * 
     * @param date
     * @return {@link DailySessionUsageStatistics}
     * @throws DAOException If an error occured regarding the database
     */
    private DailySessionUsageStatistics getStatistics(LocalDate date) throws DAOException {
        return this.dao.getUsageStatistics(date);
    }

    /**
     * Update the given statistics object in the database
     * 
     * @param statistics
     * @return true if successful; false otherwise
     * @throws DAOException If an error occured regarding the database
     * @throws IllegalArgumentException If no statistics object exists in the database for the date of the given statistics
     */
    private boolean updateStatistics(DailySessionUsageStatistics statistics) throws DAOException, IllegalArgumentException {
        if (statistics.getId() != null) {
            return this.dao.updateUsageStatistics(statistics);
        }
        throw new IllegalArgumentException("given statistics object is not a dao entity (doesn't have a database id)");
    }

    /**
     * Create a new statistics object in the database for the given date
     * 
     * @param date
     * @return the created statistics object
     * @throws DAOException If an error occured regarding the database
     */
    private synchronized DailySessionUsageStatistics initStatistics(LocalDate date) throws DAOException {

        DailySessionUsageStatistics existing = this.dao.getUsageStatistics(date);
        if (existing != null) {
            //statistics already exists, return this
            return existing;
        } else {
            DailySessionUsageStatistics stats = new DailySessionUsageStatistics(date, viewerName);
            this.dao.addUsageStatistics(stats);
            return stats;
        }

    }

}
