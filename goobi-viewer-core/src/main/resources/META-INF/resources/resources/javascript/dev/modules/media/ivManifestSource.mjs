import { parseManifestImageServices } from './iv_imageWindow.mjs';

const cache = new Map();

/**
 * Fetches the IIIF Presentation manifest for a given PI and returns the
 * ordered list of image-service base URLs for all pages.
 *
 * The result Promise is memoized per pi so repeated calls never re-fetch.
 *
 * @param {string} pi       - Goobi viewer process identifier.
 * @param {string} apiBase  - Base URL of the REST API (no trailing slash).
 * @param {Function} fetchFn - fetch-compatible function (injectable for tests).
 * @returns {Promise<string[]>}
 */
export function loadPageServices(pi, apiBase, fetchFn = fetch) {
    if (cache.has(pi)) {
        return cache.get(pi);
    }

    const url = `${apiBase}/records/${pi}/manifest`;
    const promise = fetchFn(url).then((res) => {
        if (!res.ok) {
            throw new Error(`Failed to load manifest for "${pi}": HTTP ${res.status}`);
        }
        return res.json().then((manifest) => parseManifestImageServices(manifest));
    });

    cache.set(pi, promise);

    // Evict on failure so a transient error doesn't poison the cache; the rejection is rethrown.
    return promise.catch((e) => {
        cache.delete(pi);
        throw e;
    });
}

/**
 * Clears the module-level manifest cache. Intended for use in tests only.
 */
export function _clearCache() {
    cache.clear();
}

/**
 * Maps a IIIF/W3C `sc:AnnotationList` (one annotation per OCR text line, as returned by
 * the page-text endpoint) to plain text with the lines joined by newlines.
 *
 * @param {object} annotationList - the parsed `sc:AnnotationList` JSON.
 * @returns {string} the page text, or '' when there are no text lines.
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
 * @param {string} pi        - Goobi viewer process identifier.
 * @param {string} apiBase   - Base URL of the REST API (no trailing slash).
 * @param {number} order     - 0-based page order; the endpoint is 1-based, so order + 1.
 * @param {Function} fetchFn - fetch-compatible function (injectable for tests).
 * @returns {Promise<string>} the page text, or '' if the request fails / has no text.
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
 * Maps a IIIF/W3C `sc:AnnotationList` (one annotation per OCR text line) to
 * structured lines that keep the line box from the annotation `on` selector
 * (a `xywh=x,y,w,h` fragment, found on `on` as a string, on `on['@id']`, or in
 * `on.selector.value`). Lines without a box get `rect: null`. The `id` is the
 * annotation `@id` when present, else a per-page index fallback `line-${i}`.
 *
 * @param {object} annotationList - the parsed `sc:AnnotationList` JSON.
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
 * Fetches the OCR fulltext of a single page as structured lines (chars + box).
 *
 * @param {string} pi
 * @param {string} apiBase
 * @param {number} order    - 0-based page order; the endpoint is 1-based, so order + 1.
 * @param {Function} fetchFn
 * @returns {Promise<{id:string, chars:string, rect:object|null}[]>} lines, or [] on failure.
 */
export async function loadPageLines(pi, apiBase, order, fetchFn = fetch) {
    const res = await fetchFn(`${apiBase}/records/${pi}/pages/${order + 1}/text/`);
    if (!res.ok) {
        return [];
    }
    return parsePageLines(await res.json());
}

/**
 * Granularity dispatch for the hover-linking panel. `'line'` returns the page's
 * OCR lines with boxes; `'word'` fetches the same page-text endpoint with
 * `?granularity=word` and parses the result through `parsePageLines`.
 *
 * @param {string} pi
 * @param {string} apiBase
 * @param {number} order
 * @param {string} granularity - 'line' | 'word'
 * @param {Function} fetchFn
 * @returns {Promise<Array>}
 */
export async function loadPageRegions(pi, apiBase, order, granularity, fetchFn = fetch) {
    if (granularity === 'word') {
        const res = await fetchFn(`${apiBase}/records/${pi}/pages/${order + 1}/text/?granularity=word`);
        if (!res.ok) {
            return [];
        }
        return parsePageLines(await res.json());
    }
    return loadPageLines(pi, apiBase, order, fetchFn);
}
