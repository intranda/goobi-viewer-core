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
 * Module which handles the fullscreen mode.
 * 
 * @version 3.4.0
 * @module viewerJS.fullscreen
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false;
    var _fadeout;
    var _sidebarWidth;
    var _sidebarLeft;
    var _sidebarId;
    var _lastKeyCode;
    var _lastKeyPress;
    var _maxDoubleClickDelay = 250 //ms
    var _defaults = {
    	resizeSelector: '#fullscreenViewSidebar',
    	openPanel: "panel-1",
    	sidebarOpen: true,
    	msg: {}
    };
    
    viewer.fullscreen = {
        /**
         * Method to initialize the viewer fullscreen.
         * 
         * @method init
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.fullscreen.init' );
                console.log( '##############################' );
                console.log( 'viewer.fullscreen.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );

            // hide header
            _hideHeader( true, 5000 );
            
            // display header on mousemove/touchmove
            $( '#fullscreenViewImage, #fullscreenViewSidebar' ).on( 'mousemove', function() {
            	_hideHeader( true );
            } );
            $( '#fullscreenViewSidebar' ).on( 'click', function() {
            	_hideHeader( false );
            } );
            $( '#fullscreenHeader' ).on( 'mousemove', function() {
            	_hideHeader( false );
            } );
            $( window ).on( 'touchstart touchend touchcancel touchmove', function() {
            	_hideHeader( true, 1000 );
            } );
            $( '#fullscreenViewImage, #fullscreenViewSidebar' ).on( 'touchstart touchend touchcancel touchmove', function() {
                _hideHeader( true, 1000 );
            } );
            
            // make elements resizable
            if ( window.matchMedia( '(min-width: 769px)' ).matches ) {
            	_setResizable( _defaults.resizeSelector );
            	
                // set position on resize/orientationchange
                $( window ).on( 'resize', function(e) {
                    _sidebarId = $( '#fullscreenViewSidebar' ).attr( 'id' );
                    
                    // check if sidebar is resizing
                    if ( e.target[ 'id' ] != _sidebarId ) {
                        $( 'body' ).hide();
                        window.location.href = window.location.href;                    
                    }
                } );
            } 
            else {
                // set position on resize/orientationchange
                $( window ).on( 'orientationchange', function(e) {
                    $( 'body' ).hide();                    
                    window.location.href = window.location.href;                    
                } );   
            }
            
            // set sidebar panel, status and width
            _getSidebarWidth();
            _setPanelStatus();
            _setSidebarStatus();
            _getSidebarScrollPosition();
            _hideEmptyPanels();

            // toggle sidebar
            $( '[data-close="fs-sidebar"]' ).on( 'click', function() {
            	// set global variables
            	_sidebarWidth = $( '#fullscreenViewSidebar' ).outerWidth();
            	_sidebarLeft = $( '#fullscreenViewSidebar' ).css( 'left' );
            	// save sidebar width
            	_setSidebarWidth( _sidebarWidth );
            	
            	// set sidebar left position
            	$( '#fullscreenViewSidebar' ).css( 'left', 'inherit' );

            	
            	// reset resizable
            	if ( window.matchMedia( '(min-width: 769px)' ).matches) {
            		_unsetResizable( _defaults.resizeSelector );
            	}

            	// hide panel controls
            	$( '#fullscreenSidebarPanelControls' ).hide();
            	
            	// slide out sidebar
            	$( '#fullscreenViewSidebar' ).animate( {
            		right: '-' + _sidebarWidth + 'px'
            	}, 300, function() {            		

            		// show sidebar open
                let sidebarOpenDiv = $( '#viewSidebarOpen' );
                let sidebarOpenBtn = sidebarOpenDiv[0].querySelector('[data-open="fs-sidebar"]');
            		sidebarOpenDiv.addClass( 'in' );

                // ACCESSIBILITY
                // make btn focusable 
                sidebarOpenBtn.setAttribute('tabindex', '0');
                // add display:none
                // prevents focusable elements in the sidebar from beeing focused using the tab key
            	  $( '#fullscreenViewSidebar' ).css( 'display', 'none' );

            		// show back and forward on small devices
                	if ( window.matchMedia( '(max-width: 480px)' ).matches ) {
                		$( '.image-controls__action.prev, .image-controls__action.next' ).show();
                	}

            		// save sidebar status
            		sessionStorage.setItem( 'fsSidebarStatus', false );
            	} );

            	
            	if ( $( '.fullscreen__view-image-thumbs-wrapper' ).is( ':visible' ) ) {
            		$( '.fullscreen__view-image-thumbs-wrapper' ).animate( {
            			width: '100%'
            		}, 300 );
    			}
            } );

            // Open Sidebar
            $( '[data-open="fs-sidebar"]' ).on( 'click', function() {
                let sidebarOpenDiv = $( '#viewSidebarOpen' );
                let sidebarOpenBtn = sidebarOpenDiv[0].querySelector('[data-open="fs-sidebar"]');

                // hide sidebar open
                sidebarOpenDiv.removeClass( 'in' );

                // ACCESIBILITY
                // prevent hidden btn from being focusable
                sidebarOpenBtn.setAttribute('tabindex', '-1');
            	
            	// show back and forward on small devices
            	if ( window.matchMedia( '(max-width: 480px)' ).matches ) {
            		$( '.image-controls__action.prev, .image-controls__action.next' ).hide();
            	}
            	
              // Remove display:none => make slide in animation possible (see below)
              // Originally set so focusable elements in the closed sidebar cannot be reached using the tab key 
              $( '#fullscreenViewSidebar' ).css( 'display', '' );

            	// slide in sidebar
            	$( '#fullscreenViewSidebar' ).animate( {
            		right: 0
            	}, 300, function() {

            		// set sidebar left position
            		$( '#fullscreenViewSidebar' ).css( 'left', _sidebarLeft );
            		
            		// show panel controls
            		$( '#fullscreenSidebarPanelControls' ).show();

            		// set resizable
            		if ( window.matchMedia( '(min-width: 769px)' ).matches ) {
            			_setResizable( _defaults.resizeSelector );
            		}
            		
            		// save sidebar status
            		sessionStorage.setItem( 'fsSidebarStatus', true );

            	} );
            	
            	if ( $( '.fullscreen__view-image-thumbs-wrapper' ).is( ':visible' ) ) {
            		$( '.fullscreen__view-image-thumbs-wrapper' ).animate( {
            			width: $( '#fullscreenView' ).outerWidth() - _sidebarWidth
            		}, 300 );
    			}
            } );
            
            // scroll resize handle and hide panel controls
            $( '#fullscreenViewSidebar' ).on( 'scroll', function() {
            	var currScrollPosition = $( this ).scrollTop();
            	
            	$( '.ui-resizable-handle' ).css( 'top', $( this ).scrollTop() );
            	
            	_setSidebarScrollPosition( currScrollPosition );
            	
            	if ( $( this ).scrollTop() > 0 ) {
            		$( '#fullscreenSidebarPanelControls' ).addClass( 'hidden' );
            	}
            	else {
            		$( '#fullscreenSidebarPanelControls' ).removeClass( 'hidden' );
            	}
            } );
            
            // toggle sidebar panels
            $( '.fullscreen__view-sidebar-accordeon-panel-title' ).on( 'click keydown', function(e) {

              if((e.type == 'keydown' && e.key == 'Enter') || e.type == 'click') {

                var parentPanelId = $( this ).parent().attr( 'id' );
                var panelSessionStatus = JSON.parse( sessionStorage.getItem( 'fsPanelStatus' ) );
                // scroll sidebar to top
                $( '#fullscreenViewSidebar' ).scrollTop( 0 );
                
                if ( $( this ).hasClass( 'in' ) ) {
                    $( this ).toggleClass( 'in' );
                    $( this ).next().slideToggle( 'fast' );
                    panelSessionStatus[ parentPanelId ] = false;
                    sessionStorage.setItem( 'fsPanelStatus', JSON.stringify( panelSessionStatus ) );
                }
                else {                	
                    $( '.fullscreen__view-sidebar-accordeon-panel-title' ).each( function() {
                        $( this ).removeClass( 'in' );
                    } );
                    $( '.fullscreen__view-sidebar-accordeon-panel-body' ).each( function() {
                        $( this ).slideUp( 'fast' );
                    } );

                    $( this ).toggleClass( 'in' );
                    $( this ).next().slideToggle( 'fast' );
//                    viewer.GeoMap.allMaps.forEach(geomap => {
//                    	geomap.map.invalidateSize();
//                    	geomap.layers.find(layer => !layer.isEmpty())?.setViewToFeatures(true);
//                    });
                    for ( var status in panelSessionStatus ) {
                        if(status.startsWith("panel-")) {                       
                            panelSessionStatus[ status ] = false;
                        }
                    }

                    panelSessionStatus[ parentPanelId ] = true;
                    sessionStorage.setItem( 'fsPanelStatus', JSON.stringify( panelSessionStatus ) );
                }

              }

            } );

            // hide all panels
            $( '[data-close="all-tabs"]' ).on( 'click', function() {
            	var panelSessionStatus = JSON.parse( sessionStorage.getItem( 'fsPanelStatus' ) );
            	
            	$( '.fullscreen__view-sidebar-accordeon-panel-title' ).each( function() {
            		$( this ).removeClass( 'in' );
                    $( this ).next().slideUp( 'fast' );
            	} );
            	for ( var status in panelSessionStatus ) {
            	    if(status.startsWith("panel-")) {            	        
            	        panelSessionStatus[ status ] = false;
            	    }
                }
            	
            	sessionStorage.setItem( 'fsPanelStatus', JSON.stringify( panelSessionStatus ) );
            } );

            // show all panels
            $( '[data-open="all-tabs"]' ).on( 'click', function() {
            	var panelSessionStatus = JSON.parse( sessionStorage.getItem( 'fsPanelStatus' ) );

            	$( '.fullscreen__view-sidebar-accordeon-panel-title' ).each( function() {
            		$( this ).addClass( 'in' );
            		$( this ).next().slideDown( 'fast' );
            	} );
            	
            	for ( var status in panelSessionStatus ) {
                    if(status.startsWith("panel-")) {                       
                        panelSessionStatus[ status ] = true;
                    }
                }
            	
            	sessionStorage.setItem( 'fsPanelStatus', JSON.stringify( panelSessionStatus ) );
            } ); 
            
            $(document.body).off( 'keyup' , _handleKeypress);
            $(document.body).on( 'keyup' , _handleKeypress);
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
    	
    	let sidebarOpen = sessionStorage.getItem( 'fsSidebarStatus' );
    	if(sidebarOpen == undefined) {
    	    sidebarOpen = _defaults.sidebarOpen;
    	    sessionStorage.setItem( 'fsSidebarStatus', sidebarOpen );
    	} else {    	    
    	    sidebarOpen = (sidebarOpen == 'true');
    	}

    	if(sidebarOpen) {
    		if ( window.matchMedia( '(max-width: 480px)' ).matches ) {
    			// hide sidebar
    			_hideSidebar( $( '#fullscreenViewSidebar' ).outerWidth() );
				// always set fssidebarstatus to false for very small screens (this is needed for the thumbnail view size calculation)
    			sessionStorage.setItem( 'fsSidebarStatus', false );    
    		} else {
    			sessionStorage.setItem( 'fsSidebarStatus', true );    
    		}
    	} else {
    			// hide sidebar
    			_hideSidebar( $( '#fullscreenViewSidebar' ).outerWidth() );
    			// reset resizable
    			if ( window.matchMedia( '(min-width: 769px)' ).matches ) {
    				_unsetResizable( _defaults.resizeSelector );
    			}
    	}
    	
    	// show sidebar
    	$( '.fullscreen__view-sidebar-inner' ).show();
    }
    
    function _hideEmptyPanels() {
        document.querySelectorAll(".fullscreen__view-sidebar-accordeon-panel").forEach(panel => {
            let childCount = [...panel.querySelectorAll(".fullscreen__view-sidebar-accordeon-panel-body")].flatMap(body => body.children).map(child => child.length).reduce((acc, v) => acc+v );
            if(childCount < 1) {
                panel.classList.add("d-none");
            }
          })
    }

    /**
     * @description Method to set the sidebar scroll position.
     * @method _setSidebarScrollPosition
     * */
    function _setSidebarScrollPosition( scroll ) {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _setSidebarScrollPosition' );
    		console.log( '--> scroll: ', scroll );
    	}
    	
    	sessionStorage.setItem( 'fsSidebarScrollPosition', scroll );
    }
    
    /**
     * @description Method to get the sidebar scroll position.
     * @method _getSidebarScrollPosition
     * */
    function _getSidebarScrollPosition() {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _getSidebarScrollPosition' );
    	}
    	
    	var pos = sessionStorage.getItem( 'fsSidebarScrollPosition' );
    	
    	// check if session storage value exists
    	if ( pos == undefined || pos === null ) {
    		sessionStorage.setItem( 'fsSidebarScrollPosition', 0 );
    		pos = sessionStorage.getItem( 'fsSidebarScrollPosition' );    		
    	}

    	// set sidebar scroll position
    	$( '.ui-resizable-handle' ).css( 'top', pos );
    	
    	if ( pos > 0 ) {
    		$( '#fullscreenSidebarPanelControls' ).addClass( 'hidden' );
    	}
    	else {
    		$( '#fullscreenSidebarPanelControls' ).removeClass( 'hidden' );
    	}
    	
    	$( '#fullscreenViewSidebar' ).scrollTop( pos );
    }
    
    /**
     * @description Method which hides the sidebar.
     * @method _hideSidebar
     * @param {Number} width The current sidebar width.
     * */
    function _hideSidebar( width ) {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _hideSidebar' );
    		console.log( 'width: ', width );
    	}
    	
      // set sidebar left position
      $( '#fullscreenViewSidebar' ).css( {
        'right': '-' + width + 'px',
        'left': 'inherit'
      } );
      
      // hide panel controls
      $( '#fullscreenSidebarPanelControls' ).hide();
      
      // show sidebar open
      let sidebarOpenDiv = $( '#viewSidebarOpen' );
      let sidebarOpenBtn = sidebarOpenDiv[0].querySelector('[data-open="fs-sidebar"]');
      sidebarOpenDiv.addClass( 'in' );

      // ACCESSIBILITY
      // prevent hidden btn from being focused
      sidebarOpenBtn.setAttribute('tabindex', '-1');
    }
    
    /**
     * @description Method to set the accordeon panel status.
     * @method _setPanelStatus
     * */
    function _setPanelStatus() {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _setPanelStatus' );
    	}
    	            
        let openPanelFromUrl = _getPanelStatusFromUrlParameter();
        if(_debug)console.log("Open panel url query param: " + openPanelFromUrl);
            
    	
    	var panelStatus;
    	var fsPanelStatus = sessionStorage.getItem( 'fsPanelStatus' );
    	if(fsPanelStatus) {
    	    panelStatus = JSON.parse(fsPanelStatus);
    	    if(panelStatus.persistentIdentifier !== _defaults.persistentIdentifier) {
    	        panelStatus = undefined;
    	    }
    	} else {
    	    panelStatus = undefined;
    	}
        
    	if ( !panelStatus ) {
    		panelStatus = {};
    		panelStatus.persistentIdentifier = _defaults.persistentIdentifier;
    		let openPanel = openPanelFromUrl ? openPanelFromUrl : _defaults.openPanel;
    		// build panel status object
    		$( '.fullscreen__view-sidebar-accordeon-panel' ).each( function() {
    			var currId = $( this ).attr( 'id' );
    			
    			if ( !panelStatus.hasOwnProperty( currId ) ) {
    				// disable all panels
    				if(openPanel == currId) {
    					panelStatus[ currId ] = true;
    				} else {    					
    					panelStatus[ currId ] = false;
    				}
    				
    				// enable first panel
    				// panelStatus[Object.keys(panelStatus)[_defaults.openPanel]] = true;
		    		
		    		// show active panels
		    		if ( panelStatus[ currId ] ) {
		    			$( this ).find( '.fullscreen__view-sidebar-accordeon-panel-title' ).addClass( 'in' );
		    			$( this ).find( '.fullscreen__view-sidebar-accordeon-panel-body' ).show();
		    		}    			        	
    			}
    			else {
    				return false;
    			}        	
    		} );
		
    	}
    	else {
    		
    		if( _debug ) {
    			console.log( '--> panelStatus: ', panelStatus );
    		}
    		
    		
    		$( '.fullscreen__view-sidebar-accordeon-panel' ).each( function() {
    			var currId = $( this ).attr( 'id' );

				//set active panel status from url query param    			
	    		if(openPanelFromUrl) {
	    			if(currId == openPanelFromUrl) {
	    				panelStatus[ currId ] = true;
	    			} else {
	    				panelStatus[ currId ] = false;
	    			}
	    		} 

    			// show active panels
	    		if ( panelStatus[ currId ] ) {
    				$( this ).find( '.fullscreen__view-sidebar-accordeon-panel-title' ).addClass( 'in' );
					$( this ).find( '.fullscreen__view-sidebar-accordeon-panel-body' ).show();
    			}    			        	
    		} );    		
    	}
    	    		
		// write object to session storage  
		sessionStorage.setItem( 'fsPanelStatus', JSON.stringify( panelStatus ) );    
    } 

	function _getPanelStatusFromUrlParameter() {
		
		let activetab = viewerJS.helper.getUrlSearchParam("activetab");
		if(activetab) {
			if(_debug)console.log("Set active tab from query param ", activetab);
			//find panel-id
			let tabId = $(".fs-" + activetab).attr("id");
			return tabId;
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
    			_setSidebarWidth( $( '#fullscreenViewSidebar' ).outerWidth() );
    			
    			if ( $( '.fullscreen__view-image-thumbs-wrapper' ).is( ':visible' ) ) {
    				setTimeout( function() {
    					$( '.fullscreen__view-image-thumbs-wrapper' ).outerWidth( $( '#fullscreenView' ).outerWidth() - $( '#fullscreenViewSidebar' ).outerWidth() );    					
    				}, 325 );
    			}
    		}
    	});
    }
    
    /**
     * @description Method to save the sidebar width to session storage.
     * @method _setSidebarWidth
     * @param {Number} width The current width of the sidebar.
     * */
    function _setSidebarWidth( width ) {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _setSidebarWidth' );
    		console.log( '--> width: ', width );
    	}
    	
    	sessionStorage.setItem( 'fsSidebarWidth', width );
    }

    /**
     * @description Method to get the sidebar scroll position.
     * @method _getSidebarWidth
     * */
    function _getSidebarWidth() {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _getSidebarWidth' );
    	}
    	
    	var sbWidth = sessionStorage.getItem( 'fsSidebarWidth' );
    	
    	// check if session storage value exists
    	if ( sbWidth == undefined || sbWidth === null ) {
    		sessionStorage.setItem( 'fsSidebarWidth', $( '#fullscreenViewSidebar' ).outerWidth() );
    		sbWidth = sessionStorage.getItem( 'fsSidebarWidth' );    		
    	}

    	// set sidebar width
    	if ( window.matchMedia( '(min-width: 769px)' ).matches ) {
    		$( '#fullscreenViewSidebar' ).css( 'width', sbWidth + 'px' );
    	}
    }
    
    /**
     * @description Method to dispose the resizable view.
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

      // Check if keyboard is used to navigate the page
      var usingKeyboard = false;
      if(document.body.classList.contains('using-keyboard')) {
        usingKeyboard = true;
      }
    	
    	if ( trigger && !usingKeyboard) {
    		if ( _fadeout ) {
    			clearTimeout( _fadeout );
    			$( '#fullscreenHeader' ).show();
    		}
    		
    		_fadeout = setTimeout( function() {
    			$( '#fullscreenHeader' ).fadeOut( '1000' );
    		}, delay );    		
    	}
    	else {
    		clearTimeout( _fadeout );
			$( '#fullscreenHeader' ).show();
    	}
    }
   
    /**
     * @description Method to set key events to navigate through images, and exit fullscreen mode.
     * @method _handleKeypress
     * @param {Object} event An event object to trigger key events. 
     * */
    function _handleKeypress( event ) {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _handleKeypress' );
    		console.log( '--> event: ', event );
    	}
    	
        if (event.originalEvent) {
            event = event.originalEvent;
        }

        // don't handle if the actual target is an input field
        if (event.target.tagName.toLowerCase().match(/input|textarea/)) {
            return true;
        }

        var keyCode = event.keyCode;
        var now = Date.now();

        // this is a double key press if the last entered keycode is the same as the current one and the last key press is less than maxDoubleClickDelay ago
        var doubleKeypress = (_lastKeyCode == keyCode && now - _lastKeyPress <= _maxDoubleClickDelay);
        _lastKeyCode = keyCode;
        _lastKeyPress = now;

        if (_debug) {
            console.log('key pressed ', keyCode);
            if (doubleKeypress) {
                console.log('double key press');
            }
        }

        switch (keyCode) {
            case 37:
                if (doubleKeypress && $('.image-controls__action.start a').length) {
                    $('.image-controls__action.start a').get(0).click();
                }
                else if ($('.image-controls__action.prev a').length) {
                    $('.image-controls__action.prev a').get(0).click();
                }
                break;
            case 39:
                // jump to last image, if right arrow key was pressed twice
                if (doubleKeypress && $('.image-controls__action.end a').length) {
                    $('.image-controls__action.end a').get(0).click();
                }
                // advance one image at a time, if right arrow key is pressed once
                else if ($('.image-controls__action.next a').length) {
                    $('.image-controls__action.next a').get(0).click();
                }
                break;
            case 27:
                // exit fullscreen on escape
                if ($('[data-js="exit-fullscreen"]').length && document.readyState == 'complete') {
                    $('[data-js="exit-fullscreen"]').get(0).click();
                }
        };
    }   
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
