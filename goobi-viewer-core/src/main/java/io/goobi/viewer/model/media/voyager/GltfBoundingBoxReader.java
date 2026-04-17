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
     * Returns the axis-aligned bounding box of all meshes in the given GLTF or GLB file.
     *
     * @param gltfFile path to a {@code .gltf} or {@code .glb} file
     * @return {@code double[2][3]} where {@code [0]} is the global min and {@code [1]} is the global max;
     *         both are {@code [0,0,0]} if no POSITION accessor min/max data is found
     * @throws IOException if the file cannot be read
     */
    public static double[][] computeBounds(Path gltfFile) throws IOException {
        JSONObject json = readGltfJson(gltfFile);
        return computeBoundsFromJson(json);
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
        double[][] bounds = computeBounds(gltfFile);
        return centerOf(bounds);
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
     * Returns the world-space axis-aligned bounding box from a parsed GLTF root JSON object.
     *
     * <p>When the asset contains a scene graph ({@code scenes} + {@code nodes}), the method
     * traverses it and applies each node's transform (matrix or TRS) to the POSITION accessor
     * min/max values before accumulating the global AABB.  This correctly handles models whose
     * root node carries a scale, rotation, or translation.
     *
     * <p>Falls back to a flat accessor scan when the asset has no scene graph (e.g. test data).
     *
     * @param gltfJson the root {@link JSONObject} of a GLTF asset
     * @return {@code double[2][3]} where {@code [0]} is the global min and {@code [1]} is the global max;
     *         both are {@code [0,0,0]} if no POSITION accessor with {@code min}/{@code max} is found
     */
    static double[][] computeBoundsFromJson(JSONObject gltfJson) {
        JSONArray scenes = gltfJson.optJSONArray("scenes");
        JSONArray nodes = gltfJson.optJSONArray("nodes");
        JSONArray meshes = gltfJson.optJSONArray("meshes");
        JSONArray accessors = gltfJson.optJSONArray("accessors");

        if (scenes != null && nodes != null && meshes != null && accessors != null) {
            double[] gMin = { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE };
            double[] gMax = { -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };
            boolean found = false;

            JSONObject firstScene = scenes.optJSONObject(0);
            if (firstScene != null) {
                JSONArray rootNodes = firstScene.optJSONArray("nodes");
                if (rootNodes != null) {
                    double[] identity = identityMatrix();
                    for (int i = 0; i < rootNodes.length(); i++) {
                        found |= traverseNode(rootNodes.getInt(i), identity, nodes, meshes, accessors, gMin, gMax);
                    }
                }
            }

            if (found) {
                return new double[][] { gMin, gMax };
            }
        }

        return computeRawBounds(meshes, accessors);
    }

    /**
     * Computes the bounding-box centre from a parsed GLTF root JSON object.
     *
     * @param gltfJson the root {@link JSONObject} of a GLTF asset
     * @return {@code double[3]} centre coordinates, or {@code [0, 0, 0]} if no
     *         POSITION accessor with {@code min}/{@code max} is found
     */
    static double[] computeCenterFromJson(JSONObject gltfJson) {
        return centerOf(computeBoundsFromJson(gltfJson));
    }

    private static boolean traverseNode(int nodeIdx, double[] parentTransform,
            JSONArray nodes, JSONArray meshes, JSONArray accessors,
            double[] gMin, double[] gMax) {
        JSONObject node = nodes.optJSONObject(nodeIdx);
        if (node == null) {
            return false;
        }
        double[] world = multiplyMatrices(parentTransform, getNodeTransform(node));
        boolean found = false;

        if (node.has("mesh")) {
            JSONObject mesh = meshes.optJSONObject(node.getInt("mesh"));
            if (mesh != null) {
                JSONArray primitives = mesh.optJSONArray("primitives");
                if (primitives != null) {
                    for (int p = 0; p < primitives.length(); p++) {
                        JSONObject prim = primitives.optJSONObject(p);
                        if (prim == null) {
                            continue;
                        }
                        JSONObject attrs = prim.optJSONObject("attributes");
                        if (attrs == null || !attrs.has("POSITION")) {
                            continue;
                        }
                        JSONObject acc = accessors.optJSONObject(attrs.getInt("POSITION"));
                        if (acc == null) {
                            continue;
                        }
                        Optional<double[]> minOpt = toDoubleArray(acc.optJSONArray("min"));
                        Optional<double[]> maxOpt = toDoubleArray(acc.optJSONArray("max"));
                        if (minOpt.isEmpty() || maxOpt.isEmpty()) {
                            continue;
                        }
                        double[] min = minOpt.get();
                        double[] max = maxOpt.get();
                        for (int bx = 0; bx < 2; bx++) {
                            for (int by = 0; by < 2; by++) {
                                for (int bz = 0; bz < 2; bz++) {
                                    double[] pt = transformPoint(world,
                                            bx == 0 ? min[0] : max[0],
                                            by == 0 ? min[1] : max[1],
                                            bz == 0 ? min[2] : max[2]);
                                    for (int i = 0; i < 3; i++) {
                                        if (pt[i] < gMin[i]) {
                                            gMin[i] = pt[i];
                                        }
                                        if (pt[i] > gMax[i]) {
                                            gMax[i] = pt[i];
                                        }
                                    }
                                }
                            }
                        }
                        found = true;
                    }
                }
            }
        }

        JSONArray children = node.optJSONArray("children");
        if (children != null) {
            for (int c = 0; c < children.length(); c++) {
                found |= traverseNode(children.getInt(c), world, nodes, meshes, accessors, gMin, gMax);
            }
        }
        return found;
    }

    private static double[] getNodeTransform(JSONObject node) {
        JSONArray matrix = node.optJSONArray("matrix");
        if (matrix != null && matrix.length() == 16) {
            double[] m = new double[16];
            for (int i = 0; i < 16; i++) {
                m[i] = matrix.getDouble(i);
            }
            return m;
        }
        double tx = 0, ty = 0, tz = 0;
        double qx = 0, qy = 0, qz = 0, qw = 1;
        double sx = 1, sy = 1, sz = 1;
        JSONArray t = node.optJSONArray("translation");
        if (t != null && t.length() >= 3) {
            tx = t.getDouble(0);
            ty = t.getDouble(1);
            tz = t.getDouble(2);
        }
        JSONArray r = node.optJSONArray("rotation");
        if (r != null && r.length() >= 4) {
            qx = r.getDouble(0);
            qy = r.getDouble(1);
            qz = r.getDouble(2);
            qw = r.getDouble(3);
        }
        JSONArray s = node.optJSONArray("scale");
        if (s != null && s.length() >= 3) {
            sx = s.getDouble(0);
            sy = s.getDouble(1);
            sz = s.getDouble(2);
        }
        return trsToMatrix(tx, ty, tz, qx, qy, qz, qw, sx, sy, sz);
    }

    private static double[] trsToMatrix(double tx, double ty, double tz,
            double qx, double qy, double qz, double qw,
            double sx, double sy, double sz) {
        double x2 = qx + qx, y2 = qy + qy, z2 = qz + qz;
        double xx = qx * x2, xy = qx * y2, xz = qx * z2;
        double yy = qy * y2, yz = qy * z2, zz = qz * z2;
        double wx = qw * x2, wy = qw * y2, wz = qw * z2;
        // Column-major 4×4 matrix (T * R * S)
        return new double[] {
                (1.0 - (yy + zz)) * sx, (xy + wz) * sx, (xz - wy) * sx, 0.0, // col 0
                (xy - wz) * sy, (1.0 - (xx + zz)) * sy, (yz + wx) * sy, 0.0, // col 1
                (xz + wy) * sz, (yz - wx) * sz, (1.0 - (xx + yy)) * sz, 0.0, // col 2
                tx, ty, tz, 1.0 // col 3
        };
    }

    private static double[] multiplyMatrices(double[] a, double[] b) {
        double[] result = new double[16];
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                double sum = 0;
                for (int k = 0; k < 4; k++) {
                    sum += a[k * 4 + row] * b[col * 4 + k];
                }
                result[col * 4 + row] = sum;
            }
        }
        return result;
    }

    private static double[] transformPoint(double[] m, double x, double y, double z) {
        return new double[] {
                m[0] * x + m[4] * y + m[8] * z + m[12],
                m[1] * x + m[5] * y + m[9] * z + m[13],
                m[2] * x + m[6] * y + m[10] * z + m[14]
        };
    }

    private static double[] identityMatrix() {
        return new double[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 };
    }

    private static double[][] computeRawBounds(JSONArray meshes, JSONArray accessors) {
        if (meshes == null || accessors == null) {
            return new double[][] { { 0, 0, 0 }, { 0, 0, 0 } };
        }
        double[] gMin = { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE };
        double[] gMax = { -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };
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
                JSONObject prim = primitives.optJSONObject(p);
                if (prim == null) {
                    continue;
                }
                JSONObject attrs = prim.optJSONObject("attributes");
                if (attrs == null || !attrs.has("POSITION")) {
                    continue;
                }
                JSONObject acc = accessors.optJSONObject(attrs.getInt("POSITION"));
                if (acc == null) {
                    continue;
                }
                Optional<double[]> minOpt = toDoubleArray(acc.optJSONArray("min"));
                Optional<double[]> maxOpt = toDoubleArray(acc.optJSONArray("max"));
                if (minOpt.isEmpty() || maxOpt.isEmpty()) {
                    continue;
                }
                double[] min = minOpt.get();
                double[] max = maxOpt.get();
                for (int i = 0; i < 3; i++) {
                    if (min[i] < gMin[i]) {
                        gMin[i] = min[i];
                    }
                    if (max[i] > gMax[i]) {
                        gMax[i] = max[i];
                    }
                }
                found = true;
            }
        }
        if (!found) {
            return new double[][] { { 0, 0, 0 }, { 0, 0, 0 } };
        }
        return new double[][] { gMin, gMax };
    }

    private static double[] centerOf(double[][] bounds) {
        return new double[] {
                (bounds[0][0] + bounds[1][0]) / 2.0,
                (bounds[0][1] + bounds[1][1]) / 2.0,
                (bounds[0][2] + bounds[1][2]) / 2.0
        };
    }

    private static Optional<double[]> toDoubleArray(JSONArray arr) {
        if (arr == null || arr.length() < 3) {
            return Optional.empty();
        }
        return Optional.of(new double[] { arr.getDouble(0), arr.getDouble(1), arr.getDouble(2) });
    }
}
