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
package io.goobi.viewer.controller;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.servlets.utils.ServletUtils;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrTools;

/**
 * <p>
 * JsonTools class.
 * </p>
 */
public class JsonTools {

    private static final Logger logger = LogManager.getLogger(JsonTools.class);

    private static ObjectMapper mapper = new ObjectMapper();

    /**
     * Returns a <code>JSONArray</code> containing JSON objects for every <code>SolrDocument</code> in the given result. Order remains the same as in
     * the result list.
     *
     * @param result a {@link org.apache.solr.common.SolrDocumentList} object.
     * @param expanded
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @return a {@link org.json.JSONArray} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static JSONArray getRecordJsonArray(SolrDocumentList result, Map<String, SolrDocumentList> expanded, HttpServletRequest request,
            String languageToTranslate)
            throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        JSONArray jsonArray = new JSONArray();
        Locale locale = StringUtils.isBlank(languageToTranslate) ? null : Locale.forLanguageTag(languageToTranslate);
        ThumbnailHandler thumbs = BeanUtils.getImageDeliveryBean().getThumbs();

        for (SolrDocument doc : result) {
            String pi = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);

            // If the user has no listing privilege for this record, skip it
            Collection<Object> requiredAccessConditions = doc.getFieldValues(SolrConstants.ACCESSCONDITION);
            if (requiredAccessConditions != null && !requiredAccessConditions.isEmpty()) {
                Set<String> requiredAccessConditionSet = new HashSet<>();
                for (Object o : requiredAccessConditions) {
                    requiredAccessConditionSet.add((String) o);
                }
                boolean access = AccessConditionUtils.checkAccessPermission(requiredAccessConditionSet, IPrivilegeHolder.PRIV_LIST,
                        "+" + SolrConstants.PI_TOPSTRUCT + ":" + pi, request).isGranted();
                if (!access) {
                    logger.trace("User may not list {}", pi);
                    continue;
                }
            }

            try {
                JSONObject object = getAsJson(doc, locale);

                if (expanded != null && expanded.containsKey(pi)) {
                    JSONArray array = new JSONArray();
                    for (SolrDocument childDoc : expanded.get(pi)) {
                        JSONObject child = getAsJson(childDoc, locale);
                        array.put(child);
                    }
                    object.put("children", array);
                }

                jsonArray.put(object);
            } catch (JsonProcessingException e) {
                logger.error("Error writing document to json", e);
                JSONObject jsonObj = getRecordJsonObject(doc, ServletUtils.getServletPathWithHostAsUrlFromRequest(request), thumbs);
                jsonArray.put(jsonObj);
            }

        }

        return jsonArray;
    }

    /**
     * @param mapper
     * @param locale
     * @param doc
     * @return
     * @throws JsonProcessingException
     */
    public static JSONObject getAsJson(SolrDocument doc, Locale locale) throws JsonProcessingException {
        String json = mapper.writeValueAsString(doc);
        JSONObject object = new JSONObject(json);
        if (locale != null) {
            object = translateJSONObject(locale, object);
        }
        return object;
    }

    public static String getAsJson(Object object) throws JsonProcessingException {
        return mapper.writeValueAsString(object);
    }
    
    public static <T> T getAsObject(String json, Class<T> clazz) throws IOException {
        return mapper.createParser(json).readValueAs(clazz);
    }

    public static Object getAsObjectForJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return value;
        } else if (value instanceof IMetadataValue && ((IMetadataValue) value).getNumberOfUniqueTranslations() == 1) {
            return ((IMetadataValue) value).getValue().orElse("");
        } else {
            try {
                String s = getAsJson(value);
                if (StringUtils.isBlank(s)) {
                    return null;
                } else if (s.startsWith("{")) {
                    return new JSONObject(s);
                } else if (s.matches("(?i)true|false")) {
                    return Boolean.parseBoolean(s);
                } else if (s.matches("\\d+")) {
                    return Long.parseLong(s);
                } else if (s.matches("[\\d.]+")) {
                    return Double.parseDouble(s);
                } else {
                    return s;
                }
            } catch (JsonProcessingException e) {
                return value.toString();
            }
        }
    }

    /**
     * @param locale
     * @param object
     * @return
     */
    public static JSONObject translateJSONObject(Locale locale, JSONObject object) {
        JSONObject trObject = new JSONObject();
        String[] names = JSONObject.getNames(object);
        for (String name : names) {
            Object value = object.get(name);
            String trName = Messages.translate(name, locale);
            Object trValue;
            if (value instanceof String) {
                trValue = Messages.translate((String) value, locale);
            } else {
                trValue = value;
            }
            trObject.put(trName, trValue);
        }
        object = trObject;
        return object;
    }

    /**
     * JSON array of records grouped by their import date.
     *
     * @param result a {@link org.apache.solr.common.SolrDocumentList} object.
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @return a {@link org.json.JSONArray} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static JSONArray getDateCentricRecordJsonArray(SolrDocumentList result, HttpServletRequest request)
            throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        JSONArray jsonArray = new JSONArray();

        JSONArray jsonArrayUngrouped = new JSONArray();
        ThumbnailHandler thumbs = BeanUtils.getImageDeliveryBean().getThumbs();
        for (SolrDocument doc : result) {
            String pi = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);

            // If the user has no listing privilege for this record, skip it
            Collection<Object> requiredAccessConditions = doc.getFieldValues(SolrConstants.ACCESSCONDITION);
            if (requiredAccessConditions != null && !requiredAccessConditions.isEmpty()) {
                Set<String> requiredAccessConditionSet = new HashSet<>();
                for (Object o : requiredAccessConditions) {
                    requiredAccessConditionSet.add((String) o);
                }
                boolean access = AccessConditionUtils.checkAccessPermission(requiredAccessConditionSet, IPrivilegeHolder.PRIV_LIST,
                        "+" + SolrConstants.PI_TOPSTRUCT + ":" + pi.toString(), request).isGranted();
                if (!access) {
                    logger.debug("User may not list {}", pi);
                    continue;
                }
            }

            JSONObject jsonObj = getRecordJsonObject(doc, ServletUtils.getServletPathWithHostAsUrlFromRequest(request), thumbs);
            jsonArrayUngrouped.put(jsonObj);
        }

        // Group records by their import date
        String currentDateString = null;
        JSONObject currentDateJsonObject = null;
        for (int i = 0; i < jsonArrayUngrouped.length(); ++i) {
            JSONObject jsonObject = (JSONObject) jsonArrayUngrouped.get(i);
            //            logger.debug(jsonObject.toString());
            try {
                Long dateCreatedTimestamp = (Long) jsonObject.get("dateCreated");
                String dateString =
                        DateTools.format(DateTools.getLocalDateTimeFromMillis(dateCreatedTimestamp, false), DateTools.formatterISO8601Date, false);
                if (currentDateJsonObject == null || !dateString.equals(currentDateString)) {
                    currentDateString = dateString;
                    currentDateJsonObject = new JSONObject();
                    currentDateJsonObject.put("date", dateString);
                    jsonArray.put(currentDateJsonObject);
                }
                currentDateJsonObject.put("entry" + i, jsonObject);
            } catch (JSONException e) {
                logger.warn(jsonObject.get("id") + " has no " + SolrConstants.DATECREATED + " value.");
                continue;
            }
        }

        return jsonArray;
    }

    /**
     * Creates a single <code>JSONObject</code> with metadata for the given record <code>SolrDocument</code>.
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param rootUrl a {@link java.lang.String} object.
     * @param thumbs
     * @return a {@link org.json.JSONObject} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @should add all metadata
     */
    public static JSONObject getRecordJsonObject(SolrDocument doc, String rootUrl, ThumbnailHandler thumbs) throws ViewerConfigurationException {
        return getRecordJsonObject(doc, rootUrl, null, thumbs);
    }

    /**
     * Creates a single <code>JSONObject</code> with metadata for the given record <code>SolrDocument</code>.
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param rootUrl a {@link java.lang.String} object.
     * @param language a {@link java.lang.String} object.
     * @param thumbs
     * @return a {@link org.json.JSONObject} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @should add all metadata
     */
    public static JSONObject getRecordJsonObject(SolrDocument doc, String rootUrl, String language, ThumbnailHandler thumbs)
            throws ViewerConfigurationException {
        JSONObject jsonObj = new JSONObject();

        String pi = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
        String fileName = (String) doc.getFieldValue(SolrConstants.THUMBNAIL);
        String docStructType = (String) doc.getFieldValue(SolrConstants.DOCSTRCT);
        StringBuilder sbThumbnailUrl = new StringBuilder(250);
        StringBuilder sbMediumImage = new StringBuilder(250);
        try {
            StructElement ele = new StructElement(0, doc);
            if (thumbs != null) {
                sbThumbnailUrl.append(thumbs.getThumbnailUrl(ele, 100, 120));
                sbMediumImage.append(thumbs.getThumbnailUrl(ele, 600, 500));
            }
        } catch (IndexUnreachableException e) {
            logger.error("Unable to reach index for thumbnail creation");
        }

        jsonObj.put("id", pi);
        Object title = doc.getFieldValue(SolrConstants.TITLE);
        if (title == null && StringUtils.isNotEmpty(language)) {
            title = doc.getFieldValue(SolrConstants.TITLE + SolrConstants.MIDFIX_LANG + language.toUpperCase());
        }
        if (title == null) {
            title = doc.getFieldValue(SolrConstants.LABEL);
        }
        jsonObj.put("title", title);
        jsonObj.put("dateCreated", doc.getFirstValue(SolrConstants.DATECREATED));
        jsonObj.put("collection", doc.getFieldValue(SolrConstants.DC));
        if (StringUtils.isNotEmpty(fileName)) {
            jsonObj.put("thumbnailUrl", sbThumbnailUrl.toString());
            jsonObj.put("mediumimage", sbMediumImage.toString());
        }

        String docType = (String) doc.getFieldValue(SolrConstants.DOCTYPE);
        boolean isAnchor = SolrTools.isAnchor(doc);
        PageType pageType = PageType.determinePageType(docStructType, (String) doc.getFieldValue(SolrConstants.MIMETYPE),
                isAnchor || DocType.GROUP.name().equals(docType), StringUtils.isNotEmpty(fileName), false);

        String url = new StringBuilder().append(rootUrl)
                .append('/')
                .append(DataManager.getInstance().getUrlBuilder().buildPageUrl(pi, 1, "LOG_0000", pageType, true))
                .toString();
        jsonObj.put("url", url);

        // Load remaining fields from config
        for (Map<String, String> fieldConfig : DataManager.getInstance().getConfiguration().getWebApiFields()) {
            if (StringUtils.isEmpty(fieldConfig.get("jsonField")) || StringUtils.isEmpty(fieldConfig.get("luceneField"))) {
                continue;
            }
            if ("true".equals(fieldConfig.get("multivalue"))) {
                Collection<Object> values = doc.getFieldValues(fieldConfig.get("luceneField"));
                if (values != null) {
                    jsonObj.put(fieldConfig.get("jsonField"), values);
                }
            } else {
                jsonObj.put(fieldConfig.get("jsonField"), doc.getFirstValue(fieldConfig.get("luceneField")));
            }
        }

        // logger.trace("jsonObject of pi " + pi + " : " +jsonObj);
        return jsonObj;
    }

    /**
     *
     * @param json JSON string
     * @return Version information as a single line string
     * @should format string correctly
     * @should return notAvailableKey if json invalid
     */
    public static String formatVersionString(String json) {
        final String notAvailableKey = "admin__dashboard_versions_not_available";

        if (StringUtils.isEmpty(json)) {
            return notAvailableKey;
        }

        try {
            JSONObject jsonObj = new JSONObject(json);
            return jsonObj.getString("application") + " " + jsonObj.getString("version")
                    + " " + jsonObj.getString("build-date")
                    + " " + jsonObj.getString("git-revision");
        } catch (JSONException e) {
            logger.warn(e.getMessage());
            return notAvailableKey;
        }
    }

    /**
     *
     * @param json JSON string
     * @return Only version number and git hash as a single line string
     * @should format string correctly
     * @should return notAvailableKey if json invalid
     */
    public static String shortFormatVersionString(String json) {
        final String notAvailableKey = "admin__dashboard_versions_not_available";

        if (StringUtils.isEmpty(json)) {
            return notAvailableKey;
        }

        try {
            JSONObject jsonObj = new JSONObject(json);
            return jsonObj.getString("version") + " (" + jsonObj.getString("git-revision") + ")";
        } catch (JSONException e) {
            logger.warn(e.getMessage());
            return notAvailableKey;
        }
    }

    /**
     * 
     * @param json
     * @return
     */
    public static String getVersion(String json) {
        return getValue(json, "version");
    }

    public static String getGitRevision(String json) {
        return getValue(json, "git-revision");
    }

    /**
     * 
     * @param json
     * @param field
     * @return
     */
    static String getValue(String json, String field) {
        final String notAvailableKey = "admin__dashboard_versions_not_available";

        if (StringUtils.isEmpty(json)) {
            return notAvailableKey;
        }

        try {
            JSONObject jsonObj = new JSONObject(json);
            return jsonObj.getString(field);
        } catch (JSONException e) {
            logger.warn(e.getMessage());
            return notAvailableKey;
        }
    }
}
