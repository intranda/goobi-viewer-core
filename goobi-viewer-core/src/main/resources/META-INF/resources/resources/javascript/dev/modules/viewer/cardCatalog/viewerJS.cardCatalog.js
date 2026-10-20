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
 */

/**
 * Card catalog view (Zettelkastenansicht) for archives flagged as card catalogs: drawer entry
 * points in the sidebar, KatZoom-style interval zoom with clickable gaps between boundary cards,
 * a full view with an inline OpenSeadragon stage and an endlessly scrolling thumbnail strip, and
 * an optional computed from-to register. All data comes from the /api/v1/archives/{pi}/cardcatalog
 * endpoints; navigation state lives in the URL query so every view is deep-linkable and browser
 * back/forward work.
 */
var viewerJS = (function (viewer, $) {
    'use strict';

    var _debug = false;

    var _defaults = {
        pi: '',
        apiUrl: '',
        rootUrl: '',
        osdScriptUrl: '',
        msg: {},
    };

    var _msgKeys = {
        cards: 'archives__card_catalog_cards',
        card: 'archives__card_catalog_card',
        of: 'archives__card_catalog_of',
        zoomMode: 'archives__card_catalog_zoom_mode',
        zoomIn: 'archives__card_catalog_zoom_in',
        fullView: 'archives__card_catalog_full_view',
        register: 'archives__card_catalog_register',
        chooseDrawer: 'archives__card_catalog_choose_drawer',
        noImage: 'archives__card_catalog_no_image',
        openRecord: 'archives__open_record',
        previous: 'previous',
        next: 'next',
        errOccurred: 'archives__card_catalog_error',
        loading: 'archives__card_catalog_loading',
        loadMoreError: 'archives__card_catalog_load_more_error',
        retry: 'archives__card_catalog_retry',
        back: 'archives__card_catalog_back',
        pathMore: 'archives__card_catalog_path_more',
        pathLevel: 'archives__card_catalog_path_level',
        location: 'archives__card_catalog_location',
        drawer: 'archives__card_catalog_drawer',
        showFullCards: 'archives__card_catalog_show_full_cards',
        completeCards: 'archives__card_catalog_complete_cards',
        previousDrawer: 'archives__card_catalog_previous_drawer',
        nextDrawer: 'archives__card_catalog_next_drawer',
        viewCard: 'archives__card_catalog_view_card',
        letters: 'archives__card_catalog_letters',
        frontSide: 'archives__card_catalog_front_side',
        backSide: 'archives__card_catalog_back_side',
        guide: 'archives__card_catalog_guide',
        guideDrawer: 'archives__card_catalog_guide_drawer',
        guideDrawerText: 'archives__card_catalog_guide_drawer_text',
        guideLetters: 'archives__card_catalog_guide_letters',
        guideZoom: 'archives__card_catalog_guide_zoom',
        guideZoomText: 'archives__card_catalog_guide_zoom_text',
        guideZoomSteps: 'archives__card_catalog_guide_zoom_steps',
        guideZoomStepsOne: 'archives__card_catalog_guide_zoom_steps_one',
        guideRegister: 'archives__card_catalog_guide_register',
        guideRead: 'archives__card_catalog_guide_read',
        guideReadText: 'archives__card_catalog_guide_read_text',
        guideRecord: 'archives__card_catalog_guide_record',
        fullscreenEnter: 'fullscreen_enter',
        fullscreenExit: 'fullscreen_exit',
    };

    var _volumeMsgKeys = {
        cards: 'archives__volume_catalog_cards',
        card: 'archives__volume_catalog_card',
        errOccurred: 'archives__volume_catalog_error',
        loading: 'archives__volume_catalog_loading',
        loadMoreError: 'archives__volume_catalog_load_more_error',
        showFullCards: 'archives__volume_catalog_show_full_cards',
        completeCards: 'archives__volume_catalog_complete_cards',
        viewCard: 'archives__volume_catalog_view_card',
        guideZoomText: 'archives__volume_catalog_guide_zoom_text',
        guideZoomSteps: 'archives__volume_catalog_guide_zoom_steps',
        guideZoomStepsOne: 'archives__volume_catalog_guide_zoom_steps_one',
        guideRead: 'archives__volume_catalog_guide_read',
        guideRecord: 'archives__volume_catalog_guide_record',
    };

    viewer.cardCatalog = {
        info: null,
        register: null,
        registerLoaded: null,
        registerController: null,
        registerPendingDrawer: null,
        registerErrorDrawer: null,
        state: null,
        section: null,
        stripLoading: false,
        stripPromise: null,
        stripErrorRange: null,
        stageViewer: null,
        stageItem: null,
        stageSide: 'front',
        osdPromise: null,
        renderSeq: 0,
        renderController: null,
        stripController: null,
        stageSeq: 0,
        initSeq: 0,
        initController: null,
        listenersBound: false,
        showFullPreviews: false,
        overviewPositions: null,
        contentFocusPending: false,

        /**
         * Boots the catalog: binds the sidebar and content containers, loads the catalog info and renders the state read from the URL.
         */
        init: function (config) {
            if (_debug) {
                console.log('viewer.cardCatalog.init: config - ', config);
            }
            this.config = $.extend(true, {}, _defaults, config);
            this.$drawers = $('[data-cc="drawers"]');
            this.$orientation = $('[data-cc="orientation"]');
            this.$guide = $('[data-cc="guide"]');
            this.$register = $('[data-cc="register"]');
            this.$content = $('[data-cc="content"]');
            this.initNotes();
            this.initCardRatio();
            this.cancelRender();
            this.resetContent();
            if (this.initController) {
                this.initController.abort();
            }
            if (this.registerController) {
                this.registerController.abort();
                this.registerController = null;
            }
            this.initController = new AbortController();
            var sequence = ++this.initSeq;
            this.info = null;
            this.register = { drawers: [] };
            this.registerLoaded = new Set();
            this.registerPendingDrawer = null;
            this.registerErrorDrawer = null;
            this.showFullPreviews = false;
            this.overviewPositions = new Map();
            this.contentFocusPending = false;

            this.fitViewportHeight();
            if (!this.listenersBound) {
                this.listenersBound = true;
                window.addEventListener('resize', () => this.fitViewportHeight());
                window.addEventListener('popstate', (event) => {
                    if (!this.info) {
                        return;
                    }
                    this.state = this.keptState(event.state && event.state.cc) || this.stateFromParams(new URLSearchParams(window.location.search), this.info);
                    this.prepareNavigation();
                    this.renderSidebar();
                    this.render();
                });
            }

            this.fetchJson(this.catalogUrl(), 'cardcatalog info', this.initController.signal)
                .then((info) => {
                    if (sequence !== this.initSeq) {
                        return;
                    }
                    this.info = info;
                    var params = new URLSearchParams(window.location.search);
                    this.state = this.keptState(window.history.state && window.history.state.cc) || this.stateFromParams(params, info);
                    this.renderSidebar();
                    this.render();
                    if (!this.state.drawer && params.get('selected')) {
                        this.openSelectedEntry(params.get('selected'));
                    }
                })
                .catch((error) => {
                    if (sequence === this.initSeq && error.name !== 'AbortError') {
                        console.error('ERROR - could not initialize card catalog: ', error);
                        this.showError();
                    }
                });
        },

        /**
         * App-shell layout: the wrapper takes exactly the viewport height remaining below the
         * page header, so the page itself never scrolls and sidebar/content scroll independently.
         * Applies only on wide screens; the stylesheet reverts to normal page flow on narrow ones.
         */
        fitViewportHeight: function () {
            var wrapper = document.querySelector('.archives-cardcatalog__wrapper');
            if (!wrapper) {
                return;
            }
            var offset = wrapper.getBoundingClientRect().top + window.scrollY;
            wrapper.style.height = 'calc(100vh - ' + Math.max(0, Math.round(offset)) + 'px)';
        },

        /**
         * Resolves a `?selected=` entry ID through the lookup endpoint and opens that card in the full view.
         */
        openSelectedEntry: function (entryId) {
            var selected = entryId;
            var sequence = this.initSeq;
            this.fetchJson(this.catalogUrl() + 'lookup?entryId=' + encodeURIComponent(entryId), 'lookup', this.initController.signal)
                .catch((error) => (error.status === 404 ? null : Promise.reject(error)))
                .then((location) => {
                    if (sequence !== this.initSeq || this.state.drawer || new URLSearchParams(window.location.search).get('selected') !== selected) {
                        return;
                    }
                    var drawer = location ? this.drawerById(location.drawerId) : null;
                    if (!drawer) {
                        return;
                    }
                    var range = this.windowAround(location.position, drawer);
                    this.state = {
                        drawer: location.drawerId,
                        from: range.from,
                        to: range.to,
                        card: location.position,
                        chain: [{ from: 1, to: drawer.entryCount }],
                    };
                    this.syncUrl(false);
                    this.renderSidebar();
                    this.render();
                })
                .catch((error) => {
                    if (error.name !== 'AbortError') {
                        console.error('ERROR - could not resolve selected card catalog entry: ', error);
                    }
                });
        },

        /**
         * Switches to a drawer, optionally to one of its register ranges, unless that view is already shown.
         */
        selectDrawer: function (drawerId, from, to) {
            var drawer = this.drawerById(drawerId);
            if (!drawer) {
                return;
            }
            var selectionIsFullView = (to || drawer.entryCount) - (from || 1) + 1 <= this.info.config.fullViewThreshold;
            if (
                this.state &&
                this.state.drawer === drawerId &&
                this.state.from === (from || null) &&
                this.state.to === (to || null) &&
                (!this.state.card || (selectionIsFullView && this.state.card === (from || 1)))
            ) {
                return;
            }
            this.state = {
                drawer: drawerId,
                from: from || null,
                to: to || null,
                card: null,
                chain: from ? [{ from: 1, to: drawer.entryCount }] : [],
            };
            this.prepareNavigation();
            this.syncUrl(true);
            this.renderSidebar();
            this.render();
        },

        /**
         * Narrows the drawer view to the given range and pushes the previous range onto the zoom chain.
         */
        zoomTo: function (from, to) {
            if (from === (this.state.from || 1) && to === (this.state.to || this.currentDrawer().entryCount)) {
                return;
            }
            this.state.chain.push({ from: this.state.from || 1, to: this.state.to || this.currentDrawer().entryCount });
            this.state.from = from;
            this.state.to = to;
            this.state.card = null;
            this.prepareNavigation();
            this.syncUrl(true);
            this.render();
        },

        /**
         * Returns to an earlier zoom level of the chain; index -1 is the whole drawer.
         */
        chainJump: function (index) {
            var target = index < 0 ? { from: null, to: null } : this.state.chain[index];
            if (!target) {
                return;
            }
            if (target.from === this.state.from && target.to === this.state.to && this.state.card === null) {
                return;
            }
            this.state.chain = index < 0 ? [] : this.state.chain.slice(0, index);
            this.state.from = target.from;
            this.state.to = target.to;
            this.state.card = null;
            this.prepareNavigation();
            this.syncUrl(true);
            this.render();
        },

        /**
         * Jumps from a clicked boundary card straight to the full view window around that card.
         */
        openCardAt: function (position) {
            var drawer = this.currentDrawer();
            var range = this.windowAround(position, drawer);
            this.state.chain.push({ from: this.state.from || 1, to: this.state.to || drawer.entryCount });
            this.state.from = range.from;
            this.state.to = range.to;
            this.state.card = position;
            this.prepareNavigation();
            this.syncUrl(true);
            this.render();
        },

        /**
         * Switches the stage to another loaded card (thumbnail click); the URL window follows
         * the card so deep links and reloads reproduce the view.
         */
        selectCard: function (position) {
            var item = this.section ? this.section.items.find((i) => i.position === position) : null;
            if (!item) {
                if (_debug) {
                    console.warn('viewer.cardCatalog.selectCard: position not loaded - ', position);
                }
                return;
            }
            this.state.card = position;
            if (this.state.from && (position < this.state.from || position > this.state.to)) {
                var range = this.windowAround(position, this.currentDrawer());
                this.state.from = range.from;
                this.state.to = range.to;
            }
            this.syncUrl(false);
            this.updateStage(item);
        },

        /**
         * Prev/next in the viewer: pages card by card, lazily loading the neighboring cards
         * when the target is not part of the strip yet.
         */
        shiftCard: function (delta) {
            if (!this.section) {
                return;
            }
            var drawer = this.currentDrawer();
            if ((delta > 0 && this.state.card === drawer.entryCount) || (delta < 0 && this.state.card === 1)) {
                this.moveToAdjacentDrawer(delta);
                return;
            }
            var target = Math.max(1, Math.min(this.state.card + delta, drawer.entryCount));
            if (target === this.state.card) {
                return;
            }
            if (target >= this.section.from && target <= this.section.to) {
                this.selectCard(target);
                return;
            }
            if (target < this.section.from - 1 || target > this.section.to + 1) {
                var range = this.windowAround(target, drawer);
                this.state.from = range.from;
                this.state.to = range.to;
                this.state.card = target;
                this.syncUrl(false);
                this.render();
                return;
            }
            var section = this.section;
            var expectedCard = this.state.card;
            var step = this.info.config.listPageSize;
            var loading = delta > 0 ? this.extendStrip(this.section.to + 1, this.section.to + step) : this.extendStrip(this.section.from - step, this.section.from - 1);
            loading.then(() => {
                if (this.section === section && this.state.card === expectedCard && section.items.some((item) => item.position === target)) {
                    this.selectCard(target);
                }
            });
        },

        /**
         * Next (delta > 0) or previous non-empty drawer relative to the current one; null at the catalog ends.
         */
        adjacentDrawer: function (delta) {
            var direction = delta > 0 ? 1 : -1;
            var index = this.info.drawers.findIndex((drawer) => drawer.id === this.state.drawer);
            if (index < 0 || !delta) {
                return null;
            }
            for (index += direction; index >= 0 && index < this.info.drawers.length; index += direction) {
                if (this.info.drawers[index].entryCount > 0) {
                    return this.info.drawers[index];
                }
            }
            return null;
        },

        /**
         * Enters the neighboring drawer at its first or last card, depending on the direction.
         */
        moveToAdjacentDrawer: function (delta) {
            var drawer = this.adjacentDrawer(delta);
            if (!drawer) {
                return;
            }
            var position = delta > 0 ? 1 : drawer.entryCount;
            var range = this.windowAround(position, drawer);
            var fullDrawer = drawer.entryCount <= this.info.config.fullViewThreshold;
            this.state = {
                drawer: drawer.id,
                from: fullDrawer ? null : range.from,
                to: fullDrawer ? null : range.to,
                card: position,
                chain: fullDrawer ? [] : [{ from: 1, to: drawer.entryCount }],
            };
            this.prepareNavigation();
            this.syncUrl(true);
            this.renderSidebar();
            this.render();
        },

        /**
         * Base URL of the card catalog REST resource for the configured archive.
         */
        catalogUrl: function () {
            return this.config.apiUrl + '/archives/' + encodeURIComponent(this.config.pi) + '/cardcatalog/';
        },

        /**
         * GETs a JSON resource; rejects with the HTTP status for non-OK responses.
         */
        fetchJson: function (url, what, signal) {
            return fetch(url, signal ? { signal: signal } : undefined).then((response) => {
                if (!response.ok) {
                    throw Object.assign(new Error(what + ' request failed: ' + response.status), { status: response.status });
                }
                return response.json();
            });
        },

        /**
         * Viewer root URL, derived from the API URL unless configured explicitly.
         */
        rootUrl: function () {
            return this.config.rootUrl || this.config.apiUrl.replace(/\/api\/v\d+\/?$/, '');
        },

        /**
         * Drawer with the given ID from the catalog info, or null.
         */
        drawerById: function (drawerId) {
            return this.info.drawers.find((d) => d.id === drawerId) || null;
        },

        /**
         * Drawer of the current state.
         */
        currentDrawer: function () {
            return this.drawerById(this.state.drawer);
        },

        /**
         * Renders the sidebars and the content area for the current state: hint, zoom view or full view.
         */
        render: function () {
            if (!this.state.drawer || !this.showsFullView()) {
                this.renderNoteVariables(null);
            }
            this.renderOrientation();
            this.renderGuide();
            this.renderRegister();
            this.loadRegister();
            this.$drawers.find('[data-drawer]').removeClass('-active');
            this.$drawers.find('[data-drawer]').removeAttr('aria-current');
            if (this.state.drawer) {
                this.$drawers
                    .find('[data-drawer="' + $.escapeSelector(this.state.drawer) + '"]')
                    .addClass('-active')
                    .attr('aria-current', 'true');
            }
            if (!this.state.drawer) {
                this.rememberOverviewPosition();
                this.cancelRender();
                this.resetContent();
                this.$content.html(`<div class="archives-cardcatalog__hint" tabindex="-1">${this.msg('chooseDrawer')}</div>`);
                this.focusContent();
                return;
            }

            var drawer = this.currentDrawer();
            var from = this.state.from || 1;
            var to = this.state.to || drawer.entryCount;
            if (this.showsFullView()) {
                this.renderFullView(drawer, from, to);
            } else {
                this.renderZoomView(drawer, from, to);
            }
        },

        /**
         * True if the current range is small enough for the full view instead of the zoom list.
         */
        showsFullView: function () {
            var drawer = this.currentDrawer();
            return !!drawer && (this.state.to || drawer.entryCount) - (this.state.from || 1) + 1 <= this.info.config.fullViewThreshold;
        },

        /**
         * Button back to the previous zoom level; empty at the top level.
         */
        renderBack: function () {
            var last = this.state.chain.length - 1;
            if (last < 0) {
                return '';
            }
            return `<button type="button" class="archives-cardcatalog__back" data-chain="${last > 0 ? last : -1}">${this.iconSvg('zoom-out')}<span>${this.msg('back')}</span></button>`;
        },

        /**
         * Header of the content area: drawer label, live position text, the way back one zoom level, in the zoom list the preview action, and the fullscreen action.
         */
        renderContentHeader: function (drawer, from, to, fullView) {
            var position = fullView
                ? `${this.msg('card')} ${this.formatNumber(this.state.card)} ${this.msg('of')} ${this.formatNumber(drawer.entryCount)}`
                : `${this.msg('cards')} ${this.formatNumber(from)} &ndash; ${this.formatNumber(to)} ${this.msg('of')} ${this.formatNumber(drawer.entryCount)}`;
            var preview = fullView
                ? ''
                : `<button type="button" class="archives-cardcatalog__preview-toggle" data-preview-toggle="true" aria-pressed="${this.showFullPreviews}" aria-label="${this.msg('showFullCards')}" title="${this.msg('showFullCards')}">${this.msg('completeCards')}</button>`;
            return (
                `<div class="archives-cardcatalog__content-header" data-range="${from}-${to}">` +
                `<div class="archives-cardcatalog__header-main">` +
                `<div class="archives-cardcatalog__location" tabindex="-1"><strong title="${this.escape(drawer.label)}">${this.escape(drawer.label)}</strong>` +
                `<span class="archives-cardcatalog__location-position" data-cc-location="true" role="status" aria-live="polite">${position}</span></div>` +
                `</div><div class="archives-cardcatalog__navigation">${this.renderBack()}` +
                `<div class="archives-cardcatalog__header-actions">${preview}${this.renderFullscreenButton()}</div></div></div>`
            );
        },

        /**
         * Binds the click, keyboard, scroll and wheel handlers of a freshly rendered content area.
         */
        bindContentEvents: function () {
            this.$content.off('click.cardCatalogNavigation').on('click.cardCatalogNavigation', '[data-chain]', (event) => {
                this.chainJump(parseInt($(event.currentTarget).attr('data-chain'), 10));
            });
            this.$content.off('click.cardCatalogPreview').on('click.cardCatalogPreview', '[data-preview-toggle]', () => this.togglePreviews());
            this.$content.find('[data-from]').on('click', (event) => {
                var $el = $(event.currentTarget);
                this.zoomTo(parseInt($el.attr('data-from'), 10), parseInt($el.attr('data-to'), 10));
            });
            this.$content.find('[data-open]').on('click', (event) => {
                this.openCardAt(parseInt($(event.currentTarget).attr('data-open'), 10));
            });
            this.$content.find('[data-shift]').on('click', (event) => {
                this.shiftCard(parseInt($(event.currentTarget).attr('data-shift'), 10));
            });
            this.$content.find('[data-cc-fullscreen]').on('click', () => this.toggleFullscreen());
            this.$content.find('[data-cc-side]').on('click', () => this.toggleStageSide());
            $(document)
                .off('fullscreenchange.cardCatalog')
                .on('fullscreenchange.cardCatalog', () => this.syncFullscreenButton());
            $(document)
                .off('keydown.cardCatalogFullscreen')
                .on('keydown.cardCatalogFullscreen', (event) => {
                    if (
                        event.key === 'Escape' &&
                        !event.isDefaultPrevented() &&
                        this.$content.closest('.archives-cardcatalog__wrapper').hasClass('archives-cardcatalog__wrapper--fullscreen')
                    ) {
                        event.preventDefault();
                        this.toggleFullscreen();
                    }
                });
            var $strip = this.$content.find('[data-cc-strip]');
            $strip.on('click', '[data-stage]', (event) => {
                this.selectCard(parseInt($(event.currentTarget).attr('data-stage'), 10));
            });
            this.$content.find('.archives-cardcatalog__stage-caption').on('click', '[data-strip-retry]', () => this.retryStrip());
            $strip.on('scroll', () => this.onStripScroll());
            $strip.on('wheel', (event) => {
                var strip = event.currentTarget;
                var wheel = event.originalEvent;
                if (!wheel.deltaY || wheel.deltaX) {
                    return;
                }
                var horizontal = getComputedStyle(strip).flexDirection.startsWith('row');
                if (horizontal ? strip.scrollWidth <= strip.clientWidth : strip.scrollHeight <= strip.clientHeight) {
                    this.extendStripTowards(wheel.deltaY > 0 ? 1 : -1);
                    return;
                }
                if (!horizontal) {
                    return;
                }
                event.preventDefault();
                var factor = wheel.deltaMode === 1 ? 40 : wheel.deltaMode === 2 ? strip.clientWidth : 1;
                strip.scrollLeft += wheel.deltaY * factor;
            });
        },

        /**
         * Marks the next render as a navigation so the content area receives focus afterwards.
         */
        prepareNavigation: function () {
            this.contentFocusPending = true;
        },

        /**
         * After a navigation, focuses the content heading and on stacked layouts scrolls it, or a remembered page position, into view.
         */
        focusContent: function (position) {
            if (!this.contentFocusPending) {
                return;
            }
            var target = this.$content.find('.archives-cardcatalog__location, .archives-cardcatalog__hint, .archives-cardcatalog__error')[0];
            if (target) {
                target.focus({ preventScroll: true });
                var wrapper = this.$content.closest('.archives-cardcatalog__wrapper')[0] || document.querySelector('.archives-cardcatalog__wrapper');
                if (wrapper && getComputedStyle(wrapper).flexDirection === 'column') {
                    if (position && this.isFullscreen()) {
                        wrapper.scrollTop = position.page;
                    } else if (position) {
                        window.scrollTo({ top: position.page });
                    } else {
                        target.scrollIntoView({ block: 'start' });
                    }
                }
            }
            this.contentFocusPending = false;
        },

        /**
         * Invalidates the pending content render and aborts its request.
         */
        cancelRender: function () {
            this.renderSeq++;
            if (this.renderController) {
                this.renderController.abort();
                this.renderController = null;
            }
        },

        /**
         * Starts a new content render: cancels the previous one and returns its token and abort signal.
         */
        beginRender: function () {
            this.cancelRender();
            this.renderController = new AbortController();
            return { token: this.renderSeq, signal: this.renderController.signal };
        },

        /**
         * Replaces the content area with the error notice.
         */
        showError: function () {
            this.$content.html(`<div class="archives-cardcatalog__error" role="alert" tabindex="-1">${this.msg('errOccurred')}</div>`);
            this.focusContent();
        },

        /**
         * Remembers the zoom list position, tears down the full view and shows the loading indicator.
         */
        showLoader: function () {
            this.rememberOverviewPosition();
            this.resetContent();
            this.$content.html('<div class="archives-cardcatalog__loader" role="status" aria-label="' + this.msg('loading') + '"></div>');
        },

        /**
         * Tears down everything the full view owns before the content area is replaced:
         * nulling the section makes the identity guards drop any in-flight strip response.
         */
        resetContent: function () {
            this.stageSeq++;
            this.destroyStageViewer();
            if (this.stripController) {
                this.stripController.abort();
                this.stripController = null;
            }
            this.section = null;
            this.stripLoading = false;
            this.stripPromise = null;
            this.stripErrorRange = null;
        },

        /**
         * Translated text for a key, HTML-escaped — safe to interpolate into templates directly.
         */
        msg: function (key) {
            return this.escape(this.msgText(key));
        },

        /**
         * Translated text with its {0}, {1}, … placeholders replaced, HTML-escaped.
         */
        msgFormat: function (key, values) {
            return this.escape(this.msgTextFormat(key, values));
        },

        /**
         * Raw translated text with its {0}, {1}, … placeholders replaced by the given values.
         */
        msgTextFormat: function (key, values) {
            return this.msgText(key).replace(/\{(\d)\}/g, (placeholder, index) => (values[index] == null ? placeholder : String(values[index])));
        },

        /**
         * Raw translated text for a short key or bundle key, in the page wording of a volume catalog where it differs; falls back to the key itself.
         */
        msgText: function (key) {
            var volumeKey = this.config.catalogType === 'VOLUME' ? _volumeMsgKeys[key] : null;
            var bundleKey = volumeKey || _msgKeys[key] || key;
            return String(this.config.msg[bundleKey] || bundleKey);
        },

        /**
         * Number formatted for the page language.
         */
        formatNumber: function (value) {
            try {
                return Number(value).toLocaleString(document.documentElement.lang || undefined);
            } catch (e) {
                return String(Number(value));
            }
        },

        /**
         * Inline SVG markup referencing a Tabler outline icon from the viewer's icon folder.
         */
        iconSvg: function (name) {
            return `<svg class="icon-svg" aria-hidden="true" focusable="false"><use href="${this.rootUrl()}/resources/icons/outline/${name}.svg#icon"></use></svg>`;
        },

        /**
         * HTML-escapes a value for safe interpolation into markup.
         */
        escape: function (value) {
            return String(value == null ? '' : value).replace(/[&<>"']/g, function (c) {
                return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
            });
        },
    };

    return viewer;
})(viewerJS || {}, jQuery);

if (typeof module !== 'undefined' && module.exports) {
    module.exports = viewerJS;
}
