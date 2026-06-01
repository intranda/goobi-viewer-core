/**
 * Unit tests for viewerJS.fulltextActions.
 *
 * init() does two things:
 *   1. disables every [data-entity-type] that has no
 *      [data-entity-authority-data-uri] attribute,
 *   2. for each entity that DOES have an authority-data-uri, inserts a
 *      hidden popover container, a placeholder span, and binds a
 *      Bootstrap popover.
 *
 * We stub `$.fn.popover` (the Bootstrap plugin) so the test does not
 * depend on Bootstrap being loaded, and assert the side effects on the
 * DOM instead.
 *
 * NOTE: the source mutates a module-level `_defaults` via $.extend
 * so we use jest.isolateModules per test to keep cases independent.
 */

function freshLoad(config, htmlBody) {
    let viewerJS;
    jest.isolateModules(() => {
        viewerJS = require('../viewerJS.fulltextActions.js');
    });
    document.body.innerHTML = htmlBody;
    // Stub the bootstrap popover plugin: the implementation only cares
    // that the call did not throw and that .on('shown.bs.popover', ...)
    // can attach later. Returning `this` keeps jQuery chaining alive.
    $.fn.popover = jest.fn(function () {
        return this;
    });
    viewerJS.fulltextActions.init(config);
    return viewerJS;
}

const stdConfig = {
    msg: {
        fulltextPopoverActionsTermMsg: 'Heading',
        fulltextPopoverAuthorityDataMsg: 'Authority',
        fulltextPopoverTriggerSearchMsg: 'Search',
    },
    normdataConfig: { path: '/viewer' },
};

describe('init: disable buttons without authority-data-uri', () => {
    test('every [data-entity-type] without [data-entity-authority-data-uri] gets disabled', () => {
        freshLoad(
            stdConfig,
            `
            <button data-entity-type="person" id="b1">No URI</button>
            <button data-entity-type="place" id="b2">Also no URI</button>`
        );
        expect(document.getElementById('b1').disabled).toBe(true);
        expect(document.getElementById('b2').disabled).toBe(true);
    });

    test('entities WITH an authority-data-uri remain enabled', () => {
        freshLoad(
            stdConfig,
            `
            <button data-entity-type="person"
                    data-entity-authority-data-uri="https://gnd/1"
                    data-entity-authority-data-search="/search?q=1"
                    id="b1">Has URI</button>`
        );
        expect(document.getElementById('b1').disabled).toBe(false);
    });
});

describe('init: popover wiring', () => {
    test('inserts a hidden .entity-popover-element after each authority-bearing entity', () => {
        freshLoad(
            stdConfig,
            `
            <button data-entity-type="person"
                    data-entity-authority-data-uri="https://gnd/1"
                    data-entity-authority-data-search="/search?q=1"
                    id="b1">X</button>`
        );
        const popover = document.querySelector('.entity-popover-element');
        expect(popover).not.toBeNull();
        expect(popover.classList.contains('hidden')).toBe(true);
        // Heading text is taken from the msg config.
        expect(popover.querySelector('.popover-heading').textContent).toContain('Heading');
        // The data-remotecontent attribute carries the authority URI.
        expect(popover.querySelector('button[data-remotecontent]').getAttribute('data-remotecontent')).toBe('https://gnd/1');
    });

    test('inserts a [data-placeholder="forPopover"] span next to the entity', () => {
        freshLoad(
            stdConfig,
            `
            <button data-entity-type="person"
                    data-entity-authority-data-uri="https://gnd/1"
                    data-entity-authority-data-search="/search?q=1"
                    id="b1">X</button>`
        );
        expect(document.querySelector('[data-placeholder="forPopover"]')).not.toBeNull();
    });

    test('calls the Bootstrap popover plugin once per authority-bearing entity', () => {
        freshLoad(
            stdConfig,
            `
            <button data-entity-type="person"
                    data-entity-authority-data-uri="https://gnd/1"
                    data-entity-authority-data-search="/s1" id="b1">X</button>
            <button data-entity-type="place"
                    data-entity-authority-data-uri="https://gnd/2"
                    data-entity-authority-data-search="/s2" id="b2">Y</button>`
        );
        // Plugin was called twice (once per entity)…
        const initCalls = $.fn.popover.mock.calls.filter((c) => typeof c[0] !== 'string');
        expect(initCalls).toHaveLength(2);
        // …and not as a string command (no 'show'/'hide' invocations during init).
        const cmdCalls = $.fn.popover.mock.calls.filter((c) => typeof c[0] === 'string');
        expect(cmdCalls).toHaveLength(0);
    });

    test('appends a trailing slash to normdataConfig.path when one is missing', () => {
        // The icon URL inside the popover template should contain
        // `<path>/resources/icons/...`. We verify the slash is present.
        freshLoad(
            stdConfig,
            `
            <button data-entity-type="person"
                    data-entity-authority-data-uri="https://gnd/1"
                    data-entity-authority-data-search="/s1" id="b1">X</button>`
        );
        const useEl = document.querySelector('.entity-popover-element use');
        expect(useEl.getAttribute('href')).toMatch(/^\/viewer\/resources\/icons\//);
    });
});
