/**
 * Token check — the theme findings as numbered steps, and the terminal report.
 */

const plural = (n, word) => `${n} ${word}${n === 1 ? '' : 's'}`;

const formatFinding = (f) => `    ${f.line ? `${f.file}:${f.line}` : f.file}  ${f.what}  →  ${f.fix}`;

const TODO_ORDER = [
    'token layer missing',
    'token layer not imported',
    'brand colour not set',
    'reads a token the core renamed',
    'reads a token the core removed',
    'token whose meaning changed',
    'old core token name',
    'old core overrides',
    'nothing reads this token',
    'theme without dark mode',
    'light/dark branch in a component',
    'light and dark declare different tokens',
];

const FULL_ORDER = [
    "carry the theme's colours into the token layer",
    'colours in components',
    'fixed text sizes',
    'token declared in a component',
    'light/dark branch in a component',
    'palette colour owned by the roles',
    'role name the core does not declare',
    'palette colour the core does not declare',
    'colour literal outside the palette',
    'light-dark() outside the palette',
    'delete the colour and size constants',
    'dark mode, only if wanted',
];

const groupBy = (findings, order = TODO_ORDER) => {
    const groups = new Map();
    for (const f of findings) {
        const group = f.group ?? f.fix.split(' — ')[0];
        groups.set(group, [...(groups.get(group) ?? []), f]);
    }
    const rank = (group) => (order.includes(group) ? order.indexOf(group) : order.length);
    return new Map([...groups].sort((a, b) => rank(a[0]) - rank(b[0])));
};

const STEP_HINTS = {
    'reads a token the core renamed': () => 'read the new name',
    'old core token name': () =>
        'the core no longer reads these names: move the line into the token layer under its new name and keep the value, or delete it where the core reads another token now',
    'nothing reads this token': () => 'neither the core nor the theme reads these lines: delete them',
    "carry the theme's colours into the token layer": () =>
        "add these lines, so that the roles carry the theme's own colours and the look stays; where a role is asked for more colours (also …), keep one and give the other places a theme token",
    'colours in components': (step) => {
        const declared = step("carry the theme's colours into the token layer");
        return `read a role instead of the colour; a name after the component, such as --header-bg, is a theme token${declared ? ` that step ${declared} declares` : ''}`;
    },
    'fixed text sizes': () => 'read a text role instead of the fixed size (1rem = 10px); where the role is only close, compare in the browser',
    'token declared in a component': () => 'a value only this file reads becomes --_name, a token other files read moves into misc/tokens.less, a token nobody reads goes',
    'delete the colour and size constants': () => 'delete these constants, nothing reads them once the steps above are done',
};

const adviceOf = (group) => (f) => f.advice ?? (f.fix.startsWith(`${group} — `) ? f.fix.slice(group.length + 3) : f.fix);

const themePath = (f) => f.file.replace(/^theme\//, '');

/**
 * Theme findings as numbered steps: what the new core requires, then the complete token migration.
 * A step carries its group, its findings and what it asks for, said once (`said`).
 *
 * @param {Array} findings theme findings
 * @returns {{todo: Array, full: Array, sections: Array<{title: string, about: string, steps: Array<{step: number, group: string, list: Array, said: string|null}>}>}}
 */
function themeSteps(findings) {
    const todo = findings.filter((f) => f.kind !== 'full');
    const full = findings.filter((f) => f.kind === 'full');
    let number = 0;
    const sections = [
        { title: 'REQUIRED FOR THE NEW CORE', about: 'the theme works and looks as before', groups: groupBy(todo) },
        {
            title: 'COMPLETE TOKEN MIGRATION',
            about: 'the components read tokens instead of fixed colours and sizes: needed for dark mode, and so that design changes of the core reach the theme; the first step keeps the look',
            groups: groupBy(full, FULL_ORDER),
        },
    ]
        .filter((s) => s.groups.size)
        .map(({ groups, ...s }) => ({ ...s, steps: [...groups].map(([group, list]) => ({ step: ++number, group, list })) }));
    const steps = sections.flatMap((s) => s.steps);
    const stepOf = (group) => steps.find((s) => s.group === group)?.step ?? null;
    for (const s of steps) {
        const advice = adviceOf(s.group);
        s.said = STEP_HINTS[s.group]?.(stepOf) ?? (new Set(s.list.map(advice)).size === 1 ? advice(s.list[0]) : null);
    }
    return { todo, full, sections };
}

/**
 * The code of one step, per file: "-" a line as it stands, "+" as it reads afterwards, " " a line that
 * stays, "…" lines left out, null a place without code. Lines for one mixin stand inside it, under the
 * section and label comments of the core's token files; a note stands on the last row of its change.
 *
 * @param {{group: string, list: Array, said: string|null}} step
 * @returns {Array<{file: string, tag: string|null, rows: Array<{n: number|null, sign: string|null, text: string, note: string|null}>}>}
 */
function stepFiles({ group, list, said }) {
    const advice = adviceOf(group);
    const noteOf = (f) => {
        const note = f.note ?? (f.changed ? null : advice(f));
        return note === said ? null : note;
    };
    const noted = (code) =>
        Math.max(
            0,
            code.findIndex((b) => b.rows.some((r) => r.sign === '+'))
        );
    const blocks = list.flatMap((f) => {
        const code = f.code?.length ? f.code : [{ file: f.file, line: f.line, rows: [] }];
        const target = code.find((b) => b.into);
        const moved = (b) => target && !b.into && b.rows.length > 0 && b.rows.every((r) => r.sign === '-');
        return code.map((b, i) => ({ ...b, what: f.what, note: i === noted(code) ? noteOf(f) : moved(b) ? `moves to ${themePath(target)}` : null }));
    });
    const key = (b) => `${themePath(b)}|${b.into}|${b.line}|${b.rows.map((r) => r.sign + r.text).join('\n')}`;
    const unique = blocks.filter((b, i) => !b.rows.length || blocks.findIndex((o) => key(o) === key(b)) === i);
    const files = new Map();
    unique.forEach((b) => files.set(themePath(b), [...(files.get(themePath(b)) ?? []), b]));
    const pasted = (inFile) => inFile.every((b) => b.rows.length > 0 && b.rows.every((r) => r.n === null));
    return [...files]
        .sort(([, a], [, b]) => pasted(a) - pasted(b))
        .map(([file, inFile]) => {
            const rows = [];
            const push = (b, indent = '') => {
                if (!b.rows.length && (b.line || b.note)) {
                    rows.push({ n: b.line || null, sign: null, text: b.line ? b.what : '', note: b.note });
                }
                b.rows.forEach((r, i) => rows.push({ n: r.n, sign: r.sign, text: `${indent}${r.text}`, note: i === b.rows.length - 1 ? b.note : null }));
            };
            for (const b of inFile.filter((x) => !x.into).sort((x, y) => (x.line || Infinity) - (y.line || Infinity))) {
                push(b);
            }
            const mixins = new Map();
            inFile.filter((x) => x.into).forEach((b) => mixins.set(b.into, [...(mixins.get(b.into) ?? []), b]));
            const seen = [];
            const rank = (b) => {
                const key = `${b.section}|${b.label}`;
                if (!seen.includes(key)) {
                    seen.push(key);
                }
                return [b.section?.startsWith('CORE') ? 0 : 1, b.order ?? Infinity, seen.indexOf(key)];
            };
            const compare = (x, y) => {
                const [a, b] = [rank(x), rank(y)];
                return a[0] - b[0] || a[1] - b[1] || a[2] - b[2];
            };
            for (const [into, adds] of mixins) {
                adds.forEach(rank);
                rows.push({ n: null, sign: ' ', text: `${into} {`, note: null });
                const first = rows.length;
                const blank = () => rows.push({ n: null, sign: '+', text: '', note: null });
                const gap = () => {
                    if (rows.length > first && rows[rows.length - 1].text !== '') {
                        blank();
                    }
                };
                let section = null;
                let label = null;
                for (const b of [...adds].sort(compare)) {
                    if (b.section && b.section !== section) {
                        gap();
                        rows.push({ n: null, sign: '+', text: `    /* ---------- ${b.section} ---------- */`, note: null });
                        blank();
                        section = b.section;
                        label = null;
                    }
                    if (b.label && b.label !== label) {
                        gap();
                        rows.push({ n: null, sign: '+', text: `    /* ${b.label} */`, note: null });
                        label = b.label;
                    }
                    push(b, '    ');
                }
                rows.push({ n: null, sign: ' ', text: '}', note: null });
            }
            return { file, tag: inFile.find((b) => b.tag)?.tag ?? null, rows };
        });
}

const plain = new Proxy((text) => text, { get: () => plain });

const paint = (process.stdout.isTTY || process.env.FORCE_COLOR) && !process.env.NO_COLOR ? require('ansi-colors') : plain;

const tokenNames = (text) => text.replace(/(?:var\()?--[\w-]+\)?/g, (name) => paint.magenta(name));

const SIGNS = {
    '-': (text) => paint.red(`- ${text}`),
    '+': (text) => paint.green(`+${text ? ` ${text}` : ''}`),
    ' ': (text) => `  ${text}`,
    '…': (text) => paint.dim(`  … ${text}`),
};

function wrap(text, indent, width = 100) {
    const lines = [];
    for (const word of text.split(' ')) {
        const line = lines[lines.length - 1];
        if (line !== undefined && indent + line.length + 1 + word.length <= width) {
            lines[lines.length - 1] = `${line} ${word}`;
        } else {
            lines.push(word);
        }
    }
    return lines.map((l) => `${' '.repeat(indent)}${l}`);
}

function formatStep(step) {
    const lines = ['', `${paint.bold.cyan(String(step.step).padStart(3))}  ${paint.bold(step.group)}${step.list.length > 1 ? `  ${paint.yellow(String(step.list.length))}` : ''}`];
    if (step.said) {
        lines.push(...wrap(step.said, 5).map(tokenNames));
    }
    for (const { file, tag, rows } of stepFiles(step)) {
        lines.push('', `     ${paint.cyan(file)}${tag ? `  ${paint.yellow(tag)}` : ''}`);
        const width = Math.max(3, ...rows.map((r) => String(r.n ?? '').length));
        const gutter = (n) => `     ${paint.dim(String(n ?? '').padStart(width))} ${paint.dim('│')} `;
        const column = Math.min(64, Math.max(0, ...rows.filter((r) => r.note && r.sign).map((r) => r.text.length)));
        for (const r of rows) {
            if (!r.sign) {
                lines.push(`${gutter(r.n)}  ${tokenNames([r.text, r.note].filter(Boolean).join('  '))}`);
                continue;
            }
            const code = `${gutter(r.n)}${SIGNS[r.sign](r.text)}`;
            if (!r.note) {
                lines.push(code);
            } else if (r.text.length <= column) {
                lines.push(`${code}${' '.repeat(column - r.text.length)}   ${paint.dim(r.note)}`);
            } else {
                lines.push(code, `${gutter(null)}    ${paint.dim(`↳ ${r.note}`)}`);
            }
        }
    }
    return lines;
}

function formatThemeFindings(name, findings, report) {
    const { todo, full, sections } = themeSteps(findings);
    const steps = sections.flatMap((s) => s.steps);
    const lines = [
        '',
        `${paint.bold(`Theme ${name}`)} — ${paint.yellow(`${todo.length} required`)} · ${full.length} for the complete token migration`,
        paint.dim('information only, the core check is not affected'),
    ];
    const groupWidth = Math.max(...steps.map((s) => s.group.length));
    const countWidth = Math.max(...steps.map((s) => String(s.list.length).length));
    for (const s of sections) {
        lines.push('', `  ${paint.bold(s.title)}`);
        s.steps.forEach((x) => lines.push(`${paint.cyan(String(x.step).padStart(5))}  ${x.group.padEnd(groupWidth)}  ${paint.yellow(String(x.list.length).padStart(countWidth))}`));
    }
    lines.push('');
    if (report) {
        lines.push(`  ${paint.bold('report:')} ${paint.cyan(`file://${report}`)}`);
    }
    lines.push(paint.dim('  steps in the terminal: GV_TOKENCHECK_STEP=3 (or 2,3 or all)'));
    const wanted = process.env.GV_TOKENCHECK_STEP ?? '';
    const only = (x) => wanted === 'all' || wanted.split(',').map(Number).includes(x.step);
    for (const s of sections) {
        const shown = s.steps.filter(only);
        if (!shown.length) {
            continue;
        }
        lines.push('', '', paint.bold.yellow(`━━ ${s.title} ${'━'.repeat(Math.max(3, 75 - s.title.length))}`), ...wrap(s.about, 3).map((l) => paint.dim(l)));
        shown.forEach((x) => lines.push(...formatStep(x)));
    }
    return lines;
}

/**
 * One line per finding: where, what stands there, what to do instead. Theme findings are grouped by
 * what they mean for an update and summed up; `report` is the HTML report's path, when one was written.
 *
 * @param {{core: Array, theme?: {status: string, name: string, path: string, findings?: Array}}} result
 * @param {{report?: string|null}} [options]
 * @returns {string}
 */
function formatReport({ core, theme }, { report = null } = {}) {
    const lines = [];
    if (core.length) {
        lines.push(`Token check: ${plural(core.length, 'finding')} in the core. Fix and save again.`, '');
        for (const f of core) {
            lines.push(formatFinding(f));
        }
    } else {
        lines.push(`Token check passed.${theme?.status === 'checked' ? ` Theme: ${theme.name}.` : ''}`);
    }
    if (theme?.status === 'missing') {
        lines.push(`No theme at ${theme.path} — the theme was not checked. Set GV_THEME_LESS_ROOT to your theme's css/less directory.`);
    }
    if (theme?.status === 'off') {
        lines.push('Theme check off via GV_THEMECHECK=0.');
    }
    const findings = theme?.findings || [];
    if (findings.length) {
        lines.push(...formatThemeFindings(theme.name, findings, report));
    }
    return lines.join('\n');
}

module.exports = {
    plural,
    formatFinding,
    TODO_ORDER,
    FULL_ORDER,
    groupBy,
    STEP_HINTS,
    adviceOf,
    themePath,
    themeSteps,
    stepFiles,
    plain,
    paint,
    tokenNames,
    SIGNS,
    wrap,
    formatStep,
    formatThemeFindings,
    formatReport,
};
