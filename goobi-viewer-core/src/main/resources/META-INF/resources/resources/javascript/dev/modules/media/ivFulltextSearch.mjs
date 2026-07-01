/** Snippet aus IIIF `resource` (Objekt, Array oder fehlend) ziehen. */
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
 * Parst eine IIIF Content Search `sc:AnnotationList` zu Treffern.
 * Seite ist 1-basiert (wie in der `on`-URL `/pages/{n}/canvas`); der Aufrufer
 * rechnet auf die 0-basierte IvViewer-Order um (order = page - 1).
 * `before`/`match`/`after` stammen aus dem `hits`-Block (per Annotation-Id verknüpft)
 * und sind undefined, wenn kein Kontext geliefert wird.
 * @returns {{page:number, rect:{x,y,w,h}|null, snippet:string, before?:string, match?:string, after?:string}[]}
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

export function nextIndex(i, total) {
    return total ? (i + 1) % total : -1;
}
export function prevIndex(i, total) {
    return total ? (i - 1 + total) % total : -1;
}

/** Letzte Ergebnis-Seite aus `within.last` (`…&page=N`); 1 wenn nicht vorhanden. */
function _lastPage(list) {
    const last = list && list.within && list.within.last;
    const m = typeof last === 'string' ? last.match(/[?&]page=(\d+)/) : null;
    return m ? Number(m[1]) : 1;
}

/**
 * Parst die IIIF-Search-Response tolerant gegen einen Backend-Bug: ein `search:Hit`
 * ohne Annotationen wird als `{"@type":"search:Hit","annotations"}` (Key ohne Wert)
 * serialisiert → invalides JSON. Genau dieses Muster wird vor dem Parsen repariert;
 * wir nutzen ohnehin nur `resources`, nicht `hits`.
 * TODO(iiif-api-model): nach URLOnlySerializer-Fix (leere annotations → valides JSON)
 * entfernen — dann reicht `res.json()` ohne Repair.
 */
function _parseSearchJson(text) {
    try {
        return JSON.parse(text.replace(/"annotations"\}/g, '"annotations":[]}'));
    } catch (e) {
        return {};
    }
}

/**
 * Holt alle Ergebnis-Seiten der IIIF Content Search und liefert Treffer mit
 * 0-basierter IvViewer-`order` (order = page - 1), in Dokumentreihenfolge.
 * @param {function} fetchFn fetch-kompatibel (injizierbar für Tests)
 * @param {number} maxPages Sicherheits-Cap der within-Paging-Schleife
 */
export async function search(pi, apiBase, term, fetchFn = fetch, maxPages = 50) {
    const base = `${apiBase}/records/${pi}/manifest/search?q=${encodeURIComponent(term)}`;
    // The backend's content-search paging can be broken (page 2+ may restart from the top
    // instead of returning the next slice), which would duplicate -- and needlessly re-fetch --
    // the whole list. Dedupe by annotation id (with a page/rect/snippet fallback for hits that
    // carry no id) as we go, and stop as soon as a page brings nothing new.
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
