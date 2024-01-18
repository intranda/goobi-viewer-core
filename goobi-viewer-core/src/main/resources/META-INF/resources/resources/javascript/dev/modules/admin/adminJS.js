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
        // Initialize sticky elements for admin pages
        viewerJS.stickyElements.init({initAdmin:true});

		// Initialize copy to clipboard scripts for admin pages
		viewerJS.clipboard.init();
    };

    return admin;
    
} )( jQuery );


$( document ).ready(function() {

	// toggle help text for admin forms
	$("body").on("click", '[data-toggle="helptext"]', function() {
		$(this).closest('.form-group').children('.admin__form-input, .admin__license-functions-help').find('.admin__form-help-text').toggleClass('in');
		$(this).parents().siblings('.admin__form-help-text').toggleClass('in');
		$('.-refreshHCsticky').hcSticky('refresh', {});
	});

// hide license functions if open access toggle is yes
	// check if toggle yes on page load
	if ($('.openAccessToggle input:nth-of-type(2)').prop('checked')) {
			  $('.admin__license-functions').hide();
		  }
	// check if toggle status changes
	$(".openAccessToggle input").change(function(){
	  if ($('.openAccessToggle input:nth-of-type(2)').is(':checked'))
	   $('.admin__license-functions').animate({
		    height: "toggle",
		    opacity: "toggle"
		}, 250);
	  else if ($('.openAccessToggle input:nth-of-type(1)').is(':checked'))
	   $('.admin__license-functions').animate({
		    height: "toggle",
		    opacity: "toggle"
		}, 250);
	});

// toggle next cms right block after radio button
		// check if toggle yes on page load

	// $("body").on("click", '[data-toggle="helptext"]', function()
			
	$('.blockAfterRadioToggler').each(function() {
		if ($(this).find("input:nth-of-type(2)").prop('checked')) {
			$(this).next('.admin__license-selectable-block').hide();
		}
	});
 
	// check if radio button status changes
	$("body").one("click", '.blockAfterRadioToggler', function(event) { 
		$('.blockAfterRadioToggler input').change(function() {
			if ($(this).parent('.admin__radio-switch').find('input:nth-of-type(1)').is(':checked')) {
				$(this).closest('.blockAfterRadioToggler').next('.admin__license-selectable-block').animate({
			    opacity: "toggle"
				}, 250);
			}
			else if ($(this).parent('.admin__radio-switch').find('input:nth-of-type(2)').is(':checked')) {
				$(this).closest('.blockAfterRadioToggler').next('.admin__license-selectable-block').animate({
			    opacity: "toggle"
				}, 250);
			}
		});
	});

	// pdf quota radio switch - change color of box according to state
	$('#pdf_download_quota_info_box').each(function() {
		if ($(this).find("input:nth-of-type(1)").prop('checked')) {
			$(this).children('.admin__default-block').addClass('-gray-box');
		}
	}); 
	$("body").on("click", '#pdf_download_quota_info_box', function() {
		if ($('#pdf_download_quota_info_box').find("input:nth-of-type(1)").prop('checked')) {
			$('#pdf_download_quota_info_box').children('.admin__default-block').addClass('-gray-box');
		}
		else {
			$('#pdf_download_quota_info_box').children('.admin__default-block').removeClass('-gray-box');
		}
	});	

	// vertical language tabs focus effect
	$("body").on("focus", ".admin__language-tabs-vertical-textarea", function() {
		$(this).siblings('.admin__language-tabs-vertical').find('.admin__language-tab-vertical.active a').css({"border-color": "#3365a9", "border-right-color": "#fff"})
	});

	$("body").on("focusout", ".admin__language-tabs-vertical-textarea", function() {
		$(this).siblings('.admin__language-tabs-vertical').find('.admin__language-tab-vertical.active a').css({"border-color": "#ccc", "border-right-color": "#fff"})
	});
	
	// hiding the new tab option for cms menus if link value is '#'
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

// END DOCUMENT READY
});


