<imageControls>

<div class="image_controls">
	<div class="image-controls__actions">
		<div onclick="{toggleThumbs}" class="image-controls__action thumbs {this.opts.imagecount < 2 ? 'd-none' : ''} {this.opts.showthumbs ? 'in' : ''}">
			<a>
			</a>
		</div>
		<!-- NEXT AND BACK -->
		<div if="{this.opts.image}" class="image-controls__action -imageControlsFont back {this.opts.imageindex === 0 ? '-inactive' : ''}">
			<a onclick="{previousItem}">
				<i class="image-back"></i>
			</a>
		</div>
		<div if="{this.opts.image}" class="image-controls__action -imageControlsFont forward {this.opts.imageindex === this.opts.imagecount -1 ? '-inactive' : ''}">
			<a onclick="{nextItem}">
				<i class="image-forward"></i>
			</a>
		</div>
		<!-- ROTATE LEFT + RIGHT -->			
		<div if="{this.opts.image}" class="image-controls__action -imageControlsFont rotate-left">
			<a onclick="{rotateLeft}">
				<i class="image-rotate_left"></i>
			</a>
		</div>
		<div if="{this.opts.image}" class="image-controls__action -imageControlsFont rotate-right">
			<a onclick="{rotateRight}">
				<i class="image-rotate_right"></i>
			</a>
		</div>
        <div if="{this.opts.image}" class="image-controls__action zoom-slider-wrapper">
        	<input type="range" min="0" max="1" value="0" step="0.01" class="slider zoom-slider" aria-label="zoom slider"/>
        </div>
	</div>
</div>


<script>
    
    rotateRight()
    {
        if ( this.opts.rotate ) {
            this.opts.rotate.rotateRight();
        }
    	this.handleAction("rotate", 90)
    }

    rotateLeft()
    {
        if ( this.opts.rotate ) {
            this.opts.rotate.rotateLeft();
        }
    	this.handleAction("rotate", -90)
    }


    previousItem()
    {
    	if (this.opts.imageindex > 0) {
    		this.handleAction("previousImage");
    	}
    }

    nextItem()
    {
    	if (this.opts.imageindex < this.opts.imagecount -1) {
    		this.handleAction("nextImage");
    	}
    }    

    toggleThumbs() {
    	console.log("toggle thumbs " + this.opts.showthumbs);
    	this.opts.showthumbs = !this.opts.showthumbs;
    	this.handleAction("toggleThumbs", this.opts.showthumbs)
    }
    
    handleAction(control, value) {
    	if(this.opts.actionlistener) {
    		this.opts.actionlistener.next({
    			action: control,
    			value: value
    		});
    	}
    }


    // TOOLTIP FOR IMAGE CONTROLS
	$( document ).ready(function() {
	    $('.image-controls__action.thumbs').tooltip({
	      placement: 'top',
	      title: Crowdsourcing.translate("crowdsourcing__campaign_tooltip_back_to_overview"),
	      trigger: 'hover'
	    });
	    $('.image-controls__action.back').tooltip({
	        placement: 'top',
	      title: Crowdsourcing.translate("prevImage"),
	      trigger: 'hover'
	    });
	    $('.image-controls__action.forward').tooltip({
	      placement: 'top',
	      title: Crowdsourcing.translate("nextImage"),
	      trigger: 'hover'
	    });
	    $('.image-controls__action.rotate-left').tooltip({
	      placement: 'top',
	      title: Crowdsourcing.translate("rotateLeft"),
	      trigger: 'hover'
	    });
	    $('.image-controls__action.rotate-right').tooltip({
	      placement: 'top',
	      title: Crowdsourcing.translate("rotateRight"),
	      trigger: 'hover'
	    });
	});
    
</script> 

</imageControls>