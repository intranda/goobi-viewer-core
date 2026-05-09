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
 */
package io.goobi.viewer.model.cms.pages.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that {@link CMSPageContentManager} can ingest CMS components both from regular filesystem
 * directories and from JAR-mounted directories created via {@link FileSystems#newFileSystem(URI, Map)}.
 *
 * <p>Extends {@link io.goobi.viewer.AbstractDatabaseEnabledTest} so the {@code DataManager} singleton (and its
 * configuration backing the {@link io.goobi.viewer.model.cms.CMSTemplateManager} init path) is initialized from
 * {@code localConfig/config_viewer.xml}. Existing CMS tests follow the same pattern; copying it avoids brittle
 * direct-init issues observed in earlier review rounds.</p>
 */
class CMSPageContentManagerModuleTest extends io.goobi.viewer.AbstractDatabaseEnabledTest {

    /**
     * @see CMSPageContentManager#CMSPageContentManager(Path...)
     * @verifies merge components from multiple real filesystem folders
     */
    @Test
    void constructor_shouldMergeComponentsFromMultipleRealFilesystemFolders(@TempDir Path coreDir, @TempDir Path moduleDir) throws IOException {
        Files.writeString(coreDir.resolve("alpha.xml"), buildMinimalComponent("alpha"));
        Files.writeString(moduleDir.resolve("beta.xml"), buildMinimalComponent("beta"));

        CMSPageContentManager mgr = new CMSPageContentManager(coreDir, moduleDir);

        assertEquals(2, mgr.getComponents().size());
        assertTrue(mgr.getComponent("alpha").isPresent());
        assertTrue(mgr.getComponent("beta").isPresent());
    }

    /**
     * @see CMSPageContentManager#CMSPageContentManager(Path...)
     * @verifies merge components from a real folder and a jar backed folder
     */
    @Test
    void constructor_shouldMergeComponentsFromARealFolderAndAJarBackedFolder(@TempDir Path coreDir, @TempDir Path tmp) throws IOException {
        Files.writeString(coreDir.resolve("alpha.xml"), buildMinimalComponent("alpha"));

        // Build a minimal module JAR that ships beta.xml under the conventional location.
        Path jar = tmp.resolve("module.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new ZipEntry("META-INF/resources/resources/cms/templates/crowdsourcing/beta.xml"));
            out.write(buildMinimalComponent("beta").getBytes());
            out.closeEntry();
        }

        URI jarUri = URI.create("jar:" + jar.toUri());
        try (FileSystem fs = FileSystems.newFileSystem(jarUri, Map.of("create", "true"))) {
            Path moduleFolder = fs.getPath("/META-INF/resources/resources/cms/templates/crowdsourcing");

            CMSPageContentManager mgr = new CMSPageContentManager(coreDir, moduleFolder);

            assertEquals(2, mgr.getComponents().size());
            assertTrue(mgr.getComponent("alpha").isPresent());
            assertTrue(mgr.getComponent("beta").isPresent());
        }
    }

    private static String buildMinimalComponent(String name) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                + "<component>\n"
                + "  <jsfComponent><library>cms/components/frontend/component</library><name>" + name + "</name></jsfComponent>\n"
                + "  <label>" + name + "_label</label>\n"
                + "  <description>" + name + "_desc</description>\n"
                + "  <type>layout</type>\n"
                + "  <content/>\n"
                + "</component>\n";
    }
}
