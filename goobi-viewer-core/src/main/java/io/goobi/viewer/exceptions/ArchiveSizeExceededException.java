package io.goobi.viewer.exceptions;

public class ArchiveSizeExceededException extends PresentationException {

    private static final long serialVersionUID = -8493147783169699313L;

    public ArchiveSizeExceededException(String string, Throwable e) {
        super(string, e);
    }

    public ArchiveSizeExceededException(String string) {
        super(string);
    }

    public ArchiveSizeExceededException(String string, Object... args) {
        super(string, args);
        // TODO Auto-generated constructor stub
    }

    public ArchiveSizeExceededException(Throwable e, String string, Object... args) {
        super(e, string, args);
        // TODO Auto-generated constructor stub
    }

}
