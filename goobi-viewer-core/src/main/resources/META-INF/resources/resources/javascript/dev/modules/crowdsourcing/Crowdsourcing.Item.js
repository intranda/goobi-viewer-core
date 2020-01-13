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
 * @description Represents a crowdsourcing item, consisting of a campaign and a manifest which to apply it to * 
 * @version 3.7.0
 * @module Crowdsourcing.js
 * @requires jQuery
 */
var Crowdsourcing = ( function(crowdsourcing) {
    'use strict';

    let _debug = false;
    const LOCAL_STORAGE_ITEM = "goobi_viewer_crowdsourcing_annotations";
    
    /**
     * Constructor for a new item. 
     * @param item  A json object containing the campaign item data
     * @param initialCanvasIndex the index of the canvas to open initially. If not used, index = 0 is used
     */
    crowdsourcing.Item = function(item, initialCanvasIndex) {
        if ( _debug ) {
            console.log( '##############################' );
            console.log( 'Crowdsourcing.Item' );
            console.log( 'Crowdsourcing.Item.canvases ', canvases );
            console.log( 'Crowdsourcing.Item.questions ', questions );
            console.log( '##############################' );
        }
        
        this.id = item.campaign.id;
        this.reviewMode = false;
        this.translations = item.campaign.translations;
        this.questions = item.campaign.questions.map(question => new Crowdsourcing.Question(question, this));
        this.currentCanvasIndex = initialCanvasIndex ? initialCanvasIndex : 0;
        this.imageSource = item.source;
        this.imageOpenEvents = new Rx.Subject();
        this.imageRotationEvents = new Rx.Subject();
        this.annotationRelaodEvents = new Rx.Subject();
        this.itemInitializedSubject = new Rx.Subject();

        let firstAreaQuestion = this.questions.find(q => q.isRegionTarget());
        if(firstAreaQuestion) {
            firstAreaQuestion.active = true;
        }
    };

    /**
     * Takes an Rx.Observable which should trigger every time a new image is
     */
    crowdsourcing.Item.prototype.notifyImageOpened = function(observable) {
        observable.subscribe(this.imageOpenEvents);
    }
    
    crowdsourcing.Item.prototype.notifyImageRotated = function(byDegrees) {
        this.imageRotationEvents.onNext(byDegrees);
    }
    
    crowdsourcing.Item.prototype.onImageRotated = function(eventHandler, errorHandler, completedHandler) {
        this.imageRotationEvents.subscribe(eventHandler, errorHandler, completedHandler);
    }
    
    crowdsourcing.Item.prototype.onImageOpen = function(eventHandler, errorHandler, completedHandler) {
        this.imageOpenEvents.subscribe(eventHandler, errorHandler, completedHandler);
    }
    
    crowdsourcing.Item.prototype.notifyAnnotationsReload = function() {
        this.annotationRelaodEvents.onNext();
    }
    
    crowdsourcing.Item.prototype.onAnnotationsReload = function(eventHandler, errorHandler, completedHandler) {
        this.annotationRelaodEvents.subscribe(eventHandler, errorHandler, completedHandler);
    }
    
    crowdsourcing.Item.prototype.notifyItemInitialized = function() {
        console.log("notifyItemInitialized");
        this.itemInitializedSubject.onNext();
        this.itemInitializedSubject.notified = true;
    }
    
    crowdsourcing.Item.prototype.onItemInitialized = function(eventHandler, errorHandler, completedHandler) {
        console.log("onItemInitialized");
        this.itemInitializedSubject.subscribe(eventHandler, errorHandler, completedHandler);
        //If a notification happened before we subscribe, notify us now
        if(this.itemInitializedSubject.notified) {
            this.itemInitializedSubject.onNext();
            this.itemInitializedSubject.notified = false;
        }
    }
    
    crowdsourcing.Item.prototype.initViewer = function(imageSource) {
        this.canvases = _getCanvasList(imageSource);
        this.currentCanvasIndex = Math.max(0, Math.min(this.currentCanvasIndex, this.canvases.length-1));
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

    crowdsourcing.Item.prototype.getCreator = function() {
        return {
            id: String(Crowdsourcing.user.id),
            type: "Person",
            name: Crowdsourcing.user.name,
        }
    }
    
    crowdsourcing.Item.prototype.createAnnotationMap = function(annotations) {
        let save = {
                campaign: this.id,
                manifest: this.imageSource,
                questions: []
            }
            
            this.questions.forEach(function(question) {
               let q = {
                    id: question.id,
                    pages: []
               } 
               save.questions.push(q);
            });
            
            this.addAnnotations(annotations, save);
            
            return save;
    }
    
    crowdsourcing.Item.prototype.deleteAnnotations = function(save, pageId, questionId) {
        let questions = save.questions;
        if(questionId) {
            questions = questions.filter(q => q.id == questionId);
        }
        questions.forEach(function(question) {
            let pages = question.pages;
            if(pageId) {
                pages = pages.filter(p => p.id ==pageId);
            }
            pages.forEach(function(page) {
               page.annotations = []; 
            });
        })
        return save;
    }
    
    crowdsourcing.Item.prototype.loadAnnotations = function(pageId, questionId) {
        let save = this.getFromLocalStorage();
        let annotations = [];
        let questions = save.questions;
        if(questionId) {
            questions = questions.filter(q => q.id == questionId);
        }
        questions.forEach(function(question) {
            let pages = question.pages;
            if(pageId) {
                pages = pages.filter(p => p.id ==pageId);
            }
            pages.forEach(function(page) {
               annotations = annotations.concat(page.annotations) 
            });
        })
        return annotations;
    }
    
    crowdsourcing.Item.prototype.loadAnnotationPages = function(questionId) {
        let save = this.getFromLocalStorage();
        let pages = [];
        let questions = save.questions;
        if(questionId) {
            questions = questions.filter(q => q.id == questionId);
        }
        questions.forEach(function(question) {
            question.pages.forEach(function(page) {
               let pageToLoad = pages.find(p => p.id == page.id);
               if(!pageToLoad) {
                   pageToLoad = {
                       id: page.id,
                       annotations: []
                   }
                   pages.push(pageToLoad);
               }
               pageToLoad.annotations = pageToLoad.annotations.concat(page.annotations);
            });
        })
        return pages;
    }
    
    crowdsourcing.Item.prototype.saveAnnotations = function(pageId, questionId, annotations) {
        let save = this.getFromLocalStorage();
        this.deleteAnnotations(save, pageId, questionId);
        this.addAnnotations(annotations, save);
        this.saveToLocalStorage(save);
    }

        
    crowdsourcing.Item.prototype.addAnnotations = function(annotations, save) {
        annotations.forEach(function(annotation) {
            let questionId = annotation.generator.id;
            let question = save.questions.find( q => q.id == questionId);
            let pageId = Crowdsourcing.getResourceId(annotation.target);
            if(question && pageId) {
                let page = question.pages.find( p => p.id == pageId);
                if(!page) {
                    page = {
                        id: pageId,
                        annotations: []
                    }
                    question.pages.push(page);
                }
                page.annotations.push(annotation);
            }
        });
        return save;
    }

    crowdsourcing.Item.prototype.saveToLocalStorage = function(save) {
        localStorage.setItem(LOCAL_STORAGE_ITEM, JSON.stringify(save));
    }
    
    crowdsourcing.Item.prototype.getFromLocalStorage = function() {
        let jsonString = localStorage.getItem(LOCAL_STORAGE_ITEM);
        let json = JSON.parse(jsonString);
        return json;
    }
    
    crowdsourcing.Item.prototype.setReviewMode = function(review) {
        this.reviewMode = review ? true : false;
    }

    crowdsourcing.Item.prototype.isReviewMode = function() {
        return this.reviewMode;
    }

    
    
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
    
} )( Crowdsourcing );
