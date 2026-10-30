/** Unit tests for the HTML report of the token check (gulp/tokenReport.js). */
const fs = require('fs');
const os = require('os');
const path = require('path');
const { checkTree } = require('..');
const { COPY_ICON, renderHtml, writeReport, shellPath } = require('../report');

const files = (obj) => new Map(Object.entries(obj));
const CORE = {
    'misc/variables.less': '@white: #fff;',
    'misc/palette.less': '.gv-palette() {\n    /* BRAND */\n    --color-brand: #06c;\n    --color-ink: #111;\n}',
    'misc/tokens.less':
        '.gv-base() {\n    /* TYPOGRAPHY */\n    --font-size-body: 1.4rem;\n}\n.gv-semantic-light() {\n    /* TEXT */\n    --fg-default: var(--color-ink);\n}\n.gv-semantic-dark() {\n    --fg-default: #fff;\n}\n' +
        ':root { .gv-palette(); .gv-base(); .gv-semantic-light(); }\n.color-theme-dark { .gv-semantic-dark(); }',
};
const OLD_THEME = {
    'constructor.less': "@import 'misc/variables.less';",
    'misc/variables.less': ':root {\n    --fg-general: #333;\n    --datepicker-header-bg: #06c;\n}',
    'components/x.less': '.x {\n    color: #333;\n    font-size: 14px;\n    line-height: 1.2em;\n}',
};
const result = (findings) => ({ core: [], theme: { status: 'checked', name: 'old', path: '../old/css/less', findings } });
const findings = () => checkTree(files(CORE), files(OLD_THEME)).theme;
const btn = (text, label) => `<button type="button" class="copy" data-copy="${text}" aria-label="${label}" title="${label}">${COPY_ICON}</button>`;
const check = (key, inner) => `<label class="check"><input type="checkbox" data-done="${key}">${inner}</label>`;

describe('token report', () => {
    test('renders the head, the command and the sections with one step per group, each with its check box', () => {
        const html = renderHtml(result(findings()), { command: 'GV_THEME_LESS_ROOT=~/old/css/less npm run check:tokens', generated: new Date(2026, 9, 29, 14, 36) });
        expect(html).toContain('<title>Token check — old</title>');
        expect(html).toContain('<p class="summary"><strong>3 required</strong> · 5 for the complete token migration</p>');
        expect(html).toContain('<p class="when">2026-10-29 14:36</p>');
        expect(html).toContain(
            `<div class="block cmd">${btn('GV_THEME_LESS_ROOT=~/old/css/less npm run check:tokens', 'Copy the command')}<code>GV_THEME_LESS_ROOT=~/old/css/less npm run check:tokens</code></div>`
        );
        expect(html).toContain(
            '<section class="part" id="required" data-kind="todo"><h2>REQUIRED FOR THE NEW CORE<span class="about">the theme works and looks as before</span></h2>'
        );
        expect(html).toContain('<section class="part" id="full" data-kind="full"><h2>COMPLETE TOKEN MIGRATION<span class="about">');
        expect(html).toContain(
            `<section class="step" id="step-1" data-kind="todo" data-group="token layer missing">\n<h3 class="step-head">${check('token layer missing', '<span class="no">1</span><span class="title">token layer missing</span>')}<span class="count">3 files</span></h3>`
        );
        expect(html).not.toContain('id="filter"');
        expect(html).not.toContain('class="why"');
        expect(html).not.toContain('@media (prefers-color-scheme');
    });

    test('shows edited lines as a diff; consecutive new lines form a hunk with its own copy button; every file has a check box', () => {
        const html = renderHtml(result(findings()));
        expect(html).toContain(`<h4 class="file-head">${check('token layer missing|constructor.less', '<code class="path">constructor.less</code>')}</h4>`);
        expect(html).toContain(
            '<div class="block"><table class="diff"><colgroup><col class="c-n"><col class="c-s"><col></colgroup>' +
                '<tbody><tr class="ctx"><td class="n">1</td><td class="s"></td><td class="c">@import &#39;misc/variables.less&#39;;</td></tr></tbody>' +
                `<tbody class="hunk"><tr class="ins"><td class="n"></td><td class="s">+</td><td class="c">@import &#39;misc/palette.less&#39;;${btn('@import &#39;misc/palette.less&#39;;\n@import &#39;misc/tokens.less&#39;;', 'Copy the new constructor.less lines')}</td></tr>` +
                '<tr class="ins"><td class="n"></td><td class="s">+</td><td class="c">@import &#39;misc/tokens.less&#39;;</td></tr></tbody></table></div>'
        );
        expect(html).toContain(
            '<div class="block"><table class="diff"><colgroup><col class="c-n"><col class="c-s"><col></colgroup><tbody><tr class="del"><td class="n">2</td><td class="s">−</td><td class="c">--fg-general: #333;</td></tr><tr class="del"><td class="n">3</td><td class="s">−</td><td class="c">--datepicker-header-bg: #06c;</td></tr></tbody></table></div>'
        );
        expect(html).toContain(
            `<tbody><tr class="del"><td class="n">3</td><td class="s">−</td><td class="c">font-size: 14px;</td></tr></tbody><tbody class="hunk"><tr class="ins"><td class="n"></td><td class="s">+</td><td class="c">font-size: var(--font-size-body);${btn('font-size: var(--font-size-body);', 'Copy the new components/x.less line')}</td></tr></tbody>`
        );
    });

    test('leaves out what has nothing to change; a step whose code shows no change says what to do', () => {
        const html = renderHtml(result(findings()));
        expect(html).not.toContain('line-height');
        expect(html).toContain(
            `<h3 class="step-head">${check('dark mode, only if wanted', '<span class="no">6</span><span class="title">dark mode, only if wanted</span>')}</h3>\n<p class="said">add .mx-theme-semantic-dark() with the same tokens as light`
        );
        expect(html).not.toContain('<p class="said">read a text role');
        const dark = {
            ...OLD_THEME,
            'misc/tokens.less':
                '.mx-theme-semantic-light() { --fg-ink: #333; }\n.mx-theme-semantic-dark() { --fg-ink: #eee; }\n:root { .mx-theme-semantic-light(); }\n.color-theme-dark { .mx-theme-semantic-dark(); }',
            'components/y.less': '.y {\n    color: #333;\n}\n.color-theme-dark .y {\n    color: #eee;\n    border: 0;\n    margin: 0;\n    padding: 0;\n}',
        };
        const branch = renderHtml(result(checkTree(files(CORE), files(dark)).theme));
        expect(branch).toContain(
            '<p class="said">the core switches modes through tokens now: bind what differs as a theme token in .mx-theme-semantic-light() and .mx-theme-semantic-dark() in misc/tokens.less, read the token here, then delete the branch</p>'
        );
        expect(branch).toContain(
            '<table class="diff show"><colgroup><col class="c-n"><col class="c-s"><col></colgroup><tbody><tr class="ctx"><td class="n">4</td><td class="s"></td><td class="c">.color-theme-dark .y {</td></tr><tr class="ctx"><td class="n">5</td><td class="s"></td><td class="c">    color: #eee;</td></tr>'
        );
        expect(branch).toContain('<tr class="ctx"><td class="n">9</td><td class="s"></td><td class="c">}</td></tr></tbody></table>');
    });

    test('shows new lines as a block to paste with the comments of the core, and one button for the whole block', () => {
        const html = renderHtml(result(findings()));
        expect(html).toContain(
            `<h4 class="file-head">${check('token layer missing|misc/palette.less', '<code class="path">misc/palette.less</code>')}<span class="tag">new file</span></h4>`
        );
        expect(html).toContain(
            `<div class="block">${btn('.mx-theme-palette() {\n    /* ---------- CORE PALETTE ---------- */\n\n    /* BRAND */\n    --color-brand: &lt;brand colour&gt;;\n}', 'Copy the lines to paste into misc/palette.less')}<pre class="code" tabindex="0"><span class="ins">.mx-theme-palette() {</span>\n<span class="sec">    /* ---------- CORE PALETTE ---------- */</span>\n\n<span class="lbl">    /* BRAND */</span>\n<span class="ins">    --color-brand: &lt;brand colour&gt;;</span>\n<span class="ins">}</span></pre></div>`
        );
        expect(html).toContain(
            `${btn('    /* ---------- CORE OVERRIDES ---------- */\n\n    /* TEXT */\n    --fg-default: #333;', 'Copy the lines to paste into misc/tokens.less')}<pre class="code" tabindex="0"><span class="ctx">.mx-theme-semantic-light() {</span>\n<span class="sec">    /* ---------- CORE OVERRIDES ---------- */</span>\n\n<span class="lbl">    /* TEXT */</span>\n<span class="ins">    --fg-default: #333;</span>\n<span class="ctx">}</span></pre>`
        );
        expect(html).toContain('<span class="sec">    /* ---------- THEME PALETTE ---------- */</span>\n\n<span class="ins">    --color-theme-333333: #333333;</span>');
    });

    test('renders a file with edited lines and new lines as a diff block and a paste block', () => {
        const finding = {
            file: 'theme/misc/tokens.less',
            line: 2,
            what: '--a',
            fix: 'old core token name — rename it',
            kind: 'todo',
            group: 'old core token name',
            code: [
                {
                    file: 'theme/misc/tokens.less',
                    line: 2,
                    tag: null,
                    into: null,
                    rows: [
                        { n: 2, sign: '-', text: '--a: 1;' },
                        { n: null, sign: '+', text: '--b: 1;' },
                    ],
                },
                {
                    file: 'theme/misc/tokens.less',
                    line: 0,
                    tag: null,
                    into: '.mx-theme-semantic-light()',
                    section: 'CORE OVERRIDES',
                    label: 'TEXT',
                    rows: [{ n: null, sign: '+', text: '--c: 2;' }],
                },
            ],
            changed: true,
        };
        const html = renderHtml(result([finding]));
        expect(html).toContain(
            `<div class="block"><table class="diff"><colgroup><col class="c-n"><col class="c-s"><col></colgroup><tbody><tr class="del"><td class="n">2</td><td class="s">−</td><td class="c">--a: 1;</td></tr></tbody><tbody class="hunk"><tr class="ins"><td class="n"></td><td class="s">+</td><td class="c">--b: 1;${btn('--b: 1;', 'Copy the new misc/tokens.less line')}</td></tr></tbody></table></div>\n` +
                `<div class="block">${btn('    /* ---------- CORE OVERRIDES ---------- */\n\n    /* TEXT */\n    --c: 2;', 'Copy the lines to paste into misc/tokens.less')}<pre class="code" tabindex="0"><span class="ctx">.mx-theme-semantic-light() {</span>\n<span class="sec">    /* ---------- CORE OVERRIDES ---------- */</span>\n\n<span class="lbl">    /* TEXT */</span>\n<span class="ins">    --c: 2;</span>\n<span class="ctx">}</span></pre></div>`
        );
    });

    test('escapes what the theme writes and says when there is nothing to do', () => {
        const rows = [
            { n: 1, sign: '-', text: 'content: "<b>"' },
            { n: null, sign: '+', text: 'content: "<b>"' },
        ];
        const html = renderHtml(
            result([
                {
                    file: 'theme/components/x.less',
                    line: 1,
                    what: 'content: "<b>"',
                    fix: 'colour written into the component — read a role',
                    kind: 'full',
                    group: 'colours in components',
                    code: [{ file: 'theme/components/x.less', line: 1, rows }],
                    changed: true,
                },
            ])
        );
        expect(html).toContain('content: &quot;&lt;b&gt;&quot;');
        expect(html).not.toContain('<b>');
        const clean = renderHtml(result([]));
        expect(clean).toContain('nothing to do: the theme fits the core');
        expect(clean).not.toContain('<main>');
    });

    test('writes the report next to the build output and names the command that reruns the check', () => {
        const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'token-check-'));
        const file = writeReport(result(findings()), { dir, moduleRoot: '/core' });
        expect(file).toBe(path.join(dir, 'old.html'));
        expect(fs.readFileSync(file, 'utf8')).toContain('GV_THEME_LESS_ROOT=/old/css/less npm --prefix /core run check:tokens');
        fs.rmSync(dir, { recursive: true });
    });

    test('writes paths for the shell: the home directory as ~, quotes only where needed', () => {
        expect(shellPath(path.join(os.homedir(), 'code', 'theme'))).toBe('~/code/theme');
        expect(shellPath('/opt/themes/x')).toBe('/opt/themes/x');
        expect(shellPath(path.join(os.homedir(), 'my themes', "o'neill"))).toBe(`'${os.homedir()}/my themes/o'\\''neill'`);
        expect(shellPath('/opt/my themes')).toBe("'/opt/my themes'");
    });
});
