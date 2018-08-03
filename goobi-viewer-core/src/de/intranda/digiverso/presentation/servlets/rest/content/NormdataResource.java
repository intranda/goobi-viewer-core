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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.normdataimporter.NormDataImporter;
import de.intranda.digiverso.normdataimporter.model.NormData;
import de.intranda.digiverso.normdataimporter.model.NormDataValue;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;

/**
 * Resource for delivering norm data.
 */
@Path("/normdata")
@ViewerRestServiceBinding
public class NormdataResource {

    private static final Logger logger = LoggerFactory.getLogger(NormdataResource.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    public NormdataResource() {
    }

    /**
     * For testing
     * 
     * @param request
     */
    protected NormdataResource(HttpServletRequest request) {
        this.servletRequest = request;
    }

    /**
     * Retrieves JSON representation of norm data fetched via the given URL.
     * 
     * @param url
     * @param lang
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws MalformedURLException
     * @throws ContentNotFoundException
     * @throws ServiceNotAllowedException
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     */
    @SuppressWarnings("unchecked")
    @GET
    @Path("/get/{url}/{lang}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getContentDocument(@PathParam("url") String url, @PathParam("lang") String lang) throws MalformedURLException, ContentNotFoundException, ServiceNotAllowedException {
        if (servletResponse != null) {
            servletResponse.addHeader("Access-Control-Allow-Origin", "*");
            servletResponse.setCharacterEncoding(Helper.DEFAULT_ENCODING);
        }

        Locale locale = Locale.getDefault();
        if (lang != null) {
            switch (lang) {
                case "de":
                    locale = Locale.GERMAN;
                    break;
                case "en":
                    locale = Locale.ENGLISH;
                    break;
                default:
                    locale = new Locale(lang);
                    break;
            }
        }
        //                logger.debug("norm data locale: {}", locale.toString());

        Map<String, List<NormData>> normDataMap = NormDataImporter.importNormData(url);
        if (normDataMap != null) {
            JSONArray jsonArray = new JSONArray();
            for (String key : normDataMap.keySet()) {
                JSONObject jsonObj = new JSONObject();
                for (NormData normData : normDataMap.get(key)) {
                    if (NormDataImporter.FIELD_URI_GND.equals(normData.getKey())) {
                        continue;
                    }
                    String translation = Helper.getTranslation(normData.getKey(), locale);
                    String translatedKey = StringUtils.isNotEmpty(translation) ? translation : normData.getKey();
                    for (NormDataValue value : normData.getValues()) {
                        List<Map<String, String>> valueList = (List<Map<String, String>>) jsonObj.get(translatedKey);
                        if (jsonObj.get(translatedKey) == null) {
                            valueList = new ArrayList<>();
                            jsonObj.put(translatedKey, valueList);
                        }
                        Map<String, String> valueMap = new HashMap<>();
                        if (value.getText() != null) {
                            if (value.getText().startsWith("<div ")) {
                                // Hack to discriminate pre-build HTML page
                                valueMap.put("html", value.getText());
                            } else {
                                valueMap.put("text", value.getText());
                            }
                        }
                        if (value.getIdentifier() != null) {
                            valueMap.put("identifier", value.getIdentifier());
                        }
                        if (value.getUrl() != null) {
                            valueMap.put("url", value.getUrl());
                        }
                        valueList.add(valueMap);
                        // If no text found, use the identifier
                        if (valueMap.get("text") == null) {
                            valueMap.put("text", valueMap.get("identifier"));
                        }
                        //                                logger.debug(jsonObj.toJSONString());
                    }
                }
                jsonArray.add(jsonObj);
                break; // break after first
            }

            return jsonArray.toJSONString();
        }

        throw new ContentNotFoundException("Resource not found");
    }

}
