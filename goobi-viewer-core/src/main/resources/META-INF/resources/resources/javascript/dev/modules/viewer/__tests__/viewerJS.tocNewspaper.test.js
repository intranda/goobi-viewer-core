/**
 * Unit tests for viewerJS.tocNewspaper.
 *
 * The 5 module-private init helpers are not exposed; we exercise
 * each through tocNewspaper.init({...}) with a tailored DOM fixture
 * + window.location state. The two delegating init helpers (calendar
 * popover, datepicker) are covered by spying on the targeted
 * sub-modules.
 */
const viewerJS = require('../viewerJS.tocNewspaper.js');

afterEach(() => {
    // Some tests mutate window.location.hash via history.replaceState;
    // wipe it back so other tests do not pick it up.
    history.replaceState(null, '', '/');
});

describe('init: search field clear', () => {
    test('clears any prefilled value in #newspaperSearchTerm', () => {
        document.body.innerHTML = '<input id="newspaperSearchTerm" value="leftover" />';
        viewerJS.tocNewspaper.init();
        expect(document.getElementById('newspaperSearchTerm').value).toBe('');
    });

    test('does nothing if #newspaperSearchTerm is absent', () => {
        document.body.innerHTML = '';
        expect(() => viewerJS.tocNewspaper.init()).not.toThrow();
    });
});

describe('init: tab persistence', () => {
    test('restores the tab indicated by #tab=ID in the URL hash', () => {
        document.body.innerHTML = `
            <ul class="search-calendar__nav-tabs">
                <li><a class="nav-link active" href="#tabA" data-toggle="tab">A</a></li>
                <li><a class="nav-link" href="#tabB" data-toggle="tab">B</a></li>
            </ul>
            <div class="search-calendar__tab-content">
                <div id="tabA" class="tab-pane show active">A pane</div>
                <div id="tabB" class="tab-pane">B pane</div>
            </div>`;
        history.replaceState(null, '', '/#tab=tabB');

        viewerJS.tocNewspaper.init();

        const a = document.querySelector('a[href="#tabA"]');
        const b = document.querySelector('a[href="#tabB"]');
        expect(a.classList.contains('active')).toBe(false);
        expect(b.classList.contains('active')).toBe(true);
        expect(document.getElementById('tabB').classList.contains('active')).toBe(true);
        expect(document.getElementById('tabB').classList.contains('show')).toBe(true);
    });

    test('clicking a tab pushes #tab=<id> into the URL hash', () => {
        document.body.innerHTML = `
            <ul class="search-calendar__nav-tabs">
                <li><a class="nav-link active" href="#tabA" data-toggle="tab">A</a></li>
                <li><a class="nav-link" href="#tabB" data-toggle="tab">B</a></li>
            </ul>`;
        viewerJS.tocNewspaper.init();

        document.querySelector('a[href="#tabB"]').click();

        expect(window.location.hash).toBe('#tab=tabB');
    });

    test('does nothing when .search-calendar__nav-tabs is missing', () => {
        document.body.innerHTML = '';
        expect(() => viewerJS.tocNewspaper.init()).not.toThrow();
    });
});

describe('init: decade grouping', () => {
    test('inserts decade headers when there are more than 5 chips', () => {
        document.body.innerHTML = `
            <div class="search-calendar__year-grid">
                <a class="search-calendar__year-chip" data-year="1900">1900</a>
                <a class="search-calendar__year-chip" data-year="1905">1905</a>
                <a class="search-calendar__year-chip" data-year="1910">1910</a>
                <a class="search-calendar__year-chip" data-year="1912">1912</a>
                <a class="search-calendar__year-chip" data-year="1920">1920</a>
                <a class="search-calendar__year-chip" data-year="1925">1925</a>
            </div>`;
        viewerJS.tocNewspaper.init();

        const headers = document.querySelectorAll('.search-calendar__decade-header');
        const labels = Array.from(headers).map((h) => h.textContent);
        expect(labels).toEqual(['1900er', '1910er', '1920er']);
    });

    test('skips grouping when there are 5 or fewer chips', () => {
        document.body.innerHTML = `
            <div class="search-calendar__year-grid">
                <a class="search-calendar__year-chip" data-year="1900"></a>
                <a class="search-calendar__year-chip" data-year="1910"></a>
                <a class="search-calendar__year-chip" data-year="1920"></a>
                <a class="search-calendar__year-chip" data-year="1930"></a>
                <a class="search-calendar__year-chip" data-year="1940"></a>
            </div>`;
        viewerJS.tocNewspaper.init();
        expect(document.querySelectorAll('.search-calendar__decade-header')).toHaveLength(0);
    });
});

describe('init: year selection reset', () => {
    test('removes -active from chips and hides month/issue panels when no year was clicked', () => {
        document.body.innerHTML = `
            <div class="search-calendar__year-grid">
                <a class="search-calendar__year-chip -active" data-year="1900"></a>
                <a class="search-calendar__year-chip" data-year="1910"></a>
            </div>
            <div class="search-calendar__issues-title">Issues</div>
            <div class="search-calendar__months">Months</div>`;
        viewerJS.tocNewspaper.init();

        expect(document.querySelector('.search-calendar__year-chip.-active')).toBeNull();
        expect(document.querySelector('.search-calendar__issues-title').style.display).toBe('none');
        expect(document.querySelector('.search-calendar__months').style.display).toBe('none');
    });

    test('keeps the active chip when #tab=newspaperTabYear is in the URL', () => {
        document.body.innerHTML = `
            <div class="search-calendar__year-grid">
                <a class="search-calendar__year-chip -active" data-year="1900"></a>
            </div>`;
        history.replaceState(null, '', '/#tab=newspaperTabYear');
        viewerJS.tocNewspaper.init();
        expect(document.querySelector('.search-calendar__year-chip.-active')).not.toBeNull();
    });
});

describe('init: ?year= URL persistence', () => {
    test('syncs the dropdown to ?year= when the URL has it and submits the form', () => {
        document.body.innerHTML = `
            <form>
                <select id="tocSelectYear">
                    <option value="">--</option>
                    <option value="1900">1900</option>
                    <option value="1910">1910</option>
                </select>
            </form>`;
        history.replaceState(null, '', '/?year=1910');
        const submitSpy = jest.spyOn(HTMLFormElement.prototype, 'submit').mockImplementation(() => {});

        viewerJS.tocNewspaper.init();

        expect(document.getElementById('tocSelectYear').value).toBe('1910');
        expect(submitSpy).toHaveBeenCalled();
        submitSpy.mockRestore();
    });

    test('writes the selected dropdown value back into the URL when no ?year= is present', () => {
        document.body.innerHTML = `
            <form>
                <select id="tocSelectYear">
                    <option value="1900" selected>1900</option>
                </select>
            </form>`;
        history.replaceState(null, '', '/some-page');

        viewerJS.tocNewspaper.init();

        expect(window.location.search).toContain('year=1900');
    });
});

describe('init: optional sub-module delegation', () => {
    test('does not call calendarPopover.init when contextPath is missing from config', () => {
        document.body.innerHTML = '';
        const init = jest.fn();
        viewerJS.calendarPopover = { init };
        viewerJS.tocNewspaper.init({});
        expect(init).not.toHaveBeenCalled();
    });

    test('passes a popover config built from contextPath when contextPath is present', () => {
        document.body.innerHTML = '';
        const init = jest.fn();
        viewerJS.calendarPopover = { init };
        viewerJS.tocNewspaper.init({ contextPath: '/viewer', popoverTitle: 'PT' });
        expect(init).toHaveBeenCalledWith({
            appUrl: '/viewer/',
            indexResourceUrl: '/viewer/api/v1/index/query/',
            popoverTitle: 'PT',
        });
    });

    test('calls datePicker.createRange only when locale + dateFrom/To selectors are all provided', () => {
        document.body.innerHTML = '<input id="from"/><input id="to"/>';
        const createRange = jest.fn();
        viewerJS.datePicker = { createRange };

        viewerJS.tocNewspaper.init({
            locale: 'de',
            dateFromSelector: '#from',
            dateToSelector: '#to',
        });

        expect(createRange).toHaveBeenCalled();
        const [start, end, opts] = createRange.mock.calls[0];
        expect(start.id).toBe('from');
        expect(end.id).toBe('to');
        expect(opts.locale).toBe('de');
    });
});
