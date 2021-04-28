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
 */

var viewerJS = ( function( viewer ) {
    'use strict';

    var _debug = false;
    
    var _defaults = {
            initHcSticky: false,
            initSearch: false,
            initTextTree: false
    };
 
    viewer.archives = { 
    	recordPi : "", //pi of the current record. Read from [data-name="recordPi"] and used to create image display and record links
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.archivesSeparate.init' );
                console.log( '##############################' );
                console.log( 'viewer.archivesSeparate.init: config - ', config );
            }
            this.config = $.extend( true, {}, _defaults, config );
			$(".archives__object-image").hide();
            
            
            jQuery(document).ready(($) => {
				
				this.initImageDisplay();
				viewerJS.jsfAjax.success.subscribe(() => this.initImageDisplay());
				
                if(this.config.initHcSticky) {                    
                    this.initHcStickyWithChromeHack();
                }
                viewerJS.jsfAjax.success
                .subscribe(e => {
                    $(".archives__object-image").hide();
                	this.initImageDisplay()
                	.then(() => {
	                    if(this.config.initHcSticky) {                        
	                        this.refreshStickyWithChromeHack();
	                    }
	                    this.setLocation(e.source);
                	});
                });
                
                if(this.config.initSearch) {
                    this.initSearch();
                }
            	 
            	 if(this.config.initTextTree) {
            	     this.initTextTree();
            	 }
            	 
            	
            });            
            
        },
        
        initImageDisplay() {
        	let $recordPiInput = $('[data-name="recordPi"]');
        	console.log("record pi", $recordPiInput, $recordPiInput.val());
        	if($recordPiInput.length > 0) {
        		let recordPi = $recordPiInput.val();
        		console.log("record pi is " + recordPi);
        		if(recordPi != this.recordPi) {
        			this.recordPi = recordPi;
        			let manifestUrl = rootURL + "/api/v2/records/" + recordPi + "/manifest/";
        			return fetch(manifestUrl)
        			.then(response => response.json())
        			.then(manifest => {
        				//if the manifest contains struct elements, show them as thumbnail gallery
        				if(manifest.structures && manifest.structures.length > 1) {
				        	riot.mount(".archives__object-thumbnails", "thumbnails", {
					        	language : currentLang, 
					        	type: "structures",
					        	source: manifest,
					        	imagesize: ",250",
					        	label: "MD_SHELFMARK",
					        	link: (canvas) => {
					        		if(canvas.homepage && canvas.homepage.length > 0) {
										return canvas.homepage[0].id.replace("/image/", "/fullscreen/");
									} else {
										return undefined;
									}
					        	} 
				        	});
        				} else {
        					$(".archives__object-image").show();
        				}
        			});
        		}
        	}
        	return Promise.resolve();
        },
        
        initTextTree: function() {
         // toggle text-tree view from stairs to one line
            $('body').on("click", '.archives__text-tree', function() {
                $('.archives__text-tree').toggleClass('-showAsOneLine');
            });
        },
        
        initSearch: function() {
            /* check search field for input value and show clear button */
            if(!$('.archives__search-input').val() == ''){
                $('.archives__search-clear').show();
            }
            $('body').on("click", '.archives__search-clear', function(){
                /* clear value on click*/
            $('.archives__search-input').val("");
                /* trigger empty search on click */
                $('.archives__search-submit-button').click();
            });

             // auto submit search after typing
             let timeSearchInputField = 0;

             $('body').on('input', '.archives__search-input', function () {
                 // Reset the timer while still typing
                 clearTimeout(timeSearchInputField);

                 timeSearchInputField = setTimeout(function() {
                     // submit search query
                     $('.archives__search-submit-button').click();
                 }, 1300);
             });
        },
        
        /**
         * In chome with small window size (1440x900) hcSticky breaks on ajax reload if the page is scrolled
         * all the way to the button. To prevent this we quickly scroll to the top, refresh hcSticky and then scroll back down.
         * The scolling appears to be invisible to the user, probably because it is reset before actually being carried out
         */
        refreshStickyWithChromeHack: function() {
            let currentScrollPosition = $('html').scrollTop();
            $('html').scrollTop(0);
            this.refreshHcSticky();
            if(currentScrollPosition) {
                $('html').scrollTop(currentScrollPosition);
            }
        },
        
        refreshHcSticky: function() {
            if(_debug)console.log("update hc sticky");
//            $('.archives__left-side, .archives__right-side').hcSticky('refresh');
            $('.archives__left-side, .archives__right-side').hcSticky('update', {
                stickTo: $('.archives__wrapper')[0],
                top: 80,
                bottom: 20,
                responsive: {
                    993: {
                      disable: true
                    }
                }
           });
        },
        
        /**
         * In chome with small window size (1440x900) hcSticky breaks on page load if the view was previously scrolled
         * all the way to the button. To prevent this we scroll 5 px up before refreshing hcSticky.
         * The scolling appears to be invisible to the user, probably because it is reset before actually being carried out
         */
        initHcStickyWithChromeHack: function() {
            let currentScrollPosition = $('html').scrollTop();
            $('html').scrollTop(currentScrollPosition-5);
            this.initHcSticky();
//            if(currentScrollPosition) {
//                $('html').scrollTop(currentScrollPosition);
//            }
        },

        
        initHcSticky: function() {
            if(_debug)console.log("init hc sticky");
                        
            // Sticky right side of archives view
            $('.archives__right-side').hcSticky({
                stickTo: $('.archives__wrapper')[0],
                top: 80,
                bottom: 20,
                responsive: {
                    993: {
                      disable: true
                    }
                }
            });
            
            // Sticky left side of archives view
            $('.archives__left-side').hcSticky({
                stickTo: $('.archives__wrapper')[0],
                top: 80,
                bottom: 20,
                responsive: {
                    993: {
                      disable: true
                    }
                }
            });
        },
        
        setLocation: function(element) {
            if(_debug)console.log(" clicked data-select-entry", element);
            let select = $(element).attr("data-select-entry");
            let url = window.location.origin + window.location.pathname;
            if(select) {
                url += ("?selected=" + select + "#selected");
            }
            if(_debug)console.log("set url ", url);
            window.history.pushState({}, '', url);
        }
    };


    return viewer;
} )( viewerJS || {}, jQuery );
