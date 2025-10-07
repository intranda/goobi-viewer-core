(function () {
  'use strict';

  (function () {

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
    };  

    function init() { 
        console.log("init image view", _config);
        const imageElement = document.querySelector(_config.elementSelectors.image);
        if(imageElement) {
            window.image = {};
            const imageViewConfig = createZoomableImageConfig(imageElement);
            console.log("create image view with config ", imageViewConfig);
            window.image.viewer = new ImageView.Image(imageViewConfig);
            window.image.zoom = new ImageView.Controls.Zoom(window.image.viewer);
            window.image.zoom.setSlider(_config.elementSelectors.controls.zoomSlider);
            window.image.rotation = new ImageView.Controls.Rotation(window.image.viewer);
            document.querySelectorAll(_config.elementSelectors.controls.rotateLeft).forEach(button => button.addEventListener("click", e => window.image.rotation.rotateLeft()));
            document.querySelectorAll(_config.elementSelectors.controls.rotateRight).forEach(button => button.addEventListener("click", e => window.image.rotation.rotateRight()));
            document.querySelectorAll(_config.elementSelectors.controls.reset).forEach(button => button.addEventListener("click", e => {
                window.image.rotation.rotateTo(0);
                window.image.zoom.goHome();
            }));

            const footerUrl = document.querySelector(_config.elementSelectors.data.footer).textContent;
            if(window.image.viewer.viewportMargins.bottom > 0 && footerUrl) {
                window.image.footer = new ImageView.Footer(window.image.viewer, window.image.viewer.viewportMargins.bottom);
                window.image.footer.load(footerUrl);
            }

            const tileSourcesText = document.querySelector(_config.elementSelectors.data.tileSource).textContent;
            if(tileSourcesText) {
                try {
                    const tileSources = JSON.parse(tileSourcesText);
                    Object.keys(tileSources).forEach((key) => {
                        let value = tileSources[key];
                        if(typeof value == "string" && (value.startsWith("{") || value.startsWith("["))) {
                            tileSources[key] = JSON.parse(value);
                        }
                    });
                    const imageIdToOrderMap = new Map(Object.entries(tileSources).map( ([key, value]) => [viewerJS.iiif.getId(value), Number(key)]));
                    
                    window.image.viewer.load( Object.values(tileSources) );

                } catch(e) {
                    console.error(`Error parsing tileSource "${tileSourcesText}": ${e}`);
                }        }
     
        }
      
    } 


    function createZoomableImageConfig(imageElement) {
        return  {
            element: imageElement,
            fittingMode: getFittingMode(document.querySelector(_config.elementSelectors.data.pageType).textContent),
            margins: getMargins(document.querySelector(_config.elementSelectors.data.footer).dataset[_config.datasets.data.footerHeight], document.querySelector(_config.elementSelectors.data.pageType).textContent),
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

    document.addEventListener('DOMContentLoaded', () => {
        init();
    });

  }());

}());
