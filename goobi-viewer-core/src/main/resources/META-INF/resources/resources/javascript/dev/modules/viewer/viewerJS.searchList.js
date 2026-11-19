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
 * Module which sets up the functionality for search list.
 *
 * @version 3.2.0
 * @module viewerJS.searchList
 * @requires jQuery
 */
var viewerJS = (function (viewer) {
    'use strict';

    var _debug = false;
    var _promise = null;
    var _childHits = null;
    var _searchListStyle = '';
    var _searchListShowThumbs = false;
    var _defaults = {
        contextPath: '',
        maxChildHitsToRenderOnStart: 5,
        resetSearchSelector: '#resetCurrentSearch',
        searchInputSelector: '#currentSearchInput',
        searchTriggerSelector: '#slCurrentSearchTrigger',
        saveSearchModalSelector: '#saveSearchModal',
        saveSearchInputSelector: '#saveSearchInput',
        exportDownloadSelector: '.search-list__export-dropdown-link',
        exportWrapperSelector: '.search-list__export-dropdown-wrapper',
        exportLoaderSelector: '.search-list__export-loader',
        exportToggleSelector: '.search-list__export-dropdown-toggle',
        hitContentLoaderSelector: '.search-list__loader',
        hitContentSelector: '.search-list__hit-content',
        listStyle: '',
        msg: {
            getMoreChildren: 'Mehr Treffer laden',
        },
    };

    /**
     * Triggers a browser download of the given blob under the given file name.
     *
     * @param {Blob} blob the file content to save
     * @param {string} filename the download file name
     */
    function _saveBlob(blob, filename) {
        var objectUrl = URL.createObjectURL(blob);
        var a = document.createElement('a');
        a.href = objectUrl;
        a.download = filename || 'search_export';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        setTimeout(function () {
            URL.revokeObjectURL(objectUrl);
        }, 1000);
    }

    viewer.searchList = {
        /**
         * Method to initialize the search list features.
         *
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         */
        init: function (config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.searchList.init');
                console.log('##############################');
                console.log('viewer.searchList.init: config = ', config);
            }

            $.extend(true, _defaults, config);

            // init bs tooltips
            viewerJS.helper.initBsFeatures;

            // focus save search modal input on show
            $(_defaults.saveSearchModalSelector).on('shown.bs.modal', function () {
                $(_defaults.saveSearchInputSelector).focus();
            });

            /**
             * Keep trigger buttons for modal dialogs in sync with the modal state.
             * Applies a dedicated modifier class instead of Bootstrap's `.active`
             * to avoid conflicting styles and ensures the focus outline/hover
             * feedback resets correctly after the modal closes.
             *
             * @param {string} triggerSelector  Button selector used to open the modal
             * @param {string} modalSelector    CSS selector of the modal element
             */
            function bindModalTriggerState(triggerSelector, modalSelector) {
                var $modal = $(modalSelector);
                if ($modal.length === 0) {
                    return;
                }

                var $triggers = $(triggerSelector).attr('aria-pressed', false);

                function setTriggerState(isOpen) {
                    $triggers = $(triggerSelector);
                    if ($triggers.length === 0) {
                        return;
                    }
                    $triggers.toggleClass('is-modal-trigger-open', isOpen).attr('aria-pressed', isOpen);

                    if (!isOpen) {
                        $triggers.removeClass('suppress-hover no-hover');
                        setTimeout(function () {
                            $triggers.each(function () {
                                this.blur();
                            });
                        }, 0);
                    } else {
                        $triggers.each(function () {
                            var $trigger = $(this);
                            $trigger.closest('[data-toggle="tooltip"]').tooltip('hide');
                            $trigger.find('[data-toggle="tooltip"]').tooltip('hide');
                        });
                    }
                }

                $modal
                    .on('show.bs.modal', function () {
                        setTriggerState(true);
                    })
                    .on('hidden.bs.modal', function () {
                        setTriggerState(false);
                    });

                $(document).on('click', triggerSelector, function () {
                    var $btn = $(this);
                    var btnEl = this;
                    $btn.addClass('suppress-hover');
                    setTimeout(function () {
                        $btn.removeClass('suppress-hover');
                    }, 200);
                    setTimeout(function () {
                        if (btnEl && typeof btnEl.blur === 'function') {
                            btnEl.blur();
                        }
                    }, 40);
                });
            }

            bindModalTriggerState('.search-list__help .btn.btn--icon', '#searchHelpModal');
            bindModalTriggerState('.search-list__save-search .btn.btn--icon', _defaults.saveSearchModalSelector);

            /**
             * Blur icon buttons that act as external links after pointer activation.
             * Prevents sticky hover/focus styles when a new browser tab/window opens.
             *
             * Keyboard activation keeps focus for accessibility.
             *
             * @param {string} selector
             */
            function blurOnPointerClick(selector) {
                $(document).on('click', selector, function (event) {
                    if (event && typeof event.detail === 'number' && event.detail === 0) {
                        return;
                    }

                    var el = this;
                    $(el).closest('[data-toggle="tooltip"]').tooltip('hide');

                    window.requestAnimationFrame(function () {
                        el.blur();
                    });
                });
            }

            blurOnPointerClick('.search-list__rss .btn.btn--icon');

            // set focus class if searchfield is focused
            $('#currentSearchInput').on({
                focus: function () {
                    $(this).prev().addClass('focus');
                },
                blur: function () {
                    $(this).prev().removeClass('focus');
                },
            });

            // reset current search and redirect to standard search
            $(_defaults.resetSearchSelector).on('click', function () {
                $(_defaults.searchInputSelector).val('');
                location.href = _defaults.contextPath + '/search/';
            });

            // Search hit export downloads (excel/csv/ris/endnote/bibtex): show a spinner while the file is
            // generated, then trigger the download. Access-denied (403) and timeout (503) responses are
            // surfaced as messages instead of navigating the page away. Delegated so it also covers hits
            // rendered after init. Falls back to following the link if fetch is unavailable.
            $(document).on('click', _defaults.exportDownloadSelector, function (event) {
                if (typeof window.fetch !== 'function') {
                    return;
                }
                event.preventDefault();

                var link = this;
                var $wrapper = $(link).closest(_defaults.exportWrapperSelector);
                var $loader = $wrapper.find(_defaults.exportLoaderSelector);
                var $toggle = $wrapper.find(_defaults.exportToggleSelector);
                var messages = $wrapper.data();

                $toggle.addClass('d-none');
                $loader.removeClass('d-none');

                fetch(link.href, { credentials: 'same-origin' })
                    .then(function (response) {
                        if (response.ok) {
                            var disposition = response.headers.get('Content-Disposition') || '';
                            var match = /filename="?([^";]+)"?/.exec(disposition);
                            var filename = match ? match[1] : 'search_export';
                            return response.blob().then(function (blob) {
                                _saveBlob(blob, filename);
                            });
                        }
                        if (response.status === 403) {
                            throw messages.msgDenied;
                        }
                        if (response.status === 503) {
                            throw messages.msgTimeout;
                        }
                        throw messages.msgError;
                    })
                    .catch(function (err) {
                        window.alert(typeof err === 'string' && err ? err : messages.msgError || 'Export failed');
                    })
                    .then(function () {
                        $loader.addClass('d-none');
                        $toggle.removeClass('d-none');
                    });
            });

            //** DEFINE DIFFERENT LIST STYLES FUNCTIONS */

            function setListViewDetails() {
                var $views = $('.search-list__views button');
                $views.removeClass('active').attr('aria-pressed', 'false');
                var $active = $('[data-view="search-list-default"]').addClass('active');
                $active.attr('aria-pressed', 'true');
                $('[data-toggle="hit-content"]').show();
                $('.search-list__hit-thumbnail').css({ height: 'auto' });
                stopExtendedGridLayout();
                $('.search-list__hits').css('opacity', 0).removeClass('grid').removeClass('grid-extended').removeClass('list-view');

                // set list style in local storage
                // sessionStorage.setItem( 'searchListStyle', 'default' );

                // remove header background
                $('.search-list__hit-thumbnail').css('background-image', 'none');

                $('.search-list__hits').fadeTo(300, 1);
            }

            function setListViewGrid() {
                var $views = $('.search-list__views button');
                $views.removeClass('active').attr('aria-pressed', 'false');
                var $active = $('[data-view="search-list-grid"]').addClass('active');
                $active.attr('aria-pressed', 'true');
                $('[data-toggle="hit-content"]').hide();
                $('.search-list__hit-thumbnail').css({ height: 'auto' });
                stopExtendedGridLayout();
                $('.search-list__hits').css('opacity', 0).removeClass('list-view').removeClass('grid-extended').addClass('grid');

                // set list style in local storage
                // sessionStorage.setItem( 'searchListStyle', 'grid' );

                // hide thumbnail and set src to header background
                $('.search-list__hit-thumbnail img').each(function () {
                    $(this).on('load', function (event) {
                        var imgUrl = $(event.currentTarget).attr('src');
                        if (imgUrl) {
                            $(event.currentTarget)
                                .parents('.search-list__hit-thumbnail')
                                .css('background-image', 'url("' + imgUrl + '")');
                        }
                    });

                    if (this.complete) {
                        var imgUrl = $(this).attr('src');
                        if (imgUrl) {
                            $(this)
                                .parents('.search-list__hit-thumbnail')
                                .css('background-image', 'url("' + imgUrl + '")');
                        }
                    }
                });

                $('.search-list__hits').fadeTo(300, 1);
            }

            function setListViewGridExtended() {
                // builds on the regular grid
                setListViewGrid();
                $('.search-list__hits').addClass('grid-extended');
                startExtendedGridLayout();
            }

            //** EXTENDED GRID: MASONRY LAYOUT */

            var _extendedGrids = [];

            function readPx(styles, name, fallback) {
                var value = parseFloat(styles.getPropertyValue(name));
                return isNaN(value) ? fallback : value;
            }

            /** Places the cards in a small/big checkerboard with an offset between columns. */
            function layoutExtendedGrid(grid) {
                var styles = getComputedStyle(grid);
                var gap = parseFloat(styles.columnGap) || 0;
                var cols = styles.gridTemplateColumns.split(' ').length;
                var small = readPx(styles, '--search-list-grid-extended-height-small', 270);
                var big = readPx(styles, '--search-list-grid-extended-height-big', 330);
                var step = readPx(styles, '--search-list-grid-extended-height-step', 20);
                var minOffset = readPx(styles, '--search-list-grid-extended-min-offset', 40);
                // bottom edge per column (px)
                var bottom = new Array(cols).fill(0);

                grid.classList.add('-laidOut');

                Array.prototype.forEach.call(grid.children, function (item, i) {
                    var row = Math.floor(i / cols);
                    var col = i % cols;
                    var isBig = (row + col) % 2 === 1;
                    var base = isBig ? big : small;
                    var candidates = [base, base + step, base + 2 * step];

                    // distance to the neighbouring columns
                    var offset = function (height) {
                        var edge = bottom[col] + height + gap;
                        var distances = [col - 1, col + 1]
                            .filter(function (c) {
                                return c >= 0 && c < cols;
                            })
                            .map(function (c) {
                                return Math.abs(edge - bottom[c]);
                            });
                        return distances.length ? Math.min.apply(null, distances) : Infinity;
                    };

                    // first height with enough offset, else the largest
                    var height = candidates.find(function (c) {
                        return offset(c) >= minOffset;
                    });
                    if (height === undefined) {
                        height = candidates.reduce(function (a, c) {
                            return offset(c) > offset(a) ? c : a;
                        });
                    }

                    item.style.height = height + 'px';
                    item.style.gridColumn = String(col + 1);
                    item.style.gridRow = bottom[col] + 1 + ' / span ' + Math.ceil(height + gap);
                    bottom[col] += Math.ceil(height + gap);
                });
            }

            function startExtendedGridLayout() {
                stopExtendedGridLayout();
                $('.search-list__hits.grid-extended .search-list__hits-list-items').each(function () {
                    var grid = this;
                    var entry = { grid: grid, frame: 0, observer: null };
                    // avoid a duplicate tab stop
                    $(grid).find('.search-list__hit-thumbnail a:not([tabindex])').attr({ tabindex: '-1', 'aria-hidden': 'true', 'data-extended-grid-hidden': '' });
                    var schedule = function () {
                        cancelAnimationFrame(entry.frame);
                        entry.frame = requestAnimationFrame(function () {
                            layoutExtendedGrid(grid);
                        });
                    };
                    if (typeof ResizeObserver !== 'undefined') {
                        entry.observer = new ResizeObserver(schedule);
                        entry.observer.observe(grid);
                    }
                    _extendedGrids.push(entry);
                    schedule();
                });
            }

            function stopExtendedGridLayout() {
                _extendedGrids.forEach(function (entry) {
                    cancelAnimationFrame(entry.frame);
                    if (entry.observer) {
                        entry.observer.disconnect();
                    }
                    entry.grid.classList.remove('-laidOut');
                    $(entry.grid).find('[data-extended-grid-hidden]').removeAttr('tabindex aria-hidden data-extended-grid-hidden');
                    Array.prototype.forEach.call(entry.grid.children, function (item) {
                        item.style.height = '';
                        item.style.gridColumn = '';
                        item.style.gridRow = '';
                    });
                });
                _extendedGrids = [];
            }

            function setListViewList() {
                var $views = $('.search-list__views button');
                $views.removeClass('active').attr('aria-pressed', 'false');
                var $active = $('[data-view="search-list-list"]').addClass('active');
                $active.attr('aria-pressed', 'true');
                $('[data-toggle="hit-content"]').hide();
                $('.search-list__hit-thumbnail').css({ height: '0' });
                stopExtendedGridLayout();
                $('.search-list__hits').css('opacity', 0).removeClass('grid').removeClass('grid-extended').addClass('list-view');

                // set list style in local storage
                // sessionStorage.setItem( 'searchListStyle', 'list-view' );

                $('.search-list__hits').fadeTo(300, 1);
            }

            // get/set list style from local storage

            if (!_defaults.listStyle) {
                if (sessionStorage.getItem('searchListStyle') == undefined) {
                    sessionStorage.setItem('searchListStyle', 'default');
                    if (_debug) {
                        console.log('no default view set, so it is set now');
                    }
                }
                _searchListStyle = sessionStorage.getItem('searchListStyle');
            } else if (typeof cmsSearchCustomView !== 'undefined' && sessionStorage.getItem(cmsSearchViewlistStyleID) !== null) {
                _searchListStyle = sessionStorage.getItem(cmsSearchViewlistStyleID);
                if (_debug) {
                    console.log('view for THIS special search page (based on page id): ' + sessionStorage.getItem(cmsSearchViewlistStyleID));
                }
            } else {
                _searchListStyle = _defaults.listStyle;
                if (_debug) {
                    console.log('default value used (can be based on CMS comp), nothing else defined: ' + _defaults.listStyle);
                }
            }

            // load thumbnails before appying search list style
            // console.log("Load search hits with style " + _searchListStyle);
            switch (_searchListStyle) {
                case 'default':
                case 'details':
                    setListViewDetails();
                    break;
                case 'grid':
                    setListViewGrid();
                    break;
                case 'grid-extended':
                    setListViewGridExtended();
                    break;
                case 'list-view':
                    setListViewList();
                    break;
            }

            // set default style  on button click
            $('[data-view="search-list-default"]').on('click', function () {
                setListViewDetails();
                sessionStorage.setItem('searchListStyle', 'default');
                if (cmsSearchCustomView == true) {
                    sessionStorage.setItem(cmsSearchViewlistStyleID, 'default');
                    if (_debug) {
                        console.log('set custom view list: default/details');
                    }
                }
            });

            // set grid style on button click
            $('[data-view="search-list-grid"]').on('click', function () {
                setListViewGrid();
                sessionStorage.setItem('searchListStyle', 'grid');
                if (typeof cmsSearchCustomView !== 'undefined' && cmsSearchCustomView == true) {
                    sessionStorage.setItem(cmsSearchViewlistStyleID, 'grid');
                    if (_debug) {
                        console.log('set custom view list: grid');
                    }
                }
            });
            // set list style  on button click
            $('[data-view="search-list-list"]').on('click', function () {
                setListViewList();
                sessionStorage.setItem('searchListStyle', 'list-view');
                if (typeof cmsSearchCustomView !== 'undefined' && cmsSearchCustomView == true) {
                    sessionStorage.setItem(cmsSearchViewlistStyleID, 'list-view');
                    if (_debug) {
                        console.log('set custom view list: list');
                    }
                }
            });

            this.initSubHits();

            // init thumbnail toggle
            let $thumbToggle = $('[data-action="toggle-thumbs"]');
            if ($thumbToggle.length > 0) {
                if (sessionStorage.getItem('searchListShowThumbs') == undefined) {
                    sessionStorage.setItem('searchListShowThumbs', false);
                }
                _searchListShowThumbs = sessionStorage.getItem('searchListShowThumbs').toLowerCase() === 'true';
                this.showSearchListThumbs(_searchListShowThumbs);

                $thumbToggle.on('click', () => {
                    $thumbToggle.trigger('blur');
                    _searchListShowThumbs = !_searchListShowThumbs;
                    this.showSearchListThumbs(_searchListShowThumbs);
                    sessionStorage.setItem('searchListShowThumbs', _searchListShowThumbs);
                    $thumbToggle.tooltip('show');
                });
            }
        },
        showSearchListThumbs: function (show) {
            const $thumbToggle = $('[data-action="toggle-thumbs"]');
            const activeTitle = $thumbToggle.attr('data-title-active');
            const inactiveTitle = $thumbToggle.attr('data-title-inactive');
            if (show) {
                $thumbToggle.addClass('-active').attr('title', activeTitle).attr('aria-checked', true).off('mouseleave.tooltip').off('mouseenter.tooltip');
                $('.search-list__subhit-thumbnail').show();
                $('[data-toggle="tooltip"]').tooltip('dispose');
                viewerJS.helper.initBsFeatures();
            } else {
                $thumbToggle.removeClass('-active').attr('title', inactiveTitle).attr('aria-checked', false).off('mouseleave.tooltip').off('mouseenter.tooltip');
                $('.search-list__subhit-thumbnail').hide();
                $('[data-toggle="tooltip"]').tooltip('dispose');
                viewerJS.helper.initBsFeatures();
            }
        },
        initSubHits: function () {
            document.querySelectorAll('[data-toggle="hit-content"]').forEach((button) => {
                if (parseInt(button.dataset.childhits) <= _defaults.maxChildHitsToRenderOnStart) {
                    this.openChildHits(button);
                }
            });
        },
        openChildHits: function (button) {
            // console.log("open child hits");
            var $currBtn = $(button);

            let scriptName = button.dataset.loadHitsScript;
            let toggleArea = document.querySelector("div[data-toggle-id='" + button.dataset.toggleId + "']");
            let hitsPopulated = toggleArea.querySelector('[data-hits-populated]').dataset.hitsPopulated;
            if (_debug) {
                console.log('clicked hit-content', button, scriptName, toggleArea, hitsDisplayed, _defaults.childHitsToLoadOnExpand);
            }

            $currBtn.toggleClass('in');
            $currBtn.attr('aria-expanded', $currBtn.hasClass('in') ? 'true' : 'false');
            $(toggleArea).slideToggle();
            if (_debug) console.log('call script ', scriptName, hitsPopulated);
            if (hitsPopulated < _defaults.childHitsToLoadOnExpand) {
                //show loader now already. Otherwise it will only be shown when the post request starts and the request are carried out sequentially
                this.showAjaxLoader(toggleArea.querySelector('.search-list__loader').id);
                window[scriptName](); //execute commandScript to load child hits
            }
        },

        showAjaxLoader: function (loaderId) {
            let loader = document.getElementById(loaderId);
            loader.classList.remove('d-none');
        },
        hideAjaxLoader: function (loaderId) {
            let loader = document.getElementById(loaderId);
            loader.classList.add('d-none');
        },
        initChildHitThumbs: function (dataToggleId) {
            let showThumbs = this.isShowChildHitThumbs();
            if (this.isShowChildHitThumbs()) {
                $('[data-toggle-id=' + dataToggleId + ']')
                    .find('.search-list__subhit-thumbnail')
                    .show();
            } else {
                $('[data-toggle-id=' + dataToggleId + ']')
                    .find('.search-list__subhit-thumbnail')
                    .hide();
            }
        },
        isShowChildHitThumbs() {
            return sessionStorage.getItem('searchListShowThumbs').toLowerCase() === 'true' ? true : false;
        },
    };

    return viewer;
})(viewerJS || {}, jQuery);
