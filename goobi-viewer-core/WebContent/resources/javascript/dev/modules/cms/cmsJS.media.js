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
 * @version 3.4.0
 * @module cmsJS.media
 * @requires jQuery
 * @description Module which controls the media upload and editing for the cms.
 */
var cmsJS = ( function( cms ) {
    'use strict';
    
    // variables
    var _debug = true;
    var _defaults = {};
    
    cms.media = {
        /**
         * @method init
         * @description Method which initializes the medie module.
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'cmsJS.media.init' );
                console.log( '##############################' );
                console.log( 'cmsJS.media.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );
            
            // init file upload
            _initFileUpload();
            
            // show/hide edit actions for media file
            $( '.admin-cms-media__file' ).on( 'mouseover', function() {
				$( this ).find( '.admin-cms-media__file-actions' ).show();
			});
        	$( '.admin-cms-media__file') .on( 'mouseout', function() {
				$( this ).find( '.admin-cms-media__file-actions' ).hide();
			});
        	$( '[data-action="edit"]' ).on( 'click', function() {
				$( this ).parents( '.admin-cms-media__file' ).find( '.admin-cms-media__file-metadata-view' ).removeClass( 'in');
				$( this ).parents( '.admin-cms-media__file' ).find( '.admin-cms-media__file-metadata-edit' ).addClass( 'in');
				$( this ).parent().removeClass( 'in');
				$( this ).parent().next().addClass('in');
			});
			$( '[data-action="cancel"]' ).on( 'click', function() {
				$( this ).parents( '.admin-cms-media__file' ).find( '.admin-cms-media__file-metadata-view' ).addClass( 'in');
				$( this ).parents( '.admin-cms-media__file' ).find( '.admin-cms-media__file-metadata-edit' ).removeClass( 'in');
				$( this ).parent().removeClass( 'in' );
				$( this ).parent().prev().addClass( 'in' );
			});         
        }
    };
    
    /**
     * @description Method to activate drag & drop upload for media files.
     * @method _initFileUpload
     * */
    function _initFileUpload() {
    	$('.admin-cms-media__upload').each(function () {
    	    var $form = $(this),
    	        $input = $form.find('input[type="file"]'),
    	        $label = $form.find('label'),
    	        $errorMsg = $form.find('.box__error span'),
    	        $restart = $form.find('.box__restart'),
    	        droppedFiles = false,
    	        showFiles = function (files) {
    	            $label.text(files.length > 1 ? ($input.attr('data-multiple-caption') || '').replace('{count}', files.length) : files[0].name);
    	        };

    	    // letting the server side to know we are going to make an Ajax request
    	    $form.append('<input type="hidden" name="ajax" value="1" />');

    	    // automatically submit the form on file select
    	    $input.on('change', function (e) {
    	        showFiles(e.target.files);

    	        $form.trigger('submit');
    	    });

    	    // drag&drop files if the feature is available
    	    if (_isAdvancedUpload) {
    	        $form
    	            .addClass('has-advanced-upload') // letting the CSS part to know drag&drop is supported by the browser
    	            .on('drag dragstart dragend dragover dragenter dragleave drop', function (e) {
    	                // preventing the unwanted behaviours
    	                e.preventDefault();
    	                e.stopPropagation();
    	            })
    	            .on('dragover dragenter', function () //
    	            {
    	                $form.addClass('is-dragover');
    	            })
    	            .on('dragleave dragend drop', function () {
    	                $form.removeClass('is-dragover');
    	            })
    	            .on('drop', function (e) {
    	                droppedFiles = e.originalEvent.dataTransfer.files; // the files that were dropped
    	                showFiles(droppedFiles);

    	                $form.trigger('submit'); // automatically submit the form on file drop
    	            });
    	    }

    	    // if the form was submitted
    	    $form.on('submit', function (e) {
    	        // preventing the duplicate submissions if the current one is in progress
    	        if ($form.hasClass('is-uploading')) return false;

    	        $form.addClass('is-uploading').removeClass('is-error');

    	        if (_isAdvancedUpload) // ajax file upload for modern browsers
    	        {
    	            e.preventDefault();

    	            // gathering the form data
    	            var ajaxData = new FormData($form.get(0));
    	            
    	            if (droppedFiles) {
    	                $.each(droppedFiles, function (i, file) {
    	                    ajaxData.append($input.attr('name'), file);
    	                });
    	            }

    	            // ajax request
    	            $.ajax(
    	                {
    	                    url: $form.attr('action'),
    	                    type: $form.attr('method'),
    	                    data: ajaxData,
    	                    dataType: 'json',
    	                    cache: false,
    	                    contentType: false,
    	                    processData: false,
    	                    complete: function () {
    	                        $form.removeClass('is-uploading');
    	                    },
    	                    success: function (data) {
    	                        $form.addClass(data.success == true ? 'is-success' : 'is-error');
    	                        if (!data.success) $errorMsg.text(data.error);
    	                    },
    	                    error: function () {
    	                        alert('Error. Please, contact the webmaster!');
    	                    }
    	                });
    	        }
    	        else // fallback Ajax solution upload for older browsers
    	        {
    	            var iframeName = 'uploadiframe' + new Date().getTime(),
    	                $iframe = $('<iframe name="' + iframeName + '" style="display: none;"></iframe>');

    	            $('body').append($iframe);
    	            $form.attr('target', iframeName);

    	            $iframe.one('load', function () {
    	                var data = $.parseJSON($iframe.contents().find('body').text());
    	                $form.removeClass('is-uploading').addClass(data.success == true ? 'is-success' : 'is-error').removeAttr('target');
    	                if (!data.success) $errorMsg.text(data.error);
    	                $iframe.remove();
    	            });
    	        }
    	    });

    	    // restart the form if has a state of error/success
    	    $restart.on('click', function (e) {
    	        e.preventDefault();
    	        $form.removeClass('is-error is-success');
    	        $input.trigger('click');
    	    });

    	    // Firefox focus bug fix for file input
    	    $input
    	        .on('focus', function () { $input.addClass('has-focus'); })
    	        .on('blur', function () { $input.removeClass('has-focus'); });
    	});
    }

    /**
     * @description Feature detection for drag & drop upload.
     * @method _isAdvancedUpload
     * @returns {Boolean} Returns true if feature is available.
     * */
    function _isAdvancedUpload() {
    	var div = document.createElement( 'div' );
    	
        return ( ( 'draggable' in div ) || ( 'ondragstart' in div && 'ondrop' in div ) ) && 'FormData' in window && 'FileReader' in window;
    }
    
    return cms;
    
} )( cmsJS || {}, jQuery );
