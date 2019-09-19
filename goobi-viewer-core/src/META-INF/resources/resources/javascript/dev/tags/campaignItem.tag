<campaignItem>

	<div if="{!opts.pi}" class="content">
		{Crowdsourcing.translate("crowdsourcing__error__no_item_available")}
	</div>

	<div if="{opts.pi}" class="content">
	<span if="{ this.loading }" class="loader_wrapper">
		<img src="{this.opts.loaderimageurl}" />
	</span>
	<span if="{this.error}" class="loader_wrapper">
		<span class="error_message">{this.error.message}</span>
	</span>
	</span>
		<div class="content_left" >
			<imageView if="{this.item}" id="mainImage" source="{this.item.getCurrentCanvas()}" item="{this.item}"></imageView>
			<canvasPaginator if="{this.item}" item="{this.item}"></canvasPaginator>
		</div>
		<div if="{this.item}" class="content_right">
			<h1 class="content_right__title">{Crowdsourcing.translate(this.item.translations.title)}</h1>
			<div class="questions_wrapper"  >
				<div class="question_wrapper" each="{question in this.item.questions}">
					<div class="question_wrapper__description">{Crowdsourcing.translate(question.translations.text)}</div>
					<plaintextQuestion if="{question.questionType == 'PLAINTEXT'}" question="{question}" item="{this.item}"></plaintextQuestion>
					<geoLocationQuestion if="{question.questionType == 'GEOLOCATION_POINT'}" question="{question}" item="{this.item}"></geoLocationQuestion>
				</div>
			</div>
			<div if="{!item.isReviewMode()}" class="options-wrapper options-wrapper-annotate">
				<button onclick="{saveAnnotations}" class="options-wrapper__option" id="save">{Crowdsourcing.translate("button__save")}</button>
				<button onclick="{submitForReview}" class="options-wrapper__option" id="review">{Crowdsourcing.translate("action__submit_for_review")}</button>
				<button if="{this.opts.nextitemurl}" onclick="{skipItem}" class="options-wrapper__option" id="skip">{Crowdsourcing.translate("action__skip_item")}</button>
			</div>
			<div if="{item.isReviewMode()}" class="options-wrapper options-wrapper-review">
				<button onclick="{acceptReview}" class="options-wrapper__option" id="accept">{Crowdsourcing.translate("action__accept_review")}</button>
				<button onclick="{rejectReview}" class="options-wrapper__option" id="reject">{Crowdsourcing.translate("action__reject_review")}</button>
				<button if="{this.opts.nextitemurl}" onclick="{skipItem}" class="options-wrapper__option" id="skip">{Crowdsourcing.translate("action__skip_item")}</button>
			</div>
		</div>
	 </div>

<script>

	this.itemSource = this.opts.restapiurl + "crowdsourcing/campaigns/" + this.opts.campaign + "/" + this.opts.pi;
	this.annotationSource = this.itemSource + "/annotations";
	this.loading = true;
	console.log("item url ", this.itemSource);
	console.log("annotations url ", this.annotationSource);
		
	this.on("mount", function() {
	    fetch(this.itemSource)
	    .then( response => response.json() )
	    .then( itemConfig => this.loadItem(itemConfig))
	    .then( () => fetch(this.annotationSource))
	    .then( response => response.json() )
	    .then( annotations => this.initAnnotations(annotations))
		.catch( error => {
		    console.error("ERROR ", error);
	    	this.error = error;
	    	this.update();
		})
	});

	loadItem(itemConfig) {
	    
	    this.item = new Crowdsourcing.Item(itemConfig, 0);
	    this.item.setReviewMode(this.opts.itemstatus && this.opts.itemstatus.toUpperCase() == "REVIEW");
		fetch(this.item.imageSource)
		.then( response => response.json() )
		.then((imageSource) => this.initImageView())
		.then( () => {this.loading = false;})
		.catch( error => {
		    this.loading = false;
		    console.error("ERROR ", error);  
		})
	    
		this.item.onImageRotated( () => this.update());
	}
	
	initImageView() {
	    this.item.initViewer()
	    .then( function() {	
	    	this.update();
	    }.bind(this) )
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
	    .then( () => this.update())
		.catch( error => console.error("ERROR ", error));  
	}
	
	resetQuestions() {
	    this.item.questions.forEach(question => {
		    question.loadAnnotationsFromLocalStorage();
		    question.initAnnotations();
	    })
	}
	
	saveToServer() {
	    let pages = this.item.loadAnnotationPages();
	    console.log("save annotations ", pages);
	    this.loading = true;
	    this.update();
	    return fetch(this.annotationSource, {
            method: "PUT",
            headers: {
                'Content-Type': 'application/json',
            },
            mode: 'cors', // no-cors, cors, *same-origin
            body: JSON.stringify(pages)
	    })
	    .then(() => this.resetItems())
	    .then(() => {
	        this.loading = false;
		    this.update();
	    })
	}
	
	saveAnnotations() {
	    this.saveToServer()
	    .then(() => this.setStatus("ANNOTATE"));
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
	    console.log("skip to ", this.opts.nextitemurl);
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
            mode: 'cors', // no-cors, cors, *same-origin
            body: JSON.stringify(body)
	    })
	}



</script>

</campaignItem>