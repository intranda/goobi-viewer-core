/**
 * Token check — reading LESS: comments and strings stripped, declarations, reads and static constants per file.
 */

const fs = require('fs');
const path = require('path');
const { AT_USE, COLOR_LITERAL, DECLARATION, READ, STATIC } = require('./constants');

function stripComments(source) {
    let out = '';
    let quote = null;
    let paren = 0;
    for (let i = 0; i < source.length; i++) {
        const c = source[i];
        const d = source[i + 1];
        if (quote) {
            out += c;
            if (c === '\\') {
                out += d === undefined ? '' : d;
                i++;
            } else if (c === quote) {
                quote = null;
            }
            continue;
        }
        if (c === '"' || c === "'") {
            quote = c;
            out += c;
            continue;
        }
        if (c === '/' && d === '*') {
            const end = source.indexOf('*/', i + 2);
            const stop = end === -1 ? source.length : end + 2;
            out += source.slice(i, stop).replace(/[^\n]/g, '');
            i = stop - 1;
            continue;
        }
        if (c === '/' && d === '/' && paren === 0) {
            const end = source.indexOf('\n', i);
            i = (end === -1 ? source.length : end) - 1;
            continue;
        }
        if (c === '(') {
            paren++;
        } else if (c === ')') {
            paren = Math.max(0, paren - 1);
        }
        out += c;
    }
    return out;
}

function withoutStrings(text) {
    return text.replace(/(~?)(["'])(?:\\[\s\S]|(?!\2)[^\\])*?\2/g, (match, escaped) => (escaped ? match.slice(2, -1) : ' '));
}

function colorValue(text) {
    return withoutStrings(text).replace(/\burl\((?:[^()]|\([^()]*\))*\)/gi, ' ');
}

function staticColorNames(parsed) {
    const definitions = parsed?.statics || [];
    const names = new Set(definitions.filter((d) => COLOR_LITERAL.test(colorValue(d.value))).map((d) => d.name));
    let previousSize;
    do {
        previousSize = names.size;
        for (const d of definitions) {
            if ([...colorValue(d.value).matchAll(AT_USE)].some((m) => names.has(m[1]))) {
                names.add(d.name);
            }
        }
    } while (names.size !== previousSize);
    return names;
}

function collectReads(text, line, stack, into, owner = null) {
    for (const m of withoutStrings(text).matchAll(READ)) {
        into.push({ name: m[1], fallback: m[2] === ',', line, stack, owner });
    }
}

/**
 * Minimal LESS reader: statements, block headers and their nesting.
 *
 * @param {string} source LESS source.
 * @returns {{declarations: Array, properties: Array, reads: Array, statics: Array, blocks: Array}}
 */
function parseLess(source) {
    const text = stripComments(source);
    const result = { declarations: [], properties: [], reads: [], statics: [], blocks: [] };
    const stack = [];
    let buf = '';
    let start = null;
    let line = 1;
    let quote = null;
    let paren = 0;
    let interpolation = false;
    let detached = 0;

    const headers = () => stack.map((b) => b.header);

    const statement = () => {
        const stmt = buf.trim();
        const at = start;
        buf = '';
        start = null;
        if (!stmt) {
            return;
        }
        const context = headers();
        const property = stmt.match(/^((?:--)?[\w@{}-]+)(?:\+_?)?\s*:\s*([\s\S]*)$/);
        if (property && (!property[1].startsWith('@') || property[1].startsWith('@{'))) {
            result.properties.push({ name: property[1], value: property[2].trim(), line: at, stack: context });
        }
        const declaration = stmt.match(DECLARATION);
        if (declaration) {
            result.declarations.push({ name: declaration[1], value: declaration[2].trim(), line: at, stack: context });
            collectReads(declaration[2], at, context, result.reads, declaration[1]);
            return;
        }
        const stat = stmt.match(STATIC);
        if (stat && context.length === 0) {
            result.statics.push({ name: stat[1], value: stmt.slice(stmt.indexOf(':') + 1).trim(), line: at });
            collectReads(stmt, at, context, result.reads, `@${stat[1]}`);
            return;
        }
        collectReads(stmt, at, context, result.reads, property?.[1] ?? null);
    };

    for (let i = 0; i < text.length; i++) {
        const c = text[i];
        if (c === '\n') {
            line++;
        }
        if (quote) {
            buf += c;
            if (c === quote && text[i - 1] !== '\\') {
                quote = null;
            }
            continue;
        }
        if (c === '"' || c === "'") {
            quote = c;
            if (start === null) {
                start = line;
            }
            buf += c;
            continue;
        }
        if (c === '{' && text[i - 1] === '@') {
            interpolation = true;
            buf += c;
            continue;
        }
        if (c === '}' && interpolation) {
            interpolation = false;
            buf += c;
            continue;
        }
        if (c === '(' && paren === 0 && /^\s{0,15}\{/.test(text.substr(i + 1, 16))) {
            detached++;
            continue;
        }
        if (c === ')' && paren === 0 && detached > 0 && buf.trim() === '') {
            detached--;
            continue;
        }
        if (c === '(') {
            paren++;
        } else if (c === ')') {
            paren = Math.max(0, paren - 1);
        }
        if (paren === 0 && c === '{') {
            const header = buf.trim().replace(/\s+/g, ' ');
            const block = { header, line: start === null ? line : start, stack: headers() };
            collectReads(header, block.line, block.stack, result.reads);
            result.blocks.push(block);
            stack.push(block);
            buf = '';
            start = null;
            continue;
        }
        if (paren === 0 && c === '}') {
            statement();
            stack.pop();
            continue;
        }
        if (paren === 0 && c === ';') {
            statement();
            continue;
        }
        if (start === null && !/\s/.test(c)) {
            start = line;
        }
        buf += c;
    }
    statement();
    return result;
}

const parseAll = (files) => new Map([...files].map(([file, source]) => [file, parseLess(source)]));

function readLessTree(root) {
    const out = new Map();
    const walk = (dir) => {
        for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
            const full = path.join(dir, entry.name);
            if (entry.isDirectory()) {
                walk(full);
            } else if (entry.name.endsWith('.less')) {
                out.set(path.relative(root, full).split(path.sep).join('/'), fs.readFileSync(full, 'utf8'));
            }
        }
    };
    walk(root);
    return out;
}

module.exports = {
    stripComments,
    withoutStrings,
    colorValue,
    staticColorNames,
    collectReads,
    parseLess,
    parseAll,
    readLessTree,
};
