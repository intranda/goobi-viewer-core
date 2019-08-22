<imageView>
	<div id="wrapper_{opts.id}" class="wrapper">
	
		<div class="image_container">
			<div id="image_{opts.id}" class="image"></div>
		</div>
	
		<div id="controls_{opts.id}" class="controls">
		<div>
			<button class="controls__item fa fa-rotate-left" onclick="{rotateLeft}"></button>
			<button class="controls__item fa fa-rotate-right" onclick="{rotateRight}"></button>
		</div>
			<button class="controls__item draw_overlay" >Draw Overlay</button>
			<input type="range" class="custom-range controls__item rotation_slider" step="0.01" min="-20" max="20">
			<label id="rotationLabel">0</label>
			<div>
				<label>Point coordinates: </label>
				<label id="coordinatesLabel"></label>
			</div>
		</div>
	</div>

	<script>
	
	rotateLeft() {
		if(this.image) {
			this.image.controls.rotateLeft();
		}
	}
	
	rotateRight() {
		if(this.image) {
			this.image.controls.rotateRight();
		}
	}

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
		this.loadImage(this.image);
		
		$("#controls_" + opts.id + " .rotation_slider").on("input", function(event) {
			let element = $(event.target);
			let rot = parseFloat($(element).val());
// 			image.rotate(rot);
			this.image.controls.rotateTo(parseFloat($(element).val()));
			$("#rotationLabel").html(this.image.controls.getRotation().toFixed(2));
			$("#coordinatesLabel").html(ImageView.DataPoint.convertPointsToString(this.getPosition(), 2));
		}.bind(this))
		
		$("#controls_" + opts.id + " button").on("click", function(event) {
			$("#rotationLabel").html(this.image.controls.getRotation().toFixed(2));
			$("#coordinatesLabel").html(ImageView.DataPoint.convertPointsToString(this.getPosition(), 2));
		}.bind(this));
	})
	
		const imageViewConfig = {
				global : {
					divId : "image_" + opts.id,
					fitToContainer: true,
					adaptContainerWidth: true,
					adaptContainerHeight: false,
					footerHeight: 00,
					zoomSpeed: 1.1,
					allowPanning : true,
				},
				image : {
					tileSource : opts.source.images[0].resource.service["@id"]
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
				
				let p = new OpenSeadragon.Point(0.2, image.getSizes().getAspectRatio()/2.0);
				let lineStyleY = ImageView.DataPoint.getLineStyle(4, "#0803C4");
				let pointStyle = ImageView.DataPoint.getPointStyle(6, "#FF063C");
				this.dataPoint = new ImageView.DataPoint(p, image.viewer, ImageView.DataPoint.getStyle(pointStyle, undefined, lineStyle));
				this.dataPoint.draw();

				const lineMover = new ImageView.DataPoint.Transform(image.viewer,  () => true); 
				lineMover.addDataPoint(this.dataPoint);
				lineMover.finishedTransforming().subscribe(function(p) {
					$("#coordinatesLabel").html(ImageView.DataPoint.convertPointsToString(this.getPosition(), 2));
					console.log("line position ", p.point);
				}.bind(this))
								
			}.bind(this))
		}
	
	
	</script>

</imageView>

