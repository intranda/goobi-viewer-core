package io.goobi.viewer.model.log;

import java.nio.file.Path;
import java.util.Optional;

import io.goobi.viewer.controller.DataManager;

/**
 * Enum of allowed log files. The actual path is read from config_viewer.xml.
 * Clients only ever send the symbolic name — never a file path.
 * This prevents path traversal attacks structurally.
 */
public enum LogFile {

    VIEWER("viewer"),
    OAI("oai"),
    ICS("ics"),
    INDEXER("indexer");

    private final String name;

    LogFile(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the configured absolute path for this log file.
     * Returns empty if no path is configured in config_viewer.xml.
     */
    public Optional<Path> getPath() {
        String configured = DataManager.getInstance().getConfiguration().getLogViewerFilePath(name);
        if (configured == null || configured.isBlank()) return Optional.empty();
        return Optional.of(Path.of(configured));
    }

    /**
     * Maps a client-supplied name to the corresponding enum constant.
     * Returns empty for any unknown or null name — never throws.
     */
    public static Optional<LogFile> fromName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        for (LogFile lf : values()) {
            if (lf.name.equalsIgnoreCase(name)) return Optional.of(lf);
        }
        return Optional.empty();
    }
}
