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
package io.goobi.viewer.managedbeans;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocumentList;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ApplicationInfo;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ApplicationResource;
import io.goobi.viewer.Version;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.JsonTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.statistics.usage.StatisticsLuceneFields;
import io.goobi.viewer.model.statistics.usage.StatisticsSummary;
import io.goobi.viewer.model.statistics.usage.StatisticsSummaryBuilder;
import io.goobi.viewer.model.statistics.usage.StatisticsSummaryFilter;
import io.goobi.viewer.model.viewer.StringPair;

/**
 * Bean for the statistics page.
 */
@Named
@ApplicationScoped
public class StatisticsBean implements Serializable {

    private static final long serialVersionUID = -1530519697198096431L;

    private static final Logger logger = LogManager.getLogger(StatisticsBean.class);

    private transient Map<String, StatisticsSummary> recordUsageStatisticsMap = new ConcurrentHashMap<>();

    /**
     * getCoreVersion.
     *
     * @return goobi-viewer-core version
     */
    public String getCoreVersion() {
        return JsonTools.shortFormatVersionString(Version.asJSON());
    }

    /**
     * getConnectorVersion.
     *
     * @return goobi-viewer-connector version
     */
    public String getConnectorVersion() {
        return JsonTools.shortFormatVersionString(DataManager.getInstance().getConnectorVersion());
    }

    /**
     * getContentServerVersion.
     *
     * @return intrandaContentServer version
     */
    public String getContentServerVersion() {
        try {
            ApplicationInfo info = new ApplicationResource().getApplicationInfo();
            String json = new ObjectMapper().writeValueAsString(info);
            return JsonTools.shortFormatVersionString(json);
        } catch (ContentNotFoundException | IOException e) {
            logger.error(e.getMessage());
            return "";
        }
    }

    /**
     * getIndexerVersion.
     *
     * @return goobi-viewer-indexer version
     */
    public String getIndexerVersion() {
        return JsonTools.shortFormatVersionString(DataManager.getInstance().getIndexerVersion());
    }

    /**
     * getUsageStatisticsForRecord.
     *
     * @param pi persistent identifier of the record
     * @return {@link io.goobi.viewer.model.statistics.usage.StatisticsSummary}
     * @throws io.goobi.viewer.exceptions.PresentationException
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @throws io.goobi.viewer.exceptions.DAOException
     */
    public StatisticsSummary getUsageStatisticsForRecord(String pi) throws PresentationException, IndexUnreachableException, DAOException {
        if (StringUtils.isNotBlank(pi)) {

            StatisticsSummary summary = recordUsageStatisticsMap.get(pi);
            if (summary == null || summary.isOlderThan(1, ChronoUnit.DAYS)) {
                summary = loadSummary(pi);
                recordUsageStatisticsMap.put(pi, summary);
            }
            return summary;
        }
        return new StatisticsSummary(Collections.emptyMap());
    }

    protected StatisticsSummary loadSummary(String pi) throws IndexUnreachableException, PresentationException, DAOException {
        StatisticsSummaryFilter filter = StatisticsSummaryFilter.forRecord(pi);
        try {
            return new StatisticsSummaryBuilder().loadSummary(filter);
        } catch (WebApplicationException e) {
            logger.error(e.getMessage());
            return new StatisticsSummary(Collections.emptyMap());
        }
    }

    /**
     * getLastUsageStatisticsCheck.
     *
     * @return the date of the most recent usage statistics entry in the Solr index
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public LocalDate getLastUsageStatisticsCheck() throws IndexUnreachableException {
        try {
            SolrDocumentList docs = DataManager.getInstance()
                    .getSearchIndex()
                    .search(
                            "DOCTYPE:" + io.goobi.viewer.model.statistics.usage.StatisticsLuceneFields.USAGE_STATISTICS_DOCTYPE,
                            1, Arrays.asList(new StringPair(StatisticsLuceneFields.DATE, "desc")),
                            Arrays.asList(StatisticsLuceneFields.DATE));
            if (docs.size() == 1) {
                Date date = (Date) docs.get(0).getFieldValue(StatisticsLuceneFields.DATE);
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            return LocalDate.MIN;
        } catch (PresentationException e) {
            logger.error("Error getting last usage statistics check from solr", e);
            return null;
        }

    }

    /**
     * isUsageStatisticsActive.
     *
     * @return true if usage statistics collection is currently active, false otherwise
     */
    public boolean isUsageStatisticsActive() {
        return DataManager.getInstance().getConfiguration().isStatisticsEnabled();
    }
}
