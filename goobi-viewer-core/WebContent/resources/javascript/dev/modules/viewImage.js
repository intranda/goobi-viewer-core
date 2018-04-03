/*!
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 * - http://www.intranda.com
 * - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */var viewImage = ( function() {
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
            overlayGroups: [ {
                name: "searchHighlighting",
                styleClass: "coords-highlighting",
                interactive: false
            }, {
                name: "ugc",
                styleClass: "ugcBox",
                interactive: true
            
            } ],
            zoomSpeed: 1.25,
            maxZoomLevel: 20,
            minZoomLevel: 1,
            imageControlsActive: true,
            visibilityRatio: 0.4,
            loadImageTimeout: 10 * 60 * 1000,
            maxParallelImageLoads: 1,
            adaptContainerHeight: false,
            footerHeight: 50,
            rememberZoom: false,
            rememberRotation: false,
        },
        image: {},
        getOverlayGroup: function( name ) {
            var allGroups = _defaults.global.overlayGroups;
            for ( var int = 0; int < allGroups.length; int++ ) {
                var group = allGroups[ int ];
                if ( group.name === name ) {
                    return group;
                }
            }
        },
        getCoordinates: function( name ) {
            var coodinatesArray = _defaults.image.highlightCoords;
            if ( coodinatesArray ) {
                for ( var int = 0; int < coodinatesArray.length; int++ ) {
                    var coords = coodinatesArray[ int ];
                    if ( coords.name === name ) {
                        return coords;
                    }
                }
            }
        },
    };
    
    osViewer = {
        viewer: null,
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'osViewer.init' );
                console.log( '##############################' );
            }
            
            // constructor
            $.extend( true, _defaults, config );
            // convert mimeType "image/jpeg" to "image/jpg" to provide correct
			// iiif calls
            _defaults.image.mimeType = _defaults.image.mimeType.replace("jpeg","jpg");
            _container = $( "#" + _defaults.global.divId );
            
            var sources = _defaults.image.tileSource;
            if(typeof sources === 'string' && sources.startsWith("[")) {
            	sources = JSON.parse(sources);
            } else if(!$.isArray(sources)) {
            	sources = [sources];
            }
            var promises = [];
            for ( var i=0; i<sources.length; i++) {
            	var source = sources[i];
            	// returns the OpenSeadragon.TileSource if it can be created,
				// otherweise
                // rejects the promise
            	var promise = viewImage.createTileSource(source);
            	promises.push(promise);	
	        }                
            return Q.all(promises).then(function(tileSources) {
            	var minWidth = Number.MAX_VALUE;  
            	var minHeight = Number.MAX_VALUE;
            	var minAspectRatio = Number.MAX_VALUE;
            	for ( var j=0; j<tileSources.length; j++) {
            		var tileSource = tileSources[j];
            		minWidth = Math.min(minWidth, tileSource.width);
            		minHeight = Math.min(minHeight, tileSource.height);
            		minAspectRatio = Math.min(minAspectRatio, tileSource.aspectRatio);
	            }
	            if(_debug) {                    
            	    console.log("Min aspect ratio = " + minAspectRatio);            	    
	            }
            	var x = 0;
            	for ( var i=0; i<tileSources.length; i++) {
	        		var tileSource = tileSources[i];
	        		tileSources[i] = {
	        				tileSource: tileSource,
	        				width: tileSource.aspectRatio/minAspectRatio,
// height: minHeight/tileSource.height,
	                		x : x,
	                		y: 0,
	                    }
	        		x += tileSources[i].width;
	                }              
            	return viewImage.loadImage(tileSources);
            });
            
        },
        loadImage : function(tileSources) {
            if ( _debug ) {
                console.log( 'Loading image with tilesource: ', tileSources );
            }
              
            osViewer.loadFooter();            
         
            osViewer.viewer = new OpenSeadragon( {
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
            } );
            var result = Q.defer();
                
            osViewer.observables = createObservables(window, osViewer.viewer);  
                
            osViewer.observables.viewerOpen.subscribe(function(openevent, loadevent) {            
                result.resolve(osViewer);                
            }, function(error) {            
                result.reject(error);                
            });                
                
                
            // Calculate sizes if redraw is required
            
            osViewer.observables.redrawRequired.subscribe(function(event) {            
                if(_debug) {
                    console.log("viewer " + event.osState + "ed with target location ", event.targetLocation);                    
                }
                
                osViewer.redraw();
            });
                
            if ( osViewer.controls ) {                    
                osViewer.controls.init( _defaults );
            }
            
            if ( osViewer.zoomSlider ) {
                osViewer.zoomSlider.init( _defaults );                
            }
            
            if ( osViewer.overlays ) {
                osViewer.overlays.init( _defaults );                
            }                
            
            if ( osViewer.drawRect ) {
                osViewer.drawRect.init();                
            }   
            
            if ( osViewer.transformRect ) {            
                osViewer.transformRect.init();                
            }                
            
            osViewer.observables.redrawRequired.connect();                
            
            return result.promise;
        },
        getObservables: function() {
        	console.log("Observables = ", osViewer.observables);
        	return osViewer.observables;
        },
        hasFooter: function() {
            return _footerImage != null;
        },
        getConfig: function() {
            return _defaults;
        },
        loadFooter: function() {
            if ( _defaults.image.baseFooterUrl && _defaults.global.footerHeight > 0 ) {                
                _footerImage = new Image();
                _footerImage.src = _defaults.image.baseFooterUrl.replace( "{width}", Math.round( _container.width() ) ).replace( "{height}", Math.round( _defaults.global.footerHeight ) );                
                _footerImage.src = _defaults.image.baseFooterUrl.replace( "/full/max/", "/full/!" + Math.round( _container.width() ) + "," +  Math.round( _defaults.global.footerHeight ) + "/");                
                _footerImage.onload = function() {
                    if ( _debug ) {
                        console.log( "loading footer image ", _footerImage );
                        console.log( "Calculating image Footer size" );
                    }
                    
                    osViewer.drawFooter();
                };
            }
        },
        drawFooter: function() {
            if ( osViewer.viewer ) {
                _overlayFooter();
            }
            
            osViewer.viewer.removeHandler( 'update-viewport', _overlayFooter );
            osViewer.viewer.addHandler( 'update-viewport', _overlayFooter );
        },        
        getOverlayGroup: function( name ) {
            return _defaults.getOverlayGroup( name );
        },
        getHighlightCoordinates: function( name ) {
            return _defaults.getCoordinates( name );
        },
        createPyramid: function( imageInfo ) {
            if(_debug) {
                console.log("Creating legacy tilesource from imageInfo ", imageInfo);
            }
            var fileExtension = _defaults.image.mimeType;
            fileExtension = fileExtension.replace( "image/", "" );
            fileExtension = fileExtension.replace("jpeg", "jpg").replace("tiff", "tif");
            var imageLevels = [];
            var tileSource;
            if(Array.isArray(imageInfo)) {
            	imageInfo.forEach(function(level) {
            		level.mimetype = _defaults.image.mimeType;
            	});
            	tileSource = new OpenSeadragon.LegacyTileSource(imageInfo);
            } else if(imageInfo.sizes) {
	            imageInfo.sizes.forEach(function(size) {
	                if(_debug) {                    
	                    console.log("Image level width = ", size.width)
	                    console.log("Image level height = ", size.height)
	                }
	                
	                var level = {
	                    mimetype: _defaults.image.mimeType,
	                    url: imageInfo["@id"].replace( "/info.json", "" ) + "/full/" + size.width + ",/0/default." + fileExtension,
	                    width: imageInfo.width,
	                    height: imageInfo.height
	                };
	                
	                if(_debug) {
	                    console.log("Created level ", level);
	                }
	                
	                imageLevels.push( level );
	            });
	            
	            tileSource = new OpenSeadragon.LegacyTileSource(imageLevels);
            } else {
            	tileSource = new OpenSeadragon.ImageTileSource({
            		url: imageInfo["@id"].replace( "/info.json", "" ) + "/full/full/0/default." + fileExtension,
            		crossOriginPolicy: "Anonymous",
            		buildPyramid: false
            	});
            }
            
            return tileSource;
        },
        getSizes: function() {
            return osViewer.sizes;
        },
        addImage: function( url, width, height ) {
            if ( _debug ) {
                console.log( 'osViewer.addImage: url - ' + url );
                console.log( 'osViewer.addImage: width - ' + width );
                console.log( 'osViewer.addImage: height - ' + height );
            }
            if ( osViewer.viewer ) {
                osViewer.viewer.addTiledImage( {
                    tileSource: {
                        type: "legacy-image-pyramid",
                        levels: [ {
                            url: url,
                            height: height,
                            width: width
                        } ]
                    },
                    x: 0,
                    y: 1.6,
                    width: 1
                } );
            }
            else {
                if ( _debug ) {
                    console.log( "Viewer not initialized yet; cannot add image" );
                }
            }
        },
        getImageInfo: function() {
            if(osViewer.viewer) {
                return osViewer.viewer.tileSources;
            }
            return null;
        },
        getScaleToOriginalSize: function(imageNo) {
        	return 1.0;
// if(!imageNo) {
// imageNo = 0;
// }
// var displaySize = osViewer.viewer.viewport._contentSize.x;
// return osViewer.getImageInfo()[imageNo].tileSource.width / displaySize;
        },
        scaleToOriginalSize: function( value, imageNo ) {
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
        scaleToImageSize: function( value, imageNo ) {
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
        close: function() {
            if ( _debug ) {
                console.log( "Closing openSeadragon viewer" );
            }
            
            if ( osViewer.viewer ) {
                osViewer.viewer.destroy();
            }
        },
        redraw: function() {
            if(osViewer.controls) {                    	
            	osViewer.controls.setPanning( true );
            }
            _calculateSizes(osViewer);
        },
        setImageSizes: function(imageInfo, sizes) {
        	if(sizes) {        		
        		var string = sizes.replace(/[\{\}]/, "");
        		var sizes = JSON.parse(sizes);
        		var iiifSizes = [];
        		sizes.forEach(function(size) {
        			iiifSizes.push({"width": parseInt(size), "height": parseInt(size)});
        		});
        		if(iiifSizes.length > 0) {				
        			imageInfo.sizes = iiifSizes;
        		} else {
        			delete imageInfo.sizes;
        		}
        	}
        },
        setTileSizes: function(imageInfo, tiles) {
        	if(tiles) {        		
        		var tileString = viewImage.getConfig().global.tileSizes.replace(/(\d+)/, '"$1"').replace("=", ":");
        		var tiles = JSON.parse(tileString);
        		var iiifTiles = [];
        		
        		Object.keys(tiles).forEach(function(size) {
        			var scaleFactors = tiles[size];
        			iiifTiles.push({"width": parseInt(size), "height": parseInt(size), "scaleFactors": scaleFactors})
        		});
        		
        		imageInfo.tiles = iiifTiles;
        	}
        },
        onFirstTileLoaded: function() {
        	var defer = Q.defer();
        	
        	if(viewImage.observables) {
        		viewImage.observables.firstTileLoaded.subscribe(function(event) {
        			defer.resolve(event);
        		}, function(error) {
        			defer.reject(error)
        		});
        	} else {
        		defer.reject("No observables defined");
        	}
        	
        	return defer.promise;
        },
        createTileSource: function(source) {

        	var result = Q.defer();

            viewImage.tileSourceResolver.resolveAsJson(source)
            .then(
            		function(imageInfo) {                        
		                if(_debug) {                
		                    console.log("IIIF image info ", imageInfo);                        
		                }               
		                viewImage.setImageSizes(imageInfo, _defaults.global.imageSizes);       
		                viewImage.setTileSizes(imageInfo, _defaults.global.tileSizes);                
		                var tileSource;
		                if(imageInfo.tiles && imageInfo.tiles.length > 0) {
		                    tileSource = new OpenSeadragon.IIIFTileSource(imageInfo);                    
		                } else {                
		                    console.log("tiles? ", imageInfo.tiles);
		                    tileSource  = osViewer.createPyramid(imageInfo);                    
		                }
		                
		                return tileSource;                
            		},
		            function(error) {            
		                if(viewImage.tileSourceResolver.isURI(_defaults.image.tileSource)) {
		                    if(_debug) {                    
		                        console.log("Image URL", _defaults.image.tileSource);                        
		                    }
		                    
		                    var tileSource = new OpenSeadragon.ImageTileSource( {                    
		                        url: _defaults.image.tileSource,                        
		                        buildPyramid: true,                        
		                        crossOriginPolicy: false                        
		                    } );
		
		                    return tileSource;                    
		                } else {                
		                    var errorMsg = "Failed to load tilesource from " + tileSource;
		                    
		                    if(_debug) {                    
		                        console.log(errorMsg);                        
        }
		                    
		                    return Q.reject(errorMsg);
		                    
		                }              
		            })
            .then(function(tileSource) {              
                result.resolve(tileSource);          
            }).catch(function(errorMessage) {              
                result.reject(errorMessage);          
            });
            return result.promise;
        }
    };
    
    function createObservables(window, viewer) {
        var observables = {};
        
        observables.viewerOpen = Rx.Observable.create(function(observer) {
            viewer.addOnceHandler( 'open', function( event ) {
                event.osState = "open";
                
                if(Number.isNaN(event.eventSource.viewport.getHomeBounds().x)) {
                    return observer.onError("Unknow error loading image from ", _defaults.image.tileSource);
                } else {                    
                    return observer.onNext(event);
                }
            } );
            viewer.addOnceHandler( 'open-failed', function( event ) {
                event.osState = "open-failed";
                console.log("Failed to open openseadragon ");
                
                return observer.onError(event);
            } );
        });
        
        observables.firstTileLoaded = Rx.Observable.create(function(observer) {
        	viewer.addOnceHandler( 'tile-loaded', function( event ) {
                event.osState = "tile-loaded";
                
                return observer.onNext(event);
            } );
        	viewer.addOnceHandler( 'tile-load-failed', function( event ) {
                event.osState = "tile-load-failed";
                console.log("Failed to load tile");
                
                return observer.onError(event);
            } );
        });
        
        observables.viewerZoom = Rx.Observable.create(function(observer) {
            viewer.addHandler( 'zoom', function( event ) {
                return observer.onNext(event);
            } );
        });
        observables.animationComplete = Rx.Observable.create(function(observer) {
            viewer.addHandler( 'animation-finish', function( event ) {
                return observer.onNext(event);
            } );
        });
        observables.viewportUpdate = Rx.Observable.create(function(observer) {
            viewer.addHandler( 'update-viewport', function( event ) {
                return observer.onNext(event);
            } );
        });
        observables.animation = Rx.Observable.create(function(observer) {
            viewer.addHandler( 'animation', function( event ) {
                return observer.onNext(event);
            } );
        });
        observables.viewerRotate = Rx.Observable.create(function(observer) {
            viewer.addHandler( 'rotate', function( event ) {
                event.osState = "rotate";
                return observer.onNext(event);
            } );
        });
        observables.canvasResize = Rx.Observable.create(function(observer) {
            viewer.addHandler( 'resize', function( event ) {
                event.osState = "resize";
                
                return observer.onNext(event);
            } );
        });
        observables.windowResize = Rx.Observable.fromEvent(window, "resize").map(function(event) {
            event.osState = "window resize";
            
            return event;
        });
        observables.overlayRemove = Rx.Observable.create(function(observer) {
            viewer.addHandler( 'remove-overlay', function( event ) {
                return observer.onNext(event);
            } );
        });
        observables.overlayUpdate = Rx.Observable.create(function(observer) {
            viewer.addHandler( 'update-overlay', function( event ) {
                return observer.onNext(event);
            } );
        });
        observables.levelUpdate = Rx.Observable.create(function(observer) {
            viewer.addHandler( 'update-level', function( event ) {
                return observer.onNext(event);
            } );
        });
        observables.redrawRequired = observables.viewerOpen
        .merge(observables.viewerRotate
        		.merge(observables.canvasResize)
        		.debounce(10))
        .map(function(event) {
            var location = {};
            
            if(osViewer.controls) {
                location = osViewer.controls.getLocation();
            }
            
            if(event.osState === "open") {
                location.zoom = osViewer.viewer.viewport.getHomeZoom();
                if(_defaults.image.location) {
                   location = _defaults.image.location;
                }
            }
            
            event.targetLocation = location;
            
            return event;
        }).publish();
        
        return observables;
    }
    
    function _calculateSizes(osViewer) {
        if ( _debug ) {
            console.log( "viewImage: calcualte sizes" );
            console.log("Home zoom = ", osViewer.viewer.viewport.getHomeZoom());
        }
        
        osViewer.sizes = new viewImage.Measures( osViewer );
        
        if ( _defaults.global.adaptContainerHeight ) {
            osViewer.sizes.resizeCanvas();
        }
        
        if ( osViewer.viewer != null ) {
            osViewer.viewer.viewport.setMargins( {bottom: osViewer.sizes.footerHeight + osViewer.sizes.calculateExcessHeight()} );
        }
        
        if ( _debug ) {
            console.log( "sizes: ", osViewer.sizes );
        }
        
    };

    
    function _overlayFooter( event ) {
        if ( _defaults.global.footerHeight > 0 ) {
            var footerHeight = _defaults.global.footerHeight;
            var footerPos = new OpenSeadragon.Point( 0, _container.height() - footerHeight );
            var footerSize = new OpenSeadragon.Point( _container.width(), footerHeight );
            
            if ( !_canvasScale ) {
                _canvasScale = osViewer.viewer.drawer.context.canvas.width / osViewer.viewer.drawer.context.canvas.clientWidth;
            }
            
            if ( _canvasScale != 1 ) {
                footerPos = footerPos.times( _canvasScale );
                footerSize = footerSize.times( _canvasScale );
            }
            osViewer.viewer.drawer.context.drawImage( _footerImage, footerPos.x, footerPos.y, footerSize.x, footerSize.y );
        }
    };
    
    function _timeout(promise, time) {
        var deferred = new jQuery.Deferred();

        $.when(promise).done(deferred.resolve).fail(deferred.reject).progress(deferred.notify);

        setTimeout(function() {
            deferred.reject("timeout");
        }, time);

        return deferred.promise();
    }
    
    return osViewer;    
}

)( jQuery, OpenSeadragon );

// browser backward compability
if(!String.prototype.startsWith) {
    String.prototype.startsWith = function(subString) {
        var start = this.substring(0,subString.length);
        return start.localeCompare(subString) === 0;
    }
}
if(!String.prototype.endsWith) {
    String.prototype.endsWith = function(subString) {
        var start = this.substring(this.length-subString.length,this.length);
        return start.localeCompare(subString) === 0;
    }
}
if(!Array.prototype.find) {
    Array.prototype.find = function(comparator) {
        for ( var int = 0; int < this.length; int++ ) {
            var element = this[int];
            if(comparator(element)) {
                return element;
            }
        }
    }
}
if(!Number.isNaN) {
    Number.isNaN = function(number) {
        return number !== number;
    }
}

var viewImage = ( function( osViewer ) {
    'use strict';
    
    var _debug = false;
    var _currentZoom;
    var _zoomedOut = true;
    var _panning = false;
    var _fadeout = null;
      
    osViewer.controls = {
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'osViewer.controls.init' );
                console.log( '##############################' );
            }
            
            
            if(osViewer.controls.persistence) {
                osViewer.controls.persistence.init(config);
            }
            if(_debug) {                
                console.log("Setting viewer location to", config.image.location);
            }
            
            if( osViewer.observables ) {
                // set location after viewport update
                osViewer.observables.redrawRequired
                .sample(osViewer.observables.viewportUpdate)
                .filter(function(event) {return osViewer.controls ? true : false})
                .subscribe(function(event) {
                    setLocation(event, osViewer)
                    osViewer.controls.setPanning( false );
                });
                
                // zoom home if min zoom reached
                osViewer.observables.viewerZoom.subscribe( function( event ) {
                    if ( _debug ) {
                        console.log( "zoom to " + osViewer.viewer.viewport.getZoom( true ) );
                    }
                    if ( !osViewer.controls.isPanning() ) {
                        var currentZoom = osViewer.viewer.viewport.getZoom();                   
                        if ( currentZoom <= osViewer.viewer.viewport.minZoomLevel ) {
                            if ( _debug ) {
                                console.log( "Zoomed out: Panning home" );
                            }
                            
                            osViewer.controls.setPanning(true);
                            osViewer.controls.goHome( true );
                            osViewer.controls.setPanning(false);
                        }
                    }
                } );
            }
            
            // fade out fullscreen controls
            if ( $( '#fullscreenTemplate' ).length > 0 ) {
                $( '#fullscreenTemplate' ).on( 'mousemove', function() {  
                    osViewer.controls.fullscreenControlsFadeout();
                } );
            }
        },
        getLocation: function() {
            return {
                x: osViewer.controls.getCenter().x,
                y: osViewer.controls.getCenter().y,
                zoom: osViewer.controls.getZoom()/osViewer.controls.getCurrentRotationZooming(),
                rotation: osViewer.controls.getRotation(),
            }
        },
        getCenter: function() {
            if ( _debug ) {
                console.log( "image center is " + osViewer.viewer.viewport.getCenter( true ) );
            }
            return osViewer.viewer.viewport.getCenter( true );
        },
        setCenter: function( center ) {
            
            if ( _debug ) {
                console.log( "Setting image center to " );
                console.log( center );
            }
            
            osViewer.viewer.viewport.panTo( center, true );
            
        },
        getZoom: function() {
            if ( _debug ) {
                console.log( 'osViewer.controls.getZoom' );
            }
            return osViewer.viewer.viewport.getZoom( true );
        },
        zoomTo: function( zoomTo ) {
            if ( _debug ) {
                console.log( 'osViewer.controls.myZoomTo: zoomTo - ' + zoomTo );
            }
            
            var zoomBy = parseFloat( zoomTo ) / osViewer.viewer.viewport.getZoom();
            
            if ( _debug ) {
                console.log( 'osViewer.controls.myZoomTo: zoomBy - ' + zoomBy );
            }
            
            osViewer.viewer.viewport.zoomBy( zoomBy, osViewer.viewer.viewport.getCenter( false ), true );
        },
        setFullScreen: function( enable ) {
            if ( _debug ) {
                console.log( 'osViewer.controls.setFullScreen: enable - ' + enable );
            }
            
            osViewer.viewer.setFullScreen( enable );
        },
        goHome: function( immediate ) {
            if ( _debug ) {
                console.log( 'osViewer.controls.panHome - zoom : ' + osViewer.viewer.viewport.getHomeZoom() );
            }
            osViewer.viewer.viewport.goHome( immediate );
            _zoomedOut = true;
        },
        reset: function( resetRotation ) {
            if ( _debug ) {
                console.log( 'osViewer.controls.goHome: bool - ' + resetRotation );
            }
            
            // osViewer.viewer.viewport.goHome( true );
            osViewer.controls.goHome( true );
            osViewer.viewer.viewport.zoomTo( osViewer.viewer.viewport.getHomeZoom(), null, true );
            if ( resetRotation ) {
                osViewer.controls.rotateTo( 0 );
            }
        },
        zoomIn: function() {
            if ( _debug ) {
                console.log( 'osViewer.controls.zoomIn: zoomSpeed - ' + osViewer.getConfig().global.zoomSpeed );
            }
            
            osViewer.viewer.viewport.zoomBy( osViewer.getConfig().global.zoomSpeed, osViewer.viewer.viewport.getCenter( false ), false );
        },
        zoomOut: function() {
            if ( _debug ) {
                console.log( 'osViewer.controls.zoomOut: zoomSpeed - ' + osViewer.getConfig().global.zoomSpeed );
            }
            
            osViewer.viewer.viewport.zoomBy( 1 / osViewer.getConfig().global.zoomSpeed, osViewer.viewer.viewport.getCenter( false ), false );
        },
        getHomeZoom: function( rotated ) {
            if ( rotated && osViewer.getCanvasSize().x / osViewer.getCanvasSize().y <= osViewer.getImageSize().x / osViewer.getImageSize().y ) {
                osViewer.viewer.viewport.homeFillsViewer = true;
            }
            var zoom = osViewer.viewer.viewport.getHomeZoom();
            osViewer.viewer.viewport.homeFillsViewer = false;
            return zoom;
        },
        rotateRight: function() {
            if ( _debug ) {
                console.log( 'osViewer.controls.rotateRight' );
            }
            
            var newRotation = osViewer.viewer.viewport.getRotation() + 90;
            osViewer.controls.rotateTo( newRotation );
        },
        rotateLeft: function() {
            if ( _debug ) {
                console.log( 'osViewer.controls.rotateLeft' );
            }
            
            var newRotation = osViewer.viewer.viewport.getRotation() - 90;
            osViewer.controls.rotateTo( newRotation );
        },
        getRotation: function() {
            if ( _debug ) {
                console.log( 'osViewer.controls.getRotation' );
            }
            
            return osViewer.viewer.viewport.getRotation();
        },
        setRotation: function( rotation ) {
            if ( _debug ) {
                console.log( 'osViewer.controls.setRotation: rotation - ' + rotation );
            }
            
            return osViewer.controls.rotateTo( rotation );
        },
        rotateTo: function( newRotation ) {
            if ( newRotation < 0 ) {
                newRotation = newRotation + 360;
            }
            newRotation = newRotation % 360;
            if ( _debug ) {
                console.log( 'osViewer.controls.rotateTo: newRotation - ' + newRotation );
            }
                        
            _panning = true;        
            _currentZoom = null;
            osViewer.viewer.viewport.setRotation( newRotation );
            _panning = false;

        },
        getCurrentRotationZooming: function() {
            var sizes = osViewer.getSizes();
            if(sizes && sizes.rotated()) {
                return 1/sizes.ratio(sizes.originalImageSize);
            } else {
                return 1;
            }
        },
        setPanning: function(panning) {
            _panning = panning;
        },
        isPanning: function() {
            return _panning;
        },
        fullscreenControlsFadeout: function() {
            if ( _debug ) {
                console.log( '---------- osViewer.controls.fullscreenControlsFadeout() ----------' );
            }
            
            if ( _fadeout ) {
                clearTimeout( _fadeout );
                this.showFullscreenControls();
            }
            
            _fadeout = setTimeout( this.hideFullscreenControls, 3000 );
        },
        hideFullscreenControls: function() {
            if ( _debug ) {
                console.log( '---------- osViewer.controls.hideFullscreenControls() ----------' );
            }
            
            $( '#fullscreenRotateControlsWrapper, #fullscreenZoomSliderWrapper, #fullscreenExitWrapper, #fullscreenPrevWrapper, #fullscreenNextWrapper' ).stop().fadeOut( 'slow' );
        },
        showFullscreenControls: function() {
            if ( _debug ) {
                console.log( '---------- osViewer.controls.showFullscreenControls() ----------' );
            }
            
            $( '#fullscreenRotateControlsWrapper, #fullscreenZoomSliderWrapper, #fullscreenExitWrapper, #fullscreenPrevWrapper, #fullscreenNextWrapper' ).show();
        }
    };
    
    
    // set correct location, zooming and rotation once viewport has been updated after
    // redraw
    function setLocation(event, osViewer) {
        if(_debug) {                    
            console.log("Viewer changed from " + event.osState + " event");
            console.log("target location: ", event.targetLocation);
            console.log("Home zoom = ", osViewer.viewer.viewport.getHomeZoom());
        }
         osViewer.viewer.viewport.minZoomLevel = osViewer.viewer.viewport.getHomeZoom() * osViewer.getConfig().global.minZoomLevel;
         var targetZoom = event.targetLocation.zoom;
         var targetLocation = new OpenSeadragon.Point(event.targetLocation.x, event.targetLocation.y);
         var zoomDiff = targetZoom * osViewer.viewer.viewport.getHomeZoom() - (osViewer.viewer.viewport.minZoomLevel);
// console.log("zoomDiff: " + targetZoom + " * " + osViewer.viewer.viewport.getHomeZoom()
// + " - " + osViewer.viewer.viewport.minZoomLevel + " = ", zoomDiff);
// console.log("zoomDiff: " + targetZoom + " - " + osViewer.viewer.viewport.minZoomLevel +
// "/" + osViewer.controls.getCurrentRotationZooming() + " = ", zoomDiff);
         var zoomedOut = zoomDiff < 0.001 || !targetZoom;
         if(zoomedOut) {
             if(_debug) {                         
                 console.log("Zooming home")
             }
             osViewer.controls.goHome( true );
         } else {
             if(_debug) {                         
                 console.log( "Zooming to " + targetZoom + " * " + osViewer.controls.getCurrentRotationZooming() );
                 console.log("panning to ", targetLocation);
             }
             osViewer.viewer.viewport.zoomTo( targetZoom * osViewer.controls.getCurrentRotationZooming(), null, true);
             osViewer.controls.setCenter( targetLocation);
         }
         if(event.osState === "open" && event.targetLocation.rotation !== 0) {
            osViewer.controls.rotateTo(event.targetLocation.rotation);
         }
    }
    
    return osViewer;
    
} )( viewImage || {}, jQuery );

var viewImage = ( function( osViewer ) {
    'use strict';
    
    var _debug = false; 
    
    osViewer.controls.persistence = {
        
        init: function( config ) {
            if ( typeof ( Storage ) !== 'undefined' ) {
                
                /**
                 * Set Location from local storage
                 */
                var location = null;
                var currentPersistenceId = osViewer.getConfig().global.persistenceId;
                if ( config.global.persistZoom || config.global.persistRotation ) {
                    try {
                        var location = JSON.parse( localStorage.imageLocation );
                    }
                    catch ( err ) {
                        if ( _debug ) {
                            console.error( "No readable image location in local storage" );
                        }
                    }
                    if ( location && _isValid( location ) && location.persistenceId === currentPersistenceId ) {
                        if ( _debug ) {
                            console.log( "Reading location from local storage", location );
                        }
                        config.image.location = {};
                        if ( config.global.persistZoom ) {
                            if ( _debug ) {
                                console.log( "setting zoom from local storage" );
                            }
                            config.image.location.zoom = location.zoom;
                            config.image.location.x = location.x;
                            config.image.location.y = location.y;
                        }
                        if ( config.global.persistRotation ) {
                            if ( _debug ) {
                                console.log( "setting rotation from local storage" );
                            }
                            config.image.location.rotation = location.rotation;
                        }
                        else {
                            config.image.location.rotation = 0;
                        }
                        
                    }
                    
                    /**
                     * save current location to local storage before navigating away
                     */
                    window.onbeforeunload = function() {
                        var loc = osViewer.controls.getLocation();
                        loc.persistenceId = osViewer.getConfig().global.persistenceId;
                        localStorage.imageLocation = JSON.stringify( loc );
                        if ( _debug ) {
                            console.log( "storing zoom " + localStorage.imageLocation );
                        }
                    }
                }
            }
        }
    }

    function _isValid( location ) {
        return _isNumber( location.x ) && _isNumber( location.y ) && _isNumber( location.zoom ) && _isNumber( location.rotation );
    }
    
    function _isNumber( x ) {
        return typeof x === "number" && !Number.isNaN( x );
    }
    
    return osViewer;
    
} )( viewImage || {}, jQuery );

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

var viewImage = ( function( osViewer ) {
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
        init: function() {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'osViewer.drawRect.init' );
                console.log( '##############################' );
            }
            _viewerInputHook = osViewer.viewer.addViewerInputHook( {
                hooks: [ {
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
                } ]
            } );
        },
        startDrawing: function( overlayGroup, finishHook ) {
            _active = true;
            _overlayGroup = overlayGroup;
            _finishHook = finishHook;
        },
        endDrawing: function( removeLastElement ) {
            _active = false;
            _overlayGroup = null;
            _finishHook = null;
            if ( _drawElement && removeLastElement ) {
                osViewer.viewer.removeOverlay( _drawElement );
            }
            else {
                $( _drawElement ).removeClass( drawingStyleClass );
            }
        },
        isActive: function() {
            return _active;
        },
        isDrawing: function() {
            return _drawing;
        },
        removeLastDrawnElement: function() {
            if ( _drawElement ) {
                osViewer.viewer.removeOverlay( _drawElement );
            }
        },
    }

    function _onViewerPress( event ) {
        if ( _active ) {
            _drawPoint = osViewer.viewer.viewport.viewerElementToViewportCoordinates( event.position );
            
            event.preventDefaultAction = false;
            return true;
        }
    }
    
    function _onViewerDrag( event ) {
        // if(_debug) {
        // console.log("Dragging: ");
        // console.log("_active = " + _active);
        // console.log("_drawing = " + _drawing);
        // console.log("_drawPoint = " + _drawPoint);
        // }
        if ( _drawing ) {
            var newPoint = osViewer.viewer.viewport.viewerElementToViewportCoordinates( event.position );
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
        else if ( _active && _drawPoint ) {
            var activeOverlay = osViewer.overlays.getDrawingOverlay();
            if ( activeOverlay && osViewer.transformRect && osViewer.transformRect.isActive()
                    && osViewer.overlays.contains( activeOverlay.rect, _drawPoint, _minDistanceToExistingRect ) ) {
                _drawPoint = null;
                if ( _debug )
                    console.log( "Action overlaps active overlay" );
            }
            else {
                _drawing = true;
                if ( activeOverlay && _deleteOldDrawElement ) {
                    osViewer.overlays.removeOverlay( activeOverlay );
                }
                
                _drawElement = document.createElement( "div" );
                if ( _overlayGroup ) {
                    $( _drawElement ).addClass( _overlayGroup.styleClass );
                }
                $( _drawElement ).addClass( drawingStyleClass );
                var rect = new OpenSeadragon.Rect( _drawPoint.x, _drawPoint.y, 0, 0 );
                osViewer.viewer.addOverlay( _drawElement, rect, 1 );
            }
            event.preventDefaultAction = true;
            return true;
        }
    }
    
    function _onViewerDragEnd( event ) {
        if ( _drawing ) {
            _drawing = false;
            var newPoint = osViewer.viewer.viewport.viewerElementToViewportCoordinates( event.position );
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
            
            var overlay = {
                type: osViewer.overlays.overlayTypes.RECTANGLE,
                element: _drawElement,
                rect: rect,
                group: _overlayGroup.name,
            };
            osViewer.overlays.setDrawingOverlay( overlay );
            if ( _finishHook ) {
                _finishHook( overlay );
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
    
    return osViewer;
    
} )( viewImage || {}, jQuery );

var viewImage = ( function( osViewer ) {
    'use strict';
    
    var _debug = false;
    
    osViewer.Measures = function( osViewer ) {
        this.config = osViewer.getConfig();
        this.$container = $( "#" + this.config.global.divId );
        
        this.outerCanvasSize = new OpenSeadragon.Point( this.$container.outerWidth(), this.$container.outerHeight() );
        this.innerCanvasSize = new OpenSeadragon.Point( this.$container.width(), this.$container.height() );
        this.originalImageSize = new OpenSeadragon.Point( this.getTotalImageWidth( osViewer.getImageInfo() ), this.getMaxImageHeight( osViewer.getImageInfo() ) );
        // console.log("Original image size = ", this.originalImageSize);
        this.footerHeight = this.config.global.footerHeight;
        this.rotation = osViewer.viewer != null ? osViewer.viewer.viewport.getRotation() : 0;
        this.xPadding = this.outerCanvasSize.x - this.innerCanvasSize.x;
        this.yPadding = this.outerCanvasSize.y - this.innerCanvasSize.y;
        this.innerCanvasSize.y -= this.footerHeight;
        
        // calculate image size as it should be displayed in relation to canvas size
        if ( this.fitToHeight() ) {
            this.imageDisplaySize = new OpenSeadragon.Point( this.innerCanvasSize.y / this.ratio( this.getRotatedSize( this.originalImageSize ) ), this.innerCanvasSize.y )
        }
        else {
            this.imageDisplaySize = new OpenSeadragon.Point( this.innerCanvasSize.x, this.innerCanvasSize.x * this.ratio( this.getRotatedSize( this.originalImageSize ) ) )
        }
        if ( this.rotated() ) {
            this.imageDisplaySize = this.getRotatedSize( this.imageDisplaySize );
        }
    };
    osViewer.Measures.prototype.getMaxImageWidth = function( imageInfo ) {
        var width = 0;
        if ( imageInfo && imageInfo.length > 0 ) {
            for ( var i = 0; i < imageInfo.length; i++ ) {
                var tileSource = imageInfo[ i ];
                if ( tileSource.tileSource ) {
                    correction = tileSource.width;
                    tileSource = tileSource.tileSource;
                }
                width = Math.max( width, tileSource.width * correction );
            }
        }
        return width;
    };
    osViewer.Measures.prototype.getMaxImageHeight = function( imageInfo ) {
        var height = 0;
        if ( imageInfo && imageInfo.length > 0 ) {
            for ( var i = 0; i < imageInfo.length; i++ ) {
                var tileSource = imageInfo[ i ];
                var correction = 1;
                if ( tileSource.tileSource ) {
                    correction = tileSource.width;
                    tileSource = tileSource.tileSource;
                }
                height = Math.max( height, tileSource.height * correction );
            }
        }
        return height;
    };
    osViewer.Measures.prototype.getTotalImageWidth = function( imageInfo ) {
        var width = 0;
        if ( imageInfo && imageInfo.length > 0 ) {
            for ( var i = 0; i < imageInfo.length; i++ ) {
                var tileSource = imageInfo[ i ];
                var correction = 1;
                if ( tileSource.tileSource ) {
                    correction = tileSource.width;
                    tileSource = tileSource.tileSource;
                }
                width += ( tileSource.width * correction );
            }
        }
        return width;
    };
    osViewer.Measures.prototype.getTotalImageHeight = function( imageInfo ) {
        var height = 0;
        if ( imageInfo && imageInfo.length > 0 ) {
            for ( var i = 0; i < imageInfo.length; i++ ) {
                var tileSource = imageInfo[ i ];
                var aspectRatio
                if ( tileSource.tileSource ) {
                    correction = tileSource.width;
                    tileSource = tileSource.tileSource;
                }
                height += tileSource.height * correction;
            }
        }
        return height;
    };
    osViewer.Measures.prototype.getImageHomeSize = function() {
        var ratio = this.rotated() ? 1 / this.ratio( this.originalImageSize ) : this.ratio( this.originalImageSize );
        if ( this.fitToHeight() ) {
            var height = this.innerCanvasSize.y;
            var width = height / ratio;
        }
        else {
            var width = this.innerCanvasSize.x;
            var height = width * ratio;
        }
        return this.getRotatedSize( new OpenSeadragon.Point( width, height ) );
    };
    osViewer.Measures.prototype.rotated = function() {
        return this.rotation % 180 !== 0;
    };
    osViewer.Measures.prototype.landscape = function() {
        return this.ratio( this.originalImageSize ) < 1;
    };
    osViewer.Measures.prototype.ratio = function( size ) {
        return size.y / size.x;
    };
    osViewer.Measures.prototype.getRotatedSize = function( size ) {
        return new OpenSeadragon.Point( this.rotated() ? size.y : size.x, this.rotated() ? size.x : size.y );
    };
    osViewer.Measures.prototype.fitToHeight = function() {
        return !this.config.global.adaptContainerHeight && this.ratio( this.getRotatedSize( this.originalImageSize ) ) > this.ratio( this.innerCanvasSize );
    };
    osViewer.Measures.prototype.resizeCanvas = function() {
        // Set height of container if required
        if ( this.config.global.adaptContainerHeight ) {
            if ( _debug ) {
                console.log( "adapt container height" );
            }
            this.$container.height( this.getRotatedSize( this.imageDisplaySize ).y + this.footerHeight );
        }
        this.outerCanvasSize = new OpenSeadragon.Point( this.$container.outerWidth(), this.$container.outerHeight() );
        this.innerCanvasSize = new OpenSeadragon.Point( this.$container.width(), this.$container.height() - this.footerHeight );
    };
    osViewer.Measures.prototype.calculateExcessHeight = function() {
        var imageSize = this.getRotatedSize( this.getImageHomeSize() );
        var excessHeight = this.config.global.adaptContainerHeight || this.fitToHeight() ? 0 : 0.5 * ( this.innerCanvasSize.y - imageSize.y );
        return excessHeight;
    };
    
    return osViewer;
    
} )( viewImage || {}, jQuery );

var viewImage = ( function( osViewer ) {
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
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'osViewer.overlays.init' );
                console.log( '##############################' );
            }
            
            $.extend( true, _defaults, config );
            
            osViewer.observables.overlayRemove.subscribe(function( event ) {
                if ( event.element ) {
                    $( event.element ).remove();
                }
            });
            if(_defaults.image.highlightCoords) {
               	osViewer.observables.viewerOpen.subscribe( function( data ) {
            		for ( var index=0; index<_defaults.image.highlightCoords.length; index++) {
            			var highlightCoords = _defaults.image.highlightCoords[ index ];
            			var imageIndex = highlightCoords.pageIndex;
            			osViewer.overlays.draw( highlightCoords.name, highlightCoords.displayTooltip, imageIndex);
            		}
            		if ( _initializedCallback ) {
            			_initializedCallback();
            		}
            	} );
            }
        },
        onInitialized: function( callback ) {
            var oldCallback = _initializedCallback;
            _initializedCallback = function() {
                if ( oldCallback ) {
                    oldCallback();
                }
                callback();
            }
        },
        onFocus: function( hook ) {
            var tempHook = _overlayFocusHook;
            _overlayFocusHook = function( overlay, focus ) {
                if ( tempHook )
                    tempHook( overlay, focus );
                hook( overlay, focus );
            }
        },
        onClick: function( hook ) {
            var tempHook = _overlayClickHook;
            _overlayClickHook = function( overlay ) {
                if ( tempHook )
                    tempHook( overlay );
                hook( overlay );
            }
        },
        getOverlays: function() {
            return _overlays.slice();
        },
        getRects: function() {
            return _overlays.filter( function( overlay ) {
                return overlay.type === osViewer.overlays.overlayTypes.RECTANGLE
            } ).slice();
        },
        getLines: function() {
            return _overlays.filter( function( overlay ) {
                return overlay.type === osViewer.overlays.overlayTypes.LINE
            } ).slice();
        },
        draw: function( group, displayTitle, imageIndex ) {
            if ( _debug ) {
                console.log( 'osViewer.overlays.draw: group - ' + group );
                console.log( 'osViewer.overlays.draw: displayTitle - ' + displayTitle );
                console.log( 'osViewer.overlays.draw: imageIndex - ' + imageIndex );
            }
            
            var coordList = _defaults.getCoordinates( group );
            if ( coordList ) {
                for ( var index=0; index<coordList.coordinates.length; index++ ) {
                    var coords = coordList.coordinates[ index ];
                    var title = displayTitle && coords.length > 4 ? coords[ 4 ] : '';
                    var id = coords.length > 5 ? coords[ 5 ] : index;
                    _createRectangle( coords[ 0 ], coords[ 1 ], coords[ 2 ] - coords[ 0 ], coords[ 3 ] - coords[ 1 ], title, id, group, imageIndex );
                }
            }
        },
        unDraw: function( group ) {
            if ( _debug ) {
                console.log( 'osViewer.overlays.unDraw: group - ' + group );
            }
            
            var newRects = [];
            _overlays = _overlays.filter( function( overlay ) {
                if ( overlay.group === group ) {
                    osViewer.viewer.removeOverlay( overlay.element );
                    return false;
                }
                else {
                    return true;
                }
            } );
        },
        highlight: function( group ) {
            if ( _debug ) {
                console.log( 'osViewer.overlays.highlight: group - ' + group );
            }
            
            _overlays.filter( function( overlay ) {
                return overlay.group === group;
            } ).forEach( function( overlay ) {
                if ( overlay.element ) {
                    overlay.element.highlight( true );
                }
            } );
            
        },
        unHighlight: function( group ) {
            if ( _debug ) {
                console.log( 'osViewer.overlays.unHighlight: group - ' + group );
            }
            
            _overlays.filter( function( overlay ) {
                return overlay.group === group;
            } ).forEach( function( overlay ) {
                if ( overlay.element ) {
                    overlay.element.highlight( false );
                }
            } );
            
        },
        focusBox: function( group, id ) {
            if ( _debug ) {
            	console.log( 'osViewer.overlays.highlightBox: group - ' + group );
            	console.log( 'osViewer.overlays.highlightBox: id - ' + id );
            }
            _overlays.filter( function( overlay ) {
                return overlay.group === group;
            } ).forEach( function( overlay ) {
                if ( overlay.element ) {
                    overlay.element.focus( overlay.id === id );
                }
            } );
            
        },
        addOverlay: function( overlay ) {
            if ( _debug ) {
                console.log( 'osViewer.overlays.addOverlay: overlay - ' + overlay );
            }
            
            _overlays.push( overlay );
            if ( overlay.element ) {
                osViewer.viewer.updateOverlay( overlay.element, overlay.rect, 0 );
            }
        },
        removeOverlay: function( overlay ) {
            if ( overlay ) {
                if ( _debug )
                    console.log( "Removing overlay " + overlay.id );
                var index = _overlays.indexOf( overlay );
                _overlays.splice( index, 1 );
                if ( overlay.element ) {
                    osViewer.viewer.removeOverlay( overlay.element );
                }
            }
        },
        drawRect: function( rectangle, group, title, id ) {
            if ( _debug ) {
                console.log( 'osViewer.overlays.drawRect: rectangle - ' + rectangle );
                console.log( 'osViewer.overlays.drawRect: group - ' + group );
            }
            
            _createRectangle( rectangle.x, rectangle.y, rectangle.width, rectangle.height, title ? title : "", id ? id : "", group );
        },
        drawLine: function( point1, point2, group ) {
            if ( _debug ) {
                console.log( 'osViewer.overlays.drawLine: point1 - ' + point1 );
                console.log( 'osViewer.overlays.drawLine: point2 - ' + point2 );
                console.log( 'osViewer.overlays.drawLine: group - ' + group );
            }
            
            _createLine( point1.x, point1.y, point2.x, point2.y, "", "", group );
        },
        showOverlay: function( overlay ) {
            if ( overlay && !overlay.element ) {
                _drawOverlay( overlay );
                if ( _overlayFocusHook ) {
                    _overlayFocusHook( overlay, true );
                }
            }
            
        },
        hideOverlay: function( overlay ) {
            if ( overlay && overlay.element && _drawingOverlay != overlay ) {
                _undrawOverlay( overlay );
                if ( _overlayFocusHook ) {
                    _overlayFocusHook( overlay, false );
                }
            }
        },
        getOverlay: function( id, group ) {
            var overlay =  _overlays.find( function( overlay ) {
                if ( group ) {
                    return overlay.id === id && overlay.group === group;
                }
                else {
                    return overlay.id === id
                }
            } );
// console.log("search for overlay with id = " + id);
// console.log("Found overlay ", overlay);
            return overlay;
        },
        getCoordinates: function( overlay ) {
            if(_debug){
                console.log("getCoordinates - overlay", overlay);
            }
            if ( overlay.type === osViewer.overlays.overlayTypes.RECTANGLE ) {
                var transformedRect = osViewer.viewer.viewport.viewportToImageRectangle( overlay.rect );
                transformedRect = transformedRect.times( osViewer.getScaleToOriginalSize() );
                return transformedRect;
            }
            else if ( overlay.type === osViewer.overlays.overlayTypes.LINE ) {
                var p1 = osViewer.viewer.viewport.viewportToImageCoordinates( overlay.poin1 );
                var p2 = osViewer.viewer.viewport.viewportToImageCoordinates( overlay.poin2 );
                return {
                    point1: p1,
                    point2: p2
                };
            }
        },
        getDrawingOverlay: function() {
            return _drawingOverlay;
        },
        setDrawingOverlay: function( overlay ) {
            _drawingOverlay = overlay;
        },
        showHiddenOverlays: function() {
            osViewer.viewer.addViewerInputHook( {
                hooks: [ {
                    tracker: "viewer",
                    handler: "moveHandler",
                    hookHandler: _onViewerMove
                } ]
            } );
        },
        contains: function( rect, point, precision ) {
            if ( precision == null ) {
                precision = 0;
            }
            return _isInside( rect, point, precision );
        },
        overlayTypes: {
            RECTANGLE: "rectangle",
            LINE: "line"
        },
        getRotated: function(point) {
// var rotation = osViewer.viewer.viewport.getRotation();
// var center = osViewer.viewer.viewport.getCenter( true );
// point = point.rotate(-rotation, center);
            return point;
        }
    };
    
    function _createLine( x1, y1, x2, y2, title, id, group ) {
        if ( _debug ) {
            console.log( '------------------------------' );
            console.log( 'Overlays _createLine: x1 - ' + x1 );
            console.log( 'Overlays _createLine: y1 - ' + y1 );
            console.log( 'Overlays _createLine: x2 - ' + x2 );
            console.log( 'Overlays _createLine: y2 - ' + y2 );
            console.log( 'Overlays _createLine: title - ' + title );
            console.log( 'Overlays _createLine: id - ' + id );
            console.log( 'Overlays _createLine: group - ' + group );
            console.log( '------------------------------' );
        }
        x1 = osViewer.scaleToImageSize( x1 );
        y1 = osViewer.scaleToImageSize( y1 );
        x2 = osViewer.scaleToImageSize( x2 );
        y2 = osViewer.scaleToImageSize( y2 );
        
        var p1 = new OpenSeadragon.Point( x1, y1 );
        var p2 = new OpenSeadragon.Point( x2, y2 );
        var length = p1.distanceTo( p2 );
        
        var angle = _calculateAngle( p1, p2 );
        var beta = ( 180 - angle ) / 2;
// console.log( "drawing line with length = " + length + " and angle = " + angle );
        
        y1 += length / 2 * Math.sin( angle * Math.PI / 180 );
        x1 -= length / 2 * Math.sin( angle * Math.PI / 180 ) / Math.tan( beta * Math.PI / 180 );
 
        var rectangle = osViewer.viewer.viewport.imageToViewportRectangle( x1, y1, length, 1 );
        var p1Viewer = osViewer.viewer.viewport.imageToViewportCoordinates( p1 );
        var p2Viewer = osViewer.viewer.viewport.imageToViewportCoordinates( p2 );
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
        var overlayStyle = _defaults.getOverlayGroup( overlay.group );
        if ( !overlayStyle.hidden ) {
            _drawOverlay( overlay );
        }
        _overlays.push( overlay );
        
    }
    
    /**
     * coordinates are in original image space
     */
    function _createRectangle( x, y, width, height, title, id, group, imageIndex ) {
        if ( _debug ) {
            console.log( '------------------------------' );
            console.log( 'Overlays _createRectangle: x - ' + x );
            console.log( 'Overlays _createRectangle: y - ' + y );
            console.log( 'Overlays _createRectangle: width - ' + width );
            console.log( 'Overlays _createRectangle: height - ' + height );
            console.log( 'Overlays _createRectangle: title - ' + title );
            console.log( 'Overlays _createRectangle: id - ' + id );
            console.log( 'Overlays _createRectangle: group - ' + group );
            console.log( 'Overlays _createRectangle: imageIndex - ' + imageIndex );
            console.log( '------------------------------' );
        }
        x = osViewer.scaleToImageSize( x );
        y = osViewer.scaleToImageSize( y );
        width = osViewer.scaleToImageSize( width );
        height = osViewer.scaleToImageSize( height );
        
        if(!imageIndex) {
        	imageIndex = 0;
        }
			var tiledImage = osViewer.viewer.world.getItemAt(imageIndex);
			var rectangle = tiledImage.imageToViewportRectangle( x, y, width, height );
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
			var overlayStyle = _defaults.getOverlayGroup( overlay.group );
			if ( !overlayStyle.hidden ) {
				_drawOverlay( overlay );
			}
			_overlays.push( overlay );

        
        
    }
    
    function _undrawOverlay( overlay ) {
        osViewer.viewer.removeOverlay( overlay.element );
        overlay.element = null;
    }
    
    function _drawOverlay( overlay ) {
        if(_debug) {
            console.log("viewImage.overlays._drawOverlay");
            console.log("overlay: ", overlay);
        }
        var element = document.createElement( "div" );
        $(element).attr("id", "overlay_" + overlay.id)
        var overlayStyle = _defaults.getOverlayGroup( overlay.group );
        if ( overlayStyle ) {
            if(_debug)console.log("overlay style", overlayStyle);
// element.title = overlay.title;
// $( element ).attr( "data-toggle", "tooltip" );
// $( element ).attr( "data-placement", "auto top" );
            $( element ).addClass( overlayStyle.styleClass );
            
            if ( overlay.type === osViewer.overlays.overlayTypes.LINE ) {
                _rotate( overlay.angle, element );
            }
            
            if ( overlayStyle.interactive ) {
                element.focus = function( focus ) {
                    if ( focus ) {
                        $( element ).addClass( _focusStyleClass );
                        _createTooltip(element, overlay);
                        
// tooltip.height(100);
// $( element ).tooltip( "show" );
                    }
                    else {
                        $( element ).removeClass( _focusStyleClass );
                        $(".tooltipp#tooltip_" + overlay.id).remove();
                    }
                    if ( _overlayFocusHook ) {
                        _overlayFocusHook( overlay, focus );
                    }
                };
                
                element.highlight = function( focus ) {
                    if ( focus ) {
                        $( element ).addClass( _highlightStyleClass );
                    }
                    else {
                        $( element ).removeClass( _highlightStyleClass );
                    }
                };
                
                $( element ).on( "mouseover", function() {
                    if ( _debug ) {
                        console.log( 'Overlays _drawOverlay: mouse over - ' + overlayStyle.name );
                    }
                    osViewer.overlays.focusBox( overlay.group, overlay.id );
                } );
                $( element ).on( "mouseout", function() {
                    if ( _debug ) {
                        console.log( 'Overlays _drawOverlay: mouse out - ' + overlayStyle.name );
                    }
                    element.focus( false );
                } );
                $( element ).on( "click", function() {
                    if ( _overlayClickHook ) {
                        _overlayClickHook( overlay );
                    }
                } );
            }
            overlay.element = element;
            osViewer.viewer.addOverlay( element, overlay.rect, 0 );
        }
    }
    
    function _createTooltip(element, overlay) {
    	if(overlay.title) {    		
    		var canvasCorner = osViewer.sizes.$container.offset();
    		
    		var top = $( element ).offset().top;
    		var left = $( element ).offset().left;
    		var bottom = top + $( element ).outerHeight();
    		var right = left + $( element ).outerWidth();
// console.log("Tooltip at ", left, top, right, bottom);

    		
    		var $tooltip = $("<div class='tooltipp'>" + overlay.title + "</div>");
    		$("body").append($tooltip);
    		var tooltipPadding = parseFloat($tooltip.css("padding-top"));
    		$tooltip.css("max-width",right-left);
    		$tooltip.css("top", Math.max(canvasCorner.top + tooltipPadding, top-$tooltip.outerHeight()-tooltipPadding));
    		$tooltip.css("left", Math.max(canvasCorner.left + tooltipPadding, left));
    		$tooltip.attr("id", "tooltip_" + overlay.id);
// console.log("tooltip width = ", $tooltip.width());
    		
    		// listener for zoom
    		
    		osViewer.observables.animation
    		.do(function() {
// console.log("element at: ", $(element).offset());
    			var top = Math.max($( element ).offset().top, canvasCorner.top);
        		var left = Math.max($( element ).offset().left, canvasCorner.left);
    			$tooltip.css("top", Math.max(canvasCorner.top + tooltipPadding, top-$tooltip.outerHeight()-tooltipPadding));
    			$tooltip.css("left", Math.max(canvasCorner.left + tooltipPadding, left));
    		})
    		.takeWhile(function() {
    			return $(".tooltipp").length > 0;
    		})
    		.subscribe();
    	}
    }
    
    function _rotate( angle, mapElement ) {
        if ( _debug ) {
            console.log( 'Overlays _rotate: angle - ' + angle );
            console.log( 'Overlays _rotate: mapElement - ' + mapElement );
        }
        
        if ( angle !== 0 ) {
            $( mapElement ).css( "-moz-transform", "rotate(" + angle + "deg)" );
            $( mapElement ).css( "-webkit-transform", "rotate(" + angle + "deg)" );
            $( mapElement ).css( "-ms-transform", "rotate(" + angle + "deg)" );
            $( mapElement ).css( "-o-transform", "rotate(" + angle + "deg)" );
            $( mapElement ).css( "transform", "rotate(" + angle + "deg)" );
            var sin = Math.sin( angle );
            var cos = Math.cos( angle );
            $( mapElement ).css(
                    "filter",
                    "progid:DXImageTransform.Microsoft.Matrix(M11=" + cos + ", M12=" + sin + ", M21=-" + sin + ", M22=" + cos
                            + ", sizingMethod='auto expand'" );
        }
    }
    
    function _calculateAngle( p1, p2 ) {
        if ( _debug ) {
            console.log( 'Overlays _calculateAngle: p1 - ' + p1 );
            console.log( 'Overlays _calculateAngle: p2 - ' + p2 );
        }
        
        var dx = p2.x - p1.x;
        var dy = p2.y - p1.y;
        var radians = null;
        
        if ( dx > 0 ) {
            radians = Math.atan( dy / dx );
            return radians * 180 / Math.PI;
        }
        else if ( dx < 0 ) {
            radians = Math.atan( dy / dx );
            return radians * 180 / Math.PI + 180;
        }
        else if ( dy < 0 ) {
            return 270;
        }
        else {
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
    
    function _isInside( rect, point, extra ) {
        return point.x > rect.x - extra && point.x < ( rect.x + rect.width + extra ) && point.y > rect.y - extra
                && point.y < ( rect.y + rect.height + extra );
    }
    
    function _onViewerMove( event ) { 
        var position = event.position;
        var ieVersion = viewerJS.helper.detectIEVersion();
        if(ieVersion && ieVersion === 10) {
// console.log("Correct position for ie ", ieVersion);
            position.x += $(window).scrollLeft();
            position.y += $(window).scrollTop();
        }
// console.log( "viewer move ", position);
        var point = osViewer.viewer.viewport.viewerElementToViewportCoordinates( position );
        _overlays.forEach( function( o ) {
            if ( _isInside( o.rect, point, 0 ) ) {
                osViewer.overlays.showOverlay( o );
            }
            else {
                osViewer.overlays.hideOverlay( o );
            }
        } );
    }
    
    return osViewer;
    
} )( viewImage || {}, jQuery );

var viewImage = ( function( osViewer ) {
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
        msg: {},
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
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'osViewer.readingMode.init' );
                console.log( '##############################' );
                console.log( 'osViewer.readingMode.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );
            
            // check local storage
            _localStoragePossible = viewerJS.helper.checkLocalStorage();
            
            if ( _localStoragePossible ) {
                _defaults.sidebarStatus = localStorage.getItem( 'sidebarStatus' );
                
                if ( _defaults.sidebarStatus === '' || _defaults.sidebarStatus === undefined ) {
                    localStorage.setItem( 'sidebarStatus', 'true' );
                }
                
                // set viewport
                _setViewportHeight();
                if ( _defaults.useTabs ) {
                    _setSidebarTabHeight();
                }
                _setSidebarButtonPosition();
                _checkSidebarStatus();
                setTimeout( function() {
                    _showContent();
                }, 500 );
                
                // save panel status
                if ( _defaults.useAccordeon ) {
                    _activePanel = localStorage.getItem( 'activePanel' );
                    
                    $( '.panel-collapse' ).each( function() {
                        $( this ).removeClass( 'in' );
                    } );
                    
                    if ( _activePanel === null ) {
                        localStorage.setItem( 'activePanel', '#collapseOne' );
                        _activePanel = localStorage.getItem( 'activePanel' );
                        
                        $( _activePanel ).addClass( 'in' );
                    }
                    else {
                        $( _activePanel ).addClass( 'in' );
                    }
                    
                    // click panel event
                    $( 'a[data-toggle="collapse"]' ).on( 'click', function() {
                        var currPanel = $( this ).attr( 'href' );
                        
                        localStorage.setItem( 'activePanel', currPanel );
                    } );
                }
                
                // events
                $( '[data-toggle="sidebar"]' ).on( 'click', function() {
                    $( this ).toggleClass( 'in' );
                    $( this ).parents( '.reading-mode__content-sidebar' ).toggleClass( 'in' );
                    $( this ).parents( '.reading-mode__content-sidebar' ).prev().toggleClass( 'in' );
                    
                    // set sidebar status to local storage
                    _defaults.sidebarStatus = localStorage.getItem( 'sidebarStatus' );
                    
                    if ( _defaults.sidebarStatus === 'false' ) {
                        localStorage.setItem( 'sidebarStatus', 'true' );
                    }
                    else {
                        localStorage.setItem( 'sidebarStatus', 'false' );
                    }
                } );
                
                $( window ).on( 'resize', function() {
                    _setViewportHeight();
                    if ( _defaults.useTabs ) {
                        _setSidebarTabHeight();
                    }
                    _setSidebarButtonPosition();
                } );
                
                $( window ).on( "orientationchange", function() {
                    _setViewportHeight();
                    if ( _defaults.useTabs ) {
                        _setSidebarTabHeight();
                    }
                    _setSidebarButtonPosition();
                } );
                
                // AJAX Loader Eventlistener
                if ( typeof jsf !== 'undefined' ) {
                    jsf.ajax.addOnEvent( function( data ) {
                        var ajaxstatus = data.status;
                        
                        switch ( ajaxstatus ) {
                            case "success":
                                _setViewportHeight();
                                if ( _defaults.useTabs ) {
                                    _setSidebarTabHeight();
                                }
                                _setSidebarButtonPosition();
                                break;
                        }
                    } );
                }
            }
            else {
                return false;
            }
        },
    };
    
    /**
     * Method which sets the height of the viewport elements.
     * 
     * @method _setViewportHeight
     */
    function _setViewportHeight() {
        if ( _debug ) {
            console.log( '---------- _setViewportHeight() ----------' );
            console.log( '_setViewportHeight: view = ', _defaults.viewSelector );
            console.log( '_setViewportHeight: image = ', _defaults.imageSelector );
            console.log( '_setViewportHeight: sidebar = ', _defaults.sidebarSelector );
            console.log( '_setViewportHeight: sidebarInner = ', _defaults.sidebarInnerSelector );
            console.log( '_setViewportHeight: sidebarTabs = ', _defaults.sidebarTabsSelector );
        }
        
        var viewportHeight = $( window ).outerHeight();
        var navHeight = $( _defaults.navSelector ).outerHeight();
        var newHeight = viewportHeight - navHeight;
        
        if ( _debug ) {
            console.log( '_setViewportHeight: viewportHeight = ', viewportHeight );
            console.log( '_setViewportHeight: navHeight = ', navHeight );
            console.log( '_setViewportHeight: newHeight = ', newHeight );
        }
        
        $( _defaults.viewSelector ).css( 'height', newHeight );
        $( _defaults.imageSelector ).css( 'height', newHeight );
        $( _defaults.sidebarSelector ).css( 'height', newHeight );
        $( _defaults.sidebarInnerSelector ).css( 'height', newHeight );
        
    }
    
    /**
     * Method which sets the height of the sidebar Tabs.
     * 
     * @method _setSidebarTabHeight
     */
    function _setSidebarTabHeight() {
        if ( _debug ) {
            console.log( '---------- _setSidebarTabHeight() ----------' );
            console.log( '_setSidebarTabHeight: sidebarTabs = ', _defaults.sidebarTabsSelector );
        }
        
        var viewportHeight = $( window ).outerHeight();
        var navHeight = $( _defaults.navSelector ).outerHeight();
        var newHeight = viewportHeight - navHeight;
        var tabPos = $( _defaults.sidebarTabsSelector ).position();
        var tabHeight = newHeight - tabPos.top - 15;
        var navTabsHeight = $( '.nav-tabs' ).outerHeight();
        
        if ( _debug ) {
            console.log( '_setSidebarTabHeight: tabPos = ', tabPos );
            console.log( '_setSidebarTabHeight: tabHeight = ', tabHeight );
        }
        
        if ( viewportHeight > 768 ) {
            $( _defaults.sidebarTabsSelector ).css( 'height', tabHeight );
            $( _defaults.sidebarTabContentSelector ).css( 'height', tabHeight - navTabsHeight );
            $( _defaults.sidebarTocWrapperSelector ).css( 'min-height', tabHeight - navTabsHeight );
        }
    }
    
    /**
     * Method which sets the position of the sidebar toggle button.
     * 
     * @method _setSidebarButtonPosition
     */
    function _setSidebarButtonPosition() {
        if ( _debug ) {
            console.log( '---------- _setSidebarButtonPosition() ----------' );
            console.log( '_setSidebarButtonPosition: view = ', _defaults.viewSelector );
        }
        
        var viewHalfHeight = $( _defaults.viewSelector ).outerHeight() / 2;
        
        if ( _debug ) {
            console.log( '_setSidebarButtonPosition: viewHalfHeight = ', viewHalfHeight );
        }
        
        $( _defaults.sidebarToggleButton ).css( 'top', viewHalfHeight );
        
    }
    
    /**
     * Method which checks the current sidebar status, based on a local storage value.
     * 
     * @method _checkSidebarStatus
     * @returns {Boolean} Returns false if the sidebar is inactive, returns true if the
     * sidebar is active.
     */
    function _checkSidebarStatus() {
        if ( _debug ) {
            console.log( '---------- _checkSidebarStatus() ----------' );
            console.log( '_checkSidebarStatus: sidebarStatus = ', _defaults.sidebarStatus );
        }
        
        if ( _defaults.sidebarStatus === 'false' ) {
            $( '[data-toggle="sidebar"]' ).removeClass( 'in' );
            $( '.reading-mode__content-sidebar' ).removeClass( 'in' ).prev().removeClass( 'in' );
            
            return false;
        }
        else {
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
        if ( _debug ) {
            console.log( '---------- _showContent() ----------' );
        }
        
        $( _defaults.viewSelector ).removeClass( 'invisible' );
        $( _defaults.sidebarSelector ).removeClass( 'invisible' );
    }
    
    return osViewer;
    
} )( viewImage || {}, jQuery );

var viewImage = ( function( osViewer ) {
    'use strict';
    
    var _debug = false;
    
    osViewer.tileSourceResolver = {
        
        resolveAsJsonOrURI: function( imageInfo ) {
            var deferred = Q.defer();
            if ( this.isJson( imageInfo ) ) {
                deferred.resolve( imageInfo );
            }
            else if ( this.isStringifiedJson( imageInfo ) ) {
                deferred.resolve( JSON.parse( imageInfo ) );
            }
            else {
                deferred.resolve( imageInfo );
            }
            return deferred.promise;
            
        },
        
        resolveAsJson: function( imageInfo ) {
            var deferred = Q.defer();
            if ( this.isURI( imageInfo ) ) {
                if ( this.isJsonURI( imageInfo ) ) {
                    return this.loadJsonFromURL( imageInfo );
                }
                else {
                    deferred.reject( "Url does not lead to a json object" );
                }
            }
            else if ( typeof imageInfo === "string" ) {
                try {
                    var json = JSON.parse( imageInfo );
                    deferred.resolve( json );
                }
                catch ( error ) {
                    deferred.reject( "String does not contain valid json: " + error );
                }
            }
            else if ( typeof imageInfo === "object" ) {
                deferred.resolve( imageInfo );
            }
            else {
                deferred.reject( "Neither a url nor a json object" );
            }
            return deferred.promise;
        },
        
        loadJsonFromURL: function( imageInfo ) {
            var deferred = Q.defer();
            if ( this.isJsonURI( imageInfo ) ) {
                OpenSeadragon.makeAjaxRequest( imageInfo,
                // success
                function( request ) {
                    try {
                        deferred.resolve( JSON.parse( request.responseText ) );
                    }
                    catch ( error ) {
                        deferred.reject( error )
                    }
                },
                // error
                function( error ) {
                    deferred.reject( error );
                } )
            }
            else {
                deferred.reject( "Not a json uri: " + imageInfo );
            }
            return deferred.promise;
        },
        
        loadIfJsonURL: function( imageInfo ) {
            return Q.promise( function( resolve, reject ) {
                if ( osViewer.tileSourceResolver.isURI( imageInfo ) ) {
                    var ajaxParams = {
                        url: decodeURI( imageInfo ),
                        type: "GET",
                        dataType: "JSON",
                        async: true,
                        crossDomain: true,
                        accepts: {
                            application_json: "application/json",
                            application_jsonLd: "application/ld+json",
                            text_json: "text/json",
                            text_jsonLd: "text/ld+json",
                        }
                    }
                    Q( $.ajax( ajaxParams ) ).then( function( data ) {
                        resolve( data );
                    } ).fail( function( error ) {
                        reject( "Failed to retrieve json from " + imageInfo );
                    } );
                    setTimeout( function() {
                        reject( "Timeout after 10s" );
                    }, 10000 )
                }
                else {
                    reject( "Not a uri: " + imageInfo );
                }
            } );
        },
        
        isJsonURI: function( imageInfo ) {
            if ( this.isURI( imageInfo ) ) {
                var shortened = imageInfo.replace( /\?.*/, "" );
                if ( shortened.endsWith( "/" ) ) {
                    shortened = shortened.substring( 0, shortened.length - 1 );
                }
                return shortened.toLowerCase().endsWith( ".json" );
            }
            return false;
        },
        isURI: function( imageInfo ) {
            if ( imageInfo && typeof imageInfo === "string" ) {
                if ( imageInfo.startsWith( "http://" ) || imageInfo.startsWith( "https://" ) || imageInfo.startsWith( "file:/" ) ) {
                    return true;
                }
            }
            return false;
        },
        isStringifiedJson: function( imageInfo ) {
            if ( imageInfo && typeof imageInfo === "string" ) {
                try {
                    var json = JSON.parse( imageInfo );
                    return this.isJson( json );
                }
                catch ( error ) {
                    // no json
                    return false;
                }
            }
            return false;
            
        },
        isJson: function( imageInfo ) {
            return imageInfo && typeof imageInfo === "object";
        },
    
    }

    return osViewer;
    
} )( viewImage || {}, jQuery );

var viewImage = ( function( osViewer ) {
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
        init: function() {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'osViewer.transformRect.init' );
                console.log( '##############################' );
            }
            _viewerInputHook = osViewer.viewer.addViewerInputHook( {
                hooks: [ {
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
                } ]
            } );
        },
        startDrawing: function( overlay, finishHook ) {
            if ( _debug )
                console.log( "Start drawing" );
            osViewer.overlays.setDrawingOverlay( overlay );
            _active = true;
            _group = overlay.group;
            _finishHook = finishHook;
            $( overlay.element ).addClass( _drawingStyleClass );
        },
        endDrawing: function() {
            _drawing = false;
            _group = null;
            _finishHook = null;
            _active = false;
            var drawOverlay = osViewer.overlays.getDrawingOverlay();
            if ( drawOverlay != null ) {
                $( drawOverlay.element ).removeClass( _drawingStyleClass );
                $( drawOverlay.element ).css( {
                    cursor: DEFAULT_CURSOR
                } );
            }
        },
        isActive: function() {
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
            isCorner: function( area ) {
                return area === this.TOPRIGHT || area === this.TOPLEFT || area === this.BOTTOMLEFT || area === this.BOTTOMRIGHT;
            },
            isEdge: function( area ) {
                return area === this.TOP || area === this.BOTTOM || area === this.LEFT || area === this.RIGHT;
            },
            getCursor: function( area ) {
                var rotated = osViewer.viewer.viewport.getRotation() % 180 === 90;
                if ( area === this.TOPLEFT || area === this.BOTTOMRIGHT ) {
                    return rotated ? "nesw-resize" : "nwse-resize";
                }
                else if ( area === this.TOPRIGHT || area === this.BOTTOMLEFT ) {
                    return rotated ? "nwse-resize" : "nesw-resize";
                }
                else if ( area === this.TOP || area === this.BOTTOM ) {
                    return rotated ? "ew-resize" : "ns-resize";
                }
                else if ( area === this.RIGHT || area === this.LEFT ) {
                    return rotated ? "ns-resize" : "ew-resize";
                }
                else if ( area === this.CENTER ) {
                    return "move";
                }
                else {
                    return DEFAULT_CURSOR;
                }
            }
        }
    }

    function _onViewerMove( event ) {
        if ( !_drawing && _active ) {
            var drawPoint = osViewer.viewer.viewport.viewerElementToViewportCoordinates( event.position );
            drawPoint = osViewer.overlays.getRotated( drawPoint );
            var overlayRect = osViewer.overlays.getDrawingOverlay().rect;
            var overlayElement = osViewer.overlays.getDrawingOverlay().element;
            var viewerElement = osViewer.viewer.element;
            var area = _findCorner( overlayRect, drawPoint, _sideClickPrecision );
            if ( !area ) {
                area = _findEdge( overlayRect, drawPoint, _sideClickPrecision );
            }
            if ( !area && osViewer.overlays.contains( overlayRect, drawPoint, 0 ) ) {
                area = osViewer.transformRect.hitAreas.CENTER;
            }
            if ( area ) {
                $( viewerElement ).css( {
                    cursor: osViewer.transformRect.hitAreas.getCursor( area )
                } );
            }
            else {
                $( viewerElement ).css( {
                    cursor: DEFAULT_CURSOR
                } );
            }
            event.preventDefaultAction = true;
            return true;
        }
    }
    
    function _onViewerPress( event ) {
        if ( _active ) {
            if ( !osViewer.overlays.getDrawingOverlay() ) {
                return false;
            }
            var overlayRect = osViewer.overlays.getDrawingOverlay().rect;
            var overlayElement = osViewer.overlays.getDrawingOverlay().element;
            var drawPoint = osViewer.viewer.viewport.viewerElementToViewportCoordinates( event.position );
            drawPoint = osViewer.overlays.getRotated( drawPoint );
            var drawArea = _findCorner( overlayRect, drawPoint, _sideClickPrecision );
            if ( !drawArea ) {
                drawArea = _findEdge( overlayRect, drawPoint, _sideClickPrecision );
            }
            if ( !drawArea && osViewer.overlays.contains( overlayRect, drawPoint, 0 ) ) {
                drawArea = osViewer.transformRect.hitAreas.CENTER;
            }
            if ( _debug )
                console.log( "draw area = " + drawArea );
            if ( drawArea ) {
                $( overlayElement ).tooltip( 'destroy' );
                _enterPoint = drawPoint;
            }
            _drawArea = drawArea;
            event.preventDefaultAction = true;
            return true;
        }
    }
    
    function _onViewerDrag( event ) {
        if ( _drawing ) {
            var newPoint = osViewer.viewer.viewport.viewerElementToViewportCoordinates( event.position );
            newPoint = osViewer.overlays.getRotated( newPoint );
            var rect = osViewer.overlays.getDrawingOverlay().rect;
            var topLeft;
            var bottomRight;
            // if(_debug)console.log("Draw location = " + newPoint);
            if ( _drawArea === osViewer.transformRect.hitAreas.TOPLEFT ) {
                topLeft = new OpenSeadragon.Point( Math.min( newPoint.x, rect.getBottomRight().x ), Math.min( newPoint.y, rect.getBottomRight().y ) );
                bottomRight = rect.getBottomRight();
            }
            else if ( _drawArea === osViewer.transformRect.hitAreas.TOPRIGHT ) {
                topLeft = new OpenSeadragon.Point( rect.getTopLeft().x, Math.min( newPoint.y, rect.getBottomRight().y ) );
                bottomRight = new OpenSeadragon.Point( Math.max( newPoint.x, rect.getTopLeft().x ), rect.getBottomRight().y );
            }
            else if ( _drawArea === osViewer.transformRect.hitAreas.BOTTOMLEFT ) {
                topLeft = new OpenSeadragon.Point( Math.min( newPoint.x, rect.getBottomRight().x ), rect.getTopLeft().y );
                bottomRight = new OpenSeadragon.Point( rect.getBottomRight().x, Math.max( newPoint.y, rect.getTopLeft().y ) );
            }
            else if ( _drawArea === osViewer.transformRect.hitAreas.BOTTOMRIGHT ) {
                topLeft = rect.getTopLeft();
                bottomRight = new OpenSeadragon.Point( Math.max( newPoint.x, rect.getTopLeft().x ), Math.max( newPoint.y, rect.getTopLeft().y ) );
            }
            else if ( _drawArea === osViewer.transformRect.hitAreas.LEFT ) {
                topLeft = new OpenSeadragon.Point( Math.min( newPoint.x, rect.getBottomRight().x ), rect.getTopLeft().y );
                bottomRight = rect.getBottomRight();
            }
            else if ( _drawArea === osViewer.transformRect.hitAreas.RIGHT ) {
                topLeft = rect.getTopLeft();
                bottomRight = new OpenSeadragon.Point( Math.max( newPoint.x, rect.getTopLeft().x ), rect.getBottomRight().y );
            }
            else if ( _drawArea === osViewer.transformRect.hitAreas.TOP ) {
                topLeft = new OpenSeadragon.Point( rect.getTopLeft().x, Math.min( newPoint.y, rect.getBottomRight().y ) );
                bottomRight = rect.getBottomRight();
            }
            else if ( _drawArea === osViewer.transformRect.hitAreas.BOTTOM ) {
                topLeft = rect.getTopLeft();
                bottomRight = new OpenSeadragon.Point( rect.getBottomRight().x, Math.max( newPoint.y, rect.getTopLeft().y ) );
            }
            else if ( _drawArea === osViewer.transformRect.hitAreas.CENTER && _enterPoint ) {
                var dx = _enterPoint.x - newPoint.x;
                var dy = _enterPoint.y - newPoint.y;
                rect.x -= dx;
                rect.y -= dy;
                _enterPoint = newPoint;
            }
            
            if ( topLeft && bottomRight ) {
                // if(_debug)console.log("Upper left point is " + rect.getTopLeft());
                // if(_debug)console.log("Lower right point is " + rect.getBottomRight());
                // if(_debug)console.log("Setting upper left point to " + topLeft);
                // if(_debug)console.log("Setting lower right point to " + bottomRight);
                rect.x = topLeft.x;
                rect.y = topLeft.y;
                rect.width = bottomRight.x - topLeft.x;
                rect.height = bottomRight.y - topLeft.y;
            }
            
            osViewer.viewer.updateOverlay( osViewer.overlays.getDrawingOverlay().element, rect, 0 );
            event.preventDefaultAction = true;
            return true;
        }
        else if ( _drawArea ) {
            _drawing = true;
            event.preventDefaultAction = true;
            return true;
            
        }
    }
    
    function _onViewerRelease( event ) {
        if ( _active ) {
            if ( _drawing && _finishHook ) {
                _finishHook( osViewer.overlays.getDrawingOverlay() );
            }
            _drawing = false;
            if ( osViewer.overlays.getDrawingOverlay() ) {
                $( osViewer.overlays.getDrawingOverlay().element ).tooltip();
            }
            _drawArea = "";
            _enterPoint = null;
            event.preventDefaultAction = true;
            return true;
        }
    }
    
    function _onViewerDragEnd( event ) {
        if ( _drawing ) {
            _drawing = false;
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
    
    /*
     * Determine the side of the rectangle rect the point lies on or closest at <=maxDist
     * distance
     */
    function _findEdge( rect, point, maxDist ) {
        var distanceToLeft = _distToSegment( point, rect.getTopLeft(), rect.getBottomLeft() );
        var distanceToBottom = _distToSegment( point, rect.getBottomLeft(), rect.getBottomRight() );
        var distanceToRight = _distToSegment( point, rect.getTopRight(), rect.getBottomRight() );
        var distanceToTop = _distToSegment( point, rect.getTopLeft(), rect.getTopRight() );
        
        var minDistance = Math.min( distanceToLeft, Math.min( distanceToRight, Math.min( distanceToTop, distanceToBottom ) ) );
        if ( minDistance <= maxDist ) {
            if ( distanceToLeft === minDistance ) {
                return osViewer.transformRect.hitAreas.LEFT;
            }
            if ( distanceToRight === minDistance ) {
                return osViewer.transformRect.hitAreas.RIGHT;
            }
            if ( distanceToTop === minDistance ) {
                return osViewer.transformRect.hitAreas.TOP;
            }
            if ( distanceToBottom === minDistance ) {
                return osViewer.transformRect.hitAreas.BOTTOM;
            }
        }
        return "";
    }
    
    /*
     * Determine the cornder of the rectangle rect the point lies on or closest at
     * <=maxDist distance
     */
    function _findCorner( rect, point, maxDist ) {
        var distanceToTopLeft = _dist( point, rect.getTopLeft() );
        var distanceToBottomLeft = _dist( point, rect.getBottomLeft() );
        var distanceToTopRight = _dist( point, rect.getTopRight() );
        var distanceToBottomRight = _dist( point, rect.getBottomRight() );
        
        var minDistance = Math.min( distanceToTopLeft, Math.min( distanceToTopRight, Math.min( distanceToBottomLeft, distanceToBottomRight ) ) );
        if ( minDistance <= maxDist ) {
            if ( distanceToTopLeft === minDistance ) {
                return osViewer.transformRect.hitAreas.TOPLEFT;
            }
            if ( distanceToTopRight === minDistance ) {
                return osViewer.transformRect.hitAreas.TOPRIGHT;
            }
            if ( distanceToBottomLeft === minDistance ) {
                return osViewer.transformRect.hitAreas.BOTTOMLEFT;
            }
            if ( distanceToBottomRight === minDistance ) {
                return osViewer.transformRect.hitAreas.BOTTOMRIGHT;
            }
        }
        return "";
    }
    
    function _sqr( x ) {
        return x * x
    }
    function _dist2( v, w ) {
        return _sqr( v.x - w.x ) + _sqr( v.y - w.y )
    }
    function _dist( v, w ) {
        return Math.sqrt( _dist2( v, w ) )
    }
    function _distToSegmentSquared( p, v, w ) {
        var l2 = _dist2( v, w );
        if ( l2 == 0 )
            return _dist2( p, v );
        var t = ( ( p.x - v.x ) * ( w.x - v.x ) + ( p.y - v.y ) * ( w.y - v.y ) ) / l2;
        if ( t < 0 )
            return _dist2( p, v );
        if ( t > 1 )
            return _dist2( p, w );
        return _dist2( p, {
            x: v.x + t * ( w.x - v.x ),
            y: v.y + t * ( w.y - v.y )
        } );
    }
    function _distToSegment( point, lineP1, lineP2 ) {
        return Math.sqrt( _distToSegmentSquared( point, lineP1, lineP2 ) );
    }
    return osViewer;
    
} )( viewImage || {}, jQuery );

var viewImage = ( function( osViewer ) {
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
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'osViewer.zoomSlider.init' );
                console.log( '##############################' );
            }
            
            $.extend( true, _defaults, config );
            
            if ( $( _defaults.global.zoomSlider ) ) {
                osViewer.zoomSlider.addZoomSlider( _defaults.global.zoomSlider );
                
                // handler for openSeadragon Object
                osViewer.viewer.addHandler( 'zoom', function( data ) {
                    osViewer.zoomSlider.buttonToZoom( data.zoom );
                } );
            }
        },
        buttonToMouse: function( mousePos ) {
            if ( _debug ) {
                console.log( 'osViewer.zoomSlider.buttonToMouse: mousePos - ' + mousePos );
            }
            
            var offset = _zoomSlider.$button.width() / 2;
            var newPos = mousePos - offset;
            if ( newPos < 0 ) {
                newPos = 0;
            }
            if ( newPos + 2 * offset > _zoomSlider.absoluteWidth ) {
                newPos = _zoomSlider.absoluteWidth - 2 * offset;
            }
            _zoomSlider.$button.css( {
                left: newPos
            } );
            _zoomSlider.buttonPosition = newPos;
            var factor = ( newPos / ( _zoomSlider.absoluteWidth - offset * 2 ) );
            factor = 1 / _defaults.global.sliderDilation * Math.tan( Math.atan( _defaults.global.sliderDilation ) * factor );
            
            var newScale = osViewer.viewer.viewport.getMinZoom() + ( osViewer.viewer.viewport.getMaxZoom() - osViewer.viewer.viewport.getMinZoom() ) * factor;
            
            if ( _debug ) {
                console.log( 'osViewer.zoomSlider.buttonToMouse: newScale - ' + newScale );
            }
            
            osViewer.controls.zoomTo( newScale );
        },
        buttonToZoom: function( scale ) {
            if ( _debug ) {
                console.log( 'osViewer.zoomSlider.buttonToZoom: scale - ' + scale );
            }
            
            if ( !_zoomSlider || !osViewer.viewer.viewport ) {
                return;
            }
            
            // console.log("Dilation = ", osViewer.viewer.viewport.getMinZoom())
            // console.log("minZoom = ", osViewer.viewer.viewport.getMinZoom());
            // console.log("maxZoom = ", osViewer.viewer.viewport.getMaxZoom())
            // console.log("scale = ", scale);
            
            var factor = ( scale - osViewer.viewer.viewport.getMinZoom() ) / ( osViewer.viewer.viewport.getMaxZoom() - osViewer.viewer.viewport.getMinZoom() );
            // console.log( "factor = ", factor );
            //            
            factor = 1 / Math.atan( _defaults.global.sliderDilation ) * Math.atan( _defaults.global.sliderDilation * factor );
            var newPos = factor * ( _zoomSlider.absoluteWidth - _zoomSlider.$button.width() );
            // var newPos = ( ( scale - osViewer.viewer.viewport.getMinZoom() ) / (
            // osViewer.viewer.viewport.getMaxZoom() -
            // osViewer.viewer.viewport.getMinZoom() ) )
            // * ( _zoomSlider.absoluteWidth - _zoomSlider.$button.width() );
            // console.log( "pos = ", newPos );
            
            if ( Math.abs( osViewer.viewer.viewport.getMaxZoom() - scale ) < 0.0000000001 ) {
                newPos = _zoomSlider.absoluteWidth - _zoomSlider.$button.width();
            }
            
            if ( newPos < 0 ) {
                newPos = 0;
            }
            
            _zoomSlider.$button.css( {
                left: newPos
            } );
            _zoomSlider.buttonPosition = newPos;
        },
        zoomSliderMouseUp: function() {
            if ( _debug ) {
                console.log( 'osViewer.zoomSlider.zoomSliderMouseUp' );
            }
            
            _zoomSlider.mousedown = false;
        },
        zoomSliderMouseMove: function( evt ) {
            if ( _debug ) {
                console.log( 'osViewer.zoomSlider.zoomSliderMouseMove: evt - ' + evt );
            }
            
            if ( !_zoomSlider.mousedown ) {
                return;
            }
            var offset = $( this ).offset();
            var hitX = evt.pageX - offset.left;
            osViewer.zoomSlider.buttonToMouse( hitX );
            
            if ( _debug ) {
                console.log( 'osViewer.zoomSlider.zoomSliderMouseMove: moving - ' + hitX );
            }
        },
        zoomSliderMouseDown: function( evt ) {
            if ( _debug ) {
                console.log( 'osViewer.zoomSlider.zoomSliderMouseDown: evt - ' + evt );
            }
            
            _zoomSlider.mousedown = true;
            var offset = $( this ).offset();
            var hitX = evt.pageX - offset.left;
            osViewer.zoomSlider.buttonToMouse( hitX );
        },
        buttonMouseDown: function() {
            if ( _debug ) {
                console.log( 'osViewer.zoomSlider.buttonMouseDown' );
            }
            
            _zoomSlider.mousedown = true;
            
            return false;
        },
        addZoomSlider: function( element ) {
            if ( _debug ) {
                console.log( 'osViewer.zoomSlider.addZoomSlider: element - ' + element );
            }
            
            _zoomSlider.$element = $( element );
            _zoomSlider.$button = _zoomSlider.$element.children( _defaults.global.zoomSliderHandle );
            _zoomSlider.buttonPosition = 0;
            _zoomSlider.absoluteWidth = _zoomSlider.$element.innerWidth();
            _zoomSlider.mousedown = false;
            _zoomSlider.$button.on( 'mousedown', osViewer.zoomSlider.buttonMouseDown );
            _zoomSlider.$element.click( osViewer.zoomSlider._zoomSliderClick );
            _zoomSlider.$element.mousedown( osViewer.zoomSlider.zoomSliderMouseDown );
            _zoomSlider.$element.mousemove( osViewer.zoomSlider.zoomSliderMouseMove );
            $( document ).on( 'mouseup', osViewer.zoomSlider.zoomSliderMouseUp );
        }
    };
    
    return osViewer;
    
} )( viewImage || {}, jQuery );
