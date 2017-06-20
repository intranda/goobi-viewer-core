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
 * Module which drwas a line to the image.
 * 
 * @version 3.2.0
 * @module viewImage.drawLine
 * @requires jQuery
 */
var viewImage = ( function( osViewer ) {
    'use strict';
    
    var _debug = false;
    var _drawing = true;
    var _viewerInputHook = null;
    var _hbAdd = 5;
    var _deleteOldDrawElement = true;
    var _drawElement = null;
    var _startPoint = null;
    var _drawPoint = null;
    
    osViewer.drawLine = {
        init: function() {
            _viewerInputHook = osViewer.viewer.addViewerInputHook( {
                hooks: [ {
                    tracker: "viewer",
                    handler: "clickHandler",
                    hookHandler: _disableViewerEvent
                }, {
                    tracker: "viewer",
                    handler: "scrollHandler",
                    hookHandler: _disableViewerEvent
                }, {
                    tracker: "viewer",
                    handler: "dragHandler",
                    hookHandler: _onViewerDrag
                }, {
                    tracker: "viewer",
                    handler: "pressHandler",
                    hookHandler: _onViewerPress
                }, {
                    tracker: "viewer",
                    handler: "dragEndHandler",
                    hookHandler: _onViewerDragEnd
                } ]
            } );
        },
        toggleDrawing: function() {
            _drawing = !_drawing;
        }
    }

    function _onViewerPress( event ) {
        if ( _drawing ) {
            
            if ( _drawElement && _deleteOldDrawElement ) {
                osViewer.viewer.removeOverlay( _drawElement );
            }
            
            _drawElement = document.createElement( "div" );
            _drawElement.style.border = "2px solid green";
            _drawPoint = osViewer.viewer.viewport.viewerElementToViewportCoordinates( event.position );
            _drawPoint = osViewer.overlays.getRotated( _drawPoint );
            var rect = new OpenSeadragon.Rect( _drawPoint.x, _drawPoint.y, 0, 0 );
            osViewer.viewer.addOverlay( _drawElement, rect, 1 );
            // console.log(osViewer.viewer.viewport
            // .viewerElementToImageCoordinates(event.position));
            event.preventDefaultAction = true;
            return true;
        }
    }
    
    function _onViewerDrag( event ) {
        if ( _drawing ) {
            var newPoint = osViewer.viewer.viewport.viewerElementToViewportCoordinates( event.position );
            newPoint = osViewer.overlays.getRotated( newPoint );
            var rect = new OpenSeadragon.Rect( _drawPoint.x, _drawPoint.y, newPoint.x - _drawPoint.x, newPoint.y - _drawPoint.y );
            if ( newPoint.x < _drawPoint.x ) {
                rect.x = newPoint.x;
                rect.width = _drawPoint.x - newPoint.x;
            }
            if ( newPoint.y < _drawPoint.y ) {
                rect.y = newPoint.y;
                rect.height = _drawPoint.y - newPoint.y;
            }
            osViewer.viewer.updateOverlay( _drawElement, rect, 0 );
            event.preventDefaultAction = true;
            return true;
        }
    }
    
    function _onViewerDragEnd( event ) {
        if ( _drawing ) {
            var newPoint = osViewer.viewer.viewport.viewerElementToViewportCoordinates( event.position );
            newPoint = osViewer.overlays.getRotated( newPoint );
            var rect = new OpenSeadragon.Rect( _drawPoint.x, _drawPoint.y, newPoint.x - _drawPoint.x, newPoint.y - _drawPoint.y );
            if ( newPoint.x < _drawPoint.x ) {
                rect.x = newPoint.x;
                rect.width = _drawPoint.x - newPoint.x;
            }
            if ( newPoint.y < _drawPoint.y ) {
                rect.y = newPoint.y;
                rect.height = _drawPoint.y - newPoint.y;
            }
            rect.hitBox = {
                l: rect.x - _hbAdd,
                t: rect.y - _hbAdd,
                r: rect.x + rect.width + _hbAdd,
                b: rect.y + rect.height + _hbAdd
            };
            // osViewer.overlays.addRect({
            // drawElement : _drawElement,
            // rect : rect
            // });
            // console.log(osViewer.viewer.viewport
            // .viewerElementToImageCoordinates(event.position));
            event.preventDefaultAction = true;
            return true;
        }
    }
    
    function _disableViewerEvent( event ) {
        if ( _drawing ) {
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
    
    return osViewer;
    
} )( viewImage || {}, jQuery );
