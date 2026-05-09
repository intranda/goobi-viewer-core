package io.goobi.viewer.model.cms.pages.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class JarBackedComponentDiscoverySpikeTest {

    /**
     * @verifies enumerate xml files inside a jar-backed subfolder
     */
    @Test
    void filesList_shouldEnumerateXmlFilesInsideJarBackedSubfolder(@TempDir Path tmp) throws Exception {
        // Build a tiny JAR containing two XMLs in a subfolder.
        Path jar = tmp.resolve("module.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new ZipEntry("META-INF/resources/resources/cms/templates/crowdsourcing/alpha.xml"));
            out.write("<component/>".getBytes());
            out.closeEntry();
            out.putNextEntry(new ZipEntry("META-INF/resources/resources/cms/templates/crowdsourcing/beta.xml"));
            out.write("<component/>".getBytes());
            out.closeEntry();
        }

        URI jarUri = URI.create("jar:" + jar.toUri());
        try (FileSystem fs = FileSystems.newFileSystem(jarUri, Map.of("create", "true"))) {
            Path folder = fs.getPath("/META-INF/resources/resources/cms/templates/crowdsourcing");
            assertTrue(Files.isDirectory(folder));

            long xmlCount;
            try (var stream = Files.list(folder)) {
                xmlCount = stream.filter(p -> p.getFileName().toString().endsWith(".xml")).count();
            }
            assertEquals(2, xmlCount);
        }
    }
}
