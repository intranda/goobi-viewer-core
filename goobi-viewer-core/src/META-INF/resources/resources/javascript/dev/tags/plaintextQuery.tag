<plaintextQuery>
	<div if="{this.showInstructions()}" class="annotation_instruction">
		<label>{Crowdsourcing.translate("crowdsourcing__help__create_rect_on_image")}</label>
	</div>
	<div id="annotation_{index}" each="{anno, index in this.query.annotations}">
		<div class="annotation_area" style="border-color: {anno.getColor()}" >
			<div if="{this.showAnnotationImages()}" class="annotation_area__image">
				<img src="{this.query.getImage(anno)}"></img>
			</div>
			<div class="annotation_area__text_input">
				<label>{viewerJS.getMetadataValue(this.query.translations.label)}</label>			
				<textarea onChange="{setTextFromEvent}" value="{anno.getText()}">
				</textarea>
			</div>
		</div>
		<span if="{anno.getText() || anno.getRegion()}" onClick="{deleteAnnotationFromEvent}" class="annotation_area__button">{Crowdsourcing.translate("action__delete_annotation")}</span>
	</div>

<script>

	this.query = this.opts.query;
	this.query.createAnnotation = function(anno) {
	    let annotation = new Crowdsourcing.Annotation.Plaintext(anno);
	    annotation.generator = this.opts.item.getGenerator();
	    annotation.creator = this.opts.item.getCreator();
	    return annotation;
	}.bind(this);

	
	this.on("mount", function() {
	    switch(this.query.targetSelector) {
	        case Crowdsourcing.Query.Selector.RECTANGLE:
	            this.query.initAreaSelector();
	            break;
	        case Crowdsourcing.Query.Selector.WHOLE_SOURCE:
	        case Crowdsourcing.Query.Selector.WHOLE_PAGE:
	    }
	    
	    switch(this.query.targetFrequency) {
	        case Crowdsourcing.Query.Frequency.ONE_PER_CANVAS:
	        case Crowdsourcing.Query.Frequency.MULTIPLE_PER_CANVAS:
	    		this.opts.item.onImageOpen(function() {
	    		    this.query.resetAnnotations();
	    			this.update();
	    		}.bind(this));
	    }
	    
	    if(this.query.areaSelector) {	        
	        this.query.areaSelector.finishedDrawing.subscribe(this.handleFinishedDrawing);
	        this.query.areaSelector.finishedTransforming.subscribe(this.handleFinishedTransforming);
	    }

	});
	
	this.on("updated", function() {
		if(this.query.currentAnnotationIndex > -1 && this.query.annotations && this.query.annotations.length > this.query.currentAnnotationIndex) {
		    let id = "annotation_" + this.query.currentAnnotationIndex;
		    let inputSelector = "#" + id + " textarea";
		    window.setTimeout(function(){this.root.querySelector(inputSelector).focus();}.bind(this),1);
		}
	
	}.bind(this));
	
	showAnnotationImages() {
	    return this.query.targetSelector === Crowdsourcing.Query.Selector.RECTANGLE;
	}
	
	showInstructions() {
	    return this.query.targetSelector == Crowdsourcing.Query.Selector.RECTANGLE && this.query.annotations.length == 0;
	}
	
    setTextFromEvent(event) {
        if(event.item.anno) {            
            event.item.anno.setText(event.target.value);
            this.query.saveToLocalStorage();
        } else {
            throw "No annotation to set"
        }
    }
    
    deleteAnnotationFromEvent(event) {
        if(event.item.anno) {
            this.query.deleteAnnotation(event.item.anno);
            this.update();
        }
    }
    
    handleFinishedDrawing(result) {
        this.query.addAnnotation(result.id, result.region, result.color);
        this.update();
    }
    
    handleFinishedTransforming(result) {
        this.query.setRegion(result.region, this.query.getAnnotation(result.id));
        this.update();
    }


</script>


</plaintextQuery>