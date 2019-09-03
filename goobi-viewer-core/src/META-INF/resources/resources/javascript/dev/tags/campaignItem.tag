<campaignItem>

	<div class="content">
		<div class="content_left" >
			<imageView if="{this.item}" id="mainImage" source="{this.item.getCurrentCanvas()}" item="{this.item}"></imageView>
			<canvasPaginator if="{this.item}" item="{this.item}"></canvasPaginator>
		</div>
		<div class="content_right">
		
			<div class="queries_wrapper" if="{this.item}" >
				<div class="query_wrapper" each="{query in this.item.queries}">
<!-- 					<h2 class="query_wrapper__title">{viewerJS.getMetadataValue(query.label)}</h2> -->
					<div class="query_wrapper__description">{viewerJS.getMetadataValue(query.translations.description)}</div>
					<plaintextQuery if="{query.queryType == 'PLAINTEXT'}" query="{query}" item="{this.item}"></plaintextQuery>
					<geoLocationQuery if="{query.queryType == 'GEOLOCATION_POINT'}" query="{query}" item="{this.item}"></geoLocationQuery>
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

	this.itemSource = this.opts.restapiurl + "crowdsourcing/campaign/" + this.opts.campaign + "/annotate/" + this.opts.pi;
	console.log("url ", this.itemSource);
	this.on("mount", function() {
	    fetch(this.itemSource)
	    .then( response => response.json() )
	    .then( itemConfig => this.loadItem(itemConfig));
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
	
	resetItems() {
	    this.item.queries.forEach( function(query) {
	        query.deleteFromLocalStorage();
        	query.resetAnnotations();
	    });
		this.update();
	}
	
	saveToServer() {
	    this.item.queries.forEach(function(query) {
	        let annoMap = query.getAnnotationsFromLocalStorage();
	        let json = [];
	        annoMap.forEach(function(pageAnnotations, pageId) {
				json.push({"id": pageId, "annotations": pageAnnotations});
	        })
	        console.log("send ", json, " to ", this.itemSource);
	        fetch(this.itemSource, {
	            method: "PUT",
	            headers: {
	                'Content-Type': 'application/json',
	                // 'Content-Type': 'application/x-www-form-urlencoded',
	            },
	            mode: 'cors', // no-cors, cors, *same-origin
	            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
	            body: JSON.stringify(json)
	        })
	        
	        let saveString = JSON.stringify(json);
	        console.log("PUT to server ", saveString);
	    }.bind(this))
	}
	
	submitForReview() {
	    //TODO: change item status
	}



</script>

</campaignItem>