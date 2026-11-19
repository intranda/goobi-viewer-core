/**
 * Unit tests for viewerJS.podlovePlayer.
 *
 * The color conversion needs a canvas, which jsdom does not provide, so readTokens() gets
 * a converter injected. init() runs against a mocked podlovePlayer().
 */
const viewerJS = require('../viewerJS.podlovePlayer.js');
const player = viewerJS.podlovePlayer;

const styleWith = (values) => ({ getPropertyValue: (name) => values[name] || '' });

describe('readTokens', () => {
    const identity = (value) => value || null;

    test('returns only the tokens the theme has set', () => {
        const style = styleWith({ '--podlove-player-brand': ' #1b73b3 ', '--podlove-player-contrast': '#1e2024' });
        expect(player.readTokens(style, identity)).toEqual({ brand: '#1b73b3', contrast: '#1e2024' });
    });

    test('returns an empty object when no token is set', () => {
        expect(player.readTokens(styleWith({}), identity)).toEqual({});
    });

    test('skips values the converter rejects', () => {
        const style = styleWith({ '--podlove-player-brand': 'not-a-color', '--podlove-player-alt': '#ffffff' });
        const onlyHex = (value) => (value.startsWith('#') ? value : null);
        expect(player.readTokens(style, onlyHex)).toEqual({ alt: '#ffffff' });
    });
});

describe('getLanguage', () => {
    test('uses the language of the browser if the player offers it', () => {
        expect(player.getLanguage('de-DE')).toBe('de');
        expect(player.getLanguage('fr')).toBe('fr');
    });

    test('falls back to English', () => {
        expect(player.getLanguage('ja-JP')).toBe('en');
        expect(player.getLanguage(undefined)).toBe('en');
    });
});

describe('frameCss', () => {
    test('draws the double ring with the given colors', () => {
        const css = player.frameCss('#cb002d', '#ffffff');
        expect(css).toContain('outline: 2px solid #ffffff');
        expect(css).toContain('box-shadow: 0 0 0 4px #cb002d');
    });

    test('stacks the transcript search and button on mobile', () => {
        const css = viewerJS.podlovePlayer.frameCss('#cb002d', '#ffffff');
        expect(css).toContain('max-width: 599px');
        expect(css).toContain('[data-test="tab-transcripts--follow"]');
    });

    test('contains the placeholder and reduced motion rules', () => {
        const css = player.frameCss('#cb002d', '#ffffff');
        expect(css).toContain('::placeholder');
        expect(css).toContain('prefers-reduced-motion');
    });
});

describe('init', () => {
    const config = {
        selector: '#podcastPlayer',
        restApiUrl: 'https://example.org/viewer/api/v1/',
        contextPath: '/viewer',
        pi: 'AB 1',
        showPi: '',
        title: 'Podcast player',
    };

    beforeEach(() => {
        document.body.innerHTML = '<div id="podcastPlayer"></div>';
        global.podlovePlayer = jest.fn(() => Promise.resolve());
    });

    afterEach(() => {
        delete global.podlovePlayer;
    });

    test('builds episode url and player config', () => {
        player.init(config);
        const [selector, episodeUrl, playerConfig] = global.podlovePlayer.mock.calls[0];
        expect(selector).toBe('#podcastPlayer');
        expect(episodeUrl).toBe('https://example.org/viewer/api/v1/records/AB%201/podlove/episode.json');
        expect(playerConfig.base).toBe('/viewer/resources/javascript/libs/podlove-player/');
        expect(playerConfig.version).toBe(5);
        expect(playerConfig['subscribe-button']).toBeUndefined();
        expect(playerConfig.theme).toBeUndefined();
    });

    test('adds the RSS subscribe button for episodes of a show', () => {
        player.init({ ...config, showPi: 'SHOW1' });
        const playerConfig = global.podlovePlayer.mock.calls[0][2];
        expect(playerConfig['subscribe-button']).toEqual({
            feed: 'https://example.org/viewer/api/v1/records/SHOW1/podcast/feed.rss',
            clients: [{ id: 'rss' }],
        });
    });

    test('logs an error if the player library is missing', () => {
        delete global.podlovePlayer;
        const error = jest.spyOn(console, 'error').mockImplementation(() => {});
        player.init(config);
        expect(error).toHaveBeenCalledWith('Podlove player library not loaded');
        error.mockRestore();
    });

    test('logs an error if the container is missing', () => {
        const error = jest.spyOn(console, 'error').mockImplementation(() => {});
        player.init({ ...config, selector: '#missing' });
        expect(error).toHaveBeenCalledWith('Podlove player container not found: #missing');
        expect(global.podlovePlayer).not.toHaveBeenCalled();
        error.mockRestore();
    });

    test('opens the chapters tab by default', () => {
        player.init(config);
        expect(global.podlovePlayer.mock.calls[0][2].activeTab).toBe('chapters');
    });

    test('opens the given tab', () => {
        player.init({ ...config, activeTab: 'transcripts' });
        expect(global.podlovePlayer.mock.calls[0][2].activeTab).toBe('transcripts');
    });

    test('starts collapsed with the tab "none"', () => {
        player.init({ ...config, activeTab: 'none' });
        expect(global.podlovePlayer.mock.calls[0][2]).not.toHaveProperty('activeTab');
    });

    test('reads the options from the data attributes of the container', () => {
        document.body.innerHTML =
            '<div id="podcastPlayer" data-rest-api-url="https://example.org/viewer/api/v1/" data-context-path="/viewer"' +
            ' data-pi="PI1" data-show-pi="SHOW1" data-title="Podcast player" data-language="de" data-active-tab="none"></div>';
        player.init({ selector: '#podcastPlayer' });
        const [, episodeUrl, playerConfig] = global.podlovePlayer.mock.calls[0];
        expect(episodeUrl).toBe('https://example.org/viewer/api/v1/records/PI1/podlove/episode.json');
        expect(playerConfig.base).toBe('/viewer/resources/javascript/libs/podlove-player/');
        expect(playerConfig['subscribe-button'].feed).toBe('https://example.org/viewer/api/v1/records/SHOW1/podcast/feed.rss');
        expect(playerConfig).not.toHaveProperty('activeTab');
    });

    test('prefers the config over the data attributes', () => {
        document.body.innerHTML = '<div id="podcastPlayer" data-pi="PI1" data-active-tab="none"></div>';
        player.init({ ...config, pi: 'PI2', activeTab: 'files' });
        const [, episodeUrl, playerConfig] = global.podlovePlayer.mock.calls[0];
        expect(episodeUrl).toContain('/records/PI2/');
        expect(playerConfig.activeTab).toBe('files');
    });

    test('treats an empty data attribute as not set', () => {
        document.body.innerHTML =
            '<div id="podcastPlayer" data-rest-api-url="https://example.org/api/v1" data-context-path="" data-pi="PI1" data-show-pi="" data-active-tab=""></div>';
        player.init({ selector: '#podcastPlayer' });
        const playerConfig = global.podlovePlayer.mock.calls[0][2];
        expect(playerConfig.activeTab).toBe('chapters');
        expect(playerConfig['subscribe-button']).toBeUndefined();
    });
});

describe('adjustments inside the player iframe', () => {
    const config = {
        selector: '#podcastPlayer',
        restApiUrl: 'https://example.org/viewer/api/v1/',
        contextPath: '/viewer',
        pi: 'PI1',
        title: 'Podcast-Player',
        language: 'de',
    };
    const BRAND = '--podlove-player-brand';
    const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

    let media;
    let values;
    let warn;

    // The mocked player creates the iframe like the real one: same origin, with the app root
    const embed = (setup) =>
        jest.fn((selector) => {
            const iframe = document.createElement('iframe');
            iframe.title = 'Podlove Web Player: Episode 1';
            document.querySelector(selector).appendChild(iframe);
            const doc = iframe.contentDocument;
            doc.body.innerHTML = '<div id="app"></div>';
            // Not implemented by jsdom
            iframe.contentWindow.Element.prototype.scrollIntoView = jest.fn();
            if (setup) {
                setup(iframe, doc);
            }
            return Promise.resolve();
        });
    const getIframe = () => document.querySelector('#podcastPlayer iframe');

    beforeEach(() => {
        document.body.innerHTML = '<div id="podcastPlayer"></div>';
        document.documentElement.className = '';
        // Not implemented by jsdom
        media = { addEventListener: jest.fn(), removeEventListener: jest.fn() };
        window.matchMedia = jest.fn(() => media);
        // Custom properties of the container; the color conversion is replaced because jsdom has no canvas
        values = {};
        jest.spyOn(window, 'getComputedStyle').mockImplementation(() => styleWith(values));
        global.CSS = { supports: () => true };
        jest.spyOn(HTMLCanvasElement.prototype, 'getContext').mockImplementation(() => ({
            fillRect: () => {},
            getImageData: () => ({ data: [27, 115, 179, 255] }),
        }));
        warn = jest.spyOn(console, 'warn').mockImplementation(() => {});
    });

    afterEach(() => {
        jest.useRealTimers();
        jest.restoreAllMocks();
        delete global.podlovePlayer;
        delete global.CSS;
        delete window.matchMedia;
    });

    test('localizes title and language of the iframe', async () => {
        global.podlovePlayer = embed();
        await player.init(config);
        const iframe = getIframe();
        expect(iframe.title).toBe('Podcast-Player: Episode 1');
        expect(iframe.getAttribute('aria-label')).toBe('Podcast-Player: Episode 1');
        expect(iframe.contentDocument.documentElement.lang).toBe('de');
        expect(warn).not.toHaveBeenCalled();
    });

    test('adds the frame css to the player document', async () => {
        values['--outline-focus'] = '#cb002d';
        global.podlovePlayer = embed();
        await player.init(config);
        const style = getIframe().contentDocument.head.querySelector('style');
        // Every color is converted by the mocked canvas
        expect(style.textContent).toContain('box-shadow: 0 0 0 4px #1b73b3');
        expect(style.textContent).toContain('prefers-reduced-motion');
    });

    test('sets the language on the i18n instance of the player once it is mounted', async () => {
        jest.useFakeTimers();
        const i18n = { global: { locale: 'en' } };
        let app;
        global.podlovePlayer = embed((iframe, doc) => (app = doc.getElementById('app')));
        await player.init(config);
        jest.advanceTimersByTime(300);
        expect(i18n.global.locale).toBe('en');
        const symbol = Symbol('i18n');
        app.__vue_app__ = { __VUE_I18N_SYMBOL__: symbol, _context: { provides: { [symbol]: i18n } } };
        jest.advanceTimersByTime(100);
        expect(i18n.global.locale).toBe('de');
        jest.advanceTimersByTime(10000);
        expect(warn).not.toHaveBeenCalled();
    });

    test('warns if the i18n instance of the player is not found', async () => {
        jest.useFakeTimers();
        global.podlovePlayer = embed();
        await player.init(config);
        jest.advanceTimersByTime(5000);
        expect(warn).not.toHaveBeenCalled();
        jest.advanceTimersByTime(200);
        expect(warn).toHaveBeenCalledTimes(1);
        expect(warn).toHaveBeenCalledWith('Podlove player language could not be set to "de"');
    });

    test('shows the focus of a hidden chapter entry on the visible row', async () => {
        global.podlovePlayer = embed((iframe, doc) => {
            doc.getElementById('app').innerHTML =
                '<div data-test="tab-chapters--entry" index="0"></div><div data-test="tab-chapters--entry" index="1"></div>' +
                '<ol class="sr-only"><li index="0"><button id="first"></button></li><li index="1"><button id="second"></button></li></ol>' +
                '<div id="slider"><span class="sr-only"><input id="range" /></span></div><button id="visible"></button>';
        });
        await player.init(config);
        const doc = getIframe().contentDocument;
        const fire = (id, type) => doc.getElementById(id).dispatchEvent(new doc.defaultView.Event(type, { bubbles: true }));
        const proxies = () => Array.from(doc.querySelectorAll('[data-focus-proxy]'));

        fire('second', 'focusin');
        expect(proxies()).toEqual([doc.querySelector('[data-test="tab-chapters--entry"][index="1"]')]);
        // Other hidden controls use their parent
        fire('range', 'focusin');
        expect(proxies()).toEqual([doc.getElementById('slider')]);
        fire('visible', 'focusin');
        expect(proxies()).toEqual([]);
        fire('first', 'focusin');
        fire('first', 'focusout');
        expect(proxies()).toEqual([]);
    });

    test('announces the error dialog of the player', async () => {
        global.podlovePlayer = embed();
        await player.init(config);
        const doc = getIframe().contentDocument;
        const region = doc.querySelector('[role="alert"]');
        expect(region.textContent).toBe('');
        doc.getElementById('app').innerHTML = '<div id="error"><h2 data-test="error--title"> Fehler </h2><p data-test="error--message">Datei nicht gefunden</p></div>';
        await wait(0);
        expect(region.textContent).toBe('Fehler. Datei nicht gefunden');
        doc.getElementById('error').remove();
        await wait(0);
        expect(region.textContent).toBe('');
    });

    test('passes changed theme colors to the player', async () => {
        const dispatch = jest.fn();
        values[BRAND] = '#1b73b3';
        global.podlovePlayer = embed((iframe) => (iframe.contentWindow.PODLOVE_STORE = { dispatch }));
        await player.init(config);
        // The initial colors are part of the player config
        expect(global.podlovePlayer.mock.calls[0][2].theme).toEqual({ tokens: { brand: '#1b73b3' } });
        expect(dispatch).not.toHaveBeenCalled();

        // No relevant value changed
        document.documentElement.className = 'modal-open';
        await wait(50);
        expect(dispatch).not.toHaveBeenCalled();

        values[BRAND] = '#ffffff';
        document.documentElement.className = 'dark';
        await wait(50);
        expect(dispatch).toHaveBeenCalledTimes(1);
        expect(dispatch).toHaveBeenCalledWith({ type: 'PLAYER_SET_THEME', payload: { version: 5, theme: { tokens: { brand: '#1b73b3' } } } });

        // System color scheme
        values[BRAND] = '#000000';
        media.addEventListener.mock.calls[0][1]();
        await wait(50);
        expect(dispatch).toHaveBeenCalledTimes(2);
    });

    test('does not fail on a color change if the player has no store', async () => {
        const error = jest.spyOn(console, 'error').mockImplementation(() => {});
        values[BRAND] = '#1b73b3';
        global.podlovePlayer = embed();
        await player.init(config);
        values[BRAND] = '#ffffff';
        document.documentElement.className = 'dark';
        await wait(50);
        // jsdom reports exceptions of callbacks on the console
        expect(error).not.toHaveBeenCalled();
        expect(window.getComputedStyle.mock.calls.length).toBeGreaterThan(2);
    });

    test('stops following the color scheme when the player is removed', async () => {
        global.podlovePlayer = embed();
        await player.init(config);
        expect(media.addEventListener).toHaveBeenCalledTimes(1);
        const refresh = media.addEventListener.mock.calls[0][1];
        document.body.innerHTML = '';
        document.documentElement.className = 'dark';
        await wait(50);
        expect(media.removeEventListener).toHaveBeenCalledWith('change', refresh);

        // The observer is disconnected: further changes are ignored
        media.removeEventListener.mockClear();
        document.documentElement.className = '';
        await wait(50);
        expect(media.removeEventListener).not.toHaveBeenCalled();
    });

    test('does nothing if the player created no iframe', async () => {
        global.podlovePlayer = jest.fn(() => Promise.resolve());
        await player.init(config);
        expect(window.matchMedia).not.toHaveBeenCalled();
        expect(warn).not.toHaveBeenCalled();
    });
});
