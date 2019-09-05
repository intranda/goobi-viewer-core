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

    crowdsourcing.Question = function(question, item) {
        let temp = crowdsourcing.deepCopy(question);
        Object.assign(this, temp);
        
        this.item = item;
        this.questionType = Crowdsourcing.Question.getType(this);
        this.targetFrequency = Crowdsourcing.Question.getFrequency(this);
        this.targetSelector = Crowdsourcing.Question.getSelector(this);

        this.annotations = [];
        this.currentAnnotationIndex = -1;
    }
    
    crowdsourcing.Question.prototype.createAnnotation = function(anno) {
        throw "Must implement createAnnotation";
    }
    
    
    crowdsourcing.Question.prototype.initAnnotations = function() {
        switch(this.targetSelector) {
            case Crowdsourcing.Question.Selector.WHOLE_PAGE:
            case Crowdsourcing.Question.Selector.WHOLE_SOURCE:
                if(this.annotations.length == 0) {
                    //create empty annotation
                    let anno = this.createAnnotation({});
                    anno.setTarget(this.getTarget());
                    this.annotations.push(anno);    
                }
                this.currentAnnotationIndex = this.annotations.length - 1;
        }
    }
    
    crowdsourcing.Question.prototype.resetAnnotations = function() {
        this.annotations = this.restoreFromLocalStorage();
        this.initAnnotations();
        if(this.areaSelector) {
            this.areaSelector.reset();
            this.annotations.map(anno => {return {id: anno.id,
                                          region: anno.getRegion(), 
                                          color: anno.getColor()
                                         }}).forEach(anno => this.areaSelector.addOverlay(anno, this.item.image.viewer))
        }
    }
    
    crowdsourcing.Question.prototype.initAreaSelector = function() {
        this.areaSelector = new Crowdsourcing.AreaSelector(this.item, true);
        this.areaSelector.init();
    }
    
    crowdsourcing.Question.prototype.getAnnotation = function(id) {
        return this.annotations.find(anno => anno.id == id);
    }
    
    crowdsourcing.Question.prototype.getIndex = function(anno) {
        return this.annotations.indexOf(anno);
    }

    crowdsourcing.Question.prototype.getImage = function(annotation) {
        return this.getImageUrl(annotation.getRegion(), this.item.getImageId(this.item.getCurrentCanvas()));
    }

    crowdsourcing.Question.prototype.setRegion = function(region, anno) {
        if(anno) {
            anno.setRegion(region);
            this.currentAnnotationIndex = this.getIndex(anno);
            this.saveToLocalStorage();
        }
    }
    
    crowdsourcing.Question.prototype.addAnnotation = function(id, region, color) {
        let annotation = this.createAnnotation({});
        annotation.id = id;
        annotation.setTarget(this.getTarget());
        annotation.setRegion(region);
        annotation.setColor(color);
        this.annotations.push(annotation);
        this.currentAnnotationIndex = this.annotations.length - 1;
        this.saveToLocalStorage();
    }
    
    crowdsourcing.Question.prototype.getImageUrl = function(rect, imageId) {
        let url = imageId + "/" + rect.x + "," + rect.y + "," + rect.width + "," + rect.height + "/full/0/default.jpg";
        return url;
    }
    
    crowdsourcing.Question.prototype.getTarget = function() {
        return this.item.getCurrentCanvas();

    }

    crowdsourcing.Question.prototype.deleteAnnotation = function(anno) {
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
    
    crowdsourcing.Question.prototype.deleteFromLocalStorage = function() {
        let map = this.getAnnotationsFromLocalStorage();
        if(map.has(Crowdsourcing.getResourceId(this.getTarget()))) {
            map.delete(Crowdsourcing.getResourceId(this.getTarget()));
//            map.set(Crowdsourcing.getResourceId(this.getTarget()), [] );
        }
        let value = JSON.stringify(Array.from(map.entries()));
        localStorage.setItem("CrowdsourcingQuestion_" + this.id, value);
    }
    
    crowdsourcing.Question.prototype.saveToLocalStorage = function() {
        let map = this.getAnnotationsFromLocalStorage();
        map.set(Crowdsourcing.getResourceId(this.getTarget()), this.annotations );
        let value = JSON.stringify(Array.from(map.entries()));
        localStorage.setItem("CrowdsourcingQuestion_" + this.id, value);
    }
    
    crowdsourcing.Question.prototype.restoreFromLocalStorage = function() {
        let map = this.getAnnotationsFromLocalStorage();
        let annotations;
        if(map.has(Crowdsourcing.getResourceId(this.getTarget()))) {
            annotations = map.get(Crowdsourcing.getResourceId(this.getTarget())).map( anno => this.createAnnotation(anno));
        } else {
            annotations = [];
        }
        return annotations;
    }
    
    crowdsourcing.Question.prototype.getAnnotationsFromLocalStorage = function() {
        let string = localStorage.getItem("CrowdsourcingQuestion_" + this.id);
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
    
    

    crowdsourcing.Question.Type = {};
    crowdsourcing.Question.Frequency = {};
    crowdsourcing.Question.Selector = {};

    
    crowdsourcing.Question.Type.PLAINTEXT = "PLAINTEXT";
    crowdsourcing.Question.Type.DATE_PICKER = "DATE_PICKER";
    crowdsourcing.Question.Type.GEOLOCATION_POINT = "GEOLOCATION_POINT";
    crowdsourcing.Question.Type.GEOLOCATION_AREA = "GEOLOCATION_AREA";
    crowdsourcing.Question.Type.TRANSCRIPTION = "TRANSCRIPTION";
    crowdsourcing.Question.Type.KEY_VALUE_LIST = "KEY_VALUE_LIST";
    crowdsourcing.Question.Type.get = function(text) {
        return crowdsourcing.Question.Type[text];
    }
    crowdsourcing.Question.getType = function(question) {
        return crowdsourcing.Question.Type.get(question.questionType);
    }
    
    crowdsourcing.Question.Frequency.ONE_PER_MANIFEST = "ONE_PER_MANIFEST";
    crowdsourcing.Question.Frequency.MULTIPLE_PER_MANIFEST = "MULTIPLE_PER_MANIFEST";
    crowdsourcing.Question.Frequency.ONE_PER_CANVAS = "ONE_PER_CANVAS";
    crowdsourcing.Question.Frequency.MULTIPLE_PER_CANVAS = "MULTIPLE_PER_CANVAS";
    crowdsourcing.Question.Frequency.get = function(text) {
        return crowdsourcing.Question.Frequency[text];
    }
    crowdsourcing.Question.getFrequency = function(question) {
        return crowdsourcing.Question.Frequency.get(question.targetFrequency);
    }
    
    crowdsourcing.Question.Selector.MULTIPLE_PER_CANVAS = "WHOLE_SOURCE";
    crowdsourcing.Question.Selector.WHOLE_PAGE = "WHOLE_PAGE";
    crowdsourcing.Question.Selector.RECTANGLE = "RECTANGLE";
    crowdsourcing.Question.Selector.get = function(text) {
        return crowdsourcing.Question.Selector[text];
    }
    crowdsourcing.Question.getSelector = function(question) {
        return crowdsourcing.Question.Selector.get(question.targetSelector);
    }    
    
    return crowdsourcing;
    
} )( Crowdsourcing );
