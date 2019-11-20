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
    var _userComments = {};
        
    viewer.userLogin = {
    	/**
    	 * @description Method to initialize the user login module.
    	 * @method init
    	 * */
         init : function() {
           	if ( _debug ) {
           		console.log( 'INIT: viewerJS.userLogin' );
           	}
            	
          	// set comments object to session storage
           	if ( sessionStorage.getItem( 'userComments' ) == undefined || sessionStorage.getItem( 'userComments' ) === null ) {
           		sessionStorage.setItem( 'userComments', JSON.stringify( _userComments ) );           		
           	}
            	
           	// toggle login
           	$( 'body' ).on( 'click', '[data-toggle="login"]', function() {
           		if ( $( this ).attr( 'data-target' ) ) {
           			_setUserCommentsStatus( $( this ).attr( 'data-target' ) );
           			
           			$( '#userLogin' ).addClass( 'active' );
           			$( 'html' ).addClass( 'no-overflow' );
           		}
           		else {
           			_unsetUserCommentsStatus();
           			
           			$( '#userLogin' ).addClass( 'active' );     
           			$( 'html' ).addClass( 'no-overflow' );
           		}
           	} );
            	
           	// hide login by clicking on body
           	$( 'body' ).on( 'click', '#userLogin > .fa-times', function( event ) {
           		_unsetUserCommentsStatus();
           		$( '#userLogin' ).removeClass( 'active' );
           		$( 'html' ).removeClass( 'no-overflow' );
           	} );
           	
           	// jump to user comments target if set
           	_jumpToComments();
            	
           	// toggle retrieve account
           	$( 'body' ).on( 'click', '[data-open="retrieve-account"]', function() {
           		$( '#userLoginSelectLoginWrapper, #loginType, #loginTypeCreateAccount, #userLoginOpenId' ).hide();
        		$( '#loginTypeRetrieveAccount' ).show();
        		$( '[id*="userEMailToRetrieve"]' ).focus();
           	} );
           	$( 'body' ).on( 'click', '[data-close="retrieve-account"]', function() {
           		$( '#loginTypeExternal, #loginTypeRetrieveAccount, #loginTypeCreateAccount' ).hide();
           		$( '#userLoginSelectLoginWrapper, #loginType, #userLoginOpenId' ).show();
           	} );            	
            	
           	// toggle create account
           	$( 'body' ).on( 'click', '[data-open="create-account"]', function() {
           		$( '#userLoginSelectLoginWrapper, #loginType, #loginTypeRetrieveAccount, #userLoginOpenId, #userLoginFooter' ).hide();
           		$( '#loginTypeCreateAccount' ).show();
           		$( '[id*="userCreateAccountNick"]' ).focus();
           	} );
           	$( 'body' ).on( 'click', '[data-close="create-account"]', function() {
           		$( '#loginTypeExternal, #loginTypeRetrieveAccount, #loginTypeCreateAccount' ).hide();
           		$( '#userLoginSelectLoginWrapper, #loginType, #userLoginOpenId, #userLoginFooter' ).show();
           	} );
        }
    }
    
    /**
     * @description Method to set the user comments status.
     * @method _setUserCommentsStatus
     * @param {String} target The scroll target to the user comments.
     * */
    function _setUserCommentsStatus( target ) {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _setUserCommentsStatus' );
    		console.log( '--> target: ', target );
    	}
    	
    	var comments = JSON.parse( sessionStorage.getItem( 'userComments' ) );
    	
    	comments.set = true;
		comments.target = target;
		
		sessionStorage.setItem( 'userComments', JSON.stringify( comments ) );
    }
    
    /**
     * @description Method to unset the user comments status.
     * @method _unsetUserCommentsStatus
     * */
    function _unsetUserCommentsStatus() {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _unsetUserCommentsStatus' );
    	}
    	
    	var comments = JSON.parse( sessionStorage.getItem( 'userComments' ) );
    	
    	comments.set = false;
    	comments.target = '';
    	
    	sessionStorage.setItem( 'userComments', JSON.stringify( comments ) );
    }
    
    /**
     * @description Method to jump to the user comments target.
     * @method _jumpToComments
     * */
    function _jumpToComments() {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _jumpToComments' );
    	}
    	
    	var comments = JSON.parse( sessionStorage.getItem( 'userComments' ) );
    	
    	if ( comments.set && $( '#userCommentAdd' ).length > 0 ) {
    		location.hash = comments.target;
    		$( '#userCommentAdd' ).focus();
    	}
    	else {
    		_unsetUserCommentsStatus();
    	}
    }
        
    return viewer;
    
} )( viewerJS || {}, jQuery );
