<campaignItem>

	<div if="{!opts.pi}" class="crowdsourcing-annotations__content-wrapper">
		{Crowdsourcing.translate("crowdsourcing__error__no_item_available")}
	</div>

	<div if="{opts.pi}" class="crowdsourcing-annotations__content-wrapper">
	<span if="{ this.loading }" class="crowdsourcing-annotations__loader-wrapper">
		<img src="{this.opts.loaderimageurl}" /> 
	</span>
	</span>
		<div class="crowdsourcing-annotations__content-left" >
			<imageView if="{this.item}" id="mainImage" source="{this.item.getCurrentCanvas()}" item="{this.item}"></imageView>
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
					<metadataQuestion if="{question.questionType == 'METADATA'}" question="{question}" item="{this.item}" index="{index}"></metadataQuestion>
				</div>
			</div>
			<campaignItemLog if="{item.showLog}" item="{item}"></campaignItemLog>
			<div if="{!item.isReviewMode()}" class="crowdsourcing-annotations__options-wrapper crowdsourcing-annotations__options-wrapper-annotate">
				<button onclick="{saveAnnotations}" class="crowdsourcing-annotations__options-wrapper__option btn btn--default" id="save">{Crowdsourcing.translate("button__save")}</button>
				<div>{Crowdsourcing.translate("label__or")}</div>
				<button if="{item.isReviewActive()}" onclick="{submitForReview}" class="options-wrapper__option btn btn--success" id="review">{Crowdsourcing.translate("action__submit_for_review")}</button>
				<button if="{!item.isReviewActive()}" onclick="{saveAndAcceptReview}" class="options-wrapper__option btn btn--success" id="review">{Crowdsourcing.translate("action__accept_review")}</button>
				<div>{Crowdsourcing.translate("label__or")}</div>
				<button if="{this.opts.nextitemurl}" onclick="{skipItem}" class="options-wrapper__option btn btn--link" id="skip">{Crowdsourcing.translate("action__skip_item")}</button>
			</div>
			<div if="{item.isReviewActive() && item.isReviewMode()}" class="crowdsourcing-annotations__options-wrapper crowdsourcing-annotations__options-wrapper-review">
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
	    .then(response => this.handleServerResponse(response))
	    .then( itemConfig => this.loadItem(itemConfig))
	    .then( () => this.fetch(this.annotationSource))
	    .then(response => this.handleServerResponse(response))
	    .then( annotations => this.initAnnotations(annotations))
	    .then( () => this.item.notifyItemInitialized())
		.catch( error => {
		   	this.handleError(error);
			this.item = undefined;
	    	this.loading = false;
	    	this.update();
		})
	});


	loadItem(itemConfig) {
	    this.item = new Crowdsourcing.Item(itemConfig, 0);
	    console.log("load item ", this.item);
	    this.item.logEndpoint = this.item.id + "/" + this.opts.pi + "/log/";
	    if(this.opts.currentuserid) {
	        this.item.setCurrentUser(this.opts.currentuserid, this.opts.currentusername, this.opts.currentuseravatar);
	    }
	    this.item.setReviewMode(this.opts.itemstatus && this.opts.itemstatus.toUpperCase() == "REVIEW");
		this.item.onImageRotated( () => this.update());
		fetch(this.item.imageSource)
		.then(response => this.handleServerResponse(response))
		.then( imageSource => this.item.initViewer(imageSource))
		.then( () => this.loading = false)
		.then( () => this.update())
		.then( () => this.item.onImageOpen( () => this.update()));

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
	        viewerJS.notifications.success(Crowdsourcing.translate("crowdsourcing__save_annotations__success"));
		    this.update();
	    });
	}
	
	submitForReview() {
	    this.saveToServer()
	    .then(() => this.setStatus("REVIEW"))
	    .then(() => this.skipItem());
	            
	}

	saveAndAcceptReview() {
	    this.saveToServer()
	    .then(() => this.setStatus("FINISHED"))
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
	            creator: this.item.getCreator().id,
	    }
	    return fetch(this.itemSource + (this.item.currentCanvasIndex + 1 ) + "/", {
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
	
	handleServerResponse(response) {
   		if(!response.ok){
   			try {
   				throw response.json()
   			} catch(error) {
   				response.message = error;
   				throw response;
   			}
   		} else {
   			try {
   				return response.json();
   			} catch(error) {
   				response.message = error;
   				throw response;
   			}
   		}
	}
	
	handleError(error) {
		 console.error("ERROR", error);
		    if(viewerJS.isString(error)) {
		    	viewerJS.notifications.error(error);
		    } else if(error.message && error.message.then) {
		    	error.message.then((err) => {
			    	console.log("error ", err)
			    	let errorMessage = "Error retrieving data from\n\n";
			    	errorMessage += error.url + "\n\n";
			    	if(err.message) {
			    		errorMessage += "Message = " + err.message + "\n\n";
			    	}
			    	errorMessage += "Status = " + error.status;
			    	viewerJS.notifications.error(errorMessage);
		    	})
		    } else {		    	
		    	let errorMessage = "Error retrieving data from\n\n";
		    	errorMessage += error.url + "\n\n";
		    	if(error.message) {
		    		errorMessage += "Message = " + error.message + "\n\n";
		    	}
		    	errorMessage += "Status = " + error.status;
		    	viewerJS.notifications.error(errorMessage);
		    } 
	}


</script>

</campaignItem>