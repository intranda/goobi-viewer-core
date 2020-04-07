<collectionView>

<div if="{this.collection}" class="panel-group" role="tablist">

	<div each="{collection in this.collection.members}" class="panel">
	
		<div class="panel-heading">
		
			<div class="panel-thumbnail">
				<img class="img-responsive" src="{collection.thumbnail['@id']}"/>
			</div>
			
			<h4 class="panel-title">
				<a href="{collection.rendering[0]['@id']}">{getValue(collection.label)} ({viewerJS.iiif.getContainedWorks(collection)})</a>
			</h4>
			
			<div class="panel-rss">
				<a href="{viewerJS.iiif.getRelated(collection, 'Rss feed')['@id']}">
					<i class="fa fa-rss" aria-hidden="true"/>
				</a>
			</div>
		
		</div>
	
	</div>

</div>

<script>

this.collection = undefined;

this.on("mount", () => {
    console.log("mounting collectionView", this.opts);
    
    this.fetchCollections()
    .then( () => this.update());
})

fetchCollections() {
    let url = this.opts.url;
    if(this.opts.baseCollection) {
        url += this.opts.baseCollection + "/";
    }
    if(this.opts.grouping) {
        url += "grouping/" + this.opts.grouping + "/";
    }
    return fetch(url)
    .then( result => result.json())
    .then( json => this.collection = json);
}

getValue(element) {
    return viewerJS.iiif.getValue(element, this.opts.language);
}


</script>

</collectionView>