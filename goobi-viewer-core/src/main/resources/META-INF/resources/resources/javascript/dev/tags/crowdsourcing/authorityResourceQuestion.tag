<authorityResourceQuestion>
	<div if="{this.showInstructions()}" class="crowdsourcing-annotations__instruction">
		<label>{Crowdsourcing.translate("crowdsourcing__help__create_rect_on_image")}</label>
	</div>
	<div if="{this.showInactiveInstructions()}" class="crowdsourcing-annotations__single-instruction -inactive">
		<label>{Crowdsourcing.translate("crowdsourcing__help__make_active")}</label>
	</div>
	<div class="crowdsourcing-annotations__wrapper" id="question_{opts.index}_annotation_{index}" each="{anno, index in this.question.annotations}">
	
		<div class="crowdsourcing-annotations__annotation-area -small" >
			<div if="{this.showAnnotationImages()}" class="crowdsourcing-annotations__annotation-area-image" style="border-color: {anno.getColor()}">
				<img src="{this.question.getImage(anno)}"></img>
			</div>

			<div if="{ !this.opts.item.isReviewMode() }" class="crowdsourcing-annotations__question-text-input">
				<span class="crowdsourcing-annotations__gnd-text">https://d-nb.info/gnd/</span>
				<input class="crowdsourcing-annotations__gnd-id form-control" onChange="{setIdFromEvent}" value="{question.authorityData.baseUri && getIdAsNumber(anno)}">
				</input>
			</div>
			
			<!-- FOR REVIEW MODE: DISABLED TEXT INPUT FIELD AND LINK FOR  -->
			<div if="{ this.opts.item.isReviewMode() }" class="crowdsourcing-annotations__question-text-input">
				<input class="form-control pl-1" disabled="{this.opts.item.isReviewMode() ? 'disabled' : ''}" value="{question.authorityData.baseUri}{getIdAsNumber(anno)}">
				</input>
				<div if="{ this.opts.item.isReviewMode() }" class="crowdsourcing-annotations__jump-to-gnd">
					<a target ="_blank" href="{question.authorityData.baseUri}{getIdAsNumber(anno)}">{Crowdsourcing.translate("cms_menu_create_item_new_tab")}</a>
				</div>
			</div>

			<div class="cms-module__actions crowdsourcing-annotations__annotation-action">
				<button if="{ !this.opts.item.isReviewMode() }" onClick="{deleteAnnotationFromEvent}" class="crowdsourcing-annotations__delete-annotation btn btn--clean delete">{Crowdsourcing.translate("action__delete_annotation")}
				</button>
			</div>
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
	                    //reset dirty flag of item  set by addAnnotation(). 
	                    //Automatic creation of annotation should not set status to dirty
	                    this.opts.item.dirty = false; 
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

	showLinkToGND() {
	    return this.question.isReviewMode() && this.question.isRegionTarget();
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

