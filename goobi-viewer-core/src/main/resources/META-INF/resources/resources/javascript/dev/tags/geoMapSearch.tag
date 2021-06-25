<geoMapSearch>

<yield>

<div class="geo-map__wrapper">
	<div ref="geocoder" class="geocoder"/>
	<div class="geo-map__buttons-wrapper">
	</div>
	<div ref="map" class="geo-map">
	</div>
</div>


<script>

this.on("mount", function() {
	this.initMap();
}); 

initMap() {
    //console.log("initializing geomap for search", this.refs.map);
    this.geoMap = new viewerJS.GeoMap({
        element : this.refs.map, 
        language: viewerJS.translator.language,
        fixed: this.opts.inactive ? true : false,
        layer: {
	        allowMovingFeatures: false,
	        popover: $("<div><p data-metadata='title'></p></div>"),
	        popoverOnHover: true,
	        emptyMarkerMessage: undefined,
// 	        markerIcon: {
// 	            shape: "circle",
// 	            prefix: "fa",
// 	            markerColor: "blue",
// 	            iconColor: "white",
// 	            icon: "fa-circle",
// 	            svg: true
// 	        },
		    style: {
				    fillOpacity: 0.02
			}
        }
    })
    let initialView = {
        zoom: 5,
        center: [11.073397, 49.451993] //long, lat
    };
    console.log("init map ", this.opts);
    this.geoMap.init(initialView, this.opts.features);
    this.drawLayer = new viewerJS.GeoMap.featureGroup(this.geoMap, {
   	    style : {
         	fillColor : "#d9534f",
         	color : "#d9534f",
         	fillOpacity : 0.3,
        	}
    });
    if(!this.opts.inactive) {        
        let geocoderConfig = {};
        if(this.opts.search_placeholder) {
            geocoderConfig.placeholder = this.opts.search_placeholder
        }
	    this.geoMap.initGeocoder(this.refs.geocoder, geocoderConfig);
	    this.initMapDraw();
    }
    
//     this.refs.toggleMarkers.addEventListener("click", () => {
//         this.geoMap.layers[0].setVisible(!this.geoMap.layers[0].isVisible());
//     })
    
    
    
    this.geoMap.layers[0].onFeatureClick.subscribe(f => {
        if(f.properties && f.properties.link) {
           window.location.assign(f.properties.link);
       }
    })

	if(this.opts.toggleFeatures) {   
		let ToggleFeaturesControl = L.Control.extend({
		    options: {
		        position: "topleft"
		    },
		    onAdd: function(map) {
		        let button  = this.opts.toggleFeatures;
		        L.DomEvent.on(button, "dblclick" , (e) => {
		            L.DomEvent.stopPropagation(e);
		            e.stopPropagation();
		            return false;
		        });
		        L.DomEvent.on(button, "click" , (e) => {
		            this.geoMap.layers[0].setVisible(!this.geoMap.layers[0].isVisible());
		            L.DomEvent.stopPropagation(e);
		            e.stopPropagation();
		            return false;
		        });
		        return button;
		    }.bind(this),
		    onRemove: function(map) {
		        
		    }
		})
		let control = new ToggleFeaturesControl();
		this.geoMap.map.addControl(control);
	}
    
    if(this.opts.area) {
        let shape = this.opts.area;
        if(viewerJS.isString(shape)) {
            try {                
            	shape = JSON.parse(shape);
            } catch(e) {
                console.error("Unable to draw geomap area ", this.opts.area, ": cannot parse json");
            }
        }

        let layer = undefined;
        switch(shape.type) {
            case "polygon":
                layer = this.drawLayer.drawPolygon(shape.vertices, true);
                break;
            case "circle":
                layer = this.drawLayer.drawCircle(shape.center, shape.radius, true);
                break;
            case "rectangle":
                layer = this.drawLayer.drawRectangle([shape.vertices[0], shape.vertices[2]], true);
                break;
        }
        this.onLayerDrawn({layer: layer});

    }

} 

initMapDraw() {
    //console.log("init map draw");
    this.drawnItems = new L.FeatureGroup();
    //this.drawnItems = this.drawLayer.locations;
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
    this.drawControl.setDrawingOptions({
        rectangle: {
        	shapeOptions: this.drawLayer.config.style
        },
        circle: {
        	shapeOptions: this.drawLayer.config.style
        },
        polygon: {
        	shapeOptions: this.drawLayer.config.style
        }
    });
    
    this.geoMap.map.addControl(this.drawControl);
    //console.log("initialized map draw", this.drawControl);
    
    let edited = new rxjs.Subject();
    edited.pipe(rxjs.operators.debounceTime(300)).subscribe(e => this.onLayerEdited(e));
    this.geoMap.map.on(L.Draw.Event.EDITMOVE, e => edited.next(e));
    this.geoMap.map.on(L.Draw.Event.EDITRESIZE, e => edited.next(e));
    this.geoMap.map.on(L.Draw.Event.EDITVERTEX, e => edited.next(e));

    let deleted = new rxjs.Subject();
    deleted.subscribe(e => this.onLayerDeleted(e));
    this.geoMap.map.on(L.Draw.Event.DELETED, e => deleted.next(e));
    this.geoMap.map.on(L.Draw.Event.DRAWSTART, e => deleted.next(e));
    
    this.geoMap.map.on(L.Draw.Event.CREATED, (e) => this.onLayerDrawn(e));

    if(this.opts.reset_button) {
        $(this.opts.reset_button).on("click",  e => deleted.next(e));
    }
}

onLayerDeleted(e) {
    if(this.searchLayer) {
        this.drawnItems.removeLayer(this.searchLayer);
        this.searchLayer = undefined;
    }
    this.notifyFeatureSet(undefined);
}

onLayerEdited(e) {
    //console.log("layer edited ", e);
    if(e.layer) {        
    	this.searchLayer = e.layer;
    } else if(e.poly) {
        this.searchLayer = e.poly;
    } else {
        logger.warn("Called layer edited event with no given layer ", e);
        return;
    }
	this.setSearchArea(this.searchLayer);
}

onLayerDrawn(e) {
    //console.log("layer drawn ", e);
    this.searchLayer = e.layer;
	this.drawnItems.addLayer(e.layer);
	this.searchLayer.editing.enable();
	this.setSearchArea(this.searchLayer);
}

setSearchArea(layer) {
    //console.log("set search area", layer);
    let type = this.getType(layer);
    switch(type) {
        case "polygon":
        case "rectangle":
	        let vertices = [...layer.getLatLngs()[0]];
	        if(vertices[0] != vertices[vertices.length-1]) {	            
	        	vertices.push(vertices[0]);
	        }
	        this.notifyFeatureSet({
	           type : type,
	           vertices: vertices.map(p => [p.lat, p.lng])
	        })
	        break;
        case "circle":
            let bounds = layer.getBounds();
            let circumgon = this.createCircumgon(bounds.getCenter(), bounds.getSouthWest(), bounds.getSouthWest(), 16);
            let diameterM = bounds.getSouthWest().distanceTo(bounds.getNorthWest());
            this.notifyFeatureSet({
                type : "circle",
                vertices: circumgon.map(p => [p.lat, p.lng]),
                center: layer.getLatLng(),
            	radius: layer.getRadius()
            })
            break;
    }
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

notifyFeatureSet(feature) {
    //console.log("Set feature ", feature);
    if(this.opts.onFeatureSelect) {
        this.opts.onFeatureSelect(feature);
    }

}

buildSearchString(vertices) {
    let string = "WKT_COORDS:\"IsWithin(POLYGON((";
    string += vertices.map(v => v[1] + " " + v[0]).join(", ");
    string += "))) distErrPct=0\"";
    return string;
}

getType(layer) {
    if(layer.getRadius) {
        return "circle";
    } else if(layer.setBounds) {
        return "rectangle";
    } else if(layer.getLatLngs) {
        return "polygon"
    } else {
        throw "Unknown layer type: " + layer;
    }
}

</script>

</geoMapSearch>