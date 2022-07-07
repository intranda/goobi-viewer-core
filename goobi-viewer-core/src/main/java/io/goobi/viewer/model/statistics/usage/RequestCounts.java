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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;

/**
 * @author florian
 * Class holding counts of requests of different {@link RequestType}s. Should be serialized to a string to dao storage
 */
public class RequestCounts {

    private final Map<RequestType, Long> counts = new HashMap<>();
    
    public RequestCounts() {
        
    }
    
    public RequestCounts(String data) {
        if(StringUtils.isNotBlank(data)) {            
            JSONArray array = new JSONArray(data);
            for (int i = 0; i < array.length(); i++) {
                RequestType type = RequestType.getTypeForSessionCountIndex(i);
                if(type != null) {                    
                    long count = array.getLong(i);
                    counts.put(type, count);
                }
            }
        }
    }
    
    public void setCount(RequestType type, long count) {
        this.counts.put(type, count);
    }
    
    public void incrementCount(RequestType type) {
        Long current = getCount(type);
        setCount(type, current+1);
    }
    
    public Long getCount(RequestType type) {
        return Optional.ofNullable(this.counts.get(type)).orElse(0l);
    }
    
    public String toJsonArray() {
        List<Long> countsList = new ArrayList<>(RequestType.values().length);
        for (RequestType type : counts.keySet()) {
            int index = type.getSessionCountIndex();
            Long count = counts.get(type);
            countsList.add(index, count);
        }
        return new JSONArray(countsList).toString();
    }
    
    public boolean equals(Object o) {
        if(o != null && o.getClass().equals(this.getClass())) {
            RequestCounts other = (RequestCounts)o;
            if(other.counts.size() == this.counts.size()) {
                for (RequestType type : this.counts.keySet()) {
                    Long thisCount = this.counts.get(type);
                    Long otherCount = other.counts.get(type);
                    if(!Objects.equals(thisCount, otherCount)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
}
