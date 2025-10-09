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
 * @module viewerJS.translator
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';

    viewer.toggle = {
            
            init: function() {
                let $visibilityToggles = $("input[data-toggle-action]");
                if($visibilityToggles.length > 0) {
                    $visibilityToggles.each( (index, element) => {
                        let action = $(element).attr("data-toggle-action");
                        let target = $(element).attr("data-toggle-target");
                        let checked = $(element).is(":checked");
                        let $target = $(target);
                        if(action && checked && $target.length > 0) {
                            switch(action) {
                                case "hide":
                                    $target.hide();
                                    break;
                                case "show": 
                                    $target.show();
                                    break;
                            }
                            let toggleSelector = "input[data-toggle-target='"+target+"']";
                            $("body").on("change", toggleSelector, (event) => {
                                if($(event.target).is(":checked")) {
                                    $target.animate({
                                        height: "toggle",
                                        opacity: "toggle"
                                    }, 250);
                                }
                            })
                        }
                    } )
                }
            }
    
    }
    
	return viewer;
    
} )( viewerJS || {}, jQuery );