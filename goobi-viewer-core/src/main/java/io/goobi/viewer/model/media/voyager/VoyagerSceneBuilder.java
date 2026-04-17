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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Builder that constructs a Smithsonian Voyager scene document (JSON) for one or more 3D model assets, including asset metadata, scene nodes, and
 * derivative descriptions.
 */
public class VoyagerSceneBuilder {

    private static final Logger logger = LogManager.getLogger(VoyagerSceneBuilder.class);

    private final Map<URI, Path> models = new LinkedHashMap<>();
    private final String name;
    private String unit = "mm";
    private double[] translation = null;
    private double[] boundsMin = null;
    private double[] boundsMax = null;

    public VoyagerSceneBuilder(String name) {
        this.name = name;
    }

    public VoyagerSceneBuilder setUnit(String unit) {
        this.unit = unit;
        return this;
    }

    /**
     * Sets the model-level translation stored in the SVX {@code models[i].translation} field, expressed as {@code [x, y, z]} in the scene's native
     * units.
     *
     * <p>
     * Voyager uses this field (together with {@code model.boundingBox}) to compute the orbit pivot. Pass the negated world-space bounding-box centre
     * so that the pivot lands at the scene origin and Voyager orbits around the object's true geometric centre.
     *
     * @param x translation along the X axis
     * @param y translation along the Y axis
     * @param z translation along the Z axis
     * @return this builder
     */
    public VoyagerSceneBuilder setTranslation(double x, double y, double z) {
        this.translation = new double[] { x, y, z };
        return this;
    }

    /**
     * Sets the axis-aligned bounding box of the model in its local coordinate space, as read from the GLTF accessor min/max values. Used to embed a
     * {@code boundingBox} in the SVX model entry so Voyager can correctly position the orbit pivot, and to compute a reasonable initial camera
     * distance.
     *
     * @param min lower corner {@code [x, y, z]}
     * @param max upper corner {@code [x, y, z]}
     * @return this builder
     */
    public VoyagerSceneBuilder setBounds(double[] min, double[] max) {
        this.boundsMin = min.clone();
        this.boundsMax = max.clone();
        return this;
    }

    public VoyagerSceneBuilder addModel(URI url, Path file) {
        this.models.put(url, file);
        return this;
    }

    public String build() {

        JSONArray models = new JSONArray(this.models.entrySet().stream().map(e -> createModel(e.getKey(), e.getValue())).toList());
        JSONArray nodes = new JSONArray(IntStream.range(0, models.length()).mapToObj(this::createNode).toList());
        JSONArray scenes = new JSONArray(List.of(createScene(IntStream.range(0, nodes.length()).toArray())));
        JSONArray setups = new JSONArray(List.of(createSetup()));

        JSONObject json = new JSONObject();
        json.put("asset", createAsset());
        json.put("scene", 0);
        json.put("scenes", scenes);
        json.put("nodes", nodes);
        json.put("models", models);
        json.put("setups", setups);

        return json.toString();
    }

    private JSONObject createAsset() {
        JSONObject asset = new JSONObject();
        asset.put("type", "application/si-dpo-3d.document+json");
        asset.put("version", "1.0");
        asset.put("generator", "Goobi Viewer");
        asset.put("copyright", "(c) Smithsonian Institution; intranda GmbH. All rights reserved.");
        return asset;
    }

    private JSONObject createScene(int[] nodes) {
        JSONObject scene = new JSONObject();
        scene.put("name", this.name);
        scene.put("units", this.unit);
        scene.put("nodes", new JSONArray(nodes));
        scene.put("setup", 0);
        return scene;
    }

    private JSONObject createSetup() {
        double distance = computeCameraDistance();

        JSONObject orbitParams = new JSONObject();
        orbitParams.put("orbit", new JSONArray(new double[] { 0.0, -20.0, 0.0 }));
        orbitParams.put("offset", new JSONArray(new double[] { 0.0, 0.0, distance }));
        orbitParams.put("minOrbit", new JSONArray(new Object[] { -90.0, JSONObject.NULL, JSONObject.NULL }));
        orbitParams.put("maxOrbit", new JSONArray(new Object[] { 90.0, JSONObject.NULL, JSONObject.NULL }));
        orbitParams.put("minOffset", new JSONArray(new Object[] { JSONObject.NULL, JSONObject.NULL, 0.1 }));
        orbitParams.put("maxOffset", new JSONArray(new Object[] { JSONObject.NULL, JSONObject.NULL, 10000.0 }));

        JSONObject navigation = new JSONObject();
        navigation.put("type", "Orbit");
        navigation.put("enabled", true);
        navigation.put("autoZoom", true);
        navigation.put("lightsFollowCamera", true);
        navigation.put("autoRotation", false);
        navigation.put("orbit", orbitParams);

        JSONObject setup = new JSONObject();
        setup.put("units", this.unit);
        setup.put("navigation", navigation);
        return setup;
    }

    private double computeCameraDistance() {
        if (boundsMin == null || boundsMax == null) {
            return 1000.0;
        }
        double dx = boundsMax[0] - boundsMin[0];
        double dy = boundsMax[1] - boundsMin[1];
        double dz = boundsMax[2] - boundsMin[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz) * 2.0;
    }

    private JSONObject createNode(int index) {
        JSONObject node = new JSONObject();
        node.put("name", this.name);
        node.put("model", index);
        return node;
    }

    private JSONObject createModel(URI modelUri, Path modelPath) {
        return createModel(modelUri, modelPath, this.unit);
    }

    private JSONObject createModel(URI modelUri, Path modelPath, String unit) {
        JSONObject model = new JSONObject();
        model.put("units", unit);

        if (translation != null) {
            model.put("translation", new JSONArray(translation));
        }

        if (boundsMin != null && boundsMax != null) {
            JSONObject bbox = new JSONObject();
            bbox.put("min", new JSONArray(boundsMin));
            bbox.put("max", new JSONArray(boundsMax));
            model.put("boundingBox", bbox);
        }

        JSONObject derivative = new JSONObject();
        derivative.put("usage", "Web3D");
        derivative.put("quality", "High");

        JSONObject asset = new JSONObject();
        asset.put("uri", modelUri.toString());
        asset.put("type", get3DObjectType(modelUri));
        try {
            asset.put("byteSize", Files.size(modelPath));
        } catch (IOException e) {
            logger.warn("Unable to determin file size of 3d model at {}", modelPath);
        }

        derivative.put("assets", List.of(asset));

        model.put("derivatives", List.of(derivative));

        return model;

    }

    private String get3DObjectType(URI modelUri) {
        String extension = FilenameUtils.getExtension(modelUri.getPath());
        if (StringUtils.isBlank(extension)) {
            return "";
        } else {
            switch (extension.toLowerCase()) {
                case "obj", "ply":
                    return "Geometry";
                case "gltf", "glb":
                default:
                    return "Model";
            }
        }
    }

}
