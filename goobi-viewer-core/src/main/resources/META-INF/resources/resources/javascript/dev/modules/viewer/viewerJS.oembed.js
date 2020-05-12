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

    viewer.oembed = function(url) {
        
        return fetch(url, {
            method: "GET",
            headers: {
                'Content-Type': 'application/json'
            }
        })
        .then(response => {
            if(!response.ok) {
                throw reponse.errorMessage;
            }
            return response.json();
        })
        .then(json => {
            if(json.type == "rich") {                
                return $(json.html)
            } else if(json.type == "photo") {
                return $("<img src='" + json.url + "'/>");
            }
        })
        
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
