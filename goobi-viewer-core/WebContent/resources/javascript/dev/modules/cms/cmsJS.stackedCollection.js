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
    var _debug = true;
    var _defaults = {
        collectionsSelector: '.tpl-stacked-collection__collections',
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
            
            // set first panel visible
            $( '#stackedCollections .panel:first' ).find( 'h4 a' ).attr( 'aria-expanded', 'true' ).removeClass( 'collapsed' );
            $( '#stackedCollections .panel:first' ).find( '.panel-collapse' ).attr( 'aria-expanded', 'true' ).addClass( 'in' );
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
            panelTitleLink = $( '<a />' ).attr( 'role', 'button' ).attr( 'data-toggle', 'collapse' ).attr( 'data-parent', '#stackedCollections' ).attr( 'href', '#collapse-'
                    + counter ).attr( 'aria-expanded', 'false' ).text( member.label );
            panelTitle.append( panelTitleLink );
            
            // TODO: Anzahl der Objekte im Werk in Klammern hinter den Titel
            
            // TODO: RSS-Feed verlinken
            panelRSS = $( '<div />' ).addClass( 'panel-rss' );
            panelRSSLink = $( '<a />' ).attr( 'href', '#' ).html( '<i class="fa fa-rss" aria-hidden="true"></i>' );
            panelRSS.append( panelRSSLink );
            
            // create panel thumbnail if exist
            panelThumbnail = $( '<div />' ).addClass( 'panel-thumbnail' );
            if ( member.thumbnail ) {
                panelThumbnailImage = $( '<img />' ).attr( 'src', member.thumbnail ).addClass( 'img-responsive' );
                panelThumbnail.append( panelThumbnailImage );
            }
            // build title
            panelHeading.append( panelThumbnail ).append( panelTitle ).append( panelRSS );
            // create collapse
            panelCollapse = $( '<div />' ).attr( 'id', 'collapse-' + counter ).attr( 'role', 'tabpanel' ).attr( 'aria-expanded', 'false' ).addClass( 'panel-collapse collapse' );
            // create panel body
            
            // TODO: @id muss umbenannt werden, da es zu Fehlern beim Aufruf von
            // member.@id führt
            panelBody = $( '<div />' ).addClass( 'panel-body' ).append( _renderSubCollections( member["@id"] ) );
            
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
        
        // get subcollection data
        $.ajax( {
            url: url,
            type: 'GET',
            datatype: 'JSON'
        } ).then( function( data ) {
            // add subcollection items
            data.members.forEach( function( member ) {
                // create subcollection item
                subCollectionItem = $( '<li />' );
                subCollectionItemLink = $( '<a />' ).attr( 'href', '#' ).text( member.label );
                // buils subcollection item
                subCollectionItem.append( subCollectionItemLink );
                subCollections.append( subCollectionItem );
            } );
        } );
        
        return subCollections;
    }
    
    return cms;
    
} )( cmsJS || {}, jQuery );
