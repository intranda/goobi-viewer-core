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
 * @version 22.08
 * @module adminJS.licenceToggle
 * @requires jQuery
 * @description Module for the page resources/admin/views/adminLicense.xhtml
 */
var adminJS = ( function( admin ) {
    'use strict';
    
    var _debug = false;

    admin.licenceToggle = {
        /**
         * @description Method which creates a smooth js toggle for access rights settings.
         * @method init
         */
        init: function() {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'adminJS.licenceToggle.init' );
                console.log( '##############################' );
            }

		function elementLicenceTogglerFunction() {
		
			// WATCH CHANGES
			
			var $watchedElements = $("[data-watched-element]");
			
			$("body").one("click", $($watchedElements), function(event) {
				$($watchedElements).find('input').change(function() {
					
					// console.log($(this));
					
					let clickedWatchedElement = $(this).closest($watchedElements);
					// console.log(clickedWatchedElement);
					let id = $(this).closest($watchedElements).attr("data-watched-element");
					let keepFunctionsVisible = $('[data-target-visibility="keepVisible"]');
					let redirectInputField = $('[data-target-redirect="input"]')
					
					var watchedElementsInputsNo = $($watchedElements).find('.admin__radio-switch input:nth-of-type(1)');
					var watchedElementInputThis = $(clickedWatchedElement).find('.admin__radio-switch input:nth-of-type(1)');
					
					var clickedWatchedElementYes = '';
					if ($(clickedWatchedElement).find('input:nth-of-type(2)').is(':checked')) {
						var clickedWatchedElementYes = true;
						// console.log('toggle on yes');
					} else {
						var clickedWatchedElementYes = false;
						// console.log('toggle on no');
					}
		
		            switch (true) {
		                case (id == 'exclusiveAndInput'):
							// IF CHECKBOX YES
							if (clickedWatchedElementYes == true) {
								// console.log('exlusiveandinput is true')
								$($watchedElements).not(clickedWatchedElement).animate({
		                        	height: "hide",
		                            opacity: 0
		                            }, 250);
								$(keepFunctionsVisible).animate({
		                        	height: "hide",
		                            opacity: 0
		                            }, 250);
								$(redirectInputField).animate({
		                        	height: "show",
		                            opacity: 1
		                            }, 250);
								// $(watchedElementsInputsNo).not(watchedElementInputThis).prop('checked', true);
							}
							// IF CHECKBOX NO
							if (clickedWatchedElementYes == false) {
								// console.log('exlusiveandinput is false');
								$($watchedElements).not(clickedWatchedElement).animate({
		                        	height: "show",
		                            opacity: 1
		                            }, 250);
								$(keepFunctionsVisible).animate({
		                        	height: "show",
		                            opacity: 1
		                            }, 250);
								$(redirectInputField).animate({
		                        	height: "hide",
		                            opacity: 0
		                            }, 250);
							}
		                    break;
		                case (id == 'keepFunctions'):
							// IF CHECKBOX YES
							if (clickedWatchedElementYes == true) {
								// console.log('keepfunctions is true');
								$($watchedElements).not(clickedWatchedElement, keepFunctionsVisible).animate({
		                        	height: "hide",
		                            opacity: 0
		                            }, 250);
								$(keepFunctionsVisible).animate({
		                        	height: "show",
		                            opacity: 1
		                            }, 250);
								$(redirectInputField).animate({
		                        	height: "hide",
		                            opacity: 0
		                            }, 250);
								$(watchedElementsInputsNo).not(watchedElementInputThis).prop('checked', true);
							}
							// IF CHECKBOX NO
							if (clickedWatchedElementYes == false) {
								// console.log('keepfunctions is false');
								$($watchedElements).not(clickedWatchedElement).animate({
		                        	height: "show",
		                            opacity: 1
		                            }, 250);
								$(keepFunctionsVisible).animate({
		                        	height: "show",
		                            opacity: 1
		                            }, 250);
								$(redirectInputField).animate({
		                        	height: "hide",
		                            opacity: 0
		                            }, 250);
							}
		                    break;
		                case (id == 'exclusive'):
							if (clickedWatchedElementYes == true) {
							// IF CHECKBOX YES
								// console.log('exlusive is true');
								$($watchedElements).not(clickedWatchedElement).animate({
		                        	height: "hide",
		                            opacity: 0
		                            }, 250);
								$(keepFunctionsVisible).animate({
		                        	height: "hide",
		                            opacity: 0
		                            }, 250);
								$(redirectInputField).animate({
		                        	height: "hide",
		                            opacity: 0
		                            }, 250);
								$(watchedElementsInputsNo).not(watchedElementInputThis).prop('checked', true);
							}
							if (clickedWatchedElementYes == false) {
								// console.log('exlusive is false');
								$($watchedElements).not(clickedWatchedElement).animate({
		                        	height: "show",
		                            opacity: 1
		                            }, 250);
								$(keepFunctionsVisible).animate({
		                        	height: "show",
		                            opacity: 1
		                            }, 250);
								$(redirectInputField).animate({
		                        	height: "hide",
		                            opacity: 0
		                            }, 250);
							}
		                    break;
		                default:
		                    // console.log('default');
		            }
		
					});
				
					});
				}
				elementLicenceTogglerFunction();


        }
	}
	
	return admin;
    
} )( adminJS || {}, jQuery );