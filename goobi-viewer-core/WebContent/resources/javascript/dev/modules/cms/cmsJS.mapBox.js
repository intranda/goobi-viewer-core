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
 * Module to initialize the mapbox view.
 * 
 * @version 3.2.0
 * @module cmsJS.geoLocations
 * @requires jQuery
 */
var cmsJS = ( function( cms ) {
    'use strict';
    
    // variables
    var _debug = false;
    var _map = {};
    var _mapEnlarged = {};
    var _features = [];
    var _centerCoords = [];
    var _defaults = {
        locations: '',
        mapboxAccessToken: 'pk.eyJ1IjoibGlydW1nYnYiLCJhIjoiY2lobjRzamkyMDBnM3U5bTR4cHp0NDdyeCJ9.AjNCRBlBb57j-dziFxf58A',
        mapBoxContainerSelector: 'widgetGeoLocationsMap',
        mapBoxContainerEnlargedSelector: 'widgetGeoLocationsMapEnlarged',
        mapBoxStyle: 'mapbox://styles/lirumgbv/cii024wxn009aiolzgy2zlycj',
        msg: {
            propertiesLink: ''
        }
    };
    
    cms.geoLocations = {
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'cms.geoLocations.init' );
                console.log( '##############################' );
                console.log( 'cms.geoLocations.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );
            
            if ( $( '#widgetGeoLocationsMap' ).length > 0 ) {
                mapboxgl.accessToken = _defaults.mapboxAccessToken;
                
                // get map data and infos
                _centerCoords = _getCenterCoords( _defaults.locations );
                _features = _getMapFeatures( _defaults.locations );
                
                // create map
                _map = new mapboxgl.Map( {
                    container: _defaults.mapBoxContainerSelector,
                    style: _defaults.mapBoxStyle,
                    center: _centerCoords,
                    zoom: 12.5,
                    interactive: true
                } );
                
                // build markers
                _map.on( 'style.load', function() {
                    _map.addSource( "markers", {
                        "type": "geojson",
                        "data": {
                            "type": "FeatureCollection",
                            "features": _features
                        }
                    } );
                    
                    _map.addLayer( {
                        "id": "markers",
                        "type": "symbol",
                        "source": "markers",
                        "layout": {
                            "icon-image": "pin",
                            "icon-allow-overlap": true
                        }
                    } );
                    
                    _map.addControl( new mapboxgl.FullscreenControl() );
                    _map.addControl( new mapboxgl.NavigationControl() );
                } );
                
                // add popups
                _map.on( 'click', 'markers', function( e ) {
                    new mapboxgl.Popup().setLngLat( e.features[ 0 ].geometry.coordinates ).setHTML( e.features[ 0 ].properties.infos ).addTo( _map );
                } );
            }
            
        }
    };
    
    /**
     * Method which returns an object of map features.
     * 
     * @method _getMapFeatures
     * @param {String} infos A JSON-String which contains the feature infos.
     * @returns {Array} An array of features.
     */
    function _getMapFeatures( infos ) {
        if ( _debug ) {
            console.log( '---------- _getMapFeatures() ----------' );
        }
        
        var features = [];
        var infos = JSON.parse( infos );
        
        $.each( infos.locations, function( key, location ) {
            var feature = {
                'type': 'Feature',
                'geometry': {
                    'type': 'Point',
                    'coordinates': [ location.longitude, location.latitude ]
                },
                'properties': {
                    'infos': location.infos + '<br /><a href="' + location.link + '">' + _defaults.msg.propertiesLink + '</a>'
                }
            }

            features.push( feature );
        } );
        
        return features;
    }
    
    /**
     * Method which returns an array of coordinates for centering the map.
     * 
     * @method _getCenterCoords
     * @param {String} infos A JSON-String which contains the feature infos.
     * @returns {Array} An array of coords.
     */
    function _getCenterCoords( infos ) {
        if ( _debug ) {
            console.log( '---------- _getCenterCoords() ----------' );
        }
        
        var coords = [];
        var infos = JSON.parse( infos );
        
        if ( infos.centerLocation.longitude != '' || infos.centerLocation.latitude != '' ) {
            coords.push( infos.centerLocation.longitude );
            coords.push( infos.centerLocation.latitude );
        }
        else {
            coords.push( infos.locations[ 0 ].longitude );
            coords.push( infos.locations[ 0 ].latitude );
        }
        
        return coords;
    }
    
    return cms;
    
} )( cmsJS || {}, jQuery );
