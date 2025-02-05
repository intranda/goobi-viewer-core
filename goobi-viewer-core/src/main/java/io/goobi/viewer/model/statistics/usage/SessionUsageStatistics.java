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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

/**
 * Persistence class containing request counts for a single http session
 * 
 * @author florian
 *
 */
@Entity
@Table(name = "session_statistics")
public class SessionUsageStatistics {

    /**
     * Persistence context unique identifier
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_statistics_id")
    private Long id;

    /**
     * Http session identifier of the session tracked
     */
    @Column(name = "session_id")
    private String sessionId;

    /**
     * User-Agent header content of the first request counted from the http session
     */
    @Column(name = "user_agent")
    private String userAgent;

    /**
     * Client IP of the first request counted from the http session
     */
    @Column(name = "client_ip")
    private String clientIP;

    /**
     * Holds a collection of stingyfied {@link SessionRequestCounts} mapped to requested record identifiers
     */
    @ElementCollection
    @CollectionTable(name = "session_statistics_record_requests",
            joinColumns = { @JoinColumn(name = "session_statistics_id", referencedColumnName = "session_statistics_id") })
    @MapKeyColumn(name = "record_identifier")
    @Column(name = "count")
    private Map<String, String> recordRequests = new HashMap<>();

    /**
     * Empty constructor for persistence context initialization
     */
    public SessionUsageStatistics() {

    }

    /**
     * Initialize instance for a session
     * 
     * @param sessionId http session identifier
     * @param userAgent User-Agent header content
     * @param clientIP IP-Address the request came from
     */
    public SessionUsageStatistics(String sessionId, String userAgent, String clientIP) {
        this.sessionId = sessionId;
        this.userAgent = userAgent;
        this.clientIP = clientIP;
    }

    /**
     * Cloning constructor
     * 
     * @param orig
     */
    public SessionUsageStatistics(SessionUsageStatistics orig) {
        this.sessionId = orig.sessionId;
        this.userAgent = orig.userAgent;
        this.clientIP = orig.clientIP;
        this.recordRequests.putAll(orig.recordRequests);
    }

    /**
     * @return the {@link #id}
     */
    public Long getId() {
        return id;
    }

    /**
     * 
     * @return the {@link #sessionId}
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 
     * @return the {@link #userAgent}
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * @return the {@link #clientIP}
     */
    public String getClientIP() {
        return clientIP;
    }

    /**
     * Get number of requests for a given {@link RequestType} and record identifier
     * 
     * @param type the type of the request
     * @param recordIdentifier the record identifier
     * @return the number of requests for the type and identifier
     */
    public long getRecordRequestCount(RequestType type, String recordIdentifier) {
        return Optional.ofNullable(getRecordRequests(recordIdentifier))
                .map(count -> count.getCount(type))
                .orElse(0L);
    }

    /**
     * Get number of requests for a given {@link RequestType}
     * 
     * @param type the type of the request
     * @return the number of requests for the type
     */
    public long getTotalRequestCount(RequestType type) {
        return getTotalRequestCount(type, Collections.emptyList());
    }

    /**
     * Return the number of requests of a given {@link RequestType} for the record identifiers included in identifiersToInclude
     * 
     * @param type the type of the request
     * @param identifiersToInclude record identifiers for which the number of requests should be counted
     * @return a long
     */
    public long getTotalRequestCount(RequestType type, List<String> identifiersToInclude) {
        Collection<String> requestValues = getRequestedCounts(identifiersToInclude);
        return requestValues.stream()
                .map(SessionRequestCounts::new)
                .mapToLong(count -> count.getCount(type))
                .sum();
    }

    /**
     * Get number of record identifiers which were requested with a given {@link RequestType}
     * 
     * @param type the type of the request
     * @return the number of record identifiers requested at least once for the type
     */
    public long getRequestedRecordsCount(RequestType type) {
        return this.recordRequests.values()
                .stream()
                .map(SessionRequestCounts::new)
                .mapToLong(count -> count.getCount(type))
                .map(count -> count > 0 ? 1 : 0)
                .sum();
    }

    /**
     * Set total count of requests for a {@link RequestType} and record identifier to the given number
     * 
     * @param count request count to set
     * @param type the type of the request
     * @param recordIdentifier the identifier of the requested record
     */
    public void setRecordRequectCount(RequestType type, String recordIdentifier, long count) {
        synchronized (this.recordRequests) {
            SessionRequestCounts counts = getRecordRequests(recordIdentifier);
            counts.setCount(type, count);
            setRecordRequests(recordIdentifier, counts);
        }
    }

    /**
     * Increment the total count of requests for a {@link RequestType} and record identifier by one
     * 
     * @param type the type of the request
     * @param recordIdentifier the identifier of the requested record
     */
    public void incrementRequestCount(RequestType type, String recordIdentifier) {
        synchronized (this.recordRequests) {
            long count = getRecordRequestCount(type, recordIdentifier);
            setRecordRequectCount(type, recordIdentifier, count + 1);
        }
    }

    /**
     * Get a list of all record identifiers contained in {@link #recordRequests}
     * 
     * @return list of record identifiers
     */
    public List<String> getRecordIdentifier() {
        return new ArrayList<>(this.recordRequests.keySet());
    }

    @Override
    public String toString() {
        String s = "Usage statistics for session " + this.sessionId + ":\n";
        s += this.recordRequests.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining("\n"));
        return s;
    }

    /**
     * 
     * @param identifiersToInclude
     * @return Collection<String> (immutable!)
     */
    private Collection<String> getRequestedCounts(List<String> identifiersToInclude) {
        Collection<String> requestValues;
        if (identifiersToInclude.isEmpty()) {
            requestValues = this.recordRequests.values();
        } else {
            requestValues = this.recordRequests.keySet()
                    .stream()
                    .filter(identifiersToInclude::contains)
                    .map(key -> this.recordRequests.get(key))
                    .toList();
        }
        return requestValues;
    }

    private SessionRequestCounts getRecordRequests(String identifier) {
        String s = this.recordRequests.get(identifier);
        return new SessionRequestCounts(s);
    }

    private void setRecordRequests(String identifier, SessionRequestCounts counts) {
        String s = counts.toJsonArray();
        this.recordRequests.put(identifier, s);
    }
}
