riot.tag2('adminmediaupload', '<div class="admin-cms-media__upload {isDragover ? \'is-dragover\' : \'\'}" ref="dropZone"><div class="admin-cms-media__upload-input"><p> {opts.msg.uploadText} <br><small>({opts.msg.allowedFileTypes}: {fileTypes})</small></p><label for="file" class="btn btn--default">{opts.msg.buttonUpload}</label><input id="file" class="admin-cms-media__upload-file" type="file" multiple="multiple" onchange="{buttonFilesSelected}"></div><div class="admin-cms-media__upload-messages"><div class="admin-cms-media__upload-message uploading"><i class="fa fa-spinner fa-pulse fa-fw"></i> {opts.msg.mediaUploading} </div><div class="admin-cms-media__upload-message success"><i class="fa fa-check-square-o" aria-hidden="true"></i> {opts.msg.mediaFinished} </div><div class="admin-cms-media__upload-message error"><i class="fa fa-exclamation-circle" aria-hidden="true"></i><span></span></div></div></div>', '', '', function(opts) {
        this.files = [];
        this.displayFiles = [];
        this.fileTypes = 'jpg, png, docx, doc, rtf, html, xhtml, xml';
        this.isDragover = false;

        this.on('mount', function () {
            var dropZone = (this.refs.dropZone);

            dropZone.addEventListener('dragover', function (e) {
                e.stopPropagation();
                e.preventDefault();
                e.dataTransfer.dropEffect = 'copy';

                $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.success, .admin-cms-media__upload-message.error').removeClass('in-progress');

                this.isDragover = true;
                this.update();
            }.bind(this));

            dropZone.addEventListener('dragleave', function (e) {
                this.isDragover = false;
                this.update();
            }.bind(this));

            dropZone.addEventListener('drop', (e) => {
                e.stopPropagation();
                e.preventDefault();
                this.files = [];

                for (var f of e.dataTransfer.files) {
                    this.files.push(f);
                    var sizeUnit = 'KB';
                    var size = f.size / 1000;

                    if (size > 1024) {
                        size = size / 1024;
                        sizeUnit = 'MB';
                    }

                    if (size > 1024) {
                        size = size / 1024;
                        sizeUnit = 'GB';
                    }

                    this.displayFiles.push({ name: f.name, size: Math.floor(size) + ' ' + sizeUnit, completed: 0 });
                }

                this.uploadFiles();
            });
        }.bind(this));

        this.buttonFilesSelected = function(e) {
            for (var f of e.target.files) {

                this.files.push(f);
                var sizeUnit = 'KB';
                var size = f.size / 1000;

                if (size > 1024) {
                    size = size / 1024;
                    sizeUnit = 'MB';
                }
                if (size > 1024) {
                    size = size / 1024;
                    sizeUnit = 'GB';
                }

                this.displayFiles.push({ name: f.name, size: Math.floor(size) + ' ' + sizeUnit, completed: 0 });
            }

            this.uploadFiles();
        }.bind(this)

        this.uploadFiles = function() {
            var uploads = [];

            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.success, .admin-cms-media__upload-message.error').removeClass('in-progress');

            for (i = 0; i < this.files.length; i++) {
                uploads.push(Q(this.uploadFile(i)));
            }

            Q.allSettled(uploads).then(function(results) {
                	var errorMsg = "";

                    results.forEach(function (result) {
                        if (result.state === "fulfilled") {
                        	var value = result.value;
                        	this.fileUploaded(value);
                        }
                        else {
                            var responseText = result.reason.responseText;
                            errorMsg += (responseText + "</br>");
                        }
                    }.bind(this));

                    if (errorMsg) {
                    	this.fileUploadError(errorMsg);
                    } else if(this.opts.onUploadSuccess) {
                        this.opts.onUploadSuccess();
                    }

               		if (this.opts.onUploadComplete) {
               			this.opts.onUploadComplete();
               		}
                }.bind(this))
        }.bind(this)

        this.fileUploaded = function(fileInfo) {
            console.log("file uploaded")
            $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.success').addClass('in-progress');

            setTimeout( function() {
                $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.success').removeClass('in-progress');
        	}, 5000 );
        }.bind(this)

        this.fileUploadError = function(responseText) {
        	if (responseText) {
                $('.admin-cms-media__upload-messages, .admin-cms-media__upload-message.error').addClass('in-progress');
                $('.admin-cms-media__upload-message.error span').html(responseText);
            }
        }.bind(this)

        this.uploadFile = function(i) {
            if (this.files.length <= i) {
                new Modal(this.refs.doneModal).show();

                return;
            }

            var displayFile = this.displayFiles[i];
            var config = {
                onUploadProgress: (progressEvent) => {
                    displayFile.completed = (progressEvent.loaded * 100) / progressEvent.total;
                    this.update();
                }
            };
            var data = new FormData();

            data.append('file', this.files[i])
            data.append("filename", this.files[i].name)

            return $.ajax({
                url: this.opts.postUrl,
                type: 'POST',
                data: data,
                dataType: 'json',
                cache: false,
                contentType: false,
                processData: false
            });
        }.bind(this)
});














riot.tag2('slideshow', '<a if="{manifest === undefined}" data-linkid="{opts.pis}"></a><figure class="slideshow" if="{manifest !== undefined}" onmouseenter="{mouseenter}" onmouseleave="{mouseleave}"><div class="slideshow__image"><a href="{getLink(manifest)}" class="remember-scroll-position" data-linkid="{opts.pis}" onclick="{storeScrollPosition}"><img riot-src="{getThumbnail(manifest)}" class="{\'active\' : active}" alt="{getLabel(manifest)}" onload="{setImageActive}"></a></div><figcaption><h4>{getTitleOrLabel(manifest)}</h4><p><span each="{md in metadataList}"> {getMetadataValue(manifest, md)} <br></span></p><div if="{pis.length > 1}" class="slideshow__dots"><ul><li each="{imagepi in pis}"><button class="btn btn--clean {\'active\' : pi === imagepi}" onclick="{setPi}"></button></li></ul></div></figcaption></figure>', '', '', function(opts) {

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

        console.log("tag created")

        this.on( 'mount', function() {
            console.log("tag mounted")
        	this.loadManifest( this.pis[0] );
        }.bind( this ));

        this.mouseenter = function() {
        	this.mouseover = true;
        }.bind(this)

        this.mouseleave = function() {
        	this.mouseover = false;
        }.bind(this)

        this.checkPosition = function() {
        	var slideshow = $( '#' + this.opts.id + ' figure' );

        	if ( !this.visible && this.pis.length > 1 && slideshow.isInViewport() ) {
        		this.visible = true;
            	this.moveSlides( this.pis, true );
        	}
        	else if ( this.visible && !slideshow.isInViewport() ) {
        		this.visible = false;
        		this.moveSlides( this.pis, false );
        	}
        }.bind(this)

        this.moveSlides = function( pis, move ) {
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
        }.bind(this)

        this.setPi = function( event ) {
        	let pi = event.item.imagepi;

        	if ( pi != this.pi ) {
        		this.pi = pi;

        		return this.loadManifest( pi );
        	}
        }.bind(this)

        this.setImageActive = function() {
        	this.active = true;
        	this.update();
        }.bind(this)

        this.loadManifest = function( pi ) {
        	let url = this.opts.manifest_base_url.replace( "{pi}", pi );
        	let json = this.manifests.get( url );
        	this.pi = pi;
        	this.active = false;
        	this.update();

        	if ( !json ) {
        		$.getJSON( url, function( manifest ) {
        			if ( manifest ) {

        				this.manifest = manifest;
        				this.manifests.set( url, manifest );
        				this.update();
        				console.log("manifest loaded");
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

            	setTimeout( function() {
            		this.manifest = json;
            		this.update();
            	}.bind( this ), 300 );
        	}
        }.bind(this)
        this.getThumbnail = function( manifest, width, height ) {
        	if( !manifest.thumbnail.service || ( !width && !height ) ) {
        		return manifest.thumbnail['@id'];
        	}
        	else {
        		let sizePrefix = width && height ? "!" : "";

        		return manifest.thumbnail.service['@id'] + "/full/" + sizePrefix + width + "," + height + "/0/default.jpg";
        	}
        }.bind(this)

        this.getLink = function( manifest ) {
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
        }.bind(this)

        this.getTitleOrLabel = function( manifest ) {
        	var title = this.getMetadataValue( manifest, 'Title' );

        	if(title) {
        		return title;
        	} else {
        		return getLabel( manifest );
        	}
        }.bind(this)

        this.getLabel = function( manifest ) {
        	return this.getValue(manifest.label, this.opts.locale);
        }.bind(this)

        this.getMetadataValue = function( manifest, metadataLabel ) {
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
        }.bind(this)

        this.getValue = function ( element, locale ) {
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
        }.bind(this)

        this.storeScrollPosition = function(event) {
            $target = $(event.target).closest("a");
            viewerJS.handleScrollPositionClick($target);
        }.bind(this)
});

riot.tag2('fsthumbnailimage', '<div class="fullscreen__view-image-thumb-preloader" if="{preloader}"></div><img ref="image" alt="Thumbnail Image">', '', '', function(opts) {
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

    	this.createObserver = function() {
    		var observer;
    		var options = {
    			root: document.querySelector(this.opts.root),
    		    rootMargin: "1000px 0px 1000px 0px",
    		    threshold: 0.8
    		};

    		observer = new IntersectionObserver(this.loadImages, options);
    		observer.observe(this.refs.image);
    	}.bind(this)

    	this.loadImages = function(entries, observer) {
    		entries.forEach( entry => {
    			if (entry.isIntersecting) {
    				this.preloader = true;
    				this.refs.image.src = this.opts.imgsrc;
    				this.update();
    			}
    		} );
    	}.bind(this)
});
riot.tag2('fsthumbnails', '<div class="fullscreen__view-image-thumbs" ref="thumbnailWrapper"><div each="{thumbnail in thumbnails}" class="fullscreen__view-image-thumb"><figure class="fullscreen__view-image-thumb-image"><a href="{thumbnail.rendering[\'@id\']}"><fsthumbnailimage thumbnail="{thumbnail}" observable="{observable}" root=".fullscreen__view-image-thumbs-wrapper" imgsrc="{thumbnail.thumbnail[\'@id\']}"></fsThumbnailImage></a><figcaption><div class="fullscreen__view-image-thumb-image-order {thumbnail.loaded ? \'in\' : \'\'}">{thumbnail.label}</div></figcaption></figure></div></div>', '', '', function(opts) {
        function rmObservable() {
    		riot.observable( this );
    	}

    	this.observable = new rmObservable();
        this.thumbnails = [];
    	this.wrapper = document.getElementsByClassName( 'fullscreen__view-image-thumbs-wrapper' );
    	this.controls = document.getElementsByClassName( 'image-controls' );
    	this.image = document.getElementById( 'imageContainer' );
    	this.viewportWidth;
    	this.sidebarWidth;
    	this.thumbsWidth;

    	this.on( 'mount', function() {
        	$( '[data-show="thumbs"]' ).on( 'click', function(e) {
        		e.currentTarget.classList.toggle('in');

        		if ( e.currentTarget.classList.contains( 'in' ) ) {
            		$( '[data-show="thumbs"]' ).attr( 'title', opts.msg.hideThumbs ).tooltip( 'fixTitle' ).tooltip( 'show' );
        		}
        		else {
            		$( '[data-show="thumbs"]' ).attr( 'title', opts.msg.showThumbs ).tooltip( 'fixTitle' ).tooltip( 'show' );
        		}

        		this.controls[0].classList.toggle( 'faded' );

            	this.viewportWidth = document.getElementById( 'fullscreen' ).offsetWidth;
            	this.sidebarWidth = document.getElementById( 'fullscreenViewSidebar' ).offsetWidth;
            	if ( sessionStorage.getItem( 'fsSidebarStatus' ) === 'false' ) {
                	this.thumbsWidth = this.viewportWidth;
            	}
            	else {
                	this.thumbsWidth = this.viewportWidth - this.sidebarWidth;
            	}

            	$( this.image ).toggle();

        		$( this.wrapper ).width( this.thumbsWidth ).fadeToggle( 'fast' );

            	if ( this.thumbnails.length == 0 ) {

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
});
riot.tag2('pdfdocument', '<div class="pdf-container"><pdfpage each="{page, index in pages}" page="{page}" pageno="{index+1}"></pdfPage></div>', '', '', function(opts) {

		this.pages = [];

		var loadingTask = pdfjsLib.getDocument( this.opts.data );
	    loadingTask.promise.then( function( pdf ) {
	        var pageLoadingTasks = [];
	        for(var pageNo = 1; pageNo <= pdf.numPages; pageNo++) {
   		        var page = pdf.getPage(pageNo);
   		        pageLoadingTasks.push(Q(page));
   		    }
   		    return Q.allSettled(pageLoadingTasks);
	    }.bind(this))
	    .then(function(results) {
			results.forEach(function (result) {
			    if (result.state === "fulfilled") {
                	var page = result.value;
                	this.pages.push(page);
                } else {
                    logger.error("Error loading page: ", result.reason);
                }
			}.bind(this));
			this.update();
        }.bind(this))
	    .then( function() {
			$(".pdf-container").show();
            $( '#literatureLoader' ).hide();
		} );

});
riot.tag2('pdfpage', '<div class="page" id="page_{opts.pageno}"><canvas class="pdf-canvas" id="pdf-canvas_{opts.pageno}"></canvas><div class="text-layer" id="pdf-text_{opts.pageno}"></div><div class="annotation-layer" id="pdf-annotations_{opts.pageno}"></div></div>', '', '', function(opts) {
	this.on('mount', function () {
			console.log("load page ", this.opts.pageno, this.opts.page);
			this.scale = this.opts.scale ? this.opts.scale : 1.4;
			this.viewport = this.opts.page.getViewport( this.scale );

            this.container = document.getElementById( "page_" + this.opts.pageno );
            this.canvas = document.getElementById( "pdf-canvas_" + this.opts.pageno );
            this.textLayer = document.getElementById( "pdf-text_" + this.opts.pageno );
            this.annotationLayer = document.getElementById( "pdf-annotations_" + this.opts.pageno );

            if(this.container) {
                this.loadPage();
            }
	});

            this.loadPage = function() {
	            var canvasOffset = $(this.canvas).offset();
	            var context = this.canvas.getContext( "2d" );
	            this.canvas.height = this.viewport.height;
	            this.canvas.width = this.viewport.width;

	            console.log("render ", this.opts.page, context, this.viewport);

	            this.opts.page.render( {
					canvasContext: context,
	                viewport: this.viewport
	            })
	            .then(function() {
				    return this.opts.page.getTextContent();
	            }.bind(this))
				.then(function(textContent) {

					$(this.textLayer).css({
						height : this.viewport.height+'px',
			            width : this.viewport.width+'px',
			            top : canvasOffset.top,
			            left : canvasOffset.left
			        });

					pdfjsLib.renderTextLayer({
				        textContent: textContent,
				        container: this.textLayer,
				        viewport: this.viewport,
				        textDivs: []
				    });

				    return this.opts.page.getAnnotations();
	            }.bind(this))
				.then(function(annotationData) {

					$(this.annotationLayer).css({
						width : this.viewport.width+'px',
			          	top : canvasOffset.top,
			            left : canvasOffset.left
					});

					pdfjsLib.AnnotationLayer.render({
						viewport: this.viewport.clone({ dontFlip: true }),
				        div: this.annotationLayer,
				        annotations: annotationData,
				        page: this.opts.page,
				        linkService : {
							getDestinationHash: function(dest) {
								return '#';
							},
							getAnchorUrl: function(hash) {
				                return '#';
				            },
				            isPageVisible: function() {
				                return true;
				            },
				            externalLinkTarget: pdfjsLib.LinkTarget.BLANK,
				        }
					});

	            }.bind(this))
            }.bind(this)

});
	
	
