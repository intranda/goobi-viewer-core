<imageFilters>

	<div class="imagefilters__filter-list">
		<div class="imagefilters__filter" each="{filter in filters}">
				<span class="imagefilters__label {filter.config.slider ? '' : 'imagefilters__label-long'}">{filter.config.label}</span>
				<input disabled="{filter.disabled ? 'disabled=' : ''}" class="imagefilters__checkbox"  if="{filter.config.checkbox}" type="checkbox" onChange="{apply}" checked="{filter.isActive() ? 'checked' : '' }" aria-label="{filter.config.label}"/>
				<input disabled="{filter.disabled ? 'disabled=' : ''}" class="imagefilters__slider" title="{filter.getValue()}" if="{filter.config.slider}" type="range" onInput="{apply}" value="{filter.getValue()}" min="{filter.config.min}" max="{filter.config.max}" step="{filter.config.step}" orient="horizontal" aria-label="{filter.config.label}: {filter.getValue()}"/>
		</div>
	</div>
	<div class="imagefilters__options">
		<button type="button" class="btn btn--full" onClick={resetAll}>{this.config.messages.clearAll}</button>
	</div>
	
	<script>
		
		if(!this.opts.image) { 
		    throw "ImageView object must be defined for imageFilters";
		}
	
		var defaultConfig = { 
			filters: {			    
		        brightness : {
				    label: "Brightness",
				    type: ImageView.ImageFilters.Brightness,
				    min: -255,
				    max: 255,
				    step: 1,
				    base: 0,
				    slider: true,
				    checkbox: false,
				    visible: true,
				},
		        contrast : {
				    label: "Contrast",
				    type: ImageView.ImageFilters.Contrast,
				    min: 0,
				    max: 2,
				    step: 0.05,
				    base: 1,
				    slider: true,
				    checkbox: false,
				    visible: true
				},
		        saturate : {
				    label: "Color Saturation",
				    type: ImageView.ImageFilters.ColorSaturation,
				    min: 0,
				    max: 5,
				    step: 0.1,
				    base: 1,
				    slider: true,
				    checkbox: false,
				    visible: true
				},
				hue : {
				    label: "Color rotation",
				    type: ImageView.ImageFilters.ColorRotate,
				    min: -180,
				    max: 180,
				    step: 1,
				    base: 0,
				    slider: true,
				    checkbox: false,
				    visible: true
				},
				threshold : {
				    label: "Bitonal",
				    type: ImageView.ImageFilters.Threshold,
				    min: 0,
				    max: 255,
				    step: 1,
				    base: 128,
				    slider: true,
				    checkbox: true,
				    visible: true,
				    preclude: ["grayscale", "sharpen"]
				},
		        grayscale : {
				    label: "Grayscale",
				    type: ImageView.ImageFilters.Grayscale,
				    slider: false,
				    checkbox: true,
				    visible: true,
				    preclude: ["threshold"]
				},
				invert : {
				    label: "Invert",
				    type: ImageView.ImageFilters.Invert,
				    slider: false,
				    checkbox: true,
				    visible: true
				},
		        blur : {
				    label: "Blur",
				    type: ImageView.ImageFilters.Blur,
				    min: 1,
				    max: 10,
				    step: 1,
				    base: 1,
				    slider: true,
				    checkbox: false,
				    visible: true
				},
		        sharpen : {
				    label: "Sharpen",
				    type: ImageView.ImageFilters.Sharpen,
				    base: 1,
				    slider: false,
				    checkbox: true,
				    visible: true
				},
			},
			messages : {
			    clearAll: "Clear all",
			    apply: "Apply"
			}
		}
		this.config = $.extend(true, {}, defaultConfig, this.opts.config);
		
		this.on("mount", function() {
		    this.filters = this.initFilters(this.config, this.opts.image);
			this.update();
		});
		
		initFilters(filterConfig, image) {
		    let filters = [];
		    for(var key in filterConfig.filters) {
		        let conf = filterConfig.filters[key];
		        if(conf.visible) {
		            let filter = new conf.type(image, conf.base);
		            filter.config = conf;
		            filter.name = key;
		            filters.push(filter);
		        }
		    }
		    return filters;
		}
		
		apply(event) {
		    let filter = event.item.filter;
		    let value = event.target.value;
		    if(filter) {		        
			    if(!filter.isActive()) {
			        filter.start();
			        this.disable(filter.config.preclude);
			    } else if(isNaN(value) ) {
			        filter.close();
			        this.enable(filter.config.preclude);
			    }
			    if(!isNaN(value) ) {			        
			    	filter.setValue(parseFloat(value));
			    	event.target.title = value;
			    }
		    }
		    
		}
		
		disable(filterNames) {
		    if(filterNames) {		        
			    this.filters
			    .filter( filter => filterNames.includes(filter.name) )
			    .forEach( filter => {	
			        filter.disabled = true;
			    })
			    this.update();
		    }
		}
		
		enable(filterNames) {
		    if(filterNames) {		        
			    this.filters
			    .filter( filter => filterNames.includes(filter.name) )
			    .forEach( filter => {		        
			   		filter.disabled = false;
			    })
			    this.update();
		    }
		}
		
		resetAll() {
		   this.filters.forEach( filter => {
		       filter.close();
		       filter.disabled = false;
		       if(filter.config.slider) {		           
		       	filter.setValue(filter.config.base);
		       }
		   })
		   this.update();
		}
		
	</script>

</imageFilters>