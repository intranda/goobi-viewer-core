/**
 * Search toolbar
 *
 * TEMPORARY: rearranges the search list controls for the new reference design.
 * Collects them in one band above the result list; rare actions go into an overflow menu.
 * Themes start it with viewerJS.searchToolbar.init().
 *
 * @module viewerJS.searchToolbar
 */
var viewerJS = (function (viewer) {
    'use strict';

    var _module = {};
    var _defaults = {
        optionsSelector: '.search-list__options',
        bandSelector: '.search-list__nav-helper-wrapper',
        hitsSelector: '.search-list__hits',
        initializedAttr: 'referenceToolbar',
    };

    /** Moves the controls into the band */
    _module.init = function () {
        var options = document.querySelector(_defaults.optionsSelector);
        if (!options) return;
        // the core renders the band only for several hits
        var band = document.querySelector(_defaults.bandSelector) || _createBand();
        if (!band || band.dataset[_defaults.initializedAttr]) return;
        band.dataset[_defaults.initializedAttr] = 'true';

        var group = function (name) {
            var element = document.createElement('div');
            element.className = name;
            return element;
        };
        var start = group('search-list__band-start');
        var end = group('search-list__band-end');

        // modals go to the body
        options.querySelectorAll('.modal').forEach(function (modal) {
            document.body.append(modal);
        });

        // start: hit count
        var hitcount = options.querySelector('.search-list__hitcount');
        if (hitcount) start.append(hitcount);
        band.querySelectorAll('.search-list__nav-helper-item').forEach(function (item) {
            start.append(item);
        });

        // end: pagination and views
        var paginator = band.querySelector('.inputfield-paginator');
        if (paginator) end.append(paginator);
        var views = options.querySelector('.search-list__views');
        if (views) end.append(views);

        // rare actions go into the overflow menu (explicit order: export last)
        var rare = [].concat(
            Array.from(options.querySelectorAll('.search-list__rss')),
            Array.from(options.querySelectorAll('.search-list__save-search')),
            Array.from(options.querySelectorAll('.search-list__help')),
            Array.from(options.querySelectorAll('.search-list__export-dropdown-wrapper'))
        );
        if (rare.length) end.append(_buildOverflow(rare));

        band.append(start, end);
        options.remove();
    };

    /** Creates the band for lists without one */
    function _createBand() {
        var hits = document.querySelector(_defaults.hitsSelector);
        if (!hits) return null;
        var band = document.createElement('div');
        band.className = 'search-list__nav-helper-wrapper';
        hits.before(band);
        return band;
    }

    /** Builds the overflow menu */
    function _buildOverflow(items) {
        var wrapper = document.createElement('div');
        wrapper.className = 'dropdown search-list__overflow';

        var toggle = document.createElement('button');
        toggle.type = 'button';
        toggle.className = 'btn btn--icon search-list__option-button search-list__overflow-toggle';
        toggle.dataset.toggle = 'dropdown';
        toggle.setAttribute('aria-haspopup', 'true');
        toggle.setAttribute('aria-expanded', 'false');
        var main = document.querySelector('.content__main');
        var label = main && main.dataset.labelMoreOptions;
        if (label) toggle.setAttribute('aria-label', label);
        toggle.innerHTML =
            '<span class="icon-wrapper"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><circle cx="12" cy="5" r="1.7"/><circle cx="12" cy="12" r="1.7"/><circle cx="12" cy="19" r="1.7"/></svg></span>';

        var menu = document.createElement('div');
        menu.className = 'dropdown-menu dropdown-menu-right search-list__overflow-menu';

        items.forEach(function (item) {
            // no tooltips in the menu
            item.querySelectorAll('[data-toggle="tooltip"]').forEach(function (element) {
                if (window.jQuery && $(element).tooltip) $(element).tooltip('dispose');
                element.removeAttribute('data-toggle');
            });
            // show the accessible name of icon-only controls
            item.querySelectorAll('a, button').forEach(function (control) {
                if (control.textContent.trim() || control.matches('.search-list__export-dropdown-toggle')) return;
                var text = control.getAttribute('aria-label') || control.getAttribute('title');
                if (!text) return;
                var name = document.createElement('span');
                name.className = 'search-list__overflow-label';
                name.textContent = text;
                control.append(name);
            });
            menu.append(item);
        });

        wrapper.append(toggle, menu);
        return wrapper;
    }

    viewer.searchToolbar = _module;
    return viewer;
})(viewerJS || {});

if (typeof module !== 'undefined' && module.exports) {
    module.exports = viewerJS.searchToolbar;
}
