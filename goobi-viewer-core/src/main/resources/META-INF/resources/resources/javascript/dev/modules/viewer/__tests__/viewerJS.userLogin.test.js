/**
 * Unit tests for viewerJS.userLogin.
 *
 * init() seeds sessionStorage["userComments"] when missing, then
 * binds delegated click handlers on body for login / logout / open
 * / close of the various account sub-views. We exercise:
 *   - the sessionStorage seeding (set/skip),
 *   - the [data-toggle="login"] click flow with and without
 *     data-target (which routes through _setUserCommentsStatus
 *     / _unsetUserCommentsStatus),
 *   - the close pathway (#userLogin > .fa-times),
 *   - the data-open/data-close="retrieve-account" / "create-account"
 *     visibility toggles.
 *
 * jQuery animations are forced synchronous so style assertions land.
 */
$.fx.off = true;
const viewerJS = require('../viewerJS.userLogin.js');

beforeEach(() => {
    sessionStorage.clear();
    document.documentElement.className = '';
    document.body.innerHTML = `
        <div id="userLogin">
            <span class="fa-times">x</span>
        </div>
        <button data-toggle="login" data-target="#commentsTarget">Login</button>
        <button data-toggle="login" id="loginNoTarget">Login plain</button>

        <div id="userLoginSelectLoginWrapper">A</div>
        <div id="loginType">B</div>
        <div id="loginTypeRetrieveAccount" style="display:none">RetrievePane</div>
        <div id="loginTypeCreateAccount" style="display:none">CreatePane</div>
        <div id="loginTypeExternal" style="display:none">External</div>
        <div id="userLoginOpenId">Open</div>
        <div id="userLoginCreateAccount">Create</div>

        <button data-open="retrieve-account">Open R</button>
        <button data-close="retrieve-account">Close R</button>
        <button data-open="create-account">Open C</button>
        <button data-close="create-account">Close C</button>

        <input id="userEMailToRetrieve" />
        <input class="user-login-modal__create-account-email-input" />
        <span class="user-login-modal__header-title">Title</span>
        <span class="user-login-modal__header-title-create-account" style="display:none">Create</span>
        <button class="user-login-modal__create-account-submit">Submit</button>

        <div id="createAccountAcceptTerms">
            <input type="radio" name="terms" />
            <input type="radio" name="terms" />
        </div>`;

    viewerJS.userLogin.init();
});

describe('sessionStorage seeding', () => {
    // init() seeds an empty userComments object when the key is missing.
    // It then runs _jumpToComments() which (in the absence of
    // #userCommentAdd in the DOM) calls _unsetUserCommentsStatus and
    // overwrites the value with {set: false, target: ""}. Both tests
    // therefore observe that final shape; we still distinguish the two
    // paths by whether _jumpToComments saw the seeding or a pre-set value.
    test('produces a {set:false, target:""} entry when the key was missing', () => {
        sessionStorage.removeItem('userComments');
        viewerJS.userLogin.init();
        expect(JSON.parse(sessionStorage.getItem('userComments'))).toEqual({ set: false, target: '' });
    });

    test('preserves the entry when a #userCommentAdd target is in the DOM', () => {
        // With #userCommentAdd present, _jumpToComments enters the
        // truthy branch and does NOT clear the entry — it just sets
        // location.hash and focuses the textarea.
        document.body.innerHTML += '<textarea id="userCommentAdd"></textarea>';
        sessionStorage.setItem('userComments', JSON.stringify({ set: true, target: '#commentsTarget' }));
        viewerJS.userLogin.init();
        expect(JSON.parse(sessionStorage.getItem('userComments'))).toEqual({
            set: true,
            target: '#commentsTarget',
        });
    });
});

describe('[data-toggle="login"] click', () => {
    test('with data-target: stores the target in sessionStorage and activates #userLogin', () => {
        $('[data-toggle="login"][data-target="#commentsTarget"]').trigger('click');

        expect(document.getElementById('userLogin').classList.contains('active')).toBe(true);
        expect(document.documentElement.classList.contains('no-overflow')).toBe(true);
        const stored = JSON.parse(sessionStorage.getItem('userComments'));
        expect(stored.set).toBe(true);
        expect(stored.target).toBe('#commentsTarget');
    });

    test('without data-target: clears the comments status and still activates #userLogin', () => {
        // Pre-seed a comments target so we can verify it is wiped.
        sessionStorage.setItem('userComments', JSON.stringify({ set: true, target: '#old' }));
        $('#loginNoTarget').trigger('click');

        expect(document.getElementById('userLogin').classList.contains('active')).toBe(true);
        const stored = JSON.parse(sessionStorage.getItem('userComments'));
        expect(stored.set).toBe(false);
        expect(stored.target).toBe('');
    });
});

describe('close click on #userLogin > .fa-times', () => {
    test('removes the active class and the no-overflow lock', () => {
        document.getElementById('userLogin').classList.add('active');
        document.documentElement.classList.add('no-overflow');

        $('#userLogin > .fa-times').trigger('click');

        expect(document.getElementById('userLogin').classList.contains('active')).toBe(false);
        expect(document.documentElement.classList.contains('no-overflow')).toBe(false);
    });
});

describe('retrieve-account / create-account toggles', () => {
    test('[data-open="retrieve-account"] hides the main panes and shows the retrieve pane', () => {
        $('[data-open="retrieve-account"]').trigger('click');

        expect(document.getElementById('userLoginSelectLoginWrapper').style.display).toBe('none');
        expect(document.getElementById('loginType').style.display).toBe('none');
        expect(document.getElementById('userLoginOpenId').style.display).toBe('none');
        expect(document.getElementById('userLoginCreateAccount').style.display).toBe('none');
        expect(document.getElementById('loginTypeRetrieveAccount').style.display).not.toBe('none');
    });

    test('[data-close="retrieve-account"] returns to the main login view', () => {
        // First open it.
        $('[data-open="retrieve-account"]').trigger('click');
        // Then close it.
        $('[data-close="retrieve-account"]').trigger('click');

        expect(document.getElementById('loginTypeRetrieveAccount').style.display).toBe('none');
        expect(document.getElementById('userLoginSelectLoginWrapper').style.display).not.toBe('none');
        expect(document.getElementById('loginType').style.display).not.toBe('none');
    });

    test('[data-open="create-account"] swaps the header title and reveals the create pane', () => {
        $('[data-open="create-account"]').trigger('click');

        expect(document.getElementById('loginTypeCreateAccount').style.display).not.toBe('none');
        expect(document.querySelector('.user-login-modal__header-title').style.display).toBe('none');
        expect(document.querySelector('.user-login-modal__header-title-create-account').style.display).not.toBe('none');
    });
});
