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
 * Interaction logic for the accessible language switcher rendered by the
 * changeLocalMenu composite component (components/changeLocalMenu.xhtml).
 * Implements the WAI-ARIA APG "menu button - actions" pattern: menu items
 * change the locale via a same-page form action rather than navigating to a
 * new URL, so they stay buttons (role="menuitem") instead of links. All ARIA
 * attributes/ids/labels are already rendered server-side; this module only
 * adds the open/close toggle and keyboard navigation, which cannot be done
 * without JavaScript.
 *
 * @version 1.0.0
 * @module viewerJS.changeLocalMenu
 * @requires jQuery
 */
var viewerJS = (function (viewer) {
    'use strict';

    var _debug = false;

    viewer.changeLocalMenu = {
        init: function () {
            if (_debug) {
                console.log('Initializing: viewerJS.changeLocalMenu.init');
            }

            $('.change-local-menu__button').each(function () {
                var $button = $(this);
                var $list = $('#' + $button.attr('aria-controls'));

                if (!$list.length) {
                    return;
                }

                var $items = $list.find('[role="menuitem"]');

                function isOpen() {
                    return $button.attr('aria-expanded') === 'true';
                }

                function openMenu(focusFirstItem) {
                    $button.attr('aria-expanded', 'true');
                    $list.prop('hidden', false);
                    if (focusFirstItem) {
                        $items.first().trigger('focus');
                    }
                }

                function closeMenu(returnFocusToButton) {
                    $button.attr('aria-expanded', 'false');
                    $list.prop('hidden', true);
                    if (returnFocusToButton) {
                        $button.trigger('focus');
                    }
                }

                function focusItemAt(index) {
                    if (!$items.length) {
                        return;
                    }
                    var targetIndex = (index + $items.length) % $items.length;
                    $items.eq(targetIndex).trigger('focus');
                }

                $button.on('click', function (e) {
                    e.preventDefault();
                    if (isOpen()) {
                        closeMenu(false);
                    } else {
                        openMenu(true);
                    }
                });

                $button.on('keydown', function (e) {
                    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
                        e.preventDefault();
                        openMenu(false);
                        focusItemAt(e.key === 'ArrowDown' ? 0 : $items.length - 1);
                    }
                });

                $items.on('keydown', function (e) {
                    var currentIndex = $items.index(this);

                    if (e.key === 'ArrowDown') {
                        e.preventDefault();
                        focusItemAt(currentIndex + 1);
                    } else if (e.key === 'ArrowUp') {
                        e.preventDefault();
                        focusItemAt(currentIndex - 1);
                    } else if (e.key === 'Home') {
                        e.preventDefault();
                        focusItemAt(0);
                    } else if (e.key === 'End') {
                        e.preventDefault();
                        focusItemAt($items.length - 1);
                    } else if (e.key === 'Escape') {
                        e.preventDefault();
                        closeMenu(true);
                    } else if (e.key === 'Tab') {
                        closeMenu(false);
                    }
                });

                $(document).on('click', function (e) {
                    if (
                        isOpen() &&
                        !$list.is(e.target) &&
                        $list.has(e.target).length === 0 &&
                        !$button.is(e.target) &&
                        $button.has(e.target).length === 0
                    ) {
                        closeMenu(false);
                    }
                });
            });
        },
    };

    return viewer;
})(viewerJS || {}, jQuery);

// CommonJS export for Jest. No-op in the browser where `module` is undefined.
if (typeof module !== 'undefined' && module.exports) {
    module.exports = viewerJS;
}
