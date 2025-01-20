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
package io.goobi.viewer.api.rest.v1.authorities;

import static io.goobi.viewer.api.rest.v1.ApiUrls.AUTHORITY;
import static io.goobi.viewer.api.rest.v1.ApiUrls.AUTHORITY_RESOLVER;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.intranda.digiverso.normdataimporter.NormDataImporter;
import de.intranda.digiverso.normdataimporter.model.MarcRecord;
import de.intranda.digiverso.normdataimporter.model.NormData;
import de.intranda.digiverso.normdataimporter.model.NormDataValue;
import de.intranda.digiverso.normdataimporter.model.Record;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.metadata.MetadataTools;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * Resolver for normdata authority resources identified by their escaped url.
 *
 * @author florian
 *
 */
@jakarta.ws.rs.Path(AUTHORITY)
@ViewerRestServiceBinding
public class AuthorityResource {

    private static final Logger logger = LogManager.getLogger(AuthorityResource.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    public AuthorityResource() {
        //
    }

    @GET
    @jakarta.ws.rs.Path(AUTHORITY_RESOLVER)
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    @Operation(tags = { "authority" }, summary = "Get a normdata authority resource identified by its escaped url")
    public String getIdentity(
            @Parameter(description = "Identifier url of the resource") @QueryParam("id") final String inUrl,
            @Parameter(description = "Metadata template to use") @QueryParam("template") String template,
            @Parameter(description = "Language to use for metadata fields") @QueryParam("lang") String lang)
            throws ContentNotFoundException, PresentationException {
        
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(StringTools.DEFAULT_ENCODING);
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

        String url = (inUrl == null ? "" : StringTools.unescapeCriticalUrlChracters(inUrl.trim()));
        String secondUrl = null;
        if (url.contains("$")) {
            String[] urlSplit = url.split("[$]");
            if (urlSplit.length > 1) {
                url = urlSplit[0];
                secondUrl = urlSplit[1];
            }
        }

        Record rec = MetadataTools.getAuthorityDataRecord(url);
        if (rec == null) {
            logger.trace("Record not found");
            throw new ContentNotFoundException(StringConstants.EXCEPTION_RESOURCE_NOT_FOUND);
        }

        List<NormData> normDataList = rec.getNormDataList();
        if (normDataList == null) {
            logger.trace("Normdata map is empty");
            throw new ContentNotFoundException(StringConstants.EXCEPTION_RESOURCE_NOT_FOUND);
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
        if (template == null || StringConstants.DEFAULT_NAME.equals(template) || "_ALL".equals(template)) {
            for (NormData normData : normDataList) {
                jsonArray.put(addNormDataValuesToJSON(normData, locale));
            }
            return jsonArray.toString();
        }

        List<String> normdataFields = DataManager.getInstance().getConfiguration().getNormdataFieldsForTemplate(template);
        // Missing template config - add all fields
        if (normdataFields.isEmpty()) {
            for (NormData normData : normDataList) {
                jsonArray.put(addNormDataValuesToJSON(normData, locale));
            }
            return jsonArray.toString();
        }
        // Use template config
        for (String field : normdataFields) {
            for (NormData normData : normDataList) {
                if (NormDataImporter.FIELD_URI_GND.equals(normData.getKey()) || !field.equals(normData.getKey())) {
                    continue;
                }
                jsonArray.put(addNormDataValuesToJSON(normData, locale));
            }
        }

        try {
            return jsonArray.toString();
        } catch (NoSuchMethodError e) {
            throw new PresentationException("Error creating json of normdata from " + url);
        }
    }

    /**
     * 
     * @param normData
     * @param locale
     * @return {@link JSONObject}
     */
    static JSONObject addNormDataValuesToJSON(NormData normData, Locale locale) {
        JSONObject jsonObj = new JSONObject();
        String translation = ViewerResourceBundle.getTranslation(normData.getKey(), locale);
        String translatedKey = StringUtils.isNotEmpty(translation) ? translation : normData.getKey();
        for (NormDataValue value : normData.getValues()) {
            JSONArray valueList;
            try {
                valueList = (JSONArray) jsonObj.get(translatedKey);

            } catch (JSONException e) {
                valueList = new JSONArray();
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
            valueList.put(valueMap);
            // If no text found, use the identifier
            if (valueMap.get("text") == null) {
                valueMap.put("text", valueMap.get("identifier"));
            }
            //                                logger.debug(jsonObj.toString());
        }

        return jsonObj;
    }

}
