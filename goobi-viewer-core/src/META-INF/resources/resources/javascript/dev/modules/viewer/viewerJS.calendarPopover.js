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
 * @module viewerJS.calendarPopover
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false;
    var _this = null;
    var _currApiCall = '';
    var _json = {};
    var _popoverConfig = {};
    var _popoverContent = null;
    var _defaults = {
        appUrl: '',
        calendarWrapperSelector: '.search-calendar__months',
        popoverTriggerSelector: '[data-popover-trigger="calendar-po-trigger"]',
        popoverTitle: 'Bitte übergeben Sie den Titel des Werks',
    };
    
    viewer.calendarPopover = {
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.calendarPopover.init' );
                console.log( '##############################' );
                console.log( 'viewer.calendarPopover.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );
            
            // TODO: Fehlermeldung in der Konsole beseitigen, wenn man auf den Tag ein
            // zweites Mal klickt.
            
            // show popover for current day
            $( _defaults.popoverTriggerSelector ).on( 'click', function() {
                _this = $( this );
                _currApiCall = encodeURI( _this.attr( 'data-api' ) );
                
                viewerJS.helper.getRemoteData( _currApiCall ).done( function( _json ) {
                    _popoverContent = _getPopoverContent( _json, _defaults );
                    _popoverConfig = {
                        placement: 'auto bottom',
                        title: _defaults.popoverTitle,
                        content: _popoverContent,
                        viewport: {
                            selector: _defaults.calendarWrapperSelector
                        },
                        html: true
                    };
                    
                    $( _defaults.popoverTriggerSelector ).popover( 'destroy' );
                    _this.popover( _popoverConfig );
                    _this.popover( 'show' );
                } );
            } );
            
            // remove all popovers by clicking on body
            $( 'body' ).on( 'click', function( event ) {
                if ( $( event.target ).closest( _defaults.popoverTriggerSelector ).length ) {
                    return;
                }
                else {
                    $( _defaults.popoverTriggerSelector ).popover( 'destroy' );
                }
            } );
        }
    };
    
    /**
     * Method to render the popover content.
     * 
     * @method _getPopoverContent
     * @param {Object} data A JSON-Object which contains the necessary data.
     * @param {Object} config The config object of the module.
     * @returns {String} The HTML-String of the popover content.
     */
    function _getPopoverContent( data, config ) {
        if ( _debug ) {
            console.log( '---------- _getPopoverContent() ----------' );
            console.log( '_getPopoverContent: data = ', data );
            console.log( '_getPopoverContent: config = ', config );
        }
        
        var workList = '';
        var workListLink = '';
        
        workList += '<ul class="list">';
        
        $.each( data, function( works, values ) {
            workListLink = config.appUrl + 'image/' + values.PI_TOPSTRUCT + '/' + values.THUMBPAGENO + '/' + values.LOGID + '/';
            
            workList += '<li>';
            workList += '<a href="' + workListLink + '">';
            workList += values.LABEL;
            workList += '</a>';
            workList += '</li>';
        } );
        
        workList += '</ul>';
        
        return workList;
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
