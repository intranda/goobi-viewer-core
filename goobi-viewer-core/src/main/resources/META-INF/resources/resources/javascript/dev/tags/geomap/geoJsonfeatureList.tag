<geoJsonFeatureList onclick="{preventBubble}">

<div class="custom-map__sidebar-inner-wrapper">
	<div class="custom-map__sidebar-inner-top">
		<h4 class="custom-map__sidebar-inner-heading">{getListLabel()}</h4>
		<input class="custom-map__sidebar-inner-search-input" type="text" ref="search"  oninput="{filterList}"></input>
	</div>
	<div class="custom-map__sidebar-inner-bottom">
	<ul class="custom-map__inner-wrapper-list">
		<li class="custom-map__inner-wrapper-list-entry" each="{entity in getVisibleEntities()}">
			<a href="{getLink(entity)}">{getEntityLabel(entity)}</a>
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
			this.setEntities(f.properties?.entities?.filter(e => e.visible !== false));
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
		let labels = this.opts.entityLabelFormat;
		return this.getLabel(entity, labels);
	}
}

getListLabel() {
	if(this.title) {
		return viewerJS.iiif.getValue(this.title, this.opts.locale, this.opts.defaulLocale);
	}
	if(this.entities.length) {		
		let labels = this.opts.listLabelFormat;
		return this.getLabel(this.entities[0], labels); 
	}
}

getLink(entity) {
	if(entity) {		
		return this.getLabel(entity, this.opts.entityLinkFormat);
	}
}

getLabel(entity, labels) {
	
	if(entity.title) {
		return viewerJS.iiif.getValue(entity.title, this.opts.locale, this.opts.defaulLocale);
	} else {
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

hide() {
	this.root.style.display = "none";
}

show() {
	this.root.style.display = "block";
}

</script>

</geoJsonFeatureList>