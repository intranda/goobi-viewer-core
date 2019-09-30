<questionTemplate>
	<div if="{showInstructions()}" class="annotation_instruction">
		<label>{Crowdsourcing.translate("crowdsourcing__help__create_rect_on_image")}</label>
	</div>
	<div if="{showInactiveInstructions()}" class="annotation_instruction annotation_instruction_inactive">
		<label>{Crowdsourcing.translate("crowdsourcing__help__make_active")}</label>
	</div>
	<div class="annotation_wrapper" id="question_{opts.index}_annotation_{index}" each="{anno, index in this.question.annotations}">
		<div class="annotation_area" >
		</div>
	</div>
	<button if="{showAddAnnotationButton()}" onclick="{addAnnotation}" class="options-wrapper__option btn btn--default" id="add-annotation">{Crowdsourcing.translate("action__add_annotation")}</button>
	

<script>

this.question = this.opts.question;

/**
 * Must call this.question.initializeView with the following parameters:
 * * createNewAnnotation: return an instance of the desired annotationtype, based on the given w3c annotation in json
 * * onAddAnnotation: Method to call on adding new annotation; the added annotation is passed as parameter
 * * onUpdateAnnotation: Method to call on updating an annotation; the annotation updated is passed as parameter
 * * focusAnnotation: focus the annotation with the given index
 */
this.on("mount", function() {
    this.question.initializeView((anno) => new Crowdsourcing.Annotation.Implementation(anno), this.update, this.update, this.focusAnnotation);	    
    this.opts.item.onImageOpen(function() {
        this.update()
    }.bind(this));
});

/**
 * ocus the annotation with the given index
 */
focusAnnotation(index) {
    let id = "question_" + this.opts.index + "_annotation_" + index;
    let inputSelector = "#" + id + " textarea";
    this.root.querySelector(inputSelector).focus();
}

/**
 * check if instructions on how to create a new annotation should be shown
 */
showInstructions() {
    return !this.opts.item.isReviewMode() &&  this.question.active && this.question.targetSelector == Crowdsourcing.Question.Selector.RECTANGLE && this.question.annotations.length == 0;
}

/**
 * check if instructions to acivate this question should be shown, in order to be able to create annotations for this question
 */
showInactiveInstructions() {
    return !this.opts.item.isReviewMode() &&  !this.question.active && this.question.targetSelector == Crowdsourcing.Question.Selector.RECTANGLE && this.opts.item.questions.filter(q => q.isRegionTarget()).length > 1;

}

/**
 * check if a button to add new annotations should be shown
 */
showAddAnnotationButton() {
    return !this.question.isReviewMode() && !this.question.isRegionTarget() && this.question.mayAddAnnotation();
}

/**
 * template method to change the body of an annotation based on an event
 */
setBodyFromEvent(event) {
    event.preventUpdate = true;
    if(event.item.anno) {            
        //set body
        this.question.saveToLocalStorage();
    } else {
        throw "No annotation to set"
    }
}

/**
 * delete an annotation from an event on an element with the attribute 'anno' containing the annotation to delete
 */
deleteAnnotationFromEvent(event) {
    if(event.item.anno) {
        this.question.deleteAnnotation(event.item.anno);
        this.update();
    }
}

/**
 * Create a new annotation and focus it
 */
addAnnotation() {
    this.question.addAnnotation();
    this.question.focusCurrentAnnotation();
}



</script>


</questionTemplate>

