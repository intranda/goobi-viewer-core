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
 * @description This module saves the current scroll position of an element.
 * @version 3.4.0
 * @module viewerJS.scrollPositions
 * @requires jQuery
 */
var viewerJS = ( function ( viewer ) {
    'use strict';

    var _debug = false;
    var _positions = {
    	sidebarToc: {
    		link: 0
    	},
    };

    viewer.scrollPositions = {
    	/**
    	 * @description Method to initialize the save scroll position module.
    	 * @method init 
    	 * */
    	init: function( config ) {
    		if (_debug) {
    			console.log( 'Initializing: viewerJS.scrollPositions.init' );
    		}
    		
    		// set scroll status
    		_setScrollStatus();

    		// get scroll status
            _getScrollStatus();
    	}
    }
    
    /**
     * @description Method to set the scroll status.
     * @method _setScrollStatus
     */
    function _setScrollStatus() {
        if ( _debug ) {
            console.log( 'EXECUTE: _setScrollStatus' );
        }

        // set scroll status of sidebar toc
        $( '.widget-toc__element-link a' ).each( function() {
        	if ( $( this ).parents( 'li' ).hasClass( 'active' ) ) {
        		_positions.sidebarToc.link = $( this ).parents( 'li' ).position().top;
        		
        		sessionStorage.setItem( 'scrollPositions', JSON.stringify( _positions ) );        		
        	}
        } );
    }

    /**
     * @description Method to get the scroll status.
     * @method _getScrollStatus
     */
    function _getScrollStatus() {
        if ( _debug ) {
            console.log( 'EXECUTE: _getScrollStatus' );
        }

        var positions;
        
        if ( sessionStorage.getItem( 'scrollPositions' ) == null ) {
            sessionStorage.setItem( 'scrollPositions', JSON.stringify( _positions ) );
            positions = JSON.parse( sessionStorage.getItem( 'scrollPositions' ) );
            
            // scroll sidebar toc in position
            $( '.widget-toc__elements' ).scrollTop( positions.sidebarToc.link );
        }
        else {
            positions = JSON.parse( sessionStorage.getItem( 'scrollPositions' ) );
            
            // scroll sidebar toc in position            
            $( '.widget-toc__elements' ).scrollTop( positions.sidebarToc.link );
        }
    }

    return viewer;

} )( viewerJS || {}, jQuery );
    