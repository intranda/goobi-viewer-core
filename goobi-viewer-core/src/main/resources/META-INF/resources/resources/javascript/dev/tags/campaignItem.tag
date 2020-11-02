<campaignItem>

	<div if="{!opts.pi}" class="crowdsourcing-annotations__content-wrapper">
		{Crowdsourcing.translate("crowdsourcing__error__no_item_available")}
	</div>

	<div if="{opts.pi}" class="crowdsourcing-annotations__content-wrapper">
	<span if="{ this.loading }" class="crowdsourcing-annotations__loader-wrapper">
		<img src="{this.opts.loaderimageurl}" />
	</span>
	<span if="{this.error}" class="crowdsourcing-annotations__loader-wrapper">
		<span class="error_message">{this.error.message}</span>
	</span>
	</span>
		<div class="crowdsourcing-annotations__content-left" >
			<imageView if="{this.item}" id="mainImage" source="{this.item.getCurrentCanvas()}" item="{this.item}"></imageView>
			<canvasPaginator if="{this.item}" item="{this.item}"></canvasPaginator>
		</div>
		<div if="{this.item}" class="crowdsourcing-annotations__content-right">
			<!-- <h1 class="crowdsourcing-annotations__content-right-title">{Crowdsourcing.translate(this.item.translations.title)}</h1>-->
			<div class="crowdsourcing-annotations__questions-wrapper" >
				<div each="{question, index in this.item.questions}" 
					onclick="{setActive}"
					class="crowdsourcing-annotations__question-wrapper {question.isRegionTarget() ? 'area-selector-question' : ''} {question.active ? 'active' : ''}" >
					<div class="crowdsourcing-annotations__question-wrapper-description">{Crowdsourcing.translate(question.text)}</div>
					<plaintextQuestion if="{question.questionType == 'PLAINTEXT'}" question="{question}" item="{this.item}" index="{index}"></plaintextQuestion>
					<richtextQuestion if="{question.questionType == 'RICHTEXT'}" question="{question}" item="{this.item}" index="{index}"></richtextQuestion>
					<geoLocationQuestion if="{question.questionType == 'GEOLOCATION_POINT'}" question="{question}" item="{this.item}" index="{index}"></geoLocationQuestion>
					<authorityResourceQuestion if="{question.questionType == 'NORMDATA'}" question="{question}" item="{this.item}" index="{index}"></authorityResourceQuestion>
				</div>
			</div>
			<div if="{!item.isReviewMode()}" class="crowdsourcing-annotations__options-wrapper crowdsourcing-annotations__options-wrapper-annotate">
				<button onclick="{saveAnnotations}" class="crowdsourcing-annotations__options-wrapper__option btn btn--default" id="save">{Crowdsourcing.translate("button__save")}</button>
				<div>{Crowdsourcing.translate("label__or")}</div>
				<button onclick="{submitForReview}" class="options-wrapper__option btn btn--success" id="review">{Crowdsourcing.translate("action__submit_for_review")}</button>
				<div>{Crowdsourcing.translate("label__or")}</div>
				<button if="{this.opts.nextitemurl}" onclick="{skipItem}" class="options-wrapper__option btn btn--link" id="skip">{Crowdsourcing.translate("action__skip_item")}</button>
			</div>
			<div if="{item.isReviewMode()}" class="crowdsourcing-annotations__options-wrapper crowdsourcing-annotations__options-wrapper-review">
				<button onclick="{acceptReview}" class="options-wrapper__option btn btn--success" id="accept">{Crowdsourcing.translate("action__accept_review")}</button>
				<div>{Crowdsourcing.translate("label__or")}</div>
				<button onclick="{rejectReview}" class="options-wrapper__option btn btn--danger" id="reject">{Crowdsourcing.translate("action__reject_review")}</button>
				<div>{Crowdsourcing.translate("label__or")}</div>
				<button if="{this.opts.nextitemurl}" onclick="{skipItem}" class="options-wrapper__option btn btn--link" id="skip">{Crowdsourcing.translate("action__skip_item")}</button>
			</div>
		</div>
	 </div>

<script>

	this.itemSource = this.opts.restapiurl + "crowdsourcing/campaigns/" + this.opts.campaign + "/" + this.opts.pi + "/";
	this.annotationSource = this.itemSource + "annotations/";
	this.loading = true;
		
	this.on("mount", function() {
	    fetch(this.itemSource)
	    .then( response => response.json() )
	    .then( itemConfig => this.loadItem(itemConfig))
	    .then( () => this.fetch(this.annotationSource))
	    .then( response => response.json() )
	    .then( annotations => this.initAnnotations(annotations))
	    .then( () => this.item.notifyItemInitialized())
		.catch( error => {
		    console.error("ERROR ", error);
	    	this.error = error;
	    	this.update();
		})
	});

	loadItem(itemConfig) {
	    console.log("load item ", itemConfig); 
	    this.item = new Crowdsourcing.Item(itemConfig, 0);
	    this.item.setReviewMode(this.opts.itemstatus && this.opts.itemstatus.toUpperCase() == "REVIEW");
		fetch(this.item.imageSource + "?mode=simple")
		.then( response => response.json() )
		.then( imageSource => this.initImageView(imageSource))
		.then( () => {this.loading = false; this.update()})
		.catch( error => {
		    this.loading = false;
		    console.error("ERROR ", error);  
		})
	    
		this.item.onImageRotated( () => this.update());
	}
	
	initImageView(imageSource) {
	    this.item.initViewer(imageSource)
	    this.update();
	}

	
	resolveCanvas(source) {
	    if(Crowdsourcing.isString(source)) {
	        return fetch(source)
	        .then( response => response.json() );
	    } else {
	        return Q.fcall(() => source);
	    }
	}
	
	initAnnotations(annotations) {
	    let save = this.item.createAnnotationMap(annotations);
	    this.item.saveToLocalStorage(save);
	}
	
	resetItems() {
	    fetch(this.annotationSource)
	    .then( response => response.json() )
	    .then( annotations => this.initAnnotations(annotations))
	    .then( () => this.resetQuestions())
	    .then( () => this.item.notifyAnnotationsReload())
	    .then( () => this.update())
		.catch( error => console.error("ERROR ", error));  
	}
	
	resetQuestions() {
	    this.item.questions.forEach(question => {
		    question.loadAnnotationsFromLocalStorage();
		    question.initAnnotations();
	    })
	}
	
	setActive(event) {
	    if(event.item.question.isRegionTarget()) {	        
		    this.item.questions.forEach(q => q.active = false);
		    event.item.question.active = true;
	    }
	}
 	
	saveToServer() {
	    let pages = this.item.loadAnnotationPages();
	    this.loading = true;
	    this.update();
	    return fetch(this.annotationSource, {
            method: "PUT",
            headers: {
                'Content-Type': 'application/json'
            },
            cache: "no-cache",
            mode: 'cors',
            body: JSON.stringify(pages)
	    })
	}
	
	saveAnnotations() {
	    this.saveToServer()
	    .then(() => this.resetItems())
	    .then(() => this.setStatus("ANNOTATE"))
	    .catch((error) => {
	        console.error(error);
	    })
	    .then(() => {
	        this.loading = false;
		    this.update();
	    });
	}
	
	submitForReview() {
	    this.saveToServer()
	    .then(() => this.setStatus("REVIEW"))
	    .then(() => this.skipItem());
	            
	}

	acceptReview() {
	    this.setStatus("FINISHED")
	    .then(() => this.skipItem());
	}

	rejectReview() {
	    this.setStatus("ANNOTATE")
	    .then(() => this.skipItem());
	}
	
	skipItem() {
	    window.location.href = this.opts.nextitemurl;
	}
	
	setStatus(status) {
	    let body = {
	            recordStatus: status,
	            creator: this.item.getCreator().id 
	    }
	    return fetch(this.itemSource, {
            method: "PUT",
            headers: {
                'Content-Type': 'application/json',
            },
            cache: "no-cache",
            mode: 'cors', // no-cors, cors, *same-origin
            body: JSON.stringify(body)
	    })
	}

	fetch(url) {
	    return fetch(url, {
            method: "GET",
            cache: "no-cache",
            mode: 'cors', // no-cors, cors, *same-origin
	    })
	}


</script>

</campaignItem>