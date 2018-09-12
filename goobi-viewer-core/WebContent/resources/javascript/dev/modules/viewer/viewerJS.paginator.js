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
 * Module to scroll a page back to top animated.
 * 
 * @version 3.2.0
 * @module viewerJS.paginator
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
        
    var _debug = false;

    var _defaults = {
            maxDoubleClickDelay: 250    //ms
    }
    
    viewer.paginator = {

        /**
         * Initializes keyboard bindings for paginator
         * 
         * @method init
         * @param {String} obj The selector of the jQuery object.
         * @param {String} anchor The name of the anchor to scroll to.
         */
        init: function( config ) {
            this.lastKeypress = 0;
            this.lastkeycode = 0;
            this.config = jQuery.extend(true, {}, _defaults);   //copy defaults
            jQuery.extend(true, this.config, config);           //merge config
            if(_debug) {
                console.log("Init paginator with config ", viewer.paginator.config);
            }

            $(document.body).on("keyup", viewer.paginator.keypressHandler);
            
        },    
        keypressHandler: function(event) {
            if(event.originalEvent) {
                event = event.originalEvent;
            }
            var keyCode = event.keyCode;
            var now = Date.now();
                        
            //this is a double key press if the last entered keycode is the same as the current one and the last key press is less than maxDoubleClickDelay ago
            var doubleKeypress = (viewer.paginator.lastKeycode == keyCode && now-viewer.paginator.lastKeyPress <= viewer.paginator.config.maxDoubleClickDelay);
            viewer.paginator.lastKeycode = keyCode;
            viewer.paginator.lastKeyPress = now;
            
            if(_debug) {                
                console.log("key pressed ", keyCode);
                if(doubleKeypress) {
                    console.log("double key press");
                }
            }
            switch(keyCode) {
                case 37:
                    if(doubleKeypress && viewer.paginator.config.first && $(viewer.paginator.config.first).length) {
                        $(viewer.paginator.config.first).get(0).click();
                    } else if(viewer.paginator.config.previous && $(viewer.paginator.config.previous).length) {
                        $(viewer.paginator.config.previous).get(0).click();
                    }
                    break;
                case 39:
                    if(doubleKeypress && viewer.paginator.config.last && $(viewer.paginator.config.last).length) {
                        $(viewer.paginator.config.last).get(0).click();
                    } else if(viewer.paginator.config.next && $(viewer.paginator.config.next).length) {
                        $(viewer.paginator.config.next).get(0).click();
                    }
                    break;
            }
        },
        close: function() {
            $(document.body).off("keyup", viewer.paginator.keypressHandler);
        }

    };

    
    return viewer;
    
} )( viewerJS || {}, jQuery );
