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
var Crowdsourcing = ( function(crowdsourcing) {
    'use strict';

    crowdsourcing.Query = function(query, item) {
        let temp = crowdsourcing.deepCopy(query);
        Object.assign(this, temp);
        
        this.item = item;
        this.queryType = Crowdsourcing.Query.getType(this);
        this.targetFrequency = Crowdsourcing.Query.getFrequency(this);
        this.targetSelector = Crowdsourcing.Query.getSelector(this);

        this.annotations = [];
        this.currentAnnotationIndex = -1;
    }
    
    crowdsourcing.Query.prototype.createAnnotation = function(anno) {
        throw "Must implement createAnnotation";
    }
    
    
    crowdsourcing.Query.prototype.initAnnotations = function() {
        switch(this.targetSelector) {
            case Crowdsourcing.Query.Selector.WHOLE_PAGE:
            case Crowdsourcing.Query.Selector.WHOLE_SOURCE:
                if(this.annotations.length == 0) {
                    //create empty annotation
                    let anno = this.createAnnotation({});
                    anno.setTarget(this.getTarget());
                    this.annotations.push(anno);    
                }
                this.currentAnnotationIndex = this.annotations.length - 1;
        }
    }
    
    crowdsourcing.Query.prototype.resetAnnotations = function() {
        this.annotations = this.restoreFromLocalStorage();
        console.log("reset annotations to ", this.annotations);
        this.initAnnotations();
        if(this.areaSelector) {
            this.areaSelector.reset();
            this.annotations.map(anno => {return {id: anno.id,
                                          region: anno.getRegion(), 
                                          color: anno.getColor()
                                         }}).forEach(anno => this.areaSelector.addOverlay(anno, this.item.image.viewer))
        }
    }
    
    crowdsourcing.Query.prototype.initAreaSelector = function() {
        this.areaSelector = new Crowdsourcing.AreaSelector(this.item, true);
        this.areaSelector.init();
    }
    
    crowdsourcing.Query.prototype.getAnnotation = function(id) {
        return this.annotations.find(anno => anno.id == id);
    }
    
    crowdsourcing.Query.prototype.getIndex = function(anno) {
        return this.annotations.indexOf(anno);
    }

    crowdsourcing.Query.prototype.getImage = function(annotation) {
        return this.getImageUrl(annotation.getRegion(), this.item.getImageId(this.item.getCurrentCanvas()));
    }

    crowdsourcing.Query.prototype.setRegion = function(region, anno) {
        if(anno) {
            anno.setRegion(region);
            this.currentAnnotationIndex = this.getIndex(anno);
            this.saveToLocalStorage();
        }
    }
    
    crowdsourcing.Query.prototype.addAnnotation = function(id, region, color) {
        let annotation = this.createAnnotation({});
        annotation.id = id;
        annotation.setTarget(this.getTarget());
        annotation.setRegion(region);
        annotation.setColor(color);
        this.annotations.push(annotation);
        this.currentAnnotationIndex = this.annotations.length - 1;
        this.saveToLocalStorage();
    }
    
    crowdsourcing.Query.prototype.getImageUrl = function(rect, imageId) {
        let url = imageId + "/" + rect.x + "," + rect.y + "," + rect.width + "," + rect.height + "/full/0/default.jpg";
        return url;
    }
    
    crowdsourcing.Query.prototype.getTarget = function() {
        return this.item.getCurrentCanvas();

    }

    crowdsourcing.Query.prototype.deleteAnnotation = function(anno) {
        let index = this.getIndex(anno);
        if(index > -1) {            
            this.annotations.splice(index,1);
            if(this.currentAnnotationIndex >= index) {
                this.currentAnnotationIndex--;
            }
            if(this.areaSelector) {             
                this.areaSelector.removeOverlay(anno, this.item.image.viewer);
            }
            this.saveToLocalStorage();
            this.initAnnotations();
        }
    }
    
    crowdsourcing.Query.prototype.deleteFromLocalStorage = function() {
        let map = this.getAnnotationsFromLocalStorage();
        if(map.has(Crowdsourcing.getResourceId(this.getTarget()))) {
            map.set(Crowdsourcing.getResourceId(this.getTarget()), [] );
        }
        let value = JSON.stringify(Array.from(map.entries()));
        localStorage.setItem("CrowdsourcingQuery_" + this.id, value);
    }
    
    crowdsourcing.Query.prototype.saveToLocalStorage = function() {
        let map = this.getAnnotationsFromLocalStorage();
        map.set(Crowdsourcing.getResourceId(this.getTarget()), this.annotations );
        let value = JSON.stringify(Array.from(map.entries()));
        localStorage.setItem("CrowdsourcingQuery_" + this.id, value);
    }
    
    crowdsourcing.Query.prototype.restoreFromLocalStorage = function() {
        let map = this.getAnnotationsFromLocalStorage();
        let annotations;
        if(map.has(Crowdsourcing.getResourceId(this.getTarget()))) {
            annotations = map.get(Crowdsourcing.getResourceId(this.getTarget())).map( anno => this.createAnnotation(anno));
        } else {
            annotations = [];
        }
        return annotations;
    }
    
    crowdsourcing.Query.prototype.getAnnotationsFromLocalStorage = function() {
        let string = localStorage.getItem("CrowdsourcingQuery_" + this.id);
        try {
            let array = JSON.parse(string);
            if(Array.isArray(array)) {
                return new Map(JSON.parse(string));
            } else {
                return new Map();
            }
        } catch(e) {
            console.log("Error loading json ", e);
            return new Map();
        }
    }
    
    

    crowdsourcing.Query.Type = {};
    crowdsourcing.Query.Frequency = {};
    crowdsourcing.Query.Selector = {};

    
    crowdsourcing.Query.Type.PLAINTEXT = "PLAINTEXT";
    crowdsourcing.Query.Type.DATE_PICKER = "DATE_PICKER";
    crowdsourcing.Query.Type.GEOLOCATION_POINT = "GEOLOCATION_POINT";
    crowdsourcing.Query.Type.GEOLOCATION_AREA = "GEOLOCATION_AREA";
    crowdsourcing.Query.Type.TRANSCRIPTION = "TRANSCRIPTION";
    crowdsourcing.Query.Type.KEY_VALUE_LIST = "KEY_VALUE_LIST";
    crowdsourcing.Query.Type.get = function(text) {
        return crowdsourcing.Query.Type[text];
    }
    crowdsourcing.Query.getType = function(query) {
        return crowdsourcing.Query.Type.get(query.queryType);
    }
    
    crowdsourcing.Query.Frequency.ONE_PER_MANIFEST = "ONE_PER_MANIFEST";
    crowdsourcing.Query.Frequency.MULTIPLE_PER_MANIFEST = "MULTIPLE_PER_MANIFEST";
    crowdsourcing.Query.Frequency.ONE_PER_CANVAS = "ONE_PER_CANVAS";
    crowdsourcing.Query.Frequency.MULTIPLE_PER_CANVAS = "MULTIPLE_PER_CANVAS";
    crowdsourcing.Query.Frequency.get = function(text) {
        return crowdsourcing.Query.Frequency[text];
    }
    crowdsourcing.Query.getFrequency = function(query) {
        return crowdsourcing.Query.Frequency.get(query.targetFrequency);
    }
    
    crowdsourcing.Query.Selector.MULTIPLE_PER_CANVAS = "WHOLE_SOURCE";
    crowdsourcing.Query.Selector.WHOLE_PAGE = "WHOLE_PAGE";
    crowdsourcing.Query.Selector.RECTANGLE = "RECTANGLE";
    crowdsourcing.Query.Selector.get = function(text) {
        return crowdsourcing.Query.Selector[text];
    }
    crowdsourcing.Query.getSelector = function(query) {
        return crowdsourcing.Query.Selector.get(query.targetSelector);
    }    
    
    return crowdsourcing;
    
} )( Crowdsourcing );
