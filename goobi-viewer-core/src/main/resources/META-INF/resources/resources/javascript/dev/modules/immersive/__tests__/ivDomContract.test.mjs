/** DOM contract between the immersive JS and the XHTML includes: ids, data-* selectors and data-label-* reads must exist, nothing unused. */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

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
const jsFiles = ['shared', 'data', 'engine', 'ui', '.'].flatMap((dir) =>
    fs
        .readdirSync(path.join(JS_ROOT, dir))
        .filter((f) => f.endsWith('.mjs'))
        .map((f) => path.join(JS_ROOT, dir, f))
);
const js = readAll(jsFiles);
const xhtml = readAll(XHTML_FILES);
const kebab = (s) => s.replace(/[A-Z]/g, (c) => '-' + c.toLowerCase());
const matches = (src, re, group = 1) => Array.from(src.matchAll(re), (m) => m[group]);

const xhtmlIds = new Set(matches(xhtml, /\bid="([^"#{]+)"/g));
const xhtmlAttrs = Array.from(xhtml.matchAll(/(data-[a-z-]+)(?:="([^"]*)")?/g), (m) => ({ name: m[1], value: m[2] || '' }));
const xhtmlAttrNames = new Set(xhtmlAttrs.map((a) => a.name));
const xhtmlAttrPairs = new Set(xhtmlAttrs.filter((a) => a.value).map((a) => `${a.name}=${a.value}`));
// Attributes the JS writes itself live on generated elements and are not part of the markup contract.
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
