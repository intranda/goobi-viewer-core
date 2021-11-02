/**
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 * 
 * Visit these websites for more information. - http://www.intranda.com -
 * http://digiverso.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 * @description Contains functions for increased accessibility
 * @version 3.2.0
 * @module viewerJS.accessibility
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    // Default variables
    var _debug = false;
    
    viewer.accessibility = {
      // Detect if a user uses the tab key to navigate
      // And add/remove a class to the body accordingly
      detectKeyboardUsage: function() {
        // Keyboard is used
        document.body.addEventListener('keydown', function(e) {
          // Check if tab key was pressed 
          if (e.keyCode === 9 || e.key == 'Tab') {
            document.body.classList.add('using-keyboard');
          }
        });
        // Mouse is used
        document.body.addEventListener('mousedown', function() {
          document.body.classList.remove('using-keyboard');
        });
      },

	  // Jump to footer if link or button in footer is focused with keyboard (tab)
      jumpToFooter: function() {

		if( $('#pageFooter').length ) {
			$(document).on('keydown',function(e) {
			    if(e.which == 9) {
				setTimeout(function(){ 
			        if ($("#pageFooter a").is(":focus") || $("#pageFooter button").is(":focus")) {
					    window.scrollTo(0,document.body.scrollHeight);
					}
					}, 20);
	
			    }
			});
		}
      },

      init: function() {
        if(_debug) console.log('init `viewerJS.accessibility`');
        this.detectKeyboardUsage();
		this.jumpToFooter();
      },

    };     

    return viewer;
} )( viewerJS );

