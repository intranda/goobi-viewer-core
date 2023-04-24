<featureSetFilter>

<ul>
	<li each="{filter in filters}">
			<label>{filter.field}</label>
			<div>
				<input type="radio" name="options_{filter.field}" id="options_{filter.field}_all" value="" checked onclick="{resetFilter}"/>
				<label for="options_{filter.field}_all">Alle</label>
			</div>
			<div each="{ option, index in filter.options}">
				<input type="radio" name="options_{filter.field}" id="options_{filter.field}_{index}" value="{option.name}" onclick="{setFilter}"/>
				<label for="options_{filter.field}_{index}">{option.name}</label>
			</div>
	</li>
</ul>



<script>

this.locale = undefined;
this.filters = [];

this.on("mount", () => {
	console.log("mounting featureSetFilter with", this.opts);
	this.locale = this.opts.locale;
	this.geomap = this.opts.geomap;
	this.filters = this.opts.filters.map(filter => {
		return {
			field: filter.field,
			options: this.findValues(filter.featureGroup, filter.field, this.locale).map(v => {
				return {
					name: v,
					field: filter.field
				}
			}),
			markers: filter.featureGroup.markers,
			featureGroup: filter.featureGroup
		}
	})
	this.update();
})

setEntities(entities) {
	if(entities && entities.length) {		
		this.entities = entities;
		this.update();
	}
}

findValues(featureGroup, filterField, locale) {
	return Array.from(new Set(this.findEntities(featureGroup, filterField)
	.map(e => e[filterField]).map(a => a[0])
	.map(value => viewerJS.iiif.getValue(value, locale)).filter(e => e)));
}

findEntities(featureGroup, filterField) {
	return featureGroup.markers.flatMap(m => m.feature.properties.entities).filter(e => e[filterField]);
}

getLabel(entity) {
	label = viewerJS.iiif.getValue(entity[this.opts.labelField][0], this.locale);
	return label;
}

resetFilter(event) {
	let filter = event.item.filter;
	this.getAllEntities(filter.featureGroup).forEach(entity => entity.visible = true);
	this.refreshMarkers(filter);
}

getFilterForField(field) {
	return this.filters.find(f => f.field == field);
}

getAllEntities(featureGroup) {
	let entities = featureGroup.markers.flatMap(m => m.feature.properties.entities);
	return entities ? entities : [];
}

setFilter(event) {
	let filter = this.getFilterForField(event.item.option.field);
	let value = event.item.option.name;
	this.getAllEntities(filter.featureGroup).forEach(entity => {
		entity.visible = entity[filter.field] != undefined && entity[filter.field].map(v => viewerJS.iiif.getValue(v, this.locale)).includes(value);
	});
	this.refreshMarkers(filter);
}

refreshMarkers(filter) {
	filter.featureGroup.layer.clearLayers();
	if(filter.featureGroup.cluster) {		
		filter.featureGroup.cluster.clearLayers();
		filter.featureGroup.layer.addLayer(filter.featureGroup.cluster);
	}
	filter.markers.filter(m => filter.featureGroup.getCount(m.feature.properties)).forEach(m => {
		m.setIcon(filter.featureGroup.getMarkerIcon(m.feature.properties));
		if(filter.featureGroup.cluster) {
			filter.featureGroup.cluster.addLayer(m);
		} else {
			filter.featureGroup.layer.addLayer(m);
		}
	})
}


</script>

</featureSetFilter>