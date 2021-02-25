<carousel>

<h3>SLIDER 2</h3>

<script>

    this.on( 'mount', function() {
    	this.collectionsUrl = this.opts.source;
    	
    	let pCollection = fetch(this.collectionsUrl)
    	.then(result => result.json());
    	
    	rxjs.from(pCollection)
    	.pipe(
    		rxjs.operators.flatMap(collection => collection.members),
    		rxjs.operators.filter(member => this.isCollection(member)),
    		rxjs.operators.map(collection => this.createSlide(collection)),
    		rxjs.operators.reduce((res, item) => res.concat(item), []),
    	)
    	.subscribe(slides => this.setSlides(slides))
    });
    
    setSlides(slides) {
    	this.slides = slides;
    	this.update();
    }
    
    isCollection(element) {
    	return (element.type == "Collection" || element["@type"] == "sc:Collection") && element.viewingHint != "multi-part";
    }
    
    isSingleManifest(element) {
    	return (element.type == "Manifest" || element["@type"] == "sc:Manifest") ;
    }
    
    isManifest(element) {
    	return element.type == "Manifest" || 
    	element["@type"] == "sc:Manifest" || 
    	(element.type == "Collection" && element.viewingHint == "multi-part") ||
    	(element["@type"] == "sc:Collection" && element.viewingHint == "multi-part");
    }
    
    getId(element) {
    	if(element.id) {
    		return element.id;
    	} else {
    		return element["@id"];	
    	}
    }
     
    createSlide(element) {
    	if(this.isCollection(element) || this.isManifest(element)) {
    		let slide = { 
    				text : element.label,
    				thumbnail : this.getId(element.thumbnail),
    				link : element.rendering
    					.filter(rendering => rendering.format == "text/html")
    					.map(rendering => this.getId(rendering))
    					.shift()
    		}
    		return slide;
    	} else {
    		console.err("Creating slide not implemented for ", element);
    	}
    }
    
</script>

</carousel>