<campaignItem>

	<div class="content">
		<div class="content_left" >
			<imageView if="{this.item}" id="mainImage" source="{this.item.getCurrentCanvas()}" item="{this.item}"></imageView>
			<canvasPaginator if="{this.item}" item="{this.item}"></canvasPaginator>
		</div>
		<div if="{this.item}" class="content_right">
			<h1 class="content_right__title">{viewerJS.getMetadataValue(this.item.translations.title)}</h1>
			<div class="content_right__description">{viewerJS.getMetadataValue(this.item.translations.description)}</div>
			<div class="questions_wrapper"  >
				<div class="question_wrapper" each="{question in this.item.questions}">
					<plaintextQuestion if="{question.questionType == 'PLAINTEXT'}" question="{question}" item="{this.item}"></plaintextQuestion>
					<geoLocationQuestion if="{question.questionType == 'GEOLOCATION_POINT'}" question="{question}" item="{this.item}"></geoLocationQuestion>
				</div>
			</div>
			<div class="options-wrapper">
				<button onclick="{resetItems}" class="options-wrapper__option" id="restart">{Crowdsourcing.translate("action__restart")}</button>
				<button onclick="{saveToServer}" class="options-wrapper__option" id="save">{Crowdsourcing.translate("button__save")}</button>
				<button onclick="{submitForReview}" class="options-wrapper__option" id="review">{Crowdsourcing.translate("action__submit_for_review")}</button>
			</div>
		</div>
	 </div>

<script>

	this.itemSource = this.opts.restapiurl + "crowdsourcing/campaigns/" + this.opts.campaign + "/" + this.opts.pi;
	this.annotationSource = this.itemSource + "/annotations";
	console.log("item url ", this.itemSource);
	console.log("annotations url ", this.annotationSource);
	this.on("mount", function() {
	    fetch(this.itemSource)
	    .then( response => response.json() )
	    .then( itemConfig => this.loadItem(itemConfig))
	    .then( () => fetch(this.annotationSource))
	    .then( response => response.json() )
	    .then( annotations => this.initAnnotations(annotations))
		.catch( error => console.error("ERROR ", error));  
	});

	loadItem(itemConfig) {
	    
	    this.item = new Crowdsourcing.Item(itemConfig, 0)
		fetch(this.item.imageSource)
		.then( response => response.json() )
		.then((imageSource) => this.initImageView())
		.catch( error => console.error("ERROR ", error));  
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
	    fetch(this.annotationSource, {
            method: "PUT",
            headers: {
                'Content-Type': 'application/json',
            },
            mode: 'cors', // no-cors, cors, *same-origin
            body: JSON.stringify(pages)
	    })
	    .then(() => this.resetItems());
	}
	
	submitForReview() {
	    //TODO: change item status
	}



</script>

</campaignItem>