/**
 * Chart.js renderers for index-level statistics CMS components. Each function fetches its REST endpoint and renders
 * a Chart instance into the canvas given by id.
 *
 * Browser-style module: no `export` keywords, functions hung directly on the global viewerJS.statistics.charts
 * namespace so the file can be concatenated into the existing statistics.min.js Gulp bundle (the project's core
 * Maven build excludes dev/ from the JAR; only the bundled dist/*.min.js artifacts ship). For Jest, the same file
 * works because the namespace bootstrap at the bottom is guarded with a `typeof window !== 'undefined'` check; the
 * Jest setup polyfills `document` and `Chart`/`fetch` and reaches the functions through the global namespace too.
 */

(function () {
    'use strict';

    function ensureNamespace() {
        if (typeof window === 'undefined') {
            return null;
        }
        window.viewerJS = window.viewerJS || {};
        window.viewerJS.statistics = window.viewerJS.statistics || {};
        window.viewerJS.statistics.charts = window.viewerJS.statistics.charts || {};
        return window.viewerJS.statistics.charts;
    }

    async function fetchJson(url) {
        const resp = await fetch(url);
        if (!resp.ok) {
            throw new Error('Fetch failed: ' + url + ' ' + resp.status);
        }
        return resp.json();
    }

    async function renderPublicationTypes(canvasId, endpointUrl, searchBaseUrl) {
        const data = await fetchJson(endpointUrl);
        const ctx = document.getElementById(canvasId);
        if (!ctx) return;
        // queries piggy-back on the dataset for click-through; consumed by the onClick handler below.
        new Chart(ctx, {
            type: 'pie',
            data: {
                labels: data.map(function (d) {
                    return d.label;
                }),
                datasets: [
                    {
                        data: data.map(function (d) {
                            return d.count;
                        }),
                        queries: data.map(function (d) {
                            return d.query;
                        }),
                    },
                ],
            },
            options: {
                responsive: true,
                plugins: {
                    // Legend on the right so docstruct labels sit next to the pie instead of below it.
                    legend: { position: 'right' },
                    tooltip: {
                        callbacks: {
                            label: function (item) {
                                return item.label + ': ' + item.parsed;
                            },
                        },
                    },
                },
                onClick: function (evt, elements) {
                    if (!elements.length || !searchBaseUrl) return;
                    const idx = elements[0].index;
                    const query = data[idx].query;
                    window.location.href = searchBaseUrl + '-/-/-/' + encodeURIComponent('DOCSTRCT:' + query) + '/-/';
                },
            },
        });
    }

    async function renderImportTrend(canvasId, endpointUrl) {
        const data = await fetchJson(endpointUrl);
        const ctx = document.getElementById(canvasId);
        if (!ctx) return;
        // Reverse so the oldest bucket is plotted on the left.
        const sorted = data.slice().sort(function (a, b) {
            return a.timestamp - b.timestamp;
        });
        new Chart(ctx, {
            type: 'line',
            data: {
                datasets: [
                    {
                        label: '',
                        data: sorted.map(function (d) {
                            return { x: d.timestamp, y: d.count };
                        }),
                        tension: 0.3,
                    },
                ],
            },
            options: {
                responsive: true,
                plugins: { legend: { display: false } },
                scales: {
                    x: {
                        type: 'time',
                        time: { unit: 'month' },
                    },
                },
            },
        });
    }

    async function renderImportSummary(canvasId, endpointUrl, pagesLabel, fulltextsLabel) {
        const data = await fetchJson(endpointUrl);
        const ctx = document.getElementById(canvasId);
        if (!ctx) return;
        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: [pagesLabel, fulltextsLabel],
                datasets: [{ data: [data.pages, data.fulltexts] }],
            },
            options: {
                responsive: true,
                plugins: { legend: { display: false } },
            },
        });
    }

    const ns = ensureNamespace();
    if (ns) {
        ns.renderPublicationTypes = renderPublicationTypes;
        ns.renderImportTrend = renderImportTrend;
        ns.renderImportSummary = renderImportSummary;
    }
})();
