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
 * @description Module which handles the cookie banner. 
 * @version 3.4.0
 * @module viewerJS.cookieBanner
 * @requires jQuery
 * 
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    // variables
    var _debug = false;
    var _bannerStatus = true;
    var _bannerHash = '';
    var _isWhitelisted = false;
    var _defaults = {
    	whiteList: [],
    	lastEditedHash: '',
    };
    
    viewer.cookieBanner = {
        /**
         * Method to initialize the cookie banner.
         * 
         * @method init
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.cookieBanner.init' );
                console.log( '##############################' );
                console.log( 'viewer.cookieBanner.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );
            
            // set global variables
            _bannerStatus = localStorage.getItem( 'cookieBannerStatus' );
            _bannerHash = localStorage.getItem( 'cookieBannerHash' );
            
            // set last edit hash
            if ( _bannerHash == undefined || _bannerHash == '' ) {
            	localStorage.setItem( 'cookieBannerHash', _defaults.lastEditedHash );
            	_bannerHash = localStorage.getItem( 'cookieBannerHash' );
            }
            
            // hide banner if page is on whitelist
            for ( var i = 0; i < _defaults.whiteList.length; i++ ) {            	
            	if ( $( '.' + _defaults.whiteList[i] ).length > 0 ) {
            		_isWhitelisted = true;
            		$( '#cookieBanner' ).hide();
            		
            		break;
            	}
            }
            
            // check if page is whitelisted
            if ( !_isWhitelisted ) {
            	// get/set banner status
            	if ( _bannerStatus == undefined || _bannerStatus == '' ) {
            		localStorage.setItem( 'cookieBannerStatus', true );
            		_bannerStatus = localStorage.getItem( 'cookieBannerStatus' );
            		$( '#cookieBanner' ).show();
            		_hideBanner();
            	}
            	else {
            		// check last edited hash
            		if ( _defaults.lastEditedHash === _bannerHash ) {
            			// check banner status
            			if ( _bannerStatus === 'true' ) {
            				$( '#cookieBanner' ).show();
            				_hideBanner();
            			}
            			else {
            				$( '#cookieBanner' ).hide();            	
            			}            			
            		}
            		else {
            			localStorage.setItem( 'cookieBannerStatus', true );
            			$( '#cookieBanner' ).show();
        				_hideBanner();
            		}
            		
            	}            	
            }
        }
    };
    
    /**
     * @description Method to hide the banner.
     * @method _hideBanner
     * */
    function _hideBanner() {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _hideBanner' );
    	}
    	
    	$( '[data-set="cookie-banner"]' ).off().on( 'click', function() {
			$( '.cookie-banner__info' ).slideUp( function() {
				$( '#cookieBanner' ).fadeOut( 'fast' );
				localStorage.setItem( 'cookieBannerStatus', false );
				localStorage.setItem( 'cookieBannerHash', _defaults.lastEditedHash );
				_bannerStatus = localStorage.getItem( 'cookieBannerStatus' );
				_bannerHash = localStorage.getItem( 'cookieBannerHash' );
			} );            			
		} );
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
