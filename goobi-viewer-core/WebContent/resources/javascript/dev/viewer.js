(function(){function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s}return e})()({1:[function(require,module,exports){
'use strict';

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
 * Module to manage bookshelves in the current session.
 * 
 * @version 3.2.0
 * @module viewerJS.bookshelvesSession
 * @requires jQuery
 */
var viewerJS = function (viewer) {
    'use strict';

    var _debug = false;
    var _confirmCounter = 0;
    var _defaults = {
        root: '',
        msg: {
            resetBookshelves: '',
            resetBookshelvesConfirm: '',
            saveItemToSession: ''
        }
    };

    viewer.bookshelvesSession = {
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.bookshelvesSession.init');
                console.log('##############################');
                console.log('viewer.bookshelvesSession.init: config - ', config);
            }

            $.extend(true, _defaults, config);

            // set confirm counter to local storage
            if (localStorage.getItem('confirmCounter') == undefined) {
                localStorage.setItem('confirmCounter', 0);
            }

            // render bookshelf dropdown list
            _renderDropdownList();

            // toggle bookshelf dropdown
            $('[data-bookshelf-type="dropdown"]').off().on('click', function (event) {
                event.stopPropagation();

                // hide other dropdowns
                $('.login-navigation__login-dropdown, .login-navigation__user-dropdown, .navigation__collection-panel').hide();

                _getAllSessionElements(_defaults.root).then(function (elements) {
                    if (elements.items.length > 0) {
                        $('.bookshelf-navigation__dropdown').slideToggle('fast');
                    } else {
                        return false;
                    }
                }).fail(function (error) {
                    console.error('ERROR - _getAllSessionElements: ', error.responseText);
                });
            });

            // set element count of list to counter
            _setSessionElementCount();

            // check add buttons if element is in list
            _setAddActiveState();

            // add element to session
            $('[data-bookshelf-type="add"]').off().on('click', function () {
                var currBtn = $(this);
                var currPi = currBtn.attr('data-pi');

                _isElementSet(_defaults.root, currPi).then(function (isSet) {
                    // set confirm counter
                    _confirmCounter = parseInt(localStorage.getItem('confirmCounter'));

                    if (!isSet) {
                        if (_confirmCounter == 0) {
                            if (confirm(_defaults.msg.saveItemToSession)) {
                                currBtn.addClass('added');
                                localStorage.setItem('confirmCounter', 1);
                                _setSessionElement(_defaults.root, currPi).then(function () {
                                    _setSessionElementCount();
                                    _renderDropdownList();
                                });
                            } else {
                                return false;
                            }
                        } else {
                            currBtn.addClass('added');
                            _setSessionElement(_defaults.root, currPi).then(function () {
                                _setSessionElementCount();
                                _renderDropdownList();
                            });
                        }
                    } else {
                        currBtn.removeClass('added');
                        _deleteSessionElement(_defaults.root, currPi).then(function () {
                            _setSessionElementCount();
                            _renderDropdownList();
                        });
                    }
                }).fail(function (error) {
                    console.error('ERROR - _isElementSet: ', error.responseText);
                });
            });

            // hide menus by clicking on body
            $('body').on('click', function (event) {
                var target = $(event.target);
                var dropdown = $('.bookshelf-navigation__dropdown');
                var dropdownChild = dropdown.find('*');

                if (!target.is(dropdown) && !target.is(dropdownChild)) {
                    $('.bookshelf-navigation__dropdown').hide();
                }
            });
        }
    };
    /* ######## ADD (CREATE) ######## */

    /* ######## GET (READ) ######## */
    /**
     * Method to get all elements in watchlist from current session (user not logged in).
     * 
     * @method _getAllSessionElements
     * @param {String} root The application root path.
     * @returns {Object} An JSON-Object which contains all session elements.
     */
    function _getAllSessionElements(root) {
        if (_debug) {
            console.log('---------- _getAllSessionElements() ----------');
            console.log('_getAllSessionElements: root - ', root);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/session/get/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }
    /**
     * Method to check if element is in list (user not logged in).
     * 
     * @method _isElementSet
     * @param {String} root The application root path.
     * @param {String} pi The persistent identifier of the saved element.
     * @returns {Boolean} True if element is set.
     */
    function _isElementSet(root, pi) {
        if (_debug) {
            console.log('---------- _isElementSet() ----------');
            console.log('_isElementSet: root - ', root);
            console.log('_isElementSet: pi - ', pi);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/session/contains/' + pi + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }

    /* ######## SET (UPDATE) ######## */
    /**
     * Method to add an elements to watchlist in current session (user not logged in).
     * 
     * @method _setSessionElement
     * @param {String} root The application root path.
     * @param {String} pi The persistent identifier of the saved element.
     */
    function _setSessionElement(root, pi) {
        if (_debug) {
            console.log('---------- _setSessionElement() ----------');
            console.log('_setSessionElement: root - ', root);
            console.log('_setSessionElement: pi - ', pi);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/session/add/' + pi + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }

    /* ######## DELETE ######## */
    /**
     * Method to delete an element from watchlist in current session (user not logged in).
     * 
     * @method _deleteSessionElement
     * @param {String} root The application root path.
     * @param {String} pi The persistent identifier of the saved element.
     */
    function _deleteSessionElement(root, pi) {
        if (_debug) {
            console.log('---------- _deleteSessionElement() ----------');
            console.log('_deleteSessionElement: root - ', root);
            console.log('_deleteSessionElement: pi - ', pi);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/session/delete/' + pi + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }
    /**
     * Method to delete all elements from watchlist in current session (user not logged
     * in).
     * 
     * @method _deleteAllSessionElements
     * @param {String} root The application root path.
     */
    function _deleteAllSessionElements(root) {
        if (_debug) {
            console.log('---------- _deleteAllSessionElements() ----------');
            console.log('_deleteAllSessionElements: root - ', root);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/session/delete/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }

    /* ######## BUILD ######## */
    /**
     * Method to set the count of elements in watchlist from current session (user not
     * logged in).
     * 
     * @method _setSessionElementCount
     * @param {String} root The application root path.
     */
    function _setSessionElementCount() {
        if (_debug) {
            console.log('---------- _setSessionElementCount() ----------');
        }

        _getAllSessionElements(_defaults.root).then(function (elements) {
            $('[data-bookshelf-type="counter"]').empty().text(elements.items.length);
        }).fail(function (error) {
            console.error('ERROR - _getAllSessionElements: ', error.responseText);
        });
    }
    /**
     * Method to render the element list in bookshelf dropdown (user not logged in).
     * 
     * @method _renderDropdownList
     */
    function _renderDropdownList() {
        if (_debug) {
            console.log('---------- _renderDropdownList() ----------');
        }

        _getAllSessionElements(_defaults.root).then(function (elements) {
            // DOM-Elements
            var dropdownListReset = $('<button>').addClass('btn-clean').attr('type', 'button').attr('data-bookshelf-type', 'reset').text(_defaults.msg.resetBookshelves);
            var dropdownList = $('<ul />').addClass('list');
            var dropdownListItem = null;
            var dropdownListItemRow = null;
            var dropdownListItemColLeft = null;
            var dropdownListItemColCenter = null;
            var dropdownListItemColRight = null;
            var dropdownListItemImage = null;
            var dropdownListItemName = null;
            var dropdownListItemNameLink = null;
            var dropdownListItemDelete = null;

            // set confirm counter
            if (elements.items.length < 1) {
                localStorage.setItem('confirmCounter', 0);
            }

            elements.items.forEach(function (item) {
                dropdownListItem = $('<li />');
                dropdownListItemRow = $('<div />').addClass('row no-margin');
                dropdownListItemColLeft = $('<div />').addClass('col-xs-4 no-padding');
                dropdownListItemColCenter = $('<div />').addClass('col-xs-7 no-padding');
                dropdownListItemColRight = $('<div />').addClass('col-xs-1 no-padding bookshelf-navigation__dropdown-list-remove');
                dropdownListItemImage = $('<div />').addClass('bookshelf-navigation__dropdown-list-image').css('background-image', 'url(' + item.representativeImageUrl + ')');
                dropdownListItemName = $('<h4 />');
                dropdownListItemNameLink = $('<a />').attr('href', _defaults.root + item.url).text(item.name);
                dropdownListItemDelete = $('<button />').addClass('btn-clean').attr('type', 'button').attr('data-bookshelf-type', 'delete').attr('data-pi', item.pi);

                // build bookshelf item
                dropdownListItemName.append(dropdownListItemNameLink);
                dropdownListItemColLeft.append(dropdownListItemImage);
                dropdownListItemColCenter.append(dropdownListItemName);
                dropdownListItemColRight.append(dropdownListItemDelete);
                dropdownListItemRow.append(dropdownListItemColLeft).append(dropdownListItemColCenter).append(dropdownListItemColRight);
                dropdownListItem.append(dropdownListItemRow);
                dropdownList.append(dropdownListItem);
            });

            // render complete list
            $('.bookshelf-navigation__dropdown-list').empty().append(dropdownList);

            // render reset if items exist
            if (elements.items.length > 0) {
                $('.bookshelf-navigation__dropdown-list-reset').empty().append(dropdownListReset);
            } else {
                $('.bookshelf-navigation__dropdown').hide();
                $('.bookshelf-navigation__dropdown-list-reset').empty();
            }

            // delete single item
            $('[data-bookshelf-type="delete"]').on('click', function () {
                var currBtn = $(this);
                var currPi = currBtn.attr('data-pi');

                _deleteSessionElement(_defaults.root, currPi).then(function () {
                    _setSessionElementCount();
                    _setAddActiveState();
                    _renderDropdownList();
                });
            });

            // delete all items
            $('[data-bookshelf-type="reset"]').on('click', function () {
                if (confirm(_defaults.msg.resetBookshelvesConfirm)) {
                    _deleteAllSessionElements(_defaults.root).then(function () {
                        localStorage.setItem('confirmCounter', 0);
                        _setSessionElementCount();
                        _setAddActiveState();
                        _renderDropdownList();
                    });
                } else {
                    return false;
                }
            });
        }).fail(function (error) {
            console.error('ERROR - _getAllSessionElements: ', error.responseText);
        });
    }
    /**
     * Method to set the active state of add buttons (user not logged in).
     * 
     * @method _setAddActiveState
     */
    function _setAddActiveState() {
        if (_debug) {
            console.log('---------- _setAddActiveState() ----------');
        }

        $('[data-bookshelf-type="add"]').each(function () {
            var currBtn = $(this);
            var currPi = currBtn.attr('data-pi');

            _isElementSet(_defaults.root, currPi).then(function (isSet) {
                if (isSet) {
                    currBtn.addClass('added');
                } else {
                    currBtn.removeClass('added');
                }
            }).fail(function (error) {
                console.error('ERROR - _isElementSet: ', error.responseText);
            });
        });
    }

    return viewer;
}(viewerJS || {}, jQuery);

// /rest/bookshelves/session/add/{pi}/{logid}/{page}
// Fügt der Merkliste ein Item mit der angegebenen pi, logId und Seitennummer an. Der Name
// des Items wird automatisch aus dem zur pi gehörenden SOLR-Dokument erstellt
// /rest/bookshelves/session/delete/{pi}/{logid}/{page}
// Löscht das Item mit der angegebenen pi, logid und Seitennummer aus der Merkliste, wenn
// es enthalten ist
// /rest/bookshelves/session/contains/{pi}/{logid}/{page}
// gibt "true" zurück, wenn die Merkliste ein Item mit der angegebenen pi, logid und
// Seitennummer enthält; sonst "false"
// /rest/bookshelves/session/count
// Gibt die Zahl der in der Merkliste enthaltenen Items zurück.

},{}],2:[function(require,module,exports){
'use strict';

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
 * Module to manage bookshelves if the user is logged in.
 * 
 * @version 3.2.0
 * @module viewerJS.bookshelvesUser
 * @requires jQuery
 */
var viewerJS = function (viewer) {
    'use strict';

    var _debug = false;
    var _defaults = {
        root: '',
        msg: {
            resetBookshelves: '',
            resetBookshelvesConfirm: '',
            noItemsAvailable: '',
            selectBookshelf: '',
            addNewBookshelf: ''
        }
    };

    viewer.bookshelvesUser = {
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.bookshelvesUser.init');
                console.log('##############################');
                console.log('viewer.bookshelvesUser.init: config - ', config);
            }

            $.extend(true, _defaults, config);

            // render bookshelf navigation list
            _renderBookshelfNavigationList();

            // toggle bookshelf dropdown
            $('[data-bookshelf-type="dropdown"]').off().on('click', function (event) {
                event.stopPropagation();

                // hide other dropdowns
                $('.login-navigation__login-dropdown, .login-navigation__user-dropdown, .navigation__collection-panel').hide();
                $('.bookshelf-popup').remove();

                $('.bookshelf-navigation__dropdown').slideToggle('fast');
            });

            // check if element is in any bookshelf
            _setAddedStatus();

            // render bookshelf popup
            $('[data-bookshelf-type="add"]').off().on('click', function (event) {
                event.stopPropagation();

                // hide other dropdowns
                $('.bookshelf-navigation__dropdown, .login-navigation__user-dropdown').hide();

                var currBtn = $(this);
                var currPi = currBtn.attr('data-pi');
                var currLogid = currBtn.attr('data-logid');
                var currPage = currBtn.attr('data-page');
                var currPos = currBtn.offset();
                var currSize = {
                    width: currBtn.outerWidth(),
                    height: currBtn.outerHeight()
                };

                // render bookshelf popup
                _renderBookshelfPopup(currPi, currLogid, currPage, currPos, currSize);
            });

            // hide menus/popups by clicking on body
            $('body').on('click', function (event) {
                $('.bookshelf-navigation__dropdown').hide();

                if ($('.bookshelf-popup').length > 0) {
                    var target = $(event.target);
                    var popup = $('.bookshelf-popup');
                    var popupChild = popup.find('*');

                    if (!target.is(popup) && !target.is(popupChild)) {
                        $('.bookshelf-popup').remove();
                    }
                }
            });

            // add new bookshelf in overview
            $('#addBookshelfBtn').off().on('click', function () {
                var bsName = $('#addBookshelfInput').val();

                if (bsName != '') {
                    _addNamedBookshelf(_defaults.root, bsName).then(function () {
                        location.reload();
                    }).fail(function (error) {
                        console.error('ERROR - _addNamedBookshelf: ', error.responseText);
                    });
                } else {
                    _addAutomaticNamedBookshelf(_defaults.root).then(function () {
                        location.reload();
                    }).fail(function (error) {
                        console.error('ERROR - _addAutomaticNamedBookshelf: ', error.responseText);
                    });
                }
            });

            // add new bookshelf on enter in overview
            $('#addBookshelfInput').on('keyup', function (event) {
                if (event.which == 13) {
                    $('#addBookshelfBtn').click();
                }
            });
        }
    };
    /* ######## ADD (CREATE) ######## */
    /**
     * Method to add an item to the user bookshelf by PI.
     * 
     * @method _addBookshelfItemByPi
     * @param {String} root The application root path.
     * @param {String} id The current bookshelf id.
     * @param {String} pi The pi of the item to add.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _addBookshelfItemByPi(root, id, pi) {
        if (_debug) {
            console.log('---------- _addBookshelfItemByPi() ----------');
            console.log('_addBookshelfItemByPi: root - ', root);
            console.log('_addBookshelfItemByPi: id - ', id);
            console.log('_addBookshelfItemByPi: pi - ', pi);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/user/get/' + id + '/add/' + pi + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }
    /**
     * Method to add an item with PI, LOGID and PAGE to the user bookshelf.
     * 
     * @method _addBookshelfItemByPiLogidPage
     * @param {String} root The application root path.
     * @param {String} id The current bookshelf id.
     * @param {String} pi The pi of the item to add.
     * @param {String} logid The logid of the item to add.
     * @param {String} page The page of the item to add.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _addBookshelfItemByPiLogidPage(root, id, pi, logid, page) {
        if (_debug) {
            console.log('---------- _addBookshelfItemByPiLogidPage() ----------');
            console.log('_addBookshelfItemByPiLogidPage: root - ', root);
            console.log('_addBookshelfItemByPiLogidPage: id - ', id);
            console.log('_addBookshelfItemByPiLogidPage: pi - ', pi);
            console.log('_addBookshelfItemByPiLogidPage: logid - ', logid);
            console.log('_addBookshelfItemByPiLogidPage: page - ', page);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/user/get/' + id + '/add/' + pi + '/' + logid + '/' + page + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }
    /**
     * Method to add a named bookshelf to the user account.
     * 
     * @method _addNamedBookshelf
     * @param {String} root The application root path.
     * @param {String} name The name of the bookshelf.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _addNamedBookshelf(root, name) {
        if (_debug) {
            console.log('---------- _addNamedBookshelf() ----------');
            console.log('_addNamedBookshelf: root - ', root);
            console.log('_addNamedBookshelf: name - ', name);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/user/add/' + name + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }
    /**
     * Method to add a automatic named bookshelf to the user account.
     * 
     * @method _addAutomaticNamedBookshelf
     * @param {String} root The application root path.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _addAutomaticNamedBookshelf(root) {
        if (_debug) {
            console.log('---------- _addAutomaticNamedBookshelf() ----------');
            console.log('_addAutomaticNamedBookshelf: root - ', root);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/user/add/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }
    /**
     * Method to add the session bookshelf to the user account with an automatic generated
     * name.
     * 
     * @method _addSessionBookshelf
     * @param {String} root The application root path.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _addSessionBookshelf(root) {
        if (_debug) {
            console.log('---------- _addSessionBookshelf() ----------');
            console.log('_addSessionBookshelf: root - ', root);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/user/addSessionBookshelf/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }
    /**
     * Method to add the session bookshelf to the user account with a given name.
     * 
     * @method _addSessionBookshelfNamed
     * @param {String} root The application root path.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _addSessionBookshelfNamed(root, name) {
        if (_debug) {
            console.log('---------- _addSessionBookshelfNamed() ----------');
            console.log('_addSessionBookshelfNamed: root - ', root);
            console.log('_addSessionBookshelfNamed: name - ', name);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/user/addSessionBookshelf/' + name + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }
    /* ######## GET (READ) ######## */
    /**
     * Method to get all user bookshelves.
     * 
     * @method _getAllBookshelves
     * @param {String} root The application root path.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _getAllBookshelves(root) {
        if (_debug) {
            console.log('---------- _getAllBookshelves() ----------');
            console.log('_getAllBookshelves: root - ', root);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/user/get/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }
    /**
     * Method to get a bookshelf by id.
     * 
     * @method _getBookshelfById
     * @param {String} root The application root path.
     * @param {String} id The current bookshelf id.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _getBookshelfById(root, id) {
        if (_debug) {
            console.log('---------- _getBookshelfById() ----------');
            console.log('_getBookshelfById: root - ', root);
            console.log('_getBookshelfById: id - ', id);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/user/get/' + id + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }
    /**
     * Method to get the number of items in the selected bookshelf.
     * 
     * @method _getBookshelfItemCount
     * @param {String} root The application root path.
     * @param {String} id The current bookshelf id.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _getBookshelfItemCount(root, id) {
        if (_debug) {
            console.log('---------- _getBookshelfItemCount() ----------');
            console.log('_getBookshelfItemCount: root - ', root);
            console.log('_getBookshelfItemCount: id - ', id);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/user/get/' + id + '/count/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }
    /**
     * Method to get all public bookshelves.
     * 
     * @method _getPublicBookshelves
     * @param {String} root The application root path.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _getPublicBookshelves(root) {
        if (_debug) {
            console.log('---------- _getPublicBookshelves() ----------');
            console.log('_getPublicBookshelves: root - ', root);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/public/get/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }
    /**
     * Method to get all shared bookshelves.
     * 
     * @method _getSharedBookshelves
     * @param {String} root The application root path.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _getSharedBookshelves(root) {
        if (_debug) {
            console.log('---------- _getSharedBookshelves() ----------');
            console.log('_getSharedBookshelves: root - ', root);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/shared/get/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }
    /**
     * Method to check if an item by PI if it's in user bookshelf. It returns the
     * bookshelves or false if no items are in list.
     * 
     * @method _getContainingBookshelfItemByPi
     * @param {String} root The application root path.
     * @param {String} pi The pi of the current item.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _getContainingBookshelfItemByPi(root, pi) {
        if (_debug) {
            console.log('---------- _getContainingBookshelfItemByPi() ----------');
            console.log('_getContainingBookshelfItemByPi: root - ', root);
            console.log('_getContainingBookshelfItemByPi: pi - ', pi);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/user/contains/' + pi + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }
    /**
     * Method to check if an item by PI, LOGID and PAGE if it's in user bookshelf. It
     * returns the bookshelves or false if no items are in list.
     * 
     * @method _getContainingBookshelfItemByPiLogidPage
     * @param {String} root The application root path.
     * @param {String} pi The pi of the current item.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _getContainingBookshelfItemByPiLogidPage(root, pi, logid, page) {
        if (_debug) {
            console.log('---------- _getContainingBookshelfItemByPiLogidPage() ----------');
            console.log('_getContainingBookshelfItemByPiLogidPage: root - ', root);
            console.log('_getContainingBookshelfItemByPiLogidPage: pi - ', pi);
            console.log('_getContainingBookshelfItemByPiLogidPage: logid - ', logid);
            console.log('_getContainingBookshelfItemByPiLogidPage: page - ', page);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/user/contains/' + pi + '/' + logid + '/' + page + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }
    /* ######## SET (UPDATE) ######## */
    /**
     * Method to set the name of a bookshelf.
     * 
     * @method _setBookshelfName
     * @param {String} root The application root path.
     * @param {String} id The current bookshelf id.
     * @param {String} name The name of the bookshelf.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _setBookshelfName(root, id, name) {
        if (_debug) {
            console.log('---------- _setBookshelfName() ----------');
            console.log('_setBookshelfName: root - ', root);
            console.log('_setBookshelfName: id - ', id);
            console.log('_setBookshelfName: name - ', name);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/user/get/' + id + '/set/name/' + name + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }
    /* ######## DELETE ######## */
    /**
     * Method to delete an item from the user bookshelf by PI.
     * 
     * @method _deleteBookshelfItemByPi
     * @param {String} root The application root path.
     * @param {String} id The current bookshelf id.
     * @param {String} pi The pi of the item to add.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _deleteBookshelfItemByPi(root, id, pi) {
        if (_debug) {
            console.log('---------- _deleteBookshelfItemByPi() ----------');
            console.log('_deleteBookshelfItemByPi: root - ', root);
            console.log('_deleteBookshelfItemByPi: id - ', id);
            console.log('_deleteBookshelfItemByPi: pi - ', pi);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/user/get/' + id + '/delete/' + pi + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }
    /**
     * Method to delete an item with PI, LOGID and PAGE from the user bookshelf.
     * 
     * @method _deleteBookshelfItemByPiLogidPage
     * @param {String} root The application root path.
     * @param {String} id The current bookshelf id.
     * @param {String} pi The pi of the item to add.
     * @param {String} logid The logid of the item to add.
     * @param {String} page The page of the item to add.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _deleteBookshelfItemByPiLogidPage(root, id, pi, logid, page) {
        if (_debug) {
            console.log('---------- _deleteBookshelfItemByPiLogidPage() ----------');
            console.log('_deleteBookshelfItemByPiLogidPage: root - ', root);
            console.log('_deleteBookshelfItemByPiLogidPage: id - ', id);
            console.log('_deleteBookshelfItemByPiLogidPage: pi - ', pi);
            console.log('_deleteBookshelfItemByPiLogidPage: logid - ', logid);
            console.log('_deleteBookshelfItemByPiLogidPage: page - ', page);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/user/get/' + id + '/delete/' + pi + '/' + logid + '/' + page + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }
    /**
     * Method to delete a bookshelf by ID.
     * 
     * @method _deleteBookshelfById
     * @param {String} root The application root path.
     * @param {String} id The current bookshelf id.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _deleteBookshelfById(root, id) {
        if (_debug) {
            console.log('---------- _deleteBookshelfById() ----------');
            console.log('_deleteBookshelfById: root - ', root);
            console.log('_deleteBookshelfById: id - ', id);
        }

        var promise = Q($.ajax({
            url: root + '/rest/bookshelves/user/delete/' + id + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        }));

        return promise;
    }
    /* ######## BUILD ######## */
    /**
     * Method to render a popup which contains bookshelf actions.
     * 
     * @method _renderBookshelfPopup
     * @param {String} pi The pi of the item to add.
     * @param {String} logid The logid of the item to add.
     * @param {String} page The page of the item to add.
     * @param {Object} pos The position of the clicked button.
     * @param {Object} size The width and height of the clicked button.
     */
    function _renderBookshelfPopup(pi, logid, page, pos, size) {
        if (_debug) {
            console.log('---------- _renderBookshelfPopup() ----------');
            console.log('_renderBookshelfPopup: pi - ', pi);
            console.log('_renderBookshelfPopup: logid - ', logid);
            console.log('_renderBookshelfPopup: page - ', page);
            console.log('_renderBookshelfPopup: pos - ', pos);
            console.log('_renderBookshelfPopup: size - ', size);
        }

        var pi = pi;
        var posTop = pos.top;
        var posLeft = pos.left;

        // remove all popups
        $('.bookshelf-popup').remove();

        // DOM-Elements
        var bookshelfPopup = $('<div />').addClass('bookshelf-popup bottom').css({
            'top': posTop + size.height + 10 + 'px',
            'left': posLeft - 142 + size.width / 2 + 'px'
        });
        var bookshelfPopupLoader = $('<div />').addClass('bookshelf-popup__body-loader');
        bookshelfPopup.append(bookshelfPopupLoader);

        // build popup header
        var bookshelfPopupHeader = $('<div />').addClass('bookshelf-popup__header').text(_defaults.msg.selectBookshelf);

        // build popup body
        var bookshelfPopupBody = $('<div />').addClass('bookshelf-popup__body');

        // build popup footer
        var bookshelfPopupFooter = $('<div />').addClass('bookshelf-popup__footer');
        var bookshelfPopupFooterRow = $('<div />').addClass('row no-margin');
        var bookshelfPopupFooterColLeft = $('<div />').addClass('col-xs-11 no-padding');
        var bookshelfPopupFooterInput = $('<input />').attr('type', 'text').attr('placeholder', _defaults.msg.addNewBookshelf);
        bookshelfPopupFooterColLeft.append(bookshelfPopupFooterInput);
        var bookshelfPopupFooterColright = $('<div />').addClass('col-xs-1 no-padding');
        var bookshelfPopupFooterAddBtn = $('<button />').addClass('btn-clean').attr('type', 'button').attr('data-bookshelf-type', 'add').attr('data-pi', pi);
        bookshelfPopupFooterColright.append(bookshelfPopupFooterAddBtn);
        bookshelfPopupFooterRow.append(bookshelfPopupFooterColLeft).append(bookshelfPopupFooterColright);
        bookshelfPopupFooter.append(bookshelfPopupFooterRow);

        // build popup
        bookshelfPopup.append(bookshelfPopupHeader).append(bookshelfPopupBody).append(bookshelfPopupFooter);

        // append popup
        $('body').append(bookshelfPopup);

        // render bookshelf list
        _renderBookshelfPopoverList(pi);

        // add new bookshelf in popover
        $('.bookshelf-popup__footer [data-bookshelf-type="add"]').on('click', function () {
            var bsName = $('.bookshelf-popup__footer input').val();
            var currPi = $(this).attr('data-pi');

            if (bsName != '') {
                _addNamedBookshelf(_defaults.root, bsName).then(function () {
                    $('.bookshelf-popup__footer input').val('');
                    _renderBookshelfPopoverList(currPi);
                    _renderBookshelfNavigationList();
                }).fail(function (error) {
                    console.error('ERROR - _addNamedBookshelf: ', error.responseText);
                });
            } else {
                _addAutomaticNamedBookshelf(_defaults.root).then(function () {
                    $('.bookshelf-popup__footer input').val('');
                    _renderBookshelfPopoverList(currPi);
                    _renderBookshelfNavigationList();
                }).fail(function (error) {
                    console.error('ERROR - _addAutomaticNamedBookshelf: ', error.responseText);
                });
            }
        });

        // add new bookshelf on enter in popover
        $('.bookshelf-popup__footer input').on('keyup', function (event) {
            if (event.which == 13) {
                $('.bookshelf-popup__footer [data-bookshelf-type="add"]').click();
            }
        });
    }
    /**
     * Method to render the element list in bookshelf popover.
     * 
     * @method _renderBookshelfPopoverList
     * @param {String} pi The current PI of the selected item.
     */
    function _renderBookshelfPopoverList(pi) {
        if (_debug) {
            console.log('---------- _renderBookshelfPopoverList() ----------');
            console.log('_renderBookshelfPopoverList: pi - ', pi);
        }

        _getAllBookshelves(_defaults.root).then(function (elements) {
            // DOM-Elements
            var dropdownList = $('<ul />').addClass('bookshelf-popup__body-list list');
            var dropdownListItem = null;
            var dropdownListItemText = null;
            var dropdownListItemAdd = null;
            var dropdownListItemIsInBookshelf = null;
            var dropdownListItemAddCounter = null;

            if (elements.length > 0) {
                elements.forEach(function (item) {
                    dropdownListItem = $('<li />');
                    dropdownListItemIsInBookshelf = '';
                    // check if item is in bookshelf
                    item.items.forEach(function (object) {
                        if (object.pi == pi) {
                            dropdownListItemIsInBookshelf = '<i class="fa fa-check" aria-hidden="true"></i> ';
                        }
                    });
                    dropdownListItemAddCounter = $('<span />').text(item.items.length);
                    dropdownListItemAdd = $('<button />').addClass('btn-clean').attr('type', 'button').attr('data-bookshelf-type', 'add').attr('data-id', item.id).attr('data-pi', pi).text(item.name).prepend(dropdownListItemIsInBookshelf).append(dropdownListItemAddCounter);

                    // build bookshelf item
                    dropdownListItem.append(dropdownListItemAdd);
                    dropdownList.append(dropdownListItem);
                });
            } else {
                // add empty list item
                dropdownListItem = $('<li />');
                dropdownListItemText = $('<span />').addClass('empty').text(_defaults.msg.noItemsAvailable);

                dropdownListItem.append(dropdownListItemText);
                dropdownList.append(dropdownListItem);
            }

            // render complete list
            $('.bookshelf-popup__body').empty().append(dropdownList);

            // remove loader
            $('.bookshelf-popup__body-loader').remove();

            // add item to bookshelf
            $('.bookshelf-popup__body-list [data-bookshelf-type="add"]').on('click', function () {
                var currBtn = $(this);
                var currId = currBtn.attr('data-id');
                var currPi = currBtn.attr('data-pi');
                var isChecked = currBtn.find('.fa-check');

                if (isChecked.length > 0) {
                    _deleteBookshelfItemByPi(_defaults.root, currId, currPi).then(function () {
                        _renderBookshelfPopoverList(currPi);
                        _renderBookshelfNavigationList();
                        _setAddedStatus();
                    }).fail(function (error) {
                        console.error('ERROR - _deleteBookshelfItemByPi: ', error.responseText);
                    });
                } else {
                    _addBookshelfItemByPi(_defaults.root, currId, currPi).then(function () {
                        _renderBookshelfPopoverList(currPi);
                        _renderBookshelfNavigationList();
                        _setAddedStatus();
                    }).fail(function (error) {
                        console.error('ERROR - _addBookshelfItemByPi: ', error.responseText);
                    });
                }
            });
        }).fail(function (error) {
            console.error('ERROR - _getAllBookshelves: ', error.responseText);
        });
    }
    /**
     * Method to render the element list in bookshelf navigation.
     * 
     * @method _renderBookshelfNavigationList
     */
    function _renderBookshelfNavigationList() {
        if (_debug) {
            console.log('---------- _renderBookshelfNavigationList() ----------');
        }

        var allBookshelfItems = 0;

        _getAllBookshelves(_defaults.root).then(function (elements) {
            // DOM-Elements
            var dropdownList = $('<ul />').addClass('bookshelf-navigation__dropdown-list list');
            var dropdownListItem = null;
            var dropdownListItemRow = null;
            var dropdownListItemLeft = null;
            var dropdownListItemRight = null;
            var dropdownListItemText = null;
            var dropdownListItemLink = null;
            var dropdownListItemAddCounter = null;

            if (elements.length > 0) {
                elements.forEach(function (item) {
                    dropdownListItem = $('<li />');
                    dropdownListItemRow = $('<div />').addClass('row no-margin');
                    dropdownListItemLeft = $('<div />').addClass('col-xs-10 no-padding');
                    dropdownListItemRight = $('<div />').addClass('col-xs-2 no-padding');
                    dropdownListItemLink = $('<a />').attr('href', _defaults.root + '/bookshelf/' + item.id + '/').text(item.name);
                    dropdownListItemAddCounter = $('<span />').addClass('bookshelf-navigation__dropdown-list-counter').text(item.items.length);

                    // build bookshelf item
                    dropdownListItemLeft.append(dropdownListItemLink);
                    dropdownListItemRight.append(dropdownListItemAddCounter);
                    dropdownListItemRow.append(dropdownListItemLeft).append(dropdownListItemRight);
                    dropdownListItem.append(dropdownListItemRow);
                    dropdownList.append(dropdownListItem);

                    // raise bookshelf item counter
                    allBookshelfItems += item.items.length;
                });

                // set bookshelf item counter
                $('[data-bookshelf-type="counter"]').empty().text(allBookshelfItems);
            } else {
                // add empty list item
                dropdownListItem = $('<li />');
                dropdownListItemText = $('<span />').addClass('empty').text(_defaults.msg.noItemsAvailable);

                dropdownListItem.append(dropdownListItemText);
                dropdownList.append(dropdownListItem);

                // set bookshelf item counter
                $('[data-bookshelf-type="counter"]').empty().text(allBookshelfItems);
            }

            // render complete list
            $('.bookshelf-navigation__dropdown-list').empty().append(dropdownList);
        }).fail(function (error) {
            console.error('ERROR - _getAllBookshelves: ', error.responseText);
        });
    }

    /**
     * Method to set an 'added' status to an object.
     * 
     * @method _setAddedStatus
     */
    function _setAddedStatus() {
        if (_debug) {
            console.log('---------- _setAddedStatus() ----------');
        }

        $('[data-bookshelf-type="add"]').each(function () {
            var currTrigger = $(this);
            var currTriggerPi = currTrigger.attr('data-pi');

            _isItemInBookshelf(currTrigger, currTriggerPi);
        });
    }
    /**
     * Method to check if item is in any bookshelf.
     * 
     * @method _isItemInBookshelf
     * @param {Object} object An jQuery-Object of the current item.
     * @param {String} pi The current PI of the selected item.
     */
    function _isItemInBookshelf(object, pi) {
        if (_debug) {
            console.log('---------- _isItemInBookshelf() ----------');
            console.log('_isItemInBookshelf: pi - ', pi);
            console.log('_isItemInBookshelf: object - ', object);
        }

        _getContainingBookshelfItemByPi(_defaults.root, pi).then(function (items) {
            if (items.length == 0) {
                object.removeClass('added');

                return false;
            } else {
                object.addClass('added');
            }
        }).fail(function (error) {
            console.error('ERROR - _getContainingBookshelfItemByPi: ', error.responseText);
        });
    }

    return viewer;
}(viewerJS || {}, jQuery);

},{}],3:[function(require,module,exports){
'use strict';

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
var viewerJS = function (viewer) {
    'use strict';

    var _debug = false;
    var _this = null;
    var _currApiCall = '';
    var _json = {};
    var _popoverConfig = {};
    var _popoverContent = null;
    var _defaults = {
        appUrl: '',
        calendarWrapperSelector: '.search-calendar__months',
        popoverTriggerSelector: '[data-popover-trigger="calendar-po-trigger"]',
        popoverTitle: 'Bitte übergeben Sie den Titel des Werks'
    };

    viewer.calendarPopover = {
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.calendarPopover.init');
                console.log('##############################');
                console.log('viewer.calendarPopover.init: config - ', config);
            }

            $.extend(true, _defaults, config);

            // TODO: Fehlermeldung in der Konsole beseitigen, wenn man auf den Tag ein
            // zweites Mal klickt.

            // show popover for current day
            $(_defaults.popoverTriggerSelector).on('click', function () {
                _this = $(this);
                _currApiCall = encodeURI(_this.attr('data-api'));

                viewerJS.helper.getRemoteData(_currApiCall).done(function (_json) {
                    _popoverContent = _getPopoverContent(_json, _defaults);
                    _popoverConfig = {
                        placement: 'auto bottom',
                        title: _defaults.popoverTitle,
                        content: _popoverContent,
                        viewport: {
                            selector: _defaults.calendarWrapperSelector
                        },
                        html: true
                    };

                    $(_defaults.popoverTriggerSelector).popover('destroy');
                    _this.popover(_popoverConfig);
                    _this.popover('show');
                });
            });

            // remove all popovers by clicking on body
            $('body').on('click', function (event) {
                if ($(event.target).closest(_defaults.popoverTriggerSelector).length) {
                    return;
                } else {
                    $(_defaults.popoverTriggerSelector).popover('destroy');
                }
            });
        }
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

        $.each(data, function (works, values) {
            workListLink = config.appUrl + 'image/' + values.PI_TOPSTRUCT + '/' + values.THUMBPAGENO + '/' + values.LOGID + '/';

            workList += '<li>';
            workList += '<a href="' + workListLink + '">';
            workList += values.LABEL;
            workList += '</a>';
            workList += '</li>';
        });

        workList += '</ul>';

        return workList;
    }

    return viewer;
}(viewerJS || {}, jQuery);

},{}],4:[function(require,module,exports){
'use strict';

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
 * Module to raise and degrade the page font size.
 * 
 * @version 3.2.0
 * @module viewerJS.changeFontSize
 * @requires jQuery
 */
var viewerJS = function (viewer) {
    'use strict';

    var _debug = false;
    var _parsedFontSize = 0;
    var _currFontSize = '';
    var _defaults = {
        fontDownBtn: '',
        fontUpBtn: '',
        maxFontSize: 18,
        minFontSize: 12,
        baseFontSize: '14px'
    };

    viewer.changeFontSize = {
        /**
         * Method which initializes the font size switcher.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.fontDownBtn The ID/Class of the font degrade button.
         * @param {String} config.fontUpBtn The ID/Class of the font upgrade button.
         * @param {String} config.maxFontSize The maximum font size the document should
         * scale up.
         * @param {String} config.minFontSize The minimum font size the document should
         * scale down.
         * @param {String} config.baseFontSize The base font size of the HTML-Element.
         * @example
         * 
         * <pre>
         * var changeFontSizeConfig = {
         *     fontDownBtn: '#fontSizeDown',
         *     fontUpBtn: '#fontSizeUp',
         *     maxFontSize: 18,
         *     minFontSize: 14
         * };
         * 
         * viewerJS.changeFontSize.init( changeFontSizeConfig );
         * </pre>
         */
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.changeFontSize.init');
                console.log('##############################');
                console.log('viewer.changeFontSize.init: config - ');
                console.log(config);
            }

            $.extend(true, _defaults, config);

            if (viewer.localStoragePossible) {
                // set current font-size
                _setFontSize();

                // set button state
                _setButtonState();

                $(_defaults.fontDownBtn).on('click', function () {
                    // set current font-size
                    _currFontSize = $('html').css('font-size');

                    // save font-size
                    _saveFontSize(_currFontSize);

                    // parse number of font-size
                    _parsedFontSize = _parseFontSize(_currFontSize);

                    // degrade font-size
                    _degradeFontSize(_parsedFontSize);
                });

                $(_defaults.fontUpBtn).on('click', function () {
                    // set current font-size
                    _currFontSize = $('html').css('font-size');

                    // save font-size
                    _saveFontSize(_currFontSize);

                    // parse number of font-size
                    _parsedFontSize = _parseFontSize(_currFontSize);

                    // raise font-size
                    _raiseFontSize(_parsedFontSize);
                });
            }
        }
    };

    /**
     * Method to degrade the page font-size.
     * 
     * @method _degradeFontSize
     * @param {Number} current The current font-size of the HTML-Element.
     */
    function _degradeFontSize(current) {
        if (_debug) {
            console.log('---------- _degradeFontSize() ----------');
            console.log('_degradeFontSize: current = ', current);
        }

        var size = current;
        size--;

        if (size >= _defaults.minFontSize) {
            $(_defaults.fontDownBtn).prop('disabled', false);
            $(_defaults.fontUpBtn).prop('disabled', false);
            $('html').css('font-size', size + 'px');

            // save font-size
            _saveFontSize(size + 'px');
        } else {
            $(_defaults.fontDownBtn).prop('disabled', true);
            $(_defaults.fontUpBtn).prop('disabled', false);
        }
    }

    /**
     * Method to raise the page font-size.
     * 
     * @method _raiseFontSize
     * @param {Number} current The current font-size of the HTML-Element.
     */
    function _raiseFontSize(current) {
        if (_debug) {
            console.log('---------- _raiseFontSize() ----------');
            console.log('_raiseFontSize: current = ', current);
        }

        var size = current;
        size++;

        if (size <= _defaults.maxFontSize) {
            $(_defaults.fontDownBtn).prop('disabled', false);
            $(_defaults.fontUpBtn).prop('disabled', false);
            $('html').css('font-size', size + 'px');

            // save font-size
            _saveFontSize(size + 'px');
        } else {
            $(_defaults.fontDownBtn).prop('disabled', false);
            $(_defaults.fontUpBtn).prop('disabled', true);
        }
    }

    /**
     * Method which parses a given pixel value to a number and returns it.
     * 
     * @method _parseFontSize
     * @param {String} string The string to parse.
     */
    function _parseFontSize(string) {
        if (_debug) {
            console.log('---------- _parseFontSize() ----------');
            console.log('_parseFontSize: string = ', string);
        }

        return parseInt(string.replace('px'));
    }

    /**
     * Method to save the current font-size to local storage as a string.
     * 
     * @method _saveFontSize
     * @param {String} size The String to save in local storage.
     */
    function _saveFontSize(size) {
        if (_debug) {
            console.log('---------- _saveFontSize() ----------');
            console.log('_parseFontSize: size = ', size);
        }

        localStorage.setItem('currentFontSize', size);
    }

    /**
     * Method to set the current font-size from local storage to the HTML-Element.
     * 
     * @method _setFontSize
     */
    function _setFontSize() {
        if (_debug) {
            console.log('---------- _setFontSize() ----------');
        }
        var fontSize = localStorage.getItem('currentFontSize');

        if (fontSize === null || fontSize === '') {
            localStorage.setItem('currentFontSize', _defaults.baseFontSize);
        } else {
            $('html').css('font-size', fontSize);
        }
    }

    /**
     * Method to set the state of the font-size change buttons.
     * 
     * @method _setButtonState
     */
    function _setButtonState() {
        if (_debug) {
            console.log('---------- _setButtonState() ----------');
        }
        var fontSize = localStorage.getItem('currentFontSize');
        var newFontSize = _parseFontSize(fontSize);

        if (newFontSize === _defaults.minFontSize) {
            $(_defaults.fontDownBtn).prop('disabled', true);
        }

        if (newFontSize === _defaults.maxFontSize) {
            $(_defaults.fontUpBtn).prop('disabled', true);
        }
    }

    return viewer;
}(viewerJS || {}, jQuery);

},{}],5:[function(require,module,exports){
'use strict';

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
 * Module to manage the data table features.
 * 
 * @version 3.2.0
 * @module viewerJS.dataTable
 * @requires jQuery
 */
var viewerJS = function (viewer) {
    'use strict';

    var _debug = false;
    var _dataTablePaginator = null;
    var _txtField1 = null;
    var _txtField2 = null;
    var _totalCount = null;
    var _reloadBtn = null;
    var _defaults = {
        dataTablePaginator: '',
        txtField1: '',
        txtField2: '',
        totalCount: '',
        reloadBtn: ''
    };

    viewer.dataTable = {
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.dataTable.init');
                console.log('##############################');
                console.log('viewer.dataTable.init: config = ', config);
            }

            $.extend(true, _defaults, config);

            viewer.dataTable.paginator.init();

            if ($('.column-filter-wrapper').length > 0) {
                viewer.dataTable.filter.init();
            }
        },
        /**
         * Pagination
         */
        paginator: {
            setupAjax: false,
            init: function init() {
                if (_debug) {
                    console.log('---------- dataTable.paginator.init() ----------');
                }

                _dataTablePaginator = $(_defaults.dataTablePaginator);
                _txtField1 = $(_defaults.txtField1);
                _txtField2 = $(_defaults.txtField2);
                _totalCount = $(_defaults.totalCount);
                _reloadBtn = $(_defaults.reloadBtn);

                _txtField1.on('click', function () {
                    $(this).hide();
                    viewer.dataTable.paginator.inputFieldHandler();
                });

                _totalCount.on('click', function () {
                    _txtField1.hide();
                    viewer.dataTable.paginator.inputFieldHandler();
                });

                /*
                 * AJAX Eventlistener
                 */
                if (!this.setupAjax) {
                    jsf.ajax.addOnEvent(function (data) {
                        var ajaxstatus = data.status;

                        if (_defaults.dataTablePaginator.length > 0) {
                            switch (ajaxstatus) {
                                case "begin":
                                    if (_txtField1 !== null && _txtField2 !== null) {
                                        _txtField1.off();
                                        _txtField2.off();
                                    }
                                    break;
                                case "complete":
                                    break;
                                case "success":
                                    viewer.dataTable.paginator.init();
                                    break;
                            }
                        }
                    });
                    this.setupAjax = true;
                }
            },
            inputFieldHandler: function inputFieldHandler() {
                if (_debug) {
                    console.log('---------- dataTable.paginator.inputFieldHandler() ----------');
                }

                _txtField2.show().find('input').focus().select();

                _txtField2.find('input').on('blur', function () {
                    $(this).hide();
                    _txtField1.show();
                    _reloadBtn.click();
                });

                _txtField2.find('input').on('keypress', function (event) {
                    if (event.keyCode == 13) {
                        _reloadBtn.click();
                    } else {
                        return;
                    }
                });
            }
        },
        /**
         * Filter
         */
        filter: {
            setupAjax: false,
            init: function init() {
                if (_debug) {
                    console.log('---------- dataTable.filter.init() ----------');
                }

                $('#adminAllUserForm').on('submit', function (event) {
                    event.preventDefault();

                    $('.column-filter-wrapper').find('.btn-filter').click();
                });

                /*
                 * AJAX Eventlistener
                 */
                if (!this.setupAjax) {
                    jsf.ajax.addOnEvent(function (data) {
                        var ajaxstatus = data.status;

                        if (_defaults.dataTablePaginator.length > 0) {
                            switch (ajaxstatus) {
                                case "begin":
                                    break;
                                case "complete":
                                    break;
                                case "success":
                                    viewer.dataTable.filter.init();
                                    break;
                            }
                        }
                    });
                    this.setupAjax = true;
                }
            }
        }
    };

    return viewer;
}(viewerJS || {}, jQuery);

},{}],6:[function(require,module,exports){
'use strict';

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
 * Module to render a RSS-Feed sorted by date.
 * 
 * @version 3.2.0
 * @module viewerJS.dateSortedFeed
 * @requires jQuery
 * 
 */
var viewerJS = function (viewer) {
    'use strict';

    var _debug = false;
    var _promise = null;
    var _defaults = {
        path: null,
        dataSortOrder: null,
        dataCount: null,
        dataEncoding: null,
        feedBox: null,
        monthNames: ['', 'Januar', 'Februar', 'März', 'April', 'Mai', 'Juni', 'Juli', 'August', 'September', 'Oktober', 'November', 'Dezember']
    };

    viewer.dateSortedFeed = {
        /**
         * Method which initializes the date sorted RSS-Feed.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.path The rootpath of the application.
         * @param {Object} config.feedBox An jQuery object of the wrapper DIV.
         * @example
         * 
         * <pre>
         * var dateSortedFeedConfig = {
         *     path: '#{request.contextPath}',
         *     feedBox: $( '#dateSortedFeed' )
         * };
         * 
         * viewerJS.dateSortedFeed.setDataSortOrder( '#{cc.attrs.sorting}' );
         * viewerJS.dateSortedFeed.setDataCount( '#{cc.attrs.count}' );
         * viewerJS.dateSortedFeed.setDataEncoding( '#{cc.attrs.encoding}' );
         * viewerJS.dateSortedFeed.init( dateSortedFeedConfig );
         * </pre>
         */
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.dateSortedFeed.init');
                console.log('##############################');
                console.log('viewer.dateSortedFeed.init: feedBoxObj - ' + feedBoxObj);
                console.log('viewer.dateSortedFeed.init: path - ' + path);
            }

            $.extend(true, _defaults, config);

            var dataURL = _defaults.path;
            dataURL += '/api?action=query&q=PI:*&jsonFormat=datecentric&sortField=DATECREATED';
            dataURL += '&sortOrder=';
            dataURL += _defaults.dataSortOrder;
            dataURL += '&count=';
            dataURL += _defaults.dataCount;
            dataURL += '&encoding=';
            dataURL += _defaults.dataEncoding;

            if (_debug) {
                console.log('viewer.dateSortedFeed.init: dataURL - ' + dataURL);
            }

            // checking for feedbox element and render feed
            if (_defaults.feedBox) {
                _promise = viewer.helper.getRemoteData(dataURL);

                _promise.then(function (data) {
                    _renderFeed(data);
                }).then(null, function () {
                    console.error('ERROR: viewer.dateSortedFeed.init - ', error);
                });
            } else {
                return;
            }
        },
        /**
         * Returns the sorting order of the feed.
         * 
         * @method getDataSortOrder
         * @returns {String} The sorting order.
         * 
         */
        getDataSortOrder: function getDataSortOrder() {
            return _defaults.dataSortOrder;
        },
        /**
         * Sets the sorting order of the feed.
         * 
         * @method getDataSortOrder
         * @param {String} str The sorting order (asc/desc)
         * 
         */
        setDataSortOrder: function setDataSortOrder(str) {
            _defaults.dataSortOrder = str;
        },
        /**
         * Returns the number of entries from the feed.
         * 
         * @method getDataSortOrder
         * @returns {String} The number of entries.
         * 
         */
        getDataCount: function getDataCount() {
            return _defaults.dataCount;
        },
        /**
         * Sets the number of entries from the feed.
         * 
         * @method setDataCount
         * @param {String} The number of entries.
         * 
         */
        setDataCount: function setDataCount(num) {
            _defaults.dataCount = num;
        },
        /**
         * Returns the type of encoding.
         * 
         * @method getDataEncoding
         * @returns {String} The type of encoding.
         * 
         */
        getDataEncoding: function getDataEncoding() {
            return _defaults.dataEncoding;
        },
        /**
         * Sets the type of encoding.
         * 
         * @method setDataEncoding
         * @param {String} The type of encoding.
         * 
         */
        setDataEncoding: function setDataEncoding(str) {
            _defaults.dataEncoding = str;
        }
    };

    /**
     * Renders the feed and appends it to the wrapper.
     * 
     * @method _renderFeed
     * @param {Object} data An JSON object of the feed data.
     * 
     */
    function _renderFeed(data) {
        var feed = '';
        $.each(data, function (i, j) {
            feed += '<h4>' + _dateConverter(j.date) + '</h4>';
            $.each(j, function (m, n) {
                if (n.title) {
                    for (var x = 0; x <= n.title.length; x++) {
                        if (n.title[x] !== undefined) {
                            feed += '<div class="sorted-feed-title"><a href="';
                            feed += n.url;
                            feed += '" title="';
                            feed += n.title[x];
                            feed += '">';
                            feed += n.title[x];
                            feed += '</a></div>';
                        }
                    }
                }
            });
        });

        _defaults.feedBox.append(feed);
    }

    /**
     * Converts a date to this form: 16. November 2015
     * 
     * @method _dateConverter
     * @param {String} str The date to convert.
     * @returns {String} The new formated date.
     * 
     */
    function _dateConverter(str) {
        var strArr = str.split('-');
        var monthIdx = parseInt(strArr[1]);
        var newDate = strArr[2] + '. ' + _defaults.monthNames[monthIdx] + ' ' + strArr[0];

        return newDate;
    }

    return viewer;
}(viewerJS || {}, jQuery);

},{}],7:[function(require,module,exports){
'use strict';

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
 * Module which manages the download view.
 * 
 * @version 3.2.0
 * @module viewerJS.download
 * @requires jQuery
 * 
 */
var viewerJS = function (viewer) {
    'use strict';

    // default variables

    var _debug = false;
    var _checkbox = null;
    var _downloadBtn = null;

    viewer.download = {
        /**
         * Method to initialize the download view.
         * 
         * @method init
         */
        init: function init() {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.download.init');
                console.log('##############################');
            }
            _checkbox = $('#agreeLicense');
            _downloadBtn = $('#downloadBtn');

            _downloadBtn.prop('disabled', true);

            _checkbox.on('click', function () {
                var currState = $(this).prop('checked');

                viewer.download.checkboxValidation(currState);
            });
        },
        /**
         * Method which validates the checkstate of a checkbox and enables the download
         * button.
         * 
         * @method checkboxValidation
         * @param {String} state The current checkstate of the checkbox.
         */
        checkboxValidation: function checkboxValidation(state) {
            if (_debug) {
                console.log('---------- viewer.download.checkboxValidation() ----------');
                console.log('viewer.download.checkboxValidation: state = ', state);
            }

            if (state) {
                _downloadBtn.prop('disabled', false);
            } else {
                _downloadBtn.prop('disabled', true);
                return false;
            }
        }
    };

    return viewer;
}(viewerJS || {}, jQuery);

},{}],8:[function(require,module,exports){
'use strict';

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
 * Module which generates a download modal which dynamic content.
 * 
 * @version 3.2.0
 * @module viewerJS.downloadModal
 * @requires jQuery
 * 
 */
var viewerJS = function (viewer) {
    'use strict';

    // default variables

    var _debug = false;
    var _defaults = {
        dataType: null,
        dataTitle: null,
        dataId: null,
        dataPi: null,
        downloadBtn: null,
        reCaptchaSiteKey: '',
        useReCaptcha: true,
        path: '',
        iiifPath: '',
        apiUrl: '',
        userEmail: null,
        workInfo: {},
        modal: {
            id: '',
            label: '',
            string: {
                title: '',
                body: '',
                closeBtn: '',
                saveBtn: ''
            }
        },
        messages: {
            downloadInfo: {
                text: 'Informationen zum angeforderten Download',
                title: 'Werk',
                part: 'Teil',
                fileSize: 'Größe'
            },
            reCaptchaText: 'Um die Generierung von Dokumenten durch Suchmaschinen zu verhindern bestätigen Sie bitte das reCAPTCHA.',
            rcInvalid: 'Die Überprüfung war nicht erfolgreich. Bitte bestätigen Sie die reCAPTCHA Anfrage.',
            rcValid: 'Vielen Dank. Sie können nun ihre ausgewählte Datei generieren lassen.',
            eMailText: 'Um per E-Mail informiert zu werden sobald der Download zur Verfügung steht, können Sie hier optional Ihre E-Mail Adresse hinterlassen',
            eMailTextLoggedIn: 'Sie werden über Ihre registrierte E-Mail Adresse von uns über den Fortschritt des Downloads informiert.',
            eMail: ''
        }
    };
    var _loadingOverlay = null;

    viewer.downloadModal = {
        /**
         * Method to initialize the download modal mechanic.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.dataType The data type of the current file to download.
         * @param {String} config.dataTitle The title of the current file to download.
         * @param {String} config.dataId The LOG_ID of the current file to download.
         * @param {String} config.dataPi The PI of the current file to download.
         * @param {Object} config.downloadBtn A collection of all buttons with the class
         * attribute 'download-modal'.
         * @param {String} config.reCaptchaSiteKey The site key for the google reCAPTCHA,
         * fetched from the viewer config.
         * @param {String} config.path The current application path.
         * @param {String} config.apiUrl The URL to trigger the ITM download task.
         * @param {String} config.userEmail The current user email if the user is logged
         * in. Otherwise the one which the user enters or leaves blank.
         * @param {Object} config.modal A configuration object for the download modal.
         * @param {String} config.modal.id The ID of the modal.
         * @param {String} config.modal.label The label of the modal.
         * @param {Object} config.modal.string An object of strings for the modal content.
         * @param {String} config.modal.string.title The title of the modal.
         * @param {String} config.modal.string.body The content of the modal as HTML.
         * @param {String} config.modal.string.closeBtn Buttontext
         * @param {String} config.modal.string.saveBtn Buttontext
         * @param {Object} config.messages An object of strings for the used text
         * snippets.
         * @example
         * 
         * <pre>
         * var downloadModalConfig = {
         *     downloadBtn: $( '.download-modal' ),
         *     path: '#{navigationHelper.applicationUrl}',
         *     userEmail: $( '#userEmail' ).val(),
         *     messages: {
         *         reCaptchaText: '#{msg.downloadReCaptchaText}',
         *         rcInvalid: '#{msg.downloadRcInvalid}',
         *         rcValid: '#{msg.downloadRcValid}',
         *         eMailText: '#{msg.downloadEMailText}',
         *         eMailTextLoggedIn: '#{msg.downloadEMailTextLoggedIn}',
         *         eMail: '#{msg.downloadEmail}',
         *         closeBtn: '#{msg.downloadCloseModal}',
         *         saveBtn: '#{msg.downloadGenerateFile}',
         *     }
         * };
         * 
         * viewerJS.downloadModal.init( downloadModalConfig );
         * </pre>
         */
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.downloadModal.init');
                console.log('##############################');
                console.log('viewer.downloadModal.init: config = ', config);
            }

            $.extend(true, _defaults, config);

            // build loading overlay
            _loadingOverlay = $('<div />');
            _loadingOverlay.addClass('dl-modal__overlay');
            $('body').append(_loadingOverlay);

            _defaults.downloadBtn.on('click', function () {
                // show loading overlay
                $('.dl-modal__overlay').fadeIn('fast');

                _defaults.dataType = $(this).attr('data-type');
                _defaults.dataTitle = $(this).attr('data-title');
                if ($(this).attr('data-id') !== '') {
                    _defaults.dataId = $(this).attr('data-id');
                } else {
                    _defaults.dataId = '-';
                }
                _defaults.dataPi = $(this).attr('data-pi');
                _getWorkInfo(_defaults.dataPi, _defaults.dataId, _defaults.dataType).done(function (info) {
                    _defaults.workInfo = info;

                    _defaults.modal = {
                        id: _defaults.dataPi + '-Modal',
                        label: _defaults.dataPi + '-Label',
                        string: {
                            title: _defaults.dataTitle,
                            body: viewer.downloadModal.renderModalBody(_defaults.dataType, _defaults.workInfo),
                            closeBtn: _defaults.messages.closeBtn,
                            saveBtn: _defaults.messages.saveBtn
                        }
                    };

                    // hide loading overlay
                    $('.dl-modal__overlay').fadeOut('fast');

                    // init modal
                    viewer.downloadModal.initModal(_defaults);
                });
            });
        },
        /**
         * Method which initializes the download modal and its content.
         * 
         * @method initModal
         * @param {Object} params An config object which overwrites the defaults.
         */
        initModal: function initModal(params) {
            if (_debug) {
                console.log('---------- viewer.downloadModal.initModal() ----------');
                console.log('viewer.downloadModal.initModal: params = ', params);
            }
            $('body').append(viewer.helper.renderModal(params.modal));

            // disable submit button
            $('#submitModal').attr('disabled', 'disabled');

            // show modal
            $('#' + params.modal.id).modal('show');

            // render reCAPTCHA to modal
            $('#' + params.modal.id).on('shown.bs.modal', function (e) {
                if (_defaults.useReCaptcha) {
                    var rcWidget = grecaptcha.render('reCaptchaWrapper', {
                        sitekey: _defaults.reCaptchaSiteKey,
                        callback: function callback() {
                            var rcWidgetResponse = viewer.downloadModal.validateReCaptcha(grecaptcha.getResponse(rcWidget));

                            if (rcWidgetResponse) {
                                $('#modalAlerts').append(viewer.helper.renderAlert('alert-success', _defaults.messages.rcValid, true));

                                // enable submit button
                                $('#submitModal').removeAttr('disabled').on('click', function () {
                                    _defaults.userEmail = $('#recallEMail').val();

                                    _defaults.apiUrl = viewer.downloadModal.buildAPICall(_defaults.path, _defaults.dataType, _defaults.dataPi, _defaults.dataId, _defaults.userEmail);

                                    window.location.href = _defaults.apiUrl;
                                });
                            } else {
                                $('#modalAlerts').append(viewer.helper.renderAlert('alert-danger', _defaults.messages.rcInvalid, true));
                            }
                        }
                    });
                } else {
                    // hide paragraph
                    $(this).find('.modal-body h4').next('p').hide();

                    // enable submit button
                    $('#submitModal').removeAttr('disabled').on('click', function () {
                        _defaults.userEmail = $('#recallEMail').val();

                        _defaults.apiUrl = viewer.downloadModal.buildAPICall(_defaults.path, _defaults.dataType, _defaults.dataPi, _defaults.dataId, _defaults.userEmail);

                        window.location.href = _defaults.apiUrl;
                    });
                }
            });

            // remove modal from DOM after closing
            $('#' + params.modal.id).on('hidden.bs.modal', function (e) {
                $(this).remove();
            });
        },
        /**
         * Method which returns a HTML-String to render the download modal body.
         * 
         * @method renderModalBody
         * @param {String} type The current file type to download.
         * @param {String} title The title of the current download file.
         * @returns {String} The HTML-String to render the download modal body.
         */
        renderModalBody: function renderModalBody(type, infos) {
            if (_debug) {
                console.log('---------- viewer.downloadModal.renderModalBody() ----------');
                console.log('viewer.downloadModal.renderModalBody: type = ', type);
                console.log('viewer.downloadModal.renderModalBody: infos = ', infos);
            }
            var rcResponse = null;
            var modalBody = '';

            modalBody += '';
            // alerts
            modalBody += '<div id="modalAlerts"></div>';
            // Title
            if (type === 'pdf') {
                modalBody += '<h4>';
                modalBody += '<i class="fa fa-file-pdf-o" aria-hidden="true"></i> PDF-Download: ';
                modalBody += '</h4>';
            } else {
                modalBody += '<h4>';
                modalBody += '<i class="fa fa-file-text-o" aria-hidden="true"></i> ePub-Download: ';
                modalBody += '</h4>';
            }
            // Info
            modalBody += '<p>' + _defaults.messages.downloadInfo.text + ':</p>';
            modalBody += '<dl class="dl-horizontal">';
            modalBody += '<dt>' + _defaults.messages.downloadInfo.title + ':</dt>';
            modalBody += '<dd>' + infos.title + '</dd>';
            if (infos.div !== null) {
                modalBody += '<dt>' + _defaults.messages.downloadInfo.part + ':</dt>';
                modalBody += '<dd>' + infos.div + '</dd>';
            }
            if (infos.size) {
                modalBody += '<dt>' + _defaults.messages.downloadInfo.fileSize + ':</dt>';
                modalBody += '<dd>~' + infos.size + '</dd>';
                modalBody += '</dl>';
            }
            // reCAPTCHA
            if (_defaults.useReCaptcha) {
                modalBody += '<hr />';
                modalBody += '<p><strong>reCAPTCHA</strong></p>';
                modalBody += '<p>' + _defaults.messages.reCaptchaText + ':</p>';
                modalBody += '<div id="reCaptchaWrapper"></div>';
            }
            // E-Mail
            modalBody += '<hr />';
            modalBody += '<form class="email-form">';
            modalBody += '<div class="form-group">';
            modalBody += '<label for="recallEMail">' + _defaults.messages.eMail + '</label>';
            if (_defaults.userEmail != undefined) {
                modalBody += '<p class="help-block">' + _defaults.messages.eMailTextLoggedIn + '</p>';
                modalBody += '<input type="email" class="form-control" id="recallEMail" value="' + _defaults.userEmail + '" disabled="disabled" />';
            } else {
                modalBody += '<p class="help-block">' + _defaults.messages.eMailText + ':</p>';
                modalBody += '<input type="email" class="form-control" id="recallEMail" />';
            }
            modalBody += '</div>';
            modalBody += '</form>';

            return modalBody;
        },
        /**
         * Method which checks the reCAPTCHA response.
         * 
         * @method validateReCaptcha
         * @param {String} response The reCAPTCHA response.
         * @returns {Boolean} Returns true if the reCAPTCHA sent a response.
         */
        validateReCaptcha: function validateReCaptcha(response) {
            if (_debug) {
                console.log('---------- viewer.downloadModal.validateReCaptcha() ----------');
                console.log('viewer.downloadModal.validateReCaptcha: response = ', response);
            }
            if (response == 0) {
                return false;
            } else {
                return true;
            }
        },
        /**
         * Method which returns an URL to trigger the ITM download task.
         * 
         * @method buildAPICall
         * @param {String} path The current application path.
         * @param {String} type The current file type to download.
         * @param {String} pi The PI of the current work.
         * @param {String} logid The LOG_ID of the current work.
         * @param {String} email The current user email.
         * @returns {String} The URL to trigger the ITM download task.
         */
        buildAPICall: function buildAPICall(path, type, pi, logid, email) {
            if (_debug) {
                console.log('---------- viewer.downloadModal.buildAPICall() ----------');
                console.log('viewer.downloadModal.buildAPICall: path = ', path);
                console.log('viewer.downloadModal.buildAPICall: type = ', type);
                console.log('viewer.downloadModal.buildAPICall: pi = ', pi);
                console.log('viewer.downloadModal.buildAPICall: logid = ', logid);
                console.log('viewer.downloadModal.buildAPICall: email = ', email);
            }
            var url = '';

            url += path + 'rest/download';

            if (type == '') {
                url += '/-';
            } else {
                url += '/' + type;
            }
            if (pi == '') {
                url += '/-';
            } else {
                url += '/' + pi;
            }
            if (logid == '') {
                url += '/-';
            } else {
                url += '/' + logid;
            }
            if (email == '' || email == undefined) {
                url += '/-/';
            } else {
                url += '/' + email + '/';
            }

            return encodeURI(url);
        }
    };

    /**
     * Method which returns a promise if the work info has been reached.
     * 
     * @method getWorkInfo
     * @param {String} pi The PI of the work.
     * @param {String} logid The LOG_ID of the work.
     * @returns {Promise} A promise object if the info has been reached.
     */
    function _getWorkInfo(pi, logid, type) {
        if (_debug) {
            console.log('---------- _getWorkInfo() ----------');
            console.log('_getWorkInfo: pi = ', pi);
            console.log('_getWorkInfo: logid = ', logid);
            console.log('_getWorkInfo: type = ', type);
        }

        var restCall = '';
        var workInfo = {};

        if (logid !== '' || logid !== undefined) {
            restCall = _defaults.iiifPath + type + '/mets/' + pi + '/' + logid + '/';

            if (_debug) {
                console.log('if');
                console.log('_getWorkInfo: restCall = ', restCall);
            }
        } else {
            restCall = _defaults.iiifPath + type + '/mets/' + pi + '/-/';

            if (_debug) {
                console.log('else');
                console.log('_getWorkInfo: restCall = ', restCall);
            }
        }

        return viewerJS.helper.getRemoteData(restCall);
    }

    return viewer;
}(viewerJS || {}, jQuery);

},{}],9:[function(require,module,exports){
'use strict';

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
 * Module to edit comments in user generated content.
 * 
 * @version 3.2.0
 * @module viewerJS.editComment
 * @requires jQuery
 */
var viewerJS = function (viewer) {
    'use strict';

    var _debug = false;
    var _defaults = {
        commentCount: 0,
        editBtnCount: 0,
        deleteBtnCount: 0,
        deleteModalCount: 0,
        btnId: null,
        commentText: null
    };

    viewer.editComment = {
        /**
         * Method which initializes all required events to edit comments.
         * 
         * @method init
         * @example
         * 
         * <pre>
         * $( document ).ready( function() {
         *     viewerJS.editComment.init();
         * } );
         * </pre>
         */
        init: function init() {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.editComment.init');
                console.log('##############################');
            }

            // clear texarea for new comments
            if ($('textarea[id*="newCommentInput"]').val() !== '') {
                $('textarea[id*="newCommentInput"]').focus().val('');
            }

            // eventlisteners
            $('.comment-edit').on('click', function () {
                // hide add new comment field
                $('#newCommentField').fadeOut();

                // get button id
                _defaults.btnId = $(this).attr('id').replace('commentEditBtn-', '');

                // show textfield to edit comment and hide comment
                $('#addComment').hide();
                $('#commentEditComment-' + _defaults.btnId).prev().hide();
                $('#commentEditComment-' + _defaults.btnId).show();

                // hide edit button
                $('.comment-edit').hide();
            });

            $('.comment-abort').on('click', function () {
                // show add new comment field
                $('#newCommentField').fadeIn();

                // show edit button and comment
                $('.comments-comment-text').show();
                $('.comment-edit').show();
                $('#addComment').show();

                // hide textfield to edit comment
                $(this).parent().hide();
            });

            // add counting ids to elements
            $('.comment-edit').each(function () {
                $(this).attr('id', 'commentEditBtn-' + _defaults.editBtnCount);
                _defaults.editBtnCount++;
            });

            $('.comments-comment-edit').each(function () {
                $(this).attr('id', 'commentEditComment-' + _defaults.commentCount);
                _defaults.commentCount++;
            });

            $.each($('.comments-delete-btn'), function () {
                $(this).attr('data-toggle', 'modal').attr('data-target', '#deleteCommentModal-' + _defaults.deleteBtnCount);
                _defaults.deleteBtnCount++;
            });

            $.each($('.deleteCommentModal'), function () {
                $(this).attr('id', 'deleteCommentModal-' + _defaults.deleteModalCount);
                _defaults.deleteModalCount++;
            });
        }
    };

    return viewer;
}(viewerJS || {}, jQuery);

},{}],10:[function(require,module,exports){
'use strict';

var _typeof = typeof Symbol === "function" && typeof Symbol.iterator === "symbol" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; };

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
 * Module which includes mostly used helper functions.
 * 
 * @version 3.2.0
 * @module viewerJS.helper
 * @requires jQuery
 */
var viewerJS = function (viewer) {
    'use strict';

    // default variables

    var _debug = false;

    viewer.helper = {
        /**
         * Method to truncate a string to a given length.
         * 
         * @method truncateString
         * @param {String} str The string to truncate.
         * @param {Number} size The number of characters after the string should be
         * croped.
         * @returns {String} The truncated string.
         * @example
         * 
         * <pre>
         * viewerJS.helper.truncateString( $( '.something' ).text(), 75 );
         * </pre>
         */
        truncateString: function truncateString(str, size) {
            if (_debug) {
                console.log('---------- viewer.helper.truncateString() ----------');
                console.log('viewer.helper.truncateString: str = ', str);
                console.log('viewer.helper.truncateString: size = ', size);
            }

            var strSize = parseInt(str.length);

            if (strSize > size) {
                return str.substring(0, size) + '...';
            } else {
                return str;
            }
        },
        /**
         * Method which calculates the current position of the active element in sidebar
         * toc and the image container position and saves it to lacal storage.
         * 
         * @method saveSidebarTocPosition
         * @example
         * 
         * <pre>
         * viewerJS.helper.saveSidebarTocPosition();
         * </pre>
         */
        saveSidebarTocPosition: function saveSidebarTocPosition() {
            if (_debug) {
                console.log('---------- viewer.helper.saveSidebarTocPosition() ----------');
            }

            var scrollSidebarTocPosition = null;
            var savedIdDoc = localStorage.getItem('currIdDoc');
            var sidebarTocWrapper = '.widget-toc-elem-wrapp';
            var currElement = null;
            var currUrl = '';
            var parentLogId = '';

            if (viewer.localStoragePossible) {
                if (savedIdDoc !== 'false') {
                    currElement = $('li[data-iddoc="' + savedIdDoc + '"]');

                    if (currElement.length > 0) {
                        $(sidebarTocWrapper).scrollTop(currElement.offset().top - $(sidebarTocWrapper).offset().top + $(sidebarTocWrapper).scrollTop());
                        localStorage.setItem('currIdDoc', 'false');
                    } else {
                        localStorage.setItem('currIdDoc', 'false');
                    }

                    $('.widget-toc-elem-link a').on('click', function () {
                        parentLogId = $(this).parents('li').attr('data-iddoc');
                        localStorage.setItem('currIdDoc', parentLogId);
                    });
                } else {
                    localStorage.setItem('currIdDoc', 'false');

                    // expand click
                    $('.widget-toc-elem-expand a').on('click', function () {
                        scrollSidebarTocPosition = $(sidebarTocWrapper).scrollTop();

                        localStorage.setItem('sidebarTocScrollPosition', scrollSidebarTocPosition);
                    });

                    // link click
                    $('.widget-toc-elem-link a').on('click', function (event) {
                        event.preventDefault();

                        currUrl = $(this).attr('href');
                        scrollSidebarTocPosition = $(sidebarTocWrapper).scrollTop();
                        localStorage.setItem('sidebarTocScrollPosition', scrollSidebarTocPosition);
                        location.href = currUrl;
                    });

                    // scroll to saved position
                    $(sidebarTocWrapper).scrollTop(localStorage.getItem('sidebarTocScrollPosition'));
                }
            } else {
                return false;
            }
        },
        /**
         * Returns an JSON object from a API call.
         * 
         * @method getRemoteData
         * @param {String} url The API call URL.
         * @returns {Object} A promise object, which tells about the success of receiving
         * data.
         * @example
         * 
         * <pre>
         * viewerJS.helper.getRemoteData( dataURL );
         * </pre>
         */
        getRemoteData: function getRemoteData(url) {
            if (_debug) {
                console.log('---------- viewer.helper.getRemoteData() ----------');
                console.log('viewer.helper.getRemoteData: url = ', url);
            }

            var promise = Q($.ajax({
                url: decodeURI(url),
                type: "GET",
                dataType: "JSON",
                async: true
            }));

            return promise;
        },
        /**
         * Returns a BS Modal with dynamic content.
         * 
         * @method renderModal
         * @param {Object} config An config object which includes the content of the
         * modal.
         * @param {String} config.id The ID of the modal.
         * @param {String} config.label The label of the modal.
         * @param {Object} config.string An object of strings for the modal content.
         * @param {String} config.string.title The title of the modal.
         * @param {String} config.string.body The content of the modal as HTML.
         * @param {String} config.string.closeBtn Buttontext
         * @param {String} config.string.saveBtn Buttontext
         * @returns {String} A HTML-String which renders the modal.
         */
        renderModal: function renderModal(config) {
            if (_debug) {
                console.log('---------- viewer.helper.renderModal() ----------');
                console.log('viewer.helper.renderModal: config = ', config);
            }
            var _defaults = {
                id: 'myModal',
                label: 'myModalLabel',
                closeId: 'closeModal',
                submitId: 'submitModal',
                string: {
                    title: 'Modal title',
                    body: '',
                    closeBtn: 'Close',
                    saveBtn: 'Save changes'
                }
            };

            $.extend(true, _defaults, config);

            var modal = '';

            modal += '<div class="modal fade" id="' + _defaults.id + '" tabindex="-1" role="dialog" aria-labelledby="' + _defaults.label + '">';
            modal += '<div class="modal-dialog" role="document">';
            modal += '<div class="modal-content">';
            modal += '<div class="modal-header">';
            modal += '<button type="button" class="close" data-dismiss="modal" aria-label="' + _defaults.string.closeBtn + '">';
            modal += '<span aria-hidden="true">&times;</span>';
            modal += '</button>';
            modal += '<h4 class="modal-title" id="' + _defaults.label + '">' + _defaults.string.title + '</h4>';
            modal += '</div>';
            modal += '<div class="modal-body">' + _defaults.string.body + '</div>';
            modal += '<div class="modal-footer">';
            modal += '<button type="button" id="' + _defaults.closeId + '"  class="btn" data-dismiss="modal">' + _defaults.string.closeBtn + '</button>';
            modal += '<button type="button" id="' + _defaults.submitId + '" class="btn">' + _defaults.string.saveBtn + '</button>';
            modal += '</div></div></div></div>';

            return modal;
        },
        /**
         * Returns a BS Alert with dynamic content.
         * 
         * @method renderAlert
         * @param {String} type The type of the alert.
         * @param {String} content The content of the alert.
         * @param {Boolean} dismissable Sets the option to make the alert dismissable,
         * true = dismissable.
         * @returns {String} A HTML-String which renders the alert.
         */
        renderAlert: function renderAlert(type, content, dismissable) {
            if (_debug) {
                console.log('---------- viewer.helper.renderAlert() ----------');
                console.log('viewer.helper.renderAlert: type = ', type);
                console.log('viewer.helper.renderAlert: content = ', content);
                console.log('viewer.helper.renderAlert: dismissable = ', dismissable);
            }
            var bsAlert = '';

            bsAlert += '<div role="alert" class="alert ' + type + ' alert-dismissible fade in">';
            if (dismissable) {
                bsAlert += '<button aria-label="Close" data-dismiss="alert" class="close" type="button"><span aria-hidden="true">×</span></button>';
            }
            bsAlert += content;
            bsAlert += '</div>';

            return bsAlert;
        },
        /**
         * Method to get the version number of the used MS Internet Explorer.
         * 
         * @method detectIEVersion
         * @returns {Number} The browser version.
         */
        detectIEVersion: function detectIEVersion() {
            var ua = window.navigator.userAgent;

            // IE 10 and older
            var msie = ua.indexOf('MSIE ');
            if (msie > 0) {
                // IE 10 or older => return version number
                return parseInt(ua.substring(msie + 5, ua.indexOf('.', msie)), 10);
            }

            // IE 11
            var trident = ua.indexOf('Trident/');
            if (trident > 0) {
                // IE 11 => return version number
                var rv = ua.indexOf('rv:');
                return parseInt(ua.substring(rv + 3, ua.indexOf('.', rv)), 10);
            }

            // IE 12+
            var edge = ua.indexOf('Edge/');
            if (edge > 0) {
                // Edge (IE 12+) => return version number
                return parseInt(ua.substring(edge + 5, ua.indexOf('.', edge)), 10);
            }

            // other browser
            return false;
        },

        /**
         * Method to check if it´s possible to write to local Storage
         * 
         * @method checkLocalStorage
         * @returns {Boolean} true or false
         */
        checkLocalStorage: function checkLocalStorage() {
            if ((typeof localStorage === 'undefined' ? 'undefined' : _typeof(localStorage)) === 'object') {
                try {
                    localStorage.setItem('testLocalStorage', 1);
                    localStorage.removeItem('testLocalStorage');

                    return true;
                } catch (error) {
                    console.error('Not possible to write in local Storage: ', error);

                    return false;
                }
            }
        },

        /**
         * Method to render a warning popover.
         * 
         * @method renderWarningPopover
         * @param {String} msg The message to show in the popover.
         * @returns {Object} An jQuery Object to append to DOM.
         */
        renderWarningPopover: function renderWarningPopover(msg) {
            var popover = $('<div />');
            var popoverText = $('<p />');
            var popoverButton = $('<button />');
            var popoverButtonIcon = $('<i aria-hidden="true" />');

            popover.addClass('warning-popover');

            // build button
            popoverButton.addClass('btn-clean');
            popoverButton.attr('data-toggle', 'warning-popover');
            popoverButtonIcon.addClass('fa fa-times');
            popoverButton.append(popoverButtonIcon);
            popover.append(popoverButton);

            // build text
            popoverText.html(msg);
            popover.append(popoverText);

            return popover;
        },

        /**
         * Method to equal height of sidebar and content.
         * 
         * @method equalHeight
         * @param {String} sidebar The selector of the sidebar.
         * @param {String} content The selector of the content.
         */
        equalHeight: function equalHeight(sidebar, content) {
            if (_debug) {
                console.log('---------- viewer.helper.equalHeight() ----------');
                console.log('viewer.helper.equalHeight: sidebar = ', sidebar);
                console.log('viewer.helper.equalHeight: content = ', content);
            }

            var $sidebar = $(sidebar);
            var $content = $(content);
            var sidebarHeight = $sidebar.outerHeight();
            var contentHeight = $content.outerHeight();

            if (sidebarHeight > contentHeight) {
                $content.css({
                    'min-height': sidebarHeight + 'px'
                });
            }
        },

        /**
         * Method to validate the reCAPTCHA response.
         * 
         * @method validateReCaptcha
         * @param {String} wrapper The reCAPTCHA widget wrapper.
         * @param {String} key The reCAPTCHA site key.
         * @returns {Boolean} Returns true if the response is valid.
         */
        validateReCaptcha: function validateReCaptcha(wrapper, key) {
            var widget = grecaptcha.render(wrapper, {
                sitekey: key,
                callback: function callback() {
                    var response = grecaptcha.getResponse(widget);

                    console.log(response);

                    if (response == 0) {
                        return false;
                    } else {
                        return true;
                    }
                }
            });
        },

        /**
         * Method to get the current used browser.
         * 
         * @method getCurrentBrowser
         * @returns {String} The name of the current Browser.
         */
        getCurrentBrowser: function getCurrentBrowser() {
            // Opera 8.0+
            var isOpera = !!window.opr && !!opr.addons || !!window.opera || navigator.userAgent.indexOf(' OPR/') >= 0;
            // Firefox 1.0+
            var isFirefox = typeof InstallTrigger !== 'undefined';
            // Safari 3.0+ "[object HTMLElementConstructor]"
            var isSafari = /constructor/i.test(window.HTMLElement) || function (p) {
                return p.toString() === "[object SafariRemoteNotification]";
            }(!window['safari'] || typeof safari !== 'undefined' && safari.pushNotification);
            // Internet Explorer 6-11
            var isIE = /* @cc_on!@ */false || !!document.documentMode;
            // Edge 20+
            var isEdge = !isIE && !!window.StyleMedia;
            // Chrome 1+
            var isChrome = !!window.chrome && !!window.chrome.webstore;
            // Blink engine detection
            // var isBlink = ( isChrome || isOpera ) && !!window.CSS;

            if (isOpera) {
                return 'Opera';
            } else if (isFirefox) {
                return 'Firefox';
            } else if (isSafari) {
                return 'Safari';
            } else if (isIE) {
                return 'IE';
            } else if (isEdge) {
                return 'Edge';
            } else if (isChrome) {
                return 'Chrome';
            }
        }
    };

    viewer.localStoragePossible = viewer.helper.checkLocalStorage();

    return viewer;
}(viewerJS || {}, jQuery);

},{}],11:[function(require,module,exports){
'use strict';

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
 * Base-Module which initialize the global viewer object.
 * 
 * @version 3.2.0
 * @module viewerJS
 */
var viewerJS = function () {
    'use strict';

    var _debug = false;
    var _defaults = {
        currentPage: '',
        browser: '',
        sidebarSelector: '#sidebar',
        contentSelector: '#main',
        equalHeightRSSInterval: 1000,
        equalHeightInterval: 500,
        messageBoxSelector: '.messages .alert',
        messageBoxInterval: 1000,
        messageBoxTimeout: 8000,
        pageScrollSelector: '.icon-totop',
        pageScrollAnchor: '#top',
        widgetNerSidebarRight: false
    };

    var viewer = {};

    viewer.init = function (config) {
        if (_debug) {
            console.log('##############################');
            console.log('viewer.init');
            console.log('##############################');
            console.log('viewer.init: config - ', config);
        }

        $.extend(true, _defaults, config);

        // detect current browser
        _defaults.browser = viewerJS.helper.getCurrentBrowser();

        console.info('Current Page = ', _defaults.currentPage);
        console.info('Current Browser = ', _defaults.browser);

        /*
         * ! IE10 viewport hack for Surface/desktop Windows 8 bug Copyright 2014-2015
         * Twitter, Inc. Licensed under MIT
         * (https://github.com/twbs/bootstrap/blob/master/LICENSE)
         */

        // See the Getting Started docs for more information:
        // http://getbootstrap.com/getting-started/#support-ie10-width
        (function () {
            'use strict';

            if (navigator.userAgent.match(/IEMobile\/10\.0/)) {
                var msViewportStyle = document.createElement('style');
                msViewportStyle.appendChild(document.createTextNode('@-ms-viewport{width:auto!important}'));
                document.querySelector('head').appendChild(msViewportStyle);
            }
        })();

        // enable BS tooltips
        $('[data-toggle="tooltip"]').tooltip();

        // render warning if local storage is not useable
        if (!viewer.localStoragePossible) {
            var warningPopover = this.helper.renderWarningPopover('Ihr Browser befindet sich im privaten Modus und unterstützt die Möglichkeit Informationen zur Seite lokal zu speichern nicht. Aus diesem Grund sind nicht alle Funktionen des viewers verfügbar. Bitte verlasen Sie den privaten Modus oder benutzen einen alternativen Browser. Vielen Dank.');

            $('body').append(warningPopover);

            $('[data-toggle="warning-popover"]').on('click', function () {
                $('.warning-popover').fadeOut('fast', function () {
                    $('.warning-popover').remove();
                });
            });
        }

        // off canvas
        $('[data-toggle="offcanvas"]').click(function () {
            var icon = $(this).children('.glyphicon');

            $('.row-offcanvas').toggleClass('active');
            $(this).toggleClass('in');

            if (icon.hasClass('glyphicon-option-vertical')) {
                icon.removeClass('glyphicon-option-vertical').addClass('glyphicon-option-horizontal');
            } else {
                icon.removeClass('glyphicon-option-horizontal').addClass('glyphicon-option-vertical');
            }
        });

        // toggle mobile navigation
        $('[data-toggle="mobilenav"]').on('click', function () {
            $('.btn-toggle.search').removeClass('in');
            $('.header-actions__search').hide();
            $('.btn-toggle.language').removeClass('in');
            $('#changeLocal').hide();
            $('#mobileNav').slideToggle('fast');
        });
        $('[data-toggle="mobile-image-controls"]').on('click', function () {
            $('.image-controls__actions').slideToggle('fast');
        });

        // toggle language
        $('[data-toggle="language"]').on('click', function () {
            $('.btn-toggle.search').removeClass('in');
            $('.header-actions__search').hide();
            $(this).toggleClass('in');
            $('#changeLocal').fadeToggle('fast');
        });

        // toggle search
        $('[data-toggle="search"]').on('click', function () {
            $('.btn-toggle.language').removeClass('in');
            $('#changeLocal').hide();
            $(this).toggleClass('in');
            $('.header-actions__search').fadeToggle('fast');
        });

        // set content height to sidebar height
        if (window.matchMedia('(max-width: 768px)').matches) {
            if ($('.rss_wrapp').length > 0) {
                setTimeout(function () {
                    viewerJS.helper.equalHeight(_defaults.sidebarSelector, _defaults.contentSelector);
                }, _defaults.equalHeightRSSInterval);
            } else {
                setTimeout(function () {
                    viewerJS.helper.equalHeight(_defaults.sidebarSelector, _defaults.contentSelector);
                }, _defaults.equalHeightInterval);
            }
            $(window).on("orientationchange", function () {
                viewerJS.helper.equalHeight(_defaults.sidebarSelector, _defaults.contentSelector);
            });
        }

        // fade out message box if it exists
        (function () {
            var fadeoutScheduled = false;

            setInterval(function () {
                if ($(_defaults.messageBoxSelector).size() > 0) {
                    if (!fadeoutScheduled) {
                        fadeoutScheduled = true;
                        var messageTimer = setTimeout(function () {
                            $(_defaults.messageBoxSelector).each(function () {
                                $(this).fadeOut('slow', function () {
                                    $(this).parent().remove();
                                });
                            });

                            fadeoutScheduled = false;
                        }, _defaults.messageBoxTimeout);
                    }
                }
            }, _defaults.messageBoxInterval);
        })();

        // add class on toggle sidebar widget (CMS individual sidebar widgets)
        $('.collapse').on('show.bs.collapse', function () {
            $(this).prev().find('.glyphicon').removeClass('glyphicon-arrow-down').addClass('glyphicon-arrow-up');
        });

        $('.collapse').on('hide.bs.collapse', function () {
            $(this).prev().find('.glyphicon').removeClass('glyphicon-arrow-up').addClass('glyphicon-arrow-down');
        });

        // scroll page animated
        this.pageScroll.init(_defaults.pageScrollSelector, _defaults.pageScrollAnchor);

        // check for sidebar toc and set viewport position
        if (viewer.localStoragePossible) {
            if ($('#image_container').length > 0 || currentPage === 'readingmode') {
                if (localStorage.getItem('currIdDoc') === null) {
                    localStorage.setItem('currIdDoc', 'false');
                }

                this.helper.saveSidebarTocPosition();
            } else {
                localStorage.setItem('sidebarTocScrollPosition', 0);
            }
        }

        // AJAX Loader Eventlistener
        if (typeof jsf !== 'undefined') {
            jsf.ajax.addOnEvent(function (data) {
                var ajaxstatus = data.status;
                var ajaxloader = document.getElementById("AJAXLoader");
                var ajaxloaderSidebarToc = document.getElementById("AJAXLoaderSidebarToc");

                if (ajaxloaderSidebarToc && ajaxloader) {
                    switch (ajaxstatus) {
                        case "begin":
                            ajaxloaderSidebarToc.style.display = 'block';
                            ajaxloader.style.display = 'none';
                            break;

                        case "complete":
                            ajaxloaderSidebarToc.style.display = 'none';
                            ajaxloader.style.display = 'none';
                            break;

                        case "success":
                            // enable BS tooltips
                            $('[data-toggle="tooltip"]').tooltip();

                            if (viewer.localStoragePossible) {
                                viewer.helper.saveSidebarTocPosition();

                                $('.widget-toc-elem-wrapp').scrollTop(localStorage.sidebarTocScrollPosition);
                            }
                            // set content height to sidebar height
                            if (window.matchMedia('(max-width: 768px)').matches) {
                                viewerJS.helper.equalHeight(_defaults.sidebarSelector, _defaults.contentSelector);

                                $(window).off().on("orientationchange", function () {
                                    viewerJS.helper.equalHeight(_defaults.sidebarSelector, _defaults.contentSelector);
                                });
                            }
                            break;
                    }
                } else if (ajaxloader) {
                    switch (ajaxstatus) {
                        case "begin":
                            ajaxloader.style.display = 'block';
                            break;

                        case "complete":
                            ajaxloader.style.display = 'none';
                            break;

                        case "success":
                            // enable BS tooltips
                            $('[data-toggle="tooltip"]').tooltip();

                            // set content height to sidebar height
                            if (window.matchMedia('(max-width: 768px)').matches) {
                                viewerJS.helper.equalHeight(_defaults.sidebarSelector, _defaults.contentSelector);

                                $(window).off().on("orientationchange", function () {
                                    viewerJS.helper.equalHeight(_defaults.sidebarSelector, _defaults.contentSelector);
                                });
                            }
                            break;
                    }
                } else {
                    switch (ajaxstatus) {
                        case "success":
                            // enable BS tooltips
                            $('[data-toggle="tooltip"]').tooltip();

                            // set content height to sidebar height
                            if (window.matchMedia('(max-width: 768px)').matches) {
                                viewerJS.helper.equalHeight(_defaults.sidebarSelector, _defaults.contentSelector);

                                $(window).off().on("orientationchange", function () {
                                    viewerJS.helper.equalHeight(_defaults.sidebarSelector, _defaults.contentSelector);
                                });
                            }
                            break;
                    }
                }
            });
        }

        // disable submit button on feedback
        if (currentPage === 'feedback') {
            $('#submitFeedbackBtn').attr('disabled', true);
        }

        // set sidebar position for NER-Widget
        if ($('#widgetNerFacetting').length > 0) {
            nerFacettingConfig.sidebarRight = _defaults.widgetNerSidebarRight;
            this.nerFacetting.init(nerFacettingConfig);
        }

        // make sure only integer values may be entered in input fields of class
        // 'input-integer'
        $('.input-integer').on("keypress", function (event) {
            if (event.which < 48 || event.which > 57) {
                return false;
            }
        });

        // make sure only integer values may be entered in input fields of class
        // 'input-integer'
        $('.input-float').on("keypress", function (event) {
            console.log(event);
            switch (event.which) {
                case 8: // delete
                case 9: // tab
                case 13: // enter
                case 46: // dot
                case 44: // comma
                case 43: // plus
                case 45:
                    // minus
                    return true;
                case 118:
                    return event.ctrlKey; // copy
                default:
                    switch (event.keyCode) {
                        case 8: // delete
                        case 9: // tab
                        case 13:
                            // enter
                            return true;
                        default:
                            if (event.which < 48 || event.which > 57) {
                                return false;
                            } else {
                                return true;
                            }
                    }
            }
        });

        // set tinymce language
        this.tinyConfig.language = currentLang;

        if (currentPage === 'adminCmsCreatePage') {
            this.tinyConfig.setup = function (ed) {
                // listen to changes on tinymce input fields
                ed.on('change input paste', function (e) {
                    tinymce.triggerSave();
                    createPageConfig.prevBtn.attr('disabled', true);
                    createPageConfig.prevDescription.show();
                });
            };
            // TODO: Für ZLB in der custom.js einbauen
            // viewerJS.tinyConfig.textcolor_map = [ "FFFFFF", "ZLB-Weiß", "333333",
            // "ZLB-Schwarz", "dedede", "ZLB-Hellgrau", "727c87", "ZLB-Mittelgrau",
            // "9a9a9a", "ZLB-Dunkelgrau",
            // "CD0000", "ZLB-Rot", "92406d", "ZLB-Lila", "6f2c40", "ZLB-Bordeaux",
            // "ffa100", "ZLB-Orange", "669933", "ZLB-Grün", "3e5d1e", "ZLB-Dunkelgrün",
            // "a9d0f5",
            // "ZLB-Hellblau", "28779f", "ZLB-Blau" ];
        }

        if (currentPage === 'overview') {
            // activate menubar
            viewerJS.tinyConfig.menubar = true;
            viewerJS.tinyMce.overview();
        }

        // AJAX Loader Eventlistener for tinyMCE
        if (typeof jsf !== 'undefined') {
            jsf.ajax.addOnEvent(function (data) {
                var ajaxstatus = data.status;

                switch (ajaxstatus) {
                    case "success":
                        if (currentPage === 'overview') {
                            viewerJS.tinyMce.overview();
                        }

                        viewerJS.tinyMce.init(viewerJS.tinyConfig);
                        break;
                }
            });
        }

        // init tinymce if it exists
        if ($('.tinyMCE').length > 0) {
            viewerJS.tinyMce.init(this.tinyConfig);
        }

        // handle browser bugs
        switch (_defaults.browser) {
            case 'Chrome':
                /* BROKEN IMAGES */
                $('img').error(function () {
                    $(this).addClass('broken');
                });
                break;
            case 'Firefox':
                /* BROKEN IMAGES */
                $("img").error(function () {
                    $(this).hide();
                });
                /* 1px BUG */
                if ($('.image-doublePageView').length > 0) {
                    $('.image-doublePageView').addClass('oneUp');
                }
                break;
            case 'IE':
                /* SET IE CLASS TO HTML */
                $('html').addClass('is-IE');
                /* BROKEN IMAGES */
                $("img").error(function () {
                    $(this).hide();
                });
                break;
            case 'Edge':
                /* BROKEN IMAGES */
                $("img").error(function () {
                    $(this).hide();
                });
                break;
            case 'Safari':
                /* BROKEN IMAGES */
                $("img").error(function () {
                    $(this).hide();
                });
                break;
        }
    };

    // global object for tinymce config
    viewer.tinyConfig = {};

    return viewer;
}(jQuery);

},{}],12:[function(require,module,exports){
'use strict';

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
 * This module drives the functionality of the main navigation from the Goobi viewer.
 * 
 * @version 3.2.0
 * @module viewerJS.navigation
 * @requires jQuery
 */
var viewerJS = function (viewer) {
    'use strict';

    var _debug = false;
    var _defaults = {
        navigationSelector: '#navigation',
        subMenuSelector: '[data-toggle="submenu"]',
        megaMenuSelector: '[data-toggle="megamenu"]',
        closeMegaMenuSelector: '[data-toggle="close"]'
    };

    viewer.navigation = {
        /**
         * Method to initialize the viewer main navigation.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.navigationSelector The selector for the navigation
         * element.
         * @param {String} config.subMenuSelector The selector for the submenu element.
         * @param {String} config.megaMenuSelector The selector for the mega menu element.
         * @param {String} config.closeMegaMenuSelector The selector for the close mega
         * menu element.
         */
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.navigation.init');
                console.log('##############################');
                console.log('viewer.navigation.init: config - ', config);
            }

            $.extend(true, _defaults, config);

            // TRIGGER STANDARD MENU
            $(_defaults.subMenuSelector).on('click', function () {
                var currTrigger = $(this);

                if ($(this).parents('.navigation__submenu').hasClass('in')) {
                    _resetSubMenus();
                    currTrigger.parent().addClass('active');
                    currTrigger.next('.navigation__submenu').addClass('in');
                    _calcSubMenuPosition(currTrigger.next('.navigation__submenu'));
                } else {
                    _resetMenus();
                    currTrigger.parent().addClass('active');
                    currTrigger.next('.navigation__submenu').addClass('in');
                }
            });

            // TRIGGER MEGA MENU
            $(_defaults.megaMenuSelector).on('click', function () {
                _resetMenus();

                if ($(this).next('.navigation__megamenu-wrapper').hasClass('in')) {
                    $(this).parent().removeClass('active');
                    $(this).next('.navigation__megamenu-wrapper').removeClass('in');
                } else {
                    $('.navigation__megamenu-trigger').removeClass('active');
                    $('.navigation__megamenu-wrapper').removeClass('in');
                    $(this).parent().addClass('active');
                    $(this).next('.navigation__megamenu-wrapper').addClass('in');
                }
            });

            $(_defaults.closeMegaMenuSelector).on('click', function () {
                _resetMenus();
            });

            if ($('.navigation__megamenu-wrapper').length > 0) {
                _resetMenus();
            }

            // reset all menus by clicking on body
            $('body').on('click', function (event) {
                if (event.target.id == 'navigation' || $(event.target).closest(_defaults.navigationSelector).length) {
                    return;
                } else {
                    _resetMenus();
                }
            });
        }
    };

    /**
     * Method to reset all shown menus.
     * 
     * @method _resetMenus
     */
    function _resetMenus() {
        if (_debug) {
            console.log('---------- _resetMenus() ----------');
        }

        $('.navigation__submenu-trigger').removeClass('active');
        $('.navigation__submenu').removeClass('in');
        $('.navigation__megamenu-trigger').removeClass('active');
        $('.navigation__megamenu-wrapper').removeClass('in');
    }

    /**
     * Method to reset all shown submenus.
     * 
     * @method _resetSubMenus
     */
    function _resetSubMenus() {
        $('.level-2, .level-3, .level-4, .level-5').parent().removeClass('active');
        $('.level-2, .level-3, .level-4, .level-5').removeClass('in').removeClass('left');
    }

    /**
     * Method to calculate the position of the shown submenu.
     * 
     * @method _calcSubMenuPosition
     * @param {Object} menu An jQuery object of the current submenu.
     */
    function _calcSubMenuPosition(menu) {
        if (_debug) {
            console.log('---------- _clacSubMenuPosition() ----------');
            console.log('_clacSubMenuPosition: menu - ', menu);
        }

        var currentOffsetLeft = menu.offset().left;
        var menuWidth = menu.outerWidth();
        var windowWidth = $(window).outerWidth();
        var offsetWidth = currentOffsetLeft + menuWidth;

        if (_debug) {
            console.log('_clacSubMenuPosition: currentOffsetLeft - ', currentOffsetLeft);
            console.log('_clacSubMenuPosition: menuWidth - ', menuWidth);
            console.log('_clacSubMenuPosition: windowWidth - ', windowWidth);
            console.log('_clacSubMenuPosition: offsetWidth - ', offsetWidth);
        }

        if (offsetWidth >= windowWidth) {
            menu.addClass('left').css('width', menuWidth);
        } else {
            return false;
        }
    }

    return viewer;
}(viewerJS || {}, jQuery);

},{}],13:[function(require,module,exports){
'use strict';

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
 * Module which facetts the NER-Tags in a sidebar widget and Pageview.
 * 
 * @version 3.2.0
 * @module viewerJS.nerFacetting
 * @requires jQuery
 * @requires Bootstrap
 */
var viewerJS = function (viewer) {
    'use strict';

    var _debug = false;
    var _json = null;
    var _apiCall = '';
    var _html = '';
    var _pageCount = 0;
    var _scaleCount = 0;
    var _scaleHeight = 0;
    var _sliderHandlePosition = {};
    var _movedSliderHandlePosition = 0;
    var _recurrenceCount = 0;
    var _scaleValue = 0;
    var _sliderScaleHeight = 0;
    var _start = 0;
    var _end = 0;
    var _currentNerPageRangeSelected = '';
    var _currentNerPageRange = '';
    var _currentNerType = '';
    var _promise = null;
    var _defaults = {
        currentPage: '',
        baseUrl: '',
        apiUrl: '/rest/ner/tags/',
        workId: '',
        overviewTrigger: '',
        overviewContent: '',
        sectionTrigger: '',
        sectionContent: '',
        facettingTrigger: '',
        setTagRange: '',
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
            tags: 'Tags'
        }
    };

    viewer.nerFacetting = {
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
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.nerFacetting.init');
                console.log('##############################');
                console.log('viewer.nerFacetting.init: config - ', config);
            }

            $.extend(true, _defaults, config);

            if (viewer.localStoragePossible) {
                // show loader
                $(_defaults.loader).show();

                // clean local storage
                _cleanUpLocalStorage();

                // reset select menu
                $(_defaults.setTagRange).find('option').attr('selected', false);

                if (_defaults.currentPage === 'nerfacetting') {
                    $(_defaults.setTagRangeOverview).find('option[value="1"]').prop('selected', true);
                } else {
                    $(_defaults.setTagRangeOverview).find('option[value="10"]').prop('selected', true);
                }

                // reset facetting icons
                _resetFacettingIcons();

                // get data for current work
                if (_defaults.currentPage === 'nerfacetting') {
                    _apiCall = _getAllTagsOfARange(1, '-');
                } else {
                    _apiCall = _getAllTagsOfARange(10, '-');
                }

                _promise = viewer.helper.getRemoteData(_apiCall);

                _promise.then(function (json) {
                    _json = json;

                    // check if data is not empty
                    if (_json !== null || _json !== 'undefinded') {
                        // check if overview is already loaded
                        if ($(_defaults.overviewContent).html() === '') {
                            _renderOverview(_json);
                        }
                    } else {
                        _html = viewer.helper.renderAlert('alert-danger', _defaults.msg.noJSON + '<br /><br />URL: ' + _apiCall, true);
                        $(_defaults.overviewContent).html(_html);
                    }
                }).then(null, function (error) {
                    $('.facetting-content').empty().append(viewer.helper.renderAlert('alert-danger', '<strong>Status: </strong>' + error.status + ' ' + error.statusText, false));
                    console.error('ERROR: viewer.nerFacetting.init - ', error);
                });

                /**
                 * Event if overview tab is clicked.
                 */
                $(_defaults.overviewTrigger).on('click', function () {
                    // show loader
                    $(_defaults.loader).show();

                    // resets
                    $(_defaults.setTagRange).find('option').attr('selected', false);

                    if (_defaults.currentPage === 'nerfacetting') {
                        $(_defaults.setTagRangeOverview).find('option[value="1"]').prop('selected', true);
                        localStorage.setItem('currentNerPageRange', '1');
                    } else {
                        $(_defaults.setTagRangeOverview).find('option[value="10"]').prop('selected', true);
                        localStorage.setItem('currentNerPageRange', '10');
                    }
                    _currentNerPageRange = localStorage.getItem('currentNerPageRange');

                    localStorage.setItem('currentNerType', '-');
                    _currentNerType = localStorage.getItem('currentNerType');

                    _resetFacettingIcons();

                    // check if tab is active
                    if ($(this).parent().hasClass('active')) {
                        console.info('Overview is already active.');
                    } else {
                        if (_defaults.currentPage === 'nerfacetting') {
                            _apiCall = _getAllTagsOfARange(1, '-');
                        } else {
                            _apiCall = _getAllTagsOfARange(10, '-');
                        }

                        _promise = viewer.helper.getRemoteData(_apiCall);

                        _promise.then(function (json) {
                            _json = json;
                            _renderOverview(_json);
                        }).then(null, function (error) {
                            $('.facetting-content').empty().append(viewer.helper.renderAlert('alert-danger', '<strong>Status: </strong>' + error.status + ' ' + error.statusText, false));
                            console.error('ERROR: viewer.nerFacetting.init - ', error);
                        });
                    }
                });

                /**
                 * Event if section tab is clicked.
                 */
                $(_defaults.sectionTrigger).on('click', function () {
                    // show loader
                    $(_defaults.loader).show();

                    // reset select menu
                    $(_defaults.setTagRange).find('option').attr('selected', false);

                    if (_defaults.currentPage === 'nerfacetting') {
                        $(_defaults.setTagRangeSection).find('option[value="5"]').prop('selected', true);
                    } else {
                        $(_defaults.setTagRangeSection).find('option[value="10"]').prop('selected', true);
                    }

                    // reset facetting
                    _resetFacettingIcons();

                    // set local storage value
                    if (_defaults.currentPage === 'nerfacetting') {
                        localStorage.setItem('currentNerPageRange', 5);
                    } else {
                        localStorage.setItem('currentNerPageRange', 10);
                    }
                    _currentNerPageRange = localStorage.getItem('currentNerPageRange');
                    localStorage.setItem('currentNerType', '-');
                    _currentNerType = localStorage.getItem('currentNerType');

                    // check if tab is active
                    if ($(this).parent().hasClass('active')) {
                        console.info('Section is already active.');
                    } else {
                        _renderSection();

                        // reset section stripe
                        $(_defaults.sliderSectionStripe).css('top', '0px');
                    }
                });

                /**
                 * Event if select menu changes.
                 */
                $(_defaults.setTagRange).on('change', function () {
                    var currVal = $(this).val();
                    _currentNerType = localStorage.getItem('currentNerType');

                    // show loader
                    $(_defaults.loader).show();

                    // save current value in local storage
                    localStorage.setItem('currentNerPageRange', currVal);
                    _currentNerPageRange = localStorage.getItem('currentNerPageRange');

                    // render overview
                    if ($(this).hasClass('overview')) {
                        if (_currentNerType === null || _currentNerType === '') {
                            _currentNerType = '-';
                        }
                        _apiCall = _getAllTagsOfARange(currVal, _currentNerType);

                        _promise = viewer.helper.getRemoteData(_apiCall);

                        _promise.then(function (json) {
                            _json = json;

                            // check if data is not empty
                            if (_json !== null || _json !== 'undefinded') {
                                _renderOverview(_json);
                            } else {
                                _html = viewer.helper.renderAlert('alert-danger', _defaults.msg.noJSON + '<br /><br />URL: ' + _apiCall, true);
                                $(_defaults.overviewContent).html(_html);
                            }
                        }).then(null, function (error) {
                            $('.facetting-content').empty().append(viewer.helper.renderAlert('alert-danger', '<strong>Status: </strong>' + error.status + ' ' + error.statusText, false));
                            console.error('ERROR: viewer.nerFacetting.init - ', error);
                        });
                    }
                    // render section
                    else {
                            // setup values
                            localStorage.setItem('currentNerPageRange', currVal);
                            _currentNerPageRange = localStorage.getItem('currentNerPageRange');

                            _renderSection();

                            // reset section stripe
                            if (_currentNerPageRange > _pageCount) {
                                $(_defaults.sliderSectionStripe).css({
                                    'top': '0px',
                                    'height': '600px'
                                });
                            } else {
                                $(_defaults.sliderSectionStripe).css({
                                    'top': '0px',
                                    'height': '100px'
                                });
                            }
                        }
                });

                /**
                 * Event if facetting icons are clicked.
                 */
                $(_defaults.facettingTrigger).on('click', function () {
                    var currType = $(this).attr('data-type');

                    // show loader
                    $(_defaults.loader).show();

                    // set values
                    localStorage.setItem('currentNerType', currType);
                    _currentNerType = localStorage.getItem('currentNerType');

                    if (_defaults.currentPage === 'nerfacetting') {
                        if (_currentNerPageRange == null || _currentNerPageRange === '') {
                            _currentNerPageRange = localStorage.setItem('currentNerPageRange', 1);
                        }
                    } else {
                        if (_currentNerPageRange == null || _currentNerPageRange === '') {
                            _currentNerPageRange = localStorage.setItem('currentNerPageRange', 10);
                        }
                    }
                    _currentNerPageRange = localStorage.getItem('currentNerPageRange');

                    // activate icons
                    $('.facetting-trigger').removeClass('active');
                    $(this).addClass('active');
                    $('.reset-filter').show();

                    // filter overview
                    if ($(this).parent().parent().parent().attr('id') === 'overview') {
                        // setup data
                        _apiCall = _getAllTagsOfARange(_currentNerPageRange, _currentNerType);

                        _promise = viewer.helper.getRemoteData(_apiCall);

                        _promise.then(function (json) {
                            _json = json;

                            _renderOverview(_json);

                            // hide select all
                            if ($(this).parent().hasClass('reset-filter')) {
                                $(this).parent().hide();
                            }
                            // set icons to active if "all" is selected
                            if (_currentNerType === '-') {
                                $('.facetting-trigger').addClass('active');
                            }
                        }).then(null, function (error) {
                            $('.facetting-content').empty().append(viewer.helper.renderAlert('alert-danger', '<strong>Status: </strong>' + error.status + ' ' + error.statusText, false));
                            console.error('ERROR: viewer.nerFacetting.init - ', error);
                        });
                    }
                    // filter section
                    else {
                            _renderSection();

                            // hide select all
                            if ($(this).parent().hasClass('reset-filter')) {
                                $(this).parent().hide();
                            }
                            // set icons to active if "all" is selected
                            if (_currentNerType === '-') {
                                $('.facetting-trigger').addClass('active');
                            }
                            // reset section stripe
                            $(_defaults.sliderSectionStripe).css('top', '0px');
                        }
                });
            } else {
                $('.facetting-content').empty().append(viewer.helper.renderAlert('alert-danger', '<strong>Deactivated: </strong>Not possible to write in local Storage!', false));
            }
        }
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

        _html = '';
        _html += '<ul class="overview-scale">';

        // render page number
        $.each(data.pages, function (p, page) {
            _html += '<li>';
            _html += '<div class="page-number">';
            if (data.rangeSize == 1) {
                if (_defaults.currentPage === 'nerfacetting') {
                    _html += '<a href="' + _defaults.baseUrl + '/image/' + _defaults.workId + '/' + page.pageOrder + '/">';
                } else {
                    _html += '<a href="' + _defaults.baseUrl + '/' + _defaults.currentPage + '/' + _defaults.workId + '/' + page.pageOrder + '/">';
                }
                _html += page.pageOrder;
                _html += '</a>';
            } else {
                if (_defaults.currentPage === 'nerfacetting') {
                    if (page.firstPage !== undefined || page.lastPage !== undefined) {
                        _html += '<a href="' + _defaults.baseUrl + '/image/' + _defaults.workId + '/' + page.firstPage + '/">';
                        _html += page.firstPage + '-' + page.lastPage;
                        _html += '</a>';
                    } else {
                        _html += '<a href="' + _defaults.baseUrl + '/image/' + _defaults.workId + '/' + page.pageOrder + '/">';
                        _html += page.pageOrder;
                        _html += '</a>';
                    }
                } else {
                    if (page.firstPage !== undefined || page.lastPage !== undefined) {
                        _html += '<a href="' + _defaults.baseUrl + '/' + _defaults.currentPage + '/' + _defaults.workId + '/' + page.firstPage + '/">';
                        _html += page.firstPage + '-' + page.lastPage;
                        _html += '</a>';
                    } else {
                        _html += '<a href="' + _defaults.baseUrl + '/' + _defaults.currentPage + '/' + _defaults.workId + '/' + page.pageOrder + '/">';
                        _html += page.pageOrder;
                        _html += '</a>';
                    }
                }
            }
            _html += '</div>';
            _html += '<div class="tag-container">';

            // render tags
            if (page.tags.length === 0 || page.tags.length === 'undefined') {
                _html += '<span class="page-tag empty">' + _defaults.msg.emptyTag + '</span>';
            } else {
                $.each(page.tags, function (t, tag) {
                    _html += '<span class="page-tag ' + tag.type + '">' + tag.value + '</span>';
                });
            }
            _html += '</div>';
            _html += '</li>';
        });
        _html += '</ul>';

        $(_defaults.overviewContent).hide().html(_html).find('.tag-container').each(function () {
            $(this).children('.page-tag').slice(_defaults.recurrenceNumber).remove();
        });
        $(_defaults.overviewContent).show();

        // hide loader
        $(_defaults.loader).hide();

        $('.tag-container').on({
            'mouseover': function mouseover() {
                var $this = $(this);

                _showCurrentTags($this);
            },
            'mouseout': function mouseout() {
                _hideCurrentTags();
            }
        });
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

        if (_defaults.sidebarRight) {
            if (_defaults.currentPage === 'nerfacetting') {
                $(_defaults.currentTags).html(content).css({
                    'display': 'block',
                    'top': pos.top + 25 + 'px'
                });
            } else {
                $(_defaults.currentTags).addClass('right').html(content).css({
                    'display': 'block',
                    'top': pos.top - 2 + 'px',
                    'left': 'auto',
                    'right': '100%'
                });
            }
        } else {
            if (_defaults.currentPage === 'nerfacetting') {
                $(_defaults.currentTags).html(content).css({
                    'display': 'block',
                    'top': pos.top + 25 + 'px'
                });
            } else {
                $(_defaults.currentTags).html(content).css({
                    'display': 'block',
                    'top': pos.top - 2 + 'px'
                });
            }
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
            console.log('_renderSection: _currentNerPageRange = ', _currentNerPageRange);
            console.log('_renderSection: _currentNerType = ', _currentNerType);
        }

        // set values
        _apiCall = _getAllTags();

        _promise = viewer.helper.getRemoteData(_apiCall);

        _promise.then(function (workCall) {
            _pageCount = _getPageCount(workCall);

            if (_currentNerPageRange === null || _currentNerPageRange === '') {
                _currentNerPageRange = localStorage.getItem('currentNerPageRange');
            }
            if (_currentNerType === null || _currentNerType === '') {
                _currentNerType = localStorage.getItem('currentNerType');
            }

            // render page count to scale
            if (_defaults.currentPage === 'nerfacetting') {
                $('#sliderScale .scale-page.end').html(_pageCount);
            } else {
                if (_pageCount > 1000) {
                    $('#sliderScale .scale-page.end').html('999+');
                } else {
                    $('#sliderScale .scale-page.end').html(_pageCount);
                }
            }

            // init slider
            $(_defaults.slider).slider({
                orientation: "vertical",
                range: false,
                min: 1,
                max: _pageCount,
                value: _pageCount,
                slide: function slide(event, ui) {
                    _sliderHandlePosition = $(_defaults.sliderHandle).position();
                    _scaleValue = _pageCount + 1 - ui.value;

                    // show bubble
                    $('.page-bubble').show();
                    _renderPageBubble(_scaleValue);
                },
                start: function start() {
                    _sliderHandlePosition = $(_defaults.sliderHandle).position();
                    _movedSliderHandlePosition = _sliderHandlePosition.top;
                },
                stop: function stop(event, ui) {
                    _currentNerType = localStorage.getItem('currentNerType');
                    _sliderScaleHeight = $(_defaults.sliderScale).height();

                    // set position of section stripe
                    if (_currentNerPageRange > _pageCount) {
                        $(_defaults.sliderSectionStripe).css({
                            'top': '0px',
                            'height': '600px'
                        });
                    } else {
                        if (_sliderHandlePosition.top < 100) {
                            $(_defaults.sliderSectionStripe).animate({
                                'top': '0px',
                                'height': '100px'
                            });
                        } else if (_sliderHandlePosition.top > 100) {
                            if (_sliderHandlePosition.top > 500) {
                                $(_defaults.sliderSectionStripe).animate({
                                    'top': _sliderScaleHeight - 100 + 'px',
                                    'height': '100px'
                                });
                            } else {
                                if (_movedSliderHandlePosition < _sliderHandlePosition.top) {
                                    $(_defaults.sliderSectionStripe).animate({
                                        'top': _sliderHandlePosition.top - 25 + 'px',
                                        'height': '100px'
                                    });
                                } else {
                                    $(_defaults.sliderSectionStripe).animate({
                                        'top': _sliderHandlePosition.top - 50 + 'px',
                                        'height': '100px'
                                    });
                                }
                            }
                        }
                    }

                    // render tags
                    switch (_currentNerPageRange) {
                        case '5':
                            _start = _scaleValue - 2;
                            _end = _scaleValue + 3;

                            while (_start < 1) {
                                _start++;
                                _end++;
                            }
                            while (_end > _pageCount) {
                                _start--;
                                _end--;
                            }

                            _apiCall = _getAllTagsOfPageSection(_start, _end, _currentNerType);
                            break;
                        case '10':
                            _start = _scaleValue - 5;
                            _end = _scaleValue + 5;

                            while (_start < 1) {
                                _start++;
                                _end++;
                            }
                            while (_end > _pageCount) {
                                _start--;
                                _end--;
                            }

                            _apiCall = _getAllTagsOfPageSection(_start, _end, _currentNerType);
                            break;
                        case '50':
                            _start = _scaleValue - 25;
                            _end = _scaleValue + 25;

                            while (_start < 1) {
                                _start++;
                                _end++;
                            }
                            while (_end > _pageCount) {
                                _start--;
                                _end--;
                            }

                            _apiCall = _getAllTagsOfPageSection(_start, _end, _currentNerType);
                            break;
                        case '100':
                            _start = _scaleValue - 50;
                            _end = _scaleValue + 50;

                            while (_start < 1) {
                                _start++;
                                _end++;
                            }
                            while (_end > _pageCount) {
                                _start--;
                                _end--;
                            }

                            _apiCall = _getAllTagsOfPageSection(_start, _end, _currentNerType);
                            break;
                    }

                    _promise = viewer.helper.getRemoteData(_apiCall);

                    _promise.then(function (json) {
                        _json = json;

                        _html = _renderSectionTags(_json);

                        if (_html === '') {
                            $(_defaults.sectionTags).hide().html(viewer.helper.renderAlert('alert-warning', _defaults.msg.emptyTag, false)).show();
                        } else {
                            $(_defaults.sectionTags).hide().html(_html).each(function () {
                                $(this).children('.page-tag').slice(_defaults.recurrenceSectionNumber).remove();
                            });
                            $(_defaults.sectionTags).show();
                        }

                        // hide bubble
                        $('.page-bubble').fadeOut();
                    }).then(null, function (error) {
                        $('.facetting-content').empty().append(viewer.helper.renderAlert('alert-danger', '<strong>Status: </strong>' + error.status + ' ' + error.statusText, false));
                        console.error('ERROR: viewer.nerFacetting.init - ', error);
                    });
                }
            });

            // render section tags
            _apiCall = _getAllTagsOfPageSection(0, _currentNerPageRange, _currentNerType);

            _promise = viewer.helper.getRemoteData(_apiCall);

            _promise.then(function (json) {
                _json = json;

                _html = _renderSectionTags(_json);

                if (_html === '') {
                    $(_defaults.sectionTags).hide().html(viewer.helper.renderAlert('alert-warning', _defaults.msg.emptyTag, false)).show();
                } else {
                    $(_defaults.sectionTags).hide().html(_html).each(function () {
                        $(this).children('.page-tag').slice(_defaults.recurrenceSectionNumber).remove();
                    });
                    $(_defaults.sectionTags).show();
                }

                // hide loader
                $(_defaults.loader).hide();
            }).then(null, function (error) {
                $('.facetting-content').empty().append(viewer.helper.renderAlert('alert-danger', '<strong>Status: </strong>' + error.status + ' ' + error.statusText, false));
                console.error('ERROR: viewer.nerFacetting.init - ', error);
            });
        }).then(null, function (error) {
            $('.facetting-content').empty().append(viewer.helper.renderAlert('alert-danger', '<strong>Status: </strong>' + error.status + ' ' + error.statusText, false));
            console.error('ERROR: viewer.nerFacetting.init - ', error);
        });
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
        // render tags
        $.each(data.pages, function (p, page) {
            if (page.tags.length === 0 || page.tags.length === 'undefined') {
                _html += '';
            } else {
                $.each(page.tags, function (t, tag) {
                    if (_defaults.currentPage === 'nerfacetting') {
                        if (tag.counter < 10) {
                            _html += '<span class="page-tag ' + tag.type + '" style="font-size: 1.' + tag.counter + 'rem;">' + tag.value + '</span>';
                        } else {
                            _html += '<span class="page-tag ' + tag.type + '" style="font-size: 2rem;">' + tag.value + '</span>';
                        }
                    } else {
                        if (tag.counter < 10) {
                            _html += '<span class="page-tag ' + tag.type + '" style="font-size: 1' + tag.counter + 'px;">' + tag.value + '</span>';
                        } else {
                            _html += '<span class="page-tag ' + tag.type + '" style="font-size: 19px;">' + tag.value + '</span>';
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

        switch (_currentNerPageRange) {
            case '5':
                _start = page - 2;
                _end = page + 3;

                while (_start < 1) {
                    _start++;
                    _end++;
                }
                while (_end > _pageCount) {
                    _start--;
                    _end--;
                }

                pageBubble += '<span class="page-bubble">' + _start + '-' + _end + '</span>';
                break;
            case '10':
                _start = page - 5;
                _end = page + 5;

                while (_start < 1) {
                    _start++;
                    _end++;
                }
                while (_end > _pageCount) {
                    _start--;
                    _end--;
                }

                pageBubble += '<span class="page-bubble">' + _start + '-' + _end + '</span>';
                break;
            case '50':
                _start = page - 25;
                _end = page + 25;

                while (_start < 1) {
                    _start++;
                    _end++;
                }
                while (_end > _pageCount) {
                    _start--;
                    _end--;
                }

                pageBubble += '<span class="page-bubble">' + _start + '-' + _end + '</span>';
                break;
            case '100':
                _start = page - 50;
                _end = page + 50;

                while (_start < 1) {
                    _start++;
                    _end++;
                }
                while (_end > _pageCount) {
                    _start--;
                    _end--;
                }

                pageBubble += '<span class="page-bubble">' + _start + '-' + _end + '</span>';
                break;
        }

        $('#sliderVertical .ui-slider-handle').html(pageBubble);
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

        localStorage.removeItem('currentNerPageRange');
        localStorage.removeItem('currentNerType');
    }

    /**
     * API-Calls
     */
    // get all tags from all pages: /rest/ner/tags/{pi}/
    function _getAllTags() {
        return _defaults.baseUrl + _defaults.apiUrl + _defaults.workId;
    }

    // get all tags of a range: /viewer/rest/ner/tags/ranges/{range}/{type}/{pi}/
    function _getAllTagsOfARange(range, type) {
        return _defaults.baseUrl + _defaults.apiUrl + 'ranges/' + range + '/' + type + '/' + _defaults.workId + '/';
    }

    // get all tags sorted of type: /rest/ner/tags/{type}/{pi}/
    function _getAllTagsOfAType(type) {
        return _defaults.baseUrl + _defaults.apiUrl + type + '/' + _defaults.workId + '/';
    }

    // get all tags sorted of recurrence (asc/desc):
    // /rest/ner/tags/recurrence/{type}/{order}/{pi}/
    function _getAllTagsOfRecurrence(type, order) {
        if (type === '-') {
            return _defaults.baseUrl + _defaults.apiUrl + 'recurrence/-/' + order + '/' + _defaults.workId + '/';
        } else {
            return _defaults.baseUrl + _defaults.apiUrl + 'recurrence/' + type + '/' + order + '/' + _defaults.workId + '/';
        }
    }

    // get all tags sorted of page section: /rest/ner/tags/{start}/{end}/{pi}/
    function _getAllTagsOfPageSection(start, end, type) {
        return _defaults.baseUrl + _defaults.apiUrl + start + '/' + end + '/' + type + '/' + _defaults.workId + '/';
    }

    return viewer;
}(viewerJS || {}, jQuery);

},{}],14:[function(require,module,exports){
'use strict';

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
 * Module to render NER-Popovers in fulltext pages.
 * 
 * @version 3.2.0
 * @module viewerJS.nerFulltext
 * @requires jQuery
 * 
 */
var viewerJS = function (viewer) {
    'use strict';

    // define variables

    var _debug = false;
    var _defaults = {
        path: null,
        lang: {}
    };
    var _contextPath = null;
    var _lang = null;

    viewer.nerFulltext = {
        /**
         * Method which initializes the NER Popover methods.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.path The rootpath of the application.
         * @example
         * 
         * <pre>
         * var nerConfig = {
         *     path: '#{request.contextPath}'
         * };
         * 
         * viewerJS.nerFulltext.init( nerConfig );
         * </pre>
         */
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.nerFulltext.init');
                console.log('##############################');
                console.log('viewer.nerFulltext.init: config - ');
                console.log(config);
            }

            $.extend(true, _defaults, config);

            _initNerPopover(_defaults.path);
        }
    };

    /**
     * Method which fetches data from the API and returns an JSON object.
     * 
     * @method _getRemoteData
     * @param {String} target The API call URL.
     * @returns {Object} The JSON object with the API data.
     * 
     */
    function _getRemoteData(target) {
        if (_debug) {
            console.log('viewer.nerFulltext _getRemoteData: target - ');
            console.log(target);
        }

        // show preloader for current element
        $('.ner-detail-loader', target).css({
            display: 'inline-block'
        });

        // AJAX call
        var data = $.ajax({
            url: decodeURI($(target).attr('data-remotecontent')),
            type: 'POST',
            dataType: 'JSON',
            async: false,
            complete: function complete() {
                $('.ner-detail-loader').hide();
            }
        }).responseText;

        return data;
    }

    /**
     * Method which initializes the events for the NER-Popovers.
     * 
     * @method _initNerPopover
     * @param {String} path The root path of the application.
     * 
     */
    function _initNerPopover(path) {
        if (_debug) {
            console.log('viewer.nerFulltext _initNerPopover: path - ' + path);
        }

        var data, position, title, triggerCoords, textBox, textBoxPosition, textBoxCoords;

        $('.ner-trigger').on('click', function () {
            $('body').find('.ner-popover-pointer').hide();
            $('body').find('.ner-popover').remove();
            data = _getRemoteData($(this));
            position = $(this).position();
            triggerCoords = {
                top: position.top,
                left: position.left,
                width: $(this).outerWidth()
            };
            textBox = $('#view_fulltext_wrapp');
            textBoxPosition = textBox.position();
            textBoxCoords = {
                top: textBoxPosition.top,
                left: 0,
                right: textBoxPosition.left + textBox.outerWidth()
            };
            title = $(this).attr('title');

            textBox.append(_renderNerPopover(data, _calculateNerPopoverPosition(triggerCoords, textBoxCoords), title, path));

            if ($('.ner-popover')) {
                $(this).find('.ner-popover-pointer').show();
                _removeNerPopover();

                $('.ner-detail-trigger').on('click', function () {
                    data = _getRemoteData($(this));
                    title = $(this).attr('title');

                    $(this).parent().next('.ner-popover-detail').html(_renderNerPopoverDetail(data, title));
                });
            }
        });
    }

    /**
     * Method which renders a popover to the DOM.
     * 
     * @method _renderNerPopover
     * @param {Object} data The JSON object from the API.
     * @param {Object} position A jQuery object including the position of the clicked
     * trigger.
     * @param {String} title The value of the title attribute from the clicked trigger.
     * @param {String} path The root path of the application.
     * @returns {String} The HTML string which renders the popover.
     * 
     */
    function _renderNerPopover(data, position, title, path) {
        if (_debug) {
            console.log('viewer.nerFulltext _renderNerPopover: data - ');
            console.log(data);
            console.log('viewer.nerFulltext _renderNerPopover: position - ');
            console.log(position);
            console.log('viewer.nerFulltext _renderNerPopover: title - ' + title);
            console.log('viewer.nerFulltext _renderNerPopover: path - ' + path);
        }

        var positionTop = position.top;
        var positionLeft = position.left;
        var popover = '';

        popover += '<div class="ner-popover" style="top:' + positionTop + 'px; left:' + positionLeft + 'px">';
        popover += '<div class="ner-popover-close" title="Fenster schlie&szlig;en">&times;</div>';
        popover += '<div class="ner-popover-header"><h4>' + title + '</h4></div>';
        popover += '<div class="ner-popover-body">';
        popover += '<dl class="dl-horizontal">';
        $.each($.parseJSON(data), function (i, object) {
            $.each(object, function (property, value) {
                popover += '<dt title="' + property + '">' + property + ':</dt>';
                var objValue = '';
                $.each(value, function (p, v) {
                    var icon = '';

                    switch (property) {
                        case 'Beruf':
                            icon = 'glyphicon-briefcase';
                            break;
                        case 'Verwandte Begriffe':
                            icon = 'glyphicon-briefcase';
                            break;
                        case 'Sohn':
                            icon = 'glyphicon-user';
                            break;
                        case 'Vater':
                            icon = 'glyphicon-user';
                            break;
                        case 'Geburtsort':
                            icon = 'glyphicon-map-marker';
                            break;
                        case 'Sterbeort':
                            icon = 'glyphicon-map-marker';
                            break;
                    }

                    if (v.url) {
                        objValue += '<span ';
                        objValue += 'class="ner-detail-trigger" ';
                        objValue += 'title="' + v.text + '" ';
                        objValue += 'tabindex="-1"';
                        objValue += 'data-remotecontent="' + path + '/api?action=normdata&url=' + v.url + '">';
                        objValue += '<span class="glyphicon ' + icon + '"></span>&nbsp;';
                        objValue += v.text;
                        objValue += '<span class="ner-detail-loader"></span>';
                        objValue += '</span>';
                    } else {
                        if (property === 'URI') {
                            objValue += '<a href="' + v.text + '" target="_blank">' + v.text + '</a>';
                        } else {
                            objValue += v.text;
                        }
                    }

                    objValue += '<br />';
                });
                popover += '<dd>' + objValue + '</dd>';
                popover += '<div class="ner-popover-detail"></div>';
            });
        });
        popover += '</dl>';
        popover += '</div>';
        popover += '</div>';

        return popover;
    }

    /**
     * Method which renders detail information into the popover.
     * 
     * @method _renderNerPopoverDetail
     * @param {Object} data The JSON object from the API.
     * @param {String} title The value of the title attribute from the clicked trigger.
     * @returns {String} The HTML string which renders the details.
     * 
     */
    function _renderNerPopoverDetail(data, title) {
        if (_debug) {
            console.log('viewer.nerFulltext _renderNerPopoverDetail: data - ');
            console.log(data);
            console.log('viewer.nerFulltext _renderNerPopoverDetail: title - ' + title);
        }

        var popoverDetail = '';

        popoverDetail += '<div class="ner-popover-detail">';
        popoverDetail += '<div class="ner-popover-detail-header"><h4>' + title + '</h4></div>';
        popoverDetail += '<div class="ner-popover-detail-body">';
        popoverDetail += '<dl class="dl-horizontal">';
        $.each($.parseJSON(data), function (i, object) {
            $.each(object, function (property, value) {
                popoverDetail += '<dt title="' + property + '">' + property + ':</dt>';
                var objValue = '';
                $.each(value, function (p, v) {
                    if (property === 'URI') {
                        objValue += '<a href="' + v.text + '" target="_blank">' + v.text + '</a>';
                    } else {
                        objValue += v.text;
                    }

                    objValue += '<br />';
                });
                popoverDetail += '<dd>' + objValue + '</dd>';
                popoverDetail += '<div class="ner-popover-detail"></div>';
            });
        });
        popoverDetail += '</dl>';
        popoverDetail += '</div>';
        popoverDetail += '</div>';

        return popoverDetail;
    }

    /**
     * Method which calculates the position of the popover in the DOM.
     * 
     * @method _calculateNerPopoverPosition
     * @param {Object} triggerCoords A jQuery object including the position of the clicked
     * trigger.
     * @param {Object} textBoxCoords A jQuery object including the position of the parent
     * DIV.
     * @returns {Object} An object which includes the position of the popover.
     * 
     */
    function _calculateNerPopoverPosition(triggerCoords, textBoxCoords) {
        if (_debug) {
            console.log('viewer.nerFulltext _calculateNerPopoverPosition: triggerCoords - ');
            console.log(triggerCoords);
            console.log('viewer.nerFulltext _calculateNerPopoverPosition: textBoxCoords - ');
            console.log(textBoxCoords);
        }

        var poLeftBorder = triggerCoords.left - (150 - triggerCoords.width / 2),
            poRightBorder = poLeftBorder + 300,
            tbLeftBorder = textBoxCoords.left,
            tbRightBorder = textBoxCoords.right,
            poTop,
            poLeft = poLeftBorder;

        poTop = triggerCoords.top + 27;

        if (poLeftBorder <= tbLeftBorder) {
            poLeft = tbLeftBorder;
        }

        if (poRightBorder >= tbRightBorder) {
            poLeft = textBoxCoords.right - 300;
        }

        return {
            top: poTop,
            left: poLeft
        };
    }

    /**
     * Method to remove a popover from the DOM.
     * 
     * @method _removeNerPopover
     * 
     */
    function _removeNerPopover() {
        if (_debug) {
            console.log('viewer.nerFulltext _removeNerPopover');
        }

        $('.ner-popover-close').on('click', function () {
            $('body').find('.ner-popover-pointer').hide();
            $(this).parent().remove();
        });
    }

    return viewer;
}(viewerJS || {}, jQuery);

},{}],15:[function(require,module,exports){
'use strict';

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
 * Module to render popovers including normdata.
 * 
 * @version 3.2.0
 * @module viewerJS.normdata
 * @requires jQuery
 * @requires Bootstrap
 * 
 */
var viewerJS = function (viewer) {
    'use strict';

    var _debug = false;
    var _data = null;
    var _dataURL = '';
    var _data = '';
    var _linkPos = null;
    var _popover = '';
    var _id = '';
    var _$this = null;
    var _normdataIcon = null;
    var _preloader = null;
    var _defaults = {
        id: 0,
        path: null,
        lang: {},
        elemWrapper: null
    };

    viewer.normdata = {
        /**
         * Method to initialize the timematrix slider and the events which builds the
         * matrix and popovers.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.id The starting ID of the popover.
         * @param {String} config.path The rootpath of the application.
         * @param {Object} config.lang An object of localized strings.
         * @param {Object} config.elemWrapper An jQuery object of the wrapper DIV.
         * @example
         * 
         * <pre>
         * var normdataConfig = {
         *     path: '#{request.contextPath}',
         *     lang: {
         *         popoverTitle: '#{msg.normdataPopverTitle}',
         *         popoverClose: '#{msg.normdataPopoverClose}'
         *     },
         *     elemWrapper: $( '#metadataElementWrapper' )
         * };
         * 
         * viewerJS.normdata.init( normdataConfig );
         * </pre>
         */
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.normdata.init');
                console.log('##############################');
                console.log('viewer.normdata.init: config = ', config);
            }

            $.extend(true, _defaults, config);

            // hide close icons
            $('.closeAllPopovers').hide();

            // first level click
            // console.log("Init Click on normdata");
            // console.log("normdatalink = ", $( '.normdataLink') )
            $('.normdataLink').on('click', function () {
                console.log("Click on normdata");

                _$this = $(this);

                _$this.off('focus');

                _renderPopoverAction(_$this, _defaults.id);
            });
        }
    };

    /**
     * Method which executes the click event action of the popover.
     * 
     * @method _renderPopoverAction
     * @param {Object} $Obj The jQuery object of the current clicked link.
     * @param {String} id The current id of the popover.
     */
    function _renderPopoverAction($Obj, id) {
        if (_debug) {
            console.log('---------- _renderPopoverAction() ----------');
            console.log('_renderPopoverAction: $Obj = ', $Obj);
            console.log('_renderPopoverAction: id = ', id);
        }

        _normdataIcon = $Obj.find('.fa-list-ul');
        _preloader = $Obj.find('.normdata-preloader');

        // set variables
        _dataURL = $Obj.attr('data-remotecontent');
        _data = _getRemoteData(_dataURL, _preloader, _normdataIcon);
        _linkPos = $Obj.offset();
        _popover = _buildPopover(_data, id);

        if (_debug) {
            console.log('_renderPopoverAction: _dataURL = ', _dataURL);
            console.log('_renderPopoverAction: _data = ', _data);
            console.log('_renderPopoverAction: _linkPos = ', _linkPos);
        }

        // append popover to body
        $('body').append(_popover);

        // set popover position
        _calculatePopoverPosition(id, _linkPos, $Obj);

        // show popover
        $(document).find('#normdataPopover-' + id).hide().fadeIn('fast', function () {
            // disable source button
            $Obj.attr('disabled', 'disabled').addClass('disabled');

            // hide tooltip
            $Obj.tooltip('hide');

            // set event for nth level popovers
            $('.normdataDetailLink').off('click').on('click', function () {
                _$this = $(this);
                _renderPopoverAction(_$this, _defaults.id);
            });
        }).draggable();

        // init close method
        _closeNormdataPopover($Obj);

        // increment id
        _defaults.id++;

        // init close all method
        _closeAllNormdataPopovers($Obj);
    }

    /**
     * Returns an HTML-String which renders the fetched data into a popover.
     * 
     * @method _buildPopover
     * @param {Object} data The JSON-Object which includes the data.
     * @param {String} id The incremented id of the popover.
     * @returns {String} The HTML-String with the fetched data.
     */
    function _buildPopover(data, id) {
        if (_debug) {
            console.log('---------- _buildPopover() ----------');
            console.log('_buildPopover: data = ', data);
            console.log('_buildPopover: id = ', id);
        }

        var html = '';

        html += '<div id="normdataPopover-' + id + '" class="normdata-popover">';
        html += '<div class="normdata-popover-title">';
        html += '<h4>' + _defaults.lang.popoverTitle + '</h4>';
        html += '<span class="normdata-popover-close glyphicon glyphicon-remove" title="' + _defaults.lang.popoverClose + '"></span>';
        html += '</div>';
        html += '<div class="normdata-popover-content">';
        html += '<dl class="dl-horizontal">';
        $.each(data, function (i, object) {
            $.each(object, function (property, value) {
                html += '<dt title="' + property + '">' + property + '</dt>';
                html += '<dd>';
                $.each(value, function (p, v) {
                    if (v.text) {
                        if (property === "URI") {
                            html += '<a href="' + v.text + '" target="_blank">';
                            html += v.text;
                            html += '</a>';
                        } else {
                            html += v.text;
                        }
                    }
                    if (v.identifier) {
                        html += '<a href="' + _defaults.path + '/search/-/' + v.identifier + '/1/">';
                        html += '<span class="glyphicon glyphicon-search"></span>';
                        html += '</a>';
                    }
                    if (v.url) {
                        html += '<button type="button" class="normdataDetailLink" data-remotecontent="';
                        html += _defaults.path;
                        html += '/api?action=normdata&amp;url=';
                        html += v.url;
                        html += '" title="' + _defaults.lang.showNormdata + '">';
                        html += '<i class="fa fa-list-ul" aria-hidden="true"></i>';
                        html += '<div class="normdata-preloader"></div>';
                        html += '</button>';
                    }
                    html += '<br />';
                });
                html += '</dd>';
            });
        });
        html += "</dl>";
        html += "</div>";
        html += "</div>";

        return html;
    }

    /**
     * Sets the position to the first level popovers.
     * 
     * @method _calculateFirstLevelPopoverPosition
     * @param {String} id The incremented id of the popover.
     * @param {Object} pos An Object with the current position oft the clicked link.
     * @param {Object} $Obj An jQuery-Object of the clicked link.
     * 
     */
    function _calculatePopoverPosition(id, pos, $Obj) {
        if (_debug) {
            console.log('---------- _calculatePopoverPosition() ----------');
            console.log('_calculatePopoverPosition: id = ', id);
            console.log('_calculatePopoverPosition: pos = ', pos);
            console.log('_calculatePopoverPosition: $Obj = ', $Obj);
        }

        var _bodyWidth = $('body').outerWidth();
        var _popoverWidth = $('#normdataPopover-' + id).outerWidth();
        var _popoverRight = pos.left + _popoverWidth;

        if (_debug) {
            console.log('_calculatePopoverPosition: _bodyWidth = ', _bodyWidth);
            console.log('_calculatePopoverPosition: _popoverWidth = ', _popoverWidth);
            console.log('_calculatePopoverPosition: _popoverLeft = ', pos.left);
            console.log('_calculatePopoverPosition: _popoverRight = ', _popoverRight);
        }

        if (_popoverRight > _bodyWidth) {
            var _diff = _popoverRight - _bodyWidth;

            if (_debug) {
                console.log('_calculatePopoverPosition: _diff = ', _diff);
            }

            $(document).find('#normdataPopover-' + id).css({
                top: pos.top + $Obj.outerHeight() + 5,
                left: pos.left - _diff
            });
        } else {
            $(document).find('#normdataPopover-' + id).css({
                top: pos.top + $Obj.outerHeight() + 5,
                left: pos.left
            });
        }
    }

    /**
     * Removes current popover from the DOM on click.
     * 
     * @method _closeNormdataPopover
     * 
     */
    function _closeNormdataPopover($Obj) {
        if (_debug) {
            console.log('---------- _closeNormdataPopover() ----------');
            console.log('_closeNormdataPopover: $Obj = ', $Obj);
        }

        $(document).find('.normdata-popover-close').on('click', function () {
            $(this).parent().parent().remove();
            $Obj.removeAttr('disabled').removeClass('disabled');

            if ($('.normdata-popover').length < 1) {
                $('.closeAllPopovers').hide();
            }
        });
    }

    /**
     * Removes all popovers from the DOM on click.
     * 
     * @method _closeAllNormdataPopovers
     * 
     */
    function _closeAllNormdataPopovers($Obj) {
        if (_debug) {
            console.log('---------- _closeAllNormdataPopovers() ----------');
            console.log('_closeAllNormdataPopovers: $Obj = ', $Obj);
        }

        var _close = $Obj.parent().find('i.closeAllPopovers');

        if ($('.normdata-popover').length > 0) {
            _close.show();
            _close.on('click', function () {
                // close all popovers
                $('.normdata-popover').each(function () {
                    $(this).remove();
                });

                // hide all close icons
                $('.closeAllPopovers').each(function () {
                    $(this).hide();
                });

                // set trigger to enable
                $('.normdataLink').removeAttr('disabled').removeClass('disabled');
            });
        } else {
            _close.hide();
        }
    }

    /**
     * Returns an JSON object from a API call.
     * 
     * @method _getRemoteData
     * @returns {Object} The JSON object with the API data.
     */
    function _getRemoteData(url, loader, icon) {
        if (_debug) {
            console.log('---------- _getRemoteData() ----------');
            console.log('_getRemoteData: url = ', url);
            console.log('_getRemoteData: loader = ', loader);
            console.log('_getRemoteData: icon = ', icon);
        }

        loader.show();
        icon.hide();

        var data = $.ajax({
            url: decodeURI(url),
            type: "POST",
            dataType: "JSON",
            async: false,
            success: function success() {
                loader.hide();
                icon.show();
            }
        }).responseText;

        return jQuery.parseJSON(data);
    }

    return viewer;
}(viewerJS || {}, jQuery);

},{}],16:[function(require,module,exports){
'use strict';

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
 * Module to scroll a page back to top animated.
 * 
 * @version 3.2.0
 * @module viewerJS.pageScroll
 * @requires jQuery
 */
var viewerJS = function (viewer) {
    'use strict';

    var _elem = null;
    var _text = null;

    viewer.pageScroll = {
        /**
         * Initializes the animated pagescroll.
         * 
         * @method init
         * @param {String} obj The selector of the jQuery object.
         * @param {String} anchor The name of the anchor to scroll to.
         */
        init: function init(obj, anchor) {
            _elem = $(obj);
            _text = anchor;

            // eventlistener
            $(window).on('scroll', function () {
                if (window.pageYOffset > 200) {
                    _elem.fadeIn();
                } else {
                    _elem.hide();
                }
            });

            _elem.on('click', function () {
                _scrollPage(_text);
            });
        }
    };

    /**
     * Method which scrolls the page animated.
     * 
     * @method _scrollPage
     * @param {String} anchor The name of the anchor to scroll to.
     */
    function _scrollPage(anchor) {
        $('html,body').animate({
            scrollTop: $(anchor).offset().top
        }, 1000);

        return false;
    }

    return viewer;
}(viewerJS || {}, jQuery);

},{}],17:[function(require,module,exports){
'use strict';

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
 * Module which generates a responsive image gallery in columns.
 * 
 * @version 3.2.0
 * @module viewerJS.responsiveColumnGallery
 * @requires jQuery
 * 
 */
var viewerJS = function (viewer) {
    'use strict';

    // default variables

    var _debug = false;
    var _defaults = {
        themePath: '',
        imagePath: '',
        imageDataFile: '',
        galleryObject: null,
        maxColumnCount: null,
        maxImagesPerColumn: null,
        fixedHeight: false,
        maxHeight: '',
        caption: true,
        overlayColor: '',
        lang: {},
        lightbox: {
            active: true,
            caption: true
        }
    };
    var _promise = null;
    var _imageData = null;
    var _parentImage = null;
    var _lightboxImage = null;
    var _imageLightbox = null;
    var _smallViewport = null;
    var _dataUrl = null;

    viewer.responsiveColumnGallery = {
        /**
         * Method which initializes the column gallery.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.themePath The path to the current activated viewer
         * theme.
         * @param {String} config.imagePath The path to the used images.
         * @param {String} config.imageDataFile The path to the JSON-File, which contains
         * the images data.
         * @param {Object} config.galleryObject The DIV where the gallery should be
         * rendered.
         * @param {Number} config.maxColumnCount Count count of the gallery, 4 column are
         * maximum.
         * @param {Number} config.maxImagesPerColumn Count of the images per column.
         * @param {Boolean} config.fixedHeight If true the images have a fixed height,
         * default is false.
         * @param {String} config.maxHeight Sets the given max height value for the
         * images.
         * @param {Boolean} config.caption If true the gallery images have a caption with
         * the title text, default is true.
         * @param {String} config.overlayColor Takes a HEX-value to set the color of the
         * image overlay.
         * @param {Object} config.lang An object of strings for multilanguage
         * functionality.
         * @param {Object} config.lightbox An Object to configure the image lightbox.
         * @param {Boolean} config.lightbox.active If true the lightbox functionality is
         * enabled, default is true.
         * @param {Boolean} config.lightbox.caption If true the lightbox has a caption
         * text, default is true.
         */
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.responsiveColumnGallery.init');
                console.log('##############################');
                console.log('viewer.responsiveColumnGallery.init: config = ', config);
            }

            $.extend(true, _defaults, config);

            // fetch image data and check the viewport
            _dataUrl = _defaults.themePath + _defaults.imageDataFile;

            _promise = viewer.helper.getRemoteData(_dataUrl);

            _promise.then(function (imageData) {
                _imageData = imageData;
                _smallViewport = viewer.responsiveColumnGallery.checkForSmallViewport();

                // render columns
                if (_defaults.maxColumnCount > 4) {
                    _defaults.galleryObject.append(viewer.helper.renderAlert('alert-danger', 'Die maximale Spaltenanzahl für die Galerie beträgt 4!', true));

                    return false;
                } else {
                    for (var i = 0; i < _defaults.maxColumnCount; i++) {
                        _defaults.galleryObject.append(viewer.responsiveColumnGallery.renderColumns(_defaults.maxColumnCount));
                    }
                }

                // render images
                while (_imageData.length) {
                    $.each($('.rcg-col'), function () {
                        $(this).append(viewer.responsiveColumnGallery.renderImages(_imageData.splice(0, _defaults.maxImagesPerColumn)));
                    });
                }

                // set fixed height if activated and viewport is > 375px
                if (_defaults.fixedHeight && !_smallViewport) {
                    $.each($('.rcg-image-body'), function () {
                        viewer.responsiveColumnGallery.fixedHeight($(this));
                    });
                }

                // prepare lightbox
                if (_defaults.lightbox.active) {
                    $('.lightbox-toggle').on('click', function (event) {
                        event.preventDefault();

                        _parentImage = $(this).parent().children('img');
                        _lightboxImage = viewer.responsiveColumnGallery.prepareLightbox(_parentImage);
                        _imageLightbox = viewer.responsiveColumnGallery.renderLightbox(_lightboxImage);

                        $('body').append(_imageLightbox);

                        $('.rcg-lightbox-body').hide();

                        $('.rcg-lightbox-overlay').fadeIn('slow');

                        // first load image, then center it and show it up
                        $('.rcg-lightbox-image img').load(function () {
                            viewer.responsiveColumnGallery.centerLightbox($('.rcg-lightbox-body'));
                            $('.rcg-lightbox-body').show();
                        });

                        // close lightbox via button
                        $('.rcg-lightbox-close').on('click', function () {
                            $('.rcg-lightbox-overlay').remove();
                        });

                        // close lightbox via esc
                        $(document).keypress(function (event) {
                            if (event.keyCode === 27) {
                                $('.rcg-lightbox-overlay').remove();
                            }
                        });

                        // close lightbox via click on picture
                        $('.rcg-lightbox-image img').on('click', function () {
                            $('.rcg-lightbox-overlay').remove();
                        });
                    });
                }
            }).then(null, function (error) {
                _defaults.galleryObject.append(viewer.helper.renderAlert('alert-danger', '<strong>Status: </strong>' + error.status + ' ' + error.statusText, false));
                console.error('ERROR: viewer.responsiveColumnGallery.init - ', error);
            });
        },
        /**
         * Method which renders the gallery columns.
         * 
         * @method renderColumns
         * @param {String} count The column count of the gallery.
         * @returns {String} A HTML-String which renders a column.
         */
        renderColumns: function renderColumns(count) {
            if (_debug) {
                console.log('---------- viewer.responsiveColumnGallery.renderColumns() ----------');
                console.log('viewer.responsiveColumnGallery.renderColumns: count = ', count);
            }
            var column = '';

            column += '<div class="rcg-col col-' + count + '"></div>';

            return column;
        },
        /**
         * Method which renders the gallery images.
         * 
         * @method renderImages
         * @param {Object} data An object of image data to render the images.
         * @returns {String} A HTML-String which renders the gallery images.
         */
        renderImages: function renderImages(data) {
            if (_debug) {
                console.log('---------- viewer.responsiveColumnGallery.renderImages() ----------');
                console.log('viewer.responsiveColumnGallery.renderImages: data = ', data);
            }
            var image = '';

            $.each(data, function (i, j) {
                $.each(j, function (m, n) {
                    image += '<div class="rcg-image-container">';
                    image += '<div class="rcg-image-body">';
                    image += '<a href="' + n.url + '">';
                    image += '<div class="rcg-image-overlay" style="background-color:' + _defaults.overlayColor + '"></div>';
                    image += '</a>';
                    image += '<div class="rcg-image-title">';
                    image += '<h4>' + n.title + '</h4>';
                    image += '</div>';
                    image += '<img src="' + _defaults.themePath + _defaults.imagePath + n.name + '" alt="' + n.alt + '" />';
                    if (_defaults.lightbox.active) {
                        image += '<div class="lightbox-toggle" title="' + _defaults.lang.showLightbox + '">';
                        image += '<span class="glyphicon glyphicon-fullscreen"></span>';
                        image += '</div>';
                    }
                    image += '</div>';
                    if (_defaults.caption) {
                        image += '<div class="rcg-image-footer">';
                        image += '<p>' + n.caption + '<a href="' + n.url + '" title="' + n.title + '">';
                        image += _defaults.lang.goToWork + ' <span class="glyphicon glyphicon glyphicon-picture"></span></a></p>';
                        image += '</div>';
                    }
                    image += '</div>';
                });
            });

            return image;
        },
        /**
         * Method which sets a fixed height to the gallery images.
         * 
         * @method fixedHeight
         * @param {Object} $obj An jQuery object of the element which height should be
         * fixed.
         */
        fixedHeight: function fixedHeight($obj) {
            if (_debug) {
                console.log('---------- viewer.responsiveColumnGallery.fixedHeight() ----------');
                console.log('viewer.responsiveColumnGallery.fixedHeight: $obj = ', $obj);
            }

            $obj.children('img').css({
                'height': _defaults.maxHeight
            });
        },
        /**
         * Method which checks the viewport width and returns true if it´s smaller then
         * 375px.
         * 
         * @method checkForSmallViewport
         * @returns {Boolean} Returns true if it´s smaller then 375px.
         */
        checkForSmallViewport: function checkForSmallViewport() {
            if (_debug) {
                console.log('---------- viewer.responsiveColumnGallery.checkForSmallViewport() ----------');
            }
            var windowWidth = $(window).outerWidth();

            if (windowWidth <= 375) {
                return true;
            } else {
                return false;
            }
        },
        /**
         * Method which prepares the lightbox object with the required data.
         * 
         * @method prepareLightbox
         * @param {Object} $obj An jQuery object which includes the required data
         * attributes.
         * @returns {Object} An Object which includes the required data.
         */
        prepareLightbox: function prepareLightbox($obj) {
            if (_debug) {
                console.log('---------- viewer.responsiveColumnGallery.prepareLightbox() ----------');
                console.log('viewer.responsiveColumnGallery.prepareLightbox: $obj = ', $obj);
            }
            var lightboxData = {};

            lightboxData.src = $obj.attr('src');
            lightboxData.caption = $obj.attr('alt');

            return lightboxData;
        },
        /**
         * Method which renders a lightbox for the selected image.
         * 
         * @method renderLightbox
         * @param {Object} data An object which includes the required data.
         * @returns {String} A HTML-String which renders the lightbox.
         */
        renderLightbox: function renderLightbox(data) {
            if (_debug) {
                console.log('---------- viewer.responsiveColumnGallery.renderLightbox() ----------');
                console.log('viewer.responsiveColumnGallery.renderLightbox: data = ', data);
            }
            var lightbox = '';

            lightbox += '<div class="rcg-lightbox-overlay">';
            lightbox += '<div class="rcg-lightbox-body">';
            lightbox += '<div class="rcg-lightbox-close" title="' + _defaults.lang.close + '"><span class="glyphicon glyphicon-remove"></span></div>';
            lightbox += '<div class="rcg-lightbox-image">';
            lightbox += '<img src="' + data.src + '" alt="' + data.alt + '" />';
            lightbox += '</div>'; // .rcg-lightbox-image
            if (_defaults.lightbox.caption) {
                lightbox += '<div class="rcg-lightbox-caption">';
                lightbox += '<p>' + data.caption + '</p>';
                lightbox += '</div>'; // .rcg-lightbox-caption
            }
            lightbox += '</div>'; // .rcg-lightbox-body
            lightbox += '</div>'; // .rcg-lightbox-overlay

            return lightbox;
        },
        /**
         * Method which centers the given object to the viewport.
         * 
         * @method centerLightbox
         * @param {Object} $obj An jQuery object of the element to center.
         */
        centerLightbox: function centerLightbox($obj) {
            if (_debug) {
                console.log('---------- viewer.responsiveColumnGallery.centerLightbox() ----------');
                console.log('viewer.responsiveColumnGallery.centerLightbox: $obj = ', $obj);
            }

            var boxWidth = $obj.outerWidth();
            var boxHeight = $obj.outerHeight();

            $obj.css({
                'margin-top': '-' + boxHeight / 2 + 'px',
                'margin-left': '-' + boxWidth / 2 + 'px'
            });
        }
    };

    return viewer;
}(viewerJS || {}, jQuery);

},{}],18:[function(require,module,exports){
'use strict';

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
 * Module which sets up the functionality for search advanced.
 * 
 * @version 3.2.0
 * @module viewerJS.searchAdvanced
 * @requires jQuery
 */
var viewerJS = function (viewer) {
    'use strict';

    var _debug = false;
    var _advSearchValues = {};
    var _defaults = {
        loaderSelector: '.search-advanced__loader',
        inputSelector: '.value-text',
        resetSelector: '.reset'
    };

    viewer.searchAdvanced = {
        /**
         * Method to initialize the search advanced features.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         */
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.searchAdvanced.init');
                console.log('##############################');
                console.log('viewer.searchAdvanced.init: config = ', config);
            }

            $.extend(true, _defaults, config);

            // init bs tooltips
            $('[data-toggle="tooltip"]').tooltip();

            if (viewer.localStoragePossible) {
                localStorage.setItem('advSearchValues', JSON.stringify(_advSearchValues));

                // set search values
                _setAdvSearchValues();
                _resetValue();

                // ajax eventlistener
                jsf.ajax.addOnEvent(function (data) {
                    var ajaxstatus = data.status;

                    switch (ajaxstatus) {
                        case "begin":
                            // show loader
                            $(_defaults.loaderSelector).show();
                            break;
                        case "success":
                            // init bs tooltips
                            $('[data-toggle="tooltip"]').tooltip();

                            // set search values
                            _setAdvSearchValues();
                            _getAdvSearchValues();
                            _resetValue();

                            // set disabled state to select wrapper
                            $('select').each(function () {
                                if ($(this).attr('disabled') === 'disabled') {
                                    $(this).parent().addClass('disabled');
                                } else {
                                    $(this).parent().removeClass('disabled');
                                }
                            });

                            // hide loader
                            $(_defaults.loaderSelector).hide();
                            break;
                    }
                });
            } else {
                return false;
            }
        }
    };

    function _setAdvSearchValues() {
        if (_debug) {
            console.log('---------- _setAdvSearchValues() ----------');
        }

        $(_defaults.inputSelector).off().on('keyup', function () {
            var currId = $(this).attr('id');
            var currVal = $(this).val();
            var currValues = JSON.parse(localStorage.getItem('advSearchValues'));

            // check if values are in local storage
            if (!currValues.hasOwnProperty(currVal)) {
                currValues[currId] = currVal;
            } else {
                return false;
            }

            // write values to local storage
            localStorage.setItem('advSearchValues', JSON.stringify(currValues));
        });
    }

    function _getAdvSearchValues() {
        if (_debug) {
            console.log('---------- _getAdvSearchValues() ----------');
        }

        var values = JSON.parse(localStorage.getItem('advSearchValues'));

        $.each(values, function (id, value) {
            $('#' + id).val(value);
        });
    }

    function _resetValue() {
        if (_debug) {
            console.log('---------- _resetValue() ----------');
        }

        $(_defaults.resetSelector).off().on('click', function () {
            var inputId = $(this).parents('.input-group').find('input').attr('id');
            var currValues = JSON.parse(localStorage.getItem('advSearchValues'));

            // delete value from local storage object
            if (currValues.hasOwnProperty(inputId)) {
                delete currValues[inputId];
            }

            // write new values to local storage
            localStorage.setItem('advSearchValues', JSON.stringify(currValues));

            $(this).parents('.input-group').find('input').val('');
        });
    }

    return viewer;
}(viewerJS || {}, jQuery);

},{}],19:[function(require,module,exports){
'use strict';

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
var viewerJS = function (viewer) {
    'use strict';

    var _debug = false;
    var _promise = null;
    var _childHits = null;
    var _defaults = {
        contextPath: '',
        restApiPath: '/rest/search/hit/',
        hitsPerCall: 20,
        resetSearchSelector: '#resetCurrentSearch',
        searchInputSelector: '#currentSearchInput',
        searchTriggerSelector: '#slCurrentSearchTrigger',
        saveSearchModalSelector: '#saveSearchModal',
        saveSearchInputSelector: '#saveSearchInput',
        excelExportSelector: '.excel-export-trigger',
        excelExportLoaderSelector: '.excel-export-loader',
        hitContentLoaderSelector: '.search-list__loader',
        hitContentSelector: '.search-list__hit-content',
        msg: {
            getMoreChildren: 'Mehr Treffer laden'
        }
    };

    viewer.searchList = {
        /**
         * Method to initialize the search list features.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         */
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.searchList.init');
                console.log('##############################');
                console.log('viewer.searchList.init: config = ', config);
            }

            $.extend(true, _defaults, config);

            // init bs tooltips
            $('[data-toggle="tooltip"]').tooltip();

            // focus save search modal input on show
            $(_defaults.saveSearchModalSelector).on('shown.bs.modal', function () {
                $(_defaults.saveSearchInputSelector).focus();
            });

            // reset current search and redirect to standard search
            $(_defaults.resetSearchSelector).on('click', function () {
                $(_defaults.searchInputSelector).val('');
                location.href = _defaults.contextPath + '/search/';
            });

            // show/hide loader for excel export
            $(_defaults.excelExportSelector).on('click', function () {
                var trigger = $(this);
                var excelLoader = $(_defaults.excelExportLoaderSelector);

                trigger.hide();
                excelLoader.show();

                var url = _defaults.contextPath + '/rest/download/search/waitFor/';
                var promise = Q($.ajax({
                    url: decodeURI(url),
                    type: "GET",
                    dataType: "text",
                    async: true
                }));

                promise.then(function (data) {
                    if (_debug) {
                        console.log("Download started");
                    }

                    excelLoader.hide();
                    trigger.show();
                }).catch(function (error) {
                    if (_debug) {
                        console.log("Error downloading excel sheet: ", error.responseText);
                    }

                    excelLoader.hide();
                    trigger.show();
                });
            });

            // get child hits

            $('[data-toggle="hit-content"]').each(function () {
                var currBtn = $(this);
                var currIdDoc = $(this).attr('data-iddoc');
                var currUrl = _getApiUrl(currIdDoc, _defaults.hitsPerCall);

                if (_debug) {
                    console.log('Current API Call URL: ', currUrl);
                }

                _promise = viewer.helper.getRemoteData(currUrl);

                currBtn.find(_defaults.hitContentLoaderSelector).css('display', 'inline-block');

                // get data and render hits if data is valid
                _promise.then(function (data) {
                    if (data.hitsDisplayed < _defaults.hitsPerCall) {
                        // render child hits into the DOM
                        _renderChildHits(data, currBtn);
                        // set current button active, remove loader and show content
                        currBtn.toggleClass('in').find(_defaults.hitContentLoaderSelector).hide();
                        currBtn.next().show();
                        // set event to toggle current hits
                        currBtn.off().on('click', function () {
                            $(this).toggleClass('in').next().slideToggle();
                        });
                    } else {
                        // remove loader
                        currBtn.find(_defaults.hitContentLoaderSelector).hide();
                        // set event to toggle current hits
                        currBtn.off().on('click', function () {
                            // render child hits into the DOM
                            _renderChildHits(data, currBtn);
                            // check if more children exist and render link
                            _renderGetMoreChildren(data, currIdDoc, currBtn);
                            $(this).toggleClass('in').next().slideToggle();
                        });
                    }
                }).then(null, function () {
                    currBtn.next().append(viewer.helper.renderAlert('alert-danger', '<strong>Status: </strong>' + error.status + ' ' + error.statusText, false));
                    console.error('ERROR: viewer.searchList.init - ', error);
                });
            });
        }
    };

    /**
     * Method to get the full REST-API URL.
     * 
     * @method _getApiUrl
     * @param {String} id The current IDDoc of the hit set.
     * @returns {String} The full REST-API URL.
     */
    function _getApiUrl(id, hits) {
        if (_debug) {
            console.log('---------- _getApiUrl() ----------');
            console.log('_getApiUrl: id = ', id);
        }

        return _defaults.contextPath + _defaults.restApiPath + id + '/' + hits + '/';
    }

    /**
     * Method which renders the child hits into the DOM.
     * 
     * @method _renderChildHits
     * @param {Object} data The data object which contains the child hits.
     * @param {Object} $this The current child hits trigger.
     * @returns {Object} An jquery object which contains the child hits.
     */
    function _renderChildHits(data, $this) {
        if (_debug) {
            console.log('---------- _renderChildHits() ----------');
            console.log('_renderChildHits: data = ', data);
            console.log('_renderChildHits: $this = ', $this);
        }

        var hitSet = null;

        // clean hit sets
        $this.next().empty();

        // build hits
        $.each(data.children, function (children, child) {
            hitSet = $('<div class="search-list__hit-content-set" />');

            // build title
            hitSet.append(_renderHitSetTitle(child.browseElement));

            // append metadata if exist
            hitSet.append(_renderMetdataInfo(child.foundMetadata, child.url));

            // build child hits
            if (child.hasChildren) {
                $.each(child.children, function (subChildren, subChild) {
                    hitSet.append(_renderSubChildHits(subChild.browseElement, subChild.type));
                });
            }

            // append complete set
            $this.next().append(hitSet);
        });
    }

    /**
     * Method which renders the hit set title.
     * 
     * @method _renderHitSetTitle
     * @param {Object} data The data object which contains the hit set title values.
     * @returns {Object} A jquery object which contains the hit set title.
     */
    function _renderHitSetTitle(data) {
        if (_debug) {
            console.log('---------- _renderHitSetTitle() ----------');
            console.log('_renderHitSetTitle: data = ', data);
        }

        var hitSetTitle = null;
        var hitSetTitleH5 = null;
        var hitSetTitleDl = null;
        var hitSetTitleDt = null;
        var hitSetTitleDd = null;
        var hitSetTitleLink = null;

        hitSetTitle = $('<div class="search-list__struct-title" />');
        hitSetTitleH5 = $('<h5 />');
        hitSetTitleLink = $('<a />');
        hitSetTitleLink.attr('href', _defaults.contextPath + '/' + data.url);
        hitSetTitleLink.append(data.labelShort);
        hitSetTitleH5.append(hitSetTitleLink);
        hitSetTitle.append(hitSetTitleH5);

        return hitSetTitle;
    }

    /**
     * Method which renders metadata info.
     * 
     * @method _renderMetdataInfo
     * @param {Object} data The data object which contains the sub hit values.
     * @param {String} url The URL for the current work.
     * @returns {Object} A jquery object which contains the metadata info.
     */
    function _renderMetdataInfo(data, url) {
        if (_debug) {
            console.log('---------- _renderMetdataInfo() ----------');
            console.log('_renderMetdataInfo: data = ', data);
            console.log('_renderMetdataInfo: url = ', url);
        }

        var metadataWrapper = null;
        var metadataTable = null;
        var metadataTableBody = null;
        var metadataTableRow = null;
        var metadataTableCellLeft = null;
        var metadataTableCellRight = null;
        var metadataKeyIcon = null;
        var metadataKeyLink = null;
        var metadataValueLink = null;

        if (!$.isEmptyObject(data)) {
            metadataWrapper = $('<div class="search-list__metadata-info" />');
            metadataTable = $('<table />');
            metadataTableBody = $('<tbody />');

            data.forEach(function (metadata) {
                // left cell
                metadataTableCellLeft = $('<td />');
                metadataKeyIcon = $('<i />').attr('aria-hidden', 'true').addClass('fa fa-bookmark-o');
                metadataKeyLink = $('<a />').attr('href', _defaults.contextPath + '/' + url).html(metadata.one + ':');
                metadataTableCellLeft.append(metadataKeyIcon).append(metadataKeyLink);

                // right cell
                metadataTableCellRight = $('<td />');
                metadataValueLink = $('<a />').attr('href', _defaults.contextPath + '/' + url).html(metadata.two);
                metadataTableCellRight.append(metadataValueLink);

                // row
                metadataTableRow = $('<tr />');
                metadataTableRow.append(metadataTableCellLeft).append(metadataTableCellRight);

                // body
                metadataTableBody.append(metadataTableRow);
            });

            metadataTable.append(metadataTableBody);
            metadataWrapper.append(metadataTable);

            return metadataWrapper;
        }
    }

    /**
     * Method which renders sub child hits.
     * 
     * @method _renderSubChildHits
     * @param {Object} data The data object which contains the sub hit values.
     * @param {String} type The type of hit to render.
     * @returns {Object} A jquery object which contains the sub child hits.
     */
    function _renderSubChildHits(data, type) {
        if (_debug) {
            console.log('---------- _renderSubChildHits() ----------');
            console.log('_renderSubChildHits: data = ', data);
            console.log('_renderSubChildHits: type = ', type);
        }

        var hitSetChildren = null;
        var hitSetChildrenDl = null;
        var hitSetChildrenDt = null;
        var hitSetChildrenDd = null;
        var hitSetChildrenLink = null;

        hitSetChildren = $('<div class="search-list__struct-child-hits" />');
        hitSetChildrenDl = $('<dl class="dl-horizontal" />');
        hitSetChildrenDt = $('<dt />');
        // check hit type
        switch (type) {
            case 'PAGE':
                hitSetChildrenDt.append('<i class="fa fa-file-text" aria-hidden="true"></i>');
                break;
            case 'PERSON':
                hitSetChildrenDt.append('<i class="fa fa-user" aria-hidden="true"></i>');
                break;
            case 'CORPORATION':
                hitSetChildrenDt.append('<i class="fa fa-university" aria-hidden="true"></i>');
                break;
            case 'LOCATION':
                hitSetChildrenDt.append('<i class="fa fa-location-arrow" aria-hidden="true"></i>');
                break;
            case 'SUBJECT':
                hitSetChildrenDt.append('<i class="fa fa-question-circle-o" aria-hidden="true"></i>');
                break;
            case 'PUBLISHER':
                hitSetChildrenDt.append('<i class="fa fa-copyright" aria-hidden="true"></i>');
                break;
            case 'EVENT':
                hitSetChildrenDt.append('<i class="fa fa-calendar" aria-hidden="true"></i>');
                break;
            case 'ACCESSDENIED':
                hitSetChildrenDt.append('<i class="fa fa-lock" aria-hidden="true"></i>');
                break;
        }
        hitSetChildrenDd = $('<dd />');
        hitSetChildrenLink = $('<a />').attr('href', _defaults.contextPath + '/' + data.url);
        switch (type) {
            case 'PAGE':
            case 'ACCESSDENIED':
                hitSetChildrenLink.append(data.fulltextForHtml);
                break;
            default:
                hitSetChildrenLink.append(data.labelShort);
                break;
        }
        hitSetChildrenDd.append(hitSetChildrenLink);
        hitSetChildrenDl.append(hitSetChildrenDt).append(hitSetChildrenDd);
        hitSetChildren.append(hitSetChildrenDl);

        return hitSetChildren;
    }

    /**
     * Method to render a get more children link.
     * 
     * @method _renderGetMoreChildren
     */
    function _renderGetMoreChildren(data, iddoc, $this) {
        if (_debug) {
            console.log('---------- _renderGetMoreChildren() ----------');
            console.log('_renderGetMoreChildren: data = ', data);
            console.log('_renderGetMoreChildren: iddoc = ', iddoc);
            console.log('_renderGetMoreChildren: $this = ', $this);
        }

        var apiUrl = _getApiUrl(iddoc, _defaults.hitsPerCall + data.hitsDisplayed);
        var hitContentMore = $('<div />');
        var getMoreChildrenLink = $('<button type="button" />');

        if (data.hasMoreChildren) {
            // build get more link
            hitContentMore.addClass('search-list__hit-content-more');
            getMoreChildrenLink.addClass('btn-clean');
            getMoreChildrenLink.attr('data-api', apiUrl);
            getMoreChildrenLink.attr('data-iddoc', iddoc);
            getMoreChildrenLink.append(_defaults.msg.getMoreChildren);
            hitContentMore.append(getMoreChildrenLink);
            // append links
            $this.next().append(hitContentMore);
            // render new hit set
            getMoreChildrenLink.off().on('click', function (event) {
                var currApiUrl = $(this).attr('data-api');
                var parentOffset = $this.parent().offset().top;

                // get data and render hits if data is valid
                _promise = viewer.helper.getRemoteData(currApiUrl);
                _promise.then(function (data) {
                    // render child hits into the DOM
                    _renderChildHits(data, $this);
                    // check if more children exist and render link
                    _renderGetMoreChildren(data, iddoc, $this);
                }).then(null, function () {
                    $this.next().append(viewer.helper.renderAlert('alert-danger', '<strong>Status: </strong>' + error.status + ' ' + error.statusText, false));
                    console.error('ERROR: _renderGetMoreChildren - ', error);
                });
            });
        } else {
            // clear and hide current get more link
            $this.next().find(_defaults.hitContentMoreSelector).empty().hide();
            console.info('_renderGetMoreChildren: No more child hits available');
            return false;
        }
    }

    return viewer;
}(viewerJS || {}, jQuery);

},{}],20:[function(require,module,exports){
'use strict';

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
 * Module which sets up the sorting functionality for search list.
 * 
 * @version 3.2.0
 * @module viewerJS.searchSortingDropdown
 * @requires jQuery
 */
var viewerJS = function (viewer) {
    'use strict';

    var _debug = false;
    var _selectedSorting = '';
    var _dataSortFieldState = '';
    var _currUrl = '';
    var _valueUrl = '';
    var _checkValUrl = '';
    var _dataSortField = '';
    var _defaults = {
        select: '#sortSelect'
    };

    viewer.searchSortingDropdown = {
        /**
         * Method to initialize the search sorting widget.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {Object} config.select An jQuery object which holds the sorting menu.
         */
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.searchSortingDropdown.init');
                console.log('##############################');
                console.log('viewer.searchSortingDropdown.init: config = ', config);
            }

            $.extend(true, _defaults, config);

            if (viewer.localStoragePossible) {
                _selectedSorting = localStorage.dataSortField;
                _currUrl = location.href;

                // get selected sorting type from local storage an set the menu option to
                // selected
                if (_selectedSorting !== '' || _selectedSorting !== 'undefinded') {
                    $(_defaults.select).children('option').each(function () {
                        _dataSortFieldState = $(this).attr('data-sortField');
                        _checkValUrl = $(this).val();

                        if (_dataSortFieldState === _selectedSorting && _checkValUrl === _currUrl) {
                            $(this).attr('selected', 'selected');
                        }
                    });
                }

                // get the sorting URL from the option value and reload the page on change
                $(_defaults.select).on('change', function () {
                    _valueUrl = $(this).val();
                    _dataSortField = $(this).children('option:selected').attr('data-sortField');

                    if (_valueUrl !== '') {
                        // save current sorting state to local storage
                        if (typeof Storage !== "undefined") {
                            localStorage.dataSortField = _dataSortField;
                        } else {
                            console.info('Local Storage is not defined. Current sorting state could not be saved.');

                            return false;
                        }

                        window.location.href = _valueUrl;
                    }
                });
            } else {
                return false;
            }
        }
    };

    return viewer;
}(viewerJS || {}, jQuery);

},{}],21:[function(require,module,exports){
'use strict';

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
 * Module to render a simple lightbox for images.
 * 
 * @version 3.2.0
 * @module viewerJS.simpleLightbox
 * @requires jQuery
 * @example <img src="/your/path/to/the/image.jpg" class="lightbox-image"
 * data-imgpath="/your/path/to/the/" data-imgname="image.jpg" alt="" /> *
 */
var viewerJS = function (viewer) {
    'use strict';

    var _box = null;
    var _imgpath = null;
    var _imgname = null;

    viewer.simpleLightBox = {
        /**
         * Initializes an event (click) which renders a lightbox with an bigger image.
         * 
         * @method init
         * @example
         * 
         * <pre>
         * viewerJS.simpleLightBox.init();
         * </pre>
         * 
         */
        init: function init() {
            // eventlisteners
            $('.lightbox-image').on('click', function (event) {
                event.preventDefault();

                var $this = $(this);

                _imgpath = _getImagePath($this);
                _imgname = _getImageName($this);
                _box = _setupLightBox(_imgpath, _imgname);

                $('body').append(_box);

                _centerModalBox($('.lightbox-modal-box'));

                $('.lightbox-overlay').fadeIn();

                $('.lightbox-close-btn').on('click', function () {
                    $('.lightbox-overlay').remove();
                });
            });
        }
    };

    /**
     * Returns the image path from the 'data-imgpath' attribute.
     * 
     * @method _getImagePath
     * @param {Object} $Obj Must be a jQuery-Object like $('.something')
     * @returns {String} The image path from the 'data-imgpath' attribute.
     * 
     */
    function _getImagePath($Obj) {
        _imgpath = $Obj.attr('data-imgpath');

        return _imgpath;
    }

    /**
     * Returns the image name from the 'data-imgname' attribute.
     * 
     * @method _getImageName
     * @param {Object} $Obj Must be a jQuery-Object like $('.something')
     * @returns {String} The image name from the 'data-imgname' attribute.
     * 
     */
    function _getImageName($Obj) {
        _imgname = $Obj.attr('data-imgname');

        return _imgname;
    }

    /**
     * Returns a HTML-String which renders the lightbox.
     * 
     * @method _setupLightBox
     * @param {String} path The path to the big image.
     * @param {String} name The name of the big image.
     * @returns {String} The HTML-Code to render the lightbox.
     * 
     */
    function _setupLightBox(path, name) {
        var lightbox = '';

        lightbox = '<div class="lightbox-overlay">';
        lightbox += '<div class="lightbox-modal-box">';
        lightbox += '<div class="lightbox-close">';
        lightbox += '<span class="lightbox-close-btn" title="Fenster schlie&szlig;en">&times;</span>';
        lightbox += '</div>';
        lightbox += '<img src="' + path + name + '" alt="' + name + '" /></div></div>';

        return lightbox;
    }

    /**
     * Puts the lightbox to the center of the screen.
     * 
     * @method _centerModalBox
     * @param {Object} $Obj Must be a jQuery-Object like $('.something')
     */
    function _centerModalBox($Obj) {
        var boxWidth = $Obj.outerWidth();
        var boxHeight = $Obj.outerHeight();

        $Obj.css({
            'margin-top': '-' + boxHeight / 2 + 'px',
            'margin-left': '-' + boxWidth / 2 + 'px'
        });
    }

    return viewer;
}(viewerJS || {}, jQuery);

},{}],22:[function(require,module,exports){
'use strict';

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
 * Module which renders CSS featured stacked thumbnails in search list for multivolume
 * works.
 * 
 * @version 3.2.0
 * @module viewerJS.stackedThumbnails
 * @requires jQuery
 */
var viewerJS = function (viewer) {
    'use strict';

    var _debug = false;
    var _imgWidth = null;
    var _imgHeight = null;
    var _defaults = {
        thumbs: '.stacked-thumbnail',
        thumbsBefore: '.stacked-thumbnail-before',
        thumbsAfter: '.stacked-thumbnail-after'
    };

    viewer.stackedThumbnails = {
        /**
         * Method to initialize the timematrix slider and the events which builds the
         * matrix and popovers.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {Object} config.thumbs All jQuery objects of the stacked thumbnails.
         * @param {String} config.thumbsBefore The classname of the stacked thumbnail
         * before element.
         * @param {String} config.thumbsAfter The classname of the stacked thumbnail after
         * element.
         */
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.stackedThumbnails.init');
                console.log('##############################');
                console.log('viewer.stackedThumbnails.init: config - ');
                console.log(config);
            }

            $.extend(true, _defaults, config);

            // hide stacked thumbs
            $(_defaults.thumbs).hide();
            $(_defaults.thumbs).siblings().hide();

            // iterate through thumbnails and set width and height for image stack
            $(_defaults.thumbs).each(function () {
                _imgWidth = $(this).outerWidth();
                _imgHeight = $(this).outerHeight();

                $(this).css({
                    'margin-left': '-' + _imgWidth / 2 + 'px'
                });

                $(this).siblings().css({
                    'width': _imgWidth,
                    'height': _imgHeight,
                    'margin-left': '-' + _imgWidth / 2 + 'px'
                });

                // show stacked thumbs after building them
                $(this).show();
                $(this).siblings(_defaults.thumbsBefore).fadeIn('slow', function () {
                    $(this).siblings(_defaults.thumbsAfter).fadeIn();
                });
            });
        }
    };

    return viewer;
}(viewerJS || {}, jQuery);

},{}],23:[function(require,module,exports){
'use strict';

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
 * Module to create an imagemap sorted by the year of creation.
 * 
 * @version 3.2.0
 * @module viewerJS.timematrix
 * @requires jQuery
 */
var viewerJS = function (viewer) {
    'use strict';

    // default variables

    var _debug = false;
    var _promise = null;
    var _apiData = null;
    var _defaults = {
        contextPath: null,
        apiQuery: 'api?action=timeline',
        startDateQuery: '&startDate=',
        rangeInput1: null,
        startDate: null,
        endDateQuery: '&endDate=',
        rangeInput2: null,
        endDate: null,
        countQuery: '&count=',
        count: null,
        $tmCanvas: null,
        $tmCanvasPos: null,
        $tmCanvasCoords: {},
        lang: {}
    };

    viewer.timematrix = {
        /**
         * Method to initialize the timematrix slider and the events which builds the
         * matrix and popovers.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.contextPath The rootpath of the application.
         * @param {String} config.apiQuery The API action to call.
         * @param {String} config.startDateQuery The GET-Parameter for the start date.
         * @param {Object} config.rangeInput1 An jQuery object of the first range input.
         * @param {String} config.startDate The value of the first range input.
         * @param {String} config.endDateQuery The GET-Parameter for the end date.
         * @param {Object} config.rangeInput2 An jQuery object of the second range input.
         * @param {String} config.endDate The value of the second range input.
         * @param {String} config.countQuery The GET-Parameter for the count query.
         * @param {String} config.count The number of results from the query.
         * @param {Object} config.$tmCanvas An jQuery object of the timematrix canvas.
         * @param {Object} config.$tmCanvasPos An jQuery object of the timematrix canvas
         * position.
         * @param {Object} config.lang An object of localized strings.
         * @example
         * 
         * <pre>
         * $( document ).ready( function() {
         *     var timematrixConfig = {
         *         path: '#{request.contextPath}/',
         *         lang: {
         *             closeWindow: '#{msg.timematrixCloseWindow}',
         *             goToWork: '#{msg.timematrixGoToWork}'
         *         }
         *     };
         *     viewerJS.timematrix.init( timematrixConfig );
         * } );
         * </pre>
         */
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.timematrix.init');
                console.log('##############################');
                console.log('viewer.timematrix.init: config - ');
                console.log(config);
            }

            $.extend(true, _defaults, config);

            _defaults.$tmCanvasCoords = {
                top: _defaults.$tmCanvasPos.top,
                left: 0,
                right: _defaults.$tmCanvasPos.left + _defaults.$tmCanvas.outerWidth()
            };

            // range slider settings
            $('#slider-range').slider({
                range: true,
                min: parseInt(_defaults.startDate),
                max: parseInt(_defaults.endDate),
                values: [parseInt(_defaults.startDate), parseInt(_defaults.endDate)],
                slide: function slide(event, ui) {
                    _defaults.rangeInput1.val(ui.values[0]);
                    $('.timematrix-slider-bubble-startDate').html(ui.values[0]);
                    _defaults.startDate = ui.values[0];
                    _defaults.rangeInput1.val(ui.values[1]);
                    $('.timematrix-slider-bubble-endDate').html(ui.values[1]);
                    _defaults.endDate = ui.values[1];
                }
            });

            // append slider bubble to slider handle
            $('#slider-range .ui-slider-range').next().append(_renderSliderBubble('startDate', _defaults.startDate));
            $('#slider-range .ui-slider-range').next().next().append(_renderSliderBubble('endDate', _defaults.endDate));

            // set active slider handle to top
            $('.ui-slider-handle').on('mousedown', function () {
                $('.ui-slider-handle').removeClass('top');
                $(this).addClass('top');
            });

            // listen to submit event of locate timematrix form
            $('#locateTimematrix').on('submit', function (e) {
                e.preventDefault();

                // check for popovers and remove them
                if ($('.timematrix-popover').length) {
                    $('.timematrix-popover').remove();
                }

                // build api target
                var apiTarget = _defaults.contextPath;
                apiTarget += _defaults.apiQuery;
                apiTarget += _defaults.startDateQuery;
                apiTarget += _defaults.startDate;
                apiTarget += _defaults.endDateQuery;
                apiTarget += _defaults.endDate;
                apiTarget += _defaults.countQuery;
                apiTarget += _defaults.count;

                // get data from api
                _promise = viewer.helper.getRemoteData(apiTarget);

                // render thumbnails
                _promise.then(function (data) {
                    _apiData = data;

                    _defaults.$tmCanvas.html(_renderThumbs(_apiData));
                    $('.timematrix-thumb').css({
                        height: $('.timematrix-thumb').outerWidth()
                    });

                    // show thumbs after they´ve been loaded
                    $('.timematrix-thumb img').load(function () {
                        $(this).css({
                            visibility: 'visible'
                        });
                    });

                    // listen to click event on thumbnails
                    $('.timematrix-thumb').on('click', function () {
                        if (!$('.timematrix-popover')) {
                            $('.timematrix-thumb').removeClass('marker');
                            $(this).addClass('marker');
                            _renderPopover($(this), _defaults.lang);
                        } else {
                            $('.timematrix-popover').remove();
                            $('.timematrix-thumb').removeClass('marker');
                            $(this).addClass('marker');
                            _renderPopover($(this), _defaults.lang);
                        }

                        // close popover
                        $('.timematrix-popover-close').on('click', function () {
                            $(this).parent().remove();
                            $('.timematrix-thumb').removeClass('marker');
                        });

                        // check if image is loaded and reset loader
                        $('.timematrix-popover-body img').load(function () {
                            $('.timematrix-popover-imageloader').hide();
                        });
                    });
                }).then(null, function () {
                    _defaults.$tmCanvas.append(viewer.helper.renderAlert('alert-danger', '<strong>Status: </strong>' + error.status + ' ' + error.statusText, false));
                    console.error('ERROR: viewer.timematrix.init - ', error);
                });
            });

            // remove all popovers by clicking on body
            $('body').on('click', function (event) {
                if ($(event.target).closest('.timematrix-thumb').length) {
                    return;
                } else {
                    _removePopovers();
                }
            });
        }
    };

    /**
     * Method to render image thumbnails to the timematrix canvas.
     * 
     * @method _renderThumbs
     * @param {Object} json An JSON-Object which contains the image data.
     * @returns {String} HTML-String which displays the image thumbnails.
     */
    function _renderThumbs(json) {
        if (_debug) {
            console.log('viewer.timematrix _renderThumbs: json - ');
            console.log(json);
        }

        var tlbox = '';
        tlbox += '<div class="timematrix-box">';
        tlbox += '<header class="timematrix-header">';
        if (_defaults.startDate !== '' && _defaults.endDate !== '') {
            tlbox += '<h3>' + _defaults.startDate + ' - ' + _defaults.endDate + '</h3>';
        }
        tlbox += '</header>';
        tlbox += '<section class="timematrix-body">';
        $.each(json, function (i, j) {
            tlbox += '<div class="timematrix-thumb" data-title="' + j.title + '" data-mediumimage="' + j.mediumimage + '" data-url="' + j.url + '">';
            if (j.thumbnailUrl) {
                tlbox += '<img src="' + j.thumbnailUrl + '" style="visibility: hidden;" />';
            } else {
                tlbox += '';
            }
            tlbox += '</div>';
        });
        tlbox += '</section>';
        tlbox += '<footer class="timematrix-footer"></footer>';
        tlbox += '</div>';

        return tlbox;
    }

    /**
     * Method to render a popover with a thumbnail image.
     * 
     * @method _renderPopover
     * @param {Object} $Obj Must be an jQuery-Object like $(this).
     * @param {Object} lang An Object with localized strings in the selected language.
     */
    function _renderPopover($Obj, lang) {
        if (_debug) {
            console.log('viewer.timematrix _renderPopover: obj - ');
            console.log($Obj);
            console.log('viewer.timematrix _renderPopover: lang - ' + lang);
        }

        var title = $Obj.attr('data-title');
        var mediumimage = $Obj.attr('data-mediumimage');
        var url = $Obj.attr('data-url');
        var $objPos = $Obj.position();
        var $objCoords = {
            top: $objPos.top,
            left: $objPos.left,
            width: $Obj.outerWidth()
        };
        var popoverPos = _calculatePopoverPosition($objCoords, _defaults.$tmCanvasCoords);
        var popover = '';
        popover += '<div class="timematrix-popover" style="top: ' + popoverPos.top + 'px; left: ' + popoverPos.left + 'px;">';
        popover += '<span class="timematrix-popover-close" title="' + lang.closeWindow + '">&times;</span>';
        popover += '<header class="timematrix-popover-header">';
        popover += '<h4 title="' + title + '">' + viewer.helper.truncateString(title, 75) + '</h4>';
        popover += '</header>';
        popover += '<section class="timematrix-popover-body">';
        popover += '<div class="timematrix-popover-imageloader"></div>';
        popover += '<a href="' + url + '"><img src="' + mediumimage + '" /></a>';
        popover += '</section>';
        popover += '<footer class="timematrix-popover-footer">';
        popover += '<a href="' + url + '" title="' + title + '">' + lang.goToWork + '</a>';
        popover += '</footer>';
        popover += '</div>';

        _defaults.$tmCanvas.append(popover);
    }

    /**
     * Method which calculates the position of the popover.
     * 
     * @method _calculatePopoverPosition
     * @param {Object} triggerCoords An object which contains the coordinates of the
     * element has been clicked.
     * @param {Object} tmCanvasCoords An object which contains the coordinates of the
     * wrapper element from the timematrix.
     * @returns {Object} An object which contains the top and the left position of the
     * popover.
     */
    function _calculatePopoverPosition(triggerCoords, tmCanvasCoords) {
        if (_debug) {
            console.log('viewer.timematrix _calculatePopoverPosition: triggerCoords - ');
            console.log(triggerCoords);
            console.log('viewer.timematrix _calculatePopoverPosition: tmCanvasCoords - ');
            console.log(tmCanvasCoords);
        }

        var poLeftBorder = triggerCoords.left - (150 - triggerCoords.width / 2);
        var poRightBorder = poLeftBorder + 300;
        var tbLeftBorder = tmCanvasCoords.left;
        var tbRightBorder = tmCanvasCoords.right;
        var poTop;
        var poLeft = poLeftBorder;

        poTop = triggerCoords.top + $('.timematrix-thumb').outerHeight() - 1;

        if (poLeftBorder <= tbLeftBorder) {
            poLeft = tbLeftBorder;
        }

        if (poRightBorder >= tbRightBorder) {
            poLeft = tmCanvasCoords.right - 300;
        }

        return {
            top: poTop,
            left: poLeft
        };
    }

    /**
     * Method which renders the bubbles for the slider.
     * 
     * @method _renderSliderBubble
     * @param {String} time The string for the time value.
     * @param {String} val The string for the value.
     * @returns {String} HTML-String which renders the slider-bubble.
     */
    function _renderSliderBubble(time, val) {
        if (_debug) {
            console.log('viewer.timematrix _renderSliderBubble: time - ' + time);
            console.log('viewer.timematrix _renderSliderBubble: val - ' + val);
        }

        return '<div class="timematrix-slider-bubble-' + time + '">' + val + '</div>';
    }

    /**
     * Method which removes all popovers.
     * 
     * @method _removePopovers
     */
    function _removePopovers() {
        if (_debug) {
            console.log('---------- _removePopovers() ----------');
        }

        $('.timematrix-popover').remove();
        $('.timematrix-thumb').removeClass('marker');
    }

    return viewer;
}(viewerJS || {}, jQuery);

},{}],24:[function(require,module,exports){
'use strict';

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
 * @module viewerJS.tinyMce
 * @requires jQuery
 */
var viewerJS = function (viewer) {
    'use strict';

    var _debug = false;
    var _defaults = {
        currLang: 'de',
        selector: 'textarea.tinyMCE',
        width: '100%',
        height: 400,
        theme: 'modern',
        plugins: 'print preview paste searchreplace autolink directionality code visualblocks visualchars fullscreen image link media template codesample table charmap hr pagebreak nonbreaking anchor toc insertdatetime advlist lists textcolor wordcount spellchecker imagetools media contextmenu colorpicker textpattern help',
        toolbar: 'formatselect | undo redo | bold italic underline strikethrough forecolor backcolor | link | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | fullscreen code',
        menubar: false,
        statusbar: false,
        pagebreak_separator: '<span class="pagebreak"></span>',
        relative_urls: false,
        force_br_newlines: false,
        force_p_newlines: false,
        forced_root_block: '',
        language: 'de'
    };

    viewer.tinyMce = {
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.tinyMce.init');
                console.log('##############################');
                console.log('viewer.tinyMce.init: config - ', config);
            }

            $.extend(true, _defaults, config);

            // check current language
            switch (_defaults.currLang) {
                case 'de':
                    _defaults.language = 'de';
                    break;
                case 'es':
                    _defaults.language = 'es';
                    break;
                case 'pt':
                    _defaults.language = 'pt_PT';
                    break;
                case 'ru':
                    _defaults.language = 'ru';
                    break;
            }

            // init editor
            tinymce.init(_defaults);
        },
        overview: function overview() {
            // check if description or publication editing is enabled and
            // set fullscreen options
            if ($('.overview__description-editor').length > 0) {
                viewerJS.tinyConfig.setup = function (editor) {
                    editor.on('init', function (e) {
                        $('.overview__publication-action .btn').hide();
                    });
                    editor.on('FullscreenStateChanged', function (e) {
                        if (e.state) {
                            $('.overview__description-action-fullscreen').addClass('in');
                        } else {
                            $('.overview__description-action-fullscreen').removeClass('in');
                        }
                    });
                };
            } else {
                viewerJS.tinyConfig.setup = function (editor) {
                    editor.on('init', function (e) {
                        $('.overview__description-action .btn').hide();
                    });
                    editor.on('FullscreenStateChanged', function (e) {
                        if (e.state) {
                            $('.overview__publication-action-fullscreen').addClass('in');
                        } else {
                            $('.overview__publication-action-fullscreen').removeClass('in');
                        }
                    });
                };
            }
        }
    };

    return viewer;
}(viewerJS || {}, jQuery);

},{}],25:[function(require,module,exports){
'use strict';

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
 * Module to manage the user dropdown.
 * 
 * @version 3.2.0
 * @module viewerJS.userDropdown
 * @requires jQuery
 */
var viewerJS = function (viewer) {
    'use strict';

    var _debug = false;
    var _bookselfDropdown = false;
    var _defaults = {};

    viewer.userDropdown = {
        init: function init() {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.userDropdown.init');
                console.log('##############################');
            }

            // check if bookshelfdropdown exist
            if ($('.bookshelf-navigation__dropdown').length > 0) {
                _bookselfDropdown = true;
            }

            // login dropdown
            $('[data-toggle="login-dropdown"]').on('click', function (event) {
                event.stopPropagation();

                // hide bookshelfdropdow if exist
                if (_bookselfDropdown) {
                    $('.bookshelf-navigation__dropdown').hide();
                    $('.bookshelf-popup').remove();
                }
                // hide collection panel if exist
                if ($('.navigation__collection-panel').length > 0) {
                    $('.navigation__collection-panel').hide();
                }

                $('.login-navigation__login-dropdown').slideToggle('fast');
            });
            // user dropdown
            $('[data-toggle="user-dropdown"]').on('click', function (event) {
                event.stopPropagation();

                // hide bookshelfdropdow if exist
                if (_bookselfDropdown) {
                    $('.bookshelf-navigation__dropdown').hide();
                    $('.bookshelf-popup').remove();
                }
                // hide collection panel if exist
                if ($('.navigation__collection-panel').length > 0) {
                    $('.navigation__collection-panel').hide();
                }

                $('.login-navigation__user-dropdown').slideToggle('fast');
            });

            // retrieve account
            $('[data-toggle="retrieve-account"]').on('click', function () {
                $('.login-navigation__retrieve-account').addClass('in');
            });
            $('[data-dismiss="retrieve-account"]').on('click', function () {
                $('.login-navigation__retrieve-account').removeClass('in');
            });

            // remove dropdown by clicking on body
            $('body').on('click', function (event) {
                var target = $(event.target);
                var dropdown = $('.login-navigation__user-dropdown, .login-navigation__login-dropdown');
                var dropdownChild = dropdown.find('*');

                if (!target.is(dropdown) && !target.is(dropdownChild)) {
                    dropdown.hide();
                }
            });
        }
    };

    return viewer;
}(viewerJS || {}, jQuery);

},{}],26:[function(require,module,exports){
'use strict';

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
 * Module which renders current and other versions of a work into a widget.
 * 
 * @version 3.2.0
 * @module viewerJS.versionHistory
 * @requires jQuery
 */
var viewerJS = function (viewer) {
    'use strict';

    var _debug = false;
    var _defaults = {
        versions: [],
        json: null,
        imgUrl: '',
        imgPi: '',
        versionLink: '',
        widgetInputs: '',
        widgetList: ''
    };

    viewer.versionHistory = {
        /**
         * Method to initialize the version history widget.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {Array} config.versions An array which holds all versions.
         * @param {Object} config.json An JSON-Object which takes all versions.
         * @param {String} config.imgUrl The image URL for the current work.
         * @param {String} config.imgPi The PI for the image of the current work.
         * @param {String} config.versionLink A string placeholder for the final HTML.
         * @param {String} config.widgetInputs A string placeholder for the final HTML.
         * @param {String} config.widgetList A string placeholder for the final HTML.
         */
        init: function init(config) {
            if (_debug) {
                console.log('##############################');
                console.log('viewer.versionHistory.init');
                console.log('##############################');
                console.log('viewer.versionHistory.init: config = ', config);
            }

            $.extend(true, _defaults, config);

            // push versions into an array
            $(_defaults.widgetInputs).each(function () {
                _defaults.versions.push($(this).val());
            });

            if (_debug) {
                console.log('viewer.versionHistory: versions = ', _defaults.versions);
            }

            // append list elements to widget
            for (var i = 0; i < _defaults.versions.length; i++) {
                _defaults.json = JSON.parse(_defaults.versions[i]);

                if (_defaults.json.id === _defaults.imgPi) {
                    // Aktuell geöffnete Version - kein Link
                    _defaults.versionLink = '<li><span>';
                    if (_defaults.json.label != undefined && _defaults.json.label != '') {
                        _defaults.versionLink += _defaults.json.label;
                    } else {
                        _defaults.versionLink += _defaults.json.id;
                        if (_defaults.json.year != undefined && _defaults.json.year != '') {
                            _defaults.versionLink += ' (' + _defaults.json.year + ')';
                        }
                    }
                    _defaults.versionLink += '</span></li>';

                    $(_defaults.widgetList).append(_defaults.versionLink);
                } else {
                    // Vorgänger und Nachfolger jeweils mit Link
                    _defaults.versionLink = '<li><a href="' + _defaults.imgUrl + '/' + _defaults.json.id + '/1/">';
                    if (_defaults.json.label != undefined && _defaults.json.label != '') {
                        _defaults.versionLink += _defaults.json.label;
                    } else {
                        _defaults.versionLink += _defaults.json.id;
                        if (_defaults.json.year != undefined && _defaults.json.year != '') {
                            _defaults.versionLink += ' (' + _defaults.json.year + ')';
                        }
                    }
                    _defaults.versionLink += '</a></li>';

                    $(_defaults.widgetList).append(_defaults.versionLink);
                }
            }
        }
    };

    return viewer;
}(viewerJS || {}, jQuery);

},{}]},{},[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26])
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIm5vZGVfbW9kdWxlcy9icm93c2VyLXBhY2svX3ByZWx1ZGUuanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9tb2R1bGVzL3ZpZXdlci92aWV3ZXJKUy5ib29rc2hlbHZlc1Nlc3Npb24uanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9tb2R1bGVzL3ZpZXdlci92aWV3ZXJKUy5ib29rc2hlbHZlc1VzZXIuanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9tb2R1bGVzL3ZpZXdlci92aWV3ZXJKUy5jYWxlbmRhclBvcG92ZXIuanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9tb2R1bGVzL3ZpZXdlci92aWV3ZXJKUy5jaGFuZ2VGb250U2l6ZS5qcyIsIldlYkNvbnRlbnQvcmVzb3VyY2VzL2phdmFzY3JpcHQvZGV2L21vZHVsZXMvdmlld2VyL3ZpZXdlckpTLmRhdGFUYWJsZS5qcyIsIldlYkNvbnRlbnQvcmVzb3VyY2VzL2phdmFzY3JpcHQvZGV2L21vZHVsZXMvdmlld2VyL3ZpZXdlckpTLmRhdGVTb3J0ZWRGZWVkLmpzIiwiV2ViQ29udGVudC9yZXNvdXJjZXMvamF2YXNjcmlwdC9kZXYvbW9kdWxlcy92aWV3ZXIvdmlld2VySlMuZG93bmxvYWQuanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9tb2R1bGVzL3ZpZXdlci92aWV3ZXJKUy5kb3dubG9hZE1vZGFsLmpzIiwiV2ViQ29udGVudC9yZXNvdXJjZXMvamF2YXNjcmlwdC9kZXYvbW9kdWxlcy92aWV3ZXIvdmlld2VySlMuZWRpdENvbW1lbnQuanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9tb2R1bGVzL3ZpZXdlci92aWV3ZXJKUy5oZWxwZXIuanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9tb2R1bGVzL3ZpZXdlci92aWV3ZXJKUy5qcyIsIldlYkNvbnRlbnQvcmVzb3VyY2VzL2phdmFzY3JpcHQvZGV2L21vZHVsZXMvdmlld2VyL3ZpZXdlckpTLm5hdmlnYXRpb24uanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9tb2R1bGVzL3ZpZXdlci92aWV3ZXJKUy5uZXJGYWNldHRpbmcuanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9tb2R1bGVzL3ZpZXdlci92aWV3ZXJKUy5uZXJGdWxsdGV4dC5qcyIsIldlYkNvbnRlbnQvcmVzb3VyY2VzL2phdmFzY3JpcHQvZGV2L21vZHVsZXMvdmlld2VyL3ZpZXdlckpTLm5vcm1kYXRhLmpzIiwiV2ViQ29udGVudC9yZXNvdXJjZXMvamF2YXNjcmlwdC9kZXYvbW9kdWxlcy92aWV3ZXIvdmlld2VySlMucGFnZVNjcm9sbC5qcyIsIldlYkNvbnRlbnQvcmVzb3VyY2VzL2phdmFzY3JpcHQvZGV2L21vZHVsZXMvdmlld2VyL3ZpZXdlckpTLnJlc3BvbnNpdmVDb2x1bW5HYWxsZXJ5LmpzIiwiV2ViQ29udGVudC9yZXNvdXJjZXMvamF2YXNjcmlwdC9kZXYvbW9kdWxlcy92aWV3ZXIvdmlld2VySlMuc2VhcmNoQWR2YW5jZWQuanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9tb2R1bGVzL3ZpZXdlci92aWV3ZXJKUy5zZWFyY2hMaXN0LmpzIiwiV2ViQ29udGVudC9yZXNvdXJjZXMvamF2YXNjcmlwdC9kZXYvbW9kdWxlcy92aWV3ZXIvdmlld2VySlMuc2VhcmNoU29ydGluZ0Ryb3Bkb3duLmpzIiwiV2ViQ29udGVudC9yZXNvdXJjZXMvamF2YXNjcmlwdC9kZXYvbW9kdWxlcy92aWV3ZXIvdmlld2VySlMuc2ltcGxlTGlnaHRCb3guanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9tb2R1bGVzL3ZpZXdlci92aWV3ZXJKUy5zdGFja2VkVGh1bWJuYWlscy5qcyIsIldlYkNvbnRlbnQvcmVzb3VyY2VzL2phdmFzY3JpcHQvZGV2L21vZHVsZXMvdmlld2VyL3ZpZXdlckpTLnRpbWVtYXRyaXguanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9tb2R1bGVzL3ZpZXdlci92aWV3ZXJKUy50aW55TWNlLmpzIiwiV2ViQ29udGVudC9yZXNvdXJjZXMvamF2YXNjcmlwdC9kZXYvbW9kdWxlcy92aWV3ZXIvdmlld2VySlMudXNlckRyb3Bkb3duLmpzIiwiV2ViQ29udGVudC9yZXNvdXJjZXMvamF2YXNjcmlwdC9kZXYvbW9kdWxlcy92aWV3ZXIvdmlld2VySlMudmVyc2lvbkhpc3RvcnkuanMiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6IkFBQUE7OztBQ0FBOzs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7QUF3QkEsSUFBSSxXQUFhLFVBQVUsTUFBVixFQUFtQjtBQUNoQzs7QUFFQSxRQUFJLFNBQVMsS0FBYjtBQUNBLFFBQUksa0JBQWtCLENBQXRCO0FBQ0EsUUFBSSxZQUFZO0FBQ1osY0FBTSxFQURNO0FBRVosYUFBSztBQUNELDhCQUFrQixFQURqQjtBQUVELHFDQUF5QixFQUZ4QjtBQUdELCtCQUFtQjtBQUhsQjtBQUZPLEtBQWhCOztBQVNBLFdBQU8sa0JBQVAsR0FBNEI7QUFDeEIsY0FBTSxjQUFVLE1BQVYsRUFBbUI7QUFDckIsZ0JBQUssTUFBTCxFQUFjO0FBQ1Ysd0JBQVEsR0FBUixDQUFhLGdDQUFiO0FBQ0Esd0JBQVEsR0FBUixDQUFhLGdDQUFiO0FBQ0Esd0JBQVEsR0FBUixDQUFhLGdDQUFiO0FBQ0Esd0JBQVEsR0FBUixDQUFhLDJDQUFiLEVBQTBELE1BQTFEO0FBQ0g7O0FBRUQsY0FBRSxNQUFGLENBQVUsSUFBVixFQUFnQixTQUFoQixFQUEyQixNQUEzQjs7QUFFQTtBQUNBLGdCQUFLLGFBQWEsT0FBYixDQUFzQixnQkFBdEIsS0FBNEMsU0FBakQsRUFBNkQ7QUFDekQsNkJBQWEsT0FBYixDQUFzQixnQkFBdEIsRUFBd0MsQ0FBeEM7QUFDSDs7QUFFRDtB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RSxzQ0FBVSxXQUFWLElBQXlCLE9BQU8sVUFBVSxJQUFWLENBQWUsSUFBdEIsR0FBNkIsR0FBdEQ7QUFDQTtBQUNGO0FBQ0QsOEJBQVUsV0FBVixJQUF5QixjQUF6Qjs7QUFFQSxzQkFBRyxVQUFVLFVBQWIsRUFBMEIsTUFBMUIsQ0FBa0MsVUFBVSxXQUE1QztBQUNILGlCQWZELE1BZ0JLO0FBQ0Q7QUFDQSw4QkFBVSxXQUFWLEdBQXdCLGtCQUFrQixVQUFVLE1BQTVCLEdBQXFDLEdBQXJDLEdBQTJDLFVBQVUsSUFBVixDQUFlLEVBQTFELEdBQStELE9BQXZGO0FBQ0Esd0JBQUssVUFBVSxJQUFWLENBQWUsS0FBZixJQUF3QixTQUF4QixJQUFxQyxVQUFVLElBQVYsQ0FBZSxLQUFmLElBQXdCLEVBQWxFLEVBQXVFO0FBQ3RFLGtDQUFVLFdBQVYsSUFBeUIsVUFBVSxJQUFWLENBQWUsS0FBeEM7QUFDQSxxQkFGRCxNQUVPO0FBQ04sa0NBQVUsV0FBVixJQUF5QixVQUFVLElBQVYsQ0FBZSxFQUF4QztBQUNBLDRCQUFLLFVBQVUsSUFBVixDQUFlLElBQWYsSUFBdUIsU0FBdkIsSUFBb0MsVUFBVSxJQUFWLENBQWUsSUFBZixJQUF1QixFQUFoRSxFQUFxRTtBQUNwRSxzQ0FBVSxXQUFWLElBQXlCLE9BQU8sVUFBVSxJQUFWLENBQWUsSUFBdEIsR0FBNkIsR0FBdEQ7QUFDQTtBQUNEO0FBQ0QsOEJBQVUsV0FBVixJQUF5QixXQUF6Qjs7QUFFQSxzQkFBRyxVQUFVLFVBQWIsRUFBMEIsTUFBMUIsQ0FBa0MsVUFBVSxXQUE1QztBQUNIO0FBQ0o7QUFDSjtBQXJFbUIsS0FBeEI7O0FBd0VBLFdBQU8sTUFBUDtBQUVILENBeEZjLENBd0ZWLFlBQVksRUF4RkYsRUF3Rk0sTUF4Rk4sQ0FBZiIsImZpbGUiOiJnZW5lcmF0ZWQuanMiLCJzb3VyY2VSb290IjoiIiwic291cmNlc0NvbnRlbnQiOlsiKGZ1bmN0aW9uKCl7ZnVuY3Rpb24gZSh0LG4scil7ZnVuY3Rpb24gcyhvLHUpe2lmKCFuW29dKXtpZighdFtvXSl7dmFyIGE9dHlwZW9mIHJlcXVpcmU9PVwiZnVuY3Rpb25cIiYmcmVxdWlyZTtpZighdSYmYSlyZXR1cm4gYShvLCEwKTtpZihpKXJldHVybiBpKG8sITApO3ZhciBmPW5ldyBFcnJvcihcIkNhbm5vdCBmaW5kIG1vZHVsZSAnXCIrbytcIidcIik7dGhyb3cgZi5jb2RlPVwiTU9EVUxFX05PVF9GT1VORFwiLGZ9dmFyIGw9bltvXT17ZXhwb3J0czp7fX07dFtvXVswXS5jYWxsKGwuZXhwb3J0cyxmdW5jdGlvbihlKXt2YXIgbj10W29dWzFdW2VdO3JldHVybiBzKG4/bjplKX0sbCxsLmV4cG9ydHMsZSx0LG4scil9cmV0dXJuIG5bb10uZXhwb3J0c312YXIgaT10eXBlb2YgcmVxdWlyZT09XCJmdW5jdGlvblwiJiZyZXF1aXJlO2Zvcih2YXIgbz0wO288ci5sZW5ndGg7bysrKXMocltvXSk7cmV0dXJuIHN9cmV0dXJuIGV9KSgpIiwiLyoqXG4gKiBUaGlzIGZpbGUgaXMgcGFydCBvZiB0aGUgR29vYmkgdmlld2VyIC0gYSBjb250ZW50IHByZXNlbnRhdGlvbiBhbmQgbWFuYWdlbWVudFxuICogYXBwbGljYXRpb24gZm9yIGRpZ2l0aXplZCBvYmplY3RzLlxuICogXG4gKiBWaXNpdCB0aGVzZSB3ZWJzaXRlcyBmb3IgbW9yZSBpbmZvcm1hdGlvbi4gLSBodHRwOi8vd3d3LmludHJhbmRhLmNvbSAtXG4gKiBodHRwOi8vZGlnaXZlcnNvLmNvbVxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZnJlZSBzb2Z0d2FyZTsgeW91IGNhbiByZWRpc3RyaWJ1dGUgaXQgYW5kL29yIG1vZGlmeSBpdCB1bmRlciB0aGUgdGVybXNcbiAqIG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhcyBwdWJsaXNoZWQgYnkgdGhlIEZyZWUgU29mdHdhcmUgRm91bmRhdGlvbjsgZWl0aGVyXG4gKiB2ZXJzaW9uIDIgb2YgdGhlIExpY2Vuc2UsIG9yIChhdCB5b3VyIG9wdGlvbikgYW55IGxhdGVyIHZlcnNpb24uXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBkaXN0cmlidXRlZCBpbiB0aGUgaG9wZSB0aGF0IGl0IHdpbGwgYmUgdXNlZnVsLCBidXQgV0lUSE9VVCBBTllcbiAqIFdBUlJBTlRZOyB3aXRob3V0IGV2ZW4gdGhlIGltcGxpZWQgd2FycmFudHkgb2YgTUVSQ0hBTlRBQklMSVRZIG9yIEZJVE5FU1MgRk9SIEFcbiAqIFBBUlRJQ1VMQVIgUFVSUE9TRS4gU2VlIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBmb3IgbW9yZSBkZXRhaWxzLlxuICogXG4gKiBZb3Ugc2hvdWxkIGhhdmUgcmVjZWl2ZWQgYSBjb3B5IG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhbG9uZyB3aXRoIHRoaXNcbiAqIHByb2dyYW0uIElmIG5vdCwgc2VlIDxodHRwOi8vd3d3LmdudS5vcmcvbGljZW5zZXMvPi5cbiAqIFxuICogTW9kdWxlIHRvIG1hbmFnZSBib29rc2hlbHZlcyBpbiB0aGUgY3VycmVudCBzZXNzaW9uLlxuICogXG4gKiBAdmVyc2lvbiAzLjIuMFxuICogQG1vZHVsZSB2aWV3ZXJKUy5ib29rc2hlbHZlc1Nlc3Npb25cbiAqIEByZXF1aXJlcyBqUXVlcnlcbiAqL1xudmFyIHZpZXdlckpTID0gKCBmdW5jdGlvbiggdmlld2VyICkge1xuICAgICd1c2Ugc3RyaWN0JztcbiAgICBcbiAgICB2YXIgX2RlYnVnID0gZmFsc2U7XG4gICAgdmFyIF9jb25maXJtQ291bnRlciA9IDA7XG4gICAgdmFyIF9kZWZhdWx0cyA9IHtcbiAgICAgICAgcm9vdDogJycsXG4gICAgICAgIG1zZzoge1xuICAgICAgICAgICAgcmVzZXRCb29rc2hlbHZlczogJycsXG4gICAgICAgICAgICByZXNldEJvb2tzaGVsdmVzQ29uZmlybTogJycsXG4gICAgICAgICAgICBzYXZlSXRlbVRvU2Vzc2lvbjogJydcbiAgICAgICAgfVxuICAgIH07XG4gICAgXG4gICAgdmlld2VyLmJvb2tzaGVsdmVzU2Vzc2lvbiA9IHtcbiAgICAgICAgaW5pdDogZnVuY3Rpb24oIGNvbmZpZyApIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLmJvb2tzaGVsdmVzU2Vzc2lvbi5pbml0JyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLmJvb2tzaGVsdmVzU2Vzc2lvbi5pbml0OiBjb25maWcgLSAnLCBjb25maWcgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgJC5leHRlbmQoIHRydWUsIF9kZWZhdWx0cywgY29uZmlnICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIHNldCBjb25maXJtIGNvdW50ZXIgdG8gbG9jYWwgc3RvcmFnZVxuICAgICAgICAgICAgaWYgKCBsb2NhbFN0b3JhZ2UuZ2V0SXRlbSggJ2NvbmZpcm1Db3VudGVyJyApID09IHVuZGVmaW5lZCApIHtcbiAgICAgICAgICAgICAgICBsb2NhbFN0b3JhZ2Uuc2V0SXRlbSggJ2NvbmZpcm1Db3VudGVyJywgMCApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICAvLyByZW5kZXIgYm9va3NoZWxmIGRyb3Bkb3duIGxpc3RcbiAgICAgICAgICAgIF9yZW5kZXJEcm9wZG93bkxpc3QoKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gdG9nZ2xlIGJvb2tzaGVsZiBkcm9wZG93blxuICAgICAgICAgICAgJCggJ1tkYXRhLWJvb2tzaGVsZi10eXBlPVwiZHJvcGRvd25cIl0nICkub2ZmKCkub24oICdjbGljaycsIGZ1bmN0aW9uKCBldmVudCApIHtcbiAgICAgICAgICAgICAgICBldmVudC5zdG9wUHJvcGFnYXRpb24oKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAvLyBoaWRlIG90aGVyIGRyb3Bkb3duc1xuICAgICAgICAgICAgICAgICQoICcubG9naW4tbmF2aWdhdGlvbl9fbG9naW4tZHJvcGRvd24sIC5sb2dpbi1uYXZpZ2F0aW9uX191c2VyLWRyb3Bkb3duLCAubmF2aWdhdGlvbl9fY29sbGVjdGlvbi1wYW5lbCcgKS5oaWRlKCk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgX2dldEFsbFNlc3Npb25FbGVtZW50cyggX2RlZmF1bHRzLnJvb3QgKS50aGVuKCBmdW5jdGlvbiggZWxlbWVudHMgKSB7XG4gICAgICAgICAgICAgICAgICAgIGlmICggZWxlbWVudHMuaXRlbXMubGVuZ3RoID4gMCApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICQoICcuYm9va3NoZWxmLW5hdmlnYXRpb25fX2Ryb3Bkb3duJyApLnNsaWRlVG9nZ2xlKCAnZmFzdCcgKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiBmYWxzZTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH0gKS5mYWlsKCBmdW5jdGlvbiggZXJyb3IgKSB7XG4gICAgICAgICAgICAgICAgICAgIGNvbnNvbGUuZXJyb3IoICdFUlJPUiAtIF9nZXRBbGxTZXNzaW9uRWxlbWVudHM6ICcsIGVycm9yLnJlc3BvbnNlVGV4dCApO1xuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gc2V0IGVsZW1lbnQgY291bnQgb2YgbGlzdCB0byBjb3VudGVyXG4gICAgICAgICAgICBfc2V0U2Vzc2lvbkVsZW1lbnRDb3VudCgpO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICAvLyBjaGVjayBhZGQgYnV0dG9ucyBpZiBlbGVtZW50IGlzIGluIGxpc3RcbiAgICAgICAgICAgIF9zZXRBZGRBY3RpdmVTdGF0ZSgpO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICAvLyBhZGQgZWxlbWVudCB0byBzZXNzaW9uXG4gICAgICAgICAgICAkKCAnW2RhdGEtYm9va3NoZWxmLXR5cGU9XCJhZGRcIl0nICkub2ZmKCkub24oICdjbGljaycsIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgIHZhciBjdXJyQnRuID0gJCggdGhpcyApO1xuICAgICAgICAgICAgICAgIHZhciBjdXJyUGkgPSBjdXJyQnRuLmF0dHIoICdkYXRhLXBpJyApO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIF9pc0VsZW1lbnRTZXQoIF9kZWZhdWx0cy5yb290LCBjdXJyUGkgKS50aGVuKCBmdW5jdGlvbiggaXNTZXQgKSB7XG4gICAgICAgICAgICAgICAgICAgIC8vIHNldCBjb25maXJtIGNvdW50ZXJcbiAgICAgICAgICAgICAgICAgICAgX2NvbmZpcm1Db3VudGVyID0gcGFyc2VJbnQoIGxvY2FsU3RvcmFnZS5nZXRJdGVtKCAnY29uZmlybUNvdW50ZXInICkgKTtcbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgIGlmICggIWlzU2V0ICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKCBfY29uZmlybUNvdW50ZXIgPT0gMCApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBpZiAoIGNvbmZpcm0oIF9kZWZhdWx0cy5tc2cuc2F2ZUl0ZW1Ub1Nlc3Npb24gKSApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgY3VyckJ0bi5hZGRDbGFzcyggJ2FkZGVkJyApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBsb2NhbFN0b3JhZ2Uuc2V0SXRlbSggJ2NvbmZpcm1Db3VudGVyJywgMSApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBfc2V0U2Vzc2lvbkVsZW1lbnQoIF9kZWZhdWx0cy5yb290LCBjdXJyUGkgKS50aGVuKCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIF9zZXRTZXNzaW9uRWxlbWVudENvdW50KCk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBfcmVuZGVyRHJvcGRvd25MaXN0KCk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiBmYWxzZTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBjdXJyQnRuLmFkZENsYXNzKCAnYWRkZWQnICk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgX3NldFNlc3Npb25FbGVtZW50KCBfZGVmYXVsdHMucm9vdCwgY3VyclBpICkudGhlbiggZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIF9zZXRTZXNzaW9uRWxlbWVudENvdW50KCk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIF9yZW5kZXJEcm9wZG93bkxpc3QoKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBjdXJyQnRuLnJlbW92ZUNsYXNzKCAnYWRkZWQnICk7XG4gICAgICAgICAgICAgICAgICAgICAgICBfZGVsZXRlU2Vzc2lvbkVsZW1lbnQoIF9kZWZhdWx0cy5yb290LCBjdXJyUGkgKS50aGVuKCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBfc2V0U2Vzc2lvbkVsZW1lbnRDb3VudCgpO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIF9yZW5kZXJEcm9wZG93bkxpc3QoKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH0gKS5mYWlsKCBmdW5jdGlvbiggZXJyb3IgKSB7XG4gICAgICAgICAgICAgICAgICAgIGNvbnNvbGUuZXJyb3IoICdFUlJPUiAtIF9pc0VsZW1lbnRTZXQ6ICcsIGVycm9yLnJlc3BvbnNlVGV4dCApO1xuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gaGlkZSBtZW51cyBieSBjbGlja2luZyBvbiBib2R5XG4gICAgICAgICAgICAkKCAnYm9keScgKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oIGV2ZW50ICkge1xuICAgICAgICAgICAgICAgIHZhciB0YXJnZXQgPSAkKCBldmVudC50YXJnZXQgKTtcbiAgICAgICAgICAgICAgICB2YXIgZHJvcGRvd24gPSAkKCAnLmJvb2tzaGVsZi1uYXZpZ2F0aW9uX19kcm9wZG93bicgKTtcbiAgICAgICAgICAgICAgICB2YXIgZHJvcGRvd25DaGlsZCA9IGRyb3Bkb3duLmZpbmQoICcqJyApO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIGlmICggIXRhcmdldC5pcyggZHJvcGRvd24gKSAmJiAhdGFyZ2V0LmlzKCBkcm9wZG93bkNoaWxkICkgKSB7XG4gICAgICAgICAgICAgICAgICAgICQoICcuYm9va3NoZWxmLW5hdmlnYXRpb25fX2Ryb3Bkb3duJyApLmhpZGUoKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9ICk7XG4gICAgICAgIH1cbiAgICB9O1xuICAgIC8qICMjIyMjIyMjIEFERCAoQ1JFQVRFKSAjIyMjIyMjIyAqL1xuXG4gICAgLyogIyMjIyMjIyMgR0VUIChSRUFEKSAjIyMjIyMjIyAqL1xuICAgIC8qKlxuICAgICAqIE1ldGhvZCB0byBnZXQgYWxsIGVsZW1lbnRzIGluIHdhdGNobGlzdCBmcm9tIGN1cnJlbnQgc2Vzc2lvbiAodXNlciBub3QgbG9nZ2VkIGluKS5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9nZXRBbGxTZXNzaW9uRWxlbWVudHNcbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gcm9vdCBUaGUgYXBwbGljYXRpb24gcm9vdCBwYXRoLlxuICAgICAqIEByZXR1cm5zIHtPYmplY3R9IEFuIEpTT04tT2JqZWN0IHdoaWNoIGNvbnRhaW5zIGFsbCBzZXNzaW9uIGVsZW1lbnRzLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9nZXRBbGxTZXNzaW9uRWxlbWVudHMoIHJvb3QgKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICctLS0tLS0tLS0tIF9nZXRBbGxTZXNzaW9uRWxlbWVudHMoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfZ2V0QWxsU2Vzc2lvbkVsZW1lbnRzOiByb290IC0gJywgcm9vdCApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgcHJvbWlzZSA9IFEoICQuYWpheCgge1xuICAgICAgICAgICAgdXJsOiByb290ICsgJy9yZXN0L2Jvb2tzaGVsdmVzL3Nlc3Npb24vZ2V0LycsXG4gICAgICAgICAgICB0eXBlOiBcIkdFVFwiLFxuICAgICAgICAgICAgZGF0YVR5cGU6IFwiSlNPTlwiLFxuICAgICAgICAgICAgYXN5bmM6IHRydWVcbiAgICAgICAgfSApICk7XG4gICAgICAgIFxuICAgICAgICByZXR1cm4gcHJvbWlzZTtcbiAgICB9XG4gICAgLyoqXG4gICAgICogTWV0aG9kIHRvIGNoZWNrIGlmIGVsZW1lbnQgaXMgaW4gbGlzdCAodXNlciBub3QgbG9nZ2VkIGluKS5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9pc0VsZW1lbnRTZXRcbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gcm9vdCBUaGUgYXBwbGljYXRpb24gcm9vdCBwYXRoLlxuICAgICAqIEBwYXJhbSB7U3RyaW5nfSBwaSBUaGUgcGVyc2lzdGVudCBpZGVudGlmaWVyIG9mIHRoZSBzYXZlZCBlbGVtZW50LlxuICAgICAqIEByZXR1cm5zIHtCb29sZWFufSBUcnVlIGlmIGVsZW1lbnQgaXMgc2V0LlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9pc0VsZW1lbnRTZXQoIHJvb3QsIHBpICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfaXNFbGVtZW50U2V0KCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2lzRWxlbWVudFNldDogcm9vdCAtICcsIHJvb3QgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2lzRWxlbWVudFNldDogcGkgLSAnLCBwaSApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgcHJvbWlzZSA9IFEoICQuYWpheCgge1xuICAgICAgICAgICAgdXJsOiByb290ICsgJy9yZXN0L2Jvb2tzaGVsdmVzL3Nlc3Npb24vY29udGFpbnMvJyArIHBpICsgJy8nLFxuICAgICAgICAgICAgdHlwZTogXCJHRVRcIixcbiAgICAgICAgICAgIGRhdGFUeXBlOiBcIkpTT05cIixcbiAgICAgICAgICAgIGFzeW5jOiB0cnVlXG4gICAgICAgIH0gKSApO1xuICAgICAgICBcbiAgICAgICAgcmV0dXJuIHByb21pc2VcbiAgICB9XG4gICAgXG4gICAgLyogIyMjIyMjIyMgU0VUIChVUERBVEUpICMjIyMjIyMjICovXG4gICAgLyoqXG4gICAgICogTWV0aG9kIHRvIGFkZCBhbiBlbGVtZW50cyB0byB3YXRjaGxpc3QgaW4gY3VycmVudCBzZXNzaW9uICh1c2VyIG5vdCBsb2dnZWQgaW4pLlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX3NldFNlc3Npb25FbGVtZW50XG4gICAgICogQHBhcmFtIHtTdHJpbmd9IHJvb3QgVGhlIGFwcGxpY2F0aW9uIHJvb3QgcGF0aC5cbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gcGkgVGhlIHBlcnNpc3RlbnQgaWRlbnRpZmllciBvZiB0aGUgc2F2ZWQgZWxlbWVudC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfc2V0U2Vzc2lvbkVsZW1lbnQoIHJvb3QsIHBpICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfc2V0U2Vzc2lvbkVsZW1lbnQoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfc2V0U2Vzc2lvbkVsZW1lbnQ6IHJvb3QgLSAnLCByb290ICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19zZXRTZXNzaW9uRWxlbWVudDogcGkgLSAnLCBwaSApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgcHJvbWlzZSA9IFEoICQuYWpheCgge1xuICAgICAgICAgICAgdXJsOiByb290ICsgJy9yZXN0L2Jvb2tzaGVsdmVzL3Nlc3Npb24vYWRkLycgKyBwaSArICcvJyxcbiAgICAgICAgICAgIHR5cGU6IFwiR0VUXCIsXG4gICAgICAgICAgICBkYXRhVHlwZTogXCJKU09OXCIsXG4gICAgICAgICAgICBhc3luYzogdHJ1ZVxuICAgICAgICB9ICkgKTtcbiAgICAgICAgXG4gICAgICAgIHJldHVybiBwcm9taXNlXG4gICAgfVxuICAgIFxuICAgIC8qICMjIyMjIyMjIERFTEVURSAjIyMjIyMjIyAqL1xuICAgIC8qKlxuICAgICAqIE1ldGhvZCB0byBkZWxldGUgYW4gZWxlbWVudCBmcm9tIHdhdGNobGlzdCBpbiBjdXJyZW50IHNlc3Npb24gKHVzZXIgbm90IGxvZ2dlZCBpbikuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfZGVsZXRlU2Vzc2lvbkVsZW1lbnRcbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gcm9vdCBUaGUgYXBwbGljYXRpb24gcm9vdCBwYXRoLlxuICAgICAqIEBwYXJhbSB7U3RyaW5nfSBwaSBUaGUgcGVyc2lzdGVudCBpZGVudGlmaWVyIG9mIHRoZSBzYXZlZCBlbGVtZW50LlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9kZWxldGVTZXNzaW9uRWxlbWVudCggcm9vdCwgcGkgKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICctLS0tLS0tLS0tIF9kZWxldGVTZXNzaW9uRWxlbWVudCgpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19kZWxldGVTZXNzaW9uRWxlbWVudDogcm9vdCAtICcsIHJvb3QgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2RlbGV0ZVNlc3Npb25FbGVtZW50OiBwaSAtICcsIHBpICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIHZhciBwcm9taXNlID0gUSggJC5hamF4KCB7XG4gICAgICAgICAgICB1cmw6IHJvb3QgKyAnL3Jlc3QvYm9va3NoZWx2ZXMvc2Vzc2lvbi9kZWxldGUvJyArIHBpICsgJy8nLFxuICAgICAgICAgICAgdHlwZTogXCJHRVRcIixcbiAgICAgICAgICAgIGRhdGFUeXBlOiBcIkpTT05cIixcbiAgICAgICAgICAgIGFzeW5jOiB0cnVlXG4gICAgICAgIH0gKSApO1xuICAgICAgICBcbiAgICAgICAgcmV0dXJuIHByb21pc2VcbiAgICB9XG4gICAgLyoqXG4gICAgICogTWV0aG9kIHRvIGRlbGV0ZSBhbGwgZWxlbWVudHMgZnJvbSB3YXRjaGxpc3QgaW4gY3VycmVudCBzZXNzaW9uICh1c2VyIG5vdCBsb2dnZWRcbiAgICAgKiBpbikuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfZGVsZXRlQWxsU2Vzc2lvbkVsZW1lbnRzXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IHJvb3QgVGhlIGFwcGxpY2F0aW9uIHJvb3QgcGF0aC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfZGVsZXRlQWxsU2Vzc2lvbkVsZW1lbnRzKCByb290ICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfZGVsZXRlQWxsU2Vzc2lvbkVsZW1lbnRzKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2RlbGV0ZUFsbFNlc3Npb25FbGVtZW50czogcm9vdCAtICcsIHJvb3QgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgdmFyIHByb21pc2UgPSBRKCAkLmFqYXgoIHtcbiAgICAgICAgICAgIHVybDogcm9vdCArICcvcmVzdC9ib29rc2hlbHZlcy9zZXNzaW9uL2RlbGV0ZS8nLFxuICAgICAgICAgICAgdHlwZTogXCJHRVRcIixcbiAgICAgICAgICAgIGRhdGFUeXBlOiBcIkpTT05cIixcbiAgICAgICAgICAgIGFzeW5jOiB0cnVlXG4gICAgICAgIH0gKSApO1xuICAgICAgICBcbiAgICAgICAgcmV0dXJuIHByb21pc2VcbiAgICB9XG4gICAgXG4gICAgLyogIyMjIyMjIyMgQlVJTEQgIyMjIyMjIyMgKi9cbiAgICAvKipcbiAgICAgKiBNZXRob2QgdG8gc2V0IHRoZSBjb3VudCBvZiBlbGVtZW50cyBpbiB3YXRjaGxpc3QgZnJvbSBjdXJyZW50IHNlc3Npb24gKHVzZXIgbm90XG4gICAgICogbG9nZ2VkIGluKS5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9zZXRTZXNzaW9uRWxlbWVudENvdW50XG4gICAgICogQHBhcmFtIHtTdHJpbmd9IHJvb3QgVGhlIGFwcGxpY2F0aW9uIHJvb3QgcGF0aC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfc2V0U2Vzc2lvbkVsZW1lbnRDb3VudCgpIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX3NldFNlc3Npb25FbGVtZW50Q291bnQoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICBfZ2V0QWxsU2Vzc2lvbkVsZW1lbnRzKCBfZGVmYXVsdHMucm9vdCApLnRoZW4oIGZ1bmN0aW9uKCBlbGVtZW50cyApIHtcbiAgICAgICAgICAgICQoICdbZGF0YS1ib29rc2hlbGYtdHlwZT1cImNvdW50ZXJcIl0nICkuZW1wdHkoKS50ZXh0KCBlbGVtZW50cy5pdGVtcy5sZW5ndGggKTtcbiAgICAgICAgfSApLmZhaWwoIGZ1bmN0aW9uKCBlcnJvciApIHtcbiAgICAgICAgICAgIGNvbnNvbGUuZXJyb3IoICdFUlJPUiAtIF9nZXRBbGxTZXNzaW9uRWxlbWVudHM6ICcsIGVycm9yLnJlc3BvbnNlVGV4dCApO1xuICAgICAgICB9ICk7XG4gICAgfVxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB0byByZW5kZXIgdGhlIGVsZW1lbnQgbGlzdCBpbiBib29rc2hlbGYgZHJvcGRvd24gKHVzZXIgbm90IGxvZ2dlZCBpbikuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfcmVuZGVyRHJvcGRvd25MaXN0XG4gICAgICovXG4gICAgZnVuY3Rpb24gX3JlbmRlckRyb3Bkb3duTGlzdCgpIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX3JlbmRlckRyb3Bkb3duTGlzdCgpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIF9nZXRBbGxTZXNzaW9uRWxlbWVudHMoIF9kZWZhdWx0cy5yb290ICkudGhlbiggZnVuY3Rpb24oIGVsZW1lbnRzICkge1xuICAgICAgICAgICAgLy8gRE9NLUVsZW1lbnRzXG4gICAgICAgICAgICB2YXIgZHJvcGRvd25MaXN0UmVzZXQgPSAkKCAnPGJ1dHRvbj4nICkuYWRkQ2xhc3MoICdidG4tY2xlYW4nICkuYXR0ciggJ3R5cGUnLCAnYnV0dG9uJyApLmF0dHIoICdkYXRhLWJvb2tzaGVsZi10eXBlJywgJ3Jlc2V0JyApLnRleHQoIF9kZWZhdWx0cy5tc2cucmVzZXRCb29rc2hlbHZlcyApO1xuICAgICAgICAgICAgdmFyIGRyb3Bkb3duTGlzdCA9ICQoICc8dWwgLz4nICkuYWRkQ2xhc3MoICdsaXN0JyApO1xuICAgICAgICAgICAgdmFyIGRyb3Bkb3duTGlzdEl0ZW0gPSBudWxsO1xuICAgICAgICAgICAgdmFyIGRyb3Bkb3duTGlzdEl0ZW1Sb3cgPSBudWxsO1xuICAgICAgICAgICAgdmFyIGRyb3Bkb3duTGlzdEl0ZW1Db2xMZWZ0ID0gbnVsbDtcbiAgICAgICAgICAgIHZhciBkcm9wZG93bkxpc3RJdGVtQ29sQ2VudGVyID0gbnVsbDtcbiAgICAgICAgICAgIHZhciBkcm9wZG93bkxpc3RJdGVtQ29sUmlnaHQgPSBudWxsO1xuICAgICAgICAgICAgdmFyIGRyb3Bkb3duTGlzdEl0ZW1JbWFnZSA9IG51bGw7XG4gICAgICAgICAgICB2YXIgZHJvcGRvd25MaXN0SXRlbU5hbWUgPSBudWxsO1xuICAgICAgICAgICAgdmFyIGRyb3Bkb3duTGlzdEl0ZW1OYW1lTGluayA9IG51bGw7XG4gICAgICAgICAgICB2YXIgZHJvcGRvd25MaXN0SXRlbURlbGV0ZSA9IG51bGw7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIHNldCBjb25maXJtIGNvdW50ZXJcbiAgICAgICAgICAgIGlmICggZWxlbWVudHMuaXRlbXMubGVuZ3RoIDwgMSApIHtcbiAgICAgICAgICAgICAgICBsb2NhbFN0b3JhZ2Uuc2V0SXRlbSggJ2NvbmZpcm1Db3VudGVyJywgMCApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICBlbGVtZW50cy5pdGVtc1xuICAgICAgICAgICAgICAgICAgICAuZm9yRWFjaCggZnVuY3Rpb24oIGl0ZW0gKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBkcm9wZG93bkxpc3RJdGVtID0gJCggJzxsaSAvPicgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGRyb3Bkb3duTGlzdEl0ZW1Sb3cgPSAkKCAnPGRpdiAvPicgKS5hZGRDbGFzcyggJ3JvdyBuby1tYXJnaW4nICk7XG4gICAgICAgICAgICAgICAgICAgICAgICBkcm9wZG93bkxpc3RJdGVtQ29sTGVmdCA9ICQoICc8ZGl2IC8+JyApLmFkZENsYXNzKCAnY29sLXhzLTQgbm8tcGFkZGluZycgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGRyb3Bkb3duTGlzdEl0ZW1Db2xDZW50ZXIgPSAkKCAnPGRpdiAvPicgKS5hZGRDbGFzcyggJ2NvbC14cy03IG5vLXBhZGRpbmcnICk7XG4gICAgICAgICAgICAgICAgICAgICAgICBkcm9wZG93bkxpc3RJdGVtQ29sUmlnaHQgPSAkKCAnPGRpdiAvPicgKS5hZGRDbGFzcyggJ2NvbC14cy0xIG5vLXBhZGRpbmcgYm9va3NoZWxmLW5hdmlnYXRpb25fX2Ryb3Bkb3duLWxpc3QtcmVtb3ZlJyApO1xuICAgICAgICAgICAgICAgICAgICAgICAgZHJvcGRvd25MaXN0SXRlbUltYWdlID0gJCggJzxkaXYgLz4nICkuYWRkQ2xhc3MoICdib29rc2hlbGYtbmF2aWdhdGlvbl9fZHJvcGRvd24tbGlzdC1pbWFnZScgKS5jc3MoICdiYWNrZ3JvdW5kLWltYWdlJywgJ3VybCgnXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICsgaXRlbS5yZXByZXNlbnRhdGl2ZUltYWdlVXJsICsgJyknICk7XG4gICAgICAgICAgICAgICAgICAgICAgICBkcm9wZG93bkxpc3RJdGVtTmFtZSA9ICQoICc8aDQgLz4nICk7XG4gICAgICAgICAgICAgICAgICAgICAgICBkcm9wZG93bkxpc3RJdGVtTmFtZUxpbmsgPSAkKCAnPGEgLz4nICkuYXR0ciggJ2hyZWYnLCBfZGVmYXVsdHMucm9vdCArIGl0ZW0udXJsICkudGV4dCggaXRlbS5uYW1lICk7XG4gICAgICAgICAgICAgICAgICAgICAgICBkcm9wZG93bkxpc3RJdGVtRGVsZXRlID0gJCggJzxidXR0b24gLz4nICkuYWRkQ2xhc3MoICdidG4tY2xlYW4nICkuYXR0ciggJ3R5cGUnLCAnYnV0dG9uJyApLmF0dHIoICdkYXRhLWJvb2tzaGVsZi10eXBlJywgJ2RlbGV0ZScgKVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAuYXR0ciggJ2RhdGEtcGknLCBpdGVtLnBpICk7XG4gICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgIC8vIGJ1aWxkIGJvb2tzaGVsZiBpdGVtXG4gICAgICAgICAgICAgICAgICAgICAgICBkcm9wZG93bkxpc3RJdGVtTmFtZS5hcHBlbmQoIGRyb3Bkb3duTGlzdEl0ZW1OYW1lTGluayApO1xuICAgICAgICAgICAgICAgICAgICAgICAgZHJvcGRvd25MaXN0SXRlbUNvbExlZnQuYXBwZW5kKCBkcm9wZG93bkxpc3RJdGVtSW1hZ2UgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGRyb3Bkb3duTGlzdEl0ZW1Db2xDZW50ZXIuYXBwZW5kKCBkcm9wZG93bkxpc3RJdGVtTmFtZSApO1xuICAgICAgICAgICAgICAgICAgICAgICAgZHJvcGRvd25MaXN0SXRlbUNvbFJpZ2h0LmFwcGVuZCggZHJvcGRvd25MaXN0SXRlbURlbGV0ZSApO1xuICAgICAgICAgICAgICAgICAgICAgICAgZHJvcGRvd25MaXN0SXRlbVJvdy5hcHBlbmQoIGRyb3Bkb3duTGlzdEl0ZW1Db2xMZWZ0ICkuYXBwZW5kKCBkcm9wZG93bkxpc3RJdGVtQ29sQ2VudGVyICkuYXBwZW5kKCBkcm9wZG93bkxpc3RJdGVtQ29sUmlnaHQgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGRyb3Bkb3duTGlzdEl0ZW0uYXBwZW5kKCBkcm9wZG93bkxpc3RJdGVtUm93ICk7XG4gICAgICAgICAgICAgICAgICAgICAgICBkcm9wZG93bkxpc3QuYXBwZW5kKCBkcm9wZG93bkxpc3RJdGVtICk7XG4gICAgICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gcmVuZGVyIGNvbXBsZXRlIGxpc3RcbiAgICAgICAgICAgICQoICcuYm9va3NoZWxmLW5hdmlnYXRpb25fX2Ryb3Bkb3duLWxpc3QnICkuZW1wdHkoKS5hcHBlbmQoIGRyb3Bkb3duTGlzdCApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICAvLyByZW5kZXIgcmVzZXQgaWYgaXRlbXMgZXhpc3RcbiAgICAgICAgICAgIGlmICggZWxlbWVudHMuaXRlbXMubGVuZ3RoID4gMCApIHtcbiAgICAgICAgICAgICAgICAkKCAnLmJvb2tzaGVsZi1uYXZpZ2F0aW9uX19kcm9wZG93bi1saXN0LXJlc2V0JyApLmVtcHR5KCkuYXBwZW5kKCBkcm9wZG93bkxpc3RSZXNldCApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgJCggJy5ib29rc2hlbGYtbmF2aWdhdGlvbl9fZHJvcGRvd24nICkuaGlkZSgpO1xuICAgICAgICAgICAgICAgICQoICcuYm9va3NoZWxmLW5hdmlnYXRpb25fX2Ryb3Bkb3duLWxpc3QtcmVzZXQnICkuZW1wdHkoKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gZGVsZXRlIHNpbmdsZSBpdGVtXG4gICAgICAgICAgICAkKCAnW2RhdGEtYm9va3NoZWxmLXR5cGU9XCJkZWxldGVcIl0nICkub24oICdjbGljaycsIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgIHZhciBjdXJyQnRuID0gJCggdGhpcyApO1xuICAgICAgICAgICAgICAgIHZhciBjdXJyUGkgPSBjdXJyQnRuLmF0dHIoICdkYXRhLXBpJyApO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIF9kZWxldGVTZXNzaW9uRWxlbWVudCggX2RlZmF1bHRzLnJvb3QsIGN1cnJQaSApLnRoZW4oIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICBfc2V0U2Vzc2lvbkVsZW1lbnRDb3VudCgpO1xuICAgICAgICAgICAgICAgICAgICBfc2V0QWRkQWN0aXZlU3RhdGUoKTtcbiAgICAgICAgICAgICAgICAgICAgX3JlbmRlckRyb3Bkb3duTGlzdCgpO1xuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gZGVsZXRlIGFsbCBpdGVtc1xuICAgICAgICAgICAgJCggJ1tkYXRhLWJvb2tzaGVsZi10eXBlPVwicmVzZXRcIl0nICkub24oICdjbGljaycsIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgIGlmICggY29uZmlybSggX2RlZmF1bHRzLm1zZy5yZXNldEJvb2tzaGVsdmVzQ29uZmlybSApICkge1xuICAgICAgICAgICAgICAgICAgICBfZGVsZXRlQWxsU2Vzc2lvbkVsZW1lbnRzKCBfZGVmYXVsdHMucm9vdCApLnRoZW4oIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgbG9jYWxTdG9yYWdlLnNldEl0ZW0oICdjb25maXJtQ291bnRlcicsIDAgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIF9zZXRTZXNzaW9uRWxlbWVudENvdW50KCk7XG4gICAgICAgICAgICAgICAgICAgICAgICBfc2V0QWRkQWN0aXZlU3RhdGUoKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIF9yZW5kZXJEcm9wZG93bkxpc3QoKTtcbiAgICAgICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIGZhbHNlO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIFxuICAgICAgICB9ICkuZmFpbCggZnVuY3Rpb24oIGVycm9yICkge1xuICAgICAgICAgICAgY29uc29sZS5lcnJvciggJ0VSUk9SIC0gX2dldEFsbFNlc3Npb25FbGVtZW50czogJywgZXJyb3IucmVzcG9uc2VUZXh0ICk7XG4gICAgICAgIH0gKTtcbiAgICB9XG4gICAgLyoqXG4gICAgICogTWV0aG9kIHRvIHNldCB0aGUgYWN0aXZlIHN0YXRlIG9mIGFkZCBidXR0b25zICh1c2VyIG5vdCBsb2dnZWQgaW4pLlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX3NldEFkZEFjdGl2ZVN0YXRlXG4gICAgICovXG4gICAgZnVuY3Rpb24gX3NldEFkZEFjdGl2ZVN0YXRlKCkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfc2V0QWRkQWN0aXZlU3RhdGUoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICAkKCAnW2RhdGEtYm9va3NoZWxmLXR5cGU9XCJhZGRcIl0nICkuZWFjaCggZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICB2YXIgY3VyckJ0biA9ICQoIHRoaXMgKTtcbiAgICAgICAgICAgIHZhciBjdXJyUGkgPSBjdXJyQnRuLmF0dHIoICdkYXRhLXBpJyApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBfaXNFbGVtZW50U2V0KCBfZGVmYXVsdHMucm9vdCwgY3VyclBpICkudGhlbiggZnVuY3Rpb24oIGlzU2V0ICkge1xuICAgICAgICAgICAgICAgIGlmICggaXNTZXQgKSB7XG4gICAgICAgICAgICAgICAgICAgIGN1cnJCdG4uYWRkQ2xhc3MoICdhZGRlZCcgKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIGN1cnJCdG4ucmVtb3ZlQ2xhc3MoICdhZGRlZCcgKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9ICkuZmFpbCggZnVuY3Rpb24oIGVycm9yICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUuZXJyb3IoICdFUlJPUiAtIF9pc0VsZW1lbnRTZXQ6ICcsIGVycm9yLnJlc3BvbnNlVGV4dCApO1xuICAgICAgICAgICAgfSApO1xuICAgICAgICB9ICk7XG4gICAgfVxuICAgIFxuICAgIHJldHVybiB2aWV3ZXI7XG4gICAgXG59ICkoIHZpZXdlckpTIHx8IHt9LCBqUXVlcnkgKTtcblxuLy8gL3Jlc3QvYm9va3NoZWx2ZXMvc2Vzc2lvbi9hZGQve3BpfS97bG9naWR9L3twYWdlfVxuLy8gRsO8Z3QgZGVyIE1lcmtsaXN0ZSBlaW4gSXRlbSBtaXQgZGVyIGFuZ2VnZWJlbmVuIHBpLCBsb2dJZCB1bmQgU2VpdGVubnVtbWVyIGFuLiBEZXIgTmFtZVxuLy8gZGVzIEl0ZW1zIHdpcmQgYXV0b21hdGlzY2ggYXVzIGRlbSB6dXIgcGkgZ2Vow7ZyZW5kZW4gU09MUi1Eb2t1bWVudCBlcnN0ZWxsdFxuLy8gL3Jlc3QvYm9va3NoZWx2ZXMvc2Vzc2lvbi9kZWxldGUve3BpfS97bG9naWR9L3twYWdlfVxuLy8gTMO2c2NodCBkYXMgSXRlbSBtaXQgZGVyIGFuZ2VnZWJlbmVuIHBpLCBsb2dpZCB1bmQgU2VpdGVubnVtbWVyIGF1cyBkZXIgTWVya2xpc3RlLCB3ZW5uXG4vLyBlcyBlbnRoYWx0ZW4gaXN0XG4vLyAvcmVzdC9ib29rc2hlbHZlcy9zZXNzaW9uL2NvbnRhaW5zL3twaX0ve2xvZ2lkfS97cGFnZX1cbi8vIGdpYnQgXCJ0cnVlXCIgenVyw7xjaywgd2VubiBkaWUgTWVya2xpc3RlIGVpbiBJdGVtIG1pdCBkZXIgYW5nZWdlYmVuZW4gcGksIGxvZ2lkIHVuZFxuLy8gU2VpdGVubnVtbWVyIGVudGjDpGx0OyBzb25zdCBcImZhbHNlXCJcbi8vIC9yZXN0L2Jvb2tzaGVsdmVzL3Nlc3Npb24vY291bnRcbi8vIEdpYnQgZGllIFphaGwgZGVyIGluIGRlciBNZXJrbGlzdGUgZW50aGFsdGVuZW4gSXRlbXMgenVyw7xjay5cbiIsIi8qKlxuICogVGhpcyBmaWxlIGlzIHBhcnQgb2YgdGhlIEdvb2JpIHZpZXdlciAtIGEgY29udGVudCBwcmVzZW50YXRpb24gYW5kIG1hbmFnZW1lbnRcbiAqIGFwcGxpY2F0aW9uIGZvciBkaWdpdGl6ZWQgb2JqZWN0cy5cbiAqIFxuICogVmlzaXQgdGhlc2Ugd2Vic2l0ZXMgZm9yIG1vcmUgaW5mb3JtYXRpb24uIC0gaHR0cDovL3d3dy5pbnRyYW5kYS5jb20gLVxuICogaHR0cDovL2RpZ2l2ZXJzby5jb21cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGZyZWUgc29mdHdhcmU7IHlvdSBjYW4gcmVkaXN0cmlidXRlIGl0IGFuZC9vciBtb2RpZnkgaXQgdW5kZXIgdGhlIHRlcm1zXG4gKiBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYXMgcHVibGlzaGVkIGJ5IHRoZSBGcmVlIFNvZnR3YXJlIEZvdW5kYXRpb247IGVpdGhlclxuICogdmVyc2lvbiAyIG9mIHRoZSBMaWNlbnNlLCBvciAoYXQgeW91ciBvcHRpb24pIGFueSBsYXRlciB2ZXJzaW9uLlxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZGlzdHJpYnV0ZWQgaW4gdGhlIGhvcGUgdGhhdCBpdCB3aWxsIGJlIHVzZWZ1bCwgYnV0IFdJVEhPVVQgQU5ZXG4gKiBXQVJSQU5UWTsgd2l0aG91dCBldmVuIHRoZSBpbXBsaWVkIHdhcnJhbnR5IG9mIE1FUkNIQU5UQUJJTElUWSBvciBGSVRORVNTIEZPUiBBXG4gKiBQQVJUSUNVTEFSIFBVUlBPU0UuIFNlZSB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgZm9yIG1vcmUgZGV0YWlscy5cbiAqIFxuICogWW91IHNob3VsZCBoYXZlIHJlY2VpdmVkIGEgY29weSBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYWxvbmcgd2l0aCB0aGlzXG4gKiBwcm9ncmFtLiBJZiBub3QsIHNlZSA8aHR0cDovL3d3dy5nbnUub3JnL2xpY2Vuc2VzLz4uXG4gKiBcbiAqIE1vZHVsZSB0byBtYW5hZ2UgYm9va3NoZWx2ZXMgaWYgdGhlIHVzZXIgaXMgbG9nZ2VkIGluLlxuICogXG4gKiBAdmVyc2lvbiAzLjIuMFxuICogQG1vZHVsZSB2aWV3ZXJKUy5ib29rc2hlbHZlc1VzZXJcbiAqIEByZXF1aXJlcyBqUXVlcnlcbiAqL1xudmFyIHZpZXdlckpTID0gKCBmdW5jdGlvbiggdmlld2VyICkge1xuICAgICd1c2Ugc3RyaWN0JztcbiAgICBcbiAgICB2YXIgX2RlYnVnID0gZmFsc2U7XG4gICAgdmFyIF9kZWZhdWx0cyA9IHtcbiAgICAgICAgcm9vdDogJycsXG4gICAgICAgIG1zZzoge1xuICAgICAgICAgICAgcmVzZXRCb29rc2hlbHZlczogJycsXG4gICAgICAgICAgICByZXNldEJvb2tzaGVsdmVzQ29uZmlybTogJycsXG4gICAgICAgICAgICBub0l0ZW1zQXZhaWxhYmxlOiAnJyxcbiAgICAgICAgICAgIHNlbGVjdEJvb2tzaGVsZjogJycsXG4gICAgICAgICAgICBhZGROZXdCb29rc2hlbGY6ICcnXG4gICAgICAgIH1cbiAgICB9O1xuICAgIFxuICAgIHZpZXdlci5ib29rc2hlbHZlc1VzZXIgPSB7XG4gICAgICAgIGluaXQ6IGZ1bmN0aW9uKCBjb25maWcgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci5ib29rc2hlbHZlc1VzZXIuaW5pdCcgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci5ib29rc2hlbHZlc1VzZXIuaW5pdDogY29uZmlnIC0gJywgY29uZmlnICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgICQuZXh0ZW5kKCB0cnVlLCBfZGVmYXVsdHMsIGNvbmZpZyApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICAvLyByZW5kZXIgYm9va3NoZWxmIG5hdmlnYXRpb24gbGlzdFxuICAgICAgICAgICAgX3JlbmRlckJvb2tzaGVsZk5hdmlnYXRpb25MaXN0KCk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIHRvZ2dsZSBib29rc2hlbGYgZHJvcGRvd25cbiAgICAgICAgICAgICQoICdbZGF0YS1ib29rc2hlbGYtdHlwZT1cImRyb3Bkb3duXCJdJyApLm9mZigpLm9uKCAnY2xpY2snLCBmdW5jdGlvbiggZXZlbnQgKSB7XG4gICAgICAgICAgICAgICAgZXZlbnQuc3RvcFByb3BhZ2F0aW9uKCk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLy8gaGlkZSBvdGhlciBkcm9wZG93bnNcbiAgICAgICAgICAgICAgICAkKCAnLmxvZ2luLW5hdmlnYXRpb25fX2xvZ2luLWRyb3Bkb3duLCAubG9naW4tbmF2aWdhdGlvbl9fdXNlci1kcm9wZG93biwgLm5hdmlnYXRpb25fX2NvbGxlY3Rpb24tcGFuZWwnICkuaGlkZSgpO1xuICAgICAgICAgICAgICAgICQoICcuYm9va3NoZWxmLXBvcHVwJyApLnJlbW92ZSgpO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICQoICcuYm9va3NoZWxmLW5hdmlnYXRpb25fX2Ryb3Bkb3duJyApLnNsaWRlVG9nZ2xlKCAnZmFzdCcgKTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gY2hlY2sgaWYgZWxlbWVudCBpcyBpbiBhbnkgYm9va3NoZWxmXG4gICAgICAgICAgICBfc2V0QWRkZWRTdGF0dXMoKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gcmVuZGVyIGJvb2tzaGVsZiBwb3B1cFxuICAgICAgICAgICAgJCggJ1tkYXRhLWJvb2tzaGVsZi10eXBlPVwiYWRkXCJdJyApLm9mZigpLm9uKCAnY2xpY2snLCBmdW5jdGlvbiggZXZlbnQgKSB7XG4gICAgICAgICAgICAgICAgZXZlbnQuc3RvcFByb3BhZ2F0aW9uKCk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLy8gaGlkZSBvdGhlciBkcm9wZG93bnNcbiAgICAgICAgICAgICAgICAkKCAnLmJvb2tzaGVsZi1uYXZpZ2F0aW9uX19kcm9wZG93biwgLmxvZ2luLW5hdmlnYXRpb25fX3VzZXItZHJvcGRvd24nICkuaGlkZSgpO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIHZhciBjdXJyQnRuID0gJCggdGhpcyApO1xuICAgICAgICAgICAgICAgIHZhciBjdXJyUGkgPSBjdXJyQnRuLmF0dHIoICdkYXRhLXBpJyApO1xuICAgICAgICAgICAgICAgIHZhciBjdXJyTG9naWQgPSBjdXJyQnRuLmF0dHIoICdkYXRhLWxvZ2lkJyApO1xuICAgICAgICAgICAgICAgIHZhciBjdXJyUGFnZSA9IGN1cnJCdG4uYXR0ciggJ2RhdGEtcGFnZScgKTtcbiAgICAgICAgICAgICAgICB2YXIgY3VyclBvcyA9IGN1cnJCdG4ub2Zmc2V0KCk7XG4gICAgICAgICAgICAgICAgdmFyIGN1cnJTaXplID0ge1xuICAgICAgICAgICAgICAgICAgICB3aWR0aDogY3VyckJ0bi5vdXRlcldpZHRoKCksXG4gICAgICAgICAgICAgICAgICAgIGhlaWdodDogY3VyckJ0bi5vdXRlckhlaWdodCgpXG4gICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAvLyByZW5kZXIgYm9va3NoZWxmIHBvcHVwXG4gICAgICAgICAgICAgICAgX3JlbmRlckJvb2tzaGVsZlBvcHVwKCBjdXJyUGksIGN1cnJMb2dpZCwgY3VyclBhZ2UsIGN1cnJQb3MsIGN1cnJTaXplICk7XG4gICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIGhpZGUgbWVudXMvcG9wdXBzIGJ5IGNsaWNraW5nIG9uIGJvZHlcbiAgICAgICAgICAgICQoICdib2R5JyApLm9uKCAnY2xpY2snLCBmdW5jdGlvbiggZXZlbnQgKSB7XG4gICAgICAgICAgICAgICAgJCggJy5ib29rc2hlbGYtbmF2aWdhdGlvbl9fZHJvcGRvd24nICkuaGlkZSgpO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIGlmICggJCggJy5ib29rc2hlbGYtcG9wdXAnICkubGVuZ3RoID4gMCApIHtcbiAgICAgICAgICAgICAgICAgICAgdmFyIHRhcmdldCA9ICQoIGV2ZW50LnRhcmdldCApO1xuICAgICAgICAgICAgICAgICAgICB2YXIgcG9wdXAgPSAkKCAnLmJvb2tzaGVsZi1wb3B1cCcgKTtcbiAgICAgICAgICAgICAgICAgICAgdmFyIHBvcHVwQ2hpbGQgPSBwb3B1cC5maW5kKCAnKicgKTtcbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgIGlmICggIXRhcmdldC5pcyggcG9wdXAgKSAmJiAhdGFyZ2V0LmlzKCBwb3B1cENoaWxkICkgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAkKCAnLmJvb2tzaGVsZi1wb3B1cCcgKS5yZW1vdmUoKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gYWRkIG5ldyBib29rc2hlbGYgaW4gb3ZlcnZpZXdcbiAgICAgICAgICAgICQoICcjYWRkQm9va3NoZWxmQnRuJyApLm9mZigpLm9uKCAnY2xpY2snLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICB2YXIgYnNOYW1lID0gJCggJyNhZGRCb29rc2hlbGZJbnB1dCcgKS52YWwoKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICBpZiAoIGJzTmFtZSAhPSAnJyApIHtcbiAgICAgICAgICAgICAgICAgICAgX2FkZE5hbWVkQm9va3NoZWxmKCBfZGVmYXVsdHMucm9vdCwgYnNOYW1lICkudGhlbiggZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBsb2NhdGlvbi5yZWxvYWQoKTtcbiAgICAgICAgICAgICAgICAgICAgfSApLmZhaWwoIGZ1bmN0aW9uKCBlcnJvciApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGNvbnNvbGUuZXJyb3IoICdFUlJPUiAtIF9hZGROYW1lZEJvb2tzaGVsZjogJywgZXJyb3IucmVzcG9uc2VUZXh0ICk7XG4gICAgICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIF9hZGRBdXRvbWF0aWNOYW1lZEJvb2tzaGVsZiggX2RlZmF1bHRzLnJvb3QgKS50aGVuKCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGxvY2F0aW9uLnJlbG9hZCgpO1xuICAgICAgICAgICAgICAgICAgICB9ICkuZmFpbCggZnVuY3Rpb24oIGVycm9yICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgY29uc29sZS5lcnJvciggJ0VSUk9SIC0gX2FkZEF1dG9tYXRpY05hbWVkQm9va3NoZWxmOiAnLCBlcnJvci5yZXNwb25zZVRleHQgKTtcbiAgICAgICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gYWRkIG5ldyBib29rc2hlbGYgb24gZW50ZXIgaW4gb3ZlcnZpZXdcbiAgICAgICAgICAgICQoICcjYWRkQm9va3NoZWxmSW5wdXQnICkub24oICdrZXl1cCcsIGZ1bmN0aW9uKCBldmVudCApIHtcbiAgICAgICAgICAgICAgICBpZiAoIGV2ZW50LndoaWNoID09IDEzICkge1xuICAgICAgICAgICAgICAgICAgICAkKCAnI2FkZEJvb2tzaGVsZkJ0bicgKS5jbGljaygpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgfVxuICAgIH07XG4gICAgLyogIyMjIyMjIyMgQUREIChDUkVBVEUpICMjIyMjIyMjICovXG4gICAgLyoqXG4gICAgICogTWV0aG9kIHRvIGFkZCBhbiBpdGVtIHRvIHRoZSB1c2VyIGJvb2tzaGVsZiBieSBQSS5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9hZGRCb29rc2hlbGZJdGVtQnlQaVxuICAgICAqIEBwYXJhbSB7U3RyaW5nfSByb290IFRoZSBhcHBsaWNhdGlvbiByb290IHBhdGguXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IGlkIFRoZSBjdXJyZW50IGJvb2tzaGVsZiBpZC5cbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gcGkgVGhlIHBpIG9mIHRoZSBpdGVtIHRvIGFkZC5cbiAgICAgKiBAcmV0dXJucyB7UHJvbWlzZX0gQSBwcm9taXNlIHRoYXQgY2hlY2tzIHRoZSBleGlzdGluZyBpdGVtcy5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfYWRkQm9va3NoZWxmSXRlbUJ5UGkoIHJvb3QsIGlkLCBwaSApIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX2FkZEJvb2tzaGVsZkl0ZW1CeVBpKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2FkZEJvb2tzaGVsZkl0ZW1CeVBpOiByb290IC0gJywgcm9vdCApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfYWRkQm9va3NoZWxmSXRlbUJ5UGk6IGlkIC0gJywgaWQgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2FkZEJvb2tzaGVsZkl0ZW1CeVBpOiBwaSAtICcsIHBpICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIHZhciBwcm9taXNlID0gUSggJC5hamF4KCB7XG4gICAgICAgICAgICB1cmw6IHJvb3QgKyAnL3Jlc3QvYm9va3NoZWx2ZXMvdXNlci9nZXQvJyArIGlkICsgJy9hZGQvJyArIHBpICsgJy8nLFxuICAgICAgICAgICAgdHlwZTogXCJHRVRcIixcbiAgICAgICAgICAgIGRhdGFUeXBlOiBcIkpTT05cIixcbiAgICAgICAgICAgIGFzeW5jOiB0cnVlXG4gICAgICAgIH0gKSApO1xuICAgICAgICBcbiAgICAgICAgcmV0dXJuIHByb21pc2U7XG4gICAgfVxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB0byBhZGQgYW4gaXRlbSB3aXRoIFBJLCBMT0dJRCBhbmQgUEFHRSB0byB0aGUgdXNlciBib29rc2hlbGYuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfYWRkQm9va3NoZWxmSXRlbUJ5UGlMb2dpZFBhZ2VcbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gcm9vdCBUaGUgYXBwbGljYXRpb24gcm9vdCBwYXRoLlxuICAgICAqIEBwYXJhbSB7U3RyaW5nfSBpZCBUaGUgY3VycmVudCBib29rc2hlbGYgaWQuXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IHBpIFRoZSBwaSBvZiB0aGUgaXRlbSB0byBhZGQuXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IGxvZ2lkIFRoZSBsb2dpZCBvZiB0aGUgaXRlbSB0byBhZGQuXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IHBhZ2UgVGhlIHBhZ2Ugb2YgdGhlIGl0ZW0gdG8gYWRkLlxuICAgICAqIEByZXR1cm5zIHtQcm9taXNlfSBBIHByb21pc2UgdGhhdCBjaGVja3MgdGhlIGV4aXN0aW5nIGl0ZW1zLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9hZGRCb29rc2hlbGZJdGVtQnlQaUxvZ2lkUGFnZSggcm9vdCwgaWQsIHBpLCBsb2dpZCwgcGFnZSApIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX2FkZEJvb2tzaGVsZkl0ZW1CeVBpTG9naWRQYWdlKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2FkZEJvb2tzaGVsZkl0ZW1CeVBpTG9naWRQYWdlOiByb290IC0gJywgcm9vdCApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfYWRkQm9va3NoZWxmSXRlbUJ5UGlMb2dpZFBhZ2U6IGlkIC0gJywgaWQgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2FkZEJvb2tzaGVsZkl0ZW1CeVBpTG9naWRQYWdlOiBwaSAtICcsIHBpICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19hZGRCb29rc2hlbGZJdGVtQnlQaUxvZ2lkUGFnZTogbG9naWQgLSAnLCBsb2dpZCApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfYWRkQm9va3NoZWxmSXRlbUJ5UGlMb2dpZFBhZ2U6IHBhZ2UgLSAnLCBwYWdlICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIHZhciBwcm9taXNlID0gUSggJC5hamF4KCB7XG4gICAgICAgICAgICB1cmw6IHJvb3QgKyAnL3Jlc3QvYm9va3NoZWx2ZXMvdXNlci9nZXQvJyArIGlkICsgJy9hZGQvJyArIHBpICsgJy8nICsgbG9naWQgKyAnLycgKyBwYWdlICsgJy8nLFxuICAgICAgICAgICAgdHlwZTogXCJHRVRcIixcbiAgICAgICAgICAgIGRhdGFUeXBlOiBcIkpTT05cIixcbiAgICAgICAgICAgIGFzeW5jOiB0cnVlXG4gICAgICAgIH0gKSApO1xuICAgICAgICBcbiAgICAgICAgcmV0dXJuIHByb21pc2U7XG4gICAgfVxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB0byBhZGQgYSBuYW1lZCBib29rc2hlbGYgdG8gdGhlIHVzZXIgYWNjb3VudC5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9hZGROYW1lZEJvb2tzaGVsZlxuICAgICAqIEBwYXJhbSB7U3RyaW5nfSByb290IFRoZSBhcHBsaWNhdGlvbiByb290IHBhdGguXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IG5hbWUgVGhlIG5hbWUgb2YgdGhlIGJvb2tzaGVsZi5cbiAgICAgKiBAcmV0dXJucyB7UHJvbWlzZX0gQSBwcm9taXNlIHRoYXQgY2hlY2tzIHRoZSBleGlzdGluZyBpdGVtcy5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfYWRkTmFtZWRCb29rc2hlbGYoIHJvb3QsIG5hbWUgKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICctLS0tLS0tLS0tIF9hZGROYW1lZEJvb2tzaGVsZigpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19hZGROYW1lZEJvb2tzaGVsZjogcm9vdCAtICcsIHJvb3QgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2FkZE5hbWVkQm9va3NoZWxmOiBuYW1lIC0gJywgbmFtZSApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgcHJvbWlzZSA9IFEoICQuYWpheCgge1xuICAgICAgICAgICAgdXJsOiByb290ICsgJy9yZXN0L2Jvb2tzaGVsdmVzL3VzZXIvYWRkLycgKyBuYW1lICsgJy8nLFxuICAgICAgICAgICAgdHlwZTogXCJHRVRcIixcbiAgICAgICAgICAgIGRhdGFUeXBlOiBcIkpTT05cIixcbiAgICAgICAgICAgIGFzeW5jOiB0cnVlXG4gICAgICAgIH0gKSApO1xuICAgICAgICBcbiAgICAgICAgcmV0dXJuIHByb21pc2U7XG4gICAgfVxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB0byBhZGQgYSBhdXRvbWF0aWMgbmFtZWQgYm9va3NoZWxmIHRvIHRoZSB1c2VyIGFjY291bnQuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfYWRkQXV0b21hdGljTmFtZWRCb29rc2hlbGZcbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gcm9vdCBUaGUgYXBwbGljYXRpb24gcm9vdCBwYXRoLlxuICAgICAqIEByZXR1cm5zIHtQcm9taXNlfSBBIHByb21pc2UgdGhhdCBjaGVja3MgdGhlIGV4aXN0aW5nIGl0ZW1zLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9hZGRBdXRvbWF0aWNOYW1lZEJvb2tzaGVsZiggcm9vdCApIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX2FkZEF1dG9tYXRpY05hbWVkQm9va3NoZWxmKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2FkZEF1dG9tYXRpY05hbWVkQm9va3NoZWxmOiByb290IC0gJywgcm9vdCApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgcHJvbWlzZSA9IFEoICQuYWpheCgge1xuICAgICAgICAgICAgdXJsOiByb290ICsgJy9yZXN0L2Jvb2tzaGVsdmVzL3VzZXIvYWRkLycsXG4gICAgICAgICAgICB0eXBlOiBcIkdFVFwiLFxuICAgICAgICAgICAgZGF0YVR5cGU6IFwiSlNPTlwiLFxuICAgICAgICAgICAgYXN5bmM6IHRydWVcbiAgICAgICAgfSApICk7XG4gICAgICAgIFxuICAgICAgICByZXR1cm4gcHJvbWlzZTtcbiAgICB9XG4gICAgLyoqXG4gICAgICogTWV0aG9kIHRvIGFkZCB0aGUgc2Vzc2lvbiBib29rc2hlbGYgdG8gdGhlIHVzZXIgYWNjb3VudCB3aXRoIGFuIGF1dG9tYXRpYyBnZW5lcmF0ZWRcbiAgICAgKiBuYW1lLlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX2FkZFNlc3Npb25Cb29rc2hlbGZcbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gcm9vdCBUaGUgYXBwbGljYXRpb24gcm9vdCBwYXRoLlxuICAgICAqIEByZXR1cm5zIHtQcm9taXNlfSBBIHByb21pc2UgdGhhdCBjaGVja3MgdGhlIGV4aXN0aW5nIGl0ZW1zLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9hZGRTZXNzaW9uQm9va3NoZWxmKCByb290ICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfYWRkU2Vzc2lvbkJvb2tzaGVsZigpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19hZGRTZXNzaW9uQm9va3NoZWxmOiByb290IC0gJywgcm9vdCApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgcHJvbWlzZSA9IFEoICQuYWpheCgge1xuICAgICAgICAgICAgdXJsOiByb290ICsgJy9yZXN0L2Jvb2tzaGVsdmVzL3VzZXIvYWRkU2Vzc2lvbkJvb2tzaGVsZi8nLFxuICAgICAgICAgICAgdHlwZTogXCJHRVRcIixcbiAgICAgICAgICAgIGRhdGFUeXBlOiBcIkpTT05cIixcbiAgICAgICAgICAgIGFzeW5jOiB0cnVlXG4gICAgICAgIH0gKSApO1xuICAgICAgICBcbiAgICAgICAgcmV0dXJuIHByb21pc2U7XG4gICAgfVxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB0byBhZGQgdGhlIHNlc3Npb24gYm9va3NoZWxmIHRvIHRoZSB1c2VyIGFjY291bnQgd2l0aCBhIGdpdmVuIG5hbWUuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfYWRkU2Vzc2lvbkJvb2tzaGVsZk5hbWVkXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IHJvb3QgVGhlIGFwcGxpY2F0aW9uIHJvb3QgcGF0aC5cbiAgICAgKiBAcmV0dXJucyB7UHJvbWlzZX0gQSBwcm9taXNlIHRoYXQgY2hlY2tzIHRoZSBleGlzdGluZyBpdGVtcy5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfYWRkU2Vzc2lvbkJvb2tzaGVsZk5hbWVkKCByb290LCBuYW1lICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfYWRkU2Vzc2lvbkJvb2tzaGVsZk5hbWVkKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2FkZFNlc3Npb25Cb29rc2hlbGZOYW1lZDogcm9vdCAtICcsIHJvb3QgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2FkZFNlc3Npb25Cb29rc2hlbGZOYW1lZDogbmFtZSAtICcsIG5hbWUgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgdmFyIHByb21pc2UgPSBRKCAkLmFqYXgoIHtcbiAgICAgICAgICAgIHVybDogcm9vdCArICcvcmVzdC9ib29rc2hlbHZlcy91c2VyL2FkZFNlc3Npb25Cb29rc2hlbGYvJyArIG5hbWUgKyAnLycsXG4gICAgICAgICAgICB0eXBlOiBcIkdFVFwiLFxuICAgICAgICAgICAgZGF0YVR5cGU6IFwiSlNPTlwiLFxuICAgICAgICAgICAgYXN5bmM6IHRydWVcbiAgICAgICAgfSApICk7XG4gICAgICAgIFxuICAgICAgICByZXR1cm4gcHJvbWlzZTtcbiAgICB9XG4gICAgLyogIyMjIyMjIyMgR0VUIChSRUFEKSAjIyMjIyMjIyAqL1xuICAgIC8qKlxuICAgICAqIE1ldGhvZCB0byBnZXQgYWxsIHVzZXIgYm9va3NoZWx2ZXMuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfZ2V0QWxsQm9va3NoZWx2ZXNcbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gcm9vdCBUaGUgYXBwbGljYXRpb24gcm9vdCBwYXRoLlxuICAgICAqIEByZXR1cm5zIHtQcm9taXNlfSBBIHByb21pc2UgdGhhdCBjaGVja3MgdGhlIGV4aXN0aW5nIGl0ZW1zLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9nZXRBbGxCb29rc2hlbHZlcyggcm9vdCApIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX2dldEFsbEJvb2tzaGVsdmVzKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2dldEFsbEJvb2tzaGVsdmVzOiByb290IC0gJywgcm9vdCApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgcHJvbWlzZSA9IFEoICQuYWpheCgge1xuICAgICAgICAgICAgdXJsOiByb290ICsgJy9yZXN0L2Jvb2tzaGVsdmVzL3VzZXIvZ2V0LycsXG4gICAgICAgICAgICB0eXBlOiBcIkdFVFwiLFxuICAgICAgICAgICAgZGF0YVR5cGU6IFwiSlNPTlwiLFxuICAgICAgICAgICAgYXN5bmM6IHRydWVcbiAgICAgICAgfSApICk7XG4gICAgICAgIFxuICAgICAgICByZXR1cm4gcHJvbWlzZTtcbiAgICB9XG4gICAgLyoqXG4gICAgICogTWV0aG9kIHRvIGdldCBhIGJvb2tzaGVsZiBieSBpZC5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9nZXRCb29rc2hlbGZCeUlkXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IHJvb3QgVGhlIGFwcGxpY2F0aW9uIHJvb3QgcGF0aC5cbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gaWQgVGhlIGN1cnJlbnQgYm9va3NoZWxmIGlkLlxuICAgICAqIEByZXR1cm5zIHtQcm9taXNlfSBBIHByb21pc2UgdGhhdCBjaGVja3MgdGhlIGV4aXN0aW5nIGl0ZW1zLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9nZXRCb29rc2hlbGZCeUlkKCByb290LCBpZCApIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX2dldEJvb2tzaGVsZkJ5SWQoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfZ2V0Qm9va3NoZWxmQnlJZDogcm9vdCAtICcsIHJvb3QgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2dldEJvb2tzaGVsZkJ5SWQ6IGlkIC0gJywgaWQgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgdmFyIHByb21pc2UgPSBRKCAkLmFqYXgoIHtcbiAgICAgICAgICAgIHVybDogcm9vdCArICcvcmVzdC9ib29rc2hlbHZlcy91c2VyL2dldC8nICsgaWQgKyAnLycsXG4gICAgICAgICAgICB0eXBlOiBcIkdFVFwiLFxuICAgICAgICAgICAgZGF0YVR5cGU6IFwiSlNPTlwiLFxuICAgICAgICAgICAgYXN5bmM6IHRydWVcbiAgICAgICAgfSApICk7XG4gICAgICAgIFxuICAgICAgICByZXR1cm4gcHJvbWlzZTtcbiAgICB9XG4gICAgLyoqXG4gICAgICogTWV0aG9kIHRvIGdldCB0aGUgbnVtYmVyIG9mIGl0ZW1zIGluIHRoZSBzZWxlY3RlZCBib29rc2hlbGYuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfZ2V0Qm9va3NoZWxmSXRlbUNvdW50XG4gICAgICogQHBhcmFtIHtTdHJpbmd9IHJvb3QgVGhlIGFwcGxpY2F0aW9uIHJvb3QgcGF0aC5cbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gaWQgVGhlIGN1cnJlbnQgYm9va3NoZWxmIGlkLlxuICAgICAqIEByZXR1cm5zIHtQcm9taXNlfSBBIHByb21pc2UgdGhhdCBjaGVja3MgdGhlIGV4aXN0aW5nIGl0ZW1zLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9nZXRCb29rc2hlbGZJdGVtQ291bnQoIHJvb3QsIGlkICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfZ2V0Qm9va3NoZWxmSXRlbUNvdW50KCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2dldEJvb2tzaGVsZkl0ZW1Db3VudDogcm9vdCAtICcsIHJvb3QgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2dldEJvb2tzaGVsZkl0ZW1Db3VudDogaWQgLSAnLCBpZCApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgcHJvbWlzZSA9IFEoICQuYWpheCgge1xuICAgICAgICAgICAgdXJsOiByb290ICsgJy9yZXN0L2Jvb2tzaGVsdmVzL3VzZXIvZ2V0LycgKyBpZCArICcvY291bnQvJyxcbiAgICAgICAgICAgIHR5cGU6IFwiR0VUXCIsXG4gICAgICAgICAgICBkYXRhVHlwZTogXCJKU09OXCIsXG4gICAgICAgICAgICBhc3luYzogdHJ1ZVxuICAgICAgICB9ICkgKTtcbiAgICAgICAgXG4gICAgICAgIHJldHVybiBwcm9taXNlO1xuICAgIH1cbiAgICAvKipcbiAgICAgKiBNZXRob2QgdG8gZ2V0IGFsbCBwdWJsaWMgYm9va3NoZWx2ZXMuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfZ2V0UHVibGljQm9va3NoZWx2ZXNcbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gcm9vdCBUaGUgYXBwbGljYXRpb24gcm9vdCBwYXRoLlxuICAgICAqIEByZXR1cm5zIHtQcm9taXNlfSBBIHByb21pc2UgdGhhdCBjaGVja3MgdGhlIGV4aXN0aW5nIGl0ZW1zLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9nZXRQdWJsaWNCb29rc2hlbHZlcyggcm9vdCApIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX2dldFB1YmxpY0Jvb2tzaGVsdmVzKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2dldFB1YmxpY0Jvb2tzaGVsdmVzOiByb290IC0gJywgcm9vdCApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgcHJvbWlzZSA9IFEoICQuYWpheCgge1xuICAgICAgICAgICAgdXJsOiByb290ICsgJy9yZXN0L2Jvb2tzaGVsdmVzL3B1YmxpYy9nZXQvJyxcbiAgICAgICAgICAgIHR5cGU6IFwiR0VUXCIsXG4gICAgICAgICAgICBkYXRhVHlwZTogXCJKU09OXCIsXG4gICAgICAgICAgICBhc3luYzogdHJ1ZVxuICAgICAgICB9ICkgKTtcbiAgICAgICAgXG4gICAgICAgIHJldHVybiBwcm9taXNlO1xuICAgIH1cbiAgICAvKipcbiAgICAgKiBNZXRob2QgdG8gZ2V0IGFsbCBzaGFyZWQgYm9va3NoZWx2ZXMuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfZ2V0U2hhcmVkQm9va3NoZWx2ZXNcbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gcm9vdCBUaGUgYXBwbGljYXRpb24gcm9vdCBwYXRoLlxuICAgICAqIEByZXR1cm5zIHtQcm9taXNlfSBBIHByb21pc2UgdGhhdCBjaGVja3MgdGhlIGV4aXN0aW5nIGl0ZW1zLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9nZXRTaGFyZWRCb29rc2hlbHZlcyggcm9vdCApIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX2dldFNoYXJlZEJvb2tzaGVsdmVzKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2dldFNoYXJlZEJvb2tzaGVsdmVzOiByb290IC0gJywgcm9vdCApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgcHJvbWlzZSA9IFEoICQuYWpheCgge1xuICAgICAgICAgICAgdXJsOiByb290ICsgJy9yZXN0L2Jvb2tzaGVsdmVzL3NoYXJlZC9nZXQvJyxcbiAgICAgICAgICAgIHR5cGU6IFwiR0VUXCIsXG4gICAgICAgICAgICBkYXRhVHlwZTogXCJKU09OXCIsXG4gICAgICAgICAgICBhc3luYzogdHJ1ZVxuICAgICAgICB9ICkgKTtcbiAgICAgICAgXG4gICAgICAgIHJldHVybiBwcm9taXNlO1xuICAgIH1cbiAgICAvKipcbiAgICAgKiBNZXRob2QgdG8gY2hlY2sgaWYgYW4gaXRlbSBieSBQSSBpZiBpdCdzIGluIHVzZXIgYm9va3NoZWxmLiBJdCByZXR1cm5zIHRoZVxuICAgICAqIGJvb2tzaGVsdmVzIG9yIGZhbHNlIGlmIG5vIGl0ZW1zIGFyZSBpbiBsaXN0LlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX2dldENvbnRhaW5pbmdCb29rc2hlbGZJdGVtQnlQaVxuICAgICAqIEBwYXJhbSB7U3RyaW5nfSByb290IFRoZSBhcHBsaWNhdGlvbiByb290IHBhdGguXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IHBpIFRoZSBwaSBvZiB0aGUgY3VycmVudCBpdGVtLlxuICAgICAqIEByZXR1cm5zIHtQcm9taXNlfSBBIHByb21pc2UgdGhhdCBjaGVja3MgdGhlIGV4aXN0aW5nIGl0ZW1zLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9nZXRDb250YWluaW5nQm9va3NoZWxmSXRlbUJ5UGkoIHJvb3QsIHBpICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfZ2V0Q29udGFpbmluZ0Jvb2tzaGVsZkl0ZW1CeVBpKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2dldENvbnRhaW5pbmdCb29rc2hlbGZJdGVtQnlQaTogcm9vdCAtICcsIHJvb3QgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2dldENvbnRhaW5pbmdCb29rc2hlbGZJdGVtQnlQaTogcGkgLSAnLCBwaSApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgcHJvbWlzZSA9IFEoICQuYWpheCgge1xuICAgICAgICAgICAgdXJsOiByb290ICsgJy9yZXN0L2Jvb2tzaGVsdmVzL3VzZXIvY29udGFpbnMvJyArIHBpICsgJy8nLFxuICAgICAgICAgICAgdHlwZTogXCJHRVRcIixcbiAgICAgICAgICAgIGRhdGFUeXBlOiBcIkpTT05cIixcbiAgICAgICAgICAgIGFzeW5jOiB0cnVlXG4gICAgICAgIH0gKSApO1xuICAgICAgICBcbiAgICAgICAgcmV0dXJuIHByb21pc2U7XG4gICAgfVxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB0byBjaGVjayBpZiBhbiBpdGVtIGJ5IFBJLCBMT0dJRCBhbmQgUEFHRSBpZiBpdCdzIGluIHVzZXIgYm9va3NoZWxmLiBJdFxuICAgICAqIHJldHVybnMgdGhlIGJvb2tzaGVsdmVzIG9yIGZhbHNlIGlmIG5vIGl0ZW1zIGFyZSBpbiBsaXN0LlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX2dldENvbnRhaW5pbmdCb29rc2hlbGZJdGVtQnlQaUxvZ2lkUGFnZVxuICAgICAqIEBwYXJhbSB7U3RyaW5nfSByb290IFRoZSBhcHBsaWNhdGlvbiByb290IHBhdGguXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IHBpIFRoZSBwaSBvZiB0aGUgY3VycmVudCBpdGVtLlxuICAgICAqIEByZXR1cm5zIHtQcm9taXNlfSBBIHByb21pc2UgdGhhdCBjaGVja3MgdGhlIGV4aXN0aW5nIGl0ZW1zLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9nZXRDb250YWluaW5nQm9va3NoZWxmSXRlbUJ5UGlMb2dpZFBhZ2UoIHJvb3QsIHBpLCBsb2dpZCwgcGFnZSApIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX2dldENvbnRhaW5pbmdCb29rc2hlbGZJdGVtQnlQaUxvZ2lkUGFnZSgpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19nZXRDb250YWluaW5nQm9va3NoZWxmSXRlbUJ5UGlMb2dpZFBhZ2U6IHJvb3QgLSAnLCByb290ICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19nZXRDb250YWluaW5nQm9va3NoZWxmSXRlbUJ5UGlMb2dpZFBhZ2U6IHBpIC0gJywgcGkgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2dldENvbnRhaW5pbmdCb29rc2hlbGZJdGVtQnlQaUxvZ2lkUGFnZTogbG9naWQgLSAnLCBsb2dpZCApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfZ2V0Q29udGFpbmluZ0Jvb2tzaGVsZkl0ZW1CeVBpTG9naWRQYWdlOiBwYWdlIC0gJywgcGFnZSApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgcHJvbWlzZSA9IFEoICQuYWpheCgge1xuICAgICAgICAgICAgdXJsOiByb290ICsgJy9yZXN0L2Jvb2tzaGVsdmVzL3VzZXIvY29udGFpbnMvJyArIHBpICsgJy8nICsgbG9naWQgKyAnLycgKyBwYWdlICsgJy8nLFxuICAgICAgICAgICAgdHlwZTogXCJHRVRcIixcbiAgICAgICAgICAgIGRhdGFUeXBlOiBcIkpTT05cIixcbiAgICAgICAgICAgIGFzeW5jOiB0cnVlXG4gICAgICAgIH0gKSApO1xuICAgICAgICBcbiAgICAgICAgcmV0dXJuIHByb21pc2U7XG4gICAgfVxuICAgIC8qICMjIyMjIyMjIFNFVCAoVVBEQVRFKSAjIyMjIyMjIyAqL1xuICAgIC8qKlxuICAgICAqIE1ldGhvZCB0byBzZXQgdGhlIG5hbWUgb2YgYSBib29rc2hlbGYuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfc2V0Qm9va3NoZWxmTmFtZVxuICAgICAqIEBwYXJhbSB7U3RyaW5nfSByb290IFRoZSBhcHBsaWNhdGlvbiByb290IHBhdGguXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IGlkIFRoZSBjdXJyZW50IGJvb2tzaGVsZiBpZC5cbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gbmFtZSBUaGUgbmFtZSBvZiB0aGUgYm9va3NoZWxmLlxuICAgICAqIEByZXR1cm5zIHtQcm9taXNlfSBBIHByb21pc2UgdGhhdCBjaGVja3MgdGhlIGV4aXN0aW5nIGl0ZW1zLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9zZXRCb29rc2hlbGZOYW1lKCByb290LCBpZCwgbmFtZSApIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX3NldEJvb2tzaGVsZk5hbWUoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfc2V0Qm9va3NoZWxmTmFtZTogcm9vdCAtICcsIHJvb3QgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX3NldEJvb2tzaGVsZk5hbWU6IGlkIC0gJywgaWQgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX3NldEJvb2tzaGVsZk5hbWU6IG5hbWUgLSAnLCBuYW1lICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIHZhciBwcm9taXNlID0gUSggJC5hamF4KCB7XG4gICAgICAgICAgICB1cmw6IHJvb3QgKyAnL3Jlc3QvYm9va3NoZWx2ZXMvdXNlci9nZXQvJyArIGlkICsgJy9zZXQvbmFtZS8nICsgbmFtZSArICcvJyxcbiAgICAgICAgICAgIHR5cGU6IFwiR0VUXCIsXG4gICAgICAgICAgICBkYXRhVHlwZTogXCJKU09OXCIsXG4gICAgICAgICAgICBhc3luYzogdHJ1ZVxuICAgICAgICB9ICkgKTtcbiAgICAgICAgXG4gICAgICAgIHJldHVybiBwcm9taXNlO1xuICAgIH1cbiAgICAvKiAjIyMjIyMjIyBERUxFVEUgIyMjIyMjIyMgKi9cbiAgICAvKipcbiAgICAgKiBNZXRob2QgdG8gZGVsZXRlIGFuIGl0ZW0gZnJvbSB0aGUgdXNlciBib29rc2hlbGYgYnkgUEkuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfZGVsZXRlQm9va3NoZWxmSXRlbUJ5UGlcbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gcm9vdCBUaGUgYXBwbGljYXRpb24gcm9vdCBwYXRoLlxuICAgICAqIEBwYXJhbSB7U3RyaW5nfSBpZCBUaGUgY3VycmVudCBib29rc2hlbGYgaWQuXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IHBpIFRoZSBwaSBvZiB0aGUgaXRlbSB0byBhZGQuXG4gICAgICogQHJldHVybnMge1Byb21pc2V9IEEgcHJvbWlzZSB0aGF0IGNoZWNrcyB0aGUgZXhpc3RpbmcgaXRlbXMuXG4gICAgICovXG4gICAgZnVuY3Rpb24gX2RlbGV0ZUJvb2tzaGVsZkl0ZW1CeVBpKCByb290LCBpZCwgcGkgKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICctLS0tLS0tLS0tIF9kZWxldGVCb29rc2hlbGZJdGVtQnlQaSgpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19kZWxldGVCb29rc2hlbGZJdGVtQnlQaTogcm9vdCAtICcsIHJvb3QgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2RlbGV0ZUJvb2tzaGVsZkl0ZW1CeVBpOiBpZCAtICcsIGlkICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19kZWxldGVCb29rc2hlbGZJdGVtQnlQaTogcGkgLSAnLCBwaSApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgcHJvbWlzZSA9IFEoICQuYWpheCgge1xuICAgICAgICAgICAgdXJsOiByb290ICsgJy9yZXN0L2Jvb2tzaGVsdmVzL3VzZXIvZ2V0LycgKyBpZCArICcvZGVsZXRlLycgKyBwaSArICcvJyxcbiAgICAgICAgICAgIHR5cGU6IFwiR0VUXCIsXG4gICAgICAgICAgICBkYXRhVHlwZTogXCJKU09OXCIsXG4gICAgICAgICAgICBhc3luYzogdHJ1ZVxuICAgICAgICB9ICkgKTtcbiAgICAgICAgXG4gICAgICAgIHJldHVybiBwcm9taXNlO1xuICAgIH1cbiAgICAvKipcbiAgICAgKiBNZXRob2QgdG8gZGVsZXRlIGFuIGl0ZW0gd2l0aCBQSSwgTE9HSUQgYW5kIFBBR0UgZnJvbSB0aGUgdXNlciBib29rc2hlbGYuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfZGVsZXRlQm9va3NoZWxmSXRlbUJ5UGlMb2dpZFBhZ2VcbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gcm9vdCBUaGUgYXBwbGljYXRpb24gcm9vdCBwYXRoLlxuICAgICAqIEBwYXJhbSB7U3RyaW5nfSBpZCBUaGUgY3VycmVudCBib29rc2hlbGYgaWQuXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IHBpIFRoZSBwaSBvZiB0aGUgaXRlbSB0byBhZGQuXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IGxvZ2lkIFRoZSBsb2dpZCBvZiB0aGUgaXRlbSB0byBhZGQuXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IHBhZ2UgVGhlIHBhZ2Ugb2YgdGhlIGl0ZW0gdG8gYWRkLlxuICAgICAqIEByZXR1cm5zIHtQcm9taXNlfSBBIHByb21pc2UgdGhhdCBjaGVja3MgdGhlIGV4aXN0aW5nIGl0ZW1zLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9kZWxldGVCb29rc2hlbGZJdGVtQnlQaUxvZ2lkUGFnZSggcm9vdCwgaWQsIHBpLCBsb2dpZCwgcGFnZSApIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX2RlbGV0ZUJvb2tzaGVsZkl0ZW1CeVBpTG9naWRQYWdlKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2RlbGV0ZUJvb2tzaGVsZkl0ZW1CeVBpTG9naWRQYWdlOiByb290IC0gJywgcm9vdCApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfZGVsZXRlQm9va3NoZWxmSXRlbUJ5UGlMb2dpZFBhZ2U6IGlkIC0gJywgaWQgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2RlbGV0ZUJvb2tzaGVsZkl0ZW1CeVBpTG9naWRQYWdlOiBwaSAtICcsIHBpICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19kZWxldGVCb29rc2hlbGZJdGVtQnlQaUxvZ2lkUGFnZTogbG9naWQgLSAnLCBsb2dpZCApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfZGVsZXRlQm9va3NoZWxmSXRlbUJ5UGlMb2dpZFBhZ2U6IHBhZ2UgLSAnLCBwYWdlICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIHZhciBwcm9taXNlID0gUSggJC5hamF4KCB7XG4gICAgICAgICAgICB1cmw6IHJvb3QgKyAnL3Jlc3QvYm9va3NoZWx2ZXMvdXNlci9nZXQvJyArIGlkICsgJy9kZWxldGUvJyArIHBpICsgJy8nICsgbG9naWQgKyAnLycgKyBwYWdlICsgJy8nLFxuICAgICAgICAgICAgdHlwZTogXCJHRVRcIixcbiAgICAgICAgICAgIGRhdGFUeXBlOiBcIkpTT05cIixcbiAgICAgICAgICAgIGFzeW5jOiB0cnVlXG4gICAgICAgIH0gKSApO1xuICAgICAgICBcbiAgICAgICAgcmV0dXJuIHByb21pc2U7XG4gICAgfVxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB0byBkZWxldGUgYSBib29rc2hlbGYgYnkgSUQuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfZGVsZXRlQm9va3NoZWxmQnlJZFxuICAgICAqIEBwYXJhbSB7U3RyaW5nfSByb290IFRoZSBhcHBsaWNhdGlvbiByb290IHBhdGguXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IGlkIFRoZSBjdXJyZW50IGJvb2tzaGVsZiBpZC5cbiAgICAgKiBAcmV0dXJucyB7UHJvbWlzZX0gQSBwcm9taXNlIHRoYXQgY2hlY2tzIHRoZSBleGlzdGluZyBpdGVtcy5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfZGVsZXRlQm9va3NoZWxmQnlJZCggcm9vdCwgaWQgKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICctLS0tLS0tLS0tIF9kZWxldGVCb29rc2hlbGZCeUlkKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2RlbGV0ZUJvb2tzaGVsZkJ5SWQ6IHJvb3QgLSAnLCByb290ICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19kZWxldGVCb29rc2hlbGZCeUlkOiBpZCAtICcsIGlkICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIHZhciBwcm9taXNlID0gUSggJC5hamF4KCB7XG4gICAgICAgICAgICB1cmw6IHJvb3QgKyAnL3Jlc3QvYm9va3NoZWx2ZXMvdXNlci9kZWxldGUvJyArIGlkICsgJy8nLFxuICAgICAgICAgICAgdHlwZTogXCJHRVRcIixcbiAgICAgICAgICAgIGRhdGFUeXBlOiBcIkpTT05cIixcbiAgICAgICAgICAgIGFzeW5jOiB0cnVlXG4gICAgICAgIH0gKSApO1xuICAgICAgICBcbiAgICAgICAgcmV0dXJuIHByb21pc2U7XG4gICAgfVxuICAgIC8qICMjIyMjIyMjIEJVSUxEICMjIyMjIyMjICovXG4gICAgLyoqXG4gICAgICogTWV0aG9kIHRvIHJlbmRlciBhIHBvcHVwIHdoaWNoIGNvbnRhaW5zIGJvb2tzaGVsZiBhY3Rpb25zLlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX3JlbmRlckJvb2tzaGVsZlBvcHVwXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IHBpIFRoZSBwaSBvZiB0aGUgaXRlbSB0byBhZGQuXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IGxvZ2lkIFRoZSBsb2dpZCBvZiB0aGUgaXRlbSB0byBhZGQuXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IHBhZ2UgVGhlIHBhZ2Ugb2YgdGhlIGl0ZW0gdG8gYWRkLlxuICAgICAqIEBwYXJhbSB7T2JqZWN0fSBwb3MgVGhlIHBvc2l0aW9uIG9mIHRoZSBjbGlja2VkIGJ1dHRvbi5cbiAgICAgKiBAcGFyYW0ge09iamVjdH0gc2l6ZSBUaGUgd2lkdGggYW5kIGhlaWdodCBvZiB0aGUgY2xpY2tlZCBidXR0b24uXG4gICAgICovXG4gICAgZnVuY3Rpb24gX3JlbmRlckJvb2tzaGVsZlBvcHVwKCBwaSwgbG9naWQsIHBhZ2UsIHBvcywgc2l6ZSApIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX3JlbmRlckJvb2tzaGVsZlBvcHVwKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX3JlbmRlckJvb2tzaGVsZlBvcHVwOiBwaSAtICcsIHBpICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19yZW5kZXJCb29rc2hlbGZQb3B1cDogbG9naWQgLSAnLCBsb2dpZCApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfcmVuZGVyQm9va3NoZWxmUG9wdXA6IHBhZ2UgLSAnLCBwYWdlICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19yZW5kZXJCb29rc2hlbGZQb3B1cDogcG9zIC0gJywgcG9zICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19yZW5kZXJCb29rc2hlbGZQb3B1cDogc2l6ZSAtICcsIHNpemUgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgdmFyIHBpID0gcGk7XG4gICAgICAgIHZhciBwb3NUb3AgPSBwb3MudG9wO1xuICAgICAgICB2YXIgcG9zTGVmdCA9IHBvcy5sZWZ0O1xuICAgICAgICBcbiAgICAgICAgLy8gcmVtb3ZlIGFsbCBwb3B1cHNcbiAgICAgICAgJCggJy5ib29rc2hlbGYtcG9wdXAnICkucmVtb3ZlKCk7XG4gICAgICAgIFxuICAgICAgICAvLyBET00tRWxlbWVudHNcbiAgICAgICAgdmFyIGJvb2tzaGVsZlBvcHVwID0gJCggJzxkaXYgLz4nICkuYWRkQ2xhc3MoICdib29rc2hlbGYtcG9wdXAgYm90dG9tJyApLmNzcygge1xuICAgICAgICAgICAgJ3RvcCc6ICggcG9zVG9wICsgc2l6ZS5oZWlnaHQgKSArIDEwICsgJ3B4JyxcbiAgICAgICAgICAgICdsZWZ0JzogKCBwb3NMZWZ0IC0gMTQyICkgKyAoIHNpemUud2lkdGggLyAyICkgKyAncHgnXG4gICAgICAgIH0gKTtcbiAgICAgICAgdmFyIGJvb2tzaGVsZlBvcHVwTG9hZGVyID0gJCggJzxkaXYgLz4nICkuYWRkQ2xhc3MoICdib29rc2hlbGYtcG9wdXBfX2JvZHktbG9hZGVyJyApO1xuICAgICAgICBib29rc2hlbGZQb3B1cC5hcHBlbmQoIGJvb2tzaGVsZlBvcHVwTG9hZGVyICk7XG4gICAgICAgIFxuICAgICAgICAvLyBidWlsZCBwb3B1cCBoZWFkZXJcbiAgICAgICAgdmFyIGJvb2tzaGVsZlBvcHVwSGVhZGVyID0gJCggJzxkaXYgLz4nICkuYWRkQ2xhc3MoICdib29rc2hlbGYtcG9wdXBfX2hlYWRlcicgKS50ZXh0KCBfZGVmYXVsdHMubXNnLnNlbGVjdEJvb2tzaGVsZiApO1xuICAgICAgICBcbiAgICAgICAgLy8gYnVpbGQgcG9wdXAgYm9keVxuICAgICAgICB2YXIgYm9va3NoZWxmUG9wdXBCb2R5ID0gJCggJzxkaXYgLz4nICkuYWRkQ2xhc3MoICdib29rc2hlbGYtcG9wdXBfX2JvZHknICk7XG4gICAgICAgIFxuICAgICAgICAvLyBidWlsZCBwb3B1cCBmb290ZXJcbiAgICAgICAgdmFyIGJvb2tzaGVsZlBvcHVwRm9vdGVyID0gJCggJzxkaXYgLz4nICkuYWRkQ2xhc3MoICdib29rc2hlbGYtcG9wdXBfX2Zvb3RlcicgKTtcbiAgICAgICAgdmFyIGJvb2tzaGVsZlBvcHVwRm9vdGVyUm93ID0gJCggJzxkaXYgLz4nICkuYWRkQ2xhc3MoICdyb3cgbm8tbWFyZ2luJyApO1xuICAgICAgICB2YXIgYm9va3NoZWxmUG9wdXBGb290ZXJDb2xMZWZ0ID0gJCggJzxkaXYgLz4nICkuYWRkQ2xhc3MoICdjb2wteHMtMTEgbm8tcGFkZGluZycgKTtcbiAgICAgICAgdmFyIGJvb2tzaGVsZlBvcHVwRm9vdGVySW5wdXQgPSAkKCAnPGlucHV0IC8+JyApLmF0dHIoICd0eXBlJywgJ3RleHQnICkuYXR0ciggJ3BsYWNlaG9sZGVyJywgX2RlZmF1bHRzLm1zZy5hZGROZXdCb29rc2hlbGYgKTtcbiAgICAgICAgYm9va3NoZWxmUG9wdXBGb290ZXJDb2xMZWZ0LmFwcGVuZCggYm9va3NoZWxmUG9wdXBGb290ZXJJbnB1dCApO1xuICAgICAgICB2YXIgYm9va3NoZWxmUG9wdXBGb290ZXJDb2xyaWdodCA9ICQoICc8ZGl2IC8+JyApLmFkZENsYXNzKCAnY29sLXhzLTEgbm8tcGFkZGluZycgKTtcbiAgICAgICAgdmFyIGJvb2tzaGVsZlBvcHVwRm9vdGVyQWRkQnRuID0gJCggJzxidXR0b24gLz4nICkuYWRkQ2xhc3MoICdidG4tY2xlYW4nICkuYXR0ciggJ3R5cGUnLCAnYnV0dG9uJyApLmF0dHIoICdkYXRhLWJvb2tzaGVsZi10eXBlJywgJ2FkZCcgKS5hdHRyKCAnZGF0YS1waScsIHBpICk7XG4gICAgICAgIGJvb2tzaGVsZlBvcHVwRm9vdGVyQ29scmlnaHQuYXBwZW5kKCBib29rc2hlbGZQb3B1cEZvb3RlckFkZEJ0biApO1xuICAgICAgICBib29rc2hlbGZQb3B1cEZvb3RlclJvdy5hcHBlbmQoIGJvb2tzaGVsZlBvcHVwRm9vdGVyQ29sTGVmdCApLmFwcGVuZCggYm9va3NoZWxmUG9wdXBGb290ZXJDb2xyaWdodCApO1xuICAgICAgICBib29rc2hlbGZQb3B1cEZvb3Rlci5hcHBlbmQoIGJvb2tzaGVsZlBvcHVwRm9vdGVyUm93ICk7XG4gICAgICAgIFxuICAgICAgICAvLyBidWlsZCBwb3B1cFxuICAgICAgICBib29rc2hlbGZQb3B1cC5hcHBlbmQoIGJvb2tzaGVsZlBvcHVwSGVhZGVyICkuYXBwZW5kKCBib29rc2hlbGZQb3B1cEJvZHkgKS5hcHBlbmQoIGJvb2tzaGVsZlBvcHVwRm9vdGVyICk7XG4gICAgICAgIFxuICAgICAgICAvLyBhcHBlbmQgcG9wdXBcbiAgICAgICAgJCggJ2JvZHknICkuYXBwZW5kKCBib29rc2hlbGZQb3B1cCApO1xuICAgICAgICBcbiAgICAgICAgLy8gcmVuZGVyIGJvb2tzaGVsZiBsaXN0XG4gICAgICAgIF9yZW5kZXJCb29rc2hlbGZQb3BvdmVyTGlzdCggcGkgKTtcbiAgICAgICAgXG4gICAgICAgIC8vIGFkZCBuZXcgYm9va3NoZWxmIGluIHBvcG92ZXJcbiAgICAgICAgJCggJy5ib29rc2hlbGYtcG9wdXBfX2Zvb3RlciBbZGF0YS1ib29rc2hlbGYtdHlwZT1cImFkZFwiXScgKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICB2YXIgYnNOYW1lID0gJCggJy5ib29rc2hlbGYtcG9wdXBfX2Zvb3RlciBpbnB1dCcgKS52YWwoKTtcbiAgICAgICAgICAgIHZhciBjdXJyUGkgPSAkKCB0aGlzICkuYXR0ciggJ2RhdGEtcGknICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmICggYnNOYW1lICE9ICcnICkge1xuICAgICAgICAgICAgICAgIF9hZGROYW1lZEJvb2tzaGVsZiggX2RlZmF1bHRzLnJvb3QsIGJzTmFtZSApLnRoZW4oIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICAkKCAnLmJvb2tzaGVsZi1wb3B1cF9fZm9vdGVyIGlucHV0JyApLnZhbCggJycgKTtcbiAgICAgICAgICAgICAgICAgICAgX3JlbmRlckJvb2tzaGVsZlBvcG92ZXJMaXN0KCBjdXJyUGkgKTtcbiAgICAgICAgICAgICAgICAgICAgX3JlbmRlckJvb2tzaGVsZk5hdmlnYXRpb25MaXN0KCk7XG4gICAgICAgICAgICAgICAgfSApLmZhaWwoIGZ1bmN0aW9uKCBlcnJvciApIHtcbiAgICAgICAgICAgICAgICAgICAgY29uc29sZS5lcnJvciggJ0VSUk9SIC0gX2FkZE5hbWVkQm9va3NoZWxmOiAnLCBlcnJvci5yZXNwb25zZVRleHQgKTtcbiAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICBfYWRkQXV0b21hdGljTmFtZWRCb29rc2hlbGYoIF9kZWZhdWx0cy5yb290ICkudGhlbiggZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICQoICcuYm9va3NoZWxmLXBvcHVwX19mb290ZXIgaW5wdXQnICkudmFsKCAnJyApO1xuICAgICAgICAgICAgICAgICAgICBfcmVuZGVyQm9va3NoZWxmUG9wb3Zlckxpc3QoIGN1cnJQaSApO1xuICAgICAgICAgICAgICAgICAgICBfcmVuZGVyQm9va3NoZWxmTmF2aWdhdGlvbkxpc3QoKTtcbiAgICAgICAgICAgICAgICB9ICkuZmFpbCggZnVuY3Rpb24oIGVycm9yICkge1xuICAgICAgICAgICAgICAgICAgICBjb25zb2xlLmVycm9yKCAnRVJST1IgLSBfYWRkQXV0b21hdGljTmFtZWRCb29rc2hlbGY6ICcsIGVycm9yLnJlc3BvbnNlVGV4dCApO1xuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSApO1xuICAgICAgICBcbiAgICAgICAgLy8gYWRkIG5ldyBib29rc2hlbGYgb24gZW50ZXIgaW4gcG9wb3ZlclxuICAgICAgICAkKCAnLmJvb2tzaGVsZi1wb3B1cF9fZm9vdGVyIGlucHV0JyApLm9uKCAna2V5dXAnLCBmdW5jdGlvbiggZXZlbnQgKSB7XG4gICAgICAgICAgICBpZiAoIGV2ZW50LndoaWNoID09IDEzICkge1xuICAgICAgICAgICAgICAgICQoICcuYm9va3NoZWxmLXBvcHVwX19mb290ZXIgW2RhdGEtYm9va3NoZWxmLXR5cGU9XCJhZGRcIl0nICkuY2xpY2soKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSApO1xuICAgIH1cbiAgICAvKipcbiAgICAgKiBNZXRob2QgdG8gcmVuZGVyIHRoZSBlbGVtZW50IGxpc3QgaW4gYm9va3NoZWxmIHBvcG92ZXIuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfcmVuZGVyQm9va3NoZWxmUG9wb3Zlckxpc3RcbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gcGkgVGhlIGN1cnJlbnQgUEkgb2YgdGhlIHNlbGVjdGVkIGl0ZW0uXG4gICAgICovXG4gICAgZnVuY3Rpb24gX3JlbmRlckJvb2tzaGVsZlBvcG92ZXJMaXN0KCBwaSApIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX3JlbmRlckJvb2tzaGVsZlBvcG92ZXJMaXN0KCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX3JlbmRlckJvb2tzaGVsZlBvcG92ZXJMaXN0OiBwaSAtICcsIHBpICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIF9nZXRBbGxCb29rc2hlbHZlcyggX2RlZmF1bHRzLnJvb3QgKS50aGVuKCBmdW5jdGlvbiggZWxlbWVudHMgKSB7XG4gICAgICAgICAgICAvLyBET00tRWxlbWVudHNcbiAgICAgICAgICAgIHZhciBkcm9wZG93bkxpc3QgPSAkKCAnPHVsIC8+JyApLmFkZENsYXNzKCAnYm9va3NoZWxmLXBvcHVwX19ib2R5LWxpc3QgbGlzdCcgKTtcbiAgICAgICAgICAgIHZhciBkcm9wZG93bkxpc3RJdGVtID0gbnVsbDtcbiAgICAgICAgICAgIHZhciBkcm9wZG93bkxpc3RJdGVtVGV4dCA9IG51bGw7XG4gICAgICAgICAgICB2YXIgZHJvcGRvd25MaXN0SXRlbUFkZCA9IG51bGw7XG4gICAgICAgICAgICB2YXIgZHJvcGRvd25MaXN0SXRlbUlzSW5Cb29rc2hlbGYgPSBudWxsO1xuICAgICAgICAgICAgdmFyIGRyb3Bkb3duTGlzdEl0ZW1BZGRDb3VudGVyID0gbnVsbDtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgaWYgKCBlbGVtZW50cy5sZW5ndGggPiAwICkge1xuICAgICAgICAgICAgICAgIGVsZW1lbnRzLmZvckVhY2goIGZ1bmN0aW9uKCBpdGVtICkge1xuICAgICAgICAgICAgICAgICAgICBkcm9wZG93bkxpc3RJdGVtID0gJCggJzxsaSAvPicgKTtcbiAgICAgICAgICAgICAgICAgICAgZHJvcGRvd25MaXN0SXRlbUlzSW5Cb29rc2hlbGYgPSAnJztcbiAgICAgICAgICAgICAgICAgICAgLy8gY2hlY2sgaWYgaXRlbSBpcyBpbiBib29rc2hlbGZcbiAgICAgICAgICAgICAgICAgICAgaXRlbS5pdGVtcy5mb3JFYWNoKCBmdW5jdGlvbiggb2JqZWN0ICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKCBvYmplY3QucGkgPT0gcGkgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgZHJvcGRvd25MaXN0SXRlbUlzSW5Cb29rc2hlbGYgPSAnPGkgY2xhc3M9XCJmYSBmYS1jaGVja1wiIGFyaWEtaGlkZGVuPVwidHJ1ZVwiPjwvaT4gJztcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgICAgICBkcm9wZG93bkxpc3RJdGVtQWRkQ291bnRlciA9ICQoICc8c3BhbiAvPicgKS50ZXh0KCBpdGVtLml0ZW1zLmxlbmd0aCApO1xuICAgICAgICAgICAgICAgICAgICBkcm9wZG93bkxpc3RJdGVtQWRkID0gJCggJzxidXR0b24gLz4nICkuYWRkQ2xhc3MoICdidG4tY2xlYW4nICkuYXR0ciggJ3R5cGUnLCAnYnV0dG9uJyApLmF0dHIoICdkYXRhLWJvb2tzaGVsZi10eXBlJywgJ2FkZCcgKS5hdHRyKCAnZGF0YS1pZCcsIGl0ZW0uaWQgKVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIC5hdHRyKCAnZGF0YS1waScsIHBpICkudGV4dCggaXRlbS5uYW1lICkucHJlcGVuZCggZHJvcGRvd25MaXN0SXRlbUlzSW5Cb29rc2hlbGYgKS5hcHBlbmQoIGRyb3Bkb3duTGlzdEl0ZW1BZGRDb3VudGVyICk7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAvLyBidWlsZCBib29rc2hlbGYgaXRlbVxuICAgICAgICAgICAgICAgICAgICBkcm9wZG93bkxpc3RJdGVtLmFwcGVuZCggZHJvcGRvd25MaXN0SXRlbUFkZCApO1xuICAgICAgICAgICAgICAgICAgICBkcm9wZG93bkxpc3QuYXBwZW5kKCBkcm9wZG93bkxpc3RJdGVtICk7XG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgLy8gYWRkIGVtcHR5IGxpc3QgaXRlbVxuICAgICAgICAgICAgICAgIGRyb3Bkb3duTGlzdEl0ZW0gPSAkKCAnPGxpIC8+JyApO1xuICAgICAgICAgICAgICAgIGRyb3Bkb3duTGlzdEl0ZW1UZXh0ID0gJCggJzxzcGFuIC8+JyApLmFkZENsYXNzKCAnZW1wdHknICkudGV4dCggX2RlZmF1bHRzLm1zZy5ub0l0ZW1zQXZhaWxhYmxlICk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgZHJvcGRvd25MaXN0SXRlbS5hcHBlbmQoIGRyb3Bkb3duTGlzdEl0ZW1UZXh0ICk7XG4gICAgICAgICAgICAgICAgZHJvcGRvd25MaXN0LmFwcGVuZCggZHJvcGRvd25MaXN0SXRlbSApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICAvLyByZW5kZXIgY29tcGxldGUgbGlzdFxuICAgICAgICAgICAgJCggJy5ib29rc2hlbGYtcG9wdXBfX2JvZHknICkuZW1wdHkoKS5hcHBlbmQoIGRyb3Bkb3duTGlzdCApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICAvLyByZW1vdmUgbG9hZGVyXG4gICAgICAgICAgICAkKCAnLmJvb2tzaGVsZi1wb3B1cF9fYm9keS1sb2FkZXInICkucmVtb3ZlKCk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIGFkZCBpdGVtIHRvIGJvb2tzaGVsZlxuICAgICAgICAgICAgJCggJy5ib29rc2hlbGYtcG9wdXBfX2JvZHktbGlzdCBbZGF0YS1ib29rc2hlbGYtdHlwZT1cImFkZFwiXScgKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgdmFyIGN1cnJCdG4gPSAkKCB0aGlzICk7XG4gICAgICAgICAgICAgICAgdmFyIGN1cnJJZCA9IGN1cnJCdG4uYXR0ciggJ2RhdGEtaWQnICk7XG4gICAgICAgICAgICAgICAgdmFyIGN1cnJQaSA9IGN1cnJCdG4uYXR0ciggJ2RhdGEtcGknICk7XG4gICAgICAgICAgICAgICAgdmFyIGlzQ2hlY2tlZCA9IGN1cnJCdG4uZmluZCggJy5mYS1jaGVjaycgKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICBpZiAoIGlzQ2hlY2tlZC5sZW5ndGggPiAwICkge1xuICAgICAgICAgICAgICAgICAgICBfZGVsZXRlQm9va3NoZWxmSXRlbUJ5UGkoIF9kZWZhdWx0cy5yb290LCBjdXJySWQsIGN1cnJQaSApLnRoZW4oIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgX3JlbmRlckJvb2tzaGVsZlBvcG92ZXJMaXN0KCBjdXJyUGkgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIF9yZW5kZXJCb29rc2hlbGZOYXZpZ2F0aW9uTGlzdCgpO1xuICAgICAgICAgICAgICAgICAgICAgICAgX3NldEFkZGVkU3RhdHVzKCk7XG4gICAgICAgICAgICAgICAgICAgIH0gKS5mYWlsKCBmdW5jdGlvbiggZXJyb3IgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBjb25zb2xlLmVycm9yKCAnRVJST1IgLSBfZGVsZXRlQm9va3NoZWxmSXRlbUJ5UGk6ICcsIGVycm9yLnJlc3BvbnNlVGV4dCApO1xuICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICBfYWRkQm9va3NoZWxmSXRlbUJ5UGkoIF9kZWZhdWx0cy5yb290LCBjdXJySWQsIGN1cnJQaSApLnRoZW4oIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgX3JlbmRlckJvb2tzaGVsZlBvcG92ZXJMaXN0KCBjdXJyUGkgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIF9yZW5kZXJCb29rc2hlbGZOYXZpZ2F0aW9uTGlzdCgpO1xuICAgICAgICAgICAgICAgICAgICAgICAgX3NldEFkZGVkU3RhdHVzKCk7XG4gICAgICAgICAgICAgICAgICAgIH0gKS5mYWlsKCBmdW5jdGlvbiggZXJyb3IgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBjb25zb2xlLmVycm9yKCAnRVJST1IgLSBfYWRkQm9va3NoZWxmSXRlbUJ5UGk6ICcsIGVycm9yLnJlc3BvbnNlVGV4dCApO1xuICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgXG4gICAgICAgIH0gKS5mYWlsKCBmdW5jdGlvbiggZXJyb3IgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmVycm9yKCAnRVJST1IgLSBfZ2V0QWxsQm9va3NoZWx2ZXM6ICcsIGVycm9yLnJlc3BvbnNlVGV4dCApO1xuICAgICAgICB9ICk7XG4gICAgfVxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB0byByZW5kZXIgdGhlIGVsZW1lbnQgbGlzdCBpbiBib29rc2hlbGYgbmF2aWdhdGlvbi5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9yZW5kZXJCb29rc2hlbGZOYXZpZ2F0aW9uTGlzdFxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9yZW5kZXJCb29rc2hlbGZOYXZpZ2F0aW9uTGlzdCgpIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX3JlbmRlckJvb2tzaGVsZk5hdmlnYXRpb25MaXN0KCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgdmFyIGFsbEJvb2tzaGVsZkl0ZW1zID0gMDtcbiAgICAgICAgXG4gICAgICAgIF9nZXRBbGxCb29rc2hlbHZlcyggX2RlZmF1bHRzLnJvb3QgKS50aGVuKCBmdW5jdGlvbiggZWxlbWVudHMgKSB7XG4gICAgICAgICAgICAvLyBET00tRWxlbWVudHNcbiAgICAgICAgICAgIHZhciBkcm9wZG93bkxpc3QgPSAkKCAnPHVsIC8+JyApLmFkZENsYXNzKCAnYm9va3NoZWxmLW5hdmlnYXRpb25fX2Ryb3Bkb3duLWxpc3QgbGlzdCcgKTtcbiAgICAgICAgICAgIHZhciBkcm9wZG93bkxpc3RJdGVtID0gbnVsbDtcbiAgICAgICAgICAgIHZhciBkcm9wZG93bkxpc3RJdGVtUm93ID0gbnVsbDtcbiAgICAgICAgICAgIHZhciBkcm9wZG93bkxpc3RJdGVtTGVmdCA9IG51bGw7XG4gICAgICAgICAgICB2YXIgZHJvcGRvd25MaXN0SXRlbVJpZ2h0ID0gbnVsbDtcbiAgICAgICAgICAgIHZhciBkcm9wZG93bkxpc3RJdGVtVGV4dCA9IG51bGw7XG4gICAgICAgICAgICB2YXIgZHJvcGRvd25MaXN0SXRlbUxpbmsgPSBudWxsO1xuICAgICAgICAgICAgdmFyIGRyb3Bkb3duTGlzdEl0ZW1BZGRDb3VudGVyID0gbnVsbDtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgaWYgKCBlbGVtZW50cy5sZW5ndGggPiAwICkge1xuICAgICAgICAgICAgICAgIGVsZW1lbnRzLmZvckVhY2goIGZ1bmN0aW9uKCBpdGVtICkge1xuICAgICAgICAgICAgICAgICAgICBkcm9wZG93bkxpc3RJdGVtID0gJCggJzxsaSAvPicgKTtcbiAgICAgICAgICAgICAgICAgICAgZHJvcGRvd25MaXN0SXRlbVJvdyA9ICQoICc8ZGl2IC8+JyApLmFkZENsYXNzKCAncm93IG5vLW1hcmdpbicgKTtcbiAgICAgICAgICAgICAgICAgICAgZHJvcGRvd25MaXN0SXRlbUxlZnQgPSAkKCAnPGRpdiAvPicgKS5hZGRDbGFzcyggJ2NvbC14cy0xMCBuby1wYWRkaW5nJyApO1xuICAgICAgICAgICAgICAgICAgICBkcm9wZG93bkxpc3RJdGVtUmlnaHQgPSAkKCAnPGRpdiAvPicgKS5hZGRDbGFzcyggJ2NvbC14cy0yIG5vLXBhZGRpbmcnICk7XG4gICAgICAgICAgICAgICAgICAgIGRyb3Bkb3duTGlzdEl0ZW1MaW5rID0gJCggJzxhIC8+JyApLmF0dHIoICdocmVmJywgX2RlZmF1bHRzLnJvb3QgKyAnL2Jvb2tzaGVsZi8nICsgaXRlbS5pZCArICcvJyApLnRleHQoIGl0ZW0ubmFtZSApO1xuICAgICAgICAgICAgICAgICAgICBkcm9wZG93bkxpc3RJdGVtQWRkQ291bnRlciA9ICQoICc8c3BhbiAvPicgKS5hZGRDbGFzcyggJ2Jvb2tzaGVsZi1uYXZpZ2F0aW9uX19kcm9wZG93bi1saXN0LWNvdW50ZXInICkudGV4dCggaXRlbS5pdGVtcy5sZW5ndGggKTtcbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgIC8vIGJ1aWxkIGJvb2tzaGVsZiBpdGVtXG4gICAgICAgICAgICAgICAgICAgIGRyb3Bkb3duTGlzdEl0ZW1MZWZ0LmFwcGVuZCggZHJvcGRvd25MaXN0SXRlbUxpbmsgKTtcbiAgICAgICAgICAgICAgICAgICAgZHJvcGRvd25MaXN0SXRlbVJpZ2h0LmFwcGVuZCggZHJvcGRvd25MaXN0SXRlbUFkZENvdW50ZXIgKTtcbiAgICAgICAgICAgICAgICAgICAgZHJvcGRvd25MaXN0SXRlbVJvdy5hcHBlbmQoIGRyb3Bkb3duTGlzdEl0ZW1MZWZ0ICkuYXBwZW5kKCBkcm9wZG93bkxpc3RJdGVtUmlnaHQgKVxuICAgICAgICAgICAgICAgICAgICBkcm9wZG93bkxpc3RJdGVtLmFwcGVuZCggZHJvcGRvd25MaXN0SXRlbVJvdyApO1xuICAgICAgICAgICAgICAgICAgICBkcm9wZG93bkxpc3QuYXBwZW5kKCBkcm9wZG93bkxpc3RJdGVtICk7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAvLyByYWlzZSBib29rc2hlbGYgaXRlbSBjb3VudGVyXG4gICAgICAgICAgICAgICAgICAgIGFsbEJvb2tzaGVsZkl0ZW1zICs9IGl0ZW0uaXRlbXMubGVuZ3RoO1xuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAvLyBzZXQgYm9va3NoZWxmIGl0ZW0gY291bnRlclxuICAgICAgICAgICAgICAgICQoICdbZGF0YS1ib29rc2hlbGYtdHlwZT1cImNvdW50ZXJcIl0nICkuZW1wdHkoKS50ZXh0KCBhbGxCb29rc2hlbGZJdGVtcyApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgLy8gYWRkIGVtcHR5IGxpc3QgaXRlbVxuICAgICAgICAgICAgICAgIGRyb3Bkb3duTGlzdEl0ZW0gPSAkKCAnPGxpIC8+JyApO1xuICAgICAgICAgICAgICAgIGRyb3Bkb3duTGlzdEl0ZW1UZXh0ID0gJCggJzxzcGFuIC8+JyApLmFkZENsYXNzKCAnZW1wdHknICkudGV4dCggX2RlZmF1bHRzLm1zZy5ub0l0ZW1zQXZhaWxhYmxlICk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgZHJvcGRvd25MaXN0SXRlbS5hcHBlbmQoIGRyb3Bkb3duTGlzdEl0ZW1UZXh0ICk7XG4gICAgICAgICAgICAgICAgZHJvcGRvd25MaXN0LmFwcGVuZCggZHJvcGRvd25MaXN0SXRlbSApO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIC8vIHNldCBib29rc2hlbGYgaXRlbSBjb3VudGVyXG4gICAgICAgICAgICAgICAgJCggJ1tkYXRhLWJvb2tzaGVsZi10eXBlPVwiY291bnRlclwiXScgKS5lbXB0eSgpLnRleHQoIGFsbEJvb2tzaGVsZkl0ZW1zICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIHJlbmRlciBjb21wbGV0ZSBsaXN0XG4gICAgICAgICAgICAkKCAnLmJvb2tzaGVsZi1uYXZpZ2F0aW9uX19kcm9wZG93bi1saXN0JyApLmVtcHR5KCkuYXBwZW5kKCBkcm9wZG93bkxpc3QgKTtcbiAgICAgICAgICAgIFxuICAgICAgICB9ICkuZmFpbCggZnVuY3Rpb24oIGVycm9yICkge1xuICAgICAgICAgICAgY29uc29sZS5lcnJvciggJ0VSUk9SIC0gX2dldEFsbEJvb2tzaGVsdmVzOiAnLCBlcnJvci5yZXNwb25zZVRleHQgKTtcbiAgICAgICAgfSApO1xuICAgIH1cbiAgICBcbiAgICAvKipcbiAgICAgKiBNZXRob2QgdG8gc2V0IGFuICdhZGRlZCcgc3RhdHVzIHRvIGFuIG9iamVjdC5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9zZXRBZGRlZFN0YXR1c1xuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9zZXRBZGRlZFN0YXR1cygpIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX3NldEFkZGVkU3RhdHVzKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgJCggJ1tkYXRhLWJvb2tzaGVsZi10eXBlPVwiYWRkXCJdJyApLmVhY2goIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgdmFyIGN1cnJUcmlnZ2VyID0gJCggdGhpcyApO1xuICAgICAgICAgICAgdmFyIGN1cnJUcmlnZ2VyUGkgPSBjdXJyVHJpZ2dlci5hdHRyKCAnZGF0YS1waScgKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgX2lzSXRlbUluQm9va3NoZWxmKCBjdXJyVHJpZ2dlciwgY3VyclRyaWdnZXJQaSApO1xuICAgICAgICB9ICk7XG4gICAgfVxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB0byBjaGVjayBpZiBpdGVtIGlzIGluIGFueSBib29rc2hlbGYuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfaXNJdGVtSW5Cb29rc2hlbGZcbiAgICAgKiBAcGFyYW0ge09iamVjdH0gb2JqZWN0IEFuIGpRdWVyeS1PYmplY3Qgb2YgdGhlIGN1cnJlbnQgaXRlbS5cbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gcGkgVGhlIGN1cnJlbnQgUEkgb2YgdGhlIHNlbGVjdGVkIGl0ZW0uXG4gICAgICovXG4gICAgZnVuY3Rpb24gX2lzSXRlbUluQm9va3NoZWxmKCBvYmplY3QsIHBpICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfaXNJdGVtSW5Cb29rc2hlbGYoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfaXNJdGVtSW5Cb29rc2hlbGY6IHBpIC0gJywgcGkgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2lzSXRlbUluQm9va3NoZWxmOiBvYmplY3QgLSAnLCBvYmplY3QgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgX2dldENvbnRhaW5pbmdCb29rc2hlbGZJdGVtQnlQaSggX2RlZmF1bHRzLnJvb3QsIHBpICkudGhlbiggZnVuY3Rpb24oIGl0ZW1zICkge1xuICAgICAgICAgICAgaWYgKCBpdGVtcy5sZW5ndGggPT0gMCApIHtcbiAgICAgICAgICAgICAgICBvYmplY3QucmVtb3ZlQ2xhc3MoICdhZGRlZCcgKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICByZXR1cm4gZmFsc2U7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICBvYmplY3QuYWRkQ2xhc3MoICdhZGRlZCcgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSApLmZhaWwoIGZ1bmN0aW9uKCBlcnJvciApIHtcbiAgICAgICAgICAgIGNvbnNvbGUuZXJyb3IoICdFUlJPUiAtIF9nZXRDb250YWluaW5nQm9va3NoZWxmSXRlbUJ5UGk6ICcsIGVycm9yLnJlc3BvbnNlVGV4dCApO1xuICAgICAgICB9ICk7XG4gICAgfVxuICAgIFxuICAgIHJldHVybiB2aWV3ZXI7XG4gICAgXG59ICkoIHZpZXdlckpTIHx8IHt9LCBqUXVlcnkgKTtcbiIsIi8qKlxuICogVGhpcyBmaWxlIGlzIHBhcnQgb2YgdGhlIEdvb2JpIHZpZXdlciAtIGEgY29udGVudCBwcmVzZW50YXRpb24gYW5kIG1hbmFnZW1lbnRcbiAqIGFwcGxpY2F0aW9uIGZvciBkaWdpdGl6ZWQgb2JqZWN0cy5cbiAqIFxuICogVmlzaXQgdGhlc2Ugd2Vic2l0ZXMgZm9yIG1vcmUgaW5mb3JtYXRpb24uIC0gaHR0cDovL3d3dy5pbnRyYW5kYS5jb20gLVxuICogaHR0cDovL2RpZ2l2ZXJzby5jb21cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGZyZWUgc29mdHdhcmU7IHlvdSBjYW4gcmVkaXN0cmlidXRlIGl0IGFuZC9vciBtb2RpZnkgaXQgdW5kZXIgdGhlIHRlcm1zXG4gKiBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYXMgcHVibGlzaGVkIGJ5IHRoZSBGcmVlIFNvZnR3YXJlIEZvdW5kYXRpb247IGVpdGhlclxuICogdmVyc2lvbiAyIG9mIHRoZSBMaWNlbnNlLCBvciAoYXQgeW91ciBvcHRpb24pIGFueSBsYXRlciB2ZXJzaW9uLlxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZGlzdHJpYnV0ZWQgaW4gdGhlIGhvcGUgdGhhdCBpdCB3aWxsIGJlIHVzZWZ1bCwgYnV0IFdJVEhPVVQgQU5ZXG4gKiBXQVJSQU5UWTsgd2l0aG91dCBldmVuIHRoZSBpbXBsaWVkIHdhcnJhbnR5IG9mIE1FUkNIQU5UQUJJTElUWSBvciBGSVRORVNTIEZPUiBBXG4gKiBQQVJUSUNVTEFSIFBVUlBPU0UuIFNlZSB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgZm9yIG1vcmUgZGV0YWlscy5cbiAqIFxuICogWW91IHNob3VsZCBoYXZlIHJlY2VpdmVkIGEgY29weSBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYWxvbmcgd2l0aCB0aGlzXG4gKiBwcm9ncmFtLiBJZiBub3QsIHNlZSA8aHR0cDovL3d3dy5nbnUub3JnL2xpY2Vuc2VzLz4uXG4gKiBcbiAqIDxTaG9ydCBNb2R1bGUgRGVzY3JpcHRpb24+XG4gKiBcbiAqIEB2ZXJzaW9uIDMuMi4wXG4gKiBAbW9kdWxlIHZpZXdlckpTLmNhbGVuZGFyUG9wb3ZlclxuICogQHJlcXVpcmVzIGpRdWVyeVxuICovXG52YXIgdmlld2VySlMgPSAoIGZ1bmN0aW9uKCB2aWV3ZXIgKSB7XG4gICAgJ3VzZSBzdHJpY3QnO1xuICAgIFxuICAgIHZhciBfZGVidWcgPSBmYWxzZTtcbiAgICB2YXIgX3RoaXMgPSBudWxsO1xuICAgIHZhciBfY3VyckFwaUNhbGwgPSAnJztcbiAgICB2YXIgX2pzb24gPSB7fTtcbiAgICB2YXIgX3BvcG92ZXJDb25maWcgPSB7fTtcbiAgICB2YXIgX3BvcG92ZXJDb250ZW50ID0gbnVsbDtcbiAgICB2YXIgX2RlZmF1bHRzID0ge1xuICAgICAgICBhcHBVcmw6ICcnLFxuICAgICAgICBjYWxlbmRhcldyYXBwZXJTZWxlY3RvcjogJy5zZWFyY2gtY2FsZW5kYXJfX21vbnRocycsXG4gICAgICAgIHBvcG92ZXJUcmlnZ2VyU2VsZWN0b3I6ICdbZGF0YS1wb3BvdmVyLXRyaWdnZXI9XCJjYWxlbmRhci1wby10cmlnZ2VyXCJdJyxcbiAgICAgICAgcG9wb3ZlclRpdGxlOiAnQml0dGUgw7xiZXJnZWJlbiBTaWUgZGVuIFRpdGVsIGRlcyBXZXJrcycsXG4gICAgfTtcbiAgICBcbiAgICB2aWV3ZXIuY2FsZW5kYXJQb3BvdmVyID0ge1xuICAgICAgICBpbml0OiBmdW5jdGlvbiggY29uZmlnICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICcjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIuY2FsZW5kYXJQb3BvdmVyLmluaXQnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICcjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIuY2FsZW5kYXJQb3BvdmVyLmluaXQ6IGNvbmZpZyAtICcsIGNvbmZpZyApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICAkLmV4dGVuZCggdHJ1ZSwgX2RlZmF1bHRzLCBjb25maWcgKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gVE9ETzogRmVobGVybWVsZHVuZyBpbiBkZXIgS29uc29sZSBiZXNlaXRpZ2VuLCB3ZW5uIG1hbiBhdWYgZGVuIFRhZyBlaW5cbiAgICAgICAgICAgIC8vIHp3ZWl0ZXMgTWFsIGtsaWNrdC5cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gc2hvdyBwb3BvdmVyIGZvciBjdXJyZW50IGRheVxuICAgICAgICAgICAgJCggX2RlZmF1bHRzLnBvcG92ZXJUcmlnZ2VyU2VsZWN0b3IgKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgX3RoaXMgPSAkKCB0aGlzICk7XG4gICAgICAgICAgICAgICAgX2N1cnJBcGlDYWxsID0gZW5jb2RlVVJJKCBfdGhpcy5hdHRyKCAnZGF0YS1hcGknICkgKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICB2aWV3ZXJKUy5oZWxwZXIuZ2V0UmVtb3RlRGF0YSggX2N1cnJBcGlDYWxsICkuZG9uZSggZnVuY3Rpb24oIF9qc29uICkge1xuICAgICAgICAgICAgICAgICAgICBfcG9wb3ZlckNvbnRlbnQgPSBfZ2V0UG9wb3ZlckNvbnRlbnQoIF9qc29uLCBfZGVmYXVsdHMgKTtcbiAgICAgICAgICAgICAgICAgICAgX3BvcG92ZXJDb25maWcgPSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBwbGFjZW1lbnQ6ICdhdXRvIGJvdHRvbScsXG4gICAgICAgICAgICAgICAgICAgICAgICB0aXRsZTogX2RlZmF1bHRzLnBvcG92ZXJUaXRsZSxcbiAgICAgICAgICAgICAgICAgICAgICAgIGNvbnRlbnQ6IF9wb3BvdmVyQ29udGVudCxcbiAgICAgICAgICAgICAgICAgICAgICAgIHZpZXdwb3J0OiB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgc2VsZWN0b3I6IF9kZWZhdWx0cy5jYWxlbmRhcldyYXBwZXJTZWxlY3RvclxuICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgIGh0bWw6IHRydWVcbiAgICAgICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICQoIF9kZWZhdWx0cy5wb3BvdmVyVHJpZ2dlclNlbGVjdG9yICkucG9wb3ZlciggJ2Rlc3Ryb3knICk7XG4gICAgICAgICAgICAgICAgICAgIF90aGlzLnBvcG92ZXIoIF9wb3BvdmVyQ29uZmlnICk7XG4gICAgICAgICAgICAgICAgICAgIF90aGlzLnBvcG92ZXIoICdzaG93JyApO1xuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gcmVtb3ZlIGFsbCBwb3BvdmVycyBieSBjbGlja2luZyBvbiBib2R5XG4gICAgICAgICAgICAkKCAnYm9keScgKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oIGV2ZW50ICkge1xuICAgICAgICAgICAgICAgIGlmICggJCggZXZlbnQudGFyZ2V0ICkuY2xvc2VzdCggX2RlZmF1bHRzLnBvcG92ZXJUcmlnZ2VyU2VsZWN0b3IgKS5sZW5ndGggKSB7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICQoIF9kZWZhdWx0cy5wb3BvdmVyVHJpZ2dlclNlbGVjdG9yICkucG9wb3ZlciggJ2Rlc3Ryb3knICk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfSApO1xuICAgICAgICB9XG4gICAgfTtcbiAgICBcbiAgICAvKipcbiAgICAgKiBNZXRob2QgdG8gcmVuZGVyIHRoZSBwb3BvdmVyIGNvbnRlbnQuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfZ2V0UG9wb3ZlckNvbnRlbnRcbiAgICAgKiBAcGFyYW0ge09iamVjdH0gZGF0YSBBIEpTT04tT2JqZWN0IHdoaWNoIGNvbnRhaW5zIHRoZSBuZWNlc3NhcnkgZGF0YS5cbiAgICAgKiBAcGFyYW0ge09iamVjdH0gY29uZmlnIFRoZSBjb25maWcgb2JqZWN0IG9mIHRoZSBtb2R1bGUuXG4gICAgICogQHJldHVybnMge1N0cmluZ30gVGhlIEhUTUwtU3RyaW5nIG9mIHRoZSBwb3BvdmVyIGNvbnRlbnQuXG4gICAgICovXG4gICAgZnVuY3Rpb24gX2dldFBvcG92ZXJDb250ZW50KCBkYXRhLCBjb25maWcgKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICctLS0tLS0tLS0tIF9nZXRQb3BvdmVyQ29udGVudCgpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19nZXRQb3BvdmVyQ29udGVudDogZGF0YSA9ICcsIGRhdGEgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2dldFBvcG92ZXJDb250ZW50OiBjb25maWcgPSAnLCBjb25maWcgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgdmFyIHdvcmtMaXN0ID0gJyc7XG4gICAgICAgIHZhciB3b3JrTGlzdExpbmsgPSAnJztcbiAgICAgICAgXG4gICAgICAgIHdvcmtMaXN0ICs9ICc8dWwgY2xhc3M9XCJsaXN0XCI+JztcbiAgICAgICAgXG4gICAgICAgICQuZWFjaCggZGF0YSwgZnVuY3Rpb24oIHdvcmtzLCB2YWx1ZXMgKSB7XG4gICAgICAgICAgICB3b3JrTGlzdExpbmsgPSBjb25maWcuYXBwVXJsICsgJ2ltYWdlLycgKyB2YWx1ZXMuUElfVE9QU1RSVUNUICsgJy8nICsgdmFsdWVzLlRIVU1CUEFHRU5PICsgJy8nICsgdmFsdWVzLkxPR0lEICsgJy8nO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICB3b3JrTGlzdCArPSAnPGxpPic7XG4gICAgICAgICAgICB3b3JrTGlzdCArPSAnPGEgaHJlZj1cIicgKyB3b3JrTGlzdExpbmsgKyAnXCI+JztcbiAgICAgICAgICAgIHdvcmtMaXN0ICs9IHZhbHVlcy5MQUJFTDtcbiAgICAgICAgICAgIHdvcmtMaXN0ICs9ICc8L2E+JztcbiAgICAgICAgICAgIHdvcmtMaXN0ICs9ICc8L2xpPic7XG4gICAgICAgIH0gKTtcbiAgICAgICAgXG4gICAgICAgIHdvcmtMaXN0ICs9ICc8L3VsPic7XG4gICAgICAgIFxuICAgICAgICByZXR1cm4gd29ya0xpc3Q7XG4gICAgfVxuICAgIFxuICAgIHJldHVybiB2aWV3ZXI7XG4gICAgXG59ICkoIHZpZXdlckpTIHx8IHt9LCBqUXVlcnkgKTtcbiIsIi8qKlxuICogVGhpcyBmaWxlIGlzIHBhcnQgb2YgdGhlIEdvb2JpIHZpZXdlciAtIGEgY29udGVudCBwcmVzZW50YXRpb24gYW5kIG1hbmFnZW1lbnRcbiAqIGFwcGxpY2F0aW9uIGZvciBkaWdpdGl6ZWQgb2JqZWN0cy5cbiAqIFxuICogVmlzaXQgdGhlc2Ugd2Vic2l0ZXMgZm9yIG1vcmUgaW5mb3JtYXRpb24uIC0gaHR0cDovL3d3dy5pbnRyYW5kYS5jb20gLVxuICogaHR0cDovL2RpZ2l2ZXJzby5jb21cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGZyZWUgc29mdHdhcmU7IHlvdSBjYW4gcmVkaXN0cmlidXRlIGl0IGFuZC9vciBtb2RpZnkgaXQgdW5kZXIgdGhlIHRlcm1zXG4gKiBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYXMgcHVibGlzaGVkIGJ5IHRoZSBGcmVlIFNvZnR3YXJlIEZvdW5kYXRpb247IGVpdGhlclxuICogdmVyc2lvbiAyIG9mIHRoZSBMaWNlbnNlLCBvciAoYXQgeW91ciBvcHRpb24pIGFueSBsYXRlciB2ZXJzaW9uLlxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZGlzdHJpYnV0ZWQgaW4gdGhlIGhvcGUgdGhhdCBpdCB3aWxsIGJlIHVzZWZ1bCwgYnV0IFdJVEhPVVQgQU5ZXG4gKiBXQVJSQU5UWTsgd2l0aG91dCBldmVuIHRoZSBpbXBsaWVkIHdhcnJhbnR5IG9mIE1FUkNIQU5UQUJJTElUWSBvciBGSVRORVNTIEZPUiBBXG4gKiBQQVJUSUNVTEFSIFBVUlBPU0UuIFNlZSB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgZm9yIG1vcmUgZGV0YWlscy5cbiAqIFxuICogWW91IHNob3VsZCBoYXZlIHJlY2VpdmVkIGEgY29weSBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYWxvbmcgd2l0aCB0aGlzXG4gKiBwcm9ncmFtLiBJZiBub3QsIHNlZSA8aHR0cDovL3d3dy5nbnUub3JnL2xpY2Vuc2VzLz4uXG4gKiBcbiAqIE1vZHVsZSB0byByYWlzZSBhbmQgZGVncmFkZSB0aGUgcGFnZSBmb250IHNpemUuXG4gKiBcbiAqIEB2ZXJzaW9uIDMuMi4wXG4gKiBAbW9kdWxlIHZpZXdlckpTLmNoYW5nZUZvbnRTaXplXG4gKiBAcmVxdWlyZXMgalF1ZXJ5XG4gKi9cbnZhciB2aWV3ZXJKUyA9ICggZnVuY3Rpb24oIHZpZXdlciApIHtcbiAgICAndXNlIHN0cmljdCc7XG4gICAgXG4gICAgdmFyIF9kZWJ1ZyA9IGZhbHNlO1xuICAgIHZhciBfcGFyc2VkRm9udFNpemUgPSAwO1xuICAgIHZhciBfY3VyckZvbnRTaXplID0gJyc7XG4gICAgdmFyIF9kZWZhdWx0cyA9IHtcbiAgICAgICAgZm9udERvd25CdG46ICcnLFxuICAgICAgICBmb250VXBCdG46ICcnLFxuICAgICAgICBtYXhGb250U2l6ZTogMTgsXG4gICAgICAgIG1pbkZvbnRTaXplOiAxMixcbiAgICAgICAgYmFzZUZvbnRTaXplOiAnMTRweCdcbiAgICB9O1xuICAgIFxuICAgIHZpZXdlci5jaGFuZ2VGb250U2l6ZSA9IHtcbiAgICAgICAgLyoqXG4gICAgICAgICAqIE1ldGhvZCB3aGljaCBpbml0aWFsaXplcyB0aGUgZm9udCBzaXplIHN3aXRjaGVyLlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCBpbml0XG4gICAgICAgICAqIEBwYXJhbSB7T2JqZWN0fSBjb25maWcgQW4gY29uZmlnIG9iamVjdCB3aGljaCBvdmVyd3JpdGVzIHRoZSBkZWZhdWx0cy5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5mb250RG93bkJ0biBUaGUgSUQvQ2xhc3Mgb2YgdGhlIGZvbnQgZGVncmFkZSBidXR0b24uXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcuZm9udFVwQnRuIFRoZSBJRC9DbGFzcyBvZiB0aGUgZm9udCB1cGdyYWRlIGJ1dHRvbi5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5tYXhGb250U2l6ZSBUaGUgbWF4aW11bSBmb250IHNpemUgdGhlIGRvY3VtZW50IHNob3VsZFxuICAgICAgICAgKiBzY2FsZSB1cC5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5taW5Gb250U2l6ZSBUaGUgbWluaW11bSBmb250IHNpemUgdGhlIGRvY3VtZW50IHNob3VsZFxuICAgICAgICAgKiBzY2FsZSBkb3duLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLmJhc2VGb250U2l6ZSBUaGUgYmFzZSBmb250IHNpemUgb2YgdGhlIEhUTUwtRWxlbWVudC5cbiAgICAgICAgICogQGV4YW1wbGVcbiAgICAgICAgICogXG4gICAgICAgICAqIDxwcmU+XG4gICAgICAgICAqIHZhciBjaGFuZ2VGb250U2l6ZUNvbmZpZyA9IHtcbiAgICAgICAgICogICAgIGZvbnREb3duQnRuOiAnI2ZvbnRTaXplRG93bicsXG4gICAgICAgICAqICAgICBmb250VXBCdG46ICcjZm9udFNpemVVcCcsXG4gICAgICAgICAqICAgICBtYXhGb250U2l6ZTogMTgsXG4gICAgICAgICAqICAgICBtaW5Gb250U2l6ZTogMTRcbiAgICAgICAgICogfTtcbiAgICAgICAgICogXG4gICAgICAgICAqIHZpZXdlckpTLmNoYW5nZUZvbnRTaXplLmluaXQoIGNoYW5nZUZvbnRTaXplQ29uZmlnICk7XG4gICAgICAgICAqIDwvcHJlPlxuICAgICAgICAgKi9cbiAgICAgICAgaW5pdDogZnVuY3Rpb24oIGNvbmZpZyApIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLmNoYW5nZUZvbnRTaXplLmluaXQnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICcjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIuY2hhbmdlRm9udFNpemUuaW5pdDogY29uZmlnIC0gJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCBjb25maWcgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgJC5leHRlbmQoIHRydWUsIF9kZWZhdWx0cywgY29uZmlnICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmICggdmlld2VyLmxvY2FsU3RvcmFnZVBvc3NpYmxlICkge1xuICAgICAgICAgICAgICAgIC8vIHNldCBjdXJyZW50IGZvbnQtc2l6ZVxuICAgICAgICAgICAgICAgIF9zZXRGb250U2l6ZSgpO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIC8vIHNldCBidXR0b24gc3RhdGVcbiAgICAgICAgICAgICAgICBfc2V0QnV0dG9uU3RhdGUoKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAkKCBfZGVmYXVsdHMuZm9udERvd25CdG4gKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgIC8vIHNldCBjdXJyZW50IGZvbnQtc2l6ZVxuICAgICAgICAgICAgICAgICAgICBfY3VyckZvbnRTaXplID0gJCggJ2h0bWwnICkuY3NzKCAnZm9udC1zaXplJyApO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgLy8gc2F2ZSBmb250LXNpemVcbiAgICAgICAgICAgICAgICAgICAgX3NhdmVGb250U2l6ZSggX2N1cnJGb250U2l6ZSApO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgLy8gcGFyc2UgbnVtYmVyIG9mIGZvbnQtc2l6ZVxuICAgICAgICAgICAgICAgICAgICBfcGFyc2VkRm9udFNpemUgPSBfcGFyc2VGb250U2l6ZSggX2N1cnJGb250U2l6ZSApO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgLy8gZGVncmFkZSBmb250LXNpemVcbiAgICAgICAgICAgICAgICAgICAgX2RlZ3JhZGVGb250U2l6ZSggX3BhcnNlZEZvbnRTaXplICk7XG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICQoIF9kZWZhdWx0cy5mb250VXBCdG4gKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgIC8vIHNldCBjdXJyZW50IGZvbnQtc2l6ZVxuICAgICAgICAgICAgICAgICAgICBfY3VyckZvbnRTaXplID0gJCggJ2h0bWwnICkuY3NzKCAnZm9udC1zaXplJyApO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgLy8gc2F2ZSBmb250LXNpemVcbiAgICAgICAgICAgICAgICAgICAgX3NhdmVGb250U2l6ZSggX2N1cnJGb250U2l6ZSApO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgLy8gcGFyc2UgbnVtYmVyIG9mIGZvbnQtc2l6ZVxuICAgICAgICAgICAgICAgICAgICBfcGFyc2VkRm9udFNpemUgPSBfcGFyc2VGb250U2l6ZSggX2N1cnJGb250U2l6ZSApO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgLy8gcmFpc2UgZm9udC1zaXplXG4gICAgICAgICAgICAgICAgICAgIF9yYWlzZUZvbnRTaXplKCBfcGFyc2VkRm9udFNpemUgKTtcbiAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICB9O1xuICAgIFxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB0byBkZWdyYWRlIHRoZSBwYWdlIGZvbnQtc2l6ZS5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9kZWdyYWRlRm9udFNpemVcbiAgICAgKiBAcGFyYW0ge051bWJlcn0gY3VycmVudCBUaGUgY3VycmVudCBmb250LXNpemUgb2YgdGhlIEhUTUwtRWxlbWVudC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfZGVncmFkZUZvbnRTaXplKCBjdXJyZW50ICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfZGVncmFkZUZvbnRTaXplKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2RlZ3JhZGVGb250U2l6ZTogY3VycmVudCA9ICcsIGN1cnJlbnQgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgdmFyIHNpemUgPSBjdXJyZW50O1xuICAgICAgICBzaXplLS07XG4gICAgICAgIFxuICAgICAgICBpZiAoIHNpemUgPj0gX2RlZmF1bHRzLm1pbkZvbnRTaXplICkge1xuICAgICAgICAgICAgJCggX2RlZmF1bHRzLmZvbnREb3duQnRuICkucHJvcCggJ2Rpc2FibGVkJywgZmFsc2UgKTtcbiAgICAgICAgICAgICQoIF9kZWZhdWx0cy5mb250VXBCdG4gKS5wcm9wKCAnZGlzYWJsZWQnLCBmYWxzZSApO1xuICAgICAgICAgICAgJCggJ2h0bWwnICkuY3NzKCAnZm9udC1zaXplJywgc2l6ZSArICdweCcgKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gc2F2ZSBmb250LXNpemVcbiAgICAgICAgICAgIF9zYXZlRm9udFNpemUoIHNpemUgKyAncHgnICk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAkKCBfZGVmYXVsdHMuZm9udERvd25CdG4gKS5wcm9wKCAnZGlzYWJsZWQnLCB0cnVlICk7XG4gICAgICAgICAgICAkKCBfZGVmYXVsdHMuZm9udFVwQnRuICkucHJvcCggJ2Rpc2FibGVkJywgZmFsc2UgKTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBcbiAgICAvKipcbiAgICAgKiBNZXRob2QgdG8gcmFpc2UgdGhlIHBhZ2UgZm9udC1zaXplLlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX3JhaXNlRm9udFNpemVcbiAgICAgKiBAcGFyYW0ge051bWJlcn0gY3VycmVudCBUaGUgY3VycmVudCBmb250LXNpemUgb2YgdGhlIEhUTUwtRWxlbWVudC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfcmFpc2VGb250U2l6ZSggY3VycmVudCApIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX3JhaXNlRm9udFNpemUoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfcmFpc2VGb250U2l6ZTogY3VycmVudCA9ICcsIGN1cnJlbnQgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgdmFyIHNpemUgPSBjdXJyZW50O1xuICAgICAgICBzaXplKys7XG4gICAgICAgIFxuICAgICAgICBpZiAoIHNpemUgPD0gX2RlZmF1bHRzLm1heEZvbnRTaXplICkge1xuICAgICAgICAgICAgJCggX2RlZmF1bHRzLmZvbnREb3duQnRuICkucHJvcCggJ2Rpc2FibGVkJywgZmFsc2UgKTtcbiAgICAgICAgICAgICQoIF9kZWZhdWx0cy5mb250VXBCdG4gKS5wcm9wKCAnZGlzYWJsZWQnLCBmYWxzZSApO1xuICAgICAgICAgICAgJCggJ2h0bWwnICkuY3NzKCAnZm9udC1zaXplJywgc2l6ZSArICdweCcgKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gc2F2ZSBmb250LXNpemVcbiAgICAgICAgICAgIF9zYXZlRm9udFNpemUoIHNpemUgKyAncHgnICk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAkKCBfZGVmYXVsdHMuZm9udERvd25CdG4gKS5wcm9wKCAnZGlzYWJsZWQnLCBmYWxzZSApO1xuICAgICAgICAgICAgJCggX2RlZmF1bHRzLmZvbnRVcEJ0biApLnByb3AoICdkaXNhYmxlZCcsIHRydWUgKTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBcbiAgICAvKipcbiAgICAgKiBNZXRob2Qgd2hpY2ggcGFyc2VzIGEgZ2l2ZW4gcGl4ZWwgdmFsdWUgdG8gYSBudW1iZXIgYW5kIHJldHVybnMgaXQuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfcGFyc2VGb250U2l6ZVxuICAgICAqIEBwYXJhbSB7U3RyaW5nfSBzdHJpbmcgVGhlIHN0cmluZyB0byBwYXJzZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfcGFyc2VGb250U2l6ZSggc3RyaW5nICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfcGFyc2VGb250U2l6ZSgpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19wYXJzZUZvbnRTaXplOiBzdHJpbmcgPSAnLCBzdHJpbmcgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgcmV0dXJuIHBhcnNlSW50KCBzdHJpbmcucmVwbGFjZSggJ3B4JyApICk7XG4gICAgfVxuICAgIFxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB0byBzYXZlIHRoZSBjdXJyZW50IGZvbnQtc2l6ZSB0byBsb2NhbCBzdG9yYWdlIGFzIGEgc3RyaW5nLlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX3NhdmVGb250U2l6ZVxuICAgICAqIEBwYXJhbSB7U3RyaW5nfSBzaXplIFRoZSBTdHJpbmcgdG8gc2F2ZSBpbiBsb2NhbCBzdG9yYWdlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9zYXZlRm9udFNpemUoIHNpemUgKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICctLS0tLS0tLS0tIF9zYXZlRm9udFNpemUoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfcGFyc2VGb250U2l6ZTogc2l6ZSA9ICcsIHNpemUgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgbG9jYWxTdG9yYWdlLnNldEl0ZW0oICdjdXJyZW50Rm9udFNpemUnLCBzaXplICk7XG4gICAgfVxuICAgIFxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB0byBzZXQgdGhlIGN1cnJlbnQgZm9udC1zaXplIGZyb20gbG9jYWwgc3RvcmFnZSB0byB0aGUgSFRNTC1FbGVtZW50LlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX3NldEZvbnRTaXplXG4gICAgICovXG4gICAgZnVuY3Rpb24gX3NldEZvbnRTaXplKCkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfc2V0Rm9udFNpemUoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICB9XG4gICAgICAgIHZhciBmb250U2l6ZSA9IGxvY2FsU3RvcmFnZS5nZXRJdGVtKCAnY3VycmVudEZvbnRTaXplJyApO1xuICAgICAgICBcbiAgICAgICAgaWYgKCBmb250U2l6ZSA9PT0gbnVsbCB8fCBmb250U2l6ZSA9PT0gJycgKSB7XG4gICAgICAgICAgICBsb2NhbFN0b3JhZ2Uuc2V0SXRlbSggJ2N1cnJlbnRGb250U2l6ZScsIF9kZWZhdWx0cy5iYXNlRm9udFNpemUgKTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICQoICdodG1sJyApLmNzcyggJ2ZvbnQtc2l6ZScsIGZvbnRTaXplICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgfVxuICAgIFxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB0byBzZXQgdGhlIHN0YXRlIG9mIHRoZSBmb250LXNpemUgY2hhbmdlIGJ1dHRvbnMuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfc2V0QnV0dG9uU3RhdGVcbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfc2V0QnV0dG9uU3RhdGUoKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICctLS0tLS0tLS0tIF9zZXRCdXR0b25TdGF0ZSgpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgIH1cbiAgICAgICAgdmFyIGZvbnRTaXplID0gbG9jYWxTdG9yYWdlLmdldEl0ZW0oICdjdXJyZW50Rm9udFNpemUnICk7XG4gICAgICAgIHZhciBuZXdGb250U2l6ZSA9IF9wYXJzZUZvbnRTaXplKCBmb250U2l6ZSApO1xuICAgICAgICBcbiAgICAgICAgaWYgKCBuZXdGb250U2l6ZSA9PT0gX2RlZmF1bHRzLm1pbkZvbnRTaXplICkge1xuICAgICAgICAgICAgJCggX2RlZmF1bHRzLmZvbnREb3duQnRuICkucHJvcCggJ2Rpc2FibGVkJywgdHJ1ZSApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICBpZiAoIG5ld0ZvbnRTaXplID09PSBfZGVmYXVsdHMubWF4Rm9udFNpemUgKSB7XG4gICAgICAgICAgICAkKCBfZGVmYXVsdHMuZm9udFVwQnRuICkucHJvcCggJ2Rpc2FibGVkJywgdHJ1ZSApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgIH1cbiAgICBcbiAgICByZXR1cm4gdmlld2VyO1xuICAgIFxufSApKCB2aWV3ZXJKUyB8fCB7fSwgalF1ZXJ5ICk7XG4iLCIvKipcbiAqIFRoaXMgZmlsZSBpcyBwYXJ0IG9mIHRoZSBHb29iaSB2aWV3ZXIgLSBhIGNvbnRlbnQgcHJlc2VudGF0aW9uIGFuZCBtYW5hZ2VtZW50XG4gKiBhcHBsaWNhdGlvbiBmb3IgZGlnaXRpemVkIG9iamVjdHMuXG4gKiBcbiAqIFZpc2l0IHRoZXNlIHdlYnNpdGVzIGZvciBtb3JlIGluZm9ybWF0aW9uLiAtIGh0dHA6Ly93d3cuaW50cmFuZGEuY29tIC1cbiAqIGh0dHA6Ly9kaWdpdmVyc28uY29tXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBmcmVlIHNvZnR3YXJlOyB5b3UgY2FuIHJlZGlzdHJpYnV0ZSBpdCBhbmQvb3IgbW9kaWZ5IGl0IHVuZGVyIHRoZSB0ZXJtc1xuICogb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFzIHB1Ymxpc2hlZCBieSB0aGUgRnJlZSBTb2Z0d2FyZSBGb3VuZGF0aW9uOyBlaXRoZXJcbiAqIHZlcnNpb24gMiBvZiB0aGUgTGljZW5zZSwgb3IgKGF0IHlvdXIgb3B0aW9uKSBhbnkgbGF0ZXIgdmVyc2lvbi5cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGRpc3RyaWJ1dGVkIGluIHRoZSBob3BlIHRoYXQgaXQgd2lsbCBiZSB1c2VmdWwsIGJ1dCBXSVRIT1VUIEFOWVxuICogV0FSUkFOVFk7IHdpdGhvdXQgZXZlbiB0aGUgaW1wbGllZCB3YXJyYW50eSBvZiBNRVJDSEFOVEFCSUxJVFkgb3IgRklUTkVTUyBGT1IgQVxuICogUEFSVElDVUxBUiBQVVJQT1NFLiBTZWUgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGZvciBtb3JlIGRldGFpbHMuXG4gKiBcbiAqIFlvdSBzaG91bGQgaGF2ZSByZWNlaXZlZCBhIGNvcHkgb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFsb25nIHdpdGggdGhpc1xuICogcHJvZ3JhbS4gSWYgbm90LCBzZWUgPGh0dHA6Ly93d3cuZ251Lm9yZy9saWNlbnNlcy8+LlxuICogXG4gKiBNb2R1bGUgdG8gbWFuYWdlIHRoZSBkYXRhIHRhYmxlIGZlYXR1cmVzLlxuICogXG4gKiBAdmVyc2lvbiAzLjIuMFxuICogQG1vZHVsZSB2aWV3ZXJKUy5kYXRhVGFibGVcbiAqIEByZXF1aXJlcyBqUXVlcnlcbiAqL1xudmFyIHZpZXdlckpTID0gKCBmdW5jdGlvbiggdmlld2VyICkge1xuICAgICd1c2Ugc3RyaWN0JztcbiAgICBcbiAgICB2YXIgX2RlYnVnID0gZmFsc2U7XG4gICAgdmFyIF9kYXRhVGFibGVQYWdpbmF0b3IgPSBudWxsO1xuICAgIHZhciBfdHh0RmllbGQxID0gbnVsbDtcbiAgICB2YXIgX3R4dEZpZWxkMiA9IG51bGw7XG4gICAgdmFyIF90b3RhbENvdW50ID0gbnVsbDtcbiAgICB2YXIgX3JlbG9hZEJ0biA9IG51bGw7XG4gICAgdmFyIF9kZWZhdWx0cyA9IHtcbiAgICAgICAgZGF0YVRhYmxlUGFnaW5hdG9yOiAnJyxcbiAgICAgICAgdHh0RmllbGQxOiAnJyxcbiAgICAgICAgdHh0RmllbGQyOiAnJyxcbiAgICAgICAgdG90YWxDb3VudDogJycsXG4gICAgICAgIHJlbG9hZEJ0bjogJycsXG4gICAgfTtcbiAgICBcbiAgICB2aWV3ZXIuZGF0YVRhYmxlID0ge1xuICAgICAgICBpbml0OiBmdW5jdGlvbiggY29uZmlnICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICcjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIuZGF0YVRhYmxlLmluaXQnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICcjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIuZGF0YVRhYmxlLmluaXQ6IGNvbmZpZyA9ICcsIGNvbmZpZyApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICAkLmV4dGVuZCggdHJ1ZSwgX2RlZmF1bHRzLCBjb25maWcgKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgdmlld2VyLmRhdGFUYWJsZS5wYWdpbmF0b3IuaW5pdCgpO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBpZiAoICQoICcuY29sdW1uLWZpbHRlci13cmFwcGVyJyApLmxlbmd0aCA+IDAgKSB7XG4gICAgICAgICAgICAgICAgdmlld2VyLmRhdGFUYWJsZS5maWx0ZXIuaW5pdCgpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9LFxuICAgICAgICAvKipcbiAgICAgICAgICogUGFnaW5hdGlvblxuICAgICAgICAgKi9cbiAgICAgICAgcGFnaW5hdG9yOiB7XG4gICAgICAgICAgICBzZXR1cEFqYXg6IGZhbHNlLFxuICAgICAgICAgICAgaW5pdDogZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBkYXRhVGFibGUucGFnaW5hdG9yLmluaXQoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICBfZGF0YVRhYmxlUGFnaW5hdG9yID0gJCggX2RlZmF1bHRzLmRhdGFUYWJsZVBhZ2luYXRvciApO1xuICAgICAgICAgICAgICAgIF90eHRGaWVsZDEgPSAkKCBfZGVmYXVsdHMudHh0RmllbGQxICk7XG4gICAgICAgICAgICAgICAgX3R4dEZpZWxkMiA9ICQoIF9kZWZhdWx0cy50eHRGaWVsZDIgKTtcbiAgICAgICAgICAgICAgICBfdG90YWxDb3VudCA9ICQoIF9kZWZhdWx0cy50b3RhbENvdW50ICk7XG4gICAgICAgICAgICAgICAgX3JlbG9hZEJ0biA9ICQoIF9kZWZhdWx0cy5yZWxvYWRCdG4gKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICBfdHh0RmllbGQxLm9uKCAnY2xpY2snLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgJCggdGhpcyApLmhpZGUoKTtcbiAgICAgICAgICAgICAgICAgICAgdmlld2VyLmRhdGFUYWJsZS5wYWdpbmF0b3IuaW5wdXRGaWVsZEhhbmRsZXIoKTtcbiAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgX3RvdGFsQ291bnQub24oICdjbGljaycsIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICBfdHh0RmllbGQxLmhpZGUoKTtcbiAgICAgICAgICAgICAgICAgICAgdmlld2VyLmRhdGFUYWJsZS5wYWdpbmF0b3IuaW5wdXRGaWVsZEhhbmRsZXIoKTtcbiAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLypcbiAgICAgICAgICAgICAgICAgKiBBSkFYIEV2ZW50bGlzdGVuZXJcbiAgICAgICAgICAgICAgICAgKi9cbiAgICAgICAgICAgICAgICBpZiAoICF0aGlzLnNldHVwQWpheCApIHtcbiAgICAgICAgICAgICAgICAgICAganNmLmFqYXguYWRkT25FdmVudCggZnVuY3Rpb24oIGRhdGEgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICB2YXIgYWpheHN0YXR1cyA9IGRhdGEuc3RhdHVzO1xuICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAoIF9kZWZhdWx0cy5kYXRhVGFibGVQYWdpbmF0b3IubGVuZ3RoID4gMCApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBzd2l0Y2ggKCBhamF4c3RhdHVzICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBjYXNlIFwiYmVnaW5cIjpcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGlmICggX3R4dEZpZWxkMSAhPT0gbnVsbCAmJiBfdHh0RmllbGQyICE9PSBudWxsICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIF90eHRGaWVsZDEub2ZmKCk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgX3R4dEZpZWxkMi5vZmYoKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBjYXNlIFwiY29tcGxldGVcIjpcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBjYXNlIFwic3VjY2Vzc1wiOlxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdmlld2VyLmRhdGFUYWJsZS5wYWdpbmF0b3IuaW5pdCgpO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgICAgIHRoaXMuc2V0dXBBamF4ID0gdHJ1ZTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9LFxuICAgICAgICAgICAgaW5wdXRGaWVsZEhhbmRsZXI6IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gZGF0YVRhYmxlLnBhZ2luYXRvci5pbnB1dEZpZWxkSGFuZGxlcigpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIF90eHRGaWVsZDIuc2hvdygpLmZpbmQoICdpbnB1dCcgKS5mb2N1cygpLnNlbGVjdCgpO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIF90eHRGaWVsZDIuZmluZCggJ2lucHV0JyApLm9uKCAnYmx1cicsIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICAkKCB0aGlzICkuaGlkZSgpO1xuICAgICAgICAgICAgICAgICAgICBfdHh0RmllbGQxLnNob3coKTtcbiAgICAgICAgICAgICAgICAgICAgX3JlbG9hZEJ0bi5jbGljaygpO1xuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICBfdHh0RmllbGQyLmZpbmQoICdpbnB1dCcgKS5vbiggJ2tleXByZXNzJywgZnVuY3Rpb24oIGV2ZW50ICkge1xuICAgICAgICAgICAgICAgICAgICBpZiAoIGV2ZW50LmtleUNvZGUgPT0gMTMgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBfcmVsb2FkQnRuLmNsaWNrKCk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICB9LFxuICAgICAgICB9LFxuICAgICAgICAvKipcbiAgICAgICAgICogRmlsdGVyXG4gICAgICAgICAqL1xuICAgICAgICBmaWx0ZXI6IHtcbiAgICAgICAgICAgIHNldHVwQWpheDogZmFsc2UsXG4gICAgICAgICAgICBpbml0OiBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICAgICAgY29uc29sZS5sb2coICctLS0tLS0tLS0tIGRhdGFUYWJsZS5maWx0ZXIuaW5pdCgpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICQoICcjYWRtaW5BbGxVc2VyRm9ybScgKS5vbiggJ3N1Ym1pdCcsIGZ1bmN0aW9uKCBldmVudCApIHtcbiAgICAgICAgICAgICAgICAgICAgZXZlbnQucHJldmVudERlZmF1bHQoKTtcbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICQoICcuY29sdW1uLWZpbHRlci13cmFwcGVyJyApLmZpbmQoICcuYnRuLWZpbHRlcicgKS5jbGljaygpO1xuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAvKlxuICAgICAgICAgICAgICAgICAqIEFKQVggRXZlbnRsaXN0ZW5lclxuICAgICAgICAgICAgICAgICAqL1xuICAgICAgICAgICAgICAgIGlmICggIXRoaXMuc2V0dXBBamF4ICkge1xuICAgICAgICAgICAgICAgICAgICBqc2YuYWpheC5hZGRPbkV2ZW50KCBmdW5jdGlvbiggZGF0YSApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHZhciBhamF4c3RhdHVzID0gZGF0YS5zdGF0dXM7XG4gICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICggX2RlZmF1bHRzLmRhdGFUYWJsZVBhZ2luYXRvci5sZW5ndGggPiAwICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHN3aXRjaCAoIGFqYXhzdGF0dXMgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNhc2UgXCJiZWdpblwiOlxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNhc2UgXCJjb21wbGV0ZVwiOlxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNhc2UgXCJzdWNjZXNzXCI6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB2aWV3ZXIuZGF0YVRhYmxlLmZpbHRlci5pbml0KCk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5zZXR1cEFqYXggPSB0cnVlO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH0sXG4gICAgICAgIH0sXG4gICAgfTtcbiAgICBcbiAgICByZXR1cm4gdmlld2VyO1xuICAgIFxufSApKCB2aWV3ZXJKUyB8fCB7fSwgalF1ZXJ5ICk7XG4iLCIvKipcbiAqIFRoaXMgZmlsZSBpcyBwYXJ0IG9mIHRoZSBHb29iaSB2aWV3ZXIgLSBhIGNvbnRlbnQgcHJlc2VudGF0aW9uIGFuZCBtYW5hZ2VtZW50XG4gKiBhcHBsaWNhdGlvbiBmb3IgZGlnaXRpemVkIG9iamVjdHMuXG4gKiBcbiAqIFZpc2l0IHRoZXNlIHdlYnNpdGVzIGZvciBtb3JlIGluZm9ybWF0aW9uLiAtIGh0dHA6Ly93d3cuaW50cmFuZGEuY29tIC1cbiAqIGh0dHA6Ly9kaWdpdmVyc28uY29tXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBmcmVlIHNvZnR3YXJlOyB5b3UgY2FuIHJlZGlzdHJpYnV0ZSBpdCBhbmQvb3IgbW9kaWZ5IGl0IHVuZGVyIHRoZSB0ZXJtc1xuICogb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFzIHB1Ymxpc2hlZCBieSB0aGUgRnJlZSBTb2Z0d2FyZSBGb3VuZGF0aW9uOyBlaXRoZXJcbiAqIHZlcnNpb24gMiBvZiB0aGUgTGljZW5zZSwgb3IgKGF0IHlvdXIgb3B0aW9uKSBhbnkgbGF0ZXIgdmVyc2lvbi5cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGRpc3RyaWJ1dGVkIGluIHRoZSBob3BlIHRoYXQgaXQgd2lsbCBiZSB1c2VmdWwsIGJ1dCBXSVRIT1VUIEFOWVxuICogV0FSUkFOVFk7IHdpdGhvdXQgZXZlbiB0aGUgaW1wbGllZCB3YXJyYW50eSBvZiBNRVJDSEFOVEFCSUxJVFkgb3IgRklUTkVTUyBGT1IgQVxuICogUEFSVElDVUxBUiBQVVJQT1NFLiBTZWUgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGZvciBtb3JlIGRldGFpbHMuXG4gKiBcbiAqIFlvdSBzaG91bGQgaGF2ZSByZWNlaXZlZCBhIGNvcHkgb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFsb25nIHdpdGggdGhpc1xuICogcHJvZ3JhbS4gSWYgbm90LCBzZWUgPGh0dHA6Ly93d3cuZ251Lm9yZy9saWNlbnNlcy8+LlxuICogXG4gKiBNb2R1bGUgdG8gcmVuZGVyIGEgUlNTLUZlZWQgc29ydGVkIGJ5IGRhdGUuXG4gKiBcbiAqIEB2ZXJzaW9uIDMuMi4wXG4gKiBAbW9kdWxlIHZpZXdlckpTLmRhdGVTb3J0ZWRGZWVkXG4gKiBAcmVxdWlyZXMgalF1ZXJ5XG4gKiBcbiAqL1xudmFyIHZpZXdlckpTID0gKCBmdW5jdGlvbiggdmlld2VyICkge1xuICAgICd1c2Ugc3RyaWN0JztcbiAgICBcbiAgICB2YXIgX2RlYnVnID0gZmFsc2U7XG4gICAgdmFyIF9wcm9taXNlID0gbnVsbDtcbiAgICB2YXIgX2RlZmF1bHRzID0ge1xuICAgICAgICBwYXRoOiBudWxsLFxuICAgICAgICBkYXRhU29ydE9yZGVyOiBudWxsLFxuICAgICAgICBkYXRhQ291bnQ6IG51bGwsXG4gICAgICAgIGRhdGFFbmNvZGluZzogbnVsbCxcbiAgICAgICAgZmVlZEJveDogbnVsbCxcbiAgICAgICAgbW9udGhOYW1lczogWyAnJywgJ0phbnVhcicsICdGZWJydWFyJywgJ03DpHJ6JywgJ0FwcmlsJywgJ01haScsICdKdW5pJywgJ0p1bGknLCAnQXVndXN0JywgJ1NlcHRlbWJlcicsICdPa3RvYmVyJywgJ05vdmVtYmVyJywgJ0RlemVtYmVyJyBdXG4gICAgfTtcbiAgICBcbiAgICB2aWV3ZXIuZGF0ZVNvcnRlZEZlZWQgPSB7XG4gICAgICAgIC8qKlxuICAgICAgICAgKiBNZXRob2Qgd2hpY2ggaW5pdGlhbGl6ZXMgdGhlIGRhdGUgc29ydGVkIFJTUy1GZWVkLlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCBpbml0XG4gICAgICAgICAqIEBwYXJhbSB7T2JqZWN0fSBjb25maWcgQW4gY29uZmlnIG9iamVjdCB3aGljaCBvdmVyd3JpdGVzIHRoZSBkZWZhdWx0cy5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5wYXRoIFRoZSByb290cGF0aCBvZiB0aGUgYXBwbGljYXRpb24uXG4gICAgICAgICAqIEBwYXJhbSB7T2JqZWN0fSBjb25maWcuZmVlZEJveCBBbiBqUXVlcnkgb2JqZWN0IG9mIHRoZSB3cmFwcGVyIERJVi5cbiAgICAgICAgICogQGV4YW1wbGVcbiAgICAgICAgICogXG4gICAgICAgICAqIDxwcmU+XG4gICAgICAgICAqIHZhciBkYXRlU29ydGVkRmVlZENvbmZpZyA9IHtcbiAgICAgICAgICogICAgIHBhdGg6ICcje3JlcXVlc3QuY29udGV4dFBhdGh9JyxcbiAgICAgICAgICogICAgIGZlZWRCb3g6ICQoICcjZGF0ZVNvcnRlZEZlZWQnIClcbiAgICAgICAgICogfTtcbiAgICAgICAgICogXG4gICAgICAgICAqIHZpZXdlckpTLmRhdGVTb3J0ZWRGZWVkLnNldERhdGFTb3J0T3JkZXIoICcje2NjLmF0dHJzLnNvcnRpbmd9JyApO1xuICAgICAgICAgKiB2aWV3ZXJKUy5kYXRlU29ydGVkRmVlZC5zZXREYXRhQ291bnQoICcje2NjLmF0dHJzLmNvdW50fScgKTtcbiAgICAgICAgICogdmlld2VySlMuZGF0ZVNvcnRlZEZlZWQuc2V0RGF0YUVuY29kaW5nKCAnI3tjYy5hdHRycy5lbmNvZGluZ30nICk7XG4gICAgICAgICAqIHZpZXdlckpTLmRhdGVTb3J0ZWRGZWVkLmluaXQoIGRhdGVTb3J0ZWRGZWVkQ29uZmlnICk7XG4gICAgICAgICAqIDwvcHJlPlxuICAgICAgICAgKi9cbiAgICAgICAgaW5pdDogZnVuY3Rpb24oIGNvbmZpZyApIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLmRhdGVTb3J0ZWRGZWVkLmluaXQnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICcjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIuZGF0ZVNvcnRlZEZlZWQuaW5pdDogZmVlZEJveE9iaiAtICcgKyBmZWVkQm94T2JqICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIuZGF0ZVNvcnRlZEZlZWQuaW5pdDogcGF0aCAtICcgKyBwYXRoICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgICQuZXh0ZW5kKCB0cnVlLCBfZGVmYXVsdHMsIGNvbmZpZyApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICB2YXIgZGF0YVVSTCA9IF9kZWZhdWx0cy5wYXRoO1xuICAgICAgICAgICAgZGF0YVVSTCArPSAnL2FwaT9hY3Rpb249cXVlcnkmcT1QSToqJmpzb25Gb3JtYXQ9ZGF0ZWNlbnRyaWMmc29ydEZpZWxkPURBVEVDUkVBVEVEJztcbiAgICAgICAgICAgIGRhdGFVUkwgKz0gJyZzb3J0T3JkZXI9JztcbiAgICAgICAgICAgIGRhdGFVUkwgKz0gX2RlZmF1bHRzLmRhdGFTb3J0T3JkZXI7XG4gICAgICAgICAgICBkYXRhVVJMICs9ICcmY291bnQ9JztcbiAgICAgICAgICAgIGRhdGFVUkwgKz0gX2RlZmF1bHRzLmRhdGFDb3VudDtcbiAgICAgICAgICAgIGRhdGFVUkwgKz0gJyZlbmNvZGluZz0nO1xuICAgICAgICAgICAgZGF0YVVSTCArPSBfZGVmYXVsdHMuZGF0YUVuY29kaW5nO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci5kYXRlU29ydGVkRmVlZC5pbml0OiBkYXRhVVJMIC0gJyArIGRhdGFVUkwgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gY2hlY2tpbmcgZm9yIGZlZWRib3ggZWxlbWVudCBhbmQgcmVuZGVyIGZlZWRcbiAgICAgICAgICAgIGlmICggX2RlZmF1bHRzLmZlZWRCb3ggKSB7XG4gICAgICAgICAgICAgICAgX3Byb21pc2UgPSB2aWV3ZXIuaGVscGVyLmdldFJlbW90ZURhdGEoIGRhdGFVUkwgKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICBfcHJvbWlzZS50aGVuKCBmdW5jdGlvbiggZGF0YSApIHtcbiAgICAgICAgICAgICAgICAgICAgX3JlbmRlckZlZWQoIGRhdGEgKTtcbiAgICAgICAgICAgICAgICB9ICkudGhlbiggbnVsbCwgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgIGNvbnNvbGUuZXJyb3IoICdFUlJPUjogdmlld2VyLmRhdGVTb3J0ZWRGZWVkLmluaXQgLSAnLCBlcnJvciApO1xuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSxcbiAgICAgICAgLyoqXG4gICAgICAgICAqIFJldHVybnMgdGhlIHNvcnRpbmcgb3JkZXIgb2YgdGhlIGZlZWQuXG4gICAgICAgICAqIFxuICAgICAgICAgKiBAbWV0aG9kIGdldERhdGFTb3J0T3JkZXJcbiAgICAgICAgICogQHJldHVybnMge1N0cmluZ30gVGhlIHNvcnRpbmcgb3JkZXIuXG4gICAgICAgICAqIFxuICAgICAgICAgKi9cbiAgICAgICAgZ2V0RGF0YVNvcnRPcmRlcjogZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICByZXR1cm4gX2RlZmF1bHRzLmRhdGFTb3J0T3JkZXI7XG4gICAgICAgIH0sXG4gICAgICAgIC8qKlxuICAgICAgICAgKiBTZXRzIHRoZSBzb3J0aW5nIG9yZGVyIG9mIHRoZSBmZWVkLlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCBnZXREYXRhU29ydE9yZGVyXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBzdHIgVGhlIHNvcnRpbmcgb3JkZXIgKGFzYy9kZXNjKVxuICAgICAgICAgKiBcbiAgICAgICAgICovXG4gICAgICAgIHNldERhdGFTb3J0T3JkZXI6IGZ1bmN0aW9uKCBzdHIgKSB7XG4gICAgICAgICAgICBfZGVmYXVsdHMuZGF0YVNvcnRPcmRlciA9IHN0cjtcbiAgICAgICAgfSxcbiAgICAgICAgLyoqXG4gICAgICAgICAqIFJldHVybnMgdGhlIG51bWJlciBvZiBlbnRyaWVzIGZyb20gdGhlIGZlZWQuXG4gICAgICAgICAqIFxuICAgICAgICAgKiBAbWV0aG9kIGdldERhdGFTb3J0T3JkZXJcbiAgICAgICAgICogQHJldHVybnMge1N0cmluZ30gVGhlIG51bWJlciBvZiBlbnRyaWVzLlxuICAgICAgICAgKiBcbiAgICAgICAgICovXG4gICAgICAgIGdldERhdGFDb3VudDogZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICByZXR1cm4gX2RlZmF1bHRzLmRhdGFDb3VudDtcbiAgICAgICAgfSxcbiAgICAgICAgLyoqXG4gICAgICAgICAqIFNldHMgdGhlIG51bWJlciBvZiBlbnRyaWVzIGZyb20gdGhlIGZlZWQuXG4gICAgICAgICAqIFxuICAgICAgICAgKiBAbWV0aG9kIHNldERhdGFDb3VudFxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gVGhlIG51bWJlciBvZiBlbnRyaWVzLlxuICAgICAgICAgKiBcbiAgICAgICAgICovXG4gICAgICAgIHNldERhdGFDb3VudDogZnVuY3Rpb24oIG51bSApIHtcbiAgICAgICAgICAgIF9kZWZhdWx0cy5kYXRhQ291bnQgPSBudW07XG4gICAgICAgIH0sXG4gICAgICAgIC8qKlxuICAgICAgICAgKiBSZXR1cm5zIHRoZSB0eXBlIG9mIGVuY29kaW5nLlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCBnZXREYXRhRW5jb2RpbmdcbiAgICAgICAgICogQHJldHVybnMge1N0cmluZ30gVGhlIHR5cGUgb2YgZW5jb2RpbmcuXG4gICAgICAgICAqIFxuICAgICAgICAgKi9cbiAgICAgICAgZ2V0RGF0YUVuY29kaW5nOiBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIHJldHVybiBfZGVmYXVsdHMuZGF0YUVuY29kaW5nO1xuICAgICAgICB9LFxuICAgICAgICAvKipcbiAgICAgICAgICogU2V0cyB0aGUgdHlwZSBvZiBlbmNvZGluZy5cbiAgICAgICAgICogXG4gICAgICAgICAqIEBtZXRob2Qgc2V0RGF0YUVuY29kaW5nXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBUaGUgdHlwZSBvZiBlbmNvZGluZy5cbiAgICAgICAgICogXG4gICAgICAgICAqL1xuICAgICAgICBzZXREYXRhRW5jb2Rpbmc6IGZ1bmN0aW9uKCBzdHIgKSB7XG4gICAgICAgICAgICBfZGVmYXVsdHMuZGF0YUVuY29kaW5nID0gc3RyO1xuICAgICAgICB9XG4gICAgfTtcbiAgICBcbiAgICAvKipcbiAgICAgKiBSZW5kZXJzIHRoZSBmZWVkIGFuZCBhcHBlbmRzIGl0IHRvIHRoZSB3cmFwcGVyLlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX3JlbmRlckZlZWRcbiAgICAgKiBAcGFyYW0ge09iamVjdH0gZGF0YSBBbiBKU09OIG9iamVjdCBvZiB0aGUgZmVlZCBkYXRhLlxuICAgICAqIFxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9yZW5kZXJGZWVkKCBkYXRhICkge1xuICAgICAgICB2YXIgZmVlZCA9ICcnO1xuICAgICAgICAkLmVhY2goIGRhdGEsIGZ1bmN0aW9uKCBpLCBqICkge1xuICAgICAgICAgICAgZmVlZCArPSAnPGg0PicgKyBfZGF0ZUNvbnZlcnRlciggai5kYXRlICkgKyAnPC9oND4nO1xuICAgICAgICAgICAgJC5lYWNoKCBqLCBmdW5jdGlvbiggbSwgbiApIHtcbiAgICAgICAgICAgICAgICBpZiAoIG4udGl0bGUgKSB7XG4gICAgICAgICAgICAgICAgICAgIGZvciAoIHZhciB4ID0gMDsgeCA8PSBuLnRpdGxlLmxlbmd0aDsgeCsrICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKCBuLnRpdGxlWyB4IF0gIT09IHVuZGVmaW5lZCApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBmZWVkICs9ICc8ZGl2IGNsYXNzPVwic29ydGVkLWZlZWQtdGl0bGVcIj48YSBocmVmPVwiJztcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBmZWVkICs9IG4udXJsO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGZlZWQgKz0gJ1wiIHRpdGxlPVwiJztcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBmZWVkICs9IG4udGl0bGVbIHggXTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBmZWVkICs9ICdcIj4nO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGZlZWQgKz0gbi50aXRsZVsgeCBdO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGZlZWQgKz0gJzwvYT48L2Rpdj4nO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfSApO1xuICAgICAgICB9ICk7XG4gICAgICAgIFxuICAgICAgICBfZGVmYXVsdHMuZmVlZEJveC5hcHBlbmQoIGZlZWQgKTtcbiAgICB9XG4gICAgXG4gICAgLyoqXG4gICAgICogQ29udmVydHMgYSBkYXRlIHRvIHRoaXMgZm9ybTogMTYuIE5vdmVtYmVyIDIwMTVcbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9kYXRlQ29udmVydGVyXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IHN0ciBUaGUgZGF0ZSB0byBjb252ZXJ0LlxuICAgICAqIEByZXR1cm5zIHtTdHJpbmd9IFRoZSBuZXcgZm9ybWF0ZWQgZGF0ZS5cbiAgICAgKiBcbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfZGF0ZUNvbnZlcnRlciggc3RyICkge1xuICAgICAgICB2YXIgc3RyQXJyID0gc3RyLnNwbGl0KCAnLScgKTtcbiAgICAgICAgdmFyIG1vbnRoSWR4ID0gcGFyc2VJbnQoIHN0ckFyclsgMSBdICk7XG4gICAgICAgIHZhciBuZXdEYXRlID0gc3RyQXJyWyAyIF0gKyAnLiAnICsgX2RlZmF1bHRzLm1vbnRoTmFtZXNbIG1vbnRoSWR4IF0gKyAnICcgKyBzdHJBcnJbIDAgXTtcbiAgICAgICAgXG4gICAgICAgIHJldHVybiBuZXdEYXRlO1xuICAgIH1cbiAgICBcbiAgICByZXR1cm4gdmlld2VyO1xuICAgIFxufSApKCB2aWV3ZXJKUyB8fCB7fSwgalF1ZXJ5ICk7XG4iLCIvKipcbiAqIFRoaXMgZmlsZSBpcyBwYXJ0IG9mIHRoZSBHb29iaSB2aWV3ZXIgLSBhIGNvbnRlbnQgcHJlc2VudGF0aW9uIGFuZCBtYW5hZ2VtZW50XG4gKiBhcHBsaWNhdGlvbiBmb3IgZGlnaXRpemVkIG9iamVjdHMuXG4gKiBcbiAqIFZpc2l0IHRoZXNlIHdlYnNpdGVzIGZvciBtb3JlIGluZm9ybWF0aW9uLiAtIGh0dHA6Ly93d3cuaW50cmFuZGEuY29tIC1cbiAqIGh0dHA6Ly9kaWdpdmVyc28uY29tXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBmcmVlIHNvZnR3YXJlOyB5b3UgY2FuIHJlZGlzdHJpYnV0ZSBpdCBhbmQvb3IgbW9kaWZ5IGl0IHVuZGVyIHRoZSB0ZXJtc1xuICogb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFzIHB1Ymxpc2hlZCBieSB0aGUgRnJlZSBTb2Z0d2FyZSBGb3VuZGF0aW9uOyBlaXRoZXJcbiAqIHZlcnNpb24gMiBvZiB0aGUgTGljZW5zZSwgb3IgKGF0IHlvdXIgb3B0aW9uKSBhbnkgbGF0ZXIgdmVyc2lvbi5cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGRpc3RyaWJ1dGVkIGluIHRoZSBob3BlIHRoYXQgaXQgd2lsbCBiZSB1c2VmdWwsIGJ1dCBXSVRIT1VUIEFOWVxuICogV0FSUkFOVFk7IHdpdGhvdXQgZXZlbiB0aGUgaW1wbGllZCB3YXJyYW50eSBvZiBNRVJDSEFOVEFCSUxJVFkgb3IgRklUTkVTUyBGT1IgQVxuICogUEFSVElDVUxBUiBQVVJQT1NFLiBTZWUgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGZvciBtb3JlIGRldGFpbHMuXG4gKiBcbiAqIFlvdSBzaG91bGQgaGF2ZSByZWNlaXZlZCBhIGNvcHkgb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFsb25nIHdpdGggdGhpc1xuICogcHJvZ3JhbS4gSWYgbm90LCBzZWUgPGh0dHA6Ly93d3cuZ251Lm9yZy9saWNlbnNlcy8+LlxuICogXG4gKiBNb2R1bGUgd2hpY2ggbWFuYWdlcyB0aGUgZG93bmxvYWQgdmlldy5cbiAqIFxuICogQHZlcnNpb24gMy4yLjBcbiAqIEBtb2R1bGUgdmlld2VySlMuZG93bmxvYWRcbiAqIEByZXF1aXJlcyBqUXVlcnlcbiAqIFxuICovXG52YXIgdmlld2VySlMgPSAoIGZ1bmN0aW9uKCB2aWV3ZXIgKSB7XG4gICAgJ3VzZSBzdHJpY3QnO1xuICAgIFxuICAgIC8vIGRlZmF1bHQgdmFyaWFibGVzXG4gICAgdmFyIF9kZWJ1ZyA9IGZhbHNlO1xuICAgIHZhciBfY2hlY2tib3ggPSBudWxsO1xuICAgIHZhciBfZG93bmxvYWRCdG4gPSBudWxsO1xuICAgIFxuICAgIHZpZXdlci5kb3dubG9hZCA9IHtcbiAgICAgICAgLyoqXG4gICAgICAgICAqIE1ldGhvZCB0byBpbml0aWFsaXplIHRoZSBkb3dubG9hZCB2aWV3LlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCBpbml0XG4gICAgICAgICAqL1xuICAgICAgICBpbml0OiBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLmRvd25sb2FkLmluaXQnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICcjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMnICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBfY2hlY2tib3ggPSAkKCAnI2FncmVlTGljZW5zZScgKTtcbiAgICAgICAgICAgIF9kb3dubG9hZEJ0biA9ICQoICcjZG93bmxvYWRCdG4nICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIF9kb3dubG9hZEJ0bi5wcm9wKCAnZGlzYWJsZWQnLCB0cnVlICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIF9jaGVja2JveC5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgdmFyIGN1cnJTdGF0ZSA9ICQoIHRoaXMgKS5wcm9wKCAnY2hlY2tlZCcgKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICB2aWV3ZXIuZG93bmxvYWQuY2hlY2tib3hWYWxpZGF0aW9uKCBjdXJyU3RhdGUgKTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgfSxcbiAgICAgICAgLyoqXG4gICAgICAgICAqIE1ldGhvZCB3aGljaCB2YWxpZGF0ZXMgdGhlIGNoZWNrc3RhdGUgb2YgYSBjaGVja2JveCBhbmQgZW5hYmxlcyB0aGUgZG93bmxvYWRcbiAgICAgICAgICogYnV0dG9uLlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCBjaGVja2JveFZhbGlkYXRpb25cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IHN0YXRlIFRoZSBjdXJyZW50IGNoZWNrc3RhdGUgb2YgdGhlIGNoZWNrYm94LlxuICAgICAgICAgKi9cbiAgICAgICAgY2hlY2tib3hWYWxpZGF0aW9uOiBmdW5jdGlvbiggc3RhdGUgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gdmlld2VyLmRvd25sb2FkLmNoZWNrYm94VmFsaWRhdGlvbigpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIuZG93bmxvYWQuY2hlY2tib3hWYWxpZGF0aW9uOiBzdGF0ZSA9ICcsIHN0YXRlICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmICggc3RhdGUgKSB7XG4gICAgICAgICAgICAgICAgX2Rvd25sb2FkQnRuLnByb3AoICdkaXNhYmxlZCcsIGZhbHNlICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICBfZG93bmxvYWRCdG4ucHJvcCggJ2Rpc2FibGVkJywgdHJ1ZSApO1xuICAgICAgICAgICAgICAgIHJldHVybiBmYWxzZTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSxcbiAgICB9O1xuICAgIFxuICAgIHJldHVybiB2aWV3ZXI7XG4gICAgXG59ICkoIHZpZXdlckpTIHx8IHt9LCBqUXVlcnkgKTtcbiIsIi8qKlxuICogVGhpcyBmaWxlIGlzIHBhcnQgb2YgdGhlIEdvb2JpIHZpZXdlciAtIGEgY29udGVudCBwcmVzZW50YXRpb24gYW5kIG1hbmFnZW1lbnRcbiAqIGFwcGxpY2F0aW9uIGZvciBkaWdpdGl6ZWQgb2JqZWN0cy5cbiAqIFxuICogVmlzaXQgdGhlc2Ugd2Vic2l0ZXMgZm9yIG1vcmUgaW5mb3JtYXRpb24uIC0gaHR0cDovL3d3dy5pbnRyYW5kYS5jb20gLVxuICogaHR0cDovL2RpZ2l2ZXJzby5jb21cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGZyZWUgc29mdHdhcmU7IHlvdSBjYW4gcmVkaXN0cmlidXRlIGl0IGFuZC9vciBtb2RpZnkgaXQgdW5kZXIgdGhlIHRlcm1zXG4gKiBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYXMgcHVibGlzaGVkIGJ5IHRoZSBGcmVlIFNvZnR3YXJlIEZvdW5kYXRpb247IGVpdGhlclxuICogdmVyc2lvbiAyIG9mIHRoZSBMaWNlbnNlLCBvciAoYXQgeW91ciBvcHRpb24pIGFueSBsYXRlciB2ZXJzaW9uLlxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZGlzdHJpYnV0ZWQgaW4gdGhlIGhvcGUgdGhhdCBpdCB3aWxsIGJlIHVzZWZ1bCwgYnV0IFdJVEhPVVQgQU5ZXG4gKiBXQVJSQU5UWTsgd2l0aG91dCBldmVuIHRoZSBpbXBsaWVkIHdhcnJhbnR5IG9mIE1FUkNIQU5UQUJJTElUWSBvciBGSVRORVNTIEZPUiBBXG4gKiBQQVJUSUNVTEFSIFBVUlBPU0UuIFNlZSB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgZm9yIG1vcmUgZGV0YWlscy5cbiAqIFxuICogWW91IHNob3VsZCBoYXZlIHJlY2VpdmVkIGEgY29weSBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYWxvbmcgd2l0aCB0aGlzXG4gKiBwcm9ncmFtLiBJZiBub3QsIHNlZSA8aHR0cDovL3d3dy5nbnUub3JnL2xpY2Vuc2VzLz4uXG4gKiBcbiAqIE1vZHVsZSB3aGljaCBnZW5lcmF0ZXMgYSBkb3dubG9hZCBtb2RhbCB3aGljaCBkeW5hbWljIGNvbnRlbnQuXG4gKiBcbiAqIEB2ZXJzaW9uIDMuMi4wXG4gKiBAbW9kdWxlIHZpZXdlckpTLmRvd25sb2FkTW9kYWxcbiAqIEByZXF1aXJlcyBqUXVlcnlcbiAqIFxuICovXG52YXIgdmlld2VySlMgPSAoIGZ1bmN0aW9uKCB2aWV3ZXIgKSB7XG4gICAgJ3VzZSBzdHJpY3QnO1xuICAgIFxuICAgIC8vIGRlZmF1bHQgdmFyaWFibGVzXG4gICAgdmFyIF9kZWJ1ZyA9IGZhbHNlO1xuICAgIHZhciBfZGVmYXVsdHMgPSB7XG4gICAgICAgIGRhdGFUeXBlOiBudWxsLFxuICAgICAgICBkYXRhVGl0bGU6IG51bGwsXG4gICAgICAgIGRhdGFJZDogbnVsbCxcbiAgICAgICAgZGF0YVBpOiBudWxsLFxuICAgICAgICBkb3dubG9hZEJ0bjogbnVsbCxcbiAgICAgICAgcmVDYXB0Y2hhU2l0ZUtleTogJycsXG4gICAgICAgIHVzZVJlQ2FwdGNoYTogdHJ1ZSxcbiAgICAgICAgcGF0aDogJycsXG4gICAgICAgIGlpaWZQYXRoOiAnJyxcbiAgICAgICAgYXBpVXJsOiAnJyxcbiAgICAgICAgdXNlckVtYWlsOiBudWxsLFxuICAgICAgICB3b3JrSW5mbzoge30sXG4gICAgICAgIG1vZGFsOiB7XG4gICAgICAgICAgICBpZDogJycsXG4gICAgICAgICAgICBsYWJlbDogJycsXG4gICAgICAgICAgICBzdHJpbmc6IHtcbiAgICAgICAgICAgICAgICB0aXRsZTogJycsXG4gICAgICAgICAgICAgICAgYm9keTogJycsXG4gICAgICAgICAgICAgICAgY2xvc2VCdG46ICcnLFxuICAgICAgICAgICAgICAgIHNhdmVCdG46ICcnLFxuICAgICAgICAgICAgfVxuICAgICAgICB9LFxuICAgICAgICBtZXNzYWdlczoge1xuICAgICAgICAgICAgZG93bmxvYWRJbmZvOiB7XG4gICAgICAgICAgICAgICAgdGV4dDogJ0luZm9ybWF0aW9uZW4genVtIGFuZ2Vmb3JkZXJ0ZW4gRG93bmxvYWQnLFxuICAgICAgICAgICAgICAgIHRpdGxlOiAnV2VyaycsXG4gICAgICAgICAgICAgICAgcGFydDogJ1RlaWwnLFxuICAgICAgICAgICAgICAgIGZpbGVTaXplOiAnR3LDtsOfZSdcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICByZUNhcHRjaGFUZXh0OiAnVW0gZGllIEdlbmVyaWVydW5nIHZvbiBEb2t1bWVudGVuIGR1cmNoIFN1Y2htYXNjaGluZW4genUgdmVyaGluZGVybiBiZXN0w6R0aWdlbiBTaWUgYml0dGUgZGFzIHJlQ0FQVENIQS4nLFxuICAgICAgICAgICAgcmNJbnZhbGlkOiAnRGllIMOcYmVycHLDvGZ1bmcgd2FyIG5pY2h0IGVyZm9sZ3JlaWNoLiBCaXR0ZSBiZXN0w6R0aWdlbiBTaWUgZGllIHJlQ0FQVENIQSBBbmZyYWdlLicsXG4gICAgICAgICAgICByY1ZhbGlkOiAnVmllbGVuIERhbmsuIFNpZSBrw7ZubmVuIG51biBpaHJlIGF1c2dld8OkaGx0ZSBEYXRlaSBnZW5lcmllcmVuIGxhc3Nlbi4nLFxuICAgICAgICAgICAgZU1haWxUZXh0OiAnVW0gcGVyIEUtTWFpbCBpbmZvcm1pZXJ0IHp1IHdlcmRlbiBzb2JhbGQgZGVyIERvd25sb2FkIHp1ciBWZXJmw7xndW5nIHN0ZWh0LCBrw7ZubmVuIFNpZSBoaWVyIG9wdGlvbmFsIElocmUgRS1NYWlsIEFkcmVzc2UgaGludGVybGFzc2VuJyxcbiAgICAgICAgICAgIGVNYWlsVGV4dExvZ2dlZEluOiAnU2llIHdlcmRlbiDDvGJlciBJaHJlIHJlZ2lzdHJpZXJ0ZSBFLU1haWwgQWRyZXNzZSB2b24gdW5zIMO8YmVyIGRlbiBGb3J0c2Nocml0dCBkZXMgRG93bmxvYWRzIGluZm9ybWllcnQuJyxcbiAgICAgICAgICAgIGVNYWlsOiAnJ1xuICAgICAgICB9XG4gICAgfTtcbiAgICB2YXIgX2xvYWRpbmdPdmVybGF5ID0gbnVsbDtcbiAgICBcbiAgICB2aWV3ZXIuZG93bmxvYWRNb2RhbCA9IHtcbiAgICAgICAgLyoqXG4gICAgICAgICAqIE1ldGhvZCB0byBpbml0aWFsaXplIHRoZSBkb3dubG9hZCBtb2RhbCBtZWNoYW5pYy5cbiAgICAgICAgICogXG4gICAgICAgICAqIEBtZXRob2QgaW5pdFxuICAgICAgICAgKiBAcGFyYW0ge09iamVjdH0gY29uZmlnIEFuIGNvbmZpZyBvYmplY3Qgd2hpY2ggb3ZlcndyaXRlcyB0aGUgZGVmYXVsdHMuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcuZGF0YVR5cGUgVGhlIGRhdGEgdHlwZSBvZiB0aGUgY3VycmVudCBmaWxlIHRvIGRvd25sb2FkLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLmRhdGFUaXRsZSBUaGUgdGl0bGUgb2YgdGhlIGN1cnJlbnQgZmlsZSB0byBkb3dubG9hZC5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5kYXRhSWQgVGhlIExPR19JRCBvZiB0aGUgY3VycmVudCBmaWxlIHRvIGRvd25sb2FkLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLmRhdGFQaSBUaGUgUEkgb2YgdGhlIGN1cnJlbnQgZmlsZSB0byBkb3dubG9hZC5cbiAgICAgICAgICogQHBhcmFtIHtPYmplY3R9IGNvbmZpZy5kb3dubG9hZEJ0biBBIGNvbGxlY3Rpb24gb2YgYWxsIGJ1dHRvbnMgd2l0aCB0aGUgY2xhc3NcbiAgICAgICAgICogYXR0cmlidXRlICdkb3dubG9hZC1tb2RhbCcuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcucmVDYXB0Y2hhU2l0ZUtleSBUaGUgc2l0ZSBrZXkgZm9yIHRoZSBnb29nbGUgcmVDQVBUQ0hBLFxuICAgICAgICAgKiBmZXRjaGVkIGZyb20gdGhlIHZpZXdlciBjb25maWcuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcucGF0aCBUaGUgY3VycmVudCBhcHBsaWNhdGlvbiBwYXRoLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLmFwaVVybCBUaGUgVVJMIHRvIHRyaWdnZXIgdGhlIElUTSBkb3dubG9hZCB0YXNrLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLnVzZXJFbWFpbCBUaGUgY3VycmVudCB1c2VyIGVtYWlsIGlmIHRoZSB1c2VyIGlzIGxvZ2dlZFxuICAgICAgICAgKiBpbi4gT3RoZXJ3aXNlIHRoZSBvbmUgd2hpY2ggdGhlIHVzZXIgZW50ZXJzIG9yIGxlYXZlcyBibGFuay5cbiAgICAgICAgICogQHBhcmFtIHtPYmplY3R9IGNvbmZpZy5tb2RhbCBBIGNvbmZpZ3VyYXRpb24gb2JqZWN0IGZvciB0aGUgZG93bmxvYWQgbW9kYWwuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcubW9kYWwuaWQgVGhlIElEIG9mIHRoZSBtb2RhbC5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5tb2RhbC5sYWJlbCBUaGUgbGFiZWwgb2YgdGhlIG1vZGFsLlxuICAgICAgICAgKiBAcGFyYW0ge09iamVjdH0gY29uZmlnLm1vZGFsLnN0cmluZyBBbiBvYmplY3Qgb2Ygc3RyaW5ncyBmb3IgdGhlIG1vZGFsIGNvbnRlbnQuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcubW9kYWwuc3RyaW5nLnRpdGxlIFRoZSB0aXRsZSBvZiB0aGUgbW9kYWwuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcubW9kYWwuc3RyaW5nLmJvZHkgVGhlIGNvbnRlbnQgb2YgdGhlIG1vZGFsIGFzIEhUTUwuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcubW9kYWwuc3RyaW5nLmNsb3NlQnRuIEJ1dHRvbnRleHRcbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5tb2RhbC5zdHJpbmcuc2F2ZUJ0biBCdXR0b250ZXh0XG4gICAgICAgICAqIEBwYXJhbSB7T2JqZWN0fSBjb25maWcubWVzc2FnZXMgQW4gb2JqZWN0IG9mIHN0cmluZ3MgZm9yIHRoZSB1c2VkIHRleHRcbiAgICAgICAgICogc25pcHBldHMuXG4gICAgICAgICAqIEBleGFtcGxlXG4gICAgICAgICAqIFxuICAgICAgICAgKiA8cHJlPlxuICAgICAgICAgKiB2YXIgZG93bmxvYWRNb2RhbENvbmZpZyA9IHtcbiAgICAgICAgICogICAgIGRvd25sb2FkQnRuOiAkKCAnLmRvd25sb2FkLW1vZGFsJyApLFxuICAgICAgICAgKiAgICAgcGF0aDogJyN7bmF2aWdhdGlvbkhlbHBlci5hcHBsaWNhdGlvblVybH0nLFxuICAgICAgICAgKiAgICAgdXNlckVtYWlsOiAkKCAnI3VzZXJFbWFpbCcgKS52YWwoKSxcbiAgICAgICAgICogICAgIG1lc3NhZ2VzOiB7XG4gICAgICAgICAqICAgICAgICAgcmVDYXB0Y2hhVGV4dDogJyN7bXNnLmRvd25sb2FkUmVDYXB0Y2hhVGV4dH0nLFxuICAgICAgICAgKiAgICAgICAgIHJjSW52YWxpZDogJyN7bXNnLmRvd25sb2FkUmNJbnZhbGlkfScsXG4gICAgICAgICAqICAgICAgICAgcmNWYWxpZDogJyN7bXNnLmRvd25sb2FkUmNWYWxpZH0nLFxuICAgICAgICAgKiAgICAgICAgIGVNYWlsVGV4dDogJyN7bXNnLmRvd25sb2FkRU1haWxUZXh0fScsXG4gICAgICAgICAqICAgICAgICAgZU1haWxUZXh0TG9nZ2VkSW46ICcje21zZy5kb3dubG9hZEVNYWlsVGV4dExvZ2dlZElufScsXG4gICAgICAgICAqICAgICAgICAgZU1haWw6ICcje21zZy5kb3dubG9hZEVtYWlsfScsXG4gICAgICAgICAqICAgICAgICAgY2xvc2VCdG46ICcje21zZy5kb3dubG9hZENsb3NlTW9kYWx9JyxcbiAgICAgICAgICogICAgICAgICBzYXZlQnRuOiAnI3ttc2cuZG93bmxvYWRHZW5lcmF0ZUZpbGV9JyxcbiAgICAgICAgICogICAgIH1cbiAgICAgICAgICogfTtcbiAgICAgICAgICogXG4gICAgICAgICAqIHZpZXdlckpTLmRvd25sb2FkTW9kYWwuaW5pdCggZG93bmxvYWRNb2RhbENvbmZpZyApO1xuICAgICAgICAgKiA8L3ByZT5cbiAgICAgICAgICovXG4gICAgICAgIGluaXQ6IGZ1bmN0aW9uKCBjb25maWcgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci5kb3dubG9hZE1vZGFsLmluaXQnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICcjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIuZG93bmxvYWRNb2RhbC5pbml0OiBjb25maWcgPSAnLCBjb25maWcgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgJC5leHRlbmQoIHRydWUsIF9kZWZhdWx0cywgY29uZmlnICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIGJ1aWxkIGxvYWRpbmcgb3ZlcmxheVxuICAgICAgICAgICAgX2xvYWRpbmdPdmVybGF5ID0gJCggJzxkaXYgLz4nICk7XG4gICAgICAgICAgICBfbG9hZGluZ092ZXJsYXkuYWRkQ2xhc3MoICdkbC1tb2RhbF9fb3ZlcmxheScgKTtcbiAgICAgICAgICAgICQoICdib2R5JyApLmFwcGVuZCggX2xvYWRpbmdPdmVybGF5ICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIF9kZWZhdWx0cy5kb3dubG9hZEJ0bi5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgLy8gc2hvdyBsb2FkaW5nIG92ZXJsYXlcbiAgICAgICAgICAgICAgICAkKCAnLmRsLW1vZGFsX19vdmVybGF5JyApLmZhZGVJbiggJ2Zhc3QnICk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgX2RlZmF1bHRzLmRhdGFUeXBlID0gJCggdGhpcyApLmF0dHIoICdkYXRhLXR5cGUnICk7XG4gICAgICAgICAgICAgICAgX2RlZmF1bHRzLmRhdGFUaXRsZSA9ICQoIHRoaXMgKS5hdHRyKCAnZGF0YS10aXRsZScgKTtcbiAgICAgICAgICAgICAgICBpZiAoICQoIHRoaXMgKS5hdHRyKCAnZGF0YS1pZCcgKSAhPT0gJycgKSB7XG4gICAgICAgICAgICAgICAgICAgIF9kZWZhdWx0cy5kYXRhSWQgPSAkKCB0aGlzICkuYXR0ciggJ2RhdGEtaWQnICk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICBfZGVmYXVsdHMuZGF0YUlkID0gJy0nO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBfZGVmYXVsdHMuZGF0YVBpID0gJCggdGhpcyApLmF0dHIoICdkYXRhLXBpJyApO1xuICAgICAgICAgICAgICAgIF9nZXRXb3JrSW5mbyggX2RlZmF1bHRzLmRhdGFQaSwgX2RlZmF1bHRzLmRhdGFJZCwgX2RlZmF1bHRzLmRhdGFUeXBlICkuZG9uZSggZnVuY3Rpb24oIGluZm8gKSB7XG4gICAgICAgICAgICAgICAgICAgIF9kZWZhdWx0cy53b3JrSW5mbyA9IGluZm87XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICBfZGVmYXVsdHMubW9kYWwgPSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBpZDogX2RlZmF1bHRzLmRhdGFQaSArICctTW9kYWwnLFxuICAgICAgICAgICAgICAgICAgICAgICAgbGFiZWw6IF9kZWZhdWx0cy5kYXRhUGkgKyAnLUxhYmVsJyxcbiAgICAgICAgICAgICAgICAgICAgICAgIHN0cmluZzoge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHRpdGxlOiBfZGVmYXVsdHMuZGF0YVRpdGxlLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGJvZHk6IHZpZXdlci5kb3dubG9hZE1vZGFsLnJlbmRlck1vZGFsQm9keSggX2RlZmF1bHRzLmRhdGFUeXBlLCBfZGVmYXVsdHMud29ya0luZm8gKSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBjbG9zZUJ0bjogX2RlZmF1bHRzLm1lc3NhZ2VzLmNsb3NlQnRuLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHNhdmVCdG46IF9kZWZhdWx0cy5tZXNzYWdlcy5zYXZlQnRuLFxuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgLy8gaGlkZSBsb2FkaW5nIG92ZXJsYXlcbiAgICAgICAgICAgICAgICAgICAgJCggJy5kbC1tb2RhbF9fb3ZlcmxheScgKS5mYWRlT3V0KCAnZmFzdCcgKTtcbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgIC8vIGluaXQgbW9kYWxcbiAgICAgICAgICAgICAgICAgICAgdmlld2VyLmRvd25sb2FkTW9kYWwuaW5pdE1vZGFsKCBfZGVmYXVsdHMgKTtcbiAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICB9ICk7XG4gICAgICAgIH0sXG4gICAgICAgIC8qKlxuICAgICAgICAgKiBNZXRob2Qgd2hpY2ggaW5pdGlhbGl6ZXMgdGhlIGRvd25sb2FkIG1vZGFsIGFuZCBpdHMgY29udGVudC5cbiAgICAgICAgICogXG4gICAgICAgICAqIEBtZXRob2QgaW5pdE1vZGFsXG4gICAgICAgICAqIEBwYXJhbSB7T2JqZWN0fSBwYXJhbXMgQW4gY29uZmlnIG9iamVjdCB3aGljaCBvdmVyd3JpdGVzIHRoZSBkZWZhdWx0cy5cbiAgICAgICAgICovXG4gICAgICAgIGluaXRNb2RhbDogZnVuY3Rpb24oIHBhcmFtcyApIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSB2aWV3ZXIuZG93bmxvYWRNb2RhbC5pbml0TW9kYWwoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLmRvd25sb2FkTW9kYWwuaW5pdE1vZGFsOiBwYXJhbXMgPSAnLCBwYXJhbXMgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgICQoICdib2R5JyApLmFwcGVuZCggdmlld2VyLmhlbHBlci5yZW5kZXJNb2RhbCggcGFyYW1zLm1vZGFsICkgKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gZGlzYWJsZSBzdWJtaXQgYnV0dG9uXG4gICAgICAgICAgICAkKCAnI3N1Ym1pdE1vZGFsJyApLmF0dHIoICdkaXNhYmxlZCcsICdkaXNhYmxlZCcgKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gc2hvdyBtb2RhbFxuICAgICAgICAgICAgJCggJyMnICsgcGFyYW1zLm1vZGFsLmlkICkubW9kYWwoICdzaG93JyApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICAvLyByZW5kZXIgcmVDQVBUQ0hBIHRvIG1vZGFsXG4gICAgICAgICAgICAkKCAnIycgKyBwYXJhbXMubW9kYWwuaWQgKS5vbiggJ3Nob3duLmJzLm1vZGFsJywgZnVuY3Rpb24oIGUgKSB7XG4gICAgICAgICAgICAgICAgaWYgKCBfZGVmYXVsdHMudXNlUmVDYXB0Y2hhICkge1xuICAgICAgICAgICAgICAgICAgICB2YXIgcmNXaWRnZXQgPSBncmVjYXB0Y2hhLnJlbmRlciggJ3JlQ2FwdGNoYVdyYXBwZXInLCB7XG4gICAgICAgICAgICAgICAgICAgICAgICBzaXRla2V5OiBfZGVmYXVsdHMucmVDYXB0Y2hhU2l0ZUtleSxcbiAgICAgICAgICAgICAgICAgICAgICAgIGNhbGxiYWNrOiBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YXIgcmNXaWRnZXRSZXNwb25zZSA9IHZpZXdlci5kb3dubG9hZE1vZGFsLnZhbGlkYXRlUmVDYXB0Y2hhKCBncmVjYXB0Y2hhLmdldFJlc3BvbnNlKCByY1dpZGdldCApICk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgaWYgKCByY1dpZGdldFJlc3BvbnNlICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCAnI21vZGFsQWxlcnRzJyApLmFwcGVuZCggdmlld2VyLmhlbHBlci5yZW5kZXJBbGVydCggJ2FsZXJ0LXN1Y2Nlc3MnLCBfZGVmYXVsdHMubWVzc2FnZXMucmNWYWxpZCwgdHJ1ZSApICk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyBlbmFibGUgc3VibWl0IGJ1dHRvblxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCAnI3N1Ym1pdE1vZGFsJyApLnJlbW92ZUF0dHIoICdkaXNhYmxlZCcgKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBfZGVmYXVsdHMudXNlckVtYWlsID0gJCggJyNyZWNhbGxFTWFpbCcgKS52YWwoKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgX2RlZmF1bHRzLmFwaVVybCA9IHZpZXdlci5kb3dubG9hZE1vZGFsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC5idWlsZEFQSUNhbGwoIF9kZWZhdWx0cy5wYXRoLCBfZGVmYXVsdHMuZGF0YVR5cGUsIF9kZWZhdWx0cy5kYXRhUGksIF9kZWZhdWx0cy5kYXRhSWQsIF9kZWZhdWx0cy51c2VyRW1haWwgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgd2luZG93LmxvY2F0aW9uLmhyZWYgPSBfZGVmYXVsdHMuYXBpVXJsO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCAnI21vZGFsQWxlcnRzJyApLmFwcGVuZCggdmlld2VyLmhlbHBlci5yZW5kZXJBbGVydCggJ2FsZXJ0LWRhbmdlcicsIF9kZWZhdWx0cy5tZXNzYWdlcy5yY0ludmFsaWQsIHRydWUgKSApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgLy8gaGlkZSBwYXJhZ3JhcGhcbiAgICAgICAgICAgICAgICAgICAgJCggdGhpcyApLmZpbmQoICcubW9kYWwtYm9keSBoNCcgKS5uZXh0KCAncCcgKS5oaWRlKCk7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAvLyBlbmFibGUgc3VibWl0IGJ1dHRvblxuICAgICAgICAgICAgICAgICAgICAkKCAnI3N1Ym1pdE1vZGFsJyApLnJlbW92ZUF0dHIoICdkaXNhYmxlZCcgKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBfZGVmYXVsdHMudXNlckVtYWlsID0gJCggJyNyZWNhbGxFTWFpbCcgKS52YWwoKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgX2RlZmF1bHRzLmFwaVVybCA9IHZpZXdlci5kb3dubG9hZE1vZGFsLmJ1aWxkQVBJQ2FsbCggX2RlZmF1bHRzLnBhdGgsIF9kZWZhdWx0cy5kYXRhVHlwZSwgX2RlZmF1bHRzLmRhdGFQaSwgX2RlZmF1bHRzLmRhdGFJZCwgX2RlZmF1bHRzLnVzZXJFbWFpbCApO1xuICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICB3aW5kb3cubG9jYXRpb24uaHJlZiA9IF9kZWZhdWx0cy5hcGlVcmw7XG4gICAgICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIHJlbW92ZSBtb2RhbCBmcm9tIERPTSBhZnRlciBjbG9zaW5nXG4gICAgICAgICAgICAkKCAnIycgKyBwYXJhbXMubW9kYWwuaWQgKS5vbiggJ2hpZGRlbi5icy5tb2RhbCcsIGZ1bmN0aW9uKCBlICkge1xuICAgICAgICAgICAgICAgICQoIHRoaXMgKS5yZW1vdmUoKTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgfSxcbiAgICAgICAgLyoqXG4gICAgICAgICAqIE1ldGhvZCB3aGljaCByZXR1cm5zIGEgSFRNTC1TdHJpbmcgdG8gcmVuZGVyIHRoZSBkb3dubG9hZCBtb2RhbCBib2R5LlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCByZW5kZXJNb2RhbEJvZHlcbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IHR5cGUgVGhlIGN1cnJlbnQgZmlsZSB0eXBlIHRvIGRvd25sb2FkLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gdGl0bGUgVGhlIHRpdGxlIG9mIHRoZSBjdXJyZW50IGRvd25sb2FkIGZpbGUuXG4gICAgICAgICAqIEByZXR1cm5zIHtTdHJpbmd9IFRoZSBIVE1MLVN0cmluZyB0byByZW5kZXIgdGhlIGRvd25sb2FkIG1vZGFsIGJvZHkuXG4gICAgICAgICAqL1xuICAgICAgICByZW5kZXJNb2RhbEJvZHk6IGZ1bmN0aW9uKCB0eXBlLCBpbmZvcyApIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSB2aWV3ZXIuZG93bmxvYWRNb2RhbC5yZW5kZXJNb2RhbEJvZHkoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLmRvd25sb2FkTW9kYWwucmVuZGVyTW9kYWxCb2R5OiB0eXBlID0gJywgdHlwZSApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLmRvd25sb2FkTW9kYWwucmVuZGVyTW9kYWxCb2R5OiBpbmZvcyA9ICcsIGluZm9zICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICB2YXIgcmNSZXNwb25zZSA9IG51bGw7XG4gICAgICAgICAgICB2YXIgbW9kYWxCb2R5ID0gJyc7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIG1vZGFsQm9keSArPSAnJztcbiAgICAgICAgICAgIC8vIGFsZXJ0c1xuICAgICAgICAgICAgbW9kYWxCb2R5ICs9ICc8ZGl2IGlkPVwibW9kYWxBbGVydHNcIj48L2Rpdj4nO1xuICAgICAgICAgICAgLy8gVGl0bGVcbiAgICAgICAgICAgIGlmICggdHlwZSA9PT0gJ3BkZicgKSB7XG4gICAgICAgICAgICAgICAgbW9kYWxCb2R5ICs9ICc8aDQ+JztcbiAgICAgICAgICAgICAgICBtb2RhbEJvZHkgKz0gJzxpIGNsYXNzPVwiZmEgZmEtZmlsZS1wZGYtb1wiIGFyaWEtaGlkZGVuPVwidHJ1ZVwiPjwvaT4gUERGLURvd25sb2FkOiAnO1xuICAgICAgICAgICAgICAgIG1vZGFsQm9keSArPSAnPC9oND4nO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgbW9kYWxCb2R5ICs9ICc8aDQ+JztcbiAgICAgICAgICAgICAgICBtb2RhbEJvZHkgKz0gJzxpIGNsYXNzPVwiZmEgZmEtZmlsZS10ZXh0LW9cIiBhcmlhLWhpZGRlbj1cInRydWVcIj48L2k+IGVQdWItRG93bmxvYWQ6ICc7XG4gICAgICAgICAgICAgICAgbW9kYWxCb2R5ICs9ICc8L2g0Pic7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICAvLyBJbmZvXG4gICAgICAgICAgICBtb2RhbEJvZHkgKz0gJzxwPicgKyBfZGVmYXVsdHMubWVzc2FnZXMuZG93bmxvYWRJbmZvLnRleHQgKyAnOjwvcD4nO1xuICAgICAgICAgICAgbW9kYWxCb2R5ICs9ICc8ZGwgY2xhc3M9XCJkbC1ob3Jpem9udGFsXCI+JztcbiAgICAgICAgICAgIG1vZGFsQm9keSArPSAnPGR0PicgKyBfZGVmYXVsdHMubWVzc2FnZXMuZG93bmxvYWRJbmZvLnRpdGxlICsgJzo8L2R0Pic7XG4gICAgICAgICAgICBtb2RhbEJvZHkgKz0gJzxkZD4nICsgaW5mb3MudGl0bGUgKyAnPC9kZD4nO1xuICAgICAgICAgICAgaWYgKCBpbmZvcy5kaXYgIT09IG51bGwgKSB7XG4gICAgICAgICAgICAgICAgbW9kYWxCb2R5ICs9ICc8ZHQ+JyArIF9kZWZhdWx0cy5tZXNzYWdlcy5kb3dubG9hZEluZm8ucGFydCArICc6PC9kdD4nO1xuICAgICAgICAgICAgICAgIG1vZGFsQm9keSArPSAnPGRkPicgKyBpbmZvcy5kaXYgKyAnPC9kZD4nO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaWYgKCBpbmZvcy5zaXplICkge1xuICAgICAgICAgICAgICAgIG1vZGFsQm9keSArPSAnPGR0PicgKyBfZGVmYXVsdHMubWVzc2FnZXMuZG93bmxvYWRJbmZvLmZpbGVTaXplICsgJzo8L2R0Pic7XG4gICAgICAgICAgICAgICAgbW9kYWxCb2R5ICs9ICc8ZGQ+ficgKyBpbmZvcy5zaXplICsgJzwvZGQ+JztcbiAgICAgICAgICAgICAgICBtb2RhbEJvZHkgKz0gJzwvZGw+JztcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIC8vIHJlQ0FQVENIQVxuICAgICAgICAgICAgaWYgKCBfZGVmYXVsdHMudXNlUmVDYXB0Y2hhICkge1xuICAgICAgICAgICAgICAgIG1vZGFsQm9keSArPSAnPGhyIC8+JztcbiAgICAgICAgICAgICAgICBtb2RhbEJvZHkgKz0gJzxwPjxzdHJvbmc+cmVDQVBUQ0hBPC9zdHJvbmc+PC9wPic7XG4gICAgICAgICAgICAgICAgbW9kYWxCb2R5ICs9ICc8cD4nICsgX2RlZmF1bHRzLm1lc3NhZ2VzLnJlQ2FwdGNoYVRleHQgKyAnOjwvcD4nO1xuICAgICAgICAgICAgICAgIG1vZGFsQm9keSArPSAnPGRpdiBpZD1cInJlQ2FwdGNoYVdyYXBwZXJcIj48L2Rpdj4nO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgLy8gRS1NYWlsXG4gICAgICAgICAgICBtb2RhbEJvZHkgKz0gJzxociAvPic7XG4gICAgICAgICAgICBtb2RhbEJvZHkgKz0gJzxmb3JtIGNsYXNzPVwiZW1haWwtZm9ybVwiPic7XG4gICAgICAgICAgICBtb2RhbEJvZHkgKz0gJzxkaXYgY2xhc3M9XCJmb3JtLWdyb3VwXCI+JztcbiAgICAgICAgICAgIG1vZGFsQm9keSArPSAnPGxhYmVsIGZvcj1cInJlY2FsbEVNYWlsXCI+JyArIF9kZWZhdWx0cy5tZXNzYWdlcy5lTWFpbCArICc8L2xhYmVsPic7XG4gICAgICAgICAgICBpZiAoIF9kZWZhdWx0cy51c2VyRW1haWwgIT0gdW5kZWZpbmVkICkge1xuICAgICAgICAgICAgICAgIG1vZGFsQm9keSArPSAnPHAgY2xhc3M9XCJoZWxwLWJsb2NrXCI+JyArIF9kZWZhdWx0cy5tZXNzYWdlcy5lTWFpbFRleHRMb2dnZWRJbiArICc8L3A+JztcbiAgICAgICAgICAgICAgICBtb2RhbEJvZHkgKz0gJzxpbnB1dCB0eXBlPVwiZW1haWxcIiBjbGFzcz1cImZvcm0tY29udHJvbFwiIGlkPVwicmVjYWxsRU1haWxcIiB2YWx1ZT1cIicgKyBfZGVmYXVsdHMudXNlckVtYWlsICsgJ1wiIGRpc2FibGVkPVwiZGlzYWJsZWRcIiAvPic7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICBtb2RhbEJvZHkgKz0gJzxwIGNsYXNzPVwiaGVscC1ibG9ja1wiPicgKyBfZGVmYXVsdHMubWVzc2FnZXMuZU1haWxUZXh0ICsgJzo8L3A+JztcbiAgICAgICAgICAgICAgICBtb2RhbEJvZHkgKz0gJzxpbnB1dCB0eXBlPVwiZW1haWxcIiBjbGFzcz1cImZvcm0tY29udHJvbFwiIGlkPVwicmVjYWxsRU1haWxcIiAvPic7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBtb2RhbEJvZHkgKz0gJzwvZGl2Pic7XG4gICAgICAgICAgICBtb2RhbEJvZHkgKz0gJzwvZm9ybT4nO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICByZXR1cm4gbW9kYWxCb2R5O1xuICAgICAgICB9LFxuICAgICAgICAvKipcbiAgICAgICAgICogTWV0aG9kIHdoaWNoIGNoZWNrcyB0aGUgcmVDQVBUQ0hBIHJlc3BvbnNlLlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCB2YWxpZGF0ZVJlQ2FwdGNoYVxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gcmVzcG9uc2UgVGhlIHJlQ0FQVENIQSByZXNwb25zZS5cbiAgICAgICAgICogQHJldHVybnMge0Jvb2xlYW59IFJldHVybnMgdHJ1ZSBpZiB0aGUgcmVDQVBUQ0hBIHNlbnQgYSByZXNwb25zZS5cbiAgICAgICAgICovXG4gICAgICAgIHZhbGlkYXRlUmVDYXB0Y2hhOiBmdW5jdGlvbiggcmVzcG9uc2UgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gdmlld2VyLmRvd25sb2FkTW9kYWwudmFsaWRhdGVSZUNhcHRjaGEoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLmRvd25sb2FkTW9kYWwudmFsaWRhdGVSZUNhcHRjaGE6IHJlc3BvbnNlID0gJywgcmVzcG9uc2UgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmICggcmVzcG9uc2UgPT0gMCApIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gZmFsc2U7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gdHJ1ZTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSxcbiAgICAgICAgLyoqXG4gICAgICAgICAqIE1ldGhvZCB3aGljaCByZXR1cm5zIGFuIFVSTCB0byB0cmlnZ2VyIHRoZSBJVE0gZG93bmxvYWQgdGFzay5cbiAgICAgICAgICogXG4gICAgICAgICAqIEBtZXRob2QgYnVpbGRBUElDYWxsXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBwYXRoIFRoZSBjdXJyZW50IGFwcGxpY2F0aW9uIHBhdGguXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSB0eXBlIFRoZSBjdXJyZW50IGZpbGUgdHlwZSB0byBkb3dubG9hZC5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IHBpIFRoZSBQSSBvZiB0aGUgY3VycmVudCB3b3JrLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gbG9naWQgVGhlIExPR19JRCBvZiB0aGUgY3VycmVudCB3b3JrLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gZW1haWwgVGhlIGN1cnJlbnQgdXNlciBlbWFpbC5cbiAgICAgICAgICogQHJldHVybnMge1N0cmluZ30gVGhlIFVSTCB0byB0cmlnZ2VyIHRoZSBJVE0gZG93bmxvYWQgdGFzay5cbiAgICAgICAgICovXG4gICAgICAgIGJ1aWxkQVBJQ2FsbDogZnVuY3Rpb24oIHBhdGgsIHR5cGUsIHBpLCBsb2dpZCwgZW1haWwgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gdmlld2VyLmRvd25sb2FkTW9kYWwuYnVpbGRBUElDYWxsKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci5kb3dubG9hZE1vZGFsLmJ1aWxkQVBJQ2FsbDogcGF0aCA9ICcsIHBhdGggKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci5kb3dubG9hZE1vZGFsLmJ1aWxkQVBJQ2FsbDogdHlwZSA9ICcsIHR5cGUgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci5kb3dubG9hZE1vZGFsLmJ1aWxkQVBJQ2FsbDogcGkgPSAnLCBwaSApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLmRvd25sb2FkTW9kYWwuYnVpbGRBUElDYWxsOiBsb2dpZCA9ICcsIGxvZ2lkICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIuZG93bmxvYWRNb2RhbC5idWlsZEFQSUNhbGw6IGVtYWlsID0gJywgZW1haWwgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHZhciB1cmwgPSAnJztcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgdXJsICs9IHBhdGggKyAncmVzdC9kb3dubG9hZCc7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmICggdHlwZSA9PSAnJyApIHtcbiAgICAgICAgICAgICAgICB1cmwgKz0gJy8tJztcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIHVybCArPSAnLycgKyB0eXBlO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaWYgKCBwaSA9PSAnJyApIHtcbiAgICAgICAgICAgICAgICB1cmwgKz0gJy8tJztcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIHVybCArPSAnLycgKyBwaTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmICggbG9naWQgPT0gJycgKSB7XG4gICAgICAgICAgICAgICAgdXJsICs9ICcvLSc7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICB1cmwgKz0gJy8nICsgbG9naWQ7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAoIGVtYWlsID09ICcnIHx8IGVtYWlsID09IHVuZGVmaW5lZCApIHtcbiAgICAgICAgICAgICAgICB1cmwgKz0gJy8tLyc7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICB1cmwgKz0gJy8nICsgZW1haWwgKyAnLyc7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHJldHVybiBlbmNvZGVVUkkoIHVybCApO1xuICAgICAgICB9XG4gICAgfTtcbiAgICBcbiAgICAvKipcbiAgICAgKiBNZXRob2Qgd2hpY2ggcmV0dXJucyBhIHByb21pc2UgaWYgdGhlIHdvcmsgaW5mbyBoYXMgYmVlbiByZWFjaGVkLlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgZ2V0V29ya0luZm9cbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gcGkgVGhlIFBJIG9mIHRoZSB3b3JrLlxuICAgICAqIEBwYXJhbSB7U3RyaW5nfSBsb2dpZCBUaGUgTE9HX0lEIG9mIHRoZSB3b3JrLlxuICAgICAqIEByZXR1cm5zIHtQcm9taXNlfSBBIHByb21pc2Ugb2JqZWN0IGlmIHRoZSBpbmZvIGhhcyBiZWVuIHJlYWNoZWQuXG4gICAgICovXG4gICAgZnVuY3Rpb24gX2dldFdvcmtJbmZvKCBwaSwgbG9naWQsIHR5cGUgKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICctLS0tLS0tLS0tIF9nZXRXb3JrSW5mbygpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19nZXRXb3JrSW5mbzogcGkgPSAnLCBwaSApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfZ2V0V29ya0luZm86IGxvZ2lkID0gJywgbG9naWQgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2dldFdvcmtJbmZvOiB0eXBlID0gJywgdHlwZSApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgcmVzdENhbGwgPSAnJztcbiAgICAgICAgdmFyIHdvcmtJbmZvID0ge307XG4gICAgICAgIFxuICAgICAgICBpZiAoIGxvZ2lkICE9PSAnJyB8fCBsb2dpZCAhPT0gdW5kZWZpbmVkICkge1xuICAgICAgICAgICAgcmVzdENhbGwgPSBfZGVmYXVsdHMuaWlpZlBhdGggKyB0eXBlICsgJy9tZXRzLycgKyBwaSArICcvJyArIGxvZ2lkICsgJy8nO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ2lmJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2dldFdvcmtJbmZvOiByZXN0Q2FsbCA9ICcsIHJlc3RDYWxsICk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICByZXN0Q2FsbCA9IF9kZWZhdWx0cy5paWlmUGF0aCArIHR5cGUgKyAnL21ldHMvJyArIHBpICsgJy8tLyc7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnZWxzZScgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ19nZXRXb3JrSW5mbzogcmVzdENhbGwgPSAnLCByZXN0Q2FsbCApO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICByZXR1cm4gdmlld2VySlMuaGVscGVyLmdldFJlbW90ZURhdGEoIHJlc3RDYWxsICk7XG4gICAgfVxuICAgIFxuICAgIHJldHVybiB2aWV3ZXI7XG4gICAgXG59ICkoIHZpZXdlckpTIHx8IHt9LCBqUXVlcnkgKTtcbiIsIi8qKlxuICogVGhpcyBmaWxlIGlzIHBhcnQgb2YgdGhlIEdvb2JpIHZpZXdlciAtIGEgY29udGVudCBwcmVzZW50YXRpb24gYW5kIG1hbmFnZW1lbnRcbiAqIGFwcGxpY2F0aW9uIGZvciBkaWdpdGl6ZWQgb2JqZWN0cy5cbiAqIFxuICogVmlzaXQgdGhlc2Ugd2Vic2l0ZXMgZm9yIG1vcmUgaW5mb3JtYXRpb24uIC0gaHR0cDovL3d3dy5pbnRyYW5kYS5jb20gLVxuICogaHR0cDovL2RpZ2l2ZXJzby5jb21cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGZyZWUgc29mdHdhcmU7IHlvdSBjYW4gcmVkaXN0cmlidXRlIGl0IGFuZC9vciBtb2RpZnkgaXQgdW5kZXIgdGhlIHRlcm1zXG4gKiBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYXMgcHVibGlzaGVkIGJ5IHRoZSBGcmVlIFNvZnR3YXJlIEZvdW5kYXRpb247IGVpdGhlclxuICogdmVyc2lvbiAyIG9mIHRoZSBMaWNlbnNlLCBvciAoYXQgeW91ciBvcHRpb24pIGFueSBsYXRlciB2ZXJzaW9uLlxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZGlzdHJpYnV0ZWQgaW4gdGhlIGhvcGUgdGhhdCBpdCB3aWxsIGJlIHVzZWZ1bCwgYnV0IFdJVEhPVVQgQU5ZXG4gKiBXQVJSQU5UWTsgd2l0aG91dCBldmVuIHRoZSBpbXBsaWVkIHdhcnJhbnR5IG9mIE1FUkNIQU5UQUJJTElUWSBvciBGSVRORVNTIEZPUiBBXG4gKiBQQVJUSUNVTEFSIFBVUlBPU0UuIFNlZSB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgZm9yIG1vcmUgZGV0YWlscy5cbiAqIFxuICogWW91IHNob3VsZCBoYXZlIHJlY2VpdmVkIGEgY29weSBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYWxvbmcgd2l0aCB0aGlzXG4gKiBwcm9ncmFtLiBJZiBub3QsIHNlZSA8aHR0cDovL3d3dy5nbnUub3JnL2xpY2Vuc2VzLz4uXG4gKiBcbiAqIE1vZHVsZSB0byBlZGl0IGNvbW1lbnRzIGluIHVzZXIgZ2VuZXJhdGVkIGNvbnRlbnQuXG4gKiBcbiAqIEB2ZXJzaW9uIDMuMi4wXG4gKiBAbW9kdWxlIHZpZXdlckpTLmVkaXRDb21tZW50XG4gKiBAcmVxdWlyZXMgalF1ZXJ5XG4gKi9cbnZhciB2aWV3ZXJKUyA9ICggZnVuY3Rpb24oIHZpZXdlciApIHtcbiAgICAndXNlIHN0cmljdCc7XG4gICAgXG4gICAgdmFyIF9kZWJ1ZyA9IGZhbHNlO1xuICAgIHZhciBfZGVmYXVsdHMgPSB7XG4gICAgICAgIGNvbW1lbnRDb3VudDogMCxcbiAgICAgICAgZWRpdEJ0bkNvdW50OiAwLFxuICAgICAgICBkZWxldGVCdG5Db3VudDogMCxcbiAgICAgICAgZGVsZXRlTW9kYWxDb3VudDogMCxcbiAgICAgICAgYnRuSWQ6IG51bGwsXG4gICAgICAgIGNvbW1lbnRUZXh0OiBudWxsXG4gICAgfTtcbiAgICBcbiAgICB2aWV3ZXIuZWRpdENvbW1lbnQgPSB7XG4gICAgICAgIC8qKlxuICAgICAgICAgKiBNZXRob2Qgd2hpY2ggaW5pdGlhbGl6ZXMgYWxsIHJlcXVpcmVkIGV2ZW50cyB0byBlZGl0IGNvbW1lbnRzLlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCBpbml0XG4gICAgICAgICAqIEBleGFtcGxlXG4gICAgICAgICAqIFxuICAgICAgICAgKiA8cHJlPlxuICAgICAgICAgKiAkKCBkb2N1bWVudCApLnJlYWR5KCBmdW5jdGlvbigpIHtcbiAgICAgICAgICogICAgIHZpZXdlckpTLmVkaXRDb21tZW50LmluaXQoKTtcbiAgICAgICAgICogfSApO1xuICAgICAgICAgKiA8L3ByZT5cbiAgICAgICAgICovXG4gICAgICAgIGluaXQ6IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICcjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIuZWRpdENvbW1lbnQuaW5pdCcgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gY2xlYXIgdGV4YXJlYSBmb3IgbmV3IGNvbW1lbnRzXG4gICAgICAgICAgICBpZiAoICQoICd0ZXh0YXJlYVtpZCo9XCJuZXdDb21tZW50SW5wdXRcIl0nICkudmFsKCkgIT09ICcnICkge1xuICAgICAgICAgICAgICAgICQoICd0ZXh0YXJlYVtpZCo9XCJuZXdDb21tZW50SW5wdXRcIl0nICkuZm9jdXMoKS52YWwoICcnICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIGV2ZW50bGlzdGVuZXJzXG4gICAgICAgICAgICAkKCAnLmNvbW1lbnQtZWRpdCcgKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgLy8gaGlkZSBhZGQgbmV3IGNvbW1lbnQgZmllbGRcbiAgICAgICAgICAgICAgICAkKCAnI25ld0NvbW1lbnRGaWVsZCcgKS5mYWRlT3V0KCk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLy8gZ2V0IGJ1dHRvbiBpZFxuICAgICAgICAgICAgICAgIF9kZWZhdWx0cy5idG5JZCA9ICQoIHRoaXMgKS5hdHRyKCAnaWQnICkucmVwbGFjZSggJ2NvbW1lbnRFZGl0QnRuLScsICcnICk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLy8gc2hvdyB0ZXh0ZmllbGQgdG8gZWRpdCBjb21tZW50IGFuZCBoaWRlIGNvbW1lbnRcbiAgICAgICAgICAgICAgICAkKCAnI2FkZENvbW1lbnQnICkuaGlkZSgpO1xuICAgICAgICAgICAgICAgICQoICcjY29tbWVudEVkaXRDb21tZW50LScgKyBfZGVmYXVsdHMuYnRuSWQgKS5wcmV2KCkuaGlkZSgpO1xuICAgICAgICAgICAgICAgICQoICcjY29tbWVudEVkaXRDb21tZW50LScgKyBfZGVmYXVsdHMuYnRuSWQgKS5zaG93KCk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLy8gaGlkZSBlZGl0IGJ1dHRvblxuICAgICAgICAgICAgICAgICQoICcuY29tbWVudC1lZGl0JyApLmhpZGUoKTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgJCggJy5jb21tZW50LWFib3J0JyApLm9uKCAnY2xpY2snLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAvLyBzaG93IGFkZCBuZXcgY29tbWVudCBmaWVsZFxuICAgICAgICAgICAgICAgICQoICcjbmV3Q29tbWVudEZpZWxkJyApLmZhZGVJbigpO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIC8vIHNob3cgZWRpdCBidXR0b24gYW5kIGNvbW1lbnRcbiAgICAgICAgICAgICAgICAkKCAnLmNvbW1lbnRzLWNvbW1lbnQtdGV4dCcgKS5zaG93KCk7XG4gICAgICAgICAgICAgICAgJCggJy5jb21tZW50LWVkaXQnICkuc2hvdygpO1xuICAgICAgICAgICAgICAgICQoICcjYWRkQ29tbWVudCcgKS5zaG93KCk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLy8gaGlkZSB0ZXh0ZmllbGQgdG8gZWRpdCBjb21tZW50XG4gICAgICAgICAgICAgICAgJCggdGhpcyApLnBhcmVudCgpLmhpZGUoKTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gYWRkIGNvdW50aW5nIGlkcyB0byBlbGVtZW50c1xuICAgICAgICAgICAgJCggJy5jb21tZW50LWVkaXQnICkuZWFjaCggZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgJCggdGhpcyApLmF0dHIoICdpZCcsICdjb21tZW50RWRpdEJ0bi0nICsgX2RlZmF1bHRzLmVkaXRCdG5Db3VudCApO1xuICAgICAgICAgICAgICAgIF9kZWZhdWx0cy5lZGl0QnRuQ291bnQrKztcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgJCggJy5jb21tZW50cy1jb21tZW50LWVkaXQnICkuZWFjaCggZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgJCggdGhpcyApLmF0dHIoICdpZCcsICdjb21tZW50RWRpdENvbW1lbnQtJyArIF9kZWZhdWx0cy5jb21tZW50Q291bnQgKTtcbiAgICAgICAgICAgICAgICBfZGVmYXVsdHMuY29tbWVudENvdW50Kys7XG4gICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgICQuZWFjaCggJCggJy5jb21tZW50cy1kZWxldGUtYnRuJyApLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAkKCB0aGlzICkuYXR0ciggJ2RhdGEtdG9nZ2xlJywgJ21vZGFsJyApLmF0dHIoICdkYXRhLXRhcmdldCcsICcjZGVsZXRlQ29tbWVudE1vZGFsLScgKyBfZGVmYXVsdHMuZGVsZXRlQnRuQ291bnQgKTtcbiAgICAgICAgICAgICAgICBfZGVmYXVsdHMuZGVsZXRlQnRuQ291bnQrKztcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgJC5lYWNoKCAkKCAnLmRlbGV0ZUNvbW1lbnRNb2RhbCcgKSwgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgJCggdGhpcyApLmF0dHIoICdpZCcsICdkZWxldGVDb21tZW50TW9kYWwtJyArIF9kZWZhdWx0cy5kZWxldGVNb2RhbENvdW50ICk7XG4gICAgICAgICAgICAgICAgX2RlZmF1bHRzLmRlbGV0ZU1vZGFsQ291bnQrKztcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgfVxuICAgIH07XG4gICAgXG4gICAgcmV0dXJuIHZpZXdlcjtcbiAgICBcbn0gKSggdmlld2VySlMgfHwge30sIGpRdWVyeSApO1xuIiwiLyoqXG4gKiBUaGlzIGZpbGUgaXMgcGFydCBvZiB0aGUgR29vYmkgdmlld2VyIC0gYSBjb250ZW50IHByZXNlbnRhdGlvbiBhbmQgbWFuYWdlbWVudFxuICogYXBwbGljYXRpb24gZm9yIGRpZ2l0aXplZCBvYmplY3RzLlxuICogXG4gKiBWaXNpdCB0aGVzZSB3ZWJzaXRlcyBmb3IgbW9yZSBpbmZvcm1hdGlvbi4gLSBodHRwOi8vd3d3LmludHJhbmRhLmNvbSAtXG4gKiBodHRwOi8vZGlnaXZlcnNvLmNvbVxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZnJlZSBzb2Z0d2FyZTsgeW91IGNhbiByZWRpc3RyaWJ1dGUgaXQgYW5kL29yIG1vZGlmeSBpdCB1bmRlciB0aGUgdGVybXNcbiAqIG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhcyBwdWJsaXNoZWQgYnkgdGhlIEZyZWUgU29mdHdhcmUgRm91bmRhdGlvbjsgZWl0aGVyXG4gKiB2ZXJzaW9uIDIgb2YgdGhlIExpY2Vuc2UsIG9yIChhdCB5b3VyIG9wdGlvbikgYW55IGxhdGVyIHZlcnNpb24uXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBkaXN0cmlidXRlZCBpbiB0aGUgaG9wZSB0aGF0IGl0IHdpbGwgYmUgdXNlZnVsLCBidXQgV0lUSE9VVCBBTllcbiAqIFdBUlJBTlRZOyB3aXRob3V0IGV2ZW4gdGhlIGltcGxpZWQgd2FycmFudHkgb2YgTUVSQ0hBTlRBQklMSVRZIG9yIEZJVE5FU1MgRk9SIEFcbiAqIFBBUlRJQ1VMQVIgUFVSUE9TRS4gU2VlIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBmb3IgbW9yZSBkZXRhaWxzLlxuICogXG4gKiBZb3Ugc2hvdWxkIGhhdmUgcmVjZWl2ZWQgYSBjb3B5IG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhbG9uZyB3aXRoIHRoaXNcbiAqIHByb2dyYW0uIElmIG5vdCwgc2VlIDxodHRwOi8vd3d3LmdudS5vcmcvbGljZW5zZXMvPi5cbiAqIFxuICogTW9kdWxlIHdoaWNoIGluY2x1ZGVzIG1vc3RseSB1c2VkIGhlbHBlciBmdW5jdGlvbnMuXG4gKiBcbiAqIEB2ZXJzaW9uIDMuMi4wXG4gKiBAbW9kdWxlIHZpZXdlckpTLmhlbHBlclxuICogQHJlcXVpcmVzIGpRdWVyeVxuICovXG52YXIgdmlld2VySlMgPSAoIGZ1bmN0aW9uKCB2aWV3ZXIgKSB7XG4gICAgJ3VzZSBzdHJpY3QnO1xuICAgIFxuICAgIC8vIGRlZmF1bHQgdmFyaWFibGVzXG4gICAgdmFyIF9kZWJ1ZyA9IGZhbHNlO1xuICAgIFxuICAgIHZpZXdlci5oZWxwZXIgPSB7XG4gICAgICAgIC8qKlxuICAgICAgICAgKiBNZXRob2QgdG8gdHJ1bmNhdGUgYSBzdHJpbmcgdG8gYSBnaXZlbiBsZW5ndGguXG4gICAgICAgICAqIFxuICAgICAgICAgKiBAbWV0aG9kIHRydW5jYXRlU3RyaW5nXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBzdHIgVGhlIHN0cmluZyB0byB0cnVuY2F0ZS5cbiAgICAgICAgICogQHBhcmFtIHtOdW1iZXJ9IHNpemUgVGhlIG51bWJlciBvZiBjaGFyYWN0ZXJzIGFmdGVyIHRoZSBzdHJpbmcgc2hvdWxkIGJlXG4gICAgICAgICAqIGNyb3BlZC5cbiAgICAgICAgICogQHJldHVybnMge1N0cmluZ30gVGhlIHRydW5jYXRlZCBzdHJpbmcuXG4gICAgICAgICAqIEBleGFtcGxlXG4gICAgICAgICAqIFxuICAgICAgICAgKiA8cHJlPlxuICAgICAgICAgKiB2aWV3ZXJKUy5oZWxwZXIudHJ1bmNhdGVTdHJpbmcoICQoICcuc29tZXRoaW5nJyApLnRleHQoKSwgNzUgKTtcbiAgICAgICAgICogPC9wcmU+XG4gICAgICAgICAqL1xuICAgICAgICB0cnVuY2F0ZVN0cmluZzogZnVuY3Rpb24oIHN0ciwgc2l6ZSApIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSB2aWV3ZXIuaGVscGVyLnRydW5jYXRlU3RyaW5nKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci5oZWxwZXIudHJ1bmNhdGVTdHJpbmc6IHN0ciA9ICcsIHN0ciApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLmhlbHBlci50cnVuY2F0ZVN0cmluZzogc2l6ZSA9ICcsIHNpemUgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgdmFyIHN0clNpemUgPSBwYXJzZUludCggc3RyLmxlbmd0aCApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBpZiAoIHN0clNpemUgPiBzaXplICkge1xuICAgICAgICAgICAgICAgIHJldHVybiBzdHIuc3Vic3RyaW5nKCAwLCBzaXplICkgKyAnLi4uJztcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIHJldHVybiBzdHI7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0sXG4gICAgICAgIC8qKlxuICAgICAgICAgKiBNZXRob2Qgd2hpY2ggY2FsY3VsYXRlcyB0aGUgY3VycmVudCBwb3NpdGlvbiBvZiB0aGUgYWN0aXZlIGVsZW1lbnQgaW4gc2lkZWJhclxuICAgICAgICAgKiB0b2MgYW5kIHRoZSBpbWFnZSBjb250YWluZXIgcG9zaXRpb24gYW5kIHNhdmVzIGl0IHRvIGxhY2FsIHN0b3JhZ2UuXG4gICAgICAgICAqIFxuICAgICAgICAgKiBAbWV0aG9kIHNhdmVTaWRlYmFyVG9jUG9zaXRpb25cbiAgICAgICAgICogQGV4YW1wbGVcbiAgICAgICAgICogXG4gICAgICAgICAqIDxwcmU+XG4gICAgICAgICAqIHZpZXdlckpTLmhlbHBlci5zYXZlU2lkZWJhclRvY1Bvc2l0aW9uKCk7XG4gICAgICAgICAqIDwvcHJlPlxuICAgICAgICAgKi9cbiAgICAgICAgc2F2ZVNpZGViYXJUb2NQb3NpdGlvbjogZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gdmlld2VyLmhlbHBlci5zYXZlU2lkZWJhclRvY1Bvc2l0aW9uKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgdmFyIHNjcm9sbFNpZGViYXJUb2NQb3NpdGlvbiA9IG51bGw7XG4gICAgICAgICAgICB2YXIgc2F2ZWRJZERvYyA9IGxvY2FsU3RvcmFnZS5nZXRJdGVtKCAnY3VycklkRG9jJyApO1xuICAgICAgICAgICAgdmFyIHNpZGViYXJUb2NXcmFwcGVyID0gJy53aWRnZXQtdG9jLWVsZW0td3JhcHAnO1xuICAgICAgICAgICAgdmFyIGN1cnJFbGVtZW50ID0gbnVsbDtcbiAgICAgICAgICAgIHZhciBjdXJyVXJsID0gJyc7XG4gICAgICAgICAgICB2YXIgcGFyZW50TG9nSWQgPSAnJztcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgaWYgKCB2aWV3ZXIubG9jYWxTdG9yYWdlUG9zc2libGUgKSB7XG4gICAgICAgICAgICAgICAgaWYgKCBzYXZlZElkRG9jICE9PSAnZmFsc2UnICkge1xuICAgICAgICAgICAgICAgICAgICBjdXJyRWxlbWVudCA9ICQoICdsaVtkYXRhLWlkZG9jPVwiJyArIHNhdmVkSWREb2MgKyAnXCJdJyApO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgaWYgKCBjdXJyRWxlbWVudC5sZW5ndGggPiAwICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgJCggc2lkZWJhclRvY1dyYXBwZXIgKS5zY3JvbGxUb3AoIGN1cnJFbGVtZW50Lm9mZnNldCgpLnRvcCAtICQoIHNpZGViYXJUb2NXcmFwcGVyICkub2Zmc2V0KCkudG9wICsgJCggc2lkZWJhclRvY1dyYXBwZXIgKS5zY3JvbGxUb3AoKSApO1xuICAgICAgICAgICAgICAgICAgICAgICAgbG9jYWxTdG9yYWdlLnNldEl0ZW0oICdjdXJySWREb2MnLCAnZmFsc2UnICk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBsb2NhbFN0b3JhZ2Uuc2V0SXRlbSggJ2N1cnJJZERvYycsICdmYWxzZScgKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgJCggJy53aWRnZXQtdG9jLWVsZW0tbGluayBhJyApLm9uKCAnY2xpY2snLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHBhcmVudExvZ0lkID0gJCggdGhpcyApLnBhcmVudHMoICdsaScgKS5hdHRyKCAnZGF0YS1pZGRvYycgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGxvY2FsU3RvcmFnZS5zZXRJdGVtKCAnY3VycklkRG9jJywgcGFyZW50TG9nSWQgKTtcbiAgICAgICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgbG9jYWxTdG9yYWdlLnNldEl0ZW0oICdjdXJySWREb2MnLCAnZmFsc2UnICk7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAvLyBleHBhbmQgY2xpY2tcbiAgICAgICAgICAgICAgICAgICAgJCggJy53aWRnZXQtdG9jLWVsZW0tZXhwYW5kIGEnICkub24oICdjbGljaycsIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgc2Nyb2xsU2lkZWJhclRvY1Bvc2l0aW9uID0gJCggc2lkZWJhclRvY1dyYXBwZXIgKS5zY3JvbGxUb3AoKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgbG9jYWxTdG9yYWdlLnNldEl0ZW0oICdzaWRlYmFyVG9jU2Nyb2xsUG9zaXRpb24nLCBzY3JvbGxTaWRlYmFyVG9jUG9zaXRpb24gKTtcbiAgICAgICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgLy8gbGluayBjbGlja1xuICAgICAgICAgICAgICAgICAgICAkKCAnLndpZGdldC10b2MtZWxlbS1saW5rIGEnICkub24oICdjbGljaycsIGZ1bmN0aW9uKCBldmVudCApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGV2ZW50LnByZXZlbnREZWZhdWx0KCk7XG4gICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgIGN1cnJVcmwgPSAkKCB0aGlzICkuYXR0ciggJ2hyZWYnICk7XG4gICAgICAgICAgICAgICAgICAgICAgICBzY3JvbGxTaWRlYmFyVG9jUG9zaXRpb24gPSAkKCBzaWRlYmFyVG9jV3JhcHBlciApLnNjcm9sbFRvcCgpO1xuICAgICAgICAgICAgICAgICAgICAgICAgbG9jYWxTdG9yYWdlLnNldEl0ZW0oICdzaWRlYmFyVG9jU2Nyb2xsUG9zaXRpb24nLCBzY3JvbGxTaWRlYmFyVG9jUG9zaXRpb24gKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGxvY2F0aW9uLmhyZWYgPSBjdXJyVXJsO1xuICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAvLyBzY3JvbGwgdG8gc2F2ZWQgcG9zaXRpb25cbiAgICAgICAgICAgICAgICAgICAgJCggc2lkZWJhclRvY1dyYXBwZXIgKS5zY3JvbGxUb3AoIGxvY2FsU3RvcmFnZS5nZXRJdGVtKCAnc2lkZWJhclRvY1Njcm9sbFBvc2l0aW9uJyApICk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIGZhbHNlO1xuICAgICAgICAgICAgfVxuICAgICAgICB9LFxuICAgICAgICAvKipcbiAgICAgICAgICogUmV0dXJucyBhbiBKU09OIG9iamVjdCBmcm9tIGEgQVBJIGNhbGwuXG4gICAgICAgICAqIFxuICAgICAgICAgKiBAbWV0aG9kIGdldFJlbW90ZURhdGFcbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IHVybCBUaGUgQVBJIGNhbGwgVVJMLlxuICAgICAgICAgKiBAcmV0dXJucyB7T2JqZWN0fSBBIHByb21pc2Ugb2JqZWN0LCB3aGljaCB0ZWxscyBhYm91dCB0aGUgc3VjY2VzcyBvZiByZWNlaXZpbmdcbiAgICAgICAgICogZGF0YS5cbiAgICAgICAgICogQGV4YW1wbGVcbiAgICAgICAgICogXG4gICAgICAgICAqIDxwcmU+XG4gICAgICAgICAqIHZpZXdlckpTLmhlbHBlci5nZXRSZW1vdGVEYXRhKCBkYXRhVVJMICk7XG4gICAgICAgICAqIDwvcHJlPlxuICAgICAgICAgKi9cbiAgICAgICAgZ2V0UmVtb3RlRGF0YTogZnVuY3Rpb24oIHVybCApIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSB2aWV3ZXIuaGVscGVyLmdldFJlbW90ZURhdGEoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLmhlbHBlci5nZXRSZW1vdGVEYXRhOiB1cmwgPSAnLCB1cmwgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgdmFyIHByb21pc2UgPSBRKCAkLmFqYXgoIHtcbiAgICAgICAgICAgICAgICB1cmw6IGRlY29kZVVSSSggdXJsICksXG4gICAgICAgICAgICAgICAgdHlwZTogXCJHRVRcIixcbiAgICAgICAgICAgICAgICBkYXRhVHlwZTogXCJKU09OXCIsXG4gICAgICAgICAgICAgICAgYXN5bmM6IHRydWVcbiAgICAgICAgICAgIH0gKSApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICByZXR1cm4gcHJvbWlzZTtcbiAgICAgICAgfSxcbiAgICAgICAgLyoqXG4gICAgICAgICAqIFJldHVybnMgYSBCUyBNb2RhbCB3aXRoIGR5bmFtaWMgY29udGVudC5cbiAgICAgICAgICogXG4gICAgICAgICAqIEBtZXRob2QgcmVuZGVyTW9kYWxcbiAgICAgICAgICogQHBhcmFtIHtPYmplY3R9IGNvbmZpZyBBbiBjb25maWcgb2JqZWN0IHdoaWNoIGluY2x1ZGVzIHRoZSBjb250ZW50IG9mIHRoZVxuICAgICAgICAgKiBtb2RhbC5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5pZCBUaGUgSUQgb2YgdGhlIG1vZGFsLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLmxhYmVsIFRoZSBsYWJlbCBvZiB0aGUgbW9kYWwuXG4gICAgICAgICAqIEBwYXJhbSB7T2JqZWN0fSBjb25maWcuc3RyaW5nIEFuIG9iamVjdCBvZiBzdHJpbmdzIGZvciB0aGUgbW9kYWwgY29udGVudC5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5zdHJpbmcudGl0bGUgVGhlIHRpdGxlIG9mIHRoZSBtb2RhbC5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5zdHJpbmcuYm9keSBUaGUgY29udGVudCBvZiB0aGUgbW9kYWwgYXMgSFRNTC5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5zdHJpbmcuY2xvc2VCdG4gQnV0dG9udGV4dFxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLnN0cmluZy5zYXZlQnRuIEJ1dHRvbnRleHRcbiAgICAgICAgICogQHJldHVybnMge1N0cmluZ30gQSBIVE1MLVN0cmluZyB3aGljaCByZW5kZXJzIHRoZSBtb2RhbC5cbiAgICAgICAgICovXG4gICAgICAgIHJlbmRlck1vZGFsOiBmdW5jdGlvbiggY29uZmlnICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICctLS0tLS0tLS0tIHZpZXdlci5oZWxwZXIucmVuZGVyTW9kYWwoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLmhlbHBlci5yZW5kZXJNb2RhbDogY29uZmlnID0gJywgY29uZmlnICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICB2YXIgX2RlZmF1bHRzID0ge1xuICAgICAgICAgICAgICAgIGlkOiAnbXlNb2RhbCcsXG4gICAgICAgICAgICAgICAgbGFiZWw6ICdteU1vZGFsTGFiZWwnLFxuICAgICAgICAgICAgICAgIGNsb3NlSWQ6ICdjbG9zZU1vZGFsJyxcbiAgICAgICAgICAgICAgICBzdWJtaXRJZDogJ3N1Ym1pdE1vZGFsJyxcbiAgICAgICAgICAgICAgICBzdHJpbmc6IHtcbiAgICAgICAgICAgICAgICAgICAgdGl0bGU6ICdNb2RhbCB0aXRsZScsXG4gICAgICAgICAgICAgICAgICAgIGJvZHk6ICcnLFxuICAgICAgICAgICAgICAgICAgICBjbG9zZUJ0bjogJ0Nsb3NlJyxcbiAgICAgICAgICAgICAgICAgICAgc2F2ZUJ0bjogJ1NhdmUgY2hhbmdlcycsXG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgJC5leHRlbmQoIHRydWUsIF9kZWZhdWx0cywgY29uZmlnICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHZhciBtb2RhbCA9ICcnO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBtb2RhbCArPSAnPGRpdiBjbGFzcz1cIm1vZGFsIGZhZGVcIiBpZD1cIicgKyBfZGVmYXVsdHMuaWQgKyAnXCIgdGFiaW5kZXg9XCItMVwiIHJvbGU9XCJkaWFsb2dcIiBhcmlhLWxhYmVsbGVkYnk9XCInICsgX2RlZmF1bHRzLmxhYmVsICsgJ1wiPic7XG4gICAgICAgICAgICBtb2RhbCArPSAnPGRpdiBjbGFzcz1cIm1vZGFsLWRpYWxvZ1wiIHJvbGU9XCJkb2N1bWVudFwiPic7XG4gICAgICAgICAgICBtb2RhbCArPSAnPGRpdiBjbGFzcz1cIm1vZGFsLWNvbnRlbnRcIj4nO1xuICAgICAgICAgICAgbW9kYWwgKz0gJzxkaXYgY2xhc3M9XCJtb2RhbC1oZWFkZXJcIj4nO1xuICAgICAgICAgICAgbW9kYWwgKz0gJzxidXR0b24gdHlwZT1cImJ1dHRvblwiIGNsYXNzPVwiY2xvc2VcIiBkYXRhLWRpc21pc3M9XCJtb2RhbFwiIGFyaWEtbGFiZWw9XCInICsgX2RlZmF1bHRzLnN0cmluZy5jbG9zZUJ0biArICdcIj4nO1xuICAgICAgICAgICAgbW9kYWwgKz0gJzxzcGFuIGFyaWEtaGlkZGVuPVwidHJ1ZVwiPiZ0aW1lczs8L3NwYW4+JztcbiAgICAgICAgICAgIG1vZGFsICs9ICc8L2J1dHRvbj4nO1xuICAgICAgICAgICAgbW9kYWwgKz0gJzxoNCBjbGFzcz1cIm1vZGFsLXRpdGxlXCIgaWQ9XCInICsgX2RlZmF1bHRzLmxhYmVsICsgJ1wiPicgKyBfZGVmYXVsdHMuc3RyaW5nLnRpdGxlICsgJzwvaDQ+JztcbiAgICAgICAgICAgIG1vZGFsICs9ICc8L2Rpdj4nO1xuICAgICAgICAgICAgbW9kYWwgKz0gJzxkaXYgY2xhc3M9XCJtb2RhbC1ib2R5XCI+JyArIF9kZWZhdWx0cy5zdHJpbmcuYm9keSArICc8L2Rpdj4nO1xuICAgICAgICAgICAgbW9kYWwgKz0gJzxkaXYgY2xhc3M9XCJtb2RhbC1mb290ZXJcIj4nO1xuICAgICAgICAgICAgbW9kYWwgKz0gJzxidXR0b24gdHlwZT1cImJ1dHRvblwiIGlkPVwiJyArIF9kZWZhdWx0cy5jbG9zZUlkICsgJ1wiICBjbGFzcz1cImJ0blwiIGRhdGEtZGlzbWlzcz1cIm1vZGFsXCI+JyArIF9kZWZhdWx0cy5zdHJpbmcuY2xvc2VCdG4gKyAnPC9idXR0b24+JztcbiAgICAgICAgICAgIG1vZGFsICs9ICc8YnV0dG9uIHR5cGU9XCJidXR0b25cIiBpZD1cIicgKyBfZGVmYXVsdHMuc3VibWl0SWQgKyAnXCIgY2xhc3M9XCJidG5cIj4nICsgX2RlZmF1bHRzLnN0cmluZy5zYXZlQnRuICsgJzwvYnV0dG9uPic7XG4gICAgICAgICAgICBtb2RhbCArPSAnPC9kaXY+PC9kaXY+PC9kaXY+PC9kaXY+JztcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgcmV0dXJuIG1vZGFsO1xuICAgICAgICB9LFxuICAgICAgICAvKipcbiAgICAgICAgICogUmV0dXJucyBhIEJTIEFsZXJ0IHdpdGggZHluYW1pYyBjb250ZW50LlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCByZW5kZXJBbGVydFxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gdHlwZSBUaGUgdHlwZSBvZiB0aGUgYWxlcnQuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb250ZW50IFRoZSBjb250ZW50IG9mIHRoZSBhbGVydC5cbiAgICAgICAgICogQHBhcmFtIHtCb29sZWFufSBkaXNtaXNzYWJsZSBTZXRzIHRoZSBvcHRpb24gdG8gbWFrZSB0aGUgYWxlcnQgZGlzbWlzc2FibGUsXG4gICAgICAgICAqIHRydWUgPSBkaXNtaXNzYWJsZS5cbiAgICAgICAgICogQHJldHVybnMge1N0cmluZ30gQSBIVE1MLVN0cmluZyB3aGljaCByZW5kZXJzIHRoZSBhbGVydC5cbiAgICAgICAgICovXG4gICAgICAgIHJlbmRlckFsZXJ0OiBmdW5jdGlvbiggdHlwZSwgY29udGVudCwgZGlzbWlzc2FibGUgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gdmlld2VyLmhlbHBlci5yZW5kZXJBbGVydCgpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIuaGVscGVyLnJlbmRlckFsZXJ0OiB0eXBlID0gJywgdHlwZSApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLmhlbHBlci5yZW5kZXJBbGVydDogY29udGVudCA9ICcsIGNvbnRlbnQgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci5oZWxwZXIucmVuZGVyQWxlcnQ6IGRpc21pc3NhYmxlID0gJywgZGlzbWlzc2FibGUgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHZhciBic0FsZXJ0ID0gJyc7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGJzQWxlcnQgKz0gJzxkaXYgcm9sZT1cImFsZXJ0XCIgY2xhc3M9XCJhbGVydCAnICsgdHlwZSArICcgYWxlcnQtZGlzbWlzc2libGUgZmFkZSBpblwiPic7XG4gICAgICAgICAgICBpZiAoIGRpc21pc3NhYmxlICkge1xuICAgICAgICAgICAgICAgIGJzQWxlcnQgKz0gJzxidXR0b24gYXJpYS1sYWJlbD1cIkNsb3NlXCIgZGF0YS1kaXNtaXNzPVwiYWxlcnRcIiBjbGFzcz1cImNsb3NlXCIgdHlwZT1cImJ1dHRvblwiPjxzcGFuIGFyaWEtaGlkZGVuPVwidHJ1ZVwiPsOXPC9zcGFuPjwvYnV0dG9uPic7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBic0FsZXJ0ICs9IGNvbnRlbnQ7XG4gICAgICAgICAgICBic0FsZXJ0ICs9ICc8L2Rpdj4nO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICByZXR1cm4gYnNBbGVydDtcbiAgICAgICAgfSxcbiAgICAgICAgLyoqXG4gICAgICAgICAqIE1ldGhvZCB0byBnZXQgdGhlIHZlcnNpb24gbnVtYmVyIG9mIHRoZSB1c2VkIE1TIEludGVybmV0IEV4cGxvcmVyLlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCBkZXRlY3RJRVZlcnNpb25cbiAgICAgICAgICogQHJldHVybnMge051bWJlcn0gVGhlIGJyb3dzZXIgdmVyc2lvbi5cbiAgICAgICAgICovXG4gICAgICAgIGRldGVjdElFVmVyc2lvbjogZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICB2YXIgdWEgPSB3aW5kb3cubmF2aWdhdG9yLnVzZXJBZ2VudDtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gSUUgMTAgYW5kIG9sZGVyXG4gICAgICAgICAgICB2YXIgbXNpZSA9IHVhLmluZGV4T2YoICdNU0lFICcgKTtcbiAgICAgICAgICAgIGlmICggbXNpZSA+IDAgKSB7XG4gICAgICAgICAgICAgICAgLy8gSUUgMTAgb3Igb2xkZXIgPT4gcmV0dXJuIHZlcnNpb24gbnVtYmVyXG4gICAgICAgICAgICAgICAgcmV0dXJuIHBhcnNlSW50KCB1YS5zdWJzdHJpbmcoIG1zaWUgKyA1LCB1YS5pbmRleE9mKCAnLicsIG1zaWUgKSApLCAxMCApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICAvLyBJRSAxMVxuICAgICAgICAgICAgdmFyIHRyaWRlbnQgPSB1YS5pbmRleE9mKCAnVHJpZGVudC8nICk7XG4gICAgICAgICAgICBpZiAoIHRyaWRlbnQgPiAwICkge1xuICAgICAgICAgICAgICAgIC8vIElFIDExID0+IHJldHVybiB2ZXJzaW9uIG51bWJlclxuICAgICAgICAgICAgICAgIHZhciBydiA9IHVhLmluZGV4T2YoICdydjonICk7XG4gICAgICAgICAgICAgICAgcmV0dXJuIHBhcnNlSW50KCB1YS5zdWJzdHJpbmcoIHJ2ICsgMywgdWEuaW5kZXhPZiggJy4nLCBydiApICksIDEwICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIElFIDEyK1xuICAgICAgICAgICAgdmFyIGVkZ2UgPSB1YS5pbmRleE9mKCAnRWRnZS8nICk7XG4gICAgICAgICAgICBpZiAoIGVkZ2UgPiAwICkge1xuICAgICAgICAgICAgICAgIC8vIEVkZ2UgKElFIDEyKykgPT4gcmV0dXJuIHZlcnNpb24gbnVtYmVyXG4gICAgICAgICAgICAgICAgcmV0dXJuIHBhcnNlSW50KCB1YS5zdWJzdHJpbmcoIGVkZ2UgKyA1LCB1YS5pbmRleE9mKCAnLicsIGVkZ2UgKSApLCAxMCApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICAvLyBvdGhlciBicm93c2VyXG4gICAgICAgICAgICByZXR1cm4gZmFsc2U7XG4gICAgICAgIH0sXG4gICAgICAgIFxuICAgICAgICAvKipcbiAgICAgICAgICogTWV0aG9kIHRvIGNoZWNrIGlmIGl0wrRzIHBvc3NpYmxlIHRvIHdyaXRlIHRvIGxvY2FsIFN0b3JhZ2VcbiAgICAgICAgICogXG4gICAgICAgICAqIEBtZXRob2QgY2hlY2tMb2NhbFN0b3JhZ2VcbiAgICAgICAgICogQHJldHVybnMge0Jvb2xlYW59IHRydWUgb3IgZmFsc2VcbiAgICAgICAgICovXG4gICAgICAgIGNoZWNrTG9jYWxTdG9yYWdlOiBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIGlmICggdHlwZW9mIGxvY2FsU3RvcmFnZSA9PT0gJ29iamVjdCcgKSB7XG4gICAgICAgICAgICAgICAgdHJ5IHtcbiAgICAgICAgICAgICAgICAgICAgbG9jYWxTdG9yYWdlLnNldEl0ZW0oICd0ZXN0TG9jYWxTdG9yYWdlJywgMSApO1xuICAgICAgICAgICAgICAgICAgICBsb2NhbFN0b3JhZ2UucmVtb3ZlSXRlbSggJ3Rlc3RMb2NhbFN0b3JhZ2UnICk7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICByZXR1cm4gdHJ1ZTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgY2F0Y2ggKCBlcnJvciApIHtcbiAgICAgICAgICAgICAgICAgICAgY29uc29sZS5lcnJvciggJ05vdCBwb3NzaWJsZSB0byB3cml0ZSBpbiBsb2NhbCBTdG9yYWdlOiAnLCBlcnJvciApO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIGZhbHNlO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgfSxcbiAgICAgICAgXG4gICAgICAgIC8qKlxuICAgICAgICAgKiBNZXRob2QgdG8gcmVuZGVyIGEgd2FybmluZyBwb3BvdmVyLlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCByZW5kZXJXYXJuaW5nUG9wb3ZlclxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gbXNnIFRoZSBtZXNzYWdlIHRvIHNob3cgaW4gdGhlIHBvcG92ZXIuXG4gICAgICAgICAqIEByZXR1cm5zIHtPYmplY3R9IEFuIGpRdWVyeSBPYmplY3QgdG8gYXBwZW5kIHRvIERPTS5cbiAgICAgICAgICovXG4gICAgICAgIHJlbmRlcldhcm5pbmdQb3BvdmVyOiBmdW5jdGlvbiggbXNnICkge1xuICAgICAgICAgICAgdmFyIHBvcG92ZXIgPSAkKCAnPGRpdiAvPicgKTtcbiAgICAgICAgICAgIHZhciBwb3BvdmVyVGV4dCA9ICQoICc8cCAvPicgKTtcbiAgICAgICAgICAgIHZhciBwb3BvdmVyQnV0dG9uID0gJCggJzxidXR0b24gLz4nICk7XG4gICAgICAgICAgICB2YXIgcG9wb3ZlckJ1dHRvbkljb24gPSAkKCAnPGkgYXJpYS1oaWRkZW49XCJ0cnVlXCIgLz4nICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHBvcG92ZXIuYWRkQ2xhc3MoICd3YXJuaW5nLXBvcG92ZXInICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIGJ1aWxkIGJ1dHRvblxuICAgICAgICAgICAgcG9wb3ZlckJ1dHRvbi5hZGRDbGFzcyggJ2J0bi1jbGVhbicgKTtcbiAgICAgICAgICAgIHBvcG92ZXJCdXR0b24uYXR0ciggJ2RhdGEtdG9nZ2xlJywgJ3dhcm5pbmctcG9wb3ZlcicgKTtcbiAgICAgICAgICAgIHBvcG92ZXJCdXR0b25JY29uLmFkZENsYXNzKCAnZmEgZmEtdGltZXMnICk7XG4gICAgICAgICAgICBwb3BvdmVyQnV0dG9uLmFwcGVuZCggcG9wb3ZlckJ1dHRvbkljb24gKTtcbiAgICAgICAgICAgIHBvcG92ZXIuYXBwZW5kKCBwb3BvdmVyQnV0dG9uICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIGJ1aWxkIHRleHRcbiAgICAgICAgICAgIHBvcG92ZXJUZXh0Lmh0bWwoIG1zZyApO1xuICAgICAgICAgICAgcG9wb3Zlci5hcHBlbmQoIHBvcG92ZXJUZXh0ICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHJldHVybiBwb3BvdmVyO1xuICAgICAgICB9LFxuICAgICAgICBcbiAgICAgICAgLyoqXG4gICAgICAgICAqIE1ldGhvZCB0byBlcXVhbCBoZWlnaHQgb2Ygc2lkZWJhciBhbmQgY29udGVudC5cbiAgICAgICAgICogXG4gICAgICAgICAqIEBtZXRob2QgZXF1YWxIZWlnaHRcbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IHNpZGViYXIgVGhlIHNlbGVjdG9yIG9mIHRoZSBzaWRlYmFyLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29udGVudCBUaGUgc2VsZWN0b3Igb2YgdGhlIGNvbnRlbnQuXG4gICAgICAgICAqL1xuICAgICAgICBlcXVhbEhlaWdodDogZnVuY3Rpb24oIHNpZGViYXIsIGNvbnRlbnQgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gdmlld2VyLmhlbHBlci5lcXVhbEhlaWdodCgpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIuaGVscGVyLmVxdWFsSGVpZ2h0OiBzaWRlYmFyID0gJywgc2lkZWJhciApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLmhlbHBlci5lcXVhbEhlaWdodDogY29udGVudCA9ICcsIGNvbnRlbnQgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgdmFyICRzaWRlYmFyID0gJCggc2lkZWJhciApO1xuICAgICAgICAgICAgdmFyICRjb250ZW50ID0gJCggY29udGVudCApO1xuICAgICAgICAgICAgdmFyIHNpZGViYXJIZWlnaHQgPSAkc2lkZWJhci5vdXRlckhlaWdodCgpO1xuICAgICAgICAgICAgdmFyIGNvbnRlbnRIZWlnaHQgPSAkY29udGVudC5vdXRlckhlaWdodCgpO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBpZiAoIHNpZGViYXJIZWlnaHQgPiBjb250ZW50SGVpZ2h0ICkge1xuICAgICAgICAgICAgICAgICRjb250ZW50LmNzcygge1xuICAgICAgICAgICAgICAgICAgICAnbWluLWhlaWdodCc6ICggc2lkZWJhckhlaWdodCApICsgJ3B4J1xuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSxcbiAgICAgICAgXG4gICAgICAgIC8qKlxuICAgICAgICAgKiBNZXRob2QgdG8gdmFsaWRhdGUgdGhlIHJlQ0FQVENIQSByZXNwb25zZS5cbiAgICAgICAgICogXG4gICAgICAgICAqIEBtZXRob2QgdmFsaWRhdGVSZUNhcHRjaGFcbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IHdyYXBwZXIgVGhlIHJlQ0FQVENIQSB3aWRnZXQgd3JhcHBlci5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGtleSBUaGUgcmVDQVBUQ0hBIHNpdGUga2V5LlxuICAgICAgICAgKiBAcmV0dXJucyB7Qm9vbGVhbn0gUmV0dXJucyB0cnVlIGlmIHRoZSByZXNwb25zZSBpcyB2YWxpZC5cbiAgICAgICAgICovXG4gICAgICAgIHZhbGlkYXRlUmVDYXB0Y2hhOiBmdW5jdGlvbiggd3JhcHBlciwga2V5ICkge1xuICAgICAgICAgICAgdmFyIHdpZGdldCA9IGdyZWNhcHRjaGEucmVuZGVyKCB3cmFwcGVyLCB7XG4gICAgICAgICAgICAgICAgc2l0ZWtleToga2V5LFxuICAgICAgICAgICAgICAgIGNhbGxiYWNrOiBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgdmFyIHJlc3BvbnNlID0gZ3JlY2FwdGNoYS5nZXRSZXNwb25zZSggd2lkZ2V0ICk7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggcmVzcG9uc2UgKTtcbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgIGlmICggcmVzcG9uc2UgPT0gMCApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiBmYWxzZTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiB0cnVlO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfSApO1xuICAgICAgICB9LFxuICAgICAgICBcbiAgICAgICAgLyoqXG4gICAgICAgICAqIE1ldGhvZCB0byBnZXQgdGhlIGN1cnJlbnQgdXNlZCBicm93c2VyLlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCBnZXRDdXJyZW50QnJvd3NlclxuICAgICAgICAgKiBAcmV0dXJucyB7U3RyaW5nfSBUaGUgbmFtZSBvZiB0aGUgY3VycmVudCBCcm93c2VyLlxuICAgICAgICAgKi9cbiAgICAgICAgZ2V0Q3VycmVudEJyb3dzZXI6IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgLy8gT3BlcmEgOC4wK1xuICAgICAgICAgICAgdmFyIGlzT3BlcmEgPSAoICEhd2luZG93Lm9wciAmJiAhIW9wci5hZGRvbnMgKSB8fCAhIXdpbmRvdy5vcGVyYSB8fCBuYXZpZ2F0b3IudXNlckFnZW50LmluZGV4T2YoICcgT1BSLycgKSA+PSAwO1xuICAgICAgICAgICAgLy8gRmlyZWZveCAxLjArXG4gICAgICAgICAgICB2YXIgaXNGaXJlZm94ID0gdHlwZW9mIEluc3RhbGxUcmlnZ2VyICE9PSAndW5kZWZpbmVkJztcbiAgICAgICAgICAgIC8vIFNhZmFyaSAzLjArIFwiW29iamVjdCBIVE1MRWxlbWVudENvbnN0cnVjdG9yXVwiXG4gICAgICAgICAgICB2YXIgaXNTYWZhcmkgPSAvY29uc3RydWN0b3IvaS50ZXN0KCB3aW5kb3cuSFRNTEVsZW1lbnQgKSB8fCAoIGZ1bmN0aW9uKCBwICkge1xuICAgICAgICAgICAgICAgIHJldHVybiBwLnRvU3RyaW5nKCkgPT09IFwiW29iamVjdCBTYWZhcmlSZW1vdGVOb3RpZmljYXRpb25dXCI7XG4gICAgICAgICAgICB9ICkoICF3aW5kb3dbICdzYWZhcmknIF0gfHwgKCB0eXBlb2Ygc2FmYXJpICE9PSAndW5kZWZpbmVkJyAmJiBzYWZhcmkucHVzaE5vdGlmaWNhdGlvbiApICk7XG4gICAgICAgICAgICAvLyBJbnRlcm5ldCBFeHBsb3JlciA2LTExXG4gICAgICAgICAgICB2YXIgaXNJRSA9IC8qIEBjY19vbiFAICovZmFsc2UgfHwgISFkb2N1bWVudC5kb2N1bWVudE1vZGU7XG4gICAgICAgICAgICAvLyBFZGdlIDIwK1xuICAgICAgICAgICAgdmFyIGlzRWRnZSA9ICFpc0lFICYmICEhd2luZG93LlN0eWxlTWVkaWE7XG4gICAgICAgICAgICAvLyBDaHJvbWUgMStcbiAgICAgICAgICAgIHZhciBpc0Nocm9tZSA9ICEhd2luZG93LmNocm9tZSAmJiAhIXdpbmRvdy5jaHJvbWUud2Vic3RvcmU7XG4gICAgICAgICAgICAvLyBCbGluayBlbmdpbmUgZGV0ZWN0aW9uXG4gICAgICAgICAgICAvLyB2YXIgaXNCbGluayA9ICggaXNDaHJvbWUgfHwgaXNPcGVyYSApICYmICEhd2luZG93LkNTUztcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgaWYgKCBpc09wZXJhICkge1xuICAgICAgICAgICAgICAgIHJldHVybiAnT3BlcmEnO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSBpZiAoIGlzRmlyZWZveCApIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gJ0ZpcmVmb3gnO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSBpZiAoIGlzU2FmYXJpICkge1xuICAgICAgICAgICAgICAgIHJldHVybiAnU2FmYXJpJztcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2UgaWYgKCBpc0lFICkge1xuICAgICAgICAgICAgICAgIHJldHVybiAnSUUnO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSBpZiAoIGlzRWRnZSApIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gJ0VkZ2UnO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSBpZiAoIGlzQ2hyb21lICkge1xuICAgICAgICAgICAgICAgIHJldHVybiAnQ2hyb21lJztcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSxcbiAgICB9O1xuICAgIFxuICAgIHZpZXdlci5sb2NhbFN0b3JhZ2VQb3NzaWJsZSA9IHZpZXdlci5oZWxwZXIuY2hlY2tMb2NhbFN0b3JhZ2UoKTtcbiAgICBcbiAgICByZXR1cm4gdmlld2VyO1xuICAgIFxufSApKCB2aWV3ZXJKUyB8fCB7fSwgalF1ZXJ5ICk7XG4iLCIvKipcbiAqIFRoaXMgZmlsZSBpcyBwYXJ0IG9mIHRoZSBHb29iaSB2aWV3ZXIgLSBhIGNvbnRlbnQgcHJlc2VudGF0aW9uIGFuZCBtYW5hZ2VtZW50XG4gKiBhcHBsaWNhdGlvbiBmb3IgZGlnaXRpemVkIG9iamVjdHMuXG4gKiBcbiAqIFZpc2l0IHRoZXNlIHdlYnNpdGVzIGZvciBtb3JlIGluZm9ybWF0aW9uLiAtIGh0dHA6Ly93d3cuaW50cmFuZGEuY29tIC1cbiAqIGh0dHA6Ly9kaWdpdmVyc28uY29tXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBmcmVlIHNvZnR3YXJlOyB5b3UgY2FuIHJlZGlzdHJpYnV0ZSBpdCBhbmQvb3IgbW9kaWZ5IGl0IHVuZGVyIHRoZSB0ZXJtc1xuICogb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFzIHB1Ymxpc2hlZCBieSB0aGUgRnJlZSBTb2Z0d2FyZSBGb3VuZGF0aW9uOyBlaXRoZXJcbiAqIHZlcnNpb24gMiBvZiB0aGUgTGljZW5zZSwgb3IgKGF0IHlvdXIgb3B0aW9uKSBhbnkgbGF0ZXIgdmVyc2lvbi5cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGRpc3RyaWJ1dGVkIGluIHRoZSBob3BlIHRoYXQgaXQgd2lsbCBiZSB1c2VmdWwsIGJ1dCBXSVRIT1VUIEFOWVxuICogV0FSUkFOVFk7IHdpdGhvdXQgZXZlbiB0aGUgaW1wbGllZCB3YXJyYW50eSBvZiBNRVJDSEFOVEFCSUxJVFkgb3IgRklUTkVTUyBGT1IgQVxuICogUEFSVElDVUxBUiBQVVJQT1NFLiBTZWUgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGZvciBtb3JlIGRldGFpbHMuXG4gKiBcbiAqIFlvdSBzaG91bGQgaGF2ZSByZWNlaXZlZCBhIGNvcHkgb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFsb25nIHdpdGggdGhpc1xuICogcHJvZ3JhbS4gSWYgbm90LCBzZWUgPGh0dHA6Ly93d3cuZ251Lm9yZy9saWNlbnNlcy8+LlxuICogXG4gKiBCYXNlLU1vZHVsZSB3aGljaCBpbml0aWFsaXplIHRoZSBnbG9iYWwgdmlld2VyIG9iamVjdC5cbiAqIFxuICogQHZlcnNpb24gMy4yLjBcbiAqIEBtb2R1bGUgdmlld2VySlNcbiAqL1xudmFyIHZpZXdlckpTID0gKCBmdW5jdGlvbigpIHtcbiAgICAndXNlIHN0cmljdCc7XG4gICAgXG4gICAgdmFyIF9kZWJ1ZyA9IGZhbHNlO1xuICAgIHZhciBfZGVmYXVsdHMgPSB7XG4gICAgICAgIGN1cnJlbnRQYWdlOiAnJyxcbiAgICAgICAgYnJvd3NlcjogJycsXG4gICAgICAgIHNpZGViYXJTZWxlY3RvcjogJyNzaWRlYmFyJyxcbiAgICAgICAgY29udGVudFNlbGVjdG9yOiAnI21haW4nLFxuICAgICAgICBlcXVhbEhlaWdodFJTU0ludGVydmFsOiAxMDAwLFxuICAgICAgICBlcXVhbEhlaWdodEludGVydmFsOiA1MDAsXG4gICAgICAgIG1lc3NhZ2VCb3hTZWxlY3RvcjogJy5tZXNzYWdlcyAuYWxlcnQnLFxuICAgICAgICBtZXNzYWdlQm94SW50ZXJ2YWw6IDEwMDAsXG4gICAgICAgIG1lc3NhZ2VCb3hUaW1lb3V0OiA4MDAwLFxuICAgICAgICBwYWdlU2Nyb2xsU2VsZWN0b3I6ICcuaWNvbi10b3RvcCcsXG4gICAgICAgIHBhZ2VTY3JvbGxBbmNob3I6ICcjdG9wJyxcbiAgICAgICAgd2lkZ2V0TmVyU2lkZWJhclJpZ2h0OiBmYWxzZVxuICAgIH07XG4gICAgXG4gICAgdmFyIHZpZXdlciA9IHt9O1xuICAgIFxuICAgIHZpZXdlci5pbml0ID0gZnVuY3Rpb24oIGNvbmZpZyApIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLmluaXQnICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLmluaXQ6IGNvbmZpZyAtICcsIGNvbmZpZyApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICAkLmV4dGVuZCggdHJ1ZSwgX2RlZmF1bHRzLCBjb25maWcgKTtcbiAgICAgICAgXG4gICAgICAgIC8vIGRldGVjdCBjdXJyZW50IGJyb3dzZXJcbiAgICAgICAgX2RlZmF1bHRzLmJyb3dzZXIgPSB2aWV3ZXJKUy5oZWxwZXIuZ2V0Q3VycmVudEJyb3dzZXIoKTtcbiAgICAgICAgXG4gICAgICAgIGNvbnNvbGUuaW5mbyggJ0N1cnJlbnQgUGFnZSA9ICcsIF9kZWZhdWx0cy5jdXJyZW50UGFnZSApO1xuICAgICAgICBjb25zb2xlLmluZm8oICdDdXJyZW50IEJyb3dzZXIgPSAnLCBfZGVmYXVsdHMuYnJvd3NlciApO1xuICAgICAgICBcbiAgICAgICAgLypcbiAgICAgICAgICogISBJRTEwIHZpZXdwb3J0IGhhY2sgZm9yIFN1cmZhY2UvZGVza3RvcCBXaW5kb3dzIDggYnVnIENvcHlyaWdodCAyMDE0LTIwMTVcbiAgICAgICAgICogVHdpdHRlciwgSW5jLiBMaWNlbnNlZCB1bmRlciBNSVRcbiAgICAgICAgICogKGh0dHBzOi8vZ2l0aHViLmNvbS90d2JzL2Jvb3RzdHJhcC9ibG9iL21hc3Rlci9MSUNFTlNFKVxuICAgICAgICAgKi9cblxuICAgICAgICAvLyBTZWUgdGhlIEdldHRpbmcgU3RhcnRlZCBkb2NzIGZvciBtb3JlIGluZm9ybWF0aW9uOlxuICAgICAgICAvLyBodHRwOi8vZ2V0Ym9vdHN0cmFwLmNvbS9nZXR0aW5nLXN0YXJ0ZWQvI3N1cHBvcnQtaWUxMC13aWR0aFxuICAgICAgICAoIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgJ3VzZSBzdHJpY3QnO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBpZiAoIG5hdmlnYXRvci51c2VyQWdlbnQubWF0Y2goIC9JRU1vYmlsZVxcLzEwXFwuMC8gKSApIHtcbiAgICAgICAgICAgICAgICB2YXIgbXNWaWV3cG9ydFN0eWxlID0gZG9jdW1lbnQuY3JlYXRlRWxlbWVudCggJ3N0eWxlJyApXG4gICAgICAgICAgICAgICAgbXNWaWV3cG9ydFN0eWxlLmFwcGVuZENoaWxkKCBkb2N1bWVudC5jcmVhdGVUZXh0Tm9kZSggJ0AtbXMtdmlld3BvcnR7d2lkdGg6YXV0byFpbXBvcnRhbnR9JyApIClcbiAgICAgICAgICAgICAgICBkb2N1bWVudC5xdWVyeVNlbGVjdG9yKCAnaGVhZCcgKS5hcHBlbmRDaGlsZCggbXNWaWV3cG9ydFN0eWxlIClcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSApKCk7XG4gICAgICAgIFxuICAgICAgICAvLyBlbmFibGUgQlMgdG9vbHRpcHNcbiAgICAgICAgJCggJ1tkYXRhLXRvZ2dsZT1cInRvb2x0aXBcIl0nICkudG9vbHRpcCgpO1xuICAgICAgICBcbiAgICAgICAgLy8gcmVuZGVyIHdhcm5pbmcgaWYgbG9jYWwgc3RvcmFnZSBpcyBub3QgdXNlYWJsZVxuICAgICAgICBpZiAoICF2aWV3ZXIubG9jYWxTdG9yYWdlUG9zc2libGUgKSB7XG4gICAgICAgICAgICB2YXIgd2FybmluZ1BvcG92ZXIgPSB0aGlzLmhlbHBlclxuICAgICAgICAgICAgICAgICAgICAucmVuZGVyV2FybmluZ1BvcG92ZXIoICdJaHIgQnJvd3NlciBiZWZpbmRldCBzaWNoIGltIHByaXZhdGVuIE1vZHVzIHVuZCB1bnRlcnN0w7x0enQgZGllIE3DtmdsaWNoa2VpdCBJbmZvcm1hdGlvbmVuIHp1ciBTZWl0ZSBsb2thbCB6dSBzcGVpY2hlcm4gbmljaHQuIEF1cyBkaWVzZW0gR3J1bmQgc2luZCBuaWNodCBhbGxlIEZ1bmt0aW9uZW4gZGVzIHZpZXdlcnMgdmVyZsO8Z2Jhci4gQml0dGUgdmVybGFzZW4gU2llIGRlbiBwcml2YXRlbiBNb2R1cyBvZGVyIGJlbnV0emVuIGVpbmVuIGFsdGVybmF0aXZlbiBCcm93c2VyLiBWaWVsZW4gRGFuay4nICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgICQoICdib2R5JyApLmFwcGVuZCggd2FybmluZ1BvcG92ZXIgKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgJCggJ1tkYXRhLXRvZ2dsZT1cIndhcm5pbmctcG9wb3ZlclwiXScgKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgJCggJy53YXJuaW5nLXBvcG92ZXInICkuZmFkZU91dCggJ2Zhc3QnLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgJCggJy53YXJuaW5nLXBvcG92ZXInICkucmVtb3ZlKCk7XG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgfSApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICAvLyBvZmYgY2FudmFzXG4gICAgICAgICQoICdbZGF0YS10b2dnbGU9XCJvZmZjYW52YXNcIl0nICkuY2xpY2soIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgdmFyIGljb24gPSAkKCB0aGlzICkuY2hpbGRyZW4oICcuZ2x5cGhpY29uJyApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICAkKCAnLnJvdy1vZmZjYW52YXMnICkudG9nZ2xlQ2xhc3MoICdhY3RpdmUnICk7XG4gICAgICAgICAgICAkKCB0aGlzICkudG9nZ2xlQ2xhc3MoICdpbicgKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgaWYgKCBpY29uLmhhc0NsYXNzKCAnZ2x5cGhpY29uLW9wdGlvbi12ZXJ0aWNhbCcgKSApIHtcbiAgICAgICAgICAgICAgICBpY29uLnJlbW92ZUNsYXNzKCAnZ2x5cGhpY29uLW9wdGlvbi12ZXJ0aWNhbCcgKS5hZGRDbGFzcyggJ2dseXBoaWNvbi1vcHRpb24taG9yaXpvbnRhbCcgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIGljb24ucmVtb3ZlQ2xhc3MoICdnbHlwaGljb24tb3B0aW9uLWhvcml6b250YWwnICkuYWRkQ2xhc3MoICdnbHlwaGljb24tb3B0aW9uLXZlcnRpY2FsJyApO1xuICAgICAgICAgICAgfVxuICAgICAgICB9ICk7XG4gICAgICAgIFxuICAgICAgICAvLyB0b2dnbGUgbW9iaWxlIG5hdmlnYXRpb25cbiAgICAgICAgJCggJ1tkYXRhLXRvZ2dsZT1cIm1vYmlsZW5hdlwiXScgKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAkKCAnLmJ0bi10b2dnbGUuc2VhcmNoJyApLnJlbW92ZUNsYXNzKCAnaW4nICk7XG4gICAgICAgICAgICAkKCAnLmhlYWRlci1hY3Rpb25zX19zZWFyY2gnICkuaGlkZSgpO1xuICAgICAgICAgICAgJCggJy5idG4tdG9nZ2xlLmxhbmd1YWdlJyApLnJlbW92ZUNsYXNzKCAnaW4nICk7XG4gICAgICAgICAgICAkKCAnI2NoYW5nZUxvY2FsJyApLmhpZGUoKTtcbiAgICAgICAgICAgICQoICcjbW9iaWxlTmF2JyApLnNsaWRlVG9nZ2xlKCAnZmFzdCcgKTtcbiAgICAgICAgfSApO1xuICAgICAgICAkKCAnW2RhdGEtdG9nZ2xlPVwibW9iaWxlLWltYWdlLWNvbnRyb2xzXCJdJyApLm9uKCAnY2xpY2snLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICQoICcuaW1hZ2UtY29udHJvbHNfX2FjdGlvbnMnICkuc2xpZGVUb2dnbGUoICdmYXN0JyApO1xuICAgICAgICB9ICk7XG4gICAgICAgIFxuICAgICAgICAvLyB0b2dnbGUgbGFuZ3VhZ2VcbiAgICAgICAgJCggJ1tkYXRhLXRvZ2dsZT1cImxhbmd1YWdlXCJdJyApLm9uKCAnY2xpY2snLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICQoICcuYnRuLXRvZ2dsZS5zZWFyY2gnICkucmVtb3ZlQ2xhc3MoICdpbicgKTtcbiAgICAgICAgICAgICQoICcuaGVhZGVyLWFjdGlvbnNfX3NlYXJjaCcgKS5oaWRlKCk7XG4gICAgICAgICAgICAkKCB0aGlzICkudG9nZ2xlQ2xhc3MoICdpbicgKTtcbiAgICAgICAgICAgICQoICcjY2hhbmdlTG9jYWwnICkuZmFkZVRvZ2dsZSggJ2Zhc3QnICk7XG4gICAgICAgIH0gKTtcbiAgICAgICAgXG4gICAgICAgIC8vIHRvZ2dsZSBzZWFyY2hcbiAgICAgICAgJCggJ1tkYXRhLXRvZ2dsZT1cInNlYXJjaFwiXScgKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAkKCAnLmJ0bi10b2dnbGUubGFuZ3VhZ2UnICkucmVtb3ZlQ2xhc3MoICdpbicgKTtcbiAgICAgICAgICAgICQoICcjY2hhbmdlTG9jYWwnICkuaGlkZSgpO1xuICAgICAgICAgICAgJCggdGhpcyApLnRvZ2dsZUNsYXNzKCAnaW4nICk7XG4gICAgICAgICAgICAkKCAnLmhlYWRlci1hY3Rpb25zX19zZWFyY2gnICkuZmFkZVRvZ2dsZSggJ2Zhc3QnICk7XG4gICAgICAgIH0gKTtcbiAgICAgICAgXG4gICAgICAgIC8vIHNldCBjb250ZW50IGhlaWdodCB0byBzaWRlYmFyIGhlaWdodFxuICAgICAgICBpZiAoIHdpbmRvdy5tYXRjaE1lZGlhKCAnKG1heC13aWR0aDogNzY4cHgpJyApLm1hdGNoZXMgKSB7XG4gICAgICAgICAgICBpZiAoICQoICcucnNzX3dyYXBwJyApLmxlbmd0aCA+IDAgKSB7XG4gICAgICAgICAgICAgICAgc2V0VGltZW91dCggZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgIHZpZXdlckpTLmhlbHBlci5lcXVhbEhlaWdodCggX2RlZmF1bHRzLnNpZGViYXJTZWxlY3RvciwgX2RlZmF1bHRzLmNvbnRlbnRTZWxlY3RvciApO1xuICAgICAgICAgICAgICAgIH0sIF9kZWZhdWx0cy5lcXVhbEhlaWdodFJTU0ludGVydmFsICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICBzZXRUaW1lb3V0KCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgdmlld2VySlMuaGVscGVyLmVxdWFsSGVpZ2h0KCBfZGVmYXVsdHMuc2lkZWJhclNlbGVjdG9yLCBfZGVmYXVsdHMuY29udGVudFNlbGVjdG9yICk7XG4gICAgICAgICAgICAgICAgfSwgX2RlZmF1bHRzLmVxdWFsSGVpZ2h0SW50ZXJ2YWwgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgICQoIHdpbmRvdyApLm9uKCBcIm9yaWVudGF0aW9uY2hhbmdlXCIsIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgIHZpZXdlckpTLmhlbHBlci5lcXVhbEhlaWdodCggX2RlZmF1bHRzLnNpZGViYXJTZWxlY3RvciwgX2RlZmF1bHRzLmNvbnRlbnRTZWxlY3RvciApO1xuICAgICAgICAgICAgfSApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICAvLyBmYWRlIG91dCBtZXNzYWdlIGJveCBpZiBpdCBleGlzdHNcbiAgICAgICAgKCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIHZhciBmYWRlb3V0U2NoZWR1bGVkID0gZmFsc2U7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHNldEludGVydmFsKCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICBpZiAoICQoIF9kZWZhdWx0cy5tZXNzYWdlQm94U2VsZWN0b3IgKS5zaXplKCkgPiAwICkge1xuICAgICAgICAgICAgICAgICAgICBpZiAoICFmYWRlb3V0U2NoZWR1bGVkICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgZmFkZW91dFNjaGVkdWxlZCA9IHRydWU7XG4gICAgICAgICAgICAgICAgICAgICAgICB2YXIgbWVzc2FnZVRpbWVyID0gc2V0VGltZW91dCggZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgJCggX2RlZmF1bHRzLm1lc3NhZ2VCb3hTZWxlY3RvciApLmVhY2goIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCB0aGlzICkuZmFkZU91dCggJ3Nsb3cnLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICQoIHRoaXMgKS5wYXJlbnQoKS5yZW1vdmUoKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSApXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGZhZGVvdXRTY2hlZHVsZWQgPSBmYWxzZTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH0sIF9kZWZhdWx0cy5tZXNzYWdlQm94VGltZW91dCApO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfSwgX2RlZmF1bHRzLm1lc3NhZ2VCb3hJbnRlcnZhbCApO1xuICAgICAgICB9ICkoKTtcbiAgICAgICAgXG4gICAgICAgIC8vIGFkZCBjbGFzcyBvbiB0b2dnbGUgc2lkZWJhciB3aWRnZXQgKENNUyBpbmRpdmlkdWFsIHNpZGViYXIgd2lkZ2V0cylcbiAgICAgICAgJCggJy5jb2xsYXBzZScgKS5vbiggJ3Nob3cuYnMuY29sbGFwc2UnLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICQoIHRoaXMgKS5wcmV2KCkuZmluZCggJy5nbHlwaGljb24nICkucmVtb3ZlQ2xhc3MoICdnbHlwaGljb24tYXJyb3ctZG93bicgKS5hZGRDbGFzcyggJ2dseXBoaWNvbi1hcnJvdy11cCcgKTtcbiAgICAgICAgfSApO1xuICAgICAgICBcbiAgICAgICAgJCggJy5jb2xsYXBzZScgKS5vbiggJ2hpZGUuYnMuY29sbGFwc2UnLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICQoIHRoaXMgKS5wcmV2KCkuZmluZCggJy5nbHlwaGljb24nICkucmVtb3ZlQ2xhc3MoICdnbHlwaGljb24tYXJyb3ctdXAnICkuYWRkQ2xhc3MoICdnbHlwaGljb24tYXJyb3ctZG93bicgKTtcbiAgICAgICAgfSApO1xuICAgICAgICBcbiAgICAgICAgLy8gc2Nyb2xsIHBhZ2UgYW5pbWF0ZWRcbiAgICAgICAgdGhpcy5wYWdlU2Nyb2xsLmluaXQoIF9kZWZhdWx0cy5wYWdlU2Nyb2xsU2VsZWN0b3IsIF9kZWZhdWx0cy5wYWdlU2Nyb2xsQW5jaG9yICk7XG4gICAgICAgIFxuICAgICAgICAvLyBjaGVjayBmb3Igc2lkZWJhciB0b2MgYW5kIHNldCB2aWV3cG9ydCBwb3NpdGlvblxuICAgICAgICBpZiAoIHZpZXdlci5sb2NhbFN0b3JhZ2VQb3NzaWJsZSApIHtcbiAgICAgICAgICAgIGlmICggJCggJyNpbWFnZV9jb250YWluZXInICkubGVuZ3RoID4gMCB8fCBjdXJyZW50UGFnZSA9PT0gJ3JlYWRpbmdtb2RlJyApIHtcbiAgICAgICAgICAgICAgICBpZiAoIGxvY2FsU3RvcmFnZS5nZXRJdGVtKCAnY3VycklkRG9jJyApID09PSBudWxsICkge1xuICAgICAgICAgICAgICAgICAgICBsb2NhbFN0b3JhZ2Uuc2V0SXRlbSggJ2N1cnJJZERvYycsICdmYWxzZScgKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgdGhpcy5oZWxwZXIuc2F2ZVNpZGViYXJUb2NQb3NpdGlvbigpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgbG9jYWxTdG9yYWdlLnNldEl0ZW0oICdzaWRlYmFyVG9jU2Nyb2xsUG9zaXRpb24nLCAwICk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIC8vIEFKQVggTG9hZGVyIEV2ZW50bGlzdGVuZXJcbiAgICAgICAgaWYgKCB0eXBlb2YganNmICE9PSAndW5kZWZpbmVkJyApIHtcbiAgICAgICAgICAgIGpzZi5hamF4LmFkZE9uRXZlbnQoIGZ1bmN0aW9uKCBkYXRhICkge1xuICAgICAgICAgICAgICAgIHZhciBhamF4c3RhdHVzID0gZGF0YS5zdGF0dXM7XG4gICAgICAgICAgICAgICAgdmFyIGFqYXhsb2FkZXIgPSBkb2N1bWVudC5nZXRFbGVtZW50QnlJZCggXCJBSkFYTG9hZGVyXCIgKTtcbiAgICAgICAgICAgICAgICB2YXIgYWpheGxvYWRlclNpZGViYXJUb2MgPSBkb2N1bWVudC5nZXRFbGVtZW50QnlJZCggXCJBSkFYTG9hZGVyU2lkZWJhclRvY1wiICk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgaWYgKCBhamF4bG9hZGVyU2lkZWJhclRvYyAmJiBhamF4bG9hZGVyICkge1xuICAgICAgICAgICAgICAgICAgICBzd2l0Y2ggKCBhamF4c3RhdHVzICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgY2FzZSBcImJlZ2luXCI6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgYWpheGxvYWRlclNpZGViYXJUb2Muc3R5bGUuZGlzcGxheSA9ICdibG9jayc7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgYWpheGxvYWRlci5zdHlsZS5kaXNwbGF5ID0gJ25vbmUnO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICBjYXNlIFwiY29tcGxldGVcIjpcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBhamF4bG9hZGVyU2lkZWJhclRvYy5zdHlsZS5kaXNwbGF5ID0gJ25vbmUnO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGFqYXhsb2FkZXIuc3R5bGUuZGlzcGxheSA9ICdub25lJztcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgY2FzZSBcInN1Y2Nlc3NcIjpcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyBlbmFibGUgQlMgdG9vbHRpcHNcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCAnW2RhdGEtdG9nZ2xlPVwidG9vbHRpcFwiXScgKS50b29sdGlwKCk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgaWYgKCB2aWV3ZXIubG9jYWxTdG9yYWdlUG9zc2libGUgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZpZXdlci5oZWxwZXIuc2F2ZVNpZGViYXJUb2NQb3NpdGlvbigpO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgJCggJy53aWRnZXQtdG9jLWVsZW0td3JhcHAnICkuc2Nyb2xsVG9wKCBsb2NhbFN0b3JhZ2Uuc2lkZWJhclRvY1Njcm9sbFBvc2l0aW9uICk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIHNldCBjb250ZW50IGhlaWdodCB0byBzaWRlYmFyIGhlaWdodFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGlmICggd2luZG93Lm1hdGNoTWVkaWEoICcobWF4LXdpZHRoOiA3NjhweCknICkubWF0Y2hlcyApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdmlld2VySlMuaGVscGVyLmVxdWFsSGVpZ2h0KCBfZGVmYXVsdHMuc2lkZWJhclNlbGVjdG9yLCBfZGVmYXVsdHMuY29udGVudFNlbGVjdG9yICk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCB3aW5kb3cgKS5vZmYoKS5vbiggXCJvcmllbnRhdGlvbmNoYW5nZVwiLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZpZXdlckpTLmhlbHBlci5lcXVhbEhlaWdodCggX2RlZmF1bHRzLnNpZGViYXJTZWxlY3RvciwgX2RlZmF1bHRzLmNvbnRlbnRTZWxlY3RvciApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2UgaWYgKCBhamF4bG9hZGVyICkge1xuICAgICAgICAgICAgICAgICAgICBzd2l0Y2ggKCBhamF4c3RhdHVzICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgY2FzZSBcImJlZ2luXCI6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgYWpheGxvYWRlci5zdHlsZS5kaXNwbGF5ID0gJ2Jsb2NrJztcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgY2FzZSBcImNvbXBsZXRlXCI6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgYWpheGxvYWRlci5zdHlsZS5kaXNwbGF5ID0gJ25vbmUnO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICBjYXNlIFwic3VjY2Vzc1wiOlxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIGVuYWJsZSBCUyB0b29sdGlwc1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICQoICdbZGF0YS10b2dnbGU9XCJ0b29sdGlwXCJdJyApLnRvb2x0aXAoKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyBzZXQgY29udGVudCBoZWlnaHQgdG8gc2lkZWJhciBoZWlnaHRcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBpZiAoIHdpbmRvdy5tYXRjaE1lZGlhKCAnKG1heC13aWR0aDogNzY4cHgpJyApLm1hdGNoZXMgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZpZXdlckpTLmhlbHBlci5lcXVhbEhlaWdodCggX2RlZmF1bHRzLnNpZGViYXJTZWxlY3RvciwgX2RlZmF1bHRzLmNvbnRlbnRTZWxlY3RvciApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgJCggd2luZG93ICkub2ZmKCkub24oIFwib3JpZW50YXRpb25jaGFuZ2VcIiwgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB2aWV3ZXJKUy5oZWxwZXIuZXF1YWxIZWlnaHQoIF9kZWZhdWx0cy5zaWRlYmFyU2VsZWN0b3IsIF9kZWZhdWx0cy5jb250ZW50U2VsZWN0b3IgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgc3dpdGNoICggYWpheHN0YXR1cyApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGNhc2UgXCJzdWNjZXNzXCI6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8gZW5hYmxlIEJTIHRvb2x0aXBzXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgJCggJ1tkYXRhLXRvZ2dsZT1cInRvb2x0aXBcIl0nICkudG9vbHRpcCgpO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIHNldCBjb250ZW50IGhlaWdodCB0byBzaWRlYmFyIGhlaWdodFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGlmICggd2luZG93Lm1hdGNoTWVkaWEoICcobWF4LXdpZHRoOiA3NjhweCknICkubWF0Y2hlcyApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdmlld2VySlMuaGVscGVyLmVxdWFsSGVpZ2h0KCBfZGVmYXVsdHMuc2lkZWJhclNlbGVjdG9yLCBfZGVmYXVsdHMuY29udGVudFNlbGVjdG9yICk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCB3aW5kb3cgKS5vZmYoKS5vbiggXCJvcmllbnRhdGlvbmNoYW5nZVwiLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZpZXdlckpTLmhlbHBlci5lcXVhbEhlaWdodCggX2RlZmF1bHRzLnNpZGViYXJTZWxlY3RvciwgX2RlZmF1bHRzLmNvbnRlbnRTZWxlY3RvciApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfSApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICAvLyBkaXNhYmxlIHN1Ym1pdCBidXR0b24gb24gZmVlZGJhY2tcbiAgICAgICAgaWYgKCBjdXJyZW50UGFnZSA9PT0gJ2ZlZWRiYWNrJyApIHtcbiAgICAgICAgICAgICQoICcjc3VibWl0RmVlZGJhY2tCdG4nICkuYXR0ciggJ2Rpc2FibGVkJywgdHJ1ZSApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICAvLyBzZXQgc2lkZWJhciBwb3NpdGlvbiBmb3IgTkVSLVdpZGdldFxuICAgICAgICBpZiAoICQoICcjd2lkZ2V0TmVyRmFjZXR0aW5nJyApLmxlbmd0aCA+IDAgKSB7XG4gICAgICAgICAgICBuZXJGYWNldHRpbmdDb25maWcuc2lkZWJhclJpZ2h0ID0gX2RlZmF1bHRzLndpZGdldE5lclNpZGViYXJSaWdodDtcbiAgICAgICAgICAgIHRoaXMubmVyRmFjZXR0aW5nLmluaXQoIG5lckZhY2V0dGluZ0NvbmZpZyApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICAvLyBtYWtlIHN1cmUgb25seSBpbnRlZ2VyIHZhbHVlcyBtYXkgYmUgZW50ZXJlZCBpbiBpbnB1dCBmaWVsZHMgb2YgY2xhc3NcbiAgICAgICAgLy8gJ2lucHV0LWludGVnZXInXG4gICAgICAgICQoICcuaW5wdXQtaW50ZWdlcicgKS5vbiggXCJrZXlwcmVzc1wiLCBmdW5jdGlvbiggZXZlbnQgKSB7XG4gICAgICAgICAgICBpZiAoIGV2ZW50LndoaWNoIDwgNDggfHwgZXZlbnQud2hpY2ggPiA1NyApIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gZmFsc2U7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0gKTtcbiAgICAgICAgXG4gICAgICAgIC8vIG1ha2Ugc3VyZSBvbmx5IGludGVnZXIgdmFsdWVzIG1heSBiZSBlbnRlcmVkIGluIGlucHV0IGZpZWxkcyBvZiBjbGFzc1xuICAgICAgICAvLyAnaW5wdXQtaW50ZWdlcidcbiAgICAgICAgJCggJy5pbnB1dC1mbG9hdCcgKS5vbiggXCJrZXlwcmVzc1wiLCBmdW5jdGlvbiggZXZlbnQgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggZXZlbnQgKTtcbiAgICAgICAgICAgIHN3aXRjaCAoIGV2ZW50LndoaWNoICkge1xuICAgICAgICAgICAgICAgIGNhc2UgODogLy8gZGVsZXRlXG4gICAgICAgICAgICAgICAgY2FzZSA5OiAvLyB0YWJcbiAgICAgICAgICAgICAgICBjYXNlIDEzOiAvLyBlbnRlclxuICAgICAgICAgICAgICAgIGNhc2UgNDY6IC8vIGRvdFxuICAgICAgICAgICAgICAgIGNhc2UgNDQ6IC8vIGNvbW1hXG4gICAgICAgICAgICAgICAgY2FzZSA0MzogLy8gcGx1c1xuICAgICAgICAgICAgICAgIGNhc2UgNDU6IC8vIG1pbnVzXG4gICAgICAgICAgICAgICAgICAgIHJldHVybiB0cnVlO1xuICAgICAgICAgICAgICAgIGNhc2UgMTE4OlxuICAgICAgICAgICAgICAgICAgICByZXR1cm4gZXZlbnQuY3RybEtleTsgLy8gY29weVxuICAgICAgICAgICAgICAgIGRlZmF1bHQ6XG4gICAgICAgICAgICAgICAgICAgIHN3aXRjaCAoIGV2ZW50LmtleUNvZGUgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBjYXNlIDg6IC8vIGRlbGV0ZVxuICAgICAgICAgICAgICAgICAgICAgICAgY2FzZSA5OiAvLyB0YWJcbiAgICAgICAgICAgICAgICAgICAgICAgIGNhc2UgMTM6IC8vIGVudGVyXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuIHRydWU7XG4gICAgICAgICAgICAgICAgICAgICAgICBkZWZhdWx0OlxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGlmICggZXZlbnQud2hpY2ggPCA0OCB8fCBldmVudC53aGljaCA+IDU3ICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICByZXR1cm4gZmFsc2U7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICByZXR1cm4gdHJ1ZTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgfSApO1xuICAgICAgICBcbiAgICAgICAgLy8gc2V0IHRpbnltY2UgbGFuZ3VhZ2VcbiAgICAgICAgdGhpcy50aW55Q29uZmlnLmxhbmd1YWdlID0gY3VycmVudExhbmc7XG4gICAgICAgIFxuICAgICAgICBpZiAoIGN1cnJlbnRQYWdlID09PSAnYWRtaW5DbXNDcmVhdGVQYWdlJyApIHtcbiAgICAgICAgICAgIHRoaXMudGlueUNvbmZpZy5zZXR1cCA9IGZ1bmN0aW9uKCBlZCApIHtcbiAgICAgICAgICAgICAgICAvLyBsaXN0ZW4gdG8gY2hhbmdlcyBvbiB0aW55bWNlIGlucHV0IGZpZWxkc1xuICAgICAgICAgICAgICAgIGVkLm9uKCAnY2hhbmdlIGlucHV0IHBhc3RlJywgZnVuY3Rpb24oIGUgKSB7XG4gICAgICAgICAgICAgICAgICAgIHRpbnltY2UudHJpZ2dlclNhdmUoKTtcbiAgICAgICAgICAgICAgICAgICAgY3JlYXRlUGFnZUNvbmZpZy5wcmV2QnRuLmF0dHIoICdkaXNhYmxlZCcsIHRydWUgKTtcbiAgICAgICAgICAgICAgICAgICAgY3JlYXRlUGFnZUNvbmZpZy5wcmV2RGVzY3JpcHRpb24uc2hvdygpO1xuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIH07XG4gICAgICAgICAgICAvLyBUT0RPOiBGw7xyIFpMQiBpbiBkZXIgY3VzdG9tLmpzIGVpbmJhdWVuXG4gICAgICAgICAgICAvLyB2aWV3ZXJKUy50aW55Q29uZmlnLnRleHRjb2xvcl9tYXAgPSBbIFwiRkZGRkZGXCIsIFwiWkxCLVdlacOfXCIsIFwiMzMzMzMzXCIsXG4gICAgICAgICAgICAvLyBcIlpMQi1TY2h3YXJ6XCIsIFwiZGVkZWRlXCIsIFwiWkxCLUhlbGxncmF1XCIsIFwiNzI3Yzg3XCIsIFwiWkxCLU1pdHRlbGdyYXVcIixcbiAgICAgICAgICAgIC8vIFwiOWE5YTlhXCIsIFwiWkxCLUR1bmtlbGdyYXVcIixcbiAgICAgICAgICAgIC8vIFwiQ0QwMDAwXCIsIFwiWkxCLVJvdFwiLCBcIjkyNDA2ZFwiLCBcIlpMQi1MaWxhXCIsIFwiNmYyYzQwXCIsIFwiWkxCLUJvcmRlYXV4XCIsXG4gICAgICAgICAgICAvLyBcImZmYTEwMFwiLCBcIlpMQi1PcmFuZ2VcIiwgXCI2Njk5MzNcIiwgXCJaTEItR3LDvG5cIiwgXCIzZTVkMWVcIiwgXCJaTEItRHVua2VsZ3LDvG5cIixcbiAgICAgICAgICAgIC8vIFwiYTlkMGY1XCIsXG4gICAgICAgICAgICAvLyBcIlpMQi1IZWxsYmxhdVwiLCBcIjI4Nzc5ZlwiLCBcIlpMQi1CbGF1XCIgXTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgaWYgKCBjdXJyZW50UGFnZSA9PT0gJ292ZXJ2aWV3JyApIHtcbiAgICAgICAgICAgIC8vIGFjdGl2YXRlIG1lbnViYXJcbiAgICAgICAgICAgIHZpZXdlckpTLnRpbnlDb25maWcubWVudWJhciA9IHRydWU7XG4gICAgICAgICAgICB2aWV3ZXJKUy50aW55TWNlLm92ZXJ2aWV3KCk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIC8vIEFKQVggTG9hZGVyIEV2ZW50bGlzdGVuZXIgZm9yIHRpbnlNQ0VcbiAgICAgICAgaWYgKCB0eXBlb2YganNmICE9PSAndW5kZWZpbmVkJyApIHtcbiAgICAgICAgICAgIGpzZi5hamF4LmFkZE9uRXZlbnQoIGZ1bmN0aW9uKCBkYXRhICkge1xuICAgICAgICAgICAgICAgIHZhciBhamF4c3RhdHVzID0gZGF0YS5zdGF0dXM7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgc3dpdGNoICggYWpheHN0YXR1cyApIHtcbiAgICAgICAgICAgICAgICAgICAgY2FzZSBcInN1Y2Nlc3NcIjpcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICggY3VycmVudFBhZ2UgPT09ICdvdmVydmlldycgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgdmlld2VySlMudGlueU1jZS5vdmVydmlldygpO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICB2aWV3ZXJKUy50aW55TWNlLmluaXQoIHZpZXdlckpTLnRpbnlDb25maWcgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgLy8gaW5pdCB0aW55bWNlIGlmIGl0IGV4aXN0c1xuICAgICAgICBpZiAoICQoICcudGlueU1DRScgKS5sZW5ndGggPiAwICkge1xuICAgICAgICAgICAgdmlld2VySlMudGlueU1jZS5pbml0KCB0aGlzLnRpbnlDb25maWcgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgLy8gaGFuZGxlIGJyb3dzZXIgYnVnc1xuICAgICAgICBzd2l0Y2ggKCBfZGVmYXVsdHMuYnJvd3NlciApIHtcbiAgICAgICAgICAgIGNhc2UgJ0Nocm9tZSc6XG4gICAgICAgICAgICAgICAgLyogQlJPS0VOIElNQUdFUyAqL1xuICAgICAgICAgICAgICAgICQoICdpbWcnICkuZXJyb3IoIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICAkKCB0aGlzICkuYWRkQ2xhc3MoICdicm9rZW4nICk7XG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgY2FzZSAnRmlyZWZveCc6XG4gICAgICAgICAgICAgICAgLyogQlJPS0VOIElNQUdFUyAqL1xuICAgICAgICAgICAgICAgICQoIFwiaW1nXCIgKS5lcnJvciggZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICQoIHRoaXMgKS5oaWRlKCk7XG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgIC8qIDFweCBCVUcgKi9cbiAgICAgICAgICAgICAgICBpZiAoICQoICcuaW1hZ2UtZG91YmxlUGFnZVZpZXcnICkubGVuZ3RoID4gMCApIHtcbiAgICAgICAgICAgICAgICAgICAgJCggJy5pbWFnZS1kb3VibGVQYWdlVmlldycgKS5hZGRDbGFzcyggJ29uZVVwJyApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgIGNhc2UgJ0lFJzpcbiAgICAgICAgICAgICAgICAvKiBTRVQgSUUgQ0xBU1MgVE8gSFRNTCAqL1xuICAgICAgICAgICAgICAgICQoICdodG1sJyApLmFkZENsYXNzKCAnaXMtSUUnICk7XG4gICAgICAgICAgICAgICAgLyogQlJPS0VOIElNQUdFUyAqL1xuICAgICAgICAgICAgICAgICQoIFwiaW1nXCIgKS5lcnJvciggZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICQoIHRoaXMgKS5oaWRlKCk7XG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgY2FzZSAnRWRnZSc6XG4gICAgICAgICAgICAgICAgLyogQlJPS0VOIElNQUdFUyAqL1xuICAgICAgICAgICAgICAgICQoIFwiaW1nXCIgKS5lcnJvciggZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICQoIHRoaXMgKS5oaWRlKCk7XG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgY2FzZSAnU2FmYXJpJzpcbiAgICAgICAgICAgICAgICAvKiBCUk9LRU4gSU1BR0VTICovXG4gICAgICAgICAgICAgICAgJCggXCJpbWdcIiApLmVycm9yKCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgJCggdGhpcyApLmhpZGUoKTtcbiAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgIH1cbiAgICB9O1xuICAgIFxuICAgIC8vIGdsb2JhbCBvYmplY3QgZm9yIHRpbnltY2UgY29uZmlnXG4gICAgdmlld2VyLnRpbnlDb25maWcgPSB7fTtcbiAgICBcbiAgICByZXR1cm4gdmlld2VyO1xuICAgIFxufSApKCBqUXVlcnkgKTtcbiIsIi8qKlxuICogVGhpcyBmaWxlIGlzIHBhcnQgb2YgdGhlIEdvb2JpIHZpZXdlciAtIGEgY29udGVudCBwcmVzZW50YXRpb24gYW5kIG1hbmFnZW1lbnRcbiAqIGFwcGxpY2F0aW9uIGZvciBkaWdpdGl6ZWQgb2JqZWN0cy5cbiAqIFxuICogVmlzaXQgdGhlc2Ugd2Vic2l0ZXMgZm9yIG1vcmUgaW5mb3JtYXRpb24uIC0gaHR0cDovL3d3dy5pbnRyYW5kYS5jb20gLVxuICogaHR0cDovL2RpZ2l2ZXJzby5jb21cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGZyZWUgc29mdHdhcmU7IHlvdSBjYW4gcmVkaXN0cmlidXRlIGl0IGFuZC9vciBtb2RpZnkgaXQgdW5kZXIgdGhlIHRlcm1zXG4gKiBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYXMgcHVibGlzaGVkIGJ5IHRoZSBGcmVlIFNvZnR3YXJlIEZvdW5kYXRpb247IGVpdGhlclxuICogdmVyc2lvbiAyIG9mIHRoZSBMaWNlbnNlLCBvciAoYXQgeW91ciBvcHRpb24pIGFueSBsYXRlciB2ZXJzaW9uLlxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZGlzdHJpYnV0ZWQgaW4gdGhlIGhvcGUgdGhhdCBpdCB3aWxsIGJlIHVzZWZ1bCwgYnV0IFdJVEhPVVQgQU5ZXG4gKiBXQVJSQU5UWTsgd2l0aG91dCBldmVuIHRoZSBpbXBsaWVkIHdhcnJhbnR5IG9mIE1FUkNIQU5UQUJJTElUWSBvciBGSVRORVNTIEZPUiBBXG4gKiBQQVJUSUNVTEFSIFBVUlBPU0UuIFNlZSB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgZm9yIG1vcmUgZGV0YWlscy5cbiAqIFxuICogWW91IHNob3VsZCBoYXZlIHJlY2VpdmVkIGEgY29weSBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYWxvbmcgd2l0aCB0aGlzXG4gKiBwcm9ncmFtLiBJZiBub3QsIHNlZSA8aHR0cDovL3d3dy5nbnUub3JnL2xpY2Vuc2VzLz4uXG4gKiBcbiAqIFRoaXMgbW9kdWxlIGRyaXZlcyB0aGUgZnVuY3Rpb25hbGl0eSBvZiB0aGUgbWFpbiBuYXZpZ2F0aW9uIGZyb20gdGhlIEdvb2JpIHZpZXdlci5cbiAqIFxuICogQHZlcnNpb24gMy4yLjBcbiAqIEBtb2R1bGUgdmlld2VySlMubmF2aWdhdGlvblxuICogQHJlcXVpcmVzIGpRdWVyeVxuICovXG52YXIgdmlld2VySlMgPSAoIGZ1bmN0aW9uKCB2aWV3ZXIgKSB7XG4gICAgJ3VzZSBzdHJpY3QnO1xuICAgIFxuICAgIHZhciBfZGVidWcgPSBmYWxzZTtcbiAgICB2YXIgX2RlZmF1bHRzID0ge1xuICAgICAgICBuYXZpZ2F0aW9uU2VsZWN0b3I6ICcjbmF2aWdhdGlvbicsXG4gICAgICAgIHN1Yk1lbnVTZWxlY3RvcjogJ1tkYXRhLXRvZ2dsZT1cInN1Ym1lbnVcIl0nLFxuICAgICAgICBtZWdhTWVudVNlbGVjdG9yOiAnW2RhdGEtdG9nZ2xlPVwibWVnYW1lbnVcIl0nLFxuICAgICAgICBjbG9zZU1lZ2FNZW51U2VsZWN0b3I6ICdbZGF0YS10b2dnbGU9XCJjbG9zZVwiXScsXG4gICAgfTtcbiAgICBcbiAgICB2aWV3ZXIubmF2aWdhdGlvbiA9IHtcbiAgICAgICAgLyoqXG4gICAgICAgICAqIE1ldGhvZCB0byBpbml0aWFsaXplIHRoZSB2aWV3ZXIgbWFpbiBuYXZpZ2F0aW9uLlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCBpbml0XG4gICAgICAgICAqIEBwYXJhbSB7T2JqZWN0fSBjb25maWcgQW4gY29uZmlnIG9iamVjdCB3aGljaCBvdmVyd3JpdGVzIHRoZSBkZWZhdWx0cy5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5uYXZpZ2F0aW9uU2VsZWN0b3IgVGhlIHNlbGVjdG9yIGZvciB0aGUgbmF2aWdhdGlvblxuICAgICAgICAgKiBlbGVtZW50LlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLnN1Yk1lbnVTZWxlY3RvciBUaGUgc2VsZWN0b3IgZm9yIHRoZSBzdWJtZW51IGVsZW1lbnQuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcubWVnYU1lbnVTZWxlY3RvciBUaGUgc2VsZWN0b3IgZm9yIHRoZSBtZWdhIG1lbnUgZWxlbWVudC5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5jbG9zZU1lZ2FNZW51U2VsZWN0b3IgVGhlIHNlbGVjdG9yIGZvciB0aGUgY2xvc2UgbWVnYVxuICAgICAgICAgKiBtZW51IGVsZW1lbnQuXG4gICAgICAgICAqL1xuICAgICAgICBpbml0OiBmdW5jdGlvbiggY29uZmlnICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICcjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIubmF2aWdhdGlvbi5pbml0JyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLm5hdmlnYXRpb24uaW5pdDogY29uZmlnIC0gJywgY29uZmlnICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgICQuZXh0ZW5kKCB0cnVlLCBfZGVmYXVsdHMsIGNvbmZpZyApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICAvLyBUUklHR0VSIFNUQU5EQVJEIE1FTlVcbiAgICAgICAgICAgICQoIF9kZWZhdWx0cy5zdWJNZW51U2VsZWN0b3IgKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgdmFyIGN1cnJUcmlnZ2VyID0gJCggdGhpcyApO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIGlmICggJCggdGhpcyApLnBhcmVudHMoICcubmF2aWdhdGlvbl9fc3VibWVudScgKS5oYXNDbGFzcyggJ2luJyApICkge1xuICAgICAgICAgICAgICAgICAgICBfcmVzZXRTdWJNZW51cygpO1xuICAgICAgICAgICAgICAgICAgICBjdXJyVHJpZ2dlci5wYXJlbnQoKS5hZGRDbGFzcyggJ2FjdGl2ZScgKTtcbiAgICAgICAgICAgICAgICAgICAgY3VyclRyaWdnZXIubmV4dCggJy5uYXZpZ2F0aW9uX19zdWJtZW51JyApLmFkZENsYXNzKCAnaW4nICk7XG4gICAgICAgICAgICAgICAgICAgIF9jYWxjU3ViTWVudVBvc2l0aW9uKCBjdXJyVHJpZ2dlci5uZXh0KCAnLm5hdmlnYXRpb25fX3N1Ym1lbnUnICkgKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIF9yZXNldE1lbnVzKCk7XG4gICAgICAgICAgICAgICAgICAgIGN1cnJUcmlnZ2VyLnBhcmVudCgpLmFkZENsYXNzKCAnYWN0aXZlJyApO1xuICAgICAgICAgICAgICAgICAgICBjdXJyVHJpZ2dlci5uZXh0KCAnLm5hdmlnYXRpb25fX3N1Ym1lbnUnICkuYWRkQ2xhc3MoICdpbicgKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIFRSSUdHRVIgTUVHQSBNRU5VXG4gICAgICAgICAgICAkKCBfZGVmYXVsdHMubWVnYU1lbnVTZWxlY3RvciApLm9uKCAnY2xpY2snLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICBfcmVzZXRNZW51cygpO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIGlmICggJCggdGhpcyApLm5leHQoICcubmF2aWdhdGlvbl9fbWVnYW1lbnUtd3JhcHBlcicgKS5oYXNDbGFzcyggJ2luJyApICkge1xuICAgICAgICAgICAgICAgICAgICAkKCB0aGlzICkucGFyZW50KCkucmVtb3ZlQ2xhc3MoICdhY3RpdmUnICk7XG4gICAgICAgICAgICAgICAgICAgICQoIHRoaXMgKS5uZXh0KCAnLm5hdmlnYXRpb25fX21lZ2FtZW51LXdyYXBwZXInICkucmVtb3ZlQ2xhc3MoICdpbicgKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICQoICcubmF2aWdhdGlvbl9fbWVnYW1lbnUtdHJpZ2dlcicgKS5yZW1vdmVDbGFzcyggJ2FjdGl2ZScgKTtcbiAgICAgICAgICAgICAgICAgICAgJCggJy5uYXZpZ2F0aW9uX19tZWdhbWVudS13cmFwcGVyJyApLnJlbW92ZUNsYXNzKCAnaW4nICk7XG4gICAgICAgICAgICAgICAgICAgICQoIHRoaXMgKS5wYXJlbnQoKS5hZGRDbGFzcyggJ2FjdGl2ZScgKTtcbiAgICAgICAgICAgICAgICAgICAgJCggdGhpcyApLm5leHQoICcubmF2aWdhdGlvbl9fbWVnYW1lbnUtd3JhcHBlcicgKS5hZGRDbGFzcyggJ2luJyApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgJCggX2RlZmF1bHRzLmNsb3NlTWVnYU1lbnVTZWxlY3RvciApLm9uKCAnY2xpY2snLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICBfcmVzZXRNZW51cygpO1xuICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBpZiAoICQoICcubmF2aWdhdGlvbl9fbWVnYW1lbnUtd3JhcHBlcicgKS5sZW5ndGggPiAwICkge1xuICAgICAgICAgICAgICAgIF9yZXNldE1lbnVzKCk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIHJlc2V0IGFsbCBtZW51cyBieSBjbGlja2luZyBvbiBib2R5XG4gICAgICAgICAgICAkKCAnYm9keScgKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oIGV2ZW50ICkge1xuICAgICAgICAgICAgICAgIGlmICggZXZlbnQudGFyZ2V0LmlkID09ICduYXZpZ2F0aW9uJyB8fCAkKCBldmVudC50YXJnZXQgKS5jbG9zZXN0KCBfZGVmYXVsdHMubmF2aWdhdGlvblNlbGVjdG9yICkubGVuZ3RoICkge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICBfcmVzZXRNZW51cygpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgfSxcbiAgICB9O1xuICAgIFxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB0byByZXNldCBhbGwgc2hvd24gbWVudXMuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfcmVzZXRNZW51c1xuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9yZXNldE1lbnVzKCkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfcmVzZXRNZW51cygpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgICQoICcubmF2aWdhdGlvbl9fc3VibWVudS10cmlnZ2VyJyApLnJlbW92ZUNsYXNzKCAnYWN0aXZlJyApO1xuICAgICAgICAkKCAnLm5hdmlnYXRpb25fX3N1Ym1lbnUnICkucmVtb3ZlQ2xhc3MoICdpbicgKTtcbiAgICAgICAgJCggJy5uYXZpZ2F0aW9uX19tZWdhbWVudS10cmlnZ2VyJyApLnJlbW92ZUNsYXNzKCAnYWN0aXZlJyApO1xuICAgICAgICAkKCAnLm5hdmlnYXRpb25fX21lZ2FtZW51LXdyYXBwZXInICkucmVtb3ZlQ2xhc3MoICdpbicgKTtcbiAgICB9XG4gICAgXG4gICAgLyoqXG4gICAgICogTWV0aG9kIHRvIHJlc2V0IGFsbCBzaG93biBzdWJtZW51cy5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9yZXNldFN1Yk1lbnVzXG4gICAgICovXG4gICAgZnVuY3Rpb24gX3Jlc2V0U3ViTWVudXMoKSB7XG4gICAgICAgICQoICcubGV2ZWwtMiwgLmxldmVsLTMsIC5sZXZlbC00LCAubGV2ZWwtNScgKS5wYXJlbnQoKS5yZW1vdmVDbGFzcyggJ2FjdGl2ZScgKTtcbiAgICAgICAgJCggJy5sZXZlbC0yLCAubGV2ZWwtMywgLmxldmVsLTQsIC5sZXZlbC01JyApLnJlbW92ZUNsYXNzKCAnaW4nICkucmVtb3ZlQ2xhc3MoICdsZWZ0JyApO1xuICAgIH1cbiAgICBcbiAgICAvKipcbiAgICAgKiBNZXRob2QgdG8gY2FsY3VsYXRlIHRoZSBwb3NpdGlvbiBvZiB0aGUgc2hvd24gc3VibWVudS5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9jYWxjU3ViTWVudVBvc2l0aW9uXG4gICAgICogQHBhcmFtIHtPYmplY3R9IG1lbnUgQW4galF1ZXJ5IG9iamVjdCBvZiB0aGUgY3VycmVudCBzdWJtZW51LlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9jYWxjU3ViTWVudVBvc2l0aW9uKCBtZW51ICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfY2xhY1N1Yk1lbnVQb3NpdGlvbigpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19jbGFjU3ViTWVudVBvc2l0aW9uOiBtZW51IC0gJywgbWVudSApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgY3VycmVudE9mZnNldExlZnQgPSBtZW51Lm9mZnNldCgpLmxlZnQ7XG4gICAgICAgIHZhciBtZW51V2lkdGggPSBtZW51Lm91dGVyV2lkdGgoKTtcbiAgICAgICAgdmFyIHdpbmRvd1dpZHRoID0gJCggd2luZG93ICkub3V0ZXJXaWR0aCgpO1xuICAgICAgICB2YXIgb2Zmc2V0V2lkdGggPSBjdXJyZW50T2Zmc2V0TGVmdCArIG1lbnVXaWR0aDtcbiAgICAgICAgXG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfY2xhY1N1Yk1lbnVQb3NpdGlvbjogY3VycmVudE9mZnNldExlZnQgLSAnLCBjdXJyZW50T2Zmc2V0TGVmdCApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfY2xhY1N1Yk1lbnVQb3NpdGlvbjogbWVudVdpZHRoIC0gJywgbWVudVdpZHRoICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19jbGFjU3ViTWVudVBvc2l0aW9uOiB3aW5kb3dXaWR0aCAtICcsIHdpbmRvd1dpZHRoICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19jbGFjU3ViTWVudVBvc2l0aW9uOiBvZmZzZXRXaWR0aCAtICcsIG9mZnNldFdpZHRoICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIGlmICggb2Zmc2V0V2lkdGggPj0gd2luZG93V2lkdGggKSB7XG4gICAgICAgICAgICBtZW51LmFkZENsYXNzKCAnbGVmdCcgKS5jc3MoICd3aWR0aCcsIG1lbnVXaWR0aCApO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgcmV0dXJuIGZhbHNlO1xuICAgICAgICB9XG4gICAgfVxuICAgIFxuICAgIHJldHVybiB2aWV3ZXI7XG4gICAgXG59ICkoIHZpZXdlckpTIHx8IHt9LCBqUXVlcnkgKTtcbiIsIi8qKlxuICogVGhpcyBmaWxlIGlzIHBhcnQgb2YgdGhlIEdvb2JpIHZpZXdlciAtIGEgY29udGVudCBwcmVzZW50YXRpb24gYW5kIG1hbmFnZW1lbnRcbiAqIGFwcGxpY2F0aW9uIGZvciBkaWdpdGl6ZWQgb2JqZWN0cy5cbiAqIFxuICogVmlzaXQgdGhlc2Ugd2Vic2l0ZXMgZm9yIG1vcmUgaW5mb3JtYXRpb24uIC0gaHR0cDovL3d3dy5pbnRyYW5kYS5jb20gLVxuICogaHR0cDovL2RpZ2l2ZXJzby5jb21cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGZyZWUgc29mdHdhcmU7IHlvdSBjYW4gcmVkaXN0cmlidXRlIGl0IGFuZC9vciBtb2RpZnkgaXQgdW5kZXIgdGhlIHRlcm1zXG4gKiBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYXMgcHVibGlzaGVkIGJ5IHRoZSBGcmVlIFNvZnR3YXJlIEZvdW5kYXRpb247IGVpdGhlclxuICogdmVyc2lvbiAyIG9mIHRoZSBMaWNlbnNlLCBvciAoYXQgeW91ciBvcHRpb24pIGFueSBsYXRlciB2ZXJzaW9uLlxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZGlzdHJpYnV0ZWQgaW4gdGhlIGhvcGUgdGhhdCBpdCB3aWxsIGJlIHVzZWZ1bCwgYnV0IFdJVEhPVVQgQU5ZXG4gKiBXQVJSQU5UWTsgd2l0aG91dCBldmVuIHRoZSBpbXBsaWVkIHdhcnJhbnR5IG9mIE1FUkNIQU5UQUJJTElUWSBvciBGSVRORVNTIEZPUiBBXG4gKiBQQVJUSUNVTEFSIFBVUlBPU0UuIFNlZSB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgZm9yIG1vcmUgZGV0YWlscy5cbiAqIFxuICogWW91IHNob3VsZCBoYXZlIHJlY2VpdmVkIGEgY29weSBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYWxvbmcgd2l0aCB0aGlzXG4gKiBwcm9ncmFtLiBJZiBub3QsIHNlZSA8aHR0cDovL3d3dy5nbnUub3JnL2xpY2Vuc2VzLz4uXG4gKiBcbiAqIE1vZHVsZSB3aGljaCBmYWNldHRzIHRoZSBORVItVGFncyBpbiBhIHNpZGViYXIgd2lkZ2V0IGFuZCBQYWdldmlldy5cbiAqIFxuICogQHZlcnNpb24gMy4yLjBcbiAqIEBtb2R1bGUgdmlld2VySlMubmVyRmFjZXR0aW5nXG4gKiBAcmVxdWlyZXMgalF1ZXJ5XG4gKiBAcmVxdWlyZXMgQm9vdHN0cmFwXG4gKi9cbnZhciB2aWV3ZXJKUyA9ICggZnVuY3Rpb24oIHZpZXdlciApIHtcbiAgICAndXNlIHN0cmljdCc7XG4gICAgXG4gICAgdmFyIF9kZWJ1ZyA9IGZhbHNlO1xuICAgIHZhciBfanNvbiA9IG51bGw7XG4gICAgdmFyIF9hcGlDYWxsID0gJyc7XG4gICAgdmFyIF9odG1sID0gJyc7XG4gICAgdmFyIF9wYWdlQ291bnQgPSAwO1xuICAgIHZhciBfc2NhbGVDb3VudCA9IDA7XG4gICAgdmFyIF9zY2FsZUhlaWdodCA9IDA7XG4gICAgdmFyIF9zbGlkZXJIYW5kbGVQb3NpdGlvbiA9IHt9O1xuICAgIHZhciBfbW92ZWRTbGlkZXJIYW5kbGVQb3NpdGlvbiA9IDA7XG4gICAgdmFyIF9yZWN1cnJlbmNlQ291bnQgPSAwO1xuICAgIHZhciBfc2NhbGVWYWx1ZSA9IDA7XG4gICAgdmFyIF9zbGlkZXJTY2FsZUhlaWdodCA9IDA7XG4gICAgdmFyIF9zdGFydCA9IDA7XG4gICAgdmFyIF9lbmQgPSAwO1xuICAgIHZhciBfY3VycmVudE5lclBhZ2VSYW5nZVNlbGVjdGVkID0gJyc7XG4gICAgdmFyIF9jdXJyZW50TmVyUGFnZVJhbmdlID0gJyc7XG4gICAgdmFyIF9jdXJyZW50TmVyVHlwZSA9ICcnO1xuICAgIHZhciBfcHJvbWlzZSA9IG51bGw7XG4gICAgdmFyIF9kZWZhdWx0cyA9IHtcbiAgICAgICAgY3VycmVudFBhZ2U6ICcnLFxuICAgICAgICBiYXNlVXJsOiAnJyxcbiAgICAgICAgYXBpVXJsOiAnL3Jlc3QvbmVyL3RhZ3MvJyxcbiAgICAgICAgd29ya0lkOiAnJyxcbiAgICAgICAgb3ZlcnZpZXdUcmlnZ2VyOiAnJyxcbiAgICAgICAgb3ZlcnZpZXdDb250ZW50OiAnJyxcbiAgICAgICAgc2VjdGlvblRyaWdnZXI6ICcnLFxuICAgICAgICBzZWN0aW9uQ29udGVudDogJycsXG4gICAgICAgIGZhY2V0dGluZ1RyaWdnZXI6ICcnLFxuICAgICAgICBzZXRUYWdSYW5nZTogJycsXG4gICAgICAgIHNsaWRlcjogJycsXG4gICAgICAgIHNsaWRlclNjYWxlOiAnJyxcbiAgICAgICAgc2VjdGlvblRhZ3M6ICcnLFxuICAgICAgICBjdXJyZW50VGFnczogJycsXG4gICAgICAgIHNsaWRlckhhbmRsZTogJycsXG4gICAgICAgIHNsaWRlclNlY3Rpb25TdHJpcGU6ICcnLFxuICAgICAgICByZWN1cnJlbmNlTnVtYmVyOiAwLFxuICAgICAgICByZWN1cnJlbmNlU2VjdGlvbk51bWJlcjogMCxcbiAgICAgICAgc2lkZWJhclJpZ2h0OiBmYWxzZSxcbiAgICAgICAgbG9hZGVyOiAnJyxcbiAgICAgICAgbXNnOiB7XG4gICAgICAgICAgICBub0pTT046ICdFcyBrb25udGVuIGtlaW5lIERhdGVuIGFiZ2VydWZlbiB3ZXJkZW4uJyxcbiAgICAgICAgICAgIGVtcHR5VGFnOiAnS2VpbmUgVGFncyB2b3JoYW5kZW4nLFxuICAgICAgICAgICAgcGFnZTogJ1NlaXRlJyxcbiAgICAgICAgICAgIHRhZ3M6ICdUYWdzJyxcbiAgICAgICAgfVxuICAgIH07XG4gICAgXG4gICAgdmlld2VyLm5lckZhY2V0dGluZyA9IHtcbiAgICAgICAgLyoqXG4gICAgICAgICAqIE1ldGhvZCB0byBpbml0aWFsaXplIHRoZSBORVItV2lkZ2V0IG9yIE5FUi1WaWV3LlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCBpbml0XG4gICAgICAgICAqIEBwYXJhbSB7T2JqZWN0fSBjb25maWcgQW4gY29uZmlnIG9iamVjdCB3aGljaCBvdmVyd3JpdGVzIHRoZSBkZWZhdWx0cy5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5jdXJyZW50UGFnZSBUaGUgbmFtZSBvZiB0aGUgY3VycmVudCBwYWdlLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLmJhc2VVcmwgVGhlIHJvb3QgVVJMLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLmFwaVVybCBUaGUgYmFzZSBVUkwgZm9yIHRoZSBBUEktQ2FsbHMuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcud29ya0lkIFRoZSBJRCBvZiB0aGUgY3VycmVudCB3b3JrLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLm92ZXJ2aWV3VHJpZ2dlciBUaGUgSUQvQ2xhc3Mgb2YgdGhlIG92ZXJ2aWV3IHRyaWdnZXIuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcub3ZlcnZpZXdDb250ZW50IFRoZSBJRC9DbGFzcyBvZiB0aGUgY29udGVudCBzZWN0aW9uIGZyb21cbiAgICAgICAgICogb3ZlcnZpZXcuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcuc2VjdGlvblRyaWdnZXIgVGhlIElEL0NsYXNzIG9mIHRoZSBzZWN0aW9uIHRyaWdnZXIuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcuc2VjdGlvbkNvbnRlbnQgVGhlIElEL0NsYXNzIG9mIHRoZSBjb250ZW50IHNlY3Rpb24gZnJvbVxuICAgICAgICAgKiBzZWN0aW9uLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLmZhY2V0dGluZ1RyaWdnZXIgVGhlIElEL0NsYXNzIG9mIHRoZSBmYWNldHRpbmcgdHJpZ2dlci5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5zZXRUYWdSYW5nZSBUaGUgSUQvQ2xhc3Mgb2YgdGhlIHNlbGVjdCBtZW51IGZvciB0aGVcbiAgICAgICAgICogcmFuZ2UuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcuc2xpZGVyIFRoZSBJRC9DbGFzcyBvZiB0aGUgdGFnIHJhbmdlIHNsaWRlci5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5zbGlkZXJTY2FsZSBUaGUgSUQvQ2xhc3Mgb2YgdGhlIHNsaWRlciBzY2FsZS5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5zZWN0aW9uVGFncyBUaGUgSUQvQ2xhc3Mgb2YgdGhlIHRhZyBzZWN0aW9uLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLmN1cnJlbnRUYWdzIFRoZSBJRC9DbGFzcyBvZiB0aGUgdGFnIGNvbnRhaW5lci5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5zbGlkZXJIYW5kbGUgVGhlIElEL0NsYXNzIG9mIHRoZSBzbGlkZXIgaGFuZGxlLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLnNsaWRlclNlY3Rpb25TdHJpcGUgVGhlIElEL0NsYXNzIG9mIHRoZSByYW5nZSBzdHJpcGUgb25cbiAgICAgICAgICogdGhlIHNsaWRlci5cbiAgICAgICAgICogQHBhcmFtIHtOdW1iZXJ9IGNvbmZpZy5yZWN1cnJlbmNlTnVtYmVyIFRoZSBudW1iZXIgb2YgZGlzcGxheWVkIHRhZ3MgaW4gYSByb3cuXG4gICAgICAgICAqIEBwYXJhbSB7TnVtYmVyfSBjb25maWcucmVjdXJyZW5jZVNlY3Rpb25OdW1iZXIgVGhlIG51bWJlciBvZiBkaXNwbGF5ZWQgdGFncyBpblxuICAgICAgICAgKiBhIHNlY3Rpb24uXG4gICAgICAgICAqIEBwYXJhbSB7Qm9vbGVhbn0gY29uZmlnLnNpZGViYXJSaWdodCBJZiB0cnVlLCB0aGUgY3VycmVudCB0YWcgcm93IHdpbGwgc2hvdyB1cFxuICAgICAgICAgKiB0byB0aGUgbGVmdCBvZiB0aGUgc2lkZWJhciB3aWRnZXQuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcubG9hZGVyIFRoZSBJRC9DbGFzcyBvZiB0aGUgQUpBWC1Mb2FkZXIuXG4gICAgICAgICAqIEBwYXJhbSB7T2JqZWN0fSBjb25maWcubXNnIEFuIG9iamVjdCBvZiBzdHJpbmdzIGZvciBtdWx0aSBsYW5ndWFnZSB1c2UuXG4gICAgICAgICAqL1xuICAgICAgICBpbml0OiBmdW5jdGlvbiggY29uZmlnICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICcjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIubmVyRmFjZXR0aW5nLmluaXQnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICcjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIubmVyRmFjZXR0aW5nLmluaXQ6IGNvbmZpZyAtICcsIGNvbmZpZyApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICAkLmV4dGVuZCggdHJ1ZSwgX2RlZmF1bHRzLCBjb25maWcgKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgaWYgKCB2aWV3ZXIubG9jYWxTdG9yYWdlUG9zc2libGUgKSB7XG4gICAgICAgICAgICAgICAgLy8gc2hvdyBsb2FkZXJcbiAgICAgICAgICAgICAgICAkKCBfZGVmYXVsdHMubG9hZGVyICkuc2hvdygpO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIC8vIGNsZWFuIGxvY2FsIHN0b3JhZ2VcbiAgICAgICAgICAgICAgICBfY2xlYW5VcExvY2FsU3RvcmFnZSgpO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIC8vIHJlc2V0IHNlbGVjdCBtZW51XG4gICAgICAgICAgICAgICAgJCggX2RlZmF1bHRzLnNldFRhZ1JhbmdlICkuZmluZCggJ29wdGlvbicgKS5hdHRyKCAnc2VsZWN0ZWQnLCBmYWxzZSApO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIGlmICggX2RlZmF1bHRzLmN1cnJlbnRQYWdlID09PSAnbmVyZmFjZXR0aW5nJyApIHtcbiAgICAgICAgICAgICAgICAgICAgJCggX2RlZmF1bHRzLnNldFRhZ1JhbmdlT3ZlcnZpZXcgKS5maW5kKCAnb3B0aW9uW3ZhbHVlPVwiMVwiXScgKS5wcm9wKCAnc2VsZWN0ZWQnLCB0cnVlICk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAkKCBfZGVmYXVsdHMuc2V0VGFnUmFuZ2VPdmVydmlldyApLmZpbmQoICdvcHRpb25bdmFsdWU9XCIxMFwiXScgKS5wcm9wKCAnc2VsZWN0ZWQnLCB0cnVlICk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIC8vIHJlc2V0IGZhY2V0dGluZyBpY29uc1xuICAgICAgICAgICAgICAgIF9yZXNldEZhY2V0dGluZ0ljb25zKCk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLy8gZ2V0IGRhdGEgZm9yIGN1cnJlbnQgd29ya1xuICAgICAgICAgICAgICAgIGlmICggX2RlZmF1bHRzLmN1cnJlbnRQYWdlID09PSAnbmVyZmFjZXR0aW5nJyApIHtcbiAgICAgICAgICAgICAgICAgICAgX2FwaUNhbGwgPSBfZ2V0QWxsVGFnc09mQVJhbmdlKCAxLCAnLScgKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIF9hcGlDYWxsID0gX2dldEFsbFRhZ3NPZkFSYW5nZSggMTAsICctJyApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICBfcHJvbWlzZSA9IHZpZXdlci5oZWxwZXIuZ2V0UmVtb3RlRGF0YSggX2FwaUNhbGwgKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICBfcHJvbWlzZS50aGVuKCBmdW5jdGlvbigganNvbiApIHtcbiAgICAgICAgICAgICAgICAgICAgX2pzb24gPSBqc29uO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgLy8gY2hlY2sgaWYgZGF0YSBpcyBub3QgZW1wdHlcbiAgICAgICAgICAgICAgICAgICAgaWYgKCBfanNvbiAhPT0gbnVsbCB8fCBfanNvbiAhPT0gJ3VuZGVmaW5kZWQnICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgLy8gY2hlY2sgaWYgb3ZlcnZpZXcgaXMgYWxyZWFkeSBsb2FkZWRcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICggJCggX2RlZmF1bHRzLm92ZXJ2aWV3Q29udGVudCApLmh0bWwoKSA9PT0gJycgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgX3JlbmRlck92ZXJ2aWV3KCBfanNvbiApO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgX2h0bWwgPSB2aWV3ZXIuaGVscGVyLnJlbmRlckFsZXJ0KCAnYWxlcnQtZGFuZ2VyJywgX2RlZmF1bHRzLm1zZy5ub0pTT04gKyAnPGJyIC8+PGJyIC8+VVJMOiAnICsgX2FwaUNhbGwsIHRydWUgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICQoIF9kZWZhdWx0cy5vdmVydmlld0NvbnRlbnQgKS5odG1sKCBfaHRtbCApO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfSApLnRoZW4oIG51bGwsIGZ1bmN0aW9uKCBlcnJvciApIHtcbiAgICAgICAgICAgICAgICAgICAgJCggJy5mYWNldHRpbmctY29udGVudCcgKS5lbXB0eSgpLmFwcGVuZCggdmlld2VyLmhlbHBlclxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIC5yZW5kZXJBbGVydCggJ2FsZXJ0LWRhbmdlcicsICc8c3Ryb25nPlN0YXR1czogPC9zdHJvbmc+JyArIGVycm9yLnN0YXR1cyArICcgJyArIGVycm9yLnN0YXR1c1RleHQsIGZhbHNlICkgKTtcbiAgICAgICAgICAgICAgICAgICAgY29uc29sZS5lcnJvciggJ0VSUk9SOiB2aWV3ZXIubmVyRmFjZXR0aW5nLmluaXQgLSAnLCBlcnJvciApO1xuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAvKipcbiAgICAgICAgICAgICAgICAgKiBFdmVudCBpZiBvdmVydmlldyB0YWIgaXMgY2xpY2tlZC5cbiAgICAgICAgICAgICAgICAgKi9cbiAgICAgICAgICAgICAgICAkKCBfZGVmYXVsdHMub3ZlcnZpZXdUcmlnZ2VyICkub24oICdjbGljaycsIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICAvLyBzaG93IGxvYWRlclxuICAgICAgICAgICAgICAgICAgICAkKCBfZGVmYXVsdHMubG9hZGVyICkuc2hvdygpO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgLy8gcmVzZXRzXG4gICAgICAgICAgICAgICAgICAgICQoIF9kZWZhdWx0cy5zZXRUYWdSYW5nZSApLmZpbmQoICdvcHRpb24nICkuYXR0ciggJ3NlbGVjdGVkJywgZmFsc2UgKTtcbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgIGlmICggX2RlZmF1bHRzLmN1cnJlbnRQYWdlID09PSAnbmVyZmFjZXR0aW5nJyApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICQoIF9kZWZhdWx0cy5zZXRUYWdSYW5nZU92ZXJ2aWV3ICkuZmluZCggJ29wdGlvblt2YWx1ZT1cIjFcIl0nICkucHJvcCggJ3NlbGVjdGVkJywgdHJ1ZSApO1xuICAgICAgICAgICAgICAgICAgICAgICAgbG9jYWxTdG9yYWdlLnNldEl0ZW0oICdjdXJyZW50TmVyUGFnZVJhbmdlJywgJzEnICk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAkKCBfZGVmYXVsdHMuc2V0VGFnUmFuZ2VPdmVydmlldyApLmZpbmQoICdvcHRpb25bdmFsdWU9XCIxMFwiXScgKS5wcm9wKCAnc2VsZWN0ZWQnLCB0cnVlICk7XG4gICAgICAgICAgICAgICAgICAgICAgICBsb2NhbFN0b3JhZ2Uuc2V0SXRlbSggJ2N1cnJlbnROZXJQYWdlUmFuZ2UnLCAnMTAnICk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgX2N1cnJlbnROZXJQYWdlUmFuZ2UgPSBsb2NhbFN0b3JhZ2UuZ2V0SXRlbSggJ2N1cnJlbnROZXJQYWdlUmFuZ2UnICk7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICBsb2NhbFN0b3JhZ2Uuc2V0SXRlbSggJ2N1cnJlbnROZXJUeXBlJywgJy0nICk7XG4gICAgICAgICAgICAgICAgICAgIF9jdXJyZW50TmVyVHlwZSA9IGxvY2FsU3RvcmFnZS5nZXRJdGVtKCAnY3VycmVudE5lclR5cGUnICk7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICBfcmVzZXRGYWNldHRpbmdJY29ucygpO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgLy8gY2hlY2sgaWYgdGFiIGlzIGFjdGl2ZVxuICAgICAgICAgICAgICAgICAgICBpZiAoICQoIHRoaXMgKS5wYXJlbnQoKS5oYXNDbGFzcyggJ2FjdGl2ZScgKSApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGNvbnNvbGUuaW5mbyggJ092ZXJ2aWV3IGlzIGFscmVhZHkgYWN0aXZlLicgKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICggX2RlZmF1bHRzLmN1cnJlbnRQYWdlID09PSAnbmVyZmFjZXR0aW5nJyApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBfYXBpQ2FsbCA9IF9nZXRBbGxUYWdzT2ZBUmFuZ2UoIDEsICctJyApO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgX2FwaUNhbGwgPSBfZ2V0QWxsVGFnc09mQVJhbmdlKCAxMCwgJy0nICk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgIF9wcm9taXNlID0gdmlld2VyLmhlbHBlci5nZXRSZW1vdGVEYXRhKCBfYXBpQ2FsbCApO1xuICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICBfcHJvbWlzZS50aGVuKCBmdW5jdGlvbigganNvbiApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBfanNvbiA9IGpzb247XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgX3JlbmRlck92ZXJ2aWV3KCBfanNvbiApO1xuICAgICAgICAgICAgICAgICAgICAgICAgfSApLnRoZW4oIG51bGwsIGZ1bmN0aW9uKCBlcnJvciApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCAnLmZhY2V0dGluZy1jb250ZW50JyApLmVtcHR5KCkuYXBwZW5kKCB2aWV3ZXIuaGVscGVyLnJlbmRlckFsZXJ0KCAnYWxlcnQtZGFuZ2VyJywgJzxzdHJvbmc+U3RhdHVzOiA8L3N0cm9uZz4nICsgZXJyb3Iuc3RhdHVzICsgJyAnXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICArIGVycm9yLnN0YXR1c1RleHQsIGZhbHNlICkgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBjb25zb2xlLmVycm9yKCAnRVJST1I6IHZpZXdlci5uZXJGYWNldHRpbmcuaW5pdCAtICcsIGVycm9yICk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAvKipcbiAgICAgICAgICAgICAgICAgKiBFdmVudCBpZiBzZWN0aW9uIHRhYiBpcyBjbGlja2VkLlxuICAgICAgICAgICAgICAgICAqL1xuICAgICAgICAgICAgICAgICQoIF9kZWZhdWx0cy5zZWN0aW9uVHJpZ2dlciApLm9uKCAnY2xpY2snLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgLy8gc2hvdyBsb2FkZXJcbiAgICAgICAgICAgICAgICAgICAgJCggX2RlZmF1bHRzLmxvYWRlciApLnNob3coKTtcbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgIC8vIHJlc2V0IHNlbGVjdCBtZW51XG4gICAgICAgICAgICAgICAgICAgICQoIF9kZWZhdWx0cy5zZXRUYWdSYW5nZSApLmZpbmQoICdvcHRpb24nICkuYXR0ciggJ3NlbGVjdGVkJywgZmFsc2UgKTtcbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgIGlmICggX2RlZmF1bHRzLmN1cnJlbnRQYWdlID09PSAnbmVyZmFjZXR0aW5nJyApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICQoIF9kZWZhdWx0cy5zZXRUYWdSYW5nZVNlY3Rpb24gKS5maW5kKCAnb3B0aW9uW3ZhbHVlPVwiNVwiXScgKS5wcm9wKCAnc2VsZWN0ZWQnLCB0cnVlICk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAkKCBfZGVmYXVsdHMuc2V0VGFnUmFuZ2VTZWN0aW9uICkuZmluZCggJ29wdGlvblt2YWx1ZT1cIjEwXCJdJyApLnByb3AoICdzZWxlY3RlZCcsIHRydWUgKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgLy8gcmVzZXQgZmFjZXR0aW5nXG4gICAgICAgICAgICAgICAgICAgIF9yZXNldEZhY2V0dGluZ0ljb25zKClcblxuICAgICAgICAgICAgICAgICAgICAvLyBzZXQgbG9jYWwgc3RvcmFnZSB2YWx1ZVxuICAgICAgICAgICAgICAgICAgICBpZiAoIF9kZWZhdWx0cy5jdXJyZW50UGFnZSA9PT0gJ25lcmZhY2V0dGluZycgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBsb2NhbFN0b3JhZ2Uuc2V0SXRlbSggJ2N1cnJlbnROZXJQYWdlUmFuZ2UnLCA1ICk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBsb2NhbFN0b3JhZ2Uuc2V0SXRlbSggJ2N1cnJlbnROZXJQYWdlUmFuZ2UnLCAxMCApO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIF9jdXJyZW50TmVyUGFnZVJhbmdlID0gbG9jYWxTdG9yYWdlLmdldEl0ZW0oICdjdXJyZW50TmVyUGFnZVJhbmdlJyApO1xuICAgICAgICAgICAgICAgICAgICBsb2NhbFN0b3JhZ2Uuc2V0SXRlbSggJ2N1cnJlbnROZXJUeXBlJywgJy0nICk7XG4gICAgICAgICAgICAgICAgICAgIF9jdXJyZW50TmVyVHlwZSA9IGxvY2FsU3RvcmFnZS5nZXRJdGVtKCAnY3VycmVudE5lclR5cGUnICk7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAvLyBjaGVjayBpZiB0YWIgaXMgYWN0aXZlXG4gICAgICAgICAgICAgICAgICAgIGlmICggJCggdGhpcyApLnBhcmVudCgpLmhhc0NsYXNzKCAnYWN0aXZlJyApICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgY29uc29sZS5pbmZvKCAnU2VjdGlvbiBpcyBhbHJlYWR5IGFjdGl2ZS4nICk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBfcmVuZGVyU2VjdGlvbigpO1xuICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICAvLyByZXNldCBzZWN0aW9uIHN0cmlwZVxuICAgICAgICAgICAgICAgICAgICAgICAgJCggX2RlZmF1bHRzLnNsaWRlclNlY3Rpb25TdHJpcGUgKS5jc3MoICd0b3AnLCAnMHB4JyApO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIC8qKlxuICAgICAgICAgICAgICAgICAqIEV2ZW50IGlmIHNlbGVjdCBtZW51IGNoYW5nZXMuXG4gICAgICAgICAgICAgICAgICovXG4gICAgICAgICAgICAgICAgJCggX2RlZmF1bHRzLnNldFRhZ1JhbmdlICkub24oICdjaGFuZ2UnLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgdmFyIGN1cnJWYWwgPSAkKCB0aGlzICkudmFsKCk7XG4gICAgICAgICAgICAgICAgICAgIF9jdXJyZW50TmVyVHlwZSA9IGxvY2FsU3RvcmFnZS5nZXRJdGVtKCAnY3VycmVudE5lclR5cGUnICk7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAvLyBzaG93IGxvYWRlclxuICAgICAgICAgICAgICAgICAgICAkKCBfZGVmYXVsdHMubG9hZGVyICkuc2hvdygpO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgLy8gc2F2ZSBjdXJyZW50IHZhbHVlIGluIGxvY2FsIHN0b3JhZ2VcbiAgICAgICAgICAgICAgICAgICAgbG9jYWxTdG9yYWdlLnNldEl0ZW0oICdjdXJyZW50TmVyUGFnZVJhbmdlJywgY3VyclZhbCApO1xuICAgICAgICAgICAgICAgICAgICBfY3VycmVudE5lclBhZ2VSYW5nZSA9IGxvY2FsU3RvcmFnZS5nZXRJdGVtKCAnY3VycmVudE5lclBhZ2VSYW5nZScgKTtcbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgIC8vIHJlbmRlciBvdmVydmlld1xuICAgICAgICAgICAgICAgICAgICBpZiAoICQoIHRoaXMgKS5oYXNDbGFzcyggJ292ZXJ2aWV3JyApICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKCBfY3VycmVudE5lclR5cGUgPT09IG51bGwgfHwgX2N1cnJlbnROZXJUeXBlID09PSAnJyApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBfY3VycmVudE5lclR5cGUgPSAnLSc7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBfYXBpQ2FsbCA9IF9nZXRBbGxUYWdzT2ZBUmFuZ2UoIGN1cnJWYWwsIF9jdXJyZW50TmVyVHlwZSApO1xuICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICBfcHJvbWlzZSA9IHZpZXdlci5oZWxwZXIuZ2V0UmVtb3RlRGF0YSggX2FwaUNhbGwgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgX3Byb21pc2UudGhlbiggZnVuY3Rpb24oIGpzb24gKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgX2pzb24gPSBqc29uO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIGNoZWNrIGlmIGRhdGEgaXMgbm90IGVtcHR5XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgaWYgKCBfanNvbiAhPT0gbnVsbCB8fCBfanNvbiAhPT0gJ3VuZGVmaW5kZWQnICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBfcmVuZGVyT3ZlcnZpZXcoIF9qc29uICk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBfaHRtbCA9IHZpZXdlci5oZWxwZXIucmVuZGVyQWxlcnQoICdhbGVydC1kYW5nZXInLCBfZGVmYXVsdHMubXNnLm5vSlNPTiArICc8YnIgLz48YnIgLz5VUkw6ICcgKyBfYXBpQ2FsbCwgdHJ1ZSApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCBfZGVmYXVsdHMub3ZlcnZpZXdDb250ZW50ICkuaHRtbCggX2h0bWwgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICB9ICkudGhlbiggbnVsbCwgZnVuY3Rpb24oIGVycm9yICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICQoICcuZmFjZXR0aW5nLWNvbnRlbnQnICkuZW1wdHkoKS5hcHBlbmQoIHZpZXdlci5oZWxwZXIucmVuZGVyQWxlcnQoICdhbGVydC1kYW5nZXInLCAnPHN0cm9uZz5TdGF0dXM6IDwvc3Ryb25nPicgKyBlcnJvci5zdGF0dXMgKyAnICdcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICsgZXJyb3Iuc3RhdHVzVGV4dCwgZmFsc2UgKSApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNvbnNvbGUuZXJyb3IoICdFUlJPUjogdmlld2VyLm5lckZhY2V0dGluZy5pbml0IC0gJywgZXJyb3IgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAvLyByZW5kZXIgc2VjdGlvblxuICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIC8vIHNldHVwIHZhbHVlc1xuICAgICAgICAgICAgICAgICAgICAgICAgbG9jYWxTdG9yYWdlLnNldEl0ZW0oICdjdXJyZW50TmVyUGFnZVJhbmdlJywgY3VyclZhbCApO1xuICAgICAgICAgICAgICAgICAgICAgICAgX2N1cnJlbnROZXJQYWdlUmFuZ2UgPSBsb2NhbFN0b3JhZ2UuZ2V0SXRlbSggJ2N1cnJlbnROZXJQYWdlUmFuZ2UnICk7XG4gICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgIF9yZW5kZXJTZWN0aW9uKCk7XG4gICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgIC8vIHJlc2V0IHNlY3Rpb24gc3RyaXBlXG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAoIF9jdXJyZW50TmVyUGFnZVJhbmdlID4gX3BhZ2VDb3VudCApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCBfZGVmYXVsdHMuc2xpZGVyU2VjdGlvblN0cmlwZSApLmNzcygge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAndG9wJzogJzBweCcsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICdoZWlnaHQnOiAnNjAwcHgnXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgJCggX2RlZmF1bHRzLnNsaWRlclNlY3Rpb25TdHJpcGUgKS5jc3MoIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgJ3RvcCc6ICcwcHgnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAnaGVpZ2h0JzogJzEwMHB4J1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLyoqXG4gICAgICAgICAgICAgICAgICogRXZlbnQgaWYgZmFjZXR0aW5nIGljb25zIGFyZSBjbGlja2VkLlxuICAgICAgICAgICAgICAgICAqL1xuICAgICAgICAgICAgICAgICQoIF9kZWZhdWx0cy5mYWNldHRpbmdUcmlnZ2VyICkub24oICdjbGljaycsIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICB2YXIgY3VyclR5cGUgPSAkKCB0aGlzICkuYXR0ciggJ2RhdGEtdHlwZScgKTtcbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgIC8vIHNob3cgbG9hZGVyXG4gICAgICAgICAgICAgICAgICAgICQoIF9kZWZhdWx0cy5sb2FkZXIgKS5zaG93KCk7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAvLyBzZXQgdmFsdWVzXG4gICAgICAgICAgICAgICAgICAgIGxvY2FsU3RvcmFnZS5zZXRJdGVtKCAnY3VycmVudE5lclR5cGUnLCBjdXJyVHlwZSApO1xuICAgICAgICAgICAgICAgICAgICBfY3VycmVudE5lclR5cGUgPSBsb2NhbFN0b3JhZ2UuZ2V0SXRlbSggJ2N1cnJlbnROZXJUeXBlJyApO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgaWYgKCBfZGVmYXVsdHMuY3VycmVudFBhZ2UgPT09ICduZXJmYWNldHRpbmcnICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKCBfY3VycmVudE5lclBhZ2VSYW5nZSA9PSBudWxsIHx8IF9jdXJyZW50TmVyUGFnZVJhbmdlID09PSAnJyApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBfY3VycmVudE5lclBhZ2VSYW5nZSA9IGxvY2FsU3RvcmFnZS5zZXRJdGVtKCAnY3VycmVudE5lclBhZ2VSYW5nZScsIDEgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICggX2N1cnJlbnROZXJQYWdlUmFuZ2UgPT0gbnVsbCB8fCBfY3VycmVudE5lclBhZ2VSYW5nZSA9PT0gJycgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgX2N1cnJlbnROZXJQYWdlUmFuZ2UgPSBsb2NhbFN0b3JhZ2Uuc2V0SXRlbSggJ2N1cnJlbnROZXJQYWdlUmFuZ2UnLCAxMCApO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIF9jdXJyZW50TmVyUGFnZVJhbmdlID0gbG9jYWxTdG9yYWdlLmdldEl0ZW0oICdjdXJyZW50TmVyUGFnZVJhbmdlJyApO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgLy8gYWN0aXZhdGUgaWNvbnNcbiAgICAgICAgICAgICAgICAgICAgJCggJy5mYWNldHRpbmctdHJpZ2dlcicgKS5yZW1vdmVDbGFzcyggJ2FjdGl2ZScgKTtcbiAgICAgICAgICAgICAgICAgICAgJCggdGhpcyApLmFkZENsYXNzKCAnYWN0aXZlJyApO1xuICAgICAgICAgICAgICAgICAgICAkKCAnLnJlc2V0LWZpbHRlcicgKS5zaG93KCk7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAvLyBmaWx0ZXIgb3ZlcnZpZXdcbiAgICAgICAgICAgICAgICAgICAgaWYgKCAkKCB0aGlzICkucGFyZW50KCkucGFyZW50KCkucGFyZW50KCkuYXR0ciggJ2lkJyApID09PSAnb3ZlcnZpZXcnICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgLy8gc2V0dXAgZGF0YVxuICAgICAgICAgICAgICAgICAgICAgICAgX2FwaUNhbGwgPSBfZ2V0QWxsVGFnc09mQVJhbmdlKCBfY3VycmVudE5lclBhZ2VSYW5nZSwgX2N1cnJlbnROZXJUeXBlICk7XG4gICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgIF9wcm9taXNlID0gdmlld2VyLmhlbHBlci5nZXRSZW1vdGVEYXRhKCBfYXBpQ2FsbCApO1xuICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICBfcHJvbWlzZS50aGVuKCBmdW5jdGlvbigganNvbiApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBfanNvbiA9IGpzb247XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgX3JlbmRlck92ZXJ2aWV3KCBfanNvbiApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIGhpZGUgc2VsZWN0IGFsbFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGlmICggJCggdGhpcyApLnBhcmVudCgpLmhhc0NsYXNzKCAncmVzZXQtZmlsdGVyJyApICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCB0aGlzICkucGFyZW50KCkuaGlkZSgpO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyBzZXQgaWNvbnMgdG8gYWN0aXZlIGlmIFwiYWxsXCIgaXMgc2VsZWN0ZWRcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBpZiAoIF9jdXJyZW50TmVyVHlwZSA9PT0gJy0nICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCAnLmZhY2V0dGluZy10cmlnZ2VyJyApLmFkZENsYXNzKCAnYWN0aXZlJyApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIH0gKS50aGVuKCBudWxsLCBmdW5jdGlvbiggZXJyb3IgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgJCggJy5mYWNldHRpbmctY29udGVudCcgKS5lbXB0eSgpLmFwcGVuZCggdmlld2VyLmhlbHBlci5yZW5kZXJBbGVydCggJ2FsZXJ0LWRhbmdlcicsICc8c3Ryb25nPlN0YXR1czogPC9zdHJvbmc+JyArIGVycm9yLnN0YXR1cyArICcgJ1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgKyBlcnJvci5zdGF0dXNUZXh0LCBmYWxzZSApICk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgY29uc29sZS5lcnJvciggJ0VSUk9SOiB2aWV3ZXIubmVyRmFjZXR0aW5nLmluaXQgLSAnLCBlcnJvciApO1xuICAgICAgICAgICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIC8vIGZpbHRlciBzZWN0aW9uXG4gICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgX3JlbmRlclNlY3Rpb24oKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgLy8gaGlkZSBzZWxlY3QgYWxsXG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAoICQoIHRoaXMgKS5wYXJlbnQoKS5oYXNDbGFzcyggJ3Jlc2V0LWZpbHRlcicgKSApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCB0aGlzICkucGFyZW50KCkuaGlkZSgpO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgLy8gc2V0IGljb25zIHRvIGFjdGl2ZSBpZiBcImFsbFwiIGlzIHNlbGVjdGVkXG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAoIF9jdXJyZW50TmVyVHlwZSA9PT0gJy0nICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICQoICcuZmFjZXR0aW5nLXRyaWdnZXInICkuYWRkQ2xhc3MoICdhY3RpdmUnICk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICAvLyByZXNldCBzZWN0aW9uIHN0cmlwZVxuICAgICAgICAgICAgICAgICAgICAgICAgJCggX2RlZmF1bHRzLnNsaWRlclNlY3Rpb25TdHJpcGUgKS5jc3MoICd0b3AnLCAnMHB4JyApO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICQoICcuZmFjZXR0aW5nLWNvbnRlbnQnICkuZW1wdHkoKS5hcHBlbmQoIHZpZXdlci5oZWxwZXJcbiAgICAgICAgICAgICAgICAgICAgICAgIC5yZW5kZXJBbGVydCggJ2FsZXJ0LWRhbmdlcicsICc8c3Ryb25nPkRlYWN0aXZhdGVkOiA8L3N0cm9uZz5Ob3QgcG9zc2libGUgdG8gd3JpdGUgaW4gbG9jYWwgU3RvcmFnZSEnLCBmYWxzZSApICk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICB9O1xuICAgIFxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB0byByZW5kZXIgdGhlIE5FUiBvdmVydmlldy5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9yZW5kZXJPdmVydmlld1xuICAgICAqIEBwYXJhbSB7T2JqZWN0fSBkYXRhIEEgSlNPTi1PYmplY3QuXG4gICAgICogQHJldHVybnMge1N0aW5nfSBBIEhUTUwtU3RyaW5nIHdoaWNoIHJlbmRlcnMgdGhlIG92ZXJ2aWV3LlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9yZW5kZXJPdmVydmlldyggZGF0YSApIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX3JlbmRlck92ZXJ2aWV3KCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX3JlbmRlck92ZXJ2aWV3OiBkYXRhID0gJywgZGF0YSApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICBfaHRtbCA9ICcnO1xuICAgICAgICBfaHRtbCArPSAnPHVsIGNsYXNzPVwib3ZlcnZpZXctc2NhbGVcIj4nO1xuICAgICAgICBcbiAgICAgICAgLy8gcmVuZGVyIHBhZ2UgbnVtYmVyXG4gICAgICAgICQuZWFjaCggZGF0YS5wYWdlcywgZnVuY3Rpb24oIHAsIHBhZ2UgKSB7XG4gICAgICAgICAgICBfaHRtbCArPSAnPGxpPic7XG4gICAgICAgICAgICBfaHRtbCArPSAnPGRpdiBjbGFzcz1cInBhZ2UtbnVtYmVyXCI+JztcbiAgICAgICAgICAgIGlmICggZGF0YS5yYW5nZVNpemUgPT0gMSApIHtcbiAgICAgICAgICAgICAgICBpZiAoIF9kZWZhdWx0cy5jdXJyZW50UGFnZSA9PT0gJ25lcmZhY2V0dGluZycgKSB7XG4gICAgICAgICAgICAgICAgICAgIF9odG1sICs9ICc8YSBocmVmPVwiJyArIF9kZWZhdWx0cy5iYXNlVXJsICsgJy9pbWFnZS8nICsgX2RlZmF1bHRzLndvcmtJZCArICcvJyArIHBhZ2UucGFnZU9yZGVyICsgJy9cIj4nO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgX2h0bWwgKz0gJzxhIGhyZWY9XCInICsgX2RlZmF1bHRzLmJhc2VVcmwgKyAnLycgKyBfZGVmYXVsdHMuY3VycmVudFBhZ2UgKyAnLycgKyBfZGVmYXVsdHMud29ya0lkICsgJy8nICsgcGFnZS5wYWdlT3JkZXIgKyAnL1wiPic7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIF9odG1sICs9IHBhZ2UucGFnZU9yZGVyO1xuICAgICAgICAgICAgICAgIF9odG1sICs9ICc8L2E+JztcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIGlmICggX2RlZmF1bHRzLmN1cnJlbnRQYWdlID09PSAnbmVyZmFjZXR0aW5nJyApIHtcbiAgICAgICAgICAgICAgICAgICAgaWYgKCBwYWdlLmZpcnN0UGFnZSAhPT0gdW5kZWZpbmVkIHx8IHBhZ2UubGFzdFBhZ2UgIT09IHVuZGVmaW5lZCApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIF9odG1sICs9ICc8YSBocmVmPVwiJyArIF9kZWZhdWx0cy5iYXNlVXJsICsgJy9pbWFnZS8nICsgX2RlZmF1bHRzLndvcmtJZCArICcvJyArIHBhZ2UuZmlyc3RQYWdlICsgJy9cIj4nO1xuICAgICAgICAgICAgICAgICAgICAgICAgX2h0bWwgKz0gcGFnZS5maXJzdFBhZ2UgKyAnLScgKyBwYWdlLmxhc3RQYWdlO1xuICAgICAgICAgICAgICAgICAgICAgICAgX2h0bWwgKz0gJzwvYT4nO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgX2h0bWwgKz0gJzxhIGhyZWY9XCInICsgX2RlZmF1bHRzLmJhc2VVcmwgKyAnL2ltYWdlLycgKyBfZGVmYXVsdHMud29ya0lkICsgJy8nICsgcGFnZS5wYWdlT3JkZXIgKyAnL1wiPic7XG4gICAgICAgICAgICAgICAgICAgICAgICBfaHRtbCArPSBwYWdlLnBhZ2VPcmRlcjtcbiAgICAgICAgICAgICAgICAgICAgICAgIF9odG1sICs9ICc8L2E+JztcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgaWYgKCBwYWdlLmZpcnN0UGFnZSAhPT0gdW5kZWZpbmVkIHx8IHBhZ2UubGFzdFBhZ2UgIT09IHVuZGVmaW5lZCApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIF9odG1sICs9ICc8YSBocmVmPVwiJyArIF9kZWZhdWx0cy5iYXNlVXJsICsgJy8nICsgX2RlZmF1bHRzLmN1cnJlbnRQYWdlICsgJy8nICsgX2RlZmF1bHRzLndvcmtJZCArICcvJyArIHBhZ2UuZmlyc3RQYWdlICsgJy9cIj4nO1xuICAgICAgICAgICAgICAgICAgICAgICAgX2h0bWwgKz0gcGFnZS5maXJzdFBhZ2UgKyAnLScgKyBwYWdlLmxhc3RQYWdlO1xuICAgICAgICAgICAgICAgICAgICAgICAgX2h0bWwgKz0gJzwvYT4nO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgX2h0bWwgKz0gJzxhIGhyZWY9XCInICsgX2RlZmF1bHRzLmJhc2VVcmwgKyAnLycgKyBfZGVmYXVsdHMuY3VycmVudFBhZ2UgKyAnLycgKyBfZGVmYXVsdHMud29ya0lkICsgJy8nICsgcGFnZS5wYWdlT3JkZXIgKyAnL1wiPic7XG4gICAgICAgICAgICAgICAgICAgICAgICBfaHRtbCArPSBwYWdlLnBhZ2VPcmRlcjtcbiAgICAgICAgICAgICAgICAgICAgICAgIF9odG1sICs9ICc8L2E+JztcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIF9odG1sICs9ICc8L2Rpdj4nO1xuICAgICAgICAgICAgX2h0bWwgKz0gJzxkaXYgY2xhc3M9XCJ0YWctY29udGFpbmVyXCI+JztcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gcmVuZGVyIHRhZ3NcbiAgICAgICAgICAgIGlmICggcGFnZS50YWdzLmxlbmd0aCA9PT0gMCB8fCBwYWdlLnRhZ3MubGVuZ3RoID09PSAndW5kZWZpbmVkJyApIHtcbiAgICAgICAgICAgICAgICBfaHRtbCArPSAnPHNwYW4gY2xhc3M9XCJwYWdlLXRhZyBlbXB0eVwiPicgKyBfZGVmYXVsdHMubXNnLmVtcHR5VGFnICsgJzwvc3Bhbj4nO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgJC5lYWNoKCBwYWdlLnRhZ3MsIGZ1bmN0aW9uKCB0LCB0YWcgKSB7XG4gICAgICAgICAgICAgICAgICAgIF9odG1sICs9ICc8c3BhbiBjbGFzcz1cInBhZ2UtdGFnICcgKyB0YWcudHlwZSArICdcIj4nICsgdGFnLnZhbHVlICsgJzwvc3Bhbj4nO1xuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIF9odG1sICs9ICc8L2Rpdj4nO1xuICAgICAgICAgICAgX2h0bWwgKz0gJzwvbGk+JztcbiAgICAgICAgfSApO1xuICAgICAgICBfaHRtbCArPSAnPC91bD4nO1xuICAgICAgICBcbiAgICAgICAgJCggX2RlZmF1bHRzLm92ZXJ2aWV3Q29udGVudCApLmhpZGUoKS5odG1sKCBfaHRtbCApLmZpbmQoICcudGFnLWNvbnRhaW5lcicgKS5lYWNoKCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICQoIHRoaXMgKS5jaGlsZHJlbiggJy5wYWdlLXRhZycgKS5zbGljZSggX2RlZmF1bHRzLnJlY3VycmVuY2VOdW1iZXIgKS5yZW1vdmUoKTtcbiAgICAgICAgfSApO1xuICAgICAgICAkKCBfZGVmYXVsdHMub3ZlcnZpZXdDb250ZW50ICkuc2hvdygpO1xuICAgICAgICBcbiAgICAgICAgLy8gaGlkZSBsb2FkZXJcbiAgICAgICAgJCggX2RlZmF1bHRzLmxvYWRlciApLmhpZGUoKTtcbiAgICAgICAgXG4gICAgICAgICQoICcudGFnLWNvbnRhaW5lcicgKS5vbigge1xuICAgICAgICAgICAgJ21vdXNlb3Zlcic6IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgIHZhciAkdGhpcyA9ICQoIHRoaXMgKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICBfc2hvd0N1cnJlbnRUYWdzKCAkdGhpcyApO1xuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICdtb3VzZW91dCc6IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgIF9oaWRlQ3VycmVudFRhZ3MoKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSApO1xuICAgIH1cbiAgICBcbiAgICAvKipcbiAgICAgKiBNZXRob2Qgd2hpY2ggc2hvd3MgdGhlIGN1cnJlbnQgdGFnIHJvdyBpbiBhIHRvb2x0aXAuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfc2hvd0N1cnJlbnRUYWdzXG4gICAgICogQHBhcmFtIHtPYmplY3R9ICRvYmogQW4galF1ZXJ5IG9iamVjdCBvZiB0aGUgY3VycmVudCB0YWcgcm93LlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9zaG93Q3VycmVudFRhZ3MoICRvYmogKSB7XG4gICAgICAgIHZhciBjb250ZW50ID0gJG9iai5odG1sKCk7XG4gICAgICAgIHZhciBwb3MgPSAkb2JqLnBvc2l0aW9uKCk7XG4gICAgICAgIFxuICAgICAgICBpZiAoIF9kZWZhdWx0cy5zaWRlYmFyUmlnaHQgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWZhdWx0cy5jdXJyZW50UGFnZSA9PT0gJ25lcmZhY2V0dGluZycgKSB7XG4gICAgICAgICAgICAgICAgJCggX2RlZmF1bHRzLmN1cnJlbnRUYWdzICkuaHRtbCggY29udGVudCApLmNzcygge1xuICAgICAgICAgICAgICAgICAgICAnZGlzcGxheSc6ICdibG9jaycsXG4gICAgICAgICAgICAgICAgICAgICd0b3AnOiBwb3MudG9wICsgMjUgKyAncHgnLFxuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICQoIF9kZWZhdWx0cy5jdXJyZW50VGFncyApLmFkZENsYXNzKCAncmlnaHQnICkuaHRtbCggY29udGVudCApLmNzcygge1xuICAgICAgICAgICAgICAgICAgICAnZGlzcGxheSc6ICdibG9jaycsXG4gICAgICAgICAgICAgICAgICAgICd0b3AnOiBwb3MudG9wIC0gMiArICdweCcsXG4gICAgICAgICAgICAgICAgICAgICdsZWZ0JzogJ2F1dG8nLFxuICAgICAgICAgICAgICAgICAgICAncmlnaHQnOiAnMTAwJSdcbiAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBpZiAoIF9kZWZhdWx0cy5jdXJyZW50UGFnZSA9PT0gJ25lcmZhY2V0dGluZycgKSB7XG4gICAgICAgICAgICAgICAgJCggX2RlZmF1bHRzLmN1cnJlbnRUYWdzICkuaHRtbCggY29udGVudCApLmNzcygge1xuICAgICAgICAgICAgICAgICAgICAnZGlzcGxheSc6ICdibG9jaycsXG4gICAgICAgICAgICAgICAgICAgICd0b3AnOiBwb3MudG9wICsgMjUgKyAncHgnXG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgJCggX2RlZmF1bHRzLmN1cnJlbnRUYWdzICkuaHRtbCggY29udGVudCApLmNzcygge1xuICAgICAgICAgICAgICAgICAgICAnZGlzcGxheSc6ICdibG9jaycsXG4gICAgICAgICAgICAgICAgICAgICd0b3AnOiBwb3MudG9wIC0gMiArICdweCdcbiAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICB9XG4gICAgXG4gICAgLyoqXG4gICAgICogTWV0aG9kIHdoaWNoIGhpZGVzIHRoZSBjdXJyZW50IHRhZyByb3cgdG9vbHRpcC5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9oaWRlQ3VycmVudFRhZ3NcbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfaGlkZUN1cnJlbnRUYWdzKCkge1xuICAgICAgICAkKCBfZGVmYXVsdHMuY3VycmVudFRhZ3MgKS5oaWRlKCk7XG4gICAgfVxuICAgIFxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB0byByZW5kZXIgdGhlIE5FUiBzZWN0aW9uLlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX3JlbmRlclNlY3Rpb25cbiAgICAgKiBAcGFyYW0ge09iamVjdH0gZGF0YSBBIEpTT04tT2JqZWN0LlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9yZW5kZXJTZWN0aW9uKCkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfcmVuZGVyU2VjdGlvbigpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19yZW5kZXJTZWN0aW9uOiBfY3VycmVudE5lclBhZ2VSYW5nZSA9ICcsIF9jdXJyZW50TmVyUGFnZVJhbmdlICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19yZW5kZXJTZWN0aW9uOiBfY3VycmVudE5lclR5cGUgPSAnLCBfY3VycmVudE5lclR5cGUgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgLy8gc2V0IHZhbHVlc1xuICAgICAgICBfYXBpQ2FsbCA9IF9nZXRBbGxUYWdzKCk7XG4gICAgICAgIFxuICAgICAgICBfcHJvbWlzZSA9IHZpZXdlci5oZWxwZXIuZ2V0UmVtb3RlRGF0YSggX2FwaUNhbGwgKTtcbiAgICAgICAgXG4gICAgICAgIF9wcm9taXNlLnRoZW4oIGZ1bmN0aW9uKCB3b3JrQ2FsbCApIHtcbiAgICAgICAgICAgIF9wYWdlQ291bnQgPSBfZ2V0UGFnZUNvdW50KCB3b3JrQ2FsbCApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBpZiAoIF9jdXJyZW50TmVyUGFnZVJhbmdlID09PSBudWxsIHx8IF9jdXJyZW50TmVyUGFnZVJhbmdlID09PSAnJyApIHtcbiAgICAgICAgICAgICAgICBfY3VycmVudE5lclBhZ2VSYW5nZSA9IGxvY2FsU3RvcmFnZS5nZXRJdGVtKCAnY3VycmVudE5lclBhZ2VSYW5nZScgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmICggX2N1cnJlbnROZXJUeXBlID09PSBudWxsIHx8IF9jdXJyZW50TmVyVHlwZSA9PT0gJycgKSB7XG4gICAgICAgICAgICAgICAgX2N1cnJlbnROZXJUeXBlID0gbG9jYWxTdG9yYWdlLmdldEl0ZW0oICdjdXJyZW50TmVyVHlwZScgKVxuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICAvLyByZW5kZXIgcGFnZSBjb3VudCB0byBzY2FsZVxuICAgICAgICAgICAgaWYgKCBfZGVmYXVsdHMuY3VycmVudFBhZ2UgPT09ICduZXJmYWNldHRpbmcnICkge1xuICAgICAgICAgICAgICAgICQoICcjc2xpZGVyU2NhbGUgLnNjYWxlLXBhZ2UuZW5kJyApLmh0bWwoIF9wYWdlQ291bnQgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIGlmICggX3BhZ2VDb3VudCA+IDEwMDAgKSB7XG4gICAgICAgICAgICAgICAgICAgICQoICcjc2xpZGVyU2NhbGUgLnNjYWxlLXBhZ2UuZW5kJyApLmh0bWwoICc5OTkrJyApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgJCggJyNzbGlkZXJTY2FsZSAuc2NhbGUtcGFnZS5lbmQnICkuaHRtbCggX3BhZ2VDb3VudCApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gaW5pdCBzbGlkZXJcbiAgICAgICAgICAgICQoIF9kZWZhdWx0cy5zbGlkZXIgKS5zbGlkZXIoIHtcbiAgICAgICAgICAgICAgICBvcmllbnRhdGlvbjogXCJ2ZXJ0aWNhbFwiLFxuICAgICAgICAgICAgICAgIHJhbmdlOiBmYWxzZSxcbiAgICAgICAgICAgICAgICBtaW46IDEsXG4gICAgICAgICAgICAgICAgbWF4OiBfcGFnZUNvdW50LFxuICAgICAgICAgICAgICAgIHZhbHVlOiBfcGFnZUNvdW50LFxuICAgICAgICAgICAgICAgIHNsaWRlOiBmdW5jdGlvbiggZXZlbnQsIHVpICkge1xuICAgICAgICAgICAgICAgICAgICBfc2xpZGVySGFuZGxlUG9zaXRpb24gPSAkKCBfZGVmYXVsdHMuc2xpZGVySGFuZGxlICkucG9zaXRpb24oKTtcbiAgICAgICAgICAgICAgICAgICAgX3NjYWxlVmFsdWUgPSAoIF9wYWdlQ291bnQgKyAxICkgLSB1aS52YWx1ZTtcbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgIC8vIHNob3cgYnViYmxlXG4gICAgICAgICAgICAgICAgICAgICQoICcucGFnZS1idWJibGUnICkuc2hvdygpO1xuICAgICAgICAgICAgICAgICAgICBfcmVuZGVyUGFnZUJ1YmJsZSggX3NjYWxlVmFsdWUgKTtcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIHN0YXJ0OiBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgX3NsaWRlckhhbmRsZVBvc2l0aW9uID0gJCggX2RlZmF1bHRzLnNsaWRlckhhbmRsZSApLnBvc2l0aW9uKCk7XG4gICAgICAgICAgICAgICAgICAgIF9tb3ZlZFNsaWRlckhhbmRsZVBvc2l0aW9uID0gX3NsaWRlckhhbmRsZVBvc2l0aW9uLnRvcDtcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIHN0b3A6IGZ1bmN0aW9uKCBldmVudCwgdWkgKSB7XG4gICAgICAgICAgICAgICAgICAgIF9jdXJyZW50TmVyVHlwZSA9IGxvY2FsU3RvcmFnZS5nZXRJdGVtKCAnY3VycmVudE5lclR5cGUnICk7XG4gICAgICAgICAgICAgICAgICAgIF9zbGlkZXJTY2FsZUhlaWdodCA9ICQoIF9kZWZhdWx0cy5zbGlkZXJTY2FsZSApLmhlaWdodCgpO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgLy8gc2V0IHBvc2l0aW9uIG9mIHNlY3Rpb24gc3RyaXBlXG4gICAgICAgICAgICAgICAgICAgIGlmICggX2N1cnJlbnROZXJQYWdlUmFuZ2UgPiBfcGFnZUNvdW50ICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgJCggX2RlZmF1bHRzLnNsaWRlclNlY3Rpb25TdHJpcGUgKS5jc3MoIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAndG9wJzogJzBweCcsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgJ2hlaWdodCc6ICc2MDBweCdcbiAgICAgICAgICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICggX3NsaWRlckhhbmRsZVBvc2l0aW9uLnRvcCA8IDEwMCApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCBfZGVmYXVsdHMuc2xpZGVyU2VjdGlvblN0cmlwZSApLmFuaW1hdGUoIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgJ3RvcCc6ICcwcHgnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAnaGVpZ2h0JzogJzEwMHB4J1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIGVsc2UgaWYgKCBfc2xpZGVySGFuZGxlUG9zaXRpb24udG9wID4gMTAwICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGlmICggX3NsaWRlckhhbmRsZVBvc2l0aW9uLnRvcCA+IDUwMCApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgJCggX2RlZmF1bHRzLnNsaWRlclNlY3Rpb25TdHJpcGUgKS5hbmltYXRlKCB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAndG9wJzogKCBfc2xpZGVyU2NhbGVIZWlnaHQgLSAxMDAgKSArICdweCcsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAnaGVpZ2h0JzogJzEwMHB4J1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBpZiAoIF9tb3ZlZFNsaWRlckhhbmRsZVBvc2l0aW9uIDwgX3NsaWRlckhhbmRsZVBvc2l0aW9uLnRvcCApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICQoIF9kZWZhdWx0cy5zbGlkZXJTZWN0aW9uU3RyaXBlICkuYW5pbWF0ZSgge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICd0b3AnOiBfc2xpZGVySGFuZGxlUG9zaXRpb24udG9wIC0gMjUgKyAncHgnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICdoZWlnaHQnOiAnMTAwcHgnXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCBfZGVmYXVsdHMuc2xpZGVyU2VjdGlvblN0cmlwZSApLmFuaW1hdGUoIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAndG9wJzogX3NsaWRlckhhbmRsZVBvc2l0aW9uLnRvcCAtIDUwICsgJ3B4JyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAnaGVpZ2h0JzogJzEwMHB4J1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAvLyByZW5kZXIgdGFnc1xuICAgICAgICAgICAgICAgICAgICBzd2l0Y2ggKCBfY3VycmVudE5lclBhZ2VSYW5nZSApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGNhc2UgJzUnOlxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIF9zdGFydCA9IF9zY2FsZVZhbHVlIC0gMjtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBfZW5kID0gX3NjYWxlVmFsdWUgKyAzO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHdoaWxlICggX3N0YXJ0IDwgMSApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgX3N0YXJ0Kys7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIF9lbmQrKztcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgd2hpbGUgKCBfZW5kID4gX3BhZ2VDb3VudCApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgX3N0YXJ0LS07XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIF9lbmQtLTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgX2FwaUNhbGwgPSBfZ2V0QWxsVGFnc09mUGFnZVNlY3Rpb24oIF9zdGFydCwgX2VuZCwgX2N1cnJlbnROZXJUeXBlICk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgICAgICAgICBjYXNlICcxMCc6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgX3N0YXJ0ID0gX3NjYWxlVmFsdWUgLSA1O1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIF9lbmQgPSBfc2NhbGVWYWx1ZSArIDU7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgd2hpbGUgKCBfc3RhcnQgPCAxICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBfc3RhcnQrKztcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgX2VuZCsrO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB3aGlsZSAoIF9lbmQgPiBfcGFnZUNvdW50ICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBfc3RhcnQtLTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgX2VuZC0tO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBfYXBpQ2FsbCA9IF9nZXRBbGxUYWdzT2ZQYWdlU2VjdGlvbiggX3N0YXJ0LCBfZW5kLCBfY3VycmVudE5lclR5cGUgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICAgICAgICAgIGNhc2UgJzUwJzpcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBfc3RhcnQgPSBfc2NhbGVWYWx1ZSAtIDI1O1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIF9lbmQgPSBfc2NhbGVWYWx1ZSArIDI1O1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHdoaWxlICggX3N0YXJ0IDwgMSApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgX3N0YXJ0Kys7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIF9lbmQrKztcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgd2hpbGUgKCBfZW5kID4gX3BhZ2VDb3VudCApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgX3N0YXJ0LS07XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIF9lbmQtLTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgX2FwaUNhbGwgPSBfZ2V0QWxsVGFnc09mUGFnZVNlY3Rpb24oIF9zdGFydCwgX2VuZCwgX2N1cnJlbnROZXJUeXBlICk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgICAgICAgICBjYXNlICcxMDAnOlxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIF9zdGFydCA9IF9zY2FsZVZhbHVlIC0gNTA7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgX2VuZCA9IF9zY2FsZVZhbHVlICsgNTA7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgd2hpbGUgKCBfc3RhcnQgPCAxICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBfc3RhcnQrKztcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgX2VuZCsrO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB3aGlsZSAoIF9lbmQgPiBfcGFnZUNvdW50ICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBfc3RhcnQtLTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgX2VuZC0tO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBfYXBpQ2FsbCA9IF9nZXRBbGxUYWdzT2ZQYWdlU2VjdGlvbiggX3N0YXJ0LCBfZW5kLCBfY3VycmVudE5lclR5cGUgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgX3Byb21pc2UgPSB2aWV3ZXIuaGVscGVyLmdldFJlbW90ZURhdGEoIF9hcGlDYWxsICk7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICBfcHJvbWlzZS50aGVuKCBmdW5jdGlvbigganNvbiApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIF9qc29uID0ganNvbjtcbiAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgX2h0bWwgPSBfcmVuZGVyU2VjdGlvblRhZ3MoIF9qc29uICk7XG4gICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICggX2h0bWwgPT09ICcnICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICQoIF9kZWZhdWx0cy5zZWN0aW9uVGFncyApLmhpZGUoKS5odG1sKCB2aWV3ZXIuaGVscGVyLnJlbmRlckFsZXJ0KCAnYWxlcnQtd2FybmluZycsIF9kZWZhdWx0cy5tc2cuZW1wdHlUYWcsIGZhbHNlICkgKS5zaG93KCk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCBfZGVmYXVsdHMuc2VjdGlvblRhZ3MgKS5oaWRlKCkuaHRtbCggX2h0bWwgKS5lYWNoKCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgJCggdGhpcyApLmNoaWxkcmVuKCAnLnBhZ2UtdGFnJyApLnNsaWNlKCBfZGVmYXVsdHMucmVjdXJyZW5jZVNlY3Rpb25OdW1iZXIgKS5yZW1vdmUoKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgJCggX2RlZmF1bHRzLnNlY3Rpb25UYWdzICkuc2hvdygpO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICAvLyBoaWRlIGJ1YmJsZVxuICAgICAgICAgICAgICAgICAgICAgICAgJCggJy5wYWdlLWJ1YmJsZScgKS5mYWRlT3V0KCk7XG4gICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgfSApLnRoZW4oIG51bGwsIGZ1bmN0aW9uKCBlcnJvciApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICQoICcuZmFjZXR0aW5nLWNvbnRlbnQnICkuZW1wdHkoKS5hcHBlbmQoIHZpZXdlci5oZWxwZXJcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLnJlbmRlckFsZXJ0KCAnYWxlcnQtZGFuZ2VyJywgJzxzdHJvbmc+U3RhdHVzOiA8L3N0cm9uZz4nICsgZXJyb3Iuc3RhdHVzICsgJyAnICsgZXJyb3Iuc3RhdHVzVGV4dCwgZmFsc2UgKSApO1xuICAgICAgICAgICAgICAgICAgICAgICAgY29uc29sZS5lcnJvciggJ0VSUk9SOiB2aWV3ZXIubmVyRmFjZXR0aW5nLmluaXQgLSAnLCBlcnJvciApO1xuICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICAvLyByZW5kZXIgc2VjdGlvbiB0YWdzXG4gICAgICAgICAgICBfYXBpQ2FsbCA9IF9nZXRBbGxUYWdzT2ZQYWdlU2VjdGlvbiggMCwgX2N1cnJlbnROZXJQYWdlUmFuZ2UsIF9jdXJyZW50TmVyVHlwZSApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBfcHJvbWlzZSA9IHZpZXdlci5oZWxwZXIuZ2V0UmVtb3RlRGF0YSggX2FwaUNhbGwgKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgX3Byb21pc2UudGhlbiggZnVuY3Rpb24oIGpzb24gKSB7XG4gICAgICAgICAgICAgICAgX2pzb24gPSBqc29uO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIF9odG1sID0gX3JlbmRlclNlY3Rpb25UYWdzKCBfanNvbiApO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIGlmICggX2h0bWwgPT09ICcnICkge1xuICAgICAgICAgICAgICAgICAgICAkKCBfZGVmYXVsdHMuc2VjdGlvblRhZ3MgKS5oaWRlKCkuaHRtbCggdmlld2VyLmhlbHBlci5yZW5kZXJBbGVydCggJ2FsZXJ0LXdhcm5pbmcnLCBfZGVmYXVsdHMubXNnLmVtcHR5VGFnLCBmYWxzZSApICkuc2hvdygpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgJCggX2RlZmF1bHRzLnNlY3Rpb25UYWdzICkuaGlkZSgpLmh0bWwoIF9odG1sICkuZWFjaCggZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAkKCB0aGlzICkuY2hpbGRyZW4oICcucGFnZS10YWcnICkuc2xpY2UoIF9kZWZhdWx0cy5yZWN1cnJlbmNlU2VjdGlvbk51bWJlciApLnJlbW92ZSgpO1xuICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgICAgICQoIF9kZWZhdWx0cy5zZWN0aW9uVGFncyApLnNob3coKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLy8gaGlkZSBsb2FkZXJcbiAgICAgICAgICAgICAgICAkKCBfZGVmYXVsdHMubG9hZGVyICkuaGlkZSgpO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgfSApXG4gICAgICAgICAgICAgICAgICAgIC50aGVuKCBudWxsLCBmdW5jdGlvbiggZXJyb3IgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAkKCAnLmZhY2V0dGluZy1jb250ZW50JyApLmVtcHR5KCkuYXBwZW5kKCB2aWV3ZXIuaGVscGVyXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIC5yZW5kZXJBbGVydCggJ2FsZXJ0LWRhbmdlcicsICc8c3Ryb25nPlN0YXR1czogPC9zdHJvbmc+JyArIGVycm9yLnN0YXR1cyArICcgJyArIGVycm9yLnN0YXR1c1RleHQsIGZhbHNlICkgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGNvbnNvbGUuZXJyb3IoICdFUlJPUjogdmlld2VyLm5lckZhY2V0dGluZy5pbml0IC0gJywgZXJyb3IgKTtcbiAgICAgICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgXG4gICAgICAgIH0gKS50aGVuKCBudWxsLCBmdW5jdGlvbiggZXJyb3IgKSB7XG4gICAgICAgICAgICAkKCAnLmZhY2V0dGluZy1jb250ZW50JyApLmVtcHR5KCkuYXBwZW5kKCB2aWV3ZXIuaGVscGVyLnJlbmRlckFsZXJ0KCAnYWxlcnQtZGFuZ2VyJywgJzxzdHJvbmc+U3RhdHVzOiA8L3N0cm9uZz4nICsgZXJyb3Iuc3RhdHVzICsgJyAnICsgZXJyb3Iuc3RhdHVzVGV4dCwgZmFsc2UgKSApO1xuICAgICAgICAgICAgY29uc29sZS5lcnJvciggJ0VSUk9SOiB2aWV3ZXIubmVyRmFjZXR0aW5nLmluaXQgLSAnLCBlcnJvciApO1xuICAgICAgICB9ICk7XG4gICAgfVxuICAgIFxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB3aGljaCByZW5kZXJzIHRoZSB0YWdzIGluIHRoZSBzZWN0aW9uIGFyZWEuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfcmVuZGVyU2VjdGlvblRhZ3NcbiAgICAgKiBAcGFyYW0ge09iamVjdH0gZGF0YSBBIEpTT04tT2JqZWN0LlxuICAgICAqIEByZXR1cm5zIHtTdGluZ30gQSBIVE1MLVN0cmluZyB3aGljaCByZW5kZXJzIHRoZSB0YWcgc2VjdGlvbi5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfcmVuZGVyU2VjdGlvblRhZ3MoIGRhdGEgKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICctLS0tLS0tLS0tIF9yZW5kZXJTZWN0aW9uVGFncygpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19yZW5kZXJTZWN0aW9uVGFnczogZGF0YSAtICcsIGRhdGEgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgX2h0bWwgPSAnJztcbiAgICAgICAgLy8gcmVuZGVyIHRhZ3NcbiAgICAgICAgJC5lYWNoKCBkYXRhLnBhZ2VzLCBmdW5jdGlvbiggcCwgcGFnZSApIHtcbiAgICAgICAgICAgIGlmICggcGFnZS50YWdzLmxlbmd0aCA9PT0gMCB8fCBwYWdlLnRhZ3MubGVuZ3RoID09PSAndW5kZWZpbmVkJyApIHtcbiAgICAgICAgICAgICAgICBfaHRtbCArPSAnJztcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICQuZWFjaCggcGFnZS50YWdzLCBmdW5jdGlvbiggdCwgdGFnICkge1xuICAgICAgICAgICAgICAgICAgICBpZiAoIF9kZWZhdWx0cy5jdXJyZW50UGFnZSA9PT0gJ25lcmZhY2V0dGluZycgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAoIHRhZy5jb3VudGVyIDwgMTAgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgX2h0bWwgKz0gJzxzcGFuIGNsYXNzPVwicGFnZS10YWcgJyArIHRhZy50eXBlICsgJ1wiIHN0eWxlPVwiZm9udC1zaXplOiAxLicgKyB0YWcuY291bnRlciArICdyZW07XCI+JyArIHRhZy52YWx1ZSArICc8L3NwYW4+JztcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIF9odG1sICs9ICc8c3BhbiBjbGFzcz1cInBhZ2UtdGFnICcgKyB0YWcudHlwZSArICdcIiBzdHlsZT1cImZvbnQtc2l6ZTogMnJlbTtcIj4nICsgdGFnLnZhbHVlICsgJzwvc3Bhbj4nO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKCB0YWcuY291bnRlciA8IDEwICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIF9odG1sICs9ICc8c3BhbiBjbGFzcz1cInBhZ2UtdGFnICcgKyB0YWcudHlwZSArICdcIiBzdHlsZT1cImZvbnQtc2l6ZTogMScgKyB0YWcuY291bnRlciArICdweDtcIj4nICsgdGFnLnZhbHVlICsgJzwvc3Bhbj4nO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgX2h0bWwgKz0gJzxzcGFuIGNsYXNzPVwicGFnZS10YWcgJyArIHRhZy50eXBlICsgJ1wiIHN0eWxlPVwiZm9udC1zaXplOiAxOXB4O1wiPicgKyB0YWcudmFsdWUgKyAnPC9zcGFuPic7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0gKTtcbiAgICAgICAgXG4gICAgICAgIHJldHVybiBfaHRtbDtcbiAgICB9XG4gICAgXG4gICAgLyoqXG4gICAgICogTWV0aG9kIHdoaWNoIHJlbmRlcnMgYSBzcGFuIHNob3dpbmcgdGhlIGN1cnJlbnQgcGFnZSBzZWN0aW9uLlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX3JlbmRlclBhZ2VCdWJibGVcbiAgICAgKiBAcGFyYW0ge051bWJlcn0gcGFnZSBUaGUgY3VycmVudCBwYWdlbnVtYmVyLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9yZW5kZXJQYWdlQnViYmxlKCBwYWdlICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfcmVuZGVyUGFnZUJ1YmJsZSgpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19yZW5kZXJQYWdlQnViYmxlOiBwYWdlIC0gJywgcGFnZSApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgcGFnZUJ1YmJsZSA9ICcnO1xuICAgICAgICBcbiAgICAgICAgc3dpdGNoICggX2N1cnJlbnROZXJQYWdlUmFuZ2UgKSB7XG4gICAgICAgICAgICBjYXNlICc1JzpcbiAgICAgICAgICAgICAgICBfc3RhcnQgPSBwYWdlIC0gMjtcbiAgICAgICAgICAgICAgICBfZW5kID0gcGFnZSArIDM7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgd2hpbGUgKCBfc3RhcnQgPCAxICkge1xuICAgICAgICAgICAgICAgICAgICBfc3RhcnQrKztcbiAgICAgICAgICAgICAgICAgICAgX2VuZCsrO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB3aGlsZSAoIF9lbmQgPiBfcGFnZUNvdW50ICkge1xuICAgICAgICAgICAgICAgICAgICBfc3RhcnQtLTtcbiAgICAgICAgICAgICAgICAgICAgX2VuZC0tO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICBwYWdlQnViYmxlICs9ICc8c3BhbiBjbGFzcz1cInBhZ2UtYnViYmxlXCI+JyArIF9zdGFydCArICctJyArIF9lbmQgKyAnPC9zcGFuPic7XG4gICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICBjYXNlICcxMCc6XG4gICAgICAgICAgICAgICAgX3N0YXJ0ID0gcGFnZSAtIDU7XG4gICAgICAgICAgICAgICAgX2VuZCA9IHBhZ2UgKyA1O1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIHdoaWxlICggX3N0YXJ0IDwgMSApIHtcbiAgICAgICAgICAgICAgICAgICAgX3N0YXJ0Kys7XG4gICAgICAgICAgICAgICAgICAgIF9lbmQrKztcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgd2hpbGUgKCBfZW5kID4gX3BhZ2VDb3VudCApIHtcbiAgICAgICAgICAgICAgICAgICAgX3N0YXJ0LS07XG4gICAgICAgICAgICAgICAgICAgIF9lbmQtLTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgcGFnZUJ1YmJsZSArPSAnPHNwYW4gY2xhc3M9XCJwYWdlLWJ1YmJsZVwiPicgKyBfc3RhcnQgKyAnLScgKyBfZW5kICsgJzwvc3Bhbj4nO1xuICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgY2FzZSAnNTAnOlxuICAgICAgICAgICAgICAgIF9zdGFydCA9IHBhZ2UgLSAyNTtcbiAgICAgICAgICAgICAgICBfZW5kID0gcGFnZSArIDI1O1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIHdoaWxlICggX3N0YXJ0IDwgMSApIHtcbiAgICAgICAgICAgICAgICAgICAgX3N0YXJ0Kys7XG4gICAgICAgICAgICAgICAgICAgIF9lbmQrKztcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgd2hpbGUgKCBfZW5kID4gX3BhZ2VDb3VudCApIHtcbiAgICAgICAgICAgICAgICAgICAgX3N0YXJ0LS07XG4gICAgICAgICAgICAgICAgICAgIF9lbmQtLTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgcGFnZUJ1YmJsZSArPSAnPHNwYW4gY2xhc3M9XCJwYWdlLWJ1YmJsZVwiPicgKyBfc3RhcnQgKyAnLScgKyBfZW5kICsgJzwvc3Bhbj4nO1xuICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgY2FzZSAnMTAwJzpcbiAgICAgICAgICAgICAgICBfc3RhcnQgPSBwYWdlIC0gNTA7XG4gICAgICAgICAgICAgICAgX2VuZCA9IHBhZ2UgKyA1MDtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICB3aGlsZSAoIF9zdGFydCA8IDEgKSB7XG4gICAgICAgICAgICAgICAgICAgIF9zdGFydCsrO1xuICAgICAgICAgICAgICAgICAgICBfZW5kKys7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIHdoaWxlICggX2VuZCA+IF9wYWdlQ291bnQgKSB7XG4gICAgICAgICAgICAgICAgICAgIF9zdGFydC0tO1xuICAgICAgICAgICAgICAgICAgICBfZW5kLS07XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIHBhZ2VCdWJibGUgKz0gJzxzcGFuIGNsYXNzPVwicGFnZS1idWJibGVcIj4nICsgX3N0YXJ0ICsgJy0nICsgX2VuZCArICc8L3NwYW4+JztcbiAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgJCggJyNzbGlkZXJWZXJ0aWNhbCAudWktc2xpZGVyLWhhbmRsZScgKS5odG1sKCBwYWdlQnViYmxlICk7XG4gICAgfVxuICAgIFxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB3aGljaCByZXR1cm5zIHRoZSBwYWdlIGNvdW50IG9mIHRoZSBjdXJyZW50IHdvcmsuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfZ2V0UGFnZUNvdW50XG4gICAgICogQHBhcmFtIHtPYmplY3R9IHdvcmsgVGhlIGN1cnJlbnQgd29yIG9iamVjdC5cbiAgICAgKiBAcmV0dXJucyB7TnVtYmVyfSBUaGUgcGFnZSBjb3VudCBvZiB0aGUgY3VycmVudCB3b3JrLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9nZXRQYWdlQ291bnQoIHdvcmsgKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICctLS0tLS0tLS0tIF9nZXRQYWdlQ291bnQoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfZ2V0UGFnZUNvdW50OiB3b3JrIC0gJywgd29yayApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICByZXR1cm4gd29yay5wYWdlcy5sZW5ndGg7XG4gICAgfVxuICAgIFxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB3aGljaCByZXNldHMgYWxsIGZhY2V0dGluZyBpY29ucyB0byBkZWZhdWx0XG4gICAgICogXG4gICAgICogQG1ldGhvZCBfcmVzZXRGYWNldHRpbmdJY29uc1xuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9yZXNldEZhY2V0dGluZ0ljb25zKCkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfcmVzZXRGYWNldHRpbmdJY29ucygpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgICQoICcuZmFjZXR0aW5nLXRyaWdnZXInICkuYWRkQ2xhc3MoICdhY3RpdmUnICk7XG4gICAgICAgICQoICcucmVzZXQtZmlsdGVyJyApLmhpZGUoKTtcbiAgICB9XG4gICAgXG4gICAgLyoqXG4gICAgICogTWV0aG9kIHdoaWNoIHJlbW92ZXMgYWxsIHNldCBsb2NhbCBzdG9yYWdlIHZhbHVlcy5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9jbGVhblVwTG9jYWxTdG9yYWdlXG4gICAgICovXG4gICAgZnVuY3Rpb24gX2NsZWFuVXBMb2NhbFN0b3JhZ2UoKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICctLS0tLS0tLS0tIF9jbGVhblVwTG9jYWxTdG9yYWdlKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgbG9jYWxTdG9yYWdlLnJlbW92ZUl0ZW0oICdjdXJyZW50TmVyUGFnZVJhbmdlJyApO1xuICAgICAgICBsb2NhbFN0b3JhZ2UucmVtb3ZlSXRlbSggJ2N1cnJlbnROZXJUeXBlJyApO1xuICAgIH1cbiAgICBcbiAgICAvKipcbiAgICAgKiBBUEktQ2FsbHNcbiAgICAgKi9cbiAgICAvLyBnZXQgYWxsIHRhZ3MgZnJvbSBhbGwgcGFnZXM6IC9yZXN0L25lci90YWdzL3twaX0vXG4gICAgZnVuY3Rpb24gX2dldEFsbFRhZ3MoKSB7XG4gICAgICAgIHJldHVybiBfZGVmYXVsdHMuYmFzZVVybCArIF9kZWZhdWx0cy5hcGlVcmwgKyBfZGVmYXVsdHMud29ya0lkO1xuICAgIH1cbiAgICBcbiAgICAvLyBnZXQgYWxsIHRhZ3Mgb2YgYSByYW5nZTogL3ZpZXdlci9yZXN0L25lci90YWdzL3Jhbmdlcy97cmFuZ2V9L3t0eXBlfS97cGl9L1xuICAgIGZ1bmN0aW9uIF9nZXRBbGxUYWdzT2ZBUmFuZ2UoIHJhbmdlLCB0eXBlICkge1xuICAgICAgICByZXR1cm4gX2RlZmF1bHRzLmJhc2VVcmwgKyBfZGVmYXVsdHMuYXBpVXJsICsgJ3Jhbmdlcy8nICsgcmFuZ2UgKyAnLycgKyB0eXBlICsgJy8nICsgX2RlZmF1bHRzLndvcmtJZCArICcvJztcbiAgICB9XG4gICAgXG4gICAgLy8gZ2V0IGFsbCB0YWdzIHNvcnRlZCBvZiB0eXBlOiAvcmVzdC9uZXIvdGFncy97dHlwZX0ve3BpfS9cbiAgICBmdW5jdGlvbiBfZ2V0QWxsVGFnc09mQVR5cGUoIHR5cGUgKSB7XG4gICAgICAgIHJldHVybiBfZGVmYXVsdHMuYmFzZVVybCArIF9kZWZhdWx0cy5hcGlVcmwgKyB0eXBlICsgJy8nICsgX2RlZmF1bHRzLndvcmtJZCArICcvJztcbiAgICB9XG4gICAgXG4gICAgLy8gZ2V0IGFsbCB0YWdzIHNvcnRlZCBvZiByZWN1cnJlbmNlIChhc2MvZGVzYyk6XG4gICAgLy8gL3Jlc3QvbmVyL3RhZ3MvcmVjdXJyZW5jZS97dHlwZX0ve29yZGVyfS97cGl9L1xuICAgIGZ1bmN0aW9uIF9nZXRBbGxUYWdzT2ZSZWN1cnJlbmNlKCB0eXBlLCBvcmRlciApIHtcbiAgICAgICAgaWYgKCB0eXBlID09PSAnLScgKSB7XG4gICAgICAgICAgICByZXR1cm4gX2RlZmF1bHRzLmJhc2VVcmwgKyBfZGVmYXVsdHMuYXBpVXJsICsgJ3JlY3VycmVuY2UvLS8nICsgb3JkZXIgKyAnLycgKyBfZGVmYXVsdHMud29ya0lkICsgJy8nO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgcmV0dXJuIF9kZWZhdWx0cy5iYXNlVXJsICsgX2RlZmF1bHRzLmFwaVVybCArICdyZWN1cnJlbmNlLycgKyB0eXBlICsgJy8nICsgb3JkZXIgKyAnLycgKyBfZGVmYXVsdHMud29ya0lkICsgJy8nO1xuICAgICAgICB9XG4gICAgfVxuICAgIFxuICAgIC8vIGdldCBhbGwgdGFncyBzb3J0ZWQgb2YgcGFnZSBzZWN0aW9uOiAvcmVzdC9uZXIvdGFncy97c3RhcnR9L3tlbmR9L3twaX0vXG4gICAgZnVuY3Rpb24gX2dldEFsbFRhZ3NPZlBhZ2VTZWN0aW9uKCBzdGFydCwgZW5kLCB0eXBlICkge1xuICAgICAgICByZXR1cm4gX2RlZmF1bHRzLmJhc2VVcmwgKyBfZGVmYXVsdHMuYXBpVXJsICsgc3RhcnQgKyAnLycgKyBlbmQgKyAnLycgKyB0eXBlICsgJy8nICsgX2RlZmF1bHRzLndvcmtJZCArICcvJztcbiAgICB9XG4gICAgXG4gICAgcmV0dXJuIHZpZXdlcjtcbiAgICBcbn0gKSggdmlld2VySlMgfHwge30sIGpRdWVyeSApO1xuIiwiLyoqXG4gKiBUaGlzIGZpbGUgaXMgcGFydCBvZiB0aGUgR29vYmkgdmlld2VyIC0gYSBjb250ZW50IHByZXNlbnRhdGlvbiBhbmQgbWFuYWdlbWVudFxuICogYXBwbGljYXRpb24gZm9yIGRpZ2l0aXplZCBvYmplY3RzLlxuICogXG4gKiBWaXNpdCB0aGVzZSB3ZWJzaXRlcyBmb3IgbW9yZSBpbmZvcm1hdGlvbi4gLSBodHRwOi8vd3d3LmludHJhbmRhLmNvbSAtXG4gKiBodHRwOi8vZGlnaXZlcnNvLmNvbVxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZnJlZSBzb2Z0d2FyZTsgeW91IGNhbiByZWRpc3RyaWJ1dGUgaXQgYW5kL29yIG1vZGlmeSBpdCB1bmRlciB0aGUgdGVybXNcbiAqIG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhcyBwdWJsaXNoZWQgYnkgdGhlIEZyZWUgU29mdHdhcmUgRm91bmRhdGlvbjsgZWl0aGVyXG4gKiB2ZXJzaW9uIDIgb2YgdGhlIExpY2Vuc2UsIG9yIChhdCB5b3VyIG9wdGlvbikgYW55IGxhdGVyIHZlcnNpb24uXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBkaXN0cmlidXRlZCBpbiB0aGUgaG9wZSB0aGF0IGl0IHdpbGwgYmUgdXNlZnVsLCBidXQgV0lUSE9VVCBBTllcbiAqIFdBUlJBTlRZOyB3aXRob3V0IGV2ZW4gdGhlIGltcGxpZWQgd2FycmFudHkgb2YgTUVSQ0hBTlRBQklMSVRZIG9yIEZJVE5FU1MgRk9SIEFcbiAqIFBBUlRJQ1VMQVIgUFVSUE9TRS4gU2VlIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBmb3IgbW9yZSBkZXRhaWxzLlxuICogXG4gKiBZb3Ugc2hvdWxkIGhhdmUgcmVjZWl2ZWQgYSBjb3B5IG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhbG9uZyB3aXRoIHRoaXNcbiAqIHByb2dyYW0uIElmIG5vdCwgc2VlIDxodHRwOi8vd3d3LmdudS5vcmcvbGljZW5zZXMvPi5cbiAqIFxuICogTW9kdWxlIHRvIHJlbmRlciBORVItUG9wb3ZlcnMgaW4gZnVsbHRleHQgcGFnZXMuXG4gKiBcbiAqIEB2ZXJzaW9uIDMuMi4wXG4gKiBAbW9kdWxlIHZpZXdlckpTLm5lckZ1bGx0ZXh0XG4gKiBAcmVxdWlyZXMgalF1ZXJ5XG4gKiBcbiAqL1xudmFyIHZpZXdlckpTID0gKCBmdW5jdGlvbiggdmlld2VyICkge1xuICAgICd1c2Ugc3RyaWN0JztcbiAgICBcbiAgICAvLyBkZWZpbmUgdmFyaWFibGVzXG4gICAgdmFyIF9kZWJ1ZyA9IGZhbHNlO1xuICAgIHZhciBfZGVmYXVsdHMgPSB7XG4gICAgICAgIHBhdGg6IG51bGwsXG4gICAgICAgIGxhbmc6IHt9XG4gICAgfTtcbiAgICB2YXIgX2NvbnRleHRQYXRoID0gbnVsbDtcbiAgICB2YXIgX2xhbmcgPSBudWxsO1xuICAgIFxuICAgIHZpZXdlci5uZXJGdWxsdGV4dCA9IHtcbiAgICAgICAgLyoqXG4gICAgICAgICAqIE1ldGhvZCB3aGljaCBpbml0aWFsaXplcyB0aGUgTkVSIFBvcG92ZXIgbWV0aG9kcy5cbiAgICAgICAgICogXG4gICAgICAgICAqIEBtZXRob2QgaW5pdFxuICAgICAgICAgKiBAcGFyYW0ge09iamVjdH0gY29uZmlnIEFuIGNvbmZpZyBvYmplY3Qgd2hpY2ggb3ZlcndyaXRlcyB0aGUgZGVmYXVsdHMuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcucGF0aCBUaGUgcm9vdHBhdGggb2YgdGhlIGFwcGxpY2F0aW9uLlxuICAgICAgICAgKiBAZXhhbXBsZVxuICAgICAgICAgKiBcbiAgICAgICAgICogPHByZT5cbiAgICAgICAgICogdmFyIG5lckNvbmZpZyA9IHtcbiAgICAgICAgICogICAgIHBhdGg6ICcje3JlcXVlc3QuY29udGV4dFBhdGh9J1xuICAgICAgICAgKiB9O1xuICAgICAgICAgKiBcbiAgICAgICAgICogdmlld2VySlMubmVyRnVsbHRleHQuaW5pdCggbmVyQ29uZmlnICk7XG4gICAgICAgICAqIDwvcHJlPlxuICAgICAgICAgKi9cbiAgICAgICAgaW5pdDogZnVuY3Rpb24oIGNvbmZpZyApIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLm5lckZ1bGx0ZXh0LmluaXQnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICcjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIubmVyRnVsbHRleHQuaW5pdDogY29uZmlnIC0gJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCBjb25maWcgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgJC5leHRlbmQoIHRydWUsIF9kZWZhdWx0cywgY29uZmlnICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIF9pbml0TmVyUG9wb3ZlciggX2RlZmF1bHRzLnBhdGggKTtcbiAgICAgICAgfVxuICAgIH07XG4gICAgXG4gICAgLyoqXG4gICAgICogTWV0aG9kIHdoaWNoIGZldGNoZXMgZGF0YSBmcm9tIHRoZSBBUEkgYW5kIHJldHVybnMgYW4gSlNPTiBvYmplY3QuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfZ2V0UmVtb3RlRGF0YVxuICAgICAqIEBwYXJhbSB7U3RyaW5nfSB0YXJnZXQgVGhlIEFQSSBjYWxsIFVSTC5cbiAgICAgKiBAcmV0dXJucyB7T2JqZWN0fSBUaGUgSlNPTiBvYmplY3Qgd2l0aCB0aGUgQVBJIGRhdGEuXG4gICAgICogXG4gICAgICovXG4gICAgZnVuY3Rpb24gX2dldFJlbW90ZURhdGEoIHRhcmdldCApIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci5uZXJGdWxsdGV4dCBfZ2V0UmVtb3RlRGF0YTogdGFyZ2V0IC0gJyApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coIHRhcmdldCApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICAvLyBzaG93IHByZWxvYWRlciBmb3IgY3VycmVudCBlbGVtZW50XG4gICAgICAgICQoICcubmVyLWRldGFpbC1sb2FkZXInLCB0YXJnZXQgKS5jc3MoIHtcbiAgICAgICAgICAgIGRpc3BsYXk6ICdpbmxpbmUtYmxvY2snXG4gICAgICAgIH0gKTtcbiAgICAgICAgXG4gICAgICAgIC8vIEFKQVggY2FsbFxuICAgICAgICB2YXIgZGF0YSA9ICQuYWpheCgge1xuICAgICAgICAgICAgdXJsOiBkZWNvZGVVUkkoICQoIHRhcmdldCApLmF0dHIoICdkYXRhLXJlbW90ZWNvbnRlbnQnICkgKSxcbiAgICAgICAgICAgIHR5cGU6ICdQT1NUJyxcbiAgICAgICAgICAgIGRhdGFUeXBlOiAnSlNPTicsXG4gICAgICAgICAgICBhc3luYzogZmFsc2UsXG4gICAgICAgICAgICBjb21wbGV0ZTogZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgJCggJy5uZXItZGV0YWlsLWxvYWRlcicgKS5oaWRlKCk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0gKS5yZXNwb25zZVRleHQ7XG4gICAgICAgIFxuICAgICAgICByZXR1cm4gZGF0YTtcbiAgICB9XG4gICAgXG4gICAgLyoqXG4gICAgICogTWV0aG9kIHdoaWNoIGluaXRpYWxpemVzIHRoZSBldmVudHMgZm9yIHRoZSBORVItUG9wb3ZlcnMuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfaW5pdE5lclBvcG92ZXJcbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gcGF0aCBUaGUgcm9vdCBwYXRoIG9mIHRoZSBhcHBsaWNhdGlvbi5cbiAgICAgKiBcbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfaW5pdE5lclBvcG92ZXIoIHBhdGggKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIubmVyRnVsbHRleHQgX2luaXROZXJQb3BvdmVyOiBwYXRoIC0gJyArIHBhdGggKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgdmFyIGRhdGEsIHBvc2l0aW9uLCB0aXRsZSwgdHJpZ2dlckNvb3JkcywgdGV4dEJveCwgdGV4dEJveFBvc2l0aW9uLCB0ZXh0Qm94Q29vcmRzO1xuICAgICAgICBcbiAgICAgICAgJCggJy5uZXItdHJpZ2dlcicgKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAkKCAnYm9keScgKS5maW5kKCAnLm5lci1wb3BvdmVyLXBvaW50ZXInICkuaGlkZSgpO1xuICAgICAgICAgICAgJCggJ2JvZHknICkuZmluZCggJy5uZXItcG9wb3ZlcicgKS5yZW1vdmUoKTtcbiAgICAgICAgICAgIGRhdGEgPSBfZ2V0UmVtb3RlRGF0YSggJCggdGhpcyApICk7XG4gICAgICAgICAgICBwb3NpdGlvbiA9ICQoIHRoaXMgKS5wb3NpdGlvbigpO1xuICAgICAgICAgICAgdHJpZ2dlckNvb3JkcyA9IHtcbiAgICAgICAgICAgICAgICB0b3A6IHBvc2l0aW9uLnRvcCxcbiAgICAgICAgICAgICAgICBsZWZ0OiBwb3NpdGlvbi5sZWZ0LFxuICAgICAgICAgICAgICAgIHdpZHRoOiAkKCB0aGlzICkub3V0ZXJXaWR0aCgpXG4gICAgICAgICAgICB9O1xuICAgICAgICAgICAgdGV4dEJveCA9ICQoICcjdmlld19mdWxsdGV4dF93cmFwcCcgKTtcbiAgICAgICAgICAgIHRleHRCb3hQb3NpdGlvbiA9IHRleHRCb3gucG9zaXRpb24oKTtcbiAgICAgICAgICAgIHRleHRCb3hDb29yZHMgPSB7XG4gICAgICAgICAgICAgICAgdG9wOiB0ZXh0Qm94UG9zaXRpb24udG9wLFxuICAgICAgICAgICAgICAgIGxlZnQ6IDAsXG4gICAgICAgICAgICAgICAgcmlnaHQ6IHRleHRCb3hQb3NpdGlvbi5sZWZ0ICsgdGV4dEJveC5vdXRlcldpZHRoKClcbiAgICAgICAgICAgIH07XG4gICAgICAgICAgICB0aXRsZSA9ICQoIHRoaXMgKS5hdHRyKCAndGl0bGUnICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHRleHRCb3guYXBwZW5kKCBfcmVuZGVyTmVyUG9wb3ZlciggZGF0YSwgX2NhbGN1bGF0ZU5lclBvcG92ZXJQb3NpdGlvbiggdHJpZ2dlckNvb3JkcywgdGV4dEJveENvb3JkcyApLCB0aXRsZSwgcGF0aCApICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmICggJCggJy5uZXItcG9wb3ZlcicgKSApIHtcbiAgICAgICAgICAgICAgICAkKCB0aGlzICkuZmluZCggJy5uZXItcG9wb3Zlci1wb2ludGVyJyApLnNob3coKTtcbiAgICAgICAgICAgICAgICBfcmVtb3ZlTmVyUG9wb3ZlcigpO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICQoICcubmVyLWRldGFpbC10cmlnZ2VyJyApLm9uKCAnY2xpY2snLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgZGF0YSA9IF9nZXRSZW1vdGVEYXRhKCAkKCB0aGlzICkgKTtcbiAgICAgICAgICAgICAgICAgICAgdGl0bGUgPSAkKCB0aGlzICkuYXR0ciggJ3RpdGxlJyApO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgJCggdGhpcyApLnBhcmVudCgpLm5leHQoICcubmVyLXBvcG92ZXItZGV0YWlsJyApLmh0bWwoIF9yZW5kZXJOZXJQb3BvdmVyRGV0YWlsKCBkYXRhLCB0aXRsZSApICk7XG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgfVxuICAgICAgICB9ICk7XG4gICAgfVxuICAgIFxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB3aGljaCByZW5kZXJzIGEgcG9wb3ZlciB0byB0aGUgRE9NLlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX3JlbmRlck5lclBvcG92ZXJcbiAgICAgKiBAcGFyYW0ge09iamVjdH0gZGF0YSBUaGUgSlNPTiBvYmplY3QgZnJvbSB0aGUgQVBJLlxuICAgICAqIEBwYXJhbSB7T2JqZWN0fSBwb3NpdGlvbiBBIGpRdWVyeSBvYmplY3QgaW5jbHVkaW5nIHRoZSBwb3NpdGlvbiBvZiB0aGUgY2xpY2tlZFxuICAgICAqIHRyaWdnZXIuXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IHRpdGxlIFRoZSB2YWx1ZSBvZiB0aGUgdGl0bGUgYXR0cmlidXRlIGZyb20gdGhlIGNsaWNrZWQgdHJpZ2dlci5cbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gcGF0aCBUaGUgcm9vdCBwYXRoIG9mIHRoZSBhcHBsaWNhdGlvbi5cbiAgICAgKiBAcmV0dXJucyB7U3RyaW5nfSBUaGUgSFRNTCBzdHJpbmcgd2hpY2ggcmVuZGVycyB0aGUgcG9wb3Zlci5cbiAgICAgKiBcbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfcmVuZGVyTmVyUG9wb3ZlciggZGF0YSwgcG9zaXRpb24sIHRpdGxlLCBwYXRoICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLm5lckZ1bGx0ZXh0IF9yZW5kZXJOZXJQb3BvdmVyOiBkYXRhIC0gJyApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coIGRhdGEgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLm5lckZ1bGx0ZXh0IF9yZW5kZXJOZXJQb3BvdmVyOiBwb3NpdGlvbiAtICcgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCBwb3NpdGlvbiApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIubmVyRnVsbHRleHQgX3JlbmRlck5lclBvcG92ZXI6IHRpdGxlIC0gJyArIHRpdGxlICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci5uZXJGdWxsdGV4dCBfcmVuZGVyTmVyUG9wb3ZlcjogcGF0aCAtICcgKyBwYXRoICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIHZhciBwb3NpdGlvblRvcCA9IHBvc2l0aW9uLnRvcDtcbiAgICAgICAgdmFyIHBvc2l0aW9uTGVmdCA9IHBvc2l0aW9uLmxlZnQ7XG4gICAgICAgIHZhciBwb3BvdmVyID0gJyc7XG4gICAgICAgIFxuICAgICAgICBwb3BvdmVyICs9ICc8ZGl2IGNsYXNzPVwibmVyLXBvcG92ZXJcIiBzdHlsZT1cInRvcDonICsgcG9zaXRpb25Ub3AgKyAncHg7IGxlZnQ6JyArIHBvc2l0aW9uTGVmdCArICdweFwiPic7XG4gICAgICAgIHBvcG92ZXIgKz0gJzxkaXYgY2xhc3M9XCJuZXItcG9wb3Zlci1jbG9zZVwiIHRpdGxlPVwiRmVuc3RlciBzY2hsaWUmc3psaWc7ZW5cIj4mdGltZXM7PC9kaXY+JztcbiAgICAgICAgcG9wb3ZlciArPSAnPGRpdiBjbGFzcz1cIm5lci1wb3BvdmVyLWhlYWRlclwiPjxoND4nICsgdGl0bGUgKyAnPC9oND48L2Rpdj4nO1xuICAgICAgICBwb3BvdmVyICs9ICc8ZGl2IGNsYXNzPVwibmVyLXBvcG92ZXItYm9keVwiPic7XG4gICAgICAgIHBvcG92ZXIgKz0gJzxkbCBjbGFzcz1cImRsLWhvcml6b250YWxcIj4nO1xuICAgICAgICAkLmVhY2goICQucGFyc2VKU09OKCBkYXRhICksIGZ1bmN0aW9uKCBpLCBvYmplY3QgKSB7XG4gICAgICAgICAgICAkLmVhY2goIG9iamVjdCwgZnVuY3Rpb24oIHByb3BlcnR5LCB2YWx1ZSApIHtcbiAgICAgICAgICAgICAgICBwb3BvdmVyICs9ICc8ZHQgdGl0bGU9XCInICsgcHJvcGVydHkgKyAnXCI+JyArIHByb3BlcnR5ICsgJzo8L2R0Pic7XG4gICAgICAgICAgICAgICAgdmFyIG9ialZhbHVlID0gJyc7XG4gICAgICAgICAgICAgICAgJC5lYWNoKCB2YWx1ZSwgZnVuY3Rpb24oIHAsIHYgKSB7XG4gICAgICAgICAgICAgICAgICAgIHZhciBpY29uID0gJyc7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICBzd2l0Y2ggKCBwcm9wZXJ0eSApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGNhc2UgJ0JlcnVmJzpcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBpY29uID0gJ2dseXBoaWNvbi1icmllZmNhc2UnO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICAgICAgY2FzZSAnVmVyd2FuZHRlIEJlZ3JpZmZlJzpcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBpY29uID0gJ2dseXBoaWNvbi1icmllZmNhc2UnO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICAgICAgY2FzZSAnU29obic6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgaWNvbiA9ICdnbHlwaGljb24tdXNlcic7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgICAgICAgICBjYXNlICdWYXRlcic6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgaWNvbiA9ICdnbHlwaGljb24tdXNlcic7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgICAgICAgICBjYXNlICdHZWJ1cnRzb3J0JzpcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBpY29uID0gJ2dseXBoaWNvbi1tYXAtbWFya2VyJztcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICAgICAgICAgIGNhc2UgJ1N0ZXJiZW9ydCc6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgaWNvbiA9ICdnbHlwaGljb24tbWFwLW1hcmtlcic7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgIGlmICggdi51cmwgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBvYmpWYWx1ZSArPSAnPHNwYW4gJztcbiAgICAgICAgICAgICAgICAgICAgICAgIG9ialZhbHVlICs9ICdjbGFzcz1cIm5lci1kZXRhaWwtdHJpZ2dlclwiICc7XG4gICAgICAgICAgICAgICAgICAgICAgICBvYmpWYWx1ZSArPSAndGl0bGU9XCInICsgdi50ZXh0ICsgJ1wiICc7XG4gICAgICAgICAgICAgICAgICAgICAgICBvYmpWYWx1ZSArPSAndGFiaW5kZXg9XCItMVwiJztcbiAgICAgICAgICAgICAgICAgICAgICAgIG9ialZhbHVlICs9ICdkYXRhLXJlbW90ZWNvbnRlbnQ9XCInICsgcGF0aCArICcvYXBpP2FjdGlvbj1ub3JtZGF0YSZ1cmw9JyArIHYudXJsICsgJ1wiPic7XG4gICAgICAgICAgICAgICAgICAgICAgICBvYmpWYWx1ZSArPSAnPHNwYW4gY2xhc3M9XCJnbHlwaGljb24gJyArIGljb24gKyAnXCI+PC9zcGFuPiZuYnNwOyc7XG4gICAgICAgICAgICAgICAgICAgICAgICBvYmpWYWx1ZSArPSB2LnRleHQ7XG4gICAgICAgICAgICAgICAgICAgICAgICBvYmpWYWx1ZSArPSAnPHNwYW4gY2xhc3M9XCJuZXItZGV0YWlsLWxvYWRlclwiPjwvc3Bhbj4nO1xuICAgICAgICAgICAgICAgICAgICAgICAgb2JqVmFsdWUgKz0gJzwvc3Bhbj4nO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKCBwcm9wZXJ0eSA9PT0gJ1VSSScgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgb2JqVmFsdWUgKz0gJzxhIGhyZWY9XCInICsgdi50ZXh0ICsgJ1wiIHRhcmdldD1cIl9ibGFua1wiPicgKyB2LnRleHQgKyAnPC9hPic7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBvYmpWYWx1ZSArPSB2LnRleHQ7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgIG9ialZhbHVlICs9ICc8YnIgLz4nO1xuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICBwb3BvdmVyICs9ICc8ZGQ+JyArIG9ialZhbHVlICsgJzwvZGQ+JztcbiAgICAgICAgICAgICAgICBwb3BvdmVyICs9ICc8ZGl2IGNsYXNzPVwibmVyLXBvcG92ZXItZGV0YWlsXCI+PC9kaXY+JztcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgfSApO1xuICAgICAgICBwb3BvdmVyICs9ICc8L2RsPic7XG4gICAgICAgIHBvcG92ZXIgKz0gJzwvZGl2Pic7XG4gICAgICAgIHBvcG92ZXIgKz0gJzwvZGl2Pic7XG4gICAgICAgIFxuICAgICAgICByZXR1cm4gcG9wb3ZlcjtcbiAgICB9XG4gICAgXG4gICAgLyoqXG4gICAgICogTWV0aG9kIHdoaWNoIHJlbmRlcnMgZGV0YWlsIGluZm9ybWF0aW9uIGludG8gdGhlIHBvcG92ZXIuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfcmVuZGVyTmVyUG9wb3ZlckRldGFpbFxuICAgICAqIEBwYXJhbSB7T2JqZWN0fSBkYXRhIFRoZSBKU09OIG9iamVjdCBmcm9tIHRoZSBBUEkuXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IHRpdGxlIFRoZSB2YWx1ZSBvZiB0aGUgdGl0bGUgYXR0cmlidXRlIGZyb20gdGhlIGNsaWNrZWQgdHJpZ2dlci5cbiAgICAgKiBAcmV0dXJucyB7U3RyaW5nfSBUaGUgSFRNTCBzdHJpbmcgd2hpY2ggcmVuZGVycyB0aGUgZGV0YWlscy5cbiAgICAgKiBcbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfcmVuZGVyTmVyUG9wb3ZlckRldGFpbCggZGF0YSwgdGl0bGUgKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIubmVyRnVsbHRleHQgX3JlbmRlck5lclBvcG92ZXJEZXRhaWw6IGRhdGEgLSAnICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggZGF0YSApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIubmVyRnVsbHRleHQgX3JlbmRlck5lclBvcG92ZXJEZXRhaWw6IHRpdGxlIC0gJyArIHRpdGxlICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIHZhciBwb3BvdmVyRGV0YWlsID0gJyc7XG4gICAgICAgIFxuICAgICAgICBwb3BvdmVyRGV0YWlsICs9ICc8ZGl2IGNsYXNzPVwibmVyLXBvcG92ZXItZGV0YWlsXCI+JztcbiAgICAgICAgcG9wb3ZlckRldGFpbCArPSAnPGRpdiBjbGFzcz1cIm5lci1wb3BvdmVyLWRldGFpbC1oZWFkZXJcIj48aDQ+JyArIHRpdGxlICsgJzwvaDQ+PC9kaXY+JztcbiAgICAgICAgcG9wb3ZlckRldGFpbCArPSAnPGRpdiBjbGFzcz1cIm5lci1wb3BvdmVyLWRldGFpbC1ib2R5XCI+JztcbiAgICAgICAgcG9wb3ZlckRldGFpbCArPSAnPGRsIGNsYXNzPVwiZGwtaG9yaXpvbnRhbFwiPic7XG4gICAgICAgICQuZWFjaCggJC5wYXJzZUpTT04oIGRhdGEgKSwgZnVuY3Rpb24oIGksIG9iamVjdCApIHtcbiAgICAgICAgICAgICQuZWFjaCggb2JqZWN0LCBmdW5jdGlvbiggcHJvcGVydHksIHZhbHVlICkge1xuICAgICAgICAgICAgICAgIHBvcG92ZXJEZXRhaWwgKz0gJzxkdCB0aXRsZT1cIicgKyBwcm9wZXJ0eSArICdcIj4nICsgcHJvcGVydHkgKyAnOjwvZHQ+JztcbiAgICAgICAgICAgICAgICB2YXIgb2JqVmFsdWUgPSAnJztcbiAgICAgICAgICAgICAgICAkLmVhY2goIHZhbHVlLCBmdW5jdGlvbiggcCwgdiApIHtcbiAgICAgICAgICAgICAgICAgICAgaWYgKCBwcm9wZXJ0eSA9PT0gJ1VSSScgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBvYmpWYWx1ZSArPSAnPGEgaHJlZj1cIicgKyB2LnRleHQgKyAnXCIgdGFyZ2V0PVwiX2JsYW5rXCI+JyArIHYudGV4dCArICc8L2E+JztcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIG9ialZhbHVlICs9IHYudGV4dDtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgb2JqVmFsdWUgKz0gJzxiciAvPic7XG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgIHBvcG92ZXJEZXRhaWwgKz0gJzxkZD4nICsgb2JqVmFsdWUgKyAnPC9kZD4nO1xuICAgICAgICAgICAgICAgIHBvcG92ZXJEZXRhaWwgKz0gJzxkaXYgY2xhc3M9XCJuZXItcG9wb3Zlci1kZXRhaWxcIj48L2Rpdj4nO1xuICAgICAgICAgICAgfSApO1xuICAgICAgICB9ICk7XG4gICAgICAgIHBvcG92ZXJEZXRhaWwgKz0gJzwvZGw+JztcbiAgICAgICAgcG9wb3ZlckRldGFpbCArPSAnPC9kaXY+JztcbiAgICAgICAgcG9wb3ZlckRldGFpbCArPSAnPC9kaXY+JztcbiAgICAgICAgXG4gICAgICAgIHJldHVybiBwb3BvdmVyRGV0YWlsO1xuICAgIH1cbiAgICBcbiAgICAvKipcbiAgICAgKiBNZXRob2Qgd2hpY2ggY2FsY3VsYXRlcyB0aGUgcG9zaXRpb24gb2YgdGhlIHBvcG92ZXIgaW4gdGhlIERPTS5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9jYWxjdWxhdGVOZXJQb3BvdmVyUG9zaXRpb25cbiAgICAgKiBAcGFyYW0ge09iamVjdH0gdHJpZ2dlckNvb3JkcyBBIGpRdWVyeSBvYmplY3QgaW5jbHVkaW5nIHRoZSBwb3NpdGlvbiBvZiB0aGUgY2xpY2tlZFxuICAgICAqIHRyaWdnZXIuXG4gICAgICogQHBhcmFtIHtPYmplY3R9IHRleHRCb3hDb29yZHMgQSBqUXVlcnkgb2JqZWN0IGluY2x1ZGluZyB0aGUgcG9zaXRpb24gb2YgdGhlIHBhcmVudFxuICAgICAqIERJVi5cbiAgICAgKiBAcmV0dXJucyB7T2JqZWN0fSBBbiBvYmplY3Qgd2hpY2ggaW5jbHVkZXMgdGhlIHBvc2l0aW9uIG9mIHRoZSBwb3BvdmVyLlxuICAgICAqIFxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9jYWxjdWxhdGVOZXJQb3BvdmVyUG9zaXRpb24oIHRyaWdnZXJDb29yZHMsIHRleHRCb3hDb29yZHMgKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIubmVyRnVsbHRleHQgX2NhbGN1bGF0ZU5lclBvcG92ZXJQb3NpdGlvbjogdHJpZ2dlckNvb3JkcyAtICcgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCB0cmlnZ2VyQ29vcmRzICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci5uZXJGdWxsdGV4dCBfY2FsY3VsYXRlTmVyUG9wb3ZlclBvc2l0aW9uOiB0ZXh0Qm94Q29vcmRzIC0gJyApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coIHRleHRCb3hDb29yZHMgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgdmFyIHBvTGVmdEJvcmRlciA9IHRyaWdnZXJDb29yZHMubGVmdCAtICggMTUwIC0gKCB0cmlnZ2VyQ29vcmRzLndpZHRoIC8gMiApICksIHBvUmlnaHRCb3JkZXIgPSBwb0xlZnRCb3JkZXIgKyAzMDAsIHRiTGVmdEJvcmRlciA9IHRleHRCb3hDb29yZHMubGVmdCwgdGJSaWdodEJvcmRlciA9IHRleHRCb3hDb29yZHMucmlnaHQsIHBvVG9wLCBwb0xlZnQgPSBwb0xlZnRCb3JkZXI7XG4gICAgICAgIFxuICAgICAgICBwb1RvcCA9IHRyaWdnZXJDb29yZHMudG9wICsgMjc7XG4gICAgICAgIFxuICAgICAgICBpZiAoIHBvTGVmdEJvcmRlciA8PSB0YkxlZnRCb3JkZXIgKSB7XG4gICAgICAgICAgICBwb0xlZnQgPSB0YkxlZnRCb3JkZXI7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIGlmICggcG9SaWdodEJvcmRlciA+PSB0YlJpZ2h0Qm9yZGVyICkge1xuICAgICAgICAgICAgcG9MZWZ0ID0gdGV4dEJveENvb3Jkcy5yaWdodCAtIDMwMDtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgcmV0dXJuIHtcbiAgICAgICAgICAgIHRvcDogcG9Ub3AsXG4gICAgICAgICAgICBsZWZ0OiBwb0xlZnRcbiAgICAgICAgfTtcbiAgICB9XG4gICAgXG4gICAgLyoqXG4gICAgICogTWV0aG9kIHRvIHJlbW92ZSBhIHBvcG92ZXIgZnJvbSB0aGUgRE9NLlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX3JlbW92ZU5lclBvcG92ZXJcbiAgICAgKiBcbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfcmVtb3ZlTmVyUG9wb3ZlcigpIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci5uZXJGdWxsdGV4dCBfcmVtb3ZlTmVyUG9wb3ZlcicgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgJCggJy5uZXItcG9wb3Zlci1jbG9zZScgKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAkKCAnYm9keScgKS5maW5kKCAnLm5lci1wb3BvdmVyLXBvaW50ZXInICkuaGlkZSgpO1xuICAgICAgICAgICAgJCggdGhpcyApLnBhcmVudCgpLnJlbW92ZSgpO1xuICAgICAgICB9ICk7XG4gICAgfVxuICAgIFxuICAgIHJldHVybiB2aWV3ZXI7XG4gICAgXG59ICkoIHZpZXdlckpTIHx8IHt9LCBqUXVlcnkgKTtcbiIsIi8qKlxuICogVGhpcyBmaWxlIGlzIHBhcnQgb2YgdGhlIEdvb2JpIHZpZXdlciAtIGEgY29udGVudCBwcmVzZW50YXRpb24gYW5kIG1hbmFnZW1lbnRcbiAqIGFwcGxpY2F0aW9uIGZvciBkaWdpdGl6ZWQgb2JqZWN0cy5cbiAqIFxuICogVmlzaXQgdGhlc2Ugd2Vic2l0ZXMgZm9yIG1vcmUgaW5mb3JtYXRpb24uIC0gaHR0cDovL3d3dy5pbnRyYW5kYS5jb20gLVxuICogaHR0cDovL2RpZ2l2ZXJzby5jb21cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGZyZWUgc29mdHdhcmU7IHlvdSBjYW4gcmVkaXN0cmlidXRlIGl0IGFuZC9vciBtb2RpZnkgaXQgdW5kZXIgdGhlIHRlcm1zXG4gKiBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYXMgcHVibGlzaGVkIGJ5IHRoZSBGcmVlIFNvZnR3YXJlIEZvdW5kYXRpb247IGVpdGhlclxuICogdmVyc2lvbiAyIG9mIHRoZSBMaWNlbnNlLCBvciAoYXQgeW91ciBvcHRpb24pIGFueSBsYXRlciB2ZXJzaW9uLlxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZGlzdHJpYnV0ZWQgaW4gdGhlIGhvcGUgdGhhdCBpdCB3aWxsIGJlIHVzZWZ1bCwgYnV0IFdJVEhPVVQgQU5ZXG4gKiBXQVJSQU5UWTsgd2l0aG91dCBldmVuIHRoZSBpbXBsaWVkIHdhcnJhbnR5IG9mIE1FUkNIQU5UQUJJTElUWSBvciBGSVRORVNTIEZPUiBBXG4gKiBQQVJUSUNVTEFSIFBVUlBPU0UuIFNlZSB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgZm9yIG1vcmUgZGV0YWlscy5cbiAqIFxuICogWW91IHNob3VsZCBoYXZlIHJlY2VpdmVkIGEgY29weSBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYWxvbmcgd2l0aCB0aGlzXG4gKiBwcm9ncmFtLiBJZiBub3QsIHNlZSA8aHR0cDovL3d3dy5nbnUub3JnL2xpY2Vuc2VzLz4uXG4gKiBcbiAqIE1vZHVsZSB0byByZW5kZXIgcG9wb3ZlcnMgaW5jbHVkaW5nIG5vcm1kYXRhLlxuICogXG4gKiBAdmVyc2lvbiAzLjIuMFxuICogQG1vZHVsZSB2aWV3ZXJKUy5ub3JtZGF0YVxuICogQHJlcXVpcmVzIGpRdWVyeVxuICogQHJlcXVpcmVzIEJvb3RzdHJhcFxuICogXG4gKi9cbnZhciB2aWV3ZXJKUyA9ICggZnVuY3Rpb24oIHZpZXdlciApIHtcbiAgICAndXNlIHN0cmljdCc7XG4gICAgXG4gICAgdmFyIF9kZWJ1ZyA9IGZhbHNlO1xuICAgIHZhciBfZGF0YSA9IG51bGw7XG4gICAgdmFyIF9kYXRhVVJMID0gJyc7XG4gICAgdmFyIF9kYXRhID0gJyc7XG4gICAgdmFyIF9saW5rUG9zID0gbnVsbDtcbiAgICB2YXIgX3BvcG92ZXIgPSAnJztcbiAgICB2YXIgX2lkID0gJyc7XG4gICAgdmFyIF8kdGhpcyA9IG51bGw7XG4gICAgdmFyIF9ub3JtZGF0YUljb24gPSBudWxsO1xuICAgIHZhciBfcHJlbG9hZGVyID0gbnVsbDtcbiAgICB2YXIgX2RlZmF1bHRzID0ge1xuICAgICAgICBpZDogMCxcbiAgICAgICAgcGF0aDogbnVsbCxcbiAgICAgICAgbGFuZzoge30sXG4gICAgICAgIGVsZW1XcmFwcGVyOiBudWxsXG4gICAgfTtcbiAgICBcbiAgICB2aWV3ZXIubm9ybWRhdGEgPSB7XG4gICAgICAgIC8qKlxuICAgICAgICAgKiBNZXRob2QgdG8gaW5pdGlhbGl6ZSB0aGUgdGltZW1hdHJpeCBzbGlkZXIgYW5kIHRoZSBldmVudHMgd2hpY2ggYnVpbGRzIHRoZVxuICAgICAgICAgKiBtYXRyaXggYW5kIHBvcG92ZXJzLlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCBpbml0XG4gICAgICAgICAqIEBwYXJhbSB7T2JqZWN0fSBjb25maWcgQW4gY29uZmlnIG9iamVjdCB3aGljaCBvdmVyd3JpdGVzIHRoZSBkZWZhdWx0cy5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5pZCBUaGUgc3RhcnRpbmcgSUQgb2YgdGhlIHBvcG92ZXIuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcucGF0aCBUaGUgcm9vdHBhdGggb2YgdGhlIGFwcGxpY2F0aW9uLlxuICAgICAgICAgKiBAcGFyYW0ge09iamVjdH0gY29uZmlnLmxhbmcgQW4gb2JqZWN0IG9mIGxvY2FsaXplZCBzdHJpbmdzLlxuICAgICAgICAgKiBAcGFyYW0ge09iamVjdH0gY29uZmlnLmVsZW1XcmFwcGVyIEFuIGpRdWVyeSBvYmplY3Qgb2YgdGhlIHdyYXBwZXIgRElWLlxuICAgICAgICAgKiBAZXhhbXBsZVxuICAgICAgICAgKiBcbiAgICAgICAgICogPHByZT5cbiAgICAgICAgICogdmFyIG5vcm1kYXRhQ29uZmlnID0ge1xuICAgICAgICAgKiAgICAgcGF0aDogJyN7cmVxdWVzdC5jb250ZXh0UGF0aH0nLFxuICAgICAgICAgKiAgICAgbGFuZzoge1xuICAgICAgICAgKiAgICAgICAgIHBvcG92ZXJUaXRsZTogJyN7bXNnLm5vcm1kYXRhUG9wdmVyVGl0bGV9JyxcbiAgICAgICAgICogICAgICAgICBwb3BvdmVyQ2xvc2U6ICcje21zZy5ub3JtZGF0YVBvcG92ZXJDbG9zZX0nXG4gICAgICAgICAqICAgICB9LFxuICAgICAgICAgKiAgICAgZWxlbVdyYXBwZXI6ICQoICcjbWV0YWRhdGFFbGVtZW50V3JhcHBlcicgKVxuICAgICAgICAgKiB9O1xuICAgICAgICAgKiBcbiAgICAgICAgICogdmlld2VySlMubm9ybWRhdGEuaW5pdCggbm9ybWRhdGFDb25maWcgKTtcbiAgICAgICAgICogPC9wcmU+XG4gICAgICAgICAqL1xuICAgICAgICBpbml0OiBmdW5jdGlvbiggY29uZmlnICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICcjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIubm9ybWRhdGEuaW5pdCcgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci5ub3JtZGF0YS5pbml0OiBjb25maWcgPSAnLCBjb25maWcgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgJC5leHRlbmQoIHRydWUsIF9kZWZhdWx0cywgY29uZmlnICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIGhpZGUgY2xvc2UgaWNvbnNcbiAgICAgICAgICAgICQoICcuY2xvc2VBbGxQb3BvdmVycycgKS5oaWRlKCk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIGZpcnN0IGxldmVsIGNsaWNrXG4gICAgICAgICAgICAvLyBjb25zb2xlLmxvZyhcIkluaXQgQ2xpY2sgb24gbm9ybWRhdGFcIik7XG4gICAgICAgICAgICAvLyBjb25zb2xlLmxvZyhcIm5vcm1kYXRhbGluayA9IFwiLCAkKCAnLm5vcm1kYXRhTGluaycpIClcbiAgICAgICAgICAgICQoICcubm9ybWRhdGFMaW5rJyApLm9uKCAnY2xpY2snLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggXCJDbGljayBvbiBub3JtZGF0YVwiICk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgXyR0aGlzID0gJCggdGhpcyApO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIF8kdGhpcy5vZmYoICdmb2N1cycgKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICBfcmVuZGVyUG9wb3ZlckFjdGlvbiggXyR0aGlzLCBfZGVmYXVsdHMuaWQgKTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgfSxcbiAgICB9O1xuICAgIFxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB3aGljaCBleGVjdXRlcyB0aGUgY2xpY2sgZXZlbnQgYWN0aW9uIG9mIHRoZSBwb3BvdmVyLlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX3JlbmRlclBvcG92ZXJBY3Rpb25cbiAgICAgKiBAcGFyYW0ge09iamVjdH0gJE9iaiBUaGUgalF1ZXJ5IG9iamVjdCBvZiB0aGUgY3VycmVudCBjbGlja2VkIGxpbmsuXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IGlkIFRoZSBjdXJyZW50IGlkIG9mIHRoZSBwb3BvdmVyLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9yZW5kZXJQb3BvdmVyQWN0aW9uKCAkT2JqLCBpZCApIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX3JlbmRlclBvcG92ZXJBY3Rpb24oKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfcmVuZGVyUG9wb3ZlckFjdGlvbjogJE9iaiA9ICcsICRPYmogKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX3JlbmRlclBvcG92ZXJBY3Rpb246IGlkID0gJywgaWQgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgX25vcm1kYXRhSWNvbiA9ICRPYmouZmluZCggJy5mYS1saXN0LXVsJyApO1xuICAgICAgICBfcHJlbG9hZGVyID0gJE9iai5maW5kKCAnLm5vcm1kYXRhLXByZWxvYWRlcicgKTtcbiAgICAgICAgXG4gICAgICAgIC8vIHNldCB2YXJpYWJsZXNcbiAgICAgICAgX2RhdGFVUkwgPSAkT2JqLmF0dHIoICdkYXRhLXJlbW90ZWNvbnRlbnQnICk7XG4gICAgICAgIF9kYXRhID0gX2dldFJlbW90ZURhdGEoIF9kYXRhVVJMLCBfcHJlbG9hZGVyLCBfbm9ybWRhdGFJY29uICk7XG4gICAgICAgIF9saW5rUG9zID0gJE9iai5vZmZzZXQoKTtcbiAgICAgICAgX3BvcG92ZXIgPSBfYnVpbGRQb3BvdmVyKCBfZGF0YSwgaWQgKTtcbiAgICAgICAgXG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfcmVuZGVyUG9wb3ZlckFjdGlvbjogX2RhdGFVUkwgPSAnLCBfZGF0YVVSTCApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfcmVuZGVyUG9wb3ZlckFjdGlvbjogX2RhdGEgPSAnLCBfZGF0YSApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfcmVuZGVyUG9wb3ZlckFjdGlvbjogX2xpbmtQb3MgPSAnLCBfbGlua1BvcyApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICAvLyBhcHBlbmQgcG9wb3ZlciB0byBib2R5XG4gICAgICAgICQoICdib2R5JyApLmFwcGVuZCggX3BvcG92ZXIgKTtcbiAgICAgICAgXG4gICAgICAgIC8vIHNldCBwb3BvdmVyIHBvc2l0aW9uXG4gICAgICAgIF9jYWxjdWxhdGVQb3BvdmVyUG9zaXRpb24oIGlkLCBfbGlua1BvcywgJE9iaiApO1xuICAgICAgICBcbiAgICAgICAgLy8gc2hvdyBwb3BvdmVyXG4gICAgICAgICQoIGRvY3VtZW50ICkuZmluZCggJyNub3JtZGF0YVBvcG92ZXItJyArIGlkICkuaGlkZSgpLmZhZGVJbiggJ2Zhc3QnLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIC8vIGRpc2FibGUgc291cmNlIGJ1dHRvblxuICAgICAgICAgICAgJE9iai5hdHRyKCAnZGlzYWJsZWQnLCAnZGlzYWJsZWQnICkuYWRkQ2xhc3MoICdkaXNhYmxlZCcgKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gaGlkZSB0b29sdGlwXG4gICAgICAgICAgICAkT2JqLnRvb2x0aXAoICdoaWRlJyApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICAvLyBzZXQgZXZlbnQgZm9yIG50aCBsZXZlbCBwb3BvdmVyc1xuICAgICAgICAgICAgJCggJy5ub3JtZGF0YURldGFpbExpbmsnICkub2ZmKCAnY2xpY2snICkub24oICdjbGljaycsIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgIF8kdGhpcyA9ICQoIHRoaXMgKTtcbiAgICAgICAgICAgICAgICBfcmVuZGVyUG9wb3ZlckFjdGlvbiggXyR0aGlzLCBfZGVmYXVsdHMuaWQgKTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgfSApLmRyYWdnYWJsZSgpO1xuICAgICAgICBcbiAgICAgICAgLy8gaW5pdCBjbG9zZSBtZXRob2RcbiAgICAgICAgX2Nsb3NlTm9ybWRhdGFQb3BvdmVyKCAkT2JqICk7XG4gICAgICAgIFxuICAgICAgICAvLyBpbmNyZW1lbnQgaWRcbiAgICAgICAgX2RlZmF1bHRzLmlkKys7XG4gICAgICAgIFxuICAgICAgICAvLyBpbml0IGNsb3NlIGFsbCBtZXRob2RcbiAgICAgICAgX2Nsb3NlQWxsTm9ybWRhdGFQb3BvdmVycyggJE9iaiApO1xuICAgIH1cbiAgICBcbiAgICAvKipcbiAgICAgKiBSZXR1cm5zIGFuIEhUTUwtU3RyaW5nIHdoaWNoIHJlbmRlcnMgdGhlIGZldGNoZWQgZGF0YSBpbnRvIGEgcG9wb3Zlci5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9idWlsZFBvcG92ZXJcbiAgICAgKiBAcGFyYW0ge09iamVjdH0gZGF0YSBUaGUgSlNPTi1PYmplY3Qgd2hpY2ggaW5jbHVkZXMgdGhlIGRhdGEuXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IGlkIFRoZSBpbmNyZW1lbnRlZCBpZCBvZiB0aGUgcG9wb3Zlci5cbiAgICAgKiBAcmV0dXJucyB7U3RyaW5nfSBUaGUgSFRNTC1TdHJpbmcgd2l0aCB0aGUgZmV0Y2hlZCBkYXRhLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9idWlsZFBvcG92ZXIoIGRhdGEsIGlkICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfYnVpbGRQb3BvdmVyKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2J1aWxkUG9wb3ZlcjogZGF0YSA9ICcsIGRhdGEgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2J1aWxkUG9wb3ZlcjogaWQgPSAnLCBpZCApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgaHRtbCA9ICcnO1xuICAgICAgICBcbiAgICAgICAgaHRtbCArPSAnPGRpdiBpZD1cIm5vcm1kYXRhUG9wb3Zlci0nICsgaWQgKyAnXCIgY2xhc3M9XCJub3JtZGF0YS1wb3BvdmVyXCI+JztcbiAgICAgICAgaHRtbCArPSAnPGRpdiBjbGFzcz1cIm5vcm1kYXRhLXBvcG92ZXItdGl0bGVcIj4nO1xuICAgICAgICBodG1sICs9ICc8aDQ+JyArIF9kZWZhdWx0cy5sYW5nLnBvcG92ZXJUaXRsZSArICc8L2g0Pic7XG4gICAgICAgIGh0bWwgKz0gJzxzcGFuIGNsYXNzPVwibm9ybWRhdGEtcG9wb3Zlci1jbG9zZSBnbHlwaGljb24gZ2x5cGhpY29uLXJlbW92ZVwiIHRpdGxlPVwiJyArIF9kZWZhdWx0cy5sYW5nLnBvcG92ZXJDbG9zZSArICdcIj48L3NwYW4+JztcbiAgICAgICAgaHRtbCArPSAnPC9kaXY+JztcbiAgICAgICAgaHRtbCArPSAnPGRpdiBjbGFzcz1cIm5vcm1kYXRhLXBvcG92ZXItY29udGVudFwiPic7XG4gICAgICAgIGh0bWwgKz0gJzxkbCBjbGFzcz1cImRsLWhvcml6b250YWxcIj4nO1xuICAgICAgICAkLmVhY2goIGRhdGEsIGZ1bmN0aW9uKCBpLCBvYmplY3QgKSB7XG4gICAgICAgICAgICAkLmVhY2goIG9iamVjdCwgZnVuY3Rpb24oIHByb3BlcnR5LCB2YWx1ZSApIHtcbiAgICAgICAgICAgICAgICBodG1sICs9ICc8ZHQgdGl0bGU9XCInICsgcHJvcGVydHkgKyAnXCI+JyArIHByb3BlcnR5ICsgJzwvZHQ+JztcbiAgICAgICAgICAgICAgICBodG1sICs9ICc8ZGQ+JztcbiAgICAgICAgICAgICAgICAkLmVhY2goIHZhbHVlLCBmdW5jdGlvbiggcCwgdiApIHtcbiAgICAgICAgICAgICAgICAgICAgaWYgKCB2LnRleHQgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAoIHByb3BlcnR5ID09PSBcIlVSSVwiICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGh0bWwgKz0gJzxhIGhyZWY9XCInICsgdi50ZXh0ICsgJ1wiIHRhcmdldD1cIl9ibGFua1wiPic7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgaHRtbCArPSB2LnRleHQ7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgaHRtbCArPSAnPC9hPic7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBodG1sICs9IHYudGV4dDtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBpZiAoIHYuaWRlbnRpZmllciApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGh0bWwgKz0gJzxhIGhyZWY9XCInICsgX2RlZmF1bHRzLnBhdGggKyAnL3NlYXJjaC8tLycgKyB2LmlkZW50aWZpZXIgKyAnLzEvXCI+JztcbiAgICAgICAgICAgICAgICAgICAgICAgIGh0bWwgKz0gJzxzcGFuIGNsYXNzPVwiZ2x5cGhpY29uIGdseXBoaWNvbi1zZWFyY2hcIj48L3NwYW4+JztcbiAgICAgICAgICAgICAgICAgICAgICAgIGh0bWwgKz0gJzwvYT4nO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGlmICggdi51cmwgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBodG1sICs9ICc8YnV0dG9uIHR5cGU9XCJidXR0b25cIiBjbGFzcz1cIm5vcm1kYXRhRGV0YWlsTGlua1wiIGRhdGEtcmVtb3RlY29udGVudD1cIic7XG4gICAgICAgICAgICAgICAgICAgICAgICBodG1sICs9IF9kZWZhdWx0cy5wYXRoO1xuICAgICAgICAgICAgICAgICAgICAgICAgaHRtbCArPSAnL2FwaT9hY3Rpb249bm9ybWRhdGEmYW1wO3VybD0nO1xuICAgICAgICAgICAgICAgICAgICAgICAgaHRtbCArPSB2LnVybDtcbiAgICAgICAgICAgICAgICAgICAgICAgIGh0bWwgKz0gJ1wiIHRpdGxlPVwiJyArIF9kZWZhdWx0cy5sYW5nLnNob3dOb3JtZGF0YSArICdcIj4nO1xuICAgICAgICAgICAgICAgICAgICAgICAgaHRtbCArPSAnPGkgY2xhc3M9XCJmYSBmYS1saXN0LXVsXCIgYXJpYS1oaWRkZW49XCJ0cnVlXCI+PC9pPic7XG4gICAgICAgICAgICAgICAgICAgICAgICBodG1sICs9ICc8ZGl2IGNsYXNzPVwibm9ybWRhdGEtcHJlbG9hZGVyXCI+PC9kaXY+JztcbiAgICAgICAgICAgICAgICAgICAgICAgIGh0bWwgKz0gJzwvYnV0dG9uPic7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgaHRtbCArPSAnPGJyIC8+JztcbiAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgaHRtbCArPSAnPC9kZD4nXG4gICAgICAgICAgICB9ICk7XG4gICAgICAgIH0gKTtcbiAgICAgICAgaHRtbCArPSBcIjwvZGw+XCI7XG4gICAgICAgIGh0bWwgKz0gXCI8L2Rpdj5cIjtcbiAgICAgICAgaHRtbCArPSBcIjwvZGl2PlwiO1xuICAgICAgICBcbiAgICAgICAgcmV0dXJuIGh0bWw7XG4gICAgfVxuICAgIFxuICAgIC8qKlxuICAgICAqIFNldHMgdGhlIHBvc2l0aW9uIHRvIHRoZSBmaXJzdCBsZXZlbCBwb3BvdmVycy5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9jYWxjdWxhdGVGaXJzdExldmVsUG9wb3ZlclBvc2l0aW9uXG4gICAgICogQHBhcmFtIHtTdHJpbmd9IGlkIFRoZSBpbmNyZW1lbnRlZCBpZCBvZiB0aGUgcG9wb3Zlci5cbiAgICAgKiBAcGFyYW0ge09iamVjdH0gcG9zIEFuIE9iamVjdCB3aXRoIHRoZSBjdXJyZW50IHBvc2l0aW9uIG9mdCB0aGUgY2xpY2tlZCBsaW5rLlxuICAgICAqIEBwYXJhbSB7T2JqZWN0fSAkT2JqIEFuIGpRdWVyeS1PYmplY3Qgb2YgdGhlIGNsaWNrZWQgbGluay5cbiAgICAgKiBcbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfY2FsY3VsYXRlUG9wb3ZlclBvc2l0aW9uKCBpZCwgcG9zLCAkT2JqICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfY2FsY3VsYXRlUG9wb3ZlclBvc2l0aW9uKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2NhbGN1bGF0ZVBvcG92ZXJQb3NpdGlvbjogaWQgPSAnLCBpZCApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfY2FsY3VsYXRlUG9wb3ZlclBvc2l0aW9uOiBwb3MgPSAnLCBwb3MgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2NhbGN1bGF0ZVBvcG92ZXJQb3NpdGlvbjogJE9iaiA9ICcsICRPYmogKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgdmFyIF9ib2R5V2lkdGggPSAkKCAnYm9keScgKS5vdXRlcldpZHRoKCk7XG4gICAgICAgIHZhciBfcG9wb3ZlcldpZHRoID0gJCggJyNub3JtZGF0YVBvcG92ZXItJyArIGlkICkub3V0ZXJXaWR0aCgpO1xuICAgICAgICB2YXIgX3BvcG92ZXJSaWdodCA9IHBvcy5sZWZ0ICsgX3BvcG92ZXJXaWR0aDtcbiAgICAgICAgXG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfY2FsY3VsYXRlUG9wb3ZlclBvc2l0aW9uOiBfYm9keVdpZHRoID0gJywgX2JvZHlXaWR0aCApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfY2FsY3VsYXRlUG9wb3ZlclBvc2l0aW9uOiBfcG9wb3ZlcldpZHRoID0gJywgX3BvcG92ZXJXaWR0aCApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfY2FsY3VsYXRlUG9wb3ZlclBvc2l0aW9uOiBfcG9wb3ZlckxlZnQgPSAnLCBwb3MubGVmdCApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfY2FsY3VsYXRlUG9wb3ZlclBvc2l0aW9uOiBfcG9wb3ZlclJpZ2h0ID0gJywgX3BvcG92ZXJSaWdodCApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICBpZiAoIF9wb3BvdmVyUmlnaHQgPiBfYm9keVdpZHRoICkge1xuICAgICAgICAgICAgdmFyIF9kaWZmID0gX3BvcG92ZXJSaWdodCAtIF9ib2R5V2lkdGg7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2NhbGN1bGF0ZVBvcG92ZXJQb3NpdGlvbjogX2RpZmYgPSAnLCBfZGlmZiApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICAkKCBkb2N1bWVudCApLmZpbmQoICcjbm9ybWRhdGFQb3BvdmVyLScgKyBpZCApLmNzcygge1xuICAgICAgICAgICAgICAgIHRvcDogcG9zLnRvcCArICRPYmoub3V0ZXJIZWlnaHQoKSArIDUsXG4gICAgICAgICAgICAgICAgbGVmdDogcG9zLmxlZnQgLSBfZGlmZlxuICAgICAgICAgICAgfSApO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgJCggZG9jdW1lbnQgKS5maW5kKCAnI25vcm1kYXRhUG9wb3Zlci0nICsgaWQgKS5jc3MoIHtcbiAgICAgICAgICAgICAgICB0b3A6IHBvcy50b3AgKyAkT2JqLm91dGVySGVpZ2h0KCkgKyA1LFxuICAgICAgICAgICAgICAgIGxlZnQ6IHBvcy5sZWZ0XG4gICAgICAgICAgICB9ICk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgXG4gICAgLyoqXG4gICAgICogUmVtb3ZlcyBjdXJyZW50IHBvcG92ZXIgZnJvbSB0aGUgRE9NIG9uIGNsaWNrLlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX2Nsb3NlTm9ybWRhdGFQb3BvdmVyXG4gICAgICogXG4gICAgICovXG4gICAgZnVuY3Rpb24gX2Nsb3NlTm9ybWRhdGFQb3BvdmVyKCAkT2JqICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfY2xvc2VOb3JtZGF0YVBvcG92ZXIoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfY2xvc2VOb3JtZGF0YVBvcG92ZXI6ICRPYmogPSAnLCAkT2JqICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgICQoIGRvY3VtZW50ICkuZmluZCggJy5ub3JtZGF0YS1wb3BvdmVyLWNsb3NlJyApLm9uKCAnY2xpY2snLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICQoIHRoaXMgKS5wYXJlbnQoKS5wYXJlbnQoKS5yZW1vdmUoKTtcbiAgICAgICAgICAgICRPYmoucmVtb3ZlQXR0ciggJ2Rpc2FibGVkJyApLnJlbW92ZUNsYXNzKCAnZGlzYWJsZWQnICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmICggJCggJy5ub3JtZGF0YS1wb3BvdmVyJyApLmxlbmd0aCA8IDEgKSB7XG4gICAgICAgICAgICAgICAgJCggJy5jbG9zZUFsbFBvcG92ZXJzJyApLmhpZGUoKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSApO1xuICAgIH1cbiAgICBcbiAgICAvKipcbiAgICAgKiBSZW1vdmVzIGFsbCBwb3BvdmVycyBmcm9tIHRoZSBET00gb24gY2xpY2suXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfY2xvc2VBbGxOb3JtZGF0YVBvcG92ZXJzXG4gICAgICogXG4gICAgICovXG4gICAgZnVuY3Rpb24gX2Nsb3NlQWxsTm9ybWRhdGFQb3BvdmVycyggJE9iaiApIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX2Nsb3NlQWxsTm9ybWRhdGFQb3BvdmVycygpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19jbG9zZUFsbE5vcm1kYXRhUG9wb3ZlcnM6ICRPYmogPSAnLCAkT2JqICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIHZhciBfY2xvc2UgPSAkT2JqLnBhcmVudCgpLmZpbmQoICdpLmNsb3NlQWxsUG9wb3ZlcnMnICk7XG4gICAgICAgIFxuICAgICAgICBpZiAoICQoICcubm9ybWRhdGEtcG9wb3ZlcicgKS5sZW5ndGggPiAwICkge1xuICAgICAgICAgICAgX2Nsb3NlLnNob3coKTtcbiAgICAgICAgICAgIF9jbG9zZS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgLy8gY2xvc2UgYWxsIHBvcG92ZXJzXG4gICAgICAgICAgICAgICAgJCggJy5ub3JtZGF0YS1wb3BvdmVyJyApLmVhY2goIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICAkKCB0aGlzICkucmVtb3ZlKCk7XG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIC8vIGhpZGUgYWxsIGNsb3NlIGljb25zXG4gICAgICAgICAgICAgICAgJCggJy5jbG9zZUFsbFBvcG92ZXJzJyApLmVhY2goIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICAkKCB0aGlzICkuaGlkZSgpO1xuICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAvLyBzZXQgdHJpZ2dlciB0byBlbmFibGVcbiAgICAgICAgICAgICAgICAkKCAnLm5vcm1kYXRhTGluaycgKS5yZW1vdmVBdHRyKCAnZGlzYWJsZWQnICkucmVtb3ZlQ2xhc3MoICdkaXNhYmxlZCcgKTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIF9jbG9zZS5oaWRlKCk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgXG4gICAgLyoqXG4gICAgICogUmV0dXJucyBhbiBKU09OIG9iamVjdCBmcm9tIGEgQVBJIGNhbGwuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfZ2V0UmVtb3RlRGF0YVxuICAgICAqIEByZXR1cm5zIHtPYmplY3R9IFRoZSBKU09OIG9iamVjdCB3aXRoIHRoZSBBUEkgZGF0YS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfZ2V0UmVtb3RlRGF0YSggdXJsLCBsb2FkZXIsIGljb24gKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICctLS0tLS0tLS0tIF9nZXRSZW1vdGVEYXRhKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2dldFJlbW90ZURhdGE6IHVybCA9ICcsIHVybCApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfZ2V0UmVtb3RlRGF0YTogbG9hZGVyID0gJywgbG9hZGVyICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19nZXRSZW1vdGVEYXRhOiBpY29uID0gJywgaWNvbiApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICBsb2FkZXIuc2hvdygpO1xuICAgICAgICBpY29uLmhpZGUoKTtcbiAgICAgICAgXG4gICAgICAgIHZhciBkYXRhID0gJC5hamF4KCB7XG4gICAgICAgICAgICB1cmw6IGRlY29kZVVSSSggdXJsICksXG4gICAgICAgICAgICB0eXBlOiBcIlBPU1RcIixcbiAgICAgICAgICAgIGRhdGFUeXBlOiBcIkpTT05cIixcbiAgICAgICAgICAgIGFzeW5jOiBmYWxzZSxcbiAgICAgICAgICAgIHN1Y2Nlc3M6IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgIGxvYWRlci5oaWRlKCk7XG4gICAgICAgICAgICAgICAgaWNvbi5zaG93KCk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0gKS5yZXNwb25zZVRleHQ7XG4gICAgICAgIFxuICAgICAgICByZXR1cm4galF1ZXJ5LnBhcnNlSlNPTiggZGF0YSApO1xuICAgIH1cbiAgICBcbiAgICByZXR1cm4gdmlld2VyO1xuICAgIFxufSApKCB2aWV3ZXJKUyB8fCB7fSwgalF1ZXJ5ICk7XG4iLCIvKipcbiAqIFRoaXMgZmlsZSBpcyBwYXJ0IG9mIHRoZSBHb29iaSB2aWV3ZXIgLSBhIGNvbnRlbnQgcHJlc2VudGF0aW9uIGFuZCBtYW5hZ2VtZW50XG4gKiBhcHBsaWNhdGlvbiBmb3IgZGlnaXRpemVkIG9iamVjdHMuXG4gKiBcbiAqIFZpc2l0IHRoZXNlIHdlYnNpdGVzIGZvciBtb3JlIGluZm9ybWF0aW9uLiAtIGh0dHA6Ly93d3cuaW50cmFuZGEuY29tIC1cbiAqIGh0dHA6Ly9kaWdpdmVyc28uY29tXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBmcmVlIHNvZnR3YXJlOyB5b3UgY2FuIHJlZGlzdHJpYnV0ZSBpdCBhbmQvb3IgbW9kaWZ5IGl0IHVuZGVyIHRoZSB0ZXJtc1xuICogb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFzIHB1Ymxpc2hlZCBieSB0aGUgRnJlZSBTb2Z0d2FyZSBGb3VuZGF0aW9uOyBlaXRoZXJcbiAqIHZlcnNpb24gMiBvZiB0aGUgTGljZW5zZSwgb3IgKGF0IHlvdXIgb3B0aW9uKSBhbnkgbGF0ZXIgdmVyc2lvbi5cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGRpc3RyaWJ1dGVkIGluIHRoZSBob3BlIHRoYXQgaXQgd2lsbCBiZSB1c2VmdWwsIGJ1dCBXSVRIT1VUIEFOWVxuICogV0FSUkFOVFk7IHdpdGhvdXQgZXZlbiB0aGUgaW1wbGllZCB3YXJyYW50eSBvZiBNRVJDSEFOVEFCSUxJVFkgb3IgRklUTkVTUyBGT1IgQVxuICogUEFSVElDVUxBUiBQVVJQT1NFLiBTZWUgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGZvciBtb3JlIGRldGFpbHMuXG4gKiBcbiAqIFlvdSBzaG91bGQgaGF2ZSByZWNlaXZlZCBhIGNvcHkgb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFsb25nIHdpdGggdGhpc1xuICogcHJvZ3JhbS4gSWYgbm90LCBzZWUgPGh0dHA6Ly93d3cuZ251Lm9yZy9saWNlbnNlcy8+LlxuICogXG4gKiBNb2R1bGUgdG8gc2Nyb2xsIGEgcGFnZSBiYWNrIHRvIHRvcCBhbmltYXRlZC5cbiAqIFxuICogQHZlcnNpb24gMy4yLjBcbiAqIEBtb2R1bGUgdmlld2VySlMucGFnZVNjcm9sbFxuICogQHJlcXVpcmVzIGpRdWVyeVxuICovXG52YXIgdmlld2VySlMgPSAoIGZ1bmN0aW9uKCB2aWV3ZXIgKSB7XG4gICAgJ3VzZSBzdHJpY3QnO1xuICAgIFxuICAgIHZhciBfZWxlbSA9IG51bGw7XG4gICAgdmFyIF90ZXh0ID0gbnVsbDtcbiAgICBcbiAgICB2aWV3ZXIucGFnZVNjcm9sbCA9IHtcbiAgICAgICAgLyoqXG4gICAgICAgICAqIEluaXRpYWxpemVzIHRoZSBhbmltYXRlZCBwYWdlc2Nyb2xsLlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCBpbml0XG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBvYmogVGhlIHNlbGVjdG9yIG9mIHRoZSBqUXVlcnkgb2JqZWN0LlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gYW5jaG9yIFRoZSBuYW1lIG9mIHRoZSBhbmNob3IgdG8gc2Nyb2xsIHRvLlxuICAgICAgICAgKi9cbiAgICAgICAgaW5pdDogZnVuY3Rpb24oIG9iaiwgYW5jaG9yICkge1xuICAgICAgICAgICAgX2VsZW0gPSAkKCBvYmogKTtcbiAgICAgICAgICAgIF90ZXh0ID0gYW5jaG9yO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICAvLyBldmVudGxpc3RlbmVyXG4gICAgICAgICAgICAkKCB3aW5kb3cgKS5vbiggJ3Njcm9sbCcsIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgIGlmICggd2luZG93LnBhZ2VZT2Zmc2V0ID4gMjAwICkge1xuICAgICAgICAgICAgICAgICAgICBfZWxlbS5mYWRlSW4oKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIF9lbGVtLmhpZGUoKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIF9lbGVtLm9uKCAnY2xpY2snLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICBfc2Nyb2xsUGFnZSggX3RleHQgKTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgfVxuICAgIH07XG4gICAgXG4gICAgLyoqXG4gICAgICogTWV0aG9kIHdoaWNoIHNjcm9sbHMgdGhlIHBhZ2UgYW5pbWF0ZWQuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfc2Nyb2xsUGFnZVxuICAgICAqIEBwYXJhbSB7U3RyaW5nfSBhbmNob3IgVGhlIG5hbWUgb2YgdGhlIGFuY2hvciB0byBzY3JvbGwgdG8uXG4gICAgICovXG4gICAgZnVuY3Rpb24gX3Njcm9sbFBhZ2UoIGFuY2hvciApIHtcbiAgICAgICAgJCggJ2h0bWwsYm9keScgKS5hbmltYXRlKCB7XG4gICAgICAgICAgICBzY3JvbGxUb3A6ICQoIGFuY2hvciApLm9mZnNldCgpLnRvcFxuICAgICAgICB9LCAxMDAwICk7XG4gICAgICAgIFxuICAgICAgICByZXR1cm4gZmFsc2U7XG4gICAgfVxuICAgIFxuICAgIHJldHVybiB2aWV3ZXI7XG4gICAgXG59ICkoIHZpZXdlckpTIHx8IHt9LCBqUXVlcnkgKTtcbiIsIi8qKlxuICogVGhpcyBmaWxlIGlzIHBhcnQgb2YgdGhlIEdvb2JpIHZpZXdlciAtIGEgY29udGVudCBwcmVzZW50YXRpb24gYW5kIG1hbmFnZW1lbnRcbiAqIGFwcGxpY2F0aW9uIGZvciBkaWdpdGl6ZWQgb2JqZWN0cy5cbiAqIFxuICogVmlzaXQgdGhlc2Ugd2Vic2l0ZXMgZm9yIG1vcmUgaW5mb3JtYXRpb24uIC0gaHR0cDovL3d3dy5pbnRyYW5kYS5jb20gLVxuICogaHR0cDovL2RpZ2l2ZXJzby5jb21cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGZyZWUgc29mdHdhcmU7IHlvdSBjYW4gcmVkaXN0cmlidXRlIGl0IGFuZC9vciBtb2RpZnkgaXQgdW5kZXIgdGhlIHRlcm1zXG4gKiBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYXMgcHVibGlzaGVkIGJ5IHRoZSBGcmVlIFNvZnR3YXJlIEZvdW5kYXRpb247IGVpdGhlclxuICogdmVyc2lvbiAyIG9mIHRoZSBMaWNlbnNlLCBvciAoYXQgeW91ciBvcHRpb24pIGFueSBsYXRlciB2ZXJzaW9uLlxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZGlzdHJpYnV0ZWQgaW4gdGhlIGhvcGUgdGhhdCBpdCB3aWxsIGJlIHVzZWZ1bCwgYnV0IFdJVEhPVVQgQU5ZXG4gKiBXQVJSQU5UWTsgd2l0aG91dCBldmVuIHRoZSBpbXBsaWVkIHdhcnJhbnR5IG9mIE1FUkNIQU5UQUJJTElUWSBvciBGSVRORVNTIEZPUiBBXG4gKiBQQVJUSUNVTEFSIFBVUlBPU0UuIFNlZSB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgZm9yIG1vcmUgZGV0YWlscy5cbiAqIFxuICogWW91IHNob3VsZCBoYXZlIHJlY2VpdmVkIGEgY29weSBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYWxvbmcgd2l0aCB0aGlzXG4gKiBwcm9ncmFtLiBJZiBub3QsIHNlZSA8aHR0cDovL3d3dy5nbnUub3JnL2xpY2Vuc2VzLz4uXG4gKiBcbiAqIE1vZHVsZSB3aGljaCBnZW5lcmF0ZXMgYSByZXNwb25zaXZlIGltYWdlIGdhbGxlcnkgaW4gY29sdW1ucy5cbiAqIFxuICogQHZlcnNpb24gMy4yLjBcbiAqIEBtb2R1bGUgdmlld2VySlMucmVzcG9uc2l2ZUNvbHVtbkdhbGxlcnlcbiAqIEByZXF1aXJlcyBqUXVlcnlcbiAqIFxuICovXG52YXIgdmlld2VySlMgPSAoIGZ1bmN0aW9uKCB2aWV3ZXIgKSB7XG4gICAgJ3VzZSBzdHJpY3QnO1xuICAgIFxuICAgIC8vIGRlZmF1bHQgdmFyaWFibGVzXG4gICAgdmFyIF9kZWJ1ZyA9IGZhbHNlO1xuICAgIHZhciBfZGVmYXVsdHMgPSB7XG4gICAgICAgIHRoZW1lUGF0aDogJycsXG4gICAgICAgIGltYWdlUGF0aDogJycsXG4gICAgICAgIGltYWdlRGF0YUZpbGU6ICcnLFxuICAgICAgICBnYWxsZXJ5T2JqZWN0OiBudWxsLFxuICAgICAgICBtYXhDb2x1bW5Db3VudDogbnVsbCxcbiAgICAgICAgbWF4SW1hZ2VzUGVyQ29sdW1uOiBudWxsLFxuICAgICAgICBmaXhlZEhlaWdodDogZmFsc2UsXG4gICAgICAgIG1heEhlaWdodDogJycsXG4gICAgICAgIGNhcHRpb246IHRydWUsXG4gICAgICAgIG92ZXJsYXlDb2xvcjogJycsXG4gICAgICAgIGxhbmc6IHt9LFxuICAgICAgICBsaWdodGJveDoge1xuICAgICAgICAgICAgYWN0aXZlOiB0cnVlLFxuICAgICAgICAgICAgY2FwdGlvbjogdHJ1ZVxuICAgICAgICB9LFxuICAgIH07XG4gICAgdmFyIF9wcm9taXNlID0gbnVsbDtcbiAgICB2YXIgX2ltYWdlRGF0YSA9IG51bGw7XG4gICAgdmFyIF9wYXJlbnRJbWFnZSA9IG51bGw7XG4gICAgdmFyIF9saWdodGJveEltYWdlID0gbnVsbDtcbiAgICB2YXIgX2ltYWdlTGlnaHRib3ggPSBudWxsO1xuICAgIHZhciBfc21hbGxWaWV3cG9ydCA9IG51bGw7XG4gICAgdmFyIF9kYXRhVXJsID0gbnVsbDtcbiAgICBcbiAgICB2aWV3ZXIucmVzcG9uc2l2ZUNvbHVtbkdhbGxlcnkgPSB7XG4gICAgICAgIC8qKlxuICAgICAgICAgKiBNZXRob2Qgd2hpY2ggaW5pdGlhbGl6ZXMgdGhlIGNvbHVtbiBnYWxsZXJ5LlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCBpbml0XG4gICAgICAgICAqIEBwYXJhbSB7T2JqZWN0fSBjb25maWcgQW4gY29uZmlnIG9iamVjdCB3aGljaCBvdmVyd3JpdGVzIHRoZSBkZWZhdWx0cy5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy50aGVtZVBhdGggVGhlIHBhdGggdG8gdGhlIGN1cnJlbnQgYWN0aXZhdGVkIHZpZXdlclxuICAgICAgICAgKiB0aGVtZS5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5pbWFnZVBhdGggVGhlIHBhdGggdG8gdGhlIHVzZWQgaW1hZ2VzLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLmltYWdlRGF0YUZpbGUgVGhlIHBhdGggdG8gdGhlIEpTT04tRmlsZSwgd2hpY2ggY29udGFpbnNcbiAgICAgICAgICogdGhlIGltYWdlcyBkYXRhLlxuICAgICAgICAgKiBAcGFyYW0ge09iamVjdH0gY29uZmlnLmdhbGxlcnlPYmplY3QgVGhlIERJViB3aGVyZSB0aGUgZ2FsbGVyeSBzaG91bGQgYmVcbiAgICAgICAgICogcmVuZGVyZWQuXG4gICAgICAgICAqIEBwYXJhbSB7TnVtYmVyfSBjb25maWcubWF4Q29sdW1uQ291bnQgQ291bnQgY291bnQgb2YgdGhlIGdhbGxlcnksIDQgY29sdW1uIGFyZVxuICAgICAgICAgKiBtYXhpbXVtLlxuICAgICAgICAgKiBAcGFyYW0ge051bWJlcn0gY29uZmlnLm1heEltYWdlc1BlckNvbHVtbiBDb3VudCBvZiB0aGUgaW1hZ2VzIHBlciBjb2x1bW4uXG4gICAgICAgICAqIEBwYXJhbSB7Qm9vbGVhbn0gY29uZmlnLmZpeGVkSGVpZ2h0IElmIHRydWUgdGhlIGltYWdlcyBoYXZlIGEgZml4ZWQgaGVpZ2h0LFxuICAgICAgICAgKiBkZWZhdWx0IGlzIGZhbHNlLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLm1heEhlaWdodCBTZXRzIHRoZSBnaXZlbiBtYXggaGVpZ2h0IHZhbHVlIGZvciB0aGVcbiAgICAgICAgICogaW1hZ2VzLlxuICAgICAgICAgKiBAcGFyYW0ge0Jvb2xlYW59IGNvbmZpZy5jYXB0aW9uIElmIHRydWUgdGhlIGdhbGxlcnkgaW1hZ2VzIGhhdmUgYSBjYXB0aW9uIHdpdGhcbiAgICAgICAgICogdGhlIHRpdGxlIHRleHQsIGRlZmF1bHQgaXMgdHJ1ZS5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5vdmVybGF5Q29sb3IgVGFrZXMgYSBIRVgtdmFsdWUgdG8gc2V0IHRoZSBjb2xvciBvZiB0aGVcbiAgICAgICAgICogaW1hZ2Ugb3ZlcmxheS5cbiAgICAgICAgICogQHBhcmFtIHtPYmplY3R9IGNvbmZpZy5sYW5nIEFuIG9iamVjdCBvZiBzdHJpbmdzIGZvciBtdWx0aWxhbmd1YWdlXG4gICAgICAgICAqIGZ1bmN0aW9uYWxpdHkuXG4gICAgICAgICAqIEBwYXJhbSB7T2JqZWN0fSBjb25maWcubGlnaHRib3ggQW4gT2JqZWN0IHRvIGNvbmZpZ3VyZSB0aGUgaW1hZ2UgbGlnaHRib3guXG4gICAgICAgICAqIEBwYXJhbSB7Qm9vbGVhbn0gY29uZmlnLmxpZ2h0Ym94LmFjdGl2ZSBJZiB0cnVlIHRoZSBsaWdodGJveCBmdW5jdGlvbmFsaXR5IGlzXG4gICAgICAgICAqIGVuYWJsZWQsIGRlZmF1bHQgaXMgdHJ1ZS5cbiAgICAgICAgICogQHBhcmFtIHtCb29sZWFufSBjb25maWcubGlnaHRib3guY2FwdGlvbiBJZiB0cnVlIHRoZSBsaWdodGJveCBoYXMgYSBjYXB0aW9uXG4gICAgICAgICAqIHRleHQsIGRlZmF1bHQgaXMgdHJ1ZS5cbiAgICAgICAgICovXG4gICAgICAgIGluaXQ6IGZ1bmN0aW9uKCBjb25maWcgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci5yZXNwb25zaXZlQ29sdW1uR2FsbGVyeS5pbml0JyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLnJlc3BvbnNpdmVDb2x1bW5HYWxsZXJ5LmluaXQ6IGNvbmZpZyA9ICcsIGNvbmZpZyApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICAkLmV4dGVuZCggdHJ1ZSwgX2RlZmF1bHRzLCBjb25maWcgKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gZmV0Y2ggaW1hZ2UgZGF0YSBhbmQgY2hlY2sgdGhlIHZpZXdwb3J0XG4gICAgICAgICAgICBfZGF0YVVybCA9IF9kZWZhdWx0cy50aGVtZVBhdGggKyBfZGVmYXVsdHMuaW1hZ2VEYXRhRmlsZTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgX3Byb21pc2UgPSB2aWV3ZXIuaGVscGVyLmdldFJlbW90ZURhdGEoIF9kYXRhVXJsICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIF9wcm9taXNlLnRoZW4oIGZ1bmN0aW9uKCBpbWFnZURhdGEgKSB7XG4gICAgICAgICAgICAgICAgX2ltYWdlRGF0YSA9IGltYWdlRGF0YTtcbiAgICAgICAgICAgICAgICBfc21hbGxWaWV3cG9ydCA9IHZpZXdlci5yZXNwb25zaXZlQ29sdW1uR2FsbGVyeS5jaGVja0ZvclNtYWxsVmlld3BvcnQoKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAvLyByZW5kZXIgY29sdW1uc1xuICAgICAgICAgICAgICAgIGlmICggX2RlZmF1bHRzLm1heENvbHVtbkNvdW50ID4gNCApIHtcbiAgICAgICAgICAgICAgICAgICAgX2RlZmF1bHRzLmdhbGxlcnlPYmplY3QuYXBwZW5kKCB2aWV3ZXIuaGVscGVyLnJlbmRlckFsZXJ0KCAnYWxlcnQtZGFuZ2VyJywgJ0RpZSBtYXhpbWFsZSBTcGFsdGVuYW56YWhsIGbDvHIgZGllIEdhbGVyaWUgYmV0csOkZ3QgNCEnLCB0cnVlICkgKTtcbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgIHJldHVybiBmYWxzZTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIGZvciAoIHZhciBpID0gMDsgaSA8IF9kZWZhdWx0cy5tYXhDb2x1bW5Db3VudDsgaSsrICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgX2RlZmF1bHRzLmdhbGxlcnlPYmplY3QuYXBwZW5kKCB2aWV3ZXIucmVzcG9uc2l2ZUNvbHVtbkdhbGxlcnkucmVuZGVyQ29sdW1ucyggX2RlZmF1bHRzLm1heENvbHVtbkNvdW50ICkgKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAvLyByZW5kZXIgaW1hZ2VzXG4gICAgICAgICAgICAgICAgd2hpbGUgKCBfaW1hZ2VEYXRhLmxlbmd0aCApIHtcbiAgICAgICAgICAgICAgICAgICAgJC5lYWNoKCAkKCAnLnJjZy1jb2wnICksIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgJCggdGhpcyApLmFwcGVuZCggdmlld2VyLnJlc3BvbnNpdmVDb2x1bW5HYWxsZXJ5LnJlbmRlckltYWdlcyggX2ltYWdlRGF0YS5zcGxpY2UoIDAsIF9kZWZhdWx0cy5tYXhJbWFnZXNQZXJDb2x1bW4gKSApICk7XG4gICAgICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLy8gc2V0IGZpeGVkIGhlaWdodCBpZiBhY3RpdmF0ZWQgYW5kIHZpZXdwb3J0IGlzID4gMzc1cHhcbiAgICAgICAgICAgICAgICBpZiAoIF9kZWZhdWx0cy5maXhlZEhlaWdodCAmJiAhX3NtYWxsVmlld3BvcnQgKSB7XG4gICAgICAgICAgICAgICAgICAgICQuZWFjaCggJCggJy5yY2ctaW1hZ2UtYm9keScgKSwgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICB2aWV3ZXIucmVzcG9uc2l2ZUNvbHVtbkdhbGxlcnkuZml4ZWRIZWlnaHQoICQoIHRoaXMgKSApO1xuICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIC8vIHByZXBhcmUgbGlnaHRib3hcbiAgICAgICAgICAgICAgICBpZiAoIF9kZWZhdWx0cy5saWdodGJveC5hY3RpdmUgKSB7XG4gICAgICAgICAgICAgICAgICAgICQoICcubGlnaHRib3gtdG9nZ2xlJyApLm9uKCAnY2xpY2snLCBmdW5jdGlvbiggZXZlbnQgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBldmVudC5wcmV2ZW50RGVmYXVsdCgpO1xuICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICBfcGFyZW50SW1hZ2UgPSAkKCB0aGlzICkucGFyZW50KCkuY2hpbGRyZW4oICdpbWcnICk7XG4gICAgICAgICAgICAgICAgICAgICAgICBfbGlnaHRib3hJbWFnZSA9IHZpZXdlci5yZXNwb25zaXZlQ29sdW1uR2FsbGVyeS5wcmVwYXJlTGlnaHRib3goIF9wYXJlbnRJbWFnZSApO1xuICAgICAgICAgICAgICAgICAgICAgICAgX2ltYWdlTGlnaHRib3ggPSB2aWV3ZXIucmVzcG9uc2l2ZUNvbHVtbkdhbGxlcnkucmVuZGVyTGlnaHRib3goIF9saWdodGJveEltYWdlICk7XG4gICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgICQoICdib2R5JyApLmFwcGVuZCggX2ltYWdlTGlnaHRib3ggKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgJCggJy5yY2ctbGlnaHRib3gtYm9keScgKS5oaWRlKCk7XG4gICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgICQoICcucmNnLWxpZ2h0Ym94LW92ZXJsYXknICkuZmFkZUluKCAnc2xvdycgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgLy8gZmlyc3QgbG9hZCBpbWFnZSwgdGhlbiBjZW50ZXIgaXQgYW5kIHNob3cgaXQgdXBcbiAgICAgICAgICAgICAgICAgICAgICAgICQoICcucmNnLWxpZ2h0Ym94LWltYWdlIGltZycgKS5sb2FkKCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB2aWV3ZXIucmVzcG9uc2l2ZUNvbHVtbkdhbGxlcnkuY2VudGVyTGlnaHRib3goICQoICcucmNnLWxpZ2h0Ym94LWJvZHknICkgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCAnLnJjZy1saWdodGJveC1ib2R5JyApLnNob3coKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgLy8gY2xvc2UgbGlnaHRib3ggdmlhIGJ1dHRvblxuICAgICAgICAgICAgICAgICAgICAgICAgJCggJy5yY2ctbGlnaHRib3gtY2xvc2UnICkub24oICdjbGljaycsIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICQoICcucmNnLWxpZ2h0Ym94LW92ZXJsYXknICkucmVtb3ZlKCk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgIC8vIGNsb3NlIGxpZ2h0Ym94IHZpYSBlc2NcbiAgICAgICAgICAgICAgICAgICAgICAgICQoIGRvY3VtZW50ICkua2V5cHJlc3MoIGZ1bmN0aW9uKCBldmVudCApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBpZiAoIGV2ZW50LmtleUNvZGUgPT09IDI3ICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCAnLnJjZy1saWdodGJveC1vdmVybGF5JyApLnJlbW92ZSgpO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgLy8gY2xvc2UgbGlnaHRib3ggdmlhIGNsaWNrIG9uIHBpY3R1cmVcbiAgICAgICAgICAgICAgICAgICAgICAgICQoICcucmNnLWxpZ2h0Ym94LWltYWdlIGltZycgKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgJCggJy5yY2ctbGlnaHRib3gtb3ZlcmxheScgKS5yZW1vdmUoKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH0gKS50aGVuKCBudWxsLCBmdW5jdGlvbiggZXJyb3IgKSB7XG4gICAgICAgICAgICAgICAgX2RlZmF1bHRzLmdhbGxlcnlPYmplY3QuYXBwZW5kKCB2aWV3ZXIuaGVscGVyLnJlbmRlckFsZXJ0KCAnYWxlcnQtZGFuZ2VyJywgJzxzdHJvbmc+U3RhdHVzOiA8L3N0cm9uZz4nICsgZXJyb3Iuc3RhdHVzICsgJyAnICsgZXJyb3Iuc3RhdHVzVGV4dCwgZmFsc2UgKSApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUuZXJyb3IoICdFUlJPUjogdmlld2VyLnJlc3BvbnNpdmVDb2x1bW5HYWxsZXJ5LmluaXQgLSAnLCBlcnJvciApO1xuICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgXG4gICAgICAgIH0sXG4gICAgICAgIC8qKlxuICAgICAgICAgKiBNZXRob2Qgd2hpY2ggcmVuZGVycyB0aGUgZ2FsbGVyeSBjb2x1bW5zLlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCByZW5kZXJDb2x1bW5zXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb3VudCBUaGUgY29sdW1uIGNvdW50IG9mIHRoZSBnYWxsZXJ5LlxuICAgICAgICAgKiBAcmV0dXJucyB7U3RyaW5nfSBBIEhUTUwtU3RyaW5nIHdoaWNoIHJlbmRlcnMgYSBjb2x1bW4uXG4gICAgICAgICAqL1xuICAgICAgICByZW5kZXJDb2x1bW5zOiBmdW5jdGlvbiggY291bnQgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gdmlld2VyLnJlc3BvbnNpdmVDb2x1bW5HYWxsZXJ5LnJlbmRlckNvbHVtbnMoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLnJlc3BvbnNpdmVDb2x1bW5HYWxsZXJ5LnJlbmRlckNvbHVtbnM6IGNvdW50ID0gJywgY291bnQgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHZhciBjb2x1bW4gPSAnJztcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgY29sdW1uICs9ICc8ZGl2IGNsYXNzPVwicmNnLWNvbCBjb2wtJyArIGNvdW50ICsgJ1wiPjwvZGl2Pic7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHJldHVybiBjb2x1bW47XG4gICAgICAgIH0sXG4gICAgICAgIC8qKlxuICAgICAgICAgKiBNZXRob2Qgd2hpY2ggcmVuZGVycyB0aGUgZ2FsbGVyeSBpbWFnZXMuXG4gICAgICAgICAqIFxuICAgICAgICAgKiBAbWV0aG9kIHJlbmRlckltYWdlc1xuICAgICAgICAgKiBAcGFyYW0ge09iamVjdH0gZGF0YSBBbiBvYmplY3Qgb2YgaW1hZ2UgZGF0YSB0byByZW5kZXIgdGhlIGltYWdlcy5cbiAgICAgICAgICogQHJldHVybnMge1N0cmluZ30gQSBIVE1MLVN0cmluZyB3aGljaCByZW5kZXJzIHRoZSBnYWxsZXJ5IGltYWdlcy5cbiAgICAgICAgICovXG4gICAgICAgIHJlbmRlckltYWdlczogZnVuY3Rpb24oIGRhdGEgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gdmlld2VyLnJlc3BvbnNpdmVDb2x1bW5HYWxsZXJ5LnJlbmRlckltYWdlcygpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIucmVzcG9uc2l2ZUNvbHVtbkdhbGxlcnkucmVuZGVySW1hZ2VzOiBkYXRhID0gJywgZGF0YSApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgdmFyIGltYWdlID0gJyc7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgICQuZWFjaCggZGF0YSwgZnVuY3Rpb24oIGksIGogKSB7XG4gICAgICAgICAgICAgICAgJC5lYWNoKCBqLCBmdW5jdGlvbiggbSwgbiApIHtcbiAgICAgICAgICAgICAgICAgICAgaW1hZ2UgKz0gJzxkaXYgY2xhc3M9XCJyY2ctaW1hZ2UtY29udGFpbmVyXCI+JztcbiAgICAgICAgICAgICAgICAgICAgaW1hZ2UgKz0gJzxkaXYgY2xhc3M9XCJyY2ctaW1hZ2UtYm9keVwiPic7XG4gICAgICAgICAgICAgICAgICAgIGltYWdlICs9ICc8YSBocmVmPVwiJyArIG4udXJsICsgJ1wiPic7XG4gICAgICAgICAgICAgICAgICAgIGltYWdlICs9ICc8ZGl2IGNsYXNzPVwicmNnLWltYWdlLW92ZXJsYXlcIiBzdHlsZT1cImJhY2tncm91bmQtY29sb3I6JyArIF9kZWZhdWx0cy5vdmVybGF5Q29sb3IgKyAnXCI+PC9kaXY+JztcbiAgICAgICAgICAgICAgICAgICAgaW1hZ2UgKz0gJzwvYT4nO1xuICAgICAgICAgICAgICAgICAgICBpbWFnZSArPSAnPGRpdiBjbGFzcz1cInJjZy1pbWFnZS10aXRsZVwiPic7XG4gICAgICAgICAgICAgICAgICAgIGltYWdlICs9ICc8aDQ+JyArIG4udGl0bGUgKyAnPC9oND4nO1xuICAgICAgICAgICAgICAgICAgICBpbWFnZSArPSAnPC9kaXY+JztcbiAgICAgICAgICAgICAgICAgICAgaW1hZ2UgKz0gJzxpbWcgc3JjPVwiJyArIF9kZWZhdWx0cy50aGVtZVBhdGggKyBfZGVmYXVsdHMuaW1hZ2VQYXRoICsgbi5uYW1lICsgJ1wiIGFsdD1cIicgKyBuLmFsdCArICdcIiAvPic7XG4gICAgICAgICAgICAgICAgICAgIGlmICggX2RlZmF1bHRzLmxpZ2h0Ym94LmFjdGl2ZSApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGltYWdlICs9ICc8ZGl2IGNsYXNzPVwibGlnaHRib3gtdG9nZ2xlXCIgdGl0bGU9XCInICsgX2RlZmF1bHRzLmxhbmcuc2hvd0xpZ2h0Ym94ICsgJ1wiPic7XG4gICAgICAgICAgICAgICAgICAgICAgICBpbWFnZSArPSAnPHNwYW4gY2xhc3M9XCJnbHlwaGljb24gZ2x5cGhpY29uLWZ1bGxzY3JlZW5cIj48L3NwYW4+JztcbiAgICAgICAgICAgICAgICAgICAgICAgIGltYWdlICs9ICc8L2Rpdj4nO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGltYWdlICs9ICc8L2Rpdj4nO1xuICAgICAgICAgICAgICAgICAgICBpZiAoIF9kZWZhdWx0cy5jYXB0aW9uICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgaW1hZ2UgKz0gJzxkaXYgY2xhc3M9XCJyY2ctaW1hZ2UtZm9vdGVyXCI+JztcbiAgICAgICAgICAgICAgICAgICAgICAgIGltYWdlICs9ICc8cD4nICsgbi5jYXB0aW9uICsgJzxhIGhyZWY9XCInICsgbi51cmwgKyAnXCIgdGl0bGU9XCInICsgbi50aXRsZSArICdcIj4nO1xuICAgICAgICAgICAgICAgICAgICAgICAgaW1hZ2UgKz0gX2RlZmF1bHRzLmxhbmcuZ29Ub1dvcmsgKyAnIDxzcGFuIGNsYXNzPVwiZ2x5cGhpY29uIGdseXBoaWNvbiBnbHlwaGljb24tcGljdHVyZVwiPjwvc3Bhbj48L2E+PC9wPic7XG4gICAgICAgICAgICAgICAgICAgICAgICBpbWFnZSArPSAnPC9kaXY+JztcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBpbWFnZSArPSAnPC9kaXY+JztcbiAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHJldHVybiBpbWFnZTtcbiAgICAgICAgfSxcbiAgICAgICAgLyoqXG4gICAgICAgICAqIE1ldGhvZCB3aGljaCBzZXRzIGEgZml4ZWQgaGVpZ2h0IHRvIHRoZSBnYWxsZXJ5IGltYWdlcy5cbiAgICAgICAgICogXG4gICAgICAgICAqIEBtZXRob2QgZml4ZWRIZWlnaHRcbiAgICAgICAgICogQHBhcmFtIHtPYmplY3R9ICRvYmogQW4galF1ZXJ5IG9iamVjdCBvZiB0aGUgZWxlbWVudCB3aGljaCBoZWlnaHQgc2hvdWxkIGJlXG4gICAgICAgICAqIGZpeGVkLlxuICAgICAgICAgKi9cbiAgICAgICAgZml4ZWRIZWlnaHQ6IGZ1bmN0aW9uKCAkb2JqICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICctLS0tLS0tLS0tIHZpZXdlci5yZXNwb25zaXZlQ29sdW1uR2FsbGVyeS5maXhlZEhlaWdodCgpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIucmVzcG9uc2l2ZUNvbHVtbkdhbGxlcnkuZml4ZWRIZWlnaHQ6ICRvYmogPSAnLCAkb2JqICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgICRvYmouY2hpbGRyZW4oICdpbWcnICkuY3NzKCB7XG4gICAgICAgICAgICAgICAgJ2hlaWdodCc6IF9kZWZhdWx0cy5tYXhIZWlnaHRcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgfSxcbiAgICAgICAgLyoqXG4gICAgICAgICAqIE1ldGhvZCB3aGljaCBjaGVja3MgdGhlIHZpZXdwb3J0IHdpZHRoIGFuZCByZXR1cm5zIHRydWUgaWYgaXTCtHMgc21hbGxlciB0aGVuXG4gICAgICAgICAqIDM3NXB4LlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCBjaGVja0ZvclNtYWxsVmlld3BvcnRcbiAgICAgICAgICogQHJldHVybnMge0Jvb2xlYW59IFJldHVybnMgdHJ1ZSBpZiBpdMK0cyBzbWFsbGVyIHRoZW4gMzc1cHguXG4gICAgICAgICAqL1xuICAgICAgICBjaGVja0ZvclNtYWxsVmlld3BvcnQ6IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICctLS0tLS0tLS0tIHZpZXdlci5yZXNwb25zaXZlQ29sdW1uR2FsbGVyeS5jaGVja0ZvclNtYWxsVmlld3BvcnQoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgdmFyIHdpbmRvd1dpZHRoID0gJCggd2luZG93ICkub3V0ZXJXaWR0aCgpO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBpZiAoIHdpbmRvd1dpZHRoIDw9IDM3NSApIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gdHJ1ZTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIHJldHVybiBmYWxzZTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSxcbiAgICAgICAgLyoqXG4gICAgICAgICAqIE1ldGhvZCB3aGljaCBwcmVwYXJlcyB0aGUgbGlnaHRib3ggb2JqZWN0IHdpdGggdGhlIHJlcXVpcmVkIGRhdGEuXG4gICAgICAgICAqIFxuICAgICAgICAgKiBAbWV0aG9kIHByZXBhcmVMaWdodGJveFxuICAgICAgICAgKiBAcGFyYW0ge09iamVjdH0gJG9iaiBBbiBqUXVlcnkgb2JqZWN0IHdoaWNoIGluY2x1ZGVzIHRoZSByZXF1aXJlZCBkYXRhXG4gICAgICAgICAqIGF0dHJpYnV0ZXMuXG4gICAgICAgICAqIEByZXR1cm5zIHtPYmplY3R9IEFuIE9iamVjdCB3aGljaCBpbmNsdWRlcyB0aGUgcmVxdWlyZWQgZGF0YS5cbiAgICAgICAgICovXG4gICAgICAgIHByZXBhcmVMaWdodGJveDogZnVuY3Rpb24oICRvYmogKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gdmlld2VyLnJlc3BvbnNpdmVDb2x1bW5HYWxsZXJ5LnByZXBhcmVMaWdodGJveCgpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIucmVzcG9uc2l2ZUNvbHVtbkdhbGxlcnkucHJlcGFyZUxpZ2h0Ym94OiAkb2JqID0gJywgJG9iaiApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgdmFyIGxpZ2h0Ym94RGF0YSA9IHt9O1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBsaWdodGJveERhdGEuc3JjID0gJG9iai5hdHRyKCAnc3JjJyApO1xuICAgICAgICAgICAgbGlnaHRib3hEYXRhLmNhcHRpb24gPSAkb2JqLmF0dHIoICdhbHQnICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHJldHVybiBsaWdodGJveERhdGE7XG4gICAgICAgIH0sXG4gICAgICAgIC8qKlxuICAgICAgICAgKiBNZXRob2Qgd2hpY2ggcmVuZGVycyBhIGxpZ2h0Ym94IGZvciB0aGUgc2VsZWN0ZWQgaW1hZ2UuXG4gICAgICAgICAqIFxuICAgICAgICAgKiBAbWV0aG9kIHJlbmRlckxpZ2h0Ym94XG4gICAgICAgICAqIEBwYXJhbSB7T2JqZWN0fSBkYXRhIEFuIG9iamVjdCB3aGljaCBpbmNsdWRlcyB0aGUgcmVxdWlyZWQgZGF0YS5cbiAgICAgICAgICogQHJldHVybnMge1N0cmluZ30gQSBIVE1MLVN0cmluZyB3aGljaCByZW5kZXJzIHRoZSBsaWdodGJveC5cbiAgICAgICAgICovXG4gICAgICAgIHJlbmRlckxpZ2h0Ym94OiBmdW5jdGlvbiggZGF0YSApIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSB2aWV3ZXIucmVzcG9uc2l2ZUNvbHVtbkdhbGxlcnkucmVuZGVyTGlnaHRib3goKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLnJlc3BvbnNpdmVDb2x1bW5HYWxsZXJ5LnJlbmRlckxpZ2h0Ym94OiBkYXRhID0gJywgZGF0YSApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgdmFyIGxpZ2h0Ym94ID0gJyc7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGxpZ2h0Ym94ICs9ICc8ZGl2IGNsYXNzPVwicmNnLWxpZ2h0Ym94LW92ZXJsYXlcIj4nO1xuICAgICAgICAgICAgbGlnaHRib3ggKz0gJzxkaXYgY2xhc3M9XCJyY2ctbGlnaHRib3gtYm9keVwiPic7XG4gICAgICAgICAgICBsaWdodGJveCArPSAnPGRpdiBjbGFzcz1cInJjZy1saWdodGJveC1jbG9zZVwiIHRpdGxlPVwiJyArIF9kZWZhdWx0cy5sYW5nLmNsb3NlICsgJ1wiPjxzcGFuIGNsYXNzPVwiZ2x5cGhpY29uIGdseXBoaWNvbi1yZW1vdmVcIj48L3NwYW4+PC9kaXY+JztcbiAgICAgICAgICAgIGxpZ2h0Ym94ICs9ICc8ZGl2IGNsYXNzPVwicmNnLWxpZ2h0Ym94LWltYWdlXCI+JztcbiAgICAgICAgICAgIGxpZ2h0Ym94ICs9ICc8aW1nIHNyYz1cIicgKyBkYXRhLnNyYyArICdcIiBhbHQ9XCInICsgZGF0YS5hbHQgKyAnXCIgLz4nO1xuICAgICAgICAgICAgbGlnaHRib3ggKz0gJzwvZGl2Pic7IC8vIC5yY2ctbGlnaHRib3gtaW1hZ2VcbiAgICAgICAgICAgIGlmICggX2RlZmF1bHRzLmxpZ2h0Ym94LmNhcHRpb24gKSB7XG4gICAgICAgICAgICAgICAgbGlnaHRib3ggKz0gJzxkaXYgY2xhc3M9XCJyY2ctbGlnaHRib3gtY2FwdGlvblwiPic7XG4gICAgICAgICAgICAgICAgbGlnaHRib3ggKz0gJzxwPicgKyBkYXRhLmNhcHRpb24gKyAnPC9wPic7XG4gICAgICAgICAgICAgICAgbGlnaHRib3ggKz0gJzwvZGl2Pic7IC8vIC5yY2ctbGlnaHRib3gtY2FwdGlvblxuICAgICAgICAgICAgfVxuICAgICAgICAgICAgbGlnaHRib3ggKz0gJzwvZGl2Pic7IC8vIC5yY2ctbGlnaHRib3gtYm9keVxuICAgICAgICAgICAgbGlnaHRib3ggKz0gJzwvZGl2Pic7IC8vIC5yY2ctbGlnaHRib3gtb3ZlcmxheVxuICAgICAgICAgICAgXG4gICAgICAgICAgICByZXR1cm4gbGlnaHRib3g7XG4gICAgICAgIH0sXG4gICAgICAgIC8qKlxuICAgICAgICAgKiBNZXRob2Qgd2hpY2ggY2VudGVycyB0aGUgZ2l2ZW4gb2JqZWN0IHRvIHRoZSB2aWV3cG9ydC5cbiAgICAgICAgICogXG4gICAgICAgICAqIEBtZXRob2QgY2VudGVyTGlnaHRib3hcbiAgICAgICAgICogQHBhcmFtIHtPYmplY3R9ICRvYmogQW4galF1ZXJ5IG9iamVjdCBvZiB0aGUgZWxlbWVudCB0byBjZW50ZXIuXG4gICAgICAgICAqL1xuICAgICAgICBjZW50ZXJMaWdodGJveDogZnVuY3Rpb24oICRvYmogKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gdmlld2VyLnJlc3BvbnNpdmVDb2x1bW5HYWxsZXJ5LmNlbnRlckxpZ2h0Ym94KCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci5yZXNwb25zaXZlQ29sdW1uR2FsbGVyeS5jZW50ZXJMaWdodGJveDogJG9iaiA9ICcsICRvYmogKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgdmFyIGJveFdpZHRoID0gJG9iai5vdXRlcldpZHRoKCk7XG4gICAgICAgICAgICB2YXIgYm94SGVpZ2h0ID0gJG9iai5vdXRlckhlaWdodCgpO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICAkb2JqLmNzcygge1xuICAgICAgICAgICAgICAgICdtYXJnaW4tdG9wJzogJy0nICsgYm94SGVpZ2h0IC8gMiArICdweCcsXG4gICAgICAgICAgICAgICAgJ21hcmdpbi1sZWZ0JzogJy0nICsgYm94V2lkdGggLyAyICsgJ3B4J1xuICAgICAgICAgICAgfSApO1xuICAgICAgICB9LFxuICAgIH07XG4gICAgXG4gICAgcmV0dXJuIHZpZXdlcjtcbiAgICBcbn0gKSggdmlld2VySlMgfHwge30sIGpRdWVyeSApO1xuIiwiLyoqXG4gKiBUaGlzIGZpbGUgaXMgcGFydCBvZiB0aGUgR29vYmkgdmlld2VyIC0gYSBjb250ZW50IHByZXNlbnRhdGlvbiBhbmQgbWFuYWdlbWVudFxuICogYXBwbGljYXRpb24gZm9yIGRpZ2l0aXplZCBvYmplY3RzLlxuICogXG4gKiBWaXNpdCB0aGVzZSB3ZWJzaXRlcyBmb3IgbW9yZSBpbmZvcm1hdGlvbi4gLSBodHRwOi8vd3d3LmludHJhbmRhLmNvbSAtXG4gKiBodHRwOi8vZGlnaXZlcnNvLmNvbVxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZnJlZSBzb2Z0d2FyZTsgeW91IGNhbiByZWRpc3RyaWJ1dGUgaXQgYW5kL29yIG1vZGlmeSBpdCB1bmRlciB0aGUgdGVybXNcbiAqIG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhcyBwdWJsaXNoZWQgYnkgdGhlIEZyZWUgU29mdHdhcmUgRm91bmRhdGlvbjsgZWl0aGVyXG4gKiB2ZXJzaW9uIDIgb2YgdGhlIExpY2Vuc2UsIG9yIChhdCB5b3VyIG9wdGlvbikgYW55IGxhdGVyIHZlcnNpb24uXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBkaXN0cmlidXRlZCBpbiB0aGUgaG9wZSB0aGF0IGl0IHdpbGwgYmUgdXNlZnVsLCBidXQgV0lUSE9VVCBBTllcbiAqIFdBUlJBTlRZOyB3aXRob3V0IGV2ZW4gdGhlIGltcGxpZWQgd2FycmFudHkgb2YgTUVSQ0hBTlRBQklMSVRZIG9yIEZJVE5FU1MgRk9SIEFcbiAqIFBBUlRJQ1VMQVIgUFVSUE9TRS4gU2VlIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBmb3IgbW9yZSBkZXRhaWxzLlxuICogXG4gKiBZb3Ugc2hvdWxkIGhhdmUgcmVjZWl2ZWQgYSBjb3B5IG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhbG9uZyB3aXRoIHRoaXNcbiAqIHByb2dyYW0uIElmIG5vdCwgc2VlIDxodHRwOi8vd3d3LmdudS5vcmcvbGljZW5zZXMvPi5cbiAqIFxuICogTW9kdWxlIHdoaWNoIHNldHMgdXAgdGhlIGZ1bmN0aW9uYWxpdHkgZm9yIHNlYXJjaCBhZHZhbmNlZC5cbiAqIFxuICogQHZlcnNpb24gMy4yLjBcbiAqIEBtb2R1bGUgdmlld2VySlMuc2VhcmNoQWR2YW5jZWRcbiAqIEByZXF1aXJlcyBqUXVlcnlcbiAqL1xudmFyIHZpZXdlckpTID0gKCBmdW5jdGlvbiggdmlld2VyICkge1xuICAgICd1c2Ugc3RyaWN0JztcbiAgICBcbiAgICB2YXIgX2RlYnVnID0gZmFsc2U7XG4gICAgdmFyIF9hZHZTZWFyY2hWYWx1ZXMgPSB7fTtcbiAgICB2YXIgX2RlZmF1bHRzID0ge1xuICAgICAgICBsb2FkZXJTZWxlY3RvcjogJy5zZWFyY2gtYWR2YW5jZWRfX2xvYWRlcicsXG4gICAgICAgIGlucHV0U2VsZWN0b3I6ICcudmFsdWUtdGV4dCcsXG4gICAgICAgIHJlc2V0U2VsZWN0b3I6ICcucmVzZXQnLFxuICAgIH07XG4gICAgXG4gICAgdmlld2VyLnNlYXJjaEFkdmFuY2VkID0ge1xuICAgICAgICAvKipcbiAgICAgICAgICogTWV0aG9kIHRvIGluaXRpYWxpemUgdGhlIHNlYXJjaCBhZHZhbmNlZCBmZWF0dXJlcy5cbiAgICAgICAgICogXG4gICAgICAgICAqIEBtZXRob2QgaW5pdFxuICAgICAgICAgKiBAcGFyYW0ge09iamVjdH0gY29uZmlnIEFuIGNvbmZpZyBvYmplY3Qgd2hpY2ggb3ZlcndyaXRlcyB0aGUgZGVmYXVsdHMuXG4gICAgICAgICAqL1xuICAgICAgICBpbml0OiBmdW5jdGlvbiggY29uZmlnICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICcjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIuc2VhcmNoQWR2YW5jZWQuaW5pdCcgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci5zZWFyY2hBZHZhbmNlZC5pbml0OiBjb25maWcgPSAnLCBjb25maWcgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgJC5leHRlbmQoIHRydWUsIF9kZWZhdWx0cywgY29uZmlnICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIGluaXQgYnMgdG9vbHRpcHNcbiAgICAgICAgICAgICQoICdbZGF0YS10b2dnbGU9XCJ0b29sdGlwXCJdJyApLnRvb2x0aXAoKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgaWYgKCB2aWV3ZXIubG9jYWxTdG9yYWdlUG9zc2libGUgKSB7XG4gICAgICAgICAgICAgICAgbG9jYWxTdG9yYWdlLnNldEl0ZW0oICdhZHZTZWFyY2hWYWx1ZXMnLCBKU09OLnN0cmluZ2lmeSggX2FkdlNlYXJjaFZhbHVlcyApICk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLy8gc2V0IHNlYXJjaCB2YWx1ZXNcbiAgICAgICAgICAgICAgICBfc2V0QWR2U2VhcmNoVmFsdWVzKCk7XG4gICAgICAgICAgICAgICAgX3Jlc2V0VmFsdWUoKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAvLyBhamF4IGV2ZW50bGlzdGVuZXJcbiAgICAgICAgICAgICAgICBqc2YuYWpheC5hZGRPbkV2ZW50KCBmdW5jdGlvbiggZGF0YSApIHtcbiAgICAgICAgICAgICAgICAgICAgdmFyIGFqYXhzdGF0dXMgPSBkYXRhLnN0YXR1cztcbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgIHN3aXRjaCAoIGFqYXhzdGF0dXMgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBjYXNlIFwiYmVnaW5cIjpcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyBzaG93IGxvYWRlclxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICQoIF9kZWZhdWx0cy5sb2FkZXJTZWxlY3RvciApLnNob3coKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICAgICAgICAgIGNhc2UgXCJzdWNjZXNzXCI6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8gaW5pdCBicyB0b29sdGlwc1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICQoICdbZGF0YS10b2dnbGU9XCJ0b29sdGlwXCJdJyApLnRvb2x0aXAoKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyBzZXQgc2VhcmNoIHZhbHVlc1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIF9zZXRBZHZTZWFyY2hWYWx1ZXMoKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBfZ2V0QWR2U2VhcmNoVmFsdWVzKCk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgX3Jlc2V0VmFsdWUoKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyBzZXQgZGlzYWJsZWQgc3RhdGUgdG8gc2VsZWN0IHdyYXBwZXJcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCAnc2VsZWN0JyApLmVhY2goIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBpZiAoICQoIHRoaXMgKS5hdHRyKCAnZGlzYWJsZWQnICkgPT09ICdkaXNhYmxlZCcgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCB0aGlzICkucGFyZW50KCkuYWRkQ2xhc3MoICdkaXNhYmxlZCcgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICQoIHRoaXMgKS5wYXJlbnQoKS5yZW1vdmVDbGFzcyggJ2Rpc2FibGVkJyApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIGhpZGUgbG9hZGVyXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgJCggX2RlZmF1bHRzLmxvYWRlclNlbGVjdG9yICkuaGlkZSgpO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIGZhbHNlO1xuICAgICAgICAgICAgfVxuICAgICAgICB9LFxuICAgIH07XG4gICAgXG4gICAgZnVuY3Rpb24gX3NldEFkdlNlYXJjaFZhbHVlcygpIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX3NldEFkdlNlYXJjaFZhbHVlcygpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgICQoIF9kZWZhdWx0cy5pbnB1dFNlbGVjdG9yICkub2ZmKCkub24oICdrZXl1cCcsIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgdmFyIGN1cnJJZCA9ICQoIHRoaXMgKS5hdHRyKCAnaWQnICk7XG4gICAgICAgICAgICB2YXIgY3VyclZhbCA9ICQoIHRoaXMgKS52YWwoKTtcbiAgICAgICAgICAgIHZhciBjdXJyVmFsdWVzID0gSlNPTi5wYXJzZSggbG9jYWxTdG9yYWdlLmdldEl0ZW0oICdhZHZTZWFyY2hWYWx1ZXMnICkgKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gY2hlY2sgaWYgdmFsdWVzIGFyZSBpbiBsb2NhbCBzdG9yYWdlXG4gICAgICAgICAgICBpZiAoICFjdXJyVmFsdWVzLmhhc093blByb3BlcnR5KCBjdXJyVmFsICkgKSB7XG4gICAgICAgICAgICAgICAgY3VyclZhbHVlc1sgY3VycklkIF0gPSBjdXJyVmFsO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIGZhbHNlO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICAvLyB3cml0ZSB2YWx1ZXMgdG8gbG9jYWwgc3RvcmFnZVxuICAgICAgICAgICAgbG9jYWxTdG9yYWdlLnNldEl0ZW0oICdhZHZTZWFyY2hWYWx1ZXMnLCBKU09OLnN0cmluZ2lmeSggY3VyclZhbHVlcyApICk7XG4gICAgICAgIH0gKTtcbiAgICB9XG4gICAgXG4gICAgZnVuY3Rpb24gX2dldEFkdlNlYXJjaFZhbHVlcygpIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX2dldEFkdlNlYXJjaFZhbHVlcygpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIHZhciB2YWx1ZXMgPSBKU09OLnBhcnNlKCBsb2NhbFN0b3JhZ2UuZ2V0SXRlbSggJ2FkdlNlYXJjaFZhbHVlcycgKSApO1xuICAgICAgICBcbiAgICAgICAgJC5lYWNoKCB2YWx1ZXMsIGZ1bmN0aW9uKCBpZCwgdmFsdWUgKSB7XG4gICAgICAgICAgICAkKCAnIycgKyBpZCApLnZhbCggdmFsdWUgKTtcbiAgICAgICAgfSApO1xuICAgIH1cbiAgICBcbiAgICBmdW5jdGlvbiBfcmVzZXRWYWx1ZSgpIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX3Jlc2V0VmFsdWUoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICAkKCBfZGVmYXVsdHMucmVzZXRTZWxlY3RvciApLm9mZigpLm9uKCAnY2xpY2snLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIHZhciBpbnB1dElkID0gJCggdGhpcyApLnBhcmVudHMoICcuaW5wdXQtZ3JvdXAnICkuZmluZCggJ2lucHV0JyApLmF0dHIoICdpZCcgKTtcbiAgICAgICAgICAgIHZhciBjdXJyVmFsdWVzID0gSlNPTi5wYXJzZSggbG9jYWxTdG9yYWdlLmdldEl0ZW0oICdhZHZTZWFyY2hWYWx1ZXMnICkgKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gZGVsZXRlIHZhbHVlIGZyb20gbG9jYWwgc3RvcmFnZSBvYmplY3RcbiAgICAgICAgICAgIGlmICggY3VyclZhbHVlcy5oYXNPd25Qcm9wZXJ0eSggaW5wdXRJZCApICkge1xuICAgICAgICAgICAgICAgIGRlbGV0ZSBjdXJyVmFsdWVzWyBpbnB1dElkIF07XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIHdyaXRlIG5ldyB2YWx1ZXMgdG8gbG9jYWwgc3RvcmFnZVxuICAgICAgICAgICAgbG9jYWxTdG9yYWdlLnNldEl0ZW0oICdhZHZTZWFyY2hWYWx1ZXMnLCBKU09OLnN0cmluZ2lmeSggY3VyclZhbHVlcyApICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgICQoIHRoaXMgKS5wYXJlbnRzKCAnLmlucHV0LWdyb3VwJyApLmZpbmQoICdpbnB1dCcgKS52YWwoICcnICk7XG4gICAgICAgIH0gKTtcbiAgICB9XG4gICAgXG4gICAgcmV0dXJuIHZpZXdlcjtcbiAgICBcbn0gKSggdmlld2VySlMgfHwge30sIGpRdWVyeSApO1xuIiwiLyoqXG4gKiBUaGlzIGZpbGUgaXMgcGFydCBvZiB0aGUgR29vYmkgdmlld2VyIC0gYSBjb250ZW50IHByZXNlbnRhdGlvbiBhbmQgbWFuYWdlbWVudFxuICogYXBwbGljYXRpb24gZm9yIGRpZ2l0aXplZCBvYmplY3RzLlxuICogXG4gKiBWaXNpdCB0aGVzZSB3ZWJzaXRlcyBmb3IgbW9yZSBpbmZvcm1hdGlvbi4gLSBodHRwOi8vd3d3LmludHJhbmRhLmNvbSAtXG4gKiBodHRwOi8vZGlnaXZlcnNvLmNvbVxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZnJlZSBzb2Z0d2FyZTsgeW91IGNhbiByZWRpc3RyaWJ1dGUgaXQgYW5kL29yIG1vZGlmeSBpdCB1bmRlciB0aGUgdGVybXNcbiAqIG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhcyBwdWJsaXNoZWQgYnkgdGhlIEZyZWUgU29mdHdhcmUgRm91bmRhdGlvbjsgZWl0aGVyXG4gKiB2ZXJzaW9uIDIgb2YgdGhlIExpY2Vuc2UsIG9yIChhdCB5b3VyIG9wdGlvbikgYW55IGxhdGVyIHZlcnNpb24uXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBkaXN0cmlidXRlZCBpbiB0aGUgaG9wZSB0aGF0IGl0IHdpbGwgYmUgdXNlZnVsLCBidXQgV0lUSE9VVCBBTllcbiAqIFdBUlJBTlRZOyB3aXRob3V0IGV2ZW4gdGhlIGltcGxpZWQgd2FycmFudHkgb2YgTUVSQ0hBTlRBQklMSVRZIG9yIEZJVE5FU1MgRk9SIEFcbiAqIFBBUlRJQ1VMQVIgUFVSUE9TRS4gU2VlIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBmb3IgbW9yZSBkZXRhaWxzLlxuICogXG4gKiBZb3Ugc2hvdWxkIGhhdmUgcmVjZWl2ZWQgYSBjb3B5IG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhbG9uZyB3aXRoIHRoaXNcbiAqIHByb2dyYW0uIElmIG5vdCwgc2VlIDxodHRwOi8vd3d3LmdudS5vcmcvbGljZW5zZXMvPi5cbiAqIFxuICogTW9kdWxlIHdoaWNoIHNldHMgdXAgdGhlIGZ1bmN0aW9uYWxpdHkgZm9yIHNlYXJjaCBsaXN0LlxuICogXG4gKiBAdmVyc2lvbiAzLjIuMFxuICogQG1vZHVsZSB2aWV3ZXJKUy5zZWFyY2hMaXN0XG4gKiBAcmVxdWlyZXMgalF1ZXJ5XG4gKi9cbnZhciB2aWV3ZXJKUyA9ICggZnVuY3Rpb24oIHZpZXdlciApIHtcbiAgICAndXNlIHN0cmljdCc7XG4gICAgXG4gICAgdmFyIF9kZWJ1ZyA9IGZhbHNlO1xuICAgIHZhciBfcHJvbWlzZSA9IG51bGw7XG4gICAgdmFyIF9jaGlsZEhpdHMgPSBudWxsO1xuICAgIHZhciBfZGVmYXVsdHMgPSB7XG4gICAgICAgIGNvbnRleHRQYXRoOiAnJyxcbiAgICAgICAgcmVzdEFwaVBhdGg6ICcvcmVzdC9zZWFyY2gvaGl0LycsXG4gICAgICAgIGhpdHNQZXJDYWxsOiAyMCxcbiAgICAgICAgcmVzZXRTZWFyY2hTZWxlY3RvcjogJyNyZXNldEN1cnJlbnRTZWFyY2gnLFxuICAgICAgICBzZWFyY2hJbnB1dFNlbGVjdG9yOiAnI2N1cnJlbnRTZWFyY2hJbnB1dCcsXG4gICAgICAgIHNlYXJjaFRyaWdnZXJTZWxlY3RvcjogJyNzbEN1cnJlbnRTZWFyY2hUcmlnZ2VyJyxcbiAgICAgICAgc2F2ZVNlYXJjaE1vZGFsU2VsZWN0b3I6ICcjc2F2ZVNlYXJjaE1vZGFsJyxcbiAgICAgICAgc2F2ZVNlYXJjaElucHV0U2VsZWN0b3I6ICcjc2F2ZVNlYXJjaElucHV0JyxcbiAgICAgICAgZXhjZWxFeHBvcnRTZWxlY3RvcjogJy5leGNlbC1leHBvcnQtdHJpZ2dlcicsXG4gICAgICAgIGV4Y2VsRXhwb3J0TG9hZGVyU2VsZWN0b3I6ICcuZXhjZWwtZXhwb3J0LWxvYWRlcicsXG4gICAgICAgIGhpdENvbnRlbnRMb2FkZXJTZWxlY3RvcjogJy5zZWFyY2gtbGlzdF9fbG9hZGVyJyxcbiAgICAgICAgaGl0Q29udGVudFNlbGVjdG9yOiAnLnNlYXJjaC1saXN0X19oaXQtY29udGVudCcsXG4gICAgICAgIG1zZzoge1xuICAgICAgICAgICAgZ2V0TW9yZUNoaWxkcmVuOiAnTWVociBUcmVmZmVyIGxhZGVuJyxcbiAgICAgICAgfVxuICAgIH07XG4gICAgXG4gICAgdmlld2VyLnNlYXJjaExpc3QgPSB7XG4gICAgICAgIC8qKlxuICAgICAgICAgKiBNZXRob2QgdG8gaW5pdGlhbGl6ZSB0aGUgc2VhcmNoIGxpc3QgZmVhdHVyZXMuXG4gICAgICAgICAqIFxuICAgICAgICAgKiBAbWV0aG9kIGluaXRcbiAgICAgICAgICogQHBhcmFtIHtPYmplY3R9IGNvbmZpZyBBbiBjb25maWcgb2JqZWN0IHdoaWNoIG92ZXJ3cml0ZXMgdGhlIGRlZmF1bHRzLlxuICAgICAgICAgKi9cbiAgICAgICAgaW5pdDogZnVuY3Rpb24oIGNvbmZpZyApIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLnNlYXJjaExpc3QuaW5pdCcgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci5zZWFyY2hMaXN0LmluaXQ6IGNvbmZpZyA9ICcsIGNvbmZpZyApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICAkLmV4dGVuZCggdHJ1ZSwgX2RlZmF1bHRzLCBjb25maWcgKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gaW5pdCBicyB0b29sdGlwc1xuICAgICAgICAgICAgJCggJ1tkYXRhLXRvZ2dsZT1cInRvb2x0aXBcIl0nICkudG9vbHRpcCgpO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICAvLyBmb2N1cyBzYXZlIHNlYXJjaCBtb2RhbCBpbnB1dCBvbiBzaG93XG4gICAgICAgICAgICAkKCBfZGVmYXVsdHMuc2F2ZVNlYXJjaE1vZGFsU2VsZWN0b3IgKS5vbiggJ3Nob3duLmJzLm1vZGFsJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgJCggX2RlZmF1bHRzLnNhdmVTZWFyY2hJbnB1dFNlbGVjdG9yICkuZm9jdXMoKTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gcmVzZXQgY3VycmVudCBzZWFyY2ggYW5kIHJlZGlyZWN0IHRvIHN0YW5kYXJkIHNlYXJjaFxuICAgICAgICAgICAgJCggX2RlZmF1bHRzLnJlc2V0U2VhcmNoU2VsZWN0b3IgKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgJCggX2RlZmF1bHRzLnNlYXJjaElucHV0U2VsZWN0b3IgKS52YWwoICcnICk7XG4gICAgICAgICAgICAgICAgbG9jYXRpb24uaHJlZiA9IF9kZWZhdWx0cy5jb250ZXh0UGF0aCArICcvc2VhcmNoLyc7XG4gICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIHNob3cvaGlkZSBsb2FkZXIgZm9yIGV4Y2VsIGV4cG9ydFxuICAgICAgICAgICAgJCggX2RlZmF1bHRzLmV4Y2VsRXhwb3J0U2VsZWN0b3IgKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgdmFyIHRyaWdnZXIgPSAkKCB0aGlzICk7XG4gICAgICAgICAgICAgICAgdmFyIGV4Y2VsTG9hZGVyID0gJCggX2RlZmF1bHRzLmV4Y2VsRXhwb3J0TG9hZGVyU2VsZWN0b3IgKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICB0cmlnZ2VyLmhpZGUoKTtcbiAgICAgICAgICAgICAgICBleGNlbExvYWRlci5zaG93KCk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgdmFyIHVybCA9IF9kZWZhdWx0cy5jb250ZXh0UGF0aCArICcvcmVzdC9kb3dubG9hZC9zZWFyY2gvd2FpdEZvci8nO1xuICAgICAgICAgICAgICAgIHZhciBwcm9taXNlID0gUSggJC5hamF4KCB7XG4gICAgICAgICAgICAgICAgICAgIHVybDogZGVjb2RlVVJJKCB1cmwgKSxcbiAgICAgICAgICAgICAgICAgICAgdHlwZTogXCJHRVRcIixcbiAgICAgICAgICAgICAgICAgICAgZGF0YVR5cGU6IFwidGV4dFwiLFxuICAgICAgICAgICAgICAgICAgICBhc3luYzogdHJ1ZVxuICAgICAgICAgICAgICAgIH0gKSApO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIHByb21pc2UudGhlbiggZnVuY3Rpb24oIGRhdGEgKSB7XG4gICAgICAgICAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgY29uc29sZS5sb2coXCJEb3dubG9hZCBzdGFydGVkXCIpO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICBleGNlbExvYWRlci5oaWRlKCk7XG4gICAgICAgICAgICAgICAgICAgIHRyaWdnZXIuc2hvdygpO1xuICAgICAgICAgICAgICAgIH0gKS5jYXRjaCggZnVuY3Rpb24oIGVycm9yICkge1xuICAgICAgICAgICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKFwiRXJyb3IgZG93bmxvYWRpbmcgZXhjZWwgc2hlZXQ6IFwiLCBlcnJvci5yZXNwb25zZVRleHQpO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICBleGNlbExvYWRlci5oaWRlKCk7XG4gICAgICAgICAgICAgICAgICAgIHRyaWdnZXIuc2hvdygpO1xuICAgICAgICAgICAgICAgIH0pOyAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gZ2V0IGNoaWxkIGhpdHNcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgJCggJ1tkYXRhLXRvZ2dsZT1cImhpdC1jb250ZW50XCJdJyApLmVhY2goIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgIHZhciBjdXJyQnRuID0gJCggdGhpcyApO1xuICAgICAgICAgICAgICAgIHZhciBjdXJySWREb2MgPSAkKCB0aGlzICkuYXR0ciggJ2RhdGEtaWRkb2MnICk7XG4gICAgICAgICAgICAgICAgdmFyIGN1cnJVcmwgPSBfZ2V0QXBpVXJsKCBjdXJySWREb2MsIF9kZWZhdWx0cy5oaXRzUGVyQ2FsbCApO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ0N1cnJlbnQgQVBJIENhbGwgVVJMOiAnLCBjdXJyVXJsICk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIF9wcm9taXNlID0gdmlld2VyLmhlbHBlci5nZXRSZW1vdGVEYXRhKCBjdXJyVXJsICk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgY3VyckJ0bi5maW5kKCBfZGVmYXVsdHMuaGl0Q29udGVudExvYWRlclNlbGVjdG9yICkuY3NzKCAnZGlzcGxheScsICdpbmxpbmUtYmxvY2snICk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLy8gZ2V0IGRhdGEgYW5kIHJlbmRlciBoaXRzIGlmIGRhdGEgaXMgdmFsaWRcbiAgICAgICAgICAgICAgICBfcHJvbWlzZS50aGVuKCBmdW5jdGlvbiggZGF0YSApIHtcbiAgICAgICAgICAgICAgICAgICAgaWYgKCBkYXRhLmhpdHNEaXNwbGF5ZWQgPCBfZGVmYXVsdHMuaGl0c1BlckNhbGwgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAvLyByZW5kZXIgY2hpbGQgaGl0cyBpbnRvIHRoZSBET01cbiAgICAgICAgICAgICAgICAgICAgICAgIF9yZW5kZXJDaGlsZEhpdHMoIGRhdGEsIGN1cnJCdG4gKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIC8vIHNldCBjdXJyZW50IGJ1dHRvbiBhY3RpdmUsIHJlbW92ZSBsb2FkZXIgYW5kIHNob3cgY29udGVudFxuICAgICAgICAgICAgICAgICAgICAgICAgY3VyckJ0bi50b2dnbGVDbGFzcyggJ2luJyApLmZpbmQoIF9kZWZhdWx0cy5oaXRDb250ZW50TG9hZGVyU2VsZWN0b3IgKS5oaWRlKCk7XG4gICAgICAgICAgICAgICAgICAgICAgICBjdXJyQnRuLm5leHQoKS5zaG93KCk7XG4gICAgICAgICAgICAgICAgICAgICAgICAvLyBzZXQgZXZlbnQgdG8gdG9nZ2xlIGN1cnJlbnQgaGl0c1xuICAgICAgICAgICAgICAgICAgICAgICAgY3VyckJ0bi5vZmYoKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgJCggdGhpcyApLnRvZ2dsZUNsYXNzKCAnaW4nICkubmV4dCgpLnNsaWRlVG9nZ2xlKCk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAvLyByZW1vdmUgbG9hZGVyXG4gICAgICAgICAgICAgICAgICAgICAgICBjdXJyQnRuLmZpbmQoIF9kZWZhdWx0cy5oaXRDb250ZW50TG9hZGVyU2VsZWN0b3IgKS5oaWRlKCk7XG4gICAgICAgICAgICAgICAgICAgICAgICAvLyBzZXQgZXZlbnQgdG8gdG9nZ2xlIGN1cnJlbnQgaGl0c1xuICAgICAgICAgICAgICAgICAgICAgICAgY3VyckJ0bi5vZmYoKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8gcmVuZGVyIGNoaWxkIGhpdHMgaW50byB0aGUgRE9NXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgX3JlbmRlckNoaWxkSGl0cyggZGF0YSwgY3VyckJ0biApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIGNoZWNrIGlmIG1vcmUgY2hpbGRyZW4gZXhpc3QgYW5kIHJlbmRlciBsaW5rXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgX3JlbmRlckdldE1vcmVDaGlsZHJlbiggZGF0YSwgY3VycklkRG9jLCBjdXJyQnRuICk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgJCggdGhpcyApLnRvZ2dsZUNsYXNzKCAnaW4nICkubmV4dCgpLnNsaWRlVG9nZ2xlKCk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9ICkudGhlbiggbnVsbCwgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgIGN1cnJCdG4ubmV4dCgpLmFwcGVuZCggdmlld2VyLmhlbHBlci5yZW5kZXJBbGVydCggJ2FsZXJ0LWRhbmdlcicsICc8c3Ryb25nPlN0YXR1czogPC9zdHJvbmc+JyArIGVycm9yLnN0YXR1cyArICcgJyArIGVycm9yLnN0YXR1c1RleHQsIGZhbHNlICkgKTtcbiAgICAgICAgICAgICAgICAgICAgY29uc29sZS5lcnJvciggJ0VSUk9SOiB2aWV3ZXIuc2VhcmNoTGlzdC5pbml0IC0gJywgZXJyb3IgKTtcbiAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICB9ICk7XG4gICAgICAgIH0sXG4gICAgfTtcbiAgICBcbiAgICAvKipcbiAgICAgKiBNZXRob2QgdG8gZ2V0IHRoZSBmdWxsIFJFU1QtQVBJIFVSTC5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9nZXRBcGlVcmxcbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gaWQgVGhlIGN1cnJlbnQgSUREb2Mgb2YgdGhlIGhpdCBzZXQuXG4gICAgICogQHJldHVybnMge1N0cmluZ30gVGhlIGZ1bGwgUkVTVC1BUEkgVVJMLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9nZXRBcGlVcmwoIGlkLCBoaXRzICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfZ2V0QXBpVXJsKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX2dldEFwaVVybDogaWQgPSAnLCBpZCApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICByZXR1cm4gX2RlZmF1bHRzLmNvbnRleHRQYXRoICsgX2RlZmF1bHRzLnJlc3RBcGlQYXRoICsgaWQgKyAnLycgKyBoaXRzICsgJy8nO1xuICAgIH1cbiAgICBcbiAgICAvKipcbiAgICAgKiBNZXRob2Qgd2hpY2ggcmVuZGVycyB0aGUgY2hpbGQgaGl0cyBpbnRvIHRoZSBET00uXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfcmVuZGVyQ2hpbGRIaXRzXG4gICAgICogQHBhcmFtIHtPYmplY3R9IGRhdGEgVGhlIGRhdGEgb2JqZWN0IHdoaWNoIGNvbnRhaW5zIHRoZSBjaGlsZCBoaXRzLlxuICAgICAqIEBwYXJhbSB7T2JqZWN0fSAkdGhpcyBUaGUgY3VycmVudCBjaGlsZCBoaXRzIHRyaWdnZXIuXG4gICAgICogQHJldHVybnMge09iamVjdH0gQW4ganF1ZXJ5IG9iamVjdCB3aGljaCBjb250YWlucyB0aGUgY2hpbGQgaGl0cy5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfcmVuZGVyQ2hpbGRIaXRzKCBkYXRhLCAkdGhpcyApIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX3JlbmRlckNoaWxkSGl0cygpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19yZW5kZXJDaGlsZEhpdHM6IGRhdGEgPSAnLCBkYXRhICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19yZW5kZXJDaGlsZEhpdHM6ICR0aGlzID0gJywgJHRoaXMgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgdmFyIGhpdFNldCA9IG51bGw7XG4gICAgICAgIFxuICAgICAgICAvLyBjbGVhbiBoaXQgc2V0c1xuICAgICAgICAkdGhpcy5uZXh0KCkuZW1wdHkoKTtcbiAgICAgICAgXG4gICAgICAgIC8vIGJ1aWxkIGhpdHNcbiAgICAgICAgJC5lYWNoKCBkYXRhLmNoaWxkcmVuLCBmdW5jdGlvbiggY2hpbGRyZW4sIGNoaWxkICkge1xuICAgICAgICAgICAgaGl0U2V0ID0gJCggJzxkaXYgY2xhc3M9XCJzZWFyY2gtbGlzdF9faGl0LWNvbnRlbnQtc2V0XCIgLz4nICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIGJ1aWxkIHRpdGxlXG4gICAgICAgICAgICBoaXRTZXQuYXBwZW5kKCBfcmVuZGVySGl0U2V0VGl0bGUoIGNoaWxkLmJyb3dzZUVsZW1lbnQgKSApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICAvLyBhcHBlbmQgbWV0YWRhdGEgaWYgZXhpc3RcbiAgICAgICAgICAgIGhpdFNldC5hcHBlbmQoIF9yZW5kZXJNZXRkYXRhSW5mbyggY2hpbGQuZm91bmRNZXRhZGF0YSwgY2hpbGQudXJsICkgKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gYnVpbGQgY2hpbGQgaGl0c1xuICAgICAgICAgICAgaWYgKCBjaGlsZC5oYXNDaGlsZHJlbiApIHtcbiAgICAgICAgICAgICAgICAkLmVhY2goIGNoaWxkLmNoaWxkcmVuLCBmdW5jdGlvbiggc3ViQ2hpbGRyZW4sIHN1YkNoaWxkICkge1xuICAgICAgICAgICAgICAgICAgICBoaXRTZXQuYXBwZW5kKCBfcmVuZGVyU3ViQ2hpbGRIaXRzKCBzdWJDaGlsZC5icm93c2VFbGVtZW50LCBzdWJDaGlsZC50eXBlICkgKTtcbiAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIGFwcGVuZCBjb21wbGV0ZSBzZXRcbiAgICAgICAgICAgICR0aGlzLm5leHQoKS5hcHBlbmQoIGhpdFNldCApO1xuICAgICAgICB9ICk7XG4gICAgICAgIFxuICAgIH1cbiAgICBcbiAgICAvKipcbiAgICAgKiBNZXRob2Qgd2hpY2ggcmVuZGVycyB0aGUgaGl0IHNldCB0aXRsZS5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9yZW5kZXJIaXRTZXRUaXRsZVxuICAgICAqIEBwYXJhbSB7T2JqZWN0fSBkYXRhIFRoZSBkYXRhIG9iamVjdCB3aGljaCBjb250YWlucyB0aGUgaGl0IHNldCB0aXRsZSB2YWx1ZXMuXG4gICAgICogQHJldHVybnMge09iamVjdH0gQSBqcXVlcnkgb2JqZWN0IHdoaWNoIGNvbnRhaW5zIHRoZSBoaXQgc2V0IHRpdGxlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9yZW5kZXJIaXRTZXRUaXRsZSggZGF0YSApIHtcbiAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJy0tLS0tLS0tLS0gX3JlbmRlckhpdFNldFRpdGxlKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX3JlbmRlckhpdFNldFRpdGxlOiBkYXRhID0gJywgZGF0YSApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgaGl0U2V0VGl0bGUgPSBudWxsO1xuICAgICAgICB2YXIgaGl0U2V0VGl0bGVINSA9IG51bGw7XG4gICAgICAgIHZhciBoaXRTZXRUaXRsZURsID0gbnVsbDtcbiAgICAgICAgdmFyIGhpdFNldFRpdGxlRHQgPSBudWxsO1xuICAgICAgICB2YXIgaGl0U2V0VGl0bGVEZCA9IG51bGw7XG4gICAgICAgIHZhciBoaXRTZXRUaXRsZUxpbmsgPSBudWxsO1xuICAgICAgICBcbiAgICAgICAgaGl0U2V0VGl0bGUgPSAkKCAnPGRpdiBjbGFzcz1cInNlYXJjaC1saXN0X19zdHJ1Y3QtdGl0bGVcIiAvPicgKTtcbiAgICAgICAgaGl0U2V0VGl0bGVINSA9ICQoICc8aDUgLz4nICk7XG4gICAgICAgIGhpdFNldFRpdGxlTGluayA9ICQoICc8YSAvPicgKTtcbiAgICAgICAgaGl0U2V0VGl0bGVMaW5rLmF0dHIoICdocmVmJywgX2RlZmF1bHRzLmNvbnRleHRQYXRoICsgJy8nICsgZGF0YS51cmwgKTtcbiAgICAgICAgaGl0U2V0VGl0bGVMaW5rLmFwcGVuZCggZGF0YS5sYWJlbFNob3J0ICk7XG4gICAgICAgIGhpdFNldFRpdGxlSDUuYXBwZW5kKCBoaXRTZXRUaXRsZUxpbmsgKTtcbiAgICAgICAgaGl0U2V0VGl0bGUuYXBwZW5kKCBoaXRTZXRUaXRsZUg1ICk7XG4gICAgICAgIFxuICAgICAgICByZXR1cm4gaGl0U2V0VGl0bGU7XG4gICAgfVxuICAgIFxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB3aGljaCByZW5kZXJzIG1ldGFkYXRhIGluZm8uXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfcmVuZGVyTWV0ZGF0YUluZm9cbiAgICAgKiBAcGFyYW0ge09iamVjdH0gZGF0YSBUaGUgZGF0YSBvYmplY3Qgd2hpY2ggY29udGFpbnMgdGhlIHN1YiBoaXQgdmFsdWVzLlxuICAgICAqIEBwYXJhbSB7U3RyaW5nfSB1cmwgVGhlIFVSTCBmb3IgdGhlIGN1cnJlbnQgd29yay5cbiAgICAgKiBAcmV0dXJucyB7T2JqZWN0fSBBIGpxdWVyeSBvYmplY3Qgd2hpY2ggY29udGFpbnMgdGhlIG1ldGFkYXRhIGluZm8uXG4gICAgICovXG4gICAgZnVuY3Rpb24gX3JlbmRlck1ldGRhdGFJbmZvKCBkYXRhLCB1cmwgKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICctLS0tLS0tLS0tIF9yZW5kZXJNZXRkYXRhSW5mbygpIC0tLS0tLS0tLS0nICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ19yZW5kZXJNZXRkYXRhSW5mbzogZGF0YSA9ICcsIGRhdGEgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX3JlbmRlck1ldGRhdGFJbmZvOiB1cmwgPSAnLCB1cmwgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgdmFyIG1ldGFkYXRhV3JhcHBlciA9IG51bGw7XG4gICAgICAgIHZhciBtZXRhZGF0YVRhYmxlID0gbnVsbDtcbiAgICAgICAgdmFyIG1ldGFkYXRhVGFibGVCb2R5ID0gbnVsbDtcbiAgICAgICAgdmFyIG1ldGFkYXRhVGFibGVSb3cgPSBudWxsO1xuICAgICAgICB2YXIgbWV0YWRhdGFUYWJsZUNlbGxMZWZ0ID0gbnVsbDtcbiAgICAgICAgdmFyIG1ldGFkYXRhVGFibGVDZWxsUmlnaHQgPSBudWxsO1xuICAgICAgICB2YXIgbWV0YWRhdGFLZXlJY29uID0gbnVsbDtcbiAgICAgICAgdmFyIG1ldGFkYXRhS2V5TGluayA9IG51bGw7XG4gICAgICAgIHZhciBtZXRhZGF0YVZhbHVlTGluayA9IG51bGw7XG4gICAgICAgIFxuICAgICAgICBpZiAoICEkLmlzRW1wdHlPYmplY3QoIGRhdGEgKSApIHtcbiAgICAgICAgICAgIG1ldGFkYXRhV3JhcHBlciA9ICQoICc8ZGl2IGNsYXNzPVwic2VhcmNoLWxpc3RfX21ldGFkYXRhLWluZm9cIiAvPicgKTtcbiAgICAgICAgICAgIG1ldGFkYXRhVGFibGUgPSAkKCAnPHRhYmxlIC8+JyApO1xuICAgICAgICAgICAgbWV0YWRhdGFUYWJsZUJvZHkgPSAkKCAnPHRib2R5IC8+JyApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBkYXRhLmZvckVhY2goIGZ1bmN0aW9uKCBtZXRhZGF0YSApIHtcbiAgICAgICAgICAgICAgICAvLyBsZWZ0IGNlbGxcbiAgICAgICAgICAgICAgICBtZXRhZGF0YVRhYmxlQ2VsbExlZnQgPSAkKCAnPHRkIC8+JyApO1xuICAgICAgICAgICAgICAgIG1ldGFkYXRhS2V5SWNvbiA9ICQoICc8aSAvPicgKS5hdHRyKCAnYXJpYS1oaWRkZW4nLCAndHJ1ZScgKS5hZGRDbGFzcyggJ2ZhIGZhLWJvb2ttYXJrLW8nICk7XG4gICAgICAgICAgICAgICAgbWV0YWRhdGFLZXlMaW5rID0gJCggJzxhIC8+JyApLmF0dHIoICdocmVmJywgX2RlZmF1bHRzLmNvbnRleHRQYXRoICsgJy8nICsgdXJsICkuaHRtbCggbWV0YWRhdGEub25lICsgJzonICk7XG4gICAgICAgICAgICAgICAgbWV0YWRhdGFUYWJsZUNlbGxMZWZ0LmFwcGVuZCggbWV0YWRhdGFLZXlJY29uICkuYXBwZW5kKCBtZXRhZGF0YUtleUxpbmsgKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAvLyByaWdodCBjZWxsXG4gICAgICAgICAgICAgICAgbWV0YWRhdGFUYWJsZUNlbGxSaWdodCA9ICQoICc8dGQgLz4nICk7XG4gICAgICAgICAgICAgICAgbWV0YWRhdGFWYWx1ZUxpbmsgPSAkKCAnPGEgLz4nICkuYXR0ciggJ2hyZWYnLCBfZGVmYXVsdHMuY29udGV4dFBhdGggKyAnLycgKyB1cmwgKS5odG1sKCBtZXRhZGF0YS50d28gKTtcbiAgICAgICAgICAgICAgICBtZXRhZGF0YVRhYmxlQ2VsbFJpZ2h0LmFwcGVuZCggbWV0YWRhdGFWYWx1ZUxpbmsgKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAvLyByb3dcbiAgICAgICAgICAgICAgICBtZXRhZGF0YVRhYmxlUm93ID0gJCggJzx0ciAvPicgKTtcbiAgICAgICAgICAgICAgICBtZXRhZGF0YVRhYmxlUm93LmFwcGVuZCggbWV0YWRhdGFUYWJsZUNlbGxMZWZ0ICkuYXBwZW5kKCBtZXRhZGF0YVRhYmxlQ2VsbFJpZ2h0ICk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLy8gYm9keVxuICAgICAgICAgICAgICAgIG1ldGFkYXRhVGFibGVCb2R5LmFwcGVuZCggbWV0YWRhdGFUYWJsZVJvdyApO1xuICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBtZXRhZGF0YVRhYmxlLmFwcGVuZCggbWV0YWRhdGFUYWJsZUJvZHkgKTtcbiAgICAgICAgICAgIG1ldGFkYXRhV3JhcHBlci5hcHBlbmQoIG1ldGFkYXRhVGFibGUgKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgcmV0dXJuIG1ldGFkYXRhV3JhcHBlcjtcbiAgICAgICAgfVxuICAgIH1cbiAgICBcbiAgICAvKipcbiAgICAgKiBNZXRob2Qgd2hpY2ggcmVuZGVycyBzdWIgY2hpbGQgaGl0cy5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9yZW5kZXJTdWJDaGlsZEhpdHNcbiAgICAgKiBAcGFyYW0ge09iamVjdH0gZGF0YSBUaGUgZGF0YSBvYmplY3Qgd2hpY2ggY29udGFpbnMgdGhlIHN1YiBoaXQgdmFsdWVzLlxuICAgICAqIEBwYXJhbSB7U3RyaW5nfSB0eXBlIFRoZSB0eXBlIG9mIGhpdCB0byByZW5kZXIuXG4gICAgICogQHJldHVybnMge09iamVjdH0gQSBqcXVlcnkgb2JqZWN0IHdoaWNoIGNvbnRhaW5zIHRoZSBzdWIgY2hpbGQgaGl0cy5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfcmVuZGVyU3ViQ2hpbGRIaXRzKCBkYXRhLCB0eXBlICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfcmVuZGVyU3ViQ2hpbGRIaXRzKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX3JlbmRlclN1YkNoaWxkSGl0czogZGF0YSA9ICcsIGRhdGEgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX3JlbmRlclN1YkNoaWxkSGl0czogdHlwZSA9ICcsIHR5cGUgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgdmFyIGhpdFNldENoaWxkcmVuID0gbnVsbDtcbiAgICAgICAgdmFyIGhpdFNldENoaWxkcmVuRGwgPSBudWxsO1xuICAgICAgICB2YXIgaGl0U2V0Q2hpbGRyZW5EdCA9IG51bGw7XG4gICAgICAgIHZhciBoaXRTZXRDaGlsZHJlbkRkID0gbnVsbDtcbiAgICAgICAgdmFyIGhpdFNldENoaWxkcmVuTGluayA9IG51bGw7XG4gICAgICAgIFxuICAgICAgICBoaXRTZXRDaGlsZHJlbiA9ICQoICc8ZGl2IGNsYXNzPVwic2VhcmNoLWxpc3RfX3N0cnVjdC1jaGlsZC1oaXRzXCIgLz4nICk7XG4gICAgICAgIGhpdFNldENoaWxkcmVuRGwgPSAkKCAnPGRsIGNsYXNzPVwiZGwtaG9yaXpvbnRhbFwiIC8+JyApO1xuICAgICAgICBoaXRTZXRDaGlsZHJlbkR0ID0gJCggJzxkdCAvPicgKTtcbiAgICAgICAgLy8gY2hlY2sgaGl0IHR5cGVcbiAgICAgICAgc3dpdGNoICggdHlwZSApIHtcbiAgICAgICAgICAgIGNhc2UgJ1BBR0UnOlxuICAgICAgICAgICAgICAgIGhpdFNldENoaWxkcmVuRHQuYXBwZW5kKCAnPGkgY2xhc3M9XCJmYSBmYS1maWxlLXRleHRcIiBhcmlhLWhpZGRlbj1cInRydWVcIj48L2k+JyApO1xuICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgY2FzZSAnUEVSU09OJzpcbiAgICAgICAgICAgICAgICBoaXRTZXRDaGlsZHJlbkR0LmFwcGVuZCggJzxpIGNsYXNzPVwiZmEgZmEtdXNlclwiIGFyaWEtaGlkZGVuPVwidHJ1ZVwiPjwvaT4nICk7XG4gICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICBjYXNlICdDT1JQT1JBVElPTic6XG4gICAgICAgICAgICAgICAgaGl0U2V0Q2hpbGRyZW5EdC5hcHBlbmQoICc8aSBjbGFzcz1cImZhIGZhLXVuaXZlcnNpdHlcIiBhcmlhLWhpZGRlbj1cInRydWVcIj48L2k+JyApO1xuICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgY2FzZSAnTE9DQVRJT04nOlxuICAgICAgICAgICAgICAgIGhpdFNldENoaWxkcmVuRHQuYXBwZW5kKCAnPGkgY2xhc3M9XCJmYSBmYS1sb2NhdGlvbi1hcnJvd1wiIGFyaWEtaGlkZGVuPVwidHJ1ZVwiPjwvaT4nICk7XG4gICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICBjYXNlICdTVUJKRUNUJzpcbiAgICAgICAgICAgICAgICBoaXRTZXRDaGlsZHJlbkR0LmFwcGVuZCggJzxpIGNsYXNzPVwiZmEgZmEtcXVlc3Rpb24tY2lyY2xlLW9cIiBhcmlhLWhpZGRlbj1cInRydWVcIj48L2k+JyApO1xuICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgY2FzZSAnUFVCTElTSEVSJzpcbiAgICAgICAgICAgICAgICBoaXRTZXRDaGlsZHJlbkR0LmFwcGVuZCggJzxpIGNsYXNzPVwiZmEgZmEtY29weXJpZ2h0XCIgYXJpYS1oaWRkZW49XCJ0cnVlXCI+PC9pPicgKTtcbiAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgIGNhc2UgJ0VWRU5UJzpcbiAgICAgICAgICAgICAgICBoaXRTZXRDaGlsZHJlbkR0LmFwcGVuZCggJzxpIGNsYXNzPVwiZmEgZmEtY2FsZW5kYXJcIiBhcmlhLWhpZGRlbj1cInRydWVcIj48L2k+JyApO1xuICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgY2FzZSAnQUNDRVNTREVOSUVEJzpcbiAgICAgICAgICAgICAgICBoaXRTZXRDaGlsZHJlbkR0LmFwcGVuZCggJzxpIGNsYXNzPVwiZmEgZmEtbG9ja1wiIGFyaWEtaGlkZGVuPVwidHJ1ZVwiPjwvaT4nICk7XG4gICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgIH1cbiAgICAgICAgaGl0U2V0Q2hpbGRyZW5EZCA9ICQoICc8ZGQgLz4nICk7XG4gICAgICAgIGhpdFNldENoaWxkcmVuTGluayA9ICQoICc8YSAvPicgKS5hdHRyKCAnaHJlZicsIF9kZWZhdWx0cy5jb250ZXh0UGF0aCArICcvJyArIGRhdGEudXJsICk7XG4gICAgICAgIHN3aXRjaCAoIHR5cGUgKSB7XG4gICAgICAgICAgICBjYXNlICdQQUdFJzpcbiAgICAgICAgICAgIGNhc2UgJ0FDQ0VTU0RFTklFRCc6XG4gICAgICAgICAgICAgICAgaGl0U2V0Q2hpbGRyZW5MaW5rLmFwcGVuZCggZGF0YS5mdWxsdGV4dEZvckh0bWwgKTtcbiAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgIGRlZmF1bHQ6XG4gICAgICAgICAgICAgICAgaGl0U2V0Q2hpbGRyZW5MaW5rLmFwcGVuZCggZGF0YS5sYWJlbFNob3J0ICk7XG4gICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgIH1cbiAgICAgICAgaGl0U2V0Q2hpbGRyZW5EZC5hcHBlbmQoIGhpdFNldENoaWxkcmVuTGluayApO1xuICAgICAgICBoaXRTZXRDaGlsZHJlbkRsLmFwcGVuZCggaGl0U2V0Q2hpbGRyZW5EdCApLmFwcGVuZCggaGl0U2V0Q2hpbGRyZW5EZCApO1xuICAgICAgICBoaXRTZXRDaGlsZHJlbi5hcHBlbmQoIGhpdFNldENoaWxkcmVuRGwgKTtcbiAgICAgICAgXG4gICAgICAgIHJldHVybiBoaXRTZXRDaGlsZHJlbjtcbiAgICB9XG4gICAgXG4gICAgLyoqXG4gICAgICogTWV0aG9kIHRvIHJlbmRlciBhIGdldCBtb3JlIGNoaWxkcmVuIGxpbmsuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfcmVuZGVyR2V0TW9yZUNoaWxkcmVuXG4gICAgICovXG4gICAgZnVuY3Rpb24gX3JlbmRlckdldE1vcmVDaGlsZHJlbiggZGF0YSwgaWRkb2MsICR0aGlzICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfcmVuZGVyR2V0TW9yZUNoaWxkcmVuKCkgLS0tLS0tLS0tLScgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX3JlbmRlckdldE1vcmVDaGlsZHJlbjogZGF0YSA9ICcsIGRhdGEgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnX3JlbmRlckdldE1vcmVDaGlsZHJlbjogaWRkb2MgPSAnLCBpZGRvYyApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICdfcmVuZGVyR2V0TW9yZUNoaWxkcmVuOiAkdGhpcyA9ICcsICR0aGlzICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIHZhciBhcGlVcmwgPSBfZ2V0QXBpVXJsKCBpZGRvYywgX2RlZmF1bHRzLmhpdHNQZXJDYWxsICsgZGF0YS5oaXRzRGlzcGxheWVkICk7XG4gICAgICAgIHZhciBoaXRDb250ZW50TW9yZSA9ICQoICc8ZGl2IC8+JyApO1xuICAgICAgICB2YXIgZ2V0TW9yZUNoaWxkcmVuTGluayA9ICQoICc8YnV0dG9uIHR5cGU9XCJidXR0b25cIiAvPicgKTtcbiAgICAgICAgXG4gICAgICAgIGlmICggZGF0YS5oYXNNb3JlQ2hpbGRyZW4gKSB7XG4gICAgICAgICAgICAvLyBidWlsZCBnZXQgbW9yZSBsaW5rXG4gICAgICAgICAgICBoaXRDb250ZW50TW9yZS5hZGRDbGFzcyggJ3NlYXJjaC1saXN0X19oaXQtY29udGVudC1tb3JlJyApO1xuICAgICAgICAgICAgZ2V0TW9yZUNoaWxkcmVuTGluay5hZGRDbGFzcyggJ2J0bi1jbGVhbicgKTtcbiAgICAgICAgICAgIGdldE1vcmVDaGlsZHJlbkxpbmsuYXR0ciggJ2RhdGEtYXBpJywgYXBpVXJsICk7XG4gICAgICAgICAgICBnZXRNb3JlQ2hpbGRyZW5MaW5rLmF0dHIoICdkYXRhLWlkZG9jJywgaWRkb2MgKTtcbiAgICAgICAgICAgIGdldE1vcmVDaGlsZHJlbkxpbmsuYXBwZW5kKCBfZGVmYXVsdHMubXNnLmdldE1vcmVDaGlsZHJlbiApO1xuICAgICAgICAgICAgaGl0Q29udGVudE1vcmUuYXBwZW5kKCBnZXRNb3JlQ2hpbGRyZW5MaW5rICk7XG4gICAgICAgICAgICAvLyBhcHBlbmQgbGlua3NcbiAgICAgICAgICAgICR0aGlzLm5leHQoKS5hcHBlbmQoIGhpdENvbnRlbnRNb3JlICk7XG4gICAgICAgICAgICAvLyByZW5kZXIgbmV3IGhpdCBzZXRcbiAgICAgICAgICAgIGdldE1vcmVDaGlsZHJlbkxpbmsub2ZmKCkub24oICdjbGljaycsIGZ1bmN0aW9uKCBldmVudCApIHtcbiAgICAgICAgICAgICAgICB2YXIgY3VyckFwaVVybCA9ICQoIHRoaXMgKS5hdHRyKCAnZGF0YS1hcGknICk7XG4gICAgICAgICAgICAgICAgdmFyIHBhcmVudE9mZnNldCA9ICR0aGlzLnBhcmVudCgpLm9mZnNldCgpLnRvcDtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAvLyBnZXQgZGF0YSBhbmQgcmVuZGVyIGhpdHMgaWYgZGF0YSBpcyB2YWxpZFxuICAgICAgICAgICAgICAgIF9wcm9taXNlID0gdmlld2VyLmhlbHBlci5nZXRSZW1vdGVEYXRhKCBjdXJyQXBpVXJsICk7XG4gICAgICAgICAgICAgICAgX3Byb21pc2UudGhlbiggZnVuY3Rpb24oIGRhdGEgKSB7XG4gICAgICAgICAgICAgICAgICAgIC8vIHJlbmRlciBjaGlsZCBoaXRzIGludG8gdGhlIERPTVxuICAgICAgICAgICAgICAgICAgICBfcmVuZGVyQ2hpbGRIaXRzKCBkYXRhLCAkdGhpcyApO1xuICAgICAgICAgICAgICAgICAgICAvLyBjaGVjayBpZiBtb3JlIGNoaWxkcmVuIGV4aXN0IGFuZCByZW5kZXIgbGlua1xuICAgICAgICAgICAgICAgICAgICBfcmVuZGVyR2V0TW9yZUNoaWxkcmVuKCBkYXRhLCBpZGRvYywgJHRoaXMgKTtcbiAgICAgICAgICAgICAgICB9ICkudGhlbiggbnVsbCwgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICR0aGlzLm5leHQoKS5hcHBlbmQoIHZpZXdlci5oZWxwZXIucmVuZGVyQWxlcnQoICdhbGVydC1kYW5nZXInLCAnPHN0cm9uZz5TdGF0dXM6IDwvc3Ryb25nPicgKyBlcnJvci5zdGF0dXMgKyAnICcgKyBlcnJvci5zdGF0dXNUZXh0LCBmYWxzZSApICk7XG4gICAgICAgICAgICAgICAgICAgIGNvbnNvbGUuZXJyb3IoICdFUlJPUjogX3JlbmRlckdldE1vcmVDaGlsZHJlbiAtICcsIGVycm9yICk7XG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgfSApO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgLy8gY2xlYXIgYW5kIGhpZGUgY3VycmVudCBnZXQgbW9yZSBsaW5rXG4gICAgICAgICAgICAkdGhpcy5uZXh0KCkuZmluZCggX2RlZmF1bHRzLmhpdENvbnRlbnRNb3JlU2VsZWN0b3IgKS5lbXB0eSgpLmhpZGUoKTtcbiAgICAgICAgICAgIGNvbnNvbGUuaW5mbyggJ19yZW5kZXJHZXRNb3JlQ2hpbGRyZW46IE5vIG1vcmUgY2hpbGQgaGl0cyBhdmFpbGFibGUnICk7XG4gICAgICAgICAgICByZXR1cm4gZmFsc2U7XG4gICAgICAgIH1cbiAgICB9XG4gICAgXG4gICAgcmV0dXJuIHZpZXdlcjtcbiAgICBcbn0gKSggdmlld2VySlMgfHwge30sIGpRdWVyeSApO1xuIiwiLyoqXG4gKiBUaGlzIGZpbGUgaXMgcGFydCBvZiB0aGUgR29vYmkgdmlld2VyIC0gYSBjb250ZW50IHByZXNlbnRhdGlvbiBhbmQgbWFuYWdlbWVudFxuICogYXBwbGljYXRpb24gZm9yIGRpZ2l0aXplZCBvYmplY3RzLlxuICogXG4gKiBWaXNpdCB0aGVzZSB3ZWJzaXRlcyBmb3IgbW9yZSBpbmZvcm1hdGlvbi4gLSBodHRwOi8vd3d3LmludHJhbmRhLmNvbSAtXG4gKiBodHRwOi8vZGlnaXZlcnNvLmNvbVxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZnJlZSBzb2Z0d2FyZTsgeW91IGNhbiByZWRpc3RyaWJ1dGUgaXQgYW5kL29yIG1vZGlmeSBpdCB1bmRlciB0aGUgdGVybXNcbiAqIG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhcyBwdWJsaXNoZWQgYnkgdGhlIEZyZWUgU29mdHdhcmUgRm91bmRhdGlvbjsgZWl0aGVyXG4gKiB2ZXJzaW9uIDIgb2YgdGhlIExpY2Vuc2UsIG9yIChhdCB5b3VyIG9wdGlvbikgYW55IGxhdGVyIHZlcnNpb24uXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBkaXN0cmlidXRlZCBpbiB0aGUgaG9wZSB0aGF0IGl0IHdpbGwgYmUgdXNlZnVsLCBidXQgV0lUSE9VVCBBTllcbiAqIFdBUlJBTlRZOyB3aXRob3V0IGV2ZW4gdGhlIGltcGxpZWQgd2FycmFudHkgb2YgTUVSQ0hBTlRBQklMSVRZIG9yIEZJVE5FU1MgRk9SIEFcbiAqIFBBUlRJQ1VMQVIgUFVSUE9TRS4gU2VlIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBmb3IgbW9yZSBkZXRhaWxzLlxuICogXG4gKiBZb3Ugc2hvdWxkIGhhdmUgcmVjZWl2ZWQgYSBjb3B5IG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhbG9uZyB3aXRoIHRoaXNcbiAqIHByb2dyYW0uIElmIG5vdCwgc2VlIDxodHRwOi8vd3d3LmdudS5vcmcvbGljZW5zZXMvPi5cbiAqIFxuICogTW9kdWxlIHdoaWNoIHNldHMgdXAgdGhlIHNvcnRpbmcgZnVuY3Rpb25hbGl0eSBmb3Igc2VhcmNoIGxpc3QuXG4gKiBcbiAqIEB2ZXJzaW9uIDMuMi4wXG4gKiBAbW9kdWxlIHZpZXdlckpTLnNlYXJjaFNvcnRpbmdEcm9wZG93blxuICogQHJlcXVpcmVzIGpRdWVyeVxuICovXG52YXIgdmlld2VySlMgPSAoIGZ1bmN0aW9uKCB2aWV3ZXIgKSB7XG4gICAgJ3VzZSBzdHJpY3QnO1xuICAgIFxuICAgIHZhciBfZGVidWcgPSBmYWxzZTtcbiAgICB2YXIgX3NlbGVjdGVkU29ydGluZyA9ICcnO1xuICAgIHZhciBfZGF0YVNvcnRGaWVsZFN0YXRlID0gJyc7XG4gICAgdmFyIF9jdXJyVXJsID0gJyc7XG4gICAgdmFyIF92YWx1ZVVybCA9ICcnO1xuICAgIHZhciBfY2hlY2tWYWxVcmwgPSAnJztcbiAgICB2YXIgX2RhdGFTb3J0RmllbGQgPSAnJztcbiAgICB2YXIgX2RlZmF1bHRzID0ge1xuICAgICAgICBzZWxlY3Q6ICcjc29ydFNlbGVjdCcsXG4gICAgfTtcbiAgICBcbiAgICB2aWV3ZXIuc2VhcmNoU29ydGluZ0Ryb3Bkb3duID0ge1xuICAgICAgICAvKipcbiAgICAgICAgICogTWV0aG9kIHRvIGluaXRpYWxpemUgdGhlIHNlYXJjaCBzb3J0aW5nIHdpZGdldC5cbiAgICAgICAgICogXG4gICAgICAgICAqIEBtZXRob2QgaW5pdFxuICAgICAgICAgKiBAcGFyYW0ge09iamVjdH0gY29uZmlnIEFuIGNvbmZpZyBvYmplY3Qgd2hpY2ggb3ZlcndyaXRlcyB0aGUgZGVmYXVsdHMuXG4gICAgICAgICAqIEBwYXJhbSB7T2JqZWN0fSBjb25maWcuc2VsZWN0IEFuIGpRdWVyeSBvYmplY3Qgd2hpY2ggaG9sZHMgdGhlIHNvcnRpbmcgbWVudS5cbiAgICAgICAgICovXG4gICAgICAgIGluaXQ6IGZ1bmN0aW9uKCBjb25maWcgKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci5zZWFyY2hTb3J0aW5nRHJvcGRvd24uaW5pdCcgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci5zZWFyY2hTb3J0aW5nRHJvcGRvd24uaW5pdDogY29uZmlnID0gJywgY29uZmlnICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgICQuZXh0ZW5kKCB0cnVlLCBfZGVmYXVsdHMsIGNvbmZpZyApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBpZiAoIHZpZXdlci5sb2NhbFN0b3JhZ2VQb3NzaWJsZSApIHtcbiAgICAgICAgICAgICAgICBfc2VsZWN0ZWRTb3J0aW5nID0gbG9jYWxTdG9yYWdlLmRhdGFTb3J0RmllbGQ7XG4gICAgICAgICAgICAgICAgX2N1cnJVcmwgPSBsb2NhdGlvbi5ocmVmO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIC8vIGdldCBzZWxlY3RlZCBzb3J0aW5nIHR5cGUgZnJvbSBsb2NhbCBzdG9yYWdlIGFuIHNldCB0aGUgbWVudSBvcHRpb24gdG9cbiAgICAgICAgICAgICAgICAvLyBzZWxlY3RlZFxuICAgICAgICAgICAgICAgIGlmICggX3NlbGVjdGVkU29ydGluZyAhPT0gJycgfHwgX3NlbGVjdGVkU29ydGluZyAhPT0gJ3VuZGVmaW5kZWQnICkge1xuICAgICAgICAgICAgICAgICAgICAkKCBfZGVmYXVsdHMuc2VsZWN0ICkuY2hpbGRyZW4oICdvcHRpb24nICkuZWFjaCggZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBfZGF0YVNvcnRGaWVsZFN0YXRlID0gJCggdGhpcyApLmF0dHIoICdkYXRhLXNvcnRGaWVsZCcgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIF9jaGVja1ZhbFVybCA9ICQoIHRoaXMgKS52YWwoKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKCBfZGF0YVNvcnRGaWVsZFN0YXRlID09PSBfc2VsZWN0ZWRTb3J0aW5nICYmIF9jaGVja1ZhbFVybCA9PT0gX2N1cnJVcmwgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgJCggdGhpcyApLmF0dHIoICdzZWxlY3RlZCcsICdzZWxlY3RlZCcgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAvLyBnZXQgdGhlIHNvcnRpbmcgVVJMIGZyb20gdGhlIG9wdGlvbiB2YWx1ZSBhbmQgcmVsb2FkIHRoZSBwYWdlIG9uIGNoYW5nZVxuICAgICAgICAgICAgICAgICQoIF9kZWZhdWx0cy5zZWxlY3QgKS5vbiggJ2NoYW5nZScsIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICBfdmFsdWVVcmwgPSAkKCB0aGlzICkudmFsKCk7XG4gICAgICAgICAgICAgICAgICAgIF9kYXRhU29ydEZpZWxkID0gJCggdGhpcyApLmNoaWxkcmVuKCAnb3B0aW9uOnNlbGVjdGVkJyApLmF0dHIoICdkYXRhLXNvcnRGaWVsZCcgKTtcbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgIGlmICggX3ZhbHVlVXJsICE9PSAnJyApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIC8vIHNhdmUgY3VycmVudCBzb3J0aW5nIHN0YXRlIHRvIGxvY2FsIHN0b3JhZ2VcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICggdHlwZW9mICggU3RvcmFnZSApICE9PSBcInVuZGVmaW5lZFwiICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGxvY2FsU3RvcmFnZS5kYXRhU29ydEZpZWxkID0gX2RhdGFTb3J0RmllbGQ7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBjb25zb2xlLmluZm8oICdMb2NhbCBTdG9yYWdlIGlzIG5vdCBkZWZpbmVkLiBDdXJyZW50IHNvcnRpbmcgc3RhdGUgY291bGQgbm90IGJlIHNhdmVkLicgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICByZXR1cm4gZmFsc2U7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgIHdpbmRvdy5sb2NhdGlvbi5ocmVmID0gX3ZhbHVlVXJsO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIGZhbHNlO1xuICAgICAgICAgICAgfVxuICAgICAgICB9LFxuICAgIH07XG4gICAgXG4gICAgcmV0dXJuIHZpZXdlcjtcbiAgICBcbn0gKSggdmlld2VySlMgfHwge30sIGpRdWVyeSApO1xuIiwiLyoqXG4gKiBUaGlzIGZpbGUgaXMgcGFydCBvZiB0aGUgR29vYmkgdmlld2VyIC0gYSBjb250ZW50IHByZXNlbnRhdGlvbiBhbmQgbWFuYWdlbWVudFxuICogYXBwbGljYXRpb24gZm9yIGRpZ2l0aXplZCBvYmplY3RzLlxuICogXG4gKiBWaXNpdCB0aGVzZSB3ZWJzaXRlcyBmb3IgbW9yZSBpbmZvcm1hdGlvbi4gLSBodHRwOi8vd3d3LmludHJhbmRhLmNvbSAtXG4gKiBodHRwOi8vZGlnaXZlcnNvLmNvbVxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZnJlZSBzb2Z0d2FyZTsgeW91IGNhbiByZWRpc3RyaWJ1dGUgaXQgYW5kL29yIG1vZGlmeSBpdCB1bmRlciB0aGUgdGVybXNcbiAqIG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhcyBwdWJsaXNoZWQgYnkgdGhlIEZyZWUgU29mdHdhcmUgRm91bmRhdGlvbjsgZWl0aGVyXG4gKiB2ZXJzaW9uIDIgb2YgdGhlIExpY2Vuc2UsIG9yIChhdCB5b3VyIG9wdGlvbikgYW55IGxhdGVyIHZlcnNpb24uXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBkaXN0cmlidXRlZCBpbiB0aGUgaG9wZSB0aGF0IGl0IHdpbGwgYmUgdXNlZnVsLCBidXQgV0lUSE9VVCBBTllcbiAqIFdBUlJBTlRZOyB3aXRob3V0IGV2ZW4gdGhlIGltcGxpZWQgd2FycmFudHkgb2YgTUVSQ0hBTlRBQklMSVRZIG9yIEZJVE5FU1MgRk9SIEFcbiAqIFBBUlRJQ1VMQVIgUFVSUE9TRS4gU2VlIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBmb3IgbW9yZSBkZXRhaWxzLlxuICogXG4gKiBZb3Ugc2hvdWxkIGhhdmUgcmVjZWl2ZWQgYSBjb3B5IG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhbG9uZyB3aXRoIHRoaXNcbiAqIHByb2dyYW0uIElmIG5vdCwgc2VlIDxodHRwOi8vd3d3LmdudS5vcmcvbGljZW5zZXMvPi5cbiAqIFxuICogTW9kdWxlIHRvIHJlbmRlciBhIHNpbXBsZSBsaWdodGJveCBmb3IgaW1hZ2VzLlxuICogXG4gKiBAdmVyc2lvbiAzLjIuMFxuICogQG1vZHVsZSB2aWV3ZXJKUy5zaW1wbGVMaWdodGJveFxuICogQHJlcXVpcmVzIGpRdWVyeVxuICogQGV4YW1wbGUgPGltZyBzcmM9XCIveW91ci9wYXRoL3RvL3RoZS9pbWFnZS5qcGdcIiBjbGFzcz1cImxpZ2h0Ym94LWltYWdlXCJcbiAqIGRhdGEtaW1ncGF0aD1cIi95b3VyL3BhdGgvdG8vdGhlL1wiIGRhdGEtaW1nbmFtZT1cImltYWdlLmpwZ1wiIGFsdD1cIlwiIC8+ICpcbiAqL1xudmFyIHZpZXdlckpTID0gKCBmdW5jdGlvbiggdmlld2VyICkge1xuICAgICd1c2Ugc3RyaWN0JztcbiAgICBcbiAgICB2YXIgX2JveCA9IG51bGw7XG4gICAgdmFyIF9pbWdwYXRoID0gbnVsbDtcbiAgICB2YXIgX2ltZ25hbWUgPSBudWxsO1xuICAgIFxuICAgIHZpZXdlci5zaW1wbGVMaWdodEJveCA9IHtcbiAgICAgICAgLyoqXG4gICAgICAgICAqIEluaXRpYWxpemVzIGFuIGV2ZW50IChjbGljaykgd2hpY2ggcmVuZGVycyBhIGxpZ2h0Ym94IHdpdGggYW4gYmlnZ2VyIGltYWdlLlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCBpbml0XG4gICAgICAgICAqIEBleGFtcGxlXG4gICAgICAgICAqIFxuICAgICAgICAgKiA8cHJlPlxuICAgICAgICAgKiB2aWV3ZXJKUy5zaW1wbGVMaWdodEJveC5pbml0KCk7XG4gICAgICAgICAqIDwvcHJlPlxuICAgICAgICAgKiBcbiAgICAgICAgICovXG4gICAgICAgIGluaXQ6IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgLy8gZXZlbnRsaXN0ZW5lcnNcbiAgICAgICAgICAgICQoICcubGlnaHRib3gtaW1hZ2UnICkub24oICdjbGljaycsIGZ1bmN0aW9uKCBldmVudCApIHtcbiAgICAgICAgICAgICAgICBldmVudC5wcmV2ZW50RGVmYXVsdCgpO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIHZhciAkdGhpcyA9ICQoIHRoaXMgKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICBfaW1ncGF0aCA9IF9nZXRJbWFnZVBhdGgoICR0aGlzICk7XG4gICAgICAgICAgICAgICAgX2ltZ25hbWUgPSBfZ2V0SW1hZ2VOYW1lKCAkdGhpcyApO1xuICAgICAgICAgICAgICAgIF9ib3ggPSBfc2V0dXBMaWdodEJveCggX2ltZ3BhdGgsIF9pbWduYW1lICk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgJCggJ2JvZHknICkuYXBwZW5kKCBfYm94ICk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgX2NlbnRlck1vZGFsQm94KCAkKCAnLmxpZ2h0Ym94LW1vZGFsLWJveCcgKSApO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICQoICcubGlnaHRib3gtb3ZlcmxheScgKS5mYWRlSW4oKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAkKCAnLmxpZ2h0Ym94LWNsb3NlLWJ0bicgKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICQoICcubGlnaHRib3gtb3ZlcmxheScgKS5yZW1vdmUoKTtcbiAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICB9ICk7XG4gICAgICAgIH1cbiAgICB9O1xuICAgIFxuICAgIC8qKlxuICAgICAqIFJldHVybnMgdGhlIGltYWdlIHBhdGggZnJvbSB0aGUgJ2RhdGEtaW1ncGF0aCcgYXR0cmlidXRlLlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX2dldEltYWdlUGF0aFxuICAgICAqIEBwYXJhbSB7T2JqZWN0fSAkT2JqIE11c3QgYmUgYSBqUXVlcnktT2JqZWN0IGxpa2UgJCgnLnNvbWV0aGluZycpXG4gICAgICogQHJldHVybnMge1N0cmluZ30gVGhlIGltYWdlIHBhdGggZnJvbSB0aGUgJ2RhdGEtaW1ncGF0aCcgYXR0cmlidXRlLlxuICAgICAqIFxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9nZXRJbWFnZVBhdGgoICRPYmogKSB7XG4gICAgICAgIF9pbWdwYXRoID0gJE9iai5hdHRyKCAnZGF0YS1pbWdwYXRoJyApO1xuICAgICAgICBcbiAgICAgICAgcmV0dXJuIF9pbWdwYXRoO1xuICAgIH1cbiAgICBcbiAgICAvKipcbiAgICAgKiBSZXR1cm5zIHRoZSBpbWFnZSBuYW1lIGZyb20gdGhlICdkYXRhLWltZ25hbWUnIGF0dHJpYnV0ZS5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9nZXRJbWFnZU5hbWVcbiAgICAgKiBAcGFyYW0ge09iamVjdH0gJE9iaiBNdXN0IGJlIGEgalF1ZXJ5LU9iamVjdCBsaWtlICQoJy5zb21ldGhpbmcnKVxuICAgICAqIEByZXR1cm5zIHtTdHJpbmd9IFRoZSBpbWFnZSBuYW1lIGZyb20gdGhlICdkYXRhLWltZ25hbWUnIGF0dHJpYnV0ZS5cbiAgICAgKiBcbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfZ2V0SW1hZ2VOYW1lKCAkT2JqICkge1xuICAgICAgICBfaW1nbmFtZSA9ICRPYmouYXR0ciggJ2RhdGEtaW1nbmFtZScgKTtcbiAgICAgICAgXG4gICAgICAgIHJldHVybiBfaW1nbmFtZTtcbiAgICB9XG4gICAgXG4gICAgLyoqXG4gICAgICogUmV0dXJucyBhIEhUTUwtU3RyaW5nIHdoaWNoIHJlbmRlcnMgdGhlIGxpZ2h0Ym94LlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX3NldHVwTGlnaHRCb3hcbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gcGF0aCBUaGUgcGF0aCB0byB0aGUgYmlnIGltYWdlLlxuICAgICAqIEBwYXJhbSB7U3RyaW5nfSBuYW1lIFRoZSBuYW1lIG9mIHRoZSBiaWcgaW1hZ2UuXG4gICAgICogQHJldHVybnMge1N0cmluZ30gVGhlIEhUTUwtQ29kZSB0byByZW5kZXIgdGhlIGxpZ2h0Ym94LlxuICAgICAqIFxuICAgICAqL1xuICAgIGZ1bmN0aW9uIF9zZXR1cExpZ2h0Qm94KCBwYXRoLCBuYW1lICkge1xuICAgICAgICB2YXIgbGlnaHRib3ggPSAnJztcbiAgICAgICAgXG4gICAgICAgIGxpZ2h0Ym94ID0gJzxkaXYgY2xhc3M9XCJsaWdodGJveC1vdmVybGF5XCI+JztcbiAgICAgICAgbGlnaHRib3ggKz0gJzxkaXYgY2xhc3M9XCJsaWdodGJveC1tb2RhbC1ib3hcIj4nO1xuICAgICAgICBsaWdodGJveCArPSAnPGRpdiBjbGFzcz1cImxpZ2h0Ym94LWNsb3NlXCI+JztcbiAgICAgICAgbGlnaHRib3ggKz0gJzxzcGFuIGNsYXNzPVwibGlnaHRib3gtY2xvc2UtYnRuXCIgdGl0bGU9XCJGZW5zdGVyIHNjaGxpZSZzemxpZztlblwiPiZ0aW1lczs8L3NwYW4+JztcbiAgICAgICAgbGlnaHRib3ggKz0gJzwvZGl2Pic7XG4gICAgICAgIGxpZ2h0Ym94ICs9ICc8aW1nIHNyYz1cIicgKyBwYXRoICsgbmFtZSArICdcIiBhbHQ9XCInICsgbmFtZSArICdcIiAvPjwvZGl2PjwvZGl2Pic7XG4gICAgICAgIFxuICAgICAgICByZXR1cm4gbGlnaHRib3g7XG4gICAgfVxuICAgIFxuICAgIC8qKlxuICAgICAqIFB1dHMgdGhlIGxpZ2h0Ym94IHRvIHRoZSBjZW50ZXIgb2YgdGhlIHNjcmVlbi5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9jZW50ZXJNb2RhbEJveFxuICAgICAqIEBwYXJhbSB7T2JqZWN0fSAkT2JqIE11c3QgYmUgYSBqUXVlcnktT2JqZWN0IGxpa2UgJCgnLnNvbWV0aGluZycpXG4gICAgICovXG4gICAgZnVuY3Rpb24gX2NlbnRlck1vZGFsQm94KCAkT2JqICkge1xuICAgICAgICB2YXIgYm94V2lkdGggPSAkT2JqLm91dGVyV2lkdGgoKTtcbiAgICAgICAgdmFyIGJveEhlaWdodCA9ICRPYmoub3V0ZXJIZWlnaHQoKTtcbiAgICAgICAgXG4gICAgICAgICRPYmouY3NzKCB7XG4gICAgICAgICAgICAnbWFyZ2luLXRvcCc6ICctJyArIGJveEhlaWdodCAvIDIgKyAncHgnLFxuICAgICAgICAgICAgJ21hcmdpbi1sZWZ0JzogJy0nICsgYm94V2lkdGggLyAyICsgJ3B4J1xuICAgICAgICB9ICk7XG4gICAgfVxuICAgIFxuICAgIHJldHVybiB2aWV3ZXI7XG4gICAgXG59ICkoIHZpZXdlckpTIHx8IHt9LCBqUXVlcnkgKTtcbiIsIi8qKlxuICogVGhpcyBmaWxlIGlzIHBhcnQgb2YgdGhlIEdvb2JpIHZpZXdlciAtIGEgY29udGVudCBwcmVzZW50YXRpb24gYW5kIG1hbmFnZW1lbnRcbiAqIGFwcGxpY2F0aW9uIGZvciBkaWdpdGl6ZWQgb2JqZWN0cy5cbiAqIFxuICogVmlzaXQgdGhlc2Ugd2Vic2l0ZXMgZm9yIG1vcmUgaW5mb3JtYXRpb24uIC0gaHR0cDovL3d3dy5pbnRyYW5kYS5jb20gLVxuICogaHR0cDovL2RpZ2l2ZXJzby5jb21cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGZyZWUgc29mdHdhcmU7IHlvdSBjYW4gcmVkaXN0cmlidXRlIGl0IGFuZC9vciBtb2RpZnkgaXQgdW5kZXIgdGhlIHRlcm1zXG4gKiBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYXMgcHVibGlzaGVkIGJ5IHRoZSBGcmVlIFNvZnR3YXJlIEZvdW5kYXRpb247IGVpdGhlclxuICogdmVyc2lvbiAyIG9mIHRoZSBMaWNlbnNlLCBvciAoYXQgeW91ciBvcHRpb24pIGFueSBsYXRlciB2ZXJzaW9uLlxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZGlzdHJpYnV0ZWQgaW4gdGhlIGhvcGUgdGhhdCBpdCB3aWxsIGJlIHVzZWZ1bCwgYnV0IFdJVEhPVVQgQU5ZXG4gKiBXQVJSQU5UWTsgd2l0aG91dCBldmVuIHRoZSBpbXBsaWVkIHdhcnJhbnR5IG9mIE1FUkNIQU5UQUJJTElUWSBvciBGSVRORVNTIEZPUiBBXG4gKiBQQVJUSUNVTEFSIFBVUlBPU0UuIFNlZSB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgZm9yIG1vcmUgZGV0YWlscy5cbiAqIFxuICogWW91IHNob3VsZCBoYXZlIHJlY2VpdmVkIGEgY29weSBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYWxvbmcgd2l0aCB0aGlzXG4gKiBwcm9ncmFtLiBJZiBub3QsIHNlZSA8aHR0cDovL3d3dy5nbnUub3JnL2xpY2Vuc2VzLz4uXG4gKiBcbiAqIE1vZHVsZSB3aGljaCByZW5kZXJzIENTUyBmZWF0dXJlZCBzdGFja2VkIHRodW1ibmFpbHMgaW4gc2VhcmNoIGxpc3QgZm9yIG11bHRpdm9sdW1lXG4gKiB3b3Jrcy5cbiAqIFxuICogQHZlcnNpb24gMy4yLjBcbiAqIEBtb2R1bGUgdmlld2VySlMuc3RhY2tlZFRodW1ibmFpbHNcbiAqIEByZXF1aXJlcyBqUXVlcnlcbiAqL1xudmFyIHZpZXdlckpTID0gKCBmdW5jdGlvbiggdmlld2VyICkge1xuICAgICd1c2Ugc3RyaWN0JztcbiAgICBcbiAgICB2YXIgX2RlYnVnID0gZmFsc2U7XG4gICAgdmFyIF9pbWdXaWR0aCA9IG51bGw7XG4gICAgdmFyIF9pbWdIZWlnaHQgPSBudWxsO1xuICAgIHZhciBfZGVmYXVsdHMgPSB7XG4gICAgICAgIHRodW1iczogJy5zdGFja2VkLXRodW1ibmFpbCcsXG4gICAgICAgIHRodW1ic0JlZm9yZTogJy5zdGFja2VkLXRodW1ibmFpbC1iZWZvcmUnLFxuICAgICAgICB0aHVtYnNBZnRlcjogJy5zdGFja2VkLXRodW1ibmFpbC1hZnRlcicsXG4gICAgfTtcbiAgICBcbiAgICB2aWV3ZXIuc3RhY2tlZFRodW1ibmFpbHMgPSB7XG4gICAgICAgIC8qKlxuICAgICAgICAgKiBNZXRob2QgdG8gaW5pdGlhbGl6ZSB0aGUgdGltZW1hdHJpeCBzbGlkZXIgYW5kIHRoZSBldmVudHMgd2hpY2ggYnVpbGRzIHRoZVxuICAgICAgICAgKiBtYXRyaXggYW5kIHBvcG92ZXJzLlxuICAgICAgICAgKiBcbiAgICAgICAgICogQG1ldGhvZCBpbml0XG4gICAgICAgICAqIEBwYXJhbSB7T2JqZWN0fSBjb25maWcgQW4gY29uZmlnIG9iamVjdCB3aGljaCBvdmVyd3JpdGVzIHRoZSBkZWZhdWx0cy5cbiAgICAgICAgICogQHBhcmFtIHtPYmplY3R9IGNvbmZpZy50aHVtYnMgQWxsIGpRdWVyeSBvYmplY3RzIG9mIHRoZSBzdGFja2VkIHRodW1ibmFpbHMuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcudGh1bWJzQmVmb3JlIFRoZSBjbGFzc25hbWUgb2YgdGhlIHN0YWNrZWQgdGh1bWJuYWlsXG4gICAgICAgICAqIGJlZm9yZSBlbGVtZW50LlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLnRodW1ic0FmdGVyIFRoZSBjbGFzc25hbWUgb2YgdGhlIHN0YWNrZWQgdGh1bWJuYWlsIGFmdGVyXG4gICAgICAgICAqIGVsZW1lbnQuXG4gICAgICAgICAqL1xuICAgICAgICBpbml0OiBmdW5jdGlvbiggY29uZmlnICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICcjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIuc3RhY2tlZFRodW1ibmFpbHMuaW5pdCcgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci5zdGFja2VkVGh1bWJuYWlscy5pbml0OiBjb25maWcgLSAnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coIGNvbmZpZyApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICAkLmV4dGVuZCggdHJ1ZSwgX2RlZmF1bHRzLCBjb25maWcgKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gaGlkZSBzdGFja2VkIHRodW1ic1xuICAgICAgICAgICAgJCggX2RlZmF1bHRzLnRodW1icyApLmhpZGUoKTtcbiAgICAgICAgICAgICQoIF9kZWZhdWx0cy50aHVtYnMgKS5zaWJsaW5ncygpLmhpZGUoKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gaXRlcmF0ZSB0aHJvdWdoIHRodW1ibmFpbHMgYW5kIHNldCB3aWR0aCBhbmQgaGVpZ2h0IGZvciBpbWFnZSBzdGFja1xuICAgICAgICAgICAgJCggX2RlZmF1bHRzLnRodW1icyApLmVhY2goIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgIF9pbWdXaWR0aCA9ICQoIHRoaXMgKS5vdXRlcldpZHRoKCk7XG4gICAgICAgICAgICAgICAgX2ltZ0hlaWdodCA9ICQoIHRoaXMgKS5vdXRlckhlaWdodCgpO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICQoIHRoaXMgKS5jc3MoIHtcbiAgICAgICAgICAgICAgICAgICAgJ21hcmdpbi1sZWZ0JzogJy0nICsgKCBfaW1nV2lkdGggLyAyICkgKyAncHgnXG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICQoIHRoaXMgKS5zaWJsaW5ncygpLmNzcygge1xuICAgICAgICAgICAgICAgICAgICAnd2lkdGgnOiBfaW1nV2lkdGgsXG4gICAgICAgICAgICAgICAgICAgICdoZWlnaHQnOiBfaW1nSGVpZ2h0LFxuICAgICAgICAgICAgICAgICAgICAnbWFyZ2luLWxlZnQnOiAnLScgKyAoIF9pbWdXaWR0aCAvIDIgKSArICdweCdcbiAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLy8gc2hvdyBzdGFja2VkIHRodW1icyBhZnRlciBidWlsZGluZyB0aGVtXG4gICAgICAgICAgICAgICAgJCggdGhpcyApLnNob3coKTtcbiAgICAgICAgICAgICAgICAkKCB0aGlzICkuc2libGluZ3MoIF9kZWZhdWx0cy50aHVtYnNCZWZvcmUgKS5mYWRlSW4oICdzbG93JywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICQoIHRoaXMgKS5zaWJsaW5ncyggX2RlZmF1bHRzLnRodW1ic0FmdGVyICkuZmFkZUluKCk7XG4gICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgXG4gICAgICAgIH0sXG4gICAgfTtcbiAgICBcbiAgICByZXR1cm4gdmlld2VyO1xuICAgIFxufSApKCB2aWV3ZXJKUyB8fCB7fSwgalF1ZXJ5ICk7XG4iLCIvKipcbiAqIFRoaXMgZmlsZSBpcyBwYXJ0IG9mIHRoZSBHb29iaSB2aWV3ZXIgLSBhIGNvbnRlbnQgcHJlc2VudGF0aW9uIGFuZCBtYW5hZ2VtZW50XG4gKiBhcHBsaWNhdGlvbiBmb3IgZGlnaXRpemVkIG9iamVjdHMuXG4gKiBcbiAqIFZpc2l0IHRoZXNlIHdlYnNpdGVzIGZvciBtb3JlIGluZm9ybWF0aW9uLiAtIGh0dHA6Ly93d3cuaW50cmFuZGEuY29tIC1cbiAqIGh0dHA6Ly9kaWdpdmVyc28uY29tXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBmcmVlIHNvZnR3YXJlOyB5b3UgY2FuIHJlZGlzdHJpYnV0ZSBpdCBhbmQvb3IgbW9kaWZ5IGl0IHVuZGVyIHRoZSB0ZXJtc1xuICogb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFzIHB1Ymxpc2hlZCBieSB0aGUgRnJlZSBTb2Z0d2FyZSBGb3VuZGF0aW9uOyBlaXRoZXJcbiAqIHZlcnNpb24gMiBvZiB0aGUgTGljZW5zZSwgb3IgKGF0IHlvdXIgb3B0aW9uKSBhbnkgbGF0ZXIgdmVyc2lvbi5cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGRpc3RyaWJ1dGVkIGluIHRoZSBob3BlIHRoYXQgaXQgd2lsbCBiZSB1c2VmdWwsIGJ1dCBXSVRIT1VUIEFOWVxuICogV0FSUkFOVFk7IHdpdGhvdXQgZXZlbiB0aGUgaW1wbGllZCB3YXJyYW50eSBvZiBNRVJDSEFOVEFCSUxJVFkgb3IgRklUTkVTUyBGT1IgQVxuICogUEFSVElDVUxBUiBQVVJQT1NFLiBTZWUgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGZvciBtb3JlIGRldGFpbHMuXG4gKiBcbiAqIFlvdSBzaG91bGQgaGF2ZSByZWNlaXZlZCBhIGNvcHkgb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFsb25nIHdpdGggdGhpc1xuICogcHJvZ3JhbS4gSWYgbm90LCBzZWUgPGh0dHA6Ly93d3cuZ251Lm9yZy9saWNlbnNlcy8+LlxuICogXG4gKiBNb2R1bGUgdG8gY3JlYXRlIGFuIGltYWdlbWFwIHNvcnRlZCBieSB0aGUgeWVhciBvZiBjcmVhdGlvbi5cbiAqIFxuICogQHZlcnNpb24gMy4yLjBcbiAqIEBtb2R1bGUgdmlld2VySlMudGltZW1hdHJpeFxuICogQHJlcXVpcmVzIGpRdWVyeVxuICovXG52YXIgdmlld2VySlMgPSAoIGZ1bmN0aW9uKCB2aWV3ZXIgKSB7XG4gICAgJ3VzZSBzdHJpY3QnO1xuICAgIFxuICAgIC8vIGRlZmF1bHQgdmFyaWFibGVzXG4gICAgdmFyIF9kZWJ1ZyA9IGZhbHNlO1xuICAgIHZhciBfcHJvbWlzZSA9IG51bGw7XG4gICAgdmFyIF9hcGlEYXRhID0gbnVsbDtcbiAgICB2YXIgX2RlZmF1bHRzID0ge1xuICAgICAgICBjb250ZXh0UGF0aDogbnVsbCxcbiAgICAgICAgYXBpUXVlcnk6ICdhcGk/YWN0aW9uPXRpbWVsaW5lJyxcbiAgICAgICAgc3RhcnREYXRlUXVlcnk6ICcmc3RhcnREYXRlPScsXG4gICAgICAgIHJhbmdlSW5wdXQxOiBudWxsLFxuICAgICAgICBzdGFydERhdGU6IG51bGwsXG4gICAgICAgIGVuZERhdGVRdWVyeTogJyZlbmREYXRlPScsXG4gICAgICAgIHJhbmdlSW5wdXQyOiBudWxsLFxuICAgICAgICBlbmREYXRlOiBudWxsLFxuICAgICAgICBjb3VudFF1ZXJ5OiAnJmNvdW50PScsXG4gICAgICAgIGNvdW50OiBudWxsLFxuICAgICAgICAkdG1DYW52YXM6IG51bGwsXG4gICAgICAgICR0bUNhbnZhc1BvczogbnVsbCxcbiAgICAgICAgJHRtQ2FudmFzQ29vcmRzOiB7fSxcbiAgICAgICAgbGFuZzoge31cbiAgICB9O1xuICAgIFxuICAgIHZpZXdlci50aW1lbWF0cml4ID0ge1xuICAgICAgICAvKipcbiAgICAgICAgICogTWV0aG9kIHRvIGluaXRpYWxpemUgdGhlIHRpbWVtYXRyaXggc2xpZGVyIGFuZCB0aGUgZXZlbnRzIHdoaWNoIGJ1aWxkcyB0aGVcbiAgICAgICAgICogbWF0cml4IGFuZCBwb3BvdmVycy5cbiAgICAgICAgICogXG4gICAgICAgICAqIEBtZXRob2QgaW5pdFxuICAgICAgICAgKiBAcGFyYW0ge09iamVjdH0gY29uZmlnIEFuIGNvbmZpZyBvYmplY3Qgd2hpY2ggb3ZlcndyaXRlcyB0aGUgZGVmYXVsdHMuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcuY29udGV4dFBhdGggVGhlIHJvb3RwYXRoIG9mIHRoZSBhcHBsaWNhdGlvbi5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5hcGlRdWVyeSBUaGUgQVBJIGFjdGlvbiB0byBjYWxsLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLnN0YXJ0RGF0ZVF1ZXJ5IFRoZSBHRVQtUGFyYW1ldGVyIGZvciB0aGUgc3RhcnQgZGF0ZS5cbiAgICAgICAgICogQHBhcmFtIHtPYmplY3R9IGNvbmZpZy5yYW5nZUlucHV0MSBBbiBqUXVlcnkgb2JqZWN0IG9mIHRoZSBmaXJzdCByYW5nZSBpbnB1dC5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5zdGFydERhdGUgVGhlIHZhbHVlIG9mIHRoZSBmaXJzdCByYW5nZSBpbnB1dC5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5lbmREYXRlUXVlcnkgVGhlIEdFVC1QYXJhbWV0ZXIgZm9yIHRoZSBlbmQgZGF0ZS5cbiAgICAgICAgICogQHBhcmFtIHtPYmplY3R9IGNvbmZpZy5yYW5nZUlucHV0MiBBbiBqUXVlcnkgb2JqZWN0IG9mIHRoZSBzZWNvbmQgcmFuZ2UgaW5wdXQuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcuZW5kRGF0ZSBUaGUgdmFsdWUgb2YgdGhlIHNlY29uZCByYW5nZSBpbnB1dC5cbiAgICAgICAgICogQHBhcmFtIHtTdHJpbmd9IGNvbmZpZy5jb3VudFF1ZXJ5IFRoZSBHRVQtUGFyYW1ldGVyIGZvciB0aGUgY291bnQgcXVlcnkuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcuY291bnQgVGhlIG51bWJlciBvZiByZXN1bHRzIGZyb20gdGhlIHF1ZXJ5LlxuICAgICAgICAgKiBAcGFyYW0ge09iamVjdH0gY29uZmlnLiR0bUNhbnZhcyBBbiBqUXVlcnkgb2JqZWN0IG9mIHRoZSB0aW1lbWF0cml4IGNhbnZhcy5cbiAgICAgICAgICogQHBhcmFtIHtPYmplY3R9IGNvbmZpZy4kdG1DYW52YXNQb3MgQW4galF1ZXJ5IG9iamVjdCBvZiB0aGUgdGltZW1hdHJpeCBjYW52YXNcbiAgICAgICAgICogcG9zaXRpb24uXG4gICAgICAgICAqIEBwYXJhbSB7T2JqZWN0fSBjb25maWcubGFuZyBBbiBvYmplY3Qgb2YgbG9jYWxpemVkIHN0cmluZ3MuXG4gICAgICAgICAqIEBleGFtcGxlXG4gICAgICAgICAqIFxuICAgICAgICAgKiA8cHJlPlxuICAgICAgICAgKiAkKCBkb2N1bWVudCApLnJlYWR5KCBmdW5jdGlvbigpIHtcbiAgICAgICAgICogICAgIHZhciB0aW1lbWF0cml4Q29uZmlnID0ge1xuICAgICAgICAgKiAgICAgICAgIHBhdGg6ICcje3JlcXVlc3QuY29udGV4dFBhdGh9LycsXG4gICAgICAgICAqICAgICAgICAgbGFuZzoge1xuICAgICAgICAgKiAgICAgICAgICAgICBjbG9zZVdpbmRvdzogJyN7bXNnLnRpbWVtYXRyaXhDbG9zZVdpbmRvd30nLFxuICAgICAgICAgKiAgICAgICAgICAgICBnb1RvV29yazogJyN7bXNnLnRpbWVtYXRyaXhHb1RvV29ya30nXG4gICAgICAgICAqICAgICAgICAgfVxuICAgICAgICAgKiAgICAgfTtcbiAgICAgICAgICogICAgIHZpZXdlckpTLnRpbWVtYXRyaXguaW5pdCggdGltZW1hdHJpeENvbmZpZyApO1xuICAgICAgICAgKiB9ICk7XG4gICAgICAgICAqIDwvcHJlPlxuICAgICAgICAgKi9cbiAgICAgICAgaW5pdDogZnVuY3Rpb24oIGNvbmZpZyApIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLnRpbWVtYXRyaXguaW5pdCcgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci50aW1lbWF0cml4LmluaXQ6IGNvbmZpZyAtICcgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggY29uZmlnICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgICQuZXh0ZW5kKCB0cnVlLCBfZGVmYXVsdHMsIGNvbmZpZyApO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBfZGVmYXVsdHMuJHRtQ2FudmFzQ29vcmRzID0ge1xuICAgICAgICAgICAgICAgIHRvcDogX2RlZmF1bHRzLiR0bUNhbnZhc1Bvcy50b3AsXG4gICAgICAgICAgICAgICAgbGVmdDogMCxcbiAgICAgICAgICAgICAgICByaWdodDogX2RlZmF1bHRzLiR0bUNhbnZhc1Bvcy5sZWZ0ICsgX2RlZmF1bHRzLiR0bUNhbnZhcy5vdXRlcldpZHRoKClcbiAgICAgICAgICAgIH07XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIHJhbmdlIHNsaWRlciBzZXR0aW5nc1xuICAgICAgICAgICAgJCggJyNzbGlkZXItcmFuZ2UnICkuc2xpZGVyKCB7XG4gICAgICAgICAgICAgICAgcmFuZ2U6IHRydWUsXG4gICAgICAgICAgICAgICAgbWluOiBwYXJzZUludCggX2RlZmF1bHRzLnN0YXJ0RGF0ZSApLFxuICAgICAgICAgICAgICAgIG1heDogcGFyc2VJbnQoIF9kZWZhdWx0cy5lbmREYXRlICksXG4gICAgICAgICAgICAgICAgdmFsdWVzOiBbIHBhcnNlSW50KCBfZGVmYXVsdHMuc3RhcnREYXRlICksIHBhcnNlSW50KCBfZGVmYXVsdHMuZW5kRGF0ZSApIF0sXG4gICAgICAgICAgICAgICAgc2xpZGU6IGZ1bmN0aW9uKCBldmVudCwgdWkgKSB7XG4gICAgICAgICAgICAgICAgICAgIF9kZWZhdWx0cy5yYW5nZUlucHV0MS52YWwoIHVpLnZhbHVlc1sgMCBdICk7XG4gICAgICAgICAgICAgICAgICAgICQoICcudGltZW1hdHJpeC1zbGlkZXItYnViYmxlLXN0YXJ0RGF0ZScgKS5odG1sKCB1aS52YWx1ZXNbIDAgXSApO1xuICAgICAgICAgICAgICAgICAgICBfZGVmYXVsdHMuc3RhcnREYXRlID0gdWkudmFsdWVzWyAwIF07XG4gICAgICAgICAgICAgICAgICAgIF9kZWZhdWx0cy5yYW5nZUlucHV0MS52YWwoIHVpLnZhbHVlc1sgMSBdICk7XG4gICAgICAgICAgICAgICAgICAgICQoICcudGltZW1hdHJpeC1zbGlkZXItYnViYmxlLWVuZERhdGUnICkuaHRtbCggdWkudmFsdWVzWyAxIF0gKTtcbiAgICAgICAgICAgICAgICAgICAgX2RlZmF1bHRzLmVuZERhdGUgPSB1aS52YWx1ZXNbIDEgXTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIGFwcGVuZCBzbGlkZXIgYnViYmxlIHRvIHNsaWRlciBoYW5kbGVcbiAgICAgICAgICAgICQoICcjc2xpZGVyLXJhbmdlIC51aS1zbGlkZXItcmFuZ2UnICkubmV4dCgpLmFwcGVuZCggX3JlbmRlclNsaWRlckJ1YmJsZSggJ3N0YXJ0RGF0ZScsIF9kZWZhdWx0cy5zdGFydERhdGUgKSApO1xuICAgICAgICAgICAgJCggJyNzbGlkZXItcmFuZ2UgLnVpLXNsaWRlci1yYW5nZScgKS5uZXh0KCkubmV4dCgpLmFwcGVuZCggX3JlbmRlclNsaWRlckJ1YmJsZSggJ2VuZERhdGUnLCBfZGVmYXVsdHMuZW5kRGF0ZSApICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIHNldCBhY3RpdmUgc2xpZGVyIGhhbmRsZSB0byB0b3BcbiAgICAgICAgICAgICQoICcudWktc2xpZGVyLWhhbmRsZScgKS5vbiggJ21vdXNlZG93bicsIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICQoICcudWktc2xpZGVyLWhhbmRsZScgKS5yZW1vdmVDbGFzcyggJ3RvcCcgKTtcbiAgICAgICAgICAgICAgICAkKCB0aGlzICkuYWRkQ2xhc3MoICd0b3AnICk7XG4gICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIGxpc3RlbiB0byBzdWJtaXQgZXZlbnQgb2YgbG9jYXRlIHRpbWVtYXRyaXggZm9ybVxuICAgICAgICAgICAgJCggJyNsb2NhdGVUaW1lbWF0cml4JyApLm9uKCAnc3VibWl0JywgZnVuY3Rpb24oIGUgKSB7XG4gICAgICAgICAgICAgICAgZS5wcmV2ZW50RGVmYXVsdCgpO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIC8vIGNoZWNrIGZvciBwb3BvdmVycyBhbmQgcmVtb3ZlIHRoZW1cbiAgICAgICAgICAgICAgICBpZiAoICQoICcudGltZW1hdHJpeC1wb3BvdmVyJyApLmxlbmd0aCApIHtcbiAgICAgICAgICAgICAgICAgICAgJCggJy50aW1lbWF0cml4LXBvcG92ZXInICkucmVtb3ZlKCk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIC8vIGJ1aWxkIGFwaSB0YXJnZXRcbiAgICAgICAgICAgICAgICB2YXIgYXBpVGFyZ2V0ID0gX2RlZmF1bHRzLmNvbnRleHRQYXRoO1xuICAgICAgICAgICAgICAgIGFwaVRhcmdldCArPSBfZGVmYXVsdHMuYXBpUXVlcnk7XG4gICAgICAgICAgICAgICAgYXBpVGFyZ2V0ICs9IF9kZWZhdWx0cy5zdGFydERhdGVRdWVyeTtcbiAgICAgICAgICAgICAgICBhcGlUYXJnZXQgKz0gX2RlZmF1bHRzLnN0YXJ0RGF0ZTtcbiAgICAgICAgICAgICAgICBhcGlUYXJnZXQgKz0gX2RlZmF1bHRzLmVuZERhdGVRdWVyeTtcbiAgICAgICAgICAgICAgICBhcGlUYXJnZXQgKz0gX2RlZmF1bHRzLmVuZERhdGU7XG4gICAgICAgICAgICAgICAgYXBpVGFyZ2V0ICs9IF9kZWZhdWx0cy5jb3VudFF1ZXJ5O1xuICAgICAgICAgICAgICAgIGFwaVRhcmdldCArPSBfZGVmYXVsdHMuY291bnQ7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLy8gZ2V0IGRhdGEgZnJvbSBhcGlcbiAgICAgICAgICAgICAgICBfcHJvbWlzZSA9IHZpZXdlci5oZWxwZXIuZ2V0UmVtb3RlRGF0YSggYXBpVGFyZ2V0ICk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLy8gcmVuZGVyIHRodW1ibmFpbHNcbiAgICAgICAgICAgICAgICBfcHJvbWlzZS50aGVuKCBmdW5jdGlvbiggZGF0YSApIHtcbiAgICAgICAgICAgICAgICAgICAgX2FwaURhdGEgPSBkYXRhO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgX2RlZmF1bHRzLiR0bUNhbnZhcy5odG1sKCBfcmVuZGVyVGh1bWJzKCBfYXBpRGF0YSApICk7XG4gICAgICAgICAgICAgICAgICAgICQoICcudGltZW1hdHJpeC10aHVtYicgKS5jc3MoIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGhlaWdodDogJCggJy50aW1lbWF0cml4LXRodW1iJyApLm91dGVyV2lkdGgoKVxuICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAvLyBzaG93IHRodW1icyBhZnRlciB0aGV5wrR2ZSBiZWVuIGxvYWRlZFxuICAgICAgICAgICAgICAgICAgICAkKCAnLnRpbWVtYXRyaXgtdGh1bWIgaW1nJyApLmxvYWQoIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgJCggdGhpcyApLmNzcygge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZpc2liaWxpdHk6ICd2aXNpYmxlJ1xuICAgICAgICAgICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAvLyBsaXN0ZW4gdG8gY2xpY2sgZXZlbnQgb24gdGh1bWJuYWlsc1xuICAgICAgICAgICAgICAgICAgICAkKCAnLnRpbWVtYXRyaXgtdGh1bWInICkub24oICdjbGljaycsIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKCAhJCggJy50aW1lbWF0cml4LXBvcG92ZXInICkgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgJCggJy50aW1lbWF0cml4LXRodW1iJyApLnJlbW92ZUNsYXNzKCAnbWFya2VyJyApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICQoIHRoaXMgKS5hZGRDbGFzcyggJ21hcmtlcicgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBfcmVuZGVyUG9wb3ZlciggJCggdGhpcyApLCBfZGVmYXVsdHMubGFuZyApO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgJCggJy50aW1lbWF0cml4LXBvcG92ZXInICkucmVtb3ZlKCk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgJCggJy50aW1lbWF0cml4LXRodW1iJyApLnJlbW92ZUNsYXNzKCAnbWFya2VyJyApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICQoIHRoaXMgKS5hZGRDbGFzcyggJ21hcmtlcicgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBfcmVuZGVyUG9wb3ZlciggJCggdGhpcyApLCBfZGVmYXVsdHMubGFuZyApO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICAvLyBjbG9zZSBwb3BvdmVyXG4gICAgICAgICAgICAgICAgICAgICAgICAkKCAnLnRpbWVtYXRyaXgtcG9wb3Zlci1jbG9zZScgKS5vbiggJ2NsaWNrJywgZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgJCggdGhpcyApLnBhcmVudCgpLnJlbW92ZSgpO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICQoICcudGltZW1hdHJpeC10aHVtYicgKS5yZW1vdmVDbGFzcyggJ21hcmtlcicgKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgLy8gY2hlY2sgaWYgaW1hZ2UgaXMgbG9hZGVkIGFuZCByZXNldCBsb2FkZXJcbiAgICAgICAgICAgICAgICAgICAgICAgICQoICcudGltZW1hdHJpeC1wb3BvdmVyLWJvZHkgaW1nJyApLmxvYWQoIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICQoICcudGltZW1hdHJpeC1wb3BvdmVyLWltYWdlbG9hZGVyJyApLmhpZGUoKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgICAgIH0gKS50aGVuKCBudWxsLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAgICAgX2RlZmF1bHRzLiR0bUNhbnZhcy5hcHBlbmQoIHZpZXdlci5oZWxwZXIucmVuZGVyQWxlcnQoICdhbGVydC1kYW5nZXInLCAnPHN0cm9uZz5TdGF0dXM6IDwvc3Ryb25nPicgKyBlcnJvci5zdGF0dXMgKyAnICcgKyBlcnJvci5zdGF0dXNUZXh0LCBmYWxzZSApICk7XG4gICAgICAgICAgICAgICAgICAgIGNvbnNvbGUuZXJyb3IoICdFUlJPUjogdmlld2VyLnRpbWVtYXRyaXguaW5pdCAtICcsIGVycm9yICk7XG4gICAgICAgICAgICAgICAgfSApXG4gICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIHJlbW92ZSBhbGwgcG9wb3ZlcnMgYnkgY2xpY2tpbmcgb24gYm9keVxuICAgICAgICAgICAgJCggJ2JvZHknICkub24oICdjbGljaycsIGZ1bmN0aW9uKCBldmVudCApIHtcbiAgICAgICAgICAgICAgICBpZiAoICQoIGV2ZW50LnRhcmdldCApLmNsb3Nlc3QoICcudGltZW1hdHJpeC10aHVtYicgKS5sZW5ndGggKSB7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIF9yZW1vdmVQb3BvdmVycygpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgfVxuICAgIH07XG4gICAgXG4gICAgLyoqXG4gICAgICogTWV0aG9kIHRvIHJlbmRlciBpbWFnZSB0aHVtYm5haWxzIHRvIHRoZSB0aW1lbWF0cml4IGNhbnZhcy5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9yZW5kZXJUaHVtYnNcbiAgICAgKiBAcGFyYW0ge09iamVjdH0ganNvbiBBbiBKU09OLU9iamVjdCB3aGljaCBjb250YWlucyB0aGUgaW1hZ2UgZGF0YS5cbiAgICAgKiBAcmV0dXJucyB7U3RyaW5nfSBIVE1MLVN0cmluZyB3aGljaCBkaXNwbGF5cyB0aGUgaW1hZ2UgdGh1bWJuYWlscy5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBfcmVuZGVyVGh1bWJzKCBqc29uICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLnRpbWVtYXRyaXggX3JlbmRlclRodW1iczoganNvbiAtICcgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCBqc29uICk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIHZhciB0bGJveCA9ICcnO1xuICAgICAgICB0bGJveCArPSAnPGRpdiBjbGFzcz1cInRpbWVtYXRyaXgtYm94XCI+JztcbiAgICAgICAgdGxib3ggKz0gJzxoZWFkZXIgY2xhc3M9XCJ0aW1lbWF0cml4LWhlYWRlclwiPic7XG4gICAgICAgIGlmICggX2RlZmF1bHRzLnN0YXJ0RGF0ZSAhPT0gJycgJiYgX2RlZmF1bHRzLmVuZERhdGUgIT09ICcnICkge1xuICAgICAgICAgICAgdGxib3ggKz0gJzxoMz4nICsgX2RlZmF1bHRzLnN0YXJ0RGF0ZSArICcgLSAnICsgX2RlZmF1bHRzLmVuZERhdGUgKyAnPC9oMz4nO1xuICAgICAgICB9XG4gICAgICAgIHRsYm94ICs9ICc8L2hlYWRlcj4nO1xuICAgICAgICB0bGJveCArPSAnPHNlY3Rpb24gY2xhc3M9XCJ0aW1lbWF0cml4LWJvZHlcIj4nO1xuICAgICAgICAkLmVhY2goIGpzb24sIGZ1bmN0aW9uKCBpLCBqICkge1xuICAgICAgICAgICAgdGxib3ggKz0gJzxkaXYgY2xhc3M9XCJ0aW1lbWF0cml4LXRodW1iXCIgZGF0YS10aXRsZT1cIicgKyBqLnRpdGxlICsgJ1wiIGRhdGEtbWVkaXVtaW1hZ2U9XCInICsgai5tZWRpdW1pbWFnZSArICdcIiBkYXRhLXVybD1cIicgKyBqLnVybCArICdcIj4nO1xuICAgICAgICAgICAgaWYgKCBqLnRodW1ibmFpbFVybCApIHtcbiAgICAgICAgICAgICAgICB0bGJveCArPSAnPGltZyBzcmM9XCInICsgai50aHVtYm5haWxVcmwgKyAnXCIgc3R5bGU9XCJ2aXNpYmlsaXR5OiBoaWRkZW47XCIgLz4nO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgdGxib3ggKz0gJyc7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICB0bGJveCArPSAnPC9kaXY+JztcbiAgICAgICAgfSApO1xuICAgICAgICB0bGJveCArPSAnPC9zZWN0aW9uPic7XG4gICAgICAgIHRsYm94ICs9ICc8Zm9vdGVyIGNsYXNzPVwidGltZW1hdHJpeC1mb290ZXJcIj48L2Zvb3Rlcj4nO1xuICAgICAgICB0bGJveCArPSAnPC9kaXY+JztcbiAgICAgICAgXG4gICAgICAgIHJldHVybiB0bGJveDtcbiAgICB9XG4gICAgXG4gICAgLyoqXG4gICAgICogTWV0aG9kIHRvIHJlbmRlciBhIHBvcG92ZXIgd2l0aCBhIHRodW1ibmFpbCBpbWFnZS5cbiAgICAgKiBcbiAgICAgKiBAbWV0aG9kIF9yZW5kZXJQb3BvdmVyXG4gICAgICogQHBhcmFtIHtPYmplY3R9ICRPYmogTXVzdCBiZSBhbiBqUXVlcnktT2JqZWN0IGxpa2UgJCh0aGlzKS5cbiAgICAgKiBAcGFyYW0ge09iamVjdH0gbGFuZyBBbiBPYmplY3Qgd2l0aCBsb2NhbGl6ZWQgc3RyaW5ncyBpbiB0aGUgc2VsZWN0ZWQgbGFuZ3VhZ2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gX3JlbmRlclBvcG92ZXIoICRPYmosIGxhbmcgKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIudGltZW1hdHJpeCBfcmVuZGVyUG9wb3Zlcjogb2JqIC0gJyApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coICRPYmogKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLnRpbWVtYXRyaXggX3JlbmRlclBvcG92ZXI6IGxhbmcgLSAnICsgbGFuZyApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgdGl0bGUgPSAkT2JqLmF0dHIoICdkYXRhLXRpdGxlJyApO1xuICAgICAgICB2YXIgbWVkaXVtaW1hZ2UgPSAkT2JqLmF0dHIoICdkYXRhLW1lZGl1bWltYWdlJyApO1xuICAgICAgICB2YXIgdXJsID0gJE9iai5hdHRyKCAnZGF0YS11cmwnICk7XG4gICAgICAgIHZhciAkb2JqUG9zID0gJE9iai5wb3NpdGlvbigpO1xuICAgICAgICB2YXIgJG9iakNvb3JkcyA9IHtcbiAgICAgICAgICAgIHRvcDogJG9ialBvcy50b3AsXG4gICAgICAgICAgICBsZWZ0OiAkb2JqUG9zLmxlZnQsXG4gICAgICAgICAgICB3aWR0aDogJE9iai5vdXRlcldpZHRoKClcbiAgICAgICAgfTtcbiAgICAgICAgdmFyIHBvcG92ZXJQb3MgPSBfY2FsY3VsYXRlUG9wb3ZlclBvc2l0aW9uKCAkb2JqQ29vcmRzLCBfZGVmYXVsdHMuJHRtQ2FudmFzQ29vcmRzICk7XG4gICAgICAgIHZhciBwb3BvdmVyID0gJyc7XG4gICAgICAgIHBvcG92ZXIgKz0gJzxkaXYgY2xhc3M9XCJ0aW1lbWF0cml4LXBvcG92ZXJcIiBzdHlsZT1cInRvcDogJyArIHBvcG92ZXJQb3MudG9wICsgJ3B4OyBsZWZ0OiAnICsgcG9wb3ZlclBvcy5sZWZ0ICsgJ3B4O1wiPic7XG4gICAgICAgIHBvcG92ZXIgKz0gJzxzcGFuIGNsYXNzPVwidGltZW1hdHJpeC1wb3BvdmVyLWNsb3NlXCIgdGl0bGU9XCInICsgbGFuZy5jbG9zZVdpbmRvdyArICdcIj4mdGltZXM7PC9zcGFuPic7XG4gICAgICAgIHBvcG92ZXIgKz0gJzxoZWFkZXIgY2xhc3M9XCJ0aW1lbWF0cml4LXBvcG92ZXItaGVhZGVyXCI+JztcbiAgICAgICAgcG9wb3ZlciArPSAnPGg0IHRpdGxlPVwiJyArIHRpdGxlICsgJ1wiPicgKyB2aWV3ZXIuaGVscGVyLnRydW5jYXRlU3RyaW5nKCB0aXRsZSwgNzUgKSArICc8L2g0Pic7XG4gICAgICAgIHBvcG92ZXIgKz0gJzwvaGVhZGVyPic7XG4gICAgICAgIHBvcG92ZXIgKz0gJzxzZWN0aW9uIGNsYXNzPVwidGltZW1hdHJpeC1wb3BvdmVyLWJvZHlcIj4nO1xuICAgICAgICBwb3BvdmVyICs9ICc8ZGl2IGNsYXNzPVwidGltZW1hdHJpeC1wb3BvdmVyLWltYWdlbG9hZGVyXCI+PC9kaXY+JztcbiAgICAgICAgcG9wb3ZlciArPSAnPGEgaHJlZj1cIicgKyB1cmwgKyAnXCI+PGltZyBzcmM9XCInICsgbWVkaXVtaW1hZ2UgKyAnXCIgLz48L2E+JztcbiAgICAgICAgcG9wb3ZlciArPSAnPC9zZWN0aW9uPic7XG4gICAgICAgIHBvcG92ZXIgKz0gJzxmb290ZXIgY2xhc3M9XCJ0aW1lbWF0cml4LXBvcG92ZXItZm9vdGVyXCI+JztcbiAgICAgICAgcG9wb3ZlciArPSAnPGEgaHJlZj1cIicgKyB1cmwgKyAnXCIgdGl0bGU9XCInICsgdGl0bGUgKyAnXCI+JyArIGxhbmcuZ29Ub1dvcmsgKyAnPC9hPic7XG4gICAgICAgIHBvcG92ZXIgKz0gJzwvZm9vdGVyPic7XG4gICAgICAgIHBvcG92ZXIgKz0gJzwvZGl2Pic7XG4gICAgICAgIFxuICAgICAgICBfZGVmYXVsdHMuJHRtQ2FudmFzLmFwcGVuZCggcG9wb3ZlciApO1xuICAgIH1cbiAgICBcbiAgICAvKipcbiAgICAgKiBNZXRob2Qgd2hpY2ggY2FsY3VsYXRlcyB0aGUgcG9zaXRpb24gb2YgdGhlIHBvcG92ZXIuXG4gICAgICogXG4gICAgICogQG1ldGhvZCBfY2FsY3VsYXRlUG9wb3ZlclBvc2l0aW9uXG4gICAgICogQHBhcmFtIHtPYmplY3R9IHRyaWdnZXJDb29yZHMgQW4gb2JqZWN0IHdoaWNoIGNvbnRhaW5zIHRoZSBjb29yZGluYXRlcyBvZiB0aGVcbiAgICAgKiBlbGVtZW50IGhhcyBiZWVuIGNsaWNrZWQuXG4gICAgICogQHBhcmFtIHtPYmplY3R9IHRtQ2FudmFzQ29vcmRzIEFuIG9iamVjdCB3aGljaCBjb250YWlucyB0aGUgY29vcmRpbmF0ZXMgb2YgdGhlXG4gICAgICogd3JhcHBlciBlbGVtZW50IGZyb20gdGhlIHRpbWVtYXRyaXguXG4gICAgICogQHJldHVybnMge09iamVjdH0gQW4gb2JqZWN0IHdoaWNoIGNvbnRhaW5zIHRoZSB0b3AgYW5kIHRoZSBsZWZ0IHBvc2l0aW9uIG9mIHRoZVxuICAgICAqIHBvcG92ZXIuXG4gICAgICovXG4gICAgZnVuY3Rpb24gX2NhbGN1bGF0ZVBvcG92ZXJQb3NpdGlvbiggdHJpZ2dlckNvb3JkcywgdG1DYW52YXNDb29yZHMgKSB7XG4gICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIudGltZW1hdHJpeCBfY2FsY3VsYXRlUG9wb3ZlclBvc2l0aW9uOiB0cmlnZ2VyQ29vcmRzIC0gJyApO1xuICAgICAgICAgICAgY29uc29sZS5sb2coIHRyaWdnZXJDb29yZHMgKTtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLnRpbWVtYXRyaXggX2NhbGN1bGF0ZVBvcG92ZXJQb3NpdGlvbjogdG1DYW52YXNDb29yZHMgLSAnICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggdG1DYW52YXNDb29yZHMgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgdmFyIHBvTGVmdEJvcmRlciA9IHRyaWdnZXJDb29yZHMubGVmdCAtICggMTUwIC0gKCB0cmlnZ2VyQ29vcmRzLndpZHRoIC8gMiApICk7XG4gICAgICAgIHZhciBwb1JpZ2h0Qm9yZGVyID0gcG9MZWZ0Qm9yZGVyICsgMzAwO1xuICAgICAgICB2YXIgdGJMZWZ0Qm9yZGVyID0gdG1DYW52YXNDb29yZHMubGVmdDtcbiAgICAgICAgdmFyIHRiUmlnaHRCb3JkZXIgPSB0bUNhbnZhc0Nvb3Jkcy5yaWdodDtcbiAgICAgICAgdmFyIHBvVG9wO1xuICAgICAgICB2YXIgcG9MZWZ0ID0gcG9MZWZ0Qm9yZGVyO1xuICAgICAgICBcbiAgICAgICAgcG9Ub3AgPSAoIHRyaWdnZXJDb29yZHMudG9wICsgJCggJy50aW1lbWF0cml4LXRodW1iJyApLm91dGVySGVpZ2h0KCkgKSAtIDE7XG4gICAgICAgIFxuICAgICAgICBpZiAoIHBvTGVmdEJvcmRlciA8PSB0YkxlZnRCb3JkZXIgKSB7XG4gICAgICAgICAgICBwb0xlZnQgPSB0YkxlZnRCb3JkZXI7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIGlmICggcG9SaWdodEJvcmRlciA+PSB0YlJpZ2h0Qm9yZGVyICkge1xuICAgICAgICAgICAgcG9MZWZ0ID0gdG1DYW52YXNDb29yZHMucmlnaHQgLSAzMDA7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICB0b3A6IHBvVG9wLFxuICAgICAgICAgICAgbGVmdDogcG9MZWZ0XG4gICAgICAgIH07XG4gICAgfVxuICAgIFxuICAgIC8qKlxuICAgICAqIE1ldGhvZCB3aGljaCByZW5kZXJzIHRoZSBidWJibGVzIGZvciB0aGUgc2xpZGVyLlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX3JlbmRlclNsaWRlckJ1YmJsZVxuICAgICAqIEBwYXJhbSB7U3RyaW5nfSB0aW1lIFRoZSBzdHJpbmcgZm9yIHRoZSB0aW1lIHZhbHVlLlxuICAgICAqIEBwYXJhbSB7U3RyaW5nfSB2YWwgVGhlIHN0cmluZyBmb3IgdGhlIHZhbHVlLlxuICAgICAqIEByZXR1cm5zIHtTdHJpbmd9IEhUTUwtU3RyaW5nIHdoaWNoIHJlbmRlcnMgdGhlIHNsaWRlci1idWJibGUuXG4gICAgICovXG4gICAgZnVuY3Rpb24gX3JlbmRlclNsaWRlckJ1YmJsZSggdGltZSwgdmFsICkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLnRpbWVtYXRyaXggX3JlbmRlclNsaWRlckJ1YmJsZTogdGltZSAtICcgKyB0aW1lICk7XG4gICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci50aW1lbWF0cml4IF9yZW5kZXJTbGlkZXJCdWJibGU6IHZhbCAtICcgKyB2YWwgKTtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgcmV0dXJuICc8ZGl2IGNsYXNzPVwidGltZW1hdHJpeC1zbGlkZXItYnViYmxlLScgKyB0aW1lICsgJ1wiPicgKyB2YWwgKyAnPC9kaXY+JztcbiAgICB9XG4gICAgXG4gICAgLyoqXG4gICAgICogTWV0aG9kIHdoaWNoIHJlbW92ZXMgYWxsIHBvcG92ZXJzLlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgX3JlbW92ZVBvcG92ZXJzXG4gICAgICovXG4gICAgZnVuY3Rpb24gX3JlbW92ZVBvcG92ZXJzKCkge1xuICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnLS0tLS0tLS0tLSBfcmVtb3ZlUG9wb3ZlcnMoKSAtLS0tLS0tLS0tJyApO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICAkKCAnLnRpbWVtYXRyaXgtcG9wb3ZlcicgKS5yZW1vdmUoKTtcbiAgICAgICAgJCggJy50aW1lbWF0cml4LXRodW1iJyApLnJlbW92ZUNsYXNzKCAnbWFya2VyJyApO1xuICAgIH1cbiAgICBcbiAgICByZXR1cm4gdmlld2VyO1xuICAgIFxufSApKCB2aWV3ZXJKUyB8fCB7fSwgalF1ZXJ5ICk7XG4iLCIvKipcbiAqIFRoaXMgZmlsZSBpcyBwYXJ0IG9mIHRoZSBHb29iaSB2aWV3ZXIgLSBhIGNvbnRlbnQgcHJlc2VudGF0aW9uIGFuZCBtYW5hZ2VtZW50XG4gKiBhcHBsaWNhdGlvbiBmb3IgZGlnaXRpemVkIG9iamVjdHMuXG4gKiBcbiAqIFZpc2l0IHRoZXNlIHdlYnNpdGVzIGZvciBtb3JlIGluZm9ybWF0aW9uLiAtIGh0dHA6Ly93d3cuaW50cmFuZGEuY29tIC1cbiAqIGh0dHA6Ly9kaWdpdmVyc28uY29tXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBmcmVlIHNvZnR3YXJlOyB5b3UgY2FuIHJlZGlzdHJpYnV0ZSBpdCBhbmQvb3IgbW9kaWZ5IGl0IHVuZGVyIHRoZSB0ZXJtc1xuICogb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFzIHB1Ymxpc2hlZCBieSB0aGUgRnJlZSBTb2Z0d2FyZSBGb3VuZGF0aW9uOyBlaXRoZXJcbiAqIHZlcnNpb24gMiBvZiB0aGUgTGljZW5zZSwgb3IgKGF0IHlvdXIgb3B0aW9uKSBhbnkgbGF0ZXIgdmVyc2lvbi5cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGRpc3RyaWJ1dGVkIGluIHRoZSBob3BlIHRoYXQgaXQgd2lsbCBiZSB1c2VmdWwsIGJ1dCBXSVRIT1VUIEFOWVxuICogV0FSUkFOVFk7IHdpdGhvdXQgZXZlbiB0aGUgaW1wbGllZCB3YXJyYW50eSBvZiBNRVJDSEFOVEFCSUxJVFkgb3IgRklUTkVTUyBGT1IgQVxuICogUEFSVElDVUxBUiBQVVJQT1NFLiBTZWUgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGZvciBtb3JlIGRldGFpbHMuXG4gKiBcbiAqIFlvdSBzaG91bGQgaGF2ZSByZWNlaXZlZCBhIGNvcHkgb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFsb25nIHdpdGggdGhpc1xuICogcHJvZ3JhbS4gSWYgbm90LCBzZWUgPGh0dHA6Ly93d3cuZ251Lm9yZy9saWNlbnNlcy8+LlxuICogXG4gKiA8U2hvcnQgTW9kdWxlIERlc2NyaXB0aW9uPlxuICogXG4gKiBAdmVyc2lvbiAzLjIuMFxuICogQG1vZHVsZSB2aWV3ZXJKUy50aW55TWNlXG4gKiBAcmVxdWlyZXMgalF1ZXJ5XG4gKi9cbnZhciB2aWV3ZXJKUyA9ICggZnVuY3Rpb24oIHZpZXdlciApIHtcbiAgICAndXNlIHN0cmljdCc7XG4gICAgXG4gICAgdmFyIF9kZWJ1ZyA9IGZhbHNlO1xuICAgIHZhciBfZGVmYXVsdHMgPSB7XG4gICAgICAgIGN1cnJMYW5nOiAnZGUnLFxuICAgICAgICBzZWxlY3RvcjogJ3RleHRhcmVhLnRpbnlNQ0UnLFxuICAgICAgICB3aWR0aDogJzEwMCUnLFxuICAgICAgICBoZWlnaHQ6IDQwMCxcbiAgICAgICAgdGhlbWU6ICdtb2Rlcm4nLFxuICAgICAgICBwbHVnaW5zOiAncHJpbnQgcHJldmlldyBwYXN0ZSBzZWFyY2hyZXBsYWNlIGF1dG9saW5rIGRpcmVjdGlvbmFsaXR5IGNvZGUgdmlzdWFsYmxvY2tzIHZpc3VhbGNoYXJzIGZ1bGxzY3JlZW4gaW1hZ2UgbGluayBtZWRpYSB0ZW1wbGF0ZSBjb2Rlc2FtcGxlIHRhYmxlIGNoYXJtYXAgaHIgcGFnZWJyZWFrIG5vbmJyZWFraW5nIGFuY2hvciB0b2MgaW5zZXJ0ZGF0ZXRpbWUgYWR2bGlzdCBsaXN0cyB0ZXh0Y29sb3Igd29yZGNvdW50IHNwZWxsY2hlY2tlciBpbWFnZXRvb2xzIG1lZGlhIGNvbnRleHRtZW51IGNvbG9ycGlja2VyIHRleHRwYXR0ZXJuIGhlbHAnLFxuICAgICAgICB0b29sYmFyOiAnZm9ybWF0c2VsZWN0IHwgdW5kbyByZWRvIHwgYm9sZCBpdGFsaWMgdW5kZXJsaW5lIHN0cmlrZXRocm91Z2ggZm9yZWNvbG9yIGJhY2tjb2xvciB8IGxpbmsgfCBhbGlnbmxlZnQgYWxpZ25jZW50ZXIgYWxpZ25yaWdodCBhbGlnbmp1c3RpZnkgfCBidWxsaXN0IG51bWxpc3Qgb3V0ZGVudCBpbmRlbnQgfCBmdWxsc2NyZWVuIGNvZGUnLFxuICAgICAgICBtZW51YmFyOiBmYWxzZSxcbiAgICAgICAgc3RhdHVzYmFyOiBmYWxzZSxcbiAgICAgICAgcGFnZWJyZWFrX3NlcGFyYXRvcjogJzxzcGFuIGNsYXNzPVwicGFnZWJyZWFrXCI+PC9zcGFuPicsXG4gICAgICAgIHJlbGF0aXZlX3VybHM6IGZhbHNlLFxuICAgICAgICBmb3JjZV9icl9uZXdsaW5lczogZmFsc2UsXG4gICAgICAgIGZvcmNlX3BfbmV3bGluZXM6IGZhbHNlLFxuICAgICAgICBmb3JjZWRfcm9vdF9ibG9jazogJycsXG4gICAgICAgIGxhbmd1YWdlOiAnZGUnXG4gICAgfTtcbiAgICBcbiAgICB2aWV3ZXIudGlueU1jZSA9IHtcbiAgICAgICAgaW5pdDogZnVuY3Rpb24oIGNvbmZpZyApIHtcbiAgICAgICAgICAgIGlmICggX2RlYnVnICkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAnIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjJyApO1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCAndmlld2VyLnRpbnlNY2UuaW5pdCcgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci50aW55TWNlLmluaXQ6IGNvbmZpZyAtICcsIGNvbmZpZyApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICAkLmV4dGVuZCggdHJ1ZSwgX2RlZmF1bHRzLCBjb25maWcgKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gY2hlY2sgY3VycmVudCBsYW5ndWFnZVxuICAgICAgICAgICAgc3dpdGNoICggX2RlZmF1bHRzLmN1cnJMYW5nICkge1xuICAgICAgICAgICAgICAgIGNhc2UgJ2RlJzpcbiAgICAgICAgICAgICAgICAgICAgX2RlZmF1bHRzLmxhbmd1YWdlID0gJ2RlJztcbiAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgY2FzZSAnZXMnOlxuICAgICAgICAgICAgICAgICAgICBfZGVmYXVsdHMubGFuZ3VhZ2UgPSAnZXMnO1xuICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICBjYXNlICdwdCc6XG4gICAgICAgICAgICAgICAgICAgIF9kZWZhdWx0cy5sYW5ndWFnZSA9ICdwdF9QVCc7XG4gICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgIGNhc2UgJ3J1JzpcbiAgICAgICAgICAgICAgICAgICAgX2RlZmF1bHRzLmxhbmd1YWdlID0gJ3J1JztcbiAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIGluaXQgZWRpdG9yXG4gICAgICAgICAgICB0aW55bWNlLmluaXQoIF9kZWZhdWx0cyApO1xuICAgICAgICB9LFxuICAgICAgICBvdmVydmlldzogZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAvLyBjaGVjayBpZiBkZXNjcmlwdGlvbiBvciBwdWJsaWNhdGlvbiBlZGl0aW5nIGlzIGVuYWJsZWQgYW5kXG4gICAgICAgICAgICAvLyBzZXQgZnVsbHNjcmVlbiBvcHRpb25zXG4gICAgICAgICAgICBpZiAoICQoICcub3ZlcnZpZXdfX2Rlc2NyaXB0aW9uLWVkaXRvcicgKS5sZW5ndGggPiAwICkge1xuICAgICAgICAgICAgICAgIHZpZXdlckpTLnRpbnlDb25maWcuc2V0dXAgPSBmdW5jdGlvbiggZWRpdG9yICkge1xuICAgICAgICAgICAgICAgICAgICBlZGl0b3Iub24oICdpbml0JywgZnVuY3Rpb24oIGUgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAkKCAnLm92ZXJ2aWV3X19wdWJsaWNhdGlvbi1hY3Rpb24gLmJ0bicgKS5oaWRlKCk7XG4gICAgICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICAgICAgZWRpdG9yLm9uKCAnRnVsbHNjcmVlblN0YXRlQ2hhbmdlZCcsIGZ1bmN0aW9uKCBlICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKCBlLnN0YXRlICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICQoICcub3ZlcnZpZXdfX2Rlc2NyaXB0aW9uLWFjdGlvbi1mdWxsc2NyZWVuJyApLmFkZENsYXNzKCAnaW4nICk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCAnLm92ZXJ2aWV3X19kZXNjcmlwdGlvbi1hY3Rpb24tZnVsbHNjcmVlbicgKS5yZW1vdmVDbGFzcyggJ2luJyApO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIHZpZXdlckpTLnRpbnlDb25maWcuc2V0dXAgPSBmdW5jdGlvbiggZWRpdG9yICkge1xuICAgICAgICAgICAgICAgICAgICBlZGl0b3Iub24oICdpbml0JywgZnVuY3Rpb24oIGUgKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAkKCAnLm92ZXJ2aWV3X19kZXNjcmlwdGlvbi1hY3Rpb24gLmJ0bicgKS5oaWRlKCk7XG4gICAgICAgICAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgICAgICAgICAgZWRpdG9yLm9uKCAnRnVsbHNjcmVlblN0YXRlQ2hhbmdlZCcsIGZ1bmN0aW9uKCBlICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKCBlLnN0YXRlICkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICQoICcub3ZlcnZpZXdfX3B1YmxpY2F0aW9uLWFjdGlvbi1mdWxsc2NyZWVuJyApLmFkZENsYXNzKCAnaW4nICk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAkKCAnLm92ZXJ2aWV3X19wdWJsaWNhdGlvbi1hY3Rpb24tZnVsbHNjcmVlbicgKS5yZW1vdmVDbGFzcyggJ2luJyApO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB9ICk7XG4gICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSxcbiAgICB9O1xuICAgIFxuICAgIHJldHVybiB2aWV3ZXI7XG4gICAgXG59ICkoIHZpZXdlckpTIHx8IHt9LCBqUXVlcnkgKTtcbiIsIi8qKlxuICogVGhpcyBmaWxlIGlzIHBhcnQgb2YgdGhlIEdvb2JpIHZpZXdlciAtIGEgY29udGVudCBwcmVzZW50YXRpb24gYW5kIG1hbmFnZW1lbnRcbiAqIGFwcGxpY2F0aW9uIGZvciBkaWdpdGl6ZWQgb2JqZWN0cy5cbiAqIFxuICogVmlzaXQgdGhlc2Ugd2Vic2l0ZXMgZm9yIG1vcmUgaW5mb3JtYXRpb24uIC0gaHR0cDovL3d3dy5pbnRyYW5kYS5jb20gLVxuICogaHR0cDovL2RpZ2l2ZXJzby5jb21cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGZyZWUgc29mdHdhcmU7IHlvdSBjYW4gcmVkaXN0cmlidXRlIGl0IGFuZC9vciBtb2RpZnkgaXQgdW5kZXIgdGhlIHRlcm1zXG4gKiBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYXMgcHVibGlzaGVkIGJ5IHRoZSBGcmVlIFNvZnR3YXJlIEZvdW5kYXRpb247IGVpdGhlclxuICogdmVyc2lvbiAyIG9mIHRoZSBMaWNlbnNlLCBvciAoYXQgeW91ciBvcHRpb24pIGFueSBsYXRlciB2ZXJzaW9uLlxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZGlzdHJpYnV0ZWQgaW4gdGhlIGhvcGUgdGhhdCBpdCB3aWxsIGJlIHVzZWZ1bCwgYnV0IFdJVEhPVVQgQU5ZXG4gKiBXQVJSQU5UWTsgd2l0aG91dCBldmVuIHRoZSBpbXBsaWVkIHdhcnJhbnR5IG9mIE1FUkNIQU5UQUJJTElUWSBvciBGSVRORVNTIEZPUiBBXG4gKiBQQVJUSUNVTEFSIFBVUlBPU0UuIFNlZSB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgZm9yIG1vcmUgZGV0YWlscy5cbiAqIFxuICogWW91IHNob3VsZCBoYXZlIHJlY2VpdmVkIGEgY29weSBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYWxvbmcgd2l0aCB0aGlzXG4gKiBwcm9ncmFtLiBJZiBub3QsIHNlZSA8aHR0cDovL3d3dy5nbnUub3JnL2xpY2Vuc2VzLz4uXG4gKiBcbiAqIE1vZHVsZSB0byBtYW5hZ2UgdGhlIHVzZXIgZHJvcGRvd24uXG4gKiBcbiAqIEB2ZXJzaW9uIDMuMi4wXG4gKiBAbW9kdWxlIHZpZXdlckpTLnVzZXJEcm9wZG93blxuICogQHJlcXVpcmVzIGpRdWVyeVxuICovXG52YXIgdmlld2VySlMgPSAoIGZ1bmN0aW9uKCB2aWV3ZXIgKSB7XG4gICAgJ3VzZSBzdHJpY3QnO1xuICAgIFxuICAgIHZhciBfZGVidWcgPSBmYWxzZTtcbiAgICB2YXIgX2Jvb2tzZWxmRHJvcGRvd24gPSBmYWxzZTtcbiAgICB2YXIgX2RlZmF1bHRzID0ge307XG4gICAgXG4gICAgdmlld2VyLnVzZXJEcm9wZG93biA9IHtcbiAgICAgICAgaW5pdDogZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICBpZiAoIF9kZWJ1ZyApIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci51c2VyRHJvcGRvd24uaW5pdCcgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gY2hlY2sgaWYgYm9va3NoZWxmZHJvcGRvd24gZXhpc3RcbiAgICAgICAgICAgIGlmICggJCggJy5ib29rc2hlbGYtbmF2aWdhdGlvbl9fZHJvcGRvd24nICkubGVuZ3RoID4gMCApIHtcbiAgICAgICAgICAgICAgICBfYm9va3NlbGZEcm9wZG93biA9IHRydWU7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIGxvZ2luIGRyb3Bkb3duXG4gICAgICAgICAgICAkKCAnW2RhdGEtdG9nZ2xlPVwibG9naW4tZHJvcGRvd25cIl0nICkub24oICdjbGljaycsIGZ1bmN0aW9uKCBldmVudCApIHtcbiAgICAgICAgICAgICAgICBldmVudC5zdG9wUHJvcGFnYXRpb24oKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAvLyBoaWRlIGJvb2tzaGVsZmRyb3Bkb3cgaWYgZXhpc3RcbiAgICAgICAgICAgICAgICBpZiAoIF9ib29rc2VsZkRyb3Bkb3duICkge1xuICAgICAgICAgICAgICAgICAgICAkKCAnLmJvb2tzaGVsZi1uYXZpZ2F0aW9uX19kcm9wZG93bicgKS5oaWRlKCk7XG4gICAgICAgICAgICAgICAgICAgICQoICcuYm9va3NoZWxmLXBvcHVwJyApLnJlbW92ZSgpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAvLyBoaWRlIGNvbGxlY3Rpb24gcGFuZWwgaWYgZXhpc3RcbiAgICAgICAgICAgICAgICBpZiAoICQoICcubmF2aWdhdGlvbl9fY29sbGVjdGlvbi1wYW5lbCcgKS5sZW5ndGggPiAwICkge1xuICAgICAgICAgICAgICAgICAgICAkKCAnLm5hdmlnYXRpb25fX2NvbGxlY3Rpb24tcGFuZWwnICkuaGlkZSgpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAkKCAnLmxvZ2luLW5hdmlnYXRpb25fX2xvZ2luLWRyb3Bkb3duJyApLnNsaWRlVG9nZ2xlKCAnZmFzdCcgKTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIC8vIHVzZXIgZHJvcGRvd25cbiAgICAgICAgICAgICQoICdbZGF0YS10b2dnbGU9XCJ1c2VyLWRyb3Bkb3duXCJdJyApLm9uKCAnY2xpY2snLCBmdW5jdGlvbiggZXZlbnQgKSB7XG4gICAgICAgICAgICAgICAgZXZlbnQuc3RvcFByb3BhZ2F0aW9uKCk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgLy8gaGlkZSBib29rc2hlbGZkcm9wZG93IGlmIGV4aXN0XG4gICAgICAgICAgICAgICAgaWYgKCBfYm9va3NlbGZEcm9wZG93biApIHtcbiAgICAgICAgICAgICAgICAgICAgJCggJy5ib29rc2hlbGYtbmF2aWdhdGlvbl9fZHJvcGRvd24nICkuaGlkZSgpO1xuICAgICAgICAgICAgICAgICAgICAkKCAnLmJvb2tzaGVsZi1wb3B1cCcgKS5yZW1vdmUoKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgLy8gaGlkZSBjb2xsZWN0aW9uIHBhbmVsIGlmIGV4aXN0XG4gICAgICAgICAgICAgICAgaWYgKCAkKCAnLm5hdmlnYXRpb25fX2NvbGxlY3Rpb24tcGFuZWwnICkubGVuZ3RoID4gMCApIHtcbiAgICAgICAgICAgICAgICAgICAgJCggJy5uYXZpZ2F0aW9uX19jb2xsZWN0aW9uLXBhbmVsJyApLmhpZGUoKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgJCggJy5sb2dpbi1uYXZpZ2F0aW9uX191c2VyLWRyb3Bkb3duJyApLnNsaWRlVG9nZ2xlKCAnZmFzdCcgKTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gcmV0cmlldmUgYWNjb3VudFxuICAgICAgICAgICAgJCggJ1tkYXRhLXRvZ2dsZT1cInJldHJpZXZlLWFjY291bnRcIl0nICkub24oICdjbGljaycsIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgICQoICcubG9naW4tbmF2aWdhdGlvbl9fcmV0cmlldmUtYWNjb3VudCcgKS5hZGRDbGFzcyggJ2luJyApO1xuICAgICAgICAgICAgfSApO1xuICAgICAgICAgICAgJCggJ1tkYXRhLWRpc21pc3M9XCJyZXRyaWV2ZS1hY2NvdW50XCJdJyApLm9uKCAnY2xpY2snLCBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICAkKCAnLmxvZ2luLW5hdmlnYXRpb25fX3JldHJpZXZlLWFjY291bnQnICkucmVtb3ZlQ2xhc3MoICdpbicgKTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgLy8gcmVtb3ZlIGRyb3Bkb3duIGJ5IGNsaWNraW5nIG9uIGJvZHlcbiAgICAgICAgICAgICQoICdib2R5JyApLm9uKCAnY2xpY2snLCBmdW5jdGlvbiggZXZlbnQgKSB7XG4gICAgICAgICAgICAgICAgdmFyIHRhcmdldCA9ICQoIGV2ZW50LnRhcmdldCApO1xuICAgICAgICAgICAgICAgIHZhciBkcm9wZG93biA9ICQoICcubG9naW4tbmF2aWdhdGlvbl9fdXNlci1kcm9wZG93biwgLmxvZ2luLW5hdmlnYXRpb25fX2xvZ2luLWRyb3Bkb3duJyApO1xuICAgICAgICAgICAgICAgIHZhciBkcm9wZG93bkNoaWxkID0gZHJvcGRvd24uZmluZCggJyonICk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgaWYgKCAhdGFyZ2V0LmlzKCBkcm9wZG93biApICYmICF0YXJnZXQuaXMoIGRyb3Bkb3duQ2hpbGQgKSApIHtcbiAgICAgICAgICAgICAgICAgICAgZHJvcGRvd24uaGlkZSgpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgfVxuICAgIH07XG4gICAgXG4gICAgcmV0dXJuIHZpZXdlcjtcbiAgICBcbn0gKSggdmlld2VySlMgfHwge30sIGpRdWVyeSApO1xuIiwiLyoqXG4gKiBUaGlzIGZpbGUgaXMgcGFydCBvZiB0aGUgR29vYmkgdmlld2VyIC0gYSBjb250ZW50IHByZXNlbnRhdGlvbiBhbmQgbWFuYWdlbWVudFxuICogYXBwbGljYXRpb24gZm9yIGRpZ2l0aXplZCBvYmplY3RzLlxuICogXG4gKiBWaXNpdCB0aGVzZSB3ZWJzaXRlcyBmb3IgbW9yZSBpbmZvcm1hdGlvbi4gLSBodHRwOi8vd3d3LmludHJhbmRhLmNvbSAtXG4gKiBodHRwOi8vZGlnaXZlcnNvLmNvbVxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZnJlZSBzb2Z0d2FyZTsgeW91IGNhbiByZWRpc3RyaWJ1dGUgaXQgYW5kL29yIG1vZGlmeSBpdCB1bmRlciB0aGUgdGVybXNcbiAqIG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhcyBwdWJsaXNoZWQgYnkgdGhlIEZyZWUgU29mdHdhcmUgRm91bmRhdGlvbjsgZWl0aGVyXG4gKiB2ZXJzaW9uIDIgb2YgdGhlIExpY2Vuc2UsIG9yIChhdCB5b3VyIG9wdGlvbikgYW55IGxhdGVyIHZlcnNpb24uXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBkaXN0cmlidXRlZCBpbiB0aGUgaG9wZSB0aGF0IGl0IHdpbGwgYmUgdXNlZnVsLCBidXQgV0lUSE9VVCBBTllcbiAqIFdBUlJBTlRZOyB3aXRob3V0IGV2ZW4gdGhlIGltcGxpZWQgd2FycmFudHkgb2YgTUVSQ0hBTlRBQklMSVRZIG9yIEZJVE5FU1MgRk9SIEFcbiAqIFBBUlRJQ1VMQVIgUFVSUE9TRS4gU2VlIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBmb3IgbW9yZSBkZXRhaWxzLlxuICogXG4gKiBZb3Ugc2hvdWxkIGhhdmUgcmVjZWl2ZWQgYSBjb3B5IG9mIHRoZSBHTlUgR2VuZXJhbCBQdWJsaWMgTGljZW5zZSBhbG9uZyB3aXRoIHRoaXNcbiAqIHByb2dyYW0uIElmIG5vdCwgc2VlIDxodHRwOi8vd3d3LmdudS5vcmcvbGljZW5zZXMvPi5cbiAqIFxuICogTW9kdWxlIHdoaWNoIHJlbmRlcnMgY3VycmVudCBhbmQgb3RoZXIgdmVyc2lvbnMgb2YgYSB3b3JrIGludG8gYSB3aWRnZXQuXG4gKiBcbiAqIEB2ZXJzaW9uIDMuMi4wXG4gKiBAbW9kdWxlIHZpZXdlckpTLnZlcnNpb25IaXN0b3J5XG4gKiBAcmVxdWlyZXMgalF1ZXJ5XG4gKi9cbnZhciB2aWV3ZXJKUyA9ICggZnVuY3Rpb24oIHZpZXdlciApIHtcbiAgICAndXNlIHN0cmljdCc7XG4gICAgXG4gICAgdmFyIF9kZWJ1ZyA9IGZhbHNlO1xuICAgIHZhciBfZGVmYXVsdHMgPSB7XG4gICAgICAgIHZlcnNpb25zOiBbXSxcbiAgICAgICAganNvbjogbnVsbCxcbiAgICAgICAgaW1nVXJsOiAnJyxcbiAgICAgICAgaW1nUGk6ICcnLFxuICAgICAgICB2ZXJzaW9uTGluazogJycsXG4gICAgICAgIHdpZGdldElucHV0czogJycsXG4gICAgICAgIHdpZGdldExpc3Q6ICcnLFxuICAgIH07XG4gICAgXG4gICAgdmlld2VyLnZlcnNpb25IaXN0b3J5ID0ge1xuICAgICAgICAvKipcbiAgICAgICAgICogTWV0aG9kIHRvIGluaXRpYWxpemUgdGhlIHZlcnNpb24gaGlzdG9yeSB3aWRnZXQuXG4gICAgICAgICAqIFxuICAgICAgICAgKiBAbWV0aG9kIGluaXRcbiAgICAgICAgICogQHBhcmFtIHtPYmplY3R9IGNvbmZpZyBBbiBjb25maWcgb2JqZWN0IHdoaWNoIG92ZXJ3cml0ZXMgdGhlIGRlZmF1bHRzLlxuICAgICAgICAgKiBAcGFyYW0ge0FycmF5fSBjb25maWcudmVyc2lvbnMgQW4gYXJyYXkgd2hpY2ggaG9sZHMgYWxsIHZlcnNpb25zLlxuICAgICAgICAgKiBAcGFyYW0ge09iamVjdH0gY29uZmlnLmpzb24gQW4gSlNPTi1PYmplY3Qgd2hpY2ggdGFrZXMgYWxsIHZlcnNpb25zLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLmltZ1VybCBUaGUgaW1hZ2UgVVJMIGZvciB0aGUgY3VycmVudCB3b3JrLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLmltZ1BpIFRoZSBQSSBmb3IgdGhlIGltYWdlIG9mIHRoZSBjdXJyZW50IHdvcmsuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcudmVyc2lvbkxpbmsgQSBzdHJpbmcgcGxhY2Vob2xkZXIgZm9yIHRoZSBmaW5hbCBIVE1MLlxuICAgICAgICAgKiBAcGFyYW0ge1N0cmluZ30gY29uZmlnLndpZGdldElucHV0cyBBIHN0cmluZyBwbGFjZWhvbGRlciBmb3IgdGhlIGZpbmFsIEhUTUwuXG4gICAgICAgICAqIEBwYXJhbSB7U3RyaW5nfSBjb25maWcud2lkZ2V0TGlzdCBBIHN0cmluZyBwbGFjZWhvbGRlciBmb3IgdGhlIGZpbmFsIEhUTUwuXG4gICAgICAgICAqL1xuICAgICAgICBpbml0OiBmdW5jdGlvbiggY29uZmlnICkge1xuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICcjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMnICk7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIudmVyc2lvbkhpc3RvcnkuaW5pdCcgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycgKTtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZyggJ3ZpZXdlci52ZXJzaW9uSGlzdG9yeS5pbml0OiBjb25maWcgPSAnLCBjb25maWcgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgJC5leHRlbmQoIHRydWUsIF9kZWZhdWx0cywgY29uZmlnICk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIHB1c2ggdmVyc2lvbnMgaW50byBhbiBhcnJheVxuICAgICAgICAgICAgJCggX2RlZmF1bHRzLndpZGdldElucHV0cyApLmVhY2goIGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgICAgIF9kZWZhdWx0cy52ZXJzaW9ucy5wdXNoKCAkKCB0aGlzICkudmFsKCkgKTtcbiAgICAgICAgICAgIH0gKTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgaWYgKCBfZGVidWcgKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coICd2aWV3ZXIudmVyc2lvbkhpc3Rvcnk6IHZlcnNpb25zID0gJywgX2RlZmF1bHRzLnZlcnNpb25zICk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIC8vIGFwcGVuZCBsaXN0IGVsZW1lbnRzIHRvIHdpZGdldFxuICAgICAgICAgICAgZm9yICggdmFyIGkgPSAwOyBpIDwgX2RlZmF1bHRzLnZlcnNpb25zLmxlbmd0aDsgaSsrICkge1xuICAgICAgICAgICAgICAgIF9kZWZhdWx0cy5qc29uID0gSlNPTi5wYXJzZSggX2RlZmF1bHRzLnZlcnNpb25zWyBpIF0gKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICBpZiAoIF9kZWZhdWx0cy5qc29uLmlkID09PSBfZGVmYXVsdHMuaW1nUGkgKSB7XG4gICAgICAgICAgICAgICAgICAgIC8vIEFrdHVlbGwgZ2XDtmZmbmV0ZSBWZXJzaW9uIC0ga2VpbiBMaW5rXG4gICAgICAgICAgICAgICAgICAgIF9kZWZhdWx0cy52ZXJzaW9uTGluayA9ICc8bGk+PHNwYW4+JztcbiAgICAgICAgICAgICAgICAgICAgaWYgKCBfZGVmYXVsdHMuanNvbi5sYWJlbCAhPSB1bmRlZmluZWQgJiYgX2RlZmF1bHRzLmpzb24ubGFiZWwgIT0gJycgKSB7XG4gICAgICAgICAgICAgICAgICAgIFx0X2RlZmF1bHRzLnZlcnNpb25MaW5rICs9IF9kZWZhdWx0cy5qc29uLmxhYmVsO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICBcdCBfZGVmYXVsdHMudmVyc2lvbkxpbmsgKz0gX2RlZmF1bHRzLmpzb24uaWQ7XG4gICAgICAgICAgICAgICAgICAgIFx0IGlmICggX2RlZmF1bHRzLmpzb24ueWVhciAhPSB1bmRlZmluZWQgJiYgX2RlZmF1bHRzLmpzb24ueWVhciAhPSAnJyApIHtcbiAgICAgICAgICAgICAgICAgICAgXHRcdCBfZGVmYXVsdHMudmVyc2lvbkxpbmsgKz0gJyAoJyArIF9kZWZhdWx0cy5qc29uLnllYXIgKyAnKSc7ICAgICAgICAgICAgICAgICAgICBcdFxuICAgICAgICAgICAgICAgICAgICBcdCB9XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgX2RlZmF1bHRzLnZlcnNpb25MaW5rICs9ICc8L3NwYW4+PC9saT4nO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgJCggX2RlZmF1bHRzLndpZGdldExpc3QgKS5hcHBlbmQoIF9kZWZhdWx0cy52ZXJzaW9uTGluayApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgLy8gVm9yZ8OkbmdlciB1bmQgTmFjaGZvbGdlciBqZXdlaWxzIG1pdCBMaW5rXG4gICAgICAgICAgICAgICAgICAgIF9kZWZhdWx0cy52ZXJzaW9uTGluayA9ICc8bGk+PGEgaHJlZj1cIicgKyBfZGVmYXVsdHMuaW1nVXJsICsgJy8nICsgX2RlZmF1bHRzLmpzb24uaWQgKyAnLzEvXCI+JztcbiAgICAgICAgICAgICAgICAgICAgaWYgKCBfZGVmYXVsdHMuanNvbi5sYWJlbCAhPSB1bmRlZmluZWQgJiYgX2RlZmF1bHRzLmpzb24ubGFiZWwgIT0gJycgKSB7XG4gICAgICAgICAgICAgICAgICAgIFx0X2RlZmF1bHRzLnZlcnNpb25MaW5rICs9IF9kZWZhdWx0cy5qc29uLmxhYmVsO1xuICAgICAgICAgICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICBcdF9kZWZhdWx0cy52ZXJzaW9uTGluayArPSBfZGVmYXVsdHMuanNvbi5pZDtcbiAgICAgICAgICAgICAgICAgICAgXHRpZiAoIF9kZWZhdWx0cy5qc29uLnllYXIgIT0gdW5kZWZpbmVkICYmIF9kZWZhdWx0cy5qc29uLnllYXIgIT0gJycgKSB7XG4gICAgICAgICAgICAgICAgICAgIFx0XHRfZGVmYXVsdHMudmVyc2lvbkxpbmsgKz0gJyAoJyArIF9kZWZhdWx0cy5qc29uLnllYXIgKyAnKSc7XG4gICAgICAgICAgICAgICAgICAgIFx0fVxuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIF9kZWZhdWx0cy52ZXJzaW9uTGluayArPSAnPC9hPjwvbGk+JztcbiAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICQoIF9kZWZhdWx0cy53aWRnZXRMaXN0ICkuYXBwZW5kKCBfZGVmYXVsdHMudmVyc2lvbkxpbmsgKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICB9O1xuICAgIFxuICAgIHJldHVybiB2aWV3ZXI7XG4gICAgXG59ICkoIHZpZXdlckpTIHx8IHt9LCBqUXVlcnkgKTtcbiJdfQ==
