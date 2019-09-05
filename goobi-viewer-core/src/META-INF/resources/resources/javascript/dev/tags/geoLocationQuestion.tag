<geoLocationQuestion>
	

	
	<div if="{this.showInstructions()}" class="annotation_instruction">
		<label>Halten sie die Shift-Taste gedr&#x00FCckt und ziehen Sie im Bild einen Bereich mit der Maus auf.</label>
	</div>
	
	<div id="geoMap"></div>
	
	<div id="annotation_{index}" each="{anno, index in this.annotations}">
		
	</div>

<script>
	
	this.questionType = Crowdsourcing.Question.getType(this.opts.question);
	this.targetFrequency = Crowdsourcing.Question.getFrequency(this.opts.question);
	this.targetSelector = Crowdsourcing.Question.getSelector(this.opts.question);


	this.annotations = [];
	this.currentAnnotationIndex = -1;

	this.on("mount", function() {
	    this.initMap();
	    switch(this.targetSelector) {
	        case Crowdsourcing.Question.Selector.RECTANGLE:
	            this.initAreaSelector();
	            break;
	        case Crowdsourcing.Question.Selector.WHOLE_SOURCE:
	        case Crowdsourcing.Question.Selector.WHOLE_PAGE:
	    }
	    
	    switch(this.targetFrequency) {
	        case Crowdsourcing.Question.Frequency.ONE_PER_CANVAS:
	        case Crowdsourcing.Question.Frequency.MULTIPLE_PER_CANVAS:
	    		this.opts.item.onImageOpen( () => this.resetAnnotations());
	    }

	});
	
	this.on("updated", function() {
		if(this.currentAnnotationIndex > -1 && this.annotations && this.annotations.length > this.currentAnnotationIndex) {
		    
		}
	
	}.bind(this));
	
	initMap() {
	    this.geoMap = L.map('geoMap').setView([51.505, -0.09], 13);
	}
	
	
	showAnnotationImages() {
	    return this.targetSelector === Crowdsourcing.Question.Selector.RECTANGLE;
	}
	
	showInstructions() {
	    return false;
	}
	
	initAnnotations() {
	    switch(this.targetSelector) {
	        case Crowdsourcing.Question.Selector.WHOLE_PAGE:
	        case Crowdsourcing.Question.Selector.WHOLE_SOURCE:
	            if(this.annotations.length == 0) {
	                //create empty annotation
		                            
	            }
	            this.currentAnnotationIndex = this.annotations.length - 1;
	    }
	}
	
	resetAnnotations() {
	    this.annotations = this.restoreFromLocalStorage();
	    console.log("reset annotations to ", this.annotations);
	    this.initAnnotations()
	    if(this.areaSelector) {
	    }
	    this.update();
	}
	
	initAreaSelector() {
		this.areaSelector = new Crowdsourcing.AreaSelector(this.opts.item, true);
		this.areaSelector.init();
		this.areaSelector.finishedDrawing.subscribe(this.handleFinishedDrawing);
		this.areaSelector.finishedTransforming.subscribe(this.handleFinishedTransforming);
		this.opts.item.onImageOpen( () => this.areaSelector.reset());
	}
	
	getAnnotation(id) {
	    return this.annotations.find(anno => anno.id == id);
	}
	
	getIndex(anno) {
	    return this.annotations.indexOf(anno);
	}

	getImage(annotation) {
	    return this.getImageUrl(annotation.getRegion(), this.opts.item.getImageId(this.opts.item.getCurrentCanvas()));
	}
	
	handleFinishedDrawing(result) {
	   
	}
	
	handleFinishedTransforming(result) {
		
	}
	
	getImageUrl(rect, imageId) {
	    let url = imageId + "/" + rect.x + "," + rect.y + "," + rect.width + "," + rect.height + "/full/0/default.jpg";
	    return url;
	}
    
    getTarget() {
    	return this.opts.item.getCurrentCanvas();

    }
    
    deleteAnnotationFromEvent(event) {
        if(event.item.anno) {
            this.deleteAnnotation(event.item.anno);
        }
    }
    
    deleteAnnotation(anno) {
        let index = this.getIndex(anno);
        if(index > -1) {            
	        this.annotations.splice(index,1);
	        if(this.currentAnnotationIndex >= index) {
	            this.currentAnnotationIndex--;
	        }
	        if(this.areaSelector) {	            
	        	this.areaSelector.removeOverlay(anno, this.opts.item.image.viewer);
	        }
	    	this.saveToLocalStorage();
	    	this.initAnnotations();
	    	this.update();
        }
    }
	
	saveToLocalStorage() {
	    let map = this.getAnnotationsFromLocalStorage();
	    map.set(Crowdsourcing.getResourceId(this.getTarget()), this.annotations );
	    let value = JSON.stringify(Array.from(map.entries()));
	    localStorage.setItem("CrowdsourcingQuestion_" + this.opts.question.id, value);
	}
	
	restoreFromLocalStorage() {
	    let map = this.getAnnotationsFromLocalStorage();
	    let annotations;
	    if(map.has(Crowdsourcing.getResourceId(this.getTarget()))) {
	        annotations = map.get(Crowdsourcing.getResourceId(this.getTarget())).map( anno => new Crowdsourcing.Annotation.GeoJson(anno));
	    } else {
	        annotations = [];
	    }
	    return annotations;
	}
	
	getAnnotationsFromLocalStorage() {
	    let string = localStorage.getItem("CrowdsourcingQuestion_" + this.opts.question.id);
	    try {
	        let array = JSON.parse(string);
		    if(Array.isArray(array)) {
		        return new Map(JSON.parse(string));
		    } else {
		        return new Map();
		    }
	    } catch(e) {
	        console.log("Error loading json ", e);
	        return new Map();
	    }
	}

</script>


</geoLocationQuestion>

