/**
 * Pure helper functions for the immersive image viewer's tile-source window.
 *
 * No DOM, no network, no OpenSeadragon dependencies — safe to unit-test and
 * to import in any context.
 */

/**
 * Resolve a service object or array to a single id string.
 * Returns null when no id can be found.
 *
 * @param {object|Array} service
 * @param {'@id'|'id'} primaryKey   - preferred property name
 * @param {'@id'|'id'} fallbackKey  - secondary property name
 * @returns {string|null}
 */
function resolveServiceId(service, primaryKey, fallbackKey) {
    if (!service) return null;
    const entry = Array.isArray(service) ? service[0] : service;
    if (!entry) return null;
    const id = entry[primaryKey] || entry[fallbackKey];
    return typeof id === 'string' && id.length > 0 ? id : null;
}

/**
 * Resolve a IIIF canvas `label` to a single display string. Handles a plain string,
 * a v2 `{'@value'}` object, an array of either, and a v3 language map
 * (`{ de: ['…'], none: ['…'] }`). Returns the first non-empty value found, or ''.
 *
 * @param {string|object|Array} label
 * @returns {string}
 */
function resolveCanvasLabel(label) {
    if (label == null) return '';
    if (typeof label === 'string') return label;
    if (Array.isArray(label)) {
        for (const item of label) {
            const v = resolveCanvasLabel(item);
            if (v) return v;
        }
        return '';
    }
    if (typeof label === 'object') {
        if (typeof label['@value'] === 'string' && label['@value'].length > 0) return label['@value'];
        for (const v of Object.values(label)) {
            const resolved = resolveCanvasLabel(v);
            if (resolved) return resolved;
        }
    }
    return '';
}

/**
 * Extracts ordered `{id, label}` entries from a manifest's canvases (v2 sequences/
 * canvases or v3 items). Canvases without a resolvable image-service id are skipped,
 * so the result stays index-aligned for both the service URLs and the page labels.
 *
 * @param {object} manifest
 * @returns {Array<{id: string, label: string}>}
 */
function parseManifestCanvasEntries(manifest) {
    if (!manifest || typeof manifest !== 'object') return [];

    if (Array.isArray(manifest.sequences) && manifest.sequences.length > 0) {
        const canvases = manifest.sequences[0].canvases;
        if (!Array.isArray(canvases)) return [];

        const entries = [];
        for (const canvas of canvases) {
            try {
                const id = resolveServiceId(canvas.images[0].resource.service, '@id', 'id');
                if (id !== null) entries.push({ id, label: resolveCanvasLabel(canvas.label) });
            } catch {}
        }
        return entries;
    }

    if (Array.isArray(manifest.items) && manifest.items.length > 0) {
        const entries = [];
        for (const canvas of manifest.items) {
            try {
                const id = resolveServiceId(canvas.items[0].items[0].body.service, 'id', '@id');
                if (id !== null) entries.push({ id, label: resolveCanvasLabel(canvas.label) });
            } catch {}
        }
        return entries;
    }

    return [];
}

/**
 * Extracts the ordered list of IIIF image-service base IDs from a manifest.
 *
 * Supports IIIF Presentation API v2 (sequences/canvases) and v3 (items).
 * Canvases that lack a resolvable service id are silently skipped.
 *
 * @param {object} manifest - Parsed IIIF Presentation manifest.
 * @returns {string[]} Ordered array of image-service id strings.
 */
export function parseManifestImageServices(manifest) {
    return parseManifestCanvasEntries(manifest).map((e) => e.id);
}

/**
 * Extracts the ordered list of canvas labels from a manifest, index-aligned with
 * {@link parseManifestImageServices}. Canvases without a resolvable label yield ''.
 *
 * @param {object} manifest - Parsed IIIF Presentation manifest.
 * @returns {string[]} Ordered array of label strings (one per page).
 */
export function parseManifestPageLabels(manifest) {
    return parseManifestCanvasEntries(manifest).map((e) => e.label);
}

/**
 * Returns the 0-based page indices shown together for the spread that contains
 * `order`. Book layout (LTR): the cover (page 0) stands alone, then pages are
 * paired (1,2),(3,4),… An odd final page stands alone. Pure + tested.
 *
 * @param {number} order  0-based page index
 * @param {number} total  total page count
 * @param {{coverAlone?:boolean}} [opts]
 * @returns {number[]} one or two page indices, ascending
 */
export function computeSpread(order, total, { coverAlone = true } = {}) {
    const o = Math.max(0, Math.min(order, total - 1));
    let leader;
    if (coverAlone) {
        if (o === 0) return [0];
        leader = 1 + 2 * Math.floor((o - 1) / 2);
    } else {
        leader = o - (o % 2);
    }
    return leader + 1 <= total - 1 ? [leader, leader + 1] : [leader];
}

/**
 * Page indices that make up the frame containing `order`.
 * Single mode: [order]. Double mode: the spread (computeSpread). Pure.
 *
 * @param {number} order  0-based page index
 * @param {number} total  total page count
 * @param {{double?:boolean}} [opts]
 * @returns {number[]} one or two page indices, ascending
 */
export function framePages(order, total, { double = false } = {}) {
    const o = Math.max(0, Math.min(order, total - 1));
    return double ? computeSpread(o, total) : [o];
}

/**
 * Page indices to keep resident (current frame + the immediately adjacent
 * frames) so neighbour navigation is instant. Deduped, ascending, in range. Pure.
 *
 * @param {number} order  0-based page index
 * @param {number} total  total page count
 * @param {{double?:boolean}} [opts]
 * @returns {number[]} sorted, deduplicated page indices
 */
export function residentPages(order, total, { double = false } = {}) {
    const here = framePages(order, total, { double });
    const prev = framePages(here[0] - 1, total, { double });
    const next = framePages(here[here.length - 1] + 1, total, { double });
    return [...new Set([...prev, ...here, ...next])].filter((p) => p >= 0 && p < total).sort((a, b) => a - b);
}
