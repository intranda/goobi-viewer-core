module.exports = function(grunt) {
    grunt.initConfig({
        theme: {
            name: 'viewer'
        },
        pkg: grunt.file.readJSON('package.json'),
        src: {
            jsDevFolder: 'WebContent/resources/javascript/dev/modules/',
            jsDistFolder: 'WebContent/resources/javascript/dist/',
            jsDocFolder: 'doc/jsdoc/',
            cssDevFolder: 'WebContent/resources/css/dev/',
            cssDistFolder: 'WebContent/resources/css/dist/',
            lessDevFolder: 'WebContent/resources/css/less/viewer/'
        },
        less: {
            development: {
                options: {
                    paths: [ '<%=src.lessDevFolder%>' ],
                    compress: false,
                    optimization: 9
                },
                files: {
                    '<%=src.cssDevFolder %><%=theme.name%>.css': '<%=src.lessDevFolder%>constructor.less'
                }
            },
            production: {
                options: {
                    paths: [ '<%=src.lessDevFolder%>' ],
                    compress: true,
                    sourceMap: true,
                    banner: '/*!\n'
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
                files: {
                    '<%=src.cssDistFolder %><%=theme.name%>.min.css': '<%=src.lessDevFolder%>constructor.less'
                }
            }
        },
        concat: {
            options: {
                separator: '\n',
                stripBanners: true,
                banner: '/*!\n'
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
            distViewer: {
                src: [ 
                    '<%=src.jsDevFolder %>viewer/viewerJS.js', 
                    '<%=src.jsDevFolder %>viewer/viewerJS.*.js',
                    '<%=src.jsDevFolder %>cms/cmsJS.js',
                    '<%=src.jsDevFolder %>cms/cmsJS.*.js'
                ],
                dest: '<%=src.jsDevFolder %>viewer.js'     
            },
            distViewImage: {
                src: [ 
                    '<%=src.jsDevFolder %>viewImage/viewImage.js',
                    '<%=src.jsDevFolder %>viewImage/viewImage.controls.js',
                    '<%=src.jsDevFolder %>viewImage/viewImage.*.js'
                ],
                dest: '<%=src.jsDevFolder %>viewImage.js'     
            },
        },
        uglify: {
            options: {
                banner: '/*!\n'
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
            uglifyViewer: {
                files: {
                    '<%=src.jsDistFolder %>viewer.min.js': ['<%=src.jsDevFolder %><%=theme.name%>.js'],
                    '<%=src.jsDistFolder %>viewImage.min.js': ['<%=src.jsDevFolder %>viewImage.js']
                },
            }
        },
        watch: {
            styles: {
                files: [ '<%=src.lessDevFolder%>**/*.less' ],
                tasks: [ 'less' ],
                options: {
                    nospawn: true
                }
            },
            js: {
                files: [ '<%=src.jsDevFolder %>**/*.js' ],
                tasks: [ 'concat', 'uglify' ],
                options: {
                    nospawn: true
                }
            }
        },
        jsdoc : {
            dist : {
                src: [ '<%=src.jsDevFolder %>**/*.js' ],
                options: {
                    destination: '<%=src.jsDocFolder %>',
                    template : "node_modules/ink-docstrap/template",
                    configure : "node_modules/ink-docstrap/template/jsdoc.conf.json"
                }
            }
        }
    });
    
    // load tasks
    grunt.loadNpmTasks('grunt-contrib-less');
    grunt.loadNpmTasks('grunt-contrib-concat');
    grunt.loadNpmTasks('grunt-contrib-uglify');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-jsdoc');
    
    // register tasks
    grunt.registerTask( 'default', [ 'jsdoc', 'watch' ] );
};