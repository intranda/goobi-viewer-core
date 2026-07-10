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
 * Detects whether an image-service carries a IIIF authentication/probe service,
 * which the server attaches only when the current user may not view the image
 * (see SequenceBuilder). Its presence is the server-authoritative per-page
 * "image restricted" signal.
 *
 * @param {object|Array} service the image resource's `service` value
 * @returns {boolean} true when a nested auth/probe service is present
 */
function _serviceIsRestricted(service) {
    const entry = Array.isArray(service) ? service[0] : service;
    if (!entry || typeof entry !== 'object') return false;
    const nested = entry.service;
    const list = Array.isArray(nested) ? nested : nested ? [nested] : [];
    return list.some((s) => {
        if (!s || typeof s !== 'object') return false;
        const type = String(s.type || s['@type'] || '');
        const profile = String(s.profile || '');
        const context = String(s['@context'] || '');
        return /auth/i.test(type) || /auth/i.test(profile) || /\/auth\//i.test(context);
    });
}

/**
 * Extracts ordered `{id, label, restricted}` entries from a manifest's canvases
 * (v2 sequences/canvases or v3 items). Canvases without a resolvable image-service
 * id are skipped entirely, so service URLs, page labels and access flags stay
 * index-aligned.
 *
 * @param {object} manifest
 * @param {string} [lang] preferred label language
 * @returns {Array<{id: string, label: string, restricted: boolean}>}
 */
function _parseManifestCanvasEntries(manifest, lang) {
    if (!manifest || typeof manifest !== 'object') return [];

    if (Array.isArray(manifest.sequences) && manifest.sequences.length > 0) {
        const canvases = manifest.sequences[0].canvases;
        if (!Array.isArray(canvases)) return [];

        const entries = [];
        for (const canvas of canvases) {
            try {
                const service = canvas.images[0].resource.service;
                const id = _resolveServiceId(service, '@id', 'id');
                if (id !== null) {
                    entries.push({ id, label: _resolveCanvasLabel(canvas.label, lang), restricted: _serviceIsRestricted(service) });
                }
            } catch {}
        }
        return entries;
    }

    if (Array.isArray(manifest.items) && manifest.items.length > 0) {
        const entries = [];
        for (const canvas of manifest.items) {
            try {
                const service = canvas.items[0].items[0].body.service;
                const id = _resolveServiceId(service, 'id', '@id');
                if (id !== null) {
                    entries.push({ id, label: _resolveCanvasLabel(canvas.label, lang), restricted: _serviceIsRestricted(service) });
                }
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
 * Extracts the ordered per-page "image restricted" flags, index-aligned with
 * {@link parseManifestImageServices}. true means the page carries an auth service
 * (the user may not view the image); its tiles will 403.
 *
 * @param {object} manifest parsed IIIF Presentation manifest
 * @returns {boolean[]}
 */
export function parseManifestPageAccess(manifest) {
    return _parseManifestCanvasEntries(manifest).map((e) => e.restricted);
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

/**
 * Returns the ordered per-page "image restricted" flags for all pages,
 * index-aligned with {@link loadPageServices}. Shares the memoized manifest fetch.
 *
 * @param {string} pi        Goobi viewer record identifier
 * @param {string} apiBase   base URL of the REST API (no trailing slash)
 * @param {Function} fetchFn fetch-compatible function (injectable for tests)
 * @returns {Promise<boolean[]>}
 */
export function loadPageAccess(pi, apiBase, fetchFn = fetch) {
    return loadManifest(pi, apiBase, fetchFn).then(parseManifestPageAccess);
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

/** Whether the rect's centre point lies inside `outer`. */
function _centerInside(rect, outer) {
    if (!rect || !outer) return false;
    const cx = rect.x + rect.w / 2;
    const cy = rect.y + rect.h / 2;
    return cx >= outer.x && cx < outer.x + outer.w && cy >= outer.y && cy < outer.y + outer.h;
}

/**
 * The container in `parents` whose rect holds the region's centre; overlapping
 * containers resolve to the smallest (most specific) one. Null without a match.
 */
function _findParent(region, parents) {
    let best = null;
    for (const p of parents) {
        if (!_centerInside(region.rect, p.rect)) continue;
        if (!best || p.rect.w * p.rect.h < best.rect.w * best.rect.h) best = p;
    }
    return best;
}

/** True when the block list is just the line list again (line fallback of APIs without block support). */
function _blocksAreLines(blocks, lines) {
    if (!blocks.length || blocks.length !== lines.length) return false;
    return blocks.every((b, i) => {
        const a = b.rect;
        const c = lines[i].rect;
        return (!a && !c) || (a && c && a.x === c.x && a.y === c.y && a.w === c.w && a.h === c.h);
    });
}

/**
 * Nests the three flat OCR levels into blocks > lines > words by geometric
 * containment (centre-inside, smallest container wins). Reading order follows
 * the line order: consecutive lines sharing an owner form one group, lines
 * without a containing block collect in synthetic blocks (`id: null`), and a
 * line without a rect stays with its predecessor. Blank words and words outside
 * every line are dropped; a degenerate block list that merely mirrors the lines
 * (line fallback of APIs without block granularity) is ignored. Pure + tested.
 *
 * @param {object} levels
 * @param {{id:string, chars:string, rect:object|null}[]} levels.blocks
 * @param {{id:string, chars:string, rect:object|null}[]} levels.lines
 * @param {{id:string, chars:string, rect:object|null}[]} levels.words
 * @returns {Array<{id:string|null, chars:string, rect:object|null, lines:Array}>}
 */
export function nestTextLevels({ blocks = [], lines = [], words = [] }) {
    const realBlocks = _blocksAreLines(blocks, lines) ? [] : blocks.filter((b) => b.rect);
    const visibleWords = words.filter((w) => (w.chars || '').trim() !== '');

    const result = [];
    let currentOwner;
    let group = null;
    for (const line of lines) {
        const owner = line.rect ? _findParent(line, realBlocks) : group ? currentOwner : null;
        if (!group || owner !== currentOwner) {
            currentOwner = owner;
            group = owner ? { ...owner, lines: [] } : { id: null, chars: '', rect: null, lines: [] };
            result.push(group);
        }
        group.lines.push({ ...line, words: [] });
    }

    const allLines = result.flatMap((b) => b.lines);
    for (const word of visibleWords) {
        const line = _findParent(word, allLines);
        if (line) line.words.push({ ...word });
    }
    return result;
}

/**
 * Flattens nested text levels into overlay descriptors — blocks first, then
 * lines, then words, so overlays stack with words on top. Each entry carries
 * its `level` and the `parentId` for cascading highlights; synthetic blocks
 * and regions without a rect are skipped (nothing to draw). Pure + tested.
 *
 * @param {Array} blocks  return value of {@link nestTextLevels}
 * @returns {{id:string, rect:object, level:string, parentId:string|null}[]}
 */
export function flattenTextLevels(blocks) {
    const flat = [];
    const push = (region, level, parentId) => {
        if (region.id && region.rect) flat.push({ id: region.id, rect: region.rect, level, parentId });
    };
    (blocks || []).forEach((b) => push(b, 'block', null));
    (blocks || []).forEach((b) => {
        const blockId = b.id && b.rect ? b.id : null;
        b.lines.forEach((l) => push(l, 'line', blockId));
    });
    (blocks || []).forEach((b) => {
        b.lines.forEach((l) => {
            const lineId = l.id && l.rect ? l.id : null;
            l.words.forEach((w) => push(w, 'word', lineId));
        });
    });
    return flat;
}

/**
 * Fetches a page's OCR text at block, line and word granularity in parallel
 * and nests it via {@link nestTextLevels}. A failing level degrades gracefully
 * to the remaining ones (older APIs without block support fall back to line
 * annotations, which the nesting detects and ignores).
 *
 * @param {string} pi
 * @param {string} apiBase
 * @param {number} order    0-based page order
 * @param {Function} fetchFn
 * @returns {Promise<Array>} nested blocks, see {@link nestTextLevels}
 */
export async function loadPageTextLevels(pi, apiBase, order, fetchFn = fetch) {
    const [blocks, lines, words] = await Promise.all([
        _fetchPageLines(pi, apiBase, order, fetchFn, '?granularity=block'),
        _fetchPageLines(pi, apiBase, order, fetchFn),
        _fetchPageLines(pi, apiBase, order, fetchFn, '?granularity=word'),
    ]);
    return nestTextLevels({ blocks, lines, words });
}
