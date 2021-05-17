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
 

    crowdsourcing.Annotation = function(anno) {
        let temp = crowdsourcing.deepCopy(anno ? anno : {});  
        Object.assign(this, temp);
        if(!anno) {
            this.setCreated(new Date());
        }
    }
    
    crowdsourcing.Annotation.prototype.setCreated = function(date) {
        
        //convert to UTC timezone
        var utc_timestamp = Date.UTC(date.getUTCFullYear(),date.getUTCMonth(), date.getUTCDate() , 
                date.getUTCHours(), date.getUTCMinutes(), date.getUTCSeconds(), date.getUTCMilliseconds());
        date = new Date(utc_timestamp);
        this.created = date.toISOString().replace(/\.\d{3}Z/, "Z"); //remove milliseconds to conform to format accepted by java
    }
    
    crowdsourcing.Annotation.prototype.setModified = function(date) {
        
        //convert to UTC timezone
        var utc_timestamp = Date.UTC(date.getUTCFullYear(),date.getUTCMonth(), date.getUTCDate() , 
                date.getUTCHours(), date.getUTCMinutes(), date.getUTCSeconds(), date.getUTCMilliseconds());
        date = new Date(utc_timestamp);
        this.modified = date.toISOString().replace(/\.\d{3}Z/, "Z"); //remove milliseconds to conform to format accepted by java
    }
    
    crowdsourcing.Annotation.prototype.getCreated = function() {
        return this.created ? Date.parse(this.created) : undefined;  
    }

    crowdsourcing.Annotation.prototype.getModified = function() {
        return this.modified ? Date.parse(this.modified) : undefined;  
    }
    
    /**
     * Default implementation: empty if no body is present
     */
    crowdsourcing.Annotation.prototype.isEmpty = function() {
        return this.body == undefined;
    }


    crowdsourcing.Annotation.prototype.setRegion = function(rect) {
        if(!this.target) {
            throw "Annotation needs a target before a fragment can be applied";
        } else {
            let newTarget = {
                source : Crowdsourcing.getResourceId(this.target),
                type: "SpecificResource",
                selector : {
                    type : "FragmentSelector",
                    conformsTo: "http://www.w3.org/TR/media-frags/",
                    value : "xywh=" + Math.round(rect.x) + "," + Math.round(rect.y) + "," + Math.round(rect.width) + "," + Math.round(rect.height)         
                }
            }
            if(this.target != newTarget) {                
                this.target = newTarget;
                this.setModified(new Date());
            }
        }
    }
    
    crowdsourcing.Annotation.prototype.getRegion = function() {
        if(this.target && this.target.selector && this.target.selector.type == "FragmentSelector") {
            let regex = /xywh=(\d+),(\d+),(\d+),(\d+)/;
            let match = regex.exec(this.target.selector.value);
            if(match) {
                return {
                    x : parseInt(match[1]),
                    y : parseInt(match[2]),
                    width : parseInt(match[3]),
                    height : parseInt(match[4]),
                }
            }
        } else {
            return undefined;
        }
    }
    
    crowdsourcing.Annotation.prototype.setBody = function(body) {
        this.body = body;
    }

    
    crowdsourcing.Annotation.prototype.setTarget = function(target) {
        if(this.target != target) {                
            this.setModified(new Date());
        }
        if(crowdsourcing.isString(target)) {
            this.target = target;
        } else if(target.source) {
            this.target = target.source;
        } else if(target.id) {
            this.target = target.id;
        } else if(target["@id"]) {
            this.target = target["@id"];
        } else {
            this.target = target;
        }
    }
    
    crowdsourcing.Annotation.prototype.getColor = function() {
        return this.color;
    }

    crowdsourcing.Annotation.prototype.setColor = function(color) {
        this.color = color;
    }  
    
    // USE ESCAPE TO GO BACK TO THUMBNAIL OVERVIEW
    $(document).keydown(function(event){ 
        var keyCode = (event.keyCode ? event.keyCode : event.which);   
        if (keyCode == 27) {
            $('.image-controls__action.thumbs').trigger('click');
        }
    });
    


    
    return crowdsourcing;
    
} )( Crowdsourcing );
