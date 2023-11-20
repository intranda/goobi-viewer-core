package io.goobi.viewer.model.files.external;

import java.nio.file.Path;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class DownloadResult {

    private final Supplier<Long> progressMonitor;
    private final Future<Path> path;
    
    public DownloadResult(Supplier<Long> progressMonitor, Future<Path> resultPath) {
        super();
        this.progressMonitor = progressMonitor;
        this.path = resultPath;
    }

    public Supplier<Long> getProgressMonitor() {
        return progressMonitor;
    }

    public Future<Path> getPath() {
        return path;
    }
    
    
}
