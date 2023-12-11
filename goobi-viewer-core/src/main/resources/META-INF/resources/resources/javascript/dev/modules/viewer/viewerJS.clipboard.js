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
 * Module to manage the data table features.
 * 
 * @version 3.2.0
 * @module viewerJS.clipboard
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    const _defaultSelector ="[data-copy-value]";



    viewer.clipboard = {
            init : function(selector, dataTarget) {

                if(!selector) {
                	var tooltipValueSelector = _defaultSelector;
                }
				else {
					var tooltipValueSelector = selector;
				}
	
				// console.log('clipboard js on');
                // DEFINE TOOLTIP STANDARD AND COPY DONE TEXT SELECTORS
                // var tooltipValueSelector = '[data-copy-value]'
                // var tooltipTextCopiedSelector = '[data-copy-done-msg]'
                
                // ACTIVATE TOOLTIP ONLY IF DATA COPY VALUE SELECTORS AVAILABLE
                $(tooltipValueSelector).tooltip();
                
                // COPY BUTTON VAR
                var copyClipboardButton = $(tooltipValueSelector);
                
                $(copyClipboardButton).click(function () {
                	
                	var thisCopyButton = $(this);
                	
					// GET VALUE OF DATA ATTRIBUTE COPY VALUE
					// define dataTarget to get other data attribute value than data-copy-value
	                if(!dataTarget) {
	                	var copyValue = $(this).data('copy-value');
	                }
					else {
						var copyValue = $(this).attr(dataTarget);
						console.log('datatarget is on ' + dataTarget);
						console.log('value of special: ' + copyValue);
					}
					
					// var copyValue = $(this).data('copy-value');
					// console.log(copyValue);
					
					// CREATE TEMPORARY TEXTAREA TO COPY VALUE INTO CLIPBOARD
					var $temp = $("<textarea />");
					$("body").append($temp);
					$temp.val(copyValue).select();
					document.execCommand("copy");
					$temp.remove();

					// CHANGE TOOLTIP TO COPY DONE MESSAGE
					var copyThisMessage = $(thisCopyButton).data('original-title');
					
					// IF NO COPY DONE MSG FOUND JUST USE STANDARD TEXT
					if (!$(this).data('copy-done-msg')) {
						// TO DO: TRANSLATE MSG KEY
						var copyDoneMessage = 'Copied to clipboard';
					}
					else {
						var copyDoneMessage = $(thisCopyButton).data('copy-done-msg');
					}

			    	 $(thisCopyButton).tooltip('hide');
			    	 $(thisCopyButton).tooltip('dispose');
					 $(thisCopyButton).tooltip({trigger: "manual", placement: "top", title: 'Copy'}); 
			    	 $(thisCopyButton).attr('data-original-title', copyDoneMessage);
			    	 $(thisCopyButton).tooltip('show');
					
			    	 // KEEP COPIED SUCCESS MSG FOR 2 SECONDS THEN REENABLE NORMAL TOOLTIP 
					setTimeout(function () {
					
					   $(thisCopyButton).tooltip('hide');
					   $(thisCopyButton).on('hidden.bs.tooltip', function () {
					   $(thisCopyButton).tooltip('dispose');
					   $(thisCopyButton).attr('data-original-title', copyThisMessage);
					   $(thisCopyButton).tooltip({trigger: "hover", placement: "top", title: copyThisMessage}); 
					    })
					
					}, 2000);

                });

            }
    }
        
    return viewer;
    
} )( viewerJS || {}, jQuery );
