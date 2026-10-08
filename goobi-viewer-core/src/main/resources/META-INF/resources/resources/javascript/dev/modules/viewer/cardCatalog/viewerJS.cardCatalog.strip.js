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

    var stripLoadThreshold = 200;

    Object.assign(viewer.cardCatalog, {
        renderStripThumb: function (item, currentPosition) {
            var current = item.position === currentPosition ? ' -current' : '';
            var currentAttribute = current ? ' aria-current="true"' : '';
            var image = item.thumbnailUrl ? `<img loading="lazy" src="${this.escape(item.thumbnailUrl)}" alt=""/>` : '<span class="archives-cardcatalog__no-image"></span>';
            return (
                `<button type="button" class="archives-cardcatalog__strip-thumb${current}" data-stage="${Number(item.position)}" title="${this.escape(item.label || '')}" aria-label="${this.msg('card')} ${this.formatNumber(item.position)}: ${this.escape(item.label || '')}"${currentAttribute}>` +
                `${image}<span class="archives-cardcatalog__strip-position">${this.formatNumber(item.position)}</span>` +
                `</button>`
            );
        },

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

        extendStrip: function (from, to) {
            if (!this.section) {
                return Promise.resolve();
            }
            if (this.stripLoading && this.stripPromise) {
                var pending = this.section;
                var oldFrom = pending.from;
                var oldTo = pending.to;
                return this.stripPromise.then(() => (this.section === pending && (pending.from !== oldFrom || pending.to !== oldTo) ? this.extendStrip(from, to) : undefined));
            }
            var drawer = this.currentDrawer();
            var section = this.section;
            var range = this.clampExtension(section, from, to, drawer.entryCount);
            if (!range) {
                return Promise.resolve();
            }
            from = range.from;
            to = range.to;
            this.stripErrorRange = null;
            this.$content.find('[data-strip-error]').remove();
            this.stripLoading = true;
            var controller = new AbortController();
            this.stripController = controller;
            this.stripPromise = this.fetchJson(`${this.catalogUrl()}drawers/${encodeURIComponent(drawer.id)}/entries?from=${from}&to=${to}`, 'entries', controller.signal)
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
                    var append = from > section.to;
                    var beforeHeight = strip.scrollHeight;
                    var beforeWidth = strip.scrollWidth;
                    var beforeTop = strip.scrollTop;
                    var beforeLeft = strip.scrollLeft;
                    if (append) {
                        section.items = section.items.concat(items);
                        $strip.append(html);
                    } else {
                        section.items = items.concat(section.items);
                        $strip.prepend(html);
                        strip.scrollTop = beforeTop + strip.scrollHeight - beforeHeight;
                        strip.scrollLeft = beforeLeft + strip.scrollWidth - beforeWidth;
                    }
                    var limit = Math.max(this.info.config.fullViewThreshold, this.info.config.listPageSize * 3);
                    var excess = section.items.length - limit;
                    if (excess > 0) {
                        var heightBeforeTrim = strip.scrollHeight;
                        var widthBeforeTrim = strip.scrollWidth;
                        if (append) {
                            section.items.splice(0, excess);
                            $strip.children('[data-stage]').slice(0, excess).remove();
                            strip.scrollTop -= heightBeforeTrim - strip.scrollHeight;
                            strip.scrollLeft -= widthBeforeTrim - strip.scrollWidth;
                        } else {
                            section.items.splice(-excess);
                            $strip.children('[data-stage]').slice(-excess).remove();
                        }
                    }
                    section.from = section.items[0].position;
                    section.to = section.items[section.items.length - 1].position;
                    $strip.attr('data-range', section.from + '-' + section.to);
                })
                .catch((error) => {
                    if (this.section === section) {
                        this.stripLoading = false;
                        if (error.name !== 'AbortError') {
                            this.stripErrorRange = { from: from, to: to };
                            var message =
                                `<div class="archives-cardcatalog__strip-error" data-strip-error="true" role="alert">${this.msg('loadMoreError')} ` +
                                `<button type="button" class="archives-cardcatalog__strip-retry" data-strip-retry="true">${this.msg('retry')}</button></div>`;
                            this.$content.find('.archives-cardcatalog__stage-caption').append(message);
                        }
                    }
                    if (error.name !== 'AbortError') {
                        console.error('ERROR - could not extend card catalog strip: ', error);
                    }
                })
                .finally(() => {
                    if (this.stripController === controller) {
                        this.stripController = null;
                        this.stripPromise = null;
                    }
                });
            return this.stripPromise;
        },

        retryStrip: function () {
            if (!this.stripErrorRange || this.stripLoading) {
                return;
            }
            var range = this.stripErrorRange;
            return this.extendStrip(range.from, range.to);
        },

        onStripScroll: function () {
            if (!this.section || this.stripLoading || this.stripErrorRange) {
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
            if (position + extent > total - stripLoadThreshold) {
                this.extendStrip(this.section.to + 1, this.section.to + step);
            } else if (position < stripLoadThreshold) {
                this.extendStrip(this.section.from - step, this.section.from - 1);
            }
        },
    });

    return viewer;
})(typeof module !== 'undefined' && module.exports ? require('./viewerJS.cardCatalog.js') : viewerJS, jQuery);

if (typeof module !== 'undefined' && module.exports) {
    module.exports = viewerJS;
}
