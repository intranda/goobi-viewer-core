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
});
