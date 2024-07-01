package io.goobi.viewer.api.rest.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.NetTools;

public class MediaResourceHelper {

    private static final Logger logger = LogManager.getLogger(MediaResourceHelper.class);

    private final Configuration config;

    public MediaResourceHelper(Configuration config) {
        this.config = config;
    }

    public String setContentHeaders(HttpServletResponse servletResponse, String filename, Path path) throws IOException {
        String mimeType = Files.probeContentType(path);
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
                servletResponse.setHeader(NetTools.HTTP_HEADER_CONTENT_LENGTH, String.valueOf(Files.size(path)));
                break;
            default:
                servletResponse.setHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION, contentDisposition);
        }
        return mimeType;
    }
}
