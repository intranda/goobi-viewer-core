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
 * @version 3.4.0
 * @module viewerJS.widgetToc
 * @description Module to steer widget toc functionality.
 * @requires jQuery
 */
var viewerJS = (function(viewer) {
	'use strict';

	var _debug = false;
	var _parentPos = 0;

	viewer.widgetToc = {
		/**
		 * @method init
		 * @description Initializes the widget toc module.
		 */
		init : function() {
			if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.widgetToc.init' );
                console.log( '##############################' );
            }
			
			// hide loader and overlay after successful ajax request
	         viewerJS.jsfAjax.begin.subscribe(e => {
	         	var widgetToc = $(e.source).parents('#widgetToc');
	         	if (widgetToc.length) {
            		$( '[data-iddoc*="iddoc"]' ).removeClass( 'active' );
            		$( '.widget-toc__loader, .widget-toc__overlay' ).show();
            		
            		// hide all tooltips on ajax load
            		$('[data-toggle="tooltip"]').tooltip('dispose');
        		}
	         });
	         
	         viewerJS.jsfAjax.begin.subscribe(e => {
	         	var iddoc = $( e.source ).parents( '.widget-toc__element' ).attr( 'data-iddoc' );
	         	if ( iddoc !== undefined ) {
        			_parentPos = $( '[data-iddoc="' + iddoc + '"]' ).position().top;
        			$( '[data-iddoc="' + iddoc + '"]' ).addClass( 'active' );
        			$( '.widget-toc__elements' ).scrollTop( _parentPos );	                			
        		}
                $( '.widget-toc__loader, .widget-toc__overlay' ).hide();
	         });

		}
	};

	return viewer;

})(viewerJS || {}, jQuery);