/**
 * Unit tests for viewerJS.slider.
 *
 * The module ships a Map of swiper-config presets and exposes
 * set/update/copy/get/getStyleNameOrDefault helpers around it. We
 * exercise that surface only — init() is left alone because it
 * subscribes to viewer.jsfAjax.success at call time and would need
 * the entire ajax pipeline mocked.
 */
const viewerJS = require('../viewerJS.slider.js');
const slider = viewerJS.slider;

describe('built-in styles', () => {
    test('the registry has the seven canonical built-in styles', () => {
        const expected = ['base', 'full-width', '3-slides-pagination', '3-slides-pagination-nav', 'fade-effect-auto-play', 'centered-mode', 'vertical-auto-play'];
        for (const name of expected) {
            expect(slider.styles.has(name)).toBe(true);
        }
    });

    test('the "base" style provides default swiper config keys', () => {
        const base = slider.styles.get('base');
        expect(base.maxSlides).toBe(20);
        expect(base.imageWidth).toBe(800);
        expect(base.swiperConfig.direction).toBe('horizontal');
    });
});

describe('set / update / copy', () => {
    afterEach(() => {
        // Clean up any styles registered during a test so we do not
        // leak state into subsequent specs.
        slider.styles.delete('test-style');
    });

    test('set(name, config) registers a brand-new style', () => {
        slider.set('test-style', { maxSlides: 5, swiperConfig: { loop: true } });
        expect(slider.styles.get('test-style')).toEqual({ maxSlides: 5, swiperConfig: { loop: true } });
    });

    test('update(name, fragment) deep-merges into an existing style', () => {
        slider.set('test-style', { maxSlides: 5, swiperConfig: { loop: true, slidesPerView: 1 } });
        slider.update('test-style', { swiperConfig: { slidesPerView: 3 } });
        const merged = slider.styles.get('test-style');
        // Original `loop: true` is kept, slidesPerView is overwritten.
        expect(merged.swiperConfig.loop).toBe(true);
        expect(merged.swiperConfig.slidesPerView).toBe(3);
        expect(merged.maxSlides).toBe(5);
    });

    test('copy(name) returns a deep clone (mutating the copy does not affect the registry)', () => {
        const copy = slider.copy('base');
        copy.maxSlides = 999;
        copy.swiperConfig.direction = 'vertical';
        expect(slider.styles.get('base').maxSlides).toBe(20);
        expect(slider.styles.get('base').swiperConfig.direction).toBe('horizontal');
    });
});

describe('get / getStyleNameOrDefault', () => {
    let warnSpy;

    beforeEach(() => {
        warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    });
    afterEach(() => {
        warnSpy.mockRestore();
    });

    test('get(name) returns the registered config for a known style', () => {
        const cfg = slider.get('full-width');
        expect(cfg.imageWidth).toBe(1920);
    });

    test('get(name) falls back to "base" and warns when the style is unknown', () => {
        const cfg = slider.get('does-not-exist');
        expect(cfg).toBe(slider.styles.get('base'));
        expect(warnSpy).toHaveBeenCalled();
    });

    test('getStyleNameOrDefault(name) returns the name when registered', () => {
        expect(slider.getStyleNameOrDefault('centered-mode')).toBe('centered-mode');
    });

    test('getStyleNameOrDefault(name) returns "base" for unregistered names', () => {
        expect(slider.getStyleNameOrDefault('zzz')).toBe('base');
    });
});
