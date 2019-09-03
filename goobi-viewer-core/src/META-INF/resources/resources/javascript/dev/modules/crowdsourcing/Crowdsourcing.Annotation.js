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
        let temp = crowdsourcing.deepCopy(anno);
        Object.assign(this, temp);
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
            this.target = newTarget;
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
    
    crowdsourcing.Annotation.prototype.setTarget = function(target) {
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
        if(this.body) {
            return this.body.color;
        } else {
            return undefined;
        }
    }

    crowdsourcing.Annotation.prototype.setColor = function(color) {
        if(!this.body) {
            this.body = {}
        }
        this.body.color = color;
    }
    
    return crowdsourcing;
    
} )( Crowdsourcing );
