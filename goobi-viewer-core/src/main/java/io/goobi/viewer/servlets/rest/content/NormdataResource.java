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

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.normdataimporter.NormDataImporter;
import de.intranda.digiverso.normdataimporter.model.MarcRecord;
import de.intranda.digiverso.normdataimporter.model.NormData;
import de.intranda.digiverso.normdataimporter.model.NormDataValue;
import de.intranda.digiverso.normdataimporter.model.Record;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.servlets.rest.ViewerRestServiceBinding;

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

    /**
     * <p>
     * Constructor for NormdataResource.
     * </p>
     */
    public NormdataResource() {
    }

    /**
     * For testing
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     */
    protected NormdataResource(HttpServletRequest request) {
        this.servletRequest = request;
    }

    /**
     * Retrieves JSON representation of norm data fetched via the given URL. Only fields configured for the given template are returned (_DEFAULT if
     * template is null or not found).
     *
     * @param url a {@link java.lang.String} object.
     * @param template Type of normdata set (person, corporation, etc.)
     * @param lang Locale for field name translations
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     * @return a {@link java.lang.String} object.
     * @throws java.net.MalformedURLException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException if any.
     */
    @SuppressWarnings("unchecked")
    @GET
    @Path("/get/{url}/{template}/{lang}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public String getNormData(@PathParam("url") String url, @PathParam("template") String template, @PathParam("lang") String lang)
            throws MalformedURLException, ContentNotFoundException, ServiceNotAllowedException {
        logger.trace("getNormData: {}", url);
        if (servletResponse != null) {
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
        // logger.debug("norm data locale: {}", locale.toString());

        url = BeanUtils.unescapeCriticalUrlChracters(url.trim());
        String secondUrl = null;
        if (url.contains("$")) {
            String[] urlSplit = url.split("[$]");
            if (urlSplit.length > 1) {
                url = urlSplit[0];
                secondUrl = urlSplit[1];
            }
        }

        Record record = NormDataImporter.getSingleRecord(url);
        if (record == null) {
            throw new ContentNotFoundException("Resource not found");
        }

        List<NormData> normDataList = record.getNormDataList();
        if (normDataList == null || normDataList.isEmpty()) {
            logger.trace("Normdata map is empty");
            throw new ContentNotFoundException("Resource not found");
        }

        // Add link elements for Viaf and authority entries
        if (url.contains("viaf.org")) {
            // Viaf cluster URL
            if (secondUrl != null) {
                normDataList.add(
                        new NormData("NORM_VIAF_CLUSTER_URL", new NormDataValue(secondUrl, null, null, "resources/images/authority/Viaf_icon.png")));
            }
            // Authority URL
            NormDataValue authorityUrl = MarcRecord.getAuthorityUrlFromViafUrl(url);
            if (authorityUrl != null) {
                authorityUrl.setImageFileName("resources/images/authority/" + authorityUrl.getLabel() + ".png");
                normDataList.add(new NormData("NORM_PROVIDER", authorityUrl));
            }
        }

        JSONArray jsonArray = new JSONArray();

        // Explorative mode to return all available fields
        if (template == null || "_DEFAULT".equals(template) || "_ALL".equals(template)) {
            for (NormData normData : normDataList) {
                jsonArray.add(addNormDataValuesToJSON(normData, locale));
            }
            return jsonArray.toJSONString();
        }

        List<String> normdataFields = DataManager.getInstance().getConfiguration().getNormdataFieldsForTemplate(template);
        // Missing template config - add all fields
        if (normdataFields.isEmpty()) {
            for (NormData normData : normDataList) {
                jsonArray.add(addNormDataValuesToJSON(normData, locale));
            }
            return jsonArray.toJSONString();
        }
        // Use template config
        for (String field : normdataFields) {
            for (NormData normData : normDataList) {
                if (NormDataImporter.FIELD_URI_GND.equals(normData.getKey()) || !field.equals(normData.getKey())) {
                    continue;
                }
                jsonArray.add(addNormDataValuesToJSON(normData, locale));
            }
        }

        return jsonArray.toJSONString();
    }

    @SuppressWarnings("unchecked")
    JSONObject addNormDataValuesToJSON(NormData normData, Locale locale) {
        JSONObject jsonObj = new JSONObject();
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
            if (value.getUrl() != null && !StringTools.isImageUrl(value.getUrl())) {
                valueMap.put("url", value.getUrl());
            }
            if (value.getImageFileName() != null) {
                valueMap.put("image", value.getImageFileName());
            }
            if (value.getLabel() != null) {
                valueMap.put("label", value.getLabel());
            }
            valueList.add(valueMap);
            // If no text found, use the identifier
            if (valueMap.get("text") == null) {
                valueMap.put("text", valueMap.get("identifier"));
            }
            //                                logger.debug(jsonObj.toJSONString());
        }

        return jsonObj;
    }

    /**
     * Retrieves JSON representation of norm data fetched via the given URL.
     *
     * @param url a {@link java.lang.String} object.
     * @param lang Locale for field name translations
     * @return a {@link java.lang.String} object.
     * @throws java.net.MalformedURLException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException if any.
     */
    @GET
    @Path("/get/{url}/{lang}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getNormData(@PathParam("url") String url, @PathParam("lang") String lang)
            throws MalformedURLException, ContentNotFoundException, ServiceNotAllowedException {
        return getNormData(url, "_DEFAULT", lang);
    }
}
