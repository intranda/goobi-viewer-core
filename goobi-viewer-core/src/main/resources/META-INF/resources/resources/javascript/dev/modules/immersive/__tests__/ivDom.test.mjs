/** Unit tests for the shared DOM helpers. */
import { offsetTopWithin, scrollTopToReveal, styleClassTokens, el } from '../ivDom.mjs';

describe('offsetTopWithin', () => {
    test('sums offsetTop along the offsetParent chain up to the container', () => {
        const container = {};
        const parent = { offsetTop: 100, offsetParent: container };
        const el = { offsetTop: 10, offsetParent: parent };
        expect(offsetTopWithin(el, container)).toBe(110);
    });
    test('returns the element offset when it is a direct child', () => {
        const container = {};
        expect(offsetTopWithin({ offsetTop: 42, offsetParent: container }, container)).toBe(42);
    });
});

describe('scrollTopToReveal', () => {
    test('returns null when the line is already fully visible', () => {
        expect(scrollTopToReveal({ offsetTop: 100, height: 20, scrollTop: 50, clientHeight: 200, scrollHeight: 1000 })).toBeNull();
    });
    test('centers a line below the viewport', () => {
        expect(scrollTopToReveal({ offsetTop: 600, height: 20, scrollTop: 0, clientHeight: 200, scrollHeight: 1000 })).toBe(510);
    });
    test('centers a line above the viewport', () => {
        expect(scrollTopToReveal({ offsetTop: 100, height: 20, scrollTop: 400, clientHeight: 200, scrollHeight: 1000 })).toBe(10);
    });
    test('clamps to 0 (never negative)', () => {
        expect(scrollTopToReveal({ offsetTop: 5, height: 20, scrollTop: 300, clientHeight: 200, scrollHeight: 1000 })).toBe(0);
    });
    test('clamps to max scroll (scrollHeight - clientHeight)', () => {
        expect(scrollTopToReveal({ offsetTop: 990, height: 20, scrollTop: 0, clientHeight: 200, scrollHeight: 1000 })).toBe(800);
    });
});

describe('styleClassTokens', () => {
    test('splits a configured styleClass into class tokens', () => {
        expect(styleClassTokens('alto-annotation--jacob-grimm')).toEqual(['alto-annotation--jacob-grimm']);
        expect(styleClassTokens('kunde--hand-a  extra')).toEqual(['kunde--hand-a', 'extra']);
    });

    test('blank or missing values yield an empty list', () => {
        expect(styleClassTokens('')).toEqual([]);
        expect(styleClassTokens('   ')).toEqual([]);
        expect(styleClassTokens(undefined)).toEqual([]);
    });
});

describe('el', () => {
    test('builds an element with optional class and text', () => {
        const node = el('span', 'a b', 'x');
        expect(node.tagName).toBe('SPAN');
        expect(node.className).toBe('a b');
        expect(node.textContent).toBe('x');
        expect(el('div').className).toBe('');
        expect(el('div', null, 0).textContent).toBe('0');
    });
});
