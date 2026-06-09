/**
 * Unit tests for viewerJS.jsonValidator.
 *
 * jQuery is wired up by jest-setup-browser.js.
 */
const viewerJS = require('../viewerJS.JsonValidator.js');
const validator = viewerJS.jsonValidator;

describe('viewerJS.jsonValidator.validate', function () {
    test('should return true for an object that matches the signature exactly', function () {
        const obj = { id: 1, name: 'A', active: true };
        const signature = { id: 'number', name: 'string', active: 'boolean' };
        expect(validator.validate(obj, signature)).toBe(true);
    });

    test('should return false when a property has the wrong type', function () {
        const obj = { id: '1', name: 'A' }; // id is string, not number
        const signature = { id: 'number', name: 'string' };
        expect(validator.validate(obj, signature)).toBe(false);
    });

    test('should accept null property values regardless of declared type', function () {
        // The validator skips null/undefined - this matches the implementation.
        const obj = { id: null, name: 'A' };
        const signature = { id: 'number', name: 'string' };
        expect(validator.validate(obj, signature)).toBe(true);
    });

    test('should accept undefined property values regardless of declared type', function () {
        const obj = { name: 'A' }; // id missing
        const signature = { id: 'number', name: 'string' };
        expect(validator.validate(obj, signature)).toBe(true);
    });

    test('should use embedded jsonSignature when no explicit signature is given', function () {
        const obj = {
            jsonSignature: { id: 'number' },
            id: 42,
        };
        expect(validator.validate(obj)).toBe(true);
    });

    test('should throw when first argument is not an object', function () {
        expect(function () {
            validator.validate('notAnObject', { id: 'number' });
        }).toThrow('Both parameters must be objects.');
    });

    test('should throw when first argument is null', function () {
        expect(function () {
            validator.validate(null, { id: 'number' });
        }).toThrow();
    });

    test('should throw when no signature is provided and object has no jsonSignature', function () {
        expect(function () {
            validator.validate({ id: 1 });
        }).toThrow();
    });

    test('should reject extra object properties not declared in the signature', function () {
        const obj = { id: 1, extra: 'whatever' };
        const signature = { id: 'number' };
        expect(validator.validate(obj, signature)).toBe(false);
    });

    test('should ignore the jsonSignature property when present in the object (allowlisted)', function () {
        const obj = { id: 1, jsonSignature: { id: 'number' } };
        const signature = { id: 'number' };
        expect(validator.validate(obj, signature)).toBe(true);
    });
});
