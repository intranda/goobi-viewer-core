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
 * @module viewerJS.calendarWidget
 * @description Sidebar calendar widget for newspaper/periodical records.
 * Fetches issues via REST API and renders an inline flatpickr calendar
 * with day-click navigation and multi-issue popover support.
 */
var viewerJS = (function (viewer) {
    'use strict';

    viewer.calendarWidget = {
        /**
         * Initialize the calendar widget.
         * @param {Object} config
         * @param {HTMLElement} config.container - Target DOM element
         * @param {string} config.anchorPi - PI of the anchor record
         * @param {string} config.currentYear - Year to display initially
         * @param {string} [config.locale] - Locale string (e.g. 'de')
         * @param {string} [config.appUrl] - Application base URL
         * @param {string} [config.currentLogId] - LogId of the currently viewed issue
         * @returns {Object|null} State object with destroy() method, or null if config invalid
         */
        init: function (config) {
            if (!config.container || !config.anchorPi || !config.currentYear) return null;
            if (config.appUrl && config.appUrl.slice(-1) !== '/') {
                config.appUrl += '/';
            }

            var state = {
                config: config,
                dateEntriesMap: {},
                calendarInstance: null,
                currentIssueDate: null,
                activePopover: null,
                popoverTrigger: null,
                boundOnDocClick: null,
                boundOnKeyDown: null,
                boundOnScroll: null,
            };

            state.destroy = function () {
                _destroy(state);
            };

            _fetchYearData(state, config.currentYear).then(function (dates) {
                _createCalendar(state, dates);
            });

            return state;
        },
    };

    // -- Data fetching --------------------------------------------------------

    function _fetchYearData(state, year) {
        state.currentIssueDate = null;
        var url = state.config.appUrl + 'api/v1/records/' + state.config.anchorPi + '/calendar/' + year;

        return fetch(url)
            .then(function (r) {
                if (!r.ok) throw new Error(r.status);
                return r.json();
            })
            .then(function (entries) {
                state.dateEntriesMap = {};
                var dates = [];
                entries.forEach(function (entry) {
                    if (!state.dateEntriesMap[entry.date]) {
                        state.dateEntriesMap[entry.date] = [];
                        dates.push(entry.date);
                    }
                    var entryUrl = state.config.appUrl + entry.url.replace(/^\//, '');
                    state.dateEntriesMap[entry.date].push({ label: entry.label, url: entryUrl });
                    var logId = entry.url.split('/').filter(Boolean).pop();
                    if (logId === state.config.currentLogId) {
                        state.currentIssueDate = entry.date;
                    }
                });
                return dates;
            })
            .catch(function (err) {
                console.warn('calendarWidget: failed to load year ' + year, err);
                state.dateEntriesMap = {};
                return [];
            });
    }

    // -- Popover --------------------------------------------------------------

    function _closePopover(state, returnFocus) {
        if (state.activePopover) {
            state.activePopover.remove();
            state.activePopover = null;
            if (returnFocus && state.popoverTrigger) {
                state.popoverTrigger.focus();
            }
            state.popoverTrigger = null;
        }
        _removePopoverListeners(state);
    }

    function _addPopoverListeners(state) {
        state.boundOnDocClick = function (e) {
            if (state.activePopover && !state.activePopover.contains(e.target)) {
                _closePopover(state);
            }
        };
        state.boundOnKeyDown = function (e) {
            if (!state.activePopover) return;
            if (e.key === 'Escape') {
                e.preventDefault();
                _closePopover(state, true);
            }
            if (e.key === 'Tab') {
                var items = state.activePopover.querySelectorAll('a');
                if (!items.length) return;
                var first = items[0];
                var last = items[items.length - 1];
                if (e.shiftKey && document.activeElement === first) {
                    e.preventDefault();
                    last.focus();
                } else if (!e.shiftKey && document.activeElement === last) {
                    e.preventDefault();
                    first.focus();
                }
            }
        };
        state.boundOnScroll = function () {
            _closePopover(state);
        };

        setTimeout(function () {
            document.addEventListener('click', state.boundOnDocClick);
            document.addEventListener('keydown', state.boundOnKeyDown);
            window.addEventListener('scroll', state.boundOnScroll, true);
            window.addEventListener('resize', state.boundOnScroll);
        }, 0);
    }

    function _removePopoverListeners(state) {
        if (state.boundOnDocClick) document.removeEventListener('click', state.boundOnDocClick);
        if (state.boundOnKeyDown) document.removeEventListener('keydown', state.boundOnKeyDown);
        if (state.boundOnScroll) {
            window.removeEventListener('scroll', state.boundOnScroll, true);
            window.removeEventListener('resize', state.boundOnScroll);
        }
        state.boundOnDocClick = state.boundOnKeyDown = state.boundOnScroll = null;
    }

    function _positionPopover(popover, anchorElem) {
        var rect = anchorElem.getBoundingClientRect();
        var popW = popover.offsetWidth;
        var popH = popover.offsetHeight;
        var left = rect.left + rect.width / 2 - popW / 2;
        var top = rect.top - popH - 4;

        if (top < 4) {
            top = rect.bottom + 4;
        }
        if (left < 4) {
            left = 4;
        } else if (left + popW > window.innerWidth - 4) {
            left = window.innerWidth - popW - 4;
        }

        popover.style.left = left + 'px';
        popover.style.top = top + 'px';
    }

    function _showPopover(state, dayElem, entries) {
        _closePopover(state);
        state.popoverTrigger = dayElem;

        var popover = document.createElement('div');
        popover.className = 'widget-calendar__popover';
        popover.setAttribute('role', 'menu');
        popover.setAttribute('aria-label', dayElem.getAttribute('aria-label') || '');

        entries.forEach(function (entry) {
            var link = document.createElement('a');
            link.href = entry.url;
            link.className = 'widget-calendar__popover-item';
            link.setAttribute('role', 'menuitem');
            link.textContent = entry.label || entry.url;
            popover.appendChild(link);
        });

        document.body.appendChild(popover);
        _positionPopover(popover, dayElem);
        state.activePopover = popover;

        var firstLink = popover.querySelector('a');
        if (firstLink) firstLink.focus();

        _addPopoverListeners(state);
    }

    // -- Day click handling ---------------------------------------------------

    function _handleDayClick(state, dateStr, dayElem) {
        var entries = state.dateEntriesMap[dateStr];
        if (!entries) return;
        if (entries.length === 1) {
            window.location.href = entries[0].url;
        } else if (dayElem) {
            _showPopover(state, dayElem, entries);
        }
    }

    // -- Calendar creation ----------------------------------------------------

    function _createCalendar(state, dates, targetDate) {
        if (state.calendarInstance) {
            viewer.datePicker.destroy(state.calendarInstance);
            state.config.container.innerHTML = '';
        }

        var defDate =
            targetDate ||
            (state.currentIssueDate ? viewer.datePicker._parseISODate(state.currentIssueDate) : null) ||
            (dates.length ? viewer.datePicker._parseISODate(dates[0]) : null) ||
            new Date(parseInt(state.config.currentYear, 10), 0, 1);

        state.calendarInstance = viewer.datePicker.createInlineCalendar(state.config.container, {
            hasDataDates: dates,
            hasMultipleMap: state.dateEntriesMap,
            currentIssueDate: state.currentIssueDate,
            locale: state.config.locale,
            defaultDate: defDate,
            onDayClick: function (dateStr, dateObj, dayElem) {
                _handleDayClick(state, dateStr, dayElem);
            },
            onYearChange: function (selectedDates, dateStr, instance) {
                _closePopover(state);
                var newYear = instance.currentYear;
                var month = instance.currentMonth;
                state._pendingYear = newYear;
                _fetchYearData(state, String(newYear)).then(function (newDates) {
                    if (state._pendingYear !== newYear) return;
                    _createCalendar(state, newDates, new Date(newYear, month, 1));
                });
            },
        });
    }

    // -- Cleanup --------------------------------------------------------------

    function _destroy(state) {
        _closePopover(state);
        if (state.calendarInstance) {
            viewer.datePicker.destroy(state.calendarInstance);
            state.calendarInstance = null;
        }
        state.config.container.innerHTML = '';
    }

    return viewer;
})(viewerJS || {});
