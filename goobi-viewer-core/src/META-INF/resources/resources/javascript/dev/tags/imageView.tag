/**
 * Takes a IIIF canvas object in opts.source. 
 * If opts.item exists, it creates the method opts.item.setImageSource(canvas) 
 * and provides an observable in opts.item.imageChanged triggered every time a new image source is loaded (including the first time)
 * The imageView itself is stored in opts.item.image
 */

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
		this.image = new ImageView.Image(imageViewConfig);
		this.image.load()
		.then( (image) => {
			if(this.opts.item) {
				this.opts.item.image = this.image;
			    var now = Rx.Observable.of(image);
				this.opts.item.setImageSource = function(source) {
				    this.image.setTileSource(this.getImageInfo(source));
				}.bind(this);
			    this.opts.item.notifyImageOpened(image.observables.viewerOpen.map(image).merge(now));
			}
			return image;
		})
		.then(function() {
		  	this.update();
		}.bind(this));
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

	
	</script>

</imageView>

