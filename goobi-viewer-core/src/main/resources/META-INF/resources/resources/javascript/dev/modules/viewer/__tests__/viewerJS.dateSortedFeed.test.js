/**
 * Unit tests for viewerJS.dateSortedFeed.
 *
 * jQuery + jsdom are wired up by jest-setup-browser.js. Most of this module
 * is DOM rendering and an external HTTP call via viewer.helper.getRemoteData;
 * the testable pure surface is the getter/setter API and the rendering
 * pipeline once we stub the remote call.
 */
const viewerJS = require('../viewerJS.dateSortedFeed.js');
const $ = global.$;

describe('viewerJS.dateSortedFeed', function () {
    afterEach(function () {
        document.body.innerHTML = '';
    });

    describe('getters and setters', function () {
        test('should round-trip the dataSortOrder', function () {
            viewerJS.dateSortedFeed.setDataSortOrder('asc');
            expect(viewerJS.dateSortedFeed.getDataSortOrder()).toBe('asc');
        });

        test('should round-trip the dataCount', function () {
            viewerJS.dateSortedFeed.setDataCount('20');
            expect(viewerJS.dateSortedFeed.getDataCount()).toBe('20');
        });

        test('should round-trip the dataEncoding', function () {
            viewerJS.dateSortedFeed.setDataEncoding('utf-8');
            expect(viewerJS.dateSortedFeed.getDataEncoding()).toBe('utf-8');
        });
    });

    describe('init + render', function () {
        test('should render German-formatted dates and titles into the configured feedBox', async function () {
            // Stub the helper that fetches the feed JSON. The module reads it
            // at runtime via viewer.helper.getRemoteData. Each entry holds a
            // `.date` plus arbitrarily-keyed work objects with .title (array)
            // and .url — the "datecentric" JSON shape.
            viewerJS.helper = viewerJS.helper || {};
            viewerJS.helper.getRemoteData = function () {
                return Promise.resolve([
                    {
                        date: '2024-01-15',
                        work1: { title: ['Werk Eins'], url: '/v/1' },
                    },
                    {
                        date: '2024-11-03',
                        work1: { title: ['Werk Zwei A', 'Werk Zwei B'], url: '/v/2' },
                    },
                ]);
            };

            document.body.innerHTML = '<div id="feed"></div>';
            const $box = $('#feed');

            viewerJS.dateSortedFeed.setDataSortOrder('desc');
            viewerJS.dateSortedFeed.setDataCount('10');
            viewerJS.dateSortedFeed.setDataEncoding('utf-8');
            viewerJS.dateSortedFeed.init({
                path: 'http://example.org',
                feedBox: $box,
            });

            // Wait one microtask for the resolved promise + render.
            await Promise.resolve();
            await Promise.resolve();

            const html = $box.html();
            // German date format dd. MonthName YYYY.
            expect(html).toContain('15. Januar 2024');
            expect(html).toContain('03. November 2024');
            // Both titles for the second entry rendered.
            expect(html).toContain('Werk Eins');
            expect(html).toContain('Werk Zwei A');
            expect(html).toContain('Werk Zwei B');
            // Anchor URLs preserved.
            expect(html).toContain('href="/v/1"');
        });

        test('should be a no-op when no feedBox is configured', function () {
            // No fetch should fire; we make the helper throw if called.
            // Note: the module's _defaults accumulate config across init()
            // calls, so we must explicitly null out feedBox to "unset" any
            // value left by an earlier test.
            viewerJS.helper = viewerJS.helper || {};
            viewerJS.helper.getRemoteData = jest.fn(function () {
                throw new Error('helper should not have been called');
            });

            viewerJS.dateSortedFeed.init({
                path: 'http://example.org',
                feedBox: null,
            });

            expect(viewerJS.helper.getRemoteData).not.toHaveBeenCalled();
        });
    });
});
