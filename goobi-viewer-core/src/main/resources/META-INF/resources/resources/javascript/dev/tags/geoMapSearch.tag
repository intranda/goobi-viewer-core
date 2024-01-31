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
   // console.log("mapsearch ", this.opts);
	this.geoMap = this.initMap();
	this.drawLayer = this.initDrawLayer(this.geoMap);
    if(this.opts.area) {
    	this.initArea(this.drawLayer, this.opts.area);
    } 
	if(!this.opts.inactive) { 
	    this.initGeocoder(this.geoMap);
	    this.drawnItems = this.initMapDraw(this.geoMap, this.drawLayer);
	}
 	this.hitsLayer = this.initHitsLayer(this.geoMap);
	if(this.opts.toggleFeatures) {   
		this.initToggleLayer(this.geoMap, this.hitsLayer, this.opts.toggleFeatures);
	}
	if(this.opts.heatmap?.enabled) {	    
		this.heatmap = this.initHeatmap(this.hitsLayer)
	}
}); 

initMap() {
    //console.log("initializing geomap for search", this.refs.map);
    let geoMap = new viewerJS.GeoMap({
        element : this.refs.map, 
        language: viewerJS.translator.language,
        fixed: this.opts.inactive ? true : false,
        layer: this.opts.hitsLayer
    })
    let initialView = {
        zoom: 5,
        center: [11.073397, 49.451993] //long, lat
    };
    geoMap.init(initialView, this.opts.features);
    return geoMap;
}

initDrawLayer(map) {
    let drawLayer = new viewerJS.GeoMap.featureGroup(map, {
   	    style : this.opts.areaLayer.style
    });
	return drawLayer;
}

initGeocoder(map) {
	let geocoderConfig = {};
	if(this.opts.search_placeholder) {
		geocoderConfig.placeholder = this.opts.search_placeholder
	}
	if(this.opts.search_enabled) {            
   		map.initGeocoder(this.refs.geocoder, geocoderConfig);
	} 
}
    
initToggleLayer(geoMap, layer, button) {
	let ToggleFeaturesControl = L.Control.extend({
	    options: {
	        position: "topleft"
	    },
	    onAdd: function(map) {
	        L.DomEvent.on(button, "dblclick" , (e) => {
	            L.DomEvent.stopPropagation(e);
	            e.stopPropagation();
	            return false;
	        });
	        L.DomEvent.on(button, "click" , (e) => {
	            layer.setVisible(!layer.isVisible());
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
	geoMap.map.addControl(control);
}
 
initArea(layer, shape) {
	if(viewerJS.isString(shape)) {
        try {                
        	shape = JSON.parse(shape);
        } catch(e) {
            console.error("Unable to draw geomap area ", this.opts.area, ": cannot parse json");
        }
    }

    let feature = undefined;
    switch(shape.type) {
        case "polygon":
            feature = layer.drawPolygon(shape.vertices, true);
            break;
        case "circle":
            feature = layer.drawCircle(shape.center, shape.radius, true);
            break;
        case "rectangle":
            feature = layer.drawRectangle([shape.vertices[0], shape.vertices[2]], true);
            break;
    }
    this.onLayerDrawn({layer: feature});
} 

initMapDraw(geomap, drawLayer) {
    //console.log("init map draw");
    let drawnItems = new L.FeatureGroup();
    //this.drawnItems = this.drawLayer.locations;
    geomap.map.addLayer(drawnItems);
    let drawControl = new L.Control.Draw({
        edit: {
            featureGroup: drawnItems,
            edit: false,
            remove: false
        },
        draw: {
            polyline: false,
            marker: false,
            circlemarker: false
        }
    });
    drawControl.setDrawingOptions({
        rectangle: {
        	shapeOptions: drawLayer.config.style
        },
        circle: {
        	shapeOptions: drawLayer.config.style
        },
        polygon: {
        	shapeOptions: drawLayer.config.style
        }
    });
    
    geomap.map.addControl(drawControl);
    //console.log("initialized map draw", this.drawControl);
    
    let edited = new rxjs.Subject();
    edited.pipe(rxjs.operators.debounceTime(300)).subscribe(e => this.onLayerEdited(e));
    geomap.map.on(L.Draw.Event.EDITMOVE, e => edited.next(e));
    geomap.map.on(L.Draw.Event.EDITRESIZE, e => edited.next(e));
    geomap.map.on(L.Draw.Event.EDITVERTEX, e => edited.next(e));

    let deleted = new rxjs.Subject();
    deleted.subscribe(e => this.onLayerDeleted(e));
    geomap.map.on(L.Draw.Event.DELETED, e => deleted.next(e));
    geomap.map.on(L.Draw.Event.DRAWSTART, e => deleted.next(e));
    
    geomap.map.on(L.Draw.Event.CREATED, (e) => this.onLayerDrawn(e));

    if(this.opts.reset_button) {
        $(this.opts.reset_button).on("click",  e => deleted.next(e));
    }
    return drawnItems;
}

onLayerDeleted(e) {
    if(this.searchLayer) {
        if(this.drawnItems) {
        	this.drawnItems.removeLayer(this.searchLayer);
        }
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
    if(e.layer) {        
	    this.searchLayer = e.layer;
		if(this.drawnItems) {
	    	this.drawnItems.addLayer(e.layer);
		}
		this.searchLayer.editing.enable();
		this.setSearchArea(this.searchLayer);
    }
}

setSearchArea(layer) {
    //console.log("set search area", layer);
    let type = this.getType(layer);
    switch(type) {
        case "polygon":
        case "rectangle":
            let origLayer = L.polygon(layer.getLatLngs());
            let wrappedCenter = this.geoMap.map.wrapLatLng(layer.getCenter());
            let distance = layer.getCenter().lng - wrappedCenter.lng;
            //console.log("distance ", wrappedCenter.lng, layer.getCenter().lng, distance);
	        let vertices = [...layer.getLatLngs()[0]].map(p => L.latLng(p.lat, p.lng-distance)).map(p => this.geoMap.normalizePoint(p));
            //console.log("moved ", origLayer, " by ", distance, " from ", layer.getLatLngs()[0], " to ", vertices);
	        if(vertices[0] != vertices[vertices.length-1]) {	            
	        	vertices.push(vertices[0]);
	        }
	        //console.log("drew feature ", vertices);
	        this.notifyFeatureSet({
	           type : type,
	           vertices: vertices.map(p => [p.lat, p.lng])
	        })
	        break;
        case "circle":
            let bounds = this.geoMap.map.wrapLatLngBounds(layer.getBounds());
            let center = this.geoMap.map.wrapLatLng(layer.getLatLng());
            let circumgon = this.createCircumgon(bounds.getCenter(), bounds.getSouthWest(), bounds.getSouthWest(), 16);
            let diameterM = bounds.getSouthWest().distanceTo(bounds.getNorthWest());
            this.notifyFeatureSet({
                type : "circle",
                vertices: circumgon.map(p => this.geoMap.normalizePoint(p)).map(p => [p.lat, p.lng]),
                center: center,
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

initHitsLayer(map) {
    this.opts.hitsLayer.language = viewerJS.translator.language;
	let hitsLayer = new viewerJS.GeoMap.featureGroup(map, this.opts.hitsLayer);
	map.layers.push(hitsLayer);
	// console.log("init hits layer ", this.opts.hitsLayer, hitsLayer, this.opts.features);
	hitsLayer.init(this.opts.features, false);
	hitsLayer.onFeatureClick.subscribe(f => {
		if(f.properties && f.properties.link) {
			$(this.opts.search?.loader).show();
			window.location.assign(f.properties.link);
		}
	})

	return hitsLayer;
}

initHeatmap(hitsLayer) {
	let heatmapQuery = this.opts.heatmap.mainQuery;
	let heatmapFacetQuery = this.opts.heatmap.facetQuery;
	
	let heatmap = L.solrHeatmap(this.opts.heatmap.heatmapUrl, this.opts.heatmap.featureUrl, hitsLayer, {
		field: "WKT_COORDS",
		type: "clusters",
		filterQuery: heatmapQuery,
		facetQuery: heatmapFacetQuery,
		labelField: this.opts.heatmap.labelField,
		queryAdapter: "goobiViewer"    
	});
	heatmap.addTo(this.geoMap.map);
	return heatmap;
}

</script>

</geoMapSearch>