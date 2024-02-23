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
import java.time.Month;

import org.apache.commons.lang3.StringUtils;

/**
 * A class holding values by which a {@link StatisticsSummary} instance should be filtered
 * 
 * @author florian
 *
 */
public final class StatisticsSummaryFilter {

    /**
     * {@link LOCAL_DATE_MIN} is not accepted as date by SQL, so this is the min date to use, 0000-01-01
     */
    public static final LocalDate LOCAL_DATE_MIN = LocalDate.of(0, Month.JANUARY, 1);
    /**
     * {@link LOCAL_DATE_MAX} is not accepted as date by SQL, so this is the max date to use, 3000-12-31
     */
    public static final LocalDate LOCAL_DATE_MAX = LocalDate.of(3000, Month.DECEMBER, 31);

    /**
     * Earliest date from which to collect data
     */
    private final LocalDate startDate;
    /**
     * Latest date from which to collect data
     */
    private final LocalDate endDate;
    /**
     * Solr query to filter results by. Only record identifiers which meet the query will be included in the summary
     */
    private final String filterQuery;

    /**
     * @param startDate
     * @param endDate
     * @param filterQuery
     */
    private StatisticsSummaryFilter(LocalDate startDate, LocalDate endDate, String filterQuery) {
        super();
        this.startDate = startDate;
        this.endDate = endDate;
        this.filterQuery = filterQuery;
    }

    /**
     * @return the startDate
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * @return the endDate
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * @return the filterQuery
     */
    public String getFilterQuery() {
        return filterQuery;
    }

    /**
     * Create an instance filtering by a single date
     * 
     * @param date
     * @return {@link StatisticsSummaryFilter}
     */
    public static StatisticsSummaryFilter ofDate(LocalDate date) {
        return new StatisticsSummaryFilter(date, date, "");
    }

    /**
     * Create an instance for a range of dates
     * 
     * @param start the first date to include
     * @param end the last date to include
     * @return {@link StatisticsSummaryFilter}
     */
    public static StatisticsSummaryFilter ofDateRange(LocalDate start, LocalDate end) {
        return new StatisticsSummaryFilter(start, end, "");
    }

    /**
     * Create an instance for a single record identifier
     * 
     * @param pi
     * @return {@link StatisticsSummaryFilter}
     */
    public static StatisticsSummaryFilter forRecord(String pi) {
        return new StatisticsSummaryFilter(LOCAL_DATE_MIN, LOCAL_DATE_MAX, StringUtils.isNotBlank(pi) ? ("PI:" + pi) : "PI:\"\"");
    }

    /**
     * Create an instance for all records within a single digital collection
     * 
     * @param collectionName
     * @return {@link StatisticsSummaryFilter}
     */
    public static StatisticsSummaryFilter ofDigitalCollection(String collectionName) {
        String query = "(DC:{} DC:{}.*)".replace("{}", collectionName);
        return new StatisticsSummaryFilter(LOCAL_DATE_MIN, LOCAL_DATE_MAX, query);
    }

    /**
     * Create an instance for all records returned by a SOLR query
     * 
     * @param query the SOLR query returning all record identifiers which to include in the summary
     * @return {@link StatisticsSummaryFilter}
     */
    public static StatisticsSummaryFilter ofQuery(String query) {
        return new StatisticsSummaryFilter(LOCAL_DATE_MIN, LOCAL_DATE_MAX, query);
    }

    /**
     * Create an instance of a date range and a SOLR query
     * 
     * @param start the first date to include
     * @param end the last date to include
     * @param query the SOLR query returning all record identifiers which to include in the summary
     * @return {@link StatisticsSummaryFilter}
     */
    public static StatisticsSummaryFilter of(LocalDate start, LocalDate end, String query) {
        return new StatisticsSummaryFilter(start, end, query);
    }

    /**
     * Check if a {@link #startDate} has been set for this filter
     * 
     * @return true if a {@link #startDate} has been set
     */
    public boolean hasStartDateRestriction() {
        return startDate.isAfter(LOCAL_DATE_MIN);
    }

    /**
     * Check if a {@link #endDate} has been set for this filter
     * 
     * @return true if a {@link #endDate} has been set
     */
    public boolean hasEndDateRestriction() {
        return endDate.isBefore(LOCAL_DATE_MAX);
    }

    /**
     * Check if the filter is set for a range of dates
     * 
     * @return true if the filter is set for a range of dates (more than a single date
     */
    public boolean isDateRange() {
        return hasStartDateRestriction() && hasEndDateRestriction() && this.endDate.isAfter(this.startDate);
    }

    /**
     * Check if a SOLR query has been set for the filter
     * 
     * @return true if a {@link #filterQuery} has been set for this filter
     */
    public boolean hasFilterQuery() {
        return StringUtils.isNotBlank(filterQuery);
    }

}
