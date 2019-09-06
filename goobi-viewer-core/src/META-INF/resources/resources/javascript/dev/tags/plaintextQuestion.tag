<plaintextQuestion>
	<div if="{this.showInstructions()}" class="annotation_instruction">
		<label>{Crowdsourcing.translate("crowdsourcing__help__create_rect_on_image")}</label>
	</div>
	<div id="annotation_{index}" each="{anno, index in this.question.annotations}">
		<div class="annotation_area" style="border-color: {anno.getColor()}" >
			<div if="{this.showAnnotationImages()}" class="annotation_area__image">
				<img src="{this.question.getImage(anno)}"></img>
			</div>
			<div class="annotation_area__text_input">
				<label>{viewerJS.getMetadataValue(this.question.translations.text)}</label>			
				<textarea onChange="{setTextFromEvent}" value="{anno.getText()}">
				</textarea>
			</div>
		</div>
		<span if="{anno.getText() || anno.getRegion()}" onClick="{deleteAnnotationFromEvent}" class="annotation_area__button">{Crowdsourcing.translate("action__delete_annotation")}</span>
	</div>

<script>

	this.question = this.opts.question;
	this.question.createAnnotation = function(anno) {
	    let annotation = new Crowdsourcing.Annotation.Plaintext(anno);
	    annotation.generator = this.question.getGenerator();
	    annotation.creator = this.opts.item.getCreator();
	    return annotation;
	}.bind(this);

	
	this.on("mount", function() {
	    switch(this.question.targetSelector) {
            case Crowdsourcing.Question.Selector.RECTANGLE:
                    this.question.initAreaSelector();
                    break;
	    }
	    switch(this.question.targetFrequency) {
	        case Crowdsourcing.Question.Frequency.ONE_PER_MANIFEST:
	        case Crowdsourcing.Question.Frequency.MULTIPLE_PER_MANIFEST:
	            this.opts.item.onImageOpen(function() {
	    		    this.question.initAnnotations();
	    			this.update();
	    		}.bind(this));
	        	break;
	        case Crowdsourcing.Question.Frequency.ONE_PER_CANVAS:
	        case Crowdsourcing.Question.Frequency.MULTIPLE_PER_CANVAS:
	        default:
	    		this.opts.item.onImageOpen(function() {
	    		    this.question.loadAnnotationsFromLocalStorage();
	    		    this.question.initAnnotations();
	    			this.update();
	    		}.bind(this));
	    }
	    
	    if(this.question.areaSelector) {	        
	        this.question.areaSelector.finishedDrawing.subscribe(this.handleFinishedDrawing);
	        this.question.areaSelector.finishedTransforming.subscribe(this.handleFinishedTransforming);
	    }

	});
	
	this.on("updated", function() {
		if(this.question.currentAnnotationIndex > -1 && this.question.annotations && this.question.annotations.length > this.question.currentAnnotationIndex) {
		    let id = "annotation_" + this.question.currentAnnotationIndex;
		    let inputSelector = "#" + id + " textarea";
		    window.setTimeout(function(){this.root.querySelector(inputSelector).focus();}.bind(this),1);
		}
	
	}.bind(this));
	
	showAnnotationImages() {
	    return this.question.targetSelector === Crowdsourcing.Question.Selector.RECTANGLE;
	}
	
	showInstructions() {
	    return this.question.targetSelector == Crowdsourcing.Question.Selector.RECTANGLE && this.question.annotations.length == 0;
	}
	
    setTextFromEvent(event) {
        if(event.item.anno) {            
            event.item.anno.setText(event.target.value);
            this.question.saveToLocalStorage();
        } else {
            throw "No annotation to set"
        }
    }
    
    deleteAnnotationFromEvent(event) {
        if(event.item.anno) {
            this.question.deleteAnnotation(event.item.anno);
            this.update();
        }
    }
    
    handleFinishedDrawing(result) {
        this.question.addAnnotation(result.id, result.region, result.color);
        this.update();
    }
    
    handleFinishedTransforming(result) {
        this.question.setRegion(result.region, this.question.getAnnotation(result.id));
        this.update();
    }


</script>


</plaintextQuestion>