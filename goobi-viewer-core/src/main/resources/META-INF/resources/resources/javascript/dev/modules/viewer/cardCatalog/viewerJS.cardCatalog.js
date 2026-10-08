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
        total: 'archives__card_catalog_total',
        searchArea: 'archives__card_catalog_search_area',
        interval: 'archives__card_catalog_interval',
        cards: 'archives__card_catalog_cards',
        card: 'archives__card_catalog_card',
        of: 'archives__card_catalog_of',
        zoomMode: 'archives__card_catalog_zoom_mode',
        zoomHint: 'archives__card_catalog_zoom_hint',
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
        osdPromise: null,
        renderSeq: 0,
        renderController: null,
        stripController: null,
        stageSeq: 0,
        initSeq: 0,
        initController: null,
        listenersBound: false,

        init: function (config) {
            if (_debug) {
                console.log('viewer.cardCatalog.init: config - ', config);
            }
            this.config = $.extend(true, {}, _defaults, config);
            this.$drawers = $('[data-cc="drawers"]');
            this.$counts = $('[data-cc="counts"]');
            this.$register = $('[data-cc="register"]');
            this.$content = $('[data-cc="content"]');
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

            this.fitViewportHeight();
            if (!this.listenersBound) {
                this.listenersBound = true;
                window.addEventListener('resize', () => this.fitViewportHeight());
                window.addEventListener('popstate', (event) => {
                    if (!this.info) {
                        return;
                    }
                    this.state = this.keptState(event.state && event.state.cc) || this.stateFromParams(new URLSearchParams(window.location.search), this.info);
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

        openSelectedEntry: function (entryId) {
            var selected = entryId;
            var sequence = this.initSeq;
            fetch(this.catalogUrl() + 'lookup?entryId=' + encodeURIComponent(entryId), { signal: this.initController.signal })
                .then((response) => (response.ok ? response.json() : null))
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

        selectDrawer: function (drawerId, from, to) {
            var drawer = this.drawerById(drawerId);
            if (!drawer) {
                return;
            }
            if (this.state && this.state.drawer === drawerId && this.state.from === (from || null) && this.state.to === (to || null) && !this.state.card) {
                return;
            }
            this.state = {
                drawer: drawerId,
                from: from || null,
                to: to || null,
                card: null,
                chain: from ? [{ from: 1, to: drawer.entryCount }] : [],
            };
            this.syncUrl(true);
            this.renderSidebar();
            this.render();
        },

        zoomTo: function (from, to) {
            if (from === (this.state.from || 1) && to === (this.state.to || this.currentDrawer().entryCount)) {
                return;
            }
            this.state.chain.push({ from: this.state.from || 1, to: this.state.to || this.currentDrawer().entryCount });
            this.state.from = from;
            this.state.to = to;
            this.state.card = null;
            this.syncUrl(true);
            this.render();
        },

        chainJump: function (index) {
            var target = index < 0 ? { from: null, to: null } : this.state.chain[index];
            if (target.from === this.state.from && target.to === this.state.to && this.state.card === null) {
                return;
            }
            this.state.chain = index < 0 ? [] : this.state.chain.slice(0, index);
            this.state.from = target.from;
            this.state.to = target.to;
            this.state.card = null;
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

        /* ---------- data access ---------- */

        catalogUrl: function () {
            return this.config.apiUrl + '/archives/' + encodeURIComponent(this.config.pi) + '/cardcatalog/';
        },

        fetchJson: function (url, what, signal) {
            return fetch(url, signal ? { signal: signal } : undefined).then((response) => {
                if (!response.ok) {
                    throw new Error(what + ' request failed: ' + response.status);
                }
                return response.json();
            });
        },

        rootUrl: function () {
            return this.config.rootUrl || this.config.apiUrl.replace(/\/api\/v\d+\/?$/, '');
        },

        drawerById: function (drawerId) {
            return this.info.drawers.find((d) => d.id === drawerId) || null;
        },

        currentDrawer: function () {
            return this.drawerById(this.state.drawer);
        },

        loadRegister: function () {
            if (!this.info || this.info.mode !== 'REGISTER' || !this.state.drawer) {
                if (this.registerController) {
                    this.registerController.abort();
                    this.registerController = null;
                    this.registerPendingDrawer = null;
                }
                return;
            }
            var drawerId = this.state.drawer;
            if (this.registerLoaded.has(drawerId) || this.registerPendingDrawer === drawerId) {
                return;
            }
            if (this.registerController) {
                this.registerController.abort();
            }
            var controller = new AbortController();
            this.registerController = controller;
            this.registerPendingDrawer = drawerId;
            if (this.registerErrorDrawer === drawerId) {
                this.registerErrorDrawer = null;
                this.renderRegister();
            }
            var sequence = this.initSeq;
            return this.fetchJson(this.catalogUrl() + 'drawers/' + encodeURIComponent(drawerId) + '/register', 'register', controller.signal)
                .then((register) => {
                    if (sequence !== this.initSeq || controller.signal.aborted) {
                        return;
                    }
                    if (
                        !register ||
                        typeof register.available !== 'boolean' ||
                        (register.available && (!Array.isArray(register.drawers) || register.drawers.length !== 1 || register.drawers[0].id !== drawerId))
                    ) {
                        throw new Error('invalid register response for drawer ' + drawerId);
                    }
                    this.registerLoaded.add(drawerId);
                    if (register.available) {
                        this.register.drawers.push(register.drawers[0]);
                        if (this.state.drawer === drawerId) {
                            this.renderRegister();
                        }
                    }
                })
                .catch((error) => {
                    if (error.name !== 'AbortError') {
                        console.error('ERROR - could not load card catalog register: ', error);
                        if (sequence === this.initSeq && !controller.signal.aborted) {
                            this.registerErrorDrawer = drawerId;
                            if (this.state.drawer === drawerId) {
                                this.renderRegister();
                            }
                        }
                    }
                })
                .finally(() => {
                    if (this.registerController === controller) {
                        this.registerController = null;
                        this.registerPendingDrawer = null;
                    }
                });
        },

        /**
         * Derives the KatZoom-style "Kurzanzeige" of a card from its IIIF image URL: the top part
         * of the scan (which carries the ordering word and the catalog entry) at full width, so
         * the zoom list stays readable without the scroll cost of full cards. Pure, for tests.
         */
        headCropUrl: function (imageUrl) {
            if (!imageUrl || !/\/full\/![0-9]+,[0-9]+\//.test(imageUrl)) {
                return null;
            }
            return imageUrl.replace(/\/full\/![0-9]+,[0-9]+\//, '/pct:0,0,100,40/!1200,480/');
        },

        /**
         * Derives the IIIF image info URL from a scaled image URL delivered by the API
         * (…/full/!400,400/0/default.jpg → …/info.json). Exposed as a pure function for tests.
         */
        imageInfoUrl: function (imageUrl) {
            if (!imageUrl) {
                return null;
            }
            var index = imageUrl.lastIndexOf('/full/');
            if (index < 0) {
                return null;
            }
            return imageUrl.substring(0, index) + '/info.json';
        },

        /* ---------- rendering: sidebars ---------- */

        renderSidebar: function () {
            var html = this.info.drawers
                .map((drawer) => {
                    var active = this.state.drawer === drawer.id ? ' -active' : '';
                    var current = active ? ' aria-current="true"' : '';
                    return `<button type="button" class="archives-cardcatalog__drawer${active}" data-drawer="${this.escape(drawer.id)}" title="${this.formatNumber(drawer.entryCount)} ${this.msg('cards')}"${current}>${this.escape(drawer.label)}</button>`;
                })
                .join('');
            this.$drawers.html(html);
            this.$drawers.find('[data-drawer]').on('click', (event) => {
                this.selectDrawer($(event.currentTarget).attr('data-drawer'));
            });
        },

        renderCounts: function () {
            var html = `${this.msg('total')}: <b>${this.formatNumber(this.info.totalEntries)}</b>`;
            var drawer = this.state.drawer ? this.currentDrawer() : null;
            if (drawer) {
                html += `<br/>${this.msg('searchArea')}: <b>${this.escape(drawer.label)}</b> (${this.formatNumber(drawer.entryCount)})`;
                if (this.state.from && this.state.to) {
                    html += `<br/>${this.msg('interval')}: <b>${this.formatNumber(this.state.from)} &ndash; ${this.formatNumber(this.state.to)}</b>`;
                }
            }
            this.$counts.html(html);
        },

        renderRegister: function () {
            var drawer = this.register && this.state.drawer ? this.register.drawers.find((d) => d.id === this.state.drawer) : null;
            if (!drawer) {
                if (this.state.drawer && this.registerErrorDrawer === this.state.drawer) {
                    this.$register.html(
                        `<div class="archives-cardcatalog__register-error" role="alert">${this.msg('errOccurred')} ` +
                            `<button type="button" class="archives-cardcatalog__register-retry" data-register-retry="true">${this.msg('retry')}</button></div>`
                    );
                    this.$register.find('[data-register-retry]').on('click', () => this.loadRegister());
                } else {
                    this.$register.empty();
                }
                return;
            }
            var html = `<h3 class="archives-cardcatalog__sidebar-heading">${this.msg('register')}</h3>`;
            html += drawer.ranges
                .map((range) => {
                    var active = this.state.from === range.from && this.state.to === range.to ? ' -active' : '';
                    var current = active ? ' aria-current="true"' : '';
                    return `<button type="button" class="archives-cardcatalog__register-range${active}" data-drawer="${this.escape(drawer.id)}" data-from="${Number(range.from)}" data-to="${Number(range.to)}"${current}>${this.escape(range.fromLabel)} &ndash; ${this.escape(range.toLabel)}</button>`;
                })
                .join('');
            this.$register.html(html);
            this.$register.find('[data-drawer]').on('click', (event) => {
                var $el = $(event.currentTarget);
                this.selectDrawer($el.attr('data-drawer'), parseInt($el.attr('data-from'), 10), parseInt($el.attr('data-to'), 10));
            });
        },

        /* ---------- rendering: content ---------- */

        render: function () {
            this.renderCounts();
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
                this.cancelRender();
                this.resetContent();
                this.$content.html(`<div class="archives-cardcatalog__hint">${this.msg('chooseDrawer')}</div>`);
                return;
            }

            var drawer = this.currentDrawer();
            var from = this.state.from || 1;
            var to = this.state.to || drawer.entryCount;
            if (to - from + 1 <= this.info.config.fullViewThreshold) {
                this.renderFullView(drawer, from, to);
            } else {
                this.renderZoomView(drawer, from, to);
            }
        },

        renderBreadcrumb: function (drawer, from, to) {
            var separator = '<span class="archives-cardcatalog__chip-separator">&rsaquo;</span>';
            var rootLabel = `${this.escape(drawer.label)} (1 &ndash; ${this.formatNumber(drawer.entryCount)})`;
            var html =
                this.state.chain.length === 0
                    ? `<span class="archives-cardcatalog__chip -current">${rootLabel}</span>`
                    : `<button type="button" class="archives-cardcatalog__chip" data-chain="-1">${rootLabel}</button>`;
            this.state.chain.slice(1).forEach((step, index) => {
                html += `${separator}<button type="button" class="archives-cardcatalog__chip" data-chain="${index + 1}">${this.formatNumber(step.from)} &ndash; ${this.formatNumber(step.to)}</button>`;
            });
            if (this.state.chain.length > 0) {
                html += `${separator}<span class="archives-cardcatalog__chip -current">${this.formatNumber(from)} &ndash; ${this.formatNumber(to)}</span>`;
            }
            return `<nav class="archives-cardcatalog__breadcrumb">${html}</nav>`;
        },

        renderZoomView: function (drawer, from, to) {
            var request = this.beginRender();
            this.showLoader();
            this.fetchJson(`${this.catalogUrl()}drawers/${encodeURIComponent(drawer.id)}/boundaries?from=${from}&to=${to}`, 'boundaries', request.signal)
                .then((section) => {
                    if (request.token !== this.renderSeq) {
                        return;
                    }
                    if (!section.items || !section.items.length) {
                        this.showError();
                        return;
                    }
                    var header =
                        `<div class="archives-cardcatalog__content-header">${this.renderBreadcrumb(drawer, from, to)}` +
                        `<div class="archives-cardcatalog__mode-label">${this.msg('zoomMode')} &middot; ` +
                        `${this.formatNumber(to - from + 1)} ${this.msg('cards')} &middot; ${this.msg('zoomHint')}</div></div>`;
                    var items = section.items
                        .map((item, index) => {
                            var gap = '';
                            if (index < section.items.length - 1) {
                                var next = section.items[index + 1];
                                gap =
                                    `<button type="button" class="archives-cardcatalog__gap" data-from="${Number(item.position)}" data-to="${Number(next.position)}">` +
                                    `<span class="archives-cardcatalog__gap-icon" aria-hidden="true">&raquo;&laquo;</span>` +
                                    `<span class="archives-cardcatalog__gap-label">${this.msg('zoomIn')}</span>` +
                                    `<span class="archives-cardcatalog__gap-count">${this.msg('cards')} ${this.formatNumber(item.position)} &ndash; ${this.formatNumber(next.position)}</span>` +
                                    `</button>`;
                            }
                            return `<div class="archives-cardcatalog__zoomitem">${this.renderZoomCard(item)}${gap}</div>`;
                        })
                        .join('');
                    this.$content.html(`${header}<div class="archives-cardcatalog__zoomlist">${items}</div>`);
                    this.bindContentEvents();
                })
                .catch((error) => {
                    if (request.token === this.renderSeq && error.name !== 'AbortError') {
                        console.error('ERROR - could not load card catalog boundaries: ', error);
                        this.showError();
                    }
                });
        },

        /**
         * A boundary card in the vertical zoom list: shows the IIIF head crop of the scan (the
         * ordering word) at readable size so the card content itself decides where to zoom in.
         */
        renderZoomCard: function (item) {
            var src = this.headCropUrl(item.imageUrl) || item.thumbnailUrl;
            var image = src ? `<img loading="lazy" src="${this.escape(src)}" alt=""/>` : `<span class="archives-cardcatalog__no-image">${this.msg('noImage')}</span>`;
            return (
                `<button type="button" class="archives-cardcatalog__zoomcard" data-open="${Number(item.position)}" title="${this.msg('fullView')}">` +
                `${image}<span class="archives-cardcatalog__zoomcard-caption">` +
                `<span class="archives-cardcatalog__card-position">${this.msg('card')} ${this.formatNumber(item.position)}</span>` +
                `<span class="archives-cardcatalog__card-title">${this.escape(item.label || '')}</span>` +
                `</span></button>`
            );
        },

        renderFullView: function (drawer, from, to) {
            var request = this.beginRender();
            this.showLoader();
            this.fetchJson(`${this.catalogUrl()}drawers/${encodeURIComponent(drawer.id)}/entries?from=${from}&to=${to}`, 'entries', request.signal)
                .then((section) => {
                    if (request.token !== this.renderSeq) {
                        return;
                    }
                    if (!section.items || !section.items.length) {
                        this.showError();
                        return;
                    }
                    this.section = section;
                    this.stripLoading = false;
                    if (!this.state.card || this.state.card < from || this.state.card > to) {
                        this.state.card = from;
                    }
                    this.syncUrl(false);
                    var current = section.items.find((i) => i.position === this.state.card) || section.items[0];
                    var header = `<div class="archives-cardcatalog__content-header">${this.renderBreadcrumb(drawer, from, to)}<div class="archives-cardcatalog__mode-label">${this.msg('fullView')}</div></div>`;
                    var stage =
                        `<div class="archives-cardcatalog__stage">` +
                        `<div class="archives-cardcatalog__stage-body"><div class="archives-cardcatalog__stage-viewer" id="ccStageViewer" role="img"></div></div>` +
                        `<div class="archives-cardcatalog__stage-caption"><div class="archives-cardcatalog__stage-caption-text" data-cc-caption="true"></div>` +
                        `<div class="archives-cardcatalog__stage-actions">` +
                        `<button type="button" class="archives-cardcatalog__stage-action" data-shift="-1" title="${this.msg('previous')}" aria-label="${this.msg('previous')}">${this.iconSvg('chevron-left')}</button>` +
                        `<button type="button" class="archives-cardcatalog__stage-action" data-shift="1" title="${this.msg('next')}" aria-label="${this.msg('next')}">${this.iconSvg('chevron-right')}</button>` +
                        `</div></div></div>`;
                    var strip = `<div class="archives-cardcatalog__strip" data-cc-strip="true"></div>`;
                    this.$content.html(`<div class="archives-cardcatalog__fullview">${header}<div class="archives-cardcatalog__stage-row">${stage}${strip}</div></div>`);
                    this.bindContentEvents();
                    this.updateStage(current);
                })
                .catch((error) => {
                    if (request.token === this.renderSeq && error.name !== 'AbortError') {
                        console.error('ERROR - could not load card catalog entries: ', error);
                        this.showError();
                    }
                });
        },

        /**
         * Fills the stage of the full view for the selected card: caption, thumbnail strip,
         * prev/next states and the OpenSeadragon image.
         */
        updateStage: function (item) {
            var entryCount = this.currentDrawer().entryCount;
            var titleHtml = this.escape(item.label || '');
            if (item.recordPi) {
                titleHtml = `<a class="archives-cardcatalog__stage-record" href="${this.rootUrl()}/object/${encodeURIComponent(item.recordPi)}/1/" title="${this.msg('openRecord')}">${titleHtml}${this.iconSvg('external-link')}</a>`;
            }
            this.$content
                .find('[data-cc-caption]')
                .html(
                    `<h3 class="archives-cardcatalog__stage-title">${titleHtml}</h3>` +
                        `<div class="archives-cardcatalog__stage-position">${this.msg('card')} ${this.formatNumber(item.position)} ${this.msg('of')} ${this.formatNumber(entryCount)}</div>`
                );

            var $strip = this.$content.find('[data-cc-strip]');
            var range = this.section.from + '-' + this.section.to;
            if ($strip.attr('data-range') !== range) {
                $strip.attr('data-range', range).html(this.section.items.map((i) => this.renderStripThumb(i, item.position)).join(''));
            } else {
                $strip.find('.-current').removeClass('-current').removeAttr('aria-current');
                $strip
                    .find('[data-stage="' + item.position + '"]')
                    .addClass('-current')
                    .attr('aria-current', 'true');
            }
            this.$content.find('#ccStageViewer').attr('aria-label', `${this.msgText('card')} ${this.formatNumber(item.position)}: ${item.label || ''}`);
            var currentThumb = $strip.find('[data-stage="' + item.position + '"]')[0];
            if (currentThumb) {
                $strip[0].scrollTop = currentThumb.offsetTop - ($strip[0].clientHeight - currentThumb.clientHeight) / 2;
                $strip[0].scrollLeft = currentThumb.offsetLeft - ($strip[0].clientWidth - currentThumb.clientWidth) / 2;
            }

            this.$content.find('[data-shift="-1"]').prop('disabled', item.position <= 1);
            this.$content.find('[data-shift="1"]').prop('disabled', item.position >= entryCount);

            if (this.state.from && this.state.to) {
                this.$content.find('.archives-cardcatalog__chip.-current').html(`${this.formatNumber(this.state.from)} &ndash; ${this.formatNumber(this.state.to)}`);
            }

            this.showStageImage(item);
        },

        bindContentEvents: function () {
            this.$content.find('[data-chain]').on('click', (event) => {
                this.chainJump(parseInt($(event.currentTarget).attr('data-chain'), 10));
            });
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
            var $strip = this.$content.find('[data-cc-strip]');
            $strip.on('click', '[data-stage]', (event) => {
                this.selectCard(parseInt($(event.currentTarget).attr('data-stage'), 10));
            });
            this.$content.find('.archives-cardcatalog__stage-caption').on('click', '[data-strip-retry]', () => this.retryStrip());
            $strip.on('scroll', () => this.onStripScroll());
            $strip.on('wheel', (event) => {
                var strip = event.currentTarget;
                var wheel = event.originalEvent;
                var horizontal = getComputedStyle(strip).flexDirection.startsWith('row') && strip.scrollWidth > strip.clientWidth;
                if (!wheel.deltaY || wheel.deltaX || !horizontal) {
                    return;
                }
                event.preventDefault();
                var factor = wheel.deltaMode === 1 ? 40 : wheel.deltaMode === 2 ? strip.clientWidth : 1;
                strip.scrollLeft += wheel.deltaY * factor;
            });
        },

        cancelRender: function () {
            this.renderSeq++;
            if (this.renderController) {
                this.renderController.abort();
                this.renderController = null;
            }
        },

        beginRender: function () {
            this.cancelRender();
            this.renderController = new AbortController();
            return { token: this.renderSeq, signal: this.renderController.signal };
        },

        showError: function () {
            this.$content.html(`<div class="archives-cardcatalog__error" role="alert">${this.msg('errOccurred')}</div>`);
        },

        showLoader: function () {
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

        /* ---------- util ---------- */

        /**
         * Translated text for a key, HTML-escaped — safe to interpolate into templates directly.
         */
        msg: function (key) {
            return this.escape(this.msgText(key));
        },

        msgText: function (key) {
            var bundleKey = _msgKeys[key] || key;
            return String(this.config.msg[bundleKey] || bundleKey);
        },

        formatNumber: function (value) {
            try {
                return Number(value).toLocaleString(document.documentElement.lang || undefined);
            } catch (e) {
                return String(Number(value));
            }
        },

        iconSvg: function (name) {
            return `<svg class="icon-svg" focusable="false"><use href="${this.rootUrl()}/resources/icons/outline/${name}.svg#icon"></use></svg>`;
        },

        escape: function (value) {
            return String(value == null ? '' : value).replace(/[&<>"']/g, function (c) {
                return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
            });
        },
    };

    return viewer;
})(viewerJS || {}, jQuery);

// CommonJS export for Jest. No-op in the browser where `module` is undefined.
if (typeof module !== 'undefined' && module.exports) {
    module.exports = viewerJS;
}
