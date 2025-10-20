<collectionList class="tpl-stacked-collection__collection-list">


	<div if="{collections}" each="{collection, index in collections}" class="card-group">
	 	<div class="card" role="tablist">
	 
		<div class="card-header">

			<div class="card-thumbnail">
				<img if="{collection.thumbnail}" alt="{getValue(collection.label)}" class="img-fluid" src="{collection.thumbnail['@id']}"/>
			</div>
			
			<h3 class="card-title">
				<a href="{getId(collection.rendering)}">{getValue(collection.label)} ({viewerJS.iiif.getContainedWorks(collection)})</a>
				<a if="{hasChildren(collection)}" class="collapsed card-title-collapse" href="#collapse-{this.opts.setindex}-{index}" role="button" data-toggle="collapse" aria-expanded="false">
					<svg class="tpl-stacked-collection__icon tpl-stacked-collection__icon--collapse" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
						<use riot-href="{getIconHref('chevron-down')}"></use>
					</svg>
				</a>
			</h3>
			
			<div class="tpl-stacked-collection__actions">
				<div class="tpl-stacked-collection__info-toggle">
					<a if="{hasDescription(collection)}" href="#description-{this.opts.setindex}-{index}" role="button" data-toggle="collapse" aria-expanded="false">
						<svg class="tpl-stacked-collection__icon tpl-stacked-collection__icon--info" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
							<use riot-href="{getIconHref('info-circle')}"></use>
						</svg>
					</a>
				</div>
					
				<div class="card-rss">
					<a href="{viewerJS.iiif.getRelated(collection, 'Rss feed')['@id']}">
						<svg class="tpl-stacked-collection__icon tpl-stacked-collection__icon--rss" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
							<use riot-href="{getIconHref('rss')}"></use>
						</svg>
					</a>
				</div>
			</div>
	
		</div>
	
			<div if="{hasDescription(collection)}" id="description-{this.opts.setindex}-{index}" class="card-collapse collapse" role="tabcard" aria-expanded="false">
				<p class="tpl-stacked-collection__long-info">
					<raw html="{getDescription(collection)}"></raw>
					
				</p> 
			</div>
		

		<div if="{hasChildren(collection)}" id="collapse-{this.opts.setindex}-{index}" class="card-collapse collapse" role="tabcard" aria-expanded="false">
			<div class="card-body">
				<subCollection if="{collection.members && collection.members.length > 0}" collection="{collection}" language="{this.opts.language}" defaultlanguage="{this.opts.defaultlanguage}"/>
			</div>
		</div>
	
	</div>
</div>


<script>
const ensureTrailingSlash = value => value.endsWith('/') ? value : `${value}/`;
const viewerConfig = window.viewerConfig || {};
this.iconBasePath = ensureTrailingSlash(viewerConfig.iconBasePath || viewerConfig.contextPath || '/');
this.getIconHref = iconName => `${this.iconBasePath}resources/icons/outline/${iconName}.svg#icon`;

riot.tag('raw', '', function(opts) {
    this.root.innerHTML = opts.html;
});

this.collections = this.opts.collections;

this.on("mount", () => {
    // console.log("mounting collectionList", this.opts);
    if(opts.depth == undefined) {
        opts.depth = 1;
    } else {
        opts.depth = parseInt(opts.depth);
    }
    this.loadSubCollections();
})

loadSubCollections() {
    let observable = rxjs.from(this.collections);
    
    for (let level = 0; level < opts.depth; level++) { 
        observable = observable.pipe(
         	rxjs.operators.mergeMap( child => this.fetchMembers(child) ),
         	rxjs.operators.mergeMap( child => child ),
        );
    }
    observable.pipe(
    	rxjs.operators.debounceTime(100)
     )
    .subscribe( () => this.update());

}


fetchMembers(collection) {
    if(this.hasChildren(collection)) {        
	    return fetch(collection['@id'])
	    .then( result => result.json())
	    .then(json => {collection.members = json.members; return collection.members;})
    } else {
        return Promise.resolve([collection]);
    }
}

getValue(element) {
    return viewerJS.iiif.getValue(element, this.opts.language, this.opts.defaultlanguage);
}

hasChildren(element) {
    let count = viewerJS.iiif.getChildCollections(element);
    return count > 0;
}

getChildren(collection) {
    if(collection.members) {        
    	return collection.members.filter( child => viewerJS.iiif.isCollection(child));
    } else {
        return [collection];
    }
}

hasDescription(element) {
    return element.description != undefined;
}

getDescription(element) { 
    return this.getValue(element.description);
}

getId(element) {
    if(!element) {
        return undefined;
    } else if (Array.isArray(element) && element.length > 0) {
        return viewerJS.iiif.getId(element[0]);
    } else {
        return viewerJS.iiif.getId(element);
    }
}


</script>

</collectionList>
