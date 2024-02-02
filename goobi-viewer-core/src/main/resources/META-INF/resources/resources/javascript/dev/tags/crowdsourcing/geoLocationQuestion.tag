<geoLocationQuestion>
	
	<div if="{this.showInstructions()}" class="crowdsourcing-annotations__instruction">
		<label>{Crowdsourcing.translate("crowdsourcing__help__create_rect_on_image")}</label>
	</div>
	<div if="{this.showAddMarkerInstructions()}" class="crowdsourcing-annotations__single-instruction">
		<label>{Crowdsourcing.translate("crowdsourcing__help__add_marker_to_image")}</label>
	</div>
	<div if="{this.showInactiveInstructions()}" class="crowdsourcing-annotations__single-instruction -inactive">
		<label>{Crowdsourcing.translate("crowdsourcing__help__make_active")}</label>
	</div>
	
	<div class="geo-map__wrapper">
		<div ref="geocoder" class="geocoder"/>
		<div id="geoMap_{opts.index}" class="geo-map"></div>
	</div>
	
	<div id="annotation_{index}" each="{anno, index in this.annotations}">
		
	</div>

<script>


this.question = this.opts.question;
this.annotationToMark = null;
this.addMarkerActive = !this.question.isRegionTarget() && !this.opts.item.reviewMode;

const DEFAULT_VIEW = {
    zoom: 5,
    center: [11.073397, 49.451993] //long, lat
};

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
    this.setFeatures(this.question.annotations);
    if(this.geoMap.layers[0].getMarkerCount() > 0) {
        let zoom = 12;
        if(this.geoMap.layers[0].getMarkerCount() == 1) {
            let marker = this.geoMap.layers[0].getMarker(this.question.annotations[0].markerId);
            if(marker) {                
            	zoom = marker.feature.view.zoom;
            }
        }
        let featureView = this.geoMap.getViewAroundFeatures(this.geoMap.layers[0].getFeatures(), zoom);
	    this.geoMap.setView(featureView);
    }
}

setFeatures(annotations) {
    this.geoMap.layers.forEach(l => l.resetMarkers());
    annotations.filter(anno => !anno.isEmpty()).forEach((anno) => {
        if(anno.color) {
            let markerIcon = this.geoMap.layers[0].getMarkerIcon().options;
            markerIcon.markerColor = anno.color;
            this.geoMap.layers[0].config.markerIcon = markerIcon;
        }
        let marker = this.geoMap.layers[0].addMarker(anno.body);
        anno.markerId = marker.getId();
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
        let marker = this.geoMap.layers[0].getMarker(anno.markerId);
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
    this.geoMap = new viewerJS.GeoMap({
        mapId : "geoMap_" + this.opts.index,
        language: Crowdsourcing.translator.language,
        tilesource: this.opts.geomap.tilesource,
        layers: [{
	        allowMovingFeatures: !this.opts.item.isReviewMode(),
	        popover: undefined,
	        emptyMarkerMessage: undefined,            
	        popoverOnHover: false,
	        markerIcon: {
	            shape: "circle",
	            prefix: "fa",
	            markerColor: "blue",
	            iconColor: "white",
	            icon: "fa-circle",
	            svg: true
	        }
        }]
    })

    let initialView = $.extend(true, {}, DEFAULT_VIEW, this.opts.geomap.initialView);
    this.geoMap.init(initialView);
    this.geoMap.initGeocoder(this.refs.geocoder, {placeholder: Crowdsourcing.translate("ADDRESS")});
    this.geoMap.layers.forEach(l => l.onFeatureMove.subscribe(feature => this.moveFeature(feature)));
    this.geoMap.layers.forEach(l => l.onFeatureClick.subscribe(feature => this.removeFeature(feature)));
    this.geoMap.onMapClick.subscribe(geoJson => {
        if(this.addMarkerActive && (this.question.targetFrequency == 0 || this.geoMap.layers[0].getMarkerCount() < this.question.targetFrequency)) {
            if(this.annotationToMark && this.annotationToMark.color) {
                let markerIcon = this.geoMap.layers[0].getMarkerIcon().options;
                markerIcon.markerColor = this.annotationToMark.color;
                this.geoMap.layers[0].config.markerIcon = markerIcon;
            }
            let marker = this.geoMap.layers[0].addMarker(geoJson);
            if(this.annotationToMark) {
                this.annotationToMark.markerId = marker.getId();
                this.updateFeature(marker.getId());
            } else {        
            	this.addFeature(marker.getId());
            }
	        this.addMarkerActive = !this.question.isRegionTarget();
	        if(this.question.areaSelector) {
	            this.question.areaSelector.enableDrawer();
	        }
        }
    })
}

getAnnotation(id) {
    return this.question.annotations.find(anno => anno.markerId == id);
}

updateFeature(id) {
    let annotation = this.getAnnotation(id);
    let marker = this.geoMap.layers[0].getMarker(annotation.markerId);
    annotation.setBody(marker.feature);
    annotation.setView(marker.feature.view);
    this.question.saveToLocalStorage();
}

/**
 * Add a new marker. If the marker doesn't exist as an annotation, it is added as well
 */
addFeature(id) {
    let marker = this.geoMap.layers[0].getMarker(id);
    let annotation = this.question.addAnnotation();
    annotation.markerId = id;
    annotation.setBody(marker.feature);
    annotation.setView(marker.feature.view);
    this.question.saveToLocalStorage();
}

/**
 * Change the location of a feature
 */
moveFeature(feature) {
    let annotation = this.getAnnotation(feature.id);
    if(annotation) {
        annotation.setGeometry(feature.geometry);
        annotation.setView(feature.view);
    }
    this.question.saveToLocalStorage();
}

/**
 * Remove a marker. If the marker exists as an annotation, it is removed as well
 */
removeFeature(feature) {
    this.geoMap.layers[0].removeMarker(feature);
	let annotation = this.getAnnotation(feature.id);
    if(annotation) {      
	    this.question.deleteAnnotation(annotation);
	    this.question.saveToLocalStorage();
    }
}




</script>


</geoLocationQuestion>

