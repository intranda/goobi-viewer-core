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
 * Module to select a region in a openseadragon image and supply share links for it
 * 
 * @version 4.6.0
 * @module viewerJS.shareImageFragment
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
  
    
    viewer.ShareImageFragment = function(image) {
        this.init(image);
    }
    
    viewer.ShareImageFragment.prototype.startFragmentSelect = function() {
        this.$fragmentSelectButton.tooltip("hide");
        this.$fragmentSelectButton.addClass("active");
        
        if(this.fragmentSelect) {            
            this.fragmentSelect.startSelect().subscribe( area => {
                var areaString = viewImage.getAreaString(area);
                var pageUrl = window.location.origin + window.location.pathname +  window.location.search + "#xywh=" + areaString;
                var imageUrl = viewImage.getRegionUrl(area);
                $("[data-copy-share-image='image-region-page']").attr("data-copy-share-image", pageUrl);
                $("[data-copy-share-image='image-region-image']").attr("data-copy-share-image", imageUrl);
                this.$links.show();
                this.$instructions.hide();
                this.initImageFragmentLinks(areaString);
            });
        }
    },
    
    viewer.ShareImageFragment.prototype.endFragmentSelect = function() {
        this.$fragmentSelectButton.removeClass("active");
        if(this.fragmentSelect) {
            this.fragmentSelect.remove(0);
            this.fragmentSelect.stopSelect();
        }
        this.$links.hide();
        this.hideImageFragmentLinks();
        this.$instructions.show();
    },

    viewer.ShareImageFragment.prototype.init = function(image) {
        
        this.$fragmentSelectButton = $(".share-image-region a");
        this.$links = $(".share-image-area__links");
        this.$instructions = $(".share-image-area__instructions");
        
        this.$links.hide();
        
        //fullscreen controls
        let $panels = $(".fullscreen__view-sidebar-accordeon-panel h3");
        $panels.on("click", (e) => {
            let $panel = $(e.target).closest(".fullscreen__view-sidebar-accordeon-panel h3");
            this.toggleImageShare($panel);
        });
        $(".share-image-area [data-popover='close']").on("click", (e) => {
            this.endFragmentSelect();
            $(e.target).closest(".fullscreen__view-sidebar-accordeon-panel").find("h3").click();
            $(e.target).closest(".fullscreen__view-sidebar-accordeon-panel").find("h3").focus();
        })
        
        // init area select
        try { 
            let styles = viewerJS.helper.getCss("image-fragment", ['borderTopColor', 'borderTopWidth', 'background-color']);
            var fragmentSelectConfig = {
                 removeOldAreas : true,
                 drawStyle : {
                     borderColor: styles["borderTopColor"],
                     borderWidth: parseInt(styles["borderTopWidth"]),
                     fillColor: styles["background-color"],
                 }
            };
            this.fragmentSelect = new ImageView.Tools.AreaSelect(image, fragmentSelectConfig);
        } catch(error) {
            console.error("Error initializing area select: ", error);
        }
        this.toggleImageShare($(".share-image-area h3"));
        this.$fragmentSelectButton.on("shown.bs.popover", () => this.startFragmentSelect());
        this.$fragmentSelectButton.on("hidden.bs.popover", () => this.endFragmentSelect());
    }
    
    viewer.ShareImageFragment.prototype.initImageFragmentLinks = function(fragment) {
        let $wrapper = $(".widget-usage__image-fragment-wrapper");
        if(!fragment) {                 
            fragment = viewerJS.helper.getFragmentHash();
        }
        if(fragment) {
            var pageUrl = window.location.origin + window.location.pathname + "#xywh=" + fragment;
            var imageUrl = viewImage.getRegionUrl(fragment);
            $wrapper.find(".widget-usage__image-fragment-page").attr("data-copy-share-image", pageUrl);
            $wrapper.find(".widget-usage__image-fragment-image").attr("data-copy-share-image", imageUrl);
			// ACTIVATE COPY TO CLIPBOARD
			viewerJS.clipboard.init('[data-copy-share-image]', 'data-copy-share-image'); 
            $wrapper.show();
        }
    }
    
    viewer.ShareImageFragment.prototype.hideImageFragmentLinks = function() {
        let $wrapper = $(".widget-usage__image-fragment-wrapper");
        $wrapper.hide();
    }
    

    viewer.ShareImageFragment.prototype.toggleImageShare = function($panel) {
        if($panel.closest(".fullscreen__view-sidebar-accordeon-panel").hasClass("share-image-area") && $panel.hasClass("in")) {
            this.startFragmentSelect();
        } else {
//            this.endFragmentSelect();
        }
    }

	



    return viewer;
} )( viewerJS || {}, jQuery );

