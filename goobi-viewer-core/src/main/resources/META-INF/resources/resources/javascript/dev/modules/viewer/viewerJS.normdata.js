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
 * Module to render popovers including normdata.
 * 
 * @version 3.2.0
 * @module viewerJS.normdata
 * @requires jQuery
 * @requires Bootstrap
 * 
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false;
    var _data = null;
    var _dataURL = '';
    var _data = '';
    var _linkPos = null;
    var _popover = '';
    var _id = '';
    var _$this = null;
    var _normdataIcon = null;
    var _preloader = null;
    var _defaults = {
        id: 0,
        path: null,
        lang: {},
        elemWrapper: null,
        linkSelector: ".normdataLink"
    };
    
    viewer.normdata = {
        /**
         * Method to initialize the timematrix slider and the events which builds the
         * matrix and popovers.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.id The starting ID of the popover.
         * @param {String} config.path The rootpath of the application.
         * @param {Object} config.lang An object of localized strings.
         * @param {Object} config.elemWrapper An jQuery object of the wrapper DIV.
         * @example
         * 
         * <pre>
         * var normdataConfig = {
         *     path: '#{request.contextPath}',
         *     lang: {
         *         popoverTitle: '#{msg.normdataPopoverTitle}',
         *         popoverClose: '#{msg.normdataPopoverClose}'
         *     },
         *     elemWrapper: $( '#metadataElementWrapper' )
         * };
         * 
         * viewerJS.normdata.init( normdataConfig );
         * </pre>
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.normdata.init' );
                console.log( '##############################' );
                console.log( 'viewer.normdata.init: config = ', config );
            }
            
            $.extend( true, _defaults, config );
            
            // hide close icons
            $( '.closeAllPopovers' ).hide();
            
            if ( _debug ) {
            console.log('(default) selector:' + _defaults.linkSelector )
            }
            
            
            // first level click
            $("body").on( 'click',  _defaults.linkSelector, function(event) {
            	_$this = $( this );
                
                _$this.off( 'focus' );
                
                _renderPopoverAction( _$this, _defaults.id );
                event.preventDefault();
                
                        $('.normdata-popover-close').focus();
                
            } );
        },
    };
    
    /**
     * Method which executes the click event action of the popover.
     * 
     * @method _renderPopoverAction
     * @param {Object} $Obj The jQuery object of the current clicked link.
     * @param {String} id The current id of the popover.
     */
    function _renderPopoverAction( $Obj, id ) {
        if ( _debug ) {
            console.log( '---------- _renderPopoverAction() ----------' );
            console.log( '_renderPopoverAction: $Obj = ', $Obj );
            console.log( '_renderPopoverAction: id = ', id );
        }

        
        _normdataIcon = $Obj.find( '.fa-list-ul' );
        _preloader = $Obj.find( '.normdata-preloader' );
        
        // set variables
        _dataURL = $Obj.attr( 'data-remotecontent' );
        _data = _getRemoteData( _dataURL, _preloader, _normdataIcon );
        _linkPos = $Obj.offset();
        _popover = _buildPopover( _data, id );
                
        if ( _debug ) {
            console.log( '_renderPopoverAction: _dataURL = ', _dataURL );
            console.log( '_renderPopoverAction: _data = ', _data );
            console.log( '_renderPopoverAction: _linkPos = ', _linkPos );
        }
        
        // append popover to body
        $( 'body' ).append( _popover );

        // add class to content for sytling reasons
        if ( $( '.normdata-popover-content img' ).hasClass( 'normdata-popover-content__icon' ) ) {
        	$( '.normdata-popover-content__icon' ).each( function() {
        		$(this).parent().addClass( 'img' );
        	} );        	
        }        
        
        // set popover position
        _calculatePopoverPosition( id, _linkPos, $Obj );
        
        // show popover
        $( document ).find( '#normdataPopover-' + id ).hide().fadeIn( 'fast', function() {
            // disable source button
            $Obj.attr( 'disabled', 'disabled' ).addClass( 'disabled' );
            
            // hide tooltip
            $Obj.tooltip( 'hide' );
            
            // set event for nth level popovers
            $( '.normdataDetailLink' ).off( 'click' ).on( 'click', function() {
                _$this = $( this );
                _renderPopoverAction( _$this, _defaults.id );
            } );
        } ).draggable({
            handle: ".normdata-popover-title",
        });
        
        // init close method
        _closeNormdataPopover( $Obj );
        
        // increment id
        _defaults.id++;
        
        // init close all method
        _closeAllNormdataPopovers( $Obj );
    }
    
    /**
     * Returns an HTML-String which renders the fetched data into a popover.
     * 
     * @method _buildPopover
     * @param {Object} data The JSON-Object which includes the data.
     * @param {String} id The incremented id of the popover.
     * @returns {String} The HTML-String with the fetched data.
     */
    function _buildPopover( data, id ) {
        if ( _debug ) {
            console.log( '---------- _buildPopover() ----------' );
            console.log( '_buildPopover: data = ', data );
            console.log( '_buildPopover: id = ', id );
        }
        
        var html = '';
        
        html += '<div id="normdataPopover-' + id + '" class="normdata-popover">';
        html += '<div class="normdata-popover-title">';
        html += '<h3>' + _defaults.lang.popoverTitle + '</h3>';
        html += '<button type="button" class="normdata-popover-close" aria-label="' + _defaults.lang.popoverClose + '"><span class="icon-wrapper normdata-popover-close__icon" aria-hidden="true"><svg class="icon" focusable="false"><use href="' + _defaults.path + '/resources/icons/outline/x.svg#icon"></use></svg></span></button>';
        html += '</div>';
        html += '<div class="normdata-popover-content">';
        html += '<dl class="dl-horizontal">';
        $.each( data, function( i, object ) {
            $.each( object, function( property, value ) {
                html += '<dt title="' + property + '">' + property + '</dt>';
                html += '<dd>';
                $.each( value, function( p, v ) {
                    if ( v.image ) {
                    	html += '<img class="normdata-popover-content__icon" src="' + _defaults.path + '/' + v.image + '" /> ';
                    }
                    if ( v.text ) {
                        if ( v.text.startsWith('http://') || v.text.startsWith('https://') ) {
                            html += '<a href="' + v.text + '" target="_blank">';
                            html += v.text;
                            html += '</a>';
                        }
                        else {
                            html += v.text;
                        }
                    }
                    if ( v.url ) {
                        html += '<button type="button" class="normdataDetailLink" data-remotecontent="';
                        html += _defaults.path;
                        html += '/api/v1/authority/resolver?id=';
                        html += _unicodeEscapeUri(v.url);
                        html += '?template=_DEFAULT&lang='+currentLang+'"'; // TODO use navigationHelper.localeString
                        html += '" title="' + _defaults.lang.showNormdata + '">';
                        html += '<span class="icon-wrapper normdata-popover-button__icon" aria-hidden="true"><svg class="icon" focusable="false"><use href="' + _defaults.path + '/resources/icons/outline/list-details.svg#icon"></use></svg></span>';
                        html += '<div class="normdata-preloader"></div>';
                        html += '</button>';
                    }
                    html += '<br />';
                } );
                html += '</dd>'
            } );
        } );
        html += "</dl>";
        html += "</div>";
        html += "</div>";
        
        return html;
    }
    
    /**
     * Replaces /\?% with corresponding Unicode sequences.
     * 
     * @param uri URI to escape
     */
    function _unicodeEscapeUri(uri) {
    	return uri.replace(/\//g, 'U002F').replace('/\\/g','U005C').replace('/?/g','U003F').replace('/%/g','U0025');
    }
    
    /**
     * Sets the position to the first level popovers.
     * 
     * @method _calculateFirstLevelPopoverPosition
     * @param {String} id The incremented id of the popover.
     * @param {Object} pos An Object with the current position oft the clicked link.
     * @param {Object} $Obj An jQuery-Object of the clicked link.
     * 
     */
    function _calculatePopoverPosition( id, pos, $Obj ) {
        if ( _debug ) {
            console.log( '---------- _calculatePopoverPosition() ----------' );
            console.log( '_calculatePopoverPosition: id = ', id );
            console.log( '_calculatePopoverPosition: pos = ', pos );
            console.log( '_calculatePopoverPosition: $Obj = ', $Obj );
        }
        
        var _bodyWidth = $( 'body' ).outerWidth();
        var _popoverWidth = $( '#normdataPopover-' + id ).outerWidth();
        var _popoverRight = pos.left + _popoverWidth;
        
        if ( _debug ) {
            console.log( '_calculatePopoverPosition: _bodyWidth = ', _bodyWidth );
            console.log( '_calculatePopoverPosition: _popoverWidth = ', _popoverWidth );
            console.log( '_calculatePopoverPosition: _popoverLeft = ', pos.left );
            console.log( '_calculatePopoverPosition: _popoverRight = ', _popoverRight );
        }
        
        if ( _popoverRight > _bodyWidth ) {
            var _diff = _popoverRight - _bodyWidth;
            
            if ( _debug ) {
                console.log( '_calculatePopoverPosition: _diff = ', _diff );
            }
            
            $( document ).find( '#normdataPopover-' + id ).css( {
                top: pos.top + $Obj.outerHeight() + 5,
                left: pos.left - _diff
            } );
        }
        else {
            $( document ).find( '#normdataPopover-' + id ).css( {
                top: pos.top + $Obj.outerHeight() + 5,
                left: pos.left
            } );
        }
    }
    
    /**
     * Removes current popover from the DOM on click.
     * 
     * @method _closeNormdataPopover
     * 
     */
    function _closeNormdataPopover( $Obj ) {
        if ( _debug ) {
            console.log( '---------- _closeNormdataPopover() ----------' );
            console.log( '_closeNormdataPopover: $Obj = ', $Obj );
        }
        
        $( document ).find( '.normdata-popover-close' ).on( 'click', function() {
            $( this ).parent().parent().remove();
            $Obj.removeAttr( 'disabled' ).removeClass( 'disabled' );
            
            if ( $( '.normdata-popover' ).length < 1 ) {
                $( '.closeAllPopovers' ).hide();
            }
        } );
    }
    
    /**
     * Removes all popovers from the DOM on click.
     * 
     * @method _closeAllNormdataPopovers
     * 
     */
    function _closeAllNormdataPopovers( $Obj ) {
        if ( _debug ) {
            console.log( '---------- _closeAllNormdataPopovers() ----------' );
            console.log( '_closeAllNormdataPopovers: $Obj = ', $Obj );
        }
        
        var _close = $Obj.parent().find( 'i.closeAllPopovers' );
        
        if ( $( '.normdata-popover' ).length > 0 ) {
            _close.show();
            _close.on( 'click', function(event) {
                // close all popovers
                $( '.normdata-popover' ).each( function() {
                    $( this ).remove();
                } );
                
                // hide all close icons
                $( '.closeAllPopovers' ).each( function() {
                    $( this ).hide();
                } );
                
                // set trigger to enable
                $( _defaults.linkSelector ).removeAttr( 'disabled' ).removeClass( 'disabled' );
                
                event.preventDefault();
            } );
        }
        else {
            _close.hide();
        }
    }
    
    /**
     * Returns an JSON object from a API call.
     * 
     * @method _getRemoteData
     * @returns {Object} The JSON object with the API data.
     */
    function _getRemoteData( url, loader, icon ) {
        if ( _debug ) {
            console.log( '---------- _getRemoteData() ----------' );
            console.log( '_getRemoteData: url = ', url );
            console.log( '_getRemoteData: loader = ', loader );
            console.log( '_getRemoteData: icon = ', icon );
        }
         
        loader.show();
        icon.hide();
        
        var data = $.ajax( {
            url: decodeURI( url ),
            type: "GET",
            dataType: "JSON",
            async: false,
            success: function() {
                loader.hide();
                icon.show();
            }
        } ).responseText;
        
        
        return jQuery.parseJSON( data );
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
