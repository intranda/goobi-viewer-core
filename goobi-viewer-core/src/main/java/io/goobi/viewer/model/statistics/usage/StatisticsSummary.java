package io.goobi.viewer.model.statistics.usage;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class StatisticsSummary {

    private final Map<RequestType, RequestTypeSummary> types;
    
    public StatisticsSummary(Map<RequestType, RequestTypeSummary> types) {
        this.types = types;
    }
    
    public StatisticsSummary(DailySessionUsageStatistics dailyStats) {
        this(dailyStats, Collections.emptyList());
    }
    
    public StatisticsSummary(DailySessionUsageStatistics dailyStats, List<String> includedIdentifiers) {
        Map<RequestType, RequestTypeSummary> types = new HashMap<>();
        for (RequestType type : RequestType.values()) {
            long total = dailyStats.getTotalRequestCount(type, includedIdentifiers);
            long unique = dailyStats.getUniqueRequestCount(type, includedIdentifiers);
            types.put(type, new RequestTypeSummary(total, unique, dailyStats.getDate(), dailyStats.getDate()));
        }
        this.types = types;
    }

    public static StatisticsSummary empty() {
        Map<RequestType, RequestTypeSummary> types = new HashMap<>();
        for (RequestType type : RequestType.values()) {
            types.put(type, new RequestTypeSummary(0,0));
        }
        return new StatisticsSummary(types);
    }
    
    public Map<RequestType, RequestTypeSummary> getTypes() {
        return types;
    }

    public StatisticsSummary add(StatisticsSummary other) {
        Map<RequestType, RequestTypeSummary> types = new HashMap<>();
        if(other.getTotalRequests() == 0) {
            return new StatisticsSummary(this.getTypes());
        }
        for (RequestType type : RequestType.values()) {
            RequestTypeSummary mine = this.types.get(type);
            RequestTypeSummary others = other.types.get(type);
            long total = mine.getTotalRequests() + others.getTotalRequests();
            long unique = mine.getUniqueRequests() + others.getUniqueRequests();
            LocalDate startDate = mine.getStartDate().isBefore(others.getStartDate()) ? mine.getStartDate() : others.getStartDate();
            LocalDate endDate = mine.getEndDate().isAfter(others.getEndDate()) ? mine.getEndDate() : others.getEndDate();
            types.put(type, new RequestTypeSummary(total, unique, startDate, endDate));
        }
        StatisticsSummary combined = new StatisticsSummary(types);
        return combined;
    }

    
    public long getTotalRequests(RequestType... types) {
        return this.types.entrySet().stream()
        .filter(entry -> types == null || types.length == 0 || Arrays.asList(types).contains(entry.getKey()))
        .mapToLong(entry -> entry.getValue().getTotalRequests()).sum();
    }
    
    public long getUniqueRequests(RequestType... types) {
        return this.types.entrySet().stream()
        .filter(entry -> types == null || types.length == 0 || Arrays.asList(types).contains(entry.getKey()))
        .mapToLong(entry -> entry.getValue().getUniqueRequests()).sum();
    }
    
    public LocalDate getLastRecordedDate(RequestType... types) {
        return this.types.entrySet().stream()
                .filter(entry -> types == null || types.length == 0 || Arrays.asList(types).contains(entry.getKey()))
                .map(entry -> entry.getValue().getEndDate())
                .reduce(LocalDate.ofEpochDay(0), (d1,d2) -> d1.isAfter(d2) ? d1 : d2);
    }
}


