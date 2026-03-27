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
 * @module viewerJS.tocNewspaper
 * @description Handles newspaper/periodical TOC functionality: tab persistence,
 * decade grouping of year chips, auto-select first year, datepicker init,
 * and calendar popover init. Used by viewToc.xhtml and viewCalendar.xhtml.
 */
var viewerJS = (function (viewer) {
    'use strict';

    var _tabSelector = '.search-calendar__nav-tabs';
    var _tabContentSelector = '.search-calendar__tab-content';
    var _yearGridSelector = '.search-calendar__year-grid';
    var _yearChipSelector = '.search-calendar__year-chip';
    var _decadeThreshold = 5;
    viewer.tocNewspaper = {
        /**
         * @description Initialize all newspaper TOC features. Call from viewToc.xhtml
         * and viewCalendar.xhtml. Automatically detects which features are needed
         * based on DOM elements present.
         * @param {Object} config
         * @param {string} [config.contextPath] - Application context path for calendarPopover
         * @param {string} [config.popoverTitle] - Title for calendarPopover
         * @param {string} [config.locale] - Locale string for datepicker (e.g. 'de')
         * @param {string} [config.dateFromSelector] - Selector for date-from input
         * @param {string} [config.dateToSelector] - Selector for date-to input
         */
        init: function (config) {
            config = config || {};

            _initSearchField();
            _initYearSelection();
            _initYearUrlPersistence();
            _initTabPersistence();
            _initDecadeGrouping();

            if (config.contextPath) {
                _initCalendarPopover(config);
            }

            if (config.locale && config.dateFromSelector && config.dateToSelector) {
                _initDatepicker(config);
            }
        },
    };

    /**
     * Clear the search term field on initial page load so the placeholder is visible.
     */
    function _initSearchField() {
        var field = document.getElementById('newspaperSearchTerm');
        if (field) {
            field.value = '';
        }
    }

    /**
     * Restore active tab from URL hash (#tab=ID format) and persist tab changes to hash.
     */
    function _initTabPersistence() {
        var navTabs = document.querySelector(_tabSelector);
        if (!navTabs) return;

        var hash = window.location.hash;
        if (hash && hash.indexOf('#tab=') === 0) {
            var tabId = '#' + hash.substring(5);
            var tab = navTabs.querySelector('a[href="' + tabId + '"]');
            if (tab) {
                var activeTab = navTabs.querySelector('.nav-link.active');
                var activePane = document.querySelector(_tabContentSelector + ' .tab-pane.active');
                if (activeTab) activeTab.classList.remove('active');
                if (activePane) activePane.classList.remove('show', 'active');
                tab.classList.add('active');
                var pane = document.querySelector(tabId);
                if (pane) pane.classList.add('show', 'active');
            }
            history.replaceState(null, null, window.location.pathname + window.location.search);
        }

        navTabs.querySelectorAll('a[data-toggle="tab"]').forEach(function (a) {
            a.addEventListener('click', function () {
                history.replaceState(null, null, '#tab=' + a.getAttribute('href').substring(1));
            });
        });
    }

    /**
     * Group year chips by decade when the number of years exceeds the threshold.
     */
    function _initDecadeGrouping() {
        var yearGrid = document.querySelector(_yearGridSelector);
        if (!yearGrid) return;

        var chips = Array.from(yearGrid.querySelectorAll(_yearChipSelector));
        if (chips.length <= _decadeThreshold) return;

        var fragment = document.createDocumentFragment();
        var currentDecade = null;

        chips.forEach(function (chip) {
            var year = chip.getAttribute('data-year');
            var decade = year.substring(0, 3) + '0';
            if (decade !== currentDecade) {
                var header = document.createElement('div');
                header.className = 'search-calendar__decade-header';
                header.textContent = decade + 'er';
                fragment.appendChild(header);
                currentDecade = decade;
            }
            fragment.appendChild(chip);
        });

        yearGrid.innerHTML = '';
        yearGrid.appendChild(fragment);
    }

    /**
     * On first page load (no year explicitly selected via chip click),
     * remove active state from chips and hide calendar results.
     * A chip click sets #tab=newspaperTabYear in the URL, so its presence
     * indicates an explicit year selection.
     */
    function _initYearSelection() {
        var yearGrid = document.querySelector(_yearGridSelector);
        if (!yearGrid) return;

        var hash = window.location.hash;
        var wasYearClicked = hash && hash.indexOf('#tab=newspaperTabYear') === 0;

        if (!wasYearClicked) {
            yearGrid.querySelectorAll(_yearChipSelector + '.-active').forEach(function (chip) {
                chip.classList.remove('-active');
            });

            var issuesTitle = document.querySelector('.search-calendar__issues-title');
            var monthsGrid = document.querySelector('.search-calendar__months');
            if (issuesTitle) issuesTitle.style.display = 'none';
            if (monthsGrid) monthsGrid.style.display = 'none';
        }
    }

    /**
     * Persist selected year in URL as ?year= query parameter.
     * On incoming links with ?year=, sync the dropdown and submit if needed.
     */
    function _initYearUrlPersistence() {
        var dropdown = document.getElementById('tocSelectYear');
        if (!dropdown) return;

        var params = new URLSearchParams(window.location.search);
        var urlYear = params.get('year');
        var selectedYear = dropdown.value;

        if (urlYear && urlYear !== selectedYear) {
            var option = dropdown.querySelector('option[value="' + urlYear + '"]');
            if (option) {
                dropdown.value = urlYear;
                dropdown.form.submit();
                return;
            }
        }

        if (selectedYear) {
            params.set('year', selectedYear);
            history.replaceState(null, '', window.location.pathname + '?' + params.toString());
        }
    }

    /**
     * Initialize calendar popover if trigger elements exist.
     */
    function _initCalendarPopover(config) {
        var popoverConfig = {
            appUrl: config.contextPath + '/',
            indexResourceUrl: config.contextPath + '/api/v1/index/query/',
            popoverTitle: config.popoverTitle || '',
        };

        if (viewer.calendarPopover) {
            viewer.calendarPopover.init(popoverConfig);
        }
    }
 
    /**
     * Initialize date range picker for newspaper search.
     */
    function _initDatepicker(config) {
        if (!viewer.datePicker) return;

        var startEl = document.querySelector(config.dateFromSelector);
        var endEl = document.querySelector(config.dateToSelector);
        if (startEl && endEl) {
            viewer.datePicker.createRange(startEl, endEl, {
                locale: config.locale,
            });
        }
    }

    return viewer;
})(viewerJS || {}, jQuery);
