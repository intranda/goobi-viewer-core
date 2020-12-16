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
import java.util.Collections;
import java.util.LinkedList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
import io.goobi.viewer.api.rest.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.v1.records.RecordsListResource;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.JsonTools;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
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
     * @deprecated use {@link RecordsListResource#listManifests(String, Integer, Integer, String, String, String, String)} instead
     */
    @Deprecated
    @GET
    @Path("/timematrix/q/{query}/{count}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public String getTimeMatrix(@PathParam("query") String query, @PathParam("count") int count) throws MalformedURLException,
            ContentNotFoundException, ServiceNotAllowedException, IndexUnreachableException, PresentationException, ViewerConfigurationException {
        logger.trace("getTimeMatrix({}, {})", query, count);
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
        }

        if (StringUtils.isEmpty(query)) {
            throw new ContentNotFoundException("query required");
        }
        query = new StringBuilder().append('(')
                .append(query)
                .append(')')
                .append(SearchHelper.getAllSuffixes())
                .toString();
        logger.debug("query: {}", query);

        if (count <= 0) {
            count = SolrSearchIndex.MAX_HITS;
        }
        logger.trace("count: {}", count);

        JSONArray jsonArray = new JSONArray();
        String sortfield = SolrSearchIndex.generateRandomSortField();
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
        ThumbnailHandler thumbs = BeanUtils.getImageDeliveryBean().getThumbs();
        for (CompareYearSolrDocWrapper solrWrapper : sortDocResult) {
            SolrDocument doc = solrWrapper.getSolrDocument();
            JSONObject jsonObj =
                    JsonTools.getRecordJsonObject(doc, ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest), thumbs);
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
    public String getTimeMatrix(@PathParam("startDate") String startDate, @PathParam("endDate") String endDate, @PathParam("count") int count,
            @QueryParam("subtheme") String subtheme)
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

        StringBuilder query = new StringBuilder().append(SolrConstants.ISWORK)
                .append(":true");

        if (StringUtils.isNotBlank(subtheme)) {
            String subthemeField = DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField();
            query.append(" AND ")
                    .append(subthemeField)
                    .append(":")
                    .append(subtheme);

        }

        query.append(" AND ")
                .append("YEAR:[")
                .append(startDate)
                .append(" TO ")
                .append(endDate)
                .append("]")
                .toString();

        return getTimeMatrix(query.toString(), count);
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
        if (params == null || params.query == null) {
            ret.put("status", HttpServletResponse.SC_BAD_REQUEST);
            ret.put("message", "Invalid JSON request object");
            return ret.toString();
        }
        // Solr supports dynamic random_* sorting fields. Each value represents one particular order, so a random number is required.
        String query =
                new StringBuilder().append(params.query).append(SearchHelper.getAllSuffixes(servletRequest, true, true)).toString();
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
     * @throws RecordNotFoundException
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

        ViewManager viewManager;
        try {
            viewManager = ViewManager.createViewManager(pi);
        } catch (RecordNotFoundException e) {
            throw new ContentNotFoundException("Resource not found: " + pi);
        }
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
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
        }
    }
}
