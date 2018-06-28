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
 * Module which draws rectangles to the image to highlight areas.
 * 
 * @version 3.2.0
 * @module viewImage.drawRect
 * @requires jQuery
 */
var ImageView = ( function( imageView ) {
    'use strict';
    
    var _debug = false;
    
    var drawingStyleClass = "drawing";
    var _hbAdd = 5;
    var _minDistanceToExistingRect = 0.01;
    
    var _active = false;
    var _drawing = false;
    var _overlayGroup = null;
    var _finishHook = null;
    var _viewerInputHook = null;
    var _deleteOldDrawElement = true;
    var _drawElement = null;
    var _drawPoint = null;
    
    imageView.DrawRect = function(config, image) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'osViewer.drawRect.init' );
                console.log( '##############################' );
            }
            this.config = config;
            this.image = image;
            var draw = this;
            this.viewerInputHook = image.viewer.addViewerInputHook( {
                hooks: [ {
                    tracker: "viewer",
                    handler: "clickHandler",
                    hookHandler: function(event) { _disableViewerEvent(event, draw) }
                // }, {
                // tracker: "viewer",
                // handler: "scrollHandler",
                // hookHandler: _disableViewerEvent
                }, {
                    tracker: "viewer",
                    handler: "dragHandler",
                    hookHandler: function(event) { _onViewerDrag(event, draw) }
                }, {
                    tracker: "viewer",
                    handler: "pressHandler",
                    hookHandler: function(event) { _onViewerPress(event, draw) }
                }, {
                    tracker: "viewer",
                    handler: "dragEndHandler",
                    hookHandler: function(event) { _onViewerDragEnd(event, draw) }
                } ]
            } );
        }
       imageView.DrawRect.prototype.startDrawing = function( overlayGroup, finishHook ) {
            this.active = true;
            this.overlayGroup = overlayGroup;
            this.finishHook = finishHook;
        }
        imageView.DrawRect.prototype.endDrawing = function( removeLastElement ) {
            this.active = false;
            this.overlayGroup = null;
            this.finishHook = null;
            if ( this.drawElement && removeLastElement ) {
                this.image.viewer.removeOverlay( this.drawElement );
            }
            else {
                $( this.drawElement ).removeClass( drawingStyleClass );
            }
        }
        imageView.DrawRect.prototype.isActive = function() {
            return this.active;
        }
        imageView.DrawRect.prototype.isDrawing = function() {
            return this.drawing;
        }
        imageView.DrawRect.prototype.removeLastDrawnElement = function() {
            if ( this.drawElement ) {
                this.image.viewer.removeOverlay( this.drawElement );
            }
        }

    function _onViewerPress( event, draw) {
        if ( draw.active ) {
            draw.drawPoint = draw.image.viewer.viewport.viewerElementToViewportCoordinates( event.position );
            
            event.preventDefaultAction = false;
            return true;
        }
    }
    
    function _onViewerDrag( event, draw ) {
        // if(_debug) {
        // console.log("Dragging: ");
        // console.log("_active = " + _active);
        // console.log("_drawing = " + _drawing);
        // console.log("_drawPoint = " + _drawPoint);
        // }
        if ( draw.drawing ) {
            var newPoint = draw.image.viewer.viewport.viewerElementToViewportCoordinates( event.position );
            var rect = new OpenSeadragon.Rect( draw.drawPoint.x, draw.drawPoint.y, newPoint.x - draw.drawPoint.x, newPoint.y - draw.drawPoint.y );
            if ( newPoint.x < draw.drawPoint.x ) {
                rect.x = newPoint.x;
                rect.width = draw.drawPoint.x - newPoint.x;
            }
            if ( newPoint.y < draw.drawPoint.y ) {
                rect.y = newPoint.y;
                rect.height = draw.drawPoint.y - newPoint.y;
            }
            draw.image.viewer.updateOverlay( draw.drawElement, rect, 0 );
            event.preventDefaultAction = true;
            return true;
            
        }
        else if ( draw.active && draw.drawPoint ) {
            var activeOverlay = draw.image.overlays.getDrawingOverlay();
            if ( activeOverlay && draw.image.transformRect && draw.image.transformRect.isActive()
                    && draw.image.overlays.contains( activeOverlay.rect, draw.drawPoint, _minDistanceToExistingRect ) ) {
                draw.drawPoint = null;
                if ( _debug )
                    console.log( "Action overlaps active overlay" );
            }
            else {
                draw.drawing = true;
                if ( activeOverlay && _deleteOldDrawElement ) {
                    draw.image.overlays.removeOverlay( activeOverlay );
                }
                
                draw.drawElement = document.createElement( "div" );
                if ( draw.overlayGroup ) {
                    $( draw.drawElement ).addClass( draw.overlayGroup.styleClass );
                }
                $( draw.drawElement ).addClass( drawingStyleClass );
                var rect = new OpenSeadragon.Rect( draw.drawPoint.x, draw.drawPoint.y, 0, 0 );
                draw.image.viewer.addOverlay( draw.drawElement, rect, 1 );
            }
            event.preventDefaultAction = true;
            return true;
        }
    }
    
    function _onViewerDragEnd( event, draw ) {
        if ( draw.drawing ) {
            draw.drawing = false;
            var newPoint = draw.image.viewer.viewport.viewerElementToViewportCoordinates( event.position );
            var rect = new OpenSeadragon.Rect( draw.drawPoint.x, draw.drawPoint.y, newPoint.x - draw.drawPoint.x, newPoint.y - draw.drawPoint.y );
            if ( newPoint.x < draw.drawPoint.x ) {
                rect.x = newPoint.x;
                rect.width = draw.drawPoint.x - newPoint.x;
            }
            if ( newPoint.y < draw.drawPoint.y ) {
                rect.y = newPoint.y;
                rect.height = draw.drawPoint.y - newPoint.y;
            }
            rect.hitBox = {
                l: rect.x - _hbAdd,
                t: rect.y - _hbAdd,
                r: rect.x + rect.width + _hbAdd,
                b: rect.y + rect.height + _hbAdd
            };
            
            var overlay = {
                type: imageView.Overlays.OverlayTypes.RECTANGLE,
                element: draw.drawElement,
                rect: rect,
                group: draw.overlayGroup.name,
            };
            draw.image.overlays.setDrawingOverlay( overlay );
            if ( draw.finishHook ) {
                draw.finishHook( overlay );
            }
            
            event.preventDefaultAction = true;
            return true;
        }
        
    }
    
    function _disableViewerEvent( event ) {
        if ( _active ) {
            event.preventDefaultAction = true;
            return true;
        }
    }
    
    function checkForRectHit( point ) {
        var i;
        for ( i = 0; i < _rects.length; i++ ) {
            var x = _rects[ i ];
            if ( point.x > x.hitBox.l && point.x < x.hitBox.r && point.y > x.hitBox.t && point.y < x.hitBox.b ) {
                var topLeftHb = {
                    l: x.x - _hbAdd,
                    t: x.y - _hbAdd,
                    r: x.x + _hbAdd,
                    b: x.y + _hbAdd
                };
                var topRightHb = {
                    l: x.x + x.width - _hbAdd,
                    t: x.y - _hbAdd,
                    r: x.x + x.width + _hbAdd,
                    b: x.y + _hbAdd
                };
                var bottomRightHb = {
                    l: x.x + x.width - _hbAdd,
                    t: x.y + x.height - _hbAdd,
                    r: x.x + x.width + _hbAdd,
                    b: x.y + x.height + _hbAdd
                };
                var bottomLeftHb = {
                    l: x.x - _hbAdd,
                    t: x.y + x.height - _hbAdd,
                    r: x.x + _hbAdd,
                    b: x.y + x.height + _hbAdd
                };
                var topHb = {
                    l: x.x + _hbAdd,
                    t: x.y - _hbAdd,
                    r: x.x + x.width - _hbAdd,
                    b: x.y + _hbAdd
                };
                var rightHb = {
                    l: x.x + x.width - _hbAdd,
                    t: x.y + _hbAdd,
                    r: x.x + x.width + _hbAdd,
                    b: x.y + x.height - _hbAdd
                };
                var bottomHb = {
                    l: x.x + _hbAdd,
                    t: x.y + x.height - _hbAdd,
                    r: x.x + x.width - _hbAdd,
                    b: x.y + x.height + _hbAdd
                };
                var leftHb = {
                    l: x.x - _hbAdd,
                    t: x.y + _hbAdd,
                    r: x.x + _hbAdd,
                    b: x.y + x.height - _hbAdd
                };
            }
        }
    }
    
    return imageView;
    
} )( ImageView );
