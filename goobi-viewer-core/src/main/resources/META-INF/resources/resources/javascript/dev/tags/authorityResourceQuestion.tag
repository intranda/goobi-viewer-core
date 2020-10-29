<authorityResourceQuestion>
	<div if="{this.showInstructions()}" class="annotation_instruction">
		<label>{Crowdsourcing.translate("crowdsourcing__help__create_rect_on_image")}</label>
	</div>
	<div if="{this.showInactiveInstructions()}" class="annotation_instruction annotation_instruction_inactive">
		<label>{Crowdsourcing.translate("crowdsourcing__help__make_active")}</label>
	</div>
	<div class="annotation_wrapper" id="question_{opts.index}_annotation_{index}" each="{anno, index in this.question.annotations}">
		<div class="annotation_area -small" >
			<div if="{this.showAnnotationImages()}" class="annotation_area__image" style="border-color: {anno.getColor()}">
				<img src="{this.question.getImage(anno)}"></img>
			</div>
			<span class="annotation_area__input_prefix">{question.authorityData.baseUri}</span>
			<div class="annotation_area__text_input">
				<input class="form-control" disabled="{this.opts.item.isReviewMode() ? 'disabled' : ''}" onChange="{setIdFromEvent}" value="{getIdAsNumber(anno)}">
				</input>
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
 
	this.on("mount", function() {
	    this.question.initializeView((anno) => new Crowdsourcing.Annotation.AuthorityResource(anno, this.question.authorityData.context), this.update, this.update, this.focusAnnotation);	    
	    this.opts.item.onImageOpen(function() {
	        switch(this.question.targetSelector) {
	            case Crowdsourcing.Question.Selector.WHOLE_PAGE:
	            case Crowdsourcing.Question.Selector.WHOLE_SOURCE:
	                if(this.question.annotations.length == 0 && !this.question.item.isReviewMode()) {                    
	                    this.question.addAnnotation();
	                }
	        }
	        this.update()
	    }.bind(this));
	});

	
	focusAnnotation(index) {
	    let id = "question_" + this.opts.index + "_annotation_" + index;
	    let inputSelector = "#" + id + " input";
	    this.root.querySelector(inputSelector).focus();
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
	
    setIdFromEvent(event) {
        event.preventUpdate = true;
        if(event.item.anno) {    
            let uri = this.question.authorityData.baseUri;
            if(!uri.endsWith("/")) {
                uri += "/"
            }
            uri += event.target.value;
            event.item.anno.setId(uri);
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

    getIdAsNumber(anno) {
        if(anno.isEmpty()) {
            return "";
        } else {
            return anno.getId().replace(this.question.authorityData.baseUri, "").replace("/", "");
        }
    }

</script>


</authorityResourceQuestion>

