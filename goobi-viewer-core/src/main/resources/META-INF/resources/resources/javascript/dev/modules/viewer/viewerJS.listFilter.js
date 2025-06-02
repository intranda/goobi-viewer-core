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
 * @version 3.4.0
 * @module viewerJS.listFilter
 * @requires jQuery
 */
var viewerJS = (function (viewer) {
    'use strict';

    var _debug = false;

    viewer.listFilter = function (config) {
        if (_debug) {
            console.log('##############################');
            console.log('viewer.listFilter.init');
            console.log('##############################');
            console.log('viewer.listFilter.init: config - ', config);
        }

        this.config = config;
        this.enable();
        
        // toggle filter input
        
        this.config.inputToggle.on("click", event => {
        	event.stopImmediatePropagation();

        	var $input = this.config.input;
        	
        	if ( !$input.hasClass( 'in' ) ) {
        		this.resetFilters();        		
        	}
        	
        	$input.toggleClass( 'in' ).focus();        	
        	
        	if ( $input.val() !== '' ) {
        		$input.val( '' ).trigger( 'input' );
        	}
		})
		
		this.config.header.on("click", event => {
			console.log('clicked on heading');
        	event.stopImmediatePropagation();
        	
        	var $input = this.config.input;
        	
        	this.resetFilters();
        	
        	$input.toggleClass( 'in' ).focus();
        	
        	if ( $input.val() !== '' ) {
        		$input.val( '' ).trigger( 'input' );
        	}
        } );

        this.config.input.on("keyup", event => {
			if(event.keyCode == 27) {
				this.resetFilters();
			}
		});
        
        // reset filter on body click
        $( 'body' ).on( 'click', event => {
        	if ( $( '.widget-search-facets__filter-input' ).hasClass( 'in' ) ) {
        		if ( event.target.id == 'searchListFacetsWrapper' || $( event.target ).closest( '#searchListFacetsWrapper' ).length ) {
                    return;
                }
                else {
                	this.resetFilters();
                }
        	}
        } );
        
    }

    /**
     * @description Method to reset all other filter when clicking another one.
     * @method _resetFilters
     * */
    viewer.listFilter.prototype.resetFilters = function() {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _resetFilters' );
    	}
		if ( this.config.input.hasClass( 'in' ) ) {
			this.config.input.removeClass( 'in' ).val( '' ).trigger( 'input' );
			this.config.elements.show();
		}
    }

    // set event for input
    viewer.listFilter.prototype.initListener = function () {
        this.observer = rxjs.fromEvent( this.config.input, 'input' )
            .pipe(rxjs.operators.debounceTime( 200 ))
            .subscribe( event => this.filter( event ) );
    }

    // remove events
    viewer.listFilter.prototype.removeListener = function () {
        if ( this.observer ) {
            this.observable.unsubscribe();
        }
    }

    // filter results
    viewer.listFilter.prototype.filter = function ( event ) {
        let value = $( this.config.input ).val().trim().toLowerCase();
        
        if ( value ) {
            if ( _debug ) {
                console.log( "filter for input", value, " in ", $( this.config.elements ) );
            }
            
            $( this.config.elements ).each( ( index, element ) => {
                let $element = $( element );
                let elementText = $element.find("a").text().trim().toLowerCase();
                
                if ( elementText.includes( value ) ) {
                    $element.show();
                } else {
                    $element.hide();
                }
            } );
        } 
        else {
            $( this.config.elements ).show();
        }
    }

    // unfilter elements
    viewer.listFilter.unfilter = function () {
        $( this.config.input ).val( '' );
        $( this.config.elements ).show();
    }

    // enable filter
    viewer.listFilter.prototype.enable = function () {
        $( this.config.wrapper ).show();
        this.initListener();
    }

    // disable filter
    viewer.listFilter.prototype.disable = function () {
        $( this.config.input ).val( '' );
        $( this.config.wrapper ).hide();
        this.removeListener();
    }

    return viewer;

} )( viewerJS || {}, jQuery );
    