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
 * heatmapheat
 * @module viewerJS.geoMapFacet
 * @requires jQuery
 */

var viewerJS = ( function ( viewer ) {
	'use strict';

	// default variables
	var _debug = false;

	var _defaults = {
		areaString: "",
		map: {
			mapId: "geomap",
			language: "en",
			iconPath: "/resources/images/map",
			hitsLayer: {
				allowMovingFeatures: false,
				popoverOnHover: true,
				popover: $( "<div><p data-metadata='title'></p></div>" ),
				clusterMarkers: true,
				style: {
					fillOpacity: 0.02
				},
				markerIcon: {
					icon: "fa-number",
					svg: true,
					prefix: "fa",
					iconRotate: 0
				}
			},
			areaLayer: {
				style : {
					fillColor : "#d9534f",
					color : "#d9534f",
					fillOpacity : 0.3,
				}
			}
		},
		heatmap: {
			enabled: true,
			heatmapUrl: "/viewer/api/v1/index/spatial/heatmap/{solrField}",
			featureUrl: "/viewer/api/v1/index/spatial/search/{solrField}",
			mainQuery: "BOOL_WKT_COORDS:*",
			facetQuery: "",
			labelField: "LABEL",
		}, 
		search: {
			$loader: $( "[data-loader='geoFacet']" ),
			$searchButton: $( "[data-geofacet='execute']" )
		},
		buttons: {
			$searchWithFacets: $( "[data-geofacet='trigger-execute']" ),
			$resetFacets: $( "[data-geofacet='reset']" ),
			$cancelEditMode: $( "[data-geofacet='cancel']" ),
			$facetInput: $( "[data-geofacet='feature']" ),
			$toggleMarkers: $( "[data-geofacet='toggleMarkers']" ),
			$toggleMarkersEditMode: $( "[data-geofacet='toggleMarkersFullscreen']" ),
			$openEditMode: $("#expandFacetMap"),
		},
		editMode: {
			$editModeWrapper: $('#widgetGeoFacettingOverlay'),
			$editModeMap: $("#geoFacettingOverlayMap"),
			enableAddressSearch : true,
			addressSearchPlaceholder : "Address"
		}
	}

	viewer.GeoMapFacet = function ( config ) {
		this.config = $.extend( true, {}, _defaults, config );
		if ( _debug ) console.log( "Initialize Facet-Geomap with config", config );
		this.geoMap = new viewerJS.GeoMap( this.config.map );
	}


	viewer.GeoMapFacet.prototype.init = function (features,  view ) {
		this.area = this.getArea( this.config.areaString );
		this.features = this.config.heatmap.enabled ? {} : features;
		this.geoMap.init(view);

		this.drawLayer = this.initDrawLayer();
		this.hitsLayer = this.initHitsLayer(this.features);
		if(this.config.heatmap.enabled) {
			this.heatmap = this.initHeatmap();
		}


		this.config.buttons.$toggleMarkers.on("click", () => {
			this.hitsLayer?.setVisible(!this.hitsLayer.isVisible());
			this.heatmap?.setVisible(!this.heatmap.isVisible());
		})

		$(this.config.buttons.$openEditMode).on("click", e => this.openEditMode());

		// close map overlay with escape
		$(document).on('keyup', e => {
			var $overlayMap = $( this.config.editMode.$editModeMap );
			if ( $overlayMap.length && $overlayMap.is(":visible")) {
				if (e.key == "Escape") {
					$(this.config.buttons.$cancelEditMode).click();
				}
			}
		});
	}

	viewer.GeoMapFacet.prototype.initHitsLayer = function (features) {
		//console.log("init hits layer ", this.config.map.hitsLayer);
		this.config.map.hitsLayer.language = this.config.map.language;
		let hitsLayer = new viewerJS.GeoMap.featureGroup(this.geoMap, this.config.map.hitsLayer)
		hitsLayer.init(features, false);
		hitsLayer.onFeatureClick.subscribe(f => {
			console.log("clicked on ", f);
			if(f.properties && f.properties.link) {
				$(this.config.search.loader).show();
				window.location.assign(f.properties.link);
			}
		})

		return hitsLayer;
	}

	viewer.GeoMapFacet.prototype.initHeatmap = function () {
		let heatmapQuery = this.config.heatmap.mainQuery;
		let heatmapFacetQuery = this.config.heatmap.facetQuery;
		
		let heatmap = L.solrHeatmap(this.config.heatmap.heatmapUrl, this.config.heatmap.featureUrl, this.hitsLayer, {
			field: "WKT_COORDS",
			type: "clusters",
			filterQuery: heatmapQuery,
			facetQuery: heatmapFacetQuery,
			labelField: this.config.heatmap.labelField,
			queryAdapter: "goobiViewer"    
		});
		heatmap.addTo(this.geoMap.map);
		return heatmap;
	}

	viewer.GeoMapFacet.prototype.initDrawLayer = function() {
		this.config.map.areaLayer.language = this.config.map.language;
		let drawLayer = new viewerJS.GeoMap.featureGroup(this.geoMap, this.config.map.areaLayer);
		if(this.area) {	                    	    
			switch(this.area.type) {
				case "polygon":
					this.shape = drawLayer.drawPolygon(this.area.vertices, true);
					break;
				case "circle":
					this.shape = drawLayer.drawCircle(this.area.center, this.area.radius, true);
					break;
				case "rectangle":
					this.shape = drawLayer.drawRectangle([this.area.vertices[0], this.area.vertices[2]], true);
					break;
			}
			this.config.buttons.$resetFacets.addClass("active");
			this.config.buttons.$resetFacets.on("click", () => {
				this.config.buttons.$facetInput.val("");
				this.config.buttons.$searchWithFacets.click();
			});
		}
		return drawLayer;
	}

	viewer.GeoMapFacet.prototype.openEditMode = function() {
		viewerJS.overlay.open(this.config.editMode.$editModeWrapper, false, true, $node => {
			$("body").append($node);
			$node.hide();
		})
		.then(overlay => {
			overlay.node.show();
			this.config.buttons.$searchWithFacets.off().on("click", () => {
				//console.log("trigger search");
				this.config.search.$searchButton.click();
				this.config.search.$loader.show();
			});
			this.config.buttons.$cancelEditMode.off().on("click", () => {
				overlay.close();
			})
			if(this.mapTag) {
				this.mapTag.forEach(tag => tag.unmount(true));
			}
			console.log("area layer", this.config.map.areaLayer)
			this.mapTag = riot.mount(this.config.editMode.$editModeMap, "geomapsearch", {
				inactive: false,
				area : this.area,
				features : this.features,
				toggleFeatures: this.config.buttons.$toggleMarkersEditMode.get(0),
				search_enabled: this.config.editMode.enableAddressSearch,
				search_placeholder: this.config.editMode.addressSearchPlaceholder,
				hitsLayer: this.config.map.hitsLayer,
				areaLayer: this.config.map.areaLayer,
				heatmap: this.config.heatmap,
				onFeatureSelect: area => {
					//console.log("Set facetting area", area);
					sessionStorage.setItem("geoFacet", JSON.stringify(area));
					this.config.buttons.$facetInput.val(area ? JSON.stringify(area) : "");
				}
			});
		}); 
	} 

	viewer.GeoMapFacet.prototype.getArea = function(areaString) {
		let area = areaString.length > 0 ? JSON.parse(areaString) : undefined;
		if(area) {
			let storedAreaString = sessionStorage.getItem("geoFacet");
			if(storedAreaString) {
				let storedArea = JSON.parse(storedAreaString);
				for(let v=0; v<area.vertices.length; v++) {
					for(let c=0; c<area.vertices[v].length; c++) {
						if(area.vertices[v][c] != storedArea.vertices[v][c]) {
							return area;
						}
					}
				}
				return storedArea;
			}
		}
		return area;
	}


	return viewer;

} )( viewerJS || {}, jQuery );