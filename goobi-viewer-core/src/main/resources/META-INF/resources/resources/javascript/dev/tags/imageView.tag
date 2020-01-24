/**
 * Takes a IIIF canvas object in opts.source. 
 * If opts.item exists, it creates the method opts.item.setImageSource(canvas) 
 * and provides an observable in opts.item.imageChanged triggered every time a new image source is loaded (including the first time)
 * The imageView itself is stored in opts.item.image
 */

<imageView>
	<div id="wrapper_{opts.id}" class="imageview_wrapper">
		<span if="{this.error}" class="loader_wrapper">
			<span class="error_message">{this.error.message}</span>
		</span>
		<imageControls if="{this.image}" image="{this.image}" item="{this.opts.item}"></imageControls>
	
		<div class="image_container">
			<div id="image_{opts.id}" class="image"></div>
		</div>
	</div>

	<script>

	getPosition() {
		let pos_os = this.dataPoint.getPosition();
		let pos_image = ImageView.CoordinateConversion.scaleToImage(pos_os, this.image.viewer, this.image.getOriginalImageSize());
		let pos_image_rot = ImageView.CoordinateConversion.convertPointFromImageToRotatedImage(pos_image, this.image.controls.getRotation(), this.image.getOriginalImageSize());
		return pos_image_rot;
	}
	
	
	this.on("mount", function() {
		$("#controls_" + opts.id + " .draw_overlay").on("click", function() {
			this.drawing=true; 
		}.bind(this));
		try{		    
			imageViewConfig.image.tileSource = this.getImageInfo(opts.source);
			this.image = new ImageView.Image(imageViewConfig);
			this.image.load()
			.then( (image) => {
				if(this.opts.item) {
					this.opts.item.image = this.image;
					//image load notifications
				    var now = Rx.of(image);
					this.opts.item.setImageSource = function(source) {
					    this.image.setTileSource(this.getImageInfo(source));
					}.bind(this);
				    this.opts.item.notifyImageOpened(image.observables.viewerOpen.pipe(RxOp.map( () => image),RxOp.merge(now)));
				}
				return image;
			})
			.then(function() {
			  	this.update();
			}.bind(this));
		} catch(error) {
		    console.error("ERROR ", error);
	    	this.error = error;
	    	this.update();
		}
	})
	
	
	getImageInfo(canvas) {
	    return canvas.images[0].resource.service["@id"] + "/info.json"
	}
	
	const imageViewConfig = {
			global : {
				divId : "image_" + opts.id,
				fitToContainer: true,
				adaptContainerWidth: false,
				adaptContainerHeight: false,
				footerHeight: 00,
				zoomSpeed: 1.3,
				allowPanning : true,
			},
			image : {}
	};
	
	const drawStyle = {
			borderWidth: 2,
			borderColor: "#2FEAD5"
	}
	
	const lineStyle = {
			lineWidth : 1,
			lineColor : "#EEC83B"
	}
	
	const pointStyle = ImageView.DataPoint.getPointStyle(20, "#EEC83B");

	
	</script>

</imageView>

