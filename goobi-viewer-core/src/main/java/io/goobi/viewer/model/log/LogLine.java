package io.goobi.viewer.model.log;

/**
 * Immutable representation of a single parsed log line.
 */
public record LogLine(String timestamp, String level, String location, String message) {

    public String toJson() {
        return "{\"timestamp\":\"" + escape(timestamp) + "\","
             + "\"level\":\"" + escape(level) + "\","
             + "\"location\":\"" + escape(location) + "\","
             + "\"message\":\"" + escape(message) + "\"}";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
