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
	        <div class="zoom-slider">
	            <div class="zoom-slider-handle"></div>
	        </div>
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