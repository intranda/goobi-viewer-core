/**
 * Unit tests for adminJS.codemirror.
 *
 * The source is a thin factory that calls CodeMirror.fromTextArea with
 * a merged config and returns {element, mode, codemirror}. We stub
 * `global.CodeMirror.fromTextArea` to capture the config and verify
 * the merge / extraKeys behavior without pulling in the real library.
 *
 * Indirect eval is needed because the source references a free
 * `adminJS` global to attach `.codemirror` — the standard `require()`
 * path would never expose it.
 */
const fs = require('fs');
const path = require('path');

// CodeMirror.fromTextArea returns an editor; we only need to capture
// the config so tests can assert on it. The returned object is mostly
// a vehicle for the extraKeys callbacks we want to drive.
let lastFromTextAreaCall;
function makeEditor() {
    return {
        getOption: jest.fn((key) => makeEditor._opts[key]),
        setOption: jest.fn((key, value) => {
            makeEditor._opts[key] = value;
        }),
    };
}
makeEditor._opts = {};

global.CodeMirror = {
    fromTextArea: function (element, config) {
        lastFromTextAreaCall = { element, config };
        makeEditor._opts = { ...config };
        return makeEditor();
    },
};

global.adminJS = {};
(0, eval)(fs.readFileSync(path.join(__dirname, '..', 'adminJS.codemirror.js'), 'utf8'));

const codemirror = global.adminJS.codemirror;

beforeEach(() => {
    lastFromTextAreaCall = null;
    makeEditor._opts = {};
});

describe('adminJS.codemirror factory', () => {
    test('returns {element, mode, codemirror} with mode passed through from the merged config', () => {
        const el = document.createElement('textarea');
        const result = codemirror(el, 'xml', false);
        expect(result.element).toBe(el);
        expect(result.mode).toBe('xml');
        expect(result.codemirror).toBeDefined();
    });

    test('forwards the textarea element and a merged config to CodeMirror.fromTextArea', () => {
        const el = document.createElement('textarea');
        codemirror(el, 'properties', false, { theme: 'dracula' });
        expect(lastFromTextAreaCall.element).toBe(el);
        // base config defaults
        expect(lastFromTextAreaCall.config.lineNumbers).toBe(true);
        expect(lastFromTextAreaCall.config.indentUnit).toBe(2);
        expect(lastFromTextAreaCall.config.tabSize).toBe(2);
        // user override wins for fields specified in `config`
        expect(lastFromTextAreaCall.config.theme).toBe('dracula');
        // mode comes from the second positional argument
        expect(lastFromTextAreaCall.config.mode).toBe('properties');
    });

    test('user config does not clobber the extraKeys map (deep merge)', () => {
        const el = document.createElement('textarea');
        codemirror(el, 'xml', false, { extraKeys: { 'Ctrl-Q': 'quit' } });
        // The base extraKeys (F11, Esc, Ctrl-D, Ctrl-S, Ctrl-E) survive,
        // and the user-added Ctrl-Q is layered on top.
        expect(lastFromTextAreaCall.config.extraKeys.F11).toBeDefined();
        expect(lastFromTextAreaCall.config.extraKeys['Ctrl-S']).toBeDefined();
        expect(lastFromTextAreaCall.config.extraKeys['Ctrl-Q']).toBe('quit');
    });
});

describe('extraKeys behavior', () => {
    test('F11 toggles the fullScreen option via setOption/getOption', () => {
        const el = document.createElement('textarea');
        codemirror(el, 'xml', false);
        const cm = makeEditor();
        cm.getOption.mockReturnValue(false);
        // Pull the F11 binding off of the captured config and call it.
        lastFromTextAreaCall.config.extraKeys.F11(cm);
        expect(cm.setOption).toHaveBeenCalledWith('fullScreen', true);
    });

    test('Esc disables fullScreen only when it is currently active', () => {
        const el = document.createElement('textarea');
        codemirror(el, 'xml', false);

        // Case 1: fullScreen is on → Esc turns it off.
        const cmOn = makeEditor();
        cmOn.getOption.mockReturnValue(true);
        lastFromTextAreaCall.config.extraKeys.Esc(cmOn);
        expect(cmOn.setOption).toHaveBeenCalledWith('fullScreen', false);

        // Case 2: fullScreen is off → Esc is a no-op.
        const cmOff = makeEditor();
        cmOff.getOption.mockReturnValue(false);
        lastFromTextAreaCall.config.extraKeys.Esc(cmOff);
        expect(cmOff.setOption).not.toHaveBeenCalled();
    });

    test('Ctrl-D swaps between the default and dracula themes', () => {
        const el = document.createElement('textarea');
        codemirror(el, 'xml', false);

        const cm = makeEditor();
        cm.getOption.mockReturnValueOnce('default');
        lastFromTextAreaCall.config.extraKeys['Ctrl-D'](cm);
        expect(cm.setOption).toHaveBeenCalledWith('theme', 'dracula');

        cm.setOption.mockClear();
        cm.getOption.mockReturnValueOnce('dracula');
        lastFromTextAreaCall.config.extraKeys['Ctrl-D'](cm);
        expect(cm.setOption).toHaveBeenCalledWith('theme', 'default');
    });

    test('Ctrl-S clicks the [data-cm="save"] button when the editor is not read-only', () => {
        document.body.innerHTML = '<button data-cm="save"></button>';
        const clickSpy = jest.spyOn(document.querySelector('[data-cm="save"]'), 'click');
        const el = document.createElement('textarea');
        codemirror(el, 'xml', false);

        lastFromTextAreaCall.config.extraKeys['Ctrl-S']();
        expect(clickSpy).toHaveBeenCalled();
        clickSpy.mockRestore();
    });

    test('Ctrl-S is a no-op when the editor is read-only', () => {
        document.body.innerHTML = '<button data-cm="save"></button>';
        const clickSpy = jest.spyOn(document.querySelector('[data-cm="save"]'), 'click');
        const el = document.createElement('textarea');
        codemirror(el, 'xml', /*readonly=*/ true);

        lastFromTextAreaCall.config.extraKeys['Ctrl-S']();
        expect(clickSpy).not.toHaveBeenCalled();
        clickSpy.mockRestore();
    });
});
