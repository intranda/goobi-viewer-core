<timematrix>

	<div class="timematrix__objects">
		<div each="{image in imageList}" class="timematrix__content">
			<div id="imageMap" class="timematrix__img">
				<a href="{image.url}">
					<img src="{image.mediumimage}" class="timematrix__image" data-viewer-thumbnail="thumbnail" onError="this.onerror=null;this.src='/viewer/resources/images/access_denied.png'" /> 
					<div class="timematrix__text">	
						<p if="{image.title}" name="timetext" class="timetext">{image.title[0]}</p>
					</div>
				</a>
			</div>	
		</div> 
	</div>
	 
	 <script>
	//<![CDATA[		
		 this.on( 'mount', function() {
		 	$(this.opts.button).on("click", this.updateRange)
		 	this.imageList=[]
		 })
		 
		 updateRange(event){
			this.getTimematrix()
		}
		 getTimematrix(){
		     
		     // build api target
		     var apiTarget = this.opts.contextPath;
		     apiTarget += 'rest/records/timematrix/range/';
		     apiTarget += $(this.opts.startInput).val();
		     apiTarget += "/";
		     apiTarget += $(this.opts.endInput).val();
		     apiTarget += '/';
		     apiTarget += $(this.opts.count).val();
		     apiTarget += '/';
		     
		     // render thumbnails
		    opts.loading.show()
			fetch(apiTarget)
			.then( function(result) {
			    return result.json(); 
			})
			.then( function(json) {
			    this.imageList=json;
			    this.update()
			    opts.loading.hide()
			}.bind(this));
		 }

		//]]>
	</script>
	
</timematrix>