<timematrix>

	<div class="timematrix__objects">
	<div each="{manifest in manifests}" class="timematrix__content">
			<div class="timematrix__img">
			<a href="{getViewerUrl(manifest)}"> <img src="{getImageUrl(manifest)}"
				class="timematrix__image" data-viewer-thumbnail="thumbnail"  alt="" aria-hidden="true"
				onError="this.onerror=null;this.src='/viewer/resources/images/access_denied.png'" />
					<div class="timematrix__text">	
						<p if="{hasTitle(manifest)}" name="timetext" class="timetext">{getDisplayTitle(manifest)}</p>
					</div>
				</a>
			</div>	
		</div> 
	</div>

	<script>
	    this.on( 'mount', function() {
	//         $( this.opts.button ).on( "click", this.updateRange );
	        rxjs.fromEvent($( this.opts.button ), "click").pipe(
	                rxjs.operators.map( e => this.getIIIFApiUrl()),
	                rxjs.operators.switchMap( url => {
// 	                    console.log("fetch ", url);
	                    this.opts.loading.show();
	                    return fetch(url);
	                }),
	                rxjs.operators.switchMap( result => {
// 	                    console.log("received result ", result);
	                    return result.json();
	                }),
	                ).subscribe(json => { 
	                    this.manifests = json.orderedItems;
	                    console.log("got manifests ", this.manifests);
	                    this.update();
	                    this.opts.loading.hide();
	                })
	             
	
	        this.manifests = [];
	        this.startDate = parseInt( $( this.opts.startInput ).val() );
	        this.endDate = parseInt( $( this.opts.endInput ).val() );
	        this.initSlider( this.opts.slider, this.startDate, this.endDate );
	    } );
	    
	    getViewerUrl(manifest) {
	        let viewer  = manifest.rendering;
	        if(Array.isArray(viewer)) {
	            viewer = viewer.find(r => r.format == "text/html");
	        }
	        if(viewer) {
	            return viewer["@id"];
	        } else {
	            return "";
	        }
	    }
	    
	    getImageUrl(manifest) {
	        if(manifest.thumbnail) {
	            let url = manifest.thumbnail["@id"];
	            return url;
	        }
	    }
	    
	    hasTitle(manifest) {
	        return manifest.label != undefined;
	    }
	    
	    getDisplayTitle(manifest) {
	        return viewerJS.iiif.getValue(manifest.label, this.opts.language, "en");
	    }
	    
	    
	    getIIIFApiUrl() {
	        var apiTarget = this.opts.contextPath;
	        apiTarget += "api/v1/records/list";
	        apiTarget += "?start=" + $( this.opts.startInput ).val();
	        apiTarget += "&end=" + $( this.opts.endInput ).val();
	        apiTarget += "&rows=" + $( this.opts.count ).val();
	        apiTarget += "&sort=RANDOM";
	        if ( this.opts.subtheme ) {
	            apiTarget += ( "&subtheme=" + this.opts.subtheme );
	        }
	        return apiTarget;
	    }
	    
	    getApiUrl() {
	        // build api target
	        var apiTarget = this.opts.contextPath;
	        apiTarget += 'rest/records/timematrix/range/';
	        apiTarget += $( this.opts.startInput ).val();
	        apiTarget += "/";
	        apiTarget += $( this.opts.endInput ).val();
	        apiTarget += '/';
	        apiTarget += $( this.opts.count ).val();
	        apiTarget += '/';
	        
	        if ( this.opts.subtheme ) {
	            apiTarget += ( "?subtheme=" + this.opts.subtheme );
	        }
	        
	        return apiTarget;
	    }
	
	    initSlider( sliderSelector, startDate, endDate ) {
	        let $slider = $( sliderSelector );
	        // range slider settings
	        $slider.slider( {
	            range: true,
	            min: parseInt( startDate ),
	            max: parseInt( endDate ),
	            values: [ startDate, endDate ],
	            slide: function( event, ui ) {
	                $( this.opts.startInput ).val( ui.values[ 0 ] ).change();
	                this.startDate = parseInt( ui.values[ 0 ] );
	                $( this.opts.endInput ).val( ui.values[ 1 ] ).change();
	                this.endDate = parseInt( ui.values[ 1 ] );
	            }.bind( this )
	        } );
	        
	        // set active slider handle to top19 so key events use this handle
	        $slider.find( ".ui-slider-handle" ).on( 'mousedown', function() {
	            $( '.ui-slider-handle' ).removeClass( 'top' );
	            $( this ).addClass( 'top' );
	        } );
	    }
	
	</script> 

</timematrix>
