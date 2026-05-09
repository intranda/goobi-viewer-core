/**
 * Unit tests for viewerJS.iiif.
 *
 * The module's IIFE call site reads `jQuery` as a free variable; we stub it
 * (the tested functions never invoke jQuery). `viewerJS.isString` lives in
 * viewerJS.helper.js and is required by getId(); we stub it before require()
 * so the iiif tests do not need to load helper.js.
 */
global.jQuery = global.$ = function () {
    return {};
};

const viewerJS = require('../viewerJS.iiif.js');

// getId() calls viewerJS.isString — see viewerJS.helper.js. Provide a minimal
// stub so the iiif module can run standalone.
viewerJS.isString = function (v) {
    return typeof v === 'string' || v instanceof String;
};

const iiif = viewerJS.iiif;

describe('viewerJS.iiif.getValue', function () {
    test('should return the input unchanged when it is already a string', function () {
        expect(iiif.getValue('Hello', 'en')).toBe('Hello');
    });

    test('should return undefined for null/undefined input', function () {
        expect(iiif.getValue(undefined, 'en')).toBeUndefined();
        expect(iiif.getValue(null, 'en')).toBeUndefined();
    });

    test('should pick the matching @language from an array of language objects', function () {
        const element = [
            { '@value': 'Titel', '@language': 'de' },
            { '@value': 'Title', '@language': 'en' },
        ];
        expect(iiif.getValue(element, 'de')).toBe('Titel');
        expect(iiif.getValue(element, 'en')).toBe('Title');
    });

    test('should fall back to the configured fallbackLanguage when the requested locale is missing', function () {
        const element = [
            { '@value': 'Titel', '@language': 'de' },
            { '@value': 'Title', '@language': 'en' },
        ];
        // Explicit fallbackLanguage 'de' wins over default 'en'.
        expect(iiif.getValue(element, 'fr', 'de')).toBe('Titel');
    });

    test('should default fallbackLanguage to "en" when not given', function () {
        const element = [
            { '@value': 'Titel', '@language': 'de' },
            { '@value': 'Title', '@language': 'en' },
        ];
        expect(iiif.getValue(element, 'fr')).toBe('Title');
    });

    test('should return the first array element when it is a string', function () {
        expect(iiif.getValue(['One', 'Two'], 'en')).toBe('One');
    });

    test('should unwrap a single object via @value', function () {
        expect(iiif.getValue({ '@value': 'Solo' }, 'en')).toBe('Solo');
    });

    test('should join an IIIF v3 multi-language map by locale', function () {
        const element = { de: ['Titel'], en: ['Title', 'Subtitle'] };
        expect(iiif.getValue(element, 'en')).toBe('Title, Subtitle');
    });

    test('should fall back to the IIIF v3 fallback language when locale is absent', function () {
        const element = { de: ['Titel'], en: ['Title'] };
        expect(iiif.getValue(element, 'fr', 'en')).toBe('Title');
    });

    test('should fall back to the "none" key when neither locale nor fallback match', function () {
        const element = { none: ['Untitled'] };
        expect(iiif.getValue(element, 'en')).toBe('Untitled');
    });

    test('should fall back to "_default" when "none" is also absent', function () {
        const element = { _default: ['Default'] };
        expect(iiif.getValue(element, 'en')).toBe('Default');
    });

    test('should fall back to the first available key as last resort', function () {
        const element = { fr: ['Titre'] };
        expect(iiif.getValue(element, 'en')).toEqual(['Titre']);
    });
});

describe('viewerJS.iiif.isCollection', function () {
    // Note: the source file defines isCollection twice; the later definition
    // wins. These tests pin down the *effective* behaviour.

    test('should return true for IIIF v3 type "Collection" without multi-part viewingHint', function () {
        expect(iiif.isCollection({ type: 'Collection' })).toBe(true);
    });

    test('should return true for IIIF v2 @type "sc:Collection" without multi-part viewingHint', function () {
        expect(iiif.isCollection({ '@type': 'sc:Collection' })).toBe(true);
    });

    test('should return false for a Collection that is in fact an anchor record (multi-part)', function () {
        expect(iiif.isCollection({ type: 'Collection', viewingHint: 'multi-part' })).toBe(false);
    });

    test('should return false for a Manifest', function () {
        expect(iiif.isCollection({ type: 'Manifest' })).toBe(false);
    });
});

describe('viewerJS.iiif.isManifest', function () {
    test('should return true for IIIF v3 type "Manifest"', function () {
        expect(iiif.isManifest({ type: 'Manifest' })).toBe(true);
    });

    test('should return true for IIIF v2 @type "sc:Manifest"', function () {
        expect(iiif.isManifest({ '@type': 'sc:Manifest' })).toBe(true);
    });

    test('should treat a multi-part Collection as a manifest (anchor record)', function () {
        expect(iiif.isManifest({ type: 'Collection', viewingHint: 'multi-part' })).toBe(true);
        expect(iiif.isManifest({ '@type': 'sc:Collection', viewingHint: 'multi-part' })).toBe(true);
    });

    test('should return false for a non-multi-part Collection', function () {
        expect(iiif.isManifest({ type: 'Collection' })).toBe(false);
    });
});

describe('viewerJS.iiif.isSingleManifest', function () {
    test('should return true for type "Manifest"', function () {
        expect(iiif.isSingleManifest({ type: 'Manifest' })).toBe(true);
    });

    test('should return true for @type "sc:Manifest"', function () {
        expect(iiif.isSingleManifest({ '@type': 'sc:Manifest' })).toBe(true);
    });

    test('should return false for a multi-part anchor Collection (unlike isManifest)', function () {
        // isSingleManifest is stricter than isManifest: anchor records don't qualify.
        expect(iiif.isSingleManifest({ type: 'Collection', viewingHint: 'multi-part' })).toBe(false);
    });
});

describe('viewerJS.iiif.getId', function () {
    test('should return undefined for undefined input', function () {
        expect(iiif.getId(undefined)).toBeUndefined();
    });

    test('should return the input unchanged when it is already a string', function () {
        expect(iiif.getId('https://example.org/manifest/1')).toBe('https://example.org/manifest/1');
    });

    test('should prefer the v3 `id` property over the v2 `@id` property', function () {
        expect(iiif.getId({ id: 'v3', '@id': 'v2' })).toBe('v3');
    });

    test('should fall back to v2 `@id` when `id` is absent', function () {
        expect(iiif.getId({ '@id': 'v2' })).toBe('v2');
    });
});

describe('viewerJS.iiif.getBody', function () {
    test('should return body when present (IIIF v3 annotation)', function () {
        expect(iiif.getBody({ body: { value: 'x' } })).toEqual({ value: 'x' });
    });

    test('should fall back to resource (IIIF v2 annotation)', function () {
        expect(iiif.getBody({ resource: { chars: 'x' } })).toEqual({ chars: 'x' });
    });

    test('should return an empty object when neither is present', function () {
        expect(iiif.getBody({})).toEqual({});
    });
});

describe('viewerJS.iiif.getViewerPage', function () {
    test('should return undefined when there is no rendering', function () {
        expect(iiif.getViewerPage({})).toBeUndefined();
    });

    test('should return the single rendering object when its format is text/html', function () {
        const page = { format: 'text/html', '@id': 'p1' };
        expect(iiif.getViewerPage({ rendering: page })).toBe(page);
    });

    test('should return undefined when the single rendering is not text/html', function () {
        expect(iiif.getViewerPage({ rendering: { format: 'application/pdf' } })).toBeUndefined();
    });

    test('should pick the first text/html rendering from an array', function () {
        const html = { format: 'text/html', '@id': 'html1' };
        expect(
            iiif.getViewerPage({
                rendering: [{ format: 'application/pdf' }, html, { format: 'text/html', '@id': 'html2' }],
            })
        ).toBe(html);
    });

    test('should return undefined when no text/html rendering is in the array', function () {
        expect(iiif.getViewerPage({ rendering: [{ format: 'application/pdf' }] })).toBeUndefined();
    });
});

describe('viewerJS.iiif.getChildCollections / getContainedWorks', function () {
    // The implementation matches a context ending in '/collection/extent/context.json' (singular).
    const ctx = 'http://example.org/api/collection/extent/context.json';

    test('should return 0 when the collection has no service', function () {
        expect(iiif.getChildCollections({})).toBe(0);
        expect(iiif.getContainedWorks({})).toBe(0);
    });

    test('should pick numbers from the matching service in a service array', function () {
        const collection = {
            service: [
                { '@context': 'http://example.org/some/other.json', children: 99, containedWorks: 99 },
                { '@context': ctx, children: 7, containedWorks: 42 },
            ],
        };
        expect(iiif.getChildCollections(collection)).toBe(7);
        expect(iiif.getContainedWorks(collection)).toBe(42);
    });

    test('should pick numbers from a single (non-array) extent service object', function () {
        const collection = { service: { '@context': ctx, children: 3, containedWorks: 11 } };
        expect(iiif.getChildCollections(collection)).toBe(3);
        expect(iiif.getContainedWorks(collection)).toBe(11);
    });
});
