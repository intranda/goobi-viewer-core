/** Unit tests for the bottom bar wiring: indicator, chevrons, action buttons, double page, teardown. */
import { jest } from '@jest/globals';
import { setupBottomBar } from '../ui/ivBottomBar.mjs';
import { Emitter } from '../engine/ivViewer.mjs';

function fakeViewer(total = 5) {
    let current = [0];
    let double = false;
    return {
        onceResized: jest.fn(() => jest.fn()),
        getPageCount: () => total,
        getCurrentPages: () => current,
        show(pages) {
            current = pages;
            this.onPageChange.emit(pages[0]);
        },
        next: jest.fn(),
        prev: jest.fn(),
        zoomIn: jest.fn(),
        zoomOut: jest.fn(),
        rotateLeft: jest.fn(),
        rotateRight: jest.fn(),
        resetView: jest.fn(),
        toggleDoublePage: jest.fn(() => (double = !double)),
        onPageChange: new Emitter(),
    };
}

function mountMarkup(viewer) {
    document.body.innerHTML = `
        <div class="immersive">
            <section class="immersive__viewer">
                <span class="immersive__title-text">Werk</span><span id="immersiveTitlePage"></span>
                <div data-immersive-image><div class="openseadragon-canvas"></div></div>
                <button class="immersive__btn-double-page" data-immersive-action="double-page" aria-pressed="false"></button>
                <button data-immersive-action="overview"></button>
                <button data-immersive-action="fullscreen" data-label-enter="Vollbild" data-label-exit="Vollbild verlassen" aria-label="Vollbild"></button>
                <div class="immersive__bottombar">
                    <button class="immersive__bar-btn" data-immersive-page="prev"></button>
                    <span id="immersivePageIndicator"></span>
                    <button class="immersive__bar-btn" data-immersive-page="next"></button>
                    <button data-immersive-action="zoom-in"></button><button data-immersive-action="zoom-out"></button>
                    <button data-immersive-action="rotate-left"></button><button data-immersive-action="rotate-right"></button>
                    <button data-immersive-action="reset"></button>
                </div>
            </section>
        </div>`;
    const hooks = { toggleGrid: jest.fn(), updateFulltextAvail: jest.fn() };
    const bar = setupBottomBar(document.querySelector('[data-immersive-image]'), viewer, hooks);
    const click = (sel) => document.querySelector(sel).click();
    return { bar, hooks, click, indicator: document.getElementById('immersivePageIndicator') };
}

afterEach(() => {
    document.body.innerHTML = '';
});

test('writes the page indicator, title pill and canvas label and follows page changes', () => {
    const viewer = fakeViewer();
    const { indicator } = mountMarkup(viewer);
    expect(indicator.textContent).toBe('1 / 5');
    expect(document.getElementById('immersiveTitlePage').textContent).toBe('(1 / 5)');
    expect(document.querySelector('.openseadragon-canvas').getAttribute('aria-label')).toBe('Werk, 1 / 5');
    viewer.show([2, 3]);
    expect(indicator.textContent).toBe('3–4 / 5');
});

test('the buttons drive the viewer and the hooks', () => {
    const viewer = fakeViewer();
    const { hooks, click } = mountMarkup(viewer);
    click('.immersive__bar-btn[data-immersive-page="next"]');
    click('.immersive__bar-btn[data-immersive-page="prev"]');
    ['zoom-in', 'zoom-out', 'rotate-left', 'rotate-right', 'reset', 'overview'].forEach((a) => click(`[data-immersive-action="${a}"]`));
    expect(viewer.next).toHaveBeenCalledTimes(1);
    expect(viewer.prev).toHaveBeenCalledTimes(1);
    expect(viewer.zoomIn).toHaveBeenCalledTimes(1);
    expect(viewer.zoomOut).toHaveBeenCalledTimes(1);
    expect(viewer.rotateLeft).toHaveBeenCalledTimes(1);
    expect(viewer.rotateRight).toHaveBeenCalledTimes(1);
    expect(viewer.resetView).toHaveBeenCalledTimes(1);
    expect(hooks.toggleGrid).toHaveBeenCalledTimes(1);
});

test('double page toggles the viewer state class, the pressed state and the fulltext availability', () => {
    const viewer = fakeViewer();
    const { hooks, click } = mountMarkup(viewer);
    click('[data-immersive-action="double-page"]');
    expect(viewer.toggleDoublePage).toHaveBeenCalledTimes(1);
    expect(document.querySelector('.immersive__btn-double-page').getAttribute('aria-pressed')).toBe('true');
    expect(document.querySelector('.immersive__viewer').classList.contains('is-double-page')).toBe(true);
    expect(hooks.updateFulltextAvail).toHaveBeenCalledTimes(1);
});

test('fullscreenchange re-fits the view once OSD has resized and relabels the button', () => {
    const viewer = fakeViewer();
    mountMarkup(viewer);
    document.dispatchEvent(new Event('fullscreenchange'));
    expect(viewer.onceResized).toHaveBeenCalledWith(expect.any(Function));
    const btn = document.querySelector('[data-immersive-action="fullscreen"]');
    expect(btn.getAttribute('aria-pressed')).toBe('false');
    expect(btn.getAttribute('aria-label')).toBe('Vollbild');
});

test('destroy cancels the pending fullscreen re-fit', () => {
    jest.useFakeTimers();
    try {
        const viewer = fakeViewer();
        const cancelResize = jest.fn();
        viewer.onceResized = jest.fn(() => cancelResize);
        const { bar } = mountMarkup(viewer);
        document.dispatchEvent(new Event('fullscreenchange'));
        // Bars of the earlier tests still listen on `document`, so only the delta is ours.
        const pending = jest.getTimerCount();
        bar.destroy();
        expect(jest.getTimerCount()).toBe(pending - 1);
        expect(cancelResize).toHaveBeenCalled();
        expect(viewer.resetView).not.toHaveBeenCalled();
    } finally {
        jest.useRealTimers();
    }
});

test('destroy detaches the buttons and the page-change subscription', () => {
    const viewer = fakeViewer();
    const { bar, click, indicator } = mountMarkup(viewer);
    bar.destroy();
    click('.immersive__bar-btn[data-immersive-page="next"]');
    viewer.show([3]);
    expect(viewer.next).not.toHaveBeenCalled();
    expect(indicator.textContent).toBe('1 / 5');
});
