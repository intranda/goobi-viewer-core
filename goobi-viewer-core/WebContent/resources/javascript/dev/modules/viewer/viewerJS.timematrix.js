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
        apiQuery: 'api?action=timeline',
        startDateQuery: '&startDate=',
        rangeInput1: null,
        startDate: null,
        endDateQuery: '&endDate=',
        rangeInput2: null,
        endDate: null,
        countQuery: '&count=',
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
         *         path: '#{request.contextPath}/',
         *         lang: {
         *             closeWindow: '#{msg.timematrixCloseWindow}',
         *             goToWork: '#{msg.timematrixGoToWork}'
         *         }
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
                    $( '.timematrix-slider-bubble-startDate' ).html( ui.values[ 0 ] );
                    _defaults.startDate = ui.values[ 0 ];
                    _defaults.rangeInput1.val( ui.values[ 1 ] );
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
                
                // check for popovers and remove them
                if ( $( '.timematrix-popover' ).length ) {
                    $( '.timematrix-popover' ).remove();
                }
                
                // build api target
                var apiTarget = _defaults.contextPath;
                apiTarget += _defaults.apiQuery;
                apiTarget += _defaults.startDateQuery;
                apiTarget += _defaults.startDate;
                apiTarget += _defaults.endDateQuery;
                apiTarget += _defaults.endDate;
                apiTarget += _defaults.countQuery;
                apiTarget += _defaults.count;
                
                // get data from api
                _promise = viewer.helper.getRemoteData( apiTarget );
                
                // render thumbnails
                _promise.then( function( data ) {
                    _apiData = data;
                    
                    _defaults.$tmCanvas.html( _renderThumbs( _apiData ) );
                    $( '.timematrix-thumb' ).css( {
                        height: $( '.timematrix-thumb' ).outerWidth()
                    } );
                    
                    // show thumbs after they´ve been loaded
                    $( '.timematrix-thumb img' ).load( function() {
                        $( this ).css( {
                            visibility: 'visible'
                        } );
                    } );
                    
                    // listen to click event on thumbnails
                    $( '.timematrix-thumb' ).on( 'click', function() {
                        if ( !$( '.timematrix-popover' ) ) {
                            $( '.timematrix-thumb' ).removeClass( 'marker' );
                            $( this ).addClass( 'marker' );
                            _renderPopover( $( this ), _defaults.lang );
                        }
                        else {
                            $( '.timematrix-popover' ).remove();
                            $( '.timematrix-thumb' ).removeClass( 'marker' );
                            $( this ).addClass( 'marker' );
                            _renderPopover( $( this ), _defaults.lang );
                        }
                        
                        // close popover
                        $( '.timematrix-popover-close' ).on( 'click', function() {
                            $( this ).parent().remove();
                            $( '.timematrix-thumb' ).removeClass( 'marker' );
                        } );
                        
                        // check if image is loaded and reset loader
                        $( '.timematrix-popover-body img' ).load( function() {
                            $( '.timematrix-popover-imageloader' ).hide();
                        } );
                    } );
                } ).then( null, function() {
                    _defaults.$tmCanvas.append( viewer.helper.renderAlert( 'alert-danger', '<strong>Status: </strong>' + error.status + ' ' + error.statusText, false ) );
                    console.error( 'ERROR: viewer.timematrix.init - ', error );
                } )
            } );
            
            // remove all popovers by clicking on body
            $( 'body' ).on( 'click', function( event ) {
                if ( $( event.target ).closest( '.timematrix-thumb' ).length ) {
                    return;
                }
                else {
                    _removePopovers();
                }
            } );
        }
    };
    
    /**
     * Method to render image thumbnails to the timematrix canvas.
     * 
     * @method _renderThumbs
     * @param {Object} json An JSON-Object which contains the image data.
     * @returns {String} HTML-String which displays the image thumbnails.
     */
    function _renderThumbs( json ) {
        if ( _debug ) {
            console.log( 'viewer.timematrix _renderThumbs: json - ' );
            console.log( json );
        }
        
        var tlbox = '';
        tlbox += '<div class="timematrix-box">';
        tlbox += '<header class="timematrix-header">';
        if ( _defaults.startDate !== '' && _defaults.endDate !== '' ) {
            tlbox += '<h3>' + _defaults.startDate + ' - ' + _defaults.endDate + '</h3>';
        }
        tlbox += '</header>';
        tlbox += '<section class="timematrix-body">';
        $.each( json, function( i, j ) {
            tlbox += '<div class="timematrix-thumb" data-title="' + j.title + '" data-mediumimage="' + j.mediumimage + '" data-url="' + j.url + '">';
            if ( j.thumbnailUrl ) {
                tlbox += '<img src="' + j.thumbnailUrl + '" style="visibility: hidden;" />';
            }
            else {
                tlbox += '';
            }
            tlbox += '</div>';
        } );
        tlbox += '</section>';
        tlbox += '<footer class="timematrix-footer"></footer>';
        tlbox += '</div>';
        
        return tlbox;
    }
    
    /**
     * Method to render a popover with a thumbnail image.
     * 
     * @method _renderPopover
     * @param {Object} $Obj Must be an jQuery-Object like $(this).
     * @param {Object} lang An Object with localized strings in the selected language.
     */
    function _renderPopover( $Obj, lang ) {
        if ( _debug ) {
            console.log( 'viewer.timematrix _renderPopover: obj - ' );
            console.log( $Obj );
            console.log( 'viewer.timematrix _renderPopover: lang - ' + lang );
        }
        
        var title = $Obj.attr( 'data-title' );
        var mediumimage = $Obj.attr( 'data-mediumimage' );
        var url = $Obj.attr( 'data-url' );
        var $objPos = $Obj.position();
        var $objCoords = {
            top: $objPos.top,
            left: $objPos.left,
            width: $Obj.outerWidth()
        };
        var popoverPos = _calculatePopoverPosition( $objCoords, _defaults.$tmCanvasCoords );
        var popover = '';
        popover += '<div class="timematrix-popover" style="top: ' + popoverPos.top + 'px; left: ' + popoverPos.left + 'px;">';
        popover += '<span class="timematrix-popover-close" title="' + lang.closeWindow + '">&times;</span>';
        popover += '<header class="timematrix-popover-header">';
        popover += '<h4 title="' + title + '">' + viewer.helper.truncateString( title, 75 ) + '</h4>';
        popover += '</header>';
        popover += '<section class="timematrix-popover-body">';
        popover += '<div class="timematrix-popover-imageloader"></div>';
        popover += '<a href="' + url + '"><img src="' + mediumimage + '" /></a>';
        popover += '</section>';
        popover += '<footer class="timematrix-popover-footer">';
        popover += '<a href="' + url + '" title="' + title + '">' + lang.goToWork + '</a>';
        popover += '</footer>';
        popover += '</div>';
        
        _defaults.$tmCanvas.append( popover );
    }
    
    /**
     * Method which calculates the position of the popover.
     * 
     * @method _calculatePopoverPosition
     * @param {Object} triggerCoords An object which contains the coordinates of the
     * element has been clicked.
     * @param {Object} tmCanvasCoords An object which contains the coordinates of the
     * wrapper element from the timematrix.
     * @returns {Object} An object which contains the top and the left position of the
     * popover.
     */
    function _calculatePopoverPosition( triggerCoords, tmCanvasCoords ) {
        if ( _debug ) {
            console.log( 'viewer.timematrix _calculatePopoverPosition: triggerCoords - ' );
            console.log( triggerCoords );
            console.log( 'viewer.timematrix _calculatePopoverPosition: tmCanvasCoords - ' );
            console.log( tmCanvasCoords );
        }
        
        var poLeftBorder = triggerCoords.left - ( 150 - ( triggerCoords.width / 2 ) );
        var poRightBorder = poLeftBorder + 300;
        var tbLeftBorder = tmCanvasCoords.left;
        var tbRightBorder = tmCanvasCoords.right;
        var poTop;
        var poLeft = poLeftBorder;
        
        poTop = ( triggerCoords.top + $( '.timematrix-thumb' ).outerHeight() ) - 1;
        
        if ( poLeftBorder <= tbLeftBorder ) {
            poLeft = tbLeftBorder;
        }
        
        if ( poRightBorder >= tbRightBorder ) {
            poLeft = tmCanvasCoords.right - 300;
        }
        
        return {
            top: poTop,
            left: poLeft
        };
    }
    
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
        
        return '<div class="timematrix-slider-bubble-' + time + '">' + val + '</div>';
    }
    
    /**
     * Method which removes all popovers.
     * 
     * @method _removePopovers
     */
    function _removePopovers() {
        if ( _debug ) {
            console.log( '---------- _removePopovers() ----------' );
        }
        
        $( '.timematrix-popover' ).remove();
        $( '.timematrix-thumb' ).removeClass( 'marker' );
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
