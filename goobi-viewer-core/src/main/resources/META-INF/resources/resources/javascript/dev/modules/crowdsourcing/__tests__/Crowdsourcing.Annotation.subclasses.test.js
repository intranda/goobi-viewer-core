/**
 * Unit tests for the four small Crowdsourcing.Annotation.* subclasses.
 *
 * They share the deep-copy / setRegion / getRegion / setCreated /
 * setModified machinery from the base class — those are exercised in
 * Crowdsourcing.test.js. Here we focus on each subclass's body shape
 * and its own getter/setter pair (or pairs).
 *
 * Plaintext lives in its own file (Crowdsourcing.Annotation.Plaintext.test.js).
 */
const Crowdsourcing = require('./crowdsourcing-loader')([
    'Crowdsourcing.Annotation.AuthorityResource.js',
    'Crowdsourcing.Annotation.Metadata.js',
    'Crowdsourcing.Annotation.Richtext.js',
    'Crowdsourcing.AnnotationGeoJson.js',
]);

describe('Crowdsourcing.Annotation.AuthorityResource', function () {
    test('should expose the constructor on the namespace', function () {
        expect(typeof Crowdsourcing.Annotation.AuthorityResource).toBe('function');
    });

    test('should default to an AuthorityResource body with the supplied @context', function () {
        const a = new Crowdsourcing.Annotation.AuthorityResource({ id: 'a' }, 'https://example.org/contexts/auth.json');
        expect(a.body.type).toBe('AuthorityResource');
        expect(a.body['@context']).toBe('https://example.org/contexts/auth.json');
    });

    test('should round-trip the authority id via setId/getId', function () {
        const a = new Crowdsourcing.Annotation.AuthorityResource({ id: 'a' }, 'ctx');
        expect(a.getId()).toBeUndefined();
        a.setId('https://gnd.example/123');
        expect(a.getId()).toBe('https://gnd.example/123');
    });

    test('should mark itself empty until an id is set', function () {
        const a = new Crowdsourcing.Annotation.AuthorityResource({ id: 'a' }, 'ctx');
        expect(a.isEmpty()).toBe(true);
        a.setId('https://gnd.example/123');
        expect(a.isEmpty()).toBe(false);
    });

    test('should bump `modified` when setId changes an existing id (but not on first set)', function () {
        const a = new Crowdsourcing.Annotation.AuthorityResource({ id: 'a' }, 'ctx');
        a.setId('https://x/1');
        expect(a.modified).toBeUndefined();
        a.setId('https://x/2');
        expect(a.modified).toBeDefined();
    });
});

describe('Crowdsourcing.Annotation.Metadata', function () {
    test('should default to a goobi-viewer-index Dataset body and merge the provided original data', function () {
        const m = new Crowdsourcing.Annotation.Metadata({ id: 'm' }, { title: 'T', author: ['A'] });
        expect(m.body.type).toBe('Dataset');
        expect(m.body.format).toBe('goobi-viewer-index');
        expect(m.body.data.title).toBe('T');
        expect(m.body.data.author).toEqual(['A']);
    });

    test('getValue() should join array values with "; "', function () {
        const m = new Crowdsourcing.Annotation.Metadata({ id: 'm' }, { authors: ['A', 'B', 'C'] });
        expect(m.getValue('authors')).toBe('A; B; C');
    });

    test('getValue() should return scalar values unchanged', function () {
        const m = new Crowdsourcing.Annotation.Metadata({ id: 'm' }, { title: 'T' });
        expect(m.getValue('title')).toBe('T');
    });

    test('getValue() should return empty string for unknown fields', function () {
        const m = new Crowdsourcing.Annotation.Metadata({ id: 'm' }, {});
        expect(m.getValue('missing')).toBe('');
    });

    test('setValue() should always store the value as a one-element array', function () {
        const m = new Crowdsourcing.Annotation.Metadata({ id: 'm' }, {});
        m.setValue('title', 'T');
        expect(m.body.data.title).toEqual(['T']);
    });

    test('getFields() should reflect the keys in body.data', function () {
        const m = new Crowdsourcing.Annotation.Metadata({ id: 'm' }, { a: 1, b: 2 });
        expect(m.getFields().sort()).toEqual(['a', 'b']);
    });

    test('isEmpty() should be true exactly when body.data has no fields', function () {
        const empty = new Crowdsourcing.Annotation.Metadata({ id: 'm' }, {});
        expect(empty.isEmpty()).toBe(true);
        const filled = new Crowdsourcing.Annotation.Metadata({ id: 'm' }, { a: 'x' });
        expect(filled.isEmpty()).toBe(false);
    });
});

describe('Crowdsourcing.Annotation.Richtext', function () {
    test('should default to a TextualBody with format text/html', function () {
        const r = new Crowdsourcing.Annotation.Richtext({ id: 'r' });
        expect(r.body.type).toBe('TextualBody');
        expect(r.body.format).toBe('text/html');
        expect(r.body.value).toBe('');
    });

    test('should round-trip the rich text via setText/getText', function () {
        const r = new Crowdsourcing.Annotation.Richtext({ id: 'r' });
        r.setText('<p>Hello <strong>world</strong></p>');
        expect(r.getText()).toBe('<p>Hello <strong>world</strong></p>');
    });

    test('isEmpty() should be true exactly when getText() is empty', function () {
        const r = new Crowdsourcing.Annotation.Richtext({ id: 'r' });
        expect(r.isEmpty()).toBe(true);
        r.setText('<p>x</p>');
        expect(r.isEmpty()).toBe(false);
    });
});

describe('Crowdsourcing.Annotation.GeoJson', function () {
    test('should expose the constructor on the namespace', function () {
        expect(typeof Crowdsourcing.Annotation.GeoJson).toBe('function');
    });

    test('getLocation() should return the body unchanged', function () {
        const body = { geometry: { type: 'Point', coordinates: [9.9, 51.5] } };
        const g = new Crowdsourcing.Annotation.GeoJson({ id: 'g', body: body });
        expect(g.getLocation()).toEqual(body);
    });

    test('setBody() should replace the entire body', function () {
        const g = new Crowdsourcing.Annotation.GeoJson({ id: 'g' });
        g.setBody({ geometry: { type: 'Point', coordinates: [1, 2] } });
        expect(g.body.geometry.coordinates).toEqual([1, 2]);
    });

    test('setGeometry() should attach geometry to a body that has none yet', function () {
        const g = new Crowdsourcing.Annotation.GeoJson({ id: 'g' });
        // Constructor with empty annotation → body is undefined.
        expect(g.body).toBeUndefined();
        g.setGeometry({ type: 'Point', coordinates: [9.9, 51.5] });
        expect(g.body.geometry.coordinates).toEqual([9.9, 51.5]);
    });

    test('setGeometry() should preserve other body fields (e.g. view) when set on an existing body', function () {
        const g = new Crowdsourcing.Annotation.GeoJson({ id: 'g', body: { view: { zoom: 10 } } });
        g.setGeometry({ type: 'Point', coordinates: [1, 2] });
        expect(g.body.view).toEqual({ zoom: 10 });
        expect(g.body.geometry).toEqual({ type: 'Point', coordinates: [1, 2] });
    });

    test('setView() should attach view either to an empty or an existing body', function () {
        const g1 = new Crowdsourcing.Annotation.GeoJson({ id: 'g' });
        g1.setView({ zoom: 5 });
        expect(g1.body.view).toEqual({ zoom: 5 });

        const g2 = new Crowdsourcing.Annotation.GeoJson({ id: 'g', body: { geometry: { x: 1 } } });
        g2.setView({ zoom: 5 });
        expect(g2.body.geometry).toEqual({ x: 1 });
        expect(g2.body.view).toEqual({ zoom: 5 });
    });

    test('setName() should bootstrap an empty-body annotation with name + empty geometry/view', function () {
        const g = new Crowdsourcing.Annotation.GeoJson({ id: 'g' });
        g.setName('Hauptbahnhof');
        expect(g.body.properties.name).toBe('Hauptbahnhof');
        // Bootstrap also creates empty geometry/view stubs.
        expect(g.body.geometry).toEqual({});
        expect(g.body.view).toEqual({});
    });
});
