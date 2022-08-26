package io.goobi.viewer.model.statistics.usage;

import java.time.LocalDate;

public class RequestTypeSummary {

    private final long totalRequests;
    private final long uniqueRequests;
    private final LocalDate startDate;
    private final LocalDate endDate;
    
    public RequestTypeSummary(long totalRequests, long uniqueRequests) {
        this(totalRequests, uniqueRequests, LocalDate.of(3000, 1, 1), LocalDate.ofEpochDay(0));
    }
    
    /**
     * @param totalRequests
     * @param uniqueRequests
     * @param localDate 
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
    
}
