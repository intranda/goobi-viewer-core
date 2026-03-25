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
 * @module viewerJS.datePicker
 * @description Flatpickr wrapper for the Goobi viewer. Provides date pickers,
 * date range pickers, and inline calendars with locale support,
 * format conversion, and auto-initialization from data-attributes.
 */
var viewerJS = (function (viewer) {
    'use strict';

    // -- Private helpers ------------------------------------------------------

    function _toISODate(d) {
        return (
            d.getFullYear() +
            '-' +
            String(d.getMonth() + 1).padStart(2, '0') +
            '-' +
            String(d.getDate()).padStart(2, '0')
        );
    }

    function _parseISODate(isoStr) {
        var parts = isoStr.split('-');
        return new Date(parseInt(parts[0], 10), parseInt(parts[1], 10) - 1, parseInt(parts[2], 10));
    }

    var _javaToFp = { yyyy: 'Y', MMMM: 'F', MM: 'm', dd: 'd', HH: 'H', hh: 'h', mm: 'i', aa: 'K' };
    var _fpToLuxon = { Y: 'yyyy', F: 'MMMM', m: 'MM', d: 'dd', H: 'HH', h: 'hh', i: 'mm', K: 'aa' };

    function _escapeRegex(s) {
        return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    }

    function _convertByMap(format, map) {
        if (!format) return format;
        var tokens = Object.keys(map).sort(function (a, b) {
            return b.length - a.length;
        });
        var regex = new RegExp(tokens.map(_escapeRegex).join('|'), 'g');
        return format.replace(regex, function (match) {
            return map[match];
        });
    }

    function _hideExtraWeekRow(fp) {
        var dc = fp.days;
        if (!dc) return;
        var cells = dc.querySelectorAll('.flatpickr-day');
        if (cells.length < 42) return;
        var allNextMonth = true;
        for (var i = 35; i < 42; i++) {
            if (!cells[i].classList.contains('nextMonthDay')) {
                allNextMonth = false;
                break;
            }
        }
        dc.classList.toggle('--hide-last-row', allNextMonth);
    }

    function _wrapCallback(existing, extra) {
        if (!existing) return extra;
        if (Array.isArray(existing)) return existing.concat(extra);
        return function () {
            existing.apply(this, arguments);
            extra.apply(this, arguments);
        };
    }

    // -- Public API -----------------------------------------------------------

    viewer.datePicker = {
        // Test-accessible helpers
        _toISODate: _toISODate,
        _parseISODate: _parseISODate,
        _escapeRegex: _escapeRegex,
        _convertByMap: _convertByMap,
        _wrapCallback: _wrapCallback,
        _javaToFp: _javaToFp,
        _fpToLuxon: _fpToLuxon,

        instances: [],

        // -- Locales ----------------------------------------------------------

        locales: {
            de: {
                days: ['Sonntag', 'Montag', 'Dienstag', 'Mittwoch', 'Donnerstag', 'Freitag', 'Samstag'],
                daysShort: ['Son', 'Mon', 'Die', 'Mit', 'Don', 'Fre', 'Sam'],
                daysMin: ['So', 'Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa'],
                months: [
                    'Januar',
                    'Februar',
                    'März',
                    'April',
                    'Mai',
                    'Juni',
                    'Juli',
                    'August',
                    'September',
                    'Oktober',
                    'November',
                    'Dezember',
                ],
                monthsShort: ['Jan', 'Feb', 'Mär', 'Apr', 'Mai', 'Jun', 'Jul', 'Aug', 'Sep', 'Okt', 'Nov', 'Dez'],
                today: 'Heute',
                clear: 'Löschen',
                dateFormat: 'd.m.Y',
                timeFormat: 'H:i',
                firstDay: 1,
            },
            en: {
                days: ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'],
                daysShort: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
                daysMin: ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'],
                months: [
                    'January',
                    'February',
                    'March',
                    'April',
                    'May',
                    'June',
                    'July',
                    'August',
                    'September',
                    'October',
                    'November',
                    'December',
                ],
                monthsShort: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],
                today: 'Today',
                clear: 'Clear',
                dateFormat: 'm/d/Y',
                timeFormat: 'h:i K',
                firstDay: 0,
            },
        },

        // -- Defaults ---------------------------------------------------------

        defaults: {
            closeOnSelect: true,
            clickOpens: true,
            disableMobile: true,
            allowInput: true,
            navTitles: {
                days: 'MMMM yyyy',
                months: 'yyyy',
                years: 'yyyy1 - yyyy2',
            },
            prevArrow:
                '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>',
            nextArrow:
                '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>',
        },

        // -- Locale resolution ------------------------------------------------

        _toFlatpickrLocale: function (localeObj) {
            if (!localeObj) return {};
            return {
                weekdays: {
                    shorthand: localeObj.daysMin || localeObj.daysShort || [],
                    longhand: localeObj.days || [],
                },
                months: {
                    shorthand: localeObj.monthsShort || [],
                    longhand: localeObj.months || [],
                },
                firstDayOfWeek: typeof localeObj.firstDay === 'number' ? localeObj.firstDay : 1,
            };
        },

        _resolveLocale: function (localeStr) {
            if (!localeStr) localeStr = 'de';
            var key = localeStr.toLowerCase().substring(0, 2);
            return this.locales[key] || this.locales['en'];
        },

        _resolveLocaleFromConfig: function (el, config) {
            var key = (el.dataset && el.dataset.datepickerLocale) || null;
            if (key) return this._resolveLocale(key);
            if (typeof config.locale === 'string') return this._resolveLocale(config.locale);
            if (config.locale && typeof config.locale === 'object') return config.locale;
            return this._resolveLocale('de');
        },

        // -- Date parsing -----------------------------------------------------

        _parseExistingDate: function (value, formatObj) {
            if (!value || !formatObj || !formatObj.dateFormat) return null;
            if (typeof luxon === 'undefined') return null;
            var dt = luxon.DateTime.fromFormat(value.trim(), formatObj.dateFormat);
            return dt.isValid ? dt.toJSDate() : null;
        },

        // -- Factory methods --------------------------------------------------

        create: function (el, opts) {
            if (!el || typeof flatpickr === 'undefined') {
                console.warn('viewerJS.datePicker: flatpickr not loaded or element missing');
                return null;
            }

            var config = Object.assign({}, this.defaults, opts);

            var resolvedLocale = this._resolveLocaleFromConfig(el, config);
            config.locale = resolvedLocale;

            var javaFormat = null;
            var formatAttr = el.getAttribute('data-format');
            if (formatAttr) {
                javaFormat = formatAttr;
            } else if (el.dataset && el.dataset.datepickerFormat) {
                javaFormat = el.dataset.datepickerFormat;
            }

            if (javaFormat) {
                config.dateFormat = _convertByMap(javaFormat, _javaToFp);
            }
            if (el.dataset && el.dataset.datepickerMin) {
                config.minDate = el.dataset.datepickerMin;
            }
            if (el.dataset && el.dataset.datepickerMax) {
                config.maxDate = el.dataset.datepickerMax;
            }
            if (el.dataset && el.dataset.datepickerMode === 'range') {
                config.mode = 'range';
            }
            if (el.dataset && el.dataset.datepickerInline === 'true') {
                config.inline = true;
            }

            if (el.tagName === 'INPUT' && el.value && !config.defaultDate && !config.selectedDates) {
                var parseFmt =
                    javaFormat ||
                    (resolvedLocale && resolvedLocale.dateFormat
                        ? _convertByMap(resolvedLocale.dateFormat, _fpToLuxon)
                        : null);
                if (parseFmt) {
                    var existingDate = this._parseExistingDate(el.value, { dateFormat: parseFmt });
                    if (existingDate) {
                        config.defaultDate = [existingDate];
                    }
                }
            }

            var isBuiltinLocale = resolvedLocale === this.locales.de || resolvedLocale === this.locales.en;

            var fpLocale;
            if (isBuiltinLocale) {
                fpLocale = Object.assign({}, this._toFlatpickrLocale(resolvedLocale), {
                    dateFormat: resolvedLocale.dateFormat,
                });
            } else {
                fpLocale = resolvedLocale || {};
            }

            var fpConfig = {
                closeOnSelect: config.closeOnSelect !== undefined ? config.closeOnSelect : true,
                clickOpens: config.clickOpens !== undefined ? config.clickOpens : true,
                disableMobile: config.disableMobile !== undefined ? config.disableMobile : true,
                allowInput: config.allowInput !== undefined ? config.allowInput : true,
                locale: fpLocale,
            };

            fpConfig.dateFormat = config.dateFormat || (resolvedLocale && resolvedLocale.dateFormat) || 'Y-m-d';

            var _fpPassthrough = [
                'minDate',
                'maxDate',
                'mode',
                'inline',
                'defaultDate',
                'onChange',
                'onDayCreate',
                'onReady',
                'onYearChange',
                'onMonthChange',
                'prevArrow',
                'nextArrow',
                'navTitles',
            ];

            _fpPassthrough.forEach(function (key) {
                if (config[key] !== undefined) fpConfig[key] = config[key];
            });

            // Hide empty 6th week row on all calendars
            var hideRow = function (selectedDates, dateStr, fp) {
                _hideExtraWeekRow(fp);
            };
            fpConfig.onReady = _wrapCallback(fpConfig.onReady, hideRow);
            fpConfig.onOpen = _wrapCallback(fpConfig.onOpen, hideRow);
            fpConfig.onMonthChange = _wrapCallback(fpConfig.onMonthChange, hideRow);
            fpConfig.onYearChange = _wrapCallback(fpConfig.onYearChange, hideRow);

            var instance = flatpickr(el, fpConfig);

            this.instances.push(instance);
            return instance;
        },

        createRange: function (startEl, endEl, opts) {
            var baseOpts = Object.assign({}, this.defaults, opts);
            var _updating = false;
            var externalOnChange = opts && opts.onChange ? opts.onChange : null;

            var endInstance;
            var startOpts = Object.assign({}, baseOpts);
            startOpts.onChange = function (selectedDates, dateStr, instance) {
                if (_updating) return;
                var date = selectedDates && selectedDates.length && selectedDates[0] ? selectedDates[0] : null;
                if (date && endInstance) {
                    _updating = true;
                    endInstance.set('minDate', date);
                    _updating = false;
                }
                if (externalOnChange) {
                    externalOnChange(selectedDates, dateStr, instance);
                }
            };

            var startInstance = this.create(startEl, startOpts);

            var endOpts = Object.assign({}, baseOpts);
            endOpts.onChange = function (selectedDates, dateStr, instance) {
                if (_updating) return;
                var date = selectedDates && selectedDates.length && selectedDates[0] ? selectedDates[0] : null;
                if (date && startInstance) {
                    _updating = true;
                    startInstance.set('maxDate', date);
                    _updating = false;
                }
                if (externalOnChange) {
                    externalOnChange(selectedDates, dateStr, instance);
                }
            };

            endInstance = this.create(endEl, endOpts);

            if (startInstance && startInstance.selectedDates && startInstance.selectedDates.length && endInstance) {
                endInstance.set('minDate', startInstance.selectedDates[0]);
            }
            if (endInstance && endInstance.selectedDates && endInstance.selectedDates.length && startInstance) {
                startInstance.set('maxDate', endInstance.selectedDates[0]);
            }

            return { start: startInstance, end: endInstance };
        },

        createInlineCalendar: function (containerEl, opts) {
            var hasDataDates = opts.hasDataDates || [];
            var hasMultipleMap = opts.hasMultipleMap || {};
            var currentIssueDateStr = opts.currentIssueDate || null;
            var onDayClickCb = opts.onDayClick || null;

            var fpOpts = {
                locale: opts.locale,
                defaultDate: opts.defaultDate,
                inline: true,
                onDayCreate: function (dObj, dStr, fp, dayElem) {
                    var dateStr = _toISODate(dayElem.dateObj);

                    if (hasDataDates.indexOf(dateStr) !== -1) {
                        dayElem.classList.add('-has-data-');
                    }
                    if (hasMultipleMap[dateStr] && hasMultipleMap[dateStr].length > 1) {
                        dayElem.classList.add('-has-multiple-');
                    }
                    if (dateStr === currentIssueDateStr) {
                        dayElem.classList.add('-current-issue-');
                    }
                },
                onChange: function (selectedDates, dStr, fp) {
                    if (!selectedDates || !selectedDates.length) return;
                    var dateStr = _toISODate(selectedDates[0]);

                    if (hasDataDates.indexOf(dateStr) !== -1 && onDayClickCb) {
                        var dayElem = fp.days.querySelector(
                            '.flatpickr-day.selected:not(.prevMonthDay):not(.nextMonthDay)'
                        );
                        onDayClickCb(dateStr, selectedDates[0], dayElem);
                    }
                },
            };

            if (opts.onYearChange) fpOpts.onYearChange = opts.onYearChange;

            return this.create(containerEl, fpOpts);
        },

        // -- Auto-initialization ----------------------------------------------

        init: function () {
            var self = this;

            document
                .querySelectorAll('[data-datepicker="true"]:not([data-datepicker-initialized])')
                .forEach(function (el) {
                    self.create(el);
                    el.setAttribute('data-datepicker-initialized', 'true');
                });

            document
                .querySelectorAll('[data-datepicker-range-group]:not([data-datepicker-initialized])')
                .forEach(function (startEl) {
                    var group = startEl.dataset.datepickerRangeGroup;
                    var endEl = document.querySelector('[data-datepicker-range-end="' + group + '"]');
                    if (endEl) {
                        self.createRange(startEl, endEl);
                        startEl.setAttribute('data-datepicker-initialized', 'true');
                        endEl.setAttribute('data-datepicker-initialized', 'true');
                    }
                });

            document
                .querySelectorAll('[data-datepicker-inline="true"]:not([data-datepicker-initialized])')
                .forEach(function (el) {
                    self.createInlineCalendar(el, {
                        locale: self._resolveLocale(el.dataset.datepickerLocale || 'de'),
                        hasDataDates: [],
                        currentIssueDate: el.dataset.datepickerCurrentDate || null,
                        onDayClick: function (dateStr) {},
                        onMonthChange: function (selectedDates, dateStr, instance) {},
                    });
                    el.setAttribute('data-datepicker-initialized', 'true');
                });
        },

        // -- Lifecycle --------------------------------------------------------

        destroy: function (instance) {
            if (instance && instance.destroy) {
                instance.destroy();
            }
            var idx = this.instances.indexOf(instance);
            if (idx !== -1) {
                this.instances.splice(idx, 1);
            }
        },

        destroyAll: function () {
            this.instances.forEach(function (inst) {
                if (inst && inst.destroy) {
                    inst.destroy();
                }
            });
            this.instances = [];
        },
    };

    return viewer;
})(viewerJS || {});

if (typeof module !== 'undefined' && module.exports) {
    module.exports = viewerJS;
}
