(function () {
    'use strict';

    const _config$1 = {
        styleclass: 'imageview-overlay',
        showTooltip: false,
        highlightClassName: 'highlight',
        highlightOnHover: false,
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
                tooltipClassName: this.config.styleclass + ' tooltip',
                highlightClassName: this.config.highlightClassName,
                highlightOnHover: this.config.highlightOnHover,
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
        if (Array.isArray(coords)) {
            coords.forEach((coord) => {
                if (Array.isArray(coord)) {
                    overlays.push({
                        target: target,
                        coordinates: [coord[0], coord[1], coord[2] - coord[0], coord[3] - coord[1]],
                        tooltip: coord.length > 4 ? coord[4] : undefined,
                        id: coord.length > 5 ? coord[5] : undefined,
                    });
                }
            });
        }
        return overlays;
    }

    class PageAreas {
        constructor(config, image) {
            console.log('init page areas ', config);
            this.areas = config.areas;
            let styles = viewerJS.helper.getCss('page-area', ['borderTopColor', 'borderTopWidth', 'background-color']);
            ({
                borderWidth: styles['borderTopWidth'],
                borderColor: styles['borderTopColor'],
                fillColor: styles['background-color'],
            });
            let activeStyles = viewerJS.helper.getCss('page-area focus', ['borderTopColor', 'borderTopWidth', 'background-color']);
            ({
                borderWidth: parseInt(activeStyles['borderTopWidth']),
                borderColor: activeStyles['borderTopColor'],
                fillColor: activeStyles['background-color'],
            });

            image.viewer.onOpened.subscribe((viewer) => {
                console.log('page areas on viewer open ', config);
                image.viewer.openseadragon.addHandler('canvas-press', () => {
                    this.dragging = false;
                });
                image.viewer.openseadragon.addHandler('canvas-drag', () => {
                    this.dragging = true;
                });

                let activeAreas = this.areas.filter((a) => a.logId === config.currentLogId);
                console.log('active areas: ', activeAreas);
                if (activeAreas.length > 0) {
                    this.drawActiveAreas(activeAreas, image);
                    this.initAreaClick(image, activeAreas);
                } else {
                    let inactiveAreas = this.areas.filter((a) => a.logId !== config.currentLogId);
                    let inactiveLogIds = inactiveAreas.map((a) => a.logId).filter((v, i, a) => a.indexOf(v) === i); //last filter to make logIds unique
                    inactiveLogIds.forEach((logId) => {
                        let logIdAreas = this.areas.filter((a) => a.logId === logId);
                        logIdAreas.forEach((area, index) => this.drawArea(area, index, image));
                    });
                    this.initAreaClick(image, inactiveAreas);
                }
            });
        }

        setBrowserLocation(pi, pageNo) {
            const url = new URL(window.location.href);
            let pathArray = url.pathname.split('/');
            let replacedPathPart = false;
            for (var i = pathArray.length - 1; i >= 0; i--) {
                if (/^\d+$/.test(pathArray[i]) && pathArray[i] != pi) {
                    //this is presumably the page number
                    pathArray[i] = pageNo;
                    replacedPathPart = true;
                    break;
                }
            }
            if (!replacedPathPart) {
                if (pathArray[pathArray.length - 1].length == 0) {
                    pathArray.pop();
                }
                pathArray.push(pageNo);
            }
            url.pathname = pathArray.join('/');

            //update tab url
            //alternatively use history.addState to add an entry to the browser history, so navigating back leads to the previous page
            window.history.replaceState(null, '', url.toString());
        }

        drawActiveAreas(activeAreas, imageView) {
            let areasOnCanvas = [];
            activeAreas.forEach((activeArea, index) => {
                const area = this.drawArea(activeArea, index, imageView);
                areasOnCanvas.push(area?.overlay?.bounds);
                console.log('add active area ', area, areasOnCanvas);
                let scrollPosition = window.sessionStorage.getItem('scrollPosition');
                $(document).scrollTop(parseInt(scrollPosition));
                window.sessionStorage.removeItem('scrollPosition');
            });
            let shadow = new ImageView.ImageFilters.HighlightArea(imageView.viewer, 0.5, areasOnCanvas, true);
            shadow.start();
        }

        initAreaClick(imageView, areas) {
            if (areas.length > 0) {
                let url = areas[0].url.replace('/' + areas[0].logId, '');

                const viewerClickPipe = imageView.viewer.onClick.pipe(rxjs.operators.map(() => url));
                const areaClicks = [];
                areas.forEach((area) => {
                    const areaClickPipe = area.overlay?.onClick().pipe(rxjs.operators.map(() => area.url));
                    areaClicks.push(areaClickPipe);
                });

                rxjs.merge(viewerClickPipe, ...areaClicks)
                    .pipe(
                        rxjs.operators.filter((e) => !this.dragging),
                        rxjs.operators.debounceTime(10)
                    )
                    .subscribe((url) => {
                        console.log('viewer click ', url, window.location.href);
                        if (url && url.length && url !== window.location.href) {
                            window.sessionStorage.setItem('scrollPosition', $(document).scrollTop());
                            window.location.href = url;
                        }
                    });
            }
        }

        drawArea(area, shapeIndex, image, clickToLeave) {
            let imageRect = ImageView.CoordinateConversion.convertToOpenSeadragonObject(area.coords);
            let areaSourceId = image.getTileSourceFromOrder(area.pageNo)?.id;
            let rect = image.viewer.getViewportCoordinates(imageRect, areaSourceId);
            let $area = $('#pageAreaFrame_' + area.logId + '_' + shapeIndex);
            let $label = $('#pageAreaLabel_' + area.logId + '_' + shapeIndex);
            let overlayId = area.logId + '_' + shapeIndex;
            let overlayConfig = {
                element: $area.get(0),
            };
            area.overlay = new ImageView.Overlay(rect, overlayConfig, overlayId);
            area.tooltip = new ImageView.Tooltip(area.overlay, area.label, image.viewer, {
                onHover: true,
                className: 'page-area-label page-area-label-text',
                element: $label.get(0),
                placement: 'bottom',
            });
            document.body.append($label.get(0));
            //tooltip.show();
            area.overlay.draw(image.viewer);
            $area.hover(
                () => $(area.tooltip.element).addClass('hover'),
                () => $(area.tooltip.element).removeClass('hover')
            );
            return area;
        }
    }

    // Maximum number of tile sources loaded into OpenSeadragon simultaneously in sequence mode.
    // Keeps OSD's world small enough to run smoothly; page navigation triggers a full reload
    // that re-centers the window on the newly selected page.
    const _sequenceWindowSize = 100;
    // How many images from the window boundary trigger loading the next batch.
    const _expandThreshold = 10;
    // How many images to add per expansion step.
    const _expandBatchSize = 50;

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
                downloadContent: '.download-canvas-content',
            },
        },
        datasets: {
            image: {
                viewMode: 'imageViewMode',
                showNavigator: 'imageShowNavigator',
                allowZoom: 'allowZoom',
                maxZoom: 'maxZoom',
                allowDownloadContent: 'allowDownload',
                downloadFilename: 'downloadFilename',
            },
            data: {
                footerHeight: 'height',
                styleclass: 'styleclass',
                showTooltip: 'showTooltip',
            },
        },
    };

    class ZoomableImage {
        constructor() {
            const imageElement = document.querySelector(_config.elementSelectors.image);
            if (imageElement) {
                this.pi = document.querySelector(_config.elementSelectors.data.record).textContent;
                this.logId = document.querySelector(_config.elementSelectors.data.structure).textContent;
                this.currentPageNo = document.querySelector(_config.elementSelectors.data.page).textContent;
                this.pageType = document.querySelector(_config.elementSelectors.data.pageType).textContent;
                this.viewMode = imageElement.dataset[_config.datasets.image.viewMode];

                this.topMarginElement = document.querySelector(_config.elementSelectors.data.topMarginElement)?.textContent;
                this.leftMarginElement = document.querySelector(_config.elementSelectors.data.leftMarginElement)?.textContent;
                this.rightMarginElement = document.querySelector(_config.elementSelectors.data.rightMarginElement)?.textContent;

                //download canvas content
                if (imageElement.dataset[_config.datasets.image.allowDownloadContent] === 'true') {
                    this.downloadButton = _initCanvasDownload(
                        imageElement,
                        document.querySelector(_config.elementSelectors.controls.downloadContent),
                        imageElement.dataset[_config.datasets.image.downloadFilename]
                    );
                }

                const imageViewConfig = createZoomableImageConfig(imageElement);
                this.viewer = new ImageView.Image(imageViewConfig);
                this.zoom = new ImageView.Controls.Zoom(this.viewer);
                this.rotation = new ImageView.Controls.Rotation(this.viewer);
                this.persistence = new ImageView.ViewPersistence(this.zoom, this.pageType + '.' + this.pi);
                initControls(this.zoom, this.rotation);

                this.footer = createFooter(this.viewer);

                this.tileSources = createTileSource();

                this.tileSourceIdToOrder = Object.fromEntries(Object.entries(this.tileSources).map(([order, obj]) => [viewerJS.iiif.getId(obj), order]));

                if (this.viewMode == 'sequence') {
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
                    left: (document.querySelector(this.leftMarginElement)?.offsetWidth ?? 0) + (document.querySelector(this.leftMarginElement)?.offsetLeft ?? 0),
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

        /**
         * Jumps to the image with the given IIIF id. If windowing is active and the image lies
         * outside the currently loaded window, Sequence handles the window reload automatically.
         *
         * @param {string} id  IIIF image id of the target image
         */
        setCurrentImage(id) {
            this.sequence?.setCurrentImage(id, true, false);
        }

        initWindowResize() {
            window.addEventListener('resize', () => this.resetSize());
        }

        getCurrentTileSourceIndex() {
            // OSD world is empty before viewer.load() — resolve index directly from the tileSources map
            return Object.keys(this.tileSources).indexOf(this.currentPageNo);
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
            if (this.sequence) {
                id =
                    this.sequence.urlMap
                        .entries()
                        .find((e) => e[1] == id)
                        ?.at(0) ?? id;
            }
            return this.tileSourceIdToOrder[id];
        }
    }

    function _initCanvasDownload(imageContainer, button, filename) {
        if (imageContainer && button) {
            button.addEventListener('click', () => {
                const canvas = imageContainer.querySelector('canvas');
                const link = document.createElement('a');
                link.download = filename;
                link.href = canvas.toDataURL('image/jpeg');
                link.click();
                link.remove();
            });
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
        document.querySelectorAll(_config.elementSelectors.controls.rotateLeft).forEach((button) => button.addEventListener('click', (e) => rotation.rotateLeft()));
        document.querySelectorAll(_config.elementSelectors.controls.rotateRight).forEach((button) => button.addEventListener('click', (e) => rotation.rotateRight()));
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
                bottom: Number(document.querySelector(_config.elementSelectors.data.footer)?.dataset[_config.datasets.data.footerHeight]),
            },
            zoom: {
                enabled: imageElement.dataset[_config.datasets.image.allowZoom] !== 'false',
                max: parseInt(imageElement.dataset[_config.datasets.image.maxZoom]),
            },
            sequence: getSequenceSettings(imageElement.dataset[_config.datasets.image.viewMode]),
            navigator: {
                enabled: imageElement.dataset[_config.datasets.image.showNavigator] === 'true',
                position: 'BOTTOM_RIGHT',
            },
        };
    }

    function getSequenceSettings(viewMode) {
        let columns;
        switch ((viewMode || '').toLowerCase()) {
            case 'double':
                columns = 2;
                break;
            case 'sequence':
            case 'single':
            default:
                columns = 1;
        }
        return {
            columns: columns,
            useWindowing: true,
            windowSize: _sequenceWindowSize,
            windowExpandThreshold: _expandThreshold,
            windowExpandSize: _expandBatchSize,
        };
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
            this.$fragmentSelectButton = $('.share-image-region a');
            this.$links = $('.share-image-area__links');
            this.$instructions = $('.share-image-area__instructions');

            this.$links.hide();

            //fullscreen controls
            let $panels = $('.fullscreen__view-sidebar-accordeon-panel h3');
            $panels.on('click', (e) => {
                let $panel = $(e.target).closest('.fullscreen__view-sidebar-accordeon-panel h3');
                this.toggleImageShare($panel);
            });
            $(".share-image-area [data-popover='close']").on('click', (e) => {
                this.endFragmentSelect();
                $(e.target).closest('.fullscreen__view-sidebar-accordeon-panel').find('h3').click();
                $(e.target).closest('.fullscreen__view-sidebar-accordeon-panel').find('h3').focus();
            });

            // init area select
            try {
                let styles = viewerJS.helper.getCss('image-fragment', ['borderTopColor', 'borderTopWidth', 'background-color']);
                var fragmentSelectConfig = {
                    removeOldAreas: true,
                    drawCondition: (event) => this.active && this.fragmentSelect?.currentOverlay == undefined,
                    transformCondition: (event) => this.active,
                    drawStyle: {
                        borderColor: styles['borderTopColor'],
                        borderWidth: parseInt(styles['borderTopWidth']),
                        fillColor: styles['background-color'],
                        className: 'image-fragment',
                    },
                };
                this.fragmentSelect = new ImageView.AreaSelect(image.viewer, fragmentSelectConfig);
            } catch (error) {
                console.error('Error initializing area select: ', error);
            }
            this.toggleImageShare($('.share-image-area h3'));
            this.$fragmentSelectButton.on('shown.bs.popover', () => this.startFragmentSelect());
            this.$fragmentSelectButton.on('hidden.bs.popover', () => this.endFragmentSelect());
        }

        startFragmentSelect() {
            this.$fragmentSelectButton.tooltip('hide');
            this.$fragmentSelectButton.addClass('active');
            this.active = true;

            if (this.fragmentSelect) {
                this.fragmentSelect.finishedHook.subscribe((area) => {
                    var areaString = this.getAreaString(area);
                    var pageUrl = window.location.origin + window.location.pathname + window.location.search + '#xywh=' + areaString;
                    var imageUrl = this.getRegionUrl(area);
                    console.log('set area data ', pageUrl, imageUrl);
                    $('[data-fragment-link="page"]').attr('data-copy-share-image', pageUrl);
                    $('[data-fragment-link="iiif"]').attr('data-copy-share-image', imageUrl);
                    console.log('area share ', $('[data-fragment-link="iiif"]'));
                    this.$links.show();
                    this.$instructions.hide();
                    this.initImageFragmentLinks(areaString);
                });
            }
        }

        getAreaString(area) {
            if (area && area.x != undefined && area.y != undefined && area.width != undefined && area.height != undefined) {
                var areaString = area.x.toFixed(0) + ',' + area.y.toFixed(0) + ',' + area.width.toFixed(0) + ',' + area.height.toFixed(0);
                return areaString;
            } else {
                return 'full';
            }
        }

        getAreaFromString(string) {
            const parts = string
                .split(',')
                .filter((s) => s.match(/^\d+$/))
                .map((s) => Number(s));
            if (parts.length == 4) {
                return {
                    x: parts[0],
                    y: parts[1],
                    width: parts[2],
                    height: parts[3],
                };
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
            let imageUrl = viewerJS.iiif.getId(tileSource);
            imageUrl = imageUrl + '/' + areaString + '/max/' + this.image.viewer.getRotation() + '/default.jpg';
            return imageUrl;
        }

        endFragmentSelect() {
            this.active = false;
            this.$fragmentSelectButton.removeClass('active');
            if (this.fragmentSelect) {
                this.fragmentSelect.transformer?.close();
                this.fragmentSelect.removeOverlays();
            }
            this.$links.hide();
            this.hideImageFragmentLinks();
            this.$instructions.show();
        }

        initAreaFromFragmentHash() {
            const fragment = viewerJS.helper.getFragmentHash();
            if (fragment && this.fragmentSelect) {
                const area = this.getAreaFromString(fragment);
                if (area) {
                    this.fragmentSelect.draw(area);
                    this.initImageFragmentLinks(fragment);
                }
            }
        }

        initImageFragmentLinks(fragment) {
            let $wrapper = $('[data-fragment-link="wrapper"]');

            if (fragment) {
                var pageUrl = window.location.origin + window.location.pathname + '#xywh=' + fragment;
                var imageUrl = this.getRegionUrl(fragment);
                $wrapper.find('[data-fragment-link="page"]').attr('data-copy-share-image', pageUrl);
                $wrapper.find('[data-fragment-link="iiif"]').attr('data-copy-share-image', imageUrl);
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
            if ($panel.closest('.fullscreen__view-sidebar-accordeon-panel').hasClass('share-image-area') && $panel.hasClass('in')) {
                this.startFragmentSelect();
            }
        }
    }

    const _default = {
        container: '#world',
        loadStory: '#loadStoryEditor',
        closeStory: '#closeStoryEditor',
        sizeText: '#objectSize',
        explorer: '#voyager',
        story: '#voyagerStory',
        startLoad: '#startLoadButton',
        loadProgress: '#loadingProgress',
    };

    class Voyager3dView {
        constructor(config) {
            this.config = jQuery.extend(true, {}, _default, config);
            this.container = document.querySelector(this.config.container);
            // console.log('init voyager3d', this);
            if (this.isVisible()) {
                this.loaded = this.initView().then(() => {});
            }
        }

        async initView() {
            this.loadStoryButton = document.querySelector(this.config.loadStory);
            this.closeStoryButton = document.querySelector(this.config.closeStory);
            this.startLoadButton = document.querySelector(this.config.startLoad);
            this.loadingProgressMonitor = document.querySelector(this.config.loadProgress);
            this.objectUrl = this.container.dataset.objectUrl;
            this.resourcePath = this.container.dataset.resourcePath;
            if (this.objectUrl) {
                this.startLoadButton?.addEventListener('click', (e) => this.loadExplorer());
                this.loadStoryButton?.addEventListener('click', (e) => this.loadStory());
                this.closeStoryButton?.addEventListener('click', (e) => this.loadExplorer());
                this.sceneData = await this.getSceneData();
                this.showLoadSizeText();
            }
        }

        loadExplorer() {
            const explorer = `<voyager-explorer id='voyager' bgcolor='#015999 #000' bgstyle='LinearGradient' style='display:none; color:lightgray;' document='${this.getSceneUrl()}' uiMode='menu' resourceRoot='${this.resourcePath}'></voyager-explorer>`;

            show(this.loadStoryButton);
            hide(this.closeStoryButton);
            this.unloadObject();
            this.unloadStory();
            hide(this.startLoadButton);
            show(this.loadingProgressMonitor);

            const tempElement = document.createElement('div');
            tempElement.innerHTML = explorer;
            this.container.append(...tempElement.children);

            this.voyager = document.getElementById(this.config.explorer);
            voyager.addEventListener('model-load', (e) => {
                show(voyager);
            });
        }

        loadStory() {
            const story = `<voyager-story id='voyagerStory' bgcolor='#015999 #000' bgstyle='LinearGradient' style='display:none; color:lightgray;' document='${this.getSceneUrl()}'></voyager-story>`;

            hide(this.loadStoryButton);
            show(this.closeStoryButton);
            this.unloadObject();
            this.unloadStory();
            hide(this.startLoadButton);
            show(this.loadingProgressMonitor);

            const tempElement = document.createElement('div');
            tempElement.innerHTML = story;

            console.log('appending ', ...tempElement.children, ' to ', this.container);
            this.container.append(...tempElement.children);

            const voyager = document.getElementById(this.config.story);
            voyager.addEventListener('model-load', (e) => {
                console.log('loaded ', e);
                show(voyager);
            });
        }

        showLoadSizeText() {
            const size = this.getTotalSize(this.sceneData);
            const sizeText = document.querySelector(this.config.sizeText);
            if (size && sizeText) {
                sizeText.innerHTML = (size / 1000 / 1000).toFixed(2) + ' MB';
            }
        }

        isVisible() {
            return this.container && $(this.container).is(':visible');
        }

        getTotalSize(scene) {
            return scene.models
                .flatMap((m) => m.derivatives)
                .flatMap((d) => d.assets)
                .map((a) => a.byteSize)
                .reduce((a, b) => a + b, 0);
        }

        getSceneUrl() {
            return this.objectUrl + '/scene.svx.json';
        }

        async getSceneData() {
            return fetch(this.getSceneUrl()).then((res) => res.json());
        }

        unloadObject() {
            document.querySelector(this.config.explorer)?.remove();
        }

        unloadStory() {
            document.querySelector(this.config.story)?.remove();
        }
    }

    function hide(element) {
        if (element) {
            element.style.display = 'none';
        }
    }

    function show(element) {
        if (element) {
            element.style.display = 'block';
        }
    }

    /**
     * Pure page/spread window math for the immersive image viewer.
     *
     * No DOM, no network, no OpenSeadragon dependencies — safe to unit-test and
     * to import in any context.
     */

    /**
     * Returns the 0-based page indices shown together for the spread that contains
     * `order`. Book layout (LTR): the cover (page 0) stands alone, then pages are
     * paired (1,2),(3,4),… An odd final page stands alone.
     *
     * @param {number} order  0-based page index
     * @param {number} total  total page count
     * @param {{coverAlone?:boolean}} [opts]
     * @returns {number[]} one or two page indices, ascending; empty for an empty book
     */
    function computeSpread(order, total, { coverAlone = true } = {}) {
        if (total <= 0) return [];
        const o = Math.max(0, Math.min(order, total - 1));
        let leader;
        if (coverAlone) {
            if (o === 0) return [0];
            leader = 1 + 2 * Math.floor((o - 1) / 2);
        } else {
            leader = o - (o % 2);
        }
        return leader + 1 <= total - 1 ? [leader, leader + 1] : [leader];
    }

    /**
     * Page indices that make up the frame containing `order`: just the page in
     * single mode, the spread in double mode.
     *
     * @param {number} order  0-based page index
     * @param {number} total  total page count
     * @param {{double?:boolean}} [opts]
     * @returns {number[]} one or two page indices, ascending; empty for an empty book
     */
    function framePages(order, total, { double = false } = {}) {
        if (total <= 0) return [];
        const o = Math.max(0, Math.min(order, total - 1));
        return double ? computeSpread(o, total) : [o];
    }

    /**
     * Page indices to keep resident (current frame + the immediately adjacent
     * frames) so neighbour navigation is instant.
     *
     * @param {number} order  0-based page index
     * @param {number} total  total page count
     * @param {{double?:boolean}} [opts]
     * @returns {number[]} sorted, deduplicated page indices
     */
    function residentPages(order, total, { double = false } = {}) {
        const here = framePages(order, total, { double });
        const prev = framePages(here[0] - 1, total, { double });
        const next = framePages(here[here.length - 1] + 1, total, { double });
        return [...new Set([...prev, ...here, ...next])].filter((p) => p >= 0 && p < total).sort((a, b) => a - b);
    }

    /** Frame-based transitions for the stage: tweens, tile crossfades, the frozen snapshot overlay and the height-fit band. */

    /**
     * Runs a rAF tween over `durationMs`, calling `onFrame` with the progress 0→1
     * each frame (a non-positive duration jumps straight to 1). Resolves when done.
     *
     * @param {number} durationMs
     * @param {(t: number) => void} onFrame
     * @returns {Promise<void>}
     */
    function tween(durationMs, onFrame) {
        return new Promise((resolve) => {
            let start = null;
            const step = (ts) => {
                if (start === null) start = ts;
                const t = durationMs <= 0 ? 1 : Math.min(1, (ts - start) / durationMs);
                onFrame(t);
                if (t < 1) requestAnimationFrame(step);
                else resolve();
            };
            requestAnimationFrame(step);
        });
    }

    /** Tweens one TiledImage's opacity 0→1 while fading another 1→0. */
    function crossfade(incoming, outgoing, durationMs) {
        return tween(durationMs, (t) => {
            if (incoming) incoming.setOpacity(t);
            if (outgoing) outgoing.setOpacity(1 - t);
        });
    }

    /**
     * The current view expressed relative to the page row, so it survives a reload
     * that rebuilds the world: `zoom` is the magnification per row width and `x`/`y`
     * are the centre as a fraction of the row. Null whenever there is nothing
     * meaningful to keep. Pure + tested.
     *
     * @param {{x:number, y:number, width:number, height:number}|null} row  union bounds of the displayed pages
     * @param {{x:number, y:number}|null} center  viewport centre (the animation target, not the current frame)
     * @param {number} zoom  viewport zoom (likewise the target)
     * @param {number} pages  number of pages the row is made of
     * @returns {{zoom:number, x:number, y:number, pages:number}|null}
     */
    function captureView(row, center, zoom, pages) {
        if (!row || !center || !(row.width > 0) || !(row.height > 0) || !(zoom > 0) || !(pages > 0)) {
            return null;
        }
        return { zoom: zoom * row.width, x: (center.x - row.x) / row.width, y: (center.y - row.y) / row.height, pages };
    }

    /**
     * The zoom and centre a captured view maps to on the new row, or null when it
     * must not be applied: without a view, without a usable row, or when the row is
     * made of a different number of pages - a spread and a single page want their
     * own fit, not the same magnification. Pure + tested.
     *
     * @param {{zoom:number, x:number, y:number, pages:number}|null} view
     * @param {{x:number, y:number, width:number, height:number}|null} row
     * @param {number} pages  number of pages the new row is made of
     * @returns {{zoom:number, center:{x:number, y:number}}|null}
     */
    function restoredView(view, row, pages) {
        if (!view || !row || !(row.width > 0) || !(row.height > 0) || view.pages !== pages) {
            return null;
        }
        return { zoom: view.zoom / row.width, center: { x: row.x + view.x * row.width, y: row.y + view.y * row.height } };
    }

    /**
     * Widens a bounds rect to a tall band (same y/height, 3x width, same centre).
     * As a fitBounds anchor this makes OSD fit by height, so pages of differing scan
     * aspect render at a uniform height and paging never jumps vertically.
     */
    function heightBand(rect) {
        const band = rect.clone();
        band.x = rect.x - rect.width;
        band.width = rect.width * 3;
        return band;
    }

    /**
     * Copies the current OSD canvas into an opacity overlay covering `host`, so a
     * fresh load underneath stays hidden until faded out. Uses drawImage (not
     * toDataURL) so cross-origin IIIF tiles do not taint the canvas.
     *
     * @returns {HTMLCanvasElement|null}
     */
    function snapshotOverlay(osd, host) {
        const src = osd.drawer && osd.drawer.canvas;
        if (!src || !host) return null;
        const overlay = document.createElement('canvas');
        overlay.width = src.width;
        overlay.height = src.height;
        overlay.className = 'immersive__xfade';
        try {
            overlay.getContext('2d').drawImage(src, 0, 0);
        } catch {
            return null;
        }
        if (getComputedStyle(host).position === 'static') host.style.position = 'relative';
        host.appendChild(overlay);
        return overlay;
    }

    /** Fades an overlay element out over `durationMs`, then removes it. */
    function fadeOverlay(overlay, durationMs) {
        if (!overlay) return Promise.resolve();
        return tween(durationMs, (t) => {
            overlay.style.opacity = String(1 - t);
        }).then(() => overlay.remove());
    }

    /** Serialises page navigations: one at a time, always released, with a watchdog against a lost library event. */

    /**
     * @param {number} watchdogMs  last-resort release when a navigation never settles
     * @returns {{busy: boolean, run: Function, onRelease: Function, destroy: Function}}
     */
    function createNavLock(watchdogMs) {
        let busy = false;
        let destroyed = false;
        let watchdog = null;
        const releaseHooks = [];
        return {
            get busy() {
                return busy;
            },
            /**
             * Runs `task` (returns the settle promise) under the lock; `onStuck` cleans up if the watchdog fires first.
             * A run while the lock is busy is refused instead of overwriting the running task's watchdog -- callers
             * queue their target and pick it up on the release hook.
             *
             * @param {() => Promise|any} task
             * @param {Function} [onStuck]
             * @returns {boolean} whether the task was started
             */
            run(task, onStuck) {
                if (busy || destroyed) return false;
                busy = true;
                let released = false;
                const release = () => {
                    if (released) return;
                    released = true;
                    clearTimeout(watchdog);
                    busy = false;
                    releaseHooks.forEach((hook) => {
                        try {
                            hook();
                        } catch (e) {
                            console.error('immersive: release hook failed', e);
                        }
                    });
                };
                watchdog = setTimeout(() => {
                    try {
                        if (onStuck) onStuck();
                    } catch (e) {
                        console.error('immersive: watchdog cleanup failed', e);
                    }
                    release();
                }, watchdogMs);
                try {
                    Promise.resolve(task())
                        .catch((e) => console.warn('immersive: navigation failed', e))
                        .finally(release);
                } catch (e) {
                    console.warn('immersive: navigation failed', e);
                    release();
                }
                return true;
            },
            /** Registers a callback that runs after every release (queued navigations pick up here). */
            onRelease(fn) {
                releaseHooks.push(fn);
            },
            /** Clears the watchdog and the release hooks; a task settling afterwards starts nothing, new runs are refused. */
            destroy() {
                destroyed = true;
                clearTimeout(watchdog);
                releaseHooks.length = 0;
            },
        };
    }

    /** Overlay layers on the OpenSeadragon world: highlight sets per owner, OCR text regions and drawn annotation regions. */

    class IvOverlays {
        /**
         * @param {object} opts
         * @param {() => object} opts.osd          the live OpenSeadragon instance
         * @param {(order: number) => object|null} opts.itemFor  TiledImage showing a page order, or null
         * @param {() => object|null} opts.currentItem  the TiledImage of the current page
         */
        constructor({ osd, itemFor, currentItem }) {
            this._osd = osd;
            this._itemFor = itemFor;
            this._currentItem = currentItem;
            this._highlights = new Map();
            this._textRegions = [];
            this._annoRegions = [];
        }

        _place(el, item, rect) {
            this._osd().addOverlay({ element: el, location: item.imageToViewportRectangle(rect.x, rect.y, rect.w, rect.h) });
        }

        _drop(els) {
            const osd = this._osd();
            els.forEach((el) => osd.removeOverlay(el));
        }

        /**
         * Draws highlight rectangles for `owner`; every owner keeps its own set, so one
         * panel never wipes another's marks. Rect coordinates are image pixels of the
         * page given by `rect.order`; rects for pages outside the current view are
         * skipped. An optional `className` per rect is appended.
         */
        setHighlights(rects, owner = 'default') {
            this.clearHighlights(owner);
            const els = (rects || []).flatMap((r) => {
                const item = this._itemFor(r.order);
                if (!item) return [];
                const el = document.createElement('div');
                el.className = ['immersive__highlight', r.active ? '-active' : '', r.className || ''].filter(Boolean).join(' ');
                this._place(el, item, r);
                return [el];
            });
            this._highlights.set(owner, els);
        }

        /** Removes the highlights of `owner`, or of every owner when omitted. */
        clearHighlights(owner) {
            const owners = owner === undefined ? Array.from(this._highlights.keys()) : [owner];
            owners.forEach((key) => {
                this._drop(this._highlights.get(key) || []);
                this._highlights.delete(key);
            });
        }

        /**
         * Draws OCR boxes as hoverable, id-tagged overlays and returns a Map id → element.
         * A region's `level` ('block' | 'line' | 'word') becomes a class modifier; pass
         * regions block → line → word so words stack on top and win pointer hits. Every
         * region is drawn and cleared, even with a duplicate id; the map keeps the topmost (last) element per id.
         */
        setTextRegions(regions) {
            this.clearTextRegions();
            const item = this._currentItem();
            const map = new Map();
            if (!item) return map;
            (regions || []).forEach((r) => {
                if (!r.rect) return;
                const el = document.createElement('div');
                el.className = r.level ? `immersive__text-region immersive__text-region-${r.level}` : 'immersive__text-region';
                el.dataset.ivRegionId = r.id;
                this._place(el, item, r.rect);
                this._textRegions.push(el);
                map.set(r.id, el);
            });
            return map;
        }

        clearTextRegions() {
            this._drop(this._textRegions);
            this._textRegions = [];
        }

        /** Draws ALTO layout annotations (drawn lines, note areas) as overlays; each region's `modifier` becomes a class suffix. */
        setAnnotationRegions(regions) {
            this.clearAnnotationRegions();
            const item = this._currentItem();
            const map = new Map();
            if (!item) return map;
            (regions || []).forEach((r) => {
                if (!r.rect) return;
                const el = document.createElement('div');
                el.className = ['immersive__anno-region', r.modifier ? `immersive__anno-region-${r.modifier}` : '', r.className || ''].filter(Boolean).join(' ');
                el.dataset.ivAnnoId = r.id;
                this._place(el, item, r.rect);
                this._annoRegions.push(el);
                map.set(r.id, el);
            });
            return map;
        }

        clearAnnotationRegions() {
            this._drop(this._annoRegions);
            this._annoRegions = [];
        }

        destroy() {
            this.clearHighlights();
            this.clearTextRegions();
            this.clearAnnotationRegions();
        }
    }

    /** The stage placeholder for pages the user may not see, and for a stage that could not be loaded. */

    /**
     * Builds the placeholder box: the server-rendered template's children when given, else a plain text.
     *
     * @param {object} opts
     * @param {HTMLElement|null} [opts.template]  element whose children are cloned (admin-configured image and text)
     * @param {string} [opts.text]  fallback text
     * @param {string} [opts.role='status']
     * @returns {HTMLElement}
     */
    function buildDeniedPlaceholder({ template, text, role = 'status' }) {
        const box = document.createElement('div');
        box.className = 'immersive__page-denied';
        box.setAttribute('role', role);
        const inner = document.createElement('div');
        inner.className = 'immersive__page-denied-inner';
        if (template && template.children.length) {
            for (const child of template.children) inner.appendChild(child.cloneNode(true));
        } else {
            const span = document.createElement('span');
            span.className = 'immersive__page-denied-text';
            span.textContent = text || '';
            inner.appendChild(span);
        }
        box.appendChild(inner);
        return box;
    }

    /**
     * The access-denied overlay of the stage: built once on first need, then toggled.
     *
     * @param {object} opts
     * @param {HTMLElement|null} opts.host  the OSD mount element
     * @param {HTMLElement|null} [opts.template]
     * @param {string} [opts.text]
     * @returns {{sync: (denied: boolean) => void, remove: Function}}
     */
    function createAccessOverlay({ host, template, text }) {
        let overlay = null;
        return {
            sync(denied) {
                if (!host) return;
                if (!denied) {
                    if (overlay) overlay.hidden = true;
                    return;
                }
                if (!overlay) {
                    overlay = buildDeniedPlaceholder({ template, text });
                    if (getComputedStyle(host).position === 'static') host.style.position = 'relative';
                    host.appendChild(overlay);
                }
                overlay.hidden = false;
            },
            remove() {
                if (overlay) overlay.remove();
                overlay = null;
            },
        };
    }

    /** Browser libraries the immersive view relies on, read lazily from window so tests can stub them. */

    const win = () => (typeof window !== 'undefined' ? window : {});

    const env = {
        /** OpenSeadragon wrapper from imageView.min.js. */
        get ImageView() {
            return win().ImageView;
        },
        get riot() {
            return win().riot;
        },
        get rxjs() {
            return win().rxjs;
        },
        /** jQuery, needed for the Bootstrap popover API. */
        get $() {
            return win().$ || win().jQuery;
        },
        /** Legacy core namespace (TOC widget). */
        get viewerJS() {
            return win().viewerJS;
        },
        /** Web Storage; the property read itself throws when the browser blocks it. */
        get localStorage() {
            return win().localStorage;
        },
    };

    /**
     * OSD viewport margins that keep the fitted page clear of the floating chrome.
     * Desktop reserves generous side margins; small viewports shrink them so the
     * page actually uses the screen (bars stay clear via the top/bottom values:
     * title pill 36px + inset, bottom bar 36-40px + inset). Pure + tested.
     *
     * @param {number} viewportWidth  window.innerWidth at viewer construction
     * @returns {{top:number, bottom:number, left:number, right:number}}
     */
    function stageMargins(viewportWidth) {
        if (viewportWidth <= 768) {
            return { top: 56, bottom: 60, left: 16, right: 16 };
        }
        return { top: 64, bottom: 72, left: 64, right: 64 };
    }

    /** Minimal dependency-free event emitter (rxjs-compatible `subscribe` shape). */
    class Emitter {
        constructor() {
            this._subs = new Set();
        }
        subscribe(fn) {
            this._subs.add(fn);
            return () => this._subs.delete(fn);
        }
        emit(value) {
            this._subs.forEach((fn) => fn(value));
        }
        clear() {
            this._subs.clear();
        }
    }

    /** Maps a IIIF image-service base id to an OSD tile source (its info.json URL). */
    function toTileSource(serviceId) {
        return serviceId.endsWith('/info.json') ? serviceId : `${serviceId}/info.json`;
    }

    /**
     * Default single-image sequence config (mirrors zoomableImage.mjs); _arrangeImageSequence
     * reads it on every open(). Copied per instance because _open() mutates `columns`.
     */
    const SEQUENCE_DEFAULTS = { columns: 1, useWindowing: true, windowSize: 100, windowExpandThreshold: 10, windowExpandSize: 50 };
    const PREFETCH_RADIUS = 1;
    const ZOOM_STEP = 1.5;
    const FADE_MS = 160;

    /**
     * Last-resort release for the navigation lock. Double-page navigation re-opens the library
     * (a fresh OpenSeadragon per spread) and only settles on its async 'open' event; if rapid
     * reopens lose that event the promise never settles. Freeing the lock after this window
     * keeps the viewer from staying frozen. Long enough not to abort a genuinely slow load.
     */
    const NAV_WATCHDOG_MS = 8000;

    /**
     * How long a navigation waits for the target page to paint its first tile. Shorter than
     * NAV_WATCHDOG_MS on purpose: a dead tile source then rolls the navigation back (the page
     * on screen stays) instead of leaving promise, handlers and lock to the watchdog.
     */
    const CONTENT_TIMEOUT_MS = 6000;

    /**
     * Immersive image viewer engine around a single live ImageView.Image (OSD).
     * Single pages are swapped flicker-free by fading in a preloaded neighbour; double
     * pages are composed by the library and transitioned with a snapshot crossfade.
     * Consumers subscribe to `onPageChange` / `onOpen`.
     */
    class IvViewer {
        /**
         * @param {object} opts
         * @param {HTMLElement} opts.element   OSD mount element
         * @param {string[]} opts.services     ordered IIIF image-service URLs (all pages)
         * @param {number} [opts.startOrder=0] initial 0-based page index
         * @param {number} [opts.maxZoom]
         * @param {HTMLElement} [opts.deniedContent]  server-rendered placeholder content for image-restricted pages
         */
        constructor(opts) {
            this.services = opts.services;
            this.total = opts.services.length;
            this.current = Math.max(0, Math.min(opts.startOrder ?? 0, this.total - 1));
            this.allowZoom = opts.allowZoom !== false;
            this.restricted = Array.isArray(opts.restricted) ? opts.restricted : [];
            this.deniedText = opts.deniedText || '';
            this.double = false;
            this.currentItem = null;
            this._anchor = null;
            this._preloaded = new Map();
            this._queuedNav = null;
            this._queuedReopen = false;
            this._lock = createNavLock(opts.navWatchdogMs ?? NAV_WATCHDOG_MS);
            this._lock.onRelease(() => this._runQueued());
            this._contentTimeoutMs = opts.contentTimeoutMs ?? CONTENT_TIMEOUT_MS;
            this._nav = 0;
            this._cancelContentWait = null;
            this._destroyed = false;
            this._fadeMs = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : FADE_MS;
            this.onPageChange = new Emitter();
            this.onOpen = new Emitter();
            this.onReset = new Emitter();

            this.viewer = new env.ImageView.Image({
                element: opts.element,
                fittingMode: 'fixed',
                margins: stageMargins(window.innerWidth),
                zoom: { enabled: true, max: opts.maxZoom },
                sequence: { ...SEQUENCE_DEFAULTS },
                navigator: { enabled: false },
            });
            this.viewer.openseadragon.crossOriginPolicy = 'Anonymous';
            this.zoom = new env.ImageView.Controls.Zoom(this.viewer);
            this.rotation = new env.ImageView.Controls.Rotation(this.viewer);
            this._overlays = new IvOverlays({
                osd: () => this.viewer.openseadragon,
                itemFor: (order) => this._itemForOrder(order),
                currentItem: () => this.currentItem || this.viewer.openseadragon.world.getItemAt(0),
            });
            this._access = createAccessOverlay({ host: this.viewer.element, template: opts.deniedContent || null, text: this.deniedText });

            if (!this.allowZoom) {
                const osd = this.viewer.openseadragon;
                osd.zoomPerScroll = 1;
                osd.zoomPerClick = 1;
                ['gestureSettingsMouse', 'gestureSettingsTouch', 'gestureSettingsPen', 'gestureSettingsUnknown'].forEach((k) => {
                    const g = osd[k];
                    if (g) {
                        g.scrollToZoom = false;
                        g.clickToZoom = false;
                        g.dblClickToZoom = false;
                        g.pinchToZoom = false;
                    }
                });
            }

            this.viewer.openseadragon.addHandler('canvas-key', (e) => {
                const key = e.originalEvent.key;
                if (key === 'ArrowRight') this.next();
                else if (key === 'ArrowLeft') this.prev();
                else if (key === '0') {
                    this.resetView();
                    e.preventDefaultAction = true;
                    e.originalEvent.preventDefault();
                }
                if (['ArrowLeft', 'ArrowRight'].includes(key)) e.preventDefaultAction = true;
                if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(key)) e.originalEvent.preventDefault();
                if (!this.allowZoom && ['-', '_', '+', '='].includes(key)) e.preventDefaultAction = true;
            });

            this._open(this.current)
                .then(() => this._refreshPreload())
                .catch((e) => console.error('immersive viewer initial open failed', e));
        }

        /**
         * TiledImage rendering `order` in the current view, or null if that page is not
         * shown. In double mode the spread's pages map to world items 0/1 (see _open);
         * without an `order` the single current item is used.
         */
        _itemForOrder(order) {
            const osd = this.viewer.openseadragon;
            if (order == null) return this.currentItem || osd.world.getItemAt(0);
            const idx = this.getCurrentPages().indexOf(order);
            if (idx === -1) return null;
            return this.double ? osd.world.getItemAt(idx) : this.currentItem || osd.world.getItemAt(0);
        }

        /** Highlight rectangles for `owner` (search hits, layout-tag occurrences); see IvOverlays. */
        setHighlights(rects, owner = 'default') {
            this._overlays.setHighlights(rects, owner);
        }

        /** Removes the highlights of `owner`, or of every owner when omitted. */
        clearHighlights(owner) {
            this._overlays.clearHighlights(owner);
        }

        /** OCR boxes as hoverable, id-tagged overlays; returns a Map id → element. */
        setTextRegions(regions) {
            return this._overlays.setTextRegions(regions);
        }

        clearTextRegions() {
            this._overlays.clearTextRegions();
        }

        /** ALTO layout annotations (drawn lines, note areas) as overlays; returns a Map id → element. */
        setAnnotationRegions(regions) {
            return this._overlays.setAnnotationRegions(regions);
        }

        clearAnnotationRegions() {
            this._overlays.clearAnnotationRegions();
        }

        /** Current 0-based page order (the spread leader in double mode). */
        getCurrentOrder() {
            return this.current;
        }

        /** Total page count. */
        getPageCount() {
            return this.total;
        }

        /** Whether book-spread (double-page) mode is active. */
        isDoublePage() {
            return this.double;
        }

        /** 0-based page indices currently displayed (one page, or two in double mode). */
        getCurrentPages() {
            return framePages(this.current, this.total, { double: this.double });
        }

        /** Navigate to the page/spread containing `order` (snaps to the spread leader in double mode). */
        goToPage(order) {
            const target = Math.max(0, Math.min(order, this.total - 1));
            if (this._navigating) {
                this._queuedNav = target;
                return;
            }
            if (this.double) {
                const leader = computeSpread(target, this.total)[0];
                if (leader === this.current) return;
                this._navigateSpread(leader);
                return;
            }
            if (target === this.current) return;
            this._crossfadeTo(target);
        }

        /** Pages forward: the next page, or the page after the current spread in double mode. */
        next() {
            if (this.double) {
                const pages = computeSpread(this.current, this.total);
                this.goToPage(pages[pages.length - 1] + 1);
            } else {
                this.goToPage(this.current + 1);
            }
        }

        /** Pages one page back (snaps to the containing spread in double mode). */
        prev() {
            this.goToPage(this.current - 1);
        }

        /** Whether a navigation is in flight (queued targets wait for the release). */
        get _navigating() {
            return this._lock.busy;
        }

        /**
         * Runs `run` under the navigation lock; `onStuck` cleans up if the watchdog fires (see ivNavLock).
         * `run` receives a `stale()` predicate that turns true once a later navigation has taken the lock,
         * so a body resuming after a watchdog release drops its result instead of writing state for a page
         * that is no longer on screen.
         */
        _withNavLock(run, onStuck) {
            this._lock.run(() => {
                const generation = ++this._nav;
                return run(() => this._nav !== generation);
            }, onStuck);
        }

        /** After a release: a queued mode reopen first, else the latest queued page target. */
        _runQueued() {
            if (this._queuedReopen) {
                this._queuedReopen = false;
                this._reopen();
                return;
            }
            const next = this._queuedNav;
            this._queuedNav = null;
            if (next != null) this.goToPage(next);
        }

        /** Re-opens the current position in the current mode, serialised through the nav lock. */
        _reopen() {
            this._withNavLock((stale) => this._open(this.current, stale));
        }

        zoomIn() {
            if (!this.allowZoom) return;
            this.zoom.zoomBy(ZOOM_STEP);
        }

        zoomOut() {
            if (!this.allowZoom) return;
            this.zoom.zoomBy(1 / ZOOM_STEP);
        }

        rotateLeft() {
            this.rotation.rotateLeft();
        }

        rotateRight() {
            this.rotation.rotateRight();
        }

        /**
         * Resets rotation and zoom to fit the whole page (whole spread in double mode).
         * Always goes through the library's zoom control: it fits the union of the
         * current row with margin compensation, while raw OSD viewport.goHome() FILLS
         * the viewport (homeFillsViewer) and would land on a zoomed-in spread.
         * Emits `onReset` so UI add-ons (image filters) can reset along with the view.
         */
        resetView() {
            this.rotation.rotateTo(0);
            this.zoom.goHome();
            this.onReset.emit();
        }

        /** Toggles book-spread mode and re-opens at the current position. Returns the new state. */
        toggleDoublePage() {
            this.double = !this.double;
            this.current = this.getCurrentPages()[0];
            if (this._navigating) {
                this._queuedReopen = true;
            } else {
                this._reopen();
            }
            return this.double;
        }

        /**
         * Single-page navigation: fade the (preloaded or freshly added) target page in
         * over the current one, then drop the old. No reload, no blank, no jump. A load
         * that only arrives after its watchdog release is dropped, not applied.
         */
        _crossfadeTo(target) {
            this._withNavLock(async (stale) => {
                const previous = this.currentItem;
                const from = this.current;
                this.current = target;
                if (this.isPageRestricted(target)) {
                    this._drop(previous);
                    this.currentItem = null;
                    this._emit();
                    this._refreshPreload();
                    return;
                }
                let item;
                try {
                    item = await this._acquire(target, this._anchor);
                    this._preloaded.delete(target);
                    await this._whenContent(item);
                } catch (e) {
                    this._preloaded.delete(target);
                    this._drop(item);
                    if (!stale()) this.current = from;
                    throw e;
                }
                if (stale()) {
                    this._drop(item);
                    return;
                }
                await crossfade(item, previous, this._fadeMs);
                this._drop(previous);
                this.currentItem = item;
                this._emit();
                this._refreshPreload();
            });
        }

        /**
         * Returns a cached promise for the TiledImage of `order`, adding it hidden and
         * preloaded (at `bounds`) if not already present/in-flight. Caching by order lets
         * an in-flight preload and an on-demand navigation share one image; it resolves as
         * soon as the image is added (not when fully loaded).
         */
        _acquire(order, bounds) {
            if (this._preloaded.has(order)) return this._preloaded.get(order);
            const osd = this.viewer.openseadragon;
            const anchor = bounds || (this.currentItem ? this.currentItem.getBounds() : undefined);
            const p = new Promise((resolve, reject) => {
                osd.addTiledImage({
                    tileSource: toTileSource(this.services[order]),
                    opacity: 0,
                    preload: true,
                    fitBounds: anchor,
                    success: (e) => resolve(e.item),
                    error: reject,
                });
            });
            this._preloaded.set(order, p);
            return p;
        }

        /**
         * Resolves once `item` has painted its first (low-res) tile, or is already fully
         * loaded; rejects when no tile arrives within `contentTimeoutMs`. Either way the
         * tile handlers are detached, so a page that never paints leaves nothing behind.
         * destroy() resolves a pending wait early; the navigation then sees itself stale.
         */
        _whenContent(item) {
            if (item.getFullyLoaded()) return Promise.resolve();
            const osd = this.viewer.openseadragon;
            return new Promise((resolve, reject) => {
                let done = false;
                let timer = null;
                const finish = (error) => {
                    if (done) return;
                    done = true;
                    clearTimeout(timer);
                    osd.removeHandler('tile-loaded', onTile);
                    item.removeHandler('fully-loaded-change', onFull);
                    this._cancelContentWait = null;
                    if (error) reject(error);
                    else resolve();
                };
                this._cancelContentWait = () => finish();
                const onTile = (e) => {
                    if (e.tiledImage === item) finish();
                };
                const onFull = (ev) => {
                    if (ev.fullyLoaded) finish();
                };
                osd.addHandler('tile-loaded', onTile);
                item.addHandler('fully-loaded-change', onFull);
                timer = setTimeout(() => finish(new Error(`immersive: no tile painted within ${this._contentTimeoutMs}ms`)), this._contentTimeoutMs);
            });
        }

        /** Removes an abandoned image from the world -- a page that never made it on screen must not stay in it. */
        _drop(item) {
            if (!item) return;
            try {
                this.viewer.openseadragon.world.removeItem(item);
            } catch {}
        }

        /** Keeps the adjacent pages (±1) preloaded and evicts the rest (single-page path). */
        _refreshPreload() {
            const keep = new Set(residentPages(this.current, this.total, { double: this.double }));
            for (const order of keep) {
                if (order === this.current) continue;
                if (this.isPageRestricted(order)) continue;
                if (!this._preloaded.has(order)) {
                    this._acquire(order, this._anchor).catch(() => this._preloaded.delete(order));
                }
            }
            for (const [order, p] of this._preloaded) {
                if (!keep.has(order)) {
                    p.then((item) => this._drop(item)).catch(() => {});
                    this._preloaded.delete(order);
                }
            }
        }

        /**
         * Double-page navigation: freeze the current spread as a snapshot overlay, let the
         * library re-compose the new spread (columns:2) underneath, then fade the snapshot
         * out. Neighbour spreads are tile-prewarmed for near-instant sharpness.
         */
        _navigateSpread(leader) {
            const overlay = snapshotOverlay(this.viewer.openseadragon, this.viewer.element);
            this.current = leader;
            this._withNavLock(
                async (stale) => {
                    try {
                        await this._open(leader, stale);
                    } finally {
                        fadeOverlay(overlay, this._fadeMs);
                    }
                    if (!stale()) this._prewarmSpreads();
                },
                () => {
                    if (overlay) overlay.remove();
                }
            );
        }

        /**
         * Warms OSD's tile cache for the neighbouring spreads (hidden preloaded images) so
         * the next spread renders almost immediately. The temp images are invisible and are
         * cleared by the next open(); their tiles stay cached.
         */
        _prewarmSpreads() {
            const here = computeSpread(this.current, this.total);
            const neighbours = [...computeSpread(here[0] - 1, this.total), ...computeSpread(here[here.length - 1] + 1, this.total)];
            const osd = this.viewer.openseadragon;
            for (const p of new Set(neighbours)) {
                if (p < 0 || p >= this.total || here.includes(p)) continue;
                if (this.isPageRestricted(p)) continue;
                osd.addTiledImage({ tileSource: toTileSource(this.services[p]), opacity: 0, preload: true });
            }
        }

        /**
         * Loads the page(s) for `order` via the library (a single page, or a columns:2
         * spread in double mode) and captures the single-page fit anchor. Used for the
         * initial open, mode toggles and every double-page spread change. Emits
         * `onOpen` once the library has settled, so consumers bound to the library's
         * per-load event pipeline (image filters) can re-attach. A load settling after
         * `stale()` turned true (watchdog release, destroy) writes no state and emits nothing;
         * a rejected load of an unrestricted page is logged.
         *
         * @param {number} order
         * @param {() => boolean} [stale]
         */
        _open(order, stale = () => false) {
            const pages = framePages(order, this.total, { double: this.double });
            this.viewer.config.sequence.columns = pages.length;
            const sources = pages.map((p) => toTileSource(this.services[p]));
            const kept = this._captureView();
            const settle = () => {
                if (stale() || this._destroyed) return;
                const world = this.viewer.openseadragon.world;
                this.currentItem = world.getItemCount() > 0 ? world.getItemAt(0) : null;
                if (!this.double && this.currentItem) {
                    this._anchor = heightBand(this.currentItem.getBounds());
                }
                this._restoreView(kept);
                this._preloaded.clear();
                this._emit();
                this.onOpen.emit(this.current);
            };
            const loaded = this.viewer.load(sources, 0);
            this._prefetchAround(pages[pages.length - 1]);
            return loaded.then(settle, (e) => {
                if (!pages.some((p) => this.isPageRestricted(p))) console.warn('immersive: page load failed', e);
                settle();
            });
        }

        /**
         * The page row on screen: union bounds and page count. Neighbour pages sit preloaded in the
         * same world at opacity 0 (see {@link _acquire}), so counting world items would report a row
         * that is neither the right size nor the right shape.
         */
        _visibleRow() {
            const world = this.viewer.openseadragon.world;
            let bounds = null;
            let pages = 0;
            for (let i = 0; i < world.getItemCount(); i++) {
                const item = world.getItemAt(i);
                if (item.getOpacity && item.getOpacity() <= 0) {
                    continue;
                }
                const itemBounds = item.getBounds();
                bounds = bounds ? bounds.union(itemBounds) : itemBounds;
                pages++;
            }
            return { bounds, pages };
        }

        /**
         * The view as it stands before a reload, relative to the page row, plus the rotation.
         * Reads the animation targets, so paging mid-zoom keeps where the zoom was heading
         * rather than the frame it happened to be on.
         */
        _captureView() {
            try {
                const viewport = this.viewer.openseadragon.viewport;
                // The page count comes from the row on screen, not from the mode: on a mode toggle
                // `double` already holds the new value while the row is still the old one.
                const row = this._visibleRow();
                const view = captureView(row.bounds, viewport.getCenter(false), viewport.getZoom(false), row.pages);
                return view ? { ...view, degrees: viewport.getRotation ? viewport.getRotation() : 0 } : null;
            } catch (e) {
                return null;
            }
        }

        /**
         * Re-applies a captured view after the library has rebuilt the world. Measures the new
         * row the same way the capture measured the old one, so a cover shown alone or a page
         * without an image compares honestly. Silent about every failure: the library has
         * already fitted the new row, so doing nothing leaves a correct view, while throwing
         * in here would strand the navigation lock.
         */
        _restoreView(kept) {
            try {
                const row = this._visibleRow();
                const target = restoredView(kept, row.bounds, row.pages);
                if (!target) return;
                const viewport = this.viewer.openseadragon.viewport;
                if (kept.degrees) this.rotation.rotateTo(kept.degrees);
                viewport.zoomTo(target.zoom, null, true);
                viewport.panTo(target.center, true);
            } catch (e) {
                console.warn('immersive: could not keep the view across the reload', e);
            }
        }

        /** Warms neighbour info.json in the browser cache so the next load is faster. */
        _prefetchAround(order) {
            for (let d = 1; d <= PREFETCH_RADIUS; d++) {
                for (const o of [order - d, order + d]) {
                    if (o >= 0 && o < this.total && !this.isPageRestricted(o)) {
                        fetch(toTileSource(this.services[o])).catch(() => {});
                    }
                }
            }
        }

        _emit() {
            this._syncAccessOverlay();
            this.onPageChange.emit(this.current);
        }

        /** True when the page at `order` carries a server auth service (image not viewable). */
        isPageRestricted(order) {
            return this.restricted[order] === true;
        }

        _syncAccessOverlay() {
            this._access.sync(this.getCurrentPages().some((o) => this.isPageRestricted(o)));
        }

        /** Runs `fn` once when the first tile has been painted. */
        onceTileLoaded(fn) {
            this.viewer.openseadragon.addOnceHandler('tile-loaded', fn);
        }

        /** Runs `fn` on the next viewport resize; returns a cancel function. */
        onceResized(fn) {
            const osd = this.viewer.openseadragon;
            const handler = () => {
                osd.removeHandler('resize', handler);
                fn();
            };
            osd.addHandler('resize', handler);
            return () => osd.removeHandler('resize', handler);
        }

        /** Forces a redraw of the current frame. */
        redraw() {
            this.viewer.openseadragon.forceRedraw();
        }

        /** 2D context of the drawn canvas, null before the first paint. */
        getCanvasContext() {
            return typeof this.viewer.getCanvasContext === 'function' ? this.viewer.getCanvasContext() : null;
        }

        /**
         * Runs `fn` after every drawn frame. The library re-runs its own event wiring
         * on every load() against the same OpenSeadragon instance; attaching once per
         * instance keeps this handler from stacking, and a replaced instance is picked
         * up on onOpen. Returns a detach function.
         */
        onFrame(fn) {
            let bound = null;
            const attach = () => {
                const osd = this.viewer.openseadragon;
                if (!osd || bound === osd) return;
                bound = osd;
                osd.addHandler('update-viewport', fn);
            };
            attach();
            const unsubscribe = this.onOpen.subscribe(attach);
            return () => {
                unsubscribe();
                if (bound && typeof bound.removeHandler === 'function') bound.removeHandler('update-viewport', fn);
            };
        }

        /** Releases the OpenSeadragon instance, the access overlay and all subscriptions; in-flight navigations turn stale. */
        destroy() {
            this._destroyed = true;
            this._nav++;
            if (this._cancelContentWait) this._cancelContentWait();
            this._lock.destroy();
            this._preloaded.clear();
            this._access.remove();
            this._overlays.destroy();
            if (typeof this.viewer.close === 'function') this.viewer.close();
            const osd = this.viewer.openseadragon;
            if (osd && typeof osd.destroy === 'function') osd.destroy();
            [this.onPageChange, this.onOpen, this.onReset].forEach((emitter) => emitter.clear());
        }
    }

    const cache = new Map();

    /**
     * Resolves a service object or array to a single id string.
     *
     * @param {object|Array} service
     * @param {'@id'|'id'} primaryKey   preferred property name
     * @param {'@id'|'id'} fallbackKey  secondary property name
     * @returns {string|null} the id, or null when none can be found
     */
    function _resolveServiceId(service, primaryKey, fallbackKey) {
        if (!service) return null;
        const entry = Array.isArray(service) ? service[0] : service;
        if (!entry) return null;
        const id = entry[primaryKey] || entry[fallbackKey];
        return typeof id === 'string' && id.length > 0 ? id : null;
    }

    /**
     * Resolves a IIIF canvas `label` (plain string, v2 `{'@value'}`, array, or v3
     * language map) to one display string: preferred language first, then
     * `none`/`@none`, then the first non-empty value. Returns '' when nothing matches.
     *
     * @param {string|object|Array} label
     * @param {string} [lang] preferred language code (e.g. the UI language)
     * @returns {string}
     */
    function _resolveCanvasLabel(label, lang) {
        if (label == null) return '';
        if (typeof label === 'string') return label;
        if (Array.isArray(label)) {
            for (const item of label) {
                const v = _resolveCanvasLabel(item, lang);
                if (v) return v;
            }
            return '';
        }
        if (typeof label === 'object') {
            if (typeof label['@value'] === 'string' && label['@value'].length > 0) return label['@value'];
            for (const key of [lang, 'none', '@none']) {
                if (key && label[key]) {
                    const v = _resolveCanvasLabel(label[key], lang);
                    if (v) return v;
                }
            }
            for (const v of Object.values(label)) {
                const resolved = _resolveCanvasLabel(v, lang);
                if (resolved) return resolved;
            }
        }
        return '';
    }

    /**
     * Detects whether an image-service carries a IIIF authentication/probe service,
     * which the server attaches only when the current user may not view the image
     * (see SequenceBuilder). Its presence is the server-authoritative per-page
     * "image restricted" signal.
     *
     * @param {object|Array} service the image resource's `service` value
     * @returns {boolean} true when a nested auth/probe service is present
     */
    function _serviceIsRestricted(service) {
        const entry = Array.isArray(service) ? service[0] : service;
        if (!entry || typeof entry !== 'object') return false;
        const nested = entry.service;
        const list = Array.isArray(nested) ? nested : nested ? [nested] : [];
        return list.some((s) => {
            if (!s || typeof s !== 'object') return false;
            const type = String(s.type || s['@type'] || '');
            const profile = String(s.profile || '');
            const context = String(s['@context'] || '');
            return /auth/i.test(type) || /auth/i.test(profile) || /\/auth\//i.test(context);
        });
    }

    /**
     * Extracts ordered `{id, label, restricted}` entries from a manifest's canvases
     * (v2 sequences/canvases or v3 items). Canvases without a resolvable image-service
     * id are skipped entirely, so service URLs, page labels and access flags stay
     * index-aligned.
     *
     * @param {object} manifest
     * @param {string} [lang] preferred label language
     * @returns {Array<{id: string, label: string, restricted: boolean}>}
     */
    function _parseManifestCanvasEntries(manifest, lang) {
        if (!manifest || typeof manifest !== 'object') return [];

        if (Array.isArray(manifest.sequences) && manifest.sequences.length > 0) {
            const canvases = manifest.sequences[0].canvases;
            if (!Array.isArray(canvases)) return [];

            const entries = [];
            for (const canvas of canvases) {
                try {
                    const service = canvas.images[0].resource.service;
                    const id = _resolveServiceId(service, '@id', 'id');
                    if (id !== null) {
                        entries.push({ id, label: _resolveCanvasLabel(canvas.label, lang), restricted: _serviceIsRestricted(service) });
                    }
                } catch {}
            }
            return entries;
        }

        if (Array.isArray(manifest.items) && manifest.items.length > 0) {
            const entries = [];
            for (const canvas of manifest.items) {
                try {
                    const service = canvas.items[0].items[0].body.service;
                    const id = _resolveServiceId(service, 'id', '@id');
                    if (id !== null) {
                        entries.push({ id, label: _resolveCanvasLabel(canvas.label, lang), restricted: _serviceIsRestricted(service) });
                    }
                } catch {}
            }
            return entries;
        }

        return [];
    }

    /**
     * Extracts the ordered list of IIIF image-service base IDs from a manifest
     * (Presentation API v2 and v3).
     *
     * @param {object} manifest parsed IIIF Presentation manifest
     * @returns {string[]}
     */
    function parseManifestImageServices(manifest) {
        return _parseManifestCanvasEntries(manifest).map((e) => e.id);
    }

    /**
     * Extracts the ordered list of canvas labels from a manifest, index-aligned with
     * {@link parseManifestImageServices}. Canvases without a resolvable label yield ''.
     *
     * @param {object} manifest parsed IIIF Presentation manifest
     * @param {string} [lang]   preferred label language for v3 language maps
     * @returns {string[]}
     */
    function parseManifestPageLabels(manifest, lang) {
        return _parseManifestCanvasEntries(manifest, lang).map((e) => e.label);
    }

    /**
     * Extracts the ordered per-page "image restricted" flags, index-aligned with
     * {@link parseManifestImageServices}. true means the page carries an auth service
     * (the user may not view the image); its tiles will 403.
     *
     * @param {object} manifest parsed IIIF Presentation manifest
     * @returns {boolean[]}
     */
    function parseManifestPageAccess(manifest) {
        return _parseManifestCanvasEntries(manifest).map((e) => e.restricted);
    }

    /**
     * Fetches (and memoizes per pi) the parsed IIIF Presentation manifest, so
     * services and labels share a single network request. A failed request is
     * evicted from the cache so a transient error doesn't poison it; the
     * rejection is rethrown.
     *
     * @param {string} pi        Goobi viewer record identifier
     * @param {string} apiBase   base URL of the REST API (no trailing slash)
     * @param {Function} fetchFn fetch-compatible function (injectable for tests)
     * @returns {Promise<object>} the parsed manifest JSON
     */
    function loadManifest(pi, apiBase, fetchFn = fetch) {
        if (cache.has(pi)) {
            return cache.get(pi);
        }

        const url = `${apiBase}/records/${pi}/manifest`;
        const promise = fetchFn(url).then((res) => {
            if (!res.ok) {
                throw new Error(`Failed to load manifest for "${pi}": HTTP ${res.status}`);
            }
            return res.json();
        });

        cache.set(pi, promise);

        return promise.catch((e) => {
            cache.delete(pi);
            throw e;
        });
    }

    /**
     * Returns the ordered list of image-service base URLs for all pages of a record.
     *
     * @param {string} pi        Goobi viewer record identifier
     * @param {string} apiBase   base URL of the REST API (no trailing slash)
     * @param {Function} fetchFn fetch-compatible function (injectable for tests)
     * @returns {Promise<string[]>}
     */
    function loadPageServices(pi, apiBase, fetchFn = fetch) {
        return loadManifest(pi, apiBase, fetchFn).then(parseManifestImageServices);
    }

    /**
     * Returns the ordered list of canvas labels for all pages, index-aligned with
     * {@link loadPageServices}. Shares the memoized manifest fetch.
     *
     * @param {string} pi        Goobi viewer record identifier
     * @param {string} apiBase   base URL of the REST API (no trailing slash)
     * @param {Function} fetchFn fetch-compatible function (injectable for tests)
     * @param {string} [lang]    preferred label language for v3 language maps
     * @returns {Promise<string[]>}
     */
    function loadPageLabels(pi, apiBase, fetchFn = fetch, lang) {
        return loadManifest(pi, apiBase, fetchFn).then((manifest) => parseManifestPageLabels(manifest, lang));
    }

    /**
     * Returns the ordered per-page "image restricted" flags for all pages,
     * index-aligned with {@link loadPageServices}. Shares the memoized manifest fetch.
     *
     * @param {string} pi        Goobi viewer record identifier
     * @param {string} apiBase   base URL of the REST API (no trailing slash)
     * @param {Function} fetchFn fetch-compatible function (injectable for tests)
     * @returns {Promise<boolean[]>}
     */
    function loadPageAccess(pi, apiBase, fetchFn = fetch) {
        return loadManifest(pi, apiBase, fetchFn).then(parseManifestPageAccess);
    }

    function _parseXywh(on) {
        if (!on) return null;
        const candidates =
            typeof on === 'string' ? [on] : [typeof on['@id'] === 'string' ? on['@id'] : '', on.selector && typeof on.selector.value === 'string' ? on.selector.value : ''];
        for (const c of candidates) {
            const m = c && c.match(/xywh=(\d+),(\d+),(\d+),(\d+)/);
            if (m) return { x: +m[1], y: +m[2], w: +m[3], h: +m[4] };
        }
        return null;
    }

    /**
     * Maps a IIIF/W3C `sc:AnnotationList` to one structured line per OCR annotation:
     * the box comes from the `xywh` fragment on the annotation's `on` selector
     * (rect: null without one), the id from `@id` with a per-page index fallback.
     *
     * @param {object} annotationList the parsed `sc:AnnotationList` JSON
     * @returns {{id:string, chars:string, rect:{x:number,y:number,w:number,h:number}|null}[]}
     */
    function parsePageLines(annotationList) {
        const lines = (annotationList && annotationList.resources) || [];
        return lines.map((a, i) => ({
            id: (a && a['@id']) || `line-${i}`,
            chars: (a && a.resource && a.resource.chars) || '',
            rect: _parseXywh(a && a.on),
        }));
    }

    /**
     * Fetches one page's text annotations and parses them into structured lines.
     *
     * @param {string} pi
     * @param {string} apiBase
     * @param {number} order    0-based page order; the endpoint is 1-based, so order + 1
     * @param {Function} fetchFn
     * @param {string} query    optional query string appended to the endpoint URL
     * @returns {Promise<{id:string, chars:string, rect:object|null}[]>} lines, or [] on failure
     */
    async function _fetchPageLines(pi, apiBase, order, fetchFn, query = '') {
        const res = await fetchFn(`${apiBase}/records/${pi}/pages/${order + 1}/text/${query}`);
        if (!res.ok) {
            return null;
        }
        return parsePageLines(await res.json());
    }

    /** Whether the rect's centre point lies inside `outer`. */
    function _centerInside(rect, outer) {
        if (!rect || !outer) return false;
        const cx = rect.x + rect.w / 2;
        const cy = rect.y + rect.h / 2;
        return cx >= outer.x && cx < outer.x + outer.w && cy >= outer.y && cy < outer.y + outer.h;
    }

    /**
     * The container in `parents` whose rect holds the region's centre; overlapping
     * containers resolve to the smallest (most specific) one. Null without a match.
     */
    function _findParent(region, parents) {
        let best = null;
        for (const p of parents) {
            if (!_centerInside(region.rect, p.rect)) continue;
            if (!best || p.rect.w * p.rect.h < best.rect.w * best.rect.h) best = p;
        }
        return best;
    }

    /** True when the block list is just the line list again (line fallback of APIs without block support). */
    function _blocksAreLines(blocks, lines) {
        if (!blocks.length || blocks.length !== lines.length) return false;
        return blocks.every((b, i) => {
            const a = b.rect;
            const c = lines[i].rect;
            return (!a && !c) || (a && c && a.x === c.x && a.y === c.y && a.w === c.w && a.h === c.h);
        });
    }

    /**
     * Nests the three flat OCR levels into blocks > lines > words by geometric
     * containment (centre-inside, smallest container wins). Reading order follows
     * the line order: consecutive lines sharing an owner form one group, lines
     * without a containing block collect in synthetic blocks (`id: null`), and a
     * line without a rect stays with its predecessor. Blank words and words outside
     * every line are dropped; a degenerate block list that merely mirrors the lines
     * (line fallback of APIs without block granularity) is ignored. Pure + tested.
     *
     * @param {object} levels
     * @param {{id:string, chars:string, rect:object|null}[]} levels.blocks
     * @param {{id:string, chars:string, rect:object|null}[]} levels.lines
     * @param {{id:string, chars:string, rect:object|null}[]} levels.words
     * @returns {Array<{id:string|null, chars:string, rect:object|null, lines:Array}>}
     */
    function nestTextLevels({ blocks = [], lines = [], words = [] }) {
        const realBlocks = _blocksAreLines(blocks, lines) ? [] : blocks.filter((b) => b.rect);
        const visibleWords = words.filter((w) => (w.chars || '').trim() !== '');

        const result = [];
        let currentOwner;
        let group = null;
        for (const line of lines) {
            const owner = line.rect ? _findParent(line, realBlocks) : group ? currentOwner : null;
            if (!group || owner !== currentOwner) {
                currentOwner = owner;
                group = owner ? { ...owner, lines: [] } : { id: null, chars: '', rect: null, lines: [] };
                result.push(group);
            }
            group.lines.push({ ...line, words: [] });
        }

        const allLines = result.flatMap((b) => b.lines);
        for (const word of visibleWords) {
            const line = _findParent(word, allLines);
            if (line) line.words.push({ ...word });
        }
        return result;
    }

    /**
     * Flattens nested text levels into overlay descriptors — blocks first, then
     * lines, then words, so overlays stack with words on top. Each entry carries
     * its `level` and the `parentId` for cascading highlights; synthetic blocks
     * and regions without a rect are skipped (nothing to draw). Pure + tested.
     *
     * @param {Array} blocks  return value of {@link nestTextLevels}
     * @returns {{id:string, rect:object, level:string, parentId:string|null}[]}
     */
    function flattenTextLevels(blocks) {
        const flat = [];
        const push = (region, level, parentId) => {
            if (region.id && region.rect) flat.push({ id: region.id, rect: region.rect, level, parentId });
        };
        (blocks || []).forEach((b) => push(b, 'block', null));
        (blocks || []).forEach((b) => {
            const blockId = b.id && b.rect ? b.id : null;
            b.lines.forEach((l) => push(l, 'line', blockId));
        });
        (blocks || []).forEach((b) => {
            b.lines.forEach((l) => {
                const lineId = l.id && l.rect ? l.id : null;
                l.words.forEach((w) => push(w, 'word', lineId));
            });
        });
        return flat;
    }

    /**
     * Fetches a page's OCR text at block, line and word granularity in parallel
     * and nests it via {@link nestTextLevels}. A failing level degrades gracefully
     * to the remaining ones (older APIs without block support fall back to line
     * annotations, which the nesting detects and ignores).
     *
     * @param {string} pi
     * @param {string} apiBase
     * @param {number} order    0-based page order
     * @param {Function} fetchFn
     * @returns {Promise<Array>} nested blocks, see {@link nestTextLevels}
     */
    async function loadPageTextLevels(pi, apiBase, order, fetchFn = fetch) {
        const [blocks, lines, words] = await Promise.all([
            _fetchPageLines(pi, apiBase, order, fetchFn, '?granularity=block'),
            _fetchPageLines(pi, apiBase, order, fetchFn),
            _fetchPageLines(pi, apiBase, order, fetchFn, '?granularity=word'),
        ]);
        if (lines === null) throw new Error('page text unavailable');
        return nestTextLevels({ blocks: blocks || [], lines, words: words || [] });
    }

    /**
     * Normalizes the layout-annotation endpoint response to the three lists the
     * immersive view consumes (author blocks, tagged words, drawn lines); missing
     * lists become empty arrays, other payload fields (e.g. printColumn) are
     * dropped. Null for non-object payloads. Pure + tested.
     *
     * @param {object} json  the /text/layout/ response
     * @returns {{blocks:Array, words:Array, lines:Array}|null}
     */
    function parseLayoutAnnotations(json) {
        if (!json || typeof json !== 'object') return null;
        const list = (value) => (Array.isArray(value) ? value : []);
        return {
            blocks: list(json.blocks),
            words: list(json.words),
            lines: list(json.lines),
        };
    }

    /**
     * Fetches one page's ALTO layout-tag annotations. The rects share the
     * coordinate space of the page text annotations, so both data sets join by
     * ALTO element id. A 404 means the page has no ALTO at all and yields null
     * ("no annotations"); every other HTTP error rejects, so a forbidden or
     * broken endpoint is not silently indistinguishable from an unannotated page.
     *
     * @param {string} pi
     * @param {string} apiBase
     * @param {number} order    0-based page order; the endpoint is 1-based, so order + 1
     * @param {Function} fetchFn
     * @returns {Promise<{blocks:Array, words:Array, lines:Array}|null>}
     */
    async function loadPageLayout(pi, apiBase, order, fetchFn = fetch) {
        const res = await fetchFn(`${apiBase}/records/${pi}/pages/${order + 1}/text/layout/`);
        if (res.status === 404) return null;
        if (!res.ok) throw new Error(`Failed to load layout annotations for "${pi}" page ${order + 1}: HTTP ${res.status}`);
        return parseLayoutAnnotations(await res.json());
    }

    /** Deep-linkable page URLs: history entries per page change, popstate navigation. */

    /**
     * Rewrites the page-number segment of an immersive URL path (or appends it).
     * The PI segment is never treated as the page number. Pure + tested.
     *
     * @param {string} pathname  e.g. "/viewer/immersive/PPN123/4/"
     * @param {string} pi
     * @param {number|string} pageNo  1-based page number
     * @returns {string} rewritten pathname (keeps trailing slash)
     */
    function pageUrlPath(pathname, pi, pageNo) {
        const parts = pathname.split('/');
        for (let i = parts.length - 1; i >= 0; i--) {
            if (/^\d+$/.test(parts[i]) && parts[i] !== String(pi)) {
                parts[i] = String(pageNo);
                return parts.join('/');
            }
        }
        if (parts[parts.length - 1].length === 0) {
            parts[parts.length - 1] = String(pageNo);
            parts.push('');
        } else {
            parts.push(String(pageNo), '');
        }
        return parts.join('/');
    }

    /**
     * URL-sync feature: pushes a history entry per new page (deep-linkable,
     * back/forward steps through pages) and navigates the viewer on popstate. An
     * emit for the page already in the URL (initial open, double-page toggle, a
     * popstate-driven change) replaces the entry instead of pushing a duplicate.
     * The viewer uses 0-based page orders; URLs use 1-based page numbers.
     *
     * @param {{onPageChange:{subscribe:Function}, goToPage:Function}} viewer
     * @param {string} pi
     * @returns {{destroy: function():void}}
     */
    function attachUrlSync(viewer, pi) {
        const unsubscribe = viewer.onPageChange.subscribe((order) => {
            const url = new URL(window.location.href);
            url.pathname = pageUrlPath(url.pathname, pi, order + 1);
            if (url.pathname === window.location.pathname) window.history.replaceState({ order }, '', url.toString());
            else window.history.pushState({ order }, '', url.toString());
        });
        const onPopState = (e) => {
            if (e.state && typeof e.state.order === 'number') {
                viewer.goToPage(e.state.order);
            }
        };
        window.addEventListener('popstate', onPopState);
        return {
            destroy() {
                if (typeof unsubscribe === 'function') unsubscribe();
                window.removeEventListener('popstate', onPopState);
            },
        };
    }

    /** Platform and input-context predicates shared by the keyboard handling. */

    /**
     * Whether a key-event target sits in a typing context — a form field or a
     * contenteditable region (also via ancestors) — where single-character
     * shortcuts like `?` must not fire. Pure + tested.
     *
     * @param {EventTarget|null} element  e.g. a keydown event's target
     * @returns {boolean}
     */
    function isTypingTarget(element) {
        if (!element || typeof element.closest !== 'function') return false;
        return !!element.closest('input, textarea, select, [contenteditable]:not([contenteditable="false"])');
    }

    /**
     * Whether a platform string names an Apple platform, where modifier keys are
     * conventionally shown as symbols (⌥, ⇧) instead of their PC names.
     * Pure + tested.
     *
     * @param {string} [platform]  navigator.userAgentData?.platform or navigator.platform
     * @returns {boolean}
     */
    function isMacPlatform(platform) {
        return /mac|iphone|ipad|ipod/i.test(platform || '');
    }

    /**
     * Mac display variant of a modifier key: the Apple symbol plus the key name
     * for the keycap (a bare ⇧ reads like an arrow key next to real ↑/↓ caps),
     * and the spoken name for assistive technology. Symbol and name stay separate
     * so the keycap can flex-center the symbol glyph independently of the text
     * baseline. Null for keys that read the same on every platform. Pure + tested.
     *
     * @param {string} key  lowercase modifier name from a `data-key` attribute
     * @returns {{symbol:string, name:string, label:string}|null}
     */
    function macKeyLabel(key) {
        const labels = {
            alt: { symbol: '⌥', name: 'Option', label: 'Option' },
            shift: { symbol: '⇧', name: 'Shift', label: 'Shift' },
        };
        return labels[key] || null;
    }

    /** Shared DOM contract: which immersive overlays and panels count as open for the keyboard guards. */

    /** Grid, shortcuts modal, page dropdown and Bootstrap popovers; they swallow single-key shortcuts while open. */
    const OVERLAY_OPEN_SELECTOR = '#immersiveGridOverlay:not([hidden]), #immersiveShortcuts:not([hidden]), #immersivePageDropdown:not([hidden]), .popover.show';

    /** An open left rail panel. */
    const PANEL_OPEN_SELECTOR = '.immersive__panel-left.is-open';

    /**
     * Whether an immersive overlay is open. With `panels: true` an open left panel
     * counts as well (arrow and zoom keys, the `?` shortcut); without, panels are
     * ignored (Alt+digit toggles, the annotations key inside the fulltext panel).
     *
     * @param {{panels?: boolean}} [options]
     * @returns {boolean}
     */
    function isOverlayOpen({ panels = false } = {}) {
        return !!document.querySelector(panels ? `${OVERLAY_OPEN_SELECTOR}, ${PANEL_OPEN_SELECTOR}` : OVERLAY_OPEN_SELECTOR);
    }

    /** The OpenSeadragon mount; while it holds focus, OSD owns the single-letter keys (WASD panning). */
    const STAGE_SELECTOR = '[data-immersive-image]';

    /** @returns {boolean} whether keyboard focus sits inside the image stage */
    function isStageFocused() {
        const stage = document.querySelector(STAGE_SELECTOR);
        return !!stage && stage.contains(document.activeElement);
    }

    /** Central keydown dispatcher for the immersive view. */

    /** Dispatcher priorities of every immersive key handler, lowest runs first; the single place that defines who wins a key. */
    const KEY_PRIORITY = Object.freeze({
        shortcutsModal: 10,
        gridOverlay: 20,
        pageDropdown: 30,
        layoutTagsPick: 35,
        panels: 40,
        viewerNav: 50,
        annotationsToggle: 55,
    });

    /**
     * Creates the immersive key dispatcher: one document-level keydown listener,
     * handlers consulted in ascending priority order, the first handler that
     * returns true consumes the event. This keeps the overlay precedence (modal →
     * grid → dropdown → panels) declared in one place instead of guard selectors
     * scattered over independent listeners. Pure + tested.
     *
     * @returns {{register:Function, handleEvent:Function, attach:Function, detach:Function}}
     */
    function createKeyDispatcher() {
        const handlers = [];
        let bound = null;
        const onKeydown = (e) => {
            if (dispatcher.handleEvent(e)) e.preventDefault();
        };
        const dispatcher = {
            /**
             * @param {number} priority  lower runs first
             * @param {(event: KeyboardEvent) => boolean} handler  true = consumed
             * @returns {function():void} unregisters the handler
             */
            register(priority, handler) {
                const entry = { priority, handler };
                handlers.push(entry);
                handlers.sort((a, b) => a.priority - b.priority);
                return () => {
                    const i = handlers.indexOf(entry);
                    if (i !== -1) handlers.splice(i, 1);
                };
            },
            /** @returns {boolean} whether any handler consumed the event */
            handleEvent(event) {
                for (const { handler } of [...handlers]) {
                    if (handler(event)) return true;
                }
                return false;
            },
            /** Binds the single keydown listener; attaching again moves it. */
            attach(target) {
                dispatcher.detach();
                bound = target;
                bound.addEventListener('keydown', onKeydown);
            },
            /** Removes the keydown listener. */
            detach() {
                if (bound) bound.removeEventListener('keydown', onKeydown);
                bound = null;
            },
        };
        return dispatcher;
    }

    /**
     * Registers the viewer navigation shortcuts (page Left/Right, reset 0, zoom
     * +/- and Shift+Up/Down, rotate r / Shift+R) on the shared dispatcher so they
     * work without first clicking the OSD stage -- the same keys the help modal
     * advertises. Bails when the user is typing, when an overlay/panel/popover is
     * open, when Ctrl/Meta/Alt is held (browser zoom, Alt+digit panels), or when
     * the stage itself has focus -- then OSD's own canvas-key handler owns the
     * keys, so nothing fires twice. Zoom keys are ignored when the record forbids
     * zooming; rotation stays available. Plain Up/Down stay with OSD (image
     * panning) and page scroll.
     *
     * @param {{register: Function}} keys  the immersive key dispatcher
     * @param {IvViewer} viewer
     * @param {HTMLElement} stageEl        the OSD mount element ([data-immersive-image])
     * @param {number} [priority=KEY_PRIORITY.viewerNav]
     * @returns {{destroy: function():void}}
     */
    function registerViewerNavKeys(keys, viewer, stageEl, priority = KEY_PRIORITY.viewerNav) {
        const unregister = keys.register(priority, (e) => {
            if (e.ctrlKey || e.metaKey || e.altKey) return false;
            const key = e.key;
            const zoomIn = key === '+' || key === '=' || (e.shiftKey && key === 'ArrowUp');
            const zoomOut = key === '-' || key === '_' || (e.shiftKey && key === 'ArrowDown');
            const prev = !e.shiftKey && key === 'ArrowLeft';
            const next = !e.shiftKey && key === 'ArrowRight';
            const reset = key === '0';
            const rotate = typeof key === 'string' && key.toLowerCase() === 'r';
            const rotateCW = rotate && !e.shiftKey;
            const rotateCCW = rotate && e.shiftKey;
            if (!zoomIn && !zoomOut && !prev && !next && !reset && !rotateCW && !rotateCCW) return false;
            if (isTypingTarget(e.target)) return false;
            if (stageEl && stageEl.contains(document.activeElement)) return false;
            if (isOverlayOpen({ panels: true })) return false;
            if ((zoomIn || zoomOut) && !viewer.allowZoom) return false;
            if (prev) viewer.prev();
            else if (next) viewer.next();
            else if (reset) viewer.resetView();
            else if (rotateCW) viewer.rotateRight();
            else if (rotateCCW) viewer.rotateLeft();
            else if (zoomIn) viewer.zoomIn();
            else viewer.zoomOut();
            return true;
        });
        return { destroy: unregister };
    }

    /** DOM helpers shared by the wiring modules: geometry, class tokens, element building. */

    /**
     * Computes the scrollTop needed to center a line within its scroll container,
     * or null if the line is already fully visible. Pure (numbers only) so it is
     * testable without a layout engine.
     *
     * @param {{offsetTop:number, height:number, scrollTop:number, clientHeight:number, scrollHeight:number}} metrics
     * @returns {number|null}
     */
    function scrollTopToReveal(metrics) {
        const top = metrics.offsetTop;
        const bottom = metrics.offsetTop + metrics.height;
        if (top >= metrics.scrollTop && bottom <= metrics.scrollTop + metrics.clientHeight) return null;
        const target = metrics.offsetTop - metrics.clientHeight / 2 + metrics.height / 2;
        const max = Math.max(0, metrics.scrollHeight - metrics.clientHeight);
        return Math.min(Math.max(0, target), max);
    }

    /**
     * Cumulative offsetTop of `el` relative to `container` (walks the offsetParent
     * chain), for scrolling panel content into view without moving the page.
     *
     * @param {HTMLElement} el
     * @param {HTMLElement} container
     * @returns {number}
     */
    function offsetTopWithin(el, container) {
        let top = 0;
        for (let n = el; n && n !== container; n = n.offsetParent) top += n.offsetTop;
        return top;
    }

    /**
     * Splits a configured styleClass attribute into class tokens; blank or missing
     * values yield an empty list. Pure + tested.
     *
     * @param {string} [styleClass]  the configured CSS class(es) of an author mapping
     * @returns {string[]}
     */
    function styleClassTokens(styleClass) {
        return String(styleClass || '')
            .split(/\s+/)
            .filter(Boolean);
    }

    /**
     * Creates an element with optional class list and text content.
     *
     * @param {string} tag
     * @param {string} [cls]
     * @param {string} [text]
     * @returns {HTMLElement}
     */
    function el(tag, cls, text) {
        const node = document.createElement(tag);
        if (cls) node.className = cls;
        if (text != null) node.textContent = text;
        return node;
    }

    /** Teardown bookkeeping for the setup modules: one disposer per module, one call undoes everything. */

    /** Destroyable that does nothing, returned by setup functions whose markup is not on the page. */
    const noop = Object.freeze({ destroy() {} });

    /**
     * Collects listeners and teardown callbacks so a setup module can undo itself.
     *
     * @returns {{listen: Function, add: Function, dispose: Function}}
     */
    function createDisposer() {
        const fns = [];
        return {
            /** addEventListener whose removal is queued for dispose. */
            listen(target, type, handler, options) {
                target.addEventListener(type, handler, options);
                fns.push(() => target.removeEventListener(type, handler, options));
            },
            /** Queues a teardown callback (an unsubscribe or unregister function) and returns it. */
            add(fn) {
                if (typeof fn === 'function') fns.push(fn);
                return fn;
            },
            /** Runs every queued teardown once, newest first; a throwing one is logged and does not stop the rest. */
            dispose() {
                while (fns.length) {
                    try {
                        fns.pop()();
                    } catch (e) {
                        console.error('immersive: teardown failed', e);
                    }
                }
            },
        };
    }

    /** Left rail panel wiring: open/close, keyboard toggles, resize, TOC + metadata folds. */

    /** Rail panel ids in toolbar order; the Alt+digit shortcuts 1-5 map onto this. */
    const PANEL_SHORTCUT_IDS = ['immersivePanelToc', 'immersivePanelFulltext', 'immersivePanelSearch', 'immersivePanelMetadata', 'immersivePanelLayoutTags'];

    /**
     * The rail panel an Alt+digit shortcut toggles: Alt+1-5 address the panels in
     * toolbar order (TOC, fulltext, search, metadata, layout tags). Matches on
     * `code`, not `key`, because macOS Option+digit produces characters ('¡', '™',
     * …); bare digits are deliberately no shortcut (WCAG 2.1.4 discourages
     * printable single-character shortcuts) and Ctrl/Cmd combinations stay with
     * the browser. Pure + tested.
     *
     * @param {{altKey:boolean, ctrlKey:boolean, metaKey:boolean, code:string}} event
     * @returns {string|null} panel element id, or null
     */
    function panelIdForKeyEvent({ altKey, ctrlKey, metaKey, code }) {
        if (!altKey || ctrlKey || metaKey) return null;
        const index = ['Digit1', 'Digit2', 'Digit3', 'Digit4', 'Digit5'].indexOf(code);
        return index === -1 ? null : PANEL_SHORTCUT_IDS[index];
    }

    /**
     * Picks the 1-based page number of the TOC section that should be highlighted.
     * A section owns the page range [its page, next section's page). Returns the
     * current `activeNo` unchanged while any visible page (single page, or either
     * page of a double-page spread) still falls in its range, so the highlight is
     * kept; otherwise the section owning the last visible page, or null when no
     * section starts at or before it. Pure + tested.
     *
     * @param {number[]} entryNos      1-based start pages of all TOC sections
     * @param {number[]} visiblePages  1-based page numbers currently shown
     * @param {number|null} activeNo   start page of the currently highlighted section
     * @returns {number|null}
     */
    function pickActiveTocPageNo(entryNos, visiblePages, activeNo = null) {
        if (!entryNos.length || !visiblePages.length) return activeNo;
        if (activeNo !== null) {
            const nextNo = Math.min(...entryNos.filter((n) => n > activeNo), Infinity);
            if (visiblePages.some((p) => p >= activeNo && p < nextNo)) return activeNo;
        }
        const top = Math.max(...visiblePages);
        let best = null;
        for (const no of entryNos) {
            if (no <= top && (best === null || no >= best)) best = no;
        }
        return best;
    }

    /**
     * Wires the rail buttons to their left panels (open/close with ARIA + inert
     * bookkeeping) and registers the panel keyboard shortcuts on the dispatcher:
     * Escape closes the open panel, Alt+1-5 toggles the panels in rail order. On
     * open, focus moves to the panel's [data-immersive-panel-focus] element if it
     * declares one, else to its first visible control.
     *
     * @param {HTMLElement|null} immersiveRoot  the .immersive root element
     * @param {{register: Function}} keys  the immersive key dispatcher
     * @returns {{closePanels: Function, registerCloseHook: Function, replayOpen: Function, destroy: Function}}
     */
    function setupPanels(immersiveRoot, keys) {
        const bag = createDisposer();
        const panelButtons = document.querySelectorAll('[data-immersive-panel]');
        document.querySelectorAll('.immersive__panel-left').forEach((p) => (p.inert = true));
        const syncPanelOpenFlag = () => {
            if (immersiveRoot) {
                immersiveRoot.classList.toggle('-panel-open', !!document.querySelector(PANEL_OPEN_SELECTOR));
            }
        };
        const closeHooks = [];
        let activePanelBtn = null;
        const closePanels = (restoreFocus = false) => {
            closeHooks.forEach((hook) => hook());
            const closedAny = !!document.querySelector(PANEL_OPEN_SELECTOR);
            document.querySelectorAll(PANEL_OPEN_SELECTOR).forEach((p) => {
                p.classList.remove('is-open');
                p.setAttribute('aria-hidden', 'true');
                p.inert = true;
            });
            panelButtons.forEach((b) => {
                b.classList.remove('-active');
                b.setAttribute('aria-expanded', 'false');
            });
            syncPanelOpenFlag();
            if (restoreFocus && closedAny && activePanelBtn) activePanelBtn.focus();
            activePanelBtn = null;
        };
        panelButtons.forEach((btn) => {
            bag.listen(btn, 'click', () => {
                if (btn.getAttribute('aria-disabled') === 'true') return;
                const panel = document.getElementById(btn.dataset.immersivePanel);
                if (!panel) return;
                const wasOpen = panel.classList.contains('is-open');
                closePanels();
                if (!wasOpen) {
                    panel.classList.add('is-open');
                    panel.setAttribute('aria-hidden', 'false');
                    panel.inert = false;
                    btn.classList.add('-active');
                    btn.setAttribute('aria-expanded', 'true');
                    activePanelBtn = btn;
                    syncPanelOpenFlag();
                    panel.dispatchEvent(new CustomEvent('immersive:panel-open', { bubbles: false }));
                    const active = panel.querySelector('.widget-toc__element.active');
                    if (active) {
                        panel.scrollTop = Math.max(0, offsetTopWithin(active, panel) - panel.clientHeight / 2);
                    }
                    const isMetadataPanel = panel.id === 'immersivePanelMetadata';
                    const focusTarget = isMetadataPanel
                        ? panel.querySelector('.immersive__panel-title')
                        : panel.querySelector('[data-immersive-panel-focus]') ||
                          Array.from(panel.querySelectorAll('input, a[href], button')).find((n) => !n.hidden && !n.disabled && n.offsetParent !== null);
                    if (focusTarget) {
                        if (!focusTarget.hasAttribute('tabindex')) focusTarget.setAttribute('tabindex', '-1');
                        focusTarget.focus({ preventScroll: true });
                    } else {
                        if (!panel.hasAttribute('tabindex')) panel.setAttribute('tabindex', '-1');
                        panel.focus({ preventScroll: true });
                    }
                }
            });
        });

        const unregisterKeys = keys.register(KEY_PRIORITY.panels, (e) => {
            if (e.key === 'Escape') {
                if (!document.querySelector(PANEL_OPEN_SELECTOR)) return false;
                closePanels(true);
                return true;
            }
            const panelId = panelIdForKeyEvent(e);
            if (!panelId || isOverlayOpen()) return false;
            const btn = document.querySelector(`[data-immersive-panel="${panelId}"]`);
            if (!btn) return false;
            e.preventDefault();
            btn.click();
            return true;
        });
        bag.add(unregisterKeys);

        return {
            closePanels,
            /** Registers a hook that runs whenever the panels close (e.g. fulltext teardown); returns its unregister. */
            registerCloseHook(hook) {
                if (typeof hook !== 'function') return () => {};
                closeHooks.push(hook);
                return () => {
                    const i = closeHooks.indexOf(hook);
                    if (i !== -1) closeHooks.splice(i, 1);
                };
            },
            /** Re-sends the open event for an already open panel, for modules that were wired after the click. */
            replayOpen() {
                const open = document.querySelector(PANEL_OPEN_SELECTOR);
                if (open) open.dispatchEvent(new CustomEvent('immersive:panel-open', { bubbles: false }));
            },
            destroy: bag.dispose,
        };
    }

    /**
     * Adds the drag handle that resizes the left panels: one shared
     * --immersive-panel-width custom property, persisted in sessionStorage.
     *
     * @returns {{destroy: function():void}}
     */
    function setupPanelResize(immersiveRoot) {
        const immersiveViewer = immersiveRoot && immersiveRoot.querySelector('.immersive__viewer');
        if (!immersiveViewer) return noop;
        const WIDTH_KEY = 'immersive-panel-width';
        const RAIL_WIDTH = 40;
        const MIN_WIDTH = 240;
        const MAX_WIDTH_RATIO = 0.8;
        const maxWidth = () => immersiveViewer.getBoundingClientRect().width * MAX_WIDTH_RATIO;
        const minWidth = () => {
            const open = immersiveRoot.querySelector(PANEL_OPEN_SELECTOR);
            const panelMin = open ? parseFloat(getComputedStyle(open).minWidth) || 0 : 0;
            return Math.max(MIN_WIDTH, panelMin);
        };
        const clamp = (px) => Math.min(Math.max(px, minWidth()), maxWidth());
        const applyWidth = (px) => immersiveViewer.style.setProperty('--immersive-panel-width', Math.round(px) + 'px');
        let stored = NaN;
        try {
            stored = parseInt(sessionStorage.getItem(WIDTH_KEY), 10);
        } catch {}
        if (Number.isFinite(stored)) applyWidth(clamp(stored));

        const handle = document.createElement('div');
        handle.className = 'immersive__panel-resize-handle';
        handle.setAttribute('aria-hidden', 'true');
        immersiveViewer.appendChild(handle);
        let observer = null;
        if (typeof ResizeObserver !== 'undefined') {
            observer = new ResizeObserver(() => {
                const open = immersiveViewer.querySelector(PANEL_OPEN_SELECTOR);
                if (!open || !handle.offsetParent) return;
                const panelWidth = open.getBoundingClientRect().width;
                const handleRect = handle.getBoundingClientRect();
                const varWidth = handleRect.left + handleRect.width / 2 - immersiveViewer.getBoundingClientRect().left - RAIL_WIDTH;
                if (panelWidth > varWidth + 1) applyWidth(panelWidth);
            });
            immersiveViewer.querySelectorAll('.immersive__panel-left').forEach((p) => observer.observe(p));
        }
        handle.addEventListener('pointerdown', (e) => {
            e.preventDefault();
            handle.setPointerCapture(e.pointerId);
            immersiveRoot.classList.add('-resizing');
            const viewerLeft = immersiveViewer.getBoundingClientRect().left;
            const dragMin = minWidth();
            const dragMax = maxWidth();
            const widthAt = (ev) => Math.min(Math.max(ev.clientX - viewerLeft - RAIL_WIDTH, dragMin), dragMax);
            const onMove = (ev) => applyWidth(widthAt(ev));
            const onUp = (ev) => {
                handle.releasePointerCapture(e.pointerId);
                handle.removeEventListener('pointermove', onMove);
                handle.removeEventListener('pointerup', onUp);
                immersiveRoot.classList.remove('-resizing');
                try {
                    sessionStorage.setItem(WIDTH_KEY, String(Math.round(widthAt(ev))));
                } catch {}
            };
            handle.addEventListener('pointermove', onMove);
            handle.addEventListener('pointerup', onUp);
        });
        return {
            destroy() {
                if (observer) observer.disconnect();
                handle.remove();
            },
        };
    }

    /**
     * Single entry point for all TOC-panel wiring. Looks up the panel once and
     * bails early when it is not in the DOM (conditional server-render).
     *
     * @param {IvViewer} viewer
     * @returns {{destroy: function():void}}
     */
    function setupToc(viewer) {
        const tocPanel = document.getElementById('immersivePanelToc');
        if (!tocPanel) return noop;
        const parts = [setupTocCollapseToggle(tocPanel), setupTocSync(viewer, tocPanel)];
        return {
            destroy() {
                parts.forEach((part) => part.destroy());
            },
        };
    }

    /** Wires the "collapse all / expand all" toggle on the server-rendered TOC tree (shown only when the TOC nests). */
    function setupTocCollapseToggle(tocPanel = document.getElementById('immersivePanelToc')) {
        const bag = createDisposer();
        const tocContainer = document.getElementById('widgetToc');
        const tocToggle = tocPanel && tocPanel.querySelector('[data-immersive-toc-toggle]');
        if (tocContainer && tocToggle && tocContainer.querySelector(".widget-toc__element[data-level='2']")) {
            tocToggle.hidden = false;
            const reflectTocToggle = () => {
                const collapsed = !!tocContainer.querySelector('.widget-toc__element--hidden');
                tocToggle.classList.toggle('-collapsed', collapsed);
                tocToggle.setAttribute('aria-expanded', String(!collapsed));
                const label = collapsed ? tocToggle.dataset.labelExpand : tocToggle.dataset.labelCollapse;
                tocToggle.setAttribute('aria-label', label);
                tocToggle.setAttribute('title', label);
            };
            bag.listen(tocToggle, 'click', () => {
                if (tocContainer.querySelector('.widget-toc__element--hidden')) {
                    tocContainer.querySelectorAll('.widget-toc__element--hidden').forEach((li) => li.classList.remove('widget-toc__element--hidden'));
                    tocContainer.querySelectorAll('.widget-toc__element.parent').forEach((li) => {
                        li.classList.add('widget-toc__element--expanded');
                        const t = li.querySelector('.widget-toc__toggle');
                        if (t) t.setAttribute('aria-expanded', 'true');
                    });
                } else {
                    tocContainer.querySelectorAll('.widget-toc__element').forEach((li) => {
                        const level = Number(li.dataset.level);
                        if (level >= 2) li.classList.add('widget-toc__element--hidden');
                        if (level >= 1 && li.classList.contains('parent')) {
                            li.classList.remove('widget-toc__element--expanded');
                            const t = li.querySelector('.widget-toc__toggle');
                            if (t) t.setAttribute('aria-expanded', 'false');
                        }
                    });
                }
                reflectTocToggle();
            });
            reflectTocToggle();
        }
        return { destroy: bag.dispose };
    }

    /** Wires the metadata "more / less" fold that reveals the deeper structural blocks. */
    function setupMetadataToggle() {
        const bag = createDisposer();
        const metadataToggle = document.querySelector('[data-immersive-metadata-toggle]');
        if (metadataToggle) {
            const moreEl = document.getElementById(metadataToggle.dataset.immersiveMetadataToggle);
            if (moreEl) {
                const moreLabel = metadataToggle.querySelector('.immersive__metadata-toggle-more');
                const lessLabel = metadataToggle.querySelector('.immersive__metadata-toggle-less');
                const syncMetadataToggleLabel = (open) => {
                    if (moreLabel) moreLabel.setAttribute('aria-hidden', String(open));
                    if (lessLabel) lessLabel.setAttribute('aria-hidden', String(!open));
                };
                syncMetadataToggleLabel(!moreEl.hidden);
                bag.listen(metadataToggle, 'click', () => {
                    const willOpen = moreEl.hidden;
                    moreEl.hidden = !willOpen;
                    metadataToggle.setAttribute('aria-expanded', String(willOpen));
                    syncMetadataToggleLabel(willOpen);
                });
            }
        }
        return { destroy: bag.dispose };
    }

    /**
     * TOC drawer: entry clicks navigate in place (entries carry their 1-based page
     * number as data-page-no), and the active-section highlight follows the
     * currently visible page(s).
     *
     * @param {IvViewer} viewer
     * @param {HTMLElement} [tocPanel]
     * @returns {{destroy: function():void}}
     */
    function setupTocSync(viewer, tocPanel = document.getElementById('immersivePanelToc')) {
        if (!tocPanel) return noop;
        const bag = createDisposer();
        const tocEntries = () =>
            Array.from(tocPanel.querySelectorAll('.widget-toc__element[data-page-no]'))
                .filter((el) => el.dataset.level !== '0')
                .map((el) => ({ el, no: Number(el.dataset.pageNo) }))
                .filter((x) => Number.isFinite(x.no) && x.no >= 1);

        const setTocActive = (el) => {
            if (el && el.dataset.iddoc && env.viewerJS && env.viewerJS.widgetToc) {
                env.viewerJS.widgetToc.setActive(el.dataset.iddoc.replace('iddoc_', ''));
                if (tocPanel.classList.contains('is-open')) el.scrollIntoView({ block: 'nearest' });
                return;
            }
            tocPanel.querySelectorAll('.widget-toc__element.active, .widget-toc__element-link.active').forEach((x) => x.classList.remove('active'));
            if (el) {
                el.classList.add('active');
                if (tocPanel.classList.contains('is-open')) el.scrollIntoView({ block: 'nearest' });
            }
        };

        bag.listen(tocPanel, 'click', (e) => {
            const link = e.target.closest('.widget-toc__element-link a');
            if (!link) return;
            const element = link.closest('.widget-toc__element');
            const pageNo = element ? Number(element.dataset.pageNo) : NaN;
            if (!Number.isFinite(pageNo) || pageNo < 1) return;
            e.preventDefault();
            setTocActive(element);
            viewer.goToPage(pageNo - 1);
        });

        const syncTocActive = () => {
            const entries = tocEntries();
            const pages = viewer.getCurrentPages().map((p) => p + 1);
            if (!entries.length || !pages.length) return;
            const active = tocPanel.querySelector('.widget-toc__element.active[data-page-no]');
            const activeNo = active ? Number(active.dataset.pageNo) : null;
            const targetNo = pickActiveTocPageNo(
                entries.map((x) => x.no),
                pages,
                activeNo
            );
            if (active && targetNo === activeNo) return;
            let best = null;
            entries.forEach((x) => {
                if (x.no === targetNo) best = x;
            });
            setTocActive(best ? best.el : null);
        };
        bag.add(viewer.onPageChange.subscribe(syncTocActive));
        syncTocActive();
        return { destroy: bag.dispose };
    }

    /** Shared accessibility utilities for the immersive view. */

    const FOCUSABLE_SELECTOR = 'a[href],button,input,select,textarea,[tabindex]:not([tabindex="-1"])';

    /**
     * Returns a keydown handler that traps Tab focus within `container`: reaching
     * the last focusable element wraps to the first, and Shift+Tab at the first
     * wraps to the last. Attach with addEventListener and detach when the dialog
     * closes to avoid stacking handlers.
     *
     * @param {HTMLElement} container  the modal or overlay element
     * @returns {function(KeyboardEvent):void}
     */
    function createFocusTrap(container) {
        return function trapFocus(e) {
            if (e.key !== 'Tab') return;
            const f = Array.from(container.querySelectorAll(FOCUSABLE_SELECTOR)).filter((n) => !n.hidden && !n.disabled && n.getClientRects().length > 0);
            if (!f.length) return;
            const first = f[0];
            const last = f[f.length - 1];
            if (e.shiftKey && document.activeElement === first) {
                e.preventDefault();
                last.focus();
            } else if (!e.shiftKey && document.activeElement === last) {
                e.preventDefault();
                first.focus();
            }
        };
    }

    /** Keyboard-shortcuts help modal with platform-specific modifier keycaps. */

    /**
     * Keyboard-shortcuts help modal: opened via the rail button or the `?` key,
     * closed via its X button, Escape or a click on the scrim. The rail button
     * follows panel semantics (an open left panel is closed first); `?` is a
     * no-op while another immersive overlay is open — no overlay stacking — or
     * while typing in a form field. Focus is trapped inside the dialog (grid
     * pattern) and restored on close. Registers `?`/Escape on the key dispatcher
     * with the highest precedence (priority 10).
     *
     * @param {Function} closePanels  closes the open left panel(s)
     * @param {{register: Function}} keys  the immersive key dispatcher
     * @returns {{destroy: function():void}}
     */
    function setupShortcutsModal(closePanels, keys) {
        const overlay = document.getElementById('immersiveShortcuts');
        const trigger = document.querySelector('[data-immersive-shortcuts-trigger]');
        if (!overlay || !trigger) return noop;
        const bag = createDisposer();
        if (isMacPlatform(navigator.userAgentData?.platform ?? navigator.platform)) {
            overlay.querySelectorAll('kbd[data-key]').forEach((kbd) => {
                const mac = macKeyLabel(kbd.dataset.key);
                if (!mac) return;
                const symbol = document.createElement('span');
                symbol.className = 'immersive__shortcuts-key-symbol';
                symbol.textContent = mac.symbol;
                kbd.replaceChildren(symbol, document.createTextNode(mac.name));
                kbd.setAttribute('aria-label', mac.label);
            });
        }
        const dialog = overlay.querySelector('.immersive__shortcuts-dialog');
        const closeBtn = overlay.querySelector('.immersive__shortcuts-close');
        let opener = null;
        let trapHandler = null;
        const openShortcuts = () => {
            if (!overlay.hidden || isOverlayOpen({ panels: true })) return;
            opener = document.activeElement;
            overlay.hidden = false;
            trigger.setAttribute('aria-expanded', 'true');
            trapHandler = createFocusTrap(overlay);
            overlay.addEventListener('keydown', trapHandler);
            if (closeBtn) closeBtn.focus();
        };
        const closeShortcuts = () => {
            if (overlay.hidden) return;
            overlay.hidden = true;
            trigger.setAttribute('aria-expanded', 'false');
            if (trapHandler) {
                overlay.removeEventListener('keydown', trapHandler);
                trapHandler = null;
            }
            const restore = opener && opener !== document.body ? opener : trigger;
            opener = null;
            if (restore && typeof restore.focus === 'function') restore.focus();
        };
        bag.listen(trigger, 'click', () => {
            if (!overlay.hidden) {
                closeShortcuts();
                return;
            }
            if (typeof closePanels === 'function') closePanels();
            openShortcuts();
        });
        if (closeBtn) bag.listen(closeBtn, 'click', closeShortcuts);
        bag.listen(overlay, 'click', (e) => {
            if (dialog && !dialog.contains(e.target)) closeShortcuts();
        });
        const unregisterKeys = keys.register(KEY_PRIORITY.shortcutsModal, (e) => {
            if (e.key === 'Escape' && !overlay.hidden) {
                closeShortcuts();
                return true;
            }
            if (e.key === '?' && overlay.hidden && !e.ctrlKey && !e.metaKey && !e.altKey && !isTypingTarget(e.target) && !isOverlayOpen({ panels: true })) {
                e.preventDefault();
                openShortcuts();
                return true;
            }
            return false;
        });
        bag.add(unregisterKeys);
        return {
            destroy() {
                closeShortcuts();
                bag.dispose();
            },
        };
    }

    /** OCR text ↔ image region hover linking for the immersive fulltext panel (block > line > word). */

    const ACTIVE_CLASS = 'is-linked-active';

    /**
     * Indexes elements by their `data-iv-region-id` (elements without one are skipped).
     *
     * @param {Iterable<HTMLElement>} elements
     * @returns {Map<string, HTMLElement>}
     */
    function indexById(elements) {
        const map = new Map();
        for (const el of elements) {
            const id = el.dataset && el.dataset.ivRegionId;
            if (id) map.set(id, el);
        }
        return map;
    }

    /**
     * Wires bidirectional hover highlighting between the panel text elements
     * (inside `box`) and the image region overlays (`regionEls`) that share the
     * same `data-iv-region-id`. Hover on either side toggles `is-linked-active` on
     * both. On the text side the DOM nesting (block > line > word) already puts
     * every hovered ancestor into its own mouseenter state, so each element only
     * toggles its own id; the flat image overlays cascade explicitly through the
     * `parents` chain instead. Deliberately pointer-only: the highlight is a
     * supplementary cue, the text itself stays fully readable without it.
     *
     * @param {object} opts
     * @param {HTMLElement} opts.box fulltext panel element containing the text elements
     * @param {Map<string, HTMLElement>} opts.regionEls image overlay elements, indexed by id
     * @param {Map<string, string>} [opts.parents] region id → parent region id; hovering
     *   an overlay also activates all its ancestors on both sides
     * @param {HTMLElement} [opts.scrollContainer] scrollable panel; when set, hovering an
     *   image overlay scrolls its text into view if not fully visible
     * @param {Set<string>} [opts.revealIds] overlay ids allowed to trigger that scroll;
     *   large regions (paragraphs) stay out so grazing them never jumps the panel
     * @returns {{destroy: function():void}}
     */
    function mountTextImageLink({ box, regionEls, parents, scrollContainer, revealIds }) {
        const spanEls = indexById(box.querySelectorAll('[data-iv-region-id]'));
        const regions = regionEls || new Map();
        const parentIds = parents || new Map();
        const listeners = [];

        const setActive = (id, on) => {
            const span = spanEls.get(id);
            const overlay = regions.get(id);
            if (span) span.classList.toggle(ACTIVE_CLASS, on);
            if (overlay) overlay.classList.toggle(ACTIVE_CLASS, on);
        };

        const chain = (id) => {
            const ids = [];
            for (let current = id; current != null && !ids.includes(current); current = parentIds.get(current)) {
                ids.push(current);
            }
            return ids;
        };

        const revealSpan = (id) => {
            if (!scrollContainer) return;
            const span = spanEls.get(id);
            if (!span) return;
            const target = scrollTopToReveal({
                offsetTop: offsetTopWithin(span, scrollContainer),
                height: span.offsetHeight,
                scrollTop: scrollContainer.scrollTop,
                clientHeight: scrollContainer.clientHeight,
                scrollHeight: scrollContainer.scrollHeight,
            });
            if (target !== null) scrollContainer.scrollTop = target;
        };

        const bind = (el, id, isOverlay) => {
            const ids = isOverlay ? chain(id) : [id];
            const enter = () => {
                ids.forEach((i) => setActive(i, true));
                if (isOverlay && (!revealIds || revealIds.has(id))) revealSpan(id);
            };
            const leave = () => ids.forEach((i) => setActive(i, false));
            el.addEventListener('mouseenter', enter);
            el.addEventListener('mouseleave', leave);
            listeners.push([el, enter, leave]);
        };

        spanEls.forEach((el, id) => bind(el, id, false));
        regions.forEach((el, id) => bind(el, id, true));

        return {
            destroy() {
                listeners.forEach(([el, enter, leave]) => {
                    el.removeEventListener('mouseenter', enter);
                    el.removeEventListener('mouseleave', leave);
                });
                listeners.length = 0;
            },
        };
    }

    /**
     * Builds the combined multi-level panel markup from nested OCR levels: one
     * `<div>` per text block containing one `<span>` per line, which in turn holds
     * one `<span>` per word (single spaces between words). Lines without word
     * geometry render their plain line text instead. Every real region carries
     * `data-iv-region-id`; synthetic blocks (id null) render as plain wrappers.
     * All text is set via `textContent`, so OCR content can never inject markup.
     *
     * @param {Array} blocks  nested levels, see ivManifestSource.nestTextLevels
     * @returns {DocumentFragment}
     */
    function buildTextLevels(blocks) {
        const frag = document.createDocumentFragment();
        (blocks || []).forEach((block) => {
            const blockEl = document.createElement('div');
            blockEl.className = 'immersive__fulltext-block';
            if (block.id && block.rect) blockEl.dataset.ivRegionId = block.id;
            block.lines.forEach((line) => {
                const lineEl = document.createElement('span');
                lineEl.className = 'immersive__fulltext-line';
                if (line.id && line.rect) lineEl.dataset.ivRegionId = line.id;
                if (line.words.length) {
                    line.words.forEach((word, i) => {
                        if (i > 0) lineEl.appendChild(document.createTextNode(' '));
                        const wordEl = document.createElement('span');
                        wordEl.className = 'immersive__fulltext-word';
                        wordEl.dataset.ivRegionId = word.id;
                        wordEl.textContent = word.chars || '';
                        lineEl.appendChild(wordEl);
                    });
                } else {
                    lineEl.textContent = line.chars || '';
                }
                blockEl.appendChild(lineEl);
            });
            frag.appendChild(blockEl);
        });
        return frag;
    }

    /** Persisted viewer preferences (localStorage) with tolerant read/write. */

    const THUMB_SIZE_KEY = 'immersive-thumb-size';

    const THUMB_SIZE_MAX = 2;

    const THUMB_SIZE_DEFAULT = 1;

    /**
     * Normalizes a stored thumbnail-size value to a valid grid step. Values are
     * parsed with parseInt, so leading-numeric strings are accepted; anything
     * else falls back to the default (medium). Pure + tested.
     *
     * @param {*} value
     * @returns {number} integer step between 0 and THUMB_SIZE_MAX
     */
    function normalizeThumbSizeStep(value) {
        const n = parseInt(value, 10);
        return Number.isInteger(n) && n >= 0 && n <= THUMB_SIZE_MAX ? n : THUMB_SIZE_DEFAULT;
    }

    /**
     * Reads the persisted thumbnail-size step. Falls back to the default when
     * the storage operations throw (private mode) or the value is invalid.
     *
     * @param {Storage} storage  e.g. window.localStorage
     * @returns {number} integer step between 0 and THUMB_SIZE_MAX
     */
    function readThumbSizeStep(storage) {
        try {
            return normalizeThumbSizeStep(storage.getItem(THUMB_SIZE_KEY));
        } catch {
            return THUMB_SIZE_DEFAULT;
        }
    }

    /**
     * Persists the thumbnail-size step; invalid steps are normalized. Storage
     * errors (private mode, quota) are swallowed.
     *
     * @param {Storage} storage
     * @param {number|string} step
     */
    function writeThumbSizeStep(storage, step) {
        try {
            storage.setItem(THUMB_SIZE_KEY, String(normalizeThumbSizeStep(step)));
        } catch {}
    }

    const ANNOTATIONS_KEY = 'immersive-annotations';

    /**
     * Reads the persisted annotations toggle. Only the literal 'true' enables it;
     * storage errors (private mode) fall back to off. Pure + tested.
     *
     * @param {Storage} storage  e.g. window.localStorage
     * @returns {boolean}
     */
    function readAnnotationsEnabled(storage) {
        try {
            return storage.getItem(ANNOTATIONS_KEY) === 'true';
        } catch {
            return false;
        }
    }

    /**
     * Persists the annotations toggle; storage errors are swallowed.
     *
     * @param {Storage} storage
     * @param {boolean} enabled
     */
    function writeAnnotationsEnabled(storage, enabled) {
        try {
            storage.setItem(ANNOTATIONS_KEY, String(!!enabled));
        } catch {}
    }

    /** The browser's localStorage, or null when the browser blocks the access (the property read itself can throw). */
    function localStore() {
        try {
            return env.localStorage;
        } catch {
            return null;
        }
    }

    /** ALTO annotation model: pure mappings and geometry for word marks, drawn lines, margin-note anchoring and stacking. */

    /**
     * Presentation mapping keyed by the SEMANTIC types the layout endpoint derives from the
     * configured `altoLayoutTags` mappings (raw labels stay in the payload for reference).
     * Author cards additionally carry the configured styleClass verbatim, so arbitrary
     * customer vocabularies are styled via config + theme CSS without client changes; the
     * default author colors hook onto the default styleClasses in ivAnnotations.less.
     */
    const WORD_TYPE_CLASSES = {
        underline: 'immersive__anno-word-underline',
        strikethrough: 'immersive__anno-word-strike',
    };

    /** Drawn-line types rendered as image overlays; lines without a mapped type stay hidden. */
    const LINE_TYPE_MODIFIERS = {
        underline: 'underline',
        strikethrough: 'strike',
        mark: 'mark',
    };

    /**
     * Extracts the ALTO element id from a page text annotation URI. Those URIs end
     * in `alto_{pi}_{pageNo}_{elementId}` (plus an optional query); the element id
     * itself may contain underscores, so the prefix is matched as a whole. Null
     * for synthetic ids and annotations of other pages. Pure + tested.
     *
     * @param {string} regionId  a data-iv-region-id value (annotation @id)
     * @param {string} pi
     * @param {number|string} pageNo  1-based page number
     * @returns {string|null}
     */
    function altoElementId(regionId, pi, pageNo) {
        if (!regionId) return null;
        const id = String(regionId).split('?')[0].replace(/\/+$/, '');
        const marker = `alto_${pi}_${pageNo}_`;
        const at = id.lastIndexOf(marker);
        if (at === -1) return null;
        const element = id.slice(at + marker.length);
        return element.length && !element.includes('/') ? element : null;
    }

    /**
     * Whether a layout result carries any annotation data. Pure + tested.
     *
     * @param {{blocks:Array, words:Array, lines:Array}|null} layout
     * @returns {boolean}
     */
    function hasAnnotations(layout) {
        if (!layout) return false;
        return !!((layout.blocks && layout.blocks.length) || (layout.words && layout.words.length) || (layout.lines && layout.lines.length));
    }

    /**
     * CSS classes for a word's semantic types (underline/strikethrough); unknown
     * types are ignored, duplicates collapse. Pure + tested.
     *
     * @param {string[]} [types]  semantic types from the layout endpoint
     * @returns {string[]}
     */
    function wordTypeClasses(types) {
        const classes = [];
        (types || []).forEach((type) => {
            const cls = WORD_TYPE_CLASSES[type];
            if (cls && !classes.includes(cls)) classes.push(cls);
        });
        return classes;
    }

    /**
     * All CSS classes of a marked word: the type classes (form) plus the configured
     * style classes (color). Pure + tested.
     *
     * @param {{types?:string[], styleClasses?:string[]}} word  a word from the layout endpoint
     * @returns {string[]}
     */
    function wordMarkupClasses(word) {
        const classes = wordTypeClasses(word && word.types);
        ((word && word.styleClasses) || []).forEach((cls) => {
            if (cls && !classes.includes(cls)) classes.push(cls);
        });
        return classes;
    }

    /**
     * Overlay modifier for a drawn line's semantic types — underline, strike or
     * mark; null when none of its types is rendered. Pure + tested.
     *
     * @param {string[]} [types]  semantic types from the layout endpoint
     * @returns {string|null}
     */
    function lineTypeModifier(types) {
        for (const type of types || []) {
            if (LINE_TYPE_MODIFIERS[type]) return LINE_TYPE_MODIFIERS[type];
        }
        return null;
    }

    /**
     * Index of the anchor whose vertical midpoint is closest to `midY` (all in
     * ALTO units), -1 without anchors. Pure + tested.
     *
     * @param {number} midY
     * @param {{y:number, h:number}[]} [anchors]
     * @returns {number}
     */
    function nearestAnchorIndex(midY, anchors) {
        let best = -1;
        let bestDist = Infinity;
        (anchors || []).forEach((a, i) => {
            const dist = Math.abs(a.y + a.h / 2 - midY);
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        });
        return best;
    }

    /**
     * Collision handling of the margin column: a note stays exactly at its desired
     * top (its anchor line) when it has the room. Overlapping runs merge into a
     * cluster placed at the median of its members' candidate tops (the L1-optimal
     * placement — do NOT "simplify" to the average), so the displacement is
     * balanced up AND down around the lines instead of accumulating as an
     * ever-growing downward drift (cards are taller than print lines). Clusters
     * are clamped to the container top and never overlap each other. Expects
     * notes sorted by desired top. Pure + tested.
     *
     * @param {{top:number, height:number}[]} notes
     * @param {number} gap
     * @returns {number[]} final top offsets
     */
    function stackNoteTops(notes, gap) {
        const clusterTop = (c) => {
            const cands = c.desired.map((d, i) => d - c.offsets[i]).sort((a, b) => a - b);
            const mid = Math.floor(cands.length / 2);
            const optimal = cands.length % 2 ? cands[mid] : (cands[mid - 1] + cands[mid]) / 2;
            return Math.max(0, optimal);
        };
        const clusters = [];
        (notes || []).forEach((n) => {
            clusters.push({ desired: [n.top], offsets: [0], height: n.height });
            while (clusters.length > 1) {
                const prev = clusters[clusters.length - 2];
                const last = clusters[clusters.length - 1];
                if (clusterTop(last) >= clusterTop(prev) + prev.height + gap) break;
                const base = prev.height + gap;
                last.offsets.forEach((offset, i) => {
                    prev.desired.push(last.desired[i]);
                    prev.offsets.push(base + offset);
                });
                prev.height = base + last.height;
                clusters.pop();
            }
        });
        const tops = [];
        clusters.forEach((c) => {
            const top = clusterTop(c);
            c.offsets.forEach((offset) => tops.push(top + offset));
        });
        return tops;
    }

    /**
     * Whether a note whose vertical midpoint is `midY` sits ABOVE its anchor line
     * (running heads, interlinear additions) rather than ON it — then its card
     * also goes above the content row. The threshold is the anchor's TOP edge,
     * not its midpoint: handwriting regularly sits slightly high of the printed
     * line, and that jitter must not push the card a full row up. Pure + tested.
     *
     * @param {number} midY  the note block's vertical midpoint (ALTO units)
     * @param {{y:number, h:number}|null} anchor
     * @returns {boolean}
     */
    function isAboveAnchor(midY, anchor) {
        return !!anchor && midY < anchor.y;
    }

    /**
     * Whether the center of `inner` lies inside `outer` (ALTO units) — the same
     * containment test the text nesting uses. Pure + tested.
     *
     * @param {{x:number, y:number, w:number, h:number}} inner
     * @param {{x:number, y:number, w:number, h:number}} outer
     * @returns {boolean}
     */
    function rectCenterInside(inner, outer) {
        if (!inner || !outer) return false;
        const cx = inner.x + inner.w / 2;
        const cy = inner.y + inner.h / 2;
        return cx >= outer.x && cx <= outer.x + outer.w && cy >= outer.y && cy <= outer.y + outer.h;
    }

    /**
     * Image overlay descriptors for a layout: drawn lines with a rendered tag type
     * plus the margin notes as 'note' hit areas (invisible until hovered/coupled).
     * Pure + tested.
     *
     * @param {{lines:Array, blocks:Array}|null} layout
     * @returns {{id:string, rect:object, modifier:string, className?:string}[]}
     */
    function overlayRegions(layout) {
        const regions = [];
        ((layout && layout.lines) || []).forEach((line) => {
            const modifier = lineTypeModifier(line.types);
            if (!modifier || !line.rect) return;
            const className = (line.styleClasses || []).filter(Boolean).join(' ');
            regions.push(className ? { id: line.id, rect: line.rect, modifier, className } : { id: line.id, rect: line.rect, modifier });
        });
        ((layout && layout.blocks) || []).forEach((block) => {
            if (block.author && block.rect) regions.push({ id: block.id, rect: block.rect, modifier: 'note' });
        });
        return regions;
    }

    /** Annotations toggle: persisted checkbox, root state class and the `a` shortcut. */

    /** State class on the .immersive root while annotations are switched on (panel markup AND image overlays key off it). */
    const STATE_CLASS = '-annotations-on';

    /**
     * Wires the annotations checkbox: persisted state, the root state class and
     * the `a` keyboard shortcut. The whole toggle row is hidden (not just
     * disabled — it would otherwise dictate the panel's minimum width) and the
     * root state class is dropped whenever the current page has no annotation
     * data; the persisted preference survives such pages.
     *
     * @param {object} opts
     * @param {HTMLElement} opts.root      the .immersive root (carries the state class)
     * @param {HTMLInputElement} opts.checkbox
     * @param {Storage} opts.storage      e.g. window.localStorage
     * @param {{register: Function}} [opts.keys]  the immersive key dispatcher
     * @returns {{setPageData: Function, onChange: Function, destroy: Function}}
     */
    function setupAnnotationsToggle({ root, checkbox, storage, keys }) {
        const listeners = [];
        const toggleRow = checkbox.closest('.immersive__anno-toggle');
        let available = false;

        const sync = () => {
            root.classList.toggle(STATE_CLASS, checkbox.checked && available);
        };

        const setEnabled = (on) => {
            checkbox.checked = on;
            writeAnnotationsEnabled(storage, on);
            sync();
            listeners.forEach((cb) => cb(on));
        };

        const onCheckboxChange = () => setEnabled(checkbox.checked);
        checkbox.checked = readAnnotationsEnabled(storage);
        checkbox.addEventListener('change', onCheckboxChange);

        let unregisterKeys = null;
        if (keys) {
            unregisterKeys = keys.register(KEY_PRIORITY.annotationsToggle, (e) => {
                if (e.key !== 'a' || e.ctrlKey || e.metaKey || e.altKey || e.shiftKey) return false;
                if (isTypingTarget(e.target) || isStageFocused()) return false;
                const panel = checkbox.closest('.immersive__panel-left');
                if (checkbox.disabled || (panel && !panel.classList.contains('is-open'))) return false;
                if (isOverlayOpen()) return false;
                setEnabled(!checkbox.checked);
                return true;
            });
        }

        sync();

        return {
            /**
             * Updates the toggle availability for the current page's layout data.
             *
             * @param {object|null} layout  the page's layout annotations
             */
            setPageData(layout) {
                available = hasAnnotations(layout);
                checkbox.disabled = !available;
                if (toggleRow) toggleRow.hidden = !available;
                sync();
            },
            onChange(cb) {
                listeners.push(cb);
                return () => {
                    const i = listeners.indexOf(cb);
                    if (i !== -1) listeners.splice(i, 1);
                };
            },
            destroy() {
                checkbox.removeEventListener('change', onCheckboxChange);
                if (unregisterKeys) unregisterKeys();
                listeners.length = 0;
            },
        };
    }

    /** Joins ALTO annotations onto the rendered page text and its image overlays by element id. */

    /**
     * Indexes the panel's region elements by ALTO element id (blocks, lines and
     * words alike), so layout annotations can be joined onto the rendered text.
     *
     * @param {HTMLElement} box  the fulltext panel content element
     * @param {string} pi
     * @param {number|string} pageNo  1-based page number
     * @returns {Map<string, HTMLElement>}
     */
    function indexRegionElements(box, pi, pageNo) {
        const map = new Map();
        box.querySelectorAll('[data-iv-region-id]').forEach((el) => {
            const elementId = altoElementId(el.dataset.ivRegionId, pi, pageNo);
            if (elementId) map.set(elementId, el);
        });
        return map;
    }

    /**
     * Applies the word-level annotation markup (underline/strikethrough classes) to
     * the rendered panel text. The classes only become visible while the .immersive
     * root carries the state class, so toggling never re-renders. Block-level
     * annotations are NOT styled in place — every handwriting block becomes a card
     * in the annotations column (see {@link mountBlockAnnotations}).
     *
     * @param {HTMLElement} box  the fulltext panel content element
     * @param {{words:Array}} layout
     * @param {string} pi
     * @param {number|string} pageNo  1-based page number
     * @returns {Map<string, HTMLElement>} the region element index — pass it on to
     *          {@link mountBlockAnnotations} to avoid a second DOM scan
     */
    function applyTextMarkup(box, layout, pi, pageNo) {
        const els = indexRegionElements(box, pi, pageNo);
        (layout.words || []).forEach((word) => {
            const el = els.get(word.id);
            if (el) wordMarkupClasses(word).forEach((cls) => el.classList.add(cls));
        });
        return els;
    }

    /**
     * Mirrors the word-level markup onto the page's image overlays, joined by ALTO element id.
     * That join is exact — no geometry — so every marked word is covered. The overlays already
     * carry the text↔image link's hover state, which lets CSS alone reveal a marking while its
     * word is pointed at, from the scan or from the panel.
     *
     * @param {Map<string, HTMLElement>} regionEls  image overlay elements, keyed by annotation URI
     * @param {{words:Array}|null} layout
     * @param {string} pi
     * @param {number|string} pageNo  1-based page number
     */
    function applyRegionMarkup(regionEls, layout, pi, pageNo) {
        if (!regionEls || !layout) return;
        const byElementId = new Map();
        regionEls.forEach((el, uri) => {
            const elementId = altoElementId(uri, pi, pageNo);
            if (elementId) byElementId.set(elementId, el);
        });
        (layout.words || []).forEach((word) => {
            const el = byElementId.get(word.id);
            if (el) wordMarkupClasses(word).forEach((cls) => el.classList.add(cls));
        });
    }

    /**
     * A block's reading-order source elements in the panel: the block element when
     * the panel carries a real block region id, otherwise (the nesting drops
     * degenerate block levels on some pages) the rendered lines whose geometry lies
     * inside the block.
     */
    function sourceElsFor(block, els, lineEls) {
        const srcEl = els.get(block.id);
        if (srcEl) return [srcEl];
        return lineEls.filter((l) => rectCenterInside(l.rect, block.rect)).map((l) => l.el);
    }

    /**
     * Marks every annotation block's sources in the reading-order text: `anno-src`
     * hides them while annotations are off (the printed-text column shows print
     * only), `anno-margin-src` while on (their card in the column replaces them).
     *
     * @returns {Map<string, HTMLElement[]>} block id → source elements
     */
    function markAnnotationSources(annotationBlocks, els, lineEls) {
        const sources = new Map(annotationBlocks.map((block) => [block.id, sourceElsFor(block, els, lineEls)]));
        sources.forEach((blockSources) => {
            blockSources.forEach((el) => el.classList.add('immersive__anno-src', 'immersive__anno-margin-src'));
        });
        return sources;
    }

    /** Margin-note cards for a page's handwriting blocks: line anchoring, collision stacking, popovers and hover coupling to text and image. */

    /**
     * Below this panel content width (px) the margin column collapses and notes render inline
     * under their anchor line. With annotations on, the LESS min-width (560px panel − 32px
     * padding = 528px content) keeps desktop above this threshold, so the collapsed mode is
     * the small-screen fallback only; keep 560 − padding > this value or the ResizeObserver
     * would flip-flop (see @immersive-anno-panel-min-width in ivAnnotations.less).
     */
    const COLLAPSE_BELOW = 460;

    /**
     * Vertical gap (px) between stacked margin notes. Kept small on purpose: entry
     * height + gap must stay at or below the print row height (~24px), or runs of
     * consecutive annotations can never sit on their own lines.
     */
    /** Card whose text does not fit the clamp - the only one a click unfolds. */
    const TRUNCATED_CLASS = '-truncated';

    const NOTE_GAP = 2;

    const POPOVER_TEMPLATE =
        '<div class="popover immersive__anno-popover" role="tooltip"><div class="arrow"></div><h3 class="popover-header"></h3><div class="popover-body"></div></div>';

    /** Hover state shared with the text ↔ image link, so card words and image regions light up alike. */
    const LINKED_CLASS = 'is-linked-active';

    /** Registers a mouseenter/mouseleave pair on `el` and records it in `registry` for teardown. */
    function bindHover(el, enter, leave, registry) {
        el.addEventListener('mouseenter', enter);
        el.addEventListener('mouseleave', leave);
        registry.push([el, enter, leave]);
    }

    /** Rendered line elements of the page with their ALTO geometry, for anchoring. */
    function collectLineEls(flat, els, pi, pageNo) {
        const lineEls = [];
        (flat || []).forEach((r) => {
            if (r.level !== 'line' || !r.rect) return;
            const el = els.get(altoElementId(r.id, pi, pageNo) || '');
            if (el) lineEls.push({ rect: r.rect, el });
        });
        return lineEls;
    }

    /** True while a text selection starts inside `el` — a click that ends such a drag must not open the popover. */
    function hasSelectionInside(el) {
        const selection = typeof window.getSelection === 'function' ? window.getSelection() : null;
        return !!selection && String(selection).length > 0 && !!selection.anchorNode && el.contains(selection.anchorNode);
    }

    /** Popover body: the card's text word by word with its markup, selectable for copying; built fresh on every show. */
    function buildPopoverContent(textEl) {
        const text = document.createElement('div');
        text.className = 'immersive__anno-popover-text';
        textEl.childNodes.forEach((node) => text.append(node.cloneNode(true)));
        text.querySelectorAll('[data-iv-word-id]').forEach((span) => {
            delete span.dataset.ivWordId;
            span.classList.remove(LINKED_CLASS);
        });
        return text;
    }

    /**
     * Bootstrap popovers on the note cards, one open at a time, opened by click,
     * Enter or Space. They open to the right, beside the card over the scan, and
     * live in `container` (the immersive root: outside the panel's overflow clip,
     * inside the `.immersive` styling context). The outside-click listener is
     * bound in the capture phase because OpenSeadragon stops click propagation
     * over its canvas; Escape stops propagation so the immersive key dispatcher
     * does not also close the panel.
     */
    function createNotePopovers(container) {
        const $ = env.$ || null;
        const capable = !!($ && $.fn && typeof $.fn.popover === 'function');
        let openNote = null;
        const hide = () => {
            if (openNote && capable) $(openNote).popover('hide');
        };
        const toggle = (noteEl) => {
            const wasOpen = openNote === noteEl;
            hide();
            if (!wasOpen) $(noteEl).popover('show');
        };
        const onDocumentClick = (e) => {
            if (!openNote || !(e.target instanceof Element)) return;
            if (e.target.closest('.immersive__anno-note, .immersive__anno-popover')) return;
            hide();
        };
        document.addEventListener('click', onDocumentClick, true);
        const attach = (noteEl, authorName, textEl) => {
            if (!capable) return;
            $(noteEl).popover({
                trigger: 'manual',
                placement: 'right',
                boundary: 'viewport',
                container: container || 'body',
                template: POPOVER_TEMPLATE,
                html: true,
                sanitize: false,
                title: authorName || '',
                content: () => buildPopoverContent(textEl),
            });
            noteEl.setAttribute('aria-haspopup', 'dialog');
            noteEl.setAttribute('aria-expanded', 'false');
            noteEl.addEventListener('click', () => {
                if (hasSelectionInside(noteEl)) return;
                toggle(noteEl);
            });
            noteEl.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    toggle(noteEl);
                } else if (e.key === 'Escape' && openNote === noteEl) {
                    e.preventDefault();
                    e.stopPropagation();
                    hide();
                }
            });
            $(noteEl).on('shown.bs.popover', () => {
                openNote = noteEl;
                noteEl.setAttribute('aria-expanded', 'true');
                const id = noteEl.getAttribute('aria-describedby');
                const pop = id && document.getElementById(id);
                if (pop && pop.dataset.annoEscBound !== 'true') {
                    pop.dataset.annoEscBound = 'true';
                    pop.addEventListener('keydown', (e) => {
                        if (e.key === 'Escape') {
                            e.preventDefault();
                            e.stopPropagation();
                            hide();
                            noteEl.focus();
                        }
                    });
                }
            });
            $(noteEl).on('hidden.bs.popover', () => {
                if (openNote === noteEl) openNote = null;
                noteEl.setAttribute('aria-expanded', 'false');
            });
        };
        const destroy = (notes) => {
            document.removeEventListener('click', onDocumentClick, true);
            if (capable) notes.forEach(({ noteEl }) => $(noteEl).popover('dispose'));
        };
        return { attach, hide, destroy };
    }

    /**
     * Fills a card's text from the block's source elements word by word, so the
     * words keep their markup (type classes + tag style classes) but none of the
     * source's link state. Each word remembers its source region id under
     * `data-iv-word-id` (not `data-iv-region-id`, which would collide with the
     * panel's region index) for the word-level hover coupling to the image.
     */
    function renderNoteText(textEl, blockSources, wordsById, pi, pageNo) {
        blockSources.forEach((srcEl, i) => {
            if (i > 0) textEl.append(' ');
            const wordEls = srcEl.querySelectorAll('.immersive__fulltext-word');
            if (!wordEls.length) {
                textEl.append(srcEl.textContent.trim());
                return;
            }
            wordEls.forEach((wordEl, j) => {
                if (j > 0) textEl.append(' ');
                const span = document.createElement('span');
                span.textContent = wordEl.textContent;
                if (wordEl.dataset.ivRegionId) span.dataset.ivWordId = wordEl.dataset.ivRegionId;
                const word = wordsById.get(altoElementId(wordEl.dataset.ivRegionId, pi, pageNo) || '');
                if (word) span.classList.add(...wordMarkupClasses(word));
                textEl.append(span);
            });
        });
    }

    /**
     * One card per block, anchored to the nearest printed line; hovering a card lights
     * its anchor line and its image region. The card is a focusable div, not a button,
     * so its text stays selectable for copying.
     */
    function buildNotes({ blocks, sources, anchors, popovers, getRegionEls, wordsById, pi, pageNo }) {
        return blocks
            .map((block) => {
                const blockSources = sources.get(block.id) || [];
                const noteEl = document.createElement('div');
                noteEl.setAttribute('role', 'button');
                noteEl.setAttribute('tabindex', '0');
                noteEl.className = 'immersive__anno-note';
                styleClassTokens(block.styleClass).forEach((cls) => noteEl.classList.add(cls));
                const who = document.createElement('span');
                who.className = 'immersive__anno-note-author';
                who.textContent = block.authorName || '';
                noteEl.append(who);
                const text = document.createElement('span');
                text.className = 'immersive__anno-note-text';
                if (blockSources.length) renderNoteText(text, blockSources, wordsById, pi, pageNo);
                else text.textContent = block.text || '';
                noteEl.append(text);
                popovers.attach(noteEl, block.authorName, text);
                const midY = block.rect.y + block.rect.h / 2;
                const anchorIdx = nearestAnchorIndex(midY, anchors);
                const anchor = anchorIdx >= 0 ? anchors[anchorIdx] : null;
                const aboveAnchor = isAboveAnchor(midY, anchor);
                const imageRegion = () => (getRegionEls ? getRegionEls().get(block.id) : null);
                noteEl.addEventListener('mouseenter', () => {
                    if (anchor && !aboveAnchor) anchor.el.classList.add('is-anno-anchor');
                    const region = imageRegion();
                    if (region) region.classList.add('is-anno-active');
                });
                noteEl.addEventListener('mouseleave', () => {
                    if (anchor && !aboveAnchor) anchor.el.classList.remove('is-anno-anchor');
                    const region = imageRegion();
                    if (region) region.classList.remove('is-anno-active');
                });
                return { block, noteEl, anchor, aboveAnchor };
            })
            .sort((a, b) => a.block.rect.y - b.block.rect.y);
    }

    /** Hovering an anchor line lights every card anchored to it; returns the listener registry for teardown. */
    function bindAnchorHover(notes) {
        const listeners = [];
        const byAnchor = new Map();
        notes.forEach((n) => {
            if (!n.anchor || n.aboveAnchor) return;
            if (!byAnchor.has(n.anchor.el)) byAnchor.set(n.anchor.el, []);
            byAnchor.get(n.anchor.el).push(n.noteEl);
        });
        byAnchor.forEach((noteEls, anchorEl) => {
            bindHover(
                anchorEl,
                () => noteEls.forEach((el) => el.classList.add('is-anno-active')),
                () => noteEls.forEach((el) => el.classList.remove('is-anno-active')),
                listeners
            );
        });
        return listeners;
    }

    /**
     * Couples the image regions (drawn note areas and OCR text regions) to the cards:
     * hover lights the card and scrolls it into view. Word regions and the card's
     * words are coupled one to one in both directions, sharing the text ↔ image
     * link's `is-linked-active` state.
     */
    function createRegionCoupling({ notes, sources, scrollContainer }) {
        const revealCard = (noteEl) => {
            if (!scrollContainer) return;
            const target = scrollTopToReveal({
                offsetTop: offsetTopWithin(noteEl, scrollContainer),
                height: noteEl.offsetHeight,
                scrollTop: scrollContainer.scrollTop,
                clientHeight: scrollContainer.clientHeight,
                scrollHeight: scrollContainer.scrollHeight,
            });
            if (target !== null) scrollContainer.scrollTop = target;
        };
        const notesById = new Map(notes.map((n) => [n.block.id, n]));
        const regionIdToNote = new Map();
        notes.forEach((note) => {
            (sources.get(note.block.id) || []).forEach((srcEl) => {
                const collect = (el) => {
                    const id = el.dataset && el.dataset.ivRegionId;
                    if (id) regionIdToNote.set(id, note);
                };
                collect(srcEl);
                srcEl.querySelectorAll('[data-iv-region-id]').forEach(collect);
            });
        });
        const wordSpans = new Map();
        notes.forEach((note) => {
            note.noteEl.querySelectorAll('[data-iv-word-id]').forEach((span) => wordSpans.set(span.dataset.ivWordId, { span, note }));
        });
        let textRegions = new Map();
        const listeners = [];
        const bindRegion = (el, note, span) => {
            bindHover(
                el,
                () => {
                    note.noteEl.classList.add('is-anno-active');
                    if (span) span.classList.add(LINKED_CLASS);
                    revealCard(note.noteEl);
                    if (note.anchor && !note.aboveAnchor) note.anchor.el.classList.add('is-anno-anchor');
                },
                () => {
                    note.noteEl.classList.remove('is-anno-active');
                    if (span) span.classList.remove(LINKED_CLASS);
                    if (note.anchor && !note.aboveAnchor) note.anchor.el.classList.remove('is-anno-anchor');
                },
                listeners
            );
        };
        const spanListeners = [];
        wordSpans.forEach(({ span }, id) => {
            bindHover(
                span,
                () => {
                    span.classList.add(LINKED_CLASS);
                    const region = textRegions.get(id);
                    if (region) region.classList.add(LINKED_CLASS);
                },
                () => {
                    span.classList.remove(LINKED_CLASS);
                    const region = textRegions.get(id);
                    if (region) region.classList.remove(LINKED_CLASS);
                },
                spanListeners
            );
        });
        const unbind = (registry) => {
            registry.splice(0).forEach(([el, enter, leave]) => {
                el.removeEventListener('mouseenter', enter);
                el.removeEventListener('mouseleave', leave);
            });
        };
        const attachRegions = (annoRegionMap, textRegionMap) => {
            unbind(listeners);
            textRegions = textRegionMap || new Map();
            if (annoRegionMap) {
                annoRegionMap.forEach((el, id) => {
                    const note = notesById.get(id);
                    if (note && el.classList.contains('immersive__anno-region-note')) bindRegion(el, note);
                });
            }
            if (textRegionMap) {
                textRegionMap.forEach((el, id) => {
                    const word = wordSpans.get(id);
                    const note = word ? word.note : regionIdToNote.get(id);
                    if (note) bindRegion(el, note, word ? word.span : null);
                });
            }
        };
        const destroy = () => {
            attachRegions(null, null);
            unbind(spanListeners);
        };
        return { attachRegions, destroy };
    }

    /**
     * Places the cards: in the side column with collision stacking, or inline under
     * their anchor line once the panel is narrower than COLLAPSE_BELOW. A card that
     * would overflow above the first printed line shifts the text down via the
     * --immersive-anno-shift custom property instead of being clamped onto the line.
     */
    /** Marks the cards whose clamped text overflows, so they can hint at the full text in the popover. */
    function markTruncation(notes) {
        notes.forEach(({ noteEl }) => {
            const text = noteEl.querySelector('.immersive__anno-note-text');
            const cut = !!text && text.scrollHeight > text.clientHeight + 1;
            noteEl.classList.toggle(TRUNCATED_CLASS, cut);
        });
    }

    function createNoteLayout({ box, notes, anchors, columnsHead, popovers, resizer }) {
        const column = document.createElement('div');
        column.className = 'immersive__anno-margin';
        const handle = resizer ? resizer.handle : null;
        const inlineNotes = [];
        const relayout = () => {
            popovers.hide();
            inlineNotes.splice(0).forEach((el) => el.remove());
            column.remove();
            box.style.removeProperty('--immersive-anno-shift');
            const collapsed = box.clientWidth < COLLAPSE_BELOW;
            box.classList.toggle('-anno-collapsed', collapsed);
            if (columnsHead) columnsHead.hidden = collapsed;
            if (collapsed) {
                if (handle) handle.remove();
                notes.forEach(({ noteEl, anchor, aboveAnchor }) => {
                    noteEl.classList.add('-inline');
                    noteEl.style.top = '';
                    if (anchor && aboveAnchor) anchor.el.before(noteEl);
                    else if (anchor) anchor.el.after(noteEl);
                    else box.appendChild(noteEl);
                    inlineNotes.push(noteEl);
                });
                markTruncation(notes);
                return;
            }
            notes.forEach(({ noteEl }) => {
                noteEl.classList.remove('-inline');
                column.appendChild(noteEl);
            });
            if (handle && handle.parentNode !== box) box.appendChild(handle);
            box.appendChild(column);
            markTruncation(notes);
            const rowHeight = anchors.length ? parseFloat(getComputedStyle(anchors[0].el).lineHeight) || 0 : 0;
            const desired = notes
                .map(({ noteEl, anchor, aboveAnchor }) => {
                    const anchorTop = anchor ? offsetTopWithin(anchor.el, box) : 0;
                    const height = noteEl.offsetHeight;
                    return {
                        noteEl,
                        top: aboveAnchor ? anchorTop - height - NOTE_GAP : anchorTop + (rowHeight - height) / 2,
                        height,
                    };
                })
                .sort((a, b) => a.top - b.top);
            const overflow = desired.length ? Math.max(0, -desired[0].top) : 0;
            if (overflow) {
                box.style.setProperty('--immersive-anno-shift', `${overflow}px`);
                desired.forEach((d) => {
                    d.top += overflow;
                });
            }
            stackNoteTops(desired, NOTE_GAP).forEach((top, i) => {
                desired[i].noteEl.style.top = `${top}px`;
            });
        };
        const destroy = () => {
            inlineNotes.splice(0).forEach((el) => el.remove());
            column.remove();
            if (handle) handle.remove();
            box.classList.remove('-anno-collapsed');
            box.style.removeProperty('--immersive-anno-shift');
            if (columnsHead) columnsHead.hidden = true;
        };
        return { relayout, destroy, column };
    }

    /**
     * Mounts everything block-level for one rendered page: every handwriting block
     * becomes a card in the annotations column, line-anchored at its position on
     * the page, with collisions stacked; the printed-text column shows print only.
     * Below COLLAPSE_BELOW content width the column collapses and the cards render
     * inline under their anchor line. Wires the bidirectional hover coupling between
     * cards, anchor lines and the image regions.
     *
     * @param {object} opts
     * @param {HTMLElement} opts.box     the fulltext panel content element
     * @param {object} opts.layout      the page's layout annotations
     * @param {string} opts.pi
     * @param {number|string} opts.pageNo  1-based page number
     * @param {{id:string, rect:{y:number, h:number}|null, level:string}[]} opts.flat  flattened text levels (line geometry for anchoring)
     * @param {Map<string, HTMLElement>} [opts.els]  region element index from applyTextMarkup; indexed fresh when absent
     * @param {HTMLElement} [opts.columnsHead]  the "printed text | annotations" column header, shown only while the side column is active
     * @param {HTMLElement} [opts.scrollContainer]  scrollable panel; region hover reveals the card in it
     * @param {{handle: HTMLElement}} [opts.resizer]  the column splitter, placed next to the side column while it is active
     * @param {() => Map<string, HTMLElement>} [opts.getRegionEls]  live accessor for the image overlay elements by ALTO element id
     * @returns {{relayout: function():void, attachRegions: function(Map, Map):void, destroy: function():void}|null}
     *          destroy() removes cards, listeners and marker classes; the reading order of repositioned inline sources is restored by the panel rebuild on page open
     */
    function mountBlockAnnotations({ box, layout, pi, pageNo, flat, els: givenEls, columnsHead, scrollContainer, getRegionEls, resizer }) {
        const blocks = ((layout && layout.blocks) || []).filter((b) => b.author && b.rect);
        if (!blocks.length) return null;
        const els = givenEls || indexRegionElements(box, pi, pageNo);
        const lineEls = collectLineEls(flat, els, pi, pageNo);
        const sources = markAnnotationSources(blocks, els, lineEls);
        const anchors = lineEls.filter(({ el }) => !el.closest('.immersive__anno-src')).map(({ rect, el }) => ({ y: rect.y, h: rect.h, el }));
        const popovers = createNotePopovers(box.closest('.immersive') || scrollContainer);
        const wordsById = new Map(((layout && layout.words) || []).map((w) => [w.id, w]));
        const notes = buildNotes({ blocks, sources, anchors, popovers, getRegionEls, wordsById, pi, pageNo });
        const anchorListeners = bindAnchorHover(notes);
        const coupling = createRegionCoupling({ notes, sources, scrollContainer });
        const placement = createNoteLayout({ box, notes, anchors, columnsHead, popovers, resizer });
        /* The split is anchored to the text, so a panel resize changes only the column's
           width; the box's content box does not see that. */
        let observer = null;
        if (typeof ResizeObserver !== 'undefined') {
            observer = new ResizeObserver(() => placement.relayout());
            observer.observe(box);
            observer.observe(placement.column);
        }
        placement.relayout();
        return {
            relayout: placement.relayout,
            attachRegions: coupling.attachRegions,
            destroy() {
                if (observer) observer.disconnect();
                popovers.destroy(notes);
                coupling.destroy();
                anchorListeners.splice(0).forEach(([el, enter, leave]) => {
                    el.removeEventListener('mouseenter', enter);
                    el.removeEventListener('mouseleave', leave);
                    el.classList.remove('is-anno-anchor');
                });
                placement.destroy();
                els.forEach((el) => el.classList.remove('immersive__anno-margin-src', 'immersive__anno-src'));
                if (getRegionEls) getRegionEls().forEach((el) => el.classList.remove('is-anno-active'));
            },
        };
    }

    /** Splitter between the printed text and the annotation column: it sets where the two columns split, dragged, keyed or reset. */

    /** Session key of the split position, the panel width's counterpart (see setupPanelResize). */
    const SPLIT_KEY = 'immersive-anno-split';

    /**
     * The px limits the LESS clamp() guards as well (@immersive-anno-text-min,
     * @immersive-anno-column-min, @immersive-anno-column-default, @immersive-anno-margin-gap).
     * Keeping the numbers equal is what makes the rule stop exactly under the pointer
     * instead of before it.
     */
    const TEXT_MIN = 200;
    const COLUMN_MIN = 120;
    const COLUMN_DEFAULT = 160;
    const COLUMN_GAP = 12;

    /** Arrow-key step in px, quadrupled while Shift is held. */
    const KEY_STEP = 16;

    const inert = { handle: null, destroy() {} };

    /**
     * Mounts the splitter that gives the annotation column more (or less) of the
     * fulltext panel, so long annotations can be read without opening every card.
     *
     * The split is anchored to the printed text: the custom property holds the
     * text column's width, the annotation column takes what is left. So each of
     * the two handles moves exactly one edge -- the panel's own handle moves the
     * panel edge and hands the difference to the annotations, this one moves the
     * rule between the columns -- and neither drags the other one along. Until
     * someone moves it, no property is written at all and the LESS default keeps
     * the column at @immersive-anno-column-default.
     *
     * A drag tracks the pointer on the window, not on the handle, so it survives
     * the note relayout it triggers on every frame; it ends on pointerup wherever
     * that happens, which is guaranteed by the pointer capture.
     *
     * The handle element is owned here but placed by the note layout, which drops
     * it into the box next to the margin column and takes it out again while the
     * column is collapsed.
     *
     * @param {object} opts
     * @param {HTMLElement} opts.panel  the fulltext panel (hosts the custom property)
     * @param {HTMLElement} opts.box    the fulltext content element (measures the room the split has)
     * @param {string} [opts.label]     accessible name of the splitter
     * @returns {{handle: HTMLElement|null, destroy: function():void}}
     */
    function setupAnnotationColumnResize({ panel, box, label }) {
        if (!panel || !box) return inert;
        const root = panel.closest('.immersive');

        const room = () => box.clientWidth || 1;
        /** Text column widths the current panel width allows, the annotation column's limits mirrored. */
        const bounds = () => ({ min: TEXT_MIN, max: Math.max(TEXT_MIN, room() - COLUMN_MIN - COLUMN_GAP) });
        const clamp = (width) => {
            const { min, max } = bounds();
            return Math.round(Math.min(Math.max(width, min), max));
        };

        const handle = document.createElement('div');
        handle.className = 'immersive__anno-resize-handle';
        handle.setAttribute('role', 'separator');
        handle.setAttribute('aria-orientation', 'vertical');
        handle.tabIndex = 0;
        if (label) {
            handle.setAttribute('aria-label', label);
            handle.setAttribute('title', label);
        }

        /** The split the user set, clamped to the panel's room; NaN while they never moved it. */
        let split = NaN;
        try {
            split = parseFloat(sessionStorage.getItem(SPLIT_KEY));
        } catch {}
        /** The text width in effect, whether from the user or from the LESS default. */
        const current = () => clamp(Number.isFinite(split) ? split : room() - COLUMN_DEFAULT - COLUMN_GAP);

        /** The annotation column's share of the panel, the value the splitter announces. */
        const shareOf = (textWidth) => Math.round(((room() - textWidth - COLUMN_GAP) / room()) * 100);

        const apply = (next) => {
            split = Number.isFinite(next) ? clamp(next) : NaN;
            if (Number.isFinite(split)) panel.style.setProperty('--immersive-anno-split', split + 'px');
            else panel.style.removeProperty('--immersive-anno-split');
            const { min, max } = bounds();
            const share = shareOf(current());
            handle.setAttribute('aria-valuenow', String(share));
            handle.setAttribute('aria-valuetext', share + '%');
            handle.setAttribute('aria-valuemin', String(shareOf(max)));
            handle.setAttribute('aria-valuemax', String(shareOf(min)));
        };

        /* One write per frame: a drag emits pointermoves faster than the panel can
           reflow its text and restack the cards. */
        let frame = 0;
        let pending = split;
        const flush = () => {
            frame = 0;
            apply(pending);
        };
        const applySoon = (next) => {
            pending = next;
            if (!frame) frame = requestAnimationFrame(flush);
        };
        const applyNow = (next) => {
            if (frame) cancelAnimationFrame(frame);
            frame = 0;
            apply(next);
            try {
                if (Number.isFinite(split)) sessionStorage.setItem(SPLIT_KEY, String(split));
                else sessionStorage.removeItem(SPLIT_KEY);
            } catch {}
        };

        apply(split);

        let dragging = false;
        /** Detaches the running drag's window listeners; null while no drag runs. */
        let releaseDrag = null;
        const endDrag = () => {
            dragging = false;
            if (releaseDrag) releaseDrag();
            releaseDrag = null;
            if (root) root.classList.remove('-anno-resizing');
        };

        handle.addEventListener('pointerdown', (e) => {
            if (dragging || (e.pointerType === 'mouse' && e.button !== 0)) return;
            e.preventDefault();
            dragging = true;
            if (handle.setPointerCapture) handle.setPointerCapture(e.pointerId);
            if (root) root.classList.add('-anno-resizing');
            const rect = box.getBoundingClientRect();
            const splitAt = (ev) => ev.clientX - rect.left - COLUMN_GAP / 2;
            const onMove = (ev) => {
                if (dragging) applySoon(splitAt(ev));
            };
            const onEnd = (ev) => {
                endDrag();
                applyNow(ev.type === 'pointercancel' ? split : splitAt(ev));
            };
            window.addEventListener('pointermove', onMove);
            window.addEventListener('pointerup', onEnd);
            window.addEventListener('pointercancel', onEnd);
            releaseDrag = () => {
                window.removeEventListener('pointermove', onMove);
                window.removeEventListener('pointerup', onEnd);
                window.removeEventListener('pointercancel', onEnd);
            };
        });

        /* The column grows to the left, so ArrowLeft moves the rule left. The keys stay
           with the focused splitter instead of reaching the immersive key dispatcher. */
        handle.addEventListener('keydown', (e) => {
            if (e.ctrlKey || e.metaKey || e.altKey) return;
            const towards = e.key === 'ArrowLeft' ? -1 : e.key === 'ArrowRight' ? 1 : 0;
            if (!towards) return;
            e.preventDefault();
            e.stopPropagation();
            applyNow(current() + towards * KEY_STEP * (e.shiftKey ? 4 : 1));
        });

        handle.addEventListener('dblclick', () => applyNow(NaN));

        return {
            handle,
            destroy() {
                if (frame) cancelAnimationFrame(frame);
                endDrag();
                handle.remove();
                panel.style.removeProperty('--immersive-anno-split');
            },
        };
    }

    /** Fulltext panel wiring: combined block/line/word text with image hover linking and layout annotations. */

    /**
     * Wires the fulltext panel: loads the nested OCR levels for the visible page,
     * renders them, and mounts the bidirectional text ↔ image hover linking. The
     * teardown runs through the panels' close hook so leaving the panel always
     * clears the image overlays.
     *
     * The ALTO layout annotations ride alongside. Annotations are only ever visible
     * while the fulltext panel is OPEN and the checkbox is on, so the layout JSON is
     * fetched, applied and overlaid strictly per open panel; closing the panel clears
     * everything, the persisted checkbox state kicks back in on the next open.
     *
     * @param {IvViewer} viewer
     * @param {string} pi
     * @param {string} apiBase
     * @param {{closePanels: Function, registerCloseHook: Function}} panels
     * @param {{register: Function}} [keys]  the immersive key dispatcher (annotations shortcut)
     * @returns {{updateFulltextAvail: Function, destroy: Function}}
     */
    function setupFulltextPanel(viewer, pi, apiBase, panels, keys) {
        const bag = createDisposer();
        const fulltextPanel = document.getElementById('immersivePanelFulltext');
        const fulltextBox = document.getElementById('immersiveFulltext');
        const fulltextLoader = document.getElementById('immersiveFulltextLoader');
        const fulltextBtn = document.querySelector('[data-immersive-panel="immersivePanelFulltext"]');
        const fulltextTitleDefault = fulltextBtn ? fulltextBtn.getAttribute('title') : '';

        const annoCheckbox = document.getElementById('immersiveAnnotationsToggle');
        const annoColumnsHead = document.getElementById('immersiveAnnotationsColumns');
        const immersiveRoot = fulltextPanel ? fulltextPanel.closest('.immersive') : null;
        const storage = localStore();
        const annotations = annoCheckbox && immersiveRoot ? setupAnnotationsToggle({ root: immersiveRoot, checkbox: annoCheckbox, storage, keys }) : null;
        if (annotations) bag.add(() => annotations.destroy());
        const annoResize = setupAnnotationColumnResize({
            panel: fulltextPanel,
            box: fulltextBox,
            label: annoColumnsHead ? annoColumnsHead.dataset.labelResize : '',
        });
        bag.add(() => annoResize.destroy());

        const panelOpen = () => !!fulltextPanel && fulltextPanel.classList.contains('is-open');

        let layoutReq = 0;
        /** Invalidates an in-flight layout request, so a late response cannot draw into a torn-down panel. */
        const cancelLayout = () => {
            layoutReq++;
        };
        let currentLayout = null;
        let currentFlat = null;
        let currentAnnotations = null;

        const clearBlockAnnotations = () => {
            if (currentAnnotations) {
                currentAnnotations.destroy();
                currentAnnotations = null;
            }
        };

        /** Applies inline markup + block annotations (margin column etc.) to the rendered panel text. */
        const decorate = () => {
            if (!annotations || !currentLayout || !panelOpen() || !fulltextBox || !fulltextBox.childElementCount) return;
            const pageNo = viewer.getCurrentOrder() + 1;
            const els = applyTextMarkup(fulltextBox, currentLayout, pi, pageNo);
            applyRegionMarkup(textRegionEls, currentLayout, pi, pageNo);
            clearBlockAnnotations();
            currentAnnotations = mountBlockAnnotations({
                box: fulltextBox,
                layout: currentLayout,
                pi,
                pageNo,
                flat: currentFlat,
                els,
                columnsHead: annoColumnsHead,
                scrollContainer: fulltextPanel,
                getRegionEls: () => overlayEls,
                resizer: annoResize.handle ? annoResize : null,
            });
            if (currentAnnotations) currentAnnotations.attachRegions(overlayEls, textRegionEls);
        };

        /**
         * Overlay boxes are viewport-positioned against the CURRENT OSD item, which only exists
         * once the (per-spread rebuilt) OpenSeadragon has settled — so they are (re)mounted on
         * every onOpen, not on page change; nothing is mounted while the panel is closed.
         */
        let overlayEls = new Map();
        let textRegionEls = new Map();
        const mountOverlays = () => {
            overlayEls = viewer.setAnnotationRegions(panelOpen() ? overlayRegions(currentLayout) : []);
            if (currentAnnotations) currentAnnotations.attachRegions(overlayEls, textRegionEls);
        };

        /**
         * Per-page layout annotations while the panel is open: checkbox availability,
         * image overlays and panel decoration. With the panel closed (or in double-page
         * mode, where the panel is unavailable) nothing is fetched and overlays clear.
         */
        const refreshLayout = async () => {
            if (!annotations) return;
            const req = ++layoutReq;
            currentLayout = null;
            if (!panelOpen() || viewer.isDoublePage()) {
                mountOverlays();
                return;
            }
            let layout = null;
            try {
                layout = await loadPageLayout(pi, apiBase, viewer.getCurrentOrder());
            } catch (e) {
                console.warn('immersive layout annotations failed', e);
                layout = null;
            }
            if (req !== layoutReq) return;
            currentLayout = layout;
            annotations.setPageData(layout);
            mountOverlays();
            decorate();
        };

        if (annotations) {
            bag.add(cancelLayout);
            bag.add(viewer.onPageChange.subscribe(refreshLayout));
            bag.add(viewer.onOpen.subscribe(mountOverlays));
            bag.add(
                annotations.onChange((on) => {
                    if (on && currentAnnotations) requestAnimationFrame(() => currentAnnotations && currentAnnotations.relayout());
                })
            );
        }

        if (fulltextPanel && fulltextBox) {
            let fulltextReq = 0;
            let currentLink = null;

            const clearFulltextLink = () => {
                fulltextReq++;
                if (currentLink) {
                    currentLink.destroy();
                    currentLink = null;
                }
                viewer.clearTextRegions();
                viewer.clearAnnotationRegions();
                textRegionEls = new Map();
                clearBlockAnnotations();
                if (fulltextLoader) fulltextLoader.hidden = true;
            };
            bag.add(panels.registerCloseHook(clearFulltextLink));
            bag.add(clearFulltextLink);

            const loadFulltext = async () => {
                const order = viewer.getCurrentOrder();
                clearFulltextLink();
                const req = ++fulltextReq;
                if (fulltextLoader) fulltextLoader.hidden = false;
                fulltextBox.textContent = '';
                fulltextBox.classList.remove('-empty', '-error');
                let blocks = null;
                let failed = false;
                try {
                    blocks = await loadPageTextLevels(pi, apiBase, order);
                } catch {
                    failed = true;
                }
                if (req !== fulltextReq) return;
                if (fulltextLoader) fulltextLoader.hidden = true;
                if (blocks && blocks.length) {
                    fulltextBox.replaceChildren(buildTextLevels(blocks));
                    const flat = flattenTextLevels(blocks);
                    currentFlat = flat;
                    const regionEls = viewer.setTextRegions(flat);
                    textRegionEls = regionEls;
                    const parents = new Map(flat.filter((r) => r.parentId).map((r) => [r.id, r.parentId]));
                    const revealIds = new Set(flat.filter((r) => r.level !== 'block').map((r) => r.id));
                    currentLink = mountTextImageLink({ box: fulltextBox, regionEls, parents, scrollContainer: fulltextPanel, revealIds });
                    decorate();
                } else {
                    currentFlat = null;
                    fulltextBox.textContent = (failed ? fulltextBox.dataset.labelError : fulltextBox.dataset.labelEmpty) || '';
                    fulltextBox.classList.add('-empty');
                    fulltextBox.classList.toggle('-error', failed);
                }
            };

            bag.listen(fulltextPanel, 'immersive:panel-open', loadFulltext);
            bag.listen(fulltextPanel, 'immersive:panel-open', () => {
                if (annotations) refreshLayout();
            });
            bag.add(
                viewer.onPageChange.subscribe(() => {
                    if (panelOpen()) loadFulltext();
                })
            );
        }

        const updateFulltextAvail = () => {
            if (!fulltextBtn) return;
            const doublePage = viewer.isDoublePage();
            fulltextBtn.classList.toggle('-disabled', doublePage);
            fulltextBtn.setAttribute('aria-disabled', String(doublePage));
            fulltextBtn.setAttribute('tabindex', doublePage ? '-1' : '0');
            const title = (doublePage && fulltextBtn.dataset.labelDisabled) || fulltextTitleDefault;
            fulltextBtn.setAttribute('title', title);
            fulltextBtn.setAttribute('aria-label', title);
            if (doublePage && panelOpen()) panels.closePanels();
            if (annotations) refreshLayout();
        };
        updateFulltextAvail();

        return { updateFulltextAvail, destroy: bag.dispose };
    }

    /** Layout-tag panel logic: index/detail loading and parsing, range clamping, hit-list derivation, visibility batching. */

    const indexCache = new Map();

    /** ALTO element name of a drawn stroke (underline, strikethrough, marginal line). */
    const STROKE_ELEMENT_TYPE = 'GraphicalElement';

    /**
     * Modifier class carrying a tag's semantic type; core CSS derives the type's
     * default colour from it. Empty without a type. Pure + tested.
     *
     * @param {string} [type]  underline | strikethrough | mark
     * @returns {string}
     */
    function typeClassOf(type) {
        return type ? `-type-${type}` : '';
    }

    /** Highest number of pages one visibility-driven detail request spans (the endpoint parses one ALTO per page in range). */
    const VISIBLE_BATCH = 8;

    /** Number of tags from which the pick menu shows its free-text filter. */
    const FILTER_THRESHOLD = 13;

    /**
     * Replaces `{0}`, `{1}`, … in a message template with the given values.
     * Pure + tested.
     *
     * @param {string} template
     * @param {...(string|number)} values
     * @returns {string}
     */
    function formatMessage(template, ...values) {
        return String(template || '').replace(/\{(\d+)\}/g, (m, i) => (values[i] != null ? String(values[i]) : ''));
    }

    /**
     * Normalizes the layout-tag index endpoint response: tags without a label are
     * dropped, pages become ascending, duplicate-free finite integers, missing
     * names fall back to the label, a missing group name to the group key.
     * Pure + tested.
     *
     * @param {object} json  the /layout/tags/index response
     * @returns {{pi:string, pageCount:number, tags:{label:string, name:string, styleClass:string, type:string, group:string, groupName:string, groupStyleClass:string, pages:number[]}[]}}
     */
    function parseLayoutTagIndex(json) {
        const src = json && typeof json === 'object' ? json : {};
        const tags = (Array.isArray(src.tags) ? src.tags : [])
            .filter((t) => t && typeof t.label === 'string' && t.label.length > 0)
            .map((t) => {
                const pages = Array.from(new Set((Array.isArray(t.pages) ? t.pages : []).map(Number).filter((p) => Number.isInteger(p) && p >= 1))).sort((a, b) => a - b);
                const group = typeof t.group === 'string' ? t.group : '';
                return {
                    label: t.label,
                    name: typeof t.name === 'string' && t.name ? t.name : t.label,
                    styleClass: typeof t.styleClass === 'string' ? t.styleClass : '',
                    type: typeof t.type === 'string' ? t.type : '',
                    group,
                    groupName: group && typeof t.groupName === 'string' && t.groupName ? t.groupName : group,
                    groupStyleClass: group && typeof t.groupStyleClass === 'string' ? t.groupStyleClass : '',
                    pages,
                };
            });
        return { pi: typeof src.pi === 'string' ? src.pi : '', pageCount: Number.isInteger(src.pageCount) ? src.pageCount : 0, tags };
    }

    /**
     * Parses the `x1,y1,x2,y2` coordinate string of the layout-tag endpoint into
     * an image-pixel rect; null for degenerate or unparsable boxes (the printed
     * label carries `0,0,0,0`). Pure + tested.
     *
     * @param {string} coordinates
     * @returns {{x:number, y:number, w:number, h:number}|null}
     */
    function parseCoordinates(coordinates) {
        const parts = String(coordinates || '')
            .split(',')
            .map((v) => parseFloat(v));
        if (parts.length !== 4 || parts.some((v) => !Number.isFinite(v))) return null;
        const [x1, y1, x2, y2] = parts;
        const w = x2 - x1;
        const h = y2 - y1;
        if (w <= 0 || h <= 0) return null;
        return { x: x1, y: y1, w, h };
    }

    /**
     * Maps a page-range response of `/records/{pi}/layout/tags` to occurrences per
     * page and label. Every page of the requested range gets an entry (pages the
     * response does not mention count as loaded without occurrences), so the
     * caller can mark the whole range as fetched. Occurrences without a drawable
     * rect are skipped. Pure + tested.
     *
     * @param {object} json  the endpoint response (`DocumentReference`)
     * @param {number} start first page of the requested range (1-based, inclusive)
     * @param {number} end   last page of the requested range (inclusive)
     * @returns {Map<number, Object<string, {id:string, rect:object, content:string, elementType:string}[]>>}
     */
    function parseLayoutTagDetails(json, start, end) {
        const details = new Map();
        for (let p = start; p <= end; p++) details.set(p, {});
        const groups = json && Array.isArray(json.pages) ? json.pages : [];
        groups.forEach((group) => {
            const groupPage = Number(group && (group.pageOrder != null ? group.pageOrder : group.firstPage));
            (group && Array.isArray(group.tags) ? group.tags : []).forEach((tag) => {
                if (!tag || typeof tag.value !== 'string') return;
                (Array.isArray(tag.references) ? tag.references : []).forEach((ref) => {
                    if (!ref) return;
                    const page = Number.isInteger(ref.page) ? ref.page : groupPage;
                    if (!Number.isInteger(page)) return;
                    const rect = parseCoordinates(ref.coordinates);
                    if (!rect) return;
                    if (!details.has(page)) details.set(page, {});
                    const byLabel = details.get(page);
                    (byLabel[tag.value] = byLabel[tag.value] || []).push({ id: ref.id || '', rect, content: ref.content || '', elementType: ref.elementType || '' });
                });
            });
        });
        return details;
    }

    /**
     * Pages of a tag inside the range. Pure + tested.
     *
     * @param {{pages:number[]}|null} tag
     * @param {number} from
     * @param {number} to
     * @returns {number[]}
     */
    function tagPages(tag, from, to) {
        return tag ? tag.pages.filter((p) => p >= from && p <= to) : [];
    }

    /**
     * Ascending pages on which any of the given tags occurs inside the range.
     * Pure + tested.
     *
     * @param {{pages:number[]}[]} tags
     * @param {number} from
     * @param {number} to
     * @returns {number[]}
     */
    function hitPages(tags, from, to) {
        const set = new Set();
        tags.forEach((t) => tagPages(t, from, to).forEach((p) => set.add(p)));
        return Array.from(set).sort((a, b) => a - b);
    }

    /**
     * One entry per page and tag, ordered by page, then by the given tag order —
     * a grouped tag keeps its own entries, the group only eases the selection.
     * `occurrencesOf(page, label)` returns the loaded occurrences or undefined
     * while the page detail is still pending. Pure + tested.
     *
     * @param {{label:string, pages:number[]}[]} tags  the selected tags
     * @param {number} from
     * @param {number} to
     * @param {(page:number, label:string) => Array|null|undefined} occurrencesOf  undefined = pending, null = failed
     * @returns {{page:number, label:string, pending:boolean, failed:boolean, count:number}[]}
     */
    function flatEntries(tags, from, to, occurrencesOf) {
        const out = [];
        hitPages(tags, from, to).forEach((page) => {
            tags.forEach((tag) => {
                if (!tag.pages.includes(page)) return;
                const occ = occurrencesOf(page, tag.label);
                out.push({ page, label: tag.label, pending: occ === undefined, failed: occ === null, count: occ ? occ.length : 0 });
            });
        });
        return out;
    }

    /**
     * Stable identity of a hit-list entry (`page#label`). Pure + tested.
     *
     * @param {{page:number, label:string}} entry
     * @returns {string}
     */
    function entryKey(entry) {
        return `${entry.page}#${entry.label}`;
    }

    /**
     * Index of the cursor entry: the exact key, else the first entry on the
     * cursor's page, else 0. Pure + tested.
     *
     * @param {{page:number, label:string}[]} entries
     * @param {string|null} cursorKey
     * @returns {number}
     */
    function cursorIndex(entries, cursorKey) {
        if (!cursorKey) return 0;
        const at = entries.findIndex((e) => entryKey(e) === cursorKey);
        if (at !== -1) return at;
        const page = Number(String(cursorKey).split('#')[0]);
        const onPage = entries.findIndex((e) => e.page === page);
        return onPage !== -1 ? onPage : 0;
    }

    /**
     * Clamps a typed page range to `[1, pageCount]` and swaps reversed bounds;
     * non-numeric input falls back to the respective end. Pure + tested.
     *
     * @param {*} from
     * @param {*} to
     * @param {number} pageCount
     * @returns {{from:number, to:number}}
     */
    function clampRange(from, to, pageCount) {
        const max = Math.max(1, pageCount);
        const clamp = (v, fallback) => {
            const n = Math.round(Number(v));
            return Math.max(1, Math.min(max, Number.isFinite(n) && n > 0 ? n : fallback));
        };
        let a = clamp(from, 1);
        let b = clamp(to, max);
        if (a > b) [a, b] = [b, a];
        return { from: a, to: b };
    }

    /**
     * Pick-menu items in index order: tags sharing a configured group fold into one
     * item at the position of their first member, every other tag is an item of its
     * own. An item's pages are the union of its tags' pages; its style class is the
     * group's own configured class (never a member's), or the tag's. Pure + tested.
     *
     * @param {{label:string, name:string, styleClass?:string, group?:string, groupName?:string, groupStyleClass?:string, pages:number[]}[]} tags
     * @returns {{key:string, name:string, styleClass:string, labels:string[], tags:Array, pages:number[]}[]}
     */
    function groupItems(tags) {
        const items = [];
        const byGroup = new Map();
        (tags || []).forEach((tag) => {
            if (!tag.group) {
                items.push({ key: tag.label, name: tag.name, styleClass: tag.styleClass || '', labels: [tag.label], tags: [tag], pages: tag.pages });
                return;
            }
            let item = byGroup.get(tag.group);
            if (!item) {
                item = { key: tag.group, name: tag.groupName || tag.group, styleClass: '', labels: [], tags: [], pages: [] };
                byGroup.set(tag.group, item);
                items.push(item);
            }
            if (!item.styleClass && tag.groupStyleClass) item.styleClass = tag.groupStyleClass;
            item.labels.push(tag.label);
            item.tags.push(tag);
            item.pages = hitPages(item.tags, 1, Number.MAX_SAFE_INTEGER);
        });
        return items;
    }

    /**
     * Items shown in the pick menu: those with pages in the range or a selected
     * tag, narrowed by the free-text filter (item name, tag names or labels,
     * case-insensitive); items with a selected tag always stay visible.
     * Pure + tested.
     *
     * @param {ReturnType<typeof groupItems>} items
     * @param {{selected:Set<string>, filter:string, from:number, to:number}} opts
     * @returns {Array}
     */
    function filterItems(items, { selected, filter, from, to }) {
        const f = String(filter || '')
            .trim()
            .toLowerCase();
        return items.filter((item) => {
            if (item.labels.some((label) => selected.has(label))) return true;
            if (!tagPages(item, from, to).length) return false;
            return !f || item.name.toLowerCase().includes(f) || item.tags.some((t) => t.name.toLowerCase().includes(f) || t.label.toLowerCase().includes(f));
        });
    }

    /**
     * Pages whose placeholder rows lie within one panel height above or below the
     * visible panel area, in DOM order, duplicate-free, at most `batch`.
     * Pure + tested.
     *
     * @param {{top:number, bottom:number}} panelBox
     * @param {{page:number, top:number, bottom:number}[]} rows  pending rows with their boxes
     * @param {number} batch
     * @returns {number[]}
     */
    function visiblePages(panelBox, rows, batch) {
        const height = panelBox.bottom - panelBox.top;
        const wanted = [];
        rows.forEach((r) => {
            if (r.bottom > panelBox.top - height && r.top < panelBox.bottom + height && !wanted.includes(r.page)) wanted.push(r.page);
        });
        return wanted.slice(0, batch);
    }

    /**
     * Groups pages into contiguous fetch ranges of at most `batch` pages each, so
     * a sparse selection never drags hundreds of untagged pages into one request.
     * Pure + tested.
     *
     * @param {number[]} pages
     * @param {number} batch
     * @returns {{start:number, end:number}[]}
     */
    function batchRanges(pages, batch) {
        const sorted = Array.from(new Set(pages)).sort((a, b) => a - b);
        const ranges = [];
        sorted.forEach((p) => {
            const last = ranges[ranges.length - 1];
            if (last && p - last.start + 1 <= batch) last.end = p;
            else ranges.push({ start: p, end: p });
        });
        return ranges;
    }

    /**
     * Fetches (and memoizes per pi) the record's layout-tag index. The UI language
     * is passed along so the server resolves the display names in it. A failed
     * request is evicted from the cache so a transient error does not poison it.
     *
     * @param {string} pi
     * @param {string} apiBase  base URL of the REST API (no trailing slash)
     * @param {Function} fetchFn fetch-compatible function (injectable for tests)
     * @param {string} [lang]   UI language code for the display names
     * @returns {Promise<ReturnType<typeof parseLayoutTagIndex>>}
     */
    function loadLayoutTagIndex(pi, apiBase, fetchFn = fetch, lang = '') {
        if (indexCache.has(pi)) return indexCache.get(pi);
        const query = lang ? `?lang=${encodeURIComponent(lang)}` : '';
        const promise = fetchFn(`${apiBase}/records/${pi}/layout/tags/index${query}`).then((res) => {
            if (!res.ok) throw new Error(`Failed to load layout-tag index for "${pi}": HTTP ${res.status}`);
            return res.json().then(parseLayoutTagIndex);
        });
        indexCache.set(pi, promise);
        return promise.catch((e) => {
            indexCache.delete(pi);
            throw e;
        });
    }

    /**
     * Fetches the layout-tag occurrences of a page range in one request.
     *
     * @param {string} pi
     * @param {string} apiBase
     * @param {number} start  first page (1-based, inclusive)
     * @param {number} end    last page (inclusive)
     * @param {Function} fetchFn
     * @returns {Promise<Map<number, object>>} see {@link parseLayoutTagDetails}
     */
    async function loadLayoutTagDetails(pi, apiBase, start, end, fetchFn = fetch) {
        const res = await fetchFn(`${apiBase}/records/${pi}/layout/tags/?start=${start}&end=${end}`);
        if (!res.ok) throw new Error(`Failed to load layout tags ${start}-${end} for "${pi}": HTTP ${res.status}`);
        return parseLayoutTagDetails(await res.json(), start, end);
    }

    /** Layout-tag panel state: selection, page range, cursor and per-page details, without any DOM. */

    /**
     * Creates the panel state. Queries derive everything from the index, the
     * selection, the range and the loaded details; transitions keep the cursor
     * valid (it prefers an entry on the pages the viewer currently shows). The
     * selection holds tag labels; the pick items (one per tag, or one per
     * configured group of tags) are derived from the index and only ease the
     * selection — the hit list keeps one entry per tag.
     *
     * @param {number} pageCount
     * @returns {object}
     */
    function createLayoutTagsState(pageCount) {
        const s = { index: null, items: [], failed: false, selected: new Set(), from: 1, to: pageCount, filter: '', cursorKey: null, details: new Map(), inFlight: new Set() };
        const allTags = () => (s.index ? s.index.tags : []);
        const tagByLabel = (label) => allTags().find((t) => t.label === label) || null;
        const itemByKey = (key) => s.items.find((i) => i.key === key) || null;
        const selectedTags = () => allTags().filter((t) => s.selected.has(t.label));
        const selectedItems = () => s.items.filter((i) => i.labels.some((label) => s.selected.has(label)));
        const occurrencesOf = (page, label) => {
            if (!s.details.has(page)) return undefined;
            const byLabel = s.details.get(page);
            return byLabel ? byLabel[label] || [] : null;
        };
        const entries = () => flatEntries(selectedTags(), s.from, s.to, occurrencesOf);
        const preferCursorOnPages = (pages) => {
            const list = entries();
            const onPage = list.find((e) => pages.includes(e.page));
            if (onPage) s.cursorKey = entryKey(onPage);
            else if (!s.cursorKey && list.length) s.cursorKey = entryKey(list[0]);
        };
        const keepOrResetCursor = (pages) => {
            if (!s.selected.size) {
                s.cursorKey = null;
                return;
            }
            if (!entries().some((e) => entryKey(e) === s.cursorKey)) {
                s.cursorKey = null;
                preferCursorOnPages(pages);
            }
        };
        return {
            get pageCount() {
                return pageCount;
            },
            get index() {
                return s.index;
            },
            get failed() {
                return s.failed;
            },
            get selected() {
                return s.selected;
            },
            get from() {
                return s.from;
            },
            get to() {
                return s.to;
            },
            get cursorKey() {
                return s.cursorKey;
            },
            get details() {
                return s.details;
            },
            get inFlight() {
                return s.inFlight;
            },
            allTags,
            tagByLabel,
            itemByKey,
            selectedTags,
            selectedItems,
            occurrencesOf,
            entries,
            cursorEntry() {
                const list = entries();
                return list[cursorIndex(list, s.cursorKey)] || null;
            },
            /** Display name of a pick item key or a tag label. */
            displayName(key) {
                const item = itemByKey(key);
                if (item) return item.name;
                const tag = tagByLabel(key);
                return tag ? tag.name : key;
            },
            /** Style tokens of a pick item key (a group's own configured class) or a tag label; empty without a configured class. */
            styleTokens(key) {
                const item = itemByKey(key);
                if (item) return styleClassTokens(item.styleClass);
                const tag = tagByLabel(key);
                return tag ? styleClassTokens(tag.styleClass) : [];
            },
            tagType(label) {
                const tag = tagByLabel(label);
                return tag && tag.type ? tag.type : '';
            },
            /** Whether 'all', 'some' or 'none' of an item's tags are selected. */
            itemSelection(key) {
                const item = itemByKey(key);
                if (!item) return s.selected.has(key) ? 'all' : 'none';
                const on = item.labels.filter((label) => s.selected.has(label)).length;
                return on === item.labels.length ? 'all' : on ? 'some' : 'none';
            },
            visibleItems: () => filterItems(s.items, s),
            inRangeCount: (item) => tagPages(item, s.from, s.to).length,
            rangeIsFull: () => s.from === 1 && s.to === pageCount,
            compositionKey: () => [s.failed, Array.from(s.selected).sort().join(','), s.from, s.to].join('|'),
            progress() {
                const pages = hitPages(selectedTags(), s.from, s.to);
                return { total: pages.length, loaded: pages.filter((p) => s.details.has(p)).length, failed: pages.some((p) => s.details.get(p) === null) };
            },
            setIndex(index) {
                s.index = index;
                s.items = groupItems(index ? index.tags : []);
                s.failed = false;
            },
            setFailed() {
                s.failed = true;
            },
            setFilter(text) {
                s.filter = text;
            },
            setCursor(key) {
                s.cursorKey = key;
            },
            preferCursorOnPages,
            /** Toggles a pick item: all of its tags off when they all were on, else all on. A tag label toggles that tag alone. */
            toggleItem(key, currentPages) {
                const item = itemByKey(key);
                const labels = item ? item.labels : [key];
                const allOn = labels.every((label) => s.selected.has(label));
                labels.forEach((label) => (allOn ? s.selected.delete(label) : s.selected.add(label)));
                keepOrResetCursor(currentPages);
            },
            setRange(from, to, currentPages) {
                const next = clampRange(from, to, pageCount);
                if (next.from === s.from && next.to === s.to) return false;
                s.from = next.from;
                s.to = next.to;
                keepOrResetCursor(currentPages);
                return true;
            },
            missingPages: (pages) => pages.filter((p) => !s.details.has(p) && !s.inFlight.has(p)),
            markInFlight(pages) {
                pages.forEach((p) => s.inFlight.add(p));
            },
            storeDetails(pages, details) {
                pages.forEach((p) => {
                    s.inFlight.delete(p);
                    s.details.set(p, details ? details.get(p) || {} : null);
                });
            },
        };
    }

    /** Layout-tag pick menu: multi-select rows (one per tag or configured group) with optional filter; the trigger shows the selection as text and colour dots. */

    const TRIGGER_DOTS = 4;

    /**
     * @param {object} opts
     * @param {object} opts.state  the panel state (createLayoutTagsState)
     * @param {DOMStringMap} opts.labels  data-label-* of the panel
     * @param {HTMLElement} opts.trigger
     * @param {HTMLElement} opts.menu
     * @param {HTMLElement} opts.list
     * @param {HTMLElement|null} opts.dots
     * @param {HTMLElement|null} opts.text
     * @param {HTMLElement|null} opts.filterWrap
     * @param {HTMLInputElement|null} opts.filterInput
     * @param {{register: Function}} opts.keys
     * @param {(key: string) => void} opts.onToggle  called with the pick item's key (a tag label or a group key)
     * @returns {{build: Function, sync: Function, close: Function, setFilterVisible: Function, destroy: Function}}
     */
    function setupPickMenu({ state, labels, trigger, menu, list, dots, text, filterWrap, filterInput, keys, onToggle }) {
        const bag = createDisposer();
        const isOpen = () => !menu.hidden;
        const onOutside = (e) => {
            if (!menu.contains(e.target) && !trigger.contains(e.target)) close();
        };
        /** Opens the menu; `focus` is false when it opens on its own, so it never steals the focus. */
        const open = ({ focus = true } = {}) => {
            if (isOpen()) return;
            menu.hidden = false;
            trigger.setAttribute('aria-expanded', 'true');
            document.addEventListener('pointerdown', onOutside, true);
            if (!focus) return;
            const first = filterWrap && !filterWrap.hidden && filterInput ? filterInput : menu.querySelector('input[type="checkbox"]:not([disabled])');
            if (first) first.focus();
        };
        const close = (restoreFocus = false) => {
            if (!isOpen()) return;
            menu.hidden = true;
            trigger.setAttribute('aria-expanded', 'false');
            document.removeEventListener('pointerdown', onOutside, true);
            if (restoreFocus) trigger.focus();
        };

        const syncRows = () => {
            list.querySelectorAll('.immersive__layout-tags-pick-row').forEach((row) => {
                const item = state.itemByKey(row.dataset.itemKey);
                if (!item) return;
                const inRange = state.inRangeCount(item);
                const selection = state.itemSelection(item.key);
                const cb = row.querySelector('input');
                const count = row.querySelector('.immersive__layout-tags-pick-count');
                row.classList.toggle('-empty', !inRange);
                cb.checked = selection === 'all';
                cb.indeterminate = selection === 'some';
                cb.disabled = !inRange && selection === 'none';
                if (count) count.textContent = String(inRange);
            });
        };

        const build = () => {
            list.replaceChildren();
            const items = state.visibleItems();
            if (!items.length) {
                list.append(el('p', 'immersive__layout-tags-pick-none', labels.labelFilterNone || ''));
                return;
            }
            items.forEach((item, i) => {
                const row = el('div', 'immersive__layout-tags-pick-row');
                const tokens = state.styleTokens(item.key);
                row.classList.add(...tokens);
                row.dataset.itemKey = item.key;
                const id = `immersiveLayoutTagsPick-${i}`;
                const cb = el('input', 'immersive__layout-tags-pick-checkbox');
                cb.type = 'checkbox';
                cb.id = id;
                cb.addEventListener('change', () => onToggle(item.key));
                const label = el('label', 'immersive__layout-tags-pick-label');
                label.htmlFor = id;
                label.title = item.labels.join(', ');
                // a colour dot only for entries with a configured style class
                if (tokens.length) {
                    const dot = el('span', 'immersive__layout-tags-dot');
                    dot.setAttribute('aria-hidden', 'true');
                    label.append(dot);
                }
                label.append(el('span', 'immersive__layout-tags-pick-name', item.name));
                row.append(cb, label, el('span', 'immersive__layout-tags-pick-count', String(state.inRangeCount(item))));
                list.append(row);
            });
            syncRows();
        };

        const sync = () => {
            const chosen = state.selectedItems();
            if (dots) {
                dots.replaceChildren();
                chosen
                    .filter((item) => state.styleTokens(item.key).length)
                    .slice(0, TRIGGER_DOTS)
                    .forEach((item) => {
                        const dot = el('span', 'immersive__layout-tags-dot');
                        dot.classList.add(...state.styleTokens(item.key));
                        dots.append(dot);
                    });
            }
            let caption = labels.labelPick || '';
            if (chosen.length === 1 || chosen.length === 2) caption = chosen.map((t) => t.name).join(', ');
            else if (chosen.length > 2) caption = formatMessage(labels.labelPickMore, chosen[0].name, chosen.length - 1);
            if (text) text.textContent = caption;
            trigger.setAttribute('aria-label', chosen.length ? formatMessage(labels.labelPickSelected, chosen.length) : labels.labelPick || '');
            syncRows();
        };

        bag.listen(trigger, 'click', () => (isOpen() ? close() : open()));
        if (filterInput) {
            bag.listen(filterInput, 'input', () => {
                state.setFilter(filterInput.value);
                build();
            });
        }
        bag.add(
            keys.register(KEY_PRIORITY.layoutTagsPick, (e) => {
                if (e.key !== 'Escape' || !isOpen()) return false;
                close(true);
                return true;
            })
        );

        return {
            build,
            sync,
            open,
            close,
            setFilterVisible(visible) {
                if (filterWrap) filterWrap.hidden = !visible;
            },
            destroy() {
                close();
                bag.dispose();
            },
        };
    }

    /** Layout-tag hit list: rows grouped by page, per-page counts and error markers, the active row and the progress line. */

    const REVEAL_MARGIN = 8;

    /**
     * @param {object} opts
     * @param {object} opts.state  the panel state (createLayoutTagsState)
     * @param {DOMStringMap} opts.labels  data-label-* of the panel
     * @param {HTMLElement} opts.mount
     * @param {HTMLElement} opts.panel  the scroll container
     * @param {HTMLElement|null} opts.sticky
     * @param {HTMLElement|null} opts.progress
     * @param {(index: number) => void} opts.onActivate
     * @returns {{render: Function, markActive: Function, pendingRows: Function}}
     */
    function createHitList({ state, labels, mount, panel, sticky, progress, onActivate }) {
        let revealedKey = null;
        let builtKey = null;

        const syncCounts = () => {
            mount.querySelectorAll('.immersive__layout-tags-hit').forEach((btn) => {
                const page = Number(btn.dataset.pageNo);
                const label = btn.dataset.tagLabel;
                const occ = state.occurrencesOf(page, label);
                const count = occ ? occ.length : 0;
                const countEl = btn.querySelector('.immersive__layout-tags-hit-count');
                btn.classList.toggle('-pending', occ === undefined);
                btn.classList.toggle('-error', occ === null);
                countEl.textContent = occ === undefined ? '…' : occ === null ? '!' : count > 1 ? `(${count})` : '';
                let name = `${state.displayName(label)}, ${labels.labelPage || ''} ${page}`;
                if (occ === null) name += `, ${labels.labelError || ''}`;
                else if (occ !== undefined && count > 1) name += `, ${formatMessage(labels.labelOccurrences, count)}`;
                btn.setAttribute('aria-label', name);
                btn.title = occ === null ? labels.labelError || '' : '';
            });
        };

        const emptyText = () => {
            if (state.failed) return labels.labelError || '';
            if (!state.selected.size) return labels.labelNoneSelected || '';
            return (state.rangeIsFull() ? labels.labelEmpty : labels.labelEmptyRange) || '';
        };

        const build = () => {
            revealedKey = null;
            mount.replaceChildren();
            const list = state.failed || !state.selected.size ? [] : state.entries();
            if (!list.length) {
                mount.append(el('p', 'immersive__layout-tags-empty', emptyText()));
                return;
            }
            const ul = el('ul', 'immersive__layout-tags-hits');
            let currentPage = null;
            let typeList = null;
            list.forEach((entry, i) => {
                if (entry.page !== currentPage) {
                    currentPage = entry.page;
                    const pageLi = el('li', 'immersive__layout-tags-page');
                    const pageNo = el('span', 'immersive__layout-tags-pageno');
                    pageNo.append(el('span', 'sr-only', `${labels.labelPage || ''} `), document.createTextNode(String(entry.page)));
                    typeList = el('ul', 'immersive__layout-tags-types');
                    pageLi.append(pageNo, typeList);
                    ul.append(pageLi);
                }
                const item = el('li');
                const btn = el('button', 'immersive__layout-tags-hit');
                btn.type = 'button';
                const tokens = state.styleTokens(entry.label);
                btn.classList.add(...tokens);
                btn.dataset.spotIndex = String(i);
                btn.dataset.spotKey = entryKey(entry);
                btn.dataset.pageNo = String(entry.page);
                btn.dataset.tagLabel = entry.label;
                // a colour dot only for tags with a configured style class
                if (tokens.length) {
                    const dot = el('span', 'immersive__layout-tags-dot');
                    dot.setAttribute('aria-hidden', 'true');
                    btn.append(dot);
                }
                btn.append(el('span', 'immersive__layout-tags-hit-name', state.displayName(entry.label)), el('span', 'immersive__layout-tags-hit-count'));
                btn.addEventListener('click', () => onActivate(i));
                item.append(btn);
                typeList.append(item);
            });
            mount.append(ul);
            syncCounts();
        };

        const revealActive = () => {
            if (state.cursorKey === revealedKey) return;
            const row = mount.querySelector('.immersive__layout-tags-hit.-active');
            if (!row) return;
            revealedKey = state.cursorKey;
            const stickyHeight = sticky ? sticky.offsetHeight : 0;
            const top = row.offsetTop - panel.offsetTop;
            const bottom = top + row.offsetHeight;
            const viewTop = panel.scrollTop + stickyHeight;
            const viewBottom = panel.scrollTop + panel.clientHeight;
            if (top < viewTop) panel.scrollTop = Math.max(0, top - stickyHeight - REVEAL_MARGIN);
            else if (bottom > viewBottom) panel.scrollTop = bottom - panel.clientHeight + REVEAL_MARGIN;
        };

        const syncProgress = () => {
            if (!progress) return;
            const { total, loaded, failed } = state.progress();
            let text = '';
            if (failed) text = labels.labelError || '';
            else if (total && loaded < total) text = formatMessage(labels.labelProgressLoading, loaded, total);
            else if (total === 1) text = labels.labelProgressDoneOne || '';
            else if (total) text = formatMessage(labels.labelProgressDone, total);
            progress.textContent = text;
        };

        const markActive = () => {
            const list = state.entries();
            const cursor = cursorIndex(list, state.cursorKey);
            mount.querySelectorAll('.immersive__layout-tags-hit').forEach((btn) => {
                const on = list.length > 0 && Number(btn.dataset.spotIndex) === cursor;
                btn.classList.toggle('-active', on);
                if (on) btn.setAttribute('aria-current', 'true');
                else btn.removeAttribute('aria-current');
            });
            revealActive();
            syncProgress();
        };

        const render = (force = false) => {
            const key = state.compositionKey();
            if (force || key !== builtKey) {
                builtKey = key;
                const focused = document.activeElement;
                const focusKey = focused && focused.dataset ? focused.dataset.spotKey : null;
                build();
                if (focusKey) {
                    const again = mount.querySelector(`[data-spot-key="${focusKey}"]`);
                    if (again) again.focus({ preventScroll: true });
                }
            } else {
                syncCounts();
            }
            markActive();
        };

        const pendingRows = () =>
            Array.from(mount.querySelectorAll('.immersive__layout-tags-hit.-pending'))
                .filter((btn) => !state.inFlight.has(Number(btn.dataset.pageNo)))
                .map((btn) => {
                    const r = btn.getBoundingClientRect();
                    return { page: Number(btn.dataset.pageNo), top: r.top, bottom: r.bottom };
                });

        return { render, markActive, pendingRows };
    }

    /** Layout-tag panel wiring: index and detail loading, page range, cursor and image highlights; pick menu and hit list are sub-modules. */

    const OWNER = 'layout-tags';
    /** Modifier drawing an occurrence as a bar instead of a box. */
    const STROKE_CLASS = '-stroke';

    /**
     * Wires the layout-tag panel: the record-wide index is fetched once (on the
     * first open) and drives a multi-select pick menu (one entry per tag, or per
     * configured group of tags that are selected together), a page-range row and
     * a hit list grouped by page with one row per tag. Page details (occurrence
     * counts and rects) are loaded only for the rows in view, in small page-range
     * batches. Activating a hit opens the page and highlights every occurrence of
     * that tag on it; the highlights are redrawn on page change and cleared when
     * the panel closes.
     *
     * @param {IvViewer} viewer
     * @param {string} pi
     * @param {string} apiBase
     * @param {{closePanels: Function, registerCloseHook: Function}} panels
     * @param {{register: Function}} keys  the immersive key dispatcher
     * @returns {{destroy: function():void}}
     */
    function setupLayoutTagsPanel(viewer, pi, apiBase, panels, keys) {
        const panel = document.getElementById('immersivePanelLayoutTags');
        if (!panel) return noop;
        const q = (sel) => panel.querySelector(sel);
        const sticky = q('[data-immersive-layout-tags="sticky"]');
        const rangeFrom = q('[data-immersive-layout-tags="from"]');
        const rangeTo = q('[data-immersive-layout-tags="to"]');
        const rangeApply = q('[data-immersive-layout-tags="apply"]');
        const rangeReset = q('[data-immersive-layout-tags="reset"]');
        const pickTrigger = q('[data-immersive-layout-tags="pick-trigger"]');
        const pickMenu = q('[data-immersive-layout-tags="pick-menu"]');
        const pickList = q('[data-immersive-layout-tags="pick-list"]');
        const filterWrap = q('[data-immersive-layout-tags="filter"]');
        const loader = q('[data-immersive-layout-tags="loader"]');
        const hitsMount = q('[data-immersive-layout-tags="hits"]');
        if (!rangeFrom || !rangeTo || !pickTrigger || !pickMenu || !pickList || !hitsMount) return noop;
        const bag = createDisposer();
        let destroyed = false;
        const labels = panel.dataset;
        const state = createLayoutTagsState(viewer.getPageCount());
        const panelOpen = () => panel.classList.contains('is-open');
        const currentPages = () => viewer.getCurrentPages().map((o) => o + 1);

        const pick = setupPickMenu({
            state,
            labels,
            trigger: pickTrigger,
            menu: pickMenu,
            list: pickList,
            dots: q('[data-immersive-layout-tags="pick-dots"]'),
            text: q('[data-immersive-layout-tags="pick-text"]'),
            filterWrap,
            filterInput: filterWrap && filterWrap.querySelector('input'),
            keys,
            onToggle: (key) => toggleItem(key),
        });
        bag.add(() => pick.destroy());
        const hits = createHitList({ state, labels, mount: hitsMount, panel, sticky, progress: q('[data-immersive-layout-tags="progress"]'), onActivate: (i) => goToEntry(i) });

        /* PAGE RANGE */

        const syncRangeButtons = () => {
            const dirty = Number(rangeFrom.value) !== state.from || Number(rangeTo.value) !== state.to;
            if (rangeApply) rangeApply.hidden = !dirty;
            if (rangeReset) rangeReset.hidden = dirty || state.rangeIsFull();
        };
        const syncRange = () => {
            const editing = document.activeElement === rangeFrom || document.activeElement === rangeTo;
            if (!editing) {
                rangeFrom.value = String(state.from);
                rangeTo.value = String(state.to);
            }
            syncRangeButtons();
        };
        const setRange = (from, to) => {
            if (!state.setRange(from, to, currentPages())) {
                syncRange();
                return;
            }
            refresh(true);
        };
        const applyRange = () => {
            setRange(rangeFrom.value === '' ? 1 : rangeFrom.value, rangeTo.value === '' ? state.pageCount : rangeTo.value);
            rangeFrom.value = String(state.from);
            rangeTo.value = String(state.to);
            syncRangeButtons();
            if (document.activeElement === document.body) rangeFrom.focus();
        };
        [rangeFrom, rangeTo].forEach((input) => {
            input.max = String(state.pageCount);
            bag.listen(input, 'input', syncRangeButtons);
            bag.listen(input, 'keydown', (e) => {
                if (e.key !== 'Enter') return;
                e.preventDefault();
                applyRange();
            });
        });
        if (rangeApply) bag.listen(rangeApply, 'click', applyRange);
        if (rangeReset) {
            bag.listen(rangeReset, 'click', () => {
                setRange(1, state.pageCount);
                rangeFrom.value = String(state.from);
                rangeTo.value = String(state.to);
                syncRangeButtons();
            });
        }

        /* DETAIL LOADING */

        const loadPages = (pages) => {
            const missing = state.missingPages(pages);
            if (!missing.length) return;
            batchRanges(missing, VISIBLE_BATCH).forEach(({ start, end }) => {
                const span = [];
                for (let p = start; p <= end; p++) {
                    if (!state.details.has(p) && !state.inFlight.has(p)) span.push(p);
                }
                state.markInFlight(span);
                loadLayoutTagDetails(pi, apiBase, start, end)
                    .then((details) => state.storeDetails(span, details))
                    .catch((e) => {
                        console.warn('immersive layout tags failed', e);
                        state.storeDetails(span, null);
                    })
                    .then(() => {
                        if (destroyed) return;
                        if (!state.cursorKey) state.preferCursorOnPages(currentPages());
                        render();
                        renderHighlights();
                        pumpVisible();
                    })
                    .catch((e) => console.warn('immersive layout tags render failed', e));
            });
        };
        const pumpVisible = () => {
            if (!panelOpen() || !state.selected.size) return;
            const wanted = visiblePages(panel.getBoundingClientRect(), hits.pendingRows(), VISIBLE_BATCH);
            if (wanted.length) loadPages(wanted);
        };
        const loadCursorPage = () => {
            const active = state.cursorEntry();
            if (active && active.pending) loadPages([active.page]);
        };

        /* CURSOR AND HIGHLIGHTS */

        const render = (force = false) => {
            hits.render(force);
            pick.sync();
            syncRange();
        };
        const refresh = (force) => {
            render(force);
            loadCursorPage();
            pumpVisible();
            renderHighlights();
        };
        const toggleItem = (key) => {
            state.toggleItem(key, currentPages());
            refresh(true);
        };
        const goToEntry = (i) => {
            const list = state.entries();
            if (!list.length) return;
            const entry = list[Math.max(0, Math.min(i, list.length - 1))];
            state.setCursor(entryKey(entry));
            hits.markActive();
            if (entry.pending) loadPages([entry.page]);
            if (!currentPages().includes(entry.page)) viewer.goToPage(entry.page - 1);
            else renderHighlights();
        };
        const renderHighlights = () => {
            if (!panelOpen()) return;
            const active = state.cursorEntry();
            if (!active || !currentPages().includes(active.page)) {
                viewer.clearHighlights(OWNER);
                return;
            }
            const tokens = [...state.styleTokens(active.label), typeClassOf(state.tagType(active.label))];
            const rects = (state.occurrencesOf(active.page, active.label) || []).map((o) => ({
                ...o.rect,
                order: active.page - 1,
                className: [...tokens, o.elementType === STROKE_ELEMENT_TYPE ? STROKE_CLASS : ''].filter(Boolean).join(' '),
                active: true,
            }));
            viewer.setHighlights(rects, OWNER);
        };

        /* INDEX AND EVENTS */

        let indexRequest = null;
        const ensureIndex = () => {
            if (state.index) return Promise.resolve(state.index);
            if (indexRequest) return indexRequest;
            if (loader) loader.hidden = false;
            indexRequest = loadLayoutTagIndex(pi, apiBase, fetch, document.documentElement.lang)
                .then((index) => {
                    state.setIndex(index);
                    pick.setFilterVisible(index.tags.length >= FILTER_THRESHOLD);
                    pick.build();
                    return index;
                })
                .catch((e) => {
                    console.error('immersive layout-tag index failed', e);
                    state.setFailed();
                    return null;
                })
                .then((index) => {
                    indexRequest = null;
                    if (loader) loader.hidden = true;
                    return index;
                });
            return indexRequest;
        };

        bag.listen(panel, 'immersive:panel-open', () => {
            ensureIndex().then(() => {
                if (destroyed || !panelOpen()) return;
                if (!state.cursorKey) state.preferCursorOnPages(currentPages());
                refresh(true);
                if (!state.selected.size && state.allTags().length) {
                    pick.open({ focus: false });
                }
            });
        });
        bag.add(
            panels.registerCloseHook(() => {
                if (!panelOpen()) return;
                pick.close();
                viewer.clearHighlights(OWNER);
            })
        );
        bag.add(
            viewer.onPageChange.subscribe(() => {
                if (!panelOpen() || !state.index) return;
                const pages = currentPages();
                const cursor = state.cursorEntry();
                const onPage = cursor && pages.includes(cursor.page) ? cursor : state.entries().find((e) => pages.includes(e.page));
                if (onPage) state.setCursor(entryKey(onPage));
                hits.markActive();
                renderHighlights();
                if (onPage && onPage.pending) loadPages([onPage.page]);
            })
        );
        bag.add(viewer.onOpen.subscribe(renderHighlights));
        let scrollQueued = false;
        bag.listen(panel, 'scroll', () => {
            if (scrollQueued) return;
            scrollQueued = true;
            requestAnimationFrame(() => {
                scrollQueued = false;
                if (sticky) sticky.classList.toggle('-scrolled', panel.scrollTop > 0);
                pumpVisible();
            });
        });

        syncRange();
        return {
            destroy() {
                destroyed = true;
                pick.close();
                viewer.clearHighlights(OWNER);
                bag.dispose();
            },
        };
    }

    /** Extracts the snippet text from a IIIF `resource` (object, array, or missing). */
    function _snippet(resource) {
        const r = Array.isArray(resource) ? resource[0] : resource;
        return r && typeof r.value === 'string' ? r.value : '';
    }

    /**
     * Indexes the `search:Hit` list by annotation id → surrounding context
     * (`before` / `match` / `after`), for building a teaser around each match.
     */
    function _contextByAnnotation(annotationList) {
        const map = new Map();
        const hits = Array.isArray(annotationList.hits) ? annotationList.hits : [];
        for (const hit of hits) {
            const annos = Array.isArray(hit.annotations) ? hit.annotations : hit.annotations ? [hit.annotations] : [];
            for (const id of annos) {
                map.set(id, { before: hit.before, match: hit.match, after: hit.after });
            }
        }
        return map;
    }

    /**
     * Parses a IIIF Content Search `sc:AnnotationList` into hits. Pages are 1-based
     * (as in the `on` URL `/pages/{n}/canvas`); the caller converts to the 0-based
     * IvViewer order. `before`/`match`/`after` come from the `hits` block and are
     * undefined when no context is provided.
     *
     * @returns {{id:string, page:number, rect:{x,y,w,h}|null, snippet:string, before?:string, match?:string, after?:string}[]}
     */
    function parseSearchHits(annotationList) {
        if (!annotationList || !Array.isArray(annotationList.resources)) return [];
        const context = _contextByAnnotation(annotationList);
        const hits = [];
        for (const res of annotationList.resources) {
            const on = typeof res.on === 'string' ? res.on : (res.on && res.on['@id']) || '';
            const page = on.match(/\/pages\/(\d+)\/canvas/);
            if (!page) continue;
            const xywh = on.match(/#xywh=(\d+),(\d+),(\d+),(\d+)/);
            const id = res['@id'] || res.id;
            const ctx = context.get(id) || {};
            hits.push({
                id,
                page: Number(page[1]),
                rect: xywh ? { x: +xywh[1], y: +xywh[2], w: +xywh[3], h: +xywh[4] } : null,
                snippet: _snippet(res.resource),
                before: ctx.before,
                match: ctx.match,
                after: ctx.after,
            });
        }
        return hits;
    }

    /** Next hit index with wrap-around; -1 when there are no hits. */
    function nextIndex(i, total) {
        return total ? (i + 1) % total : -1;
    }

    /** Previous hit index with wrap-around; -1 when there are no hits. */
    function prevIndex(i, total) {
        return total ? (i - 1 + total) % total : -1;
    }

    /** Last result page from `within.last` (`…&page=N`); 1 when absent. */
    function _lastPage(list) {
        const last = list && list.within && list.within.last;
        const m = typeof last === 'string' ? last.match(/[?&]page=(\d+)/) : null;
        return m ? Number(m[1]) : 1;
    }

    /**
     * Parses the IIIF search response tolerantly against a backend bug: a `search:Hit`
     * without annotations is serialized as `{"@type":"search:Hit","annotations"}` (key
     * without value) → invalid JSON. Plain parsing is tried first so valid responses
     * are never rewritten.
     * TODO(iiif-api-model): remove once the URLOnlySerializer fix lands (empty
     * annotations → valid JSON) — then a plain `res.json()` suffices.
     */
    function _parseSearchJson(text) {
        try {
            return JSON.parse(text);
        } catch {
            try {
                return JSON.parse(text.replace(/"annotations"\}/g, '"annotations":[]}'));
            } catch {
                return {};
            }
        }
    }

    /**
     * Fetches all result pages of the IIIF Content Search and returns hits with the
     * 0-based IvViewer `order` (order = page - 1), in document order. Tolerates
     * broken backend paging (page 2+ may restart from the top): hits are deduped by
     * annotation id and fetching stops once a page adds nothing new.
     *
     * @param {string} pi        Goobi viewer record identifier
     * @param {string} apiBase   base URL of the REST API (no trailing slash)
     * @param {string} term      search term
     * @param {Function} fetchFn fetch-compatible function (injectable for tests)
     * @param {number} maxPages  safety cap for the within-paging loop
     * @throws when the FIRST result page fails -- an unreachable or forbidden search endpoint must
     *         not read as "no hits"; a later page failing keeps the hits collected so far.
     */
    async function search(pi, apiBase, term, fetchFn = fetch, maxPages = 50) {
        const base = `${apiBase}/records/${pi}/manifest/search?q=${encodeURIComponent(term)}`;
        const seen = new Set();
        const out = [];
        for (let p = 1; p <= maxPages; p++) {
            const res = await fetchFn(p === 1 ? base : `${base}&page=${p}`);
            if (!res.ok) {
                if (p === 1) throw new Error(`Failed to search "${term}" in "${pi}": HTTP ${res.status}`);
                break;
            }
            const list = _parseSearchJson(await res.text());
            const hits = parseSearchHits(list);
            let added = 0;
            for (const h of hits) {
                const rect = h.rect ? `${h.rect.x},${h.rect.y},${h.rect.w},${h.rect.h}` : '';
                const key = h.id || `${h.page}|${rect}|${h.snippet}`;
                if (seen.has(key)) continue;
                seen.add(key);
                out.push({ ...h, order: h.page - 1 });
                added++;
            }
            if (hits.length === 0 || added === 0 || p >= _lastPage(list)) break;
        }
        return out;
    }

    /** Search panel wiring: in-work IIIF content search with hit list and highlights. */

    /**
     * Wires the in-place IIIF content search: result list in the left panel,
     * prev/next hit buttons, and hit highlights on the image.
     *
     * @param {IvViewer} viewer
     * @param {string} pi
     * @param {string} apiBase
     * @returns {{destroy: function():void}}
     */
    function setupSearchPanel(viewer, pi, apiBase) {
        const bag = createDisposer();
        const searchState = { hits: [], activeIndex: -1, term: '', failed: false };
        let searchReq = 0;
        const resultsBox = document.getElementById('immersiveSearchResults');
        const resultsList = document.getElementById('immersiveResultsList');
        const hitCounter = document.getElementById('immersiveHitCounter');
        const hitsOnOrder = (order) => searchState.hits.filter((h) => h.order === order);

        const renderHighlights = () => {
            const activeHit = searchState.hits[searchState.activeIndex];
            const rects = viewer.getCurrentPages().flatMap((o) =>
                hitsOnOrder(o)
                    .filter((h) => h.rect)
                    .map((h) => ({ ...h.rect, order: o, active: h === activeHit }))
            );
            viewer.setHighlights(rects, 'search');
        };

        const renderResults = () => {
            if (!resultsBox || !resultsList) return;
            resultsBox.hidden = !searchState.term;
            const emptyLabel = (searchState.failed ? resultsBox.dataset.labelError : resultsBox.dataset.labelEmpty) || '';
            if (hitCounter) {
                hitCounter.textContent = searchState.hits.length ? `${searchState.activeIndex + 1} / ${searchState.hits.length}` : (searchState.term && emptyLabel) || '';
            }
            resultsList.replaceChildren();
            if (searchState.term && !searchState.hits.length) {
                const empty = document.createElement('li');
                empty.className = 'immersive__results-empty';
                empty.textContent = emptyLabel;
                resultsList.appendChild(empty);
                return;
            }
            searchState.hits.forEach((h, i) => {
                const li = document.createElement('li');
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'immersive__results-item' + (i === searchState.activeIndex ? ' -active' : '');
                const page = document.createElement('span');
                page.className = 'immersive__results-page';
                page.textContent = h.page;
                const snippet = document.createElement('span');
                snippet.className = 'immersive__results-snippet';
                if (h.before || h.after) {
                    const mark = document.createElement('mark');
                    mark.className = 'immersive__results-match';
                    mark.textContent = h.match || h.snippet || '';
                    snippet.append(document.createTextNode(h.before ? `${h.before} ` : ''), mark, document.createTextNode(h.after ? ` ${h.after}` : ''));
                } else {
                    snippet.textContent = h.snippet || '';
                }
                btn.append(page, snippet);
                btn.addEventListener('click', () => goToHit(i));
                li.append(btn);
                resultsList.appendChild(li);
            });
        };

        const goToHit = (i) => {
            if (!searchState.hits.length) return;
            searchState.activeIndex = (i + searchState.hits.length) % searchState.hits.length;
            const hit = searchState.hits[searchState.activeIndex];
            if (!viewer.getCurrentPages().includes(hit.order)) viewer.goToPage(hit.order);
            else renderHighlights();
            renderResults();
        };

        /** Only the latest submitted search may render; a slower older response is dropped. */
        const runSearch = async (term) => {
            const req = ++searchReq;
            let hits = [];
            let failed = false;
            if (term) {
                try {
                    hits = await search(pi, apiBase, term);
                } catch (e) {
                    console.error('immersive fulltext search failed', e);
                    failed = true;
                }
            }
            if (req !== searchReq) return;
            searchState.term = term;
            searchState.hits = hits;
            searchState.failed = failed;
            searchState.activeIndex = hits.length ? 0 : -1;
            renderResults();
            if (searchState.activeIndex >= 0) goToHit(0);
            else viewer.clearHighlights('search');
        };

        bag.add(viewer.onPageChange.subscribe(renderHighlights));

        const searchPanel = document.getElementById('immersivePanelSearch');
        const searchForm = searchPanel && searchPanel.querySelector('form');
        const searchInput = searchPanel && searchPanel.querySelector('input[type="text"]');
        if (searchForm && searchInput) {
            bag.listen(searchForm, 'submit', (e) => {
                e.preventDefault();
                runSearch(searchInput.value.trim());
            });

            const clearBtn = document.createElement('button');
            clearBtn.type = 'button';
            clearBtn.className = 'immersive__search-clear';
            clearBtn.textContent = '×';
            clearBtn.setAttribute('aria-label', (resultsBox && resultsBox.dataset.labelReset) || '');
            clearBtn.hidden = !searchInput.value;
            const group = searchInput.closest('.input-group') || searchInput.parentElement;
            group.insertBefore(clearBtn, group.querySelector('.input-group-addon') || null);
            bag.add(() => clearBtn.remove());
            const syncClear = () => {
                clearBtn.hidden = !searchInput.value;
            };
            const resetSearch = () => {
                searchInput.value = '';
                syncClear();
                runSearch('');
                searchInput.focus();
            };
            bag.listen(searchInput, 'input', syncClear);
            bag.listen(clearBtn, 'click', resetSearch);
            bag.listen(searchInput, 'keydown', (e) => {
                if (e.key === 'Escape' && searchInput.value) {
                    e.preventDefault();
                    e.stopPropagation();
                    resetSearch();
                }
            });
        }
        document.querySelectorAll('[data-immersive-hit]').forEach((btn) => {
            bag.listen(btn, 'click', () => {
                const n = searchState.hits.length;
                goToHit(btn.dataset.immersiveHit === 'next' ? nextIndex(searchState.activeIndex, n) : prevIndex(searchState.activeIndex, n));
            });
        });
        return { destroy: bag.dispose };
    }

    /** Title page picker: "go to page" input plus a labelled listbox per page. */

    /**
     * Builds the title page picker: a "go to page" input plus a listbox with one
     * option per page, labelled with the manifest page labels. Registers its
     * Escape handling on the key dispatcher (priority 30: under the modal and the
     * grid, above the panels).
     *
     * @param {IvViewer} viewer
     * @param {string[]} labels page labels, index-aligned with the page orders
     * @param {{register: Function}} keys  the immersive key dispatcher
     * @returns {{destroy: function():void}}
     */
    function setupPageDropdown(viewer, labels, keys) {
        const total = viewer.getPageCount();
        const trigger = document.querySelector('[data-immersive-title-trigger]');
        const dropdown = document.getElementById('immersivePageDropdown');
        const list = document.getElementById('immersivePageList');
        const input = document.getElementById('immersivePageInput');
        if (!trigger || !dropdown || !list) return noop;
        if (total < 2) {
            trigger.classList.add('immersive__title-trigger-static');
            trigger.removeAttribute('aria-haspopup');
            trigger.removeAttribute('aria-expanded');
            trigger.removeAttribute('aria-controls');
            return noop;
        }
        const bag = createDisposer();
        if (input) input.max = String(total);
        const listLabel = input && input.labels && input.labels[0] ? input.labels[0].textContent.trim() : '';
        if (listLabel) list.setAttribute('aria-label', listLabel);

        const items = [];
        for (let order = 0; order < total; order++) {
            const li = document.createElement('li');
            li.setAttribute('role', 'presentation');
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'immersive__page-dropdown-item';
            btn.dataset.order = String(order);
            btn.setAttribute('role', 'option');
            btn.setAttribute('aria-selected', 'false');
            btn.tabIndex = -1;
            const num = document.createElement('span');
            num.className = 'immersive__page-dropdown-number';
            num.textContent = `${order + 1}:`;
            const lbl = document.createElement('span');
            lbl.className = 'immersive__page-dropdown-item-label';
            lbl.textContent = (labels[order] || '').trim();
            btn.append(num, lbl);
            li.append(btn);
            list.append(li);
            items.push(btn);
        }

        const markActive = () => {
            const current = viewer.getCurrentPages();
            let rovingSet = false;
            items.forEach((btn) => {
                const on = current.includes(Number(btn.dataset.order));
                btn.classList.toggle('-active', on);
                btn.setAttribute('aria-selected', on ? 'true' : 'false');
                btn.tabIndex = on && !rovingSet ? 0 : -1;
                if (on) rovingSet = true;
            });
            if (!rovingSet && items.length) items[0].tabIndex = 0;
        };

        const focusOption = (idx) => {
            if (idx < 0) idx = 0;
            if (idx > items.length - 1) idx = items.length - 1;
            const target = items[idx];
            if (!target) return;
            items.forEach((btn) => (btn.tabIndex = btn === target ? 0 : -1));
            target.focus();
        };

        const isOpen = () => !dropdown.hidden;
        const onOutside = (e) => {
            if (!dropdown.contains(e.target) && !trigger.contains(e.target)) close();
        };
        const close = () => {
            if (!isOpen()) return;
            dropdown.hidden = true;
            trigger.setAttribute('aria-expanded', 'false');
            document.removeEventListener('pointerdown', onOutside, true);
        };
        const open = () => {
            if (isOpen()) return;
            markActive();
            dropdown.hidden = false;
            trigger.setAttribute('aria-expanded', 'true');
            document.addEventListener('pointerdown', onOutside, true);
            const active = list.querySelector('.-active');
            if (active) {
                list.scrollTop = Math.max(0, active.offsetTop - list.offsetTop - (list.clientHeight - active.clientHeight) / 2);
            }
            if (input) {
                input.value = '';
                input.focus({ preventScroll: true });
            }
        };

        bag.listen(trigger, 'click', () => (isOpen() ? close() : open()));
        bag.listen(list, 'click', (e) => {
            const btn = e.target.closest('.immersive__page-dropdown-item');
            if (!btn) return;
            viewer.goToPage(Number(btn.dataset.order));
            close();
        });
        bag.listen(list, 'keydown', (e) => {
            const cur = items.indexOf(document.activeElement);
            if (cur === -1) return;
            switch (e.key) {
                case 'ArrowDown':
                    e.preventDefault();
                    focusOption(cur + 1);
                    break;
                case 'ArrowUp':
                    e.preventDefault();
                    focusOption(cur - 1);
                    break;
                case 'Home':
                    e.preventDefault();
                    focusOption(0);
                    break;
                case 'End':
                    e.preventDefault();
                    focusOption(items.length - 1);
                    break;
                case 'Enter':
                case ' ':
                case 'Spacebar':
                    e.preventDefault();
                    viewer.goToPage(Number(items[cur].dataset.order));
                    close();
                    trigger.focus();
                    break;
            }
        });
        if (input) {
            bag.listen(input, 'keydown', (e) => {
                if (e.key !== 'Enter') return;
                e.preventDefault();
                const n = parseInt(input.value, 10);
                if (Number.isFinite(n) && n >= 1 && n <= total) {
                    viewer.goToPage(n - 1);
                    close();
                }
            });
        }
        bag.add(
            keys.register(KEY_PRIORITY.pageDropdown, (e) => {
                if (e.key !== 'Escape' || !isOpen()) return false;
                close();
                trigger.focus();
                return true;
            })
        );
        bag.add(viewer.onPageChange.subscribe(markActive));
        markActive();
        return {
            destroy() {
                close();
                bag.dispose();
            },
        };
    }

    /** Thumbnail overview overlay: lazy riot grid with modal focus handling. */

    /**
     * Mounts the thumbnail grid overlay lazily (riot `thumbnails` tag) with modal
     * focus handling and keeps its selection on the current page. Also wires the
     * thumbnail-size slider (persisted via localStorage) and a back-to-top button
     * for the grid scroll container. Escape closes via the key dispatcher
     * (priority 20: under the shortcuts modal, above dropdown and panels).
     *
     * @param {IvViewer} viewer
     * @param {string} pi
     * @param {string} apiBase
     * @param {{register: Function}} keys  the immersive key dispatcher
     * @returns {{toggleGrid: Function, destroy: Function}}
     */
    function setupOverviewGrid(viewer, pi, apiBase, keys) {
        const bag = createDisposer();
        const GRID_LOADER_TIMEOUT_MS = 8000;
        const GRID_TOP_VISIBLE_AFTER_PX = 200;
        const GRID_THUMB_IMAGE_SIZE = '!400,560';
        const gridOverlay = document.getElementById('immersiveGridOverlay');
        const gridLoader = document.getElementById('immersiveGridLoader');
        const gridClose = gridOverlay && gridOverlay.querySelector('.immersive__grid-close');
        const gridTrigger = document.querySelector('[data-immersive-action="overview"]:not(.immersive__grid-close)');
        const gridScroll = gridOverlay && gridOverlay.querySelector('.immersive__grid-scroll');
        const gridTopBtn = gridOverlay && gridOverlay.querySelector('.immersive__grid-top');
        const gridSizeSlider = document.getElementById('immersiveGridSize');
        const gridSizeLabels = gridSizeSlider ? [gridSizeSlider.dataset.labelSmall, gridSizeSlider.dataset.labelMedium, gridSizeSlider.dataset.labelLarge] : [];
        const applyGridThumbSize = (step) => {
            if (gridOverlay) gridOverlay.setAttribute('data-thumb-size', String(step));
            if (gridSizeSlider) {
                gridSizeSlider.value = String(step);
                gridSizeSlider.style.setProperty('--immersive-slider-fill', `${(step / THUMB_SIZE_MAX) * 100}%`);
                if (gridSizeLabels[step]) {
                    gridSizeSlider.setAttribute('aria-valuetext', gridSizeLabels[step]);
                } else {
                    gridSizeSlider.removeAttribute('aria-valuetext');
                }
            }
        };
        applyGridThumbSize(readThumbSizeStep(localStore()));
        if (gridSizeSlider) {
            bag.listen(gridSizeSlider, 'input', () => {
                const step = normalizeThumbSizeStep(gridSizeSlider.value);
                applyGridThumbSize(step);
                writeThumbSizeStep(localStore(), step);
            });
        }
        if (gridScroll && gridTopBtn) {
            bag.listen(gridTopBtn, 'click', () => {
                const behavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth';
                gridScroll.scrollTo({ top: 0, behavior });
            });
            bag.listen(
                gridScroll,
                'scroll',
                () => {
                    const hide = gridScroll.scrollTop < GRID_TOP_VISIBLE_AFTER_PX;
                    if (hide && document.activeElement === gridTopBtn && gridClose) gridClose.focus();
                    if (gridTopBtn.hidden !== hide) gridTopBtn.hidden = hide;
                },
                { passive: true }
            );
        }
        let gridMounted = false;
        let gridLoaderTimer = null;
        bag.add(() => clearTimeout(gridLoaderTimer));
        let gridTag = null;
        let gridOpener = null;
        let gridTrapHandler = null;
        const gridActions = new env.rxjs.Subject();
        const gridActionsSub = gridActions.subscribe((e) => {
            if (e && e.action === 'clickImage' && typeof e.value === 'number') {
                viewer.goToPage(e.value);
                closeGrid();
            }
        });
        bag.add(() => gridActionsSub.unsubscribe());
        const syncGridSelection = () => {
            if (gridTag) {
                gridTag.opts.index = viewer.getCurrentOrder();
                gridTag.update();
            }
        };
        bag.add(
            viewer.onPageChange.subscribe(() => {
                if (gridOverlay && !gridOverlay.hidden) syncGridSelection();
            })
        );
        const openGrid = () => {
            gridOpener = gridTrigger || document.activeElement;
            gridOverlay.hidden = false;
            gridOverlay.setAttribute('role', 'dialog');
            gridOverlay.setAttribute('aria-modal', 'true');
            const gridLabel = gridTrigger && gridTrigger.getAttribute('aria-label');
            if (gridLabel) gridOverlay.setAttribute('aria-label', gridLabel);
            gridTrapHandler = createFocusTrap(gridOverlay);
            gridOverlay.addEventListener('keydown', gridTrapHandler);
            if (!gridMounted) {
                gridTag = env.riot.mount('#immersiveThumbnails', 'thumbnails', {
                    source: `${apiBase}/records/${pi}/manifest`,
                    type: 'sequence',
                    actionlistener: gridActions,
                    imagesize: GRID_THUMB_IMAGE_SIZE,
                    index: viewer.getCurrentOrder(),
                })[0];
                gridMounted = true;
                let gridLoaderDone = false;
                const hideGridLoader = () => {
                    if (gridLoaderDone) return;
                    gridLoaderDone = true;
                    clearTimeout(gridLoaderTimer);
                    if (gridLoader) gridLoader.hidden = true;
                };
                const thumbsMount = document.getElementById('immersiveThumbnails');
                if (thumbsMount) {
                    bag.listen(thumbsMount, 'load', hideGridLoader, { capture: true, once: true });
                    const templateImg = document.querySelector('#immersiveDeniedContent .immersive__page-denied-img');
                    const deniedThumb = (templateImg && templateImg.getAttribute('src')) || `${apiBase.replace(/\/api\/v1\/?$/, '')}/resources/images/access_denied.png`;
                    bag.listen(
                        thumbsMount,
                        'error',
                        (e) => {
                            const t = e.target;
                            if (t && t.tagName === 'IMG' && !t.dataset.ivDenied) {
                                t.dataset.ivDenied = '1';
                                t.classList.add('immersive__grid-thumb--denied');
                                t.src = deniedThumb;
                            }
                        },
                        { capture: true }
                    );
                }
                gridLoaderTimer = setTimeout(hideGridLoader, GRID_LOADER_TIMEOUT_MS);
            } else {
                syncGridSelection();
            }
            if (gridTrigger) gridTrigger.setAttribute('aria-expanded', 'true');
            if (gridClose) gridClose.focus();
        };
        const closeGrid = () => {
            if (!gridOverlay || gridOverlay.hidden) return;
            gridOverlay.hidden = true;
            if (gridTrigger) gridTrigger.setAttribute('aria-expanded', 'false');
            if (gridTrapHandler) {
                gridOverlay.removeEventListener('keydown', gridTrapHandler);
                gridTrapHandler = null;
            }
            const restore = gridOpener || gridTrigger;
            gridOpener = null;
            if (restore && typeof restore.focus === 'function') restore.focus();
        };
        const toggleGrid = () => {
            if (!gridOverlay) return;
            if (gridOverlay.hidden) openGrid();
            else closeGrid();
        };

        bag.add(
            keys.register(KEY_PRIORITY.gridOverlay, (e) => {
                if (e.key !== 'Escape' || !gridOverlay || gridOverlay.hidden) return false;
                closeGrid();
                return true;
            })
        );
        return {
            toggleGrid,
            destroy() {
                closeGrid();
                if (gridTag && typeof gridTag.unmount === 'function') gridTag.unmount(true);
                gridTag = null;
                gridMounted = false;
                bag.dispose();
            },
        };
    }

    /** Bottom bar and page-status wiring: indicator and actions. */

    /**
     * Wires the paging UI and the bottom-bar actions: page indicator (bottom bar +
     * title pill + canvas aria-label), the action buttons
     * (zoom, rotate, reset, double page, overview, fullscreen) and the
     * fullscreen-change bookkeeping incl. re-homing Bootstrap popovers into the
     * fullscreen element.
     *
     * @param {HTMLElement} el  the [data-immersive-image] mount element
     * @param {IvViewer} viewer
     * @param {object} hooks
     * @param {Function} hooks.toggleGrid  toggles the thumbnail overview overlay
     * @param {Function} hooks.updateFulltextAvail  fulltext availability follows double-page mode
     * @returns {{destroy: function():void}}
     */
    function setupBottomBar(el, viewer, { toggleGrid, updateFulltextAvail }) {
        const bag = createDisposer();
        const indicator = document.getElementById('immersivePageIndicator');
        const titlePage = document.getElementById('immersiveTitlePage');
        const total = viewer.getPageCount();
        const workTitle = (document.querySelector('.immersive__title-text')?.textContent || '').trim();
        const updateIndicator = () => {
            const pages = viewer.getCurrentPages().map((p) => p + 1);
            const label = pages.length > 1 ? `${pages[0]}–${pages[pages.length - 1]}` : `${pages[0]}`;
            if (indicator) indicator.textContent = `${label} / ${total}`;
            if (titlePage) titlePage.textContent = `(${label} / ${total})`;
            const imageSurface = el.querySelector('.openseadragon-canvas') || el;
            imageSurface.setAttribute('role', 'img');
            imageSurface.setAttribute('aria-label', workTitle ? `${workTitle}, ${label} / ${total}` : `${label} / ${total}`);
        };
        updateIndicator();
        bag.add(viewer.onPageChange.subscribe(() => updateIndicator()));

        document.querySelectorAll('[data-immersive-page]').forEach((btn) => {
            bag.listen(btn, 'click', () => (btn.dataset.immersivePage === 'next' ? viewer.next() : viewer.prev()));
        });
        document.querySelectorAll('[data-immersive-action]').forEach((btn) => {
            bag.listen(btn, 'click', () => {
                const action = btn.dataset.immersiveAction;
                if (action === 'zoom-in') viewer.zoomIn();
                else if (action === 'zoom-out') viewer.zoomOut();
                else if (action === 'rotate-left') viewer.rotateLeft();
                else if (action === 'rotate-right') viewer.rotateRight();
                else if (action === 'reset') viewer.resetView();
                else if (action === 'fullscreen') toggleImmersiveFullscreen();
                else if (action === 'overview') toggleGrid();
                else if (action === 'double-page') {
                    const on = viewer.toggleDoublePage();
                    btn.setAttribute('aria-pressed', String(on));
                    btn.classList.toggle('-active', on);
                    document.querySelector('.immersive__viewer')?.classList.toggle('is-double-page', on);
                    updateFulltextAvail();
                }
            });
        });

        const fullscreenBtn = document.querySelector('[data-immersive-action="fullscreen"]');
        let resetTimer = null;
        let resetFrame = 0;
        let cancelResize = null;
        bag.add(() => {
            clearTimeout(resetTimer);
            cancelAnimationFrame(resetFrame);
            if (cancelResize) cancelResize();
        });
        bag.listen(document, 'fullscreenchange', () => {
            let done = false;
            const doReset = () => {
                if (done) return;
                done = true;
                clearTimeout(resetTimer);
                if (cancelResize) cancelResize();
                cancelResize = null;
                resetFrame = requestAnimationFrame(() => viewer.resetView());
            };
            if (cancelResize) cancelResize();
            cancelResize = viewer.onceResized(doReset);
            clearTimeout(resetTimer);
            resetTimer = setTimeout(doReset, 300);
            if (fullscreenBtn) {
                const isFullscreen = !!document.fullscreenElement;
                fullscreenBtn.setAttribute('aria-pressed', String(isFullscreen));
                const exitLabel = fullscreenBtn.dataset.labelExit;
                const enterLabel = fullscreenBtn.dataset.labelEnter || fullscreenBtn.getAttribute('aria-label');
                const label = isFullscreen && exitLabel ? exitLabel : enterLabel;
                if (label) {
                    fullscreenBtn.setAttribute('aria-label', label);
                    fullscreenBtn.setAttribute('title', label);
                }
                const iconUse = fullscreenBtn.querySelector('svg use');
                const enterIcon = fullscreenBtn.dataset.iconEnter;
                const exitIcon = fullscreenBtn.dataset.iconExit;
                if (iconUse && enterIcon && exitIcon) {
                    const iconName = isFullscreen ? exitIcon : enterIcon;
                    const href = iconUse.getAttribute('href') || iconUse.getAttribute('xlink:href') || '';
                    iconUse.setAttribute('href', href.replace(/[^/]+\.svg/, `${iconName}.svg`));
                }
            }

            document.querySelectorAll('[data-popover-element]').forEach((trigger) => {
                const $trigger = env.$ && env.$(trigger);
                const inst = $trigger && $trigger.data('bs.popover');
                if (!inst || !inst.config) return;
                $trigger.popover('hide');
                if (document.fullscreenElement) {
                    if (inst.config._savedContainer === undefined) {
                        inst.config._savedContainer = inst.config.container;
                    }
                    inst.config.container = document.fullscreenElement;
                } else if (inst.config._savedContainer !== undefined) {
                    inst.config.container = inst.config._savedContainer;
                    delete inst.config._savedContainer;
                }
            });
        });
        return { destroy: bag.dispose };
    }

    /** Toggles native browser fullscreen on the immersive viewer hero (Esc exits). */
    function toggleImmersiveFullscreen() {
        const el = document.querySelector('.immersive__viewer');
        if (!el) return;
        if (document.fullscreenElement) {
            document.exitFullscreen();
        } else {
            el.requestFullscreen?.();
        }
    }

    /** Image-filter popover wiring plus a11y for the immersive Bootstrap popovers. */

    const FILTER_TRIGGER_SELECTOR = '[data-popover-element="#immersiveFilterPopover"]';

    /**
     * Whether the viewer's canvas is origin-clean (CORS): pixel filters would throw on a
     * tainted canvas. Checked both up front and again at mount time, because the taint
     * status can change once tiles have actually loaded.
     */
    function isViewerOriginClean(viewer) {
        const image = viewer.viewer;
        return typeof image.isOriginClean !== 'function' || image.isOriginClean();
    }

    /**
     * Defers the imageFilters mount until the Filter popover is first shown: Bootstrap only
     * portals #immersiveFilterPopover (and its <imageFilters> child) into the DOM on show,
     * so mounting on load would find nothing and the popover would open empty. After the
     * mount the popover is repositioned, because riot fills it only after Popper has
     * already measured the still-empty shell.
     *
     * The origin-clean check runs at open time (the canvas only taints once tiles have
     * been drawn); a persistent handler with a mounted flag replaces one(), so an open
     * where the mount cannot happen yet does not burn the only mount attempt.
     *
     * @returns {{destroy: function():void}}
     */
    function setupImageFilters(viewer) {
        const btn = document.querySelector(FILTER_TRIGGER_SELECTOR);
        if (!btn) return noop;
        const bag = createDisposer();
        const $ = env.$;
        if (!$) {
            mountImageFilters(viewer, bag);
            return { destroy: bag.dispose };
        }
        let mounted = false;
        $(btn).on('shown.bs.popover.immersiveFilters', () => {
            if (mounted) return;
            if (!isViewerOriginClean(viewer)) {
                $(btn).popover('hide');
                btn.hidden = true;
                return;
            }
            mounted = mountImageFilters(viewer, bag);
            if (mounted) $(btn).popover('update');
        });
        bag.add(() => $(btn).off('shown.bs.popover.immersiveFilters'));
        return { destroy: bag.dispose };
    }

    /**
     * Adds focus/keyboard management to the three immersive Bootstrap popovers (Share / Cite /
     * Filter) without touching the shared popovers controller. On open, focus moves into the
     * portaled popover; Escape inside it closes and returns focus to the trigger; aria-expanded
     * is kept in sync. Hooks only these triggers via Bootstrap's shown/hidden.bs.popover events.
     *
     * @returns {{destroy: function():void}}
     */
    function setupImmersivePopoverA11y() {
        const $ = env.$;
        if (!$) return noop;
        const bag = createDisposer();
        const selectors = ['#immersiveSharePopover', '#immersiveCitationPopover', '#immersiveFilterPopover'];
        selectors.forEach((sel) => {
            const trigger = document.querySelector(`[data-popover-element="${sel}"]`);
            if (!trigger || trigger.dataset.a11yBound === 'true') return;
            trigger.dataset.a11yBound = 'true';

            const popoverEl = () => {
                const id = trigger.getAttribute('aria-describedby');
                const byId = id && document.getElementById(id);
                if (byId) return byId;
                const all = Array.from(document.querySelectorAll('.popover')).filter((p) => p.offsetParent !== null);
                return all.length ? all[all.length - 1] : null;
            };

            $(trigger).on('shown.bs.popover.ivA11y', () => {
                trigger.setAttribute('aria-expanded', 'true');
                const pop = popoverEl();
                if (!pop) return;
                pop.setAttribute('role', 'dialog');
                const name = trigger.getAttribute('aria-label') || trigger.getAttribute('title');
                if (name) pop.setAttribute('aria-label', name);
                if (!pop.dataset.ivEscBound) {
                    pop.dataset.ivEscBound = '1';
                    pop.addEventListener('keydown', (e) => {
                        if (e.key === 'Escape') {
                            e.preventDefault();
                            $(trigger).popover('hide');
                            trigger.focus();
                        }
                    });
                }
                const first = pop.querySelector(FOCUSABLE_SELECTOR);
                if (first) {
                    const $first = $(first);
                    const hasTip = typeof $first.tooltip === 'function' && !!$first.data('bs.tooltip');
                    if (hasTip) $first.tooltip('disable');
                    first.focus();
                    if (hasTip) $first.tooltip('enable');
                } else {
                    if (!pop.hasAttribute('tabindex')) pop.setAttribute('tabindex', '-1');
                    pop.focus();
                }
            });

            $(trigger).on('hidden.bs.popover.ivA11y', () => {
                trigger.setAttribute('aria-expanded', 'false');
                const active = document.activeElement;
                if (!active || active === document.body) trigger.focus();
            });
            bag.add(() => {
                $(trigger).off('.ivA11y');
                delete trigger.dataset.a11yBound;
            });
        });
        return { destroy: bag.dispose };
    }

    /**
     * Builds the riot imageFilters config from the translated labels the page puts on the
     * <imageFilters> element (a missing label keeps the tag's default); blur stays hidden.
     *
     * @param {DOMStringMap} dataset
     * @returns {{filters: object, messages: {clearAll?: string}}}
     */
    function filterConfigFrom(dataset) {
        const filter = (label) => (label ? { label } : {});
        return {
            filters: {
                brightness: filter(dataset.labelBrightness),
                contrast: filter(dataset.labelContrast),
                saturate: filter(dataset.labelSaturate),
                grayscale: filter(dataset.labelGrayscale),
                threshold: filter(dataset.labelBitonal),
                blur: { ...filter(dataset.labelBlur), visible: false },
                sharpen: filter(dataset.labelSharpen),
                invert: filter(dataset.labelInvert),
                hue: filter(dataset.labelRotate),
            },
            messages: dataset.labelClearAll ? { clearAll: dataset.labelClearAll } : {},
        };
    }

    /**
     * Mounts the reused imageFilters riot tag on the viewer's live ImageView.Image so the
     * Filter popover adjusts brightness/contrast/etc.
     *
     * @param {{add: Function}} bag  the caller's disposer; the tag's subscriptions are queued on it
     * @returns {boolean} whether the tag was mounted
     */
    function mountImageFilters(viewer, bag) {
        const mount = document.querySelector('imageFilters');
        if (!mount) return false;
        const [tag] = env.riot.mount('imageFilters', { image: viewer.viewer, config: filterConfigFrom(mount.dataset) });
        if (tag) {
            bag.add(() => {
                if (typeof tag.unmount === 'function') tag.unmount(true);
            });
            bag.add(bindImageFiltersRendering(viewer, tag));
            bag.add(viewer.onReset.subscribe(() => tag.resetAll()));
        }
        return true;
    }

    /**
     * Replaces the library's per-filter event subscriptions with one direct OSD
     * handler that applies every active filter exactly once per drawn frame.
     *
     * The library re-runs its observable wiring on every load() (each double-page
     * toggle) and stacks another 'update-viewport' forwarder onto the SAME
     * OpenSeadragon instance without ever tearing the old ones down, so a filter
     * subscribed through that chain runs N+1 times per frame after N reopens and
     * compounds its own output onto the already-filtered canvas (contrast^N).
     * Applying the filter methods ourselves from a single handler sidesteps the
     * chain entirely; the tag's start/close/isActive/apply are rebased onto a
     * plain active-set so its UI logic (checkboxes, precludes, reset) keeps working.
     *
     * @returns {function():void} detaches the frame handler and the onOpen subscription
     */
    function bindImageFiltersRendering(viewer, tag) {
        const redraw = () => viewer.redraw();
        const active = new Set();
        (tag.filters || []).forEach((filter) => {
            filter.start = () => {
                active.add(filter);
                redraw();
            };
            filter.close = () => {
                active.delete(filter);
                redraw();
            };
            filter.isActive = () => active.has(filter);
            filter.apply = redraw;
        });
        const renderFilters = () => {
            if (!active.size) return;
            const context = viewer.getCanvasContext();
            if (!context) return;
            let data = context.getImageData(0, 0, context.canvas.width, context.canvas.height);
            active.forEach((filter) => {
                data = filter.filterMethod(data) || data;
            });
            context.putImageData(data, 0, 0);
        };
        return viewer.onFrame(renderFilters);
    }

    /** Immersive view bootstrap: engine construction plus the UI wiring modules. */

    /** Hides the stage loading indicator at the latest after this, even without a painted tile. */
    const STAGE_LOADER_TIMEOUT_MS = 8000;

    /** Fills the stage with the access-denied styled placeholder, worded for a failed load. */
    function showStageError(el) {
        const text = el.dataset.msgError;
        if (!text) return;
        if (getComputedStyle(el).position === 'static') el.style.position = 'relative';
        el.appendChild(buildDeniedPlaceholder({ text, role: 'alert' }));
    }

    /**
     * Bootstraps the immersive view. The chrome (panels, keyboard dispatcher,
     * shortcuts modal, metadata fold) is wired whenever the view is present, even
     * without VIEW_IMAGES. The image engine and its dependent wiring (bottom bar,
     * grid, fulltext, search, layout tags, page dropdown, TOC sync) are only built when the
     * server rendered the [data-immersive-image] mount, i.e. the user may view the
     * images. Keyboard shortcuts run through one dispatcher; KEY_PRIORITY in
     * shared/ivKeys defines who wins a key. Every setup module returns a destroyable; the
     * returned handle tears all of them down along with the engine.
     *
     * @param {HTMLElement} immersiveRoot  the .immersive root element
     * @returns {{destroy: function():void}}
     */
    function initImmersiveViewer(immersiveRoot) {
        const parts = [];
        const use = (part) => {
            parts.push(part);
            return part;
        };
        let viewer = null;
        let destroyed = false;
        let stageLoaderTimer = null;
        /** Tears down the engine and everything wired on top of it; the chrome stays. */
        const disposeEngine = () => {
            while (parts.length > chromeParts) parts.pop().destroy();
            if (viewer) viewer.destroy();
            viewer = null;
        };
        const handle = {
            destroy() {
                destroyed = true;
                clearTimeout(stageLoaderTimer);
                disposeEngine();
                while (parts.length) parts.pop().destroy();
                keys.detach();
            },
        };

        const keys = createKeyDispatcher();
        keys.attach(document);
        const panels = use(setupPanels(immersiveRoot, keys));
        use(setupPanelResize(immersiveRoot));
        use(setupMetadataToggle());
        use(setupImmersivePopoverA11y());
        use(setupShortcutsModal(panels.closePanels, keys));
        const chromeParts = parts.length;

        const el = immersiveRoot.querySelector('[data-immersive-image]');
        if (!el) return handle;

        const pi = el.dataset.pi;
        const apiBase = el.dataset.apiBase;
        const startOrder = Number(el.dataset.startOrder) || 0;
        const maxZoom = el.dataset.maxZoom ? parseInt(el.dataset.maxZoom, 10) : undefined;
        const allowZoom = el.dataset.allowZoom !== 'false';

        const stageLoader = document.getElementById('immersiveStageLoader');
        let stageLoaderDone = false;
        const hideStageLoader = () => {
            if (stageLoaderDone) return;
            stageLoaderDone = true;
            if (stageLoader) stageLoader.hidden = true;
        };
        stageLoaderTimer = setTimeout(hideStageLoader, STAGE_LOADER_TIMEOUT_MS);

        const deniedText = el.dataset.msgDenied || '';

        Promise.all([loadPageServices(pi, apiBase), loadPageAccess(pi, apiBase)])
            .then(([services, restricted]) => {
                if (destroyed) return;
                if (!services.length) throw new Error(`immersive: record ${pi} has no pages`);
                viewer = new IvViewer({
                    element: el,
                    services,
                    startOrder,
                    maxZoom,
                    allowZoom,
                    restricted,
                    deniedText,
                    deniedContent: document.getElementById('immersiveDeniedContent'),
                });
                viewer.onceTileLoaded(hideStageLoader);
                viewer.onOpen.subscribe(hideStageLoader);
                use(attachUrlSync(viewer, pi));
                use(setupImageFilters(viewer));
                use(registerViewerNavKeys(keys, viewer, el));

                loadPageLabels(pi, apiBase, fetch, document.documentElement.lang)
                    .then((labels) => {
                        if (viewer) use(setupPageDropdown(viewer, labels, keys));
                    })
                    .catch((e) => console.warn('immersive page labels failed', e));

                use(setupToc(viewer));
                use(setupSearchPanel(viewer, pi, apiBase));
                const fulltext = use(setupFulltextPanel(viewer, pi, apiBase, panels, keys));
                use(setupLayoutTagsPanel(viewer, pi, apiBase, panels, keys));
                const grid = use(setupOverviewGrid(viewer, pi, apiBase, keys));
                use(setupBottomBar(el, viewer, { toggleGrid: grid.toggleGrid, updateFulltextAvail: fulltext.updateFulltextAvail }));
                panels.replayOpen();
            })
            .catch((e) => {
                console.error('immersive viewer init failed', e);
                hideStageLoader();
                if (destroyed) return;
                disposeEngine();
                showStageError(el);
            });
        return handle;
    }

    window.ShareImageFragment = ShareImageFragment;

    window.zoomableImageLoaded = new rxjs.Subject();

    document.addEventListener('DOMContentLoaded', () => {
        if (document.querySelector('[data-image="zoomable"]')) {
            window.image = new ZoomableImage();
            window.image
                .load()
                .then((image) => {
                    window.zoomableImageLoaded.next(image);
                })
                .catch((e) => {
                    window.zoomableImageLoaded.error(e);
                });
        }

        window.voyager3dView = new Voyager3dView();

        // Wire the immersive chrome whenever the view is present; the image viewer
        // itself is only built when the user has VIEW_IMAGES (see ivInit).
        const immersiveRoot = document.querySelector('.immersive');
        if (immersiveRoot) {
            initImmersiveViewer(immersiveRoot);
        }
    });
})();
