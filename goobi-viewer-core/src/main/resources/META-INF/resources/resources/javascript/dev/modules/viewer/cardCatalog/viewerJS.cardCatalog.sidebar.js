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
         * Renders the drawer list, grouped behind a letter navigation when the catalog has more than 26 drawers.
         */
        renderSidebar: function () {
            var drawers = this.info.drawers;
            var html;
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
                html = `<div class="archives-cardcatalog__letters" role="group" aria-label="${this.msg('letters')}">${navigation}</div>`;
                this.$drawers.addClass('archives-cardcatalog__drawers--letters');
            } else {
                html = '';
                this.$drawers.removeClass('archives-cardcatalog__drawers--letters');
            }
            var drawerButtons = drawers
                .map((drawer) => {
                    var active = this.state.drawer === drawer.id ? ' -active' : '';
                    var current = active ? ' aria-current="true"' : '';
                    return `<button type="button" class="archives-cardcatalog__drawer${active}" data-drawer="${this.escape(drawer.id)}" title="${this.formatNumber(drawer.entryCount)} ${this.msg('cards')}"${current}>${this.escape(drawer.label)}</button>`;
                })
                .join('');
            html += grouped ? `<div class="archives-cardcatalog__drawer-group">${drawerButtons}</div>` : drawerButtons;
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
         * Renders the catalog figures as labelled values with the index date and, once a drawer is chosen, the current selection.
         */
        renderCounts: function () {
            var info = this.info;
            var stat = (label, value) => `<div class="archives-cardcatalog__stat"><dt>${this.msg(label)}</dt><dd>${this.formatNumber(value)}</dd></div>`;
            var html =
                `<h2 class="archives-cardcatalog__sidebar-heading">${this.msg('catalog')}</h2>` +
                `<dl class="archives-cardcatalog__stats">${stat('cards', info.totalEntries)}${stat('drawers', info.drawers.length)}</dl>` +
                (info.dateIndexed ? `<p class="archives-cardcatalog__facts">${this.msgFormat('indexDate', [this.formatDate(info.dateIndexed)])}</p>` : '');
            var drawer = this.state.drawer ? this.currentDrawer() : null;
            if (drawer) {
                html +=
                    `<h2 class="archives-cardcatalog__sidebar-heading">${this.msg('selection')}</h2><dl class="archives-cardcatalog__selection">` +
                    `<dt>${this.msg('drawer')}</dt><dd>${this.escape(drawer.label)}</dd>` +
                    `<dt>${this.msg('cards')}</dt><dd>${this.formatNumber(drawer.entryCount)}</dd>`;
                if (this.state.from && this.state.to) {
                    html += `<dt>${this.msg('interval')}</dt><dd>${this.formatNumber(this.state.from)} &ndash; ${this.formatNumber(this.state.to)}</dd>`;
                }
                html += '</dl>';
            }
            this.$counts.html(html);
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
