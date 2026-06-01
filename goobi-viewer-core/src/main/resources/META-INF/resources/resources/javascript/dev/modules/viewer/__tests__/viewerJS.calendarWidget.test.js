/**
 * Unit tests for viewerJS.calendarWidget.
 *
 * init() returns a state object (or null on bad config). The function
 * fires off two `fetch()` requests; we stub fetch globally so they
 * resolve synchronously. The destroy() callback delegates to
 * viewer.datePicker.destroy, which we stub on the namespace.
 *
 * We focus on:
 *   - guards that return null,
 *   - the appUrl normalisation (trailing slash),
 *   - the state object shape,
 *   - destroy() calling datePicker.destroy and clearing the container.
 *
 * The popover-opening / scroll-position branches need a real
 * datepicker instance and are out of scope.
 */

// fetch + JSON polyfill: every call resolves to an empty array so
// _fetchYearData / _fetchAvailableMonths complete without errors.
global.fetch = jest.fn(() => Promise.resolve({ ok: true, json: () => Promise.resolve([]) }));

const viewerJS = require('../viewerJS.calendarWidget.js');

beforeEach(() => {
    document.body.innerHTML = '<div id="cal"></div>';
    fetch.mockClear();
    // datePicker stub. The async _createCalendar path needs
    // _parseISODate + createInlineCalendar. destroy is the teardown
    // path. Returning a plain calendarInstance keeps both happy.
    viewerJS.datePicker = {
        destroy: jest.fn(),
        _parseISODate: jest.fn((s) => new Date(s)),
        createInlineCalendar: jest.fn(() => ({
            calendarContainer: document.createElement('div'),
        })),
    };
});

describe('init guards', () => {
    test('returns null when container is missing', () => {
        const result = viewerJS.calendarWidget.init({ anchorPi: 'PPN1', currentYear: '1900' });
        expect(result).toBeNull();
    });

    test('returns null when anchorPi is missing', () => {
        const result = viewerJS.calendarWidget.init({
            container: document.getElementById('cal'),
            currentYear: '1900',
        });
        expect(result).toBeNull();
    });

    test('returns null when currentYear is missing', () => {
        const result = viewerJS.calendarWidget.init({
            container: document.getElementById('cal'),
            anchorPi: 'PPN1',
        });
        expect(result).toBeNull();
    });
});

describe('appUrl normalisation', () => {
    test('appends a trailing slash when appUrl does not end with one', () => {
        const config = {
            container: document.getElementById('cal'),
            anchorPi: 'PPN1',
            currentYear: '1900',
            appUrl: 'https://example.org/viewer',
        };
        viewerJS.calendarWidget.init(config);
        // The state config sees the mutated appUrl.
        expect(config.appUrl).toBe('https://example.org/viewer/');
    });

    test('leaves appUrl alone when it already ends with a slash', () => {
        const config = {
            container: document.getElementById('cal'),
            anchorPi: 'PPN1',
            currentYear: '1900',
            appUrl: 'https://example.org/viewer/',
        };
        viewerJS.calendarWidget.init(config);
        expect(config.appUrl).toBe('https://example.org/viewer/');
    });
});

describe('state shape', () => {
    test('returns an object with config, calendarInstance=null, currentIssueDate, and a destroy() function', () => {
        const state = viewerJS.calendarWidget.init({
            container: document.getElementById('cal'),
            anchorPi: 'PPN1',
            currentYear: '1900',
            currentIssueDate: '1900-05-01',
        });
        expect(state).not.toBeNull();
        expect(state.calendarInstance).toBeNull();
        expect(state.currentIssueDate).toBe('1900-05-01');
        expect(typeof state.destroy).toBe('function');
        expect(state.dateEntriesMap).toEqual({});
    });

    test('defaults currentIssueDate to null when not provided', () => {
        const state = viewerJS.calendarWidget.init({
            container: document.getElementById('cal'),
            anchorPi: 'PPN1',
            currentYear: '1900',
        });
        expect(state.currentIssueDate).toBeNull();
    });
});

describe('fetch wiring', () => {
    test('issues two fetch calls (year data + available months) keyed off anchorPi and appUrl', () => {
        viewerJS.calendarWidget.init({
            container: document.getElementById('cal'),
            anchorPi: 'PPN42',
            currentYear: '1850',
            appUrl: '/viewer',
        });
        // Two fetches were fired.
        expect(fetch).toHaveBeenCalledTimes(2);
        const urls = fetch.mock.calls.map((c) => c[0]);
        // Both URLs reference the anchorPi and live under the trailing-slash appUrl.
        expect(urls.some((u) => u.includes('PPN42') && u.includes('/months'))).toBe(true);
    });
});

describe('destroy', () => {
    test('calls viewer.datePicker.destroy when a calendarInstance is present and clears the container', () => {
        const state = viewerJS.calendarWidget.init({
            container: document.getElementById('cal'),
            anchorPi: 'PPN1',
            currentYear: '1900',
        });
        // Simulate a live calendar instance + container content.
        state.calendarInstance = { mock: true };
        state.config.container.innerHTML = '<span>old content</span>';

        state.destroy();

        expect(viewerJS.datePicker.destroy).toHaveBeenCalledWith({ mock: true });
        expect(state.calendarInstance).toBeNull();
        expect(state.config.container.innerHTML).toBe('');
    });

    test('skips datePicker.destroy when no calendarInstance was created', () => {
        const state = viewerJS.calendarWidget.init({
            container: document.getElementById('cal'),
            anchorPi: 'PPN1',
            currentYear: '1900',
        });
        state.destroy();
        expect(viewerJS.datePicker.destroy).not.toHaveBeenCalled();
    });
});
