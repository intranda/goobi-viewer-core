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
 * Module which sets up the functionality for search advanced.
 * 
 * @version 3.2.0
 * @module viewerJS.searchAdvanced
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false;
    var _advSearchValues = {};
    var _defaults = {
        loaderSelector: '.search-advanced__loader',
        inputSelector: '.value-text',
        resetSelector: '.reset',
    };
    
    viewer.searchAdvanced = {
        /**
         * Method to initialize the search advanced features.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.searchAdvanced.init' );
                console.log( '##############################' );
                console.log( 'viewer.searchAdvanced.init: config = ', config );
            }
            
            $.extend( true, _defaults, config );
            
            // init bs tooltips
            $( '[data-toggle="tooltip"]' ).tooltip( {
                trigger : 'hover'
            } );

			sessionStorage.setItem('advSearchValues', JSON.stringify(_advSearchValues));

			// set search values
			_setAdvSearchValues();
			_resetValue();

			// ajax eventlistener

            viewerJS.jsfAjax.begin.subscribe(e => {
                $(_defaults.loaderSelector).show();
            });
            viewerJS.jsfAjax.success.subscribe(e => {
                	// init bs tooltips
					$( '[data-toggle="tooltip"]' ).tooltip( {
			            trigger : 'hover'
			        } );

					// set search values
					_setAdvSearchValues();
					_getAdvSearchValues();
					_resetValue();

					// set disabled state to select wrapper
					$('select').each(function() {
						if ($(this).attr('disabled') === 'disabled') {
							$(this).parent().addClass('disabled');
						} else {
							$(this).parent().removeClass('disabled');
						}
					});

					// hide loader
					$(_defaults.loaderSelector).hide();
            });
		},
    };
    
    function _setAdvSearchValues() {
        if ( _debug ) {
            console.log( '---------- _setAdvSearchValues() ----------' );
        }

        $( _defaults.inputSelector ).off().on( 'keyup, change', function() {
        	var currId = $( this ).attr( 'id' );
        	var currVal = $( this ).val();
        	var currValues = JSON.parse( sessionStorage.getItem( 'advSearchValues' ) );
        	
        	// check if values are in local storage
        	if ( !currValues.hasOwnProperty( currVal ) ) {
        		currValues[ currId ] = currVal;
        	}
        	else {
        		return false;
        	}
        	
        	// write values to local storage
        	sessionStorage.setItem( 'advSearchValues', JSON.stringify( currValues ) );
        } );
    }
    
    function _getAdvSearchValues() {
        if ( _debug ) {
            console.log( '---------- _getAdvSearchValues() ----------' );
        }
        
        var values = JSON.parse( sessionStorage.getItem( 'advSearchValues' ) );

        $.each( values, function( id, value ) {
            $( '#' + CSS.escape(id) ).val( value );
        } );
    }
    
    function _resetValue() {
        if ( _debug ) {
            console.log( '---------- _resetValue() ----------' );
        }
        
        $( _defaults.resetSelector ).off().on( 'click', function() {
            var inputId = $( this ).parents( '.input-group' ).find( 'input' ).attr( 'id' );
            var currValues = JSON.parse( sessionStorage.getItem( 'advSearchValues' ) );
            
            // delete value from local storage object
            if ( currValues.hasOwnProperty( inputId ) ) {
                delete currValues[ inputId ];
            }
            
            // write new values to local storage
            sessionStorage.setItem( 'advSearchValues', JSON.stringify( currValues ) );
            
            $( this ).parents( '.input-group' ).find( 'input' ).val( '' );
        } );
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
