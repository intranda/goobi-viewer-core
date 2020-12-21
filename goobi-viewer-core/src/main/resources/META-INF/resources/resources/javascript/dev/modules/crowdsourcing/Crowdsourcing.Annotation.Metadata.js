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


    crowdsourcing.Annotation.Metadata = function(annotation, originalData) {
        crowdsourcing.Annotation.call(this, annotation);
        if(!this.body) {
            this.body = {
                    type: "Dataset",
                    format: "goobi-viewer-index",
                    data: {}
            }
        }
        $.extend(this.body.data, originalData);
        console.log("initialied metadata annotation ", this);
    }
    
    crowdsourcing.Annotation.Metadata.prototype = Object.create(crowdsourcing.Annotation.prototype);
    
    crowdsourcing.Annotation.Metadata.prototype.getValue = function(field) {
            let value = this.body.data[field];
            if(!value) {
                return "";
            } else if(Array.isArray(value)) {                
                return value.join("; ");
            } else {
                return value;
            }
    }
        
    crowdsourcing.Annotation.Metadata.prototype.setValue = function(field, value) {
        if(this.body.data[field] != value) {                
            this.setModified(new Date());
        }
        this.body.data[field] = [value];
    }
    
    crowdsourcing.Annotation.Metadata.prototype.getFields = function() {
        return Object.keys(this.body.data);
    }

    /**
     * annotation is empty if body has no fields apart from id, context, type and format
     */
    crowdsourcing.Annotation.Metadata.prototype.isEmpty = function(text) {
        return this.getFields().length == 0;
    }
    
    
    return crowdsourcing;
    
} )( Crowdsourcing );
