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
var Crowdsourcing = ( function() {
    'use strict';
    
    var _debug = false; 
    
    var crowdsourcing = {};
    
    crowdsourcing.isString = function(variable) {
        return typeof variable === 'string' || variable instanceof String
    }
    
    crowdsourcing.deepCopy = function(obj) {
        return JSON.parse(JSON.stringify(obj));
    }
    
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
    
    crowdsourcing.initTranslations = function(keys, restApiUrl) {
        if(crowdsourcing.isString(keys)) {
            keys = [keys];
        }
        let keyList = keys.join("$");
        let url = restApiUrl + "messages/translate/" + keyList;
        return fetch(url)
        .then( response => response.json() )
        .then( function(json) {
            crowdsourcing.translations = json;
        })
    }
    
    crowdsourcing.translate = function(key, language) {
        if(!language) {
            language = "de";
        }
        if(!crowdsourcing.translations) {
            throw "Must call 'initTranslations' before translating"
        }
        if(!crowdsourcing.translations[key]) {
            throw "message key " + key + " not initialized";
        }
        return viewerJS.getMetadataValue(crowdsourcing.translations[key], language);

    }

    return crowdsourcing;
    
} )( jQuery );
