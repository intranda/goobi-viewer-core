<featureSetFilter>

<ul>
	<li each="{filter in filters}">
			<label>{filter.label}</label>
			<div>
				<input type="radio" name="options_{filter.field}" id="options_{filter.field}_all" value="" checked onclick="{resetFilter}"/>
				<label for="options_{filter.field}_all">{opts.msg.alle}</label>
			</div>
			<div each="{ option, index in filter.options}">
				<input type="radio" name="options_{filter.field}" id="options_{filter.field}_{index}" value="{option.name}" onclick="{setFilter}"/>
				<label for="options_{filter.field}_{index}">{option.name}</label>
			</div>
	</li>
</ul>



<script>

this.filters = [];

this.on("mount", () => {
	console.log("mounting featureSetFilter with", this.opts);
	this.geomap = this.opts.geomap;
	this.featureGroups = this.opts.featureGroups;
	this.filters = this.createFilters(this.opts.filters, this.featureGroups);
	this.geomap.onActiveLayerChange.subscribe(groups => {
		this.featureGroups = groups;
		this.filters = this.createFilters(this.opts.filters, this.featureGroups);
 		this.update();
	})
	this.update();
})

createFilters(filterMap, featureGroups) {
	
	let layerNames = featureGroups.map(layer => {
		let label = layer.config.label;
		let labelString = viewerJS.iiif.getValue(label, this.opts.defaultLocale);
		return labelString;
	})
	let filterOptions = layerNames.flatMap(name => filterMap.get(name));
	return filterOptions.filter(f => f != undefined).map(filter => {
		let f = {
			field: filter.value,
			label: filter.label,
			options: this.findValues(featureGroups, filter.value).map(v => {
				return {
					name: v,
					field: filter.value
				}
			}),
		};
		return f;
	})
	.filter(filter => filter.options.length > 1);
}

findValues(featureGroups, filterField) {
	return Array.from(new Set(this.findEntities(featureGroups, filterField)
	.map(e => e[filterField]).map(a => a[0])
	.map(value => viewerJS.iiif.getValue(value, this.opts.locale, this.opts.defaultLocale)).filter(e => e)));
}

findEntities(featureGroups, filterField) {
	return featureGroups.flatMap(group => group.markers).flatMap(m => m.feature.properties.entities).filter(e => e[filterField]);
}

resetFilter(event) {
	//console.log("reset ", event.item.filter);
	let filter = event.item.filter;
	this.featureGroups.forEach(g => g.showMarkers(entity => this.isShowMarker(entity, filter, undefined)));
// 	this.featureGroups.forEach(g => g.showMarkers());
}

setFilter(event) {
	//console.log("set ", event.item.option);
	let filter = this.getFilterForField(event.item.option.field);
	let value = event.item.option.name;
	this.featureGroups.forEach(g => g.showMarkers(entity => this.isShowMarker(entity, filter, value)));
}

isShowMarker(entity, filter, value) {
// 	console.log("isShowMarker", entity, filter, value);
	filter.selectedValue = value;
	let match = this.filters.map(filter => {
		if(filter.selectedValue) {
			let show = entity[filter.field] != undefined && entity[filter.field].map(v => viewerJS.iiif.getValue(v, this.opts.locale, this.opts.defaultLocale)).includes(filter.selectedValue);
// 			console.log("filter " + filter.label + " shows: " + show + " for entity ", entity);
			return show;
		} else {
// 			console.log("filter " + filter.label + " shows all");
			return true;	
		}
	})
	.every(match => match);
	return match;
}

getFilterForField(field) {
	return this.filters.find(f => f.field == field);
}



</script>

</featureSetFilter>