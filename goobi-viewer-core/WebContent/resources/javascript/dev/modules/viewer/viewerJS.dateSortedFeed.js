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
 * Module to render a RSS-Feed sorted by date.
 * 
 * @version 3.2.0
 * @module viewerJS.dateSortedFeed
 * @requires jQuery
 * 
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false;
    var _promise = null;
    var _defaults = {
        path: null,
        dataSortOrder: null,
        dataCount: null,
        dataEncoding: null,
        feedBox: null,
        monthNames: [ '', 'Januar', 'Februar', 'März', 'April', 'Mai', 'Juni', 'Juli', 'August', 'September', 'Oktober', 'November', 'Dezember' ]
    };
    
    viewer.dateSortedFeed = {
        /**
         * Method which initializes the date sorted RSS-Feed.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.path The rootpath of the application.
         * @param {Object} config.feedBox An jQuery object of the wrapper DIV.
         * @example
         * 
         * <pre>
         * var dateSortedFeedConfig = {
         *     path: '#{request.contextPath}',
         *     feedBox: $( '#dateSortedFeed' )
         * };
         * 
         * viewerJS.dateSortedFeed.setDataSortOrder( '#{cc.attrs.sorting}' );
         * viewerJS.dateSortedFeed.setDataCount( '#{cc.attrs.count}' );
         * viewerJS.dateSortedFeed.setDataEncoding( '#{cc.attrs.encoding}' );
         * viewerJS.dateSortedFeed.init( dateSortedFeedConfig );
         * </pre>
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.dateSortedFeed.init' );
                console.log( '##############################' );
                console.log( 'viewer.dateSortedFeed.init: feedBoxObj - ' + feedBoxObj );
                console.log( 'viewer.dateSortedFeed.init: path - ' + path );
            }
            
            $.extend( true, _defaults, config );
            
            var dataURL = _defaults.path;
            dataURL += '/api?action=query&q=PI:*&jsonFormat=datecentric&sortField=DATECREATED';
            dataURL += '&sortOrder=';
            dataURL += _defaults.dataSortOrder;
            dataURL += '&count=';
            dataURL += _defaults.dataCount;
            dataURL += '&encoding=';
            dataURL += _defaults.dataEncoding;
            
            if ( _debug ) {
                console.log( 'viewer.dateSortedFeed.init: dataURL - ' + dataURL );
            }
            
            // checking for feedbox element and render feed
            if ( _defaults.feedBox ) {
                _promise = viewer.helper.getRemoteData( dataURL );
                
                _promise.then( function( data ) {
                    _renderFeed( data );
                } ).then( null, function() {
                    console.error( 'ERROR: viewer.dateSortedFeed.init - ', error );
                } );
            }
            else {
                return;
            }
        },
        /**
         * Returns the sorting order of the feed.
         * 
         * @method getDataSortOrder
         * @returns {String} The sorting order.
         * 
         */
        getDataSortOrder: function() {
            return _defaults.dataSortOrder;
        },
        /**
         * Sets the sorting order of the feed.
         * 
         * @method getDataSortOrder
         * @param {String} str The sorting order (asc/desc)
         * 
         */
        setDataSortOrder: function( str ) {
            _defaults.dataSortOrder = str;
        },
        /**
         * Returns the number of entries from the feed.
         * 
         * @method getDataSortOrder
         * @returns {String} The number of entries.
         * 
         */
        getDataCount: function() {
            return _defaults.dataCount;
        },
        /**
         * Sets the number of entries from the feed.
         * 
         * @method setDataCount
         * @param {String} The number of entries.
         * 
         */
        setDataCount: function( num ) {
            _defaults.dataCount = num;
        },
        /**
         * Returns the type of encoding.
         * 
         * @method getDataEncoding
         * @returns {String} The type of encoding.
         * 
         */
        getDataEncoding: function() {
            return _defaults.dataEncoding;
        },
        /**
         * Sets the type of encoding.
         * 
         * @method setDataEncoding
         * @param {String} The type of encoding.
         * 
         */
        setDataEncoding: function( str ) {
            _defaults.dataEncoding = str;
        }
    };
    
    /**
     * Renders the feed and appends it to the wrapper.
     * 
     * @method _renderFeed
     * @param {Object} data An JSON object of the feed data.
     * 
     */
    function _renderFeed( data ) {
        var feed = '';
        $.each( data, function( i, j ) {
            feed += '<h4>' + _dateConverter( j.date ) + '</h4>';
            $.each( j, function( m, n ) {
                if ( n.title ) {
                    for ( var x = 0; x <= n.title.length; x++ ) {
                        if ( n.title[ x ] !== undefined ) {
                            feed += '<div class="sorted-feed-title"><a href="';
                            feed += n.url;
                            feed += '" title="';
                            feed += n.title[ x ];
                            feed += '">';
                            feed += n.title[ x ];
                            feed += '</a></div>';
                        }
                    }
                }
            } );
        } );
        
        _defaults.feedBox.append( feed );
    }
    
    /**
     * Converts a date to this form: 16. November 2015
     * 
     * @method _dateConverter
     * @param {String} str The date to convert.
     * @returns {String} The new formated date.
     * 
     */
    function _dateConverter( str ) {
        var strArr = str.split( '-' );
        var monthIdx = parseInt( strArr[ 1 ] );
        var newDate = strArr[ 2 ] + '. ' + _defaults.monthNames[ monthIdx ] + ' ' + strArr[ 0 ];
        
        return newDate;
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
