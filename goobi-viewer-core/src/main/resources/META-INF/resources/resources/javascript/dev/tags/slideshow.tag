

<slideshow>

<div ref="container" class="swiper-container slider-{this.style}__container">
	<div class="swiper-wrapper slider-{this.style}__wrapper">
		<div each="{slide in slides}" class="swiper-slide slider-{this.style}__slide">
			<a href="{slide.link}">
				<h3 class="slider-{this.style}__header">{translate(slide.header)}</h3>
				<div class="slider-{this.style}__image" style="background-image: url({getImage(slide)})">
				</div>
				<div class="slider-{this.style}__description">{translate(slide.description)}</div>
			</a>
		</div>
	</div>
</div>

<script>

    this.on( 'mount', function() {
    	this.style = this.opts.style;
//     	console.log("inti slider with", this.style);
    	
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
    		rxjs.operators.flatMap(source => source),
    		rxjs.operators.flatMap(uri => fetch(uri), undefined, 5),
    		rxjs.operators.filter(result => result.status == 200),
    		rxjs.operators.takeUntil(rxjs.timer(10000)),
    		rxjs.operators.flatMap(result => result.json()),
    		rxjs.operators.map(element => this.createSlide(element)),
    		rxjs.operators.filter(element => element != undefined),
    		rxjs.operators.reduce((res, item) => res.concat(item), []),
    	)
    	.subscribe(slides => this.setSlides(slides))
    });
    
    this.on( 'updated', function() {
    	if(this.slides && this.slides.length > 0) {
    		if(this.slider) {
    			this.slider.destroy();
    		}
    		let style = this.opts.styles.get(this.opts.style);
//     		console.log("create slideshow with ", style)
    		this.swiper = new Swiper(this.refs.container, style);
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
    				image : viewerJS.iiif.getId(element.thumbnail),
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
    	} else if(image["@id"]) {
    		return image["@id"]
    	} else {
    		return image.id;
    	}
    }
    
</script>

</slideshow>