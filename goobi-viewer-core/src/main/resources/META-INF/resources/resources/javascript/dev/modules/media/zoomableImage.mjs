import ZoomableImageOverlayGroup from "./zoomableImageOverlayGroup.mjs";

const _debug = true;

const _config = {
    elementSelectors: {
        image: '[data-image="zoomable"]',
        data: {
            tileSource: '[data-image="zoomable"] [data-image-data="tileSource"]',
            record: '[data-image="zoomable"] [data-image-data="structureIdentifier"]',
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
}

export default class ZoomableImage {

    constructor() {
        console.log("init image view", _config);
        const imageElement = document.querySelector(_config.elementSelectors.image);
        if(imageElement) { 
            
            const imageViewConfig = createZoomableImageConfig(imageElement);
            if(_debug)console.log("create image view with config ", imageViewConfig);
            this.viewer = new ImageView.Image(imageViewConfig);
            this.zoom = new ImageView.Controls.Zoom(this.viewer);
            this.rotation = new ImageView.Controls.Rotation(this.viewer);
            initControls(this.zoom, this.rotation);

            this.footer = createFooter(this.viewer);

            this.tileSources = createTileSource();
            if(_debug)console.log("use TileSources ", this.tileSources);
            this.tileSourceIdToOrder = Object.fromEntries(
                Object.entries(this.tileSources).map(([order, obj]) => [viewerJS.iiif.getId(obj), order])
            );

            this.pi = document.querySelector(_config.elementSelectors.data.record).textContent;
            this.logId = document.querySelector(_config.elementSelectors.data.structure).textContent;
            this.currentPageNo = document.querySelector(_config.elementSelectors.data.page).textContent;
            this.pageType = document.querySelector(_config.elementSelectors.data.pageType).textContent;

            
            this.overlayGroups = [];
            document.querySelectorAll(_config.elementSelectors.data.overlays).forEach( element => {
                const coordsString = element.innerHTML;
                if(coordsString.trim().length > 0) {
                    try {
                        const coords = JSON.parse(coordsString);
    
                        const overlays = new ZoomableImageOverlayGroup(this, coords, {
                            styleclass: element.dataset[_config.datasets.data.styleclass],
                            showTooltip: element.dataset[_config.datasets.data.showTooltip]
                        })
                        this.overlayGroups.push(overlays);
                    } catch(e) {
                        console.error("Error parsing coords string ", coordsString, e);
                    }
                }
            });
        }
    }

    load() {
        this.viewer.load( Object.values(this.tileSources) )
        .then(image => {
            this.overlayGroups.forEach(group => group.show());
        });
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
            console.error(`Error parsing tileSource "${tileSourcesString}": ${e}`)
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
    document.querySelectorAll(_config.elementSelectors.controls.rotateRight).forEach(button => button.addEventListener("click", e => rotation.rotateRight()))
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