/**
 * Chart.js renderers for index-level statistics CMS components. Each function fetches its REST endpoint, then renders
 * a Chart instance into the canvas given by id.
 *
 * The functions are also exposed on the global viewerJS.statistics.charts namespace for inline-script use from JSF.
 */

export async function renderPublicationTypes(canvasId, endpointUrl, searchBaseUrl) {
    const data = await fetchJson(endpointUrl);
    const ctx = document.getElementById(canvasId);
    if (!ctx) return;
    new Chart(ctx, {
        type: 'pie',
        data: {
            labels: data.map((d) => d.label),
            datasets: [
                {
                    data: data.map((d) => d.count),
                    // queries piggy-back on the dataset for click-through; consumed by the onClick handler.
                    queries: data.map((d) => d.query),
                },
            ],
        },
        options: {
            responsive: true,
            plugins: {
                legend: { position: 'bottom' },
                tooltip: { callbacks: { label: (item) => `${item.label}: ${item.parsed}` } },
            },
            onClick: (evt, elements) => {
                if (!elements.length || !searchBaseUrl) return;
                const idx = elements[0].index;
                const query = data[idx].query;
                window.location.href = `${searchBaseUrl}-/-/-/${encodeURIComponent('DOCSTRCT:' + query)}/-/`;
            },
        },
    });
}

export async function renderImportTrend(canvasId, endpointUrl) {
    const data = await fetchJson(endpointUrl);
    const ctx = document.getElementById(canvasId);
    if (!ctx) return;
    // Reverse so the oldest bucket is plotted on the left.
    const sorted = [...data].sort((a, b) => a.timestamp - b.timestamp);
    new Chart(ctx, {
        type: 'line',
        data: {
            datasets: [
                {
                    label: '',
                    data: sorted.map((d) => ({ x: d.timestamp, y: d.count })),
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

export async function renderImportSummary(canvasId, endpointUrl, pagesLabel, fulltextsLabel) {
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

async function fetchJson(url) {
    const resp = await fetch(url);
    if (!resp.ok) throw new Error(`Fetch failed: ${url} ${resp.status}`);
    return resp.json();
}

// Expose on viewerJS.statistics.charts for inline xhtml use.
if (typeof window !== 'undefined') {
    window.viewerJS = window.viewerJS || {};
    window.viewerJS.statistics = window.viewerJS.statistics || {};
    window.viewerJS.statistics.charts = window.viewerJS.statistics.charts || {};
    window.viewerJS.statistics.charts.renderPublicationTypes = renderPublicationTypes;
    window.viewerJS.statistics.charts.renderImportTrend = renderImportTrend;
    window.viewerJS.statistics.charts.renderImportSummary = renderImportSummary;
}
