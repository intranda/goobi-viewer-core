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

var viewerJS = (function (viewer, $) {
    'use strict';

    var letterGroupThreshold = 26;
    var pathFoldThreshold = 5;

    Object.assign(viewer.cardCatalog, {
        /**
         * Loads the register of the current drawer once (register mode only), aborting a pending load for another drawer.
         */
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
         * Scrolls the active drawer button into the visible part of the sidebar.
         */
        showActiveDrawer: function () {
            var active = this.$drawers.find('[aria-current="true"]')[0];
            var body = this.$drawers.closest('.archives-cardcatalog__sidebar-body')[0];
            if (!active || !body) {
                return;
            }
            var offset = active.getBoundingClientRect().top - body.getBoundingClientRect().top;
            if (offset < 0 || offset + active.offsetHeight > body.clientHeight) {
                body.scrollTop += offset - (body.clientHeight - active.offsetHeight) / 2;
            }
        },

        /**
         * Initial letter used to group a drawer in the letter navigation; '#' for non-alphabetic labels.
         */
        drawerLetter: function (drawer) {
            var first = (drawer.label || '').trim().normalize('NFD').charAt(0).toUpperCase();
            return /^[A-Z]$/.test(first) ? first : '#';
        },

        /**
         * Renders the headed drawer list, grouped behind a headed letter navigation when the catalog has more than 26 drawers.
         */
        renderSidebar: function () {
            var drawers = this.info.drawers;
            var html = '';
            var grouped = drawers.length > letterGroupThreshold;
            if (grouped) {
                var groups = new Map();
                drawers.forEach((drawer) => {
                    var letter = this.drawerLetter(drawer);
                    if (!groups.has(letter)) {
                        groups.set(letter, []);
                    }
                    groups.get(letter).push(drawer);
                });
                var activeDrawer = this.state.drawer ? this.drawerById(this.state.drawer) : null;
                var activeLetter = activeDrawer ? this.drawerLetter(activeDrawer) : groups.keys().next().value;
                var letters = Array.from(groups.keys()).sort();
                var navigation = letters
                    .map((letter) => {
                        var active = letter === activeLetter;
                        return `<button type="button" class="archives-cardcatalog__letter${active ? ' -active' : ''}" data-letter="${letter}" aria-pressed="${active}">${letter}</button>`;
                    })
                    .join('');
                drawers = groups.get(activeLetter) || [];
                html =
                    `<h2 class="archives-cardcatalog__sidebar-heading">${this.msg('letters')}</h2>` +
                    `<div class="archives-cardcatalog__letters" role="group" aria-label="${this.msg('letters')}">${navigation}</div>`;
            }
            var level = grouped ? 'h3' : 'h2';
            var drawerButtons = drawers
                .map((drawer) => {
                    var active = this.state.drawer === drawer.id ? ' -active' : '';
                    var current = active ? ' aria-current="true"' : '';
                    return `<button type="button" class="archives-cardcatalog__drawer${active}" data-drawer="${this.escape(drawer.id)}" title="${this.formatNumber(drawer.entryCount)} ${this.msg('cards')}"${current}>${this.escape(drawer.label)}</button>`;
                })
                .join('');
            html += `<${level} class="archives-cardcatalog__sidebar-heading">${this.msg('drawer')}</${level}><div class="archives-cardcatalog__drawer-group">${drawerButtons}</div>`;
            this.$drawers.html(html);
            this.$drawers.find('[data-letter]').on('click', (event) => {
                var letter = $(event.currentTarget).attr('data-letter');
                this.selectDrawer(groups.get(letter)[0].id);
            });
            this.$drawers.find('[data-drawer]').on('click', (event) => {
                this.selectDrawer($(event.currentTarget).attr('data-drawer'));
            });
            this.showActiveDrawer();
        },

        /**
         * Location trail from the chosen drawer down to the current range, one numbered zoom level per row; deep trails fold their middle levels behind a button.
         */
        renderPath: function (drawer) {
            var chain = this.state.chain;
            var steps = [{ index: -1, label: this.escape(drawer.label), from: 1, to: drawer.entryCount }];
            if (chain.length) {
                chain.slice(1).forEach((step, index) => steps.push({ index: index + 1, label: this.msgFormat('pathLevel', [index + 1]), from: step.from, to: step.to }));
                steps.push({ label: this.msgFormat('pathLevel', [chain.length]), from: this.state.from || 1, to: this.state.to || drawer.entryCount });
            }
            steps[steps.length - 1].current = true;
            var folded = steps.length > pathFoldThreshold ? steps.length - 4 : 0;
            var items = steps.map((step, position) => {
                var text =
                    `<span class="archives-cardcatalog__path-text"><span class="archives-cardcatalog__path-label">${step.label}</span> ` +
                    `<span class="archives-cardcatalog__path-range"><span class="sr-only">${this.msg('cards')} </span>${this.formatNumber(step.from)} &ndash; ${this.formatNumber(step.to)}</span></span>`;
                var content = step.current
                    ? `<span class="archives-cardcatalog__path-current" aria-current="location">${text}</span>`
                    : `<button type="button" class="archives-cardcatalog__path-link" data-chain="${step.index}">${text}</button>`;
                var hidden = folded && position >= 2 && position < steps.length - 2 ? ' hidden' : '';
                var item = `<li class="archives-cardcatalog__path-step${step.current ? ' -current' : ''}"${hidden}>${content}</li>`;
                if (folded && position === 2) {
                    item =
                        `<li class="archives-cardcatalog__path-step archives-cardcatalog__path-step--more">` +
                        `<button type="button" class="archives-cardcatalog__path-more" data-path-more="true"><span class="archives-cardcatalog__path-text">${this.msgFormat('pathMore', [folded])}</span></button></li>` +
                        item;
                }
                return item;
            });
            return (
                `<nav class="archives-cardcatalog__path" aria-labelledby="archivesCardCatalogPathHeading">` +
                `<h2 class="archives-cardcatalog__sidebar-heading" id="archivesCardCatalogPathHeading">${this.msg('location')}</h2>` +
                `<ol class="archives-cardcatalog__path-steps" role="list">${items.join('')}</ol></nav>`
            );
        },

        /**
         * Renders where the user is: nothing before a drawer is chosen, then the location trail of the drawer and its zoom levels.
         */
        renderOrientation: function () {
            this.$orientation.html(this.state.drawer ? this.renderPath(this.currentDrawer()) : '');
            this.$orientation.find('[data-chain]').on('click', (event) => {
                this.chainJump(parseInt($(event.currentTarget).attr('data-chain'), 10));
            });
            this.$orientation.find('[data-path-more]').on('click', (event) => {
                var $more = $(event.currentTarget).closest('li');
                var $revealed = $more.nextAll('[hidden]').prop('hidden', false);
                $more.remove();
                $revealed.first().find('button').trigger('focus');
            });
        },

        /**
         * Number of zoom levels needed to reach the full view in the largest drawer, from the configured zoom factor and full-view threshold.
         */
        zoomLevels: function () {
            var config = this.info.config || {};
            var zoomFactor = Math.max(2, config.zoomFactor || 0);
            var size = this.info.drawers.reduce((max, drawer) => Math.max(max, drawer.entryCount), 0);
            var levels = 0;
            while (size > config.fullViewThreshold && levels < 50) {
                size = Math.ceil((size - 1) / zoomFactor) + 1;
                levels++;
            }
            return levels;
        },

        /**
         * Renders the usage guide from the catalog data and configuration, marks the step that matches the current view and adds the question tip.
         */
        renderGuide: function () {
            var drawers = this.info.drawers;
            var steps = [];
            var drawerText = drawers.length ? this.msgFormat('guideDrawerText', [drawers[0].label, drawers[drawers.length - 1].label]) : '';
            if (drawers.length > letterGroupThreshold) {
                drawerText += ' ' + this.msg('guideLetters');
            }
            steps.push({ id: 'drawer', title: this.msg('guideDrawer'), text: drawerText });
            var levels = this.zoomLevels();
            if (levels > 0) {
                var zoomText = [this.msg('guideZoomText'), levels === 1 ? this.msg('guideZoomStepsOne') : this.msgFormat('guideZoomSteps', [levels])];
                if (this.info.mode === 'REGISTER') {
                    zoomText.push(this.msg('guideRegister'));
                }
                steps.push({ id: 'zoom', title: this.msg('guideZoom'), text: zoomText.join(' ') });
            }
            var record = `<span class="archives-cardcatalog__guide-record" data-cc-guide-record="true" hidden>${this.msg('guideRecord')}</span>`;
            steps.push({ id: 'read', title: this.msg('guideRead'), text: this.msg('guideReadText') + record });
            var current = !this.state.drawer ? 'drawer' : this.showsFullView() ? 'read' : 'zoom';
            var currentIndex = steps.findIndex((step) => step.id === current);
            var items = steps
                .map((step, index) => {
                    var mark = index === currentIndex ? ' -active' : index < currentIndex ? ' -done' : '';
                    var aria = index === currentIndex ? ' aria-current="step"' : '';
                    return (
                        `<li class="archives-cardcatalog__guide-step${mark}"${aria}>` +
                        `<span class="archives-cardcatalog__guide-title">${step.title}</span>` +
                        `<span class="archives-cardcatalog__guide-text">${step.text}</span></li>`
                    );
                })
                .join('');
            var tip = document.getElementById('feedbackModal')
                ? `<p class="archives-cardcatalog__guide-tip">${this.iconSvg('message-question')}<span>${this.msg('guideFeedbackText')}</span></p>`
                : '';
            this.$guide.html(
                `<h2 class="archives-cardcatalog__sidebar-heading">${this.msg('guide')}</h2><ol class="archives-cardcatalog__guide-steps" role="list">${items}</ol>${tip}`
            );
            if (this.stageItem && current === 'read') {
                this.$guide.find('[data-cc-guide-record]').prop('hidden', !this.stageItem.recordPi);
            }
        },

        /**
         * Renders the from–to register of the current drawer, or a retry notice after a failed register load.
         */
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
    });

    return viewer;
})(typeof module !== 'undefined' && module.exports ? require('./viewerJS.cardCatalog.js') : viewerJS, jQuery);

if (typeof module !== 'undefined' && module.exports) {
    module.exports = viewerJS;
}
