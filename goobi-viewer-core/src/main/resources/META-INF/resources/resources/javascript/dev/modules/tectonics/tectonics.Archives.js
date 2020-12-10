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
 */

var viewerJS = ( function( viewer ) {
    'use strict';

    var _debug = false;
 
    viewer.tectonicsArchivesView = {
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.tectonicsArchivesView.init' );
                console.log( '##############################' );
                console.log( 'viewer.tectonicsArchivesView.init: config - ', config );
            }

            jQuery(document).ready(($) => {

                this.initHcStickyWithChromeHack();
                viewerJS.jsfAjax.success
                .subscribe(e => this.refreshStickyWithChromeHack())

                
            	/* check search field for input value and show clear button */
                if(!$('.tec-archives__search-input').val() == ''){
            		$('.tec-archives__search-clear').show();
                }
            	$('body').on("click", '.tec-archives__search-clear', function(){
            		/* clear value on click*/
                $('.tec-archives__search-input').val("");
            	    /* trigger empty search on click */
            	    $('.tec-archives__search-submit-button').click();
            	});
            	
//            	if($('.admin__table-entry').length == 0) {
//            		$('.admin__table-content').append('<br/><p class="">#{msg.hitsZero}</p>');
//            	}
            	
            	
            	 // auto submit search after typing
            	 let timeSearchInputField = 0;

            	 $('body').on('input', '.tec-archives__search-input', function () {
            	     // Reset the timer while still typing
            	     clearTimeout(timeSearchInputField);

            	     timeSearchInputField = setTimeout(function() {
            	         // submit search query
            	    	 $('.tec-archives__search-submit-button').click();
            	     }, 1300);
            	 });
            	 
            	 // toggle text-tree view from stairs to one line
            	 $('body').on("click", '.tec-archives__text-tree', function() {
            		 $('.tec-archives__text-tree').toggleClass('-showAsOneLine');
            	 });
            	
            });            
            
        },
        
        /**
         * In chome with small window size (1440x900) hcSticky breaks on ajax reload if the page is scrolled
         * all the way to the button. To prevent this we quickly scroll to the top, refresh hcSticky and then scroll back down.
         * The scolling appears to be invisible to the user, probably because it is reset before actually being carried out
         */
        refreshStickyWithChromeHack: function() {
            let currentScrollPosition = $('html').scrollTop();
            $('html').scrollTop(0);
            this.refreshHcSticky();
            if(currentScrollPosition) {
                $('html').scrollTop(currentScrollPosition);
            }
        },
        
        refreshHcSticky: function() {
            if(_debug)console.log("update hc sticky");

//            $('.tec-archives__left-side, .tec-archives__right-side').hcSticky('refresh');
            $('.tec-archives__left-side, .tec-archives__right-side').hcSticky('update', {
                stickTo: $('.tec-archives__wrapper')[0],
                top: 80,
                bottom: 20,
                responsive: {
                    993: {
                      disable: true
                    }
                }
           });
        },
        
        /**
         * In chome with small window size (1440x900) hcSticky breaks on page load if the view was previously scrolled
         * all the way to the button. To prevent this we scroll 5 px up before refreshing hcSticky.
         * The scolling appears to be invisible to the user, probably because it is reset before actually being carried out
         */
        initHcStickyWithChromeHack: function() {
            let currentScrollPosition = $('html').scrollTop();
            $('html').scrollTop(currentScrollPosition-5);
            this.initHcSticky();
//            if(currentScrollPosition) {
//                $('html').scrollTop(currentScrollPosition);
//            }
        },

        
        initHcSticky: function() {
            if(_debug)console.log("init hc sticky");
                        
            // Sticky right side of archives view
            $('.tec-archives__right-side').hcSticky({
                stickTo: $('.tec-archives__wrapper')[0],
                top: 80,
                bottom: 20,
                responsive: {
                    993: {
                      disable: true
                    }
                }
            });
            
            // Sticky left side of archives view
            $('.tec-archives__left-side').hcSticky({
                stickTo: $('.tec-archives__wrapper')[0],
                top: 80,
                bottom: 20,
                responsive: {
                    993: {
                      disable: true
                    }
                }
            });
        }
    };


    return viewer;
} )( viewerJS || {}, jQuery );
