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
 * <Short Module Description>
 * 
 * @version 3.4.0
 * @module viewerJS.chronoSlider
 * @requires jQuery, jQuery UI
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = true;
    var _firstHandlePos;
    var _lastHandlePos;
    var _defaults = {
    	yearList: [],            		
        startValue: 0,
        endValue: 0,
        currentMinRangeValue: '',
        currentMaxRangeValue: '',
    };
    
    viewer.chronoSlider = {
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.chronoSlider.init' );
                console.log( '##############################' );
                console.log( 'viewer.chronoSlider.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );
            
            $( "#chronoSlider" ).slider({
        		range: true,
				min: 0,
        		max: _defaults.yearList.length - 1,
				values: [ _defaults.yearList.indexOf( _defaults.currentMinRangeValue ), _defaults.yearList.indexOf( _defaults.currentMaxRangeValue) ],
        		slide: function( event, ui ) {
        			$( '.chronology-slider-start' ).html( _defaults.yearList[ ui.values[ 0 ] ] );
        			$( '.chronology-slider-end' ).html( _defaults.yearList[ ui.values[ 1 ] ] );

        			// set handler position
        			if ( ui.values[ 0 ] == ui.values[ 1 ] ) {
                		$( "#chronoSlider .ui-slider-handle:first" ).css('margin-right', '-10px');
                		$( "#chronoSlider .ui-slider-handle:last" ).css('margin-left', '0');
                	}
        			else {
        				$( "#chronoSlider .ui-slider-handle:last" ).css('margin-left', '-10px');
        			}
        		},
        		stop: function( event, ui ) {
        			var startDate = parseInt( $( '.chronology-slider-start' ).text() );
        			var endDate = parseInt( $( '.chronology-slider-end' ).text() );
        			
        			// show loader
        			$( '.chronology-slider-action-loader' ).addClass( 'active' );
        			
        			// set query to hidden input
        			$( '[id*="chronologySliderInput"]' ).val( '[' + startDate + ' TO ' + endDate + ']' );
        			
        			// submit form
        			$( '[id*="chronologySliderForm"] input[type="submit"]' ).click();
        		},
        	});
            
            // set handler position
        	_firstHandlePos = parseInt( $( "#chronoSlider .ui-slider-handle:first" ).css('left') );
        	_lastHandlePos = parseInt( $( "#chronoSlider .ui-slider-handle:last" ).css('left') );
        	
        	$( "#chronoSlider .ui-slider-handle:last" ).css('margin-left', '-10px');
        	
        	if ( _firstHandlePos == _lastHandlePos ) {
        		$( "#chronoSlider .ui-slider-handle:last" ).css('margin-left', '0');	
        	}
        	
        	// reset slider
        	if ( _defaults.startValue > _defaults.yearList[ 0 ] || _defaults.endValue < _defaults.yearList[ _defaults.yearList.length - 1 ] ) {
        		$( '.chronology-slider-action-reset' ).addClass( 'active' );
        		$( '[data-reset="chrono-slider"]' ).on( 'click', function() {
            		_resetChronoSlider( _defaults.yearList );
            	} );
        	} 
        }
    };
    
    /**
     * Method to reset the chronology slider.
     * 
     * @method _resetChronoSlider
     * @param {Array} years An Array of all possible years.
     * */
    function _resetChronoSlider( years ) {
    	if ( _debug ) {
            console.log( '---------- _resetChronoSlider() ----------' );
            console.log( '_resetChronoSlider: years = ', years );
        }
    	
		var $slider = $( '#chronoSlider' );
		
		$slider.slider( {
			min: 0,
			max: years.length - 1
		} );
		
		$( '[id*="chronologySliderInput"]' ).val( '[' + years[ 0 ] + ' TO ' + years[years.length - 1] + ']' );
		$( '[id*="chronologySliderForm"] input[type="submit"]' ).click();
	}
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
