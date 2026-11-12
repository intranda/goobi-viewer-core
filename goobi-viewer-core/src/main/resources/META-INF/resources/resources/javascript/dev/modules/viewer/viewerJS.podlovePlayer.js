/**
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information. - http://www.intranda.com -
 * http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * @description Embeds the Podlove Web Player: theme colors from CSS custom properties
 * and accessibility fixes inside the player iframe.
 * @version 1.0.0
 * @module viewerJS.podlovePlayer
 */
var viewerJS = (function (viewer) {
    'use strict';

    // Player color tokens and the custom property a theme sets for each of them
    const COLOR_TOKENS = {
        brand: '--podlove-player-brand',
        brandDark: '--podlove-player-brand-dark',
        brandDarkest: '--podlove-player-brand-darkest',
        brandLightest: '--podlove-player-brand-lightest',
        shadeDark: '--podlove-player-shade-dark',
        shadeBase: '--podlove-player-shade-base',
        contrast: '--podlove-player-contrast',
        alt: '--podlove-player-alt',
    };
    // UI languages of the bundled player (5.13.0), English is the fallback
    const LANGUAGES = ['en', 'de', 'eo', 'fr', 'cs', 'ar'];

    let _probe = null;

    // Any CSS color to #rrggbb; the player does not understand color-mix() or relative colors
    function _toHex(color) {
        if (!color || !CSS.supports('color', color)) {
            return null;
        }
        _probe = _probe || document.createElement('canvas').getContext('2d');
        // Paint over white so that transparent colors become opaque
        _probe.fillStyle = '#ffffff';
        _probe.fillRect(0, 0, 1, 1);
        _probe.fillStyle = color;
        _probe.fillRect(0, 0, 1, 1);
        const rgb = _probe.getImageData(0, 0, 1, 1).data;
        return '#' + [rgb[0], rgb[1], rgb[2]].map((channel) => channel.toString(16).padStart(2, '0')).join('');
    }

    // Tokens the theme has set; unset ones keep the Podlove default
    function _readTokens(style, toHex) {
        const convert = toHex || _toHex;
        const tokens = {};
        Object.keys(COLOR_TOKENS).forEach((token) => {
            const hex = convert(style.getPropertyValue(COLOR_TOKENS[token]).trim());
            if (hex) {
                tokens[token] = hex;
            }
        });
        return tokens;
    }

    function _getLanguage(browserLanguage) {
        const language = (browserLanguage || 'en').split('-')[0];
        return LANGUAGES.indexOf(language) > -1 ? language : 'en';
    }

    // CSS for the player document: focus ring, placeholder, reduced motion
    function _frameCss(ring, inner) {
        return (
            // Double ring like the viewer; the player removes the outline of its sliders (WCAG 2.4.7)
            '#app :focus-visible { outline: 2px solid ' + inner + '; outline-offset: -2px; box-shadow: 0 0 0 4px ' + ring + '; }' +
            // Hidden controls mirror their focus on a visible element; drawn inside so scroll areas do not clip it
            '#app [data-focus-proxy] { outline: 2px solid ' + ring + '; outline-offset: -2px; box-shadow: inset 0 0 0 4px ' + inner + '; }' +
            // The default placeholder color has about 1.4:1 contrast (WCAG 1.4.3)
            '#app .input::placeholder { color: inherit; opacity: 0.75; }' +
            // Transcript tab on mobile (player breakpoint): search field above the "stop following" button
            '@media (max-width: 599px) {' +
            ' #app :has(> [data-test="tab-transcripts--follow"]) { flex-direction: column; }' +
            ' #app :has(> [data-test="tab-transcripts--follow"]) > [data-test="tab-transcripts--follow"] { width: 100%; margin: 0.5rem 0 0; }' +
            // The header has a fixed height of 145px, too small for the stacked controls
            ' #app .header:has([data-test="tab-transcripts--follow"]) { height: auto; }' +
            '}' +
            '@media (prefers-reduced-motion: reduce) { #app *, #app *::before, #app *::after { animation: none !important; transition: none !important; } }'
        );
    }

    // The player moves focus into visually hidden controls (chapter list, second slider); show the ring on their visible counterpart
    function _mirrorFocus(doc) {
        const clear = () => {
            doc.querySelectorAll('[data-focus-proxy]').forEach((element) => element.removeAttribute('data-focus-proxy'));
        };
        doc.addEventListener('focusout', clear);
        doc.addEventListener('focusin', (event) => {
            clear();
            const hidden = event.target.closest('.sr-only');
            if (!hidden) {
                return;
            }
            // The hidden chapter list mirrors the visible rows by index
            const item = event.target.closest('li[index]');
            const proxy = item ? doc.querySelector('[data-test="tab-chapters--entry"][index="' + item.getAttribute('index') + '"]') : hidden.parentElement;
            if (proxy) {
                proxy.setAttribute('data-focus-proxy', '');
                proxy.scrollIntoView({ block: 'nearest' });
            }
        });
    }

    // The player has no live regions; announce its error dialog (WCAG 4.1.3)
    function _announceErrors(doc) {
        const region = doc.createElement('div');
        region.className = 'sr-only';
        region.setAttribute('role', 'alert');
        doc.body.appendChild(region);
        new MutationObserver(() => {
            // Cheap early exit: the observer also fires for timer updates during playback
            const error = doc.getElementById('error');
            if (!error && !region.textContent) {
                return;
            }
            const text = ['title', 'message']
                .map((part) => error && error.querySelector('[data-test="error--' + part + '"]'))
                .filter((element) => element)
                .map((element) => element.textContent.trim())
                .join('. ');
            if (region.textContent !== text) {
                region.textContent = text;
            }
        }).observe(doc.getElementById('app') || doc.body, { childList: true, subtree: true });
    }

    // The player takes its language from the browser and does not react to changes later; set it on its i18n instance
    function _setLanguage(doc, language) {
        let tries = 0;
        const timer = setInterval(() => {
            const app = doc.getElementById('app') && doc.getElementById('app').__vue_app__;
            const i18n = app && app._context.provides[app.__VUE_I18N_SYMBOL__];
            if (i18n) {
                i18n.global.locale = language;
            }
            // Wait for the player to mount
            if (i18n) {
                clearInterval(timer);
            } else if (++tries > 50) {
                clearInterval(timer);
                // Relies on internals of the player (Vue app and vue-i18n instance); check them after a player update
                console.warn('Podlove player language could not be set to "' + language + '"');
            }
        }, 100);
    }

    // Adjusts the player iframe once the player has created it (same-origin, written by the player itself)
    function _improve(container, config) {
        const iframe = container.querySelector('iframe');
        const doc = iframe && iframe.contentDocument;
        if (!doc || !doc.head) {
            return;
        }
        // The player names the iframe in English only
        iframe.title = iframe.title.replace(/^Podlove Web Player/, config.title);
        iframe.setAttribute('aria-label', iframe.title);
        const language = _getLanguage(config.language || navigator.language);
        doc.documentElement.lang = language;
        _setLanguage(doc, language);
        _mirrorFocus(doc);
        _announceErrors(doc);

        const style = doc.createElement('style');
        doc.head.appendChild(style);
        const properties = Object.values(COLOR_TOKENS).concat(['--outline-focus', '--outline-focus-inner']);
        let signature = '';
        const media = window.matchMedia('(prefers-color-scheme: dark)');
        const refresh = () => requestAnimationFrame(() => update(true));
        const observer = new MutationObserver(refresh);
        const update = (retheme) => {
            // The player is gone (e.g. replaced by an Ajax update): stop following the color scheme
            if (!iframe.isConnected) {
                observer.disconnect();
                media.removeEventListener('change', refresh);
                return;
            }
            const computed = getComputedStyle(container);
            // Skip the color conversion if no relevant value changed (the html class also changes for modals etc.)
            const current = properties.map((name) => computed.getPropertyValue(name)).join('|');
            if (current === signature) {
                return;
            }
            signature = current;
            const tokens = _readTokens(computed);
            style.textContent = _frameCss(_toHex(computed.getPropertyValue('--outline-focus')) || tokens.contrast || '#000000', _toHex(computed.getPropertyValue('--outline-focus-inner')) || '#ffffff');
            const store = iframe.contentWindow && iframe.contentWindow.PODLOVE_STORE;
            if (retheme && store && Object.keys(tokens).length > 0) {
                store.dispatch({ type: 'PLAYER_SET_THEME', payload: { version: 5, theme: { tokens: tokens } } });
            }
        };
        update(false);

        // Follow the color scheme: theme toggle (class on html) and system setting
        observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] });
        media.addEventListener('change', refresh);
    }

    viewer.podlovePlayer = {
        /**
         * Every option except the selector may also be given as data attribute of the container (data-rest-api-url,
         * data-context-path, data-pi, data-show-pi, data-title, data-language, data-active-tab); the config has precedence.
         *
         * @method init
         * @param {Object} config
         * @param {String} config.selector Container of the player
         * @param {String} [config.restApiUrl] REST API base URL
         * @param {String} [config.contextPath] Context path of the viewer
         * @param {String} [config.pi] Identifier of the episode
         * @param {String} [config.showPi] Identifier of the show; enables the RSS subscribe button
         * @param {String} [config.title] Localized title of the player iframe
         * @param {String} [config.language] Language of the viewer, defaults to the browser language
         * @param {String} [config.activeTab] Tab that is open on load, defaults to 'chapters'; 'none' starts the player collapsed
         * @returns {Promise|undefined} Resolves when the player is embedded and adjusted
         */
        init: function (config) {
            if (typeof podlovePlayer === 'undefined') {
                console.error('Podlove player library not loaded');
                return;
            }
            const container = document.querySelector(config.selector);
            if (!container) {
                console.error('Podlove player container not found: ' + config.selector);
                return;
            }
            const data = container.dataset;
            config = Object.assign(
                {
                    restApiUrl: data.restApiUrl,
                    contextPath: data.contextPath,
                    pi: data.pi,
                    showPi: data.showPi,
                    title: data.title,
                    language: data.language,
                    activeTab: data.activeTab,
                },
                config
            );
            const restApiUrl = config.restApiUrl.replace(/\/$/, '');
            const playerConfig = {
                version: 5,
                // Location of the lazy-loaded player assets
                base: config.contextPath + '/resources/javascript/libs/podlove-player/',
                share: { channels: ['link', 'mail'], sharePlaytime: true },
            };
            // Tab that is open on load; without one the player starts collapsed and tabs open on click
            const activeTab = config.activeTab || 'chapters';
            if (activeTab !== 'none') {
                playerConfig.activeTab = activeTab;
            }
            const tokens = _readTokens(getComputedStyle(container));
            if (Object.keys(tokens).length > 0) {
                playerConfig.theme = { tokens: tokens };
            }
            // Only clients that subscribe by feed URL; directory clients would point to a listing that does not exist
            if (config.showPi) {
                playerConfig['subscribe-button'] = {
                    feed: restApiUrl + '/records/' + encodeURIComponent(config.showPi) + '/podcast/feed.rss',
                    clients: [{ id: 'rss' }],
                };
            }
            const episodeUrl = restApiUrl + '/records/' + encodeURIComponent(config.pi) + '/podlove/episode.json';
            return Promise.resolve(podlovePlayer(config.selector, episodeUrl, playerConfig))
                .then(() => _improve(container, config))
                .catch((error) => console.warn('Podlove player adjustments failed', error));
        },
        // Exposed for tests
        readTokens: _readTokens,
        getLanguage: _getLanguage,
        frameCss: _frameCss,
    };

    return viewer;
})(viewerJS || {});

// CommonJS export for Jest. No-op in the browser where `module` is undefined.
if (typeof module !== 'undefined' && module.exports) {
    module.exports = viewerJS;
}
