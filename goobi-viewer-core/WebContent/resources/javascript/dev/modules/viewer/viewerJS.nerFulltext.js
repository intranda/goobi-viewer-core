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
 * Module to render NER-Popovers in fulltext pages.
 * 
 * @version 3.2.0
 * @module viewerJS.nerFulltext
 * @requires jQuery
 * 
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    // define variables
    var _debug = false;
    var _defaults = {
        path: null,
        lang: {}
    };
    var _contextPath = null;
    var _lang = null;
    
    viewer.nerFulltext = {
        /**
         * Method which initializes the NER Popover methods.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.path The rootpath of the application.
         * @example
         * 
         * <pre>
         * var nerConfig = {
         *     path: '#{request.contextPath}'
         * };
         * 
         * viewerJS.nerFulltext.init( nerConfig );
         * </pre>
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.nerFulltext.init' );
                console.log( '##############################' );
                console.log( 'viewer.nerFulltext.init: config - ' );
                console.log( config );
            }
            
            $.extend( true, _defaults, config );
            
            _initNerPopover( _defaults.path );
        }
    };
    
    /**
     * Method which fetches data from the API and returns an JSON object.
     * 
     * @method _getRemoteData
     * @param {String} target The API call URL.
     * @returns {Object} The JSON object with the API data.
     * 
     */
    function _getRemoteData( target ) {
        if ( _debug ) {
            console.log( 'viewer.nerFulltext _getRemoteData: target - ' );
            console.log( target );
        }
        
        // show preloader for current element
        $( '.ner-detail-loader', target ).css( {
            display: 'inline-block'
        } );
        
        // AJAX call
        var data = $.ajax( {
            url: decodeURI( $( target ).attr( 'data-remotecontent' ) ),
            type: 'POST',
            dataType: 'JSON',
            async: false,
            complete: function() {
                $( '.ner-detail-loader' ).hide();
            }
        } ).responseText;
        
        return data;
    }
    
    /**
     * Method which initializes the events for the NER-Popovers.
     * 
     * @method _initNerPopover
     * @param {String} path The root path of the application.
     * 
     */
    function _initNerPopover( path ) {
        if ( _debug ) {
            console.log( 'viewer.nerFulltext _initNerPopover: path - ' + path );
        }
        
        var data, position, title, triggerCoords, textBox, textBoxPosition, textBoxCoords;
        
        $( '.ner-trigger' ).on( 'click', function() {
            $( 'body' ).find( '.ner-popover-pointer' ).hide();
            $( 'body' ).find( '.ner-popover' ).remove();
            data = _getRemoteData( $( this ) );
            position = $( this ).position();
            triggerCoords = {
                top: position.top,
                left: position.left,
                width: $( this ).outerWidth()
            };
            textBox = $( '#view_fulltext_wrapp' );
            textBoxPosition = textBox.position();
            textBoxCoords = {
                top: textBoxPosition.top,
                left: 0,
                right: textBoxPosition.left + textBox.outerWidth()
            };
            title = $( this ).attr( 'title' );
            
            textBox.append( _renderNerPopover( data, _calculateNerPopoverPosition( triggerCoords, textBoxCoords ), title, path ) );
            
            if ( $( '.ner-popover' ) ) {
                $( this ).find( '.ner-popover-pointer' ).show();
                _removeNerPopover();
                
                $( '.ner-detail-trigger' ).on( 'click', function() {
                    data = _getRemoteData( $( this ) );
                    title = $( this ).attr( 'title' );
                    
                    $( this ).parent().next( '.ner-popover-detail' ).html( _renderNerPopoverDetail( data, title ) );
                } );
            }
        } );
    }
    
    /**
     * Method which renders a popover to the DOM.
     * 
     * @method _renderNerPopover
     * @param {Object} data The JSON object from the API.
     * @param {Object} position A jQuery object including the position of the clicked
     * trigger.
     * @param {String} title The value of the title attribute from the clicked trigger.
     * @param {String} path The root path of the application.
     * @returns {String} The HTML string which renders the popover.
     * 
     */
    function _renderNerPopover( data, position, title, path ) {
        if ( _debug ) {
            console.log( 'viewer.nerFulltext _renderNerPopover: data - ' );
            console.log( data );
            console.log( 'viewer.nerFulltext _renderNerPopover: position - ' );
            console.log( position );
            console.log( 'viewer.nerFulltext _renderNerPopover: title - ' + title );
            console.log( 'viewer.nerFulltext _renderNerPopover: path - ' + path );
        }
        
        var positionTop = position.top;
        var positionLeft = position.left;
        var popover = '';
        
        popover += '<div class="ner-popover" style="top:' + positionTop + 'px; left:' + positionLeft + 'px">';
        popover += '<div class="ner-popover-close" title="Fenster schlie&szlig;en">&times;</div>';
        popover += '<div class="ner-popover-header"><h4>' + title + '</h4></div>';
        popover += '<div class="ner-popover-body">';
        popover += '<dl class="dl-horizontal">';
        $.each( $.parseJSON( data ), function( i, object ) {
            $.each( object, function( property, value ) {
                popover += '<dt title="' + property + '">' + property + ':</dt>';
                var objValue = '';
                $.each( value, function( p, v ) {
                    var icon = '';
                    
                    switch ( property ) {
                        case 'Beruf':
                            icon = 'glyphicon-briefcase';
                            break;
                        case 'Verwandte Begriffe':
                            icon = 'glyphicon-briefcase';
                            break;
                        case 'Sohn':
                            icon = 'glyphicon-user';
                            break;
                        case 'Vater':
                            icon = 'glyphicon-user';
                            break;
                        case 'Geburtsort':
                            icon = 'glyphicon-map-marker';
                            break;
                        case 'Sterbeort':
                            icon = 'glyphicon-map-marker';
                            break;
                    }
                    
                    if ( v.url ) {
                        objValue += '<span ';
                        objValue += 'class="ner-detail-trigger" ';
                        objValue += 'title="' + v.text + '" ';
                        objValue += 'tabindex="-1"';
                        objValue += 'data-remotecontent="' + path + '/api?action=normdata&url=' + v.url + '">';
                        objValue += '<span class="glyphicon ' + icon + '"></span>&nbsp;';
                        objValue += v.text;
                        objValue += '<span class="ner-detail-loader"></span>';
                        objValue += '</span>';
                    }
                    else {
                        if ( property === 'URI' ) {
                            objValue += '<a href="' + v.text + '" target="_blank">' + v.text + '</a>';
                        }
                        else {
                            objValue += v.text;
                        }
                    }
                    
                    objValue += '<br />';
                } );
                popover += '<dd>' + objValue + '</dd>';
                popover += '<div class="ner-popover-detail"></div>';
            } );
        } );
        popover += '</dl>';
        popover += '</div>';
        popover += '</div>';
        
        return popover;
    }
    
    /**
     * Method which renders detail information into the popover.
     * 
     * @method _renderNerPopoverDetail
     * @param {Object} data The JSON object from the API.
     * @param {String} title The value of the title attribute from the clicked trigger.
     * @returns {String} The HTML string which renders the details.
     * 
     */
    function _renderNerPopoverDetail( data, title ) {
        if ( _debug ) {
            console.log( 'viewer.nerFulltext _renderNerPopoverDetail: data - ' );
            console.log( data );
            console.log( 'viewer.nerFulltext _renderNerPopoverDetail: title - ' + title );
        }
        
        var popoverDetail = '';
        
        popoverDetail += '<div class="ner-popover-detail">';
        popoverDetail += '<div class="ner-popover-detail-header"><h4>' + title + '</h4></div>';
        popoverDetail += '<div class="ner-popover-detail-body">';
        popoverDetail += '<dl class="dl-horizontal">';
        $.each( $.parseJSON( data ), function( i, object ) {
            $.each( object, function( property, value ) {
                popoverDetail += '<dt title="' + property + '">' + property + ':</dt>';
                var objValue = '';
                $.each( value, function( p, v ) {
                    if ( property === 'URI' ) {
                        objValue += '<a href="' + v.text + '" target="_blank">' + v.text + '</a>';
                    }
                    else {
                        objValue += v.text;
                    }
                    
                    objValue += '<br />';
                } );
                popoverDetail += '<dd>' + objValue + '</dd>';
                popoverDetail += '<div class="ner-popover-detail"></div>';
            } );
        } );
        popoverDetail += '</dl>';
        popoverDetail += '</div>';
        popoverDetail += '</div>';
        
        return popoverDetail;
    }
    
    /**
     * Method which calculates the position of the popover in the DOM.
     * 
     * @method _calculateNerPopoverPosition
     * @param {Object} triggerCoords A jQuery object including the position of the clicked
     * trigger.
     * @param {Object} textBoxCoords A jQuery object including the position of the parent
     * DIV.
     * @returns {Object} An object which includes the position of the popover.
     * 
     */
    function _calculateNerPopoverPosition( triggerCoords, textBoxCoords ) {
        if ( _debug ) {
            console.log( 'viewer.nerFulltext _calculateNerPopoverPosition: triggerCoords - ' );
            console.log( triggerCoords );
            console.log( 'viewer.nerFulltext _calculateNerPopoverPosition: textBoxCoords - ' );
            console.log( textBoxCoords );
        }
        
        var poLeftBorder = triggerCoords.left - ( 150 - ( triggerCoords.width / 2 ) ), poRightBorder = poLeftBorder + 300, tbLeftBorder = textBoxCoords.left, tbRightBorder = textBoxCoords.right, poTop, poLeft = poLeftBorder;
        
        poTop = triggerCoords.top + 27;
        
        if ( poLeftBorder <= tbLeftBorder ) {
            poLeft = tbLeftBorder;
        }
        
        if ( poRightBorder >= tbRightBorder ) {
            poLeft = textBoxCoords.right - 300;
        }
        
        return {
            top: poTop,
            left: poLeft
        };
    }
    
    /**
     * Method to remove a popover from the DOM.
     * 
     * @method _removeNerPopover
     * 
     */
    function _removeNerPopover() {
        if ( _debug ) {
            console.log( 'viewer.nerFulltext _removeNerPopover' );
        }
        
        $( '.ner-popover-close' ).on( 'click', function() {
            $( 'body' ).find( '.ner-popover-pointer' ).hide();
            $( this ).parent().remove();
        } );
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
