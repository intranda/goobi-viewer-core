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
var viewerJS = (function(viewer) {
	'use strict';

	var _debug = false;
	var _confirmCounter = 0;
	var _defaults = {
		root : '',
		msg : {
			resetBookshelves : '',
			resetBookshelvesConfirm : '',
			saveItemToSession : ''
		}
	};

	viewer.bookshelvesSession = {
		init : function(config) {
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
			$('[data-bookshelf-type="dropdown"]').off().on('click', function(event) {
				event.stopPropagation();

				// hide other dropdowns
				$('.login-navigation__login-dropdown, .login-navigation__user-dropdown, .navigation__collection-panel').hide();

				_getAllSessionElements(_defaults.root).then(function(elements) {
					if (elements.items.length > 0) {
						$('.bookshelf-navigation__dropdown').slideToggle('fast');
					} else {
						return false;
					}
				}).fail(function(error) {
					console.error('ERROR - _getAllSessionElements: ', error.responseText);
				});

			});

			// set element count of list to counter
			_setSessionElementCount();

			// check add buttons if element is in list
			_setAddActiveState();

			// add element to session
			$('[data-bookshelf-type="add"]').off().on('click', function() {
				var currBtn = $(this);
				var currPi = currBtn.attr('data-pi');

				_isElementSet(_defaults.root, currPi).then(function(isSet) {
					// set confirm counter
					_confirmCounter = parseInt(localStorage.getItem('confirmCounter'));

					if (!isSet) {
						if (_confirmCounter == 0) {
							if (confirm(_defaults.msg.saveItemToSession)) {
								currBtn.addClass('added');
								localStorage.setItem('confirmCounter', 1);
								_setSessionElement(_defaults.root, currPi).then(function() {
									_setSessionElementCount();
									_renderDropdownList();
								});
							} else {
								return false;
							}
						} else {
							currBtn.addClass('added');
							_setSessionElement(_defaults.root, currPi).then(function() {
								_setSessionElementCount();
								_renderDropdownList();
							});
						}
					} else {
						currBtn.removeClass('added');
						_deleteSessionElement(_defaults.root, currPi).then(function() {
							_setSessionElementCount();
							_renderDropdownList();
						});
					}
				}).fail(function(error) {
					console.error('ERROR - _isElementSet: ', error.responseText);
				});
			});

			// hide menus by clicking on body
			$('body').on('click', function(event) {
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
			url : root + '/rest/bookshelves/session/get/',
			type : "GET",
			dataType : "JSON",
			async : true
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
			url : root + '/rest/bookshelves/session/contains/' + pi + '/',
			type : "GET",
			dataType : "JSON",
			async : true
		}));

		return promise
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
			url : root + '/rest/bookshelves/session/add/' + pi + '/',
			type : "GET",
			dataType : "JSON",
			async : true
		}));

		return promise
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
			url : root + '/rest/bookshelves/session/delete/' + pi + '/',
			type : "GET",
			dataType : "JSON",
			async : true
		}));

		return promise
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
			url : root + '/rest/bookshelves/session/delete/',
			type : "GET",
			dataType : "JSON",
			async : true
		}));

		return promise
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

		_getAllSessionElements(_defaults.root).then(function(elements) {
			$('[data-bookshelf-type="counter"]').empty().text(elements.items.length);
		}).fail(function(error) {
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

		_getAllSessionElements(_defaults.root).then(function(elements) {
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

			elements.items
				.forEach(function(item) {
					dropdownListItem = $('<li />');
					dropdownListItemRow = $('<div />').addClass('row no-margin');
					dropdownListItemColLeft = $('<div />').addClass('col-xs-4 no-padding');
					dropdownListItemColCenter = $('<div />').addClass('col-xs-7 no-padding');
					dropdownListItemColRight = $('<div />').addClass('col-xs-1 no-padding bookshelf-navigation__dropdown-list-remove');
					dropdownListItemImage = $('<div />').addClass('bookshelf-navigation__dropdown-list-image').css('background-image', 'url('
						+ item.representativeImageUrl + ')');
					dropdownListItemName = $('<h4 />');
					dropdownListItemNameLink = $('<a />').attr('href', _defaults.root + item.url).text(item.name);
					dropdownListItemDelete = $('<button />').addClass('btn-clean').attr('type', 'button').attr('data-bookshelf-type', 'delete')
						.attr('data-pi', item.pi);

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
			$('[data-bookshelf-type="delete"]').on('click', function() {
				var currBtn = $(this);
				var currPi = currBtn.attr('data-pi');

				_deleteSessionElement(_defaults.root, currPi).then(function() {
					_setSessionElementCount();
					_setAddActiveState();
					_renderDropdownList();
				});
			});

			// delete all items
			$('[data-bookshelf-type="reset"]').on('click', function() {
				if (confirm(_defaults.msg.resetBookshelvesConfirm)) {
					_deleteAllSessionElements(_defaults.root).then(function() {
						localStorage.setItem('confirmCounter', 0);
						_setSessionElementCount();
						_setAddActiveState();
						_renderDropdownList();
					});
				} else {
					return false;
				}
			});

		}).fail(function(error) {
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

		$('[data-bookshelf-type="add"]').each(function() {
			var currBtn = $(this);
			var currPi = currBtn.attr('data-pi');

			_isElementSet(_defaults.root, currPi).then(function(isSet) {
				if (isSet) {
					currBtn.addClass('added');
				} else {
					currBtn.removeClass('added');
				}
			}).fail(function(error) {
				console.error('ERROR - _isElementSet: ', error.responseText);
			});
		});
	}

	return viewer;

})(viewerJS || {}, jQuery);

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