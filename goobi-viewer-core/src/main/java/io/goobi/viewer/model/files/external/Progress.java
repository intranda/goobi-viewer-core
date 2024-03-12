package io.goobi.viewer.model.files.external;

public class Progress {

    private final long totalSize;
    private final long progress;

    public Progress(long progress, long totalSize) {
        this.totalSize = totalSize;
        this.progress = progress;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public long getProgressAbsolute() {
        return this.progress;
    }

    public double getProgressRelative() {
        return this.progress / (double) this.totalSize;
    }

    public int getProgressPercentage() {
        return (int) (100 * progress / this.totalSize);
    }

    public boolean started() {
        return this.progress > 0;
    }

    public boolean complete() {
        return this.progress >= this.totalSize;
    }

    @Override
    public String toString() {
        return String.format("Progress: %s/%s", this.progress, this.totalSize);
    }

}
