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
 * Module to scroll a page back to top animated.
 * 
 * @version 3.2.0
 * @module viewerJS.pageScroll
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _elem = null;
    var _text = null;
    
    viewer.pageScroll = {
        /**
         * Initializes the animated pagescroll.
         * 
         * @method init
         * @param {String} obj The selector of the jQuery object.
         * @param {String} anchor The name of the anchor to scroll to.
         */
        init: function( obj, anchor ) {
            _elem = $( obj );
            _text = anchor;
            
            // eventlistener
            $( window ).on( 'scroll', function() {
                if ( window.pageYOffset > 200 ) {
                    _elem.fadeIn();
                }
                else {
                    _elem.hide();
                }
            } );
            
            _elem.on( 'click', function() {
                _scrollPage( _text );
            } );
        }
    };
    
    /**
     * Method which scrolls the page animated.
     * 
     * @method _scrollPage
     * @param {String} anchor The name of the anchor to scroll to.
     */
    function _scrollPage( anchor ) {
        $( 'html,body' ).animate( {
            scrollTop: $( anchor ).offset().top
        }, 1000 );
        
        return false;
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
