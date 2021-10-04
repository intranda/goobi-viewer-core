// Static path definitions for goobi viewer JS + CSS dependencies 
// They define what will be copied from node_modules to which gv core lib directory when using a grunt task called `copyDeps`
// Run this task after updating dependencies with `npm update` etc


  /////////////////////
 /////// JS  /////////
/////////////////////

// Goobi viewer keeps JS libraries here
const jsLibsDir = 'src/main/resources/META-INF/resources/resources/javascript/libs/'

// These paths, renames etc. are feed to the copy task,
// which can be called with 'grunt copy' as well as 'grunt copyDeps'
const depsPathsJS = [
  
  { // Bootstrap
    expand:true,
    cwd: 'node_modules/bootstrap/dist/js/',
    src: ['bootstrap.bundle.min.js*'],
    dest:`${jsLibsDir}bs/`
  },
  {
    expand:true,
    cwd: 'node_modules/bootstrap/',
    src: 'LICENSE',
    dest:`${jsLibsDir}bs/`
  },

  { // Clipboard
    expand:true,
    cwd: 'node_modules/clipboard/dist/',
    src: 'clipboard.min.js',
    dest:`${jsLibsDir}clipboard/`
  },
  {
    expand:true,
    cwd: 'node_modules/clipboard/',
    src: 'LICENSE',
    dest:`${jsLibsDir}clipboard/`
  },

  { // HC-STICKY 
    expand:true,
    cwd: 'node_modules/hc-sticky/dist/',
    src: 'hc-sticky.js', 
    dest:`${jsLibsDir}hcsticky/`
  },
  {
    expand:true,
    cwd: 'node_modules/hc-sticky/',
    src: 'LICENSE', 
    dest:`${jsLibsDir}hcsticky/`
  },

  { // jQuery
    expand:true,
    cwd: 'node_modules/jquery/',
    src: ['LICENSE.txt', 'dist/jquery.min.js'], 
    flatten: true,
    dest:`${jsLibsDir}jquery/`
  },

  { // LEAFLET
    expand:true,
    cwd: 'node_modules/leaflet/dist/',
    src: 'leaflet.js*', 
    dest:`${jsLibsDir}leaflet/`
  },
  {
    expand:true,
    cwd: 'node_modules/leaflet/',
    src: 'LICENSE', 
    dest:`${jsLibsDir}leaflet/`
  },
  { // Leaflet extra-markers 
    expand:true,
    cwd: 'node_modules/leaflet-extra-markers/dist/js/',
    src: 'leaflet.extra-markers.*', 
    dest:`${jsLibsDir}leaflet/extra-markers/`
  },
  { 
    expand:true,
    cwd: 'node_modules/leaflet-extra-markers/',
    src: 'LICENSE', 
    dest:`${jsLibsDir}leaflet/extra-markers/`
  },
  { // Leaflet markercluster 
    expand:true,
    cwd: 'node_modules/leaflet.markercluster/dist/',
    src: 'leaflet.markercluster.js*', 
    dest:`${jsLibsDir}leaflet/markercluster/`
  },
  { 
    expand:true,
    cwd: 'node_modules/leaflet.markercluster/',
    src: 'MIT-LICENCE.txt', 
    dest:`${jsLibsDir}leaflet/markercluster/`
  },
  { // Leaflet draw
    expand:true,
    cwd: 'node_modules/leaflet-draw/dist/',
    src: 'leaflet.draw.js', 
    dest:`${jsLibsDir}leaflet/draw/`
  },
  { 
    expand:true,
    cwd: 'node_modules/leaflet.markercluster/',
    src: 'MIT-LICENCE.txt', 
    dest:`${jsLibsDir}leaflet/markercluster`
  },

  { // MAPBOX GL
    expand:true,
    cwd: 'node_modules/mapbox-gl/dist/',
    src: 'mapbox-gl.js*', 
    dest:`${jsLibsDir}mapbox/`
  },
  { // MAPBOX geocoder
    expand:true,
    cwd: 'node_modules/@mapbox/mapbox-gl-geocoder/dist/',
    src: 'mapbox-gl-geocoder.min.js*', 
    dest:`${jsLibsDir}mapbox/geocoder/`
  },

  { // MASONRY LAYOUT 
    expand:true,
    cwd: 'node_modules/masonry-layout/dist/',
    src: 'masonry.pkgd.min.js*', 
    rename: () => `${jsLibsDir}masonry/masonry.min.js`
  },
  { // IMAGES LOADED (masonry layout dependency)
    expand:true,
    cwd: 'node_modules/imagesloaded/',
    src: 'imagesloaded.pkgd.min.js*', 
    rename: () => `${jsLibsDir}masonry/imagesloaded.min.js`
  },

  { // MIRADOR
    expand:true,
    cwd: 'node_modules/mirador/dist/',
    src: 'mirador.min.js*', 
    dest:`${jsLibsDir}mirador/`
  },

  { // PDFJS
    expand:true,
    cwd: 'node_modules/pdfjs-dist/build/',
    src: ['pdf*'],
    dest: `${jsLibsDir}pdfjs/`
  },

  { // Q-PROMISES
    expand:true,
    cwd: 'node_modules/q/',
    src: 'q.js', 
    rename: () => `${jsLibsDir}q-promises/q.min.js`
  },
  {
    expand:true,
    cwd: 'node_modules/q/',
    src: 'LICENSE', 
    dest:`${jsLibsDir}q-promises/`
  },

  { // RIOT
    expand:true,
    cwd: 'node_modules/riot/',
    src: ['riot.min.js', 'riot+compiler.min.js', 'LICENSE.txt'],
    dest: `${jsLibsDir}riot/`
  },

  { // RXJS (reactiveX)
    expand:true,
    cwd: 'node_modules/@reactivex/rxjs/dist/package/bundles/',
    src: ['rxjs.umd.min.js*'],
    dest: `${jsLibsDir}reactiveX/`
  },
  { 
    expand:true,
    cwd: 'node_modules/@reactivex/rxjs/',
    src: ['LICENSE.txt'],
    dest: `${jsLibsDir}reactiveX/`
  },

  { // SWAGGER-UI
    expand:true,
    cwd: 'node_modules/swagger-ui-dist/',
    src: ['swagger-ui-bundle.js*'],
    dest: `${jsLibsDir}swagger/`
  },

  { // SWEETALERT2 
    expand:true,
    cwd: 'node_modules/sweetalert2/dist',
    src: ['sweetalert2.min.js'],
    dest: `${jsLibsDir}sweetalert/`
  },
  {
    expand:true,
    cwd: 'node_modules/sweetalert2/',
    src: ['LICENSE'],
    dest: `${jsLibsDir}sweetalert/`
  },

  { // SWIPER
    expand:true,
    cwd: 'node_modules/swiper/',
    src: ['swiper-bundle.min.js*', 'LICENSE'],
    dest: `${jsLibsDir}swiper/`
  },
  
  /*
  { // ThreeJS
    expand:true,
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
    expand:true,
    cwd: 'node_modules/tinymce/',
    src: ['jquery.tinymce.min.js*','tinymce.min.js*','license.txt'],
    dest: `${jsLibsDir}tinymce/`
  },
  { // icons (tinymce)
    expand:true,
    cwd: 'node_modules/tinymce/icons/',
    src: ['**/*'],
    dest: `${jsLibsDir}tinymce/icons/`
  },
  { // plugins (tinymce)
    expand:true,
    cwd: 'node_modules/tinymce/plugins/',
    src: ['**/*'],
    dest: `${jsLibsDir}tinymce/plugins/`
  },
  { // skins (tinymce)
    expand:true,
    cwd: 'node_modules/tinymce/skins/',
    src: ['**/*'],
    dest: `${jsLibsDir}tinymce/skins/`
  },
  { // themes (tinymce)
    expand:true,
    cwd: 'node_modules/tinymce/themes/',
    src: ['**/*'],
    dest: `${jsLibsDir}tinymce/themes/`
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
    src: 'css/font-awesome.min.css', 
    flatten: true,
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

