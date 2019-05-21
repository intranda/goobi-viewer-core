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
 * @description Module to manage the user login options.
 * @version 3.4.0
 * @module viewerJS.userLogin
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false;
        
    viewer.userLogin = {
    		/**
    		 * @description Method to initialize the user login module.
    		 * @method init
    		 * */
            init : function() {
            	if ( _debug ) {
            		console.log( 'INIT: viewerJS.userLogin' );
            	}
            	
            	// toggle login
            	$( 'body' ).on( 'click', '[data-toggle="login"]', function() {
            		$( '#userLogin' ).addClass( 'active' );
            	} );
            	
            	// hide login by clicking on body
            	$( 'body' ).on( 'click', '#userLogin > .fa-times', function( event ) {
            		$( '#userLogin' ).removeClass( 'active' );
            	} );
            	
            	// toggle retrieve account
            	$( 'body' ).on( 'click', '[data-open="retrieve-account"]', function() {
            		$( '#userLoginSelectLoginWrapper, #loginType, #loginTypeCreateAccount' ).hide();
        			$( '#loginTypeRetrieveAccount' ).show();
        			$( '[id*="userEMailToRetrieve"]' ).focus();
            	} );
            	$( 'body' ).on( 'click', '[data-close="retrieve-account"]', function() {
            		$( '#loginTypeExternal, #loginTypeRetrieveAccount, #loginTypeCreateAccount' ).hide();
            		$( '#userLoginSelectLoginWrapper, #loginType' ).show();
            	} );            	
            	
            	// toggle create account
            	$( 'body' ).on( 'click', '[data-open="create-account"]', function() {
            		$( '#userLoginSelectLoginWrapper, #loginType, #loginTypeRetrieveAccount' ).hide();
            		$( '#loginTypeCreateAccount' ).show();
            		$( '[id*="userCreateAccountNick"]' ).focus();
            	} );
            	$( 'body' ).on( 'click', '[data-close="create-account"]', function() {
            		$( '#loginTypeExternal, #loginTypeRetrieveAccount, #loginTypeCreateAccount' ).hide();
            		$( '#userLoginSelectLoginWrapper, #loginType' ).show();
            	} );            	
            }
    }
        
    return viewer;
    
} )( viewerJS || {}, jQuery );
