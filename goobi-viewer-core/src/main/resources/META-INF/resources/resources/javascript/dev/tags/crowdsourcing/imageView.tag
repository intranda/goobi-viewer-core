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
			item="{this.opts.item}" 
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
				<button ref="filter_unfinished" class="thumbnails-filter-unfinished btn btn--clean">Show unfinished</button>
				<button ref="filter_reset" class="thumbnails-filter-reset btn btn--clean">Show all</button>
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
	
	// INIT TOOLTIPS
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
			imageViewConfig.image.tileSource = this.getImageInfo(opts.source);
			this.image = new ImageView.Image(imageViewConfig);
			this.image.load()
			.then( (image) => {
				if(this.opts.item) {
					this.opts.item.image = this.image;
					//image load notifications
				    var now = rxjs.of(image);
					this.opts.item.setImageSource = function(source) {
					    this.image.setTileSource(this.getImageInfo(source));
					}.bind(this);
				    this.opts.item.notifyImageOpened(image.observables.viewerOpen.pipe(rxjs.operators.map( () => image),rxjs.operators.merge(now)));
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

	})

	// TOOLTIPS FOR PAGE STATUS	
	initTooltips() {
	    $('.thumbnails-image-wrapper.review').tooltip({
	        placement: 'top',
	      title: 'In review',
	      trigger: 'hover'
	    });
	    
// 	    $('.thumbnails-image-wrapper.annotate').tooltip({
// 	        placement: 'top',
// 	      title: 'There are already annotations for this page',
// 	      trigger: 'hover'
// 	    });
	    
	    $('.thumbnails-image-wrapper.finished').tooltip({
	        placement: 'top',
	      title: 'Completed',
	      trigger: 'hover'
	    });

	    // UPDATE INTERVAL FOR TOOLTIP FOR LOCKED PAGES
	    function updateLockedTooltip() {
	    	$('.thumbnails-image-wrapper.locked').tooltip('dispose');
		    $('.thumbnails-image-wrapper.locked').tooltip({
		        placement: 'top',
		      title: 'Locked by other user',
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
	    
	    // FILTERING TOOLTIPS
// 	    $('.thumbnails-filter-unfinished').tooltip({
// 	        placement: 'top',
// 	      title: 'Show unfinished pages',
// 	      trigger: 'hover'
// 	    });
	    
// 	    $('.thumbnails-filter-finished').tooltip({
// 	        placement: 'top',
// 	      title: 'Show finished pages',
// 	      trigger: 'hover'
// 	    });
	    
// 	    $('.thumbnails-filter-reset').tooltip({
// 	        placement: 'top',
// 	      title: 'Show all',
// 	      trigger: 'hover'
// 	    });

// 	    $('.thumbnails-filter-annotated').tooltip({
// 	        placement: 'top',
// 	      title: 'Show annotated pages',
// 	      trigger: 'hover'
// 	    });
	    
 
	}
	
	initFilters() {
    	this.refs.filter_unfinished.onclick = event => {
    		$('.thumbnails-image-wrapper').show();
    		$('.thumbnails-image-wrapper.review').hide();
    		$('.thumbnails-image-wrapper.annotate').hide();
    		$('.thumbnails-image-wrapper.finished').hide();
    	};
    	
//     	this.refs.filter_finished.onclick = event => {
//     		$('.thumbnails-image-wrapper').hide();
//     		$('.thumbnails-image-wrapper.review').show();
//     	};
    	
    	this.refs.filter_reset.onclick = event => {
    		$('.thumbnails-image-wrapper').show();
    	};
    	
//     	this.refs.filter_annotated.onclick = event => {
//     		$('.thumbnails-image-wrapper').hide();
//     		$('.thumbnails-image-wrapper.annotate').show();
//     	};
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
		//console.log("event", event);
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
				this.opts.item.loadImage(event.value);
		}
	}
	
	getImageInfo(canvas) {
	    return canvas.images[0].resource.service["@id"] + "/info.json"
	}
	
	getPageStatusMap() {
		return this.opts.item.pageStatusMap;
	}
	
	isShowThumbs() {
		if(this.opts.item.reviewMode) {
			//cound canvases in REVIEW status
			let count = 0;
			for(let status of this.opts.item.pageStatusMap.values()) {
				if(status == "REVIEW") {
					count++;
				}
			}
			return count > 1;
		} else {			
			return this.opts.item.canvases.length > 1
		}
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

