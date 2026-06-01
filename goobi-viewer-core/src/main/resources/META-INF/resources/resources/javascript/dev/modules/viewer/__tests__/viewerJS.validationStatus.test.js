/**
 * Unit tests for viewerJS.validationStatus.
 *
 * The init() guards on the presence of `.-validation-mark` in the DOM.
 * When present, it subscribes to viewer.jsfAjax.success and applies a
 * severity class to elements following the validation message.
 *
 * We supply a minimal Subject implementation for jsfAjax.success so we
 * can drive the subscriber from inside each test.
 */
const viewerJS = require('../viewerJS.validationStatus.js');

// Minimal hand-rolled rxjs Subject — push values via .next().
function makeSubject() {
    const subs = [];
    return {
        subscribe(handler) {
            subs.push(handler);
        },
        next(value) {
            subs.forEach((h) => h(value));
        },
        _subs: subs,
    };
}

let success;

beforeEach(() => {
    success = makeSubject();
    viewerJS.jsfAjax = { success };
});

describe('viewerJS.validationStatus.init', () => {
    test('does not subscribe when the DOM has no .-validation-mark', () => {
        document.body.innerHTML = '<div id="empty"></div>';
        viewerJS.validationStatus.init();
        // No subscribers registered when there is nothing to validate.
        expect(success._subs).toHaveLength(0);
    });

    test('subscribes to jsfAjax.success when at least one .-validation-mark is present', () => {
        document.body.innerHTML = '<span class="-validation-mark"></span>';
        viewerJS.validationStatus.init();
        expect(success._subs).toHaveLength(1);
    });

    test('on success, copies the message severity class onto the following input/mark elements', () => {
        document.body.innerHTML = `
            <div>
                <span class="-validation-message error">err!</span>
                <input class="-validation-input" />
                <span class="-validation-mark"></span>
            </div>`;
        viewerJS.validationStatus.init();

        success.next();

        // The 'error' class (everything after stripping '-validation-message')
        // is applied to the following input + mark.
        expect(document.querySelector('.-validation-input').classList.contains('error')).toBe(true);
        expect(document.querySelector('.-validation-mark').classList.contains('error')).toBe(true);
    });

    test('also tags tinymce containers as -validation-input on success', () => {
        document.body.innerHTML = `
            <div>
                <div class="tox tox-tinymce"></div>
                <span class="-validation-mark"></span>
            </div>`;
        viewerJS.validationStatus.init();
        success.next();
        expect(document.querySelector('.tox.tox-tinymce').classList.contains('-validation-input')).toBe(true);
    });
});
