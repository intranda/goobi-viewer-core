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
 * Module to edit comments in user generated content.
 * 
 * @version 3.4.0
 * @module viewerJS.userComments
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false;
    var _defaults = {
        commentEditLoader: '.user-comments__comment-content-loader'
    };
    
    viewer.userComments = {
        /**
         * Method which initializes all required events to edit comments.
         * 
         * @method init
         * @example
         * 
         * <pre>
         * $( document ).ready( function() {
         *     viewerJS.userComments.init();
         * } );
         * </pre>
         */
        init: function() {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.userComments.init' );
                console.log( '##############################' );
            }
            
            // clear texarea for new comments
            if ( $( '#userCommentAdd' ).val() !== '' ) {
                $( '#userCommentAdd' ).focus().val( '' );
            }
            
            // edit comment
            $('[data-edit="comment"]').on('click', function() {        		
        		$(this).parent().removeClass('in');
        		$(this).parents('.user-comments__comment-content-options').find('.user-comments__comment-content-options-cancel, .user-comments__comment-content-options-save').addClass('in');
        		$(this).parents('.user-comments__comment-content').find('.user-comments__comment-content-options-text').removeClass('in');
        		$(this).parents('.user-comments__comment-content').find('.user-comments__comment-content-options-text-edit').addClass('in');
        		$(this).parents('.user-comments__comment-content').find('.user-comments__comment-content-options-text-edit textarea').focus();
        	});
        	
        	// cancel edit
            $('[data-edit="cancel"]').on('click', function() {
        		$(this).parents('.user-comments__comment-content-options').find('.user-comments__comment-content-options-cancel, .user-comments__comment-content-options-save').removeClass('in');
        		$(this).parents('.user-comments__comment-content-options').find('.user-comments__comment-content-options-edit').addClass('in');
        		$(this).parents('.user-comments__comment-content').find('.user-comments__comment-content-options-text').addClass('in');
        		$(this).parents('.user-comments__comment-content').find('.user-comments__comment-content-options-text-edit').removeClass('in');
        	});
            
            // show/hide loader on AJAX calls
            $('[data-edit="save"]').on('click', function() {
            	window.currContent = $( this ).parents('.user-comments__comment-content');
            	window.currContent.find( _defaults.commentEditLoader ).show();
        	});
            
            if ( $( _defaults.commentEditLoader ).is(":visible") ) {
	            viewer.dataTable.filter.init();
	            viewerJS.jsfAjax.success.subscribe(e => window.currContent.find( _defaults.commentEditLoader ).hide());           	
            }
            
        }
    };
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
