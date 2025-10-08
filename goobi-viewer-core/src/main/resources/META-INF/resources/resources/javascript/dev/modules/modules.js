(function () {
    'use strict';

    const _config$1 = {
        styleclass: "imageview-overlay",
        showTooltip: false
    };

    class ZoomableImageOverlayGroup {

        /**
         * @param {ZoomableImage} image 
         * @param {Array} overlaySources 
         * @param {Object} config
         */
        constructor(image, overlaySources, config) {

            this.config = jQuery.extend(true, {}, _config$1, config);

            this.overlayGroup = new ImageView.OverlayGroup(image.viewer, {
                className: this.config.styleclass,
                tooltipClassName:  this.config.styleclass + " tooltip",
                highlightClassName:  this.config.styleclass + " highlight"
            }); 

            this.overlays = createOverlays(overlaySources, image.getCurrentTileSourceId());

        }

        show() {
            this.overlayGroup.addToViewer(this.overlays);
        }

        hide() {
            this.overlayGroup.removeFromViewer(this.overlays);
        }
    }

    function createOverlays(coords, target) {
        const overlays = [];
        if(Array.isArray(coords)) {
            coords.forEach(coord => {
                if(Array.isArray(coord)) {
                    overlays.push({
                        target: target,
                        coordinates: [coord[0], coord[1], coord[2] - coord[0], coord[3] - coord[1]]
                    });
                }
            });
        }
        return overlays;
    }

    const _config = {
        elementSelectors: {
            image: '[data-image="zoomable"]',
            data: {
                tileSource: '[data-image="zoomable"] [data-image-data="tileSource"]',
                record: '[data-image="zoomable"] [data-image-data="recordIdentifier"]',
                structure: '[data-image="zoomable"] [data-image-data="structureIdentifier"]',
                pageType: '[data-image="zoomable"] [data-image-data="pageType"]',
                page: '[data-image="zoomable"] [data-image-data="pageNumber"]',
                footer: '[data-image="zoomable"] [data-image-data="footer"]',
                pageAreas: '[data-image="zoomable"] [data-image-data="pageAreas"]',
                overlays: '[data-image="zoomable"] [data-image-data="overlays"]',
            },
            controls: { 
                rotateLeft: '.rotate-left',
                rotateRight: '.rotate-right',
            	reset: '.reset', 
                zoomSlider: '.zoom-slider'
            }
        },
        datasets: {
            image: {
                viewMode: "imageViewMode",
                showNavigator: "showNavigator",
                allowDownload: "allowDownload",
                allowZoom: "allowZoom"
            },
            data: { 
                footerHeight: "height",
                styleclass: "styleclass",
                showTooltip: "showTooltip"
            }
        }
    };

    class ZoomableImage {

        constructor() {
            console.log("init image view", _config);
            const imageElement = document.querySelector(_config.elementSelectors.image);
            if(imageElement) { 
                
                const imageViewConfig = createZoomableImageConfig(imageElement);
                console.log("create image view with config ", imageViewConfig);
                this.viewer = new ImageView.Image(imageViewConfig);
                this.zoom = new ImageView.Controls.Zoom(this.viewer);
                this.rotation = new ImageView.Controls.Rotation(this.viewer);
                initControls(this.zoom, this.rotation);

                this.footer = createFooter(this.viewer);

                this.tileSources = createTileSource();
                console.log("use TileSources ", this.tileSources);
                this.tileSourceIdToOrder = Object.fromEntries(
                    Object.entries(this.tileSources).map(([order, obj]) => [viewerJS.iiif.getId(obj), order])
                );

                this.pi = document.querySelector(_config.elementSelectors.data.record).textContent;
                this.logId = document.querySelector(_config.elementSelectors.data.structure).textContent;
                this.currentPageNo = document.querySelector(_config.elementSelectors.data.page).textContent;
                this.pageType = document.querySelector(_config.elementSelectors.data.pageType).textContent;
                this.viewMode = imageElement.dataset[_config.datasets.image.viewMode];

                if(this.viewMode == "sequence") {
                    console.log("initialize sequence mode");
                    this.sequence = new ImageView.Sequence(this.viewer, this.zoom);
                }
                
                
                this.overlayGroups = [];
                document.querySelectorAll(_config.elementSelectors.data.overlays).forEach( element => {
                    const coordsString = element.innerHTML;
                    if(coordsString.trim().length > 0) {
                        try {
                            const coords = JSON.parse(coordsString);
        
                            const overlays = new ZoomableImageOverlayGroup(this, coords, {
                                styleclass: element.dataset[_config.datasets.data.styleclass],
                                showTooltip: element.dataset[_config.datasets.data.showTooltip]
                            });
                            this.overlayGroups.push(overlays);
                        } catch(e) {
                            console.error("Error parsing coords string ", coordsString, e);
                        }
                    }
                });

            }
        }
     
        load() {
            return this.viewer.load( Object.values(this.tileSources), this.getCurrentTileSourceIndex() )
            .then(image => {
                this.sequence?.initialize(this.getCurrentTileSourceId());
                this.overlayGroups.forEach(group => group.show());
                return this;
            });
        }

        getCurrentTileSourceIndex() {
            return this.viewer.getImageIndexById(this.getCurrentTileSourceId());
        }

        getCurrentTileSourceOrder() {
            return this.currentPageNo;
        }

        getCurrentTileSource() {
            return this.getTileSourceFromOrder(this.currentPageNo);
        }

        getCurrentTileSourceId() {
            return viewerJS.iiif.getId(this.getCurrentTileSource());
        }

        getTileSourceFromOrder(order) {
            return this.tileSources[order]
        }

        getTileSourceFromId(id) {
            return this.tileSources.values().find(source => viewerJS.iiif.getId(value) == id);
        }

        getTileSourceIdFromOrder(order) {
            return viewerJS.iiif.getId(this.getTileSourceFromOrder(order));
        }

        getTileSourceOrderFromId(id) {
            return this.tileSourceIdToOrder[id];
        }
      
    }

    function createTileSource() {
        const tileSourcesString = document.querySelector(_config.elementSelectors.data.tileSource).textContent;
        if(tileSourcesString) {
            try {
                const tileSources = JSON.parse(tileSourcesString);
                Object.keys(tileSources).forEach((key) => {
                    let value = tileSources[key];
                    if(typeof value == "string" && (value.startsWith("{") || value.startsWith("["))) {
                        tileSources[key] = JSON.parse(value);
                    }
                });
                return tileSources;
            } catch(e) {
                console.error(`Error parsing tileSource "${tileSourcesString}": ${e}`);
            }
        }
    }

    function createFooter(viewer) {
        const footerUrl = document.querySelector(_config.elementSelectors.data.footer).textContent;
        if(viewer.viewportMargins.bottom > 0 && footerUrl) {
            const footer = new ImageView.Footer(viewer, viewer.viewportMargins.bottom);
            footer.load(footerUrl);
        } 
        return footer;
    }

    function initControls(zoom, rotation) {
        zoom.setSlider(_config.elementSelectors.controls.zoomSlider);
        document.querySelectorAll(_config.elementSelectors.controls.rotateLeft).forEach(button => button.addEventListener("click", e => rotation.rotateLeft()));
        document.querySelectorAll(_config.elementSelectors.controls.rotateRight).forEach(button => button.addEventListener("click", e => rotation.rotateRight()));
        document.querySelectorAll(_config.elementSelectors.controls.reset).forEach(button => button.addEventListener("click", e => {
            rotation.rotateTo(0);
            zoom.goHome();
        })); 
    }




    function createZoomableImageConfig(imageElement) {
        return  {
            element: imageElement,
            fittingMode: getFittingMode(document.querySelector(_config.elementSelectors.data.pageType)?.textContent),
            margins: getMargins(document.querySelector(_config.elementSelectors.data.footer)?.dataset[_config.datasets.data.footerHeight], document.querySelector(_config.elementSelectors.data.pageType)?.textContent),
            zoom:  {
                enabled: imageElement.dataset[_config.datasets.image.allowZoom] !== "false"
            },
            sequence: getSequenceSettings(imageElement.dataset[_config.datasets.image.viewMode]),
            navigator: {
                enabled: imageElement.dataset[_config.datasets.image.showNavigator] === "true"
            }
        }
    } 

    function getSequenceSettings(viewMode) {
        switch((viewMode || "").toLowerCase()) {
            case "double": return {
                columns: 2
            };
            case "sequence": return {
                columns: 1
            };
            case "single":
            default: return {
                columns: 1
            };
        }
    } 

    function getFittingMode(pageType) {
        switch((pageType || "").toLowerCase()) {
            case "viewfulltext":
                return "fixed";
            default:
                return "toWidth";
        }
    }

    function getMargins(footerHeight, pageType) {
        return {
            bottom: Number(footerHeight)
        }
    }

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

    class ShareImageFragment {
        
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
            });
            
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
                    console.log("finished drawing or updated area ", area);
                    var areaString = this.getAreaString(area);
                    var pageUrl = window.location.origin + window.location.pathname +  window.location.search + "#xywh=" + areaString;
                    var imageUrl = this.getRegionUrl(area);
                    console.log("set area data ", pageUrl, imageUrl);
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
             
             tileSource = image.getCurrentTileSource();
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
            console.log("initAreaFromFragmentHash", fragment);
            if(fragment && this.fragmentSelect) {
                const area = this.getAreaFromString(fragment);
                if(area) {
                    this.fragmentSelect.draw(area);
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
            }
        }
    }

    window.ShareImageFragment = ShareImageFragment;

    window.zoomableImageLoaded = new rxjs.Subject();

    document.addEventListener('DOMContentLoaded', () => {
        window.image = new ZoomableImage();
        window.image.load()
        .then(image => {
            window.zoomableImageLoaded.next(image);
        })
        .catch(e => {
            window.zoomableImageLoaded.error(e);
        });
    });

})();
