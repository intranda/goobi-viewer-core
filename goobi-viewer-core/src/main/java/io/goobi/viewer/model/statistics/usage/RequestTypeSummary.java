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

import com.fasterxml.jackson.annotation.JsonIgnore;

public class RequestTypeSummary {

    private final long totalRequests;
    private final long uniqueRequests;
    @JsonIgnore
    private final LocalDate startDate;
    @JsonIgnore
    private final LocalDate endDate;

    public RequestTypeSummary(long totalRequests, long uniqueRequests) {
        this(totalRequests, uniqueRequests, LocalDate.of(3000, 1, 1), LocalDate.ofEpochDay(0));
    }

    /**
     * @param totalRequests
     * @param uniqueRequests
     * @param startDate
     * @param endDate
     */
    public RequestTypeSummary(long totalRequests, long uniqueRequests, LocalDate startDate, LocalDate endDate) {
        super();
        this.totalRequests = totalRequests;
        this.uniqueRequests = uniqueRequests;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * @return the totalRequests
     */
    public long getTotalRequests() {
        return totalRequests;
    }

    /**
     * @return the uniqueRequests
     */
    public long getUniqueRequests() {
        return uniqueRequests;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    @JsonIgnore
    public boolean isEmtpy() {
        return totalRequests == 0;
    }
}
