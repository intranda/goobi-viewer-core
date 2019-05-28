<pdfPage>

	<div class="page" id="page_{opts.pageno}">
		<canvas class="pdf-canvas" id="pdf-canvas_{opts.pageno}"></canvas>
		<div class="text-layer" id="pdf-text_{opts.pageno}"></div>
		<div class="annotation-layer" id="pdf-annotations_{opts.pageno}"></div>
	</div>

<script>
	this.on('mount', function () {
		console.log("load page ", this.opts.pageno, this.opts.page);

           this.container = document.getElementById( "page_" + this.opts.pageno );
           this.canvas = document.getElementById( "pdf-canvas_" + this.opts.pageno );
           this.textLayer = document.getElementById( "pdf-text_" + this.opts.pageno );
           this.annotationLayer = document.getElementById( "pdf-annotations_" + this.opts.pageno );
           
		var containerWidth = $(this.container).width();
		var pageWidth = this.opts.page._pageInfo.view[2];
           var scale = containerWidth/pageWidth;
		this.viewport = this.opts.page.getViewport( scale );
		
           if(this.container) {
               this.loadPage();
           }
	});
            


    loadPage() {
        var canvasOffset = $( this.canvas ).offset();
        var context = this.canvas.getContext( "2d" );
        this.canvas.height = this.viewport.height;
        this.canvas.width = this.viewport.width;
        
        console.log( "render ", this.opts.page, context, this.viewport );
        
        this.opts.page.render( {
            canvasContext: context,
            viewport: this.viewport
        } ).then( function() {
            return this.opts.page.getTextContent();
        }.bind( this ) ).then( function( textContent ) {
            console.log( "viewport ", this.viewport );
            $( this.textLayer ).css( {
                height: this.viewport.height + 'px',
                width: this.viewport.width + 'px',
            } );
            
            // Create new instance of TextLayerBuilder class
            pdfjsLib.renderTextLayer( {
                textContent: textContent,
                container: this.textLayer,
                viewport: this.viewport,
                textDivs: []
            } );
            
            return this.opts.page.getAnnotations();
        }.bind( this ) ).then( function( annotationData ) {
            
            $( this.annotationLayer ).css( {
                width: this.viewport.width + 'px',
            } );
            
            pdfjsLib.AnnotationLayer.render( {
                viewport: this.viewport.clone( {
                    dontFlip: true
                } ),
                div: this.annotationLayer,
                annotations: annotationData,
                page: this.opts.page,
                linkService: {
                    getDestinationHash: function( dest ) {
                        return '#';
                    },
                    getAnchorUrl: function( hash ) {
                        return '#';
                    },
                    isPageVisible: function() {
                        return true;
                    },
                    externalLinkTarget: pdfjsLib.LinkTarget.BLANK,
                }
            } );
            
        }.bind( this ) )
    }

    //     class SimpleLinkService {
    //         constructor() {
    //           this.externalLinkTarget = null;
    //           this.externalLinkRel = null;
    //         }
    
    //         get pagesCount() {
    //           return 0;
    //         }
    
    //         get page() {
    //           return 0;
    //         }
    
    //         set page(value) {}
    
    //         get rotation() {
    //           return 0;
    //         }
    
    //         set rotation(value) {}
    
    //         navigateTo(dest) {}
    
    //         getDestinationHash(dest) {
    //           return '#';
    //         }
    
    //         getAnchorUrl(hash) {
    //           return '#';
    //         }
    
    //         setHash(hash) {}
    
    //         executeNamedAction(action) {}
    
    //         cachePageRef(pageNum, pageRef) {}
    
    //         isPageVisible(pageNumber) {
    //           return true;
    //         }
    //       }
</script> 
</pdfPage>
	
	