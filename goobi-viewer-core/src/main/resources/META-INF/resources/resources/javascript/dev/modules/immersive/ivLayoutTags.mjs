/** Layout-tag panel logic: index/detail loading and parsing, range clamping, hit-list derivation, visibility batching. */

const indexCache = new Map();

/** Highest number of pages one visibility-driven detail request spans (the endpoint parses one ALTO per page in range). */
export const VISIBLE_BATCH = 8;

/** Number of tags from which the pick menu shows its free-text filter. */
export const FILTER_THRESHOLD = 13;

/**
 * Replaces `{0}`, `{1}`, … in a message template with the given values.
 * Pure + tested.
 *
 * @param {string} template
 * @param {...(string|number)} values
 * @returns {string}
 */
export function formatMessage(template, ...values) {
    return String(template || '').replace(/\{(\d+)\}/g, (m, i) => (values[i] != null ? String(values[i]) : ''));
}

/**
 * Normalizes the layout-tag index endpoint response: tags without a label are
 * dropped, pages become ascending, duplicate-free finite integers, missing
 * names fall back to the label. Pure + tested.
 *
 * @param {object} json  the /layout/tags/index response
 * @returns {{pi:string, pageCount:number, tags:{label:string, name:string, styleClass:string, pages:number[]}[]}}
 */
export function parseLayoutTagIndex(json) {
    const src = json && typeof json === 'object' ? json : {};
    const tags = (Array.isArray(src.tags) ? src.tags : [])
        .filter((t) => t && typeof t.label === 'string' && t.label.length > 0)
        .map((t) => {
            const pages = Array.from(new Set((Array.isArray(t.pages) ? t.pages : []).map(Number).filter((p) => Number.isInteger(p) && p >= 1))).sort((a, b) => a - b);
            return { label: t.label, name: typeof t.name === 'string' && t.name ? t.name : t.label, styleClass: typeof t.styleClass === 'string' ? t.styleClass : '', pages };
        });
    return { pi: typeof src.pi === 'string' ? src.pi : '', pageCount: Number.isInteger(src.pageCount) ? src.pageCount : 0, tags };
}

/**
 * Parses the `x1,y1,x2,y2` coordinate string of the layout-tag endpoint into
 * an image-pixel rect; null for degenerate or unparsable boxes (the printed
 * label carries `0,0,0,0`). Pure + tested.
 *
 * @param {string} coordinates
 * @returns {{x:number, y:number, w:number, h:number}|null}
 */
export function parseCoordinates(coordinates) {
    const parts = String(coordinates || '')
        .split(',')
        .map((v) => parseFloat(v));
    if (parts.length !== 4 || parts.some((v) => !Number.isFinite(v))) return null;
    const [x1, y1, x2, y2] = parts;
    const w = x2 - x1;
    const h = y2 - y1;
    if (w <= 0 || h <= 0) return null;
    return { x: x1, y: y1, w, h };
}

/**
 * Maps a page-range response of `/records/{pi}/layout/tags` to occurrences per
 * page and label. Every page of the requested range gets an entry (pages the
 * response does not mention count as loaded without occurrences), so the
 * caller can mark the whole range as fetched. Occurrences without a drawable
 * rect are skipped. Pure + tested.
 *
 * @param {object} json  the endpoint response (`DocumentReference`)
 * @param {number} start first page of the requested range (1-based, inclusive)
 * @param {number} end   last page of the requested range (inclusive)
 * @returns {Map<number, Object<string, {id:string, rect:object, content:string}[]>>}
 */
export function parseLayoutTagDetails(json, start, end) {
    const details = new Map();
    for (let p = start; p <= end; p++) details.set(p, {});
    const groups = json && Array.isArray(json.pages) ? json.pages : [];
    groups.forEach((group) => {
        const groupPage = Number(group && (group.pageOrder != null ? group.pageOrder : group.firstPage));
        (group && Array.isArray(group.tags) ? group.tags : []).forEach((tag) => {
            if (!tag || typeof tag.value !== 'string') return;
            (Array.isArray(tag.references) ? tag.references : []).forEach((ref) => {
                if (!ref) return;
                const page = Number.isInteger(ref.page) ? ref.page : groupPage;
                if (!Number.isInteger(page)) return;
                const rect = parseCoordinates(ref.coordinates);
                if (!rect) return;
                if (!details.has(page)) details.set(page, {});
                const byLabel = details.get(page);
                (byLabel[tag.value] = byLabel[tag.value] || []).push({ id: ref.id || '', rect, content: ref.content || '' });
            });
        });
    });
    return details;
}

/**
 * Pages of a tag inside the range. Pure + tested.
 *
 * @param {{pages:number[]}|null} tag
 * @param {number} from
 * @param {number} to
 * @returns {number[]}
 */
export function tagPages(tag, from, to) {
    return tag ? tag.pages.filter((p) => p >= from && p <= to) : [];
}

/**
 * Ascending pages on which any of the given tags occurs inside the range.
 * Pure + tested.
 *
 * @param {{pages:number[]}[]} tags
 * @param {number} from
 * @param {number} to
 * @returns {number[]}
 */
export function hitPages(tags, from, to) {
    const set = new Set();
    tags.forEach((t) => tagPages(t, from, to).forEach((p) => set.add(p)));
    return Array.from(set).sort((a, b) => a - b);
}

/**
 * One entry per page and tag, ordered by page, then by the given tag order.
 * `occurrencesOf(page, label)` returns the loaded occurrences or undefined
 * while the page detail is still pending. Pure + tested.
 *
 * @param {{label:string, pages:number[]}[]} tags  the selected tags
 * @param {number} from
 * @param {number} to
 * @param {(page:number, label:string) => Array|undefined} occurrencesOf
 * @returns {{page:number, label:string, pending:boolean, count:number}[]}
 */
export function flatEntries(tags, from, to, occurrencesOf) {
    const out = [];
    hitPages(tags, from, to).forEach((page) => {
        tags.forEach((tag) => {
            if (!tag.pages.includes(page)) return;
            const occ = occurrencesOf(page, tag.label);
            out.push({ page, label: tag.label, pending: occ === undefined, count: occ ? occ.length : 0 });
        });
    });
    return out;
}

/**
 * Stable identity of a hit-list entry (`page#label`). Pure + tested.
 *
 * @param {{page:number, label:string}} entry
 * @returns {string}
 */
export function entryKey(entry) {
    return `${entry.page}#${entry.label}`;
}

/**
 * Index of the cursor entry: the exact key, else the first entry on the
 * cursor's page, else 0. Pure + tested.
 *
 * @param {{page:number, label:string}[]} entries
 * @param {string|null} cursorKey
 * @returns {number}
 */
export function cursorIndex(entries, cursorKey) {
    if (!cursorKey) return 0;
    const at = entries.findIndex((e) => entryKey(e) === cursorKey);
    if (at !== -1) return at;
    const page = Number(String(cursorKey).split('#')[0]);
    const onPage = entries.findIndex((e) => e.page === page);
    return onPage !== -1 ? onPage : 0;
}

/**
 * Clamps a typed page range to `[1, pageCount]` and swaps reversed bounds;
 * non-numeric input falls back to the respective end. Pure + tested.
 *
 * @param {*} from
 * @param {*} to
 * @param {number} pageCount
 * @returns {{from:number, to:number}}
 */
export function clampRange(from, to, pageCount) {
    const max = Math.max(1, pageCount);
    const clamp = (v, fallback) => {
        const n = Math.round(Number(v));
        return Math.max(1, Math.min(max, Number.isFinite(n) && n > 0 ? n : fallback));
    };
    let a = clamp(from, 1);
    let b = clamp(to, max);
    if (a > b) [a, b] = [b, a];
    return { from: a, to: b };
}

/**
 * Tags shown in the pick menu: those with pages in the range or already
 * selected, narrowed by the free-text filter (name or label, case-insensitive);
 * selected tags always stay visible. Pure + tested.
 *
 * @param {{label:string, name:string, pages:number[]}[]} tags
 * @param {{selected:Set<string>, filter:string, from:number, to:number}} opts
 * @returns {Array}
 */
export function filterTags(tags, { selected, filter, from, to }) {
    const f = String(filter || '')
        .trim()
        .toLowerCase();
    return tags.filter((t) => {
        if (selected.has(t.label)) return true;
        if (!tagPages(t, from, to).length) return false;
        return !f || t.name.toLowerCase().includes(f) || t.label.toLowerCase().includes(f);
    });
}

/**
 * Pages whose placeholder rows lie within one panel height above or below the
 * visible panel area, in DOM order, duplicate-free, at most `batch`.
 * Pure + tested.
 *
 * @param {{top:number, bottom:number}} panelBox
 * @param {{page:number, top:number, bottom:number}[]} rows  pending rows with their boxes
 * @param {number} batch
 * @returns {number[]}
 */
export function visiblePages(panelBox, rows, batch) {
    const height = panelBox.bottom - panelBox.top;
    const wanted = [];
    rows.forEach((r) => {
        if (r.bottom > panelBox.top - height && r.top < panelBox.bottom + height && !wanted.includes(r.page)) wanted.push(r.page);
    });
    return wanted.slice(0, batch);
}

/**
 * Groups pages into contiguous fetch ranges of at most `batch` pages each, so
 * a sparse selection never drags hundreds of untagged pages into one request.
 * Pure + tested.
 *
 * @param {number[]} pages
 * @param {number} batch
 * @returns {{start:number, end:number}[]}
 */
export function batchRanges(pages, batch) {
    const sorted = Array.from(new Set(pages)).sort((a, b) => a - b);
    const ranges = [];
    sorted.forEach((p) => {
        const last = ranges[ranges.length - 1];
        if (last && p - last.start + 1 <= batch) last.end = p;
        else ranges.push({ start: p, end: p });
    });
    return ranges;
}

/**
 * Fetches (and memoizes per pi) the record's layout-tag index. The UI language
 * is passed along so the server resolves the display names in it. A failed
 * request is evicted from the cache so a transient error does not poison it.
 *
 * @param {string} pi
 * @param {string} apiBase  base URL of the REST API (no trailing slash)
 * @param {Function} fetchFn fetch-compatible function (injectable for tests)
 * @param {string} [lang]   UI language code for the display names
 * @returns {Promise<ReturnType<typeof parseLayoutTagIndex>>}
 */
export function loadLayoutTagIndex(pi, apiBase, fetchFn = fetch, lang = '') {
    if (indexCache.has(pi)) return indexCache.get(pi);
    const query = lang ? `?lang=${encodeURIComponent(lang)}` : '';
    const promise = fetchFn(`${apiBase}/records/${pi}/layout/tags/index${query}`).then((res) => {
        if (!res.ok) throw new Error(`Failed to load layout-tag index for "${pi}": HTTP ${res.status}`);
        return res.json().then(parseLayoutTagIndex);
    });
    indexCache.set(pi, promise);
    return promise.catch((e) => {
        indexCache.delete(pi);
        throw e;
    });
}

/**
 * Fetches the layout-tag occurrences of a page range in one request.
 *
 * @param {string} pi
 * @param {string} apiBase
 * @param {number} start  first page (1-based, inclusive)
 * @param {number} end    last page (inclusive)
 * @param {Function} fetchFn
 * @returns {Promise<Map<number, object>>} see {@link parseLayoutTagDetails}
 */
export async function loadLayoutTagDetails(pi, apiBase, start, end, fetchFn = fetch) {
    const res = await fetchFn(`${apiBase}/records/${pi}/layout/tags/?start=${start}&end=${end}`);
    if (!res.ok) throw new Error(`Failed to load layout tags ${start}-${end} for "${pi}": HTTP ${res.status}`);
    return parseLayoutTagDetails(await res.json(), start, end);
}

/** Clears the module-level index cache. Test use only. */
export function _clearCache() {
    indexCache.clear();
}
