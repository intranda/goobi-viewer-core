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
 * Module which sets up the sorting functionality for search list.
 * 
 * @version 3.2.0
 * @module viewerJS.searchSortingDropdown
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false;
    var _selectedSorting = '';
    var _dataSortFieldState = '';
    var _currUrl = '';
    var _valueUrl = '';
    var _checkValUrl = '';
    var _dataSortField = '';
    var _defaults = {
        select: '#sortSelect',
    };
    
    viewer.searchSortingDropdown = {
        /**
         * Method to initialize the search sorting widget.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {Object} config.select An jQuery object which holds the sorting menu.
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.searchSortingDropdown.init' );
                console.log( '##############################' );
                console.log( 'viewer.searchSortingDropdown.init: config = ', config );
            }
            
            $.extend( true, _defaults, config );
            
            if ( viewer.localStoragePossible ) {
                _selectedSorting = localStorage.dataSortField;
                _currUrl = location.href;
                
                // get selected sorting type from local storage an set the menu option to
                // selected
                if ( _selectedSorting !== '' || _selectedSorting !== 'undefinded' ) {
                    $( _defaults.select ).children( 'option' ).each( function() {
                        _dataSortFieldState = $( this ).attr( 'data-sortField' );
                        _checkValUrl = $( this ).val();
                        
                        if ( _dataSortFieldState === _selectedSorting && _checkValUrl === _currUrl ) {
                            $( this ).attr( 'selected', 'selected' );
                        }
                    } );
                }
                
                // get the sorting URL from the option value and reload the page on change
                $( _defaults.select ).on( 'change', function() {
                    _valueUrl = $( this ).val();
                    _dataSortField = $( this ).children( 'option:selected' ).attr( 'data-sortField' );
                    
                    if ( _valueUrl !== '' ) {
                        // save current sorting state to local storage
                        if ( typeof ( Storage ) !== "undefined" ) {
                            localStorage.dataSortField = _dataSortField;
                        }
                        else {
                            console.info( 'Local Storage is not defined. Current sorting state could not be saved.' );
                            
                            return false;
                        }
                        
                        window.location.href = _valueUrl;
                    }
                } );
            }
            else {
                return false;
            }
        },
    };
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
