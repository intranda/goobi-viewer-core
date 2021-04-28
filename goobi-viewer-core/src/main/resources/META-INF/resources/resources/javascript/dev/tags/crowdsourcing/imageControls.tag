<imageControls>

<div class="image_controls">
	<div class="image-controls__actions">
		<div class="image-controls__action thumbs {this.showThumbs ? 'in' : ''}">
			<a onclick="{toggleThumbs}">
				<i class="image-thumbs"></i>
			</a>
		</div>
		<div if="{this.opts.image && !this.showThumbs}" class="image-controls__action rotate-left">
			<a onclick="{rotateLeft}">
				<i class="image-rotate_left"></i>
			</a>
		</div>
		<div if="{this.opts.image && !this.showThumbs}" class="image-controls__action rotate-right">
			<a onclick="{rotateRight}">
				<i class="image-rotate_right"></i>
			</a>
		</div>
        <div if="{this.opts.image && !this.showThumbs}" class="image-controls__action zoom-slider-wrapper">
        	<input type="range" min="0" max="1" value="0" step="0.01" class="slider zoom-slider" aria-label="zoom slider"/>
        </div>
	</div>
</div>


<script>
    this.on( "mount", function() {
        this.showThumbs = this.opts.showThumbs ? true : false;
    } );
    
    rotateRight()
    {
        if ( this.opts.image ) {
            this.opts.image.controls.rotateRight();
        }
    	this.handleAction("rotate", 90)
    }

    rotateLeft()
    {
        if ( this.opts.image ) {
            this.opts.image.controls.rotateLeft();
        }
    	this.handleAction("rotate", -90)
    }
    
    toggleThumbs() {
    	this.showThumbs = !this.showThumbs;
    	this.handleAction("toggleThumbs", this.showThumbs)
    }
    
    handleAction(control, value) {
    	if(this.opts.actionlistener) {
    		this.opts.actionlistener.next({
    			action: control,
    			value: value
    		});
    	}
    }
</script> 

</imageControls>