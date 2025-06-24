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
package io.goobi.viewer.servlets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.faces.validators.PIValidator;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.metadata.MetadataContainer;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.servlets.utils.ServletUtils;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This Servlet maps a given lucene field value to a url and then either redirects there or forwards there, depending on the config.
 */
public class IdentifierResolver extends HttpServlet {

    /** Loggers for this class. */
    private static final Logger logger = LogManager.getLogger(IdentifierResolver.class);

    private static final long serialVersionUID = 1L;

    private static final String CUSTOM_IDENTIFIER_PARAMETER = "identifier";
    private static final String CUSTOM_FIELD_PARAMETER = "field";
    private static final String PAGE_PARAMETER = "page";

    private static final String ATTRIBUTE_ERRMSG = "errMsg";

    // error messages
    //    private static final String ERRTXT_QUERY_PARSE = "Query string could not be parsed, check your input value. ";
    //    private static final String ERRTXT_DOC_NOT_FOUND = "No matching document could be found. ";
    private static final String ERRTXT_TARGET_FIELD_NOT_FOUND =
            "A document was found but it did not contain the specified target field name required for the mapping. Target field name is: ";
    private static final String ERRTXT_NO_ARGUMENT =
            "You didnt not specify a source field value for the mapping."
                    + " Append the value to the URL as a request parameter; expected param name is :";
    private static final String ERRTXT_ILLEGAL_IDENTIFIER = "Illegal identifier";
    //    private static final String ERRTXT_MULTIMATCH = "Multiple documents matched the search query. No unambiguous mapping possible.";
    //    private static final String ERRTXT_NOCFG = "The configuration file lucene_url_mapper_config.xml could not be loaded. ";

    /**
     * Fills the given field and value maps with values found in the given request parameter map.
     *
     * @param parameterMap Parameter map from a HttpServletRequest
     * @param moreFields Field names map
     * @param moreValues Field values map
     * @should parse fields and values correctly
     */
    static void parseFieldValueParameters(Map<String, String[]> parameterMap, Map<Integer, String> moreFields, Map<Integer, String> moreValues) {
        if (parameterMap == null || parameterMap.isEmpty()) {
            return;
        }

        for (Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (entry.getValue() == null || entry.getValue().length == 0) {
                continue;
            }
            if (entry.getKey().startsWith(CUSTOM_FIELD_PARAMETER) && entry.getKey().length() > CUSTOM_FIELD_PARAMETER.length()) {
                String number = entry.getKey().substring(CUSTOM_FIELD_PARAMETER.length());
                moreFields.put(Integer.valueOf(number), entry.getValue()[0]);
            } else if (entry.getKey().startsWith("value") && entry.getKey().length() > "value".length()) {
                String number = entry.getKey().substring("value".length());
                moreValues.put(Integer.valueOf(number), entry.getValue()[0]);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * For a given lucene field name parameter, this method either forwards or redirects to the target URL. The target URL is generated by inserting
     * the target lucene field into the target work url, if a document could be identified by the source field. Otherwise, a document is searched for
     * using the page field; if a document is found in this alternative way, target field and page field of the document are inserted into the target
     * page url. NOTE: If you forward, the target URL must be on the same server and must be below the context root of this servlet, e.g. this servlet
     * can not forward to a target above '/'. A redirect changes the URL displayed in the browser, a forward does not.
     * 
     * @should return 400 if record identifier missing
     * @should return 404 if record not found
     * @should return 400 if record field name bad
     * @should return 400 if record field value bad
     * @should forward to relative url
     * @should redirect to full url
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Override config settings if "id" and "field" parameters have been passed
        boolean customMode = false;
        if (request.getParameter(CUSTOM_IDENTIFIER_PARAMETER) != null && request.getParameter(CUSTOM_FIELD_PARAMETER) != null) {
            customMode = true;
        }

        String fieldName;
        String fieldValue = request.getParameter("urn");
        if (customMode) {
            fieldName = request.getParameter(CUSTOM_FIELD_PARAMETER);
            fieldValue = request.getParameter(CUSTOM_IDENTIFIER_PARAMETER);
        } else {
            fieldName = DataManager.getInstance().getConfiguration().getUrnResolverFields().get(0);
        }
        if (StringUtils.isEmpty(fieldValue)) {
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ERRTXT_NO_ARGUMENT + "urn");
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            return;
        }

        try {
            fieldValue = URLDecoder.decode(fieldValue, SearchBean.URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        }

        if (SolrConstants.PI.contentEquals(fieldName) && !PIValidator.validatePi(fieldValue)) {
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ERRTXT_ILLEGAL_IDENTIFIER + ": " + fieldValue);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            return;
        }

        logger.trace("output: {}", request.getQueryString());

        // Parse optional additional field/value pairs
        Map<Integer, String> moreFields = new HashMap<>();
        Map<Integer, String> moreValues = new HashMap<>();
        parseFieldValueParameters(request.getParameterMap(), moreFields, moreValues);

        try {
            // 3. evaluate the search
            SolrDocumentList hits = query(fieldName, fieldValue, moreFields, moreValues, request);

            if (hits.getNumFound() == 0) {
                // 3.1 start the alternative page field search
                if (!customMode) {
                    boolean found = false;
                    // Try additional configured URN fields first
                    if (DataManager.getInstance().getConfiguration().getUrnResolverFields().size() > 1) {
                        for (String f : DataManager.getInstance().getConfiguration().getUrnResolverFields()) {
                            if (!fieldName.equals(f)) {
                                hits = query(f, fieldValue, moreFields, moreValues, request);
                                if (hits.getNumFound() > 0) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                    // Page search if no hit
                    if (!found) {
                        try {
                            doPageSearch(fieldValue, request, response);
                            return;
                        } catch (IOException | ServletException e) {
                            logger.error(e.getMessage());
                        }
                    }
                } else {
                    // logger.trace("not found: {}:{}", fieldName, fieldValue); //NOSONAR Debug
                    try {
                        redirectToError(HttpServletResponse.SC_NOT_FOUND, fieldValue, request, response);
                    } catch (IOException | ServletException e) {
                        logger.error(e.getMessage());
                    }
                }
            } else if (hits.getNumFound() > 1) {
                // 3.2 show multiple match, that indicates corrupted index
                try {
                    redirectToError(HttpServletResponse.SC_CONFLICT, fieldValue, request, response);
                } catch (IOException | ServletException e) {
                    logger.error(e.getMessage());
                }
                return;
            }

            if (hits.getNumFound() == 0) {
                try {
                    redirectToError(HttpServletResponse.SC_NOT_FOUND, fieldValue, request, response);
                } catch (IOException | ServletException e) {
                    logger.error(e.getMessage());
                }
                return;
            }

            // 4. extract the target field value of the single found document
            SolrDocument targetDoc = hits.get(0);

            String pi = (String) targetDoc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
            int page = 0;

            // Deleted record check
            if (targetDoc.getFieldValue(SolrConstants.DATEDELETED) != null) {
                logger.debug("Record '{}' has been deleted, trace document found.", targetDoc.getFieldValue(SolrConstants.PI));
                try {
                    redirectToError(HttpServletResponse.SC_GONE, fieldValue, request, response);
                } catch (IOException | ServletException e) {
                    logger.error(e.getMessage());
                }
                return;
            }

            // If this is not the top level docstruct, retrieve the correct page number
            if ((targetDoc.getFieldValue(SolrConstants.ISWORK) == null || !((Boolean) targetDoc.getFieldValue(SolrConstants.ISWORK)))
                    && (targetDoc.getFieldValue(SolrConstants.ISANCHOR) == null || !((Boolean) targetDoc.getFieldValue(SolrConstants.ISANCHOR)))) {
                if (pi == null && targetDoc.getFieldValue(SolrConstants.PI_TOPSTRUCT) != null) {
                    pi = (String) targetDoc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
                }
                if (targetDoc.getFieldValue(SolrConstants.THUMBPAGENO) != null) {
                    page = (Integer) targetDoc.getFieldValue(SolrConstants.THUMBPAGENO);
                }
            }

            if (pi == null) {
                try {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, ERRTXT_TARGET_FIELD_NOT_FOUND + SolrConstants.PI);
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
                return;
            }

            // Custom page, if parameter given
            if (request.getParameter(PAGE_PARAMETER) != null) {
                try {
                    page = Integer.valueOf(request.getParameter(PAGE_PARAMETER));
                } catch (NumberFormatException e) {
                    logger.debug(e.getMessage());
                    try {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                    } catch (IOException e1) {
                        logger.error(e1.getMessage(), e1);
                    }
                    return;
                }
            }

            // If the user has no listing privilege for this record, act as if it does not exist
            boolean access = false;
            try {
                access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(pi, null, IPrivilegeHolder.PRIV_LIST, request).isGranted();
            } catch (RecordNotFoundException e) {
                try {
                    redirectToError(HttpServletResponse.SC_NOT_FOUND, fieldValue, request, response);
                } catch (ServletException | IOException e1) {
                    logger.error(e1.getMessage());
                }
                return;
            } catch (IndexUnreachableException | DAOException e) {
                try {
                    redirectToError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, fieldValue, request, response);
                } catch (ServletException | IOException e1) {
                    logger.error(e1.getMessage());
                }
            }
            if (!access) {
                logger.debug("User may not list record '{}'.", pi);
                try {
                    redirectToError(HttpServletResponse.SC_NOT_FOUND, fieldValue, request, response);
                } catch (ServletException | IOException e) {
                    logger.error(e.getMessage(), e);
                }
                return;
            }

            String result = page == 0 ? constructUrl(targetDoc, false) : constructUrl(targetDoc, false, page);
            logger.trace("URL: {}", result);

            // 5. redirect or forward using the target field value
            if (DataManager.getInstance().getConfiguration().isUrnDoRedirect()) {
                try {
                    String absoluteUrl = ServletUtils.getServletPathWithHostAsUrlFromRequest(request) + result;
                    response.sendRedirect(absoluteUrl);
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            } else {
                try {
                    getServletContext().getRequestDispatcher(result).forward(request, response);
                } catch (ServletException | IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        } catch (PresentationException | IndexUnreachableException e) {
            if (e.getMessage().contains("undefined field")) {
                try {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Undefined field name: " + fieldName);
                } catch (IOException e1) {
                    logger.error(e1.getMessage());
                }
            } else {
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                } catch (IOException e1) {
                    logger.error(e1.getMessage());
                }
            }
        }
    }

    /**
     * This private method is responsible for the alternative page field search; it is called by doGet(), if no document could be found using the
     * standard source field search.
     *
     * @param fieldValue the request param, which is the value to be searched
     * @param request simple forward
     * @param response simple forward
     * @throws IOException
     * @throws ServletException
     */
    private void doPageSearch(String fieldValue, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // logger.trace("doPageSearch {}", fieldValue); //NOSONAR Debug
        // A.1 Search for documents, that contain the request param in their page field

        // A.2 Evaluate the search
        SolrDocumentList hits;
        try {
            hits = query(SolrConstants.IMAGEURN, fieldValue, null, null, request);
        } catch (PresentationException e) {
            logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException e1) {
                logger.error(e1.getMessage(), e1);
            }
            return;
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException e1) {
                logger.error(e1.getMessage(), e1);
            }
            return;
        }
        if (hits.getNumFound() == 0) {
            // A2.1 show, that no document was found at all
            try {
                redirectToError(HttpServletResponse.SC_NOT_FOUND, fieldValue, request, response);
            } catch (IOException | ServletException e) {
                logger.error(e.getMessage());
            }
            return;
        } else if (hits.getNumFound() > 1) {
            // A2.2 show multiple match, that indicates inconsistencies within the index
            try {
                redirectToError(HttpServletResponse.SC_CONFLICT, fieldValue, request, response);
            } catch (IOException | ServletException e) {
                logger.error(e.getMessage());
            }
            return;
        }

        // Retrieve the corresponding page document from the index, then extract the main document's PI and the current page number
        SolrDocument targetDoc = hits.get(0);
        String pi = (String) targetDoc.getFieldValue(SolrConstants.PI_TOPSTRUCT);

        // If the user has no listing privilege for this record, act as if it does not exist
        boolean access;
        try {
            access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(pi, null, IPrivilegeHolder.PRIV_LIST, request).isGranted();
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException e1) {
                logger.error(e1.getMessage(), e1);
            }
            return;
        } catch (DAOException e) {
            logger.debug("DAOException thrown here: {}", e.getMessage());
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException e1) {
                logger.error(e1.getMessage(), e1);
            }
            return;
        } catch (RecordNotFoundException e) {
            try {
                redirectToError(HttpServletResponse.SC_NOT_FOUND, fieldValue, request, response);
            } catch (IOException | ServletException e1) {
                logger.error(e1.getMessage(), e1);
            }
            return;
        }
        if (!access) {
            logger.debug("User may not list {}", pi);
            try {
                redirectToError(HttpServletResponse.SC_NOT_FOUND, fieldValue, request, response);
            } catch (IOException | ServletException e1) {
                logger.error(e1.getMessage(), e1);
            }
            return;
        }

        // A.5 Form a result url by inserting the target field of the document and the in A.4 determined value into the target page url
        String result = constructUrl(targetDoc, true);
        logger.debug("URL: {}", result);
        // A.6 redirect or forward to this newly created url
        if (DataManager.getInstance().getConfiguration().isUrnDoRedirect()) {
            String absoluteUrl = ServletUtils.getServletPathWithHostAsUrlFromRequest(request) + result;
            try {
                logger.trace("Redirecting to: {}", absoluteUrl);
                response.sendRedirect(absoluteUrl);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            try {
                getServletContext().getRequestDispatcher(result).forward(request, response);
            } catch (IOException | ServletException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 
     * @param fieldName
     * @param fieldValue
     * @param moreFields
     * @param moreValues
     * @param request
     * @return {@link SolrDocumentList}
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private static SolrDocumentList query(String fieldName, String fieldValue, Map<Integer, String> moreFields, Map<Integer, String> moreValues,
            HttpServletRequest request) throws PresentationException, IndexUnreachableException {
        logger.trace("Querying field: {}", fieldName);
        StringBuilder sbQuery = new StringBuilder()
                .append('+')
                .append(ClientUtils.escapeQueryChars(fieldName.toUpperCase()))
                .append(':')
                .append('"')
                .append(ClientUtils.escapeQueryChars(fieldValue))
                .append('"');

        // Add additional field/value pairs to the query
        if (moreFields != null && moreValues != null) {
            for (Entry<Integer, String> entry : moreFields.entrySet()) {
                if (moreValues.get(entry.getKey()) != null) {
                    sbQuery.append(" +")
                            .append(entry.getValue())
                            .append(':')
                            .append(moreValues.get(entry.getKey()));
                }
            }
        }

        sbQuery.append(SearchHelper.getAllSuffixes(request, false, false));
        String query = StringTools.stripPatternBreakingChars(sbQuery.toString());
        logger.trace("query: {}", query); //NOSONAR Debug

        // 3. evaluate the search
        return DataManager.getInstance().getSearchIndex().search(query);
    }

    /**
     *
     * @param code
     * @param identifier
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    private static void redirectToError(int code, String identifier, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String url = "/error/";
        response.setStatus(code);
        switch (code) {
            case HttpServletResponse.SC_NOT_FOUND:
                request.setAttribute("type", "recordNotFound");
                request.setAttribute(ATTRIBUTE_ERRMSG,
                        ViewerResourceBundle.getTranslation("errRecordNotFoundMsg", BeanUtils.getLocale()).replace("{0}", identifier));
                break;
            case HttpServletResponse.SC_GONE:
                request.setAttribute("type", "recordDeleted");
                request.setAttribute(ATTRIBUTE_ERRMSG,
                        ViewerResourceBundle.getTranslation("errRecordDeletedMsg", BeanUtils.getLocale()).replace("{0}", identifier));
                break;
            case HttpServletResponse.SC_CONFLICT:
                request.setAttribute("type", "general");
                request.setAttribute(ATTRIBUTE_ERRMSG,
                        ViewerResourceBundle.getTranslation("errMultiMatch", BeanUtils.getLocale()).replace("{0}", identifier));
                break;
            default:
                break;
        }
        request.setAttribute("sourceUrl", NavigationHelper.getFullRequestUrl(request, null));
        request.getRequestDispatcher(url).forward(request, response);
    }

    /**
     * <p>
     * constructUrl.
     * </p>
     *
     * @param targetDoc a {@link org.apache.solr.common.SolrDocument} object
     * @param pageResolverUrl a boolean
     * @return Generated URL
     */
    public static String constructUrl(SolrDocument targetDoc, boolean pageResolverUrl) {
        int order = 1;
        if (targetDoc.containsKey(SolrConstants.THUMBPAGENO)) {
            order = (int) targetDoc.getFieldValue(SolrConstants.THUMBPAGENO);
        } else if (targetDoc.containsKey(SolrConstants.ORDER)) {
            order = (int) targetDoc.getFieldValue(SolrConstants.ORDER);
        }
        return constructUrl(targetDoc, pageResolverUrl, order);
    }

    public static String constructUrl(MetadataContainer targetDoc, boolean pageResolverUrl) {
        Integer order = 1;
        if (targetDoc.containsField(SolrConstants.THUMBPAGENO)) {
            order = targetDoc.getFirstIntValue(SolrConstants.THUMBPAGENO);
        } else if (targetDoc.containsField(SolrConstants.ORDER)) {
            order = targetDoc.getFirstIntValue(SolrConstants.ORDER);
        }
        return constructUrl(targetDoc, pageResolverUrl, order);
    }

    /**
     *
     * @param targetDoc
     * @param pageResolverUrl
     * @param order
     * @return Generated URL
     * @should construct url correctly
     * @should construct anchor url correctly
     * @should construct group url correctly
     * @should construct page url correctly
     * @should construct preferred view url correctly
     * @should construct application mime type url correctly
     */
    static String constructUrl(SolrDocument targetDoc, boolean pageResolverUrl, int order) {
        String docStructType = (String) targetDoc.getFieldValue(SolrConstants.DOCSTRCT);
        String mimeType = (String) targetDoc.getFieldValue(SolrConstants.MIMETYPE);
        String topstructPi = (String) targetDoc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
        boolean topstruct = SolrTools.getAsBoolean(targetDoc.getFieldValue(SolrConstants.ISWORK));
        boolean anchorOrGroup = SolrTools.isAnchor(targetDoc) || SolrTools.isGroup(targetDoc);
        boolean hasImages = targetDoc.containsKey(SolrConstants.ORDER) || (targetDoc.containsKey(SolrConstants.THUMBNAIL)
                && !StringUtils.isEmpty((String) targetDoc.getFieldValue(SolrConstants.THUMBNAIL)));

        PageType pageType = PageType.determinePageType(docStructType, mimeType, anchorOrGroup, hasImages, pageResolverUrl);

        StringBuilder sb = new StringBuilder("/");
        sb.append(DataManager.getInstance()
                .getUrlBuilder()
                .buildPageUrl(topstructPi, order, (String) targetDoc.getFieldValue(SolrConstants.LOGID), pageType, topstruct || anchorOrGroup));

        // logger.trace("Resolved to: {}", sb.toString()); //NOSONAR Debug
        return sb.toString();
    }

    static String constructUrl(MetadataContainer targetDoc, boolean pageResolverUrl, Integer order) {
        String docStructType = targetDoc.getFirstValue(SolrConstants.DOCSTRCT);
        String mimeType = (String) targetDoc.getFirstValue(SolrConstants.MIMETYPE);
        String topstructPi = (String) targetDoc.getFirstValue(SolrConstants.PI_TOPSTRUCT);
        boolean topstruct = SolrTools.getAsBoolean(targetDoc.getFirstValue(SolrConstants.ISWORK));
        boolean anchorOrGroup = SolrTools.isAnchor(targetDoc) || SolrTools.isGroup(targetDoc);
        boolean hasImages = targetDoc.containsField(SolrConstants.ORDER) || (targetDoc.containsField(SolrConstants.THUMBNAIL)
                && !StringUtils.isEmpty((String) targetDoc.getFirstValue(SolrConstants.THUMBNAIL)));

        PageType pageType = PageType.determinePageType(docStructType, mimeType, anchorOrGroup, hasImages, pageResolverUrl);

        StringBuilder sb = new StringBuilder("/");
        int effectiveOrder = order == null ? 1 : order;
        sb.append(DataManager.getInstance()
                .getUrlBuilder()
                .buildPageUrl(topstructPi, effectiveOrder, targetDoc.getFirstValue(SolrConstants.LOGID), pageType, topstruct || anchorOrGroup));

        // logger.trace("Resolved to: {}", sb.toString()); //NOSONAR Debug
        return sb.toString();
    }
}
