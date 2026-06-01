/**
 * Unit tests for viewerJS.jsfAjax.
 *
 * The source builds Subjects via `new rxjs.Subject().pipe(rxjs.operators.filter(...))`
 * and then calls `.next()` on the result — the production rxjs build
 * thus appears to return the same Subject from .pipe(). We provide a
 * matching stub: pipe() chains operators on the same instance,
 * filter() blocks values that fail the predicate, first() is a no-op
 * (we only test single emissions anyway).
 */

function makeStubSubject() {
    const subs = [];
    const filters = [];
    return {
        next: function (v) {
            for (const f of filters) {
                if (!f(v)) return;
            }
            subs.slice().forEach((s) => s(v));
        },
        subscribe: function (handler) {
            subs.push(handler);
            return { unsubscribe: () => subs.splice(subs.indexOf(handler), 1) };
        },
        pipe: function (...ops) {
            ops.forEach((op) => op && op(filters));
            return this;
        },
        _subs: subs,
        _filters: filters,
    };
}

global.rxjs = {
    Subject: function () {
        return makeStubSubject();
    },
    operators: {
        filter: (predicate) => (filters) => filters.push(predicate),
        first: () => (filters) => {}, // no-op stub — adequate for our assertions
    },
};

const viewerJS = require('../viewerJS.jsfAjax.js');
const jsfAjax = viewerJS.jsfAjax;

describe('module shape', () => {
    test('exposes begin / complete / success / error streams plus init / handleResponse', () => {
        for (const name of ['begin', 'complete', 'success', 'error']) {
            expect(typeof jsfAjax[name].next).toBe('function');
            expect(typeof jsfAjax[name].subscribe).toBe('function');
        }
        expect(typeof jsfAjax.init).toBe('function');
        expect(typeof jsfAjax.handleResponse).toBe('function');
    });
});

describe('filterAjaxEvents', () => {
    test('events whose source.dataset.jsfUpdateType === "ignore" are dropped', () => {
        const seen = [];
        jsfAjax.success.subscribe((e) => seen.push(e));
        jsfAjax.success.next({ source: { dataset: { jsfUpdateType: 'ignore' } } });
        expect(seen).toHaveLength(0);
    });

    test('events with any other jsfUpdateType pass through', () => {
        const seen = [];
        jsfAjax.success.subscribe((e) => seen.push(e));
        jsfAjax.success.next({ source: { dataset: {} } });
        jsfAjax.success.next({ source: { dataset: { jsfUpdateType: 'other' } } });
        expect(seen).toHaveLength(2);
    });
});

describe('handleResponse', () => {
    test('200 → calls success callback with the response', () => {
        const onSuccess = jest.fn();
        const onError = jest.fn();
        jsfAjax.handleResponse(onSuccess, onError);
        jsfAjax.complete.next({ responseCode: 200, source: { dataset: {} } });
        expect(onSuccess).toHaveBeenCalledWith({ responseCode: 200, source: { dataset: {} } });
        expect(onError).not.toHaveBeenCalled();
    });

    test('non-200 → calls error callback with the response', () => {
        const onSuccess = jest.fn();
        const onError = jest.fn();
        jsfAjax.handleResponse(onSuccess, onError);
        jsfAjax.complete.next({ responseCode: 500, source: { dataset: {} } });
        expect(onError).toHaveBeenCalledWith({ responseCode: 500, source: { dataset: {} } });
        expect(onSuccess).not.toHaveBeenCalled();
    });

    test('missing callbacks are tolerated (no throw)', () => {
        jsfAjax.handleResponse();
        expect(() => jsfAjax.complete.next({ responseCode: 200, source: { dataset: {} } })).not.toThrow();
        expect(() => jsfAjax.complete.next({ responseCode: 500, source: { dataset: {} } })).not.toThrow();
    });
});
