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
package io.goobi.viewer.model.job.mq;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.mq.MessageStatus;

class Center3DObjectHandlerTest {

    private static final double DELTA = 1e-6;

    @TempDir
    Path tempDir;

    /**
     * @verifies skip files that are not GLTF or GLB
     * @see Center3DObjectHandler#processCentering(String, String, Path, boolean)
     */
    @Test
    void processCentering_shouldSkipNonGltfFiles() throws IOException {
        Path nonGltf = tempDir.resolve("model.obj");
        Files.writeString(nonGltf, "v 0 0 0");

        Center3DObjectHandler handler = new Center3DObjectHandler(new ApiUrls("http://localhost"));
        MessageStatus status = handler.processCentering("PI_1", "model.obj", tempDir, false);

        assertEquals(MessageStatus.FINISH, status);
        assertFalse(Files.exists(tempDir.resolve("model.svx.json")), "No SVX should be created for non-GLTF files");
    }

    /**
     * @verifies skip when an SVX file already exists
     * @see Center3DObjectHandler#processCentering(String, String, Path, boolean)
     */
    @Test
    void processCentering_shouldSkipWhenSvxAlreadyExists() throws IOException {
        Path gltfFile = tempDir.resolve("model.gltf");
        Files.writeString(gltfFile, minimalGltfJson());

        Path svxFile = tempDir.resolve("model.svx.json");
        Files.writeString(svxFile, "{\"existing\":true}");

        Center3DObjectHandler handler = new Center3DObjectHandler(new ApiUrls("http://localhost"));
        MessageStatus status = handler.processCentering("PI_1", "model.gltf", tempDir, false);

        assertEquals(MessageStatus.FINISH, status);
        // Original content must be preserved
        assertTrue(new JSONObject(Files.readString(svxFile)).has("existing"));
    }

    /**
     * @verifies return ERROR when the model file does not exist
     * @see Center3DObjectHandler#processCentering(String, String, Path, boolean)
     */
    @Test
    void processCentering_shouldReturnErrorWhenModelFileMissing() {
        Center3DObjectHandler handler = new Center3DObjectHandler(new ApiUrls("http://localhost"));
        MessageStatus status = handler.processCentering("PI_1", "missing.gltf", tempDir, false);

        assertEquals(MessageStatus.ERROR, status);
    }

    /**
     * @verifies create a centered SVX file with the negated bounding-box centre as translation
     * @see Center3DObjectHandler#processCentering(String, String, Path, boolean)
     */
    @Test
    void processCentering_shouldCreateSvxWithNegatedBoundingBoxCentre() throws IOException {
        // POSITION accessor: min=[0,0,0] max=[2,4,6]  →  centre=[1,2,3]  →  translation=[-1,-2,-3]
        Path gltfFile = tempDir.resolve("model.gltf");
        Files.writeString(gltfFile, minimalGltfJson());

        Center3DObjectHandler handler = new Center3DObjectHandler(new ApiUrls("http://localhost"));
        MessageStatus status = handler.processCentering("PI_1", "model.gltf", tempDir, false);

        assertEquals(MessageStatus.FINISH, status);

        Path svxFile = tempDir.resolve("model.svx.json");
        assertTrue(Files.exists(svxFile), "SVX file should have been created");

        JSONObject svx = new JSONObject(Files.readString(svxFile));
        JSONArray nodes = svx.getJSONArray("models");
        assertEquals(1, nodes.length());

        JSONArray translation = nodes.getJSONObject(0).getJSONArray("translation");
        assertArrayEquals(
                new double[] { -1.0, -2.0, -3.0 },
                new double[] { translation.getDouble(0), translation.getDouble(1), translation.getDouble(2) },
                DELTA);
    }

    /**
     * @verifies overwrite existing SVX file when force is true
     * @see Center3DObjectHandler#processCentering(String, String, Path, boolean)
     */
    @Test
    void processCentering_shouldOverwriteExistingSvxWhenForceIsTrue() throws IOException {
        Path gltfFile = tempDir.resolve("model.gltf");
        Files.writeString(gltfFile, minimalGltfJson());

        Path svxFile = tempDir.resolve("model.svx.json");
        Files.writeString(svxFile, "{\"existing\":true}");

        Center3DObjectHandler handler = new Center3DObjectHandler(new ApiUrls("http://localhost"));
        MessageStatus status = handler.processCentering("PI_1", "model.gltf", tempDir, true);

        assertEquals(MessageStatus.FINISH, status);
        // Existing content must have been replaced with the generated scene
        JSONObject svx = new JSONObject(Files.readString(svxFile));
        assertFalse(svx.has("existing"), "Old SVX content should have been overwritten");
        assertTrue(svx.has("nodes"), "New SVX should contain nodes from the scene builder");
    }

    /**
     * @verifies return the correct handler name
     * @see Center3DObjectHandler#getMessageHandlerName()
     */
    @Test
    void getMessageHandlerName_shouldReturnCenterTaskTypeName() {
        assertEquals("CENTER_3D_OBJECT", new Center3DObjectHandler().getMessageHandlerName());
    }

    // --- helpers ---

    /** Minimal GLTF JSON with a single POSITION accessor: min=[0,0,0] max=[2,4,6]. */
    private static String minimalGltfJson() {
        return """
                {
                  "asset": {"version": "2.0"},
                  "meshes": [{"primitives": [{"attributes": {"POSITION": 0}}]}],
                  "accessors": [{"componentType": 5126, "count": 3, "type": "VEC3",
                                 "min": [0.0, 0.0, 0.0], "max": [2.0, 4.0, 6.0]}]
                }
                """;
    }
}
