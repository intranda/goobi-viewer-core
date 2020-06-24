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
	$("body").on("click", '[data-toggle="helptext"]', function() {
		$(this).closest('.form-group').children('.admin__form-input, .admin__license-functions-help').find('.admin__form-help-text').toggleClass('in');
	});

// hide license functions if open access checkbox is checked
	// check if checkbox already checked on page load
	if ($('.openAccessToggle input:nth-of-type(1)').prop('checked')) {
			  $('.admin__license-functions').hide();
		  }
	// check if checkbox status changes
	$(".openAccessToggle input").change(function(){
		  if ($('.openAccessToggle input:nth-of-type(1)').is(':checked'))
		   $('.admin__license-functions').fadeOut('fast');
		  else if ($('.openAccessToggle input:nth-of-type(2)').is(':checked'))
			   $('.admin__license-functions').fadeIn('fast');
	});
});

// hiding the new tab option for cms menus if link value is '#'
$(document).ready(function(){
	$('.cms-module__option-url').each(function() {
		if ($(this).val() == "#") {
			$(this).parent().parent().next(".cms-module__option-group").hide();
		}
	});
	// check if form input value changes
	$('.cms-module__option-url').each(function() {
		$(this).on('keyup change ready', function() {
			if ($(this).val() == "#") {
				$(this).parent().parent().next(".cms-module__option-group").fadeOut();
			}
			else {
				$(this).parent().parent().next(".cms-module__option-group").fadeIn();
			}
		});
	});
});