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

	grunt.file.setBase('../');
	
	// ---------- PROJECT CONFIG ----------
	grunt.initConfig({
		theme : {
			name : 'viewer'
		},
		pkg : grunt.file.readJSON('package.json'),
		karma: {
		    unit: {
		        configFile: 'karma.conf.js',
	            browsers: ['Firefox']
		    },
		    //continuous integration mode: run tests once in PhantomJS browser.
		    continuous: {
		      configFile: 'karma.conf.js',
		      singleRun: true,
		      browsers: ['Firefox']
		    },
		}
	});
	
	// ---------- LOAD TASKS ----------
	grunt.loadNpmTasks('grunt-karma');

	// ---------- REGISTER DEVELOPMENT TASKS ----------
	grunt.registerTask('default', [ 'karma:continuous' ]);
};