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
 * @description This module listens to the jsf ajax event an reloads several events.
 * @version 3.4.0
 * @module viewerJS.jsfAjax
 * @requires jQuery
 */
var viewerJS = ( function ( viewer ) {
    'use strict';

    var _debug = false;
    var _defaults = {};

	const filterAjaxEvents = event => {
		return event.source.dataset.jsfUpdateType !== "ignore";
	}

    viewer.jsfAjax = {
    	/**
    	 * @description Method to initialize the jsf ajax listener.
    	 * @method init 
    	 * */
        begin: new rxjs.Subject().pipe(rxjs.operators.filter(filterAjaxEvents)),
        complete: new rxjs.Subject().pipe(rxjs.operators.filter(filterAjaxEvents)),
        success: new rxjs.Subject().pipe(rxjs.operators.filter(filterAjaxEvents)),
        error: new rxjs.Subject().pipe(rxjs.operators.filter(filterAjaxEvents)),
        handleResponse: function(success, error) {
        	this.complete
		    .pipe(rxjs.operators.first())
		    .subscribe(response => {
		        switch(response.responseCode) {
		            case 200:
		            	if(success) {
				        	success(response);
						}		            	
						break;
					default:
						if(error) {
						    error(response);
						}						
		        }
		    });

        },
    	init: function( config ) {
    		if (_debug) {
    		    console.log( 'Initializing: viewerJS.jsfAjax.init' );
    		    console.log( '--> config = ', config );
    		}
    		
    		$.extend( true, _defaults, config );
    		// listen to jsf ajax event
            if ( typeof jsf !== 'undefined' ) {
                jsf.ajax.addOnEvent( ( data ) => {
                    if ( _debug ) {
                        console.log( 'JSF AJAX - data: ', data );
                    }
                    
                    var ajaxloader = document.getElementById( "AJAXLoader" );
                    switch ( data.status ) {
                        case 'begin': 
                        	if ( ajaxloader ) {
                        		ajaxloader.style.display = 'block';                        		
                        	}
                        	this.begin.next(data);
                        	break;
                        case 'complete':
                        	if ( ajaxloader ) {                        		
                        		ajaxloader.style.display = 'none';
                        	}
                        	this.complete.next(data);
                        	break;
                        case 'success':
                        	// init Bootstrap features
                            viewerJS.helper.initBsFeatures();
                            // init tinyMCE
                            let isTiny = data.responseText.includes("tinyMCE");
                            if(isTiny) {
//                            if ( $( '.tinyMCE' ).length > 0 ) {
                                viewerJS.tinyMce.close();
                                viewerJS.tinyMce.init( viewerJS.tinyConfig );
                            }
                            this.success.next(data);
                            break;
                        case 'error':
                            this.error.next(data);
                            break;
                            
                    }
                });
            }
    	}
    }

    return viewer;

} )( viewerJS || {}, jQuery );
    