

<slider>

<div ref="container" class="swiper-container slider-{this.styleName}__container">
	<div class="swiper-wrapper slider-{this.styleName}__wrapper">
		<div each="{slide in slides}" class="swiper-slide slider-{this.styleName}__slide">
			<a class="slider-{this.styleName}__link" href="{getLink(slide)}" target="{this.linkTarget}">
				<h3 class="slider-{this.styleName}__header">{translate(slide.header)}</h3>
				<div class="slider-{this.styleName}__image" style="background-image: url({getImage(slide)})">
				</div>
				<div class="slider-{this.styleName}__description">{translate(slide.description)}</div>
			</a>
		</div>
	</div>
	<div if="{this.showPaginator}" ref="paginator" class="swiper-pagination slider-{this.styleName}__dots"></div>
</div>

<script>

	//initially show paginator so it can be referenced when amending style config (see this.amendStyle())
	this.showPaginator = true;

    this.on( 'mount', function() {
    	console.log("mounting 'slider.tag' ", this.opts);
		this.style = this.opts.styles.get(this.opts.style);
		this.amendStyle(this.style);
		this.styleName = this.opts.styles.getStyleNameOrDefault(this.opts.style);
    	console.log("init slider with '" + this.opts.style + "''", this.style);
		this.timeout = this.style.timeout ? this.style.timeout : 100000;
		this.maxSlides = this.style.maxSlides ? this.style.maxSlides : 1000;
		this.linkTarget = this.opts.linktarget ? this.opts.linktarget : "_self";
    	
    	let pSource;
    	if(this.opts.sourceelement) {
    		let sourceElement = document.getElementById(this.opts.sourceelement);
    		if(sourceElement) {    			
    			pSource = Promise.resolve(JSON.parse(sourceElement.textContent));
//     			console.log("getting source from ", sourceElement.textContent);
    		} else {
    			logger.error("sourceElement was included but no matching dom element found");
    			return;
    		}
    	}  else {
    		pSource = fetch(this.opts.source)
        	.then(result => result.json());
    	}
    	rxjs.from(pSource)
    	.pipe(
    		rxjs.operators.flatMap(source => source),					//split array into sub-observables
    		rxjs.operators.flatMap(uri => fetch(uri), undefined, 5),	//call urls in list, maximal 5 at a time
    		rxjs.operators.filter(result => result.status == 200),		//drop all failed requests
    		rxjs.operators.takeUntil(rxjs.timer(this.timeout)),			//stop waiting for responses after timeout expires
    		rxjs.operators.flatMap(result => result.json()),			//get json object from response
    		rxjs.operators.map(element => this.createSlide(element)),	//create slide object from response
    		rxjs.operators.filter(element => element != undefined),		//drop all responses which could not be mapped to a slide
    		rxjs.operators.take(this.maxSlides),						//stop after maxSlides slides have been created
    		rxjs.operators.reduce((res, item) => res.concat(item), []),	//create an array out of the sub-observables
    	)
    	.subscribe(slides => this.setSlides(slides))		//add the slides and call update
    });
    
    this.on( 'updated', function() {
    	if(this.slides && this.slides.length > 0) {
    		if(this.slider) {
    			this.slider.destroy();
    		}
    		this.swiper = new Swiper(this.refs.container, this.style.swiperConfig);
    	}
    });
    
    setSlides(slides) {
//     	console.log("set " + slides.length + " slides");
    	this.slides = slides;
    	this.update();
    }
    
	getElements(source) {
		if(viewerJS.iiif.isCollection(source)) {
			return source.members.filter(member => viewerJS.iiif.isCollection(member));
		} else {
			console.error("Cannot get slides from ", source);
		}
	}
     
    createSlide(element) {
//     	console.log("got element ", element);
    	if(viewerJS.iiif.isCollection(element) || viewerJS.iiif.isManifest(element)) {
    		let slide = { 
    				header : element.label,
    				description : element.description,
    				image : element.thumbnail,
    				link : viewerJS.iiif.getId(viewerJS.iiif.getViewerPage(element))
    		}
    		return slide;
    	} else {
    		return element;
    	}
    }
    
    translate(text) {
    	let translation =  viewerJS.iiif.getValue(text, this.opts.language);
    	if(!translation) {
    			translation = viewerJS.getMetadataValue(text, this.opts.language);
    	}
    	return translation;
    }
    
    getImage(slide) {
    	let image = slide.image;
    	if(image == undefined) {
    		return undefined;
    	} else if(viewerJS.isString(image)) {
    		return image;
    	} else if(image.service && (this.style.imageWidth || this.style.imageHeight)) {
    		let url = viewerJS.iiif.getId(image.service) + "/full/" + this.getIIIFSize(this.style.imageWidth, this.style.imageHeight) + "/0/default.jpg"
    		return url;
    	} else if(image["@id"]) {
    		return image["@id"]
    	} else {
    		return image.id;
    	}
    }
    
    getIIIFSize(width, height) {
    	if(width && height) {
    		return "!" + width + "," + height;
    	} else if(width) {
    		return width + ",";
    	} else if(height) {
    		return "," + height;
    	} else {
    		return "max";
    	}
    }
    
    getLink(slide) {
    	if(this.linkTarget == 'none') {
    		return "";
    	} else {
    		return slide.link;
    	}
    }
    
    amendStyle(styleConfig) {
    	let swiperConfig = styleConfig.swiperConfig;
    	if(swiperConfig.pagination) {
    		swiperConfig.pagination.el = this.refs.paginator;
    		this.showPaginator = true;
    	} else {
    		this.showPaginator = false;
    	}
    }
    
</script>

</slider>