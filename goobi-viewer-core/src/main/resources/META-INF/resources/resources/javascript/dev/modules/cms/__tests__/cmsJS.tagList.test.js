/**
 * Unit tests for cmsJS.tagList — a jQuery-and-DOM-driven tag list widget.
 *
 * Ported from the deleted Jasmine spec
 * `tests/spec/cms.tagList-spec.js`.
 *
 * jsdom provides window/document; we bind real jQuery to it and load the
 * module via require(). cmsJS.tagList uses the `cmsJS || {}` IIFE fallback
 * idiom so it loads cleanly in Node without the Crowdsourcing-style
 * indirect-eval workaround.
 */
// jQuery + jsdom are wired up by jest-setup-browser.js.
// cmsJS.tagList wires the in-list <input> to jQuery UI's autocomplete plugin
// for tag suggestions. The plugin is not part of the bare jquery package; in
// the original Karma run it was loaded as a global script. The autocomplete
// behaviour is out of scope for these unit tests, so we stub it as a no-op.
global.$.fn.autocomplete = function () {
    return this;
};

const cmsJS = require('../cmsJS.tagList.js');

describe('cmsJS.tagList', function () {
    let config;

    beforeEach(function () {
        // Re-create the form in body before each test so state doesn't leak.
        document.body.innerHTML =
            '<form id="tagListForm">' +
            '  <input type="text" id="inputField"/>' +
            '  <input type="text" id="inputFieldWithTags" value=\'["tag1","tag2","tag3"]\'/>' +
            '  <ul id="tagList"></ul>' +
            '  <div id="targetDiv"></div>' +
            '</form>';

        config = {
            inputFieldId: 'inputField',
            tagListId: 'tagList',
            autoSuggestUrl: '/http:viewer/rest/contentAssist/mediaTags/',
        };
    });

    afterEach(function () {
        // Wiping the body is enough to release jQuery's data/event bindings
        // for this list. Calling cmsJS.tagList.close() would NPE on tests
        // that didn't call init().
        document.body.innerHTML = '';
    });

    test('should expose a tagList namespace', function () {
        expect(cmsJS.tagList).toBeDefined();
    });

    describe('init', function () {
        test('should bind the configured input and list elements', function () {
            cmsJS.tagList.init(config);
            expect(cmsJS.tagList.$inputField.attr('id')).toBe(config.inputFieldId);
            expect(cmsJS.tagList.$tagListElement.attr('id')).toBe(config.tagListId);
            expect(cmsJS.tagList.autoSuggestUrl).toBe(config.autoSuggestUrl);
        });

        test('should render one list item per JSON tag found in the bound input field', function () {
            config.inputFieldId = 'inputFieldWithTags';
            cmsJS.tagList.init(config);

            const $tagLis = cmsJS.tagList.$tagListElement.children('[id$=_item]');
            expect($tagLis.length).toBe(3);

            const $inputLi = cmsJS.tagList.$tagListElement.parent().find('[id$=_inputField]');
            expect($inputLi.length).toBe(1);
        });
    });

    describe('list item readers (input pre-populated with three tags)', function () {
        beforeEach(function () {
            config.inputFieldId = 'inputFieldWithTags';
            cmsJS.tagList.init(config);
        });

        test('getTags() should return a jQuery collection containing all three items', function () {
            expect(cmsJS.tagList.getTags().length).toBe(3);
        });

        test('getTagValues() should return all three values in order', function () {
            const values = cmsJS.tagList.getTagValues();
            expect(values.length).toBe(3);
            expect(values[1]).toBe('tag2');
        });

        test('getTag() should resolve known values and return undefined for unknown ones', function () {
            expect(cmsJS.tagList.getTag('tag2')).toBeDefined();
            expect(cmsJS.tagList.getTag('tag4')).toBeUndefined();
        });

        test('getValue() should return the text of a tag DOM node', function () {
            expect(cmsJS.tagList.getValue(cmsJS.tagList.getTags()[1])).toBe('tag2');
        });
    });

    describe('addTag', function () {
        test('should append a new <li> with the value of the inline input on `change`', function () {
            cmsJS.tagList.init(config);
            const $input = cmsJS.tagList.$tagListElement.parent().find('[id$=_inputField]');
            expect($input.length).toBe(1);

            // Empty list initially.
            expect(cmsJS.tagList.$tagListElement.children('[id$=_item]').length).toBe(0);

            $input.val('tag42');
            $input.trigger('change');

            const $tagLis = cmsJS.tagList.$tagListElement.children('[id$=_item]');
            expect($tagLis.length).toBe(1);
            expect($tagLis.text()).toBe('tag42');
            // The bound hidden input is updated with the JSON-encoded list.
            expect(cmsJS.tagList.$inputField.val()).toBe('["tag42"]');
        });

        test('should not add a duplicate when the tag is already in the list', function () {
            config.inputFieldId = 'inputFieldWithTags';
            cmsJS.tagList.init(config);
            expect(cmsJS.tagList.$tagListElement.children('[id$=_item]').length).toBe(3);

            cmsJS.tagList.addTag('tag2');

            expect(cmsJS.tagList.$tagListElement.children('[id$=_item]').length).toBe(3);
        });
    });

    describe('deleteTag (via the rendered tag-terminator click)', function () {
        test('should remove the clicked item and re-serialise the remaining tags into the input', function () {
            config.inputFieldId = 'inputFieldWithTags';
            cmsJS.tagList.init(config);

            let $tagLis = cmsJS.tagList.$tagListElement.children('[id$=_item]');
            expect($tagLis.length).toBe(3);

            const $secondTagTerminator = $tagLis.eq(1).find('.tag-terminator');
            expect($secondTagTerminator.length).toBe(1);
            $secondTagTerminator.trigger('click');

            $tagLis = cmsJS.tagList.$tagListElement.children('[id$=_item]');
            expect($tagLis.length).toBe(2);
            expect($tagLis.eq(0).text()).toBe('tag1');
            expect($tagLis.eq(1).text()).toBe('tag3');
            expect(cmsJS.tagList.$inputField.val()).toBe('["tag1","tag3"]');
        });
    });
});
