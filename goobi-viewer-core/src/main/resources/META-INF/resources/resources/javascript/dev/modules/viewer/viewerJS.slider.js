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
 * Manages styles for (image) sliders. A style includes both css-styling (defined by a less file in resources/css/less/slider/)
 * and behaviour which is defined in the styles map in this module.
 * the "base" slider style must always exist as a default for slider without style information.
 * 
 * Additional styles may be added by using the 'set' method; existing styles may be altered by using the 'update' method. 
 * Any such changes must be performed before document.ready is called to they are available when the styles are used.
 * 
 * Styles are used in two places:
 *   1. the slider.tag riot-tag in  resources/javascript/dev/tags/ to render the actual slider
 *   2. createSlider.js in resources/javascript/dev/modules/cms/ where a dropdown is rendered via jquery to select a style in resorces/cms/adminCmsSliderEdit.xhtml
 * 
 * @version 3.2.0
 * @module viewerJS.slideshow
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    // default variables
    var _debug = false; 
    
    viewer.slider = {
	
		sliders: [],

     	
     	styles: new Map([
		  ["base", {
	     		maxSlides: 20,
	     		timeout: 10000, //ms
	     		imageWidth: 800,
	     		imageHeight: 1000,
	     		swiperConfig: {
				  direction: 'horizontal',
				  loop: false,
			      slidesPerView: 2,
			      spaceBetween: 20,
				  a11y: {
				    prevSlideMessage: 'Previous slide',
				    nextSlideMessage: 'Next slide',
				  },
			    }
		  }], 
     	["full-width", {
	     		maxSlides: 20,
	     		timeout: 10000, //ms
	     		imageWidth: 1920,
	     		imageHeight: 1000,
	     		swiperConfig: {
				  direction: 'horizontal',
				  loop: false,
			      slidesPerView: 1,
				  a11y: {
				    prevSlideMessage: 'Previous slide',
				    nextSlideMessage: 'Next slide',
				  },
			    }
			  }],
		["3-slides-pagination", {
				maxSlides: 20,
	     		timeout: 10000, //ms
	     		imageWidth: 800,
	     		imageHeight: 1000,
	     		swiperConfig: {
				  loop: true,
			      slidesPerView: 1, 
			      spaceBetween: 20,
			      pagination: {
//			          el: '.swiper-pagination',
			          clickable: true
			      },
			      breakpoints: {
			    	    // when window width is >= 450px
			    	    450: {
			    	      slidesPerView: 1,
			    	      spaceBetween: 0
			    	    },
			    	    // when window width is >= 320px
			    	    700: {
			    	      slidesPerView: 2,
			    	      spaceBetween: 10
			    	    },
			    	    // when window width is >= 640px
			    	    1200: {
			    	      slidesPerView: 3,
			    	      spaceBetween: 20
			    	    }
			      },
				  a11y: {
					enabled: true,
				    prevSlideMessage: 'Previous slide',
				    nextSlideMessage: 'Next slide',
				  },
			    }
			}],
		["fade-effect-auto-play", {
				maxSlides: 20,
	     		timeout: 10000, //ms
	     		imageWidth: 1600,
	     		imageHeight: 1000,
	     		swiperConfig: {
				  loop: true,
				  simulateTouch: false,
			      effect: 'fade',
			      speed: 850,
			      fadeEffect: {
			        crossFade: true
			      },
			      autoplay: {
			    	   delay: 3700,
			      },
			    }
			}],
		["centered-mode", {
				maxSlides: 20,
	     		timeout: 10000, //ms
	     		imageWidth: 800,
	     		imageHeight: 1000,
	     		swiperConfig: {
	     		  centeredSlides: true,
			      spaceBetween: 30,
			      slidesPerView: 4,
			      loop: true,
			      
			    }
			}],
		["vertical-auto-play", {
				maxSlides: 20,
	     		timeout: 10000, //ms
	     		imageWidth: 800,
	     		imageHeight: 1000,
	     		swiperConfig: {
				  direction: 'vertical',
				  loop: true,
			      slidesPerView: 1,
			      spaceBetween: 10,
			      autoplay: {
			    	   delay: 3200,
			      },
			    }
			}],
     	]),
     	init: function() {
     		if(_debug)console.log("loading slider ", this);
     		riot.mount("slider", {language: currentLang, styles: this});
     		
     		//Remount all sliders after each ajax call which responst contains a slider tag
     		viewer.jsfAjax.success
     		.pipe(
     			rxjs.operators.filter( e => e.responseText && e.responseText.includes("<slider ")),
     			rxjs.operators.debounceTime(500)
     			)
     		.subscribe((e) => {
     			// console.log("update slider");
     			riot.mount("slider", {language: currentLang, styles: this});
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
     	},
     	get: function(name) {
     		let config = this.styles.get(name);
     		if(!config) {
     			console.warn("Style \"" + name + "\" is not included in the list of slider styles. Using \"base\" as fallback");
     			return this.styles.get("base");
     		} else {
     			return config;
     		}
     	},
     	getStyleNameOrDefault: function(name) {
     		if(this.styles.has(name)) {
     			return name;
     		} else {
     			return "base";
     		}
     	}
            
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );