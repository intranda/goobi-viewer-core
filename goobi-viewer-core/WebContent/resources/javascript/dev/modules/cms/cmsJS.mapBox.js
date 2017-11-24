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
    var _debug = true;
    var _defaults = {
        mapboxAccessToken: 'pk.eyJ1IjoibGlydW1nYnYiLCJhIjoiY2lobjRzamkyMDBnM3U5bTR4cHp0NDdyeCJ9.AjNCRBlBb57j-dziFxf58A',
        mapBoxContainerSelector: 'widgetGeoLocationsMap',
        mapBoxStyle: 'mapbox://styles/lirumgbv/cii024wxn009aiolzgy2zlycj',
        goettingenCenterCoords: [ 9.94100, 51.53950 ],
        goettingenZoomFactorOneCollectionForSidebar: 16,
        goettingenOverviewZoomFactorForSidebar: 12.5,
        goettingenOverviewZoomFactorForOneCollection: 16,
        goettingenOverviewZoomFactorForSomeCollections: 13,
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
                
                var map = new mapboxgl.Map( {
                    container: _defaults.mapBoxContainerSelector,
                    style: _defaults.mapBoxStyle,
                    center: _defaults.goettingenCenterCoords,
                    zoom: _defaults.goettingenOverviewZoomFactorForSidebar,
                    interactive: true
                } );
                
                // build markers
                map.on( 'style.load', function() {
                    map.addSource( "markers", {
                        "type": "geojson",
                        "data": {
                            "type": "Feature",
                            "geometry": {
                                "type": "Point",
                                "coordinates": [ 9.94100, 51.53950 ]
                            },
                            "properties": {
                                "description": '<strong>Sammlung der Gipsabgüsse antiker Skulpturen</strong>',
                                "adresse": 'Nikolausberger Weg 15',
                                "isil": 'slg_1022'
                            }
                        }
                    } );
                    
                    map.addLayer( {
                        "id": "markers",
                        "type": "symbol",
                        "source": "markers",
                        "layout": {
                            "icon-image": "pin",
                            "icon-allow-overlap": true
                        }
                    } );
                } );
            }
            
            // cmsJS.geoLocations.checkMapGeneration( infos );
            
            // hover static map
            // $( '#widgetGeoLocationsMap' ).on( {
            // mouseenter: function() {
            // $( this ).prepend( '<div id="sidebarMapEnlargOverlay"
            // class="widget-geo-locations__overlay"></div>' );
            // $( '#sidebarMapEnlargOverlay' ).fadeIn( 'fast' );
            // },
            // mouseleave: function() {
            // $( '#sidebarMapEnlargOverlay' ).fadeOut( 'fast', function() {
            // $( '#sidebarMapEnlargOverlay' ).remove();
            // } );
            // }
            // } );
            
            // click on static map --> open large map in popup
            // $( document ).on( 'mouseup', '#widgetGeoLocationsMap
            // #sidebarMapEnlargOverlay', function() {
            // append overlay to document
            // $( 'body' ).append( '<div id="mapPopupOverlay"><div
            // id="mapPopupOverlayCloseButton" title="Karte schließen"></div><div
            // id="mapPopupContainer"></div></div>' );
            // $( '#mapPopupOverlay' ).css( {
            // width: $( window ).width(),
            // height: $( window ).height()
            // } );
            // $( '#mapPopupContainer' ).css( {
            // width: ( $( window ).width() * 0.8 ),
            // height: ( $( window ).height() * 0.8 ),
            // top: ( $( window ).height() * 0.1 ),
            // left: ( $( window ).width() * 0.1 )
            // } );
            // $( '#mapPopupOverlay' ).fadeIn( 300 );
            // var zoomFactor =
            // _defaults.goettingenOverviewZoomFactorForOneCollection;
            // var centerCoords = _defaults.collections[ 0 ].coords;
            // if ( _defaults.collections.length > 1 ) {
            // zoomFactor = _defaults.goettingenOverviewZoomFactorForSomeCollections;
            // centerCoords = _defaults.goettingenCenterCoords;
            // }
            //                    
            // cms.geoLocations.generateMapForCollections( 'mapPopupContainer',
            // centerCoords, _defaults.collections, zoomFactor, true );
            // } );
            
            // close map-Popup
            // on "click"
            // $( document ).on( 'click', '#mapPopupOverlay
            // #mapPopupOverlayCloseButton', function() {
            // $( '#mapPopupOverlay' ).fadeOut( 300, function() {
            // $( '#mapPopupOverlay' ).remove();
            // } );
            // } );
            // on ESC
            // $( document ).on( 'keyup', function( e ) {
            // if ( e.keyCode == 27 ) {
            // $( '#mapPopupOverlay #mapPopupOverlayCloseButton' ).trigger( 'click' );
            // }
            // } );
            
        },
        // check if the static map in sidebar should be loaded (either /sammlung or
        // /sammlungen)
        // checkMapGeneration: function( sammlungsinfos ) {
        // var url = window.location.href;
        //            
        // if ( url.indexOf( '/sammlung/' ) > 0 ) {
        // // labeling
        // $( '#sammlung_sammlungenMapLabel' ).html( 'Sammlung' );
        //                
        // // extract isil from url
        // var isil = url.substr( url.indexOf( '/sammlung/' ) + 10 );
        // isil = isil.replace( '/', '' );
        // var coords = new Array();
        // var name = '';
        // var adresse = '';
        //                
        // $.each( sammlungsinfos, function( key, value ) {
        // if ( value.isil == isil ) {
        // name = value.name;
        // coords = Array( 1 * value.lon, 1 * value.lat );
        // adresse = value.adresse;
        // }
        // } );
        //                
        // _defaults.collections = new Array();
        // var collection = {
        // 'coords': coords,
        // 'name': name,
        // 'isil': isil,
        // 'adresse': adresse
        // }
        // _defaults.collections.push( collection );
        //                
        // cms.geoLocations.generateMapForCollections( 'widgetGeoLocationsMap', coords,
        // _defaults.collections, _defaults.goettingenZoomFactorOneCollectionForSidebar,
        // false );
        // }
        // else if ( url.indexOf( '/sammlungen/gruppe' ) > 0 ) {
        // // labeling
        // $( '#sammlung_sammlungenMapLabel' ).html( 'Sammlungen' );
        //                
        // // extract cluster from url
        // var clusterID = url.substr( url.indexOf( '/sammlungen/gruppe' ) + 18 );
        // clusterID = clusterID.replace( '/', '' );
        //                
        // // center of göttingen town
        // var centercoords = _defaults.goettingenCenterCoords;
        // _defaults.collections = new Array();
        // var name = '';
        // var adresse = '';
        //                
        // $.each( sammlungsinfos, function( key, value ) {
        // if ( value.clusterID == clusterID ) {
        // name = value.name;
        // coords = Array( 1 * value.lon, 1 * value.lat );
        // adresse = value.adresse;
        // var isil = value.isil;
        // var collection = {
        // 'coords': coords,
        // 'name': name,
        // 'isil': isil,
        // 'adresse': adresse
        // }
        // _defaults.collections.push( collection );
        // }
        // } );
        //                
        // cms.geoLocations.generateMapForCollections( 'widgetGeoLocationsMap',
        // centercoords, _defaults.collections,
        // _defaults.goettingenOverviewZoomFactorForSidebar, false );
        // }
        // else if ( url.indexOf( '/sammlungen/' ) > 0 || $( '#widgetGeoLocationsMap'
        // ).length == 1 ) {
        // // labeling
        // $( '#sammlung_sammlungenMapLabel' ).html( 'Sammlungen' );
        //                
        // // center of göttingen town
        // var centercoords = _defaults.goettingenCenterCoords;
        // _defaults.collections = new Array();
        // var name = '';
        // var adresse = '';
        //                
        // $.each( sammlungsinfos, function( key, value ) {
        // name = value.name;
        // coords = Array( 1 * value.lon, 1 * value.lat );
        // adresse = value.adresse;
        // var isil = value.isil;
        // var collection = {
        // 'coords': coords,
        // 'name': name,
        // 'isil': isil,
        // 'adresse': adresse
        // }
        // _defaults.collections.push( collection );
        // } );
        //                
        // cms.geoLocations.generateMapForCollections( 'widgetGeoLocationsMap',
        // centercoords, _defaults.collections,
        // _defaults.goettingenOverviewZoomFactorForSidebar, false );
        // }
        // else if ( url.indexOf( '/objekt/' ) > 0 ) {
        // // center of göttingen town
        // var centercoords = _defaults.goettingenCenterCoords;
        // _defaults.collections = new Array();
        // var name = '';
        // var adresse = '';
        //                
        // $.each( sammlungsinfos, function( key, value ) {
        // name = value.name;
        // coords = Array( 1 * value.lon, 1 * value.lat );
        // adresse = value.adresse;
        // var isil = value.isil;
        // var collection = {
        // 'coords': coords,
        // 'name': name,
        // 'isil': isil,
        // 'adresse': adresse
        // }
        // _defaults.collections.push( collection );
        // } );
        //                
        // cms.geoLocations.generateMapForCollections( 'widgetGeoLocationsMap',
        // centercoords, _defaults.collections,
        // _defaults.goettingenOverviewZoomFactorForSidebar, false );
        // }
        // },
        // load and generate "non interactive" map for sidebar
        generateMapForCollections: function( container, centercoords, collections, zoom, interactive ) {
            mapboxgl.accessToken = _defaults.mapboxAccessToken;
            var map = new mapboxgl.Map( {
                container: container, // container id
                style: 'mapbox://styles/lirumgbv/cii024wxn009aiolzgy2zlycj', // stylesheet
                // location
                center: centercoords, // starting position
                zoom: zoom, // starting zoom
                interactive: interactive
            // map static or interactive?
            } );
            
            if ( interactive ) {
                // add controls
                map.addControl( new mapboxgl.Navigation() );
                
                // disable map rotation
                map.dragRotate.disable();
            }
            
            // build markers
            var features = new Array;
            $.each( collections, function( key, value ) {
                var feature = {
                    "type": "Feature",
                    "geometry": {
                        "type": "Point",
                        "coordinates": value.coords
                    },
                    "properties": {
                        "description": '<strong>' + value.name + '</strong>',
                        "adresse": value.adresse,
                        "isil": value.isil
                    }
                }
                features.push( feature );
            } );
            
            map.on( 'style.load', function() {
                map.addSource( "markers", {
                    "type": "geojson",
                    "data": {
                        "type": "FeatureCollection",
                        "features": features
                    }
                } );
                
                map.addLayer( {
                    "id": "markers",
                    "type": "symbol",
                    "source": "markers",
                    "layout": {
                        "icon-image": "pin",
                        "icon-allow-overlap": true
                    }
                } );
            } );
            
            // Create a popup, but don't add it to the map yet.
            var popup = new mapboxgl.Popup( {
                closeButton: false,
                closeOnClick: false
            } );
            
            map.on( 'mousemove', function( e ) {
                var features = map.queryRenderedFeatures( e.point, {
                    layers: [ 'markers' ]
                } );
                
                // Change the cursor style as a UI indicator.
                map.getCanvas().style.cursor = ( features.length ) ? 'pointer' : '';
                
                if ( !features.length ) {
                    popup.remove();
                    return;
                }
                
                var feature = features[ 0 ];
                
                // Populate the popup and set its coordinates
                // based on the feature found.
                popup.setLngLat( feature.geometry.coordinates ).setHTML( feature.properties.description + '<br />' + feature.properties.adresse ).addTo( map );
            } );
            
            map.on( 'click', function( e ) {
                var features = map.queryRenderedFeatures( e.point, {
                    layers: [ 'markers' ]
                } );
                
                // Change the cursor style as a UI indicator.
                map.getCanvas().style.cursor = ( features.length ) ? 'pointer' : '';
                
                if ( !features.length ) {
                    popup.remove();
                    return;
                }
                
                var feature = features[ 0 ];
                // Populate the popup and set its coordinates
                // based on the feature found.
                window.location = '/sammlung/' + feature.properties.isil + '/';
            } );
        }
    };
    
    /**
     * Method which .
     * 
     * @method _showCurrentImageInfo
     */
    // function _showCurrentImageInfo() {
    // if ( _debug ) {
    // console.log( '---------- _showCurrentImageInfo() ----------' );
    // }
    // }
    return cms;
    
} )( cmsJS || {}, jQuery );
