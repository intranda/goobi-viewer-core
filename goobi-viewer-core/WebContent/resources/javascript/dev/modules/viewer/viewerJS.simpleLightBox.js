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
 * Module to render a simple lightbox for images.
 * 
 * @version 3.2.0
 * @module viewerJS.simpleLightbox
 * @requires jQuery
 * @example <img src="/your/path/to/the/image.jpg" class="lightbox-image"
 * data-imgpath="/your/path/to/the/" data-imgname="image.jpg" alt="" /> *
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _box = null;
    var _imgpath = null;
    var _imgname = null;
    
    viewer.simpleLightBox = {
        /**
         * Initializes an event (click) which renders a lightbox with an bigger image.
         * 
         * @method init
         * @example
         * 
         * <pre>
         * viewerJS.simpleLightBox.init();
         * </pre>
         * 
         */
        init: function() {
            // eventlisteners
            $( '.lightbox-image' ).on( 'click', function( event ) {
                event.preventDefault();
                
                var $this = $( this );
                
                _imgpath = _getImagePath( $this );
                _imgname = _getImageName( $this );
                _box = _setupLightBox( _imgpath, _imgname );
                
                $( 'body' ).append( _box );
                
                _centerModalBox( $( '.lightbox-modal-box' ) );
                
                $( '.lightbox-overlay' ).fadeIn();
                
                $( '.lightbox-close-btn' ).on( 'click', function() {
                    $( '.lightbox-overlay' ).remove();
                } );
            } );
        }
    };
    
    /**
     * Returns the image path from the 'data-imgpath' attribute.
     * 
     * @method _getImagePath
     * @param {Object} $Obj Must be a jQuery-Object like $('.something')
     * @returns {String} The image path from the 'data-imgpath' attribute.
     * 
     */
    function _getImagePath( $Obj ) {
        _imgpath = $Obj.attr( 'data-imgpath' );
        
        return _imgpath;
    }
    
    /**
     * Returns the image name from the 'data-imgname' attribute.
     * 
     * @method _getImageName
     * @param {Object} $Obj Must be a jQuery-Object like $('.something')
     * @returns {String} The image name from the 'data-imgname' attribute.
     * 
     */
    function _getImageName( $Obj ) {
        _imgname = $Obj.attr( 'data-imgname' );
        
        return _imgname;
    }
    
    /**
     * Returns a HTML-String which renders the lightbox.
     * 
     * @method _setupLightBox
     * @param {String} path The path to the big image.
     * @param {String} name The name of the big image.
     * @returns {String} The HTML-Code to render the lightbox.
     * 
     */
    function _setupLightBox( path, name ) {
        var lightbox = '';
        
        lightbox = '<div class="lightbox-overlay">';
        lightbox += '<div class="lightbox-modal-box">';
        lightbox += '<div class="lightbox-close">';
        lightbox += '<span class="lightbox-close-btn" title="Fenster schlie&szlig;en">&times;</span>';
        lightbox += '</div>';
        lightbox += '<img src="' + path + name + '" alt="' + name + '" /></div></div>';
        
        return lightbox;
    }
    
    /**
     * Puts the lightbox to the center of the screen.
     * 
     * @method _centerModalBox
     * @param {Object} $Obj Must be a jQuery-Object like $('.something')
     */
    function _centerModalBox( $Obj ) {
        var boxWidth = $Obj.outerWidth();
        var boxHeight = $Obj.outerHeight();
        
        $Obj.css( {
            'margin-top': '-' + boxHeight / 2 + 'px',
            'margin-left': '-' + boxWidth / 2 + 'px'
        } );
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
