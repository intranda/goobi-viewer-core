

<slider>

<div ref="container" class="swiper slider-{this.styleName}__container">
	<div class="swiper-wrapper slider-{this.styleName}__wrapper">
		<div each="{slide, index in slides}" class="swiper-slide slider-{this.styleName}__slide" ref="slide_{index}">
		</div>
	</div>
	<div if="{this.showPaginator}" ref="paginator" class="swiper-pagination slider-{this.styleName}__dots"></div>
</div>

<script>

	//initially show paginator so it can be referenced when amending style config (see this.amendStyle())
	this.showPaginator = true;
	

    this.on( 'mount', function() {
		this.style = this.opts.styles.get(this.opts.style);
    	// console.log(this.style);
     	// console.log("mounting 'slider.tag' ", this.opts, this.style);
		this.amendStyle(this.style);
		this.styleName = this.opts.styles.getStyleNameOrDefault(this.opts.style);
    	// console.log("init slider with '" + this.opts.style + "''", this.style);
		this.timeout = this.style.timeout ? this.style.timeout : 100000;
		this.maxSlides = this.style.maxSlides ? this.style.maxSlides : 1000;
		this.linkTarget = this.opts.linktarget ? this.opts.linktarget : "_self";
		

		
		firstSlideMessage = this.opts.firstslidemessage;

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
    		rxjs.operators.map(array => array.sort( (s1,s2) => s1.order-s2.order ))
    	)
    	.subscribe(slides => this.setSlides(slides))		//add the slides and call update
    });
    
    this.on( 'updated', function() {
    	
    	// console.log("layout = ", this.getLayout());
    	
    	if(this.slides && this.slides.length > 0) {
    		if(this.slider) {
    			this.slider.destroy();
    			
    		}
			this.initSlideTags(this.slides);
    		this.swiper = new Swiper(this.refs.container, this.style.swiperConfig);
    		window.viewerJS.slider.sliders.push(this.swiper);

    		 
    		console.log(this.swiper);
    		// console.log(this.refs.container);
    		
    	}
    	
    	if (this.style.onUpdate) {
    		this.style.onUpdate();
    	}
    	
    });
    
    setSlides(slides) {
//     	console.log("set " + slides.length + " slides");
    	this.slides = slides;
    	this.update();
    }
    
    let imagealtmsgkey = this.opts.imagealtmsgkey;
    
    /**
    * Mount riot tag for all <slide> elements using a riot tag named "slide_[style.layout]"
    */
    initSlideTags(slides) {
    	slides.forEach( (slide, index) => {
    		let tagElement = this.refs["slide_" + index];
//     		console.log("mount slide in ", tagElement);
    		riot.mount(tagElement, "slide_" + this.getLayout(),  {
    			stylename: this.styleName,
   				link: this.getLink(slide),
   				link_target: this.linkTarget,
   				image: this.getImage(slide),
   				label: this.translate(slide.label),
   				description: this.translate(slide.description),
   				alttext: this.translate(slide.altText),
   				altimagemsgkey: this.translate(imagealtmsgkey),
    		});
    	});
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
    				label : element.label,
    				description : element.description,
    				image : element.thumbnail,
    				link : viewerJS.iiif.getId(viewerJS.iiif.getViewerPage(element)),
    				order : element.order
    		}
    		return slide;
    	} else {
    		return element;
    	}
    }
    
    translate(text) {
    	let translation =  viewerJS.iiif.getValue(text, this.opts.language, this.opts.defaultlanguage);
    	if(!translation) {
    			translation = viewerJS.getMetadataValue(text, this.opts.language, this.opts.defaultlanguage);
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
    	if(swiperConfig.pagination && !swiperConfig.pagination.el)  {
    		swiperConfig.pagination.el = this.refs.paginator;
    		this.showPaginator = true;
    	} else {
    		this.showPaginator = false;
    	}
	  	swiperConfig.a11y = {
	  		prevSlideMessage: this.opts.prevslideMessage,
			nextSlideMessage: this.opts.nextslideMessage,
	  		lastSlideMessage: this.opts.firstslidemessage,
			firstSlideMessage: this.opts.lastslidemessage,
			paginationBulletMessage: this.opts.paginationbulletmessage + ' \{\{index\}\}',
		}
	}
    
    getLayout() {
    	let layout = this.style.layout ? this.style.layout : 'default';
    	// console.log('layout:' + this.style.layout); 
    	return layout;
    }
    
</script>

</slider>