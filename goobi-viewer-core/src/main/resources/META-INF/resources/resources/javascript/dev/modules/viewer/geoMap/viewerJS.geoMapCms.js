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
 * Display a map created in the administration backend/cms-section
 * GeoJson coordinates are always [lng, lat]
 * 
 * @version 3.4.0
 * @module viewerJS.geoMapCms
 * @requires jQuery
 */
 
 var viewerJS = ( function( viewer ) {
    'use strict'; 
        
    // default variables
    var _debug = false;
    
    var _defaults = {
    		mapType: "MANUAL", // or "SOLR_QUERY",
    		openSearchOnMarkerClick: true,
    		documentIdToHighlight: undefined,
            map: {
	            mapId : "geomap",
	            language: "en",
	            iconPath: "/resources/images/map",
	            layer: {
	            	allowMovingFeatures: false,
	           		popover: undefined,
	           		popoverOnHover: false,
	           		clusterMarkers: true,
	           		markerIcon: undefined,
	            }
            },
            search: {
            	openSearchOnMarkerClick: true,
            	searchUrlTemplate : '/viewer/search/-/WKT_COORDS:"Intersects(POINT({lng} {lat})) distErrPct=0"/1/-/-/',
            	linkTarget : "_blank"
            },
            heatmap: {
            	showSearchResultsHeatmap: false,
            	heatmapUrl: "/viewer/api/v1/index/spatial/heatmap/{solrField}",
            	featureUrl: "/viewer/api/v1/index/spatial/search/{solrField}",
            	filterQuery: "BOOL_WKT_COORDS:*",
		        labelField: "LABEL",
            }
    }
    
    viewer.GeoMapCms = function(config) {
 		this.config = $.extend( true, {}, _defaults, config );
 		if(_debug)console.log("Initialize CMS-Geomap with config", config);
		this.geoMap = new viewerJS.GeoMap(this.config.map);
   }
   
   viewer.GeoMapCms.prototype.init = function(view, features) {
	    this.geoMap.init(view);
	    this.config.map.layer.language = this.config.map.language;
    	this.layer = new viewerJS.GeoMap.featureGroup(this.geoMap, this.config.map.layer)

		//when clicking on features with an associated link, open that link
    	this.layer.onFeatureClick.subscribe(feature => {
   	       if(feature.properties && feature.properties.link && !feature.properties.highlighted) {
   	           window.location.assign(feature.properties.link);
   	       }
   	    });
   	    
   	    //link to search url on feature click
    	if(this.config.search.openSearchOnMarkerClick) {
			let searchUrlTemplate = this.config.search.searchUrlTemplate;
            this.layer.onFeatureClick.subscribe( (feature) => {
				// viewerJS.notifications.confirm("Do you want to show search results for this location?")
				// .then(() => {
					$(this.config.search.loader).show();
					let queryUrl = searchUrlTemplate.replace("{lng}", feature.geometry.coordinates[0]);
					queryUrl = queryUrl.replace("{lat}", feature.geometry.coordinates[1]);
					window.open(queryUrl, this.config.search.linkTarget);
				// })
            });
        }
    	
    	//Hightlight the marker belinging to a given SOLR document
    	let highlightDocumentId = this.config.documentIdToHighlight;
    	if(highlightDocumentId) {
    	    features.filter(f => f.properties.documentId == highlightDocumentId).forEach(f => f.properties.highlighted = true);
    	}
    	
    	//display search results as heatmap
    	if(this.config.heatmap.showSearchResultsHeatmap) {	        	    
        	let heatmapUrl = this.config.heatmap.heatmapUrl;
        	let featureUrl = this.config.heatmap.featureUrl;
        	
        	this.heatmap = L.solrHeatmap(heatmapUrl, featureUrl, this.layer, {
        	    field: "WKT_COORDS",
        	    type: "clusters",
        	    filterQuery: this.config.heatmap.filterQuery,
        	    labelField: this.config.heatmap.labelField,
        	    queryAdapter: "goobiViewer"    
        	});
        	this.heatmap.addTo(this.geoMap.map);
    	}     	

		//initialize layer
     	this.layer.init(features, features?.length);
	}
	
	return viewer;

} )( viewerJS || {}, jQuery );