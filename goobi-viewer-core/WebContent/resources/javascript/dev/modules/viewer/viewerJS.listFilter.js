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
 * Allows filtering a list of text entries with the content of an input element. Takes the selector of the input and one pointing to all
 * elements to filter. If the input element is not empty, hide all list elements which don't start with the input element value 
 * 
 * config looks like this:
 * 
 *  {
 *  	input : input.filter,
 *  	elements : ul li.filterelement
 *  }
 * 
 * @version 3.4.0
 * @module viewerJS.fullscreen
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false;
    
    viewer.listFilter = function(config) {
        if ( _debug ) {
            console.log( '##############################' );
            console.log( 'viewer.listFilter.init' );
            console.log( '##############################' );
            console.log( 'viewer.listFilter.init: config - ', config );
        }
        
        this.config = config;
        this.enable();
    }
    
    viewer.listFilter.prototype.initListener = function() {
//	    	$(this.config.input).on('input', this.filter.bind(this));
        this.observer = Rx.Observable.fromEvent($(this.config.input), "input")
        .debounce(100)
        .subscribe( event => this.filter(event));
    }
    
    viewer.listFilter.prototype.removeListener = function() {
//	    	$(this.config.input).off('input', this.filter);
        if(this.observer) {
            this.observable.unsubscribe();
        }
    }
    
    viewer.listFilter.prototype.filter = function(event) {
    	let value = $(this.config.input).val().trim().toLowerCase();
    	if(value) {    		
    		if(_debug) {
    			console.log("filter for input", value, " in ", $(this.config.elements));
    		}
    		$(this.config.elements).each( (index, element) => {
    			let $element = $(element);
    			let elementText = $element.text().trim().toLowerCase();
    			if(elementText.includes(value)) {
    				$element.show();
    			} else {
    				$element.hide();
    			}
    		});
    	} else {
    		$(this.config.elements).show();
    	}
    }
    
    viewer.listFilter.prototype.unfilter = function() {
    	$(this.config.input).val("");
    	$(this.config.elements).show();
    }
    
    viewer.listFilter.prototype.enable = function() {
        $(this.config.input).show();
        this.initListener();
    }
    
    viewer.listFilter.prototype.disable = function() {
    	$(this.config.input).val("");
    	$(this.config.input).hide();
        this.removeListener();
    }
                
    return viewer;
    
} )( viewerJS || {}, jQuery );
    