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
 * Module which generates a responsive image gallery in columns.
 * 
 * @version 3.2.0
 * @module viewerJS.responsiveColumnGallery
 * @requires jQuery
 * 
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    // default variables
    var _debug = false;
    var _defaults = {
        themePath: '',
        imagePath: '',
        imageDataFile: '',
        galleryObject: null,
        maxColumnCount: null,
        maxImagesPerColumn: null,
        fixedHeight: false,
        maxHeight: '',
        caption: true,
        overlayColor: '',
        lang: {},
        lightbox: {
            active: true,
            caption: true
        },
    };
    var _promise = null;
    var _imageData = null;
    var _parentImage = null;
    var _lightboxImage = null;
    var _imageLightbox = null;
    var _smallViewport = null;
    var _dataUrl = null;
    
    viewer.responsiveColumnGallery = {
        /**
         * Method which initializes the column gallery.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.themePath The path to the current activated viewer
         * theme.
         * @param {String} config.imagePath The path to the used images.
         * @param {String} config.imageDataFile The path to the JSON-File, which contains
         * the images data.
         * @param {Object} config.galleryObject The DIV where the gallery should be
         * rendered.
         * @param {Number} config.maxColumnCount Count count of the gallery, 4 column are
         * maximum.
         * @param {Number} config.maxImagesPerColumn Count of the images per column.
         * @param {Boolean} config.fixedHeight If true the images have a fixed height,
         * default is false.
         * @param {String} config.maxHeight Sets the given max height value for the
         * images.
         * @param {Boolean} config.caption If true the gallery images have a caption with
         * the title text, default is true.
         * @param {String} config.overlayColor Takes a HEX-value to set the color of the
         * image overlay.
         * @param {Object} config.lang An object of strings for multilanguage
         * functionality.
         * @param {Object} config.lightbox An Object to configure the image lightbox.
         * @param {Boolean} config.lightbox.active If true the lightbox functionality is
         * enabled, default is true.
         * @param {Boolean} config.lightbox.caption If true the lightbox has a caption
         * text, default is true.
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.responsiveColumnGallery.init' );
                console.log( '##############################' );
                console.log( 'viewer.responsiveColumnGallery.init: config = ', config );
            }
            
            $.extend( true, _defaults, config );
            
            // fetch image data and check the viewport
            _dataUrl = _defaults.themePath + _defaults.imageDataFile;
            
            _promise = viewer.helper.getRemoteData( _dataUrl );
            
            _promise.then( function( imageData ) {
                _imageData = imageData;
                _smallViewport = viewer.responsiveColumnGallery.checkForSmallViewport();
                
                // render columns
                if ( _defaults.maxColumnCount > 4 ) {
                    _defaults.galleryObject.append( viewer.helper.renderAlert( 'alert-danger', 'Die maximale Spaltenanzahl für die Galerie beträgt 4!', true ) );
                    
                    return false;
                }
                else {
                    for ( var i = 0; i < _defaults.maxColumnCount; i++ ) {
                        _defaults.galleryObject.append( viewer.responsiveColumnGallery.renderColumns( _defaults.maxColumnCount ) );
                    }
                }
                
                // render images
                while ( _imageData.length ) {
                    $.each( $( '.rcg-col' ), function() {
                        $( this ).append( viewer.responsiveColumnGallery.renderImages( _imageData.splice( 0, _defaults.maxImagesPerColumn ) ) );
                    } );
                }
                
                // set fixed height if activated and viewport is > 375px
                if ( _defaults.fixedHeight && !_smallViewport ) {
                    $.each( $( '.rcg-image-body' ), function() {
                        viewer.responsiveColumnGallery.fixedHeight( $( this ) );
                    } );
                }
                
                // prepare lightbox
                if ( _defaults.lightbox.active ) {
                    $( '.lightbox-toggle' ).on( 'click', function( event ) {
                        event.preventDefault();
                        
                        _parentImage = $( this ).parent().children( 'img' );
                        _lightboxImage = viewer.responsiveColumnGallery.prepareLightbox( _parentImage );
                        _imageLightbox = viewer.responsiveColumnGallery.renderLightbox( _lightboxImage );
                        
                        $( 'body' ).append( _imageLightbox );
                        
                        $( '.rcg-lightbox-body' ).hide();
                        
                        $( '.rcg-lightbox-overlay' ).fadeIn( 'slow' );
                        
                        // first load image, then center it and show it up
                        $( '.rcg-lightbox-image img' ).on("load", function() {
                            viewer.responsiveColumnGallery.centerLightbox( $( '.rcg-lightbox-body' ) );
                            $( '.rcg-lightbox-body' ).show();
                        } );
                        
                        // close lightbox via button
                        $( '.rcg-lightbox-close' ).on( 'click', function() {
                            $( '.rcg-lightbox-overlay' ).remove();
                        } );
                        
                        // close lightbox via esc
                        $( document ).keypress( function( event ) {
                            if ( event.keyCode === 27 ) {
                                $( '.rcg-lightbox-overlay' ).remove();
                            }
                        } );
                        
                        // close lightbox via click on picture
                        $( '.rcg-lightbox-image img' ).on( 'click', function() {
                            $( '.rcg-lightbox-overlay' ).remove();
                        } );
                    } );
                }
            } ).then( null, function( error ) {
                _defaults.galleryObject.append( viewer.helper.renderAlert( 'alert-danger', '<strong>Status: </strong>' + error.status + ' ' + error.statusText, false ) );
                console.error( 'ERROR: viewer.responsiveColumnGallery.init - ', error );
            } );
            
        },
        /**
         * Method which renders the gallery columns.
         * 
         * @method renderColumns
         * @param {String} count The column count of the gallery.
         * @returns {String} A HTML-String which renders a column.
         */
        renderColumns: function( count ) {
            if ( _debug ) {
                console.log( '---------- viewer.responsiveColumnGallery.renderColumns() ----------' );
                console.log( 'viewer.responsiveColumnGallery.renderColumns: count = ', count );
            }
            var column = '';
            
            column += '<div class="rcg-col col-' + count + '"></div>';
            
            return column;
        },
        /**
         * Method which renders the gallery images.
         * 
         * @method renderImages
         * @param {Object} data An object of image data to render the images.
         * @returns {String} A HTML-String which renders the gallery images.
         */
        renderImages: function( data ) {
            if ( _debug ) {
                console.log( '---------- viewer.responsiveColumnGallery.renderImages() ----------' );
                console.log( 'viewer.responsiveColumnGallery.renderImages: data = ', data );
            }
            var image = '';
            
            $.each( data, function( i, j ) {
                $.each( j, function( m, n ) {
                    image += '<div class="rcg-image-container">';
                    image += '<div class="rcg-image-body">';
                    image += '<a href="' + n.url + '">';
                    image += '<div class="rcg-image-overlay" style="background-color:' + _defaults.overlayColor + '"></div>';
                    image += '</a>';
                    image += '<div class="rcg-image-title">';
                    image += '<h3>' + n.title + '</h3>';
                    image += '</div>';
                    image += '<img src="' + _defaults.themePath + _defaults.imagePath + n.name + '" alt="' + n.alt + '" />';
                    if ( _defaults.lightbox.active ) {
                        image += '<div class="lightbox-toggle" title="' + _defaults.lang.showLightbox + '">';
                        image += '<span class="icon-wrapper lightbox-toggle__icon" aria-hidden="true">';
                        image += '<svg class="icon" focusable="false"><use href="' + _defaults.themePath + 'resources/icons/outline/arrow-move.svg#icon"></use></svg>';
                        image += '</span>';
                        image += '</div>';
                    }
                    image += '</div>';
                    if ( _defaults.caption ) {
                        image += '<div class="rcg-image-footer">';
                        image += '<p>' + n.caption + '<a href="' + n.url + '" title="' + n.title + '">';
                        image += _defaults.lang.goToWork + ' <span class="icon-wrapper rcg-image-footer__icon" aria-hidden="true">';
                        image += '<svg class="icon" focusable="false"><use href="' + _defaults.themePath + 'resources/icons/outline/photo.svg#icon"></use></svg>';
                        image += '</span></a></p>';
                        image += '</div>';
                    }
                    image += '</div>';
                } );
            } );
            
            return image;
        },
        /**
         * Method which sets a fixed height to the gallery images.
         * 
         * @method fixedHeight
         * @param {Object} $obj An jQuery object of the element which height should be
         * fixed.
         */
        fixedHeight: function( $obj ) {
            if ( _debug ) {
                console.log( '---------- viewer.responsiveColumnGallery.fixedHeight() ----------' );
                console.log( 'viewer.responsiveColumnGallery.fixedHeight: $obj = ', $obj );
            }
            
            $obj.children( 'img' ).css( {
                'height': _defaults.maxHeight
            } );
        },
        /**
         * Method which checks the viewport width and returns true if it´s smaller then
         * 375px.
         * 
         * @method checkForSmallViewport
         * @returns {Boolean} Returns true if it´s smaller then 375px.
         */
        checkForSmallViewport: function() {
            if ( _debug ) {
                console.log( '---------- viewer.responsiveColumnGallery.checkForSmallViewport() ----------' );
            }
            var windowWidth = $( window ).outerWidth();
            
            if ( windowWidth <= 375 ) {
                return true;
            }
            else {
                return false;
            }
        },
        /**
         * Method which prepares the lightbox object with the required data.
         * 
         * @method prepareLightbox
         * @param {Object} $obj An jQuery object which includes the required data
         * attributes.
         * @returns {Object} An Object which includes the required data.
         */
        prepareLightbox: function( $obj ) {
            if ( _debug ) {
                console.log( '---------- viewer.responsiveColumnGallery.prepareLightbox() ----------' );
                console.log( 'viewer.responsiveColumnGallery.prepareLightbox: $obj = ', $obj );
            }
            var lightboxData = {};
            
            lightboxData.src = $obj.attr( 'src' );
            lightboxData.caption = $obj.attr( 'alt' );
            
            return lightboxData;
        },
        /**
         * Method which renders a lightbox for the selected image.
         * 
         * @method renderLightbox
         * @param {Object} data An object which includes the required data.
         * @returns {String} A HTML-String which renders the lightbox.
         */
        renderLightbox: function( data ) {
            if ( _debug ) {
                console.log( '---------- viewer.responsiveColumnGallery.renderLightbox() ----------' );
                console.log( 'viewer.responsiveColumnGallery.renderLightbox: data = ', data );
            }
            var lightbox = '';
            
            lightbox += '<div class="rcg-lightbox-overlay">';
            lightbox += '<div class="rcg-lightbox-body">';
            lightbox += '<div class="rcg-lightbox-close" title="' + _defaults.lang.close + '"><span class="icon-wrapper rcg-lightbox-close__icon" aria-hidden="true">';
            lightbox += '<svg class="icon" focusable="false"><use href="' + _defaults.themePath + 'resources/icons/outline/x.svg#icon"></use></svg>';
            lightbox += '</span></div>';
            lightbox += '<div class="rcg-lightbox-image">';
            lightbox += '<img src="' + data.src + '" alt="' + data.alt + '" />';
            lightbox += '</div>'; // .rcg-lightbox-image
            if ( _defaults.lightbox.caption ) {
                lightbox += '<div class="rcg-lightbox-caption">';
                lightbox += '<p>' + data.caption + '</p>';
                lightbox += '</div>'; // .rcg-lightbox-caption
            }
            lightbox += '</div>'; // .rcg-lightbox-body
            lightbox += '</div>'; // .rcg-lightbox-overlay
            
            return lightbox;
        },
        /**
         * Method which centers the given object to the viewport.
         * 
         * @method centerLightbox
         * @param {Object} $obj An jQuery object of the element to center.
         */
        centerLightbox: function( $obj ) {
            if ( _debug ) {
                console.log( '---------- viewer.responsiveColumnGallery.centerLightbox() ----------' );
                console.log( 'viewer.responsiveColumnGallery.centerLightbox: $obj = ', $obj );
            }
            
            var boxWidth = $obj.outerWidth();
            var boxHeight = $obj.outerHeight();
            
            $obj.css( {
                'margin-top': '-' + boxHeight / 2 + 'px',
                'margin-left': '-' + boxWidth / 2 + 'px'
            } );
        },
    };
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
