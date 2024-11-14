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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.goobi.viewer.api.rest.model.statistics.usage.UsageStatisticsInformation;
import io.goobi.viewer.api.rest.v1.statistics.usage.UsageStatisticsResource;
import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * Summary of request counts for a certain date range. Used for delivering record counts to users
 * 
 * @author florian
 *
 */
@JsonInclude(Include.NON_NULL)
public class StatisticsSummary {

    /**
     * Request counts sorted by {@link RequestType}
     */
    private final Map<RequestType, RequestTypeSummary> types;
    private final LocalDateTime creationTime = LocalDateTime.now();
    private UsageStatisticsInformation info = null;

    public StatisticsSummary() {
        this(Collections.emptyMap());
    }

    /**
     * Default constructor
     * 
     * @param types Request counts sorted by {@link RequestType}
     */
    public StatisticsSummary(Map<RequestType, RequestTypeSummary> types) {
        this.types = types;
    }

    /**
     * Constructor to create an instance from a {@link DailySessionUsageStatistics} object
     * 
     * @param dailyStats The {@link DailySessionUsageStatistics} from which to retrieve the request counts
     */
    public StatisticsSummary(DailySessionUsageStatistics dailyStats) {
        this(dailyStats, Collections.emptyList());
    }

    /**
     * Constructor to create an instance from a {@link DailySessionUsageStatistics} object filtered by a list of record identifiers
     * 
     * @param dailyStats The {@link DailySessionUsageStatistics} from which to retrieve the request counts
     * @param includedIdentifiers A list of record identifiers for which to count the requests. If empty, all requests will be counted
     */
    public StatisticsSummary(DailySessionUsageStatistics dailyStats, List<String> includedIdentifiers) {
        Map<RequestType, RequestTypeSummary> tempTypes = new EnumMap<>(RequestType.class);
        for (RequestType type : RequestType.getUsedValues()) {
            long total = dailyStats.getTotalRequestCount(type, includedIdentifiers);
            long unique = dailyStats.getUniqueRequestCount(type, includedIdentifiers);
            tempTypes.put(type, new RequestTypeSummary(total, unique, dailyStats.getDate(), dailyStats.getDate()));
        }
        this.types = tempTypes;
    }

    /**
     * Create an empty summary
     * 
     * @return an empty {@link StatisticsSummary}
     */
    public static StatisticsSummary empty() {
        Map<RequestType, RequestTypeSummary> types = new EnumMap<>(RequestType.class);
        for (RequestType type : RequestType.getUsedValues()) {
            types.put(type, new RequestTypeSummary(0, 0));
        }
        return new StatisticsSummary(types);
    }

    /**
     * Get the request counts sorted by {@link RequestType}
     * 
     * @return a {@link Map}
     */
    public Map<RequestType, RequestTypeSummary> getTypes() {
        return types;
    }

    /**
     * Create a new summary with the sum of request counts from this and another summary
     * 
     * @param other the other {@link SummaryStatistics} to add to this one
     * @return the sum of {@link SummaryStatistics}
     */
    public StatisticsSummary add(StatisticsSummary other) {
        Map<RequestType, RequestTypeSummary> combinedTypes = new EnumMap<>(RequestType.class);
        if (other.getTotalRequests() == 0) {
            return new StatisticsSummary(this.getTypes());
        }
        for (RequestType type : RequestType.getUsedValues()) {
            RequestTypeSummary mine = this.types.get(type);
            RequestTypeSummary others = other.types.get(type);
            long total = mine.getTotalRequests() + others.getTotalRequests();
            long unique = mine.getUniqueRequests() + others.getUniqueRequests();
            LocalDate startDate = mine.getStartDate().isBefore(others.getStartDate()) ? mine.getStartDate() : others.getStartDate();
            LocalDate endDate = mine.getEndDate().isAfter(others.getEndDate()) ? mine.getEndDate() : others.getEndDate();
            combinedTypes.put(type, new RequestTypeSummary(total, unique, startDate, endDate));
        }
        return new StatisticsSummary(combinedTypes);
    }

    /**
     * Get the total amount for requests for a given {@link RequestType}
     * 
     * @param types the {@link RequestType}s to count
     * @return number of requests
     */
    public long getTotalRequests(RequestType... types) {
        return this.types.entrySet()
                .stream()
                .filter(entry -> types == null || types.length == 0 || Arrays.asList(types).contains(entry.getKey()))
                .mapToLong(entry -> entry.getValue().getTotalRequests())
                .sum();
    }

    /**
     * Get the number of unique request for a given {@link RequestType}
     * 
     * @param types the {@link RequestType}s to count
     * @return number of unique requests
     */
    public long getUniqueRequests(RequestType... types) {
        return this.types.entrySet()
                .stream()
                .filter(entry -> types == null || types.length == 0 || Arrays.asList(types).contains(entry.getKey()))
                .mapToLong(entry -> entry.getValue().getUniqueRequests())
                .sum();
    }

    /**
     * Get the last date for which requests have been recorded
     * 
     * @param types the {@link RequestType} to check
     * @return {@link LocalDate}
     */
    public LocalDate getLastRecordedDate(RequestType... types) {
        return this.types.entrySet()
                .stream()
                .filter(entry -> types == null || types.length == 0 || Arrays.asList(types).contains(entry.getKey()))
                .map(entry -> entry.getValue().getEndDate())
                .reduce(LocalDate.ofEpochDay(0), (d1, d2) -> d1.isAfter(d2) ? d1 : d2);
    }

    @JsonIgnore
    public String getAsCsv(Locale locale, String separator) {
        StringBuilder sb = new StringBuilder();
        for (Entry<RequestType, RequestTypeSummary> entry : this.getTypes().entrySet()) {
            RequestType type = entry.getKey();
            RequestTypeSummary summary = entry.getValue();
            DateTimeFormatter format = DateTimeFormatter.ofPattern(UsageStatisticsResource.DATE_FORMAT);
            String timeString = getAsString(entry.getValue().getStartDate(), entry.getValue().getEndDate(), format);
            sb.append(timeString)
                    .append(separator)
                    .append(ViewerResourceBundle.getTranslation(type.getLabel(), locale))
                    .append(separator)
                    .append(ViewerResourceBundle.getTranslation("statistics__total_requests", locale))
                    .append(separator)
                    .append(summary.getTotalRequests())
                    .append("\n");
            sb.append(timeString)
                    .append(separator)
                    .append(ViewerResourceBundle.getTranslation(type.getLabel(), locale))
                    .append(separator)
                    .append(ViewerResourceBundle.getTranslation("statistics__unique_requests", locale))
                    .append(separator)
                    .append(summary.getUniqueRequests())
                    .append("\n");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * 
     * @param start
     * @param end
     * @param format
     * @return Formatted date range
     */
    private static String getAsString(LocalDate start, LocalDate end, DateTimeFormatter format) {
        if (end.isAfter(start)) {
            return format.format(start) + " - " + format.format(end);
        }
        return format.format(start);
    }

    public String calculateStartDate() {
        return this.types.values()
                .stream()
                .reduce((s1, s2) -> s1.getStartDate().isBefore(s2.getStartDate()) ? s1 : s2)
                .map(RequestTypeSummary::getStartDate)
                .map(d -> DateTimeFormatter.ofPattern(UsageStatisticsResource.DATE_FORMAT).format(d))
                .orElse(null);
    }

    public String calculateEndDate() {
        return this.types.values()
                .stream()
                .reduce((s1, s2) -> s1.getEndDate().isAfter(s2.getEndDate()) ? s1 : s2)
                .map(RequestTypeSummary::getEndDate)
                .map(d -> DateTimeFormatter.ofPattern(UsageStatisticsResource.DATE_FORMAT).format(d))
                .orElse(null);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return this.types.values().stream().allMatch(RequestTypeSummary::isEmtpy);
    }

    public void setInformation(UsageStatisticsInformation info) {
        this.info = info;
    }

    public UsageStatisticsInformation getInformation() {
        return info;
    }

    public boolean isOlderThan(long num, TemporalUnit unit) {
        return isOlderThan(num, unit, LocalDateTime.now());
    }

    public boolean isOlderThan(long num, TemporalUnit unit, Temporal currentTime) {
        Duration maxAge = Duration.of(num, unit);
        Duration sinceCreation = Duration.between(getCreationTime(), currentTime);
        return sinceCreation.compareTo(maxAge) > 0;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }
}
