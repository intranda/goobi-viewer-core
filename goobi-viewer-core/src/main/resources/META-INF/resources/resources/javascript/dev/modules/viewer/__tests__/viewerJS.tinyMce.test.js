const viewerJS = require('../viewerJS.tinyMce.js');

describe('viewerJS.tinyMce.getConfig', () => {
    test('sets the mandatory self-hosted license key', () => {
        const config = viewerJS.tinyMce.getConfig({});
        expect(config.license_key).toBe('gpl');
    });

    test('does not include any plugin removed/paywalled in TinyMCE 8', () => {
        const config = viewerJS.tinyMce.getConfig({});
        const plugins = config.plugins.split(/\s+/);
        ['print', 'paste', 'hr', 'template', 'textpattern'].forEach((removed) => {
            expect(plugins).not.toContain(removed);
        });
    });

    test('uses the TinyMCE 6+ toolbar item names, not the removed 5.x ones', () => {
        const config = viewerJS.tinyMce.getConfig({});
        expect(config.toolbar).toMatch(/\bblocks\b/);
        expect(config.toolbar).toMatch(/\bfontsize\b/);
        expect(config.toolbar).not.toMatch(/\bformatselect\b|\bfontsizeselect\b/);
    });

    test('uses font_size_formats, not the removed fontsize_formats key', () => {
        const config = viewerJS.tinyMce.getConfig({});
        expect(config.font_size_formats).toBeDefined();
        expect(config.fontsize_formats).toBeUndefined();
    });

    test('disables paste_data_images, restoring pre-upgrade paste behavior', () => {
        const config = viewerJS.tinyMce.getConfig({});
        expect(config.paste_data_images).toBe(false);
    });

    test('disables convert_unsafe_embeds, since most editor surfaces have no server-side sanitizer', () => {
        const config = viewerJS.tinyMce.getConfig({});
        expect(config.convert_unsafe_embeds).toBe(false);
    });

    test('merges caller overrides on top of the shared defaults without losing the license key', () => {
        const config = viewerJS.tinyMce.getConfig({ height: 999, selector: '#custom' });
        expect(config.height).toBe(999);
        expect(config.selector).toBe('#custom');
        expect(config.license_key).toBe('gpl');
    });
});

describe('viewerJS.tinyMce _defaults.setup (via getConfig)', () => {
    // ed.on('change input paste', ...) is registered as ONE call with a
    // multi-event string (that's how jQuery's plugin-style .on() accepts
    // several event names at once) — this mock keeps that string as the
    // literal key, matching what the real handler does.
    function makeMockEditor(targetElm) {
        const handlers = {};
        return {
            targetElm,
            on: jest.fn((event, handler) => {
                handlers[event] = handler;
            }),
            save: jest.fn(),
            getElement: jest.fn(() => targetElm),
            _handlers: handlers,
        };
    }

    beforeEach(() => {
        // setup() references these two as page-global free variables (set by
        // an inline <script> on specific admin pages, e.g. adminCmsCreatePage);
        // Jest has no such page, so declare them on the global object with a
        // harmless value — otherwise the bare reference throws ReferenceError.
        global.currentPage = 'somePage';
        global.createPageConfig = {
            prevBtn: { attr: jest.fn() },
            prevDescription: { show: jest.fn() },
        };
    });

    test('registers exactly init, "change input paste", and blur handlers', () => {
        const config = viewerJS.tinyMce.getConfig({});
        const ed = makeMockEditor(document.createElement('textarea'));

        config.setup(ed);

        expect(ed.on).toHaveBeenCalledWith('init', expect.any(Function));
        expect(ed.on).toHaveBeenCalledWith('change input paste', expect.any(Function));
        expect(ed.on).toHaveBeenCalledWith('blur', expect.any(Function));
        expect(ed.on).toHaveBeenCalledTimes(3);
    });

    test('the "change input paste" handler saves the editor and notifies the target textarea', () => {
        const config = viewerJS.tinyMce.getConfig({});
        const targetElm = document.createElement('textarea');
        document.body.appendChild(targetElm);
        const ed = makeMockEditor(targetElm);
        config.setup(ed);

        const seenChange = jest.fn();
        $(targetElm).on('change', seenChange);
        ed._handlers['change input paste']({});

        expect(ed.save).toHaveBeenCalled();
        expect(seenChange).toHaveBeenCalled();
    });

    test('on the adminCmsNewPage, the "change input paste" handler disables the prev button', () => {
        global.currentPage = 'adminCmsNewPage';
        const config = viewerJS.tinyMce.getConfig({});
        const ed = makeMockEditor(document.createElement('textarea'));
        config.setup(ed);

        ed._handlers['change input paste']({});

        expect(global.createPageConfig.prevBtn.attr).toHaveBeenCalledWith('disabled', true);
        expect(global.createPageConfig.prevDescription.show).toHaveBeenCalled();
    });

    test('does not throw when currentPage is not defined at all — this is what Task 5 Step 5b guards, and what Task 11 relies on for the OCR editor page, which never declares currentPage', () => {
        delete global.currentPage;
        const config = viewerJS.tinyMce.getConfig({});
        const ed = makeMockEditor(document.createElement('textarea'));
        config.setup(ed);

        expect(() => ed._handlers['change input paste']({})).not.toThrow();
    });

    test('the blur handler blurs the target element — this is the handler an earlier draft of this task would have deleted', () => {
        const config = viewerJS.tinyMce.getConfig({});
        const targetElm = document.createElement('textarea');
        document.body.appendChild(targetElm);
        targetElm.focus();
        const ed = makeMockEditor(targetElm);
        config.setup(ed);

        const seenBlur = jest.fn();
        $(targetElm).on('blur', seenBlur);
        ed._handlers['blur']({});

        expect(seenBlur).toHaveBeenCalled();
    });
});
