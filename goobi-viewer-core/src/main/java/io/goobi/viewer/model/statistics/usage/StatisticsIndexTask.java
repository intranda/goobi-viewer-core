package io.goobi.viewer.model.statistics.usage;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.solr.SolrSearchIndex;

public class StatisticsIndexTask {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsIndexTask.class);

    private static final long DELAY_BETWEEN_INDEX_CHECKS_SECONDS = 2 * 60l;
    private static final long TIMEOUT_INDEX_CHECKS_HOURS = 2 * 24l;

    private final IDAO dao;
    private final StatisticsIndexer indexer;
    private final SolrSearchIndex solrIndex;

    public StatisticsIndexTask(IDAO dao, StatisticsIndexer indexer, SolrSearchIndex solrIndex) {
        this.dao = dao;
        this.indexer = indexer;
        this.solrIndex = solrIndex;
    }

    public StatisticsIndexTask() throws DAOException {
        this.dao = DataManager.getInstance().getDao();
        this.indexer = new StatisticsIndexer();
        this.solrIndex = DataManager.getInstance().getSearchIndex();
    }

    public void startTask() throws DAOException, IOException {

        List<DailySessionUsageStatistics> stats = this.dao.getAllUsageStatistics();
        if (!stats.isEmpty()) {
            logger.info("Moving {} daily usage statistics to SOLR", stats.size());
            for (DailySessionUsageStatistics stat : stats) {
                Path indexFile = indexer.indexStatistics(stat);
                logger.info("Written usage statistics for {} to file {}", stat.getDate(), indexFile);
            }
        }
        
        long timeStartIndexing = System.currentTimeMillis();
        Map<DailySessionUsageStatistics, Boolean> statsIndexed = stats.stream().collect(Collectors.toMap(Function.identity(), s -> Boolean.FALSE));
        try {
            while (System.currentTimeMillis() < timeStartIndexing + getTimeoutMillis()) {
                Thread.sleep(getCheckDelayMillis());
                List<DailySessionUsageStatistics> statsNotIndexed =
                        statsIndexed.entrySet().stream().filter(e -> !e.getValue()).map(Entry::getKey).collect(Collectors.toList());
                for (DailySessionUsageStatistics stat : statsNotIndexed) {
                    //TODO: Get solr docs    this.solrIndex.search("+DOCTYPE:USAGE_STATISTICS +usage_statistics_date:" + stat.getDate());
                    //if doc exists
                    {
                        statsIndexed.put(stat, Boolean.TRUE);
                    }
                }
                if (!statsIndexed.containsValue(Boolean.FALSE)) {
                    //all stats indexed
                    break;
                }
            }
        } catch (InterruptedException e1) {
            logger.warn("Checking indexed status of usage statistics has been interrupted");
            Thread.currentThread().interrupt();
        }
    }

    private long getCheckDelayMillis() {
        return Duration.of(DELAY_BETWEEN_INDEX_CHECKS_SECONDS, ChronoUnit.SECONDS).toMillis();
    }

    private long getTimeoutMillis() {
        return Duration.of(TIMEOUT_INDEX_CHECKS_HOURS, ChronoUnit.HOURS).toMillis();
    }

}
