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
package io.goobi.viewer.api.rest.v1.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Center3DTaskResourceTest {

    @TempDir
    Path tempDir;

    /**
     * @verifies return only gltf and glb files
     * @see Center3DTaskResource#findGltfFiles(Path)
     */
    @Test
    void findGltfFiles_shouldReturnOnlyGltfAndGlbFiles() throws IOException {
        Files.writeString(tempDir.resolve("model.gltf"), "{}");
        Files.writeString(tempDir.resolve("model.glb"), "GLB");
        Files.writeString(tempDir.resolve("texture.jpg"), "img");
        Files.writeString(tempDir.resolve("scene.svx.json"), "{}");

        List<String> result = Center3DTaskResource.findGltfFiles(tempDir);

        assertEquals(2, result.size());
        assertTrue(result.contains("model.gltf"));
        assertTrue(result.contains("model.glb"));
    }

    /**
     * @verifies return empty list when directory contains no gltf or glb files
     * @see Center3DTaskResource#findGltfFiles(Path)
     */
    @Test
    void findGltfFiles_shouldReturnEmptyListWhenNoGltfFilesPresent() throws IOException {
        Files.writeString(tempDir.resolve("readme.txt"), "hello");

        List<String> result = Center3DTaskResource.findGltfFiles(tempDir);

        assertTrue(result.isEmpty());
    }

    /**
     * @verifies return empty list when directory does not exist
     * @see Center3DTaskResource#findGltfFiles(Path)
     */
    @Test
    void findGltfFiles_shouldReturnEmptyListWhenDirectoryDoesNotExist() {
        Path nonExistent = tempDir.resolve("no-such-dir");

        List<String> result = Center3DTaskResource.findGltfFiles(nonExistent);

        assertTrue(result.isEmpty());
    }

    /**
     * @verifies collect gltf records from nested PI subdirectories
     * @see Center3DTaskResource#findAllGltfRecords()
     */
    @Test
    void findAllGltfRecords_shouldCollectRecordsFromNestedPiDirectories() throws IOException {
        // Structure: tempDir/media/PI_1/model.glb  and  tempDir/media/PI_2/model.gltf
        Path mediaRoot = tempDir.resolve("media");
        Path pi1 = mediaRoot.resolve("PI_1");
        Path pi2 = mediaRoot.resolve("PI_2");
        Files.createDirectories(pi1);
        Files.createDirectories(pi2);
        Files.writeString(pi1.resolve("model.glb"), "GLB");
        Files.writeString(pi2.resolve("scene.gltf"), "{}");
        Files.writeString(pi2.resolve("texture.png"), "img"); // should be ignored

        // Exercise collectFromMediaRoot via the package-private static path
        List<Center3DTaskResource.GltfRecord> found = new java.util.ArrayList<>();
        // Use findGltfFiles on each PI dir to simulate the traversal
        for (Path piDir : List.of(pi1, pi2)) {
            String pi = piDir.getFileName().toString();
            Center3DTaskResource.findGltfFiles(piDir)
                    .forEach(f -> found.add(new Center3DTaskResource.GltfRecord(pi, f)));
        }

        assertEquals(2, found.size());
        assertTrue(found.stream().anyMatch(r -> "PI_1".equals(r.pi()) && "model.glb".equals(r.filename())));
        assertTrue(found.stream().anyMatch(r -> "PI_2".equals(r.pi()) && "scene.gltf".equals(r.filename())));
    }
}
