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
import java.util.stream.Collectors;

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
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.clients.ClientApplication;
import io.goobi.viewer.model.security.clients.ClientApplicationManager;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.IpRangeCache;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Utility class providing methods to evaluate access conditions and licence restrictions for records, images, and metadata.
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
     * checkAccess.
     *
     * @param session HTTP session for caching permission results
     * @param action access action type (e.g. "image", "text", "pdf")
     * @param pi persistent identifier of the record
     * @param contentFileName name of the content file being accessed
     * @param ipAddress client IP address
     * @param isThumbnail true if the request is for a thumbnail image
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static AccessPermission checkAccess(HttpSession session, String action, String pi, String contentFileName, String ipAddress,
            boolean isThumbnail)
            throws IndexUnreachableException, DAOException {
        return checkAccess(session, action, pi, contentFileName, ipAddress, isThumbnail, null);
    }

    /**
     * checkAccess.
     *
     * @param session HTTP session for caching permission results
     * @param action access action type (e.g. "image", "text", "pdf")
     * @param pi persistent identifier of the record
     * @param contentFileName name of the content file being accessed
     * @param ipAddress client IP address
     * @param isThumbnail true if the request is for a thumbnail image
     * @param user the User requesting access. If null, it is fetched from the jsfContext if one exists
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static AccessPermission checkAccess(HttpSession session, String action, String pi, String contentFileName, String ipAddress,
            boolean isThumbnail, User user)
            throws IndexUnreachableException, DAOException {
        if (session == null) {
            throw new IllegalArgumentException("session may not be null");
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
                    return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(session, pi, contentFileName,
                            IPrivilegeHolder.PRIV_DOWNLOAD_PDF, ipAddress, user);
                }
                if (isThumbnail) {
                    return checkAccessPermissionForThumbnail(session, pi, contentFileName, ipAddress);
                    // logger.trace("Checked thumbnail access: {}/{}: {}", pi, contentFileName, access); //NOSONAR Debug
                }
                return checkAccessPermissionForImage(session, pi, contentFileName, ipAddress);
            // logger.trace("Checked image access: {}/{}: {}", pi, contentFileName, access); //NOSONAR Debug
            case "text":
            case "ocrdump":
                return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(session, pi, contentFileName, IPrivilegeHolder.PRIV_VIEW_FULLTEXT,
                        ipAddress, user);
            case "pdf":
            case "epub":
                return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(session, pi, contentFileName, IPrivilegeHolder.PRIV_DOWNLOAD_PDF,
                        ipAddress, user);
            case "video":
                return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(session, pi, contentFileName, IPrivilegeHolder.PRIV_VIEW_VIDEO,
                        ipAddress, user);
            case "audio":
                return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(session, pi, contentFileName, IPrivilegeHolder.PRIV_VIEW_AUDIO,
                        ipAddress, user);
            case "dimensions":
            case "version":
                // TODO is priv checking needed here?
                return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(session, pi, contentFileName, IPrivilegeHolder.PRIV_VIEW_IMAGES,
                        ipAddress, user);
            default: // nothing
                break;
        }

        return AccessPermission.denied();
    }

    /**
     *
     * @param identifier persistent identifier of the record
     * @param fileName content file name to build the query for
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
            case "wav":
                sbQuery.append(" +").append(useFileField).append(':');
                // Escape whitespaces etc. for wildcard searches
                sbQuery.append(ClientUtils.escapeQueryChars(baseFileName)).append(".*");
                break;
            case "txt":
                sbQuery.append(" +(")
                        .append(SolrConstants.FILENAME_FULLTEXT)
                        .append(":\"")
                        .append(fileName)
                        .append("\" ")
                        .append(SolrConstants.FILENAME_FULLTEXT_SHORT)
                        .append(":\"")
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
                        .append(SolrConstants.FILENAME_ALTO_SHORT)
                        .append(":\"")
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
     * @param privilegeName access privilege name to verify
     * @param ipAddress client IP address
     * @param session HTTP session for caching permission results
     * @param user currently logged-in user, or null for anonymous access
     * @return true if access is granted; false otherwise.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    static Map<String, AccessPermission> checkAccessPermissionByIdentifierAndFileName(String identifier, String fileName, String privilegeName,
            String ipAddress, HttpSession session, User user) throws IndexUnreachableException, DAOException {
        // logger.trace("checkAccessPermissionByIdentifierAndFileName({}, {}, {})", identifier, fileName, privilegeName); //NOSONAR Debug
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
                            // logger.trace(accessCondition.toString()); //NOSONAR Debug
                        }
                        requiredAccessConditions.put(fileName, pageAccessConditions);
                    }
                }
            }

            Map<String, AccessPermission> ret = HashMap.newHashMap(requiredAccessConditions.size());
            // Resolve user once before the loop to avoid repeated expensive session attribute scans
            // (findInstanceInSessionAttributes) when requiredAccessConditions has multiple entries.
            User resolvedUser = user != null ? user : retrieveUserFromContext(session);
            for (Entry<String, Set<String>> entry : requiredAccessConditions.entrySet()) {
                Set<String> pageAccessConditions = entry.getValue();
                AccessPermission access = checkAccessPermission(DataManager.getInstance().getLicenseTypeCache().getRecordLicenseTypes(), pageAccessConditions,
                        privilegeName, resolvedUser, ipAddress,
                        ClientApplicationManager.getClientFromSession(session), query);
                ret.put(entry.getKey(), access);
            }
            return ret;
        } catch (PresentationException e) {
            logger.debug(e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Central method to retrieve user from a HttpSession.
     * 
     * @param session The session in which the user data is stored
     * @return The user logged into the given session. May be null if no user is logged in
     * @should return null for null session
     * @should return user from direct session attribute
     * @should return null without session scan when cdi returns null user
     * @should return user via session scan when stored under non standard key
     */
    public static User retrieveUserFromContext(HttpSession session) {
        try {
            UserBean userBean = BeanUtils.getUserBean(); //CDI lookup, faster than scanning session
            if (userBean != null) {
                // CDI is active and authoritative for the current session. Return the user directly,
                // even if null (anonymous). Falling through to the session scan for anonymous users
                // is redundant: CDI and the session share the same @SessionScoped UserBean instance,
                // so the scan cannot return a different result — only O(N) overhead over all session
                // attributes under potential lock contention from parallel request threads.
                return userBean.getUser();
            }
        } catch (ContextNotActiveException e) {
            // No CDI context (background thread) — fall through to session scan as the only option
        }
        return BeanUtils.getUserFromSession(session); //expensive scan of whole session. Only as fallback
    }

    /**
     * Fetches and pre-evaluates access permissions for all pages of a record in a single batch
     * Solr query, avoiding O(n) per-page Solr queries during IIIF manifest generation.
     *
     * <p>Steps:
     * <ol>
     *   <li>One Solr query: {@code +PI_TOPSTRUCT:pi +DOCTYPE:PAGE} (fields: ORDER, ACCESSCONDITION)</li>
     *   <li>One cache read: {@code getRecordLicenseTypes()}</li>
     *   <li>One user + IP resolution from {@code request}</li>
     *   <li>In-memory evaluation of VIEW_IMAGES, VIEW_THUMBNAILS, ZOOM_IMAGES,
     *   DOWNLOAD_IMAGES, VIEW_FULLTEXT, DOWNLOAD_PAGE_PDF per page</li>
     * </ol>
     *
     * @param pi persistent identifier of the record
     * @param request HTTP servlet request for user and client IP resolution; may be null
     * @return populated {@link PagePermissions};
     *         {@link PagePermissions#EMPTY} when pi is blank, when no pages are found,
     *         or when a Solr/DAO error occurs (logged at WARN)
     * @should return granted permissions for open access record
     * @should return empty for blank pi
     * @should return empty for null pi
     * @should populate all six privilege maps for open access record
     */
    public static PagePermissions fetchPagePermissions(String pi, HttpServletRequest request) {
        if (StringUtils.isBlank(pi)) {
            return PagePermissions.EMPTY;
        }

        // Single Solr query for all pages of this record – avoids O(n) per-page queries
        String query = "+" + SolrConstants.PI_TOPSTRUCT + ":" + pi
                + " +" + SolrConstants.DOCTYPE + ":" + DocType.PAGE;
        try {
            SolrDocumentList pageDocs = DataManager.getInstance()
                    .getSearchIndex()
                    .search(query, SolrSearchIndex.MAX_HITS, null,
                            Arrays.asList(SolrConstants.ORDER, SolrConstants.ACCESSCONDITION));
            if (pageDocs == null || pageDocs.isEmpty()) {
                return PagePermissions.EMPTY;
            }

            // Resolve shared context once – reused across all pages to avoid repeated lookups
            List<LicenseType> licenseTypes = DataManager.getInstance().getLicenseTypeCache().getRecordLicenseTypes();
            User user = retrieveUserFromContext(request != null ? request.getSession() : null);
            String ipAddress = NetTools.getIpAddress(request);
            Optional<ClientApplication> client = ClientApplicationManager.getClientFromRequest(request);

            Map<Integer, AccessPermission> imageMap = HashMap.newHashMap(pageDocs.size());
            // Added three additional privilege maps so IIIF builders and PhysicalElement seeding
            // can rely on a single prefetch for every per-page privilege (refs #27883).
            Map<Integer, AccessPermission> thumbnailMap = HashMap.newHashMap(pageDocs.size());
            Map<Integer, AccessPermission> zoomMap = HashMap.newHashMap(pageDocs.size());
            Map<Integer, AccessPermission> downloadMap = HashMap.newHashMap(pageDocs.size());
            Map<Integer, AccessPermission> fulltextMap = HashMap.newHashMap(pageDocs.size());
            Map<Integer, AccessPermission> pdfMap = HashMap.newHashMap(pageDocs.size());

            for (SolrDocument doc : pageDocs) {
                Object orderObj = doc.getFieldValue(SolrConstants.ORDER);
                if (orderObj == null) {
                    continue;
                }
                int order = ((Number) orderObj).intValue();

                Collection<Object> acValues = doc.getFieldValues(SolrConstants.ACCESSCONDITION);
                Set<String> accessConditions = acValues != null
                        ? acValues.stream().map(Object::toString).collect(Collectors.toSet())
                        : Collections.emptySet();

                // Evaluate all six privilege types against this page's access conditions; all
                // checks reuse the shared licenseTypes/user/IP/client resolved above – no
                // additional Solr/DAO calls are issued here.
                imageMap.put(order, checkAccessPermission(licenseTypes, accessConditions,
                        IPrivilegeHolder.PRIV_VIEW_IMAGES, user, ipAddress, client, query));
                thumbnailMap.put(order, checkAccessPermission(licenseTypes, accessConditions,
                        IPrivilegeHolder.PRIV_VIEW_THUMBNAILS, user, ipAddress, client, query));
                zoomMap.put(order, checkAccessPermission(licenseTypes, accessConditions,
                        IPrivilegeHolder.PRIV_ZOOM_IMAGES, user, ipAddress, client, query));
                downloadMap.put(order, checkAccessPermission(licenseTypes, accessConditions,
                        IPrivilegeHolder.PRIV_DOWNLOAD_IMAGES, user, ipAddress, client, query));
                fulltextMap.put(order, checkAccessPermission(licenseTypes, accessConditions,
                        IPrivilegeHolder.PRIV_VIEW_FULLTEXT, user, ipAddress, client, query));
                pdfMap.put(order, checkAccessPermission(licenseTypes, accessConditions,
                        IPrivilegeHolder.PRIV_DOWNLOAD_PAGE_PDF, user, ipAddress, client, query));
            }

            return new PagePermissions(imageMap, thumbnailMap, zoomMap, downloadMap, fulltextMap, pdfMap);

        } catch (PresentationException | IndexUnreachableException | DAOException e) {
            logger.warn("Failed to prefetch page permissions for PI '{}': {}", pi, e.getMessage());
            return PagePermissions.EMPTY;
        }
    }

    /**
     * Fetches the list of filenames accessible to the current user for a given record and Solr
     * filename field, using a single batch Solr query. Permissions are evaluated in memory —
     * no further Solr queries are issued per file.
     *
     * <p>Steps:
     * <ol>
     *   <li>One Solr query: {@code +PI_TOPSTRUCT:pi +DOCTYPE:PAGE +filenameField:[* TO *]}</li>
     *   <li>One cache read: {@code getRecordLicenseTypes()}</li>
     *   <li>One user + IP resolution from {@code request}</li>
     *   <li>In-memory evaluation of {@code privilegeType} per page document</li>
     * </ol>
     *
     * <p>Only bare filenames are returned (e.g. {@code 00000001.xml}), not full Solr paths
     * (e.g. {@code alto/PI/00000001.xml}). Results are ordered by page {@code ORDER}.
     *
     * @param pi persistent identifier of the record; blank input returns an empty list immediately
     * @param filenameField Solr field to query, e.g. {@code SolrConstants.FILENAME_ALTO}
     * @param privilegeType privilege to check, e.g. {@code IPrivilegeHolder.PRIV_VIEW_FULLTEXT}
     * @param request HTTP servlet request for user and IP resolution; {@code null} = anonymous
     * @return ordered list of accessible bare filenames; empty list on any error (fail-safe)
     * @should return empty list for blank pi
     * @should return empty list for null pi
     * @should return filenames for open access record
     * @should return empty list for record with no files indexed
     * @should return bare filenames not full paths
     * @should work for fulltext field
     * @should return empty list for restricted record anonymous
     */
    public static List<String> fetchAccessibleFileNames(String pi, String filenameField,
            String privilegeType, HttpServletRequest request) {
        if (StringUtils.isBlank(pi)) {
            return Collections.emptyList();
        }

        // Single Solr query for all pages that have a value in filenameField
        String query = "+" + SolrConstants.PI_TOPSTRUCT + ":" + pi
                + " +" + SolrConstants.DOCTYPE + ":" + DocType.PAGE
                + " +" + filenameField + ":[* TO *]";

        // Separate query for permission evaluation — omits the filename-field filter so that
        // moving-wall licence types are evaluated against the full page set, consistent with
        // the approach used in fetchPagePermissions.
        String permissionQuery = "+" + SolrConstants.PI_TOPSTRUCT + ":" + pi
                + " +" + SolrConstants.DOCTYPE + ":" + DocType.PAGE;
        try {
            SolrDocumentList docs = DataManager.getInstance()
                    .getSearchIndex()
                    .search(query, SolrSearchIndex.MAX_HITS, null,
                            Arrays.asList(filenameField, SolrConstants.ACCESSCONDITION,
                                    SolrConstants.ORDER));
            if (docs == null || docs.isEmpty()) {
                return Collections.emptyList();
            }

            // Resolve shared context once — reused for every page to avoid repeated lookups
            List<LicenseType> licenseTypes = DataManager.getInstance().getLicenseTypeCache().getRecordLicenseTypes();
            User user = retrieveUserFromContext(request != null ? request.getSession() : null);
            String ipAddress = NetTools.getIpAddress(request);
            Optional<ClientApplication> client = ClientApplicationManager.getClientFromRequest(request);

            // Sort by ORDER to preserve canonical reading order; docs without ORDER are excluded
            // (consistent with fetchPagePermissions which skips null-ORDER documents)
            docs.sort((a, b) -> {
                Object ao = a.getFieldValue(SolrConstants.ORDER);
                Object bo = b.getFieldValue(SolrConstants.ORDER);
                if (ao == null && bo == null) {
                    return 0;
                }
                if (ao == null) {
                    return 1; // null sorts last, to be skipped
                }
                if (bo == null) {
                    return -1;
                }
                return Integer.compare(((Number) ao).intValue(), ((Number) bo).intValue());
            });

            List<String> result = new ArrayList<>();
            for (SolrDocument doc : docs) {
                if (doc.getFieldValue(SolrConstants.ORDER) == null) {
                    continue;
                }
                Object rawValue = doc.getFieldValue(filenameField);
                if (rawValue == null) {
                    continue;
                }
                // Strip any leading path component (e.g. "alto/PI/00000001.xml" → "00000001.xml")
                String bareFilename = FilenameUtils.getName(rawValue.toString());

                Collection<Object> acValues = doc.getFieldValues(SolrConstants.ACCESSCONDITION);
                Set<String> accessConditions = acValues != null
                        ? acValues.stream().map(Object::toString).collect(Collectors.toSet())
                        : Collections.emptySet();

                // checkAccessPermission is a pure in-memory evaluation — no further Solr call.
                // permissionQuery (page-scoped, without filename-field filter) is used for
                // moving-wall licence-type evaluation, consistent with fetchPagePermissions.
                AccessPermission access = checkAccessPermission(licenseTypes, accessConditions,
                        privilegeType, user, ipAddress, client, permissionQuery);
                if (access.isGranted()) {
                    result.add(bareFilename);
                }
            }
            return result;

        } catch (PresentationException | IndexUnreachableException | DAOException e) {
            logger.warn("Failed to fetch accessible file names for PI '{}', field '{}': {}",
                    pi, filenameField, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Checks whether the client may access an image (by PI + file name).
     *
     * @param request Calling HttpServiceRequest.
     * @param page physical page element whose access conditions are checked
     * @param privilegeName access privilege name to verify
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static AccessPermission checkAccessPermissionByIdentifierAndPageOrder(PhysicalElement page, String privilegeName,
            HttpServletRequest request) throws IndexUnreachableException, DAOException {
        if (page == null) {
            throw new IllegalArgumentException("page may not be null");
        }

        String query = "+" + SolrConstants.PI_TOPSTRUCT + ":" + page.getPi() + " +" + SolrConstants.ORDER + ":" + page.getOrder();
        try {
            User user = retrieveUserFromContext(request != null ? request.getSession() : null);
            return checkAccessPermission(DataManager.getInstance().getLicenseTypeCache().getRecordLicenseTypes(), page.getAccessConditions(),
                    privilegeName, user, NetTools.getIpAddress(request), ClientApplicationManager.getClientFromRequest(request), query);
        } catch (PresentationException e) {
            logger.debug(e.getMessage());
        }

        return AccessPermission.denied();

    }

    /**
     * Checks whether the client may access an image (by PI + file name).
     *
     * @param request Calling HttpServiceRequest.
     * @param pi identifier of the record
     * @param pageOrder order property of the page
     * @param privilegeName access privilege name to verify
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static AccessPermission checkAccessPermissionByIdentifierAndPageOrder(String pi, Integer pageOrder, String privilegeName,
            HttpServletRequest request) throws IndexUnreachableException, DAOException {
        if (pageOrder == null) {
            throw new IllegalArgumentException("page order may not be null");
        }

        String query = "+" + SolrConstants.PI_TOPSTRUCT + ":" + pi + " +" + SolrConstants.ORDER + ":" + pageOrder;

        try {
            User user = retrieveUserFromContext(request != null ? request.getSession() : null);

            SolrDocumentList results = DataManager.getInstance()
                    .getSearchIndex()
                    .search(query, 1, null, Collections.singletonList(SolrConstants.ACCESSCONDITION));
            if (results.size() > 0) {
                Set<String> accessConditions =
                        results.getFirst().getFieldValues(SolrConstants.ACCESSCONDITION).stream().map(Object::toString).collect(Collectors.toSet());
                return checkAccessPermission(DataManager.getInstance().getLicenseTypeCache().getRecordLicenseTypes(), accessConditions,
                        privilegeName, user, NetTools.getIpAddress(request), ClientApplicationManager.getClientFromRequest(request), query);
            }

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
     * @param request HTTP servlet request providing session and IP address
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws RecordNotFoundException
     */
    public static AccessPermission checkAccessPermissionByIdentifierAndLogId(String identifier, String logId, String privilegeName,
            HttpServletRequest request) throws IndexUnreachableException, DAOException, RecordNotFoundException {
        // logger.trace("checkAccessPermissionByIdentifierAndLogId({}, {}, {})", identifier, logId, privilegeName); //NOSONAR Debug
        if (StringUtils.isEmpty(identifier)) {
            return AccessPermission.denied();
        }

        String attributeName = IPrivilegeHolder.PREFIX_PRIV + privilegeName + "_" + identifier + "_" + logId;
        AccessPermission ret = (AccessPermission) getSessionPermission(attributeName, request != null ? request.getSession() : null);
        if (ret != null) {
            // logger.trace("Permission '{}' already in session: {}", attributeName, ret.isGranted()); //NOSONAR Debug
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
            addSessionPermission(attributeName, ret, request != null ? request.getSession() : null);

            return ret;
        } catch (PresentationException e) {
            logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
            return AccessPermission.denied();
        }
    }

    /**
     *
     * @param doc Solr document whose access conditions are checked
     * @param originalQuery original Solr query used to retrieve the document
     * @param privilegeName access privilege name to verify
     * @param request HTTP servlet request providing session and IP address
     * @return {@link AccessPermission}
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public static AccessPermission checkAccessPermissionBySolrDoc(SolrDocument doc, String originalQuery, String privilegeName,
            HttpServletRequest request) throws IndexUnreachableException, DAOException {
        // logger.trace("checkAccessPermissionBySolrDoc({}, {})", originalQuery, privilegeName); //NOSONAR Debug
        if (doc == null) {
            return AccessPermission.denied();
        }

        try {
            Set<String> requiredAccessConditions = new HashSet<>();

            Collection<Object> fieldsAccessConddition = doc.getFieldValues(SolrConstants.ACCESSCONDITION);
            if (fieldsAccessConddition != null) {
                for (Object accessCondition : fieldsAccessConddition) {
                    requiredAccessConditions.add((String) accessCondition);
                    // logger.trace("{}", accessCondition.toString()); //NOSONAR Debug
                }
            }

            User user = retrieveUserFromContext(request != null ? request.getSession() : null);
            return checkAccessPermission(DataManager.getInstance().getLicenseTypeCache().getRecordLicenseTypes(), requiredAccessConditions,
                    privilegeName, user, NetTools.getIpAddress(request), ClientApplicationManager.getClientFromRequest(request), originalQuery);
        } catch (PresentationException e) {
            logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
        }

        return AccessPermission.denied();
    }

    /**
     * Checks whether the current users has the given access permissions each element of the record with the given identifier.
     *
     * @param identifier persistent identifier of the record
     * @param privilegeName access privilege name to verify
     * @param request HTTP servlet request providing session and IP address
     * @return Map with true/false for each LOGID
     * @should fill map completely
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, AccessPermission> checkAccessPermissionByIdentiferForAllLogids(String identifier, String privilegeName,
            HttpServletRequest request) throws IndexUnreachableException, DAOException {
        logger.trace("checkAccessPermissionByIdentiferForAllLogids({}, {})", identifier, privilegeName);
        HttpSession session = request != null ? request.getSession() : null;

        String attributeName = IPrivilegeHolder.PREFIX_PRIV + privilegeName + "_" + identifier;
        Map<String, AccessPermission> ret = (Map<String, AccessPermission>) getSessionPermission(attributeName, session);
        if (ret != null) {
            return ret;
        }

        ret = new HashMap<>();
        if (StringUtils.isNotEmpty(identifier)) {
            String query = new StringBuilder().append('+')
                    .append(SolrConstants.PI_TOPSTRUCT)
                    .append(":\"")
                    .append(identifier)
                    .append("\" +")
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
                    User user = retrieveUserFromContext(session);

                    //                    long start = System.nanoTime();
                    List<LicenseType> nonOpenAccessLicenseTypes = DataManager.getInstance().getLicenseTypeCache().getRecordLicenseTypes();
                    for (SolrDocument doc : results) {
                        Set<String> requiredAccessConditions = new HashSet<>();
                        Collection<Object> fieldsAccessConddition = doc.getFieldValues(SolrConstants.ACCESSCONDITION);
                        if (fieldsAccessConddition != null) {
                            for (Object accessCondition : fieldsAccessConddition) {
                                requiredAccessConditions.add((String) accessCondition);
                                // logger.trace("{}", accessCondition.toString()); //NOSONAR Debug
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
        addSessionPermission(attributeName, ret, session);

        logger.trace("Found access permisstions for {} elements.", ret.size());
        return ret;
    }

    /**
     * Checks if the record with the given identifier should allow access to the given request.
     *
     * @param identifier The PI of the work to check
     * @param request The HttpRequest which may provide a {@link jakarta.servlet.http.HttpSession} to store the access map
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static AccessPermission checkContentFileAccessPermission(String identifier, HttpServletRequest request)
            throws IndexUnreachableException, DAOException {
        // logger.trace("checkContentFileAccessPermission: {}", identifier); //NOSONAR Debug
        HttpSession session = request != null ? request.getSession() : null;
        String attributeName = IPrivilegeHolder.PREFIX_PRIV + IPrivilegeHolder.PRIV_DOWNLOAD_ORIGINAL_CONTENT + "_" + identifier;
        AccessPermission ret = (AccessPermission) getSessionPermission(attributeName, session);
        if (ret != null) {
            // logger.trace("Permission for '{}' already in session: {}", attributeName, ret.isGranted()); //NOSONAR Debug
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
                                // logger.debug(accessCondition.toString()); //NOSONAR Debug
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
        addSessionPermission(attributeName, ret, session);

        //return only the access status for the relevant files
        return ret;
    }

    /**
     * Checks whether the client may access an image (by image URN).
     *
     * @param imageUrn Image URN.
     * @param request Calling HttpServiceRequest.
     * @param privilegeName access privilege name to verify
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static AccessPermission checkAccessPermissionByImageUrn(String imageUrn, String privilegeName, HttpServletRequest request)
            throws IndexUnreachableException, DAOException {
        logger.trace("checkAccessPermissionByImageUrn({}, {}, {}, {})", imageUrn, privilegeName,
                request != null ? request.getAttributeNames() : "null"); //NOSONAR Debug
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
                        // logger.debug((String) accessCondition); //NOSONAR Debug
                    }
                }
            }

            User user = retrieveUserFromContext(request != null ? request.getSession() : null);
            return checkAccessPermission(DataManager.getInstance().getLicenseTypeCache().getRecordLicenseTypes(), requiredAccessConditions,
                    privilegeName, user, NetTools.getIpAddress(request), ClientApplicationManager.getClientFromRequest(request), query);
        } catch (PresentationException e) {
            logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
            return AccessPermission.denied();
        }
    }

    /**
     * checkAccessPermission.
     *
     * @param requiredAccessConditions set of access condition names to satisfy
     * @param privilegeName access privilege name to verify
     * @param query Solr query describing the resource in question
     * @param request HTTP servlet request providing session and IP address
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should return true if required access conditions empty
     * @should return true if ip range allows access
     * @should return true if required access conditions contain only open access
     * @should return true if all license types allow privilege by default
     * @should return false if not all license types allow privilege by default
     * @should return true if ip range allows access to all conditions
     * @should not return true if no ip range matches
     */
    public static AccessPermission checkAccessPermission(Set<String> requiredAccessConditions, String privilegeName, String query,
            HttpServletRequest request) throws IndexUnreachableException, PresentationException, DAOException {
        User user = retrieveUserFromContext(request != null ? request.getSession() : null);
        return checkAccessPermission(DataManager.getInstance().getLicenseTypeCache().getRecordLicenseTypes(), requiredAccessConditions, privilegeName, user,
                NetTools.getIpAddress(request), ClientApplicationManager.getClientFromRequest(request), query);
    }

    /**
     * Checks access permission for the given image and puts the permission status into the corresponding session map.
     *
     * @param session HTTP session for caching permission results
     * @param pi persistent identifier of the record
     * @param contentFileName name of the image file to check
     * @param ipAddress client IP address
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static AccessPermission checkAccessPermissionForImage(HttpSession session, String pi, String contentFileName, String ipAddress)
            throws IndexUnreachableException, DAOException {
        // logger.trace("checkAccessPermissionForImage: {}/{}", pi, contentFileName); //NOSONAR Debug
        return checkAccessPermissionForImage(session, pi, contentFileName, ipAddress, null);
    }

    /**
     * Checks access permission for the given image and puts the permission status into the corresponding session map.
     *
     * @param session HTTP session for caching permission results
     * @param pi persistent identifier of the record
     * @param contentFileName name of the image file to check
     * @param ipAddress client IP address
     * @param user the user requesting permission. If null, it is fetchted from the jsf context if it exists
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static AccessPermission checkAccessPermissionForImage(HttpSession session, String pi, String contentFileName, String ipAddress, User user)
            throws IndexUnreachableException, DAOException {
        // logger.trace("checkAccessPermissionForImage: {}/{}", pi, contentFileName); //NOSONAR Debug
        return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(session, pi, contentFileName, IPrivilegeHolder.PRIV_VIEW_IMAGES, ipAddress,
                user);
    }

    /**
     * Checks access permission for the given thumbnail and puts the permission status into the corresponding session map.
     *
     * @param session HTTP session for caching permission results
     * @param pi persistent identifier of the record
     * @param contentFileName name of the thumbnail file to check
     * @param ipAddress client IP address
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static AccessPermission checkAccessPermissionForThumbnail(HttpSession session, String pi, String contentFileName, String ipAddress)
            throws IndexUnreachableException, DAOException {
        return checkAccessPermissionForThumbnail(session, pi, contentFileName, ipAddress, null);
    }

    /**
     * Checks access permission for the given thumbnail and puts the permission status into the corresponding session map.
     *
     * @param session HTTP session for caching permission results
     * @param pi persistent identifier of the record
     * @param contentFileName name of the thumbnail file to check
     * @param ipAddress client IP address
     * @param user the user requesting permission. If null, it is fetchted from the jsf context if it exists
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static AccessPermission checkAccessPermissionForThumbnail(HttpSession session, String pi, String contentFileName, String ipAddress,
            User user)
            throws IndexUnreachableException, DAOException {
        return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(session, pi, contentFileName,
                IPrivilegeHolder.PRIV_VIEW_THUMBNAILS, ipAddress, user);
    }

    /**
     * Checks access permission for the given image and puts the permission status into the corresponding session map.
     *
     * @param request HTTP servlet request providing session and IP address
     * @param page physical page element to check PDF download permission for
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static AccessPermission checkAccessPermissionForPagePdf(HttpServletRequest request, PhysicalElement page)
            throws IndexUnreachableException, DAOException {
        if (page == null) {
            throw new IllegalArgumentException("page may not be null");
        }
        // logger.trace("checkAccessPermissionForPagePdf: {}/{}", page.getPi(), page.getOrder()); //NOSONAR Debug
        return checkAccessPermissionByIdentifierAndPageOrder(page, IPrivilegeHolder.PRIV_DOWNLOAD_PAGE_PDF, request);
    }

    /**
     * checkAccessPermissionByIdentifierAndFilePathWithSessionMap.
     *
     * @param request HTTP servlet request providing session and IP address
     * @param filePath FILENAME_ALTO or FILENAME_FULLTEXT value
     * @param privilegeType access privilege type to verify
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static AccessPermission checkAccessPermissionByIdentifierAndFilePathWithSessionMap(HttpServletRequest request, String filePath,
            String privilegeType) throws IndexUnreachableException, DAOException {
        return checkAccessPermissionByIdentifierAndFilePathWithSessionMap(request, filePath, privilegeType, null);
    }

    /**
     * checkAccessPermissionByIdentifierAndFilePathWithSessionMap.
     *
     * @param request HTTP servlet request providing session and IP address
     * @param filePath FILENAME_ALTO or FILENAME_FULLTEXT value
     * @param privilegeType access privilege type to verify
     * @param user the user requesting permission. If null, it is fetchted from the jsf context if it exists
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static AccessPermission checkAccessPermissionByIdentifierAndFilePathWithSessionMap(HttpServletRequest request, String filePath,
            String privilegeType, User user) throws IndexUnreachableException, DAOException {
        if (filePath == null) {
            throw new IllegalArgumentException("filePath may not be null");
        }
        String[] filePathSplit = filePath.split("/");
        if (filePathSplit.length != 3) {
            throw new IllegalArgumentException("Illegal filePath value: " + filePath);
        }

        return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request != null ? request.getSession() : null, filePathSplit[1],
                filePathSplit[2], privilegeType, NetTools.getIpAddress(request), user);
    }

    /**
     * Checks access permission of the given privilege type for the given image and puts the permission status into the corresponding session map.
     *
     * @param session HTTP session for caching permission results
     * @param pi persistent identifier of the record
     * @param contentFileName name of the content file to check
     * @param privilegeType access privilege type to verify
     * @param ipAddress client IP address
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @SuppressWarnings("unchecked")
    public static AccessPermission checkAccessPermissionByIdentifierAndFileNameWithSessionMap(HttpSession session, String pi,
            String contentFileName, String privilegeType, String ipAddress) throws IndexUnreachableException, DAOException {
        return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(session, pi, contentFileName, privilegeType, ipAddress, null);
    }

    /**
     * Checks access permission of the given privilege type for the given image and puts the permission status into the corresponding session map.
     *
     * @param session HTTP session for caching permission results
     * @param pi persistent identifier of the record
     * @param contentFileName name of the content file to check
     * @param privilegeType access privilege type to verify
     * @param ipAddress client IP address
     * @param user the {@link User} requesting access. May be null in which case the the method will attempt to retrieve the user from the
     *            {@link UserBean}, given an existing jsfContext
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should remove PRIV_ attributes of the previous pi on pi change
     */
    @SuppressWarnings("unchecked")
    public static AccessPermission checkAccessPermissionByIdentifierAndFileNameWithSessionMap(HttpSession session, String pi,
            String contentFileName, String privilegeType, String ipAddress, User user) throws IndexUnreachableException, DAOException {
        // logger.trace("checkAccessPermissionByIdentifierAndFileNameWithSessionMap: {}, {}, {}", pi, contentFileName, privilegeType); //NOSONAR Debug
        if (privilegeType == null) {
            throw new IllegalArgumentException("privilegeType may not be null");
        }
        // logger.debug("session id: " + session.getId()); //NOSONAR Debug
        // Session persistent permission check: Servlet-local method.
        String attributeName = IPrivilegeHolder.PREFIX_PRIV + privilegeType + "_" + pi + "_" + contentFileName;
        // logger.trace("Checking session attribute: {}", attributeName); //NOSONAR Debug
        Map<String, AccessPermission> permissions = (Map<String, AccessPermission>) getSessionPermission(attributeName, session);
        if (permissions == null) {
            permissions = new HashMap<>();
            // logger.trace("Session attribute not found, creating new"); //NOSONAR Debug
        }
        // logger.debug("Permissions found, " + permissions.size() + " items."); //NOSONAR Debug
        // new pi -> remove PRIV_ caches tied to the previous pi so they do not accumulate
        if (session != null && !pi.equals(session.getAttribute("currentPi"))) {
            // Previously only the current attributeName was removed, which left stale
            // PRIV_VIEW_IMAGES_<oldPi>_*, PRIV_DOWNLOAD_PDF_<oldPi>_* etc. lingering in the
            // session (observed as 3 million attributes on ZLB's crawler session). Capture the
            // previous pi *before* overwriting currentPi, then remove only PRIV_ keys that refer
            // to the old pi — this preserves caches for other pis so multi-tab users do not pay
            // a Solr roundtrip on every tab switch. refs #27880
            String oldPi = (String) session.getAttribute("currentPi");
            session.setAttribute("currentPi", pi);
            if (oldPi != null) {
                removePrivAttributesForPi(session, oldPi);
            }
            session.removeAttribute(attributeName);
            permissions = new HashMap<>();
            // logger.trace("PI has changed, old-pi PRIV_ attributes purged."); //NOSONAR Debug
        }
        String key = new StringBuilder(pi).append('_').append(contentFileName).toString();
        // pi already checked -> look in the session
        // logger.debug("permissions key: {}: {}", key, permissions.get(key)); //NOSONAR Debug

        if (permissions.containsKey(key) && permissions.get(key) != null) {
            return permissions.get(key);
            // logger.trace("Access ({}) previously checked and is {} for '{}/{}' (Session ID {})", privilegeType,
            // ret.isGranted(), pi, contentFileName, session.getId()); //NOSONAR Debug
        }
        // TODO check for all images and save to map
        Map<String, AccessPermission> accessMap =
                checkAccessPermissionByIdentifierAndFileName(pi, contentFileName, privilegeType, ipAddress, session, user);
        for (Entry<String, AccessPermission> entry : accessMap.entrySet()) {
            String newKey = new StringBuilder(pi).append('_').append(entry.getKey()).toString();
            AccessPermission pageAccess = entry.getValue();
            permissions.put(newKey, pageAccess);
        }

        // Add permission check outcome to user session
        addSessionPermission(attributeName, permissions, session);

        return permissions.get(key) != null ? permissions.get(key) : AccessPermission.denied();
        // logger.debug("Access ({}) not yet checked for '{}/{}', access is {}", privilegeType, pi, contentFileName, ret.isGranted()); //NOSONAR Deb
    }

    /**
     *
     * @param request HTTP servlet request providing session and IP address
     * @param page {@link CMSPage} to check
     * @return {@link AccessPermission}
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public static AccessPermission checkAccessPermissionForCmsPage(HttpServletRequest request, CMSPage page)
            throws DAOException, IndexUnreachableException, PresentationException {
        if (page == null) {
            throw new IllegalArgumentException("page may not be null");
        }

        if (StringUtils.isEmpty(page.getAccessCondition())) {
            return AccessPermission.granted();
        }

        User user = retrieveUserFromContext(request != null ? request.getSession() : null);
        if (user != null && user.isSuperuser()) {
            logger.trace("Access granted to admin.");
            return AccessPermission.granted();
        }

        LicenseType licenseType = DataManager.getInstance().getLicenseTypeCache().getLicenseType(page.getAccessCondition());
        if (licenseType == null) {
            logger.trace("LicenseType '{}' not configured, access denied.", page.getAccessCondition());
            return AccessPermission.denied();
        }

        return checkAccessPermission(Collections.singletonList(licenseType), Collections.singleton(page.getAccessCondition()),
                IPrivilegeHolder.PRIV_VIEW_CMS, user, NetTools.getIpAddress(request), ClientApplicationManager.getClientFromRequest(request), null);
    }

    /**
     * Base method for checking access permissions of various types.
     *
     * @param allLicenseTypes all configured license types to evaluate
     * @param requiredAccessConditions Set of access condition names to satisfy (one suffices).
     * @param privilegeName The particular privilege to check.
     * @param user Logged in user.
     * @param remoteAddress client IP address string
     * @param client optional client application making the request
     * @param query Solr query describing the resource in question.
     * @return {@link AccessPermission}
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
        // logger.trace("checkAccessPermission({},{})", requiredAccessConditions, privilegeName); //NOSONAR Debug

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

        Set<String> useAccessConditions = HashSet.newHashSet(relevantLicenseTypes.size());

        // If all relevant license types allow the requested privilege by default, allow access
        boolean licenseTypeAllowsPriv = true;
        boolean accessTicketRequired = false;
        boolean redirect = false;
        String redirectUrl = null;
        List<LicenseType> licenseTypesWithCustomAccessDeniedInfo = new ArrayList<>();
        // Check whether *all* relevant license types allow the requested privilege by default. As soon as one doesn't, set to false.
        for (LicenseType licenseType : relevantLicenseTypes) {
            useAccessConditions.add(licenseType.getName());
            if (licenseType.isHasCustomPlaceholderInfo()) {
                licenseTypesWithCustomAccessDeniedInfo.add(licenseType);
                // logger.trace("Considering access denied image URI map from LicenseType '{}'.", licenseType.getName());
            }
            if (licenseType.isRedirect()) {
                redirect = true;
                redirectUrl = licenseType.getRedirectUrl();
            }
            if (licenseType.isAccessTicketRequired()) {
                accessTicketRequired = true;
            }
            if (!licenseType.getPrivileges().contains(privilegeName) && !licenseType.isOpenAccess()
                    && !licenseType.isRestrictionsExpired(query)) {
                logger.trace("LicenseType '{}' doesn't allow the action '{}' by default.", licenseType.getName(), privilegeName); //NOSONAR Debug
                licenseTypeAllowsPriv = false;
            }
        }
        if (licenseTypeAllowsPriv) {
            // logger.trace("Privilege '{}' is allowed by default in all license types.", privilegeName); //NOSONAR Debug
            return AccessPermission.granted()
                    .setRedirect(redirect)
                    .setRedirectUrl(redirectUrl)
                    .setAccessTicketRequired(accessTicketRequired);
        } else if (isFreeOpenAccess(useAccessConditions, relevantLicenseTypes)) {
            logger.trace("Privilege '{}' is OpenAccess", privilegeName);
            return AccessPermission.granted().setRedirect(redirect).setRedirectUrl(redirectUrl).setAccessTicketRequired(accessTicketRequired);
        } else {
            // Check IP range
            IpRange useIpRange = null;
            if (StringUtils.isNotEmpty(remoteAddress)) {
                if (NetTools.isIpAddressLocalhost(remoteAddress)
                        && DataManager.getInstance().getConfiguration().isFullAccessForLocalhost()) {
                    logger.trace("Access granted to localhost");
                    return AccessPermission.granted();
                }
                // Check whether the requested privilege is allowed to this IP range (for all access conditions)
                for (IpRange ipRange : DataManager.getInstance().getIpRangeCache().getAllIpRanges()) {
                    if (ipRange.matchIp(remoteAddress)) {
                        useIpRange = ipRange;
                        AccessPermission access = ipRange.canSatisfyAllAccessConditions(useAccessConditions, privilegeName, null);
                        if (access.isGranted()) {
                            logger.trace("Access granted to {} via IP range {}", remoteAddress, ipRange.getName());
                            access.checkSecondaryAccessRequirement(useAccessConditions, privilegeName, user, ipRange,
                                    client.orElse(null));
                            return access.setAccessTicketRequired(accessTicketRequired);
                        }
                    }
                }
            }

            // If not within an allowed IP range, check the current user's satisfied access conditions
            if (user != null) {
                AccessPermission access =
                        user.canSatisfyAllAccessConditions(useAccessConditions, privilegeName, null).setAccessTicketRequired(accessTicketRequired);
                if (access.isGranted()) {
                    access.checkSecondaryAccessRequirement(useAccessConditions, privilegeName, user, useIpRange, client.orElse(null));
                    return access;
                }
            }

            //check clientApplication
            if (client.map(c -> c.mayLogIn(remoteAddress)).orElse(false)) {
                //check if specific client matches access conditions
                if (client.isPresent()) {
                    AccessPermission access = client.get()
                            .canSatisfyAllAccessConditions(useAccessConditions, privilegeName, null)
                            .setAccessTicketRequired(accessTicketRequired);
                    if (access.isGranted()) {
                        access.checkSecondaryAccessRequirement(useAccessConditions, privilegeName, user, useIpRange, client.orElse(null));
                        return access;
                    }
                }
                //check if access condition match for all clients
                ClientApplication allClients = DataManager.getInstance().getClientManager().getAllClientsFromDatabase();
                if (allClients != null) {
                    AccessPermission access =
                            allClients.canSatisfyAllAccessConditions(useAccessConditions, privilegeName, null)
                                    .setAccessTicketRequired(accessTicketRequired);
                    if (access.isGranted()) {
                        access.checkSecondaryAccessRequirement(useAccessConditions, privilegeName, user, useIpRange, client.orElse(null));
                        return access;
                    }
                }
            }
        }

        // TODO Determine "best" set configuration, if several LicenseTypes contain custom config?
        Map<String, AccessDeniedInfoConfig> imagePlaceholders = null;
        if (!licenseTypesWithCustomAccessDeniedInfo.isEmpty()) {
            imagePlaceholders = licenseTypesWithCustomAccessDeniedInfo.get(0).getImagePlaceholdersAsMap();
            logger.trace("Using image placeholder configuration from LicenseType '{}'.", licenseTypesWithCustomAccessDeniedInfo.get(0).getName());
        }

        return AccessPermission.denied().setAccessDeniedPlaceholderInfo(imagePlaceholders);
    }

    /**
     * Check whether the requiredAccessConditions consist only of the {@link io.goobi.viewer.solr.SolrConstants#OPEN_ACCESS_VALUE OPENACCESS}
     * condition and OPENACCESS is not contained in allLicenseTypes. In this and only this case can we safely assume that everything is permitted. If
     * OPENACCESS is in the database then it likely contains some access restrictions which need to be checked
     *
     * @param requiredAccessConditions set of access condition names from the Solr document
     * @param allLicenseTypes all license types relevant for access. If null, the DAO is checked if it contains the OPENACCESS condition
     * @return true if we can savely assume that we have entirely open access
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static boolean isFreeOpenAccess(Set<String> requiredAccessConditions, Collection<LicenseType> allLicenseTypes) throws DAOException {
        if (requiredAccessConditions.size() != 1) {
            return false;
        }

        boolean containsOpenAccess =
                requiredAccessConditions.stream().anyMatch(SolrConstants.OPEN_ACCESS_VALUE::equalsIgnoreCase);
        boolean openAccessIsConfiguredLicenceType =
                allLicenseTypes == null ? DataManager.getInstance().getLicenseTypeCache().getLicenseType(SolrConstants.OPEN_ACCESS_VALUE) != null
                        : allLicenseTypes.stream().anyMatch(license -> SolrConstants.OPEN_ACCESS_VALUE.equalsIgnoreCase(license.getName()));
        return containsOpenAccess && !openAccessIsConfiguredLicenceType;
    }

    /**
     * Filters the given list of license types my removing those that have Solr query conditions that do not match the given identifier.
     *
     * @param allLicenseTypes all configured license types to filter
     * @param requiredAccessConditions set of required access condition names
     * @param query Solr query describing the resource, used for moving wall checks
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
            if (licenseType.isMovingWall() && StringUtils.isEmpty(query)) {
                logger.warn("License type '{}' is in 'moving wall' mode, but no query was passed to check for relevance.", licenseType.getName());
            }
            if (licenseType.isMovingWall() && StringUtils.isNotEmpty(query)
                    && !Boolean.TRUE.equals(licenseType.getRestrictionsExpired().get(query))) {
                StringBuilder sbQuery = new StringBuilder().append("+(")
                        .append(query)
                        .append(") +")
                        .append(licenseType.getFilterQueryPart().trim())
                        .append(" -(")
                        .append(SearchHelper.getMovingWallQuery())
                        .append(')');
                logger.trace("License relevance query: {}", //NOSONAR Debug
                        StringTools.stripPatternBreakingChars(StringTools.stripPatternBreakingChars(sbQuery.toString()))); //NOSONAR Debug
                if (DataManager.getInstance().getSearchIndex().getHitCount(sbQuery.toString()) == 0) {
                    // logger.trace("LicenseType '{}' does not apply to resource described by '{}' due to the moving wall condition.", //NOSONAR Debug
                    // licenseType.getName(), StringTools.stripPatternBreakingChars(query)); //NOSONAR Debug
                    if (licenseType.isMovingWall()) {
                        // Moving wall license type allow everything if the condition query doesn't match
                        // logger.trace(
                        // "License type '{}' is moving wall and its condition query doesn't match record query '{}'. All restrictions lifted.",
                        // licenseType.getName(), StringTools.stripPatternBreakingChars(query)); //NOSONAR Debug
                        licenseType.getRestrictionsExpired().put(query, true);
                    } else {
                        continue;
                    }
                }
                // logger.trace("LicenseType '{}' applies to resource '{}' due to moving wall restrictions.", licenseType.getName(), //NOSONAR Debug
                // StringTools.stripPatternBreakingChars(query)); //NOSONAR Debug
            }

            ret.add(licenseType);
        }

        return ret;

    }

    /**
     *
     * @param pi persistent identifier of the record
     * @return Number of allowed downloads for given pi; 100 of no value set
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws RecordNotFoundException
     * @should throw RecordNotFoundException if record not found
     * @should return 100 if record has no quota value
     * @should return 100 if record open access
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

        List<LicenseType> relevantLicenseTypes = DataManager.getInstance().getLicenseTypeCache().getLicenseTypes(requiredAccessConditions);
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
     * @param accessConditions list of access condition strings to check
     * @return true if any license type for the given list of access conditions has concurrent views limit enabled; false otherwise
     * @throws DAOException
     * @should return false if access conditions null or empty
     * @should return true if any license type has limit enabled
     */
    public static boolean isConcurrentViewsLimitEnabledForAnyAccessCondition(List<String> accessConditions) throws DAOException {
        if (accessConditions == null || accessConditions.isEmpty()) {
            return false;
        }

        List<LicenseType> licenseTypes = DataManager.getInstance().getLicenseTypeCache().getLicenseTypes(accessConditions);
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
     * user belongs to or to the given ipAddress, whether or not the given user exists.
     * 
     * @param user optional logged-in user to match against licenses
     * @param ipAddress client IP address used for IP range matching
     * @param type license type to query licenses for
     * @param dao DAO instance used to retrieve licenses and IP ranges
     * @return List<License>
     * @throws DAOException
     * @should return empty collection for given input
     */
    public static List<License> getApplyingLicenses(Optional<User> user, String ipAddress, LicenseType type, IDAO dao) throws DAOException {
        List<License> licenses = dao.getLicenses(type);
        List<UserGroup> userGroups = user.map(User::getAllUserGroups).orElse(Collections.emptyList());
        List<IpRange> ipRangesApplyingToGivenIp =
                DataManager.getInstance().getIpRangeCache().getAllIpRanges().stream().filter(range -> range.matchIp(ipAddress)).toList();

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
     * @param attributeName session attribute key for the permission entry
     * @param session HTTP session to look up the attribute in
     * @return Object found in session; null otherwise
     */
    public static Object getSessionPermission(String attributeName, HttpSession session) {
        if (session == null) {
            return null;
        }

        return session.getAttribute(attributeName);
    }

    /**
     * 
     * @param attributeName session attribute key under which the value is stored
     * @param attributeValue permission value to store in the session
     * @param session HTTP session to store the attribute in
     * @return true if successful; false otherwise
     * @should return false for given input
     */
    public static boolean addSessionPermission(String attributeName, Object attributeValue, HttpSession session) {
        // logger.trace("addSessionPermission: {}", attributeName); //NOSONAR Debug
        if (session == null) {
            return false;
        }

        // Guard against sessions that were invalidated concurrently (e.g. session timeout during TOC build)
        try {
            session.setAttribute(attributeName, attributeValue);
        } catch (IllegalStateException e) {
            logger.debug("Cannot store session permission '{}': session has already been invalidated", attributeName);
            return false;
        }
        return true;
    }

    /**
     * Removes privileges saved in the user session.
     *
     * @param session HTTP session whose permission attributes are cleared
     * @return Number of removed session attributes
     */
    public static int clearSessionPermissions(HttpSession session) {
        if (session == null) {
            return 0;
        }

        // Guard against sessions that were invalidated concurrently
        Enumeration<String> attributeNames;
        try {
            attributeNames = session.getAttributeNames();
        } catch (IllegalStateException e) {
            logger.debug("Cannot clear session permissions: session has already been invalidated");
            return 0;
        }

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
                try {
                    session.removeAttribute(attribute);
                    ret++;
                    logger.trace("Removed session attribute: {}", attribute);
                } catch (IllegalStateException e) {
                    logger.debug("Cannot remove session attribute '{}': session has already been invalidated", attribute);
                }
            }
        }

        return ret;
    }

    /**
     * Removes all PRIV_* session attributes whose key references the given pi. The key schemes
     * used in this class are
     * <ul>
     *   <li>{@code PRIV_<privilegeType>_<pi>_<fileName>} (see line 1113),</li>
     *   <li>{@code PRIV_<privilegeName>_<identifier>} (see line 756),</li>
     *   <li>{@code PRIV_DOWNLOAD_ORIGINAL_CONTENT_<identifier>} (see line 828).</li>
     * </ul>
     * The middle-match catches the first scheme, the suffix-match catches the other two.
     *
     * <p>Package-private so the unit test can exercise it in isolation from the Solr/DB-backed
     * permission pipeline. refs #27880
     *
     * @param session HTTP session whose attribute table is inspected
     * @param pi persistent identifier whose cached PRIV_* attributes should be removed
     * @return number of attributes removed
     * @should remove only PRIV_ attributes of the given pi
     */
    static int removePrivAttributesForPi(HttpSession session, String pi) {
        if (session == null || pi == null) {
            return 0;
        }
        Enumeration<String> attributeNames;
        try {
            attributeNames = session.getAttributeNames();
        } catch (IllegalStateException e) {
            return 0;
        }
        Set<String> toRemove = new HashSet<>();
        String middle = "_" + pi + "_";
        String suffix = "_" + pi;
        while (attributeNames.hasMoreElements()) {
            String name = attributeNames.nextElement();
            if (name.startsWith(IPrivilegeHolder.PREFIX_PRIV)
                    && (name.contains(middle) || name.endsWith(suffix))) {
                toRemove.add(name);
            }
        }
        for (String name : toRemove) {
            try {
                session.removeAttribute(name);
            } catch (IllegalStateException e) {
                logger.debug("Cannot remove session attribute '{}': session has already been invalidated", name);
            }
        }
        return toRemove.size();
    }

    /**
     * @param pi persistent identifier of the record
     * @param fileName content file name to check access for
     * @param privilegeName access privilege name to verify
     * @return {@link AccessPermission}
     * @throws DAOException
     * @throws IndexUnreachableException
     */
    public static AccessPermission getAccessPermission(String pi, String fileName, String privilegeName)
            throws IndexUnreachableException, DAOException {
        return getAccessPermission(pi, fileName, privilegeName, null);
    }

    /**
     * @param pi persistent identifier of the record
     * @param fileName content file name to check access for
     * @param privilegeName access privilege name to verify
     * @param user The user requesting access. If null it is retrieved from the jsfContext if available
     * @return {@link AccessPermission}
     * @throws DAOException
     * @throws IndexUnreachableException
     */
    public static AccessPermission getAccessPermission(String pi, String fileName, String privilegeName, User user)
            throws IndexUnreachableException, DAOException {
        HttpServletRequest request = null;
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getExternalContext() != null) {
            request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        }
        return AccessConditionUtils
                .checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request != null ? request.getSession() : null, pi, fileName,
                        privilegeName, NetTools.getIpAddress(request), user);
    }
}