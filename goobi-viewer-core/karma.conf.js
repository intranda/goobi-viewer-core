// Karma configuration
// Generated on Wed Aug 17 2016 00:01:09 GMT+0200 (Mitteleuropäische Sommerzeit)

module.exports = function(config) {
    var configuration = {

      // base path that will be used to resolve all patterns (eg. files, exclude)
      basePath: 'WebContent/resources/javascript/',

      // plugins starting with karma- are autoloaded
      //plugins: ['karma-chrome-launcher', 'karma-jasmine', 'karma-jasmine-jquery'],
      plugins: ['karma-chrome-launcher', 'karma-jasmine', 'karma-mocha-reporter'],

      // frameworks to use
      // available frameworks: https://npmjs.org/browse/keyword/karma-adapter
      //frameworks: ['jasmine-jquery', 'jasmine'],
      frameworks: ['jasmine'],

      // list of files / patterns to load in the browser
      files: [
	      'tests/lib/jquery-1.11.3.js',

	      'libs/q-promises/q.min.js',
	      'libs/reactiveX/rx.lite.min.js',
	      'libs/jqplot/jquery.jqplot.min.js',
	      'libs/jqueryUi/1.11.0/jquery-ui.min.js',
	      'libs/openseadragon/openseadragon.js',
	      'libs/openseadragon/openseadragon-viewerinputhook.js',

	      'dev/modules/statistics/statistics.js',
	      'dev/modules/viewer/viewerJS.helper.js',
	      'dev/modules/imageView/imageView.image.js',
	      'dev/modules/imageView/imageView.controls.js',
	      'dev/modules/imageView/imageView.controls.persistence.js',
	      'dev/modules/imageView/imageView.drawRect.js',
	      'dev/modules/imageView/imageView.measures.js',
	      'dev/modules/imageView/imageView.overlays.js',
	      'dev/modules/imageView/imageView.readingMode.js',
	      'dev/modules/imageView/imageView.tileSourceResolver.js',
	      'dev/modules/imageView/imageView.transformRect.js',
	      'dev/modules/imageView/imageView.zoomSlider.js',
	      'dev/modules/cms/cmsJS.js',
	      'dev/modules/cms/cmsJS.tagList.js',
	      'dev/modules/cms/cmsJS.stackedCollection.js',

	      'tests/spec/cms.tagList-spec.js',
	      'tests/spec/cms.stackedCollection-spec.js',
	      'tests/spec/openseadragon-specs.js',
	      'tests/spec/statistics-specs.js',
	      'tests/spec/viewerHelper-spec.js',
	      'tests/spec/viewImage-spec.js',
	      'tests/spec/viewImage.controls-spec.js',
	      'tests/spec/viewImage.controls.persistence-spec.js',
	      'tests/spec/viewImage.tileSourceResolver-spec.js'
      ],

      // list of files to exclude
      exclude: [
      ],

      // preprocess matching files before serving them to the browser
      // available preprocessors: https://npmjs.org/browse/keyword/karma-preprocessor
      preprocessors: {
      },

      // test results reporter to use
      // possible values: 'dots', 'progress'
      // available reporters: https://npmjs.org/browse/keyword/karma-reporter
      reporters: ['mocha'],

      // web server port
      port: 9876,

      // enable / disable colors in the output (reporters and logs)
      colors: true,

      // level of logging
      // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
      logLevel: config.LOG_INFO,

      // enable / disable watching file and executing tests whenever any file changes
      autoWatch: true,

      // start these browsers
      // available browser launchers: https://npmjs.org/browse/keyword/karma-launcher
      browsers: ['Chrome'],

      // e.g see https://swizec.com/blog/how-to-run-javascript-tests-in-chrome-on-travis/swizec/6647
      customLaunchers: {
        Chrome_travis_ci: {
          base: 'Chrome',
          flags: ['--no-sandbox']
        }
      },

      // Continuous Integration mode
      // if true, Karma captures browsers, runs the tests and exits
      singleRun: false,

      // Concurrency level
      // how many browser should be started simultaneous
      concurrency: Infinity
    };

    if (process.env.TRAVIS) {
        configuration.browsers = ['Chrome_travis_ci'];
    }

    config.set(configuration);
} 
