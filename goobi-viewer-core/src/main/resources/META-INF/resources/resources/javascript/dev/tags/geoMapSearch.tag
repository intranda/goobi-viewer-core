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
    this.initMapDraw();

} 

initMapDraw() {
    console.log("init map draw");
    this.drawnItems = new L.FeatureGroup();
    this.geoMap.map.addLayer(this.drawnItems);
    this.drawControl = new L.Control.Draw({
        edit: {
            featureGroup: this.drawnItems,
            edit: false,
            remove: false
        },
        draw: {
            polyline: false,
            marker: false,
            circlemarker: false
        }
    });
    this.geoMap.map.addControl(this.drawControl);
    console.log("initialized map draw", this.drawControl);
    
    this.geoMap.map.on(L.Draw.Event.DRAWSTART, (e) => {
        if(this.searchLayer) {
            this.drawnItems.removeLayer(this.searchLayer);
            this.searchLayer = undefined;
        }
    });
    
    this.geoMap.map.on(L.Draw.Event.CREATED, (e) => {
        console.log("draw event ", e);
        let type = e.layerType;
        this.searchLayer = e.layer;
    	this.drawnItems.addLayer(e.layer);
    });
}

</script>

</geoMapSearch>