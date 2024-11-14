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
 * Module to manage the data table features.
 * 
 * @version 3.2.0
 * @module viewerJS.dataTable
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false;
    var _dataTablePaginator = null;
    var _txtField1 = null;
    var _txtField2 = null;
    var _totalCount = null;
    var _reloadBtn = null;
    var _defaults = {
        dataTablePaginator: '',
        txtField1: '',
        txtField2: '',
        totalCount: '',
        reloadBtn: '',
    };
    
    viewer.dataTable = {
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.dataTable.init' );
                console.log( '##############################' );
                console.log( 'viewer.dataTable.init: config = ', config );
            }
            
            $.extend( true, _defaults, config );
            
            viewer.dataTable.paginator.init();
            
            if ( $( '.column-filter-wrapper' ).length > 0 ) {
                viewer.dataTable.filter.init();
            }
        },
        /**
         * Pagination
         */
        paginator: {
            setupAjax: false,
            init: function() {
                if ( _debug ) {
                    console.log( '---------- dataTable.paginator.init() ----------' );
                }
                
                _dataTablePaginator = $( _defaults.dataTablePaginator );
                _txtField1 = $( _defaults.txtField1 );
                _txtField2 = $( _defaults.txtField2 );
                _totalCount = $( _defaults.totalCount );
                _reloadBtn = $( _defaults.reloadBtn );
                
                _txtField1.on( 'click', function() {
                    $( this ).hide();
                    viewer.dataTable.paginator.inputFieldHandler();
                } );
                
                _totalCount.on( 'click', function() {
                    _txtField1.hide();
                    viewer.dataTable.paginator.inputFieldHandler();
                } );
                
                /*
                 * AJAX Eventlistener
                 */
                if ( !this.setupAjax ) {
                	viewerJS.jsfAjax.begin.subscribe(e => {
                		if ( _defaults.dataTablePaginator.length > 0 ) {
                			viewer.dataTable.paginator.init();
                		}
                	});
                	viewerJS.jsfAjax.success.subscribe(e => {
                		if ( _defaults.dataTablePaginator.length > 0 ) {
                			if ( _txtField1 !== null && _txtField2 !== null ) {
                                _txtField1.off();
                                _txtField2.off();
                            }
                		}
                	});
                    this.setupAjax = true;
                }
            },
            inputFieldHandler: function() {
                if ( _debug ) {
                    console.log( '---------- dataTable.paginator.inputFieldHandler() ----------' );
                }
                
                _txtField2.show().find( 'input' ).focus().select();
                
                _txtField2.find( 'input' ).on( 'blur', function() {
                    $( this ).hide();
                    _txtField1.show();
                    _reloadBtn.click();
                } );
                
                _txtField2.find( 'input' ).on( 'keypress', function( event ) {
                    if ( event.keyCode == 13 ) {
                        _reloadBtn.click();
                    }
                    else {
                        return;
                    }
                } );
            },
        },
        /**
         * Filter
         */
        filter: {
            setupAjax: false,
            init: function() {
                if ( _debug ) {
                    console.log( '---------- dataTable.filter.init() ----------' );
                }
                
                $( '#adminAllUserForm' ).on( 'submit', function( event ) {
                    event.preventDefault();
                    
                    $( '.column-filter-wrapper' ).find( '.btn-filter' ).click();
                } );
                
                /*
                 * AJAX Eventlistener
                 */
                if ( !this.setupAjax ) {
                
                	viewerJS.jsfAjax.success.subscribe(e => {
                		if ( _defaults.dataTablePaginator.length > 0 ) {
                			viewer.dataTable.filter.init();
                		}
                	});
                    this.setupAjax = true;
                }
            },
        },
    };
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
