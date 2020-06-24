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
 * Creates a visible popup by attaching a copy of the given <popup> Element to the body and offsetting it to the appropriate position
 * The popup closes on a click anywhere outside of it, removing its associated eventhandlers in the process
 * 
 * @version 3.2.0
 * @module viewerJS.popovers
 * @requires jQuery
 * 
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    const _debug = false;
    
    const _triggerElementSelector = "[data-popover-element]";
    const _popoverSelectorAttribute = "data-popover-element";
    const _popoverTitleAttribute = "data-popover-title";
    const _popoverPlacementAttribute = "data-popover-placement";
    const _popoverTemplate = "<div class='popover' role='tooltip'><div class='arrow'></div><h3 class='popover-title-custom'>#{title}</h3><div class='popover-body'></div></div>";    
    const _popoverTemplateNoTitle = "<div class='popover' role='tooltip'><div class='arrow'></div><div class='popover-body'></div></div>";    
    const _dismissPopoverAttribute = "data-popover-dismiss";
    const _clickOutside = "click-outside";
    const _popoverOnShowAttribute = "data-popover-onshow";
    const _popoverOnCloseAttribute = "data-popover-onclose";
    const _popoverWidth = 250; //rounded max-width of bootstrap-popovers
    
    viewer.popovers = {
            
            init: function() {
                $(_triggerElementSelector).each( (index, element) => {
                    
                    let $element = $(element);
                    let popoverSelector = $element.attr(_popoverSelectorAttribute);
                    let $popover = $(popoverSelector);
                    if($popover.length === 1) {
                        this.initPopover($element, $popover);
                    } else if($popover.length > 1) {
                        console.error("Found more than one popover matching selector '" + popoverSelector + "'. Cannot initialize popover");
                    } else {
                        console.error("Found no popover matching selector '" + popoverSelector + "'. Cannot initialize popover");
                    }
                })
            },
            
            initPopover: function($trigger, $popover) {
                
                //add manual show shandler
                $trigger.on("click", (event) => {
                    $trigger.blur();
                    $trigger.popover("toggle");
                })
                
                //add dismiss handler if configured
                if($trigger.attr(_dismissPopoverAttribute) === _clickOutside) {
                    $trigger.on("shown.bs.popover", (event) => {
                        this.addCloseHandler($trigger);
                    });
                }
                
                let config = _createPopoverConfig($trigger, $popover);
                
                $popover.find("[data-popover='close']").on("click", () => {
                    $trigger.popover("hide");
                })
                
                if(config.onShow) {
                    $trigger.on("shown.bs.popover", config.onShow);
                }
                
                if(config.onClose) {
                    $trigger.on("hidden.bs.popover", config.onClose);
                }
                
                $trigger.popover(config);
                console.log("init popover done", config);
            },
            
            fromEvent(anchor, event, popoverSelector, config) {
                if(_debug) {                    
                    console.log("Popovers.fromEvent ", event);
                    console.log("Popovers.fromEvent ", popoverSelector);
                    console.log("Popovers.fromEvent ", config);
                }
                
                if(config == undefined) {
                    config = {};
                }

                config.html = true;
                config.content = $(popoverSelector).get(0);
                config.trigger = "manual";
                if(config.title != undefined && config.title.length == 0) {
                    config.template = _popoverTemplateNoTitle;
                }
                
                
                let popover = $(anchor).popover(config)
                .one("shown.bs.popover", function() {
                    viewer.popovers.addCloseHandler($(anchor))
                    let $wrapper = $(popoverSelector).closest(".popover");
                    let $arrow = $wrapper.find(".arrow");
                    $wrapper.offset({top:0,left:0});

                    $wrapper.offset({
                        top: event.pageY - Math.ceil($wrapper.height()/2) + ( (config.offset && config.offset.top) ? config.offset.top : 0),
                        left: event.pageX + $arrow.outerWidth() + ( (config.offset && config.offset.left) ? config.offset.left : 0)
                    })
                    
                    $(popoverSelector).find("[data-popover='close']").on("click", () => {
                        $(anchor).popover("hide");
                    })
                    
                    $arrow.css("top", "50%");
                    if(config.onShow) {
                        config.onShow();
                    }
                })
                .one("hide.bs.popover", function() {
                    $(popoverSelector).find("[data-popover='close']").off("click");
                    if(config.onClose) {
                        config.onClose();
                    }
                })
                .popover("show");
                                
                return popover;
            },
            
            addCloseHandler : function($trigger) {
                $('body').on("click.popover", event => {
                    if($(event.target).closest("popover").length == 0) {
                        $trigger.popover("hide");
                        $('body').off("click.popover");
                    }
                    
                });
            }
            
    }
    
    function _createPopoverConfig($trigger, $popover) {
        
        let config = {
                html: true,
                content: $popover.get(0),
                trigger: "manual"
        } 
        
        let placement = $trigger.attr(_popoverPlacementAttribute);
        if(placement) {
            config.placement = placement;
        } else {
            //default placement is right. But without sufficient space, place the popover left
            let windowRight = $(window).width();
            let triggerPos = $trigger.offset().left;
            let remainingSpace = windowRight - triggerPos - _popoverWidth;
            config.placement = remainingSpace < 0 ? "left" : "right";
        }
        
        let onShow = $trigger.attr(_popoverOnShowAttribute);
        if(onShow) {
            config.onShow = viewerJS.helper.getFunctionByName(onShow, window);
        }
        
        let onClose = $trigger.attr(_popoverOnCloseAttribute);
        if(onClose) {
            config.onClose = viewerJS.helper.getFunctionByName(onClose, window);
        }
       
        
        let title = $trigger.attr(_popoverTitleAttribute);
        if(title != undefined) {
            if(title.length) {                        
                config.template = _popoverTemplate.replace("#{title}", title);
            } else {
                config.template = _popoverTemplateNoTitle;
            }
        }
        return config;
    }
    

    
    return viewer;
    
} )( viewerJS || {}, jQuery );
