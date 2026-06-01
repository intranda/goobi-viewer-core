/**
 * Unit tests for viewerJS.widgetToc.
 *
 * The module wires a single delegated click listener on #widgetToc
 * and exposes setActive(iddoc). All other helpers (_expandAll /
 * _collapseAll / _ensureVisible / etc.) are private; we exercise
 * them through clicks and setActive() with carefully sized fixtures.
 */
const viewerJS = require('../viewerJS.widgetToc.js');

/** Build a fixture with N levels of nesting and an optional active element. */
function buildToc({ activeIddoc } = {}) {
    document.body.innerHTML = `
        <div id="widgetToc">
            <div class="widget-toc__elements">
                <button data-action="expand-all">Expand all</button>
                <button data-action="collapse-all">Collapse all</button>
                <li class="widget-toc__element parent widget-toc__element--expanded"
                    data-level="0" data-iddoc="iddoc_root">
                    <button class="widget-toc__toggle" aria-expanded="true"></button>
                    <a class="widget-toc__element-link">Root</a>
                </li>
                <li class="widget-toc__element parent" data-level="1" data-iddoc="iddoc_chapter">
                    <button class="widget-toc__toggle" aria-expanded="false"></button>
                    <a class="widget-toc__element-link">Chapter</a>
                </li>
                <li class="widget-toc__element widget-toc__element--hidden"
                    data-level="2" data-iddoc="iddoc_section">
                    <a class="widget-toc__element-link">Section</a>
                </li>
            </div>
        </div>`;
    if (activeIddoc) {
        const li = document.querySelector(`[data-iddoc="iddoc_${activeIddoc}"]`);
        if (li) li.classList.add('active');
    }
}

describe('init: missing container is a safe no-op', () => {
    test('does nothing when #widgetToc is not in the DOM', () => {
        document.body.innerHTML = '';
        expect(() => viewerJS.widgetToc.init()).not.toThrow();
    });
});

describe('toggle clicks', () => {
    beforeEach(() => {
        buildToc();
        viewerJS.widgetToc.init();
    });

    test('clicking a collapsed toggle expands the parent and reveals its direct children', () => {
        const chapterLi = document.querySelector('[data-iddoc="iddoc_chapter"]');
        const chapterToggle = chapterLi.querySelector('.widget-toc__toggle');

        chapterToggle.click();

        expect(chapterLi.classList.contains('widget-toc__element--expanded')).toBe(true);
        expect(chapterToggle.getAttribute('aria-expanded')).toBe('true');
        // The level-2 child becomes visible.
        const sectionLi = document.querySelector('[data-iddoc="iddoc_section"]');
        expect(sectionLi.classList.contains('widget-toc__element--hidden')).toBe(false);
    });

    test('clicking an expanded toggle collapses the parent and hides its descendants', () => {
        // Pre-state: root is expanded; chapter is below it at level 1.
        const rootLi = document.querySelector('[data-iddoc="iddoc_root"]');
        const rootToggle = rootLi.querySelector('.widget-toc__toggle');

        rootToggle.click();

        expect(rootLi.classList.contains('widget-toc__element--expanded')).toBe(false);
        expect(rootToggle.getAttribute('aria-expanded')).toBe('false');
        // The chapter (level 1, descendant of root) is now hidden.
        const chapterLi = document.querySelector('[data-iddoc="iddoc_chapter"]');
        expect(chapterLi.classList.contains('widget-toc__element--hidden')).toBe(true);
    });
});

describe('expand-all / collapse-all', () => {
    beforeEach(() => {
        buildToc();
        viewerJS.widgetToc.init();
    });

    test('clicking [data-action="expand-all"] reveals every element and expands every parent', () => {
        document.querySelector('[data-action="expand-all"]').click();
        const sectionLi = document.querySelector('[data-iddoc="iddoc_section"]');
        const chapterLi = document.querySelector('[data-iddoc="iddoc_chapter"]');
        expect(sectionLi.classList.contains('widget-toc__element--hidden')).toBe(false);
        expect(chapterLi.classList.contains('widget-toc__element--expanded')).toBe(true);
    });

    test('clicking [data-action="collapse-all"] hides non-root elements and collapses parents', () => {
        document.querySelector('[data-action="collapse-all"]').click();
        const sectionLi = document.querySelector('[data-iddoc="iddoc_section"]');
        const chapterLi = document.querySelector('[data-iddoc="iddoc_chapter"]');
        expect(sectionLi.classList.contains('widget-toc__element--hidden')).toBe(true);
        expect(chapterLi.classList.contains('widget-toc__element--expanded')).toBe(false);
        // The level-0 root remains visible (its level is 0, not > 0).
        expect(document.querySelector('[data-iddoc="iddoc_root"]').classList.contains('widget-toc__element--hidden')).toBe(false);
    });
});

describe('setActive', () => {
    test('moves the active class to the matching iddoc and removes it from previous holders', () => {
        buildToc({ activeIddoc: 'root' });
        viewerJS.widgetToc.init();

        viewerJS.widgetToc.setActive('chapter');

        const rootLi = document.querySelector('[data-iddoc="iddoc_root"]');
        const chapterLi = document.querySelector('[data-iddoc="iddoc_chapter"]');
        expect(rootLi.classList.contains('active')).toBe(false);
        expect(chapterLi.classList.contains('active')).toBe(true);
        // The element-link is also flagged active so the styling can target it.
        expect(chapterLi.querySelector('.widget-toc__element-link').classList.contains('active')).toBe(true);
    });

    test('expanding the parent chain happens automatically when the new active is hidden', () => {
        buildToc();
        // Make section currently hidden — that is the default.
        viewerJS.widgetToc.init();

        viewerJS.widgetToc.setActive('section');

        const sectionLi = document.querySelector('[data-iddoc="iddoc_section"]');
        const chapterLi = document.querySelector('[data-iddoc="iddoc_chapter"]');
        // Section is no longer hidden, and its level-1 ancestor was expanded
        // to bring it into view.
        expect(sectionLi.classList.contains('widget-toc__element--hidden')).toBe(false);
        expect(chapterLi.classList.contains('widget-toc__element--expanded')).toBe(true);
    });

    test('is a safe no-op when the iddoc has no matching element', () => {
        buildToc();
        viewerJS.widgetToc.init();
        expect(() => viewerJS.widgetToc.setActive('nonexistent')).not.toThrow();
    });

    test('does nothing when #widgetToc is missing', () => {
        document.body.innerHTML = '';
        expect(() => viewerJS.widgetToc.setActive('root')).not.toThrow();
    });
});
