/**
 * Unit tests for the Statistics module.
 *
 * Replaces the deleted Jasmine spec `tests/spec/statistics-specs.js`. The
 * legacy spec exercised mostly two things: (1) the existence of each chart
 * constructor, and (2) that calling .plot() invokes the global
 * `jQuery.jqplot` plotter. We do the same here, plus a real unit test for
 * the data-transformation logic in PublicationTypes.plot() and for the
 * pure helpers `Statistics.shortString` / `Statistics.getBarSize`.
 *
 * jqplot is not in our dev dependencies; it's stubbed with the shape the
 * module touches at load time and at plot-time.
 */
// jQuery + jsdom are wired up by jest-setup-browser.js.
// Stub jqplot. The module reads $.jqplot.config.enablePlugins at load and
// references PieRenderer/BarRenderer/CategoryAxisRenderer/AxisTickRenderer
// inside plot(). We track every call to assert it gets invoked.
const jqplotCalls = [];
global.$.jqplot = function () {
    jqplotCalls.push(Array.prototype.slice.call(arguments));
    return {};
};
global.$.jqplot.config = { enablePlugins: false };
global.$.jqplot.PieRenderer = function () {};
global.$.jqplot.BarRenderer = function () {};
global.$.jqplot.CategoryAxisRenderer = function () {};
global.$.jqplot.AxisTickRenderer = function () {};

const Statistics = require('../statistics.js');

describe('Statistics', function () {
    beforeEach(function () {
        jqplotCalls.length = 0;
        document.body.innerHTML = '<div id="graph-div"></div><div id="fulltext-div"></div><div id="content-div"></div>';
    });

    test('should expose the Statistics namespace', function () {
        expect(Statistics).toBeDefined();
    });

    describe('chart constructors', function () {
        test.each([['PublicationTypes'], ['MostEditedWorks'], ['NumberOfPages'], ['MostImportedWorksTrend'], ['CrowdSourcingProgress']])(
            'should expose %s as a constructor with a .plot method',
            function (name) {
                expect(typeof Statistics[name]).toBe('function');
                expect(typeof Statistics[name].prototype.plot).toBe('function');
            }
        );
    });

    describe('PublicationTypes.plot', function () {
        test('should parse the bracketed `Type::count::Label` list and pass parsed numeric counts to jqplot', function () {
            const plotter = new Statistics.PublicationTypes({
                labelList: '[Monograph::1337::Monographie,Manuscript::42::Handschrift,Periodical::21::Zeitschrift]',
                labelDesc: 'Werke',
            });
            plotter.plot('graph-div');

            expect(jqplotCalls.length).toBe(1);
            const [divId, dataSeries] = jqplotCalls[0];
            expect(divId).toBe('graph-div');
            // [[ ['Monograph', 1337, 'Monographie'], ['Manuscript', 42, 'Handschrift'], ... ]]
            expect(dataSeries).toEqual([
                [
                    ['Monograph', 1337, 'Monographie'],
                    ['Manuscript', 42, 'Handschrift'],
                    ['Periodical', 21, 'Zeitschrift'],
                ],
            ]);
        });
    });

    describe('MostEditedWorks.plot / NumberOfPages.plot / MostImportedWorksTrend.plot', function () {
        test('MostEditedWorks should invoke jqplot', function () {
            new Statistics.MostEditedWorks({ labelList: '[]', labelDesc: 'Label' }).plot('graph-div');
            expect(jqplotCalls.length).toBeGreaterThanOrEqual(1);
        });

        test('NumberOfPages should invoke jqplot', function () {
            new Statistics.NumberOfPages({
                titlePages: 'Pages',
                numPages: '100',
                titleFullTexts: 'Fulltexts',
                numFullTexts: '50',
            }).plot('graph-div');
            expect(jqplotCalls.length).toBeGreaterThanOrEqual(1);
        });
    });

    describe('CrowdSourcingProgress.plot', function () {
        test('should NOT invoke jqplot — sets bar widths via CSS instead', function () {
            // Mirrors the original spec assertion: this constructor uses CSS,
            // not jqplot.
            new Statistics.CrowdSourcingProgress({
                fulltextProgressString: '50/100',
                contentProgressString: '25/100',
            }).plot('fulltext-div', 'content-div');
            expect(jqplotCalls.length).toBe(0);
        });

        test('should set bar widths proportional to the progress string', function () {
            // 50/100 of 160px = 80px ; 25/100 of 160px = 40px
            new Statistics.CrowdSourcingProgress({
                fulltextProgressString: '50/100',
                contentProgressString: '25/100',
            }).plot('fulltext-div', 'content-div');
            expect($('#fulltext-div').css('width')).toBe('80px');
            expect($('#content-div').css('width')).toBe('40px');
        });
    });

    describe('Statistics.shortString', function () {
        test('should return the input unchanged when it fits the limit', function () {
            expect(Statistics.shortString('hello', 10)).toBe('hello');
        });

        test('should append " [...]" when the input exceeds the limit, breaking on word boundaries', function () {
            const result = Statistics.shortString('one two three four five six seven', 15);
            expect(result.endsWith(' [...]')).toBe(true);
            // Truncation happens on whole words: the loop keeps appending words
            // until *adding the next one* would exceed the limit. With 15 chars
            // we expect "one two three" (= 14 chars incl. leading space).
            expect(result).toBe(' one two three [...]');
        });
    });

    describe('Statistics.getBarSize', function () {
        test('should compute a fraction of the bar width from a "completed/total" string', function () {
            expect(Statistics.getBarSize('50/100', 160)).toBe(80);
            expect(Statistics.getBarSize('1/4', 200)).toBe(50);
        });

        test('should return 0 when nothing is completed', function () {
            expect(Statistics.getBarSize('0/100', 160)).toBe(0);
        });
    });
});
