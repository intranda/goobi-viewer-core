
package io.goobi.viewer;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;


/**
 * <p>Version class.</p>
 *
 */
public class Version {
    /** Constant <code>APPLICATION_NAME</code> */
    public final static String APPLICATION_NAME;
    /** Constant <code>VERSION</code> */
    public final static String VERSION;
    /** Constant <code>PUBLIC_VERSION</code> */
    public final static String PUBLIC_VERSION;
    /** Constant <code>BUILDVERSION</code> */
    public final static String BUILDVERSION;
    /** Constant <code>BUILDDATE</code> */
    public final static String BUILDDATE;
    
    private static final String MANIFEST_DATE_PATTERN = "yyyy-MM-dd_HH-mm";

    static {
        String manifest = getManifestStringFromJar();
        if (StringUtils.isNotBlank(manifest)) {
            APPLICATION_NAME = getInfo("ApplicationName", manifest);
            VERSION = getInfo("version", manifest);
            BUILDDATE = getInfo("Implementation-Build-Date", manifest);
            BUILDVERSION = getInfo("Implementation-Version", manifest);
            PUBLIC_VERSION = getInfo("PublicVersion", manifest);
        } else {
            APPLICATION_NAME = "goobi-viewer-core";
            VERSION = "unknown";
            BUILDDATE = new Date().toString();
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

    private static String getInfo(String label, String infoText) {
        String regex = label + ": *(\\S*)";
        Matcher matcher = Pattern.compile(regex).matcher(infoText);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "?";
        }
    }
    
    /**
     * <p>getBuildDate.</p>
     *
     * @param pattern a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getBuildDate(String pattern) {
        return convertDate(BUILDDATE, MANIFEST_DATE_PATTERN, pattern);
    }

    /**
     * <p>convertDate.</p>
     *
     * @param inputString a {@link java.lang.String} object.
     * @param inputPattern a {@link java.lang.String} object.
     * @param outputPattern a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String convertDate(String inputString, String inputPattern, String outputPattern) {
        DateTimeFormatter in = DateTimeFormatter.ofPattern(inputPattern);
        DateTimeFormatter out = DateTimeFormatter.ofPattern(outputPattern);
        
        TemporalAccessor time = in.parse(inputString);
        String formatted = out.format(time);
        return formatted;
    }

}
