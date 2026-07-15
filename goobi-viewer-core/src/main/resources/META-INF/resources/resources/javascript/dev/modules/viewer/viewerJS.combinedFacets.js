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
 * Section toggles of the combined facets container: collapse/expand state is
 * persisted in sessionStorage and re-initialized after f:ajax section re-renders.
 *
 * @version 26.07
 * @module viewerJS.combinedFacets
 */
var viewerJS = (function (viewer) {
    'use strict';

    var _defaults = {
        toggleSelector: '[data-section-toggle]',
        sectionSelector: '.widget',
        collapsedClass: '-section-collapsed',
        storageKey: 'viewerJS.combinedFacets.collapsedSections',
    };

    var _ajaxSubscription = null;

    viewer.combinedFacets = {
        init: function (config) {
            var settings = Object.assign({}, _defaults, config);
            _initSections(settings);
            if (!_ajaxSubscription && typeof viewer.jsfAjax !== 'undefined') {
                _ajaxSubscription = viewer.jsfAjax.success.subscribe(function (event) {
                    if (event.source && event.source.getAttribute && event.source.getAttribute('data-collapse-link')) {
                        _initSections(settings);
                    }
                });
            }
        },
    };

    function _initSections(settings) {
        var collapsedSections = _readState(settings);
        document.querySelectorAll(settings.toggleSelector).forEach(function (toggle) {
            var section = toggle.closest(settings.sectionSelector);
            if (!section) {
                return;
            }
            if (collapsedSections.indexOf(toggle.dataset.sectionToggle) > -1) {
                _setCollapsed(toggle, section, true, settings);
            }
            if (toggle.dataset.sectionToggleBound) {
                return;
            }
            toggle.dataset.sectionToggleBound = 'true';
            toggle.addEventListener('click', function () {
                var collapse = !section.classList.contains(settings.collapsedClass);
                _setCollapsed(toggle, section, collapse, settings);
                _persist(toggle.dataset.sectionToggle, collapse, settings);
                if (!collapse) {
                    // sections may contain maps or sliders that need a relayout after expanding
                    window.dispatchEvent(new Event('resize'));
                }
            });
        });
    }

    function _setCollapsed(toggle, section, collapsed, settings) {
        section.classList.toggle(settings.collapsedClass, collapsed);
        toggle.setAttribute('aria-expanded', String(!collapsed));
    }

    function _readState(settings) {
        try {
            return JSON.parse(window.sessionStorage.getItem(settings.storageKey)) || [];
        } catch (e) {
            return [];
        }
    }

    function _persist(field, collapsed, settings) {
        var state = _readState(settings).filter(function (entry) {
            return entry !== field;
        });
        if (collapsed) {
            state.push(field);
        }
        window.sessionStorage.setItem(settings.storageKey, JSON.stringify(state));
    }

    return viewer;
})(viewerJS || {});

if (typeof module !== 'undefined' && module.exports) {
    module.exports = viewerJS;
}
