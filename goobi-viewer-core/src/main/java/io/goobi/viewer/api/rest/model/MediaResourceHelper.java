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
package io.goobi.viewer.api.rest.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.model.security.clients.ClientApplicationManager;
import io.goobi.viewer.model.variables.VariableReplacer;

/**
 * Helper class for serving media resources via the REST API, handling content-type detection,
 * redirect decisions, and HTTP response header configuration based on viewer media-type settings.
 */
public class MediaResourceHelper {

    private static final Logger logger = LogManager.getLogger(MediaResourceHelper.class);

    private static final String MIME_TYPE_PDF = "application/pdf";

    private final Configuration config;

    public MediaResourceHelper(Configuration config) {
        this.config = config;
    }

    public boolean shouldRedirect(String filename) throws IOException {
        String mimeType = Files.probeContentType(Path.of(filename));
        String contentDisposition = config.getMediaTypeHandling(mimeType);
        return "redirect".equals(contentDisposition);
    }

    public String getRedirectUrl(String pi, String filename) throws IOException {
        String mimeType = Files.probeContentType(Path.of(filename));
        String urlTemplate = config.getMediaTypeRedirectUrl(mimeType);
        VariableReplacer vr = new VariableReplacer(config);
        vr.addReplacement("pi", pi);
        vr.addReplacement("filename", filename);
        String url = vr.replace(urlTemplate).stream().findFirst().orElse(urlTemplate);
        return url;
    }

    public String setContentHeaders(HttpServletResponse servletResponse, String filename, Path path) throws IOException {
        return setContentHeaders(null, servletResponse, filename, path, false);
    }

    /**
     * Sets content type and content disposition headers for the given file. Requests made by a client application get a plain inline
     * disposition for PDF files, or for all files if inlineForAllTypes is true; all other requests get the disposition configured for the
     * file's mime type.
     *
     * @param servletRequest request used to detect client applications; may be null
     * @param servletResponse response to set the headers on
     * @param filename file name announced in an attachment disposition
     * @param path path of the delivered file; must be within the viewer home folder
     * @param inlineForAllTypes if true, client applications get an inline disposition regardless of the mime type
     * @return the probed mime type of the file
     * @throws IOException
     * @should set inline disposition for pdf if requested by client application
     * @should set inline disposition for any type if requested by client application and inlineForAllTypes is true
     * @should set configured disposition for non pdf if requested by client application and inlineForAllTypes is false
     * @should set configured disposition for regular request
     */
    public String setContentHeaders(HttpServletRequest servletRequest, HttpServletResponse servletResponse, String filename, Path path,
            boolean inlineForAllTypes) throws IOException {
        Path viewerBase = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome());
        if (path == null) {
            throw new IllegalArgumentException("path may not be null");
        }
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (!normalizedPath.startsWith(viewerBase)) {
            throw new SecurityException("Illegal path: " + normalizedPath);
        }

        String mimeType = Files.probeContentType(normalizedPath);
        logger.trace("content type: {}", mimeType);
        if (StringUtils.isNotBlank(mimeType)) {
            servletResponse.setContentType(mimeType);
            servletResponse.setHeader(NetTools.HTTP_HEADER_CONTENT_TYPE, mimeType);
        }
        if (ClientApplicationManager.isClientApplicationRequest(servletRequest) && (inlineForAllTypes || MIME_TYPE_PDF.equals(mimeType))) {
            servletResponse.setHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION, NetTools.HTTP_HEADER_VALUE_INLINE);
            return mimeType;
        }
        String contentDisposition = config.getMediaTypeHandling(mimeType);
        switch (contentDisposition) {
            case "attachment":
                servletResponse.setHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION,
                        NetTools.buildAttachmentContentDisposition(filename));
                servletResponse.setHeader(NetTools.HTTP_HEADER_CONTENT_LENGTH, String.valueOf(Files.size(normalizedPath)));
                break;
            default:
                servletResponse.setHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION, contentDisposition);
        }
        return mimeType;
    }

}
