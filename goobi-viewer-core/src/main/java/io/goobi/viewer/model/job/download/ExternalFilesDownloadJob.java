package io.goobi.viewer.model.job.download;

import java.nio.file.Path;

import io.goobi.viewer.model.files.external.Progress;

public class ExternalFilesDownloadJob  {

    private final Progress progress;
    private final String identifier;
    private final Path path;
    
    public ExternalFilesDownloadJob(Progress progress, String identifier, Path path) {
        super();
        this.progress = progress;
        this.identifier = identifier;
        this.path = path;
    }
    
    public Progress getProgress() {
        return progress;
    }
    
    public String getIdentifier() {
        return identifier;
    }
    
    public Path getPath() {
        return path;
    }
    
    

}
