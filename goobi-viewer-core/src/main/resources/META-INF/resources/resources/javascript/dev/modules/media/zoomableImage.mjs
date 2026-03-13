import ZoomableImageOverlayGroup from './zoomableImageOverlayGroup.mjs';
import PageAreas from './pageAreas.mjs';

const _debug = false;

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
            topMarginElement: '[data-image="zoomable"] [data-image-data="topMarginElement"]',
            leftMarginElement: '[data-image="zoomable"] [data-image-data="leftMarginElement"]',
            rightMarginElement: '[data-image="zoomable"] [data-image-data="rightMarginElement"]',
        },
        controls: {
            rotateLeft: '.rotate-left',
            rotateRight: '.rotate-right',
            reset: '.reset',
            zoomSlider: '.zoom-slider',
        },
    },
    datasets: {
        image: {
            viewMode: 'imageViewMode',
            showNavigator: 'showNavigator',
            allowDownload: 'allowDownload',
            allowZoom: 'allowZoom',
        },
        data: {
            footerHeight: 'height',
            styleclass: 'styleclass',
            showTooltip: 'showTooltip',
        },
    },
};

export default class ZoomableImage {
    constructor() {
        if (_debug) console.log('init image view', _config);
        const imageElement = document.querySelector(_config.elementSelectors.image);
        if (imageElement) {
            this.pi = document.querySelector(_config.elementSelectors.data.record).textContent;
            this.logId = document.querySelector(_config.elementSelectors.data.structure).textContent;
            this.currentPageNo = document.querySelector(_config.elementSelectors.data.page).textContent;
            this.pageType = document.querySelector(_config.elementSelectors.data.pageType).textContent;
            this.viewMode = imageElement.dataset[_config.datasets.image.viewMode];

            this.topMarginElement = document.querySelector(_config.elementSelectors.data.topMarginElement)?.textContent;
            this.leftMarginElement = document.querySelector(
                _config.elementSelectors.data.leftMarginElement
            )?.textContent;
            this.rightMarginElement = document.querySelector(
                _config.elementSelectors.data.rightMarginElement
            )?.textContent;

            const imageViewConfig = createZoomableImageConfig(imageElement);
            if (_debug) console.log('create image view with config ', imageViewConfig);
            this.viewer = new ImageView.Image(imageViewConfig);
            this.zoom = new ImageView.Controls.Zoom(this.viewer);
            this.rotation = new ImageView.Controls.Rotation(this.viewer);
            this.persistence = new ImageView.ViewPersistence(this.zoom, this.pageType + '.' + this.pi);
            initControls(this.zoom, this.rotation);

            this.footer = createFooter(this.viewer);

            this.tileSources = createTileSource();
            if (_debug) console.log('use TileSources ', this.tileSources);

            this.tileSourceIdToOrder = Object.fromEntries(
                Object.entries(this.tileSources).map(([order, obj]) => [viewerJS.iiif.getId(obj), order])
            );

            if (this.viewMode == 'sequence') {
                if (_debug) console.log('initialize sequence mode');
                this.sequence = new ImageView.Sequence(this.viewer, this.zoom);
            }

            this.overlayGroups = [];
            document.querySelectorAll(_config.elementSelectors.data.overlays).forEach((element) => {
                const coordsString = element.innerHTML;
                if (coordsString.trim().length > 0) {
                    try {
                        const coords = JSON.parse(coordsString);

                        const overlays = new ZoomableImageOverlayGroup(this, coords, {
                            styleclass: element.dataset[_config.datasets.data.styleclass],
                            showTooltip: element.dataset[_config.datasets.data.showTooltip],
                            highlightClassName: 'focus',
                            highlightOnHover: true,
                        });
                        this.overlayGroups.push(overlays);
                    } catch (e) {
                        console.error('Error parsing coords string ', coordsString, e);
                    }
                }
            });

            this.pageAreaGroup = _drawPageAreas(this);
        }
    }

    resetSize() {
        this.viewer?.extent?.setSize(this.viewer.tileSources);
        this.updateMargins();
    }

    updateMargins() {
        if (this.viewer?.openseadragon?.viewport) {
            const viewerRight = this.viewer.element.offsetLeft + this.viewer.element.offsetWidth;
            const sidebarRightLeft = document.querySelector(this.rightMarginElement)?.offsetLeft;
            const margins = {
                left:
                    (document.querySelector(this.leftMarginElement)?.offsetWidth ?? 0) +
                    (document.querySelector(this.leftMarginElement)?.offsetLeft ?? 0),
                right: sidebarRightLeft ? viewerRight - sidebarRightLeft : 0,
                top: document.querySelector(this.topMarginElement)?.offsetHeight ?? 0,
            };
            this.viewer.setMargins(margins);
        }
    }

    load() {
        if (this.viewer) {
            return this.viewer.load(Object.values(this.tileSources), this.getCurrentTileSourceIndex()).then((image) => {
                this.sequence?.initialize(this.getCurrentTileSourceId());
                this.overlayGroups.forEach((group) => group.show());
                this.initWindowResize();
                return this;
            });
        } else {
            return new Promise((resolve, reject) => {
                reject('no image found');
            });
        }
    }

    initWindowResize() {
        window.addEventListener('resize', () => this.resetSize());
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
        return this.tileSources[order];
    }

    getTileSourceFromId(id) {
        return this.tileSources.values().find((source) => viewerJS.iiif.getId(value) == id);
    }

    getTileSourceIdFromOrder(order) {
        return viewerJS.iiif.getId(this.getTileSourceFromOrder(order));
    }

    getTileSourceOrderFromId(id) {
        return this.tileSourceIdToOrder[id];
    }
}

function _drawPageAreas(image) {
    const pageAreaElement = document.querySelector(_config.elementSelectors.data.pageAreas);
    const text = pageAreaElement.textContent;
    if (text && text.length) {
        try {
            const areas = JSON.parse(text);
            const pageAreas = new PageAreas(
                {
                    currentLogId: image.logId,
                    areas: areas,
                },
                image
            );
            return pageAreas;
        } catch (e) {
            console.error('Error reading page areas ', text, e);
        }
    }
}

function createTileSource() {
    const tileSourcesString = document.querySelector(_config.elementSelectors.data.tileSource).textContent;
    if (tileSourcesString) {
        try {
            const tileSources = JSON.parse(tileSourcesString);
            Object.keys(tileSources).forEach((key) => {
                let value = tileSources[key];
                if (typeof value == 'string' && (value.startsWith('{') || value.startsWith('['))) {
                    tileSources[key] = JSON.parse(value);
                } else if (value.endsWith('/info.json')) {
                    tileSources[key] = value.replace('/info.json', '');
                }
            });
            return tileSources;
        } catch (e) {
            //if no image number map is passed, but a simple url string
            return { 1: tileSourcesString };
        }
    }
}

function createFooter(viewer) {
    const footerUrl = document.querySelector(_config.elementSelectors.data.footer).textContent;
    if (viewer.viewportMargins.bottom > 0 && footerUrl) {
        const footer = new ImageView.Footer(viewer, viewer.viewportMargins.bottom);
        footer.load(footerUrl);
        return footer;
    }
}

function initControls(zoom, rotation) {
    if (document.querySelector(_config.elementSelectors.controls.zoomSlider)) {
        zoom.setSlider(_config.elementSelectors.controls.zoomSlider, 3);
    }
    document
        .querySelectorAll(_config.elementSelectors.controls.rotateLeft)
        .forEach((button) => button.addEventListener('click', (e) => rotation.rotateLeft()));
    document
        .querySelectorAll(_config.elementSelectors.controls.rotateRight)
        .forEach((button) => button.addEventListener('click', (e) => rotation.rotateRight()));
    document.querySelectorAll(_config.elementSelectors.controls.reset).forEach((button) =>
        button.addEventListener('click', (e) => {
            rotation.rotateTo(0);
            zoom.goHome();
        })
    );
}

function createZoomableImageConfig(imageElement) {
    return {
        element: imageElement,
        fittingMode: getFittingMode(document.querySelector(_config.elementSelectors.data.pageType)?.textContent),
        margins: {
            bottom: Number(
                document.querySelector(_config.elementSelectors.data.footer)?.dataset[
                    _config.datasets.data.footerHeight
                ]
            ),
        },
        zoom: {
            enabled: imageElement.dataset[_config.datasets.image.allowZoom] !== 'false',
        },
        sequence: getSequenceSettings(imageElement.dataset[_config.datasets.image.viewMode]),
        navigator: {
            enabled: imageElement.dataset[_config.datasets.image.showNavigator] === 'true',
        },
    };
}

function getSequenceSettings(viewMode) {
    switch ((viewMode || '').toLowerCase()) {
        case 'double':
            return {
                columns: 2,
            };
        case 'sequence':
            return {
                columns: 1,
            };
        case 'single':
        default:
            return {
                columns: 1,
            };
    }
}

function getFittingMode(pageType) {
    switch ((pageType || '').toLowerCase()) {
        case 'viewfullscreen':
        case 'crowdsourcing':
            return 'fixed';
        default:
            return 'toWidth';
    }
}
