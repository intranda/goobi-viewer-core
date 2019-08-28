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
    
    var _debug = false; 
    
    crowdsourcing.Answer.Type.Textual = "TextualBody";
    crowdsourcing.Answer.Type.Dataset = "Dataset";
    crowdsourcing.Answer.Type.Link = "Link";

    crowdsourcing.Answer.createFromAnnotation = function(annotation, sources) {
        let answer = new crowdsourcing.Answer();
        if(crowdsourcing.isString(annotation.body)) {
            answer.body = {type: crowdsourcing.Answer.Type.Link, url: annotation.body};
        } else if(annotation.body && annotation.body.type == "TextualBody") {
            answer.body = {type: crowdsourcing.Answer.Type.Textual, text: annotation.body.value};
        } else if(annotation.body) {
            answer.body = {type: crowdsourcing.Answer.Type.Dataset, value: annotation.body.value ? annotation.body.value : annotation.body };
        } else {
            answer.body = {};
        }
        
        let targetId = undefined;
        if(crowdsourcing.isString(annotation.target)) {            
            targetId = annotation.target;
        } else if(annotation.target.source) {
            targetId = annotation.target.source;
        } else {
            targetId = annotation.target.id;
        }
        let annoSources = sources.filter( (source) => source == targetId || source.id == targetId || source["@id"] == targetId ).
        if(annoSources.length) {
            anno
        }
    }

    crowdsourcing.Answer = function(body, selector) {
        if(body) {
            this.body = body;            
        } else {
            this.body = {type: crowdsourcing.Answer.Type.Textual, text: ""};
        }
        if(selector) {            
            this.selector = selector;
        } else {
            this.selector = {color: undefined, region: undefined};
        }
    }
    
    crowdsourcing.Answer.prototype.setTextFromEvent = function(e) {
        console.log("set text from ", e);
        let text = e.target.value;
        this.setText(text);
    }
    
    crowdsourcing.Answer.prototype.setText = function(text) {
        this.body.type = crowdsourcing.Answer.Type.Textual;
        this.body.text = text;
    }
    
    crowdsourcing.Answer.prototype.createAnnotation = function(targetResource) {
        let annotation = {
                "@context": "http://www.w3.org/ns/anno.jsonld",
                type: "Annotation"
                body: this.createBody(this.body),
                target: this.createTarget(targetResource, this.target)
        }
        if(this.creator) {
            annotation.creator = this.creator
        }
        if(this.generator) {
            annotation.generator = this.generator
        }
        if(this.created) {
            annotation.created = this.created;
            annotation.modified = new Date().toISOString();
        } else {
            annotation.created = new Date().toISOString();
        }
        return annotation;
    }
    
    crowdsourcing.Answer.prototype.createTarget = function(targetResource, fragment) {
        let target = targetResource.id;
        if(fragment.region) {
            target = {
                  source: targetResource.id,
                  selector: {
                      type: "FragmentSelector",
                      value: "xywh=" + fragment.region.x + "," + fragment.region.y + "," + fragment.region.width + "," + fragment.region.height
                  }
            }
        }
    }

    
    crowdsourcing.Answer.prototype.createBody = function(bodyInfo) {
        let body = {};
        if(bodyInfo.type == crowdsourcing.Answer.Type.Textual) {
            //create textual body
            body.type = "TextualBody";
            body.format = "text/plain";
            body.value = bodyInfo.text;
        } else if (bodyInfo.type == crowdsourcing.Answer.Type.Dataset) {
            body.type = crowdsourcing.Answer.Type.Dataset;
            body.format = "application/json";
            body.value = bodyInfo.data;
        } else if (bodyInfo.type == crowdsourcing.Answer.Type.Link) {
            body = bodyInfo.url;
        }
    }
    
    return crowdsourcing;
    
} )( Crowdsourcing );
