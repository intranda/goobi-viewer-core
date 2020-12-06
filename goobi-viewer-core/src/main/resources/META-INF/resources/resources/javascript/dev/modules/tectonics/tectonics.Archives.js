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

            jQuery(document).ready(function($) {
            	// Sticky right side of archives view
            	$('.tec-archives__right-side').hcSticky({
            		stickTo: $('.tec-archives__wrapper')[0],
            		top: 80,
            		bottom: 20
            	});
            	
            	// Sticky left side of archives view
            	$('.tec-archives__left-side').hcSticky({
            		stickTo: $('.tec-archives__wrapper')[0],
            		top: 80,
            		bottom: 20
            	});

            	/* check search field for input value and show clear button */
                if(!$('.tec-archives__search-input').val() == ''){
            		$('.tec-archives__search-clear').show();
                }
            	$('.tec-archives__search-clear').click(function(){
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

            	 $('.tec-archives__search-input').on('input', function () {
            	     // Reset the timer while still typing
            	     clearTimeout(timeSearchInputField);

            	     timeSearchInputField = setTimeout(function() {
            	         // submit search query
            	    	 $('.tec-archives__search-submit-button').click();
            	     }, 1300);
            	 });
            	
            });            
            
        }
    };


    return viewer;
} )( viewerJS || {}, jQuery );
