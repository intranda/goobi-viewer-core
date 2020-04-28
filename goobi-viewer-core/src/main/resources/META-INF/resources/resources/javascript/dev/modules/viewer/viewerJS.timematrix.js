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
 * Module to create an imagemap sorted by the year of creation.
 * 
 * @version 3.2.0
 * @module viewerJS.timematrix
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    // default variables
    var _debug = false;
    var _promise = null;
    var _apiData = null;
    var _defaults = {
        contextPath: null,
        apiQuery: 'rest/records/timematrix/range',
        startDateQuery: '/',
        rangeInput1: null,
        startDate: null,
        endDateQuery: '/',
        rangeInput2: null,
        endDate: null,
        countQuery: '/',
        count: null,
        $tmCanvas: null,
        $tmCanvasPos: null,
        $tmCanvasCoords: {},
        lang: {}
    };
    
    viewer.timematrix = {
        /**
         * Method to initialize the timematrix slider and the events which builds the
         * matrix and popovers.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.contextPath The rootpath of the application.
         * @param {String} config.apiQuery The API action to call.
         * @param {String} config.startDateQuery The GET-Parameter for the start date.
         * @param {Object} config.rangeInput1 An jQuery object of the first range input.
         * @param {String} config.startDate The value of the first range input.
         * @param {String} config.endDateQuery The GET-Parameter for the end date.
         * @param {Object} config.rangeInput2 An jQuery object of the second range input.
         * @param {String} config.endDate The value of the second range input.
         * @param {String} config.countQuery The GET-Parameter for the count query.
         * @param {String} config.count The number of results from the query.
         * @param {Object} config.$tmCanvas An jQuery object of the timematrix canvas.
         * @param {Object} config.$tmCanvasPos An jQuery object of the timematrix canvas
         * position.
         * @param {Object} config.lang An object of localized strings.
         * @example
         * 
         * <pre>
         * $( document ).ready( function() {
         *     var timematrixConfig = {
         *         path: '#{request.contextPath}/'
         *     };
         *     viewerJS.timematrix.init( timematrixConfig );
         * } );
         * </pre>
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.timematrix.init' );
                console.log( '##############################' );
                console.log( 'viewer.timematrix.init: config - ' );
                console.log( config );
            }
            
            $.extend( true, _defaults, config );
            
            _defaults.$tmCanvasCoords = {
                top: _defaults.$tmCanvasPos.top,
                left: 0,
                right: _defaults.$tmCanvasPos.left + _defaults.$tmCanvas.outerWidth()
            };
            
            // range slider settings
            $( '#slider-range' ).slider( {
                range: true,
                min: parseInt( _defaults.startDate ),
                max: parseInt( _defaults.endDate ),
                values: [ parseInt( _defaults.startDate ), parseInt( _defaults.endDate ) ],
                slide: function( event, ui ) {
                    _defaults.rangeInput1.val( ui.values[ 0 ] );
                    _defaults.rangeInput1.change();
                    $( '.timematrix-slider-bubble-startDate' ).html( ui.values[ 0 ] );
                    _defaults.startDate = ui.values[ 0 ];
                    _defaults.rangeInput2.val( ui.values[ 1 ] );
                    _defaults.rangeInput2.change();
                    $( '.timematrix-slider-bubble-endDate' ).html( ui.values[ 1 ] );
                    _defaults.endDate = ui.values[ 1 ];
                }
            } );
            
            // append slider bubble to slider handle
            $( '#slider-range .ui-slider-range' ).next().append( _renderSliderBubble( 'startDate', _defaults.startDate ) );
            $( '#slider-range .ui-slider-range' ).next().next().append( _renderSliderBubble( 'endDate', _defaults.endDate ) );
            
            // set active slider handle to top
            $( '.ui-slider-handle' ).on( 'mousedown', function() {
                $( '.ui-slider-handle' ).removeClass( 'top' );
                $( this ).addClass( 'top' );
            } );
            
            // listen to submit event of locate timematrix form
            $( '#locateTimematrix' ).on( 'submit', function( e ) {
                e.preventDefault();

                // build api target
                var apiTarget = _defaults.contextPath;
                apiTarget += _defaults.apiQuery;
                apiTarget += _defaults.startDateQuery;
                apiTarget += _defaults.startDate;
                apiTarget += _defaults.endDateQuery;
                apiTarget += _defaults.endDate;
                apiTarget += _defaults.countQuery;
                apiTarget += _defaults.count;
                apiTarget += '/';
                
            } );
        }
    };
    
    /**
     * Method which renders the bubbles for the slider.
     * 
     * @method _renderSliderBubble
     * @param {String} time The string for the time value.
     * @param {String} val The string for the value.
     * @returns {String} HTML-String which renders the slider-bubble.
     */
    function _renderSliderBubble( time, val ) {
        if ( _debug ) {
            console.log( 'viewer.timematrix _renderSliderBubble: time - ' + time );
            console.log( 'viewer.timematrix _renderSliderBubble: val - ' + val );
        }
        
        return '<div class="timematrix-slider-bubble</div>';
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
