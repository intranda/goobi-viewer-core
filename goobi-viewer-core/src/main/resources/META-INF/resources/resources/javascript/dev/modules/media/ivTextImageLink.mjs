/** OCR line ↔ image region hover linking for the immersive fulltext panel. */

const ACTIVE_CLASS = 'is-linked-active';

/**
 * Builds the fulltext line spans for the panel: one `<span>` per line, tagged
 * with `data-iv-region-id` to match its image overlay. Text is set via
 * `textContent`, so OCR content can never inject markup.
 *
 * @param {{id:string, chars:string}[]} lines
 * @returns {DocumentFragment}
 */
export function buildLineSpans(lines) {
    const frag = document.createDocumentFragment();
    (lines || []).forEach((line) => {
        const span = document.createElement('span');
        span.className = 'immersive__fulltext-line';
        span.dataset.ivRegionId = line.id;
        span.textContent = line.chars || '';
        frag.appendChild(span);
    });
    return frag;
}

/**
 * Indexes elements by their `data-iv-region-id` (elements without one are skipped).
 * @param {Iterable<HTMLElement>} elements
 * @returns {Map<string, HTMLElement>}
 */
export function indexById(elements) {
    const map = new Map();
    for (const el of elements) {
        const id = el.dataset && el.dataset.ivRegionId;
        if (id) map.set(id, el);
    }
    return map;
}

/**
 * Computes the scrollTop needed to center a line within its scroll container,
 * or null if the line is already fully visible. Pure (numbers only) so it is
 * testable without a layout engine.
 *
 * @param {{offsetTop:number, height:number, scrollTop:number, clientHeight:number, scrollHeight:number}} m
 * @returns {number|null}
 */
export function scrollTopToReveal(m) {
    const top = m.offsetTop;
    const bottom = m.offsetTop + m.height;
    if (top >= m.scrollTop && bottom <= m.scrollTop + m.clientHeight) return null;
    const target = m.offsetTop - m.clientHeight / 2 + m.height / 2;
    const max = Math.max(0, m.scrollHeight - m.clientHeight);
    return Math.min(Math.max(0, target), max);
}

/**
 * Cumulative offsetTop of `el` relative to `container`, mirroring the panel
 * scroll math used elsewhere in the immersive view.
 * @param {HTMLElement} el
 * @param {HTMLElement} container
 * @returns {number}
 */
function _offsetTopWithin(el, container) {
    let top = 0;
    for (let n = el; n && n !== container; n = n.offsetParent) top += n.offsetTop;
    return top;
}

/**
 * Wires bidirectional hover highlighting between the panel line spans (inside
 * `box`) and the image region overlays (`regionEls`) that share the same
 * `data-iv-region-id`. Hover on either side toggles `is-linked-active` on both.
 *
 * @param {object} opts
 * @param {HTMLElement} opts.box - fulltext panel element containing the line spans.
 * @param {Map<string, HTMLElement>} opts.regionEls - image overlay elements, already indexed by id.
 * @param {HTMLElement} [opts.scrollContainer] - scrollable panel; when set, hovering an
 *   image overlay scrolls its line into view if not fully visible.
 * @returns {{destroy: function():void}}
 */
export function mountTextImageLink({ box, regionEls, scrollContainer }) {
    const spanEls = indexById(box.querySelectorAll('[data-iv-region-id]'));
    const regions = regionEls || new Map();
    const listeners = [];

    const setActive = (id, on) => {
        const span = spanEls.get(id);
        const overlay = regions.get(id);
        if (span) span.classList.toggle(ACTIVE_CLASS, on);
        if (overlay) overlay.classList.toggle(ACTIVE_CLASS, on);
    };

    const revealSpan = (id) => {
        if (!scrollContainer) return;
        const span = spanEls.get(id);
        if (!span) return;
        const target = scrollTopToReveal({
            offsetTop: _offsetTopWithin(span, scrollContainer),
            height: span.offsetHeight,
            scrollTop: scrollContainer.scrollTop,
            clientHeight: scrollContainer.clientHeight,
            scrollHeight: scrollContainer.scrollHeight,
        });
        if (target !== null) scrollContainer.scrollTop = target;
    };

    const bind = (el, id, isOverlay) => {
        const enter = () => {
            setActive(id, true);
            if (isOverlay) revealSpan(id);
        };
        const leave = () => setActive(id, false);
        el.addEventListener('mouseenter', enter);
        el.addEventListener('mouseleave', leave);
        listeners.push([el, enter, leave]);
    };

    spanEls.forEach((el, id) => bind(el, id, false));
    regions.forEach((el, id) => bind(el, id, true));

    return {
        destroy() {
            listeners.forEach(([el, enter, leave]) => {
                el.removeEventListener('mouseenter', enter);
                el.removeEventListener('mouseleave', leave);
            });
            listeners.length = 0;
        },
    };
}

/**
 * Groups consecutive word regions into lines by their `rect.y` (within
 * `tolerance` px). Order preserved; a word without a rect stays on the current
 * line. Pure.
 *
 * @param {{id:string, chars:string, rect:{x:number,y:number,w:number,h:number}|null}[]} regions
 * @param {number} tolerance
 * @returns {Array<Array<object>>}
 */
export function groupWordsIntoLines(regions, tolerance = 8) {
    const lines = [];
    let current = null;
    let lastY = null;
    for (const r of regions || []) {
        const y = r && r.rect ? r.rect.y : null;
        const newLine = current === null || (y !== null && lastY !== null && Math.abs(y - lastY) > tolerance);
        if (newLine) {
            current = [r];
            lines.push(current);
        } else {
            current.push(r);
        }
        if (y !== null) lastY = y;
    }
    return lines;
}

/**
 * Builds word spans grouped into line blocks (preserving the original line
 * layout). Each word is a hoverable `<span data-iv-region-id>`; text via
 * `textContent`. Reuses the `immersive__fulltext-line` block class.
 *
 * @param {{id:string, chars:string, rect:object|null}[]} regions
 * @returns {DocumentFragment}
 */
export function buildWordSpans(regions) {
    const frag = document.createDocumentFragment();
    groupWordsIntoLines(regions).forEach((words) => {
        const line = document.createElement('span');
        line.className = 'immersive__fulltext-line';
        words.forEach((w, i) => {
            const span = document.createElement('span');
            span.className = 'immersive__fulltext-word';
            span.dataset.ivRegionId = w.id;
            span.textContent = w.chars || '';
            line.appendChild(span);
            if (i < words.length - 1) line.appendChild(document.createTextNode(' '));
        });
        frag.appendChild(line);
    });
    return frag;
}
