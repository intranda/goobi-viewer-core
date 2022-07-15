package io.goobi.viewer.model.statistics.usage;

import java.time.LocalDate;

/**
 * A class holding values by which a {@link StatisticsSummary} instance should be filtered
 * @author florian
 *
 */
public class StatisticsSummaryFilter {

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
    
    public static StatisticsSummaryFilter ofDate(LocalDate date) {
        return new StatisticsSummaryFilter(date, date, "");
    }
    
    public static StatisticsSummaryFilter ofDateRange(LocalDate start, LocalDate end) {
        return new StatisticsSummaryFilter(start, end, "");
    }
    
    public static StatisticsSummaryFilter forRecord(String pi) {
        return new StatisticsSummaryFilter(LocalDate.MIN, LocalDate.MAX, "PI:" + pi);
    }
    
    public static StatisticsSummaryFilter ofDigitalCollection(String collectionName) {
        String query = "(DC:{} DC:{}.*)".replace("{}", collectionName);
        return new StatisticsSummaryFilter(LocalDate.MIN, LocalDate.MAX, query);
    }
    
    public static StatisticsSummaryFilter ofQuery(String query) {
        return new StatisticsSummaryFilter(LocalDate.MIN, LocalDate.MAX, query);
    }

    public static StatisticsSummaryFilter of(LocalDate start, LocalDate end, String query) {
        return new StatisticsSummaryFilter(start, end, query);
    }

}
