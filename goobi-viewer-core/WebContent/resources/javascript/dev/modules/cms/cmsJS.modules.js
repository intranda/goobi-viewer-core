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
 * Module which enables a sortable List based on jQuery UI. This list is used for the
 * mainmenu items and dynamic sidebar elements.
 * 
 * @version 3.2.0
 * @module cmsJS.sortableList
 * @requires jQuery, jQuery UI
 */
var cmsJS = ( function( cms ) {
    'use strict';
    
    var _debug = false;
    
    cms.modules = {
        /**
         * Method which initializes the CMS sortable list items and sets events.
         * 
         * @method init
         */
        init: function() {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'cmsJS.modules.init' );
                console.log( '##############################' );
            }
            
            // toggle input helptext
            $( '[data-toggle="helptext"]' ).on( 'click', function() {
            	$( this ).toggleClass( 'in' );
            	$( this ).parent().prev().toggleClass( 'in' );
            	$( this ).parent().prev().find( '.cms-module__option-control-helptext' ).toggleClass( 'in' );
            } );
            
            // toggle add new item
            $( '[data-toggle="available-items"]' ).on( 'click', function() {
            	$( this ).toggleClass( 'in' );
            	$( this ).parent().find( '.cms-menu__available-items-toggle' ).slideToggle( 'fast' );
            } );
        }
    };
    
    return cms;
    
} )( cmsJS || {}, jQuery );
