/** ALTO layout-tag annotations: toggle state, inline markup, margin note column and image overlay regions. */

import { isTypingTarget } from '../shared/ivPlatform.mjs';
import { readAnnotationsEnabled, writeAnnotationsEnabled } from '../shared/ivPrefs.mjs';
import { offsetTopWithin, scrollTopToReveal, styleClassTokens } from '../shared/ivDom.mjs';
import { isOverlayOpen } from '../shared/ivSelectors.mjs';

/** localStorage key persisting the annotations toggle, following the immersive-* key convention. */
/** State class on the .immersive root while annotations are switched on (panel markup AND image overlays key off it). */
const STATE_CLASS = '-annotations-on';

/**
 * Below this panel content width (px) the margin column collapses and notes render inline
 * under their anchor line. With annotations on, the LESS min-width (560px panel − 32px
 * padding = 528px content) keeps desktop above this threshold, so the collapsed mode is
 * the small-screen fallback only; keep 560 − padding > this value or the ResizeObserver
 * would flip-flop (see @immersive-anno-panel-min-width in ivAnnotations.less).
 */
const COLLAPSE_BELOW = 460;

/**
 * Vertical gap (px) between stacked margin notes. Kept small on purpose: entry
 * height + gap must stay at or below the print row height (~24px), or runs of
 * consecutive annotations can never sit on their own lines.
 */
const NOTE_GAP = 2;

/**
 * Presentation mapping keyed by the SEMANTIC types the layout endpoint derives from the
 * configured `altoLayoutTags` mappings (raw labels stay in the payload for reference).
 * Author cards additionally carry the configured styleClass verbatim, so arbitrary
 * customer vocabularies are styled via config + theme CSS without client changes; the
 * default author colors hook onto the default styleClasses in ivAnnotations.less.
 */
const WORD_TYPE_CLASSES = {
    underline: 'immersive__anno-word-underline',
    strikethrough: 'immersive__anno-word-strike',
};

/** Drawn-line types rendered as image overlays; lines without a mapped type stay hidden. */
const LINE_TYPE_MODIFIERS = {
    underline: 'underline',
    strikethrough: 'strike',
    mark: 'mark',
};

/**
 * Extracts the ALTO element id from a page text annotation URI. Those URIs end
 * in `alto_{pi}_{pageNo}_{elementId}` (plus an optional query); the element id
 * itself may contain underscores, so the prefix is matched as a whole. Null
 * for synthetic ids and annotations of other pages. Pure + tested.
 *
 * @param {string} regionId  a data-iv-region-id value (annotation @id)
 * @param {string} pi
 * @param {number|string} pageNo  1-based page number
 * @returns {string|null}
 */
export function altoElementId(regionId, pi, pageNo) {
    if (!regionId) return null;
    const id = String(regionId).split('?')[0].replace(/\/+$/, '');
    const marker = `alto_${pi}_${pageNo}_`;
    const at = id.lastIndexOf(marker);
    if (at === -1) return null;
    const element = id.slice(at + marker.length);
    return element.length && !element.includes('/') ? element : null;
}

/**
 * Whether a layout result carries any annotation data. Pure + tested.
 *
 * @param {{blocks:Array, words:Array, lines:Array}|null} layout
 * @returns {boolean}
 */
export function hasAnnotations(layout) {
    if (!layout) return false;
    return !!((layout.blocks && layout.blocks.length) || (layout.words && layout.words.length) || (layout.lines && layout.lines.length));
}

/**
 * CSS classes for a word's semantic types (underline/strikethrough); unknown
 * types are ignored, duplicates collapse. Pure + tested.
 *
 * @param {string[]} [types]  semantic types from the layout endpoint
 * @returns {string[]}
 */
export function wordTypeClasses(types) {
    const classes = [];
    (types || []).forEach((type) => {
        const cls = WORD_TYPE_CLASSES[type];
        if (cls && !classes.includes(cls)) classes.push(cls);
    });
    return classes;
}

/**
 * Overlay modifier for a drawn line's semantic types — underline, strike or
 * mark; null when none of its types is rendered. Pure + tested.
 *
 * @param {string[]} [types]  semantic types from the layout endpoint
 * @returns {string|null}
 */
export function lineTypeModifier(types) {
    for (const type of types || []) {
        if (LINE_TYPE_MODIFIERS[type]) return LINE_TYPE_MODIFIERS[type];
    }
    return null;
}

/**
 * Index of the anchor whose vertical midpoint is closest to `midY` (all in
 * ALTO units), -1 without anchors. Pure + tested.
 *
 * @param {number} midY
 * @param {{y:number, h:number}[]} [anchors]
 * @returns {number}
 */
export function nearestAnchorIndex(midY, anchors) {
    let best = -1;
    let bestDist = Infinity;
    (anchors || []).forEach((a, i) => {
        const dist = Math.abs(a.y + a.h / 2 - midY);
        if (dist < bestDist) {
            bestDist = dist;
            best = i;
        }
    });
    return best;
}

/**
 * Collision handling of the margin column: a note stays exactly at its desired
 * top (its anchor line) when it has the room. Overlapping runs merge into a
 * cluster placed at the median of its members' candidate tops (the L1-optimal
 * placement — do NOT "simplify" to the average), so the displacement is
 * balanced up AND down around the lines instead of accumulating as an
 * ever-growing downward drift (cards are taller than print lines). Clusters
 * are clamped to the container top and never overlap each other. Expects
 * notes sorted by desired top. Pure + tested.
 *
 * @param {{top:number, height:number}[]} notes
 * @param {number} gap
 * @returns {number[]} final top offsets
 */
export function stackNoteTops(notes, gap) {
    const clusterTop = (c) => {
        const cands = c.desired.map((d, i) => d - c.offsets[i]).sort((a, b) => a - b);
        const mid = Math.floor(cands.length / 2);
        const optimal = cands.length % 2 ? cands[mid] : (cands[mid - 1] + cands[mid]) / 2;
        return Math.max(0, optimal);
    };
    const clusters = [];
    (notes || []).forEach((n) => {
        clusters.push({ desired: [n.top], offsets: [0], height: n.height });
        while (clusters.length > 1) {
            const prev = clusters[clusters.length - 2];
            const last = clusters[clusters.length - 1];
            if (clusterTop(last) >= clusterTop(prev) + prev.height + gap) break;
            const base = prev.height + gap;
            last.offsets.forEach((offset, i) => {
                prev.desired.push(last.desired[i]);
                prev.offsets.push(base + offset);
            });
            prev.height = base + last.height;
            clusters.pop();
        }
    });
    const tops = [];
    clusters.forEach((c) => {
        const top = clusterTop(c);
        c.offsets.forEach((offset) => tops.push(top + offset));
    });
    return tops;
}

/**
 * Whether a note whose vertical midpoint is `midY` sits ABOVE its anchor line
 * (running heads, interlinear additions) rather than ON it — then its card
 * also goes above the content row. The threshold is the anchor's TOP edge,
 * not its midpoint: handwriting regularly sits slightly high of the printed
 * line, and that jitter must not push the card a full row up. Pure + tested.
 *
 * @param {number} midY  the note block's vertical midpoint (ALTO units)
 * @param {{y:number, h:number}|null} anchor
 * @returns {boolean}
 */
export function isAboveAnchor(midY, anchor) {
    return !!anchor && midY < anchor.y;
}

/**
 * Whether the center of `inner` lies inside `outer` (ALTO units) — the same
 * containment test the text nesting uses. Pure + tested.
 *
 * @param {{x:number, y:number, w:number, h:number}} inner
 * @param {{x:number, y:number, w:number, h:number}} outer
 * @returns {boolean}
 */
export function rectCenterInside(inner, outer) {
    if (!inner || !outer) return false;
    const cx = inner.x + inner.w / 2;
    const cy = inner.y + inner.h / 2;
    return cx >= outer.x && cx <= outer.x + outer.w && cy >= outer.y && cy <= outer.y + outer.h;
}

/**
 * Image overlay descriptors for a layout: drawn lines with a rendered tag type
 * plus the margin notes as 'note' hit areas (invisible until hovered/coupled).
 * Pure + tested.
 *
 * @param {{lines:Array, blocks:Array}|null} layout
 * @returns {{id:string, rect:object, modifier:string}[]}
 */
export function overlayRegions(layout) {
    const regions = [];
    ((layout && layout.lines) || []).forEach((line) => {
        const modifier = lineTypeModifier(line.types);
        if (modifier && line.rect) regions.push({ id: line.id, rect: line.rect, modifier });
    });
    ((layout && layout.blocks) || []).forEach((block) => {
        if (block.author && block.rect) regions.push({ id: block.id, rect: block.rect, modifier: 'note' });
    });
    return regions;
}

/**
 * Indexes the panel's region elements by ALTO element id (blocks, lines and
 * words alike), so layout annotations can be joined onto the rendered text.
 *
 * @param {HTMLElement} box  the fulltext panel content element
 * @param {string} pi
 * @param {number|string} pageNo  1-based page number
 * @returns {Map<string, HTMLElement>}
 */
function indexRegionElements(box, pi, pageNo) {
    const map = new Map();
    box.querySelectorAll('[data-iv-region-id]').forEach((el) => {
        const elementId = altoElementId(el.dataset.ivRegionId, pi, pageNo);
        if (elementId) map.set(elementId, el);
    });
    return map;
}

/**
 * Applies the word-level annotation markup (underline/strikethrough classes) to
 * the rendered panel text. The classes only become visible while the .immersive
 * root carries the state class, so toggling never re-renders. Block-level
 * annotations are NOT styled in place — every handwriting block becomes a card
 * in the annotations column (see {@link mountBlockAnnotations}).
 *
 * @param {HTMLElement} box  the fulltext panel content element
 * @param {{words:Array}} layout
 * @param {string} pi
 * @param {number|string} pageNo  1-based page number
 * @returns {Map<string, HTMLElement>} the region element index — pass it on to
 *          {@link mountBlockAnnotations} to avoid a second DOM scan
 */
export function applyTextMarkup(box, layout, pi, pageNo) {
    const els = indexRegionElements(box, pi, pageNo);
    (layout.words || []).forEach((word) => {
        const el = els.get(word.id);
        if (el) wordTypeClasses(word.types).forEach((cls) => el.classList.add(cls));
    });
    return els;
}

/**
 * Mirrors the word-level markup onto the page's image overlays, joined by ALTO element id.
 * That join is exact — no geometry — so every marked word is covered. The overlays already
 * carry the text↔image link's hover state, which lets CSS alone reveal a marking while its
 * word is pointed at, from the scan or from the panel.
 *
 * @param {Map<string, HTMLElement>} regionEls  image overlay elements, keyed by annotation URI
 * @param {{words:Array}|null} layout
 * @param {string} pi
 * @param {number|string} pageNo  1-based page number
 */
export function applyRegionMarkup(regionEls, layout, pi, pageNo) {
    if (!regionEls || !layout) return;
    const byElementId = new Map();
    regionEls.forEach((el, uri) => {
        const elementId = altoElementId(uri, pi, pageNo);
        if (elementId) byElementId.set(elementId, el);
    });
    (layout.words || []).forEach((word) => {
        const el = byElementId.get(word.id);
        if (el) wordTypeClasses(word.types).forEach((cls) => el.classList.add(cls));
    });
}

/**
 * A block's reading-order source elements in the panel: the block element when
 * the panel carries a real block region id, otherwise (the nesting drops
 * degenerate block levels on some pages) the rendered lines whose geometry lies
 * inside the block.
 */
function sourceElsFor(block, els, lineEls) {
    const srcEl = els.get(block.id);
    if (srcEl) return [srcEl];
    return lineEls.filter((l) => rectCenterInside(l.rect, block.rect)).map((l) => l.el);
}

/**
 * Marks every annotation block's sources in the reading-order text: `anno-src`
 * hides them while annotations are off (the printed-text column shows print
 * only), `anno-margin-src` while on (their card in the column replaces them).
 *
 * @returns {Map<string, HTMLElement[]>} block id → source elements
 */
function markAnnotationSources(annotationBlocks, els, lineEls) {
    const sources = new Map(annotationBlocks.map((block) => [block.id, sourceElsFor(block, els, lineEls)]));
    sources.forEach((blockSources) => {
        blockSources.forEach((el) => el.classList.add('immersive__anno-src', 'immersive__anno-margin-src'));
    });
    return sources;
}

/** Registers a mouseenter/mouseleave pair on `el` and records it in `registry` for teardown. */
function bindHover(el, enter, leave, registry) {
    el.addEventListener('mouseenter', enter);
    el.addEventListener('mouseleave', leave);
    registry.push([el, enter, leave]);
}

/**
 * Mounts everything block-level for one rendered page: EVERY handwriting block
 * (margin notes, running heads, callouts, insertion marks alike) becomes a card
 * in the annotations column, line-anchored at its position on the page, with
 * collisions stacked; the printed-text column shows print only (the sources are
 * hidden in both toggle states). Below {@link COLLAPSE_BELOW} content width the
 * column collapses and the cards render inline under their anchor line. Wires
 * the bidirectional hover coupling between cards, anchor lines and the image
 * regions.
 *
 * Implementation constraints: the outside-click listener closing the note
 * popovers is bound in the CAPTURE phase because OpenSeadragon stops click
 * propagation over its canvas (a bubble listener never sees clicks on the
 * image); the notes' Escape handling calls stopPropagation so the immersive
 * key dispatcher on document does not ALSO close the fulltext panel; and a
 * card overflowing above the first printed line is compensated by shifting
 * the text down via the --immersive-anno-shift padding custom property (LESS
 * applies it only in annotations mode, so switching the checkbox off cleans
 * the shift up without JS) instead of clamping the card onto the line.
 *
 * @param {object} opts
 * @param {HTMLElement} opts.box     the fulltext panel content element
 * @param {object} opts.layout      the page's layout annotations
 * @param {string} opts.pi
 * @param {number|string} opts.pageNo  1-based page number
 * @param {{id:string, rect:{y:number, h:number}|null, level:string}[]} opts.flat
 *        flattened text levels of the page (line geometry for anchoring)
 * @param {Map<string, HTMLElement>} [opts.els]  region element index from
 *        {@link applyTextMarkup}; indexed fresh when absent
 * @param {HTMLElement} [opts.columnsHead]  the "printed text | annotations" column header;
 *        shown only while the side column is active
 * @param {HTMLElement} [opts.scrollContainer]  scrollable panel; region hover reveals the card in it
 * @param {() => Map<string, HTMLElement>} [opts.getRegionEls]  live accessor for the image
 *        overlay elements by ALTO element id (card hover highlights the note's region on the scan)
 * @returns {{relayout: function():void, attachRegions: function(Map, Map):void, destroy: function():void}|null}
 *          destroy() removes cards, listeners and marker classes but does NOT restore the
 *          reading order of repositioned inline sources — the panel rebuild on page open does
 */
export function mountBlockAnnotations({ box, layout, pi, pageNo, flat, els: givenEls, columnsHead, scrollContainer, getRegionEls }) {
    const annotationBlocks = ((layout && layout.blocks) || []).filter((b) => b.author && b.rect);
    if (!annotationBlocks.length) return null;

    const els = givenEls || indexRegionElements(box, pi, pageNo);

    const lineEls = [];
    (flat || []).forEach((r) => {
        if (r.level !== 'line' || !r.rect) return;
        const el = els.get(altoElementId(r.id, pi, pageNo) || '');
        if (el) lineEls.push({ rect: r.rect, el });
    });

    const sources = markAnnotationSources(annotationBlocks, els, lineEls);

    const anchors = lineEls.filter(({ el }) => !el.closest('.immersive__anno-src')).map(({ rect, el }) => ({ y: rect.y, h: rect.h, el }));

    const column = document.createElement('div');
    column.className = 'immersive__anno-margin';

    const revealCard = (noteEl) => {
        if (!scrollContainer) return;
        const target = scrollTopToReveal({
            offsetTop: offsetTopWithin(noteEl, scrollContainer),
            height: noteEl.offsetHeight,
            scrollTop: scrollContainer.scrollTop,
            clientHeight: scrollContainer.clientHeight,
            scrollHeight: scrollContainer.scrollHeight,
        });
        if (target !== null) scrollContainer.scrollTop = target;
    };

    const $ = typeof window !== 'undefined' ? window.$ || window.jQuery : null;
    const popoverCapable = !!($ && $.fn && typeof $.fn.popover === 'function');
    const POPOVER_TEMPLATE =
        '<div class="popover immersive__anno-popover" role="tooltip"><div class="arrow"></div><h3 class="popover-header"></h3><div class="popover-body"></div></div>';
    let openNote = null;

    const hideNotePopover = () => {
        if (openNote && popoverCapable) $(openNote).popover('hide');
    };

    const onDocumentClick = (e) => {
        if (!openNote || !(e.target instanceof Element)) return;
        if (e.target.closest('.immersive__anno-note, .immersive__anno-popover')) return;
        hideNotePopover();
    };
    document.addEventListener('click', onDocumentClick, true);

    const attachNotePopover = (noteEl, authorName, fullText) => {
        if (!popoverCapable) return;
        $(noteEl).popover({
            trigger: 'manual',
            placement: 'left',
            container: scrollContainer || 'body',
            template: POPOVER_TEMPLATE,
            title: authorName || '',
            content: fullText,
        });
        noteEl.setAttribute('aria-haspopup', 'dialog');
        noteEl.setAttribute('aria-expanded', 'false');
        noteEl.addEventListener('click', () => {
            const wasOpen = openNote === noteEl;
            hideNotePopover();
            if (!wasOpen) $(noteEl).popover('show');
        });
        noteEl.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && openNote === noteEl) {
                e.preventDefault();
                e.stopPropagation();
                hideNotePopover();
            }
        });
        $(noteEl).on('shown.bs.popover', () => {
            openNote = noteEl;
            noteEl.setAttribute('aria-expanded', 'true');
            const id = noteEl.getAttribute('aria-describedby');
            const pop = id && document.getElementById(id);
            if (pop && pop.dataset.annoEscBound !== 'true') {
                pop.dataset.annoEscBound = 'true';
                pop.addEventListener('keydown', (e) => {
                    if (e.key === 'Escape') {
                        e.preventDefault();
                        e.stopPropagation();
                        hideNotePopover();
                        noteEl.focus();
                    }
                });
            }
        });
        $(noteEl).on('hidden.bs.popover', () => {
            if (openNote === noteEl) openNote = null;
            noteEl.setAttribute('aria-expanded', 'false');
        });
    };

    const notes = annotationBlocks
        .map((block) => {
            const blockSources = sources.get(block.id) || [];
            const noteEl = document.createElement('button');
            noteEl.type = 'button';
            noteEl.className = 'immersive__anno-note';
            styleClassTokens(block.styleClass).forEach((cls) => noteEl.classList.add(cls));
            const who = document.createElement('span');
            who.className = 'immersive__anno-note-author';
            who.textContent = block.authorName || '';
            noteEl.append(who);
            const text = document.createElement('span');
            text.className = 'immersive__anno-note-text';
            text.textContent = blockSources.length
                ? blockSources
                      .map((el) => el.textContent)
                      .join(' ')
                      .trim()
                : block.text || '';
            noteEl.append(text);
            attachNotePopover(noteEl, block.authorName, text.textContent);
            const midY = block.rect.y + block.rect.h / 2;
            const anchorIdx = nearestAnchorIndex(midY, anchors);
            const anchor = anchorIdx >= 0 ? anchors[anchorIdx] : null;
            const aboveAnchor = isAboveAnchor(midY, anchor);
            const imageRegion = () => (getRegionEls ? getRegionEls().get(block.id) : null);
            noteEl.addEventListener('mouseenter', () => {
                if (anchor && !aboveAnchor) anchor.el.classList.add('is-anno-anchor');
                const region = imageRegion();
                if (region) region.classList.add('is-anno-active');
            });
            noteEl.addEventListener('mouseleave', () => {
                if (anchor && !aboveAnchor) anchor.el.classList.remove('is-anno-anchor');
                const region = imageRegion();
                if (region) region.classList.remove('is-anno-active');
            });
            return { block, noteEl, anchor, aboveAnchor };
        })
        .sort((a, b) => a.block.rect.y - b.block.rect.y);

    const anchorListeners = [];
    const notesByAnchor = new Map();
    notes.forEach((n) => {
        if (!n.anchor || n.aboveAnchor) return;
        if (!notesByAnchor.has(n.anchor.el)) notesByAnchor.set(n.anchor.el, []);
        notesByAnchor.get(n.anchor.el).push(n.noteEl);
    });
    notesByAnchor.forEach((noteEls, anchorEl) => {
        bindHover(
            anchorEl,
            () => noteEls.forEach((el) => el.classList.add('is-anno-active')),
            () => noteEls.forEach((el) => el.classList.remove('is-anno-active')),
            anchorListeners
        );
    });

    const notesById = new Map(notes.map((n) => [n.block.id, n]));
    const regionIdToNote = new Map();
    notes.forEach((note) => {
        (sources.get(note.block.id) || []).forEach((srcEl) => {
            const collect = (el) => {
                const id = el.dataset && el.dataset.ivRegionId;
                if (id) regionIdToNote.set(id, note);
            };
            collect(srcEl);
            srcEl.querySelectorAll('[data-iv-region-id]').forEach(collect);
        });
    });
    const regionListeners = [];
    const bindRegion = (el, note) => {
        bindHover(
            el,
            () => {
                note.noteEl.classList.add('is-anno-active');
                revealCard(note.noteEl);
                if (note.anchor && !note.aboveAnchor) note.anchor.el.classList.add('is-anno-anchor');
            },
            () => {
                note.noteEl.classList.remove('is-anno-active');
                if (note.anchor && !note.aboveAnchor) note.anchor.el.classList.remove('is-anno-anchor');
            },
            regionListeners
        );
    };
    const attachRegions = (annoRegionMap, textRegionMap) => {
        regionListeners.splice(0).forEach(([el, enter, leave]) => {
            el.removeEventListener('mouseenter', enter);
            el.removeEventListener('mouseleave', leave);
        });
        if (annoRegionMap) {
            annoRegionMap.forEach((el, id) => {
                const note = notesById.get(id);
                if (note && el.classList.contains('immersive__anno-region-note')) bindRegion(el, note);
            });
        }
        if (textRegionMap) {
            textRegionMap.forEach((el, id) => {
                const note = regionIdToNote.get(id);
                if (note) bindRegion(el, note);
            });
        }
    };

    const inlineNotes = [];

    const relayout = () => {
        hideNotePopover();
        inlineNotes.splice(0).forEach((el) => el.remove());
        column.remove();
        box.style.removeProperty('--immersive-anno-shift');
        const collapsed = box.clientWidth < COLLAPSE_BELOW;
        box.classList.toggle('-anno-collapsed', collapsed);
        if (columnsHead) columnsHead.hidden = collapsed;
        if (collapsed) {
            notes.forEach(({ noteEl, anchor, aboveAnchor }) => {
                noteEl.classList.add('-inline');
                noteEl.style.top = '';
                if (anchor && aboveAnchor) anchor.el.before(noteEl);
                else if (anchor) anchor.el.after(noteEl);
                else box.appendChild(noteEl);
                inlineNotes.push(noteEl);
            });
            return;
        }
        notes.forEach(({ noteEl }) => {
            noteEl.classList.remove('-inline');
            column.appendChild(noteEl);
        });
        box.appendChild(column);
        const rowHeight = anchors.length ? parseFloat(getComputedStyle(anchors[0].el).lineHeight) || 0 : 0;
        const desired = notes
            .map(({ noteEl, anchor, aboveAnchor }) => {
                const anchorTop = anchor ? offsetTopWithin(anchor.el, box) : 0;
                const height = noteEl.offsetHeight;
                return {
                    noteEl,
                    top: aboveAnchor ? anchorTop - height - NOTE_GAP : anchorTop + (rowHeight - height) / 2,
                    height,
                };
            })
            .sort((a, b) => a.top - b.top);
        const overflow = desired.length ? Math.max(0, -desired[0].top) : 0;
        if (overflow) {
            box.style.setProperty('--immersive-anno-shift', `${overflow}px`);
            desired.forEach((d) => {
                d.top += overflow;
            });
        }
        stackNoteTops(desired, NOTE_GAP).forEach((top, i) => {
            desired[i].noteEl.style.top = `${top}px`;
        });
    };

    let observer = null;
    if (typeof ResizeObserver !== 'undefined') {
        observer = new ResizeObserver(() => relayout());
        observer.observe(box);
    }
    relayout();

    return {
        relayout,
        attachRegions,
        destroy() {
            if (observer) observer.disconnect();
            document.removeEventListener('click', onDocumentClick, true);
            if (popoverCapable) notes.forEach(({ noteEl }) => $(noteEl).popover('dispose'));
            attachRegions(null, null);
            anchorListeners.splice(0).forEach(([el, enter, leave]) => {
                el.removeEventListener('mouseenter', enter);
                el.removeEventListener('mouseleave', leave);
                el.classList.remove('is-anno-anchor');
            });
            inlineNotes.splice(0).forEach((el) => el.remove());
            column.remove();
            box.classList.remove('-anno-collapsed');
            box.style.removeProperty('--immersive-anno-shift');
            if (columnsHead) columnsHead.hidden = true;
            els.forEach((el) => el.classList.remove('immersive__anno-margin-src', 'immersive__anno-src'));
            if (getRegionEls) getRegionEls().forEach((el) => el.classList.remove('is-anno-active'));
        },
    };
}

/**
 * Wires the annotations checkbox: persisted state, the root state class and
 * the `a` keyboard shortcut. The whole toggle row is hidden (not just
 * disabled — it would otherwise dictate the panel's minimum width) and the
 * root state class is dropped whenever the current page has no annotation
 * data; the persisted preference survives such pages.
 *
 * @param {object} opts
 * @param {HTMLElement} opts.root      the .immersive root (carries the state class)
 * @param {HTMLInputElement} opts.checkbox
 * @param {Storage} opts.storage      e.g. window.localStorage
 * @param {{register: Function}} [opts.keys]  the immersive key dispatcher
 * @returns {{setPageData: Function, onChange: Function, destroy: Function}}
 */
export function setupAnnotationsToggle({ root, checkbox, storage, keys }) {
    const listeners = [];
    const toggleRow = checkbox.closest('.immersive__anno-toggle');
    let available = false;

    const sync = () => {
        root.classList.toggle(STATE_CLASS, checkbox.checked && available);
    };

    const setEnabled = (on) => {
        checkbox.checked = on;
        writeAnnotationsEnabled(storage, on);
        sync();
        listeners.forEach((cb) => cb(on));
    };

    const onCheckboxChange = () => setEnabled(checkbox.checked);
    checkbox.checked = readAnnotationsEnabled(storage);
    checkbox.addEventListener('change', onCheckboxChange);

    let unregisterKeys = null;
    if (keys) {
        unregisterKeys = keys.register(55, (e) => {
            if (e.key !== 'a' || e.ctrlKey || e.metaKey || e.altKey || e.shiftKey) return false;
            if (isTypingTarget(e.target)) return false;
            const panel = checkbox.closest('.immersive__panel-left');
            if (checkbox.disabled || (panel && !panel.classList.contains('is-open'))) return false;
            if (isOverlayOpen()) return false;
            setEnabled(!checkbox.checked);
            return true;
        });
    }

    sync();

    return {
        /**
         * Updates the toggle availability for the current page's layout data.
         *
         * @param {object|null} layout  the page's layout annotations
         */
        setPageData(layout) {
            available = hasAnnotations(layout);
            checkbox.disabled = !available;
            if (toggleRow) toggleRow.hidden = !available;
            sync();
        },
        onChange(cb) {
            listeners.push(cb);
            return () => {
                const i = listeners.indexOf(cb);
                if (i !== -1) listeners.splice(i, 1);
            };
        },
        destroy() {
            checkbox.removeEventListener('change', onCheckboxChange);
            if (unregisterKeys) unregisterKeys();
            listeners.length = 0;
        },
    };
}
