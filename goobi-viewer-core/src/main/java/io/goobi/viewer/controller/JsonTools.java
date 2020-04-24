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
package io.goobi.viewer.controller;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.SolrConstants.DocType;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.servlets.utils.ServletUtils;

/**
 * <p>
 * JsonTools class.
 * </p>
 */
public class JsonTools {

    private static final Logger logger = LoggerFactory.getLogger(JsonTools.class);

    /**
     * Returns a <code>JSONArray</code> containing JSON objects for every <code>SolrDocument</code> in the given result. Order remains the same as in
     * the result list.
     *
     * @param result a {@link org.apache.solr.common.SolrDocumentList} object.
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @return a {@link org.json.JSONArray} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static JSONArray getRecordJsonArray(SolrDocumentList result, HttpServletRequest request)
            throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        JSONArray jsonArray = new JSONArray();

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
                        new StringBuilder(SolrConstants.PI_TOPSTRUCT).append(':').append(pi).toString(), request);
                if (!access) {
                    logger.trace("User may not list {}", pi);
                    continue;
                }
            }

            JSONObject jsonObj = getRecordJsonObject(doc, ServletUtils.getServletPathWithHostAsUrlFromRequest(request));
            jsonArray.put(jsonObj);
        }

        return jsonArray;
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
                        new StringBuilder(SolrConstants.PI_TOPSTRUCT).append(':').append(pi).toString(), request);
                if (!access) {
                    logger.debug("User may not list {}", pi);
                    continue;
                }
            }

            JSONObject jsonObj = getRecordJsonObject(doc, ServletUtils.getServletPathWithHostAsUrlFromRequest(request));
            jsonArrayUngrouped.put(jsonObj);
        }

        // Group records by their import date
        String currentDateString = null;
        JSONObject currentDateJsonObject = null;
        for (int i = 0; i < jsonArrayUngrouped.length(); ++i) {
            JSONObject jsonObject = (JSONObject) jsonArrayUngrouped.get(i);
            //            logger.debug(jsonObject.toString());
            Long dateCreatedTimestamp = (Long) jsonObject.get("dateCreated");
            if (dateCreatedTimestamp == null) {
                logger.warn(jsonObject.get("id") + " has no " + SolrConstants.DATECREATED + " value.");
                continue;
            }
            String dateString = DateTools.formatterISO8601Date.print(dateCreatedTimestamp);
            if (currentDateJsonObject == null || !dateString.equals(currentDateString)) {
                currentDateString = dateString;
                currentDateJsonObject = new JSONObject();
                currentDateJsonObject.put("date", dateString);
                jsonArray.put(currentDateJsonObject);
            }
            currentDateJsonObject.put("entry" + i, jsonObject);
        }

        return jsonArray;
    }

    /**
     * Creates a single <code>JSONObject</code> with metadata for the given record <code>SolrDocument</code>.
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param rootUrl a {@link java.lang.String} object.
     * @should add all metadata
     * @return a {@link org.json.simple.JSONObject} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static JSONObject getRecordJsonObject(SolrDocument doc, String rootUrl) throws ViewerConfigurationException {
        return getRecordJsonObject(doc, rootUrl, null);
    }

    /**
     * Creates a single <code>JSONObject</code> with metadata for the given record <code>SolrDocument</code>.
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param rootUrl a {@link java.lang.String} object.
     * @param language a {@link java.lang.String} object.
     * @should add all metadata
     * @return a {@link org.json.simple.JSONObject} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static JSONObject getRecordJsonObject(SolrDocument doc, String rootUrl, String language) throws ViewerConfigurationException {
        JSONObject jsonObj = new JSONObject();

        String pi = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
        String fileName = (String) doc.getFieldValue(SolrConstants.THUMBNAIL);
        String docStructType = (String) doc.getFieldValue(SolrConstants.DOCSTRCT);
        StringBuilder sbThumbnailUrl = new StringBuilder(250);
        StringBuilder sbMediumImage = new StringBuilder(250);
        try {
            StructElement ele = new StructElement(0, doc);
            sbThumbnailUrl.append(BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(ele, 100, 120));
            sbMediumImage.append(BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(ele, 600, 500));
        } catch (IndexUnreachableException e) {
            logger.error("Unable to reach index for thumbnail creation");
        }

        jsonObj.put("id", pi);
        Object title = doc.getFieldValue(SolrConstants.TITLE);
        if (title == null && StringUtils.isNotEmpty(language)) {
            title = doc.getFieldValue(SolrConstants.TITLE + SolrConstants._LANG_ + language.toUpperCase());
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
        boolean isAnchor = doc.getFieldValue(SolrConstants.ISANCHOR) != null && (Boolean) doc.getFieldValue(SolrConstants.ISANCHOR);
        PageType pageType = PageType.determinePageType(docStructType, (String) doc.getFieldValue(SolrConstants.MIMETYPE),
                isAnchor || DocType.GROUP.name().equals(docType), StringUtils.isNotEmpty(fileName), false);

        String url = new StringBuilder().append(rootUrl)
                .append('/')
                .append(DataManager.getInstance().getUrlBuilder().buildPageUrl(pi, 1, "LOG_0000", pageType))
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
}
