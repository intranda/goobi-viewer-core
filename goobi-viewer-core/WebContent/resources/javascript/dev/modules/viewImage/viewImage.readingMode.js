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
 * Module which handles the viewer reading mode.
 * 
 * @version 3.2.0
 * @module viewImage.readingMode
 * @requires jQuery
 */
var viewImage = ( function( osViewer ) {
    'use strict';
    
    var _debug = false;
    var _localStoragePossible = true;
    var _activePanel = null;
    var _defaults = {
        navSelector: '.reading-mode__navigation',
        viewSelector: '#contentView',
        imageContainerSelector: '.reading-mode__content-view-image',
        imageSelector: '#readingModeImage',
        sidebarSelector: '#contentSidebar',
        sidebarToggleButton: '.reading-mode__content-sidebar-toggle',
        sidebarInnerSelector: '.reading-mode__content-sidebar-inner',
        sidebarTabsSelector: '.reading-mode__content-sidebar-tabs',
        sidebarTabContentSelector: '.tab-content',
        sidebarTocWrapperSelector: '.widget-toc-elem-wrapp',
        sidebarStatus: '',
        useTabs: true,
        useAccordeon: false,
        msg: {},
    };
    
    osViewer.readingMode = {
        /**
         * Method to initialize the viewer reading mode.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.navSelector A string which contains the selector for the
         * navigation.
         * @param {String} config.viewSelector A string which contains the selector for
         * the content view.
         * @param {String} config.imageContainerSelector A string which contains the
         * selector for the image container.
         * @param {String} config.imageSelector A string which contains the selector for
         * the image.
         * @param {String} config.sidebarSelector A string which contains the selector for
         * the sidebar.
         * @param {String} config.sidebarToggleButton A string which contains the selector
         * for the sidebar toggle button.
         * @param {String} config.sidebarInnerSelector A string which contains the
         * selector for the inner sidebar container.
         * @param {String} config.sidebarStatus A string which contains the current
         * sidebar status.
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'osViewer.readingMode.init' );
                console.log( '##############################' );
                console.log( 'osViewer.readingMode.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );
            
            // check local storage
            _localStoragePossible = viewerJS.helper.checkLocalStorage();
            
            if ( _localStoragePossible ) {
                _defaults.sidebarStatus = localStorage.getItem( 'sidebarStatus' );
                
                if ( _defaults.sidebarStatus === '' || _defaults.sidebarStatus === undefined ) {
                    localStorage.setItem( 'sidebarStatus', 'true' );
                }
                
                // set viewport
                _setViewportHeight();
                if ( _defaults.useTabs ) {
                    _setSidebarTabHeight();
                }
                _setSidebarButtonPosition();
                _checkSidebarStatus();
                setTimeout( function() {
                    _showContent();
                }, 500 );
                
                // save panel status
                if ( _defaults.useAccordeon ) {
                    _activePanel = localStorage.getItem( 'activePanel' );
                    
                    $( '.panel-collapse' ).each( function() {
                        $( this ).removeClass( 'in' );
                    } );
                    
                    if ( _activePanel === null ) {
                        localStorage.setItem( 'activePanel', '#collapseOne' );
                        _activePanel = localStorage.getItem( 'activePanel' );
                        
                        $( _activePanel ).addClass( 'in' );
                    }
                    else {
                        $( _activePanel ).addClass( 'in' );
                    }
                    
                    // click panel event
                    $( 'a[data-toggle="collapse"]' ).on( 'click', function() {
                        var currPanel = $( this ).attr( 'href' );
                        
                        localStorage.setItem( 'activePanel', currPanel );
                    } );
                }
                
                // events
                $( '[data-toggle="sidebar"]' ).on( 'click', function() {
                    $( this ).toggleClass( 'in' );
                    $( this ).parents( '.reading-mode__content-sidebar' ).toggleClass( 'in' );
                    $( this ).parents( '.reading-mode__content-sidebar' ).prev().toggleClass( 'in' );
                    
                    // set sidebar status to local storage
                    _defaults.sidebarStatus = localStorage.getItem( 'sidebarStatus' );
                    
                    if ( _defaults.sidebarStatus === 'false' ) {
                        localStorage.setItem( 'sidebarStatus', 'true' );
                    }
                    else {
                        localStorage.setItem( 'sidebarStatus', 'false' );
                    }
                } );
                
                $( window ).on( 'resize', function() {
                    _setViewportHeight();
                    if ( _defaults.useTabs ) {
                        _setSidebarTabHeight();
                    }
                    _setSidebarButtonPosition();
                } );
                
                $( window ).on( "orientationchange", function() {
                    _setViewportHeight();
                    if ( _defaults.useTabs ) {
                        _setSidebarTabHeight();
                    }
                    _setSidebarButtonPosition();
                } );
                
                // AJAX Loader Eventlistener
                if ( typeof jsf !== 'undefined' ) {
                    jsf.ajax.addOnEvent( function( data ) {
                        var ajaxstatus = data.status;
                        
                        switch ( ajaxstatus ) {
                            case "success":
                                _setViewportHeight();
                                if ( _defaults.useTabs ) {
                                    _setSidebarTabHeight();
                                }
                                _setSidebarButtonPosition();
                                break;
                        }
                    } );
                }
            }
            else {
                return false;
            }
        },
    };
    
    /**
     * Method which sets the height of the viewport elements.
     * 
     * @method _setViewportHeight
     */
    function _setViewportHeight() {
        if ( _debug ) {
            console.log( '---------- _setViewportHeight() ----------' );
            console.log( '_setViewportHeight: view = ', _defaults.viewSelector );
            console.log( '_setViewportHeight: image = ', _defaults.imageSelector );
            console.log( '_setViewportHeight: sidebar = ', _defaults.sidebarSelector );
            console.log( '_setViewportHeight: sidebarInner = ', _defaults.sidebarInnerSelector );
            console.log( '_setViewportHeight: sidebarTabs = ', _defaults.sidebarTabsSelector );
        }
        
        var viewportHeight = $( window ).outerHeight();
        var navHeight = $( _defaults.navSelector ).outerHeight();
        var newHeight = viewportHeight - navHeight;
        
        if ( _debug ) {
            console.log( '_setViewportHeight: viewportHeight = ', viewportHeight );
            console.log( '_setViewportHeight: navHeight = ', navHeight );
            console.log( '_setViewportHeight: newHeight = ', newHeight );
        }
        
        $( _defaults.viewSelector ).css( 'height', newHeight );
        $( _defaults.imageSelector ).css( 'height', newHeight );
        $( _defaults.sidebarSelector ).css( 'height', newHeight );
        $( _defaults.sidebarInnerSelector ).css( 'height', newHeight );
        
    }
    
    /**
     * Method which sets the height of the sidebar Tabs.
     * 
     * @method _setSidebarTabHeight
     */
    function _setSidebarTabHeight() {
        if ( _debug ) {
            console.log( '---------- _setSidebarTabHeight() ----------' );
            console.log( '_setSidebarTabHeight: sidebarTabs = ', _defaults.sidebarTabsSelector );
        }
        
        var viewportHeight = $( window ).outerHeight();
        var navHeight = $( _defaults.navSelector ).outerHeight();
        var newHeight = viewportHeight - navHeight;
        var tabPos = $( _defaults.sidebarTabsSelector ).position();
        var tabHeight = newHeight - tabPos.top - 15;
        var navTabsHeight = $( '.nav-tabs' ).outerHeight();
        
        if ( _debug ) {
            console.log( '_setSidebarTabHeight: tabPos = ', tabPos );
            console.log( '_setSidebarTabHeight: tabHeight = ', tabHeight );
        }
        
        if ( viewportHeight > 768 ) {
            $( _defaults.sidebarTabsSelector ).css( 'height', tabHeight );
            $( _defaults.sidebarTabContentSelector ).css( 'height', tabHeight - navTabsHeight );
            $( _defaults.sidebarTocWrapperSelector ).css( 'min-height', tabHeight - navTabsHeight );
        }
    }
    
    /**
     * Method which sets the position of the sidebar toggle button.
     * 
     * @method _setSidebarButtonPosition
     */
    function _setSidebarButtonPosition() {
        if ( _debug ) {
            console.log( '---------- _setSidebarButtonPosition() ----------' );
            console.log( '_setSidebarButtonPosition: view = ', _defaults.viewSelector );
        }
        
        var viewHalfHeight = $( _defaults.viewSelector ).outerHeight() / 2;
        
        if ( _debug ) {
            console.log( '_setSidebarButtonPosition: viewHalfHeight = ', viewHalfHeight );
        }
        
        $( _defaults.sidebarToggleButton ).css( 'top', viewHalfHeight );
        
    }
    
    /**
     * Method which checks the current sidebar status, based on a local storage value.
     * 
     * @method _checkSidebarStatus
     * @returns {Boolean} Returns false if the sidebar is inactive, returns true if the
     * sidebar is active.
     */
    function _checkSidebarStatus() {
        if ( _debug ) {
            console.log( '---------- _checkSidebarStatus() ----------' );
            console.log( '_checkSidebarStatus: sidebarStatus = ', _defaults.sidebarStatus );
        }
        
        if ( _defaults.sidebarStatus === 'false' ) {
            $( '[data-toggle="sidebar"]' ).removeClass( 'in' );
            $( '.reading-mode__content-sidebar' ).removeClass( 'in' ).prev().removeClass( 'in' );
            
            return false;
        }
        else {
            return true;
        }
        
    }
    
    /**
     * Method which shows the content by removing CSS-Classes after loading every page
     * element.
     * 
     * @method _showContent
     */
    function _showContent() {
        if ( _debug ) {
            console.log( '---------- _showContent() ----------' );
        }
        
        $( _defaults.viewSelector ).removeClass( 'invisible' );
        $( _defaults.sidebarSelector ).removeClass( 'invisible' );
    }
    
    return osViewer;
    
} )( viewImage || {}, jQuery );
