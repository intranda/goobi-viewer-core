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

    viewer.overlay = {
            init : function() {
                $("[data-overlay='content']").each( (index, overlay) => {
                    let $overlay = $(overlay);
                    let $node = $overlay.children();
                    let $trigger = $("#" + $overlay.attr("data-overlay-trigger"));
                    let type = $overlay.attr("data-overlay-type");
                    if(!type) {
                        type = "default";
                    }
                    let strClosable = $overlay.attr("data-overlay-closable");
                    let closable = true;
                    if(strClosable && strClosable.toLowerCase() == "false") {
                        closable = false;
                    }
                    let strFullscreen = $overlay.attr("data-overlay-fullscreen");
                    let fullscreen = false;
                    if(strFullscreen && strFullscreen.toLowerCase() == "true") {
                        fullscreen = true;
                    }
                    $trigger.on("click", (event) => {
                        switch(type) {
                            case "modal":
                                viewer.overlay.openModal($overlay, closable);
                                break;
                            default:
                                viewer.overlay.open($node, closable, fullscreen);
                        }
                        $node.show();
                    });
                })
            }
    }
    
    viewer.overlay.openModal = function(node, closable, onClose) {
        
        return new Promise( (resolve, reject) => {
        
	        let $overlay = $("#overlayModal");
	        if($overlay.length > 0) {
	            if($overlay.hasClass("active")) {
	                defer.reject("overlay is already active");
	                return;
	            }
	            console.log("open in ", $overlay);
	            let $node = $(node);
	            let $contentHeader = $node.find("[data-overlay-content='header']");
	            let $contentBody = $node.find("[data-overlay-content='body']");
	            let $contentFooter = $node.find("[data-overlay-content='footer']");
	            
	            let $areaHeader = $overlay.find("[data-overlay-area='header']");
	            let $areaBody = $overlay.find("[data-overlay-area='body']");
	            let $areaFooter = $overlay.find("[data-overlay-area='footer']");
	            
	            if($areaHeader.length > 0 && $contentHeader.length > 0) {
	                $areaHeader.append($contentHeader);
	            }
				console.log("append ", $contentBody, " to ", $areaBody);
	            if($areaBody.length > 0 && $contentBody.length > 0) {
	                $areaBody.append($contentBody);
	            }
	            if($areaFooter.length > 0 && $contentFooter.length > 0) {
	                $areaFooter.append($contentFooter);
	            }
	            
	            $overlay.addClass("modal-container");
	            let $modal = $("#overlayModal");
	            $modal.modal({
	                backdrop: true,
	                keyboard: closable,
	                focus: true,
	                show: true
	            })
	            $modal.on("hide.bs.modal", event =>  {
	                return closable;
	            })
	            $modal.on("shown.bs.modal", event => {
	            	resolve(node);
	            });
	            $modal.on("hidden.bs.modal", event => {
	                $modal.modal("dispose");
	                $overlay.removeClass("modal-container");
	                if(onClose) {
	                	onClose(node);
	                }
	                $("body").off("click", ".close-modal");
	            });
	            let $dismissButtons = $overlay.find("[data-overlay-action='dismiss']")
	            if(closable === true) {      
	                $dismissButtons.show();
	                $( 'body' ).one( 'click.close-modal', "[data-overlay-action='dismiss']", event => {
	                    $modal.modal("hide");
	                });
	            } else {
	                $dismissButtons.hide();
	            }
	        } else {
	            reject("No overlay element found");
	        }
        
        });
        
    }
    
    viewer.overlay.open = function(node, closable, fullscreen, onClose) {
   
   		return new Promise( (resolve, reject) => {
	        let $overlay = $(".overlay");
   		// console.log("open overlay", node, $overlay);
	        if($overlay.length > 0) {
	            if($overlay.hasClass("active")) {
	                reject("overlay is already active");
	                return;
	            }
	            
	            let $node = $(node);
	            $overlay.append($node);
	            $overlay.addClass("active");
	            $( 'html' ).addClass( 'no-overflow' );
	            
	           let overlay = {
	        		node: $node,
	        		wrapper: $overlay,
		        	close: function() {
		        		($node).detach();
		                $overlay.removeClass("active");
		                $overlay.removeClass("fullscreen");
		                $( 'html' ).removeClass( 'no-overflow' );
		                if(onClose) {
		                	onClose(node);
		                }
		                $("body").off("click.close-modal");
		        	}
		        
		        }
	                    
	            
	            let $dismissButtons = $overlay.find("[data-overlay-action='dismiss']")
	            if(closable === true) {      
	                $dismissButtons.show();
	                $( 'body' ).one( 'click.close-modal', "[data-overlay-action='dismiss']", event => {
	                    overlay.close();
	                });
	                //close on click outside content
	                $( 'body' ).one( 'click.close-modal', ".overlay", event => {
	                    if($(event.target).hasClass("overlay")) {
	                        overlay.close();
	                    }
	                });
	            } else {
	                $dismissButtons.hide();
	            }
	            
	            if(fullscreen) {
	                $overlay.addClass("fullscreen");
	            }
	            resolve(overlay);
	        } else {
	            reject("No overlay element found");
	        }
	        
        });
    }
    
    $(document).ready( () => {        
        viewer.overlay.init();
    })
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
