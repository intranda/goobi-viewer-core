package io.goobi.viewer.model.job.download;

import java.nio.file.Path;

import io.goobi.viewer.model.files.external.Progress;

public class ExternalFilesDownloadJob  {

    private final Progress progress;
    private final String identifier;
    private final Path path;
    private final String messageId;
    
    public ExternalFilesDownloadJob(Progress progress, String identifier, Path path, String messageId) {
        super();
        this.progress = progress;
        this.identifier = identifier;
        this.path = path;
        this.messageId = messageId;
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
    
    public String getMessageId() {
        return messageId;
    }
    
    

}
