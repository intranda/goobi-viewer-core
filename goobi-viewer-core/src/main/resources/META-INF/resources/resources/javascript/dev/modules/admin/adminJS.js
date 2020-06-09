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
 * @description Base-Module which initialize the global admin object. * 
 * @version 3.4.0
 * @module adminJS
 * @requires jQuery
 */
var adminJS = ( function() {
    'use strict';
    
    var _debug = false; 
    var admin = {};
    
    /**
     * @description Method which initializes the admin module. 
     * @method init
     */
    admin.init = function() {
        if ( _debug ) {
            console.log( '##############################' );
            console.log( 'adminJS.init' );
            console.log( '##############################' );
        }
        
        // init sidebar
        adminJS.sidebar.init();
    };
    
    return admin;
    
} )( jQuery );


$( document ).ready(function() {
// toggle help text for admin forms
	$('[data-toggle="helptext"]').click(function() {
		$(this).closest(".form-group").children('.admin__form-input').children('.admin__form-help-text').toggleClass('in');
		$(this).parent().parent().next().next('.admin__license-functions-help').children('.admin__form-help-text').toggleClass('in');
	});

// hide license functions if open access checkbox is checked
	// check if checkbox already checked on page load
	if ($('#openaccess').prop('checked')) {
			  $('.admin__license-functions').hide();
		  }
	// check if checkbox status changes
	$("#openaccess").change(function(){
		  if ($(this).is(':checked'))
		   $('.admin__license-functions').fadeOut('fast');
		  else $('.admin__license-functions').fadeIn('fast');
	});
});



