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
        /**
         * Builds the view state from URL parameters, validating drawer, interval and card against the catalog info.
         */
        stateFromParams: function (params, info) {
            var state = { drawer: null, from: null, to: null, card: null, chain: [] };
            var drawerId = params.get('drawer');
            var drawer = drawerId && info ? info.drawers.find((d) => d.id === drawerId) : null;
            if (drawer) {
                state.drawer = drawerId;
                var from = this.parsePosition(params.get('from'));
                var to = this.parsePosition(params.get('to'));
                if (from && to && from <= drawer.entryCount && to >= from) {
                    state.from = from;
                    state.to = Math.min(to, drawer.entryCount);
                    state.chain = [{ from: 1, to: drawer.entryCount }];
                }
                var card = this.parsePosition(params.get('card'));
                if (card && card <= drawer.entryCount) {
                    state.card = card;
                    if (
                        info.config &&
                        ((state.from && (card < state.from || card > state.to)) ||
                            (drawer.entryCount > info.config.fullViewThreshold && (!state.from || state.to - state.from + 1 > info.config.fullViewThreshold)))
                    ) {
                        var range = this.windowAround(card, drawer, info.config);
                        state.from = range.from;
                        state.to = range.to;
                        state.chain = [{ from: 1, to: drawer.entryCount }];
                    }
                }
            }
            return state;
        },

        /**
         * Positive integer from a URL parameter, or null for anything else.
         */
        parsePosition: function (value) {
            return value && /^[1-9]\d*$/.test(value) && Number.isSafeInteger(Number(value)) ? Number(value) : null;
        },

        /**
         * Reuses a history-entry state when it matches the URL, keeping its zoom chain if the chain is still valid.
         */
        keptState: function (state) {
            if (!state || !this.info) {
                return null;
            }
            var urlState = this.stateFromParams(new URLSearchParams(window.location.search), this.info);
            if (state.drawer !== urlState.drawer || state.from !== urlState.from || state.to !== urlState.to || state.card !== urlState.card) {
                return null;
            }
            var drawer = urlState.drawer && this.drawerById(urlState.drawer);
            var chain = Array.isArray(state.chain) ? state.chain : [];
            if (
                drawer &&
                urlState.from &&
                chain.length &&
                chain[0].from === 1 &&
                chain[0].to === drawer.entryCount &&
                chain.every((step) => Number.isInteger(step.from) && Number.isInteger(step.to) && step.from >= 1 && step.to <= drawer.entryCount && step.to >= step.from)
            ) {
                urlState.chain = chain.map((step) => ({ from: step.from, to: step.to }));
            }
            return urlState;
        },

        /**
         * Writes the view state into URL parameters, replacing the catalog parameters and keeping unrelated ones.
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
         * Path and query of the current view, the deep link to the shown card or range.
         */
        cardUrl: function () {
            var params = this.paramsFromState(this.state, new URLSearchParams(window.location.search));
            params.delete('selected');
            var query = params.toString();
            return window.location.pathname + (query ? '?' + query : '');
        },

        /**
         * Writes the current state to the URL, as a new history entry or replacing the current one.
         */
        syncUrl: function (push) {
            history[push ? 'pushState' : 'replaceState'](Object.assign({}, history.state, { cc: this.state }), '', this.cardUrl() + window.location.hash);
        },

        /**
         * Full-view window of configured size centered on a position and clamped to the drawer bounds.
         */
        windowAround: function (position, drawer, config) {
            var size = Math.max(1, (config || this.info.config).fullViewThreshold);
            var from = Math.max(1, Math.min(position - Math.floor(size / 2), drawer.entryCount - size + 1));
            return { from: from, to: Math.min(from + size - 1, drawer.entryCount) };
        },
    });

    return viewer;
})(typeof module !== 'undefined' && module.exports ? require('./viewerJS.cardCatalog.js') : viewerJS);

if (typeof module !== 'undefined' && module.exports) {
    module.exports = viewerJS;
}
