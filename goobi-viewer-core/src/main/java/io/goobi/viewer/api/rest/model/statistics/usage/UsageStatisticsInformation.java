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
package io.goobi.viewer.api.rest.model.statistics.usage;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class UsageStatisticsInformation {

    private final String startDate;
    private final String endDate;
    private final String query;

    /**
     * @param startDate
     * @param endDate
     * @param query
     */
    public UsageStatisticsInformation(String startDate, String endDate, String query) {
        super();
        this.startDate = startDate;
        this.endDate = endDate;
        this.query = query == null ? "" : query;
    }

    /**
     * @return the startDate
     */
    public String getStartDate() {
        return startDate;
    }

    /**
     * @return the endDate
     */
    public String getEndDate() {
        return endDate;
    }

    @JsonIgnore
    public String getDateString() {
        if (Objects.equals(startDate, endDate)) {
            return this.startDate;
        } else {
            return this.startDate + " - " + this.endDate;
        }
    }

    /**
     * @return the query
     */
    public String getQuery() {
        return query;
    }

}
