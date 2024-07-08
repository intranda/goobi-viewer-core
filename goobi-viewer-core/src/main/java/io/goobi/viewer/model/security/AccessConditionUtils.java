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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

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
public final class AccessConditionUtils {

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
                    //                                logger.trace("Checked thumbnail access: {}/{}: {}", pi, contentFileName, access); //NOSONAR Logging sometimes needed for debugging
                }
                return checkAccessPermissionForImage(request, pi, contentFileName);
            //                                logger.trace("Checked image access: {}/{}: {}", pi, contentFileName, access); //NOSONAR Logging sometimes needed for debugging
            case "text":
            case "ocrdump":
                return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName, IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
            case "pdf":
            case "epub":
                return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName, IPrivilegeHolder.PRIV_DOWNLOAD_PDF);
            case "video":
                return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName, IPrivilegeHolder.PRIV_VIEW_VIDEO);
            case "audio":
                return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName, IPrivilegeHolder.PRIV_VIEW_AUDIO);
            case "dimensions":
            case "version":
                // TODO is priv checking needed here?
                return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName, IPrivilegeHolder.PRIV_VIEW_IMAGES);
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
        //if fileName is an absolute http(s) url, assume that the filename is exactly the entire url
        if (fileName.matches("https?:\\/\\/.*")) {
            sbQuery.append(" +").append(useFileField).append(":\"").append(fileName).append('"');
            return sbQuery.toString();
        }
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
            case "tif", "tiff":
                sbQuery.append(" +(")
                        .append(useFileField)
                        .append(":\"")
                        .append(simpleFileName)
                        .append("\" ")
                        .append(SolrConstants.FILENAME)
                        .append("_TIFF:\"")
                        .append(simpleFileName)
                        .append("\")");
                break;
            case "jpg", "jpeg":
                sbQuery.append(" +(")
                        .append(useFileField)
                        .append(":\"")
                        .append(simpleFileName)
                        .append("\" ")
                        .append(SolrConstants.FILENAME)
                        .append("_JPEG:\"")
                        .append(simpleFileName)
                        .append("\")");
                break;
            case "png":
            case "jp2":
            case "obj":
            case "gltf":
            case "glb":
            case "pdf":
            case "epub":
                sbQuery.append(" +").append(useFileField).append(":\"").append(simpleFileName).append('"');
                break;
            default:
                // Escape whitespaces etc. for wildcard searches
                sbQuery.append(" +").append(useFileField).append(':').append(ClientUtils.escapeQueryChars(simpleFileName)).append(".*");
                break;
        }

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
        // logger.trace("checkAccessPermissionByIdentifierAndFileName({}, {}, {})", identifier, fileName, privilegeName); //NOSONAR Debugging
        if (StringUtils.isEmpty(identifier)) {
            return Collections.emptyMap();
        }

        String query = generateAccessCheckQuery(identifier, fileName);
        // logger.trace("query: {}", query); //NOSONAR Sonar considers this log msg a security issue, so leave it commented out when not needed
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
                            // logger.trace(accessCondition.toString()); //NOSONAR Logging sometimes needed for debugging
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
            HttpServletRequest request) throws IndexUnreachableException, DAOException, RecordNotFoundException {
        // logger.trace("checkAccessPermissionByIdentifierAndLogId({}, {}, {})", identifier, logId, privilegeName); //NOSONAR Debugging
        if (StringUtils.isEmpty(identifier)) {
            return AccessPermission.denied();
        }

        String attributeName = IPrivilegeHolder.PREFIX_PRIV + privilegeName + "_" + identifier + "_" + logId;
        AccessPermission ret = (AccessPermission) getSessionPermission(attributeName, request);
        if (ret != null) {
            // logger.trace("Permission '{}' already in session: {}", attributeName, ret.isGranted()); //NOSONAR Logging sometimes needed for debugging
            return ret;
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
                    .append(":(")
                    .append(DocType.DOCSTRCT.name())
                    .append(' ')
                    .append(DocType.ARCHIVE)
                    .append(')')
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

            ret = checkAccessPermissionBySolrDoc(results.get(0), query, privilegeName, request);

            // Add permission check outcome to user session
            addSessionPermission(attributeName, ret, request);

            return ret;
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
        // logger.trace("checkAccessPermissionBySolrDoc({}, {}, {})", identifier, logId, privilegeName); //NOSONAR Debugging
        if (doc == null) {
            return AccessPermission.denied();
        }

        try {
            Set<String> requiredAccessConditions = new HashSet<>();

            Collection<Object> fieldsAccessConddition = doc.getFieldValues(SolrConstants.ACCESSCONDITION);
            if (fieldsAccessConddition != null) {
                for (Object accessCondition : fieldsAccessConddition) {
                    requiredAccessConditions.add((String) accessCondition);
                    // logger.trace("{}", accessCondition.toString()); //NOSONAR Debugging
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
        Map<String, AccessPermission> ret = (Map<String, AccessPermission>) getSessionPermission(attributeName, request);
        if (ret != null) {
            return ret;
        }

        ret = new HashMap<>();
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
                                // logger.trace("{}", accessCondition.toString()); //NOSONAR Debugging
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

        // Add permission check outcome to user session
        addSessionPermission(attributeName, ret, request);

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
        // logger.trace("checkContentFileAccessPermission: {}", identifier); //NOSONAR Debugging
        String attributeName = IPrivilegeHolder.PREFIX_PRIV + IPrivilegeHolder.PRIV_DOWNLOAD_ORIGINAL_CONTENT + "_" + identifier;
        AccessPermission ret = (AccessPermission) getSessionPermission(attributeName, request);
        if (ret != null) {
            // logger.trace("Permission for '{}' already in session: {}", attributeName, ret.isGranted()); //NOSONAR Debugging
            return ret;
        }

        if (StringUtils.isNotEmpty(identifier)) {
            try {
                Set<String> requiredAccessConditions = new HashSet<>();
                String query = "+" + SolrConstants.PI + ":" + identifier;
                SolrDocumentList results = DataManager.getInstance()
                        .getSearchIndex()
                        .search(query, 1, null, Arrays.asList(SolrConstants.ACCESSCONDITION));
                if (results != null) {
                    for (SolrDocument doc : results) {
                        Collection<Object> fieldsAccessConddition = doc.getFieldValues(SolrConstants.ACCESSCONDITION);
                        if (fieldsAccessConddition != null) {
                            for (Object accessCondition : fieldsAccessConddition) {
                                requiredAccessConditions.add((String) accessCondition);
                                // logger.debug(accessCondition.toString()); //NOSONAR Debugging
                            }
                        }
                    }
                }

                ret = checkAccessPermission(requiredAccessConditions, IPrivilegeHolder.PRIV_DOWNLOAD_ORIGINAL_CONTENT, query, request);
                logger.trace("Permission found for '{}': {}", identifier, ret.isGranted());

            } catch (PresentationException e) {
                logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
            }
        }

        // Add permission check outcome to user session
        addSessionPermission(attributeName, ret, request);

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
        logger.trace("checkAccessPermissionByImageUrn({}, {}, {}, {})", imageUrn, privilegeName, request.getAttributeNames()); //NOSONAR Debugging
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
                        // logger.debug((String) accessCondition); //NOSONAR Debugging
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
        // logger.trace("checkAccessPermissionForImage: {}/{}", pi, contentFileName); //NOSONAR Debugging
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
        // logger.trace("checkAccessPermissionForPagePdf: {}/{}", page.getPi(), page.getOrder()); //NOSONAR Debugging
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
        // logger.trace("checkAccessPermissionByIdentifierAndFileNameWithSessionMap: {}, {}, {}", pi, contentFileName, privilegeType); //NOSONAR Debugging
        if (privilegeType == null) {
            throw new IllegalArgumentException("privilegeType may not be null");
        }
        // logger.debug("session id: " + request.getSession().getId()); //NOSONAR Debugging
        // Session persistent permission check: Servlet-local method.
        String attributeName = IPrivilegeHolder.PREFIX_PRIV + privilegeType + "_" + pi + "_" + contentFileName;
        // logger.trace("Checking session attribute: {}", attributeName); //NOSONAR Debugging
        Map<String, AccessPermission> permissions = (Map<String, AccessPermission>) getSessionPermission(attributeName, request);
        if (permissions == null) {
            permissions = new HashMap<>();
            // logger.trace("Session attribute not found, creating new"); //NOSONAR Debugging
        }
        // logger.debug("Permissions found, " + permissions.size() + " items."); //NOSONAR Debugging
        // new pi -> create an new empty map in the session
        if (request != null && !pi.equals(request.getSession().getAttribute("currentPi"))) {
            request.getSession().setAttribute("currentPi", pi);
            request.getSession().removeAttribute(attributeName);
            permissions = new HashMap<>();
            // logger.trace("PI has changed, permissions map reset."); //NOSONAR Debugging
        }
        String key = new StringBuilder(pi).append('_').append(contentFileName).toString();
        // pi already checked -> look in the session
        // logger.debug("permissions key: {}: {}", key, permissions.get(key)); //NOSONAR Debugging

        if (permissions.containsKey(key) && permissions.get(key) != null) {
            return permissions.get(key);
            // logger.trace("Access ({}) previously checked and is {} for '{}/{}' (Session ID {})", privilegeType,
            // ret.isGranted(), pi, contentFileName, request.getSession().getId()); //NOSONAR Debugging
        }
        // TODO check for all images and save to map
        Map<String, AccessPermission> accessMap = checkAccessPermissionByIdentifierAndFileName(pi, contentFileName, privilegeType, request);
        for (Entry<String, AccessPermission> entry : accessMap.entrySet()) {
            String newKey = new StringBuilder(pi).append('_').append(entry.getKey()).toString();
            AccessPermission pageAccess = entry.getValue();
            permissions.put(newKey, pageAccess);
        }

        // Add permission check outcome to user session
        addSessionPermission(attributeName, permissions, request);

        return permissions.get(key) != null ? permissions.get(key) : AccessPermission.denied();
        // logger.debug("Access ({}) not yet checked for '{}/{}', access is {}", privilegeType, pi, contentFileName, ret.isGranted()); //NOSONAR Deb
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
     * @param remoteAddress a {@link java.lang.String} object.
     * @param client
     * @param query Solr query describing the resource in question.
     * @return Map&lt;String, AccessPermission&gt;
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
    public static AccessPermission checkAccessPermission(List<LicenseType> allLicenseTypes, final Set<String> requiredAccessConditions,
            String privilegeName, User user, String remoteAddress, Optional<ClientApplication> client, String query)
            throws IndexUnreachableException, PresentationException, DAOException {
        // logger.trace("checkAccessPermission({},{})", requiredAccessConditions, privilegeName); //NOSONAR Debugging

        // If user is superuser, allow immediately
        if (user != null && user.isSuperuser()) {
            return AccessPermission.granted();
        }
        // If no access condition given, allow immediately (though this should never be the case)
        if (requiredAccessConditions.isEmpty()) {
            logger.trace("No required access conditions given, access granted.");
            return AccessPermission.granted();
        }
        // If OPENACCESS is the only condition, allow immediately
        if (isFreeOpenAccess(requiredAccessConditions, allLicenseTypes)) {
            return AccessPermission.granted();
        }
        // If no license types are configured or no privilege name is given, deny immediately
        if (allLicenseTypes == null || !StringUtils.isNotEmpty(privilegeName)) {
            logger.trace("No license types or no privilege name given.");
            return AccessPermission.denied();
        }

        List<LicenseType> relevantLicenseTypes = getRelevantLicenseTypesOnly(allLicenseTypes, requiredAccessConditions, query);
        // If no relevant license types found (configured), deny all
        if (relevantLicenseTypes.isEmpty()) {
            logger.trace("No relevant license types found.");
            return AccessPermission.denied();
        }

        Set<String> useAccessConditions = new HashSet<>(relevantLicenseTypes.size());

        // If all relevant license types allow the requested privilege by default, allow access
        boolean licenseTypeAllowsPriv = true;
        boolean redirect = false;
        String redirectUrl = null;
        // Check whether *all* relevant license types allow the requested privilege by default. As soon as one doesn't, set to false.
        for (LicenseType licenseType : relevantLicenseTypes) {
            useAccessConditions.add(licenseType.getName());
            if (licenseType.isRedirect()) {
                redirect = true;
                redirectUrl = licenseType.getRedirectUrl();
            }
            if (!licenseType.getPrivileges().contains(privilegeName) && !licenseType.isOpenAccess()
                    && !licenseType.isRestrictionsExpired(query)) {
                // logger.trace("LicenseType '{}' doesn't allow the action '{}' by default.", licenseType.getName(), privilegeName); //NOSONAR Debugging
                licenseTypeAllowsPriv = false;
            }
        }
        if (licenseTypeAllowsPriv) {
            // logger.trace("Privilege '{}' is allowed by default in all license types.", privilegeName); //NOSONAR Debugging
            return AccessPermission.granted().setRedirect(redirect).setRedirectUrl(redirectUrl);
        } else if (isFreeOpenAccess(useAccessConditions, relevantLicenseTypes)) {
            logger.trace("Privilege '{}' is OpenAccess", privilegeName);
            return AccessPermission.granted().setRedirect(redirect).setRedirectUrl(redirectUrl);
        } else {
            // Check IP range
            if (StringUtils.isNotEmpty(remoteAddress)) {
                if (NetTools.isIpAddressLocalhost(remoteAddress)
                        && DataManager.getInstance().getConfiguration().isFullAccessForLocalhost()) {
                    logger.trace("Access granted to localhost");
                    return AccessPermission.granted();
                }
                // Check whether the requested privilege is allowed to this IP range (for all access conditions)
                for (IpRange ipRange : DataManager.getInstance().getDao().getAllIpRanges()) {
                    if (ipRange.matchIp(remoteAddress)) {
                        AccessPermission access =
                                ipRange.canSatisfyAllAccessConditions(useAccessConditions, relevantLicenseTypes, privilegeName, null);
                        if (access.isGranted()) {
                            logger.trace("Access granted to {} via IP range {}", remoteAddress, ipRange.getName());
                            return access;
                        }
                    }
                }
            }

            // If not within an allowed IP range, check the current user's satisfied access conditions
            if (user != null) {
                AccessPermission access =
                        user.canSatisfyAllAccessConditions(useAccessConditions, privilegeName, null);
                if (access.isGranted()) {
                    return access;
                }
            }

            //check clientApplication
            if (client.map(c -> c.mayLogIn(remoteAddress)).orElse(false)) {
                //check if specific client matches access conditions
                if (client.isPresent()) {
                    AccessPermission access = client.get().canSatisfyAllAccessConditions(useAccessConditions, privilegeName, null);
                    if (access.isGranted()) {
                        return access;
                    }
                }
                //check if accesscondition match for all clients
                ClientApplication allClients = DataManager.getInstance().getClientManager().getAllClientsFromDatabase();
                if (allClients != null) {
                    AccessPermission access =
                            allClients.canSatisfyAllAccessConditions(useAccessConditions, privilegeName, null);
                    if (access.isGranted()) {
                        return access;
                    }
                }
            }
        }

        return AccessPermission.denied();
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
     * @return Map<String, List<LicenseType>>
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @should remove license types whose names do not match access conditions
     * @should not remove moving wall license types to open access if condition query excludes given pi
     */
    static List<LicenseType> getRelevantLicenseTypesOnly(List<LicenseType> allLicenseTypes, Set<String> requiredAccessConditions, String query)
            throws IndexUnreachableException, PresentationException {
        if (requiredAccessConditions == null || requiredAccessConditions.isEmpty()) {
            // return accessMap.keySet().stream().collect(Collectors.toMap(Function.identity(), key -> Collections.emptyList()));
            return Collections.emptyList();
        }

        // logger.trace("getRelevantLicenseTypesOnly: {} | {}", query, requiredAccessConditions); //NOSONAR Debug
        List<LicenseType> ret = new ArrayList<>();
        for (LicenseType licenseType : allLicenseTypes) {
            // logger.trace("{}, moving wall: {}", licenseType.getName(), licenseType.isMovingWall()); //NOSONAR Debug
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
                // logger.trace("License relevance query: {}", //NOSONAR Logging sometimes needed for debugging
                // StringTools.stripPatternBreakingChars(StringTools.stripPatternBreakingChars(sbQuery.toString()))); //NOSONAR Debug
                if (DataManager.getInstance().getSearchIndex().getHitCount(sbQuery.toString()) == 0) {
                    // logger.trace("LicenseType '{}' does not apply to resource described by '{}' due to the moving wall condition.", //NOSONAR Logging sometimes needed for debugging
                    // licenseType.getName(), StringTools.stripPatternBreakingChars(query)); //NOSONAR Debug
                    if (licenseType.isMovingWall()) {
                        // Moving wall license type allow everything if the condition query doesn't match
                        // logger.trace("License type '{}' is a moving wall type and its condition query doesn't match the record query '{}'. //NOSONAR Logging sometimes needed for debugging
                        // All restrictions lifted.", licenseType.getName(), StringTools.stripPatternBreakingChars(query)); //NOSONAR Debug
                        licenseType.getRestrictionsExpired().put(query, true);
                    } else {
                        continue;
                    }
                }
                // logger.trace("LicenseType '{}' applies to resource described by '{}' due to moving wall restrictions.", licenseType.getName(), //NOSONAR Logging sometimes needed for debugging
                // StringTools.stripPatternBreakingChars(query)); //NOSONAR Debug
            }

            ret.add(licenseType);
        }

        return ret;
    }

    /**
     *
     * @param pi
     * @return Number of allowed downloads for given pi; 100 of no value set
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
     * 
     * @param doc The document containing access condition metadata
     * @param privilegeName The privilege to check
     * @param request The request trying to access the resource
     * @return true if granted; false otherwise
     */
    public static boolean isPrivilegeGrantedForDoc(SolrDocument doc, String privilegeName, HttpServletRequest request) {
        // Check whether user may see full-text, before adding them to count
        String pi = SolrTools.getSingleFieldStringValue(doc, SolrConstants.PI_TOPSTRUCT);
        Collection<Object> accessConditions = doc.getFieldValues(SolrConstants.ACCESSCONDITION);
        if (!(accessConditions.size() == 1 && accessConditions.contains(SolrConstants.OPEN_ACCESS_VALUE))) {
            try {
                boolean ret = checkAccessPermissionByIdentifierAndLogId(pi, null, privilegeName, request).isGranted();
                logger.trace("{} access checked for {} and is: {}", privilegeName, pi, ret);
                return ret;
            } catch (IndexUnreachableException | DAOException | RecordNotFoundException e) {
                logger.error(e.getMessage());
            }
        }

        return true;
    }

    /**
     * List all licenses ("rights") that the given user and ipAddress is entitled to, either because they are directly given to the user, a group the
     * user belongs to or to the given ipAddress, whether or not the given user exists
     * 
     * @param user
     * @param ipAddress
     * @param type
     * @param dao
     * @return List<License>
     * @throws DAOException
     */
    public static List<License> getApplyingLicenses(Optional<User> user, String ipAddress, LicenseType type, IDAO dao) throws DAOException {
        List<License> licenses = dao.getLicenses(type);
        List<UserGroup> userGroups = user.map(User::getAllUserGroups).orElse(Collections.emptyList());
        List<IpRange> ipRangesApplyingToGivenIp =
                dao.getAllIpRanges().stream().filter(range -> range.matchIp(ipAddress)).toList();

        List<License> applyingLicenses = licenses.stream()
                .filter(license -> {
                    return user.map(u -> u.equals(license.getUser())).orElse(false)
                            || userGroups.contains(license.getUserGroup())
                            || ipRangesApplyingToGivenIp.stream().anyMatch(r -> r.getSubnetMask().equals(license.getIpRange().getSubnetMask()));
                })
                .toList();

        return applyingLicenses.stream()
                .filter(l -> {
                    return applyingLicenses.stream()
                            .filter(ol -> !ol.equals(l))
                            .noneMatch(ol -> l.getLicenseType().getOverriddenLicenseTypes().contains(ol.getLicenseType()));
                })
                .toList();
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

    public static boolean addDownloadTicketToSession(String pi, HttpSession session) {
        if (pi == null || session == null) {
            return false;
        }

        String attributeName = IPrivilegeHolder.PREFIX_TICKET + pi;
        session.setAttribute(attributeName, true);
        return true;
    }

    /**
     * 
     * @param attributeName
     * @param request
     * @return Object found in session; null otherwise
     */
    public static Object getSessionPermission(String attributeName, HttpServletRequest request) {
        if (request == null || request.getSession() == null) {
            return null;
        }

        return request.getSession().getAttribute(attributeName);
    }

    /**
     * 
     * @param attributeName
     * @param attributeValue
     * @param request
     * @return true if successful; false otherwise
     */
    public static boolean addSessionPermission(String attributeName, Object attributeValue, HttpServletRequest request) {
        // logger.trace("addSessionPermission: {}", attributeName); //NOSONAR Debugging
        if (request == null || request.getSession() == null) {
            return false;
        }

        request.getSession().setAttribute(attributeName, attributeValue);
        return true;
    }

    /**
     * Removes privileges saved in the user session.
     * 
     * @param session
     * @return Number of removed session attributes
     */
    public static int clearSessionPermissions(HttpSession session) {
        if (session == null) {
            return 0;
        }

        Enumeration<String> attributeNames = session.getAttributeNames();
        Set<String> attributesToRemove = new HashSet<>();
        while (attributeNames.hasMoreElements()) {
            String attribute = attributeNames.nextElement();
            if (attribute.startsWith(IPrivilegeHolder.PREFIX_PRIV)) {
                attributesToRemove.add(attribute);
            }
        }

        int ret = 0;
        if (!attributesToRemove.isEmpty()) {
            for (String attribute : attributesToRemove) {
                session.removeAttribute(attribute);
                ret++;
                logger.trace("Removed session attribute: {}", attribute);
            }
        }

        return ret;
    }
}
