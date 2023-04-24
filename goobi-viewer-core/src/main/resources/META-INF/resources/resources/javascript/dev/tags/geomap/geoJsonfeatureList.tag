<geoJsonFeatureList>

<h4>{opts.title}</h4>
<ul>
	<li each="{entity in entities}">{getLabel(entity)}</li>
</ul>



<script>

this.locale = undefined;
this.entities = [];

this.on("mount", () => {
	console.log("mounting ", this.opts);
	this.locale = this.opts.locale;
	this.opts.featureGroups.forEach(group => {
		
		group.onFeatureClick.subscribe(f => this.setEntities(f.properties?.entities?.filter(e => e.visible !== false)));
	})
	
})

setEntities(entities) {
	if(entities && entities.length) {		
		this.entities = entities;
		this.update();
	}
}

getLabel(entity) {
	label = viewerJS.iiif.getValue(entity[this.opts.labelField][0], this.locale);
	return label;
}

</script>

</geoJsonFeatureList>