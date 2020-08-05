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
package io.goobi.viewer.model.security;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrConstants.DocType;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.viewer.PhysicalElement;

/**
 * <p>
 * AccessConditionUtils class.
 * </p>
 */
public class AccessConditionUtils {

    private static final Logger logger = LoggerFactory.getLogger(AccessConditionUtils.class);

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
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static boolean checkAccess(HttpServletRequest request, String action, String pi, String contentFileName, boolean isThumbnail)
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
                switch (FilenameUtils.getExtension(contentFileName).toLowerCase()) {
                    // This check is needed so that the "application" action cannot be abused to download images w/o the proper permission
                    case "pdf":
                        return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName,
                                IPrivilegeHolder.PRIV_DOWNLOAD_PDF);
                    default:
                        if (isThumbnail) {
                            return checkAccessPermissionForThumbnail(request, pi, contentFileName);
                            //                                logger.trace("Checked thumbnail access: {}/{}: {}", pi, contentFileName, access);
                        }
                        return checkAccessPermissionForImage(request, pi, contentFileName);
                    //                                logger.trace("Checked image access: {}/{}: {}", pi, contentFileName, access);
                }
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

        return false;
    }

    /**
     * 
     * @param fileName
     * @param identifier
     * @return
     * @should use correct field name for AV files
     * @should use correct file name for text files
     * @should escape file name for wildcard search correctly
     */
    static String[] generateAccessCheckQuery(String identifier, String fileName) {
        if (fileName == null) {
            return new String[2];
        }

        String[] ret = new String[2];
        StringBuilder sbQuery = new StringBuilder();
        String useFileField = SolrConstants.FILENAME;
        String useFileName = Paths.get(fileName).getFileName().toString();
        boolean wildcard = false;
        // Different media types have the file name in different fields
        String extension = FilenameUtils.getExtension(fileName).toLowerCase();
        switch (extension) {
            case "webm":
            case "mp4":
            case "mp3":
            case "ogg":
            case "ogv":
                break;
            case "txt":
            case "xml":
                useFileName = useFileName.replace("." + extension, "");
                wildcard = true;
                break;
            case "":
                wildcard = true;
            default:
                break;
        }
        sbQuery.append('+').append(SolrConstants.PI_TOPSTRUCT).append(':').append(identifier).append(" +").append(useFileField).append(':');
        if (wildcard) {
            // Escape whitespaces etc. for wildcard searches
            useFileName = ClientUtils.escapeQueryChars(useFileName) + ".*";
            sbQuery.append(useFileName);
        } else {
            sbQuery.append('"').append(useFileName).append('"');
        }

        // logger.trace(sbQuery.toString());
        ret[0] = sbQuery.toString();
        ret[1] = useFileField;

        return ret;
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
    static Map<String, Boolean> checkAccessPermissionByIdentifierAndFileName(String identifier, String fileName, String privilegeName,
            HttpServletRequest request) throws IndexUnreachableException, DAOException {
        // logger.trace("checkAccessPermissionByIdentifierAndFileName({}, {}, {})", identifier, fileName, privilegeName);
        if (StringUtils.isEmpty(identifier)) {
            return Collections.emptyMap();
        }

        String[] query = generateAccessCheckQuery(identifier, fileName);
        // logger.trace("query: {}", query[0]);
        try {
            // Collect access conditions required by the page
            Map<String, Set<String>> requiredAccessConditions = new HashMap<>();
            SolrDocumentList results = DataManager.getInstance()
                    .getSearchIndex()
                    .search(query[0], "*".equals(fileName) ? SolrSearchIndex.MAX_HITS : 1, null,
                            Arrays.asList(new String[] { query[1], SolrConstants.ACCESSCONDITION }));
            if (results != null) {
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
            Map<String, Boolean> ret = new HashMap<>(requiredAccessConditions.size());
            for (String pageFileName : requiredAccessConditions.keySet()) {
                Set<String> pageAccessConditions = requiredAccessConditions.get(pageFileName);
                boolean access = checkAccessPermission(DataManager.getInstance().getDao().getRecordLicenseTypes(), pageAccessConditions,
                        privilegeName, user, NetTools.getIpAddress(request), query[0]);
                ret.put(pageFileName, access);
            }
            return ret;
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Checks whether the client may access an image (by PI + file name).
     *
     * @param request Calling HttpServiceRequest.
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @param privilegeName a {@link java.lang.String} object.
     * @return true if access is granted; false otherwise.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    static boolean checkAccessPermissionByIdentifierAndPageOrder(PhysicalElement page, String privilegeName, HttpServletRequest request)
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
            Map<String, Boolean> ret = new HashMap<>(page.getAccessConditions().size());
            boolean access = checkAccessPermission(DataManager.getInstance().getDao().getRecordLicenseTypes(), page.getAccessConditions(),
                    privilegeName, user, NetTools.getIpAddress(request), query);
            return access;
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
        }

        return false;

    }

    /**
     * Checks whether the current users has the given access permissions to the element with the given identifier and LOGID.
     *
     * @param identifier The PI to check.
     * @param logId The LOGID to check (optional).
     * @param privilegeName Particular privilege for which to check the permission.
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static boolean checkAccessPermissionByIdentifierAndLogId(String identifier, String logId, String privilegeName, HttpServletRequest request)
            throws IndexUnreachableException, DAOException {
        logger.trace("checkAccessPermissionByIdentifierAndLogId({}, {}, {})", identifier, logId, privilegeName);
        if (StringUtils.isNotEmpty(identifier)) {
            StringBuilder sbQuery = new StringBuilder("+");
            if (StringUtils.isNotEmpty(logId)) {
                // Sub-docstruct
                sbQuery.append(SolrConstants.PI_TOPSTRUCT)
                        .append(':')
                        .append(identifier)
                        .append(" +")
                        .append(SolrConstants.LOGID)
                        .append(':')
                        .append(logId);
            } else {
                // Top document
                sbQuery.append(SolrConstants.PI).append(':').append(identifier);
            }
            // Only query docstruct docs because metadata/event docs may not contain values defined in the license type filter query
            sbQuery.append(" +").append(SolrConstants.DOCTYPE).append(':').append(DocType.DOCSTRCT.name());
            try {
                Set<String> requiredAccessConditions = new HashSet<>();
                logger.trace(sbQuery.toString());
                SolrDocumentList results = DataManager.getInstance()
                        .getSearchIndex()
                        .search(sbQuery.toString(), 1, null, Arrays.asList(new String[] { SolrConstants.ACCESSCONDITION }));
                if (results != null) {
                    for (SolrDocument doc : results) {
                        Collection<Object> fieldsAccessConddition = doc.getFieldValues(SolrConstants.ACCESSCONDITION);
                        if (fieldsAccessConddition != null) {
                            for (Object accessCondition : fieldsAccessConddition) {
                                requiredAccessConditions.add((String) accessCondition);
                                logger.trace("{}", accessCondition.toString());
                            }
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
                        privilegeName, user, NetTools.getIpAddress(request), sbQuery.toString());
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
            }
        }

        return false;
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
    public static Map<String, Boolean> checkAccessPermissionByIdentiferForAllLogids(String identifier, String privilegeName,
            HttpServletRequest request) throws IndexUnreachableException, DAOException {
        logger.trace("checkAccessPermissionByIdentiferForAllLogids({}, {})", identifier, privilegeName);

        String attributeName = IPrivilegeHolder._PRIV_PREFIX + privilegeName + "_" + identifier;
        Map<String, Boolean> ret = new HashMap<>();
        if (request != null && request.getSession() != null) {
            try {
                ret = (Map<String, Boolean>) request.getSession().getAttribute(attributeName);
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
                                Arrays.asList(new String[] { SolrConstants.LOGID, SolrConstants.ACCESSCONDITION }));
                if (results != null) {
                    User user = BeanUtils.getUserFromRequest(request);
                    if (user == null) {
                        UserBean userBean = BeanUtils.getUserBean();
                        if (userBean != null) {
                            user = userBean.getUser();
                        }
                    }

                    long start = System.nanoTime();
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
                                    NetTools.getIpAddress(request), query));
                        }
                    }
                    long end = System.nanoTime();
                }

            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
            }
        }

        if (request != null && request.getSession() != null) {
            request.getSession().setAttribute(attributeName, ret);
        }

        logger.trace("Found access permisstions for {} elements.", ret.size());
        return ret;
    }

    /**
     * Checks access conditions for all files in the given files list. If possible the conditions are stored in the request's session as a session
     * attribute. Access is checked for general access, ip-range access, user access for both the given PI and the individual filenames if the
     * relevant {@link io.goobi.viewer.model.security.LicenseType}s define a 'FILENAME' condition
     *
     * @param identifier The PI of the work to check
     * @param request The HttpRequest which may provide a {@link javax.servlet.http.HttpSession} to store the access map
     * @param files The files to check access for. Must be non-empty
     * @return A map of filePaths and their corresponding access rights
     * @throws java.lang.SecurityException if no or an empty file list is given
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Boolean> checkContentFileAccessPermission(String identifier, HttpServletRequest request, List<Path> files)
            throws IndexUnreachableException, DAOException {

        if (files == null || files.isEmpty()) {
            throw new SecurityException("Must provide list of file to check access for");
        }

        String attributeName = IPrivilegeHolder._PRIV_PREFIX + IPrivilegeHolder.PRIV_DOWNLOAD_ORIGINAL_CONTENT;
        Map<String, Boolean> accessMap = new HashMap<>();
        if (request != null && request.getSession() != null) {
            try {
                accessMap = (Map<String, Boolean>) request.getSession().getAttribute(attributeName);
                if (accessMap != null && accessMap.keySet().containsAll(files.stream().map(Path::toString).collect(Collectors.toList()))) {
                    return accessMap;
                } else if (accessMap == null) {
                    accessMap = new HashMap<>();
                }
            } catch (ClassCastException e) {
                logger.error("Cannot cast session attribute '" + attributeName + "' to Map", e);
            }
        }

        // logger.trace("checkContentFileAccessPermission({})", identifier);
        if (StringUtils.isNotEmpty(identifier)) {
            try {
                String query = "+" + SolrConstants.PI + ":" + identifier;
                Set<String> requiredAccessConditions = new HashSet<>();
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

                User user = BeanUtils.getUserFromRequest(request);
                if (user == null) {
                    UserBean userBean = BeanUtils.getUserBean();
                    if (userBean != null) {
                        user = userBean.getUser();
                    }
                }

                Map<String, Boolean> lokalAccessMap =
                        checkAccessPermission(DataManager.getInstance().getDao().getRecordLicenseTypes(), requiredAccessConditions,
                                IPrivilegeHolder.PRIV_DOWNLOAD_ORIGINAL_CONTENT, user, NetTools.getIpAddress(request), query, files);
                accessMap.putAll(lokalAccessMap);
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
            }
        }
        if (request != null && request.getSession() != null) {
            request.getSession().setAttribute(attributeName, accessMap);
        }
        //return only the access status for the relevant files
        return accessMap.entrySet()
                .stream()
                .filter(entry -> files.contains(Paths.get(entry.getKey())))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    /**
     * Checks whether the client may access an image (by image URN).
     *
     * @param imageUrn Image URN.
     * @param request Calling HttpServiceRequest.
     * @return true if access is granted; false otherwise.
     * @param privilegeName a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static boolean checkAccessPermissionByImageUrn(String imageUrn, String privilegeName, HttpServletRequest request)
            throws IndexUnreachableException, DAOException {
        logger.trace("checkAccessPermissionByImageUrn({}, {}, {}, {})", imageUrn, privilegeName, request.getAttributeNames());
        if (StringUtils.isEmpty(imageUrn)) {
            return false;
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
                    .search(query, 1, null,
                            Arrays.asList(new String[] { SolrConstants.ACCESSCONDITION, SolrConstants.PI_TOPSTRUCT }));
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
                    privilegeName, user, NetTools.getIpAddress(request), query);
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
            return false;
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
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static boolean checkAccessPermission(Set<String> requiredAccessConditions, String privilegeName, String query, HttpServletRequest request)
            throws IndexUnreachableException, PresentationException, DAOException {
        User user = BeanUtils.getUserFromRequest(request);
        if (user == null) {
            UserBean userBean = BeanUtils.getUserBean();
            if (userBean != null) {
                user = userBean.getUser();
            }
        }
        return checkAccessPermission(DataManager.getInstance().getDao().getRecordLicenseTypes(), requiredAccessConditions, privilegeName, user,
                NetTools.getIpAddress(request), query);
    }

    /**
     * Checks access permission for the given image and puts the permission status into the corresponding session map.
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param pi a {@link java.lang.String} object.
     * @param contentFileName a {@link java.lang.String} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static boolean checkAccessPermissionForImage(HttpServletRequest request, String pi, String contentFileName)
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
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static boolean checkAccessPermissionForThumbnail(HttpServletRequest request, String pi, String contentFileName)
            throws IndexUnreachableException, DAOException {
        return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName, IPrivilegeHolder.PRIV_VIEW_THUMBNAILS);
    }

    /**
     * Checks access permission for the given image and puts the permission status into the corresponding session map.
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static boolean checkAccessPermissionForPagePdf(HttpServletRequest request, PhysicalElement page)
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
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static boolean checkAccessPermissionByIdentifierAndFilePathWithSessionMap(HttpServletRequest request, String filePath,
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
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @SuppressWarnings("unchecked")
    public static boolean checkAccessPermissionByIdentifierAndFileNameWithSessionMap(HttpServletRequest request, String pi, String contentFileName,
            String privilegeType) throws IndexUnreachableException, DAOException {
        // logger.trace("checkAccessPermissionByIdentifierAndFileNameWithSessionMap: {}, {}, {}", pi, contentFileName, privilegeType);
        if (privilegeType == null) {
            throw new IllegalArgumentException("privilegeType may not be null");
        }
        boolean access = false;
        // logger.debug("session id: " + request.getSession().getId());
        // Session persistent permission check: Servlet-local method.
        String attributeName = IPrivilegeHolder._PRIV_PREFIX + privilegeType;
        // logger.trace("Checking session attribute: {}", attributeName);
        Map<String, Boolean> permissions = null;
        if (request != null) {
            permissions = (Map<String, Boolean>) request.getSession().getAttribute(attributeName);
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
        // logger.debug("permissions key: " + key + ": " + permissions.get(key));
        if (permissions.containsKey(key)) {
            access = permissions.get(key);
            //            logger.trace("Access ({}) previously checked and is {} for '{}/{}' (Session ID {})", privilegeType, access, pi, contentFileName,
            //                    request.getSession().getId());
        } else {
            // TODO check for all images and save to map
            Map<String, Boolean> accessMap = checkAccessPermissionByIdentifierAndFileName(pi, contentFileName, privilegeType, request);
            for (String pageFileName : accessMap.keySet()) {
                String newKey = new StringBuilder(pi).append('_').append(pageFileName).toString();
                boolean pageAccess = accessMap.get(pageFileName);
                permissions.put(newKey, pageAccess);
            }
            access = permissions.get(key) != null ? permissions.get(key) : false;
            // logger.debug("Access ({}) not yet checked for '{}/{}', access is {}", privilegeType, pi, contentFileName,
            // access);
            if (request != null) {
                request.getSession().setAttribute(attributeName, permissions);
            }
        }

        return access;
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
     * @should return true if required access conditions empty
     * @should return true if required access conditions contain only open access
     * @should return true if all license types allow privilege by default
     * @should return false if not all license types allow privilege by default
     * @should return true if ip range allows access
     * @should not return true if no ip range matches
     *
     *         TODO user license checks
     * @param remoteAddress a {@link java.lang.String} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static boolean checkAccessPermission(List<LicenseType> allLicenseTypes, Set<String> requiredAccessConditions, String privilegeName,
            User user, String remoteAddress, String query) throws IndexUnreachableException, PresentationException, DAOException {
        return !checkAccessPermission(allLicenseTypes, requiredAccessConditions, privilegeName, user, remoteAddress, query, null).values()
                .contains(Boolean.FALSE);
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
     */
    static Map<String, Boolean> checkAccessPermission(List<LicenseType> allLicenseTypes, Set<String> requiredAccessConditions,
            String privilegeName, User user, String remoteAddress, String query, List<Path> files)
            throws IndexUnreachableException, PresentationException, DAOException {
        // logger.trace("checkAccessPermission({},{})", requiredAccessConditions, privilegeName);

        Map<String, Boolean> accessMap = new HashMap<>();
        if (files != null) {
            files.forEach(file -> accessMap.put(file.toString(), Boolean.FALSE));
        } else {
            accessMap.put("", Boolean.FALSE);
        }
        // If user is superuser, allow immediately
        if (user != null && user.isSuperuser()) {
            accessMap.keySet().forEach(key -> accessMap.put(key, Boolean.TRUE));
            return accessMap;
        }
        // If no access condition given, allow immediately (though this should never be the case)
        if (requiredAccessConditions.isEmpty()) {
            logger.trace("No required access conditions given, access granted.");
            accessMap.keySet().forEach(key -> accessMap.put(key, Boolean.TRUE));
            return accessMap;
        }
        // If OPENACCESS is the only condition, allow immediately
        if (isFreeOpenAccess(requiredAccessConditions, allLicenseTypes)) {
            accessMap.keySet().forEach(key -> accessMap.put(key, Boolean.TRUE));
            return accessMap;
        }
        // If no license types are configured or no privilege name is given, deny immediately
        if (allLicenseTypes == null || !StringUtils.isNotEmpty(privilegeName)) {
            logger.trace("No license types or no privilege name given.");
            accessMap.keySet().forEach(key -> accessMap.put(key, Boolean.FALSE));
            return accessMap;
        }

        Map<String, List<LicenseType>> licenseMap = getRelevantLicenseTypesOnly(allLicenseTypes, requiredAccessConditions, query, accessMap);
        // If no relevant license types found (configured), deny all
        if (licenseMap.isEmpty()) {
            accessMap.keySet().forEach(key -> accessMap.put(key, Boolean.FALSE));
            return accessMap;
        }

        for (String key : licenseMap.keySet()) {
            List<LicenseType> relevantLicenseTypes = licenseMap.get(key);
            requiredAccessConditions = new HashSet<>(relevantLicenseTypes.size());
            if (relevantLicenseTypes.isEmpty()) {
                // No relevant license types for this file, set to false and continue
                logger.trace("No relevant license types.");
                accessMap.put(key, Boolean.FALSE);
                continue;
            }

            // If all relevant license types allow the requested privilege by default, allow access
            boolean licenseTypeAllowsPriv = true;
            // Check whether *all* relevant license types allow the requested privilege by default. As soon as one doesn't, set to false.
            for (LicenseType licenseType : relevantLicenseTypes) {
                requiredAccessConditions.add(licenseType.getName());
                if (!licenseType.getPrivileges().contains(privilegeName) && !licenseType.isOpenAccess() && !licenseType.isRestrictionsExpired(query)) {
                    logger.trace("LicenseType '{}' does not allow the action '{}' by default.", licenseType.getName(), privilegeName);
                    licenseTypeAllowsPriv = false;
                }
            }
            if (licenseTypeAllowsPriv) {
                logger.trace("Privilege '{}' is allowed by default in all license types.", privilegeName);
                accessMap.put(key, Boolean.TRUE);
            } else if (isFreeOpenAccess(requiredAccessConditions, relevantLicenseTypes)) {
                logger.trace("Privilege '{}' is OpenAccess", privilegeName);
                accessMap.put(key, Boolean.TRUE);
            } else {
                // Check IP range
                if (StringUtils.isNotEmpty(remoteAddress)) {
                    if (NetTools.isIpAddressLocalhost(remoteAddress)
                            && DataManager.getInstance().getConfiguration().isFullAccessForLocalhost()) {
                        logger.trace("Access granted to localhost");
                        accessMap.put(key, Boolean.TRUE);
                        continue;
                    }
                    // Check whether the requested privilege is allowed to this IP range (for all access conditions)
                    for (IpRange ipRange : DataManager.getInstance().getDao().getAllIpRanges()) {
                        // logger.debug("ip range: " + ipRange.getSubnetMask());
                        if (ipRange.matchIp(remoteAddress)
                                && ipRange.canSatisfyAllAccessConditions(requiredAccessConditions, relevantLicenseTypes, privilegeName, null)) {
                            logger.trace("Access granted to {} via IP range {}", remoteAddress, ipRange.getName());
                            accessMap.put(key, Boolean.TRUE);
                            continue;
                        }
                    }
                }

                // If not within an allowed IP range, check the current user's satisfied access conditions

                if (user != null && user.canSatisfyAllAccessConditions(requiredAccessConditions, privilegeName, null)) {
                    accessMap.put(key, Boolean.TRUE);
                }
            }
        }

        return accessMap;
    }

    /**
     * Check whether the requiredAccessConditions consist only of the {@link io.goobi.viewer.controller.SolrConstants#OPEN_ACCESS_VALUE OPENACCESS}
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
     * @should remove license types whose condition query excludes the given pi
     * @should not remove moving wall license types to open access if condition query excludes given pi
     */
    static Map<String, List<LicenseType>> getRelevantLicenseTypesOnly(List<LicenseType> allLicenseTypes, Set<String> requiredAccessConditions,
            String query, Map<String, Boolean> accessMap) throws IndexUnreachableException, PresentationException {
        if (requiredAccessConditions == null || requiredAccessConditions.isEmpty()) {
            return accessMap.keySet().stream().collect(Collectors.toMap(Function.identity(), key -> Collections.emptyList()));
        }

        //         logger.trace("getRelevantLicenseTypesOnly: {} | {}", query, requiredAccessConditions);
        Map<String, List<LicenseType>> ret = new HashMap<>(accessMap.size());
        for (LicenseType licenseType : allLicenseTypes) {
            // logger.trace("{}, moving wall: {}", licenseType.getName(), licenseType.isMovingWall());
            if (!requiredAccessConditions.contains(licenseType.getName())) {
                continue;
            }
            // Check whether the license type contains conditions that exclude the given record, in that case disregard this license type
            if (StringUtils.isNotEmpty(licenseType.getProcessedConditions()) && StringUtils.isNotEmpty(query)) {
                String conditions = licenseType.getProcessedConditions();
                // logger.trace("License conditions: {}", conditions);
                StringBuilder sbQuery = new StringBuilder().append("+(").append(query).append(')');
                if (conditions.charAt(0) == '-' || conditions.charAt(0) == '+') {
                    sbQuery.append(' ').append(conditions);
                } else {
                    // Make sure the condition query is not optional (starts with + or -)
                    sbQuery.append(" +(").append(conditions).append(')');
                }
                logger.trace("License relevance query: {}", sbQuery.toString());
                if (DataManager.getInstance().getSearchIndex().getHitCount(sbQuery.toString()) == 0) {
                    // logger.trace("LicenseType '{}' does not apply to resource described by '{}' due to configured the license subquery.", licenseType.getName(), query);
                    if (licenseType.isMovingWall()) {
                        // Moving wall license type allow everything if the condition query doesn't match
                        logger.trace(
                                "License type '{}' is a moving wall type and its condition query doesn't match the record query '{}'. All restrictions lifted.",
                                licenseType.getName(), query);
                        licenseType.getRestrictionsExpired().put(query, true);
                    } else {
                        continue;
                    }
                }
                logger.trace("LicenseType '{}' applies to resource described by '{}' due to configured license subquery.", licenseType.getName(),
                        query);
            }

            String filenameConditions = licenseType.getFilenameConditions();
            if (StringUtils.isNotBlank(filenameConditions)) {
                for (String path : accessMap.keySet()) {
                    if (StringUtils.isNotBlank(path)) {
                        List<LicenseType> types = ret.get(path);
                        if (types == null) {
                            types = new ArrayList<>();
                            ret.put(path, types);
                        }
                        if (Pattern.matches(licenseType.getFilenameConditions(), Paths.get(path).getFileName().toString())) {
                            types.add(licenseType);
                        }
                    }
                }
            } else {
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
        }

        return ret;
    }
}
