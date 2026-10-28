/**
 * Record showcase
 *
 * Controls of the record showcase CMS component: scrubber, step buttons and progress for a swiper row.
 * Themes pass init as the swiper "beforeInit" callback of their showcase slider.
 *
 * @module cmsJS.recordShowcase
 */
var cmsJS = (function (cms) {
    'use strict';

    var _module = {};
    // one swiper per showcase
    const _sliders = new WeakMap();

    /** Connects the controls to the swiper */
    function initRecordShowcase(swiper) {
        const root = swiper.el.closest('.cms-record-showcase');
        if (!root) return;

        const previousSwiper = _sliders.get(root);
        if (previousSwiper && previousSwiper !== swiper && !previousSwiper.destroyed) previousSwiper.destroy(false, false);
        _sliders.set(root, swiper);

        const controls = root.querySelector('.cms-record-showcase__controls');
        const progress = root.querySelector('.cms-record-showcase__progress');
        const scrubber = root.querySelector('.cms-record-showcase__scrubber');
        const prev = root.querySelector('.cms-record-showcase__step--prev');
        const next = root.querySelector('.cms-record-showcase__step--next');
        const navigation = { prevEl: prev, nextEl: next };
        Object.assign(swiper.params.navigation, navigation);
        Object.assign(swiper.originalParams.navigation, navigation);

        if (prev?.getAttribute('aria-label')) swiper.params.a11y.prevSlideMessage = prev.getAttribute('aria-label');
        if (next?.getAttribute('aria-label')) swiper.params.a11y.nextSlideMessage = next.getAttribute('aria-label');
        Object.assign(swiper.originalParams.a11y, {
            prevSlideMessage: swiper.params.a11y.prevSlideMessage,
            nextSlideMessage: swiper.params.a11y.nextSlideMessage,
        });

        const reducedMotionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
        const updateMotion = () => {
            swiper.params.speed = reducedMotionQuery.matches ? 0 : 560;
            swiper.originalParams.speed = swiper.params.speed;
            progress?.style.setProperty('--_showcase-transition-duration', `${swiper.params.speed}ms`);
        };
        updateMotion();
        reducedMotionQuery.addEventListener('change', updateMotion);

        const formatIndex = (value) => String(value).padStart(2, '0');
        const syncControls = () => {
            if (swiper.destroyed) return;
            const count = swiper.slides.length;
            const lastPage = Math.max(0, swiper.snapGrid.length - 1);
            const page = Math.min(lastPage, Math.max(0, swiper.snapIndex || 0));
            const locked = count < 2 || swiper.isLocked || lastPage === 0;
            const position = locked ? 0 : page / lastPage;
            const visible = swiper.visibleSlidesIndexes || [];
            const first = count ? (visible.length ? Math.min(...visible) : swiper.activeIndex) + 1 : 0;
            const last = count ? (visible.length ? Math.max(...visible) : swiper.activeIndex) + 1 : 0;
            const range = first === last ? formatIndex(first) : `${formatIndex(first)}–${formatIndex(last)}`;

            if (controls) controls.hidden = count === 0;
            if (progress) {
                progress.style.setProperty('--_showcase-progress-position', swiper.rtlTranslate ? 1 - position : position);
                progress.style.setProperty('--_showcase-visible-fraction', locked ? 1 : Math.min(1, swiper.params.slidesPerView / count));
            }
            if (scrubber) {
                scrubber.disabled = locked;
                scrubber.max = lastPage;
                scrubber.step = 1;
                scrubber.value = page;
                scrubber.setAttribute('aria-valuetext', `${range} / ${formatIndex(count)}`);
                if (swiper.wrapperEl.id) scrubber.setAttribute('aria-controls', swiper.wrapperEl.id);
            }
            [
                [prev, locked || swiper.isBeginning],
                [next, locked || swiper.isEnd],
            ].forEach(([button, disabled]) => {
                if (!button) return;
                button.disabled = disabled;
                button.setAttribute('aria-disabled', String(disabled));
                button.classList.toggle('swiper-button-disabled', disabled);
            });
            swiper.slides.forEach((slide) => {
                const heading = slide.querySelector('.slider-showcase__header');
                if (heading) heading.title = heading.textContent.trim();
            });
        };
        const showPage = (page) => {
            const target = Math.round(Math.min(swiper.snapGrid.length - 1, Math.max(0, page)));
            swiper.slideTo(target * swiper.params.slidesPerGroup, swiper.params.speed);
            syncControls();
        };
        const scrub = () => {
            if (!scrubber.disabled) showPage(Number(scrubber.value));
        };
        const navigate = (event) => {
            if (scrubber.disabled || event.altKey || event.ctrlKey || event.metaKey) return;
            const horizontal = swiper.rtlTranslate ? -1 : 1;
            const increments = { ArrowRight: horizontal, ArrowLeft: -horizontal, ArrowUp: 1, ArrowDown: -1 };
            if (event.key in increments) {
                event.preventDefault();
                showPage(swiper.snapIndex + increments[event.key]);
            } else if (event.key === 'Home' || event.key === 'End') {
                event.preventDefault();
                showPage(event.key === 'End' ? swiper.snapGrid.length - 1 : 0);
            } else if (event.key === 'PageUp' || event.key === 'PageDown') {
                event.preventDefault();
                showPage(swiper.snapIndex + (event.key === 'PageDown' ? 1 : -1));
            }
        };
        scrubber?.addEventListener('input', scrub);
        scrubber?.addEventListener('keydown', navigate);

        const updateImages = (event) => {
            if (event.target.tagName === 'IMG' && !swiper.destroyed) swiper.update();
        };
        swiper.el.addEventListener('load', updateImages, true);
        swiper.el.addEventListener('error', updateImages, true);

        const removalObserver = new MutationObserver(() => {
            if (!swiper.destroyed && !swiper.el.isConnected) swiper.destroy(true, false);
        });
        removalObserver.observe(root.ownerDocument.documentElement, { childList: true, subtree: true });

        swiper.on('setTransition', (_swiper, duration) => {
            progress?.style.setProperty('--_showcase-transition-duration', `${duration}ms`);
        });
        swiper.on('afterInit update resize slidesUpdated snapIndexChange setTranslate lock unlock', syncControls);
        swiper.on('destroy', () => {
            removalObserver.disconnect();
            reducedMotionQuery.removeEventListener('change', updateMotion);
            scrubber?.removeEventListener('input', scrub);
            scrubber?.removeEventListener('keydown', navigate);
            swiper.el.removeEventListener('load', updateImages, true);
            swiper.el.removeEventListener('error', updateImages, true);
            if (_sliders.get(root) === swiper) {
                _sliders.delete(root);
                if (controls) controls.hidden = true;
            }
        });
    }

    _module.init = initRecordShowcase;

    cms.recordShowcase = _module;
    return cms;
})(cmsJS || {});

if (typeof module !== 'undefined' && module.exports) {
    module.exports = cmsJS.recordShowcase;
}
