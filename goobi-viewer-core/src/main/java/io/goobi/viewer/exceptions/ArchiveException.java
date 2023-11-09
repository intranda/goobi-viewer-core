package io.goobi.viewer.exceptions;

public abstract class ArchiveException extends PresentationException {

    public ArchiveException(String message, String resourceName, String resourceLocation, Throwable e) {
        super(createMessage(message, resourceName, resourceLocation), e);
    }

    public ArchiveException(String message, String resourceName, String resourceLocation) {
        super(createMessage(message, resourceName, resourceLocation));
    }
    
    protected static String createMessage(String message, String...replacements) {
        int i = 0;
        while(message.contains("{}") && i < replacements.length) {
            message = message.replaceFirst("\\{\\}", replacements[i]);
            i++;
        }
        return message;
    }
    

}
