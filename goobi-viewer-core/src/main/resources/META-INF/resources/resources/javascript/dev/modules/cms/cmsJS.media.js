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
 * @module cmsJS.media
 * @requires jQuery
 * @description Module which controls the media upload and editing for the cms.
 */
var cmsJS = ( function( cms ) {
    'use strict';
    
    // variables
    var _debug = false;
    var _defaults = {
    		allowEdit: true,
    		forceGrid: false,
    		isMediaSelectModal: false
    };
    var _adminCmsMediaGrid = '';
    
    cms.media = {
        /**
         * @description Method which initializes the media module.
         * @method init
         * @param {Object} config The config object.
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'cmsJS.media.init' );
                console.log( '##############################' );
                console.log( 'cmsJS.media.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );
            
            // init events
            this.initEventListeners();
            
            // check if media select modal is open
            this.checkMediaSelectModal( _defaults.isMediaSelectModal );
        },
        /**
         * @description Method to check if the media select modal exist.
         * @method checkMediaSelectModal
         * @param {Boolean} modal The status of the media select modal.
         * */
        checkMediaSelectModal: function( modal ) {
        	if ( _debug ) {
        		console.log( 'EXECUTE: checkMediaSelectModal' );
        		console.log( '--> modal: ', modal );
        	}
        	
        	if ( modal ) {
        		$( 'html' ).addClass( 'no-overflow' );
        	}
        	else {
        		$( 'html' ).removeClass( 'no-overflow' );            	
        	}        	
        },
        /**
         * @description Method to reload all event listeners.
         * @method onReload
         * @param {Object} data The ajax data object from jsf.
         * */
        onReload: function( data ) {
        	if ( _debug ) {
        		console.log( 'EXECUTE: onReload' );
        		console.log( '--> data: ', data );
        	}

        	if ( data && data.status == 'begin' ) {
            	cms.media.removeEventListeners();
            } 
            else if ( !data || data.status == 'success' ) {
            	cms.media.initEventListeners();
            }
        },
        /**
         * @description Method to initialize all events.
         * @method initEventListeners
         * */
        initEventListeners: function() {
            // select all media items
            $( '#selectAllMediaItems' ).on( 'change', function() {
            	if ( this.checked ) {
                    $( 'input[name*="selectMediaItem"]' ).each( function() {
                    	$( this ).prop( 'checked', true );
                    } );
                }
            	else {
            		$( 'input[name*="selectMediaItem"]' ).each( function() {
            			$( this ).prop( 'checked', false );
            		} );            		
            	}
            } );
            
            // reset bulk actions
            $( '#selectAllMediaItems, input[name*="selectMediaItem"]' ).on( 'change', function() {
            	$( '#bulkActionSelect' ).val( 'bulk' );
            	$( '[data-action="cancel"]' ).click();
            } );
            
            // bulk action edit
            $( '#bulkActionSelect' ).on( 'change', function() {
            	var action = $( this ).val();
            	
            	switch ( action ) {
            		case 'edit':
            			_bulkActionEdit();
            			break;
                    case 'delete':
                        viewerJS.notifications.confirm(
                            '',
                            undefined,
                            undefined,
                            _defaults.msg.bulkDeleteConfirm
                        )
                        .then( () => {
                            $( '#deleteSelectedItemsButton' ).click();
                        } )
                        .catch( () => {
                            $( '#selectAllMediaItems' ).prop( 'checked', false );
                            $( 'input[name*="selectMediaItem"]' ).each( function() {
                                $( this ).prop( 'checked', false );
                            } );
                            $( '#bulkActionSelect' ).val( 'bulk' );
                        } );
                        break;
                            }
                        } );
			
			// switch file view
			_adminCmsMediaGrid = sessionStorage.getItem( 'adminCmsMediaGrid' );
			
			if ( _defaults.forceGrid ) {
				$( '[data-switch="list"]' ).removeClass( 'active' );
				$( '.admin-cms-media__files' ).addClass( 'grid' );
			} 
			else if ( sessionStorage.getItem( 'adminCmsMediaGrid' ) == undefined || sessionStorage.getItem( 'adminCmsMediaGrid' ) == '' ) {
				sessionStorage.setItem( 'adminCmsMediaGrid', true );
				_adminCmsMediaGrid = sessionStorage.getItem( 'adminCmsMediaGrid' );
				_setMediaGridStatus( _adminCmsMediaGrid );
			}
			else {
				_setMediaGridStatus( _adminCmsMediaGrid );			
			}

			$( '[data-switch="list"]' ).on( 'click', function() {
				$( this ).addClass( 'active' );
				$( '[data-switch="grid"]' ).removeClass( 'active' );
				$( '.admin-cms-media__files' ).removeClass( 'grid' );
				$( '#selectAllMediaItems, #bulkActionSelect' ).prop( 'disabled', false ).removeClass( 'disabled' );
				sessionStorage.setItem( 'adminCmsMediaGrid', false );
			});
			$( '[data-switch="grid"]' ).on( 'click', function() {
				$( this ).addClass( 'active' );
				$( '[data-switch="list"]' ).removeClass( 'active' );
				$( '.admin-cms-media__files' ).addClass( 'grid' );
				$( '#selectAllMediaItems, #bulkActionSelect' ).prop( 'disabled', true ).addClass( 'disabled' );
				sessionStorage.setItem( 'adminCmsMediaGrid', true );
			});
			
            // show/hide edit actions for media file
            _setFileCancelClick( false );
            
            $( '[data-action="edit"]' ).on( 'click', function() {
            	_toggleEditMode( $( this ).parents( '.admin-cms-media__file' ), false );
            	$( this ).parent().removeClass( 'in');
            	$( this ).parent().next().addClass('in');
			});
			
			// enlarge file
            if( _defaults.allowEdit ) {
				$( '.admin-cms-media__file-image, .admin-cms-media__file-close' ).on( 'click', function() {
					var $thisFile = $( this ).parents( '.admin-cms-media__file' ); 
					
					// show modal
					$thisFile.toggleClass( 'fixed' );
					$( '.admin-cms-media__overlay' ).toggle();
	
					// set modal events				
					if ( $thisFile.hasClass( 'fixed' ) ) {
						_setFileCancelClick( true );
						_setFileKeyEvents( true );
						_toggleEditMode( $( '.admin-cms-media__file' ), true );					
					}
					else {
						_setFileCancelClick( false );
						_setFileKeyEvents( false );
						$( '[data-action="cancel"]' ).click();
					}
				} );
            }
			
			// navigate through overlays
			$( '.admin-cms-media__file' ).first().find( '.admin-cms-media__file-prev' ).addClass( 'disabled' );
			$( '.admin-cms-media__file' ).last().find( '.admin-cms-media__file-next' ).addClass( 'disabled' );
			
			$( '.admin-cms-media__file-next' ).on( 'click', function() {
				if ( $( this ).parent().is( '.admin-cms-media__file:last' ) ) {
					$( this ).addClass( 'disabled' );
					return false;
				}
				else {
					$( this ).removeClass( 'disabled' );
					$( this ).parent().removeClass( 'fixed' ).next().addClass( 'fixed' );
				}
			} );
			$( '.admin-cms-media__file-prev' ).on( 'click', function() {
				if ( $( this ).parent().is( '.admin-cms-media__file:first' ) ) {
					$( this ).addClass( 'disabled' );
					return false;
				}
				else {
					$( this ).removeClass( 'disabled' );
					$( this ).parent().removeClass( 'fixed' ).prev().addClass( 'fixed' );
				}
			} );
        },
        /**
         * @description Method to remove all events.
         * @method removeEventListeners
         * */
        removeEventListeners: function() {
            // select all media items
            $( '#selectAllMediaItems' ).off();
            
            // reset bulk actions
            $( '#selectAllMediaItems, input[name*="selectMediaItem"]' ).off();
            
            // bulk action edit
            $( '#bulkActionSelect' ).off();
			
			// switch file view
			_adminCmsMediaGrid = sessionStorage.getItem( 'adminCmsMediaGrid' );

			$( '[data-switch="list"]' ).off();
			$( '[data-switch="grid"]' ).off();            
            $( '[data-action="edit"]' ).off();
			
			// enlarge file
			$( '.admin-cms-media__file-image, .admin-cms-media__file-close' ).off();
			
			// navigate through overlays			
			$( '.admin-cms-media__file-next' ).off();
			$( '.admin-cms-media__file-prev' ).off();			
			$( '.admin-cms-media__file' ).off( 'mouseover' ).off( 'mouseout' ).off( 'keydown' );	
			$( '[data-action="cancel"]' ).off();
        }
    };
    
    /**
     * @description Method to set the status of the media item grid and the switches.
     * @method _setMediaGridStatus
     * @param {String} status The status of the media item grid.
     * */
    function _setMediaGridStatus( status ) {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _setMediaGridStatus' );
    		console.log( '--> status: ', status );
    	}
    	
    	if ( status === 'true' ) {
			$( '.admin-cms-media__files' ).addClass( 'grid' );
			$( '[data-switch="list"]' ).removeClass( 'active' );
			$( '[data-switch="grid"]' ).addClass( 'active' );
			$( '#selectAllMediaItems, #bulkActionSelect' ).prop( 'disabled', true ).addClass( 'disabled' );
		}
		else {
			$( '.admin-cms-media__files' ).removeClass( 'grid' );					
			$( '[data-switch="list"]' ).addClass( 'active' );
			$( '[data-switch="grid"]' ).removeClass( 'active' );
			$( '#selectAllMediaItems, #bulkActionSelect' ).prop( 'disabled', false ).removeClass( 'disabled' );
		}
    }

    /**
     * @description Method to set multiple media items to edit mode.
     * @method _bulkActionEdit
     * */
    function _bulkActionEdit() {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _bulkActionEdit' );
    	}
    	
    	$( '.admin-cms-media__file' ).each( function() {
    		var $file = $( this ),
    		    $cbStatus = $file.find( 'input[name*="selectMediaItem"]' ).prop( 'checked' ),
    		    $editButton = $file.find( '[data-action="edit"]' );
    		
    		if ( $cbStatus ) {
    			$editButton.click();
    		}
    	} );
    }
    
    /**
     * @description Method to switch the file edit mode.
     * @method _toggleEditMode
     * @param {Object} item A jQuery object which contains the current media file.
     * @param {Boolean} modal Trigger to switch between modal and list files.
     * */
    function _toggleEditMode( item, modal ) {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _toggleEditMode' );
    		console.log( '--> item: ', item );
    		console.log( '--> modal: ', modal );
    	}
    	
    	if ( modal ) {
    		item.each( function() {
    			$( this ).find( '.admin-cms-media__file-metadata-view' ).removeClass( 'in');
    			$( this ).find( '.admin-cms-media__file-metadata-edit' ).addClass( 'in');
    			$( this ).find( '.admin-cms-media__file-actions-view' ).removeClass( 'in' );
    			$( this ).find( '.admin-cms-media__file-actions-edit' ).addClass( 'in' );
    		} );
    	}
    	else {
    		item.find( '.admin-cms-media__file-metadata-view' ).removeClass( 'in');
    		item.find( '.admin-cms-media__file-metadata-edit' ).addClass( 'in');
    	}
    	
    }
    
    /**
     * @description Method to set the keyboard events for the media file modal.
     * @method _setFileKeyEvents
     * @param {Boolean} modal Trigger to switch between modal and list files.
     * */
    function _setFileKeyEvents( modal ) {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _setFileKeyEvents' );
    		console.log( '--> modal: ', modal );
    	}
    	
    	if ( modal ) {
    		$( '.admin-cms-media__file' ).off().on( 'keydown', function( e ) {
    			var code = e.keyCode || e.which;
    			var isInput = e.target.tagName.toLowerCase().match( /input|select|textarea/ );
    			
    			if ( isInput ) {
    				return;
    			}
    			else {
    				switch ( code ) {
    				// esc
    				case 27:
    					$( this ).find( '[data-action="cancel"]' ).click();
    					$( '.admin-cms-media__file' ).off( 'keydown' );
    					break;
    					// left
    				case 37:
    					if ( $( this ).is( '.admin-cms-media__file:first' ) ) {
    						return false;
    					}
    					else {
    						$( this ).removeClass( 'fixed' ).prev().addClass( 'fixed' ).focus();	    					
    					}	    				
    					break;
    					// right
    				case 39:
    					if ( $( this ).is( '.admin-cms-media__file:last' ) ) {
    						return false;
    					}
    					else {
    						$( this ).removeClass( 'fixed' ).next().addClass( 'fixed' ).focus();
    					}	    				
    					break;
    				}    				
    			}
    		} );
    	}
    	else {
    		$( '.admin-cms-media__file' ).off( 'keydown' );		
    	}
    }
    
    /**
     * @description Method to set the click event for media file cancel button.
     * @method _setFileCancelClick
     * @param {Boolean} modal Trigger to switch between modal and list files.
     * */
    function _setFileCancelClick( modal ) {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _setFileCancelClick' );
    		console.log( '--> modal: ', modal );
    	}
    	
    	if ( modal ) {
    		$( '[data-action="cancel"]' ).off().on( 'click', function() {
				$( this ).parents( '.admin-cms-media__file' ).toggleClass( 'fixed' );
				$( '.admin-cms-media__overlay' ).toggle();
				_setFileCancelClick( false );
				$( '[data-action="cancel"]' ).click();
			});
    	}
    	else {
    		$( '[data-action="cancel"]' ).off().on( 'click', function() {
				$( this ).parents( '.admin-cms-media__file' ).find( '.admin-cms-media__file-metadata-view' ).addClass( 'in');
				$( this ).parents( '.admin-cms-media__file' ).find( '.admin-cms-media__file-metadata-edit' ).removeClass( 'in');
				$( this ).parent().removeClass( 'in' );
				$( this ).parent().prev().addClass( 'in' );
			});
    	}
    }
    
    return cms;
    
} )( cmsJS || {}, jQuery );
