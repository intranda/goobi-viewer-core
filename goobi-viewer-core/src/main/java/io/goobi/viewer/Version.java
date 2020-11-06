
package io.goobi.viewer;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import io.goobi.viewer.controller.DateTools;

/**
 * <p>
 * Version class.
 * </p>
 */
public class Version {
    /** Constant <code>APPLICATION_NAME</code> */
    public static final String APPLICATION_NAME;
    /** Constant <code>VERSION</code> */
    public static final String VERSION;
    /** Constant <code>PUBLIC_VERSION</code> */
    public static final String PUBLIC_VERSION;
    /** Constant <code>BUILDVERSION</code> */
    public static final String BUILDVERSION;
    /** Constant <code>BUILDDATE</code> */
    public static final String BUILDDATE;

    static {
        String manifest = getManifestStringFromJar();
        if (StringUtils.isNotBlank(manifest)) {
            APPLICATION_NAME = getInfo("ApplicationName", manifest);
            VERSION = getInfo("version", manifest);
            BUILDDATE = getInfo("Implementation-Build-Date", manifest);
            BUILDVERSION = getInfo("Implementation-Version", manifest);
            PUBLIC_VERSION = getInfo("Public-Version", manifest);
        } else {
            APPLICATION_NAME = "goobi-viewer-core";
            VERSION = "unknown";
            BUILDDATE = LocalDateTime.now().format(DateTools.formatterISO8601DateTimeNoSeconds);
            BUILDVERSION = "unknown";
            PUBLIC_VERSION = "unknown";
        }
    }

    private static String getManifestStringFromJar() {
        Class clazz = Version.class;
        String className = clazz.getSimpleName() + ".class";
        String classPath = clazz.getResource(className).toString();
        String value = null;
        String manifestPath;
        if (classPath.startsWith("jar")) {
            manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
        } else {
            manifestPath = classPath.substring(0, classPath.lastIndexOf("classes") + 7) + "/META-INF/MANIFEST.MF";
        }
        try (InputStream inputStream = new URL(manifestPath).openStream()) {
            StringWriter writer = new StringWriter();
            IOUtils.copy(inputStream, writer, "utf-8");
            String manifestString = writer.toString();
            value = manifestString;
        } catch (MalformedURLException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
        return value;
    }

    /**
     * 
     * @param label
     * @param infoText
     * @return
     * @should extract fields correctly
     */
    static String getInfo(String label, String infoText) {
        String regex = label + ": *(.*)";
        Matcher matcher = Pattern.compile(regex).matcher(infoText);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return "?";
    }

    /**
     * <p>
     * convertDate.
     * </p>
     *
     * @param inputString a {@link java.lang.String} object.
     * @param inputPattern a {@link java.lang.String} object.
     * @param outputPattern a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    @Deprecated
    public static String convertDate(String inputString, String inputPattern, String outputPattern) {
        DateTimeFormatter in = DateTimeFormatter.ofPattern(inputPattern);
        DateTimeFormatter out = DateTimeFormatter.ofPattern(outputPattern);

        TemporalAccessor time = in.parse(inputString);
        String formatted = out.format(time);
        return formatted;
    }

    /**
     * 
     * @return Version info as a single string
     */
    public static String asString() {
        return APPLICATION_NAME + " " + VERSION + " " + BUILDDATE + " " + BUILDVERSION;
    }

    /**
     * 
     * @return JSON object containing version info
     */
    public static String asJSON() {
        return new JSONObject().put("application", APPLICATION_NAME)
                .put("version", VERSION)
                .put("build-date", BUILDDATE)
                .put("git-revision", BUILDVERSION)
                .toString();
    }
}
