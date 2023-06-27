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
 * Javascript implementation of a collection of features within a map. Wraps a L.FeatureGroup layer
 * GeoJson coordinates are always [lng, lat]
 * 
 * @version 3.4.0
 * @module viewerJS.geoMap.featureGroup
 * @requires jQuery
 */

var viewerJS = ( function( viewer ) {
    'use strict'; 
        
    // default variables
    var _debug = false;
 
    var _defaults_featureGroup = {
			features: [],
            allowMovingFeatures: false,
            clusterMarkers : false,
            popover: undefined,
            emptyMarkerMessage: undefined,
            popoverOnHover: false,
            markerIcon : {
				html: '<div><span>\${count}</span></div>', 	
			},
            search: {
            	openSearchOnMarkerClick: true,
            	searchUrlTemplate : '/viewer/search/-/WKT_COORDS:"Intersects(POINT({lng} {lat})) distErrPct=0"/1/-/-/',
            	linkTarget : "_blank"
            },
            heatmap: {
            	enabled: false,
            	heatmapUrl: "/viewer/api/v1/index/spatial/heatmap/{solrField}",
            	featureUrl: "/viewer/api/v1/index/spatial/search/{solrField}",
            	filterQuery: "BOOL_WKT_COORDS:*",
		        labelField: "LABEL",
            },
            style: {
            	stroke: true,
            	color: '#3388ff',
            	highlightColor: '#d9534f',
            	weight: 3,
            	opactity: 1.0,
            	fill: true,
            	fillColor: undefined, //defaults to color
            	fillOpacity: 0.1,
            }
    }
    
        
    viewer.GeoMap.featureGroup = function(geoMap, config) {
 		this.geoMap = geoMap;
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
    
    viewer.GeoMap.featureGroup.prototype.init = function(features) {
		this.initFeatures(features);
		if(this.config.heatmap.enabled) {	        	    
			this.initHeatmap();
		}
	}
		
	viewer.GeoMap.featureGroup.prototype.initFeatures = function(features) {

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
                    icon: this.getMarkerIcon(geoJsonPoint.properties),
                    count: this.getCount(geoJsonPoint.properties)
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
    }
    
viewer.GeoMap.featureGroup.prototype.initHeatmap = function() {

        	let heatmapUrl = this.config.heatmap.heatmapUrl;
        	let featureUrl = this.config.heatmap.featureUrl;
        	
        	this.heatmap = L.solrHeatmap(heatmapUrl, featureUrl, this, {
        	    field: "WKT_COORDS",
        	    type: "clusters",
        	    filterQuery: this.config.heatmap.filterQuery,
        	    labelField: this.config.heatmap.labelField,
        	    queryAdapter: "goobiViewer"    
        	});
        	this.heatmap.addTo(this.geoMap.map);        
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
                return this.getClusterIcon(this.getClusterCount(cluster));
            }.bind(this)
        });
        if(!this.geoMap.config.fixed) {            
            cluster.on('clustermouseover', function (a) {
                this.removePolygon();
                this.shownLayer = a.layer;
                this.polygon = L.polygon(a.layer.getConvexHull());
                this.layer.addLayer(this.polygon);
            }.bind(this));
            cluster.on('clustermouseout', () => this.removePolygon());
            this.geoMap.map.on('zoomend', () => this.removePolygon());
        }
        return cluster;
    }
    
    viewer.GeoMap.featureGroup.prototype.getClusterCount = function(cluster) { 
	  	let count = cluster.getAllChildMarkers().map(child => this.getCount(child.feature.properties)).reduce((a, b) => a + b, 0)
	  	return count;
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
		
		if(this.config.markerIcon?.type == 'DivIcon') {
			let options =  $.extend(true, {}, this.config.markerIcon);
			if(!options) {
				throw "marker icon of type 'divIcon' needs an 'options' properties containing the constructor options to pass to the icon";
			} else {
				options.html = options.html.replaceAll("${count}", num);
				options.iconSize = new L.Point(40, 40);
				return new L.DivIcon(options);
			}
		} else {			
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
		
    }
    
    viewer.GeoMap.featureGroup.prototype.getMarkerIcon = function(properties) {
    	
    	let count = this.getCount(properties); 
    	let highlighted = properties?.highlighted;
        if(this.config.markerIcon && !jQuery.isEmptyObject(this.config.markerIcon)) {
        	if(this.config.markerIcon.useDefault) { 
        		if(this.config.markerIcon.highlightIcon && highlighted) {
        		let icon = new L.Icon.Default({
        			imagePath : this.geoMap.config.iconPath + "/"
        		});
        		icon.options.iconUrl = this.config.markerIcon.highlightIcon;
				icon.options.iconRetinaUrl = this.config.markerIcon.highlightIcon;
        		//console.log("use hightlight icon ", icon);
        		return icon; 
        		} else {
        			return new L.Icon.Default();
        		}
        	} else if(this.config.markerIcon.type) {
				if(this.config.markerIcon.type == 'DivIcon') {
					let options =  $.extend(true, {}, this.config.markerIcon);
					options.html = options.html.replace("${count}", "1");
					options.iconSize = new L.Point(40, 40);
					let icon = count > 1 ? this.getClusterIcon(count) : new L.DivIcon(options);
					return icon;
				} else {
					let icon = count > 1 ? this.getClusterIcon(count) : L.ExtraMarkers.icon(this.config.markerIcon);
		        	icon.options.name = "";	//remove name property to avoid it being displayed on the map
		            if(this.config.markerIcon.shadow === false) {                
		                icon.options.shadowSize = [0,0];
		            }
		            if(highlighted) {
		            	icon.options.markerColor = this.config.markerIcon.highlightColor;
		            }
		            return icon;
				}
        	} else {
	            let icon = count > 1 ? this.getClusterIcon(count) : L.ExtraMarkers.icon(this.config.markerIcon);
	        	icon.options.name = "";	//remove name property to avoid it being displayed on the map
	            if(this.config.markerIcon.shadow === false) {                
	                icon.options.shadowSize = [0,0];
	            }
	            if(highlighted) {
	            	icon.options.markerColor = this.config.markerIcon.highlightColor;
	            }
	            return icon;
            }
        } else {
            return new L.Icon.Default();
        }
    }

	viewer.GeoMap.featureGroup.prototype.getCount = function(properties) {
		if(properties.entities) {
			return properties.entities.filter(e => e.visible !== false).length;
		} else if(properties.count){
			return properties.count;
		} else {
			return 1;
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
            $popover.find("[data-metadata='title']").html(title);
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
    
    viewer.GeoMap.featureGroup.prototype.removeAllMarkers = function() {
    	this.markers.forEach(m => m.remove());
    	this.initFeatures();
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
    
    viewer.GeoMap.featureGroup.prototype.showMarkers = function(entityFilter) {
		_getAllEntities(this).filter(entity => entity).forEach(entity => {
			entity.visible = _isVisible(entity, entityFilter);
		});
		this.hideMarkers();
		this.markers.filter(m => this.getCount(m.feature.properties))
		.forEach(m => {
			m.setIcon(this.getMarkerIcon(m.feature.properties));
			if(this.cluster) {
				this.cluster.addLayer(m);
			} else {
				this.layer.addLayer(m);
			}
		})
	}
	
	viewer.GeoMap.featureGroup.prototype.hideMarkers = function() {
		this.layer.clearLayers();
		if(this.cluster) {		
			this.cluster.clearLayers();
			this.layer.addLayer(this.cluster);
		}
	}

	function _getAllEntities(featureGroup) {
		let entities = featureGroup.markers.flatMap(m => m.feature.properties.entities);
		return entities ? entities : [];
	}
    
    function _isVisible(entity, filter) {
		if(typeof filter === 'function') {
			return filter(entity);
		} else if(filter === undefined) {
			return true;
		} else {
			return filter ? true : false;
		}
	}
    
    return viewer;
    
} )( viewerJS || {}, jQuery );


