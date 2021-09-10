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
            mapBoxToken : undefined,
            language: "de",
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

        this.layers = [];
        
        this.onMapRightclick = new rxjs.Subject();
        this.onMapClick = new rxjs.Subject();
        this.onMapMove = new rxjs.Subject();
        this.initialized = new Promise( (resolve, reject) => {
        	this.resolveInitialization = resolve;
        	this.rejectInitialization = reject;
        });

        new viewer.GeoMap.featureGroup(this, this.config.layer);

		viewer.GeoMap.allMaps.push(this);
    }
    
    viewer.GeoMap.prototype.init = function(view, features) {
       
       if(_debug)console.log("init geomap with", view, features);
       
        if(this.map) {
            this.map.remove();
        }
        //init mapBox config. If no config object is set in viewerJS, only get token from viewerJS
        //if that doesn't exists, don't create mapBox config
        if(!this.config.mapBox && viewerJS.getMapBoxToken()) {
            if(viewerJS.mapBoxConfig) {
                this.config.mapBox = viewerJS.mapBoxConfig;
            } else {
                this.config.mapBox = {
                        token : viewerJS.getMapBoxToken()
                }
            }
        }
        if(this.config.mapBox && !this.config.mapBox.user) {
            this.config.mapBox.user = "mapbox";
        }
        if(this.config.mapBox && !this.config.mapBox.styleId) {
            this.config.mapBox.styleId = "streets-v11";
        }
        
        if(_debug) {
            console.log("init GeoMap with config ", this.config);
        }
        
        this.map = new L.Map(this.config.element ? this.config.element : this.config.mapId, {
            zoomControl: !this.config.fixed,
            doubleClickZoom: !this.config.fixed,
            scrollWheelZoom: !this.config.fixed,
            dragging: !this.config.fixed,    
            keyboard: !this.config.fixed,
            // Fix desktop safari browsers: 
            // disabling the tap option shows popups when clicking on geoMap markers in safari
            // it should however be set to true when a mobile version of Safari is used
            tap: viewer.iOS() ? true : false
        });
        this.htmlElement = this.map._container;
        
        this.map.whenReady(e => {
        	this.resolveInitialization(this);
        });
        
        if(this.config.mapBox) {
            let url = 'https://api.mapbox.com/styles/v1/{1}/{2}/tiles/{z}/{x}/{y}?access_token={3}'
                .replace("{1}", this.config.mapBox.user)
                .replace("{2}", this.config.mapBox.styleId)
                .replace("{3}", this.config.mapBox.token);
            var mapbox = new L.TileLayer(url, {
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
        
        //this.setView(this.config.initialView);
        
        //init map events
        rxjs.fromEvent(this.map, "moveend").pipe(rxjs.operators.map(e => this.getView())).subscribe(this.onMapMove);
        rxjs.fromEvent(this.map, "contextmenu")
        .pipe(rxjs.operators.map(e => this.layers[0].createGeoJson(e.latlng, this.map.getZoom(), this.map.getCenter())))
        .subscribe(this.onMapRightclick);
        rxjs.fromEvent(this.map, "click")
        .pipe(rxjs.operators.map(e => this.layers[0].createGeoJson(e.latlng, this.map.getZoom(), this.map.getCenter())))
        .subscribe(this.onMapClick);
    
        
       	this.layers[0].init(features, false);
        if(features && features.length > 0) {
        	this.layers[0].setViewToFeatures(true)
            
        } else if(view){                                                    
            this.setView(view);
        }
        
        return this.initialized;
        
    }
    
    viewer.GeoMap.prototype.initGeocoder = function(element, config) {
    	if(this.config.mapBox && this.config.mapBox.token) {
	    	config = $.extend(config ? config: {}, {
	    		accessToken : this.config.mapBox.token,
	    		mapboxgl: mapboxgl
	    	});
	    	if(_debug)console.log("init geocoder with config" , config);
	    	this.geocoder = new MapboxGeocoder(config);
	    	this.geocoder.addTo(element);
	    	this.geocoder.on("result", (event) => {
	    		//console.log("geocoder result",  event.result, event.result.center, event.result.place_type, event.result.place_name);
	    		
	    		if(event.result.bbox) {
	    			let p1 = new L.latLng(event.result.bbox[1], event.result.bbox[0]);
	    			let p2 = new L.latLng(event.result.bbox[3], event.result.bbox[2]);
	    			let bounds = new L.latLngBounds(p1, p2);
	    			this.map.fitBounds(bounds);
	    		} else {
		    		let view = {
		                "zoom": this.config.maxZoom,
		                "center": event.result.center
		            }
		            this.setView(view);
	    		}
	    	});
    	} else {
    		console.warn("Cannot initialize geocoder: No mapbox token");
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
        this.view = view;
        if(!view) {
            return;
        } else if(typeof view === "string") {
            view = JSON.parse(view);
        }
        view.zoom = (view.zoom == undefined || Number.isNaN(view.zoom)) ? 1 : Math.max(view.zoom, 1);
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
    
    viewer.GeoMap.prototype.getViewAroundFeatures = function(features, defaultZoom, zoomPadding) {
        if(!defaultZoom) {
            defaultZoom = this.map.getZoom();
        }
        if(!zoomPadding) {
        	zoomPadding = 0.2;
        }
        if(!features || features.length == 0) {
            return undefined;
        } else {
            if(_debug) {
        	console.log("view around ", features);
            }
        	let bounds = L.latLngBounds();
        	features.map(f => L.geoJson(f).getBounds()).forEach(b => bounds.extend(b));
            let center = bounds.getCenter();
            let diameter = this.getDiameter(bounds);
            return {
                "zoom": diameter > 0 ?  Math.max(1, this.map.getBoundsZoom(bounds.pad(zoomPadding))) : defaultZoom,
                "center": [center.lng, center.lat]
            }
        }
    }

    
    viewer.GeoMap.prototype.getZoom = function() {
        return this.map.getZoom();
    }

    
    viewer.GeoMap.prototype.close = function() {
        this.onMapClick.complete();
        this.layers.forEach(l => l.close());
        this.onMapMove.complete();
        if(this.map) {
            this.map.remove();
        }
    }
    
    // FEATURE GROUP
    var _defaults_featureGroup = {
            allowMovingFeatures: false,
            clusterMarkers : false,
            popover: undefined,
            emptyMarkerMessage: undefined,
            popoverOnHover: false,
            markerIcon : undefined,
            style: {
            	stroke: true,
            	color: '#3388ff',
            	highlightColor: '#d9534f',
            	wight: 3,
            	opactity: 1.0,
            	fill: true,
            	fillColor: undefined, //defaults to color
            	fillOpacity: 0.1,
            }
    }
    
        
    viewer.GeoMap.featureGroup = function(geoMap, config) {
 		this.geoMap = geoMap;
 		this.geoMap.layers.push(this);
        this.config = $.extend( true, {}, _defaults_featureGroup, geoMap.config.layer, config );
        if(_debug) {
            console.log("create featureGroup with config ",  config);
        }

		this.markerIdCounter = 1;
        this.markers = [];
        this.areas = [];
        this.highlighted = []; //list of currently highlighted colors

        this.onFeatureClick = new rxjs.Subject();
        this.onFeatureMove = new rxjs.Subject();

    }
    
    viewer.GeoMap.featureGroup.prototype.init = function(features, zoomToFeatures) {
       
       if(_debug)console.log("init featureGroup ", features);
        
        this.markerIdCounter = 1;
        this.markers = [];
        this.areas = [];
        this.highlighted = []; //list of currently highlighted colors


        if(this.layer) {
        	this.geoMap.map.removeLayer(this.layer);
        }
		this.layer = new L.FeatureGroup();
        
        //init feature layer
        this.locations = L.geoJSON([], {
            
            style: function(feature) {
            	if(feature.properties && feature.properties.highlighted) {
					let style = $.extend(true, {}, this.config.style);
					style.color = this.config.style.highlightColor;
					return style;       		
            	} else {
	            	return this.config.style;
            	}
            }.bind(this),
            
            pointToLayer: function(geoJsonPoint, latlng) {
                let marker = L.marker(latlng, {
                    draggable: this.config.allowMovingFeatures,
                    icon: this.getMarkerIcon(geoJsonPoint.properties && geoJsonPoint.properties.highlighted)
                });
                return marker;
            }.bind(this),
            
            onEachFeature: function(feature, layer) {
            	if(_debug)console.log("onEachFeature ", feature, layer, this);
            	
            	layer.id = feature.id;
                layer.view = feature.view;
                    
                layer.getId = function() {
                    return this.id;
                }
                
                rxjs.fromEvent(layer, "dragend")
                .pipe(rxjs.operators.map(() => this.openPopup(layer)), rxjs.operators.map(() => this.updatePosition(layer)))
                .subscribe(this.onFeatureMove);
                rxjs.fromEvent(layer, "click").pipe(rxjs.operators.map(e => layer.feature)).subscribe(this.onFeatureClick);

				let title = viewerJS.getMetadataValue(feature.properties.title, this.config.language);
       			let desc = viewerJS.getMetadataValue(feature.properties.description, this.config.language);                
       			if(this.config.popover && feature.properties && (title || desc || this.config.emptyMarkerMessage)) {                    
                    if(this.config.popoverOnHover) {                    
                        rxjs.fromEvent(layer, "mouseover").subscribe(() => layer.openPopup());
                        rxjs.fromEvent(layer, "mouseout").subscribe(() => layer.closePopup());
                    }
                    layer.bindPopup(() => this.createPopup(layer),{
                    	closeButton: !this.config.popoverOnHover,
                    	autoPan: false,
                    	closeOnClick: false,
                    });
                }
            	this.markers.push(layer);    
                if(this.cluster) {
                    this.cluster.addLayer(layer);
                }
            }.bind(this)
        });
        
        //add layer
		this.geoMap.map.addLayer(this.layer);
        if(this.config.clusterMarkers) {        
            try {                
                this.cluster = this.createMarkerCluster();
                this.layer.addLayer(this.cluster);
            } catch(error) {
                console.warn(error);
                this.layer.addLayer(this.locations);
            }
        } else {
            this.layer.addLayer(this.locations);
        }
        
        
        if(features && features.length > 0) {
            features
            .sort( (f1,f2) => this.compareFeatures(f1,f2) )
            .forEach(feature => {
            	let type = feature.geometry.type;
            	if(_debug)console.log("add feature for " + type, feature);
            	this.addMarker(feature);
            })
        }
        
        if(zoomToFeatures) {
        	this.setViewToFeatures(true);
        }
        
    }
    
    viewer.GeoMap.featureGroup.prototype.setViewToFeatures = function(setViewToHighlighted) {
    	let features = this.getFeatures();
    	if(features && features.length > 0) {
            let zoom = this.geoMap.view ? this.geoMap.zoom : this.geoMap.config.initialView.zoom;
            let highlightedFeatures = features.filter(f => f.properties.highlighted);
            //console.log(" highlightedFeatures", highlightedFeatures);
            if(setViewToHighlighted && highlightedFeatures.length > 0) {
            	let viewAroundFeatures = this.geoMap.getViewAroundFeatures(highlightedFeatures, zoom, 0.5);
	            this.geoMap.setView(viewAroundFeatures);
            } else {
	            let viewAroundFeatures = this.geoMap.getViewAroundFeatures(features, zoom);
	            this.geoMap.setView(viewAroundFeatures);
            }
        }
    }
    
        viewer.GeoMap.featureGroup.prototype.isEmpty = function() {
        	return this.markers.length == 0 && this.areas.length == 0;
        }
    
    
    
    viewer.GeoMap.featureGroup.prototype.compareFeatures = function(f1, f2) {
    
    	return this.getSize(f2) - this.getSize(f1);
    
    }
    
    
    viewer.GeoMap.featureGroup.prototype.getSize = function(feature) {
    	if(feature.geometry.type == "Point") {
    		return 0;
    	} else {
    		let polygon = L.polygon(feature.geometry.coordinates);
    		return this.geoMap.getDiameter(polygon.getBounds());
    	}
    }

    
    viewer.GeoMap.featureGroup.prototype.createMarkerCluster = function() {
        let cluster = L.markerClusterGroup({
            maxClusterRadius: 80,
            zoomToBoundsOnClick: !this.geoMap.config.fixed,
            iconCreateFunction: function(cluster) {
                return this.getClusterIcon(cluster.getChildCount());
            }.bind(this)
        });
        if(!this.geoMap.config.fixed) {            
            cluster.on('clustermouseover', function (a) {
                this.removePolygon();
                
//            a.layer.setOpacity(0.2);
                this.shownLayer = a.layer;
                this.polygon = L.polygon(a.layer.getConvexHull());
                this.layer.addLayer(this.polygon);
            }.bind(this));
            cluster.on('clustermouseout', () => this.removePolygon());
            this.geoMap.map.on('zoomend', () => this.removePolygon());
        }
        return cluster;
    }
    
    viewer.GeoMap.featureGroup.prototype.removePolygon = function() {
        if (this.shownLayer) {
            this.shownLayer.setOpacity(1);
            this.shownLayer = null;
        }
        if (this.polygon) {
            this.geoMap.map.removeLayer(this.polygon);
            this.polygon = null;
        }
    };
    
    viewer.GeoMap.featureGroup.prototype.openPopup = function(marker) {
        try{
            marker.openPopup(); 
        } catch(e) {
            //swallow
        }
    }
    
    viewer.GeoMap.featureGroup.prototype.highlightMarker = function(feature) {
        if(feature) {            
            let marker = this.getMarker(feature.id);
            let icon  = marker.getIcon();
            icon.options.defaultColor = icon.options.markerColor;
            icon.options.markerColor = icon.options.highlightColor;
            marker.setIcon(icon);
            this.highlighted.push(marker);
        } else {
            this.highlighted.forEach(marker => {
                let icon  = marker.getIcon();
                icon.options.markerColor = icon.options.defaultColor;
                icon.options.defaultColor = undefined;
                marker.setIcon(icon);
                let index = this.highlighted.indexOf(marker);
                this.highlighted.splice(index, 1);
            })
        }
    }

    
    viewer.GeoMap.featureGroup.prototype.getClusterIcon = function(num) {
        let iconConfig = {
            icon: "fa-number",
            number: num,
            svg: true,
            prefix: "fa",
            iconRotate: 0
        }; 
        if(this.config.markerIcon) {
            iconConfig = $.extend(true, {}, this.config.markerIcon, iconConfig);
            iconConfig.name = ""; //remove name because it shows up as a label underneath the marker
        }
        let icon = L.ExtraMarkers.icon(iconConfig);
        return icon;
    }
    
    viewer.GeoMap.featureGroup.prototype.getMarkerIcon = function(highlighted) {
        if(this.config.markerIcon && !jQuery.isEmptyObject(this.config.markerIcon)) {
            let icon = L.ExtraMarkers.icon(this.config.markerIcon);
        	icon.options.name = "";	//remove name property to avoid it being displayed on the map
            if(this.config.markerIcon.shadow === false) {                
                icon.options.shadowSize = [0,0];
            }
            if(highlighted) {
            console.log(this.config.markerIcon.highlightColor);
            	icon.options.markerColor = this.config.markerIcon.highlightColor;
            }
            return icon;
        } else {
            return new L.Icon.Default();
        }
    }

    
    viewer.GeoMap.featureGroup.prototype.updatePosition = function(marker) {
        marker.feature.geometry = marker.toGeoJSON().geometry;
        marker.feature.view = {zoom: this.geoMap.map.getZoom(), center: [marker.getLatLng().lng, marker.getLatLng().lat]};
        return marker.feature;
    }

    
    viewer.GeoMap.featureGroup.prototype.createPopup = function(marker) {
        let title = viewerJS.getMetadataValue(marker.feature.properties.title, this.config.language);
        let desc = viewerJS.getMetadataValue(marker.feature.properties.description, this.config.language);
        if(this.config.popover && (title || desc) ) {
            let $popover = $(this.config.popover).clone();
            $popover.find("[data-metadata='title']").text(title);
            $popover.find("[data-metadata='description']").html(desc);
            $popover.css("display", "block");
            return $popover.get(0);
        } else if(this.config.popover){
            return this.config.emptyMarkerMessage;
        }
    }

    viewer.GeoMap.prototype.normalizePoint = function(p) {
//   		let wrapped = new L.latLng(Math.max(Math.min(p.lat, 90), -90), p.lng - Math.round(p.lng/360)*360);
   		let wrapped = new L.latLng(Math.max(Math.min(p.lat, 90), -90), Math.max(Math.min(p.lng, 180), -180))
    	//console.log("Point ",p, wrapped);
    	return wrapped;
    }

        
    viewer.GeoMap.prototype.getDiameter = function(bounds) {
    	if(!bounds || !bounds.isValid()) {
    		return 0;
    	} else {
		    let diameter = this.map.distance(bounds.getSouthWest(), bounds.getNorthEast());
		    return diameter;
    	}
    }
    
    viewer.GeoMap.featureGroup.prototype.getArea = function(bounds, feature) {
    	if(!bounds || !bounds.isValid()) {
    		return 0;
    	} else {
		    let area = this.geoMap.map.distance(bounds.getSouthWest(), bounds.getNorthWest()) * this.geoMap.map.distance(bounds.getSouthWest(), bounds.getSouthEast())
    	}
    }
    
    viewer.GeoMap.featureGroup.prototype.updateMarker = function(id) {
        let marker = this.getMarker(id);
        if(marker) {            
            marker.openPopup();
        }
    }
    
    viewer.GeoMap.featureGroup.prototype.resetMarkers = function() {
        this.markerIdCounter = 1;
        this.markers.forEach((marker) => {
            marker.remove();
        })
        this.markers = [];
    }

    viewer.GeoMap.featureGroup.prototype.getMarker = function(id) {
        return this.markers.find(marker => marker.getId() == id);
    }
    
    viewer.GeoMap.featureGroup.prototype.getFeatures = function(id) {
        return this.markers.map(marker => marker.feature);
    }

    viewer.GeoMap.featureGroup.prototype.addMarker = function(geoJson) {
        let id = this.markerIdCounter++;
        geoJson.id = id;
        this.locations.addData(geoJson);
        let marker = this.getMarker(id);
        if(_debug) {            
            console.log("Add marker ", marker);
        }
        return marker;
    }


    viewer.GeoMap.featureGroup.prototype.removeMarker = function(feature) {
        let marker = this.getMarker(feature.id);
        marker.remove();
        let index = this.markers.indexOf(marker);
        this.markers.splice(index, 1);
    }


    viewer.GeoMap.featureGroup.prototype.createGeoJson = function(location, zoom, center) {
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

    viewer.GeoMap.featureGroup.prototype.getMarkerCount = function() {
        return this.markers.length;
    }
    
        
    viewer.GeoMap.featureGroup.prototype.drawPolygon = function(points, centerView) {
    	let config = $.extend({interactive: false}, this.config.style, true); 
    	let polygon = new L.Polygon(points, config);
    	polygon.addTo(this.geoMap.map);
    	this.areas.push(polygon);
    	if(centerView) {
    		this.geoMap.map.fitBounds(polygon.getBounds());
    	}
    	return polygon;
    }
    
   viewer.GeoMap.featureGroup.prototype.drawRectangle = function(points, centerView) {
    	let config = $.extend({interactive: false}, this.config.style, true); 
    	let rect = new L.Rectangle(points, config);
    	rect.addTo(this.geoMap.map);
    	this.areas.push(rect);
    	if(centerView) {
    		this.geoMap.map.fitBounds(rect.getBounds());
    	}
    	return rect;
    }
    
    viewer.GeoMap.featureGroup.prototype.drawCircle = function(center, radius, centerView) {
    	let config = $.extend({interactive: false}, this.config.style, true); 
    	let circle = new L.Circle(center, radius, config);
    	circle.addTo(this.geoMap.map);
    	this.areas.push(circle);
    	if(centerView) {
    		this.geoMap.map.fitBounds(circle.getBounds());
    	}
    	return circle;
	}
    
    viewer.GeoMap.featureGroup.prototype.isVisible = function() {
		return this.geoMap.map.hasLayer(this.layer);
    }
    
    viewer.GeoMap.featureGroup.prototype.setVisible = function(visible) {
	    if(visible && !this.isVisible()) {
	    	this.geoMap.map.addLayer(this.layer);
	    } else if(!visible) {
	    	this.geoMap.map.removeLayer(this.layer);
	    }
    }
    
    viewer.GeoMap.featureGroup.prototype.close = function() {
	    this.onFeatureClick.complete();
        this.onFeatureMove.complete();	
    }
    
    //static methods to get all loaded maps
    viewer.GeoMap.allMaps = [];
    

    return viewer;
    
} )( viewerJS || {}, jQuery );


