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
    var _debug = true;
    var _defaults = {
    	lastEditedHash: '',
    	active : true,
    };
    
    viewer.cookieBanner = {
        bannerStatus : true,
   		bannerHash : '',
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
            
            this.config = $.extend(true, {}, _defaults, config );
            if(_debug)console.log("init cookie banner with config", this.config);
            if(this.config.active) {
            
		        // set global variables
		        this.bannerStatus = this.getStoredBannerStatus();
		        this.bannerHash = this.getStoredLastEditedHash();
		        if(_debug)console.log("banner status: ", this.bannerStatus, "; hash: ", this.bannerHash);
		        // set last edit hash
		        if ( this.bannerHash === undefined) {
		        	this.storeLastEditedHash(this.config.lastEditedHash );
		        	this.bannerHash = this.getStoredLastEditedHash();
		        }
				if(_debug)console.log("banner status: ", this.bannerStatus, "; hash: ", this.bannerHash);
				
            	// get/set banner status
            	if ( this.bannerStatus === undefined ) {
            		if(_debug)console.log("banner status unset");
            		this.storeBannerStatus( true );
            		this.bannerStatus = this.getStoredBannerStatus();
            		$( '#cookieBanner' ).show();
            		this.hideBanner();
            	} else {
            		// check last edited hash
            		if ( this.config.lastEditedHash === this.bannerHash ) {
            		if(_debug)console.log("last edited hash equals stored value. Banner status = ", this.bannerStatus);
            			// check banner status
            			if ( this.bannerStatus ) {
            				$( '#cookieBanner' ).show();
            				this.hideBanner();
            			}
            			else {
            				$( '#cookieBanner' ).hide();            	
            			}            			
            		}
            		else {
            			this.storeBannerStatus( true );
            			$( '#cookieBanner' ).show();
        				this.hideBanner();
            		}
            		
            	}            	
            } else {
            	$( '#cookieBanner' ).hide();
            }
        },
        getStoredLastEditedHash() {
        	let string = localStorage.getItem( 'cookieBannerHash' );
        	if(string) {
        		return parseInt(string);
        	} else {
        		return undefined;
        	}
        },
        getStoredBannerStatus() {
        	let string = localStorage.getItem( 'cookieBannerStatus' );
        	if(!string) {
        		return undefined;
        	} else {
        		return string.toLowerCase() == "true";
			}        	
        },
        storeLastEditedHash(hash) {
        	localStorage.setItem( 'cookieBannerHash', String(hash) );
        },
        storeBannerStatus(status) {
        	localStorage.setItem( 'cookieBannerStatus', String(status) );
        },
	    hideBanner() {
	    	if ( _debug ) {
	    		console.log( 'EXECUTE: _hideBanner' );
	    	}
	    	
	    	$( '[data-set="cookie-banner"]' ).off().on( 'click', function() {
				$( '.cookie-banner__info' ).slideUp( function() {
					$( '#cookieBanner' ).fadeOut( 'fast' );
					localStorage.setItem( 'cookieBannerStatus', false );
					localStorage.setItem( 'cookieBannerHash', this.config.lastEditedHash );
					this.config.bannerStatus = this.getStoredBannerStatus();
					this.config.bannerHash = this.getStoredLastEditedHash();
					if(_debug)console.log("accepted cookie banner. Set banner status to ", this.config.bannerStatus, ", hash to ", this.config.bannerHash);
				}.bind(this) );            			
			}.bind(this) );
	    }
    };
    

    
    return viewer;
    
} )( viewerJS || {}, jQuery );
