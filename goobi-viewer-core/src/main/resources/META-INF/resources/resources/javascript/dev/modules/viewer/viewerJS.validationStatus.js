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
 * 
 * @version 4.7.0
 * @module viewerJS.oembed
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = true;
    

    viewer.validationStatus = {
        
        init: function() {
            if(_debug)console.log("init validation status")
            if($(".-validation-mark").length) {                
                viewer.jsfAjax.success.subscribe( e => {
                    let $validationMessages = $(".-validation-message");
                    if(_debug)console.log("check validation messages ", $validationMessages);
                    $validationMessages.each( (index,message) => {
                        let $message = $(message);
                        let severity = $message.attr("class").replace("-validation-message", "").trim();
                        if(_debug)console.log("set validation severity ", severity);
                        $message.nextAll(".-validation-mark, .-validation-input").addClass(severity);
                        let forAttr = $message.attr("for");
                        if(forAttr) {
                            $("#" + forAttr).addClass(severity);
                        }
                    } )
                })
            }
        }
        
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
