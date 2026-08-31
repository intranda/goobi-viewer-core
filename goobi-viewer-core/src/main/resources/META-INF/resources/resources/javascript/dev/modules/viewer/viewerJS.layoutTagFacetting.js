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
 * Module which facets the ALTO layout tags in a sidebar widget and page view. Forked from viewerJS.nerFacetting;
 * layout tags have no fixed type set (filter chips are built dynamically) and no authority URIs.
 *
 * @version 3.2.0
 * @module viewerJS.layoutTagFacetting
 * @requires jQuery
 * @requires Bootstrap
 */
var viewerJS = (function (viewer) {
    'use strict';

    var _debug = false;
    var _json = null;
    var _apiCall = '';
    var _html = '';
    var _pageCount = 0;
    var _sliderHandlePosition = {};
    var _movedSliderHandlePosition = 0;
    var _scaleValue = 0;
    var _sliderScaleHeight = 0;
    var _start = 0;
    var _end = 0;
    var _currentLayoutTagPageRangeSelected = '';
    var _currentLayoutTagPageRange = '';
    var _currentLayoutTagType = '';
    var _allLabels = [];
    var _promise = null;
    var _defaults = {
        currentPage: '',
        baseUrl: '',
        apiUrl: '/api/v1/records/',
        workId: '',
        pageCount: 0,
        overviewTrigger: '',
        overviewContent: '',
        sectionTrigger: '',
        sectionContent: '',
        facettingTrigger: '',
        setTagRange: '',
        setTagRangeOverview: '',
        setTagRangeSection: '',
        slider: '',
        sliderScale: '',
        sectionTags: '',
        currentTags: '',
        sliderHandle: '',
        sliderSectionStripe: '',
        recurrenceNumber: 0,
        recurrenceSectionNumber: 0,
        sidebarRight: false,
        loader: '',
        msg: {
            noJSON: 'Es konnten keine Daten abgerufen werden.',
            emptyTag: 'Keine Tags vorhanden',
            page: 'Seite',
            tags: 'Tags',
            all: 'Alle',
        },
    };

    viewer.layoutTagFacetting = {
        /**
         * Method to initialize the NER-Widget or NER-View.
         *
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.currentPage The name of the current page.
         * @param {String} config.baseUrl The root URL.
         * @param {String} config.apiUrl The base URL for the API-Calls.
         * @param {String} config.workId The ID of the current work.
         * @param {String} config.overviewTrigger The ID/Class of the overview trigger.
         * @param {String} config.overviewContent The ID/Class of the content section from
         * overview.
         * @param {String} config.sectionTrigger The ID/Class of the section trigger.
         * @param {String} config.sectionContent The ID/Class of the content section from
         * section.
         * @param {String} config.facettingTrigger The ID/Class of the facetting trigger.
         * @param {String} config.setTagRange The ID/Class of the select menu for the
         * range.
         * @param {String} config.slider The ID/Class of the tag range slider.
         * @param {String} config.sliderScale The ID/Class of the slider scale.
         * @param {String} config.sectionTags The ID/Class of the tag section.
         * @param {String} config.currentTags The ID/Class of the tag container.
         * @param {String} config.sliderHandle The ID/Class of the slider handle.
         * @param {String} config.sliderSectionStripe The ID/Class of the range stripe on
         * the slider.
         * @param {Number} config.recurrenceNumber The number of displayed tags in a row.
         * @param {Number} config.recurrenceSectionNumber The number of displayed tags in
         * a section.
         * @param {Boolean} config.sidebarRight If true, the current tag row will show up
         * to the left of the sidebar widget.
         * @param {String} config.loader The ID/Class of the AJAX-Loader.
         * @param {Object} config.msg An object of strings for multi language use.
         */
        init: function (config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.layoutTagFacetting.init');
                console.log('##############################');
                console.log('viewer.layoutTagFacetting.init: config - ', config);
            }

            $.extend(true, _defaults, config);

            if (viewer.localStoragePossible) {
                $(_defaults.loader).show();

                _cleanUpLocalStorage();

                $(_defaults.setTagRange).find('option').attr('selected', false);

                if (_defaults.currentPage === 'layouttagfacetting') {
                    $(_defaults.setTagRangeOverview).find('option[value="1"]').prop('selected', true);
                } else {
                    $(_defaults.setTagRangeOverview).find('option[value="10"]').prop('selected', true);
                }

                _resetFacettingIcons();

                if (_defaults.currentPage === 'layouttagfacetting') {
                    _apiCall = _getAllTagsOfARange(1, '-');
                } else {
                    _apiCall = _getAllTagsOfARange(10, '-');
                }

                _promise = viewer.helper.getRemoteData(_apiCall);

                _promise
                    .then(function (json) {
                        _json = json;

                        if ($(_defaults.overviewContent).html() === '') {
                            _showOverview(_json);
                        }
                    })
                    .then(null, _renderError);

                /**
                 * Event if overview tab is clicked.
                 */
                $(_defaults.overviewTrigger)
                    .off('click.layoutTagFacetting')
                    .on('click.layoutTagFacetting', function () {
                        $(_defaults.loader).show();

                        $(_defaults.setTagRange).find('option').attr('selected', false);

                        if (_defaults.currentPage === 'layouttagfacetting') {
                            $(_defaults.setTagRangeOverview).find('option[value="1"]').prop('selected', true);
                            sessionStorage.setItem('currentLayoutTagPageRange', '1');
                        } else {
                            $(_defaults.setTagRangeOverview).find('option[value="10"]').prop('selected', true);
                            sessionStorage.setItem('currentLayoutTagPageRange', '10');
                        }
                        _currentLayoutTagPageRange = sessionStorage.getItem('currentLayoutTagPageRange');

                        sessionStorage.setItem('currentLayoutTagType', '-');
                        _currentLayoutTagType = sessionStorage.getItem('currentLayoutTagType');

                        _resetFacettingIcons();

                        if ($(this).parent().hasClass('active')) {
                            console.info('Overview is already active.');
                        } else {
                            if (_defaults.currentPage === 'layouttagfacetting') {
                                _apiCall = _getAllTagsOfARange(1, '-');
                            } else {
                                _apiCall = _getAllTagsOfARange(10, '-');
                            }

                            _promise = viewer.helper.getRemoteData(_apiCall);

                            _promise.then(_showOverview).then(null, _renderError);
                        }
                    });

                /**
                 * Event if section tab is clicked.
                 */
                $(_defaults.sectionTrigger)
                    .off('click.layoutTagFacetting')
                    .on('click.layoutTagFacetting', function () {
                        $(_defaults.loader).show();

                        $(_defaults.setTagRange).find('option').attr('selected', false);

                        if (_defaults.currentPage === 'layouttagfacetting') {
                            $(_defaults.setTagRangeSection).find('option[value="5"]').prop('selected', true);
                        } else {
                            $(_defaults.setTagRangeSection).find('option[value="10"]').prop('selected', true);
                        }

                        _resetFacettingIcons();

                        if (_defaults.currentPage === 'layouttagfacetting') {
                            sessionStorage.setItem('currentLayoutTagPageRange', 5);
                        } else {
                            sessionStorage.setItem('currentLayoutTagPageRange', 10);
                        }
                        _currentLayoutTagPageRange = sessionStorage.getItem('currentLayoutTagPageRange');
                        sessionStorage.setItem('currentLayoutTagType', '-');
                        _currentLayoutTagType = sessionStorage.getItem('currentLayoutTagType');

                        if ($(this).parent().hasClass('active')) {
                            console.info('Section is already active.');
                        } else {
                            _renderSection();

                            $(_defaults.sliderSectionStripe).css('top', '0px');
                        }
                    });

                /**
                 * Event if select menu changes.
                 */
                $(_defaults.setTagRange)
                    .off('change.layoutTagFacetting')
                    .on('change.layoutTagFacetting', function () {
                        var currVal = $(this).val();
                        _currentLayoutTagType = sessionStorage.getItem('currentLayoutTagType');

                        $(_defaults.loader).show();

                        sessionStorage.setItem('currentLayoutTagPageRange', currVal);
                        _currentLayoutTagPageRange = sessionStorage.getItem('currentLayoutTagPageRange');

                        if ($(this).hasClass('overview')) {
                            if (_currentLayoutTagType === null || _currentLayoutTagType === '') {
                                _currentLayoutTagType = '-';
                            }
                            _apiCall = _getAllTagsOfARange(currVal, _currentLayoutTagType);

                            _promise = viewer.helper.getRemoteData(_apiCall);

                            _promise.then(_showOverview).then(null, _renderError);
                        } else {
                            _renderSection();

                            if (_currentLayoutTagPageRange > _pageCount) {
                                $(_defaults.sliderSectionStripe).css({
                                    top: '0px',
                                    height: '600px',
                                });
                            } else {
                                $(_defaults.sliderSectionStripe).css({
                                    top: '0px',
                                    height: '100px',
                                });
                            }
                        }
                    });

                /**
                 * Event if facetting icons are clicked.
                 */
                $('.filter-tags')
                    .off('click.layoutTagFacetting')
                    .on('click.layoutTagFacetting', _defaults.facettingTrigger, function () {
                        var currType = $(this).attr('data-type');

                        $(_defaults.loader).show();

                        sessionStorage.setItem('currentLayoutTagType', currType);
                        _currentLayoutTagType = sessionStorage.getItem('currentLayoutTagType');

                        if (_defaults.currentPage === 'layouttagfacetting') {
                            if (_currentLayoutTagPageRange == null || _currentLayoutTagPageRange === '') {
                                _currentLayoutTagPageRange = sessionStorage.setItem('currentLayoutTagPageRange', 1);
                            }
                        } else {
                            if (_currentLayoutTagPageRange == null || _currentLayoutTagPageRange === '') {
                                _currentLayoutTagPageRange = sessionStorage.setItem('currentLayoutTagPageRange', 10);
                            }
                        }
                        _currentLayoutTagPageRange = sessionStorage.getItem('currentLayoutTagPageRange');

                        $('.facetting-trigger').removeClass('active');
                        $(this).addClass('active');
                        $('.reset-filter').show();

                        if ($(this).parent().parent().parent().attr('id') === 'overview') {
                            _apiCall = _getAllTagsOfARange(_currentLayoutTagPageRange, _currentLayoutTagType);

                            _promise = viewer.helper.getRemoteData(_apiCall);

                            _promise
                                .then(function (json) {
                                    _showOverview(json);

                                    if (_currentLayoutTagType === '-') {
                                        $('.facetting-trigger').addClass('active');
                                    }
                                })
                                .then(null, _renderError);
                        } else {
                            _renderSection();

                            if ($(this).parent().hasClass('reset-filter')) {
                                $(this).parent().hide();
                            }
                            if (_currentLayoutTagType === '-') {
                                $('.facetting-trigger').addClass('active');
                            }
                            $(_defaults.sliderSectionStripe).css('top', '0px');
                        }
                    });
            } else {
                $('.facetting-content')
                    .empty()
                    .append(viewer.helper.renderAlert('alert-danger', '<strong>Deactivated: </strong>Not possible to write in local Storage!', false));
            }
        },
    };

    /**
     * Method to render the NER overview.
     *
     * @method _renderOverview
     * @param {Object} data A JSON-Object.
     * @returns {Sting} A HTML-String which renders the overview.
     */
    function _renderOverview(data) {
        if (_debug) {
            console.log('---------- _renderOverview() ----------');
            console.log('_renderOverview: data = ', data);
        }

        _renderFilterChips(data);

        var linkBase = _defaults.baseUrl + (_defaults.currentPage === 'layouttagfacetting' ? '/image/' : '/' + _defaults.currentPage + '/') + _defaults.workId + '/';

        _html = '';
        _html += '<ul class="overview-scale">';

        $.each(data.pages, function (p, page) {
            _html += '<li>';
            _html += '<div class="page-number">';
            if (data.rangeSize != 1 && (page.firstPage !== undefined || page.lastPage !== undefined)) {
                _html += '<a href="' + linkBase + page.firstPage + '/">' + page.firstPage + '-' + page.lastPage + '</a>';
            } else {
                _html += '<a href="' + linkBase + page.pageOrder + '/">' + page.pageOrder + '</a>';
            }
            _html += '</div>';
            _html += '<div class="tag-container">';

            if (page.tags.length === 0 || page.tags.length === 'undefined') {
                _html += '<span class="page-tag empty">' + _defaults.msg.emptyTag + '</span>';
            } else {
                $.each(page.tags, function (t, tag) {
                    _html += '<span class="page-tag ' + tag.type + '">' + _escapeHtml(tag.value) + '</span>';
                });
            }
            _html += '</div>';
            _html += '</li>';
        });
        _html += '</ul>';

        $(_defaults.overviewContent)
            .hide()
            .html(_html)
            .find('.tag-container')
            .each(function () {
                $(this).children('.page-tag').slice(_defaults.recurrenceNumber).remove();
            });
        $(_defaults.overviewContent).show();

        $(_defaults.loader).hide();

        $('.tag-container').on({
            mouseover: function () {
                var $this = $(this);

                _showCurrentTags($this);
            },
            mouseout: function () {
                _hideCurrentTags();
            },
        });
    }

    /**
     * Populates the (initially empty) ".filter-tags" lists with one chip per distinct layout-tag label seen so far,
     * plus a reset ("all") chip. Layout tags have no fixed type set, so — unlike the NER widget — the chips are
     * discovered from the data instead of being hardcoded in the widget markup. Labels accumulate across responses,
     * so rendering a FILTERED response keeps the chips of all other labels selectable. Preserves the currently
     * selected label's active state.
     *
     * @method _renderFilterChips
     * @param {Object} data the DocumentReference JSON (pages[].tags[].value)
     */
    function _renderFilterChips(data) {
        if (data && data.pages) {
            $.each(data.pages, function (p, page) {
                $.each(page.tags || [], function (t, tag) {
                    if (tag.value && _allLabels.indexOf(tag.value) === -1) {
                        _allLabels.push(tag.value);
                    }
                });
            });
        }
        _allLabels.sort();

        var selected = _currentLayoutTagType;
        if (selected === null || selected === '') {
            selected = '-';
        }

        var chips = '';
        chips += '<li class="reset-filter"><a class="facetting-trigger' + (selected === '-' ? ' active' : '') + '" data-type="-">' + _escapeHtml(_defaults.msg.all) + '</a></li>';
        $.each(_allLabels, function (i, label) {
            chips += '<li><a class="facetting-trigger' + (selected === label ? ' active' : '') + '" data-type="' + _escapeHtml(label) + '">' + _escapeHtml(label) + '</a></li>';
        });

        $('.filter-tags').html(chips);
    }

    /**
     * Method which renders the overview for the given JSON, or the noJSON message when
     * the response carries no pages.
     *
     * @method _showOverview
     * @param {Object} json A JSON-Object.
     */
    function _showOverview(json) {
        _json = json;

        if (_json && _json.pages && _json.pages.length) {
            _renderOverview(_json);
        } else {
            $(_defaults.overviewContent).html(viewer.helper.renderAlert('alert-danger', _defaults.msg.noJSON, true));
            $(_defaults.loader).hide();
        }
    }

    /**
     * Method which shows the current tag row in a tooltip.
     *
     * @method _showCurrentTags
     * @param {Object} $obj An jQuery object of the current tag row.
     */
    function _showCurrentTags($obj) {
        var content = $obj.html();
        var pos = $obj.position();

        if (_defaults.currentPage === 'layouttagfacetting') {
            $(_defaults.currentTags)
                .html(content)
                .css({
                    display: 'block',
                    top: pos.top + 25 + 'px',
                });
        } else if (_defaults.sidebarRight) {
            $(_defaults.currentTags)
                .addClass('right')
                .html(content)
                .css({
                    display: 'block',
                    top: pos.top - 2 + 'px',
                    left: 'auto',
                    right: '100%',
                });
        } else {
            $(_defaults.currentTags)
                .html(content)
                .css({
                    display: 'block',
                    top: pos.top - 2 + 'px',
                });
        }
    }

    /**
     * Method which hides the current tag row tooltip.
     *
     * @method _hideCurrentTags
     */
    function _hideCurrentTags() {
        $(_defaults.currentTags).hide();
    }

    /**
     * Method to render the NER section.
     *
     * @method _renderSection
     * @param {Object} data A JSON-Object.
     */
    function _renderSection() {
        if (_debug) {
            console.log('---------- _renderSection() ----------');
            console.log('_renderSection: _currentLayoutTagPageRange = ', _currentLayoutTagPageRange);
            console.log('_renderSection: _currentLayoutTagType = ', _currentLayoutTagType);
        }

        if (_defaults.pageCount > 0) {
            _initSection(_defaults.pageCount);
        } else {
            _apiCall = _getAllTags();

            _promise = viewer.helper.getRemoteData(_apiCall);

            _promise
                .then(function (workCall) {
                    _initSection(_getPageCount(workCall));
                })
                .then(null, _renderError);
        }
    }

    /**
     * Method which initializes the section tab for a known page count: renders the
     * scale, mounts the slider and loads the initial section tags.
     *
     * @method _initSection
     * @param {Number} pageCount The page count of the current work.
     */
    function _initSection(pageCount) {
        _pageCount = pageCount;

        if (_currentLayoutTagPageRange === null || _currentLayoutTagPageRange === '') {
            _currentLayoutTagPageRange = sessionStorage.getItem('currentLayoutTagPageRange');
        }
        if (_currentLayoutTagType === null || _currentLayoutTagType === '') {
            _currentLayoutTagType = sessionStorage.getItem('currentLayoutTagType');
        }

        if (_defaults.currentPage === 'layouttagfacetting') {
            $('#sliderScale .scale-page.end').html(_pageCount);
        } else {
            if (_pageCount > 1000) {
                $('#sliderScale .scale-page.end').html('999+');
            } else {
                $('#sliderScale .scale-page.end').html(_pageCount);
            }
        }

        $(_defaults.slider).slider({
            orientation: 'vertical',
            range: false,
            min: 1,
            max: _pageCount,
            value: _pageCount,
            slide: function (event, ui) {
                _sliderHandlePosition = $(_defaults.sliderHandle).position();
                _scaleValue = _pageCount + 1 - ui.value;

                $('.page-bubble').show();
                _renderPageBubble(_scaleValue);
            },
            start: function () {
                _sliderHandlePosition = $(_defaults.sliderHandle).position();
                _movedSliderHandlePosition = _sliderHandlePosition.top;
            },
            stop: function (event, ui) {
                _currentLayoutTagType = sessionStorage.getItem('currentLayoutTagType');
                _sliderScaleHeight = $(_defaults.sliderScale).height();

                if (_currentLayoutTagPageRange > _pageCount) {
                    $(_defaults.sliderSectionStripe).css({
                        top: '0px',
                        height: '600px',
                    });
                } else {
                    if (_sliderHandlePosition.top < 100) {
                        $(_defaults.sliderSectionStripe).animate({
                            top: '0px',
                            height: '100px',
                        });
                    } else if (_sliderHandlePosition.top > 100) {
                        if (_sliderHandlePosition.top > 500) {
                            $(_defaults.sliderSectionStripe).animate({
                                top: _sliderScaleHeight - 100 + 'px',
                                height: '100px',
                            });
                        } else {
                            if (_movedSliderHandlePosition < _sliderHandlePosition.top) {
                                $(_defaults.sliderSectionStripe).animate({
                                    top: _sliderHandlePosition.top - 25 + 'px',
                                    height: '100px',
                                });
                            } else {
                                $(_defaults.sliderSectionStripe).animate({
                                    top: _sliderHandlePosition.top - 50 + 'px',
                                    height: '100px',
                                });
                            }
                        }
                    }
                }

                var bounds = _sectionBounds(_scaleValue);
                if (!bounds) {
                    $('.page-bubble').fadeOut();
                    return;
                }
                _start = bounds.start;
                _end = bounds.end;
                _apiCall = _getAllTagsOfPageSection(_start, _end, _currentLayoutTagType);

                _promise = viewer.helper.getRemoteData(_apiCall);

                _promise
                    .then(function (json) {
                        _showSectionTags(json);

                        $('.page-bubble').fadeOut();
                    })
                    .then(null, _renderError);
            },
        });

        _apiCall = _getAllTagsOfPageSection(0, _currentLayoutTagPageRange, _currentLayoutTagType);

        _promise = viewer.helper.getRemoteData(_apiCall);

        _promise
            .then(function (json) {
                _showSectionTags(json);

                $(_defaults.loader).hide();
            })
            .then(null, _renderError);
    }

    /**
     * Method which renders the tags in the section area.
     *
     * @method _renderSectionTags
     * @param {Object} data A JSON-Object.
     * @returns {Sting} A HTML-String which renders the tag section.
     */
    function _renderSectionTags(data) {
        if (_debug) {
            console.log('---------- _renderSectionTags() ----------');
            console.log('_renderSectionTags: data - ', data);
        }

        _html = '';
        $.each(data.pages, function (p, page) {
            if (page.tags.length === 0 || page.tags.length === 'undefined') {
                _html += '';
            } else {
                $.each(page.tags, function (t, tag) {
                    if (_defaults.currentPage === 'layouttagfacetting') {
                        if (tag.counter < 10) {
                            _html += '<span class="page-tag ' + tag.type + '" style="font-size: 1.' + tag.counter + 'rem;">' + _escapeHtml(tag.value) + '</span>';
                        } else {
                            _html += '<span class="page-tag ' + tag.type + '" style="font-size: 2rem;">' + _escapeHtml(tag.value) + '</span>';
                        }
                    } else {
                        if (tag.counter < 10) {
                            _html += '<span class="page-tag ' + tag.type + '" style="font-size: 1' + tag.counter + 'px;">' + _escapeHtml(tag.value) + '</span>';
                        } else {
                            _html += '<span class="page-tag ' + tag.type + '" style="font-size: 19px;">' + _escapeHtml(tag.value) + '</span>';
                        }
                    }
                });
            }
        });

        return _html;
    }

    /**
     * Method which renders a span showing the current page section.
     *
     * @method _renderPageBubble
     * @param {Number} page The current pagenumber.
     */
    function _renderPageBubble(page) {
        if (_debug) {
            console.log('---------- _renderPageBubble() ----------');
            console.log('_renderPageBubble: page - ', page);
        }

        var pageBubble = '';
        var bounds = _sectionBounds(page);

        if (bounds) {
            _start = bounds.start;
            _end = bounds.end;
            pageBubble = '<span class="page-bubble">' + _start + '-' + _end + '</span>';
        }

        $('#sliderVertical .ui-slider-handle').html(pageBubble);
    }

    /**
     * Method which renders an AJAX error into the facetting content and logs it.
     *
     * @method _renderError
     * @param {Object} error The jqXHR error object.
     */
    function _renderError(error) {
        $('.facetting-content')
            .empty()
            .append(viewer.helper.renderAlert('alert-danger', '<strong>Status: </strong>' + error.status + ' ' + error.statusText, false));
        console.error('ERROR: viewer.layoutTagFacetting.init - ', error);
    }

    /** Half-window offsets per page-range setting (start/end are inclusive). */
    var _RANGE_OFFSETS = {
        5: [-2, 3],
        10: [-5, 5],
        50: [-25, 25],
        100: [-50, 50],
    };

    /**
     * Method which computes the start/end page of the section window around a center
     * page, clamped to [1, _pageCount]; a window larger than the record collapses to
     * the whole record. Null for page ranges without a section window.
     *
     * @method _sectionBounds
     * @param {Number} center The center page of the window.
     * @returns {Object|null} An object with start and end page.
     */
    function _sectionBounds(center) {
        var offsets = _RANGE_OFFSETS[_currentLayoutTagPageRange];
        if (!offsets) {
            return null;
        }
        var start = center + offsets[0];
        var end = center + offsets[1];
        while (start < 1) {
            start++;
            end++;
        }
        while (end > _pageCount && start > 1) {
            start--;
            end--;
        }
        if (end > _pageCount) {
            end = _pageCount;
        }
        return { start: start, end: end };
    }

    /**
     * Method which renders the given JSON into the section tag cloud; an empty
     * result renders a warning alert instead.
     *
     * @method _showSectionTags
     * @param {Object} json A JSON-Object.
     */
    function _showSectionTags(json) {
        _json = json;
        _html = _renderSectionTags(_json);

        if (_html === '') {
            $(_defaults.sectionTags)
                .hide()
                .html(viewer.helper.renderAlert('alert-warning', _defaults.msg.emptyTag, false))
                .show();
        } else {
            $(_defaults.sectionTags)
                .hide()
                .html(_html)
                .each(function () {
                    $(this).children('.page-tag').slice(_defaults.recurrenceSectionNumber).remove();
                });
            $(_defaults.sectionTags).show();
        }
    }

    /**
     * Method which returns the page count of the current work.
     *
     * @method _getPageCount
     * @param {Object} work The current wor object.
     * @returns {Number} The page count of the current work.
     */
    function _getPageCount(work) {
        if (_debug) {
            console.log('---------- _getPageCount() ----------');
            console.log('_getPageCount: work - ', work);
        }

        return work.pages.length;
    }

    /**
     * Method which resets all facetting icons to default
     *
     * @method _resetFacettingIcons
     */
    function _resetFacettingIcons() {
        if (_debug) {
            console.log('---------- _resetFacettingIcons() ----------');
        }

        $('.facetting-trigger').addClass('active');
        $('.reset-filter').hide();
    }

    /**
     * Method which removes all set local storage values.
     *
     * @method _cleanUpLocalStorage
     */
    function _cleanUpLocalStorage() {
        if (_debug) {
            console.log('---------- _cleanUpLocalStorage() ----------');
        }

        sessionStorage.removeItem('currentLayoutTagPageRange');
        sessionStorage.removeItem('currentLayoutTagType');
    }

    /**
     * API-Calls
     */
    // get all tags from all pages: /api/v1/records/{pi}/layout/tags/
    function _getAllTags() {
        let url = _defaults.baseUrl + _defaults.apiUrl + _defaults.workId + '/layout/tags/';
        if (_debug) console.log('get all tags from ', url);
        return url;
    }

    // get all tags of a range: /api/v1/records/{pi}/layout/tags/?step={range}[&type={type}]
    function _getAllTagsOfARange(range, type) {
        let url = _getAllTags() + '?step=' + range;
        if (type && type != '-') {
            url += '&type=' + type;
        }
        return url;
    }

    // get all tags of a page section: /api/v1/records/{pi}/layout/tags/?start={start}&end={end}[&type={type}]
    function _getAllTagsOfPageSection(start, end, type) {
        let url = _getAllTags();
        url += '?start=' + start;
        url += '&end=' + end;
        if (type && type != '-') {
            url += '&type=' + type;
        }
        return url;
    }

    /**
     * Escapes HTML special characters in a string to prevent XSS when the value
     * is concatenated into an HTML string. NER tag labels come from ALTO files via
     * the NER API and contain raw Unicode text — a single escaping pass is both
     * necessary and sufficient.
     *
     * @param str value to escape; null/undefined are treated as empty string
     * @returns {String} HTML-safe string
     */
    function _escapeHtml(str) {
        if (str == null) {
            return '';
        }
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    return viewer;
})(viewerJS || {}, jQuery);
