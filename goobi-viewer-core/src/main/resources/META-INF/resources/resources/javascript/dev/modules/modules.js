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
     * Resolve a IIIF canvas `label` to a single display string. Handles a plain string,
     * a v2 `{'@value'}` object, an array of either, and a v3 language map
     * (`{ de: ['…'], none: ['…'] }`). Returns the first non-empty value found, or ''.
     *
     * @param {string|object|Array} label
     * @returns {string}
     */
    function resolveCanvasLabel(label) {
        if (label == null) return '';
        if (typeof label === 'string') return label;
        if (Array.isArray(label)) {
            for (const item of label) {
                const v = resolveCanvasLabel(item);
                if (v) return v;
            }
            return '';
        }
        if (typeof label === 'object') {
            if (typeof label['@value'] === 'string' && label['@value'].length > 0) return label['@value'];
            for (const v of Object.values(label)) {
                const resolved = resolveCanvasLabel(v);
                if (resolved) return resolved;
            }
        }
        return '';
    }

    /**
     * Extracts ordered `{id, label}` entries from a manifest's canvases (v2 sequences/
     * canvases or v3 items). Canvases without a resolvable image-service id are skipped,
     * so the result stays index-aligned for both the service URLs and the page labels.
     *
     * @param {object} manifest
     * @returns {Array<{id: string, label: string}>}
     */
    function parseManifestCanvasEntries(manifest) {
        if (!manifest || typeof manifest !== 'object') return [];

        if (Array.isArray(manifest.sequences) && manifest.sequences.length > 0) {
            const canvases = manifest.sequences[0].canvases;
            if (!Array.isArray(canvases)) return [];

            const entries = [];
            for (const canvas of canvases) {
                try {
                    const id = resolveServiceId(canvas.images[0].resource.service, '@id', 'id');
                    if (id !== null) entries.push({ id, label: resolveCanvasLabel(canvas.label) });
                } catch {}
            }
            return entries;
        }

        if (Array.isArray(manifest.items) && manifest.items.length > 0) {
            const entries = [];
            for (const canvas of manifest.items) {
                try {
                    const id = resolveServiceId(canvas.items[0].items[0].body.service, 'id', '@id');
                    if (id !== null) entries.push({ id, label: resolveCanvasLabel(canvas.label) });
                } catch {}
            }
            return entries;
        }

        return [];
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
        return parseManifestCanvasEntries(manifest).map((e) => e.id);
    }

    /**
     * Extracts the ordered list of canvas labels from a manifest, index-aligned with
     * {@link parseManifestImageServices}. Canvases without a resolvable label yield ''.
     *
     * @param {object} manifest - Parsed IIIF Presentation manifest.
     * @returns {string[]} Ordered array of label strings (one per page).
     */
    function parseManifestPageLabels(manifest) {
        return parseManifestCanvasEntries(manifest).map((e) => e.label);
    }

    /**
     * Returns the 0-based page indices shown together for the spread that contains
     * `order`. Book layout (LTR): the cover (page 0) stands alone, then pages are
     * paired (1,2),(3,4),… An odd final page stands alone. Pure + tested.
     *
     * @param {number} order  0-based page index
     * @param {number} total  total page count
     * @param {{coverAlone?:boolean}} [opts]
     * @returns {number[]} one or two page indices, ascending
     */
    function computeSpread(order, total, { coverAlone = true } = {}) {
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
     * Page indices that make up the frame containing `order`.
     * Single mode: [order]. Double mode: the spread (computeSpread). Pure.
     *
     * @param {number} order  0-based page index
     * @param {number} total  total page count
     * @param {{double?:boolean}} [opts]
     * @returns {number[]} one or two page indices, ascending
     */
    function framePages(order, total, { double = false } = {}) {
        const o = Math.max(0, Math.min(order, total - 1));
        return double ? computeSpread(o, total) : [o];
    }

    /**
     * Page indices to keep resident (current frame + the immediately adjacent
     * frames) so neighbour navigation is instant. Deduped, ascending, in range. Pure.
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

    /** Tweens one TiledImage's opacity 0→1 while fading another 1→0 (rAF). */
    function _crossfade(incoming, outgoing, durationMs) {
        return new Promise((resolve) => {
            let start = null;
            const step = (ts) => {
                if (start === null) start = ts;
                const t = durationMs <= 0 ? 1 : Math.min(1, (ts - start) / durationMs);
                if (incoming) incoming.setOpacity(t);
                if (outgoing) outgoing.setOpacity(1 - t);
                if (t < 1) requestAnimationFrame(step);
                else resolve();
            };
            requestAnimationFrame(step);
        });
    }

    /**
     * Widens a bounds rect to a tall band (same y/height, 3x width, same centre).
     * As a fitBounds anchor this makes OSD fit by HEIGHT, so pages of differing scan
     * aspect render at a uniform height and paging never jumps vertically.
     */
    function _heightBand(rect) {
        const band = rect.clone();
        band.x = rect.x - rect.width;
        band.width = rect.width * 3;
        return band;
    }

    /** Single-image sequence config (mirrors zoomableImage.mjs); _arrangeImageSequence reads it on every open(). */
    const _sequence = { columns: 1, useWindowing: true, windowSize: 100, windowExpandThreshold: 10, windowExpandSize: 50 };
    const PREFETCH_RADIUS = 1;

    /**
     * Immersive image viewer engine around a single live ImageView.Image (OSD).
     * Single pages are swapped flicker-free by fading in a preloaded neighbour; double
     * pages are composed by the library and transitioned with a snapshot crossfade.
     * Consumers subscribe to `onPageChange` / `onLoaded`.
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
            this.double = false;
            this.currentItem = null;
            this._anchor = null;
            this._preloaded = new Map();
            this._navigating = false;
            this._highlights = [];
            this._fadeMs = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : 160;
            this.onPageChange = new Emitter();
            this.onLoaded = new Emitter();

            this.viewer = new ImageView.Image({
                element: opts.element,
                fittingMode: 'fixed',
                margins: { top: 64, bottom: 72, left: 64, right: 64 },
                zoom: { enabled: true, max: opts.maxZoom },
                sequence: _sequence,
                navigator: { enabled: false },
            });
            this.zoom = new ImageView.Controls.Zoom(this.viewer);
            this.rotation = new ImageView.Controls.Rotation(this.viewer);

            this._open(this.current).then(() => {
                this._refreshPreload();
                this.onLoaded.emit(this.current);
            });
        }

        // --- search highlights ---

        /** Zeichnet Such-Treffer-Rechtecke (Bildpixel) als Overlays über das aktuelle Bild. */
        setHighlights(rects) {
            this.clearHighlights();
            const osd = this.viewer.openseadragon;
            const item = this.currentItem || osd.world.getItemAt(0);
            if (!item) return;
            this._highlights = (rects || []).map((r) => {
                const el = document.createElement('div');
                el.className = 'immersive__hl';
                osd.addOverlay({ element: el, location: item.imageToViewportRectangle(r.x, r.y, r.w, r.h) });
                return el;
            });
        }

        /** Entfernt alle Treffer-Overlays. */
        clearHighlights() {
            const osd = this.viewer.openseadragon;
            (this._highlights || []).forEach((el) => osd.removeOverlay(el));
            this._highlights = [];
        }

        // --- text-region overlays (hover linking, separate from search highlights) ---

        /**
         * Draws OCR line boxes (image pixels) as hoverable, id-tagged overlays and
         * returns a Map id → overlay element. Regions without a `rect` are skipped.
         */
        setTextRegions(regions) {
            this.clearTextRegions();
            const osd = this.viewer.openseadragon;
            const item = this.currentItem || osd.world.getItemAt(0);
            const map = new Map();
            if (!item) return map;
            (regions || []).forEach((r) => {
                if (!r.rect) return;
                const el = document.createElement('div');
                el.className = 'immersive__text-region';
                el.dataset.ivRegionId = r.id;
                osd.addOverlay({ element: el, location: item.imageToViewportRectangle(r.rect.x, r.rect.y, r.rect.w, r.rect.h) });
                map.set(r.id, el);
            });
            this._textRegions = Array.from(map.values());
            return map;
        }

        /** Removes all text-region overlays (leaves search highlights untouched). */
        clearTextRegions() {
            const osd = this.viewer.openseadragon;
            (this._textRegions || []).forEach((el) => osd.removeOverlay(el));
            this._textRegions = [];
        }

        // --- state ---

        getCurrentOrder() {
            return this.current;
        }

        getPageCount() {
            return this.total;
        }

        isDoublePage() {
            return this.double;
        }

        /** 0-based page indices currently displayed (one page, or two in double mode). */
        getCurrentPages() {
            return this.double ? computeSpread(this.current, this.total) : [this.current];
        }

        // --- navigation ---

        /** Navigate to the page/spread containing `order` (snaps to the spread leader in double mode). */
        goToPage(order) {
            const target = Math.max(0, Math.min(order, this.total - 1));
            if (this.double) {
                const leader = computeSpread(target, this.total)[0];
                if (leader === this.current) return;
                this._navigateSpread(leader);
                return;
            }
            if (target === this.current) return;
            this._crossfadeTo(target);
        }

        next() {
            if (this.double) {
                const pages = computeSpread(this.current, this.total);
                this.goToPage(pages[pages.length - 1] + 1);
            } else {
                this.goToPage(this.current + 1);
            }
        }

        prev() {
            this.goToPage(this.current - 1);
        }

        // --- view controls ---

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

        /** Resets rotation and zoom to fit the whole page (whole spread in double mode). */
        resetView() {
            this.rotation.rotateTo(0);
            if (this.double) {
                this.viewer.openseadragon.viewport.goHome(true);
            } else {
                this.zoom.goHome();
            }
        }

        /** Toggles book-spread mode and re-opens at the current position. Returns the new state. */
        toggleDoublePage() {
            this.double = !this.double;
            this.current = this.getCurrentPages()[0];
            this._open(this.current);
            return this.double;
        }

        // --- single-page crossfade ---

        /**
         * Single-page navigation: fade the (preloaded or freshly added) target page in
         * over the current one, then drop the old. No reload, no blank, no jump.
         */
        async _crossfadeTo(target) {
            if (this._navigating) return;
            this._navigating = true;
            try {
                const previous = this.currentItem;
                this.current = target;
                const item = await this._acquire(target, this._anchor);
                this._preloaded.delete(target);
                await this._whenContent(item);
                await _crossfade(item, previous, this._fadeMs);
                if (previous) this.viewer.openseadragon.world.removeItem(previous);
                this.currentItem = item;
                this._emit();
                this._refreshPreload();
            } finally {
                this._navigating = false;
            }
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

        /** Resolves once `item` has painted its first (low-res) tile, or is already fully loaded. */
        _whenContent(item) {
            if (item.getFullyLoaded()) return Promise.resolve();
            const osd = this.viewer.openseadragon;
            return new Promise((resolve) => {
                let done = false;
                const finish = () => {
                    if (done) return;
                    done = true;
                    osd.removeHandler('tile-loaded', onTile);
                    item.removeHandler('fully-loaded-change', onFull);
                    resolve();
                };
                const onTile = (e) => {
                    if (e.tiledImage === item) finish();
                };
                const onFull = (ev) => {
                    if (ev.fullyLoaded) finish();
                };
                osd.addHandler('tile-loaded', onTile);
                item.addHandler('fully-loaded-change', onFull);
            });
        }

        /** Keeps the adjacent pages (±1) preloaded and evicts the rest (single-page path). */
        _refreshPreload() {
            const resident = () => residentPages(this.current, this.total, { double: this.double });
            const keep = new Set(resident());
            for (const order of keep) {
                if (order === this.current) continue;
                if (!this._preloaded.has(order)) {
                    this._acquire(order, this._anchor);
                }
            }
            for (const [order, p] of this._preloaded) {
                if (!keep.has(order)) {
                    p.then((item) => {
                        try {
                            this.viewer.openseadragon.world.removeItem(item);
                        } catch (e) {}
                    }).catch(() => {});
                    this._preloaded.delete(order);
                }
            }
        }

        // --- double-page spread (snapshot crossfade) ---

        /**
         * Double-page navigation: freeze the current spread as a snapshot overlay, let the
         * library re-compose the new spread (columns:2) underneath, then fade the snapshot
         * out. Neighbour spreads are tile-prewarmed for near-instant sharpness.
         */
        async _navigateSpread(leader) {
            if (this._navigating) return;
            this._navigating = true;
            const overlay = this._snapshotOverlay();
            this.current = leader;
            try {
                await this._open(leader);
                this._fadeOverlay(overlay);
                this._prewarmSpreads();
            } catch (e) {
                if (overlay) overlay.remove();
            } finally {
                this._navigating = false;
            }
        }

        /**
         * Copies the current OSD canvas into an opacity overlay covering the viewer so the
         * fresh load() underneath stays hidden until faded out. Uses canvas drawImage (not
         * toDataURL) so cross-origin IIIF tiles don't taint the canvas.
         */
        _snapshotOverlay() {
            const osd = this.viewer.openseadragon;
            const src = osd.drawer && osd.drawer.canvas;
            const host = this.viewer.element;
            if (!src || !host) return null;
            const overlay = document.createElement('canvas');
            overlay.width = src.width;
            overlay.height = src.height;
            overlay.className = 'immersive__xfade';
            overlay.style.cssText = 'position:absolute;inset:0;width:100%;height:100%;pointer-events:none;z-index:2;';
            try {
                overlay.getContext('2d').drawImage(src, 0, 0);
            } catch (e) {
                return null;
            }
            if (getComputedStyle(host).position === 'static') host.style.position = 'relative';
            host.appendChild(overlay);
            return overlay;
        }

        /** Fades an overlay element out over `_fadeMs`, then removes it. */
        _fadeOverlay(overlay) {
            if (!overlay) return;
            let start = null;
            const dur = this._fadeMs;
            const step = (ts) => {
                if (start === null) start = ts;
                const t = dur <= 0 ? 1 : Math.min(1, (ts - start) / dur);
                overlay.style.opacity = String(1 - t);
                if (t < 1) requestAnimationFrame(step);
                else overlay.remove();
            };
            requestAnimationFrame(step);
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
                osd.addTiledImage({ tileSource: toTileSource(this.services[p]), opacity: 0, preload: true });
            }
        }

        // --- loading ---

        /**
         * Loads the page(s) for `order` via the library (a single page, or a columns:2
         * spread in double mode) and captures the single-page fit anchor. Used for the
         * initial open, mode toggles and every double-page spread change.
         */
        _open(order) {
            const pages = this.double ? computeSpread(order, this.total) : [order];
            this.viewer.config.sequence.columns = pages.length;
            const sources = pages.map((p) => toTileSource(this.services[p]));
            const loaded = this.viewer.load(sources, 0);
            this._prefetchAround(pages[pages.length - 1]);
            return loaded.then(() => {
                const world = this.viewer.openseadragon.world;
                this.currentItem = world.getItemAt(0);
                if (!this.double && this.currentItem) {
                    this._anchor = _heightBand(this.currentItem.getBounds());
                }
                this._preloaded.clear();
                this._emit();
            });
        }

        /** Warms neighbour info.json in the browser cache so the next load is faster. */
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

    const cache = new Map();

    /**
     * Fetches (and memoizes per pi) the parsed IIIF Presentation manifest for a PI, so
     * services and labels share a single network request.
     *
     * @param {string} pi       - Goobi viewer process identifier.
     * @param {string} apiBase  - Base URL of the REST API (no trailing slash).
     * @param {Function} fetchFn - fetch-compatible function (injectable for tests).
     * @returns {Promise<object>} the parsed manifest JSON.
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

        // Evict on failure so a transient error doesn't poison the cache; the rejection is rethrown.
        return promise.catch((e) => {
            cache.delete(pi);
            throw e;
        });
    }

    /**
     * Returns the ordered list of image-service base URLs for all pages of a record.
     *
     * @param {string} pi       - Goobi viewer process identifier.
     * @param {string} apiBase  - Base URL of the REST API (no trailing slash).
     * @param {Function} fetchFn - fetch-compatible function (injectable for tests).
     * @returns {Promise<string[]>}
     */
    function loadPageServices(pi, apiBase, fetchFn = fetch) {
        return loadManifest(pi, apiBase, fetchFn).then(parseManifestImageServices);
    }

    /**
     * Returns the ordered list of canvas labels for all pages, index-aligned with
     * {@link loadPageServices}. Shares the memoized manifest fetch (no extra request).
     *
     * @param {string} pi       - Goobi viewer process identifier.
     * @param {string} apiBase  - Base URL of the REST API (no trailing slash).
     * @param {Function} fetchFn - fetch-compatible function (injectable for tests).
     * @returns {Promise<string[]>}
     */
    function loadPageLabels(pi, apiBase, fetchFn = fetch) {
        return loadManifest(pi, apiBase, fetchFn).then(parseManifestPageLabels);
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
     * Maps a IIIF/W3C `sc:AnnotationList` (one annotation per OCR text line) to
     * structured lines that keep the line box from the annotation `on` selector
     * (a `xywh=x,y,w,h` fragment, found on `on` as a string, on `on['@id']`, or in
     * `on.selector.value`). Lines without a box get `rect: null`. The `id` is the
     * annotation `@id` when present, else a per-page index fallback `line-${i}`.
     *
     * @param {object} annotationList - the parsed `sc:AnnotationList` JSON.
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
     * Fetches the OCR fulltext of a single page as structured lines (chars + box).
     *
     * @param {string} pi
     * @param {string} apiBase
     * @param {number} order    - 0-based page order; the endpoint is 1-based, so order + 1.
     * @param {Function} fetchFn
     * @returns {Promise<{id:string, chars:string, rect:object|null}[]>} lines, or [] on failure.
     */
    async function loadPageLines(pi, apiBase, order, fetchFn = fetch) {
        const res = await fetchFn(`${apiBase}/records/${pi}/pages/${order + 1}/text/`);
        if (!res.ok) {
            return [];
        }
        return parsePageLines(await res.json());
    }

    /**
     * Granularity dispatch for the hover-linking panel. `'line'` returns the page's
     * OCR lines with boxes; `'word'` fetches the same page-text endpoint with
     * `?granularity=word` and parses the result through `parsePageLines`.
     *
     * @param {string} pi
     * @param {string} apiBase
     * @param {number} order
     * @param {string} granularity - 'line' | 'word'
     * @param {Function} fetchFn
     * @returns {Promise<Array>}
     */
    async function loadPageRegions(pi, apiBase, order, granularity, fetchFn = fetch) {
        if (granularity === 'word') {
            const res = await fetchFn(`${apiBase}/records/${pi}/pages/${order + 1}/text/?granularity=word`);
            if (!res.ok) {
                return [];
            }
            return parsePageLines(await res.json()).filter((r) => (r.chars || '').trim() !== '');
        }
        return loadPageLines(pi, apiBase, order, fetchFn);
    }

    /** OCR line ↔ image region hover linking for the immersive fulltext panel. */

    const ACTIVE_CLASS = 'is-linked-active';

    /**
     * Builds the fulltext line spans for the panel: one `<span>` per line, tagged
     * with `data-iv-region-id` to match its image overlay. Text is set via
     * `textContent`, so OCR content can never inject markup.
     *
     * @param {{id:string, chars:string}[]} lines
     * @returns {DocumentFragment}
     */
    function buildLineSpans(lines) {
        const frag = document.createDocumentFragment();
        (lines || []).forEach((line) => {
            const span = document.createElement('span');
            span.className = 'immersive__fulltext-line';
            span.dataset.ivRegionId = line.id;
            span.textContent = line.chars || '';
            frag.appendChild(span);
        });
        return frag;
    }

    /**
     * Indexes elements by their `data-iv-region-id` (elements without one are skipped).
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
     * Computes the scrollTop needed to center a line within its scroll container,
     * or null if the line is already fully visible. Pure (numbers only) so it is
     * testable without a layout engine.
     *
     * @param {{offsetTop:number, height:number, scrollTop:number, clientHeight:number, scrollHeight:number}} m
     * @returns {number|null}
     */
    function scrollTopToReveal(m) {
        const top = m.offsetTop;
        const bottom = m.offsetTop + m.height;
        if (top >= m.scrollTop && bottom <= m.scrollTop + m.clientHeight) return null;
        const target = m.offsetTop - m.clientHeight / 2 + m.height / 2;
        const max = Math.max(0, m.scrollHeight - m.clientHeight);
        return Math.min(Math.max(0, target), max);
    }

    /**
     * Cumulative offsetTop of `el` relative to `container`, mirroring the panel
     * scroll math used elsewhere in the immersive view.
     * @param {HTMLElement} el
     * @param {HTMLElement} container
     * @returns {number}
     */
    function _offsetTopWithin(el, container) {
        let top = 0;
        for (let n = el; n && n !== container; n = n.offsetParent) top += n.offsetTop;
        return top;
    }

    /**
     * Wires bidirectional hover highlighting between the panel line spans (inside
     * `box`) and the image region overlays (`regionEls`) that share the same
     * `data-iv-region-id`. Hover on either side toggles `is-linked-active` on both.
     *
     * @param {object} opts
     * @param {HTMLElement} opts.box - fulltext panel element containing the line spans.
     * @param {Map<string, HTMLElement>} opts.regionEls - image overlay elements, already indexed by id.
     * @param {HTMLElement} [opts.scrollContainer] - scrollable panel; when set, hovering an
     *   image overlay scrolls its line into view if not fully visible.
     * @returns {{destroy: function():void}}
     */
    function mountTextImageLink({ box, regionEls, scrollContainer }) {
        const spanEls = indexById(box.querySelectorAll('[data-iv-region-id]'));
        const regions = regionEls || new Map();
        const listeners = [];

        const setActive = (id, on) => {
            const span = spanEls.get(id);
            const overlay = regions.get(id);
            if (span) span.classList.toggle(ACTIVE_CLASS, on);
            if (overlay) overlay.classList.toggle(ACTIVE_CLASS, on);
        };

        const revealSpan = (id) => {
            if (!scrollContainer) return;
            const span = spanEls.get(id);
            if (!span) return;
            const target = scrollTopToReveal({
                offsetTop: _offsetTopWithin(span, scrollContainer),
                height: span.offsetHeight,
                scrollTop: scrollContainer.scrollTop,
                clientHeight: scrollContainer.clientHeight,
                scrollHeight: scrollContainer.scrollHeight,
            });
            if (target !== null) scrollContainer.scrollTop = target;
        };

        const bind = (el, id, isOverlay) => {
            const enter = () => {
                setActive(id, true);
                if (isOverlay) revealSpan(id);
            };
            const leave = () => setActive(id, false);
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
     * Groups consecutive word regions into lines by VERTICAL OVERLAP of their
     * boxes (robust against within-line top variation from ascenders/descenders).
     * Two consecutive words share a line when their vertical ranges overlap by more
     * than half the shorter box height. Words without a rect stay on the current
     * line and do not reset the baseline. Order preserved. Pure.
     *
     * @param {{id:string, chars:string, rect:{x:number,y:number,w:number,h:number}|null}[]} regions
     * @returns {Array<Array<object>>}
     */
    function groupWordsIntoLines(regions) {
        const lines = [];
        let cur = null;
        let pTop = null;
        let pBot = null;
        for (const r of regions || []) {
            const rect = r && r.rect;
            const top = rect ? rect.y : null;
            const bot = rect ? rect.y + rect.h : null;
            let newLine;
            if (cur === null) {
                newLine = true;
            } else if (top === null || pTop === null) {
                newLine = false;
            } else {
                const overlap = Math.min(pBot, bot) - Math.max(pTop, top);
                const minH = Math.min(pBot - pTop, bot - top);
                newLine = overlap <= 0.5 * minH;
            }
            if (newLine) {
                cur = [r];
                lines.push(cur);
            } else {
                cur.push(r);
            }
            if (top !== null) {
                pTop = top;
                pBot = bot;
            }
        }
        return lines;
    }

    /**
     * Builds word spans grouped into line blocks (same layout as line mode, but each
     * word is an individually hoverable `<span data-iv-region-id>`). Blank-chars
     * words (ALTO spaces) are skipped; a single space separates rendered words.
     * Text via `textContent`. Line blocks reuse `immersive__fulltext-line`.
     *
     * @param {{id:string, chars:string, rect:object|null}[]} regions
     * @returns {DocumentFragment}
     */
    function buildWordSpans(regions) {
        const frag = document.createDocumentFragment();
        for (const words of groupWordsIntoLines(regions)) {
            const line = document.createElement('span');
            line.className = 'immersive__fulltext-line';
            let first = true;
            for (const w of words) {
                const text = (w && w.chars) || '';
                if (!text.trim()) {
                    continue;
                }
                if (!first) {
                    line.appendChild(document.createTextNode(' '));
                }
                const span = document.createElement('span');
                span.className = 'immersive__fulltext-word';
                span.dataset.ivRegionId = w.id;
                span.textContent = text;
                line.appendChild(span);
                first = false;
            }
            if (line.childNodes.length) {
                frag.appendChild(line);
            }
        }
        return frag;
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

    /** Snippet aus IIIF `resource` (Objekt, Array oder fehlend) ziehen. */
    function _snippet(resource) {
        const r = Array.isArray(resource) ? resource[0] : resource;
        return r && typeof r.value === 'string' ? r.value : '';
    }

    /**
     * Parst eine IIIF Content Search `sc:AnnotationList` zu Treffern.
     * Seite ist 1-basiert (wie in der `on`-URL `/pages/{n}/canvas`); der Aufrufer
     * rechnet auf die 0-basierte IvViewer-Order um (order = page - 1).
     * @returns {{page:number, rect:{x,y,w,h}|null, snippet:string}[]}
     */
    function parseSearchHits(annotationList) {
        if (!annotationList || !Array.isArray(annotationList.resources)) return [];
        const hits = [];
        for (const res of annotationList.resources) {
            const on = typeof res.on === 'string' ? res.on : (res.on && res.on['@id']) || '';
            const page = on.match(/\/pages\/(\d+)\/canvas/);
            if (!page) continue;
            const xywh = on.match(/#xywh=(\d+),(\d+),(\d+),(\d+)/);
            hits.push({
                page: Number(page[1]),
                rect: xywh ? { x: +xywh[1], y: +xywh[2], w: +xywh[3], h: +xywh[4] } : null,
                snippet: _snippet(res.resource),
            });
        }
        return hits;
    }

    function nextIndex(i, total) {
        return total ? (i + 1) % total : -1;
    }
    function prevIndex(i, total) {
        return total ? (i - 1 + total) % total : -1;
    }

    /** Letzte Ergebnis-Seite aus `within.last` (`…&page=N`); 1 wenn nicht vorhanden. */
    function _lastPage(list) {
        const last = list && list.within && list.within.last;
        const m = typeof last === 'string' ? last.match(/[?&]page=(\d+)/) : null;
        return m ? Number(m[1]) : 1;
    }

    /**
     * Parst die IIIF-Search-Response tolerant gegen einen Backend-Bug: ein `search:Hit`
     * ohne Annotationen wird als `{"@type":"search:Hit","annotations"}` (Key ohne Wert)
     * serialisiert → invalides JSON. Genau dieses Muster wird vor dem Parsen repariert;
     * wir nutzen ohnehin nur `resources`, nicht `hits`.
     * TODO(iiif-api-model): nach URLOnlySerializer-Fix (leere annotations → valides JSON)
     * entfernen — dann reicht `res.json()` ohne Repair.
     */
    function _parseSearchJson(text) {
        try {
            return JSON.parse(text.replace(/"annotations"\}/g, '"annotations":[]}'));
        } catch (e) {
            return {};
        }
    }

    /**
     * Holt alle Ergebnis-Seiten der IIIF Content Search und liefert Treffer mit
     * 0-basierter IvViewer-`order` (order = page - 1), in Dokumentreihenfolge.
     * @param {function} fetchFn fetch-kompatibel (injizierbar für Tests)
     * @param {number} maxPages Sicherheits-Cap der within-Paging-Schleife
     */
    async function search(pi, apiBase, term, fetchFn = fetch, maxPages = 50) {
        const base = `${apiBase}/records/${pi}/manifest/search?q=${encodeURIComponent(term)}`;
        const all = [];
        for (let p = 1; p <= maxPages; p++) {
            const res = await fetchFn(p === 1 ? base : `${base}&page=${p}`);
            if (!res.ok) break;
            const list = _parseSearchJson(await res.text());
            const hits = parseSearchHits(list);
            all.push(...hits);
            if (hits.length === 0 || p >= _lastPage(list)) break;
        }
        return all.map((h) => ({ ...h, order: h.page - 1 }));
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

        // Slide-out panels (TOC / search): bind open/close immediately -- before the IIIF
        // services fetch -- so the server-rendered sidebar opens without waiting for the
        // first image. The triggering button is marked active while its panel is open.
        const immersiveRoot = el.closest('.immersive');
        const panelButtons = document.querySelectorAll('[data-immersive-panel]');
        // Flag the root while a left panel is open so CSS can hide the floating title and
        // prev chevron over the image (the title + close live in the panel header now).
        const syncPanelOpenFlag = () => {
            if (immersiveRoot) {
                immersiveRoot.classList.toggle('immersive--panel-open', !!document.querySelector('.immersive__panel--left.is-open'));
            }
        };
        // Set by the fulltext block; clears its image overlays + hover wiring when the panel closes.
        let onFulltextClose = null;
        const closePanels = () => {
            if (typeof onFulltextClose === 'function') onFulltextClose();
            document.querySelectorAll('.immersive__panel--left.is-open').forEach((p) => {
                p.classList.remove('is-open');
                p.setAttribute('aria-hidden', 'true');
            });
            panelButtons.forEach((b) => {
                b.classList.remove('immersive__tool-btn--active');
                b.setAttribute('aria-expanded', 'false');
            });
            syncPanelOpenFlag();
        };
        panelButtons.forEach((btn) => {
            btn.addEventListener('click', () => {
                // A disabled rail tool (e.g. fulltext in double-page mode) must not open its panel.
                if (btn.getAttribute('aria-disabled') === 'true') return;
                const panel = document.getElementById(btn.dataset.immersivePanel);
                if (!panel) return;
                const wasOpen = panel.classList.contains('is-open');
                closePanels();
                if (!wasOpen) {
                    panel.classList.add('is-open');
                    panel.setAttribute('aria-hidden', 'false');
                    btn.classList.add('immersive__tool-btn--active');
                    btn.setAttribute('aria-expanded', 'true');
                    syncPanelOpenFlag();
                    // Bring the active TOC entry into view (e.g. reloaded on a page far down,
                    // so the highlighted section isn't left off-screen at the top). Scroll the
                    // panel via offset math, not scrollIntoView, so the whole page never moves.
                    const active = panel.querySelector('.widget-toc__element.active');
                    if (active) {
                        let top = 0;
                        for (let n = active; n && n !== panel; n = n.offsetParent) top += n.offsetTop;
                        panel.scrollTop = Math.max(0, top - panel.clientHeight / 2);
                    }
                }
            });
        });
        // No close button in the panel anymore: Escape closes the open panel (re-clicking
        // the burger toggles it shut too).
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && document.querySelector('.immersive__panel--left.is-open')) {
                closePanels();
            }
        });

        // Sidebar resize: one drag handle at the open panel's right edge sets a single
        // --immersive-panel-width on .immersive__viewer, so all left panels share one width.
        // The chosen width persists in localStorage and is re-applied (clamped) on load.
        const immersiveViewer = immersiveRoot && immersiveRoot.querySelector('.immersive__viewer');
        if (immersiveViewer) {
            const WIDTH_KEY = 'immersive-panel-width';
            const RAIL_WIDTH = 40; // left tool rail; panels start at left: 40px
            const MIN_WIDTH = 240;
            const maxWidth = () => immersiveViewer.getBoundingClientRect().width * 0.8;
            const clamp = (px) => Math.min(Math.max(px, MIN_WIDTH), maxWidth());
            const applyWidth = (px) => immersiveViewer.style.setProperty('--immersive-panel-width', Math.round(px) + 'px');
            let stored = NaN;
            try {
                stored = parseInt(localStorage.getItem(WIDTH_KEY), 10);
            } catch (e) {
                // localStorage may be unavailable (private mode / blocked) -- defaults apply.
            }
            if (Number.isFinite(stored)) applyWidth(clamp(stored));

            const handle = document.createElement('div');
            handle.className = 'immersive__panel-resize-handle';
            handle.setAttribute('aria-hidden', 'true');
            immersiveViewer.appendChild(handle);
            handle.addEventListener('pointerdown', (e) => {
                e.preventDefault();
                handle.setPointerCapture(e.pointerId);
                immersiveRoot.classList.add('immersive--resizing');
                const viewerLeft = immersiveViewer.getBoundingClientRect().left;
                const widthAt = (ev) => clamp(ev.clientX - viewerLeft - RAIL_WIDTH);
                const onMove = (ev) => applyWidth(widthAt(ev));
                const onUp = (ev) => {
                    handle.releasePointerCapture(e.pointerId);
                    handle.removeEventListener('pointermove', onMove);
                    handle.removeEventListener('pointerup', onUp);
                    immersiveRoot.classList.remove('immersive--resizing');
                    try {
                        localStorage.setItem(WIDTH_KEY, String(Math.round(widthAt(ev))));
                    } catch (err) {
                        // ignore: nothing to persist if storage is unavailable
                    }
                };
                handle.addEventListener('pointermove', onMove);
                handle.addEventListener('pointerup', onUp);
            });
        }

        // TOC "collapse all / expand all" toggle: the tree is server-rendered, so wire it up
        // immediately (no viewer needed) and show it right away -- only when the TOC actually
        // nests. Collapse folds to the top-level chapters (the record root is hidden, so we
        // never fold to it); state-driven, so a click expands all if anything is collapsed.
        const tocPanel = document.getElementById('immersivePanelMenu');
        const tocContainer = document.getElementById('widgetToc');
        const tocToggle = tocPanel && tocPanel.querySelector('[data-immersive-toc-toggle]');
        if (tocContainer && tocToggle && tocContainer.querySelector(".widget-toc__element[data-level='2']")) {
            tocToggle.hidden = false;
            const reflectTocToggle = () => {
                const collapsed = !!tocContainer.querySelector('.widget-toc__element--hidden');
                tocToggle.classList.toggle('immersive__toc-collapse--collapsed', collapsed);
                const label = collapsed ? tocToggle.dataset.labelExpand : tocToggle.dataset.labelCollapse;
                tocToggle.setAttribute('aria-label', label);
                tocToggle.setAttribute('title', label);
            };
            tocToggle.addEventListener('click', () => {
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

        loadPageServices(pi, apiBase)
            .then((services) => {
                const viewer = new IvViewer({ element: el, services, startOrder, maxZoom });
                window.ivViewer = viewer;
                attachUrlSync(viewer, pi);
                viewer.onLoaded.subscribe(() => mountImageFilters(viewer));

                const indicator = document.getElementById('immersivePageIndicator');
                const titlePage = document.getElementById('immersiveTitlePage');
                const total = viewer.getPageCount();
                const updateIndicator = () => {
                    const pages = viewer.getCurrentPages().map((p) => p + 1);
                    const label = pages.length > 1 ? `${pages[0]}–${pages[pages.length - 1]}` : `${pages[0]}`;
                    if (indicator) indicator.textContent = `${label} / ${total}`;
                    // Mirror the page next to the work title, e.g. "(5 / 40)".
                    if (titlePage) titlePage.textContent = `(${label} / ${total})`;
                };
                updateIndicator();
                viewer.onPageChange.subscribe(() => updateIndicator());

                // Page chevrons: only show an arrow when paging that way is possible
                // (hide prev on the first page, next on the last page).
                const prevChevron = document.querySelector('.immersive__chevron[data-immersive-page="prev"]');
                const nextChevron = document.querySelector('.immersive__chevron[data-immersive-page="next"]');
                const updateChevrons = () => {
                    const pages = viewer.getCurrentPages();
                    if (!pages.length) return;
                    if (prevChevron) prevChevron.hidden = Math.min(...pages) <= 0;
                    if (nextChevron) nextChevron.hidden = Math.max(...pages) >= total - 1;
                };
                updateChevrons();
                viewer.onPageChange.subscribe(updateChevrons);

                // Title page picker: clicking the work title opens a dropdown with a "go to page"
                // input and a scrollable list of the IIIF manifest page labels. Selecting jumps
                // the viewer. Labels share the memoized manifest fetch, so this costs no request.
                const setupPageDropdown = (labels) => {
                    const trigger = document.querySelector('[data-immersive-title-trigger]');
                    const dropdown = document.getElementById('immersivePageDropdown');
                    const list = document.getElementById('immersivePageList');
                    const input = document.getElementById('immersivePageInput');
                    if (!trigger || !dropdown || !list) return;
                    // A single-page record has nothing to pick: leave the title non-interactive.
                    if (total < 2) {
                        trigger.classList.add('immersive__title-trigger--static');
                        return;
                    }
                    if (input) input.max = String(total);

                    // One row per page as "<running number>: <raw manifest label>", e.g.
                    // "1: -", "3: [1]", "8: 5" -- the manifest label is shown verbatim
                    // (a blank " - " label trims to "-"), like the classic page dropdown.
                    const items = [];
                    for (let order = 0; order < total; order++) {
                        const li = document.createElement('li');
                        li.setAttribute('role', 'option');
                        const btn = document.createElement('button');
                        btn.type = 'button';
                        btn.className = 'immersive__page-dropdown-item';
                        btn.dataset.order = String(order);
                        const num = document.createElement('span');
                        num.className = 'immersive__page-dropdown-num';
                        num.textContent = `${order + 1}:`;
                        const lbl = document.createElement('span');
                        lbl.className = 'immersive__page-dropdown-itemlabel';
                        lbl.textContent = (labels[order] || '').trim();
                        btn.append(num, lbl);
                        li.append(btn);
                        list.append(li);
                        items.push(btn);
                    }

                    const markActive = () => {
                        const current = viewer.getCurrentPages();
                        items.forEach((btn) => {
                            const on = current.includes(Number(btn.dataset.order));
                            btn.classList.toggle('is-active', on);
                            btn.setAttribute('aria-selected', on ? 'true' : 'false');
                        });
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
                        // Centre the active row WITHIN the list only -- never via scrollIntoView /
                        // focus(), which would scroll the page and visibly shift the image.
                        const active = list.querySelector('.immersive__page-dropdown-item.is-active');
                        if (active) {
                            list.scrollTop = Math.max(0, active.offsetTop - list.offsetTop - (list.clientHeight - active.clientHeight) / 2);
                        }
                        if (input) {
                            input.value = '';
                            input.focus({ preventScroll: true });
                        }
                    };

                    trigger.addEventListener('click', () => (isOpen() ? close() : open()));
                    list.addEventListener('click', (e) => {
                        const btn = e.target.closest('.immersive__page-dropdown-item');
                        if (!btn) return;
                        viewer.goToPage(Number(btn.dataset.order));
                        close();
                    });
                    if (input) {
                        input.addEventListener('keydown', (e) => {
                            if (e.key !== 'Enter') return;
                            e.preventDefault();
                            const n = parseInt(input.value, 10);
                            if (Number.isFinite(n) && n >= 1 && n <= total) {
                                viewer.goToPage(n - 1);
                                close();
                            }
                        });
                    }
                    document.addEventListener('keydown', (e) => {
                        if (e.key === 'Escape' && isOpen()) {
                            close();
                            trigger.focus();
                        }
                    });
                    viewer.onPageChange.subscribe(markActive);
                    markActive();
                };
                loadPageLabels(pi, apiBase)
                    .then(setupPageDropdown)
                    .catch(() => {});

                // Fulltext: in-place IIIF content search → result list (left panel) + image hit highlights.
                const fts = { hits: [], idx: -1, term: '' };
                const resultsBox = document.getElementById('immersiveSearchResults');
                const resultsList = document.getElementById('immersiveResultsList');
                const hitCounter = document.getElementById('immersiveHitCounter');
                const hitsOnOrder = (order) => fts.hits.filter((h) => h.order === order);

                const renderHighlights = () => {
                    const rects = viewer.getCurrentPages().flatMap((o) =>
                        hitsOnOrder(o)
                            .map((h) => h.rect)
                            .filter(Boolean)
                    );
                    viewer.setHighlights(rects);
                };

                const renderResults = () => {
                    if (!resultsBox || !resultsList) return;
                    resultsBox.hidden = !fts.term;
                    hitCounter.textContent = fts.hits.length ? `${fts.idx + 1} / ${fts.hits.length}` : '';
                    resultsList.innerHTML = '';
                    if (fts.term && !fts.hits.length) {
                        const empty = document.createElement('li');
                        empty.className = 'immersive__results-empty';
                        empty.textContent = resultsBox.dataset.labelEmpty;
                        resultsList.appendChild(empty);
                        return;
                    }
                    fts.hits.forEach((h, i) => {
                        const li = document.createElement('li');
                        li.className = 'immersive__results-item' + (i === fts.idx ? ' is-active' : '');
                        const page = document.createElement('span');
                        page.className = 'immersive__results-page';
                        page.textContent = h.page;
                        const snippet = document.createElement('span');
                        snippet.className = 'immersive__results-snippet';
                        snippet.textContent = h.snippet || '';
                        li.append(page, snippet);
                        li.addEventListener('click', () => gotoHit(i));
                        resultsList.appendChild(li);
                    });
                };

                const gotoHit = (i) => {
                    if (!fts.hits.length) return;
                    fts.idx = (i + fts.hits.length) % fts.hits.length;
                    const hit = fts.hits[fts.idx];
                    if (!viewer.getCurrentPages().includes(hit.order)) viewer.goToPage(hit.order);
                    else renderHighlights();
                    renderResults();
                };

                const runSearch = async (term) => {
                    fts.term = term;
                    fts.hits = term ? await search(pi, apiBase, term) : [];
                    fts.idx = fts.hits.length ? 0 : -1;
                    renderResults();
                    if (fts.idx >= 0) gotoHit(0);
                    else viewer.clearHighlights();
                };

                viewer.onPageChange.subscribe(renderHighlights);

                const searchPanel = document.getElementById('immersivePanelSearch');
                const searchForm = searchPanel && searchPanel.querySelector('form');
                const searchInput = searchPanel && searchPanel.querySelector('input[type="text"]');
                if (searchForm && searchInput) {
                    searchForm.addEventListener('submit', (e) => {
                        e.preventDefault();
                        runSearch(searchInput.value.trim());
                    });
                }
                document.querySelectorAll('[data-immersive-hit]').forEach((btn) => {
                    btn.addEventListener('click', () => {
                        const n = fts.hits.length;
                        gotoHit(btn.dataset.immersiveHit === 'next' ? nextIndex(fts.idx, n) : prevIndex(fts.idx, n));
                    });
                });

                // Fulltext (page OCR): render the current page's transcription in the left panel,
                // DFG-Viewer style. The text endpoint is per single page, so this is single-page
                // only -- double-page mode disables the rail button (see updateFulltextAvail).
                const fulltextPanel = document.getElementById('immersivePanelFulltext');
                const fulltextBox = document.getElementById('immersiveFulltext');
                const fulltextLoader = document.getElementById('immersiveFulltextLoader');
                const fulltextBtn = document.querySelector('[data-immersive-panel="immersivePanelFulltext"]');
                const fulltextTitleDefault = fulltextBtn ? fulltextBtn.getAttribute('title') : '';
                if (fulltextPanel && fulltextBox) {
                    let granularity = 'line';
                    let fulltextReq = 0;
                    let currentLink = null;

                    const clearFulltextLink = () => {
                        if (currentLink) {
                            currentLink.destroy();
                            currentLink = null;
                        }
                        viewer.clearTextRegions();
                        if (fulltextLoader) fulltextLoader.hidden = true;
                    };
                    // Close-Hook: when any panel close runs, drop our overlays + wiring.
                    onFulltextClose = clearFulltextLink;

                    // Out-of-order guard: only the latest page request paints its text.
                    const loadFulltext = async () => {
                        const order = viewer.getCurrentPages()[0];
                        if (order === undefined) return;
                        const req = ++fulltextReq;
                        clearFulltextLink();
                        if (fulltextLoader) fulltextLoader.hidden = false;
                        fulltextBox.textContent = '';
                        fulltextBox.classList.remove('immersive__fulltext--empty');
                        let regions = null;
                        try {
                            regions = await loadPageRegions(pi, apiBase, order, granularity);
                        } catch (e) {
                            regions = null;
                        }
                        if (req !== fulltextReq) return;
                        if (fulltextLoader) fulltextLoader.hidden = true;
                        if (regions && regions.length) {
                            const fragment = granularity === 'word' ? buildWordSpans(regions) : buildLineSpans(regions);
                            fulltextBox.replaceChildren(fragment);
                            const regionEls = viewer.setTextRegions(regions);
                            currentLink = mountTextImageLink({ box: fulltextBox, regionEls, scrollContainer: fulltextPanel });
                        } else {
                            fulltextBox.textContent = fulltextBox.dataset.labelEmpty || '';
                            fulltextBox.classList.add('immersive__fulltext--empty');
                        }
                    };

                    // Granularity toggle (v1: 'word' is disabled in the markup; hook is ready).
                    const granularityBtns = fulltextPanel.querySelectorAll('[data-immersive-granularity]');
                    granularityBtns.forEach((b) => {
                        b.addEventListener('click', () => {
                            if (b.disabled) return;
                            granularity = b.dataset.immersiveGranularity;
                            granularityBtns.forEach((x) => {
                                const on = x === b;
                                x.classList.toggle('is-active', on);
                                x.setAttribute('aria-pressed', String(on));
                            });
                            if (fulltextPanel.classList.contains('is-open')) loadFulltext();
                        });
                    });

                    if (fulltextBtn) {
                        fulltextBtn.addEventListener('click', () => {
                            if (fulltextPanel.classList.contains('is-open')) loadFulltext();
                        });
                    }
                    viewer.onPageChange.subscribe(() => {
                        if (fulltextPanel.classList.contains('is-open')) loadFulltext();
                    });
                }

                // Fulltext is per single page: in double-page mode grey out + disable the rail
                // button (its tooltip explains why) and close the panel if it was open.
                const updateFulltextAvail = () => {
                    if (!fulltextBtn) return;
                    const doublePage = !!(viewer.isDoublePage && viewer.isDoublePage());
                    fulltextBtn.classList.toggle('immersive__tool-btn--disabled', doublePage);
                    fulltextBtn.setAttribute('aria-disabled', String(doublePage));
                    const title = (doublePage && fulltextBtn.dataset.titleDisabled) || fulltextTitleDefault;
                    fulltextBtn.setAttribute('title', title);
                    fulltextBtn.setAttribute('aria-label', title);
                    if (doublePage && fulltextPanel && fulltextPanel.classList.contains('is-open')) closePanels();
                };
                updateFulltextAvail();

                // Overview: thumbnail grid overlay (lazy-mounted).
                const gridOverlay = document.getElementById('immersiveGridOverlay');
                const gridLoader = document.getElementById('immersiveGridLoader');
                let gridMounted = false;
                let gridTag = null;
                const gridActions = new rxjs.Subject();
                gridActions.subscribe((e) => {
                    if (e && e.action === 'clickImage' && typeof e.value === 'number') {
                        viewer.goToPage(e.value);
                        if (gridOverlay) gridOverlay.hidden = true;
                    }
                });
                // The grid highlights the current page via opts.index -- the 0-based
                // canvas index, which equals the viewer's 0-based page order. Keep it in
                // sync so the right sheet stays selected as the page changes.
                const currentOrder = () => {
                    const pages = viewer.getCurrentPages ? viewer.getCurrentPages() : [];
                    return pages.length ? pages[0] : 0;
                };
                const syncGridSelection = () => {
                    if (gridTag) {
                        gridTag.opts.index = currentOrder();
                        gridTag.update();
                    }
                };
                viewer.onPageChange.subscribe(() => {
                    if (gridOverlay && !gridOverlay.hidden) syncGridSelection();
                });
                const toggleGrid = () => {
                    if (!gridOverlay) return;
                    const opening = gridOverlay.hidden;
                    gridOverlay.hidden = !opening;
                    if (!opening) return;
                    if (!gridMounted) {
                        gridTag = riot.mount('#immersiveThumbnails', 'thumbnails', {
                            source: `${apiBase}/records/${pi}/manifest`,
                            type: 'sequence',
                            actionlistener: gridActions,
                            imagesize: '!320,440', // IIIF size string (fit within 320x440, crisp on HiDPI)
                            index: currentOrder(),
                        })[0];
                        gridMounted = true;
                        // Hide the loading screen as soon as the first thumbnail paints
                        // (safety timeout in case the manifest/images never resolve).
                        let gridLoaderDone = false;
                        let gridLoaderTimer;
                        const hideGridLoader = () => {
                            if (gridLoaderDone) return;
                            gridLoaderDone = true;
                            clearTimeout(gridLoaderTimer);
                            if (gridLoader) gridLoader.hidden = true;
                        };
                        // <img> load events don't bubble -> listen in the capture phase
                        const thumbsMount = document.getElementById('immersiveThumbnails');
                        if (thumbsMount) thumbsMount.addEventListener('load', hideGridLoader, { capture: true, once: true });
                        gridLoaderTimer = setTimeout(hideGridLoader, 8000);
                    } else {
                        syncGridSelection();
                    }
                };

                // Esc closes the open overview overlay (mirrors the close button).
                document.addEventListener('keydown', (e) => {
                    if (e.key === 'Escape' && gridOverlay && !gridOverlay.hidden) {
                        gridOverlay.hidden = true;
                    }
                });

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
                        else if (action === 'double-page') {
                            const on = viewer.toggleDoublePage();
                            btn.setAttribute('aria-pressed', String(on));
                            btn.classList.toggle('immersive__tool-btn--active', on);
                            document.querySelector('.immersive__viewer')?.classList.toggle('is-double-page', on);
                            updateFulltextAvail();
                        }
                    });
                });

                // Native fullscreen runs on .immersive__viewer; Bootstrap appends the
                // share/cite/filter popovers to <body>, which is outside the fullscreen
                // element, so they don't paint. Re-home those popovers into the fullscreen
                // element while fullscreen is active, and restore the default on exit.
                document.addEventListener('fullscreenchange', () => {
                    document.querySelectorAll('[data-popover-element]').forEach((trigger) => {
                        const $trigger = window.$ && window.$(trigger);
                        const inst = $trigger && $trigger.data('bs.popover');
                        if (!inst) return;
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

                // (Panel open/close is bound earlier -- before loadPageServices -- so the
                // sidebar opens immediately, without waiting for the first image to load.)

                // TOC drawer: clicking an entry navigates in place (no reload) and
                // highlights that section immediately, so the click intent always wins --
                // regardless of load latency or which page of a double-page spread the
                // section starts on. Entries carry their 1-based physical page number as
                // data-page-no; entries without one fall through to normal navigation.
                const menuPanel = document.getElementById('immersivePanelMenu');
                if (menuPanel) {
                    const tocEntries = () =>
                        Array.from(menuPanel.querySelectorAll('.widget-toc__element[data-page-no]'))
                            .filter((el) => el.dataset.level !== '0') // skip the hidden record root
                            .map((el) => ({ el, no: Number(el.dataset.pageNo) }))
                            .filter((x) => Number.isFinite(x.no) && x.no >= 1);

                    const setTocActive = (el) => {
                        // Tree-view: let the widget set active + expand collapsed ancestors, so a
                        // section reached via the grid/chevrons inside a collapsed branch opens up.
                        // (Its own scroll is a no-op here -- the panel scrolls, not the list -- so
                        // we still scrollIntoView ourselves when the panel is open.)
                        if (el && el.dataset.iddoc && window.viewerJS && viewerJS.widgetToc) {
                            viewerJS.widgetToc.setActive(el.dataset.iddoc.replace('iddoc_', ''));
                            if (menuPanel.classList.contains('is-open')) el.scrollIntoView({ block: 'nearest' });
                            return;
                        }
                        menuPanel.querySelectorAll('.widget-toc__element.active, .widget-toc__element-link.active').forEach((x) => x.classList.remove('active'));
                        if (el) {
                            el.classList.add('active');
                            if (menuPanel.classList.contains('is-open')) el.scrollIntoView({ block: 'nearest' });
                        }
                    };

                    menuPanel.addEventListener('click', (e) => {
                        const link = e.target.closest('.widget-toc__element-link a');
                        if (!link) return;
                        const element = link.closest('.widget-toc__element');
                        const pageNo = element ? Number(element.dataset.pageNo) : NaN;
                        if (!Number.isFinite(pageNo) || pageNo < 1) return;
                        e.preventDefault();
                        setTocActive(element);
                        viewer.goToPage(pageNo - 1);
                    });

                    // Keep the highlight on the section the reader is in and let it follow
                    // along when paging via the chevrons/grid/search. A section owns the
                    // page range [pageNo, nextPageNo). The active section is kept while any
                    // visible page (single page, or either page of a double-page spread)
                    // still falls in its range; otherwise the section owning the last
                    // visible page takes over. Range-based, so a section starting on the
                    // right page of a spread no longer mis-picks its neighbour.
                    const syncTocActive = () => {
                        const entries = tocEntries();
                        const pages = viewer.getCurrentPages().map((p) => p + 1);
                        if (!entries.length || !pages.length) return;
                        const active = menuPanel.querySelector('.widget-toc__element.active[data-page-no]');
                        if (active) {
                            const no = Number(active.dataset.pageNo);
                            const nextNo = Math.min(Infinity, ...entries.map((x) => x.no).filter((n) => n > no));
                            if (pages.some((p) => p >= no && p < nextNo)) return;
                        }
                        const top = Math.max(...pages);
                        let best = null;
                        entries.forEach((x) => {
                            if (x.no <= top && (!best || x.no >= best.no)) best = x;
                        });
                        setTocActive(best ? best.el : null);
                    };
                    viewer.onPageChange.subscribe(syncTocActive);
                    syncTocActive();
                }
            })
            .catch((e) => console.error('immersive viewer init failed', e));
    }

    /**
     * Mounts the reused imageFilters riot tag on the viewer's live ImageView.Image so the
     * Filter popover adjusts brightness/contrast/etc. Pixel filters need an origin-clean
     * canvas (CORS); if the tiles taint it, the Filter button is hidden instead.
     */
    function mountImageFilters(viewer) {
        const btn = document.querySelector('[data-popover-element="#immersiveFilterPopover"]');
        if (!btn || !document.querySelector('imageFilters') || !window.immersiveFilterConfig) return;
        const image = viewer.viewer;
        const originClean = typeof image.isOriginClean !== 'function' || image.isOriginClean();
        if (originClean) {
            riot.mount('imageFilters', { image, config: window.immersiveFilterConfig });
        } else {
            btn.hidden = true;
        }
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
