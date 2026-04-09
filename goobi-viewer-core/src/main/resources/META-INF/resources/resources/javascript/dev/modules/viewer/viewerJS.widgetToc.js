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
 *
 * @module viewerJS.widgetToc
 * @description Client-side expand/collapse for the sidebar TOC widget (used in both
 * sidebar and fullscreen views). Uses a flat list of <li> elements with data-level
 * attributes. On init, scrolls to the currently active TOC entry.
 * @requires jQuery
 */
var viewerJS = (function (viewer) {
    'use strict';

    var _debug = false;

    viewer.widgetToc = {
        /**
         * @description Initializes the widget TOC. Attaches a delegated click listener
         * for toggle, expand-all, and collapse-all actions, then scrolls to the active element.
         * @method init
         */
        init: function () {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.widgetToc.init');
                console.log('##############################');
            }

            var container = document.getElementById('widgetToc');
            if (!container) return;

            container.addEventListener('click', function (e) {
                var toggle = e.target.closest('.widget-toc__toggle');
                if (toggle) {
                    var li = toggle.closest('.widget-toc__element');
                    if (li.classList.contains('widget-toc__element--expanded')) {
                        _collapseElement(li);
                    } else {
                        _expandElement(li);
                    }
                    return;
                }

                if (e.target.closest('[data-action="expand-all"]')) {
                    _expandAll(container);
                    return;
                }

                if (e.target.closest('[data-action="collapse-all"]')) {
                    _collapseAll(container);
                    return;
                }
            });

            _scrollToActive(container);
        },
    };

    /**
     * @description Returns the nesting level of a TOC element from its data-level attribute.
     * @param {HTMLElement} li - The TOC list item.
     * @returns {number} The nesting level (0 = root).
     */
    function _getLevel(li) {
        return parseInt(li.dataset.level, 10);
    }

    /**
     * @description Updates the aria-expanded attribute on the toggle button of a TOC element.
     * @param {HTMLElement} li - The TOC list item.
     * @param {boolean} expanded - Whether the element is expanded.
     */
    function _updateToggle(li, expanded) {
        var btn = li.querySelector('.widget-toc__toggle');
        if (btn) {
            btn.setAttribute('aria-expanded', String(expanded));
        }
    }

    /**
     * @description Shows the direct children (level + 1) of a parent element.
     * Recursively shows children of already-expanded descendants.
     * @param {HTMLElement} parentLi - The parent TOC list item.
     */
    function _showDirectChildren(parentLi) {
        var level = _getLevel(parentLi);
        var next = parentLi.nextElementSibling;
        while (next) {
            var nextLevel = _getLevel(next);
            if (nextLevel <= level) break;
            if (nextLevel === level + 1) {
                next.classList.remove('widget-toc__element--hidden');
                if (next.classList.contains('widget-toc__element--expanded')) {
                    _showDirectChildren(next);
                }
            }
            next = next.nextElementSibling;
        }
    }

    /**
     * @description Hides all descendant elements of a parent by adding the hidden class.
     * @param {HTMLElement} parentLi - The parent TOC list item.
     */
    function _hideDescendants(parentLi) {
        var level = _getLevel(parentLi);
        var next = parentLi.nextElementSibling;
        while (next) {
            if (_getLevel(next) <= level) break;
            next.classList.add('widget-toc__element--hidden');
            next = next.nextElementSibling;
        }
    }

    /**
     * @description Expands a single TOC element: marks it as expanded, updates the toggle
     * button, and reveals its direct children.
     * @param {HTMLElement} li - The TOC list item to expand.
     */
    function _expandElement(li) {
        li.classList.add('widget-toc__element--expanded');
        _updateToggle(li, true);
        _showDirectChildren(li);
    }

    /**
     * @description Collapses a single TOC element: removes the expanded state, updates the
     * toggle button, and hides all descendants.
     * @param {HTMLElement} li - The TOC list item to collapse.
     */
    function _collapseElement(li) {
        li.classList.remove('widget-toc__element--expanded');
        _updateToggle(li, false);
        _hideDescendants(li);
    }

    /**
     * @description Expands all parent elements and reveals all hidden items in the TOC.
     * @param {HTMLElement} container - The TOC container element.
     */
    function _expandAll(container) {
        container.querySelectorAll('.widget-toc__element.parent').forEach(function (li) {
            li.classList.add('widget-toc__element--expanded');
            _updateToggle(li, true);
        });
        container.querySelectorAll('.widget-toc__element--hidden').forEach(function (li) {
            li.classList.remove('widget-toc__element--hidden');
        });
    }

    /**
     * @description Collapses all elements: hides all non-root items and removes the
     * expanded state from all parents.
     * @param {HTMLElement} container - The TOC container element.
     */
    function _collapseAll(container) {
        container.querySelectorAll('.widget-toc__element').forEach(function (li) {
            if (_getLevel(li) > 0) {
                li.classList.add('widget-toc__element--hidden');
            }
            if (li.classList.contains('parent')) {
                li.classList.remove('widget-toc__element--expanded');
                _updateToggle(li, false);
            }
        });
    }

    /**
     * @description Ensures a TOC element is visible by removing the hidden class and
     * expanding all ancestor elements up to the root.
     * @param {HTMLElement} li - The TOC list item to make visible.
     */
    function _ensureVisible(li) {
        if (!li.classList.contains('widget-toc__element--hidden')) return;
        li.classList.remove('widget-toc__element--hidden');
        var level = _getLevel(li);
        if (level === 0) return;
        var prev = li.previousElementSibling;
        while (prev) {
            var prevLevel = _getLevel(prev);
            if (prevLevel < level) {
                prev.classList.add('widget-toc__element--expanded');
                _updateToggle(prev, true);
                prev.classList.remove('widget-toc__element--hidden');
                if (prevLevel === 0) break;
                level = prevLevel;
            }
            prev = prev.previousElementSibling;
        }
    }

    /**
     * @description Scrolls the TOC container so the currently active element is visible.
     * Expands ancestor elements if the active element is hidden.
     * @param {HTMLElement} container - The TOC container element.
     */
    function _scrollToActive(container) {
        var active = container.querySelector('.widget-toc__element.active');
        if (!active) return;
        _ensureVisible(active);
        var scrollParent = container.querySelector('.widget-toc__elements');
        if (!scrollParent || scrollParent.scrollHeight <= scrollParent.clientHeight) return;
        requestAnimationFrame(function () {
            var top = active.offsetTop;
            var el = active.offsetParent;
            while (el && el !== scrollParent) {
                top += el.offsetTop;
                el = el.offsetParent;
            }
            scrollParent.scrollTop = top - scrollParent.clientHeight / 2;
        });
    }

    return viewer;
})(viewerJS || {}, jQuery);
