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
package de.intranda.digiverso.presentation.servlets;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.DateTools;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrConstants.DocType;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.metadata.CompareYearSolrDocWrapper;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.security.AccessConditionUtils;
import de.intranda.digiverso.presentation.model.security.IPrivilegeHolder;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.model.viewer.StringPair;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

/**
 * Web API servlet.
 */
public class WebApiServlet extends HttpServlet implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(WebApiServlet.class);

    public static final int MAX_HITS = 1000000;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public WebApiServlet() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = null;
        String encoding = "utf-8";
        if (request.getParameterMap().get("action") != null && request.getParameterMap().get("action").length > 0) {
            action = request.getParameterMap().get("action")[0];
            logger.trace("Web API action: {}", action);
        }

        String[] encodingParam = request.getParameterMap().get("encoding");
        if (encodingParam != null && StringUtils.isNotEmpty(encodingParam[0])) {
            encoding = String.valueOf(encodingParam[0]);
        }

        if (StringUtils.isNotEmpty(action)) {
            StringBuilder ret = new StringBuilder();
            switch (action) {
                case "timematrix":
                case "timeline": {
                    String query = null;
                    String[] queryParameter = request.getParameterMap().get("q");
                    if (queryParameter != null && StringUtils.isNotEmpty(queryParameter[0])) {
                        // Query given as parameter
                        try {
                            // Time matrix query automatically filters by the current sub-theme discriminator value
                            query = new StringBuilder().append('(')
                                    .append(queryParameter[0])
                                    .append(')')
                                    .append(SearchHelper.getAllSuffixes(true))
                                    .toString();
                        } catch (IndexUnreachableException e) {
                            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                            return;
                        }
                        logger.debug("q: {}", query);
                    } else {
                        // Build query from other parameters
                        // Example ?action=timeline&startDate=1900&endDate=1950&count=10
                        String[] startDateParameter = request.getParameterMap().get("startDate");
                        if (startDateParameter == null || startDateParameter.length == 0 || StringUtils.isEmpty(startDateParameter[0])) {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "startDate missing.");
                            return;
                        }
                        String startDate = startDateParameter[0];
                        logger.trace("start Date: {}", startDate);

                        String[] endDateParameter = request.getParameterMap().get("endDate");
                        if (endDateParameter == null || endDateParameter.length == 0 || StringUtils.isEmpty(endDateParameter[0])) {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "endDate missing.");
                            return;
                        }
                        String endDate = endDateParameter[0];
                        logger.trace("end Date: {}", endDate);

                        try {
                            query = new StringBuilder().append('(')
                                    .append(SolrConstants.ISWORK)
                                    .append(":true AND YEAR:[")
                                    .append(startDate)
                                    .append(" TO ")
                                    .append(endDate)
                                    .append("])")
                                    .append(SearchHelper.getAllSuffixes(true))
                                    .toString();
                            logger.debug("query: {}", query);
                        } catch (IndexUnreachableException e) {
                            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                            return;
                        }
                    }

                    String[] countParameter = request.getParameterMap().get("count");
                    int count = 0;
                    if (countParameter == null || StringUtils.isEmpty(countParameter[0])) {
                        count = MAX_HITS;
                    } else {
                        count = Integer.valueOf(countParameter[0]);
                    }
                    logger.trace("count: {}", count);

                    JSONArray jsonArray = new JSONArray();
                    try {
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
                            JSONObject jsonObj = getRecordJsonObject(doc, ServletUtils.getServletPathWithHostAsUrlFromRequest(request));
                            jsonArray.add(jsonObj);
                        }
                    } catch (PresentationException e) {
                        logger.debug("PresentationException thrown here: {}", e.getMessage());
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        return;
                    } catch (IndexUnreachableException e) {
                        logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        return;
                    } catch (ViewerConfigurationException e) {
                        logger.debug("ViewerConfigurationException thrown here: {}", e.getMessage());
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        return;
                    }
                    ret.append(jsonArray.toJSONString());
                }
                    break;
                case "query": {
                    // Example ?action=query&q=ISWORK:true AND YEAR:[1900 TO *] AND YEAR:[* TO 2000]&random=true
                    String[] queryParameter = request.getParameterMap().get("q");
                    if (queryParameter == null || queryParameter.length == 0 || StringUtils.isEmpty(queryParameter[0])) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "q missing.");
                        return;
                    }
                    String query;
                    try {
                        // Custom query does not filter by the sub-theme discriminator value by default, it has to be added to the custom query via #{navigationHelper.subThemeDiscriminatorValueSubQuery}
                        query = new StringBuilder().append(queryParameter[0]).append(SearchHelper.getAllSuffixes(request, true, false)).toString();
                    } catch (IndexUnreachableException e) {
                        logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        return;
                    }
                    logger.debug("q: {}", query);

                    String[] countParameter = request.getParameterMap().get("count");
                    int count = MAX_HITS;
                    if (countParameter != null && StringUtils.isNotEmpty(countParameter[0])) {
                        count = Integer.valueOf(countParameter[0]);
                    }
                    logger.trace("count: {}", count);

                    boolean sortDescending = false;
                    String[] sortOrder = request.getParameterMap().get("sortOrder");
                    if (sortOrder != null && "desc".equals(sortOrder[0])) {
                        sortDescending = true;
                    }
                    String sortOrderString = sortDescending ? "desc" : "asc";
                    logger.trace("sortOrder: {}", sortDescending);

                    List<StringPair> sortFields = new ArrayList<>();
                    String[] sortFieldArray = request.getParameterMap().get("sortField");
                    if (sortFieldArray != null) {
                        sortFields.clear();
                        for (String sortField : sortFieldArray) {
                            if (StringUtils.isNotEmpty(sortField)) {
                                sortFields.add(new StringPair(sortField, sortOrderString));
                            }
                        }
                        logger.trace("sortFields: {}", sortFields.toString());
                    }

                    boolean randomize = false;
                    String[] randomParameter = request.getParameterMap().get("random");
                    if (randomParameter != null && StringUtils.isNotEmpty(randomParameter[0])) {
                        randomize = Boolean.valueOf(randomParameter[0]);
                    }
                    logger.trace("randomize: {}", randomize);

                    String jsonFormat = "";
                    String[] jsonFormatParameter = request.getParameterMap().get("jsonFormat");
                    if (jsonFormatParameter != null && StringUtils.isNotEmpty(jsonFormatParameter[0])) {
                        jsonFormat = String.valueOf(jsonFormatParameter[0]);
                        logger.trace("jsonFormat: {}", jsonFormat);
                    }

                    if (randomize) {
                        sortFields.clear();
                        // Solr supports dynamic random_* sorting fields. Each value represents one particular order, so a random number is required.
                        Random random = new Random();
                        sortFields.add(new StringPair("random_" + random.nextInt(Integer.MAX_VALUE), sortOrderString));
                        logger.trace("sortFields: {}", sortFields);
                    }

                    try {
                        SolrDocumentList result =
                                DataManager.getInstance().getSearchIndex().search(query, 0, count, sortFields, null, null).getResults();

                        JSONArray jsonArray = null;
                        switch (jsonFormat) {
                            case "datecentric":
                                jsonArray = getDateCentricRecordJsonArray(result, request);
                                break;
                            default:
                                jsonArray = getRecordJsonArray(result, request);
                                break;
                        }
                        if (jsonArray == null) {
                            jsonArray = new JSONArray();
                        }

                        ret.append(jsonArray.toJSONString());
                    } catch (PresentationException e) {
                        logger.debug("PresentationException thrown here: {}", e.getMessage());
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        return;
                    } catch (IndexUnreachableException e) {
                        logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        return;
                    } catch (DAOException e) {
                        logger.debug("DAOException thrown here: {}", e.getMessage());
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        return;
                    } catch (ViewerConfigurationException e) {
                        logger.debug("ViewerConfigurationException thrown here: {}", e.getMessage());
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                    }
                }
                    break;
                case "count": {
                    // Example ?action=count&q=ISWORK:true AND YEAR:[1900 TO *] AND YEAR:[* TO 2000]
                    String[] queryParameter = request.getParameterMap().get("q");
                    if (queryParameter == null || queryParameter.length == 0 || StringUtils.isEmpty(queryParameter[0])) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "q missing.");
                        return;
                    }
                    String query;
                    try {
                        // Solr supports dynamic random_* sorting fields. Each value represents one particular order, so a random number is required.
                        query = new StringBuilder().append(queryParameter[0]).append(SearchHelper.getAllSuffixes(request, true, false)).toString();
                    } catch (IndexUnreachableException e) {
                        logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        return;
                    }
                    logger.debug("q: {}", query);

                    String[] sortOrder = request.getParameterMap().get("sortOrder");
                    if (sortOrder != null && "desc".equals(sortOrder[0])) {
                    }

                    String jsonFormat = "";
                    String[] jsonFormatParameter = request.getParameterMap().get("jsonFormat");
                    if (jsonFormatParameter != null && StringUtils.isNotEmpty(jsonFormatParameter[0])) {
                        jsonFormat = String.valueOf(jsonFormatParameter[0]);
                        logger.trace("jsonFormat: {}", jsonFormat);
                    }

                    try {
                        long count = DataManager.getInstance().getSearchIndex().search(query, 0, 0, null, null, null).getResults().getNumFound();

                        //                        JSONArray jsonArray ;
                        //                        if (jsonArray == null) {
                        //                            jsonArray = new JSONArray();
                        //                        }
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("count", count);
                        ret.append(jsonObject.toJSONString());
                    } catch (PresentationException e) {
                        logger.debug("PresentationException thrown here: {}", e.getMessage());
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        return;
                    } catch (IndexUnreachableException e) {
                        logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        return;
                    }
                }
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
                    return;
            }

            response.setCharacterEncoding(encoding);
            response.getWriter().write(ret.toString());
        }
    }

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
    private static JSONArray getRecordJsonArray(SolrDocumentList result, HttpServletRequest request)
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
    private static JSONArray getDateCentricRecordJsonArray(SolrDocumentList result, HttpServletRequest request)
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
    protected static JSONObject getRecordJsonObject(SolrDocument doc, String rootUrl) throws ViewerConfigurationException {
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

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
