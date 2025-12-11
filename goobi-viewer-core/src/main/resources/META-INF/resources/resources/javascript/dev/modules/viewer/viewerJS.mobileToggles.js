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
 * @description This module steers the mobile toggle features for some old responsive themes.
 * @version 3.4.0
 * @module viewerJS.mobileToggles
 * @requires jQuery
 */
var viewerJS = ( function ( viewer ) {
    'use strict';

    var _debug = false;

    viewer.mobileToggles = {
    	/**
    	 * @description Method to initialize the mobile toggle features.
    	 * @method init 
    	 * */
    	init: function( config ) {
    		if (_debug) {
    			console.log( 'Initializing: viewerJS.mobileToggles.init' );
    		}
    		
    		// off canvas
            $( '[data-toggle="offcanvas"]' ).on( 'click', function() {
                $( '.row-offcanvas' ).toggleClass( 'active' );
                $( this ).toggleClass( 'in' );
            } );
            
            // toggle mobile navigation
            $( '[data-toggle="mobilenav"]' ).on( 'click', function() {
                $( '.btn-toggle.search' ).removeClass( 'in' );
                $( '.header-actions__search' ).hide();
                $( '.btn-toggle.language' ).removeClass( 'in' );
                $( '#mobileNav' ).slideToggle( 'fast' );
            } );
            $( '[data-toggle="mobile-image-controls"]' ).on( 'click', function() {
                $( '.image-controls' ).slideToggle( 'fast' );
            } );
            
            // toggle language
            $( '[data-toggle="language"]' ).on( 'click', function() {
                $( '.btn-toggle.search' ).removeClass( 'in' );
                $( '.header-actions__search' ).hide();
                $( this ).toggleClass( 'in' );
                $( '#changeLocal' ).fadeToggle( 'fast' );
            } );
            
            // toggle search
            $( '[data-toggle="search"]' ).on( 'click', function() {
                $( '.btn-toggle.language' ).removeClass( 'in' );
                $( '#changeLocal' ).hide();
                $( this ).toggleClass( 'in' );
                $( '.header-actions__search' ).fadeToggle( 'fast' );
            } );
    	}
    }

    return viewer;

} )( viewerJS || {}, jQuery );
    
