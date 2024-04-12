<geoJsonFeatureList onclick="{preventBubble}">

<div class="custom-map__sidebar-inner-wrapper">
	<div class="custom-map__sidebar-inner-top">
		<h4 class="custom-map__sidebar-inner-heading">
			<rawhtml content="{getListLabel()}"></rawhtml>
		</h4>
		<input if="{getVisibleEntities().length > 0}" class="custom-map__sidebar-inner-search-input" type="text" ref="search"  oninput="{filterList}"></input>
	</div>
	<div class="custom-map__sidebar-inner-bottom">
	<ul if="{getVisibleEntities().length > 0}" class="custom-map__inner-wrapper-list">
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
	this.opts.featureGroups.forEach(group => {
		group.onFeatureClick.subscribe(f => { 
			this.title = f.properties?.title;
			this.setEntities(f.properties?.entities?.filter(e => e.visible !== false).filter(e => this.getEntityLabel(e)?.length > 0));
		});
	})
	this.opts.geomap.onMapClick.subscribe(e => this.hide());
	this.hide();
})

setEntities(entities) {
	//console.log("Show Entities", entities, this.opts.showAlways);
	this.entities = [];
	this.filteredEntities = undefined;
	if(this.refs["search"]) {		
		this.refs["search"].value = "";
	}
	if(entities?.length || this.opts.showAlways) {		
		this.entities = entities;
		this.show();
		this.update();
	}
}

getVisibleEntities() {
	if(!this.entities) {
		return [];
	} else if(this.filteredEntities === undefined) {
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
}

getLink(entity) {
	if(entity) {		
		let labels = this.opts.entityLinkFormat;
		label = labels.map(format => {
			let groups = [...format.matchAll(/\${(.*?)}/g)];
			let l = "";
			groups.forEach(group => {
				if(group.length > 1) {
					let value = entity[group[1]]?.map(s => viewerJS.iiif.getValue(s, this.opts.locale, this.opts.defaultLocale)).join(", ");
					if(value) {					
						l += format.replaceAll(group[0], value ? value : "");
					}
				}
			})
			return l;
		}).join("");
		return label;
		
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