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
package io.goobi.viewer.model.transkribus;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.model.job.JobStatus;

/**
 * <p>
 * TranskribusUtils class.
 * </p>
 */
public class TranskribusUtils {

    private static final Logger logger = LogManager.getLogger(TranskribusUtils.class);

    /** Constant <code>TRANSRIBUS_REST_URL="https://transkribus.eu/TrpServer/rest/"</code> */
    public static final String TRANSRIBUS_REST_URL = "https://transkribus.eu/TrpServer/rest/";
    /** Constant <code>TRANSRIBUS_REST_TESTING_URL="https://transkribus.eu/TrpServerTesting"{trunked}</code> */
    public static final String TRANSRIBUS_REST_TESTING_URL = "https://transkribus.eu/TrpServerTesting/rest/";
    private static final String URLPART_CHECK_JOB_STATUS = "jobs/{id}";
    private static final String URLPART_COLLECTION_LIST = "collections/listByName";
    private static final String URLPART_CREATE_COLLECTION = "collections/createCollection";
    private static final String URLPART_CREATE_DOC_FROM_METS_URL = "collections/{collId}/createDocFromMetsUrl";
    private static final String URLPART_GRANT_COLLECTION_PRIVS = "collections/{collId}/addOrModifyUserInCollection";
    private static final String URLPART_LOGIN = "auth/login";

    private static final String ERROR_BASEURL_NULL = "baseUrl may not be null";

    /** Private constructor. */
    private TranskribusUtils() {
        //
    }

    /**
     * <p>
     * ingestRecord.
     * </p>
     *
     * @param restApiUrl a {@link java.lang.String} object.
     * @param userSession a {@link io.goobi.viewer.model.transkribus.TranskribusSession} object.
     * @param pi a {@link java.lang.String} object.
     * @param metsResolverUrlRoot Root of the METS resolver URL (without the identifier).
     * @return a {@link io.goobi.viewer.model.transkribus.TranskribusJob} object.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.HTTPException if any.
     * @throws org.jdom2.JDOMException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static TranskribusJob ingestRecord(String restApiUrl, TranskribusSession userSession, String pi, String metsResolverUrlRoot)
            throws IOException, HTTPException, JDOMException, DAOException {
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }
        if (userSession == null) {
            throw new IllegalArgumentException("userSession may not be null");
        }
        if (metsResolverUrlRoot == null) {
            throw new IllegalArgumentException("metsResolverUrl may not be null");
        }

        if (!DataManager.getInstance().getConfiguration().isTranskribusEnabled()) {
            return null;
        }

        // Check and create the user's collection for this viewer instance
        String userCollectionId = getCollectionId(restApiUrl, userSession.getSessionId(),
                DataManager.getInstance().getConfiguration().getTranskribusDefaultCollection());
        if (userCollectionId == null) {
            userCollectionId = createCollection(restApiUrl, userSession.getSessionId(),
                    DataManager.getInstance().getConfiguration().getTranskribusDefaultCollection());
            if (userCollectionId == null) {
                logger.error("Could not create add collection '{}' to the user acccount '{}'.",
                        DataManager.getInstance().getConfiguration().getTranskribusDefaultCollection(), userSession.getUserName());
                return null;
            }
        }

        // Get this viewer's Transkribus session metadata
        TranskribusSession viewerSession = login(restApiUrl, DataManager.getInstance().getConfiguration().getTranskribusUserName(),
                DataManager.getInstance().getConfiguration().getTranskribusPassword());
        logger.trace("{} - {}", DataManager.getInstance().getConfiguration().getTranskribusUserName(),
                DataManager.getInstance().getConfiguration().getTranskribusPassword());
        if (viewerSession == null) {
            logger.error("No viewer session");
            return null;
        }

        // Check and create the default viewer instance collection
        String viewerCollectionId =
                getCollectionId(restApiUrl, viewerSession.getSessionId(),
                        DataManager.getInstance().getConfiguration().getTranskribusDefaultCollection());
        if (viewerCollectionId == null) {
            viewerCollectionId =
                    createCollection(restApiUrl, viewerSession.getSessionId(),
                            DataManager.getInstance().getConfiguration().getTranskribusDefaultCollection());
            if (viewerCollectionId == null) {
                logger.error("Could not create the default collection '{}' for the viewer instance.",
                        DataManager.getInstance().getConfiguration().getTranskribusDefaultCollection());
                return null;
            }
        }

        // Grant editor privs to the viewer account for the user's collection
        grantCollectionPrivsToViewer(restApiUrl, userSession.getSessionId(), userCollectionId, viewerSession.getUserId(), false);

        // Create new doc via the METS resolver URL
        TranskribusJob job = ingestRecordToCollections(restApiUrl, userSession, pi,
                metsResolverUrlRoot.endsWith(pi) ? metsResolverUrlRoot : metsResolverUrlRoot + pi, userCollectionId, viewerCollectionId);

        // Add new job to the DB and schedule periodical checks
        DataManager.getInstance().getDao().addTranskribusJob(job);

        return job;
    }

    /**
     * <p>
     * login.
     * </p>
     *
     * @param baseUrl a {@link java.lang.String} object.
     * @param userName a {@link java.lang.String} object.
     * @param password a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.transkribus.TranskribusSession} object.
     * @throws java.io.IOException if any.
     * @throws org.jdom2.JDOMException if any.
     * @throws io.goobi.viewer.exceptions.HTTPException if any.
     */
    public static TranskribusSession login(String baseUrl, String userName, String password) throws IOException, JDOMException, HTTPException {
        if (baseUrl == null) {
            throw new IllegalArgumentException(ERROR_BASEURL_NULL);
        }
        if (userName == null) {
            throw new IllegalArgumentException("userName may not be null");
        }

        Document doc = auth(baseUrl, userName, password);
        if (doc != null && doc.getRootElement() != null) {
            return new TranskribusSession(doc.getRootElement().getChildText("userId"), doc.getRootElement().getChildText("userName"),
                    doc.getRootElement().getChildText("sessionId"));
        }

        logger.error("Transkribus login failed for account '{}'", userName);
        return null;
    }

    /**
     * <p>
     * auth.
     * </p>
     *
     * @param baseUrl a {@link java.lang.String} object.
     * @param userName a {@link java.lang.String} object.
     * @param password a {@link java.lang.String} object.
     * @return JDOM object containing the API response
     * @should auth correctly
     * @throws java.io.IOException if any.
     * @throws org.jdom2.JDOMException if any.
     */
    public static Document auth(String baseUrl, String userName, String password) throws IOException, JDOMException {
        if (baseUrl == null) {
            throw new IllegalArgumentException(ERROR_BASEURL_NULL);
        }
        if (userName == null) {
            throw new IllegalArgumentException("userName may not be null");
        }

        StringBuilder sbUrl = new StringBuilder(baseUrl).append(URLPART_LOGIN);
        Map<String, String> params = new HashMap<>(2);
        params.put("user", userName);
        params.put("pw", password);
        String response = NetTools.getWebContentPOST(sbUrl.toString(), null, params, null, null, null, null);

        return XmlTools.getDocumentFromString(response, StringTools.DEFAULT_ENCODING);
    }

    //    public static Document oauth(String endpoint, String clientId) {
    //        String oAuthState = 
    //           new StringBuilder(String.valueOf(System.currentTimeMillis())).append(Helper.getServletPathWithHostAsUrlFromJsfContext())
    //                .toString();
    //        OAuthClientRequest request = 
    // OAuthClientRequest.authorizationProvider(OAuthProviderType.GOOGLE).setResponseType(ResponseType.CODE.name()
    //                .toLowerCase()).setClientId(clientId)
    //        .setRedirectURI(Helper.getServletPathWithHostAsUrlFromJsfContext() + "/" + OAuthServlet.URL)
    //                .setState(oAuthState).setScope("openid email").buildQueryMessage();
    //
    //    }

    /**
     * Returns the ID of the first collection that has the given collection name.
     *
     * @param baseUrl a {@link java.lang.String} object.
     * @param sessionId a {@link java.lang.String} object.
     * @param collectionName a {@link java.lang.String} object.
     * @should retrieve correct id
     * @return a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.HTTPException if any.
     */
    public static String getCollectionId(String baseUrl, String sessionId, String collectionName) throws IOException, HTTPException {
        logger.trace("getCollectionId: {}", collectionName);
        if (baseUrl == null) {
            throw new IllegalArgumentException(ERROR_BASEURL_NULL);
        }
        if (collectionName == null) {
            throw new IllegalArgumentException("collection may not be null");
        }
        if (sessionId == null) {
            throw new IllegalArgumentException("ownerSessionId may not be null");
        }

        StringBuilder sbUrl = new StringBuilder(baseUrl).append(URLPART_COLLECTION_LIST);
        sbUrl.append("?JSESSIONID=").append(sessionId).append("&name=").append(collectionName);
        String response = NetTools.getWebContentGET(sbUrl.toString());
        if (response != null) {
            JSONTokener tokener = new JSONTokener(response);
            JSONArray jsonArray = new JSONArray(tokener);
            for (Object o : jsonArray) {
                JSONObject jsonObj = (JSONObject) o;
                if (jsonObj.has("colId")) {
                    Long collectionId = (Long) jsonObj.get("colId");
                    if (collectionId != null) {
                        return String.valueOf(collectionId);
                    }
                }
            }
        }

        return null;
    }

    /**
     * <p>
     * createCollection.
     * </p>
     *
     * @param baseUrl a {@link java.lang.String} object.
     * @param sessionId a {@link java.lang.String} object.
     * @should create collection and return numeric id
     * @param collectionName a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     */
    protected static String createCollection(String baseUrl, String sessionId, String collectionName) throws IOException {
        if (baseUrl == null) {
            throw new IllegalArgumentException(ERROR_BASEURL_NULL);
        }
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId may not be null");
        }
        if (collectionName == null) {
            throw new IllegalArgumentException("collectionName may not be null");
        }

        StringBuilder sbUrl = new StringBuilder(baseUrl).append(URLPART_CREATE_COLLECTION).append("?collName=").append(collectionName);
        Map<String, String> params = new HashMap<>(1);
        params.put("JSESSIONID", sessionId);
        //        params.put("collName", collectionName);

        return NetTools.getWebContentPOST(sbUrl.toString(), params, null, null, null, null, null);
    }

    /**
     * <p>
     * grantCollectionPrivsToViewer.
     * </p>
     *
     * @param baseUrl a {@link java.lang.String} object.
     * @param sessionId a {@link java.lang.String} object.
     * @param collectionId a {@link java.lang.String} object.
     * @param recipientUserId a {@link java.lang.String} object.
     * @param sendMail a boolean.
     * @should grant privs correctly
     * @return a boolean.
     * @throws java.io.IOException if any.
     */
    protected static boolean grantCollectionPrivsToViewer(String baseUrl, String sessionId, String collectionId, String recipientUserId,
            boolean sendMail) throws IOException {
        if (baseUrl == null) {
            throw new IllegalArgumentException(ERROR_BASEURL_NULL);
        }
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId may not be null");
        }
        if (collectionId == null) {
            throw new IllegalArgumentException("collectionId may not be null");
        }
        if (recipientUserId == null) {
            throw new IllegalArgumentException("recipientUserId may not be null");
        }

        StringBuilder sbUrl = new StringBuilder(baseUrl).append(URLPART_GRANT_COLLECTION_PRIVS.replace("{collId}", collectionId));
        sbUrl.append("?userid=").append(recipientUserId).append("&role=Editor").append("&sendMail=").append(String.valueOf(sendMail));
        Map<String, String> params = new HashMap<>(1);
        params.put("JSESSIONID", sessionId);
        //        params.put("userid", recipientUserId);
        //        params.put("role", "Editor");
        //        params.put("sendMail", String.valueOf(sendMail));
        NetTools.getWebContentPOST(sbUrl.toString(), params, null, null, null, null, null);
        // Status 200 means success
        return true;
    }

    /**
     * <p>
     * ingestRecordToCollections.
     * </p>
     *
     * @param baseUrl a {@link java.lang.String} object.
     * @param session a {@link io.goobi.viewer.model.transkribus.TranskribusSession} object.
     * @param pi a {@link java.lang.String} object.
     * @param metsUrl a {@link java.lang.String} object.
     * @param userCollectionId a {@link java.lang.String} object.
     * @param viewerCollectionId a {@link java.lang.String} object.
     * @should ingest record correctly
     * @return a {@link io.goobi.viewer.model.transkribus.TranskribusJob} object.
     * @throws org.apache.http.client.ClientProtocolException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.HTTPException if any.
     * @throws org.jdom2.JDOMException if any.
     */
    protected static TranskribusJob ingestRecordToCollections(String baseUrl, TranskribusSession session, String pi, String metsUrl,
            String userCollectionId, String viewerCollectionId) throws ClientProtocolException, IOException, HTTPException, JDOMException {
        if (baseUrl == null) {
            throw new IllegalArgumentException(ERROR_BASEURL_NULL);
        }
        if (session == null) {
            throw new IllegalArgumentException("session may not be null");
        }
        if (metsUrl == null) {
            throw new IllegalArgumentException("metsUrl may not be null");
        }
        if (userCollectionId == null) {
            throw new IllegalArgumentException("userCollectionId may not be null");
        }
        if (viewerCollectionId == null) {
            throw new IllegalArgumentException("viewerCollectionId may not be null");
        }

        StringBuilder sbUrl = new StringBuilder(baseUrl).append(URLPART_CREATE_DOC_FROM_METS_URL.replace("{collId}", userCollectionId));
        sbUrl.append("?fileName=").append(URLEncoder.encode(metsUrl, StringTools.DEFAULT_ENCODING)).append("&collId=").append(viewerCollectionId);
        Map<String, String> params = new HashMap<>(1);
        params.put("JSESSIONID", session.getSessionId());
        String response = NetTools.getWebContentPOST(sbUrl.toString(), params, null, null, null, null, null);
        TranskribusJob job = new TranskribusJob();
        job.setPi(pi);
        job.setOwnerId(session.getUserId());
        job.setUserCollectionId(userCollectionId);
        job.setViewerCollectionId(viewerCollectionId);
        job.setJobId(response);
        job.setStatus(JobStatus.WAITING);
        job.setDateCreated(LocalDateTime.now());

        // TODO retrieve doc id

        return job;

    }

    /**
     * <p>
     * checkJobStatus.
     * </p>
     *
     * @param baseUrl a {@link java.lang.String} object.
     * @param sessionId a {@link java.lang.String} object.
     * @param jobId a {@link java.lang.String} object.
     * @should return correct status
     * @return a {@link io.goobi.viewer.model.transkribus.TranskribusJob.JobStatus} object.
     * @throws org.apache.http.client.ClientProtocolException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.HTTPException if any.
     */
    protected static JobStatus checkJobStatus(String baseUrl, String sessionId, String jobId)
            throws ClientProtocolException, IOException, HTTPException {
        if (baseUrl == null) {
            throw new IllegalArgumentException(ERROR_BASEURL_NULL);
        }
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId may not be null");
        }
        if (jobId == null) {
            throw new IllegalArgumentException("jobId may not be null");
        }

        StringBuilder sbUrl = new StringBuilder(baseUrl).append(URLPART_CHECK_JOB_STATUS.replace("{id}", jobId).replace("{docId}", "TODO"))
                .append("?JSESSIONID=")
                .append(sessionId);
        String response = NetTools.getWebContentGET(sbUrl.toString());
        if (response != null) {
            JSONTokener tokener = new JSONTokener(response);
            JSONObject jsonObj = new JSONObject(tokener);
            if (jsonObj.has("state")) {
                String state = (String) jsonObj.get("state");
                logger.trace("State for job {}: {}", jobId, state);
                if (state != null) {
                    switch (state) {
                        // TODO more statuses
                        case "FINISHED":
                            return JobStatus.READY;
                        case "FAILED":
                            return JobStatus.ERROR;
                    }
                }
            }
        }

        return null;
    }
}
