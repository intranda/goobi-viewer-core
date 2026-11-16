/*
 * Every inline <script> in the core XHTML must be valid JavaScript once Facelets has replaced its EL.
 * A script that does not parse never runs in the browser, and nothing else reports it.
 */
const fs = require('fs');
const path = require('path');
const { parse } = require('@babel/parser');

const RESOURCES = path.join(__dirname, '../../src/main/resources/META-INF/resources');
const SKIP_DIRS = new Set(['node_modules', 'libs', 'dist']);
const SCRIPT = /<script\b((?:[^>/]|\/(?!>))*)>([\s\S]*?)<\/script>/g;
const FACELETS_TAG = /<(ui|c|h|f|o|util|viewerComponent):[A-Za-z]/;

const decode = (s) => s.replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&apos;/g, "'").replace(/&amp;/g, '&');

function maskEL(code) {
    let out = '';
    let i = 0;
    while (i < code.length) {
        if ((code[i] === '#' || code[i] === '$') && code[i + 1] === '{' && code[i - 1] !== '\\') {
            let j = i + 2;
            let depth = 1;
            let quote = null;
            while (j < code.length && depth > 0) {
                const c = code[j];
                if (quote) {
                    if (c === '\\') {
                        j += 2;
                        continue;
                    }
                    if (c === quote) quote = null;
                } else if (c === "'" || c === '"') quote = c;
                else if (c === '{') depth++;
                else if (c === '}') depth--;
                j++;
            }
            out += '__EL__';
            i = j;
            continue;
        }
        out += code[i++];
    }
    return out;
}

function brokenScripts(source) {
    const found = [];
    let m;
    SCRIPT.lastIndex = 0;
    while ((m = SCRIPT.exec(source))) {
        const [, attrs, body] = m;
        if (!body.trim() || /\bsrc=/.test(attrs) || /type="(text\/template|text\/html|application\/json|module)"/.test(attrs)) continue;
        if (FACELETS_TAG.test(body)) continue;
        const code = body
            .split(/(<!\[CDATA\[[\s\S]*?\]\]>)/)
            .map((part) => (part.startsWith('<![CDATA[') ? part.slice(9, -3) : decode(part)))
            .join('');
        try {
            parse(maskEL(code), { sourceType: 'script', allowReturnOutsideFunction: true });
        } catch (e) {
            found.push(`${source.slice(0, m.index).split('\n').length}: ${e.message}`);
        }
    }
    return found;
}

function xhtmlFiles(dir) {
    return fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
        const full = path.join(dir, entry.name);
        if (entry.isDirectory()) return SKIP_DIRS.has(entry.name) ? [] : xhtmlFiles(full);
        return /\.(xhtml|html)$/.test(entry.name) ? [full] : [];
    });
}

it('accepts EL inside strings and as a value', () => {
    expect(brokenScripts("<script>var a = '#{msg.x}'; var b = #{bean.list};</script>")).toEqual([]);
});

it('flags a string that a formatter broke across lines', () => {
    expect(brokenScripts("<script>\n$('.x').append('\n<br />\n');\n</script>")).toHaveLength(1);
});

it('flags an escaped operator that lost its second ampersand', () => {
    expect(brokenScripts('<script>var p = a &amp; &amp; b;</script>')).toHaveLength(1);
});

it('skips scripts that interleave Facelets tags', () => {
    expect(brokenScripts('<script>var a = [<ui:repeat value="#{x}" var="i">#{i},</ui:repeat>];</script>')).toEqual([]);
});

it('finds no broken inline script in the core XHTML', () => {
    const broken = xhtmlFiles(RESOURCES).flatMap((file) => brokenScripts(fs.readFileSync(file, 'utf8')).map((f) => `${path.relative(RESOURCES, file)}:${f}`));
    expect(broken).toEqual([]);
});
