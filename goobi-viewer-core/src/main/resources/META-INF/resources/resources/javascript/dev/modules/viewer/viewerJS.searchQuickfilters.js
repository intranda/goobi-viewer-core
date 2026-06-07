/**
 * Quick Filters module for the Goobi Viewer search bar.
 *
 * Provides an expandable filter panel below the search input with configurable
 * filter types: facet dropdowns (loaded from Solr), date range inputs, and
 * checkbox groups. All UI interaction is client-side; filter values are submitted
 * to the server via JSF form bindings on search.
 *
 * @module viewerJS.searchQuickfilters
 */
var viewerJS = (function (viewer) {
    'use strict';

    var _apiBase = null;
    var _initialized = false;
    var _clickOutsideHandler = null;
    var _escapeHandler = null;
    var _loadingFields = {};
    var _searchDebounceTimers = {};
    var _module = {};
    var _defaults = {
        panelSelector: '[data-quickfilter-panel]',
        toggleSelector: '[data-quickfilter-toggle]',
        dropdownSelector: '[data-quickfilter-dropdown]',
        fieldSelector: '[data-quickfilter-field]',
        hiddenSelector: '[data-quickfilter-hidden]',
        dateSelector: '[data-quickfilter-date]',
        actionsSelector: '[data-quickfilter-actions]',
        resetSelector: '[data-quickfilter-reset]',
        statusBarSelector: '[data-quickfilter-status]',
        statusTextSelector: '[data-quickfilter-status-text]',
        statusResetSelector: '[data-quickfilter-status-reset]',
        statusTooltipSelector: '[data-quickfilter-status-tooltip]',
        checkboxSelector: '[data-quickfilter-checkbox]',
        clearSelector: '[data-quickfilter-clear]',
        defaultTextAttr: 'data-quickfilter-default-text',
        dropdownPanelPrefix: '[data-quickfilter-dropdown-panel="',
        dropdownSearchPrefix: '[data-quickfilter-dropdown-search="',
        dropdownListPrefix: '[data-quickfilter-dropdown-list="',
        dropdownClosePrefix: '[data-quickfilter-dropdown-close="',
        headerTextPrefix: '[data-quickfilter-dropdown-header-text="',
        searchDebounceMs: 150,
    };
    var _config = _defaults;

    /**
     * Initializes all quick filter components. Safe to call multiple times —
     * cleans up previous listeners before re-binding to prevent duplicates
     * (e.g. after JSF AJAX updates).
     */
    _module.init = function () {
        _destroy();
        _initApiUrls();
        _initToggleButton();
        _initPanelClickStop();
        _initDropdownTriggers();
        _initCloseButtons();
        _initSearchInputs();
        _initClickOutside();
        _initDateInputs();
        _initResetLink();
        _initStatusBarReset();
        _initStatusTooltips();
        _initFieldClearButtons();
        _syncFieldClearButtons();
        _preloadDropdowns();
        _updateStatusBar();
        _initialized = true;
    };

    /**
     * Removes the document-level click-outside listener to prevent
     * stacking on repeated init() calls. Element-level listeners are
     * cleaned up by DOM replacement (JSF re-renders the component tree).
     */
    function _destroy() {
        if (_clickOutsideHandler) {
            document.removeEventListener('click', _clickOutsideHandler);
            _clickOutsideHandler = null;
        }
        if (_escapeHandler) {
            document.removeEventListener('keydown', _escapeHandler);
            _escapeHandler = null;
        }
        Object.keys(_searchDebounceTimers).forEach(function (key) {
            clearTimeout(_searchDebounceTimers[key]);
        });
        _searchDebounceTimers = {};
        _loadingFields = {};
        _initialized = false;
    }

    /**
     * Resolves the REST API base URL and mock data URL from the global
     * viewerJS helper. Called once per init() cycle.
     */
    function _initApiUrls() {
        var restApi = viewerJS.getRestApiUrl();
        _apiBase = restApi + 'quickfilters/facets';
    }

    /**
     * Loads facet values for all dropdowns in the background on init,
     * so data is ready when the user opens a dropdown.
     */
    function _preloadDropdowns() {
        document.querySelectorAll(_config.dropdownSelector).forEach(function (trigger) {
            var field = trigger.getAttribute('data-quickfilter-dropdown');
            var panel = document.querySelector(_config.dropdownPanelPrefix + field + '"]');
            if (!panel) return;
            var list = panel.querySelector(_config.dropdownListPrefix + field + '"]');
            if (list && list.children.length === 0) {
                _loadFacetValues(field, list);
            }
        });
    }

    /**
     * Binds the filter toggle button to open/close the quick filters panel.
     * Switches between filter and close icons, updates aria-expanded,
     * and toggles the -filters-open modifier on the search input container.
     */
    function _initToggleButton() {
        var toggle = document.querySelector(_config.toggleSelector);
        if (!toggle) return;

        toggle.addEventListener('click', function (e) {
            e.stopPropagation();
            var panel = document.querySelector(_config.panelSelector);
            if (!panel) return;

            var isOpen = panel.classList.toggle('-open');
            var openIcon = toggle.querySelector('.search-standard__filter-toggle-icon.-open');
            var closeIcon = toggle.querySelector('.search-standard__filter-toggle-icon.-close');

            if (openIcon) openIcon.style.display = isOpen ? 'none' : '';
            if (closeIcon) closeIcon.style.display = isOpen ? '' : 'none';

            toggle.setAttribute('aria-expanded', isOpen ? 'true' : 'false');

            var inputSlim = document.querySelector('.search-standard__input-slim.-with-filters');
            if (inputSlim) {
                inputSlim.classList.toggle('-filters-open', isOpen);
            }

            _updateStatusBar();
        });
    }

    /**
     * Prevents clicks inside the filter panel from bubbling up to the
     * document click handler (which would close all dropdowns).
     */
    function _initPanelClickStop() {
        var panel = document.querySelector(_config.panelSelector);
        if (panel) {
            panel.addEventListener('click', function (e) {
                e.stopPropagation();
            });
        }
    }

    /**
     * Binds click handlers on each dropdown trigger. On click: closes any
     * other open dropdown, opens the clicked one, focuses the search input
     * inside, updates the header text, and loads facet data if not yet loaded.
     */
    function _initDropdownTriggers() {
        var triggers = document.querySelectorAll(_config.dropdownSelector);

        triggers.forEach(function (trigger) {
            trigger.addEventListener('click', function (e) {
                e.stopPropagation();
                _openDropdownForTrigger(trigger);
            });
        });
    }

    function _openDropdownForTrigger(trigger) {
        var field = trigger.getAttribute('data-quickfilter-dropdown');
        var panel = document.querySelector(_config.dropdownPanelPrefix + field + '"]');
        if (!panel) return;

        var isOpen = panel.classList.contains('-open');
        _closeAllDropdowns();

        if (!isOpen) {
            panel.classList.add('-open');
            trigger.setAttribute('aria-expanded', 'true');
            var fieldEl = trigger.closest(_config.fieldSelector);
            if (fieldEl) fieldEl.classList.add('-dropdown-open');

            var searchInput = panel.querySelector(_config.dropdownSearchPrefix + field + '"]');
            if (searchInput) {
                searchInput.value = '';
                setTimeout(function () {
                    searchInput.focus();
                }, 50);
            }

            _updateDropdownHeaderText(field);

            var list = panel.querySelector(_config.dropdownListPrefix + field + '"]');
            if (list && list.children.length === 0 && !_loadingFields[field]) {
                _loadFacetValues(field, list);
            }
        }
    }

    /**
     * Binds click handlers on dropdown close buttons to close all open dropdowns.
     */
    function _initCloseButtons() {
        var buttons = document.querySelectorAll('[data-quickfilter-dropdown-close]');

        buttons.forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.stopPropagation();
                _closeAllDropdowns();
            });
        });
    }

    /**
     * Binds input handlers on search fields inside dropdowns. Filters the
     * dropdown item list client-side by matching the query against data-value
     * attributes. Hides groups that have no visible items. Debounced to avoid
     * lag with large lists.
     */
    function _initSearchInputs() {
        var inputs = document.querySelectorAll('[data-quickfilter-dropdown-search]');

        inputs.forEach(function (input) {
            input.addEventListener('input', function () {
                var field = input.getAttribute('data-quickfilter-dropdown-search');

                if (_searchDebounceTimers[field]) {
                    clearTimeout(_searchDebounceTimers[field]);
                }

                _searchDebounceTimers[field] = setTimeout(function () {
                    _filterDropdownItems(input, field);
                    _searchDebounceTimers[field] = null;
                }, _config.searchDebounceMs);
            });

            input.addEventListener('click', function (e) {
                e.stopPropagation();
            });
        });
    }

    /**
     * Filters dropdown items by query string, hiding non-matching items
     * and empty groups. Uses a hidden data attribute instead of inline
     * style checks for more robust visibility tracking.
     *
     * @param {HTMLElement} input - The search input element
     * @param {string} field - The Solr field name
     */
    function _filterDropdownItems(input, field) {
        var query = input.value.toLowerCase();
        var panel = document.querySelector(_config.dropdownPanelPrefix + field + '"]');
        if (!panel) return;

        var items = panel.querySelectorAll('.search-quick-filters__dropdown-item');
        items.forEach(function (item) {
            if (!item.hasAttribute('data-text-original')) {
                item.setAttribute('data-text-original', item.textContent);
            }
            var originalText = item.getAttribute('data-text-original');
            var value = (item.getAttribute('data-value') || '').toLowerCase();
            var matches = query === '' || value.indexOf(query) !== -1;

            if (matches && query !== '') {
                item.innerHTML = _highlightMatch(originalText, input.value);
            } else {
                item.textContent = originalText;
            }

            item.style.display = matches ? '' : 'none';
            item.setAttribute('data-hidden', matches ? 'false' : 'true');
        });

        var groups = panel.querySelectorAll('.search-quick-filters__dropdown-group');
        groups.forEach(function (group) {
            var visibleItems = group.querySelectorAll('.search-quick-filters__dropdown-item[data-hidden="false"]');
            group.style.display = visibleItems.length > 0 ? '' : 'none';
        });
    }

    /**
     * Closes all open dropdowns when clicking anywhere outside the filter panel.
     * Stores the handler reference so it can be removed on destroy().
     */
    function _initClickOutside() {
        _clickOutsideHandler = function () {
            _closeAllDropdowns();
        };
        document.addEventListener('click', _clickOutsideHandler);

        _escapeHandler = function (e) {
            if (e.key !== 'Escape' && e.key !== 'Esc') return;
            var openPanel = document.querySelector('[data-quickfilter-dropdown-panel].-open');
            if (!openPanel) return;
            var field = openPanel.getAttribute('data-quickfilter-dropdown-panel');
            _closeAllDropdowns();
            var trigger = document.querySelector(_config.dropdownSelector.replace(']', '="' + field + '"]'));
            if (trigger) trigger.focus();
        };
        document.addEventListener('keydown', _escapeHandler);
    }

    /**
     * Removes -open and -dropdown-open states from all dropdowns
     * and resets aria-expanded to false on all triggers.
     */
    function _closeAllDropdowns() {
        document.querySelectorAll('[data-quickfilter-dropdown-panel].-open').forEach(function (panel) {
            panel.classList.remove('-open');
        });
        document.querySelectorAll(_config.dropdownSelector + '[aria-expanded="true"]').forEach(function (trigger) {
            trigger.setAttribute('aria-expanded', 'false');
        });
        document.querySelectorAll(_config.fieldSelector + '.-dropdown-open').forEach(function (field) {
            field.classList.remove('-dropdown-open');
        });
    }

    /**
     * Fetches facet values from the REST API for a given Solr field.
     * On success, renders the items into the dropdown list.
     * Tracks loading state per field to prevent duplicate requests.
     *
     * @param {string} field - The Solr field name (e.g. 'MD_CREATOR')
     * @param {HTMLElement} listEl - The dropdown list container element
     */
    function _loadFacetValues(field, listEl) {
        _loadingFields[field] = true;

        var lang = document.documentElement.lang || 'en';
        fetch(_apiBase + '?field=' + encodeURIComponent(field) + '&lang=' + encodeURIComponent(lang))
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('API not available');
                }
                return response.json();
            })
            .then(function (data) {
                _renderDropdownItems(data, listEl, field);
                _restoreDropdownSelections(field);
            })
            .catch(function (err) {
                console.error('Failed to load quick filter facets for ' + field + ': ' + err.message);
                listEl.innerHTML = '';
            })
            .finally(function () {
                _loadingFields[field] = false;
            });
    }

    /**
     * Renders facet data into a dropdown list as alphabetically grouped items.
     * Each group has a letter header and clickable items showing value + count.
     *
     * @param {Object} data - Grouped facet data, e.g. { "A": [{ value: "Art", count: 3 }] }
     * @param {HTMLElement} listEl - The dropdown list container element
     * @param {string} field - The Solr field name (used for click handler binding)
     */
    function _renderDropdownItems(data, listEl, field) {
        listEl.innerHTML = '';

        var letters = Object.keys(data).sort();

        letters.forEach(function (letter) {
            var group = document.createElement('div');
            group.className = 'search-quick-filters__dropdown-group';

            var header = document.createElement('div');
            header.className = 'search-quick-filters__dropdown-group-header';

            var headerText = document.createElement('span');
            headerText.textContent = letter;
            header.appendChild(headerText);

            var chevronWrapper = document.createElement('span');
            chevronWrapper.className = 'icon-wrapper';
            chevronWrapper.setAttribute('aria-hidden', 'true');
            chevronWrapper.innerHTML =
                '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg>';
            header.appendChild(chevronWrapper);

            group.appendChild(header);

            var itemsContainer = document.createElement('div');
            itemsContainer.className = 'search-quick-filters__dropdown-group-items';

            data[letter].forEach(function (entry) {
                var displayLabel = entry.label || entry.value;
                var item = document.createElement('div');
                item.className = 'search-quick-filters__dropdown-item';
                item.setAttribute('role', 'option');
                item.setAttribute('tabindex', '0');
                item.setAttribute('data-value', displayLabel.toLowerCase());
                item.setAttribute('data-raw-value', entry.value);
                item.setAttribute('data-hidden', 'false');
                item.textContent = displayLabel + ' (' + entry.count + ')';
                item.addEventListener('click', function (e) {
                    e.stopPropagation();
                    _selectValue(field, entry.value, displayLabel);
                });
                item.addEventListener('keydown', function (e) {
                    if (e.key === 'Enter' || e.key === ' ' || e.key === 'Spacebar') {
                        e.preventDefault();
                        e.stopPropagation();
                        _selectValue(field, entry.value, displayLabel);
                    }
                });
                itemsContainer.appendChild(item);
            });

            group.appendChild(itemsContainer);
            listEl.appendChild(group);
        });
    }

    /**
     * Applies a selected value from a dropdown: updates the hidden input
     * (for JSF form submission), updates the trigger display text,
     * closes all dropdowns, and shows the actions bar.
     *
     * @param {string} field - The Solr field name
     * @param {string} value - The raw Solr facet value (stored in hidden input)
     * @param {string} [displayLabel] - The translated display label (shown in trigger)
     */
    function _selectValue(field, value, displayLabel) {
        var hidden = document.querySelector(_config.hiddenSelector.replace(']', '="' + field + '"]'));
        if (hidden) {
            hidden.value = value;
        }

        var trigger = document.querySelector(_config.dropdownSelector.replace(']', '="' + field + '"]'));
        if (trigger) {
            var span = trigger.querySelector('[' + _config.defaultTextAttr + ']');
            if (span) {
                span.textContent = displayLabel || value;
            }
        }

        _toggleClearButton(field, !!value);

        _closeAllDropdowns();
        _updateActionsVisibility();

        if (trigger) trigger.focus();
    }

    function _initFieldClearButtons() {
        document.querySelectorAll(_config.clearSelector).forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.stopPropagation();
                e.preventDefault();
                var field = btn.getAttribute('data-quickfilter-clear');
                var trigger = document.querySelector(_config.dropdownSelector.replace(']', '="' + field + '"]'));
                var defaultText = '';
                if (trigger) {
                    var span = trigger.querySelector('[' + _config.defaultTextAttr + ']');
                    if (span) defaultText = span.getAttribute(_config.defaultTextAttr) || '';
                }
                _selectValue(field, '', defaultText);
            });
        });
    }

    function _syncFieldClearButtons() {
        document.querySelectorAll(_config.clearSelector).forEach(function (btn) {
            var field = btn.getAttribute('data-quickfilter-clear');
            var hidden = document.querySelector(_config.hiddenSelector.replace(']', '="' + field + '"]'));
            var hasValue = !!(hidden && hidden.value && hidden.value.trim() !== '');
            btn.style.display = hasValue ? '' : 'none';
        });
    }

    function _toggleClearButton(field, visible) {
        var btn = document.querySelector(_config.clearSelector.replace(']', '="' + field + '"]'));
        if (btn) btn.style.display = visible ? '' : 'none';
    }

    /**
     * Syncs the dropdown header text with the current trigger display text
     * when a dropdown is opened.
     *
     * @param {string} field - The Solr field name
     */
    function _updateDropdownHeaderText(field) {
        var trigger = document.querySelector(_config.dropdownSelector.replace(']', '="' + field + '"]'));
        var headerText = document.querySelector(_config.headerTextPrefix + field + '"]');
        if (trigger && headerText) {
            var span = trigger.querySelector('[' + _config.defaultTextAttr + ']');
            if (span) {
                headerText.textContent = span.textContent;
            }
        }
    }

    /**
     * Shows or hides the actions bar (reset link) based on whether any
     * filter has an active value (dropdown selection or date input).
     */
    function _updateActionsVisibility() {
        var actions = document.querySelector(_config.actionsSelector);
        if (!actions) return;

        var hasActiveFilter = _countActiveFilters() > 0;
        actions.classList.toggle('-visible', hasActiveFilter);
        _updateStatusBar();
    }

    /**
     * Counts the number of active quick filters (dropdowns with a value
     * and date inputs with a value).
     *
     * @returns {number} The number of active filters
     */
    function _countActiveFilters() {
        var count = 0;

        document.querySelectorAll(_config.hiddenSelector).forEach(function (h) {
            if (h.value && h.value.trim() !== '') {
                count++;
            }
        });

        document.querySelectorAll(_config.dateSelector).forEach(function (input) {
            if (input.value && input.value.trim() !== '') {
                count++;
            }
        });

        return count;
    }

    function _updateStatusBar() {
        var bars = document.querySelectorAll(_config.statusBarSelector);
        if (!bars.length) return;

        var count = _countActiveFilters();
        var panel = document.querySelector(_config.panelSelector);
        var isPanelOpen = !!(panel && panel.classList.contains('-open'));

        bars.forEach(function (bar) {
            var isInner = bar.classList.contains('-inner');
            var shouldShow = count > 0 && (isInner ? isPanelOpen : !isPanelOpen);

            if (shouldShow) {
                bar.style.display = '';
                var textEl = bar.querySelector(_config.statusTextSelector);
                if (textEl) {
                    var label =
                        count === 1
                            ? bar.getAttribute('data-quickfilter-status-label-singular') || 'active filter'
                            : bar.getAttribute('data-quickfilter-status-label-plural') || 'active filters';
                    textEl.textContent = count + ' ' + label;
                }
            } else {
                bar.style.display = 'none';
            }
        });

        _refreshStatusTooltips();
    }

    function _buildActiveFilterTitle() {
        var lines = [];

        document.querySelectorAll(_config.fieldSelector).forEach(function (field) {
            var labelEl = field.querySelector('.search-quick-filters__field-label');
            var labelText = labelEl ? labelEl.textContent.trim() : '';

            var hidden = field.querySelector(_config.hiddenSelector);
            if (hidden && hidden.value && hidden.value.trim() !== '') {
                var valueEl = field.querySelector('.search-quick-filters__field-text');
                if (valueEl) {
                    lines.push(labelText + ': ' + valueEl.textContent.trim());
                }
            }

            field.querySelectorAll(_config.dateSelector).forEach(function (dateInput) {
                if (dateInput.value && dateInput.value.trim() !== '') {
                    lines.push(labelText + ': ' + dateInput.value.trim());
                }
            });
        });

        document.querySelectorAll(_config.checkboxSelector).forEach(function (cb) {
            if (cb.checked) {
                var cbLabel = document.querySelector('label[for="' + cb.id + '"]');
                if (cbLabel) {
                    lines.push(cbLabel.textContent.trim());
                }
            }
        });

        if (lines.length === 0) return '';
        return '<ul class="tooltip--quickfilter__list"><li>' + lines.map(_escapeHtml).join('</li><li>') + '</li></ul>';
    }

    function _escapeHtml(str) {
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function _highlightMatch(text, query) {
        var escapedText = _escapeHtml(text);
        if (!query) return escapedText;
        var safeQuery = query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        var regex = new RegExp('(' + safeQuery + ')', 'gi');
        return escapedText.replace(regex, '<mark class="search-quick-filters__dropdown-item-highlight">$1</mark>');
    }

    function _initStatusTooltips() {
        if (typeof $ !== 'function' || !$.fn || !$.fn.tooltip) return;
        $(_config.statusTooltipSelector).each(function () {
            var $el = $(this);
            if ($el.data('bs.tooltip')) {
                $el.tooltip('dispose');
            }
            $el.tooltip({
                html: true,
                trigger: 'hover focus',
                placement: 'top',
                delay: { show: 300, hide: 100 },
                template: '<div class="tooltip tooltip--quickfilter" role="tooltip">' + '<div class="arrow"></div>' + '<div class="tooltip-inner"></div>' + '</div>',
                title: ' ',
            });
        });
    }

    function _refreshStatusTooltips() {
        if (typeof $ !== 'function' || !$.fn || !$.fn.tooltip) return;
        var html = _buildActiveFilterTitle();
        $(_config.statusTooltipSelector).each(function () {
            var $el = $(this);
            $el.attr('data-original-title', html);
            if (!html) {
                $el.tooltip('hide');
            }
        });
    }

    /**
     * Binds the status bar reset link to clear all filters,
     * mirroring the behavior of the in-panel reset link.
     */
    function _initStatusBarReset() {
        var resetLinks = document.querySelectorAll(_config.statusResetSelector);
        resetLinks.forEach(function (resetLink) {
            resetLink.addEventListener('click', function (e) {
                e.preventDefault();
                _resetAllFilters();
            });
        });
    }

    /**
     * Clears all quick filter values: hidden inputs, dropdown display
     * texts, date inputs, and dropdown item lists. Shared by both the
     * in-panel reset link and the status bar reset link.
     */
    function _resetAllFilters() {
        document.querySelectorAll(_config.hiddenSelector).forEach(function (h) {
            h.value = '';
        });
        document.querySelectorAll(_config.dropdownSelector + ' [' + _config.defaultTextAttr + ']').forEach(function (span) {
            span.textContent = span.getAttribute(_config.defaultTextAttr) || '';
        });
        document.querySelectorAll(_config.dateSelector).forEach(function (input) {
            input.value = '';
        });
        document.querySelectorAll('[data-quickfilter-dropdown-list]').forEach(function (list) {
            list.innerHTML = '';
        });
        _syncFieldClearButtons();
        _validateDateRange();
        _updateActionsVisibility();
    }

    /**
     * Binds input and blur handlers on date fields. On input: strips
     * non-digit characters, enforces max 4 characters, validates the
     * date range, and updates action bar visibility. On blur: re-validates.
     */
    function _initDateInputs() {
        var inputs = document.querySelectorAll(_config.dateSelector);
        inputs.forEach(function (input) {
            // Strip non-digits on input and enforce max 4 chars
            input.addEventListener('input', function () {
                var cursorPos = input.selectionStart;
                var oldLength = input.value.length;
                input.value = input.value.replace(/\D/g, '').substring(0, 4);
                var newLength = input.value.length;
                // Adjust cursor position after stripping chars
                input.selectionStart = input.selectionEnd = cursorPos - (oldLength - newLength);
                _validateDateRange();
                _updateActionsVisibility();
            });

            input.addEventListener('blur', function () {
                _validateDateRange();
            });
        });
    }

    /**
     * Validates that the "to" year is not before the "from" year.
     * Only validates when both fields contain complete 4-digit years.
     * Toggles the -invalid CSS class on both inputs.
     */
    function _validateDateRange() {
        var fromInput = document.querySelector(_config.dateSelector.replace(']', '="from"]'));
        var toInput = document.querySelector(_config.dateSelector.replace(']', '="to"]'));
        if (!fromInput || !toInput) return;

        var fromVal = fromInput.value.trim();
        var toVal = toInput.value.trim();

        // Only validate if both fields have complete 4-digit years
        var isInvalid = false;
        if (fromVal.length === 4 && toVal.length === 4) {
            var fromYear = parseInt(fromVal, 10);
            var toYear = parseInt(toVal, 10);
            if (!isNaN(fromYear) && !isNaN(toYear) && toYear < fromYear) {
                isInvalid = true;
            }
        }

        fromInput.classList.toggle('-invalid', isInvalid);
        toInput.classList.toggle('-invalid', isInvalid);
    }

    /**
     * Binds click handler on the reset link. Clears all filter values:
     * hidden inputs, dropdown display texts (reset to defaults), date inputs,
     * and dropdown item lists. Hides the actions bar.
     */
    function _initResetLink() {
        var resetLink = document.querySelector(_config.resetSelector);
        if (!resetLink) return;

        resetLink.addEventListener('click', function (e) {
            e.preventDefault();
            _resetAllFilters();
        });
    }

    /**
     * Restores dropdown display text from hidden input values after a page
     * reload, looking up translated labels from loaded dropdown items.
     * Called after dropdown data has been fetched and rendered.
     *
     * @param {string} [forField] - If specified, only restore the given field
     */
    function _restoreDropdownSelections(forField) {
        document.querySelectorAll(_config.hiddenSelector).forEach(function (hidden) {
            var field = hidden.getAttribute('data-quickfilter-hidden');
            if (!field) return;
            if (forField && field !== forField) return;

            var value = hidden.value ? hidden.value.trim() : '';
            if (!value) return;

            var trigger = document.querySelector(_config.dropdownSelector.replace(']', '="' + field + '"]'));
            if (!trigger) return;

            var span = trigger.querySelector('[' + _config.defaultTextAttr + ']');
            if (!span) return;

            // Look up the translated label from rendered dropdown items
            var panel = document.querySelector(_config.dropdownPanelPrefix + field + '"]');
            if (panel) {
                var matchingItem = panel.querySelector('.search-quick-filters__dropdown-item[data-raw-value="' + CSS.escape(value) + '"]');
                if (matchingItem) {
                    var text = matchingItem.textContent || '';
                    var match = text.match(/^(.+?)\s*\(\d+\)$/);
                    span.textContent = match ? match[1] : text;
                } else {
                    span.textContent = value;
                }
            } else {
                span.textContent = value;
            }
        });
        _updateActionsVisibility();
    }

    _module._renderDropdownItems = _renderDropdownItems;
    _module._validateDateRange = _validateDateRange;

    /**
     * Opens the filter panel programmatically. No-op if already open
     * or if the panel element does not exist.
     */
    _module.open = function () {
        var panel = document.querySelector(_config.panelSelector);
        if (panel && !panel.classList.contains('-open')) {
            var toggle = document.querySelector(_config.toggleSelector);
            if (toggle) toggle.click();
        }
    };

    viewer.searchQuickfilters = _module;
    return viewer;
})(viewerJS || {});

// CommonJS export for testing
if (typeof module !== 'undefined' && module.exports) {
    module.exports = viewerJS.searchQuickfilters;
}
