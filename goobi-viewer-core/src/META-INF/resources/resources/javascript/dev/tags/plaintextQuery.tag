<plaintextQuery>
	<div if="{this.showInstructions()}" class="annotation_instruction">
		<label>{Crowdsourcing.translate("crowdsourcing__help__create_rect_on_image")}</label>
	</div>
	<div id="annotation_{index}" each="{anno, index in this.annotations}">
		<div class="annotation_area" style="border-color: {anno.getColor()}" >
			<div if="{this.showAnnotationImages()}" class="annotation_area__image">
				<img src="{this.getImage(anno)}"></img>
			</div>
			<div class="annotation_area__text_input">
				<label>{viewerJS.getMetadataValue(this.opts.query.label)}</label>			
				<textarea onChange="{this.setTextFromEvent}" value="{anno.getText()}">
				</textarea>
			</div>
		</div>
		<span if="{anno.getText() || anno.getRegion()}" onClick="{this.deleteAnnotationFromEvent}" class="annotation_area__button">{Crowdsourcing.translate("action__delete_annotation")}</span>
	</div>

<script>

	this.queryType = Crowdsourcing.Query.getType(this.opts.query);
	this.targetFrequency = Crowdsourcing.Query.getFrequency(this.opts.query);
	this.targetSelector = Crowdsourcing.Query.getSelector(this.opts.query);
	
// 	console.log("this.queryType = ", this.queryType);
// 	console.log("this.targetFrequency = ", this.targetFrequency);
// 	console.log("this.targetSelector = ", this.targetSelector);

	this.annotations = [];
	this.currentAnnotationIndex = -1;

	this.on("mount", function() {
	    this.deleteFromLocalStorage();
	    switch(this.targetSelector) {
	        case Crowdsourcing.Query.Selector.RECTANGLE:
	            this.initAreaSelector();
	            break;
	        case Crowdsourcing.Query.Selector.WHOLE_SOURCE:
	        case Crowdsourcing.Query.Selector.WHOLE_PAGE:
	    }
	    
	    switch(this.targetFrequency) {
	        case Crowdsourcing.Query.Frequency.ONE_PER_CANVAS:
	        case Crowdsourcing.Query.Frequency.MULTIPLE_PER_CANVAS:
	    		this.opts.item.onImageOpen( () => this.resetAnnotations());
	    }

	});
	
	this.on("updated", function() {
		if(this.currentAnnotationIndex > -1 && this.annotations && this.annotations.length > this.currentAnnotationIndex) {
		    let id = "annotation_" + this.currentAnnotationIndex;
		    let inputSelector = "#" + id + " textarea";
		    window.setTimeout(function(){this.root.querySelector(inputSelector).focus();}.bind(this),1);
		}
	
	}.bind(this));
	
	showAnnotationImages() {
	    return this.targetSelector === Crowdsourcing.Query.Selector.RECTANGLE;
	}
	
	showInstructions() {
	    return this.targetSelector == Crowdsourcing.Query.Selector.RECTANGLE && this.annotations.length == 0;
	}
	
	initAnnotations() {
	    switch(this.targetSelector) {
	        case Crowdsourcing.Query.Selector.WHOLE_PAGE:
	        case Crowdsourcing.Query.Selector.WHOLE_SOURCE:
	            if(this.annotations.length == 0) {
	                //create empty annotation
		            let anno = new Crowdsourcing.Annotation.Plaintext({});
		            anno.setTarget(this.getTarget());
		            this.annotations.push(anno);	                
	            }
	            this.currentAnnotationIndex = this.annotations.length - 1;
	    }
	}
	
	resetAnnotations() {
	    this.annotations = this.restoreFromLocalStorage();
	    console.log("reset annotations to ", this.annotations);
	    this.initAnnotations()
	    if(this.areaSelector) {
	        this.annotations.map(anno => {return {id: anno.id,
	            					      region: anno.getRegion(), 
	            						  color: anno.getColor()
	            					     }}).forEach(anno => this.areaSelector.addOverlay(anno, this.opts.item.image.viewer))
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
	    let annotation = new Crowdsourcing.Annotation.Plaintext({});
	    annotation.id = result.id;
	    annotation.setTarget(this.getTarget());
	    annotation.setRegion(result.region);
	    annotation.setColor(result.color);
	    this.annotations.push(annotation);
	    this.currentAnnotationIndex = this.annotations.length - 1;
    	this.saveToLocalStorage();
	    this.update();
	}
	
	handleFinishedTransforming(result) {
		let anno = this.getAnnotation(result.id);
		if(anno) {
		    anno.setRegion(result.region);
		    this.currentAnnotationIndex = this.getIndex(anno);
        	this.saveToLocalStorage();
		    this.update();
		}
	}
	
	getImageUrl(rect, imageId) {
	    let url = imageId + "/" + rect.x + "," + rect.y + "," + rect.width + "," + rect.height + "/full/0/default.jpg";
	    return url;
	}
	
    setTextFromEvent(event) {
        if(event.item.anno) {            
            event.item.anno.setText(event.target.value);
        	this.saveToLocalStorage();
        } else {
            throw "No annotation to set"
        }
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
	
    deleteFromLocalStorage() {
        let map = this.getAnnotationsFromLocalStorage();
	    if(map.has(Crowdsourcing.getResourceId(this.getTarget()))) {
		    map.set(Crowdsourcing.getResourceId(this.getTarget()), [] );
	    }
	    let value = JSON.stringify(Array.from(map.entries()));
	    localStorage.setItem("CrowdsourcingQuery_" + this.opts.query.id, value);
    }
    
	saveToLocalStorage() {
	    let map = this.getAnnotationsFromLocalStorage();
	    map.set(Crowdsourcing.getResourceId(this.getTarget()), this.annotations );
	    let value = JSON.stringify(Array.from(map.entries()));
	    localStorage.setItem("CrowdsourcingQuery_" + this.opts.query.id, value);
	}
	
	restoreFromLocalStorage() {
	    let map = this.getAnnotationsFromLocalStorage();
	    let annotations;
	    if(map.has(Crowdsourcing.getResourceId(this.getTarget()))) {
	        annotations = map.get(Crowdsourcing.getResourceId(this.getTarget())).map( anno => new Crowdsourcing.Annotation.Plaintext(anno));
	    } else {
	        annotations = [];
	    }
	    return annotations;
	}
	
	getAnnotationsFromLocalStorage() {
	    let string = localStorage.getItem("CrowdsourcingQuery_" + this.opts.query.id);
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


</plaintextQuery>