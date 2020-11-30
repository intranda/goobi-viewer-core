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


    crowdsourcing.Annotation.Plaintext = function(annotation) {
        crowdsourcing.Annotation.call(this, annotation);
        if(!this.body) {
            this.body = {
                    type: "TextualBody",
                    format: "text/plain",
                    value: "",
            }
        }
    }
    
    crowdsourcing.Annotation.Plaintext.prototype = Object.create(crowdsourcing.Annotation.prototype);
    
    crowdsourcing.Annotation.Plaintext.prototype.getText = function() {
            return this.body.value;

    }
        
    crowdsourcing.Annotation.Plaintext.prototype.setText = function(text) {
        if(this.body.value != text) {                
            this.setModified(new Date());
        }
        this.body.value = text;
    }

    crowdsourcing.Annotation.Plaintext.prototype.isEmpty = function(text) {
        if(this.getText() && this.getText().length > 0) {
            return false;
        } else {
            return true;
        }
    }
    
    
    return crowdsourcing;
    
} )( Crowdsourcing );
