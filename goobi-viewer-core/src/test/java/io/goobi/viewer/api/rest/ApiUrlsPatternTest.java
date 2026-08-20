/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.api.rest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

// Imported under its short name; only v2.ApiUrls collides and stays fully-qualified inline below.
import io.goobi.viewer.api.rest.v1.ApiUrls;

/**
 * Verifies that the JAX-RS path-template regex constraints declared in the {@link io.goobi.viewer.api.rest.v1.ApiUrls}
 * and {@link io.goobi.viewer.api.rest.v2.ApiUrls} classes are compatible with the ECMA-262 regex dialect used by the
 * generated OpenAPI/JSON-Schema. Inline flag groups such as {@code (?i)} are valid in Java but rejected by the OpenAPI
 * validator ("Invalid special open parenthesis"), so the case-insensitivity must be expressed with character classes.
 */
class ApiUrlsPatternTest {

    /** Matches a JAX-RS path template variable with a regex constraint: {@code {name: regex}}. */
    private static final Pattern TEMPLATE_CONSTRAINT = Pattern.compile("\\{[^:{}]+:\\s*([^{}]+)\\}");

    private static List<String> collectConstraintRegexes(Class<?> apiUrlsClass) throws IllegalAccessException {
        List<String> regexes = new ArrayList<>();
        for (Field field : apiUrlsClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                String value = (String) field.get(null);
                if (value == null) {
                    continue;
                }
                Matcher m = TEMPLATE_CONSTRAINT.matcher(value);
                while (m.find()) {
                    regexes.add(m.group(1).trim());
                }
            }
        }
        return regexes;
    }

    private static String constraintOf(String pathTemplate) {
        Matcher m = TEMPLATE_CONSTRAINT.matcher(pathTemplate);
        assertTrue(m.find(), "No regex constraint found in: " + pathTemplate);
        String last = m.group(1).trim();
        while (m.find()) {
            last = m.group(1).trim();
        }
        return last;
    }

    /**
     * @see ApiUrls
     * @see io.goobi.viewer.api.rest.v2.ApiUrls
     * @verifies not contain inline regex flags in any path template
     */
    @Test
    void pathTemplates_shouldNotUseInlineRegexFlags() throws Exception {
        // v2.ApiUrls stays fully-qualified inline: its short name collides with the v1.ApiUrls import above.
        for (Class<?> clazz : new Class<?>[] { ApiUrls.class, io.goobi.viewer.api.rest.v2.ApiUrls.class }) {
            for (String regex : collectConstraintRegexes(clazz)) {
                // Inline flags like (?i) are valid in Java but break ECMA-262/OpenAPI schema validation
                assertFalse(regex.contains("(?i)"),
                        "Path template constraint uses inline flag (?i), which is invalid in ECMA-262/OpenAPI: " + regex + " in " + clazz.getName());
            }
        }
    }

    /**
     * Raw scan that complements {@link #pathTemplates_shouldNotUseInlineRegexFlags()}: the
     * TEMPLATE_CONSTRAINT regex above only extracts a constraint when the field value has no
     * nested braces, so a constraint like {@code \w{1,4}} (containing its own {@code {}}) is
     * silently skipped by the parser above and would slip through undetected. This test instead
     * checks every declared field's raw string value directly, independent of parsing.
     *
     * @see ApiUrls
     * @see io.goobi.viewer.api.rest.v2.ApiUrls
     * @verifies not contain inline regex flags in any declared field value
     */
    @Test
    void fieldValues_shouldNotContainInlineRegexFlags() throws IllegalAccessException {
        for (Class<?> clazz : new Class<?>[] { ApiUrls.class, io.goobi.viewer.api.rest.v2.ApiUrls.class }) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()) && field.getType() == String.class) {
                    String value = (String) field.get(null);
                    if (value != null) {
                        assertFalse(value.contains("(?i)"),
                                "Field " + clazz.getName() + "." + field.getName() + " contains inline flag (?i): " + value);
                    }
                }
            }
        }
    }

    /**
     * @see ApiUrls#CMS_MEDIA_FILES_FILE_IMAGE
     * @verifies match image file extensions case insensitively
     */
    @Test
    void imagePattern_shouldMatchImageExtensionsCaseInsensitively() {
        String regex = constraintOf(ApiUrls.CMS_MEDIA_FILES_FILE_IMAGE);
        Pattern p = Pattern.compile(regex);
        for (String ok : new String[] { "a.jpg", "a.JPG", "a.Jpeg", "a.tif", "a.TIFF", "a.png", "a.PNG", "a.gif", "a.jp2", "a.JP2" }) {
            assertTrue(p.matcher(ok).matches(), "expected match: " + ok);
        }
        for (String bad : new String[] { "a.txt", "a.bmp", "a.pdf" }) {
            assertFalse(p.matcher(bad).matches(), "expected no match: " + bad);
        }
    }

    /**
     * @see ApiUrls#CMS_MEDIA_FILES_FILE_PDF
     * @see ApiUrls#CMS_MEDIA_FILES_FILE_HTML
     * @see ApiUrls#CMS_MEDIA_FILES_FILE_SVG
     * @see ApiUrls#CMS_MEDIA_FILES_FILE_ICO
     * @see ApiUrls#CMS_MEDIA_FILES_FILE_AUDIO
     * @see ApiUrls#CMS_MEDIA_FILES_FILE_VIDEO
     * @verifies match non-image media extensions case insensitively
     */
    @Test
    void mediaPatterns_shouldMatchExtensionsCaseInsensitively() {
        assertMatch(ApiUrls.CMS_MEDIA_FILES_FILE_PDF, new String[] { "a.pdf", "a.PDF" }, new String[] { "a.txt" });
        assertMatch(ApiUrls.CMS_MEDIA_FILES_FILE_HTML, new String[] { "a.html", "a.HTML" }, new String[] { "a.htm" });
        assertMatch(ApiUrls.CMS_MEDIA_FILES_FILE_SVG, new String[] { "a.svg", "a.SVG" }, new String[] { "a.png" });
        assertMatch(ApiUrls.CMS_MEDIA_FILES_FILE_ICO, new String[] { "a.ico", "a.ICO" }, new String[] { "a.png" });
        assertMatch(ApiUrls.CMS_MEDIA_FILES_FILE_AUDIO, new String[] { "a.mp3", "a.MP3", "a.WAV" }, new String[] { "a.txt" });
        assertMatch(ApiUrls.CMS_MEDIA_FILES_FILE_VIDEO, new String[] { "a.mp4", "a.MP4", "a.MOV" }, new String[] { "a.txt" });
    }

    private static void assertMatch(String pathTemplate, String[] shouldMatch, String[] shouldNotMatch) {
        Pattern p = Pattern.compile(constraintOf(pathTemplate));
        for (String ok : shouldMatch) {
            assertTrue(p.matcher(ok).matches(), "expected match: " + ok + " for " + pathTemplate);
        }
        for (String bad : shouldNotMatch) {
            assertFalse(p.matcher(bad).matches(), "expected no match: " + bad + " for " + pathTemplate);
        }
    }
}
