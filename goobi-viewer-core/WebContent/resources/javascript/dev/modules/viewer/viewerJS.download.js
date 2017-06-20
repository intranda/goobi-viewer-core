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
 * Module which manages the download view.
 * 
 * @version 3.2.0
 * @module viewerJS.download
 * @requires jQuery
 * 
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    // default variables
    var _debug = false;
    var _checkbox = null;
    var _downloadBtn = null;
    
    viewer.download = {
        /**
         * Method to initialize the download view.
         * 
         * @method init
         */
        init: function() {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.download.init' );
                console.log( '##############################' );
            }
            _checkbox = $( '#agreeLicense' );
            _downloadBtn = $( '#downloadBtn' );
            
            _downloadBtn.prop( 'disabled', true );
            
            _checkbox.on( 'click', function() {
                var currState = $( this ).prop( 'checked' );
                
                viewer.download.checkboxValidation( currState );
            } );
        },
        /**
         * Method which validates the checkstate of a checkbox and enables the download
         * button.
         * 
         * @method checkboxValidation
         * @param {String} state The current checkstate of the checkbox.
         */
        checkboxValidation: function( state ) {
            if ( _debug ) {
                console.log( '---------- viewer.download.checkboxValidation() ----------' );
                console.log( 'viewer.download.checkboxValidation: state = ', state );
            }
            
            if ( state ) {
                _downloadBtn.prop( 'disabled', false );
            }
            else {
                _downloadBtn.prop( 'disabled', true );
                return false;
            }
        },
    };
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
