/**
 * Unit tests for Crowdsourcing.Annotation.Plaintext.
 *
 * Ported from the deleted Jasmine spec
 * `tests/spec/crowdsourcing/Crowdsourcing.Annotation.Plaintext-spec.js`.
 *
 * jQuery + jsdom are wired up by jest-setup-browser.js. The Crowdsourcing
 * IIFE pattern needs special loading via indirect eval — see crowdsourcing-loader.js
 * for the rationale.
 */
const Crowdsourcing = require('./crowdsourcing-loader')(['Crowdsourcing.Annotation.Plaintext.js']);

// Fixtures from the original spec, unchanged.
const simpleAnnotation = {
    '@context': 'http://www.w3.org/ns/anno.jsonld',
    id: 'http://example.org/anno5',
    type: 'Annotation',
    body: {
        type: 'TextualBody',
        value: "<p>j'adore !</p>",
        format: 'text/html',
        language: 'fr',
    },
    target: 'http://example.org/photo1',
};

const fragmentAnnotation = {
    '@context': 'http://www.w3.org/ns/anno.jsonld',
    id: 'http://example.org/anno5',
    type: 'Annotation',
    body: {
        type: 'TextualBody',
        value: "j'adore !",
        format: 'text/plain',
        language: 'fr',
    },
    target: {
        source: 'http://example.org/image1',
        selector: {
            type: 'FragmentSelector',
            conformsTo: 'http://www.w3.org/TR/media-frags/',
            value: 'xywh=60,70,100,200',
        },
    },
};

describe('Crowdsourcing.Annotation.Plaintext', function () {
    test('should expose the Plaintext constructor on the Crowdsourcing namespace', function () {
        expect(Crowdsourcing.Annotation.Plaintext).toBeDefined();
        expect(typeof Crowdsourcing.Annotation.Plaintext).toBe('function');
    });

    describe('constructor', function () {
        test('should produce an annotation whose getText() returns the body value of the input', function () {
            const anno = new Crowdsourcing.Annotation.Plaintext(simpleAnnotation);
            expect(anno).toBeDefined();
            expect(anno.getText()).toBe(simpleAnnotation.body.value);
        });

        test('should deep-copy the body so two instances from the same source are independent', function () {
            const anno1 = new Crowdsourcing.Annotation.Plaintext(simpleAnnotation);
            const anno2 = new Crowdsourcing.Annotation.Plaintext(simpleAnnotation);
            anno1.setText('Text 1');
            anno2.setText('Text 2');
            // The shared fixture must not have been mutated.
            expect(simpleAnnotation.body.value).toBe("<p>j'adore !</p>");
            expect(anno1.getText()).toBe('Text 1');
            expect(anno2.getText()).toBe('Text 2');
        });

        test('should support copy-construction from another Plaintext annotation (deep copy of body and target)', function () {
            const anno = new Crowdsourcing.Annotation.Plaintext(simpleAnnotation);
            const copy = new Crowdsourcing.Annotation.Plaintext(anno);
            expect(copy.target).toEqual(anno.target);
            expect(copy.body).toEqual(anno.body);
            copy.setText('TEST');
            // Original is unchanged after the copy mutates.
            expect(anno.getText()).toBe("<p>j'adore !</p>");
            expect(copy.getText()).toBe('TEST');
        });
    });

    describe('Fragment target (inherited from Crowdsourcing.Annotation)', function () {
        test('should preserve the FragmentSelector value carried in the input annotation', function () {
            const anno = new Crowdsourcing.Annotation.Plaintext(fragmentAnnotation);
            expect(anno.target.selector.value).toBe('xywh=60,70,100,200');
        });

        test('should parse the xywh selector into an x/y/width/height rectangle via getRegion()', function () {
            const anno = new Crowdsourcing.Annotation.Plaintext(fragmentAnnotation);
            const r = anno.getRegion();
            expect(r.x).toBe(60);
            expect(r.y).toBe(70);
            expect(r.width).toBe(100);
            expect(r.height).toBe(200);
        });

        test('should let setRegion() replace the existing fragment with a new rectangle', function () {
            const anno = new Crowdsourcing.Annotation.Plaintext(fragmentAnnotation);
            anno.setRegion({ x: 1000, y: 2000, width: 50, height: 20 });
            const r = anno.getRegion();
            expect(r.x).toBe(1000);
            expect(r.y).toBe(2000);
            expect(r.width).toBe(50);
            expect(r.height).toBe(20);
        });

        test('should let setRegion() create a fragment from scratch on an annotation with a plain target', function () {
            const anno = new Crowdsourcing.Annotation.Plaintext(simpleAnnotation);
            // The plain target has no FragmentSelector yet — getRegion() returns undefined.
            expect(anno.getRegion()).toBeUndefined();
            anno.setRegion({ x: 1000, y: 2000, width: 50, height: 20 });
            const r = anno.getRegion();
            expect(r.x).toBe(1000);
            expect(r.y).toBe(2000);
            expect(r.width).toBe(50);
            expect(r.height).toBe(20);
        });
    });
});
