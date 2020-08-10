<collectionList class="tpl-stacked-collection__collection-list">


	<div if="{collections}" each="{collection, index in collections}" class="card-group">
	 	<div class="card" role="tablist">
	 
		<div class="card-header">

			<div class="card-thumbnail">
				<img if="{collection.thumbnail}" class="img-fluid" src="{collection.thumbnail['@id']}"/>
			</div>
			
			<h4 class="card-title">
				<a if="{!hasChildren(collection)}" href="{collection.rendering[0]['@id']}">{getValue(collection.label)} ({viewerJS.iiif.getContainedWorks(collection)})</a>
				<a if="{hasChildren(collection)}" class="collapsed" href="#collapse-{this.opts.setindex}-{index}" role="button" data-toggle="collapse" aria-expanded="false">
					<span>{getValue(collection.label)} ({viewerJS.iiif.getContainedWorks(collection)})</span>
					<i class="fa fa-angle-flip" aria-hidden="true"></i>
				</a>
			</h4>
			
			<div class="tpl-stacked-collection__actions">
				<div class="tpl-stacked-collection__info-toggle">
					<a if="{hasDescription(collection)}" href="#description-{this.opts.setindex}-{index}" role="button" data-toggle="collapse" aria-expanded="false">
						<i class="fa fa-info-circle" aria-hidden="true"></i>
					</a>
				</div>
					
				<div class="card-rss">
					<a href="{viewerJS.iiif.getRelated(collection, 'Rss feed')['@id']}">
						<i class="fa fa-rss" aria-hidden="true"/>
					</a>
				</div>
			</div>
	
		</div>
	
			<div if="{hasDescription(collection)}" id="description-{this.opts.setindex}-{index}" class="card-collapse collapse" role="tabcard" aria-expanded="false">
				<p class="tpl-stacked-collection__long-info">
					{getDescription(collection)}
				</p>
			</div>
		

		<div if="{hasChildren(collection)}" id="collapse-{this.opts.setindex}-{index}" class="card-collapse collapse" role="tabcard" aria-expanded="false">
			<div class="card-body">
				<ul if="{collection.members && collection.members.length > 0}" class="list">
					<li each="{child in getChildren(collection)}">
						<a class="card-body__collection" href="{child.rendering[0]['@id']}">{getValue(child.label)} ({viewerJS.iiif.getContainedWorks(child)})</a>
						<a class="card-body__rss" href="{viewerJS.iiif.getRelated(child, 'Rss feed')['@id']}" target="_blank">
							<i class="fa fa-rss" aria-hidden="true"/>
						</a>
					</li>
					
				</ul>
			</div>
		</div>
	
	</div>
</div>


<script>

this.collections = this.opts.collections;

this.on("mount", () => {
    console.log("mounting collectionList", this.opts);
    this.loadSubCollections();
})

loadSubCollections() {
    let promises = [];
    
    let subject = new Rx.Subject();
    this.collections.forEach( child => {
        fetch(child['@id'])
        .then( result => result.json())
        .then(json => {
            child.members = json.members;
            subject.next(child);
        })
        .catch( error => {
           subject.error(error);
        });
    });

    subject
    .pipe(RxOp.debounceTime(100))
    .subscribe( () => this.update())
}

getValue(element) {
    return viewerJS.iiif.getValue(element, this.opts.language);
}

hasChildren(element) {
    let count = viewerJS.iiif.getChildCollections(element);
    return count > 0;
}

getChildren(collection) {
    return collection.members.filter( child => viewerJS.iiif.isCollection(child));
}

hasDescription(element) {
    return element.description != undefined;
}

getDescription(element) { 
    return this.getValue(element.description);
}


</script>

</collectionList>

