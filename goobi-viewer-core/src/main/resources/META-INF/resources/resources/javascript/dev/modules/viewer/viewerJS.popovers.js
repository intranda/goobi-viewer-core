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
 * Creates a visible popup by attaching a copy of the given <popup> Element to the body and offsetting it to the appropriate position
 * The popup closes on a click anywhere outside of it, removing its associated eventhandlers in the process
 * 
 * @version 3.2.0
 * @module viewerJS.popovers
 * @requires jQuery
 * 
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    const _debug = false;
    
    const _triggerElementSelector = "[data-popover-element]";
    const _popoverSelectorAttribute = "data-popover-element";
    const _popoverTitleAttribute = "data-popover-title";
    const _popoverTemplate = "<div class='popover' role='tooltip'><div class='arrow'></div><h3 class='popover-title-custom'>#{title}</h3><div class='popover-content'></div></div>";    
    
    viewer.popovers = {
            
            init: function() {
                $(_triggerElementSelector).each( (index, element) => {
                    
                    let $element = $(element);
                    let popoverSelector = $element.attr(_popoverSelectorAttribute);
                    let $popover = $(popoverSelector);
                    if($popover.length === 1) {
                        this.initPopover($element, $popover);
                    } else if($popover.length > 1) {
                        console.error("Found more than one popover matching selector '" + popoverSelector + "'. Cannot initialize popover");
                    } else {
                        console.error("Found no popover matching selector '" + popoverSelector + "'. Cannot initialize popover");
                    }
                    
                    
                    
                })
            },
            
            initPopover: function($trigger, $popover) {
                console.log("create popover with content ", $popover.get(0));
                
                let config = {
                        html: true,
                        content: $popover.get(0)
                }
                
                let title = $trigger.attr(_popoverTitleAttribute);
                if(title) {
                    config.template = _popoverTemplate.replace("#{title}", title);
                }
                console.log("init popover with ", config);
                $trigger.popover(config);
            }
            
    }
    

    
    return viewer;
    
} )( viewerJS || {}, jQuery );
