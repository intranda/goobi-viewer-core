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
		<imageControls if="{this.image}" 
			image="{this.image}"
			rotate="{this.rotate}"
			imageindex="{this.opts.item.currentCanvasIndex}"
			imagecount="{this.opts.item.canvases.length}"
			actionlistener="{this.actionListener}" 
			showthumbs="{this.showThumbs}"
			class="{this.showThumbs ? 'd-none' : ''}">
		</imageControls>
		
		<div class="image_container {this.showThumbs ? 'd-none' : ''}">
			<div id="image_{opts.id}" class="image"></div>
		</div>
		
		<div class="image_thumbnails-wrapper  {this.opts.item.reviewMode ? 'reviewmode' : ''} {this.showThumbs ? '' : 'd-none'}">
			<div class="thumbnails-filters">
<!-- 				<button ref="filter_annotated" class="thumbnails-filter-annotated btn btn--clean"></button> -->
<!-- 				<button ref="filter_finished" class="thumbnails-filter-finished btn btn--clean"></button> -->
				<button ref="filter_unfinished" class="thumbnails-filter-unfinished btn btn--clean">{Crowdsourcing.translate("crowdsourcing__campaign_filter_show_unfinished")}</button>
				<button ref="filter_reset" class="thumbnails-filter-reset btn btn--clean">{Crowdsourcing.translate("crowdsourcing__campaign_filter_show_all")}</button>
			</div>
			
			<thumbnails class="image_thumbnails" 
				source="{{items: this.opts.item.canvases}}"  
				actionlistener="{this.actionListener}" 
				imagesize=",200"
				index="{this.opts.item.currentCanvasIndex}" 
				statusmap="{getPageStatusMap()}"/>
		</div>
		
		<!-- <canvasPaginator if="{!this.showThumbs}" items="{this.opts.item.canvases}" index="{this.opts.item.currentCanvasIndex}" actionlistener="{this.actionListener}" ></canvasPaginator> -->
	</div> 
	

	<script>

	// INIT TOOLTIPS'TIPS
	this.on("updated", function() {
		this.initTooltips();
	});
	
	// MOUNT IMAGE VIEW
	this.on("mount", function() {
		this.showThumbs = this.isShowThumbs();
		this.initFilters();
		//console.log("mount image view ", this.opts.item);
		$("#controls_" + opts.id + " .draw_overlay").on("click", () => this.drawing = true);
		try{		    
			const tileSource = this.getImageInfo(opts.source);
			this.image = new ImageView.Image(imageViewConfig);
			this.zoom = new ImageView.Controls.Zoom(this.image);
			this.rotate = new ImageView.Controls.Rotation(this.image);
			this.image.load(tileSource)
			.then( (event) => {
				if(this.opts.item) {
					this.opts.item.image = this.image;
					//image load notifications
				    var now = rxjs.of(this.image);
					this.opts.item.setImageSource = function(source) {
						console.log("set image source", source);
					    this.update();
					    this.image.load(this.getImageInfo(source))
					    .then(e => this.zoom.goHome());
					}.bind(this);
				    this.opts.item.notifyImageOpened(this.image.onOpened.pipe(rxjs.operators.map( () => this.image),rxjs.operators.merge(now)));
				}
				return image;
			})
		} catch(error) {
		    console.error("ERROR ", error);
	    	this.error = error;
	    	this.update();
		}
		
		this.actionListener = new rxjs.Subject();
		this.actionListener.subscribe((event) => this.handleImageControlAction(event));
		if(this.opts.item.setShowThumbs) {
		    this.opts.item.setShowThumbs.subscribe(show => {
		        this.showThumbs = show;
		        this.update();
		    });
		}
	})

	// TOOLTIPS FOR PAGE STATUS	
	initTooltips() {
	    $('.thumbnails-image-wrapper.review').tooltip('dispose');
	    $('.thumbnails-image-wrapper.review').tooltip({
	        placement: 'top',
	      title: Crowdsourcing.translate("crowdsourcing__campaign_tooltip_in_review"),
	      trigger: 'hover'
	    });
	    
	    $('.thumbnails-image-wrapper.finished').tooltip('dispose');
	    $('.thumbnails-image-wrapper.finished').tooltip({
	        placement: 'top',
	      title: Crowdsourcing.translate("crowdsourcing__campaign_tooltip_completed"),
	      trigger: 'hover'
	    });

	    // UPDATE INTERVAL FOR TOOLTIP FOR LOCKED PAGES
	    function updateLockedTooltip() {
	    	$('.thumbnails-image-wrapper.locked').tooltip('dispose');
		    $('.thumbnails-image-wrapper.locked').tooltip({
		      placement: 'top',
		      title: Crowdsourcing.translate("crowdsourcing__campaign_tooltip_locked"),
		      trigger: 'hover'
		    });
		    
		    $(".thumbnails-image-wrapper.locked").each(function() {
				if ($(this).is(":hover")) {
    				$(this).tooltip('show');
			  }
			})

		    setTimeout(updateLockedTooltip, 4000);
	    }
	    updateLockedTooltip();

 
	}
	
	initFilters() {
    	this.refs.filter_unfinished.onclick = event => {
    		$('.thumbnails-image-wrapper').show();
    		$('.thumbnails-image-wrapper.review').hide();
    		$('.thumbnails-image-wrapper.annotate').hide();
    		$('.thumbnails-image-wrapper.finished').hide();
    	};

    	this.refs.filter_reset.onclick = event => {
    		$('.thumbnails-image-wrapper').show();
    	};

	}
	
	$( document ).ready(function() {
	    // FILTER ACTIVE STATE HIGHLIGHTING
	    $('.thumbnails-filter-reset').addClass('-activeFilter');
	    $('.thumbnails-filter-reset, .thumbnails-filter-finished, .thumbnails-filter-unfinished, .thumbnails-filter-annotated').click(function() {
	    	$('.thumbnails-filter-reset, .thumbnails-filter-finished, .thumbnails-filter-unfinished, .thumbnails-filter-annotated').removeClass('-activeFilter');
	    	$(this).addClass('-activeFilter');
	    });
	});

    $('.image_thumbnails-wrapper.reviewmode .thumbnails-image-wrapper:not(".image_thumbnails-wrapper.reviewmode .thumbnails-image-wrapper.finished")').tooltip('dispose');
	
	getPosition() {
		let pos_os = this.dataPoint.getPosition();
		let pos_image = ImageView.CoordinateConversion.scaleToImage(pos_os, this.image.viewer, this.image.getOriginalImageSize());
		let pos_image_rot = ImageView.CoordinateConversion.convertPointFromImageToRotatedImage(pos_image, this.image.controls.getRotation(), this.image.getOriginalImageSize());
		return pos_image_rot;
	}
	
	handleImageControlAction(event) {
		switch(event.action) {
			case "toggleThumbs":
				this.showThumbs = event.value;
				this.update();
				break;
			case "rotate":
		        if(this.opts.item) {
		            this.opts.item.notifyImageRotated(event.value);
		        }
		        break;	
			case "clickImage":
				this.showThumbs = false;
			case "setImageIndex": 
			    this.opts.item.loadImage(event.value, true);
			    break;
			case "previousImage":
			    this.opts.item.loadImage(this.opts.item.getPreviousAccessibleIndex(this.opts.item.currentCanvasIndex), true);
				break;
			case "nextImage":
			    this.opts.item.loadImage(this.opts.item.getNextAccessibleIndex(this.opts.item.currentCanvasIndex), true);
			    break;

		}
	}
	
	getImageInfo(canvas) {
	    return canvas.images[0].resource.service["@id"] + "/info.json"
	}
	
	getPageStatusMap() {
			return this.opts.item.pageStatusMap;
	}
	
	isShowThumbs() {
		if(this.opts.item.reviewMode && this.opts.item.pageStatisticMode) {
			//count canvases in REVIEW status
			let count = 0;
			for(let status of this.opts.item.pageStatusMap.values()) {
			    if(status.toUpperCase() == "REVIEW") {
					count++;
				}
			}
			return count !== 1; 
		} else {			
			return this.opts.item.canvases.length > 1
		}
	}
	
	const imageViewConfig = {
			element: "#image_" + opts.id,
			fittingMode: "fixed"
	};
	
	const drawStyle = {
			borderWidth: 2,
			borderColor: "#2FEAD5"
	}
	
	const lineStyle = {
			lineWidth : 1,
			lineColor : "#EEC83B"
	}
	
	const pointStyle = ImageView.DataPoint.Point.getPointStyle(20, "#EEC83B");

	</script>

</imageView>

