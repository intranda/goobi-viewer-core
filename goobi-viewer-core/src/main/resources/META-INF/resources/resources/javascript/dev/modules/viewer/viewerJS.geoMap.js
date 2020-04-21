/**
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 * 
 * Visit these websites for more information. - http://www.intranda.com -
 * http://digiverso.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Javascript interface for geoMap.tag
 * GeoJson coordinates are always [lng, lat]
 * 
 * @version 3.4.0
 * @module viewerJS.geoMap
 * @requires jQuery
 */

var viewerJS = ( function( viewer ) {
    'use strict'; 
    
    // default variables
    var _debug = false;
    
    var _defaults = {
            mapId : "geomap",
            minZoom : 0,
            maxZoom : 20,
            initialView : {
                zoom: 5,
                center: [49.451993, 11.073397] //lat, long
            },
            allowMovingFeatures: true
    }
    
    viewer.GeoMap = function(config) {
        
        if (typeof L == "undefined") {
            throw "leaflet.js is not loaded";
        }
        
        this.markerIdCounter = 1;
        this.markers = [];
        
        this.config = $.extend( true, {}, _defaults, config );
        
        this.onMapClick = new Rx.Subject();
        this.onFeatureClick = new Rx.Subject();
        this.onFeatureMoved = new Rx.Subject();

    }
    
    viewer.GeoMap.prototype.init = function() {
        this.map = new L.Map(this.config.mapId);
        var osm = new L.TileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
          minZoom: this.config.minZoom,
          maxZoom: this.config.maxZoom,
          attribution: 'Map data &copy; <a href="https://openstreetmap.org">OpenStreetMap</a> contributors'
        });
     
        // define view
        this.setView(this.config.initialView);
        this.map.addLayer(osm);
        
        this.locations = L.geoJSON([], {
            pointToLayer: function(geoJsonPoint, latlng) {
                let marker = L.marker(latlng, {
                    draggable: this.config.allowMovingFeatures
                });
                             
                marker.id = geoJsonPoint.id;
                marker.view = geoJsonPoint.view;
                    
                marker.getId = function() {
                    return this.id;
                }
                
                marker.on("dragend", function(event) {
                    let position = marker.getLatLng();
                    marker.feature.geometry = marker.toGeoJSON().geometry;
                    marker.feature.view = {zoom: this.map.getZoom(), center: [marker.getLatLng().lng, marker.getLatLng().lat]};
                    console.log("position ", position, marker.feature);
                    this.onFeatureMoved.next(marker.feature);
                }.bind(this));
                
                marker.on("click", function(event) {
                    this.onFeatureClick.next(marker.feature);
                }.bind(this));
                
                this.markers.push(marker);    
                
                return marker;
            }.bind(this)
        }).addTo(this.map);
            
        
        this.map.on("click", function(e) {
                var location= e.latlng;
                let geoJson = this.createGeoJson(location, this.map.getZoom(), this.map.getCenter());
                this.onMapClick.next(geoJson);
        }.bind(this))

    }
    
    /**
     * Center must be an array containing latitude and longitude as numbers - in that order
     * zoom must be a number
     */
    viewer.GeoMap.prototype.setView = function(view) {
        this.map.setView(view.center, view.zoom);
    }
    
    viewer.GeoMap.prototype.resetMarkers = function() {
        this.markerIdCounter = 1;
        this.markers.forEach((marker) => {
            marker.remove();
        })
        this.markers = [];
    }

    viewer.GeoMap.prototype.createGeoJson = function(location, zoom, center) {
        let id = this.markerIdCounter++;
        var geojsonFeature = {
                "type": "Feature",
                "id": id,
                "properties": {
                    "name": "",
                },
                "geometry": {
                    "type": "Point",
                    "coordinates": [location.lng, location.lat]
                },
                "view": {
                    "zoom": zoom,
                    "center": [location.lng, location.lat]
                }
            };
        return geojsonFeature;
    }

    viewer.GeoMap.prototype.getMarker = function(id) {
        return this.markers.find(marker => marker.getId() == id);
    }

    viewer.GeoMap.prototype.addMarker = function(geoJson) {
        let id = this.markerIdCounter++;
        geoJson.id = id;
        this.locations.addData(geoJson);
        return id;
    }


    viewer.GeoMap.prototype.removeMarker = function(feature) {
        let marker = this.getMarker(feature.id);
        marker.remove();
        let index = this.markers.indexOf(marker);
        this.markers.splice(index, 1);
    }
    
    viewer.GeoMap.prototype.close = function() {
        this.onMapClick.complete();
        this.onFeatureClick.complete();
        this.onFeatureMoved.complete();
        if(this.map) {
            this.map.remove();
        }
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );


