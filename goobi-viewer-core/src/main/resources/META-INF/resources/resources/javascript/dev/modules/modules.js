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
     * Page indices that make up the frame containing `order`: just the page in
     * single mode, the spread in double mode.
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

    /** Rail panel ids in toolbar order; the Alt+digit shortcuts 1-4 map onto this. */
    const PANEL_SHORTCUT_IDS = ['immersivePanelMenu', 'immersivePanelFulltext', 'immersivePanelSearch', 'immersivePanelMetadata'];

    /**
     * The rail panel an Alt+digit shortcut toggles: Alt+1-4 address the panels in
     * toolbar order (TOC, fulltext, search, metadata). Matches on `code`, not
     * `key`, because macOS Option+digit produces characters ('¡', '™', …); bare
     * digits are deliberately no shortcut (WCAG 2.1.4 discourages printable
     * single-character shortcuts) and Ctrl/Cmd combinations stay with the browser.
     * Pure + tested.
     *
     * @param {{altKey:boolean, ctrlKey:boolean, metaKey:boolean, code:string}} event
     * @returns {string|null} panel element id, or null
     */
    function panelIdForKeyEvent({ altKey, ctrlKey, metaKey, code }) {
        if (!altKey || ctrlKey || metaKey) return null;
        const index = ['Digit1', 'Digit2', 'Digit3', 'Digit4'].indexOf(code);
        return index === -1 ? null : PANEL_SHORTCUT_IDS[index];
    }

    /**
     * First visible page as 0-based order (the leading page of a double-page
     * spread), 0 when the viewer has no pages yet. Pure + tested.
     *
     * @param {{getCurrentPages?: Function}} viewer
     * @returns {number}
     */
    function currentOrder(viewer) {
        const pages = viewer.getCurrentPages ? viewer.getCurrentPages() : [];
        return pages.length ? pages[0] : 0;
    }

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

    /**
     * Runs a rAF tween over `durationMs`, calling `onFrame` with the progress 0→1
     * each frame (a non-positive duration jumps straight to 1). Resolves when done.
     */
    function _tween(durationMs, onFrame) {
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

    /** Tweens one TiledImage's opacity 0→1 while fading another 1→0 (rAF). */
    function _crossfade(incoming, outgoing, durationMs) {
        return _tween(durationMs, (t) => {
            if (incoming) incoming.setOpacity(t);
            if (outgoing) outgoing.setOpacity(1 - t);
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
            this._queuedNav = null;
            this._queuedReopen = false;
            this._navWatchdogMs = opts.navWatchdogMs ?? NAV_WATCHDOG_MS;
            this._highlights = [];
            this._fadeMs = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : FADE_MS;
            this.onPageChange = new Emitter();
            this.onLoaded = new Emitter();
            this.onOpen = new Emitter();
            this.onReset = new Emitter();

            this.viewer = new ImageView.Image({
                element: opts.element,
                fittingMode: 'fixed',
                margins: stageMargins(window.innerWidth),
                zoom: { enabled: true, max: opts.maxZoom },
                sequence: { ...SEQUENCE_DEFAULTS },
                navigator: { enabled: false },
            });
            this.viewer.openseadragon.crossOriginPolicy = 'Anonymous';
            this.zoom = new ImageView.Controls.Zoom(this.viewer);
            this.rotation = new ImageView.Controls.Rotation(this.viewer);

            this.viewer.openseadragon.addHandler('canvas-key', (e) => {
                const key = e.originalEvent.key;
                if (key === 'ArrowRight') this.next();
                else if (key === 'ArrowLeft') this.prev();
                else if (key === '0') this.resetView();
                else return;
                e.preventDefaultAction = true;
                e.originalEvent.preventDefault();
            });

            this._open(this.current)
                .then(() => {
                    this._refreshPreload();
                    this.onLoaded.emit(this.current);
                })
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

        /**
         * Draws search-hit rectangles as overlays. Rect coordinates are image pixels of
         * the page given by `rect.order`, so spread hits land on the right half of a
         * double-page view; rects for pages outside the current view are skipped.
         */
        setHighlights(rects) {
            this.clearHighlights();
            const osd = this.viewer.openseadragon;
            this._highlights = (rects || []).flatMap((r) => {
                const item = this._itemForOrder(r.order);
                if (!item) return [];
                const el = document.createElement('div');
                el.className = r.active ? 'immersive__highlight immersive__highlight--active' : 'immersive__highlight';
                osd.addOverlay({ element: el, location: item.imageToViewportRectangle(r.x, r.y, r.w, r.h) });
                return [el];
            });
        }

        /** Removes all search-hit overlays (leaves text regions untouched). */
        clearHighlights() {
            const osd = this.viewer.openseadragon;
            (this._highlights || []).forEach((el) => osd.removeOverlay(el));
            this._highlights = [];
        }

        /**
         * Draws OCR boxes (image pixels) as hoverable, id-tagged overlays and
         * returns a Map id → overlay element. A region's optional `level`
         * ('block' | 'line' | 'word') becomes a class modifier; pass regions in
         * block → line → word order so words stack on top and win pointer hits.
         * Regions without a `rect` are skipped.
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
                el.className = r.level ? `immersive__text-region immersive__text-region--${r.level}` : 'immersive__text-region';
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
            return this.double ? computeSpread(this.current, this.total) : [this.current];
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

        /**
         * Runs exactly one navigation at a time and ALWAYS releases the lock afterwards. `run`
         * returns the settle promise; `onStuck` (optional) cleans up if the watchdog fires. A
         * request that arrived while busy is honoured on release: a queued mode reopen first,
         * else the latest queued page target -- so rapid clicks converge on the last one and a
         * lost OSD 'open' can never leave the viewer permanently frozen.
         */
        _withNavLock(run, onStuck) {
            this._navigating = true;
            let released = false;
            const release = () => {
                if (released) return;
                released = true;
                clearTimeout(watchdog);
                this._navigating = false;
                if (this._queuedReopen) {
                    this._queuedReopen = false;
                    this._reopen();
                    return;
                }
                const next = this._queuedNav;
                this._queuedNav = null;
                if (next != null) this.goToPage(next);
            };
            const watchdog = setTimeout(() => {
                try {
                    if (onStuck) onStuck();
                } catch {}
                release();
            }, this._navWatchdogMs);
            try {
                Promise.resolve(run())
                    .catch(() => {})
                    .finally(release);
            } catch {
                release();
            }
        }

        /** Re-opens the current position in the current mode, serialised through the nav lock. */
        _reopen() {
            this._withNavLock(() => this._open(this.current));
        }

        zoomIn() {
            this.zoom.zoomBy(ZOOM_STEP);
        }

        zoomOut() {
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
         * over the current one, then drop the old. No reload, no blank, no jump.
         */
        _crossfadeTo(target) {
            this._withNavLock(async () => {
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
                        } catch {}
                    }).catch(() => {});
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
            const overlay = this._snapshotOverlay();
            this.current = leader;
            this._withNavLock(
                async () => {
                    await this._open(leader);
                    this._fadeOverlay(overlay);
                    this._prewarmSpreads();
                },
                // onStuck: drop the frozen snapshot if the reopen never settles.
                () => {
                    if (overlay) overlay.remove();
                }
            );
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
            } catch {
                return null;
            }
            if (getComputedStyle(host).position === 'static') host.style.position = 'relative';
            host.appendChild(overlay);
            return overlay;
        }

        /** Fades an overlay element out over `_fadeMs`, then removes it. */
        _fadeOverlay(overlay) {
            if (!overlay) return;
            _tween(this._fadeMs, (t) => {
                overlay.style.opacity = String(1 - t);
            }).then(() => overlay.remove());
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

        /**
         * Loads the page(s) for `order` via the library (a single page, or a columns:2
         * spread in double mode) and captures the single-page fit anchor. Used for the
         * initial open, mode toggles and every double-page spread change. Emits
         * `onOpen` once the library has settled, so consumers bound to the library's
         * per-load event pipeline (image filters) can re-attach.
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
                this.onOpen.emit(this.current);
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
     * Extracts ordered `{id, label}` entries from a manifest's canvases (v2 sequences/
     * canvases or v3 items). Canvases without a resolvable image-service id are skipped
     * entirely, so service URLs and page labels stay index-aligned.
     *
     * @param {object} manifest
     * @param {string} [lang] preferred label language
     * @returns {Array<{id: string, label: string}>}
     */
    function _parseManifestCanvasEntries(manifest, lang) {
        if (!manifest || typeof manifest !== 'object') return [];

        if (Array.isArray(manifest.sequences) && manifest.sequences.length > 0) {
            const canvases = manifest.sequences[0].canvases;
            if (!Array.isArray(canvases)) return [];

            const entries = [];
            for (const canvas of canvases) {
                try {
                    const id = _resolveServiceId(canvas.images[0].resource.service, '@id', 'id');
                    if (id !== null) entries.push({ id, label: _resolveCanvasLabel(canvas.label, lang) });
                } catch {}
            }
            return entries;
        }

        if (Array.isArray(manifest.items) && manifest.items.length > 0) {
            const entries = [];
            for (const canvas of manifest.items) {
                try {
                    const id = _resolveServiceId(canvas.items[0].items[0].body.service, 'id', '@id');
                    if (id !== null) entries.push({ id, label: _resolveCanvasLabel(canvas.label, lang) });
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
            return [];
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
        return nestTextLevels({ blocks, lines, words });
    }

    /** Central keydown dispatcher for the immersive view. */

    /**
     * Creates the immersive key dispatcher: one document-level keydown listener,
     * handlers consulted in ascending priority order, the first handler that
     * returns true consumes the event. This keeps the overlay precedence (modal →
     * grid → dropdown → panels) declared in one place instead of guard selectors
     * scattered over independent listeners. Pure + tested.
     *
     * @returns {{register:Function, handleEvent:Function, attach:Function}}
     */
    function createKeyDispatcher() {
        const handlers = [];
        return {
            /**
             * @param {number} priority  lower runs first
             * @param {(event: KeyboardEvent) => boolean} handler  true = consumed
             */
            register(priority, handler) {
                handlers.push({ priority, handler });
                handlers.sort((a, b) => a.priority - b.priority);
            },
            /** @returns {boolean} whether any handler consumed the event */
            handleEvent(event) {
                for (const { handler } of handlers) {
                    if (handler(event)) return true;
                }
                return false;
            },
            /** Binds the single keydown listener. */
            attach(target) {
                target.addEventListener('keydown', (e) => this.handleEvent(e));
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

    /** Left rail panel wiring: open/close, keyboard toggles, resize, TOC + metadata folds. */

    /**
     * Wires the rail buttons to their left panels (open/close with ARIA + inert
     * bookkeeping) and registers the panel keyboard shortcuts on the dispatcher:
     * Escape closes the open panel, Alt+1-4 toggles the panels in rail order.
     *
     * @param {HTMLElement|null} immersiveRoot  the .immersive root element
     * @param {{register: Function}} keys  the immersive key dispatcher
     * @returns {{closePanels: Function, registerCloseHook: Function}}
     */
    function setupPanels(immersiveRoot, keys) {
        const panelButtons = document.querySelectorAll('[data-immersive-panel]');
        document.querySelectorAll('.immersive__panel--left').forEach((p) => (p.inert = true));
        const syncPanelOpenFlag = () => {
            if (immersiveRoot) {
                immersiveRoot.classList.toggle('immersive--panel-open', !!document.querySelector('.immersive__panel--left.is-open'));
            }
        };
        const closeHooks = [];
        let activePanelBtn = null;
        const closePanels = (restoreFocus = false) => {
            closeHooks.forEach((hook) => hook());
            const closedAny = !!document.querySelector('.immersive__panel--left.is-open');
            document.querySelectorAll('.immersive__panel--left.is-open').forEach((p) => {
                p.classList.remove('is-open');
                p.setAttribute('aria-hidden', 'true');
                p.inert = true;
            });
            panelButtons.forEach((b) => {
                b.classList.remove('immersive__tool-btn--active');
                b.setAttribute('aria-expanded', 'false');
            });
            syncPanelOpenFlag();
            if (restoreFocus && closedAny && activePanelBtn) activePanelBtn.focus();
            activePanelBtn = null;
        };
        panelButtons.forEach((btn) => {
            btn.addEventListener('click', () => {
                if (btn.getAttribute('aria-disabled') === 'true') return;
                const panel = document.getElementById(btn.dataset.immersivePanel);
                if (!panel) return;
                const wasOpen = panel.classList.contains('is-open');
                closePanels();
                if (!wasOpen) {
                    panel.classList.add('is-open');
                    panel.setAttribute('aria-hidden', 'false');
                    panel.inert = false;
                    btn.classList.add('immersive__tool-btn--active');
                    btn.setAttribute('aria-expanded', 'true');
                    activePanelBtn = btn;
                    syncPanelOpenFlag();
                    // Scroll via offset math, not scrollIntoView, so the page itself never moves.
                    const active = panel.querySelector('.widget-toc__element.active');
                    if (active) {
                        panel.scrollTop = Math.max(0, offsetTopWithin(active, panel) - panel.clientHeight / 2);
                    }
                    const focusTarget = Array.from(panel.querySelectorAll('input, a[href], button')).find((n) => !n.hidden && !n.disabled && n.offsetParent !== null);
                    if (focusTarget) {
                        focusTarget.focus({ preventScroll: true });
                    } else {
                        if (!panel.hasAttribute('tabindex')) panel.setAttribute('tabindex', '-1');
                        panel.focus({ preventScroll: true });
                    }
                }
            });
        });

        const overlayOpen = () => !!document.querySelector('#immersiveGridOverlay:not([hidden]), #immersiveShortcuts:not([hidden]), #immersivePageDropdown:not([hidden])');
        keys.register(40, (e) => {
            if (e.key === 'Escape') {
                if (!document.querySelector('.immersive__panel--left.is-open')) return false;
                closePanels(true);
                return true;
            }
            const panelId = panelIdForKeyEvent(e);
            if (!panelId || overlayOpen()) return false;
            const btn = document.querySelector(`[data-immersive-panel="${panelId}"]`);
            if (!btn) return false;
            e.preventDefault();
            btn.click();
            return true;
        });

        return {
            closePanels,
            /** Registers a hook that runs whenever the panels close (e.g. fulltext teardown). */
            registerCloseHook(hook) {
                if (typeof hook === 'function') closeHooks.push(hook);
            },
        };
    }

    /**
     * Adds the drag handle that resizes the left panels: one shared
     * --immersive-panel-width custom property, persisted in localStorage.
     */
    function setupPanelResize(immersiveRoot) {
        const immersiveViewer = immersiveRoot && immersiveRoot.querySelector('.immersive__viewer');
        if (!immersiveViewer) return;
        const WIDTH_KEY = 'immersive-panel-width';
        const RAIL_WIDTH = 40;
        const MIN_WIDTH = 240;
        const MAX_WIDTH_RATIO = 0.8;
        const maxWidth = () => immersiveViewer.getBoundingClientRect().width * MAX_WIDTH_RATIO;
        const clamp = (px) => Math.min(Math.max(px, MIN_WIDTH), maxWidth());
        const applyWidth = (px) => immersiveViewer.style.setProperty('--immersive-panel-width', Math.round(px) + 'px');
        let stored = NaN;
        try {
            stored = parseInt(localStorage.getItem(WIDTH_KEY), 10);
        } catch {}
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
                } catch {}
            };
            handle.addEventListener('pointermove', onMove);
            handle.addEventListener('pointerup', onUp);
        });
    }

    /** Wires the "collapse all / expand all" toggle on the server-rendered TOC tree (shown only when the TOC nests). */
    function setupTocCollapseToggle() {
        const tocPanel = document.getElementById('immersivePanelMenu');
        const tocContainer = document.getElementById('widgetToc');
        const tocToggle = tocPanel && tocPanel.querySelector('[data-immersive-toc-toggle]');
        if (tocContainer && tocToggle && tocContainer.querySelector(".widget-toc__element[data-level='2']")) {
            tocToggle.hidden = false;
            const reflectTocToggle = () => {
                const collapsed = !!tocContainer.querySelector('.widget-toc__element--hidden');
                tocToggle.classList.toggle('immersive__toc-collapse--collapsed', collapsed);
                tocToggle.setAttribute('aria-expanded', String(!collapsed));
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
    }

    /** Wires the metadata "more / less" fold that reveals the deeper structural blocks. */
    function setupMetadataToggle() {
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
                metadataToggle.addEventListener('click', () => {
                    const willOpen = moreEl.hidden;
                    moreEl.hidden = !willOpen;
                    metadataToggle.setAttribute('aria-expanded', String(willOpen));
                    syncMetadataToggleLabel(willOpen);
                });
            }
        }
    }

    /**
     * TOC drawer: entry clicks navigate in place (entries carry their 1-based page
     * number as data-page-no), and the active-section highlight follows the
     * currently visible page(s).
     *
     * @param {IvViewer} viewer
     */
    function setupTocSync(viewer) {
        const menuPanel = document.getElementById('immersivePanelMenu');
        if (!menuPanel) return;
        const tocEntries = () =>
            Array.from(menuPanel.querySelectorAll('.widget-toc__element[data-page-no]'))
                .filter((el) => el.dataset.level !== '0') // skip the hidden record root
                .map((el) => ({ el, no: Number(el.dataset.pageNo) }))
                .filter((x) => Number.isFinite(x.no) && x.no >= 1);

        const setTocActive = (el) => {
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

        const syncTocActive = () => {
            const entries = tocEntries();
            const pages = viewer.getCurrentPages().map((p) => p + 1);
            if (!entries.length || !pages.length) return;
            const active = menuPanel.querySelector('.widget-toc__element.active[data-page-no]');
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
        viewer.onPageChange.subscribe(syncTocActive);
        syncTocActive();
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
     */
    function setupShortcutsModal(closePanels, keys) {
        const overlay = document.getElementById('immersiveShortcuts');
        const trigger = document.querySelector('[data-immersive-shortcuts-trigger]');
        if (!overlay || !trigger) return;
        // On Apple platforms modifiers are conventionally shown as symbols (⌥, ⇧);
        // the accessible name keeps the spoken key name. The symbol gets its own
        // span so the flex keycap centers the glyph instead of baseline-aligning it.
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
        const focusables = () =>
            Array.from(overlay.querySelectorAll('a[href],button,input,[tabindex]:not([tabindex="-1"])')).filter((n) => !n.hidden && !n.disabled && n.offsetParent !== null);
        const otherOverlayOpen = () =>
            !!document.querySelector('#immersiveGridOverlay:not([hidden]), #immersivePageDropdown:not([hidden]), .immersive__panel--left.is-open, .popover.show');
        const openShortcuts = () => {
            if (!overlay.hidden || otherOverlayOpen()) return;
            opener = document.activeElement;
            overlay.hidden = false;
            trigger.setAttribute('aria-expanded', 'true');
            trapHandler = (e) => {
                if (e.key !== 'Tab') return;
                const f = focusables();
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
            // A '?'-opened dialog has no focused opener (activeElement is <body>);
            // falling back to the trigger keeps keyboard users at a sensible spot.
            const restore = opener && opener !== document.body ? opener : trigger;
            opener = null;
            if (restore && typeof restore.focus === 'function') restore.focus();
        };
        trigger.addEventListener('click', () => {
            if (!overlay.hidden) {
                closeShortcuts();
                return;
            }
            if (typeof closePanels === 'function') closePanels();
            openShortcuts();
        });
        if (closeBtn) closeBtn.addEventListener('click', closeShortcuts);
        overlay.addEventListener('click', (e) => {
            if (dialog && !dialog.contains(e.target)) closeShortcuts();
        });
        keys.register(10, (e) => {
            if (e.key === 'Escape' && !overlay.hidden) {
                closeShortcuts();
                return true;
            }
            if (e.key === '?' && overlay.hidden && !e.ctrlKey && !e.metaKey && !e.altKey && !isTypingTarget(e.target) && !otherOverlayOpen()) {
                e.preventDefault();
                openShortcuts();
                return true;
            }
            return false;
        });
    }

    /** Fulltext panel wiring: combined block/line/word text with image hover linking. */

    /**
     * Wires the fulltext panel: loads the nested OCR levels for the visible page,
     * renders them, and mounts the bidirectional text ↔ image hover linking. The
     * teardown runs through the panels' close hook so leaving the panel always
     * clears the image overlays.
     *
     * @param {IvViewer} viewer
     * @param {string} pi
     * @param {string} apiBase
     * @param {{closePanels: Function, registerCloseHook: Function}} panels
     * @returns {{updateFulltextAvail: Function}}
     */
    function setupFulltextPanel(viewer, pi, apiBase, panels) {
        const fulltextPanel = document.getElementById('immersivePanelFulltext');
        const fulltextBox = document.getElementById('immersiveFulltext');
        const fulltextLoader = document.getElementById('immersiveFulltextLoader');
        const fulltextBtn = document.querySelector('[data-immersive-panel="immersivePanelFulltext"]');
        const fulltextTitleDefault = fulltextBtn ? fulltextBtn.getAttribute('title') : '';

        if (fulltextPanel && fulltextBox) {
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
            panels.registerCloseHook(clearFulltextLink);

            const loadFulltext = async () => {
                const order = currentOrder(viewer);
                const req = ++fulltextReq;
                clearFulltextLink();
                if (fulltextLoader) fulltextLoader.hidden = false;
                fulltextBox.textContent = '';
                fulltextBox.classList.remove('immersive__fulltext--empty');
                let blocks = null;
                try {
                    blocks = await loadPageTextLevels(pi, apiBase, order);
                } catch {
                    blocks = null;
                }
                if (req !== fulltextReq) return;
                if (fulltextLoader) fulltextLoader.hidden = true;
                if (blocks && blocks.length) {
                    fulltextBox.replaceChildren(buildTextLevels(blocks));
                    const flat = flattenTextLevels(blocks);
                    const regionEls = viewer.setTextRegions(flat);
                    const parents = new Map(flat.filter((r) => r.parentId).map((r) => [r.id, r.parentId]));
                    const revealIds = new Set(flat.filter((r) => r.level !== 'block').map((r) => r.id));
                    currentLink = mountTextImageLink({ box: fulltextBox, regionEls, parents, scrollContainer: fulltextPanel, revealIds });
                } else {
                    fulltextBox.textContent = fulltextBox.dataset.labelEmpty || '';
                    fulltextBox.classList.add('immersive__fulltext--empty');
                }
            };

            if (fulltextBtn) {
                fulltextBtn.addEventListener('click', () => {
                    if (fulltextPanel.classList.contains('is-open')) loadFulltext();
                });
            }
            viewer.onPageChange.subscribe(() => {
                if (fulltextPanel.classList.contains('is-open')) loadFulltext();
            });
        }

        const updateFulltextAvail = () => {
            if (!fulltextBtn) return;
            const doublePage = !!(viewer.isDoublePage && viewer.isDoublePage());
            fulltextBtn.classList.toggle('immersive__tool-btn--disabled', doublePage);
            fulltextBtn.setAttribute('aria-disabled', String(doublePage));
            fulltextBtn.setAttribute('tabindex', doublePage ? '-1' : '0');
            const title = (doublePage && fulltextBtn.dataset.labelDisabled) || fulltextTitleDefault;
            fulltextBtn.setAttribute('title', title);
            fulltextBtn.setAttribute('aria-label', title);
            if (doublePage && fulltextPanel && fulltextPanel.classList.contains('is-open')) panels.closePanels();
        };
        updateFulltextAvail();

        return { updateFulltextAvail };
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
     */
    async function search(pi, apiBase, term, fetchFn = fetch, maxPages = 50) {
        const base = `${apiBase}/records/${pi}/manifest/search?q=${encodeURIComponent(term)}`;
        const seen = new Set();
        const out = [];
        for (let p = 1; p <= maxPages; p++) {
            const res = await fetchFn(p === 1 ? base : `${base}&page=${p}`);
            if (!res.ok) break;
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
     */
    function setupFulltextSearch(viewer, pi, apiBase) {
        const searchState = { hits: [], activeIndex: -1, term: '' };
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
            viewer.setHighlights(rects);
        };

        const renderResults = () => {
            if (!resultsBox || !resultsList) return;
            resultsBox.hidden = !searchState.term;
            if (hitCounter) {
                hitCounter.textContent = searchState.hits.length
                    ? `${searchState.activeIndex + 1} / ${searchState.hits.length}`
                    : (searchState.term && resultsBox.dataset.labelEmpty) || '';
            }
            resultsList.innerHTML = '';
            if (searchState.term && !searchState.hits.length) {
                const empty = document.createElement('li');
                empty.className = 'immersive__results-empty';
                empty.textContent = resultsBox.dataset.labelEmpty || '';
                resultsList.appendChild(empty);
                return;
            }
            searchState.hits.forEach((h, i) => {
                const li = document.createElement('li');
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'immersive__results-item' + (i === searchState.activeIndex ? ' immersive__results-item--active' : '');
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

        const runSearch = async (term) => {
            searchState.term = term;
            searchState.hits = term
                ? await search(pi, apiBase, term).catch((e) => {
                      console.error('immersive fulltext search failed', e);
                      return [];
                  })
                : [];
            searchState.activeIndex = searchState.hits.length ? 0 : -1;
            renderResults();
            if (searchState.activeIndex >= 0) goToHit(0);
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

            const clearBtn = document.createElement('button');
            clearBtn.type = 'button';
            clearBtn.className = 'immersive__search-clear';
            clearBtn.textContent = '×';
            clearBtn.setAttribute('aria-label', (resultsBox && resultsBox.dataset.labelReset) || '');
            clearBtn.hidden = !searchInput.value;
            const group = searchInput.closest('.input-group') || searchInput.parentElement;
            group.insertBefore(clearBtn, group.querySelector('.input-group-addon') || null);
            const syncClear = () => {
                clearBtn.hidden = !searchInput.value;
            };
            const resetSearch = () => {
                searchInput.value = '';
                syncClear();
                runSearch('');
                searchInput.focus();
            };
            searchInput.addEventListener('input', syncClear);
            clearBtn.addEventListener('click', resetSearch);
            searchInput.addEventListener('keydown', (e) => {
                if (e.key === 'Escape' && searchInput.value) {
                    e.preventDefault();
                    e.stopPropagation();
                    resetSearch();
                }
            });
        }
        document.querySelectorAll('[data-immersive-hit]').forEach((btn) => {
            btn.addEventListener('click', () => {
                const n = searchState.hits.length;
                goToHit(btn.dataset.immersiveHit === 'next' ? nextIndex(searchState.activeIndex, n) : prevIndex(searchState.activeIndex, n));
            });
        });
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
     */
    function setupPageDropdown(viewer, labels, keys) {
        const total = viewer.getPageCount();
        const trigger = document.querySelector('[data-immersive-title-trigger]');
        const dropdown = document.getElementById('immersivePageDropdown');
        const list = document.getElementById('immersivePageList');
        const input = document.getElementById('immersivePageInput');
        if (!trigger || !dropdown || !list) return;
        if (total < 2) {
            trigger.classList.add('immersive__title-trigger--static');
            // no dropdown for single-page records: drop the popup semantics announced by the markup
            trigger.removeAttribute('aria-haspopup');
            trigger.removeAttribute('aria-expanded');
            trigger.removeAttribute('aria-controls');
            return;
        }
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
                btn.classList.toggle('immersive__page-dropdown-item--active', on);
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
            const active = list.querySelector('.immersive__page-dropdown-item--active');
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
        list.addEventListener('keydown', (e) => {
            const cur = items.indexOf(document.activeElement);
            if (cur === -1 && !['Escape'].includes(e.key)) return;
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
                case 'Escape':
                    e.preventDefault();
                    close();
                    trigger.focus();
                    break;
            }
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
        keys.register(30, (e) => {
            if (e.key !== 'Escape' || !isOpen()) return false;
            close();
            trigger.focus();
            return true;
        });
        viewer.onPageChange.subscribe(markActive);
        markActive();
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
     * @returns {function():void} toggles the overlay
     */
    function setupOverviewGrid(viewer, pi, apiBase, keys) {
        const GRID_LOADER_TIMEOUT_MS = 8000;
        const GRID_TOP_VISIBLE_AFTER_PX = 200;
        // IIIF size; must cover the largest grid step (230x330 CSS px) on HiDPI
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
        applyGridThumbSize(readThumbSizeStep(window.localStorage));
        if (gridSizeSlider) {
            gridSizeSlider.addEventListener('input', () => {
                const step = normalizeThumbSizeStep(gridSizeSlider.value);
                applyGridThumbSize(step);
                writeThumbSizeStep(window.localStorage, step);
            });
        }
        if (gridScroll && gridTopBtn) {
            gridTopBtn.addEventListener('click', () => {
                const behavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth';
                gridScroll.scrollTo({ top: 0, behavior });
            });
            gridScroll.addEventListener(
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
        let gridTag = null;
        let gridOpener = null;
        let gridTrapHandler = null;
        const gridFocusables = () =>
            Array.from(gridOverlay.querySelectorAll('a[href],button,input,[tabindex]:not([tabindex="-1"])')).filter((n) => !n.hidden && !n.disabled && n.offsetParent !== null);
        const gridActions = new rxjs.Subject();
        gridActions.subscribe((e) => {
            if (e && e.action === 'clickImage' && typeof e.value === 'number') {
                viewer.goToPage(e.value);
                closeGrid();
            }
        });
        const syncGridSelection = () => {
            if (gridTag) {
                gridTag.opts.index = currentOrder(viewer);
                gridTag.update();
            }
        };
        viewer.onPageChange.subscribe(() => {
            if (gridOverlay && !gridOverlay.hidden) syncGridSelection();
        });
        const openGrid = () => {
            gridOpener = gridTrigger || document.activeElement;
            gridOverlay.hidden = false;
            gridOverlay.setAttribute('role', 'dialog');
            gridOverlay.setAttribute('aria-modal', 'true');
            const gridLabel = gridTrigger && gridTrigger.getAttribute('aria-label');
            if (gridLabel) gridOverlay.setAttribute('aria-label', gridLabel);
            gridTrapHandler = (e) => {
                if (e.key !== 'Tab') return;
                const f = gridFocusables();
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
            gridOverlay.addEventListener('keydown', gridTrapHandler);
            if (!gridMounted) {
                gridTag = riot.mount('#immersiveThumbnails', 'thumbnails', {
                    source: `${apiBase}/records/${pi}/manifest`,
                    type: 'sequence',
                    actionlistener: gridActions,
                    imagesize: GRID_THUMB_IMAGE_SIZE,
                    index: currentOrder(viewer),
                })[0];
                gridMounted = true;
                let gridLoaderDone = false;
                let gridLoaderTimer;
                const hideGridLoader = () => {
                    if (gridLoaderDone) return;
                    gridLoaderDone = true;
                    clearTimeout(gridLoaderTimer);
                    if (gridLoader) gridLoader.hidden = true;
                };
                const thumbsMount = document.getElementById('immersiveThumbnails');
                if (thumbsMount) thumbsMount.addEventListener('load', hideGridLoader, { capture: true, once: true });
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

        keys.register(20, (e) => {
            if (e.key !== 'Escape' || !gridOverlay || gridOverlay.hidden) return false;
            closeGrid();
            return true;
        });
        return toggleGrid;
    }

    /** Bottom bar and page-status wiring: indicator, chevrons, actions, fullscreen. */

    /**
     * Wires the paging UI and the bottom-bar actions: page indicator (bottom bar +
     * title pill + canvas aria-label), chevron visibility, the action buttons
     * (zoom, rotate, reset, double page, overview, fullscreen) and the
     * fullscreen-change bookkeeping incl. re-homing Bootstrap popovers into the
     * fullscreen element.
     *
     * @param {HTMLElement} el  the [data-immersive-image] mount element
     * @param {IvViewer} viewer
     * @param {object} hooks
     * @param {Function} hooks.toggleGrid  toggles the thumbnail overview overlay
     * @param {Function} hooks.updateFulltextAvail  fulltext availability follows double-page mode
     */
    function setupBottomBar(el, viewer, { toggleGrid, updateFulltextAvail }) {
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
        viewer.onPageChange.subscribe(() => updateIndicator());

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

        const fullscreenBtn = document.querySelector('[data-immersive-action="fullscreen"]');
        document.addEventListener('fullscreenchange', () => {
            if (fullscreenBtn) {
                fullscreenBtn.setAttribute('aria-pressed', String(!!document.fullscreenElement));
                const exitLabel = fullscreenBtn.dataset.labelExit;
                const enterLabel = fullscreenBtn.dataset.labelEnter || fullscreenBtn.getAttribute('aria-label');
                const label = document.fullscreenElement && exitLabel ? exitLabel : enterLabel;
                if (label) {
                    fullscreenBtn.setAttribute('aria-label', label);
                    fullscreenBtn.setAttribute('title', label);
                }
            }
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
     */
    function bindImageFiltersMount(viewer) {
        const btn = document.querySelector(FILTER_TRIGGER_SELECTOR);
        if (!btn) return;
        const $ = window.$ || window.jQuery;
        if (!$) {
            mountImageFilters(viewer);
            return;
        }
        let mounted = false;
        $(btn).on('shown.bs.popover.immersiveFilters', () => {
            if (mounted) return;
            if (!isViewerOriginClean(viewer)) {
                $(btn).popover('hide');
                btn.hidden = true;
                return;
            }
            mounted = mountImageFilters(viewer);
            if (mounted) $(btn).popover('update');
        });
    }

    /**
     * Adds focus/keyboard management to the three immersive Bootstrap popovers (Share / Cite /
     * Filter) without touching the shared popovers controller. On open, focus moves into the
     * portaled popover; Escape inside it closes and returns focus to the trigger; aria-expanded
     * is kept in sync. Hooks only these triggers via Bootstrap's shown/hidden.bs.popover events.
     */
    function setupImmersivePopoverA11y() {
        const $ = window.$ || window.jQuery;
        if (!$) return;
        const FOCUSABLE = 'a[href],button,input,select,textarea,[tabindex]:not([tabindex="-1"])';
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

            $(trigger).on('shown.bs.popover', () => {
                trigger.setAttribute('aria-expanded', 'true');
                const pop = popoverEl();
                if (!pop) return;
                pop.addEventListener('keydown', (e) => {
                    if (e.key === 'Escape') {
                        e.preventDefault();
                        $(trigger).popover('hide');
                        trigger.focus();
                    }
                });
                const first = pop.querySelector(FOCUSABLE);
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

            $(trigger).on('hidden.bs.popover', () => {
                trigger.setAttribute('aria-expanded', 'false');
                const active = document.activeElement;
                if (!active || active === document.body) trigger.focus();
            });
        });
    }

    /**
     * Mounts the reused imageFilters riot tag on the viewer's live ImageView.Image so the
     * Filter popover adjusts brightness/contrast/etc.
     *
     * @returns {boolean} whether the tag was mounted
     */
    function mountImageFilters(viewer) {
        if (!document.querySelector('imageFilters') || !window.immersiveFilterConfig) return false;
        const [tag] = riot.mount('imageFilters', { image: viewer.viewer, config: window.immersiveFilterConfig });
        if (tag) {
            bindImageFiltersRendering(viewer, tag);
            // "Reset view" (button and '0' key) clears the image filters as well.
            viewer.onReset.subscribe(() => tag.resetAll());
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
     */
    function bindImageFiltersRendering(viewer, tag) {
        const image = viewer.viewer;
        const redraw = () => image.openseadragon.forceRedraw();
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
            const context = image.getCanvasContext();
            if (!context) return;
            let data = context.getImageData(0, 0, context.canvas.width, context.canvas.height);
            active.forEach((filter) => {
                data = filter.filterMethod(data) || data;
            });
            context.putImageData(data, 0, 0);
        };
        let boundOsd = null;
        const attach = () => {
            if (!image.openseadragon || boundOsd === image.openseadragon) return;
            boundOsd = image.openseadragon;
            boundOsd.addHandler('update-viewport', renderFilters);
        };
        attach();
        viewer.onOpen.subscribe(attach);
    }

    /** Immersive view bootstrap: engine construction plus the UI wiring modules. */

    /** Hides the stage loading indicator at the latest after this, even without a painted tile. */
    const STAGE_LOADER_TIMEOUT_MS = 8000;

    /**
     * Bootstraps the immersive image viewer: fetches the IIIF manifest, instantiates
     * the engine, and attaches the wiring modules. Keyboard shortcuts run through
     * one dispatcher whose priorities define the overlay precedence: shortcuts
     * modal (10) → grid overlay (20) → page dropdown (30) → panels (40).
     *
     * @param {HTMLElement} el  the [data-immersive-image] mount element
     */
    function initImmersiveViewer(el) {
        const pi = el.dataset.pi;
        const apiBase = el.dataset.apiBase;
        const startOrder = Number(el.dataset.startOrder) || 0;
        const maxZoom = el.dataset.maxZoom ? parseInt(el.dataset.maxZoom, 10) : undefined;
        const immersiveRoot = el.closest('.immersive');

        const keys = createKeyDispatcher();
        keys.attach(document);
        const panels = setupPanels(immersiveRoot, keys);
        setupPanelResize(immersiveRoot);
        setupTocCollapseToggle();
        setupMetadataToggle();
        setupImmersivePopoverA11y();
        setupShortcutsModal(panels.closePanels, keys);

        // Stage loading indicator (same pattern as the thumbnail grid): hidden once
        // the first tile has been painted, with a timeout and init-failure fallback.
        const stageLoader = document.getElementById('immersiveStageLoader');
        let stageLoaderDone = false;
        const hideStageLoader = () => {
            if (stageLoaderDone) return;
            stageLoaderDone = true;
            if (stageLoader) stageLoader.hidden = true;
        };
        setTimeout(hideStageLoader, STAGE_LOADER_TIMEOUT_MS);

        loadPageServices(pi, apiBase)
            .then((services) => {
                const viewer = new IvViewer({ element: el, services, startOrder, maxZoom });
                viewer.viewer.openseadragon.addOnceHandler('tile-loaded', hideStageLoader);
                window.ivViewer = viewer;
                attachUrlSync(viewer, pi);
                bindImageFiltersMount(viewer);

                loadPageLabels(pi, apiBase, fetch, document.documentElement.lang)
                    .then((labels) => setupPageDropdown(viewer, labels, keys))
                    .catch((e) => console.warn('immersive page labels failed', e));

                setupFulltextSearch(viewer, pi, apiBase);
                const fulltext = setupFulltextPanel(viewer, pi, apiBase, panels);
                const toggleGrid = setupOverviewGrid(viewer, pi, apiBase, keys);
                setupBottomBar(el, viewer, { toggleGrid, updateFulltextAvail: fulltext.updateFulltextAvail });
                setupTocSync(viewer);
            })
            .catch((e) => {
                hideStageLoader();
                console.error('immersive viewer init failed', e);
            });
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

        const immersiveEl = document.querySelector('[data-immersive-image]');
        if (immersiveEl) {
            initImmersiveViewer(immersiveEl);
        }
    });
})();
