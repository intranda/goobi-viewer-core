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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.Path;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class VoyagerSceneBuilderTest {

    private static final double DELTA = 1e-6;

    /**
     * @verifies include a setups array with one orbit navigation entry
     * @see VoyagerSceneBuilder#build()
     */
    @Test
    void build_shouldIncludeOrbitSetupSection() {
        String json = new VoyagerSceneBuilder("TestModel").build();
        JSONObject root = new JSONObject(json);

        assertTrue(root.has("setups"), "setups array missing");
        JSONArray setups = root.getJSONArray("setups");
        assertEquals(1, setups.length());

        JSONObject navigation = setups.getJSONObject(0).getJSONObject("navigation");
        assertEquals("Orbit", navigation.getString("type"));
        assertTrue(navigation.getBoolean("autoZoom"));
    }

    /**
     * @verifies reference setup 0 from the first scene
     * @see VoyagerSceneBuilder#build()
     */
    @Test
    void build_shouldReferenceSetupFromScene() {
        String json = new VoyagerSceneBuilder("TestModel").build();
        JSONObject root = new JSONObject(json);

        JSONObject scene = root.getJSONArray("scenes").getJSONObject(0);
        assertTrue(scene.has("setup"), "scene should have setup reference");
        assertEquals(0, scene.getInt("setup"));
    }

    /**
     * @verifies include a boundingBox in the model when bounds are set
     * @see VoyagerSceneBuilder#setBounds(double[], double[])
     */
    @Test
    void build_shouldIncludeModelBoundingBoxWhenBoundsSet() {
        String json = new VoyagerSceneBuilder("TestModel")
                .addModel(URI.create("https://example.com/model.glb"), Path.of("/nonexistent.glb"))
                .setBounds(new double[] { -1.0, -2.0, -3.0 }, new double[] { 1.0, 2.0, 3.0 })
                .build();
        JSONObject root = new JSONObject(json);

        JSONObject model = root.getJSONArray("models").getJSONObject(0);
        assertTrue(model.has("boundingBox"), "model should have boundingBox");
        JSONObject bbox = model.getJSONObject("boundingBox");
        assertArrayEquals(new double[] { -1.0, -2.0, -3.0 }, toDoubleArray(bbox.getJSONArray("min")), DELTA);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, toDoubleArray(bbox.getJSONArray("max")), DELTA);
    }

    /**
     * @verifies not include a boundingBox when no bounds are set
     * @see VoyagerSceneBuilder#build()
     */
    @Test
    void build_shouldNotIncludeModelBoundingBoxWhenNoBoundsSet() {
        String json = new VoyagerSceneBuilder("TestModel")
                .addModel(URI.create("https://example.com/model.glb"), Path.of("/nonexistent.glb"))
                .build();
        JSONObject root = new JSONObject(json);

        JSONObject model = root.getJSONArray("models").getJSONObject(0);
        assertFalse(model.has("boundingBox"), "model should not have boundingBox when none was set");
    }

    /**
     * @verifies compute a positive camera distance from the bounding box diagonal
     * @see VoyagerSceneBuilder#setBounds(double[], double[])
     */
    @Test
    void build_shouldComputePositiveCameraDistanceFromBounds() {
        // 10×10×10 box → diagonal ≈ 17.3
        String json = new VoyagerSceneBuilder("TestModel")
                .setBounds(new double[] { -5.0, -5.0, -5.0 }, new double[] { 5.0, 5.0, 5.0 })
                .build();
        JSONObject root = new JSONObject(json);

        JSONArray offset = root.getJSONArray("setups")
                .getJSONObject(0)
                .getJSONObject("navigation")
                .getJSONObject("orbit")
                .getJSONArray("offset");
        double distance = offset.getDouble(2);
        assertTrue(distance > 0, "camera distance must be positive");
        assertTrue(distance > 10.0, "camera distance should be substantially larger than half the diagonal (8.66)");
    }

    /**
     * @verifies set the translation on the model object not on the scene node
     * @see VoyagerSceneBuilder#setTranslation(double, double, double)
     */
    @Test
    void build_shouldSetTranslationOnModelNotOnNode() {
        String json = new VoyagerSceneBuilder("TestModel")
                .addModel(URI.create("https://example.com/model.glb"), Path.of("/nonexistent.glb"))
                .setTranslation(1.0, 2.0, 3.0)
                .build();
        JSONObject root = new JSONObject(json);

        JSONObject node = root.getJSONArray("nodes").getJSONObject(0);
        assertFalse(node.has("translation"), "node should not have translation");

        JSONObject model = root.getJSONArray("models").getJSONObject(0);
        assertTrue(model.has("translation"), "model should have translation");
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, toDoubleArray(model.getJSONArray("translation")), DELTA);
    }

    // --- helper ---

    private static double[] toDoubleArray(JSONArray arr) {
        return new double[] { arr.getDouble(0), arr.getDouble(1), arr.getDouble(2) };
    }
}
