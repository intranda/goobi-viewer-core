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
 * Module which renders current and other versions of a work into a widget.
 * 
 * @version 3.2.0
 * @module viewerJS.versionHistory
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false;
    var _defaults = {
        versions: [],
        json: null,
        imgUrl: '',
        imgPi: '',
        versionLink: '',
        widgetInputs: '',
        widgetList: '',
    };
    
    viewer.versionHistory = {
        /**
         * Method to initialize the version history widget.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {Array} config.versions An array which holds all versions.
         * @param {Object} config.json An JSON-Object which takes all versions.
         * @param {String} config.imgUrl The image URL for the current work.
         * @param {String} config.imgPi The PI for the image of the current work.
         * @param {String} config.versionLink A string placeholder for the final HTML.
         * @param {String} config.widgetInputs A string placeholder for the final HTML.
         * @param {String} config.widgetList A string placeholder for the final HTML.
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.versionHistory.init' );
                console.log( '##############################' );
                console.log( 'viewer.versionHistory.init: config = ', config );
            }
            
            $.extend( true, _defaults, config );
            
            // push versions into an array
            $( _defaults.widgetInputs ).each( function() {
                _defaults.versions.push( $( this ).val() );
            } );
            
            if ( _debug ) {
                console.log( 'viewer.versionHistory: versions = ', _defaults.versions );
            }
            
            // append list elements to widget
            for ( var i = 0; i < _defaults.versions.length; i++ ) {
                _defaults.json = JSON.parse( _defaults.versions[ i ] );
                
                if ( _defaults.json.id === _defaults.imgPi ) {
                    // Aktuell geöffnete Version - kein Link
                    _defaults.versionLink = '<li><span>';
                    if ( _defaults.json.label != undefined && _defaults.json.label != '' ) {
                    	_defaults.versionLink += _defaults.json.label;
                    }
                    else {
                    	 _defaults.versionLink += _defaults.json.id;
                    	 if ( _defaults.json.year != undefined && _defaults.json.year != '' ) {
                    		 _defaults.versionLink += ' (' + _defaults.json.year + ')';                    	
                    	 }
                    }
                    _defaults.versionLink += '</span></li>';
                    
                    $( _defaults.widgetList ).append( _defaults.versionLink );
                }
                else {
                    // Vorgänger und Nachfolger jeweils mit Link
                    _defaults.versionLink = '<li><a href="' + _defaults.imgUrl + '/' + _defaults.json.id + '/1/">';
                    if ( _defaults.json.label != undefined && _defaults.json.label != '' ) {
                    	_defaults.versionLink += _defaults.json.label;
                    } else {
                    	_defaults.versionLink += _defaults.json.id;
                    	if ( _defaults.json.year != undefined && _defaults.json.year != '' ) {
                    		_defaults.versionLink += ' (' + _defaults.json.year + ')';
                    	}
                    }
                    _defaults.versionLink += '</a></li>';
                    
                    $( _defaults.widgetList ).append( _defaults.versionLink );
                }
            }
        }
    };
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
