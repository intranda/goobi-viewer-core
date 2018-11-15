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
 * Module which renders the RSS Feed.
 * 
 * @version 3.2.0
 * @module cmsJS.rssFeed
 * @requires jQuery
 */
var cmsJS = ( function( cms ) {
    'use strict';
    
    // variables
    var _debug = false;
    var _defaults = {
        rssFeedSelector: '.tpl-rss__feed',
        msg: {
            continueReading: 'Weiterlesen'
        }
    };
    
    cms.rssFeed = {
        /**
         * Method which initializes the RSS Feed.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {Object} data An data object which contains the images sources for the
         * grid.
         */
        init: function( config, data ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'cmsJS.rssFeed.init' );
                console.log( '##############################' );
                console.log( 'cmsJS.rssFeed.init: config - ', config );
                console.log( 'cmsJS.rssFeed.init: data - ', data );
            }
            
            $.extend( true, _defaults, config );
            
            // render RSS Feed
            _renderRssFeed( data );
        }
    };
    
    /**
     * Method which renders the RSS feed into the DOM.
     * 
     * @method _renderRssFeed
     * @param {Object} data The RSS information data object.
     */
    function _renderRssFeed( data ) {
        if ( _debug ) {
            console.log( '---------- _renderRssFeed() ----------' );
            console.log( '_renderRssFeed: data = ', data );
        }
        
        // DOM elements
        var rssItem = null;
        var rssItemTitle = null;
        var rssItemTitleH4 = null;
        var rssItemTitleLink = null;
        var rssItemRow = null;
        var rssItemColLeft = null;
        var rssItemColRight = null;
        var rssItemImageWrapper = null;
        var rssItemDocTypeH3 = null;
        var rssItemImage = null;
        var rssItemImageLink = null;
        var rssItemDate = null;
        var rssItemTime = null;
        var rssItemMetadata = null;
        var rssItemMetadataKey = null;
        var rssItemMetadataValue = null;
        var rssItemLinkWrapper = null;
        var rssItemLink = null;
        
        // create items
        data.items.forEach( function( item ) {
            // create item wrapper
            rssItem = $( '<div />' );
            rssItem.addClass( 'tpl-rss__item' );
            
            // create item content
            rssItemRow = $( '<div />' ).addClass( 'row' );
            
            // left
            rssItemColLeft = $( '<div />' ).addClass( 'col-xs-3' );
            rssItemDocTypeH3 = $( '<h3 />' ).text( item.docType );
            rssItemImageWrapper = $( '<div />' ).addClass( 'tpl-rss__item-image' );
            rssItemImageLink = $( '<a />' ).attr( 'href', item.link );
            rssItemImage = $( '<img />' ).attr( 'src', item.description.image ).addClass( 'img-responsive' );
            rssItemImageLink.append( rssItemImage );
            rssItemImageWrapper.append( rssItemImageLink );
            rssItemColLeft.append( rssItemDocTypeH3 ).append( rssItemImageWrapper );
            
            // right
            rssItemColRight = $( '<div />' ).addClass( 'col-xs-9' );
            
            // create item title
            rssItemTitle = $( '<div />' ).addClass( 'tpl-rss__item-title' );
            rssItemTitleH4 = $( '<h4 />' );
            rssItemTitleLink = $( '<a />' ).attr( 'href', item.link ).text( item.title );
            rssItemTitleH4.append( rssItemTitleLink );
            rssItemTitle.append( rssItemTitleH4 );
                        
            // create metadata
            rssItemMetadata = $( '<dl />' ).addClass( 'tpl-rss__item-metadata dl-horizontal' );
            item.description.metadata.forEach( function( metadata ) {
                rssItemMetadataKey = $( '<dt />' ).text( metadata.label + ':' );
                rssItemMetadataValue = $( '<dd />' ).text( metadata.value );
                rssItemMetadata.append( rssItemMetadataKey ).append( rssItemMetadataValue );
            } );
            // link to work
            rssItemLinkWrapper = $( '<div />' ).addClass( 'tpl-rss__item-link' );
            rssItemLink = $( '<a />' ).attr( 'href', item.link ).addClass( 'btn btn--full' ).text( _defaults.msg.continueReading );
            rssItemLinkWrapper.append( rssItemLink );

            // append to col right
            rssItemColRight.append( rssItemTitle ).append( rssItemDate ).append( rssItemMetadata ).append( rssItemLinkWrapper);
            
            // append to row
            rssItemRow.append( rssItemColLeft ).append( rssItemColRight );
            
            // create item
            rssItem.append( rssItemRow );
            
            $( _defaults.rssFeedSelector ).append( rssItem );
        } );
    }
    
    return cms;
    
} )( cmsJS || {}, jQuery );
