<featureSetFilter>

<ul if="{filters.length > 0}">
	<li each="{filter in filters}"  class="{filter.styleClass}">
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
	this.geomap = this.opts.geomap;
	this.featureGroups = this.opts.featureGroups;
	this.filters = this.createFilters(this.opts.filters, this.featureGroups);
	if(this.opts.comparator) {
		this.filters.forEach(filter => {
			if(filter.options) {				
				filter.options.sort(this.opts.comparator.compare);
			}
		})
	}
	this.geomap.onActiveLayerChange.subscribe(groups => {
		this.featureGroups = groups;
		this.filters = this.createFilters(this.opts.filters, this.featureGroups);
 		this.update();
	})
	this.update();
})

createFilters(filterMap, featureGroups) {
	let filters = [];
	for (const entry of filterMap.entries()) {
		let layerName = entry[0];
		let filterConfigs = entry[1];
		let groups = featureGroups.filter(g => this.getLayerName(g) == layerName);
		if(layerName && filterConfigs && filterConfigs.length > 0 && groups.length > 0) {
			filterConfigs.forEach(filterConfig => {
				let filter = {
						field: filterConfig.value,
						label: filterConfig.label,
						styleClass: filterConfig.styleClass,
						layers: groups,
						options: this.findValues(groups, filterConfig.value).map(v => {
							return {
								name: v,
								field: filterConfig.value
							}
						}),
					};
				filters.push(filter);
			});
		}
	}
	return filters.filter(filter => filter.options.length > 1);
}

getLayerName(layer) {
	let name = viewerJS.iiif.getValue(layer.config.label, this.opts.defaultLocale);
	return name;
}

getFilterName(filter) {
	let name = viewerJS.iiif.getValue(filter.label, this.opts.defaultLocale);
	return name;
}

findValues(featureGroups, filterField) {
	return Array.from(new Set(this.findEntities(featureGroups, filterField)
	.map(e => e[filterField]).map(a => a[0])
	.map(value => viewerJS.iiif.getValue(value, this.opts.locale, this.opts.defaultLocale)).filter(e => e)));
}

findEntities(featureGroups, filterField) {
	let entities = featureGroups.flatMap(group => group.markers).filter(m => m.feature.properties.entities).flatMap(m => m.feature.properties.entities).filter(e => e[filterField]);
	return entities;
}

resetFilter(event) {
	let filter = event.item.filter;
	filter.layers.forEach(g => g.showMarkers(entity => this.isShowMarker(entity, filter, undefined)));
}

setFilter(event) {
	let filter = this.getFilterForField(event.item.option.field);
	let value = event.item.option.name;
	filter.layers.forEach(g => g.showMarkers(entity => this.isShowMarker(entity, filter, value)));
}

isShowMarker(entity, filter, value) {
	let filters = this.filters.filter(f => f.layers.filter(g => filter.layers.includes(g)).length > 0);

	filter.selectedValue = value;
	let match = filters.map(filter => {
		if(filter.selectedValue) {
			let show = entity[filter.field] != undefined && entity[filter.field].map(v => viewerJS.iiif.getValue(v, this.opts.locale, this.opts.defaultLocale)).includes(filter.selectedValue);
			return show;
		} else {
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