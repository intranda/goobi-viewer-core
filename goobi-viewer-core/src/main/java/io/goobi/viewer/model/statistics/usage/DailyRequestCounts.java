package io.goobi.viewer.model.statistics.usage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONException;

public class DailyRequestCounts {

    private final Map<RequestType, Long> totalCounts = new HashMap<>();
    private final Map<RequestType, Long> uniqueCounts = new HashMap<>();
    
    public DailyRequestCounts() {
        
    }
    
    public DailyRequestCounts(String json) {
        JSONArray array = new JSONArray(json);
        if(array.length() != 6) {
            throw new JSONException("Expected json array of size 6, but got " + json);
        }
        for (RequestType type : RequestType.values()) {
            Long totalCount = array.getLong(type.getTotalCountIndex());
            Long uniqueCount = array.getLong(type.getUniqueCountIndex());
            totalCounts.put(type, totalCount);
            uniqueCounts.put(type, uniqueCount);
        }
    }
    
    public String toJsonArray() {
        JSONArray array = new JSONArray(6);
        for (int i = 0; i < 6; i+=2) {
            RequestType type = RequestType.getTypeForTotalCountIndex(i);
            Long totalCount = Optional.ofNullable(totalCounts.get(type)).orElse(0l);
            array.put(totalCount);
            Long uniqueCount = Optional.ofNullable(uniqueCounts.get(type)).orElse(0l);
            array.put(uniqueCount);
        }
        return array.toString();
    }
    
    public long getTotalCount(RequestType type) {
        return Optional.ofNullable(totalCounts.get(type)).orElse(0l);
    }
    
    public long getUniqueCount(RequestType type) {
        return Optional.ofNullable(uniqueCounts.get(type)).orElse(0l);
    }
    

}
