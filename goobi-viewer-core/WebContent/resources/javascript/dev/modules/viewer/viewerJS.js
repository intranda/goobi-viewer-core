/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects. Visit these websites for more information. -
 * http://www.intranda.com - http://digiverso.com This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version. This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. Base-Module which initialize the global
 * viewer object.
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
        widgetNerSidebarRight: false,
        accessDeniedImage: '/resources/images/access_denied.png',
        notFoundImage: '/resources/images/not_found.png'
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
        
        // enable BS tooltips
        $( '[data-toggle="tooltip"]' ).tooltip( {
            trigger : 'hover'
        } );
        
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
            var icon = $( this ).children( '.fa' );
            
            $( '.row-offcanvas' ).toggleClass( 'active' );
            $( this ).toggleClass( 'in' );
            
            if ( icon.hasClass( 'fa-ellipsis-v' ) ) {
                icon.removeClass( 'fa-ellipsis-v' ).addClass( 'fa-ellipsis-h' );
            }
            else {
                icon.removeClass( 'fa-ellipsis-h' ).addClass( 'fa-ellipsis-v' );
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
                if ( $( _defaults.messageBoxSelector ).length > 0 ) {
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
            $( this ).prev().find( '.fa' ).removeClass( 'fa-arrow-down' ).addClass( 'fa-arrow-up' );
        } );
        
        $( '.collapse' ).on( 'hide.bs.collapse', function() {
            $( this ).prev().find( '.fa' ).removeClass( 'fa-arrow-up' ).addClass( 'fa-arrow-down' );
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
        
        // reset searchfield on focus
        $( 'input[id*="searchField"]' ).on( 'focus', function() {
        	$( this ).val( '' );
        } );
        
        viewer.loadThumbnails();
        
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
                            viewer.loadThumbnails();
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
                            viewer.loadThumbnails();
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
                            viewer.loadThumbnails();
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
        
        // fire search query in autocomplete on enter
        $( '#pfAutocomplete_input, [id*=":pfAutocomplete_input"]' ).on( 'keyup', function( event ) {
        	if ( event.keyCode == 13 ) {
        		$( '#submitSearch, [id*=":submitSearch"]' ).click();
        	}
        });
        
        // make sure only integer values may be entered in input fields of class
        // 'input-integer'
        $( '.input-integer' ).on( "keypress", function( event ) {
            if ( event.which < 48 || event.which > 57 ) {
                return false;
            }
        } );
        
        // make sure only integer values may be entered in input fields of class
        // 'input-float'
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
                $( 'img' ).on("error", function() {
                    $( this ).addClass( 'broken' );
                } );
                break;
            case 'Firefox':
                /* 1px BUG */
                if ( $( '.image-doublePageView' ).length > 0 ) {
                    $( '.image-doublePageView' ).addClass( 'oneUp' );
                }
                break;
            case 'IE':
                /* SET IE CLASS TO HTML */
                $( 'html' ).addClass( 'is-IE' );
                break;
            case 'Edge':
                break;
            case 'Safari':
                break;
        }
    };
    
    // load images with error handling
    viewer.loadThumbnails = function() {
        $('.viewer-thumbnail').each(function() {
            var element = this;
            var source = element.dataset.src; 
            if(source && !element.src) { 
                var accessDenied = currentPath + _defaults.accessDeniedImage;
                var notFound = currentPath + _defaults.notFoundImage;
                $.ajax({
                    url: source,
                    cache: true,
                    xhrFields: {
                        responseType: 'blob'
                    },
                })
                .done(function(blob) {
                    var url = window.URL || window.webkitURL;
                    element.src = url.createObjectURL(blob);
                })
                .fail(function(error) {
                    var status = error.status;
                        switch(status) {
                            case 403:
                                element.src = accessDenied;
                                break;
                            case 404:
                                element.src = notFound;
                                break;
                            default:
                                element.src = source;
                        }
                    });                    
            }
        });
    }
    
    // global object for tinymce config
    viewer.tinyConfig = {};
    
    return viewer;
    
} )( jQuery );
