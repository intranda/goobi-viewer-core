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

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;

/**
 * Class holding counts of requests of different {@link RequestType}s. Should be serialized to a string to dao storage
 * 
 * @author florian
 */
public class SessionRequestCounts {

    private final Map<RequestType, Long> counts = new EnumMap<>(RequestType.class);

    /**
     * Empty default constructor
     */
    public SessionRequestCounts() {

    }

    /**
     * Constructor to deserialize data from a string
     * 
     * @param data
     */
    public SessionRequestCounts(String data) {
        if (StringUtils.isNotBlank(data)) {
            JSONArray array = new JSONArray(data);
            for (int i = 0; i < array.length(); i++) {
                RequestType type = RequestType.getTypeForSessionCountIndex(i);
                if (type != null) {
                    long count = array.getLong(i);
                    counts.put(type, count);
                }
            }
        }
    }

    /**
     * Set the total count of requests for a given {@link RequestType}
     * 
     * @param type
     * @param count
     */
    public void setCount(RequestType type, long count) {
        this.counts.put(type, count);
    }

    /**
     * Increment the total count of requests for a given {@link RequestType} by one
     * 
     * @param type
     */
    public void incrementCount(RequestType type) {
        Long current = getCount(type);
        setCount(type, current + 1);
    }

    /**
     * Get the total count of requests for a given {@link RequestType}
     * 
     * @param type
     * @return {@link Long}
     */
    public Long getCount(RequestType type) {
        return Optional.ofNullable(this.counts.get(type)).orElse(0L);
    }

    /**
     * Turn into a json representation
     * 
     * @return a json String
     */
    public String toJsonArray() {
        int numTypes = RequestType.values().length;
        List<Long> countsList = Arrays.asList(new Long[numTypes]);
        for (int index = 0; index < numTypes; index++) {
            RequestType type = RequestType.getTypeForSessionCountIndex(index);
            Long count = Optional.ofNullable(counts.get(type)).orElse(0L);
            countsList.set(index, count);
        }
        return new JSONArray(countsList).toString();
    }

    @Override
    public int hashCode() {
        return this.counts.hashCode();
    }

    /**
     * Two SessionRequestCounts are equal if the have the same request counts for all {@link RequestType}s.
     * @param o
     * @return true if objects equal; false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o != null && o.getClass().equals(this.getClass())) {
            SessionRequestCounts other = (SessionRequestCounts) o;
            if (other.counts.size() == this.counts.size()) {
                for (Entry<RequestType, Long> entry : this.counts.entrySet()) {
                    RequestType type = entry.getKey();
                    Long thisCount = entry.getValue();
                    Long otherCount = other.counts.get(type);
                    if (!Objects.equals(thisCount, otherCount)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
}
