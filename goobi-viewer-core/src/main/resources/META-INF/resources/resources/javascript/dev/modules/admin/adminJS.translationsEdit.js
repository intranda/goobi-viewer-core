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
 * @version 21.4
 * @module adminJS.translationsEdit
 * @requires jQuery
 * @description Module for the page resources/admin/views/adminTranslationsEdit.xhtml
 */
var adminJS = ( function( admin ) {
    'use strict';
    
    var _debug = false;

    admin.translationsEdit = {
        /**
         * @description Method which initializes the admin sidebar module.
         * @method init
         */
        init: function() {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'adminJS.translationsEdit.init' );
                console.log( '##############################' );
            }
            
            viewerJS.jsfAjax.success.subscribe(function() {
            	/* SIDEBAR STICKY */
           		$('.-sticky').hcSticky('refresh', {});
           		
    			/* check if translations area contains zzz on ajax load */
        		$('.admin__translations-textarea').keyup(function() {
            		$('.admin__translations-textarea').each(function(i, obj) {
            		    if ($(this).val().indexOf('zzz') > -1 || (!$(this).val())) {
            		        $(this).parent().addClass('admin__form-input-highlight');
            		        $(this).parent().siblings('.admin__form-label').addClass('admin__form-label-highlight');
            		    }
            		    else {
            		        $(this).parent().removeClass('admin__form-input-highlight');
            		        $(this).parent().siblings('.admin__form-label').removeClass('admin__form-label-highlight');
            		    }
            		});
            		
        			/* check if global msg key identical on msg key up*/
    				var globalKey = $(this).parents('.admin__translations-fields').find('.admin__translations-global-key').text();

	    			if ((globalKey) != ($(this).val()) && ((globalKey) != "")){
	    			  $(this).parents('.admin__translations-fields').find('.admin__translations-global-hint').fadeIn('fast');
	    			}
	    			else {
	    				$(this).parents('.admin__translations-fields').find('.admin__translations-global-hint').fadeOut('fast');
	    			}
            		
      			});
    			
                /* show global msg keys if it differs on ajax load */
        			$('.admin__translations-fields').each(function(i, obj) {
        				var globalKey = $(this).find('.admin__translations-global-key').text();
        			if(_debug)console.log(globalKey);
       			  	if (!(globalKey) == "" && (globalKey) != ($(this).find('.admin__translations-textarea').val())) {
       				    $(this).find('.admin__translations-global-hint').fadeIn('fast');
       				    if(_debug)console.log('erfolg');
       				  }
        			else {
        				$(this).find('.admin__translations-global-hint').fadeOut('fast');
        			}

        			}); 
                });
  
                /* show global msg keys if it differs on page load */
   			$('.admin__translations-fields').each(function(i, obj) {
   				var globalKey = $(this).find('.admin__translations-global-key').text();
   			if(_debug)console.log(globalKey);

       		  if ((globalKey) !== "" && (globalKey) != ($(this).find('.admin__translations-textarea').val())) {
       		    $(this).find('.admin__translations-global-hint').fadeIn('fast');
       		    if(_debug)console.log('erfolg');
       		  }
   			else {
   				$(this).find('.admin__translations-global-hint').fadeOut('fast');
   			}

   			}); 

			/* check on any key up */
    		$('.admin__translations-textarea').keyup(function() {
    			
    			/* check if global msg key identical on msg key up*/
				var globalKey = $(this).parents('.admin__translations-fields').find('.admin__translations-global-key').text();

    			if ((globalKey) != ($(this).val()) && ((globalKey) != "")){
    			  $(this).parents('.admin__translations-fields').find('.admin__translations-global-hint').fadeIn('fast');
    			}
    			else {
    				$(this).parents('.admin__translations-fields').find('.admin__translations-global-hint').fadeOut('fast');
    			}

    			/* check if translations area contains zzz on msg key up*/
        		$('.admin__translations-textarea').each(function(i, obj) {
        		    if ($(this).val().indexOf('zzz') > -1 || (!$(this).val())) {
        		        $(this).parent().addClass('admin__form-input-highlight');
        		        $(this).parent().siblings('.admin__form-label').addClass('admin__form-label-highlight');
        		    }
        		    else {
        		        $(this).parent().removeClass('admin__form-input-highlight');
        		        $(this).parent().siblings('.admin__form-label').removeClass('admin__form-label-highlight');
        		    }
        		});
  			});

    		$( document ).ready(function() {
    			
    			// SEARCH FUNCTION
    			/* check for input value and show clear button */
    	        if(!$('.admin__translations-search-input').val() == ''){
    				$('.admin__translations-search-clear').show();
    	        }
    			$('.admin__translations-search-clear').click(function(){
        				/* clear value on click*/
        		    $('.admin__translations-search-input').val("");
        		    $('.admin__translations-search-clear').hide();	
        			    /* trigger empty search on click */
        			    $('.admin__translations-search-button').click();
    			});
    			
    			if($('.admin__table-entry').length == 0) {
    				$('.admin__table-content').append('<br/><p class="">#{msg.hitsZero}</p>');
    			}
    			
    			$('.admin__translations-search-input').keyup(function() {
        	        if ($('.admin__translations-search-input').val() == ''){
        				$('.admin__translations-search-clear').hide();
            		}
        	        
    			});
    		});
    		
            viewerJS.jsfAjax.success.subscribe(function() {
            	// SEARCH FUNCTION AJAX
    			/* check for input value and show clear button */
    	        if(!$('.admin__translations-search-input').val() == ''){
    				$('.admin__translations-search-clear').show();
    	        }
    			$('.admin__translations-search-clear').click(function(){
        				/* clear value on click*/
        		    $('.admin__translations-search-input').val("");
        		    $('.admin__translations-search-clear').hide();	
        			    /* trigger empty search on click */
        			    $('.admin__translations-search-button').click();
    			});
    			
    			if($('.admin__table-entry').length == 0) {
    				$('.admin__table-content').append('<br/><p class="">#{msg.hitsZero}</p>');
    			}
            });
        }
	} 
	
	return admin;
    
} )( adminJS || {}, jQuery );