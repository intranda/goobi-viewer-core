<plaintextQuery>

<img class="selected_image" src="{this.imageUrl}"></img>
<input id="textInput" class="text_input"></input>

<script>

	this.on("mount", function() {
	    if(this.opts.query.)
		this.areaSelector = new Crowdsourcing.AreaSelector(this.opts.item, true);
		this.areaSelector.init();
		this.areaSelector.finishedDrawing.subscribe(this.handleFinishedDrawing);
		
		this.item.
	});

	
	handleFinishedDrawing(result) {
	    console.log("Finished drawing ", result);
	    this.imageUrl = this.getImageUrl(result.region, this.opts.item.getImageId(this.opts.item.getCurrentCanvas()));
// 	    this.root.querySelector('.text_input').focus();
	    window.setTimeout(function(){this.root.querySelector('.text_input').focus();}.bind(this),1);
	    this.update();
	}
	
	getImageUrl(rect, imageId) {
	    let url = imageId + "/" + rect.x + "," + rect.y + "," + rect.width + "," + rect.height + "/full/0/default.jpg";
	    return url;
	}

</script>


</plaintextQuery>