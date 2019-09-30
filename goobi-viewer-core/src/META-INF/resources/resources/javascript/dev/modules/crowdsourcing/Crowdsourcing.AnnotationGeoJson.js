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


    crowdsourcing.Annotation.GeoJson = function(annotation) {
        crowdsourcing.Annotation.call(this, annotation);
 
    }
    crowdsourcing.Annotation.GeoJson.prototype = Object.create(crowdsourcing.Annotation.prototype);
    
    crowdsourcing.Annotation.GeoJson.prototype.getLocation = function() {
        return this.body;
     
    }
    
    crowdsourcing.Annotation.GeoJson.prototype.setBody = function(geoJson) {
        this.body = geoJson;
    }
        
    crowdsourcing.Annotation.GeoJson.prototype.setGeometry = function(geometry) {
        if(!this.body) {            
            this.body = {
                    geometry : geometry
            }
        } else {
            this.body.geometry = geometry;
        }
    }
    
    crowdsourcing.Annotation.GeoJson.prototype.setView = function(view) {
        if(!this.body) {            
            this.body = {
                    view : view
            }
        } else {
            this.body.view = view;
        }
    }
    
    crowdsourcing.Annotation.GeoJson.prototype.setName = function(name) {
        if(!this.body) {            
            this.body = {
                    geometry : {},
                    view : {},
                    properties : {
                        name : name
                    }
            }
        } else if(!this.body.properties){
            this.body.properties = {
                    name : name
            }
        } else {
            this.body.properties.name = name;
        }
    }

    
    
    return crowdsourcing;
    
} )( Crowdsourcing );
