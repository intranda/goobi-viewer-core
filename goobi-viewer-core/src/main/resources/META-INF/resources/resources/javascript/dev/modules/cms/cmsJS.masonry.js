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
        loaderSelector: '.tpl-masonry__loader',
		language: 'de'
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
        init: function( configuration, data ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'cmsJS.masonry.init' );
                console.log( '##############################' );
                console.log( 'cmsJS.masonry.init: config - ', configuration );
                console.log( 'cmsJS.masonry.init: data - ', data );
            }
            
            var config = $.extend( true, {}, _defaults, configuration );
            
            // show loader
            $( config.loaderSelector ).show();
            
            // render grid
            _renderMasonryGrid( data, config );
            
            let $images = config.$grid.find('img');
            let promises = [];
            $images.each( (index,element) => {
            	let promise = new Promise((res, rej) => {
            		element.onload = e => res(e);
            		element.onerror = e => rej(e);
            	});
            	promises.push(promise);
            });
            
            Promise.allSettled(promises)
            .then( array => {
            	//console.log("all images loaded in ", config.$grid, array);
                // init Masonry after all images have loaded
                config.$grid.masonry( {
                    itemSelector: '.grid-item',
                    columnWidth: '.grid-sizer',
                    gutter: '.gutter-sizer',
                    percentPosition: true
                } );
            })
            .catch( e => {
            	console.log("error when trying to detect all images loaded", e);
            });

            // fade in grid after rendering
            config.$grid.on( 'layoutComplete', function( event, laidOutItems ) {
                // hide loader 
                $( config.loaderSelector ).hide();
                // show images
                config.$grid.addClass( 'ready' );
            } );
            
        }
    };
    
    /**
     * Method which renders the masonry grid.
     * 
     * @method _renderMasonryGrid
     * @param {Object} data An data object which contains the images sources for the grid.
     */
    function _renderMasonryGrid( data, config ) {
        if ( _debug ) {
            console.log( '---------- _renderMasonryGrid() ----------' );
            console.log( '_renderMasonryGrid: data = ', data );
        }
        
        // create items
        data.mediaItems.forEach( function( item ) {
            // grid item
            $gridItem = $( '<div />' );
            if ( item.important ) {
                $gridItem.addClass( 'grid-item important ' + item.tags.join(' ') );
            }

            else {
                $gridItem.addClass( 'grid-item ' + item.tags.join(' '));
            }
            
            let label = viewerJS.getMetadataValue(item.label, config.language );
            let description = viewerJS.getMetadataValue(item.description, config.language );
            
            let image = item.image["@id"];
            if(item.image.service) { 
                image = item.image.service["@id"] + "/full/max/0/default.jpg";
            }
            

            // grid item title
            if(label) {                
                $gridItemTitle = $( '<div />' );
                if ( item.link ) {
                    $gridItemTitleLink = $( '<a />' );
                    $gridItemTitleLink.attr( 'href', item.link );
                    $gridItemTitleLink.attr( 'title', label );
                }
                $gridItemTitle.addClass( 'grid-item-title' );
                $gridItemTitle.text( label );
                if ( item.link  ) {
                    $gridItemTitleLink.append( $gridItemTitle );
                    $gridItem.append( $gridItemTitleLink );
                }
                else {
                    $gridItem.append( $gridItemTitle );
                }
            }
            
            // grid item caption
            if ( description ) {
                $gridItemCaption = $( '<div />' );
                $gridItemCaption.addClass( 'grid-item-caption' );
                $gridItemCaption.html( '<span>' + description + '</span>');
                
                // grid item caption heading
                $gridItemCaptionHeading = $( '<h2 />' );
                $gridItemCaptionHeading.addClass( 'h3' );
                $gridItemCaptionHeading.text( label );
                $gridItemCaption.prepend( $gridItemCaptionHeading );
                
                if ( item.link !== '' ) {
                    // grid item caption link
                    $gridItemCaptionLink = $( '<a />' );
                    $gridItemCaptionLink.attr( 'href', item.link );
                    $gridItemCaptionLink.attr( 'title', label );
                    
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
            $gridItemImage.attr( 'src', image );
            $gridItemImage.attr( 'alt', label );
            $gridItemImage.addClass( 'img-fluid' );
             
            if ( item.link !== '' ) {
                // grid item image link
                $gridItemImageLink = $( '<a />' );
                $gridItemImageLink.attr( 'href', item.link );
                $gridItemImageLink.attr( 'title', label );
                $gridItemImageLink.append( $gridItemImage );
                $gridItem.append( $gridItemImageLink );
            }
            else {
                $gridItem.append( $gridItemImage );
            }
            
            // append to grid
            config.$grid.append( $gridItem );
        } );
    }
    
    return cms;
    
} )( cmsJS || {}, jQuery );
