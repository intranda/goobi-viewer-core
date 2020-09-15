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
            maxZoom : 19,
            initialView : {
                zoom: 5,
                center: [11.073397, 49.451993] //long, lat
            },
            allowMovingFeatures: false,
            mapBoxToken : undefined,
            mapBoxWorldId: "mapbox.world-bright",
            language: "de",
            popover: undefined,
            emptyMarkerMessage: undefined,
            popoverOnHover: false,
            fixed: false
            
    }
    
    viewer.GeoMap = function(config) {
        
        if (typeof L == "undefined") {
            throw "leaflet.js is not loaded";
        }
        this.config = $.extend( true, {}, _defaults, config );
        if(_debug) {
            console.log("load GeoMap with config ", this.config);
        }
        
        this.markerIdCounter = 1;
        this.markers = [];
        
        
        this.onMapRightclick = new rxjs.Subject();
        this.onMapClick = new rxjs.Subject();
        this.onFeatureClick = new rxjs.Subject();
        this.onFeatureMove = new rxjs.Subject();
        this.onMapMove = new rxjs.Subject();


    }
    
    viewer.GeoMap.prototype.init = function(view, features) {
        if(this.map) {
            this.map.remove();
        }
        
        if(!this.config.mapBoxToken) {
            this.config.mapBoxToken = viewerJS.getMapBoxToken();
        }
        if(_debug) {
            console.log("init GeoMap with config ", this.config);
        }
        
        this.map = new L.Map(this.config.mapId, {
            zoomControl: !this.config.fixed,
            doubleClickZoom: !this.config.fixed,
            scrollWheelZoom: !this.config.fixed,
            dragging: !this.config.fixed,    
            keyboard: !this.config.fixed
        });
        
        if(this.config.mapBoxToken) {
            var mapbox = new L.TileLayer(
                    'https://api.mapbox.com/styles/v1/mapbox/streets-v11/tiles/{z}/{x}/{y}?access_token=' + this.config.mapBoxToken, {
                        tileSize: 512,
                        zoomOffset: -1,
                        attribution: '© <a href="https://apps.mapbox.com/feedback/">Mapbox</a> © <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                    });
            if(_debug) {                
                console.log("Add mapbox layer");
            }
            this.map.addLayer(mapbox);
        } else {            
            var osm = new L.TileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                minZoom: this.config.minZoom,
                maxZoom: this.config.maxZoom,
                attribution: 'Map data &copy; <a href="https://openstreetmap.org">OpenStreetMap</a> contributors'
            });
            if(_debug) {                         
                console.log("add openStreatMap layer");
            }
            this.map.addLayer(osm);
        }
        
//        this.setView(this.config.initialView);
        
        //init map events
        rxjs.fromEvent(this.map, "moveend").pipe(rxjs.operators.map(e => this.getView())).subscribe(this.onMapMove);
        rxjs.fromEvent(this.map, "contextmenu")
        .pipe(rxjs.operators.map(e => this.createGeoJson(e.latlng, this.map.getZoom(), this.map.getCenter())))
        .subscribe(this.onMapRightclick);
        rxjs.fromEvent(this.map, "click")
        .pipe(rxjs.operators.map(e => this.createGeoJson(e.latlng, this.map.getZoom(), this.map.getCenter())))
        .subscribe(this.onMapClick);
    
        //init feature layer
        this.locations = L.geoJSON([], {
            
            pointToLayer: function(geoJsonPoint, latlng) {
                let marker = L.marker(latlng, {
                    draggable: this.config.allowMovingFeatures,
                    icon: this.getMarkerIcon()
                });
                marker.id = geoJsonPoint.id;
                marker.view = geoJsonPoint.view;
                    
                marker.getId = function() {
                    return this.id;
                }
                
                rxjs.fromEvent(marker, "dragend")
                .pipe(rxjs.operators.map(() => this.openPopup(marker)), rxjs.operators.map(() => this.updatePosition(marker)))
                .subscribe(this.onFeatureMove);
                rxjs.fromEvent(marker, "click").pipe(rxjs.operators.map(e => marker.feature)).subscribe(this.onFeatureClick);
                
                if(this.config.popover) {                    
                    if(this.config.popoverOnHover) {                    
                        rxjs.fromEvent(marker, "mouseover").subscribe(() => marker.openPopup());
                        rxjs.fromEvent(marker, "mouseout").subscribe(() => marker.closePopup());
                    }
                    marker.bindPopup(() => this.createPopup(marker),{closeButton: !this.config.popoverOnHover});
                }
                
                this.markers.push(marker);    
                if(this.cluster) {
                    this.cluster.addLayer(marker);
                }
                return marker;
            }.bind(this)
        });
        
        if(this.config.clusterMarkers) {        
            try {                
                this.cluster = this.createMarkerCluster();
                this.map.addLayer(this.cluster);
            } catch(error) {
                console.warn(error);
                this.map.addLayer(this.locations);
            }
        } else {
            this.map.addLayer(this.locations);
        }
        
        
        if(features && features.length > 0) {
            features.forEach(feature => {
                this.addMarker(feature);
            })
            let zoom = view ? view.zoom : this.config.initialView.zoom;
            this.setView(this.getViewAroundFeatures(zoom));
        } else if(view){                                                    
            this.setView(view);
        }
        
    }
    
    viewer.GeoMap.prototype.createMarkerCluster = function() {
        let cluster = L.markerClusterGroup({
            maxClusterRadius: 80,
            zoomToBoundsOnClick: !this.config.fixed,
            iconCreateFunction: function(cluster) {
                return this.getClusterIcon(cluster.getChildCount());
            }.bind(this)
        });
        if(!this.config.fixed) {            
            cluster.on('clustermouseover', function (a) {
                this.removePolygon();
                
//            a.layer.setOpacity(0.2);
                this.shownLayer = a.layer;
                this.polygon = L.polygon(a.layer.getConvexHull());
                this.map.addLayer(this.polygon);
            }.bind(this));
            cluster.on('clustermouseout', () => this.removePolygon());
            this.map.on('zoomend', () => this.removePolygon());
        }
        return cluster;
    }
    
    viewer.GeoMap.prototype.removePolygon = function() {
        if (this.shownLayer) {
            this.shownLayer.setOpacity(1);
            this.shownLayer = null;
        }
        if (this.polygon) {
            this.map.removeLayer(this.polygon);
            this.polygon = null;
        }
    };
    
    viewer.GeoMap.prototype.openPopup = function(marker) {
        try{
            marker.openPopup();
        } catch(e) {
            //swallow
        }
    }
    
    viewer.GeoMap.prototype.setMarkerIcon = function(icon) {
        this.markerIcon = icon;
        if(this.markerIcon) {            
            this.markerIcon.name = "";
        }
    }
    
    viewer.GeoMap.prototype.getClusterIcon = function(num) {
        let iconConfig = {
            icon: "fa-number",
            number: num,
            svg: true,
            prefix: "fa",
            iconRotate: 0
        };
        if(this.markerIcon) {
            iconConfig = $.extend(true, {}, this.markerIcon, iconConfig);
        }
        let icon = L.ExtraMarkers.icon(iconConfig);
        return icon;
    }
    
    viewer.GeoMap.prototype.getMarkerIcon = function() {
        if(this.markerIcon) {       
            let icon = L.ExtraMarkers.icon(this.markerIcon);
            if(this.markerIcon.shadow === false) {                
                icon.options.shadowSize = [0,0];
            }
            return icon;
        } else {
            return new L.Icon.Default();
        }
    }

    
    viewer.GeoMap.prototype.updatePosition = function(marker) {
        marker.feature.geometry = marker.toGeoJSON().geometry;
        marker.feature.view = {zoom: this.map.getZoom(), center: [marker.getLatLng().lng, marker.getLatLng().lat]};
        return marker.feature;
    }

    
    viewer.GeoMap.prototype.createPopup = function(marker) {
        let title = viewerJS.getMetadataValue(marker.feature.properties.title, this.config.language);
        let desc = viewerJS.getMetadataValue(marker.feature.properties.description, this.config.language);
        if(this.config.popover && (title || desc) ) {
            let $popover = $(this.config.popover).clone();
            $popover.find("[data-metadata='title']").text(title);
            $popover.find("[data-metadata='description']").html(desc);
            $popover.css("display", "block");
            return $popover.get(0);
        } else if(this.config.popover){
            console.log("empty marker message ", this.config.popover);
            return this.config.emptyMarkerMessage;
        }
    }
    
    /**
     * Center must be an array containing longitude andlatitude as numbers - in that order
     * zoom must be a number
     */
    viewer.GeoMap.prototype.setView = function(view) {
        if(_debug) {
            console.log("set view to ", view);
        }
        if(!view) {
            return;
        } else if(typeof view === "string") {
            view = JSON.parse(view);
        }
        view.zoom = Math.max(view.zoom, 1);
        if(view.center) {
            let center = L.latLng(view.center[1], view.center[0]);
            if(view.zoom) {
                this.map.setView(center, view.zoom);
            } else {                
                this.map.panTo(center);
            }
        } else if(view.zoom) {            
            this.map.setZoom(view.zoom);
        }
    }
    
    viewer.GeoMap.prototype.getView = function() {
        let zoom  = this.map.getZoom();
        let center = this.map.getCenter();
        return {
            "zoom": zoom,
            "center": [center.lng, center.lat]
        }
    }
    
    viewer.GeoMap.prototype.getViewAroundFeatures = function(defaultZoom) {
        if(!defaultZoom) {
            defaultZoom = this.map.getZoom();
        }
        let features = this.getFeatures();
        if(features.length == 0) {
            return undefined;
        } else if(features.length == 1) {
            return {
                "zoom": defaultZoom,
                "center": features[0].geometry.coordinates
            }
        } else {
            let points = features.map(f => f.geometry.coordinates).map(c =>  L.latLng(c[1], c[0]));
            let bounds = L.latLngBounds(points).pad(0.2);
            let center = bounds.getCenter();
            return {
                "zoom": Math.min(this.map.getBoundsZoom(bounds), defaultZoom),
                "center": [center.lng, center.lat]
            }
        }
    }
    
    viewer.GeoMap.prototype.updateMarker = function(id) {
        let marker = this.getMarker(id);
        if(marker) {            
            marker.openPopup();
        }
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
                    "title": "",
                    "description":""
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
    
    viewer.GeoMap.prototype.getFeatures = function(id) {
        return this.markers.map(marker => marker.feature);
    }

    viewer.GeoMap.prototype.addMarker = function(geoJson) {
        let id = this.markerIdCounter++;
        geoJson.id = id;
        this.locations.addData(geoJson);
        let marker = this.getMarker(id);
        if(_debug) {            
            console.log("Add marker ", marker);
        }
        return marker;
    }


    viewer.GeoMap.prototype.removeMarker = function(feature) {
        let marker = this.getMarker(feature.id);
        marker.remove();
        let index = this.markers.indexOf(marker);
        this.markers.splice(index, 1);
    }
    
    viewer.GeoMap.prototype.getZoom = function() {
        return this.map.getZoom();
    }

    viewer.GeoMap.prototype.getMarkerCount = function() {
        return this.markers.length;
    }

    
    viewer.GeoMap.prototype.close = function() {
        this.onMapClick.complete();
        this.onFeatureClick.complete();
        this.onFeatureMove.complete();
        this.onMapMove.complete();
        if(this.map) {
            this.map.remove();
        }
    }

    return viewer;
    
} )( viewerJS || {}, jQuery );


