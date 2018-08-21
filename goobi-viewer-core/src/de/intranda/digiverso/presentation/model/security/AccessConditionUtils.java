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
package de.intranda.digiverso.presentation.model.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrConstants.DocType;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.UserBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.security.user.IpRange;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;

public class AccessConditionUtils {

    private static final Logger logger = LoggerFactory.getLogger(AccessConditionUtils.class);

    /**
     * 
     * @param request
     * @param action
     * @param pi
     * @param contentFileName
     * @param isThumbnail
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
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
     */
    static String[] generateAccessCheckQuery(String identifier, String fileName) {
        String[] ret = new String[2];
        if (fileName != null) {
            StringBuilder sbQuery = new StringBuilder();
            String useFileField = SolrConstants.FILENAME;
            String useFileName = fileName;
            // Different media types have the file name in different fields
            String extension = FilenameUtils.getExtension(fileName).toLowerCase();
            switch (extension) {
                case "webm":
                    useFileField = SolrConstants.FILENAME_WEBM;
                    break;
                case "mp4":
                    useFileField = SolrConstants.FILENAME_MP4;
                    break;
                case "mp3":
                    // if the mime type in METS is not audio/mpeg3 but something else, access will be false
                    useFileField = SolrConstants.FILENAME_MPEG3;
                    break;
                case "ogg":
                case "ogv":
                    useFileField = SolrConstants.FILENAME_OGG;
                    break;
                case "txt":
                case "xml":
                    useFileName = fileName.replace(extension, "*");
                    break;
                case "":
                    useFileName = fileName + ".*";
                default:
                    break;
            }
            sbQuery.append(SolrConstants.PI_TOPSTRUCT).append(':').append(identifier).append(" AND ").append(useFileField).append(':');
            if (useFileName.endsWith(".*")) {
                sbQuery.append(useFileName);
            } else {
                sbQuery.append("\"").append(useFileName).append("\"");
            }

            // logger.trace(sbQuery.toString());
            ret[0] = sbQuery.toString();
            ret[1] = useFileField;
        }
        return ret;
    }

    /**
     * Checks whether the client may access an image (by PI + file name).
     *
     * @param identifier Work identifier (PI).
     * @param fileName Image file name. For all files of a record, use "*".
     * @param request Calling HttpServiceRequest.
     * @return true if access is granted; false otherwise.
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    protected static Map<String, Boolean> checkAccessPermissionByIdentifierAndFileName(String identifier, String fileName, String privilegeName,
            HttpServletRequest request) throws IndexUnreachableException, DAOException {
        // logger.trace("checkAccessPermissionByIdentifierAndFileName({}, {}, {})", identifier, fileName, privilegeName);
        if (StringUtils.isNotEmpty(identifier)) {
            String[] query = generateAccessCheckQuery(identifier, fileName);
            logger.trace("query: {}", query[0]);
            try {
                // Collect access conditions required by the page
                Map<String, Set<String>> requiredAccessConditions = new HashMap<>();
                SolrDocumentList results =
                        DataManager.getInstance().getSearchIndex().search(query[0], "*".equals(fileName) ? SolrSearchIndex.MAX_HITS : 1, null,
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
                    boolean access = checkAccessPermission(DataManager.getInstance().getDao().getNonOpenAccessLicenseTypes(), pageAccessConditions,
                            privilegeName, user, Helper.getIpAddress(request), query[0]);
                    ret.put(pageFileName, access);
                }
                return ret;
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
            }
        }

        return new HashMap<>(0);

    }

    /**
     * Checks whether the client may access an image (by PI + file name).
     *
     * @param identifier Work identifier (PI).
     * @param fileName Image file name. For all files of a record, use "*".
     * @param request Calling HttpServiceRequest.
     * @return true if access is granted; false otherwise.
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    protected static boolean checkAccessPermissionByIdentifierAndPageOrder(PhysicalElement page, String privilegeName, HttpServletRequest request)
            throws IndexUnreachableException, DAOException {
        if (page == null) {
            throw new IllegalArgumentException("page may not be null");
        }

        String query = SolrConstants.PI_TOPSTRUCT + ":" + page.getPi() + " AND " + SolrConstants.ORDER + ":" + page.getOrder();
        try {
            User user = BeanUtils.getUserFromRequest(request);
            if (user == null) {
                UserBean userBean = BeanUtils.getUserBean();
                if (userBean != null) {
                    user = userBean.getUser();
                }
            }
            Map<String, Boolean> ret = new HashMap<>(page.getAccessConditions().size());
            boolean access = checkAccessPermission(DataManager.getInstance().getDao().getNonOpenAccessLicenseTypes(), page.getAccessConditions(),
                    privilegeName, user, Helper.getIpAddress(request), query);
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
     * @param request
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public static boolean checkAccessPermissionByIdentifierAndLogId(String identifier, String logId, String privilegeName, HttpServletRequest request)
            throws IndexUnreachableException, DAOException {
        logger.trace("checkAccessPermissionByIdentifierAndLogId({}, {}, {})", identifier, logId, privilegeName);
        if (StringUtils.isNotEmpty(identifier)) {
            StringBuilder sbQuery = new StringBuilder();
            if (StringUtils.isNotEmpty(logId)) {
                // Sub-docstruct
                sbQuery.append(SolrConstants.PI_TOPSTRUCT).append(':').append(identifier);
                sbQuery.append(" AND ").append(SolrConstants.LOGID).append(':').append(logId);
            } else {
                // Top document
                sbQuery.append(SolrConstants.PI).append(':').append(identifier);
            }
            // Only query docstruct docs because metadata/event docs may not contain values defined in the license type filter query
            sbQuery.append(" AND ").append(SolrConstants.DOCTYPE).append(':').append(DocType.DOCSTRCT.name());
            try {
                Set<String> requiredAccessConditions = new HashSet<>();
                logger.trace(sbQuery.toString());
                SolrDocumentList results = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), 1, null,
                        Arrays.asList(new String[] { SolrConstants.ACCESSCONDITION }));
                if (results != null) {
                    for (SolrDocument doc : results) {
                        Collection<Object> fieldsAccessConddition = doc.getFieldValues(SolrConstants.ACCESSCONDITION);
                        if (fieldsAccessConddition != null) {
                            for (Object accessCondition : fieldsAccessConddition) {
                                requiredAccessConditions.add((String) accessCondition);
                                logger.debug(accessCondition.toString());
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
                return checkAccessPermission(DataManager.getInstance().getDao().getNonOpenAccessLicenseTypes(), requiredAccessConditions,
                        privilegeName, user, Helper.getIpAddress(request), sbQuery.toString());
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
            }
        }

        return false;
    }

    public static boolean checkContentFileAccessPermission(String identifier, HttpServletRequest request)
            throws IndexUnreachableException, DAOException {
        // logger.trace("checkContentFileAccessPermission({})", identifier);
        if (StringUtils.isNotEmpty(identifier)) {
            StringBuilder sbQuery = new StringBuilder();
            sbQuery.append(SolrConstants.PI).append(':').append(identifier);
            try {
                Set<String> requiredAccessConditions = new HashSet<>();
                SolrDocumentList results = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), 1, null,
                        Arrays.asList(new String[] { SolrConstants.ACCESSCONDITION }));
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
                return checkAccessPermission(DataManager.getInstance().getDao().getNonOpenAccessLicenseTypes(), requiredAccessConditions,
                        IPrivilegeHolder.PRIV_DOWNLOAD_ORIGINAL_CONTENT, user, Helper.getIpAddress(request), sbQuery.toString());
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
            }
        }

        return false;
    }

    /**
     * Checks whether the client may access an image (by image URN).
     *
     * @param imageUrn Image URN.
     * @param request Calling HttpServiceRequest.
     * @return true if access is granted; false otherwise.
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public static boolean checkAccessPermissionByImageUrn(String imageUrn, String privilegeName, HttpServletRequest request)
            throws IndexUnreachableException, DAOException {
        logger.trace("checkAccessPermissionByImageUrn({}, {}, {}, {})", imageUrn, privilegeName, request.getAttributeNames());
        if (StringUtils.isNotEmpty(imageUrn)) {
            StringBuilder sbQuery = new StringBuilder();
            sbQuery.append(SolrConstants.IMAGEURN).append(':').append(imageUrn.replace(":", "\\:"));
            try {
                Set<String> requiredAccessConditions = new HashSet<>();
                SolrDocumentList hits = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), 1, null,
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
                return checkAccessPermission(DataManager.getInstance().getDao().getNonOpenAccessLicenseTypes(), requiredAccessConditions,
                        privilegeName, user, Helper.getIpAddress(request), sbQuery.toString());
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
            }
        }

        return false;
    }

    /**
     *
     * @param requiredAccessConditions
     * @param privilegeName
     * @param pi
     * @param request
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
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
        return checkAccessPermission(DataManager.getInstance().getDao().getNonOpenAccessLicenseTypes(), requiredAccessConditions, privilegeName, user,
                Helper.getIpAddress(request), query);
    }

    /**
     * Checks access permission for the given image and puts the permission status into the corresponding session map.
     *
     * @param request
     * @param pi
     * @param contentFileName
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public static boolean checkAccessPermissionForImage(HttpServletRequest request, String pi, String contentFileName)
            throws IndexUnreachableException, DAOException {
        logger.trace("checkAccessPermissionForImage: {}/{}", pi, contentFileName);
        return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName, IPrivilegeHolder.PRIV_VIEW_IMAGES);
    }

    /**
     * Checks access permission for the given thumbnail and puts the permission status into the corresponding session map.
     *
     * @param request
     * @param pi
     * @param contentFileName
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public static boolean checkAccessPermissionForThumbnail(HttpServletRequest request, String pi, String contentFileName)
            throws IndexUnreachableException, DAOException {
        return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName, IPrivilegeHolder.PRIV_VIEW_THUMBNAILS);
    }

    /**
     * Checks access permission for the given image and puts the permission status into the corresponding session map.
     *
     * @param request
     * @param pi
     * @param contentFileName
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public static boolean checkAccessPermissionForPagePdf(HttpServletRequest request, PhysicalElement page)
            throws IndexUnreachableException, DAOException {
        if (page == null) {
            throw new IllegalArgumentException("page may not be null");
        }
        logger.trace("checkAccessPermissionForPagePdf: {}/{}", page.getPi(), page.getOrder());
        return checkAccessPermissionByIdentifierAndPageOrder(page, IPrivilegeHolder.PRIV_DOWNLOAD_PDF, request);
    }

    /**
     * 
     * @param request
     * @param filePath FILENAME_ALTO or FILENAME_FULLTEXT value
     * @param privilegeType
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
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
     * @param request
     * @param pi
     * @param contentFileName
     * @param privilegeType
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    @SuppressWarnings("unchecked")
    public static boolean checkAccessPermissionByIdentifierAndFileNameWithSessionMap(HttpServletRequest request, String pi, String contentFileName,
            String privilegeType) throws IndexUnreachableException, DAOException {
        logger.trace("checkAccessPermissionByIdentifierAndFileNameWithSessionMap: {}, {}, {}", pi, contentFileName, privilegeType);
        if (privilegeType == null) {
            throw new IllegalArgumentException("privilegeType may not be null");
        }
        boolean access = false;
        // logger.debug("session id: " + request.getSession().getId());
        // Session persistent permission check: Servlet-local method.
        String attributeName = IPrivilegeHolder._PRIV_PREFIX + privilegeType;
        logger.trace("Checking session attribute: {}", attributeName);
        Map<String, Boolean> permissions = null;
        if (request != null) {
            permissions = (Map<String, Boolean>) request.getSession().getAttribute(attributeName);
        }
        if (permissions == null) {
            permissions = new HashMap<>();
            logger.trace("Session attribute not found, creating new");
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
            logger.trace("Access ({}) previously checked and is {} for '{}/{}' (Session ID {})", privilegeType, access, pi, contentFileName,
                    request.getSession().getId());
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
     *
     * @param allLicenseTypes
     * @param requiredAccessConditions Set of access condition names to satisfy (one suffices).
     * @param privilegeName The particular privilege to check.
     * @param user Logged in user.
     * @param query Solr query describing the resource in question.
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     * @should return true if required access conditions empty
     * @should return true if required access conditions contain only open access
     * @should return true if all license types allow privilege by default
     * @should return false if not all license types allow privilege by default
     * @should return true if ip range allows access
     * @should not return true if no ip range matches
     * 
     *         TODO user license checks
     */
    public static boolean checkAccessPermission(List<LicenseType> allLicenseTypes, Set<String> requiredAccessConditions, String privilegeName,
            User user, String remoteAddress, String query) throws IndexUnreachableException, PresentationException, DAOException {
        // logger.trace("checkAccessPermission({},{})", requiredAccessConditions, privilegeName);
        // If OPENACCESS is the only condition, allow immediately
        if (requiredAccessConditions.isEmpty()) {
            logger.debug("No required access conditions given, access granted.");
            return true;
        }
        if (requiredAccessConditions.size() == 1 && (requiredAccessConditions.contains(SolrConstants.OPEN_ACCESS_VALUE)
                || requiredAccessConditions.contains(SolrConstants.OPEN_ACCESS_VALUE.toLowerCase()))) {
            return true;
        }
        // If no license types are configured or no privilege name is given, allow immediately
        if (allLicenseTypes == null || !StringUtils.isNotEmpty(privilegeName)) {
            logger.trace("No license types or no privilege name given.");
            return true;
        }

        List<LicenseType> relevantLicenseTypes = getRelevantLicenseTypesOnly(allLicenseTypes, requiredAccessConditions, query);
        requiredAccessConditions = new HashSet<>(relevantLicenseTypes.size());
        if (relevantLicenseTypes.isEmpty()) {
            logger.trace("No relevant license types.");
            return true;
        }

        // If all relevant license types allow the requested privilege by default, allow access
        {
            boolean licenseTypeAllowsPriv = true;
            // Check whether *all* relevant license types allow the requested privilege by default
            for (LicenseType licenseType : relevantLicenseTypes) {
                requiredAccessConditions.add(licenseType.getName());
                if (!licenseType.getPrivileges().contains(privilegeName)) {
                    // logger.debug("LicenseType '" + licenseType.getName() + "' does not allow the action '" + privilegeName
                    // + "' by default.");
                    licenseTypeAllowsPriv = false;
                }
            }
            if (licenseTypeAllowsPriv) {
                logger.trace("Privilege '{}' is allowed by default in all license types.", privilegeName);
                return true;
            }
        }

        // Check IP range
        if (StringUtils.isNotEmpty(remoteAddress)) {
            if (Helper.ADDRESS_LOCALHOST_IPV6.equals(remoteAddress) || Helper.ADDRESS_LOCALHOST_IPV4.equals(remoteAddress)) {
                if (DataManager.getInstance().getConfiguration().isFullAccessForLocalhost()) {
                    logger.debug("Access granted to localhost");
                    return true;
                }
            } else {
                // Check whether the requested privilege is allowed to this IP range (for all access conditions)
                Map<String, Boolean> permissionMap = new HashMap<>(requiredAccessConditions.size());
                for (IpRange ipRange : DataManager.getInstance().getDao().getAllIpRanges()) {
                    // logger.debug("ip range: " + ipRange.getSubnetMask());
                    if (ipRange.matchIp(remoteAddress) && ipRange.canSatisfyAllAccessConditions(requiredAccessConditions, privilegeName, null)) {
                        logger.debug("Access granted to {} via IP range {}", remoteAddress, ipRange.getName());
                        return true;
                    }
                }
            }
        }

        // If not within an allowed IP range, check the current user's satisfied access conditions
        if (user != null && user.canSatisfyAllAccessConditions(requiredAccessConditions, privilegeName, null)) {
            return true;
        }

        // logger.trace("not allowed");
        return false;
    }

    /**
     * Filters the given list of license types my removing those that have Solr query conditions that do not match the given identifier.
     *
     * @param allLicenseTypes
     * @param requiredAccessConditions
     * @param query
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @should remove license types whose names do not match access conditions
     * @should remove license types whose condition query excludes the given pi
     */
    static List<LicenseType> getRelevantLicenseTypesOnly(List<LicenseType> allLicenseTypes, Set<String> requiredAccessConditions, String query)
            throws IndexUnreachableException, PresentationException {
        if (requiredAccessConditions == null || requiredAccessConditions.isEmpty()) {
            return Collections.emptyList();
        }

        logger.trace("getRelevantLicenseTypesOnly: {} | {}", query, requiredAccessConditions);
        List<LicenseType> ret = new ArrayList<>(allLicenseTypes.size());
        for (LicenseType licenseType : allLicenseTypes) {
            // logger.trace(licenseType.getName());
            if (!requiredAccessConditions.contains(licenseType.getName())) {
                continue;
            }
            // Check whether the license type contains conditions that exclude the given record, in that case disregard this
            // license type
            if (StringUtils.isNotEmpty(licenseType.getConditions()) && StringUtils.isNotEmpty(query)) {
                String conditions = licenseType.getProcessedConditions();
                // logger.trace("License conditions: {}", conditions);
                StringBuilder sbQuery = new StringBuilder(query);
                if (conditions.charAt(0) == '-') {
                    // do not wrap the conditions in parentheses if it starts with a negation, otherwise it won't work
                    sbQuery.append(" AND ").append(conditions);
                } else {
                    sbQuery.append(" AND (").append(conditions).append(')');
                }
                // logger.trace("License relevance query: {}", sbQuery.toString());
                if (DataManager.getInstance().getSearchIndex().getHitCount(sbQuery.toString()) == 0) {
                    // logger.trace("LicenseType '{}' does not apply to resource described by '{}' due to configured the
                    // license subquery.", licenseType
                    // .getName(), query);
                    continue;
                }
                logger.trace("LicenseType '{}' applies to resource described by '{}' due to configured license subquery.", licenseType.getName(),
                        query);
            }
            ret.add(licenseType);
        }

        return ret;
    }
}
