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
 * @module viewerJS.popups
 * @requires jQuery
 * @requires leaflet
 * 
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    const _debug = false;

    const _anchoring = {
            top: "bottom", //top,middle,bottom
            left: "right", //left,center,right
            bottom: undefined, //top,middle,bottom
            right: undefined, //left,center,right
            middle: undefined, //top,middle,bottom
            center: undefined, //left,center,right
            offset: {
                left: 0,
                top: 0
            }
    }

    /**
     * Creates a new popup
     * @param anchor    The DOM element at which to anchor the popup; by default the popup is placed at the bottom right corner of the anchor
     * @param popup     The <popup>-Element to show as popup. If none is given, the next <popup>-Element in the DOM-tree after the anchor is used
     * @param anchoring Additional options to anchor the popup
     * 
     */
    viewer.Popup = function(anchor, popup, anchoring) { 
        
        if(anchor.target) {
            this.event = anchor;
        } else if(anchor) {            
            this.$anchor = $(anchor);
        }

        if(!anchoring) {
            anchoring = {};
        }
        this.anchoring = $.extend({}, _anchoring, anchoring);
        if(popup) {    
            this.$popup = $(popup).clone();
        } else if(!anchor){
            this.$popup = this.$anchor.next("popup").clone();
        } else {
            this.$popup = $("popup").clone(); 
        }
        this.onClose = new Rx.Subject();

        
        if ( _debug ) {
            console.log( '##############################' );
            console.log( 'viewer.Popup' );
            console.log( '##############################' );
            console.log( 'viewer.Popup popup',  this.$popup);
            console.log( 'viewer.Popup anchor',  this.$anchor ? this.$anchor : this.event);
            console.log( 'viewer.Popup anchoring', this.anchoring);
            console.log( '##############################' );

        }
        
        this.$popup.appendTo($("body"));
        this.$popup.offset(this.calcOffset());
        this.$popup.show();
        //setTimeout to wait until click event is completely handled        
        setTimeout( () => this.addCloseHandler(), 0);
    }
       
    viewer.Popup.prototype.addCloseHandler = function() {
        $('body').on("click.popup", this.close.bind(this));
    }

    viewer.Popup.prototype.close = function(event) {
            if(!event || $(event.target).closest("popup").length == 0) {
                this.$popup.off();
                this.$popup.remove();
                $('body').off("click.popup");
                this.onClose.onNext(event);
            }
    }
    
    viewer.Popup.prototype.calcOffset = function() {
        let offset = {};
        let anchorPos = {top: 0, left: 0};
        let anchorSize = {width: 0, height: 0};
        if(this.event) {
            anchorPos.top = this.event.pageY;
            anchorPos.left = this.event.pageX;
        } else if(this.$anchor) {            
            anchorPos = this.$anchor.offset();
            anchorSize.width = this.$anchor.innerWidth();
            anchorSize.height = this.$anchor.innerHeight();
        }
        let popupSize = {width: this.$popup.outerWidth(), height:this.$popup.outerHeight()}
        
        if(this.anchoring.bottom) {
            switch(this.anchoring.bottom) {
                case "top": offset.top = anchorPos.top - popupSize.height; break;
                case "center" : offset.top = anchorPos.top + anchorSize.height/2 - popupSize.height; break;
                case "bottom" : offset.top = anchorPos.top + anchorSize.height - popupSize.height; break;
            }
        } else if(this.anchoring.middle) {
            switch(this.anchoring.middle) {
                case "top": offset.top = anchorPos.top - popupSize.height/2; break;
                case "middle" : offset.top = anchorPos.top + anchorSize.height/2 - popupSize.height/2; break;
                case "bottom" : offset.top = anchorPos.top + anchorSize.height - popupSize.height/2; break;
            }
        } else {            
            switch(this.anchoring.top) {
                case "top": offset.top = anchorPos.top; break;
                case "middle" : offset.top = anchorPos.top + anchorSize.height/2; break;
                case "bottom" : offset.top = anchorPos.top + anchorSize.height; break;
            }
        }
        

        if(this.anchoring.right) {
            switch(this.anchoring.right) {
                case "left": offset.left = anchorPos.left - popupSize.width; break;
                case "center" : offset.left = anchorPos.left + anchorSize.width/2 - popupSize.width; break;
                case "right" : offset.left = anchorPos.left + anchorSize.width - popupSize.width; break;
            }
        } else if(this.anchoring.center) {
            switch(this.anchoring.center) {
                case "left": offset.left = anchorPos.left - popupSize.width/2; break;
                case "center" : offset.left = anchorPos.left + anchorSize.width/2 - popupSize.width/2; break;
                case "right" : offset.left = anchorPos.left + anchorSize.width - popupSize.width/2; break;
            }
        } else {            
            switch(this.anchoring.left) {
                case "left": offset.left = anchorPos.left; break;
                case "center" : offset.left = anchorPos.left + anchorSize.width/2; break;
                case "right" : offset.left = anchorPos.left + anchorSize.width; break;
            }
        }

        offset.top += this.anchoring.offset.top;
        offset.left += this.anchoring.offset.left;
        if(_debug) {
            console.log("offset ", offset, anchorPos, anchorSize, popupSize);
        }
        return offset;
    }
    

    
    return viewer;
    
} )( viewerJS || {}, jQuery );
