package io.goobi.viewer.exceptions;

public abstract class ArchiveException extends PresentationException {

    private static final long serialVersionUID = -6999584810651228875L;

    /**
     * 
     * @param message
     * @param resourceName
     * @param resourceLocation
     * @param e
     */
    protected ArchiveException(String message, String resourceName, String resourceLocation, Throwable e) {
        super(createMessage(message, resourceName, resourceLocation), e);
    }

    /**
     * 
     * @param message
     * @param resourceName
     * @param resourceLocation
     */
    protected ArchiveException(String message, String resourceName, String resourceLocation) {
        super(createMessage(message, resourceName, resourceLocation));
    }

    /**
     * 
     * @param message
     * @param replacements
     * @return Updated message
     */
    protected static String createMessage(String message, String... replacements) {
        int i = 0;
        String msg = message;
        while (msg.contains("{}") && i < replacements.length) {
            msg = msg.replaceFirst("\\{\\}", replacements[i]);
            i++;
        }
        return msg;
    }
}
