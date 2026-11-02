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
    var cardRatioKey = 'cardCatalogRatio:';
    var cardRatioPattern = /^\d+ \/ \d+$/;
    var cardRatioProbeTimeout = 3000;

    Object.assign(viewer.cardCatalog, {
        /**
         * Restores the card format remembered for this catalog and listens for the first loaded thumbnail to measure it otherwise.
         */
        initCardRatio: function () {
            this.cardRatio = null;
            var stored = null;
            try {
                stored = sessionStorage.getItem(cardRatioKey + this.config.pi);
            } catch (e) {
                stored = null;
            }
            if (stored && cardRatioPattern.test(stored)) {
                this.setCardRatio(stored);
            }
            if (this.cardRatioTarget) {
                this.cardRatioTarget.removeEventListener('load', this.cardRatioListener, true);
            }
            this.cardRatioListener =
                this.cardRatioListener ||
                ((event) => {
                    if (event.target.matches && event.target.matches('.archives-cardcatalog__strip-thumb img')) {
                        this.applyCardRatio(event.target);
                    }
                });
            this.cardRatioTarget = this.$content[0] || null;
            if (this.cardRatioTarget) {
                this.cardRatioTarget.addEventListener('load', this.cardRatioListener, true);
            }
        },

        /**
         * Shapes the thumbnails after the format of the first loaded card image and remembers it for the session.
         */
        applyCardRatio: function (image) {
            if (this.cardRatio || !image.naturalWidth || !image.naturalHeight) {
                return;
            }
            this.setCardRatio(image.naturalWidth + ' / ' + image.naturalHeight);
            try {
                sessionStorage.setItem(cardRatioKey + this.config.pi, this.cardRatio);
            } catch (e) {
                console.warn('card catalog: could not remember the card format', e);
            }
            this.centerCurrentThumb();
        },

        /**
         * Loads one card image to learn the card format before a view that depends on it; settles on load, on error and after a timeout.
         */
        learnCardRatio: function (url) {
            return new Promise((resolve) => {
                var image = new Image();
                var timer = setTimeout(resolve, cardRatioProbeTimeout);
                image.addEventListener('load', () => {
                    clearTimeout(timer);
                    this.applyCardRatio(image);
                    resolve();
                });
                image.addEventListener('error', () => {
                    clearTimeout(timer);
                    resolve();
                });
                image.src = url;
            });
        },

        /**
         * Sets the card format as aspect ratio for the thumbnails of the catalog.
         */
        setCardRatio: function (ratio) {
            this.cardRatio = ratio;
            var wrapper = this.$content.closest('.archives-cardcatalog__wrapper')[0];
            if (wrapper) {
                wrapper.style.setProperty('--_archives-cardcatalog-card-ratio', ratio);
            }
        },

        /**
         * Scrolls the strip so that the thumbnail of the shown card sits in its middle.
         */
        centerCurrentThumb: function () {
            var strip = this.$content.find('[data-cc-strip]')[0];
            var currentThumb = strip ? strip.querySelector('.archives-cardcatalog__strip-thumb.-current') : null;
            if (currentThumb) {
                strip.scrollTop = currentThumb.offsetTop - (strip.clientHeight - currentThumb.clientHeight) / 2;
                strip.scrollLeft = currentThumb.offsetLeft - (strip.clientWidth - currentThumb.clientWidth) / 2;
            }
        },

        /**
         * Thumbnail button of a card in the strip, marked as current when it is the shown card.
         */
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

        /**
         * Trims a requested strip extension to the drawer bounds and to the part not loaded yet; null when nothing is left.
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
         * Loads neighboring cards into the strip, keeps the scroll position stable and evicts the far end beyond the size limit.
         */
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

        /**
         * Retries the strip extension that failed last.
         */
        retryStrip: function () {
            if (!this.stripErrorRange || this.stripLoading) {
                return;
            }
            var range = this.stripErrorRange;
            return this.extendStrip(range.from, range.to);
        },

        /**
         * Near either end of the strip, loads the next page of cards in that direction.
         */
        onStripScroll: function () {
            if (!this.section || this.stripLoading || this.stripErrorRange) {
                return;
            }
            var strip = this.$content.find('[data-cc-strip]')[0];
            if (!strip) {
                return;
            }
            var vertical = !getComputedStyle(strip).flexDirection.startsWith('row');
            var position = vertical ? strip.scrollTop : strip.scrollLeft;
            var extent = vertical ? strip.clientHeight : strip.clientWidth;
            var total = vertical ? strip.scrollHeight : strip.scrollWidth;
            if (position + extent > total - stripLoadThreshold) {
                this.extendStripTowards(1);
            } else if (position < stripLoadThreshold) {
                this.extendStripTowards(-1);
            }
        },

        /**
         * Next strip page after (direction > 0) or before the loaded cards, or null at that end of the drawer.
         */
        nextStripRange: function (direction) {
            var step = this.info.config.listPageSize;
            var range = direction > 0 ? [this.section.to + 1, this.section.to + step] : [this.section.from - step, this.section.from - 1];
            return range[0] > this.currentDrawer().entryCount || range[1] < 1 ? null : range;
        },

        /**
         * Loads the next strip page in a direction unless a load is pending or failed.
         */
        extendStripTowards: function (direction) {
            if (!this.section || this.stripLoading || this.stripErrorRange) {
                return Promise.resolve();
            }
            var range = this.nextStripRange(direction);
            return range ? this.extendStrip(range[0], range[1]) : Promise.resolve();
        },

        /**
         * Loads pages after, then before, the loaded cards until the strip overflows and can scroll, or the drawer is exhausted.
         */
        fillStrip: function () {
            var strip = this.$content.find('[data-cc-strip]')[0];
            var section = this.section;
            if (!strip || !section || this.stripLoading || this.stripErrorRange) {
                return Promise.resolve();
            }
            var vertical = !getComputedStyle(strip).flexDirection.startsWith('row');
            var extent = vertical ? strip.clientHeight : strip.clientWidth;
            var total = vertical ? strip.scrollHeight : strip.scrollWidth;
            var range = extent && total <= extent ? this.nextStripRange(1) || this.nextStripRange(-1) : null;
            if (!range) {
                return Promise.resolve();
            }
            var before = section.from + '-' + section.to;
            return this.extendStrip(range[0], range[1]).then(() => (this.section === section && section.from + '-' + section.to !== before ? this.fillStrip() : undefined));
        },
    });

    return viewer;
})(typeof module !== 'undefined' && module.exports ? require('./viewerJS.cardCatalog.js') : viewerJS, jQuery);

if (typeof module !== 'undefined' && module.exports) {
    module.exports = viewerJS;
}
