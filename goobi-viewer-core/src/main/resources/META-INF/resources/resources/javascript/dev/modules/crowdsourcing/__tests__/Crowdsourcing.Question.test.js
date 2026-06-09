/**
 * Unit tests for Crowdsourcing.Question.
 *
 * Loaded through the existing crowdsourcing-loader (indirect eval),
 * which already pulls in Crowdsourcing.js + Crowdsourcing.Annotation.js
 * — both base classes that Question.js depends on.
 *
 * We exercise the static lookup tables (Type/Selector enums, get()),
 * the constructor's property wiring, and the pure prototype helpers
 * (mayAddAnnotation, isRegionTarget, getGenerator, setColors,
 * isReviewMode delegation, getTarget/getTargetId branch on
 * targetSelector). Methods that mutate areaSelector / openseadragon /
 * the underlying canvas state are out of scope.
 */
const Crowdsourcing = require('./crowdsourcing-loader')(['Crowdsourcing.Question.js']);

/** Build a question with sensible defaults; override via `extras`. */
function makeQuestion(extras) {
    const baseQuestion = {
        id: 42,
        questionType: 'PLAINTEXT',
        targetSelector: 'WHOLE_PAGE',
        targetFrequency: 0,
    };
    const item = {
        imageSource: 'https://example.org/image-source',
        getCurrentCanvas: () => 'https://example.org/current-canvas',
        isReviewMode: () => false,
        saveAnnotations: jest.fn(),
        loadAnnotations: jest.fn(() => []),
        deleteAnnotations: jest.fn(),
    };
    return new Crowdsourcing.Question(Object.assign({}, baseQuestion, extras || {}), item);
}

describe('static enums', () => {
    test('Type lookup map has all expected entries and resolves them via get()', () => {
        expect(Crowdsourcing.Question.Type.get('PLAINTEXT')).toBe('PLAINTEXT');
        expect(Crowdsourcing.Question.Type.get('METADATA')).toBe('METADATA');
        expect(Crowdsourcing.Question.Type.get('GEOLOCATION_POINT')).toBe('GEOLOCATION_POINT');
    });

    test('Type.get returns undefined for unknown question types', () => {
        expect(Crowdsourcing.Question.Type.get('NOT_A_TYPE')).toBeUndefined();
    });

    test('Selector lookup map and get() round-trip the canonical names', () => {
        // Note that MULTIPLE_PER_CANVAS deliberately maps to "WHOLE_SOURCE"
        // — the selector key and the value diverge here.
        expect(Crowdsourcing.Question.Selector.MULTIPLE_PER_CANVAS).toBe('WHOLE_SOURCE');
        expect(Crowdsourcing.Question.Selector.get('WHOLE_PAGE')).toBe('WHOLE_PAGE');
        expect(Crowdsourcing.Question.Selector.get('RECTANGLE')).toBe('RECTANGLE');
    });

    test('getType / getSelector look the value up using the question instance', () => {
        const q = { questionType: 'RICHTEXT', targetSelector: 'RECTANGLE' };
        expect(Crowdsourcing.Question.getType(q)).toBe('RICHTEXT');
        expect(Crowdsourcing.Question.getSelector(q)).toBe('RECTANGLE');
    });
});

describe('constructor wiring', () => {
    test('deep-copies the question fields onto the instance', () => {
        const q = makeQuestion({ targetFrequency: 5 });
        expect(q.id).toBe(42);
        expect(q.targetFrequency).toBe(5);
    });

    test('derives questionType and targetSelector from the lookup helpers', () => {
        const q = makeQuestion({ questionType: 'METADATA', targetSelector: 'RECTANGLE' });
        expect(q.questionType).toBe('METADATA');
        expect(q.targetSelector).toBe('RECTANGLE');
    });

    test('starts with an empty annotation list and currentAnnotationIndex = -1', () => {
        const q = makeQuestion();
        expect(q.annotations).toEqual([]);
        expect(q.currentAnnotationIndex).toBe(-1);
    });

    test('uses Crowdsourcing.frameColors when set, falls back to the built-in palette otherwise', () => {
        // Default path — no frameColors set on the namespace.
        const fallback = makeQuestion();
        expect(Array.isArray(fallback.colors)).toBe(true);
        expect(fallback.colors.length).toBeGreaterThan(0);

        // Override via the public setter on the namespace.
        Crowdsourcing.setFrameColors(['#000', '#fff']);
        try {
            const overridden = makeQuestion();
            expect(overridden.colors).toEqual(['#000', '#fff']);
        } finally {
            // Reset to keep test order independent.
            Crowdsourcing.frameColors = undefined;
        }
    });
});

describe('pure prototype helpers', () => {
    test('mayAddAnnotation returns true when targetFrequency is 0 (unlimited)', () => {
        const q = makeQuestion({ targetFrequency: 0 });
        expect(q.mayAddAnnotation()).toBe(true);
    });

    test('mayAddAnnotation returns true while annotations.length < targetFrequency', () => {
        const q = makeQuestion({ targetFrequency: 3 });
        q.annotations = [{}, {}];
        expect(q.mayAddAnnotation()).toBe(true);
    });

    test('mayAddAnnotation returns false once annotations.length === targetFrequency', () => {
        const q = makeQuestion({ targetFrequency: 2 });
        q.annotations = [{}, {}];
        expect(q.mayAddAnnotation()).toBe(false);
    });

    test('isRegionTarget is true exactly when targetSelector === RECTANGLE', () => {
        expect(makeQuestion({ targetSelector: 'RECTANGLE' }).isRegionTarget()).toBe(true);
        expect(makeQuestion({ targetSelector: 'WHOLE_PAGE' }).isRegionTarget()).toBe(false);
        expect(makeQuestion({ targetSelector: 'WHOLE_SOURCE' }).isRegionTarget()).toBe(false);
    });

    test('getGenerator returns a Software descriptor with the question id stringified', () => {
        const q = makeQuestion({ id: 7 });
        expect(q.getGenerator()).toEqual({ id: '7', type: 'Software' });
    });

    test('setColors replaces the colors array', () => {
        const q = makeQuestion();
        q.setColors(['#aaa', '#bbb']);
        expect(q.colors).toEqual(['#aaa', '#bbb']);
    });

    test('isReviewMode delegates to the underlying item', () => {
        const item = { isReviewMode: jest.fn(() => true) };
        const q = new Crowdsourcing.Question({ id: 1, questionType: 'PLAINTEXT', targetSelector: 'WHOLE_PAGE', targetFrequency: 0 }, item);
        expect(q.isReviewMode()).toBe(true);
        expect(item.isReviewMode).toHaveBeenCalled();
    });
});

describe('getTarget falls back to the current canvas (BUG: WHOLE_SOURCE branch is dead code)', () => {
    // The implementation compares this.targetSelector against
    // Crowdsourcing.Question.Selector.WHOLE_SOURCE, but the Selector
    // enum has no key named WHOLE_SOURCE — the value 'WHOLE_SOURCE'
    // is bound to the key MULTIPLE_PER_CANVAS. As a result, the
    // comparison is always `string == undefined` → false, and
    // getTarget() always returns item.getCurrentCanvas(). Pinning
    // current behavior; once the source uses Selector.MULTIPLE_PER_CANVAS
    // (or the enum is reshaped) these tests will need to be flipped.
    test('targetSelector "WHOLE_SOURCE" still returns getCurrentCanvas() because the enum lookup is broken', () => {
        const q = makeQuestion({ targetSelector: 'MULTIPLE_PER_CANVAS' });
        // The Selector.get() lookup converts MULTIPLE_PER_CANVAS to 'WHOLE_SOURCE'.
        expect(q.targetSelector).toBe('WHOLE_SOURCE');
        // Expected (post-fix): 'https://example.org/image-source'.
        expect(q.getTarget()).toBe('https://example.org/current-canvas');
    });

    test('any other selector → falls back to item.getCurrentCanvas()', () => {
        const q = makeQuestion({ targetSelector: 'WHOLE_PAGE' });
        expect(q.getTarget()).toBe('https://example.org/current-canvas');
    });
});
