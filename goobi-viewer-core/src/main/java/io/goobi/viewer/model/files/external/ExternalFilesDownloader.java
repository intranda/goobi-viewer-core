package io.goobi.viewer.model.files.external;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringTools;

public class ExternalFilesDownloader {

    private static PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    private static int REQUEST_TIMEOUT_MILLIS = 4000;
    private static int SOCKET_TIMEOUT_MILLIS = 4000;
    private static int CONNECTION_TIMEOUT_MILLIS = 4000;
    
    private final Path destinationFolder;
    
    public ExternalFilesDownloader(Path destinationdFolder) {
        this.destinationFolder = destinationdFolder;
    }
    
    public Path downloadExternalFiles(URI downloadUri) {
        
        NetTools.callUrlGET(null)
        
    }
    
    private void downloadFromUrl(URI uri) {
        // logger.trace("callUrlGET: {}", url);
        try (final CloseableHttpClient client = createHttpClient()) {
            try (final CloseableHttpResponse response = createHttpResponse(client, uri)) {
                final int statusCode = response.getStatusLine().getStatusCode();
                switch (statusCode) {
                    case HttpServletResponse.SC_OK:
                        extractContentToPath(this.destinationFolder, response.getEntity().getContent(), response.getEntity().getContentType().getValue());
                        break;
                    case 401:
                        logger.warn("Error code: {}", response.getStatusLine().getStatusCode());
                        ret[1] = response.getStatusLine().getReasonPhrase();
                        break;
                    default:
                        // logger.warn("Error code: {}", response.getStatusLine().getStatusCode());
                        ret[1] = response.getStatusLine().getReasonPhrase();
                        break;
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        return ret;
    }
    
    private void extractContentToPath(Path destination, InputStream input, String contentMimeType) throws IOException {
        switch(contentMimeType) {
            case "application/zip":
                try(GZIPInputStream gis = new GZIPInputStream(input)) {
                    
                }
        }
    }

    private static CloseableHttpClient createHttpClient() {
        return HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setRedirectStrategy(new LaxRedirectStrategy())
                .build();
    }
    
    private static CloseableHttpResponse createHttpResponse(CloseableHttpClient client, URI uri) throws ClientProtocolException, IOException {
        HttpHead request = new HttpHead(uri);
        RequestConfig config = RequestConfig.custom().setConnectionRequestTimeout(REQUEST_TIMEOUT_MILLIS).setSocketTimeout(SOCKET_TIMEOUT_MILLIS).setConnectTimeout(CONNECTION_TIMEOUT_MILLIS).build();
        request.setConfig(config);
        return client.execute(request);
    }
}
