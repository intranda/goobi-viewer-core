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

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.HTTPException;
import de.intranda.digiverso.presentation.model.transkribus.TranskribusJob;
import de.intranda.digiverso.presentation.model.transkribus.TranskribusJob.JobStatus;
import de.intranda.digiverso.presentation.model.transkribus.TranskribusSession;

public class TranskribusUtils {

    private static final Logger logger = LoggerFactory.getLogger(TranskribusUtils.class);

    public static final String TRANSRIBUS_REST_URL = "https://transkribus.eu/TrpServer/rest/";
    public static final String TRANSRIBUS_REST_TESTING_URL = "https://transkribus.eu/TrpServerTesting/rest/";
    private static final String URLPART_CHECK_JOB_STATUS = "jobs/{id}";
    private static final String URLPART_COLLECTION_LIST = "collections/listByName";
    private static final String URLPART_CREATE_COLLECTION = "collections/createCollection";
    private static final String URLPART_CREATE_DOC_FROM_METS_URL = "collections/{collId}/createDocFromMetsUrl";
    private static final String URLPART_GRANT_COLLECTION_PRIVS = "collections/{collId}/addOrModifyUserInCollection";
    private static final String URLPART_LOGIN = "auth/login";

    /**
     * 
     * @param restApiUrl
     * @param userSession
     * @param pi
     * @param metsResolverUrlRoot Root of the METS resolver URL (without the identifier).
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @throws HTTPException
     * @throws JDOMException
     * @throws ParseException
     * @throws DAOException
     */
    public static TranskribusJob ingestRecord(String restApiUrl, TranskribusSession userSession, String pi, String metsResolverUrlRoot)
            throws ClientProtocolException, IOException, HTTPException, JDOMException, ParseException, DAOException {
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
        String userCollectionId = getCollectionId(restApiUrl, userSession.getSessionId(), DataManager.getInstance().getConfiguration()
                .getTranskribusDefaultCollection());
        if (userCollectionId == null) {
            userCollectionId = createCollection(restApiUrl, userSession.getSessionId(), DataManager.getInstance().getConfiguration()
                    .getTranskribusDefaultCollection());
            if (userCollectionId == null) {
                logger.error("Could not create add collection '{}' to the user acccount '{}'.", DataManager.getInstance().getConfiguration()
                        .getDefaultCollection(), userSession.getUserName());
                return null;
            }
        }

        // Get this viewer's Transkribus session metadata
        TranskribusSession viewerSession = login(restApiUrl, DataManager.getInstance().getConfiguration().getTranskribusUserName(), DataManager
                .getInstance().getConfiguration().getTranskribusPassword());
        logger.trace(DataManager.getInstance().getConfiguration().getTranskribusUserName() + " - " + DataManager.getInstance().getConfiguration().getTranskribusPassword());

        // Check and create the default viewer instance collection
        String viewerCollectionId = getCollectionId(restApiUrl, viewerSession.getSessionId(), DataManager.getInstance().getConfiguration()
                .getDefaultCollection());
        if (viewerCollectionId == null) {
            viewerCollectionId = createCollection(restApiUrl, viewerSession.getSessionId(), DataManager.getInstance().getConfiguration().getDefaultCollection());
            if (viewerCollectionId == null) {
                logger.error("Could not create the default collection '{}' for the viewer instance.", DataManager.getInstance().getConfiguration()
                        .getDefaultCollection());
                return null;
            }
        }

        // Grant editor privs to the viewer account for the user's collection
        grantCollectionPrivsToViewer(restApiUrl, userSession.getSessionId(), userCollectionId, viewerSession.getUserId(), false);

        // Create new doc via the METS resolver URL
        TranskribusJob job = ingestRecordToCollections(restApiUrl, userSession, pi, metsResolverUrlRoot.endsWith(pi) ? metsResolverUrlRoot
                : metsResolverUrlRoot + pi, userCollectionId, viewerCollectionId);

        // Add new job to the DB and schedule periodical checks
        DataManager.getInstance().getDao().addTranskribusJob(job);

        return job;
    }

    /**
     * 
     * @param baseUrl
     * @param userName
     * @param password
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @throws JDOMException
     * @throws HTTPException
     */
    public static TranskribusSession login(String baseUrl, String userName, String password) throws ClientProtocolException, IOException,
            JDOMException, HTTPException {
        if (baseUrl == null) {
            throw new IllegalArgumentException("baseUrl may not be null");
        }
        if (userName == null) {
            throw new IllegalArgumentException("userName may not be null");
        }

        Document doc = auth(baseUrl, userName, password);
        if (doc != null && doc.getRootElement() != null) {
            return new TranskribusSession(doc.getRootElement().getChildText("userId"), doc.getRootElement().getChildText("userName"), doc
                    .getRootElement().getChildText("sessionId"));
        }

        logger.error("Transkribus login failed for account '{}'", userName);
        return null;
    }

    /**
     * 
     * @param baseUrl
     * @param userName
     * @param password
     * @return JDOM object containing the API response
     * @throws IOException
     * @throws ClientProtocolException
     * @throws JDOMException
     * @throws HTTPException
     * @should auth correctly
     */
    public static Document auth(String baseUrl, String userName, String password) throws ClientProtocolException, IOException, JDOMException,
            HTTPException {
        if (baseUrl == null) {
            throw new IllegalArgumentException("baseUrl may not be null");
        }
        if (userName == null) {
            throw new IllegalArgumentException("userName may not be null");
        }

        StringBuilder sbUrl = new StringBuilder(baseUrl).append(URLPART_LOGIN);
        Map<String, String> params = new HashMap<>(2);
        params.put("user", userName);
        params.put("pw", password);
        String response = Helper.getWebContentPOST(sbUrl.toString(), params, null);

        return FileTools.getDocumentFromString(response, Helper.DEFAULT_ENCODING);
    }

    //    public static Document oauth(String endpoint, String clientId) {
    //        String oAuthState = new StringBuilder(String.valueOf(System.currentTimeMillis())).append(Helper.getServletPathWithHostAsUrlFromJsfContext())
    //                .toString();
    //        OAuthClientRequest request = OAuthClientRequest.authorizationProvider(OAuthProviderType.GOOGLE).setResponseType(ResponseType.CODE.name()
    //                .toLowerCase()).setClientId(clientId).setRedirectURI(Helper.getServletPathWithHostAsUrlFromJsfContext() + "/" + OAuthServlet.URL)
    //                .setState(oAuthState).setScope("openid email").buildQueryMessage();
    //      
    //    }

    /**
     * Returns the ID of the first collection that has the given collection name.
     * 
     * @param baseUrl
     * @param sessionId
     * @param collectionName
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @throws HTTPException
     * @throws ParseException
     * @should retrieve correct id
     */
    public static String getCollectionId(String baseUrl, String sessionId, String collectionName) throws ClientProtocolException, IOException,
            HTTPException, ParseException {
        logger.trace("getCollectionId: {}", collectionName);
        if (baseUrl == null) {
            throw new IllegalArgumentException("baseUrl may not be null");
        }
        if (collectionName == null) {
            throw new IllegalArgumentException("collection may not be null");
        }
        if (sessionId == null) {
            throw new IllegalArgumentException("ownerSessionId may not be null");
        }

        StringBuilder sbUrl = new StringBuilder(baseUrl).append(URLPART_COLLECTION_LIST);
        sbUrl.append("?JSESSIONID=").append(sessionId).append("&name=").append(collectionName);
        String response = Helper.getWebContentGET(sbUrl.toString());
        if (response != null) {
            JSONArray jsonArray = (JSONArray) new JSONParser().parse(response);
            if (jsonArray != null) {
                for (Object o : jsonArray) {
                    JSONObject jsonObj = (JSONObject) o;
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
     * 
     * @param baseUrl
     * @param sessionId
     * @param collection
     * @return
     * @throws HTTPException
     * @throws IOException
     * @throws ClientProtocolException
     * @throws JDOMException
     * @should create collection and return numeric id
     * 
     */
    protected static String createCollection(String baseUrl, String sessionId, String collectionName) throws ClientProtocolException, IOException,
            HTTPException, JDOMException {
        if (baseUrl == null) {
            throw new IllegalArgumentException("baseUrl may not be null");
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

        return Helper.getWebContentPOST(sbUrl.toString(), params, null);
    }

    /**
     * 
     * @param baseUrl
     * @param sessionId
     * @param collectionId
     * @param recipientUserId
     * @param recipientSessionId
     * @param sendMail
     * @return
     * @throws HTTPException
     * @throws IOException
     * @throws ClientProtocolException
     * @throws JDOMException
     * @should grant privs correctly
     */
    protected static boolean grantCollectionPrivsToViewer(String baseUrl, String sessionId, String collectionId, String recipientUserId,
            boolean sendMail) throws ClientProtocolException, IOException, HTTPException, JDOMException {
        if (baseUrl == null) {
            throw new IllegalArgumentException("baseUrl may not be null");
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
        Helper.getWebContentPOST(sbUrl.toString(), params, null);
        // Status 200 means success
        return true;
    }

    /**
     * 
     * @param baseUrl
     * @param session
     * @param pi
     * @param metsUrl
     * @param userCollectionId
     * @param viewerCollectionId
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @throws HTTPException
     * @throws JDOMException
     * @should ingest record correctly
     */
    protected static TranskribusJob ingestRecordToCollections(String baseUrl, TranskribusSession session, String pi, String metsUrl,
            String userCollectionId, String viewerCollectionId) throws ClientProtocolException, IOException, HTTPException, JDOMException {
        if (baseUrl == null) {
            throw new IllegalArgumentException("baseUrl may not be null");
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
        sbUrl.append("?fileName=").append(URLEncoder.encode(metsUrl, Helper.DEFAULT_ENCODING)).append("&collId=").append(viewerCollectionId);
        Map<String, String> params = new HashMap<>(1);
        params.put("JSESSIONID", session.getSessionId());
        String response = Helper.getWebContentPOST(sbUrl.toString(), params, null);
        TranskribusJob job = new TranskribusJob();
        job.setPi(pi);
        job.setOwnerId(session.getUserId());
        job.setUserCollectionId(userCollectionId);
        job.setViewerCollectionId(viewerCollectionId);
        job.setJobId(response);
        job.setStatus(JobStatus.WAITING);
        job.setDateCreated(new Date());

        // TODO retrieve doc id

        return job;

    }

    /**
     * 
     * @param baseUrl
     * @param sessionId
     * @param jobId
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @throws HTTPException
     * @throws ParseException
     * @should return correct status
     */
    protected static TranskribusJob.JobStatus checkJobStatus(String baseUrl, String sessionId, String jobId) throws ClientProtocolException,
            IOException, HTTPException, ParseException {
        if (baseUrl == null) {
            throw new IllegalArgumentException("baseUrl may not be null");
        }
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId may not be null");
        }
        if (jobId == null) {
            throw new IllegalArgumentException("jobId may not be null");
        }

        StringBuilder sbUrl = new StringBuilder(baseUrl).append(URLPART_CHECK_JOB_STATUS.replace("{id}", jobId).replace("{docId}", "TODO")).append(
                "?JSESSIONID=").append(sessionId);
        String response = Helper.getWebContentGET(sbUrl.toString());
        if (response != null) {
            JSONObject jsonObj = (JSONObject) new JSONParser().parse(response);
            if (jsonObj != null) {
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
