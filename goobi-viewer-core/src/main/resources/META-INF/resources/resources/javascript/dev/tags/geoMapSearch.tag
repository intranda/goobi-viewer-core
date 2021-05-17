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
    	switch(type) {
    	    case "circle":
    	        {    	        
	    	        let bounds = this.searchLayer.getBounds();
	    	        //L.rectangle(bounds, {color: 'yellow', weight: 1}).addTo(this.geoMap.map);
	    	        let circumgon = this.createCircumgon(bounds.getCenter(), bounds.getSouthWest(), bounds.getSouthWest(), 16);
	    	        //this.drawPolygon(circumgon);
	    	        let diameterM = bounds.getSouthWest().distanceTo(bounds.getNorthWest());
	    	        this.notifyFeatureSet({
	    	            type : "polygon",
	    	            vertices: circumgon.map(p => [p.lat, p.lng]),
	    	        })
	    	        break;
    	        }
    	    case "rectangle":
    	        {    	        
	    	        let bounds = this.searchLayer.getBounds();
	    	        let vertices = [bounds.getNorthWest(), bounds.getNorthEast(), bounds.getSouthEast(), bounds.getSouthWest(), bounds.getNorthWest()];
	    	        this.notifyFeatureSet({
	    	            type : "polygon",
	    	            vertices: vertices.map(p => [p.lat, p.lng])
	    	        })
	    	        break;
    	        }
    	    case "polygon":
    	    {    	
    	        let vertices = this.searchLayer.getLatLngs()[0];
    	        vertices.push(vertices[0]);
    	        console.log("vertices", vertices);
    	        this.notifyFeatureSet({
    	            type : "polygon",
    	            vertices: vertices.map(p => [p.lat, p.lng])
    	        })
    	        break;
	        }
    	}
    });
}

createCircumgon(center, sw, ne, numVertices) {
    //console.log("create circumgon ", center, sw, ne, numVertices);
    let lSW = this.geoMap.map.latLngToLayerPoint(sw);
    let lNE = this.geoMap.map.latLngToLayerPoint(ne);
    let lCenter = this.geoMap.map.latLngToLayerPoint(center);
    //console.log("layer points ", lSW, lNE, lCenter);
    let radius = Math.abs(lCenter.x - lSW.x);
    //console.log("radius ", radius);
    let points = [];
    for(let i = 0; i < numVertices; i++) {
        let x = lCenter.x + radius *  Math.cos(2*Math.PI*i/numVertices);
        let y = lCenter.y + radius *  Math.sin(2*Math.PI*i/numVertices);
        points.push([x,y]);
    }
    points.push(points[0]);
    let geoPoints = points.map(p => this.geoMap.map.layerPointToLatLng(p));
    return geoPoints;
}

drawPolygon(points) {
    console.log("draw polygon", points, "in");
    let conf =  {color: 'red'};
    let poly = L.polygon(points, conf).addTo(this.geoMap.map);
    console.log("poly ", poly)
}

notifyFeatureSet(feature) {
    console.log("Set feature ", feature);
    switch(feature.type) {
        case "polygon":
            let searchString = this.buildSearchString(feature.vertices);
            console.log("search string = ", searchString);
            if(this.opts.onFeatureSelect) {
                this.opts.onFeatureSelect(searchString);
            }
            break;
        default:
           logger.error("Feature type not implemented", feature);
    }
}

buildSearchString(vertices) {
    let string = "WKT_COORDS:\"IsWithin(POLYGON((";
    string += vertices.map(v => v[1] + " " + v[0]).join(", ");
    string += "))) distErrPct=0\"";
    return string;
}

</script>

</geoMapSearch>