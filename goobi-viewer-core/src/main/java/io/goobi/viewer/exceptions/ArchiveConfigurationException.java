package io.goobi.viewer.exceptions;

public class ArchiveConfigurationException extends ArchiveException {

    public ArchiveConfigurationException(String message, String resourceName, String resourceLocation, Throwable e) {
        super(message, resourceName, resourceLocation, e);
    }

    public ArchiveConfigurationException(String message, String resourceName, String resourceLocation) {
        super(message, resourceName, resourceLocation);
    }
}
