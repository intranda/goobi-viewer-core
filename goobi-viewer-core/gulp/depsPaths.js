/*
Static path definitions for Goobi viewer JS + CSS dependencies 
They define what will be copied from node_modules to which Goobi viewer core lib directory
*/

/////////////////////
/////// JS  /////////
/////////////////////

// Path to node modules, relative to gulpfile.js
const nodeModules = 'node_modules/';

// Goobi viewer keeps JS libraries here
const jsLibsDir = 'src/main/resources/META-INF/resources/resources/javascript/libs/';
// Goobi viewer keeps CSS libraries here, mostly (some can be found in corresponding JS Lib dirs)
const cssLibsDir = 'src/main/resources/META-INF/resources/resources/css/libs/';
const iconDir = 'src/main/resources/META-INF/resources/resources/icons/';

const depsPathsJS = [
    {
        // TABLER-ICONS
        expand: true,
        cwd: nodeModules,
        flatten: true,
        src: ['@tabler/icons/LICENSE'],
        dest: `${iconDir}`,
    },

    {
        // Bootstrap
        expand: true,
        cwd: nodeModules,
        src: ['bootstrap/dist/js/bootstrap.bundle.min.js*', 'bootstrap/LICENSE'],
        flatten: true, // write files to dest without creating subfolders
        dest: `${jsLibsDir}bs/`,
    },

    {
        // Clipboard
        expand: true,
        cwd: nodeModules,
        src: ['clipboard/dist/clipboard.min.js', 'clipboard/LICENSE'],
        flatten: true,
        dest: `${jsLibsDir}clipboard/`,
    },

    {
        // Codemirror
        expand: true,
        cwd: nodeModules,
        src: [
            // BASIC FUNCTIONALITY
            'codemirror/lib/codemirror.js',
            // SEARCH ADDON
            'codemirror/addon/search/search.js',
            'codemirror/addon/search/match-highlighter.js',
            'codemirror/addon/search/searchcursor.js',
            'codemirror/addon/search/jump-to-line.js',
            // FULLSCREEN ADDON
            'codemirror/addon/display/fullscreen.js',
            // DIALOG NEEDED FOR SEARCH ADDON
            'codemirror/addon/dialog/dialog.js', //  - RELIES ON dialog.css
            // HIGHLIGHT ACTIVE LINE ADDON
            'codemirror/addon/selection/active-line.js',
            // XML SYNTAX FILE
            'codemirror/mode/xml/xml.js',
            // JS SYNTAX FILE
            'codemirror/mode/javascript/javascript.js',
            // CSS SYNTAX FILE
            'codemirror/mode/css/css.js',
            // PROPERTIES SYNTAX FILE
            'codemirror/mode/properties/properties.js',
            // LICENSE FILE
            'codemirror/LICENSE',
        ],
        flatten: true,
        dest: `${jsLibsDir}codemirror/`,
    },

    {
        // HC-STICKY
        expand: true,
        cwd: nodeModules,
        src: ['hc-sticky/dist/hc-sticky.js', 'hc-sticky/LICENSE'],
        flatten: true,
        dest: `${jsLibsDir}hcsticky/`,
    },

    {
        // jszip
        expand: true,
        cwd: nodeModules,
        src: ['jszip/dist/jszip.min.js'],
        flatten: true,
        dest: `${jsLibsDir}jszip/`,
    },

    {
        // epub-ts — replaces the unmaintained epubjs (last release 2022), whose prebuilt
        // bundle carried @xmldom/xmldom 0.7.13 with five open high-severity advisories.
        // epub-ts is an API-compatible TypeScript rewrite of exactly that epubjs version
        // and pulls in jszip only. The UMD build registers the same global `ePub`
        // function that viewEpub.xhtml calls, so the reader code stays unchanged.
        expand: true,
        cwd: nodeModules,
        src: ['@likecoin/epub-ts/dist/epub.umd.js'],
        flatten: true,
        dest: `${jsLibsDir}epubts/`,
    },

    {
        // FLATPICKR
        expand: true,
        cwd: nodeModules,
        src: ['flatpickr/dist/flatpickr.js'],
        flatten: true,
        dest: `${jsLibsDir}flatpickr/`,
    },
    {
        // FLATPICKR GERMAN LOCALE
        expand: true,
        cwd: nodeModules,
        src: ['flatpickr/dist/l10n/de.js'],
        flatten: true,
        dest: `${jsLibsDir}flatpickr/l10n/`,
    },

    {
        // jQuery
        expand: true,
        cwd: nodeModules,
        src: ['jquery/dist/jquery.min.js', 'jquery/LICENSE.txt'],
        flatten: true,
        dest: `${jsLibsDir}jquery/`,
    },

    {
        // jQuery UI
        expand: true,
        cwd: nodeModules,
        src: ['jquery-ui/dist/jquery-ui.min.js', 'jquery-ui/LICENCE.txt'],
        flatten: true,
        dest: `${jsLibsDir}jqueryUi/`,
    },

    {
        // LEAFLET
        expand: true,
        cwd: nodeModules,
        src: ['leaflet/dist/leaflet.js*', 'leaflet/LICENSE'],
        flatten: true,
        dest: `${jsLibsDir}leaflet/`,
    },

    {
        // LEAFLET EXTRA-MARKERS
        expand: true,
        cwd: nodeModules,
        src: ['leaflet-extra-markers/dist/js/leaflet.extra-markers.*', 'leaflet-extra-markers/LICENSE'],
        flatten: true,
        dest: `${jsLibsDir}leaflet/extra-markers/`,
    },

    {
        // LEAFLET MARKERCLUSTER
        expand: true,
        cwd: nodeModules,
        src: ['leaflet.markercluster/dist/leaflet.markercluster.js*', 'leaflet.markercluster/MIT-LICENCE.txt'],
        flatten: true,
        dest: `${jsLibsDir}leaflet/markercluster/`,
    },

    {
        // LEAFLET DRAW
        expand: true,
        cwd: nodeModules,
        src: ['leaflet-draw/dist/leaflet.draw.js'],
        flatten: true,
        dest: `${jsLibsDir}leaflet/draw/`,
    },

    {
        // SIMPLE-LIGHTBOX
        expand: true,
        cwd: nodeModules,
        src: ['simple-lightbox/dist/simpleLightbox.min.js', 'simple-lightbox/LICENSE'],
        flatten: true,
        dest: `${jsLibsDir}simpleLightbox/`,
    },

    {
        // Simplebar
        expand: true,
        cwd: nodeModules,
        src: ['simplebar/dist/simplebar.min.js', 'simplebar/LICENSE'],
        flatten: true,
        dest: `${jsLibsDir}simplebar/`,
    },

    {
        // MAPBOX GL
        expand: true,
        cwd: nodeModules,
        src: ['mapbox-gl/dist/mapbox-gl.js*'],
        flatten: true,
        dest: `${jsLibsDir}mapbox/`,
    },

    {
        // MAPBOX GEOCODER
        expand: true,
        cwd: nodeModules,
        src: ['@mapbox/mapbox-gl-geocoder/dist/mapbox-gl-geocoder.min.js*'],
        flatten: true,
        dest: `${jsLibsDir}mapbox/geocoder/`,
    },

    {
        // MASONRY LAYOUT
        expand: true,
        cwd: nodeModules,
        src: ['masonry-layout/dist/masonry.pkgd.min.js*'],
        flatten: true,
        dest: `${jsLibsDir}masonry/`,
        rename: (dest) => `${dest}masonry.min.js`,
    },

    {
        // MIRADOR
        expand: true,
        cwd: nodeModules,
        src: 'mirador/dist/mirador.min.js*',
        flatten: true,
        dest: `${jsLibsDir}mirador/`,
    },

    /* current versions are not compatible with viewer core
  { // PDFJS
    expand: true,
    cwd: nodeModules,
    src: ['pdfjs-dist/build/pdf*'],
    flatten: true,
    dest: `${jsLibsDir}pdfjs/`
  },
  */

    {
        // RIOT
        expand: true,
        cwd: nodeModules,
        src: ['riot/riot.min.js', 'riot/riot+compiler.min.js', 'riot/LICENSE.txt'],
        flatten: true,
        dest: `${jsLibsDir}riot/`,
    },

    {
        // RXJS (reactiveX)
        expand: true,
        cwd: nodeModules,
        src: ['@reactivex/rxjs/dist/package/bundles/rxjs.umd.min.js*', '@reactivex/rxjs/LICENSE.txt'],
        flatten: true,
        dest: `${jsLibsDir}reactiveX/`,
    },

    {
        // SCALAR API REFERENCE
        // The standalone build is self-contained: it references no chunk files
        // and injects its own styles, so there is no CSS counterpart below.
        //
        // Checklist when bumping @scalar/api-reference:
        // 1. Re-grep the new standalone.js for `*.scalar.com` hosts - a version
        //    bump can add new outbound calls that withDefaultFonts/hideClientButton/
        //    showDeveloperTools do not cover yet. Pay particular attention to the
        //    proxy default: 1.64.1 falls back to proxy.scalar.com only for
        //    `layout: 'web'`, which we never use, and that is the one place where an
        //    outbound call can appear without any explicit configuration.
        // 2. Verify requestBuilder still exposes path: {variables, raw} and that
        //    the beforeRequest hook still runs before the payload is built - Scalar
        //    documents onBeforeRequest itself as an "Experimental API", and a break
        //    here fails silently (POST requests just stop reaching their resource).
        //    See viewerJS.apiDocs.js.
        // 3. libs/scalar/LICENSE.txt is hand-written, not copied from node_modules
        //    (the package ships no LICENSE file) - diff it against the upstream
        //    license by hand on every bump.
        // 4. Check whether Scalar still hides elements behind Tailwind's
        //    `hidden <variant>:<display>` pattern. viewer.min.css carries a global
        //    `.hidden { display: none !important }`, which beats those rules and
        //    collapsed the whole navigation sidebar until css/misc/rest_api.css
        //    re-asserted them. New occurrences need the same treatment there.
        expand: true,
        cwd: nodeModules,
        src: ['@scalar/api-reference/dist/browser/standalone.js'],
        flatten: true,
        dest: `${jsLibsDir}scalar/`,
    },

    {
        // SWEETALERT2
        expand: true,
        cwd: nodeModules,
        src: ['sweetalert2/dist/sweetalert2.min.js', 'sweetalert2/LICENSE'],
        flatten: true,
        dest: `${jsLibsDir}sweetalert/`,
    },

    {
        // SWIPER
        expand: true,
        cwd: nodeModules,
        src: ['swiper/swiper-bundle.min.js*', 'swiper/LICENSE'],
        flatten: true,
        dest: `${jsLibsDir}swiper/`,
    },

    {
        // CHARTJS
        expand: true,
        cwd: nodeModules,
        src: ['chart.js/dist/chart.umd.js*', 'chart.js/LICENSE.md'],
        flatten: true,
        dest: `${jsLibsDir}chartjs/`,
    },

    {
        // LUXON (date and time module for chartjs)
        expand: true,
        cwd: nodeModules,
        src: ['luxon/build/global/luxon.js*', 'luxon/LICENSE.md'],
        flatten: true,
        dest: `${jsLibsDir}chartjs/luxon`,
    },

    {
        // LUXON-CHARTJS-INTEGRATION
        expand: true,
        cwd: nodeModules,
        src: ['chartjs-adapter-luxon/dist/chartjs-adapter-luxon.umd.min.js', 'chartjs-adapter-luxon/LICENSE.md'],
        flatten: true,
        dest: `${jsLibsDir}chartjs/chartjs-adapter-luxon`,
    },
	
    {
        // replayWeb.page — only the service worker is copied verbatim.
        // ui.js is NOT copied: gulp/replaywebpagePatch.js generates it from
        // node_modules with the download-menu patch applied. See that file's
        // header for why the output has to keep the name ui.js.
        expand: true,
        cwd: nodeModules,
        src: ['replaywebpage/sw.js'],
        flatten: true,
        dest: `${jsLibsDir}replaywebpage/`,
    },


    {
        // TINYMCE
        expand: true,
        cwd: nodeModules,
        src: [
            'tinymce/tinymce.min.js*',
            'tinymce/license.md',
            'tinymce/notices.txt',
            'tinymce/icons/**/*',
            'tinymce/models/**/*',
            'tinymce/plugins/**/*',
            'tinymce/skins/**/*',
            'tinymce/themes/**/*',
        ],
        flatten: false,
        dest: `${jsLibsDir}`,
    },
];

/////////////////////
/////// CSS /////////
/////////////////////

const depsPathsCSS = [
    {
        // CODEMIRROR
        expand: true,
        cwd: nodeModules,
        src: [
            'codemirror/lib/codemirror.css',
            'codemirror/addon/display/fullscreen.css',
            'codemirror/addon/dialog/dialog.css',
            'codemirror/theme/dracula.css',
        ],
        flatten: true,
        dest: `${cssLibsDir}codemirror/`,
    },

    {
        // FONT-AWESOME
        expand: true,
        cwd: nodeModules,
        src: ['font-awesome/css/font-awesome.min.css', 'font-awesome/fonts/*', 'font-awesome/README.md'],
        flatten: false,
        dest: `${cssLibsDir}`,
    },

    {
        // FLATPICKR
        expand: true,
        cwd: nodeModules,
        src: ['flatpickr/dist/flatpickr.css'],
        flatten: true,
        dest: `${cssLibsDir}flatpickr/`,
    },

    {
        // JQUERY UI
        expand: true,
        cwd: `${nodeModules}/jquery-ui/dist/themes/base/`,
        src: ['jquery-ui.min.css'],
        flatten: false,
        dest: `${cssLibsDir}jQueryUi/`,
    },

    {
        // LEAFLET
        expand: true,
        cwd: `${nodeModules}/leaflet/dist/`,
        src: ['leaflet.css', 'images/*'],
        flatten: false,
        dest: `${cssLibsDir}leaflet/`,
    },

    {
        // Leaflet draw
        expand: true,
        cwd: `${nodeModules}/leaflet-draw/dist/`,
        src: ['leaflet.draw.css', 'images/*'],
        flatten: false,
        dest: `${cssLibsDir}leaflet/draw/`,
    },

    {
        // Leaflet extra-markers
        expand: true,
        cwd: nodeModules,
        src: 'leaflet-extra-markers/dist/css/leaflet.extra-markers.min.css',
        flatten: true,
        dest: `${cssLibsDir}leaflet/extra-markers`,
    },
    {
        // ! extra-markers images folder is copied into the leaflet root dir !
        expand: true,
        cwd: nodeModules,
        src: 'leaflet-extra-markers/dist/img/*',
        flatten: true,
        dest: `${cssLibsDir}leaflet/img`,
    },

    {
        // Leaflet markerCluster
        expand: true,
        cwd: nodeModules,
        src: 'leaflet.markercluster/dist/MarkerCluster.css',
        flatten: true,
        dest: `${cssLibsDir}leaflet/markercluster`,
    },

    {
        // Simple Lightbox
        expand: true,
        cwd: nodeModules,
        src: 'simple-lightbox/dist/simpleLightbox.min.css',
        flatten: true,
        dest: `${cssLibsDir}simpleLightbox/`,
    },

    {
        // Simplebar
        expand: true,
        cwd: nodeModules,
        src: 'simplebar/dist/simplebar.min.css',
        flatten: true,
        dest: `${cssLibsDir}simplebar/`,
    },

    {
        // MAPBOX GL
        expand: true,
        cwd: nodeModules,
        src: 'mapbox-gl/dist/mapbox-gl.css*',
        flatten: true,
        dest: `${cssLibsDir}mapbox/`,
    },

    {
        // MAPBOX geocoder
        expand: true,
        cwd: nodeModules,
        src: '@mapbox/mapbox-gl-geocoder/dist/mapbox-gl-geocoder.css',
        flatten: true,
        dest: `${cssLibsDir}mapbox/geocoder/`,
    },

    {
        // SWEETALERT2
        expand: true,
        cwd: nodeModules,
        src: ['sweetalert2/dist/sweetalert2.min.css'],
        flatten: true,
        dest: `${cssLibsDir}sweetalert/`,
    },

    {
        // SWIPER
        expand: true,
        cwd: nodeModules,
        src: ['swiper/swiper-bundle.min.css'],
        flatten: true,
        dest: `${cssLibsDir}swiper/`,
    },
];

const tablerIconSources = [
    {
        variant: 'outline',
        src: `${nodeModules}@tabler/icons/icons/outline/**/*.svg`,
        base: `${nodeModules}@tabler/icons/icons/outline/`,
    },
    {
        variant: 'filled',
        src: `${nodeModules}@tabler/icons/icons/filled/**/*.svg`,
        base: `${nodeModules}@tabler/icons/icons/filled/`,
    },
];

module.exports = {
    depsPathsJS,
    depsPathsCSS,
    tablerIconSources,
};
