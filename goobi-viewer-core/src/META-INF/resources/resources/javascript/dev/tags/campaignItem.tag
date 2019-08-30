<campaignItem>

	<div class="content">
		<div class="content_left" >
			<imageView if="{this.item}" id="mainImage" source="{this.item.getCurrentCanvas()}" item="{this.item}"></imageView>
			<canvasPaginator if="{this.item}" item="{this.item}"></canvasPaginator>
		</div>
		<div class="content_right">
		
			<span  if="{this.item}" >
				<div class="query_wrapper" each="{query in this.item.queries}">
<!-- 					<h2 class="query_wrapper__title">{viewerJS.getMetadataValue(query.label)}</h2> -->
					<div class="query_wrapper__description">{viewerJS.getMetadataValue(query.description)}</div>
					<plaintextQuery if="{query.queryType == 'PLAINTEXT'}" query="{query}" item="{this.item}"></plaintextQuery>
					<geoLocationQuery if="{query.queryType == 'GEOLOCATION_POINT'}" query="{query}" item="{this.item}"></geoLocationQuery>
				</div>
			</span>
		</div>
	 </div>

<script>

	this.on("mount", function() {
	    fetch(this.opts.source)
	    .then( response => response.json() )
	    .then( itemConfig => this.loadItem(itemConfig));
	});

	loadItem(itemConfig) {
	    
	    this.item = new Crowdsourcing.Item(itemConfig.source, itemConfig.queries, 0)
	    console.log("load item ", this.item);
// 		this.update();
		fetch(this.item.imageSource)
		.then( response => response.json() )
		.then((imageSource) => this.initImageView())
// 		.catch( error => console.error("ERROR ", error));  
	}
	
	initImageView() {
	    this.item.initViewer()
	    .then( function() {	
	    	this.update();
	    }.bind(this) )
	}

	
	resolveCanvas(source) {
	    if(isString(source)) {
	        return fetch(source)
	        .then( response => response.json() );
	    } else {
	        return Q.fcall(() => source);
	    }
	}
	
	isString(variable) {
	    return typeof variable === 'string' || variable instanceof String
	}


</script>

</campaignItem>