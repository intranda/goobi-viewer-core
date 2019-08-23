<campaignItem>

	<div class="content">
		<div class="content_left">
			<imageView if="{this.loader}" id="mainImage" source="{this.loader.getCurrentImage()}" loader="{this.loader}"></imageView>
			<canvasPaginator if="{this.loader}" loader="{this.loader}"></canvasPaginator>
		</div>
		<div class="content_right">
		</div>
	 </div>

<script>

	let CanvasLoader = function(canvasList, initialIndex, loadMethod) {
	
		this.canvasList = canvasList;
		this.imageIndex = initialIndex;
		this.load = loadMethod;
		this.loadImage = (index) =>  {
		    this.imageIndex = index;
		    this.load();
		}
		this.getCurrentImage = () => this.canvasList[this.imageIndex];
		
	};

	this.on("mount", function() {
		console.log("load ", this.opts.source);
	    fetch(this.opts.source)
	    .then( response => response.json() )
	    .then( item => this.loadItem(item));
	});
	
	loadItem(item) {
		console.log("load item ", item);
		fetch(item.source)
		.then( response => response.json() )
		.then((imageSource) => this.initImageView(this.getCanvasList(imageSource), 0))
		.catch( error => console.error("ERROR ", error));  
	}
	
	initImageView(canvases, initialCanvasIndex) {
	    if(!canvases || !canvases.length) {
	        return;
	    }
	    initialCanvasIndex = Math.max(0, Math.min(initialCanvasIndex, canvases.length-1));
	    this.loader = new CanvasLoader(canvases, initialCanvasIndex, this.loadImage);
	    console.log("initialized image loader ", this.loader);
	    this.update();
	}
	
	loadImage() {
	    this.loader.updateImageSource();
	}

	
	resolveCanvas(source) {
	    if(isString(source)) {
	        return fetch(source)
	        .then( response => response.json() );
	    } else {
	        return Q.fcall(() => source);
	    }
	}
	
	isString(variable) {
	    return typeof variable === 'string' || variable instanceof String
	}
	
	/**
		get a list containing all canvas json items or canvas urls contained in the source object
		The source must be either a manifest, a range or a single canvas
	*/
	getCanvasList(source) {
	    let sourceType = source.type;
	    if(!sourceType) {
	        sourceType = source["@type"];
	    }
	    
	    switch(sourceType) {
	        case "sc:Manifest":
	            return source.sequences[0].canvases;
	        case "sc:Canvas":
	            return [source];
	        case "sc:Range":
	            return source.canvases;
	        default:
	            console.log("Unknown source type, cannot retrieve canvases", source);
	    }
	}

</script>

</campaignItem>