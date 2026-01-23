package io.goobi.viewer.api.rest.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.model.variables.VariableReplacer;

public class MediaResourceHelper {

    private static final Logger logger = LogManager.getLogger(MediaResourceHelper.class);

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
        String contentDisposition = config.getMediaTypeHandling(mimeType);
        switch (contentDisposition) {
            case "attachment":
                servletResponse.setHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION,
                        new StringBuilder("attachment;filename=").append(filename).toString());
                servletResponse.setHeader(NetTools.HTTP_HEADER_CONTENT_LENGTH, String.valueOf(Files.size(normalizedPath)));
                break;
            default:
                servletResponse.setHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION, contentDisposition);
        }
        return mimeType;
    }

}
