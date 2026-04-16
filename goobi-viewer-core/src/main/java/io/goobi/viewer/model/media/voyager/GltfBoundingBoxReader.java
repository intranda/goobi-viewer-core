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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Reads the axis-aligned bounding box center of a GLTF 2.0 or GLB 3D model
 * without a dedicated GLTF library.
 *
 * <p>The GLTF 2.0 specification requires that every vertex-position accessor
 * carries {@code min} and {@code max} arrays.  This class extracts those values
 * directly from the JSON metadata, making binary buffer parsing unnecessary.
 *
 * <p>All methods are static; this class is not meant to be instantiated.
 */
public class GltfBoundingBoxReader {

    private GltfBoundingBoxReader() {
    }

    /**
     * Computes the bounding-box centre of all meshes in the given GLTF or GLB file.
     *
     * @param gltfFile path to a {@code .gltf} or {@code .glb} file
     * @return {@code double[3]} with {@code [x, y, z]} of the centre;
     *         {@code [0, 0, 0]} if no POSITION accessor min/max data is found
     * @throws IOException if the file cannot be read
     */
    public static double[] computeCenter(Path gltfFile) throws IOException {
        JSONObject json = readGltfJson(gltfFile);
        return computeCenterFromJson(json);
    }

    /**
     * Reads the GLTF JSON from a file.  For GLB files the 12-byte binary header
     * is skipped and only the first (JSON) chunk is parsed.
     */
    static JSONObject readGltfJson(Path file) throws IOException {
        String ext = FilenameUtils.getExtension(file.getFileName().toString()).toLowerCase();
        if ("glb".equals(ext)) {
            return readGlbJson(file);
        }
        return new JSONObject(Files.readString(file, StandardCharsets.UTF_8));
    }

    /**
     * Extracts the JSON chunk from a GLB binary file.
     *
     * <p>GLB layout:
     * <ul>
     *   <li>12-byte file header: magic (4), version (4), total length (4)</li>
     *   <li>First chunk header: chunk length (4, LE), chunk type (4)</li>
     *   <li>First chunk data: {@code chunkLength} bytes of UTF-8 JSON</li>
     * </ul>
     */
    private static JSONObject readGlbJson(Path file) throws IOException {
        try (InputStream is = Files.newInputStream(file)) {
            byte[] fileHeader = is.readNBytes(12);
            if (fileHeader.length < 12) {
                throw new IOException("File too short to be a valid GLB: " + file);
            }
            byte[] chunkHeader = is.readNBytes(8);
            if (chunkHeader.length < 8) {
                throw new IOException("Missing chunk header in GLB: " + file);
            }
            int jsonLength = ByteBuffer.wrap(chunkHeader, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            byte[] jsonBytes = is.readNBytes(jsonLength);
            return new JSONObject(new String(jsonBytes, StandardCharsets.UTF_8));
        }
    }

    /**
     * Computes the bounding-box centre from a parsed GLTF root JSON object.
     *
     * <p>Iterates every mesh primitive, looks up its {@code POSITION} accessor,
     * and accumulates the global axis-aligned min and max.  The centre is the
     * arithmetic mean of those extremes.
     *
     * @param gltfJson the root {@link JSONObject} of a GLTF asset
     * @return {@code double[3]} centre coordinates, or {@code [0, 0, 0]} if no
     *         POSITION accessor with {@code min}/{@code max} is found
     */
    static double[] computeCenterFromJson(JSONObject gltfJson) {
        JSONArray accessors = gltfJson.optJSONArray("accessors");
        JSONArray meshes = gltfJson.optJSONArray("meshes");

        if (accessors == null || meshes == null) {
            return new double[] { 0, 0, 0 };
        }

        double[] globalMin = { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE };
        double[] globalMax = { -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };
        boolean found = false;

        for (int m = 0; m < meshes.length(); m++) {
            JSONObject mesh = meshes.optJSONObject(m);
            if (mesh == null) {
                continue;
            }
            JSONArray primitives = mesh.optJSONArray("primitives");
            if (primitives == null) {
                continue;
            }
            for (int p = 0; p < primitives.length(); p++) {
                JSONObject primitive = primitives.optJSONObject(p);
                if (primitive == null) {
                    continue;
                }
                JSONObject attributes = primitive.optJSONObject("attributes");
                if (attributes == null || !attributes.has("POSITION")) {
                    continue;
                }
                int accessorIndex = attributes.getInt("POSITION");
                JSONObject accessor = accessors.optJSONObject(accessorIndex);
                if (accessor == null) {
                    continue;
                }
                Optional<double[]> minOpt = toDoubleArray(accessor.optJSONArray("min"));
                Optional<double[]> maxOpt = toDoubleArray(accessor.optJSONArray("max"));
                if (minOpt.isEmpty() || maxOpt.isEmpty()) {
                    continue;
                }
                double[] min = minOpt.get();
                double[] max = maxOpt.get();
                for (int i = 0; i < 3; i++) {
                    if (min[i] < globalMin[i]) {
                        globalMin[i] = min[i];
                    }
                    if (max[i] > globalMax[i]) {
                        globalMax[i] = max[i];
                    }
                }
                found = true;
            }
        }

        if (!found) {
            return new double[] { 0, 0, 0 };
        }
        return new double[] {
                (globalMin[0] + globalMax[0]) / 2.0,
                (globalMin[1] + globalMax[1]) / 2.0,
                (globalMin[2] + globalMax[2]) / 2.0
        };
    }

    private static Optional<double[]> toDoubleArray(JSONArray arr) {
        if (arr == null || arr.length() < 3) {
            return Optional.empty();
        }
        return Optional.of(new double[] { arr.getDouble(0), arr.getDouble(1), arr.getDouble(2) });
    }
}
