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
 * Displays a map in which one can draw shapes and start a search for objects within that area 
 * GeoJson coordinates are always [lng, lat]
 * 
 * @version 3.4.0
 * @module viewerJS.geoMapSearch
 * @requires jQuery
 */

var viewerJS = ( function ( viewer ) {
	'use strict';

	// default variables
	var _debug = false;

	var _defaults = {
		submitSearchSelector: '',
		inputSearchSelector: '',
		opts: {
			area : '',
			search_enabled: true,
			search_placeholder: "Address",
			reset_button: $("[data-feature='reset']"),
			msg: {
				action__toggle_map_markers : "toggle markers"
			},
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
			heatmap: {
				enabled: true,
				heatmapUrl: "/viewer/api/v1/index/spatial/heatmap/{solrField}",
				featureUrl: "/viewer/api/v1/index/spatial/search/{solrField}",
				mainQuery: "BOOL_WKT_COORDS:*",
				facetQuery: "",
				labelField: "LABEL",
			},
			areaLayer: {
				style : {
					fillColor : "#d9534f",
					color : "#d9534f",
					fillOpacity : 0.3,
				}
			}
		}
	}

	viewer.GeoMapSearch = function ( config ) {
		this.config = $.extend( true, {}, _defaults, config );
		if ( _debug ) console.log( "Initialize Search-Geomap with config", config );
	}


	viewer.GeoMapSearch.prototype.init = function ( view ) {
		this.config.opts.onFeatureSelect = (feature) => this.setSearchString(JSON.stringify(feature));
		riot.mount("#geoMapSearch", "geomapsearch", this.config.opts);
	}

	viewer.GeoMapSearch.prototype.setSearchString = function(feature) {
		//console.log("set search feature ", feature);
		sessionStorage.setItem("geoFacet", feature);
		$(this.config.inputSearchSelector).val(feature);
		if(!feature) {
			$(this.config.submitSearchSelector).attr("disabled", "disabled");
		} else {
			$(this.config.submitSearchSelector).removeAttr("disabled");
		}
	}


    // console.log("viewer ", viewer);
	return viewer;

} )( viewerJS || {}, jQuery );