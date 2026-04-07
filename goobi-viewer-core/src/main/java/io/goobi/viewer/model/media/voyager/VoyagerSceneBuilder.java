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

public class VoyagerSceneBuilder {

    private static final Logger logger = LogManager.getLogger(VoyagerSceneBuilder.class);

    private final Map<URI, Path> models = new LinkedHashMap<>();
    private final String name;
    private String unit = "mm";

    public VoyagerSceneBuilder(String name) {
        this.name = name;
    }

    public VoyagerSceneBuilder setUnit(String unit) {
        this.unit = unit;
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

        JSONObject json = new JSONObject();
        json.put("asset", createAsset());
        json.put("scene", 0); //always set the first scene
        json.put("scenes", scenes);
        json.put("nodes", nodes);
        json.put("models", models);

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
        return scene;
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
