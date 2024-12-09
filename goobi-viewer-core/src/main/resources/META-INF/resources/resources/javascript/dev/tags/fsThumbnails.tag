<fsThumbnails>
    <div class="fullscreen__view-image-thumbs" ref="thumbnailWrapper">
        <!-- THUMBNAIL -->
        <div each={thumbnail in thumbnails} class="fullscreen__view-image-thumb">
            <!-- THUMBNAIL IMAGE -->
            <figure class="fullscreen__view-image-thumb-image">
                <a href="{getViewerPageUrl(thumbnail)['@id']}">
                    <fsThumbnailImage thumbnail={thumbnail} observable={observable} root=".fullscreen__view-image-thumbs-wrapper" imgsrc={thumbnail.thumbnail['@id']}></fsThumbnailImage>
                </a>
    
                <figcaption>
                    <!-- THUMBNAIL IMAGE ORDER -->
                    <div class="fullscreen__view-image-thumb-image-order {thumbnail.loaded ? 'in' : ''}">{thumbnail.label}</div>
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
    	this.wrapper = document.getElementsByClassName( 'fullscreen__view-image-thumbs-wrapper' );
    	this.controls = document.getElementsByClassName( 'image-controls' );
    	this.image = document.getElementById( 'imageContainer' );
    	this.sidebarScrollPreview = document.getElementById( 'sidebarScrollPreview' );
    	this.viewportWidth;
    	this.sidebarWidth;
    	this.thumbsWidth;
    	
    	this.on( 'mount', function() {    		
        	$( '[data-show="thumbs"]' ).on( 'click', function(e) {
        		e.currentTarget.classList.toggle('in');
        		
        		// change tooltip
        		if ( e.currentTarget.classList.contains( 'in' ) ) {
            		$( '[data-show="thumbs"]' ).attr( 'title', opts.msg.hideThumbs ).tooltip( '_fixTitle' ).tooltip( 'show' );
        		}
        		else {
            		$( '[data-show="thumbs"]' ).attr( 'title', opts.msg.showThumbs ).tooltip( '_fixTitle' ).tooltip( 'show' );
        		}
        		
        		// hide image controls
        		for (let control of this.controls) {
        		    control.classList.toggle( 'faded' );
        		};
        		
        		// set element widths
            	this.viewportWidth = document.getElementById( 'fullscreen' ).offsetWidth;
            	this.sidebarWidth = document.getElementById( 'fullscreenViewSidebar' ).offsetWidth;
            	if ( sessionStorage.getItem( 'fsSidebarStatus' ) === 'false' ) {
                	this.thumbsWidth = this.viewportWidth;            		
            	}
            	else {
                	this.thumbsWidth = this.viewportWidth - this.sidebarWidth;            		
            	}
            	
            	// toggle image
            	let visibility = $( this.image ).css('visibility');
            	if(visibility == 'hidden') {
            		$( this.image ).css('visibility','visible');
            		$( this.sidebarScrollPreview ).show();
            		
            		
            	} else {            	    
            		$( this.image ).css('visibility','hidden');
            		$( this.sidebarScrollPreview ).hide();
            	}
            	
            	// show thumb wrapper
        		$( this.wrapper ).outerWidth( this.thumbsWidth ).fadeToggle( 'fast' );
        		
    			// get thumbnail images
            	if ( this.thumbnails.length == 0 ) {
            		// get thumbnails
            		$.ajax( {
                        url: opts.thumbnailUrl,
                        type: "GET",
                        datatype: "JSON"
                    } ).then( function( data ) {
                    	this.thumbnails = data.canvases;//.map(c => c.thumbnail["@id"]);
                    	this.update();                       
                    }.bind( this ) );                        
    			}    			
        	}.bind(this));
    	}.bind( this ) );
    	
    	this.observable.on( 'imageLoaded', function( thumbnail ) {
    		thumbnail.loaded = true;
    		this.update();
    	}.bind( this ) );
    	
    	getViewerPageUrl(thumbnail) {
    	    if(thumbnail.rendering) {
    	        if(Array.isArray(thumbnail.rendering)) {
    	            return thumbnail.rendering.find(render => "text/html" == render.format)
    	        } else {
    	            return thumbnail.rendering;
    	        }
    	    }
    	}
    </script>
</fsThumbnails>