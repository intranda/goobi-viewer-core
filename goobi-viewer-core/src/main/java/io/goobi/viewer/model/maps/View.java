package io.goobi.viewer.model.maps;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Determines the visible area of a {@link GeoMap}. Consists of a zoom factor and a center point
 * @author florian
 *
 */
public class View {

    private final double zoom;
    private final Point center;
    
    public View(double zoom, Point center) {
        this.zoom = zoom;
        this.center = center;
    }
    
    public View(double zoom, double lng, double lat) {
        this(zoom, new Point(lng, lat));
    }
    
    public String getGeoJson() {
        JSONObject json = new JSONObject();
        json.put("zoom", this.zoom);
        json.put("center", List.of(this.center.lng, this.center.lat));
        return json.toString();
    }
    
    public static View fromGeoJson(String s) {
        JSONObject json = new JSONObject(s);
        double zoom = json.getLong("zoom");
        JSONArray pos = json.getJSONArray("center");
        double lng = pos.getDouble(0);
        double lat = pos.getDouble(1);
        return new View(zoom, lng, lat);        
    }
    
    @Override
        public String toString() {
            return getGeoJson();
        }
    
}
