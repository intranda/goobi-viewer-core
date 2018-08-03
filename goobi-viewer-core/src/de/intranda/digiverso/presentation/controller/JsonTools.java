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
package de.intranda.digiverso.presentation.controller;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.SolrConstants.DocType;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.security.AccessConditionUtils;
import de.intranda.digiverso.presentation.model.security.IPrivilegeHolder;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

public class JsonTools {

    private static final Logger logger = LoggerFactory.getLogger(JsonTools.class);

    /**
     * Returns a <code>JSONArray</code> containing JSON objects for every <code>SolrDocument</code> in the given result. Order remains the same as in
     * the result list.
     *
     * @param result
     * @param request
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    @SuppressWarnings("unchecked")
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
            jsonArray.add(jsonObj);
        }

        return jsonArray;
    }

    /**
     * JSON array of records grouped by their import date.
     *
     * @param result
     * @param request
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    @SuppressWarnings("unchecked")
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
            jsonArrayUngrouped.add(jsonObj);
        }

        // Group records by their import date
        String currentDateString = null;
        JSONObject currentDateJsonObject = null;
        for (int i = 0; i < jsonArrayUngrouped.size(); ++i) {
            JSONObject jsonObject = (JSONObject) jsonArrayUngrouped.get(i);
            //            logger.debug(jsonObject.toJSONString());
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
                jsonArray.add(currentDateJsonObject);
            }
            currentDateJsonObject.put("entry" + i, jsonObject);
        }

        return jsonArray;
    }

    /**
     * Creates a single <code>JSONObject</code> with metadata for the given record <code>SolrDocument</code>.
     *
     * @param doc
     * @param rootUrl
     * @return
     * @throws ViewerConfigurationException
     * @should add all metadata
     */
    @SuppressWarnings("unchecked")
    public static JSONObject getRecordJsonObject(SolrDocument doc, String rootUrl) throws ViewerConfigurationException {
        JSONObject jsonObj = new JSONObject();

        String pi = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
        String fileName = (String) doc.getFieldValue(SolrConstants.THUMBNAIL);
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
        jsonObj.put("title", doc.getFieldValue(SolrConstants.TITLE));
        jsonObj.put("dateCreated", doc.getFirstValue(SolrConstants.DATECREATED));
        jsonObj.put("collection", doc.getFieldValue(SolrConstants.DC));
        if (StringUtils.isNotEmpty(fileName)) {
            jsonObj.put("thumbnailUrl", sbThumbnailUrl.toString());
            jsonObj.put("mediumimage", sbMediumImage.toString());
        }

        // TODO link to overview page, where applicable
        String view = PageType.viewImage.getName();
        String docType = (String) doc.getFieldValue(SolrConstants.DOCTYPE);
        boolean isAnchor = doc.getFieldValue(SolrConstants.ISANCHOR) != null && (Boolean) doc.getFieldValue(SolrConstants.ISANCHOR);
        if (StringUtils.isEmpty(fileName) || "application".equals(doc.getFieldValue(SolrConstants.MIMETYPE))) {
            view = PageType.viewMetadata.getName();
        } else if (isAnchor || DocType.GROUP.name().equals(docType)) {
            view = PageType.viewToc.getName();
        }

        String url = new StringBuilder().append(rootUrl).append('/').append(view).append('/').append(pi).append("/1/LOG_0000/").toString();
        jsonObj.put("url", url);

        // Load remaining fields from config
        for (Map<String, String> fieldConfig : DataManager.getInstance().getConfiguration().getWebApiFields()) {
            if (StringUtils.isNotEmpty(fieldConfig.get("jsonField")) && StringUtils.isNotEmpty(fieldConfig.get("luceneField"))) {
                if ("true".equals(fieldConfig.get("multivalue"))) {
                    Collection<Object> values = doc.getFieldValues(fieldConfig.get("luceneField"));
                    if (values != null) {
                        jsonObj.put(fieldConfig.get("jsonField"), values);
                    }
                } else {
                    jsonObj.put(fieldConfig.get("jsonField"), doc.getFirstValue(fieldConfig.get("luceneField")));
                }
            }
        }

        // logger.trace("jsonObject of pi " + pi + " : " +jsonObj);
        return jsonObj;
    }
}
