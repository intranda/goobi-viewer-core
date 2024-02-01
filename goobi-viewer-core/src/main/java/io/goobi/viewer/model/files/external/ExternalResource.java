package io.goobi.viewer.model.files.external;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.Future;

public class ExternalResource {
    
    private final URI exteralResourceUri;
    private final ExternalFilesDownloader downloader;
    private final boolean exists;
    
    public ExternalResource(URI exteralResourceUri, ExternalFilesDownloader downloader) {
        this.exteralResourceUri = exteralResourceUri;
        this.downloader = downloader;
        this.exists = checkExistance();
    }
    
    private boolean checkExistance() {
        return downloader.resourceExists(exteralResourceUri);
    }
    
    public boolean exists() {
        return exists;
    }
    
    public Future<ExternalResource> downloadResource() throws IOException {
        this.downloader.downloadExternalFiles(exteralResourceUri);
        return null;
    }
        
}
