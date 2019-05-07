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
 * Module which initializes a masonry grid.
 * 
 * @version 3.2.0
 * @module cmsJS.masonry
 * @requires jQuery
 */
var cmsJS = ( function( cms ) {
    'use strict';
    
    // variables
    var _debug = false;
    var _data = null;
    var _lazyGrid = null;
    var _defaults = {
        $grid: null,
        loaderSelector: '.tpl-masonry__loader'
    };
    
    // DOM-Elements
    var $gridItem = null;
    var $gridItemImage = null;
    var $gridItemImageLink = null;
    var $gridItemTitle = null;
    var $gridItemTitleLink = null;
    var $gridItemCaption = null;
    var $gridItemCaptionHeading = null;
    var $gridItemCaptionLink = null;
    
    cms.masonry = {
        /**
         * Method which initializes the Masonry Grid.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {Object} config.$grid An jQuery object which represents the grid
         * container.
         * @param {Object} data An data object which contains the images sources for the
         * grid.
         */
        init: function( config, data ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'cmsJS.masonry.init' );
                console.log( '##############################' );
                console.log( 'cmsJS.masonry.init: config - ', config );
                console.log( 'cmsJS.masonry.init: data - ', data );
            }
            
            $.extend( true, _defaults, config );
            
            // show loader
            $( _defaults.loaderSelector ).show();
            
            // render grid
            _renderMasonryGrid( data );
            
            // init masonry
            _lazyGrid = _defaults.$grid.imagesLoaded( function() {
                // init Masonry after all images have loaded
                _lazyGrid.masonry( {
                    itemSelector: '.grid-item',
                    columnWidth: '.grid-sizer',
                    gutter: '.gutter-sizer',
                    percentPosition: true
                } );
            } );
            
            // fade in grid after rendering
            _lazyGrid.on( 'layoutComplete', function( event, laidOutItems ) {
                // hide loader
                $( _defaults.loaderSelector ).hide();
                // show images
                _defaults.$grid.addClass( 'ready' );
            } );
            
        }
    };
    
    /**
     * Method which renders the masonry grid.
     * 
     * @method _renderMasonryGrid
     * @param {Object} data An data object which contains the images sources for the grid.
     */
    function _renderMasonryGrid( data ) {
        if ( _debug ) {
            console.log( '---------- _renderMasonryGrid() ----------' );
            console.log( '_renderMasonryGrid: data = ', data );
        }
        
        // create items
        data.items.forEach( function( item ) {
            // grid item
            $gridItem = $( '<div />' );
            if ( item.important ) {
                $gridItem.addClass( 'grid-item important' );
            }
            else {
                $gridItem.addClass( 'grid-item' );
            }
            
            // grid item title
            $gridItemTitle = $( '<div />' );
            if ( item.url !== '' ) {
                $gridItemTitleLink = $( '<a />' );
                $gridItemTitleLink.attr( 'href', item.url );
                $gridItemTitleLink.attr( 'title', item.title );
            }
            $gridItemTitle.addClass( 'grid-item-title' );
            $gridItemTitle.text( item.title );
            if ( item.url !== '' ) {
                $gridItemTitleLink.append( $gridItemTitle );
                $gridItem.append( $gridItemTitleLink );
            }
            else {
                $gridItem.append( $gridItemTitle );
            }
            
            // grid item caption
            if ( item.caption !== '' ) {
                $gridItemCaption = $( '<div />' );
                $gridItemCaption.addClass( 'grid-item-caption' );
                $gridItemCaption.html( item.caption );
                
                // grid item caption heading
                $gridItemCaptionHeading = $( '<h4 />' );
                $gridItemCaptionHeading.text( item.title );
                $gridItemCaption.prepend( $gridItemCaptionHeading );
                
                if ( item.url !== '' ) {
                    // grid item caption link
                    $gridItemCaptionLink = $( '<a />' );
                    $gridItemCaptionLink.attr( 'href', item.url );
                    $gridItemCaptionLink.attr( 'title', item.title );
                    
                    // append to grid item
                    $gridItemCaptionLink.append( $gridItemCaption );
                    $gridItem.append( $gridItemCaptionLink );
                }
                else {
                    $gridItem.append( $gridItemCaption );
                }
            }
            
            // grid item image
            $gridItemImage = $( '<img />' );
            $gridItemImage.attr( 'src', item.name );
            $gridItemImage.addClass( 'img-responsive' );
            
            if ( item.url !== '' ) {
                // grid item image link
                $gridItemImageLink = $( '<a />' );
                $gridItemImageLink.attr( 'href', item.url );
                $gridItemImageLink.attr( 'title', item.title );
                $gridItemImageLink.append( $gridItemImage );
                $gridItem.append( $gridItemImageLink );
            }
            else {
                $gridItem.append( $gridItemImage );
            }
            
            // append to grid
            _defaults.$grid.append( $gridItem );
        } );
    }
    
    return cms;
    
} )( cmsJS || {}, jQuery );
