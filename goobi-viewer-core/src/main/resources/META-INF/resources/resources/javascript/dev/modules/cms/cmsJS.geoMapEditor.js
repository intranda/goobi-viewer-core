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
 * Module to initialize the backend functionality for geomaps
 * 
 * @version 4.6.0
 * @module cmsJS.geoMapBackend
 * @requires jQuery
 */
var cmsJS = ( function( cms ) {
    'use strict';
    
    // variables
    var _debug = true;
    var _defaults = {
        mapId: "geomap",
        displayLanguage : "de",
        supportedLanguages: ["de", "en"],
        popover: "#geoMapPopoverTemplate",
        allowEditFeatures: false,
        featuresInput: "#featuresInput",
        viewInput: "#viewInput",
        metadataArea: "#featureForm",
        msg: {
            emptyMarker: "...",
            titleLabel: "title",
            titleHelp: "help title",
            descLabel: "description",
            descHelp: "help description",
            deleteLabel: "delete"
        }
    };
    
    cms.GeoMapEditor = function(config){
        this.config = $.extend({}, _defaults, config);
        this.currentFeature = undefined;
        this.onDeleteClick = new Rx.Subject();
        this.onMetadataUpdate = new Rx.Subject();
        this.metadataProvider = new Rx.Subject();
        
        this.geoMap = new viewerJS.GeoMap({
            language: this.config.displayLanguage,
            popover: this.config.popover,
            emptyMarkerMessage: this.config.msg.emptyMarker,
            allowMovingFeatures: this.config.allowEditFeatures,
        });
        
        this.geoMap.onMapRightclick
        .pipe(RxOp.takeWhile(() => this.config.allowEditFeatures), RxOp.map(geojson => this.addFeature(geojson)))
        .subscribe(() => this.saveFeatures());
        
        this.geoMap.onMapClick
        .pipe(RxOp.takeWhile(() => this.config.allowEditFeatures))
        .subscribe(() => this.setCurrentFeature());
        
        this.geoMap.onFeatureClick
        .pipe(RxOp.takeWhile(() => this.config.allowEditFeatures))
        .subscribe(geojson => this.setCurrentFeature(geojson, true));
        
        this.geoMap.onFeatureMove
        .pipe(RxOp.takeWhile(() => this.config.allowEditFeatures), RxOp.map(geojson => this.setCurrentFeature(geojson)))
        .subscribe(() => this.saveFeatures());
        
        this.geoMap.onMapMove
        .subscribe(() => this.saveView())
        
        this.onDeleteClick
        .pipe(RxOp.takeWhile(() => this.config.allowEditFeatures), RxOp.map(() => this.deleteCurrentFeature()))
        .subscribe(() => this.saveFeatures());
        
        this.onMetadataUpdate
        .pipe(RxOp.takeWhile(() => this.config.allowEditFeatures), RxOp.map((metadata) => this.updateCurrentFeatureMetadata(metadata)))
        .subscribe(() => this.saveFeatures());
    }
    
    cms.GeoMapEditor.prototype.init = function() {
        this.geoMap.init();
        if($("metadataEditor").length > 0) {  
            if(this.metadataEditor) {
                this.metadataEditor.forEach(component => {
                    component.unmount(true);
                })
            }
            this.metadataEditor = riot.mount("metadataEditor", {
                languages: this.config.supportedLanguages,
                metadata: undefined,
                provider: this.metadataProvider,
                currentLanguage: this.config.displayLanguage,
                updateListener: this.onMetadataUpdate,
                deleteListener : this.onDeleteClick,
                deleteLabel : this.config.msg.deleteLabel
            });
        }
    }
    
    cms.GeoMapEditor.prototype.addFeature = function(geojson) {
        this.currentFeature = geojson;
        this.geoMap.addMarker(geojson).openPopup();
        this.updateMetadata(geojson);
    }
    
    cms.GeoMapEditor.prototype.saveFeatures = function() {
        $(this.config.featuresInput).val(JSON.stringify(this.geoMap.getFeatures()));
    }
    
    cms.GeoMapEditor.prototype.saveView = function() {
        $(this.config.viewInput).val(JSON.stringify(this.geoMap.getView()));
    }
    
    /**
     * Updates the content of the metadata editor with the content of the given geojson object
     * Also scrolls to the top of the metadata edtor (config.metadataArea)
     */
    cms.GeoMapEditor.prototype.updateMetadata = function(geojson) {
        let metadataList = [
            {   
                property: "title",
                tyle: "text",
                label: this.config.msg.titleLabel,
                value: geojson ? geojson.properties.title : "",
                required: false,
                helptext: this.config.msg.titleHelp,
                editable: geojson != undefined
            },
            {   
                property: "description",
                type:"longtext",
                label: this.config.msg.descLabel,
                value: geojson ? geojson.properties.description: "",
                required: false,
                helptext: this.config.msg.descHelp,
                editable: geojson != undefined
            }
        ]
        this.metadataProvider.next(metadataList);
        if(geojson && this.config.metadataArea) {
            $("html, body").scrollTop($(this.config.metadataArea).offset().top)
        }
    }
    
    /**
     * Set the current feature to the given feature if it isn't already the current feature or 'deselectIfCurrent' is false
     * Otherwise, set the current feature to undefined, effectively unselecting the given feature
     * Update the metadata editor with the current feature
     */
    cms.GeoMapEditor.prototype.setCurrentFeature = function(geojson, deselectIfCurrent) {
        if(_debug)console.log("set current feature ", geojson);
        if(deselectIfCurrent && this.currentFeature == geojson) {
            this.currentFeature = undefined;
            this.updateMetadata();
        } else {        
            this.currentFeature = geojson;
            this.updateMetadata(geojson);
        }
    }
    
    cms.GeoMapEditor.prototype.deleteCurrentFeature = function() {
        if(this.currentFeature) {
            this.geoMap.removeMarker(this.currentFeature);
            this.currentFeature = undefined;
            this.updateMetadata();
        }
    }
    
    cms.GeoMapEditor.prototype.updateCurrentFeatureMetadata = function(metadata) {
        if(this.currentFeature && metadata) {                                                
            this.currentFeature.properties[metadata.property] = metadata.value;
            this.geoMap.updateMarker(this.currentFeature.id);
        }
    }
    

    
    cms.GeoMapEditor.prototype.updateView = function() {
        let view = $(this.config.viewInput).val();
        this.geoMap.setView(view);
        this.geoMap.setView(this.geoMap.getViewAroundFeatures());
    }
    
    /**
     * Update all map features from the content of the config.featuresInput field
     */
    cms.GeoMapEditor.prototype.updateFeatures = function() {

        let features = JSON.parse($(this.config.featuresInput).val())
        if(_debug)console.log("update features", features);
        features.forEach( feature => {
            this.geoMap.addMarker(feature);
        })
    }
    
    cms.GeoMapEditor.prototype.setAllowEditFeatures = function(allow) {
        this.config.allowEditFeatures = allow;
        this.geoMap.config.allowMovingFeatures = allow;
        this.geoMap.markers.filter(m => m.dragging).forEach(marker => {
            if(allow) {
                marker.dragging.enable();
            } else {
                marker.dragging.disable();
            }
        })
    }

        
       
    return cms;
    
} )( cmsJS || {}, jQuery );