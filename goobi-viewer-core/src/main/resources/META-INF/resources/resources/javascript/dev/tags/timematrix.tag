<timematrix>

	<div class="card">
		<img each="{image in imageList}" src="{image.mediumimage}" width="100px" height="100px"> 
				
		<a each="{image in imageList}" href="{image.url}">zum Werk</a>
	
		<h4 each="{image in imageList}" name="{image.title}" value="{image.title}"></h4>
	</div>

	

 <script> 
 this.on( 'mount', function() {
 	$(this.opts.button).on("click", this.updateRange)
 	this.imageList=[]
 })
 
 updateRange(event){
	console.log('event', event)
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
     apiTarget += this.opts.count;
     apiTarget += '/';
     console.log('apiTarget', apiTarget)
     
     // get data from api
     var promise = fetch(apiTarget)
     
     // render thumbnails
	fetch(apiTarget)
	.then( function(result) {
	    return result.json(); 
	})
	.then( function(json) {
	    console.log("answer", json);
	    this.imageList=json;
	    this.update()
	}.bind(this))
 }
  
 console.log('Hello', this.opts);
	
 </script>

</timematrix>