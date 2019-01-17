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
 * @version 3.4.0
 * @module viewerJS.readingMode
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false;
    var _fadeout;
    var _sidebarWidth;
    var _sidebarLeft;
    var _sidebarId;
    var _defaults = {
    	resizeSelector: '#readingModeViewSidebar',
    	msg: {}
    };
    
    viewer.readingMode = {
        /**
         * Method to initialize the viewer reading mode.
         * 
         * @method init
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.readingMode.init' );
                console.log( '##############################' );
                console.log( 'viewer.readingMode.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );
            
            // display header on mousemove/touchmove
            $( '#readingModeViewImage, #readingModeViewSidebar' ).on( 'mousemove', function() {
            	_hideHeader( true );
            } );
            $( '#readingModeViewSidebar' ).on( 'click', function() {
            	_hideHeader( false );
            } );
            $( '#readingModeHeader' ).on( 'mousemove', function() {
            	_hideHeader( false );
            } );
            $( window ).on( 'touchstart touchend touchcancel touchmove', function() {
            	_hideHeader( true, 1000 );
            } );
            
            // make elements resizable
            if ( window.matchMedia( '(min-width: 769px)' ).matches ) {
            	_setResizable( _defaults.resizeSelector );
            }
            
            // set position on resize/orientationchange
            $( window ).on( 'resize orientationchange', function(e) {
            	_sidebarId = $( '#readingModeViewSidebar' ).attr( 'id' );
            	
            	// check if sidebar is resizing
            	if ( e.target['id'] != _sidebarId ) {
	            	$( 'body' ).hide();
	            	window.location.href = window.location.href;            		
            	}
            } );            
            
            // reset tooltips for sidebar
            $( '.widget-toc__title-expand [data-toggle="tooltip"]' ).tooltip( 'destroy' );
            $( '.widget-toc__title-expand [data-toggle="tooltip"]' ).tooltip( {
            	placement: 'bottom'
            } );
            
            // set sidebar and panel status
            _setSidebarStatus();
            _setPanelStatus();

            // toggle sidebar
            $( '[data-close="rm-sidebar"]' ).on( 'click', function() {
            	// set global variables
            	_sidebarWidth = $( '#readingModeViewSidebar' ).outerWidth();
            	_sidebarLeft = $( '#readingModeViewSidebar' ).css( 'left' );
            	
            	// set sidebar left position
            	$( '#readingModeViewSidebar' ).css( 'left', 'inherit' );
            	
            	// reset resizable
            	if ( window.matchMedia( '(min-width: 769px)' ).matches ) {
            		_unsetResizable( _defaults.resizeSelector );
            	}

            	// hide panel controls
            	$( '#readingModeSidebarPanelControls' ).hide();
            	
            	// slide out sidebar
            	$( '#readingModeViewSidebar' ).animate( {
            		right: '-' + _sidebarWidth + 'px'
            	}, 300, function() {            		
            		// show sidebar open
            		$( '#viewSidebarOpen' ).show();
            		
            		// show back and forward on small devices
                	if ( window.matchMedia( '(max-width: 480px)' ).matches ) {
                		$( '.image-controls__action.back, .image-controls__action.forward' ).show();
                	}

            		// save sidebar status
            		sessionStorage.setItem( 'rmSidebarStatus', false );
            	} );
            	
            	if ( $( '.reading-mode__view-image-thumbs-wrapper' ).is( ':visible' ) ) {
            		$( '.reading-mode__view-image-thumbs-wrapper' ).animate( {
            			width: '100%'
            		}, 300 );
    			}
            } );
            $( '[data-open="rm-sidebar"]' ).on( 'click', function() {
            	// hide sidebar open
            	$( '#viewSidebarOpen' ).hide();
            	
            	// show back and forward on small devices
            	if ( window.matchMedia( '(max-width: 480px)' ).matches ) {
            		$( '.image-controls__action.back, .image-controls__action.forward' ).hide();
            	}

            	// slide in sidebar
            	$( '#readingModeViewSidebar' ).animate( {
            		right: 0
            	}, 300, function() {
            		// set sidebar left position
            		$( '#readingModeViewSidebar' ).css( 'left', _sidebarLeft );
            		
            		// show panel controls
            		$( '#readingModeSidebarPanelControls' ).show();

            		// set resizable
            		if ( window.matchMedia( '(min-width: 769px)' ).matches ) {
            			_setResizable( _defaults.resizeSelector );
            		}
            		
            		// save sidebar status
            		sessionStorage.setItem( 'rmSidebarStatus', true );
            	} );
            	
            	if ( $( '.reading-mode__view-image-thumbs-wrapper' ).is( ':visible' ) ) {
            		$( '.reading-mode__view-image-thumbs-wrapper' ).animate( {
            			width: $( '#readingModeView' ).outerWidth() - _sidebarWidth
            		}, 300 );
    			}
            } );
            
            // scroll resize handle and hide panel controls
            $( '#readingModeViewSidebar' ).on( 'scroll', function() {
            	$( '.ui-resizable-handle' ).css( 'top', $( this ).scrollTop() );
            	
            	if ( $( this ).scrollTop() > 0 ) {
            		$( '#readingModeSidebarPanelControls' ).addClass( 'hidden' );
            	}
            	else {
            		$( '#readingModeSidebarPanelControls' ).removeClass( 'hidden' );
            	}
            } );
            
            // toggle sidebar panels
            $( 'body' ).on( 'click', '.reading-mode__view-sidebar-accordeon-panel-title', function() {
                var parentPanelId = $( this ).parent().attr( 'id' );
                var panelSessionStatus = JSON.parse( sessionStorage.getItem( 'rmPanelStatus' ) );
                
                // scroll sidebar to top
                $( '#readingModeViewSidebar' ).scrollTop( 0 );
                
                if ( $( this ).hasClass( 'in' ) ) {
                    $( this ).toggleClass( 'in' );
                    $( this ).next().slideToggle( 'fast' );
                    
                    panelSessionStatus[ parentPanelId ] = false;
                    sessionStorage.setItem( 'rmPanelStatus', JSON.stringify( panelSessionStatus ) );
                }
                else {                	
                    $( '.reading-mode__view-sidebar-accordeon-panel-title' ).each( function() {
                        $( this ).removeClass( 'in' );
                    } );
                    $( '.reading-mode__view-sidebar-accordeon-panel-body' ).each( function() {
                        $( this ).slideUp( 'fast' );
                    } );

                    $( this ).toggleClass( 'in' );
                    $( this ).next().slideToggle( 'fast' );
                    
                    for ( var status in panelSessionStatus ) {
                    	panelSessionStatus[ status ] = false;
                    }

                    panelSessionStatus[ parentPanelId ] = true;
                    sessionStorage.setItem( 'rmPanelStatus', JSON.stringify( panelSessionStatus ) );
                }
            } );

            // hide all panels
            $( 'body' ).on( 'click', '[data-close="all-tabs"]', function() {
            	var panelSessionStatus = JSON.parse( sessionStorage.getItem( 'rmPanelStatus' ) );
            	
            	$( '.reading-mode__view-sidebar-accordeon-panel-title' ).each( function() {
            		$( this ).removeClass( 'in' );
                    $( this ).next().slideUp( 'fast' );
            	} );
            	
            	for ( var status in panelSessionStatus ) {
                	panelSessionStatus[ status ] = false;
                }
            	
            	sessionStorage.setItem( 'rmPanelStatus', JSON.stringify( panelSessionStatus ) );
            } );

            // show all panels
            $( 'body' ).on( 'click', '[data-open="all-tabs"]', function() {
            	var panelSessionStatus = JSON.parse( sessionStorage.getItem( 'rmPanelStatus' ) );

            	$( '.reading-mode__view-sidebar-accordeon-panel-title' ).each( function() {
            		$( this ).addClass( 'in' );
            		$( this ).next().slideDown( 'fast' );
            	} );
            	
            	for ( var status in panelSessionStatus ) {
                	panelSessionStatus[ status ] = true;
                }
            	
            	sessionStorage.setItem( 'rmPanelStatus', JSON.stringify( panelSessionStatus ) );
            } );            
        },
    };
    
    /**
     * @description Method to set the sidebar status.
     * @method _setSidebarStatus
     * */
    function _setSidebarStatus() {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _setSidebarStatus' );
    	}
    	
    	var sidebarStatus;
    	
    	// set global variables
    	_sidebarWidth = $( '#readingModeViewSidebar' ).outerWidth();
    	
    	
    	if ( sessionStorage.getItem( 'rmSidebarStatus' ) == undefined || sessionStorage.getItem( 'rmSidebarStatus' ) == '' ) {
    		sessionStorage.setItem( 'rmSidebarStatus', true );
    	}
    	else {
    		sidebarStatus = sessionStorage.getItem( 'rmSidebarStatus' );
    		
    		if ( sidebarStatus === 'false'  ) {
    			// set sidebar left position
    			$( '#readingModeViewSidebar' ).css( {
    				'right': '-' + _sidebarWidth + 'px',
    				'left': 'inherit'
    			} );
    			
    			// hide panel controls
    			$( '#readingModeSidebarPanelControls' ).hide();
    			
    			// show sidebar open
    			$( '#viewSidebarOpen' ).show();
    			
    			// reset resizable
    			if ( window.matchMedia( '(min-width: 769px)' ).matches ) {
    				_unsetResizable( _defaults.resizeSelector );
    			}
    		}
    	}
    	
    	// show sidebar
    	$( '.reading-mode__view-sidebar-inner' ).show();
    }
    
    /**
     * @description Method to set the accordeon panel status.
     * @method _setPanelStatus
     * */
    function _setPanelStatus() {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _setPanelStatus' );
    	}
    	
    	var panelStatus;
    	
    	if ( sessionStorage.getItem( 'rmPanelStatus' ) == undefined || sessionStorage.getItem( 'rmPanelStatus' ) == '' ) {
    		panelStatus = {};
    		
    		// build panel status object
    		$( '.reading-mode__view-sidebar-accordeon-panel' ).each( function() {
    			var currId = $( this ).attr( 'id' );
    			
    			if ( !panelStatus.hasOwnProperty( currId ) ) {
    				// disable all panels
    				panelStatus[ currId ] = false;
    				
    				// enable first panel
		    		panelStatus[Object.keys(panelStatus)[0]] = true;
		    		
		    		// show active panels
		    		if ( panelStatus[ currId ] ) {
		    			$( this ).find( '.reading-mode__view-sidebar-accordeon-panel-title' ).addClass( 'in' );
		    			$( this ).find( '.reading-mode__view-sidebar-accordeon-panel-body' ).show();
		    		}    			        	
    			}
    			else {
    				return false;
    			}        	
    		} );
    		
    		// write object to session storage
    		sessionStorage.setItem( 'rmPanelStatus', JSON.stringify( panelStatus ) );    		
    	}
    	else {
    		panelStatus = JSON.parse( sessionStorage.getItem( 'rmPanelStatus' ) );
    		
    		$( '.reading-mode__view-sidebar-accordeon-panel' ).each( function() {
    			var currId = $( this ).attr( 'id' );
    			
    			// show active panels
    			if ( panelStatus[ currId ] ) {
    				$( this ).find( '.reading-mode__view-sidebar-accordeon-panel-title' ).addClass( 'in' );
    				$( this ).find( '.reading-mode__view-sidebar-accordeon-panel-body' ).show();
    			}    			        	
    		} );
    	}
    }

    /**
     * @description Method to initialize the resizable view.
     * @method _setResizable
     * @param {String} selector The selector of the element which should be resizable.
     * */
    function _setResizable( selector ) {
    	if ( _debug ) {
            console.log( 'EXECUTE: _setResizable' );
            console.log( 'selector: ', selector );
        }
    	
    	$( selector ).resizable({
    		handles: 'w',
    		minWidth: 500,
    		maxWidth: 900,
    		resize: function( event, ui ) {
    			if ( $( '.reading-mode__view-image-thumbs-wrapper' ).is( ':visible' ) ) {
    				setTimeout( function() {
    					$( '.reading-mode__view-image-thumbs-wrapper' ).outerWidth( $( '#readingModeView' ).outerWidth() - $( '#readingModeViewSidebar' ).outerWidth() );    					
    				}, 325 );
    			}
    		}
    	});
    }
    
    /**
     * @description Method to destroy the resizable view.
     * @method _unsetResizable
     * @param {String} selector The selector of the element which should be unset.
     * */
    function _unsetResizable( selector ) {
    	if ( _debug ) {
            console.log( 'EXECUTE: _unsetResizable' );
            console.log( 'selector: ', selector );
        }

    	$( selector ).resizable( 'destroy' );
    }

    /**
     * @description Method to switch the view to fullscreen.
     * @method _hideHeader
     * @param {Boolean} trigger A boolean which enables/disables the fadeout.
     * @param {Number} delay The delay when header is hiding. 
     * */
    function _hideHeader( trigger, delay ) {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _hideHeader' );
    	}
    	
    	var delay;
    	
    	if ( delay != undefined ) {
    		delay = delay;
    	}
    	else {
    		delay = 5000;
    	}
    	
    	if ( trigger ) {
    		if ( _fadeout ) {
    			clearTimeout( _fadeout );
    			$( '#readingModeHeader' ).show();
    		}
    		
    		_fadeout = setTimeout( function() {
    			$( '#readingModeHeader' ).fadeOut( '1000' );
    		}, delay );    		
    	}
    	else {
    		clearTimeout( _fadeout );
			$( '#readingModeHeader' ).show();
    	}
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );