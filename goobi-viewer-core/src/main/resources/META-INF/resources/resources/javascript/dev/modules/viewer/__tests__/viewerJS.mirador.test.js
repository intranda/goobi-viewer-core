/**
 * Unit tests for viewerJS.mirador.
 *
 * The module exposes only init(); every helper is private. All assertions
 * therefore go through mirador.init({...}) and inspect the config object
 * handed to Mirador.viewer(), which is stubbed on the global scope.
 *
 * These tests pin the *pre-processing* — URL query parsing, manifest URL
 * construction, canvasIndex derivation and the shape of the emitted config.
 * None of it depends on the Mirador bundle itself, so the suite stays valid
 * across a Mirador major version bump and isolates whether a regression sits
 * in our code or in Mirador.
 */
const viewerJS = require('../viewerJS.mirador.js');

// getId() resolves both IIIF 2 ("@id") and IIIF 3 ("id"); use the real
// implementation rather than a stub so the tests exercise what ships.
// It calls viewerJS.isString, which lives in viewerJS.helper.js — stub it on
// the iiif module's own viewerJS object, mirroring viewerJS.iiif.test.js.
const iiifModule = require('../viewerJS.iiif.js');
iiifModule.isString = function (v) {
    return typeof v === 'string' || v instanceof String;
};
viewerJS.iiif = iiifModule.iiif;

/** Resolves once Mirador.viewer() has been called, with its config argument. */
function initAndCaptureConfig(config = {}) {
    return new Promise((resolve, reject) => {
        global.Mirador = {
            viewer: (miradorConfig) => {
                resolve(miradorConfig);
                return {};
            },
        };
        viewerJS.mirador.init({
            restEndpoint: 'https://viewer.example.org/api/v1/',
            manifestEndpoint: 'https://viewer.example.org/api/v1/',
            ...config,
        });
        setTimeout(() => reject(new Error('Mirador.viewer was never called')), 2000);
    });
}

function setLocation(search, pathname = '/viewMirador.xhtml') {
    history.replaceState(null, '', pathname + search);
}

beforeEach(() => {
    // translator is a sibling module; init() constructs one unconditionally.
    viewerJS.translator = function () {
        this.init = () => Promise.resolve();
        this.translate = (key) => key;
    };
    global.fetch = jest.fn(() => Promise.reject(new Error('fetch not stubbed for this test')));
    setLocation('');
});

afterEach(() => {
    delete global.Mirador;
    history.replaceState(null, '', '/');
});

describe('manifests from the URL query', () => {
    test('takes a manifest URL from ?manifest', async () => {
        setLocation('?manifest=https://other.example.org/manifest/');
        const config = await initAndCaptureConfig();
        expect(config.manifests.map((m) => m.manifestUri)).toEqual(['https://other.example.org/manifest/']);
    });

    test('builds a manifest URL from ?pi using manifestEndpoint', async () => {
        setLocation('?pi=PPN123');
        const config = await initAndCaptureConfig();
        expect(config.manifests.map((m) => m.manifestUri)).toEqual(['https://viewer.example.org/api/v1/records/PPN123/manifest/']);
    });

    test('accepts ?pi repeated', async () => {
        setLocation('?pi=PPN1&pi=PPN2');
        const config = await initAndCaptureConfig();
        expect(config.manifests).toHaveLength(2);
        expect(config.manifests[1].manifestUri).toContain('PPN2');
    });

    test('splits a single ?pi value on commas', async () => {
        setLocation('?pi=PPN1,PPN2,PPN3');
        const config = await initAndCaptureConfig();
        expect(config.manifests).toHaveLength(3);
    });

    test('splits a single ?pi value on dollar signs', async () => {
        setLocation('?pi=PPN1$PPN2');
        const config = await initAndCaptureConfig();
        expect(config.manifests).toHaveLength(2);
    });

    test('drops empty segments produced by the split', async () => {
        setLocation('?pi=PPN1,,PPN2');
        const config = await initAndCaptureConfig();
        expect(config.manifests).toHaveLength(2);
    });

    test('concatenates ?manifest and ?pi, manifest URLs first', async () => {
        setLocation('?pi=PPN1&manifest=https://other.example.org/m/');
        const config = await initAndCaptureConfig();
        expect(config.manifests.map((m) => m.manifestUri)).toEqual(['https://other.example.org/m/', 'https://viewer.example.org/api/v1/records/PPN1/manifest/']);
    });

    test('falls back to restEndpoint when manifestEndpoint is not configured', async () => {
        setLocation('?pi=PPN1');
        const config = await initAndCaptureConfig({ manifestEndpoint: undefined });
        expect(config.manifests[0].manifestUri).toBe('https://viewer.example.org/api/v1/records/PPN1/manifest/');
    });
});

describe('canvasIndex derivation', () => {
    test('maps ?page=5 to canvasIndex 4 on every window', async () => {
        setLocation('?pi=PPN1,PPN2&page=5');
        const config = await initAndCaptureConfig({ startPage: undefined });
        expect(config.windows.map((w) => w.canvasIndex)).toEqual([4, 4]);
    });

    test('defaults canvasIndex to 0 without ?page', async () => {
        setLocation('?pi=PPN1');
        const config = await initAndCaptureConfig({ startPage: undefined });
        expect(config.windows[0].canvasIndex).toBe(0);
    });

    test('lets an explicitly configured startPage win over the query', async () => {
        setLocation('?pi=PPN1&page=5');
        const config = await initAndCaptureConfig({ startPage: 3 });
        expect(config.windows[0].canvasIndex).toBe(2);
    });
});

describe('emitted Mirador config shape', () => {
    test('carries the container id, single view and the annotation motivation filter', async () => {
        setLocation('?pi=PPN1');
        const config = await initAndCaptureConfig();
        expect(config.id).toBe('miradorViewer');
        expect(config.window.defaultView).toBe('single');
        expect(config.annotations.filteredMotivations).toEqual(expect.arrayContaining(['oa:commenting', 'oa:tagging', 'oa:describing']));
        // sc:painting and supplementing stay out so fulltext is not rendered as annotations
        expect(config.annotations.filteredMotivations).not.toContain('sc:painting');
        expect(config.annotations.filteredMotivations).not.toContain('supplementing');
    });

    test('emits one window per manifest, each pinned to its own manifest', async () => {
        setLocation('?pi=PPN1,PPN2');
        const config = await initAndCaptureConfig();
        expect(config.windows).toHaveLength(2);
        expect(config.windows.map((w) => w.loadedManifest)).toEqual(config.manifests.map((m) => m.manifestUri));
        expect(config.windows[0].thumbnailNavigationPosition).toBe('far-bottom');
    });

    test('does not pass any workspace layout - Mirador arranges the windows itself', async () => {
        setLocation('?pi=PPN1,PPN2,PPN3,PPN4,PPN5');
        const config = await initAndCaptureConfig();
        expect(config.workspace).toBeUndefined();
        expect(config).not.toHaveProperty('columns');
        expect(config).not.toHaveProperty('rows');
    });
});

describe('bookmark list URLs', () => {
    test('requests the user bookmark collection for /mirador/id/<n>/', async () => {
        setLocation('', '/mirador/id/42/');
        global.fetch = jest.fn(() => Promise.resolve({ json: () => Promise.resolve({ members: [{ type: 'Manifest', id: 'https://m/1' }] }) }));
        const config = await initAndCaptureConfig();
        expect(global.fetch).toHaveBeenCalledWith('https://viewer.example.org/api/v1/bookmarks/42/collection.json');
        expect(config.manifests.map((m) => m.manifestUri)).toEqual(['https://m/1']);
    });

    test('requests the shared bookmark collection for /mirador/key/<key>/', async () => {
        setLocation('', '/mirador/key/abc123/');
        global.fetch = jest.fn(() => Promise.resolve({ json: () => Promise.resolve({ members: [{ '@type': 'sc:Manifest', '@id': 'https://m/2' }] }) }));
        const config = await initAndCaptureConfig();
        expect(global.fetch).toHaveBeenCalledWith('https://viewer.example.org/api/v1/bookmarks/shared/abc123/collection.json');
        expect(config.manifests.map((m) => m.manifestUri)).toEqual(['https://m/2']);
    });

    test('keeps only members that are IIIF manifests', async () => {
        setLocation('', '/mirador/id/7/');
        global.fetch = jest.fn(() =>
            Promise.resolve({
                json: () =>
                    Promise.resolve({
                        members: [
                            { type: 'Manifest', id: 'https://m/keep' },
                            { type: 'Collection', id: 'https://m/drop' },
                        ],
                    }),
            })
        );
        const config = await initAndCaptureConfig();
        expect(config.manifests.map((m) => m.manifestUri)).toEqual(['https://m/keep']);
    });

    test('prefers query manifests over a bookmark list id in the path', async () => {
        setLocation('?pi=PPN1', '/mirador/id/42/');
        const config = await initAndCaptureConfig();
        expect(global.fetch).not.toHaveBeenCalled();
        expect(config.manifests[0].manifestUri).toContain('PPN1');
    });
});

/*
 * The two cases below pin defects rather than intended behaviour, so that a
 * later fix shows up as a failing test instead of passing silently.
 */
describe('current behaviour without any parameters (known defect)', () => {
    test('opens an empty Mirador instead of loading the session bookmark list', async () => {
        setLocation('', '/viewMirador.xhtml');
        const config = await initAndCaptureConfig();
        // The session branch guards on `_getBookmarkListId() !== null` after an
        // earlier `!= null` branch already returned, so it can never be entered
        // and no bookmarks request is made.
        expect(global.fetch).not.toHaveBeenCalled();
        expect(config).toEqual({ id: 'miradorViewer' });
    });

    test('leaves the emitted config without manifests, which breaks the title override', async () => {
        setLocation('', '/viewMirador.xhtml');
        const config = await initAndCaptureConfig();
        // init() dereferences miradorConfig.manifests.length right after
        // Mirador.viewer(); with this config that throws and the error is
        // swallowed by the surrounding catch.
        expect(config.manifests).toBeUndefined();
    });
});
