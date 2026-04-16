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
package io.goobi.viewer.model.media.voyager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class GltfBoundingBoxReaderTest {

    private static final double DELTA = 1e-6;

    /**
     * @verifies compute centre from a single mesh with one POSITION accessor
     * @see GltfBoundingBoxReader#computeCenterFromJson(JSONObject)
     */
    @Test
    void computeCenterFromJson_shouldComputeCentreFromSinglePositionAccessor() {
        JSONObject gltf = buildGltfJson(
                """
                        [{"name":"Cube","primitives":[{"attributes":{"POSITION":0}}]}]
                        """,
                """
                        [{"componentType":5126,"count":3,"type":"VEC3","min":[0.0,0.0,0.0],"max":[2.0,4.0,6.0]}]
                        """);

        double[] center = GltfBoundingBoxReader.computeCenterFromJson(gltf);

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, center, DELTA);
    }

    /**
     * @verifies take the global min/max across multiple meshes
     * @see GltfBoundingBoxReader#computeCenterFromJson(JSONObject)
     */
    @Test
    void computeCenterFromJson_shouldTakeGlobalBoundsAcrossMultipleMeshes() {
        JSONObject gltf = buildGltfJson(
                """
                        [
                          {"primitives":[{"attributes":{"POSITION":0}}]},
                          {"primitives":[{"attributes":{"POSITION":1}}]}
                        ]
                        """,
                """
                        [
                          {"componentType":5126,"count":3,"type":"VEC3","min":[-10.0,0.0,0.0],"max":[0.0,0.0,0.0]},
                          {"componentType":5126,"count":3,"type":"VEC3","min":[0.0,0.0,0.0],"max":[0.0,20.0,0.0]}
                        ]
                        """);

        double[] center = GltfBoundingBoxReader.computeCenterFromJson(gltf);

        assertArrayEquals(new double[] { -5.0, 10.0, 0.0 }, center, DELTA);
    }

    /**
     * @verifies return zero vector when no POSITION accessor min/max is present
     * @see GltfBoundingBoxReader#computeCenterFromJson(JSONObject)
     */
    @Test
    void computeCenterFromJson_shouldReturnZeroWhenNoPositionAccessorFound() {
        JSONObject gltf = buildGltfJson(
                """
                        [{"primitives":[{"attributes":{"NORMAL":0}}]}]
                        """,
                """
                        [{"componentType":5126,"count":3,"type":"VEC3","min":[-1.0,-1.0,-1.0],"max":[1.0,1.0,1.0]}]
                        """);

        double[] center = GltfBoundingBoxReader.computeCenterFromJson(gltf);

        assertArrayEquals(new double[] { 0.0, 0.0, 0.0 }, center, DELTA);
    }

    /**
     * @verifies return zero vector when JSON contains no meshes
     * @see GltfBoundingBoxReader#computeCenterFromJson(JSONObject)
     */
    @Test
    void computeCenterFromJson_shouldReturnZeroWhenNoMeshes() {
        JSONObject gltf = new JSONObject("{\"asset\":{\"version\":\"2.0\"}}");

        double[] center = GltfBoundingBoxReader.computeCenterFromJson(gltf);

        assertArrayEquals(new double[] { 0.0, 0.0, 0.0 }, center, DELTA);
    }

    /**
     * @verifies read GLTF file from disk
     * @see GltfBoundingBoxReader#computeCenter(Path)
     */
    @Test
    void computeCenter_shouldReadGltfFileFromDisk() throws IOException {
        Path gltfFile = Paths.get("src/test/resources/data/viewer/3d/centered.gltf");

        double[] center = GltfBoundingBoxReader.computeCenter(gltfFile);

        // centered.gltf POSITION accessor: min=[0,0,0] max=[2,4,6] → centre=[1,2,3]
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, center, DELTA);
    }

    /**
     * @verifies parse the JSON chunk from a minimal GLB binary
     * @see GltfBoundingBoxReader#readGltfJson(Path)
     */
    @Test
    void readGltfJson_shouldParseJsonChunkFromGlbBinary() throws IOException {
        String gltfContent = buildGltfJson(
                """
                        [{"primitives":[{"attributes":{"POSITION":0}}]}]
                        """,
                """
                        [{"componentType":5126,"count":3,"type":"VEC3","min":[-1.0,-1.0,-1.0],"max":[1.0,1.0,1.0]}]
                        """).toString();

        Path glbFile = Files.createTempFile("test-", ".glb");
        try {
            Files.write(glbFile, buildMinimalGlb(gltfContent));

            JSONObject parsed = GltfBoundingBoxReader.readGltfJson(glbFile);
            double[] center = GltfBoundingBoxReader.computeCenterFromJson(parsed);

            assertArrayEquals(new double[] { 0.0, 0.0, 0.0 }, center, DELTA);
        } finally {
            Files.deleteIfExists(glbFile);
        }
    }

    // --- helpers ---

    private static JSONObject buildGltfJson(String meshesJson, String accessorsJson) {
        return new JSONObject()
                .put("asset", new JSONObject().put("version", "2.0"))
                .put("meshes", new org.json.JSONArray(meshesJson))
                .put("accessors", new org.json.JSONArray(accessorsJson));
    }

    /**
     * Builds a minimal valid GLB binary containing only the given JSON chunk.
     * The binary chunk is omitted – the format allows this for JSON-only assets.
     */
    private static byte[] buildMinimalGlb(String json) throws IOException {
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        // JSON chunk length must be a multiple of 4 – pad with spaces
        int paddedLength = (jsonBytes.length + 3) & ~3;
        byte[] paddedJson = new byte[paddedLength];
        System.arraycopy(jsonBytes, 0, paddedJson, 0, jsonBytes.length);
        for (int i = jsonBytes.length; i < paddedLength; i++) {
            paddedJson[i] = 0x20; // space
        }

        int totalLength = 12 + 8 + paddedLength; // file header + chunk header + chunk data
        ByteArrayOutputStream out = new ByteArrayOutputStream(totalLength);

        // file header
        out.write(le32(0x46546C67)); // magic "glTF"
        out.write(le32(2));          // version
        out.write(le32(totalLength));

        // JSON chunk header
        out.write(le32(paddedLength));
        out.write(le32(0x4E4F534A)); // type "JSON"

        out.write(paddedJson);
        return out.toByteArray();
    }

    private static byte[] le32(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }
}
