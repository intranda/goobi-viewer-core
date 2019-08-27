<plaintextQuery>

	<div id="answer_{index}" class="annotation_area" each="{answer, index in this.answers}">
		
		<div if="{this.showAnswerImages()}" class="annotation_area__image">
			<img  src="{this.getImage(answer.selector)}"></img>
		</div>
		<div class="annotation_area__text_input">
			<label>{viewerJS.getMetadataValue(this.opts.query.label)}</label>			
			<input  onChange="{answer.setTextFromEvent}" value="{answer.body.text}"></input>
		</div>
	
	</div>

<script>

	this.queryType = Crowdsourcing.Query.getType(this.opts.query);
	this.targetFrequency = Crowdsourcing.Query.getFrequency(this.opts.query);
	this.targetSelector = Crowdsourcing.Query.getSelector(this.opts.query);
	
	console.log("this.queryType = ", this.queryType);
	console.log("this.targetFrequency = ", this.targetFrequency);
	console.log("this.targetSelector = ", this.targetSelector);

	this.answers = [];
	
	this.on("mount", function() {
	    
	    switch(this.targetSelector) {
	        case Crowdsourcing.Query.Selector.RECTANGLE:
	            this.initAreaSelector();
	            break;
	        case Crowdsourcing.Query.Selector.WHOLE_SOURCE:
	        case Crowdsourcing.Query.Selector.WHOLE_PAGE:
	    }
	    
	    switch(this.targetFrequency) {
	        case Crowdsourcing.Query.Frequency.ONE_PER_CANVAS:
	        case Crowdsourcing.Query.Frequency.MULTIPLE_PER_CANVAS:
	    		this.opts.item.onImageOpen( () => this.resetAnswers());
	    }

	});
	
	this.on("updated", function() {
		if(this.answers.length > 0) {
		    let answerId = "answer_" + (this.answers.length-1);
		    let inputSelector = "#"+answerId + " input";
		    window.setTimeout(function(){this.root.querySelector(inputSelector).focus();}.bind(this),1);

		}
	
	}.bind(this));
	
	showAnswerImages() {
	    return this.targetSelector === Crowdsourcing.Query.Selector.RECTANGLE;
	}
	
	resetAnswers() {
	    this.answers = [];
	    
	    switch(this.targetSelector) {
	        case Crowdsourcing.Query.Selector.WHOLE_PAGE:
	        case Crowdsourcing.Query.Selector.WHOLE_SOURCE:
	            this.answers.push(new Crowdsourcing.Answer());
	    }
	    this.update();
	}
	
	initAreaSelector() {
		this.areaSelector = new Crowdsourcing.AreaSelector(this.opts.item, true);
		this.areaSelector.init();
		this.areaSelector.finishedDrawing.subscribe(this.handleFinishedDrawing);
		this.opts.item.onImageOpen( () => this.areaSelector.reset());
	}
	
	getId(answer) {
	    return this.answers.indexOf(answer);
	}

	getImage(answer) {
	    return this.getImageUrl(answer.region, this.opts.item.getImageId(this.opts.item.getCurrentCanvas()));
	}
	
	handleFinishedDrawing(result) {
	    console.log("Finished drawing ", result);
	    this.answers.push(new Crowdsourcing.Answer({}, result));
	    this.update();
	}
	
	getImageUrl(rect, imageId) {
	    let url = imageId + "/" + rect.x + "," + rect.y + "," + rect.width + "," + rect.height + "/full/0/default.jpg";
	    return url;
	}

</script>


</plaintextQuery>