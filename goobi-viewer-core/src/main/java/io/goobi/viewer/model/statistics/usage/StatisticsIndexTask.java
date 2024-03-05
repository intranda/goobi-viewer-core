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

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.api.rest.model.tasks.TaskManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;

/**
 * Task managed by the viewers {@link TaskManager} which indexes all {@link DailySessionUsageStatistics} up to the day before today. The task monitors
 * the indexing progress and deletes the {@link DailySessionUsageStatistics} for each day once a STATISTICS_USAGE document exists in the index for
 * that day. Should run in its own thread
 * 
 * @author florian
 *
 */
public class StatisticsIndexTask {

    private static final Logger logger = LogManager.getLogger(StatisticsIndexTask.class);

    private static final long DELAY_BETWEEN_INDEX_CHECKS_SECONDS = 2 * 60L;
    private static final long TIMEOUT_INDEX_CHECKS_HOURS = 2 * 24L;

    private final IDAO dao;
    private final StatisticsIndexer indexer;
    private final SolrSearchIndex solrIndex;

    /**
     * Default constructor
     * 
     * @param dao the DAO from which to collect the usage statistics
     * @param indexer the {@link StatisticsIndexer} to use for indexing
     * @param solrIndex the {@link SolrSearchIndex} for querying the SOLR for indexed STATISTICS_USAGE documents
     */
    public StatisticsIndexTask(IDAO dao, StatisticsIndexer indexer, SolrSearchIndex solrIndex) {
        this.dao = dao;
        this.indexer = indexer;
        this.solrIndex = solrIndex;
    }

    /**
     * Constructor using instances from {@link DataManager}
     * 
     * @throws DAOException
     */
    public StatisticsIndexTask() throws DAOException {
        this.dao = DataManager.getInstance().getDao();
        this.indexer = new StatisticsIndexer();
        this.solrIndex = DataManager.getInstance().getSearchIndex();
    }

    /**
     * Start the indexint
     * 
     * @throws DAOException
     * @throws IOException
     */
    public void startTask() throws DAOException, IOException {

        List<DailySessionUsageStatistics> stats = this.dao.getAllUsageStatistics()
                .stream()
                .filter(stat -> stat.getDate().isBefore(LocalDate.now()))
                .toList();
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
                        statsIndexed.entrySet().stream().filter(e -> !e.getValue()).map(Entry::getKey).toList();
                for (DailySessionUsageStatistics stat : statsNotIndexed) {
                    String query = String.format("+%s:%s +%s:\"%s\"",
                            SolrConstants.DOCTYPE,
                            StatisticsLuceneFields.USAGE_STATISTICS_DOCTYPE,
                            StatisticsLuceneFields.DATE,
                            StatisticsLuceneFields.SOLR_DATE_FORMATTER.format(stat.getDate().atStartOfDay()));
                    SolrDocumentList list = this.solrIndex.search(query);
                    if (!list.isEmpty()) {
                        logger.info("Indexing of usage statistics for {} finished", stat.getDate());
                        statsIndexed.put(stat, Boolean.TRUE);
                        logger.info("Deleting usage statistics in DAO for {}", stat.getDate());
                        this.dao.deleteUsageStatistics(stat.getId());
                    }
                }
                if (!statsIndexed.containsValue(Boolean.FALSE)) {
                    logger.info("all statistics from database have been moved to solr");
                    break;
                }
            }
        } catch (InterruptedException e1) {
            logger.warn("Checking indexed status of usage statistics has been interrupted");
            Thread.currentThread().interrupt();
        } catch (PresentationException | IndexUnreachableException e1) {
            logger.warn("Checking indexed status of usage statistics failed with error {}", e1.toString());
        }
    }

    private static long getCheckDelayMillis() {
        return Duration.of(DELAY_BETWEEN_INDEX_CHECKS_SECONDS, ChronoUnit.SECONDS).toMillis();
    }

    private static long getTimeoutMillis() {
        return Duration.of(TIMEOUT_INDEX_CHECKS_HOURS, ChronoUnit.HOURS).toMillis();
    }

}
