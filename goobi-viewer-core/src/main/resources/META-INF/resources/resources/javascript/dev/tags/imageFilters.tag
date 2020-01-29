<imageFilters>

	<div class="image-filters__filter-list">
		<div class="image-filters__options">
			<button type="button" onClick={resetAll}>{this.config.messages.clearAll}</button>
		</div>
		<div class="image-filters__filter" each="{filter in filters}">
			<span class="image-filters__label">{filter.config.label}</span>
<!-- 			<button class="image-filters__toggle">{this.config.messages.apply}</button> -->
			<input if="{filter.config.checkbox}" type="checkbox" onChange="{apply}" checked="{filter.isActive() ? 'checked' : '' }"/>
			<input if="{filter.config.slider}" type="range" onChange="{apply}" value="{filter.getValue()}" min="{filter.config.min}" max="{filter.config.max}" step="{filter.config.step}" orient="horizontal"/>
		</div>
		
	</div>
	
	<script>
		
		if(!this.opts.image) {
		    throw "ImageView object must be defined for imageFilters";
		}
	
		var defaultConfig = {
			filters: {			    
		        brightness : {
				    label: "Brightness",
				    type: ImageView.Tools.Filter.Brightness,
				    min: -255,
				    max: 255,
				    step: 1,
				    base: 0,
				    slider: true,
				    checkbox: false,
				    visible: true
				},
		        contrast : {
				    label: "Contrast",
				    type: ImageView.Tools.Filter.Contrast,
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
				    type: ImageView.Tools.Filter.ColorSaturation,
				    min: 0,
				    max: 5,
				    step: 0.1,
				    base: 1,
				    slider: true,
				    checkbox: false,
				    visible: true
				},
		        grayscale : {
				    label: "Grayscale",
				    type: ImageView.Tools.Filter.Grayscale,
				    slider: false,
				    checkbox: true,
				    visible: true
				},
				threshold : {
				    label: "Bitonal",
				    type: ImageView.Tools.Filter.Threshold,
				    min: 0,
				    max: 255,
				    step: 1,
				    base: 128,
				    slider: true,
				    checkbox: true,
				    visible: true
				},
		        blur : {
				    label: "Blur",
				    type: ImageView.Tools.Filter.Blur,
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
				    type: ImageView.Tools.Filter.Sharpen,
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
		console.log("config ", this.opts.config);
		this.config = $.extend(true, {}, defaultConfig, this.opts.config);
		
		this.on("mount", function() {
		    this.filters = this.initFilters(this.config, this.opts.image);
			console.log("initialized filters ", this.filters);
			this.update();
		});
		
		initFilters(filterConfig, image) {
		    let filters = [];
		    for(var key in filterConfig.filters) {
		        let conf = filterConfig.filters[key];
		        if(conf.visible) {
		            let filter = new conf.type(image, conf.base);
		            filter.config = conf;
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
			    } else if(isNaN(value) ) {
			        filter.close();
			    }
			    if(!isNaN(value) ) {			        
			    	filter.setValue(parseFloat(value));
			    }
		    }
		    
		}
		
		resetAll() {
		   this.filters.forEach( filter => {
		       filter.close();
		       if(filter.config.slider) {		           
		       	filter.setValue(filter.config.base);
		       }
		   })
		   this.update();
		}
		
	</script>

</imageFilters>