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
 * @version 3.2.0
 * @module viewerJS.slideshow
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    // default variables
    var _debug = false;
    
    viewer.slideshows = {
     	
     	styles: new Map([
	     	["base", {
	     		maxSlides: 20,
	     		timeout: 10000, //ms
	     		imageWidth: 800,
	     		imageHeight: 1000,
	     		swiperConfig: {
				  direction: 'horizontal',
				  loop: false,
			      slidesPerView: 1,
			      spaceBetween: 50,
			    }
			  }],
			["vertical", {
				maxSlides: 20,
	     		timeout: 10000, //ms
	     		imageWidth: 800,
	     		imageHeight: 1000,
	     		swiperConfig: {
				  direction: 'vertical',
				  loop: true,
			      slidesPerView: 1,
			      spaceBetween: 50,
			    }
			}],	
     	]),
     	init: function() {
     		riot.mount("slideshow", {language: currentLang, styles: this.styles});
     		
     		//Remount all sliders after each ajax call which responst contains a slideshow tag
     		viewer.jsfAjax.success
     		.pipe(
     			rxjs.operators.filter( e => e.responseText && e.responseText.includes("<slideshow ")),
     			rxjs.operators.debounceTime(500)
     			)
     		.subscribe((e) => {
     			console.log("update slideshow");
     			riot.mount("slideshow", {language: currentLang, styles: this.styles});
     		});
     	},
     	set: function(name, config) {
     		this.styles.set(name, config);
     	},
     	update: function(name, configFragment) {
     		let config = $.extend( true, {}, this.styles.get(name), configFragment );
     		this.set(name, config);
     	},
     	copy: function(name) {
     		let config = $.extend( true, {}, this.styles.get(name));
     		return config;
     	}
            
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );