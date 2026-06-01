/**
 * Unit tests for viewerJS.expandableToc.
 *
 * The module is pure-DOM (no jQuery use inside) — it operates on the
 * `#expandableToc` container via classList. jsdom is wired up by
 * jest-setup-browser.js.
 */
const viewerJS = require('../viewerJS.expandableToc.js');

const HIDDEN = 'toc__list-expandable-elem--hidden';
const EXPANDED = 'toc__list-expandable-elem--expanded';

/**
 * Build a 3-level TOC fixture:
 *   [0] root1 (parent, expanded)
 *     [1] childA (parent)
 *       [2] grandchild1
 *     [1] childB
 *   [0] root2
 */
function buildToc() {
    // The expand-all / collapse-all buttons must live INSIDE #expandableToc;
    // the click handler is delegated on the container, not on document.
    document.body.innerHTML =
        '<ul id="expandableToc">' +
        '  <li><button data-action="expand-all">Expand all</button></li>' +
        '  <li><button data-action="collapse-all">Collapse all</button></li>' +
        '  <li class="toc__list-expandable-elem parent ' +
        EXPANDED +
        '" data-level="0" id="root1">' +
        '    <button class="toc__list-expandable-toggle" aria-expanded="true">root1</button>' +
        '  </li>' +
        '  <li class="toc__list-expandable-elem parent" data-level="1" id="childA">' +
        '    <button class="toc__list-expandable-toggle" aria-expanded="false">childA</button>' +
        '  </li>' +
        '  <li class="toc__list-expandable-elem ' +
        HIDDEN +
        '" data-level="2" id="grandchild1">grandchild1</li>' +
        '  <li class="toc__list-expandable-elem" data-level="1" id="childB">childB</li>' +
        '  <li class="toc__list-expandable-elem" data-level="0" id="root2">root2</li>' +
        '</ul>';
}

function $id(id) {
    return document.getElementById(id);
}

describe('viewerJS.expandableToc.init', function () {
    beforeEach(function () {
        buildToc();
        viewerJS.expandableToc.init();
    });

    afterEach(function () {
        document.body.innerHTML = '';
    });

    test('should be a no-op when no #expandableToc container exists', function () {
        document.body.innerHTML = '<div>nothing</div>';
        // Should not throw.
        expect(function () {
            viewerJS.expandableToc.init();
        }).not.toThrow();
    });

    describe('toggle (clicking a single expand/collapse button)', function () {
        test('should expand a collapsed parent: reveals its direct children and sets aria-expanded', function () {
            const childA = $id('childA');
            // Sanity: childA starts collapsed.
            expect(childA.classList.contains(EXPANDED)).toBe(false);

            const toggle = childA.querySelector('.toc__list-expandable-toggle');
            toggle.click();

            expect(childA.classList.contains(EXPANDED)).toBe(true);
            expect(toggle.getAttribute('aria-expanded')).toBe('true');
            // grandchild1 (level=2, child of childA) is no longer hidden.
            expect($id('grandchild1').classList.contains(HIDDEN)).toBe(false);
        });

        test('should collapse an expanded parent and hide all descendants', function () {
            const root1 = $id('root1');
            const toggle = root1.querySelector('.toc__list-expandable-toggle');
            // root1 is initially expanded.
            expect(root1.classList.contains(EXPANDED)).toBe(true);

            toggle.click();

            expect(root1.classList.contains(EXPANDED)).toBe(false);
            expect(toggle.getAttribute('aria-expanded')).toBe('false');
            // All descendants of root1 (childA, grandchild1, childB) are hidden.
            expect($id('childA').classList.contains(HIDDEN)).toBe(true);
            expect($id('grandchild1').classList.contains(HIDDEN)).toBe(true);
            expect($id('childB').classList.contains(HIDDEN)).toBe(true);
            // root2 is a sibling, not a descendant — stays visible.
            expect($id('root2').classList.contains(HIDDEN)).toBe(false);
        });
    });

    describe('expand-all action', function () {
        test('should mark every parent as expanded and remove every hidden flag', function () {
            // Sanity: childA is initially collapsed; grandchild1 is hidden.
            expect($id('childA').classList.contains(EXPANDED)).toBe(false);
            expect($id('grandchild1').classList.contains(HIDDEN)).toBe(true);

            document.querySelector('[data-action="expand-all"]').click();

            // All parents marked expanded.
            expect($id('root1').classList.contains(EXPANDED)).toBe(true);
            expect($id('childA').classList.contains(EXPANDED)).toBe(true);
            // No element is hidden anymore.
            expect($id('grandchild1').classList.contains(HIDDEN)).toBe(false);
            // aria attribute reflects expanded state.
            expect($id('childA').querySelector('.toc__list-expandable-toggle').getAttribute('aria-expanded')).toBe('true');
        });
    });

    describe('collapse-all action', function () {
        test('should hide every non-root element and remove the expanded state from all parents', function () {
            document.querySelector('[data-action="collapse-all"]').click();

            // root1 (parent) is no longer expanded.
            expect($id('root1').classList.contains(EXPANDED)).toBe(false);
            expect($id('root1').querySelector('.toc__list-expandable-toggle').getAttribute('aria-expanded')).toBe('false');
            // Non-root items are hidden.
            expect($id('childA').classList.contains(HIDDEN)).toBe(true);
            expect($id('grandchild1').classList.contains(HIDDEN)).toBe(true);
            expect($id('childB').classList.contains(HIDDEN)).toBe(true);
            // Root items are NOT hidden by collapse-all (level=0).
            expect($id('root1').classList.contains(HIDDEN)).toBe(false);
            expect($id('root2').classList.contains(HIDDEN)).toBe(false);
        });
    });
});
