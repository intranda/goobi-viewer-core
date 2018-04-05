module.exports = function(grunt) {
	grunt.initConfig({
		theme : {
			name : 'viewer'
		},
		pkg : grunt.file.readJSON('package.json'),
		src : {
			jsDevFolder : 'WebContent/resources/javascript/dev/',
			jsDevFolderModules : 'WebContent/resources/javascript/dev/modules/',
			jsDevFolderES6 : 'WebContent/resources/javascript/dev/es6/',
			jsDistFolder : 'WebContent/resources/javascript/dist/',
//			jsDocFolder : 'doc/jsdoc/',
			cssDevFolder : 'WebContent/resources/css/dev/',
			cssDistFolder : 'WebContent/resources/css/dist/',
			lessDevFolder : 'WebContent/resources/css/less/viewer/'
		},
		less : {
			development : {
				options : {
					paths : [ '<%=src.lessDevFolder%>' ],
					plugins : [
						new ( require('less-plugin-autoprefix') )({
							browsers : [ "last 2 versions" ],
							grid : true
						})
					],
					compress : false,
					optimization : 9
				},
				files : {
					'<%=src.cssDevFolder %><%=theme.name%>.css' : '<%=src.lessDevFolder%>constructor.less'
				}
			},
			production : {
				options : {
					paths : [ '<%=src.lessDevFolder%>' ],
					plugins : [
						new ( require('less-plugin-autoprefix') )({
							browsers : [ "last 2 versions" ]
						})
					],
					compress : true,
					sourceMap : true,
					banner : '/*!\n'
						+ ' * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.\n'
						+ ' *\n'
						+ ' * Visit these websites for more information.\n'
						+ ' * - http://www.intranda.com\n'
						+ ' * - http://digiverso.com\n'
						+ ' *\n'
						+ ' * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free\n'
						+ ' * Software Foundation; either version 2 of the License, or (at your option) any later version.\n'
						+ ' *\n'
						+ ' * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or\n'
						+ ' * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.\n'
						+ ' *\n'
						+ ' * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.\n'
						+ ' */'
				},
				files : {
					'<%=src.cssDistFolder %><%=theme.name%>.min.css' : '<%=src.lessDevFolder%>constructor.less'
				}
			}
		},
		browserify : {
			dist : {
				files : {
					'<%=src.jsDevFolder%>es6.viewer.js' : '<%=src.jsDevFolderES6%>viewer/*.js',
					'<%=src.jsDevFolder%>es6.viewer.js' : '<%=src.jsDevFolderES6%>viewer/**/*.js',
					'<%=src.jsDevFolder%>es6.viewImage.js' : '<%=src.jsDevFolderES6%>viewImage/*.js',
					'<%=src.jsDevFolder%>es6.viewImage.js' : '<%=src.jsDevFolderES6%>viewImage/**/*.js'
				},
				options : {
					transform : [ [ 'babelify', {
						presets : "es2015"
					} ] ],
					browserifyOptions : {
						debug : true
					}
				}
			}
		},
		concat : {
			options : {
				separator : '\n',
				stripBanners : true,
				banner : '/*!\n'
					+ ' * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.\n'
					+ ' *\n'
					+ ' * Visit these websites for more information.\n'
					+ ' * - http://www.intranda.com\n'
					+ ' * - http://digiverso.com\n'
					+ ' *\n'
					+ ' * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free\n'
					+ ' * Software Foundation; either version 2 of the License, or (at your option) any later version.\n'
					+ ' *\n'
					+ ' * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or\n'
					+ ' * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.\n'
					+ ' *\n'
					+ ' * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.\n'
					+ ' */'
			},
			distViewer : {
				src : [
					'<%=src.jsDevFolderModules %>viewer/viewerJS.js',
					'<%=src.jsDevFolderModules %>viewer/viewerJS.*.js',
					'<%=src.jsDevFolderModules %>cms/cmsJS.js',
					'<%=src.jsDevFolderModules %>cms/cmsJS.*.js'
				],
				dest : '<%=src.jsDevFolderModules %>viewer.js'
			},
			distViewImage : {
				src : [
					'<%=src.jsDevFolderModules %>viewImage/viewImage.js',
					'<%=src.jsDevFolderModules %>viewImage/viewImage.controls.js',
					'<%=src.jsDevFolderModules %>viewImage/viewImage.*.js'
				],
				dest : '<%=src.jsDevFolderModules %>viewImage.js'
			},
		},
		uglify : {
			options : {
				mangle : true,
				compress : true,
				beautify : false,
				sourceMap : true,
				banner : '/*!\n'
					+ ' * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.\n'
					+ ' *\n'
					+ ' * Visit these websites for more information.\n'
					+ ' * - http://www.intranda.com\n'
					+ ' * - http://digiverso.com\n'
					+ ' *\n'
					+ ' * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free\n'
					+ ' * Software Foundation; either version 2 of the License, or (at your option) any later version.\n'
					+ ' *\n'
					+ ' * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or\n'
					+ ' * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.\n'
					+ ' *\n'
					+ ' * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.\n'
					+ ' */'
			},
			uglifyViewer : {
				files : {
					'<%=src.jsDistFolder%>viewer.min.js' : [ '<%=src.jsDevFolderModules%>viewer.js' ],
					'<%=src.jsDistFolder%>viewImage.min.js' : [ '<%=src.jsDevFolderModules%>viewImage.js' ],
					'<%=src.jsDistFolder%>es6.viewer.min.js' : [ '<%=src.jsDevFolder%>es6.viewer.js' ],
					'<%=src.jsDistFolder%>es6.viewImage.min.js' : [ '<%=src.jsDevFolder%>es6.viewImage.js' ]
				},
			}
		},
		watch : {
			styles : {
				files : [ '<%=src.lessDevFolder%>**/*.less' ],
				tasks : [ 'less' ],
				options : {
					nospawn : true
				}
			},
			js : {
				files : [
					'<%=src.jsDevFolderModules%>**/*.js',
					'<%=src.jsDevFolderES6%>**/*.js'
				],
				tasks : [ 'concat', 'uglify', 'browserify' ],
				options : {
					nospawn : true
				}
			}
		},
//		jsdoc : {
//			dist : {
//				src : [ '<%=src.jsDevFolderModules %>**/*.js' ],
//				options : {
//					destination : '<%=src.jsDocFolder %>',
//					template : "node_modules/ink-docstrap/template",
//					configure : "node_modules/ink-docstrap/template/jsdoc.conf.json"
//				}
//			}
//		}
	});

	// load tasks
	grunt.loadNpmTasks('grunt-contrib-less');
	grunt.loadNpmTasks('grunt-contrib-concat');
	grunt.loadNpmTasks('grunt-contrib-uglify');
	grunt.loadNpmTasks('grunt-contrib-watch');
//	grunt.loadNpmTasks('grunt-jsdoc');
	grunt.loadNpmTasks('grunt-browserify');

	// register tasks
////	grunt.registerTask('default', [ 'jsdoc', 'watch' ]);
// register development tasks
    grunt.registerTask( 'default', [ 'watch' ] );
};
