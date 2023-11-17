package io.goobi.viewer.model.files.external;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletResponse;

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
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExternalFilesDownloader {

    private static final Logger logger = LogManager.getLogger(ExternalFilesDownloader.class);

    private static PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    private static final int REQUEST_TIMEOUT_MILLIS = 4000;
    private static final int SOCKET_TIMEOUT_MILLIS = 4000;
    private static final int CONNECTION_TIMEOUT_MILLIS = 4000;

    private final Path destinationFolder;

    public ExternalFilesDownloader(Path destinationdFolder) {
        this.destinationFolder = destinationdFolder;
    }

    public Path downloadExternalFiles(URI downloadUri) throws IOException {

        return downloadFromUrl(downloadUri);

    }

    private Path downloadFromUrl(URI uri) throws IOException {
        logger.trace("download from url {}", uri);
        try (final CloseableHttpClient client = createHttpClient()) {
            try (final CloseableHttpResponse response = createHttpResponse(client, uri)) {
                final int statusCode = response.getStatusLine().getStatusCode();
                switch (statusCode) {
                    case HttpServletResponse.SC_OK:
                        String filename = getFilename(uri, response);
                        if(response.getEntity() != null) {                            
                            return extractContentToPath(this.destinationFolder.resolve(filename), response.getEntity().getContent(),
                                    response.getEntity().getContentType().getValue());
                        }
                    case 401:
                    default:
                        logger.warn("Error code: {}", response.getStatusLine().getStatusCode());
                        throw new IOException(response.getStatusLine().getReasonPhrase());
                }
            }
        }
    }

    private Path extractContentToPath(Path destination, InputStream input, String contentMimeType) throws IOException {
        switch (contentMimeType) {
            case "application/zip":
                destination = prepareNewFolder(destination);
                logger.trace("Writing to output folder {}", destination);
                try (ZipInputStream zis = new ZipInputStream(input)) {
                    ZipEntry entry = null;
                    while ((entry = zis.getNextEntry()) != null) {
                        String name = entry.getName();
                        Path entryFile = destination.resolve(entry.getName());
                        if (entry.isDirectory()) {
                            logger.trace("Creating directory {}", entryFile);
                            Files.createDirectory(entryFile);
                        } else {
                            logger.trace("Writing file {}", entryFile);
                            if(!Files.isDirectory(entryFile.getParent())) {
                                Files.createDirectories(entryFile.getParent());
                            }
                            writeFile(entryFile, zis);
                        }
                    }
                }
                break;
            default://assume normal file
                writeFile(destination, input);
        }
        return destination;
    }

    private void writeFile(Path entryFile, InputStream zis) throws IOException {
        Files.deleteIfExists(entryFile);
        Files.copy(zis, entryFile);
    }

    private String getFilename(URI uri, CloseableHttpResponse response) {
        String header = Optional.ofNullable(response).map(r -> r.getFirstHeader("content-disposition")).map(Header::getValue).orElse("");
        if (StringUtils.isNotBlank(header) && header.contains("filename=")) {
            Matcher matcher = Pattern.compile("filename=(.+?)(;|$)").matcher(header);
            if(matcher.find() && StringUtils.isNotBlank(matcher.group(1))) {
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
        Files.createDirectory(path);
        return path;
    }

    private static CloseableHttpClient createHttpClient() {
        return HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .build();
    }

    private static CloseableHttpResponse createHttpResponse(CloseableHttpClient client, URI uri) throws ClientProtocolException, IOException {
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
