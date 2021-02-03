<imageControls>

<div class="image_controls">
	<div class="image-controls__actions">
		<div class="image-controls__action rotate-left">
			<a onclick="{rotateLeft}">
				<i class="image-rotate_left"></i>
			</a>
		</div>
		<div class="image-controls__action rotate-right">
			<a onclick="{rotateRight}">
				<i class="image-rotate_right"></i>
			</a>
		</div>
        <div class="image-controls__action zoom-slider-wrapper">
        	<input type="range" min="0" max="1" value="0" step="0.01" class="slider zoom-slider" aria-label="zoom slider"/>
        </div>
	</div>
</div>


<script>
    this.on( "mount", function() {
        
    } );
    
    rotateRight()
    {
        if ( this.opts.image ) {
            this.opts.image.controls.rotateRight();
        }
        if(this.opts.item) {
            this.opts.item.notifyImageRotated(90);
        }
    }

    rotateLeft()
    {
        if ( this.opts.image ) {
            this.opts.image.controls.rotateLeft();
        }
        if(this.opts.item) {
            this.opts.item.notifyImageRotated(-90);
        }
    }
</script> 

</imageControls>