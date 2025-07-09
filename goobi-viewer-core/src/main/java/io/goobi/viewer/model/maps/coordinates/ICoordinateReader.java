package io.goobi.viewer.model.maps.coordinates;

import mil.nga.sf.geojson.Geometry;

public interface ICoordinateReader {

    public boolean canRead(String value);

    public Geometry read(String value);

}
