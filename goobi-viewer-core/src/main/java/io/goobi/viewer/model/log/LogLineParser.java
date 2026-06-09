package io.goobi.viewer.model.log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Parses raw log4j2 output into structured LogLine objects. Supports both formats: New: %-5level %d{...} [%thread] %location - %msg Old: %-5level
 * %d{...} [%thread] %location (message on next line) Stacktrace/continuation lines are appended to the preceding entry's message.
 */
public final class LogLineParser {

    static final Pattern LINE_START = Pattern.compile(
            "^(ERROR|WARN |INFO |DEBUG|TRACE)\\s++(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s++\\[([^\\]]*+)\\]\\s++"
                    + "(\\S++)(?:\\s++-\\s++(.*))?$",
            Pattern.MULTILINE);

    private LogLineParser() {
    }

    public static List<LogLine> parse(String raw) {
        List<LogLine> result = new ArrayList<>();
        if (StringUtils.isBlank(raw)) {
            return result;
        }

        String[] lines = raw.split("\\r?\\n");
        String currentLevel = null, currentTimestamp = null, currentThread = null, currentLocation = null;
        StringBuilder currentMessage = new StringBuilder();

        for (String line : lines) {
            Matcher m = LINE_START.matcher(line);
            if (m.matches()) {
                if (currentLevel != null) {
                    result.add(new LogLine(
                            currentTimestamp, currentLevel.strip(),
                            currentThread, currentLocation, currentMessage.toString().strip()));
                }
                currentLevel = m.group(1);
                currentTimestamp = m.group(2);
                currentThread = m.group(3);
                currentLocation = m.group(4);
                String msg = m.group(5);
                currentMessage = new StringBuilder(msg != null ? msg : "");
            } else if (currentLevel != null) {
                if (currentMessage.length() > 0) {
                    currentMessage.append("\n");
                }
                currentMessage.append(line.stripLeading());
            }
        }
        if (currentLevel != null) {
            result.add(new LogLine(
                    currentTimestamp, currentLevel.strip(),
                    currentThread, currentLocation, currentMessage.toString().strip()));
        }
        return result;
    }

    /**
     * Returns true if the given line matches a log entry header. Used by the stateful tailer to detect entry boundaries.
     */
    public static boolean isHeaderLine(String line) {
        return line != null && LINE_START.matcher(line).matches();
    }
}
