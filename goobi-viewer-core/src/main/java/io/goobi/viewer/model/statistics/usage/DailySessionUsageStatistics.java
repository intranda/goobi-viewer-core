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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.DataManager;

/**
 * @author florian
 *
 */
@Entity
@Table(name = "usage_statistics")
public class DailySessionUsageStatistics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usage_statistics_id")
    private long id;
    
    /**
     * The date the statistics were recorded
     */
    @Column(name = "date")
    private LocalDate date;

    /**
     * A name for the viewer instance the statistics were recorded for
     */
    @Column(name = "viewer_instance")
    private String viewerInstance;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinTable(name = "usage_statistics_daily_session", joinColumns = @JoinColumn(name = "usage_statistics_id"),
            inverseJoinColumns = @JoinColumn(name = "session_statistics_id"))
    private final List<SessionUsageStatistics> sessions = new ArrayList<>();
    
    public DailySessionUsageStatistics(LocalDate date, String viewer) {
        this.date = date;
        this.viewerInstance = viewer;
    }

    public DailySessionUsageStatistics() {
        this(LocalDate.now(), DataManager.getInstance().getConfiguration().getTheme());   
    }
    
    public DailySessionUsageStatistics(DailySessionUsageStatistics orig) {
        this(orig.date, orig.viewerInstance);
        this.sessions.addAll(orig.sessions.stream().map(s -> new SessionUsageStatistics(s)).collect(Collectors.toList()));
    }
    
    public SessionUsageStatistics getSession(String sessionId) {
        if(StringUtils.isNotBlank(sessionId)) {            
            return sessions.stream().filter(s -> sessionId.equals(s.getSessionId())).findAny().orElse(null);
        } else {
            return null;
        }
    }
    
    public void addSession(SessionUsageStatistics session) {
        this.sessions.add(session);
    }
    
    /**
     * @return the id
     */
    public long getId() {
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
     * @param string
     * @return
     */
    public long getTotalRequestCount(String pi) {
        return this.sessions.stream().mapToLong(s -> s.getRecordRequestCount(pi)).sum();
    }

    /**
     * @param string
     * @return
     */
    public long getUniqueRequestCount(String pi) {
        return this.sessions.stream().mapToLong(s -> s.getRecordRequestCount(pi) > 0 ? 1l : 0l).sum();
    }
}
