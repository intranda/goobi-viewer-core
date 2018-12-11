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
    
    const _defaultSelector ="[data-copy]";
        
    viewer.clipboard = {
            init : function(selector) {
                if(!selector) {
                    selector = _defaultSelector;
                }
                var clipboard = new ClipboardJS( selector );
                $(selector + "[data-copy-done]").tooltip();

                
                clipboard.on("success", function(e) {
                   var $trigger = $(e.trigger);
                   var tooltipText = $trigger.attr('data-copy-done');
                   if(tooltipText) {                       
                       if(!$trigger.attr('data-original-original-title')) {
                           $trigger.attr('data-original-original-title', $trigger.attr('data-original-title'));
                       }
                       $trigger
                       .attr('data-original-title', tooltipText)
                       .tooltip('fixTitle')
                       .tooltip('show')
                       .attr('data-original-title', $trigger.attr('data-original-original-title'))
                       .tooltip('fixTitle');
                   }
        
                })
            }
    }
        
    return viewer;
    
} )( viewerJS || {}, jQuery );
