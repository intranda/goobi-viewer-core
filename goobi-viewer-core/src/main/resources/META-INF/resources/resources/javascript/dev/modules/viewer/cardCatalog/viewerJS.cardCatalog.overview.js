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

    Object.assign(viewer.cardCatalog, {
        /**
         * Derives the KatZoom-style "Kurzanzeige" of a card from its IIIF image URL: the top part
         * of the scan (which carries the ordering word and the catalog entry) at full width, so
         * the zoom list stays readable without the scroll cost of full cards. Landscape index cards
         * carry their name line lower than portrait pages and forms, so they get a taller part.
         */
        headCropUrl: function (imageUrl) {
            if (!imageUrl || !/\/full\/![0-9]+,[0-9]+\//.test(imageUrl)) {
                return null;
            }
            var size = /^(\d+) \/ (\d+)$/.exec(this.cardRatio || '');
            var height = size && Number(size[1]) > Number(size[2]) ? 35 : 15;
            return imageUrl.replace(/\/full\/![0-9]+,[0-9]+\//, `/pct:0,0,100,${height}/!1200,480/`);
        },

        /**
         * Loads the boundary cards of a range and renders the zoom list with a zoom-in gap between each pair.
         */
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
                    var probe = this.cardRatio ? null : section.items.find((item) => item.thumbnailUrl || item.imageUrl);
                    if (!probe) {
                        this.renderZoomList(drawer, from, to, section);
                        return;
                    }
                    return this.learnCardRatio(probe.thumbnailUrl || probe.imageUrl).then(() => {
                        if (request.token === this.renderSeq) {
                            this.renderZoomList(drawer, from, to, section);
                        }
                    });
                })
                .catch((error) => {
                    if (request.token === this.renderSeq && error.name !== 'AbortError') {
                        console.error('ERROR - could not load card catalog boundaries: ', error);
                        this.showError();
                    }
                });
        },

        /**
         * Renders the zoom list of a range: the boundary cards with a zoom-in gap between each pair.
         */
        renderZoomList: function (drawer, from, to, section) {
            var header = this.renderContentHeader(drawer, from, to, false);
            var items = section.items
                .map((item, index) => {
                    var gap = '';
                    if (index < section.items.length - 1) {
                        var next = section.items[index + 1];
                        gap =
                            `<button type="button" class="archives-cardcatalog__gap" data-from="${Number(item.position)}" data-to="${Number(next.position)}">` +
                            `<span class="archives-cardcatalog__gap-content">` +
                            `<span class="archives-cardcatalog__gap-icon" aria-hidden="true">${this.iconSvg('zoom-in')}</span>` +
                            `<span class="archives-cardcatalog__gap-label">${this.msg('zoomIn')}</span>` +
                            `<span class="archives-cardcatalog__gap-count">${this.msg('cards')} ${this.formatNumber(item.position)} &ndash; ${this.formatNumber(next.position)}</span>` +
                            `</span>` +
                            `</button>`;
                    }
                    return this.renderZoomCard(item) + gap;
                })
                .join('');
            var key = JSON.stringify([drawer.id, from, to]);
            var modifiers = this.showFullPreviews ? ' archives-cardcatalog__zoomlist--full' : '';
            this.$content.html(
                `<div class="archives-cardcatalog__overview">${header}<div class="archives-cardcatalog__zoomlist${modifiers}" data-overview="${this.escape(key)}">${items}</div></div>`
            );
            this.bindContentEvents();
            this.restoreOverviewPosition(key);
        },

        /**
         * A boundary card in the vertical zoom list: shows the IIIF head crop of the scan (the
         * ordering word) at readable size so the card content itself decides where to zoom in.
         */
        renderZoomCard: function (item) {
            var full = item.imageUrl || item.thumbnailUrl;
            var crop = this.headCropUrl(item.imageUrl) || full;
            var src = this.showFullPreviews ? full : crop;
            var image = src
                ? `<img loading="lazy" src="${this.escape(src)}" data-preview-full="${this.escape(full)}" data-preview-crop="${this.escape(crop)}" alt=""/>`
                : `<span class="archives-cardcatalog__no-image">${this.msg('noImage')}</span>`;
            return (
                `<button type="button" class="archives-cardcatalog__zoomcard" data-open="${Number(item.position)}" title="${this.msg('fullView')}">` +
                `${image}<span class="archives-cardcatalog__zoomcard-caption">` +
                `<span class="archives-cardcatalog__card-position">${this.msg('card')} ${this.formatNumber(item.position)}</span>` +
                `<span class="archives-cardcatalog__card-title">${this.escape(item.label || '')}</span>` +
                `<span class="archives-cardcatalog__card-action">${this.msg('viewCard')}</span>` +
                `</span></button>`
            );
        },

        /**
         * Switches the zoom list between head crops and complete card images.
         */
        togglePreviews: function () {
            this.showFullPreviews = !this.showFullPreviews;
            this.$content.find('.archives-cardcatalog__zoomlist').toggleClass('archives-cardcatalog__zoomlist--full', this.showFullPreviews);
            this.$content.find('[data-preview-toggle]').attr('aria-pressed', String(this.showFullPreviews));
            this.$content.find('[data-preview-full]').each((index, image) => {
                image.src = image.getAttribute(this.showFullPreviews ? 'data-preview-full' : 'data-preview-crop');
            });
        },

        /**
         * Stores the scroll positions of the current zoom list so returning to it restores them; keeps the last 32 ranges.
         */
        rememberOverviewPosition: function () {
            var overview = this.$content.find('[data-overview]')[0];
            if (!overview) {
                return;
            }
            if (!this.overviewPositions) {
                this.overviewPositions = new Map();
            }
            var key = overview.getAttribute('data-overview');
            this.overviewPositions.delete(key);
            var wrapper = this.$content.closest('.archives-cardcatalog__wrapper')[0];
            var page = wrapper && this.isFullscreen() ? wrapper.scrollTop : window.scrollY;
            this.overviewPositions.set(key, { content: this.$content.parent().scrollTop(), page: page });
            if (this.overviewPositions.size > 32) {
                this.overviewPositions.delete(this.overviewPositions.keys().next().value);
            }
        },

        /**
         * Restores the remembered scroll positions of a zoom list, or scrolls to the top for an unknown range.
         */
        restoreOverviewPosition: function (key) {
            var position = this.overviewPositions && this.overviewPositions.get(key);
            this.$content.parent().scrollTop(position ? position.content : 0);
            this.focusContent(position);
        },
    });

    return viewer;
})(typeof module !== 'undefined' && module.exports ? require('./viewerJS.cardCatalog.js') : viewerJS, jQuery);

if (typeof module !== 'undefined' && module.exports) {
    module.exports = viewerJS;
}
