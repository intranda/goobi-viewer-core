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
 * @version 3.4.0
 * @module adminJS.sidebar
 * @requires jQuery
 * @description Module which sets the behavior of the admin sidebar.
 */
var adminJS = ( function( admin ) {
    'use strict';
    
    var _debug = false;
    
    admin.sidebar = {
        /**
         * @description Method which initializes the admin sidebar module.
         * @method init
         */
        init: function() {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'adminJS.sidebar.init' );
                console.log( '##############################' );
            }
            
            // set scroll event
            _setScrollEvent();
            
            // set scroll event on resize and orientationchange
            $( window ).on( 'resize orientationchange', function() {
            	_setScrollEvent();            	
            } );
            
            // set scroll event on ajax success
            if ( typeof jsf !== 'undefined' ) {
	            jsf.ajax.addOnEvent( function( data ) {
	                var ajaxstatus = data.status;
	                
	                switch ( ajaxstatus ) {                        
		                case "success":
		                	// set scroll event
		                	_setScrollEvent();
		                	break;
	                }
	            } );
            }            
        }
    }
    
    /**
     * @description Method to set an scroll event for the sidebar.
     * @method _setScrollEvent
     * */
    function _setScrollEvent() {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _setScrollEvent' );
    	}
    	
    	$( window ).off( 'scroll' ).on( 'scroll', function() {
    		var windowHeight = $( this ).height();
    		var sidebarHeight = $( '#adminSidebar' ).outerHeight();
    		var contentHeight = $( '#adminContent' ).outerHeight();
    		var diffSidebar = sidebarHeight - windowHeight;
    		var diffContent = contentHeight - windowHeight;
    		var scrollPos = $( this ).scrollTop();
    		
    		if(sidebarHeight <= contentHeight) {
    		    if ( scrollPos >= diffSidebar && windowHeight < sidebarHeight ) {
                    $( '#adminSidebar' ).removeClass( 'fixed-top' ).addClass( 'fixed-bottom' );
                } else if ( windowHeight > sidebarHeight ) {
                    $( '#adminSidebar' ).removeClass( 'fixed-bottom' ).addClass( 'fixed-top' );;
                } else {
                    $( '#adminSidebar, #adminContent' ).removeClass( 'fixed-top, fixed-bottom' );
                }
    		} else {
    		    if ( scrollPos >= diffContent && windowHeight < contentHeight ) {
    		        $( '#adminContent' ).removeClass( 'fixed-top' ).addClass( 'fixed-bottom' );
    		    } else if ( windowHeight > contentHeight ) {
    		        $( '#adminContent' ).removeClass( 'fixed-bottom' ).addClass( 'fixed-top' );;
    		    }else {
    		        $( '#adminSidebar, #adminContent' ).removeClass( 'fixed-top, fixed-bottom' );
    		    }
    		}
    		
    	} );    	
    }
    
    return admin;
    
} )( adminJS || {}, jQuery );
