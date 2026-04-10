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
         * @param {string} [config.currentIssueDate] - ISO date (YYYY-MM-DD) of the currently viewed issue
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
                currentIssueDate: config.currentIssueDate || null,
                activePopover: null,
                popoverTrigger: null,
                boundOnDocClick: null,
                boundOnKeyDown: null,
                boundOnScroll: null,
                availableMonths: [],
                availableYears: [],
                _lastMonth: null,
            };

            state.destroy = function () {
                _destroy(state);
            };

            Promise.all([_fetchYearData(state, config.currentYear), _fetchAvailableMonths(state)]).then(
                function (results) {
                    _createCalendar(state, results[0]);
                }
            );

            return state;
        },
    };

    // -- Data fetching --------------------------------------------------------

    function _fetchAvailableMonths(state) {
        var url = state.config.appUrl + 'api/v1/records/' + state.config.anchorPi + '/calendar/months';
        return fetch(url)
            .then(function (r) {
                if (!r.ok) throw new Error(r.status);
                return r.json();
            })
            .then(function (months) {
                state.availableMonths = months;
                state.availableYears = _extractYears(months);
            })
            .catch(function (err) {
                console.warn('calendarWidget: failed to load available months', err);
                state.availableMonths = [];
                state.availableYears = [];
            });
    }

    function _toYearMonth(year, month) {
        return String(year).padStart(4, '0') + '-' + String(month + 1).padStart(2, '0');
    }

    function _extractYears(months) {
        var years = [];
        var seen = {};
        for (var i = 0; i < months.length; i++) {
            var y = months[i].substring(0, 4);
            if (!seen[y]) {
                seen[y] = true;
                years.push(parseInt(y, 10));
            }
        }
        return years;
    }

    function _findNextAvailableYear(state, currentYear, direction) {
        var years = state.availableYears;
        if (!years.length) return null;
        if (direction > 0) {
            for (var i = 0; i < years.length; i++) {
                if (years[i] > currentYear) return years[i];
            }
        } else {
            for (var j = years.length - 1; j >= 0; j--) {
                if (years[j] < currentYear) return years[j];
            }
        }
        return null;
    }

    function _findFirstMonthInYear(state, year) {
        var prefix = String(year).padStart(4, '0') + '-';
        for (var i = 0; i < state.availableMonths.length; i++) {
            if (state.availableMonths[i].indexOf(prefix) === 0) {
                return parseInt(state.availableMonths[i].substring(5), 10) - 1;
            }
        }
        return 0;
    }

    function _findLastMonthInYear(state, year) {
        var prefix = String(year).padStart(4, '0') + '-';
        for (var i = state.availableMonths.length - 1; i >= 0; i--) {
            if (state.availableMonths[i].indexOf(prefix) === 0) {
                return parseInt(state.availableMonths[i].substring(5), 10) - 1;
            }
        }
        return 11;
    }

    function _findNextAvailableMonth(state, year, month, direction) {
        var months = state.availableMonths;
        if (!months.length) return null;

        var current = _toYearMonth(year, month);
        if (direction > 0) {
            for (var i = 0; i < months.length; i++) {
                if (months[i] > current) return months[i];
            }
        } else {
            for (var j = months.length - 1; j >= 0; j--) {
                if (months[j] < current) return months[j];
            }
        }
        return null;
    }

    function _fetchYearData(state, year) {
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
                    state.dateEntriesMap[entry.date].push({ label: entry.label, url: entryUrl, date: entry.date });
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
            if (entry.date === state.currentIssueDate) {
                link.classList.add('-active-');
                link.setAttribute('aria-current', 'page');
            }
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
            _clearSelection(state.calendarInstance);
        }
    }

    // -- Selection clearing ---------------------------------------------------

    function _clearSelection(instance) {
        if (!instance) return;
        instance.selectedDates = [];
        instance.latestSelectedDateObj = undefined;
        var sel = instance.calendarContainer.querySelectorAll('.flatpickr-day.selected');
        for (var i = 0; i < sel.length; i++) {
            sel[i].classList.remove('selected');
        }
    }

    // -- Arrow state ----------------------------------------------------------

    function _updateArrowState(state, instance) {
        if (!state.availableMonths.length || !instance) return;

        // Month arrows
        var prevBtn = instance.calendarContainer.querySelector('.flatpickr-prev-month');
        var nextBtn = instance.calendarContainer.querySelector('.flatpickr-next-month');
        if (prevBtn) {
            var hasPrev = _findNextAvailableMonth(state, instance.currentYear, instance.currentMonth, -1) !== null;
            prevBtn.classList.toggle('-disabled-', !hasPrev);
            prevBtn.style.pointerEvents = hasPrev ? '' : 'none';
        }
        if (nextBtn) {
            var hasNext = _findNextAvailableMonth(state, instance.currentYear, instance.currentMonth, 1) !== null;
            nextBtn.classList.toggle('-disabled-', !hasNext);
            nextBtn.style.pointerEvents = hasNext ? '' : 'none';
        }

        // Year spinner arrows
        var yearWrapper = instance.calendarContainer.querySelector('.numInputWrapper');
        if (yearWrapper) {
            var arrowUp = yearWrapper.querySelector('.arrowUp');
            var arrowDown = yearWrapper.querySelector('.arrowDown');
            var hasNextYear = _findNextAvailableYear(state, instance.currentYear, 1) !== null;
            var hasPrevYear = _findNextAvailableYear(state, instance.currentYear, -1) !== null;
            if (arrowUp) {
                arrowUp.classList.toggle('-disabled-', !hasNextYear);
                arrowUp.style.pointerEvents = hasNextYear ? '' : 'none';
            }
            if (arrowDown) {
                arrowDown.classList.toggle('-disabled-', !hasPrevYear);
                arrowDown.style.pointerEvents = hasPrevYear ? '' : 'none';
            }
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

        state._lastMonth = _toYearMonth(defDate.getFullYear(), defDate.getMonth());
        state._skipInProgress = false;

        state.calendarInstance = viewer.datePicker.createInlineCalendar(state.config.container, {
            hasDataDates: dates,
            hasMultipleMap: state.dateEntriesMap,
            currentIssueDate: state.currentIssueDate,
            locale: state.config.locale,
            defaultDate: defDate,
            onDayClick: function (dateStr, dateObj, dayElem) {
                _handleDayClick(state, dateStr, dayElem);
            },
            onMonthChange: function (selectedDates, dateStr, instance) {
                if (state._skipInProgress) return;
                if (!state.availableMonths.length) {
                    state._lastMonth = _toYearMonth(instance.currentYear, instance.currentMonth);
                    return;
                }

                var current = _toYearMonth(instance.currentYear, instance.currentMonth);
                if (state.availableMonths.indexOf(current) !== -1) {
                    state._lastMonth = current;
                    _updateArrowState(state, instance);
                    return;
                }

                var direction = current > state._lastMonth ? 1 : -1;
                var target = _findNextAvailableMonth(state, instance.currentYear, instance.currentMonth, direction);
                if (!target) {
                    state._lastMonth = current;
                    _updateArrowState(state, instance);
                    return;
                }

                var parts = target.split('-');
                var targetYear = parseInt(parts[0], 10);
                var targetMonth = parseInt(parts[1], 10) - 1;

                state._skipInProgress = true;
                state._lastMonth = target;

                if (targetYear !== instance.currentYear) {
                    state._pendingYear = targetYear;
                    _fetchYearData(state, String(targetYear)).then(function (newDates) {
                        if (state._pendingYear !== targetYear) return;
                        state._skipInProgress = false;
                        _createCalendar(state, newDates, new Date(targetYear, targetMonth, 1));
                    });
                } else {
                    instance.changeMonth(targetMonth - instance.currentMonth, false);
                    state._skipInProgress = false;
                    _updateArrowState(state, instance);
                }
            },
            onYearChange: function (selectedDates, dateStr, instance) {
                if (state._skipInProgress) return;
                _closePopover(state);
                var lastYear = parseInt(state._lastMonth.substring(0, 4), 10);
                var direction = instance.currentYear > lastYear ? 1 : -1;
                var targetYear = _findNextAvailableYear(state, lastYear, direction);
                if (!targetYear) return;

                var month =
                    direction > 0 ? _findFirstMonthInYear(state, targetYear) : _findLastMonthInYear(state, targetYear);
                state._skipInProgress = true;
                state._pendingYear = targetYear;
                _fetchYearData(state, String(targetYear)).then(function (newDates) {
                    if (state._pendingYear !== targetYear) return;
                    state._skipInProgress = false;
                    _createCalendar(state, newDates, new Date(targetYear, month, 1));
                });
            },
        });

        _clearSelection(state.calendarInstance);
        _updateArrowState(state, state.calendarInstance);

        var yearInput = state.calendarInstance.calendarContainer.querySelector('.cur-year');
        if (yearInput) yearInput.setAttribute('readonly', 'readonly');
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
