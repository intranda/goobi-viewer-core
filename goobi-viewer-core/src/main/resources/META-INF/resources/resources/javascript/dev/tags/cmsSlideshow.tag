<!-- HOW TO USE IN XHTML -->
<!-- <slideshow  -->
<!-- Text field from CMS template, can contain several PIs separated by spaces. -->
<!--     pis="#{cmsPage.getContent('pi01')}"  -->
<!-- URL to the IIIF Manifesto of PI. -->
<!--     manifest_base_url="#{navigationHelper.applicationUrl}rest/iiif/manifests/\{pi\}/manifest/simple" -->
<!-- Metadata fields, passed as a message key. -->
<!--     metadata="#{msg.MD_ARTIST}, #{msg.MD_YEARPUBLISH}"  -->
<!-- Current language version -->
<!--     locale="#{navigationHelper.localeString}"></slideshow> -->

<!-- MOUNT IN XHTML -->
<!-- riot.mount( 'slideshow' ); -->

<cmsSlideshow>
	<!-- dummy link to catch scrollling to stored position (viewerJS.checkScrollPosition()) which executes before manifest is loaded -->
	<a if="{manifest === undefined}" data-linkid="{opts.pis}"/>
    <figure class="slideshow" if="{manifest !== undefined}" onmouseenter="{mouseenter}" onmouseleave="{mouseleave}"> 
        <!-- IMAGE -->
        <div class="slideshow__image">
            <a href="{getLink(manifest)}" class="remember-scroll-position" data-linkid="{opts.pis}" onclick="{storeScrollPosition}">
                <img src="{getThumbnail(manifest)}" class="{'active' : active}" alt="{getLabel(manifest)}" onload="{setImageActive}" />
            </a>
        </div>
    
        <!-- CAPTION -->
        <figcaption>
            <h4>{getTitleOrLabel(manifest)}</h4>
            <p>
                <span each="{md in metadataList}">
                    {getMetadataValue(manifest, md)}
                    <br />
                </span>
            </p>
            
            <!-- IMAGE NAVIGATION -->
            <div if="{pis.length > 1}" class="slideshow__dots">
                <ul>
                    <li each="{imagepi in pis}">
                        <button class="btn btn--clean {'active' : pi === imagepi}" onclick="{setPi}"></button>
                    </li>
                </ul>
            </div>
        </figcaption>
    </figure>
    
    <script>
    	/**
    	 * @description JQuery method which checks, if element is in viewport.
    	 * @method isInViewport
    	 * @returns {Boolean} Returns true if element is in viewport.
    	 */
    	$.fn.isInViewport = function() {
        	var elementTop = $( this ).offset().top;
        	var elementBottom = elementTop + $( this ).outerHeight();
        	var elementHeight = $( this ).outerHeight();
        	var viewportTop = $( window ).scrollTop();
        	var viewportBottom = viewportTop + $( window ).height();
        	
        	return elementBottom > (viewportTop + elementHeight) && elementTop < (viewportBottom - elementHeight);
    	};
    	 
    	this.pis = this.opts.pis.split(/[\s,;]+/);
    	this.pis = this.pis.filter( function( pi ) {
    		return pi != undefined && pi.length > 0;
    	} );
        this.metadataList = this.opts.metadata.split(/[,;]+/);
        this.manifest = undefined;
        this.manifests = new Map();
        this.active = false;
        this.visible = false;
        this.mouseover = false;
        
        
        this.on( 'mount', function() {
        	this.loadManifest( this.pis[0] );
        }.bind( this ));
        
        mouseenter() {
        	this.mouseover = true;
        }

        mouseleave() {
        	this.mouseover = false;
        }
        
        checkPosition() {
        	var slideshow = $( '#' + this.opts.id + ' figure' );

        	if ( !this.visible && this.pis.length > 1 && slideshow.isInViewport() ) {
        		this.visible = true;        	
            	this.moveSlides( this.pis, true );            	
        	}
        	else if ( this.visible && !slideshow.isInViewport() ) {
        		this.visible = false;            	
        		this.moveSlides( this.pis, false );
        	}        	
        }
        
        moveSlides( pis, move ) {
        	var index = 1;
        	
        	if ( move ) {
        		clearInterval( this.interval );
        		
        		this.interval = setInterval( function() {
                	if ( index === pis.length ) {
                		index = 0;
                	}
                	if ( !this.mouseover ) {
            			this.loadManifest( pis[ index ] );
                    	index++;
                	}
                }.bind( this ), 3000 );
        	}
        	else {
        		clearInterval( this.interval );
        	}        	
        }
        
        setPi( event ) {
        	let pi = event.item.imagepi;
        	
        	if ( pi != this.pi ) {
        		this.pi = pi;
        		
        		return this.loadManifest( pi );
        	}
        }
        
        setImageActive() {
        	this.active = true;
        	this.update();        		
        }
        
        loadManifest( pi ) {
        	let url = this.opts.manifest_base_url.replace( "{pi}", pi );
        	let json = this.manifests.get( url );
        	this.pi = pi;
        	this.active = false;
        	this.update();
        	
        	if ( !json ) {
        		$.getJSON( url, function( manifest ) {
        			if ( manifest ) {
        				// TODO: 404 abfangen, dass nicht ausgestiegen wird
        				this.manifest = manifest;
        				this.manifests.set( url, manifest );
        				this.update();
            			this.checkPosition();
            			
        				$( window ).on( 'resize scroll', function() {
            				this.checkPosition();
        				}.bind( this ) );
        			}
        		}.bind( this ))
        		.then(function(data) {
        		})
        		.catch(function(error) {
        			console.error("error laoding ", url, ": ", error);
        		});
        	} 
        	else {
        		// timeout for css transition
            	setTimeout( function() {
            		this.manifest = json;
            		this.update();
            	}.bind( this ), 300 );
        	}
        }
        getThumbnail( manifest, width, height ) {
        	if( !manifest.thumbnail.service || ( !width && !height ) ) {
        		return manifest.thumbnail['@id'];		
        	} 
        	else {
        		let sizePrefix = width && height ? "!" : "";
        		
        		return manifest.thumbnail.service['@id'] + "/full/" + sizePrefix + width + "," + height + "/0/default.jpg";
        	}
        }
        
        getLink( manifest ) {
        	rendering = manifest.rendering;
        	
        	if ( Array.isArray( rendering ) ) {
        		rendering = rendering.find( ( rend ) => rend.format == "text/html" );
        	}
        	if ( rendering ) {
        		return rendering['@id'];
        	} 
        	else {
        		return '';
        	}
        }
        
        getTitleOrLabel( manifest ) {
        	var title = this.getMetadataValue( manifest, 'Title' );
        	
        	if(title) {
        		return title;
        	} else {
        		return getLabel( manifest );
        	}
        }
       
        getLabel( manifest ) {
        	return this.getValue(manifest.label, this.opts.locale);
        }
        
        getMetadataValue( manifest, metadataLabel ) {
        	if ( manifest && metadataLabel ) {		
        		let metadata = manifest.metadata.find( ( md ) => {
        			let label = md.label;
        			if ( Array.isArray( label ) ) {
        				label = label.find( (l) => l['@value'].trim() == metadataLabel.trim());
        				if ( label ) {
        					label = label['@value']
        				}
        			}
        			return label && label.trim() == metadataLabel.trim();
        		});
        		
        		if ( metadata ) {			
        			let value = this.getValue( metadata.value, this.opts.locale );
        			
        			return value;
        		}
        	}
        }
        
        getValue ( element, locale ) {
            if ( element ) {
            	if ( typeof element === 'string' ) {
            		return element;
            	} 
        		else if ( Array.isArray( element ) ) {
            		var fallback;
                    
            		for ( var index in element  ) {
            			var item = element[index];
                       
            			if ( typeof item === 'string' ) {
            				return item;
            			} 
            			else {
            				var value = item['@value'];
            				var language = item['@language'];
                           
            				if ( locale == language ) {
            					return value;
            				} 
            				else if ( !fallback || language == 'en' ) {
            					fallback = value;
            				}
            			}
            		}
                    
            		return fallback;
            	} 
            	else {
            		return element['@value'];                
            	}
            }
        }
        
        storeScrollPosition(event) {
            $target = $(event.target).closest("a");
            viewerJS.handleScrollPositionClick($target);
        }
    </script> 
</cmsSlideshow>
