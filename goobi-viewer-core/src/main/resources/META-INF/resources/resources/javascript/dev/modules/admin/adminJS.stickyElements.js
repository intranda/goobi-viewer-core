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
 * @module adminJS.sidebar
 * @requires jQuery
 * @description Module which sets the behavior of the admin sidebar.
 */
var adminJS = ( function( admin ) {
    'use strict';
    
    var _debug = false;

    admin.stickyElements = {
        /**
         * @description Method which initializes the admin sidebar module.
         * @method init
         */
        init: function() {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'adminJS.sidebar.init' );
                console.log( '##############################' );
            }

         // STICKY ELEMENTS TARGETS AND OPTIONS
         	// sticky admin main menu sidebar left side
         	$('.admin__sidebar').hcSticky({
         		stickTo: $('.admin__content')[0],
         		innerTop: 0
         	});
         	
         	// general sticky element for admin backend - sticks to selector .admin__content-wrapper
         	$('.-sticky').hcSticky({
         		stickTo: $('.admin__content-wrapper')[0],
         		innerTop: -50,
         		bottom: 0
         	});

         	// sticky content main area for create campaign
         	$('#crowdAddCampaignView .admin__content-main').hcSticky({
         		stickTo: $('.admin__content-wrapper')[0],
         		innerTop: -50
         	});
   
        }
    }
	
	return admin;
    
} )( adminJS || {}, jQuery );

