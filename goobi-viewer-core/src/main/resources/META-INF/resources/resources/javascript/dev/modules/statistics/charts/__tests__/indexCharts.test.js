/**
 * @jest-environment jsdom
 */

// Browser-style file (no exports). Load it for its global side-effects: the IIFE attaches
// renderPublicationTypes / renderImportTrend / renderImportSummary onto window.viewerJS.statistics.charts.
require('../indexCharts.js');

describe('indexCharts (browser-bundle namespace)', () => {
    let mockChartCtor;

    beforeEach(() => {
        const canvas = document.createElement('canvas');
        canvas.id = 'test-canvas';
        document.body.appendChild(canvas);
        mockChartCtor = jest.fn();
        global.Chart = mockChartCtor;
        global.fetch = jest.fn();
    });

    afterEach(() => {
        document.body.innerHTML = '';
        delete global.Chart;
        delete global.fetch;
    });

    test('renderPublicationTypes_shouldFetchAndCreatePieChart', async () => {
        global.fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => [
                { label: 'Monograph', count: 50, query: 'Monograph' },
                { label: 'Periodical', count: 30, query: 'Periodical' },
            ],
        });

        await window.viewerJS.statistics.charts.renderPublicationTypes('test-canvas', '/api/x', '/search/');

        expect(global.fetch).toHaveBeenCalledWith('/api/x');
        expect(mockChartCtor).toHaveBeenCalledTimes(1);
        const config = mockChartCtor.mock.calls[0][1];
        expect(config.type).toBe('pie');
        expect(config.data.labels).toEqual(['Monograph', 'Periodical']);
        expect(config.data.datasets[0].data).toEqual([50, 30]);
    });

    test('renderImportTrend_shouldFetchAndCreateLineChartWithLuxonTimeAxis', async () => {
        global.fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => [
                { timestamp: 1700000000000, count: 5 },
                { timestamp: 1690000000000, count: 3 },
            ],
        });

        await window.viewerJS.statistics.charts.renderImportTrend('test-canvas', '/api/y');

        expect(mockChartCtor).toHaveBeenCalledTimes(1);
        const config = mockChartCtor.mock.calls[0][1];
        expect(config.type).toBe('line');
        expect(config.data.datasets[0].data).toHaveLength(2);
        // The chartjs-adapter-luxon was loaded for a reason — force usage by pinning the axis to type:'time'.
        expect(config.options.scales.x.type).toBe('time');
    });

    test('renderImportSummary_shouldFetchAndCreateBarChart', async () => {
        global.fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ pages: 100, fulltexts: 40 }),
        });

        await window.viewerJS.statistics.charts.renderImportSummary('test-canvas', '/api/z', 'Pages', 'Fulltexts');

        expect(mockChartCtor).toHaveBeenCalledTimes(1);
        const config = mockChartCtor.mock.calls[0][1];
        expect(config.type).toBe('bar');
        expect(config.data.labels).toEqual(['Pages', 'Fulltexts']);
        expect(config.data.datasets[0].data).toEqual([100, 40]);
    });
});
