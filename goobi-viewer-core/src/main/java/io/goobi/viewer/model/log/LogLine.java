package io.goobi.viewer.model.log;

/**
 * Immutable representation of a single parsed log line.
 *
 * @param timestamp ISO-formatted timestamp string from the log entry
 * @param level     log level (INFO, WARN, ERROR, DEBUG, TRACE)
 * @param thread    name of the thread that produced the log entry
 * @param location  source location (logger name or class) of the log entry
 * @param message   message body, including any stacktrace continuation lines
 */
public record LogLine(String timestamp, String level, String thread, String location, String message) {

    public String toJson() {
        return "{\"timestamp\":\"" + escape(timestamp) + "\","
             + "\"level\":\"" + escape(level) + "\","
             + "\"thread\":\"" + escape(thread) + "\","
             + "\"location\":\"" + escape(location) + "\","
             + "\"message\":\"" + escape(message) + "\"}";
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
