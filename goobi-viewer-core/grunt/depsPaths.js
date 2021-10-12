/*
Static path definitions for Goobi viewer JS + CSS dependencies 
They define what will be copied from node_modules to which Goobi viewer core lib directory

They are feed into the copy tasks `copy:js` and `copy:css`,
which can be called separately (`grunt copy:js` / `grunt copy:css`), or at once (`grunt copyDeps`)
Run these tasks after updating dependencies with `npm update`
Add new definitions after installing new packages with `npm install [<@scope>/]<name>` 
*/


  /////////////////////
 /////// JS  /////////
/////////////////////

// Path to node modules, relative to Gruntfile.js
const nodeModules = 'node_modules/';

// Goobi viewer keeps JS libraries here
const jsLibsDir = 'src/main/resources/META-INF/resources/resources/javascript/libs/';

const depsPathsJS = [
  
  { // Bootstrap
    expand: true,
    cwd: nodeModules,
    src: [
      'bootstrap/dist/js/bootstrap.bundle.min.js*', 
      'bootstrap/LICENSE'
    ],
    flatten: true, // write files to dest without creating subfolders
    dest:`${jsLibsDir}bs/`
  },

  { // Clipboard
    expand: true,
    cwd: nodeModules,
    src: [
      'clipboard/dist/clipboard.min.js', 
      'clipboard/LICENSE'
    ],
    flatten: true,
    dest:`${jsLibsDir}clipboard/`
  },

  { // HC-STICKY 
    expand: true,
    cwd: nodeModules,
    src:  [
      'hc-sticky/dist/hc-sticky.js', 
      'hc-sticky/LICENSE'
    ],
    flatten: true,
    dest:`${jsLibsDir}hcsticky/`
  },

  { // jQuery
    expand: true,
    cwd: nodeModules,
    src: [
      'jquery/dist/jquery.min.js', 
      'jquery/LICENSE.txt'
    ], 
    flatten: true,
    dest:`${jsLibsDir}jquery/`
  },

  { // LEAFLET
    expand: true,
    cwd: nodeModules,
    src: [
      'leaflet/dist/leaflet.js*', 
      'leaflet/LICENSE'
    ], 
    flatten: true,
    dest:`${jsLibsDir}leaflet/`
  },

  { // LEAFLET EXTRA-MARKERS 
    expand: true,
    cwd: nodeModules,
    src: [
      'leaflet-extra-markers/dist/js/leaflet.extra-markers.*',
      'leaflet-extra-markers/LICENSE'
    ], 
    flatten: true,
    dest:`${jsLibsDir}leaflet/extra-markers/`
  },

  { // LEAFLET MARKERCLUSTER 
    expand: true,
    cwd: nodeModules,
    src: [
      'leaflet.markercluster/dist/leaflet.markercluster.js*',
      'leaflet.markercluster/MIT-LICENCE.txt'
    ], 
    flatten: true,
    dest:`${jsLibsDir}leaflet/markercluster/`
  },

  { // LEAFLET DRAW
    expand: true,
    cwd: nodeModules,
    src: [
      'leaflet-draw/dist/leaflet.draw.js'
    ],
    flatten: true,
    dest:`${jsLibsDir}leaflet/draw/`
  },

  { // MAPBOX GL
    expand: true,
    cwd: nodeModules,
    src: [
      'mapbox-gl/dist/mapbox-gl.js*'
    ], 
    flatten: true,
    dest:`${jsLibsDir}mapbox/`
  },

  { // MAPBOX GEOCODER
    expand: true,
    cwd: nodeModules,
    src: [
      '@mapbox/mapbox-gl-geocoder/dist/mapbox-gl-geocoder.min.js*'
    ], 
    flatten: true,
    dest:`${jsLibsDir}mapbox/geocoder/`
  },

  { // MASONRY LAYOUT 
    expand: true,
    cwd: nodeModules,
    src: [
      'masonry-layout/dist/masonry.pkgd.min.js*'
    ], 
    flatten: true,
    dest: `${jsLibsDir}masonry/`,
    rename: (dest) => `${dest}masonry.min.js`
  },

  { // IMAGES LOADED (masonry layout dependency)
    expand: true,
    cwd: nodeModules,
    src: 'imagesloaded/imagesloaded.pkgd.min.js*', 
    flatten: true,
    dest: `${jsLibsDir}masonry/`,
    rename: (dest) => `${dest}imagesloaded.min.js`
  },

  { // MIRADOR
    expand: true,
    cwd: nodeModules,
    src: 'mirador/dist/mirador.min.js*', 
    flatten: true,
    dest:`${jsLibsDir}mirador/`
  },

  /* current versions are not compatible with viewer core
   * => pdfJS will not be updated when running grunt copyDeps
  { // PDFJS
    expand: true,
    cwd: nodeModules,
    src: ['pdfjs-dist/build/pdf*'],
    flatten: true,
    dest: `${jsLibsDir}pdfjs/`
  },
  */

  { // Q-PROMISES
    expand: true,
    cwd: nodeModules,
    src: 'q/q.js', 
    flatten: true,
    dest: `${jsLibsDir}q-promises/`,
    rename: (dest) => `${dest}q.min.js`
  },
  {
    expand: true,
    cwd: nodeModules,
    src: 'q/LICENSE', 
    flatten: true,
    dest:`${jsLibsDir}q-promises/`
  },

  { // RIOT
    expand: true,
    cwd: nodeModules,
    src: [
      'riot/riot.min.js',
      'riot/riot+compiler.min.js',
      'riot/LICENSE.txt'
    ],
    flatten: true,
    dest: `${jsLibsDir}riot/`
  },

  { // RXJS (reactiveX)
    expand: true,
    cwd: nodeModules,
    src: [
      '@reactivex/rxjs/dist/package/bundles/rxjs.umd.min.js*',
      '@reactivex/rxjs/LICENSE.txt'
    ],
    flatten: true,
    dest: `${jsLibsDir}reactiveX/`
  },

  { // SWAGGER-UI
    expand: true,
    cwd: nodeModules,
    src: [
      'swagger-ui-dist/swagger-ui-bundle.js*'
    ],
    flatten: true,
    dest: `${jsLibsDir}swagger/`
  },

  { // SWEETALERT2 
    expand: true,
    cwd: nodeModules,
    src: [
      'sweetalert2/dist/sweetalert2.min.js',
      'sweetalert2/LICENSE'
    ],
    flatten: true,
    dest: `${jsLibsDir}sweetalert/`
  },

  { // SWIPER
    expand: true,
    cwd: nodeModules,
    src: [
      'swiper/swiper-bundle.min.js*', 
      'swiper/LICENSE'
    ],
    flatten: true,
    dest: `${jsLibsDir}swiper/`
  },
  
  /*
  { // ThreeJS
    expand: true,
    cwd: nodeModules,
    cwd: 'node_modules/three/build/',
    src: ['three.min.js*'],
    dest: `${jsLibsDir}three/`
  },
  { // draco (ThreeJS)
    expand:true,
    cwd: 'node_modules/three/examples/jsm/controls/',
    src: ['OrbitControls.js'],
    dest: `${jsLibsDir}three/controls/`
  },
  { // draco (ThreeJS)
    expand:true,
    cwd: 'node_modules/three/examples/js/libs/draco/gltf/',
    src: ['*'],
    dest: `${jsLibsDir}three/dependencies/draco/`
  },
  { // laoders (ThreeJS)
    expand:true,
    cwd: 'node_modules/three/examples/js/loaders/',
    src: [
      'DRACOLoader.js', 
      'FBXLoader.js', 
      'GLTFLoader.js',
      'MTLLoader.js', 
      'OBJLoader.js',
      'PLYLoader.js',
      'STLLoader.js',
      'TDSLoader.js'
    ],
    dest: `${jsLibsDir}three/loaders/`
  },
  */

  { // TINYMCE
    expand: true,
    cwd: nodeModules,
    src: [
      'tinymce/jquery.tinymce.min.js*',
      'tinymce/tinymce.min.js*',
      'tinymce/license.txt',
      'tinymce/icons/**/*',
      'tinymce/plugins/**/*',
      'tinymce/skins/**/*',
      'tinymce/themes/**/*',
    ],
    flatten: false,
    dest: `${jsLibsDir}`
  },

  { // x3dom
    expand: true,
    cwd: nodeModules,
    src: [
      'x3dom/x3dom.js',
      'x3dom/README.md'
    ],
    flatten: false,
    dest: `${jsLibsDir}`
  }

];

  /////////////////////
 /////// CCS /////////
/////////////////////

// Goobi viewer keeps CSS libraries here, mostly (some can be found in corresponding JS Lib dirs)
const cssLibsDir = 'src/main/resources/META-INF/resources/resources/css/libs/'

const depsPathsCSS = [

  { // FONT-AWESOME
    expand:true,
    cwd: 'node_modules/font-awesome/',
    src: ['css/font-awesome.min.css', 'fonts/*', 'README.md'], 
    dest:`${cssLibsDir}font-awesome/`
  },

  { // LEAFLET
    expand:true,
    cwd: 'node_modules/leaflet/dist/',
    src: 'leaflet.css', 
    dest:`${cssLibsDir}leaflet/`
  },
  {
    expand:true,
    cwd: 'node_modules/leaflet/dist/images/',
    src: '*', 
    dest:`${cssLibsDir}leaflet/images/`
  },
  { // Leaflet draw
    expand:true,
    cwd: 'node_modules/leaflet-draw/dist/',
    src: 'leaflet.draw.css', 
    dest:`${cssLibsDir}leaflet/draw/`
  },
  {
    expand:true,
    cwd: 'node_modules/leaflet-draw/dist/images/',
    src: '*', 
    dest:`${cssLibsDir}leaflet/draw/images`
  },
  { // Leaflet extra-markers 
    expand:true,
    cwd: 'node_modules/leaflet-extra-markers/dist/css/',
    src: 'leaflet.extra-markers.min.css', 
    dest:`${cssLibsDir}leaflet/extra-markers`
  },
  { // this folder is copied into the leaflet root dir 
    expand:true,
    cwd: 'node_modules/leaflet-extra-markers/dist/',
    src: 'img/*', 
    dest:`${cssLibsDir}leaflet/`
  },
  { // Leaflet markerCluster
    expand:true,
    cwd: 'node_modules/leaflet.markercluster/dist/',
    src: 'MarkerCluster.css', 
    dest:`${cssLibsDir}leaflet/markercluster`
  },

  { // MAPBOX GL
    expand:true,
    cwd: 'node_modules/mapbox-gl/dist/',
    src: 'mapbox-gl.css*', 
    dest:`${cssLibsDir}mapbox/`
  },
  { // MAPBOX geocoder
    expand:true,
    cwd: 'node_modules/@mapbox/mapbox-gl-geocoder/dist/',
    src: 'mapbox-gl-geocoder.css', 
    dest:`${cssLibsDir}mapbox/geocoder/`
  },

  { // SWAGGER-UI
    expand:true,
    cwd: 'node_modules/swagger-ui-dist/',
    src: ['swagger-ui.css*'],
    dest: `${cssLibsDir}swagger/`
  },

  { // SWEETALERT2 
    expand:true,
    cwd: 'node_modules/sweetalert2/dist/',
    src: ['sweetalert2.min.css'],
    dest: `${cssLibsDir}sweetalert/`
  },

  { // SWIPER
    expand:true,
    cwd: 'node_modules/swiper/',
    src: ['swiper-bundle.min.css'],
    dest: `${cssLibsDir}swiper/`
  },
]


module.exports = {
  depsPathsJS,
  depsPathsCSS
}

