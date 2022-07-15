package io.goobi.viewer.model.statistics.usage;

public class RequestTypeSummary {

    private final long totalRequests;
    private final long uniqueRequests;
    /**
     * @param totalRequests
     * @param uniqueRequests
     */
    public RequestTypeSummary(long totalRequests, long uniqueRequests) {
        super();
        this.totalRequests = totalRequests;
        this.uniqueRequests = uniqueRequests;
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

    
    
}
