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
 * Module to raise and degrade the page font size.
 * 
 * @version 3.2.0
 * @module viewerJS.changeFontSize
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false;
    var _parsedFontSize = 0;
    var _currFontSize = '';
    var _defaults = {
        fontDownBtn: '',
        fontUpBtn: '',
        maxFontSize: 18,
        minFontSize: 12,
        baseFontSize: '14px'
    };
    
    viewer.changeFontSize = {
        /**
         * Method which initializes the font size switcher.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.fontDownBtn The ID/Class of the font degrade button.
         * @param {String} config.fontUpBtn The ID/Class of the font upgrade button.
         * @param {String} config.maxFontSize The maximum font size the document should
         * scale up.
         * @param {String} config.minFontSize The minimum font size the document should
         * scale down.
         * @param {String} config.baseFontSize The base font size of the HTML-Element.
         * @example
         * 
         * <pre>
         * var changeFontSizeConfig = {
         *     fontDownBtn: '#fontSizeDown',
         *     fontUpBtn: '#fontSizeUp',
         *     maxFontSize: 18,
         *     minFontSize: 14
         * };
         * 
         * viewerJS.changeFontSize.init( changeFontSizeConfig );
         * </pre>
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.changeFontSize.init' );
                console.log( '##############################' );
                console.log( 'viewer.changeFontSize.init: config - ' );
                console.log( config );
            }
            
            $.extend( true, _defaults, config );
            
            if ( viewer.localStoragePossible ) {
                // set current font-size
                _setFontSize();
                
                // set button state
                _setButtonState();
                
                $( _defaults.fontDownBtn ).on( 'click', function() {
                    // set current font-size
                    _currFontSize = $( 'html' ).css( 'font-size' );
                    
                    // save font-size
                    _saveFontSize( _currFontSize );
                    
                    // parse number of font-size
                    _parsedFontSize = _parseFontSize( _currFontSize );
                    
                    // degrade font-size
                    _degradeFontSize( _parsedFontSize );
                } );
                
                $( _defaults.fontUpBtn ).on( 'click', function() {
                    // set current font-size
                    _currFontSize = $( 'html' ).css( 'font-size' );
                    
                    // save font-size
                    _saveFontSize( _currFontSize );
                    
                    // parse number of font-size
                    _parsedFontSize = _parseFontSize( _currFontSize );
                    
                    // raise font-size
                    _raiseFontSize( _parsedFontSize );
                } );
            }
        }
    };
    
    /**
     * Method to degrade the page font-size.
     * 
     * @method _degradeFontSize
     * @param {Number} current The current font-size of the HTML-Element.
     */
    function _degradeFontSize( current ) {
        if ( _debug ) {
            console.log( '---------- _degradeFontSize() ----------' );
            console.log( '_degradeFontSize: current = ', current );
        }
        
        var size = current;
        size--;
        
        if ( size >= _defaults.minFontSize ) {
            $( _defaults.fontDownBtn ).prop( 'disabled', false );
            $( _defaults.fontUpBtn ).prop( 'disabled', false );
            $( 'html' ).css( 'font-size', size + 'px' );
            
            // save font-size
            _saveFontSize( size + 'px' );
        }
        else {
            $( _defaults.fontDownBtn ).prop( 'disabled', true );
            $( _defaults.fontUpBtn ).prop( 'disabled', false );
        }
    }
    
    /**
     * Method to raise the page font-size.
     * 
     * @method _raiseFontSize
     * @param {Number} current The current font-size of the HTML-Element.
     */
    function _raiseFontSize( current ) {
        if ( _debug ) {
            console.log( '---------- _raiseFontSize() ----------' );
            console.log( '_raiseFontSize: current = ', current );
        }
        
        var size = current;
        size++;
        
        if ( size <= _defaults.maxFontSize ) {
            $( _defaults.fontDownBtn ).prop( 'disabled', false );
            $( _defaults.fontUpBtn ).prop( 'disabled', false );
            $( 'html' ).css( 'font-size', size + 'px' );
            
            // save font-size
            _saveFontSize( size + 'px' );
        }
        else {
            $( _defaults.fontDownBtn ).prop( 'disabled', false );
            $( _defaults.fontUpBtn ).prop( 'disabled', true );
        }
    }
    
    /**
     * Method which parses a given pixel value to a number and returns it.
     * 
     * @method _parseFontSize
     * @param {String} string The string to parse.
     */
    function _parseFontSize( string ) {
        if ( _debug ) {
            console.log( '---------- _parseFontSize() ----------' );
            console.log( '_parseFontSize: string = ', string );
        }
        
        return parseInt( string.replace( 'px' ) );
    }
    
    /**
     * Method to save the current font-size to local storage as a string.
     * 
     * @method _saveFontSize
     * @param {String} size The String to save in local storage.
     */
    function _saveFontSize( size ) {
        if ( _debug ) {
            console.log( '---------- _saveFontSize() ----------' );
            console.log( '_parseFontSize: size = ', size );
        }
        
        localStorage.setItem( 'currentFontSize', size );
    }
    
    /**
     * Method to set the current font-size from local storage to the HTML-Element.
     * 
     * @method _setFontSize
     */
    function _setFontSize() {
        if ( _debug ) {
            console.log( '---------- _setFontSize() ----------' );
        }
        var fontSize = localStorage.getItem( 'currentFontSize' );
        
        if ( fontSize === null || fontSize === '' ) {
            localStorage.setItem( 'currentFontSize', _defaults.baseFontSize );
        }
        else {
            $( 'html' ).css( 'font-size', fontSize );
        }
        
    }
    
    /**
     * Method to set the state of the font-size change buttons.
     * 
     * @method _setButtonState
     */
    function _setButtonState() {
        if ( _debug ) {
            console.log( '---------- _setButtonState() ----------' );
        }
        var fontSize = localStorage.getItem( 'currentFontSize' );
        var newFontSize = _parseFontSize( fontSize );
        
        if ( newFontSize === _defaults.minFontSize ) {
            $( _defaults.fontDownBtn ).prop( 'disabled', true );
        }
        
        if ( newFontSize === _defaults.maxFontSize ) {
            $( _defaults.fontUpBtn ).prop( 'disabled', true );
        }
        
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
