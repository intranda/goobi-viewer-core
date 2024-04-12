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
package io.goobi.viewer.model.export;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.search.SearchHit;
import io.goobi.viewer.model.viewer.StringPair;

/**
 *
 */
public class RISExport {

    private static final Logger logger = LogManager.getLogger(RISExport.class);

    private final String fileName;
    private List<SearchHit> searchHits;

    /**
     * Constructor.
     * 
     * @should set fileName correctly
     */
    public RISExport() {
        this.fileName = "viewer_search_"
                + LocalDateTime.now().format(DateTools.FORMATTERFILENAME) + ".ris";
    }

    /**
     * 
     * @param finalQuery
     * @param sortFields
     * @param filterQueries
     * @param params
     * @param searchTerms
     * @param locale
     * @param proximitySearchDistance
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     * @throws ViewerConfigurationException
     * @throws ContentLibException
     * @should execute search correctly
     */
    public void executeSearch(String finalQuery, List<StringPair> sortFields, List<String> filterQueries, Map<String, String> params,
            Map<String, Set<String>> searchTerms, Locale locale, int proximitySearchDistance)
            throws IndexUnreachableException, DAOException, PresentationException, ViewerConfigurationException {
        logger.trace("exportSearchAsRIS");
        long totalHits = DataManager.getInstance().getSearchIndex().getHitCount(finalQuery, filterQueries);
        int batchSize = 100;
        int totalBatches = (int) Math.ceil((double) totalHits / batchSize);
        searchHits = new ArrayList<>((int) totalHits);
        for (int i = 0; i < totalBatches; ++i) {
            int first = i * batchSize;
            int max = first + batchSize - 1;
            if (max > totalHits) {
                max = (int) (totalHits - 1);
                batchSize = (int) (totalHits - first);
            }
            logger.trace("Fetching search hits {}-{} out of {}", first, max, totalHits);
            List<SearchHit> batch =
                    SearchHelper.searchWithAggregation(finalQuery, first, batchSize, sortFields, null, filterQueries, params, searchTerms, null, null,
                            locale, false, proximitySearchDistance);
            searchHits.addAll(batch);
        }
    }

    /**
     * @param os
     * @return True if successful; false otherwise
     * @throws IOException
     */
    public boolean writeToResponse(OutputStream os) throws IOException {
        if (os == null) {
            throw new IllegalArgumentException("os may not be null");
        }

        Path tempFile = Paths.get(DataManager.getInstance().getConfiguration().getTempFolder(), fileName);
        if (Files.exists(tempFile)) {
            FileUtils.deleteQuietly(tempFile.toFile());
        }
        logger.trace("Exporting {} search hits as RIS...", searchHits.size());
        try (FileWriter fw = new FileWriter(tempFile.toFile(), true); BufferedWriter bw = new BufferedWriter(fw)) {
            for (SearchHit searchHit : searchHits) {
                String ris = searchHit.getBrowseElement().getRisExport();
                if (ris == null) {
                    logger.warn("No RIS generated for '{}'", searchHit.getBrowseElement().getPi());
                    continue;
                }
                bw.append(ris);
            }
        }

        try (FileInputStream in = new FileInputStream(tempFile.toFile())) {
            FileTools.copyStream(os, in);
            return true;
        } catch (IOException e) {
            logger.error("Error reading RIS from temp file {}", tempFile, e);
        } finally {
            if (Files.exists(tempFile)) {
                FileUtils.deleteQuietly(tempFile.toFile());
            }
        }

        return false;
    }

    /**
     * @return true if searchHits not empty; false otherwise
     * @should return correct value
     */
    public boolean isHasResults() {
        return searchHits != null && !searchHits.isEmpty();
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return the searchHits
     */
    public List<SearchHit> getSearchHits() {
        return searchHits;
    }
}
