/** Layering of the immersive modules: shared < data < engine < ui < ivInit, relative imports only downwards, no cycles. */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const RANK = { shared: 0, data: 1, engine: 2, ui: 3, '.': 4 };

function modules() {
    const out = [];
    for (const layer of Object.keys(RANK)) {
        const dir = path.join(ROOT, layer);
        for (const file of fs.readdirSync(dir)) {
            if (file.endsWith('.mjs')) out.push({ layer, file: path.join(dir, file) });
        }
    }
    return out;
}

function importsOf(file) {
    const src = fs.readFileSync(file, 'utf8');
    return Array.from(src.matchAll(/from '(\.\.?\/[^']+)'/g), (m) => path.resolve(path.dirname(file), m[1]));
}

const graph = new Map(modules().map(({ layer, file }) => [file, { layer, imports: importsOf(file) }]));
const layerOf = (file) => graph.get(file).layer;

test('every relative import resolves to an immersive module', () => {
    for (const [file, { imports }] of graph) {
        for (const target of imports)
            expect({ from: path.relative(ROOT, file), to: path.relative(ROOT, target), exists: graph.has(target) }).toEqual(expect.objectContaining({ exists: true }));
    }
});

test('modules import only from their own or a lower layer', () => {
    const violations = [];
    for (const [file, { layer, imports }] of graph) {
        for (const target of imports) {
            if (RANK[layerOf(target)] > RANK[layer]) violations.push(`${path.relative(ROOT, file)} -> ${path.relative(ROOT, target)}`);
        }
    }
    expect(violations).toEqual([]);
});

test('the import graph has no cycles', () => {
    const cycles = [];
    const visit = (file, stack) => {
        if (stack.includes(file)) {
            cycles.push(
                stack
                    .slice(stack.indexOf(file))
                    .concat(file)
                    .map((f) => path.relative(ROOT, f))
                    .join(' -> ')
            );
            return;
        }
        for (const target of graph.get(file).imports) visit(target, stack.concat(file));
    };
    for (const file of graph.keys()) visit(file, []);
    expect(cycles).toEqual([]);
});
