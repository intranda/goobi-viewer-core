<geoJsonFeatureList onclick="{preventBubble}">

<h4>{getListLabel()}</h4>
<input type="text" ref="search"  oninput="{filterList}"></input>
<ul>
	<li each="{entity in getVisibleEntities()}">{getLabel(entity)}</li>
</ul>



<script>

this.defaultDisplay = undefined;
this.entities = [];
this.filteredEntities = undefined;

this.on("mount", () => {
	console.log("mounting ", this.opts);
	this.opts.featureGroups.forEach(group => {
		group.onFeatureClick.subscribe(f => this.setEntities(f.properties?.entities?.filter(e => e.visible !== false)));
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

getListLabel() {
	return this.entities[0]["MD_LOCATION"]?.map(s => viewerJS.iiif.getValue(s, this.opts.locale, this.opts.defaultLocale)).join(", ")
}

getLabel(entity) {
	let labels = this.opts.labelFormat;
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

hide() {
	this.defaultDisplay = this.root.style.display;
	this.root.style.display = "none";
}

show() {
	this.root.style.display = this.defaultDisplay;
}

</script>

</geoJsonFeatureList>