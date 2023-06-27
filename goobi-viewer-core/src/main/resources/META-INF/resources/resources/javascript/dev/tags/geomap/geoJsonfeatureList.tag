<geoJsonFeatureList onclick="{preventBubble}">

<div class="custom-map__sidebar-inner-wrapper">
	<div class="custom-map__sidebar-inner-top">
		<h4 class="custom-map__sidebar-inner-heading">
			<rawhtml content="{getListLabel()}"></rawhtml>
		</h4>
		<input class="custom-map__sidebar-inner-search-input" type="text" ref="search"  oninput="{filterList}"></input>
	</div>
	<div class="custom-map__sidebar-inner-bottom">
	<ul class="custom-map__inner-wrapper-list">
		<li class="custom-map__inner-wrapper-list-entry" each="{entity in getVisibleEntities()}">
			<a href="{getLink(entity)}"><rawhtml content="{getEntityLabel(entity)}"></rawhtml></a>
		</li>
	</ul>
	</div>
</div>



<script>

this.entities = [];
this.filteredEntities = undefined;

this.on("mount", () => {
	console.log("mount geoJsonFeatureList", this.opts);
	this.opts.featureGroups.forEach(group => {
		group.onFeatureClick.subscribe(f => { 
			console.log("clicked on :", f);
			this.title = f.properties?.title;
			this.setEntities(f.properties?.entities?.filter(e => e.visible !== false).filter(e => this.getLabel(e)?.length > 0));
			console.log("entities", this.entities);
		});
	})
	this.opts.geomap.onMapClick.subscribe(e => this.hide());
	this.hide();
})

setEntities(entities) {
	this.entities = [];
	this.filteredEntities = undefined;
	this.refs["search"].value = "";
	if(entities && entities.length) {		
		this.entities = entities;
		this.show();
		this.update();
	}
}

getVisibleEntities() {
	if(this.filteredEntities === undefined) {
		return this.entities;
	} else {
		return this.filteredEntities;
	}
}

preventBubble(e) {
	event.stopPropagation();
}

filterList(e) {
	let filter = e.target.value;
	if(filter) {		
		this.filteredEntities = this.entities.filter(e => this.getLabel(e).toLowerCase().includes(filter.toLowerCase() ));
	} else {
		this.filteredEntities = undefined;
	}
}

getEntityLabel(entity) {
	if(entity) {		
		return this.getLabel(entity);
	}
}

getListLabel() {
	if(this.title) {
		let label = viewerJS.iiif.getValue(this.title, this.opts.locale, this.opts.defaulLocale);
		return label;
	}
	if(this.entities.length) {		
		let labels = this.opts.listLabelFormat;
		return this.getLabel(this.entities[0], labels); 
	}
}

getLink(entity) {
	if(entity) {		
		return this.getLabel(entity);
	}
}

getLabel(entity) {
	
	if(entity.title) {
		let label = viewerJS.iiif.getValue(entity.title, this.opts.locale, this.opts.defaulLocale);
		return label;
	} else {
		return "";
	}
	
}

hide() {
	this.root.style.display = "none";
}

show() {
	this.root.style.display = "block";
}

</script>

</geoJsonFeatureList>