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
 * an optional computed from-to register. All data comes from the /api/v2/archives/{pi}/cardcatalog
 * endpoints; navigation state lives in the URL query so every view is deep-linkable and browser
 * back/forward work.
 */
var viewerJS = (function (viewer, $) {
    'use strict';

    var _debug = false;

    var _stripLoadThreshold = 200;

    var _defaults = {
        pi: '',
        apiUrl: '',
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
    };

    viewer.cardCatalog = {
        info: null,
        register: null,
        state: null,
        section: null,
        stripLoading: false,
        stripPromise: null,
        stageViewer: null,
        osdPromise: null,
        renderSeq: 0,
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

            this.fetchJson(this.catalogUrl(), 'cardcatalog info')
                .then((info) => {
                    this.info = info;
                    var params = new URLSearchParams(window.location.search);
                    this.state = this.keptState(window.history.state && window.history.state.cc) || this.stateFromParams(params, info);
                    this.renderSidebar();
                    this.render();
                    if (info.mode === 'REGISTER') {
                        this.loadRegister();
                    }
                    if (!this.state.drawer && params.get('selected')) {
                        this.openSelectedEntry(params.get('selected'));
                    }
                })
                .catch((error) => {
                    console.error('ERROR - could not initialize card catalog: ', error);
                    this.$content.html(`<div class="archives-cardcatalog__error">${this.msg('errOccurred')}</div>`);
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

        /* ---------- state ---------- */

        /**
         * Derives the navigation state from URL query parameters. Exposed as a pure function for tests.
         * @param params URLSearchParams of the current location
         * @param info bootstrap info of the catalog (for drawer validation)
         * @returns state object {drawer, from, to, card, chain}
         */
        stateFromParams: function (params, info) {
            var state = { drawer: null, from: null, to: null, card: null, chain: [] };
            var drawerId = params.get('drawer');
            var drawer = drawerId && info ? info.drawers.find((d) => d.id === drawerId) : null;
            if (drawer) {
                state.drawer = drawerId;
                var from = parseInt(params.get('from'), 10);
                var to = Math.min(parseInt(params.get('to'), 10), drawer.entryCount);
                if (from > 0 && to >= from) {
                    state.from = from;
                    state.to = to;
                    state.chain = [{ from: 1, to: drawer.entryCount }];
                }
                var card = parseInt(params.get('card'), 10);
                if (card > 0 && card <= drawer.entryCount) {
                    state.card = card;
                }
            }
            return state;
        },

        /**
         * Accepts a restored history.state entry only if its drawer still exists in the loaded
         * info — persisted entries can predate a reindex. Falls back to null (→ URL parameters).
         */
        keptState: function (state) {
            return state && state.drawer && this.info.drawers.some((d) => d.id === state.drawer) ? state : null;
        },

        /**
         * Serializes the navigation state into URL query parameters, preserving unrelated parameters.
         * The chain is intentionally not serialized: it lives in history.state only, and
         * stateFromParams synthesizes its root step for plain deep links. Pure, for tests.
         */
        paramsFromState: function (state, params) {
            ['drawer', 'from', 'to', 'card'].forEach((key) => params.delete(key));
            if (state.drawer) {
                params.set('drawer', state.drawer);
                if (state.from && state.to) {
                    params.set('from', state.from);
                    params.set('to', state.to);
                }
                if (state.card) {
                    params.set('card', state.card);
                }
            }
            return params;
        },

        /**
         * Writes the current state into the URL: push creates a history entry (user navigation),
         * replace only syncs the current one. A resolved ?selected= deep link is always dropped.
         */
        syncUrl: function (push) {
            var params = this.paramsFromState(this.state, new URLSearchParams(window.location.search));
            params.delete('selected');
            var query = params.toString();
            var url = window.location.pathname + (query ? '?' + query : '') + window.location.hash;
            history[push ? 'pushState' : 'replaceState']({ cc: this.state }, '', url);
        },

        /**
         * Full view window of the configured size centered around a card, clamped to the drawer.
         */
        windowAround: function (position, drawer) {
            var size = this.info.config.fullViewThreshold;
            var from = Math.max(1, Math.min(position - Math.floor(size / 2), drawer.entryCount - size + 1));
            return { from: from, to: Math.min(from + size - 1, drawer.entryCount) };
        },

        openSelectedEntry: function (entryId) {
            fetch(this.catalogUrl() + 'lookup?entryId=' + encodeURIComponent(entryId))
                .then((response) => (response.ok ? response.json() : null))
                .then((location) => {
                    if (this.state.drawer) {
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
                .catch((error) => console.error('ERROR - could not resolve selected card catalog entry: ', error));
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
            var section = this.section;
            var expectedCard = this.state.card;
            var step = this.info.config.listPageSize;
            var loading = delta > 0 ? this.extendStrip(this.section.to + 1, this.section.to + step) : this.extendStrip(this.section.from - step, this.section.from - 1);
            loading.then(() => {
                if (this.section === section && this.state.card === expectedCard) {
                    this.selectCard(target);
                }
            });
        },

        /* ---------- data access ---------- */

        catalogUrl: function () {
            return this.config.apiUrl + '/archives/' + encodeURIComponent(this.config.pi) + '/cardcatalog/';
        },

        fetchJson: function (url, what) {
            return fetch(url).then((response) => {
                if (!response.ok) {
                    throw new Error(what + ' request failed: ' + response.status);
                }
                return response.json();
            });
        },

        rootUrl: function () {
            return this.config.apiUrl.replace(/\/api\/v2$/, '');
        },

        drawerById: function (drawerId) {
            return this.info.drawers.find((d) => d.id === drawerId) || null;
        },

        currentDrawer: function () {
            return this.drawerById(this.state.drawer);
        },

        loadRegister: function () {
            fetch(this.catalogUrl() + 'register')
                .then((response) => (response.ok ? response.json() : null))
                .then((register) => {
                    if (register && register.available) {
                        this.register = register;
                        this.renderRegister();
                    }
                })
                .catch((error) => console.error('ERROR - could not load card catalog register: ', error));
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
                    return `<button type="button" class="archives-cardcatalog__drawer${active}" data-drawer="${this.escape(drawer.id)}" title="${this.formatNumber(drawer.entryCount)} ${this.msg('cards')}">${this.escape(drawer.label)}</button>`;
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
                this.$register.empty();
                return;
            }
            var html = `<h3 class="archives-cardcatalog__sidebar-heading">${this.msg('register')}</h3>`;
            html += drawer.ranges
                .map((range) => {
                    var active = this.state.from === range.from && this.state.to === range.to ? ' -active' : '';
                    return `<button type="button" class="archives-cardcatalog__register-range${active}" data-drawer="${this.escape(drawer.id)}" data-from="${Number(range.from)}" data-to="${Number(range.to)}">${this.escape(range.fromLabel)} &ndash; ${this.escape(range.toLabel)}</button>`;
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
            this.$drawers.find('[data-drawer]').removeClass('-active');
            if (this.state.drawer) {
                this.$drawers.find('[data-drawer="' + $.escapeSelector(this.state.drawer) + '"]').addClass('-active');
            }
            if (!this.state.drawer) {
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
            var token = ++this.renderSeq;
            this.showLoader();
            this.fetchJson(`${this.catalogUrl()}drawers/${encodeURIComponent(drawer.id)}/boundaries?from=${from}&to=${to}`, 'boundaries')
                .then((section) => {
                    if (token !== this.renderSeq) {
                        return;
                    }
                    if (!section.items || !section.items.length) {
                        this.$content.html(`<div class="archives-cardcatalog__error">${this.msg('errOccurred')}</div>`);
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
                    console.error('ERROR - could not load card catalog boundaries: ', error);
                    if (token === this.renderSeq) {
                        this.$content.html(`<div class="archives-cardcatalog__error">${this.msg('errOccurred')}</div>`);
                    }
                });
        },

        /**
         * A boundary card in the vertical zoom list: shows the IIIF head crop of the scan (the
         * ordering word) at readable size so the card content itself decides where to zoom in.
         */
        renderZoomCard: function (item) {
            var src = this.headCropUrl(item.imageUrl) || item.thumbnailUrl;
            var image = src
                ? `<img loading="lazy" src="${this.escape(src)}" alt="${this.escape(item.label || '')}"/>`
                : `<span class="archives-cardcatalog__no-image">${this.msg('noImage')}</span>`;
            return (
                `<figure class="archives-cardcatalog__zoomcard" data-open="${Number(item.position)}" role="button" tabindex="0" title="${this.msg('fullView')}">` +
                `${image}<figcaption>` +
                `<span class="archives-cardcatalog__card-position">${this.msg('card')} ${this.formatNumber(item.position)}</span>` +
                `<span class="archives-cardcatalog__card-title">${this.escape(item.label || '')}</span>` +
                `</figcaption></figure>`
            );
        },

        renderFullView: function (drawer, from, to) {
            var token = ++this.renderSeq;
            this.showLoader();
            this.fetchJson(`${this.catalogUrl()}drawers/${encodeURIComponent(drawer.id)}/entries?from=${from}&to=${to}`, 'entries')
                .then((section) => {
                    if (token !== this.renderSeq) {
                        return;
                    }
                    if (!section.items || !section.items.length) {
                        this.$content.html(`<div class="archives-cardcatalog__error">${this.msg('errOccurred')}</div>`);
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
                        `<div class="archives-cardcatalog__stage-body"><div class="archives-cardcatalog__stage-viewer" id="ccStageViewer"></div></div>` +
                        `<div class="archives-cardcatalog__stage-caption"><div class="archives-cardcatalog__stage-caption-text" data-cc-caption="true"></div>` +
                        `<div class="archives-cardcatalog__stage-actions">` +
                        `<button type="button" class="archives-cardcatalog__stage-action" data-shift="-1" title="${this.msg('previous')}">${this.iconSvg('chevron-left')}</button>` +
                        `<button type="button" class="archives-cardcatalog__stage-action" data-shift="1" title="${this.msg('next')}">${this.iconSvg('chevron-right')}</button>` +
                        `</div></div></div>`;
                    var strip = `<div class="archives-cardcatalog__strip" data-cc-strip="true"></div>`;
                    this.$content.html(`<div class="archives-cardcatalog__fullview">${header}<div class="archives-cardcatalog__stage-row">${stage}${strip}</div></div>`);
                    this.bindContentEvents();
                    this.updateStage(current);
                })
                .catch((error) => {
                    console.error('ERROR - could not load card catalog entries: ', error);
                    if (token === this.renderSeq) {
                        this.$content.html(`<div class="archives-cardcatalog__error">${this.msg('errOccurred')}</div>`);
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
                $strip.find('.-current').removeClass('-current');
                $strip.find('[data-stage="' + item.position + '"]').addClass('-current');
            }
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

        renderStripThumb: function (item, currentPosition) {
            var current = item.position === currentPosition ? ' -current' : '';
            var image = item.thumbnailUrl
                ? `<img loading="lazy" src="${this.escape(item.thumbnailUrl)}" alt="${this.escape(item.label || '')}"/>`
                : '<span class="archives-cardcatalog__no-image"></span>';
            return (
                `<button type="button" class="archives-cardcatalog__strip-thumb${current}" data-stage="${Number(item.position)}" title="${this.escape(item.label || '')}">` +
                `${image}<span class="archives-cardcatalog__strip-position">${this.formatNumber(item.position)}</span>` +
                `</button>`
            );
        },

        /**
         * Clamps a requested strip extension to the drawer bounds and trims the part that is
         * already loaded. Returns null when nothing is left to load. Pure, for tests.
         */
        clampExtension: function (section, from, to, entryCount) {
            from = Math.max(1, from);
            to = Math.min(entryCount, to);
            if (from >= section.from && from <= section.to) {
                from = section.to + 1;
            }
            if (to >= section.from && to <= section.to) {
                to = section.from - 1;
            }
            return to < from ? null : { from: from, to: to };
        },

        /**
         * Extends the loaded strip contiguously at either end while the section object keeps
         * spanning everything that is loaded; prepending compensates the scroll position.
         */
        extendStrip: function (from, to) {
            if (!this.section) {
                return Promise.resolve();
            }
            if (this.stripLoading && this.stripPromise) {
                var pending = this.section;
                return this.stripPromise.then(() => (this.section === pending ? this.extendStrip(from, to) : undefined));
            }
            var drawer = this.currentDrawer();
            var section = this.section;
            var range = this.clampExtension(section, from, to, drawer.entryCount);
            if (!range) {
                return Promise.resolve();
            }
            from = range.from;
            to = range.to;
            this.stripLoading = true;
            this.stripPromise = this.fetchJson(`${this.catalogUrl()}drawers/${encodeURIComponent(drawer.id)}/entries?from=${from}&to=${to}`, 'entries')
                .then((chunk) => {
                    if (this.section !== section) {
                        return;
                    }
                    this.stripLoading = false;
                    var items = chunk.items || [];
                    if (!items.length) {
                        return;
                    }
                    var $strip = this.$content.find('[data-cc-strip]');
                    var strip = $strip[0];
                    var html = items.map((item) => this.renderStripThumb(item, this.state.card)).join('');
                    if (from > section.to) {
                        section.items = section.items.concat(items);
                        section.to = from + items.length - 1;
                        $strip.append(html);
                    } else {
                        var beforeHeight = strip.scrollHeight;
                        var beforeWidth = strip.scrollWidth;
                        section.items = items.concat(section.items);
                        section.from = to - items.length + 1;
                        $strip.prepend(html);
                        strip.scrollTop += strip.scrollHeight - beforeHeight;
                        strip.scrollLeft += strip.scrollWidth - beforeWidth;
                    }
                    $strip.attr('data-range', section.from + '-' + section.to);
                })
                .catch((error) => {
                    if (this.section === section) {
                        this.stripLoading = false;
                    }
                    console.error('ERROR - could not extend card catalog strip: ', error);
                });
            return this.stripPromise;
        },

        /**
         * Scrolling near either end of the strip loads the adjacent cards — the strip behaves
         * like the whole drawer instead of a paged window.
         */
        onStripScroll: function () {
            if (!this.section || this.stripLoading) {
                return;
            }
            var strip = this.$content.find('[data-cc-strip]')[0];
            if (!strip) {
                return;
            }
            var step = this.info.config.listPageSize;
            var vertical = !getComputedStyle(strip).flexDirection.startsWith('row');
            var position = vertical ? strip.scrollTop : strip.scrollLeft;
            var extent = vertical ? strip.clientHeight : strip.clientWidth;
            var total = vertical ? strip.scrollHeight : strip.scrollWidth;
            if (position + extent > total - _stripLoadThreshold) {
                this.extendStrip(this.section.to + 1, this.section.to + step);
            } else if (position < _stripLoadThreshold) {
                this.extendStrip(this.section.from - step, this.section.from - 1);
            }
        },

        bindContentEvents: function () {
            this.$content.find('[data-chain]').on('click', (event) => {
                this.chainJump(parseInt($(event.currentTarget).attr('data-chain'), 10));
            });
            this.$content.find('[data-from]').on('click', (event) => {
                var $el = $(event.currentTarget);
                this.zoomTo(parseInt($el.attr('data-from'), 10), parseInt($el.attr('data-to'), 10));
            });
            this.$content.find('[data-open]').on('click keydown', (event) => {
                if (event.type === 'keydown' && event.key !== 'Enter' && event.key !== ' ') {
                    return;
                }
                event.preventDefault();
                this.openCardAt(parseInt($(event.currentTarget).attr('data-open'), 10));
            });
            this.$content.find('[data-shift]').on('click', (event) => {
                this.shiftCard(parseInt($(event.currentTarget).attr('data-shift'), 10));
            });
            var $strip = this.$content.find('[data-cc-strip]');
            $strip.on('click', '[data-stage]', (event) => {
                this.selectCard(parseInt($(event.currentTarget).attr('data-stage'), 10));
            });
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

        showLoader: function () {
            this.resetContent();
            this.$content.html('<div class="archives-cardcatalog__loader"></div>');
        },

        /**
         * Tears down everything the full view owns before the content area is replaced:
         * nulling the section makes the identity guards drop any in-flight strip response.
         */
        resetContent: function () {
            this.destroyStageViewer();
            this.section = null;
            this.stripLoading = false;
        },

        /* ---------- stage image ---------- */

        /**
         * Shows a card on the stage: OpenSeadragon fills the stage so the card can be zoomed
         * and panned in place. The viewer instance is kept while paging within the window.
         */
        showStageImage: function (item) {
            if (!document.getElementById('ccStageViewer')) {
                return;
            }
            var infoUrl = this.imageInfoUrl(item.imageUrl || item.thumbnailUrl);
            var renderPlainImage = () => {
                this.destroyStageViewer();
                var container = document.getElementById('ccStageViewer');
                var src = item.imageUrl || item.thumbnailUrl;
                if (container) {
                    container.innerHTML = src
                        ? `<img src="${this.escape(src)}" alt="${this.escape(item.label || '')}"/>`
                        : `<span class="archives-cardcatalog__no-image">${this.msg('noImage')}</span>`;
                }
            };
            if (!infoUrl) {
                renderPlainImage();
                return;
            }
            this.loadOpenSeadragon()
                .then(() => {
                    var container = document.getElementById('ccStageViewer');
                    if (!container) {
                        return;
                    }
                    if (this.stageViewer && !this.stageViewer.element.isConnected) {
                        this.destroyStageViewer();
                    }
                    if (this.stageViewer) {
                        this.stageViewer.removeAllHandlers('open-failed');
                        this.stageViewer.addOnceHandler('open-failed', renderPlainImage);
                        this.stageViewer.open([infoUrl]);
                        return;
                    }
                    container.innerHTML = '';
                    this.stageViewer = OpenSeadragon({
                        element: container,
                        tileSources: [infoUrl],
                        prefixUrl: '',
                        showNavigationControl: false,
                        gestureSettingsMouse: { clickToZoom: true, dblClickToZoom: true },
                        crossOriginPolicy: 'Anonymous',
                        drawer: 'canvas',
                    });
                    this.stageViewer.addOnceHandler('open-failed', renderPlainImage);
                })
                .catch(renderPlainImage);
        },

        destroyStageViewer: function () {
            if (this.stageViewer) {
                this.stageViewer.destroy();
                this.stageViewer = null;
            }
        },

        loadOpenSeadragon: function () {
            if (window.OpenSeadragon) {
                return Promise.resolve();
            }
            if (!this.config.osdScriptUrl) {
                return Promise.reject(new Error('no OpenSeadragon script url configured'));
            }
            if (!this.osdPromise) {
                this.osdPromise = new Promise((resolve, reject) => {
                    var script = document.createElement('script');
                    script.src = this.config.osdScriptUrl;
                    script.onload = resolve;
                    script.onerror = () => {
                        this.osdPromise = null;
                        reject(new Error('could not load OpenSeadragon'));
                    };
                    document.head.appendChild(script);
                });
            }
            return this.osdPromise;
        },

        /* ---------- util ---------- */

        /**
         * Translated text for a key, HTML-escaped — safe to interpolate into templates directly.
         */
        msg: function (key) {
            var bundleKey = _msgKeys[key] || key;
            return this.escape(this.config.msg[bundleKey] || bundleKey);
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
