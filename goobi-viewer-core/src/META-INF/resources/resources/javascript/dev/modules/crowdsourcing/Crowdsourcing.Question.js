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

    const COLORS = [" #e74c3c ", " #2980b9 ",  "#2ecc71 ", " #f1c40f ", " #9b59b6 ", " #d35400 ", " #1abc9c ", " #f06292 ", " #7e57c2 ", " #aed581 ",
          " #CCFF00 ", " #9575cd ", " #ff8a65 ", " #CC6699 ", " #CC6699 ", " #8e24aa ", " #d35400 ", " #e74c3c "]
    
    crowdsourcing.Question = function(question, item) {
        let temp = crowdsourcing.deepCopy(question);
        Object.assign(this, temp);
        
        this.item = item;
        this.questionType = Crowdsourcing.Question.getType(this);
        this.targetFrequency = this.targetFrequency;
        this.targetSelector = Crowdsourcing.Question.getSelector(this);

        this.annotations = [];
        this.currentAnnotationIndex = -1;
    }
    
    crowdsourcing.Question.prototype.createAnnotation = function(anno) {
        throw "Must implement createAnnotation";
    }
    
    
    crowdsourcing.Question.prototype.loadAnnotationsFromLocalStorage = function() {
        this.annotations = this.getAnnotationsFromLocalStorage();
    }
    
    crowdsourcing.Question.prototype.setDrawingPermission = function() {
        if(this.areaSelector) {       
            if(this.item.isReviewMode()) {
                this.areaSelector.disableDrawer();
                this.areaSelector.disableTransformer();
            } else if(this.targetFrequency > 0 && this.targetFrequency <= this.annotations.length) {
                this.areaSelector.disableDrawer();
                this.areaSelector.enableTransformer();
            } else {
                this.areaSelector.enableDrawer();
                this.areaSelector.enableTransformer();
            }
        }
    }

    /**
     * Initializes the annotations for the current page
     */
    crowdsourcing.Question.prototype.initAnnotations = function() {
        switch(this.targetSelector) {
            case Crowdsourcing.Question.Selector.RECTANGLE:
                if(this.areaSelector) {                    
                    this.areaSelector.reset();
                    this.setDrawingPermission();
                    this.annotations
                    .forEach(anno => this.areaSelector.addOverlay(anno, this.item.image.viewer));
                    this.areaSelector.setDrawingStyle(this.areaSelector.getStyle());
                }
                break;
            case Crowdsourcing.Question.Selector.WHOLE_PAGE:
            case Crowdsourcing.Question.Selector.WHOLE_SOURCE:
                if(this.annotations.length == 0 && !this.item.isReviewMode()) {                    
                    this.createEmptyAnnotation();
                }
        }
    }
    
    crowdsourcing.Question.prototype.initAreaSelector = function() {
        this.areaSelector = new Crowdsourcing.AreaSelector(this.item, true, COLORS);
        this.areaSelector.init();

    }
    
    crowdsourcing.Question.prototype.getAnnotation = function(id) {
        return this.annotations.find(anno => anno.id == id);
    }
    
    crowdsourcing.Question.prototype.getAnnotationByOverlayId = function(id) {
        return this.annotations.find(anno => anno.overlayId == id);
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
        annotation.overlayId = id;
        annotation.setTarget(this.getTarget());
        annotation.setRegion(region);
        annotation.setColor(color);
        this.annotations.push(annotation);
        this.currentAnnotationIndex = this.annotations.length - 1;
        this.saveToLocalStorage();
        this.setDrawingPermission();
    }
    
    crowdsourcing.Question.prototype.getImageUrl = function(rect, imageId) {
        let rotation = this.item.image ? this.item.image.getRotation() : 0;
        let url = imageId + "/" + rect.x + "," + rect.y + "," + rect.width + "," + rect.height + "/full/" + rotation + "/default.jpg";
        return url;
    }
    
    crowdsourcing.Question.prototype.getTarget = function() {
        if(this.targetSelector == Crowdsourcing.Question.Selector.WHOLE_SOURCE) {
            return this.item.imageSource;
        } else {            
            return this.item.getCurrentCanvas();
        }
    }
    
    crowdsourcing.Question.prototype.getTargetId = function() {
        let target = (this.targetSelector == Crowdsourcing.Question.Selector.WHOLE_SOURCE) ? this.item.imageSource : this.item.getCurrentCanvas();
        return crowdsourcing.getResourceId(target);
    }

    crowdsourcing.Question.prototype.deleteAnnotation = function(anno) {
        let index = this.getIndex(anno);
        if(index > -1) {            
            this.annotations.splice(index,1);
            if(this.currentAnnotationIndex >= index) {
                this.currentAnnotationIndex--;
            }
            if(this.areaSelector) {             
                this.areaSelector.removeOverlay(anno);
            }
            this.saveToLocalStorage();
        }
        this.setDrawingPermission();
    }

    
    crowdsourcing.Question.prototype.createEmptyAnnotation = function() {
        let anno = this.createAnnotation({});
        anno.setTarget(this.getTarget());
        this.annotations.push(anno);    
        this.currentAnnotationIndex = this.annotations.length - 1;
    }


    crowdsourcing.Question.prototype.getGenerator = function() {
        return  {
            id: String(this.id),
            type: "Software"
        }
    }
    
    crowdsourcing.Question.prototype.deleteFromLocalStorage = function() {
        this.item.deleteAnnotations(save, this.getTargetId(), this.id);
    }
    
    crowdsourcing.Question.prototype.saveToLocalStorage = function() {
        let annotationsToSave = this.annotations;
        if(this.targetSelector == Crowdsourcing.Question.Selector.WHOLE_PAGE ||
            this.targetSelector == Crowdsourcing.Question.Selector.WHOLE_SOURCE) {
            annotationsToSave = annotationsToSave.filter(anno => anno.getText() && anno.getText().length > 0);
        }
        this.item.saveAnnotations(this.getTargetId(), this.id, annotationsToSave);
    }
    
    crowdsourcing.Question.prototype.getAnnotationsFromLocalStorage = function() {
        let targetId = this.targetSelector == Crowdsourcing.Question.Selector.WHOLE_SOURCE ? undefined : this.getTargetId();
        let annotations = this.item.loadAnnotations(targetId, this.id);
        annotations = annotations.map(anno => this.createAnnotation(anno));

        return annotations;
    }
    
    

    crowdsourcing.Question.Type = {};
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
