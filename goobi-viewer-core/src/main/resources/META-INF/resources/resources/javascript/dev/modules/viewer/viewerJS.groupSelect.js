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
 * @description This module enables selection and deselecion of groups of checkbox elements
 * @version 3.4.0
 * @module viewerJS.jsfAjax
 * @requires jQuery
 */
var viewerJS = ( function ( viewer ) {
    'use strict';

    var _debug = false;
    var _defaults = {
            groupAttribute: "data-group",
            selectAllSelector: "[data-group-select='select-all']",
            selectOneSelector: "[data-group-select='select-one']"            
    };

    viewer.groupSelect = {

        init: function( config ) {
            this.config = $.extend( true, {}, _defaults, config );
            if(_debug) {
                console.log("Init group select with config ", this.config);
            }
            
            $(this.config.selectAllSelector).each((index, groupSelectCheckbox) => {
                if(_debug)console.log("found select all checkbox", groupSelectCheckbox);
                let $groupSelectCheckbox = $(groupSelectCheckbox);
                let groupName = $(groupSelectCheckbox).attr(this.config.groupAttribute);
                if(_debug)console.log("Set up selection group " + groupName);
                if(groupName) {
                    let groupSelector = "[" + this.config.groupAttribute + "='" + groupName + "']";
                    let $singleSelects = $(groupSelector + this.config.selectOneSelector)
                    if(_debug)console.log("add change event to ", $groupSelectCheckbox);
                    $groupSelectCheckbox.on("change", event => {
                        console.log("changed ", event.target);
                        let checked = $groupSelectCheckbox.is(":checked");
                        if(_debug)console.log("set all checkboxed to " + checked);
                        $singleSelects.prop('checked', checked).trigger("change");
                    });
                    
                    if(_debug)console.log("add change event to ", $singleSelects);
                    $singleSelects.on("click", event => {
                        let anyNotChecked = $singleSelects.is(':not(:checked)');
                        if(_debug)console.log("anyNotChecked", anyNotChecked);
                        $groupSelectCheckbox.prop('checked', !anyNotChecked);
                    });
                }
            })

            
        }
    }

    return viewer;

} )( viewerJS || {}, jQuery );