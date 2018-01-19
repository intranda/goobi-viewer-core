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
 * Base-Module which initialize the global viewer object.
 * 
 * @version 3.2.0
 * @module viewerJS
 */
var viewerJS = ( function() {
    'use strict';
    
    var _debug = false;
    var _defaults = {
        currentPage: '',
        browser: '',
        sidebarSelector: '#sidebar',
        contentSelector: '#main',
        equalHeightRSSInterval: 1000,
        equalHeightInterval: 500,
        messageBoxSelector: '.messages .alert',
        messageBoxInterval: 1000,
        messageBoxTimeout: 8000,
        pageScrollSelector: '.icon-totop',
        pageScrollAnchor: '#top',
        widgetNerSidebarRight: false
    };
    
    var viewer = {};
    
    viewer.init = function( config ) {
        if ( _debug ) {
            console.log( '##############################' );
            console.log( 'viewer.init' );
            console.log( '##############################' );
            console.log( 'viewer.init: config - ', config );
        }
        
        $.extend( true, _defaults, config );
        
        // detect current browser
        _defaults.browser = viewerJS.helper.getCurrentBrowser();
        
        console.info( 'Current Page = ', _defaults.currentPage );
        console.info( 'Current Browser = ', _defaults.browser );
        
        /*
         * ! IE10 viewport hack for Surface/desktop Windows 8 bug Copyright 2014-2015
         * Twitter, Inc. Licensed under MIT
         * (https://github.com/twbs/bootstrap/blob/master/LICENSE)
         */

        // See the Getting Started docs for more information:
        // http://getbootstrap.com/getting-started/#support-ie10-width
        ( function() {
            'use strict';
            
            if ( navigator.userAgent.match( /IEMobile\/10\.0/ ) ) {
                var msViewportStyle = document.createElement( 'style' )
                msViewportStyle.appendChild( document.createTextNode( '@-ms-viewport{width:auto!important}' ) )
                document.querySelector( 'head' ).appendChild( msViewportStyle )
            }
        } )();
        
        // enable BS tooltips
        $( '[data-toggle="tooltip"]' ).tooltip();
        
        // render warning if local storage is not useable
        if ( !viewer.localStoragePossible ) {
            var warningPopover = this.helper
                    .renderWarningPopover( 'Ihr Browser befindet sich im privaten Modus und unterstützt die Möglichkeit Informationen zur Seite lokal zu speichern nicht. Aus diesem Grund sind nicht alle Funktionen des viewers verfügbar. Bitte verlasen Sie den privaten Modus oder benutzen einen alternativen Browser. Vielen Dank.' );
            
            $( 'body' ).append( warningPopover );
            
            $( '[data-toggle="warning-popover"]' ).on( 'click', function() {
                $( '.warning-popover' ).fadeOut( 'fast', function() {
                    $( '.warning-popover' ).remove();
                } );
            } );
        }
        
        // off canvas
        $( '[data-toggle="offcanvas"]' ).click( function() {
            var icon = $( this ).children( '.glyphicon' );
            
            $( '.row-offcanvas' ).toggleClass( 'active' );
            $( this ).toggleClass( 'in' );
            
            if ( icon.hasClass( 'glyphicon-option-vertical' ) ) {
                icon.removeClass( 'glyphicon-option-vertical' ).addClass( 'glyphicon-option-horizontal' );
            }
            else {
                icon.removeClass( 'glyphicon-option-horizontal' ).addClass( 'glyphicon-option-vertical' );
            }
        } );
        
        // toggle mobile navigation
        $( '[data-toggle="mobilenav"]' ).on( 'click', function() {
            $( '.btn-toggle.search' ).removeClass( 'in' );
            $( '.header-actions__search' ).hide();
            $( '.btn-toggle.language' ).removeClass( 'in' );
            $( '#changeLocal' ).hide();
            $( '#mobileNav' ).slideToggle( 'fast' );
        } );
        $( '[data-toggle="mobile-image-controls"]' ).on( 'click', function() {
            $( '.image-controls__actions' ).slideToggle( 'fast' );
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
        
        // set content height to sidebar height
        if ( window.matchMedia( '(max-width: 768px)' ).matches ) {
            if ( $( '.rss_wrapp' ).length > 0 ) {
                setTimeout( function() {
                    viewerJS.helper.equalHeight( _defaults.sidebarSelector, _defaults.contentSelector );
                }, _defaults.equalHeightRSSInterval );
            }
            else {
                setTimeout( function() {
                    viewerJS.helper.equalHeight( _defaults.sidebarSelector, _defaults.contentSelector );
                }, _defaults.equalHeightInterval );
            }
            $( window ).on( "orientationchange", function() {
                viewerJS.helper.equalHeight( _defaults.sidebarSelector, _defaults.contentSelector );
            } );
        }
        
        // fade out message box if it exists
        ( function() {
            var fadeoutScheduled = false;
            
            setInterval( function() {
                if ( $( _defaults.messageBoxSelector ).size() > 0 ) {
                    if ( !fadeoutScheduled ) {
                        fadeoutScheduled = true;
                        var messageTimer = setTimeout( function() {
                            $( _defaults.messageBoxSelector ).each( function() {
                                $( this ).fadeOut( 'slow', function() {
                                    $( this ).parent().remove();
                                } )
                            } );
                            
                            fadeoutScheduled = false;
                        }, _defaults.messageBoxTimeout );
                    }
                }
            }, _defaults.messageBoxInterval );
        } )();
        
        // add class on toggle sidebar widget (CMS individual sidebar widgets)
        $( '.collapse' ).on( 'show.bs.collapse', function() {
            $( this ).prev().find( '.glyphicon' ).removeClass( 'glyphicon-arrow-down' ).addClass( 'glyphicon-arrow-up' );
        } );
        
        $( '.collapse' ).on( 'hide.bs.collapse', function() {
            $( this ).prev().find( '.glyphicon' ).removeClass( 'glyphicon-arrow-up' ).addClass( 'glyphicon-arrow-down' );
        } );
        
        // scroll page animated
        this.pageScroll.init( _defaults.pageScrollSelector, _defaults.pageScrollAnchor );
        
        // check for sidebar toc and set viewport position
        if ( viewer.localStoragePossible ) {
            if ( $( '#image_container' ).length > 0 || currentPage === 'readingmode' ) {
                if ( localStorage.getItem( 'currIdDoc' ) === null ) {
                    localStorage.setItem( 'currIdDoc', 'false' );
                }
                
                this.helper.saveSidebarTocPosition();
            }
            else {
                localStorage.setItem( 'sidebarTocScrollPosition', 0 );
            }
        }
        
        // AJAX Loader Eventlistener
        if ( typeof jsf !== 'undefined' ) {
            jsf.ajax.addOnEvent( function( data ) {
                var ajaxstatus = data.status;
                var ajaxloader = document.getElementById( "AJAXLoader" );
                var ajaxloaderSidebarToc = document.getElementById( "AJAXLoaderSidebarToc" );
                
                if ( ajaxloaderSidebarToc && ajaxloader ) {
                    switch ( ajaxstatus ) {
                        case "begin":
                            ajaxloaderSidebarToc.style.display = 'block';
                            ajaxloader.style.display = 'none';
                            break;
                        
                        case "complete":
                            ajaxloaderSidebarToc.style.display = 'none';
                            ajaxloader.style.display = 'none';
                            break;
                        
                        case "success":
                            // enable BS tooltips
                            $( '[data-toggle="tooltip"]' ).tooltip();
                            
                            if ( viewer.localStoragePossible ) {
                                viewer.helper.saveSidebarTocPosition();
                                
                                $( '.widget-toc-elem-wrapp' ).scrollTop( localStorage.sidebarTocScrollPosition );
                            }
                            // set content height to sidebar height
                            if ( window.matchMedia( '(max-width: 768px)' ).matches ) {
                                viewerJS.helper.equalHeight( _defaults.sidebarSelector, _defaults.contentSelector );
                                
                                $( window ).off().on( "orientationchange", function() {
                                    viewerJS.helper.equalHeight( _defaults.sidebarSelector, _defaults.contentSelector );
                                } );
                            }
                            break;
                    }
                }
                else if ( ajaxloader ) {
                    switch ( ajaxstatus ) {
                        case "begin":
                            ajaxloader.style.display = 'block';
                            break;
                        
                        case "complete":
                            ajaxloader.style.display = 'none';
                            break;
                        
                        case "success":
                            // enable BS tooltips
                            $( '[data-toggle="tooltip"]' ).tooltip();
                            
                            // set content height to sidebar height
                            if ( window.matchMedia( '(max-width: 768px)' ).matches ) {
                                viewerJS.helper.equalHeight( _defaults.sidebarSelector, _defaults.contentSelector );
                                
                                $( window ).off().on( "orientationchange", function() {
                                    viewerJS.helper.equalHeight( _defaults.sidebarSelector, _defaults.contentSelector );
                                } );
                            }
                            break;
                    }
                }
                else {
                    switch ( ajaxstatus ) {
                        case "success":
                            // enable BS tooltips
                            $( '[data-toggle="tooltip"]' ).tooltip();
                            
                            // set content height to sidebar height
                            if ( window.matchMedia( '(max-width: 768px)' ).matches ) {
                                viewerJS.helper.equalHeight( _defaults.sidebarSelector, _defaults.contentSelector );
                                
                                $( window ).off().on( "orientationchange", function() {
                                    viewerJS.helper.equalHeight( _defaults.sidebarSelector, _defaults.contentSelector );
                                } );
                            }
                            break;
                    }
                }
            } );
        }
        
        // disable submit button on feedback
        if ( currentPage === 'feedback' ) {
            $( '#submitFeedbackBtn' ).attr( 'disabled', true );
        }
        
        // set sidebar position for NER-Widget
        if ( $( '#widgetNerFacetting' ).length > 0 ) {
            nerFacettingConfig.sidebarRight = _defaults.widgetNerSidebarRight;
            this.nerFacetting.init( nerFacettingConfig );
        }
        
        // make sure only integer values may be entered in input fields of class
        // 'input-integer'
        $( '.input-integer' ).on( "keypress", function( event ) {
            if ( event.which < 48 || event.which > 57 ) {
                return false;
            }
        } );
        
        // make sure only integer values may be entered in input fields of class
        // 'input-integer'
        $( '.input-float' ).on( "keypress", function( event ) {
            console.log( event );
            switch ( event.which ) {
                case 8: // delete
                case 9: // tab
                case 13: // enter
                case 46: // dot
                case 44: // comma
                case 43: // plus
                case 45: // minus
                    return true;
                case 118:
                    return event.ctrlKey; // copy
                default:
                    switch ( event.keyCode ) {
                        case 8: // delete
                        case 9: // tab
                        case 13: // enter
                            return true;
                        default:
                            if ( event.which < 48 || event.which > 57 ) {
                                return false;
                            }
                            else {
                                return true;
                            }
                    }
            }
        } );
        
        // set tinymce language
        this.tinyConfig.language = currentLang;
        
        if ( currentPage === 'adminCmsCreatePage' ) {
            this.tinyConfig.setup = function( ed ) {
                // listen to changes on tinymce input fields
                ed.on( 'change input paste', function( e ) {
                    tinymce.triggerSave();
                    createPageConfig.prevBtn.attr( 'disabled', true );
                    createPageConfig.prevDescription.show();
                } );
            };
            // TODO: Für ZLB in der custom.js einbauen
            // viewerJS.tinyConfig.textcolor_map = [ "FFFFFF", "ZLB-Weiß", "333333",
            // "ZLB-Schwarz", "dedede", "ZLB-Hellgrau", "727c87", "ZLB-Mittelgrau",
            // "9a9a9a", "ZLB-Dunkelgrau",
            // "CD0000", "ZLB-Rot", "92406d", "ZLB-Lila", "6f2c40", "ZLB-Bordeaux",
            // "ffa100", "ZLB-Orange", "669933", "ZLB-Grün", "3e5d1e", "ZLB-Dunkelgrün",
            // "a9d0f5",
            // "ZLB-Hellblau", "28779f", "ZLB-Blau" ];
        }
        
        if ( currentPage === 'overview' ) {
            // activate menubar
            viewerJS.tinyConfig.menubar = true;
            viewerJS.tinyMce.overview();
        }
        
        // AJAX Loader Eventlistener for tinyMCE
        if ( typeof jsf !== 'undefined' ) {
            jsf.ajax.addOnEvent( function( data ) {
                var ajaxstatus = data.status;
                
                switch ( ajaxstatus ) {
                    case "success":
                        if ( currentPage === 'overview' ) {
                            viewerJS.tinyMce.overview();
                        }
                        
                        viewerJS.tinyMce.init( viewerJS.tinyConfig );
                        break;
                }
            } );
        }
        
        // init tinymce if it exists
        if ( $( '.tinyMCE' ).length > 0 ) {
            viewerJS.tinyMce.init( this.tinyConfig );
        }
        
        // handle browser bugs
        switch ( _defaults.browser ) {
            case 'Chrome':
                /* BROKEN IMAGES */
                $( 'img' ).error( function() {
                    $( this ).addClass( 'broken' );
                } );
                break;
            case 'Firefox':
                /* BROKEN IMAGES */
                $( "img" ).error( function() {
                    $( this ).hide();
                } );
                /* 1px BUG */
                if ( $( '.image-doublePageView' ).length > 0 ) {
                    $( '.image-doublePageView' ).addClass( 'oneUp' );
                }
                break;
            case 'IE':
                /* SET IE CLASS TO HTML */
                $( 'html' ).addClass( 'is-IE' );
                /* BROKEN IMAGES */
                $( "img" ).error( function() {
                    $( this ).hide();
                } );
                break;
            case 'Edge':
                /* BROKEN IMAGES */
                $( "img" ).error( function() {
                    $( this ).hide();
                } );
                break;
            case 'Safari':
                /* BROKEN IMAGES */
                $( "img" ).error( function() {
                    $( this ).hide();
                } );
                break;
        }
    };
    
    // global object for tinymce config
    viewer.tinyConfig = {};
    
    return viewer;
    
} )( jQuery );
