package io.goobi.viewer.model.job.download;

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.model.files.external.Progress;

public class ExternalFilesDownloadJob  {

    private final Progress progress;
    private final String identifier;
    private final Path path;
    private final String messageId;
    private final String errorMessage;
    
    public ExternalFilesDownloadJob(String identifier, String messageId, String errorMessage) {
        super();
        this.progress = new Progress(0, 1);
        this.identifier = identifier;
        this.path = Path.of("");
        this.messageId = messageId;
        this.errorMessage = errorMessage;
    }
    
    public ExternalFilesDownloadJob(Progress progress, String identifier, Path path, String messageId) {
        super();
        this.progress = progress;
        this.identifier = identifier;
        this.path = path;
        this.messageId = messageId;
        this.errorMessage = "";
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
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public boolean isError() {
        return StringUtils.isNotBlank(errorMessage);
    }

}
