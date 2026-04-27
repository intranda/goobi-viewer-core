
package io.goobi.viewer.connector;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import io.goobi.viewer.connector.utils.Utils;

/**
 * <p>
 * Version class.
 * </p>
 *
 */
public final class Version {
    /** Constant <code>APPLICATION</code> */
    public static final String APPLICATION;
    /** Constant <code>VERSION</code> */
    public static final String VERSION;
    /** Constant <code>BUILDVERSION</code> */
    public static final String BUILDVERSION;
    /** Constant <code>BUILDDATE</code> */
    public static final String BUILDDATE;

    static {
        String manifest = getManifestStringFromJar();
        if (StringUtils.isNotBlank(manifest)) {
            APPLICATION = getInfo("ApplicationName", manifest);
            VERSION = getInfo("version", manifest);
            BUILDDATE = getInfo("Implementation-Build-Date", manifest);
            BUILDVERSION = getInfo("Implementation-Version", manifest);
        } else {
            APPLICATION = "goobi-viewer-connector";
            VERSION = "unknown";
            BUILDDATE = LocalDateTime.now().format(Utils.FORMATTER_ISO8601_DATETIME_NO_SECONDS);
            BUILDVERSION = "unknown";
        }
    }

    /** Private constructor */
    private Version() {
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
        try (InputStream inputStream = new URI(manifestPath).toURL().openStream()) {
            StringWriter writer = new StringWriter();
            IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
            String manifestString = writer.toString();
            value = manifestString;
        } catch (IOException | URISyntaxException e) {
            return null;
        }

        return value;
    }

    private static String getInfo(String label, String infoText) {
        String regex = label + ": *(.*)";
        Matcher matcher = Pattern.compile(regex).matcher(infoText);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "?";
    }

    public static String asJSON() {
        return new JSONObject().put("application", APPLICATION)
                .put("version", VERSION)
                .put("build-date", BUILDDATE)
                .put("git-revision", BUILDVERSION)
                .toString();
    }

}
