<timematrix>

	<div class="timematrix__objects">
		<div each="{image in imageList}" class="timematrix__content">
			<div id="imageMap" class="timematrix__img">
				<a href="{image.url}">
					<img src="{image.mediumimage}" class="timematrix__image" data-viewer-thumbnail="thumbnail" onError="this.onerror=null;this.src='/viewer/resources/images/access_denied.png'" /> 
					<div class="timematrix__text">	
						<p if="{image.title}" name="timetext" class="timetext">{image.title[0]}</p>
					</div>
				</a>
			</div>	
		</div> 
	</div>
	 
	 <script>
	//<![CDATA[		
		 this.on( 'mount', function() {
		 	$(this.opts.button).on("click", this.updateRange);
		 	this.imageList=[];
		 	this.startDate = parseInt($(this.opts.startInput).val());
		 	this.endDate = parseInt($(this.opts.endInput).val());
		 	this.initSlider(this.opts.slider, this.startDate, this.endDate);
		 });
		 
		 updateRange(event){
			this.getTimematrix()
		}
		 getTimematrix(){
		     
		     // build api target
		     var apiTarget = this.opts.contextPath;
		     apiTarget += 'rest/records/timematrix/range/';
		     apiTarget += $(this.opts.startInput).val();
		     apiTarget += "/";
		     apiTarget += $(this.opts.endInput).val();
		     apiTarget += '/';
		     apiTarget += $(this.opts.count).val();
		     apiTarget += '/';
		     
		     if(this.opts.subtheme) {
		         apiTarget += ("?subtheme=" + this.opts.subtheme);
		     }
		     
		     // render thumbnails
		    opts.loading.show()
			let fetchPromise = fetch(apiTarget);
		    fetchPromise.then( function(result) {
			    return result.json(); 
			})
			.then( function(json) {
			    this.imageList=json;
			    this.update()
			    opts.loading.hide()
			}.bind(this));
		 }
		 
		 initSlider(sliderSelector, startDate, endDate) {
		     let $slider = $(sliderSelector);
		  // range slider settings
	            $slider.slider( {
	                range: true,
	                min: parseInt( startDate ),
	                max: parseInt( endDate ),
	                values: [ startDate, endDate ],
	                slide: function( event, ui ) {
	                    $(this.opts.startInput).val( ui.values[ 0 ] ).change();
	                    this.startDate = parseInt(ui.values[ 0 ]);
	                    $(this.opts.endInput).val( ui.values[ 1 ] ).change();
	                    this.endDate = parseInt(ui.values[ 1 ]);
	                }.bind(this)
	            } );
	            
	            // set active slider handle to top19 so key events use this handle
	            $slider.find(".ui-slider-handle").on( 'mousedown', function() {
	                $( '.ui-slider-handle' ).removeClass( 'top' );
	                $( this ).addClass( 'top' );
	            } );
		 }

		//]]>
	</script>
	
</timematrix>