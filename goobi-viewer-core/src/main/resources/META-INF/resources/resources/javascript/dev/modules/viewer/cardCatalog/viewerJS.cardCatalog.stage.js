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

        /**
         * Loads a range of cards and renders the full view: header, thumbnail strip, stage and the footer toolbar.
         */
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
                    var header = this.renderContentHeader(drawer, from, to, true);
                    var stage =
                        `<div class="archives-cardcatalog__stage">` +
                        `<div class="archives-cardcatalog__stage-body"><div class="archives-cardcatalog__stage-viewer" id="ccStageViewer" role="img"></div></div></div>`;
                    var iconAction = (attributes, label, icon) =>
                        `<button type="button" class="archives-cardcatalog__stage-action archives-cardcatalog__stage-action--icon" ${attributes} aria-label="${label}" title="${label}">${this.iconSvg(icon)}</button>`;
                    var tools =
                        iconAction('data-cc-fullscreen="true" aria-pressed="false"', this.msg('fullscreenEnter'), 'maximize') +
                        (document.getElementById('feedbackModal') ? iconAction('data-cc-feedback="true"', this.msg('feedback'), 'message-question') : '');
                    var navigation =
                        `<button type="button" class="archives-cardcatalog__stage-action" data-shift="-1">${this.iconSvg('chevron-left')}<span>${this.msg('previous')}</span></button>` +
                        `<button type="button" class="archives-cardcatalog__stage-action" data-shift="1"><span>${this.msg('next')}</span>${this.iconSvg('chevron-right')}</button>`;
                    var footer =
                        `<div class="archives-cardcatalog__stage-footer">` +
                        `<div class="archives-cardcatalog__stage-caption"><div class="archives-cardcatalog__stage-caption-text" data-cc-caption="true"></div></div>` +
                        `<div class="archives-cardcatalog__stage-actions">` +
                        `<div class="archives-cardcatalog__stage-group">${tools}</div>` +
                        `<div class="archives-cardcatalog__stage-group archives-cardcatalog__stage-group--navigation">${navigation}</div>` +
                        `</div></div>`;
                    var strip = `<div class="archives-cardcatalog__strip" data-cc-strip="true"></div>`;
                    this.$content.html(`<div class="archives-cardcatalog__fullview">${header}<div class="archives-cardcatalog__stage-row">${strip}${stage}</div>${footer}</div>`);
                    this.bindContentEvents();
                    this.updateStage(current);
                    this.focusContent();
                    this.fillStrip();
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
            this.stageItem = item;
            this.renderNoteVariables(item);
            this.$guide.find('[data-cc-guide-record]').prop('hidden', !item.recordPi);
            var drawer = this.currentDrawer();
            var entryCount = drawer.entryCount;
            var from = this.state.from || 1;
            var to = this.state.to || entryCount;
            var $header = this.$content.find('.archives-cardcatalog__content-header');
            if ($header.attr('data-range') !== from + '-' + to) {
                this.renderOrientation();
                this.renderRegister();
                $header.attr('data-range', from + '-' + to);
            }
            $header.find('[data-cc-location]').text(`${this.msgText('card')} ${this.formatNumber(item.position)} ${this.msgText('of')} ${this.formatNumber(entryCount)}`);
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

            [-1, 1].forEach((direction) => {
                var atEdge = direction < 0 ? item.position <= 1 : item.position >= entryCount;
                var adjacent = atEdge ? this.adjacentDrawer(direction) : null;
                var label = adjacent ? this.msgText(direction < 0 ? 'previousDrawer' : 'nextDrawer') + ': ' + adjacent.label : this.msgText(direction < 0 ? 'previous' : 'next');
                var $button = this.$content.find('[data-shift="' + direction + '"]');
                $button
                    .prop('disabled', atEdge && !adjacent)
                    .attr('title', label)
                    .attr('aria-label', label);
                $button.find('span').text(adjacent ? this.msgText(direction < 0 ? 'previousDrawer' : 'nextDrawer') : label);
            });

            this.showStageImage(item);
        },

        /**
         * Enters or leaves fullscreen for the full view, natively or via the viewport fallback when the API is unavailable.
         */
        toggleFullscreen: function () {
            var fullview = this.$content.find('.archives-cardcatalog__fullview')[0];
            if (!fullview) {
                return;
            }
            var $wrapper = this.$content.closest('.archives-cardcatalog__wrapper');
            var action;
            if (document.fullscreenElement === fullview) {
                action = document.exitFullscreen();
            } else if ($wrapper.hasClass('archives-cardcatalog__wrapper--fullscreen')) {
                $wrapper.removeClass('archives-cardcatalog__wrapper--fullscreen');
                this.syncFullscreenButton();
            } else if (fullview.requestFullscreen && document.fullscreenEnabled !== false) {
                action = fullview.requestFullscreen();
            } else {
                $wrapper.addClass('archives-cardcatalog__wrapper--fullscreen');
                this.syncFullscreenButton();
            }
            if (action && action.catch) {
                action.catch((error) => console.error('ERROR - could not change card catalog fullscreen mode: ', error));
            }
        },

        /**
         * Updates icon, labels and pressed state of the fullscreen button and resizes the stage viewer.
         */
        syncFullscreenButton: function () {
            var $button = this.$content.find('[data-cc-fullscreen]');
            if (!$button.length) {
                return;
            }
            var expanded = this.isFullscreen();
            var label = this.msgText(expanded ? 'fullscreenExit' : 'fullscreenEnter');
            $button.attr({ 'aria-pressed': String(expanded), 'aria-label': label, title: label });
            $button.find('use').attr('href', `${this.rootUrl()}/resources/icons/outline/${expanded ? 'minimize' : 'maximize'}.svg#icon`);
            if (this.stageViewer && this.stageViewer.forceResize) {
                this.stageViewer.forceResize();
            }
        },

        /**
         * True while the full view is shown fullscreen, natively or via the viewport fallback.
         */
        isFullscreen: function () {
            var fullview = this.$content.find('.archives-cardcatalog__fullview')[0];
            return (
                (!!fullview && document.fullscreenElement === fullview) ||
                this.$content.closest('.archives-cardcatalog__wrapper').hasClass('archives-cardcatalog__wrapper--fullscreen')
            );
        },

        /**
         * Opens the site's contact modal with the displayed card and its deep link as message draft.
         * Fullscreen is left first because the modal lives outside the fullscreen element.
         */
        openFeedback: function () {
            var item = this.stageItem;
            var $modal = $('#feedbackModal');
            if (!item || !$modal.length) {
                return;
            }
            if (this.isFullscreen()) {
                this.toggleFullscreen();
            }
            var values = [item.label || '', this.currentDrawer().label, this.formatNumber(item.position), window.location.origin + this.cardUrl()];
            var message = this.msgTextFormat('feedbackMessage', values);
            $modal.find('textarea[id$="feedbackMessage"]').val(message).trigger('change');
            $modal.modal('show');
        },

        /**
         * Shows a card on the stage in OpenSeadragon, falling back to a plain image when the IIIF info or the viewer is unavailable.
         */
        showStageImage: function (item) {
            var container = document.getElementById('ccStageViewer');
            if (!container) {
                return;
            }
            var sequence = ++this.stageSeq;
            var isCurrent = () => sequence === this.stageSeq && document.getElementById('ccStageViewer') === container;
            var infoUrl = this.imageInfoUrl(item.imageUrl || item.thumbnailUrl);
            var renderPlainImage = () => {
                if (!isCurrent()) {
                    return;
                }
                this.destroyStageViewer();
                var src = item.imageUrl || item.thumbnailUrl;
                container.innerHTML = src
                    ? `<img src="${this.escape(src)}" alt="${this.escape(item.label || '')}"/>`
                    : `<span class="archives-cardcatalog__no-image">${this.msg('noImage')}</span>`;
            };
            if (!infoUrl) {
                renderPlainImage();
                return;
            }
            this.loadOpenSeadragon()
                .then(() => {
                    if (!isCurrent()) {
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
                    this.stageViewer = window.OpenSeadragon({
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

        /**
         * Disposes the OpenSeadragon viewer of the stage, if any.
         */
        destroyStageViewer: function () {
            if (this.stageViewer) {
                this.stageViewer.destroy();
                this.stageViewer = null;
            }
        },

        /**
         * Loads the OpenSeadragon script once on demand; resolves immediately when it is already present.
         */
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
    });

    return viewer;
})(typeof module !== 'undefined' && module.exports ? require('./viewerJS.cardCatalog.js') : viewerJS, jQuery);

if (typeof module !== 'undefined' && module.exports) {
    module.exports = viewerJS;
}
