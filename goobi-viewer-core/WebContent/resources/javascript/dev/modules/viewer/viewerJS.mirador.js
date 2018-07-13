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
 * Module which manages the download view.
 * 
 * @version 3.4.0
 * @module viewerJS.mirador
 * @requires jQuery
 * 
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    // variables
    var _debug = true;
    var _defaults = {
    	root: '',
    	userLoggedIn: false,
    };
    
    viewer.mirador = {
        /**
         * Method to initialize the mirador viewer.
         * 
         * @method init
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.mirador.init' );
                console.log( '##############################' );
                console.log( 'viewer.mirador.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );
            
            console.log(  );
            
            // ckeck login status
            if ( _defaults.userLoggedIn ) {
            	_getMiradorObjects( _defaults.root ).then( function( elements ) {        			
        			$( function() {
						Mirador( elements );
					});
        		}).fail(function(error) {
        			console.error('ERROR - _getMiradorObjects: ', error.responseText);
        		});
            }
            else {
            	_getMiradorSessionObjects( _defaults.root ).then( function( elements ) {
            		$( function() {
						Mirador( elements );
					});
				}).fail(function(error) {
        			console.error('ERROR - _getMiradorSessionObjects: ', error.responseText);
        		});
            }
        }
    };
    
    /* ######## GET (READ) ######## */
    /**
	 * Method to get the mirador objects.
	 * 
	 * @method _getMiradorObjects
	 * @param {String} root The application root path.
	 * @returns {Object} An JSON-Object which contains all session elements.
	 */
	function _getMiradorObjects( root ) {
		if ( _debug ) { 
			console.log( '---------- _getSessionElementCount() ----------' );
			console.log( '_getSessionElementCount: root - ', root );
		}

		var promise = Q($.ajax({
			url : root + '/rest/bookshelves/user/mirador/32/',
			type : "GET",
			dataType : "JSON",
			async : true
		}));

		return promise
	}
	/**
	 * Method to get the mirador session objects.
	 * 
	 * @method _getMiradorSessionObjects
	 * @param {String} root The application root path.
	 * @returns {Object} An JSON-Object which contains all session elements.
	 */
	function _getMiradorSessionObjects( root ) {
		if ( _debug ) { 
			console.log( '---------- _getMiradorSessionObjects() ----------' );
			console.log( '_getMiradorSessionObjects: root - ', root );
		}

		var promise = Q($.ajax({
			url : root + '/rest/bookshelves/session/mirador/',
			type : "GET",
			dataType : "JSON",
			async : true
		}));

		return promise
	}
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
