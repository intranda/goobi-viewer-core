<plaintextQuestion>
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
			<div class="crowdsourcing-annotations__question-text-input">
				<textarea disabled="{this.opts.item.isReviewMode() ? 'disabled' : ''}" onChange="{setTextFromEvent}" value="{anno.getText()}">
				</textarea>
			</div>
		</div>
		<div class="cms-module__actions crowdsourcing-annotations__annotation-action">
			<button if="{ !this.opts.item.isReviewMode() }" onClick="{deleteAnnotationFromEvent}" class="crowdsourcing-annotations__delete-annotation btn btn--clean delete">{Crowdsourcing.translate("action__delete_annotation")}
			</button>
		</div>
	</div>
	<button if="{showAddAnnotationButton()}" onclick="{addAnnotation}" class="options-wrapper__option btn btn--default" id="add-annotation">{Crowdsourcing.translate("action__add_annotation")}</button>
	

<script>

	this.question = this.opts.question;

	this.on("mount", function() {
	    this.question.initializeView((anno) => new Crowdsourcing.Annotation.Plaintext(anno), this.update, this.update, this.focusAnnotation);	    
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
	    let inputSelector = "#" + id + " textarea";
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
	
    setTextFromEvent(event) {
        console.log("set text");
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
        this.question.addAnnotation();
        this.question.focusCurrentAnnotation();
    }



</script>


</plaintextQuestion>