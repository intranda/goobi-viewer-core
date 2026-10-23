/** DOM contract between the immersive JS and the XHTML includes: ids, data-* selectors and data-label-* reads must exist, nothing unused. */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import less from 'less';
import { PANEL_SHORTCUT_IDS } from '../ui/ivPanelsWiring.mjs';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const JS_ROOT = path.resolve(HERE, '..');
const RESOURCES = path.resolve(HERE, '../../../../..');
const XHTML_FILES = [
    ...fs
        .readdirSync(path.join(RESOURCES, 'includes/immersive'))
        .filter((f) => f.endsWith('.xhtml'))
        .map((f) => path.join(RESOURCES, 'includes/immersive', f)),
    path.join(RESOURCES, 'components/widgets/widget_toc.xhtml'),
];

const readAll = (files) => files.map((f) => fs.readFileSync(f, 'utf8')).join('\n');
const listModules = (dir) =>
    fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
        if (entry.isDirectory()) return entry.name === '__tests__' ? [] : listModules(path.join(dir, entry.name));
        return entry.name.endsWith('.mjs') ? [path.join(dir, entry.name)] : [];
    });
const kebab = (s) => s.replace(/[A-Z]/g, (c) => '-' + c.toLowerCase());
const matches = (src, re, group = 1) => Array.from(src.matchAll(re), (m) => m[group]);
const jsFiles = listModules(JS_ROOT);
const xhtml = readAll(XHTML_FILES);
// inline page scripts read data-label-* too (normdata init in iv_layout.xhtml)
const js = readAll(jsFiles) + '\n' + matches(xhtml, /<script\b(?![^>]*\/>)[^>]*>([\s\S]*?)<\/script>/g).join('\n');

const xhtmlIds = new Set(matches(xhtml, /\bid="([^"#{]+)"/g));
const xhtmlAttrs = Array.from(xhtml.matchAll(/(data-[a-z-]+)(?:="([^"]*)")?/g), (m) => ({ name: m[1], value: m[2] || '' }));
const xhtmlAttrNames = new Set(xhtmlAttrs.map((a) => a.name));
const xhtmlAttrPairs = new Set(xhtmlAttrs.filter((a) => a.value).map((a) => `${a.name}=${a.value}`));
const jsOwned = new Set(matches(js, /\.dataset\.([a-zA-Z]+)\s*=[^=]/g).map((n) => 'data-' + kebab(n)));

test('every id the JS looks up is rendered by the immersive includes', () => {
    const ids = new Set([...matches(js, /getElementById\('([A-Za-z][\w-]*)'\)/g), ...matches(js, /#(immersive[A-Za-z0-9]*)/g)]);
    expect([...ids].filter((id) => !xhtmlIds.has(id))).toEqual([]);
});

test('every data-attribute selector in the JS matches markup, including the value', () => {
    const selectors = Array.from(js.matchAll(/\[(data-[a-z-]+)(?:="([^"]*)")?\]/g), (m) => ({ name: m[1], value: m[2] || '' })).filter(
        (s) => !s.value.includes('${') && !jsOwned.has(s.name)
    );
    const missing = selectors
        .filter((s) => !xhtmlAttrNames.has(s.name) || (s.value && !xhtmlAttrPairs.has(`${s.name}=${s.value}`)))
        .map((s) => (s.value ? `[${s.name}="${s.value}"]` : `[${s.name}]`));
    expect(missing).toEqual([]);
});

test('every data-label-* the JS reads is provided by the markup', () => {
    const labels = new Set(matches(js, /(?:dataset|labels)\.(label[A-Z]\w*)/g).map((n) => 'data-' + kebab(n)));
    expect([...labels].filter((a) => !xhtmlAttrNames.has(a))).toEqual([]);
});

test('the markup carries no data-immersive-* or data-label-* attribute the JS never reads', () => {
    const readImmersive = new Set([...matches(js, /(data-immersive-[a-z-]+)/g), ...matches(js, /dataset\.immersive([A-Z]\w*)/g).map((n) => 'data-immersive' + kebab(n))]);
    const readLabels = new Set(matches(js, /(?:dataset|labels)\.(label[A-Z]\w*)/g).map((n) => 'data-' + kebab(n)));
    const unused = [...xhtmlAttrNames].filter((a) => (a.startsWith('data-immersive-') && !readImmersive.has(a)) || (a.startsWith('data-label-') && !readLabels.has(a)));
    expect(unused).toEqual([]);
});

test('a visible label is never replaced by an aria-label (WCAG 2.5.3 label in name)', () => {
    const labelled = new Set(matches(xhtml, /<label[^>]*\bfor="([^"]+)"/g));
    const inputs = Array.from(xhtml.matchAll(/<(?:input|select|textarea)\b([^>]*)>/g), (m) => m[1])
        .filter((attrs) => /\baria-label=/.test(attrs) && labelled.has((attrs.match(/\bid="([^"]+)"/) || [])[1]))
        .map((attrs) => attrs.match(/\bid="([^"]+)"/)[1]);
    const buttons = Array.from(xhtml.matchAll(/<button\b([^>]*)>([\s\S]*?)<\/button>/g))
        .filter((m) => /\baria-label=/.test(m[1]) && m[2].replace(/<[^>]+>/g, '').trim())
        .map((m) => m[2].replace(/<[^>]+>/g, '').trim());
    expect([...inputs, ...buttons]).toEqual([]);
});

test('every loader image in a status region carries a text alternative', () => {
    const silent = Array.from(xhtml.matchAll(/<div\b[^>]*role="status"[^>]*>\s*<img\b([^>]*)>/g), (m) => m[1]).filter((attrs) => /\balt=""/.test(attrs));
    expect(silent).toEqual([]);
});

test('Alt+digit order equals the rail button order and the shortcuts-modal rows', () => {
    const toolbar = fs.readFileSync(path.join(RESOURCES, 'includes/immersive/iv_toolbar_left.xhtml'), 'utf8');
    const modal = fs.readFileSync(path.join(RESOURCES, 'includes/immersive/iv_shortcuts_modal.xhtml'), 'utf8');
    const rail = Array.from(toolbar.matchAll(/data-immersive-panel="(\w+)"[^>]*title="#\{msg\.(\w+)\}"/g), (m) => ({ id: m[1], msg: m[2] }));
    expect(rail.map((r) => r.id)).toEqual(PANEL_SHORTCUT_IDS);
    const altRows = modal
        .split('<tr>')
        .filter((row) => row.includes('data-key="alt"'))
        .map((row) => ({ msg: row.match(/#\{msg\.(\w+)\}/)[1], digit: Number(row.match(/<kbd>(\d)<\/kbd>/)[1]) }));
    expect(altRows).toEqual(rail.map((r, i) => ({ msg: r.msg, digit: i + 1 })));
});

/**
 * Compiles one core view stylesheet on its own: every LESS variable and every
 * `.mx-*()` mixin it references is stubbed, so only its own rules are under test.
 */
function compileView(file) {
    const src = fs.readFileSync(file, 'utf8');
    const atRules = new Set(['media', 'import', 'supports', 'keyframes', 'font-face', 'charset', 'page', 'container']);
    const vars = new Set(matches(src, /@([a-zA-Z][\w-]*)/g).filter((v) => !atRules.has(v)));
    const mixins = new Set(matches(src, /\.(mx-[\w-]+)\(/g));
    const stubs = [...[...vars].map((v) => `@${v}: 0;`), ...[...mixins].map((m) => `.${m}() {}`)].join('\n');
    let css = '';
    less.render(`${stubs}\n${src}`, { filename: file }, (err, out) => {
        if (err) throw err;
        css = out.css;
    });
    return Array.from(css.matchAll(/([^{}]+)\{([^{}]*)\}/g), (m) => ({ selector: m[1].trim(), body: m[2].replace(/\s+/g, '') }));
}

/**
 * The fullscreen page is a bare <body> -- no wrapper carries a height or a flex
 * context -- so everything inside the viewer (absolutely positioned chrome, a
 * canvas at height:100%) collapses unless the layout mode sizes itself.
 */
describe('LESS contract: the fullscreen layout brings its own height', () => {
    test('the mode fills the viewport itself and hands the room to the viewer', () => {
        const rules = compileView(path.join(RESOURCES, 'css/less/views/immersive/ivStage.less'));
        const mode = rules.find((r) => r.selector === '.immersive--fullscreen');
        expect(mode).toBeDefined();
        expect(mode.body).toMatch(/height:100vh/);
        expect(mode.body).toMatch(/display:flex/);
        const viewer = rules.find((r) => r.selector === '.immersive--fullscreen .immersive__viewer');
        expect(viewer).toBeDefined();
        expect(viewer.body).toMatch(/flex:1/);
    });
});

describe('LESS contract: margin notes stay hidden while annotations are switched off', () => {
    test('no rule displays an .immersive__anno-note outside the -annotations-on state', () => {
        const rules = compileView(path.join(RESOURCES, 'css/less/views/immersive/ivAnnotations.less'));
        expect(rules.length).toBeGreaterThan(20);
        const showing = rules.filter((r) => /__anno-note(?![\w-])/.test(r.selector) && /display:(?!none)/.test(r.body));
        expect(showing.length).toBeGreaterThan(0);
        const unguarded = showing.filter((r) => !r.selector.includes('-annotations-on')).map((r) => r.selector);
        expect(unguarded).toEqual([]);
    });
});
