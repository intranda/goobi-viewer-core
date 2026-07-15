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
 *
 * Limits the active facets badge list to a maximum number of rows; a "+N more"
 * button ({0} = hidden count, template from data-more-label) reveals the rest.
 *
 * @version 26.07
 * @module viewerJS.activeFacetsBadges
 */
var viewerJS = (function (viewer) {
    'use strict';

    var _defaults = {
        wrapperSelector: '[data-badges="activeFacets"]',
        badgeSelector: '[data-badge="facet"]',
        moreButtonClass: 'active-facets-badges__more',
        hiddenClass: '-overflow',
        maxRows: 2,
        resizeDebounce: 200,
    };

    var _resizeBound = false;
    var _resizeTimer = null;
    var _lastSettings = null;

    viewer.activeFacetsBadges = {
        init: function (config) {
            var settings = Object.assign({}, _defaults, config);
            _lastSettings = settings;
            document.querySelectorAll(settings.wrapperSelector).forEach(function (wrapper) {
                _applyOverflow(wrapper, settings);
            });
            if (!_resizeBound) {
                _resizeBound = true;
                window.addEventListener('resize', _onResize);
            }
        },
    };

    // row assignments are measured once via offsetTop, so they go stale when the
    // available width changes (rotation, drawer, window resize): reset and reapply
    function _onResize() {
        window.clearTimeout(_resizeTimer);
        _resizeTimer = window.setTimeout(function () {
            document.querySelectorAll(_lastSettings.wrapperSelector).forEach(function (wrapper) {
                _resetOverflow(wrapper, _lastSettings);
                _applyOverflow(wrapper, _lastSettings);
            });
        }, _lastSettings.resizeDebounce);
    }

    function _resetOverflow(wrapper, settings) {
        var moreButton = wrapper.querySelector('.' + settings.moreButtonClass);
        if (moreButton) {
            (moreButton.parentElement.tagName === 'LI' ? moreButton.parentElement : moreButton).remove();
        }
        wrapper.querySelectorAll('.' + settings.hiddenClass).forEach(function (el) {
            el.classList.remove(settings.hiddenClass);
        });
    }

    function _applyOverflow(wrapper, settings) {
        if (wrapper.querySelector('.' + settings.moreButtonClass)) {
            return;
        }
        var badges = Array.from(wrapper.querySelectorAll(settings.badgeSelector));
        var rowTops = Array.from(new Set(badges.map((badge) => badge.offsetTop))).sort((a, b) => a - b);
        if (rowTops.length <= settings.maxRows) {
            return;
        }

        var cutoff = rowTops[settings.maxRows];
        var hiddenBadges = badges.filter((badge) => badge.offsetTop >= cutoff);
        hiddenBadges.forEach((badge) => _rowElement(badge).classList.add(settings.hiddenClass));

        var moreButton = document.createElement('button');
        moreButton.type = 'button';
        moreButton.className = settings.moreButtonClass;
        moreButton.textContent = (wrapper.dataset.moreLabel || '+{0}').replace('{0}', hiddenBadges.length);
        var mount = moreButton;
        if (wrapper.tagName === 'UL' || wrapper.tagName === 'OL') {
            mount = document.createElement('li');
            mount.className = 'active-facets-badges__list-item';
            mount.appendChild(moreButton);
        }
        moreButton.addEventListener('click', function () {
            hiddenBadges.forEach((badge) => _rowElement(badge).classList.remove(settings.hiddenClass));
            hiddenBadges[0].focus();
            mount.remove();
        });
        wrapper.appendChild(mount);
    }

    function _rowElement(badge) {
        return badge.closest('li') || badge;
    }

    return viewer;
})(viewerJS || {});

if (typeof module !== 'undefined' && module.exports) {
    module.exports = viewerJS;
}
