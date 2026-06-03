/**
 * Unit tests for viewerJS.notifications.
 *
 * The source subscribes to viewer.jsfAjax.success at module load, so
 * we must seed that subject (plus a translator stub) BEFORE the source
 * runs. The standard `require()` does not work here because the
 * module-local `var viewerJS = (function(viewer){...})(viewerJS || {})`
 * shadows the global — the IIFE always sees an empty object via the
 * local. We side-step that by loading the source via indirect eval,
 * which is the established pattern in this repo (see
 * crowdsourcing-loader.js + geomap-loader.js).
 *
 * We test only the synchronous delegation surface — success/error/warn
 * → notify(_, _, severity) — and the swaltoasts.onSuccess filter.
 * notify() itself ends up at the legacy `alert()` fallback when none
 * of Swal/sweetAlert/overhang are present, which we exercise in one
 * smoke test.
 */
const fs = require('fs');
const path = require('path');

function makeSubject() {
    const subs = [];
    return {
        subscribe: (h) => subs.push(h),
        next: (v) => subs.forEach((h) => h(v)),
        _subs: subs,
    };
}

global.viewerJS = {
    jsfAjax: { success: makeSubject() },
    translator: {
        addTranslations: () => Promise.resolve(),
        translate: (k) => k,
    },
};

(0, eval)(fs.readFileSync(path.join(__dirname, '..', 'viewerJS.notifications.js'), 'utf8'));

const notifications = global.viewerJS.notifications;
const swaltoasts = global.viewerJS.swaltoasts;

describe('viewerJS.notifications.success/error/warn', () => {
    let notifySpy;
    beforeEach(() => {
        notifySpy = jest.spyOn(notifications, 'notify').mockImplementation(() => {});
    });
    afterEach(() => {
        notifySpy.mockRestore();
    });

    test('success delegates to notify with severity "success"', () => {
        notifications.success('Title', 'Body');
        expect(notifySpy).toHaveBeenCalledWith('Title', 'Body', 'success');
    });

    test('error delegates to notify with severity "error"', () => {
        notifications.error('Title', 'Body');
        expect(notifySpy).toHaveBeenCalledWith('Title', 'Body', 'error');
    });

    test('warn delegates to notify with severity "warning"', () => {
        // Note the type is "warning", not "warn" — pin this since the
        // outer alias is `warn` but the underlying severity is the
        // sweetalert2 vocabulary.
        notifications.warn('Title', 'Body');
        expect(notifySpy).toHaveBeenCalledWith('Title', 'Body', 'warning');
    });
});

describe('viewerJS.notifications.notify (no Swal/sweetAlert/overhang)', () => {
    test('falls through to window.alert when no notification library is loaded', () => {
        const alertSpy = jest.spyOn(window, 'alert').mockImplementation(() => {});
        try {
            notifications.notify('T', 'M', 'success');
            expect(alertSpy).toHaveBeenCalledWith('M');
        } finally {
            alertSpy.mockRestore();
        }
    });
});

describe('viewerJS.swaltoasts.onSuccess', () => {
    test('returns the live toaster when event.status === "success"', () => {
        const live = swaltoasts.onSuccess({ status: 'success' });
        expect(live).toBe(swaltoasts);
    });

    test('returns no-op stubs for non-success statuses', () => {
        const noop = swaltoasts.onSuccess({ status: 'error' });
        // The stub object has the same call signature but does nothing.
        expect(typeof noop.success).toBe('function');
        expect(noop.success('a', 'b')).toBeUndefined();
        expect(typeof noop.error).toBe('function');
        expect(typeof noop.warn).toBe('function');
        // Crucially, it is NOT the live toaster.
        expect(noop).not.toBe(swaltoasts);
    });
});

describe('viewerJS.swaltoasts.success/error/warn (no Swal)', () => {
    test('falls through to window.alert when no Swal is available', () => {
        const alertSpy = jest.spyOn(window, 'alert').mockImplementation(() => {});
        try {
            swaltoasts.success('T', 'M');
            expect(alertSpy).toHaveBeenCalledWith('M');
        } finally {
            alertSpy.mockRestore();
        }
    });
});
