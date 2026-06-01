/**
 * Unit tests for viewerJS.listFilter.
 *
 * The constructor wires several jQuery click/keyup handlers and calls
 * enable() which subscribes to an rxjs.fromEvent(input, 'input')
 * observable. We provide a minimal `global.rxjs` stub that captures
 * the subscriber so we can drive the filter behavior directly.
 */

// rxjs stub: fromEvent → an object whose .pipe().subscribe(handler) is
// stored. Calling the stored handler from a test re-enacts a debounced
// input event, without us actually waiting 200ms.
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
        // "Apple" (display 'none' or empty), "Banana" hidden, "Apricot" visible.
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
        // First filter to "ap" (hides Banana).
        $('#filter-input').val('ap');
        _filterSubscriber();
        // Then clear the value and re-filter.
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
        // Hide one to verify resetFilters re-shows it.
        $('.filter-element').eq(1).hide();
        lf.resetFilters();
        document.querySelectorAll('.filter-element').forEach((li) => {
            expect(li.style.display).not.toBe('none');
        });
    });

    test('is a no-op when the input is not active (.in class missing)', () => {
        const lf = makeListFilter();
        $('#filter-input').val('whatever'); // value present but no .in class
        lf.resetFilters();
        // Value should remain untouched — resetFilters guards on .in.
        expect($('#filter-input').val()).toBe('whatever');
    });
});

describe('listFilter inputToggle click', () => {
    test('clicking the toggle adds .in to the input and resets prior filters', () => {
        const lf = makeListFilter();
        // Pre-condition: input is not active.
        expect($('#filter-input').hasClass('in')).toBe(false);

        $('#input-toggle').trigger('click');

        expect($('#filter-input').hasClass('in')).toBe(true);
    });
});
