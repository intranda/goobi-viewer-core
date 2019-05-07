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
 * This module drives the functionality of the main navigation from the Goobi viewer.
 * 
 * @version 3.2.0
 * @module viewerJS.navigation
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false;
    var _defaults = {
        navigationSelector: '#navigation',
        subMenuSelector: '[data-toggle="submenu"]',
        megaMenuSelector: '[data-toggle="megamenu"]',
        closeMegaMenuSelector: '[data-toggle="close"]',
    };
    
    viewer.navigation = {
        /**
         * Method to initialize the viewer main navigation.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.navigationSelector The selector for the navigation
         * element.
         * @param {String} config.subMenuSelector The selector for the submenu element.
         * @param {String} config.megaMenuSelector The selector for the mega menu element.
         * @param {String} config.closeMegaMenuSelector The selector for the close mega
         * menu element.
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.navigation.init' );
                console.log( '##############################' );
                console.log( 'viewer.navigation.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );
            
            // TRIGGER STANDARD MENU
            $( _defaults.subMenuSelector ).on( 'click', function() {
                var currTrigger = $( this );
                
                if ( $( this ).parents( '.navigation__submenu' ).hasClass( 'in' ) ) {
                    _resetSubMenus();
                    currTrigger.parent().addClass( 'active' );
                    currTrigger.next( '.navigation__submenu' ).addClass( 'in' );
                    _calcSubMenuPosition( currTrigger.next( '.navigation__submenu' ) );
                }
                else {
                    _resetMenus();
                    currTrigger.parent().addClass( 'active' );
                    currTrigger.next( '.navigation__submenu' ).addClass( 'in' );
                }
            } );
            
            // TRIGGER MEGA MENU
            $( _defaults.megaMenuSelector ).on( 'click', function() {
                _resetMenus();
                
                if ( $( this ).next( '.navigation__megamenu-wrapper' ).hasClass( 'in' ) ) {
                    $( this ).parent().removeClass( 'active' );
                    $( this ).next( '.navigation__megamenu-wrapper' ).removeClass( 'in' );
                }
                else {
                    $( '.navigation__megamenu-trigger' ).removeClass( 'active' );
                    $( '.navigation__megamenu-wrapper' ).removeClass( 'in' );
                    $( this ).parent().addClass( 'active' );
                    $( this ).next( '.navigation__megamenu-wrapper' ).addClass( 'in' );
                }
            } );
            
            $( _defaults.closeMegaMenuSelector ).on( 'click', function() {
                _resetMenus();
            } );
            
            if ( $( '.navigation__megamenu-wrapper' ).length > 0 ) {
                _resetMenus();
            }
            
            // reset all menus by clicking on body
            $( 'body' ).on( 'click', function( event ) {
                if ( event.target.id == 'navigation' || $( event.target ).closest( _defaults.navigationSelector ).length ) {
                    return;
                }
                else {
                    _resetMenus();
                }
            } );
        },
    };
    
    /**
     * Method to reset all shown menus.
     * 
     * @method _resetMenus
     */
    function _resetMenus() {
        if ( _debug ) {
            console.log( '---------- _resetMenus() ----------' );
        }
        
        $( '.navigation__submenu-trigger' ).removeClass( 'active' );
        $( '.navigation__submenu' ).removeClass( 'in' );
        $( '.navigation__megamenu-trigger' ).removeClass( 'active' );
        $( '.navigation__megamenu-wrapper' ).removeClass( 'in' );
    }
    
    /**
     * Method to reset all shown submenus.
     * 
     * @method _resetSubMenus
     */
    function _resetSubMenus() {
        $( '.level-2, .level-3, .level-4, .level-5' ).parent().removeClass( 'active' );
        $( '.level-2, .level-3, .level-4, .level-5' ).removeClass( 'in' ).removeClass( 'left' );
    }
    
    /**
     * Method to calculate the position of the shown submenu.
     * 
     * @method _calcSubMenuPosition
     * @param {Object} menu An jQuery object of the current submenu.
     */
    function _calcSubMenuPosition( menu ) {
        if ( _debug ) {
            console.log( '---------- _clacSubMenuPosition() ----------' );
            console.log( '_clacSubMenuPosition: menu - ', menu );
        }
        
        var currentOffsetLeft = menu.offset().left;
        var menuWidth = menu.outerWidth();
        var windowWidth = $( window ).outerWidth();
        var offsetWidth = currentOffsetLeft + menuWidth;
        
        if ( _debug ) {
            console.log( '_clacSubMenuPosition: currentOffsetLeft - ', currentOffsetLeft );
            console.log( '_clacSubMenuPosition: menuWidth - ', menuWidth );
            console.log( '_clacSubMenuPosition: windowWidth - ', windowWidth );
            console.log( '_clacSubMenuPosition: offsetWidth - ', offsetWidth );
        }
        
        if ( offsetWidth >= windowWidth ) {
            menu.addClass( 'left' ).css( 'width', menuWidth );
        }
        else {
            return false;
        }
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
