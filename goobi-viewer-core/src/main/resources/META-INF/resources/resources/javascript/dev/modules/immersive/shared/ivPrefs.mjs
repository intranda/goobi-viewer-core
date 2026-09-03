/** Persisted viewer preferences (localStorage) with tolerant read/write. */

export const THUMB_SIZE_KEY = 'immersive-thumb-size';

export const THUMB_SIZE_MAX = 2;

const THUMB_SIZE_DEFAULT = 1;

/**
 * Normalizes a stored thumbnail-size value to a valid grid step. Values are
 * parsed with parseInt, so leading-numeric strings are accepted; anything
 * else falls back to the default (medium). Pure + tested.
 *
 * @param {*} value
 * @returns {number} integer step between 0 and THUMB_SIZE_MAX
 */
export function normalizeThumbSizeStep(value) {
    const n = parseInt(value, 10);
    return Number.isInteger(n) && n >= 0 && n <= THUMB_SIZE_MAX ? n : THUMB_SIZE_DEFAULT;
}

/**
 * Reads the persisted thumbnail-size step. Falls back to the default when
 * the storage operations throw (private mode) or the value is invalid.
 *
 * @param {Storage} storage  e.g. window.localStorage
 * @returns {number} integer step between 0 and THUMB_SIZE_MAX
 */
export function readThumbSizeStep(storage) {
    try {
        return normalizeThumbSizeStep(storage.getItem(THUMB_SIZE_KEY));
    } catch {
        return THUMB_SIZE_DEFAULT;
    }
}

/**
 * Persists the thumbnail-size step; invalid steps are normalized. Storage
 * errors (private mode, quota) are swallowed.
 *
 * @param {Storage} storage
 * @param {number|string} step
 */
export function writeThumbSizeStep(storage, step) {
    try {
        storage.setItem(THUMB_SIZE_KEY, String(normalizeThumbSizeStep(step)));
    } catch {}
}

export const ANNOTATIONS_KEY = 'immersive-annotations';

/**
 * Reads the persisted annotations toggle. Only the literal 'true' enables it;
 * storage errors (private mode) fall back to off. Pure + tested.
 *
 * @param {Storage} storage  e.g. window.localStorage
 * @returns {boolean}
 */
export function readAnnotationsEnabled(storage) {
    try {
        return storage.getItem(ANNOTATIONS_KEY) === 'true';
    } catch {
        return false;
    }
}

/**
 * Persists the annotations toggle; storage errors are swallowed.
 *
 * @param {Storage} storage
 * @param {boolean} enabled
 */
export function writeAnnotationsEnabled(storage, enabled) {
    try {
        storage.setItem(ANNOTATIONS_KEY, String(!!enabled));
    } catch {}
}
