<chronoSlider>

<div class="widget-chronology-slider__body widget__body">
	<!-- START/END YEAR -->
	<div class="widget-chronology-slider__item chronology-slider-start">
		<input ref="inputStart" data-input='number'
			class="widget-chronology-slider__item-input -no-outline -active-border"
			value="{startYear}" title="{msg.enterYear}" data-toggle="tooltip"
			data-placement="top" aria-label="{msg.enterYear}"></input>
	</div>
	<div class="widget-chronology-slider__item chronology-slider-end">
		<input ref="inputEnd" data-input='number'
			class="widget-chronology-slider__item-input -no-outline -active-border"
			value="{endYear}" title="{msg.enterYear}" data-toggle="tooltip"
			data-placement="top" aria-label="{msg.enterYear}"></input>
	</div>

	<!-- RANGE SLIDER -->
	<div class="widget-chronology-slider__item chronology-slider">
		<div ref="slider"></div>
	</div>
</div>

<script>

this.msg={}
this.on("mount", () => {
	console.log("init chrono slider with ", opts);
	this.startYear = parseInt(opts.startYear);
	this.endYear = parseInt(opts.endYear);
	this.yearList = JSON.parse(opts.yearList);
	this.minYear = this.yearList[0];
	this.maxYear = this.yearList[this.yearList.length - 1];
	this.valueInput = document.getElementById(opts.valueInput);
	this.updateFacet = document.getElementById(opts.updateFacet);
	this.removeFacet = document.getElementById(opts.removeFacet);
	this.loader = document.getElementById(opts.loader);	
	this.msg = opts.msg;
	this.rtl = $( this.refs.slider ).closest('[dir="rtl"]').length > 0;
	this.update();
});

this.on("updated", () => {
	this.initSlider();
	this.initChangeEvents();
	this.setHandlePositions();
	this.initSliderReset();
});


initSliderReset() {
	if ( this.startYear > this.yearList[ 0 ] || this.endYear < this.yearList[ this.yearList.length - 1 ] ) {
		$( '.chronology-slider-action-reset' ).addClass( 'active' );
		$( '[data-reset="chrono-slider"]' ).on( 'click', function() {
    		this.resetChronoSlider( this.yearList );
    	} );
	}
}

setHandlePositions() {
	// set handler position
	let firstHandlePos = parseInt( $(this.refs.slider).find(".ui-slider-handle:first" ).css('left') );
	let lastHandlePos = parseInt( $(this.refs.slider).find(".ui-slider-handle:last" ).css('left') );
	
	console.log("set handle positions ", firstHandlePos, lastHandlePos);
	if (this.rtl) {
		
		$(this.refs.slider).find(".ui-slider-handle" ).first().css('margin-left', '-10px');
		$(this.refs.slider).find(".ui-slider-handle" ).last().css('margin-left', '0px');
    	
    	if ( firstHandlePos == lastHandlePos ) {
    		$(this.refs.slider).find(".ui-slider-handle" ).last().css('margin-left', '-10px');	
    	}
    	
    	// NOT PERFECTLY WORKING YET
    	// $( "#chronoSlider .ui-slider-handle" ).first().css('margin-left', 1 * $( "#chronoSlider .ui-slider-handle" ).last().width() * ($( "#chronoSlider" ).slider( "values", 1 ) / $( "#chronoSlider" ).slider('option', 'max')));
		// $( "#chronoSlider .ui-slider-handle" ).last().css('margin-left', 1 * $( "#chronoSlider .ui-slider-handle" ).first().width() * ($( "#chronoSlider" ).slider( "values", 0 ) / $( "#chronoSlider" ).slider('option', 'max')));
	} else {
		$(this.refs.slider).find(".ui-slider-handle" ).last().css('margin-left', -1 * $(this.refs.slider).find(".ui-slider-handle" ).last().width() * ($(this.refs.slider).slider( "values", 1 ) / $(this.refs.slider).slider('option', 'max')));
		$(this.refs.slider).find(".ui-slider-handle" ).first().css('margin-left', -1 * $(this.refs.slider).find(".ui-slider-handle" ).first().width() * ($(this.refs.slider).slider( "values", 0 ) / $(this.refs.slider).slider('option', 'max')));
	}
}

initChangeEvents() {
	$(this.refs.slider).find( '.chronology-slider-start input' ).on("change", (event) => {
//      console.log("change event ", event);
      let value = parseInt(event.target.value);
      if(!isNaN(value)) {                    
          let yearIndex = this.getClosestYearIndexAbove(value, this.yearList);
//          console.log("changed start ", value, yearIndex);
          $(this.refs.slider).slider( "values", 0, yearIndex );
      }
  })
  $(this.refs.slider).find( '.chronology-slider-end input' ).on("change", (event) => {
      let value = parseInt(event.target.value);
      if(!isNaN(value)) {                    
          let yearIndex = this.getClosestYearIndexBelow(value, this.yearList);
//          console.log("changed end ", value, yearIndex);
          $(this.refs.slider).slider( "values", 1, yearIndex );
      }
  })
}

initSlider() {
	console.log("init slider", this);
	let options = {
			range: true,
			isRTL: this.rtl,
			min: this.minYear,
			max: this.maxYear,
			values: [ this.yearList.indexOf( this.startYear ), this.yearList.indexOf( this.endYear ) ],
			slide: ( event, ui ) => {
				
				$( this.refs.inputStart ).val( this.yearList[ ui.values[ 0 ] ] );
				$( this.refs.inputEnd ).val( this.yearList[ ui.values[ 1 ] ] );

				if (this.rtl) {
					// RTL
					// method used here for handling the RTL position is not pixel-perfect
					// set handler position
					if ( ui.values[ 0 ] == ui.values[ 1 ] ) {
		        		$(this.refs.slider).find( ".ui-slider-handle" ).first().css('margin-right', '0px');
		        		$(this.refs.slider).find( ".ui-slider-handle" ).last().css('margin-left', '-10px');
		        	}	else {
		        		$(this.refs.slider).find( ".ui-slider-handle" ).last().css('margin-left', '0px');
					}

					$(this.refs.slider).find( ".ui-slider-handle" ).first().css('margin-left', '-10px');


					// NOT WORKING YET
//					$( "#chronoSlider .ui-slider-handle" ).first().css('margin-left', 1 * $( "#chronoSlider .ui-slider-handle" ).last().width() * ($( "#chronoSlider" ).slider( "values", 1 ) / $( "#chronoSlider" ).slider('option', 'max')));
//					$( "#chronoSlider .ui-slider-handle" ).last().css('margin-left', 1 * $( "#chronoSlider .ui-slider-handle" ).first().width() * ($( "#chronoSlider" ).slider( "values", 0 ) / $( "#chronoSlider" ).slider('option', 'max')));
				}
				else {
					// LTR
					// working JS calculation method for handling slider handle positions (jQuery slider)
	    			// set handler position
	    			//console.log("setup right handle", this.$getLastHandle(), this.$getLastHandle().width(), this.$getSlider().slider( "values", 1 ), this.$getSlider().slider('option', 'max'));
	    			this.$getLastHandle().css('margin-left', -1 * this.$getLastHandle().width() * ( this.$getSlider().slider( "values", 1 ) / this.$getSlider().slider('option', 'max')));
	    			this.$getFirstHandle().css('margin-left', -1 * this.$getFirstHandle().width() * ( this.$getSlider().slider( "values", 0 ) / this.$getSlider().slider('option', 'max')));
				}

			},
			change: ( event, ui ) => {
				var startDate = parseInt( $( this.refs.inputStart ).val() );
				var endDate = parseInt( $( this.refs.inputEnd ).val() );
				
				startDate =  this.yearList[ui.values[0]];
				endDate =  this.yearList[ui.values[1]];
				//console.log("move to ", startDate, endDate);
				
				if(endDate >= startDate) {        			    
				    // show loader
				    $( this.loader ).addClass( 'active' );
				    
				    // set query to hidden input
				    let value = '[' + startDate + ' TO ' + endDate + ']' ;
				    $( this.valueInput ).val(value);
				    console.log("set slider value ", this, this.valueInput, value)
				    // submit form
				    $( this.updateFacet ).click();
				    console.log("submit", $( this.updateFacet ));
				}
			},
		}
	console.log("slider options ", options)
	
    $( this.refs.slider ).slider(options);
}

getClosestYearIndexAbove(value, years) {
    for (var i = 0; i < years.length; i++) {
        let year = years[i];
        if(year >= value) {
            return i;
        }
    }
    return years.length-1;
}

getClosestYearIndexBelow(value, years) {
    for (var i = years.length; i > -1 ; i--) {
        let year = years[i];
        if(year <= value) {
            return i;
        }
    }
    return 0;
}

resetChronoSlider( years ) {
	
	
	$(this.refs.slider).slider( {
		min: 0,
		max: years.length - 1
	} );
	
//	$( '[id*="chronologySliderInput"]' ).val( '[' + years[ 0 ] + ' TO ' + years[years.length - 1] + ']' );
//	$( '[id*="chronologySliderForm"] input[type="submit"]' ).click();
	$( this.removeFacet ).click();
}  

$getFirstHandle() {
	return $(this.refs.slider).find( ".ui-slider-handle" ).first();	
}

$getLastHandle() {
	return $(this.refs.slider).find( ".ui-slider-handle" ).last();	
}

$getSlider() {
	return $(this.refs.slider);
}


</script> 

</chronoSlider>