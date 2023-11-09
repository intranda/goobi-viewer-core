package io.goobi.viewer.exceptions;

public class ArchiveConnectionException extends ArchiveException {

    public ArchiveConnectionException(String message, String resourceName, String resourceLocation, Throwable e) {
        super(message, resourceName, resourceLocation, e);
    }

    public ArchiveConnectionException(String message, String resourceName, String resourceLocation) {
        super(message, resourceName, resourceLocation);
    }

}
