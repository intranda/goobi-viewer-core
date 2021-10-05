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
     * @param item  A json object built from CampaignItem.java containing the campaign item data
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
        
        this.id = item.campaign.url;
        this.campaignId = item.campaign.id;
        this.recordIdentifier = item.recordIdentifier;
        this.reviewMode = false;
        this.showLog = item.campaign.showLog;
        if(this.showLog) {
            this.log = item.log;
        }
        this.translations = item.campaign.translations;
        this.questions = item.campaign.questions.map(question => new Crowdsourcing.Question(question, this));
        this.currentCanvasIndex = initialCanvasIndex ? initialCanvasIndex : 0;
        this.imageSource = item.source;
        this.metadata = item.metadata;
        this.pageStatisticMode = item.pageStatisticMode;
        //maps page numbers (1-based!) to one of the following status: blank, annotate, locked, review, finished
        this.pageStatusMap = viewerJS.parseMap(item.pageStatusMap);
        this.reviewActive = item.campaign.reviewMode != "NO_REVIEW";
        this.currentUser = {};
        this.imageOpenEvents = new rxjs.Subject();
        this.toggleImageViewEvents = new rxjs.Subject();
        this.imageRotationEvents = new rxjs.Subject();
        this.annotationRelaodEvents = new rxjs.Subject();
        this.itemInitializedSubject = new rxjs.Subject();
        this.statusMapUpdates = new rxjs.Subject();
        //Used to manually force imageControls to show thumbnail view
        this.setShowThumbs = new rxjs.Subject();
		//signals that there are annotations which have not been saved to the server
		//Is set on a per record or a per page basis depending on this.pageStatisticMode
		this.dirty = false;

        let firstAreaQuestion = this.questions.find(q => q.isRegionTarget());
        if(firstAreaQuestion) {
            firstAreaQuestion.active = true;
        }
        
        if(this.pageStatisticMode) {
	        this.initWebSocket();
        }        
        this.initKeyboardEvents();
        
        // console.log("initialized crowdsourcing item ", this);
        
    };
    
    crowdsourcing.Item.prototype.initKeyboardEvents = function() {
    	document.addEventListener('keyup', (event) => {
    		//don't handle events if an input element is focused
    		if($(event.target).closest("input").length > 0) {
    			return;
    		}
    		let keyName = event.key;
    		let targetIndex = undefined;
    		switch(keyName) {
    			case "ArrowLeft":
    				targetIndex = this.getPreviousAccessibleIndex(this.currentCanvasIndex);
    				break;
    			case "ArrowRight":
    				targetIndex = this.getNextAccessibleIndex(this.currentCanvasIndex);
    				break;
    		}
    		if(targetIndex != undefined) {
    			this.loadImage(targetIndex, true);
    		}
    				
    	});
    }
    
    
    crowdsourcing.Item.prototype.initWebSocket = function() {
 		this.socket = new viewerJS.WebSocket(window.location.host, window.currentPath, viewerJS.WebSocket.PATH_CAMPAIGN_SOCKET);
    	this.socket.onMessage.subscribe((event) => {
    		//console.log("received message ", event.data);
    		let data = JSON.parse(event.data);
    		if(data.status) {
    			this.handleMessage(data);
    		} else {
		    	this.handleLocks(data);   		
    		}
    		
    	});
    	this.onImageOpen((image) => {
    		//console.log("Call websocket on image open " + this.currentCanvasIndex);
    		let message = {
    			campaign : this.campaignId,
    			record : this.recordIdentifier,
    			page : this.currentCanvasIndex + 1,
    		}
    		this.socket.sendMessage(JSON.stringify(message));
    	});
    }
    
    crowdsourcing.Item.prototype.handleMessage = function(message) {
    	let status = message.status;
    	let messageKey = message.message;
    	if(status && messageKey) {
    		viewerJS.translator.addTranslations(messageKey)
    		.then( () => {
    			let message = viewerJS.translator.translate(messageKey);
    			viewerJS.notifications.notify(message, type);
    		});
    	}
    }
    
    
    crowdsourcing.Item.prototype.handleLocks = function(locks) {
    	this.buildPageStatusMap(locks);
    	if(this.isPageAccessible(this.currentCanvasIndex)) {
    		this.statusMapUpdates.next(this.pageStatusMap);
    	} else {
    		let targetIndex = this.getNextAccessibleIndex(this.currentCanvasIndex);
    		console.log("handle locks", targetIndex);
    		if(targetIndex == undefined) {
    			this.setShowThumbs.next(true);
    		} else {
    			//console.log("load next image: " + targetIndex);
	    		this.loadImage(targetIndex);
	    	}
    	}
    }
    
    crowdsourcing.Item.prototype.buildPageStatusMap = function(locks) {
    	this.pageStatusMap = new Map();
    	this.canvases.forEach((canvas, index) => {
	    	if(this.pageStatisticMode) {
	    		let status = locks[index+1];
	    		this.pageStatusMap.set(index, status ? status.toLowerCase() : "blank");
    		} else {
    			this.pageStatusMap.set(index, this.reviewMode ?  "review" : "annotate" );
    		}
    	});
    };
    
    crowdsourcing.Item.prototype.isPageAccessible = function(index) {
    	if(!this.pageStatisticMode) {
    		return index > -1 && index < this.canvases.length;
    	} else {    	
	    	let status = this.pageStatusMap.get(index);
	    	if(this.reviewMode) {
	    		return "review" == status;
	    	} else {
	    		return "annotate" == status || "blank" == status;
	    	}
    	}
    };
    
   crowdsourcing.Item.prototype.getNextAccessibleIndex = function(currentIndex) {
   		if(this.canvases.length == 1) {
   			return undefined;
   		} else if(currentIndex == this.canvases.length-1) {
   			return this.getNextAccessibleIndex(-1);
   		} else {
	   		for (let i = currentIndex+1; i < this.canvases.length; i++) { 
	   			if(this.isPageAccessible(i)) {
	   				return i;
	   			} 
	   		}
	   		return undefined;
   		}
   }
    
   crowdsourcing.Item.prototype.getPreviousAccessibleIndex = function(currentIndex) {
   		if(this.canvases.length == 1) {
   			return undefined;
   		} else if(currentIndex == 0) {
   			return this.getPreviousAccessibleIndex(this.canvases.length);
   		} else {
	   		for (let i = currentIndex-1; i > -1; i--) { 
	   			if(this.isPageAccessible(i)) {
	   				return i;
	   			} 
	   		}
	   		return undefined;
   		}
   }

    crowdsourcing.Item.prototype.setCurrentUser = function(id, name, avatar) {
        this.currentUser.userId = id;
        this.currentUser.name = name;
        this.currentUser.avatar = avatar;
    }
    
    /**
     * add a new message to the log and also to the messages to send back with the item status
     */
    crowdsourcing.Item.prototype.addLogMessage = function(message) {
        this.log.push(message);
        if(this.logEndpoint) {            
            return fetch(this.logEndpoint, {
                method: "POST",
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(message),
                cache: "no-cache",
                mode: 'cors',
            })
        }
    }


 
    /**
     * Takes an rxjs.Observable which should trigger every time a new image is
     */
    crowdsourcing.Item.prototype.notifyImageOpened = function(observable) {
        observable.subscribe(this.imageOpenEvents);
    }
    
    crowdsourcing.Item.prototype.notifyImageRotated = function(byDegrees) {
        this.imageRotationEvents.next(byDegrees);
    }
    
    crowdsourcing.Item.prototype.notifyImageViewChanged = function(viewThumbs) {
        this.toggleImageViewEvents.next(viewThumbs);
    }
    
    crowdsourcing.Item.prototype.onImageRotated = function(eventHandler, errorHandler, completedHandler) {
        this.imageRotationEvents.subscribe(eventHandler, errorHandler, completedHandler);
    }
    
    crowdsourcing.Item.prototype.onImageOpen = function(eventHandler, errorHandler, completedHandler) {
        this.imageOpenEvents.subscribe(eventHandler, errorHandler, completedHandler);
    }
    
    crowdsourcing.Item.prototype.onImageViewChanged = function(eventHandler, errorHandler, completedHandler) {
        this.toggleImageViewEvents.subscribe(eventHandler, errorHandler, completedHandler);
    }
    
    crowdsourcing.Item.prototype.notifyAnnotationsReload = function() {
        this.annotationRelaodEvents.next();
    }
    
    crowdsourcing.Item.prototype.onAnnotationsReload = function(eventHandler, errorHandler, completedHandler) {
        this.annotationRelaodEvents.subscribe(eventHandler, errorHandler, completedHandler);
    }
    
    crowdsourcing.Item.prototype.notifyItemInitialized = function() {
        this.itemInitializedSubject.next();
        this.itemInitializedSubject.notified = true;
    }
    
    crowdsourcing.Item.prototype.onItemInitialized = function(eventHandler, errorHandler, completedHandler) {
        this.itemInitializedSubject.subscribe(eventHandler, errorHandler, completedHandler);
        //If a notification happened before we subscribe, notify us now
        if(this.itemInitializedSubject.notified) {
            this.itemInitializedSubject.next();
            this.itemInitializedSubject.notified = false;
        }
    }
    
    crowdsourcing.Item.prototype.initViewer = function(imageSource) {
        this.canvases = _getCanvasList(imageSource);
        this.currentCanvasIndex = Math.max(0, Math.min(this.currentCanvasIndex, this.canvases.length-1));
        //build a simple page status map now that the canvas list is known
        if(!this.pageStatisticMode) {
        	this.buildPageStatusMap();
        }
    }
    
    crowdsourcing.Item.prototype.loadImage = function(index, requireConfirmation) {
        if(index == undefined) {
        	return;
        }
        //console.log("load image", this.dirty, requireConfirmation, index, this.currentCanvasIndex);
        if(this.pageStatisticMode && this.dirty && requireConfirmation && index != this.currentCanvasIndex) {
        	viewerJS.notifications.confirm(Crowdsourcing.translate("crowdsourcing__confirm_skip_page"))
        	.then( () => {
        		this.dirty = false;
	            this.currentCanvasIndex = index;
	        	if(this.setImageSource) {            
		            this.setImageSource(this.getCurrentCanvas());
	        	}
        	})
        	.catch(() => {});
        } else {
        	if(this.pageStatisticMode) {
        		this.dirty = false;
        	}
            this.currentCanvasIndex = index;
	        if(this.setImageSource) {            
		    	this.setImageSource(this.getCurrentCanvas());
	        }
        }
    }
    
    crowdsourcing.Item.prototype.getCurrentCanvas = function() {
        return this.canvases[this.currentCanvasIndex];
    }
    
    crowdsourcing.Item.prototype.getImageService = (canvas) =>  {
        return canvas.images[0].resource.service["@id"] + "/info.json";
    }
    
    crowdsourcing.Item.prototype.getImageId = (canvas) =>  {
        return canvas.images[0].resource.service["@id"];
    }

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
    
    /**
    * From the given save, remove all annotations. If pageId and/or questionId are given
    * only delete from that page and/or question
    */
    crowdsourcing.Item.prototype.deleteAnnotations = function(save, pageId, questionId) {
	    if(!save) {
	    	return
	    }
    
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
    
    /**
    * Return list of annotations, optionally filtered by pageId and questionId
    */
    crowdsourcing.Item.prototype.loadAnnotations = function(pageId, questionId) {
        let annotations = [];
        let save = this.getFromLocalStorage();
		if(save) {
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
        }
        return annotations;
    }
    
    /**
    * Return a list of pages consisting of a pageId and an array of all annotations on that page; optionally filter by questionId 
    */
    crowdsourcing.Item.prototype.loadAnnotationPages = function(questionId, pageId) {
        let save = this.getFromLocalStorage();
        let pages = [];
        let questions = save.questions;
        if(questionId) {
            questions = questions.filter(q => q.id == questionId);
        }
        questions.forEach(function(question) {
            let questionPages = question.pages
            if(pageId) {
            	questionPages = questionPages.filter(page => pageId == page.id)
            }
            questionPages.forEach(function(page) {
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
    
    
    /**
    * Remove all annotations for the given pageId and questionId (all if pageId/questionId is not given) from local storage
    * and add the given annotations to local storage
    */
    crowdsourcing.Item.prototype.saveAnnotations = function(pageId, questionId, annotations) {
        let save = this.getFromLocalStorage();
        this.deleteAnnotations(save, pageId, questionId);
        this.addAnnotations(annotations, save);
        this.saveToLocalStorage(save);
        this.dirty = true;
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
        sessionStorage.setItem(LOCAL_STORAGE_ITEM, JSON.stringify(save));
    }
    
    crowdsourcing.Item.prototype.getFromLocalStorage = function() {
        let jsonString = sessionStorage.getItem(LOCAL_STORAGE_ITEM);
        let json = JSON.parse(jsonString);
        return json;
    }
    
    crowdsourcing.Item.prototype.setReviewMode = function(review) {
        this.reviewMode = review ? true : false;
    }

    crowdsourcing.Item.prototype.isReviewMode = function() {
        if (this.pageStatisticMode) {
            //console.log('statistic mode index ' + (this.currentCanvasIndex) + ': '  + (this.pageStatusMap.get(this.currentCanvasIndex)))
            return this.pageStatusMap.get(this.currentCanvasIndex) == 'review';
        } else {
            return this.reviewMode;
        }
    }
    
    crowdsourcing.Item.prototype.isReviewActive = function() {
        return this.reviewActive;
    }

    crowdsourcing.Item.prototype.getCurrentPageId = function() {
		let canvas = this.canvases[this.currentCanvasIndex];
		if(canvas) {
			return viewerJS.iiif.getId(canvas);
		} else {
			return undefined;
		}
    }
    
    crowdsourcing.Item.prototype.loadNextItem = function(requireConfirmation) {
	     let promise = Promise.resolve();
	     if(this.dirty && requireConfirmation) {
	     	promise = viewerJS.notifications.confirm(Crowdsourcing.translate("crowdsourcing__confirm_skip_page"))
	     }
	     promise.then( () => {
		     window.location.href = this.nextItemUrl;
	     })
	     .catch((e) => {});
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
