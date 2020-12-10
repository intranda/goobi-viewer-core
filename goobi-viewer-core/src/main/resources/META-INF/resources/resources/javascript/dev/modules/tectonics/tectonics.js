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
 */

var viewerJS = ( function( viewer ) {
    'use strict';

    var _debug = false;
 
    
    viewer.tectonics = {
        init: function( config ) {
            if ( _debug ) { 
                console.log( '##############################' );
                console.log( 'viewer.tectonics.init' );
                console.log( '##############################' );
                console.log( 'viewer.tectonics.init: config - ', config );
            }
            
            viewerJS.jsfAjax.success.subscribe(e => {
                this.setLocation(e.source);
            })

            

        },

        setLocation: function(element) {
            if(_debug)console.log(" clicked data-select-entry", element);
            let select = $(element).attr("data-select-entry");
            let url = window.location.origin + window.location.pathname;
            if(select) {
                url += ("?selected=" + select + "#selected");
            }
            if(_debug)console.log("set url ", url);
            window.history.pushState({}, '', url);
        }
    };


    return viewer;
} )( viewerJS || {}, jQuery );
