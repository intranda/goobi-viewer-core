/** Splitter between the printed text and the annotation column: it sets where the two columns split, dragged, keyed or reset. */

/** Session key of the split position, the panel width's counterpart (see setupPanelResize). */
const SPLIT_KEY = 'immersive-anno-split';

/**
 * The px limits the LESS clamp() guards as well (@immersive-anno-text-min,
 * @immersive-anno-column-min, @immersive-anno-column-default, @immersive-anno-margin-gap).
 * Keeping the numbers equal is what makes the rule stop exactly under the pointer
 * instead of before it.
 */
const TEXT_MIN = 200;
const COLUMN_MIN = 120;
const COLUMN_DEFAULT = 160;
const COLUMN_GAP = 12;

/** Arrow-key step in px, quadrupled while Shift is held. */
const KEY_STEP = 16;

const inert = { handle: null, destroy() {} };

/**
 * Mounts the splitter that gives the annotation column more (or less) of the
 * fulltext panel, so long annotations can be read without opening every card.
 *
 * The split is anchored to the printed text: the custom property holds the
 * text column's width, the annotation column takes what is left. So each of
 * the two handles moves exactly one edge -- the panel's own handle moves the
 * panel edge and hands the difference to the annotations, this one moves the
 * rule between the columns -- and neither drags the other one along. Until
 * someone moves it, no property is written at all and the LESS default keeps
 * the column at @immersive-anno-column-default.
 *
 * A drag tracks the pointer on the window, not on the handle, so it survives
 * the note relayout it triggers on every frame; it ends on pointerup wherever
 * that happens, which is guaranteed by the pointer capture.
 *
 * The handle element is owned here but placed by the note layout, which drops
 * it into the box next to the margin column and takes it out again while the
 * column is collapsed.
 *
 * @param {object} opts
 * @param {HTMLElement} opts.panel  the fulltext panel (hosts the custom property)
 * @param {HTMLElement} opts.box    the fulltext content element (measures the room the split has)
 * @param {string} [opts.label]     accessible name of the splitter
 * @returns {{handle: HTMLElement|null, destroy: function():void}}
 */
export function setupAnnotationColumnResize({ panel, box, label }) {
    if (!panel || !box) return inert;
    const root = panel.closest('.immersive');

    const room = () => box.clientWidth || 1;
    /** Text column widths the current panel width allows, the annotation column's limits mirrored. */
    const bounds = () => ({ min: TEXT_MIN, max: Math.max(TEXT_MIN, room() - COLUMN_MIN - COLUMN_GAP) });
    const clamp = (width) => {
        const { min, max } = bounds();
        return Math.round(Math.min(Math.max(width, min), max));
    };

    const handle = document.createElement('div');
    handle.className = 'immersive__anno-resize-handle';
    handle.setAttribute('role', 'separator');
    handle.setAttribute('aria-orientation', 'vertical');
    handle.tabIndex = 0;
    if (label) {
        handle.setAttribute('aria-label', label);
        handle.setAttribute('title', label);
    }

    /** The split the user set, clamped to the panel's room; NaN while they never moved it. */
    let split = NaN;
    try {
        split = parseFloat(sessionStorage.getItem(SPLIT_KEY));
    } catch {}
    /** The text width in effect, whether from the user or from the LESS default. */
    const current = () => clamp(Number.isFinite(split) ? split : room() - COLUMN_DEFAULT - COLUMN_GAP);

    /** The annotation column's share of the panel, the value the splitter announces. */
    const shareOf = (textWidth) => Math.round(((room() - textWidth - COLUMN_GAP) / room()) * 100);

    const apply = (next) => {
        split = Number.isFinite(next) ? clamp(next) : NaN;
        if (Number.isFinite(split)) panel.style.setProperty('--_immersive-anno-split', split + 'px');
        else panel.style.removeProperty('--_immersive-anno-split');
        const { min, max } = bounds();
        const share = shareOf(current());
        handle.setAttribute('aria-valuenow', String(share));
        handle.setAttribute('aria-valuetext', share + '%');
        handle.setAttribute('aria-valuemin', String(shareOf(max)));
        handle.setAttribute('aria-valuemax', String(shareOf(min)));
    };

    /* One write per frame: a drag emits pointermoves faster than the panel can
       reflow its text and restack the cards. */
    let frame = 0;
    let pending = split;
    const flush = () => {
        frame = 0;
        apply(pending);
    };
    const applySoon = (next) => {
        pending = next;
        if (!frame) frame = requestAnimationFrame(flush);
    };
    const applyNow = (next) => {
        if (frame) cancelAnimationFrame(frame);
        frame = 0;
        apply(next);
        try {
            if (Number.isFinite(split)) sessionStorage.setItem(SPLIT_KEY, String(split));
            else sessionStorage.removeItem(SPLIT_KEY);
        } catch {}
    };

    apply(split);

    let dragging = false;
    /** Detaches the running drag's window listeners; null while no drag runs. */
    let releaseDrag = null;
    const endDrag = () => {
        dragging = false;
        if (releaseDrag) releaseDrag();
        releaseDrag = null;
        if (root) root.classList.remove('-anno-resizing');
    };

    handle.addEventListener('pointerdown', (e) => {
        if (dragging || (e.pointerType === 'mouse' && e.button !== 0)) return;
        e.preventDefault();
        dragging = true;
        if (handle.setPointerCapture) handle.setPointerCapture(e.pointerId);
        if (root) root.classList.add('-anno-resizing');
        const rect = box.getBoundingClientRect();
        const splitAt = (ev) => ev.clientX - rect.left - COLUMN_GAP / 2;
        const onMove = (ev) => {
            if (dragging) applySoon(splitAt(ev));
        };
        const onEnd = (ev) => {
            endDrag();
            applyNow(ev.type === 'pointercancel' ? split : splitAt(ev));
        };
        window.addEventListener('pointermove', onMove);
        window.addEventListener('pointerup', onEnd);
        window.addEventListener('pointercancel', onEnd);
        releaseDrag = () => {
            window.removeEventListener('pointermove', onMove);
            window.removeEventListener('pointerup', onEnd);
            window.removeEventListener('pointercancel', onEnd);
        };
    });

    /* The column grows to the left, so ArrowLeft moves the rule left. The keys stay
       with the focused splitter instead of reaching the immersive key dispatcher. */
    handle.addEventListener('keydown', (e) => {
        if (e.ctrlKey || e.metaKey || e.altKey) return;
        const towards = e.key === 'ArrowLeft' ? -1 : e.key === 'ArrowRight' ? 1 : 0;
        if (!towards) return;
        e.preventDefault();
        e.stopPropagation();
        applyNow(current() + towards * KEY_STEP * (e.shiftKey ? 4 : 1));
    });

    handle.addEventListener('dblclick', () => applyNow(NaN));

    return {
        handle,
        destroy() {
            if (frame) cancelAnimationFrame(frame);
            endDrag();
            handle.remove();
            panel.style.removeProperty('--_immersive-anno-split');
        },
    };
}
