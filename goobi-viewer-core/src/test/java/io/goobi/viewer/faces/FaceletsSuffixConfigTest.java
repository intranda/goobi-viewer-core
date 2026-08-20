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
package io.goobi.viewer.faces;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;

/**
 * Verifies that the Facelets suffixes declared in <code>META-INF/web-fragment.xml</code> cover every file extension the bundled views use in a
 * <code>template</code> attribute or a <code>&lt;ui:include&gt;</code>.
 *
 * <p>
 * Background: Mojarra 4.1.13 introduced <code>DefaultFaceletFactory.requireFaceletResource()</code>, which rejects a template or include whose
 * resolved URL does not end with one of the configured Facelets suffixes (default: <code>.xhtml</code> only). The viewer needs two more: theme
 * templates use the <code>.html</code> extension, and icons are inlined as markup via <code>&lt;ui:include src="....svg"&gt;</code>. Dropping either
 * from <code>jakarta.faces.FACELETS_SUFFIX</code> breaks the affected views with "... is not a Facelet resource".
 * </p>
 */
class FaceletsSuffixConfigTest extends AbstractTest {

    private static final Path WEB_FRAGMENT = Paths.get("src/main/resources/META-INF/web-fragment.xml");

    private static final Path VIEWS_ROOT = Paths.get("src/main/resources/META-INF/resources");

    /** Matches the param-value of the jakarta.faces.FACELETS_SUFFIX context-param, ignoring whitespace between the elements. */
    private static final Pattern FACELETS_SUFFIX_PATTERN = Pattern.compile(
            "<param-name>\\s*jakarta\\.faces\\.FACELETS_SUFFIX\\s*</param-name>\\s*<param-value>\\s*([^<]+?)\\s*</param-value>");

    /**
     * Matches the value of a template attribute, e.g. template="/resources/themes/#{navigationHelper.theme}/template.html". The look-behind keeps
     * unrelated attributes ending in "template" (e.g. data-quickfilter-template) out.
     */
    private static final Pattern TEMPLATE_ATTRIBUTE_PATTERN = Pattern.compile("(?<![\\w-])template\\s*=\\s*\"([^\"]+)\"");

    /** Matches the src attribute of a ui:include tag; the character class spans line breaks, so attributes may be wrapped. */
    private static final Pattern INCLUDE_SRC_PATTERN = Pattern.compile("<ui:include\\b[^>]*?\\bsrc\\s*=\\s*\"([^\"]+)\"");

    /** A literal file extension; values whose tail is an EL expression have no statically known extension. */
    private static final Pattern LITERAL_EXTENSION_PATTERN = Pattern.compile("\\.[A-Za-z0-9]+");

    /**
     * Every extension used in a template attribute or ui:include of a bundled view must be declared as a Facelets suffix.
     */
    @Test
    void faceletsSuffixes_shouldCoverAllFaceletExtensionsUsedByBundledViews() throws IOException {
        Set<String> configuredSuffixes = readConfiguredFaceletsSuffixes();
        assertTrue(configuredSuffixes.contains(".xhtml"), ".xhtml must remain configured, otherwise no view can be resolved at all");

        Set<String> usedExtensions = collectFaceletExtensions();
        assertFalse(usedExtensions.isEmpty(), "No template attributes or ui:include tags found - test setup is broken");

        for (String extension : usedExtensions) {
            assertTrue(configuredSuffixes.contains(extension),
                    "Extension " + extension + " is referenced by bundled views but not declared in jakarta.faces.FACELETS_SUFFIX "
                            + configuredSuffixes + "; Mojarra would reject those files as \"not a Facelet resource\"");
        }
    }

    private static Set<String> readConfiguredFaceletsSuffixes() throws IOException {
        String webFragment = Files.readString(WEB_FRAGMENT, StandardCharsets.UTF_8);
        Matcher matcher = FACELETS_SUFFIX_PATTERN.matcher(webFragment);
        assertTrue(matcher.find(), "Context param jakarta.faces.FACELETS_SUFFIX is missing from " + WEB_FRAGMENT);

        return Set.of(matcher.group(1).trim().split("\\s+"));
    }

    private static Set<String> collectFaceletExtensions() throws IOException {
        Set<String> extensions = new HashSet<>();
        try (Stream<Path> views = Files.walk(VIEWS_ROOT)) {
            List<Path> viewFiles = views.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().endsWith(".xhtml")).toList();
            for (Path viewFile : viewFiles) {
                String view = Files.readString(viewFile, StandardCharsets.UTF_8);
                addLiteralExtensions(TEMPLATE_ATTRIBUTE_PATTERN.matcher(view), extensions);
                addLiteralExtensions(INCLUDE_SRC_PATTERN.matcher(view), extensions);
            }
        }

        return extensions;
    }

    /**
     * Adds the extension of every match's first group to the given set, skipping values that end in an EL expression.
     */
    private static void addLiteralExtensions(Matcher matcher, Set<String> extensions) {
        while (matcher.find()) {
            String path = matcher.group(1);
            int dotIndex = path.lastIndexOf('.');
            if (dotIndex > -1) {
                String extension = path.substring(dotIndex);
                if (LITERAL_EXTENSION_PATTERN.matcher(extension).matches()) {
                    extensions.add(extension);
                }
            }
        }
    }
}
