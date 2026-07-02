/**
 * Tests for the accessibility logic of the riot tag tags/thumbnails.tag.
 *
 * Riot tag scripts use riot's method shorthand (`getAriaLabel(canvas, index) {...}`
 * on statement level), which is not valid plain JavaScript. The tag source is
 * therefore compiled with riot-compiler first; the compiled output calls
 * `riot.tag2(name, tmpl, css, attrs, scriptFn)`, from which the script function
 * is captured and executed against a minimal mock tag context. The tag methods
 * are then available on that context for direct unit testing.
 */

const fs = require('fs');
const path = require('path');
const compiler = require('riot-compiler');

const TAG_PATH = path.resolve(__dirname, '../thumbnails.tag');

global.viewerJS = {
    isString: (v) => typeof v === 'string',
    iiif: {
        getValue: (v) => v,
        getId: (v) => (typeof v === 'string' ? v : v && v.id),
    },
};

let scriptFn;

beforeAll(() => {
    const source = fs.readFileSync(TAG_PATH, 'utf-8');
    const compiled = compiler.compile(source);
    const riotStub = {
        tag2: (name, tmpl, css, attrs, fn) => {
            scriptFn = fn;
        },
    };
    new Function('riot', compiled)(riotStub);
});

/** Creates a fresh tag context with the given opts and runs the tag script on it. */
function createTag(opts = {}) {
    const ctx = { opts, refs: {}, on: () => {} };
    scriptFn.call(ctx, opts);
    return ctx;
}

/** Creates a fake riot event as passed to handlers inside an each-loop. */
function createEvent(props = {}) {
    return Object.assign({ item: { canvas: {}, index: 0 }, preventDefault: jest.fn() }, props);
}

const CANVAS_WITH_HOMEPAGE = { homepage: [{ id: 'http://example.com/image/1/' }] };

describe('thumbnails.tag', () => {
    describe('getAriaLabel', () => {
        test('should return the canvas label when present', () => {
            const tag = createTag();
            expect(tag.getAriaLabel({ label: 'First page' }, 0)).toBe('First page');
        });

        test('should trim the canvas label', () => {
            const tag = createTag();
            expect(tag.getAriaLabel({ label: '  First page  ' }, 0)).toBe('First page');
        });

        test.each([
            ['empty label', { label: '' }],
            ['whitespace-only label', { label: '   ' }],
            ['missing label', {}],
        ])('should fall back to the 1-based page number for %s', (_, canvas) => {
            const tag = createTag();
            expect(tag.getAriaLabel(canvas, 4)).toBe('5');
        });

        test('should not throw on non-string label values from malformed manifests', () => {
            const tag = createTag();
            expect(tag.getAriaLabel({ label: { broken: true } }, 2)).toBe('3');
        });

        test('should prefix the page number fallback with the translated opts.msg.page', () => {
            const tag = createTag({ msg: { page: 'Page' } });
            expect(tag.getAriaLabel({}, 4)).toBe('Page 5');
        });
    });

    describe('needsKeyboardFocus', () => {
        test('should be true without href but with actionlistener', () => {
            const tag = createTag({ actionlistener: { next: jest.fn() } });
            expect(tag.needsKeyboardFocus({})).toBe(true);
        });

        test('should be false when the canvas has a homepage link', () => {
            const tag = createTag({ actionlistener: { next: jest.fn() } });
            expect(tag.needsKeyboardFocus(CANVAS_WITH_HOMEPAGE)).toBe(false);
        });

        test('should be false without actionlistener (element would be focusable but dead)', () => {
            const tag = createTag();
            expect(tag.needsKeyboardFocus({})).toBe(false);
        });

        test('should respect a custom opts.link callback', () => {
            const tag = createTag({ actionlistener: { next: jest.fn() }, link: () => undefined });
            expect(tag.needsKeyboardFocus(CANVAS_WITH_HOMEPAGE)).toBe(true);
        });
    });

    describe('handleClickOnImage', () => {
        test('should notify the actionlistener and prevent link navigation', () => {
            const next = jest.fn();
            const tag = createTag({ actionlistener: { next } });
            const event = createEvent({ item: { canvas: {}, index: 2 } });

            tag.handleClickOnImage(event);

            expect(next).toHaveBeenCalledWith({ action: 'clickImage', value: 2 });
            expect(event.preventDefault).toHaveBeenCalled();
            expect(event.preventUpdate).toBe(true);
        });

        test('should keep native navigation without actionlistener', () => {
            const tag = createTag();
            const event = createEvent();

            tag.handleClickOnImage(event);

            expect(event.preventDefault).not.toHaveBeenCalled();
            expect(event.preventUpdate).toBe(true);
        });

        test.each([['ctrlKey'], ['metaKey'], ['shiftKey']])('should keep the browser default for %s-modified clicks (open in new tab)', (modifier) => {
            const next = jest.fn();
            const tag = createTag({ actionlistener: { next } });
            const event = createEvent({ [modifier]: true });

            tag.handleClickOnImage(event);

            expect(next).not.toHaveBeenCalled();
            expect(event.preventDefault).not.toHaveBeenCalled();
            expect(event.preventUpdate).toBe(true);
        });
    });

    describe('handleKeydownOnImage', () => {
        test('should activate a link without href on Enter', () => {
            const next = jest.fn();
            const tag = createTag({ actionlistener: { next } });
            const event = createEvent({ key: 'Enter', item: { canvas: {}, index: 3 } });

            tag.handleKeydownOnImage(event);

            expect(event.preventDefault).toHaveBeenCalled();
            expect(next).toHaveBeenCalledWith({ action: 'clickImage', value: 3 });
        });

        test('should leave links with a real href to native Enter handling', () => {
            const next = jest.fn();
            const tag = createTag({ actionlistener: { next } });
            const event = createEvent({ key: 'Enter', item: { canvas: CANVAS_WITH_HOMEPAGE, index: 0 } });

            tag.handleKeydownOnImage(event);

            expect(event.preventDefault).not.toHaveBeenCalled();
            expect(next).not.toHaveBeenCalled();
            expect(event.preventUpdate).toBe(true);
        });

        test('should not activate on Space (link semantics: Enter only)', () => {
            const next = jest.fn();
            const tag = createTag({ actionlistener: { next } });
            const event = createEvent({ key: ' ' });

            tag.handleKeydownOnImage(event);

            expect(event.preventDefault).not.toHaveBeenCalled();
            expect(next).not.toHaveBeenCalled();
            expect(event.preventUpdate).toBe(true);
        });

        test('should not block other keys like Tab', () => {
            const tag = createTag({ actionlistener: { next: jest.fn() } });
            const event = createEvent({ key: 'Tab' });

            tag.handleKeydownOnImage(event);

            expect(event.preventDefault).not.toHaveBeenCalled();
            expect(event.preventUpdate).toBe(true);
        });

        test('should always prevent the riot auto-update', () => {
            const tag = createTag({ actionlistener: { next: jest.fn() } });
            const event = createEvent({ key: 'Enter' });

            tag.handleKeydownOnImage(event);

            expect(event.preventUpdate).toBe(true);
        });
    });
});
