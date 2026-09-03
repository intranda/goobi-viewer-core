/** ALTO annotation model: pure mappings and geometry for word marks, drawn lines, margin-note anchoring and stacking. */

/**
 * Presentation mapping keyed by the SEMANTIC types the layout endpoint derives from the
 * configured `altoLayoutTags` mappings (raw labels stay in the payload for reference).
 * Author cards additionally carry the configured styleClass verbatim, so arbitrary
 * customer vocabularies are styled via config + theme CSS without client changes; the
 * default author colors hook onto the default styleClasses in ivAnnotations.less.
 */
const WORD_TYPE_CLASSES = {
    underline: 'immersive__anno-word-underline',
    strikethrough: 'immersive__anno-word-strike',
};

/** Drawn-line types rendered as image overlays; lines without a mapped type stay hidden. */
const LINE_TYPE_MODIFIERS = {
    underline: 'underline',
    strikethrough: 'strike',
    mark: 'mark',
};

/**
 * Extracts the ALTO element id from a page text annotation URI. Those URIs end
 * in `alto_{pi}_{pageNo}_{elementId}` (plus an optional query); the element id
 * itself may contain underscores, so the prefix is matched as a whole. Null
 * for synthetic ids and annotations of other pages. Pure + tested.
 *
 * @param {string} regionId  a data-iv-region-id value (annotation @id)
 * @param {string} pi
 * @param {number|string} pageNo  1-based page number
 * @returns {string|null}
 */
export function altoElementId(regionId, pi, pageNo) {
    if (!regionId) return null;
    const id = String(regionId).split('?')[0].replace(/\/+$/, '');
    const marker = `alto_${pi}_${pageNo}_`;
    const at = id.lastIndexOf(marker);
    if (at === -1) return null;
    const element = id.slice(at + marker.length);
    return element.length && !element.includes('/') ? element : null;
}

/**
 * Whether a layout result carries any annotation data. Pure + tested.
 *
 * @param {{blocks:Array, words:Array, lines:Array}|null} layout
 * @returns {boolean}
 */
export function hasAnnotations(layout) {
    if (!layout) return false;
    return !!((layout.blocks && layout.blocks.length) || (layout.words && layout.words.length) || (layout.lines && layout.lines.length));
}

/**
 * CSS classes for a word's semantic types (underline/strikethrough); unknown
 * types are ignored, duplicates collapse. Pure + tested.
 *
 * @param {string[]} [types]  semantic types from the layout endpoint
 * @returns {string[]}
 */
export function wordTypeClasses(types) {
    const classes = [];
    (types || []).forEach((type) => {
        const cls = WORD_TYPE_CLASSES[type];
        if (cls && !classes.includes(cls)) classes.push(cls);
    });
    return classes;
}

/**
 * Overlay modifier for a drawn line's semantic types — underline, strike or
 * mark; null when none of its types is rendered. Pure + tested.
 *
 * @param {string[]} [types]  semantic types from the layout endpoint
 * @returns {string|null}
 */
export function lineTypeModifier(types) {
    for (const type of types || []) {
        if (LINE_TYPE_MODIFIERS[type]) return LINE_TYPE_MODIFIERS[type];
    }
    return null;
}

/**
 * Index of the anchor whose vertical midpoint is closest to `midY` (all in
 * ALTO units), -1 without anchors. Pure + tested.
 *
 * @param {number} midY
 * @param {{y:number, h:number}[]} [anchors]
 * @returns {number}
 */
export function nearestAnchorIndex(midY, anchors) {
    let best = -1;
    let bestDist = Infinity;
    (anchors || []).forEach((a, i) => {
        const dist = Math.abs(a.y + a.h / 2 - midY);
        if (dist < bestDist) {
            bestDist = dist;
            best = i;
        }
    });
    return best;
}

/**
 * Collision handling of the margin column: a note stays exactly at its desired
 * top (its anchor line) when it has the room. Overlapping runs merge into a
 * cluster placed at the median of its members' candidate tops (the L1-optimal
 * placement — do NOT "simplify" to the average), so the displacement is
 * balanced up AND down around the lines instead of accumulating as an
 * ever-growing downward drift (cards are taller than print lines). Clusters
 * are clamped to the container top and never overlap each other. Expects
 * notes sorted by desired top. Pure + tested.
 *
 * @param {{top:number, height:number}[]} notes
 * @param {number} gap
 * @returns {number[]} final top offsets
 */
export function stackNoteTops(notes, gap) {
    const clusterTop = (c) => {
        const cands = c.desired.map((d, i) => d - c.offsets[i]).sort((a, b) => a - b);
        const mid = Math.floor(cands.length / 2);
        const optimal = cands.length % 2 ? cands[mid] : (cands[mid - 1] + cands[mid]) / 2;
        return Math.max(0, optimal);
    };
    const clusters = [];
    (notes || []).forEach((n) => {
        clusters.push({ desired: [n.top], offsets: [0], height: n.height });
        while (clusters.length > 1) {
            const prev = clusters[clusters.length - 2];
            const last = clusters[clusters.length - 1];
            if (clusterTop(last) >= clusterTop(prev) + prev.height + gap) break;
            const base = prev.height + gap;
            last.offsets.forEach((offset, i) => {
                prev.desired.push(last.desired[i]);
                prev.offsets.push(base + offset);
            });
            prev.height = base + last.height;
            clusters.pop();
        }
    });
    const tops = [];
    clusters.forEach((c) => {
        const top = clusterTop(c);
        c.offsets.forEach((offset) => tops.push(top + offset));
    });
    return tops;
}

/**
 * Whether a note whose vertical midpoint is `midY` sits ABOVE its anchor line
 * (running heads, interlinear additions) rather than ON it — then its card
 * also goes above the content row. The threshold is the anchor's TOP edge,
 * not its midpoint: handwriting regularly sits slightly high of the printed
 * line, and that jitter must not push the card a full row up. Pure + tested.
 *
 * @param {number} midY  the note block's vertical midpoint (ALTO units)
 * @param {{y:number, h:number}|null} anchor
 * @returns {boolean}
 */
export function isAboveAnchor(midY, anchor) {
    return !!anchor && midY < anchor.y;
}

/**
 * Whether the center of `inner` lies inside `outer` (ALTO units) — the same
 * containment test the text nesting uses. Pure + tested.
 *
 * @param {{x:number, y:number, w:number, h:number}} inner
 * @param {{x:number, y:number, w:number, h:number}} outer
 * @returns {boolean}
 */
export function rectCenterInside(inner, outer) {
    if (!inner || !outer) return false;
    const cx = inner.x + inner.w / 2;
    const cy = inner.y + inner.h / 2;
    return cx >= outer.x && cx <= outer.x + outer.w && cy >= outer.y && cy <= outer.y + outer.h;
}

/**
 * Image overlay descriptors for a layout: drawn lines with a rendered tag type
 * plus the margin notes as 'note' hit areas (invisible until hovered/coupled).
 * Pure + tested.
 *
 * @param {{lines:Array, blocks:Array}|null} layout
 * @returns {{id:string, rect:object, modifier:string}[]}
 */
export function overlayRegions(layout) {
    const regions = [];
    ((layout && layout.lines) || []).forEach((line) => {
        const modifier = lineTypeModifier(line.types);
        if (modifier && line.rect) regions.push({ id: line.id, rect: line.rect, modifier });
    });
    ((layout && layout.blocks) || []).forEach((block) => {
        if (block.author && block.rect) regions.push({ id: block.id, rect: block.rect, modifier: 'note' });
    });
    return regions;
}
