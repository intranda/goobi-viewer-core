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
 * <Short Module Description>
 * 
 * @version 3.2.0
 * @module viewerJS.tinyMce
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false;
    var _defaults = {
        currLang: 'de',
        selector: 'textarea.tinyMCE',
        width: '100%',
        height: 400,
        theme: 'silver',
        plugins: 'print preview paste searchreplace autolink directionality code visualblocks visualchars fullscreen image link media template codesample table charmap hr pagebreak nonbreaking anchor insertdatetime advlist lists wordcount media textpattern help',
        toolbar: ['formatselect | undo redo | bold italic underline strikethrough superscript forecolor backcolor | link | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | fullscreen code | addFeedbackModalLink'],
        menubar: false,
        statusbar: false,
        pagebreak_separator: '<span class="pagebreak"></span>',
        relative_urls: false,
        language: 'de',
		valid_children: '+a[div]',
        setup : function (ed) {
            // listen to changes on tinymce input fields
            ed.on('init', function (e) {
                viewerJS.stickyElements.refresh.next();
            });
            
            ed.on('change input paste', function (e) {
		       console.log("trigger save")
		       ed.save();
               //tinymce.triggerSave();
               //trigger a change event on the underlying textArea
               console.log("target ", ed.targetElm, ed.getElement())
               $(ed.targetElm).change();
                if (currentPage === 'adminCmsNewPage') {
                    createPageConfig.prevBtn.attr('disabled', true);
                    createPageConfig.prevDescription.show();
                }
            });
            ed.on('blur', function(e) {
		        $(ed.targetElm).blur();
		    });
            
			ed.ui.registry.addButton('myCustomToolbarButton', {
				text: 'My Custom Button',
	              onAction: function () {
	                alert('Button clicked!');
	              }
    		});
        }
    };
    
    viewer.tinyMce = {
        getConfig: function(config) {
            let c = $.extend( true, {}, _defaults, config );
            return c;
        },
        init: function( config ) {
            this.config = $.extend( true, {}, _defaults, config);
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.tinyMce.init' );
                console.log( '##############################' );
                console.log( 'viewer.tinyMce.init: config - ', this.config );
            }
            
             
            // check current language
            switch ( this.config.currLang ) {
                case 'de':
                    this.config.language = 'de';
                    break;
                case 'es':
                    this.config.language = 'es';
                    break;
                case 'pt':
                    this.config.language = 'pt_PT';
                    break;
                case 'ru':
                    this.config.language = 'ru';
                    break;
            }
			// console.log("tinymce init ", this.config);
            tinymce.init( this.config );
        },
        close: function() {
            tinymce.remove();
        },
        overview: function() {
            // check if description or publication editing is enabled and
            // set fullscreen options
            if ( $( '.overview__description-editor' ).length > 0 ) {
                viewerJS.tinyConfig.setup = function( editor ) {
                    editor.on( 'init', function( e ) {
                        $( '.overview__publication-action .btn' ).hide();
                    } );
                    editor.on( 'FullscreenStateChanged', function( e ) {
                        if ( e.state ) {
                            $( '.overview__description-action-fullscreen' ).addClass( 'in' );
                        }
                        else {
                            $( '.overview__description-action-fullscreen' ).removeClass( 'in' );
                        }
                    } );
                };
            }
            else {
                viewerJS.tinyConfig.setup = function( editor ) {
                    editor.on( 'init', function( e ) {
                        $( '.overview__description-action .btn' ).hide();
                    } );
                    editor.on( 'FullscreenStateChanged', function( e ) {
                        if ( e.state ) {
                            $( '.overview__publication-action-fullscreen' ).addClass( 'in' );
                        }
                        else {
                            $( '.overview__publication-action-fullscreen' ).removeClass( 'in' );
                        }
                    } );
                };
            }
        },
    };
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
