(function(){function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s}return e})()({1:[function(require,module,exports){
'use strict';

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
 * Module which handles the image controls of the image view.
 * 
 * @version 3.2.0
 * @module viewImage.controls
 * @requires jQuery
 */
var viewImage = function (osViewer) {
    'use strict';

    var _debug = false;
    var _currentZoom;
    var _zoomedOut = true;
    var _panning = false;
    var _fadeout = null;

    osViewer.controls = {
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('osViewer.controls.init');
                console.log('##############################');
            }

            if (osViewer.controls.persistence) {
                osViewer.controls.persistence.init(config);
            }
            if (_debug) {
                console.log("Setting viewer location to", config.image.location);
            }

            if (osViewer.observables) {
                // set location after viewport update
                osViewer.observables.redrawRequired.sample(osViewer.observables.viewportUpdate).filter(function (event) {
                    return osViewer.controls ? true : false;
                }).subscribe(function (event) {
                    setLocation(event, osViewer);
                    osViewer.controls.setPanning(false);
                });

                // zoom home if min zoom reached
                osViewer.observables.viewerZoom.subscribe(function (event) {
                    if (_debug) {
                        console.log("zoom to " + osViewer.viewer.viewport.getZoom(true));
                    }
                    if (!osViewer.controls.isPanning()) {
                        var currentZoom = osViewer.viewer.viewport.getZoom();
                        if (currentZoom <= osViewer.viewer.viewport.minZoomLevel) {
                            if (_debug) {
                                console.log("Zoomed out: Panning home");
                            }

                            osViewer.controls.setPanning(true);
                            osViewer.controls.goHome(true);
                            osViewer.controls.setPanning(false);
                        }
                    }
                });
            }

            // fade out fullscreen controls
            if ($('#fullscreenTemplate').length > 0) {
                $('#fullscreenTemplate').on('mousemove', function () {
                    osViewer.controls.fullscreenControlsFadeout();
                });
            }
        },
        getLocation: function getLocation() {
            return {
                x: osViewer.controls.getCenter().x,
                y: osViewer.controls.getCenter().y,
                zoom: osViewer.controls.getZoom() / osViewer.controls.getCurrentRotationZooming(),
                rotation: osViewer.controls.getRotation()
            };
        },
        getCenter: function getCenter() {
            if (_debug) {
                console.log("image center is " + osViewer.viewer.viewport.getCenter(true));
            }
            return osViewer.viewer.viewport.getCenter(true);
        },
        setCenter: function setCenter(center) {

            if (_debug) {
                console.log("Setting image center to ");
                console.log(center);
            }

            osViewer.viewer.viewport.panTo(center, true);
        },
        getZoom: function getZoom() {
            if (_debug) {
                console.log('osViewer.controls.getZoom');
            }
            return osViewer.viewer.viewport.getZoom(true);
        },
        zoomTo: function zoomTo(_zoomTo) {
            if (_debug) {
                console.log('osViewer.controls.myZoomTo: zoomTo - ' + _zoomTo);
            }

            var zoomBy = parseFloat(_zoomTo) / osViewer.viewer.viewport.getZoom();

            if (_debug) {
                console.log('osViewer.controls.myZoomTo: zoomBy - ' + zoomBy);
            }

            osViewer.viewer.viewport.zoomBy(zoomBy, osViewer.viewer.viewport.getCenter(false), true);
        },
        setFullScreen: function setFullScreen(enable) {
            if (_debug) {
                console.log('osViewer.controls.setFullScreen: enable - ' + enable);
            }

            osViewer.viewer.setFullScreen(enable);
        },
        goHome: function goHome(immediate) {
            if (_debug) {
                console.log('osViewer.controls.panHome - zoom : ' + osViewer.viewer.viewport.getHomeZoom());
            }
            osViewer.viewer.viewport.goHome(immediate);
            _zoomedOut = true;
        },
        reset: function reset(resetRotation) {
            if (_debug) {
                console.log('osViewer.controls.goHome: bool - ' + resetRotation);
            }

            // osViewer.viewer.viewport.goHome( true );
            osViewer.controls.goHome(true);
            osViewer.viewer.viewport.zoomTo(osViewer.viewer.viewport.getHomeZoom(), null, true);
            if (resetRotation) {
                osViewer.controls.rotateTo(0);
            }
        },
        zoomIn: function zoomIn() {
            if (_debug) {
                console.log('osViewer.controls.zoomIn: zoomSpeed - ' + osViewer.getConfig().global.zoomSpeed);
            }

            osViewer.viewer.viewport.zoomBy(osViewer.getConfig().global.zoomSpeed, osViewer.viewer.viewport.getCenter(false), false);
        },
        zoomOut: function zoomOut() {
            if (_debug) {
                console.log('osViewer.controls.zoomOut: zoomSpeed - ' + osViewer.getConfig().global.zoomSpeed);
            }

            osViewer.viewer.viewport.zoomBy(1 / osViewer.getConfig().global.zoomSpeed, osViewer.viewer.viewport.getCenter(false), false);
        },
        getHomeZoom: function getHomeZoom(rotated) {
            if (rotated && osViewer.getCanvasSize().x / osViewer.getCanvasSize().y <= osViewer.getImageSize().x / osViewer.getImageSize().y) {
                osViewer.viewer.viewport.homeFillsViewer = true;
            }
            var zoom = osViewer.viewer.viewport.getHomeZoom();
            osViewer.viewer.viewport.homeFillsViewer = false;
            return zoom;
        },
        rotateRight: function rotateRight() {
            if (_debug) {
                console.log('osViewer.controls.rotateRight');
            }

            var newRotation = osViewer.viewer.viewport.getRotation() + 90;
            osViewer.controls.rotateTo(newRotation);
        },
        rotateLeft: function rotateLeft() {
            if (_debug) {
                console.log('osViewer.controls.rotateLeft');
            }

            var newRotation = osViewer.viewer.viewport.getRotation() - 90;
            osViewer.controls.rotateTo(newRotation);
        },
        getRotation: function getRotation() {
            if (_debug) {
                console.log('osViewer.controls.getRotation');
            }

            return osViewer.viewer.viewport.getRotation();
        },
        setRotation: function setRotation(rotation) {
            if (_debug) {
                console.log('osViewer.controls.setRotation: rotation - ' + rotation);
            }

            return osViewer.controls.rotateTo(rotation);
        },
        rotateTo: function rotateTo(newRotation) {
            if (newRotation < 0) {
                newRotation = newRotation + 360;
            }
            newRotation = newRotation % 360;
            if (_debug) {
                console.log('osViewer.controls.rotateTo: newRotation - ' + newRotation);
            }

            _panning = true;
            _currentZoom = null;
            osViewer.viewer.viewport.setRotation(newRotation);
            _panning = false;
        },
        getCurrentRotationZooming: function getCurrentRotationZooming() {
            var sizes = osViewer.getSizes();
            if (sizes && sizes.rotated()) {
                return 1 / sizes.ratio(sizes.originalImageSize);
            } else {
                return 1;
            }
        },
        setPanning: function setPanning(panning) {
            _panning = panning;
        },
        isPanning: function isPanning() {
            return _panning;
        },
        fullscreenControlsFadeout: function fullscreenControlsFadeout() {
            if (_debug) {
                console.log('---------- osViewer.controls.fullscreenControlsFadeout() ----------');
            }

            if (_fadeout) {
                clearTimeout(_fadeout);
                this.showFullscreenControls();
            }

            _fadeout = setTimeout(this.hideFullscreenControls, 3000);
        },
        hideFullscreenControls: function hideFullscreenControls() {
            if (_debug) {
                console.log('---------- osViewer.controls.hideFullscreenControls() ----------');
            }

            $('#fullscreenRotateControlsWrapper, #fullscreenZoomSliderWrapper, #fullscreenExitWrapper, #fullscreenPrevWrapper, #fullscreenNextWrapper').stop().fadeOut('slow');
        },
        showFullscreenControls: function showFullscreenControls() {
            if (_debug) {
                console.log('---------- osViewer.controls.showFullscreenControls() ----------');
            }

            $('#fullscreenRotateControlsWrapper, #fullscreenZoomSliderWrapper, #fullscreenExitWrapper, #fullscreenPrevWrapper, #fullscreenNextWrapper').show();
        }
    };

    // set correct location, zooming and rotation once viewport has been updated after
    // redraw
    function setLocation(event, osViewer) {
        if (_debug) {
            console.log("Viewer changed from " + event.osState + " event");
            console.log("target location: ", event.targetLocation);
            console.log("Home zoom = ", osViewer.viewer.viewport.getHomeZoom());
        }
        osViewer.viewer.viewport.minZoomLevel = osViewer.viewer.viewport.getHomeZoom() * osViewer.getConfig().global.minZoomLevel;
        var targetZoom = event.targetLocation.zoom;
        var targetLocation = new OpenSeadragon.Point(event.targetLocation.x, event.targetLocation.y);
        var zoomDiff = targetZoom * osViewer.viewer.viewport.getHomeZoom() - osViewer.viewer.viewport.minZoomLevel;
        // console.log("zoomDiff: " + targetZoom + " * " + osViewer.viewer.viewport.getHomeZoom()
        // + " - " + osViewer.viewer.viewport.minZoomLevel + " = ", zoomDiff);
        // console.log("zoomDiff: " + targetZoom + " - " + osViewer.viewer.viewport.minZoomLevel +
        // "/" + osViewer.controls.getCurrentRotationZooming() + " = ", zoomDiff);
        var zoomedOut = zoomDiff < 0.001 || !targetZoom;
        if (zoomedOut) {
            if (_debug) {
                console.log("Zooming home");
            }
            osViewer.controls.goHome(true);
        } else {
            if (_debug) {
                console.log("Zooming to " + targetZoom + " * " + osViewer.controls.getCurrentRotationZooming());
                console.log("panning to ", targetLocation);
            }
            osViewer.viewer.viewport.zoomTo(targetZoom * osViewer.controls.getCurrentRotationZooming(), null, true);
            osViewer.controls.setCenter(targetLocation);
        }
        if (event.osState === "open" && event.targetLocation.rotation !== 0) {
            osViewer.controls.rotateTo(event.targetLocation.rotation);
        }
    }

    return osViewer;
}(viewImage || {}, jQuery);

},{}],2:[function(require,module,exports){
'use strict';

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
 * Module which handles the persistence of zoom and rotation levels.
 * 
 * @version 3.2.0
 * @module viewImage.controls.persistence
 * @requires jQuery
 */
var viewImage = function (osViewer) {
    'use strict';

    var _debug = false;

    osViewer.controls.persistence = {

        init: function init(config) {
            if (typeof Storage !== 'undefined') {

                /**
                 * Set Location from local storage
                 */
                var location = null;
                var currentPersistenceId = osViewer.getConfig().global.persistenceId;
                if (config.global.persistZoom || config.global.persistRotation) {
                    try {
                        var location = JSON.parse(localStorage.imageLocation);
                    } catch (err) {
                        if (_debug) {
                            console.error("No readable image location in local storage");
                        }
                    }
                    if (location && _isValid(location) && location.persistenceId === currentPersistenceId) {
                        if (_debug) {
                            console.log("Reading location from local storage", location);
                        }
                        config.image.location = {};
                        if (config.global.persistZoom) {
                            if (_debug) {
                                console.log("setting zoom from local storage");
                            }
                            config.image.location.zoom = location.zoom;
                            config.image.location.x = location.x;
                            config.image.location.y = location.y;
                        }
                        if (config.global.persistRotation) {
                            if (_debug) {
                                console.log("setting rotation from local storage");
                            }
                            config.image.location.rotation = location.rotation;
                        } else {
                            config.image.location.rotation = 0;
                        }
                    }

                    /**
                     * save current location to local storage before navigating away
                     */
                    window.onbeforeunload = function () {
                        var loc = osViewer.controls.getLocation();
                        loc.persistenceId = osViewer.getConfig().global.persistenceId;
                        localStorage.imageLocation = JSON.stringify(loc);
                        if (_debug) {
                            console.log("storing zoom " + localStorage.imageLocation);
                        }
                    };
                }
            }
        }
    };

    function _isValid(location) {
        return _isNumber(location.x) && _isNumber(location.y) && _isNumber(location.zoom) && _isNumber(location.rotation);
    }

    function _isNumber(x) {
        return typeof x === "number" && !Number.isNaN(x);
    }

    return osViewer;
}(viewImage || {}, jQuery);

},{}],3:[function(require,module,exports){
"use strict";

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
var viewImage = function (osViewer) {
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
        init: function init() {
            _viewerInputHook = osViewer.viewer.addViewerInputHook({
                hooks: [{
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
                }]
            });
        },
        toggleDrawing: function toggleDrawing() {
            _drawing = !_drawing;
        }
    };

    function _onViewerPress(event) {
        if (_drawing) {

            if (_drawElement && _deleteOldDrawElement) {
                osViewer.viewer.removeOverlay(_drawElement);
            }

            _drawElement = document.createElement("div");
            _drawElement.style.border = "2px solid green";
            _drawPoint = osViewer.viewer.viewport.viewerElementToViewportCoordinates(event.position);
            _drawPoint = osViewer.overlays.getRotated(_drawPoint);
            var rect = new OpenSeadragon.Rect(_drawPoint.x, _drawPoint.y, 0, 0);
            osViewer.viewer.addOverlay(_drawElement, rect, 1);
            // console.log(osViewer.viewer.viewport
            // .viewerElementToImageCoordinates(event.position));
            event.preventDefaultAction = true;
            return true;
        }
    }

    function _onViewerDrag(event) {
        if (_drawing) {
            var newPoint = osViewer.viewer.viewport.viewerElementToViewportCoordinates(event.position);
            newPoint = osViewer.overlays.getRotated(newPoint);
            var rect = new OpenSeadragon.Rect(_drawPoint.x, _drawPoint.y, newPoint.x - _drawPoint.x, newPoint.y - _drawPoint.y);
            if (newPoint.x < _drawPoint.x) {
                rect.x = newPoint.x;
                rect.width = _drawPoint.x - newPoint.x;
            }
            if (newPoint.y < _drawPoint.y) {
                rect.y = newPoint.y;
                rect.height = _drawPoint.y - newPoint.y;
            }
            osViewer.viewer.updateOverlay(_drawElement, rect, 0);
            event.preventDefaultAction = true;
            return true;
        }
    }

    function _onViewerDragEnd(event) {
        if (_drawing) {
            var newPoint = osViewer.viewer.viewport.viewerElementToViewportCoordinates(event.position);
            newPoint = osViewer.overlays.getRotated(newPoint);
            var rect = new OpenSeadragon.Rect(_drawPoint.x, _drawPoint.y, newPoint.x - _drawPoint.x, newPoint.y - _drawPoint.y);
            if (newPoint.x < _drawPoint.x) {
                rect.x = newPoint.x;
                rect.width = _drawPoint.x - newPoint.x;
            }
            if (newPoint.y < _drawPoint.y) {
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

    function _disableViewerEvent(event) {
        if (_drawing) {
            event.preventDefaultAction = true;
            return true;
        }
    }
    function checkForRectHit(point) {
        var i;
        for (i = 0; i < _rects.length; i++) {
            var x = _rects[i];
            if (point.x > x.hitBox.l && point.x < x.hitBox.r && point.y > x.hitBox.t && point.y < x.hitBox.b) {
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
}(viewImage || {}, jQuery);

},{}],4:[function(require,module,exports){
'use strict';

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
var viewImage = function (osViewer) {
    'use strict';

    var _debug = false;

    var drawingStyleClass = "drawing";

    var _active = false;
    var _drawing = false;
    var _overlayGroup = null;
    var _finishHook = null;
    var _viewerInputHook = null;
    var _hbAdd = 5;
    var _minDistanceToExistingRect = 0.01;
    var _deleteOldDrawElement = true;
    var _drawElement = null;
    var _drawPoint = null;

    osViewer.drawRect = {
        init: function init() {
            if (_debug) {
                console.log('##############################');
                console.log('osViewer.drawRect.init');
                console.log('##############################');
            }
            _viewerInputHook = osViewer.viewer.addViewerInputHook({
                hooks: [{
                    tracker: "viewer",
                    handler: "clickHandler",
                    hookHandler: _disableViewerEvent
                    // }, {
                    // tracker: "viewer",
                    // handler: "scrollHandler",
                    // hookHandler: _disableViewerEvent
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
                }]
            });
        },
        startDrawing: function startDrawing(overlayGroup, finishHook) {
            _active = true;
            _overlayGroup = overlayGroup;
            _finishHook = finishHook;
        },
        endDrawing: function endDrawing(removeLastElement) {
            _active = false;
            _overlayGroup = null;
            _finishHook = null;
            if (_drawElement && removeLastElement) {
                osViewer.viewer.removeOverlay(_drawElement);
            } else {
                $(_drawElement).removeClass(drawingStyleClass);
            }
        },
        isActive: function isActive() {
            return _active;
        },
        isDrawing: function isDrawing() {
            return _drawing;
        },
        removeLastDrawnElement: function removeLastDrawnElement() {
            if (_drawElement) {
                osViewer.viewer.removeOverlay(_drawElement);
            }
        }
    };

    function _onViewerPress(event) {
        if (_active) {
            _drawPoint = osViewer.viewer.viewport.viewerElementToViewportCoordinates(event.position);

            event.preventDefaultAction = false;
            return true;
        }
    }

    function _onViewerDrag(event) {
        // if(_debug) {
        // console.log("Dragging: ");
        // console.log("_active = " + _active);
        // console.log("_drawing = " + _drawing);
        // console.log("_drawPoint = " + _drawPoint);
        // }
        if (_drawing) {
            var newPoint = osViewer.viewer.viewport.viewerElementToViewportCoordinates(event.position);
            var rect = new OpenSeadragon.Rect(_drawPoint.x, _drawPoint.y, newPoint.x - _drawPoint.x, newPoint.y - _drawPoint.y);
            if (newPoint.x < _drawPoint.x) {
                rect.x = newPoint.x;
                rect.width = _drawPoint.x - newPoint.x;
            }
            if (newPoint.y < _drawPoint.y) {
                rect.y = newPoint.y;
                rect.height = _drawPoint.y - newPoint.y;
            }
            osViewer.viewer.updateOverlay(_drawElement, rect, 0);
            event.preventDefaultAction = true;
            return true;
        } else if (_active && _drawPoint) {
            var activeOverlay = osViewer.overlays.getDrawingOverlay();
            if (activeOverlay && osViewer.transformRect && osViewer.transformRect.isActive() && osViewer.overlays.contains(activeOverlay.rect, _drawPoint, _minDistanceToExistingRect)) {
                _drawPoint = null;
                if (_debug) console.log("Action overlaps active overlay");
            } else {
                _drawing = true;
                if (activeOverlay && _deleteOldDrawElement) {
                    osViewer.overlays.removeOverlay(activeOverlay);
                }

                _drawElement = document.createElement("div");
                if (_overlayGroup) {
                    $(_drawElement).addClass(_overlayGroup.styleClass);
                }
                $(_drawElement).addClass(drawingStyleClass);
                var rect = new OpenSeadragon.Rect(_drawPoint.x, _drawPoint.y, 0, 0);
                osViewer.viewer.addOverlay(_drawElement, rect, 1);
            }
            event.preventDefaultAction = true;
            return true;
        }
    }

    function _onViewerDragEnd(event) {
        if (_drawing) {
            _drawing = false;
            var newPoint = osViewer.viewer.viewport.viewerElementToViewportCoordinates(event.position);
            var rect = new OpenSeadragon.Rect(_drawPoint.x, _drawPoint.y, newPoint.x - _drawPoint.x, newPoint.y - _drawPoint.y);
            if (newPoint.x < _drawPoint.x) {
                rect.x = newPoint.x;
                rect.width = _drawPoint.x - newPoint.x;
            }
            if (newPoint.y < _drawPoint.y) {
                rect.y = newPoint.y;
                rect.height = _drawPoint.y - newPoint.y;
            }
            rect.hitBox = {
                l: rect.x - _hbAdd,
                t: rect.y - _hbAdd,
                r: rect.x + rect.width + _hbAdd,
                b: rect.y + rect.height + _hbAdd
            };

            var overlay = {
                type: osViewer.overlays.overlayTypes.RECTANGLE,
                element: _drawElement,
                rect: rect,
                group: _overlayGroup.name
            };
            osViewer.overlays.setDrawingOverlay(overlay);
            if (_finishHook) {
                _finishHook(overlay);
            }

            event.preventDefaultAction = true;
            return true;
        }
    }

    function _disableViewerEvent(event) {
        if (_active) {
            event.preventDefaultAction = true;
            return true;
        }
    }

    function checkForRectHit(point) {
        var i;
        for (i = 0; i < _rects.length; i++) {
            var x = _rects[i];
            if (point.x > x.hitBox.l && point.x < x.hitBox.r && point.y > x.hitBox.t && point.y < x.hitBox.b) {
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
}(viewImage || {}, jQuery);

},{}],5:[function(require,module,exports){
"use strict";

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
 * Module which initializes the viewerJS module.
 * 
 * @version 3.2.0
 * @module viewImage
 * @requires jQuery
 */
var viewImage = function () {
    'use strict';

    var osViewer = {};
    var _debug = false;
    var _footerImage = null;
    var _canvasScale;
    var _container;
    var _defaults = {
        global: {
            divId: "map",
            zoomSlider: ".zoom-slider",
            zoomSliderHandle: '.zoom-slider-handle',
            overlayGroups: [{
                name: "searchHighlighting",
                styleClass: "coords-highlighting",
                interactive: false
            }, {
                name: "ugc",
                styleClass: "ugcBox",
                interactive: true

            }],
            zoomSpeed: 1.25,
            maxZoomLevel: 20,
            minZoomLevel: 1,
            useTiles: true,
            imageControlsActive: true,
            visibilityRatio: 0.4,
            loadImageTimeout: 10 * 60 * 1000,
            maxParallelImageLoads: 4,
            adaptContainerHeight: false,
            footerHeight: 50,
            rememberZoom: false,
            rememberRotation: false
        },
        image: {},
        getOverlayGroup: function getOverlayGroup(name) {
            var allGroups = _defaults.global.overlayGroups;
            for (var int = 0; int < allGroups.length; int++) {
                var group = allGroups[int];
                if (group.name === name) {
                    return group;
                }
            }
        },
        getCoordinates: function getCoordinates(name) {
            var coodinatesArray = _defaults.image.highlightCoords;
            if (coodinatesArray) {
                for (var int = 0; int < coodinatesArray.length; int++) {
                    var coords = coodinatesArray[int];
                    if (coords.name === name) {
                        return coords;
                    }
                }
            }
        }
    };

    osViewer = {
        viewer: null,
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('osViewer.init');
                console.log('##############################');
            }

            // constructor
            $.extend(true, _defaults, config);
            // convert mimeType "image/jpeg" to "image/jpg" to provide correct
            // iiif calls
            _defaults.image.mimeType = _defaults.image.mimeType.replace("jpeg", "jpg");
            _container = $("#" + _defaults.global.divId);

            var sources = _defaults.image.tileSource;
            if (typeof sources === 'string' && sources.startsWith("[")) {
                sources = JSON.parse(sources);
            } else if (!$.isArray(sources)) {
                sources = [sources];
            }
            var promises = [];
            for (var i = 0; i < sources.length; i++) {
                var source = sources[i];
                // returns the OpenSeadragon.TileSource if it can be created,
                // otherweise
                // rejects the promise
                var promise = viewImage.createTileSource(source);
                promises.push(promise);
            }
            return Q.all(promises).then(function (tileSources) {
                var minWidth = Number.MAX_VALUE;
                var minHeight = Number.MAX_VALUE;
                var minAspectRatio = Number.MAX_VALUE;
                for (var j = 0; j < tileSources.length; j++) {
                    var tileSource = tileSources[j];
                    minWidth = Math.min(minWidth, tileSource.width);
                    minHeight = Math.min(minHeight, tileSource.height);
                    minAspectRatio = Math.min(minAspectRatio, tileSource.aspectRatio);
                }
                if (_debug) {
                    console.log("Min aspect ratio = " + minAspectRatio);
                }
                var x = 0;
                for (var i = 0; i < tileSources.length; i++) {
                    var tileSource = tileSources[i];
                    tileSources[i] = {
                        tileSource: tileSource,
                        width: tileSource.aspectRatio / minAspectRatio,
                        // height: minHeight/tileSource.height,
                        x: x,
                        y: 0
                    };
                    x += tileSources[i].width;
                }
                return viewImage.loadImage(tileSources);
            });
        },
        loadImage: function loadImage(tileSources) {
            if (_debug) {
                console.log('Loading image with tilesource: ', tileSources);
            }

            osViewer.loadFooter();

            osViewer.viewer = new OpenSeadragon({
                immediateRender: false,
                visibilityRatio: _defaults.global.visibilityRatio,
                sequenceMode: false,
                id: _defaults.global.divId,
                controlsEnabled: false,
                prefixUrl: "/openseadragon-bin/images/",
                zoomPerClick: 1,
                maxZoomLevel: _defaults.global.maxZoomLevel,
                minZoomLevel: _defaults.global.minZoomLevel,
                zoomPerScroll: _defaults.global.zoomSpeed,
                mouseNavEnabled: _defaults.global.zoomSpeed > 1,
                showNavigationControl: false,
                showZoomControl: false,
                showHomeControl: false,
                showFullPageControl: true,
                timeout: _defaults.global.loadImageTimeout,
                tileSources: tileSources,
                blendTime: .5,
                alwaysBlend: false,
                imageLoaderLimit: _defaults.global.maxParallelImageLoads,
                viewportMargins: {
                    top: 0,
                    left: 0,
                    right: 0,
                    bottom: _defaults.global.footerHeight
                }
            });

            var result = Q.defer();

            osViewer.observables = createObservables(window, osViewer.viewer);

            osViewer.observables.viewerOpen.subscribe(function (openevent, loadevent) {
                result.resolve(osViewer);
            }, function (error) {
                result.reject(error);
            });

            // Calculate sizes if redraw is required

            osViewer.observables.redrawRequired.subscribe(function (event) {
                if (_debug) {
                    console.log("viewer " + event.osState + "ed with target location ", event.targetLocation);
                }

                osViewer.redraw();
            });

            if (osViewer.controls) {
                osViewer.controls.init(_defaults);
            }

            if (osViewer.zoomSlider) {
                osViewer.zoomSlider.init(_defaults);
            }

            if (osViewer.overlays) {
                osViewer.overlays.init(_defaults);
            }

            if (osViewer.drawRect) {
                osViewer.drawRect.init();
            }

            if (osViewer.transformRect) {
                osViewer.transformRect.init();
            }

            osViewer.observables.redrawRequired.connect();

            return result.promise;
        },
        getObservables: function getObservables() {
            console.log("Observables = ", osViewer.observables);
            return osViewer.observables;
        },
        hasFooter: function hasFooter() {
            return _footerImage != null;
        },
        getConfig: function getConfig() {
            return _defaults;
        },
        loadFooter: function loadFooter() {
            if (_defaults.image.baseFooterUrl && _defaults.global.footerHeight > 0) {
                _footerImage = new Image();
                _footerImage.src = _defaults.image.baseFooterUrl.replace("{width}", Math.round(_container.width())).replace("{height}", Math.round(_defaults.global.footerHeight));
                _footerImage.onload = function () {
                    if (_debug) {
                        console.log("loading footer image ", _footerImage);
                        console.log("Calculating image Footer size");
                    }

                    osViewer.drawFooter();
                };
            }
        },
        drawFooter: function drawFooter() {
            if (osViewer.viewer) {
                _overlayFooter();
            }

            osViewer.viewer.removeHandler('update-viewport', _overlayFooter);
            osViewer.viewer.addHandler('update-viewport', _overlayFooter);
        },
        getOverlayGroup: function getOverlayGroup(name) {
            return _defaults.getOverlayGroup(name);
        },
        getHighlightCoordinates: function getHighlightCoordinates(name) {
            return _defaults.getCoordinates(name);
        },
        createPyramid: function createPyramid(imageInfo) {
            var fileExtension = _defaults.image.mimeType;
            fileExtension = fileExtension.replace("image/", "");
            fileExtension = fileExtension.replace("jpeg", "jpg").replace("tiff", "tif");
            var imageLevels = [];
            var tileSource;
            if (Array.isArray(imageInfo)) {
                imageInfo.forEach(function (level) {
                    level.mimetype = _defaults.image.mimeType;
                });
                tileSource = new OpenSeadragon.LegacyTileSource(imageInfo);
            } else if (imageInfo.sizes) {
                imageInfo.sizes.forEach(function (size) {
                    if (_debug) {
                        console.log("Image level width = ", size.width);
                        console.log("Image level height = ", size.height);
                    }

                    var level = {
                        mimetype: _defaults.image.mimeType,
                        url: imageInfo["@id"].replace("/info.json", "") + "/full/" + size.width + ",/0/default." + fileExtension,
                        width: imageInfo.width,
                        height: imageInfo.height
                    };

                    if (_debug) {
                        console.log("Created level ", level);
                    }

                    imageLevels.push(level);
                });

                tileSource = new OpenSeadragon.LegacyTileSource(imageLevels);
            } else {
                tileSource = new OpenSeadragon.ImageTileSource({
                    url: imageInfo["@id"].replace("/info.json", "") + "/full/full/0/default." + fileExtension,
                    crossOriginPolicy: "Anonymous",
                    buildPyramid: false
                });
            }

            return tileSource;
        },
        getSizes: function getSizes() {
            return osViewer.sizes;
        },
        addImage: function addImage(url, width, height) {
            if (_debug) {
                console.log('osViewer.addImage: url - ' + url);
                console.log('osViewer.addImage: width - ' + width);
                console.log('osViewer.addImage: height - ' + height);
            }
            if (osViewer.viewer) {
                osViewer.viewer.addTiledImage({
                    tileSource: {
                        type: "legacy-image-pyramid",
                        levels: [{
                            url: url,
                            height: height,
                            width: width
                        }]
                    },
                    x: 0,
                    y: 1.6,
                    width: 1
                });
            } else {
                if (_debug) {
                    console.log("Viewer not initialized yet; cannot add image");
                }
            }
        },
        getImageInfo: function getImageInfo() {
            if (osViewer.viewer) {
                return osViewer.viewer.tileSources;
            }
            return null;
        },
        getScaleToOriginalSize: function getScaleToOriginalSize(imageNo) {
            return 1.0;
            // if(!imageNo) {
            // imageNo = 0;
            // }
            // var displaySize = osViewer.viewer.viewport._contentSize.x;
            // return osViewer.getImageInfo()[imageNo].tileSource.width / displaySize;
        },
        scaleToOriginalSize: function scaleToOriginalSize(value, imageNo) {
            return value;
            // if ( _debug ) {
            // console.log( 'Overlays _scaleToOriginalSize: value - ' + value );
            // }
            //            
            // if(!imageNo) {
            // imageNo = 0;
            // }
            //            
            // var displaySize = osViewer.viewer.viewport._contentSize.x;
            // return value / displaySize * osViewer.getImageInfo()[imageNo].tileSource.width;
        },
        scaleToImageSize: function scaleToImageSize(value, imageNo) {
            return value;
            // if ( _debug ) {
            // console.log( 'Overlays _scaleToImageSize: value - ' + value );
            // }
            //            
            // if(!imageNo) {
            // imageNo = 0;
            // }
            //            
            // var displaySize = osViewer.viewer.viewport._contentSize.x;
            // return value * displaySize / osViewer.getImageInfo()[imageNo].tileSource.width;
        },
        close: function close() {
            if (_debug) {
                console.log("Closing openSeadragon viewer");
            }

            if (osViewer.viewer) {
                osViewer.viewer.destroy();
            }
        },
        redraw: function redraw() {
            if (osViewer.controls) {
                osViewer.controls.setPanning(true);
            }
            _calculateSizes(osViewer);
        },
        setImageSizes: function setImageSizes(imageInfo, sizes) {
            if (sizes) {
                var string = sizes.replace(/[\{\}]/, "");
                var sizes = JSON.parse(sizes);
                var iiifSizes = [];
                sizes.forEach(function (size) {
                    iiifSizes.push({ "width": parseInt(size), "height": parseInt(size) });
                });
                if (iiifSizes.length > 0) {
                    imageInfo.sizes = iiifSizes;
                } else {
                    delete imageInfo.sizes;
                }
            }
        },
        setTileSizes: function setTileSizes(imageInfo, tiles) {
            if (tiles) {
                var tileString = viewImage.getConfig().global.tileSizes.replace(/(\d+)/, '"$1"').replace("=", ":");
                var tiles = JSON.parse(tileString);
                var iiifTiles = [];

                Object.keys(tiles).forEach(function (size) {
                    var scaleFactors = tiles[size];
                    iiifTiles.push({ "width": parseInt(size), "height": parseInt(size), "scaleFactors": scaleFactors });
                });

                imageInfo.tiles = iiifTiles;
            }
        },
        onFirstTileLoaded: function onFirstTileLoaded() {
            var defer = Q.defer();

            if (viewImage.observables) {
                viewImage.observables.firstTileLoaded.subscribe(function (event) {
                    defer.resolve(event);
                }, function (error) {
                    defer.reject(error);
                });
            } else {
                defer.reject("No observables defined");
            }

            return defer.promise;
        },
        createTileSource: function createTileSource(source) {

            var result = Q.defer();

            viewImage.tileSourceResolver.resolveAsJson(source).then(function (imageInfo) {
                if (_debug) {
                    console.log("IIIF image info ", imageInfo);
                }
                viewImage.setImageSizes(imageInfo, _defaults.global.imageSizes);
                viewImage.setTileSizes(imageInfo, _defaults.global.tileSizes);
                var tileSource;
                if (_defaults.global.useTiles) {
                    tileSource = new OpenSeadragon.IIIFTileSource(imageInfo);
                } else {
                    tileSource = osViewer.createPyramid(imageInfo);
                }

                return tileSource;
            }, function (error) {
                if (viewImage.tileSourceResolver.isURI(_defaults.image.tileSource)) {
                    if (_debug) {
                        console.log("Image URL", _defaults.image.tileSource);
                    }

                    var tileSource = new OpenSeadragon.ImageTileSource({
                        url: _defaults.image.tileSource,
                        buildPyramid: true,
                        crossOriginPolicy: false
                    });

                    return tileSource;
                } else {
                    var errorMsg = "Failed to load tilesource from " + tileSource;

                    if (_debug) {
                        console.log(errorMsg);
                    }

                    return Q.reject(errorMsg);
                }
            }).then(function (tileSource) {
                result.resolve(tileSource);
            }).catch(function (errorMessage) {
                result.reject(errorMessage);
            });
            return result.promise;
        }
    };

    function createObservables(window, viewer) {
        var observables = {};

        observables.viewerOpen = Rx.Observable.create(function (observer) {
            viewer.addOnceHandler('open', function (event) {
                event.osState = "open";

                if (Number.isNaN(event.eventSource.viewport.getHomeBounds().x)) {
                    return observer.onError("Unknow error loading image from ", _defaults.image.tileSource);
                } else {
                    return observer.onNext(event);
                }
            });
            viewer.addOnceHandler('open-failed', function (event) {
                event.osState = "open-failed";
                console.log("Failed to open openseadragon ");

                return observer.onError(event);
            });
        });

        observables.firstTileLoaded = Rx.Observable.create(function (observer) {
            viewer.addOnceHandler('tile-loaded', function (event) {
                event.osState = "tile-loaded";

                return observer.onNext(event);
            });
            viewer.addOnceHandler('tile-load-failed', function (event) {
                event.osState = "tile-load-failed";
                console.log("Failed to load tile");

                return observer.onError(event);
            });
        });

        observables.viewerZoom = Rx.Observable.create(function (observer) {
            viewer.addHandler('zoom', function (event) {
                return observer.onNext(event);
            });
        });
        observables.animationComplete = Rx.Observable.create(function (observer) {
            viewer.addHandler('animation-finish', function (event) {
                return observer.onNext(event);
            });
        });
        observables.viewportUpdate = Rx.Observable.create(function (observer) {
            viewer.addHandler('update-viewport', function (event) {
                return observer.onNext(event);
            });
        });
        observables.animation = Rx.Observable.create(function (observer) {
            viewer.addHandler('animation', function (event) {
                return observer.onNext(event);
            });
        });
        observables.viewerRotate = Rx.Observable.create(function (observer) {
            viewer.addHandler('rotate', function (event) {
                event.osState = "rotate";
                return observer.onNext(event);
            });
        });
        observables.canvasResize = Rx.Observable.create(function (observer) {
            viewer.addHandler('resize', function (event) {
                event.osState = "resize";

                return observer.onNext(event);
            });
        });
        observables.windowResize = Rx.Observable.fromEvent(window, "resize").map(function (event) {
            event.osState = "window resize";

            return event;
        });
        observables.overlayRemove = Rx.Observable.create(function (observer) {
            viewer.addHandler('remove-overlay', function (event) {
                return observer.onNext(event);
            });
        });
        observables.overlayUpdate = Rx.Observable.create(function (observer) {
            viewer.addHandler('update-overlay', function (event) {
                return observer.onNext(event);
            });
        });
        observables.levelUpdate = Rx.Observable.create(function (observer) {
            viewer.addHandler('update-level', function (event) {
                return observer.onNext(event);
            });
        });
        observables.redrawRequired = observables.viewerOpen.merge(observables.viewerRotate.merge(observables.canvasResize).debounce(10)).map(function (event) {
            var location = {};

            if (osViewer.controls) {
                location = osViewer.controls.getLocation();
            }

            if (event.osState === "open") {
                location.zoom = osViewer.viewer.viewport.getHomeZoom();
                if (_defaults.image.location) {
                    location = _defaults.image.location;
                }
            }

            event.targetLocation = location;

            return event;
        }).publish();

        return observables;
    }

    function _calculateSizes(osViewer) {
        if (_debug) {
            console.log("viewImage: calcualte sizes");
            console.log("Home zoom = ", osViewer.viewer.viewport.getHomeZoom());
        }

        osViewer.sizes = new viewImage.Measures(osViewer);

        if (_defaults.global.adaptContainerHeight) {
            osViewer.sizes.resizeCanvas();
        }

        if (osViewer.viewer != null) {
            osViewer.viewer.viewport.setMargins({ bottom: osViewer.sizes.footerHeight + osViewer.sizes.calculateExcessHeight() });
        }

        if (_debug) {
            console.log("sizes: ", osViewer.sizes);
        }
    };

    function _overlayFooter(event) {
        if (_defaults.global.footerHeight > 0) {
            var footerHeight = _defaults.global.footerHeight;
            var footerPos = new OpenSeadragon.Point(0, _container.height() - footerHeight);
            var footerSize = new OpenSeadragon.Point(_container.width(), footerHeight);

            if (!_canvasScale) {
                _canvasScale = osViewer.viewer.drawer.context.canvas.width / osViewer.viewer.drawer.context.canvas.clientWidth;
            }

            if (_canvasScale != 1) {
                footerPos = footerPos.times(_canvasScale);
                footerSize = footerSize.times(_canvasScale);
            }
            osViewer.viewer.drawer.context.drawImage(_footerImage, footerPos.x, footerPos.y, footerSize.x, footerSize.y);
        }
    };

    function _timeout(promise, time) {
        var deferred = new jQuery.Deferred();

        $.when(promise).done(deferred.resolve).fail(deferred.reject).progress(deferred.notify);

        setTimeout(function () {
            deferred.reject("timeout");
        }, time);

        return deferred.promise();
    }

    return osViewer;
}(jQuery, OpenSeadragon);

// browser backward compability
if (!String.prototype.startsWith) {
    String.prototype.startsWith = function (subString) {
        var start = this.substring(0, subString.length);
        return start.localeCompare(subString) === 0;
    };
}
if (!String.prototype.endsWith) {
    String.prototype.endsWith = function (subString) {
        var start = this.substring(this.length - subString.length, this.length);
        return start.localeCompare(subString) === 0;
    };
}
if (!Array.prototype.find) {
    Array.prototype.find = function (comparator) {
        for (var int = 0; int < this.length; int++) {
            var element = this[int];
            if (comparator(element)) {
                return element;
            }
        }
    };
}
if (!Number.isNaN) {
    Number.isNaN = function (number) {
        return number !== number;
    };
}

},{}],6:[function(require,module,exports){
"use strict";

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
 * Module which contains all image informations like size, scale etc.
 * 
 * @version 3.2.0
 * @module viewImage.Measures
 * @requires jQuery
 */
var viewImage = function (osViewer) {
    'use strict';

    var _debug = false;

    osViewer.Measures = function (osViewer) {
        this.config = osViewer.getConfig();
        this.$container = $("#" + this.config.global.divId);

        this.outerCanvasSize = new OpenSeadragon.Point(this.$container.outerWidth(), this.$container.outerHeight());
        this.innerCanvasSize = new OpenSeadragon.Point(this.$container.width(), this.$container.height());
        this.originalImageSize = new OpenSeadragon.Point(this.getTotalImageWidth(osViewer.getImageInfo()), this.getMaxImageHeight(osViewer.getImageInfo()));
        // console.log("Original image size = ", this.originalImageSize);
        this.footerHeight = this.config.global.footerHeight;
        this.rotation = osViewer.viewer != null ? osViewer.viewer.viewport.getRotation() : 0;
        this.xPadding = this.outerCanvasSize.x - this.innerCanvasSize.x;
        this.yPadding = this.outerCanvasSize.y - this.innerCanvasSize.y;
        this.innerCanvasSize.y -= this.footerHeight;

        // calculate image size as it should be displayed in relation to canvas size
        if (this.fitToHeight()) {
            this.imageDisplaySize = new OpenSeadragon.Point(this.innerCanvasSize.y / this.ratio(this.getRotatedSize(this.originalImageSize)), this.innerCanvasSize.y);
        } else {
            this.imageDisplaySize = new OpenSeadragon.Point(this.innerCanvasSize.x, this.innerCanvasSize.x * this.ratio(this.getRotatedSize(this.originalImageSize)));
        }
        if (this.rotated()) {
            this.imageDisplaySize = this.getRotatedSize(this.imageDisplaySize);
        }
    };
    osViewer.Measures.prototype.getMaxImageWidth = function (imageInfo) {
        var width = 0;
        if (imageInfo && imageInfo.length > 0) {
            for (var i = 0; i < imageInfo.length; i++) {
                var tileSource = imageInfo[i];
                if (tileSource.tileSource) {
                    correction = tileSource.width;
                    tileSource = tileSource.tileSource;
                }
                width = Math.max(width, tileSource.width * correction);
            }
        }
        return width;
    };
    osViewer.Measures.prototype.getMaxImageHeight = function (imageInfo) {
        var height = 0;
        if (imageInfo && imageInfo.length > 0) {
            for (var i = 0; i < imageInfo.length; i++) {
                var tileSource = imageInfo[i];
                var correction = 1;
                if (tileSource.tileSource) {
                    correction = tileSource.width;
                    tileSource = tileSource.tileSource;
                }
                height = Math.max(height, tileSource.height * correction);
            }
        }
        return height;
    };
    osViewer.Measures.prototype.getTotalImageWidth = function (imageInfo) {
        var width = 0;
        if (imageInfo && imageInfo.length > 0) {
            for (var i = 0; i < imageInfo.length; i++) {
                var tileSource = imageInfo[i];
                var correction = 1;
                if (tileSource.tileSource) {
                    correction = tileSource.width;
                    tileSource = tileSource.tileSource;
                }
                width += tileSource.width * correction;
            }
        }
        return width;
    };
    osViewer.Measures.prototype.getTotalImageHeight = function (imageInfo) {
        var height = 0;
        if (imageInfo && imageInfo.length > 0) {
            for (var i = 0; i < imageInfo.length; i++) {
                var tileSource = imageInfo[i];
                var aspectRatio;
                if (tileSource.tileSource) {
                    correction = tileSource.width;
                    tileSource = tileSource.tileSource;
                }
                height += tileSource.height * correction;
            }
        }
        return height;
    };
    osViewer.Measures.prototype.getImageHomeSize = function () {
        var ratio = this.rotated() ? 1 / this.ratio(this.originalImageSize) : this.ratio(this.originalImageSize);
        if (this.fitToHeight()) {
            var height = this.innerCanvasSize.y;
            var width = height / ratio;
        } else {
            var width = this.innerCanvasSize.x;
            var height = width * ratio;
        }
        return this.getRotatedSize(new OpenSeadragon.Point(width, height));
    };
    osViewer.Measures.prototype.rotated = function () {
        return this.rotation % 180 !== 0;
    };
    osViewer.Measures.prototype.landscape = function () {
        return this.ratio(this.originalImageSize) < 1;
    };
    osViewer.Measures.prototype.ratio = function (size) {
        return size.y / size.x;
    };
    osViewer.Measures.prototype.getRotatedSize = function (size) {
        return new OpenSeadragon.Point(this.rotated() ? size.y : size.x, this.rotated() ? size.x : size.y);
    };
    osViewer.Measures.prototype.fitToHeight = function () {
        return !this.config.global.adaptContainerHeight && this.ratio(this.getRotatedSize(this.originalImageSize)) > this.ratio(this.innerCanvasSize);
    };
    osViewer.Measures.prototype.resizeCanvas = function () {
        // Set height of container if required
        if (this.config.global.adaptContainerHeight) {
            if (_debug) {
                console.log("adapt container height");
            }
            this.$container.height(this.getRotatedSize(this.imageDisplaySize).y + this.footerHeight);
        }
        this.outerCanvasSize = new OpenSeadragon.Point(this.$container.outerWidth(), this.$container.outerHeight());
        this.innerCanvasSize = new OpenSeadragon.Point(this.$container.width(), this.$container.height() - this.footerHeight);
    };
    osViewer.Measures.prototype.calculateExcessHeight = function () {
        var imageSize = this.getRotatedSize(this.getImageHomeSize());
        var excessHeight = this.config.global.adaptContainerHeight || this.fitToHeight() ? 0 : 0.5 * (this.innerCanvasSize.y - imageSize.y);
        return excessHeight;
    };

    return osViewer;
}(viewImage || {}, jQuery);

},{}],7:[function(require,module,exports){
'use strict';

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
 * Module which shows existing overlays on images.
 * 
 * @version 3.2.0
 * @module viewImage.overlays
 * @requires jQuery
 */
var viewImage = function (osViewer) {
    'use strict';

    var _debug = false;
    var _focusStyleClass = 'focus';
    var _highlightStyleClass = 'highlight';
    var _overlayFocusHook = null;
    var _overlayClickHook = null;
    var _drawingOverlay = null;
    var _overlays = [];
    var _defaults = {};

    var _initializedCallback = null;

    osViewer.overlays = {
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('osViewer.overlays.init');
                console.log('##############################');
            }

            $.extend(true, _defaults, config);

            osViewer.observables.overlayRemove.subscribe(function (event) {
                if (event.element) {
                    $(event.element).remove();
                }
            });
            if (_defaults.image.highlightCoords) {
                osViewer.observables.viewerOpen.subscribe(function (data) {
                    for (var index = 0; index < _defaults.image.highlightCoords.length; index++) {
                        var highlightCoords = _defaults.image.highlightCoords[index];
                        var imageIndex = highlightCoords.pageIndex;
                        osViewer.overlays.draw(highlightCoords.name, highlightCoords.displayTooltip, imageIndex);
                    }
                    if (_initializedCallback) {
                        _initializedCallback();
                    }
                });
            }
        },
        onInitialized: function onInitialized(callback) {
            var oldCallback = _initializedCallback;
            _initializedCallback = function _initializedCallback() {
                if (oldCallback) {
                    oldCallback();
                }
                callback();
            };
        },
        onFocus: function onFocus(hook) {
            var tempHook = _overlayFocusHook;
            _overlayFocusHook = function _overlayFocusHook(overlay, focus) {
                if (tempHook) tempHook(overlay, focus);
                hook(overlay, focus);
            };
        },
        onClick: function onClick(hook) {
            var tempHook = _overlayClickHook;
            _overlayClickHook = function _overlayClickHook(overlay) {
                if (tempHook) tempHook(overlay);
                hook(overlay);
            };
        },
        getOverlays: function getOverlays() {
            return _overlays.slice();
        },
        getRects: function getRects() {
            return _overlays.filter(function (overlay) {
                return overlay.type === osViewer.overlays.overlayTypes.RECTANGLE;
            }).slice();
        },
        getLines: function getLines() {
            return _overlays.filter(function (overlay) {
                return overlay.type === osViewer.overlays.overlayTypes.LINE;
            }).slice();
        },
        draw: function draw(group, displayTitle, imageIndex) {
            if (_debug) {
                console.log('osViewer.overlays.draw: group - ' + group);
                console.log('osViewer.overlays.draw: displayTitle - ' + displayTitle);
                console.log('osViewer.overlays.draw: imageIndex - ' + imageIndex);
            }

            var coordList = _defaults.getCoordinates(group);
            if (coordList) {
                for (var index = 0; index < coordList.coordinates.length; index++) {
                    var coords = coordList.coordinates[index];
                    var title = displayTitle && coords.length > 4 ? coords[4] : '';
                    var id = coords.length > 5 ? coords[5] : index;
                    _createRectangle(coords[0], coords[1], coords[2] - coords[0], coords[3] - coords[1], title, id, group, imageIndex);
                }
            }
        },
        unDraw: function unDraw(group) {
            if (_debug) {
                console.log('osViewer.overlays.unDraw: group - ' + group);
            }

            var newRects = [];
            _overlays = _overlays.filter(function (overlay) {
                if (overlay.group === group) {
                    osViewer.viewer.removeOverlay(overlay.element);
                    return false;
                } else {
                    return true;
                }
            });
        },
        highlight: function highlight(group) {
            if (_debug) {
                console.log('osViewer.overlays.highlight: group - ' + group);
            }

            _overlays.filter(function (overlay) {
                return overlay.group === group;
            }).forEach(function (overlay) {
                if (overlay.element) {
                    overlay.element.highlight(true);
                }
            });
        },
        unHighlight: function unHighlight(group) {
            if (_debug) {
                console.log('osViewer.overlays.unHighlight: group - ' + group);
            }

            _overlays.filter(function (overlay) {
                return overlay.group === group;
            }).forEach(function (overlay) {
                if (overlay.element) {
                    overlay.element.highlight(false);
                }
            });
        },
        focusBox: function focusBox(group, id) {
            if (_debug) {
                console.log('osViewer.overlays.highlightBox: group - ' + group);
                console.log('osViewer.overlays.highlightBox: id - ' + id);
            }
            _overlays.filter(function (overlay) {
                return overlay.group === group;
            }).forEach(function (overlay) {
                if (overlay.element) {
                    overlay.element.focus(overlay.id === id);
                }
            });
        },
        addOverlay: function addOverlay(overlay) {
            if (_debug) {
                console.log('osViewer.overlays.addOverlay: overlay - ' + overlay);
            }

            _overlays.push(overlay);
            if (overlay.element) {
                osViewer.viewer.updateOverlay(overlay.element, overlay.rect, 0);
            }
        },
        removeOverlay: function removeOverlay(overlay) {
            if (overlay) {
                if (_debug) console.log("Removing overlay " + overlay.id);
                var index = _overlays.indexOf(overlay);
                _overlays.splice(index, 1);
                if (overlay.element) {
                    osViewer.viewer.removeOverlay(overlay.element);
                }
            }
        },
        drawRect: function drawRect(rectangle, group, title, id) {
            if (_debug) {
                console.log('osViewer.overlays.drawRect: rectangle - ' + rectangle);
                console.log('osViewer.overlays.drawRect: group - ' + group);
            }

            _createRectangle(rectangle.x, rectangle.y, rectangle.width, rectangle.height, title ? title : "", id ? id : "", group);
        },
        drawLine: function drawLine(point1, point2, group) {
            if (_debug) {
                console.log('osViewer.overlays.drawLine: point1 - ' + point1);
                console.log('osViewer.overlays.drawLine: point2 - ' + point2);
                console.log('osViewer.overlays.drawLine: group - ' + group);
            }

            _createLine(point1.x, point1.y, point2.x, point2.y, "", "", group);
        },
        showOverlay: function showOverlay(overlay) {
            if (overlay && !overlay.element) {
                _drawOverlay(overlay);
                if (_overlayFocusHook) {
                    _overlayFocusHook(overlay, true);
                }
            }
        },
        hideOverlay: function hideOverlay(overlay) {
            if (overlay && overlay.element && _drawingOverlay != overlay) {
                _undrawOverlay(overlay);
                if (_overlayFocusHook) {
                    _overlayFocusHook(overlay, false);
                }
            }
        },
        getOverlay: function getOverlay(id, group) {
            var overlay = _overlays.find(function (overlay) {
                if (group) {
                    return overlay.id === id && overlay.group === group;
                } else {
                    return overlay.id === id;
                }
            });
            // console.log("search for overlay with id = " + id);
            // console.log("Found overlay ", overlay);
            return overlay;
        },
        getCoordinates: function getCoordinates(overlay) {
            if (_debug) {
                console.log("getCoordinates - overlay", overlay);
            }
            if (overlay.type === osViewer.overlays.overlayTypes.RECTANGLE) {
                var transformedRect = osViewer.viewer.viewport.viewportToImageRectangle(overlay.rect);
                transformedRect = transformedRect.times(osViewer.getScaleToOriginalSize());
                return transformedRect;
            } else if (overlay.type === osViewer.overlays.overlayTypes.LINE) {
                var p1 = osViewer.viewer.viewport.viewportToImageCoordinates(overlay.poin1);
                var p2 = osViewer.viewer.viewport.viewportToImageCoordinates(overlay.poin2);
                return {
                    point1: p1,
                    point2: p2
                };
            }
        },
        getDrawingOverlay: function getDrawingOverlay() {
            return _drawingOverlay;
        },
        setDrawingOverlay: function setDrawingOverlay(overlay) {
            _drawingOverlay = overlay;
        },
        showHiddenOverlays: function showHiddenOverlays() {
            osViewer.viewer.addViewerInputHook({
                hooks: [{
                    tracker: "viewer",
                    handler: "moveHandler",
                    hookHandler: _onViewerMove
                }]
            });
        },
        contains: function contains(rect, point, precision) {
            if (precision == null) {
                precision = 0;
            }
            return _isInside(rect, point, precision);
        },
        overlayTypes: {
            RECTANGLE: "rectangle",
            LINE: "line"
        },
        getRotated: function getRotated(point) {
            // var rotation = osViewer.viewer.viewport.getRotation();
            // var center = osViewer.viewer.viewport.getCenter( true );
            // point = point.rotate(-rotation, center);
            return point;
        }
    };

    function _createLine(x1, y1, x2, y2, title, id, group) {
        if (_debug) {
            console.log('------------------------------');
            console.log('Overlays _createLine: x1 - ' + x1);
            console.log('Overlays _createLine: y1 - ' + y1);
            console.log('Overlays _createLine: x2 - ' + x2);
            console.log('Overlays _createLine: y2 - ' + y2);
            console.log('Overlays _createLine: title - ' + title);
            console.log('Overlays _createLine: id - ' + id);
            console.log('Overlays _createLine: group - ' + group);
            console.log('------------------------------');
        }
        x1 = osViewer.scaleToImageSize(x1);
        y1 = osViewer.scaleToImageSize(y1);
        x2 = osViewer.scaleToImageSize(x2);
        y2 = osViewer.scaleToImageSize(y2);

        var p1 = new OpenSeadragon.Point(x1, y1);
        var p2 = new OpenSeadragon.Point(x2, y2);
        var length = p1.distanceTo(p2);

        var angle = _calculateAngle(p1, p2);
        var beta = (180 - angle) / 2;
        // console.log( "drawing line with length = " + length + " and angle = " + angle );

        y1 += length / 2 * Math.sin(angle * Math.PI / 180);
        x1 -= length / 2 * Math.sin(angle * Math.PI / 180) / Math.tan(beta * Math.PI / 180);

        var rectangle = osViewer.viewer.viewport.imageToViewportRectangle(x1, y1, length, 1);
        var p1Viewer = osViewer.viewer.viewport.imageToViewportCoordinates(p1);
        var p2Viewer = osViewer.viewer.viewport.imageToViewportCoordinates(p2);
        var overlay = {
            type: osViewer.overlays.overlayTypes.LINE,
            rect: rectangle,
            angle: angle,
            point1: p1Viewer,
            point2: p2Viewer,
            group: group,
            id: id,
            title: title
        };
        var overlayStyle = _defaults.getOverlayGroup(overlay.group);
        if (!overlayStyle.hidden) {
            _drawOverlay(overlay);
        }
        _overlays.push(overlay);
    }

    /**
     * coordinates are in original image space
     */
    function _createRectangle(x, y, width, height, title, id, group, imageIndex) {
        if (_debug) {
            console.log('------------------------------');
            console.log('Overlays _createRectangle: x - ' + x);
            console.log('Overlays _createRectangle: y - ' + y);
            console.log('Overlays _createRectangle: width - ' + width);
            console.log('Overlays _createRectangle: height - ' + height);
            console.log('Overlays _createRectangle: title - ' + title);
            console.log('Overlays _createRectangle: id - ' + id);
            console.log('Overlays _createRectangle: group - ' + group);
            console.log('Overlays _createRectangle: imageIndex - ' + imageIndex);
            console.log('------------------------------');
        }
        x = osViewer.scaleToImageSize(x);
        y = osViewer.scaleToImageSize(y);
        width = osViewer.scaleToImageSize(width);
        height = osViewer.scaleToImageSize(height);

        if (!imageIndex) {
            imageIndex = 0;
        }
        var tiledImage = osViewer.viewer.world.getItemAt(imageIndex);
        var rectangle = tiledImage.imageToViewportRectangle(x, y, width, height);
        // console.log("Found rect ", rectangle);
        // var rectangle = osViewer.viewer.viewport.imageToViewportRectangle( x, y, width, height
        // );
        var overlay = {
            type: osViewer.overlays.overlayTypes.RECTANGLE,
            rect: rectangle,
            group: group,
            id: id,
            title: title
        };
        var overlayStyle = _defaults.getOverlayGroup(overlay.group);
        if (!overlayStyle.hidden) {
            _drawOverlay(overlay);
        }
        _overlays.push(overlay);
    }

    function _undrawOverlay(overlay) {
        osViewer.viewer.removeOverlay(overlay.element);
        overlay.element = null;
    }

    function _drawOverlay(overlay) {
        if (_debug) {
            console.log("viewImage.overlays._drawOverlay");
            console.log("overlay: ", overlay);
        }
        var element = document.createElement("div");
        $(element).attr("id", "overlay_" + overlay.id);
        var overlayStyle = _defaults.getOverlayGroup(overlay.group);
        if (overlayStyle) {
            if (_debug) console.log("overlay style", overlayStyle);
            // element.title = overlay.title;
            // $( element ).attr( "data-toggle", "tooltip" );
            // $( element ).attr( "data-placement", "auto top" );
            $(element).addClass(overlayStyle.styleClass);

            if (overlay.type === osViewer.overlays.overlayTypes.LINE) {
                _rotate(overlay.angle, element);
            }

            if (overlayStyle.interactive) {
                element.focus = function (focus) {
                    if (focus) {
                        $(element).addClass(_focusStyleClass);
                        _createTooltip(element, overlay);

                        // tooltip.height(100);
                        // $( element ).tooltip( "show" );
                    } else {
                        $(element).removeClass(_focusStyleClass);
                        $(".tooltipp#tooltip_" + overlay.id).remove();
                    }
                    if (_overlayFocusHook) {
                        _overlayFocusHook(overlay, focus);
                    }
                };

                element.highlight = function (focus) {
                    if (focus) {
                        $(element).addClass(_highlightStyleClass);
                    } else {
                        $(element).removeClass(_highlightStyleClass);
                    }
                };

                $(element).on("mouseover", function () {
                    if (_debug) {
                        console.log('Overlays _drawOverlay: mouse over - ' + overlayStyle.name);
                    }
                    osViewer.overlays.focusBox(overlay.group, overlay.id);
                });
                $(element).on("mouseout", function () {
                    if (_debug) {
                        console.log('Overlays _drawOverlay: mouse out - ' + overlayStyle.name);
                    }
                    element.focus(false);
                });
                $(element).on("click", function () {
                    if (_overlayClickHook) {
                        _overlayClickHook(overlay);
                    }
                });
            }
            overlay.element = element;
            osViewer.viewer.addOverlay(element, overlay.rect, 0);
        }
    }

    function _createTooltip(element, overlay) {
        if (overlay.title) {
            var canvasCorner = osViewer.sizes.$container.offset();

            var top = $(element).offset().top;
            var left = $(element).offset().left;
            var bottom = top + $(element).outerHeight();
            var right = left + $(element).outerWidth();
            // console.log("Tooltip at ", left, top, right, bottom);


            var $tooltip = $("<div class='tooltipp'>" + overlay.title + "</div>");
            $("body").append($tooltip);
            var tooltipPadding = parseFloat($tooltip.css("padding-top"));
            $tooltip.css("max-width", right - left);
            $tooltip.css("top", Math.max(canvasCorner.top + tooltipPadding, top - $tooltip.outerHeight() - tooltipPadding));
            $tooltip.css("left", Math.max(canvasCorner.left + tooltipPadding, left));
            $tooltip.attr("id", "tooltip_" + overlay.id);
            // console.log("tooltip width = ", $tooltip.width());

            // listener for zoom

            osViewer.observables.animation.do(function () {
                // console.log("element at: ", $(element).offset());
                var top = Math.max($(element).offset().top, canvasCorner.top);
                var left = Math.max($(element).offset().left, canvasCorner.left);
                $tooltip.css("top", Math.max(canvasCorner.top + tooltipPadding, top - $tooltip.outerHeight() - tooltipPadding));
                $tooltip.css("left", Math.max(canvasCorner.left + tooltipPadding, left));
            }).takeWhile(function () {
                return $(".tooltipp").length > 0;
            }).subscribe();
        }
    }

    function _rotate(angle, mapElement) {
        if (_debug) {
            console.log('Overlays _rotate: angle - ' + angle);
            console.log('Overlays _rotate: mapElement - ' + mapElement);
        }

        if (angle !== 0) {
            $(mapElement).css("-moz-transform", "rotate(" + angle + "deg)");
            $(mapElement).css("-webkit-transform", "rotate(" + angle + "deg)");
            $(mapElement).css("-ms-transform", "rotate(" + angle + "deg)");
            $(mapElement).css("-o-transform", "rotate(" + angle + "deg)");
            $(mapElement).css("transform", "rotate(" + angle + "deg)");
            var sin = Math.sin(angle);
            var cos = Math.cos(angle);
            $(mapElement).css("filter", "progid:DXImageTransform.Microsoft.Matrix(M11=" + cos + ", M12=" + sin + ", M21=-" + sin + ", M22=" + cos + ", sizingMethod='auto expand'");
        }
    }

    function _calculateAngle(p1, p2) {
        if (_debug) {
            console.log('Overlays _calculateAngle: p1 - ' + p1);
            console.log('Overlays _calculateAngle: p2 - ' + p2);
        }

        var dx = p2.x - p1.x;
        var dy = p2.y - p1.y;
        var radians = null;

        if (dx > 0) {
            radians = Math.atan(dy / dx);
            return radians * 180 / Math.PI;
        } else if (dx < 0) {
            radians = Math.atan(dy / dx);
            return radians * 180 / Math.PI + 180;
        } else if (dy < 0) {
            return 270;
        } else {
            return 90;
        }
    }

    // function _getScaleToOriginalSize() {
    // var displaySize = osViewer.viewer.world.getItemAt(0).source.dimensions.x;//
    // osViewer.viewer.viewport.contentSize.x;
    // return osViewer.getImageInfo().width / displaySize;
    // }
    //    
    // function _scaleToOriginalSize( value ) {
    // if ( _debug ) {
    // console.log( 'Overlays _scaleToOriginalSize: value - ' + value );
    // }
    //        
    // var displaySize = osViewer.viewer.world.getItemAt(0).source.dimensions.x;//
    // osViewer.viewer.viewport.contentSize.x;
    // return value / displaySize * osViewer.getImageInfo().width;
    // }
    //    
    // function _scaleToImageSize( value ) {
    // if ( _debug ) {
    // console.log( 'Overlays _scaleToImageSize: value - ' + value );
    // }
    //        
    // var displaySize = osViewer.viewer.world.getItemAt(0).source.dimensions.x;//
    // osViewer.viewer.viewport.contentSize.x;
    // return value * displaySize / osViewer.getImageInfo().width;
    // }

    function _isInside(rect, point, extra) {
        return point.x > rect.x - extra && point.x < rect.x + rect.width + extra && point.y > rect.y - extra && point.y < rect.y + rect.height + extra;
    }

    function _onViewerMove(event) {
        var position = event.position;
        var ieVersion = viewerJS.helper.detectIEVersion();
        if (ieVersion && ieVersion === 10) {
            // console.log("Correct position for ie ", ieVersion);
            position.x += $(window).scrollLeft();
            position.y += $(window).scrollTop();
        }
        // console.log( "viewer move ", position);
        var point = osViewer.viewer.viewport.viewerElementToViewportCoordinates(position);
        _overlays.forEach(function (o) {
            if (_isInside(o.rect, point, 0)) {
                osViewer.overlays.showOverlay(o);
            } else {
                osViewer.overlays.hideOverlay(o);
            }
        });
    }

    return osViewer;
}(viewImage || {}, jQuery);

},{}],8:[function(require,module,exports){
'use strict';

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
 * Module which handles the viewer reading mode.
 * 
 * @version 3.2.0
 * @module viewImage.readingMode
 * @requires jQuery
 */
var viewImage = function (osViewer) {
    'use strict';

    var _debug = false;
    var _localStoragePossible = true;
    var _activePanel = null;
    var _defaults = {
        navSelector: '.reading-mode__navigation',
        viewSelector: '#contentView',
        imageContainerSelector: '.reading-mode__content-view-image',
        imageSelector: '#readingModeImage',
        sidebarSelector: '#contentSidebar',
        sidebarToggleButton: '.reading-mode__content-sidebar-toggle',
        sidebarInnerSelector: '.reading-mode__content-sidebar-inner',
        sidebarTabsSelector: '.reading-mode__content-sidebar-tabs',
        sidebarTabContentSelector: '.tab-content',
        sidebarTocWrapperSelector: '.widget-toc-elem-wrapp',
        sidebarStatus: '',
        useTabs: true,
        useAccordeon: false,
        msg: {}
    };

    osViewer.readingMode = {
        /**
         * Method to initialize the viewer reading mode.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.navSelector A string which contains the selector for the
         * navigation.
         * @param {String} config.viewSelector A string which contains the selector for
         * the content view.
         * @param {String} config.imageContainerSelector A string which contains the
         * selector for the image container.
         * @param {String} config.imageSelector A string which contains the selector for
         * the image.
         * @param {String} config.sidebarSelector A string which contains the selector for
         * the sidebar.
         * @param {String} config.sidebarToggleButton A string which contains the selector
         * for the sidebar toggle button.
         * @param {String} config.sidebarInnerSelector A string which contains the
         * selector for the inner sidebar container.
         * @param {String} config.sidebarStatus A string which contains the current
         * sidebar status.
         */
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('osViewer.readingMode.init');
                console.log('##############################');
                console.log('osViewer.readingMode.init: config - ', config);
            }

            $.extend(true, _defaults, config);

            // check local storage
            _localStoragePossible = viewerJS.helper.checkLocalStorage();

            if (_localStoragePossible) {
                _defaults.sidebarStatus = localStorage.getItem('sidebarStatus');

                if (_defaults.sidebarStatus === '' || _defaults.sidebarStatus === undefined) {
                    localStorage.setItem('sidebarStatus', 'true');
                }

                // set viewport
                _setViewportHeight();
                if (_defaults.useTabs) {
                    _setSidebarTabHeight();
                }
                _setSidebarButtonPosition();
                _checkSidebarStatus();
                setTimeout(function () {
                    _showContent();
                }, 500);

                // save panel status
                if (_defaults.useAccordeon) {
                    _activePanel = localStorage.getItem('activePanel');

                    $('.panel-collapse').each(function () {
                        $(this).removeClass('in');
                    });

                    if (_activePanel === null) {
                        localStorage.setItem('activePanel', '#collapseOne');
                        _activePanel = localStorage.getItem('activePanel');

                        $(_activePanel).addClass('in');
                    } else {
                        $(_activePanel).addClass('in');
                    }

                    // click panel event
                    $('a[data-toggle="collapse"]').on('click', function () {
                        var currPanel = $(this).attr('href');

                        localStorage.setItem('activePanel', currPanel);
                    });
                }

                // events
                $('[data-toggle="sidebar"]').on('click', function () {
                    $(this).toggleClass('in');
                    $(this).parents('.reading-mode__content-sidebar').toggleClass('in');
                    $(this).parents('.reading-mode__content-sidebar').prev().toggleClass('in');

                    // set sidebar status to local storage
                    _defaults.sidebarStatus = localStorage.getItem('sidebarStatus');

                    if (_defaults.sidebarStatus === 'false') {
                        localStorage.setItem('sidebarStatus', 'true');
                    } else {
                        localStorage.setItem('sidebarStatus', 'false');
                    }
                });

                $(window).on('resize', function () {
                    _setViewportHeight();
                    if (_defaults.useTabs) {
                        _setSidebarTabHeight();
                    }
                    _setSidebarButtonPosition();
                });

                $(window).on("orientationchange", function () {
                    _setViewportHeight();
                    if (_defaults.useTabs) {
                        _setSidebarTabHeight();
                    }
                    _setSidebarButtonPosition();
                });

                // AJAX Loader Eventlistener
                if (typeof jsf !== 'undefined') {
                    jsf.ajax.addOnEvent(function (data) {
                        var ajaxstatus = data.status;

                        switch (ajaxstatus) {
                            case "success":
                                _setViewportHeight();
                                if (_defaults.useTabs) {
                                    _setSidebarTabHeight();
                                }
                                _setSidebarButtonPosition();
                                break;
                        }
                    });
                }
            } else {
                return false;
            }
        }
    };

    /**
     * Method which sets the height of the viewport elements.
     * 
     * @method _setViewportHeight
     */
    function _setViewportHeight() {
        if (_debug) {
            console.log('---------- _setViewportHeight() ----------');
            console.log('_setViewportHeight: view = ', _defaults.viewSelector);
            console.log('_setViewportHeight: image = ', _defaults.imageSelector);
            console.log('_setViewportHeight: sidebar = ', _defaults.sidebarSelector);
            console.log('_setViewportHeight: sidebarInner = ', _defaults.sidebarInnerSelector);
            console.log('_setViewportHeight: sidebarTabs = ', _defaults.sidebarTabsSelector);
        }

        var viewportHeight = $(window).outerHeight();
        var navHeight = $(_defaults.navSelector).outerHeight();
        var newHeight = viewportHeight - navHeight;

        if (_debug) {
            console.log('_setViewportHeight: viewportHeight = ', viewportHeight);
            console.log('_setViewportHeight: navHeight = ', navHeight);
            console.log('_setViewportHeight: newHeight = ', newHeight);
        }

        $(_defaults.viewSelector).css('height', newHeight);
        $(_defaults.imageSelector).css('height', newHeight);
        $(_defaults.sidebarSelector).css('height', newHeight);
        $(_defaults.sidebarInnerSelector).css('height', newHeight);
    }

    /**
     * Method which sets the height of the sidebar Tabs.
     * 
     * @method _setSidebarTabHeight
     */
    function _setSidebarTabHeight() {
        if (_debug) {
            console.log('---------- _setSidebarTabHeight() ----------');
            console.log('_setSidebarTabHeight: sidebarTabs = ', _defaults.sidebarTabsSelector);
        }

        var viewportHeight = $(window).outerHeight();
        var navHeight = $(_defaults.navSelector).outerHeight();
        var newHeight = viewportHeight - navHeight;
        var tabPos = $(_defaults.sidebarTabsSelector).position();
        var tabHeight = newHeight - tabPos.top - 15;
        var navTabsHeight = $('.nav-tabs').outerHeight();

        if (_debug) {
            console.log('_setSidebarTabHeight: tabPos = ', tabPos);
            console.log('_setSidebarTabHeight: tabHeight = ', tabHeight);
        }

        if (viewportHeight > 768) {
            $(_defaults.sidebarTabsSelector).css('height', tabHeight);
            $(_defaults.sidebarTabContentSelector).css('height', tabHeight - navTabsHeight);
            $(_defaults.sidebarTocWrapperSelector).css('min-height', tabHeight - navTabsHeight);
        }
    }

    /**
     * Method which sets the position of the sidebar toggle button.
     * 
     * @method _setSidebarButtonPosition
     */
    function _setSidebarButtonPosition() {
        if (_debug) {
            console.log('---------- _setSidebarButtonPosition() ----------');
            console.log('_setSidebarButtonPosition: view = ', _defaults.viewSelector);
        }

        var viewHalfHeight = $(_defaults.viewSelector).outerHeight() / 2;

        if (_debug) {
            console.log('_setSidebarButtonPosition: viewHalfHeight = ', viewHalfHeight);
        }

        $(_defaults.sidebarToggleButton).css('top', viewHalfHeight);
    }

    /**
     * Method which checks the current sidebar status, based on a local storage value.
     * 
     * @method _checkSidebarStatus
     * @returns {Boolean} Returns false if the sidebar is inactive, returns true if the
     * sidebar is active.
     */
    function _checkSidebarStatus() {
        if (_debug) {
            console.log('---------- _checkSidebarStatus() ----------');
            console.log('_checkSidebarStatus: sidebarStatus = ', _defaults.sidebarStatus);
        }

        if (_defaults.sidebarStatus === 'false') {
            $('[data-toggle="sidebar"]').removeClass('in');
            $('.reading-mode__content-sidebar').removeClass('in').prev().removeClass('in');

            return false;
        } else {
            return true;
        }
    }

    /**
     * Method which shows the content by removing CSS-Classes after loading every page
     * element.
     * 
     * @method _showContent
     */
    function _showContent() {
        if (_debug) {
            console.log('---------- _showContent() ----------');
        }

        $(_defaults.viewSelector).removeClass('invisible');
        $(_defaults.sidebarSelector).removeClass('invisible');
    }

    return osViewer;
}(viewImage || {}, jQuery);

},{}],9:[function(require,module,exports){
"use strict";

var _typeof = typeof Symbol === "function" && typeof Symbol.iterator === "symbol" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; };

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
 * Module which interprets the image information.
 * 
 * @version 3.2.0
 * @module viewImage.tileSourceResolver
 * @requires jQuery
 */
var viewImage = function (osViewer) {
    'use strict';

    var _debug = false;

    osViewer.tileSourceResolver = {

        resolveAsJsonOrURI: function resolveAsJsonOrURI(imageInfo) {
            var deferred = Q.defer();
            if (this.isJson(imageInfo)) {
                deferred.resolve(imageInfo);
            } else if (this.isStringifiedJson(imageInfo)) {
                deferred.resolve(JSON.parse(imageInfo));
            } else {
                deferred.resolve(imageInfo);
            }
            return deferred.promise;
        },

        resolveAsJson: function resolveAsJson(imageInfo) {
            var deferred = Q.defer();
            if (this.isURI(imageInfo)) {
                if (this.isJsonURI(imageInfo)) {
                    return this.loadJsonFromURL(imageInfo);
                } else {
                    deferred.reject("Url does not lead to a json object");
                }
            } else if (typeof imageInfo === "string") {
                try {
                    var json = JSON.parse(imageInfo);
                    deferred.resolve(json);
                } catch (error) {
                    deferred.reject("String does not contain valid json: " + error);
                }
            } else if ((typeof imageInfo === "undefined" ? "undefined" : _typeof(imageInfo)) === "object") {
                deferred.resolve(imageInfo);
            } else {
                deferred.reject("Neither a url nor a json object");
            }
            return deferred.promise;
        },

        loadJsonFromURL: function loadJsonFromURL(imageInfo) {
            var deferred = Q.defer();
            if (this.isJsonURI(imageInfo)) {
                OpenSeadragon.makeAjaxRequest(imageInfo,
                // success
                function (request) {
                    try {
                        deferred.resolve(JSON.parse(request.responseText));
                    } catch (error) {
                        deferred.reject(error);
                    }
                },
                // error
                function (error) {
                    deferred.reject(error);
                });
            } else {
                deferred.reject("Not a json uri: " + imageInfo);
            }
            return deferred.promise;
        },

        loadIfJsonURL: function loadIfJsonURL(imageInfo) {
            return Q.promise(function (resolve, reject) {
                if (osViewer.tileSourceResolver.isURI(imageInfo)) {
                    var ajaxParams = {
                        url: decodeURI(imageInfo),
                        type: "GET",
                        dataType: "JSON",
                        async: true,
                        crossDomain: true,
                        accepts: {
                            application_json: "application/json",
                            application_jsonLd: "application/ld+json",
                            text_json: "text/json",
                            text_jsonLd: "text/ld+json"
                        }
                    };
                    Q($.ajax(ajaxParams)).then(function (data) {
                        resolve(data);
                    }).fail(function (error) {
                        reject("Failed to retrieve json from " + imageInfo);
                    });
                    setTimeout(function () {
                        reject("Timeout after 10s");
                    }, 10000);
                } else {
                    reject("Not a uri: " + imageInfo);
                }
            });
        },

        isJsonURI: function isJsonURI(imageInfo) {
            if (this.isURI(imageInfo)) {
                var shortened = imageInfo.replace(/\?.*/, "");
                if (shortened.endsWith("/")) {
                    shortened = shortened.substring(0, shortened.length - 1);
                }
                return shortened.toLowerCase().endsWith(".json");
            }
            return false;
        },
        isURI: function isURI(imageInfo) {
            if (imageInfo && typeof imageInfo === "string") {
                if (imageInfo.startsWith("http://") || imageInfo.startsWith("https://") || imageInfo.startsWith("file:/")) {
                    return true;
                }
            }
            return false;
        },
        isStringifiedJson: function isStringifiedJson(imageInfo) {
            if (imageInfo && typeof imageInfo === "string") {
                try {
                    var json = JSON.parse(imageInfo);
                    return this.isJson(json);
                } catch (error) {
                    // no json
                    return false;
                }
            }
            return false;
        },
        isJson: function isJson(imageInfo) {
            return imageInfo && (typeof imageInfo === "undefined" ? "undefined" : _typeof(imageInfo)) === "object";
        }

    };

    return osViewer;
}(viewImage || {}, jQuery);

},{}],10:[function(require,module,exports){
"use strict";

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
 * Module which resizes existing rectangles on an image.
 * 
 * @version 3.2.0
 * @module viewImage.transformRect
 * @requires jQuery
 */
var viewImage = function (osViewer) {
    'use strict';

    var DEFAULT_CURSOR = "default";

    var _debug = false;

    var _drawingStyleClass = "transforming";

    var _active = false;
    var _drawing = false;
    var _group = null;
    var _finishHook = null;
    var _viewerInputHook = null;
    var _hbAdd = 5;
    var _sideClickPrecision = 0.004;
    var _drawArea = "";
    var _enterPoint = null;

    osViewer.transformRect = {
        init: function init() {
            if (_debug) {
                console.log('##############################');
                console.log('osViewer.transformRect.init');
                console.log('##############################');
            }
            _viewerInputHook = osViewer.viewer.addViewerInputHook({
                hooks: [{
                    tracker: "viewer",
                    handler: "clickHandler",
                    hookHandler: _disableViewerEvent
                    // }, {
                    // tracker : "viewer",
                    // handler : "scrollHandler",
                    // hookHandler : _disableViewerEvent
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
                }, {
                    tracker: "viewer",
                    handler: "releaseHandler",
                    hookHandler: _onViewerRelease
                }, {
                    tracker: "viewer",
                    handler: "moveHandler",
                    hookHandler: _onViewerMove
                }]
            });
        },
        startDrawing: function startDrawing(overlay, finishHook) {
            if (_debug) console.log("Start drawing");
            osViewer.overlays.setDrawingOverlay(overlay);
            _active = true;
            _group = overlay.group;
            _finishHook = finishHook;
            $(overlay.element).addClass(_drawingStyleClass);
        },
        endDrawing: function endDrawing() {
            _drawing = false;
            _group = null;
            _finishHook = null;
            _active = false;
            var drawOverlay = osViewer.overlays.getDrawingOverlay();
            if (drawOverlay != null) {
                $(drawOverlay.element).removeClass(_drawingStyleClass);
                $(drawOverlay.element).css({
                    cursor: DEFAULT_CURSOR
                });
            }
        },
        isActive: function isActive() {
            return _active;
        },
        hitAreas: {
            TOP: "t",
            BOTTOM: "b",
            RIGHT: "r",
            LEFT: "l",
            TOPLEFT: "tl",
            TOPRIGHT: "tr",
            BOTTOMLEFT: "bl",
            BOTTOMRIGHT: "br",
            CENTER: "c",
            isCorner: function isCorner(area) {
                return area === this.TOPRIGHT || area === this.TOPLEFT || area === this.BOTTOMLEFT || area === this.BOTTOMRIGHT;
            },
            isEdge: function isEdge(area) {
                return area === this.TOP || area === this.BOTTOM || area === this.LEFT || area === this.RIGHT;
            },
            getCursor: function getCursor(area) {
                var rotated = osViewer.viewer.viewport.getRotation() % 180 === 90;
                if (area === this.TOPLEFT || area === this.BOTTOMRIGHT) {
                    return rotated ? "nesw-resize" : "nwse-resize";
                } else if (area === this.TOPRIGHT || area === this.BOTTOMLEFT) {
                    return rotated ? "nwse-resize" : "nesw-resize";
                } else if (area === this.TOP || area === this.BOTTOM) {
                    return rotated ? "ew-resize" : "ns-resize";
                } else if (area === this.RIGHT || area === this.LEFT) {
                    return rotated ? "ns-resize" : "ew-resize";
                } else if (area === this.CENTER) {
                    return "move";
                } else {
                    return DEFAULT_CURSOR;
                }
            }
        }
    };

    function _onViewerMove(event) {
        if (!_drawing && _active) {
            var drawPoint = osViewer.viewer.viewport.viewerElementToViewportCoordinates(event.position);
            drawPoint = osViewer.overlays.getRotated(drawPoint);
            var overlayRect = osViewer.overlays.getDrawingOverlay().rect;
            var overlayElement = osViewer.overlays.getDrawingOverlay().element;
            var viewerElement = osViewer.viewer.element;
            var area = _findCorner(overlayRect, drawPoint, _sideClickPrecision);
            if (!area) {
                area = _findEdge(overlayRect, drawPoint, _sideClickPrecision);
            }
            if (!area && osViewer.overlays.contains(overlayRect, drawPoint, 0)) {
                area = osViewer.transformRect.hitAreas.CENTER;
            }
            if (area) {
                $(viewerElement).css({
                    cursor: osViewer.transformRect.hitAreas.getCursor(area)
                });
            } else {
                $(viewerElement).css({
                    cursor: DEFAULT_CURSOR
                });
            }
            event.preventDefaultAction = true;
            return true;
        }
    }

    function _onViewerPress(event) {
        if (_active) {
            if (!osViewer.overlays.getDrawingOverlay()) {
                return false;
            }
            var overlayRect = osViewer.overlays.getDrawingOverlay().rect;
            var overlayElement = osViewer.overlays.getDrawingOverlay().element;
            var drawPoint = osViewer.viewer.viewport.viewerElementToViewportCoordinates(event.position);
            drawPoint = osViewer.overlays.getRotated(drawPoint);
            var drawArea = _findCorner(overlayRect, drawPoint, _sideClickPrecision);
            if (!drawArea) {
                drawArea = _findEdge(overlayRect, drawPoint, _sideClickPrecision);
            }
            if (!drawArea && osViewer.overlays.contains(overlayRect, drawPoint, 0)) {
                drawArea = osViewer.transformRect.hitAreas.CENTER;
            }
            if (_debug) console.log("draw area = " + drawArea);
            if (drawArea) {
                $(overlayElement).tooltip('destroy');
                _enterPoint = drawPoint;
            }
            _drawArea = drawArea;
            event.preventDefaultAction = true;
            return true;
        }
    }

    function _onViewerDrag(event) {
        if (_drawing) {
            var newPoint = osViewer.viewer.viewport.viewerElementToViewportCoordinates(event.position);
            newPoint = osViewer.overlays.getRotated(newPoint);
            var rect = osViewer.overlays.getDrawingOverlay().rect;
            var topLeft;
            var bottomRight;
            // if(_debug)console.log("Draw location = " + newPoint);
            if (_drawArea === osViewer.transformRect.hitAreas.TOPLEFT) {
                topLeft = new OpenSeadragon.Point(Math.min(newPoint.x, rect.getBottomRight().x), Math.min(newPoint.y, rect.getBottomRight().y));
                bottomRight = rect.getBottomRight();
            } else if (_drawArea === osViewer.transformRect.hitAreas.TOPRIGHT) {
                topLeft = new OpenSeadragon.Point(rect.getTopLeft().x, Math.min(newPoint.y, rect.getBottomRight().y));
                bottomRight = new OpenSeadragon.Point(Math.max(newPoint.x, rect.getTopLeft().x), rect.getBottomRight().y);
            } else if (_drawArea === osViewer.transformRect.hitAreas.BOTTOMLEFT) {
                topLeft = new OpenSeadragon.Point(Math.min(newPoint.x, rect.getBottomRight().x), rect.getTopLeft().y);
                bottomRight = new OpenSeadragon.Point(rect.getBottomRight().x, Math.max(newPoint.y, rect.getTopLeft().y));
            } else if (_drawArea === osViewer.transformRect.hitAreas.BOTTOMRIGHT) {
                topLeft = rect.getTopLeft();
                bottomRight = new OpenSeadragon.Point(Math.max(newPoint.x, rect.getTopLeft().x), Math.max(newPoint.y, rect.getTopLeft().y));
            } else if (_drawArea === osViewer.transformRect.hitAreas.LEFT) {
                topLeft = new OpenSeadragon.Point(Math.min(newPoint.x, rect.getBottomRight().x), rect.getTopLeft().y);
                bottomRight = rect.getBottomRight();
            } else if (_drawArea === osViewer.transformRect.hitAreas.RIGHT) {
                topLeft = rect.getTopLeft();
                bottomRight = new OpenSeadragon.Point(Math.max(newPoint.x, rect.getTopLeft().x), rect.getBottomRight().y);
            } else if (_drawArea === osViewer.transformRect.hitAreas.TOP) {
                topLeft = new OpenSeadragon.Point(rect.getTopLeft().x, Math.min(newPoint.y, rect.getBottomRight().y));
                bottomRight = rect.getBottomRight();
            } else if (_drawArea === osViewer.transformRect.hitAreas.BOTTOM) {
                topLeft = rect.getTopLeft();
                bottomRight = new OpenSeadragon.Point(rect.getBottomRight().x, Math.max(newPoint.y, rect.getTopLeft().y));
            } else if (_drawArea === osViewer.transformRect.hitAreas.CENTER && _enterPoint) {
                var dx = _enterPoint.x - newPoint.x;
                var dy = _enterPoint.y - newPoint.y;
                rect.x -= dx;
                rect.y -= dy;
                _enterPoint = newPoint;
            }

            if (topLeft && bottomRight) {
                // if(_debug)console.log("Upper left point is " + rect.getTopLeft());
                // if(_debug)console.log("Lower right point is " + rect.getBottomRight());
                // if(_debug)console.log("Setting upper left point to " + topLeft);
                // if(_debug)console.log("Setting lower right point to " + bottomRight);
                rect.x = topLeft.x;
                rect.y = topLeft.y;
                rect.width = bottomRight.x - topLeft.x;
                rect.height = bottomRight.y - topLeft.y;
            }

            osViewer.viewer.updateOverlay(osViewer.overlays.getDrawingOverlay().element, rect, 0);
            event.preventDefaultAction = true;
            return true;
        } else if (_drawArea) {
            _drawing = true;
            event.preventDefaultAction = true;
            return true;
        }
    }

    function _onViewerRelease(event) {
        if (_active) {
            if (_drawing && _finishHook) {
                _finishHook(osViewer.overlays.getDrawingOverlay());
            }
            _drawing = false;
            if (osViewer.overlays.getDrawingOverlay()) {
                $(osViewer.overlays.getDrawingOverlay().element).tooltip();
            }
            _drawArea = "";
            _enterPoint = null;
            event.preventDefaultAction = true;
            return true;
        }
    }

    function _onViewerDragEnd(event) {
        if (_drawing) {
            _drawing = false;
            event.preventDefaultAction = true;
            return true;
        }
    }

    function _disableViewerEvent(event) {
        if (_drawing) {
            event.preventDefaultAction = true;
            return true;
        }
    }
    function checkForRectHit(point) {
        var i;
        for (i = 0; i < _rects.length; i++) {
            var x = _rects[i];
            if (point.x > x.hitBox.l && point.x < x.hitBox.r && point.y > x.hitBox.t && point.y < x.hitBox.b) {
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

    /*
     * Determine the side of the rectangle rect the point lies on or closest at <=maxDist
     * distance
     */
    function _findEdge(rect, point, maxDist) {
        var distanceToLeft = _distToSegment(point, rect.getTopLeft(), rect.getBottomLeft());
        var distanceToBottom = _distToSegment(point, rect.getBottomLeft(), rect.getBottomRight());
        var distanceToRight = _distToSegment(point, rect.getTopRight(), rect.getBottomRight());
        var distanceToTop = _distToSegment(point, rect.getTopLeft(), rect.getTopRight());

        var minDistance = Math.min(distanceToLeft, Math.min(distanceToRight, Math.min(distanceToTop, distanceToBottom)));
        if (minDistance <= maxDist) {
            if (distanceToLeft === minDistance) {
                return osViewer.transformRect.hitAreas.LEFT;
            }
            if (distanceToRight === minDistance) {
                return osViewer.transformRect.hitAreas.RIGHT;
            }
            if (distanceToTop === minDistance) {
                return osViewer.transformRect.hitAreas.TOP;
            }
            if (distanceToBottom === minDistance) {
                return osViewer.transformRect.hitAreas.BOTTOM;
            }
        }
        return "";
    }

    /*
     * Determine the cornder of the rectangle rect the point lies on or closest at
     * <=maxDist distance
     */
    function _findCorner(rect, point, maxDist) {
        var distanceToTopLeft = _dist(point, rect.getTopLeft());
        var distanceToBottomLeft = _dist(point, rect.getBottomLeft());
        var distanceToTopRight = _dist(point, rect.getTopRight());
        var distanceToBottomRight = _dist(point, rect.getBottomRight());

        var minDistance = Math.min(distanceToTopLeft, Math.min(distanceToTopRight, Math.min(distanceToBottomLeft, distanceToBottomRight)));
        if (minDistance <= maxDist) {
            if (distanceToTopLeft === minDistance) {
                return osViewer.transformRect.hitAreas.TOPLEFT;
            }
            if (distanceToTopRight === minDistance) {
                return osViewer.transformRect.hitAreas.TOPRIGHT;
            }
            if (distanceToBottomLeft === minDistance) {
                return osViewer.transformRect.hitAreas.BOTTOMLEFT;
            }
            if (distanceToBottomRight === minDistance) {
                return osViewer.transformRect.hitAreas.BOTTOMRIGHT;
            }
        }
        return "";
    }

    function _sqr(x) {
        return x * x;
    }
    function _dist2(v, w) {
        return _sqr(v.x - w.x) + _sqr(v.y - w.y);
    }
    function _dist(v, w) {
        return Math.sqrt(_dist2(v, w));
    }
    function _distToSegmentSquared(p, v, w) {
        var l2 = _dist2(v, w);
        if (l2 == 0) return _dist2(p, v);
        var t = ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2;
        if (t < 0) return _dist2(p, v);
        if (t > 1) return _dist2(p, w);
        return _dist2(p, {
            x: v.x + t * (w.x - v.x),
            y: v.y + t * (w.y - v.y)
        });
    }
    function _distToSegment(point, lineP1, lineP2) {
        return Math.sqrt(_distToSegmentSquared(point, lineP1, lineP2));
    }
    return osViewer;
}(viewImage || {}, jQuery);

},{}],11:[function(require,module,exports){
'use strict';

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
 * Module which handles the zoomslider functionality.
 * 
 * @version 3.2.0
 * @module viewImage.zoomSlider
 * @requires jQuery
 */
var viewImage = function (osViewer) {
    'use strict';

    var _debug = false;
    var _zoomSlider = {};
    var _defaults = {
        global: {
            /**
             * The position of the zoom-slider is "dilated" by a function d(zoom) =
             * 1/sliderDilation*tan[atan(sliderDilation)*zoom] This makes the slider
             * position change slower for small zoom and faster for larger zoom The
             * function is chosen so that d(0) = 0 and d(1) = 1
             */
            sliderDilation: 12
        }
    };

    osViewer.zoomSlider = {
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('osViewer.zoomSlider.init');
                console.log('##############################');
            }

            $.extend(true, _defaults, config);

            if ($(_defaults.global.zoomSlider)) {
                osViewer.zoomSlider.addZoomSlider(_defaults.global.zoomSlider);

                // handler for openSeadragon Object
                osViewer.viewer.addHandler('zoom', function (data) {
                    osViewer.zoomSlider.buttonToZoom(data.zoom);
                });
            }
        },
        buttonToMouse: function buttonToMouse(mousePos) {
            if (_debug) {
                console.log('osViewer.zoomSlider.buttonToMouse: mousePos - ' + mousePos);
            }

            var offset = _zoomSlider.$button.width() / 2;
            var newPos = mousePos - offset;
            if (newPos < 0) {
                newPos = 0;
            }
            if (newPos + 2 * offset > _zoomSlider.absoluteWidth) {
                newPos = _zoomSlider.absoluteWidth - 2 * offset;
            }
            _zoomSlider.$button.css({
                left: newPos
            });
            _zoomSlider.buttonPosition = newPos;
            var factor = newPos / (_zoomSlider.absoluteWidth - offset * 2);
            factor = 1 / _defaults.global.sliderDilation * Math.tan(Math.atan(_defaults.global.sliderDilation) * factor);

            var newScale = osViewer.viewer.viewport.getMinZoom() + (osViewer.viewer.viewport.getMaxZoom() - osViewer.viewer.viewport.getMinZoom()) * factor;

            if (_debug) {
                console.log('osViewer.zoomSlider.buttonToMouse: newScale - ' + newScale);
            }

            osViewer.controls.zoomTo(newScale);
        },
        buttonToZoom: function buttonToZoom(scale) {
            if (_debug) {
                console.log('osViewer.zoomSlider.buttonToZoom: scale - ' + scale);
            }

            if (!_zoomSlider || !osViewer.viewer.viewport) {
                return;
            }

            // console.log("Dilation = ", osViewer.viewer.viewport.getMinZoom())
            // console.log("minZoom = ", osViewer.viewer.viewport.getMinZoom());
            // console.log("maxZoom = ", osViewer.viewer.viewport.getMaxZoom())
            // console.log("scale = ", scale);

            var factor = (scale - osViewer.viewer.viewport.getMinZoom()) / (osViewer.viewer.viewport.getMaxZoom() - osViewer.viewer.viewport.getMinZoom());
            // console.log( "factor = ", factor );
            //            
            factor = 1 / Math.atan(_defaults.global.sliderDilation) * Math.atan(_defaults.global.sliderDilation * factor);
            var newPos = factor * (_zoomSlider.absoluteWidth - _zoomSlider.$button.width());
            // var newPos = ( ( scale - osViewer.viewer.viewport.getMinZoom() ) / (
            // osViewer.viewer.viewport.getMaxZoom() -
            // osViewer.viewer.viewport.getMinZoom() ) )
            // * ( _zoomSlider.absoluteWidth - _zoomSlider.$button.width() );
            // console.log( "pos = ", newPos );

            if (Math.abs(osViewer.viewer.viewport.getMaxZoom() - scale) < 0.0000000001) {
                newPos = _zoomSlider.absoluteWidth - _zoomSlider.$button.width();
            }

            if (newPos < 0) {
                newPos = 0;
            }

            _zoomSlider.$button.css({
                left: newPos
            });
            _zoomSlider.buttonPosition = newPos;
        },
        zoomSliderMouseUp: function zoomSliderMouseUp() {
            if (_debug) {
                console.log('osViewer.zoomSlider.zoomSliderMouseUp');
            }

            _zoomSlider.mousedown = false;
        },
        zoomSliderMouseMove: function zoomSliderMouseMove(evt) {
            if (_debug) {
                console.log('osViewer.zoomSlider.zoomSliderMouseMove: evt - ' + evt);
            }

            if (!_zoomSlider.mousedown) {
                return;
            }
            var offset = $(this).offset();
            var hitX = evt.pageX - offset.left;
            osViewer.zoomSlider.buttonToMouse(hitX);

            if (_debug) {
                console.log('osViewer.zoomSlider.zoomSliderMouseMove: moving - ' + hitX);
            }
        },
        zoomSliderMouseDown: function zoomSliderMouseDown(evt) {
            if (_debug) {
                console.log('osViewer.zoomSlider.zoomSliderMouseDown: evt - ' + evt);
            }

            _zoomSlider.mousedown = true;
            var offset = $(this).offset();
            var hitX = evt.pageX - offset.left;
            osViewer.zoomSlider.buttonToMouse(hitX);
        },
        buttonMouseDown: function buttonMouseDown() {
            if (_debug) {
                console.log('osViewer.zoomSlider.buttonMouseDown');
            }

            _zoomSlider.mousedown = true;

            return false;
        },
        addZoomSlider: function addZoomSlider(element) {
            if (_debug) {
                console.log('osViewer.zoomSlider.addZoomSlider: element - ' + element);
            }

            _zoomSlider.$element = $(element);
            _zoomSlider.$button = _zoomSlider.$element.children(_defaults.global.zoomSliderHandle);
            _zoomSlider.buttonPosition = 0;
            _zoomSlider.absoluteWidth = _zoomSlider.$element.innerWidth();
            _zoomSlider.mousedown = false;
            _zoomSlider.$button.on('mousedown', osViewer.zoomSlider.buttonMouseDown);
            _zoomSlider.$element.click(osViewer.zoomSlider._zoomSliderClick);
            _zoomSlider.$element.mousedown(osViewer.zoomSlider.zoomSliderMouseDown);
            _zoomSlider.$element.mousemove(osViewer.zoomSlider.zoomSliderMouseMove);
            $(document).on('mouseup', osViewer.zoomSlider.zoomSliderMouseUp);
        }
    };

    return osViewer;
}(viewImage || {}, jQuery);

},{}]},{},[1,2,3,4,5,6,7,8,9,10,11])
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIm5vZGVfbW9kdWxlcy9icm93c2VyLXBhY2svX3ByZWx1ZGUuanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9tb2R1bGVzL3ZpZXdJbWFnZS92aWV3SW1hZ2UuY29udHJvbHMuanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9tb2R1bGVzL3ZpZXdJbWFnZS92aWV3SW1hZ2UuY29udHJvbHMucGVyc2lzdGVuY2UuanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9tb2R1bGVzL3ZpZXdJbWFnZS92aWV3SW1hZ2UuZHJhd0xpbmUuanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9tb2R1bGVzL3ZpZXdJbWFnZS92aWV3SW1hZ2UuZHJhd1JlY3QuanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9tb2R1bGVzL3ZpZXdJbWFnZS92aWV3SW1hZ2UuanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9tb2R1bGVzL3ZpZXdJbWFnZS92aWV3SW1hZ2UubWVhc3VyZXMuanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9tb2R1bGVzL3ZpZXdJbWFnZS92aWV3SW1hZ2Uub3ZlcmxheXMuanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9tb2R1bGVzL3ZpZXdJbWFnZS92aWV3SW1hZ2UucmVhZGluZ01vZGUuanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9tb2R1bGVzL3ZpZXdJbWFnZS92aWV3SW1hZ2UudGlsZVNvdXJjZVJlc29sdmVyLmpzIiwiV2ViQ29udGVudC9yZXNvdXJjZXMvamF2YXNjcmlwdC9kZXYvbW9kdWxlcy92aWV3SW1hZ2Uvdmlld0ltYWdlLnRyYW5zZm9ybVJlY3QuanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rl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QWlCLGdCQUFoRCxDQUF0QjtBQUNBLHdCQUFZLGNBQVosR0FBNkIsQ0FBN0I7QUFDQSx3QkFBWSxhQUFaLEdBQTRCLFlBQVksUUFBWixDQUFxQixVQUFyQixFQUE1QjtBQUNBLHdCQUFZLFNBQVosR0FBd0IsS0FBeEI7QUFDQSx3QkFBWSxPQUFaLENBQW9CLEVBQXBCLENBQXdCLFdBQXhCLEVBQXFDLFNBQVMsVUFBVCxDQUFvQixlQUF6RDtBQUNBLHdCQUFZLFFBQVosQ0FBcUIsS0FBckIsQ0FBNEIsU0FBUyxVQUFULENBQW9CLGdCQUFoRDtBQUNBLHdCQUFZLFFBQVosQ0FBcUIsU0FBckIsQ0FBZ0MsU0FBUyxVQUFULENBQW9CLG1CQUFwRDtBQUNBLHdCQUFZLFFBQVosQ0FBcUIsU0FBckIsQ0FBZ0MsU0FBUyxVQUFULENBQW9CLG1CQUFwRDtBQUNBLGNBQUcsUUFBSCxFQUFjLEVBQWQsQ0FBa0IsU0FBbEIsRUFBNkIsU0FBUyxVQUFULENBQW9CLGlCQUFqRDtBQUNIO0FBOUlpQixLQUF0Qjs7QUFpSkEsV0FBTyxRQUFQO0FBRUgsQ0FwS2UsQ0FvS1gsYUFBYSxFQXBLRixFQW9LTSxNQXBLTixDQUFoQiIsImZpbGUiOiJnZW5lcmF0ZWQuanMiLCJzb3VyY2VSb290IjoiIiwic291cmNlc0NvbnRlbnQiOlsiKGZ1bmN0aW9uKCl7ZnVuY3Rpb24gZSh0LG4scil7ZnVuY3Rpb24gcyhvLHUpe2lmKCFuW29dKXtpZighdFtvXSl7dmFyIGE9dHlwZW9mIHJlcXVpcmU9PVwiZnVuY3Rpb25cIiYmcmVxdWlyZTtpZighdSYmYSlyZXR1cm4gYShvLCEwKTtpZihpKXJldHVybiBpKG8sITApO3ZhciBmPW5ldyBFcnJvcihcIkNhbm5vdCBmaW5kIG1vZHVsZSAnXCIrbytcIidcIik7dGhyb3cgZi5jb2RlPVwiTU9EVUxFX05PVF9GT1VORFwiLGZ9dmFyIGw9bltvXT17ZXhwb3J0czp7fX07dFtvXVswXS5jYWxsKGwuZXhwb3J0cyxmdW5jdGlvbihlKXt2YXIgbj10W29dWzFdW2VdO3JldHVybiBzKG4/bjplKX0sbCxsLmV4cG9ydHMsZSx0LG4scil9cmV0dXJuIG5bb10uZXhwb3J0c312YXIgaT10eXBlb2YgcmVxdWlyZT09XCJmdW5jdGlvblwiJiZyZXF1aXJlO2Zvcih2YXIgbz0wO288ci5sZW5ndGg7bysrKXMocltvXSk7cmV0dXJuIHN9cmV0dXJuIGV9KSgpIiwiLyoqXG4gKiBUaGlzIGZpbGUgaXMgcGFydCBvZiB0aGUgR29vYmkgdmlld2VyIC0gYSBjb250ZW50IHByZXNlbnRhdGlvbiBhbmQgbWFuYWdlbWVudFxuICogYXBwbGljYXRpb24gZm9yIGRpZ2l0aXplZCBvYmplY3RzLlxuICogXG4gKiBWaXNpdCB0aGVzZSB3ZWJzaXRlcyBmb3IgbW9yZSBpbmZvcm1hdGlvbi4gLSBodHRwOi8vd3d3LmludHJhbmRhLmNvbSAtXG4gKiBodHRwOi8vZGlnaXZlcnNvLmNvbVxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZnJlZSBzb2Z0d2FyZTsgeW91IGNhbiByZWRpc3RyaWJ1dGUgaXQgYW5kL29yIG1vZGlmeSBpdCB1bmRlciB0aGUgdGVybXNcbiAqIG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhcyBwdWJsaXNoZWQgYnkgdGhlIEZyZWUgU29mdHdhcmUgRm91bmRhdGlvbjsgZWl0aGVyXG4gKiB2ZXJzaW9uIDIgb2YgdGhlIExpY2Vuc2UsIG9yIChhdCB5b3VyIG9wdGlvbikgYW55IGxhdGVyIHZlcnNpb24uXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBkaXN0cmlidXRlZCBpbiB0aGUgaG9wZSB0aGF0IGl0IHdpbGwgYmUgdXNlZnVsLCBidXQgV0lUSE9VVCBBTllcbiAqIFdBUlJBTlRZOyB3aXRob3V0IGV2ZW4gdGhlIGltcGxpZWQgd2FycmFudHkgb2YgTUVSQ0hBTlRBQklMSVRZIG9yIEZJVE5FU1MgRk9SIEFcbiAqIFBBUlRJQ1VMQVIgUFVSUE9TRS4gU2VlIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBmb3IgbW9yZSBkZXRhaWxzLlxuICogXG4gKiBZb3Ugc2hvdWxkIGhhdmUgcmVjZWl2ZWQgYSBjb3B5IG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhbG9uZyB3aXRoIHRoaXNcbiAqIHByb2dyYW0uIElmIG5vdCwgc2VlIDxodHRwOi8vd3d3LmdudS5vcmcvbGljZW5zZXMvPi5cbiAqIFxuICogTW9kdWxlIHdoaWNoIGhhbmRsZXMgdGhlIGltYWdlIGNvbnRyb2xzIG9mIHRoZSBpbWFnZSB2aWV3LlxuICogXG4gKiBAdmVyc2lvbiAzLjIuMFxuICogQG1vZHVsZSB2aWV3SW1hZ2UuY29udHJvbHNcbiAqIEByZXF1aXJlcyBqUXVlcnlcbiAqL1xudmFyIHZpZXdJbWFnZSA9ICggZnVuY3Rpb24oIG9zVmlld2VyICkge1xuICAgICd1c2Ugc3RyaWN0JztcbiAgICBcbiAgICB2YXIgX2RlYnVnID0gZmFsc2U7XG4gICAgdmFyIF9jdXJyZW50Wm9vbTtcbiAgICB2YXIgX3pvb21lZE91dCA9IHRydWU7XG4gICAgdmFyIF9wYW5uaW5nID0gZmFsc2U7XG4gICAgdmFyIF9mYWRlb3V0ID0gbnVsbDtcbiAgICAgIFxuICAgIG9zVmlld2VyLmNvbnRyb2xzID0ge1xuICAgICAgICBpbml0OiBmdW5jdGlvbiggY29uZmlnICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICcjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICdvc1ZpZXdlci5jb250cm9scy5pbml0JyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjJyApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmKG9zVmlld2VyLmNvbnRyb2xzLnBlcnNpc3RlbmNlKSB7XG4gICAgICAgICAgICAgICAgb3NWaWV3ZXIuY29udHJvbHMucGVyc2lzdGVuY2UuaW5pdChjb25maWcpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaWYoX2RlYnVnKSB7ICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKFwiU2V0dGluZyB2aWV3ZXIgbG9jYXRpb24gdG9cIiwgY29uZmlnLmltYWdlLmxvY2F0aW9uKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgaWYoIG9zVmlld2VyLm9ic2VydmFibGVzICkge1xuICAgICAgICAgICAgICAgIC8vIHNldCBsb2NhdGlvbiBhZnRlciB2aWV3cG9ydCB1cGRhdGVcbiAgICAgICAgICAgICAgICBvc1ZpZXdlci5vYnNlcnZhYmxlcy5yZWRyYXdSZXF1aXJlZFxuICAgICAgICAgICAgICAgIC5zYW1wbGUob3NWaWV3ZXIub2JzZXJ2YWJsZXMudmlld3BvcnRVcGRhdGUpXG4gICAgICAgICAgICAgICAgLmZpbHRlcihmdW5jdGlvbihldmVudCkge3JldHVybiBvc1ZpZXdlci5jb250cm9scyA/IHRydWUgOiBmYWxzZX0pXG4gICAgICAgICAgICAgICAgLnN1YnNjcmliZShmdW5jdGlvbihldmVudCkge1xuICAgICAgICAgICAgICAgICAgICBzZXRMb2NhdGlvbihldmVudCwgb3NWaWV3ZXIpXG4gICAgICAgICAgICAgICAgICAgIG9zVmlld2VyLmNvbnRyb2xzLnNldFBhbm5pbmcoIGZhbHNlICk7XG4gICAgICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLy8gem9vbSBob21lIGlmIG1pbiB6b29tIHJlYWNoZWRcbiAgICAgICAgICAgICAgICBvc1ZpZXdlci5vYnNlcnZhYmxlcy52aWV3ZXJab29tLnN1YnNjcmliZSggZnVuY3Rpb24oIGV2ZW50ICkge1xuICAgICAgICAgICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCBcInpvb20gdG8gXCIgKyBvc1ZpZXdlci52aWV3ZXIudmlld3BvcnQuZ2V0Wm9vbSggdHJ1ZSApICk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgaWYgKCAhb3NWaWV3ZXIuY29udHJvbHMuaXNQYW5uaW5nKCkgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICB2YXIgY3VycmVudFpvb20gPSBvc1ZpZXdlci52aWV3ZXIudmlld3BvcnQuZ2V0Wm9vbSgpOyAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICggY3VycmVudFpvb20gPD0gb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0Lm1pblpvb21MZXZlbCApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgY29uc29sZS5sb2coIFwiWm9vbWVkIG91dDogUGFubmluZyBob21lXCIgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgb3NWaWV3ZXIuY29udHJvbHMuc2V0UGFubmluZyh0cnVlKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBvc1ZpZXdlci5jb250cm9scy5nb0hvbWUoIHRydWUgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBvc1ZpZXdlci5jb250cm9scy5zZXRQYW5uaW5nKGZhbHNlKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gZmFkZSBvdXQgZnVsbHNjcmVlbiBjb250cm9sc1xuICAgICAgICAgICAgaWYgKCAkKCAnI2Z1bGxzY3JlZW5UZW1wbGF0ZScgKS5sZW5ndGggPiAwICkge1xuICAgICAgICAgICAgICAgICQoICcjZnVsbHNjcmVlblRlbXBsYXRlJyApLm9uKCAnbW91c2Vtb3ZlJywgZnVuY3Rpb24oKSB7ICBcbiAgICAgICAgICAgICAgICAgICAgb3NWaWV3ZXIuY29udHJvbHMuZnVsbHNjcmVlbkNvbnRyb2xzRmFkZW91dCgpO1xuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSxcbiAgICAgICAgZ2V0TG9jYXRpb246IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgcmV0dXJuIHtcbiAgICAgICAgICAgICAgICB4OiBvc1ZpZXdlci5jb250cm9scy5nZXRDZW50ZXIoKS54LFxuICAgICAgICAgICAgICAgIHk6IG9zVmlld2VyLmNvbnRyb2xzLmdldENlbnRlcigpLnksXG4gICAgICAgICAgICAgICAgem9vbTogb3NWaWV3ZXIuY29udHJvbHMuZ2V0Wm9vbSgpL29zVmlld2VyLmNvbnRyb2xzLmdldEN1cnJlbnRSb3RhdGlvblpvb21pbmcoKSxcbiAgICAgICAgICAgICAgICByb3RhdGlvbjogb3NWaWV3ZXIuY29udHJvbHMuZ2V0Um90YXRpb24oKSxcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSxcbiAgICAgICAgZ2V0Q2VudGVyOiBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCBcImltYWdlIGNlbnRlciBpcyBcIiArIG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC5nZXRDZW50ZXIoIHRydWUgKSApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgcmV0dXJuIG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC5nZXRDZW50ZXIoIHRydWUgKTtcbiAgICAgICAgfSxcbiAgICAgICAgc2V0Q2VudGVyOiBmdW5jdGlvbiggY2VudGVyICkge1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggXCJTZXR0aW5nIGltYWdlIGNlbnRlciB0byBcIiApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCBjZW50ZXIgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0LnBhblRvKCBjZW50ZXIsIHRydWUgKTtcbiAgICAgICAgICAgIFxuICAgICAgICB9LFxuICAgICAgICBnZXRab29tOiBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnb3NWaWV3ZXIuY29udHJvbHMuZ2V0Wm9vbScgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJldHVybiBvc1ZpZXdlci52aWV3ZXIudmlld3BvcnQuZ2V0Wm9vbSggdHJ1ZSApO1xuICAgICAgICB9LFxuICAgICAgICB6b29tVG86IGZ1bmN0aW9uKCB6b29tVG8gKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ29zVmlld2VyLmNvbnRyb2xzLm15Wm9vbVRvOiB6b29tVG8gLSAnICsgem9vbVRvICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHZhciB6b29tQnkgPSBwYXJzZUZsb2F0KCB6b29tVG8gKSAvIG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC5nZXRab29tKCk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnb3NWaWV3ZXIuY29udHJvbHMubXlab29tVG86IHpvb21CeSAtICcgKyB6b29tQnkgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0Lnpvb21CeSggem9vbUJ5LCBvc1ZpZXdlci52aWV3ZXIudmlld3BvcnQuZ2V0Q2VudGVyKCBmYWxzZSApLCB0cnVlICk7XG4gICAgICAgIH0sXG4gICAgICAgIHNldEZ1bGxTY3JlZW46IGZ1bmN0aW9uKCBlbmFibGUgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ29zVmlld2VyLmNvbnRyb2xzLnNldEZ1bGxTY3JlZW46IGVuYWJsZSAtICcgKyBlbmFibGUgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgb3NWaWV3ZXIudmlld2VyLnNldEZ1bGxTY3JlZW4oIGVuYWJsZSApO1xuICAgICAgICB9LFxuICAgICAgICBnb0hvbWU6IGZ1bmN0aW9uKCBpbW1lZGlhdGUgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ29zVmlld2VyLmNvbnRyb2xzLnBhbkhvbWUgLSB6b29tIDogJyArIG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC5nZXRIb21lWm9vbSgpICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBvc1ZpZXdlci52aWV3ZXIudmlld3BvcnQuZ29Ib21lKCBpbW1lZGlhdGUgKTtcbiAgICAgICAgICAgIF96b29tZWRPdXQgPSB0cnVlO1xuICAgICAgICB9LFxuICAgICAgICByZXNldDogZnVuY3Rpb24oIHJlc2V0Um90YXRpb24gKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ29zVmlld2VyLmNvbnRyb2xzLmdvSG9tZTogYm9vbCAtICcgKyByZXNldFJvdGF0aW9uICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC5nb0hvbWUoIHRydWUgKTtcbiAgICAgICAgICAgIG9zVmlld2VyLmNvbnRyb2xzLmdvSG9tZSggdHJ1ZSApO1xuICAgICAgICAgICAgb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0Lnpvb21Ubyggb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0LmdldEhvbWVab29tKCksIG51bGwsIHRydWUgKTtcbiAgICAgICAgICAgIGlmICggcmVzZXRSb3RhdGlvbiApIHtcbiAgICAgICAgICAgICAgICBvc1ZpZXdlci5jb250cm9scy5yb3RhdGVUbyggMCApO1xuICAgICAgICAgICAgfVxuICAgICAgICB9LFxuICAgICAgICB6b29tSW46IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICdvc1ZpZXdlci5jb250cm9scy56b29tSW46IHpvb21TcGVlZCAtICcgKyBvc1ZpZXdlci5nZXRDb25maWcoKS5nbG9iYWwuem9vbVNwZWVkICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC56b29tQnkoIG9zVmlld2VyLmdldENvbmZpZygpLmdsb2JhbC56b29tU3BlZWQsIG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC5nZXRDZW50ZXIoIGZhbHNlICksIGZhbHNlICk7XG4gICAgICAgIH0sXG4gICAgICAgIHpvb21PdXQ6IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICdvc1ZpZXdlci5jb250cm9scy56b29tT3V0OiB6b29tU3BlZWQgLSAnICsgb3NWaWV3ZXIuZ2V0Q29uZmlnKCkuZ2xvYmFsLnpvb21TcGVlZCApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICBvc1ZpZXdlci52aWV3ZXIudmlld3BvcnQuem9vbUJ5KCAxIC8gb3NWaWV3ZXIuZ2V0Q29uZmlnKCkuZ2xvYmFsLnpvb21TcGVlZCwgb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0LmdldENlbnRlciggZmFsc2UgKSwgZmFsc2UgKTtcbiAgICAgICAgfSxcbiAgICAgICAgZ2V0SG9tZVpvb206IGZ1bmN0aW9uKCByb3RhdGVkICkge1xuICAgICAgICAgICAgaWYgKCByb3RhdGVkICYmIG9zVmlld2VyLmdldENhbnZhc1NpemUoKS54IC8gb3NWaWV3ZXIuZ2V0Q2FudmFzU2l6ZSgpLnkgPD0gb3NWaWV3ZXIuZ2V0SW1hZ2VTaXplKCkueCAvIG9zVmlld2VyLmdldEltYWdlU2l6ZSgpLnkgKSB7XG4gICAgICAgICAgICAgICAgb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0LmhvbWVGaWxsc1ZpZXdlciA9IHRydWU7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICB2YXIgem9vbSA9IG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC5nZXRIb21lWm9vbSgpO1xuICAgICAgICAgICAgb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0LmhvbWVGaWxsc1ZpZXdlciA9IGZhbHNlO1xuICAgICAgICAgICAgcmV0dXJuIHpvb207XG4gICAgICAgIH0sXG4gICAgICAgIHJvdGF0ZVJpZ2h0OiBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnb3NWaWV3ZXIuY29udHJvbHMucm90YXRlUmlnaHQnICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHZhciBuZXdSb3RhdGlvbiA9IG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC5nZXRSb3RhdGlvbigpICsgOTA7XG4gICAgICAgICAgICBvc1ZpZXdlci5jb250cm9scy5yb3RhdGVUbyggbmV3Um90YXRpb24gKTtcbiAgICAgICAgfSxcbiAgICAgICAgcm90YXRlTGVmdDogZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ29zVmlld2VyLmNvbnRyb2xzLnJvdGF0ZUxlZnQnICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHZhciBuZXdSb3RhdGlvbiA9IG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC5nZXRSb3RhdGlvbigpIC0gOTA7XG4gICAgICAgICAgICBvc1ZpZXdlci5jb250cm9scy5yb3RhdGVUbyggbmV3Um90YXRpb24gKTtcbiAgICAgICAgfSxcbiAgICAgICAgZ2V0Um90YXRpb246IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICdvc1ZpZXdlci5jb250cm9scy5nZXRSb3RhdGlvbicgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgcmV0dXJuIG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC5nZXRSb3RhdGlvbigpO1xuICAgICAgICB9LFxuICAgICAgICBzZXRSb3RhdGlvbjogZnVuY3Rpb24oIHJvdGF0aW9uICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICdvc1ZpZXdlci5jb250cm9scy5zZXRSb3RhdGlvbjogcm90YXRpb24gLSAnICsgcm90YXRpb24gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgcmV0dXJuIG9zVmlld2VyLmNvbnRyb2xzLnJvdGF0ZVRvKCByb3RhdGlvbiApO1xuICAgICAgICB9LFxuICAgICAgICByb3RhdGVUbzogZnVuY3Rpb24oIG5ld1JvdGF0aW9uICkge1xuICAgICAgICAgICAgaWYgKCBuZXdSb3RhdGlvbiA8IDAgKSB7XG4gICAgICAgICAgICAgICAgbmV3Um90YXRpb24gPSBuZXdSb3RhdGlvbiArIDM2MDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIG5ld1JvdGF0aW9uID0gbmV3Um90YXRpb24gJSAzNjA7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ29zVmlld2VyLmNvbnRyb2xzLnJvdGF0ZVRvOiBuZXdSb3RhdGlvbiAtICcgKyBuZXdSb3RhdGlvbiApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICBfcGFubmluZyA9IHRydWU7ICAgICAgICBcbiAgICAgICAgICAgIF9jdXJyZW50Wm9vbSA9IG51bGw7XG4gICAgICAgICAgICBvc1ZpZXdlci52aWV3ZXIudmlld3BvcnQuc2V0Um90YXRpb24oIG5ld1JvdGF0aW9uICk7XG4gICAgICAgICAgICBfcGFubmluZyA9IGZhbHNlO1xuXG4gICAgICAgIH0sXG4gICAgICAgIGdldEN1cnJlbnRSb3RhdGlvblpvb21pbmc6IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgdmFyIHNpemVzID0gb3NWaWV3ZXIuZ2V0U2l6ZXMoKTtcbiAgICAgICAgICAgIGlmKHNpemVzICYmIHNpemVzLnJvdGF0ZWQoKSkge1xuICAgICAgICAgICAgICAgIHJldHVybiAxL3NpemVzLnJhdGlvKHNpemVzLm9yaWdpbmFsSW1hZ2VTaXplKTtcbiAgICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIDE7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0sXG4gICAgICAgIHNldFBhbm5pbmc6IGZ1bmN0aW9uKHBhbm5pbmcpIHtcbiAgICAgICAgICAgIF9wYW5uaW5nID0gcGFubmluZztcbiAgICAgICAgfSxcbiAgICAgICAgaXNQYW5uaW5nOiBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIHJldHVybiBfcGFubmluZztcbiAgICAgICAgfSxcbiAgICAgICAgZnVsbHNjcmVlbkNvbnRyb2xzRmFkZW91dDogZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gb3NWaWV3ZXIuY29udHJvbHMuZnVsbHNjcmVlbkNvbnRyb2xzRmFkZW91dCgpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmICggX2ZhZGVvdXQgKSB7XG4gICAgICAgICAgICAgICAgY2xlYXJUaW1lb3V0KCBfZmFkZW91dCApO1xuICAgICAgICAgICAgICAgIHRoaXMuc2hvd0Z1bGxzY3JlZW5Db250cm9scygpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICBfZmFkZW91dCA9IHNldFRpbWVvdXQoIHRoaXMuaGlkZUZ1bGxzY3JlZW5Db250cm9scywgMzAwMCApO1xuICAgICAgICB9LFxuICAgICAgICBoaWRlRnVsbHNjcmVlbkNvbnRyb2xzOiBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBvc1ZpZXdlci5jb250cm9scy5oaWRlRnVsbHNjcmVlbkNvbnRyb2xzKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgJCggJyNmdWxsc2NyZWVuUm90YXRlQ29udHJvbHNXcmFwcGVyLCAjZnVsbHNjcmVlblpvb21TbGlkZXJXcmFwcGVyLCAjZnVsbHNjcmVlbkV4aXRXcmFwcGVyLCAjZnVsbHNjcmVlblByZXZXcmFwcGVyLCAjZnVsbHNjcmVlbk5leHRXcmFwcGVyJyApLnN0b3AoKS5mYWRlT3V0KCAnc2xvdycgKTtcbiAgICAgICAgfSxcbiAgICAgICAgc2hvd0Z1bGxzY3JlZW5Db250cm9sczogZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gb3NWaWV3ZXIuY29udHJvbHMuc2hvd0Z1bGxzY3JlZW5Db250cm9scygpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgICQoICcjZnVsbHNjcmVlblJvdGF0ZUNvbnRyb2xzV3JhcHBlciwgI2Z1bGxzY3JlZW5ab29tU2xpZGVyV3JhcHBlciwgI2Z1bGxzY3JlZW5FeGl0V3JhcHBlciwgI2Z1bGxzY3JlZW5QcmV2V3JhcHBlciwgI2Z1bGxzY3JlZW5OZXh0V3JhcHBlcicgKS5zaG93KCk7XG4gICAgICAgIH1cbiAgICB9O1xuICAgIFxuICAgIFxuICAgIC8vIHNldCBjb3JyZWN0IGxvY2F0aW9uLCB6b29taW5nIGFuZCByb3RhdGlvbiBvbmNlIHZpZXdwb3J0IGhhcyBiZWVuIHVwZGF0ZWQgYWZ0ZXJcbiAgICAvLyByZWRyYXdcbiAgICBmdW5jdGlvbiBzZXRMb2NhdGlvbihldmVudCwgb3NWaWV3ZXIpIHtcbiAgICAgICAgaWYoX2RlYnVnKSB7ICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKFwiVmlld2VyIGNoYW5nZWQgZnJvbSBcIiArIGV2ZW50Lm9zU3RhdGUgKyBcIiBldmVudFwiKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKFwidGFyZ2V0IGxvY2F0aW9uOiBcIiwgZXZlbnQudGFyZ2V0TG9jYXRpb24pO1xuICAgICAgICAgICAgY29uc29sZS5sb2coXCJIb21lIHpvb20gPSBcIiwgb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0LmdldEhvbWVab29tKCkpO1xuICAgICAgICB9XG4gICAgICAgICBvc1ZpZXdlci52aWV3ZXIudmlld3BvcnQubWluWm9vbUxldmVsID0gb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0LmdldEhvbWVab29tKCkgKiBvc1ZpZXdlci5nZXRDb25maWcoKS5nbG9iYWwubWluWm9vbUxldmVsO1xuICAgICAgICAgdmFyIHRhcmdldFpvb20gPSBldmVudC50YXJnZXRMb2NhdGlvbi56b29tO1xuICAgICAgICAgdmFyIHRhcmdldExvY2F0aW9uID0gbmV3IE9wZW5TZWFkcmFnb24uUG9pbnQoZXZlbnQudGFyZ2V0TG9jYXRpb24ueCwgZXZlbnQudGFyZ2V0TG9jYXRpb24ueSk7XG4gICAgICAgICB2YXIgem9vbURpZmYgPSB0YXJnZXRab29tICogb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0LmdldEhvbWVab29tKCkgLSAob3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0Lm1pblpvb21MZXZlbCk7XG4vLyBjb25zb2xlLmxvZyhcInpvb21EaWZmOiBcIiArIHRhcmdldFpvb20gKyBcIiAqIFwiICsgb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0LmdldEhvbWVab29tKClcbi8vICsgXCIgLSBcIiArIG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC5taW5ab29tTGV2ZWwgKyBcIiA9IFwiLCB6b29tRGlmZik7XG4vLyBjb25zb2xlLmxvZyhcInpvb21EaWZmOiBcIiArIHRhcmdldFpvb20gKyBcIiAtIFwiICsgb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0Lm1pblpvb21MZXZlbCArXG4vLyBcIi9cIiArIG9zVmlld2VyLmNvbnRyb2xzLmdldEN1cnJlbnRSb3RhdGlvblpvb21pbmcoKSArIFwiID0gXCIsIHpvb21EaWZmKTtcbiAgICAgICAgIHZhciB6b29tZWRPdXQgPSB6b29tRGlmZiA8IDAuMDAxIHx8ICF0YXJnZXRab29tO1xuICAgICAgICAgaWYoem9vbWVkT3V0KSB7XG4gICAgICAgICAgICAgaWYoX2RlYnVnKSB7ICAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyhcIlpvb21pbmcgaG9tZVwiKVxuICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICBvc1ZpZXdlci5jb250cm9scy5nb0hvbWUoIHRydWUgKTtcbiAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICAgaWYoX2RlYnVnKSB7ICAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggXCJab29taW5nIHRvIFwiICsgdGFyZ2V0Wm9vbSArIFwiICogXCIgKyBvc1ZpZXdlci5jb250cm9scy5nZXRDdXJyZW50Um90YXRpb25ab29taW5nKCkgKTtcbiAgICAgICAgICAgICAgICAgY29uc29sZS5sb2coXCJwYW5uaW5nIHRvIFwiLCB0YXJnZXRMb2NhdGlvbik7XG4gICAgICAgICAgICAgfVxuICAgICAgICAgICAgIG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC56b29tVG8oIHRhcmdldFpvb20gKiBvc1ZpZXdlci5jb250cm9scy5nZXRDdXJyZW50Um90YXRpb25ab29taW5nKCksIG51bGwsIHRydWUpO1xuICAgICAgICAgICAgIG9zVmlld2VyLmNvbnRyb2xzLnNldENlbnRlciggdGFyZ2V0TG9jYXRpb24pO1xuICAgICAgICAgfVxuICAgICAgICAgaWYoZXZlbnQub3NTdGF0ZSA9PT0gXCJvcGVuXCIgJiYgZXZlbnQudGFyZ2V0TG9jYXRpb24ucm90YXRpb24gIT09IDApIHtcbiAgICAgICAgICAgIG9zVmlld2VyLmNvbnRyb2xzLnJvdGF0ZVRvKGV2ZW50LnRhcmdldExvY2F0aW9uLnJvdGF0aW9uKTtcbiAgICAgICAgIH1cbiAgICB9XG4gICAgXG4gICAgcmV0dXJuIG9zVmlld2VyO1xuICAgIFxufSApKCB2aWV3SW1hZ2UgfHwge30sIGpRdWVyeSApO1xuIiwiLyoqXG4gKiBUaGlzIGZpbGUgaXMgcGFydCBvZiB0aGUgR29vYmkgdmlld2VyIC0gYSBjb250ZW50IHByZXNlbnRhdGlvbiBhbmQgbWFuYWdlbWVudFxuICogYXBwbGljYXRpb24gZm9yIGRpZ2l0aXplZCBvYmplY3RzLlxuICogXG4gKiBWaXNpdCB0aGVzZSB3ZWJzaXRlcyBmb3IgbW9yZSBpbmZvcm1hdGlvbi4gLSBodHRwOi8vd3d3LmludHJhbmRhLmNvbSAtXG4gKiBodHRwOi8vZGlnaXZlcnNvLmNvbVxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZnJlZSBzb2Z0d2FyZTsgeW91IGNhbiByZWRpc3RyaWJ1dGUgaXQgYW5kL29yIG1vZGlmeSBpdCB1bmRlciB0aGUgdGVybXNcbiAqIG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhcyBwdWJsaXNoZWQgYnkgdGhlIEZyZWUgU29mdHdhcmUgRm91bmRhdGlvbjsgZWl0aGVyXG4gKiB2ZXJzaW9uIDIgb2YgdGhlIExpY2Vuc2UsIG9yIChhdCB5b3VyIG9wdGlvbikgYW55IGxhdGVyIHZlcnNpb24uXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBkaXN0cmlidXRlZCBpbiB0aGUgaG9wZSB0aGF0IGl0IHdpbGwgYmUgdXNlZnVsLCBidXQgV0lUSE9VVCBBTllcbiAqIFdBUlJBTlRZOyB3aXRob3V0IGV2ZW4gdGhlIGltcGxpZWQgd2FycmFudHkgb2YgTUVSQ0hBTlRBQklMSVRZIG9yIEZJVE5FU1MgRk9SIEFcbiAqIFBBUlRJQ1VMQVIgUFVSUE9TRS4gU2VlIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBmb3IgbW9yZSBkZXRhaWxzLlxuICogXG4gKiBZb3Ugc2hvdWxkIGhhdmUgcmVjZWl2ZWQgYSBjb3B5IG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhbG9uZyB3aXRoIHRoaXNcbiAqIHByb2dyYW0uIElmIG5vdCwgc2VlIDxodHRwOi8vd3d3LmdudS5vcmcvbGljZW5zZXMvPi5cbiAqIFxuICogTW9kdWxlIHdoaWNoIGhhbmRsZXMgdGhlIHBlcnNpc3RlbmNlIG9mIHpvb20gYW5kIHJvdGF0aW9uIGxldmVscy5cbiAqIFxuICogQHZlcnNpb24gMy4yLjBcbiAqIEBtb2R1bGUgdmlld0ltYWdlLmNvbnRyb2xzLnBlcnNpc3RlbmNlXG4gKiBAcmVxdWlyZXMgalF1ZXJ5XG4gKi9cbnZhciB2aWV3SW1hZ2UgPSAoIGZ1bmN0aW9uKCBvc1ZpZXdlciApIHtcbiAgICAndXNlIHN0cmljdCc7XG4gICAgXG4gICAgdmFyIF9kZWJ1ZyA9IGZhbHNlOyBcbiAgICBcbiAgICBvc1ZpZXdlci5jb250cm9scy5wZXJzaXN0ZW5jZSA9IHtcbiAgICAgICAgXG4gICAgICAgIGluaXQ6IGZ1bmN0aW9uKCBjb25maWcgKSB7XG4gICAgICAgICAgICBpZiAoIHR5cGVvZiAoIFN0b3JhZ2UgKSAhPT0gJ3VuZGVmaW5lZCcgKSB7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLyoqXG4gICAgICAgICAgICAgICAgICogU2V0IExvY2F0aW9uIGZyb20gbG9jYWwgc3RvcmFnZVxuICAgICAgICAgICAgICAgICAqL1xuICAgICAgICAgICAgICAgIHZhciBsb2NhdGlvbiA9IG51bGw7XG4gICAgICAgICAgICAgICAgdmFyIGN1cnJlbnRQZXJzaXN0ZW5jZUlkID0gb3NWaWV3ZXIuZ2V0Q29uZmlnKCkuZ2xvYmFsLnBlcnNpc3RlbmNlSWQ7XG4gICAgICAgICAgICAgICAgaWYgKCBjb25maWcuZ2xvYmFsLnBlcnNpc3Rab29tIHx8IGNvbmZpZy5nbG9iYWwucGVyc2lzdFJvdGF0aW9uICkge1xuICAgICAgICAgICAgICAgICAgICB0cnkge1xuICAgICAgICAgICAgICAgICAgICAgICAgdmFyIGxvY2F0aW9uID0gSlNPTi5wYXJzZSggbG9jYWxTdG9yYWdlLmltYWdlTG9jYXRpb24gKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBjYXRjaCAoIGVyciApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNvbnNvbGUuZXJyb3IoIFwiTm8gcmVhZGFibGUgaW1hZ2UgbG9jYXRpb24gaW4gbG9jYWwgc3RvcmFnZVwiICk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgaWYgKCBsb2NhdGlvbiAmJiBfaXNWYWxpZCggbG9jYXRpb24gKSAmJiBsb2NhdGlvbi5wZXJzaXN0ZW5jZUlkID09PSBjdXJyZW50UGVyc2lzdGVuY2VJZCApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCBcIlJlYWRpbmcgbG9jYXRpb24gZnJvbSBsb2NhbCBzdG9yYWdlXCIsIGxvY2F0aW9uICk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBjb25maWcuaW1hZ2UubG9jYXRpb24gPSB7fTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICggY29uZmlnLmdsb2JhbC5wZXJzaXN0Wm9vbSApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgY29uc29sZS5sb2coIFwic2V0dGluZyB6b29tIGZyb20gbG9jYWwgc3RvcmFnZVwiICk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNvbmZpZy5pbWFnZS5sb2NhdGlvbi56b29tID0gbG9jYXRpb24uem9vbTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBjb25maWcuaW1hZ2UubG9jYXRpb24ueCA9IGxvY2F0aW9uLng7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgY29uZmlnLmltYWdlLmxvY2F0aW9uLnkgPSBsb2NhdGlvbi55O1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKCBjb25maWcuZ2xvYmFsLnBlcnNpc3RSb3RhdGlvbiApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgY29uc29sZS5sb2coIFwic2V0dGluZyByb3RhdGlvbiBmcm9tIGxvY2FsIHN0b3JhZ2VcIiApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBjb25maWcuaW1hZ2UubG9jYXRpb24ucm90YXRpb24gPSBsb2NhdGlvbi5yb3RhdGlvbjtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNvbmZpZy5pbWFnZS5sb2NhdGlvbi5yb3RhdGlvbiA9IDA7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgLyoqXG4gICAgICAgICAgICAgICAgICAgICAqIHNhdmUgY3VycmVudCBsb2NhdGlvbiB0byBsb2NhbCBzdG9yYWdlIGJlZm9yZSBuYXZpZ2F0aW5nIGF3YXlcbiAgICAgICAgICAgICAgICAgICAgICovXG4gICAgICAgICAgICAgICAgICAgIHdpbmRvdy5vbmJlZm9yZXVubG9hZCA9IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgdmFyIGxvYyA9IG9zVmlld2VyLmNvbnRyb2xzLmdldExvY2F0aW9uKCk7XG4gICAgICAgICAgICAgICAgICAgICAgICBsb2MucGVyc2lzdGVuY2VJZCA9IG9zVmlld2VyLmdldENvbmZpZygpLmdsb2JhbC5wZXJzaXN0ZW5jZUlkO1xuICAgICAgICAgICAgICAgICAgICAgICAgbG9jYWxTdG9yYWdlLmltYWdlTG9jYXRpb24gPSBKU09OLnN0cmluZ2lmeSggbG9jICk7XG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggXCJzdG9yaW5nIHpvb20gXCIgKyBsb2NhbFN0b3JhZ2UuaW1hZ2VMb2NhdGlvbiApO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgfVxuXG4gICAgZnVuY3Rpb24gX2lzVmFsaWQoIGxvY2F0aW9uICkge1xuICAgICAgICByZXR1cm4gX2lzTnVtYmVyKCBsb2NhdGlvbi54ICkgJiYgX2lzTnVtYmVyKCBsb2NhdGlvbi55ICkgJiYgX2lzTnVtYmVyKCBsb2NhdGlvbi56b29tICkgJiYgX2lzTnVtYmVyKCBsb2NhdGlvbi5yb3RhdGlvbiApO1xuICAgIH1cbiAgICBcbiAgICBmdW5jdGlvbiBfaXNOdW1iZXIoIHggKSB7XG4gICAgICAgIHJldHVybiB0eXBlb2YgeCA9PT0gXCJudW1iZXJcIiAmJiAhTnVtYmVyLmlzTmFOKCB4ICk7XG4gICAgfVxuICAgIFxuICAgIHJldHVybiBvc1ZpZXdlcjtcbiAgICBcbn0gKSggdmlld0ltYWdlIHx8IHt9LCBqUXVlcnkgKTtcbiIsIi8qKlxuICogVGhpcyBmaWxlIGlzIHBhcnQgb2YgdGhlIEdvb2JpIHZpZXdlciAtIGEgY29udGVudCBwcmVzZW50YXRpb24gYW5kIG1hbmFnZW1lbnRcbiAqIGFwcGxpY2F0aW9uIGZvciBkaWdpdGl6ZWQgb2JqZWN0cy5cbiAqIFxuICogVmlzaXQgdGhlc2Ugd2Vic2l0ZXMgZm9yIG1vcmUgaW5mb3JtYXRpb24uIC0gaHR0cDovL3d3dy5pbnRyYW5kYS5jb20gLVxuICogaHR0cDovL2RpZ2l2ZXJzby5jb21cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGZyZWUgc29mdHdhcmU7IHlvdSBjYW4gcmVkaXN0cmlidXRlIGl0IGFuZC9vciBtb2RpZnkgaXQgdW5kZXIgdGhlIHRlcm1zXG4gKiBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYXMgcHVibGlzaGVkIGJ5IHRoZSBGcmVlIFNvZnR3YXJlIEZvdW5kYXRpb247IGVpdGhlclxuICogdmVyc2lvbiAyIG9mIHRoZSBMaWNlbnNlLCBvciAoYXQgeW91ciBvcHRpb24pIGFueSBsYXRlciB2ZXJzaW9uLlxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZGlzdHJpYnV0ZWQgaW4gdGhlIGhvcGUgdGhhdCBpdCB3aWxsIGJlIHVzZWZ1bCwgYnV0IFdJVEhPVVQgQU5ZXG4gKiBXQVJSQU5UWTsgd2l0aG91dCBldmVuIHRoZSBpbXBsaWVkIHdhcnJhbnR5IG9mIE1FUkNIQU5UQUJJTElUWSBvciBGSVRORVNTIEZPUiBBXG4gKiBQQVJUSUNVTEFSIFBVUlBPU0UuIFNlZSB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgZm9yIG1vcmUgZGV0YWlscy5cbiAqIFxuICogWW91IHNob3VsZCBoYXZlIHJlY2VpdmVkIGEgY29weSBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYWxvbmcgd2l0aCB0aGlzXG4gKiBwcm9ncmFtLiBJZiBub3QsIHNlZSA8aHR0cDovL3d3dy5nbnUub3JnL2xpY2Vuc2VzLz4uXG4gKiBcbiAqIE1vZHVsZSB3aGljaCBkcndhcyBhIGxpbmUgdG8gdGhlIGltYWdlLlxuICogXG4gKiBAdmVyc2lvbiAzLjIuMFxuICogQG1vZHVsZSB2aWV3SW1hZ2UuZHJhd0xpbmVcbiAqIEByZXF1aXJlcyBqUXVlcnlcbiAqL1xudmFyIHZpZXdJbWFnZSA9ICggZnVuY3Rpb24oIG9zVmlld2VyICkge1xuICAgICd1c2Ugc3RyaWN0JztcbiAgICBcbiAgICB2YXIgX2RlYnVnID0gZmFsc2U7XG4gICAgdmFyIF9kcmF3aW5nID0gdHJ1ZTtcbiAgICB2YXIgX3ZpZXdlcklucHV0SG9vayA9IG51bGw7XG4gICAgdmFyIF9oYkFkZCA9IDU7XG4gICAgdmFyIF9kZWxldGVPbGREcmF3RWxlbWVudCA9IHRydWU7XG4gICAgdmFyIF9kcmF3RWxlbWVudCA9IG51bGw7XG4gICAgdmFyIF9zdGFydFBvaW50ID0gbnVsbDtcbiAgICB2YXIgX2RyYXdQb2ludCA9IG51bGw7XG4gICAgXG4gICAgb3NWaWV3ZXIuZHJhd0xpbmUgPSB7XG4gICAgICAgIGluaXQ6IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgX3ZpZXdlcklucHV0SG9vayA9IG9zVmlld2VyLnZpZXdlci5hZGRWaWV3ZXJJbnB1dEhvb2soIHtcbiAgICAgICAgICAgICAgICBob29rczogWyB7XG4gICAgICAgICAgICAgICAgICAgIHRyYWNrZXI6IFwidmlld2VyXCIsXG4gICAgICAgICAgICAgICAgICAgIGhhbmRsZXI6IFwiY2xpY2tIYW5kbGVyXCIsXG4gICAgICAgICAgICAgICAgICAgIGhvb2tIYW5kbGVyOiBfZGlzYWJsZVZpZXdlckV2ZW50XG4gICAgICAgICAgICAgICAgfSwge1xuICAgICAgICAgICAgICAgICAgICB0cmFja2VyOiBcInZpZXdlclwiLFxuICAgICAgICAgICAgICAgICAgICBoYW5kbGVyOiBcInNjcm9sbEhhbmRsZXJcIixcbiAgICAgICAgICAgICAgICAgICAgaG9va0hhbmRsZXI6IF9kaXNhYmxlVmlld2VyRXZlbnRcbiAgICAgICAgICAgICAgICB9LCB7XG4gICAgICAgICAgICAgICAgICAgIHRyYWNrZXI6IFwidmlld2VyXCIsXG4gICAgICAgICAgICAgICAgICAgIGhhbmRsZXI6IFwiZHJhZ0hhbmRsZXJcIixcbiAgICAgICAgICAgICAgICAgICAgaG9va0hhbmRsZXI6IF9vblZpZXdlckRyYWdcbiAgICAgICAgICAgICAgICB9LCB7XG4gICAgICAgICAgICAgICAgICAgIHRyYWNrZXI6IFwidmlld2VyXCIsXG4gICAgICAgICAgICAgICAgICAgIGhhbmRsZXI6IFwicHJlc3NIYW5kbGVyXCIsXG4gICAgICAgICAgICAgICAgICAgIGhvb2tIYW5kbGVyOiBfb25WaWV3ZXJQcmVzc1xuICAgICAgICAgICAgICAgIH0sIHtcbiAgICAgICAgICAgICAgICAgICAgdHJhY2tlcjogXCJ2aWV3ZXJcIixcbiAgICAgICAgICAgICAgICAgICAgaGFuZGxlcjogXCJkcmFnRW5kSGFuZGxlclwiLFxuICAgICAgICAgICAgICAgICAgICBob29rSGFuZGxlcjogX29uVmlld2VyRHJhZ0VuZFxuICAgICAgICAgICAgICAgIH0gXVxuICAgICAgICAgICAgfSApO1xuICAgICAgICB9LFxuICAgICAgICB0b2dnbGVEcmF3aW5nOiBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIF9kcmF3aW5nID0gIV9kcmF3aW5nO1xuICAgICAgICB9XG4gICAgfVxuXG4gICAgZnVuY3Rpb24gX29uVmlld2VyUHJlc3MoIGV2ZW50ICkge1xuICAgICAgICBpZiAoIF9kcmF3aW5nICkge1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBpZiAoIF9kcmF3RWxlbWVudCAmJiBfZGVsZXRlT2xkRHJhd0VsZW1lbnQgKSB7XG4gICAgICAgICAgICAgICAgb3NWaWV3ZXIudmlld2VyLnJlbW92ZU92ZXJsYXkoIF9kcmF3RWxlbWVudCApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICBfZHJhd0VsZW1lbnQgPSBkb2N1bWVudC5jcmVhdGVFbGVtZW50KCBcImRpdlwiICk7XG4gICAgICAgICAgICBfZHJhd0VsZW1lbnQuc3R5bGUuYm9yZGVyID0gXCIycHggc29saWQgZ3JlZW5cIjtcbiAgICAgICAgICAgIF9kcmF3UG9pbnQgPSBvc1ZpZXdlci52aWV3ZXIudmlld3BvcnQudmlld2VyRWxlbWVudFRvVmlld3BvcnRDb29yZGluYXRlcyggZXZlbnQucG9zaXRpb24gKTtcbiAgICAgICAgICAgIF9kcmF3UG9pbnQgPSBvc1ZpZXdlci5vdmVybGF5cy5nZXRSb3RhdGVkKCBfZHJhd1BvaW50ICk7XG4gICAgICAgICAgICB2YXIgcmVjdCA9IG5ldyBPcGVuU2VhZHJhZ29uLlJlY3QoIF9kcmF3UG9pbnQueCwgX2RyYXdQb2ludC55LCAwLCAwICk7XG4gICAgICAgICAgICBvc1ZpZXdlci52aWV3ZXIuYWRkT3ZlcmxheSggX2RyYXdFbGVtZW50LCByZWN0LCAxICk7XG4gICAgICAgICAgICAvLyBjb25zb2xlLmxvZyhvc1ZpZXdlci52aWV3ZXIudmlld3BvcnRcbiAgICAgICAgICAgIC8vIC52aWV3ZXJFbGVtZW50VG9JbWFnZUNvb3JkaW5hdGVzKGV2ZW50LnBvc2l0aW9uKSk7XG4gICAgICAgICAgICBldmVudC5wcmV2ZW50RGVmYXVsdEFjdGlvbiA9IHRydWU7XG4gICAgICAgICAgICByZXR1cm4gdHJ1ZTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBcbiAgICBmdW5jdGlvbiBfb25WaWV3ZXJEcmFnKCBldmVudCApIHtcbiAgICAgICAgaWYgKCBfZHJhd2luZyApIHtcbiAgICAgICAgICAgIHZhciBuZXdQb2ludCA9IG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC52aWV3ZXJFbGVtZW50VG9WaWV3cG9ydENvb3JkaW5hdGVzKCBldmVudC5wb3NpdGlvbiApO1xuICAgICAgICAgICAgbmV3UG9pbnQgPSBvc1ZpZXdlci5vdmVybGF5cy5nZXRSb3RhdGVkKCBuZXdQb2ludCApO1xuICAgICAgICAgICAgdmFyIHJlY3QgPSBuZXcgT3BlblNlYWRyYWdvbi5SZWN0KCBfZHJhd1BvaW50LngsIF9kcmF3UG9pbnQueSwgbmV3UG9pbnQueCAtIF9kcmF3UG9pbnQueCwgbmV3UG9pbnQueSAtIF9kcmF3UG9pbnQueSApO1xuICAgICAgICAgICAgaWYgKCBuZXdQb2ludC54IDwgX2RyYXdQb2ludC54ICkge1xuICAgICAgICAgICAgICAgIHJlY3QueCA9IG5ld1BvaW50Lng7XG4gICAgICAgICAgICAgICAgcmVjdC53aWR0aCA9IF9kcmF3UG9pbnQueCAtIG5ld1BvaW50Lng7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAoIG5ld1BvaW50LnkgPCBfZHJhd1BvaW50LnkgKSB7XG4gICAgICAgICAgICAgICAgcmVjdC55ID0gbmV3UG9pbnQueTtcbiAgICAgICAgICAgICAgICByZWN0LmhlaWdodCA9IF9kcmF3UG9pbnQueSAtIG5ld1BvaW50Lnk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBvc1ZpZXdlci52aWV3ZXIudXBkYXRlT3ZlcmxheSggX2RyYXdFbGVtZW50LCByZWN0LCAwICk7XG4gICAgICAgICAgICBldmVudC5wcmV2ZW50RGVmYXVsdEFjdGlvbiA9IHRydWU7XG4gICAgICAgICAgICByZXR1cm4gdHJ1ZTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBcbiAgICBmdW5jdGlvbiBfb25WaWV3ZXJEcmFnRW5kKCBldmVudCApIHtcbiAgICAgICAgaWYgKCBfZHJhd2luZyApIHtcbiAgICAgICAgICAgIHZhciBuZXdQb2ludCA9IG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC52aWV3ZXJFbGVtZW50VG9WaWV3cG9ydENvb3JkaW5hdGVzKCBldmVudC5wb3NpdGlvbiApO1xuICAgICAgICAgICAgbmV3UG9pbnQgPSBvc1ZpZXdlci5vdmVybGF5cy5nZXRSb3RhdGVkKCBuZXdQb2ludCApO1xuICAgICAgICAgICAgdmFyIHJlY3QgPSBuZXcgT3BlblNlYWRyYWdvbi5SZWN0KCBfZHJhd1BvaW50LngsIF9kcmF3UG9pbnQueSwgbmV3UG9pbnQueCAtIF9kcmF3UG9pbnQueCwgbmV3UG9pbnQueSAtIF9kcmF3UG9pbnQueSApO1xuICAgICAgICAgICAgaWYgKCBuZXdQb2ludC54IDwgX2RyYXdQb2ludC54ICkge1xuICAgICAgICAgICAgICAgIHJlY3QueCA9IG5ld1BvaW50Lng7XG4gICAgICAgICAgICAgICAgcmVjdC53aWR0aCA9IF9kcmF3UG9pbnQueCAtIG5ld1BvaW50Lng7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAoIG5ld1BvaW50LnkgPCBfZHJhd1BvaW50LnkgKSB7XG4gICAgICAgICAgICAgICAgcmVjdC55ID0gbmV3UG9pbnQueTtcbiAgICAgICAgICAgICAgICByZWN0LmhlaWdodCA9IF9kcmF3UG9pbnQueSAtIG5ld1BvaW50Lnk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICByZWN0LmhpdEJveCA9IHtcbiAgICAgICAgICAgICAgICBsOiByZWN0LnggLSBfaGJBZGQsXG4gICAgICAgICAgICAgICAgdDogcmVjdC55IC0gX2hiQWRkLFxuICAgICAgICAgICAgICAgIHI6IHJlY3QueCArIHJlY3Qud2lkdGggKyBfaGJBZGQsXG4gICAgICAgICAgICAgICAgYjogcmVjdC55ICsgcmVjdC5oZWlnaHQgKyBfaGJBZGRcbiAgICAgICAgICAgIH07XG4gICAgICAgICAgICAvLyBvc1ZpZXdlci5vdmVybGF5cy5hZGRSZWN0KHtcbiAgICAgICAgICAgIC8vIGRyYXdFbGVtZW50IDogX2RyYXdFbGVtZW50LFxuICAgICAgICAgICAgLy8gcmVjdCA6IHJlY3RcbiAgICAgICAgICAgIC8vIH0pO1xuICAgICAgICAgICAgLy8gY29uc29sZS5sb2cob3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0XG4gICAgICAgICAgICAvLyAudmlld2VyRWxlbWVudFRvSW1hZ2VDb29yZGluYXRlcyhldmVudC5wb3NpdGlvbikpO1xuICAgICAgICAgICAgZXZlbnQucHJldmVudERlZmF1bHRBY3Rpb24gPSB0cnVlO1xuICAgICAgICAgICAgcmV0dXJuIHRydWU7XG4gICAgICAgIH1cbiAgICB9XG4gICAgXG4gICAgZnVuY3Rpb24gX2Rpc2FibGVWaWV3ZXJFdmVudCggZXZlbnQgKSB7XG4gICAgICAgIGlmICggX2RyYXdpbmcgKSB7XG4gICAgICAgICAgICBldmVudC5wcmV2ZW50RGVmYXVsdEFjdGlvbiA9IHRydWU7XG4gICAgICAgICAgICByZXR1cm4gdHJ1ZTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBmdW5jdGlvbiBjaGVja0ZvclJlY3RIaXQoIHBvaW50ICkge1xuICAgICAgICB2YXIgaTtcbiAgICAgICAgZm9yICggaSA9IDA7IGkgPCBfcmVjdHMubGVuZ3RoOyBpKysgKSB7XG4gICAgICAgICAgICB2YXIgeCA9IF9yZWN0c1sgaSBdO1xuICAgICAgICAgICAgaWYgKCBwb2ludC54ID4geC5oaXRCb3gubCAmJiBwb2ludC54IDwgeC5oaXRCb3guciAmJiBwb2ludC55ID4geC5oaXRCb3gudCAmJiBwb2ludC55IDwgeC5oaXRCb3guYiApIHtcbiAgICAgICAgICAgICAgICB2YXIgdG9wTGVmdEhiID0ge1xuICAgICAgICAgICAgICAgICAgICBsOiB4LnggLSBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIHQ6IHgueSAtIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgcjogeC54ICsgX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICBiOiB4LnkgKyBfaGJBZGRcbiAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICAgIHZhciB0b3BSaWdodEhiID0ge1xuICAgICAgICAgICAgICAgICAgICBsOiB4LnggKyB4LndpZHRoIC0gX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICB0OiB4LnkgLSBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIHI6IHgueCArIHgud2lkdGggKyBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIGI6IHgueSArIF9oYkFkZFxuICAgICAgICAgICAgICAgIH07XG4gICAgICAgICAgICAgICAgdmFyIGJvdHRvbVJpZ2h0SGIgPSB7XG4gICAgICAgICAgICAgICAgICAgIGw6IHgueCArIHgud2lkdGggLSBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIHQ6IHgueSArIHguaGVpZ2h0IC0gX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICByOiB4LnggKyB4LndpZHRoICsgX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICBiOiB4LnkgKyB4LmhlaWdodCArIF9oYkFkZFxuICAgICAgICAgICAgICAgIH07XG4gICAgICAgICAgICAgICAgdmFyIGJvdHRvbUxlZnRIYiA9IHtcbiAgICAgICAgICAgICAgICAgICAgbDogeC54IC0gX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICB0OiB4LnkgKyB4LmhlaWdodCAtIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgcjogeC54ICsgX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICBiOiB4LnkgKyB4LmhlaWdodCArIF9oYkFkZFxuICAgICAgICAgICAgICAgIH07XG4gICAgICAgICAgICAgICAgdmFyIHRvcEhiID0ge1xuICAgICAgICAgICAgICAgICAgICBsOiB4LnggKyBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIHQ6IHgueSAtIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgcjogeC54ICsgeC53aWR0aCAtIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgYjogeC55ICsgX2hiQWRkXG4gICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgICAgICB2YXIgcmlnaHRIYiA9IHtcbiAgICAgICAgICAgICAgICAgICAgbDogeC54ICsgeC53aWR0aCAtIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgdDogeC55ICsgX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICByOiB4LnggKyB4LndpZHRoICsgX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICBiOiB4LnkgKyB4LmhlaWdodCAtIF9oYkFkZFxuICAgICAgICAgICAgICAgIH07XG4gICAgICAgICAgICAgICAgdmFyIGJvdHRvbUhiID0ge1xuICAgICAgICAgICAgICAgICAgICBsOiB4LnggKyBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIHQ6IHgueSArIHguaGVpZ2h0IC0gX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICByOiB4LnggKyB4LndpZHRoIC0gX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICBiOiB4LnkgKyB4LmhlaWdodCArIF9oYkFkZFxuICAgICAgICAgICAgICAgIH07XG4gICAgICAgICAgICAgICAgdmFyIGxlZnRIYiA9IHtcbiAgICAgICAgICAgICAgICAgICAgbDogeC54IC0gX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICB0OiB4LnkgKyBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIHI6IHgueCArIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgYjogeC55ICsgeC5oZWlnaHQgLSBfaGJBZGRcbiAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgfVxuICAgIFxuICAgIHJldHVybiBvc1ZpZXdlcjtcbiAgICBcbn0gKSggdmlld0ltYWdlIHx8IHt9LCBqUXVlcnkgKTtcbiIsIi8qKlxuICogVGhpcyBmaWxlIGlzIHBhcnQgb2YgdGhlIEdvb2JpIHZpZXdlciAtIGEgY29udGVudCBwcmVzZW50YXRpb24gYW5kIG1hbmFnZW1lbnRcbiAqIGFwcGxpY2F0aW9uIGZvciBkaWdpdGl6ZWQgb2JqZWN0cy5cbiAqIFxuICogVmlzaXQgdGhlc2Ugd2Vic2l0ZXMgZm9yIG1vcmUgaW5mb3JtYXRpb24uIC0gaHR0cDovL3d3dy5pbnRyYW5kYS5jb20gLVxuICogaHR0cDovL2RpZ2l2ZXJzby5jb21cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGZyZWUgc29mdHdhcmU7IHlvdSBjYW4gcmVkaXN0cmlidXRlIGl0IGFuZC9vciBtb2RpZnkgaXQgdW5kZXIgdGhlIHRlcm1zXG4gKiBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYXMgcHVibGlzaGVkIGJ5IHRoZSBGcmVlIFNvZnR3YXJlIEZvdW5kYXRpb247IGVpdGhlclxuICogdmVyc2lvbiAyIG9mIHRoZSBMaWNlbnNlLCBvciAoYXQgeW91ciBvcHRpb24pIGFueSBsYXRlciB2ZXJzaW9uLlxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZGlzdHJpYnV0ZWQgaW4gdGhlIGhvcGUgdGhhdCBpdCB3aWxsIGJlIHVzZWZ1bCwgYnV0IFdJVEhPVVQgQU5ZXG4gKiBXQVJSQU5UWTsgd2l0aG91dCBldmVuIHRoZSBpbXBsaWVkIHdhcnJhbnR5IG9mIE1FUkNIQU5UQUJJTElUWSBvciBGSVRORVNTIEZPUiBBXG4gKiBQQVJUSUNVTEFSIFBVUlBPU0UuIFNlZSB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgZm9yIG1vcmUgZGV0YWlscy5cbiAqIFxuICogWW91IHNob3VsZCBoYXZlIHJlY2VpdmVkIGEgY29weSBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYWxvbmcgd2l0aCB0aGlzXG4gKiBwcm9ncmFtLiBJZiBub3QsIHNlZSA8aHR0cDovL3d3dy5nbnUub3JnL2xpY2Vuc2VzLz4uXG4gKiBcbiAqIE1vZHVsZSB3aGljaCBkcmF3cyByZWN0YW5nbGVzIHRvIHRoZSBpbWFnZSB0byBoaWdobGlnaHQgYXJlYXMuXG4gKiBcbiAqIEB2ZXJzaW9uIDMuMi4wXG4gKiBAbW9kdWxlIHZpZXdJbWFnZS5kcmF3UmVjdFxuICogQHJlcXVpcmVzIGpRdWVyeVxuICovXG52YXIgdmlld0ltYWdlID0gKCBmdW5jdGlvbiggb3NWaWV3ZXIgKSB7XG4gICAgJ3VzZSBzdHJpY3QnO1xuICAgIFxuICAgIHZhciBfZGVidWcgPSBmYWxzZTtcbiAgICBcbiAgICB2YXIgZHJhd2luZ1N0eWxlQ2xhc3MgPSBcImRyYXdpbmdcIjtcbiAgICBcbiAgICB2YXIgX2FjdGl2ZSA9IGZhbHNlO1xuICAgIHZhciBfZHJhd2luZyA9IGZhbHNlO1xuICAgIHZhciBfb3ZlcmxheUdyb3VwID0gbnVsbDtcbiAgICB2YXIgX2ZpbmlzaEhvb2sgPSBudWxsO1xuICAgIHZhciBfdmlld2VySW5wdXRIb29rID0gbnVsbDtcbiAgICB2YXIgX2hiQWRkID0gNTtcbiAgICB2YXIgX21pbkRpc3RhbmNlVG9FeGlzdGluZ1JlY3QgPSAwLjAxO1xuICAgIHZhciBfZGVsZXRlT2xkRHJhd0VsZW1lbnQgPSB0cnVlO1xuICAgIHZhciBfZHJhd0VsZW1lbnQgPSBudWxsO1xuICAgIHZhciBfZHJhd1BvaW50ID0gbnVsbDtcbiAgICBcbiAgICBvc1ZpZXdlci5kcmF3UmVjdCA9IHtcbiAgICAgICAgaW5pdDogZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ29zVmlld2VyLmRyYXdSZWN0LmluaXQnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICcjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMnICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBfdmlld2VySW5wdXRIb29rID0gb3NWaWV3ZXIudmlld2VyLmFkZFZpZXdlcklucHV0SG9vaygge1xuICAgICAgICAgICAgICAgIGhvb2tzOiBbIHtcbiAgICAgICAgICAgICAgICAgICAgdHJhY2tlcjogXCJ2aWV3ZXJcIixcbiAgICAgICAgICAgICAgICAgICAgaGFuZGxlcjogXCJjbGlja0hhbmRsZXJcIixcbiAgICAgICAgICAgICAgICAgICAgaG9va0hhbmRsZXI6IF9kaXNhYmxlVmlld2VyRXZlbnRcbiAgICAgICAgICAgICAgICAvLyB9LCB7XG4gICAgICAgICAgICAgICAgLy8gdHJhY2tlcjogXCJ2aWV3ZXJcIixcbiAgICAgICAgICAgICAgICAvLyBoYW5kbGVyOiBcInNjcm9sbEhhbmRsZXJcIixcbiAgICAgICAgICAgICAgICAvLyBob29rSGFuZGxlcjogX2Rpc2FibGVWaWV3ZXJFdmVudFxuICAgICAgICAgICAgICAgIH0sIHtcbiAgICAgICAgICAgICAgICAgICAgdHJhY2tlcjogXCJ2aWV3ZXJcIixcbiAgICAgICAgICAgICAgICAgICAgaGFuZGxlcjogXCJkcmFnSGFuZGxlclwiLFxuICAgICAgICAgICAgICAgICAgICBob29rSGFuZGxlcjogX29uVmlld2VyRHJhZ1xuICAgICAgICAgICAgICAgIH0sIHtcbiAgICAgICAgICAgICAgICAgICAgdHJhY2tlcjogXCJ2aWV3ZXJcIixcbiAgICAgICAgICAgICAgICAgICAgaGFuZGxlcjogXCJwcmVzc0hhbmRsZXJcIixcbiAgICAgICAgICAgICAgICAgICAgaG9va0hhbmRsZXI6IF9vblZpZXdlclByZXNzXG4gICAgICAgICAgICAgICAgfSwge1xuICAgICAgICAgICAgICAgICAgICB0cmFja2VyOiBcInZpZXdlclwiLFxuICAgICAgICAgICAgICAgICAgICBoYW5kbGVyOiBcImRyYWdFbmRIYW5kbGVyXCIsXG4gICAgICAgICAgICAgICAgICAgIGhvb2tIYW5kbGVyOiBfb25WaWV3ZXJEcmFnRW5kXG4gICAgICAgICAgICAgICAgfSBdXG4gICAgICAgICAgICB9ICk7XG4gICAgICAgIH0sXG4gICAgICAgIHN0YXJ0RHJhd2luZzogZnVuY3Rpb24oIG92ZXJsYXlHcm91cCwgZmluaXNoSG9vayApIHtcbiAgICAgICAgICAgIF9hY3RpdmUgPSB0cnVlO1xuICAgICAgICAgICAgX292ZXJsYXlHcm91cCA9IG92ZXJsYXlHcm91cDtcbiAgICAgICAgICAgIF9maW5pc2hIb29rID0gZmluaXNoSG9vaztcbiAgICAgICAgfSxcbiAgICAgICAgZW5kRHJhd2luZzogZnVuY3Rpb24oIHJlbW92ZUxhc3RFbGVtZW50ICkge1xuICAgICAgICAgICAgX2FjdGl2ZSA9IGZhbHNlO1xuICAgICAgICAgICAgX292ZXJsYXlHcm91cCA9IG51bGw7XG4gICAgICAgICAgICBfZmluaXNoSG9vayA9IG51bGw7XG4gICAgICAgICAgICBpZiAoIF9kcmF3RWxlbWVudCAmJiByZW1vdmVMYXN0RWxlbWVudCApIHtcbiAgICAgICAgICAgICAgICBvc1ZpZXdlci52aWV3ZXIucmVtb3ZlT3ZlcmxheSggX2RyYXdFbGVtZW50ICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAkKCBfZHJhd0VsZW1lbnQgKS5yZW1vdmVDbGFzcyggZHJhd2luZ1N0eWxlQ2xhc3MgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSxcbiAgICAgICAgaXNBY3RpdmU6IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgcmV0dXJuIF9hY3RpdmU7XG4gICAgICAgIH0sXG4gICAgICAgIGlzRHJhd2luZzogZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICByZXR1cm4gX2RyYXdpbmc7XG4gICAgICAgIH0sXG4gICAgICAgIHJlbW92ZUxhc3REcmF3bkVsZW1lbnQ6IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgaWYgKCBfZHJhd0VsZW1lbnQgKSB7XG4gICAgICAgICAgICAgICAgb3NWaWV3ZXIudmlld2VyLnJlbW92ZU92ZXJsYXkoIF9kcmF3RWxlbWVudCApO1xuICAgICAgICAgICAgfVxuICAgICAgICB9LFxuICAgIH1cblxuICAgIGZ1bmN0aW9uIF9vblZpZXdlclByZXNzKCBldmVudCApIHtcbiAgICAgICAgaWYgKCBfYWN0aXZlICkge1xuICAgICAgICAgICAgX2RyYXdQb2ludCA9IG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC52aWV3ZXJFbGVtZW50VG9WaWV3cG9ydENvb3JkaW5hdGVzKCBldmVudC5wb3NpdGlvbiApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBldmVudC5wcmV2ZW50RGVmYXVsdEFjdGlvbiA9IGZhbHNlO1xuICAgICAgICAgICAgcmV0dXJuIHRydWU7XG4gICAgICAgIH1cbiAgICB9XG4gICAgXG4gICAgZnVuY3Rpb24gX29uVmlld2VyRHJhZyggZXZlbnQgKSB7XG4gICAgICAgIC8vIGlmKF9kZWJ1Zykge1xuICAgICAgICAvLyBjb25zb2xlLmxvZyhcIkRyYWdnaW5nOiBcIik7XG4gICAgICAgIC8vIGNvbnNvbGUubG9nKFwiX2FjdGl2ZSA9IFwiICsgX2FjdGl2ZSk7XG4gICAgICAgIC8vIGNvbnNvbGUubG9nKFwiX2RyYXdpbmcgPSBcIiArIF9kcmF3aW5nKTtcbiAgICAgICAgLy8gY29uc29sZS5sb2coXCJfZHJhd1BvaW50ID0gXCIgKyBfZHJhd1BvaW50KTtcbiAgICAgICAgLy8gfVxuICAgICAgICBpZiAoIF9kcmF3aW5nICkge1xuICAgICAgICAgICAgdmFyIG5ld1BvaW50ID0gb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0LnZpZXdlckVsZW1lbnRUb1ZpZXdwb3J0Q29vcmRpbmF0ZXMoIGV2ZW50LnBvc2l0aW9uICk7XG4gICAgICAgICAgICB2YXIgcmVjdCA9IG5ldyBPcGVuU2VhZHJhZ29uLlJlY3QoIF9kcmF3UG9pbnQueCwgX2RyYXdQb2ludC55LCBuZXdQb2ludC54IC0gX2RyYXdQb2ludC54LCBuZXdQb2ludC55IC0gX2RyYXdQb2ludC55ICk7XG4gICAgICAgICAgICBpZiAoIG5ld1BvaW50LnggPCBfZHJhd1BvaW50LnggKSB7XG4gICAgICAgICAgICAgICAgcmVjdC54ID0gbmV3UG9pbnQueDtcbiAgICAgICAgICAgICAgICByZWN0LndpZHRoID0gX2RyYXdQb2ludC54IC0gbmV3UG9pbnQueDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmICggbmV3UG9pbnQueSA8IF9kcmF3UG9pbnQueSApIHtcbiAgICAgICAgICAgICAgICByZWN0LnkgPSBuZXdQb2ludC55O1xuICAgICAgICAgICAgICAgIHJlY3QuaGVpZ2h0ID0gX2RyYXdQb2ludC55IC0gbmV3UG9pbnQueTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIG9zVmlld2VyLnZpZXdlci51cGRhdGVPdmVybGF5KCBfZHJhd0VsZW1lbnQsIHJlY3QsIDAgKTtcbiAgICAgICAgICAgIGV2ZW50LnByZXZlbnREZWZhdWx0QWN0aW9uID0gdHJ1ZTtcbiAgICAgICAgICAgIHJldHVybiB0cnVlO1xuICAgICAgICAgICAgXG4gICAgICAgIH1cbiAgICAgICAgZWxzZSBpZiAoIF9hY3RpdmUgJiYgX2RyYXdQb2ludCApIHtcbiAgICAgICAgICAgIHZhciBhY3RpdmVPdmVybGF5ID0gb3NWaWV3ZXIub3ZlcmxheXMuZ2V0RHJhd2luZ092ZXJsYXkoKTtcbiAgICAgICAgICAgIGlmICggYWN0aXZlT3ZlcmxheSAmJiBvc1ZpZXdlci50cmFuc2Zvcm1SZWN0ICYmIG9zVmlld2VyLnRyYW5zZm9ybVJlY3QuaXNBY3RpdmUoKVxuICAgICAgICAgICAgICAgICAgICAmJiBvc1ZpZXdlci5vdmVybGF5cy5jb250YWlucyggYWN0aXZlT3ZlcmxheS5yZWN0LCBfZHJhd1BvaW50LCBfbWluRGlzdGFuY2VUb0V4aXN0aW5nUmVjdCApICkge1xuICAgICAgICAgICAgICAgIF9kcmF3UG9pbnQgPSBudWxsO1xuICAgICAgICAgICAgICAgIGlmICggX2RlYnVnIClcbiAgICAgICAgICAgICAgICAgICAgY29uc29sZS5sb2coIFwiQWN0aW9uIG92ZXJsYXBzIGFjdGl2ZSBvdmVybGF5XCIgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIF9kcmF3aW5nID0gdHJ1ZTtcbiAgICAgICAgICAgICAgICBpZiAoIGFjdGl2ZU92ZXJsYXkgJiYgX2RlbGV0ZU9sZERyYXdFbGVtZW50ICkge1xuICAgICAgICAgICAgICAgICAgICBvc1ZpZXdlci5vdmVybGF5cy5yZW1vdmVPdmVybGF5KCBhY3RpdmVPdmVybGF5ICk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIF9kcmF3RWxlbWVudCA9IGRvY3VtZW50LmNyZWF0ZUVsZW1lbnQoIFwiZGl2XCIgKTtcbiAgICAgICAgICAgICAgICBpZiAoIF9vdmVybGF5R3JvdXAgKSB7XG4gICAgICAgICAgICAgICAgICAgICQoIF9kcmF3RWxlbWVudCApLmFkZENsYXNzKCBfb3ZlcmxheUdyb3VwLnN0eWxlQ2xhc3MgKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgJCggX2RyYXdFbGVtZW50ICkuYWRkQ2xhc3MoIGRyYXdpbmdTdHlsZUNsYXNzICk7XG4gICAgICAgICAgICAgICAgdmFyIHJlY3QgPSBuZXcgT3BlblNlYWRyYWdvbi5SZWN0KCBfZHJhd1BvaW50LngsIF9kcmF3UG9pbnQueSwgMCwgMCApO1xuICAgICAgICAgICAgICAgIG9zVmlld2VyLnZpZXdlci5hZGRPdmVybGF5KCBfZHJhd0VsZW1lbnQsIHJlY3QsIDEgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGV2ZW50LnByZXZlbnREZWZhdWx0QWN0aW9uID0gdHJ1ZTtcbiAgICAgICAgICAgIHJldHVybiB0cnVlO1xuICAgICAgICB9XG4gICAgfVxuICAgIFxuICAgIGZ1bmN0aW9uIF9vblZpZXdlckRyYWdFbmQoIGV2ZW50ICkge1xuICAgICAgICBpZiAoIF9kcmF3aW5nICkge1xuICAgICAgICAgICAgX2RyYXdpbmcgPSBmYWxzZTtcbiAgICAgICAgICAgIHZhciBuZXdQb2ludCA9IG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC52aWV3ZXJFbGVtZW50VG9WaWV3cG9ydENvb3JkaW5hdGVzKCBldmVudC5wb3NpdGlvbiApO1xuICAgICAgICAgICAgdmFyIHJlY3QgPSBuZXcgT3BlblNlYWRyYWdvbi5SZWN0KCBfZHJhd1BvaW50LngsIF9kcmF3UG9pbnQueSwgbmV3UG9pbnQueCAtIF9kcmF3UG9pbnQueCwgbmV3UG9pbnQueSAtIF9kcmF3UG9pbnQueSApO1xuICAgICAgICAgICAgaWYgKCBuZXdQb2ludC54IDwgX2RyYXdQb2ludC54ICkge1xuICAgICAgICAgICAgICAgIHJlY3QueCA9IG5ld1BvaW50Lng7XG4gICAgICAgICAgICAgICAgcmVjdC53aWR0aCA9IF9kcmF3UG9pbnQueCAtIG5ld1BvaW50Lng7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAoIG5ld1BvaW50LnkgPCBfZHJhd1BvaW50LnkgKSB7XG4gICAgICAgICAgICAgICAgcmVjdC55ID0gbmV3UG9pbnQueTtcbiAgICAgICAgICAgICAgICByZWN0LmhlaWdodCA9IF9kcmF3UG9pbnQueSAtIG5ld1BvaW50Lnk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICByZWN0LmhpdEJveCA9IHtcbiAgICAgICAgICAgICAgICBsOiByZWN0LnggLSBfaGJBZGQsXG4gICAgICAgICAgICAgICAgdDogcmVjdC55IC0gX2hiQWRkLFxuICAgICAgICAgICAgICAgIHI6IHJlY3QueCArIHJlY3Qud2lkdGggKyBfaGJBZGQsXG4gICAgICAgICAgICAgICAgYjogcmVjdC55ICsgcmVjdC5oZWlnaHQgKyBfaGJBZGRcbiAgICAgICAgICAgIH07XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHZhciBvdmVybGF5ID0ge1xuICAgICAgICAgICAgICAgIHR5cGU6IG9zVmlld2VyLm92ZXJsYXlzLm92ZXJsYXlUeXBlcy5SRUNUQU5HTEUsXG4gICAgICAgICAgICAgICAgZWxlbWVudDogX2RyYXdFbGVtZW50LFxuICAgICAgICAgICAgICAgIHJlY3Q6IHJlY3QsXG4gICAgICAgICAgICAgICAgZ3JvdXA6IF9vdmVybGF5R3JvdXAubmFtZSxcbiAgICAgICAgICAgIH07XG4gICAgICAgICAgICBvc1ZpZXdlci5vdmVybGF5cy5zZXREcmF3aW5nT3ZlcmxheSggb3ZlcmxheSApO1xuICAgICAgICAgICAgaWYgKCBfZmluaXNoSG9vayApIHtcbiAgICAgICAgICAgICAgICBfZmluaXNoSG9vayggb3ZlcmxheSApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICBldmVudC5wcmV2ZW50RGVmYXVsdEFjdGlvbiA9IHRydWU7XG4gICAgICAgICAgICByZXR1cm4gdHJ1ZTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICB9XG4gICAgXG4gICAgZnVuY3Rpb24gX2Rpc2FibGVWaWV3ZXJFdmVudCggZXZlbnQgKSB7XG4gICAgICAgIGlmICggX2FjdGl2ZSApIHtcbiAgICAgICAgICAgIGV2ZW50LnByZXZlbnREZWZhdWx0QWN0aW9uID0gdHJ1ZTtcbiAgICAgICAgICAgIHJldHVybiB0cnVlO1xuICAgICAgICB9XG4gICAgfVxuICAgIFxuICAgIGZ1bmN0aW9uIGNoZWNrRm9yUmVjdEhpdCggcG9pbnQgKSB7XG4gICAgICAgIHZhciBpO1xuICAgICAgICBmb3IgKCBpID0gMDsgaSA8IF9yZWN0cy5sZW5ndGg7IGkrKyApIHtcbiAgICAgICAgICAgIHZhciB4ID0gX3JlY3RzWyBpIF07XG4gICAgICAgICAgICBpZiAoIHBvaW50LnggPiB4LmhpdEJveC5sICYmIHBvaW50LnggPCB4LmhpdEJveC5yICYmIHBvaW50LnkgPiB4LmhpdEJveC50ICYmIHBvaW50LnkgPCB4LmhpdEJveC5iICkge1xuICAgICAgICAgICAgICAgIHZhciB0b3BMZWZ0SGIgPSB7XG4gICAgICAgICAgICAgICAgICAgIGw6IHgueCAtIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgdDogeC55IC0gX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICByOiB4LnggKyBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIGI6IHgueSArIF9oYkFkZFxuICAgICAgICAgICAgICAgIH07XG4gICAgICAgICAgICAgICAgdmFyIHRvcFJpZ2h0SGIgPSB7XG4gICAgICAgICAgICAgICAgICAgIGw6IHgueCArIHgud2lkdGggLSBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIHQ6IHgueSAtIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgcjogeC54ICsgeC53aWR0aCArIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgYjogeC55ICsgX2hiQWRkXG4gICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgICAgICB2YXIgYm90dG9tUmlnaHRIYiA9IHtcbiAgICAgICAgICAgICAgICAgICAgbDogeC54ICsgeC53aWR0aCAtIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgdDogeC55ICsgeC5oZWlnaHQgLSBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIHI6IHgueCArIHgud2lkdGggKyBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIGI6IHgueSArIHguaGVpZ2h0ICsgX2hiQWRkXG4gICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgICAgICB2YXIgYm90dG9tTGVmdEhiID0ge1xuICAgICAgICAgICAgICAgICAgICBsOiB4LnggLSBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIHQ6IHgueSArIHguaGVpZ2h0IC0gX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICByOiB4LnggKyBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIGI6IHgueSArIHguaGVpZ2h0ICsgX2hiQWRkXG4gICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgICAgICB2YXIgdG9wSGIgPSB7XG4gICAgICAgICAgICAgICAgICAgIGw6IHgueCArIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgdDogeC55IC0gX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICByOiB4LnggKyB4LndpZHRoIC0gX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICBiOiB4LnkgKyBfaGJBZGRcbiAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICAgIHZhciByaWdodEhiID0ge1xuICAgICAgICAgICAgICAgICAgICBsOiB4LnggKyB4LndpZHRoIC0gX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICB0OiB4LnkgKyBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIHI6IHgueCArIHgud2lkdGggKyBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIGI6IHgueSArIHguaGVpZ2h0IC0gX2hiQWRkXG4gICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgICAgICB2YXIgYm90dG9tSGIgPSB7XG4gICAgICAgICAgICAgICAgICAgIGw6IHgueCArIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgdDogeC55ICsgeC5oZWlnaHQgLSBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIHI6IHgueCArIHgud2lkdGggLSBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIGI6IHgueSArIHguaGVpZ2h0ICsgX2hiQWRkXG4gICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgICAgICB2YXIgbGVmdEhiID0ge1xuICAgICAgICAgICAgICAgICAgICBsOiB4LnggLSBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIHQ6IHgueSArIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgcjogeC54ICsgX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICBiOiB4LnkgKyB4LmhlaWdodCAtIF9oYkFkZFxuICAgICAgICAgICAgICAgIH07XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICB9XG4gICAgXG4gICAgcmV0dXJuIG9zVmlld2VyO1xuICAgIFxufSApKCB2aWV3SW1hZ2UgfHwge30sIGpRdWVyeSApO1xuIiwiLyoqXG4gKiBUaGlzIGZpbGUgaXMgcGFydCBvZiB0aGUgR29vYmkgdmlld2VyIC0gYSBjb250ZW50IHByZXNlbnRhdGlvbiBhbmQgbWFuYWdlbWVudFxuICogYXBwbGljYXRpb24gZm9yIGRpZ2l0aXplZCBvYmplY3RzLlxuICogXG4gKiBWaXNpdCB0aGVzZSB3ZWJzaXRlcyBmb3IgbW9yZSBpbmZvcm1hdGlvbi4gLSBodHRwOi8vd3d3LmludHJhbmRhLmNvbSAtXG4gKiBodHRwOi8vZGlnaXZlcnNvLmNvbVxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZnJlZSBzb2Z0d2FyZTsgeW91IGNhbiByZWRpc3RyaWJ1dGUgaXQgYW5kL29yIG1vZGlmeSBpdCB1bmRlciB0aGUgdGVybXNcbiAqIG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhcyBwdWJsaXNoZWQgYnkgdGhlIEZyZWUgU29mdHdhcmUgRm91bmRhdGlvbjsgZWl0aGVyXG4gKiB2ZXJzaW9uIDIgb2YgdGhlIExpY2Vuc2UsIG9yIChhdCB5b3VyIG9wdGlvbikgYW55IGxhdGVyIHZlcnNpb24uXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBkaXN0cmlidXRlZCBpbiB0aGUgaG9wZSB0aGF0IGl0IHdpbGwgYmUgdXNlZnVsLCBidXQgV0lUSE9VVCBBTllcbiAqIFdBUlJBTlRZOyB3aXRob3V0IGV2ZW4gdGhlIGltcGxpZWQgd2FycmFudHkgb2YgTUVSQ0hBTlRBQklMSVRZIG9yIEZJVE5FU1MgRk9SIEFcbiAqIFBBUlRJQ1VMQVIgUFVSUE9TRS4gU2VlIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBmb3IgbW9yZSBkZXRhaWxzLlxuICogXG4gKiBZb3Ugc2hvdWxkIGhhdmUgcmVjZWl2ZWQgYSBjb3B5IG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhbG9uZyB3aXRoIHRoaXNcbiAqIHByb2dyYW0uIElmIG5vdCwgc2VlIDxodHRwOi8vd3d3LmdudS5vcmcvbGljZW5zZXMvPi5cbiAqIFxuICogTW9kdWxlIHdoaWNoIGluaXRpYWxpemVzIHRoZSB2aWV3ZXJKUyBtb2R1bGUuXG4gKiBcbiAqIEB2ZXJzaW9uIDMuMi4wXG4gKiBAbW9kdWxlIHZpZXdJbWFnZVxuICogQHJlcXVpcmVzIGpRdWVyeVxuICovXG52YXIgdmlld0ltYWdlID0gKCBmdW5jdGlvbigpIHtcbiAgICAndXNlIHN0cmljdCc7XG4gICAgXG4gICAgdmFyIG9zVmlld2VyID0ge307XG4gICAgdmFyIF9kZWJ1ZyA9IGZhbHNlO1xuICAgIHZhciBfZm9vdGVySW1hZ2UgPSBudWxsO1xuICAgIHZhciBfY2FudmFzU2NhbGU7XG4gICAgdmFyIF9jb250YWluZXI7XG4gICAgdmFyIF9kZWZhdWx0cyA9IHsgIFxuICAgICAgICBnbG9iYWw6IHtcbiAgICAgICAgICAgIGRpdklkOiBcIm1hcFwiLFxuICAgICAgICAgICAgem9vbVNsaWRlcjogXCIuem9vbS1zbGlkZXJcIixcbiAgICAgICAgICAgIHpvb21TbGlkZXJIYW5kbGU6ICcuem9vbS1zbGlkZXItaGFuZGxlJyxcbiAgICAgICAgICAgIG92ZXJsYXlHcm91cHM6IFsge1xuICAgICAgICAgICAgICAgIG5hbWU6IFwic2VhcmNoSGlnaGxpZ2h0aW5nXCIsXG4gICAgICAgICAgICAgICAgc3R5bGVDbGFzczogXCJjb29yZHMtaGlnaGxpZ2h0aW5nXCIsXG4gICAgICAgICAgICAgICAgaW50ZXJhY3RpdmU6IGZhbHNlXG4gICAgICAgICAgICB9LCB7XG4gICAgICAgICAgICAgICAgbmFtZTogXCJ1Z2NcIixcbiAgICAgICAgICAgICAgICBzdHlsZUNsYXNzOiBcInVnY0JveFwiLFxuICAgICAgICAgICAgICAgIGludGVyYWN0aXZlOiB0cnVlXG4gICAgICAgICAgICBcbiAgICAgICAgICAgIH0gXSxcbiAgICAgICAgICAgIHpvb21TcGVlZDogMS4yNSxcbiAgICAgICAgICAgIG1heFpvb21MZXZlbDogMjAsXG4gICAgICAgICAgICBtaW5ab29tTGV2ZWw6IDEsXG4gICAgICAgICAgICB1c2VUaWxlczogdHJ1ZSxcbiAgICAgICAgICAgIGltYWdlQ29udHJvbHNBY3RpdmU6IHRydWUsXG4gICAgICAgICAgICB2aXNpYmlsaXR5UmF0aW86IDAuNCxcbiAgICAgICAgICAgIGxvYWRJbWFnZVRpbWVvdXQ6IDEwICogNjAgKiAxMDAwLFxuICAgICAgICAgICAgbWF4UGFyYWxsZWxJbWFnZUxvYWRzOiA0LFxuICAgICAgICAgICAgYWRhcHRDb250YWluZXJIZWlnaHQ6IGZhbHNlLFxuICAgICAgICAgICAgZm9vdGVySGVpZ2h0OiA1MCxcbiAgICAgICAgICAgIHJlbWVtYmVyWm9vbTogZmFsc2UsXG4gICAgICAgICAgICByZW1lbWJlclJvdGF0aW9uOiBmYWxzZSxcbiAgICAgICAgfSxcbiAgICAgICAgaW1hZ2U6IHt9LFxuICAgICAgICBnZXRPdmVybGF5R3JvdXA6IGZ1bmN0aW9uKCBuYW1lICkge1xuICAgICAgICAgICAgdmFyIGFsbEdyb3VwcyA9IF9kZWZhdWx0cy5nbG9iYWwub3ZlcmxheUdyb3VwcztcbiAgICAgICAgICAgIGZvciAoIHZhciBpbnQgPSAwOyBpbnQgPCBhbGxHcm91cHMubGVuZ3RoOyBpbnQrKyApIHtcbiAgICAgICAgICAgICAgICB2YXIgZ3JvdXAgPSBhbGxHcm91cHNbIGludCBdO1xuICAgICAgICAgICAgICAgIGlmICggZ3JvdXAubmFtZSA9PT0gbmFtZSApIHtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIGdyb3VwO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgfSxcbiAgICAgICAgZ2V0Q29vcmRpbmF0ZXM6IGZ1bmN0aW9uKCBuYW1lICkge1xuICAgICAgICAgICAgdmFyIGNvb2RpbmF0ZXNBcnJheSA9IF9kZWZhdWx0cy5pbWFnZS5oaWdobGlnaHRDb29yZHM7XG4gICAgICAgICAgICBpZiAoIGNvb2RpbmF0ZXNBcnJheSApIHtcbiAgICAgICAgICAgICAgICBmb3IgKCB2YXIgaW50ID0gMDsgaW50IDwgY29vZGluYXRlc0FycmF5Lmxlbmd0aDsgaW50KysgKSB7XG4gICAgICAgICAgICAgICAgICAgIHZhciBjb29yZHMgPSBjb29kaW5hdGVzQXJyYXlbIGludCBdO1xuICAgICAgICAgICAgICAgICAgICBpZiAoIGNvb3Jkcy5uYW1lID09PSBuYW1lICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuIGNvb3JkcztcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgfSxcbiAgICB9O1xuICAgIFxuICAgIG9zVmlld2VyID0ge1xuICAgICAgICB2aWV3ZXI6IG51bGwsXG4gICAgICAgIGluaXQ6IGZ1bmN0aW9uKCBjb25maWcgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ29zVmlld2VyLmluaXQnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICcjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMnICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIGNvbnN0cnVjdG9yXG4gICAgICAgICAgICAkLmV4dGVuZCggdHJ1ZSwgX2RlZmF1bHRzLCBjb25maWcgKTtcbiAgICAgICAgICAgIC8vIGNvbnZlcnQgbWltZVR5cGUgXCJpbWFnZS9qcGVnXCIgdG8gXCJpbWFnZS9qcGdcIiB0byBwcm92aWRlIGNvcnJlY3Rcblx0XHRcdC8vIGlpaWYgY2FsbHNcbiAgICAgICAgICAgIF9kZWZhdWx0cy5pbWFnZS5taW1lVHlwZSA9IF9kZWZhdWx0cy5pbWFnZS5taW1lVHlwZS5yZXBsYWNlKFwianBlZ1wiLFwianBnXCIpO1xuICAgICAgICAgICAgX2NvbnRhaW5lciA9ICQoIFwiI1wiICsgX2RlZmF1bHRzLmdsb2JhbC5kaXZJZCApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICB2YXIgc291cmNlcyA9IF9kZWZhdWx0cy5pbWFnZS50aWxlU291cmNlO1xuICAgICAgICAgICAgaWYodHlwZW9mIHNvdXJjZXMgPT09ICdzdHJpbmcnICYmIHNvdXJjZXMuc3RhcnRzV2l0aChcIltcIikpIHtcbiAgICAgICAgICAgIFx0c291cmNlcyA9IEpTT04ucGFyc2Uoc291cmNlcyk7XG4gICAgICAgICAgICB9IGVsc2UgaWYoISQuaXNBcnJheShzb3VyY2VzKSkge1xuICAgICAgICAgICAgXHRzb3VyY2VzID0gW3NvdXJjZXNdO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgdmFyIHByb21pc2VzID0gW107XG4gICAgICAgICAgICBmb3IgKCB2YXIgaT0wOyBpPHNvdXJjZXMubGVuZ3RoOyBpKyspIHtcbiAgICAgICAgICAgIFx0dmFyIHNvdXJjZSA9IHNvdXJjZXNbaV07XG4gICAgICAgICAgICBcdC8vIHJldHVybnMgdGhlIE9wZW5TZWFkcmFnb24uVGlsZVNvdXJjZSBpZiBpdCBjYW4gYmUgY3JlYXRlZCxcblx0XHRcdFx0Ly8gb3RoZXJ3ZWlzZVxuICAgICAgICAgICAgICAgIC8vIHJlamVjdHMgdGhlIHByb21pc2VcbiAgICAgICAgICAgIFx0dmFyIHByb21pc2UgPSB2aWV3SW1hZ2UuY3JlYXRlVGlsZVNvdXJjZShzb3VyY2UpO1xuICAgICAgICAgICAgXHRwcm9taXNlcy5wdXNoKHByb21pc2UpO1x0XG5cdCAgICAgICAgICAgICAgICB9ICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgcmV0dXJuIFEuYWxsKHByb21pc2VzKS50aGVuKGZ1bmN0aW9uKHRpbGVTb3VyY2VzKSB7XG4gICAgICAgICAgICBcdHZhciBtaW5XaWR0aCA9IE51bWJlci5NQVhfVkFMVUU7ICBcbiAgICAgICAgICAgIFx0dmFyIG1pbkhlaWdodCA9IE51bWJlci5NQVhfVkFMVUU7XG4gICAgICAgICAgICBcdHZhciBtaW5Bc3BlY3RSYXRpbyA9IE51bWJlci5NQVhfVkFMVUU7XG4gICAgICAgICAgICBcdGZvciAoIHZhciBqPTA7IGo8dGlsZVNvdXJjZXMubGVuZ3RoOyBqKyspIHtcbiAgICAgICAgICAgIFx0XHR2YXIgdGlsZVNvdXJjZSA9IHRpbGVTb3VyY2VzW2pdO1xuICAgICAgICAgICAgXHRcdG1pbldpZHRoID0gTWF0aC5taW4obWluV2lkdGgsIHRpbGVTb3VyY2Uud2lkdGgpO1xuICAgICAgICAgICAgXHRcdG1pbkhlaWdodCA9IE1hdGgubWluKG1pbkhlaWdodCwgdGlsZVNvdXJjZS5oZWlnaHQpO1xuICAgICAgICAgICAgXHRcdG1pbkFzcGVjdFJhdGlvID0gTWF0aC5taW4obWluQXNwZWN0UmF0aW8sIHRpbGVTb3VyY2UuYXNwZWN0UmF0aW8pO1xuXHQgICAgICAgICAgICAgICAgfVxuXHQgICAgICAgICAgICAgICAgICAgIGlmKF9kZWJ1ZykgeyAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICBcdCAgICBjb25zb2xlLmxvZyhcIk1pbiBhc3BlY3QgcmF0aW8gPSBcIiArIG1pbkFzcGVjdFJhdGlvKTsgICAgICAgICAgICBcdCAgICBcblx0ICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICBcdHZhciB4ID0gMDtcbiAgICAgICAgICAgIFx0Zm9yICggdmFyIGk9MDsgaTx0aWxlU291cmNlcy5sZW5ndGg7IGkrKykge1xuXHQgICAgICAgIFx0XHR2YXIgdGlsZVNvdXJjZSA9IHRpbGVTb3VyY2VzW2ldO1xuXHQgICAgICAgIFx0XHR0aWxlU291cmNlc1tpXSA9IHtcblx0ICAgICAgICBcdFx0XHRcdHRpbGVTb3VyY2U6IHRpbGVTb3VyY2UsXG5cdCAgICAgICAgXHRcdFx0XHR3aWR0aDogdGlsZVNvdXJjZS5hc3BlY3RSYXRpby9taW5Bc3BlY3RSYXRpbyxcbi8vIGhlaWdodDogbWluSGVpZ2h0L3RpbGVTb3VyY2UuaGVpZ2h0LFxuXHQgICAgICAgICAgICAgICAgXHRcdHggOiB4LFxuXHQgICAgICAgICAgICAgICAgXHRcdHk6IDAsXG5cdCAgICAgICAgICAgICAgICAgICAgfVxuXHQgICAgICAgIFx0XHR4ICs9IHRpbGVTb3VyY2VzW2ldLndpZHRoO1xuXHQgICAgICAgICAgICAgICAgfSAgICAgICAgICAgICAgXG4gICAgICAgICAgICBcdHJldHVybiB2aWV3SW1hZ2UubG9hZEltYWdlKHRpbGVTb3VyY2VzKTtcbiAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgXG4gICAgICAgIH0sXG4gICAgICAgIGxvYWRJbWFnZSA6IGZ1bmN0aW9uKHRpbGVTb3VyY2VzKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ0xvYWRpbmcgaW1hZ2Ugd2l0aCB0aWxlc291cmNlOiAnLCB0aWxlU291cmNlcyApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgICBcbiAgICAgICAgICAgIG9zVmlld2VyLmxvYWRGb290ZXIoKTsgICAgICAgICAgICBcbiAgICAgICAgIFxuICAgICAgICAgICAgb3NWaWV3ZXIudmlld2VyID0gbmV3IE9wZW5TZWFkcmFnb24oIHtcbiAgICAgICAgICAgICAgICBpbW1lZGlhdGVSZW5kZXI6IGZhbHNlLFxuICAgICAgICAgICAgICAgIHZpc2liaWxpdHlSYXRpbzogX2RlZmF1bHRzLmdsb2JhbC52aXNpYmlsaXR5UmF0aW8sXG4gICAgICAgICAgICAgICAgc2VxdWVuY2VNb2RlOiBmYWxzZSxcbiAgICAgICAgICAgICAgICBpZDogX2RlZmF1bHRzLmdsb2JhbC5kaXZJZCxcbiAgICAgICAgICAgICAgICBjb250cm9sc0VuYWJsZWQ6IGZhbHNlLFxuICAgICAgICAgICAgICAgIHByZWZpeFVybDogXCIvb3BlbnNlYWRyYWdvbi1iaW4vaW1hZ2VzL1wiLFxuICAgICAgICAgICAgICAgIHpvb21QZXJDbGljazogMSxcbiAgICAgICAgICAgICAgICBtYXhab29tTGV2ZWw6IF9kZWZhdWx0cy5nbG9iYWwubWF4Wm9vbUxldmVsLFxuICAgICAgICAgICAgICAgIG1pblpvb21MZXZlbDogX2RlZmF1bHRzLmdsb2JhbC5taW5ab29tTGV2ZWwsXG4gICAgICAgICAgICAgICAgem9vbVBlclNjcm9sbDogX2RlZmF1bHRzLmdsb2JhbC56b29tU3BlZWQsXG4gICAgICAgICAgICAgICAgbW91c2VOYXZFbmFibGVkOiBfZGVmYXVsdHMuZ2xvYmFsLnpvb21TcGVlZCA+IDEsXG4gICAgICAgICAgICAgICAgc2hvd05hdmlnYXRpb25Db250cm9sOiBmYWxzZSxcbiAgICAgICAgICAgICAgICBzaG93Wm9vbUNvbnRyb2w6IGZhbHNlLFxuICAgICAgICAgICAgICAgIHNob3dIb21lQ29udHJvbDogZmFsc2UsXG4gICAgICAgICAgICAgICAgc2hvd0Z1bGxQYWdlQ29udHJvbDogdHJ1ZSxcbiAgICAgICAgICAgICAgICB0aW1lb3V0OiBfZGVmYXVsdHMuZ2xvYmFsLmxvYWRJbWFnZVRpbWVvdXQsXG4gICAgICAgICAgICAgICAgdGlsZVNvdXJjZXM6IHRpbGVTb3VyY2VzLFxuICAgICAgICAgICAgICAgIGJsZW5kVGltZTogLjUsXG4gICAgICAgICAgICAgICAgYWx3YXlzQmxlbmQ6IGZhbHNlLFxuICAgICAgICAgICAgICAgIGltYWdlTG9hZGVyTGltaXQ6IF9kZWZhdWx0cy5nbG9iYWwubWF4UGFyYWxsZWxJbWFnZUxvYWRzLFxuICAgICAgICAgICAgICAgIHZpZXdwb3J0TWFyZ2luczoge1xuICAgICAgICAgICAgICAgICAgICB0b3A6IDAsXG4gICAgICAgICAgICAgICAgICAgIGxlZnQ6IDAsXG4gICAgICAgICAgICAgICAgICAgIHJpZ2h0OiAwLFxuICAgICAgICAgICAgICAgICAgICBib3R0b206IF9kZWZhdWx0cy5nbG9iYWwuZm9vdGVySGVpZ2h0XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfSApO1xuXG4gICAgICAgICAgICB2YXIgcmVzdWx0ID0gUS5kZWZlcigpO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgb3NWaWV3ZXIub2JzZXJ2YWJsZXMgPSBjcmVhdGVPYnNlcnZhYmxlcyh3aW5kb3csIG9zVmlld2VyLnZpZXdlcik7ICBcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgIG9zVmlld2VyLm9ic2VydmFibGVzLnZpZXdlck9wZW4uc3Vic2NyaWJlKGZ1bmN0aW9uKG9wZW5ldmVudCwgbG9hZGV2ZW50KSB7ICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgcmVzdWx0LnJlc29sdmUob3NWaWV3ZXIpOyAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgIH0sIGZ1bmN0aW9uKGVycm9yKSB7ICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgcmVzdWx0LnJlamVjdChlcnJvcik7ICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgfSk7ICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gQ2FsY3VsYXRlIHNpemVzIGlmIHJlZHJhdyBpcyByZXF1aXJlZFxuICAgICAgICAgICAgXG4gICAgICAgICAgICBvc1ZpZXdlci5vYnNlcnZhYmxlcy5yZWRyYXdSZXF1aXJlZC5zdWJzY3JpYmUoZnVuY3Rpb24oZXZlbnQpIHsgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICBpZihfZGVidWcpIHtcbiAgICAgICAgICAgICAgICAgICAgY29uc29sZS5sb2coXCJ2aWV3ZXIgXCIgKyBldmVudC5vc1N0YXRlICsgXCJlZCB3aXRoIHRhcmdldCBsb2NhdGlvbiBcIiwgZXZlbnQudGFyZ2V0TG9jYXRpb24pOyAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIG9zVmlld2VyLnJlZHJhdygpO1xuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICBpZiAoIG9zVmlld2VyLmNvbnRyb2xzICkgeyAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgb3NWaWV3ZXIuY29udHJvbHMuaW5pdCggX2RlZmF1bHRzICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmICggb3NWaWV3ZXIuem9vbVNsaWRlciApIHtcbiAgICAgICAgICAgICAgICBvc1ZpZXdlci56b29tU2xpZGVyLmluaXQoIF9kZWZhdWx0cyApOyAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgaWYgKCBvc1ZpZXdlci5vdmVybGF5cyApIHtcbiAgICAgICAgICAgICAgICBvc1ZpZXdlci5vdmVybGF5cy5pbml0KCBfZGVmYXVsdHMgKTsgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICB9ICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgXG4gICAgICAgICAgICBpZiAoIG9zVmlld2VyLmRyYXdSZWN0ICkge1xuICAgICAgICAgICAgICAgIG9zVmlld2VyLmRyYXdSZWN0LmluaXQoKTsgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICB9ICAgXG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmICggb3NWaWV3ZXIudHJhbnNmb3JtUmVjdCApIHsgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICBvc1ZpZXdlci50cmFuc2Zvcm1SZWN0LmluaXQoKTsgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICB9ICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgXG4gICAgICAgICAgICBvc1ZpZXdlci5vYnNlcnZhYmxlcy5yZWRyYXdSZXF1aXJlZC5jb25uZWN0KCk7ICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgXG4gICAgICAgICAgICByZXR1cm4gcmVzdWx0LnByb21pc2U7XG4gICAgICAgIH0sXG4gICAgICAgIGdldE9ic2VydmFibGVzOiBmdW5jdGlvbigpIHtcbiAgICAgICAgXHRjb25zb2xlLmxvZyhcIk9ic2VydmFibGVzID0gXCIsIG9zVmlld2VyLm9ic2VydmFibGVzKTtcbiAgICAgICAgXHRyZXR1cm4gb3NWaWV3ZXIub2JzZXJ2YWJsZXM7XG4gICAgICAgIH0sXG4gICAgICAgIGhhc0Zvb3RlcjogZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICByZXR1cm4gX2Zvb3RlckltYWdlICE9IG51bGw7XG4gICAgICAgIH0sXG4gICAgICAgIGdldENvbmZpZzogZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICByZXR1cm4gX2RlZmF1bHRzO1xuICAgICAgICB9LFxuICAgICAgICBsb2FkRm9vdGVyOiBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIGlmICggX2RlZmF1bHRzLmltYWdlLmJhc2VGb290ZXJVcmwgJiYgX2RlZmF1bHRzLmdsb2JhbC5mb290ZXJIZWlnaHQgPiAwICkgeyAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICBfZm9vdGVySW1hZ2UgPSBuZXcgSW1hZ2UoKTtcbiAgICAgICAgICAgICAgICBfZm9vdGVySW1hZ2Uuc3JjID0gX2RlZmF1bHRzLmltYWdlLmJhc2VGb290ZXJVcmwucmVwbGFjZSggXCJ7d2lkdGh9XCIsIE1hdGgucm91bmQoIF9jb250YWluZXIud2lkdGgoKSApICkucmVwbGFjZSggXCJ7aGVpZ2h0fVwiLCBNYXRoLnJvdW5kKCBfZGVmYXVsdHMuZ2xvYmFsLmZvb3RlckhlaWdodCApICk7ICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIF9mb290ZXJJbWFnZS5vbmxvYWQgPSBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggXCJsb2FkaW5nIGZvb3RlciBpbWFnZSBcIiwgX2Zvb3RlckltYWdlICk7XG4gICAgICAgICAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggXCJDYWxjdWxhdGluZyBpbWFnZSBGb290ZXIgc2l6ZVwiICk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgIG9zVmlld2VyLmRyYXdGb290ZXIoKTtcbiAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgfVxuICAgICAgICB9LFxuICAgICAgICBkcmF3Rm9vdGVyOiBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIGlmICggb3NWaWV3ZXIudmlld2VyICkge1xuICAgICAgICAgICAgICAgIF9vdmVybGF5Rm9vdGVyKCk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIG9zVmlld2VyLnZpZXdlci5yZW1vdmVIYW5kbGVyKCAndXBkYXRlLXZpZXdwb3J0JywgX292ZXJsYXlGb290ZXIgKTtcbiAgICAgICAgICAgIG9zVmlld2VyLnZpZXdlci5hZGRIYW5kbGVyKCAndXBkYXRlLXZpZXdwb3J0JywgX292ZXJsYXlGb290ZXIgKTtcbiAgICAgICAgfSwgICAgICAgIFxuICAgICAgICBnZXRPdmVybGF5R3JvdXA6IGZ1bmN0aW9uKCBuYW1lICkge1xuICAgICAgICAgICAgcmV0dXJuIF9kZWZhdWx0cy5nZXRPdmVybGF5R3JvdXAoIG5hbWUgKTtcbiAgICAgICAgfSxcbiAgICAgICAgZ2V0SGlnaGxpZ2h0Q29vcmRpbmF0ZXM6IGZ1bmN0aW9uKCBuYW1lICkge1xuICAgICAgICAgICAgcmV0dXJuIF9kZWZhdWx0cy5nZXRDb29yZGluYXRlcyggbmFtZSApO1xuICAgICAgICB9LFxuICAgICAgICBjcmVhdGVQeXJhbWlkOiBmdW5jdGlvbiggaW1hZ2VJbmZvICkge1xuICAgICAgICAgICAgdmFyIGZpbGVFeHRlbnNpb24gPSBfZGVmYXVsdHMuaW1hZ2UubWltZVR5cGU7XG4gICAgICAgICAgICBmaWxlRXh0ZW5zaW9uID0gZmlsZUV4dGVuc2lvbi5yZXBsYWNlKCBcImltYWdlL1wiLCBcIlwiICk7XG4gICAgICAgICAgICBmaWxlRXh0ZW5zaW9uID0gZmlsZUV4dGVuc2lvbi5yZXBsYWNlKFwianBlZ1wiLCBcImpwZ1wiKS5yZXBsYWNlKFwidGlmZlwiLCBcInRpZlwiKTtcbiAgICAgICAgICAgIHZhciBpbWFnZUxldmVscyA9IFtdO1xuICAgICAgICAgICAgdmFyIHRpbGVTb3VyY2U7XG4gICAgICAgICAgICBpZihBcnJheS5pc0FycmF5KGltYWdlSW5mbykpIHtcbiAgICAgICAgICAgIFx0aW1hZ2VJbmZvLmZvckVhY2goZnVuY3Rpb24obGV2ZWwpIHtcbiAgICAgICAgICAgIFx0XHRsZXZlbC5taW1ldHlwZSA9IF9kZWZhdWx0cy5pbWFnZS5taW1lVHlwZTtcbiAgICAgICAgICAgIFx0fSk7XG4gICAgICAgICAgICBcdHRpbGVTb3VyY2UgPSBuZXcgT3BlblNlYWRyYWdvbi5MZWdhY3lUaWxlU291cmNlKGltYWdlSW5mbyk7XG4gICAgICAgICAgICB9IGVsc2UgaWYoaW1hZ2VJbmZvLnNpemVzKSB7XG5cdCAgICAgICAgICAgIGltYWdlSW5mby5zaXplcy5mb3JFYWNoKGZ1bmN0aW9uKHNpemUpIHtcblx0ICAgICAgICAgICAgICAgIGlmKF9kZWJ1ZykgeyAgICAgICAgICAgICAgICAgICAgXG5cdCAgICAgICAgICAgICAgICAgICAgY29uc29sZS5sb2coXCJJbWFnZSBsZXZlbCB3aWR0aCA9IFwiLCBzaXplLndpZHRoKVxuXHQgICAgICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKFwiSW1hZ2UgbGV2ZWwgaGVpZ2h0ID0gXCIsIHNpemUuaGVpZ2h0KVxuXHQgICAgICAgICAgICAgICAgfVxuXHQgICAgICAgICAgICAgICAgXG5cdCAgICAgICAgICAgICAgICB2YXIgbGV2ZWwgPSB7XG5cdCAgICAgICAgICAgICAgICAgICAgbWltZXR5cGU6IF9kZWZhdWx0cy5pbWFnZS5taW1lVHlwZSxcblx0ICAgICAgICAgICAgICAgICAgICB1cmw6IGltYWdlSW5mb1tcIkBpZFwiXS5yZXBsYWNlKCBcIi9pbmZvLmpzb25cIiwgXCJcIiApICsgXCIvZnVsbC9cIiArIHNpemUud2lkdGggKyBcIiwvMC9kZWZhdWx0LlwiICsgZmlsZUV4dGVuc2lvbixcblx0ICAgICAgICAgICAgICAgICAgICB3aWR0aDogaW1hZ2VJbmZvLndpZHRoLFxuXHQgICAgICAgICAgICAgICAgICAgIGhlaWdodDogaW1hZ2VJbmZvLmhlaWdodFxuXHQgICAgICAgICAgICAgICAgfTtcblx0ICAgICAgICAgICAgICAgIFxuXHQgICAgICAgICAgICAgICAgaWYoX2RlYnVnKSB7XG5cdCAgICAgICAgICAgICAgICAgICAgY29uc29sZS5sb2coXCJDcmVhdGVkIGxldmVsIFwiLCBsZXZlbCk7XG5cdCAgICAgICAgICAgICAgICB9XG5cdCAgICAgICAgICAgICAgICBcblx0ICAgICAgICAgICAgICAgIGltYWdlTGV2ZWxzLnB1c2goIGxldmVsICk7XG5cdCAgICAgICAgICAgIH0pO1xuXHQgICAgICAgICAgICBcblx0ICAgICAgICAgICAgdGlsZVNvdXJjZSA9IG5ldyBPcGVuU2VhZHJhZ29uLkxlZ2FjeVRpbGVTb3VyY2UoaW1hZ2VMZXZlbHMpO1xuICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIFx0dGlsZVNvdXJjZSA9IG5ldyBPcGVuU2VhZHJhZ29uLkltYWdlVGlsZVNvdXJjZSh7XG4gICAgICAgICAgICBcdFx0dXJsOiBpbWFnZUluZm9bXCJAaWRcIl0ucmVwbGFjZSggXCIvaW5mby5qc29uXCIsIFwiXCIgKSArIFwiL2Z1bGwvZnVsbC8wL2RlZmF1bHQuXCIgKyBmaWxlRXh0ZW5zaW9uLFxuICAgICAgICAgICAgXHRcdGNyb3NzT3JpZ2luUG9saWN5OiBcIkFub255bW91c1wiLFxuICAgICAgICAgICAgXHRcdGJ1aWxkUHlyYW1pZDogZmFsc2VcbiAgICAgICAgICAgIFx0fSk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHJldHVybiB0aWxlU291cmNlO1xuICAgICAgICB9LFxuICAgICAgICBnZXRTaXplczogZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICByZXR1cm4gb3NWaWV3ZXIuc2l6ZXM7XG4gICAgICAgIH0sXG4gICAgICAgIGFkZEltYWdlOiBmdW5jdGlvbiggdXJsLCB3aWR0aCwgaGVpZ2h0ICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICdvc1ZpZXdlci5hZGRJbWFnZTogdXJsIC0gJyArIHVybCApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnb3NWaWV3ZXIuYWRkSW1hZ2U6IHdpZHRoIC0gJyArIHdpZHRoICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICdvc1ZpZXdlci5hZGRJbWFnZTogaGVpZ2h0IC0gJyArIGhlaWdodCApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaWYgKCBvc1ZpZXdlci52aWV3ZXIgKSB7XG4gICAgICAgICAgICAgICAgb3NWaWV3ZXIudmlld2VyLmFkZFRpbGVkSW1hZ2UoIHtcbiAgICAgICAgICAgICAgICAgICAgdGlsZVNvdXJjZToge1xuICAgICAgICAgICAgICAgICAgICAgICAgdHlwZTogXCJsZWdhY3ktaW1hZ2UtcHlyYW1pZFwiLFxuICAgICAgICAgICAgICAgICAgICAgICAgbGV2ZWxzOiBbIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB1cmw6IHVybCxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBoZWlnaHQ6IGhlaWdodCxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB3aWR0aDogd2lkdGhcbiAgICAgICAgICAgICAgICAgICAgICAgIH0gXVxuICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICB4OiAwLFxuICAgICAgICAgICAgICAgICAgICB5OiAxLjYsXG4gICAgICAgICAgICAgICAgICAgIHdpZHRoOiAxXG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCBcIlZpZXdlciBub3QgaW5pdGlhbGl6ZWQgeWV0OyBjYW5ub3QgYWRkIGltYWdlXCIgKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH0sXG4gICAgICAgIGdldEltYWdlSW5mbzogZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICBpZihvc1ZpZXdlci52aWV3ZXIpIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gb3NWaWV3ZXIudmlld2VyLnRpbGVTb3VyY2VzO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgcmV0dXJuIG51bGw7XG4gICAgICAgIH0sXG4gICAgICAgIGdldFNjYWxlVG9PcmlnaW5hbFNpemU6IGZ1bmN0aW9uKGltYWdlTm8pIHtcbiAgICAgICAgXHRyZXR1cm4gMS4wO1xuLy8gaWYoIWltYWdlTm8pIHtcbi8vIGltYWdlTm8gPSAwO1xuLy8gfVxuLy8gdmFyIGRpc3BsYXlTaXplID0gb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0Ll9jb250ZW50U2l6ZS54O1xuLy8gcmV0dXJuIG9zVmlld2VyLmdldEltYWdlSW5mbygpW2ltYWdlTm9dLnRpbGVTb3VyY2Uud2lkdGggLyBkaXNwbGF5U2l6ZTtcbiAgICAgICAgfSxcbiAgICAgICAgc2NhbGVUb09yaWdpbmFsU2l6ZTogZnVuY3Rpb24oIHZhbHVlLCBpbWFnZU5vICkge1xuICAgICAgICBcdHJldHVybiB2YWx1ZTtcbi8vIGlmICggX2RlYnVnICkge1xuLy8gY29uc29sZS5sb2coICdPdmVybGF5cyBfc2NhbGVUb09yaWdpbmFsU2l6ZTogdmFsdWUgLSAnICsgdmFsdWUgKTtcbi8vIH1cbi8vICAgICAgICAgICAgXG4vLyBpZighaW1hZ2VObykge1xuLy8gaW1hZ2VObyA9IDA7XG4vLyB9XG4vLyAgICAgICAgICAgIFxuLy8gdmFyIGRpc3BsYXlTaXplID0gb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0Ll9jb250ZW50U2l6ZS54O1xuLy8gcmV0dXJuIHZhbHVlIC8gZGlzcGxheVNpemUgKiBvc1ZpZXdlci5nZXRJbWFnZUluZm8oKVtpbWFnZU5vXS50aWxlU291cmNlLndpZHRoO1xuICAgICAgICB9LFxuICAgICAgICBzY2FsZVRvSW1hZ2VTaXplOiBmdW5jdGlvbiggdmFsdWUsIGltYWdlTm8gKSB7XG4gICAgICAgIFx0cmV0dXJuIHZhbHVlO1xuLy8gaWYgKCBfZGVidWcgKSB7XG4vLyBjb25zb2xlLmxvZyggJ092ZXJsYXlzIF9zY2FsZVRvSW1hZ2VTaXplOiB2YWx1ZSAtICcgKyB2YWx1ZSApO1xuLy8gfVxuLy8gICAgICAgICAgICBcbi8vIGlmKCFpbWFnZU5vKSB7XG4vLyBpbWFnZU5vID0gMDtcbi8vIH1cbi8vICAgICAgICAgICAgXG4vLyB2YXIgZGlzcGxheVNpemUgPSBvc1ZpZXdlci52aWV3ZXIudmlld3BvcnQuX2NvbnRlbnRTaXplLng7XG4vLyByZXR1cm4gdmFsdWUgKiBkaXNwbGF5U2l6ZSAvIG9zVmlld2VyLmdldEltYWdlSW5mbygpW2ltYWdlTm9dLnRpbGVTb3VyY2Uud2lkdGg7XG4gICAgICAgIH0sXG4gICAgICAgIGNsb3NlOiBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCBcIkNsb3Npbmcgb3BlblNlYWRyYWdvbiB2aWV3ZXJcIiApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICBpZiAoIG9zVmlld2VyLnZpZXdlciApIHtcbiAgICAgICAgICAgICAgICBvc1ZpZXdlci52aWV3ZXIuZGVzdHJveSgpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9LFxuICAgICAgICByZWRyYXc6IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgaWYob3NWaWV3ZXIuY29udHJvbHMpIHsgICAgICAgICAgICAgICAgICAgIFx0XG4gICAgICAgICAgICBcdG9zVmlld2VyLmNvbnRyb2xzLnNldFBhbm5pbmcoIHRydWUgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIF9jYWxjdWxhdGVTaXplcyhvc1ZpZXdlcik7XG4gICAgICAgIH0sXG4gICAgICAgIHNldEltYWdlU2l6ZXM6IGZ1bmN0aW9uKGltYWdlSW5mbywgc2l6ZXMpIHtcbiAgICAgICAgXHRpZihzaXplcykgeyAgICAgICAgXHRcdFxuICAgICAgICBcdFx0dmFyIHN0cmluZyA9IHNpemVzLnJlcGxhY2UoL1tcXHtcXH1dLywgXCJcIik7XG4gICAgICAgIFx0XHR2YXIgc2l6ZXMgPSBKU09OLnBhcnNlKHNpemVzKTtcbiAgICAgICAgXHRcdHZhciBpaWlmU2l6ZXMgPSBbXTtcbiAgICAgICAgXHRcdHNpemVzLmZvckVhY2goZnVuY3Rpb24oc2l6ZSkge1xuICAgICAgICBcdFx0XHRpaWlmU2l6ZXMucHVzaCh7XCJ3aWR0aFwiOiBwYXJzZUludChzaXplKSwgXCJoZWlnaHRcIjogcGFyc2VJbnQoc2l6ZSl9KTtcbiAgICAgICAgXHRcdH0pO1xuICAgICAgICBcdFx0aWYoaWlpZlNpemVzLmxlbmd0aCA+IDApIHtcdFx0XHRcdFxuICAgICAgICBcdFx0XHRpbWFnZUluZm8uc2l6ZXMgPSBpaWlmU2l6ZXM7XG4gICAgICAgIFx0XHR9IGVsc2Uge1xuICAgICAgICBcdFx0XHRkZWxldGUgaW1hZ2VJbmZvLnNpemVzO1xuICAgICAgICBcdFx0fVxuICAgICAgICBcdH1cbiAgICAgICAgfSxcbiAgICAgICAgc2V0VGlsZVNpemVzOiBmdW5jdGlvbihpbWFnZUluZm8sIHRpbGVzKSB7XG4gICAgICAgIFx0aWYodGlsZXMpIHsgICAgICAgIFx0XHRcbiAgICAgICAgXHRcdHZhciB0aWxlU3RyaW5nID0gdmlld0ltYWdlLmdldENvbmZpZygpLmdsb2JhbC50aWxlU2l6ZXMucmVwbGFjZSgvKFxcZCspLywgJ1wiJDFcIicpLnJlcGxhY2UoXCI9XCIsIFwiOlwiKTtcbiAgICAgICAgXHRcdHZhciB0aWxlcyA9IEpTT04ucGFyc2UodGlsZVN0cmluZyk7XG4gICAgICAgIFx0XHR2YXIgaWlpZlRpbGVzID0gW107XG4gICAgICAgIFx0XHRcbiAgICAgICAgXHRcdE9iamVjdC5rZXlzKHRpbGVzKS5mb3JFYWNoKGZ1bmN0aW9uKHNpemUpIHtcbiAgICAgICAgXHRcdFx0dmFyIHNjYWxlRmFjdG9ycyA9IHRpbGVzW3NpemVdO1xuICAgICAgICBcdFx0XHRpaWlmVGlsZXMucHVzaCh7XCJ3aWR0aFwiOiBwYXJzZUludChzaXplKSwgXCJoZWlnaHRcIjogcGFyc2VJbnQoc2l6ZSksIFwic2NhbGVGYWN0b3JzXCI6IHNjYWxlRmFjdG9yc30pXG4gICAgICAgIFx0XHR9KTtcbiAgICAgICAgXHRcdFxuICAgICAgICBcdFx0aW1hZ2VJbmZvLnRpbGVzID0gaWlpZlRpbGVzO1xuICAgICAgICBcdH1cbiAgICAgICAgfSxcbiAgICAgICAgb25GaXJzdFRpbGVMb2FkZWQ6IGZ1bmN0aW9uKCkge1xuICAgICAgICBcdHZhciBkZWZlciA9IFEuZGVmZXIoKTtcbiAgICAgICAgXHRcbiAgICAgICAgXHRpZih2aWV3SW1hZ2Uub2JzZXJ2YWJsZXMpIHtcbiAgICAgICAgXHRcdHZpZXdJbWFnZS5vYnNlcnZhYmxlcy5maXJzdFRpbGVMb2FkZWQuc3Vic2NyaWJlKGZ1bmN0aW9uKGV2ZW50KSB7XG4gICAgICAgIFx0XHRcdGRlZmVyLnJlc29sdmUoZXZlbnQpO1xuICAgICAgICBcdFx0fSwgZnVuY3Rpb24oZXJyb3IpIHtcbiAgICAgICAgXHRcdFx0ZGVmZXIucmVqZWN0KGVycm9yKVxuICAgICAgICBcdFx0fSk7XG4gICAgICAgIFx0fSBlbHNlIHtcbiAgICAgICAgXHRcdGRlZmVyLnJlamVjdChcIk5vIG9ic2VydmFibGVzIGRlZmluZWRcIik7XG4gICAgICAgIFx0fVxuICAgICAgICBcdFxuICAgICAgICBcdHJldHVybiBkZWZlci5wcm9taXNlO1xuICAgICAgICB9LFxuICAgICAgICBjcmVhdGVUaWxlU291cmNlOiBmdW5jdGlvbihzb3VyY2UpIHtcblxuICAgICAgICBcdHZhciByZXN1bHQgPSBRLmRlZmVyKCk7XG5cbiAgICAgICAgICAgIHZpZXdJbWFnZS50aWxlU291cmNlUmVzb2x2ZXIucmVzb2x2ZUFzSnNvbihzb3VyY2UpXG4gICAgICAgICAgICAudGhlbihcbiAgICAgICAgICAgIFx0XHRmdW5jdGlvbihpbWFnZUluZm8pIHsgICAgICAgICAgICAgICAgICAgICAgICBcblx0XHQgICAgICAgICAgICAgICAgaWYoX2RlYnVnKSB7ICAgICAgICAgICAgICAgIFxuXHRcdCAgICAgICAgICAgICAgICAgICAgY29uc29sZS5sb2coXCJJSUlGIGltYWdlIGluZm8gXCIsIGltYWdlSW5mbyk7ICAgICAgICAgICAgICAgICAgICAgICAgXG5cdFx0ICAgICAgICAgICAgICAgIH0gICAgICAgICAgICAgICBcblx0XHQgICAgICAgICAgICAgICAgdmlld0ltYWdlLnNldEltYWdlU2l6ZXMoaW1hZ2VJbmZvLCBfZGVmYXVsdHMuZ2xvYmFsLmltYWdlU2l6ZXMpOyAgICAgICBcblx0XHQgICAgICAgICAgICAgICAgdmlld0ltYWdlLnNldFRpbGVTaXplcyhpbWFnZUluZm8sIF9kZWZhdWx0cy5nbG9iYWwudGlsZVNpemVzKTsgICAgICAgICAgICAgICAgXG5cdFx0ICAgICAgICAgICAgICAgIHZhciB0aWxlU291cmNlO1xuXHRcdCAgICAgICAgICAgICAgICBpZihfZGVmYXVsdHMuZ2xvYmFsLnVzZVRpbGVzKSB7XG5cdFx0ICAgICAgICAgICAgICAgICAgICB0aWxlU291cmNlID0gbmV3IE9wZW5TZWFkcmFnb24uSUlJRlRpbGVTb3VyY2UoaW1hZ2VJbmZvKTsgICAgICAgICAgICAgICAgICAgIFxuXHRcdCAgICAgICAgICAgICAgICB9IGVsc2UgeyAgICAgICAgICAgICAgICBcblx0XHQgICAgICAgICAgICAgICAgICAgIHRpbGVTb3VyY2UgID0gb3NWaWV3ZXIuY3JlYXRlUHlyYW1pZChpbWFnZUluZm8pOyAgICAgICAgICAgICAgICAgICAgXG5cdFx0ICAgICAgICAgICAgICAgIH1cblx0XHQgICAgICAgICAgICAgICAgXG5cdFx0ICAgICAgICAgICAgICAgIHJldHVybiB0aWxlU291cmNlOyAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgIFx0XHR9LFxuXHRcdCAgICAgICAgICAgIGZ1bmN0aW9uKGVycm9yKSB7ICAgICAgICAgICAgXG5cdFx0ICAgICAgICAgICAgICAgIGlmKHZpZXdJbWFnZS50aWxlU291cmNlUmVzb2x2ZXIuaXNVUkkoX2RlZmF1bHRzLmltYWdlLnRpbGVTb3VyY2UpKSB7XG5cdFx0ICAgICAgICAgICAgICAgICAgICBpZihfZGVidWcpIHsgICAgICAgICAgICAgICAgICAgIFxuXHRcdCAgICAgICAgICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKFwiSW1hZ2UgVVJMXCIsIF9kZWZhdWx0cy5pbWFnZS50aWxlU291cmNlKTsgICAgICAgICAgICAgICAgICAgICAgICBcblx0XHQgICAgICAgICAgICAgICAgICAgIH1cblx0XHQgICAgICAgICAgICAgICAgICAgIFxuXHRcdCAgICAgICAgICAgICAgICAgICAgdmFyIHRpbGVTb3VyY2UgPSBuZXcgT3BlblNlYWRyYWdvbi5JbWFnZVRpbGVTb3VyY2UoIHsgICAgICAgICAgICAgICAgICAgIFxuXHRcdCAgICAgICAgICAgICAgICAgICAgICAgIHVybDogX2RlZmF1bHRzLmltYWdlLnRpbGVTb3VyY2UsICAgICAgICAgICAgICAgICAgICAgICAgXG5cdFx0ICAgICAgICAgICAgICAgICAgICAgICAgYnVpbGRQeXJhbWlkOiB0cnVlLCAgICAgICAgICAgICAgICAgICAgICAgIFxuXHRcdCAgICAgICAgICAgICAgICAgICAgICAgIGNyb3NzT3JpZ2luUG9saWN5OiBmYWxzZSAgICAgICAgICAgICAgICAgICAgICAgIFxuXHRcdCAgICAgICAgICAgICAgICAgICAgfSApO1xuXHRcdFxuXHRcdCAgICAgICAgICAgICAgICAgICAgcmV0dXJuIHRpbGVTb3VyY2U7ICAgICAgICAgICAgICAgICAgICBcblx0XHQgICAgICAgICAgICAgICAgfSBlbHNlIHsgICAgICAgICAgICAgICAgXG5cdFx0ICAgICAgICAgICAgICAgICAgICB2YXIgZXJyb3JNc2cgPSBcIkZhaWxlZCB0byBsb2FkIHRpbGVzb3VyY2UgZnJvbSBcIiArIHRpbGVTb3VyY2U7XG5cdFx0ICAgICAgICAgICAgICAgICAgICBcblx0XHQgICAgICAgICAgICAgICAgICAgIGlmKF9kZWJ1ZykgeyAgICAgICAgICAgICAgICAgICAgXG5cdFx0ICAgICAgICAgICAgICAgICAgICAgICAgY29uc29sZS5sb2coZXJyb3JNc2cpOyAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICB9XG5cdFx0ICAgICAgICAgICAgICAgICAgICBcblx0XHQgICAgICAgICAgICAgICAgICAgIHJldHVybiBRLnJlamVjdChlcnJvck1zZyk7XG5cdFx0ICAgICAgICAgICAgICAgICAgICBcblx0XHQgICAgICAgICAgICAgICAgfSAgICAgICAgICAgICAgXG5cdFx0ICAgICAgICAgICAgfSlcbiAgICAgICAgICAgIC50aGVuKGZ1bmN0aW9uKHRpbGVTb3VyY2UpIHsgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIHJlc3VsdC5yZXNvbHZlKHRpbGVTb3VyY2UpOyAgICAgICAgICBcbiAgICAgICAgICAgIH0pLmNhdGNoKGZ1bmN0aW9uKGVycm9yTWVzc2FnZSkgeyAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgcmVzdWx0LnJlamVjdChlcnJvck1lc3NhZ2UpOyAgICAgICAgICBcbiAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgcmV0dXJuIHJlc3VsdC5wcm9taXNlO1xuICAgICAgICB9XG4gICAgfTtcbiAgICBcbiAgICBmdW5jdGlvbiBjcmVhdGVPYnNlcnZhYmxlcyh3aW5kb3csIHZpZXdlcikge1xuICAgICAgICB2YXIgb2JzZXJ2YWJsZXMgPSB7fTtcbiAgICAgICAgXG4gICAgICAgIG9ic2VydmFibGVzLnZpZXdlck9wZW4gPSBSeC5PYnNlcnZhYmxlLmNyZWF0ZShmdW5jdGlvbihvYnNlcnZlcikge1xuICAgICAgICAgICAgdmlld2VyLmFkZE9uY2VIYW5kbGVyKCAnb3BlbicsIGZ1bmN0aW9uKCBldmVudCApIHtcbiAgICAgICAgICAgICAgICBldmVudC5vc1N0YXRlID0gXCJvcGVuXCI7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgaWYoTnVtYmVyLmlzTmFOKGV2ZW50LmV2ZW50U291cmNlLnZpZXdwb3J0LmdldEhvbWVCb3VuZHMoKS54KSkge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4gb2JzZXJ2ZXIub25FcnJvcihcIlVua25vdyBlcnJvciBsb2FkaW5nIGltYWdlIGZyb20gXCIsIF9kZWZhdWx0cy5pbWFnZS50aWxlU291cmNlKTtcbiAgICAgICAgICAgICAgICB9IGVsc2UgeyAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgIHJldHVybiBvYnNlcnZlci5vbk5leHQoZXZlbnQpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIHZpZXdlci5hZGRPbmNlSGFuZGxlciggJ29wZW4tZmFpbGVkJywgZnVuY3Rpb24oIGV2ZW50ICkge1xuICAgICAgICAgICAgICAgIGV2ZW50Lm9zU3RhdGUgPSBcIm9wZW4tZmFpbGVkXCI7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gb3BlbiBvcGVuc2VhZHJhZ29uIFwiKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICByZXR1cm4gb2JzZXJ2ZXIub25FcnJvcihldmVudCk7XG4gICAgICAgICAgICB9ICk7XG4gICAgICAgIH0pO1xuICAgICAgICBcbiAgICAgICAgb2JzZXJ2YWJsZXMuZmlyc3RUaWxlTG9hZGVkID0gUnguT2JzZXJ2YWJsZS5jcmVhdGUoZnVuY3Rpb24ob2JzZXJ2ZXIpIHtcbiAgICAgICAgXHR2aWV3ZXIuYWRkT25jZUhhbmRsZXIoICd0aWxlLWxvYWRlZCcsIGZ1bmN0aW9uKCBldmVudCApIHtcbiAgICAgICAgICAgICAgICBldmVudC5vc1N0YXRlID0gXCJ0aWxlLWxvYWRlZFwiO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIHJldHVybiBvYnNlcnZlci5vbk5leHQoZXZlbnQpO1xuICAgICAgICAgICAgfSApO1xuICAgICAgICBcdHZpZXdlci5hZGRPbmNlSGFuZGxlciggJ3RpbGUtbG9hZC1mYWlsZWQnLCBmdW5jdGlvbiggZXZlbnQgKSB7XG4gICAgICAgICAgICAgICAgZXZlbnQub3NTdGF0ZSA9IFwidGlsZS1sb2FkLWZhaWxlZFwiO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGxvYWQgdGlsZVwiKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICByZXR1cm4gb2JzZXJ2ZXIub25FcnJvcihldmVudCk7XG4gICAgICAgICAgICB9ICk7XG4gICAgICAgIH0pO1xuICAgICAgICBcbiAgICAgICAgb2JzZXJ2YWJsZXMudmlld2VyWm9vbSA9IFJ4Lk9ic2VydmFibGUuY3JlYXRlKGZ1bmN0aW9uKG9ic2VydmVyKSB7XG4gICAgICAgICAgICB2aWV3ZXIuYWRkSGFuZGxlciggJ3pvb20nLCBmdW5jdGlvbiggZXZlbnQgKSB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIG9ic2VydmVyLm9uTmV4dChldmVudCk7XG4gICAgICAgICAgICB9ICk7XG4gICAgICAgIH0pO1xuICAgICAgICBvYnNlcnZhYmxlcy5hbmltYXRpb25Db21wbGV0ZSA9IFJ4Lk9ic2VydmFibGUuY3JlYXRlKGZ1bmN0aW9uKG9ic2VydmVyKSB7XG4gICAgICAgICAgICB2aWV3ZXIuYWRkSGFuZGxlciggJ2FuaW1hdGlvbi1maW5pc2gnLCBmdW5jdGlvbiggZXZlbnQgKSB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIG9ic2VydmVyLm9uTmV4dChldmVudCk7XG4gICAgICAgICAgICB9ICk7XG4gICAgICAgIH0pO1xuICAgICAgICBvYnNlcnZhYmxlcy52aWV3cG9ydFVwZGF0ZSA9IFJ4Lk9ic2VydmFibGUuY3JlYXRlKGZ1bmN0aW9uKG9ic2VydmVyKSB7XG4gICAgICAgICAgICB2aWV3ZXIuYWRkSGFuZGxlciggJ3VwZGF0ZS12aWV3cG9ydCcsIGZ1bmN0aW9uKCBldmVudCApIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gb2JzZXJ2ZXIub25OZXh0KGV2ZW50KTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgfSk7XG4gICAgICAgIG9ic2VydmFibGVzLmFuaW1hdGlvbiA9IFJ4Lk9ic2VydmFibGUuY3JlYXRlKGZ1bmN0aW9uKG9ic2VydmVyKSB7XG4gICAgICAgICAgICB2aWV3ZXIuYWRkSGFuZGxlciggJ2FuaW1hdGlvbicsIGZ1bmN0aW9uKCBldmVudCApIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gb2JzZXJ2ZXIub25OZXh0KGV2ZW50KTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgfSk7XG4gICAgICAgIG9ic2VydmFibGVzLnZpZXdlclJvdGF0ZSA9IFJ4Lk9ic2VydmFibGUuY3JlYXRlKGZ1bmN0aW9uKG9ic2VydmVyKSB7XG4gICAgICAgICAgICB2aWV3ZXIuYWRkSGFuZGxlciggJ3JvdGF0ZScsIGZ1bmN0aW9uKCBldmVudCApIHtcbiAgICAgICAgICAgICAgICBldmVudC5vc1N0YXRlID0gXCJyb3RhdGVcIjtcbiAgICAgICAgICAgICAgICByZXR1cm4gb2JzZXJ2ZXIub25OZXh0KGV2ZW50KTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgfSk7XG4gICAgICAgIG9ic2VydmFibGVzLmNhbnZhc1Jlc2l6ZSA9IFJ4Lk9ic2VydmFibGUuY3JlYXRlKGZ1bmN0aW9uKG9ic2VydmVyKSB7XG4gICAgICAgICAgICB2aWV3ZXIuYWRkSGFuZGxlciggJ3Jlc2l6ZScsIGZ1bmN0aW9uKCBldmVudCApIHtcbiAgICAgICAgICAgICAgICBldmVudC5vc1N0YXRlID0gXCJyZXNpemVcIjtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICByZXR1cm4gb2JzZXJ2ZXIub25OZXh0KGV2ZW50KTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgfSk7XG4gICAgICAgIG9ic2VydmFibGVzLndpbmRvd1Jlc2l6ZSA9IFJ4Lk9ic2VydmFibGUuZnJvbUV2ZW50KHdpbmRvdywgXCJyZXNpemVcIikubWFwKGZ1bmN0aW9uKGV2ZW50KSB7XG4gICAgICAgICAgICBldmVudC5vc1N0YXRlID0gXCJ3aW5kb3cgcmVzaXplXCI7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHJldHVybiBldmVudDtcbiAgICAgICAgfSk7XG4gICAgICAgIG9ic2VydmFibGVzLm92ZXJsYXlSZW1vdmUgPSBSeC5PYnNlcnZhYmxlLmNyZWF0ZShmdW5jdGlvbihvYnNlcnZlcikge1xuICAgICAgICAgICAgdmlld2VyLmFkZEhhbmRsZXIoICdyZW1vdmUtb3ZlcmxheScsIGZ1bmN0aW9uKCBldmVudCApIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gb2JzZXJ2ZXIub25OZXh0KGV2ZW50KTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgfSk7XG4gICAgICAgIG9ic2VydmFibGVzLm92ZXJsYXlVcGRhdGUgPSBSeC5PYnNlcnZhYmxlLmNyZWF0ZShmdW5jdGlvbihvYnNlcnZlcikge1xuICAgICAgICAgICAgdmlld2VyLmFkZEhhbmRsZXIoICd1cGRhdGUtb3ZlcmxheScsIGZ1bmN0aW9uKCBldmVudCApIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gb2JzZXJ2ZXIub25OZXh0KGV2ZW50KTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgfSk7XG4gICAgICAgIG9ic2VydmFibGVzLmxldmVsVXBkYXRlID0gUnguT2JzZXJ2YWJsZS5jcmVhdGUoZnVuY3Rpb24ob2JzZXJ2ZXIpIHtcbiAgICAgICAgICAgIHZpZXdlci5hZGRIYW5kbGVyKCAndXBkYXRlLWxldmVsJywgZnVuY3Rpb24oIGV2ZW50ICkge1xuICAgICAgICAgICAgICAgIHJldHVybiBvYnNlcnZlci5vbk5leHQoZXZlbnQpO1xuICAgICAgICAgICAgfSApO1xuICAgICAgICB9KTtcbiAgICAgICAgb2JzZXJ2YWJsZXMucmVkcmF3UmVxdWlyZWQgPSBvYnNlcnZhYmxlcy52aWV3ZXJPcGVuXG4gICAgICAgIC5tZXJnZShvYnNlcnZhYmxlcy52aWV3ZXJSb3RhdGVcbiAgICAgICAgXHRcdC5tZXJnZShvYnNlcnZhYmxlcy5jYW52YXNSZXNpemUpXG4gICAgICAgIFx0XHQuZGVib3VuY2UoMTApKVxuICAgICAgICAubWFwKGZ1bmN0aW9uKGV2ZW50KSB7XG4gICAgICAgICAgICB2YXIgbG9jYXRpb24gPSB7fTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgaWYob3NWaWV3ZXIuY29udHJvbHMpIHtcbiAgICAgICAgICAgICAgICBsb2NhdGlvbiA9IG9zVmlld2VyLmNvbnRyb2xzLmdldExvY2F0aW9uKCk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmKGV2ZW50Lm9zU3RhdGUgPT09IFwib3BlblwiKSB7XG4gICAgICAgICAgICAgICAgbG9jYXRpb24uem9vbSA9IG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC5nZXRIb21lWm9vbSgpO1xuICAgICAgICAgICAgICAgIGlmKF9kZWZhdWx0cy5pbWFnZS5sb2NhdGlvbikge1xuICAgICAgICAgICAgICAgICAgIGxvY2F0aW9uID0gX2RlZmF1bHRzLmltYWdlLmxvY2F0aW9uO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgZXZlbnQudGFyZ2V0TG9jYXRpb24gPSBsb2NhdGlvbjtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgcmV0dXJuIGV2ZW50O1xuICAgICAgICB9KS5wdWJsaXNoKCk7XG4gICAgICAgIFxuICAgICAgICByZXR1cm4gb2JzZXJ2YWJsZXM7XG4gICAgfVxuICAgIFxuICAgIGZ1bmN0aW9uIF9jYWxjdWxhdGVTaXplcyhvc1ZpZXdlcikge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCBcInZpZXdJbWFnZTogY2FsY3VhbHRlIHNpemVzXCIgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKFwiSG9tZSB6b29tID0gXCIsIG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC5nZXRIb21lWm9vbSgpKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgb3NWaWV3ZXIuc2l6ZXMgPSBuZXcgdmlld0ltYWdlLk1lYXN1cmVzKCBvc1ZpZXdlciApO1xuICAgICAgICBcbiAgICAgICAgaWYgKCBfZGVmYXVsdHMuZ2xvYmFsLmFkYXB0Q29udGFpbmVySGVpZ2h0ICkge1xuICAgICAgICAgICAgb3NWaWV3ZXIuc2l6ZXMucmVzaXplQ2FudmFzKCk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIGlmICggb3NWaWV3ZXIudmlld2VyICE9IG51bGwgKSB7XG4gICAgICAgICAgICBvc1ZpZXdlci52aWV3ZXIudmlld3BvcnQuc2V0TWFyZ2lucygge2JvdHRvbTogb3NWaWV3ZXIuc2l6ZXMuZm9vdGVySGVpZ2h0ICsgb3NWaWV3ZXIuc2l6ZXMuY2FsY3VsYXRlRXhjZXNzSGVpZ2h0KCl9ICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coIFwic2l6ZXM6IFwiLCBvc1ZpZXdlci5zaXplcyApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgIH07XG5cbiAgICBcbiAgICBmdW5jdGlvbiBfb3ZlcmxheUZvb3RlciggZXZlbnQgKSB7XG4gICAgICAgIGlmICggX2RlZmF1bHRzLmdsb2JhbC5mb290ZXJIZWlnaHQgPiAwICkge1xuICAgICAgICAgICAgdmFyIGZvb3RlckhlaWdodCA9IF9kZWZhdWx0cy5nbG9iYWwuZm9vdGVySGVpZ2h0O1xuICAgICAgICAgICAgdmFyIGZvb3RlclBvcyA9IG5ldyBPcGVuU2VhZHJhZ29uLlBvaW50KCAwLCBfY29udGFpbmVyLmhlaWdodCgpIC0gZm9vdGVySGVpZ2h0ICk7XG4gICAgICAgICAgICB2YXIgZm9vdGVyU2l6ZSA9IG5ldyBPcGVuU2VhZHJhZ29uLlBvaW50KCBfY29udGFpbmVyLndpZHRoKCksIGZvb3RlckhlaWdodCApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBpZiAoICFfY2FudmFzU2NhbGUgKSB7XG4gICAgICAgICAgICAgICAgX2NhbnZhc1NjYWxlID0gb3NWaWV3ZXIudmlld2VyLmRyYXdlci5jb250ZXh0LmNhbnZhcy53aWR0aCAvIG9zVmlld2VyLnZpZXdlci5kcmF3ZXIuY29udGV4dC5jYW52YXMuY2xpZW50V2lkdGg7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmICggX2NhbnZhc1NjYWxlICE9IDEgKSB7XG4gICAgICAgICAgICAgICAgZm9vdGVyUG9zID0gZm9vdGVyUG9zLnRpbWVzKCBfY2FudmFzU2NhbGUgKTtcbiAgICAgICAgICAgICAgICBmb290ZXJTaXplID0gZm9vdGVyU2l6ZS50aW1lcyggX2NhbnZhc1NjYWxlICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBvc1ZpZXdlci52aWV3ZXIuZHJhd2VyLmNvbnRleHQuZHJhd0ltYWdlKCBfZm9vdGVySW1hZ2UsIGZvb3RlclBvcy54LCBmb290ZXJQb3MueSwgZm9vdGVyU2l6ZS54LCBmb290ZXJTaXplLnkgKTtcbiAgICAgICAgfVxuICAgIH07XG4gICAgXG4gICAgZnVuY3Rpb24gX3RpbWVvdXQocHJvbWlzZSwgdGltZSkge1xuICAgICAgICB2YXIgZGVmZXJyZWQgPSBuZXcgalF1ZXJ5LkRlZmVycmVkKCk7XG5cbiAgICAgICAgJC53aGVuKHByb21pc2UpLmRvbmUoZGVmZXJyZWQucmVzb2x2ZSkuZmFpbChkZWZlcnJlZC5yZWplY3QpLnByb2dyZXNzKGRlZmVycmVkLm5vdGlmeSk7XG5cbiAgICAgICAgc2V0VGltZW91dChmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIGRlZmVycmVkLnJlamVjdChcInRpbWVvdXRcIik7XG4gICAgICAgIH0sIHRpbWUpO1xuXG4gICAgICAgIHJldHVybiBkZWZlcnJlZC5wcm9taXNlKCk7XG4gICAgfVxuICAgIFxuICAgIHJldHVybiBvc1ZpZXdlcjsgICAgXG59XG5cbikoIGpRdWVyeSwgT3BlblNlYWRyYWdvbiApO1xuXG4vLyBicm93c2VyIGJhY2t3YXJkIGNvbXBhYmlsaXR5XG5pZighU3RyaW5nLnByb3RvdHlwZS5zdGFydHNXaXRoKSB7XG4gICAgU3RyaW5nLnByb3RvdHlwZS5zdGFydHNXaXRoID0gZnVuY3Rpb24oc3ViU3RyaW5nKSB7XG4gICAgICAgIHZhciBzdGFydCA9IHRoaXMuc3Vic3RyaW5nKDAsc3ViU3RyaW5nLmxlbmd0aCk7XG4gICAgICAgIHJldHVybiBzdGFydC5sb2NhbGVDb21wYXJlKHN1YlN0cmluZykgPT09IDA7XG4gICAgfVxufVxuaWYoIVN0cmluZy5wcm90b3R5cGUuZW5kc1dpdGgpIHtcbiAgICBTdHJpbmcucHJvdG90eXBlLmVuZHNXaXRoID0gZnVuY3Rpb24oc3ViU3RyaW5nKSB7XG4gICAgICAgIHZhciBzdGFydCA9IHRoaXMuc3Vic3RyaW5nKHRoaXMubGVuZ3RoLXN1YlN0cmluZy5sZW5ndGgsdGhpcy5sZW5ndGgpO1xuICAgICAgICByZXR1cm4gc3RhcnQubG9jYWxlQ29tcGFyZShzdWJTdHJpbmcpID09PSAwO1xuICAgIH1cbn1cbmlmKCFBcnJheS5wcm90b3R5cGUuZmluZCkge1xuICAgIEFycmF5LnByb3RvdHlwZS5maW5kID0gZnVuY3Rpb24oY29tcGFyYXRvcikge1xuICAgICAgICBmb3IgKCB2YXIgaW50ID0gMDsgaW50IDwgdGhpcy5sZW5ndGg7IGludCsrICkge1xuICAgICAgICAgICAgdmFyIGVsZW1lbnQgPSB0aGlzW2ludF07XG4gICAgICAgICAgICBpZihjb21wYXJhdG9yKGVsZW1lbnQpKSB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIGVsZW1lbnQ7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICB9XG59XG5pZighTnVtYmVyLmlzTmFOKSB7XG4gICAgTnVtYmVyLmlzTmFOID0gZnVuY3Rpb24obnVtYmVyKSB7XG4gICAgICAgIHJldHVybiBudW1iZXIgIT09IG51bWJlcjtcbiAgICB9XG59XG4iLCIvKipcbiAqIFRoaXMgZmlsZSBpcyBwYXJ0IG9mIHRoZSBHb29iaSB2aWV3ZXIgLSBhIGNvbnRlbnQgcHJlc2VudGF0aW9uIGFuZCBtYW5hZ2VtZW50XG4gKiBhcHBsaWNhdGlvbiBmb3IgZGlnaXRpemVkIG9iamVjdHMuXG4gKiBcbiAqIFZpc2l0IHRoZXNlIHdlYnNpdGVzIGZvciBtb3JlIGluZm9ybWF0aW9uLiAtIGh0dHA6Ly93d3cuaW50cmFuZGEuY29tIC1cbiAqIGh0dHA6Ly9kaWdpdmVyc28uY29tXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBmcmVlIHNvZnR3YXJlOyB5b3UgY2FuIHJlZGlzdHJpYnV0ZSBpdCBhbmQvb3IgbW9kaWZ5IGl0IHVuZGVyIHRoZSB0ZXJtc1xuICogb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFzIHB1Ymxpc2hlZCBieSB0aGUgRnJlZSBTb2Z0d2FyZSBGb3VuZGF0aW9uOyBlaXRoZXJcbiAqIHZlcnNpb24gMiBvZiB0aGUgTGljZW5zZSwgb3IgKGF0IHlvdXIgb3B0aW9uKSBhbnkgbGF0ZXIgdmVyc2lvbi5cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGRpc3RyaWJ1dGVkIGluIHRoZSBob3BlIHRoYXQgaXQgd2lsbCBiZSB1c2VmdWwsIGJ1dCBXSVRIT1VUIEFOWVxuICogV0FSUkFOVFk7IHdpdGhvdXQgZXZlbiB0aGUgaW1wbGllZCB3YXJyYW50eSBvZiBNRVJDSEFOVEFCSUxJVFkgb3IgRklUTkVTUyBGT1IgQVxuICogUEFSVElDVUxBUiBQVVJQT1NFLiBTZWUgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGZvciBtb3JlIGRldGFpbHMuXG4gKiBcbiAqIFlvdSBzaG91bGQgaGF2ZSByZWNlaXZlZCBhIGNvcHkgb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFsb25nIHdpdGggdGhpc1xuICogcHJvZ3JhbS4gSWYgbm90LCBzZWUgPGh0dHA6Ly93d3cuZ251Lm9yZy9saWNlbnNlcy8+LlxuICogXG4gKiBNb2R1bGUgd2hpY2ggY29udGFpbnMgYWxsIGltYWdlIGluZm9ybWF0aW9ucyBsaWtlIHNpemUsIHNjYWxlIGV0Yy5cbiAqIFxuICogQHZlcnNpb24gMy4yLjBcbiAqIEBtb2R1bGUgdmlld0ltYWdlLk1lYXN1cmVzXG4gKiBAcmVxdWlyZXMgalF1ZXJ5XG4gKi9cbnZhciB2aWV3SW1hZ2UgPSAoIGZ1bmN0aW9uKCBvc1ZpZXdlciApIHtcbiAgICAndXNlIHN0cmljdCc7XG4gICAgXG4gICAgdmFyIF9kZWJ1ZyA9IGZhbHNlO1xuICAgIFxuICAgIG9zVmlld2VyLk1lYXN1cmVzID0gZnVuY3Rpb24oIG9zVmlld2VyICkge1xuICAgICAgICB0aGlzLmNvbmZpZyA9IG9zVmlld2VyLmdldENvbmZpZygpO1xuICAgICAgICB0aGlzLiRjb250YWluZXIgPSAkKCBcIiNcIiArIHRoaXMuY29uZmlnLmdsb2JhbC5kaXZJZCApO1xuICAgICAgICBcbiAgICAgICAgdGhpcy5vdXRlckNhbnZhc1NpemUgPSBuZXcgT3BlblNlYWRyYWdvbi5Qb2ludCggdGhpcy4kY29udGFpbmVyLm91dGVyV2lkdGgoKSwgdGhpcy4kY29udGFpbmVyLm91dGVySGVpZ2h0KCkgKTtcbiAgICAgICAgdGhpcy5pbm5lckNhbnZhc1NpemUgPSBuZXcgT3BlblNlYWRyYWdvbi5Qb2ludCggdGhpcy4kY29udGFpbmVyLndpZHRoKCksIHRoaXMuJGNvbnRhaW5lci5oZWlnaHQoKSApO1xuICAgICAgICB0aGlzLm9yaWdpbmFsSW1hZ2VTaXplID0gbmV3IE9wZW5TZWFkcmFnb24uUG9pbnQoIHRoaXMuZ2V0VG90YWxJbWFnZVdpZHRoKCBvc1ZpZXdlci5nZXRJbWFnZUluZm8oKSApLCB0aGlzLmdldE1heEltYWdlSGVpZ2h0KCBvc1ZpZXdlci5nZXRJbWFnZUluZm8oKSApICk7XG4gICAgICAgIC8vIGNvbnNvbGUubG9nKFwiT3JpZ2luYWwgaW1hZ2Ugc2l6ZSA9IFwiLCB0aGlzLm9yaWdpbmFsSW1hZ2VTaXplKTtcbiAgICAgICAgdGhpcy5mb290ZXJIZWlnaHQgPSB0aGlzLmNvbmZpZy5nbG9iYWwuZm9vdGVySGVpZ2h0O1xuICAgICAgICB0aGlzLnJvdGF0aW9uID0gb3NWaWV3ZXIudmlld2VyICE9IG51bGwgPyBvc1ZpZXdlci52aWV3ZXIudmlld3BvcnQuZ2V0Um90YXRpb24oKSA6IDA7XG4gICAgICAgIHRoaXMueFBhZGRpbmcgPSB0aGlzLm91dGVyQ2FudmFzU2l6ZS54IC0gdGhpcy5pbm5lckNhbnZhc1NpemUueDtcbiAgICAgICAgdGhpcy55UGFkZGluZyA9IHRoaXMub3V0ZXJDYW52YXNTaXplLnkgLSB0aGlzLmlubmVyQ2FudmFzU2l6ZS55O1xuICAgICAgICB0aGlzLmlubmVyQ2FudmFzU2l6ZS55IC09IHRoaXMuZm9vdGVySGVpZ2h0O1xuICAgICAgICBcbiAgICAgICAgLy8gY2FsY3VsYXRlIGltYWdlIHNpemUgYXMgaXQgc2hvdWxkIGJlIGRpc3BsYXllZCBpbiByZWxhdGlvbiB0byBjYW52YXMgc2l6ZVxuICAgICAgICBpZiAoIHRoaXMuZml0VG9IZWlnaHQoKSApIHtcbiAgICAgICAgICAgIHRoaXMuaW1hZ2VEaXNwbGF5U2l6ZSA9IG5ldyBPcGVuU2VhZHJhZ29uLlBvaW50KCB0aGlzLmlubmVyQ2FudmFzU2l6ZS55IC8gdGhpcy5yYXRpbyggdGhpcy5nZXRSb3RhdGVkU2l6ZSggdGhpcy5vcmlnaW5hbEltYWdlU2l6ZSApICksIHRoaXMuaW5uZXJDYW52YXNTaXplLnkgKVxuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgdGhpcy5pbWFnZURpc3BsYXlTaXplID0gbmV3IE9wZW5TZWFkcmFnb24uUG9pbnQoIHRoaXMuaW5uZXJDYW52YXNTaXplLngsIHRoaXMuaW5uZXJDYW52YXNTaXplLnggKiB0aGlzLnJhdGlvKCB0aGlzLmdldFJvdGF0ZWRTaXplKCB0aGlzLm9yaWdpbmFsSW1hZ2VTaXplICkgKSApXG4gICAgICAgIH1cbiAgICAgICAgaWYgKCB0aGlzLnJvdGF0ZWQoKSApIHtcbiAgICAgICAgICAgIHRoaXMuaW1hZ2VEaXNwbGF5U2l6ZSA9IHRoaXMuZ2V0Um90YXRlZFNpemUoIHRoaXMuaW1hZ2VEaXNwbGF5U2l6ZSApO1xuICAgICAgICB9XG4gICAgfTtcbiAgICBvc1ZpZXdlci5NZWFzdXJlcy5wcm90b3R5cGUuZ2V0TWF4SW1hZ2VXaWR0aCA9IGZ1bmN0aW9uKCBpbWFnZUluZm8gKSB7XG4gICAgICAgIHZhciB3aWR0aCA9IDA7XG4gICAgICAgIGlmICggaW1hZ2VJbmZvICYmIGltYWdlSW5mby5sZW5ndGggPiAwICkge1xuICAgICAgICAgICAgZm9yICggdmFyIGkgPSAwOyBpIDwgaW1hZ2VJbmZvLmxlbmd0aDsgaSsrICkge1xuICAgICAgICAgICAgICAgIHZhciB0aWxlU291cmNlID0gaW1hZ2VJbmZvWyBpIF07XG4gICAgICAgICAgICAgICAgaWYgKCB0aWxlU291cmNlLnRpbGVTb3VyY2UgKSB7XG4gICAgICAgICAgICAgICAgICAgIGNvcnJlY3Rpb24gPSB0aWxlU291cmNlLndpZHRoO1xuICAgICAgICAgICAgICAgICAgICB0aWxlU291cmNlID0gdGlsZVNvdXJjZS50aWxlU291cmNlO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB3aWR0aCA9IE1hdGgubWF4KCB3aWR0aCwgdGlsZVNvdXJjZS53aWR0aCAqIGNvcnJlY3Rpb24gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gd2lkdGg7XG4gICAgfTtcbiAgICBvc1ZpZXdlci5NZWFzdXJlcy5wcm90b3R5cGUuZ2V0TWF4SW1hZ2VIZWlnaHQgPSBmdW5jdGlvbiggaW1hZ2VJbmZvICkge1xuICAgICAgICB2YXIgaGVpZ2h0ID0gMDtcbiAgICAgICAgaWYgKCBpbWFnZUluZm8gJiYgaW1hZ2VJbmZvLmxlbmd0aCA+IDAgKSB7XG4gICAgICAgICAgICBmb3IgKCB2YXIgaSA9IDA7IGkgPCBpbWFnZUluZm8ubGVuZ3RoOyBpKysgKSB7XG4gICAgICAgICAgICAgICAgdmFyIHRpbGVTb3VyY2UgPSBpbWFnZUluZm9bIGkgXTtcbiAgICAgICAgICAgICAgICB2YXIgY29ycmVjdGlvbiA9IDE7XG4gICAgICAgICAgICAgICAgaWYgKCB0aWxlU291cmNlLnRpbGVTb3VyY2UgKSB7XG4gICAgICAgICAgICAgICAgICAgIGNvcnJlY3Rpb24gPSB0aWxlU291cmNlLndpZHRoO1xuICAgICAgICAgICAgICAgICAgICB0aWxlU291cmNlID0gdGlsZVNvdXJjZS50aWxlU291cmNlO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBoZWlnaHQgPSBNYXRoLm1heCggaGVpZ2h0LCB0aWxlU291cmNlLmhlaWdodCAqIGNvcnJlY3Rpb24gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gaGVpZ2h0O1xuICAgIH07XG4gICAgb3NWaWV3ZXIuTWVhc3VyZXMucHJvdG90eXBlLmdldFRvdGFsSW1hZ2VXaWR0aCA9IGZ1bmN0aW9uKCBpbWFnZUluZm8gKSB7XG4gICAgICAgIHZhciB3aWR0aCA9IDA7XG4gICAgICAgIGlmICggaW1hZ2VJbmZvICYmIGltYWdlSW5mby5sZW5ndGggPiAwICkge1xuICAgICAgICAgICAgZm9yICggdmFyIGkgPSAwOyBpIDwgaW1hZ2VJbmZvLmxlbmd0aDsgaSsrICkge1xuICAgICAgICAgICAgICAgIHZhciB0aWxlU291cmNlID0gaW1hZ2VJbmZvWyBpIF07XG4gICAgICAgICAgICAgICAgdmFyIGNvcnJlY3Rpb24gPSAxO1xuICAgICAgICAgICAgICAgIGlmICggdGlsZVNvdXJjZS50aWxlU291cmNlICkge1xuICAgICAgICAgICAgICAgICAgICBjb3JyZWN0aW9uID0gdGlsZVNvdXJjZS53aWR0aDtcbiAgICAgICAgICAgICAgICAgICAgdGlsZVNvdXJjZSA9IHRpbGVTb3VyY2UudGlsZVNvdXJjZTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgd2lkdGggKz0gKCB0aWxlU291cmNlLndpZHRoICogY29ycmVjdGlvbiApO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIHJldHVybiB3aWR0aDtcbiAgICB9O1xuICAgIG9zVmlld2VyLk1lYXN1cmVzLnByb3RvdHlwZS5nZXRUb3RhbEltYWdlSGVpZ2h0ID0gZnVuY3Rpb24oIGltYWdlSW5mbyApIHtcbiAgICAgICAgdmFyIGhlaWdodCA9IDA7XG4gICAgICAgIGlmICggaW1hZ2VJbmZvICYmIGltYWdlSW5mby5sZW5ndGggPiAwICkge1xuICAgICAgICAgICAgZm9yICggdmFyIGkgPSAwOyBpIDwgaW1hZ2VJbmZvLmxlbmd0aDsgaSsrICkge1xuICAgICAgICAgICAgICAgIHZhciB0aWxlU291cmNlID0gaW1hZ2VJbmZvWyBpIF07XG4gICAgICAgICAgICAgICAgdmFyIGFzcGVjdFJhdGlvXG4gICAgICAgICAgICAgICAgaWYgKCB0aWxlU291cmNlLnRpbGVTb3VyY2UgKSB7XG4gICAgICAgICAgICAgICAgICAgIGNvcnJlY3Rpb24gPSB0aWxlU291cmNlLndpZHRoO1xuICAgICAgICAgICAgICAgICAgICB0aWxlU291cmNlID0gdGlsZVNvdXJjZS50aWxlU291cmNlO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBoZWlnaHQgKz0gdGlsZVNvdXJjZS5oZWlnaHQgKiBjb3JyZWN0aW9uO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIHJldHVybiBoZWlnaHQ7XG4gICAgfTtcbiAgICBvc1ZpZXdlci5NZWFzdXJlcy5wcm90b3R5cGUuZ2V0SW1hZ2VIb21lU2l6ZSA9IGZ1bmN0aW9uKCkge1xuICAgICAgICB2YXIgcmF0aW8gPSB0aGlzLnJvdGF0ZWQoKSA/IDEgLyB0aGlzLnJhdGlvKCB0aGlzLm9yaWdpbmFsSW1hZ2VTaXplICkgOiB0aGlzLnJhdGlvKCB0aGlzLm9yaWdpbmFsSW1hZ2VTaXplICk7XG4gICAgICAgIGlmICggdGhpcy5maXRUb0hlaWdodCgpICkge1xuICAgICAgICAgICAgdmFyIGhlaWdodCA9IHRoaXMuaW5uZXJDYW52YXNTaXplLnk7XG4gICAgICAgICAgICB2YXIgd2lkdGggPSBoZWlnaHQgLyByYXRpbztcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHZhciB3aWR0aCA9IHRoaXMuaW5uZXJDYW52YXNTaXplLng7XG4gICAgICAgICAgICB2YXIgaGVpZ2h0ID0gd2lkdGggKiByYXRpbztcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gdGhpcy5nZXRSb3RhdGVkU2l6ZSggbmV3IE9wZW5TZWFkcmFnb24uUG9pbnQoIHdpZHRoLCBoZWlnaHQgKSApO1xuICAgIH07XG4gICAgb3NWaWV3ZXIuTWVhc3VyZXMucHJvdG90eXBlLnJvdGF0ZWQgPSBmdW5jdGlvbigpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMucm90YXRpb24gJSAxODAgIT09IDA7XG4gICAgfTtcbiAgICBvc1ZpZXdlci5NZWFzdXJlcy5wcm90b3R5cGUubGFuZHNjYXBlID0gZnVuY3Rpb24oKSB7XG4gICAgICAgIHJldHVybiB0aGlzLnJhdGlvKCB0aGlzLm9yaWdpbmFsSW1hZ2VTaXplICkgPCAxO1xuICAgIH07XG4gICAgb3NWaWV3ZXIuTWVhc3VyZXMucHJvdG90eXBlLnJhdGlvID0gZnVuY3Rpb24oIHNpemUgKSB7XG4gICAgICAgIHJldHVybiBzaXplLnkgLyBzaXplLng7XG4gICAgfTtcbiAgICBvc1ZpZXdlci5NZWFzdXJlcy5wcm90b3R5cGUuZ2V0Um90YXRlZFNpemUgPSBmdW5jdGlvbiggc2l6ZSApIHtcbiAgICAgICAgcmV0dXJuIG5ldyBPcGVuU2VhZHJhZ29uLlBvaW50KCB0aGlzLnJvdGF0ZWQoKSA/IHNpemUueSA6IHNpemUueCwgdGhpcy5yb3RhdGVkKCkgPyBzaXplLnggOiBzaXplLnkgKTtcbiAgICB9O1xuICAgIG9zVmlld2VyLk1lYXN1cmVzLnByb3RvdHlwZS5maXRUb0hlaWdodCA9IGZ1bmN0aW9uKCkge1xuICAgICAgICByZXR1cm4gIXRoaXMuY29uZmlnLmdsb2JhbC5hZGFwdENvbnRhaW5lckhlaWdodCAmJiB0aGlzLnJhdGlvKCB0aGlzLmdldFJvdGF0ZWRTaXplKCB0aGlzLm9yaWdpbmFsSW1hZ2VTaXplICkgKSA+IHRoaXMucmF0aW8oIHRoaXMuaW5uZXJDYW52YXNTaXplICk7XG4gICAgfTtcbiAgICBvc1ZpZXdlci5NZWFzdXJlcy5wcm90b3R5cGUucmVzaXplQ2FudmFzID0gZnVuY3Rpb24oKSB7XG4gICAgICAgIC8vIFNldCBoZWlnaHQgb2YgY29udGFpbmVyIGlmIHJlcXVpcmVkXG4gICAgICAgIGlmICggdGhpcy5jb25maWcuZ2xvYmFsLmFkYXB0Q29udGFpbmVySGVpZ2h0ICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coIFwiYWRhcHQgY29udGFpbmVyIGhlaWdodFwiICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICB0aGlzLiRjb250YWluZXIuaGVpZ2h0KCB0aGlzLmdldFJvdGF0ZWRTaXplKCB0aGlzLmltYWdlRGlzcGxheVNpemUgKS55ICsgdGhpcy5mb290ZXJIZWlnaHQgKTtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLm91dGVyQ2FudmFzU2l6ZSA9IG5ldyBPcGVuU2VhZHJhZ29uLlBvaW50KCB0aGlzLiRjb250YWluZXIub3V0ZXJXaWR0aCgpLCB0aGlzLiRjb250YWluZXIub3V0ZXJIZWlnaHQoKSApO1xuICAgICAgICB0aGlzLmlubmVyQ2FudmFzU2l6ZSA9IG5ldyBPcGVuU2VhZHJhZ29uLlBvaW50KCB0aGlzLiRjb250YWluZXIud2lkdGgoKSwgdGhpcy4kY29udGFpbmVyLmhlaWdodCgpIC0gdGhpcy5mb290ZXJIZWlnaHQgKTtcbiAgICB9O1xuICAgIG9zVmlld2VyLk1lYXN1cmVzLnByb3RvdHlwZS5jYWxjdWxhdGVFeGNlc3NIZWlnaHQgPSBmdW5jdGlvbigpIHtcbiAgICAgICAgdmFyIGltYWdlU2l6ZSA9IHRoaXMuZ2V0Um90YXRlZFNpemUoIHRoaXMuZ2V0SW1hZ2VIb21lU2l6ZSgpICk7XG4gICAgICAgIHZhciBleGNlc3NIZWlnaHQgPSB0aGlzLmNvbmZpZy5nbG9iYWwuYWRhcHRDb250YWluZXJIZWlnaHQgfHwgdGhpcy5maXRUb0hlaWdodCgpID8gMCA6IDAuNSAqICggdGhpcy5pbm5lckNhbnZhc1NpemUueSAtIGltYWdlU2l6ZS55ICk7XG4gICAgICAgIHJldHVybiBleGNlc3NIZWlnaHQ7XG4gICAgfTtcbiAgICBcbiAgICByZXR1cm4gb3NWaWV3ZXI7XG4gICAgXG59ICkoIHZpZXdJbWFnZSB8fCB7fSwgalF1ZXJ5ICk7XG4iLCIvKipcbiAqIFRoaXMgZmlsZSBpcyBwYXJ0IG9mIHRoZSBHb29iaSB2aWV3ZXIgLSBhIGNvbnRlbnQgcHJlc2VudGF0aW9uIGFuZCBtYW5hZ2VtZW50XG4gKiBhcHBsaWNhdGlvbiBmb3IgZGlnaXRpemVkIG9iamVjdHMuXG4gKiBcbiAqIFZpc2l0IHRoZXNlIHdlYnNpdGVzIGZvciBtb3JlIGluZm9ybWF0aW9uLiAtIGh0dHA6Ly93d3cuaW50cmFuZGEuY29tIC1cbiAqIGh0dHA6Ly9kaWdpdmVyc28uY29tXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBmcmVlIHNvZnR3YXJlOyB5b3UgY2FuIHJlZGlzdHJpYnV0ZSBpdCBhbmQvb3IgbW9kaWZ5IGl0IHVuZGVyIHRoZSB0ZXJtc1xuICogb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFzIHB1Ymxpc2hlZCBieSB0aGUgRnJlZSBTb2Z0d2FyZSBGb3VuZGF0aW9uOyBlaXRoZXJcbiAqIHZlcnNpb24gMiBvZiB0aGUgTGljZW5zZSwgb3IgKGF0IHlvdXIgb3B0aW9uKSBhbnkgbGF0ZXIgdmVyc2lvbi5cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGRpc3RyaWJ1dGVkIGluIHRoZSBob3BlIHRoYXQgaXQgd2lsbCBiZSB1c2VmdWwsIGJ1dCBXSVRIT1VUIEFOWVxuICogV0FSUkFOVFk7IHdpdGhvdXQgZXZlbiB0aGUgaW1wbGllZCB3YXJyYW50eSBvZiBNRVJDSEFOVEFCSUxJVFkgb3IgRklUTkVTUyBGT1IgQVxuICogUEFSVElDVUxBUiBQVVJQT1NFLiBTZWUgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGZvciBtb3JlIGRldGFpbHMuXG4gKiBcbiAqIFlvdSBzaG91bGQgaGF2ZSByZWNlaXZlZCBhIGNvcHkgb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFsb25nIHdpdGggdGhpc1xuICogcHJvZ3JhbS4gSWYgbm90LCBzZWUgPGh0dHA6Ly93d3cuZ251Lm9yZy9saWNlbnNlcy8+LlxuICogXG4gKiBNb2R1bGUgd2hpY2ggc2hvd3MgZXhpc3Rpbmcgb3ZlcmxheXMgb24gaW1hZ2VzLlxuICogXG4gKiBAdmVyc2lvbiAzLjIuMFxuICogQG1vZHVsZSB2aWV3SW1hZ2Uub3ZlcmxheXNcbiAqIEByZXF1aXJlcyBqUXVlcnlcbiAqL1xudmFyIHZpZXdJbWFnZSA9ICggZnVuY3Rpb24oIG9zVmlld2VyICkge1xuICAgICd1c2Ugc3RyaWN0JztcbiAgICBcbiAgICB2YXIgX2RlYnVnID0gZmFsc2U7XG4gICAgdmFyIF9mb2N1c1N0eWxlQ2xhc3MgPSAnZm9jdXMnO1xuICAgIHZhciBfaGlnaGxpZ2h0U3R5bGVDbGFzcyA9ICdoaWdobGlnaHQnO1xuICAgIHZhciBfb3ZlcmxheUZvY3VzSG9vayA9IG51bGw7XG4gICAgdmFyIF9vdmVybGF5Q2xpY2tIb29rID0gbnVsbDtcbiAgICB2YXIgX2RyYXdpbmdPdmVybGF5ID0gbnVsbDtcbiAgICB2YXIgX292ZXJsYXlzID0gW107XG4gICAgdmFyIF9kZWZhdWx0cyA9IHt9O1xuICAgIFxuICAgIHZhciBfaW5pdGlhbGl6ZWRDYWxsYmFjayA9IG51bGw7XG5cbiAgICBvc1ZpZXdlci5vdmVybGF5cyA9IHtcbiAgICAgICAgaW5pdDogZnVuY3Rpb24oIGNvbmZpZyApIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnb3NWaWV3ZXIub3ZlcmxheXMuaW5pdCcgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgJC5leHRlbmQoIHRydWUsIF9kZWZhdWx0cywgY29uZmlnICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIG9zVmlld2VyLm9ic2VydmFibGVzLm92ZXJsYXlSZW1vdmUuc3Vic2NyaWJlKGZ1bmN0aW9uKCBldmVudCApIHtcbiAgICAgICAgICAgICAgICBpZiAoIGV2ZW50LmVsZW1lbnQgKSB7XG4gICAgICAgICAgICAgICAgICAgICQoIGV2ZW50LmVsZW1lbnQgKS5yZW1vdmUoKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9KTtcbiAgICAgICAgICAgIGlmKF9kZWZhdWx0cy5pbWFnZS5oaWdobGlnaHRDb29yZHMpIHtcbiAgICAgICAgICAgICAgIFx0b3NWaWV3ZXIub2JzZXJ2YWJsZXMudmlld2VyT3Blbi5zdWJzY3JpYmUoIGZ1bmN0aW9uKCBkYXRhICkge1xuICAgICAgICAgICAgXHRcdGZvciAoIHZhciBpbmRleD0wOyBpbmRleDxfZGVmYXVsdHMuaW1hZ2UuaGlnaGxpZ2h0Q29vcmRzLmxlbmd0aDsgaW5kZXgrKykge1xuICAgICAgICAgICAgXHRcdFx0dmFyIGhpZ2hsaWdodENvb3JkcyA9IF9kZWZhdWx0cy5pbWFnZS5oaWdobGlnaHRDb29yZHNbIGluZGV4IF07XG4gICAgICAgICAgICBcdFx0XHR2YXIgaW1hZ2VJbmRleCA9IGhpZ2hsaWdodENvb3Jkcy5wYWdlSW5kZXg7XG4gICAgICAgICAgICBcdFx0XHRvc1ZpZXdlci5vdmVybGF5cy5kcmF3KCBoaWdobGlnaHRDb29yZHMubmFtZSwgaGlnaGxpZ2h0Q29vcmRzLmRpc3BsYXlUb29sdGlwLCBpbWFnZUluZGV4KTtcbiAgICAgICAgICAgIFx0XHR9XG4gICAgICAgICAgICBcdFx0aWYgKCBfaW5pdGlhbGl6ZWRDYWxsYmFjayApIHtcbiAgICAgICAgICAgIFx0XHRcdF9pbml0aWFsaXplZENhbGxiYWNrKCk7XG4gICAgICAgICAgICBcdFx0fVxuICAgICAgICAgICAgXHR9ICk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0sXG4gICAgICAgIG9uSW5pdGlhbGl6ZWQ6IGZ1bmN0aW9uKCBjYWxsYmFjayApIHtcbiAgICAgICAgICAgIHZhciBvbGRDYWxsYmFjayA9IF9pbml0aWFsaXplZENhbGxiYWNrO1xuICAgICAgICAgICAgX2luaXRpYWxpemVkQ2FsbGJhY2sgPSBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICBpZiAoIG9sZENhbGxiYWNrICkge1xuICAgICAgICAgICAgICAgICAgICBvbGRDYWxsYmFjaygpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBjYWxsYmFjaygpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9LFxuICAgICAgICBvbkZvY3VzOiBmdW5jdGlvbiggaG9vayApIHtcbiAgICAgICAgICAgIHZhciB0ZW1wSG9vayA9IF9vdmVybGF5Rm9jdXNIb29rO1xuICAgICAgICAgICAgX292ZXJsYXlGb2N1c0hvb2sgPSBmdW5jdGlvbiggb3ZlcmxheSwgZm9jdXMgKSB7XG4gICAgICAgICAgICAgICAgaWYgKCB0ZW1wSG9vayApXG4gICAgICAgICAgICAgICAgICAgIHRlbXBIb29rKCBvdmVybGF5LCBmb2N1cyApO1xuICAgICAgICAgICAgICAgIGhvb2soIG92ZXJsYXksIGZvY3VzICk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0sXG4gICAgICAgIG9uQ2xpY2s6IGZ1bmN0aW9uKCBob29rICkge1xuICAgICAgICAgICAgdmFyIHRlbXBIb29rID0gX292ZXJsYXlDbGlja0hvb2s7XG4gICAgICAgICAgICBfb3ZlcmxheUNsaWNrSG9vayA9IGZ1bmN0aW9uKCBvdmVybGF5ICkge1xuICAgICAgICAgICAgICAgIGlmICggdGVtcEhvb2sgKVxuICAgICAgICAgICAgICAgICAgICB0ZW1wSG9vayggb3ZlcmxheSApO1xuICAgICAgICAgICAgICAgIGhvb2soIG92ZXJsYXkgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSxcbiAgICAgICAgZ2V0T3ZlcmxheXM6IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgcmV0dXJuIF9vdmVybGF5cy5zbGljZSgpO1xuICAgICAgICB9LFxuICAgICAgICBnZXRSZWN0czogZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICByZXR1cm4gX292ZXJsYXlzLmZpbHRlciggZnVuY3Rpb24oIG92ZXJsYXkgKSB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIG92ZXJsYXkudHlwZSA9PT0gb3NWaWV3ZXIub3ZlcmxheXMub3ZlcmxheVR5cGVzLlJFQ1RBTkdMRVxuICAgICAgICAgICAgfSApLnNsaWNlKCk7XG4gICAgICAgIH0sXG4gICAgICAgIGdldExpbmVzOiBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIHJldHVybiBfb3ZlcmxheXMuZmlsdGVyKCBmdW5jdGlvbiggb3ZlcmxheSApIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gb3ZlcmxheS50eXBlID09PSBvc1ZpZXdlci5vdmVybGF5cy5vdmVybGF5VHlwZXMuTElORVxuICAgICAgICAgICAgfSApLnNsaWNlKCk7XG4gICAgICAgIH0sXG4gICAgICAgIGRyYXc6IGZ1bmN0aW9uKCBncm91cCwgZGlzcGxheVRpdGxlLCBpbWFnZUluZGV4ICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICdvc1ZpZXdlci5vdmVybGF5cy5kcmF3OiBncm91cCAtICcgKyBncm91cCApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnb3NWaWV3ZXIub3ZlcmxheXMuZHJhdzogZGlzcGxheVRpdGxlIC0gJyArIGRpc3BsYXlUaXRsZSApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnb3NWaWV3ZXIub3ZlcmxheXMuZHJhdzogaW1hZ2VJbmRleCAtICcgKyBpbWFnZUluZGV4ICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHZhciBjb29yZExpc3QgPSBfZGVmYXVsdHMuZ2V0Q29vcmRpbmF0ZXMoIGdyb3VwICk7XG4gICAgICAgICAgICBpZiAoIGNvb3JkTGlzdCApIHtcbiAgICAgICAgICAgICAgICBmb3IgKCB2YXIgaW5kZXg9MDsgaW5kZXg8Y29vcmRMaXN0LmNvb3JkaW5hdGVzLmxlbmd0aDsgaW5kZXgrKyApIHtcbiAgICAgICAgICAgICAgICAgICAgdmFyIGNvb3JkcyA9IGNvb3JkTGlzdC5jb29yZGluYXRlc1sgaW5kZXggXTtcbiAgICAgICAgICAgICAgICAgICAgdmFyIHRpdGxlID0gZGlzcGxheVRpdGxlICYmIGNvb3Jkcy5sZW5ndGggPiA0ID8gY29vcmRzWyA0IF0gOiAnJztcbiAgICAgICAgICAgICAgICAgICAgdmFyIGlkID0gY29vcmRzLmxlbmd0aCA+IDUgPyBjb29yZHNbIDUgXSA6IGluZGV4O1xuICAgICAgICAgICAgICAgICAgICBfY3JlYXRlUmVjdGFuZ2xlKCBjb29yZHNbIDAgXSwgY29vcmRzWyAxIF0sIGNvb3Jkc1sgMiBdIC0gY29vcmRzWyAwIF0sIGNvb3Jkc1sgMyBdIC0gY29vcmRzWyAxIF0sIHRpdGxlLCBpZCwgZ3JvdXAsIGltYWdlSW5kZXggKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH0sXG4gICAgICAgIHVuRHJhdzogZnVuY3Rpb24oIGdyb3VwICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICdvc1ZpZXdlci5vdmVybGF5cy51bkRyYXc6IGdyb3VwIC0gJyArIGdyb3VwICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHZhciBuZXdSZWN0cyA9IFtdO1xuICAgICAgICAgICAgX292ZXJsYXlzID0gX292ZXJsYXlzLmZpbHRlciggZnVuY3Rpb24oIG92ZXJsYXkgKSB7XG4gICAgICAgICAgICAgICAgaWYgKCBvdmVybGF5Lmdyb3VwID09PSBncm91cCApIHtcbiAgICAgICAgICAgICAgICAgICAgb3NWaWV3ZXIudmlld2VyLnJlbW92ZU92ZXJsYXkoIG92ZXJsYXkuZWxlbWVudCApO1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4gZmFsc2U7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4gdHJ1ZTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9ICk7XG4gICAgICAgIH0sXG4gICAgICAgIGhpZ2hsaWdodDogZnVuY3Rpb24oIGdyb3VwICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICdvc1ZpZXdlci5vdmVybGF5cy5oaWdobGlnaHQ6IGdyb3VwIC0gJyArIGdyb3VwICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIF9vdmVybGF5cy5maWx0ZXIoIGZ1bmN0aW9uKCBvdmVybGF5ICkge1xuICAgICAgICAgICAgICAgIHJldHVybiBvdmVybGF5Lmdyb3VwID09PSBncm91cDtcbiAgICAgICAgICAgIH0gKS5mb3JFYWNoKCBmdW5jdGlvbiggb3ZlcmxheSApIHtcbiAgICAgICAgICAgICAgICBpZiAoIG92ZXJsYXkuZWxlbWVudCApIHtcbiAgICAgICAgICAgICAgICAgICAgb3ZlcmxheS5lbGVtZW50LmhpZ2hsaWdodCggdHJ1ZSApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIFxuICAgICAgICB9LFxuICAgICAgICB1bkhpZ2hsaWdodDogZnVuY3Rpb24oIGdyb3VwICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICdvc1ZpZXdlci5vdmVybGF5cy51bkhpZ2hsaWdodDogZ3JvdXAgLSAnICsgZ3JvdXAgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgX292ZXJsYXlzLmZpbHRlciggZnVuY3Rpb24oIG92ZXJsYXkgKSB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIG92ZXJsYXkuZ3JvdXAgPT09IGdyb3VwO1xuICAgICAgICAgICAgfSApLmZvckVhY2goIGZ1bmN0aW9uKCBvdmVybGF5ICkge1xuICAgICAgICAgICAgICAgIGlmICggb3ZlcmxheS5lbGVtZW50ICkge1xuICAgICAgICAgICAgICAgICAgICBvdmVybGF5LmVsZW1lbnQuaGlnaGxpZ2h0KCBmYWxzZSApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIFxuICAgICAgICB9LFxuICAgICAgICBmb2N1c0JveDogZnVuY3Rpb24oIGdyb3VwLCBpZCApIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgXHRjb25zb2xlLmxvZyggJ29zVmlld2VyLm92ZXJsYXlzLmhpZ2hsaWdodEJveDogZ3JvdXAgLSAnICsgZ3JvdXAgKTtcbiAgICAgICAgICAgIFx0Y29uc29sZS5sb2coICdvc1ZpZXdlci5vdmVybGF5cy5oaWdobGlnaHRCb3g6IGlkIC0gJyArIGlkICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBfb3ZlcmxheXMuZmlsdGVyKCBmdW5jdGlvbiggb3ZlcmxheSApIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gb3ZlcmxheS5ncm91cCA9PT0gZ3JvdXA7XG4gICAgICAgICAgICB9ICkuZm9yRWFjaCggZnVuY3Rpb24oIG92ZXJsYXkgKSB7XG4gICAgICAgICAgICAgICAgaWYgKCBvdmVybGF5LmVsZW1lbnQgKSB7XG4gICAgICAgICAgICAgICAgICAgIG92ZXJsYXkuZWxlbWVudC5mb2N1cyggb3ZlcmxheS5pZCA9PT0gaWQgKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICBcbiAgICAgICAgfSxcbiAgICAgICAgYWRkT3ZlcmxheTogZnVuY3Rpb24oIG92ZXJsYXkgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ29zVmlld2VyLm92ZXJsYXlzLmFkZE92ZXJsYXk6IG92ZXJsYXkgLSAnICsgb3ZlcmxheSApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICBfb3ZlcmxheXMucHVzaCggb3ZlcmxheSApO1xuICAgICAgICAgICAgaWYgKCBvdmVybGF5LmVsZW1lbnQgKSB7XG4gICAgICAgICAgICAgICAgb3NWaWV3ZXIudmlld2VyLnVwZGF0ZU92ZXJsYXkoIG92ZXJsYXkuZWxlbWVudCwgb3ZlcmxheS5yZWN0LCAwICk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0sXG4gICAgICAgIHJlbW92ZU92ZXJsYXk6IGZ1bmN0aW9uKCBvdmVybGF5ICkge1xuICAgICAgICAgICAgaWYgKCBvdmVybGF5ICkge1xuICAgICAgICAgICAgICAgIGlmICggX2RlYnVnIClcbiAgICAgICAgICAgICAgICAgICAgY29uc29sZS5sb2coIFwiUmVtb3Zpbmcgb3ZlcmxheSBcIiArIG92ZXJsYXkuaWQgKTtcbiAgICAgICAgICAgICAgICB2YXIgaW5kZXggPSBfb3ZlcmxheXMuaW5kZXhPZiggb3ZlcmxheSApO1xuICAgICAgICAgICAgICAgIF9vdmVybGF5cy5zcGxpY2UoIGluZGV4LCAxICk7XG4gICAgICAgICAgICAgICAgaWYgKCBvdmVybGF5LmVsZW1lbnQgKSB7XG4gICAgICAgICAgICAgICAgICAgIG9zVmlld2VyLnZpZXdlci5yZW1vdmVPdmVybGF5KCBvdmVybGF5LmVsZW1lbnQgKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH0sXG4gICAgICAgIGRyYXdSZWN0OiBmdW5jdGlvbiggcmVjdGFuZ2xlLCBncm91cCwgdGl0bGUsIGlkICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICdvc1ZpZXdlci5vdmVybGF5cy5kcmF3UmVjdDogcmVjdGFuZ2xlIC0gJyArIHJlY3RhbmdsZSApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnb3NWaWV3ZXIub3ZlcmxheXMuZHJhd1JlY3Q6IGdyb3VwIC0gJyArIGdyb3VwICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIF9jcmVhdGVSZWN0YW5nbGUoIHJlY3RhbmdsZS54LCByZWN0YW5nbGUueSwgcmVjdGFuZ2xlLndpZHRoLCByZWN0YW5nbGUuaGVpZ2h0LCB0aXRsZSA/IHRpdGxlIDogXCJcIiwgaWQgPyBpZCA6IFwiXCIsIGdyb3VwICk7XG4gICAgICAgIH0sXG4gICAgICAgIGRyYXdMaW5lOiBmdW5jdGlvbiggcG9pbnQxLCBwb2ludDIsIGdyb3VwICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICdvc1ZpZXdlci5vdmVybGF5cy5kcmF3TGluZTogcG9pbnQxIC0gJyArIHBvaW50MSApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnb3NWaWV3ZXIub3ZlcmxheXMuZHJhd0xpbmU6IHBvaW50MiAtICcgKyBwb2ludDIgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ29zVmlld2VyLm92ZXJsYXlzLmRyYXdMaW5lOiBncm91cCAtICcgKyBncm91cCApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICBfY3JlYXRlTGluZSggcG9pbnQxLngsIHBvaW50MS55LCBwb2ludDIueCwgcG9pbnQyLnksIFwiXCIsIFwiXCIsIGdyb3VwICk7XG4gICAgICAgIH0sXG4gICAgICAgIHNob3dPdmVybGF5OiBmdW5jdGlvbiggb3ZlcmxheSApIHtcbiAgICAgICAgICAgIGlmICggb3ZlcmxheSAmJiAhb3ZlcmxheS5lbGVtZW50ICkge1xuICAgICAgICAgICAgICAgIF9kcmF3T3ZlcmxheSggb3ZlcmxheSApO1xuICAgICAgICAgICAgICAgIGlmICggX292ZXJsYXlGb2N1c0hvb2sgKSB7XG4gICAgICAgICAgICAgICAgICAgIF9vdmVybGF5Rm9jdXNIb29rKCBvdmVybGF5LCB0cnVlICk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgIH0sXG4gICAgICAgIGhpZGVPdmVybGF5OiBmdW5jdGlvbiggb3ZlcmxheSApIHtcbiAgICAgICAgICAgIGlmICggb3ZlcmxheSAmJiBvdmVybGF5LmVsZW1lbnQgJiYgX2RyYXdpbmdPdmVybGF5ICE9IG92ZXJsYXkgKSB7XG4gICAgICAgICAgICAgICAgX3VuZHJhd092ZXJsYXkoIG92ZXJsYXkgKTtcbiAgICAgICAgICAgICAgICBpZiAoIF9vdmVybGF5Rm9jdXNIb29rICkge1xuICAgICAgICAgICAgICAgICAgICBfb3ZlcmxheUZvY3VzSG9vayggb3ZlcmxheSwgZmFsc2UgKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH0sXG4gICAgICAgIGdldE92ZXJsYXk6IGZ1bmN0aW9uKCBpZCwgZ3JvdXAgKSB7XG4gICAgICAgICAgICB2YXIgb3ZlcmxheSA9ICBfb3ZlcmxheXMuZmluZCggZnVuY3Rpb24oIG92ZXJsYXkgKSB7XG4gICAgICAgICAgICAgICAgaWYgKCBncm91cCApIHtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIG92ZXJsYXkuaWQgPT09IGlkICYmIG92ZXJsYXkuZ3JvdXAgPT09IGdyb3VwO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIG92ZXJsYXkuaWQgPT09IGlkXG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfSApO1xuLy8gY29uc29sZS5sb2coXCJzZWFyY2ggZm9yIG92ZXJsYXkgd2l0aCBpZCA9IFwiICsgaWQpO1xuLy8gY29uc29sZS5sb2coXCJGb3VuZCBvdmVybGF5IFwiLCBvdmVybGF5KTtcbiAgICAgICAgICAgIHJldHVybiBvdmVybGF5O1xuICAgICAgICB9LFxuICAgICAgICBnZXRDb29yZGluYXRlczogZnVuY3Rpb24oIG92ZXJsYXkgKSB7XG4gICAgICAgICAgICBpZihfZGVidWcpe1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKFwiZ2V0Q29vcmRpbmF0ZXMgLSBvdmVybGF5XCIsIG92ZXJsYXkpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaWYgKCBvdmVybGF5LnR5cGUgPT09IG9zVmlld2VyLm92ZXJsYXlzLm92ZXJsYXlUeXBlcy5SRUNUQU5HTEUgKSB7XG4gICAgICAgICAgICAgICAgdmFyIHRyYW5zZm9ybWVkUmVjdCA9IG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC52aWV3cG9ydFRvSW1hZ2VSZWN0YW5nbGUoIG92ZXJsYXkucmVjdCApO1xuICAgICAgICAgICAgICAgIHRyYW5zZm9ybWVkUmVjdCA9IHRyYW5zZm9ybWVkUmVjdC50aW1lcyggb3NWaWV3ZXIuZ2V0U2NhbGVUb09yaWdpbmFsU2l6ZSgpICk7XG4gICAgICAgICAgICAgICAgcmV0dXJuIHRyYW5zZm9ybWVkUmVjdDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2UgaWYgKCBvdmVybGF5LnR5cGUgPT09IG9zVmlld2VyLm92ZXJsYXlzLm92ZXJsYXlUeXBlcy5MSU5FICkge1xuICAgICAgICAgICAgICAgIHZhciBwMSA9IG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC52aWV3cG9ydFRvSW1hZ2VDb29yZGluYXRlcyggb3ZlcmxheS5wb2luMSApO1xuICAgICAgICAgICAgICAgIHZhciBwMiA9IG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC52aWV3cG9ydFRvSW1hZ2VDb29yZGluYXRlcyggb3ZlcmxheS5wb2luMiApO1xuICAgICAgICAgICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICAgICAgICAgIHBvaW50MTogcDEsXG4gICAgICAgICAgICAgICAgICAgIHBvaW50MjogcDJcbiAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgfVxuICAgICAgICB9LFxuICAgICAgICBnZXREcmF3aW5nT3ZlcmxheTogZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICByZXR1cm4gX2RyYXdpbmdPdmVybGF5O1xuICAgICAgICB9LFxuICAgICAgICBzZXREcmF3aW5nT3ZlcmxheTogZnVuY3Rpb24oIG92ZXJsYXkgKSB7XG4gICAgICAgICAgICBfZHJhd2luZ092ZXJsYXkgPSBvdmVybGF5O1xuICAgICAgICB9LFxuICAgICAgICBzaG93SGlkZGVuT3ZlcmxheXM6IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgb3NWaWV3ZXIudmlld2VyLmFkZFZpZXdlcklucHV0SG9vaygge1xuICAgICAgICAgICAgICAgIGhvb2tzOiBbIHtcbiAgICAgICAgICAgICAgICAgICAgdHJhY2tlcjogXCJ2aWV3ZXJcIixcbiAgICAgICAgICAgICAgICAgICAgaGFuZGxlcjogXCJtb3ZlSGFuZGxlclwiLFxuICAgICAgICAgICAgICAgICAgICBob29rSGFuZGxlcjogX29uVmlld2VyTW92ZVxuICAgICAgICAgICAgICAgIH0gXVxuICAgICAgICAgICAgfSApO1xuICAgICAgICB9LFxuICAgICAgICBjb250YWluczogZnVuY3Rpb24oIHJlY3QsIHBvaW50LCBwcmVjaXNpb24gKSB7XG4gICAgICAgICAgICBpZiAoIHByZWNpc2lvbiA9PSBudWxsICkge1xuICAgICAgICAgICAgICAgIHByZWNpc2lvbiA9IDA7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICByZXR1cm4gX2lzSW5zaWRlKCByZWN0LCBwb2ludCwgcHJlY2lzaW9uICk7XG4gICAgICAgIH0sXG4gICAgICAgIG92ZXJsYXlUeXBlczoge1xuICAgICAgICAgICAgUkVDVEFOR0xFOiBcInJlY3RhbmdsZVwiLFxuICAgICAgICAgICAgTElORTogXCJsaW5lXCJcbiAgICAgICAgfSxcbiAgICAgICAgZ2V0Um90YXRlZDogZnVuY3Rpb24ocG9pbnQpIHtcbi8vIHZhciByb3RhdGlvbiA9IG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC5nZXRSb3RhdGlvbigpO1xuLy8gdmFyIGNlbnRlciA9IG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC5nZXRDZW50ZXIoIHRydWUgKTtcbi8vIHBvaW50ID0gcG9pbnQucm90YXRlKC1yb3RhdGlvbiwgY2VudGVyKTtcbiAgICAgICAgICAgIHJldHVybiBwb2ludDtcbiAgICAgICAgfVxuICAgIH07XG4gICAgXG4gICAgZnVuY3Rpb24gX2NyZWF0ZUxpbmUoIHgxLCB5MSwgeDIsIHkyLCB0aXRsZSwgaWQsIGdyb3VwICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdPdmVybGF5cyBfY3JlYXRlTGluZTogeDEgLSAnICsgeDEgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnT3ZlcmxheXMgX2NyZWF0ZUxpbmU6IHkxIC0gJyArIHkxICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ092ZXJsYXlzIF9jcmVhdGVMaW5lOiB4MiAtICcgKyB4MiApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdPdmVybGF5cyBfY3JlYXRlTGluZTogeTIgLSAnICsgeTIgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnT3ZlcmxheXMgX2NyZWF0ZUxpbmU6IHRpdGxlIC0gJyArIHRpdGxlICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ092ZXJsYXlzIF9jcmVhdGVMaW5lOiBpZCAtICcgKyBpZCApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdPdmVybGF5cyBfY3JlYXRlTGluZTogZ3JvdXAgLSAnICsgZ3JvdXAgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tJyApO1xuICAgICAgICB9XG4gICAgICAgIHgxID0gb3NWaWV3ZXIuc2NhbGVUb0ltYWdlU2l6ZSggeDEgKTtcbiAgICAgICAgeTEgPSBvc1ZpZXdlci5zY2FsZVRvSW1hZ2VTaXplKCB5MSApO1xuICAgICAgICB4MiA9IG9zVmlld2VyLnNjYWxlVG9JbWFnZVNpemUoIHgyICk7XG4gICAgICAgIHkyID0gb3NWaWV3ZXIuc2NhbGVUb0ltYWdlU2l6ZSggeTIgKTtcbiAgICAgICAgXG4gICAgICAgIHZhciBwMSA9IG5ldyBPcGVuU2VhZHJhZ29uLlBvaW50KCB4MSwgeTEgKTtcbiAgICAgICAgdmFyIHAyID0gbmV3IE9wZW5TZWFkcmFnb24uUG9pbnQoIHgyLCB5MiApO1xuICAgICAgICB2YXIgbGVuZ3RoID0gcDEuZGlzdGFuY2VUbyggcDIgKTtcbiAgICAgICAgXG4gICAgICAgIHZhciBhbmdsZSA9IF9jYWxjdWxhdGVBbmdsZSggcDEsIHAyICk7XG4gICAgICAgIHZhciBiZXRhID0gKCAxODAgLSBhbmdsZSApIC8gMjtcbi8vIGNvbnNvbGUubG9nKCBcImRyYXdpbmcgbGluZSB3aXRoIGxlbmd0aCA9IFwiICsgbGVuZ3RoICsgXCIgYW5kIGFuZ2xlID0gXCIgKyBhbmdsZSApO1xuICAgICAgICBcbiAgICAgICAgeTEgKz0gbGVuZ3RoIC8gMiAqIE1hdGguc2luKCBhbmdsZSAqIE1hdGguUEkgLyAxODAgKTtcbiAgICAgICAgeDEgLT0gbGVuZ3RoIC8gMiAqIE1hdGguc2luKCBhbmdsZSAqIE1hdGguUEkgLyAxODAgKSAvIE1hdGgudGFuKCBiZXRhICogTWF0aC5QSSAvIDE4MCApO1xuIFxuICAgICAgICB2YXIgcmVjdGFuZ2xlID0gb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0LmltYWdlVG9WaWV3cG9ydFJlY3RhbmdsZSggeDEsIHkxLCBsZW5ndGgsIDEgKTtcbiAgICAgICAgdmFyIHAxVmlld2VyID0gb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0LmltYWdlVG9WaWV3cG9ydENvb3JkaW5hdGVzKCBwMSApO1xuICAgICAgICB2YXIgcDJWaWV3ZXIgPSBvc1ZpZXdlci52aWV3ZXIudmlld3BvcnQuaW1hZ2VUb1ZpZXdwb3J0Q29vcmRpbmF0ZXMoIHAyICk7XG4gICAgICAgIHZhciBvdmVybGF5ID0ge1xuICAgICAgICAgICAgdHlwZTogb3NWaWV3ZXIub3ZlcmxheXMub3ZlcmxheVR5cGVzLkxJTkUsXG4gICAgICAgICAgICByZWN0OiByZWN0YW5nbGUsXG4gICAgICAgICAgICBhbmdsZTogYW5nbGUsXG4gICAgICAgICAgICBwb2ludDE6IHAxVmlld2VyLFxuICAgICAgICAgICAgcG9pbnQyOiBwMlZpZXdlcixcbiAgICAgICAgICAgIGdyb3VwOiBncm91cCxcbiAgICAgICAgICAgIGlkOiBpZCxcbiAgICAgICAgICAgIHRpdGxlOiB0aXRsZVxuICAgICAgICB9O1xuICAgICAgICB2YXIgb3ZlcmxheVN0eWxlID0gX2RlZmF1bHRzLmdldE92ZXJsYXlHcm91cCggb3ZlcmxheS5ncm91cCApO1xuICAgICAgICBpZiAoICFvdmVybGF5U3R5bGUuaGlkZGVuICkge1xuICAgICAgICAgICAgX2RyYXdPdmVybGF5KCBvdmVybGF5ICk7XG4gICAgICAgIH1cbiAgICAgICAgX292ZXJsYXlzLnB1c2goIG92ZXJsYXkgKTtcbiAgICAgICAgXG4gICAgfVxuICAgIFxuICAgIC8qKlxuICAgICAqIGNvb3JkaW5hdGVzIGFyZSBpbiBvcmlnaW5hbCBpbWFnZSBzcGFjZVxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9jcmVhdGVSZWN0YW5nbGUoIHgsIHksIHdpZHRoLCBoZWlnaHQsIHRpdGxlLCBpZCwgZ3JvdXAsIGltYWdlSW5kZXggKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICctLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ092ZXJsYXlzIF9jcmVhdGVSZWN0YW5nbGU6IHggLSAnICsgeCApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdPdmVybGF5cyBfY3JlYXRlUmVjdGFuZ2xlOiB5IC0gJyArIHkgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnT3ZlcmxheXMgX2NyZWF0ZVJlY3RhbmdsZTogd2lkdGggLSAnICsgd2lkdGggKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnT3ZlcmxheXMgX2NyZWF0ZVJlY3RhbmdsZTogaGVpZ2h0IC0gJyArIGhlaWdodCApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdPdmVybGF5cyBfY3JlYXRlUmVjdGFuZ2xlOiB0aXRsZSAtICcgKyB0aXRsZSApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdPdmVybGF5cyBfY3JlYXRlUmVjdGFuZ2xlOiBpZCAtICcgKyBpZCApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdPdmVybGF5cyBfY3JlYXRlUmVjdGFuZ2xlOiBncm91cCAtICcgKyBncm91cCApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdPdmVybGF5cyBfY3JlYXRlUmVjdGFuZ2xlOiBpbWFnZUluZGV4IC0gJyArIGltYWdlSW5kZXggKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tJyApO1xuICAgICAgICB9XG4gICAgICAgIHggPSBvc1ZpZXdlci5zY2FsZVRvSW1hZ2VTaXplKCB4ICk7XG4gICAgICAgIHkgPSBvc1ZpZXdlci5zY2FsZVRvSW1hZ2VTaXplKCB5ICk7XG4gICAgICAgIHdpZHRoID0gb3NWaWV3ZXIuc2NhbGVUb0ltYWdlU2l6ZSggd2lkdGggKTtcbiAgICAgICAgaGVpZ2h0ID0gb3NWaWV3ZXIuc2NhbGVUb0ltYWdlU2l6ZSggaGVpZ2h0ICk7XG4gICAgICAgIFxuICAgICAgICBpZighaW1hZ2VJbmRleCkge1xuICAgICAgICBcdGltYWdlSW5kZXggPSAwO1xuICAgICAgICB9XG5cdFx0XHR2YXIgdGlsZWRJbWFnZSA9IG9zVmlld2VyLnZpZXdlci53b3JsZC5nZXRJdGVtQXQoaW1hZ2VJbmRleCk7XG5cdFx0XHR2YXIgcmVjdGFuZ2xlID0gdGlsZWRJbWFnZS5pbWFnZVRvVmlld3BvcnRSZWN0YW5nbGUoIHgsIHksIHdpZHRoLCBoZWlnaHQgKTtcbi8vIGNvbnNvbGUubG9nKFwiRm91bmQgcmVjdCBcIiwgcmVjdGFuZ2xlKTtcbi8vIHZhciByZWN0YW5nbGUgPSBvc1ZpZXdlci52aWV3ZXIudmlld3BvcnQuaW1hZ2VUb1ZpZXdwb3J0UmVjdGFuZ2xlKCB4LCB5LCB3aWR0aCwgaGVpZ2h0XG4vLyApO1xuXHRcdFx0dmFyIG92ZXJsYXkgPSB7XG5cdFx0XHRcdFx0dHlwZTogb3NWaWV3ZXIub3ZlcmxheXMub3ZlcmxheVR5cGVzLlJFQ1RBTkdMRSxcblx0XHRcdFx0XHRyZWN0OiByZWN0YW5nbGUsXG5cdFx0XHRcdFx0Z3JvdXA6IGdyb3VwLFxuXHRcdFx0XHRcdGlkOiBpZCxcblx0XHRcdFx0XHR0aXRsZTogdGl0bGVcblx0XHRcdH07XG5cdFx0XHR2YXIgb3ZlcmxheVN0eWxlID0gX2RlZmF1bHRzLmdldE92ZXJsYXlHcm91cCggb3ZlcmxheS5ncm91cCApO1xuXHRcdFx0aWYgKCAhb3ZlcmxheVN0eWxlLmhpZGRlbiApIHtcblx0XHRcdFx0X2RyYXdPdmVybGF5KCBvdmVybGF5ICk7XG5cdFx0XHR9XG5cdFx0XHRfb3ZlcmxheXMucHVzaCggb3ZlcmxheSApO1xuXG4gICAgICAgIFxuICAgICAgICBcbiAgICB9XG4gICAgXG4gICAgZnVuY3Rpb24gX3VuZHJhd092ZXJsYXkoIG92ZXJsYXkgKSB7XG4gICAgICAgIG9zVmlld2VyLnZpZXdlci5yZW1vdmVPdmVybGF5KCBvdmVybGF5LmVsZW1lbnQgKTtcbiAgICAgICAgb3ZlcmxheS5lbGVtZW50ID0gbnVsbDtcbiAgICB9XG4gICAgXG4gICAgZnVuY3Rpb24gX2RyYXdPdmVybGF5KCBvdmVybGF5ICkge1xuICAgICAgICBpZihfZGVidWcpIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKFwidmlld0ltYWdlLm92ZXJsYXlzLl9kcmF3T3ZlcmxheVwiKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKFwib3ZlcmxheTogXCIsIG92ZXJsYXkpO1xuICAgICAgICB9XG4gICAgICAgIHZhciBlbGVtZW50ID0gZG9jdW1lbnQuY3JlYXRlRWxlbWVudCggXCJkaXZcIiApO1xuICAgICAgICAkKGVsZW1lbnQpLmF0dHIoXCJpZFwiLCBcIm92ZXJsYXlfXCIgKyBvdmVybGF5LmlkKVxuICAgICAgICB2YXIgb3ZlcmxheVN0eWxlID0gX2RlZmF1bHRzLmdldE92ZXJsYXlHcm91cCggb3ZlcmxheS5ncm91cCApO1xuICAgICAgICBpZiAoIG92ZXJsYXlTdHlsZSApIHtcbiAgICAgICAgICAgIGlmKF9kZWJ1Zyljb25zb2xlLmxvZyhcIm92ZXJsYXkgc3R5bGVcIiwgb3ZlcmxheVN0eWxlKTtcbi8vIGVsZW1lbnQudGl0bGUgPSBvdmVybGF5LnRpdGxlO1xuLy8gJCggZWxlbWVudCApLmF0dHIoIFwiZGF0YS10b2dnbGVcIiwgXCJ0b29sdGlwXCIgKTtcbi8vICQoIGVsZW1lbnQgKS5hdHRyKCBcImRhdGEtcGxhY2VtZW50XCIsIFwiYXV0byB0b3BcIiApO1xuICAgICAgICAgICAgJCggZWxlbWVudCApLmFkZENsYXNzKCBvdmVybGF5U3R5bGUuc3R5bGVDbGFzcyApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBpZiAoIG92ZXJsYXkudHlwZSA9PT0gb3NWaWV3ZXIub3ZlcmxheXMub3ZlcmxheVR5cGVzLkxJTkUgKSB7XG4gICAgICAgICAgICAgICAgX3JvdGF0ZSggb3ZlcmxheS5hbmdsZSwgZWxlbWVudCApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICBpZiAoIG92ZXJsYXlTdHlsZS5pbnRlcmFjdGl2ZSApIHtcbiAgICAgICAgICAgICAgICBlbGVtZW50LmZvY3VzID0gZnVuY3Rpb24oIGZvY3VzICkge1xuICAgICAgICAgICAgICAgICAgICBpZiAoIGZvY3VzICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgJCggZWxlbWVudCApLmFkZENsYXNzKCBfZm9jdXNTdHlsZUNsYXNzICk7XG4gICAgICAgICAgICAgICAgICAgICAgICBfY3JlYXRlVG9vbHRpcChlbGVtZW50LCBvdmVybGF5KTtcbiAgICAgICAgICAgICAgICAgICAgICAgIFxuLy8gdG9vbHRpcC5oZWlnaHQoMTAwKTtcbi8vICQoIGVsZW1lbnQgKS50b29sdGlwKCBcInNob3dcIiApO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgJCggZWxlbWVudCApLnJlbW92ZUNsYXNzKCBfZm9jdXNTdHlsZUNsYXNzICk7XG4gICAgICAgICAgICAgICAgICAgICAgICAkKFwiLnRvb2x0aXBwI3Rvb2x0aXBfXCIgKyBvdmVybGF5LmlkKS5yZW1vdmUoKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBpZiAoIF9vdmVybGF5Rm9jdXNIb29rICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgX292ZXJsYXlGb2N1c0hvb2soIG92ZXJsYXksIGZvY3VzICk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIGVsZW1lbnQuaGlnaGxpZ2h0ID0gZnVuY3Rpb24oIGZvY3VzICkge1xuICAgICAgICAgICAgICAgICAgICBpZiAoIGZvY3VzICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgJCggZWxlbWVudCApLmFkZENsYXNzKCBfaGlnaGxpZ2h0U3R5bGVDbGFzcyApO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgJCggZWxlbWVudCApLnJlbW92ZUNsYXNzKCBfaGlnaGxpZ2h0U3R5bGVDbGFzcyApO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAkKCBlbGVtZW50ICkub24oIFwibW91c2VvdmVyXCIsIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnT3ZlcmxheXMgX2RyYXdPdmVybGF5OiBtb3VzZSBvdmVyIC0gJyArIG92ZXJsYXlTdHlsZS5uYW1lICk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgb3NWaWV3ZXIub3ZlcmxheXMuZm9jdXNCb3goIG92ZXJsYXkuZ3JvdXAsIG92ZXJsYXkuaWQgKTtcbiAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgJCggZWxlbWVudCApLm9uKCBcIm1vdXNlb3V0XCIsIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnT3ZlcmxheXMgX2RyYXdPdmVybGF5OiBtb3VzZSBvdXQgLSAnICsgb3ZlcmxheVN0eWxlLm5hbWUgKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBlbGVtZW50LmZvY3VzKCBmYWxzZSApO1xuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICAkKCBlbGVtZW50ICkub24oIFwiY2xpY2tcIiwgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgIGlmICggX292ZXJsYXlDbGlja0hvb2sgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBfb3ZlcmxheUNsaWNrSG9vayggb3ZlcmxheSApO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgb3ZlcmxheS5lbGVtZW50ID0gZWxlbWVudDtcbiAgICAgICAgICAgIG9zVmlld2VyLnZpZXdlci5hZGRPdmVybGF5KCBlbGVtZW50LCBvdmVybGF5LnJlY3QsIDAgKTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBcbiAgICBmdW5jdGlvbiBfY3JlYXRlVG9vbHRpcChlbGVtZW50LCBvdmVybGF5KSB7XG4gICAgXHRpZihvdmVybGF5LnRpdGxlKSB7ICAgIFx0XHRcbiAgICBcdFx0dmFyIGNhbnZhc0Nvcm5lciA9IG9zVmlld2VyLnNpemVzLiRjb250YWluZXIub2Zmc2V0KCk7XG4gICAgXHRcdFxuICAgIFx0XHR2YXIgdG9wID0gJCggZWxlbWVudCApLm9mZnNldCgpLnRvcDtcbiAgICBcdFx0dmFyIGxlZnQgPSAkKCBlbGVtZW50ICkub2Zmc2V0KCkubGVmdDtcbiAgICBcdFx0dmFyIGJvdHRvbSA9IHRvcCArICQoIGVsZW1lbnQgKS5vdXRlckhlaWdodCgpO1xuICAgIFx0XHR2YXIgcmlnaHQgPSBsZWZ0ICsgJCggZWxlbWVudCApLm91dGVyV2lkdGgoKTtcbi8vIGNvbnNvbGUubG9nKFwiVG9vbHRpcCBhdCBcIiwgbGVmdCwgdG9wLCByaWdodCwgYm90dG9tKTtcblxuICAgIFx0XHRcbiAgICBcdFx0dmFyICR0b29sdGlwID0gJChcIjxkaXYgY2xhc3M9J3Rvb2x0aXBwJz5cIiArIG92ZXJsYXkudGl0bGUgKyBcIjwvZGl2PlwiKTtcbiAgICBcdFx0JChcImJvZHlcIikuYXBwZW5kKCR0b29sdGlwKTtcbiAgICBcdFx0dmFyIHRvb2x0aXBQYWRkaW5nID0gcGFyc2VGbG9hdCgkdG9vbHRpcC5jc3MoXCJwYWRkaW5nLXRvcFwiKSk7XG4gICAgXHRcdCR0b29sdGlwLmNzcyhcIm1heC13aWR0aFwiLHJpZ2h0LWxlZnQpO1xuICAgIFx0XHQkdG9vbHRpcC5jc3MoXCJ0b3BcIiwgTWF0aC5tYXgoY2FudmFzQ29ybmVyLnRvcCArIHRvb2x0aXBQYWRkaW5nLCB0b3AtJHRvb2x0aXAub3V0ZXJIZWlnaHQoKS10b29sdGlwUGFkZGluZykpO1xuICAgIFx0XHQkdG9vbHRpcC5jc3MoXCJsZWZ0XCIsIE1hdGgubWF4KGNhbnZhc0Nvcm5lci5sZWZ0ICsgdG9vbHRpcFBhZGRpbmcsIGxlZnQpKTtcbiAgICBcdFx0JHRvb2x0aXAuYXR0cihcImlkXCIsIFwidG9vbHRpcF9cIiArIG92ZXJsYXkuaWQpO1xuLy8gY29uc29sZS5sb2coXCJ0b29sdGlwIHdpZHRoID0gXCIsICR0b29sdGlwLndpZHRoKCkpO1xuICAgIFx0XHRcbiAgICBcdFx0Ly8gbGlzdGVuZXIgZm9yIHpvb21cbiAgICBcdFx0XG4gICAgXHRcdG9zVmlld2VyLm9ic2VydmFibGVzLmFuaW1hdGlvblxuICAgIFx0XHQuZG8oZnVuY3Rpb24oKSB7XG4vLyBjb25zb2xlLmxvZyhcImVsZW1lbnQgYXQ6IFwiLCAkKGVsZW1lbnQpLm9mZnNldCgpKTtcbiAgICBcdFx0XHR2YXIgdG9wID0gTWF0aC5tYXgoJCggZWxlbWVudCApLm9mZnNldCgpLnRvcCwgY2FudmFzQ29ybmVyLnRvcCk7XG4gICAgICAgIFx0XHR2YXIgbGVmdCA9IE1hdGgubWF4KCQoIGVsZW1lbnQgKS5vZmZzZXQoKS5sZWZ0LCBjYW52YXNDb3JuZXIubGVmdCk7XG4gICAgXHRcdFx0JHRvb2x0aXAuY3NzKFwidG9wXCIsIE1hdGgubWF4KGNhbnZhc0Nvcm5lci50b3AgKyB0b29sdGlwUGFkZGluZywgdG9wLSR0b29sdGlwLm91dGVySGVpZ2h0KCktdG9vbHRpcFBhZGRpbmcpKTtcbiAgICBcdFx0XHQkdG9vbHRpcC5jc3MoXCJsZWZ0XCIsIE1hdGgubWF4KGNhbnZhc0Nvcm5lci5sZWZ0ICsgdG9vbHRpcFBhZGRpbmcsIGxlZnQpKTtcbiAgICBcdFx0fSlcbiAgICBcdFx0LnRha2VXaGlsZShmdW5jdGlvbigpIHtcbiAgICBcdFx0XHRyZXR1cm4gJChcIi50b29sdGlwcFwiKS5sZW5ndGggPiAwO1xuICAgIFx0XHR9KVxuICAgIFx0XHQuc3Vic2NyaWJlKCk7XG4gICAgXHR9XG4gICAgfVxuICAgIFxuICAgIGZ1bmN0aW9uIF9yb3RhdGUoIGFuZ2xlLCBtYXBFbGVtZW50ICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnT3ZlcmxheXMgX3JvdGF0ZTogYW5nbGUgLSAnICsgYW5nbGUgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnT3ZlcmxheXMgX3JvdGF0ZTogbWFwRWxlbWVudCAtICcgKyBtYXBFbGVtZW50ICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIGlmICggYW5nbGUgIT09IDAgKSB7XG4gICAgICAgICAgICAkKCBtYXBFbGVtZW50ICkuY3NzKCBcIi1tb3otdHJhbnNmb3JtXCIsIFwicm90YXRlKFwiICsgYW5nbGUgKyBcImRlZylcIiApO1xuICAgICAgICAgICAgJCggbWFwRWxlbWVudCApLmNzcyggXCItd2Via2l0LXRyYW5zZm9ybVwiLCBcInJvdGF0ZShcIiArIGFuZ2xlICsgXCJkZWcpXCIgKTtcbiAgICAgICAgICAgICQoIG1hcEVsZW1lbnQgKS5jc3MoIFwiLW1zLXRyYW5zZm9ybVwiLCBcInJvdGF0ZShcIiArIGFuZ2xlICsgXCJkZWcpXCIgKTtcbiAgICAgICAgICAgICQoIG1hcEVsZW1lbnQgKS5jc3MoIFwiLW8tdHJhbnNmb3JtXCIsIFwicm90YXRlKFwiICsgYW5nbGUgKyBcImRlZylcIiApO1xuICAgICAgICAgICAgJCggbWFwRWxlbWVudCApLmNzcyggXCJ0cmFuc2Zvcm1cIiwgXCJyb3RhdGUoXCIgKyBhbmdsZSArIFwiZGVnKVwiICk7XG4gICAgICAgICAgICB2YXIgc2luID0gTWF0aC5zaW4oIGFuZ2xlICk7XG4gICAgICAgICAgICB2YXIgY29zID0gTWF0aC5jb3MoIGFuZ2xlICk7XG4gICAgICAgICAgICAkKCBtYXBFbGVtZW50ICkuY3NzKFxuICAgICAgICAgICAgICAgICAgICBcImZpbHRlclwiLFxuICAgICAgICAgICAgICAgICAgICBcInByb2dpZDpEWEltYWdlVHJhbnNmb3JtLk1pY3Jvc29mdC5NYXRyaXgoTTExPVwiICsgY29zICsgXCIsIE0xMj1cIiArIHNpbiArIFwiLCBNMjE9LVwiICsgc2luICsgXCIsIE0yMj1cIiArIGNvc1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICsgXCIsIHNpemluZ01ldGhvZD0nYXV0byBleHBhbmQnXCIgKTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBcbiAgICBmdW5jdGlvbiBfY2FsY3VsYXRlQW5nbGUoIHAxLCBwMiApIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ092ZXJsYXlzIF9jYWxjdWxhdGVBbmdsZTogcDEgLSAnICsgcDEgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnT3ZlcmxheXMgX2NhbGN1bGF0ZUFuZ2xlOiBwMiAtICcgKyBwMiApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgZHggPSBwMi54IC0gcDEueDtcbiAgICAgICAgdmFyIGR5ID0gcDIueSAtIHAxLnk7XG4gICAgICAgIHZhciByYWRpYW5zID0gbnVsbDtcbiAgICAgICAgXG4gICAgICAgIGlmICggZHggPiAwICkge1xuICAgICAgICAgICAgcmFkaWFucyA9IE1hdGguYXRhbiggZHkgLyBkeCApO1xuICAgICAgICAgICAgcmV0dXJuIHJhZGlhbnMgKiAxODAgLyBNYXRoLlBJO1xuICAgICAgICB9XG4gICAgICAgIGVsc2UgaWYgKCBkeCA8IDAgKSB7XG4gICAgICAgICAgICByYWRpYW5zID0gTWF0aC5hdGFuKCBkeSAvIGR4ICk7XG4gICAgICAgICAgICByZXR1cm4gcmFkaWFucyAqIDE4MCAvIE1hdGguUEkgKyAxODA7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSBpZiAoIGR5IDwgMCApIHtcbiAgICAgICAgICAgIHJldHVybiAyNzA7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICByZXR1cm4gOTA7XG4gICAgICAgIH1cbiAgICB9XG4gICAgXG4vLyBmdW5jdGlvbiBfZ2V0U2NhbGVUb09yaWdpbmFsU2l6ZSgpIHtcbi8vIHZhciBkaXNwbGF5U2l6ZSA9IG9zVmlld2VyLnZpZXdlci53b3JsZC5nZXRJdGVtQXQoMCkuc291cmNlLmRpbWVuc2lvbnMueDsvL1xuLy8gb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0LmNvbnRlbnRTaXplLng7XG4vLyByZXR1cm4gb3NWaWV3ZXIuZ2V0SW1hZ2VJbmZvKCkud2lkdGggLyBkaXNwbGF5U2l6ZTtcbi8vIH1cbi8vICAgIFxuLy8gZnVuY3Rpb24gX3NjYWxlVG9PcmlnaW5hbFNpemUoIHZhbHVlICkge1xuLy8gaWYgKCBfZGVidWcgKSB7XG4vLyBjb25zb2xlLmxvZyggJ092ZXJsYXlzIF9zY2FsZVRvT3JpZ2luYWxTaXplOiB2YWx1ZSAtICcgKyB2YWx1ZSApO1xuLy8gfVxuLy8gICAgICAgIFxuLy8gdmFyIGRpc3BsYXlTaXplID0gb3NWaWV3ZXIudmlld2VyLndvcmxkLmdldEl0ZW1BdCgwKS5zb3VyY2UuZGltZW5zaW9ucy54Oy8vXG4vLyBvc1ZpZXdlci52aWV3ZXIudmlld3BvcnQuY29udGVudFNpemUueDtcbi8vIHJldHVybiB2YWx1ZSAvIGRpc3BsYXlTaXplICogb3NWaWV3ZXIuZ2V0SW1hZ2VJbmZvKCkud2lkdGg7XG4vLyB9XG4vLyAgICBcbi8vIGZ1bmN0aW9uIF9zY2FsZVRvSW1hZ2VTaXplKCB2YWx1ZSApIHtcbi8vIGlmICggX2RlYnVnICkge1xuLy8gY29uc29sZS5sb2coICdPdmVybGF5cyBfc2NhbGVUb0ltYWdlU2l6ZTogdmFsdWUgLSAnICsgdmFsdWUgKTtcbi8vIH1cbi8vICAgICAgICBcbi8vIHZhciBkaXNwbGF5U2l6ZSA9IG9zVmlld2VyLnZpZXdlci53b3JsZC5nZXRJdGVtQXQoMCkuc291cmNlLmRpbWVuc2lvbnMueDsvL1xuLy8gb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0LmNvbnRlbnRTaXplLng7XG4vLyByZXR1cm4gdmFsdWUgKiBkaXNwbGF5U2l6ZSAvIG9zVmlld2VyLmdldEltYWdlSW5mbygpLndpZHRoO1xuLy8gfVxuICAgIFxuICAgIGZ1bmN0aW9uIF9pc0luc2lkZSggcmVjdCwgcG9pbnQsIGV4dHJhICkge1xuICAgICAgICByZXR1cm4gcG9pbnQueCA+IHJlY3QueCAtIGV4dHJhICYmIHBvaW50LnggPCAoIHJlY3QueCArIHJlY3Qud2lkdGggKyBleHRyYSApICYmIHBvaW50LnkgPiByZWN0LnkgLSBleHRyYVxuICAgICAgICAgICAgICAgICYmIHBvaW50LnkgPCAoIHJlY3QueSArIHJlY3QuaGVpZ2h0ICsgZXh0cmEgKTtcbiAgICB9XG4gICAgXG4gICAgZnVuY3Rpb24gX29uVmlld2VyTW92ZSggZXZlbnQgKSB7IFxuICAgICAgICB2YXIgcG9zaXRpb24gPSBldmVudC5wb3NpdGlvbjtcbiAgICAgICAgdmFyIGllVmVyc2lvbiA9IHZpZXdlckpTLmhlbHBlci5kZXRlY3RJRVZlcnNpb24oKTtcbiAgICAgICAgaWYoaWVWZXJzaW9uICYmIGllVmVyc2lvbiA9PT0gMTApIHtcbi8vIGNvbnNvbGUubG9nKFwiQ29ycmVjdCBwb3NpdGlvbiBmb3IgaWUgXCIsIGllVmVyc2lvbik7XG4gICAgICAgICAgICBwb3NpdGlvbi54ICs9ICQod2luZG93KS5zY3JvbGxMZWZ0KCk7XG4gICAgICAgICAgICBwb3NpdGlvbi55ICs9ICQod2luZG93KS5zY3JvbGxUb3AoKTtcbiAgICAgICAgfVxuLy8gY29uc29sZS5sb2coIFwidmlld2VyIG1vdmUgXCIsIHBvc2l0aW9uKTtcbiAgICAgICAgdmFyIHBvaW50ID0gb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0LnZpZXdlckVsZW1lbnRUb1ZpZXdwb3J0Q29vcmRpbmF0ZXMoIHBvc2l0aW9uICk7XG4gICAgICAgIF9vdmVybGF5cy5mb3JFYWNoKCBmdW5jdGlvbiggbyApIHtcbiAgICAgICAgICAgIGlmICggX2lzSW5zaWRlKCBvLnJlY3QsIHBvaW50LCAwICkgKSB7XG4gICAgICAgICAgICAgICAgb3NWaWV3ZXIub3ZlcmxheXMuc2hvd092ZXJsYXkoIG8gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIG9zVmlld2VyLm92ZXJsYXlzLmhpZGVPdmVybGF5KCBvICk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0gKTtcbiAgICB9XG4gICAgXG4gICAgcmV0dXJuIG9zVmlld2VyO1xuICAgIFxufSApKCB2aWV3SW1hZ2UgfHwge30sIGpRdWVyeSApO1xuIiwiLyoqXG4gKiBUaGlzIGZpbGUgaXMgcGFydCBvZiB0aGUgR29vYmkgdmlld2VyIC0gYSBjb250ZW50IHByZXNlbnRhdGlvbiBhbmQgbWFuYWdlbWVudFxuICogYXBwbGljYXRpb24gZm9yIGRpZ2l0aXplZCBvYmplY3RzLlxuICogXG4gKiBWaXNpdCB0aGVzZSB3ZWJzaXRlcyBmb3IgbW9yZSBpbmZvcm1hdGlvbi4gLSBodHRwOi8vd3d3LmludHJhbmRhLmNvbSAtXG4gKiBodHRwOi8vZGlnaXZlcnNvLmNvbVxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZnJlZSBzb2Z0d2FyZTsgeW91IGNhbiByZWRpc3RyaWJ1dGUgaXQgYW5kL29yIG1vZGlmeSBpdCB1bmRlciB0aGUgdGVybXNcbiAqIG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhcyBwdWJsaXNoZWQgYnkgdGhlIEZyZWUgU29mdHdhcmUgRm91bmRhdGlvbjsgZWl0aGVyXG4gKiB2ZXJzaW9uIDIgb2YgdGhlIExpY2Vuc2UsIG9yIChhdCB5b3VyIG9wdGlvbikgYW55IGxhdGVyIHZlcnNpb24uXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBkaXN0cmlidXRlZCBpbiB0aGUgaG9wZSB0aGF0IGl0IHdpbGwgYmUgdXNlZnVsLCBidXQgV0lUSE9VVCBBTllcbiAqIFdBUlJBTlRZOyB3aXRob3V0IGV2ZW4gdGhlIGltcGxpZWQgd2FycmFudHkgb2YgTUVSQ0hBTlRBQklMSVRZIG9yIEZJVE5FU1MgRk9SIEFcbiAqIFBBUlRJQ1VMQVIgUFVSUE9TRS4gU2VlIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBmb3IgbW9yZSBkZXRhaWxzLlxuICogXG4gKiBZb3Ugc2hvdWxkIGhhdmUgcmVjZWl2ZWQgYSBjb3B5IG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhbG9uZyB3aXRoIHRoaXNcbiAqIHByb2dyYW0uIElmIG5vdCwgc2VlIDxodHRwOi8vd3d3LmdudS5vcmcvbGljZW5zZXMvPi5cbiAqIFxuICogTW9kdWxlIHdoaWNoIGhhbmRsZXMgdGhlIHZpZXdlciByZWFkaW5nIG1vZGUuXG4gKiBcbiAqIEB2ZXJzaW9uIDMuMi4wXG4gKiBAbW9kdWxlIHZpZXdJbWFnZS5yZWFkaW5nTW9kZVxuICogQHJlcXVpcmVzIGpRdWVyeVxuICovXG52YXIgdmlld0ltYWdlID0gKCBmdW5jdGlvbiggb3NWaWV3ZXIgKSB7XG4gICAgJ3VzZSBzdHJpY3QnO1xuICAgIFxuICAgIHZhciBfZGVidWcgPSBmYWxzZTtcbiAgICB2YXIgX2xvY2FsU3RvcmFnZVBvc3NpYmxlID0gdHJ1ZTtcbiAgICB2YXIgX2FjdGl2ZVBhbmVsID0gbnVsbDtcbiAgICB2YXIgX2RlZmF1bHRzID0ge1xuICAgICAgICBuYXZTZWxlY3RvcjogJy5yZWFkaW5nLW1vZGVfX25hdmlnYXRpb24nLFxuICAgICAgICB2aWV3U2VsZWN0b3I6ICcjY29udGVudFZpZXcnLFxuICAgICAgICBpbWFnZUNvbnRhaW5lclNlbGVjdG9yOiAnLnJlYWRpbmctbW9kZV9fY29udGVudC12aWV3LWltYWdlJyxcbiAgICAgICAgaW1hZ2VTZWxlY3RvcjogJyNyZWFkaW5nTW9kZUltYWdlJyxcbiAgICAgICAgc2lkZWJhclNlbGVjdG9yOiAnI2NvbnRlbnRTaWRlYmFyJyxcbiAgICAgICAgc2lkZWJhclRvZ2dsZUJ1dHRvbjogJy5yZWFkaW5nLW1vZGVfX2NvbnRlbnQtc2lkZWJhci10b2dnbGUnLFxuICAgICAgICBzaWRlYmFySW5uZXJTZWxlY3RvcjogJy5yZWFkaW5nLW1vZGVfX2NvbnRlbnQtc2lkZWJhci1pbm5lcicsXG4gICAgICAgIHNpZGViYXJUYWJzU2VsZWN0b3I6ICcucmVhZGluZy1tb2RlX19jb250ZW50LXNpZGViYXItdGFicycsXG4gICAgICAgIHNpZGViYXJUYWJDb250ZW50U2VsZWN0b3I6ICcudGFiLWNvbnRlbnQnLFxuICAgICAgICBzaWRlYmFyVG9jV3JhcHBlclNlbGVjdG9yOiAnLndpZGdldC10b2MtZWxlbS13cmFwcCcsXG4gICAgICAgIHNpZGViYXJTdGF0dXM6ICcnLFxuICAgICAgICB1c2VUYWJzOiB0cnVlLFxuICAgICAgICB1c2VBY2NvcmRlb246IGZhbHNlLFxuICAgICAgICBtc2c6IHt9LFxuICAgIH07XG4gICAgXG4gICAgb3NWaWV3ZXIucmVhZGluZ01vZGUgPSB7XG4gICAgICAgIC8qKlxuICAgICAgICAgKiBNZXRob2QgdG8gaW5pdGlhbGl6ZSB0aGUgdmlld2VyIHJlYWRpbmcgbW9kZS5cbiAgICAgICAgICogXG4gICAgICAgICAqIEBtZXRob2QgaW5pdFxuICAgICAgICAgKiBAcGFyYW0ge09iamVjdH0gY29uZmlnIEFuIGNvbmZpZyBvYmplY3Qgd2hpY2ggb3ZlcndyaXRlcyB0aGUgZGVmYXVsdHMuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcubmF2U2VsZWN0b3IgQSBzdHJpbmcgd2hpY2ggY29udGFpbnMgdGhlIHNlbGVjdG9yIGZvciB0aGVcbiAgICAgICAgICogbmF2aWdhdGlvbi5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy52aWV3U2VsZWN0b3IgQSBzdHJpbmcgd2hpY2ggY29udGFpbnMgdGhlIHNlbGVjdG9yIGZvclxuICAgICAgICAgKiB0aGUgY29udGVudCB2aWV3LlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLmltYWdlQ29udGFpbmVyU2VsZWN0b3IgQSBzdHJpbmcgd2hpY2ggY29udGFpbnMgdGhlXG4gICAgICAgICAqIHNlbGVjdG9yIGZvciB0aGUgaW1hZ2UgY29udGFpbmVyLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLmltYWdlU2VsZWN0b3IgQSBzdHJpbmcgd2hpY2ggY29udGFpbnMgdGhlIHNlbGVjdG9yIGZvclxuICAgICAgICAgKiB0aGUgaW1hZ2UuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcuc2lkZWJhclNlbGVjdG9yIEEgc3RyaW5nIHdoaWNoIGNvbnRhaW5zIHRoZSBzZWxlY3RvciBmb3JcbiAgICAgICAgICogdGhlIHNpZGViYXIuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcuc2lkZWJhclRvZ2dsZUJ1dHRvbiBBIHN0cmluZyB3aGljaCBjb250YWlucyB0aGUgc2VsZWN0b3JcbiAgICAgICAgICogZm9yIHRoZSBzaWRlYmFyIHRvZ2dsZSBidXR0b24uXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcuc2lkZWJhcklubmVyU2VsZWN0b3IgQSBzdHJpbmcgd2hpY2ggY29udGFpbnMgdGhlXG4gICAgICAgICAqIHNlbGVjdG9yIGZvciB0aGUgaW5uZXIgc2lkZWJhciBjb250YWluZXIuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcuc2lkZWJhclN0YXR1cyBBIHN0cmluZyB3aGljaCBjb250YWlucyB0aGUgY3VycmVudFxuICAgICAgICAgKiBzaWRlYmFyIHN0YXR1cy5cbiAgICAgICAgICovXG4gICAgICAgIGluaXQ6IGZ1bmN0aW9uKCBjb25maWcgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ29zVmlld2VyLnJlYWRpbmdNb2RlLmluaXQnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICcjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICdvc1ZpZXdlci5yZWFkaW5nTW9kZS5pbml0OiBjb25maWcgLSAnLCBjb25maWcgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgJC5leHRlbmQoIHRydWUsIF9kZWZhdWx0cywgY29uZmlnICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIGNoZWNrIGxvY2FsIHN0b3JhZ2VcbiAgICAgICAgICAgIF9sb2NhbFN0b3JhZ2VQb3NzaWJsZSA9IHZpZXdlckpTLmhlbHBlci5jaGVja0xvY2FsU3RvcmFnZSgpO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBpZiAoIF9sb2NhbFN0b3JhZ2VQb3NzaWJsZSApIHtcbiAgICAgICAgICAgICAgICBfZGVmYXVsdHMuc2lkZWJhclN0YXR1cyA9IGxvY2FsU3RvcmFnZS5nZXRJdGVtKCAnc2lkZWJhclN0YXR1cycgKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICBpZiAoIF9kZWZhdWx0cy5zaWRlYmFyU3RhdHVzID09PSAnJyB8fCBfZGVmYXVsdHMuc2lkZWJhclN0YXR1cyA9PT0gdW5kZWZpbmVkICkge1xuICAgICAgICAgICAgICAgICAgICBsb2NhbFN0b3JhZ2Uuc2V0SXRlbSggJ3NpZGViYXJTdGF0dXMnLCAndHJ1ZScgKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLy8gc2V0IHZpZXdwb3J0XG4gICAgICAgICAgICAgICAgX3NldFZpZXdwb3J0SGVpZ2h0KCk7XG4gICAgICAgICAgICAgICAgaWYgKCBfZGVmYXVsdHMudXNlVGFicyApIHtcbiAgICAgICAgICAgICAgICAgICAgX3NldFNpZGViYXJUYWJIZWlnaHQoKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgX3NldFNpZGViYXJCdXR0b25Qb3NpdGlvbigpO1xuICAgICAgICAgICAgICAgIF9jaGVja1NpZGViYXJTdGF0dXMoKTtcbiAgICAgICAgICAgICAgICBzZXRUaW1lb3V0KCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgX3Nob3dDb250ZW50KCk7XG4gICAgICAgICAgICAgICAgfSwgNTAwICk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLy8gc2F2ZSBwYW5lbCBzdGF0dXNcbiAgICAgICAgICAgICAgICBpZiAoIF9kZWZhdWx0cy51c2VBY2NvcmRlb24gKSB7XG4gICAgICAgICAgICAgICAgICAgIF9hY3RpdmVQYW5lbCA9IGxvY2FsU3RvcmFnZS5nZXRJdGVtKCAnYWN0aXZlUGFuZWwnICk7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAkKCAnLnBhbmVsLWNvbGxhcHNlJyApLmVhY2goIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgJCggdGhpcyApLnJlbW92ZUNsYXNzKCAnaW4nICk7XG4gICAgICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgIGlmICggX2FjdGl2ZVBhbmVsID09PSBudWxsICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgbG9jYWxTdG9yYWdlLnNldEl0ZW0oICdhY3RpdmVQYW5lbCcsICcjY29sbGFwc2VPbmUnICk7XG4gICAgICAgICAgICAgICAgICAgICAgICBfYWN0aXZlUGFuZWwgPSBsb2NhbFN0b3JhZ2UuZ2V0SXRlbSggJ2FjdGl2ZVBhbmVsJyApO1xuICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICAkKCBfYWN0aXZlUGFuZWwgKS5hZGRDbGFzcyggJ2luJyApO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgJCggX2FjdGl2ZVBhbmVsICkuYWRkQ2xhc3MoICdpbicgKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgLy8gY2xpY2sgcGFuZWwgZXZlbnRcbiAgICAgICAgICAgICAgICAgICAgJCggJ2FbZGF0YS10b2dnbGU9XCJjb2xsYXBzZVwiXScgKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICB2YXIgY3VyclBhbmVsID0gJCggdGhpcyApLmF0dHIoICdocmVmJyApO1xuICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICBsb2NhbFN0b3JhZ2Uuc2V0SXRlbSggJ2FjdGl2ZVBhbmVsJywgY3VyclBhbmVsICk7XG4gICAgICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLy8gZXZlbnRzXG4gICAgICAgICAgICAgICAgJCggJ1tkYXRhLXRvZ2dsZT1cInNpZGViYXJcIl0nICkub24oICdjbGljaycsIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICAkKCB0aGlzICkudG9nZ2xlQ2xhc3MoICdpbicgKTtcbiAgICAgICAgICAgICAgICAgICAgJCggdGhpcyApLnBhcmVudHMoICcucmVhZGluZy1tb2RlX19jb250ZW50LXNpZGViYXInICkudG9nZ2xlQ2xhc3MoICdpbicgKTtcbiAgICAgICAgICAgICAgICAgICAgJCggdGhpcyApLnBhcmVudHMoICcucmVhZGluZy1tb2RlX19jb250ZW50LXNpZGViYXInICkucHJldigpLnRvZ2dsZUNsYXNzKCAnaW4nICk7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAvLyBzZXQgc2lkZWJhciBzdGF0dXMgdG8gbG9jYWwgc3RvcmFnZVxuICAgICAgICAgICAgICAgICAgICBfZGVmYXVsdHMuc2lkZWJhclN0YXR1cyA9IGxvY2FsU3RvcmFnZS5nZXRJdGVtKCAnc2lkZWJhclN0YXR1cycgKTtcbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgIGlmICggX2RlZmF1bHRzLnNpZGViYXJTdGF0dXMgPT09ICdmYWxzZScgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBsb2NhbFN0b3JhZ2Uuc2V0SXRlbSggJ3NpZGViYXJTdGF0dXMnLCAndHJ1ZScgKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGxvY2FsU3RvcmFnZS5zZXRJdGVtKCAnc2lkZWJhclN0YXR1cycsICdmYWxzZScgKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAkKCB3aW5kb3cgKS5vbiggJ3Jlc2l6ZScsIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICBfc2V0Vmlld3BvcnRIZWlnaHQoKTtcbiAgICAgICAgICAgICAgICAgICAgaWYgKCBfZGVmYXVsdHMudXNlVGFicyApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIF9zZXRTaWRlYmFyVGFiSGVpZ2h0KCk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgX3NldFNpZGViYXJCdXR0b25Qb3NpdGlvbigpO1xuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAkKCB3aW5kb3cgKS5vbiggXCJvcmllbnRhdGlvbmNoYW5nZVwiLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgX3NldFZpZXdwb3J0SGVpZ2h0KCk7XG4gICAgICAgICAgICAgICAgICAgIGlmICggX2RlZmF1bHRzLnVzZVRhYnMgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBfc2V0U2lkZWJhclRhYkhlaWdodCgpO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIF9zZXRTaWRlYmFyQnV0dG9uUG9zaXRpb24oKTtcbiAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLy8gQUpBWCBMb2FkZXIgRXZlbnRsaXN0ZW5lclxuICAgICAgICAgICAgICAgIGlmICggdHlwZW9mIGpzZiAhPT0gJ3VuZGVmaW5lZCcgKSB7XG4gICAgICAgICAgICAgICAgICAgIGpzZi5hamF4LmFkZE9uRXZlbnQoIGZ1bmN0aW9uKCBkYXRhICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgdmFyIGFqYXhzdGF0dXMgPSBkYXRhLnN0YXR1cztcbiAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgc3dpdGNoICggYWpheHN0YXR1cyApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBjYXNlIFwic3VjY2Vzc1wiOlxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBfc2V0Vmlld3BvcnRIZWlnaHQoKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaWYgKCBfZGVmYXVsdHMudXNlVGFicyApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIF9zZXRTaWRlYmFyVGFiSGVpZ2h0KCk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgX3NldFNpZGViYXJCdXR0b25Qb3NpdGlvbigpO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIHJldHVybiBmYWxzZTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSxcbiAgICB9O1xuICAgIFxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB3aGljaCBzZXRzIHRoZSBoZWlnaHQgb2YgdGhlIHZpZXdwb3J0IGVsZW1lbnRzLlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX3NldFZpZXdwb3J0SGVpZ2h0XG4gICAgICovXG4gICAgZnVuY3Rpb24gX3NldFZpZXdwb3J0SGVpZ2h0KCkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfc2V0Vmlld3BvcnRIZWlnaHQoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfc2V0Vmlld3BvcnRIZWlnaHQ6IHZpZXcgPSAnLCBfZGVmYXVsdHMudmlld1NlbGVjdG9yICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19zZXRWaWV3cG9ydEhlaWdodDogaW1hZ2UgPSAnLCBfZGVmYXVsdHMuaW1hZ2VTZWxlY3RvciApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfc2V0Vmlld3BvcnRIZWlnaHQ6IHNpZGViYXIgPSAnLCBfZGVmYXVsdHMuc2lkZWJhclNlbGVjdG9yICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19zZXRWaWV3cG9ydEhlaWdodDogc2lkZWJhcklubmVyID0gJywgX2RlZmF1bHRzLnNpZGViYXJJbm5lclNlbGVjdG9yICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19zZXRWaWV3cG9ydEhlaWdodDogc2lkZWJhclRhYnMgPSAnLCBfZGVmYXVsdHMuc2lkZWJhclRhYnNTZWxlY3RvciApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgdmlld3BvcnRIZWlnaHQgPSAkKCB3aW5kb3cgKS5vdXRlckhlaWdodCgpO1xuICAgICAgICB2YXIgbmF2SGVpZ2h0ID0gJCggX2RlZmF1bHRzLm5hdlNlbGVjdG9yICkub3V0ZXJIZWlnaHQoKTtcbiAgICAgICAgdmFyIG5ld0hlaWdodCA9IHZpZXdwb3J0SGVpZ2h0IC0gbmF2SGVpZ2h0O1xuICAgICAgICBcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19zZXRWaWV3cG9ydEhlaWdodDogdmlld3BvcnRIZWlnaHQgPSAnLCB2aWV3cG9ydEhlaWdodCApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfc2V0Vmlld3BvcnRIZWlnaHQ6IG5hdkhlaWdodCA9ICcsIG5hdkhlaWdodCApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfc2V0Vmlld3BvcnRIZWlnaHQ6IG5ld0hlaWdodCA9ICcsIG5ld0hlaWdodCApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICAkKCBfZGVmYXVsdHMudmlld1NlbGVjdG9yICkuY3NzKCAnaGVpZ2h0JywgbmV3SGVpZ2h0ICk7XG4gICAgICAgICQoIF9kZWZhdWx0cy5pbWFnZVNlbGVjdG9yICkuY3NzKCAnaGVpZ2h0JywgbmV3SGVpZ2h0ICk7XG4gICAgICAgICQoIF9kZWZhdWx0cy5zaWRlYmFyU2VsZWN0b3IgKS5jc3MoICdoZWlnaHQnLCBuZXdIZWlnaHQgKTtcbiAgICAgICAgJCggX2RlZmF1bHRzLnNpZGViYXJJbm5lclNlbGVjdG9yICkuY3NzKCAnaGVpZ2h0JywgbmV3SGVpZ2h0ICk7XG4gICAgICAgIFxuICAgIH1cbiAgICBcbiAgICAvKipcbiAgICAgKiBNZXRob2Qgd2hpY2ggc2V0cyB0aGUgaGVpZ2h0IG9mIHRoZSBzaWRlYmFyIFRhYnMuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfc2V0U2lkZWJhclRhYkhlaWdodFxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9zZXRTaWRlYmFyVGFiSGVpZ2h0KCkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfc2V0U2lkZWJhclRhYkhlaWdodCgpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19zZXRTaWRlYmFyVGFiSGVpZ2h0OiBzaWRlYmFyVGFicyA9ICcsIF9kZWZhdWx0cy5zaWRlYmFyVGFic1NlbGVjdG9yICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIHZhciB2aWV3cG9ydEhlaWdodCA9ICQoIHdpbmRvdyApLm91dGVySGVpZ2h0KCk7XG4gICAgICAgIHZhciBuYXZIZWlnaHQgPSAkKCBfZGVmYXVsdHMubmF2U2VsZWN0b3IgKS5vdXRlckhlaWdodCgpO1xuICAgICAgICB2YXIgbmV3SGVpZ2h0ID0gdmlld3BvcnRIZWlnaHQgLSBuYXZIZWlnaHQ7XG4gICAgICAgIHZhciB0YWJQb3MgPSAkKCBfZGVmYXVsdHMuc2lkZWJhclRhYnNTZWxlY3RvciApLnBvc2l0aW9uKCk7XG4gICAgICAgIHZhciB0YWJIZWlnaHQgPSBuZXdIZWlnaHQgLSB0YWJQb3MudG9wIC0gMTU7XG4gICAgICAgIHZhciBuYXZUYWJzSGVpZ2h0ID0gJCggJy5uYXYtdGFicycgKS5vdXRlckhlaWdodCgpO1xuICAgICAgICBcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19zZXRTaWRlYmFyVGFiSGVpZ2h0OiB0YWJQb3MgPSAnLCB0YWJQb3MgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX3NldFNpZGViYXJUYWJIZWlnaHQ6IHRhYkhlaWdodCA9ICcsIHRhYkhlaWdodCApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICBpZiAoIHZpZXdwb3J0SGVpZ2h0ID4gNzY4ICkge1xuICAgICAgICAgICAgJCggX2RlZmF1bHRzLnNpZGViYXJUYWJzU2VsZWN0b3IgKS5jc3MoICdoZWlnaHQnLCB0YWJIZWlnaHQgKTtcbiAgICAgICAgICAgICQoIF9kZWZhdWx0cy5zaWRlYmFyVGFiQ29udGVudFNlbGVjdG9yICkuY3NzKCAnaGVpZ2h0JywgdGFiSGVpZ2h0IC0gbmF2VGFic0hlaWdodCApO1xuICAgICAgICAgICAgJCggX2RlZmF1bHRzLnNpZGViYXJUb2NXcmFwcGVyU2VsZWN0b3IgKS5jc3MoICdtaW4taGVpZ2h0JywgdGFiSGVpZ2h0IC0gbmF2VGFic0hlaWdodCApO1xuICAgICAgICB9XG4gICAgfVxuICAgIFxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB3aGljaCBzZXRzIHRoZSBwb3NpdGlvbiBvZiB0aGUgc2lkZWJhciB0b2dnbGUgYnV0dG9uLlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX3NldFNpZGViYXJCdXR0b25Qb3NpdGlvblxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9zZXRTaWRlYmFyQnV0dG9uUG9zaXRpb24oKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICctLS0tLS0tLS0tIF9zZXRTaWRlYmFyQnV0dG9uUG9zaXRpb24oKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfc2V0U2lkZWJhckJ1dHRvblBvc2l0aW9uOiB2aWV3ID0gJywgX2RlZmF1bHRzLnZpZXdTZWxlY3RvciApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgdmlld0hhbGZIZWlnaHQgPSAkKCBfZGVmYXVsdHMudmlld1NlbGVjdG9yICkub3V0ZXJIZWlnaHQoKSAvIDI7XG4gICAgICAgIFxuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX3NldFNpZGViYXJCdXR0b25Qb3NpdGlvbjogdmlld0hhbGZIZWlnaHQgPSAnLCB2aWV3SGFsZkhlaWdodCApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICAkKCBfZGVmYXVsdHMuc2lkZWJhclRvZ2dsZUJ1dHRvbiApLmNzcyggJ3RvcCcsIHZpZXdIYWxmSGVpZ2h0ICk7XG4gICAgICAgIFxuICAgIH1cbiAgICBcbiAgICAvKipcbiAgICAgKiBNZXRob2Qgd2hpY2ggY2hlY2tzIHRoZSBjdXJyZW50IHNpZGViYXIgc3RhdHVzLCBiYXNlZCBvbiBhIGxvY2FsIHN0b3JhZ2UgdmFsdWUuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfY2hlY2tTaWRlYmFyU3RhdHVzXG4gICAgICogQHJldHVybnMge0Jvb2xlYW59IFJldHVybnMgZmFsc2UgaWYgdGhlIHNpZGViYXIgaXMgaW5hY3RpdmUsIHJldHVybnMgdHJ1ZSBpZiB0aGVcbiAgICAgKiBzaWRlYmFyIGlzIGFjdGl2ZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfY2hlY2tTaWRlYmFyU3RhdHVzKCkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfY2hlY2tTaWRlYmFyU3RhdHVzKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2NoZWNrU2lkZWJhclN0YXR1czogc2lkZWJhclN0YXR1cyA9ICcsIF9kZWZhdWx0cy5zaWRlYmFyU3RhdHVzICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIGlmICggX2RlZmF1bHRzLnNpZGViYXJTdGF0dXMgPT09ICdmYWxzZScgKSB7XG4gICAgICAgICAgICAkKCAnW2RhdGEtdG9nZ2xlPVwic2lkZWJhclwiXScgKS5yZW1vdmVDbGFzcyggJ2luJyApO1xuICAgICAgICAgICAgJCggJy5yZWFkaW5nLW1vZGVfX2NvbnRlbnQtc2lkZWJhcicgKS5yZW1vdmVDbGFzcyggJ2luJyApLnByZXYoKS5yZW1vdmVDbGFzcyggJ2luJyApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICByZXR1cm4gZmFsc2U7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICByZXR1cm4gdHJ1ZTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICB9XG4gICAgXG4gICAgLyoqXG4gICAgICogTWV0aG9kIHdoaWNoIHNob3dzIHRoZSBjb250ZW50IGJ5IHJlbW92aW5nIENTUy1DbGFzc2VzIGFmdGVyIGxvYWRpbmcgZXZlcnkgcGFnZVxuICAgICAqIGVsZW1lbnQuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfc2hvd0NvbnRlbnRcbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfc2hvd0NvbnRlbnQoKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICctLS0tLS0tLS0tIF9zaG93Q29udGVudCgpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgICQoIF9kZWZhdWx0cy52aWV3U2VsZWN0b3IgKS5yZW1vdmVDbGFzcyggJ2ludmlzaWJsZScgKTtcbiAgICAgICAgJCggX2RlZmF1bHRzLnNpZGViYXJTZWxlY3RvciApLnJlbW92ZUNsYXNzKCAnaW52aXNpYmxlJyApO1xuICAgIH1cbiAgICBcbiAgICByZXR1cm4gb3NWaWV3ZXI7XG4gICAgXG59ICkoIHZpZXdJbWFnZSB8fCB7fSwgalF1ZXJ5ICk7XG4iLCIvKipcbiAqIFRoaXMgZmlsZSBpcyBwYXJ0IG9mIHRoZSBHb29iaSB2aWV3ZXIgLSBhIGNvbnRlbnQgcHJlc2VudGF0aW9uIGFuZCBtYW5hZ2VtZW50XG4gKiBhcHBsaWNhdGlvbiBmb3IgZGlnaXRpemVkIG9iamVjdHMuXG4gKiBcbiAqIFZpc2l0IHRoZXNlIHdlYnNpdGVzIGZvciBtb3JlIGluZm9ybWF0aW9uLiAtIGh0dHA6Ly93d3cuaW50cmFuZGEuY29tIC1cbiAqIGh0dHA6Ly9kaWdpdmVyc28uY29tXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBmcmVlIHNvZnR3YXJlOyB5b3UgY2FuIHJlZGlzdHJpYnV0ZSBpdCBhbmQvb3IgbW9kaWZ5IGl0IHVuZGVyIHRoZSB0ZXJtc1xuICogb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFzIHB1Ymxpc2hlZCBieSB0aGUgRnJlZSBTb2Z0d2FyZSBGb3VuZGF0aW9uOyBlaXRoZXJcbiAqIHZlcnNpb24gMiBvZiB0aGUgTGljZW5zZSwgb3IgKGF0IHlvdXIgb3B0aW9uKSBhbnkgbGF0ZXIgdmVyc2lvbi5cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGRpc3RyaWJ1dGVkIGluIHRoZSBob3BlIHRoYXQgaXQgd2lsbCBiZSB1c2VmdWwsIGJ1dCBXSVRIT1VUIEFOWVxuICogV0FSUkFOVFk7IHdpdGhvdXQgZXZlbiB0aGUgaW1wbGllZCB3YXJyYW50eSBvZiBNRVJDSEFOVEFCSUxJVFkgb3IgRklUTkVTUyBGT1IgQVxuICogUEFSVElDVUxBUiBQVVJQT1NFLiBTZWUgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGZvciBtb3JlIGRldGFpbHMuXG4gKiBcbiAqIFlvdSBzaG91bGQgaGF2ZSByZWNlaXZlZCBhIGNvcHkgb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFsb25nIHdpdGggdGhpc1xuICogcHJvZ3JhbS4gSWYgbm90LCBzZWUgPGh0dHA6Ly93d3cuZ251Lm9yZy9saWNlbnNlcy8+LlxuICogXG4gKiBNb2R1bGUgd2hpY2ggaW50ZXJwcmV0cyB0aGUgaW1hZ2UgaW5mb3JtYXRpb24uXG4gKiBcbiAqIEB2ZXJzaW9uIDMuMi4wXG4gKiBAbW9kdWxlIHZpZXdJbWFnZS50aWxlU291cmNlUmVzb2x2ZXJcbiAqIEByZXF1aXJlcyBqUXVlcnlcbiAqL1xudmFyIHZpZXdJbWFnZSA9ICggZnVuY3Rpb24oIG9zVmlld2VyICkge1xuICAgICd1c2Ugc3RyaWN0JztcbiAgICBcbiAgICB2YXIgX2RlYnVnID0gZmFsc2U7XG4gICAgXG4gICAgb3NWaWV3ZXIudGlsZVNvdXJjZVJlc29sdmVyID0ge1xuICAgICAgICBcbiAgICAgICAgcmVzb2x2ZUFzSnNvbk9yVVJJOiBmdW5jdGlvbiggaW1hZ2VJbmZvICkge1xuICAgICAgICAgICAgdmFyIGRlZmVycmVkID0gUS5kZWZlcigpO1xuICAgICAgICAgICAgaWYgKCB0aGlzLmlzSnNvbiggaW1hZ2VJbmZvICkgKSB7XG4gICAgICAgICAgICAgICAgZGVmZXJyZWQucmVzb2x2ZSggaW1hZ2VJbmZvICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIGlmICggdGhpcy5pc1N0cmluZ2lmaWVkSnNvbiggaW1hZ2VJbmZvICkgKSB7XG4gICAgICAgICAgICAgICAgZGVmZXJyZWQucmVzb2x2ZSggSlNPTi5wYXJzZSggaW1hZ2VJbmZvICkgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIGRlZmVycmVkLnJlc29sdmUoIGltYWdlSW5mbyApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgcmV0dXJuIGRlZmVycmVkLnByb21pc2U7XG4gICAgICAgICAgICBcbiAgICAgICAgfSxcbiAgICAgICAgXG4gICAgICAgIHJlc29sdmVBc0pzb246IGZ1bmN0aW9uKCBpbWFnZUluZm8gKSB7XG4gICAgICAgICAgICB2YXIgZGVmZXJyZWQgPSBRLmRlZmVyKCk7XG4gICAgICAgICAgICBpZiAoIHRoaXMuaXNVUkkoIGltYWdlSW5mbyApICkge1xuICAgICAgICAgICAgICAgIGlmICggdGhpcy5pc0pzb25VUkkoIGltYWdlSW5mbyApICkge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4gdGhpcy5sb2FkSnNvbkZyb21VUkwoIGltYWdlSW5mbyApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgZGVmZXJyZWQucmVqZWN0KCBcIlVybCBkb2VzIG5vdCBsZWFkIHRvIGEganNvbiBvYmplY3RcIiApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2UgaWYgKCB0eXBlb2YgaW1hZ2VJbmZvID09PSBcInN0cmluZ1wiICkge1xuICAgICAgICAgICAgICAgIHRyeSB7XG4gICAgICAgICAgICAgICAgICAgIHZhciBqc29uID0gSlNPTi5wYXJzZSggaW1hZ2VJbmZvICk7XG4gICAgICAgICAgICAgICAgICAgIGRlZmVycmVkLnJlc29sdmUoIGpzb24gKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgY2F0Y2ggKCBlcnJvciApIHtcbiAgICAgICAgICAgICAgICAgICAgZGVmZXJyZWQucmVqZWN0KCBcIlN0cmluZyBkb2VzIG5vdCBjb250YWluIHZhbGlkIGpzb246IFwiICsgZXJyb3IgKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIGlmICggdHlwZW9mIGltYWdlSW5mbyA9PT0gXCJvYmplY3RcIiApIHtcbiAgICAgICAgICAgICAgICBkZWZlcnJlZC5yZXNvbHZlKCBpbWFnZUluZm8gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIGRlZmVycmVkLnJlamVjdCggXCJOZWl0aGVyIGEgdXJsIG5vciBhIGpzb24gb2JqZWN0XCIgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJldHVybiBkZWZlcnJlZC5wcm9taXNlO1xuICAgICAgICB9LFxuICAgICAgICBcbiAgICAgICAgbG9hZEpzb25Gcm9tVVJMOiBmdW5jdGlvbiggaW1hZ2VJbmZvICkge1xuICAgICAgICAgICAgdmFyIGRlZmVycmVkID0gUS5kZWZlcigpO1xuICAgICAgICAgICAgaWYgKCB0aGlzLmlzSnNvblVSSSggaW1hZ2VJbmZvICkgKSB7XG4gICAgICAgICAgICAgICAgT3BlblNlYWRyYWdvbi5tYWtlQWpheFJlcXVlc3QoIGltYWdlSW5mbyxcbiAgICAgICAgICAgICAgICAvLyBzdWNjZXNzXG4gICAgICAgICAgICAgICAgZnVuY3Rpb24oIHJlcXVlc3QgKSB7XG4gICAgICAgICAgICAgICAgICAgIHRyeSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBkZWZlcnJlZC5yZXNvbHZlKCBKU09OLnBhcnNlKCByZXF1ZXN0LnJlc3BvbnNlVGV4dCApICk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgY2F0Y2ggKCBlcnJvciApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGRlZmVycmVkLnJlamVjdCggZXJyb3IgKVxuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAvLyBlcnJvclxuICAgICAgICAgICAgICAgIGZ1bmN0aW9uKCBlcnJvciApIHtcbiAgICAgICAgICAgICAgICAgICAgZGVmZXJyZWQucmVqZWN0KCBlcnJvciApO1xuICAgICAgICAgICAgICAgIH0gKVxuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgZGVmZXJyZWQucmVqZWN0KCBcIk5vdCBhIGpzb24gdXJpOiBcIiArIGltYWdlSW5mbyApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgcmV0dXJuIGRlZmVycmVkLnByb21pc2U7XG4gICAgICAgIH0sXG4gICAgICAgIFxuICAgICAgICBsb2FkSWZKc29uVVJMOiBmdW5jdGlvbiggaW1hZ2VJbmZvICkge1xuICAgICAgICAgICAgcmV0dXJuIFEucHJvbWlzZSggZnVuY3Rpb24oIHJlc29sdmUsIHJlamVjdCApIHtcbiAgICAgICAgICAgICAgICBpZiAoIG9zVmlld2VyLnRpbGVTb3VyY2VSZXNvbHZlci5pc1VSSSggaW1hZ2VJbmZvICkgKSB7XG4gICAgICAgICAgICAgICAgICAgIHZhciBhamF4UGFyYW1zID0ge1xuICAgICAgICAgICAgICAgICAgICAgICAgdXJsOiBkZWNvZGVVUkkoIGltYWdlSW5mbyApLFxuICAgICAgICAgICAgICAgICAgICAgICAgdHlwZTogXCJHRVRcIixcbiAgICAgICAgICAgICAgICAgICAgICAgIGRhdGFUeXBlOiBcIkpTT05cIixcbiAgICAgICAgICAgICAgICAgICAgICAgIGFzeW5jOiB0cnVlLFxuICAgICAgICAgICAgICAgICAgICAgICAgY3Jvc3NEb21haW46IHRydWUsXG4gICAgICAgICAgICAgICAgICAgICAgICBhY2NlcHRzOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgYXBwbGljYXRpb25fanNvbjogXCJhcHBsaWNhdGlvbi9qc29uXCIsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgYXBwbGljYXRpb25fanNvbkxkOiBcImFwcGxpY2F0aW9uL2xkK2pzb25cIixcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB0ZXh0X2pzb246IFwidGV4dC9qc29uXCIsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgdGV4dF9qc29uTGQ6IFwidGV4dC9sZCtqc29uXCIsXG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgUSggJC5hamF4KCBhamF4UGFyYW1zICkgKS50aGVuKCBmdW5jdGlvbiggZGF0YSApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJlc29sdmUoIGRhdGEgKTtcbiAgICAgICAgICAgICAgICAgICAgfSApLmZhaWwoIGZ1bmN0aW9uKCBlcnJvciApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJlamVjdCggXCJGYWlsZWQgdG8gcmV0cmlldmUganNvbiBmcm9tIFwiICsgaW1hZ2VJbmZvICk7XG4gICAgICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICAgICAgc2V0VGltZW91dCggZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICByZWplY3QoIFwiVGltZW91dCBhZnRlciAxMHNcIiApO1xuICAgICAgICAgICAgICAgICAgICB9LCAxMDAwMCApXG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICByZWplY3QoIFwiTm90IGEgdXJpOiBcIiArIGltYWdlSW5mbyApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgfSxcbiAgICAgICAgXG4gICAgICAgIGlzSnNvblVSSTogZnVuY3Rpb24oIGltYWdlSW5mbyApIHtcbiAgICAgICAgICAgIGlmICggdGhpcy5pc1VSSSggaW1hZ2VJbmZvICkgKSB7XG4gICAgICAgICAgICAgICAgdmFyIHNob3J0ZW5lZCA9IGltYWdlSW5mby5yZXBsYWNlKCAvXFw/LiovLCBcIlwiICk7XG4gICAgICAgICAgICAgICAgaWYgKCBzaG9ydGVuZWQuZW5kc1dpdGgoIFwiL1wiICkgKSB7XG4gICAgICAgICAgICAgICAgICAgIHNob3J0ZW5lZCA9IHNob3J0ZW5lZC5zdWJzdHJpbmcoIDAsIHNob3J0ZW5lZC5sZW5ndGggLSAxICk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIHJldHVybiBzaG9ydGVuZWQudG9Mb3dlckNhc2UoKS5lbmRzV2l0aCggXCIuanNvblwiICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICByZXR1cm4gZmFsc2U7XG4gICAgICAgIH0sXG4gICAgICAgIGlzVVJJOiBmdW5jdGlvbiggaW1hZ2VJbmZvICkge1xuICAgICAgICAgICAgaWYgKCBpbWFnZUluZm8gJiYgdHlwZW9mIGltYWdlSW5mbyA9PT0gXCJzdHJpbmdcIiApIHtcbiAgICAgICAgICAgICAgICBpZiAoIGltYWdlSW5mby5zdGFydHNXaXRoKCBcImh0dHA6Ly9cIiApIHx8IGltYWdlSW5mby5zdGFydHNXaXRoKCBcImh0dHBzOi8vXCIgKSB8fCBpbWFnZUluZm8uc3RhcnRzV2l0aCggXCJmaWxlOi9cIiApICkge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4gdHJ1ZTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICByZXR1cm4gZmFsc2U7XG4gICAgICAgIH0sXG4gICAgICAgIGlzU3RyaW5naWZpZWRKc29uOiBmdW5jdGlvbiggaW1hZ2VJbmZvICkge1xuICAgICAgICAgICAgaWYgKCBpbWFnZUluZm8gJiYgdHlwZW9mIGltYWdlSW5mbyA9PT0gXCJzdHJpbmdcIiApIHtcbiAgICAgICAgICAgICAgICB0cnkge1xuICAgICAgICAgICAgICAgICAgICB2YXIganNvbiA9IEpTT04ucGFyc2UoIGltYWdlSW5mbyApO1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4gdGhpcy5pc0pzb24oIGpzb24gKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgY2F0Y2ggKCBlcnJvciApIHtcbiAgICAgICAgICAgICAgICAgICAgLy8gbm8ganNvblxuICAgICAgICAgICAgICAgICAgICByZXR1cm4gZmFsc2U7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICAgICAgcmV0dXJuIGZhbHNlO1xuICAgICAgICAgICAgXG4gICAgICAgIH0sXG4gICAgICAgIGlzSnNvbjogZnVuY3Rpb24oIGltYWdlSW5mbyApIHtcbiAgICAgICAgICAgIHJldHVybiBpbWFnZUluZm8gJiYgdHlwZW9mIGltYWdlSW5mbyA9PT0gXCJvYmplY3RcIjtcbiAgICAgICAgfSxcbiAgICBcbiAgICB9XG5cbiAgICByZXR1cm4gb3NWaWV3ZXI7XG4gICAgXG59ICkoIHZpZXdJbWFnZSB8fCB7fSwgalF1ZXJ5ICk7XG4iLCIvKipcbiAqIFRoaXMgZmlsZSBpcyBwYXJ0IG9mIHRoZSBHb29iaSB2aWV3ZXIgLSBhIGNvbnRlbnQgcHJlc2VudGF0aW9uIGFuZCBtYW5hZ2VtZW50XG4gKiBhcHBsaWNhdGlvbiBmb3IgZGlnaXRpemVkIG9iamVjdHMuXG4gKiBcbiAqIFZpc2l0IHRoZXNlIHdlYnNpdGVzIGZvciBtb3JlIGluZm9ybWF0aW9uLiAtIGh0dHA6Ly93d3cuaW50cmFuZGEuY29tIC1cbiAqIGh0dHA6Ly9kaWdpdmVyc28uY29tXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBmcmVlIHNvZnR3YXJlOyB5b3UgY2FuIHJlZGlzdHJpYnV0ZSBpdCBhbmQvb3IgbW9kaWZ5IGl0IHVuZGVyIHRoZSB0ZXJtc1xuICogb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFzIHB1Ymxpc2hlZCBieSB0aGUgRnJlZSBTb2Z0d2FyZSBGb3VuZGF0aW9uOyBlaXRoZXJcbiAqIHZlcnNpb24gMiBvZiB0aGUgTGljZW5zZSwgb3IgKGF0IHlvdXIgb3B0aW9uKSBhbnkgbGF0ZXIgdmVyc2lvbi5cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGRpc3RyaWJ1dGVkIGluIHRoZSBob3BlIHRoYXQgaXQgd2lsbCBiZSB1c2VmdWwsIGJ1dCBXSVRIT1VUIEFOWVxuICogV0FSUkFOVFk7IHdpdGhvdXQgZXZlbiB0aGUgaW1wbGllZCB3YXJyYW50eSBvZiBNRVJDSEFOVEFCSUxJVFkgb3IgRklUTkVTUyBGT1IgQVxuICogUEFSVElDVUxBUiBQVVJQT1NFLiBTZWUgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGZvciBtb3JlIGRldGFpbHMuXG4gKiBcbiAqIFlvdSBzaG91bGQgaGF2ZSByZWNlaXZlZCBhIGNvcHkgb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFsb25nIHdpdGggdGhpc1xuICogcHJvZ3JhbS4gSWYgbm90LCBzZWUgPGh0dHA6Ly93d3cuZ251Lm9yZy9saWNlbnNlcy8+LlxuICogXG4gKiBNb2R1bGUgd2hpY2ggcmVzaXplcyBleGlzdGluZyByZWN0YW5nbGVzIG9uIGFuIGltYWdlLlxuICogXG4gKiBAdmVyc2lvbiAzLjIuMFxuICogQG1vZHVsZSB2aWV3SW1hZ2UudHJhbnNmb3JtUmVjdFxuICogQHJlcXVpcmVzIGpRdWVyeVxuICovXG52YXIgdmlld0ltYWdlID0gKCBmdW5jdGlvbiggb3NWaWV3ZXIgKSB7XG4gICAgJ3VzZSBzdHJpY3QnO1xuICAgIFxuICAgIHZhciBERUZBVUxUX0NVUlNPUiA9IFwiZGVmYXVsdFwiO1xuICAgIFxuICAgIHZhciBfZGVidWcgPSBmYWxzZTtcbiAgICBcbiAgICB2YXIgX2RyYXdpbmdTdHlsZUNsYXNzID0gXCJ0cmFuc2Zvcm1pbmdcIjtcbiAgICBcbiAgICB2YXIgX2FjdGl2ZSA9IGZhbHNlO1xuICAgIHZhciBfZHJhd2luZyA9IGZhbHNlO1xuICAgIHZhciBfZ3JvdXAgPSBudWxsO1xuICAgIHZhciBfZmluaXNoSG9vayA9IG51bGw7XG4gICAgdmFyIF92aWV3ZXJJbnB1dEhvb2sgPSBudWxsO1xuICAgIHZhciBfaGJBZGQgPSA1O1xuICAgIHZhciBfc2lkZUNsaWNrUHJlY2lzaW9uID0gMC4wMDQ7XG4gICAgdmFyIF9kcmF3QXJlYSA9IFwiXCI7XG4gICAgdmFyIF9lbnRlclBvaW50ID0gbnVsbDtcbiAgICBcbiAgICBvc1ZpZXdlci50cmFuc2Zvcm1SZWN0ID0ge1xuICAgICAgICBpbml0OiBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnb3NWaWV3ZXIudHJhbnNmb3JtUmVjdC5pbml0JyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjJyApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgX3ZpZXdlcklucHV0SG9vayA9IG9zVmlld2VyLnZpZXdlci5hZGRWaWV3ZXJJbnB1dEhvb2soIHtcbiAgICAgICAgICAgICAgICBob29rczogWyB7XG4gICAgICAgICAgICAgICAgICAgIHRyYWNrZXI6IFwidmlld2VyXCIsXG4gICAgICAgICAgICAgICAgICAgIGhhbmRsZXI6IFwiY2xpY2tIYW5kbGVyXCIsXG4gICAgICAgICAgICAgICAgICAgIGhvb2tIYW5kbGVyOiBfZGlzYWJsZVZpZXdlckV2ZW50XG4gICAgICAgICAgICAgICAgLy8gfSwge1xuICAgICAgICAgICAgICAgIC8vIHRyYWNrZXIgOiBcInZpZXdlclwiLFxuICAgICAgICAgICAgICAgIC8vIGhhbmRsZXIgOiBcInNjcm9sbEhhbmRsZXJcIixcbiAgICAgICAgICAgICAgICAvLyBob29rSGFuZGxlciA6IF9kaXNhYmxlVmlld2VyRXZlbnRcbiAgICAgICAgICAgICAgICB9LCB7XG4gICAgICAgICAgICAgICAgICAgIHRyYWNrZXI6IFwidmlld2VyXCIsXG4gICAgICAgICAgICAgICAgICAgIGhhbmRsZXI6IFwiZHJhZ0hhbmRsZXJcIixcbiAgICAgICAgICAgICAgICAgICAgaG9va0hhbmRsZXI6IF9vblZpZXdlckRyYWdcbiAgICAgICAgICAgICAgICB9LCB7XG4gICAgICAgICAgICAgICAgICAgIHRyYWNrZXI6IFwidmlld2VyXCIsXG4gICAgICAgICAgICAgICAgICAgIGhhbmRsZXI6IFwicHJlc3NIYW5kbGVyXCIsXG4gICAgICAgICAgICAgICAgICAgIGhvb2tIYW5kbGVyOiBfb25WaWV3ZXJQcmVzc1xuICAgICAgICAgICAgICAgIH0sIHtcbiAgICAgICAgICAgICAgICAgICAgdHJhY2tlcjogXCJ2aWV3ZXJcIixcbiAgICAgICAgICAgICAgICAgICAgaGFuZGxlcjogXCJkcmFnRW5kSGFuZGxlclwiLFxuICAgICAgICAgICAgICAgICAgICBob29rSGFuZGxlcjogX29uVmlld2VyRHJhZ0VuZFxuICAgICAgICAgICAgICAgIH0sIHtcbiAgICAgICAgICAgICAgICAgICAgdHJhY2tlcjogXCJ2aWV3ZXJcIixcbiAgICAgICAgICAgICAgICAgICAgaGFuZGxlcjogXCJyZWxlYXNlSGFuZGxlclwiLFxuICAgICAgICAgICAgICAgICAgICBob29rSGFuZGxlcjogX29uVmlld2VyUmVsZWFzZVxuICAgICAgICAgICAgICAgIH0sIHtcbiAgICAgICAgICAgICAgICAgICAgdHJhY2tlcjogXCJ2aWV3ZXJcIixcbiAgICAgICAgICAgICAgICAgICAgaGFuZGxlcjogXCJtb3ZlSGFuZGxlclwiLFxuICAgICAgICAgICAgICAgICAgICBob29rSGFuZGxlcjogX29uVmlld2VyTW92ZVxuICAgICAgICAgICAgICAgIH0gXVxuICAgICAgICAgICAgfSApO1xuICAgICAgICB9LFxuICAgICAgICBzdGFydERyYXdpbmc6IGZ1bmN0aW9uKCBvdmVybGF5LCBmaW5pc2hIb29rICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKVxuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCBcIlN0YXJ0IGRyYXdpbmdcIiApO1xuICAgICAgICAgICAgb3NWaWV3ZXIub3ZlcmxheXMuc2V0RHJhd2luZ092ZXJsYXkoIG92ZXJsYXkgKTtcbiAgICAgICAgICAgIF9hY3RpdmUgPSB0cnVlO1xuICAgICAgICAgICAgX2dyb3VwID0gb3ZlcmxheS5ncm91cDtcbiAgICAgICAgICAgIF9maW5pc2hIb29rID0gZmluaXNoSG9vaztcbiAgICAgICAgICAgICQoIG92ZXJsYXkuZWxlbWVudCApLmFkZENsYXNzKCBfZHJhd2luZ1N0eWxlQ2xhc3MgKTtcbiAgICAgICAgfSxcbiAgICAgICAgZW5kRHJhd2luZzogZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICBfZHJhd2luZyA9IGZhbHNlO1xuICAgICAgICAgICAgX2dyb3VwID0gbnVsbDtcbiAgICAgICAgICAgIF9maW5pc2hIb29rID0gbnVsbDtcbiAgICAgICAgICAgIF9hY3RpdmUgPSBmYWxzZTtcbiAgICAgICAgICAgIHZhciBkcmF3T3ZlcmxheSA9IG9zVmlld2VyLm92ZXJsYXlzLmdldERyYXdpbmdPdmVybGF5KCk7XG4gICAgICAgICAgICBpZiAoIGRyYXdPdmVybGF5ICE9IG51bGwgKSB7XG4gICAgICAgICAgICAgICAgJCggZHJhd092ZXJsYXkuZWxlbWVudCApLnJlbW92ZUNsYXNzKCBfZHJhd2luZ1N0eWxlQ2xhc3MgKTtcbiAgICAgICAgICAgICAgICAkKCBkcmF3T3ZlcmxheS5lbGVtZW50ICkuY3NzKCB7XG4gICAgICAgICAgICAgICAgICAgIGN1cnNvcjogREVGQVVMVF9DVVJTT1JcbiAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0sXG4gICAgICAgIGlzQWN0aXZlOiBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIHJldHVybiBfYWN0aXZlO1xuICAgICAgICB9LFxuICAgICAgICBoaXRBcmVhczoge1xuICAgICAgICAgICAgVE9QOiBcInRcIixcbiAgICAgICAgICAgIEJPVFRPTTogXCJiXCIsXG4gICAgICAgICAgICBSSUdIVDogXCJyXCIsXG4gICAgICAgICAgICBMRUZUOiBcImxcIixcbiAgICAgICAgICAgIFRPUExFRlQ6IFwidGxcIixcbiAgICAgICAgICAgIFRPUFJJR0hUOiBcInRyXCIsXG4gICAgICAgICAgICBCT1RUT01MRUZUOiBcImJsXCIsXG4gICAgICAgICAgICBCT1RUT01SSUdIVDogXCJiclwiLFxuICAgICAgICAgICAgQ0VOVEVSOiBcImNcIixcbiAgICAgICAgICAgIGlzQ29ybmVyOiBmdW5jdGlvbiggYXJlYSApIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gYXJlYSA9PT0gdGhpcy5UT1BSSUdIVCB8fCBhcmVhID09PSB0aGlzLlRPUExFRlQgfHwgYXJlYSA9PT0gdGhpcy5CT1RUT01MRUZUIHx8IGFyZWEgPT09IHRoaXMuQk9UVE9NUklHSFQ7XG4gICAgICAgICAgICB9LFxuICAgICAgICAgICAgaXNFZGdlOiBmdW5jdGlvbiggYXJlYSApIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gYXJlYSA9PT0gdGhpcy5UT1AgfHwgYXJlYSA9PT0gdGhpcy5CT1RUT00gfHwgYXJlYSA9PT0gdGhpcy5MRUZUIHx8IGFyZWEgPT09IHRoaXMuUklHSFQ7XG4gICAgICAgICAgICB9LFxuICAgICAgICAgICAgZ2V0Q3Vyc29yOiBmdW5jdGlvbiggYXJlYSApIHtcbiAgICAgICAgICAgICAgICB2YXIgcm90YXRlZCA9IG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC5nZXRSb3RhdGlvbigpICUgMTgwID09PSA5MDtcbiAgICAgICAgICAgICAgICBpZiAoIGFyZWEgPT09IHRoaXMuVE9QTEVGVCB8fCBhcmVhID09PSB0aGlzLkJPVFRPTVJJR0hUICkge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4gcm90YXRlZCA/IFwibmVzdy1yZXNpemVcIiA6IFwibndzZS1yZXNpemVcIjtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSBpZiAoIGFyZWEgPT09IHRoaXMuVE9QUklHSFQgfHwgYXJlYSA9PT0gdGhpcy5CT1RUT01MRUZUICkge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4gcm90YXRlZCA/IFwibndzZS1yZXNpemVcIiA6IFwibmVzdy1yZXNpemVcIjtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSBpZiAoIGFyZWEgPT09IHRoaXMuVE9QIHx8IGFyZWEgPT09IHRoaXMuQk9UVE9NICkge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4gcm90YXRlZCA/IFwiZXctcmVzaXplXCIgOiBcIm5zLXJlc2l6ZVwiO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIGlmICggYXJlYSA9PT0gdGhpcy5SSUdIVCB8fCBhcmVhID09PSB0aGlzLkxFRlQgKSB7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiByb3RhdGVkID8gXCJucy1yZXNpemVcIiA6IFwiZXctcmVzaXplXCI7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2UgaWYgKCBhcmVhID09PSB0aGlzLkNFTlRFUiApIHtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIFwibW92ZVwiO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIERFRkFVTFRfQ1VSU09SO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgIH1cblxuICAgIGZ1bmN0aW9uIF9vblZpZXdlck1vdmUoIGV2ZW50ICkge1xuICAgICAgICBpZiAoICFfZHJhd2luZyAmJiBfYWN0aXZlICkge1xuICAgICAgICAgICAgdmFyIGRyYXdQb2ludCA9IG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC52aWV3ZXJFbGVtZW50VG9WaWV3cG9ydENvb3JkaW5hdGVzKCBldmVudC5wb3NpdGlvbiApO1xuICAgICAgICAgICAgZHJhd1BvaW50ID0gb3NWaWV3ZXIub3ZlcmxheXMuZ2V0Um90YXRlZCggZHJhd1BvaW50ICk7XG4gICAgICAgICAgICB2YXIgb3ZlcmxheVJlY3QgPSBvc1ZpZXdlci5vdmVybGF5cy5nZXREcmF3aW5nT3ZlcmxheSgpLnJlY3Q7XG4gICAgICAgICAgICB2YXIgb3ZlcmxheUVsZW1lbnQgPSBvc1ZpZXdlci5vdmVybGF5cy5nZXREcmF3aW5nT3ZlcmxheSgpLmVsZW1lbnQ7XG4gICAgICAgICAgICB2YXIgdmlld2VyRWxlbWVudCA9IG9zVmlld2VyLnZpZXdlci5lbGVtZW50O1xuICAgICAgICAgICAgdmFyIGFyZWEgPSBfZmluZENvcm5lciggb3ZlcmxheVJlY3QsIGRyYXdQb2ludCwgX3NpZGVDbGlja1ByZWNpc2lvbiApO1xuICAgICAgICAgICAgaWYgKCAhYXJlYSApIHtcbiAgICAgICAgICAgICAgICBhcmVhID0gX2ZpbmRFZGdlKCBvdmVybGF5UmVjdCwgZHJhd1BvaW50LCBfc2lkZUNsaWNrUHJlY2lzaW9uICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAoICFhcmVhICYmIG9zVmlld2VyLm92ZXJsYXlzLmNvbnRhaW5zKCBvdmVybGF5UmVjdCwgZHJhd1BvaW50LCAwICkgKSB7XG4gICAgICAgICAgICAgICAgYXJlYSA9IG9zVmlld2VyLnRyYW5zZm9ybVJlY3QuaGl0QXJlYXMuQ0VOVEVSO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaWYgKCBhcmVhICkge1xuICAgICAgICAgICAgICAgICQoIHZpZXdlckVsZW1lbnQgKS5jc3MoIHtcbiAgICAgICAgICAgICAgICAgICAgY3Vyc29yOiBvc1ZpZXdlci50cmFuc2Zvcm1SZWN0LmhpdEFyZWFzLmdldEN1cnNvciggYXJlYSApXG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgJCggdmlld2VyRWxlbWVudCApLmNzcygge1xuICAgICAgICAgICAgICAgICAgICBjdXJzb3I6IERFRkFVTFRfQ1VSU09SXG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZXZlbnQucHJldmVudERlZmF1bHRBY3Rpb24gPSB0cnVlO1xuICAgICAgICAgICAgcmV0dXJuIHRydWU7XG4gICAgICAgIH1cbiAgICB9XG4gICAgXG4gICAgZnVuY3Rpb24gX29uVmlld2VyUHJlc3MoIGV2ZW50ICkge1xuICAgICAgICBpZiAoIF9hY3RpdmUgKSB7XG4gICAgICAgICAgICBpZiAoICFvc1ZpZXdlci5vdmVybGF5cy5nZXREcmF3aW5nT3ZlcmxheSgpICkge1xuICAgICAgICAgICAgICAgIHJldHVybiBmYWxzZTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHZhciBvdmVybGF5UmVjdCA9IG9zVmlld2VyLm92ZXJsYXlzLmdldERyYXdpbmdPdmVybGF5KCkucmVjdDtcbiAgICAgICAgICAgIHZhciBvdmVybGF5RWxlbWVudCA9IG9zVmlld2VyLm92ZXJsYXlzLmdldERyYXdpbmdPdmVybGF5KCkuZWxlbWVudDtcbiAgICAgICAgICAgIHZhciBkcmF3UG9pbnQgPSBvc1ZpZXdlci52aWV3ZXIudmlld3BvcnQudmlld2VyRWxlbWVudFRvVmlld3BvcnRDb29yZGluYXRlcyggZXZlbnQucG9zaXRpb24gKTtcbiAgICAgICAgICAgIGRyYXdQb2ludCA9IG9zVmlld2VyLm92ZXJsYXlzLmdldFJvdGF0ZWQoIGRyYXdQb2ludCApO1xuICAgICAgICAgICAgdmFyIGRyYXdBcmVhID0gX2ZpbmRDb3JuZXIoIG92ZXJsYXlSZWN0LCBkcmF3UG9pbnQsIF9zaWRlQ2xpY2tQcmVjaXNpb24gKTtcbiAgICAgICAgICAgIGlmICggIWRyYXdBcmVhICkge1xuICAgICAgICAgICAgICAgIGRyYXdBcmVhID0gX2ZpbmRFZGdlKCBvdmVybGF5UmVjdCwgZHJhd1BvaW50LCBfc2lkZUNsaWNrUHJlY2lzaW9uICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAoICFkcmF3QXJlYSAmJiBvc1ZpZXdlci5vdmVybGF5cy5jb250YWlucyggb3ZlcmxheVJlY3QsIGRyYXdQb2ludCwgMCApICkge1xuICAgICAgICAgICAgICAgIGRyYXdBcmVhID0gb3NWaWV3ZXIudHJhbnNmb3JtUmVjdC5oaXRBcmVhcy5DRU5URVI7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApXG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coIFwiZHJhdyBhcmVhID0gXCIgKyBkcmF3QXJlYSApO1xuICAgICAgICAgICAgaWYgKCBkcmF3QXJlYSApIHtcbiAgICAgICAgICAgICAgICAkKCBvdmVybGF5RWxlbWVudCApLnRvb2x0aXAoICdkZXN0cm95JyApO1xuICAgICAgICAgICAgICAgIF9lbnRlclBvaW50ID0gZHJhd1BvaW50O1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgX2RyYXdBcmVhID0gZHJhd0FyZWE7XG4gICAgICAgICAgICBldmVudC5wcmV2ZW50RGVmYXVsdEFjdGlvbiA9IHRydWU7XG4gICAgICAgICAgICByZXR1cm4gdHJ1ZTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBcbiAgICBmdW5jdGlvbiBfb25WaWV3ZXJEcmFnKCBldmVudCApIHtcbiAgICAgICAgaWYgKCBfZHJhd2luZyApIHtcbiAgICAgICAgICAgIHZhciBuZXdQb2ludCA9IG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC52aWV3ZXJFbGVtZW50VG9WaWV3cG9ydENvb3JkaW5hdGVzKCBldmVudC5wb3NpdGlvbiApO1xuICAgICAgICAgICAgbmV3UG9pbnQgPSBvc1ZpZXdlci5vdmVybGF5cy5nZXRSb3RhdGVkKCBuZXdQb2ludCApO1xuICAgICAgICAgICAgdmFyIHJlY3QgPSBvc1ZpZXdlci5vdmVybGF5cy5nZXREcmF3aW5nT3ZlcmxheSgpLnJlY3Q7XG4gICAgICAgICAgICB2YXIgdG9wTGVmdDtcbiAgICAgICAgICAgIHZhciBib3R0b21SaWdodDtcbiAgICAgICAgICAgIC8vIGlmKF9kZWJ1Zyljb25zb2xlLmxvZyhcIkRyYXcgbG9jYXRpb24gPSBcIiArIG5ld1BvaW50KTtcbiAgICAgICAgICAgIGlmICggX2RyYXdBcmVhID09PSBvc1ZpZXdlci50cmFuc2Zvcm1SZWN0LmhpdEFyZWFzLlRPUExFRlQgKSB7XG4gICAgICAgICAgICAgICAgdG9wTGVmdCA9IG5ldyBPcGVuU2VhZHJhZ29uLlBvaW50KCBNYXRoLm1pbiggbmV3UG9pbnQueCwgcmVjdC5nZXRCb3R0b21SaWdodCgpLnggKSwgTWF0aC5taW4oIG5ld1BvaW50LnksIHJlY3QuZ2V0Qm90dG9tUmlnaHQoKS55ICkgKTtcbiAgICAgICAgICAgICAgICBib3R0b21SaWdodCA9IHJlY3QuZ2V0Qm90dG9tUmlnaHQoKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2UgaWYgKCBfZHJhd0FyZWEgPT09IG9zVmlld2VyLnRyYW5zZm9ybVJlY3QuaGl0QXJlYXMuVE9QUklHSFQgKSB7XG4gICAgICAgICAgICAgICAgdG9wTGVmdCA9IG5ldyBPcGVuU2VhZHJhZ29uLlBvaW50KCByZWN0LmdldFRvcExlZnQoKS54LCBNYXRoLm1pbiggbmV3UG9pbnQueSwgcmVjdC5nZXRCb3R0b21SaWdodCgpLnkgKSApO1xuICAgICAgICAgICAgICAgIGJvdHRvbVJpZ2h0ID0gbmV3IE9wZW5TZWFkcmFnb24uUG9pbnQoIE1hdGgubWF4KCBuZXdQb2ludC54LCByZWN0LmdldFRvcExlZnQoKS54ICksIHJlY3QuZ2V0Qm90dG9tUmlnaHQoKS55ICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIGlmICggX2RyYXdBcmVhID09PSBvc1ZpZXdlci50cmFuc2Zvcm1SZWN0LmhpdEFyZWFzLkJPVFRPTUxFRlQgKSB7XG4gICAgICAgICAgICAgICAgdG9wTGVmdCA9IG5ldyBPcGVuU2VhZHJhZ29uLlBvaW50KCBNYXRoLm1pbiggbmV3UG9pbnQueCwgcmVjdC5nZXRCb3R0b21SaWdodCgpLnggKSwgcmVjdC5nZXRUb3BMZWZ0KCkueSApO1xuICAgICAgICAgICAgICAgIGJvdHRvbVJpZ2h0ID0gbmV3IE9wZW5TZWFkcmFnb24uUG9pbnQoIHJlY3QuZ2V0Qm90dG9tUmlnaHQoKS54LCBNYXRoLm1heCggbmV3UG9pbnQueSwgcmVjdC5nZXRUb3BMZWZ0KCkueSApICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIGlmICggX2RyYXdBcmVhID09PSBvc1ZpZXdlci50cmFuc2Zvcm1SZWN0LmhpdEFyZWFzLkJPVFRPTVJJR0hUICkge1xuICAgICAgICAgICAgICAgIHRvcExlZnQgPSByZWN0LmdldFRvcExlZnQoKTtcbiAgICAgICAgICAgICAgICBib3R0b21SaWdodCA9IG5ldyBPcGVuU2VhZHJhZ29uLlBvaW50KCBNYXRoLm1heCggbmV3UG9pbnQueCwgcmVjdC5nZXRUb3BMZWZ0KCkueCApLCBNYXRoLm1heCggbmV3UG9pbnQueSwgcmVjdC5nZXRUb3BMZWZ0KCkueSApICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIGlmICggX2RyYXdBcmVhID09PSBvc1ZpZXdlci50cmFuc2Zvcm1SZWN0LmhpdEFyZWFzLkxFRlQgKSB7XG4gICAgICAgICAgICAgICAgdG9wTGVmdCA9IG5ldyBPcGVuU2VhZHJhZ29uLlBvaW50KCBNYXRoLm1pbiggbmV3UG9pbnQueCwgcmVjdC5nZXRCb3R0b21SaWdodCgpLnggKSwgcmVjdC5nZXRUb3BMZWZ0KCkueSApO1xuICAgICAgICAgICAgICAgIGJvdHRvbVJpZ2h0ID0gcmVjdC5nZXRCb3R0b21SaWdodCgpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSBpZiAoIF9kcmF3QXJlYSA9PT0gb3NWaWV3ZXIudHJhbnNmb3JtUmVjdC5oaXRBcmVhcy5SSUdIVCApIHtcbiAgICAgICAgICAgICAgICB0b3BMZWZ0ID0gcmVjdC5nZXRUb3BMZWZ0KCk7XG4gICAgICAgICAgICAgICAgYm90dG9tUmlnaHQgPSBuZXcgT3BlblNlYWRyYWdvbi5Qb2ludCggTWF0aC5tYXgoIG5ld1BvaW50LngsIHJlY3QuZ2V0VG9wTGVmdCgpLnggKSwgcmVjdC5nZXRCb3R0b21SaWdodCgpLnkgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2UgaWYgKCBfZHJhd0FyZWEgPT09IG9zVmlld2VyLnRyYW5zZm9ybVJlY3QuaGl0QXJlYXMuVE9QICkge1xuICAgICAgICAgICAgICAgIHRvcExlZnQgPSBuZXcgT3BlblNlYWRyYWdvbi5Qb2ludCggcmVjdC5nZXRUb3BMZWZ0KCkueCwgTWF0aC5taW4oIG5ld1BvaW50LnksIHJlY3QuZ2V0Qm90dG9tUmlnaHQoKS55ICkgKTtcbiAgICAgICAgICAgICAgICBib3R0b21SaWdodCA9IHJlY3QuZ2V0Qm90dG9tUmlnaHQoKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2UgaWYgKCBfZHJhd0FyZWEgPT09IG9zVmlld2VyLnRyYW5zZm9ybVJlY3QuaGl0QXJlYXMuQk9UVE9NICkge1xuICAgICAgICAgICAgICAgIHRvcExlZnQgPSByZWN0LmdldFRvcExlZnQoKTtcbiAgICAgICAgICAgICAgICBib3R0b21SaWdodCA9IG5ldyBPcGVuU2VhZHJhZ29uLlBvaW50KCByZWN0LmdldEJvdHRvbVJpZ2h0KCkueCwgTWF0aC5tYXgoIG5ld1BvaW50LnksIHJlY3QuZ2V0VG9wTGVmdCgpLnkgKSApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSBpZiAoIF9kcmF3QXJlYSA9PT0gb3NWaWV3ZXIudHJhbnNmb3JtUmVjdC5oaXRBcmVhcy5DRU5URVIgJiYgX2VudGVyUG9pbnQgKSB7XG4gICAgICAgICAgICAgICAgdmFyIGR4ID0gX2VudGVyUG9pbnQueCAtIG5ld1BvaW50Lng7XG4gICAgICAgICAgICAgICAgdmFyIGR5ID0gX2VudGVyUG9pbnQueSAtIG5ld1BvaW50Lnk7XG4gICAgICAgICAgICAgICAgcmVjdC54IC09IGR4O1xuICAgICAgICAgICAgICAgIHJlY3QueSAtPSBkeTtcbiAgICAgICAgICAgICAgICBfZW50ZXJQb2ludCA9IG5ld1BvaW50O1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICBpZiAoIHRvcExlZnQgJiYgYm90dG9tUmlnaHQgKSB7XG4gICAgICAgICAgICAgICAgLy8gaWYoX2RlYnVnKWNvbnNvbGUubG9nKFwiVXBwZXIgbGVmdCBwb2ludCBpcyBcIiArIHJlY3QuZ2V0VG9wTGVmdCgpKTtcbiAgICAgICAgICAgICAgICAvLyBpZihfZGVidWcpY29uc29sZS5sb2coXCJMb3dlciByaWdodCBwb2ludCBpcyBcIiArIHJlY3QuZ2V0Qm90dG9tUmlnaHQoKSk7XG4gICAgICAgICAgICAgICAgLy8gaWYoX2RlYnVnKWNvbnNvbGUubG9nKFwiU2V0dGluZyB1cHBlciBsZWZ0IHBvaW50IHRvIFwiICsgdG9wTGVmdCk7XG4gICAgICAgICAgICAgICAgLy8gaWYoX2RlYnVnKWNvbnNvbGUubG9nKFwiU2V0dGluZyBsb3dlciByaWdodCBwb2ludCB0byBcIiArIGJvdHRvbVJpZ2h0KTtcbiAgICAgICAgICAgICAgICByZWN0LnggPSB0b3BMZWZ0Lng7XG4gICAgICAgICAgICAgICAgcmVjdC55ID0gdG9wTGVmdC55O1xuICAgICAgICAgICAgICAgIHJlY3Qud2lkdGggPSBib3R0b21SaWdodC54IC0gdG9wTGVmdC54O1xuICAgICAgICAgICAgICAgIHJlY3QuaGVpZ2h0ID0gYm90dG9tUmlnaHQueSAtIHRvcExlZnQueTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgb3NWaWV3ZXIudmlld2VyLnVwZGF0ZU92ZXJsYXkoIG9zVmlld2VyLm92ZXJsYXlzLmdldERyYXdpbmdPdmVybGF5KCkuZWxlbWVudCwgcmVjdCwgMCApO1xuICAgICAgICAgICAgZXZlbnQucHJldmVudERlZmF1bHRBY3Rpb24gPSB0cnVlO1xuICAgICAgICAgICAgcmV0dXJuIHRydWU7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSBpZiAoIF9kcmF3QXJlYSApIHtcbiAgICAgICAgICAgIF9kcmF3aW5nID0gdHJ1ZTtcbiAgICAgICAgICAgIGV2ZW50LnByZXZlbnREZWZhdWx0QWN0aW9uID0gdHJ1ZTtcbiAgICAgICAgICAgIHJldHVybiB0cnVlO1xuICAgICAgICAgICAgXG4gICAgICAgIH1cbiAgICB9XG4gICAgXG4gICAgZnVuY3Rpb24gX29uVmlld2VyUmVsZWFzZSggZXZlbnQgKSB7XG4gICAgICAgIGlmICggX2FjdGl2ZSApIHtcbiAgICAgICAgICAgIGlmICggX2RyYXdpbmcgJiYgX2ZpbmlzaEhvb2sgKSB7XG4gICAgICAgICAgICAgICAgX2ZpbmlzaEhvb2soIG9zVmlld2VyLm92ZXJsYXlzLmdldERyYXdpbmdPdmVybGF5KCkgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIF9kcmF3aW5nID0gZmFsc2U7XG4gICAgICAgICAgICBpZiAoIG9zVmlld2VyLm92ZXJsYXlzLmdldERyYXdpbmdPdmVybGF5KCkgKSB7XG4gICAgICAgICAgICAgICAgJCggb3NWaWV3ZXIub3ZlcmxheXMuZ2V0RHJhd2luZ092ZXJsYXkoKS5lbGVtZW50ICkudG9vbHRpcCgpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgX2RyYXdBcmVhID0gXCJcIjtcbiAgICAgICAgICAgIF9lbnRlclBvaW50ID0gbnVsbDtcbiAgICAgICAgICAgIGV2ZW50LnByZXZlbnREZWZhdWx0QWN0aW9uID0gdHJ1ZTtcbiAgICAgICAgICAgIHJldHVybiB0cnVlO1xuICAgICAgICB9XG4gICAgfVxuICAgIFxuICAgIGZ1bmN0aW9uIF9vblZpZXdlckRyYWdFbmQoIGV2ZW50ICkge1xuICAgICAgICBpZiAoIF9kcmF3aW5nICkge1xuICAgICAgICAgICAgX2RyYXdpbmcgPSBmYWxzZTtcbiAgICAgICAgICAgIGV2ZW50LnByZXZlbnREZWZhdWx0QWN0aW9uID0gdHJ1ZTtcbiAgICAgICAgICAgIHJldHVybiB0cnVlO1xuICAgICAgICB9XG4gICAgfVxuICAgIFxuICAgIGZ1bmN0aW9uIF9kaXNhYmxlVmlld2VyRXZlbnQoIGV2ZW50ICkge1xuICAgICAgICBpZiAoIF9kcmF3aW5nICkge1xuICAgICAgICAgICAgZXZlbnQucHJldmVudERlZmF1bHRBY3Rpb24gPSB0cnVlO1xuICAgICAgICAgICAgcmV0dXJuIHRydWU7XG4gICAgICAgIH1cbiAgICB9XG4gICAgZnVuY3Rpb24gY2hlY2tGb3JSZWN0SGl0KCBwb2ludCApIHtcbiAgICAgICAgdmFyIGk7XG4gICAgICAgIGZvciAoIGkgPSAwOyBpIDwgX3JlY3RzLmxlbmd0aDsgaSsrICkge1xuICAgICAgICAgICAgdmFyIHggPSBfcmVjdHNbIGkgXTtcbiAgICAgICAgICAgIGlmICggcG9pbnQueCA+IHguaGl0Qm94LmwgJiYgcG9pbnQueCA8IHguaGl0Qm94LnIgJiYgcG9pbnQueSA+IHguaGl0Qm94LnQgJiYgcG9pbnQueSA8IHguaGl0Qm94LmIgKSB7XG4gICAgICAgICAgICAgICAgdmFyIHRvcExlZnRIYiA9IHtcbiAgICAgICAgICAgICAgICAgICAgbDogeC54IC0gX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICB0OiB4LnkgLSBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIHI6IHgueCArIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgYjogeC55ICsgX2hiQWRkXG4gICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgICAgICB2YXIgdG9wUmlnaHRIYiA9IHtcbiAgICAgICAgICAgICAgICAgICAgbDogeC54ICsgeC53aWR0aCAtIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgdDogeC55IC0gX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICByOiB4LnggKyB4LndpZHRoICsgX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICBiOiB4LnkgKyBfaGJBZGRcbiAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICAgIHZhciBib3R0b21SaWdodEhiID0ge1xuICAgICAgICAgICAgICAgICAgICBsOiB4LnggKyB4LndpZHRoIC0gX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICB0OiB4LnkgKyB4LmhlaWdodCAtIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgcjogeC54ICsgeC53aWR0aCArIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgYjogeC55ICsgeC5oZWlnaHQgKyBfaGJBZGRcbiAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICAgIHZhciBib3R0b21MZWZ0SGIgPSB7XG4gICAgICAgICAgICAgICAgICAgIGw6IHgueCAtIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgdDogeC55ICsgeC5oZWlnaHQgLSBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIHI6IHgueCArIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgYjogeC55ICsgeC5oZWlnaHQgKyBfaGJBZGRcbiAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICAgIHZhciB0b3BIYiA9IHtcbiAgICAgICAgICAgICAgICAgICAgbDogeC54ICsgX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICB0OiB4LnkgLSBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIHI6IHgueCArIHgud2lkdGggLSBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIGI6IHgueSArIF9oYkFkZFxuICAgICAgICAgICAgICAgIH07XG4gICAgICAgICAgICAgICAgdmFyIHJpZ2h0SGIgPSB7XG4gICAgICAgICAgICAgICAgICAgIGw6IHgueCArIHgud2lkdGggLSBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIHQ6IHgueSArIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgcjogeC54ICsgeC53aWR0aCArIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgYjogeC55ICsgeC5oZWlnaHQgLSBfaGJBZGRcbiAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICAgIHZhciBib3R0b21IYiA9IHtcbiAgICAgICAgICAgICAgICAgICAgbDogeC54ICsgX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICB0OiB4LnkgKyB4LmhlaWdodCAtIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgcjogeC54ICsgeC53aWR0aCAtIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgYjogeC55ICsgeC5oZWlnaHQgKyBfaGJBZGRcbiAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICAgIHZhciBsZWZ0SGIgPSB7XG4gICAgICAgICAgICAgICAgICAgIGw6IHgueCAtIF9oYkFkZCxcbiAgICAgICAgICAgICAgICAgICAgdDogeC55ICsgX2hiQWRkLFxuICAgICAgICAgICAgICAgICAgICByOiB4LnggKyBfaGJBZGQsXG4gICAgICAgICAgICAgICAgICAgIGI6IHgueSArIHguaGVpZ2h0IC0gX2hiQWRkXG4gICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgIH1cbiAgICBcbiAgICAvKlxuICAgICAqIERldGVybWluZSB0aGUgc2lkZSBvZiB0aGUgcmVjdGFuZ2xlIHJlY3QgdGhlIHBvaW50IGxpZXMgb24gb3IgY2xvc2VzdCBhdCA8PW1heERpc3RcbiAgICAgKiBkaXN0YW5jZVxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9maW5kRWRnZSggcmVjdCwgcG9pbnQsIG1heERpc3QgKSB7XG4gICAgICAgIHZhciBkaXN0YW5jZVRvTGVmdCA9IF9kaXN0VG9TZWdtZW50KCBwb2ludCwgcmVjdC5nZXRUb3BMZWZ0KCksIHJlY3QuZ2V0Qm90dG9tTGVmdCgpICk7XG4gICAgICAgIHZhciBkaXN0YW5jZVRvQm90dG9tID0gX2Rpc3RUb1NlZ21lbnQoIHBvaW50LCByZWN0LmdldEJvdHRvbUxlZnQoKSwgcmVjdC5nZXRCb3R0b21SaWdodCgpICk7XG4gICAgICAgIHZhciBkaXN0YW5jZVRvUmlnaHQgPSBfZGlzdFRvU2VnbWVudCggcG9pbnQsIHJlY3QuZ2V0VG9wUmlnaHQoKSwgcmVjdC5nZXRCb3R0b21SaWdodCgpICk7XG4gICAgICAgIHZhciBkaXN0YW5jZVRvVG9wID0gX2Rpc3RUb1NlZ21lbnQoIHBvaW50LCByZWN0LmdldFRvcExlZnQoKSwgcmVjdC5nZXRUb3BSaWdodCgpICk7XG4gICAgICAgIFxuICAgICAgICB2YXIgbWluRGlzdGFuY2UgPSBNYXRoLm1pbiggZGlzdGFuY2VUb0xlZnQsIE1hdGgubWluKCBkaXN0YW5jZVRvUmlnaHQsIE1hdGgubWluKCBkaXN0YW5jZVRvVG9wLCBkaXN0YW5jZVRvQm90dG9tICkgKSApO1xuICAgICAgICBpZiAoIG1pbkRpc3RhbmNlIDw9IG1heERpc3QgKSB7XG4gICAgICAgICAgICBpZiAoIGRpc3RhbmNlVG9MZWZ0ID09PSBtaW5EaXN0YW5jZSApIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gb3NWaWV3ZXIudHJhbnNmb3JtUmVjdC5oaXRBcmVhcy5MRUZUO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaWYgKCBkaXN0YW5jZVRvUmlnaHQgPT09IG1pbkRpc3RhbmNlICkge1xuICAgICAgICAgICAgICAgIHJldHVybiBvc1ZpZXdlci50cmFuc2Zvcm1SZWN0LmhpdEFyZWFzLlJJR0hUO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaWYgKCBkaXN0YW5jZVRvVG9wID09PSBtaW5EaXN0YW5jZSApIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gb3NWaWV3ZXIudHJhbnNmb3JtUmVjdC5oaXRBcmVhcy5UT1A7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAoIGRpc3RhbmNlVG9Cb3R0b20gPT09IG1pbkRpc3RhbmNlICkge1xuICAgICAgICAgICAgICAgIHJldHVybiBvc1ZpZXdlci50cmFuc2Zvcm1SZWN0LmhpdEFyZWFzLkJPVFRPTTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gXCJcIjtcbiAgICB9XG4gICAgXG4gICAgLypcbiAgICAgKiBEZXRlcm1pbmUgdGhlIGNvcm5kZXIgb2YgdGhlIHJlY3RhbmdsZSByZWN0IHRoZSBwb2ludCBsaWVzIG9uIG9yIGNsb3Nlc3QgYXRcbiAgICAgKiA8PW1heERpc3QgZGlzdGFuY2VcbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfZmluZENvcm5lciggcmVjdCwgcG9pbnQsIG1heERpc3QgKSB7XG4gICAgICAgIHZhciBkaXN0YW5jZVRvVG9wTGVmdCA9IF9kaXN0KCBwb2ludCwgcmVjdC5nZXRUb3BMZWZ0KCkgKTtcbiAgICAgICAgdmFyIGRpc3RhbmNlVG9Cb3R0b21MZWZ0ID0gX2Rpc3QoIHBvaW50LCByZWN0LmdldEJvdHRvbUxlZnQoKSApO1xuICAgICAgICB2YXIgZGlzdGFuY2VUb1RvcFJpZ2h0ID0gX2Rpc3QoIHBvaW50LCByZWN0LmdldFRvcFJpZ2h0KCkgKTtcbiAgICAgICAgdmFyIGRpc3RhbmNlVG9Cb3R0b21SaWdodCA9IF9kaXN0KCBwb2ludCwgcmVjdC5nZXRCb3R0b21SaWdodCgpICk7XG4gICAgICAgIFxuICAgICAgICB2YXIgbWluRGlzdGFuY2UgPSBNYXRoLm1pbiggZGlzdGFuY2VUb1RvcExlZnQsIE1hdGgubWluKCBkaXN0YW5jZVRvVG9wUmlnaHQsIE1hdGgubWluKCBkaXN0YW5jZVRvQm90dG9tTGVmdCwgZGlzdGFuY2VUb0JvdHRvbVJpZ2h0ICkgKSApO1xuICAgICAgICBpZiAoIG1pbkRpc3RhbmNlIDw9IG1heERpc3QgKSB7XG4gICAgICAgICAgICBpZiAoIGRpc3RhbmNlVG9Ub3BMZWZ0ID09PSBtaW5EaXN0YW5jZSApIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gb3NWaWV3ZXIudHJhbnNmb3JtUmVjdC5oaXRBcmVhcy5UT1BMRUZUO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaWYgKCBkaXN0YW5jZVRvVG9wUmlnaHQgPT09IG1pbkRpc3RhbmNlICkge1xuICAgICAgICAgICAgICAgIHJldHVybiBvc1ZpZXdlci50cmFuc2Zvcm1SZWN0LmhpdEFyZWFzLlRPUFJJR0hUO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaWYgKCBkaXN0YW5jZVRvQm90dG9tTGVmdCA9PT0gbWluRGlzdGFuY2UgKSB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIG9zVmlld2VyLnRyYW5zZm9ybVJlY3QuaGl0QXJlYXMuQk9UVE9NTEVGVDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmICggZGlzdGFuY2VUb0JvdHRvbVJpZ2h0ID09PSBtaW5EaXN0YW5jZSApIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gb3NWaWV3ZXIudHJhbnNmb3JtUmVjdC5oaXRBcmVhcy5CT1RUT01SSUdIVDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gXCJcIjtcbiAgICB9XG4gICAgXG4gICAgZnVuY3Rpb24gX3NxciggeCApIHtcbiAgICAgICAgcmV0dXJuIHggKiB4XG4gICAgfVxuICAgIGZ1bmN0aW9uIF9kaXN0MiggdiwgdyApIHtcbiAgICAgICAgcmV0dXJuIF9zcXIoIHYueCAtIHcueCApICsgX3Nxciggdi55IC0gdy55IClcbiAgICB9XG4gICAgZnVuY3Rpb24gX2Rpc3QoIHYsIHcgKSB7XG4gICAgICAgIHJldHVybiBNYXRoLnNxcnQoIF9kaXN0MiggdiwgdyApIClcbiAgICB9XG4gICAgZnVuY3Rpb24gX2Rpc3RUb1NlZ21lbnRTcXVhcmVkKCBwLCB2LCB3ICkge1xuICAgICAgICB2YXIgbDIgPSBfZGlzdDIoIHYsIHcgKTtcbiAgICAgICAgaWYgKCBsMiA9PSAwIClcbiAgICAgICAgICAgIHJldHVybiBfZGlzdDIoIHAsIHYgKTtcbiAgICAgICAgdmFyIHQgPSAoICggcC54IC0gdi54ICkgKiAoIHcueCAtIHYueCApICsgKCBwLnkgLSB2LnkgKSAqICggdy55IC0gdi55ICkgKSAvIGwyO1xuICAgICAgICBpZiAoIHQgPCAwIClcbiAgICAgICAgICAgIHJldHVybiBfZGlzdDIoIHAsIHYgKTtcbiAgICAgICAgaWYgKCB0ID4gMSApXG4gICAgICAgICAgICByZXR1cm4gX2Rpc3QyKCBwLCB3ICk7XG4gICAgICAgIHJldHVybiBfZGlzdDIoIHAsIHtcbiAgICAgICAgICAgIHg6IHYueCArIHQgKiAoIHcueCAtIHYueCApLFxuICAgICAgICAgICAgeTogdi55ICsgdCAqICggdy55IC0gdi55IClcbiAgICAgICAgfSApO1xuICAgIH1cbiAgICBmdW5jdGlvbiBfZGlzdFRvU2VnbWVudCggcG9pbnQsIGxpbmVQMSwgbGluZVAyICkge1xuICAgICAgICByZXR1cm4gTWF0aC5zcXJ0KCBfZGlzdFRvU2VnbWVudFNxdWFyZWQoIHBvaW50LCBsaW5lUDEsIGxpbmVQMiApICk7XG4gICAgfVxuICAgIHJldHVybiBvc1ZpZXdlcjtcbiAgICBcbn0gKSggdmlld0ltYWdlIHx8IHt9LCBqUXVlcnkgKTtcbiIsIi8qKlxuICogVGhpcyBmaWxlIGlzIHBhcnQgb2YgdGhlIEdvb2JpIHZpZXdlciAtIGEgY29udGVudCBwcmVzZW50YXRpb24gYW5kIG1hbmFnZW1lbnRcbiAqIGFwcGxpY2F0aW9uIGZvciBkaWdpdGl6ZWQgb2JqZWN0cy5cbiAqIFxuICogVmlzaXQgdGhlc2Ugd2Vic2l0ZXMgZm9yIG1vcmUgaW5mb3JtYXRpb24uIC0gaHR0cDovL3d3dy5pbnRyYW5kYS5jb20gLVxuICogaHR0cDovL2RpZ2l2ZXJzby5jb21cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGZyZWUgc29mdHdhcmU7IHlvdSBjYW4gcmVkaXN0cmlidXRlIGl0IGFuZC9vciBtb2RpZnkgaXQgdW5kZXIgdGhlIHRlcm1zXG4gKiBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYXMgcHVibGlzaGVkIGJ5IHRoZSBGcmVlIFNvZnR3YXJlIEZvdW5kYXRpb247IGVpdGhlclxuICogdmVyc2lvbiAyIG9mIHRoZSBMaWNlbnNlLCBvciAoYXQgeW91ciBvcHRpb24pIGFueSBsYXRlciB2ZXJzaW9uLlxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZGlzdHJpYnV0ZWQgaW4gdGhlIGhvcGUgdGhhdCBpdCB3aWxsIGJlIHVzZWZ1bCwgYnV0IFdJVEhPVVQgQU5ZXG4gKiBXQVJSQU5UWTsgd2l0aG91dCBldmVuIHRoZSBpbXBsaWVkIHdhcnJhbnR5IG9mIE1FUkNIQU5UQUJJTElUWSBvciBGSVRORVNTIEZPUiBBXG4gKiBQQVJUSUNVTEFSIFBVUlBPU0UuIFNlZSB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgZm9yIG1vcmUgZGV0YWlscy5cbiAqIFxuICogWW91IHNob3VsZCBoYXZlIHJlY2VpdmVkIGEgY29weSBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYWxvbmcgd2l0aCB0aGlzXG4gKiBwcm9ncmFtLiBJZiBub3QsIHNlZSA8aHR0cDovL3d3dy5nbnUub3JnL2xpY2Vuc2VzLz4uXG4gKiBcbiAqIE1vZHVsZSB3aGljaCBoYW5kbGVzIHRoZSB6b29tc2xpZGVyIGZ1bmN0aW9uYWxpdHkuXG4gKiBcbiAqIEB2ZXJzaW9uIDMuMi4wXG4gKiBAbW9kdWxlIHZpZXdJbWFnZS56b29tU2xpZGVyXG4gKiBAcmVxdWlyZXMgalF1ZXJ5XG4gKi9cbnZhciB2aWV3SW1hZ2UgPSAoIGZ1bmN0aW9uKCBvc1ZpZXdlciApIHtcbiAgICAndXNlIHN0cmljdCc7XG4gICAgXG4gICAgdmFyIF9kZWJ1ZyA9IGZhbHNlO1xuICAgIHZhciBfem9vbVNsaWRlciA9IHt9O1xuICAgIHZhciBfZGVmYXVsdHMgPSB7XG4gICAgICAgIGdsb2JhbDoge1xuICAgICAgICAgICAgLyoqXG4gICAgICAgICAgICAgKiBUaGUgcG9zaXRpb24gb2YgdGhlIHpvb20tc2xpZGVyIGlzIFwiZGlsYXRlZFwiIGJ5IGEgZnVuY3Rpb24gZCh6b29tKSA9XG4gICAgICAgICAgICAgKiAxL3NsaWRlckRpbGF0aW9uKnRhblthdGFuKHNsaWRlckRpbGF0aW9uKSp6b29tXSBUaGlzIG1ha2VzIHRoZSBzbGlkZXJcbiAgICAgICAgICAgICAqIHBvc2l0aW9uIGNoYW5nZSBzbG93ZXIgZm9yIHNtYWxsIHpvb20gYW5kIGZhc3RlciBmb3IgbGFyZ2VyIHpvb20gVGhlXG4gICAgICAgICAgICAgKiBmdW5jdGlvbiBpcyBjaG9zZW4gc28gdGhhdCBkKDApID0gMCBhbmQgZCgxKSA9IDFcbiAgICAgICAgICAgICAqL1xuICAgICAgICAgICAgc2xpZGVyRGlsYXRpb246IDEyXG4gICAgICAgIH1cbiAgICB9O1xuICAgIFxuICAgIG9zVmlld2VyLnpvb21TbGlkZXIgPSB7XG4gICAgICAgIGluaXQ6IGZ1bmN0aW9uKCBjb25maWcgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ29zVmlld2VyLnpvb21TbGlkZXIuaW5pdCcgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgJC5leHRlbmQoIHRydWUsIF9kZWZhdWx0cywgY29uZmlnICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmICggJCggX2RlZmF1bHRzLmdsb2JhbC56b29tU2xpZGVyICkgKSB7XG4gICAgICAgICAgICAgICAgb3NWaWV3ZXIuem9vbVNsaWRlci5hZGRab29tU2xpZGVyKCBfZGVmYXVsdHMuZ2xvYmFsLnpvb21TbGlkZXIgKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAvLyBoYW5kbGVyIGZvciBvcGVuU2VhZHJhZ29uIE9iamVjdFxuICAgICAgICAgICAgICAgIG9zVmlld2VyLnZpZXdlci5hZGRIYW5kbGVyKCAnem9vbScsIGZ1bmN0aW9uKCBkYXRhICkge1xuICAgICAgICAgICAgICAgICAgICBvc1ZpZXdlci56b29tU2xpZGVyLmJ1dHRvblRvWm9vbSggZGF0YS56b29tICk7XG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgfVxuICAgICAgICB9LFxuICAgICAgICBidXR0b25Ub01vdXNlOiBmdW5jdGlvbiggbW91c2VQb3MgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ29zVmlld2VyLnpvb21TbGlkZXIuYnV0dG9uVG9Nb3VzZTogbW91c2VQb3MgLSAnICsgbW91c2VQb3MgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgdmFyIG9mZnNldCA9IF96b29tU2xpZGVyLiRidXR0b24ud2lkdGgoKSAvIDI7XG4gICAgICAgICAgICB2YXIgbmV3UG9zID0gbW91c2VQb3MgLSBvZmZzZXQ7XG4gICAgICAgICAgICBpZiAoIG5ld1BvcyA8IDAgKSB7XG4gICAgICAgICAgICAgICAgbmV3UG9zID0gMDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmICggbmV3UG9zICsgMiAqIG9mZnNldCA+IF96b29tU2xpZGVyLmFic29sdXRlV2lkdGggKSB7XG4gICAgICAgICAgICAgICAgbmV3UG9zID0gX3pvb21TbGlkZXIuYWJzb2x1dGVXaWR0aCAtIDIgKiBvZmZzZXQ7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBfem9vbVNsaWRlci4kYnV0dG9uLmNzcygge1xuICAgICAgICAgICAgICAgIGxlZnQ6IG5ld1Bvc1xuICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgX3pvb21TbGlkZXIuYnV0dG9uUG9zaXRpb24gPSBuZXdQb3M7XG4gICAgICAgICAgICB2YXIgZmFjdG9yID0gKCBuZXdQb3MgLyAoIF96b29tU2xpZGVyLmFic29sdXRlV2lkdGggLSBvZmZzZXQgKiAyICkgKTtcbiAgICAgICAgICAgIGZhY3RvciA9IDEgLyBfZGVmYXVsdHMuZ2xvYmFsLnNsaWRlckRpbGF0aW9uICogTWF0aC50YW4oIE1hdGguYXRhbiggX2RlZmF1bHRzLmdsb2JhbC5zbGlkZXJEaWxhdGlvbiApICogZmFjdG9yICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHZhciBuZXdTY2FsZSA9IG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC5nZXRNaW5ab29tKCkgKyAoIG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC5nZXRNYXhab29tKCkgLSBvc1ZpZXdlci52aWV3ZXIudmlld3BvcnQuZ2V0TWluWm9vbSgpICkgKiBmYWN0b3I7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnb3NWaWV3ZXIuem9vbVNsaWRlci5idXR0b25Ub01vdXNlOiBuZXdTY2FsZSAtICcgKyBuZXdTY2FsZSApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICBvc1ZpZXdlci5jb250cm9scy56b29tVG8oIG5ld1NjYWxlICk7XG4gICAgICAgIH0sXG4gICAgICAgIGJ1dHRvblRvWm9vbTogZnVuY3Rpb24oIHNjYWxlICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICdvc1ZpZXdlci56b29tU2xpZGVyLmJ1dHRvblRvWm9vbTogc2NhbGUgLSAnICsgc2NhbGUgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgaWYgKCAhX3pvb21TbGlkZXIgfHwgIW9zVmlld2VyLnZpZXdlci52aWV3cG9ydCApIHtcbiAgICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIGNvbnNvbGUubG9nKFwiRGlsYXRpb24gPSBcIiwgb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0LmdldE1pblpvb20oKSlcbiAgICAgICAgICAgIC8vIGNvbnNvbGUubG9nKFwibWluWm9vbSA9IFwiLCBvc1ZpZXdlci52aWV3ZXIudmlld3BvcnQuZ2V0TWluWm9vbSgpKTtcbiAgICAgICAgICAgIC8vIGNvbnNvbGUubG9nKFwibWF4Wm9vbSA9IFwiLCBvc1ZpZXdlci52aWV3ZXIudmlld3BvcnQuZ2V0TWF4Wm9vbSgpKVxuICAgICAgICAgICAgLy8gY29uc29sZS5sb2coXCJzY2FsZSA9IFwiLCBzY2FsZSk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHZhciBmYWN0b3IgPSAoIHNjYWxlIC0gb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0LmdldE1pblpvb20oKSApIC8gKCBvc1ZpZXdlci52aWV3ZXIudmlld3BvcnQuZ2V0TWF4Wm9vbSgpIC0gb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0LmdldE1pblpvb20oKSApO1xuICAgICAgICAgICAgLy8gY29uc29sZS5sb2coIFwiZmFjdG9yID0gXCIsIGZhY3RvciApO1xuICAgICAgICAgICAgLy8gICAgICAgICAgICBcbiAgICAgICAgICAgIGZhY3RvciA9IDEgLyBNYXRoLmF0YW4oIF9kZWZhdWx0cy5nbG9iYWwuc2xpZGVyRGlsYXRpb24gKSAqIE1hdGguYXRhbiggX2RlZmF1bHRzLmdsb2JhbC5zbGlkZXJEaWxhdGlvbiAqIGZhY3RvciApO1xuICAgICAgICAgICAgdmFyIG5ld1BvcyA9IGZhY3RvciAqICggX3pvb21TbGlkZXIuYWJzb2x1dGVXaWR0aCAtIF96b29tU2xpZGVyLiRidXR0b24ud2lkdGgoKSApO1xuICAgICAgICAgICAgLy8gdmFyIG5ld1BvcyA9ICggKCBzY2FsZSAtIG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC5nZXRNaW5ab29tKCkgKSAvIChcbiAgICAgICAgICAgIC8vIG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC5nZXRNYXhab29tKCkgLVxuICAgICAgICAgICAgLy8gb3NWaWV3ZXIudmlld2VyLnZpZXdwb3J0LmdldE1pblpvb20oKSApIClcbiAgICAgICAgICAgIC8vICogKCBfem9vbVNsaWRlci5hYnNvbHV0ZVdpZHRoIC0gX3pvb21TbGlkZXIuJGJ1dHRvbi53aWR0aCgpICk7XG4gICAgICAgICAgICAvLyBjb25zb2xlLmxvZyggXCJwb3MgPSBcIiwgbmV3UG9zICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmICggTWF0aC5hYnMoIG9zVmlld2VyLnZpZXdlci52aWV3cG9ydC5nZXRNYXhab29tKCkgLSBzY2FsZSApIDwgMC4wMDAwMDAwMDAxICkge1xuICAgICAgICAgICAgICAgIG5ld1BvcyA9IF96b29tU2xpZGVyLmFic29sdXRlV2lkdGggLSBfem9vbVNsaWRlci4kYnV0dG9uLndpZHRoKCk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmICggbmV3UG9zIDwgMCApIHtcbiAgICAgICAgICAgICAgICBuZXdQb3MgPSAwO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICBfem9vbVNsaWRlci4kYnV0dG9uLmNzcygge1xuICAgICAgICAgICAgICAgIGxlZnQ6IG5ld1Bvc1xuICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgX3pvb21TbGlkZXIuYnV0dG9uUG9zaXRpb24gPSBuZXdQb3M7XG4gICAgICAgIH0sXG4gICAgICAgIHpvb21TbGlkZXJNb3VzZVVwOiBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnb3NWaWV3ZXIuem9vbVNsaWRlci56b29tU2xpZGVyTW91c2VVcCcgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgX3pvb21TbGlkZXIubW91c2Vkb3duID0gZmFsc2U7XG4gICAgICAgIH0sXG4gICAgICAgIHpvb21TbGlkZXJNb3VzZU1vdmU6IGZ1bmN0aW9uKCBldnQgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ29zVmlld2VyLnpvb21TbGlkZXIuem9vbVNsaWRlck1vdXNlTW92ZTogZXZ0IC0gJyArIGV2dCApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICBpZiAoICFfem9vbVNsaWRlci5tb3VzZWRvd24gKSB7XG4gICAgICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgdmFyIG9mZnNldCA9ICQoIHRoaXMgKS5vZmZzZXQoKTtcbiAgICAgICAgICAgIHZhciBoaXRYID0gZXZ0LnBhZ2VYIC0gb2Zmc2V0LmxlZnQ7XG4gICAgICAgICAgICBvc1ZpZXdlci56b29tU2xpZGVyLmJ1dHRvblRvTW91c2UoIGhpdFggKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICdvc1ZpZXdlci56b29tU2xpZGVyLnpvb21TbGlkZXJNb3VzZU1vdmU6IG1vdmluZyAtICcgKyBoaXRYICk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0sXG4gICAgICAgIHpvb21TbGlkZXJNb3VzZURvd246IGZ1bmN0aW9uKCBldnQgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ29zVmlld2VyLnpvb21TbGlkZXIuem9vbVNsaWRlck1vdXNlRG93bjogZXZ0IC0gJyArIGV2dCApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICBfem9vbVNsaWRlci5tb3VzZWRvd24gPSB0cnVlO1xuICAgICAgICAgICAgdmFyIG9mZnNldCA9ICQoIHRoaXMgKS5vZmZzZXQoKTtcbiAgICAgICAgICAgIHZhciBoaXRYID0gZXZ0LnBhZ2VYIC0gb2Zmc2V0LmxlZnQ7XG4gICAgICAgICAgICBvc1ZpZXdlci56b29tU2xpZGVyLmJ1dHRvblRvTW91c2UoIGhpdFggKTtcbiAgICAgICAgfSxcbiAgICAgICAgYnV0dG9uTW91c2VEb3duOiBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnb3NWaWV3ZXIuem9vbVNsaWRlci5idXR0b25Nb3VzZURvd24nICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIF96b29tU2xpZGVyLm1vdXNlZG93biA9IHRydWU7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHJldHVybiBmYWxzZTtcbiAgICAgICAgfSxcbiAgICAgICAgYWRkWm9vbVNsaWRlcjogZnVuY3Rpb24oIGVsZW1lbnQgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ29zVmlld2VyLnpvb21TbGlkZXIuYWRkWm9vbVNsaWRlcjogZWxlbWVudCAtICcgKyBlbGVtZW50ICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIF96b29tU2xpZGVyLiRlbGVtZW50ID0gJCggZWxlbWVudCApO1xuICAgICAgICAgICAgX3pvb21TbGlkZXIuJGJ1dHRvbiA9IF96b29tU2xpZGVyLiRlbGVtZW50LmNoaWxkcmVuKCBfZGVmYXVsdHMuZ2xvYmFsLnpvb21TbGlkZXJIYW5kbGUgKTtcbiAgICAgICAgICAgIF96b29tU2xpZGVyLmJ1dHRvblBvc2l0aW9uID0gMDtcbiAgICAgICAgICAgIF96b29tU2xpZGVyLmFic29sdXRlV2lkdGggPSBfem9vbVNsaWRlci4kZWxlbWVudC5pbm5lcldpZHRoKCk7XG4gICAgICAgICAgICBfem9vbVNsaWRlci5tb3VzZWRvd24gPSBmYWxzZTtcbiAgICAgICAgICAgIF96b29tU2xpZGVyLiRidXR0b24ub24oICdtb3VzZWRvd24nLCBvc1ZpZXdlci56b29tU2xpZGVyLmJ1dHRvbk1vdXNlRG93biApO1xuICAgICAgICAgICAgX3pvb21TbGlkZXIuJGVsZW1lbnQuY2xpY2soIG9zVmlld2VyLnpvb21TbGlkZXIuX3pvb21TbGlkZXJDbGljayApO1xuICAgICAgICAgICAgX3pvb21TbGlkZXIuJGVsZW1lbnQubW91c2Vkb3duKCBvc1ZpZXdlci56b29tU2xpZGVyLnpvb21TbGlkZXJNb3VzZURvd24gKTtcbiAgICAgICAgICAgIF96b29tU2xpZGVyLiRlbGVtZW50Lm1vdXNlbW92ZSggb3NWaWV3ZXIuem9vbVNsaWRlci56b29tU2xpZGVyTW91c2VNb3ZlICk7XG4gICAgICAgICAgICAkKCBkb2N1bWVudCApLm9uKCAnbW91c2V1cCcsIG9zVmlld2VyLnpvb21TbGlkZXIuem9vbVNsaWRlck1vdXNlVXAgKTtcbiAgICAgICAgfVxuICAgIH07XG4gICAgXG4gICAgcmV0dXJuIG9zVmlld2VyO1xuICAgIFxufSApKCB2aWV3SW1hZ2UgfHwge30sIGpRdWVyeSApO1xuIl19
