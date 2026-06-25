/** Snippet aus IIIF `resource` (Objekt, Array oder fehlend) ziehen. */
function _snippet(resource) {
    const r = Array.isArray(resource) ? resource[0] : resource;
    return r && typeof r.value === 'string' ? r.value : '';
}

/**
 * Parst eine IIIF Content Search `sc:AnnotationList` zu Treffern.
 * Seite ist 1-basiert (wie in der `on`-URL `/pages/{n}/canvas`); der Aufrufer
 * rechnet auf die 0-basierte IvViewer-Order um (order = page - 1).
 * @returns {{page:number, rect:{x,y,w,h}|null, snippet:string}[]}
 */
export function parseSearchHits(annotationList) {
    if (!annotationList || !Array.isArray(annotationList.resources)) return [];
    const hits = [];
    for (const res of annotationList.resources) {
        const on = typeof res.on === 'string' ? res.on : (res.on && res.on['@id']) || '';
        const page = on.match(/\/pages\/(\d+)\/canvas/);
        if (!page) continue;
        const xywh = on.match(/#xywh=(\d+),(\d+),(\d+),(\d+)/);
        hits.push({
            page: Number(page[1]),
            rect: xywh ? { x: +xywh[1], y: +xywh[2], w: +xywh[3], h: +xywh[4] } : null,
            snippet: _snippet(res.resource),
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
    const all = [];
    for (let p = 1; p <= maxPages; p++) {
        const res = await fetchFn(p === 1 ? base : `${base}&page=${p}`);
        if (!res.ok) break;
        const list = _parseSearchJson(await res.text());
        const hits = parseSearchHits(list);
        all.push(...hits);
        if (hits.length === 0 || p >= _lastPage(list)) break;
    }
    return all.map((h) => ({ ...h, order: h.page - 1 }));
}
