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

createFilters(filterOptions, featureGroups) {
	return filterOptions.map(filter => {
		return {
			field: filter,
			options: this.findValues(featureGroups, filter).map(v => {
				return {
					name: v,
					field: filter.field
				}
			}),
		}
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

resetFilter() {
	this.featureGroups.forEach(g => g.showMarkers());
}

setFilter(event) {
	let filter = this.getFilterForField(event.item.option.field);
	let value = event.item.option.name;
	this.featureGroups.forEach(g => g.showMarkers(entity => entity[filter.field] != undefined && entity[filter.field].map(v => viewerJS.iiif.getValue(v, this.opts.locale, this.opts.defaultLocale)).includes(value)));
}

getFilterForField(field) {
	return this.filters.find(f => f.field == field);
}



</script>

</featureSetFilter>