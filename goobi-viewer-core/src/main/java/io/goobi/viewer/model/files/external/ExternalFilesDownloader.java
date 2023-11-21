package io.goobi.viewer.model.files.external;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.controller.DataManager;

public class ExternalFilesDownloader {

    private static final Logger logger = LogManager.getLogger(ExternalFilesDownloader.class);

    private static final int REQUEST_TIMEOUT_MILLIS = 4000;
    private static final int SOCKET_TIMEOUT_MILLIS = 4000;
    private static final int CONNECTION_TIMEOUT_MILLIS = 4000;
    private static final int HEAD_REQUEST_TIMEOUT_MILLIS = 200;
    private static final int HEAD_SOCKET_TIMEOUT_MILLIS = 200;
    private static final int HEAD_CONNECTION_TIMEOUT_MILLIS = 200;

    private final Path destinationFolder;

    public ExternalFilesDownloader(Path destinationdFolder) {
        this.destinationFolder = destinationdFolder;
    }

    public DownloadResult downloadExternalFiles(URI downloadUri) throws IOException {

        return downloadFromUrl(downloadUri);

    }

    public boolean resourceExists(URI uri) {
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

    private boolean checkHttpResource(URI uri) throws IOException {
        try (final CloseableHttpClient client = createHttpClient()) {
            try (final CloseableHttpResponse response = createHttpHeadResponse(client, uri)) {
                return Status.OK.getStatusCode() == response.getStatusLine().getStatusCode();
            }
        }
    }

    private boolean checkFileResource(URI uri) {
        Path sourcePath = PathConverter.getPath(uri);
        return Files.exists(sourcePath);
    }

    private DownloadResult downloadFromUrl(URI uri) throws IOException {
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

    private DownloadResult downloadFileResource(URI uri) throws IOException {
        Path sourcePath = PathConverter.getPath(uri);
        if (Files.exists(sourcePath)) {
            Path target = this.destinationFolder.resolve(sourcePath.getFileName());
            InputStream in = Files.newInputStream(sourcePath);
            return extractContentToPath(target, in, Files.probeContentType(sourcePath), Files.size(sourcePath), List.of(in));
        } else {
            throw new IOException("No file resource found at " + uri);
        }
    }

    public DownloadResult downloadHttpResource(URI uri) throws IOException, ClientProtocolException {
        final CloseableHttpClient client = createHttpClient();
        final CloseableHttpResponse response = createHttpGetResponse(client, uri);
        final int statusCode = response.getStatusLine().getStatusCode();
        switch (statusCode) {
            case HttpServletResponse.SC_OK:
                String filename = getFilename(uri, response);
                if (response.getEntity() != null) {
                    DownloadResult result = extractContentToPath(this.destinationFolder.resolve(filename), response.getEntity().getContent(),
                            response.getEntity().getContentType().getValue(), response.getEntity().getContentLength(), List.of(response, client));
                    return result;
                }
            case 401:
            default:
                logger.warn("Error code: {}", response.getStatusLine().getStatusCode());
                throw new IOException(response.getStatusLine().getReasonPhrase());
        }
    }

    private DownloadResult extractContentToPath(Path destination, InputStream input, String contentMimeType, long size, List<Closeable> toClose)
            throws IOException {
        switch (contentMimeType) {
            case "application/zip":
                Path targetFolder = prepareNewFolder(destination);
                logger.trace("Writing to output folder {}", destination);
                ProgressInputStream monitored = new ProgressInputStream(input, size, Optional.empty());
                ZipInputStream zis = new ZipInputStream(monitored);
                Future<Path> future =
                        DataManager.getInstance()
                                .getThreadPoolManager()
                                .getExecutorService()
                                .submit(() -> extractZip(targetFolder, zis, ListUtils.union(List.of(zis, monitored), toClose)));
                return new DownloadResult(monitored.getMonitor(), future, size);
            default://assume normal file
                monitored = new ProgressInputStream(input, size, Optional.empty());
                future = DataManager.getInstance()
                                .getThreadPoolManager()
                                .getExecutorService()
                                .submit(() -> writeFile(destination, monitored, ListUtils.union(List.of(monitored), toClose)));
                return new DownloadResult(monitored.getMonitor(), future, size);
        }
    }

    public Path extractZip(Path destination, ZipInputStream zis, List<Closeable> toClose) throws IOException {
        ZipEntry entry = null;
        try {
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                Path entryFile = destination.resolve(entry.getName());
                if (entry.isDirectory()) {
                    logger.trace("Creating directory {}", entryFile);
                    Files.createDirectory(entryFile);
                } else {
                    logger.trace("Writing file {}", entryFile);
                    if (!Files.isDirectory(entryFile.getParent())) {
                        Files.createDirectories(entryFile.getParent());
                    }
                    writeFile(entryFile, zis, Collections.emptyList());
                }
            }
            return destination;
        } finally {
            for (Closeable closeable : toClose) {
                closeable.close();
            }
        }
    }

    private Path writeFile(Path entryFile, InputStream zis, List<Closeable> toClose) throws IOException {
        try {
            Files.deleteIfExists(entryFile);
            Files.copy(zis, entryFile);
            return entryFile;
        } finally {
            for (Closeable closeable : toClose) {
                closeable.close();
            }
            }
    }

    private String getFilename(URI uri, CloseableHttpResponse response) {
        String header = Optional.ofNullable(response).map(r -> r.getFirstHeader("content-disposition")).map(Header::getValue).orElse("");
        if (StringUtils.isNotBlank(header) && header.contains("filename=")) {
            Matcher matcher = Pattern.compile("filename=(.+?)(;|$)").matcher(header);
            if (matcher.find() && StringUtils.isNotBlank(matcher.group(1))) {
                return matcher.group(1);
            }
        }
        return Path.of(uri.getPath()).getFileName().toString();

    }

    private Path prepareNewFolder(Path destination) throws IOException {
        Path path = destination.getParent().resolve(FileNameUtils.getBaseName(destination.getFileName()));
        if (Files.exists(path)) {
            FileUtils.deleteDirectory(path.toFile());
        }
        Files.createDirectories(path);
        return path;
    }

    private static CloseableHttpClient createHttpClient() {
        return HttpClients.custom().build();
    }

    private static CloseableHttpResponse createHttpHeadResponse(CloseableHttpClient client, URI uri) throws ClientProtocolException, IOException {
        HttpHead request = new HttpHead(uri);
        RequestConfig config = RequestConfig.custom()
                .setConnectionRequestTimeout(HEAD_REQUEST_TIMEOUT_MILLIS)
                .setSocketTimeout(HEAD_SOCKET_TIMEOUT_MILLIS)
                .setConnectTimeout(HEAD_CONNECTION_TIMEOUT_MILLIS)
                .build();
        request.setConfig(config);
        return client.execute(request);
    }

    private static CloseableHttpResponse createHttpGetResponse(CloseableHttpClient client, URI uri) throws ClientProtocolException, IOException {
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
