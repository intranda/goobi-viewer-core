<imageControls>

<div class="image_controls">
		<div class="image_controls__item">
			<button class="controls__item fa fa-rotate-left"
				onclick="{rotateLeft}"></button>
		</div>
		<div class="image_controls__item">
			<button class="controls__item fa fa-rotate-right"
				onclick="{rotateRight}"></button>
		</div>
		<div class="image_controls__item zoom-slider-wrapper">
	        <div class="zoom-slider">
	            <div class="zoom-slider-handle"></div>
	        </div>
		</div>
</div>


<script>
    this.on( "mount", function() {
        console.log( "mount image controls" );
    } );
    
    rotateRight()
    {
        if ( this.opts.image ) {
            this.opts.image.controls.rotateRight();
        }
    }

    rotateLeft()
    {
        if ( this.opts.image ) {
            this.opts.image.controls.rotateLeft();
        }
    }
</script> 

</imageControls>