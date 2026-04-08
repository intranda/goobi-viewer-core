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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.DataManager;

/**
 * Persistence class holding the usage statistics for a single day in the form of a list of {@link SessionUsageStatistics}.
 * 
 * @author florian
 */
@Entity
@Table(name = "usage_statistics")
public class DailySessionUsageStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usage_statistics_id")
    private Long id;

    /**
     * The date the statistics were recorded.
     */
    @Column(name = "date")
    private LocalDate date;

    /**
     * A name for the viewer instance the statistics were recorded for.
     */
    @Column(name = "viewer_instance")
    private String viewerInstance;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(name = "usage_statistics_daily_session", joinColumns = @JoinColumn(name = "usage_statistics_id"),
            inverseJoinColumns = @JoinColumn(name = "session_statistics_id"))
    private List<SessionUsageStatistics> sessions = new ArrayList<>();

    /**
     *
     * @param date the date these statistics were recorded
     * @param viewer the name of the viewer instance
     */
    public DailySessionUsageStatistics(LocalDate date, String viewer) {
        this.date = date;
        this.viewerInstance = viewer;
    }

    public DailySessionUsageStatistics() {
        this(LocalDate.now(), DataManager.getInstance().getConfiguration().getTheme());
    }

    /**
     *
     * @param orig the instance to copy
     */
    public DailySessionUsageStatistics(DailySessionUsageStatistics orig) {
        this(orig.date, orig.viewerInstance);
        this.sessions.addAll(orig.sessions.stream().map(SessionUsageStatistics::new).collect(Collectors.toList()));
    }

    /**
     *
     * @param sessionId the HTTP session ID to look up
     * @return {@link SessionUsageStatistics}
     */
    public SessionUsageStatistics getSession(String sessionId) {
        if (StringUtils.isNotBlank(sessionId)) {
            return sessions.stream().filter(s -> sessionId.equals(s.getSessionId())).findAny().orElse(null);
        }
        return null;
    }

    /**
     *
     * @param session the session statistics to add
     */
    public void addSession(SessionUsageStatistics session) {
        this.sessions.add(session);
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @return the date
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * @return the viewerInstance
     */
    public String getViewerInstance() {
        return viewerInstance;
    }

    /**
     * @param type request type to count
     * @param pi persistent identifier of the record to filter by
     * @return a long
     */
    public long getTotalRequestCount(RequestType type, String pi) {
        return this.sessions.stream().mapToLong(s -> s.getRecordRequestCount(type, pi)).sum();
    }

    /**
     *
     * @param type request type to count
     * @return a long
     */
    public long getTotalRequestCount(RequestType type) {
        return getTotalRequestCount(type, Collections.emptyList());
    }

    /**
     *
     * @param type request type to count
     * @param identifiersToInclude only count requests for these record identifiers
     * @return a long
     */
    public long getTotalRequestCount(RequestType type, List<String> identifiersToInclude) {
        return this.sessions.stream().mapToLong(s -> s.getTotalRequestCount(type, identifiersToInclude)).sum();
    }

    /**
     *
     * @param type request type to count
     * @return a long
     */
    public long getUniqueRequestCount(RequestType type) {
        return this.sessions.stream().mapToLong(s -> s.getRequestedRecordsCount(type)).sum();
    }

    /**
     *
     * @param type request type to count
     * @param pi persistent identifier of the record to filter by
     * @return a long
     */
    public long getUniqueRequestCount(RequestType type, String pi) {
        return this.sessions.stream().mapToLong(s -> s.getRecordRequestCount(type, pi) > 0 ? 1L : 0L).sum();
    }

    /**
     * @param type request type to count
     * @param includedIdentifiers only count sessions that requested any of these identifiers
     * @return a long
     */
    public long getUniqueRequestCount(RequestType type, List<String> includedIdentifiers) {
        return this.sessions.stream().mapToLong(s -> s.getTotalRequestCount(type, includedIdentifiers) > 0 ? 1L : 0L).sum();
    }

    /**
     * 
     * @return List<String>
     */
    public List<String> getRecordIdentifier() {
        return this.sessions.stream().flatMap(s -> s.getRecordIdentifier().stream()).distinct().collect(Collectors.toList());
    }

    @Override
    public String toString() {
        String s = "Usage statistics for " + date + " in " + viewerInstance + ". Countains " + sessions.size() + " session instances\n";
        s += this.sessions.stream().map(SessionUsageStatistics::toString).collect(Collectors.joining("\n"));
        return s;

    }

}
