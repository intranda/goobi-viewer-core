<geoLocationQuestion>
	

	
	<div if="{this.showInstructions()}" class="annotation_instruction">
		<label>Halten sie die Shift-Taste gedr&#x00FCckt und ziehen Sie im Bild einen Bereich mit der Maus auf.</label>
	</div>
	
	<div id="geoMap" class="geo-map"></div>
	
	<div id="annotation_{index}" each="{anno, index in this.annotations}">
		
	</div>

<script>

this.question = this.opts.question;
this.question.createAnnotation = function(anno) {
    let annotation = new Crowdsourcing.Annotation.GeoJson(anno);
    annotation.generator = this.question.getGenerator();
    annotation.creator = this.opts.item.getCreator();
    return annotation;
}.bind(this);

this.on("mount", function() {
    this.initMap();
})

initMap() {
    this.map = new L.Map('geoMap');
    var osm = new L.TileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      minZoom: 0,
      maxZoom: 20,
      attribution: 'Map data &copy; <a href="https://openstreetmap.org">OpenStreetMap</a> contributors'
    });
 
    // define view
    this.map.setView(new L.LatLng(49.451993, 11.073397), 5);
    this.map.addLayer(osm);
    
    this.locations = L.geoJSON([], {
        style : {
                "color": "#ff7800",
            	"weight": 5,
            	"opacity": 0.65  
        }
    }).addTo(this.map);
    
    this.map.on("click", function(e) {
        var location= e.latlng;
        this.addFeature(location);
    }.bind(this))
    
    //add geojson layer
//     L.geoJSON(geojsonFeature).addTo(map);
}


addFeature(location) {

    var geojsonFeature = {
        	"type": "Feature",
        	"properties": {
        		"name": "Coors Field",
        		"amenity": "Baseball Stadium",
        		"popupContent": "This is where the Rockies play!"
        	},
        	"geometry": {
        		"type": "Point",
        		"coordinates": [location.lng, location.lat]
        	}
        };
    console.log("add feature ", geojsonFeature ), 
    this.locations.addData(geojsonFeature);
}

showInstructions() {
    return !this.opts.item.isReviewMode() && this.question.targetSelector == Crowdsourcing.Question.Selector.RECTANGLE && this.question.annotations.length == 0;
}

</script>


</geoLocationQuestion>

