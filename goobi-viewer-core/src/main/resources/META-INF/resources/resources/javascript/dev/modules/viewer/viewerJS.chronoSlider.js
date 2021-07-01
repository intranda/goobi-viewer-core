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
    
    var _debug = false;
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
    	/**
    	 * Method to initialize the chronology slider.
    	 * 
    	 * @method init
    	 * @param {Object} config An config object which overwrites the defaults.
    	 * @param {Array} config.yearList An Array of all possible years.
    	 * @param {Number} config.startValue The value of the first year.
    	 * @param {Number} config.endValue The value of the last year.
    	 * @param {String} config.currentMinRangeValue The lower range value.
    	 * @param {String} config.currentMaxRangeValue The higher range value.
    	 */
    	init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.chronoSlider.init' );
                console.log( '##############################' );
                console.log( 'viewer.chronoSlider.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );
            
            let rtl = $( "#chronoSlider" ).closest('[dir="rtl"]').length > 0;
            
            var sliderHandle1 = $('.ui-slider-handle').first();
            var sliderHandle2 = $('.ui-slider-handle').last();
            
            $( "#chronoSlider" ).slider({
        		range: true,
        		isRTL: rtl,
				min: 0,
        		max: _defaults.yearList.length - 1,
				values: [ _defaults.yearList.indexOf( _defaults.currentMinRangeValue ), _defaults.yearList.indexOf( _defaults.currentMaxRangeValue) ],
        		slide: function( event, ui ) {
        			
        			$( '.chronology-slider-start input' ).val( _defaults.yearList[ ui.values[ 0 ] ] );
        			$( '.chronology-slider-end input' ).val( _defaults.yearList[ ui.values[ 1 ] ] );

        			if (rtl) {
        				// RTL
        				// method used here for handling the RTL position is not pixel-perfect
						// set handler position
						if ( ui.values[ 0 ] == ui.values[ 1 ] ) {
			        		$( "#chronoSlider .ui-slider-handle" ).first().css('margin-right', '0px');
			        		$( "#chronoSlider .ui-slider-handle" ).last().css('margin-left', '-10px');
			        		console.log('uebereinander');
			        	}	else {
								$( "#chronoSlider .ui-slider-handle" ).last().css('margin-left', '0px');
							}

        				$( "#chronoSlider .ui-slider-handle" ).first().css('margin-left', '-10px');


        				// NOT WORKING YET
//        				$( "#chronoSlider .ui-slider-handle" ).first().css('margin-left', 1 * $( "#chronoSlider .ui-slider-handle" ).last().width() * ($( "#chronoSlider" ).slider( "values", 1 ) / $( "#chronoSlider" ).slider('option', 'max')));
//        				$( "#chronoSlider .ui-slider-handle" ).last().css('margin-left', 1 * $( "#chronoSlider .ui-slider-handle" ).first().width() * ($( "#chronoSlider" ).slider( "values", 0 ) / $( "#chronoSlider" ).slider('option', 'max')));
        			}
        			else {
        				// LTR
        				// working JS calculation method for handling slider handle positions (jQuery slider)
	        			// set handler position
        				$( "#chronoSlider .ui-slider-handle" ).last().css('margin-left', -1 * $( "#chronoSlider .ui-slider-handle" ).last().width() * ($( "#chronoSlider" ).slider( "values", 1 ) / $( "#chronoSlider" ).slider('option', 'max')));
        				$( "#chronoSlider .ui-slider-handle" ).first().css('margin-left', -1 * $( "#chronoSlider .ui-slider-handle" ).first().width() * ($( "#chronoSlider" ).slider( "values", 0 ) / $( "#chronoSlider" ).slider('option', 'max')));
        			}

        		},
        		change: function( event, ui ) {
        			var startDate = parseInt( $( '.chronology-slider-start input' ).val() );
        			var endDate = parseInt( $( '.chronology-slider-end input' ).val() );
        			
        			startDate =  _defaults.yearList[ui.values[0]];
        			endDate =  _defaults.yearList[ui.values[1]];
//        			console.log("move to ", startDate, endDate);
        			
        			if(endDate >= startDate) {        			    
        			    // show loader
        			    $( '.chronology-slider-action-loader' ).addClass( 'active' );
        			    
        			    // set query to hidden input
        			    let value = '[' + startDate + ' TO ' + endDate + ']' ;
        			    $( '[id*="chronologySliderInput"]' ).val(value);
//        			   console.log("set slider value ", value)
        			    // submit form
        			    $( '[id*="chronologySliderSet' ).click();
        			}
        		},
        	});
            $( '.chronology-slider-start input' ).on("change", (event) => {
//                console.log("change event ", event);
                let value = parseInt(event.target.value);
                if(!isNaN(value)) {                    
                    let yearIndex = _getClosestYearIndexAbove(value, _defaults.yearList);
//                    console.log("changed start ", value, yearIndex);
                    $( "#chronoSlider" ).slider( "values", 0, yearIndex );
                }
            })
            $( '.chronology-slider-end input' ).on("change", (event) => {
                let value = parseInt(event.target.value);
                if(!isNaN(value)) {                    
                    let yearIndex = _getClosestYearIndexBelow(value, _defaults.yearList);
//                    console.log("changed end ", value, yearIndex);
                    $( "#chronoSlider" ).slider( "values", 1, yearIndex );
                }
            })
            
            // set handler position
        	_firstHandlePos = parseInt( $( "#chronoSlider .ui-slider-handle:first" ).css('left') );
        	_lastHandlePos = parseInt( $( "#chronoSlider .ui-slider-handle:last" ).css('left') );
        	
			if (rtl) {
				
	        	$( "#chronoSlider .ui-slider-handle" ).first().css('margin-left', '-10px');
	        	$( "#chronoSlider .ui-slider-handle" ).last().css('margin-left', '0px');
	        	
	        	if ( _firstHandlePos == _lastHandlePos ) {
	        		$( "#chronoSlider .ui-slider-handle" ).last().css('margin-left', '-10px');	
	        	}
	        	
	        	// NOT PERFECTLY WORKING YET
	        	// $( "#chronoSlider .ui-slider-handle" ).first().css('margin-left', 1 * $( "#chronoSlider .ui-slider-handle" ).last().width() * ($( "#chronoSlider" ).slider( "values", 1 ) / $( "#chronoSlider" ).slider('option', 'max')));
				// $( "#chronoSlider .ui-slider-handle" ).last().css('margin-left', 1 * $( "#chronoSlider .ui-slider-handle" ).first().width() * ($( "#chronoSlider" ).slider( "values", 0 ) / $( "#chronoSlider" ).slider('option', 'max')));
			}
			else {
				$( "#chronoSlider .ui-slider-handle" ).last().css('margin-left', -1 * $( "#chronoSlider .ui-slider-handle" ).last().width() * ($( "#chronoSlider" ).slider( "values", 1 ) / $( "#chronoSlider" ).slider('option', 'max')));
				$( "#chronoSlider .ui-slider-handle" ).first().css('margin-left', -1 * $( "#chronoSlider .ui-slider-handle" ).first().width() * ($( "#chronoSlider" ).slider( "values", 0 ) / $( "#chronoSlider" ).slider('option', 'max')));
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
    
    function _getClosestYearIndexAbove(value, years) {
        for (var i = 0; i < years.length; i++) {
            let year = years[i];
            if(year >= value) {
                return i;
            }
        }
        return years.length-1;
    }
    
    function _getClosestYearIndexBelow(value, years) {
        for (var i = years.length; i > -1 ; i--) {
            let year = years[i];
            if(year <= value) {
                return i;
            }
        }
        return 0;
    }
    
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
		
//		$( '[id*="chronologySliderInput"]' ).val( '[' + years[ 0 ] + ' TO ' + years[years.length - 1] + ']' );
//		$( '[id*="chronologySliderForm"] input[type="submit"]' ).click();
		$( '[id*="chronologySliderReset"]' ).click();
	}  

    return viewer;
    
} )( viewerJS || {}, jQuery );
