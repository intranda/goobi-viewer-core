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
        theme: 'modern',
        plugins: 'print preview paste searchreplace autolink directionality code visualblocks visualchars fullscreen image link media template codesample table charmap hr pagebreak nonbreaking anchor toc insertdatetime advlist lists textcolor wordcount spellchecker imagetools media contextmenu colorpicker textpattern help',
        toolbar: 'formatselect | undo redo | bold italic underline strikethrough forecolor backcolor | link | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | fullscreen code',
        menubar: false,
        statusbar: false,
        pagebreak_separator: '<span class="pagebreak"></span>',
        relative_urls: false,
        force_br_newlines: false,
        force_p_newlines: false,
        forced_root_block: '',
        language: 'de'
    };
    
    viewer.tinyMce = {
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.tinyMce.init' );
                console.log( '##############################' );
                console.log( 'viewer.tinyMce.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );
            
            // check current language
            switch ( _defaults.currLang ) {
                case 'de':
                    _defaults.language = 'de';
                    break;
                case 'es':
                    _defaults.language = 'es';
                    break;
                case 'pt':
                    _defaults.language = 'pt_PT';
                    break;
                case 'ru':
                    _defaults.language = 'ru';
                    break;
            }
            
            // init editor
            tinymce.init( _defaults );
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
