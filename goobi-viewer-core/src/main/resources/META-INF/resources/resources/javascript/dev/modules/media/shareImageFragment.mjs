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
 */

export default class ShareImageFragment {
    
    constructor(image) {
        this.active = false;
        this.init(image);
        this.initAreaFromFragmentHash();
    }
    
    init(image) {
        this.image = image;
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
                 drawCondition: event => this.active && this.fragmentSelect?.currentOverlay == undefined,
                 transformCondition: event => this.active,
                 drawStyle : {
                     borderColor: styles["borderTopColor"],
                     borderWidth: parseInt(styles["borderTopWidth"]),
                     fillColor: styles["background-color"],
                     className: "image-fragment"
                 }
            };
            this.fragmentSelect = new ImageView.AreaSelect(image.viewer, fragmentSelectConfig);
        } catch(error) {
            console.error("Error initializing area select: ", error);
        }
        this.toggleImageShare($(".share-image-area h3"));
        this.$fragmentSelectButton.on("shown.bs.popover", () => this.startFragmentSelect());
        this.$fragmentSelectButton.on("hidden.bs.popover", () => this.endFragmentSelect());
    }

    startFragmentSelect() {
        this.$fragmentSelectButton.tooltip("hide");
        this.$fragmentSelectButton.addClass("active");
        this.active = true;

        if(this.fragmentSelect) {            
            this.fragmentSelect.finishedHook.subscribe( area => {
                var areaString = this.getAreaString(area);
                var pageUrl = window.location.origin + window.location.pathname +  window.location.search + "#xywh=" + areaString;
                var imageUrl = this.getRegionUrl(area);
                console.log("set area data ", pageUrl, imageUrl)
                $('[data-fragment-link="page"]').attr("data-copy-share-image", pageUrl);
                $('[data-fragment-link="iiif"]').attr("data-copy-share-image", imageUrl);
                console.log("area share ",  $('[data-fragment-link="iiif"]'));
                this.$links.show();
                this.$instructions.hide();
                this.initImageFragmentLinks(areaString);
            });
        }
    }

    getAreaString(area) {
         if(area && area.x != undefined && area.y != undefined && area.width != undefined && area.height != undefined) {             
             var areaString = area.x.toFixed(0) + "," + area.y.toFixed(0) + "," + area.width.toFixed(0) + "," + area.height.toFixed(0);
             return areaString;
         } else {
             return "full";
         }
     }

     getAreaFromString(string) {
        const parts = string.split(",").filter(s => s.match(/^\d+$/)).map(s => Number(s));
        if(parts.length == 4) {
            return  {
                x: parts[0],
                y: parts[1],
                width: parts[2],
                height: parts[3]
            }
        }
     }

     getRegionUrl(rect) {
         let areaString, tileSource;
         
         tileSource = image.getCurrentTileSource()
         if (typeof rect === 'string' || rect instanceof String) {  
             areaString = rect;
         } else {
             const imageBounds = this.image.viewer.getCurrentImage().getBounds();
             rect = rect.translate(imageBounds.getTopLeft().times(-1)); //substract the image position
             areaString = this.getAreaString(rect);
         }
         let imageUrl = tileSource["@id"];
         imageUrl = imageUrl + "/" + areaString + "/max/" + this.image.viewer.getRotation() + "/default.jpg";
         return imageUrl;
     }
    
    endFragmentSelect() {
        this.active = false;
        this.$fragmentSelectButton.removeClass("active");
        if(this.fragmentSelect) {
            this.fragmentSelect.transformer?.close();
            this.fragmentSelect.removeOverlays();
        }
        this.$links.hide();
        this.hideImageFragmentLinks();
        this.$instructions.show();
    }

    initAreaFromFragmentHash() {
        const fragment = viewerJS.helper.getFragmentHash();
        if(fragment && this.fragmentSelect) {
            const area = this.getAreaFromString(fragment);
            if(area) {
                this.fragmentSelect.draw(area)
                this.initImageFragmentLinks(fragment);
            }
        }
    }
    
    initImageFragmentLinks(fragment) {
        let $wrapper = $('[data-fragment-link="wrapper"]');

        if(fragment) {
            var pageUrl = window.location.origin + window.location.pathname + "#xywh=" + fragment;
            var imageUrl = this.getRegionUrl(fragment);
            $wrapper.find('[data-fragment-link="page"]').attr("data-copy-share-image", pageUrl);
            $wrapper.find('[data-fragment-link="iiif"]').attr("data-copy-share-image", imageUrl);
			// ACTIVATE COPY TO CLIPBOARD 
			viewerJS.clipboard.init('[data-copy-share-image]', 'data-copy-share-image'); 
            $wrapper.show();
        }
    }
    
    hideImageFragmentLinks() {
        let $wrapper = $('[data-fragment-link="wrapper"]');
        $wrapper.hide();
    }
    

    toggleImageShare($panel) {
        if($panel.closest(".fullscreen__view-sidebar-accordeon-panel").hasClass("share-image-area") && $panel.hasClass("in")) {
            this.startFragmentSelect();
        } else {
//            this.endFragmentSelect();
        }
    }
}
