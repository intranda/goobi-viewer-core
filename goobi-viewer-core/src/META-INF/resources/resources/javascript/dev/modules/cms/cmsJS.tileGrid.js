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
 * Module which initializes a tile grid.
 * 
 * @version 3.2.0
 * @module cmsJS.tileGrid
 * @requires jQuery
 */
var cmsJS = ( function( cms ) {
    'use strict';
    
    // variables
    var _debug = false;
    var _data = null;
    var _tiles = [];
    var _defaults = {
        $highlights: null,
        $grid: null
    };
    
    // DOM-Elements
    var $gridItem = null;
    var $gridItemLink = null;
    var $gridItemCaptionLink = null;
    var $gridItemCaptionIcon = null;
    var $gridItemFigure = null;
    var $gridItemFigcaption = null;
    var $gridItemFigcaptionHeader = null;
    var $gridItemImage = null;
    
    cms.tileGrid = {
        init: function( config, data ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'cmsJS.tileGrid.init' );
                console.log( '##############################' );
                console.log( 'cmsJS.tileGrid.init: config - ', config );
                console.log( 'cmsJS.tileGrid.init: data - ', data );
            }
            
            $.extend( true, _defaults, config );
            
            _tiles = _getTiles( data );
            _renderTileGrid( _tiles );
        }
    };
    
    function _getTiles( data ) {
        if ( _debug ) {
            console.log( '---------- _getTiles() ----------' );
            console.log( '_getTiles: data = ', data );
        }
        
        var tiles = [];
        
        data.items.forEach( function( item ) {
            $gridItem = $( '<div />' );
            $gridItemLink = $( '<a />' );
            $gridItemCaptionLink = $( '<a />' );
            $gridItemCaptionIcon = $( '<i aria-hidden="true" />' );
            $gridItemFigure = $( '<figure />' );
            $gridItemFigcaption = $( '<figcaption />' );
            $gridItemFigcaptionHeader = $( '<h3 />' );
            $gridItemImage = $( '<img />' );
            
            // create item
            if ( item.important ) {
                $gridItem.addClass( 'tpl-tile-grid__grid-item important' );
            }
            else {
                $gridItem.addClass( 'tpl-tile-grid__grid-item' );
            }
            if ( item.size !== '' ) {
                $gridItem.addClass( item.size );
            }
            $gridItemImage.attr( 'src', item.name );
            // $gridItemImage.attr( 'class', 'img-responsive' );
            $gridItemImage.attr( 'alt', item.title );
            if ( item.url !== '' ) {
                $gridItemLink.attr( 'href', item.url );
                $gridItemLink.append( $gridItemImage );
                $gridItemFigure.append( $gridItemLink );
            }
            else {
                $gridItemFigure.append( $gridItemImage );
            }
            $gridItemFigcaptionHeader.text( item.title );
            $gridItemFigure.append( $gridItemFigcaptionHeader );
            $gridItemFigcaption.append( item.caption );
            if ( item.url !== '' ) {
                $gridItemCaptionLink.attr( 'href', item.url );
                $gridItemCaptionIcon.addClass( 'fa fa-arrow-right' );
                $gridItemCaptionLink.append( $gridItemCaptionIcon );
                $gridItemFigcaption.append( $gridItemCaptionLink );
            }
            $gridItemFigure.append( $gridItemFigcaption );
            $gridItem.append( $gridItemFigure );
            
            // push tile into tiles array
            tiles.push( $gridItem );
        } );
        
        return tiles;
    }
    
    function _renderTileGrid( tiles ) {
        if ( _debug ) {
            console.log( '---------- _renderTileGrid() ----------' );
            console.log( '_renderTileGrid: tiles = ', tiles );
        }
        
        tiles.forEach( function( tile ) {
            if ( tile.hasClass( 'important' ) ) {
                _defaults.$highlights.append( tile );
            }
            else {
                _defaults.$grid.append( tile );
            }
        } );
    }
    
    return cms;
    
} )( cmsJS || {}, jQuery );
