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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.user.IPrivilegeHolder;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.servlet.controller.GetImageAction;
import net.balusc.webapp.ContentDeliveryServlet;

/**
 * ContentServer wrapper servlet.
 */
public class ContentServerWrapperServlet extends HttpServlet implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -4174862970784876821L;

    private static final Logger logger = LoggerFactory.getLogger(ContentServerWrapperServlet.class);

    private static int maxImageWidth = DataManager.getInstance().getConfiguration().getViewerMaxImageWidth();
    private static int maxImageHeight = DataManager.getInstance().getConfiguration().getViewerMaxImageHeight();
    private static int maxImageScale = DataManager.getInstance().getConfiguration().getViewerMaxImageScale();

    // private HttpClient httpClient;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public ContentServerWrapperServlet() {
        super();
        // httpClient = new DefaultHttpClient();
        // // httpClient.setHttpConnectionManager(new MultiThreadedHttpConnectionManager());
        // httpClient.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // http://localhost:8080/viewer/content/PPN345858735_0059_01/800/0/00000379.jpg
        // http://localhost:8080/viewer/cs?action=image&sourcepath=/PPN345858735_0059_01/00000379.tif&cale=75&rotate=0&resolution=72
        // http://localhost:8080/viewer/content/PPN623026171/800/0/00000001.jpg´
        // long start = System.currentTimeMillis();
        boolean slashed = false;
        String csType = "cs";
        String action = "image";
        String format = "jpg";
        String mimeType = "image/tiff";
        String pi = null;
        String dataRepository = null;
        String mediaFilePath = null;
        String contentFileName = null;

        int imageWidth = 600;
        boolean access = true;
        StringBuilder url = new StringBuilder();
        url.append(DataManager.getInstance().getConfiguration().getContentServerRealUrl());
        //        String tileGroup = null;

        StringBuilder urlArgs = new StringBuilder(50);
        if (request.getParameterMap().size() > 0) {
            // Regular URLs
            Set<String> keys = request.getParameterMap().keySet();
            
            if(request.getParameterMap().get("action") != null) {
                action = request.getParameterMap().get("action")[0];
                keys.remove("action");
            }
            if(request.getParameterMap().get("format") != null) {
                format = request.getParameterMap().get("format")[0];
                keys.remove("format");
            }
            if(request.getParameterMap().get("mimeType") != null) {
                mimeType = request.getParameterMap().get("mimeType")[0];
                keys.remove("mimeType");
            }
            if(request.getParameterMap().get("sourcepath") != null) {
                String sourcepath = request.getParameterMap().get("sourcepath")[0];
                if (!sourcepath.startsWith("file") && !sourcepath.startsWith("http")) {
                    mediaFilePath = sourcepath;
                    String[] pathSplit = sourcepath.split("[/]");
                    if (pathSplit.length > 1) {
                        if (pathSplit.length < 3) {
                            pi = pathSplit[0];
                            contentFileName = pathSplit[1];
                        } else {
                            pi = pathSplit[1];
                            contentFileName = pathSplit[2];
                        }
                        if (pi.contains("http") || pi.contains(":")) {
                            logger.warn("Parsed invalid PI: {}", pi);
                        }
                    } else {
                        contentFileName = mediaFilePath;
                    }
                    // metsFileNameSplit = mediaFileName.split("[.]");
                    keys.remove("sourcepath");
                }
            }
            if(request.getParameterMap().get("identifier") != null) {
                pi = request.getParameterMap().get("identifier")[0];
                keys.remove("identifier");
            }
            if(request.getParameterMap().get("width") != null) {
                imageWidth = Integer.valueOf(request.getParameterMap().get("width")[0]);
            }
            if(request.getParameterMap().get("targetFileName") != null) {
                keys.remove("targetFileName");
            }
            if(request.getParameterMap().get("metsFile") != null && request.getParameterMap().get("images") == null) {
                csType = "gcs";
            }

            for (String s : keys) {
                String[] values = request.getParameterMap().get(s);
                if (values[0] != null) {
                    urlArgs.append('&').append(s).append('=').append(values[0]);
                }
            }
        } else {
            // Slashed URLs
            try {
                String params = request.getRequestURI().replace(request.getContextPath() + "/content/", "");
                slashed = true;
                String[] paramsSplit = params.split("[/]");

                pi = paramsSplit[0];
                try {
                    dataRepository = DataManager.getInstance().getSearchIndex().findDataRepository(pi);
                } catch (PresentationException e) {
                    logger.debug("PresentationException thrown here: {}", e.getMessage());
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                    return;
                }
                try {
                    imageWidth = Integer.valueOf(paramsSplit[1]);
                } catch (NumberFormatException e) {
                    // If NaN, then the mime type is in here
                    action = paramsSplit[1];
                    mimeType = paramsSplit[1] + "/" + paramsSplit[2];
                }
                contentFileName = paramsSplit[3];
                String[] imageFileNameSplit = contentFileName.split("[.]");

                if ("image".equals(action) && pi != null) {
                    // Search for the real file name in Lucene
                    String watermarkIdField = DataManager.getInstance().getConfiguration().getWatermarkIdField();
                    String query = new StringBuilder(SolrConstants.PI_TOPSTRUCT).append(':').append(pi).append(" AND ").append(SolrConstants.FILENAME)
                            .append(':').append(imageFileNameSplit[0]).append('*').toString();
                    String[] fieldNameFilter = { SolrConstants.FILENAME, SolrConstants.IMAGEURN, watermarkIdField };
                    SolrDocumentList docs = DataManager.getInstance().getSearchIndex().search(query, SolrSearchIndex.MAX_HITS, null, Arrays.asList(
                            fieldNameFilter));
                    if (docs != null && docs.size() > 0) {
                        SolrDocument doc = docs.get(0);
                        Object field = doc.getFieldValue(SolrConstants.FILENAME);
                        if (field != null) {
                            contentFileName = (String) field;
                        }
                        // Add URN parameter for DFG
                        Object fieldUrn = doc.getFieldValue(SolrConstants.IMAGEURN);
                        if (fieldUrn != null) {
                            urlArgs.append("&watermarkText=").append(fieldUrn);
                        }
                        String watermarkId = null;
                        if (doc.getFieldNames().contains(watermarkIdField)) {
                            // Check for the watermark ID in the page doc (should rarely be the case)
                            Object obj = doc.getFieldValue(watermarkIdField);
                            if (obj instanceof String) {
                                watermarkId = (String) obj;
                            } else if (obj instanceof ArrayList) {
                                List<Object> objList = (List<Object>) obj;
                                if (!objList.isEmpty()) {
                                    watermarkId = (String) objList.get(0);
                                }
                            }
                        } else {
                            // Check for the watermark ID in the top document for the watermark ID field
                            SolrDocumentList topDocList = DataManager.getInstance().getSearchIndex().search(new StringBuilder(SolrConstants.PI)
                                    .append(':').append(pi).toString(), SolrSearchIndex.MAX_HITS, null, Collections.singletonList(watermarkIdField));
                            if (topDocList != null && topDocList.size() > 0) {
                                Object obj = topDocList.get(0).getFieldValue(watermarkIdField);
                                if (obj instanceof String) {
                                    watermarkId = (String) topDocList.get(0).getFieldValue(watermarkIdField);
                                } else if (obj instanceof ArrayList) {
                                    List<Object> objList = (List<Object>) obj;
                                    if (!objList.isEmpty()) {
                                        watermarkId = (String) objList.get(0);
                                    }
                                }
                            }
                        }
                        // Add watermark ID value, if applicable
                        if (StringUtils.isNotEmpty(watermarkId)) {
                            urlArgs.append("&watermarkId=").append(watermarkId);
                            logger.debug("watermarkId={}", watermarkId);
                        }
                    }
                }

                if (StringUtils.isNotEmpty(dataRepository)) {
                    mediaFilePath = new StringBuilder(dataRepository).append('/').append(DataManager.getInstance().getConfiguration()
                            .getMediaFolder()).append('/').append(pi).append('/').append(contentFileName).toString();
                } else {
                    // Backwards compatibility with old indexes
                    mediaFilePath = new StringBuilder(pi).append('/').append(contentFileName).toString();
                }
                format = imageFileNameSplit[1];
                try {
                    int width = Integer.parseInt(paramsSplit[1]);
                    if (maxImageWidth > 0 && width > maxImageWidth) {
                        width = maxImageWidth;
                    }
                    urlArgs.append("&width=").append(width);
                } catch (NumberFormatException e) {
                }
                urlArgs.append("&rotate=").append(paramsSplit[2]);
                urlArgs.append("&compression=95");
            } catch (ArrayIndexOutOfBoundsException e) {
                logger.debug("Error processing request URL: {}", request.getRequestURL().toString());
                logger.debug(e.getMessage(), e);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                return;
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

        boolean isThumbnail = imageWidth <= DataManager.getInstance().getConfiguration().getUnconditionalImageAccessMaxWidth();

        // Check access conditions, if an actual document with a PI is involved
        if (pi != null) {
            // TODO maybe put the or case out of this if and extract the permission check to a function
            // 1) retrieve the PhysicalElement for this media file
            // 2) if physicalElement.accessGranted == true, set access == true
            // 3) otherwise check via checkAccessPermissionByPhysicalElement() and set pif physicalElement.accessGranted to whatever
            // checkAccessPermission returns
            try {
                access = checkAccess(request, action, pi, contentFileName, isThumbnail);
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                return;
            } catch (DAOException e) {
                logger.debug("DAOException thrown here: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                return;
            }
        }

        // access = false;
        if (access) {
            if (mediaFilePath != null) {
                urlArgs.append("&sourcepath=").append(mediaFilePath);
            }
            if (isThumbnail) {
                urlArgs.append("&thumbnail=true&ignoreWatermark=true");
            }
        } else {
            if (isThumbnail) {
                format = "jpg";
                urlArgs.append("&sourcepath=").append(ServletUtils.getServletPathWithHostAsUrlFromRequest(request)).append("/resources/themes/")
                        .append(DataManager.getInstance().getConfiguration().getTheme()).append("/images/thumbnail_access_denied.jpg");
            } else {
                format = "png";
                urlArgs.append("&ignoreWatermark=true&sourcepath=").append(ServletUtils.getServletPathWithHostAsUrlFromRequest(request)).append(
                        "/resources/themes/").append(DataManager.getInstance().getConfiguration().getTheme()).append("/images/access_denied.png");
            }
        }
        url.append(csType).append("?action=").append(action).append("&format=").append(format).append(urlArgs.toString());

        //        logger.debug("Access condition check took " + (System.currentTimeMillis() - startTime) + " ms.");
        try {
            if (access) {
                //See if we call an external servlet
                if (DataManager.getInstance().getConfiguration().isUseExternalCS() && ("image".equals(action) || "pdf".equals(action) || "dimensions"
                        .equals(action) || "version".equals(action))) {
                    // response.sendRedirect(url.toString());
                    logger.trace("FORWARD to {}", url.toString());
                    request.getRequestDispatcher(url.toString()).forward(request, response);
                } else {
                    switch (action) {
                        case "image":
                            // Retrieve image using the ContentServer API
                            if (!slashed && request.getParameterMap().size() > 0) {
                                // Retrieve image via the ContentServer
                                Map<String, String[]> requestMapCopy = new HashMap<>(request.getParameterMap());
                                // If no image size has been given, add a static size to prevent full size image loading
                                if (requestMapCopy.get("width") != null) {
                                    int width = Integer.parseInt(requestMapCopy.get("width")[0]);
                                    if (maxImageWidth > 0 && width > maxImageWidth) {
                                        logger.debug("Image width parameter out of range ({}), replacing with {}", width, maxImageWidth);
                                        width = maxImageWidth;
                                        requestMapCopy.put("width", new String[] { String.valueOf(width) });
                                    }
                                }
                                if (requestMapCopy.get("height") != null) {
                                    int height = Integer.parseInt(requestMapCopy.get("height")[0]);
                                    if (maxImageHeight > 0 && height > maxImageHeight) {
                                        logger.debug("Image height parameter out of range ({}), replacing with {}", height, maxImageHeight);
                                        height = maxImageHeight;
                                        requestMapCopy.put("height", new String[] { String.valueOf(height) });
                                    }
                                }
                                if (requestMapCopy.get("scale") != null) {
                                    int scale = Integer.parseInt(requestMapCopy.get("scale")[0]);
                                    if (maxImageScale > 0 && scale > maxImageScale) {
                                        logger.debug("Image scale parameter out of range ({}), replacing with {}", scale, maxImageScale);
                                        scale = maxImageScale;
                                        requestMapCopy.put("scale", new String[] { String.valueOf(scale) });
                                    }
                                }
                                long start = System.currentTimeMillis();

                                byte[] image = new GetImageAction().getImage(request.getParameterMap());
                                if (image != null) {
                                    response.setContentType("image/" + format);
                                    response.setContentLength(image.length);
                                    response.flushBuffer();
                                    try (OutputStream os = response.getOutputStream()) {
                                        os.write(image);
                                        os.flush();
                                    }
                                    long end = System.currentTimeMillis();
                                    long elapsed = end - start;
                                    logger.trace("Image fetched in {} ms", elapsed);
                                }
                            } else {
                                // Slashed URLs don't have a usable param map
                                getServletContext().getRequestDispatcher(url.toString()).forward(request, response);
                            }
                            return;
                        case "video":
                        case "audio": {
                            // Retrieve audio/video directly from the file syste
                            File file;
                            if (StringUtils.isNotEmpty(dataRepository)) {
                                file = new File(DataManager.getInstance().getConfiguration().getDataRepositoriesHome() + mediaFilePath);
                            } else {
                                // Backwards compatibility with old indexes
                                file = new File(DataManager.getInstance().getConfiguration().getViewerHome() + DataManager.getInstance()
                                        .getConfiguration().getMediaFolder() + '/' + mediaFilePath + '/');
                            }
                            if (file.isFile()) {
                                logger.debug("AV file: {} ({} bytes)", file.getAbsolutePath(), file.length());
                                new ContentDeliveryServlet().processRequest(request, response, true, file.getAbsolutePath(), mimeType);

                            } else {
                                logger.error("File '{}' not found.", file.getAbsolutePath());
                            }
                        }
                            return;
                        case "application":
                        case "text": {
                            // http://localhost:8080/viewer/content?action=text&format=txt&sourcepath=PPN517154005/00000001.txt
                            File file = null;
                            String recordPath;
                            if (StringUtils.isEmpty(dataRepository)) {
                                recordPath = DataManager.getInstance().getConfiguration().getViewerHome();
                            } else {
                                recordPath = new StringBuilder(DataManager.getInstance().getConfiguration().getDataRepositoriesHome()).append(
                                        dataRepository).append("/").toString();
                            }
                            logger.trace("FORMAT: {}", format);
                            switch (format) {
                                case "alto":
                                    file = new File(new StringBuilder(recordPath).append(DataManager.getInstance().getConfiguration().getAltoFolder())
                                            .append('/').append(pi).toString(), contentFileName);
                                    response.setContentType("text/xml");
                                    break;
                                case "abbyy":
                                    file = new File(new StringBuilder(recordPath).append(DataManager.getInstance().getConfiguration()
                                            .getAbbyyFolder()).append('/').append(pi).toString(), contentFileName);
                                    response.setContentType("text/xml");
                                    break;
                                case "txt":
                                    file = new File(new StringBuilder(recordPath).append(DataManager.getInstance().getConfiguration()
                                            .getFulltextFolder()).append('/').append(pi).toString(), contentFileName);
                                    response.setContentType("text/html; charset=UTF-8");
                                    break;
                                case "wc":
                                    file = new File(new StringBuilder(recordPath).append(DataManager.getInstance().getConfiguration().getWcFolder())
                                            .append(DataManager.getInstance().getConfiguration().getWcFolder()).append(pi).toString(),
                                            contentFileName);
                                    response.setContentType("text/xml");
                                    break;
                                case "pdf":
                                    // E-publication PDFs
                                    file = new File(new StringBuilder(recordPath).append(DataManager.getInstance().getConfiguration()
                                            .getMediaFolder()).append("/").append(pi).toString(), contentFileName);
                                    response.setContentType("application/pdf");
                                    break;
                                default: // nothing
                                    // TODO page PDFs?
                                    // TODO crowdsourcing?
                            }
                            if (file != null && file.isFile()) {
                                try {
                                    response.setHeader("Content-Disposition", "filename=" + file.getName());
                                    response.setHeader("Content-Length", String.valueOf(file.length()));
                                    response.flushBuffer();
                                    OutputStream os = response.getOutputStream();
                                    try (FileInputStream fis = new FileInputStream(file)) {
                                        byte[] buffer = new byte[1024];
                                        int bytesRead = 0;
                                        while ((bytesRead = fis.read(buffer)) != -1) {
                                            os.write(buffer, 0, bytesRead);
                                        }
                                    }
                                    // os.flush();
                                } catch (ClientAbortException | SocketException e) {
                                    logger.warn("Client {} has aborted the connection: {}", request.getRemoteAddr(), e.getMessage());
                                } catch (IOException e) {
                                    logger.error(e.getMessage(), e);
                                }
                            } else {
                                String filename = file == null ? contentFileName : file.getAbsolutePath();
                                logger.warn("File not found: {}", filename);
                                response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found: " + filename);
                            }
                        }
                            return;
                        case "pdf":
                            if (DataManager.getInstance().getConfiguration().isPdfApiDisabled()) {
                                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                                return;
                            }
                            response.setContentType("application/pdf");
                            logger.debug("ContentServer call URL:  {}", url.toString());
                            getServletContext().getRequestDispatcher(url.toString()).forward(request, response);
                            return;
                        case "dimensions":
                        case "version":
                            response.setContentType("text/html; charset=UTF-8");
                            logger.debug("ContentServer call URL: {}", url.toString());
                            getServletContext().getRequestDispatcher(url.toString()).forward(request, response);
                            //                    response.sendRedirect(url.toString());
                            break;
                        default: // nothing
                    }
                }
            } else {
                logger.debug("ContentServer call URL: {}", url.toString());
                getServletContext().getRequestDispatcher(url.toString()).forward(request, response);
            }
        } catch (ContentLibException e) {
            logger.warn("Image not found: {}", e.getMessage());
            // TODO display a "image not found" image or smt
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.debug("Error processing request URL: {}", request.getRequestURL().toString(), e);
        }

        // long start = System.currentTimeMillis();
        // getServletContext().getRequestDispatcher(url.toString()).forward(request, response);
        // long end = System.currentTimeMillis();
        // logger.trace("Image fetched in " + (end - start) + " ms");
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected static boolean checkAccess(HttpServletRequest request, String action, String pi, String contentFileName, boolean isThumbnail)
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
                        return SearchHelper.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName,
                                IPrivilegeHolder.PRIV_DOWNLOAD_PDF);
                    default:
                        if (isThumbnail) {
                            return SearchHelper.checkAccessPermissionForThumbnail(request, pi, contentFileName);
                            //                                logger.trace("Checked thumbnail access: {}/{}: {}", pi, contentFileName, access);
                        }
                        return SearchHelper.checkAccessPermissionForImage(request, pi, contentFileName);
                    //                                logger.trace("Checked image access: {}/{}: {}", pi, contentFileName, access);
                }
            case "text":
            case "ocrdump":
                return SearchHelper.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName,
                        IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
            case "pdf":
                return SearchHelper.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName,
                        IPrivilegeHolder.PRIV_DOWNLOAD_PDF);
            case "video":
                return SearchHelper.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName,
                        IPrivilegeHolder.PRIV_VIEW_VIDEO);
            case "audio":
                return SearchHelper.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName,
                        IPrivilegeHolder.PRIV_VIEW_AUDIO);
            case "dimensions":
            case "version":
                return SearchHelper.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName,
                        IPrivilegeHolder.PRIV_VIEW_IMAGES); // TODO is priv checking needed here?
            default: // nothing
                break;
        }

        return false;
    }
}
