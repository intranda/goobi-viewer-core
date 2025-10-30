                
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

    // load images with error handling
    viewer.loadThumbnails = function(notFoundImage, accessDeniedImage) {
        this.notFound = notFoundImage;
        this.accessDenied = accessDeniedImage;
        this.thumbnailImageLoaded = new rxjs.Subject();
        
        viewer.jsfAjax.success.subscribe(() => this.loadAll());
		this.loadAll();
    }
    
    viewer.loadThumbnails.prototype.loadAll = function() {
   		$('[data-viewer-thumbnail="thumbnail"]').each((index, element) => this.load(element));
    }
    
    
    viewer.loadThumbnails.prototype.load = function(element) {
    	var source = element.src
        var dataSource = element.dataset.src; 
        if(dataSource && !source) { 
             this.loadImage(element, dataSource);            
        } else if (source) {     
               var onErrorCallback = () => {
                   this.loadImage(element, element.src)
               }
               //reload image if error event occurs
               $(element).one("error", onErrorCallback)
               //if image is already loaded but has not width, assume error and also reload
               if(element.complete && element.naturalWidth === 0) {
                   $(element).off("error", onErrorCallback);
                   this.loadImage(element, element.src)
               }
        }
    }
    
    
    viewer.loadThumbnails.prototype.loadImage = function(element, source) {
		//Hide broken image icon while loading by either setting style.display to "none" or setting empty alt attribute
		//first solution hides whole image, the latter only its content
		let alt = element.alt;
		let display = element.style.display;
		element.style.display = "none";
		//element.alt = "";
        $.ajax({
            url: source,
            cache: true,
            xhrFields: {
                responseType: 'blob'
            },
        })
        .done((blob) => {
        
            var url = window.URL || window.webkitURL;
            element.src = url.createObjectURL(blob);
            element.alt = alt;
            element.style.display = display;
            this.thumbnailImageLoaded.next(element);
        })
        .fail((error) => {
            var status = error.status;
                switch(status) {
                    case 403:
                        element.src = this.getAccessDeniedUrl(element);
                        break;
                    case 500:
                    case 404:
                        element.src = this.notFound;
                        break;
                    default:
                         element.src = source;
                }
                element.alt = alt;
                element.style.display = display;
                this.thumbnailImageLoaded.next(element);
            });  
    }

    viewer.loadThumbnails.prototype.getAccessDeniedUrl = function(imageElement) {
        return imageElement.dataset.viewerAccessDeniedUrl ?? this.accessDenied;
    }
    
    return viewer;
    
})( viewerJS || {}, jQuery );
                
                
                