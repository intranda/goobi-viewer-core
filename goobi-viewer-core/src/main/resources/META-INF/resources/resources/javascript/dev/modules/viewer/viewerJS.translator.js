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
 * Module to manage the data table features.
 * 
 * @version 3.2.0
 * @module viewerJS.translator
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    

    viewer.translator = function(restApiUrl, defaultLanguage) {
        this.keys = [];
        this.restApiUrl = restApiUrl;
        this.language = defaultLanguage
    }
    
    /**
     * Fetches a list of translations for all given message keys. 
     * These are then returned for the keys when calling translator.translate()
     * 
     * @param keys  a list of message key strings to translate
     * @param restApiUrl    The base url to the viewer rest api to use
     * @defaultLanguage the language to be used as default
     * 
     */
    viewer.translator.prototype.init = function(keys) {
        return this.addTranslations(keys);
    }
    
    viewer.translator.prototype.addTranslations = function(keys) {
        if(keys == undefined) {
            return Promise.reject("No keys given to translate");
        }
        if(viewer.isString(keys)) {
            keys = [keys];
        }
        keys = keys.filter(key => key != undefined && key.length > 0).filter(key => this.keys && !this.keys.includes(key));
        if(keys && keys.length > 0) {  
            let keyList = keys.join(",");
            let url = this.restApiUrl + "localization/translations?keys=" + keyList;
            return fetch(url)
            .then( response => response.json() )
            .then( function(json) {
                if(this.translations) {
                    this.translations = $.extend(true, this.translations, json);
                } else {                
                    this.translations = json;
                }
                this.keys = this.keys ? this.keys.concat(keys) : keys;
            }.bind(this))
            .catch(error => {
                console.error("Error fetching " + url + ": " + error);
                this.translations = {};
            });
        } else {
            return Promise.resolve();
        }
    }

    
    /**
     * Returns a translation for the given message key in the given language. 
     * If language is undefined, the language property of Crowdsourcing is used
     * if no translation was found, the key itself is returned
     * Requires the method Crowdsourcing.initTranslations() to be called first 
     */
    viewer.translator.prototype.translate = function(key, language) {
        if(!language) {
            language = this.language;
        }
        if(viewer.isString(key)) {            
            if(!this.translations) {
                console.warn("Must call 'initTranslations' before translating");
                return key;
            }
            if(!this.translations[key]) {
                console.warn("message key " + key + " not initialized");
                return key;
            }
            let translation = viewerJS.getMetadataValue(this.translations[key], language);
            if(!translation) {
                translation = viewerJS.getMetadataValue(this.translations[key], this.fallbackLanguage);
            }
            return translation;
        } else {
            let translation = viewerJS.getMetadataValue(key, language);
            if(!translation) {
                translation = viewerJS.getMetadataValue(key, this.language);
            }
            return translation;
        }

    }
        
    return viewer;
    
} )( viewerJS || {}, jQuery );
