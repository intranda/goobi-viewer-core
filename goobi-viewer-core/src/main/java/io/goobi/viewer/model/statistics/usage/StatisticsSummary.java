package io.goobi.viewer.model.statistics.usage;

import java.util.HashMap;
import java.util.Map;

public class StatisticsSummary {

    private final Map<RequestType, RequestTypeSummary> types;
    
    public StatisticsSummary(Map<RequestType, RequestTypeSummary> types) {
        this.types = types;
    }
    
    public StatisticsSummary(DailySessionUsageStatistics dailyStats) {
        Map<RequestType, RequestTypeSummary> types = new HashMap<>();
        for (RequestType type : RequestType.values()) {
            long total = dailyStats.getTotalRequestCount(type);
            long unique = dailyStats.getUniqueRequestCount(type);
            types.put(type, new RequestTypeSummary(total, unique));
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
        for (RequestType type : RequestType.values()) {
            long total = this.types.get(type).getTotalRequests() + other.types.get(type).getTotalRequests();
            long unique = this.types.get(type).getUniqueRequests() + other.types.get(type).getUniqueRequests();
            types.put(type, new RequestTypeSummary(total, unique));
        }
        StatisticsSummary combined = new StatisticsSummary(types);
        return combined;
    }
    
}


