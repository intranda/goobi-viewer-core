<geoLocationQuestion>
	
	<div if="{this.showInstructions()}" class="annotation_instruction">
		<label>{Crowdsourcing.translate("crowdsourcing__help__create_rect_on_image")}</label>
	</div>
	<div if="{this.showAddMarkerInstructions()}" class="annotation_instruction">
		<label>{Crowdsourcing.translate("crowdsourcing__help__add_marker_to_image")}</label>
	</div>
	<div if="{this.showInactiveInstructions()}" class="annotation_instruction annotation_instruction_inactive">
		<label>{Crowdsourcing.translate("crowdsourcing__help__make_active")}</label>
	</div>
	
	<div id="geoMap_{opts.index}" class="geo-map"></div>
	
	<div id="annotation_{index}" each="{anno, index in this.annotations}">
		
	</div>

<script>


this.question = this.opts.question;
this.annotationToMark = null;
this.addMarkerActive = !this.question.isRegionTarget() && !this.opts.item.isReviewMode();


this.on("mount", function() {
	this.opts.item.onItemInitialized( () => {	    
	    this.question.initializeView((anno) => new Crowdsourcing.Annotation.GeoJson(anno), this.addAnnotation, this.updateAnnotation, this.focusAnnotation);	    
	    this.initMap();
	    this.opts.item.onImageOpen(() => this.resetFeatures());
	    this.opts.item.onAnnotationsReload(() => this.resetFeatures());
	})
});

setView(view) {
    this.map.setView(view.center, view.zoom);
}

resetFeatures() {
    this.markerIdCounter = 1;
    this.setFeatures(this.question.annotations);
    if(this.markers.length > 0) {            
   		this.setView(this.markers[0].view);
    }
}

setFeatures(annotations) {
    this.markers.forEach((marker) => {
        marker.remove();
    })
    this.markers = [];
    annotations.filter(anno => !anno.isEmpty()).forEach((anno) => {
        let markerId = this.addGeoJson(anno.body);
        anno.markerId = markerId;
    });
}

addAnnotation(anno) {
   this.addMarkerActive = true; 
   this.annotationToMark = anno;
   if(this.question.areaSelector) {
       this.question.areaSelector.disableDrawer();
   }
   this.update();
}

updateAnnotation(anno) {
    this.focusAnnotation(this.question.getIndex(anno));
}

/**
 * focus the annotation with the given index
 */
focusAnnotation(index) {
    let anno = this.question.getByIndex(index);
    if(anno) {
        let marker = this.getMarker(anno.markerId);
        if(marker) {            
	        console.log("focus ", anno, marker);
	        
        }
    }
}

/**
 * check if instructions on how to create a new annotation should be shown
 */
showInstructions() {
    return !this.addMarkerActive && !this.opts.item.isReviewMode() &&  this.question.active && this.question.isRegionTarget();
}

/**
 * check if instructions to acivate this question should be shown, in order to be able to create annotations for this question
 */
showInactiveInstructions() {
    return !this.opts.item.isReviewMode() &&  !this.question.active && this.question.isRegionTarget() && this.opts.item.questions.filter(q => q.isRegionTarget()).length > 1;

}

showAddMarkerInstructions() {
    return this.addMarkerActive && !this.opts.item.isReviewMode() &&  this.question.active && this.question.isRegionTarget() ;

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
    this.map = new L.Map('geoMap_' + this.opts.index);
    var osm = new L.TileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      minZoom: 0,
      maxZoom: 20,
      attribution: 'Map data &copy; <a href="https://openstreetmap.org">OpenStreetMap</a> contributors'
    });
 
    // define view
    this.map.setView(new L.LatLng(49.451993, 11.073397), 5);
    this.map.addLayer(osm);
    
    this.locations = L.geoJSON([], {
        pointToLayer: function(geoJsonPoint, latlng) {
            let marker = L.marker(latlng, {
                draggable: !this.opts.item.isReviewMode()
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
        if(this.addMarkerActive && (this.question.targetFrequency == 0 || this.markers.length < this.question.targetFrequency)) {
	        var location= e.latlng;
	        this.createGeoJson(location, this.map.getZoom(), this.map.getCenter());
	        this.addMarkerActive = !this.question.isRegionTarget();
	        if(this.question.areaSelector) {
	            this.question.areaSelector.enableDrawer();
	        }
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
        		"center": [location.lat, location.lng]
        	}
        };
    this.locations.addData(geojsonFeature);
    if(this.annotationToMark) {
        this.annotationToMark.markerId = id;
        this.updateFeature(id);
    } else {        
    	this.addFeature(id);
    }
}

getAnnotation(id) {
    return this.question.annotations.find(anno => anno.markerId == id);
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

updateFeature(id) {
    let annotation = this.getAnnotation(id);
    let marker = this.getMarker(annotation.markerId);
    annotation.setBody(marker.toGeoJSON());
    annotation.setView(marker.view);
    this.question.saveToLocalStorage();
}

/**
 * Add a new marker. If the marker doesn't exist as an annotation, it is added as well
 */
addFeature(id) {
    let annotation = this.question.addAnnotation();
    annotation.markerId = id;
    let marker = this.getMarker(id);
    annotation.setBody(marker.toGeoJSON());
    annotation.setView(marker.view);
    this.question.saveToLocalStorage();
}

/**
 * Change the location of a feature
 */
moveFeature(marker, location) {
    marker.setLatLng(location);
    let annotation = this.getAnnotation(marker.getId());
    if(annotation) {
        annotation.setGeometry(marker.toGeoJSON().geometry);
        annotation.setView({zoom: this.map.getZoom(), center: [marker.getLatLng().lat, marker.getLatLng().lng]});
    }
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
    if(annotation) {      
	    this.question.deleteAnnotation(annotation);
	    this.question.saveToLocalStorage();
    }
}


</script>


</geoLocationQuestion>

