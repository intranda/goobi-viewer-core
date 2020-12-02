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

    var _debug = true;
    var _defaults = {
            groupAttribute: "data-group",
            selectAllSelector: "[data-group-select='select-all']",
            selectOneSelector: "[data-group-select='select-one']",
            reseter: undefined
                
    };

    viewer.groupSelect = {

        checkObservables: new Map(),
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
                    this.initGroup(groupName, $groupSelectCheckbox);
                }
            });
            
            if(this.config.reseter) {
                $(this.config.reseter).on("click", e => {
                    if(_debug)console.log("group select reset by ", e.target);
                    $(this.config.selectAllSelector).prop("checked", false);
                    $(this.config.selectOneSelector).prop("checked", false);
                })
            }
        },
        
        initGroup: function(groupName) {
            let groupSelectObservable = new rxjs.Subject();
            rxjs.fromEvent(document, "click")
            .pipe(rxjs.operators.filter(e => {
                return e.target.matches(this.getSelectAllSelector(groupName));
            }))
            .subscribe(e => {
                let checked = $(e.target).is(":checked");
                $(this.getSelectOneSelector(groupName)).prop('checked', checked);
                e.allChecked = checked;
                e.anyChecked = checked;
                groupSelectObservable.next(e);
            })
            rxjs.fromEvent(document, "click")
            .pipe(rxjs.operators.filter(e => {
                return e.target.matches(this.getSelectOneSelector(groupName));
            }))
            .subscribe(e => {
                let checked = $(e.target).is(":checked");
                let numChecked = $(this.getSelectOneSelector(groupName) + ":checked").length;
                let numTotal = $(this.getSelectOneSelector(groupName)).length;
                if(_debug)console.log(numChecked + " out of " + numTotal + " checkboxed checked");
                e.allChecked = numChecked == numTotal;
                e.anyChecked = numChecked > 0;
                $(this.getSelectAllSelector(groupName)).prop('checked', e.allChecked)
                groupSelectObservable.next(e);
            })
            this.checkObservables.set(groupName, groupSelectObservable);

        },
        
        getGroupSelector: function(groupName) {
            let groupSelector = "[" + this.config.groupAttribute + "='" + groupName + "']";
            return groupSelector;
        },
        getSelectAllSelector: function(groupName) {
            let selector = this.getGroupSelector(groupName) + this.config.selectAllSelector;
            return selector;
        },
        getSelectOneSelector: function(groupName) {
            let selector = this.getGroupSelector(groupName) + this.config.selectOneSelector;
            return selector;
        },
        
        onChange: function(groupName) {
            return this.checkObservables.get(groupName);
        }
    }

    return viewer;

} )( viewerJS || {}, jQuery );