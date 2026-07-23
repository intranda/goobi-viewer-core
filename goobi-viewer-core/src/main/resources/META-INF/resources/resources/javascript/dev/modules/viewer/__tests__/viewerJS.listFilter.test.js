// rxjs stub: captures the subscriber so tests can trigger the debounced input handler directly
let _filterSubscriber = null;
global.rxjs = {
    fromEvent: function (target, eventName) {
        return {
            pipe: function () {
                return {
                    subscribe: function (handler) {
                        _filterSubscriber = handler;
                        return { unsubscribe: jest.fn() };
                    },
                };
            },
        };
    },
    operators: {
        debounceTime: function () {
            return null;
        },
    },
};

const viewerJS = require('../viewerJS.listFilter.js');

function makeListFilter() {
    document.body.innerHTML = `
        <div id="wrapper">
            <input id="filter-input" type="text" />
            <button id="input-toggle">T</button>
            <span id="filter-status" role="status" data-filter-status="" data-filter-status-label="{0} sichtbar"></span>
            <h3 id="filter-header">Header</h3>
            <ul>
                <li class="filter-element"><a>Apple</a></li>
                <li class="filter-element"><a>Banana</a></li>
                <li class="filter-element"><a>Apricot</a></li>
            </ul>
        </div>`;

    return new viewerJS.listFilter({
        wrapper: '#wrapper',
        input: $('#filter-input'),
        inputToggle: $('#input-toggle'),
        header: $('#filter-header'),
        elements: $('.filter-element'),
    });
}

beforeEach(() => {
    _filterSubscriber = null;
});

describe('listFilter constructor + enable', () => {
    test('shows the wrapper element when constructed', () => {
        document.body.innerHTML = `
            <div id="wrapper" style="display:none">
                <input id="filter-input" />
                <button id="input-toggle"></button>
                <h3 id="filter-header"></h3>
                <ul><li class="filter-element"><a>x</a></li></ul>
            </div>`;
        new viewerJS.listFilter({
            wrapper: '#wrapper',
            input: $('#filter-input'),
            inputToggle: $('#input-toggle'),
            header: $('#filter-header'),
            elements: $('.filter-element'),
        });
        expect(document.getElementById('wrapper').style.display).not.toBe('none');
    });

    test('subscribes to input events on the configured input', () => {
        makeListFilter();
        expect(typeof _filterSubscriber).toBe('function');
    });
});

describe('listFilter.filter', () => {
    test('hides elements whose anchor text does not contain the filter value', () => {
        const lf = makeListFilter();
        $('#filter-input').val('apr');
        _filterSubscriber();

        const items = document.querySelectorAll('.filter-element');
        const visibleByText = (text) => Array.from(items).find((li) => li.textContent.trim() === text).style.display;
        expect(visibleByText('Apricot')).not.toBe('none');
        expect(visibleByText('Banana')).toBe('none');
        expect(visibleByText('Apple')).toBe('none');
    });

    test('matches case-insensitively', () => {
        const lf = makeListFilter();
        $('#filter-input').val('APPLE');
        _filterSubscriber();
        const apple = Array.from(document.querySelectorAll('.filter-element')).find((li) => li.textContent.trim() === 'Apple');
        expect(apple.style.display).not.toBe('none');
    });

    test('shows all elements when the input is empty', () => {
        const lf = makeListFilter();
        $('#filter-input').val('ap');
        _filterSubscriber();
        $('#filter-input').val('');
        _filterSubscriber();
        document.querySelectorAll('.filter-element').forEach((li) => {
            expect(li.style.display).not.toBe('none');
        });
    });
});

describe('listFilter.resetFilters', () => {
    test('clears the value and removes the .in class when the input is currently active', () => {
        const lf = makeListFilter();
        $('#filter-input').addClass('in').val('apple');

        lf.resetFilters();

        expect($('#filter-input').hasClass('in')).toBe(false);
        expect($('#filter-input').val()).toBe('');
    });

    test('shows all elements again on reset', () => {
        const lf = makeListFilter();
        $('#filter-input').addClass('in').val('apple');
        $('.filter-element').eq(1).hide();
        lf.resetFilters();
        document.querySelectorAll('.filter-element').forEach((li) => {
            expect(li.style.display).not.toBe('none');
        });
    });

    test('is a no-op when the input is not active (.in class missing)', () => {
        const lf = makeListFilter();
        $('#filter-input').val('whatever');
        lf.resetFilters();
        expect($('#filter-input').val()).toBe('whatever');
    });
});

describe('listFilter inputToggle click', () => {
    test('clicking the toggle adds .in to the input and resets prior filters', () => {
        const lf = makeListFilter();
        expect($('#filter-input').hasClass('in')).toBe(false);

        $('#input-toggle').trigger('click');

        expect($('#filter-input').hasClass('in')).toBe(true);
    });
});

describe('listFilter header click (default mode)', () => {
    test('toggles the filter input', () => {
        makeListFilter();
        $('#filter-header').trigger('click');
        expect($('#filter-input').hasClass('in')).toBe(true);
    });
});

describe('listFilter aria-expanded on the input toggle', () => {
    test('reflects the closed state after construction', () => {
        makeListFilter();
        expect($('#input-toggle').attr('aria-expanded')).toBe('false');
    });

    test('switches to true when the toggle opens the input and back to false on the second click', () => {
        makeListFilter();
        $('#input-toggle').trigger('click');
        expect($('#input-toggle').attr('aria-expanded')).toBe('true');
        $('#input-toggle').trigger('click');
        expect($('#input-toggle').attr('aria-expanded')).toBe('false');
    });

    test('switches to false when the filter is reset (e.g. via Escape)', () => {
        const lf = makeListFilter();
        $('#input-toggle').trigger('click');
        lf.resetFilters();
        expect($('#input-toggle').attr('aria-expanded')).toBe('false');
    });

    test('stays in sync when the header toggles the input in default mode', () => {
        makeListFilter();
        $('#filter-header').trigger('click');
        expect($('#input-toggle').attr('aria-expanded')).toBe('true');
    });
});

describe('listFilter status announcement', () => {
    test('announces the number of visible entries after filtering', () => {
        makeListFilter();
        $('#filter-input').val('ap');
        _filterSubscriber();
        expect(document.getElementById('filter-status').textContent).toBe('2 sichtbar');
    });

    test('announces zero matches', () => {
        makeListFilter();
        $('#filter-input').val('zzz');
        _filterSubscriber();
        expect(document.getElementById('filter-status').textContent).toBe('0 sichtbar');
    });

    test('clears the announcement when the input is emptied', () => {
        makeListFilter();
        $('#filter-input').val('ap');
        _filterSubscriber();
        $('#filter-input').val('');
        _filterSubscriber();
        expect(document.getElementById('filter-status').textContent).toBe('');
    });

    test('does not fail when no status element is present', () => {
        makeListFilter();
        document.getElementById('filter-status').remove();
        $('#filter-input').val('ap');
        expect(() => _filterSubscriber()).not.toThrow();
    });
});

describe('listFilter with persistent flag (e.g. combined facets sidebar)', () => {
    function makePersistentListFilter() {
        document.body.innerHTML = `
            <div id="wrapper">
                <input id="filter-input" class="widget-search-facets__filter-input" type="text" />
                <button id="input-toggle">T</button>
                <h3 id="filter-header">Header</h3>
                <ul>
                    <li class="filter-element"><a>Apple</a></li>
                    <li class="filter-element"><a>Banana</a></li>
                </ul>
            </div>
            <div id="outside">outside</div>`;

        return new viewerJS.listFilter({
            wrapper: '#wrapper',
            input: $('#filter-input'),
            inputToggle: $('#input-toggle'),
            header: $('#filter-header'),
            elements: $('.filter-element'),
            persistent: true,
        });
    }

    test('header click does not toggle the filter input', () => {
        makePersistentListFilter();
        $('#filter-header').trigger('click');
        expect($('#filter-input').hasClass('in')).toBe(false);
    });

    test('clicks outside the sidebar do not close an open filter input', () => {
        makePersistentListFilter();
        $('#input-toggle').trigger('click');
        expect($('#filter-input').hasClass('in')).toBe(true);
        $('#outside').trigger('click');
        expect($('#filter-input').hasClass('in')).toBe(true);
    });
});
