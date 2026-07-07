/**
 * Unit tests for the viewerImmersive.mjs helpers (pure functions and the
 * DOM-bound attachUrlSync).
 *
 * Note: Jest's `jest` global is not auto-injected in native ESM projects
 * (transform: {}). We import it explicitly from @jest/globals.
 */
import { jest } from '@jest/globals';
import {
    pageUrlPath,
    pickActiveTocPageNo,
    attachUrlSync,
    normalizeThumbSizeStep,
    readThumbSizeStep,
    writeThumbSizeStep,
    THUMB_SIZE_KEY,
    isTypingTarget,
    panelIdForKeyEvent,
    isMacPlatform,
    macKeyLabel,
} from '../viewerImmersive.mjs';

// ---------------------------------------------------------------------------
// pageUrlPath
// ---------------------------------------------------------------------------

describe('pageUrlPath', function () {
    test('replaces existing page-number segment in URL', function () {
        expect(pageUrlPath('/viewer/immersive/PPN123/4/', 'PPN123', 7)).toBe('/viewer/immersive/PPN123/7/');
    });

    test('appends page number when no numeric segment exists', function () {
        expect(pageUrlPath('/viewer/immersive/PPN123/', 'PPN123', 3)).toBe('/viewer/immersive/PPN123/3/');
    });

    test('does not mistake a numeric PI for the page-number segment', function () {
        expect(pageUrlPath('/viewer/immersive/15920242/', '15920242', 5)).toBe('/viewer/immersive/15920242/5/');
    });
});

// ---------------------------------------------------------------------------
// pickActiveTocPageNo
// ---------------------------------------------------------------------------

describe('pickActiveTocPageNo', function () {
    test('keeps the active section while any visible page is in its range', function () {
        expect(pickActiveTocPageNo([1, 5, 9], [5, 6], 5)).toBe(5);
        // double-page spread: the left page belongs to the previous section, the
        // right page still falls in the active one -> keep it
        expect(pickActiveTocPageNo([1, 5, 9], [4, 5], 5)).toBe(5);
    });

    test('a section starting on the right page of a spread wins when none is active', function () {
        expect(pickActiveTocPageNo([1, 6], [5, 6])).toBe(6);
    });

    test('the next section takes over once the active one leaves the visible range', function () {
        expect(pickActiveTocPageNo([1, 6], [6, 7], 1)).toBe(6);
    });

    test('without an active section the owner of the last visible page wins', function () {
        expect(pickActiveTocPageNo([1, 5, 9], [7])).toBe(5);
        expect(pickActiveTocPageNo([1, 5, 9], [9, 10])).toBe(9);
    });

    test('returns null when no section starts at or before the visible pages', function () {
        expect(pickActiveTocPageNo([10], [2])).toBeNull();
    });

    test('the last section owns all pages to the end', function () {
        expect(pickActiveTocPageNo([1], [40], 1)).toBe(1);
    });

    test('empty entries or pages leave the active section untouched', function () {
        expect(pickActiveTocPageNo([], [5], 3)).toBe(3);
        expect(pickActiveTocPageNo([1, 5], [], 5)).toBe(5);
    });
});

// ---------------------------------------------------------------------------
// attachUrlSync
// ---------------------------------------------------------------------------

class FakeEmitter {
    constructor() {
        this._subscribers = [];
    }
    subscribe(fn) {
        this._subscribers.push(fn);
    }
    emit(v) {
        this._subscribers.forEach((fn) => fn(v));
    }
}

describe('attachUrlSync', function () {
    test('page-change pushes history entry with order+1 in pathname', function () {
        window.history.replaceState({}, '', '/viewer/immersive/PPN123/1/');
        const viewer = { onPageChange: new FakeEmitter(), goToPage: jest.fn() };
        attachUrlSync(viewer, 'PPN123');
        viewer.onPageChange.emit(2);
        expect(window.location.pathname).toBe('/viewer/immersive/PPN123/3/');
    });

    test('popstate with state.order calls viewer.goToPage', function () {
        const viewer = { onPageChange: new FakeEmitter(), goToPage: jest.fn() };
        attachUrlSync(viewer, 'PPN123');
        window.dispatchEvent(new PopStateEvent('popstate', { state: { order: 4 } }));
        expect(viewer.goToPage).toHaveBeenCalledWith(4);
    });

    test('popstate with null state does not call viewer.goToPage', function () {
        const viewer = { onPageChange: new FakeEmitter(), goToPage: jest.fn() };
        attachUrlSync(viewer, 'PPN123');
        window.dispatchEvent(new PopStateEvent('popstate', { state: null }));
        expect(viewer.goToPage).not.toHaveBeenCalled();
    });
});

// ---------------------------------------------------------------------------
// normalizeThumbSizeStep
// ---------------------------------------------------------------------------

describe('normalizeThumbSizeStep', function () {
    test('accepts the valid steps 0, 1 and 2 (number or numeric string)', function () {
        expect(normalizeThumbSizeStep('0')).toBe(0);
        expect(normalizeThumbSizeStep('1')).toBe(1);
        expect(normalizeThumbSizeStep('2')).toBe(2);
        expect(normalizeThumbSizeStep(0)).toBe(0);
        expect(normalizeThumbSizeStep(2)).toBe(2);
    });

    test('accepts leading-numeric strings (parseInt semantics)', function () {
        expect(normalizeThumbSizeStep('2abc')).toBe(2);
        expect(normalizeThumbSizeStep('1.5')).toBe(1);
    });

    test('falls back to the default step (1) for invalid values', function () {
        expect(normalizeThumbSizeStep(null)).toBe(1);
        expect(normalizeThumbSizeStep(undefined)).toBe(1);
        expect(normalizeThumbSizeStep('')).toBe(1);
        expect(normalizeThumbSizeStep('abc')).toBe(1);
        expect(normalizeThumbSizeStep('5')).toBe(1);
        expect(normalizeThumbSizeStep(-1)).toBe(1);
    });
});

// ---------------------------------------------------------------------------
// readThumbSizeStep / writeThumbSizeStep
// ---------------------------------------------------------------------------

describe('readThumbSizeStep / writeThumbSizeStep', function () {
    function fakeStorage() {
        const store = {};
        return {
            getItem: (k) => (k in store ? store[k] : null),
            setItem: (k, v) => {
                store[k] = String(v);
            },
        };
    }

    test('round-trips a written step', function () {
        const storage = fakeStorage();
        writeThumbSizeStep(storage, 2);
        expect(readThumbSizeStep(storage)).toBe(2);
    });

    test('returns the default step for an empty storage', function () {
        expect(readThumbSizeStep(fakeStorage())).toBe(1);
    });

    test('returns the default step for an invalid stored value', function () {
        const storage = fakeStorage();
        storage.setItem(THUMB_SIZE_KEY, 'garbage');
        expect(readThumbSizeStep(storage)).toBe(1);
        storage.setItem(THUMB_SIZE_KEY, '7');
        expect(readThumbSizeStep(storage)).toBe(1);
    });

    test('normalizes invalid values when writing', function () {
        const storage = fakeStorage();
        writeThumbSizeStep(storage, 7);
        expect(storage.getItem(THUMB_SIZE_KEY)).toBe('1');
    });

    test('swallows storage errors (private mode)', function () {
        const throwing = {
            getItem: () => {
                throw new Error('denied');
            },
            setItem: () => {
                throw new Error('denied');
            },
        };
        expect(readThumbSizeStep(throwing)).toBe(1);
        expect(() => writeThumbSizeStep(throwing, 2)).not.toThrow();
    });
});

// ---------------------------------------------------------------------------
// isTypingTarget
// ---------------------------------------------------------------------------

describe('isTypingTarget', function () {
    test('true for form fields (input, textarea, select)', function () {
        ['input', 'textarea', 'select'].forEach(function (tag) {
            expect(isTypingTarget(document.createElement(tag))).toBe(true);
        });
    });

    test('true for contenteditable elements and their descendants', function () {
        const editor = document.createElement('div');
        editor.setAttribute('contenteditable', 'true');
        const child = document.createElement('span');
        editor.appendChild(child);
        expect(isTypingTarget(editor)).toBe(true);
        expect(isTypingTarget(child)).toBe(true);
    });

    test('true for an empty contenteditable attribute', function () {
        const editor = document.createElement('div');
        editor.setAttribute('contenteditable', '');
        expect(isTypingTarget(editor)).toBe(true);
    });

    test('false for contenteditable="false"', function () {
        const el = document.createElement('div');
        el.setAttribute('contenteditable', 'false');
        expect(isTypingTarget(el)).toBe(false);
    });

    test('false for plain elements and non-element targets', function () {
        expect(isTypingTarget(document.createElement('button'))).toBe(false);
        expect(isTypingTarget(document.body)).toBe(false);
        expect(isTypingTarget(document)).toBe(false);
        expect(isTypingTarget(null)).toBe(false);
    });
});

// ---------------------------------------------------------------------------
// panelIdForKeyEvent
// ---------------------------------------------------------------------------

describe('panelIdForKeyEvent', function () {
    const altDigit = (code) => ({ altKey: true, ctrlKey: false, metaKey: false, code });

    test('maps Alt+1-4 to the rail panels in toolbar order (TOC, fulltext, search, metadata)', function () {
        expect(panelIdForKeyEvent(altDigit('Digit1'))).toBe('immersivePanelMenu');
        expect(panelIdForKeyEvent(altDigit('Digit2'))).toBe('immersivePanelFulltext');
        expect(panelIdForKeyEvent(altDigit('Digit3'))).toBe('immersivePanelSearch');
        expect(panelIdForKeyEvent(altDigit('Digit4'))).toBe('immersivePanelMetadata');
    });

    test('bare digits are no shortcut (WCAG 2.1.4: no printable single-character shortcuts)', function () {
        expect(panelIdForKeyEvent({ altKey: false, ctrlKey: false, metaKey: false, code: 'Digit1' })).toBeNull();
    });

    test('Ctrl/Cmd combinations stay with the browser (tab switching)', function () {
        expect(panelIdForKeyEvent({ altKey: true, ctrlKey: true, metaKey: false, code: 'Digit1' })).toBeNull();
        expect(panelIdForKeyEvent({ altKey: true, ctrlKey: false, metaKey: true, code: 'Digit1' })).toBeNull();
    });

    test('numpad digits and other codes are ignored (Alt+numpad types characters on Windows)', function () {
        expect(panelIdForKeyEvent(altDigit('Numpad1'))).toBeNull();
        expect(panelIdForKeyEvent(altDigit('Digit5'))).toBeNull();
        expect(panelIdForKeyEvent(altDigit('KeyA'))).toBeNull();
        expect(panelIdForKeyEvent(altDigit(undefined))).toBeNull();
    });
});

// ---------------------------------------------------------------------------
// isMacPlatform / macKeyLabel
// ---------------------------------------------------------------------------

describe('isMacPlatform', function () {
    test('true for Apple platforms (macOS, iPadOS/iOS hardware keyboards)', function () {
        expect(isMacPlatform('MacIntel')).toBe(true);
        expect(isMacPlatform('macOS')).toBe(true);
        expect(isMacPlatform('iPad')).toBe(true);
        expect(isMacPlatform('iPhone')).toBe(true);
    });

    test('false for Windows, Linux and unknown values', function () {
        expect(isMacPlatform('Win32')).toBe(false);
        expect(isMacPlatform('Windows')).toBe(false);
        expect(isMacPlatform('Linux x86_64')).toBe(false);
        expect(isMacPlatform('')).toBe(false);
        expect(isMacPlatform(undefined)).toBe(false);
    });
});

describe('macKeyLabel', function () {
    test('maps modifier names to Mac symbol plus key name (a bare ⇧ reads like an arrow) and accessible name', function () {
        expect(macKeyLabel('alt')).toEqual({ symbol: '⌥', name: 'Option', label: 'Option' });
        expect(macKeyLabel('shift')).toEqual({ symbol: '⇧', name: 'Shift', label: 'Shift' });
    });

    test('returns null for keys without a Mac-specific label', function () {
        expect(macKeyLabel('r')).toBeNull();
        expect(macKeyLabel('')).toBeNull();
        expect(macKeyLabel(undefined)).toBeNull();
    });
});
