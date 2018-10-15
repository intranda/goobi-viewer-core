module.exports = function(grunt) {
	// ---------- VARIABLES ----------
	var banner = '/*!\n'
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
		+ ' */';

	// ---------- PROJECT CONFIG ----------
	grunt.initConfig({
		theme : {
			name : 'viewer'
		},
		pkg : grunt.file.readJSON('package.json'),
		src : {
			jsDevFolder : 'WebContent/resources/javascript/dev/',
			jsDevFolderModules : 'WebContent/resources/javascript/dev/modules/',
			jsDistFolder : 'WebContent/resources/javascript/dist/',
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
					banner : banner,
					paths : [ '<%=src.lessDevFolder%>' ],
					plugins : [
						new ( require('less-plugin-autoprefix') )({
							browsers : [ "last 2 versions" ],
							grid : true
						})
					],
					compress : true,
					sourceMap : true,
					outputSourceFiles: true,
				},
				files : {
					'<%=src.cssDistFolder %><%=theme.name%>.min.css' : '<%=src.lessDevFolder%>constructor.less'
				}
			}
		},
		concat : {
			options : {
				banner : banner,
				separator : '\n',
				stripBanners : true,
				sourceMap : false
			},
			distViewer : {
				src : [
					'<%=src.jsDevFolderModules %>viewer/viewerJS.js',
					'<%=src.jsDevFolderModules %>viewer/viewerJS.*.js',
					'<%=src.jsDevFolderModules %>cms/cmsJS.js',
					'<%=src.jsDevFolderModules %>cms/cmsJS.*.js'
				],
				dest : '<%=src.jsDistFolder%>viewer.min.js'
			},
			distViewImage : {
				src : [
					'<%=src.jsDevFolderModules %>imageView/imageView.image.js',
					'<%=src.jsDevFolderModules %>imageView/imageView.controls.js',
					'<%=src.jsDevFolderModules %>imageView/imageView.overlay.js',
					'<%=src.jsDevFolderModules %>imageView/imageView.*.js'
				],
				dest : '<%=src.jsDistFolder%>viewImage.min.js'
			},
			distViewObject : {
                src : [
                    '<%=src.jsDevFolderModules %>objectView/*Loader.js',
                    '<%=src.jsDevFolderModules %>objectView/WorldGenerator.js',
                ],
                dest : '<%=src.jsDistFolder%>objectView.min.js'
            },
		},
		watch : {
			configFiles : {
				files : [ 'Gruntfile.js' ],
				options : {
					reload : true
				}
			},
			css : {
				files : [ '<%=src.lessDevFolder%>**/*.less' ],
				tasks : [ 'less' ],
				options : {
					spawn : false,
				}
			},
			scripts : {
				files : [
					'<%=src.jsDevFolderModules%>**/*.js'
				],
				tasks : [ 'concat' ],
				options : {
					spawn : false,
				}
			},
			riot: {
				files : [
					'<%=src.jsDevFolder %>tags/**/*.tag'
				],
				tasks : [ 'riot' ],
				options : {
					spawn : false,
				}
			}
		},
		riot: {
            options:{
                concat: true
            },
            dist: {
                expand: false,
                src: '<%=src.jsDevFolder %>tags/**/*.tag',
                dest: '<%=src.jsDistFolder%>riot-tags.js'
            }
        },
	});
	
	// ---------- LOAD TASKS ----------
	grunt.loadNpmTasks('grunt-contrib-less');
	grunt.loadNpmTasks('grunt-contrib-concat');
	grunt.loadNpmTasks('grunt-riot');
	grunt.loadNpmTasks('grunt-contrib-watch');

	// ---------- REGISTER DEVELOPMENT TASKS ----------
	grunt.registerTask('default', [ 'watch' ]);
};