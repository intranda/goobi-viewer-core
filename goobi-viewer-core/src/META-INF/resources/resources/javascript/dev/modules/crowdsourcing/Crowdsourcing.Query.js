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

    crowdsourcing.Query =  {
            Type : {},
            Frequency: {},
            Selector: {}
        }
    
    crowdsourcing.Query.Type.PLAINTEXT = "PLAINTEXT";
    crowdsourcing.Query.Type.DATE_PICKER = "DATE_PICKER";
    crowdsourcing.Query.Type.GEOLOCATION_POINT = "GEOLOCATION_POINT";
    crowdsourcing.Query.Type.GEOLOCATION_AREA = "GEOLOCATION_AREA";
    crowdsourcing.Query.Type.TRANSCRIPTION = "TRANSCRIPTION";
    crowdsourcing.Query.Type.KEY_VALUE_LIST = "KEY_VALUE_LIST";
    crowdsourcing.Query.Type.get = function(text) {
        return crowdsourcing.Query.Type[text];
    }
    crowdsourcing.Query.getType = function(query) {
        return crowdsourcing.Query.Type.get(query.queryType);
    }
    
    crowdsourcing.Query.Frequency.ONE_PER_MANIFEST = "ONE_PER_MANIFEST";
    crowdsourcing.Query.Frequency.MULTIPLE_PER_MANIFEST = "MULTIPLE_PER_MANIFEST";
    crowdsourcing.Query.Frequency.ONE_PER_CANVAS = "ONE_PER_CANVAS";
    crowdsourcing.Query.Frequency.MULTIPLE_PER_CANVAS = "MULTIPLE_PER_CANVAS";
    crowdsourcing.Query.Frequency.get = function(text) {
        return crowdsourcing.Query.Frequency[text];
    }
    crowdsourcing.Query.getFrequency = function(query) {
        return crowdsourcing.Query.Frequency.get(query.targetFrequency);
    }
    
    crowdsourcing.Query.Selector.MULTIPLE_PER_CANVAS = "WHOLE_SOURCE";
    crowdsourcing.Query.Selector.WHOLE_PAGE = "WHOLE_PAGE";
    crowdsourcing.Query.Selector.RECTANGLE = "RECTANGLE";
    crowdsourcing.Query.Selector.get = function(text) {
        return crowdsourcing.Query.Selector[text];
    }
    crowdsourcing.Query.getSelector = function(query) {
        return crowdsourcing.Query.Selector.get(query.targetSelector);
    }
    
    return crowdsourcing;
    
} )( Crowdsourcing );
