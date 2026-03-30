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
 * <Short Module Description>
 *
 * @version 3.2.0
 * @module viewerJS.calendarPopover
 * @requires jQuery
 */
var viewerJS = (function (viewer) {
    'use strict';

    var _debug = false;
    var _this = null;
    var _currApiCall = '';
    var _json = {};
    var _popoverConfig = {};
    var _popoverContent = null;
    var _defaults = {
        appUrl: '',
        indexResourceUrl: '',
        calendarWrapperSelector: '.search-calendar__months',
        popoverTriggerSelector: '[data-popover-trigger="calendar-po-trigger"]',
        popoverTitle: 'Bitte übergeben Sie den Titel des Werks',
    };

    viewer.calendarPopover = {
        init: function (config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.calendarPopover.init');
                console.log('##############################');
                console.log('viewer.calendarPopover.init: config - ', config);
            }

            $.extend(true, _defaults, config);

            // TODO #27682 Basti: Fehlermeldung in der Konsole beseitigen, wenn man auf den Tag ein
            // zweites Mal klickt.

            // show popover for current day
            $(document).on('click', _defaults.popoverTriggerSelector, function () {
                _this = $(this);

                let query = _this.attr('data-query');
                let fields = _this.attr('data-fields').split(',');
                let requestBody = JSON.stringify({
                    query: query,
                    resultFields: fields,
                });

                fetch(_defaults.indexResourceUrl, {
                    method: 'POST',
                    body: requestBody,
                    headers: {
                        'Content-Type': 'application/json',
                    },
                })
                    .then((response) => response.json())
                    .then((response) => {
                        _popoverContent = _getPopoverContent(response, _defaults);
                        _popoverConfig = {
                            placement: 'bottom',
                            title: _defaults.popoverTitle,
                            content: _popoverContent,
                            viewport: {
                                selector: _defaults.calendarWrapperSelector,
                            },
                            html: true,
                        }; 

                        $(_defaults.popoverTriggerSelector).popover('dispose');
                        _this.popover(_popoverConfig);
                        _this.one('inserted.bs.popover', function () {
                            var popoverId = _this.attr('aria-describedby');
                            $('#' + popoverId).addClass('search-calendar__day-popover');
                        });
                        _this.one('shown.bs.popover', function () {
                            var $trigger = _this;
                            var popoverId = $trigger.attr('aria-describedby');
                            var $popover = $('#' + popoverId);
                            var focusableSelectors = 'a[href], button:not([disabled]), input:not([disabled]), [tabindex]:not([tabindex="-1"])';
                            $popover.attr('tabindex', '-1');
                            $popover.trigger('focus');

                            $popover.on('keydown.calendarPopover', function (e) {
                                if (e.key === 'Escape') { 
                                    // Escape: close popover, return focus to trigger
                                    $trigger.popover('dispose');
                                    $trigger.trigger('focus');
                                } else if (e.key === 'Tab') {
                                    var $focusable = $popover.find('a, button').filter(':visible');
                                    var $first = $focusable.first();
                                    var $last = $focusable.last();

                                    if (e.shiftKey && ($(e.target).is($first) || $(e.target).is($popover))) {
                                        // Shift+Tab on first item: return focus to trigger
                                        e.preventDefault();
                                        $trigger.popover('dispose');
                                        $trigger.trigger('focus');
                                    } else if (!e.shiftKey && $(e.target).is($last)) {
                                        // Tab on last item: move focus to next element after trigger
                                        e.preventDefault();
                                        $trigger.popover('dispose');
                                        var $all = $(focusableSelectors).filter(':visible');
                                        $all.eq($all.index($trigger) + 1).trigger('focus');
                                    }
                                }
                            });
                        });
                        _this.popover('show');
                    })
                    .catch((error) => {
                        console.error('Error calling ' + _defaults.indexResourceUrl + ' :' + error);
                    });
            });

            // remove all popovers by clicking on body
            $(document).on('click', function (event) {
                if ($(event.target).closest(_defaults.popoverTriggerSelector).length) {
                    return;
                } else {
                    $(_defaults.popoverTriggerSelector).popover('dispose');
                }
            });
        },
    };

    /**
     * Method to render the popover content.
     *
     * @method _getPopoverContent
     * @param {Object} data A JSON-Object which contains the necessary data.
     * @param {Object} config The config object of the module.
     * @returns {String} The HTML-String of the popover content.
     */
    function _getPopoverContent(data, config) {
        if (_debug) {
            console.log('---------- _getPopoverContent() ----------');
            console.log('_getPopoverContent: data = ', data);
            console.log('_getPopoverContent: config = ', config);
        }

        var workList = '';
        var workListLink = '';

        workList += '<ul class="list">';
        if (data && data.docs) {
            data.docs.forEach((item) => {
                workListLink =
                    config.appUrl + 'image/' + item.PI_TOPSTRUCT + '/' + item.THUMBPAGENO + '/' + item.LOGID + '/';

                workList += '<li>';
                workList += '<a href="' + workListLink + '">';
                workList += item.LABEL;
                workList += '</a>';
                workList += '</li>';
            });
        } else {
            console.error('Unexpected json data: ', data);
        }

        workList += '</ul>';

        return workList;
    }

    return viewer;
})(viewerJS || {}, jQuery);
