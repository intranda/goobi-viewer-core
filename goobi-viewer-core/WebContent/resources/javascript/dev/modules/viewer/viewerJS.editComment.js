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
 * @version 3.2.0
 * @module viewerJS.editComment
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false;
    var _defaults = {
        commentCount: 0,
        editBtnCount: 0,
        deleteBtnCount: 0,
        deleteModalCount: 0,
        btnId: null,
        commentText: null
    };
    
    viewer.editComment = {
        /**
         * Method which initializes all required events to edit comments.
         * 
         * @method init
         * @example
         * 
         * <pre>
         * $( document ).ready( function() {
         *     viewerJS.editComment.init();
         * } );
         * </pre>
         */
        init: function() {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.editComment.init' );
                console.log( '##############################' );
            }
            
            // clear texarea for new comments
            if ( $( 'textarea[id*="newCommentInput"]' ).val() !== '' ) {
                $( 'textarea[id*="newCommentInput"]' ).focus().val( '' );
            }
            
            // eventlisteners
            $( '.comment-edit' ).on( 'click', function() {
                // hide add new comment field
                $( '#newCommentField' ).fadeOut();
                
                // get button id
                _defaults.btnId = $( this ).attr( 'id' ).replace( 'commentEditBtn-', '' );
                
                // show textfield to edit comment and hide comment
                $( '#addComment' ).hide();
                $( '#commentEditComment-' + _defaults.btnId ).prev().hide();
                $( '#commentEditComment-' + _defaults.btnId ).show();
                
                // hide edit button
                $( '.comment-edit' ).hide();
            } );
            
            $( '.comment-abort' ).on( 'click', function() {
                // show add new comment field
                $( '#newCommentField' ).fadeIn();
                
                // show edit button and comment
                $( '.comments-comment-text' ).show();
                $( '.comment-edit' ).show();
                $( '#addComment' ).show();
                
                // hide textfield to edit comment
                $( this ).parent().hide();
            } );
            
            // add counting ids to elements
            $( '.comment-edit' ).each( function() {
                $( this ).attr( 'id', 'commentEditBtn-' + _defaults.editBtnCount );
                _defaults.editBtnCount++;
            } );
            
            $( '.comments-comment-edit' ).each( function() {
                $( this ).attr( 'id', 'commentEditComment-' + _defaults.commentCount );
                _defaults.commentCount++;
            } );
            
            $.each( $( '.comments-delete-btn' ), function() {
                $( this ).attr( 'data-toggle', 'modal' ).attr( 'data-target', '#deleteCommentModal-' + _defaults.deleteBtnCount );
                _defaults.deleteBtnCount++;
            } );
            
            $.each( $( '.deleteCommentModal' ), function() {
                $( this ).attr( 'id', 'deleteCommentModal-' + _defaults.deleteModalCount );
                _defaults.deleteModalCount++;
            } );
        }
    };
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
