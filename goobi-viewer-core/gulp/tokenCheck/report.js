/**
 * Token check — the theme findings as an HTML page: what to do, where, and the code to copy.
 * `npm run check:tokens` writes it to target/token-check/<theme>.html and prints the link.
 */

const fs = require('fs');
const os = require('os');
const path = require('path');
const { MODULE_ROOT } = require('./constants');
const { themeSteps, stepFiles } = require('./steps');

const REPORT_DIR = path.join(MODULE_ROOT, 'target', 'token-check');

const esc = (text) => String(text ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);

const COPY_ICON =
    '<svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true"><rect x="5.5" y="5.5" width="9" height="9" rx="1.5" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M10.5 5.5V3a1.5 1.5 0 0 0-1.5-1.5H3A1.5 1.5 0 0 0 1.5 3v6A1.5 1.5 0 0 0 3 10.5h2.5" fill="none" stroke="currentColor" stroke-width="1.5"/></svg>';
const copyButton = (text, label) => `<button type="button" class="copy" data-copy="${esc(text)}" aria-label="${esc(label)}" title="${esc(label)}">${COPY_ICON}</button>`;
const block = (inner, copy, label) => `<div class="block">${copy ? copyButton(copy, label) : ''}${inner}</div>`;
const doneBox = (key, inner) => `<label class="check"><input type="checkbox" data-done="${esc(key)}">${inner}</label>`;

function renderPaste(rows) {
    const line = (r) => {
        if (r.text === '') {
            return '';
        }
        const kind = r.sign !== '+' ? 'ctx' : /^\s*\/\* -{3,} .* -{3,} \*\/$/.test(r.text) ? 'sec' : /^\s*\/\*.*\*\/$/.test(r.text) ? 'lbl' : 'ins';
        return `<span class="${kind}">${esc(r.text)}</span>`;
    };
    return `<pre class="code" tabindex="0">${rows.map(line).join('\n')}</pre>`;
}

/**
 * The edited lines of one file as a diff: line number, "−" a line as it stands, "+" as it reads
 * afterwards. Consecutive new lines form a hunk with its own copy button, shown when the hunk is
 * hovered. The gutter is not selectable, so a selection copies clean code.
 *
 * @param {string} file the file, for the button labels
 * @param {Array<{n: number|null, sign: string|null, text: string, note: string|null}>} rows from stepFiles, without the lines to paste
 * @returns {string} HTML
 */
function renderDiff(file, rows) {
    const kind = (r) => (r.sign === '+' ? 'ins' : r.sign === '-' ? 'del' : r.sign === '…' ? 'gap' : 'ctx');
    const sign = { '+': '+', '-': '−', '…': '', ' ': '' };
    const row = (r, extra = '') =>
        `<tr class="${kind(r)}"><td class="n">${r.n ?? ''}</td><td class="s">${sign[r.sign] ?? ''}</td><td class="c">${r.sign === '…' ? '… ' : ''}${esc(r.text)}${extra}</td></tr>`;
    const show = rows.every((r) => r.sign === ' ' || r.sign === '…');
    const bodies = [];
    let plain = [];
    const flush = () => {
        if (plain.length) {
            bodies.push(`<tbody>${plain.join('')}</tbody>`);
            plain = [];
        }
    };
    for (let i = 0; i < rows.length; i++) {
        if (rows[i].sign !== '+') {
            plain.push(row(rows[i]));
            continue;
        }
        flush();
        let j = i;
        while (j < rows.length && rows[j].sign === '+') {
            j++;
        }
        const hunk = rows.slice(i, j);
        const copy = copyButton(hunk.map((h) => h.text).join('\n'), `Copy the new ${file} ${hunk.length === 1 ? 'line' : 'lines'}`);
        bodies.push(`<tbody class="hunk">${hunk.map((h, k) => row(h, k === 0 ? copy : '')).join('')}</tbody>`);
        i = j - 1;
    }
    flush();
    return `<table class="diff${show ? ' show' : ''}"><colgroup><col class="c-n"><col class="c-s"><col></colgroup>${bodies.join('')}</table>`;
}

function renderFile(step, { file, tag, rows }) {
    const edited = rows.some((r) => r.n !== null || r.sign === null);
    const split = edited ? rows.findIndex((r) => r.n === null && r.sign === ' ') : 0;
    const diff = split < 0 ? rows : rows.slice(0, split);
    const paste = split < 0 ? [] : rows.slice(split);
    const added = (list) =>
        list
            .filter((r) => r.sign === '+')
            .map((r) => r.text)
            .join('\n');
    const body = [diff.length ? block(renderDiff(file, diff)) : '', paste.length ? block(renderPaste(paste), added(paste), `Copy the lines to paste into ${file}`) : '']
        .filter(Boolean)
        .join('\n');
    if (!body) {
        return '';
    }
    return `<section class="file" data-file="${esc(file)}">
<h4 class="file-head">${doneBox(`${step.group}|${file}`, `<code class="path">${esc(file)}</code>`)}${tag ? `<span class="tag">${esc(tag)}</span>` : ''}</h4>
<div class="file-body">
${body}
</div>
</section>`;
}

const shown = (f) => f.changed || (f.changes ?? []).some((c) => c.block) || !(f.code ?? []).every((b) => b.rows.length > 0 && b.rows.every((r) => r.sign === ' '));

function renderStep(step, kind) {
    const files = stepFiles({ ...step, list: step.list.filter(shown) });
    const rendered = files.map((f) => renderFile(step, f)).filter(Boolean);
    const speaks = files.some((f) => f.rows.some((r) => r.sign === '+' || r.sign === '-'));
    return `<section class="step" id="step-${step.step}" data-kind="${kind}" data-group="${esc(step.group)}">
<h3 class="step-head">${doneBox(step.group, `<span class="no">${step.step}</span><span class="title">${esc(step.group)}</span>`)}${rendered.length > 1 ? `<span class="count">${rendered.length} files</span>` : ''}</h3>
${!speaks && step.said ? `<p class="said">${esc(step.said)}</p>` : ''}
<div class="step-body">
${rendered.join('\n')}
</div>
</section>`;
}

const STYLE = `
:root { color-scheme: light; --bg: #fff; --fg: #1c1e21; --muted: #6b7078; --line: #e3e5e8; --rule: #c9ccd1; --head: #f6f7f8; --del-bg: #fdecec; --del-fg: #8a1c1c; --ins-bg: #e5f6ea; --ins-fg: #14532d; --accent: #1d4ed8; --req: #b45309; --full: #1d4ed8; --tag: #fff1cc; --tag-fg: #6b4a00; }
* { box-sizing: border-box; }
body { margin: 0; padding: 0 clamp(16px, 2.5vw, 40px) 6rem; background: var(--bg); color: var(--fg); font: 14px/1.5 system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif; overflow-x: hidden; }
h1, h2, h3, h4 { margin: 0; font-weight: 600; }
p { margin: 0; }
code, pre { font: 12.5px/1.6 ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
.sr { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
[hidden] { display: none !important; }
:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }

/* HEAD */
.top { display: flex; align-items: baseline; gap: 1rem; flex-wrap: wrap; padding: 1.5rem 0 0.75rem; }
.top h1 { font-size: 1.35rem; }
.top h1 .theme { color: var(--muted); font-weight: 500; }
.top .summary { color: var(--muted); }
.top .when { margin-left: auto; color: var(--muted); font-size: 0.85rem; }
.cmd code { display: block; padding: 0.5rem 3rem 0.5rem 0.8rem; background: var(--head); border: 1px solid var(--line); border-radius: 6px; white-space: pre-wrap; overflow-wrap: anywhere; }


/* CHECKS AND COPY, as task lists and code blocks have them */
.check { display: inline-flex; align-items: center; gap: 0.6rem; cursor: pointer; }
.check input { width: 15px; height: 15px; margin: 0; accent-color: var(--ins-fg); cursor: pointer; }
.block { position: relative; margin-top: 0.4rem; }
.block > .copy { position: absolute; top: 0.3rem; right: 0.3rem; z-index: 1; opacity: 0; transition: opacity 0.12s; }
.block:hover > .copy, .block > .copy:focus-visible, .block > .copy.done, .block.cmd > .copy { opacity: 1; }
button.copy { display: inline-grid; place-items: center; width: 1.9rem; height: 1.9rem; padding: 0; border: 1px solid var(--rule); border-radius: 6px; background: var(--bg); color: var(--muted); cursor: pointer; }
button.copy:hover { color: var(--fg); }
button.copy.done { color: var(--ins-fg); border-color: var(--ins-fg); font: 700 13px/1 system-ui, sans-serif; }

/* SECTIONS AND STEPS */
.part { margin-top: 2.25rem; }
.part h2 { font-size: 0.8rem; letter-spacing: 0.1em; text-transform: uppercase; padding-bottom: 0.4rem; border-bottom: 1px solid var(--rule); }
.part[data-kind='todo'] h2 { color: var(--req); }
.part[data-kind='full'] h2 { color: var(--full); }
.part h2 .about { margin-left: 0.8rem; color: var(--muted); font-weight: 400; letter-spacing: 0; text-transform: none; }
.step { margin-top: 2rem; }
.step-head { display: flex; align-items: center; gap: 0.7rem; font-size: 1.3rem; }
.no { font-weight: 700; font-variant-numeric: tabular-nums; }
.title { font-weight: 700; }
.count { color: var(--muted); font-size: 0.9rem; font-weight: 400; }
.step.done .title, .step.done .count, .step.done .no { color: var(--muted); text-decoration: line-through; }
.step.done .step-body, .step.done .said { display: none; }
.said { margin: 0.35rem 0 0 2rem; color: var(--muted); max-width: 110ch; }

/* FILES */
.file { margin: 1rem 0 0 2rem; }
.file-head { display: flex; align-items: center; gap: 0.7rem; min-height: 2rem; padding: 0.2rem 0; border-bottom: 1px solid var(--rule); font-weight: 600; }
.file.done .file-head { border-bottom-color: var(--line); }
.file.done .path { color: var(--muted); text-decoration: line-through; }
.file.done .file-body { display: none; }
.tag { font-size: 0.7rem; font-weight: 600; letter-spacing: 0.05em; text-transform: uppercase; background: var(--tag); color: var(--tag-fg); border-radius: 4px; padding: 0.05rem 0.4rem; }

/* DIFF: one grid for every file */
table.diff { border-collapse: collapse; width: 100%; table-layout: fixed; }
col.c-n { width: 3.2rem; }
col.c-s { width: 1.4rem; }
.diff td { padding: 0.1rem 0.4rem; vertical-align: top; font: 12.5px/1.7 ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
.diff td.n, .diff td.s { user-select: none; color: var(--muted); }
.diff td.n { padding-left: 0; text-align: right; font-variant-numeric: tabular-nums; }
.diff td.s { text-align: center; }
.diff td.c { white-space: pre-wrap; overflow-wrap: anywhere; border-radius: 3px; }
.diff tr.del td.c { background: var(--del-bg); color: var(--del-fg); }
.diff tr.del td.s { color: var(--del-fg); }
.diff tr.ins td.c { background: var(--ins-bg); color: var(--ins-fg); }
.diff tr.ins td.s { color: var(--ins-fg); }
.diff tr.ctx td.c, .diff tr.gap td.c { color: var(--muted); }
.diff.show tr.ctx td.c { color: var(--fg); }
.diff tbody.hunk td.c { position: relative; padding-right: 2.4rem; }
.diff tbody.hunk .copy { position: absolute; top: 0.15rem; right: 0.3rem; width: 1.6rem; height: 1.6rem; opacity: 0; transition: opacity 0.12s; }
.diff tbody.hunk:hover .copy, .diff tbody.hunk .copy:focus-visible, .diff tbody.hunk .copy.done { opacity: 1; }

/* CODE TO PASTE */
pre.code { margin: 0; padding: 0.7rem 3rem 0.7rem 0.9rem; background: var(--head); border-left: 3px solid var(--ins-fg); border-radius: 0 6px 6px 0; white-space: pre-wrap; overflow-wrap: anywhere; }
pre .ctx { color: var(--muted); user-select: none; }
pre .sec { font-weight: 700; }
.part[data-kind='todo'] pre .sec { color: var(--req); }
.part[data-kind='full'] pre .sec { color: var(--full); }
pre .lbl { color: var(--muted); font-weight: 600; }
@media (max-width: 720px) { .file { margin-left: 0; } }
`;

const SCRIPT = `
var status = document.getElementById('status');
var theme = document.body.dataset.theme;
var store = { get: function (k) { try { return localStorage.getItem('token-check:' + theme + ':' + k) === '1'; } catch (e) { return false; } }, set: function (k, v) { try { if (v) localStorage.setItem('token-check:' + theme + ':' + k, '1'); else localStorage.removeItem('token-check:' + theme + ':' + k); } catch (e) {} } };
function fallback(text) { var t = document.createElement('textarea'); t.value = text; t.setAttribute('readonly', ''); t.style.position = 'fixed'; t.style.opacity = '0'; document.body.append(t); t.select(); document.execCommand('copy'); t.remove(); }
function copied(b) { var icon = b.innerHTML; b.classList.add('done'); b.textContent = '✓'; status.textContent = 'Copied to the clipboard'; setTimeout(function () { b.classList.remove('done'); b.innerHTML = icon; }, 1500); }
document.addEventListener('click', function (e) {
    var b = e.target.closest('button.copy');
    if (!b || b.classList.contains('done')) return;
    var text = b.dataset.copy;
    if (navigator.clipboard && navigator.clipboard.writeText) navigator.clipboard.writeText(text).then(function () { copied(b); }, function () { fallback(text); copied(b); });
    else { fallback(text); copied(b); }
});
document.querySelectorAll('.step').forEach(function (s) {
    var files = Array.prototype.slice.call(s.querySelectorAll('.file input[data-done]'));
    var box = s.querySelector('.step-head input[data-done]');
    function setStep(done) { s.classList.toggle('done', done); box.checked = done; store.set(s.dataset.group, done); }
    setStep(store.get(s.dataset.group));
    box.addEventListener('change', function () { setStep(box.checked); });
    files.forEach(function (fb) {
        var f = fb.closest('.file');
        fb.checked = store.get(fb.dataset.done);
        f.classList.toggle('done', fb.checked);
        fb.addEventListener('change', function () {
            f.classList.toggle('done', fb.checked);
            store.set(fb.dataset.done, fb.checked);
            if (files.length > 1 && files.every(function (b) { return b.checked; })) setStep(true);
        });
    });
});
`;

/**
 * The report page for one theme.
 *
 * @param {{theme: {name: string, path: string, findings: Array}}} result from runTokenCheck
 * @param {{command?: string|null, generated?: Date}} [meta] the command that reruns the check, when it ran
 * @returns {string} HTML
 */
function renderHtml({ theme }, { command = null, generated = new Date() } = {}) {
    const { todo, full, sections } = themeSteps(theme.findings ?? []);
    const when = generated.toLocaleString('sv-SE', { dateStyle: 'short', timeStyle: 'short' });
    const kindOf = (s) => (s.title.startsWith('REQUIRED') ? 'todo' : 'full');
    const idOf = (s) => (kindOf(s) === 'todo' ? 'required' : 'full');
    const body = sections
        .map(
            (s) =>
                `<section class="part" id="${idOf(s)}" data-kind="${kindOf(s)}"><h2>${esc(s.title)}<span class="about">${esc(s.about)}</span></h2>\n${s.steps.map((x) => renderStep(x, kindOf(s))).join('\n')}</section>`
        )
        .join('\n');
    const summary =
        todo.length + full.length ? `<strong>${todo.length} required</strong> · ${full.length} for the complete token migration` : 'nothing to do: the theme fits the core';
    return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Token check — ${esc(theme.name)}</title>
<style>${STYLE}</style>
</head>
<body data-theme="${esc(theme.name)}">
<header class="top">
<h1>Token check <span class="theme">${esc(theme.name)}</span></h1>
<p class="summary">${summary}</p>
<p class="when">${esc(when)}</p>
</header>
${command ? `<div class="block cmd">${copyButton(command, 'Copy the command')}<code>${esc(command)}</code></div>` : ''}
${sections.length ? `<main>\n${body}\n</main>` : ''}
<p id="status" class="sr" role="status" aria-live="polite"></p>
<script>${SCRIPT}</script>
</body>
</html>
`;
}

function shellPath(p) {
    if (/[\s'"$`\\]/.test(p)) {
        return `'${p.replace(/'/g, `'\\''`)}'`;
    }
    const home = os.homedir();
    return p === home || p.startsWith(home + path.sep) ? `~${p.slice(home.length)}` : p;
}

/**
 * Writes the report for a checked theme and returns its path.
 *
 * @param {{theme: {name: string, path: string, findings: Array}}} result from runTokenCheck
 * @param {{dir?: string, moduleRoot?: string}} [options] where the file goes; the core module the command runs in
 * @returns {string} the file written
 */
function writeReport(result, { dir = REPORT_DIR, moduleRoot = MODULE_ROOT } = {}) {
    const lessRoot = path.resolve(moduleRoot, result.theme.path);
    const command = `GV_THEME_LESS_ROOT=${shellPath(lessRoot)} npm --prefix ${shellPath(moduleRoot)} run check:tokens`;
    const file = path.join(dir, `${result.theme.name}.html`);
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(file, renderHtml(result, { command }));
    return file;
}

module.exports = { REPORT_DIR, COPY_ICON, renderHtml, writeReport, shellPath };
