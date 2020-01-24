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
package io.goobi.viewer.servlets.rest.metadata;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.servlets.rest.ViewerRestServiceBinding;

/**
 * <p>MetadataResource class.</p>
 */
@Path("/metadata")
@ViewerRestServiceBinding
public class MetadataResource {

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * <p>getTagsForPageJson.</p>
     *
     * @param query a {@link java.lang.String} object.
     * @param fields a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    @SuppressWarnings("unchecked")
    @GET
    @Path("/{query}/{fields}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getTagsForPageJson(@PathParam("query") String query, @PathParam("fields") String fields) throws PresentationException,
            IndexUnreachableException {
        JSONArray jsonArray = new JSONArray();

        String[] fieldsSplit = fields.split(",");
        SolrDocumentList result = DataManager.getInstance().getSearchIndex().search(query, Integer.MAX_VALUE, null, Arrays.asList(fieldsSplit));
        if (result != null && !result.isEmpty()) {
            for (SolrDocument doc : result) {
                JSONObject jsonObj = new JSONObject();
                for (String field : fieldsSplit) {
                    jsonObj.put(field, doc.getFieldValue(field));
                }
                jsonArray.add(jsonObj);
            }
        }

        return jsonArray.toJSONString();
    }
}
