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
 * @description Base-Module for all crowdsourcing annotate and review views
 * @version 3.7.0
 * @module Crowdsourcing.js
 */
var Crowdsourcing = ( function() {
    'use strict';
    
    var _debug = false; 
    
    var crowdsourcing = {};
    
    crowdsourcing.language = "de";
    crowdsourcing.fallbackLanguage = "en";
    
    /**
     * Check if a variable is a string
     */
    crowdsourcing.isString = function(variable) {
        return typeof variable === 'string' || variable instanceof String
    }
    
    /**
     * @return a deep copy of the given object 
     */
    crowdsourcing.deepCopy = function(obj) {
        return JSON.parse(JSON.stringify(obj));
    }
    
    /**
     * @return the identifier url for any json+ld object. Returns the original object if it was a string
     */
    crowdsourcing.getResourceId = function(resource) {
        if(crowdsourcing.isString(resource)) {
            return resource;
        } else if(resource.source) {
            return resource.source;
        } else if(resource.id) {
            return resource.id;
        } else if(resource["@id"]) {
            return resource["@id"];
        } else {
            return JSON.stringify(resource);
        }
    }
    
    /**
     * Set the colors to use for drawing frames on the image. All questions use the same color palette.
     * This may either be a list of color codes or an object returning a color on calling its next() method
     */
    crowdsourcing.setFrameColors = function(colors) {
        this.frameColors = colors;
    }

    
    /**
     * Returns a translation for the given message key in the given language. 
     * If language is undefined, the language property of Crowdsourcing is used
     * if no translation was found, the key itself is returned
     * Requires the method Crowdsourcing.initTranslations() to be called first 
     */
    crowdsourcing.translate = function(key, language) {
       if(!crowdsourcing.translator) {
           return key;
       } else {
           return crowdsourcing.translator.translate(key, language);
       }
    }
    
    crowdsourcing.loadTranslations = function(keys) {
        if(crowdsourcing.translator) {            
            return crowdsourcing.translator.addTranslations(keys);
        } else {
            throw "Translator not initialized";
        }
    }

    return crowdsourcing;
    
} )( jQuery );
