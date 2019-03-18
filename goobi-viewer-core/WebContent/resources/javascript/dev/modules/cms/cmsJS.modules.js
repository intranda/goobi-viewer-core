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
 * @module cmsJS.modules
 * @requires jQuery, jQuery UI
 * @description Module which enables the cms module functionality.
 */
var cmsJS = ( function( cms ) {
    'use strict';
    
    var _debug = false;
    
    cms.modules = {
        /**
         * @method init
         */
        init: function() {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'cmsJS.modules.init' );
                console.log( '##############################' );
            }
            this.initEventListeners();
        },
        
        onReload: function(data) {
            if(data && data.status == "begin") {
            	cms.modules.removeEventListeners();
            } else if(!data || data.status == "success") {
            	cms.modules.initEventListeners();
            }
        },

        removeEventListeners: function() {
            $( '[data-toggle="helptext"]' ).off( 'click' );
            $( '[data-toggle="available-items"]' ).off( 'click' );
            $( '[data-toggle="option-dropdown"]' ).off( 'click' );
        },

        initEventListeners: function() {
            // toggle input helptext
            $( '[data-toggle="helptext"]' ).on( 'click', function() {
            	$( this ).toggleClass( 'in' );
            	
            	var $input = $( this ).closest( '.cms-module__option-group' ).find( '.cms-module__option-control, .cms-module__option-dropdown' );
            	$input.toggleClass( 'in' );
            	$input.find( '.cms-module__option-control-helptext' ).toggleClass( 'in' );
            	// focus input
            	$input.find( '.form-control' ).focus();
            } );
            
            // toggle add new item accordeon
            $( '[data-toggle="available-items"]' ).on( 'click', function() {
            	if ( $( this ).hasClass( 'in' ) ) {
            		$( this ).toggleClass( 'in' );
                	$( this ).parent().find( '.cms-menu__available-items-toggle' ).slideToggle( 'fast', function() {
                		// focus first input if available
                		$( '.cms-menu__available-items-toggle .cms-module__option-group' ).first().find( '.form-control' ).focus();
                	} );
            	}
            	else {
            		$( '[data-toggle="available-items"], .cms-menu__available-items-toggle' ).each( function() {
            			$( '[data-toggle="available-items"]' ).removeClass( 'in' );
            			$( '.cms-menu__available-items-toggle' ).slideUp( 'fast' );
            		} );
            		
            		$( this ).toggleClass( 'in' );
            		$( this ).parent().find( '.cms-menu__available-items-toggle' ).slideToggle( 'fast', function() {
            			// focus first input if available
            			$( '.cms-menu__available-items-toggle .cms-module__option-group' ).first().find( '.form-control' ).focus();
            		} );            		
            	}
            } );
            
            // toggle option dropdown
            $( '[data-toggle="option-dropdown"]' ).on( 'click', function() {
            	$( this ).next().slideToggle( 'fast' );
            } );
            $( document ).on( 'click', function( event ) {
            	if ( $( event.target ).closest( '.cms-module__option-dropdown' ).length ) {	
            		return;
            	}
            	else {            		
            		$( '.cms-module__option-dropdown' ).find( 'ul' ).hide();
            	}
            } );
        }
    };
    
    return cms;
    
} )( cmsJS || {}, jQuery );
