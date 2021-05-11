<geoMapSearch>

<div class="geo-map__wrapper">
	<div ref="geocoder" class="geocoder"/>
	<div id="geoMapSearch" class="geo-map"></div>
</div>


<script>

this.on("mount", function() {
	this.initMap();
});

initMap() {
    console.log("initializing geomap for search", viewerJS);
    this.geoMap = new viewerJS.GeoMap({
        mapId : "geoMapSearch",
        allowMovingFeatures: false,
        language: viewerJS.translator.language,
        popover: undefined,
        emptyMarkerMessage: undefined,
        popoverOnHover: false,
    })
    let initialView = {
        zoom: 5,
        center: [11.073397, 49.451993] //long, lat
    };
    this.geoMap.setMarkerIcon({
        shape: "circle",
        prefix: "fa",
        markerColor: "blue",
        iconColor: "white",
        icon: "fa-circle",
        svg: true
    })
    this.geoMap.init(initialView);
    this.geoMap.initGeocoder(this.refs.geocoder);

}

</script>

</geoMapSearch>