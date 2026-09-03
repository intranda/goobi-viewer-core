/** Unit tests for the platform and typing-context predicates. */
import { isTypingTarget, isMacPlatform, macKeyLabel } from '../shared/ivPlatform.mjs';

describe('isTypingTarget', function () {
    test('true for form fields (input, textarea, select)', function () {
        ['input', 'textarea', 'select'].forEach(function (tag) {
            expect(isTypingTarget(document.createElement(tag))).toBe(true);
        });
    });

    test('true for contenteditable elements and their descendants', function () {
        const editor = document.createElement('div');
        editor.setAttribute('contenteditable', 'true');
        const child = document.createElement('span');
        editor.appendChild(child);
        expect(isTypingTarget(editor)).toBe(true);
        expect(isTypingTarget(child)).toBe(true);
    });

    test('true for an empty contenteditable attribute', function () {
        const editor = document.createElement('div');
        editor.setAttribute('contenteditable', '');
        expect(isTypingTarget(editor)).toBe(true);
    });

    test('false for contenteditable="false"', function () {
        const el = document.createElement('div');
        el.setAttribute('contenteditable', 'false');
        expect(isTypingTarget(el)).toBe(false);
    });

    test('false for plain elements and non-element targets', function () {
        expect(isTypingTarget(document.createElement('button'))).toBe(false);
        expect(isTypingTarget(document.body)).toBe(false);
        expect(isTypingTarget(document)).toBe(false);
        expect(isTypingTarget(null)).toBe(false);
    });
});

describe('isMacPlatform', function () {
    test('true for Apple platforms (macOS, iPadOS/iOS hardware keyboards)', function () {
        expect(isMacPlatform('MacIntel')).toBe(true);
        expect(isMacPlatform('macOS')).toBe(true);
        expect(isMacPlatform('iPad')).toBe(true);
        expect(isMacPlatform('iPhone')).toBe(true);
    });

    test('false for Windows, Linux and unknown values', function () {
        expect(isMacPlatform('Win32')).toBe(false);
        expect(isMacPlatform('Windows')).toBe(false);
        expect(isMacPlatform('Linux x86_64')).toBe(false);
        expect(isMacPlatform('')).toBe(false);
        expect(isMacPlatform(undefined)).toBe(false);
    });
});

describe('macKeyLabel', function () {
    test('maps modifier names to Mac symbol plus key name (a bare ⇧ reads like an arrow) and accessible name', function () {
        expect(macKeyLabel('alt')).toEqual({ symbol: '⌥', name: 'Option', label: 'Option' });
        expect(macKeyLabel('shift')).toEqual({ symbol: '⇧', name: 'Shift', label: 'Shift' });
    });

    test('returns null for keys without a Mac-specific label', function () {
        expect(macKeyLabel('r')).toBeNull();
        expect(macKeyLabel('')).toBeNull();
        expect(macKeyLabel(undefined)).toBeNull();
    });
});
