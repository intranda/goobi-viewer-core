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
 * Opens an overlay spanning the entire viewport and containing a passed element
 * Returns a promise that is rejected if no .overlay element is present in the DOM or if it is already active
 * and which is resolved - returning the (now detached) passed element - when the overlay is closed
 * 
 * @version 4.7.0
 * @module viewerJS.oembed
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';

    viewer.overlay = function(node) {
        
        let defer = Q.defer()
        
        let $overlay = $(".overlay");
        if($overlay.length > 0) {
            if($overlay.hasClass("active")) {
                defer.reject("overlay is already active");
            }
            
            let $node = $(node);
            $overlay.append($node);
            $overlay.addClass("active");
            $( 'html' ).addClass( 'no-overflow' );
            
            $( 'body' ).one( 'click', '.overlay > .fa-times', event => {
                ($node).detach();
                $overlay.removeClass("active");
                $( 'html' ).removeClass( 'no-overflow' );
                defer.resolve(node);
            });
        } else {
            defer.reject("No overlay element found");
        }
        
        return defer.promise;
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
