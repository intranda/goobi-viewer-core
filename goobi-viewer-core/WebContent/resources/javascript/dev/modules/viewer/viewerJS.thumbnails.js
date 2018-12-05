                
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
 * Module to respond to errors loading thumbnail images
 * 
 * @version 3.2.0
 * @module viewerJS.thumbnails
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _notFound;
    var _accessDenied
    
    // load images with error handling
    viewer.loadThumbnails = function() {
        

        _notFound = currentPath + '/resources/themes/' + viewer.theme + '/images/image_not_found.png';
        _accessDenied = currentPath + '/resources/themes/' + viewer.theme + '/images/access_denied.png';
        
        $('.viewer-thumbnail').each(function() {
            var element = this;
            var source = element.src
            var dataSource = element.dataset.src; 
            if(dataSource && !source) { 
                 _loadImage(element, dataSource);                
            }else if (source) {                   
                   var onErrorCallback = function() {
                       _loadImage(element, element.src)
                   }
                   //reload image if error event occurs
                   $(element).one("error", onErrorCallback)
                   //if image is already loaded but has not width, assume error and also reload
                   if(element.complete && element.naturalWidth === 0) {
                       $(element).off("error", onErrorCallback);
                       _loadImage(element, element.src)
                   }
            }
        });
    }
    
    function _loadImage(element, source) {

        $.ajax({
            url: source,
            cache: true,
            xhrFields: {
                responseType: 'blob'
            },
        })
        .done(function(blob) {
            var url = window.URL || window.webkitURL;
            element.src = url.createObjectURL(blob);
        })
        .fail(function(error) {
            console.log("loading image failed with  error ", error, error.status);
            var status = error.status;
                switch(status) {
                    case 403:
                        element.src = _accessDenied;
                        break;
                    case 500:
                    case 404:
                        element.src = _notFound;
                        break;
                    default:
                         element.src = source;
//                        element.src = _notFound;
                }
            });  
    }
    
    return viewer;
    
})( viewerJS || {}, jQuery );
                
                
                