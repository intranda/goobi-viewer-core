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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.faces.validators.PIValidator;
import de.intranda.digiverso.presentation.model.security.AccessConditionUtils;

/**
 * Servlet for original content file download.
 */
public class FileServlet extends HttpServlet implements Serializable {

    private static final long serialVersionUID = -3607375020549274741L;

    private static final Logger logger = LoggerFactory.getLogger(FileServlet.class);

    // private HttpClient httpClient;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public FileServlet() {
        super();
        // httpClient = new HttpClient();
        // httpClient.setHttpConnectionManager(new MultiThreadedHttpConnectionManager());
        // httpClient.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pi = null;
        String fileName = null;
        String page = null;

        if (request.getParameterMap().size() > 0) {
            // Regular URLs
            Set<String> keys = request.getParameterMap().keySet();
            for (String s : keys) {
                String[] values = request.getParameterMap().get(s);
                if (values[0] != null) {
                    switch (s) {
                        case "pi":
                            pi = values[0];
                            break;
                        case "file":
                            fileName = values[0];
                            break;
                        case "page":
                            page = values[0];
                            break;
                    }
                }
            }
        }
        if (pi == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameter: pi");
            return;
        } else if (!PIValidator.validatePi(pi)) {
            logger.warn("PI is invalid: {}", pi);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "pi value invalid");
            return;
        }
        if (fileName == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameter: fileName");
            return;
        }
        logger.trace("File request for /{}/{}", pi, fileName);

        // Check access conditions, if an actual document with a PI is involved
        boolean access = false;
        try {
            access = AccessConditionUtils.checkContentFileAccessPermission(pi, request);
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        } catch (DAOException e) {
            logger.debug("DAOException thrown here: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }

        if (access) {
            try {
                String dataRepository = DataManager.getInstance().getSearchIndex().findDataRepository(pi);
                StringBuilder sbFilePath = new StringBuilder();
                if (StringUtils.isNotEmpty(dataRepository)) {
                    sbFilePath.append(DataManager.getInstance().getConfiguration().getDataRepositoriesHome()).append(dataRepository).append(
                            File.separator);
                } else {
                    sbFilePath.append(DataManager.getInstance().getConfiguration().getViewerHome());
                }
                sbFilePath.append(DataManager.getInstance().getConfiguration().getOrigContentFolder());
                sbFilePath.append(File.separator).append(pi).append(File.separator);

                if (page != null) {
                    sbFilePath.append(page).append(File.separator);
                }
                sbFilePath.append(fileName);
                Path path = Paths.get(sbFilePath.toString());
                if (Files.isRegularFile(path)) {
                    try {
                        String contentType = Files.probeContentType(path);
                        logger.trace("content type: {}", contentType);
                        if (contentType == null) {
                            contentType = "application/pdf";
                        }
                        response.setContentType(contentType);
                        response.setHeader("Content-Disposition", new StringBuilder("attachment;filename=").append(fileName).toString());
                        response.setHeader("Content-Length", String.valueOf(Files.size(path)));
                        response.flushBuffer();
                        OutputStream os = response.getOutputStream();
                        try (FileInputStream fis = new FileInputStream(path.toFile())) {
                            byte[] buffer = new byte[1024];
                            int bytesRead = 0;
                            while ((bytesRead = fis.read(buffer)) != -1) {
                                os.write(buffer, 0, bytesRead);
                            }
                        }
                        // os.flush();
                    } catch (ClientAbortException | SocketException e) {
                        logger.warn("Client {} has abborted the connection: {}", request.getRemoteAddr(), e.getMessage());
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                } else {
                    logger.error("File not found: {}", path.toAbsolutePath().toString());
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, path.getFileName().toString());
                    return;
                }
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                return;
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                return;
            }
        } else {
            logger.debug("Access condition for download not met for '{}'.", pi);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
