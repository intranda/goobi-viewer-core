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
	this.startYear = opts.startYear;
	this.endYear = opts.endYear;
	this.yearList = JSON.parse(opts.yearList).sort();
	this.minYear = this.yearList[0];
	this.maxYear = this.yearList[this.yearList.length];
	this.valueInput = document.getElementById(opts.valueInput);
	this.updateFacet = document.getElementById(opts.updateFacet);
	this.removeFacet = document.getElementById(opts.removeFacet);
	this.loader = document.getElementById(opts.loader);
	this.msg = opts.msg;
	this.rtl = $( this.slider ).closest('[dir="rtl"]').length > 0;
	this.initSlider();
	this.update();
});

initSlider() {
	
	let options = {
			range: true,
			isRTL: this.rtl,
			min: this.minYear,
			max: this.maxYear,
			values: [ this.yearList.indexOf( this.startYear ), this.yearList.indexOf( this.endYear ) ],
			slide: function( event, ui ) {
				
				$( this.refs.inputStart ).val( this.yearList[ ui.values[ 0 ] ] );
				$( this.refs.inputStart ).val( this.yearList[ ui.values[ 1 ] ] );

				if (rtl) {
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
					$(this.refs.slider).find( ".ui-slider-handle" ).last().css('margin-left', -1 * $(this.refs.slider).find( ".ui-slider-handle" ).last().width() * ($( this.refs.slider ).slider( "values", 1 ) / $( this.refs.slider ).slider('option', 'max')));
					$(this.refs.slider).find( ".ui-slider-handle" ).first().css('margin-left', -1 * $(this.refs.slider).find( ".ui-slider-handle" ).first().width() * ($( this.refs.slider ).slider( "values", 0 ) / $( this.refs.slider ).slider('option', 'max')));
				}

			},
			change: function( event, ui ) {
				var startDate = parseInt( $( this.refs.inputStart ).val() );
				var endDate = parseInt( $( this.refs.inputEnd ).val() );
				
				startDate =  this.yearList[ui.values[0]];
				endDate =  this.yearList[ui.values[1]];
//				console.log("move to ", startDate, endDate);
				
				if(endDate >= startDate) {        			    
				    // show loader
				    $( this.loader ).addClass( 'active' );
				    
				    // set query to hidden input
				    let value = '[' + startDate + ' TO ' + endDate + ']' ;
				    $( this.valueInput ).val(value);
//				   console.log("set slider value ", value)
				    // submit form
				    $( this.updateFacet ).click();
				}
			},
		}
	console.log("slider options ", options)
	
    $( this.refs.slider ).slider(options);
}



</script> 

</chronoSlider>