package io.goobi.viewer.model.maps.coordinates;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import mil.nga.sf.geojson.FeatureConverter;
import mil.nga.sf.geojson.Geometry;
import mil.nga.sf.geojson.GeometryCollection;

public class GeoJsonReader implements ICoordinateReader {

    @Override
    public boolean canRead(String value) {
        try {
            new JSONObject(value);
        } catch (JSONException e) {
            return false;
        }
        return true;
    }

    @Override
    public Geometry read(String value) {
        JSONObject json = new JSONObject(value);
        if (json.has("coordinates")) {
            return FeatureConverter.toGeometry(value);
        } else if (json.has("geometry")) {
            return read(json.getJSONObject("geometry").toString());
        } else if (json.has("features")) {
            List<Geometry> geoEntries = new ArrayList<>();
            for (Object entry : json.getJSONArray("features")) {
                if (entry instanceof JSONObject) {
                    geoEntries.add(read(entry.toString()));
                }
            }
            if (geoEntries.size() == 1) {
                return geoEntries.get(0);
            } else {
                GeometryCollection collection = new GeometryCollection();
                collection.setGeometries(geoEntries);
                return collection;
            }
        } else {
            throw new IllegalArgumentException("Cannot read json '" + value + "' as geojson or geometry");
        }
    }

}
