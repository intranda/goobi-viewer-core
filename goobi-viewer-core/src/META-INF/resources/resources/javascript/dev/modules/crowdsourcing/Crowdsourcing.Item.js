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
 * @description Base-Module which initialize the global admin object. * 
 * @version 3.4.0
 * @module Crowdsourcing.js
 * @requires jQuery
 */
var Crowdsourcing = ( function() {
    'use strict';
    
    var _debug = false; 
    var canvases = [];
    var currentCanvasIndex = 0;
    var viewer = null;
    var queries = [];
    
    var crowdsourcing = {};
    
    crowdsourcing.Query.Type.PLAINTEXT = "PLAINTEXT";
    crowdsourcing.Query.Type.DATE_PICKER = "DATE_PICKER";
    crowdsourcing.Query.Type.GEOLOCATION_POINT = "GEOLOCATION_POINT";
    crowdsourcing.Query.Type.GEOLOCATION_AREA = "GEOLOCATION_AREA";
    crowdsourcing.Query.Type.TRANSCRIPTION = "TRANSCRIPTION";
    crowdsourcing.Query.Type.KEY_VALUE_LIST = "KEY_VALUE_LIST";
    crowdsourcing.Query.Type.get = function(text) {
        crowdsourcing.Query.Type[text];
    }
    crowdsourcing.Query.getType(query) {
        return
    }
    
    crowdsourcing.Query.Frequency.ONE_PER_MANIFEST = "ONE_PER_MANIFEST";
    crowdsourcing.Query.Frequency.MULTIPLE_PER_MANIFEST = "MULTIPLE_PER_MANIFEST";
    crowdsourcing.Query.Frequency.ONE_PER_CANVAS = "ONE_PER_CANVAS";
    crowdsourcing.Query.Frequency.MULTIPLE_PER_CANVAS = "MULTIPLE_PER_CANVAS";
    crowdsourcing.Query.Frequency.get = function(text) {
        crowdsourcing.Query.Frequency[text];
    }
    
    crowdsourcing.Query.Selector.MULTIPLE_PER_CANVAS = "WHOLE_SOURCE";
    crowdsourcing.Query.Selector.MULTIPLE_PER_CANVAS = "WHOLE_PAGE";
    crowdsourcing.Query.Selector.MULTIPLE_PER_CANVAS = "RECTANGLE";
    crowdsourcing.Query.Selector.get = function(text) {
        crowdsourcing.Query.Selector[text];
    }
    
    crowdsourcing.Item = function(imageSource, queries, initialCanvasIndex) {
        if ( _debug ) {
            console.log( '##############################' );
            console.log( 'Crowdsourcing.Item' );
            console.log( 'Crowdsourcing.Item.canvases ', canvases );
            console.log( 'Crowdsourcing.Item.queries ', queries );
            console.log( '##############################' );
        }
        this.queries = queries;
        this.currentCanvasIndex = initialCanvasIndex ? initialCanvasIndex : 0;
        this.imageSource = imageSource;
        this.imageOpenEvents = new Rx.Subject();

    };

    crowdsourcing.Item.prototype.notifyImageOpened = function(observable) {
        observable.subscribe(this.imageOpenEvents);
    }
    
    crowdsourcing.Item.prototype.onImageOpen = function(eventHandler, errorHandler, completedHandler) {
        this.imageOpenEvents.subscribe(eventHandler, errorHandler, completedHandler);
    }
    
    crowdsourcing.Item.prototype.initViewer = function() {
        return fetch(this.imageSource)
        .then( (response) => response.json())
        .then( function(json) {
            this.canvases = _getCanvasList(json);
            currentCanvasIndex = Math.max(0, Math.min(currentCanvasIndex, this.canvases.length-1));
        }.bind(this) )
    }
    
    crowdsourcing.Item.prototype.loadImage = function(index) {
        if(index !== undefined) {            
            this.currentCanvasIndex = index;
        }
        if(this.setImageSource) {            
            this.setImageSource(this.getCurrentCanvas());
        }
    }
    
    crowdsourcing.Item.prototype.getCurrentCanvas = function() {
        return this.canvases[this.currentCanvasIndex];
    }
    
    crowdsourcing.Item.prototype.getImageService = (canvas) =>  canvas.images[0].resource.service["@id"] + "/info.json";
    
    crowdsourcing.Item.prototype.getImageId = (canvas) =>  canvas.images[0].resource.service["@id"];
    
    /**
        get a list containing all canvas json items or canvas urls contained in the source object
        The source must be either a manifest, a range or a single canvas
    */

    function _getCanvasList(source) {
        let sourceType = source.type;
        if(!sourceType) {
            sourceType = source["@type"];
        }
        
        switch(sourceType) {
            case "sc:Manifest":
                return source.sequences[0].canvases;
            case "sc:Canvas":
                return [source];
            case "sc:Range":
                return source.canvases;
            default:
                console.log("Unknown source type, cannot retrieve canvases", source);
        }
    }
    
    return crowdsourcing;
    
} )( jQuery );
