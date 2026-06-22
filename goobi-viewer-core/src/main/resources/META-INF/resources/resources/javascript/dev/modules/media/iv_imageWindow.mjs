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
 * Extracts the ordered list of IIIF image-service base IDs from a manifest.
 *
 * Supports IIIF Presentation API v2 (sequences/canvases) and v3 (items).
 * Canvases that lack a resolvable service id are silently skipped.
 *
 * @param {object} manifest - Parsed IIIF Presentation manifest.
 * @returns {string[]} Ordered array of image-service id strings.
 */
export function parseManifestImageServices(manifest) {
    if (!manifest || typeof manifest !== 'object') return [];

    // IIIF v2: manifest.sequences[0].canvases
    if (Array.isArray(manifest.sequences) && manifest.sequences.length > 0) {
        const canvases = manifest.sequences[0].canvases;
        if (!Array.isArray(canvases)) return [];

        const ids = [];
        for (const canvas of canvases) {
            try {
                const service = canvas.images[0].resource.service;
                const id = resolveServiceId(service, '@id', 'id');
                if (id !== null) ids.push(id);
            } catch {
                // canvas structure incomplete — skip
            }
        }
        return ids;
    }

    // IIIF v3: manifest.items (canvases)
    if (Array.isArray(manifest.items) && manifest.items.length > 0) {
        const ids = [];
        for (const canvas of manifest.items) {
            try {
                const service = canvas.items[0].items[0].body.service;
                const id = resolveServiceId(service, 'id', '@id');
                if (id !== null) ids.push(id);
            } catch {
                // canvas structure incomplete — skip
            }
        }
        return ids;
    }

    return [];
}

/**
 * Computes a contiguous slice of page indices centered on the current page,
 * clamped to [0, total).
 *
 * @param {number} currentIndex - 0-based index of the current page.
 * @param {number} total        - Total number of pages (>= 0).
 * @param {number} windowSize   - Maximum number of pages in the window (>= 1).
 * @returns {{ start: number, end: number, indexInWindow: number }}
 *   start         - inclusive 0-based start of the window
 *   end           - exclusive end of the window (window = [start, end))
 *   indexInWindow - position of currentIndex within the window
 */
export function computeWindow(currentIndex, total, windowSize) {
    if (total === 0) {
        return { start: 0, end: 0, indexInWindow: 0 };
    }

    const size = Math.min(windowSize, total);

    // Center the window on currentIndex.
    let start = currentIndex - Math.floor(size / 2);
    let end = start + size;

    // Clamp so the window stays within [0, total].
    if (start < 0) {
        start = 0;
        end = size;
    } else if (end > total) {
        end = total;
        start = total - size;
    }

    return { start, end, indexInWindow: currentIndex - start };
}
