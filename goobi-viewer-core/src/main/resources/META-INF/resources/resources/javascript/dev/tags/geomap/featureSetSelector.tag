<featureSetSelector>

<div class="tab" if="{featureGroups.length > 1}">
	<button each="{featureGroup, index in featureGroups}" class="tablinks {isActive(featureGroup) ? '-active':''}" onclick="{setFeatureGroup}">{getLabel(featureGroup)}</button>
</div>


<script>

this.featureGroups = [];

this.on("mount", () => {
	this.featureGroups = opts.featureGroups;
	this.geomap = opts.geomap;
	this.update();
})

setFeatureGroup(event) {
	let featureGroup = event.item.featureGroup;
	this.geomap.setActiveLayers([featureGroup]);
}

getLabel(featureGroup) {
	return viewerJS.iiif.getValue(featureGroup.config.label, this.opts.locale, this.opts.defaultLocale);
}

isActive(featureGroup) {
	return featureGroup.active;
}

</script>

</featureSetSelector>