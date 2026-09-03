/** Margin-note cards for a page's handwriting blocks: line anchoring, collision stacking, popovers and hover coupling to text and image. */

import { env } from '../../shared/ivEnv.mjs';
import { offsetTopWithin, scrollTopToReveal, styleClassTokens } from '../../shared/ivDom.mjs';
import { altoElementId, nearestAnchorIndex, stackNoteTops, isAboveAnchor } from '../../data/ivAnnotationModel.mjs';
import { indexRegionElements, markAnnotationSources } from './ivAnnotationMarkup.mjs';

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

const POPOVER_TEMPLATE =
    '<div class="popover immersive__anno-popover" role="tooltip"><div class="arrow"></div><h3 class="popover-header"></h3><div class="popover-body"></div></div>';

/** Registers a mouseenter/mouseleave pair on `el` and records it in `registry` for teardown. */
function bindHover(el, enter, leave, registry) {
    el.addEventListener('mouseenter', enter);
    el.addEventListener('mouseleave', leave);
    registry.push([el, enter, leave]);
}

/** Rendered line elements of the page with their ALTO geometry, for anchoring. */
function collectLineEls(flat, els, pi, pageNo) {
    const lineEls = [];
    (flat || []).forEach((r) => {
        if (r.level !== 'line' || !r.rect) return;
        const el = els.get(altoElementId(r.id, pi, pageNo) || '');
        if (el) lineEls.push({ rect: r.rect, el });
    });
    return lineEls;
}

/**
 * Bootstrap popovers on the note cards, one open at a time. The outside-click
 * listener is bound in the capture phase because OpenSeadragon stops click
 * propagation over its canvas; Escape stops propagation so the immersive key
 * dispatcher does not also close the panel.
 */
function createNotePopovers(scrollContainer) {
    const $ = env.$ || null;
    const capable = !!($ && $.fn && typeof $.fn.popover === 'function');
    let openNote = null;
    const hide = () => {
        if (openNote && capable) $(openNote).popover('hide');
    };
    const onDocumentClick = (e) => {
        if (!openNote || !(e.target instanceof Element)) return;
        if (e.target.closest('.immersive__anno-note, .immersive__anno-popover')) return;
        hide();
    };
    document.addEventListener('click', onDocumentClick, true);
    const attach = (noteEl, authorName, fullText) => {
        if (!capable) return;
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
            hide();
            if (!wasOpen) $(noteEl).popover('show');
        });
        noteEl.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && openNote === noteEl) {
                e.preventDefault();
                e.stopPropagation();
                hide();
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
                        hide();
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
    const destroy = (notes) => {
        document.removeEventListener('click', onDocumentClick, true);
        if (capable) notes.forEach(({ noteEl }) => $(noteEl).popover('dispose'));
    };
    return { attach, hide, destroy };
}

/** One card per block, anchored to the nearest printed line; hovering a card lights its anchor line and its image region. */
function buildNotes({ blocks, sources, anchors, popovers, getRegionEls }) {
    return blocks
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
            popovers.attach(noteEl, block.authorName, text.textContent);
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
}

/** Hovering an anchor line lights every card anchored to it; returns the listener registry for teardown. */
function bindAnchorHover(notes) {
    const listeners = [];
    const byAnchor = new Map();
    notes.forEach((n) => {
        if (!n.anchor || n.aboveAnchor) return;
        if (!byAnchor.has(n.anchor.el)) byAnchor.set(n.anchor.el, []);
        byAnchor.get(n.anchor.el).push(n.noteEl);
    });
    byAnchor.forEach((noteEls, anchorEl) => {
        bindHover(
            anchorEl,
            () => noteEls.forEach((el) => el.classList.add('is-anno-active')),
            () => noteEls.forEach((el) => el.classList.remove('is-anno-active')),
            listeners
        );
    });
    return listeners;
}

/** Couples the image regions (drawn note areas and OCR text regions) to the cards: hover lights the card and scrolls it into view. */
function createRegionCoupling({ notes, sources, scrollContainer }) {
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
    const listeners = [];
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
            listeners
        );
    };
    const attachRegions = (annoRegionMap, textRegionMap) => {
        listeners.splice(0).forEach(([el, enter, leave]) => {
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
    return { attachRegions };
}

/**
 * Places the cards: in the side column with collision stacking, or inline under
 * their anchor line once the panel is narrower than COLLAPSE_BELOW. A card that
 * would overflow above the first printed line shifts the text down via the
 * --immersive-anno-shift custom property instead of being clamped onto the line.
 */
function createNoteLayout({ box, notes, anchors, columnsHead, popovers }) {
    const column = document.createElement('div');
    column.className = 'immersive__anno-margin';
    const inlineNotes = [];
    const relayout = () => {
        popovers.hide();
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
    const destroy = () => {
        inlineNotes.splice(0).forEach((el) => el.remove());
        column.remove();
        box.classList.remove('-anno-collapsed');
        box.style.removeProperty('--immersive-anno-shift');
        if (columnsHead) columnsHead.hidden = true;
    };
    return { relayout, destroy };
}

/**
 * Mounts everything block-level for one rendered page: every handwriting block
 * becomes a card in the annotations column, line-anchored at its position on
 * the page, with collisions stacked; the printed-text column shows print only.
 * Below COLLAPSE_BELOW content width the column collapses and the cards render
 * inline under their anchor line. Wires the bidirectional hover coupling between
 * cards, anchor lines and the image regions.
 *
 * @param {object} opts
 * @param {HTMLElement} opts.box     the fulltext panel content element
 * @param {object} opts.layout      the page's layout annotations
 * @param {string} opts.pi
 * @param {number|string} opts.pageNo  1-based page number
 * @param {{id:string, rect:{y:number, h:number}|null, level:string}[]} opts.flat  flattened text levels (line geometry for anchoring)
 * @param {Map<string, HTMLElement>} [opts.els]  region element index from applyTextMarkup; indexed fresh when absent
 * @param {HTMLElement} [opts.columnsHead]  the "printed text | annotations" column header, shown only while the side column is active
 * @param {HTMLElement} [opts.scrollContainer]  scrollable panel; region hover reveals the card in it
 * @param {() => Map<string, HTMLElement>} [opts.getRegionEls]  live accessor for the image overlay elements by ALTO element id
 * @returns {{relayout: function():void, attachRegions: function(Map, Map):void, destroy: function():void}|null}
 *          destroy() removes cards, listeners and marker classes; the reading order of repositioned inline sources is restored by the panel rebuild on page open
 */
export function mountBlockAnnotations({ box, layout, pi, pageNo, flat, els: givenEls, columnsHead, scrollContainer, getRegionEls }) {
    const blocks = ((layout && layout.blocks) || []).filter((b) => b.author && b.rect);
    if (!blocks.length) return null;
    const els = givenEls || indexRegionElements(box, pi, pageNo);
    const lineEls = collectLineEls(flat, els, pi, pageNo);
    const sources = markAnnotationSources(blocks, els, lineEls);
    const anchors = lineEls.filter(({ el }) => !el.closest('.immersive__anno-src')).map(({ rect, el }) => ({ y: rect.y, h: rect.h, el }));
    const popovers = createNotePopovers(scrollContainer);
    const notes = buildNotes({ blocks, sources, anchors, popovers, getRegionEls });
    const anchorListeners = bindAnchorHover(notes);
    const coupling = createRegionCoupling({ notes, sources, scrollContainer });
    const placement = createNoteLayout({ box, notes, anchors, columnsHead, popovers });
    let observer = null;
    if (typeof ResizeObserver !== 'undefined') {
        observer = new ResizeObserver(() => placement.relayout());
        observer.observe(box);
    }
    placement.relayout();
    return {
        relayout: placement.relayout,
        attachRegions: coupling.attachRegions,
        destroy() {
            if (observer) observer.disconnect();
            popovers.destroy(notes);
            coupling.attachRegions(null, null);
            anchorListeners.splice(0).forEach(([el, enter, leave]) => {
                el.removeEventListener('mouseenter', enter);
                el.removeEventListener('mouseleave', leave);
                el.classList.remove('is-anno-anchor');
            });
            placement.destroy();
            els.forEach((el) => el.classList.remove('immersive__anno-margin-src', 'immersive__anno-src'));
            if (getRegionEls) getRegionEls().forEach((el) => el.classList.remove('is-anno-active'));
        },
    };
}
