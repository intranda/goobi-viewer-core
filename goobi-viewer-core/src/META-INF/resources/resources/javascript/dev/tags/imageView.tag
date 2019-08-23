<imageView>
	<div id="wrapper_{opts.id}" class="imageview_wrapper">
	
		<imageControls if="{this.image}" image="{this.image}"></imageControls>
	
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
		console.log("load image ", imageViewConfig);
		this.image = new ImageView.Image(imageViewConfig);
		this.loadImage(this.image);
		this.opts.loader.updateImageSource = () => this.image.setTileSource(this.getImageInfo(this.opts.loader.getCurrentImage()));
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
				image : {
					tileSource : this.getImageInfo(opts.source)
				}
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
	
		loadImage(image) {
			image.load()
			.then(function(image) {	
				const drawer = new ImageView.Draw(image.viewer, drawStyle, () => this.drawing);
				drawer.finishedDrawing().subscribe(function(rect) {
					this.drawing = false;
					rect.draw();
				}.bind(this))			
			}.bind(this))
			.then( () => this.update());
		}
	
	
	</script>

</imageView>

