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
            },
        },
        datasets: {
            image: {
                viewMode: 'imageViewMode',
                showNavigator: 'imageShowNavigator',
                allowDownload: 'allowDownload',
                allowZoom: 'allowZoom',
                maxZoom: 'maxZoom',
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
            const explorer = `<voyager-explorer id='voyager' bgcolor='#015999 #000' bgstyle='LinearGradient' style='display:none;' document='${this.getSceneUrl()}' uiMode='menu' resourceRoot='${this.resourcePath}'></voyager-explorer>`;

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
            const story = `<voyager-story id='voyagerStory' bgcolor='#015999 #000' bgstyle='LinearGradient' style='display:none;' document='${this.getSceneUrl()}'></voyager-story>`;

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
     * Pure helper functions for the immersive image viewer's tile-source window.
     *
     * No DOM, no network, no OpenSeadragon dependencies — safe to unit-test and
     * to import in any context.
     */

    /**
     * Resolve a service object or array to a single id string.
     * Returns null when no id can be found.
     *
     * @param {object|Array} service
     * @param {'@id'|'id'} primaryKey   - preferred property name
     * @param {'@id'|'id'} fallbackKey  - secondary property name
     * @returns {string|null}
     */
    function resolveServiceId(service, primaryKey, fallbackKey) {
        if (!service) return null;
        const entry = Array.isArray(service) ? service[0] : service;
        if (!entry) return null;
        const id = entry[primaryKey] || entry[fallbackKey];
        return typeof id === 'string' && id.length > 0 ? id : null;
    }

    /**
     * Extracts the ordered list of IIIF image-service base IDs from a manifest.
     *
     * Supports IIIF Presentation API v2 (sequences/canvases) and v3 (items).
     * Canvases that lack a resolvable service id are silently skipped.
     *
     * @param {object} manifest - Parsed IIIF Presentation manifest.
     * @returns {string[]} Ordered array of image-service id strings.
     */
    function parseManifestImageServices(manifest) {
        if (!manifest || typeof manifest !== 'object') return [];

        // IIIF v2: manifest.sequences[0].canvases
        if (Array.isArray(manifest.sequences) && manifest.sequences.length > 0) {
            const canvases = manifest.sequences[0].canvases;
            if (!Array.isArray(canvases)) return [];

            const ids = [];
            for (const canvas of canvases) {
                try {
                    const service = canvas.images[0].resource.service;
                    const id = resolveServiceId(service, '@id', 'id');
                    if (id !== null) ids.push(id);
                } catch {
                    // canvas structure incomplete — skip
                }
            }
            return ids;
        }

        // IIIF v3: manifest.items (canvases)
        if (Array.isArray(manifest.items) && manifest.items.length > 0) {
            const ids = [];
            for (const canvas of manifest.items) {
                try {
                    const service = canvas.items[0].items[0].body.service;
                    const id = resolveServiceId(service, 'id', '@id');
                    if (id !== null) ids.push(id);
                } catch {
                    // canvas structure incomplete — skip
                }
            }
            return ids;
        }

        return [];
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
    }

    /** Maps a IIIF image-service base id to an OSD tile source (its info.json URL). */
    function toTileSource(serviceId) {
        return serviceId.endsWith('/info.json') ? serviceId : `${serviceId}/info.json`;
    }

    // Sequence config mirrors the proven single-image path in zoomableImage.mjs
    // (getSequenceSettings('single')). _arrangeImageSequence reads it on every open,
    // so it must be present even though we load one image at a time.
    const _sequence = { columns: 1, useWindowing: true, windowSize: 100, windowExpandThreshold: 10, windowExpandSize: 50 };
    const PREFETCH_RADIUS = 1;

    /**
     * Immersive image viewer engine. Wraps a single live ImageView.Image instance
     * and swaps the displayed page IN-PLACE on navigation (no JSF page reload),
     * reusing OSD's proven single-image contain-fit. Neighbours are prefetched so
     * the next swap is fast. Emits page changes; knows nothing about buttons/URLs/overlays.
     *
     * NOTE: a windowed multi-image variant (load a sliding window once, jump in place
     * via setCurrentImage + goHome) was prototyped to also remove the brief tile load
     * between pages. It was reverted: ImageView's 'fixed' (contain-fit) mode is built
     * for a single image, and its goHome fit math (ZoomControls 'fixed' branch handles
     * only bottom/right margins) over-zooms non-first images in the stacked multi-image
     * world, breaking the fit. The pure window math (`resolveNavigation`) is kept and
     * tested for a future iteration that adds custom per-page fitBounds with margin insets.
     */
    class IvViewer {
        /**
         * @param {object} opts
         * @param {HTMLElement} opts.element   OSD mount element
         * @param {string[]} opts.services     ordered IIIF image-service URLs (all pages)
         * @param {number} [opts.startOrder=0] initial 0-based page index
         * @param {number} [opts.maxZoom]
         */
        constructor(opts) {
            this.services = opts.services;
            this.total = opts.services.length;
            this.current = Math.max(0, Math.min(opts.startOrder ?? 0, this.total - 1));
            this.onPageChange = new Emitter();
            this.onLoaded = new Emitter();

            this.viewer = new ImageView.Image({
                element: opts.element,
                // 'fixed' = fit the whole image into the (full-bleed) viewport, like the
                // fullscreen view. 'toWidth' would grow the canvas height and blow the
                // image up in this wide container.
                fittingMode: 'fixed',
                // Inset the image so it never sits under the overlay chrome: the page is
                // centered on the dark stage with margins for the title (top), bottom bar,
                // and the left/right icon rails - this is what makes it read as a framed
                // viewer area rather than an image bleeding to the edges.
                margins: { top: 64, bottom: 72, left: 64, right: 64 },
                zoom: { enabled: true, max: opts.maxZoom },
                sequence: _sequence,
                navigator: { enabled: false },
            });
            this.zoom = new ImageView.Controls.Zoom(this.viewer);
            this.rotation = new ImageView.Controls.Rotation(this.viewer);

            this._open(this.current).then(() => this.onLoaded.emit(this.current));
        }

        getCurrentOrder() {
            return this.current;
        }
        getPageCount() {
            return this.total;
        }
        next() {
            this.goToPage(this.current + 1);
        }
        prev() {
            this.goToPage(this.current - 1);
        }

        // --- image controls (delegated to the OSD wrapper) ---
        zoomIn() {
            this.zoom.zoomBy(1.5);
        }
        zoomOut() {
            this.zoom.zoomBy(1 / 1.5);
        }
        rotateLeft() {
            this.rotation.rotateLeft();
        }
        rotateRight() {
            this.rotation.rotateRight();
        }
        resetView() {
            this.rotation.rotateTo(0);
            this.zoom.goHome();
        }

        /**
         * Navigate to a page. The viewer instance stays alive, so this is an
         * in-place image swap (no page reload).
         * @param {number} order 0-based page index
         */
        goToPage(order) {
            const target = Math.max(0, Math.min(order, this.total - 1));
            if (target === this.current) return;
            this.current = target;
            this._open(target);
        }

        /** Loads a single page and fits it; prefetches neighbours. */
        _open(order) {
            const loaded = this.viewer.load([toTileSource(this.services[order])], 0);
            this._prefetchAround(order);
            return loaded.then(() => this._emit());
        }

        /** Warms neighbour info.json in the browser cache so the next swap is fast. */
        _prefetchAround(order) {
            for (let d = 1; d <= PREFETCH_RADIUS; d++) {
                for (const o of [order - d, order + d]) {
                    if (o >= 0 && o < this.total) {
                        fetch(toTileSource(this.services[o])).catch(() => {});
                    }
                }
            }
        }

        _emit() {
            this.onPageChange.emit(this.current);
        }
    }

    // Per-pi cache of in-flight/resolved Promises.
    const cache = new Map();

    /**
     * Fetches the IIIF Presentation manifest for a given PI and returns the
     * ordered list of image-service base URLs for all pages.
     *
     * The result Promise is memoized per pi so repeated calls never re-fetch.
     *
     * @param {string} pi       - Goobi viewer process identifier.
     * @param {string} apiBase  - Base URL of the REST API (no trailing slash).
     * @param {Function} fetchFn - fetch-compatible function (injectable for tests).
     * @returns {Promise<string[]>}
     */
    function loadPageServices(pi, apiBase, fetchFn = fetch) {
        if (cache.has(pi)) {
            return cache.get(pi);
        }

        const url = `${apiBase}/records/${pi}/manifest`;
        const promise = fetchFn(url).then((res) => {
            if (!res.ok) {
                throw new Error(`Failed to load manifest for "${pi}": HTTP ${res.status}`);
            }
            return res.json().then((manifest) => parseManifestImageServices(manifest));
        });

        cache.set(pi, promise);

        // Evict on rejection so a transient failure does not permanently poison
        // the cache; a later call will retry. The original rejection is rethrown.
        return promise.catch((e) => {
            cache.delete(pi);
            throw e;
        });
    }

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
     * URL-sync feature: pushes a history entry on page change (deep-linkable,
     * back/forward steps through pages) and navigates the viewer on popstate.
     * The viewer uses 0-based page orders; URLs use 1-based page numbers.
     *
     * @param {{onPageChange:{subscribe:Function}, goToPage:Function}} viewer
     * @param {string} pi
     */
    function attachUrlSync(viewer, pi) {
        viewer.onPageChange.subscribe((order) => {
            const url = new URL(window.location.href);
            url.pathname = pageUrlPath(url.pathname, pi, order + 1);
            window.history.pushState({ order }, '', url.toString());
        });
        window.addEventListener('popstate', (e) => {
            if (e.state && typeof e.state.order === 'number') {
                viewer.goToPage(e.state.order);
            }
        });
    }

    window.ShareImageFragment = ShareImageFragment;

    window.zoomableImageLoaded = new rxjs.Subject();

    document.addEventListener('DOMContentLoaded', () => {
        // Legacy object/fullscreen image view — only when its mount is present.
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

        // Immersive image viewer — only when its mount is present.
        const immersiveEl = document.querySelector('[data-immersive-image]');
        if (immersiveEl) {
            initImmersiveViewer(immersiveEl);
        }
    });

    /**
     * Bootstraps the immersive image viewer: fetches the IIIF manifest, instantiates
     * the engine, and attaches the URL-sync + minimal paging controls. Everything
     * else hooks onto viewer.onPageChange (no engine changes needed to extend).
     *
     * @param {HTMLElement} el  the [data-immersive-image] mount element
     */
    function initImmersiveViewer(el) {
        const pi = el.dataset.pi;
        const apiBase = el.dataset.apiBase;
        const startOrder = Number(el.dataset.startOrder) || 0;
        const maxZoom = el.dataset.maxZoom ? parseInt(el.dataset.maxZoom) : undefined;

        loadPageServices(pi, apiBase)
            .then((services) => {
                const viewer = new IvViewer({ element: el, services, startOrder, maxZoom });
                window.ivViewer = viewer;
                attachUrlSync(viewer, pi);

                const indicator = document.getElementById('immersivePageIndicator');
                const total = viewer.getPageCount();
                const updateIndicator = (order) => {
                    if (indicator) indicator.textContent = `${order + 1} / ${total}`;
                };
                updateIndicator(viewer.getCurrentOrder());
                viewer.onPageChange.subscribe(updateIndicator);

                // Overview: lazy-mounted thumbnail grid overlay; clicking a thumbnail
                // navigates in-place (no page reload) via the viewer engine.
                const gridOverlay = document.getElementById('immersiveGridOverlay');
                let gridMounted = false;
                const gridActions = new rxjs.Subject();
                gridActions.subscribe((e) => {
                    if (e && e.action === 'clickImage' && typeof e.value === 'number') {
                        viewer.goToPage(e.value);
                        if (gridOverlay) gridOverlay.hidden = true;
                    }
                });
                const toggleGrid = () => {
                    if (!gridOverlay) return;
                    const opening = gridOverlay.hidden;
                    gridOverlay.hidden = !opening;
                    if (opening && !gridMounted) {
                        riot.mount('#immersiveThumbnails', 'thumbnails', {
                            // IIIF v2 manifest (sequences[0].canvases); 'items' would be v3.
                            source: `${apiBase}/records/${pi}/manifest`,
                            type: 'sequence',
                            actionlistener: gridActions,
                            imagesize: '!160,220', // IIIF size string (fit within 160x220)
                        });
                        gridMounted = true;
                    }
                };

                document.querySelectorAll('[data-immersive-page]').forEach((btn) => {
                    btn.addEventListener('click', () => (btn.dataset.immersivePage === 'next' ? viewer.next() : viewer.prev()));
                });
                document.querySelectorAll('[data-immersive-action]').forEach((btn) => {
                    btn.addEventListener('click', () => {
                        const action = btn.dataset.immersiveAction;
                        if (action === 'zoom-in') viewer.zoomIn();
                        else if (action === 'zoom-out') viewer.zoomOut();
                        else if (action === 'rotate-left') viewer.rotateLeft();
                        else if (action === 'rotate-right') viewer.rotateRight();
                        else if (action === 'reset') viewer.resetView();
                        else if (action === 'fullscreen') toggleImmersiveFullscreen();
                        else if (action === 'overview') toggleGrid();
                    });
                });

                // Left slide-out panels (TOC / in-work search): toggle, one open at a time.
                document.querySelectorAll('[data-immersive-panel]').forEach((btn) => {
                    btn.addEventListener('click', () => {
                        const panel = document.getElementById(btn.dataset.immersivePanel);
                        if (!panel) return;
                        const wasOpen = panel.classList.contains('is-open');
                        document.querySelectorAll('.immersive__panel--left.is-open').forEach((p) => {
                            p.classList.remove('is-open');
                            p.setAttribute('aria-hidden', 'true');
                        });
                        if (!wasOpen) {
                            panel.classList.add('is-open');
                            panel.setAttribute('aria-hidden', 'false');
                        }
                    });
                });
            })
            .catch((e) => console.error('immersive viewer init failed', e));
    }

    /** Toggles native browser fullscreen on the immersive viewer hero (H3 = real fullscreen, Esc exits). */
    function toggleImmersiveFullscreen() {
        const el = document.querySelector('.immersive__viewer');
        if (!el) return;
        if (document.fullscreenElement) {
            document.exitFullscreen();
        } else {
            el.requestFullscreen?.();
        }
    }
})();
