<plaintextQuestion>
	<div if="{this.showInstructions()}" class="annotation_instruction">
		<label>{Crowdsourcing.translate("crowdsourcing__help__create_rect_on_image")}</label>
	</div>
	<div class="annotation_wrapper" id="question_{opts.index}_annotation_{index}" each="{anno, index in this.question.annotations}">
		<div class="annotation_area" >
			<div if="{this.showAnnotationImages()}" class="annotation_area__image" style="border-color: {anno.getColor()}">
				<img src="{this.question.getImage(anno)}"></img>
			</div>
			<div class="annotation_area__text_input">
				<textarea disabled="{this.opts.item.isReviewMode() ? 'disabled' : ''}" onChange="{setTextFromEvent}" value="{anno.getText()}">
				</textarea>
			</div>
		</div>
		<div class="cms-module__actions">
			<button if="{ !this.opts.item.isReviewMode() }" onClick="{deleteAnnotationFromEvent}" class="annotation_area__button btn btn--clean delete">{Crowdsourcing.translate("action__delete_annotation")}
			</button>
		</div>
	</div>
	<button if="{showAddAnnotationButton()}" onclick="{addAnnotation}" class="options-wrapper__option btn btn--default" id="add-annotation">{Crowdsourcing.translate("action__add_annotation")}</button>
	

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
	        //if target is whole source, load annotations just once
            case Crowdsourcing.Question.Selector.WHOLE_SOURCE:
                this.question.loadAnnotationsFromLocalStorage();
                this.opts.item.onImageOpen(function() {
	    		    this.question.initAnnotations();
	    			this.update();
	    		}.bind(this));
	        	break;
		    //if target is a rectangle region on page, initialize drawing on canvas
            case Crowdsourcing.Question.Selector.RECTANGLE:
                    this.question.initAreaSelector();
            //if target is page or region on page, load matching annotations on each image change
            case Crowdsourcing.Question.Selector.WHOLE_PAGE:
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

	
	focusCurrentAnnotation() {
	    if(this.question.currentAnnotationIndex > -1 && this.question.annotations && this.question.annotations.length > this.question.currentAnnotationIndex) {
		    let id = "question_" + this.opts.index + "_annotation_" + this.question.currentAnnotationIndex;
		    let inputSelector = "#" + id + " textarea";
		    window.setTimeout(function(){this.root.querySelector(inputSelector).focus();}.bind(this),1);
		}
	}
	
	showAnnotationImages() {
	    return this.question.targetSelector === Crowdsourcing.Question.Selector.RECTANGLE;
	}
	
	showInstructions() {
	    return !this.opts.item.isReviewMode() && this.question.targetSelector == Crowdsourcing.Question.Selector.RECTANGLE && this.question.annotations.length == 0;
	}
	
	showAddAnnotationButton() {
	    if(!this.opts.item.isReviewMode()) {	        
		    switch(this.question.targetSelector) {
		        case Crowdsourcing.Question.Selector.WHOLE_PAGE:
		        case Crowdsourcing.Question.Selector.WHOLE_SOURCE:
					return this.question.targetFrequency == 0 || this.question.targetFrequency > this.question.annotations.length;
		    }
	    }
	    return false;
	}
	
    setTextFromEvent(event) {
        event.preventUpdate = true;
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
    
    addAnnotation() {
        this.question.createEmptyAnnotation();
        this.focusCurrentAnnotation();
    }
    
    handleFinishedDrawing(result) {
        this.question.addAnnotation(result.id, result.region, result.color);
        this.focusCurrentAnnotation();
        this.update();
    }
    
    handleFinishedTransforming(result) {
        this.question.setRegion(result.region, this.question.getAnnotationByOverlayId(result.id));
        this.update();
    }


</script>


</plaintextQuestion>