/**
 * Unit tests for the Crowdsourcing namespace utilities and the
 * Crowdsourcing.Annotation base class.
 *
 * Subclass-specific tests live in sibling files
 * (Crowdsourcing.Annotation.<Subtype>.test.js).
 */
const Crowdsourcing = require('./crowdsourcing-loader')();

describe('Crowdsourcing namespace utilities', function () {
    describe('isString', function () {
        test.each([
            ['string literal', 'abc', true],
            ['String object', new String('abc'), true],
            ['number', 42, false],
            ['null', null, false],
            ['object', {}, false],
            ['array', ['a'], false],
        ])('should return %s → %s for %p', function (_label, input, expected) {
            expect(Crowdsourcing.isString(input)).toBe(expected);
        });
    });

    describe('deepCopy', function () {
        test('should return a structurally equal but reference-different copy', function () {
            const original = { a: 1, nested: { b: 2 } };
            const copy = Crowdsourcing.deepCopy(original);
            expect(copy).toEqual(original);
            expect(copy).not.toBe(original);
            expect(copy.nested).not.toBe(original.nested);
        });

        test('should not propagate mutations of the copy back to the source', function () {
            const original = { nested: { value: 'one' } };
            const copy = Crowdsourcing.deepCopy(original);
            copy.nested.value = 'two';
            expect(original.nested.value).toBe('one');
        });
    });

    describe('getResourceId', function () {
        test('should return the input unchanged when it is already a string', function () {
            expect(Crowdsourcing.getResourceId('https://example.org/foo')).toBe('https://example.org/foo');
        });

        test('should prefer .source over .id and @id', function () {
            expect(Crowdsourcing.getResourceId({ source: 's', id: 'i', '@id': '@i' })).toBe('s');
        });

        test('should fall back to .id when .source is absent', function () {
            expect(Crowdsourcing.getResourceId({ id: 'i', '@id': '@i' })).toBe('i');
        });

        test('should fall back to @id last (legacy IIIF v2 shape)', function () {
            expect(Crowdsourcing.getResourceId({ '@id': '@i' })).toBe('@i');
        });

        test('should JSON-stringify the input as a deterministic last resort', function () {
            expect(Crowdsourcing.getResourceId({ foo: 'bar' })).toBe('{"foo":"bar"}');
        });
    });

    describe('translate', function () {
        test('should return the key itself when no translator is wired up', function () {
            // No initTranslations() called → translator is undefined.
            expect(Crowdsourcing.translate('hello')).toBe('hello');
        });

        test('should delegate to the wired-up translator when present', function () {
            Crowdsourcing.translator = {
                translate: function (key, lang) {
                    return key.toUpperCase() + '@' + (lang || 'default');
                },
            };
            try {
                expect(Crowdsourcing.translate('hi', 'de')).toBe('HI@de');
            } finally {
                delete Crowdsourcing.translator;
            }
        });
    });

    describe('loadTranslations', function () {
        test('should throw when no translator is initialised', function () {
            expect(function () {
                Crowdsourcing.loadTranslations(['a']);
            }).toThrow();
        });

        test('should delegate to the translator when present', function () {
            const calls = [];
            Crowdsourcing.translator = {
                addTranslations: function (keys) {
                    calls.push(keys);
                    return Promise.resolve('ok');
                },
            };
            try {
                expect(Crowdsourcing.loadTranslations(['a', 'b'])).toBeInstanceOf(Promise);
                expect(calls).toEqual([['a', 'b']]);
            } finally {
                delete Crowdsourcing.translator;
            }
        });
    });
});

describe('Crowdsourcing.Annotation (base class)', function () {
    test('should deep-copy the input annotation so the source object is decoupled', function () {
        const source = { id: 'a1', body: { value: 'x' } };
        const anno = new Crowdsourcing.Annotation(source);
        anno.body.value = 'y';
        expect(source.body.value).toBe('x');
    });

    test('should default isEmpty() to "no body" semantics', function () {
        expect(new Crowdsourcing.Annotation({ id: 'a' }).isEmpty()).toBe(true);
        expect(new Crowdsourcing.Annotation({ id: 'a', body: {} }).isEmpty()).toBe(false);
    });

    test('should set `created` to a UTC ISO string without milliseconds', function () {
        const anno = new Crowdsourcing.Annotation({});
        anno.setCreated(new Date('2024-03-15T10:20:30.456Z'));
        expect(anno.created).toBe('2024-03-15T10:20:30Z');
    });

    test('should fill .created automatically when constructed without an annotation', function () {
        const before = new Date();
        const anno = new Crowdsourcing.Annotation();
        const created = new Date(anno.created);
        // Created stamp should be within the last few seconds.
        expect(created.getTime()).toBeGreaterThanOrEqual(before.getTime() - 1000);
        expect(created.getTime()).toBeLessThanOrEqual(Date.now() + 1000);
    });

    test('getCreated() should round-trip the ISO string into a millisecond timestamp', function () {
        const anno = new Crowdsourcing.Annotation({});
        anno.setCreated(new Date('2024-01-01T00:00:00Z'));
        expect(anno.getCreated()).toBe(Date.parse('2024-01-01T00:00:00Z'));
    });

    test('getModified() should return undefined until setModified is called', function () {
        const anno = new Crowdsourcing.Annotation({ id: 'a' });
        expect(anno.getModified()).toBeUndefined();
        anno.setModified(new Date('2024-06-01T12:00:00Z'));
        expect(anno.getModified()).toBe(Date.parse('2024-06-01T12:00:00Z'));
    });

    describe('setRegion / getRegion (FragmentSelector)', function () {
        test('should throw when called without a target', function () {
            const anno = new Crowdsourcing.Annotation({});
            expect(function () {
                anno.setRegion({ x: 0, y: 0, width: 1, height: 1 });
            }).toThrow();
        });

        test('should attach an xywh FragmentSelector to a string-target', function () {
            const anno = new Crowdsourcing.Annotation({ target: 'https://example.org/img' });
            anno.setRegion({ x: 10, y: 20, width: 30, height: 40 });
            expect(anno.target.selector.value).toBe('xywh=10,20,30,40');
            expect(anno.target.source).toBe('https://example.org/img');
        });

        test('should round float coordinates when serialising', function () {
            const anno = new Crowdsourcing.Annotation({ target: 'x' });
            anno.setRegion({ x: 1.4, y: 2.6, width: 99.5, height: 0.4 });
            expect(anno.target.selector.value).toBe('xywh=1,3,100,0');
        });

        test('should parse an xywh selector back into x/y/width/height ints', function () {
            const anno = new Crowdsourcing.Annotation({
                target: { selector: { type: 'FragmentSelector', value: 'xywh=5,6,7,8' } },
            });
            expect(anno.getRegion()).toEqual({ x: 5, y: 6, width: 7, height: 8 });
        });

        test('should return undefined for getRegion when target has no selector', function () {
            const anno = new Crowdsourcing.Annotation({ target: 'https://example.org/img' });
            expect(anno.getRegion()).toBeUndefined();
        });
    });

    describe('setColor / getColor', function () {
        test('should round-trip a colour value', function () {
            const anno = new Crowdsourcing.Annotation({});
            expect(anno.getColor()).toBeUndefined();
            anno.setColor('#ff0033');
            expect(anno.getColor()).toBe('#ff0033');
        });
    });
});
