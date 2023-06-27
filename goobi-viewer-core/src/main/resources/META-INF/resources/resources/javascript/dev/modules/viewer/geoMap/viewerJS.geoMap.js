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
 * Javascript container for a leaflet map and a number of featureGroups to be displayed on that map
 * GeoJson coordinates are always [lng, lat]
 * 
 * @version 3.4.0
 * @module viewerJS.geoMap
 * @requires jQuery
 */

var viewerJS = ( function( viewer ) {
    'use strict'; 
    
    //ifndef    
    if(viewer.GeoMap) {
    	return;
    }
        
    // default variables
    var _debug = false;
    
    var _defaults = {
            mapId : "geomap",
            layers: [],
            minZoom : 1,
            maxZoom : 19,
            initialView : {
                zoom: 5,
                center: [11.073397, 49.451993] //long, lat
            },
            tilesource: "mapbox",
            mapBoxToken : undefined,
            language: "de",
            fixed: false,
            heatmap: true,
            
    }
    
    viewer.GeoMap = function(config) {
        
        
        if (typeof L == "undefined") {
            throw "leaflet.js is not loaded";
        }
        this.config = $.extend( true, {}, _defaults, config );
        if(_debug) {
            console.log("load GeoMap with config ", this.config);
        }
        
        viewer.GeoMap.maps.set(this.config.mapId, this);

        this.layers = [];
        this.initialized = false;
        
        this.onMapRightclick = new rxjs.Subject();
        this.onMapClick = new rxjs.Subject();
        this.onMapMove = new rxjs.Subject();
        this.onActiveLayerChange = new rxjs.Subject();
        this.onInitialized = new Promise( (resolve, reject) => {
        	this.resolveInitialization = resolve;
        	this.rejectInitialization = reject;
        });

        this.config.layers.forEach(layer => this.addFeatureGroup(layer, layer.features));

		viewer.GeoMap.allMaps.push(this);
    }
    
    viewer.GeoMap.maps = new Map();

    viewer.GeoMap.prototype.init = function(view) {
       
       if(_debug)console.log("init geomap with", view);
       
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
            zoomControl: false,
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
        	this.initialized = true;
        });
        if(this.config.tilesource.toLowerCase() == "mapbox" && this.config.mapBox) {
            let url = 'https://api.mapbox.com/styles/v1/{1}/{2}/tiles/{z}/{x}/{y}?access_token={3}'
                .replace("{1}", this.config.mapBox.user)
                .replace("{2}", this.config.mapBox.styleId)
                .replace("{3}", this.config.mapBox.token);
            var mapbox = new L.TileLayer(url, {
                        tileSize: 512,
                        zoomOffset: -1,
                        minZoom: this.config.minZoom,
                		maxZoom: this.config.maxZoom,
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
            
		let allFeatures = this.config.layers.map(layer => layer.features).flat().filter(f => f != undefined);
        if(allFeatures && allFeatures.length > 0) {
        	this.setViewToFeatures(allFeatures, true)
        } else if(view){                                                    
            this.setView(view); 
        }
        
        return this.onInitialized;
        
    }
    
    viewer.GeoMap.prototype.addFeatureGroup = function(featureGroupConfig, features) {
		if(!featureGroupConfig.language) {
			featureGroupConfig.language = this.config.language;
		}
		let featureGroup = new viewer.GeoMap.featureGroup(this, featureGroupConfig);
		this.layers.push(featureGroup);
		if(this.initialized) {
			featureGroup.init(features, false);
		} else {
			this.onInitialized
			.then(() => featureGroup.init(features, false));
		}
		return featureGroup;
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
    viewer.GeoMap.prototype.setView = function(view, panning) {
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
                this.map.setView(center, view.zoom, {animate: panning});
            } else {                
                this.map.panTo(center, {animate: panning});
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
    
    viewer.GeoMap.prototype.setViewToFeatures = function(features, setViewToHighlighted, zoom) {
    	if(features && features.length > 0) {
            if(!zoom) {
                zoom = this.view ? this.zoom : this.config.initialView.zoom;
            }
            let highlightedFeatures = features.filter(f => f?.properties?.highlighted);
            //console.log(" highlightedFeatures", highlightedFeatures);
            if(setViewToHighlighted && highlightedFeatures.length > 0) {
            	let viewAroundFeatures = this.getViewAroundFeatures(highlightedFeatures, zoom, 0.5);
	            this.setView(viewAroundFeatures);
            } else {
	            let viewAroundFeatures = this.getViewAroundFeatures(features, zoom);
	            this.setView(viewAroundFeatures);
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

	viewer.GeoMap.prototype.setActiveLayers = function(groups) {
		this.layers.forEach(layer => {
			if(groups.includes(layer)) {
				layer.active = true;
				layer.showMarkers();
			} else {
				layer.active = false;
				layer.hideMarkers();
			}
		})
		this.onActiveLayerChange.next(this.getActiveLayers());
	}

	viewer.GeoMap.prototype.getActiveLayers = function() {
		return this.layers.filter(l => l.active !== false);
	}
    
    //static methods to get all loaded maps
    viewer.GeoMap.allMaps = [];
    

    return viewer;
    
} )( viewerJS || {}, jQuery );


