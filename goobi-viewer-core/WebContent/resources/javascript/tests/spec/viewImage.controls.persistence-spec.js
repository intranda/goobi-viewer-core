/**
 * This Spec tests the viewer control Methods in viewImageJS.
 * 
 * @version 1.0
 * @author Marc Lettau-Poelchen
 * @copyright intranda GmbH 2017
 */
describe( 'ImageView zoom/rotation persistence Tests', function() {
	var config = null;
	
	beforeEach( function(){
		
		$('<span id="test"><style> #map { width: 800px; }</style><form id="tagListForm"><div id="map"></div></form></span>').appendTo('body');
//		jasmine.getFixtures().load("viewImage.html");
		localStorage.imageLocation = "";
//		viewImage.getConfig().image.location = null;
		config = {
				    global: {
				    	divId: "map",
				        footerHeight: 0,
				        adaptContainerHeight: true,
				        persistZoom: true,
			            persistRotation: true,
			            persistenceId: "a",
				    },
				    image: {
				        mimeType: "image/jpg",
					    tileSource : 'some/image/url',
				        baseFooterUrl : "",
				        highlightCoords: [{
				                name: "searchHighlighting",
				                coordinates: [],
				                displayTooltip: false
				            }, 
				            {
				                name: "ugc",
				                coordinates: [],
				                displayTooltip: true
				            }
				        ]
				    }
				};
	});

	afterEach(function(){
		$('#test').remove()
	});


		it(" zooms and rotates the image using config settings", function(done) {
			
			localStorage.imageLocation = '{"x":0.2,"y":0.8,"zoom":4.5,"rotation":180,"persistenceId":"a"}';
			var expectedLocation = JSON.parse(localStorage.imageLocation);
			
			config.image.tileSource = "http://www.intranda.com/wp-content/uploads/2014/01/banner_digitisation_small.jpg";
			var viewImage = new ImageView.Image(config);
			Rx.Observable.fromPromise(viewImage.load())
			.map(function(osViewer) {
			       expect(osViewer.getConfig().image.location.x).toEqual(expectedLocation.x);
			       expect(osViewer.getConfig().image.location.y).toEqual(expectedLocation.y);
			       expect(osViewer.getConfig().image.location.zoom).toEqual(expectedLocation.zoom);
			       expect(osViewer.getConfig().image.location.rotation).toEqual(expectedLocation.rotation);
			       return osViewer;
			})
			.map(function(osViewer) {
				osViewer.observables.viewerZoom
				.merge(osViewer.observables.viewerRotate)
					.debounce(10)
					.take(1)
					.filter(function() {
						expect(osViewer.controls.getZoom()).toEqual(expectedLocation.zoom);
						expect(osViewer.controls.getCenter().x).toEqual(expectedLocation.x);
						expect(osViewer.controls.getCenter().y).toEqual(expectedLocation.y);
						expect(osViewer.controls.getRotation()).toEqual(expectedLocation.rotation);
						return true;
				})
				.subscribe(function() {
					done();
				});
			})		
			.catch(function(error){
			    	console.log(error);
			        done.fail("Faile loading image: " + error.message);
			 })
			.subscribe();
			
		})
		
		it(" stores current zoom and rotation", function(done) {
			var expectedLocation = {
				x : 0.2,
				y : 0.8,
				zoom: 4,
				rotation: 180,
				persistenceId: "a"
			}
			config.image.tileSource = "http://www.intranda.com/wp-content/uploads/2014/01/banner_digitisation_small.jpg";

			
			var viewImage = new ImageView.Image(config);
            Rx.Observable.fromPromise(viewImage.load())
			.do(function(osViewer) {
				   expect(osViewer.controls.getRotation()).toEqual(0);
			       osViewer.controls.rotateTo(expectedLocation.rotation);
			       osViewer.controls.setCenter(expectedLocation);
			       osViewer.controls.zoomTo(expectedLocation.zoom);
			})
			.do(function(osViewer) {
				onbeforeunload();
				expect(JSON.parse(localStorage.imageLocation)).toEqual(expectedLocation)
			})
			.catch(function(error){
			    	console.log(error);
			        done.fail("Faile loading image: " + error.message);
			 })
			.subscribe(function(){done()});
			
		})
} );
