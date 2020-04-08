<collectionList>

<div if="{this.collections}" class="panel-group" role="tablist">

	<div each="{collection, index in getChildren(this.collections)}" class="panel">
	
		<div class="panel-heading">
		
			<div class="panel-thumbnail">
				<img class="img-responsive" src="{collection.thumbnail['@id']}"/>
			</div>
			
			<h4 class="panel-title">
				<a if="{!hasChildren(collection)}" href="{collection.rendering[0]['@id']}">{getValue(collection.label)} ({viewerJS.iiif.getContainedWorks(collection)})</a>
				<a if="{hasChildren(collection)}" class="collapsed" href="#collapse-{index}" role="button" data-toggle="collapse" aria-expanded="false">{getValue(collection.label)} ({viewerJS.iiif.getContainedWorks(collection)})</a>
			</h4>
			
			<div class="panel-rss">
				<a href="{viewerJS.iiif.getRelated(collection, 'Rss feed')['@id']}">
					<i class="fa fa-rss" aria-hidden="true"/>
				</a>
			</div>
		
		</div>
		
		<div if="{hasChildren(collection)}" id="collapse-{index}" class="panel-collapse collapse" role="tabpanel" aria-expanded="false">
			<div class="panel-body">
				<ul if="{collection.members && collection.members.length > 0}" class="list">
					<li each="{child in getChildren(collection)}">
						<a class="panel-body__collection" href="{child.rendering[0]['@id']}">{getValue(child.label)} ({viewerJS.iiif.getContainedWorks(child)})</a>
						<a class="panel-body__rss" href="{viewerJS.iiif.getRelated(child, 'Rss feed')['@id']}" target="_blank">
							<i class="fa fa-rss" aria-hidden="true"/>
						</a>
					</li>
					
				</ul>
			</div>
		</div>
	
	</div>

</div>

<script>

this.collections = undefined;
this.level = this.opts.level ? this.opts.level : 0;

this.on("mount", () => {
    console.log("mounting collectionView", this.opts);
    
    this.fetchCollections()
    .then( () => this.update())
    .then( () => this.loadSubCollections())
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
    .then( json => this.collections = json);
}

loadSubCollections() {
    let list = this.getChildren(this.collections);
    let promises = [];
    
    let subject = new Rx.Subject();
    list.forEach( child => {
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


</script>

</collectionList>

