import { parseManifestImageServices } from './iv_imageWindow.mjs';

// Per-pi cache of in-flight/resolved Promises.
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

    // Evict on rejection so a transient failure does not permanently poison
    // the cache; a later call will retry. The original rejection is rethrown.
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
