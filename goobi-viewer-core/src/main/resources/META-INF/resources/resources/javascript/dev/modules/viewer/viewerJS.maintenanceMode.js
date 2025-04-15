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
 * @description Module which handles the maintenance mode banner. 
 * @version 3.4.0
 * @module viewerJS.maintenanceMode
 * @requires jQuery
 * 
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    // variables
    var _debug = false;
    var _defaults = {
    	lastEditedHash: '',
    	active : true,
    };
    
    viewer.maintenanceMode = {
        /**
         * Method to initialize the maintenance mode banner.
         * 
         * @method init
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.maintenanceMode.init' );
                console.log( '##############################' );
                console.log( 'viewer.maintenanceMode.init: config - ', config );
            }
            
            this.config = $.extend(true, {}, _defaults, config );
            if(_debug)console.log("init maintenance mode with config", this.config);

			if(this.config.active) {
				// Close maintenance banner and set session storage item
				$( '[data-trigger="closeMaintenanceInfo"]' ).off().on( 'click', () => this.hideInfo(true) );

				// Check if user already closed maintenance banner in this session

				if ( sessionStorage.getItem( 'hideMaintenanceBanner' ) === 'true' ) {
					// DO NOTHING
				} else {
					// SHOW THE BANNER
					$('#maintenanceModeBanner').slideDown();
				}
			}
 
		},
			
			hideInfo() {
			 	if ( _debug ) {
					console.log( 'EXECUTE: hideInfo');
				}
				$('#maintenanceModeBanner').slideUp( function() {
					sessionStorage.setItem( 'hideMaintenanceBanner', true );
				}); 
			},
			
        };

    return viewer;
    
} )( viewerJS || {}, jQuery );
