<metadataQuestion>
	<div if="{this.showInstructions()}" class="crowdsourcing-annotations__instruction">
		<label>{Crowdsourcing.translate("crowdsourcing__help__create_rect_on_image")}</label>
	</div>
	<div if="{this.showInactiveInstructions()}" class="crowdsourcing-annotations__single-instruction -inactive">
		<label>{Crowdsourcing.translate("crowdsourcing__help__make_active")}</label>
	</div>
	<div class="crowdsourcing-annotations__wrapper" id="question_{opts.index}_annotation_{index}" each="{anno, index in this.question.annotations}">
		<div class="crowdsourcing-annotations__annotation-area" >
			<div if="{this.showAnnotationImages()}" class="crowdsourcing-annotations__annotation-area-image" style="border-color: {anno.getColor()}">
				<img src="{this.question.getImage(anno)}"></img>
			</div>
			<div class="crowdsourcing-annotations__question-metadata-list">
				<div each="{field, fieldindex in this.metadataFields}" class="crowdsourcing-annotations__question-metadata-list-item mb-2">
					<label class="crowdsourcing-annotations__question-metadata-list-item-label">{Crowdsourcing.translate(field)}:</label>
					<div class="crowdsourcing-annotations__question-metadata-list-item-field" if="{this.hasOriginalValue(field)}">{this.getOriginalValue(field)}</div>
					<input class="crowdsourcing-annotations__question-metadata-list-item-input form-control" if="{!this.hasOriginalValue(field)}" disabled="{this.opts.item.isReviewMode() ? 'disabled' : ''}" ref="input_{index}_{fieldindex}" type="text" data-annotationindex="{index}" value="{anno.getValue(field)}" onChange="{setValueFromEvent}">
					</input>
				</div>
			</div>
		</div>
		<!-- No delete button because metadata annotations should be deleted. Rather metadata fields may be remvoved by 
		giving them an empty value -->
	</div>
	<button if="{showAddAnnotationButton()}" onclick="{addAnnotation}" class="options-wrapper__option btn btn--default" id="add-annotation">{Crowdsourcing.translate("action__add_annotation")}</button>
	

<script>

	this.question = this.opts.question;
	//all metadata fields configured for the question
	this.metadataFields = [];
	//An object with all configured metadata fields wich have values in SOLR with their respective values
	this.originalData = {};

	this.on("mount", function() { 
	    this.initOriginalMetadata(this.question);
	    this.question.initializeView((anno) => new Crowdsourcing.Annotation.Metadata(anno, this.originalData), this.update, this.update, this.focusAnnotation);	    
	    console.log("mounted ", this);
		    this.opts.item.onImageOpen(function() {
		        switch(this.question.targetSelector) {
		            case Crowdsourcing.Question.Selector.WHOLE_PAGE:
		            case Crowdsourcing.Question.Selector.WHOLE_SOURCE:
		                if(this.question.annotations.length == 0 && !this.question.item.isReviewMode()) {                    
		                    this.question.addAnnotation();
		                    //reset dirty flag of item  set by addAnnotation(). 
		                    //Automatic creation of annotation should not set status to dirty
		                    this.opts.item.dirty = false; 
		                }
		        }
		        this.update();
		    }.bind(this));
	    Crowdsourcing.translator.addTranslations(this.question.metadataFields)
	    .then(() => this.update());
	});

	initOriginalMetadata(question) {
        this.metadataFields = question.metadataFields;
        let allMetadata = question.item.metadata;
        this.originalData = {};
        this.metadataFields.forEach(field => {
            let value = allMetadata[field];
            if(value) {                
                this.originalData[field] = value;
            }
        })
	}

    hasOriginalValue(field) {
        return this.originalData[field] != undefined;
    }
    
    getOriginalValue(field) {        
	    let value =this.originalData[field];
	    if(!value) {
	        return "";
	    } else if(Array.isArray(value)) {                
	        return value.join("; ");
	    } else {
	        return value;
	    }
    }
	
	focusAnnotation(index) {
	    let id = "question_" + this.opts.index + "_annotation_" + index + "_0";
	    let inputSelector = "#" + id;
	    this.refs.input_0.focus();
	}
	
	showAnnotationImages() {
	    return this.question.isRegionTarget();
	}
	
	showInstructions() {
	    return !this.opts.item.isReviewMode() &&  this.question.active && this.question.targetSelector == Crowdsourcing.Question.Selector.RECTANGLE && this.question.annotations.length == 0;
	}
	
	showInactiveInstructions() {
	    return !this.opts.item.isReviewMode() &&  !this.question.active && this.question.targetSelector == Crowdsourcing.Question.Selector.RECTANGLE && this.opts.item.questions.filter(q => q.isRegionTarget()).length > 1;
 
	}
	
	showAddAnnotationButton() {
	    return !this.question.isReviewMode() && !this.question.isRegionTarget() && this.question.mayAddAnnotation();
	}
	
	setValueFromEvent(event) {
        event.preventUpdate = true;
        let annoIndex = event.target.dataset.annotationindex;
        let anno = this.question.annotations[annoIndex];
        let field = event.item.field;
        let value = event.target.value;
        if(anno && field) {            
            anno.setValue(field, value);
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
        this.question.addAnnotation();
        this.question.focusCurrentAnnotation();
    }



</script>


</metadataQuestion>
