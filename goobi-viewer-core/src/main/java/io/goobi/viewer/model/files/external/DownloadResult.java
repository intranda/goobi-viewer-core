package io.goobi.viewer.model.files.external;

import java.nio.file.Path;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class DownloadResult {

    private final Supplier<Long> progressMonitor;
    private final Future<Path> path;
    private final long size;
    
    public DownloadResult(Supplier<Long> progressMonitor, Future<Path> resultPath, long size) {
        super();
        this.progressMonitor = progressMonitor;
        this.path = resultPath;
        this.size = size;
    }

    public Supplier<Long> getProgressMonitor() {
        return progressMonitor;
    }

    public Future<Path> getPath() {
        return path;
    }
    
    public long getSize() {
        return size;
    }
    
    public long getProgressPercent() {
        return 100*progressMonitor.get()/size;
    }
    
}
