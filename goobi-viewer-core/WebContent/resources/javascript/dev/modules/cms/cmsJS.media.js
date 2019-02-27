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
    
    cms.media = {
        /**
         * @method init
         * @description Method which initializes the medie module.
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'cmsJS.media.init' );
                console.log( '##############################' );
                console.log( 'cmsJS.media.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );
            
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
			
			// switch file view
			$( '[data-switch="list"]' ).on( 'click', function() {
				$( '.admin-cms-media__files' ).removeClass( 'grid' );
			});
			$( '[data-switch="grid"]' ).on( 'click', function() {
				$( '.admin-cms-media__files' ).addClass( 'grid' );
			});
			
			// enlarge file
			$( '.admin-cms-media__file-image, .admin-cms-media__file-close' ).on( 'click', function() {
				$( this ).parents( '.admin-cms-media__file' ).toggleClass( 'fixed' );
				$( '.admin-cms-media__overlay' ).toggle();
			} );
        }
    };
    
    return cms;
    
} )( cmsJS || {}, jQuery );
