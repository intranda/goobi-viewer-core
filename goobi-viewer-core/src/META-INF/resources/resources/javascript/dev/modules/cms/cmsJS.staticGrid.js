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
 * Module which initializes a static grid.
 * 
 * @version 3.2.0
 * @module cmsJS.staticGrid
 * @requires jQuery
 */
var cmsJS = ( function( cms ) {
    'use strict';
    
    // variables
    var _debug = false;
    var _data = null;
    var _defaults = {
        gridSelector: '.tpl-static-grid__grid'
    };
    
    // DOM elements
    var _grid = null;
    var _gridRow = null;
    var _gridCol = null;
    var _gridTile = null;
    var _gridTileTitle = null;
    var _gridTileTitleLink = null;
    var _gridTileTitleH4 = null;
    var _gridTileImage = null;
    var _gridTileImageLink = null;
    
    cms.staticGrid = {
        /**
         * Method which initializes the Masonry Grid.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.gridSelector The selector for the grid container.
         * @param {Object} data An data object which contains the images sources for the
         * grid.
         */
        init: function( config, data ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'cmsJS.staticGrid.init' );
                console.log( '##############################' );
                console.log( 'cmsJS.staticGrid.init: config - ', config );
                console.log( 'cmsJS.staticGrid.init: data - ', data );
            }
            
            $.extend( true, _defaults, config );
            
            // render grid
            _grid = _buildGrid( data );
            $( _defaults.gridSelector ).append( _grid );
        }
    };
    
    /**
     * Method to build the elements of the static grid.
     * 
     * @method _buildGrid
     * @param {Object} data A JSON data object which contains the image informations.
     * @returns {Object} An jQuery object which contains all grid elements.
     */
    function _buildGrid( data ) {
        if ( _debug ) {
            console.log( '---------- _buildGrid() ----------' );
            console.log( '_buildGrid: data = ', data );
        }
        
        _gridRow = $( '<div class="row" />' );
        
        data.items.forEach( function( item ) {
            _gridCol = $( '<div class="col-xs-6 col-sm-3" />' );
            // tile
            _gridTile = $( '<div class="grid-tile" />' );
            // title
            _gridTileTitle = $( '<div class="grid-tile__title" />' );
            _gridTileTitleH4 = $( '<h4 />' );
            _gridTileTitleLink = $( '<a />' );
            if ( item.url !== '' ) {
                _gridTileTitleLink.attr( 'href', item.url );
            }
            else {
                _gridTileTitleLink.attr( 'href', '#' );
            }
            _gridTileTitleLink.attr( 'title', item.title );
            _gridTileTitleLink.append( item.title );
            _gridTileTitleH4.append( _gridTileTitleLink );
            // image
            _gridTileImage = $( '<div class="grid-tile__image" />' );
            _gridTileImage.css( 'background-image', 'url(' + item.name + ')' );
            _gridTileImageLink = $( '<a />' );
            if ( item.url !== '' ) {
                _gridTileImageLink.attr( 'href', item.url );
            }
            else {
                _gridTileImageLink.attr( 'href', '#' );
            }
            // concat everything
            _gridTileTitle.append( _gridTileTitleH4 );
            _gridTile.append( _gridTileTitle );
            _gridTileImage.append( _gridTileImageLink );
            _gridTile.append( _gridTileTitle );
            _gridTile.append( _gridTileImage );
            _gridCol.append( _gridTile );
            _gridRow.append( _gridCol );
        } );
        
        return _gridRow;
    }
    
    return cms;
    
} )( cmsJS || {}, jQuery );
