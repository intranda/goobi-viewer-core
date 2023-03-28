const fs = require("fs")
const XML = require('pixl-xml');
const process = require('process');
const { depsPathsCSS, depsPathsJS }  = require('./grunt/depsPaths');

function getTomcatDir() {
	let homedir = require("os").homedir();
	let rawdata = fs.readFileSync(homedir + '/.config/grunt_userconfig.json');
	let config = JSON.parse(rawdata);
	let os = process.platform
	let xml_string = undefined;
	if(os.toLowerCase().startsWith("win")) {	    
	    xml_string = fs.readFileSync("c:/opt/digiverso/viewer/config/config_viewer.xml", "utf-8");
	} else {
	    xml_string = fs.readFileSync("/opt/digiverso/viewer/config/config_viewer.xml", "utf-8");
	}
	let viewer_config = XML.parse(xml_string);
	 
	if (viewer_config.viewer.theme.specialName && viewer_config.viewer.theme.specialName.length) {
		return config.tomcat_dir + "/goobi-viewer-theme-" + viewer_config.viewer.theme.specialName;
	}
	else {
		return config.tomcat_dir + "/goobi-viewer-theme-" + viewer_config.viewer.theme.mainTheme;
	}
			
}

module.exports = function (grunt) {
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
		theme: {
			name: 'viewer'
		},
		pkg: grunt.file.readJSON('package.json'),
		src: {
			jsDevFolder: 'src/main/resources/META-INF/resources/resources/javascript/dev/',
			jsDevFolderModules: 'src/main/resources/META-INF/resources/resources/javascript/dev/modules/',
			jsDistFolder: 'src/main/resources/META-INF/resources/resources/javascript/dist/',
			jsLibsFolder: 'src/main/resources/META-INF/resources/resources/javascript/libs/',
			cssFolder: 'src/main/resources/META-INF/resources/resources/css/',
			cssDistFolder: 'src/main/resources/META-INF/resources/resources/css/dist/',
			cssLibsFolder: 'src/main/resources/META-INF/resources/resources/css/libs/',
			lessDevFolder: 'src/main/resources/META-INF/resources/resources/css/less/',
		},
		less: {
			dist: {
				options: {
					banner: banner,
					paths: ['<%=src.lessDevFolder%>'],
					plugins: [
						new (require('less-plugin-autoprefix'))({
							browsers: ["last 2 versions"],
							grid: true
						})
					],
					compress: true,
					sourceMap: true,
          			sourceMapURL: '/viewer/resources/css/dist/viewer.min.css.map',
					outputSourceFiles: true,
				},
				files: {
					'<%=src.cssDistFolder %><%=theme.name%>.min.css': '<%=src.lessDevFolder%>constructor.less'
				}
			}
		},
		concat: {
			options: {
				banner: banner,
				separator: '\n',
				stripBanners: true,
				sourceMap: false
			},
			viewer: {
				src: [ 
					'<%=src.jsDevFolderModules %>viewer/viewerJS.js',
					'<%=src.jsDevFolderModules %>viewer/viewerJS.helper.js',
					'<%=src.jsDevFolderModules %>viewer/viewerJS.*.js',
					'<%=src.jsDevFolderModules %>viewer/geoMap/viewerJS.geoMap.js',
					'<%=src.jsDevFolderModules %>viewer/geoMap/*.js',
					'<%=src.jsDevFolderModules %>cms/cmsJS.js',
					'<%=src.jsDevFolderModules %>cms/cmsJS.*.js',
					'<%=src.jsDevFolderModules %>admin/adminJS.js',
					'<%=src.jsDevFolderModules %>admin/adminJS.*.js',
	                '<%=src.jsDevFolderModules %>crowdsourcing/Crowdsourcing.js',
					'<%=src.jsDevFolderModules %>crowdsourcing/Crowdsourcing.Annotation.js',
	                '<%=src.jsDevFolderModules %>crowdsourcing/Crowdsourcing.*.js',

				], 
				dest: '<%=src.jsDistFolder%>viewer.min.js'
			},
			statistics: {
                src: [
                    '<%=src.jsDevFolderModules %>statistics/statistics.js',
                ],
                dest: '<%=src.jsDistFolder%>statistics.min.js'
            },
			browser: {
                src: [
                    '<%=src.jsDevFolderModules %>browsersupport/browsersupport.js',
                ],
                dest: '<%=src.jsDistFolder%>browsersupport.min.js'
            } 
		},
		sync: {
			main: {
        		files: [{
					cwd: 'src/main/resources/META-INF/resources',
					src: ['**'],
					dest: getTomcatDir()
				}],
				verbose: true
			},
		},
		riot: {
			options: {
				concat: true
			},
			dist: {
				expand: false,
				src: '<%=src.jsDevFolder %>tags/**/*.tag',
				dest: '<%=src.jsDistFolder%>riot-tags.js'
			}
		},
		watch: {
			configFiles: {
				files: ['Gruntfile.js'],
				options: {
					reload: true
				}
			},
			less: {
				files: ['<%=src.lessDevFolder%>**/*.less'],
				tasks: ['less', 'sync'],
				options: {
					spawn: false,
				}
			},
			static: {
				files: [
					'src/main/resources/META-INF/resources/*.xhtml',
					'src/main/resources/META-INF/resources/*.xml',
					'src/main/resources/META-INF/resources/*.xls',
					'src/main/resources/META-INF/resources/resources/**/*.xhtml',
					'src/main/resources/META-INF/resources/resources/**/*.html',
					'src/main/resources/META-INF/resources/resources/**/*.jpg',
					'src/main/resources/META-INF/resources/resources/**/*.png',
					'src/main/resources/META-INF/resources/resources/**/*.svg',
					'src/main/resources/META-INF/resources/resources/**/*.gif',
					'src/main/resources/META-INF/resources/resources/**/*.ico',
					'src/main/resources/META-INF/resources/resources/**/*.css',
					'src/main/resources/META-INF/resources/resources/**/*min.js',
				],
				tasks: ['sync'],
				options: {
					spawn: false,
				}
			},
			scripts: {
				files: [
					'<%=src.jsDevFolderModules%>**/*.js'
				],
				tasks: ['concat', 'sync'],
				options: {
					spawn: false,
				}
			},
			riot: {
				files: [
					'<%=src.jsDevFolder %>tags/**/*.tag'
				],
				tasks: ['riot', 'sync'],
				options: {
					spawn: false,
				}
			}
		},
		concurrent: {
			options: {
				logConcurrentOutput: true
			},
			dev: {
				tasks: ['watch:configFiles', 'watch:less', 'watch:scripts', 'watch:riot']
			},
			devsync: {
				tasks: ['watch:configFiles', 'watch:less', 'watch:scripts', 'watch:riot', 'watch:static']
			}
		},
    copy: {
      // copy JS + CSS dependencies from node modules
      // find path definitions here: ./grunt/depsPath.js
      js: {
        files: depsPathsJS
      }, 
      css: {
        files: depsPathsCSS
      }
    }
        
	});

	// ---------- LOAD TASKS ---------
    grunt.loadNpmTasks('grunt-contrib-copy');
	grunt.loadNpmTasks('grunt-contrib-less');
	grunt.loadNpmTasks('grunt-contrib-concat');
	grunt.loadNpmTasks('grunt-riot');
	grunt.loadNpmTasks('grunt-contrib-watch');
	grunt.loadNpmTasks('grunt-sync');
	grunt.loadNpmTasks('grunt-concurrent');
	grunt.loadNpmTasks('grunt-copy-deps');

	// ---------- REGISTER TASKS ----------

	// ----------
    // concurrent:dev task.
	// This runs all tasks without sync.
	// $ grunt dev
	grunt.registerTask('dev', ['concurrent:dev']);
	
	// ----------
    // concurrent:devsync task.
    // This runs all tasks including sync.
	// $ grunt devsync
	grunt.registerTask('devsync', ['concurrent:devsync']);
	
	// ----------
    // build task.
    // This runs all tasks to compile the neccessary CSS and JS files.
	// $ grunt build
	grunt.registerTask('build', ['less', 'concat', 'riot']);
	
	// ----------
    // default task.
    // Default which runs the build task.
	// $ grunt
	grunt.registerTask('default', ['build']);
	
  // Copy dependencies from node_modules/
  grunt.registerTask('copyDeps', ['copy:js', 'copy:css'] )
        
};
