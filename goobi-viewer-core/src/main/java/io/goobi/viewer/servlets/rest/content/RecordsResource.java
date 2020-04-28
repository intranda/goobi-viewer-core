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
package io.goobi.viewer.servlets.rest.content;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.controller.JsonTools;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.metadata.CompareYearSolrDocWrapper;
import io.goobi.viewer.model.metadata.MetadataTools;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.toc.TOC;
import io.goobi.viewer.model.toc.export.pdf.TocWriter;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.ViewManager;
import io.goobi.viewer.servlets.rest.ViewerRestServiceBinding;
import io.goobi.viewer.servlets.utils.ServletUtils;

/**
 * Resource for JSON datasets containing records that match the given query, range, etc. Replaces <code>WebApiServlet</code>.
 */
@Path("/records")
@ViewerRestServiceBinding
public class RecordsResource {

    private static final Logger logger = LoggerFactory.getLogger(RecordsResource.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * <p>
     * Constructor for RecordsResource.
     * </p>
     */
    public RecordsResource() {
    }

    /**
     * For testing
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
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
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     * @throws java.net.MalformedURLException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    @GET
    @Path("/timematrix/q/{query}/{count}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public String getTimeMatrix(@PathParam("query") String query, @PathParam("count") int count) throws MalformedURLException,
            ContentNotFoundException, ServiceNotAllowedException, IndexUnreachableException, PresentationException, ViewerConfigurationException {
        logger.trace("getTimeMatrix({}, {})", query, count);
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(Helper.DEFAULT_ENCODING);
        }

        if (StringUtils.isEmpty(query)) {
            throw new ContentNotFoundException("query required");
        }
        query = new StringBuilder().append('(')
                .append(query)
                .append(')')
                .append(SearchHelper.getAllSuffixes(DataManager.getInstance().getConfiguration().isSubthemeAddFilterQuery()))
                .toString();
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
            jsonArray.put(jsonObj);
        }

        return jsonArray.toString();
    }

    /**
     * Returns a JSON array containing time matrix content data for the given date range and size.
     *
     * @param startDate Lower date limit
     * @param endDate Upper date limit
     * @param count Max number of records
     * @return JSON array
     * @throws java.net.MalformedURLException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
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
     * <p>
     * getRecordsForQuery.
     * </p>
     *
     * @return JSON array
     * @param params a {@link io.goobi.viewer.servlets.rest.content.RecordsRequestParameters} object.
     * @throws java.net.MalformedURLException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
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
            return ret.toString();
        }

        // Custom query does not filter by the sub-theme discriminator value by default, it has to be added to the custom query via #{navigationHelper.subThemeDiscriminatorValueSubQuery}
        String query =
                new StringBuilder().append(params.getQuery()).append(SearchHelper.getAllSuffixes(servletRequest, null, true, true, false)).toString();
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
        logger.trace("hits: {}", result.size());
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

        return jsonArray.toString();
    }

    /**
     * Returns the hit count for the given query in a JSON object.
     *
     * @return JSON object containing the count
     * @param params a {@link io.goobi.viewer.servlets.rest.content.RecordsRequestParameters} object.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    @POST
    @Path("/count")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    public String getCount(RecordsRequestParameters params) throws ContentNotFoundException, IndexUnreachableException, PresentationException {
        JSONObject ret = new JSONObject();
        if (params == null || params.getQuery() == null) {
            ret.put("status", HttpServletResponse.SC_BAD_REQUEST);
            ret.put("message", "Invalid JSON request object");
            return ret.toString();
        }
        // Solr supports dynamic random_* sorting fields. Each value represents one particular order, so a random number is required.
        String query =
                new StringBuilder().append(params.getQuery()).append(SearchHelper.getAllSuffixes(servletRequest, null, true, true, false)).toString();
        logger.debug("q: {}", query);
        long count = DataManager.getInstance().getSearchIndex().search(query, 0, 0, null, null, null).getResults().getNumFound();
        ret.put("count", count);

        return ret.toString();
    }

    /**
     * <p>
     * getRISAsFile.
     * </p>
     *
     * @param iddoc a long.
     * @return a {@link javax.ws.rs.core.StreamingOutput} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws ContentLibException
     */
    @GET
    @Path("/ris/file/{iddoc}")
    @Produces({ MediaType.TEXT_PLAIN })
    public StreamingOutput getRISAsFile(@PathParam("iddoc") long iddoc)
            throws PresentationException, IndexUnreachableException, DAOException, ContentLibException {
        StructElement se = new StructElement(iddoc);

        String fileName = se.getPi() + "_" + se.getLogid() + ".ris";
        setResponseHeader(fileName);

        if (!AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(se.getPi(), se.getLogid(), IPrivilegeHolder.PRIV_LIST, servletRequest)) {
            throw new ContentNotFoundException("Resource not found");
        }

        String ris = MetadataTools.generateRIS(se);
        if (ris == null) {
            throw new ContentNotFoundException("Resource not found");
        }

        java.nio.file.Path tempFile = Paths.get(DataManager.getInstance().getConfiguration().getTempFolder(), fileName);
        try {
            Files.write(tempFile, ris.getBytes());
        } catch (IOException e) {
            if (Files.exists(tempFile)) {
                FileUtils.deleteQuietly(tempFile.toFile());
            }
            throw new ContentLibException("Could not create RIS file " + tempFile.toAbsolutePath().toString());
        }

        return (out) -> {
            try (FileInputStream in = new FileInputStream(tempFile.toFile())) {
                FileTools.copyStream(out, in);
                out.flush();
                out.close();
            } catch (IOException e) {
                logger.trace(e.getMessage(), e);
            } finally {
                if (Files.exists(tempFile)) {
                    FileUtils.deleteQuietly(tempFile.toFile());
                }
            }
        };
    }

    /**
     * <p>
     * getRISAsText.
     * </p>
     *
     * @param iddoc a long.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @GET
    @Path("/ris/text/{iddoc}")
    @Produces({ MediaType.TEXT_PLAIN })
    public String getRISAsText(@PathParam("iddoc") long iddoc)
            throws PresentationException, IndexUnreachableException, ContentNotFoundException, DAOException {
        setResponseHeader("");
        StructElement se = new StructElement(iddoc);

        if (!AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(se.getPi(), se.getLogid(), IPrivilegeHolder.PRIV_LIST, servletRequest)) {
            throw new ContentNotFoundException("Resource not found");
        }

        return MetadataTools.generateRIS(se);
    }

    /**
     * 
     * @param pi
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws ContentNotFoundException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    @GET
    @Path("/toc/{pi}")
    @Produces({ MediaType.TEXT_PLAIN })
    public String getTOCAsText(@PathParam("pi") String pi)
            throws PresentationException, IndexUnreachableException, ContentNotFoundException, DAOException, ViewerConfigurationException {
        setResponseHeader("");

        if (!AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(pi, null, IPrivilegeHolder.PRIV_DOWNLOAD_METADATA, servletRequest)) {
            throw new ContentNotFoundException("Resource not found");
        }

        ViewManager viewManager = ViewManager.createViewManager(pi);
        TOC toc = new TOC();
        toc.generate(viewManager.getTopDocument(), viewManager.isListAllVolumesInTOC(), viewManager.getMainMimeType(), 1);
        TocWriter writer = new TocWriter("", viewManager.getTopDocument().getLabel().toUpperCase());
        writer.setLevelIndent(5);

        return writer.getAsText(toc.getTocElements());
    }

    /**
     * @param filename
     */
    private void setResponseHeader(String filename) {
        if (servletResponse != null) {
            if (StringUtils.isNotBlank(filename)) {
                servletResponse.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            }
            servletResponse.setCharacterEncoding(Helper.DEFAULT_ENCODING);
        }
    }
}
