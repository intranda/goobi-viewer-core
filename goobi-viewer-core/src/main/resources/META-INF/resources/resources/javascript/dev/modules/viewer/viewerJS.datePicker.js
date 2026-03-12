var viewerJS = (function (viewer) {
    'use strict';

    viewer.datePicker = {
        instances: [],

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

        _convertFormat: function (airFormat) {
            if (!airFormat) return airFormat;
            return airFormat
                .replace(/yyyy/g, 'Y')
                .replace(/MMMM/g, 'F')
                .replace(/MM/g, 'm')
                .replace(/dd/g, 'd')
                .replace(/HH/g, 'H')
                .replace(/hh/g, 'h')
                .replace(/mm/g, 'i')
                .replace(/aa/g, 'K');
        },

        _convertFormatToLuxon: function (fpFormat) {
            if (!fpFormat) return fpFormat;
            return fpFormat
                .replace(/Y/g, 'yyyy')
                .replace(/F/g, 'MMMM')
                .replace(/m/g, 'MM')
                .replace(/d/g, 'dd')
                .replace(/H/g, 'HH')
                .replace(/h/g, 'hh')
                .replace(/i/g, 'mm')
                .replace(/K/g, 'aa');
        },

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

        _parseExistingDate: function (value, formatObj) {
            if (!value || !formatObj || !formatObj.dateFormat) return null;
            if (typeof luxon === 'undefined') return null;
            var dt = luxon.DateTime.fromFormat(value.trim(), formatObj.dateFormat);
            return dt.isValid ? dt.toJSDate() : null;
        },

        create: function (el, opts) {
            if (!el || typeof flatpickr === 'undefined') {
                console.warn('viewerJS.datePicker: flatpickr not loaded or element missing');
                return null;
            }

            var config = Object.assign({}, this.defaults, opts);

            var resolvedLocale;
            if (el.dataset && el.dataset.datepickerLocale) {
                resolvedLocale = this._resolveLocale(el.dataset.datepickerLocale);
            } else if (typeof config.locale === 'string') {
                resolvedLocale = this._resolveLocale(config.locale);
            } else if (config.locale && typeof config.locale === 'object' && config.locale.dateFormat) {
                resolvedLocale = config.locale;
            } else if (!config.locale) {
                resolvedLocale = this._resolveLocale('de');
            } else {
                resolvedLocale = config.locale;
            }

            config.locale = resolvedLocale;

            var javaFormat = null;
            var formatAttr = el.getAttribute('data-format');
            if (formatAttr) {
                javaFormat = formatAttr;
            } else if (el.dataset && el.dataset.datepickerFormat) {
                javaFormat = el.dataset.datepickerFormat;
            }

            if (javaFormat) {
                config.dateFormat = this._convertFormat(javaFormat);
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
                        ? this._convertFormatToLuxon(resolvedLocale.dateFormat)
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
            if (config.prevArrow) fpConfig.prevArrow = config.prevArrow;
            if (config.nextArrow) fpConfig.nextArrow = config.nextArrow;
            if (config.defaultDate) fpConfig.defaultDate = config.defaultDate;
            if (config.minDate) fpConfig.minDate = config.minDate;
            if (config.maxDate) fpConfig.maxDate = config.maxDate;
            if (config.inline) fpConfig.inline = config.inline;
            if (config.mode) fpConfig.mode = config.mode;
            if (config.onChange) fpConfig.onChange = config.onChange;
            if (config.onDayCreate) fpConfig.onDayCreate = config.onDayCreate;
            if (config.onYearChange) fpConfig.onYearChange = config.onYearChange;
            if (config.onMonthChange) fpConfig.onMonthChange = config.onMonthChange;
            if (config.navTitles) fpConfig.navTitles = config.navTitles;

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
            var currentIssueDateStr = opts.currentIssueDate || null;
            var onDayClickCb = opts.onDayClick || null;

            var config = Object.assign({}, this.defaults, opts, {
                inline: true,
                onDayCreate: function (dObj, dStr, fp, dayElem) {
                    var d = dayElem.dateObj;
                    var dateStr =
                        d.getFullYear() +
                        '-' +
                        String(d.getMonth() + 1).padStart(2, '0') +
                        '-' +
                        String(d.getDate()).padStart(2, '0');

                    if (hasDataDates.indexOf(dateStr) !== -1) {
                        dayElem.classList.add('-has-data-');
                    }
                    if (dateStr === currentIssueDateStr) {
                        dayElem.classList.add('-current-issue-');
                    }
                },
                onChange: function (selectedDates) {
                    if (!selectedDates || !selectedDates.length) return;
                    var d = selectedDates[0];
                    var dateStr =
                        d.getFullYear() +
                        '-' +
                        String(d.getMonth() + 1).padStart(2, '0') +
                        '-' +
                        String(d.getDate()).padStart(2, '0');

                    if (hasDataDates.indexOf(dateStr) !== -1 && onDayClickCb) {
                        onDayClickCb(dateStr, d);
                    }
                },
            });

            delete config.hasDataDates;
            delete config.currentIssueDate;
            delete config.onDayClick;

            return this.create(containerEl, config);
        },

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
