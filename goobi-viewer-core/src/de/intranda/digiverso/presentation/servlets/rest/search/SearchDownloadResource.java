/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.intranda.digiverso.presentation.servlets.rest.search;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.DateTools;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.managedbeans.SearchBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.viewer.StringPair;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;

@Path("/download/search")
@ViewerRestServiceBinding
public class SearchDownloadResource {

    private static final Logger logger = LoggerFactory.getLogger(SearchDownloadResource.class);

    @Context
    private HttpServletRequest servletRequest;

    @GET
    @Path("/waitFor")
    @Produces({ MediaType.TEXT_PLAIN })
    public boolean waitForDownloadStatus(@Context HttpServletResponse response) {
        SearchBean searchBean = BeanUtils.getSearchBean();
        long startTime = System.nanoTime();
        while (System.nanoTime() < startTime + TimeUnit.SECONDS.toNanos(10)) {
            Future<Boolean> downloadReady = searchBean.isDownloadReady();
            if (downloadReady != null) {
                try {
                    int timeout = DataManager.getInstance().getConfiguration().getExcelDownloadTimeout(); //[s]
                    return downloadReady.get(timeout, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.debug("Download interrupted");
                    return false;
                } catch (ExecutionException e) {
                    logger.debug("Download execution error", e);
                    return false;
                } catch (TimeoutException | CancellationException e) {
                    logger.debug("Downloadtimed out");
                    return false;
                } catch (Throwable e) {
                    logger.error("Unknow error " + e.toString());
                }
            }
        }
        return false;
    }

    @GET
    @Path("/excel")
    @Produces({ "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" })
    public ExcelStreamingOutput downloadAsExcel(@Context HttpServletResponse response, @Context HttpServletRequest request)
            throws DAOException, PresentationException, IndexUnreachableException, ViewerConfigurationException {
        SearchBean searchBean = (SearchBean) servletRequest.getSession().getAttribute("searchBean");
        String currentQuery = SearchHelper.prepareQuery(searchBean.getSearchString(), SearchHelper.getDocstrctWhitelistFilterSuffix());
        List<StringPair> sortFields = searchBean.getCurrentSearch().getSortFields();
        Map<String, Set<String>> searchTerms = searchBean.getSearchTerms();

        final String query = SearchHelper.buildFinalQuery(currentQuery, DataManager.getInstance().getConfiguration().isAggregateHits());

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment;filename=\"viewer_search_" + DateTools.formatterISO8601DateTime.print(System.currentTimeMillis()) + ".xlsx\"");

        Map<String, String> params = SearchHelper.generateQueryParams();
        final SXSSFWorkbook wb = SearchHelper.exportSearchAsExcel(query, currentQuery, sortFields,
                searchBean.getFacets().generateFacetFilterQueries(searchBean.getAdvancedSearchGroupOperator(), true), params, searchTerms,
                BeanUtils.getLocale(), DataManager.getInstance().getConfiguration().isAggregateHits(), servletRequest);
        try {
            return new ExcelStreamingOutput(wb);
        } finally {
        }
    }

    public static class ExcelStreamingOutput implements StreamingOutput {

        private SXSSFWorkbook wb;

        public ExcelStreamingOutput(SXSSFWorkbook wb) {
            this.wb = wb;
        }

        @Override
        public void write(OutputStream output) throws IOException, WebApplicationException {
            try {
                try {
                    Thread.sleep(3000);
                    wb.write(output);
                } finally {
                    logger.error("WRITTEN");
                    wb.dispose();
                }
            } catch (IOException e) {
                logger.trace("aborted writing excel sheet");
            } catch (Throwable e) {
                throw new WebApplicationException(e);
            }

        }
    }
}
