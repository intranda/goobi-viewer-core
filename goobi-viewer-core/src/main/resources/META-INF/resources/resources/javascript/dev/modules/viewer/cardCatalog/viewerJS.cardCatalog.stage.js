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

var viewerJS = (function (viewer) {
    'use strict';

    Object.assign(viewer.cardCatalog, {
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
    });

    return viewer;
})(typeof module !== 'undefined' && module.exports ? require('./viewerJS.cardCatalog.js') : viewerJS);

if (typeof module !== 'undefined' && module.exports) {
    module.exports = viewerJS;
}
