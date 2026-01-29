package io.goobi.viewer.model.media.voyager;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class VoyagerSceneBuilder {

    private final List<URI> models = new ArrayList<>();
    private final String name;
    private String unit = "mm";

    public VoyagerSceneBuilder(String name) {
        this.name = name;
    }

    public VoyagerSceneBuilder setUnit(String unit) {
        this.unit = unit;
        return this;
    }

    public VoyagerSceneBuilder addModel(URI model) {
        this.models.add(model);
        return this;
    }

    public String build() {

        JSONArray models = new JSONArray(this.models.stream().map(this::createModel).toList());
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

    private JSONObject createModel(URI modelUri) {
        return createModel(modelUri, this.unit);
    }

    private JSONObject createModel(URI modelUri, String unit) {
        JSONObject model = new JSONObject();
        model.put("units", unit);

        JSONObject derivative = new JSONObject();
        derivative.put("usage", "Web3D");
        derivative.put("quality", "High");

        JSONObject asset = new JSONObject();
        asset.put("uri", modelUri.toString());
        asset.put("type", get3DObjectType(modelUri));

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
