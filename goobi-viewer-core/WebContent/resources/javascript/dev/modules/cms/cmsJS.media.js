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
    var _debug = true;
    var _defaults = {};
    var _adminCmsMediaGrid = '';
    
    cms.media = {
        /**
         * @description Method which initializes the medie module.
         * @method init
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'cmsJS.media.init' );
                console.log( '##############################' );
                console.log( 'cmsJS.media.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );
            
            // select all media items
            $( '#selectAllMediaItems' ).on( 'change', function() {
            	if ( this.checked ) {
                    $( 'input[name="selectMediaItem"]' ).each( function() {
                    	$( this ).prop( 'checked', true );
                    } );
                }
            	else {
            		$( 'input[name="selectMediaItem"]' ).each( function() {
            			$( this ).prop( 'checked', false );
            		} );            		
            	}
            } );
            
            // reset bulk actions
            $( '#selectAllMediaItems, input[name="selectMediaItem"]' ).on( 'change', function() {
            	$( '#bulkActionSelect' ).val( 'bulk' );
            } );
            
            // bulk action edit
            $( '#bulkActionSelect' ).on( 'change', function() {
            	var action = $( this ).val();
            	
            	switch ( action ) {
            		case 'edit':
            			_bulkActionEdit();
            			break;
            		case 'delete':
            			return false;            			
            			break;
            	}
            } );
			
			// switch file view
			_adminCmsMediaGrid = localStorage.getItem( 'adminCmsMediaGrid' );
			
			if ( localStorage.getItem( 'adminCmsMediaGrid' ) == undefined || localStorage.getItem( 'adminCmsMediaGrid' ) == '' ) {
				localStorage.setItem( 'adminCmsMediaGrid', false );
				_adminCmsMediaGrid = localStorage.getItem( 'adminCmsMediaGrid' );
				_setMediaGridStatus( _adminCmsMediaGrid );
			}
			else {
				_setMediaGridStatus( _adminCmsMediaGrid );			
			}

			$( '[data-switch="list"]' ).on( 'click', function() {
				$( this ).addClass( 'active' );
				$( '[data-switch="grid"]' ).removeClass( 'active' );
				$( '.admin-cms-media__files' ).removeClass( 'grid' );
				localStorage.setItem( 'adminCmsMediaGrid', false );
			});
			$( '[data-switch="grid"]' ).on( 'click', function() {
				$( this ).addClass( 'active' );
				$( '[data-switch="list"]' ).removeClass( 'active' );
				$( '.admin-cms-media__files' ).addClass( 'grid' );
				localStorage.setItem( 'adminCmsMediaGrid', true );
			});
			
            // show/hide edit actions for media file
            $( '.admin-cms-media__file' ).on( 'mouseover', function() {
				$( this ).find( '.admin-cms-media__file-actions' ).show();
			});
        	$( '.admin-cms-media__file') .on( 'mouseout', function() {
				$( this ).find( '.admin-cms-media__file-actions' ).hide();
			});
        	$( '[data-action="edit"]' ).on( 'click', function() {
				$( this ).parents( '.admin-cms-media__file' ).find( '.admin-cms-media__file-metadata-view' ).removeClass( 'in');
				$( this ).parents( '.admin-cms-media__file' ).find( '.admin-cms-media__file-metadata-edit' ).addClass( 'in');
				$( this ).parent().removeClass( 'in');
				$( this ).parent().next().addClass('in');
			});
			$( '[data-action="cancel"]' ).on( 'click', function() {
				$( this ).parents( '.admin-cms-media__file' ).find( '.admin-cms-media__file-metadata-view' ).addClass( 'in');
				$( this ).parents( '.admin-cms-media__file' ).find( '.admin-cms-media__file-metadata-edit' ).removeClass( 'in');
				$( this ).parent().removeClass( 'in' );
				$( this ).parent().prev().addClass( 'in' );
			});			
			
			// enlarge file
			$( '.admin-cms-media__file-image, .admin-cms-media__file-close' ).on( 'click', function() {
				$( this ).parents( '.admin-cms-media__file' ).toggleClass( 'fixed' );
				$( '.admin-cms-media__overlay' ).toggle();
			} );
			
			// navigate through overlays
			$( '.admin-cms-media__file' ).first().find( '.admin-cms-media__file-prev' ).addClass( 'disabled' );
			$( '.admin-cms-media__file' ).last().find( '.admin-cms-media__file-next' ).addClass( 'disabled' );
			
			$( '.admin-cms-media__file-next' ).on( 'click', function() {
				if ( $( this ).parent().next().is( ':last-child' ) ) {
					$( this ).addClass( 'disabled' );
					return false;
				}
				else {
					$( this ).removeClass( 'disabled' );
					$( this ).parent().removeClass( 'fixed' ).next().addClass( 'fixed' );
				}
			} );
			$( '.admin-cms-media__file-prev' ).on( 'click', function() {
				if ( $( this ).parent().prev().is( ':first-child' ) ) {
					$( this ).addClass( 'disabled' );
					return false;
				}
				else {
					$( this ).removeClass( 'disabled' );
					$( this ).parent().removeClass( 'fixed' ).prev().addClass( 'fixed' );
				}
			} );
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
		}
		else {
			$( '.admin-cms-media__files' ).removeClass( 'grid' );					
			$( '[data-switch="list"]' ).addClass( 'active' );
			$( '[data-switch="grid"]' ).removeClass( 'active' );
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
    		    $cbStatus = $file.find( 'input[name="selectMediaItem"]' ).prop( 'checked' ),
    		    $editButton = $file.find( '[data-action="edit"]' );
    		
    		if ( $cbStatus ) {
    			$editButton.click();
    		}
    	} );
    }
    
    return cms;
    
} )( cmsJS || {}, jQuery );
