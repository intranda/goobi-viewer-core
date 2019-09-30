<geoLocationQuestion>
	
	<div if="{this.showInstructions()}" class="annotation_instruction">
		<label>{Crowdsourcing.translate("crowdsourcing__help__create_rect_on_image")}</label>
	</div>
	<div if="{this.showInactiveInstructions()}" class="annotation_instruction annotation_instruction_inactive">
		<label>{Crowdsourcing.translate("crowdsourcing__help__make_active")}</label>
	</div>
	
	<div id="geoMap" class="geo-map"></div>
	
	<div id="annotation_{index}" each="{anno, index in this.annotations}">
		
	</div>

<script>


this.question = this.opts.question;
this.markerIdCounter = 1;
this.markers = [];

this.on("mount", function() {
    this.question.initializeView((anno) => new Crowdsourcing.Annotation.GeoJson(anno), this.addAnnotation, this.updateAnnotation, this.focusAnnotation);	    
    this.initMap();
    this.opts.item.onImageOpen(function() {
        this.markerIdCounter = 1;
        this.setFeatures(this.question.annotations);
    }.bind(this));
});

setFeatures(annotations) {
    this.markers.forEach((marker) => {
        marker.remove();
    })
    this.markers = [];
    annotations.filter(anno => !anno.isEmpty()).forEach((anno) => {
        let overlayId = this.addGeoJson(anno.body);
        anno.overlayId = overlayId;
    });
}

addAnnotation(anno) {
    
}

updateAnnotation(anno) {
    //update something on map if annotation is updated on image?
}

/**
 * focus the annotation with the given index
 */
focusAnnotation(index) {
    let anno = this.question.getByIndex(index);
    if(anno) {
        let marker = this.getMarker(anno.overlayId);
        console.log("focus ", anno, marker);
        marker.style.color = "#ff0000";
    }
}

/**
 * check if instructions on how to create a new annotation should be shown
 */
showInstructions() {
    return !this.opts.item.isReviewMode() &&  this.question.active && this.question.targetSelector == Crowdsourcing.Question.Selector.RECTANGLE && this.question.annotations.length == 0;
}

/**
 * check if instructions to acivate this question should be shown, in order to be able to create annotations for this question
 */
showInactiveInstructions() {
    return !this.opts.item.isReviewMode() &&  !this.question.active && this.question.targetSelector == Crowdsourcing.Question.Selector.RECTANGLE && this.opts.item.questions.filter(q => q.isRegionTarget()).length > 1;

}

/**
 * check if a button to add new annotations should be shown
 */
showAddAnnotationButton() {
    return !this.question.isReviewMode() && !this.question.isRegionTarget() && this.question.mayAddAnnotation();
}

/**
 * template method to change the body of an annotation based on an event
 */
setNameFromEvent(event) {
    event.preventUpdate = true;
    if(event.item.anno) {            
        anno.setName(event.target.value);
        this.question.saveToLocalStorage();
    } else {
        throw "No annotation to set"
    }
}

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
                         
            marker.id = geoJsonPoint.id;
            marker.view = geoJsonPoint.view;
                
            marker.getId = function() {
                return this.id;
            }
            
            marker.on("dragend", function(event) {
                var position = marker.getLatLng();
                this.moveFeature(marker, position);
            }.bind(this));
            
            marker.on("click", function(event) {
                this.removeFeature(marker);
            }.bind(this));
            
            this.markers.push(marker);    
            
            return marker;
        }.bind(this)
    }).addTo(this.map);
        
    this.map.on("click", function(e) {
        if(this.question.targetFrequency == 0 || this.markers.length < this.question.targetFrequency) {
	        var location= e.latlng;
	        this.createGeoJson(location, this.map.getZoom(), this.map.getCenter());
        }
    }.bind(this))

}


createGeoJson(location, zoom, center) {
	let id = this.markerIdCounter++;
    var geojsonFeature = {
        	"type": "Feature",
        	"id": id,
        	"properties": {
        		"name": "",
        	},
        	"geometry": {
        		"type": "Point",
        		"coordinates": [location.lng, location.lat]
        	},
        	"view": {
        	    "zoom": zoom,
        		"center": [center.lng, center.lat]
        	}
        };
    this.locations.addData(geojsonFeature);
    this.addFeature(id);
}

getAnnotation(id) {
    return this.question.annotations.find(anno => anno.overlayId == id);
}

getMarker(id) {
    return this.markers.find(marker => marker.getId() == id);
}

addGeoJson(geoJson) {
    let id = this.markerIdCounter++;
    geoJson.id = id;
    this.locations.addData(geoJson);
    return id;
}

/**
 * Add a new marker. If the marker doesn't exist as an annotation, it is added as well
 */
addFeature(id) {
    this.question.addAnnotation(id);
    let annotation = this.getAnnotation(id);
    let marker = this.getMarker(annotation.overlayId);
    annotation.setBody(marker.toGeoJSON());
    annotation.setView(marker.view);
    this.question.saveToLocalStorage();
}

/**
 * Change the location of a feature
 */
moveFeature(marker, location) {
    let annotation = this.getAnnotation(marker.getId());
    marker.setLatLng(location);
    console.log("move marker ", marker, " to ", location);
    if(annotation) {
        annotation.setGeometry(marker.toGeoJSON().geometry);
        annotation.setView({zoom: this.map.getZoom(), center: [this.map.getCenter().lng, this.map.getCenter().lat]});
    }
    console.log("annotations ", this.question.annotations);
    this.question.saveToLocalStorage();
}

/**
 * Remove a marker. If the marker exists as an annotation, it is removed as well
 */
removeFeature(marker) {
    marker.remove();
    let index = this.markers.indexOf(marker);
    this.markers.splice(index, 1);
    let annotation = this.getAnnotation(marker.getId());
    console.log("delete annotation ", annotation);
    if(annotation) {      
	    this.question.deleteAnnotation(annotation);
	    this.question.saveToLocalStorage();
    }
}


</script>


</geoLocationQuestion>

