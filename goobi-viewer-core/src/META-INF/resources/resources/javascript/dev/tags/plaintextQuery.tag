<plaintextQuery>

	<div id="annotation_{index}" class="annotation_area" each="annottation, index in this.annotations}">
		
		<div if="{this.showAnnotationImages()}" class="annotation_area__image">
			<img  src="{this.getImage(annotation.selector)}"></img>
		</div>
		<div class="annotation_area__text_input">
			<label>{viewerJS.getMetadataValue(this.opts.query.label)}</label>			
			<textarea  onChange="{annotation.setTextFromEvent}" value="{annotation.getText()}"></textarea>
		</div>
	
	</div>

<script>

	this.queryType = Crowdsourcing.Query.getType(this.opts.query);
	this.targetFrequency = Crowdsourcing.Query.getFrequency(this.opts.query);
	this.targetSelector = Crowdsourcing.Query.getSelector(this.opts.query);
	
	console.log("this.queryType = ", this.queryType);
	console.log("this.targetFrequency = ", this.targetFrequency);
	console.log("this.targetSelector = ", this.targetSelector);

	this.annotations = [];
	this.selectedAnnotation = undefined;
	this.target = this.opts.item.getCurrentCanvas();

	this.on("mount", function() {
	    
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
		if(this.annotations.length > 0) {
		    let id = "annotation_" + (this.annotations.length-1);
		    this.selectedAnnotation = this.annotations[this.annotations.length-1];
		    let inputSelector = "#"id + " textarea";
		    window.setTimeout(function(){this.root.querySelector(inputSelector).focus();}.bind(this),1);

		}
	
	}.bind(this));
	
	showAnnotationImages() {
	    return this.targetSelector === Crowdsourcing.Query.Selector.RECTANGLE;
	}
	
	resetAnnotations() {
	    this.annotations = [];
	    
	    switch(this.targetSelector) {
	        case Crowdsourcing.Query.Selector.WHOLE_PAGE:
	        case Crowdsourcing.Query.Selector.WHOLE_SOURCE:
	            let anno = new Crowdsourcing.Annotation.Plaintext({});
	            anno.setTarget(this.target);
	            this.annotations.push(anno));
	    }
	    this.update();
	}
	
	initAreaSelector() {
		this.areaSelector = new Crowdsourcing.AreaSelector(this.opts.item, true);
		this.areaSelector.init();
		this.areaSelector.finishedDrawing.subscribe(this.handleFinishedDrawing);
		this.opts.item.onImageOpen( () => this.areaSelector.reset());
	}
	
	getId(annotation) {
	    return this.annotations.indexOf(annotation);
	}

	getImage(annotation) {
	    return this.getImageUrl(annotation.getRegion(), this.opts.item.getImageId(this.opts.item.getCurrentCanvas()));
	}
	
	handleFinishedDrawing(result) {
	    console.log("Finished drawing ", result);
	    let Annotation
	    
	    this.answers.push(new Crowdsourcing.Answer({}, result));
	    this.update();
	}
	
	getImageUrl(rect, imageId) {
	    let url = imageId + "/" + rect.x + "," + rect.y + "," + rect.width + "," + rect.height + "/full/0/default.jpg";
	    return url;
	}
	
	saveToLocalStorage() {
	    let annos = this.annotations.forEach( anno => JSON.parse(anno));
	    console.log("annotation list ", annos);
	}

</script>


</plaintextQuery>