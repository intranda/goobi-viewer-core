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
 * Module to render GeoJson data on a leaflet map
 * The GeoJson may be loaded multiple times using the leaflet.load(url) method. 
 * The url should return a json object containing a list property items or resources consisting of OpenAnnotation/WebAnnotation objects which 
 * contain the GeoJson in their resource/body property
 * 
 * If the GeoJson objects contain an additional object 'view' with properties 'zoom' and 'center', the view property of the first
 * loaded GeoJson object is used to zoom/center the map.
 * 
 * @version 3.2.0
 * @module viewerJS.normdata
 * @requires jQuery
 * @requires leaflet
 * 
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    const _debug = false;

    /**
     * Create a new Leaflet object using new viewerJS.Leaflet(config)
     * @params config   object which should contain the following properties:
     * <ul>
     * <li> msg.propertiesLink - message to display on popover when hovering a location marker. If this doesn't exist, no popover is rendered</li>
     * <li> widgetSelector - dom selector for the map element. If this is given and no annotations are loaded, the element found under this selector is hidden on the first call to #load(url)</li>
     * <li> mapId - The id of the container element for the leaflet map. must be given to initialize the map. </li>
     * </ul>
     */
    viewer.Leaflet = function(config) {
        if ( _debug ) {
            console.log( '##############################' );
            console.log( 'viewer.leaflet.init' );
            console.log( '##############################' );
            console.log( 'viewer.leaflet.init: config = ', config );
        }
        
        this.config = config;
        this.map = null;
        this.markers = [];
            
    };
    
    /**
     * Load GeoJson data from the given url. The url must return a json object with a list property items or resources containing objects which
     * in turn contain the GeoJson in their body or resource property.
     * 
     * @param url   The url to load the GeoJson data from. Typically an IIIF AnnotationList or a WebAnnotation AnnotationPge
     */
    viewer.Leaflet.prototype.load = function(url) {
        fetch(url)
        .then( response => response.json() )
        .then( annoPage => _getItems(annoPage))
        .then( locations => {
            locations = locations.filter( loc => _getBody(loc).type === "Feature");
            if(_debug) console.log("Loaded GeoJson locations ", locations);
            if(locations.length > 0) {
                if(!this.map) {           
                    if(_debug) console.log("initialize map");
                    this.map = _initMap(this.config);
                    if(_debug) console.log("set view to ",  _getBody(locations[0]).view);
                    _setView(this.map, _getBody(locations[0]).view);
                }
                this.markers = this.markers.concat(_addMarkers(this.map, locations, this.config.msg ? this.config.msg.propertiesLink : undefined));
            } 
            if(this.config.widgetSelector) {                
                if(!this.map) {
                    if(_debug) console.log("hide map");
                    $(this.config.widgetSelector).hide();
                } else if($(this.config.widgetSelector).is(":hidden")) {
                    if(_debug) console.log("show map");
                    $(this.config.widgetSelector).show();
                    this.map.invalidateSize();
                }
            }
        })
    }
    
    function _getBody(anno) {
        if(anno.body) {
            return anno.body;
        } else if(anno.resource) {
            return anno.resource;
        } else {
            return {};
        }
    }
    
    function _getItems(annoPage) {
        if(annoPage.items) {            
            return annoPage.items
        } else if(annoPage.resources) {
            return annoPage.resources;
        } else {
            return [];
        }
    }
    
    function _setView(map, view) {
        map.setView(new L.LatLng(view.center[0], view.center[1]), view.zoom);
    }
    
    function _initMap(config) {
        let map = new L.Map(config.mapId);
        var osm = new L.TileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
          minZoom: 0,
          maxZoom: 20,
//          attribution: 'Map data &copy; <a href="https://openstreetmap.org">OpenStreetMap</a> contributors'
        });
     
        // define view
        map.setView(new L.LatLng(49.451993, 11.073397), 5);
        map.addLayer(osm);
        return map;
    }
    
    function _addMarkers(map, annotations, popupText) {
        let markers = new Map();
        annotations.forEach( anno => {
            let geoJson = _getBody(anno);
            var marker = L.geoJSON(geoJson).addTo(map);
            if(popupText) {                
                marker.bindPopup(popupText);
                marker.on("mouseover", function() {
                    this.openPopup();
                })
                marker.on("mouseout", function() {
                    this.closePopup();
                })
            }
            markers.set(anno, marker);

        })
        return markers;
    }
    
    

    
    return viewer;
    
} )( viewerJS || {}, jQuery );
