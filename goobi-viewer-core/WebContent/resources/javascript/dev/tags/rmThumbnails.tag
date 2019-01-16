<rmThumbnails>
    <div class="reading-mode__view-image-thumbs" ref="thumbnailWrapper">
        <!-- THUMBNAIL -->
        <div each={thumbnail in thumbnails} class="reading-mode__view-image-thumb">
            <!-- THUMBNAIL IMAGE -->
            <figure class="reading-mode__view-image-thumb-image">
                <a href="{thumbnail.rendering['@id']}">
                    <rmThumbnailImage thumbnail={thumbnail} observable={observable} root=".reading-mode__view-image-thumbs-wrapper" imgsrc={thumbnail.thumbnail['@id']}></rmThumbnailImage>
                </a>
    
                <figcaption>
                    <!-- THUMBNAIL IMAGE ORDER -->
                    <div class="reading-mode__view-image-thumb-image-order {thumbnail.loaded ? 'in' : ''}">{thumbnail.label}</div>
                </figcaption>
            </figure>
        </div>
    </div>
    
    <script>
        function rmObservable() {
    		riot.observable( this );
    	}
        
    	this.observable = new rmObservable();
        this.thumbnails = [];
    	this.wrapper = document.getElementsByClassName( 'reading-mode__view-image-thumbs-wrapper' );
    	this.controls = document.getElementsByClassName( 'image-controls' );
    	this.image = document.getElementById( 'imageContainer' );
    	this.viewportWidth;
    	this.sidebarWidth;
    	this.thumbsWidth;
    	
    	this.on( 'mount', function() {
        	$( '[data-show="thumbs"]' ).on( 'click', function(e) {
        		e.currentTarget.classList.toggle('in');
        		
        		// hide image controls
        		this.controls[0].classList.toggle( 'faded' );
        		
        		// set element widths
            	this.viewportWidth = document.getElementById( 'readingMode' ).offsetWidth;
            	this.sidebarWidth = document.getElementById( 'readingModeViewSidebar' ).offsetWidth;
            	if ( sessionStorage.getItem( 'rmSidebarStatus' ) === 'false' ) {
                	this.thumbsWidth = this.viewportWidth;            		
            	}
            	else {
                	this.thumbsWidth = this.viewportWidth - this.sidebarWidth;            		
            	}
            	
            	// toggle image
            	$( this.image ).toggle();
            	
            	// show thumb wrapper
        		$( this.wrapper ).width( this.thumbsWidth ).fadeToggle( 'fast' );
        		
    			// get thumbnail images
            	if ( this.thumbnails.length == 0 ) {
            		// get thumbnails
            		$.ajax( {
                        url: opts.thumbnailUrl,
                        type: "GET",
                        datatype: "JSON"
                    } ).then( function( data ) {
                    	this.thumbnails = data;
                    	this.update();                       
                    }.bind( this ) );                        
    			}    			
        	}.bind(this));
    	}.bind( this ) );
    	
    	this.observable.on( 'imageLoaded', function( thumbnail ) {
    		thumbnail.loaded = true;
    		this.update();
    	}.bind( this ) );
    </script>
</rmThumbnails>