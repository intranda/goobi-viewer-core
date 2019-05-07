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
 * Module to manage the user dropdown.
 * 
 * @version 3.2.0
 * @module viewerJS.userDropdown
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false;
    var _bookshelfDropdown = false;
    var _defaults = {};
    
    viewer.userDropdown = {
        init: function() {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.userDropdown.init' );
                console.log( '##############################' );
            }
            
            // check if bookshelfdropdown exist
            if ( $( '.bookshelf-navigation__dropdown:visible' ) ) {
                _bookshelfDropdown = true;
            }
            
            // login dropdown
            $( '[data-toggle="login-dropdown"]' ).on( 'click', function( event ) {
                event.stopPropagation();
                
                // hide bookshelfdropdow if exist
                if ( _bookshelfDropdown ) {
                    $( '.bookshelf-navigation__dropdown' ).hide();
                    $( '.bookshelf-popup' ).remove();
                }
                // hide collection panel if exist
                if ( $( '.navigation__collection-panel' ).length > 0 ) {
                    $( '.navigation__collection-panel' ).hide();
                }
                
                $( '.login-navigation__login-dropdown' ).slideToggle( 'fast' );
            } );
            // user dropdown
            $( '[data-toggle="user-dropdown"]' ).on( 'click', function( event ) {
                event.stopPropagation();
                
                // hide bookshelfdropdow if exist
                if ( _bookshelfDropdown ) {
                    $( '.bookshelf-navigation__dropdown' ).hide();
                    $( '.bookshelf-popup' ).remove();
                }
                // hide collection panel if exist
                if ( $( '.navigation__collection-panel' ).length > 0 ) {
                    $( '.navigation__collection-panel' ).hide();
                }
                
                $( '.login-navigation__user-dropdown' ).slideToggle( 'fast' );
            } );
            
            // retrieve account
            $( '[data-toggle="retrieve-account"]' ).on( 'click', function() {
                $( '.login-navigation__retrieve-account' ).addClass( 'in' );
            } );
            $( '[data-dismiss="retrieve-account"]' ).on( 'click', function() {
                $( '.login-navigation__retrieve-account' ).removeClass( 'in' );
            } );
            
            // remove dropdown by clicking on body
            $( 'body' ).on( 'click', function( event ) {
                var target = $( event.target );
                var dropdown = $( '.login-navigation__user-dropdown, .login-navigation__login-dropdown' );
                var dropdownChild = dropdown.find( '*' );
                
                if ( !target.is( dropdown ) && !target.is( dropdownChild ) ) {
                    dropdown.hide();
                }
            } );
        }
    };
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
