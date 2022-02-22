<pdfDocument>

	<div class="pdf-container">
		
			<pdfPage each="{page, index in pages}" page="{page}" pageNo="{index+1}"></pdfPage>
	
	</div>

	<script>
	
		this.pages = [];
	
		var loadingTask = pdfjsLib.getDocument( this.opts.data );
	    loadingTask.promise.then( function( pdf ) {
	        var pageLoadingTasks = [];
	        for(var pageNo = 1; pageNo <= pdf.numPages; pageNo++) {
   		        var page = pdf.getPage(pageNo);
   		        pageLoadingTasks.push(page);
   		    }
   		    return Promise.allSettled(pageLoadingTasks);
	    }.bind(this))
	    .then(function(results) {
			results.forEach(result => {
			    if (result.status === "fulfilled") {
                	var page = result.value;
                	this.pages.push(page);
                } else {
                    console.log("Error loading page: ", result);
                }
			});
			this.update();
        }.bind(this))
	    .then( function() {
			$(".pdf-container").show();
            $( '#literatureLoader' ).hide();
		} );
	
	</script>

</pdfDocument>