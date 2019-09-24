<geoLocationQuestion>
	

	
	<div if="{this.showInstructions()}" class="annotation_instruction">
		<label>Halten sie die Shift-Taste gedr&#x00FCckt und ziehen Sie im Bild einen Bereich mit der Maus auf.</label>
	</div>
	
	<div id="geoMap" class="geo-map"></div>
	
	<div id="annotation_{index}" each="{anno, index in this.annotations}">
		
	</div>

<script>

this.features = [];
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
        },
        pointToLayer: function(geoJsonPoint, latlng) {
            let marker = L.marker(latlng, {
                draggable: true
            });
            
            marker.on("dragend", function(event) {
                var position = marker.getLatLng();
                geoJsonPoint.geometry.coordinates = [position.lng, position.lat];
                geoJsonPoint.view.zoom = this.
            }.bind(this));
            return marker;
        }.bind(this)
    }).addTo(this.map);
    
    this.map.on("click", function(e) {
        if(this.question.targetFrequency == 0 || this.features.length < this.question.targetFrequency) {
	        var location= e.latlng;
	        this.addFeature(location);
        }
    }.bind(this))

}


addFeature(location) {

    var geojsonFeature = {
        	"type": "Feature",
        	"properties": {
        		"name": "",
        	},
        	"geometry": {
        		"type": "Point",
        		"coordinates": [location.lng, location.lat]
        	},
        	"view" {
        	    "zoom": this.map.getZoom(),
        		"center": this.map.getCenter()
        	}
        };
    this.locations.addData(geojsonFeature);
    this.features.push(geojsonFeature);
}

showInstructions() {
    return !this.opts.item.isReviewMode() && this.question.targetSelector == Crowdsourcing.Question.Selector.RECTANGLE && this.question.annotations.length == 0;
}

</script>


</geoLocationQuestion>

