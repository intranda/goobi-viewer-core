/**
 * Unit tests for viewerJS.userComments.
 *
 * The init() binds direct (non-delegated) click handlers, so each
 * test sets up its own DOM fixture and calls init() afterwards.
 */
const viewerJS = require('../viewerJS.userComments.js');

describe('userComments init: prefilled-textarea cleanup', () => {
    test('clears a prefilled #userCommentAdd textarea', () => {
        document.body.innerHTML = '<textarea id="userCommentAdd">leftover</textarea>';
        viewerJS.userComments.init();
        expect(document.getElementById('userCommentAdd').value).toBe('');
    });

    test('does nothing when #userCommentAdd is empty', () => {
        document.body.innerHTML = '<textarea id="userCommentAdd"></textarea>';
        viewerJS.userComments.init();
        expect(document.getElementById('userCommentAdd').value).toBe('');
    });
});

describe('userComments init: edit / cancel toggle', () => {
    function setupCommentDom() {
        document.body.innerHTML = `
            <div class="user-comments__comment-content">
                <div class="user-comments__comment-content-options">
                    <span class="user-comments__comment-content-options-edit in">
                        <button data-edit="comment">Edit</button>
                    </span>
                    <span class="user-comments__comment-content-options-cancel">
                        <button data-edit="cancel">Cancel</button>
                    </span>
                    <span class="user-comments__comment-content-options-save">
                        <button data-edit="save">Save</button>
                    </span>
                </div>
                <div class="user-comments__comment-content-options-text in">read view</div>
                <div class="user-comments__comment-content-options-text-edit">
                    <textarea>edit view</textarea>
                </div>
            </div>`;
    }

    test('clicking the edit button hides the read view and shows the edit view', () => {
        setupCommentDom();
        viewerJS.userComments.init();

        $('[data-edit="comment"]').trigger('click');

        expect($('.user-comments__comment-content-options-edit').hasClass('in')).toBe(false);
        expect($('.user-comments__comment-content-options-cancel').hasClass('in')).toBe(true);
        expect($('.user-comments__comment-content-options-save').hasClass('in')).toBe(true);
        expect($('.user-comments__comment-content-options-text').hasClass('in')).toBe(false);
        expect($('.user-comments__comment-content-options-text-edit').hasClass('in')).toBe(true);
    });

    test('clicking the cancel button reverts to the read view', () => {
        setupCommentDom();
        viewerJS.userComments.init();
        $('[data-edit="comment"]').trigger('click');

        $('[data-edit="cancel"]').trigger('click');

        expect($('.user-comments__comment-content-options-edit').hasClass('in')).toBe(true);
        expect($('.user-comments__comment-content-options-cancel').hasClass('in')).toBe(false);
        expect($('.user-comments__comment-content-options-save').hasClass('in')).toBe(false);
        expect($('.user-comments__comment-content-options-text').hasClass('in')).toBe(true);
        expect($('.user-comments__comment-content-options-text-edit').hasClass('in')).toBe(false);
    });

    test('clicking the save button shows the loader inside the parent comment-content', () => {
        document.body.innerHTML = `
            <div class="user-comments__comment-content">
                <button data-edit="save">Save</button>
                <div class="user-comments__comment-content-loader" style="display:none">L</div>
            </div>`;
        viewerJS.userComments.init();

        $('[data-edit="save"]').trigger('click');

        expect(document.querySelector('.user-comments__comment-content-loader').style.display).not.toBe('none');
    });
});
