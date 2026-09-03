/** Extracts the snippet text from a IIIF `resource` (object, array, or missing). */
function _snippet(resource) {
    const r = Array.isArray(resource) ? resource[0] : resource;
    return r && typeof r.value === 'string' ? r.value : '';
}

/**
 * Indexes the `search:Hit` list by annotation id → surrounding context
 * (`before` / `match` / `after`), for building a teaser around each match.
 */
function _contextByAnnotation(annotationList) {
    const map = new Map();
    const hits = Array.isArray(annotationList.hits) ? annotationList.hits : [];
    for (const hit of hits) {
        const annos = Array.isArray(hit.annotations) ? hit.annotations : hit.annotations ? [hit.annotations] : [];
        for (const id of annos) {
            map.set(id, { before: hit.before, match: hit.match, after: hit.after });
        }
    }
    return map;
}

/**
 * Parses a IIIF Content Search `sc:AnnotationList` into hits. Pages are 1-based
 * (as in the `on` URL `/pages/{n}/canvas`); the caller converts to the 0-based
 * IvViewer order. `before`/`match`/`after` come from the `hits` block and are
 * undefined when no context is provided.
 *
 * @returns {{id:string, page:number, rect:{x,y,w,h}|null, snippet:string, before?:string, match?:string, after?:string}[]}
 */
export function parseSearchHits(annotationList) {
    if (!annotationList || !Array.isArray(annotationList.resources)) return [];
    const context = _contextByAnnotation(annotationList);
    const hits = [];
    for (const res of annotationList.resources) {
        const on = typeof res.on === 'string' ? res.on : (res.on && res.on['@id']) || '';
        const page = on.match(/\/pages\/(\d+)\/canvas/);
        if (!page) continue;
        const xywh = on.match(/#xywh=(\d+),(\d+),(\d+),(\d+)/);
        const id = res['@id'] || res.id;
        const ctx = context.get(id) || {};
        hits.push({
            id,
            page: Number(page[1]),
            rect: xywh ? { x: +xywh[1], y: +xywh[2], w: +xywh[3], h: +xywh[4] } : null,
            snippet: _snippet(res.resource),
            before: ctx.before,
            match: ctx.match,
            after: ctx.after,
        });
    }
    return hits;
}

/** Next hit index with wrap-around; -1 when there are no hits. */
export function nextIndex(i, total) {
    return total ? (i + 1) % total : -1;
}

/** Previous hit index with wrap-around; -1 when there are no hits. */
export function prevIndex(i, total) {
    return total ? (i - 1 + total) % total : -1;
}

/** Last result page from `within.last` (`…&page=N`); 1 when absent. */
function _lastPage(list) {
    const last = list && list.within && list.within.last;
    const m = typeof last === 'string' ? last.match(/[?&]page=(\d+)/) : null;
    return m ? Number(m[1]) : 1;
}

/**
 * Parses the IIIF search response tolerantly against a backend bug: a `search:Hit`
 * without annotations is serialized as `{"@type":"search:Hit","annotations"}` (key
 * without value) → invalid JSON. Plain parsing is tried first so valid responses
 * are never rewritten.
 * TODO(iiif-api-model): remove once the URLOnlySerializer fix lands (empty
 * annotations → valid JSON) — then a plain `res.json()` suffices.
 */
function _parseSearchJson(text) {
    try {
        return JSON.parse(text);
    } catch {
        try {
            return JSON.parse(text.replace(/"annotations"\}/g, '"annotations":[]}'));
        } catch {
            return {};
        }
    }
}

/**
 * Fetches all result pages of the IIIF Content Search and returns hits with the
 * 0-based IvViewer `order` (order = page - 1), in document order. Tolerates
 * broken backend paging (page 2+ may restart from the top): hits are deduped by
 * annotation id and fetching stops once a page adds nothing new.
 *
 * @param {string} pi        Goobi viewer record identifier
 * @param {string} apiBase   base URL of the REST API (no trailing slash)
 * @param {string} term      search term
 * @param {Function} fetchFn fetch-compatible function (injectable for tests)
 * @param {number} maxPages  safety cap for the within-paging loop
 */
export async function search(pi, apiBase, term, fetchFn = fetch, maxPages = 50) {
    const base = `${apiBase}/records/${pi}/manifest/search?q=${encodeURIComponent(term)}`;
    const seen = new Set();
    const out = [];
    for (let p = 1; p <= maxPages; p++) {
        const res = await fetchFn(p === 1 ? base : `${base}&page=${p}`);
        if (!res.ok) break;
        const list = _parseSearchJson(await res.text());
        const hits = parseSearchHits(list);
        let added = 0;
        for (const h of hits) {
            const rect = h.rect ? `${h.rect.x},${h.rect.y},${h.rect.w},${h.rect.h}` : '';
            const key = h.id || `${h.page}|${rect}|${h.snippet}`;
            if (seen.has(key)) continue;
            seen.add(key);
            out.push({ ...h, order: h.page - 1 });
            added++;
        }
        if (hits.length === 0 || added === 0 || p >= _lastPage(list)) break;
    }
    return out;
}
