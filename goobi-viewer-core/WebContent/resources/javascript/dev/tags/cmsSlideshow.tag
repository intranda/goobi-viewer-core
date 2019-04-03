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

<slideshow>
    <figure class="slideshow" if="{manifest !== undefined}"> 
        <!-- IMAGE -->
        <div class="slideshow__image">
            <a href="{getLink(manifest)}">
                <img src="{getThumbnail(manifest)}" class="{'active' : active}" alt="{getLabel(manifest)}" onload="{setImageActive}" />
            </a>
        </div>
    
        <!-- CAPTION -->
        <figcaption>
            <h4>{getLabel(manifest)}</h4>
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
    	this.pis = this.opts.pis.split(/[\s,;]+/);
        this.metadataList = this.opts.metadata.split(/[,;]+/);
        this.manifest = undefined;
        this.manifests = new Map();
        this.active = false;
        
        this.on( 'mount', function() {
        	this.pi = this.pis[0];
        	this.loadManifest( this.pi );        	
        }.bind( this ));
        
        setPi( event ) {
        	let pi = event.item.imagepi;
        	
        	if ( pi != this.pi ) {
        		this.pi = pi;
        		this.active = false;
        		
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
        	
        	if ( !json ) {		
        		$.getJSON( url, function( manifest ) {
        			if ( manifest ) {
        				// TODO: 404 abfangen, dass nicht ausgestiegen wird
        				this.manifest = manifest;
        				this.manifests.set( url, manifest );
        				this.update();
        			}
        		}.bind( this ));
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
    </script> 
</slideshow>
