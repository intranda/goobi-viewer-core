<timematrix>

	<div class="timematrix__subarea">
        <!-- LOADER -->
		<span class="timematrix__loader" ref="loader">
			<img if="{loading}" src="{opts.contextPath}resources/images/infinity_loader.svg" class="img-fluid" alt="Timematrix Loader" />
		</span> 
	</div>

	<div class="timematrix__selection">
		<div id="locateTimematrix">
			<div class="timematrix__bar">
				<div class="timematrix__period">
					<span>{translate("timematrix__timePeriod")}:</span>&#xA0;

					<input tabindex="0" aria-label="{translate('aria_label__timeline_period_start')}" class="timematrix__selectionRangeInput" ref="inputStartYear" value="{this.startYear}" maxlength="4" />
					&#xA0;<span>-</span>&#xA0; <!-- The spaces created are inserted outside of the span using hexa-code, since the spaces typed in the span caused different spacing in the different themes and the entities in the span had no effect -->
					<input tabindex="0" aria-label="{translate('aria_label__timeline_period_end')}" class="timematrix__selectionRangeInput" ref="inputEndYear" value="{this.endYear}" maxlength="4" />

				</div>
				<div class="timematrix__hitsForm">
					<div class="timematrix__hitsInput">
						<span>{translate("timematrix__maxResults")}: &#xA0;</span>
							<input onChange="{updateHitsPerPage}" type="text" id="hitsPerPage" class="hitsPerPage" name="hitsPerPage" value="{this.maxHits}" placeholder="" maxlength="5" aria-label="{translate('aria_label__timeline_hits')}"/>
					</div>
				</div>
			</div>
			<div id="slider-range" ref="sliderRange"></div>
			<button type="submit" ref="setTimematrix" class="btn btn--full setTimematrix">{translate("timematrix__calculate")}</button>
		</div>
	</div>

	<div class="timematrix__objects">
	<label if="{!loading && manifests.length == 0}">{translate("hitsZero")}</label>
	<div each="{manifest in manifests}" class="timematrix__content">
			<div class="timematrix__img">
			<a href="{getViewerUrl(manifest)}"> <img ref="image" src="{getImageUrl(manifest)}"
				class="timematrix__image" data-viewer-thumbnail="thumbnail"  alt="" aria-hidden="true"
				data-viewer-access-denied-url="{getAccessDeniedThumbnailUrl(manifest)}"
				onLoad="$(this).parents('.timematrix__img').css('background', 'transparent')" />
					<div class="timematrix__text">	
						<p if="{hasTitle(manifest)}" name="timetext" class="timetext">{getDisplayTitle(manifest)}</p>
					</div>
				</a>
			</div>	
		</div> 
	</div>
 
	<script>
		this.manifests = [];
		this.loading = true;
	
		this.on( 'updated', function() {
		    if(this.refs.image) {
		        if(Array.isArray(this.refs.image)) {		            
				    this.refs.image.forEach(ele => {
				        if(!ele.src) {			            
				        	viewerJS.thumbnailLoader.load(ele);
				        }
				    })
		        } else {
		            viewerJS.thumbnailLoader.load(this.refs.image)
		        }
		    }
		});
		
	    this.on( 'mount', function() {
	        
	        let restoredValues = this.restoreValues();
	        if(restoredValues) {
	            this.startYear = restoredValues.startYear;
		        this.endYear = restoredValues.endYear;
		        this.maxHits = restoredValues.maxHits;
	        } else {	            
		        this.startYear = this.opts.minYear;
		        this.endYear = this.opts.maxYear;
		        this.maxHits = this.opts.maxHits;
	        }
	        
	        
	        
	        this.updateTimeMatrix = new rxjs.Subject();
	        
	//         $( this.opts.button ).on( "click", this.updateRange );
	        this.updateTimeMatrix.pipe(
	                rxjs.operators.map( e => this.getIIIFApiUrl()),
	                rxjs.operators.switchMap( url => {
	                    //console.log("fetch ", url);
	                    this.loading = true;
	                    this.update();
	                    return fetch(url);
	                }),
	                rxjs.operators.switchMap( result => {
// 	                    console.log("received result ", result);
	                    return result.json();
	                }),
	                ).subscribe(json => { 
	                    this.manifests = json.orderedItems ? json.orderedItems : [];
	                    //console.log("manifests = ", this.manifests, json);
	                    this.loading = false;
	                    this.update();
	                })
	             
	
	        this.initSlider( this.opts.slider, this.startYear, this.endYear, this.opts.minYear, this.opts.maxYear );
	        this.updateTimeMatrix.next();
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
	    
	    /**
	     * Extracts an optional custom access denied thumbnail URL for the current language from JSON.
	     */
	    getAccessDeniedThumbnailUrl(manifest) {
	    	try {
	    		const uris = manifest.accessDeniedThumbnailUris;
	    		if (!uris) { 
	    		    return null;
	    		}
	    		const uri = uris[this.opts.language];
	    		return uri || null;
	    		} catch (e) {
	    		    console.error("getAccessDeniedThumbnailUrl() failed:", e);
	    		    return null;
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
	        apiTarget += "?start=" + this.startYear;
	        apiTarget += "&end=" + this.endYear;
	        apiTarget += "&rows=" + this.maxHits;
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
	        apiTarget += $( this.maxHits ).val();
	        apiTarget += '/';
	        
	        if ( this.opts.subtheme ) {
	            apiTarget += ( "?subtheme=" + this.opts.subtheme );
	        }
	        
	        return apiTarget;
	    }
	
	    initSlider( sliderSelector, startYear, endYear, minYear, maxYear ) {
	        let $slider = $( this.refs.sliderRange );
	        //console.log("init slider ", this.refs.sliderRange)
	        let rtl = $slider.closest('[dir="rtl"]').length > 0;
	        // range slider settings
	        $slider.slider( {
	            range: true,
	            isRTL: rtl,
	            min: minYear,
	            max: maxYear,
	            values: [ startYear, endYear ],
	            slide: function( event, ui ) {
	                $( this.refs.inputStartYear ).val( ui.values[ 0 ] ).change();
	                this.startYear = parseInt( ui.values[ 0 ] );
	                $( this.refs.inputEndYear ).val( ui.values[ 1 ] ).change();
	                this.endYear = parseInt( ui.values[ 1 ] );
	            }.bind( this ),
	            stop: (event, ui) => {
	                this.updateTimeMatrix.next();
                    this.storeValues();
	            }
	        } );
	        
	        // set active slider handle to top19 so key events use this handle
	        $slider.find( ".ui-slider-handle" ).on( 'mousedown', function() {
	            $( '.ui-slider-handle' ).removeClass( 'top' );
	            $( this ).addClass( 'top' );
	        } );
	    }

	    translate(key) {
	        return this.opts.msg[key];
	    }
	    updateHitsPerPage(event) {
	        this.maxHits = event.target.value;
	        this.storeValues();
	        this.updateTimeMatrix.next();
	    }
	    
	    restoreValues() {
	        let string = sessionStorage.getItem("viewer_timematrix");
	        if(string) {
	            let json = JSON.parse(string);
	            return json;
	        } else {
	            return undefined;
	        }
	    }
	    
	    storeValues() {
	        let json = {startYear: this.startYear, endYear: this.endYear, maxHits: this.maxHits}
	        let string = JSON.stringify(json);
	        sessionStorage.setItem("viewer_timematrix", string);
	    }

	    
	</script> 

</timematrix>
