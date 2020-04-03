<timematrix>

	<div class="timematrix__objects">
		<div each="{image in imageList}" class="timematrix__content">
			
			<div class="imgContent">
				<img src="{image.mediumimage}"> 
			</div>
				
			<h4>{image.title[0]}</h4>
			
			<a href="{image.url}">{opts.msg.goToWork}</a>
		</div> 
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