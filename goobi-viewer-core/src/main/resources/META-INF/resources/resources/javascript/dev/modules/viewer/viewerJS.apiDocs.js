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
 * Module which renders the OpenAPI documentation of the viewer REST API with
 * Scalar API Reference. Replaces the former inline Swagger UI setup in
 * restApi.xhtml.
 *
 * @version 1.0.0
 * @module viewerJS.apiDocs
 */
var viewerJS = (function (viewer) {
    'use strict';

    const _debug = false;

    viewer.apiDocs = {
        /**
         * Appends a trailing slash where the viewer's internal request forwarding
         * needs one: without it a request does not reach its resource, which used
         * to break every POST fired from the docs page.
         *
         * @param {string} url a path or url
         * @returns {string} the input, with a trailing slash appended where needed
         */
        withTrailingSlash: function (url) {
            if (typeof url !== 'string' || url === '' || url.endsWith('/')) {
                return url;
            }
            return url + '/';
        },

        /**
         * Builds the Scalar configuration. Both API versions are offered as
         * sources so Scalar renders its own document switcher; the page no
         * longer needs the former hand-written v1/v2 buttons.
         *
         * Three options are mandatory, not cosmetic - each one prevents an
         * outbound request to a scalar.com host:
         *   withDefaultFonts   would load Inter and JetBrains Mono from fonts.scalar.com
         *   hideClientButton   would show a button posting the internal spec url to client.scalar.com
         *   showDeveloperTools defaults to localhost detection, which turns on the "Deploy",
         *                      "Register" and "Ask AI" actions on every developer machine
         * proxyUrl is deliberately unset - all requests are same-origin.
         *
         * @param {string} contextPath the servlet context path, may be empty
         * @param {string} localeString the viewer locale, e.g. 'de'
         * @returns {object} configuration for Scalar.createApiReference
         */
        buildConfig: function (contextPath, localeString) {
            const prefix = contextPath || '';
            // The viewer has historically shipped Hebrew translations under both
            // locale codes (messages_he.properties and messages_iw.properties), but
            // Scalar's RTL locale set only recognizes {ar, fa, he, ur}. Without this
            // mapping, 'iw' would render Scalar LTR and in English instead of Hebrew.
            const locale = localeString === 'iw' ? 'he' : localeString || 'en';
            return {
                sources: [
                    {
                        title: 'API v1',
                        slug: 'v1',
                        url: prefix + '/api/v1/openapi.json',
                        default: true,
                    },
                    {
                        title: 'API v2',
                        slug: 'v2',
                        url: prefix + '/api/v2/openapi.json',
                    },
                ],
                layout: 'modern',
                localization: { locale: locale },
                withDefaultFonts: false,
                hideClientButton: true,
                showDeveloperTools: 'never',

                // The viewer forwards REST requests internally, so a request without a
                // trailing slash never reaches its resource. Swagger UI did this in a
                // requestInterceptor; Scalar's equivalent is onBeforeRequest, and the
                // value to change is path.raw - path itself is an object {variables, raw}
                // that the url builder reads afterwards.
                //
                // The old interceptor skipped urls containing '?' because appending a
                // slash to a full url would have corrupted the query string. Scalar keeps
                // the query in builder.query, so no such guard is needed here - and
                // dropping it fixes requests that carry query parameters, which the old
                // implementation left without a slash.
                onBeforeRequest: function (context) {
                    const path = context && context.requestBuilder && context.requestBuilder.path;
                    if (!path) {
                        return;
                    }
                    path.raw = viewer.apiDocs.withTrailingSlash(path.raw);
                },
            };
        },

        /**
         * Mounts the API reference into the given container.
         *
         * @param {string} selector CSS selector of the mount container
         * @param {string} contextPath the servlet context path, may be empty
         * @param {string} localeString the viewer locale, e.g. 'de'
         */
        init: function (selector, contextPath, localeString) {
            if (typeof Scalar === 'undefined' || typeof Scalar.createApiReference !== 'function') {
                console.error('viewerJS.apiDocs: Scalar bundle not loaded, cannot render API documentation.');
                return;
            }
            const config = this.buildConfig(contextPath, localeString);
            if (_debug) {
                console.log('viewerJS.apiDocs: mounting into', selector, 'with', config);
            }
            Scalar.createApiReference(selector, config);
        },
    };

    return viewer;
})(viewerJS || {}, jQuery);

// CommonJS export for Jest. No-op in the browser where `module` is undefined.
// Mirrors the pattern in viewerJS.JsonValidator.js.
if (typeof module !== 'undefined' && module.exports) {
    module.exports = viewerJS;
}
