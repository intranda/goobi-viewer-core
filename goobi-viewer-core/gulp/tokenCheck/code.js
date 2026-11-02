/**
 * Token check — the code of a finding: the lines as they stand and as they read afterwards, and where a token line goes in the token layer.
 */

const escapeRegExp = (text) => text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

const UNTOUCHED = /\/\/.*$|\/\*.*?\*\/|(?<!~)"(?:\\.|[^"\\])*"|(?<!~)'(?:\\.|[^'\\])*'|\burl\((?:[^()]|\([^()]*\))*\)/gi;

const uniqueLines = (changes) => changes.filter((c, i) => changes.findIndex((o) => o.file === c.file && o.line === c.line) === i);

function replaceToken(text, [from, to]) {
    const name = from.startsWith('var(') ? `var\\(\\s*${escapeRegExp(from.slice(4))}` : escapeRegExp(from);
    const pattern = new RegExp(`(?<![\\w-])${name}(?![\\w-])`, 'g');
    let out = '';
    let last = 0;
    for (const m of text.matchAll(UNTOUCHED)) {
        out += text.slice(last, m.index).replace(pattern, () => to) + m[0];
        last = m.index + m[0].length;
    }
    return out + text.slice(last).replace(pattern, () => to);
}

function spanOf(code, line, block) {
    const first = line - 1;
    if (block) {
        let depth = 0;
        let opened = false;
        for (let i = first; i < code.length; i++) {
            for (const c of code[i].replace(/@\{[\w-]+\}/g, '')) {
                if (c === '{') {
                    depth++;
                    opened = true;
                } else if (c === '}') {
                    depth--;
                }
            }
            if (opened && depth <= 0) {
                return [first, i];
            }
        }
        return [first, first];
    }
    for (let i = first; i < Math.min(code.length, first + 8); i++) {
        if (code[i].includes(';') || (i === first && code[i].includes('}'))) {
            return [first, i];
        }
        if (i > first && /[{}]/.test(code[i])) {
            return [first, i - 1];
        }
    }
    return [first, first];
}

/**
 * One change as rows of code: "-" a line as it stands, "+" as it reads afterwards, " " a line that
 * stays, "…" lines left out. `n` is the line number in the file, null for a new line.
 *
 * @param {object} change file and line, and what happens there: add (new lines, into a mixin), insert (after the line), before/after, remove, block (remove|unwrap|show), after (the new line), replace ([from, to] pairs); none of them shows the line
 * @param {{raw: string[], code: string[]}|null} source the file's lines, as written and without comments
 * @returns {{file: string, line: number, tag: string|null, into: string|null, rows: Array<{n: number|null, sign: string, text: string}>}}
 */
function codeBlock(change, source) {
    const { file, line, tag = null, into = null, section = null, label = null, order = null } = change;
    const block = { file, line, tag, into, section, label, order, rows: [] };
    const added = (texts) => texts.map((text) => ({ n: null, sign: '+', text }));
    if (change.add) {
        return { ...block, line: 0, rows: added(change.add) };
    }
    if (!line || !source) {
        return block;
    }
    if (change.before !== undefined) {
        return { ...block, rows: [{ n: line, sign: '-', text: change.before }, ...added([change.after])] };
    }
    if (change.insert) {
        return { ...block, rows: [{ n: line, sign: ' ', text: source.raw[line - 1].trim() }, ...added(change.insert)] };
    }
    const [first, last] = spanOf(source.code, line, Boolean(change.block));
    const expanded = source.raw.slice(first, last + 1).map((l) => l.replace(/\t/g, '    ').trimEnd());
    const indent = Math.min(...expanded.filter((l) => l.trim()).map((l) => l.match(/^ */)[0].length));
    const lines = expanded.map((text, i) => ({ n: first + i + 1, text: text.slice(indent) }));
    const rows = (sign, list) => list.map((l) => ({ n: l.n, sign, text: l.text }));
    const ends = (sign, what) => [...rows(sign, [lines[0]]), { n: null, sign: '…', text: `${lines.length - 2} ${what}` }, ...rows(sign, [lines[lines.length - 1]])];
    if (change.block === 'remove') {
        return { ...block, rows: lines.length > 4 ? ends('-', 'more lines') : rows('-', lines) };
    }
    if (change.block === 'unwrap' && lines.length > 2) {
        return { ...block, rows: ends('-', 'lines stay') };
    }
    if (change.block) {
        return { ...block, rows: rows(' ', lines) };
    }
    if (change.remove) {
        return { ...block, rows: rows('-', lines) };
    }
    if (change.after !== undefined) {
        return { ...block, rows: [...rows('-', lines), ...added([change.after])] };
    }
    return {
        ...block,
        rows: lines.flatMap((l) => {
            const after = (change.replace ?? []).reduce(replaceToken, l.text);
            return after === l.text ? rows(' ', [l]) : [...rows('-', [l]), ...added([after])];
        }),
    };
}

function tokenLayout(source) {
    const layout = new Map();
    const labels = [];
    let mixin = null;
    let label = null;
    for (const raw of source.split('\n')) {
        const line = raw.trim();
        const header = line.match(/^(\.[\w-]+\(\))\s*\{$/);
        if (header) {
            mixin = header[1];
            label = null;
            continue;
        }
        if (line === '}') {
            mixin = null;
            continue;
        }
        const comment = line.match(/^\/\*\s*(?:-+\s*)?([^*-][^*]*?)\s*(?:-+\s*)?\*\/$/);
        if (comment) {
            label = comment[1];
            if (!labels.includes(label)) {
                labels.push(label);
            }
            continue;
        }
        const token = line.match(/^(--[\w-]+)\s*:/);
        if (token && mixin && !mixin.includes('dark') && !layout.has(token[1])) {
            layout.set(token[1], { mixin, label, order: label ? labels.indexOf(label) : null });
        }
    }
    return layout;
}

module.exports = {
    escapeRegExp,
    UNTOUCHED,
    uniqueLines,
    replaceToken,
    spanOf,
    codeBlock,
    tokenLayout,
};
