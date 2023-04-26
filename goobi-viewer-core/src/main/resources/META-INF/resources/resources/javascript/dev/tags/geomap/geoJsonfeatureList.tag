<geoJsonFeatureList>

<h4>{opts.title}</h4>
<ul>
	<li each="{entity in entities}">{getLabel(entity)}</li>
</ul>



<script>

this.locale = undefined;
this.defaultDisplay = undefined;
this.entities = [];

this.on("mount", () => {
	console.log("mounting ", this.opts);
	this.locale = this.opts.locale;
	this.opts.featureGroups.forEach(group => {
		group.onFeatureClick.subscribe(f => this.setEntities(f.properties?.entities?.filter(e => e.visible !== false)));
	})
	this.opts.geomap.onMapClick.subscribe(e => this.hide());
	this.hide();
})

setEntities(entities) {
	if(entities && entities.length) {		
		this.entities = entities;
		this.show();
		this.update();
	}
}

getLabel(entity) {
	let groups = [...this.opts.labelFormat.matchAll(/\${(.*?)}/g)];
	let label = this.opts.labelFormat;
	groups.forEach(group => {
		if(group.length > 1) {
			console.log("group ", entity[group[1]]);
			let value = entity[group[1]].map(s => viewerJS.iiif.getValue(s, this.locale)).join(", ");
			label = label.replaceAll(group[0], value);
		}
	})
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