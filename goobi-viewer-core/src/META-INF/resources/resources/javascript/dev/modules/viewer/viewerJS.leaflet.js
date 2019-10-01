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
 * Module to render popovers including normdata.
 * 
 * @version 3.2.0
 * @module viewerJS.normdata
 * @requires jQuery
 * @requires Bootstrap
 * 
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    const _debug = false;

    
    viewer.Leaflet = function(config) {
        if ( _debug ) {
            console.log( '##############################' );
            console.log( 'viewer.leaflet.init' );
            console.log( '##############################' );
            console.log( 'viewer.leaflet.init: config = ', config );
        }
        
        this.config = config;
        this.locations = [];
        
        fetch(config.pageLocations)
        .then( response => response.json() )
        .then( annoPage => {if(annoPage && annoPage.items){this.locations = this.locations.concat(annoPage.items)}})
        .then( () => fetch(config.workLocations) )
        .then( response => response.json() )
        .then( annoPage => {if(annoPage && annoPage.items){this.locations = this.locations.concat(annoPage.items)}})
        .then( () => {
            console.log("locations ", this.locations);
            if(this.locations.length > 0) {                
                this.map = _initMap(this.config);
                this.markers = _addMarkers(this.map, this.locations, this.config.msg.propertiesLink);
                _setView(this.map, this.locations[0].body.view);
            } else {
                $(config.widgetSelector).hide();
            }
        });
            
    };
    
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
            let geoJson = anno.body;
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
