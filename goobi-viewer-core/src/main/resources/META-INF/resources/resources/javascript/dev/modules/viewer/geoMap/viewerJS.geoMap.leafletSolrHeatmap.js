// Check if L (leaflet) exists, before executing 

if (typeof L !== 'undefined') {
	// console.log('leaflet loaded');
	/**
	  Leaflet-Solr-Heatmap v0.3.0
	  by Jack Reed, @mejackreed
	     Steve McDonald, @spacemansteve
	*/
	
	/**
	* A Base SolrHeatmap QueryAdapter, used for defining a request and response to
	* an API that uses the Solr Facet Heatmap functionality
	*/
	L.SolrHeatmapBaseQueryAdapter = L.Class.extend({
	  initialize: function(options, layer) {
	    this.layer = layer;
	    L.setOptions(this, options);
	  },
	  /*
	  * @param bounds optional param for a spatial query
	  */
	  ajaxOptions: function(bounds) {
	    throw('Not implemented');
	  },
	  responseFormatter: function() {
	    throw('Not implemented');
	  } 
	});
	
	/**
	* A POJO used for defining and accessing Query Adapters
	*/
	L.SolrHeatmapQueryAdapters = {
	  default: L.SolrHeatmapBaseQueryAdapter.extend({
	    ajaxOptions: function(bounds) {
	      return {
	        url: this._solrQuery(),
	        dataType: 'JSONP',
	        data: {
	          q: '*:*',
	          wt: 'json',
	           facet: true,
	           'facet.heatmap': this.options.field,
	           'facet.heatmap.geom': this.layer._mapViewToWkt(bounds),
	           fq: this.options.field + this.layer._mapViewToEnvelope(bounds)
	        },
	        jsonp: 'json.wrf'
	      };
	    },
	    responseFormatter: function(data) {
	      this.layer.count = data.response.numFound;
	      return data.facet_counts.facet_heatmaps;
	    },
	    _solrQuery: function() {
	      return this.layer._heatmapUrl + '/' + this.options.solrRequestHandler + '?' + this.options.field;
	    }
	  }),
	  blacklight: L.SolrHeatmapBaseQueryAdapter.extend({
	    ajaxOptions: function(bounds) {
	      return {
	        url: this.layer._heatmapUrl,
	        dataType: 'JSON',
	        data: {
	          bbox: this._mapViewToBbox(bounds),
	          format: 'json',
	        },
	        jsonp: false
	      };
	    },
	    responseFormatter: function(data) {
	      this.layer.count = data.response.pages.total_count;
	      return data.response.facet_heatmaps;
	    },
	    _mapViewToBbox: function (bounds) {
	      if (this._map === undefined) {
	        return '-180,-90,180,90';
	      }
	      if (bounds === undefined) {
	        bounds = this._map.getBounds();
	      }
	      var wrappedSw = bounds.getSouthWest().wrap();
	      var wrappedNe = bounds.getNorthEast().wrap();
	      return [wrappedSw.lng, bounds.getSouth(), wrappedNe.lng, bounds.getNorth()].join(',');
	    }
	  }),
	  goobiViewer: L.SolrHeatmapBaseQueryAdapter.extend({
	    ajaxOptions: function(bounds) {
	        let options = {
	          url: this._solrQuery(bounds)
	        };
	        return options;
	      },
	      ajaxOptionsForSearchHits: function(bounds) {
	        let options = {
	     	  url: this._searchHitsSolrQuery(bounds)
	   		};
	        return options;
	      },
	      responseFormatter: function(data) {
	        return {
	            "WKT_COORDS" : data
	        }
	      },
	      _solrQuery: function(bounds) {
	        let b = this._getBoundsForQuery(bounds);
	        let region = this.layer._mapViewToWkt(b);
	        let query = this.layer._heatmapUrl.replace("{solrField}", this.options.field) + '?' + 
	                "region=" + encodeURIComponent(region) + '&' + 
		            "query=" + this.options.filterQuery;
		    if(this.options.facetQuery) {
		    	query += '&facetQuery=' + this.options.facetQuery;
		    }
		    return query;
	      },
	      _searchHitsSolrQuery: function(bounds) {
	          bounds = this._getBoundsForQuery(bounds);
	          let region = this.layer._mapViewToWkt(bounds);
		      let query = this.layer.featureUrl.replace("{solrField}", this.options.field) + '?' + 
		                "region=" + encodeURIComponent(region) + '&' + 
		                "labelField=" + this.options.labelField + '&' + 
		                "query=" + this.options.filterQuery;
	       	  if(this.options.facetQuery) {
	    	  	query += '&facetQuery=' + this.options.facetQuery;
	           }
	    	  return query;
		  },
		  _getBoundsForQuery(bounds) {
		  	   if (bounds === undefined) {
	          let rawBounds = this.layer._mapToAdd.getBounds();
	          let east = rawBounds.getEast() > 180 ? 180 : rawBounds.getEast();
	          let west = rawBounds.getWest() < -180 ? -180 : rawBounds.getWest();
	          bounds = new L.latLngBounds(L.latLng(rawBounds.getNorth(), west), L.latLng(rawBounds.getSouth(), east));
	        	}
	       		return bounds;
	       },
	    }),
	  goobiViewerHeatmap: L.SolrHeatmapBaseQueryAdapter.extend({
	    ajaxOptions: function(bounds) {
	        let options = {
	          url: this._solrQuery(bounds)
	        };
	        return options;
	      },
	      ajaxOptionsForSearchHits: function(bounds) {
	        return undefined;
	      },
	      responseFormatter: function(data) {
	        return {
	            "WKT_COORDS" : data
	        }
	      },
	      _solrQuery: function(bounds) {
	        let b = this._getBoundsForQuery(bounds);
	        let region = this.layer._mapViewToWkt(b);
	        let query = this.layer._heatmapUrl.replace("{solrField}", this.options.field) + '?' + 
	                "region=" + encodeURIComponent(region) + '&' + 
		            "query=" + this.options.filterQuery;
		    if(this.options.facetQuery) {
		    	query += '&facetQuery=' + this.options.facetQuery;
		    }
		    return query;
	      },
		  _getBoundsForQuery(bounds) {
		  	   if (bounds === undefined) {
	          let rawBounds = this.layer._mapToAdd.getBounds();
	          let east = rawBounds.getEast() > 180 ? 180 : rawBounds.getEast();
	          let west = rawBounds.getWest() < -180 ? -180 : rawBounds.getWest();
	          bounds = new L.latLngBounds(L.latLng(rawBounds.getNorth(), west), L.latLng(rawBounds.getSouth(), east));
	        	}
	       		return bounds;
	       },
	    })
	  
	  }
	
	/**
	* A Leaflet extension to be used for adding a SolrHeatmap layer to a Leaflet map
	*/
	L.SolrHeatmap = L.GeoJSON.extend({
	  options: {
	    solrRequestHandler: 'select',
	    type: 'geojsonGrid',
	    colors: ['#f1eef6', '#d7b5d8', '#df65b0', '#dd1c77', '#980043'],
	    maxSampleSize: Number.MAX_SAFE_INTEGER,  // for Jenks classification
	    queryAdapter: 'default',
	    queryRadius: 40, // In pixels, used for nearby query
	  },
	
	  visible: true,
	
	  initialize: function(heatmapUrl, featureUrl, featureGroup, options) {
	    var _this = this;
	    L.setOptions(_this, options);
	    _this.featureGroup = featureGroup;
	    _this.queryAdapter = new L.SolrHeatmapQueryAdapters[this.options.queryAdapter](this.options, _this);
	    _this._heatmapUrl = heatmapUrl;
	    _this.featureUrl = featureUrl;
	    _this._layers = {};
	  },
	
	  onAdd: function (map) {
	    // Call the parent function
	    L.GeoJSON.prototype.onAdd.call(this, map);
	
	    map.on('moveend', this._resetLayer, this);
	  },
	
	  onRemove: function(map) {
	    // Call the parent function
	    L.GeoJSON.prototype.onRemove.call(this, map);
	    map.off('moveend', this._resetLayer, this);
	  },
	
	  beforeAdd: function() {
	    this._getData();
	  },
	
	  isVisible: function() {
	    return this.visible;
	  },
	
	  setVisible: function(visible) {
	      this.visible = visible;
	      this._resetLayer();
	      this.featureGroup.setVisible(visible);
	  },
	
	  _resetLayer: function() {
	    if(this.clusterMarkers) {
	      this._clearLayers();
	      this._getData();
	    }
	  },
	
	  _queryNearby: function(bounds) {
	    var _this = this;
	    var startTime = Date.now();
	    var options = _this.queryAdapter.ajaxOptions(bounds);
	    options.success = function(data) {
	      _this.nearbyResponseTime = Date.now() - startTime;
	      data.bounds = bounds;
	      _this.fireEvent('nearbyQueried', data);
	    }
	    $.ajax(options);
	  },
	
	  requestNearby: function(layerPoint) {
	    var dist = this.options.queryRadius;
	    var bounds = L.latLngBounds(
	      this._map.layerPointToLatLng([layerPoint.x + dist, layerPoint.y + dist]),
	      this._map.layerPointToLatLng([layerPoint.x - dist, layerPoint.y - dist])
	    );
	    this._queryNearby(bounds);
	  },
	
	
	
	  _computeHeatmapObject: function(data) {
	    var _this = this;
	    _this.facetHeatmap = {},
	      facetHeatmapArray = _this.queryAdapter.responseFormatter(data)[this.options.field];
			
		if(Array.isArray(facetHeatmapArray)) {
		    // Convert array to an object
		    $.each(facetHeatmapArray, function(index, value) {
		      if ((index + 1) % 2 !== 0) {
		        // Set object keys for even items
		        _this.facetHeatmap[value] = '';
		      }else {
		        // Set object values for odd items
		        _this.facetHeatmap[facetHeatmapArray[index - 1]] = value;
		      }
		    });
	    } else {
	    	this.facetHeatmap = facetHeatmapArray;
	    }
	
	    this._computeIntArrays();
	  },
	
	  _clearLayers: function() {
	    var _this = this;
	
	    switch (_this.options.type) {
	      case 'geojsonGrid':
	        _this.clearLayers();
	        break;
	      case 'clusters':
	        _this.clusterMarkers.clearLayers();
	        break;
	      case 'heatmap':
	    _this._map.removeLayer(_this.heatmapLayer);
	    break;
	    }
	  },
	  
	
	  _createGeojson: function() {
	    var _this = this;
	    var geojson = {};
	
	    geojson.type = 'FeatureCollection';
	    geojson.features = [];
	
	    $.each(_this.facetHeatmap.counts_ints2D, function(row, value) {
	      if (value === null) {
	        return;
	      }
	
	      $.each(value, function(column, val) {
	        if (val === 0) {
	          return;
	        }
	
	        var newFeature = {
	          type: 'Feature',
	          geometry: {
	            type: 'Polygon',
	            coordinates: [
	              [
	                [_this._minLng(column), _this._minLat(row)],
	                [_this._minLng(column), _this._maxLat(row)],
	                [_this._maxLng(column), _this._maxLat(row)],
	                [_this._maxLng(column), _this._minLat(row)],
	                [_this._minLng(column), _this._minLat(row)]
	              ]
	            ]
	          },
	          properties: {
	            count: val
	          }
	        };
	        geojson.features.push(newFeature);
	      });
	    });
	    _this.addData(geojson);
	    var colors = _this.options.colors; 
	    var classifications = _this._getClassifications(colors.length);
	    _this._styleByCount(classifications);
	  },
	
	  _createHeatmap: function(){
	    var _this = this;
	    var heatmapCells = [];
	    var cellSize = _this._getCellSize() * .75;
	    var colors = _this.options.colors; 
	    var classifications = _this._getClassifications(colors.length - 1);
	    var maxValue = classifications[classifications.length - 1];
	    var gradient = _this._getGradient(classifications);
	
	    $.each(_this.facetHeatmap.counts_ints2D, function(row, value) {
	      if (value === null) {
	        return;
	      }
	
	      $.each(value, function(column, val) {
	        if (val === 0) {
	          return;
	        }
	  var scaledValue = Math.min((val / maxValue), 1);
	  var current = [_this._minLat(row), _this._minLng(column), scaledValue];
	  heatmapCells.push(current);
	  // need to create options object to set gradient, blu, radius, max
	      })
	    });
	
	    // settting max due to bug
	    // http://stackoverflow.com/questions/26767722/leaflet-heat-issue-with-adding-points-with-intensity
	    var options = {max: .0001, radius: cellSize, gradient: gradient};
	    var heatmapLayer = L.heatLayer(heatmapCells, options);
	    heatmapLayer.addTo(_this._map);
	    _this.heatmapLayer = heatmapLayer;
	  },
	
	  // heatmap display need hash of scaled counts value, color pairs
	  _getGradient: function (classifications){
	    var gradient = {};
	    var maxValue = classifications[classifications.length - 1];
	    var colors = _this.options.colors; 
	    // skip first lower bound, assumed to be 0 from Jenks
	    for (var i = 1 ; i < classifications.length ; i++)
	  gradient[classifications[i] / maxValue] = colors[i];
	    return gradient;
	  },
	
	  // compute size of heatmap cells in pixels
	  _getCellSize: function(){
	    _this = this;
	    var mapSize = _this._map.getSize();  // should't we use solr returned map extent?
	    var widthInPixels = mapSize.x; 
	    var heightInPixels = mapSize.y;
	    var heatmapRows = _this.facetHeatmap.rows;
	    var heatmapColumns = _this.facetHeatmap.columns;
	    var sizeX = widthInPixels / heatmapColumns;
	    var sizeY = heightInPixels / heatmapRows;
	    var size = Math.ceil(Math.max(sizeX, sizeY));
	    return size;
	},
	
	  _setRenderTime: function() {
	    var _this = this;
	    _this.renderTime = (Date.now() - _this.renderStart);
	  },
	  
	  _createMarker(count) {
	  
	    return this.featureGroup.getClusterIcon(count);
	  
	//  	let background = this.featureGroup.config.markerIcon.markerColor;
	//    let color = this.featureGroup.config.markerIcon.iconColor;
	//    return new L.DivIcon({ html: '<div style="background-color:'+background+'; color:'+color+'"><span>' + count + '</span></div>', className: 'geomap-heatmap-marker', iconSize: new L.Point(40, 40) });
	  },
	
	  _createClusters: function() {
			var _this = this;
			_this.clusterMarkers = new L.MarkerClusterGroup({
	      	maxClusterRadius: 140,
	      	iconCreateFunction: function(cluster) {
	       		return _this._createMarker(_this._computeTotalChildHits(cluster));
	       	}
	    });
		_this.featureGroup.removeAllMarkers();
	
	    $.each(_this.facetHeatmap.counts_ints2D, function(row, value) {
	      if (value === null) {
	        return;
	      }
	
	      $.each(value, function(column, val) {
	        if (val === 0) {
	          return;
	        }
	
	        var bounds = new L.latLngBounds([
	          [_this._minLat(row), _this._minLng(column)],
	          [_this._maxLat(row), _this._maxLng(column)]
	        ]);
	        
	        let marker = new L.Marker(bounds.getCenter(), {
	          icon: L.divIcon({
	          	iconSize: [0,0]
	          }),
	          count: val,
	          bounds : bounds
	        });
			if(_this.queryAdapter.ajaxOptionsForSearchHits()) {
				marker.on('add', e => {
					setTimeout(() => _this._expandMarker(e.target), 0);
				});
			} else {
				marker = new L.Marker(bounds.getCenter(), {
					icon: _this.featureGroup.getMarkerIcon({count: val}),
					count: val,
					bounds : bounds
				  });
			}
	        //marker.on('click', e => _this._expandMarker(e.target));
	        _this.clusterMarkers.addLayer(marker);
	      });
	    });
	 
	    _this._map.addLayer(_this.clusterMarkers);
	  },
	  
	  _expandMarker(marker) {
	  		var visibleOne = this.clusterMarkers.getVisibleParent(marker);
		    if(visibleOne === marker) {
				let bounds = marker.options.bounds;
			    if(bounds && this.queryAdapter.ajaxOptionsForSearchHits) {
			        $.ajax(this.queryAdapter.ajaxOptionsForSearchHits(bounds))
			        .then(res => {
	        			res.forEach(geoJson => {
	                let m = this.featureGroup.addMarker(geoJson);
	                //console.log("added ", geoJson, " to ", marker);
	        			});
	        		});
			       		
	        		if(marker.options.count) {
	        			marker.options.count = 0;
	        		}
	        	}
	    	}
	  },
	
	  _computeTotalChildHits(cluster) {
	  	let count = 0;
	  	// if(cluster._childClusters && cluster._childClusters.length > 0) {
	  	// 	count += cluster._childClusters.map(child => this._computeTotalChildHits(child)).reduce((a, b) => a + b, 0)
	  	// }
	  	// if(cluster._markers && cluster._markers.length > 0) {
	  	// 	count += cluster._markers.map(child => child.options.count ? child.options.count : 0).reduce((a, b) => a + b, 0)
	    // }
	    count +=  cluster.getAllChildMarkers().map(child => child.options.count ? child.options.count : 0).reduce((a, b) => a + b, 0);
	  	if(cluster.options && cluster.options.count) {
	  		count += cluster.options.count;
	  	}
	  	return count;
	  },
	
	  _computeIntArrays: function() {
	    var _this = this;
	
	    _this.lengthX = (_this.facetHeatmap.maxX - _this.facetHeatmap.minX) / _this.facetHeatmap.columns;
	    _this.lengthY = (_this.facetHeatmap.maxY - _this.facetHeatmap.minY) / _this.facetHeatmap.rows;
	
	    switch (_this.options.type) {
	      case 'geojsonGrid':
	        _this._createGeojson();
	        break;
	      case 'clusters':
	        _this._createClusters();
	        break;
	      case 'heatmap':
	        _this._createHeatmap();
	        break;
	    }
	    _this._setRenderTime();
	  },
	
	  _getClassifications: function(howMany)
	  {
	    var _this = this;
	    var one_d_array = [];
	    for(var i = 0; i < _this.facetHeatmap.counts_ints2D.length; i++) {
	      if (_this.facetHeatmap.counts_ints2D[i] != null) {
	        one_d_array = one_d_array.concat(_this.facetHeatmap.counts_ints2D[i]);
	      }
	    }
	    var sampled_array = _this._sampleCounts(one_d_array);
	
	    var series = new geostats(sampled_array);
	    var scale = _this.options.colors; 
	    var classifications = series.getClassJenks(howMany);
	    return classifications;
	  },
	
	  _styleByCount: function(classifications) {
	    var _this = this;
	    var scale = _this.options.colors;
	
	    _this.eachLayer(function(layer) {
	      var color;
	      $.each(classifications, function(i, val) {
	        if (layer.feature.properties.count >= val) {
	          color = scale[i];
	        }
	      });
	      layer.setStyle({
	        fillColor: color,
	        fillOpacity: 0.5,
	        weight: 0
	      });
	    });
	  },
	
	  // Jenks classification can be slow so we optionally sample the data
	  // typically any big sample of counts are much the same, don't need to classify on all of them
	  _sampleCounts: function(passedArray) {
	    var _this = this;
	    if (passedArray.length <= _this.options.maxSampleSize) {
	      return passedArray;   // array too small to sample
	    };
	
	    var maxValue = Math.max.apply(Math, passedArray);
	    var sampledArray = [];
	    var period = Math.ceil(passedArray.length / _this.options.maxSampleSize);
	    for (i = 0 ; i < passedArray.length ; i = i + period) {
	      sampledArray.push(passedArray[i]);
	    }
	
	    sampledArray.push(maxValue);  // make sure largest value gets in, doesn't matter much if duplicated
	    return sampledArray
	  },
	
	  _minLng: function(column) {
	    return this.facetHeatmap.minX + (this.lengthX * column);
	  },
	
	  _minLat: function(row) {
	    return this.facetHeatmap.maxY - (this.lengthY * row) - this.lengthY;
	  },
	
	  _maxLng: function(column) {
	    return this.facetHeatmap.minX + (this.lengthX * column) + this.lengthX;
	  },
	
	  _maxLat: function(row) {
	    return this.facetHeatmap.maxY - (this.lengthY * row);
	  },
	
	  _getData: function() {
	    if(this.visible) {
	      var _this = this;
	      var startTime = Date.now();
	      var options = _this.queryAdapter.ajaxOptions();
	      options.success = function(data) {
	        _this.responseTime = Date.now() - startTime;
	        _this.renderStart = Date.now();
	        _this._computeHeatmapObject(data);
	        _this.fireEvent('dataAdded', data);
	      }
	      $.ajax(options);
	    }
	  },
	
	  _mapViewToEnvelope: function(bounds) {
	    if (this._map === undefined) {
	      return ':"Intersects(ENVELOPE(-180, 180, 90, -90))"';
	    }
	    if (bounds === undefined) {
	      bounds = this._map.getBounds();
	    }
	    var wrappedSw = bounds.getSouthWest().wrap();
	    var wrappedNe = bounds.getNorthEast().wrap();
	    return ':"Intersects(ENVELOPE(' + wrappedSw.lng + ', ' + wrappedNe.lng + ', ' + bounds.getNorth() + ', ' + bounds.getSouth() + '))"';
	  },
	
	  _mapViewToWkt: function(bounds) {
	    let map = this._map ? this._map : this._mapToAdd;
	    if (map === undefined) {
	      return '["-180 -90" TO "180 90"]';
	    }
	    if (bounds === undefined) {
	      bounds = map.getBounds();
	    }
	    var wrappedSw = bounds.getSouthWest().wrap();
	    var wrappedNe = bounds.getNorthEast().wrap();
	    return '["' + wrappedSw.lng + ' ' + bounds.getSouth() + '" TO "' + wrappedNe.lng + ' ' + bounds.getNorth() + '"]';
	  }
	});
	
	L.solrHeatmap = function(heatmapUrl, featureUrl, featureGroup, options) {
	  return new L.SolrHeatmap(heatmapUrl, featureUrl, featureGroup, options);
	};
}
