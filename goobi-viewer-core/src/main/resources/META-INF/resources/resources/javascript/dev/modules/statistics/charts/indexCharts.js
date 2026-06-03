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

    // Shared palette across statistics charts so visually-related charts (publication-types pie,
    // import-summary bars, most-edited-records bars) draw their slices/bars from the same set of
    // colors. Order chosen to match Chart.js' classic categorical palette.
    const PALETTE = [
        'rgb(255, 99, 132)', // pink-red
        'rgb(54, 162, 235)', // blue
        'rgb(255, 206, 86)', // yellow
        'rgb(75, 192, 192)', // teal
        'rgb(153, 102, 255)', // purple
        'rgb(255, 159, 64)', // orange
        'rgb(199, 199, 199)', // grey
    ];

    function paletteColors(count) {
        const out = new Array(count);
        for (let i = 0; i < count; i++) {
            out[i] = PALETTE[i % PALETTE.length];
        }
        return out;
    }

    function ensureNamespace() {
        if (typeof window === 'undefined') {
            return null;
        }
        window.viewerJS = window.viewerJS || {};
        window.viewerJS.statistics = window.viewerJS.statistics || {};
        window.viewerJS.statistics.charts = window.viewerJS.statistics.charts || {};
        // Expose the palette so the crowdsourcing module's renderers can reuse it.
        window.viewerJS.statistics.charts._palette = PALETTE;
        window.viewerJS.statistics.charts._paletteColors = paletteColors;
        return window.viewerJS.statistics.charts;
    }

    async function fetchJson(url) {
        const resp = await fetch(url);
        if (!resp.ok) {
            throw new Error('Fetch failed: ' + url + ' ' + resp.status);
        }
        return resp.json();
    }

    /**
     * Tear down any pre-existing Chart.js instance bound to {@code canvas}. JSF re-renders the CMS page (and any
     * f:ajax-driven update on a parent) cause our composite scripts to run again on the same canvas; without this,
     * Chart.js throws "Canvas is already in use". Idempotent — no-op if no chart is bound yet.
     */
    function destroyExistingChart(canvas) {
        if (typeof Chart !== 'undefined' && Chart.getChart) {
            const existing = Chart.getChart(canvas);
            if (existing) existing.destroy();
        }
    }

    async function renderPublicationTypes(canvasId, endpointUrl, searchBaseUrl, filter) {
        const data = await fetchJson(endpointUrl);
        const ctx = document.getElementById(canvasId);
        if (!ctx) return;
        destroyExistingChart(ctx);
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
                        backgroundColor: paletteColors(data.length),
                    },
                ],
            },
            options: {
                responsive: true,
                // The composite places the canvas inside a flex wrapper that already reserves the
                // remaining space below the heading. With maintainAspectRatio Chart.js would force
                // a 1:1 box and bleed past the container; letting the wrapper drive the size keeps
                // the canvas inside the framed box.
                maintainAspectRatio: false,
                plugins: {
                    // No legend — docstruct labels are visible via tooltip on hover, and the user
                    // requested a clean pie without the side-list.
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: function (item) {
                                return item.label + ': ' + item.parsed;
                            },
                        },
                    },
                },
                onClick: function (evt, elements) {
                    // When a filter is active, click-through is disabled: safely combining the admin's filter with
                    // the slice's DOCSTRCT term is non-trivial (the filter can contain its own boolean structure),
                    // so v1 just skips navigation. Tooltip on hover still works.
                    if (filter) return;
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
        destroyExistingChart(ctx);
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
                        borderColor: PALETTE[1], // blue, matches second slice in the pie
                        backgroundColor: PALETTE[1],
                        borderWidth: 2,
                        pointRadius: 4,
                        pointHoverRadius: 6,
                    },
                ],
            },
            options: {
                responsive: true,
                // Wrapper-driven sizing — see the publication-types renderer above.
                maintainAspectRatio: false,
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
        destroyExistingChart(ctx);
        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: [pagesLabel, fulltextsLabel],
                datasets: [
                    {
                        data: [data.pages, data.fulltexts],
                        // Yellow + green — distinct from the publication-types pie above, and visually
                        // less aggressive than the original red/blue pair.
                        backgroundColor: [PALETTE[2], PALETTE[3]],
                    },
                ],
            },
            options: {
                responsive: true,
                // Wrapper-driven sizing — see the publication-types renderer above.
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
            },
        });
    }

    async function renderPublicationCenturies(canvasId, endpointUrl, logarithmicScale) {
        const data = await fetchJson(endpointUrl);
        const ctx = document.getElementById(canvasId);
        if (!ctx) return;
        // X-axis labels are formatted client-side as "{century}. Jh." — see the plan footnote about pushing this
        // to the server if locale-specific formatting becomes important.
        const labels = data.map(function (d) {
            return d.century + '. Jh.';
        });
        destroyExistingChart(ctx);
        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [
                    {
                        data: data.map(function (d) {
                            return d.count;
                        }),
                        backgroundColor: paletteColors(data.length),
                    },
                ],
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                // Y-axis scale is admin-toggleable: log compresses the long-tail centuries when one bucket
                // dominates (typical for periodicals over a few decades), linear is the default.
                scales: {
                    y: {
                        type: logarithmicScale ? 'logarithmic' : 'linear',
                        beginAtZero: !logarithmicScale,
                    },
                },
            },
        });
    }

    async function renderLanguages(canvasId, endpointUrl) {
        const data = await fetchJson(endpointUrl);
        const ctx = document.getElementById(canvasId);
        if (!ctx) return;
        destroyExistingChart(ctx);
        new Chart(ctx, {
            // Doughnut instead of pie so the centre is empty and the side legend reads as the main key —
            // most languages collapse to a long tail and the side legend is the readable layout.
            type: 'doughnut',
            data: {
                labels: data.map(function (d) {
                    return d.label;
                }),
                datasets: [
                    {
                        data: data.map(function (d) {
                            return d.count;
                        }),
                        backgroundColor: paletteColors(data.length),
                    },
                ],
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: true, position: 'right' },
                },
            },
        });
    }

    async function renderTopCollections(canvasId, endpointUrl, browseBaseUrl) {
        const data = await fetchJson(endpointUrl);
        const ctx = document.getElementById(canvasId);
        if (!ctx) return;
        destroyExistingChart(ctx);
        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: data.map(function (d) {
                    return d.label;
                }),
                datasets: [
                    {
                        data: data.map(function (d) {
                            return d.count;
                        }),
                        // queries piggy-back on the dataset for click-through; consumed by onClick below.
                        names: data.map(function (d) {
                            return d.name;
                        }),
                        backgroundColor: paletteColors(data.length),
                    },
                ],
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                onClick: function (evt, elements) {
                    if (!elements.length || !browseBaseUrl) return;
                    const idx = elements[0].index;
                    const name = data[idx].name;
                    // Pretty-config browse mapping: /browse/{collection}/{search}/{page}/{sort}/{activeFacets}/.
                    // The DC token belongs in the activeFacets slot, not the search slot.
                    window.location.href = browseBaseUrl + '-/-/1/-/' + encodeURIComponent('DC:' + name) + '/';
                },
            },
        });
    }

    const ns = ensureNamespace();
    if (ns) {
        ns.renderPublicationTypes = renderPublicationTypes;
        ns.renderImportTrend = renderImportTrend;
        ns.renderImportSummary = renderImportSummary;
        ns.renderPublicationCenturies = renderPublicationCenturies;
        ns.renderLanguages = renderLanguages;
        ns.renderTopCollections = renderTopCollections;
    }
})();
