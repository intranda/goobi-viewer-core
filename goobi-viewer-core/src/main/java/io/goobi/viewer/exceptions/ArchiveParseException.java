package io.goobi.viewer.exceptions;

public class ArchiveParseException extends ArchiveException {

    public ArchiveParseException(String message, String resourceName, String resourceLocation, Throwable e) {
        super(message, resourceName, resourceLocation, e);
    }

    public ArchiveParseException(String message, String resourceName, String resourceLocation) {
        super(message, resourceName, resourceLocation);
    }

}
