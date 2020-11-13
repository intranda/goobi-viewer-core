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

    crowdsourcing.Annotation.AuthorityResource = function(annotation, context) {
        crowdsourcing.Annotation.call(this, annotation);
        if(!this.body) {
            this.body = {
                    type: "AuthorityResource",
                    "@context": context
            }
        }
        console.log("new authority resource ", this.body);
    }
    crowdsourcing.Annotation.AuthorityResource.prototype = Object.create(crowdsourcing.Annotation.prototype);
    
    crowdsourcing.Annotation.AuthorityResource.prototype.getId = function() {
            return this.body.id;
    }
        
    crowdsourcing.Annotation.AuthorityResource.prototype.setId = function(uri) {
        if(this.body.id && this.body.id != uri) {                
            this.setModified(new Date());
        }
        this.body.id = uri;
    }

    crowdsourcing.Annotation.AuthorityResource.prototype.isEmpty = function() {
        if(this.getId() && this.getId().length > 0) {
            return false;
        } else {
            return true;
        }
    }
    
    
    return crowdsourcing;
    
} )( Crowdsourcing );
