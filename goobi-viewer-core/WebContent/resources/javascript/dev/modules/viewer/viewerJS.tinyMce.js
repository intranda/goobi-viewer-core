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
 * @module viewerJS.
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = true;
    var _defaults = {
        currLang: 'de',
        selector: 'textarea.tinyMCE',
        width: '100%',
        height: 400,
        theme: 'modern',
        plugins: [ "advlist autolink link image lists charmap print preview hr anchor pagebreak spellchecker",
                "searchreplace wordcount visualblocks visualchars code fullscreen insertdatetime media nonbreaking",
                "save table contextmenu directionality emoticons template paste textcolor"

        ],
        toolbar: "bold italic underline | forecolor backcolor | fontsizeselect | alignleft aligncenter alignright alignjustify | bullist numlist  | link | code preview",
        menubar: false,
        statusbar: false,
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
    };
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
