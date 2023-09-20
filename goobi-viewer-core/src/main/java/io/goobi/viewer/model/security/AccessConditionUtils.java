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
package io.goobi.viewer.model.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jboss.weld.contexts.ContextNotActiveException;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.clients.ClientApplication;
import io.goobi.viewer.model.security.clients.ClientApplicationManager;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

/**
 * <p>
 * AccessConditionUtils class.
 * </p>
 */
public class AccessConditionUtils {

    private static final Logger logger = LogManager.getLogger(AccessConditionUtils.class);

    /**
     * Private constructor to prevent instantiation.
     */
    private AccessConditionUtils() {
        //
    }

    /**
     * <p>
     * checkAccess.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param action a {@link java.lang.String} object.
     * @param pi a {@link java.lang.String} object.
     * @param contentFileName a {@link java.lang.String} object.
     * @param isThumbnail a boolean.
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static AccessPermission checkAccess(HttpServletRequest request, String action, String pi, String contentFileName, boolean isThumbnail)
            throws IndexUnreachableException, DAOException {
        if (request == null) {
            throw new IllegalArgumentException("request may not be null");
        }
        if (action == null) {
            throw new IllegalArgumentException("action may not be null");
        }
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }

        switch (action) {
            case "image":
            case "application":
                if ("pdf".equalsIgnoreCase(FilenameUtils.getExtension(contentFileName))) {
                    return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName,
                            IPrivilegeHolder.PRIV_DOWNLOAD_PDF);
                }
                if (isThumbnail) {
                    return checkAccessPermissionForThumbnail(request, pi, contentFileName);
                    //                                logger.trace("Checked thumbnail access: {}/{}: {}", pi, contentFileName, access);
                }
                return checkAccessPermissionForImage(request, pi, contentFileName);
            //                                logger.trace("Checked image access: {}/{}: {}", pi, contentFileName, access);
            case "text":
            case "ocrdump":
                return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName, IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
            case "pdf":
                return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName, IPrivilegeHolder.PRIV_DOWNLOAD_PDF);
            case "video":
                return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName, IPrivilegeHolder.PRIV_VIEW_VIDEO);
            case "audio":
                return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName, IPrivilegeHolder.PRIV_VIEW_AUDIO);
            case "dimensions":
            case "version":
                return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName, IPrivilegeHolder.PRIV_VIEW_IMAGES); // TODO is priv checking needed here?
            default: // nothing
                break;
        }

        return AccessPermission.denied();
    }

    /**
     *
     * @param identifier
     * @param fileName
     * @return Constructed query
     * @should use correct field name for AV files
     * @should use correct file name for text files
     * @should use correct file name for pdf files
     * @should adapt basic alto file name
     * @should escape file name for wildcard search correctly
     * @should work correctly with urls
     */
    static String generateAccessCheckQuery(String identifier, String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            return null;
        }

        StringBuilder sbQuery = new StringBuilder();
        String useFileField = SolrConstants.FILENAME;
        String simpleFileName = FileTools.getPathFromUrlString(fileName).getFileName().toString();
        String baseFileName = FilenameUtils.getBaseName(simpleFileName);
        sbQuery.append('+').append(SolrConstants.PI_TOPSTRUCT).append(':').append(identifier);
        // Different media types have the file name in different fields
        String extension = FilenameUtils.getExtension(fileName).toLowerCase();
        switch (extension) {
            case "webm":
            case "mp4":
            case "mp3":
            case "ogg":
            case "ogv":
            case "flv":
                sbQuery.append(" +").append(useFileField).append(':');
                // Escape whitespaces etc. for wildcard searches
                sbQuery.append(ClientUtils.escapeQueryChars(baseFileName)).append(".*");
                break;
            case "txt":
                sbQuery.append(" +(")
                        .append(SolrConstants.FILENAME_FULLTEXT)
                        .append(':')
                        .append("\"")
                        .append(fileName)
                        .append("\" ")
                        .append("FILENAME_PLAIN:\"")
                        .append(simpleFileName)
                        .append("\")");
                break;
            case "xml":
                String altoFileName = "\"" + fileName + "\"";
                if (!altoFileName.contains("/")) {
                    // Basic file name with no alto folder path received
                    altoFileName = "*/" + identifier + "/" + fileName;
                }
                sbQuery.append(" +(")
                        .append(SolrConstants.FILENAME_ALTO)
                        .append(':')
                        .append(altoFileName)
                        .append(" ")
                        .append("FILENAME_XML:\"")
                        .append(simpleFileName)
                        .append("\")");
                break;
            case "tif":
            case "tiff":
            case "jpg":
            case "jpeg":
            case "png":
            case "jp2":
            case "obj":
            case "gltf":
            case "glb":
            case "pdf":
                sbQuery.append(" +").append(useFileField).append(":\"").append(simpleFileName).append('"');
                break;
            default:
                // Escape whitespaces etc. for wildcard searches
                sbQuery.append(" +").append(useFileField).append(':').append(ClientUtils.escapeQueryChars(simpleFileName)).append(".*");
                break;
        }

        // logger.trace(sbQuery.toString());
        return sbQuery.toString();
    }

    /**
     * Checks whether the client may access an image (by PI + file name).
     *
     * @param identifier Work identifier (PI).
     * @param fileName Image file name. For all files of a record, use "*".
     * @param request Calling HttpServiceRequest.
     * @param privilegeName a {@link java.lang.String} object.
     * @return true if access is granted; false otherwise.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    static Map<String, AccessPermission> checkAccessPermissionByIdentifierAndFileName(String identifier, String fileName, String privilegeName,
            HttpServletRequest request) throws IndexUnreachableException, DAOException {
        // logger.trace("checkAccessPermissionByIdentifierAndFileName({}, {}, {})", identifier, fileName, privilegeName);
        if (StringUtils.isEmpty(identifier)) {
            return Collections.emptyMap();
        }

        String query = generateAccessCheckQuery(identifier, fileName);
        // logger.trace("query: {}", query); // Sonar considers this log msg a security issue, so leave it commented out when not needed
        try {
            // Collect access conditions required by the page
            Map<String, Set<String>> requiredAccessConditions = new HashMap<>();
            SolrDocumentList results = DataManager.getInstance()
                    .getSearchIndex()
                    .search(query, "*".equals(fileName) ? SolrSearchIndex.MAX_HITS : 1, null,
                            Arrays.asList(SolrConstants.ACCESSCONDITION));
            if (results != null) {
                if (results.isEmpty()) {
                    logger.debug("No hits for permission check query: {}", query); //NOSONAR this will help identify index inconsistencies
                }
                for (SolrDocument doc : results) {
                    Collection<Object> fieldsAccessConddition = doc.getFieldValues(SolrConstants.ACCESSCONDITION);
                    if (fieldsAccessConddition != null) {
                        Set<String> pageAccessConditions = new HashSet<>();
                        for (Object accessCondition : fieldsAccessConddition) {
                            pageAccessConditions.add(accessCondition.toString());
                            // logger.trace(accessCondition.toString());
                        }
                        requiredAccessConditions.put(fileName, pageAccessConditions);
                    }
                }
            }

            User user = BeanUtils.getUserFromRequest(request);
            if (user == null) {
                UserBean userBean = BeanUtils.getUserBean();
                if (userBean != null) {
                    user = userBean.getUser();
                }
            }

            Map<String, AccessPermission> ret = new HashMap<>(requiredAccessConditions.size());
            for (Entry<String, Set<String>> entry : requiredAccessConditions.entrySet()) {
                Set<String> pageAccessConditions = entry.getValue();
                AccessPermission access = checkAccessPermission(DataManager.getInstance().getDao().getRecordLicenseTypes(), pageAccessConditions,
                        privilegeName, user, NetTools.getIpAddress(request), ClientApplicationManager.getClientFromRequest(request), query);
                ret.put(entry.getKey(), access);
            }
            return ret;
        } catch (PresentationException e) {
            logger.debug(e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Checks whether the client may access an image (by PI + file name).
     *
     * @param request Calling HttpServiceRequest.
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @param privilegeName a {@link java.lang.String} object.
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    static AccessPermission checkAccessPermissionByIdentifierAndPageOrder(PhysicalElement page, String privilegeName, HttpServletRequest request)
            throws IndexUnreachableException, DAOException {
        if (page == null) {
            throw new IllegalArgumentException("page may not be null");
        }

        String query = "+" + SolrConstants.PI_TOPSTRUCT + ":" + page.getPi() + " +" + SolrConstants.ORDER + ":" + page.getOrder();
        try {
            User user = BeanUtils.getUserFromRequest(request);
            if (user == null) {
                UserBean userBean = BeanUtils.getUserBean();
                if (userBean != null) {
                    user = userBean.getUser();
                }
            }
            return checkAccessPermission(DataManager.getInstance().getDao().getRecordLicenseTypes(), page.getAccessConditions(),
                    privilegeName, user, NetTools.getIpAddress(request), ClientApplicationManager.getClientFromRequest(request), query);
        } catch (PresentationException e) {
            logger.debug(e.getMessage());
        }

        return AccessPermission.denied();

    }

    /**
     * Checks whether the current users has the given access permissions to the element with the given identifier and LOGID.
     *
     * @param identifier The PI to check.
     * @param logId The LOGID to check (optional).
     * @param privilegeName Particular privilege for which to check the permission.
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws RecordNotFoundException
     */
    public static AccessPermission checkAccessPermissionByIdentifierAndLogId(String identifier, String logId, String privilegeName,
            HttpServletRequest request)
            throws IndexUnreachableException, DAOException, RecordNotFoundException {
        // logger.trace("checkAccessPermissionByIdentifierAndLogId({}, {}, {})", identifier, logId, privilegeName);
        if (StringUtils.isEmpty(identifier)) {
            return AccessPermission.denied();
        }

        String query;
        if (StringUtils.isNotEmpty(logId)) {
            // Sub-docstruct
            query = new StringBuilder("+").append(SolrConstants.PI_TOPSTRUCT)
                    .append(':')
                    .append(identifier)
                    .append(" +")
                    .append(SolrConstants.LOGID)
                    .append(':')
                    .append(logId)
                    .append(" +")
                    .append(SolrConstants.DOCTYPE)
                    .append(':')
                    .append(DocType.DOCSTRCT.name())
                    .toString();
        } else {
            // Top document
            query = new StringBuilder("+").append(SolrConstants.PI).append(':').append(identifier).toString();
        }

        try {
            SolrDocumentList results = DataManager.getInstance()
                    .getSearchIndex()
                    .search(query, 1, null, Collections.singletonList(SolrConstants.ACCESSCONDITION));
            if (results == null || results.isEmpty()) {
                throw new RecordNotFoundException(identifier);
            }

            return checkAccessPermissionBySolrDoc(results.get(0), query, privilegeName, request);
        } catch (PresentationException e) {
            logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
            return AccessPermission.denied();
        }
    }

    /**
     *
     * @param doc
     * @param originalQuery
     * @param privilegeName
     * @param request
     * @return {@link AccessPermission}
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public static AccessPermission checkAccessPermissionBySolrDoc(SolrDocument doc, String originalQuery, String privilegeName,
            HttpServletRequest request) throws IndexUnreachableException, DAOException {
        // logger.trace("checkAccessPermissionByIdentifierAndLogId({}, {}, {})", identifier, logId, privilegeName);
        if (doc == null) {
            return AccessPermission.denied();
        }

        try {
            Set<String> requiredAccessConditions = new HashSet<>();

            Collection<Object> fieldsAccessConddition = doc.getFieldValues(SolrConstants.ACCESSCONDITION);
            if (fieldsAccessConddition != null) {
                for (Object accessCondition : fieldsAccessConddition) {
                    requiredAccessConditions.add((String) accessCondition);
                    // logger.trace("{}", accessCondition.toString());
                }
            }

            User user = BeanUtils.getUserFromRequest(request);
            if (user == null) {
                UserBean userBean = BeanUtils.getUserBean();
                if (userBean != null) {
                    try {
                        user = userBean.getUser();
                    } catch (ContextNotActiveException e) {
                        logger.trace("Cannot access bean method from different thread: UserBean.getUser()");
                    }
                }
            }
            return checkAccessPermission(DataManager.getInstance().getDao().getRecordLicenseTypes(), requiredAccessConditions,
                    privilegeName, user, NetTools.getIpAddress(request), ClientApplicationManager.getClientFromRequest(request), originalQuery);
        } catch (PresentationException e) {
            logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
        }

        return AccessPermission.denied();
    }

    /**
     * Checks whether the current users has the given access permissions each element of the record with the given identifier.
     *
     * @param identifier a {@link java.lang.String} object.
     * @param privilegeName a {@link java.lang.String} object.
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @return Map with true/false for each LOGID
     * @should fill map completely
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, AccessPermission> checkAccessPermissionByIdentiferForAllLogids(String identifier, String privilegeName,
            HttpServletRequest request) throws IndexUnreachableException, DAOException {
        logger.trace("checkAccessPermissionByIdentiferForAllLogids({}, {})", identifier, privilegeName);

        String attributeName = IPrivilegeHolder.PREFIX_PRIV + privilegeName + "_" + identifier;
        Map<String, AccessPermission> ret = new HashMap<>();
        if (request != null && request.getSession() != null) {
            try {
                ret = (Map<String, AccessPermission>) request.getSession().getAttribute(attributeName);
                if (ret != null) {
                    return ret;
                }
                ret = new HashMap<>();
            } catch (ClassCastException e) {
                logger.error("Cannot cast session attribute '" + attributeName + "' to Map", e);
            }
        }

        if (StringUtils.isNotEmpty(identifier)) {
            String query = new StringBuilder().append('+')
                    .append(SolrConstants.PI_TOPSTRUCT)
                    .append(':')
                    .append(identifier)
                    .append(" +")
                    .append(SolrConstants.DOCTYPE)
                    .append(':')
                    .append(DocType.DOCSTRCT.name())
                    .toString();
            try {
                logger.trace(query);
                SolrDocumentList results = DataManager.getInstance()
                        .getSearchIndex()
                        .search(query, SolrSearchIndex.MAX_HITS, null,
                                Arrays.asList(SolrConstants.LOGID, SolrConstants.ACCESSCONDITION));
                if (results != null) {
                    User user = BeanUtils.getUserFromRequest(request);
                    if (user == null) {
                        UserBean userBean = BeanUtils.getUserBean();
                        if (userBean != null) {
                            user = userBean.getUser();
                        }
                    }

                    //                    long start = System.nanoTime();
                    List<LicenseType> nonOpenAccessLicenseTypes = DataManager.getInstance().getDao().getRecordLicenseTypes();
                    for (SolrDocument doc : results) {
                        Set<String> requiredAccessConditions = new HashSet<>();
                        Collection<Object> fieldsAccessConddition = doc.getFieldValues(SolrConstants.ACCESSCONDITION);
                        if (fieldsAccessConddition != null) {
                            for (Object accessCondition : fieldsAccessConddition) {
                                requiredAccessConditions.add((String) accessCondition);
                                // logger.trace("{}", accessCondition.toString());
                            }
                        }

                        String logid = (String) doc.getFieldValue(SolrConstants.LOGID);
                        if (logid != null) {
                            ret.put(logid, checkAccessPermission(nonOpenAccessLicenseTypes, requiredAccessConditions, privilegeName, user,
                                    NetTools.getIpAddress(request), ClientApplicationManager.getClientFromRequest(request), query));
                        }
                    }
                    //                    long end = System.nanoTime();
                }

            } catch (PresentationException e) {
                logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
            }
        }

        if (request != null && request.getSession() != null) {
            request.getSession().setAttribute(attributeName, ret);
        }

        logger.trace("Found access permisstions for {} elements.", ret.size());
        return ret;
    }

    /**
     * Checks if the record with the given identifier should allow access to the given request
     *
     * @param identifier The PI of the work to check
     * @param request The HttpRequest which may provide a {@link javax.servlet.http.HttpSession} to store the access map
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static AccessPermission checkContentFileAccessPermission(String identifier, HttpServletRequest request)
            throws IndexUnreachableException, DAOException {

        AccessPermission ret = null;
        String attributeName = IPrivilegeHolder.PREFIX_PRIV + IPrivilegeHolder.PRIV_DOWNLOAD_ORIGINAL_CONTENT;
        if (request != null && request.getSession() != null) {
            try {
                ret = (AccessPermission) request.getSession().getAttribute(attributeName);
                if (ret != null) {
                    // Permission already saved in session
                    return ret;
                }
            } catch (ClassCastException e) {
                logger.error("Cannot cast session attribute '" + attributeName + "' to Map", e);
            }
        }

        // logger.trace("checkContentFileAccessPermission({})", identifier);
        if (StringUtils.isNotEmpty(identifier)) {
            try {
                Set<String> requiredAccessConditions = new HashSet<>();
                String query = "+" + SolrConstants.PI + ":" + identifier;
                SolrDocumentList results = DataManager.getInstance()
                        .getSearchIndex()
                        .search(query, 1, null, Arrays.asList(new String[] { SolrConstants.ACCESSCONDITION }));
                if (results != null) {
                    for (SolrDocument doc : results) {
                        Collection<Object> fieldsAccessConddition = doc.getFieldValues(SolrConstants.ACCESSCONDITION);
                        if (fieldsAccessConddition != null) {
                            for (Object accessCondition : fieldsAccessConddition) {
                                requiredAccessConditions.add((String) accessCondition);
                                // logger.debug(accessCondition.toString());
                            }
                        }
                    }
                }

                ret = checkAccessPermission(requiredAccessConditions, IPrivilegeHolder.PRIV_DOWNLOAD_ORIGINAL_CONTENT, query, request);

            } catch (PresentationException e) {
                logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
            }
        }
        if (request != null && request.getSession() != null) {
            request.getSession().setAttribute(attributeName, ret);
        }
        //return only the access status for the relevant files
        return ret;
    }

    /**
     * Checks whether the client may access an image (by image URN).
     *
     * @param imageUrn Image URN.
     * @param request Calling HttpServiceRequest.
     * @return {@link AccessPermission}
     * @param privilegeName a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static AccessPermission checkAccessPermissionByImageUrn(String imageUrn, String privilegeName, HttpServletRequest request)
            throws IndexUnreachableException, DAOException {
        logger.trace("checkAccessPermissionByImageUrn({}, {}, {}, {})", imageUrn, privilegeName, request.getAttributeNames());
        if (StringUtils.isEmpty(imageUrn)) {
            return AccessPermission.denied();
        }

        String query = new StringBuilder()
                .append('+')
                .append(SolrConstants.IMAGEURN)
                .append(':')
                .append(imageUrn.replace(":", "\\:"))
                .toString();
        try {
            Set<String> requiredAccessConditions = new HashSet<>();
            SolrDocumentList hits = DataManager.getInstance()
                    .getSearchIndex()
                    .search(query, 1, null, Arrays.asList(SolrConstants.ACCESSCONDITION, SolrConstants.PI_TOPSTRUCT));
            for (SolrDocument doc : hits) {
                Collection<Object> fieldsAccessConddition = doc.getFieldValues(SolrConstants.ACCESSCONDITION);
                if (fieldsAccessConddition != null) {
                    for (Object accessCondition : fieldsAccessConddition) {
                        requiredAccessConditions.add((String) accessCondition);
                        // logger.debug((String) accessCondition);
                    }
                }
            }

            User user = BeanUtils.getUserFromRequest(request);
            if (user == null) {
                UserBean userBean = BeanUtils.getUserBean();
                if (userBean != null) {
                    user = userBean.getUser();
                }
            }
            return checkAccessPermission(DataManager.getInstance().getDao().getRecordLicenseTypes(), requiredAccessConditions,
                    privilegeName, user, NetTools.getIpAddress(request), ClientApplicationManager.getClientFromRequest(request), query);
        } catch (PresentationException e) {
            logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
            return AccessPermission.denied();
        }
    }

    /**
     * <p>
     * checkAccessPermission.
     * </p>
     *
     * @param requiredAccessConditions a {@link java.util.Set} object.
     * @param privilegeName a {@link java.lang.String} object.
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param query a {@link java.lang.String} object.
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static AccessPermission checkAccessPermission(Set<String> requiredAccessConditions, String privilegeName, String query,
            HttpServletRequest request) throws IndexUnreachableException, PresentationException, DAOException {
        User user = BeanUtils.getUserFromRequest(request);
        if (user == null) {
            UserBean userBean = BeanUtils.getUserBean();
            if (userBean != null) {
                user = userBean.getUser();
            }
        }
        return checkAccessPermission(DataManager.getInstance().getDao().getRecordLicenseTypes(), requiredAccessConditions, privilegeName, user,
                NetTools.getIpAddress(request), ClientApplicationManager.getClientFromRequest(request), query);
    }

    /**
     * Checks access permission for the given image and puts the permission status into the corresponding session map.
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param pi a {@link java.lang.String} object.
     * @param contentFileName a {@link java.lang.String} object.
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static AccessPermission checkAccessPermissionForImage(HttpServletRequest request, String pi, String contentFileName)
            throws IndexUnreachableException, DAOException {
        // logger.trace("checkAccessPermissionForImage: {}/{}", pi, contentFileName);
        return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName, IPrivilegeHolder.PRIV_VIEW_IMAGES);
    }

    /**
     * Checks access permission for the given thumbnail and puts the permission status into the corresponding session map.
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param pi a {@link java.lang.String} object.
     * @param contentFileName a {@link java.lang.String} object.
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static AccessPermission checkAccessPermissionForThumbnail(HttpServletRequest request, String pi, String contentFileName)
            throws IndexUnreachableException, DAOException {
        return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName, IPrivilegeHolder.PRIV_VIEW_THUMBNAILS);
    }

    /**
     * Checks access permission for the given image and puts the permission status into the corresponding session map.
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static AccessPermission checkAccessPermissionForPagePdf(HttpServletRequest request, PhysicalElement page)
            throws IndexUnreachableException, DAOException {
        if (page == null) {
            throw new IllegalArgumentException("page may not be null");
        }
        // logger.trace("checkAccessPermissionForPagePdf: {}/{}", page.getPi(), page.getOrder());
        return checkAccessPermissionByIdentifierAndPageOrder(page, IPrivilegeHolder.PRIV_DOWNLOAD_PAGE_PDF, request);
    }

    /**
     * <p>
     * checkAccessPermissionByIdentifierAndFilePathWithSessionMap.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param filePath FILENAME_ALTO or FILENAME_FULLTEXT value
     * @param privilegeType a {@link java.lang.String} object.
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static AccessPermission checkAccessPermissionByIdentifierAndFilePathWithSessionMap(HttpServletRequest request, String filePath,
            String privilegeType) throws IndexUnreachableException, DAOException {
        if (filePath == null) {
            throw new IllegalArgumentException("filePath may not be null");
        }
        String[] filePathSplit = filePath.split("/");
        if (filePathSplit.length != 3) {
            throw new IllegalArgumentException("Illegal filePath value: " + filePath);
        }

        return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, filePathSplit[1], filePathSplit[2], privilegeType);
    }

    /**
     * Checks access permission of the given privilege type for the given image and puts the permission status into the corresponding session map.
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param pi a {@link java.lang.String} object.
     * @param contentFileName a {@link java.lang.String} object.
     * @param privilegeType a {@link java.lang.String} object.
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @SuppressWarnings("unchecked")
    public static AccessPermission checkAccessPermissionByIdentifierAndFileNameWithSessionMap(HttpServletRequest request, String pi,
            String contentFileName, String privilegeType) throws IndexUnreachableException, DAOException {
        // logger.trace("checkAccessPermissionByIdentifierAndFileNameWithSessionMap: {}, {}, {}", pi, contentFileName, privilegeType); // Sonar considers this log msg a security issue, so leave it commented out when not needed
        if (privilegeType == null) {
            throw new IllegalArgumentException("privilegeType may not be null");
        }
        // logger.debug("sesaccesssion id: " + request.getSession().getId());
        // Session persistent permission check: Servlet-local method.
        String attributeName = IPrivilegeHolder.PREFIX_PRIV + privilegeType;
        // logger.trace("Checking session attribute: {}", attributeName);
        Map<String, AccessPermission> permissions = null;
        if (request != null) {
            permissions = (Map<String, AccessPermission>) request.getSession().getAttribute(attributeName);
        }
        if (permissions == null) {
            permissions = new HashMap<>();
            // logger.trace("Session attribute not found, creating new");
        }
        // logger.debug("Permissions found, " + permissions.size() + " items.");
        // new pi -> create an new empty map in the session
        if (request != null && !pi.equals(request.getSession().getAttribute("currentPi"))) {
            request.getSession().setAttribute("currentPi", pi);
            request.getSession().removeAttribute(attributeName);
            permissions = new HashMap<>();
            // logger.trace("PI has changed, permissions map reset.");
        }
        String key = new StringBuilder(pi).append('_').append(contentFileName).toString();
        // pi already checked -> look in the session
        // logger.debug("permissions key: {}: {}", key, permissions.get(key)); // Sonar considers this log msg a security issue, so leave it commented out when not needed

        AccessPermission ret = AccessPermission.denied();
        if (permissions.containsKey(key) && permissions.get(key) != null) {
            ret = permissions.get(key);
            //            logger.trace("Access ({}) previously checked and is {} for '{}/{}' (Session ID {})", privilegeType, ret.isGranted(), pi, contentFileName,
            //                    request.getSession().getId());
        } else {
            // TODO check for all images and save to map
            Map<String, AccessPermission> accessMap = checkAccessPermissionByIdentifierAndFileName(pi, contentFileName, privilegeType, request);
            for (Entry<String, AccessPermission> entry : accessMap.entrySet()) {
                String newKey = new StringBuilder(pi).append('_').append(entry.getKey()).toString();
                AccessPermission pageAccess = entry.getValue();
                permissions.put(newKey, pageAccess);
            }
            ret = permissions.get(key) != null ? permissions.get(key) : AccessPermission.denied();
            // logger.debug("Access ({}) not yet checked for '{}/{}', access is {}", privilegeType, pi, contentFileName, ret.isGranted()); // Sonar considers this log msg a security issue, so leave it commented out when not needed
            if (request != null) {
                request.getSession().setAttribute(attributeName, permissions);
            }
        }

        return ret;
    }

    /**
     * <p>
     * checkAccessPermission.
     * </p>
     *
     * @param allLicenseTypes a {@link java.util.List} object.
     * @param requiredAccessConditions Set of access condition names to satisfy (one suffices).
     * @param privilegeName The particular privilege to check.
     * @param user Logged in user.
     * @param query Solr query describing the resource in question.
     * @param remoteAddress a {@link java.lang.String} object.
     * @param client
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should return true if required access conditions empty
     * @should return true if required access conditions contain only open access
     * @should return true if all license types allow privilege by default
     * @should return false if not all license types allow privilege by default
     * @should return true if ip range allows access
     * @should not return true if no ip range matches
     * @should preserve ticket requirement
     */
    public static AccessPermission checkAccessPermission(List<LicenseType> allLicenseTypes, Set<String> requiredAccessConditions,
            String privilegeName, User user, String remoteAddress, Optional<ClientApplication> client, String query)
            throws IndexUnreachableException, PresentationException, DAOException {
        Map<String, AccessPermission> result =
                checkAccessPermissions(allLicenseTypes, requiredAccessConditions, privilegeName, user, remoteAddress, client, query);
        boolean ticketRequired = false;
        boolean redirect = false;
        String redirectUrl = null;
        for (Entry<String, AccessPermission> entry : result.entrySet()) {
            // Preserve ticket requirement
            if (entry.getValue().isTicketRequired()) {
                ticketRequired = true;
            }
            if (entry.getValue().isRedirect()) {
                redirect = true;
            }
            if (StringUtils.isNotEmpty(entry.getValue().getRedirectUrl())) {
                redirectUrl = entry.getValue().getRedirectUrl();
            }
            if (!entry.getValue().isGranted()) {
                return AccessPermission.denied();
            }
        }

        return AccessPermission.granted().setTicketRequired(ticketRequired).setRedirect(redirect).setRedirectUrl(redirectUrl);
    }

    /**
     * <p>
     * Base method for checking access permissions of various types.
     * </p>
     *
     * @param allLicenseTypes a {@link java.util.List} object.
     * @param requiredAccessConditions Set of access condition names to satisfy (one suffices).
     * @param privilegeName The particular privilege to check.
     * @param user Logged in user.
     * @param query Solr query describing the resource in question.
     * @param remoteAddress a {@link java.lang.String} object.
     * @param client
     * @param files a {@link java.util.List} object.
     * @return a {@link java.util.Map} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should return true if required access conditions empty
     * @should return true if required access conditions contain only open access
     * @should return true if all license types allow privilege by default
     * @should return false if not all license types allow privilege by default
     * @should return true if ip range allows access
     * @should not return true if no ip range matches
     * @should capture redirect metadata if on any license type
     */
    static Map<String, AccessPermission> checkAccessPermissions(List<LicenseType> allLicenseTypes, Set<String> requiredAccessConditions,
            String privilegeName, User user, String remoteAddress, Optional<ClientApplication> client, String query)
            throws IndexUnreachableException, PresentationException, DAOException {
        // logger.trace("checkAccessPermission({},{})", requiredAccessConditions, privilegeName);

        Map<String, AccessPermission> accessMap = new HashMap<>();
        accessMap.put("", AccessPermission.denied());

        // If user is superuser, allow immediately
        if (user != null && user.isSuperuser()) {
            accessMap.keySet().forEach(key -> accessMap.put(key, AccessPermission.granted()));
            return accessMap;
        }
        // If no access condition given, allow immediately (though this should never be the case)
        if (requiredAccessConditions.isEmpty()) {
            logger.trace("No required access conditions given, access granted.");
            accessMap.keySet().forEach(key -> accessMap.put(key, AccessPermission.granted()));
            return accessMap;
        }
        // If OPENACCESS is the only condition, allow immediately
        if (isFreeOpenAccess(requiredAccessConditions, allLicenseTypes)) {
            accessMap.keySet().forEach(key -> accessMap.put(key, AccessPermission.granted()));
            return accessMap;
        }
        // If no license types are configured or no privilege name is given, deny immediately
        if (allLicenseTypes == null || !StringUtils.isNotEmpty(privilegeName)) {
            logger.trace("No license types or no privilege name given.");
            accessMap.keySet().forEach(key -> accessMap.put(key, AccessPermission.denied()));
            return accessMap;
        }

        Map<String, List<LicenseType>> licenseMap = getRelevantLicenseTypesOnly(allLicenseTypes, requiredAccessConditions, query, accessMap);
        // If no relevant license types found (configured), deny all
        if (licenseMap.isEmpty()) {
            logger.trace("No relevant license types found.");
            accessMap.keySet().forEach(key -> accessMap.put(key, AccessPermission.denied()));
            return accessMap;
        }

        for (Entry<String, List<LicenseType>> entry : licenseMap.entrySet()) {
            List<LicenseType> relevantLicenseTypes = entry.getValue();
            requiredAccessConditions = new HashSet<>(relevantLicenseTypes.size());
            if (relevantLicenseTypes.isEmpty()) {
                // No relevant license types for this file, set to false and continue
                logger.trace("No relevant license types.");
                accessMap.put(entry.getKey(), AccessPermission.denied());
                continue;
            }

            // If all relevant license types allow the requested privilege by default, allow access
            boolean licenseTypeAllowsPriv = true;
            boolean redirect = false;
            String redirectUrl = null;
            // Check whether *all* relevant license types allow the requested privilege by default. As soon as one doesn't, set to false.
            for (LicenseType licenseType : relevantLicenseTypes) {
                requiredAccessConditions.add(licenseType.getName());
                if (licenseType.isRedirect()) {
                    redirect = true;
                    redirectUrl = licenseType.getRedirectUrl();
                }
                if (!licenseType.getPrivileges().contains(privilegeName) && !licenseType.isOpenAccess()
                        && !licenseType.isRestrictionsExpired(query)) {
                    // logger.trace("LicenseType '{}' does not allow the action '{}' by default.", licenseType.getName(), privilegeName);
                    licenseTypeAllowsPriv = false;
                }
            }
            if (licenseTypeAllowsPriv) {
                // logger.trace("Privilege '{}' is allowed by default in all license types.", privilegeName);
                accessMap.put(entry.getKey(), AccessPermission.granted().setRedirect(redirect).setRedirectUrl(redirectUrl));
            } else if (isFreeOpenAccess(requiredAccessConditions, relevantLicenseTypes)) {
                logger.trace("Privilege '{}' is OpenAccess", privilegeName);
                accessMap.put(entry.getKey(), AccessPermission.granted().setRedirect(redirect).setRedirectUrl(redirectUrl));
            } else {
                // Check IP range
                if (StringUtils.isNotEmpty(remoteAddress)) {
                    if (NetTools.isIpAddressLocalhost(remoteAddress)
                            && DataManager.getInstance().getConfiguration().isFullAccessForLocalhost()) {
                        logger.trace("Access granted to localhost");
                        accessMap.put(entry.getKey(), AccessPermission.granted());
                        continue;
                    }
                    // Check whether the requested privilege is allowed to this IP range (for all access conditions)
                    for (IpRange ipRange : DataManager.getInstance().getDao().getAllIpRanges()) {
                        if (ipRange.matchIp(remoteAddress)) {
                            AccessPermission access =
                                    ipRange.canSatisfyAllAccessConditions(requiredAccessConditions, relevantLicenseTypes, privilegeName, null);
                            if (access.isGranted()) {
                                logger.trace("Access granted to {} via IP range {}", remoteAddress, ipRange.getName());
                                accessMap.put(entry.getKey(), access);
                            }
                        }
                    }
                }

                // If not within an allowed IP range, check the current user's satisfied access conditions
                if (user != null) {
                    AccessPermission access =
                            user.canSatisfyAllAccessConditions(requiredAccessConditions, privilegeName, null);
                    if (access.isGranted()) {
                        accessMap.put(entry.getKey(), access);
                    }
                }

                //check clientApplication
                if (client.map(c -> c.mayLogIn(remoteAddress)).orElse(false)) {
                    //check if specific client matches access conditions
                    boolean clientAccessGranted = false;
                    if (client.isPresent()) {
                        AccessPermission access = client.get().canSatisfyAllAccessConditions(requiredAccessConditions, privilegeName, null);
                        if (access.isGranted()) {
                            accessMap.put(entry.getKey(), access);
                            clientAccessGranted = true;
                        }
                    }
                    if (!clientAccessGranted) {
                        //check if accesscondition match for all clients
                        ClientApplication allClients = DataManager.getInstance().getClientManager().getAllClientsFromDatabase();
                        if (allClients != null) {
                            AccessPermission access =
                                    allClients.canSatisfyAllAccessConditions(requiredAccessConditions, privilegeName, null);
                            if (access.isGranted()) {
                                accessMap.put(entry.getKey(), access);
                            }
                        }
                    }
                }
            }
        }

        return accessMap;
    }

    /**
     * Check whether the requiredAccessConditions consist only of the {@link io.goobi.viewer.solr.SolrConstants#OPEN_ACCESS_VALUE OPENACCESS}
     * condition and OPENACCESS is not contained in allLicenseTypes. In this and only this case can we savely assume that everything is permitted. If
     * OPENACCESS is in the database then it likely contains some access restrictions which need to be checked
     *
     * @param requiredAccessConditions a {@link java.util.Set} object.
     * @param allLicenseTypes all license types relevant for access. If null, the DAO is checked if it contains the OPENACCESS condition
     * @return true if we can savely assume that we have entirely open access
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static boolean isFreeOpenAccess(Set<String> requiredAccessConditions, Collection<LicenseType> allLicenseTypes) throws DAOException {
        if (requiredAccessConditions.size() != 1) {
            return false;
        }

        boolean containsOpenAccess =
                requiredAccessConditions.stream().anyMatch(condition -> SolrConstants.OPEN_ACCESS_VALUE.equalsIgnoreCase(condition));
        boolean openAccessIsConfiguredLicenceType =
                allLicenseTypes == null ? DataManager.getInstance().getDao().getLicenseType(SolrConstants.OPEN_ACCESS_VALUE) != null
                        : allLicenseTypes.stream().anyMatch(license -> SolrConstants.OPEN_ACCESS_VALUE.equalsIgnoreCase(license.getName()));
        return containsOpenAccess && !openAccessIsConfiguredLicenceType;
    }

    /**
     * Filters the given list of license types my removing those that have Solr query conditions that do not match the given identifier.
     *
     * @param allLicenseTypes
     * @param requiredAccessConditions
     * @param query
     * @param accessMap
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @should remove license types whose names do not match access conditions
     * @should not remove moving wall license types to open access if condition query excludes given pi
     */
    static Map<String, List<LicenseType>> getRelevantLicenseTypesOnly(List<LicenseType> allLicenseTypes, Set<String> requiredAccessConditions,
            String query, Map<String, AccessPermission> accessMap) throws IndexUnreachableException, PresentationException {
        if (requiredAccessConditions == null || requiredAccessConditions.isEmpty()) {
            return accessMap.keySet().stream().collect(Collectors.toMap(Function.identity(), key -> Collections.emptyList()));
        }

        // logger.trace("getRelevantLicenseTypesOnly: {} | {}", query, requiredAccessConditions);
        Map<String, List<LicenseType>> ret = new HashMap<>(accessMap.size());
        for (LicenseType licenseType : allLicenseTypes) {
            // logger.trace("{}, moving wall: {}", licenseType.getName(), licenseType.isMovingWall());
            if (!requiredAccessConditions.contains(licenseType.getName())) {
                continue;
            }
            // Check whether the license type contains conditions that exclude the given record, in that case disregard this license type
            if (licenseType.isMovingWall() && StringUtils.isNotEmpty(query)) {
                StringBuilder sbQuery = new StringBuilder().append("+(")
                        .append(query)
                        .append(") +")
                        .append(licenseType.getFilterQueryPart().trim())
                        .append(" -(")
                        .append(SearchHelper.getMovingWallQuery())
                        .append(')');
                // logger.trace("License relevance query: {}",
                // StringTools.stripPatternBreakingChars(StringTools.stripPatternBreakingChars(sbQuery.toString())));
                if (DataManager.getInstance().getSearchIndex().getHitCount(sbQuery.toString()) == 0) {
                    // logger.trace("LicenseType '{}' does not apply to resource described by '{}' due to the moving wall condition.",
                    // licenseType.getName(), StringTools.stripPatternBreakingChars(query));
                    if (licenseType.isMovingWall()) {
                        // Moving wall license type allow everything if the condition query doesn't match
                        //                        logger.trace(
                        //                                "License type '{}' is a moving wall type and its condition query doesn't match the record query '{}'. All restrictions lifted.",
                        //                                licenseType.getName(), StringTools.stripPatternBreakingChars(query));
                        licenseType.getRestrictionsExpired().put(query, true);
                    } else {
                        continue;
                    }
                }
                // logger.trace("LicenseType '{}' applies to resource described by '{}' due to moving wall restrictions.", licenseType.getName(),
                // StringTools.stripPatternBreakingChars(query));
            }

            //no individual file conditions. Write same licenseTypes for all files
            for (String key : accessMap.keySet()) {
                List<LicenseType> types = ret.get(key);
                if (types == null) {
                    types = new ArrayList<>();
                    ret.put(key, types);
                }
                types.add(licenseType);
            }
        }

        return ret;
    }

    /**
     *
     * @param pi
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws RecordNotFoundException
     * @should throw RecordNotFoundException if record not found
     * @should return 100 if record has no quota value
     * @should return 100 if record open access
     * @should return 0 if no license configured
     * @should return actual quota value if found
     */
    public static int getPdfDownloadQuotaForRecord(String pi)
            throws PresentationException, IndexUnreachableException, DAOException, RecordNotFoundException {
        if (StringUtils.isEmpty(pi)) {
            return 0;
        }

        SolrDocument doc = DataManager.getInstance()
                .getSearchIndex()
                .getFirstDoc("+" + SolrConstants.PI + ":\"" + pi + '"',
                        Arrays.asList(SolrConstants.ACCESSCONDITION, SolrConstants.ACCESSCONDITION_PDF_PERCENTAGE_QUOTA));
        if (doc == null) {
            throw new RecordNotFoundException(pi + " not found in index");
        }
        if (!doc.containsKey(SolrConstants.ACCESSCONDITION_PDF_PERCENTAGE_QUOTA)) {
            logger.trace("Record '{}' has no field '{}'", pi, SolrConstants.ACCESSCONDITION_PDF_PERCENTAGE_QUOTA);
            return 100;
        }

        List<String> requiredAccessConditions = SolrTools.getMetadataValues(doc, SolrConstants.ACCESSCONDITION);
        // No relevant access condition values
        if (requiredAccessConditions == null || requiredAccessConditions.isEmpty()
                || requiredAccessConditions.get(0).equals(SolrConstants.OPEN_ACCESS_VALUE)) {
            logger.trace("Record '{}' is open access", pi);
            return 100;
        }

        List<LicenseType> relevantLicenseTypes = DataManager.getInstance().getDao().getLicenseTypes(requiredAccessConditions);
        // Deny access if record's license types aren't configured
        if (relevantLicenseTypes.isEmpty()) {
            logger.trace("Record '{}' hsd access conditions that aren't configured in the database", pi);
            return 0;
        }
        // Check whether this record has an access condition that implements a PDF quota
        for (LicenseType licenseType : relevantLicenseTypes) {
            if (licenseType.isPdfDownloadQuota()) {
                try {
                    return Integer.valueOf((String) doc.getFieldValue(SolrConstants.ACCESSCONDITION_PDF_PERCENTAGE_QUOTA));
                } catch (NumberFormatException e) {
                    logger.error(e.getMessage());
                    return 0;
                }
            }
        }

        return 100;
    }

    /**
     *
     * @param accessConditions
     * @return true if any license type for the given list of access conditions has concurrent views limit enabled; false otherwise
     * @throws DAOException
     * @should return false if access conditions null or empty
     * @should return true if any license type has limit enabled
     */
    public static boolean isConcurrentViewsLimitEnabledForAnyAccessCondition(List<String> accessConditions) throws DAOException {
        if (accessConditions == null || accessConditions.isEmpty()) {
            return false;
        }

        List<LicenseType> licenseTypes = DataManager.getInstance().getDao().getLicenseTypes(accessConditions);
        if (licenseTypes.isEmpty()) {
            return false;
        }

        for (LicenseType licenseType : licenseTypes) {
            if (licenseType.isConcurrentViewsLimit()) {
                return true;
            }
        }

        return false;
    }

    /**
     * List all licenses ("rights") that the given user and ipAddress is entitled to, either because they are directly given to the user, a group the
     * user belongs to or to the given ipAddress, whether or not the given user exists
     * 
     * @param user
     * @param ipAddress
     * @param type
     * @param dao
     * @return
     * @throws DAOException
     */
    public static List<License> getApplyingLicenses(Optional<User> user, String ipAddress, LicenseType type, IDAO dao) throws DAOException {
        List<License> licenses = dao.getLicenses(type);
        List<UserGroup> userGroups = user.map(User::getAllUserGroups).orElse(Collections.emptyList());
        List<IpRange> ipRangesApplyingToGivenIp =
                dao.getAllIpRanges().stream().filter(range -> range.matchIp(ipAddress)).collect(Collectors.toList());

        List<License> applyingLicenses = licenses.stream()
                .filter(license -> {
                    return user.map(u -> u.equals(license.getUser())).orElse(false)
                            || userGroups.contains(license.getUserGroup())
                            || ipRangesApplyingToGivenIp.stream().anyMatch(r -> r.getSubnetMask().equals(license.getIpRange().getSubnetMask()));
                })
                .collect(Collectors.toList());

        return applyingLicenses.stream()
                .filter(l -> {
                    return applyingLicenses.stream()
                            .filter(ol -> !ol.equals(l))
                            .noneMatch(ol -> l.getLicenseType().getOverriddenLicenseTypes().contains(ol.getLicenseType()));
                })
                .collect(Collectors.toList());
    }

    /**
     * 
     * @param pi Record identifier
     * @param session {@link HttpSession} that contains permission attributes
     * @return true if given <code>session</code> contains permission for <code>pi</code>; false otherwise
     */
    public static boolean isHasDownloadTicket(String pi, HttpSession session) {
        if (pi == null || session == null) {
            return false;
        }

        String attributeName = IPrivilegeHolder.PREFIX_TICKET + pi;
        Boolean hasTicket = (Boolean) session.getAttribute(attributeName);

        return hasTicket != null && hasTicket;
    }

    public static boolean addPermissionToSession(String pi, HttpSession session) {
        if (pi == null || session == null) {
            return false;
        }

        String attributeName = IPrivilegeHolder.PREFIX_TICKET + pi;
        session.setAttribute(attributeName, true);
        return true;
    }
}
