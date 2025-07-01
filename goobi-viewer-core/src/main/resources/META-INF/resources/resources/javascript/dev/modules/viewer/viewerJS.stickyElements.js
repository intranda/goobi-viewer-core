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
 * Module which initializes and manages sticky behaviour of scrollable elements. 
 * Uses the hc-sticky libary (https://github.com/somewebmedia/hc-sticky)
 * 
 * @version 3.2.0
 * @module viewerJS.stickyElements
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';

	var _debug = false;    
     
    viewer.stickyElements = {
    	initialized: false,
		refresh: new rxjs.Subject(),
		initRefresh: function() {
			this.refresh
			.pipe(rxjs.operators.delay(0))
	        .subscribe(() => {
	        	if(_debug)console.log("refresh hcSticky rx");
	        	$(document).ready(() => $(".-sticky, .-refreshHCsticky").hcSticky('refresh', {}));
	        }); 
			 
			/**
			* Refresh hcsticky after ajax requests
			**/
			viewerJS.jsfAjax.success.subscribe(this.refresh);
		},
		init: function(config) {

			if(_debug) {
				console.log( '##############################' );
		        console.log( 'viewer.stickyElements.init' );
		        console.log( '##############################' );
		        console.log( 'viewer.stickyElements.init: config - ' );
		        console.log( config );
		        console.log( '##############################' );
		        console.log( 'viewer.stickyElements.init: initialized - ' );
		        console.log( this.initialized );
			}

			if(!this.initialized) {
				this.initRefresh();
				this.initialized = true;
			}
			
			if(config.initAdmin) {
				// STICKY ELEMENTS TARGETS AND OPTIONS
				// sticky admin main menu sidebar left side
				if ($(".admin__sidebar").length) {
					$(".admin__sidebar-inner").hcSticky({
						stickTo: $('.admin')[0],
						innerTop: -35
					});
				}
	
				// general sticky element for admin backend - sticks to selector .admin__content-wrapper
				if ($(".admin__sidebar").length) {
					$('.-sticky').hcSticky({
						stickTo: $('.admin__content-wrapper')[0],
						innerTop: -50,
						bottom: 0
					});
				}
				
				// sticky content main area for create campaign
				if ($("#crowdAddCampaignView").length) {
					$('#crowdAddCampaignView .admin__content-main').hcSticky({
						stickTo: $('.admin__content-wrapper')[0],
						innerTop: -50
					});
				}
				
				// sticky content main area for create CMS page
				if ($("#cmsCreatePage").length) {
					$('#cmsCreatePage .admin__content-main').hcSticky({
						stickTo: $('.admin__content-wrapper')[0],
						innerTop: -50
					});
				}
				
			}
			
			if(config.initFrontend) {
				// toggle collapsible widgets
	       		viewer.toggledCollapsible.subscribe(e => {
	            	var pos = $(e.target).position().top;
		       		var offset = $(e.target).offset().top;
		       		var currentPos = document.documentElement.scrollTop;
		       		if(currentPos > pos) {
	            		window.scrollTo(0,pos);
					}		       		
     				viewerJS.stickyElements.refresh.next();
				});
				
				// sticky sidebar
			 	$('[data-target="sticky-sidebar"]').hcSticky({
			    	top: 100,
				    responsive: {
				      768: {
				        disable: true
				      }
				    }
			 	});	
			}
		},

	};
	return viewer;
} )( viewerJS || {}, jQuery );

