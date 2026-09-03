/** Image-filter popover wiring plus a11y for the immersive Bootstrap popovers. */

import { createDisposer, noop } from '../shared/ivLifecycle.mjs';

const FILTER_TRIGGER_SELECTOR = '[data-popover-element="#immersiveFilterPopover"]';

/**
 * Whether the viewer's canvas is origin-clean (CORS): pixel filters would throw on a
 * tainted canvas. Checked both up front and again at mount time, because the taint
 * status can change once tiles have actually loaded.
 */
function isViewerOriginClean(viewer) {
    const image = viewer.viewer;
    return typeof image.isOriginClean !== 'function' || image.isOriginClean();
}

/**
 * Defers the imageFilters mount until the Filter popover is first shown: Bootstrap only
 * portals #immersiveFilterPopover (and its <imageFilters> child) into the DOM on show,
 * so mounting on load would find nothing and the popover would open empty. After the
 * mount the popover is repositioned, because riot fills it only after Popper has
 * already measured the still-empty shell.
 *
 * The origin-clean check runs at open time (the canvas only taints once tiles have
 * been drawn); a persistent handler with a mounted flag replaces one(), so an open
 * where the mount cannot happen yet does not burn the only mount attempt.
 *
 * @returns {{destroy: function():void}}
 */
export function setupImageFilters(viewer) {
    const btn = document.querySelector(FILTER_TRIGGER_SELECTOR);
    if (!btn) return noop;
    const bag = createDisposer();
    const $ = window.$ || window.jQuery;
    if (!$) {
        mountImageFilters(viewer, bag);
        return { destroy: bag.dispose };
    }
    let mounted = false;
    $(btn).on('shown.bs.popover.immersiveFilters', () => {
        if (mounted) return;
        if (!isViewerOriginClean(viewer)) {
            $(btn).popover('hide');
            btn.hidden = true;
            return;
        }
        mounted = mountImageFilters(viewer, bag);
        if (mounted) $(btn).popover('update');
    });
    bag.add(() => $(btn).off('shown.bs.popover.immersiveFilters'));
    return { destroy: bag.dispose };
}

/**
 * Adds focus/keyboard management to the three immersive Bootstrap popovers (Share / Cite /
 * Filter) without touching the shared popovers controller. On open, focus moves into the
 * portaled popover; Escape inside it closes and returns focus to the trigger; aria-expanded
 * is kept in sync. Hooks only these triggers via Bootstrap's shown/hidden.bs.popover events.
 *
 * @returns {{destroy: function():void}}
 */
export function setupImmersivePopoverA11y() {
    const $ = window.$ || window.jQuery;
    if (!$) return noop;
    const bag = createDisposer();
    const FOCUSABLE = 'a[href],button,input,select,textarea,[tabindex]:not([tabindex="-1"])';
    const selectors = ['#immersiveSharePopover', '#immersiveCitationPopover', '#immersiveFilterPopover'];
    selectors.forEach((sel) => {
        const trigger = document.querySelector(`[data-popover-element="${sel}"]`);
        if (!trigger || trigger.dataset.a11yBound === 'true') return;
        trigger.dataset.a11yBound = 'true';

        const popoverEl = () => {
            const id = trigger.getAttribute('aria-describedby');
            const byId = id && document.getElementById(id);
            if (byId) return byId;
            const all = Array.from(document.querySelectorAll('.popover')).filter((p) => p.offsetParent !== null);
            return all.length ? all[all.length - 1] : null;
        };

        $(trigger).on('shown.bs.popover.ivA11y', () => {
            trigger.setAttribute('aria-expanded', 'true');
            const pop = popoverEl();
            if (!pop) return;
            pop.addEventListener('keydown', (e) => {
                if (e.key === 'Escape') {
                    e.preventDefault();
                    $(trigger).popover('hide');
                    trigger.focus();
                }
            });
            const first = pop.querySelector(FOCUSABLE);
            if (first) {
                const $first = $(first);
                const hasTip = typeof $first.tooltip === 'function' && !!$first.data('bs.tooltip');
                if (hasTip) $first.tooltip('disable');
                first.focus();
                if (hasTip) $first.tooltip('enable');
            } else {
                if (!pop.hasAttribute('tabindex')) pop.setAttribute('tabindex', '-1');
                pop.focus();
            }
        });

        $(trigger).on('hidden.bs.popover.ivA11y', () => {
            trigger.setAttribute('aria-expanded', 'false');
            const active = document.activeElement;
            if (!active || active === document.body) trigger.focus();
        });
        bag.add(() => {
            $(trigger).off('.ivA11y');
            delete trigger.dataset.a11yBound;
        });
    });
    return { destroy: bag.dispose };
}

/**
 * Mounts the reused imageFilters riot tag on the viewer's live ImageView.Image so the
 * Filter popover adjusts brightness/contrast/etc.
 *
 * @param {{add: Function}} bag  the caller's disposer; the tag's subscriptions are queued on it
 * @returns {boolean} whether the tag was mounted
 */
function mountImageFilters(viewer, bag) {
    if (!document.querySelector('imageFilters') || !window.immersiveFilterConfig) return false;
    const [tag] = riot.mount('imageFilters', { image: viewer.viewer, config: window.immersiveFilterConfig });
    if (tag) {
        bag.add(bindImageFiltersRendering(viewer, tag));
        // "Reset view" (button and '0' key) clears the image filters as well.
        bag.add(viewer.onReset.subscribe(() => tag.resetAll()));
    }
    return true;
}

/**
 * Replaces the library's per-filter event subscriptions with one direct OSD
 * handler that applies every active filter exactly once per drawn frame.
 *
 * The library re-runs its observable wiring on every load() (each double-page
 * toggle) and stacks another 'update-viewport' forwarder onto the SAME
 * OpenSeadragon instance without ever tearing the old ones down, so a filter
 * subscribed through that chain runs N+1 times per frame after N reopens and
 * compounds its own output onto the already-filtered canvas (contrast^N).
 * Applying the filter methods ourselves from a single handler sidesteps the
 * chain entirely; the tag's start/close/isActive/apply are rebased onto a
 * plain active-set so its UI logic (checkboxes, precludes, reset) keeps working.
 *
 * @returns {function():void} detaches the frame handler and the onOpen subscription
 */
export function bindImageFiltersRendering(viewer, tag) {
    const image = viewer.viewer;
    const redraw = () => image.openseadragon.forceRedraw();
    const active = new Set();
    (tag.filters || []).forEach((filter) => {
        filter.start = () => {
            active.add(filter);
            redraw();
        };
        filter.close = () => {
            active.delete(filter);
            redraw();
        };
        filter.isActive = () => active.has(filter);
        filter.apply = redraw;
    });
    const renderFilters = () => {
        if (!active.size) return;
        const context = image.getCanvasContext();
        if (!context) return;
        let data = context.getImageData(0, 0, context.canvas.width, context.canvas.height);
        active.forEach((filter) => {
            data = filter.filterMethod(data) || data;
        });
        context.putImageData(data, 0, 0);
    };
    let boundOsd = null;
    const attach = () => {
        if (!image.openseadragon || boundOsd === image.openseadragon) return;
        boundOsd = image.openseadragon;
        boundOsd.addHandler('update-viewport', renderFilters);
    };
    attach();
    const unsubscribe = viewer.onOpen.subscribe(attach);
    return () => {
        unsubscribe();
        if (boundOsd && typeof boundOsd.removeHandler === 'function') boundOsd.removeHandler('update-viewport', renderFilters);
    };
}
