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
package de.intranda.digiverso.presentation.servlets.rest.content;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.JsonTools;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.model.metadata.CompareYearSolrDocWrapper;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.viewer.StringPair;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;

/**
 * Resource for delivering norm data.
 */
@Path("/records")
@ViewerRestServiceBinding
public class RecordsResource {

    private static final Logger logger = LoggerFactory.getLogger(RecordsResource.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    public RecordsResource() {
    }

    /**
     * For testing
     * 
     * @param request
     */
    protected RecordsResource(HttpServletRequest request) {
        this.servletRequest = request;
    }

    /**
     * Returns a JSON array containing time matrix content data for the given Solr query and size.
     * 
     * @param query Solr query
     * @param count Max number of records
     * @return JOSN array
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws MalformedURLException
     * @throws ContentNotFoundException
     * @throws ServiceNotAllowedException
     * @throws ViewerConfigurationException
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     */
    @SuppressWarnings("unchecked")
    @GET
    @Path("/timematrix/q/{query}/{count}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getTimeMatrix(@PathParam("query") String query, @PathParam("count") int count) throws MalformedURLException,
            ContentNotFoundException, ServiceNotAllowedException, IndexUnreachableException, PresentationException, ViewerConfigurationException {
        logger.trace("getTimeMatrix({}, {})", query, count);
        if (servletResponse != null) {
            servletResponse.addHeader("Access-Control-Allow-Origin", "*");
            servletResponse.setCharacterEncoding(Helper.DEFAULT_ENCODING);
        }

        if (StringUtils.isEmpty(query)) {
            throw new ContentNotFoundException("query required");
        }
        query = new StringBuilder().append('(').append(query).append(')').append(SearchHelper.getAllSuffixes(true)).toString();
        logger.debug("query: {}", query);

        if (count <= 0) {
            count = SolrSearchIndex.MAX_HITS;
        }
        logger.trace("count: {}", count);

        JSONArray jsonArray = new JSONArray();
        // Solr supports dynamic random_* sorting fields. Each value represents one particular order, so a random number is required.
        Random random = new Random();
        String sortfield = new StringBuilder().append("random_").append(random.nextInt(Integer.MAX_VALUE)).toString();
        SolrDocumentList result = DataManager.getInstance()
                .getSearchIndex()
                .search(query, 0, count, Collections.singletonList(new StringPair(sortfield, "asc")), null, null)
                .getResults();
        LinkedList<CompareYearSolrDocWrapper> sortDocResult = new LinkedList<>();
        if (result != null) {
            logger.debug("count: {} result.getNumFound: {} size: {}", count, result.getNumFound(), result.size());
            for (SolrDocument doc : result) {
                sortDocResult.add(new CompareYearSolrDocWrapper(doc));
            }
        }

        Collections.sort(sortDocResult);
        for (CompareYearSolrDocWrapper solrWrapper : sortDocResult) {
            SolrDocument doc = solrWrapper.getSolrDocument();
            JSONObject jsonObj = JsonTools.getRecordJsonObject(doc, ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest));
            jsonArray.add(jsonObj);
        }

        return jsonArray.toJSONString();
    }

    /**
     * Returns a JSON array containing time matrix content data for the given date range and size.
     * 
     * @param startDate Lower date limit
     * @param endDate Upper date limit
     * @param count Max number of records
     * @return JSON array
     * @throws MalformedURLException
     * @throws ContentNotFoundException
     * @throws ServiceNotAllowedException
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
     */
    @GET
    @Path("/timematrix/range/{startDate}/{endDate}/{count}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getTimeMatrix(@PathParam("startDate") String startDate, @PathParam("endDate") String endDate, @PathParam("count") int count)
            throws MalformedURLException, ContentNotFoundException, ServiceNotAllowedException, IndexUnreachableException, PresentationException,
            ViewerConfigurationException {
        logger.trace("getTimeMatrix({}, {}, {})", startDate, endDate, count);
        if (StringUtils.isEmpty(startDate)) {
            throw new ContentNotFoundException("startDate required");
        }
        logger.trace("start Date: {}", startDate);

        if (StringUtils.isEmpty(endDate)) {
            throw new ContentNotFoundException("endDate required");
        }
        logger.trace("end Date: {}", endDate);

        String query = new StringBuilder().append(SolrConstants.ISWORK)
                .append(":true AND YEAR:[")
                .append(startDate)
                .append(" TO ")
                .append(endDate)
                .append("]")
                .toString();

        return getTimeMatrix(query, count);
    }

    /**
     * 
     * @param query Solr query
     * @param sortFields
     * @param sortOrder
     * @param jsonFormat
     * @param count Max number of records
     * @param offset
     * @param randomize If true, the result will contain random records within the given search parameters
     * @return JSON array
     * @throws MalformedURLException
     * @throws ContentNotFoundException
     * @throws ServiceNotAllowedException
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
     * @throws DAOException
     */
    @SuppressWarnings("unchecked")
    @POST
    @Path("/q")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String getRecordsForQuery(RecordsRequestParameters params) throws MalformedURLException, ContentNotFoundException,
            ServiceNotAllowedException, IndexUnreachableException, PresentationException, ViewerConfigurationException, DAOException {
        JSONObject ret = new JSONObject();
        if (params == null || params.getQuery() == null) {
            ret.put("status", HttpServletResponse.SC_BAD_REQUEST);
            ret.put("message", "Invalid JSON request object");
            return ret.toJSONString();
        }

        // Custom query does not filter by the sub-theme discriminator value by default, it has to be added to the custom query via #{navigationHelper.subThemeDiscriminatorValueSubQuery}
        String query = new StringBuilder().append(params.getQuery()).append(SearchHelper.getAllSuffixes(servletRequest, true, false)).toString();
        logger.trace("query: {}", query);

        int count = params.getCount();
        if (count <= 0) {
            count = SolrSearchIndex.MAX_HITS;
        }

        List<StringPair> sortFieldList = new ArrayList<>();
        if (StringUtils.isNotEmpty(params.getSortFields())) {
            String[] sortFieldArray = params.getSortFields().split(";");
            for (String sortField : sortFieldArray) {
                if (StringUtils.isNotEmpty(sortField)) {
                    sortFieldList.add(new StringPair(sortField, params.getSortOrder()));
                }
            }
            logger.trace("sortFields: {}", params.getSortFields().toString());
        }
        logger.trace("count: {}", count);
        logger.trace("offset: {}", params.getOffset());
        logger.trace("sortOrder: {}", params.getSortOrder());
        logger.trace("randomize: {}", params.isRandomize());
        logger.trace("jsonFormat: {}", params.getJsonFormat());

        if (params.isRandomize()) {
            sortFieldList.clear();
            // Solr supports dynamic random_* sorting fields. Each value represents one particular order, so a random number is required.
            Random random = new Random();
            sortFieldList.add(new StringPair("random_" + random.nextInt(Integer.MAX_VALUE), ("desc".equals(params.getSortOrder()) ? "desc" : "asc")));
        }

        SolrDocumentList result =
                DataManager.getInstance().getSearchIndex().search(query, params.getOffset(), count, sortFieldList, null, null).getResults();
        JSONArray jsonArray = null;
        if (params.getJsonFormat() != null) {
            switch (params.getJsonFormat()) {
                case "datecentric":
                    jsonArray = JsonTools.getDateCentricRecordJsonArray(result, servletRequest);
                    break;
                default:
                    jsonArray = JsonTools.getRecordJsonArray(result, servletRequest);
                    break;
            }
        } else {
            jsonArray = JsonTools.getRecordJsonArray(result, servletRequest);
        }
        if (jsonArray == null) {
            jsonArray = new JSONArray();
        }

        return jsonArray.toJSONString();
    }

    /**
     * Returns the hit count for the given query in a JSON object.
     * 
     * @param query
     * @return JSON object containing the count
     * @throws ContentNotFoundException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    @SuppressWarnings("unchecked")
    @POST
    @Path("/count")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    public String getCount(RecordsRequestParameters params) throws ContentNotFoundException, IndexUnreachableException, PresentationException {
        JSONObject ret = new JSONObject();
        if (params == null || params.getQuery() == null) {
            ret.put("status", HttpServletResponse.SC_BAD_REQUEST);
            ret.put("message", "Invalid JSON request object");
            return ret.toJSONString();
        }
        // Solr supports dynamic random_* sorting fields. Each value represents one particular order, so a random number is required.
        String query = new StringBuilder().append(params.getQuery()).append(SearchHelper.getAllSuffixes(servletRequest, true, false)).toString();
        logger.debug("q: {}", query);
        long count = DataManager.getInstance().getSearchIndex().search(query, 0, 0, null, null, null).getResults().getNumFound();
        ret.put("count", count);

        return ret.toJSONString();
    }
}
