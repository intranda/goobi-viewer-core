<fsThumbnailImage>
    <div class="fullscreen__view-image-thumb-preloader" if={preloader}></div>
    <img ref="image" alt="Thumbnail Image" />

    <script>
    	this.preloader = false;
    
    	this.on('mount', function() {
    		this.createObserver();
    		
    		this.refs.image.onload = function() {
        		this.refs.image.classList.add( 'in' );
				this.opts.observable.trigger( 'imageLoaded', this.opts.thumbnail );
        		this.preloader = false;
        		this.update();
    		}.bind(this);
    	}.bind(this));
    	
    	createObserver() {
    		var observer;
    		var options = {
    			root: document.querySelector(this.opts.root),
    		    rootMargin: "1000px 0px 1000px 0px",
    		    threshold: 0.8
    		};
    		
    		observer = new IntersectionObserver(this.loadImages, options);
    		observer.observe(this.refs.image);
    	}
    	
    	loadImages(entries, observer) {
    		entries.forEach( entry => {
    			if (entry.isIntersecting) {
    				this.preloader = true;
    				this.refs.image.src = this.opts.imgsrc;
    				this.update();
    			}
    		} );
    	}
    </script>
</fsThumbnailImage>