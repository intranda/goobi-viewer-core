const cache = new Map();

/**
 * Resolves a service object or array to a single id string.
 *
 * @param {object|Array} service
 * @param {'@id'|'id'} primaryKey   preferred property name
 * @param {'@id'|'id'} fallbackKey  secondary property name
 * @returns {string|null} the id, or null when none can be found
 */
function _resolveServiceId(service, primaryKey, fallbackKey) {
    if (!service) return null;
    const entry = Array.isArray(service) ? service[0] : service;
    if (!entry) return null;
    const id = entry[primaryKey] || entry[fallbackKey];
    return typeof id === 'string' && id.length > 0 ? id : null;
}

/**
 * Resolves a IIIF canvas `label` (plain string, v2 `{'@value'}`, array, or v3
 * language map) to one display string: preferred language first, then
 * `none`/`@none`, then the first non-empty value. Returns '' when nothing matches.
 *
 * @param {string|object|Array} label
 * @param {string} [lang] preferred language code (e.g. the UI language)
 * @returns {string}
 */
function _resolveCanvasLabel(label, lang) {
    if (label == null) return '';
    if (typeof label === 'string') return label;
    if (Array.isArray(label)) {
        for (const item of label) {
            const v = _resolveCanvasLabel(item, lang);
            if (v) return v;
        }
        return '';
    }
    if (typeof label === 'object') {
        if (typeof label['@value'] === 'string' && label['@value'].length > 0) return label['@value'];
        for (const key of [lang, 'none', '@none']) {
            if (key && label[key]) {
                const v = _resolveCanvasLabel(label[key], lang);
                if (v) return v;
            }
        }
        for (const v of Object.values(label)) {
            const resolved = _resolveCanvasLabel(v, lang);
            if (resolved) return resolved;
        }
    }
    return '';
}

/**
 * Extracts ordered `{id, label}` entries from a manifest's canvases (v2 sequences/
 * canvases or v3 items). Canvases without a resolvable image-service id are skipped
 * entirely, so service URLs and page labels stay index-aligned.
 *
 * @param {object} manifest
 * @param {string} [lang] preferred label language
 * @returns {Array<{id: string, label: string}>}
 */
function _parseManifestCanvasEntries(manifest, lang) {
    if (!manifest || typeof manifest !== 'object') return [];

    if (Array.isArray(manifest.sequences) && manifest.sequences.length > 0) {
        const canvases = manifest.sequences[0].canvases;
        if (!Array.isArray(canvases)) return [];

        const entries = [];
        for (const canvas of canvases) {
            try {
                const id = _resolveServiceId(canvas.images[0].resource.service, '@id', 'id');
                if (id !== null) entries.push({ id, label: _resolveCanvasLabel(canvas.label, lang) });
            } catch {}
        }
        return entries;
    }

    if (Array.isArray(manifest.items) && manifest.items.length > 0) {
        const entries = [];
        for (const canvas of manifest.items) {
            try {
                const id = _resolveServiceId(canvas.items[0].items[0].body.service, 'id', '@id');
                if (id !== null) entries.push({ id, label: _resolveCanvasLabel(canvas.label, lang) });
            } catch {}
        }
        return entries;
    }

    return [];
}

/**
 * Extracts the ordered list of IIIF image-service base IDs from a manifest
 * (Presentation API v2 and v3).
 *
 * @param {object} manifest parsed IIIF Presentation manifest
 * @returns {string[]}
 */
export function parseManifestImageServices(manifest) {
    return _parseManifestCanvasEntries(manifest).map((e) => e.id);
}

/**
 * Extracts the ordered list of canvas labels from a manifest, index-aligned with
 * {@link parseManifestImageServices}. Canvases without a resolvable label yield ''.
 *
 * @param {object} manifest parsed IIIF Presentation manifest
 * @param {string} [lang]   preferred label language for v3 language maps
 * @returns {string[]}
 */
export function parseManifestPageLabels(manifest, lang) {
    return _parseManifestCanvasEntries(manifest, lang).map((e) => e.label);
}

/**
 * Fetches (and memoizes per pi) the parsed IIIF Presentation manifest, so
 * services and labels share a single network request. A failed request is
 * evicted from the cache so a transient error doesn't poison it; the
 * rejection is rethrown.
 *
 * @param {string} pi        Goobi viewer record identifier
 * @param {string} apiBase   base URL of the REST API (no trailing slash)
 * @param {Function} fetchFn fetch-compatible function (injectable for tests)
 * @returns {Promise<object>} the parsed manifest JSON
 */
function loadManifest(pi, apiBase, fetchFn = fetch) {
    if (cache.has(pi)) {
        return cache.get(pi);
    }

    const url = `${apiBase}/records/${pi}/manifest`;
    const promise = fetchFn(url).then((res) => {
        if (!res.ok) {
            throw new Error(`Failed to load manifest for "${pi}": HTTP ${res.status}`);
        }
        return res.json();
    });

    cache.set(pi, promise);

    return promise.catch((e) => {
        cache.delete(pi);
        throw e;
    });
}

/**
 * Returns the ordered list of image-service base URLs for all pages of a record.
 *
 * @param {string} pi        Goobi viewer record identifier
 * @param {string} apiBase   base URL of the REST API (no trailing slash)
 * @param {Function} fetchFn fetch-compatible function (injectable for tests)
 * @returns {Promise<string[]>}
 */
export function loadPageServices(pi, apiBase, fetchFn = fetch) {
    return loadManifest(pi, apiBase, fetchFn).then(parseManifestImageServices);
}

/**
 * Returns the ordered list of canvas labels for all pages, index-aligned with
 * {@link loadPageServices}. Shares the memoized manifest fetch.
 *
 * @param {string} pi        Goobi viewer record identifier
 * @param {string} apiBase   base URL of the REST API (no trailing slash)
 * @param {Function} fetchFn fetch-compatible function (injectable for tests)
 * @param {string} [lang]    preferred label language for v3 language maps
 * @returns {Promise<string[]>}
 */
export function loadPageLabels(pi, apiBase, fetchFn = fetch, lang) {
    return loadManifest(pi, apiBase, fetchFn).then((manifest) => parseManifestPageLabels(manifest, lang));
}

/** Clears the module-level manifest cache. Test use only. */
export function _clearCache() {
    cache.clear();
}

/**
 * Maps a IIIF/W3C `sc:AnnotationList` (one annotation per OCR text line) to plain
 * text with the lines joined by newlines.
 *
 * @param {object} annotationList the parsed `sc:AnnotationList` JSON
 * @returns {string} the page text, or '' when there are no text lines
 */
export function parsePageText(annotationList) {
    const lines = (annotationList && annotationList.resources) || [];
    return lines
        .map((a) => (a && a.resource && a.resource.chars) || '')
        .join('\n')
        .trim();
}

/**
 * Fetches the OCR fulltext of a single page as plain text.
 *
 * @param {string} pi        Goobi viewer record identifier
 * @param {string} apiBase   base URL of the REST API (no trailing slash)
 * @param {number} order     0-based page order; the endpoint is 1-based, so order + 1
 * @param {Function} fetchFn fetch-compatible function (injectable for tests)
 * @returns {Promise<string>} the page text, or '' if the request fails / has no text
 */
export async function loadPageText(pi, apiBase, order, fetchFn = fetch) {
    const res = await fetchFn(`${apiBase}/records/${pi}/pages/${order + 1}/text/`);
    if (!res.ok) {
        return '';
    }
    return parsePageText(await res.json());
}

function _parseXywh(on) {
    if (!on) return null;
    const candidates =
        typeof on === 'string' ? [on] : [typeof on['@id'] === 'string' ? on['@id'] : '', on.selector && typeof on.selector.value === 'string' ? on.selector.value : ''];
    for (const c of candidates) {
        const m = c && c.match(/xywh=(\d+),(\d+),(\d+),(\d+)/);
        if (m) return { x: +m[1], y: +m[2], w: +m[3], h: +m[4] };
    }
    return null;
}

/**
 * Maps a IIIF/W3C `sc:AnnotationList` to one structured line per OCR annotation:
 * the box comes from the `xywh` fragment on the annotation's `on` selector
 * (rect: null without one), the id from `@id` with a per-page index fallback.
 *
 * @param {object} annotationList the parsed `sc:AnnotationList` JSON
 * @returns {{id:string, chars:string, rect:{x:number,y:number,w:number,h:number}|null}[]}
 */
export function parsePageLines(annotationList) {
    const lines = (annotationList && annotationList.resources) || [];
    return lines.map((a, i) => ({
        id: (a && a['@id']) || `line-${i}`,
        chars: (a && a.resource && a.resource.chars) || '',
        rect: _parseXywh(a && a.on),
    }));
}

/**
 * Fetches one page's text annotations and parses them into structured lines.
 *
 * @param {string} pi
 * @param {string} apiBase
 * @param {number} order    0-based page order; the endpoint is 1-based, so order + 1
 * @param {Function} fetchFn
 * @param {string} query    optional query string appended to the endpoint URL
 * @returns {Promise<{id:string, chars:string, rect:object|null}[]>} lines, or [] on failure
 */
async function _fetchPageLines(pi, apiBase, order, fetchFn, query = '') {
    const res = await fetchFn(`${apiBase}/records/${pi}/pages/${order + 1}/text/${query}`);
    if (!res.ok) {
        return [];
    }
    return parsePageLines(await res.json());
}

/**
 * Fetches the OCR fulltext of a single page as structured lines (chars + box).
 *
 * @param {string} pi
 * @param {string} apiBase
 * @param {number} order    0-based page order; the endpoint is 1-based, so order + 1
 * @param {Function} fetchFn
 * @returns {Promise<{id:string, chars:string, rect:object|null}[]>} lines, or [] on failure
 */
export function loadPageLines(pi, apiBase, order, fetchFn = fetch) {
    return _fetchPageLines(pi, apiBase, order, fetchFn);
}

/**
 * Granularity dispatch for the hover-linking panel: `'line'` returns the page's
 * OCR lines, `'word'` fetches `?granularity=word` and drops blank words (ALTO spaces).
 *
 * @param {string} pi
 * @param {string} apiBase
 * @param {number} order
 * @param {string} granularity 'line' | 'word'
 * @param {Function} fetchFn
 * @returns {Promise<Array>}
 */
export async function loadPageRegions(pi, apiBase, order, granularity, fetchFn = fetch) {
    if (granularity === 'word') {
        const words = await _fetchPageLines(pi, apiBase, order, fetchFn, '?granularity=word');
        return words.filter((r) => (r.chars || '').trim() !== '');
    }
    return loadPageLines(pi, apiBase, order, fetchFn);
}
