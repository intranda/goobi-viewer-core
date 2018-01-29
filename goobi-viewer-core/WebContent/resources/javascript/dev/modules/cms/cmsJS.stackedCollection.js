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
 * Module which renders collections as an accordion view.
 * 
 * @version 3.2.0
 * @module cmsJS.stackedCollection
 * @requires jQuery
 */
var cmsJS = ( function( cms ) {
    'use strict';
    
    // variables
    var _debug = false;
    var _toggleAttr = false;
    var _defaults = {
        collectionsSelector: '.tpl-stacked-collection__collections',
        collectionDefaultThumb: '',
        msg: {
            noSubCollectionText: ''
        }
    };
    
    cms.stackedCollection = {
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
                console.log( 'cmsJS.stackedCollections.init' );
                console.log( '##############################' );
                console.log( 'cmsJS.stackedCollections.init: config - ', config );
                console.log( 'cmsJS.stackedCollections.init: data - ', data );
            }
            
            $.extend( true, _defaults, config );
            
            // render RSS Feed
            _renderCollections( data );
        }
    };
    
    /**
     * Method which renders the collection accordion.
     * 
     * @method _renderCollections
     * @param {Object} data The RSS information data object.
     */
    function _renderCollections( data ) {
        if ( _debug ) {
            console.log( '---------- _renderCollections() ----------' );
            console.log( '_renderCollections: data = ', data );
        }
        
        var counter = 0;
        
        // DOM elements
        var panelGroup = $( '<div />' ).attr( 'id', 'stackedCollections' ).attr( 'role', 'tablist' ).addClass( 'panel-group' );
        var panel = null;
        var panelHeading = null;
        var panelThumbnail = null;
        var panelThumbnailImage = null;
        var panelTitle = null;
        var panelTitleLink = null;
        var panelRSS = null;
        var panelRSSLink = null;
        var panelCollapse = null;
        var panelBody = null;
        
        // create members
        data.members.forEach( function( member ) {
            // increase counter
            counter++;
            // create panels
            panel = $( '<div />' ).addClass( 'panel' );
            // create panel title
            panelHeading = $( '<div />' ).addClass( 'panel-heading' );
            panelTitle = $( '<h4 />' ).addClass( 'panel-title' );
            panelTitleLink = $( '<a />' ).text( member.label + ' (' + _getMetadataValue( member, 'volumes' ) + ')' );
            // check if subcollections exist
            if ( _getMetadataValue( member, 'subCollections' ) > 0 ) {
                panelTitleLink.attr( 'href', '#collapse-' + counter ).attr( 'role', 'button' ).attr( 'data-toggle', 'collapse' ).attr( 'data-parent', '#stackedCollections' )
                        .attr( 'aria-expanded', 'false' );
            }
            else {
                panelTitleLink.attr( 'href', member.rendering[ '@id' ] );
            }
            panelTitle.append( panelTitleLink );
            // create RSS link
            panelRSS = $( '<div />' ).addClass( 'panel-rss' );
            panelRSSLink = $( '<a />' ).attr( 'href', member.related[ '@id' ] ).attr( 'target', '_blank' ).html( '<i class="fa fa-rss" aria-hidden="true"></i>' );
            panelRSS.append( panelRSSLink );
            // create panel thumbnail if exist
            panelThumbnail = $( '<div />' ).addClass( 'panel-thumbnail' );
            if ( member.thumbnail ) {
                panelThumbnailImage = $( '<img />' ).attr( 'src', member.thumbnail ).addClass( 'img-responsive' );
                panelThumbnail.append( panelThumbnailImage );
            }
            else {
                panelThumbnailImage = $( '<img />' ).attr( 'src', _defaults.collectionDefaultThumb ).addClass( 'img-responsive' );
                panelThumbnail.append( panelThumbnailImage );
            }
            // build title
            panelHeading.append( panelThumbnail ).append( panelTitle ).append( panelRSS );
            // create collapse
            panelCollapse = $( '<div />' ).attr( 'id', 'collapse-' + counter ).attr( 'role', 'tabpanel' ).attr( 'aria-expanded', 'false' ).addClass( 'panel-collapse collapse' );
            // create panel body
            panelBody = $( '<div />' ).addClass( 'panel-body' ).append( _renderSubCollections( member[ "@id" ] ) );
            // build collapse
            panelCollapse.append( panelBody );
            // build panel
            panel.append( panelHeading ).append( panelCollapse );
            // build panel group
            panelGroup.append( panel );
            
            $( _defaults.collectionsSelector ).append( panelGroup );
        } );
    }
    
    /**
     * Method to retrieve metadata value of the metadata object with the given label and
     * within the given collection object.
     * 
     * @param collection {Object} The iiif-presentation collection object cotaining the
     * metadata.
     * @param label {String} The label property value of the metadata to return.
     * @returns {String} The count of works in the collection.
     */
    function _getMetadataValue( collection, label ) {
        if ( _debug ) {
            console.log( '---------- _getMetadataValue() ----------' );
            console.log( '_getMetadataValue: collection = ', collection );
            console.log( '_getMetadataValue: label = ', label );
        }
        
        var value = '';
        
        collection.metadata.forEach( function( metadata ) {
            if ( metadata.label == label ) {
                value = metadata.value;
            }
        } );
        
        return value;
    }
    
    /**
     * Method which renders the subcollections.
     * 
     * @method _renderSubCollections
     * @param {String} url The URL to the API which fetches the subcollection data.
     * @returns {String} The HTML string of the subcollections.
     */
    function _renderSubCollections( url ) {
        if ( _debug ) {
            console.log( '---------- _renderSubCollections() ----------' );
            console.log( '_renderSubCollections: url = ', url );
        }
        
        // DOM elements
        var subCollections = $( '<ul />' ).addClass( 'list' );
        var subCollectionItem = null;
        var subCollectionItemLink = null;
        var subCollectionItemRSSLink = null;
        
        // get subcollection data
        $.ajax( {
            url: url,
            type: 'GET',
            datatype: 'JSON'
        } ).then( function( data ) {
            subCollectionItem = $( '<li />' );
            
            if ( !$.isEmptyObject( data.members ) ) {
                // add subcollection items
                data.members.forEach( function( member ) {
                    subCollectionItemLink = $( '<a />' ).attr( 'href', member.rendering[ '@id' ] ).addClass( 'panel-body__collection' ).text( member.label + ' ('
                            + _getMetadataValue( member, 'volumes' ) + ')' );
                    subCollectionItemRSSLink = $( '<a />' ).attr( 'href', member.related[ '@id' ] ).attr( 'target', '_blank' ).addClass( 'panel-body__rss' )
                            .html( '<i class="fa fa-rss" aria-hidden="true"></i>' );
                    // build subcollection item
                    subCollectionItem.append( subCollectionItemLink ).append( subCollectionItemRSSLink );
                    subCollections.append( subCollectionItem );
                } );
            }
            else {
                // create empty item link
                subCollectionItemLink = $( '<a />' ).attr( 'href', data.rendering[ '@id' ] ).text( _defaults.msg.noSubCollectionText + '.' );
                // build empty item
                subCollectionItem.append( subCollectionItemLink );
                subCollections.append( subCollectionItem );
            }
        } );
        
        return subCollections;
    }
    
    return cms;
    
} )( cmsJS || {}, jQuery );
