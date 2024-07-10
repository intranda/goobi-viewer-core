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
package io.goobi.viewer.model.files.external;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.controller.files.ZipUnpacker;
import io.goobi.viewer.exceptions.ArchiveSizeExceededException;

public class ExternalFilesDownloader {

    private static final Logger logger = LogManager.getLogger(ExternalFilesDownloader.class);

    private static final int REQUEST_TIMEOUT_MILLIS = 4000;
    private static final int SOCKET_TIMEOUT_MILLIS = 4000;
    private static final int CONNECTION_TIMEOUT_MILLIS = 4000;
    private static final int HEAD_REQUEST_TIMEOUT_MILLIS = 2000;
    private static final int HEAD_SOCKET_TIMEOUT_MILLIS = 2000;
    private static final int HEAD_CONNECTION_TIMEOUT_MILLIS = 2000;

    private final Path destinationFolder;
    private final Consumer<Progress> progressMonitor;

    public ExternalFilesDownloader(Path destinationdFolder, Consumer<Progress> progressMonitor) {
        this.destinationFolder = destinationdFolder;
        this.progressMonitor = progressMonitor;
    }

    public Path downloadExternalFiles(URI downloadUri) throws IOException {

        return downloadFromUrl(downloadUri);

    }

    public static boolean resourceExists(String url) {
        try {
            return resourceExists(new URI(url));
        } catch (URISyntaxException e) {
            logger.error("Error checking resource at {}. Not a valid url", url);
            return false;
        }
    }

    public static boolean resourceExists(URI uri) {
        logger.trace("checking url {}", uri);
        switch (uri.getScheme()) {
            case "http":
            case "https":
                try {
                    return checkHttpResource(uri);
                } catch (IOException e) {
                    logger.debug("Checking http resource {} resulted in IOException {}", uri, e.toString());
                    return false;
                }
            case "file":
                return checkFileResource(uri);
            default:
                throw new IllegalArgumentException("Cannot check " + uri + ": No implementation for scheme " + uri.getScheme());
        }
    }

    private static boolean checkHttpResource(URI uri) throws IOException {
        try (final CloseableHttpClient client = createHttpClient()) {
            try (final CloseableHttpResponse response = createHttpHeadResponse(client, uri)) {
                return Status.OK.getStatusCode() == response.getStatusLine().getStatusCode();
            }
        }
    }

    private static boolean checkFileResource(URI uri) {
        Path sourcePath = PathConverter.getPath(uri);
        return Files.exists(sourcePath);
    }

    private Path downloadFromUrl(URI uri) throws IOException {
        logger.trace("download from url {}", uri);
        switch (uri.getScheme()) {
            case "http":
            case "https":
                return downloadHttpResource(uri);
            case "file":
                return downloadFileResource(uri);
            default:
                throw new IllegalArgumentException("Cannot download from " + uri + ": No download implementation for scheme " + uri.getScheme());
        }

    }

    private Path downloadFileResource(URI uri) throws IOException {
        Path sourcePath = PathConverter.getPath(uri);
        if (Files.exists(sourcePath)) {
            Path target = this.destinationFolder.resolve(sourcePath.getFileName());
            try (InputStream in = Files.newInputStream(sourcePath)) {
                return extractContentToPath(target, in, Files.probeContentType(sourcePath), Files.size(sourcePath));
            } catch (ArchiveSizeExceededException e) {
                throw new IOException(
                        "Aborted extraction of archive at " + uri + " because of maximum archive size violation: " + e.getMessage());
            }
        }
        throw new IOException("No file resource found at " + uri);
    }

    public Path downloadHttpResource(URI uri) throws IOException {
        final CloseableHttpClient client = createHttpClient();
        final CloseableHttpResponse response = createHttpGetResponse(client, uri);
        final int statusCode = response.getStatusLine().getStatusCode();
        switch (statusCode) {
            case HttpServletResponse.SC_OK:
                String filename = getFilename(uri, response);
                if (response.getEntity() != null) {
                    long length = response.getEntity().getContentLength();
                    try {
                        return extractContentToPath(this.destinationFolder.resolve(filename), response.getEntity().getContent(),
                                response.getEntity().getContentType().getValue(), length);
                    } catch (ArchiveSizeExceededException e) {
                        throw new IOException(
                                "Aborted extraction of archive at " + uri + " because of maximum archive size violation: " + e.getMessage());
                    }
                }
            case 401:
            default:
                logger.warn("Error code: {}", response.getStatusLine().getStatusCode());
                throw new IOException(response.getStatusLine().getReasonPhrase());
        }
    }

    private Path extractContentToPath(Path destination, InputStream input, String contentMimeType, long size)
            throws IOException, ArchiveSizeExceededException {
        switch (contentMimeType) {
            case "application/zip":
                Path targetFolder = prepareNewFolder(destination);
                logger.trace("Writing to output folder {}", destination);
                try (ProgressInputStream monitored = new ProgressInputStream(input, size, Optional.of(this.progressMonitor));
                        ZipInputStream zis = new ZipInputStream(monitored)) {
                    return new ZipUnpacker().extractZip(targetFolder, zis);
                }
            default://assume normal file
                prepareNewFolder(destination.getParent());
                try (ProgressInputStream monitored = new ProgressInputStream(input, size, Optional.of(this.progressMonitor))) {
                    return writeFile(destination, monitored);
                }
        }
    }

    private static Path writeFile(Path entryFile, InputStream zis) throws IOException {
        Files.deleteIfExists(entryFile);
        Files.copy(zis, entryFile);
        return entryFile;
    }

    private static String getFilename(URI uri, CloseableHttpResponse response) {
        String header = Optional.ofNullable(response).map(r -> r.getFirstHeader("content-disposition")).map(Header::getValue).orElse("");
        if (StringUtils.isNotBlank(header) && header.contains("filename=")) {
            Matcher matcher = Pattern.compile("filename=(.+?)(;|$)").matcher(header);
            if (matcher.find() && StringUtils.isNotBlank(matcher.group(1))) {
                return matcher.group(1);
            }
        }
        return Path.of(uri.getPath()).getFileName().toString();

    }

    private static Path prepareNewFolder(Path destination) throws IOException {
        Path path = destination.getParent().resolve(FilenameUtils.getBaseName(destination.getFileName().toString()));
        if (Files.exists(path)) {
            FileUtils.deleteDirectory(path.toFile());
        }
        Files.createDirectories(path);
        return path;
    }

    private static CloseableHttpClient createHttpClient() {
        return HttpClients.custom().build();
    }

    private static CloseableHttpResponse createHttpHeadResponse(CloseableHttpClient client, URI uri) throws IOException {
        HttpHead request = new HttpHead(uri);
        RequestConfig config = RequestConfig.custom()
                .setConnectionRequestTimeout(HEAD_REQUEST_TIMEOUT_MILLIS)
                .setSocketTimeout(HEAD_SOCKET_TIMEOUT_MILLIS)
                .setConnectTimeout(HEAD_CONNECTION_TIMEOUT_MILLIS)
                .build();
        request.setConfig(config);
        return client.execute(request);
    }

    private static CloseableHttpResponse createHttpGetResponse(CloseableHttpClient client, URI uri) throws IOException {
        HttpGet request = new HttpGet(uri);
        RequestConfig config = RequestConfig.custom()
                .setConnectionRequestTimeout(REQUEST_TIMEOUT_MILLIS)
                .setSocketTimeout(SOCKET_TIMEOUT_MILLIS)
                .setConnectTimeout(CONNECTION_TIMEOUT_MILLIS)
                .build();
        request.setConfig(config);
        return client.execute(request);
    }

}
