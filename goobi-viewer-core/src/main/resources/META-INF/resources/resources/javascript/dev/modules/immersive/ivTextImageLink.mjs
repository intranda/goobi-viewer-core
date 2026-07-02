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
 *
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
 * @param {{offsetTop:number, height:number, scrollTop:number, clientHeight:number, scrollHeight:number}} metrics
 * @returns {number|null}
 */
export function scrollTopToReveal(metrics) {
    const top = metrics.offsetTop;
    const bottom = metrics.offsetTop + metrics.height;
    if (top >= metrics.scrollTop && bottom <= metrics.scrollTop + metrics.clientHeight) return null;
    const target = metrics.offsetTop - metrics.clientHeight / 2 + metrics.height / 2;
    const max = Math.max(0, metrics.scrollHeight - metrics.clientHeight);
    return Math.min(Math.max(0, target), max);
}

/**
 * Cumulative offsetTop of `el` relative to `container` (walks the offsetParent
 * chain), for scrolling panel content into view without moving the page.
 *
 * @param {HTMLElement} el
 * @param {HTMLElement} container
 * @returns {number}
 */
export function offsetTopWithin(el, container) {
    let top = 0;
    for (let n = el; n && n !== container; n = n.offsetParent) top += n.offsetTop;
    return top;
}

/**
 * Wires bidirectional hover highlighting between the panel line spans (inside
 * `box`) and the image region overlays (`regionEls`) that share the same
 * `data-iv-region-id`. Hover on either side toggles `is-linked-active` on both.
 * Deliberately pointer-only: the highlight is a supplementary cue, the text
 * itself stays fully readable and reachable without it.
 *
 * @param {object} opts
 * @param {HTMLElement} opts.box fulltext panel element containing the line spans
 * @param {Map<string, HTMLElement>} opts.regionEls image overlay elements, indexed by id
 * @param {HTMLElement} [opts.scrollContainer] scrollable panel; when set, hovering an
 *   image overlay scrolls its line into view if not fully visible
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
            offsetTop: offsetTopWithin(span, scrollContainer),
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
 * Groups consecutive word regions into lines by vertical overlap of their boxes
 * (robust against within-line top variation from ascenders/descenders). Two
 * consecutive words share a line when their vertical ranges overlap by more than
 * half the shorter box height; words without a rect stay on the current line.
 *
 * @param {{id:string, chars:string, rect:{x:number,y:number,w:number,h:number}|null}[]} regions
 * @returns {Array<Array<object>>}
 */
export function groupWordsIntoLines(regions) {
    const lines = [];
    let currentLine = null;
    let prevTop = null;
    let prevBottom = null;
    for (const r of regions || []) {
        const rect = r && r.rect;
        const top = rect ? rect.y : null;
        const bottom = rect ? rect.y + rect.h : null;
        let newLine;
        if (currentLine === null) {
            newLine = true;
        } else if (top === null || prevTop === null) {
            newLine = false;
        } else {
            const overlap = Math.min(prevBottom, bottom) - Math.max(prevTop, top);
            const minHeight = Math.min(prevBottom - prevTop, bottom - top);
            newLine = overlap <= 0.5 * minHeight;
        }
        if (newLine) {
            currentLine = [r];
            lines.push(currentLine);
        } else {
            currentLine.push(r);
        }
        if (top !== null) {
            prevTop = top;
            prevBottom = bottom;
        }
    }
    return lines;
}

/**
 * Builds word spans grouped into line blocks (same layout as line mode, but each
 * word is an individually hoverable `<span data-iv-region-id>`). Blank words
 * (ALTO spaces) are skipped; a single space separates rendered words.
 *
 * @param {{id:string, chars:string, rect:object|null}[]} regions
 * @returns {DocumentFragment}
 */
export function buildWordSpans(regions) {
    const frag = document.createDocumentFragment();
    for (const words of groupWordsIntoLines(regions)) {
        const line = document.createElement('span');
        line.className = 'immersive__fulltext-line';
        let first = true;
        for (const w of words) {
            const text = (w && w.chars) || '';
            if (!text.trim()) {
                continue;
            }
            if (!first) {
                line.appendChild(document.createTextNode(' '));
            }
            const span = document.createElement('span');
            span.className = 'immersive__fulltext-word';
            span.dataset.ivRegionId = w.id;
            span.textContent = text;
            line.appendChild(span);
            first = false;
        }
        if (line.childNodes.length) {
            frag.appendChild(line);
        }
    }
    return frag;
}
